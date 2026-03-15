# Renda Java: 一站式 Java 学习与面试知识库与代码实验室

[![Build Status](https://img.shields.io/github/actions/workflow/status/RendaZhang/renda-java/ci.yml?branch=main)](https://github.com/RendaZhang/renda-java/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Java Version](https://img.shields.io/badge/Java-21-blue.svg)](https://www.oracle.com/java/technologies/downloads/)
[![Coverage](https://img.shields.io/badge/Coverage-85%25-green.svg)](#)
[![Algorithm Progress](https://img.shields.io/badge/LeetCode-200%2B-brightgreen.svg)](algorithm/README.md)

这是一个面向 **Java 学习与面试的一站式知识库与代码实验室**。本项目不仅包含了深度的技术笔记，还通过多模块 Maven 工程提供了可运行的算法实战、后端核心示例以及云原生实验场。

---

## 🗺️ 学习路径索引 | Learning Roadmap
我们为您设计了递进式的学习路径，点击下方链接进入索引页：
👉 **[查看学习路径索引 (必学→进阶→源码→面试)](docs/LEARNING_PATH.md)**

---

## 📂 项目模块说明 | Directory Structure

```bash
renda-java/
├── algorithm/                  # 算法实验室 (LeetCode, 洛谷, 剑指 Offer)
├── backend-interview/          # 后端面试实验室 (JVM, 并发, Spring, 云原生)
├── playground/                 # 实验场 (微服务模板, 响应式编程等)
├── docs/                       # 核心文档与笔记 (Markdown + AsciiDoc)
├── docker-compose.yml          # 一键启动开发环境 (MySQL, Redis, Kafka)
├── VISION.md                   # 长期演进路线与贡献指南
└── README.md                   # 项目门户
```

---

## 🚀 快速启动 | Quick Start

### 1. 启动本地开发环境
```bash
docker-compose up -d
```

### 2. 构建并运行算法示例
```bash
mvn clean install
cd algorithm
mvn exec:java -Dexec.mainClass="com.renda.algorithm.Main"
```

### 3. 运行后端集成测试 (需 Docker 环境)
```bash
mvn test -pl backend-interview
```

---

## 🎯 核心特性 | Core Features

### 🧩 算法实验室 (Algorithm Lab)
- **多源分类**：按 LeetCode、洛谷、剑指 Offer 分包管理。
- **设计模式**：采用“抽象工厂+策略模式”统一题目启动入口。
- **三位一体**：每个题目包含 `题目描述`、`多种解法`、`单元测试` 及 `JMH 性能基准`。

### 🛡️ 后端面试实验室 (Backend Lab)
- **源码级示例**：深入 JVM 内存模型、JUC 并发、Spring 核心机制。
- **云原生就绪**：集成 Kubernetes Client、Micrometer、Testcontainers。
- **中心化配置**：支持 Profile 级联与外部配置仓库。

### ⚙️ 自动化与质量保障
- **GitHub Actions**：全量测试、集成测试、文档自动发布。
- **质量阈值**：集成 SpotBugs、PMD、Checkstyle。
- **一键脚手架**：提供 Maven Archetype 模板，一键生成新子模块。

---

## 🤝 贡献与演进 | Vision & Contributing
请参考 **[VISION.md](VISION.md)** 了解我们的长期愿景、演进路线及社区贡献规范。

---

## 📧 联系作者 | Contact
- **GitHub**: [RendaZhang](https://github.com/RendaZhang)
- **Project URL**: [https://github.com/RendaZhang/renda-java](https://github.com/RendaZhang/renda-java)

---
*Generated with ❤️ by Renda Zhang. 一站式 Java 进阶之选。*
