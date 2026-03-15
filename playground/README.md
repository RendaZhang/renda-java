# Experimental Playground Module

## 职责 (Responsibilities)
可扩展实验模块，用于存放微服务模板、响应式编程示例等。

## 技术栈 (Tech Stack)
- Java 21
- 可按子模块自定义

## 快速启动 (Quick Start)
```bash
mvn clean install -pl playground
```

## 扩展指南 (Extension Guide)
### 1. 生成新模块 (Generate Module)
使用根目录下的脚本一键生成：
```bash
bash scripts/generate-module.sh [your-module-name]
```

### 2. 验证与注册 (Verify & Register)
- 检查 `playground/pom.xml` 中是否已自动添加 `<module>[your-module-name]</module>`。
- 在新模块下编写代码并运行测试：`mvn test -pl playground/[your-module-name]`。
- 遵循统一的 `com.renda.playground.[module]` 包命名规范。
