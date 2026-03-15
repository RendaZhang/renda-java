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

## 扩展指南 (Extension Guide)
1. 在 `com.renda.algorithm.[source]` 下创建新题目。
2. 实现 `AlgorithmProblem` 接口。
3. 在 `ProblemRegistry` 中注册新题目。
