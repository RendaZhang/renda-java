# 生产级 Spring AI 服务 K8s 完整部署清单 + Helm Chart + 灰度发布配置
以下所有配置均基于 **K8s 1.26+ 稳定版** 编写，兼容阿里云ACK、腾讯云TKE、AWS EKS、自建K8s集群，严格遵循云原生生产级最佳实践，可直接修改后落地使用。

## 一、前置环境准备
1.  已部署 K8s 1.26+ 集群，节点配置满足 AI 服务最低要求（2核4G起步，生产环境推荐4核8G以上）
2.  集群已部署 **Nginx Ingress Controller**（流量入口）
3.  集群已部署 **Prometheus + Grafana**（可观测性，可选但生产推荐）
4.  已准备私有镜像仓库（如 Harbor、阿里云ACR），存放 Spring AI 服务镜像
5.  可选组件：Argo Rollouts（灰度发布）、KEDA（进阶弹性伸缩）、HashiCorp Vault（密钥管理）

---

## 二、完整生产级 K8s 部署清单（按部署顺序执行）
### 1. 命名空间与 RBAC 权限配置（必须最先部署）
生产环境严格遵循**最小权限原则**，独立命名空间隔离，禁止使用默认`default`命名空间，禁止授予集群管理员权限。
```yaml
# 01-namespace-rbac.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: ai-service
  labels:
    app: ai-service
    environment: prod
---
# 服务账户
apiVersion: v1
kind: ServiceAccount
metadata:
  name: ai-service-account
  namespace: ai-service
automountServiceAccountToken: true
---
# 角色：仅授予必要的权限
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: ai-service-role
  namespace: ai-service
rules:
  - apiGroups: [""]
    resources: ["configmaps", "secrets", "pods", "services", "endpoints"]
    verbs: ["get", "list", "watch"]
  - apiGroups: ["apps"]
    resources: ["deployments"]
    verbs: ["get", "list", "watch"]
---
# 角色绑定
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: ai-service-role-binding
  namespace: ai-service
subjects:
  - kind: ServiceAccount
    name: ai-service-account
    namespace: ai-service
roleRef:
  kind: Role
  name: ai-service-role
  apiGroup: rbac.authorization.k8s.io
```
执行命令：`kubectl apply -f 01-namespace-rbac.yaml`

---

### 2. 网络策略（安全隔离，生产必配）
限制命名空间内的流量访问，仅允许必要的服务间通信，防止横向渗透，严格遵循零信任原则。
```yaml
# 02-network-policy.yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: ai-service-default-deny
  namespace: ai-service
spec:
  podSelector: {}
  policyTypes:
    - Ingress
    - Egress
  # 默认拒绝所有入站/出站流量，仅放开下方白名单
---
# 允许Ingress控制器访问AI网关
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-ingress-to-gateway
  namespace: ai-service
spec:
  podSelector:
    matchLabels:
      app: ai-gateway
  policyTypes:
    - Ingress
  ingress:
    - from:
        - namespaceSelector:
            matchLabels:
              kubernetes.io/metadata.name: ingress-nginx
      ports:
        - protocol: TCP
          port: 8080
---
# 允许AI服务间内部通信
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-ai-service-internal
  namespace: ai-service
spec:
  podSelector:
    matchLabels:
      app.kubernetes.io/part-of: ai-service
  policyTypes:
    - Ingress
  ingress:
    - from:
        - podSelector:
            matchLabels:
              app.kubernetes.io/part-of: ai-service
---
# 允许所有AI服务出站访问中间件、MCP服务、大模型API
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-ai-service-egress
  namespace: ai-service
spec:
  podSelector:
    matchLabels:
      app.kubernetes.io/part-of: ai-service
  policyTypes:
    - Egress
  egress:
    # 允许访问集群内部DNS
    - to:
        - namespaceSelector:
            matchLabels:
              kubernetes.io/metadata.name: kube-system
      ports:
        - protocol: UDP
          port: 53
        - protocol: TCP
          port: 53
    # 允许访问中间件命名空间（MySQL、Redis、Kafka、向量库）
    - to:
        - namespaceSelector:
            matchLabels:
              kubernetes.io/metadata.name: middleware
    # 允许访问业务MCP服务命名空间
    - to:
        - namespaceSelector:
            matchLabels:
              kubernetes.io/metadata.name: business-service
    # 允许访问公网大模型API（可按需限制IP段）
    - to:
        - ipBlock:
            cidr: 0.0.0.0/0
      ports:
        - protocol: TCP
          port: 443
```
执行命令：`kubectl apply -f 02-network-policy.yaml`

