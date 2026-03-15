# Algorithm Module

## 职责 (Responsibilities)
按来源分类的算法与数据结构练习，包含 LeetCode、洛谷、剑指 Offer。

## 技术栈 (Tech Stack)
- Java 21
- JUnit 5 + AssertJ (Testing)
- JMH (Performance Benchmarking)

## 快速启动 (Quick Start)
```bash
mvn clean test -pl algorithm
```

## 扩展指南 (Extension Guide)
1. 在 `com.renda.algorithm.[source]` 下创建新题目。
2. 实现 `AlgorithmProblem` 接口。
3. 在 `ProblemRegistry` 中注册新题目。
