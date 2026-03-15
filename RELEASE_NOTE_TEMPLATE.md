# Release Note Template

## [v1.0.0] - 2026-03-15

### 🚀 新特性 (New Features)
- [algorithm] 初始化算法实验室框架，支持 LeetCode, 洛谷, 剑指 Offer。
- [backend-interview] 建立后端面试实验室，集成 Spring Boot 3.2 与 Testcontainers。
- [docs] 建立了“必学→进阶→源码→面试”递进式学习路径索引。

### 🐞 修复 (Bug Fixes)
- [algorithm] 修正了 LC 322 动态规划的空间复杂度说明。
- [docs] 统一了全书标题层级规范。

### 📝 面试题新增清单 (New Interview Questions)
- [Java] Java 21 虚拟线程 (Virtual Threads) 核心原理。
- [Architecture] 分布式架构落地方法论。

### ⚠️ 兼容性说明 (Breaking Changes)
- 重构了 Maven 模块结构，由单一模块升级为多模块架构。
- 包名由 `com.renda.leetcode` 迁移至 `com.renda.algorithm.leetcode`。

---

## 📈 版本号说明 (SemVer)
- **MAJOR**: 重大架构调整或不兼容的 API 变更。
- **MINOR**: 向下兼容的新特性增加。
- **PATCH**: 向下兼容的 Bug 修复。