---

### 3. 配置中心 ConfigMap（非敏感配置）
存放服务配置、Prompt模板、MCP服务地址、RAG参数等非敏感信息，支持动态更新，无需重启服务。
```yaml
# 03-configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: ai-service-common-config
  namespace: ai-service
data:
  SPRING_PROFILES_ACTIVE: "prod"
  TZ: "Asia/Shanghai"
  # 大模型代理网关地址（统一收口LLM调用）
  LLM_PROXY_BASE_URL: "http://llm-proxy.middleware.svc.cluster.local:8080"
  # 向量库地址
  VECTOR_STORE_REDIS_HOST: "redis-vector.middleware.svc.cluster.local"
  VECTOR_STORE_REDIS_PORT: "6379"
  # MCP服务统一网关地址
  MCP_GATEWAY_URL: "http://ai-mcp-gateway.ai-service.svc.cluster.local:8080/mcp"
  # 客服系统通用Prompt模板
  CUSTOMER_SERVICE_SYSTEM_PROMPT: |
    你是电商平台专业智能客服，需严格遵循以下规则：
    1. 仅基于提供的上下文回答，禁止编造信息；
    2. 回答友好、简洁、专业，避免技术术语；
    3. 上下文不足时，引导用户提供信息或转接人工；
    4. 禁止泄露用户隐私和系统内部信息。
---
# 各服务专属配置
apiVersion: v1
kind: ConfigMap
metadata:
  name: ai-gateway-config
  namespace: ai-service
data:
  SERVER_PORT: "8080"
  # 限流配置
  RATE_LIMIT_ENABLED: "true"
  RATE_LIMIT_MAX_REQUESTS_PER_SECOND: "1000"
  RATE_LIMIT_BURST: "2000"
---
apiVersion: v1
kind: ConfigMap
metadata:
  name: ai-orchestration-config
  namespace: ai-service
data:
  SERVER_PORT: "8080"
  # Spring AI 配置
  SPRING_AI_OPENAI_CHAT_OPTIONS_MODEL: "deepseek-chat"
  SPRING_AI_OPENAI_CHAT_OPTIONS_TEMPERATURE: "0.3"
  SPRING_AI_OPENAI_CHAT_OPTIONS_MAX_TOKENS: "2000"
  # MCP客户端配置
  MCP_CLIENT_TIMEOUT: "5000ms"
  # RAG检索配置
  RAG_TOP_K: "3"
  RAG_SIMILARITY_THRESHOLD: "0.7"
---
apiVersion: v1
kind: ConfigMap
metadata:
  name: ai-rag-config
  namespace: ai-service
data:
  SERVER_PORT: "8080"
  # 文档分块配置
  DOCUMENT_CHUNK_SIZE: "1000"
  DOCUMENT_CHUNK_OVERLAP: "200"
  # 向量索引配置
  VECTOR_INDEX_TYPE: "HNSW"
  VECTOR_DIMENSION: "1536"
---
apiVersion: v1
kind: ConfigMap
metadata:
  name: ai-mcp-gateway-config
  namespace: ai-service
data:
  SERVER_PORT: "8080"
  # MCP服务注册配置
  MCP_REGISTRY_REFRESH_INTERVAL: "30000ms"
  MCP_CALL_TIMEOUT: "5000ms"
```
执行命令：`kubectl apply -f 03-configmap.yaml`

---

### 4. 敏感信息 Secret（生产绝对禁止硬编码）
存放API Key、数据库密码、Token密钥等敏感信息，**禁止将明文写入Git仓库**，通过命令行或Vault创建。
#### 4.1 先执行命令创建Secret（推荐）
```bash
# 创建AI服务通用密钥
kubectl create secret generic ai-service-secret \
  --namespace ai-service \
  --from-literal=SPRING_AI_OPENAI_API_KEY="你的大模型API Key" \
  --from-literal=VECTOR_STORE_REDIS_PASSWORD="向量库Redis密码" \
  --from-literal=MCP_GATEWAY_API_KEY="MCP网关鉴权Key"

# 创建HTTPS证书Secret（Ingress使用，必须替换为你的有效证书）
kubectl create secret tls ai-service-tls \
  --namespace ai-service \
  --cert=./your-domain.crt \
  --key=./your-domain.key
```

