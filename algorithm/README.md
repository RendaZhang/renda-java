# Algorithm Module

## 职责 (Responsibilities)
按来源分类的算法与数据结构练习，包含 LeetCode、洛谷、剑指 Offer。

## 技术栈 (Tech Stack)
- Java 21
- JUnit 5 + AssertJ (Testing)
- JMH (Performance Benchmarking)

## 快速启动 (Quick Start)
### 1. 运行单元测试 (Unit Tests)
```bash
mvn clean test -pl algorithm
```

### 2. 运行性能基准测试 (JMH Benchmarks)
```bash
# 构建整个项目以生成基准测试代码
mvn clean install -DskipTests
# 运行 JMH 基准测试 (通过 exec-maven-plugin 或直接运行编译后的 JAR)
mvn exec:java -pl algorithm -Dexec.mainClass="org.openjdk.jmh.Main"
```

### 3. 运行算法实验室主程序
```bash
mvn exec:java -pl algorithm -Dexec.mainClass="com.renda.algorithm.Main"
```

主程序启动后会提供交互式菜单，选择题目后执行。

也可以通过参数直接运行：

```bash
# 运行全部
mvn exec:java -pl algorithm -Dexec.mainClass="com.renda.algorithm.Main" -Dexec.args="all"

# 运行单题（可用 ProblemId 或 Source:ProblemId）
mvn exec:java -pl algorithm -Dexec.mainClass="com.renda.algorithm.Main" -Dexec.args="LC1"
```

## 扩展指南 (Extension Guide)
1. 在 `com.renda.algorithm.[source]` 下创建新题目。
2. 实现 `AlgorithmProblem` 接口。
3. 提供无参构造方法，并放在 `com.renda.algorithm.leetcode` / `com.renda.algorithm.luogu` / `com.renda.algorithm.swordoffer` 包及其子包下，启动时会自动扫描并注册。
