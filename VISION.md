# Renda Java Vision & Roadmap

## 愿景 (Vision)
打造全球领先的 Java 学习与面试一站式知识库与代码实验室，不仅提供理论知识，更提供可运行、可验证、高性能的代码示例。

## 长期演进路线 (Roadmap)
- **Phase 1: 基础建设 (Completed)**
  - 多模块 Maven 架构初始化
  - 算法实验室核心框架 (Strategy + Factory)
  - 文档体系重构
- **Phase 2: 深度扩展 (In Progress)**
  - 集成 Testcontainers 进行真实的数据库与中间件测试
  - 引入 JMH 性能基准测试
  - 完善 Spring Boot 3.x 源码级示例
- **Phase 3: 云原生与生态 (Planned)**
  - 接入 Nacos/Consul 配置中心
  - 实现基于 Kubernetes Client 的自动化运维示例
  - 提供标准的微服务脚手架

## 社区贡献规范 (Contribution Guidelines)
1. 采用 Fork & PR 模式。
2. 所有新增代码必须包含 JUnit 5 单元测试。
3. 提交 PR 前请运行 `mvn clean verify` 确保通过 Checkstyle 与质量阈值。
4. 算法题目请标注时间与空间复杂度。

## 许可证 (License)
本项目遵循 MIT 许可证。