#### 4.2 备用YAML方式（仅用于测试，生产禁止明文）
```yaml
# 04-secret.yaml（测试用，生产必须用上述命令创建，base64编码）
apiVersion: v1
kind: Secret
metadata:
  name: ai-service-secret
  namespace: ai-service
type: Opaque
data:
  # 以下值需替换为base64编码后的内容，生产禁止明文提交
  SPRING_AI_OPENAI_API_KEY: "eW91ci1vcGVuYWktYXBpLWtleQ=="
  VECTOR_STORE_REDIS_PASSWORD: "eW91ci1yZWRpcy1wYXNzd29yZA=="
  MCP_GATEWAY_API_KEY: "eW91ci1tY3AtYXBpLWtleQ=="
```

---

### 5. 核心服务 Deployment（生产级配置）
包含4个核心微服务，严格遵循最佳实践：非root用户运行、Pod反亲和性、三节点容灾、完善的健康探针、资源隔离、优雅关闭。

#### 5.1 AI 对话网关（流量入口，无状态，可水平扩容）
```yaml
# 05-deployment-ai-gateway.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ai-gateway
  namespace: ai-service
  labels:
    app: ai-gateway
    app.kubernetes.io/name: ai-gateway
    app.kubernetes.io/part-of: ai-service
    app.kubernetes.io/version: 1.0.0
    environment: prod
spec:
  replicas: 3
  selector:
    matchLabels:
      app: ai-gateway
  # 滚动更新策略：零停机发布
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  template:
    metadata:
      labels:
        app: ai-gateway
        app.kubernetes.io/name: ai-gateway
        app.kubernetes.io/part-of: ai-service
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "8080"
        prometheus.io/path: "/actuator/prometheus"
    spec:
      # 服务账户
      serviceAccountName: ai-service-account
      # 非root用户运行，安全加固
      securityContext:
        runAsUser: 1000
        runAsGroup: 1000
        fsGroup: 1000
        runAsNonRoot: true
        seccompProfile:
          type: RuntimeDefault
      # Pod反亲和性：强制分散在不同节点，避免单节点故障
      affinity:
        podAntiAffinity:
          requiredDuringSchedulingIgnoredDuringExecution:
            - labelSelector:
                matchExpressions:
                  - key: app
                    operator: In
                    values: ["ai-gateway"]
              topologyKey: "kubernetes.io/hostname"
      # 节点亲和性：优先调度到专属节点池（可选）
      nodeAffinity:
        preferredDuringSchedulingIgnoredDuringExecution:
          - weight: 100
            preference:
              matchExpressions:
                - key: node-role
                  operator: In
                  values: ["ai-service"]
      containers:
        - name: ai-gateway
          # 替换为你的私有仓库镜像地址
          image: registry.example.com/ai/ai-gateway:v1.0.0
          imagePullPolicy: Always
          ports:
            - name: http
              containerPort: 8080
              protocol: TCP
          # 资源限制：生产必须配置，避免资源争抢
          resources:
            requests:
              cpu: 2
              memory: 2Gi
            limits:
              cpu: 4
              memory: 4Gi
          # 环境变量注入配置
          envFrom:
            - configMapRef:
                name: ai-service-common-config
            - configMapRef:
                name: ai-gateway-config
            - secretRef:
                name: ai-service-secret
          # 安全加固：禁止特权模式，只读文件系统
          securityContext:
            allowPrivilegeEscalation: false
            privileged: false
            readOnlyRootFilesystem: true
            capabilities:
              drop: ["ALL"]
          # 健康检查探针：Spring AI服务启动慢，必须配置startup探针
          startupProbe:
            httpGet:
              path: /actuator/health/liveness
              port: http
            failureThreshold: 30
            periodSeconds: 10
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: http
            periodSeconds: 10
            timeoutSeconds: 5
            failureThreshold: 3
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: http
            periodSeconds: 5
            timeoutSeconds: 3
            failureThreshold: 2
          # 优雅关闭：等待流量切走再停止
          lifecycle:
            preStop:
              exec:
                command: ["sh", "-c", "sleep 10"]
          # 临时目录挂载（只读文件系统需要）
          volumeMounts:
            - name: tmp
              mountPath: /tmp
      # 临时卷
      volumes:
        - name: tmp
          emptyDir: {}
      # 优雅关闭宽限期
      terminationGracePeriodSeconds: 30
      # 镜像拉取密钥（私有仓库需要）
      imagePullSecrets:
        - name: registry-auth
```

#### 5.2 AI 对话编排服务（Spring AI 核心，业务逻辑收口）
```yaml
# 05-deployment-ai-orchestration.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ai-orchestration
  namespace: ai-service
  labels:
    app: ai-orchestration
    app.kubernetes.io/name: ai-orchestration
    app.kubernetes.io/part-of: ai-service
    app.kubernetes.io/version: 1.0.0
    environment: prod
spec:
  replicas: 3
  selector:
    matchLabels:
      app: ai-orchestration
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  template:
    metadata:
      labels:
        app: ai-orchestration
        app.kubernetes.io/name: ai-orchestration
        app.kubernetes.io/part-of: ai-service
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "8080"
        prometheus.io/path: "/actuator/prometheus"
    spec:
      serviceAccountName: ai-service-account
      securityContext:
        runAsUser: 1000
        runAsGroup: 1000
        fsGroup: 1000
        runAsNonRoot: true
        seccompProfile:
          type: RuntimeDefault
      affinity:
        podAntiAffinity:
          requiredDuringSchedulingIgnoredDuringExecution:
            - labelSelector:
                matchExpressions:
                  - key: app
                    operator: In
                    values: ["ai-orchestration"]
              topologyKey: "kubernetes.io/hostname"
      containers:
        - name: ai-orchestration
          image: registry.example.com/ai/ai-orchestration:v1.0.0
          imagePullPolicy: Always
          ports:
            - name: http
              containerPort: 8080
              protocol: TCP
          # 核心服务资源配置更高，适配长耗时LLM调用
          resources:
            requests:
              cpu: 4
              memory: 8Gi
            limits:
              cpu: 8
              memory: 16Gi
          envFrom:
            - configMapRef:
                name: ai-service-common-config
            - configMapRef:
                name: ai-orchestration-config
            - secretRef:
                name: ai-service-secret
          securityContext:
            allowPrivilegeEscalation: false
            privileged: false
            readOnlyRootFilesystem: true
            capabilities:
              drop: ["ALL"]
          # 启动探针阈值更高，适配Spring AI服务启动加载时间
          startupProbe:
            httpGet:
              path: /actuator/health/liveness
              port: http
            failureThreshold: 40
            periodSeconds: 10
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: http
            periodSeconds: 10
            timeoutSeconds: 5
            failureThreshold: 3
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: http
            periodSeconds: 5
            timeoutSeconds: 3
            failureThreshold: 2
          lifecycle:
            preStop:
              exec:
                command: ["sh", "-c", "sleep 15"]
          volumeMounts:
            - name: tmp
              mountPath: /tmp
      volumes:
        - name: tmp
          emptyDir: {}
      terminationGracePeriodSeconds: 40
      imagePullSecrets:
        - name: registry-auth
```

#### 5.3 AI RAG 管理服务 + 5.4 AI MCP 网关服务
上述两个服务的Deployment配置结构完全一致，仅需修改`name`、`image`、`resources`、`configMapRef`对应的配置即可，此处不再重复粘贴。

执行命令：`kubectl apply -f 05-deployment-*.yaml`

---

### 6. 服务发现 Service
内部服务使用ClusterIP，仅AI网关暴露给Ingress，禁止内部服务直接暴露公网。
```yaml
# 06-service.yaml
apiVersion: v1
kind: Service
metadata:
  name: ai-gateway
  namespace: ai-service
  labels:
    app: ai-gateway
    app.kubernetes.io/part-of: ai-service
spec:
  type: ClusterIP
  selector:
    app: ai-gateway
  ports:
    - name: http
      port: 8080
      targetPort: http
---
apiVersion: v1
kind: Service
metadata:
  name: ai-orchestration
  namespace: ai-service
  labels:
    app: ai-orchestration
    app.kubernetes.io/part-of: ai-service
spec:
  type: ClusterIP
  selector:
    app: ai-orchestration
  ports:
    - name: http
      port: 8080
      targetPort: http
---
apiVersion: v1
kind: Service
metadata:
  name: ai-rag
  namespace: ai-service
  labels:
    app: ai-rag
    app.kubernetes.io/part-of: ai-service
spec:
  type: ClusterIP
  selector:
    app: ai-rag
  ports:
    - name: http
      port: 8080
      targetPort: http
---
apiVersion: v1
kind: Service
metadata:
  name: ai-mcp-gateway
  namespace: ai-service
  labels:
    app: ai-mcp-gateway
    app.kubernetes.io/part-of: ai-service
spec:
  type: ClusterIP
  selector:
    app: ai-mcp-gateway
  ports:
    - name: http
      port: 8080
      targetPort: http
```
执行命令：`kubectl apply -f 06-service.yaml`

---

### 7. 流量入口 Ingress（生产必配HTTPS）
基于Nginx Ingress Controller，配置HTTPS、限流、TLS重定向、会话保持，生产环境禁止使用HTTP明文传输。
```yaml
# 07-ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: ai-service-ingress
  namespace: ai-service
  annotations:
    # 强制HTTPS重定向
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/force-ssl-redirect: "true"
    # 限流配置：每秒1000次请求，突发2000次
    nginx.ingress.kubernetes.io/limit-rps: "1000"
    nginx.ingress.kubernetes.io/limit-burst: "2000"
    # 上传文件大小限制（RAG文档上传用）
    nginx.ingress.kubernetes.io/proxy-body-size: "50m"
    # 代理超时配置（适配LLM长耗时调用）
    nginx.ingress.kubernetes.io/proxy-connect-timeout: "10"
    nginx.ingress.kubernetes.io/proxy-read-timeout: "60"
    nginx.ingress.kubernetes.io/proxy-send-timeout: "60"
    # 开启Gzip压缩
    nginx.ingress.kubernetes.io/gzip-enabled: "true"
    nginx.ingress.kubernetes.io/gzip-types: "application/json text/plain"
spec:
  # 替换为你的HTTPS证书Secret
  tls:
    - hosts:
        - ai-customer-service.example.com
      secretName: ai-service-tls
  ingressClassName: nginx
  rules:
    - host: ai-customer-service.example.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: ai-gateway
                port:
                  number: 8080
```
执行命令：`kubectl apply -f 07-ingress.yaml`

---

### 8. 弹性伸缩配置
#### 8.1 原生 HPA 基础弹性（必配）
基于CPU/内存使用率的水平伸缩，保证常规流量波动下的服务稳定性。
```yaml
# 08-hpa.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: ai-gateway-hpa
  namespace: ai-service
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: ai-gateway
  minReplicas: 2
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
    - type: Resource
      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: 80
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: ai-orchestration-hpa
  namespace: ai-service
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: ai-orchestration
  minReplicas: 2
  maxReplicas: 20
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
    - type: Resource
      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: 80
```
执行命令：`kubectl apply -f 08-hpa.yaml`

#### 8.2 KEDA 进阶弹性（生产推荐）
基于QPS、Kafka消息堆积、LLM调用延迟等业务指标弹性伸缩，适配大促等突发流量场景，需提前部署KEDA组件。
```yaml
# 08-keda-scaledobject.yaml
apiVersion: keda.sh/v1alpha1
kind: ScaledObject
metadata:
  name: ai-orchestration-scaledobject
  namespace: ai-service
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: ai-orchestration
  minReplicaCount: 2
  maxReplicaCount: 30
  pollingInterval: 5
  cooldownPeriod: 60
  # 弹性触发规则
  triggers:
    # 基于Prometheus QPS指标触发
    - type: prometheus
      metadata:
        serverAddress: http://prometheus.middleware.svc.cluster.local:9090
        metricName: http_server_requests_seconds_count
        query: sum(rate(http_server_requests_seconds_count{service="ai-orchestration"}[1m]))
        threshold: "100" # 单Pod每秒处理100个请求，超过则扩容
    # 基于CPU使用率兜底
    - type: cpu
      metricType: Utilization
      metadata:
        value: "70"
```

---

### 9. 可观测性配置（Prometheus 自动发现）
让Prometheus自动抓取服务指标，实现监控告警，需提前部署Prometheus Operator。
```yaml
# 09-servicemonitor.yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: ai-service-monitor
  namespace: ai-service
  labels:
    release: prometheus
spec:
  selector:
    matchLabels:
      app.kubernetes.io/part-of: ai-service
  namespaceSelector:
    matchNames:
      - ai-service
  endpoints:
    - port: http
      path: /actuator/prometheus
      interval: 15s
      scrapeTimeout: 5s
```
执行命令：`kubectl apply -f 09-servicemonitor.yaml`

---

## 三、生产级灰度发布（Argo Rollouts 金丝雀发布）
Spring AI服务迭代频率极高（Prompt优化、RAG策略调整、模型切换），原生滚动更新无法满足灰度验证需求，推荐使用Argo Rollouts实现金丝雀发布，先切10%流量验证，无问题再全量放量，支持一键回滚。

### 1. 前置条件：集群已部署 Argo Rollouts
```bash
# 安装Argo Rollouts
kubectl create namespace argo-rollouts
kubectl apply -n argo-rollouts -f https://github.com/argoproj/argo-rollouts/releases/latest/download/install.yaml
```

### 2. 金丝雀发布 Rollout 配置（替换原Deployment）
```yaml
# 10-argo-rollout.yaml
apiVersion: argoproj.io/v1alpha1
kind: Rollout
metadata:
  name: ai-orchestration
  namespace: ai-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: ai-orchestration
  template:
    # 此处粘贴原Deployment中的template.spec完整内容，和之前一致
    metadata:
      labels:
        app: ai-orchestration
        app.kubernetes.io/name: ai-orchestration
        app.kubernetes.io/part-of: ai-service
    spec:
      # 原Deployment中的spec内容，此处省略，完全复用之前的配置
  # 金丝雀发布策略
  strategy:
    canary:
      # 流量切分基于Nginx Ingress
      trafficRouting:
        nginx:
          stableIngress: ai-service-ingress
      # 灰度放量步骤
      steps:
        # 第一步：切10%流量，暂停等待人工确认
        - setWeight: 10
        - pause: {}
        # 第二步：切30%流量，自动暂停5分钟
        - setWeight: 30
        - pause:
            duration: 5m
        # 第三步：切50%流量，自动暂停3分钟
        - setWeight: 50
        - pause:
            duration: 3m
        # 第四步：全量100%流量
        - setWeight: 100
      # 回滚配置：新版本健康检查失败自动回滚
      maxUnavailable: 0
      maxSurge: 1
      progressDeadlineSeconds: 600
```

### 3. 发布与回滚操作
```bash
# 发布新版本：更新镜像
kubectl argo rollouts set image ai-orchestration ai-orchestration=registry.example.com/ai/ai-orchestration:v1.1.0 -n ai-service

# 查看发布状态
kubectl argo rollouts get rollout ai-orchestration -n ai-service

# 人工确认继续放量
kubectl argo rollouts promote ai-orchestration -n ai-service

# 一键回滚到上一个稳定版本
kubectl argo rollouts undo ai-orchestration -n ai-service

# 全量发布完成
kubectl argo rollouts promote --full ai-orchestration -n ai-service
```

---

## 四、一键部署 Helm Chart 模板
将上述所有资源封装为Helm Chart，支持多环境一键部署，可直接托管到Harbor仓库，适配CI/CD流水线。

### Chart 目录结构
```
ai-service-chart/
├── Chart.yaml          # Chart元信息
├── values.yaml         # 全局配置值（可按需修改）
├── templates/          # 模板文件
│   ├── _helpers.tpl    # 模板辅助函数
│   ├── namespace.yaml
│   ├── rbac.yaml
│   ├── networkpolicy.yaml
│   ├── configmap.yaml
│   ├── secret.yaml
│   ├── deployment.yaml
│   ├── service.yaml
│   ├── ingress.yaml
│   ├── hpa.yaml
│   └── servicemonitor.yaml
└── charts/             # 依赖Chart
```

### 核心文件示例
#### 1. Chart.yaml
```yaml
apiVersion: v2
name: ai-service
description: 企业级Spring AI智能客服服务Helm Chart
type: application
version: 1.0.0
appVersion: "1.0.0"
keywords:
  - spring-ai
  - ai-customer-service
  - mcp
  - rag
  - kubernetes
home: https://example.com/ai-service
maintainers:
  - name: AI Team
    email: ai-team@example.com
```

#### 2. values.yaml（核心配置入口）
```yaml
# 全局配置
global:
  namespace: ai-service
  environment: prod
  imageRegistry: registry.example.com
  imagePullSecrets:
    - name: registry-auth

# 服务通用配置
common:
  serviceAccount: ai-service-account
  tz: Asia/Shanghai
  llm:
    baseUrl: http://llm-proxy.middleware.svc.cluster.local:8080
    model: deepseek-chat
    temperature: 0.3
  vectorStore:
    host: redis-vector.middleware.svc.cluster.local
    port: 6379
  mcp:
    gatewayUrl: http://ai-mcp-gateway.ai-service.svc.cluster.local:8080/mcp

# 各服务配置
services:
  ai-gateway:
    replicaCount: 3
    image:
      repository: ai/ai-gateway
      tag: v1.0.0
    resources:
      requests:
        cpu: 2
        memory: 2Gi
      limits:
        cpu: 4
        memory: 4Gi
    hpa:
      minReplicas: 2
      maxReplicas: 10
      targetCPUUtilization: 70
      targetMemoryUtilization: 80

  ai-orchestration:
    replicaCount: 3
    image:
      repository: ai/ai-orchestration
      tag: v1.0.0
    resources:
      requests:
        cpu: 4
        memory: 8Gi
      limits:
        cpu: 8
        memory: 16Gi
    hpa:
      minReplicas: 2
      maxReplicas: 20
      targetCPUUtilization: 70
      targetMemoryUtilization: 80

  ai-rag:
    replicaCount: 2
    image:
      repository: ai/ai-rag
      tag: v1.0.0
    resources:
      requests:
        cpu: 2
        memory: 4Gi
      limits:
        cpu: 4
        memory: 8Gi

  ai-mcp-gateway:
    replicaCount: 2
    image:
      repository: ai/ai-mcp-gateway
      tag: v1.0.0
    resources:
      requests:
        cpu: 2
        memory: 2Gi
      limits:
        cpu: 4
        memory: 4Gi

# Ingress配置
ingress:
  enabled: true
  className: nginx
  host: ai-customer-service.example.com
  tls:
    enabled: true
    secretName: ai-service-tls

# 监控配置
monitoring:
  enabled: true
  prometheus:
    scrape: true
    serviceMonitor:
      enabled: true
      interval: 15s

# 网络策略
networkPolicy:
  enabled: true
  defaultDeny: true
```

### 3. 部署命令
```bash
# 开发环境部署
helm install ai-service ./ai-service-chart -n ai-service --create-namespace -f values-dev.yaml

# 生产环境部署
helm install ai-service ./ai-service-chart -n ai-service --create-namespace -f values-prod.yaml

# 升级版本
helm upgrade ai-service ./ai-service-chart -n ai-service -f values-prod.yaml

# 卸载
helm uninstall ai-service -n ai-service
```

---

## 五、部署验证与运维指南
### 1. 部署完成验证
```bash
# 查看命名空间下所有资源
kubectl get all -n ai-service

# 查看Pod运行状态，确保所有Pod都是Running状态
kubectl get pods -n ai-service -o wide

# 查看服务日志，验证启动无报错
kubectl logs -f deployment/ai-orchestration -n ai-service

# 验证接口连通性
curl -X POST https://ai-customer-service.example.com/api/v1/customer-service/chat \
  -H "Content-Type: application/json" \
  -d '{"query":"你好，请问如何查询订单？"}'
```

### 2. 生产环境运维核心注意事项
1.  **配置管理**：所有环境配置通过Helm values.yaml管理，禁止直接修改K8s资源；敏感信息通过Vault/Secret管理，禁止提交到Git。
2.  **版本管理**：镜像标签必须使用语义化版本（v1.0.0），禁止使用latest标签，确保发布可追溯、可回滚。
3.  **监控告警**：核心告警指标：Pod不可用、接口错误率>1%、P99延迟>5s、LLM调用失败、CPU/内存使用率>90%。
4.  **成本管控**：通过HPA/KEDA实现按需弹性伸缩，低峰期自动缩容，避免资源浪费；通过LLM代理网关管控Token用量，设置配额告警。
5.  **容灾备份**：向量库、对话日志定期备份；多可用区部署，Pod分散在不同节点/可用区，避免单机房故障。

---
