# AGENTS Knowledge Base & Best Practices

This document systemically organizes practical experiences, technical points, and best practices across five professional roles within the `renda-java` project.

---

## 📅 Version Change Record

| Version | Date | Author | Description |
| :--- | :--- | :--- | :--- |
| v1.0.0 | 2026-03-15 | Renda Zhang | Initial consolidation of project-wide role-based practices. |

---

## 📑 Table of Contents

1. [Development Role](#1-development-role)
2. [Testing Role](#2-testing-role)
3. [Documentation Role](#3-documentation-role)
4. [Review and Release Role](#4-review-and-release-role)
5. [Execution Planning Role](#5-execution-planning-role)

---

## 1. Development Role

### Background & Objectives
Transition from a flat structure to a scalable multi-module Maven architecture to support high-performance Java 21 applications and distributed system experiments.

### Key Practices
- **Multi-Module Design**: Decoupling code by responsibility (`algorithm`, `backend-interview`, `playground`).
- **Modern Java (JDK 21)**: Leveraging Virtual Threads and Record types for performance and clarity.
- **Design Patterns**: Utilizing Strategy and Factory patterns to unify algorithm problem execution.
- **Dependency Management**: Using a Parent POM with `dependencyManagement` to avoid version conflicts.

### Typical Cases
- **Re-architecture (2026-03-15)**: Migrated `leetcode-training` to a structured `algorithm` module. Unified problem execution via a `ProblemStrategy` interface, allowing for new sources (Luogu, SwordOffer) without modifying core logic.
- **XML Parsing Fix**: Identified that bare `&` in `<name>` tags causes `ModelParseException`. Corrected to `&amp;` to ensure CI stability.

### Reusable Assets
- [Parent POM Template](pom.xml)
- [Algorithm Factory Framework](algorithm/src/main/java/com/renda/algorithm/core/)

### Improvement Suggestions
- Consider adopting a Hexagonal Architecture for the `backend-interview` module to better isolate domain logic from infrastructure.

---

## 2. Testing Role

### Background & Objectives
Ensure high code quality and reliability through automated unit, integration, and performance testing, targeting 80%+ coverage.

### Key Practices
- **Unit Testing**: Standardizing on JUnit 5 and AssertJ for readable and robust assertions.
- **Integration Testing**: Using Testcontainers to spin up real MySQL/Redis/Kafka instances during the build.
- **Performance Benchmarking**: Integrating JMH to detect performance regressions in algorithm implementations.
- **CI Integration**: Automating all tests via GitHub Actions on every Pull Request.

### Typical Cases
- **CDC Integration Test**: Using Debezium with Testcontainers to verify Outbox patterns without needing a persistent development database.
- **JMH Benchmarks**: Quantifying the performance difference between iterative and recursive solutions for Dynamic Programming problems.

### Reusable Assets
- [CI Workflow Configuration](.github/workflows/ci.yml)
- Test Base Classes for Testcontainers.

### Improvement Suggestions
- Implement Mutation Testing (e.g., PITest) to evaluate the effectiveness of the existing test suite.

---

## 3. Documentation Role

### Background & Objectives
Maintain a professional, multi-language, and searchable knowledge base that synchronizes code changes with theoretical insights.

### Key Practices
- **Dual Format Support**: Using Markdown for quick updates and AsciiDoc for structured, complex technical manuals.
- **Path Indexing**: Creating a centralized `LEARNING_PATH.md` to guide users through tiered learning (Basics -> Advanced -> Source -> Interview).
- **Automated Publishing**: Deploying docs to GitHub Pages via CI/CD pipelines.
- **Bilingual Documentation**: Providing both Chinese and English descriptions to cater to a wider audience.

### Typical Cases
- **Readme Overhaul**: Replaced the legacy README with a professional portal featuring dynamic badges and modular indexes.
- **Audit Reports**: Standardizing quality audit reports for document hierarchy and link integrity.

### Reusable Assets
- [README Template](README.md)
- [Learning Path Index](docs/LEARNING_PATH.md)

### Improvement Suggestions
- Integrate a search engine (e.g., Algolia) into the published documentation site for better discoverability.

---

## 4. Review and Release Role

### Background & Objectives
Standardize the release process and ensure that only high-quality, audited code reaches the `main` branch.

### Key Practices
- **Pre-commit Audits**: Using `pre-commit` hooks for linting (`yamllint`, `Checkstyle`) and format checks.
- **Conventional Commits**: Enforcing `type: description` format for commits to facilitate automatic CHANGELOG generation.
- **Semantic Versioning (SemVer)**: Following `MAJOR.MINOR.PATCH` rules for all project releases.
- **Release Note Standard**: Using a consistent template to communicate features, bug fixes, and breaking changes.

### Typical Cases
- **Architecture Release (v1.0.0)**: Executed a major version bump when transitioning to the multi-module structure, explicitly documenting breaking package name changes.
- **Audit-led Fixes**: Using automated audit tools to identify and fix 70+ broken links before a major release.

### Reusable Assets
- [Release Note Template](RELEASE_NOTE_TEMPLATE.md)
- [Checkstyle Configuration](checkstyle.xml)

### Improvement Suggestions
- Automate the generation of `CHANGELOG.md` using `conventional-changelog` tools in the CI pipeline.

---

## 5. Execution Planning Role

### Background & Objectives
Decompose complex architectural requirements into actionable tasks and ensure timely delivery through milestone tracking.

### Key Practices
- **Requirement Decomposition**: Breaking down high-level visions (e.g., "One-stop Lab") into atomic tasks (Maven setup, pattern implementation, CI config).
- **Prioritization**: Focusing on foundational architecture first (H1-H2) before adding specific content.
- **Milestone Tracking**: Using `VISION.md` to map out phases (Foundation -> Expansion -> Ecosystem).
- **Risk Identification**: Identifying early on that `pre-commit` hook failures or XML errors could block the CI pipeline.

### Typical Cases
- **Roadmap Planning**: Successfully executed Phase 1 of the vision by prioritizing the core factory framework over individual algorithm updates.
- **Resource Coordination**: Managing the transition of 80+ files while maintaining git history through careful `git mv` operations.

### Reusable Assets
- [Project Vision & Roadmap](VISION.md)
- Task Checklists for major re-architecting tasks.

### Improvement Suggestions
- Introduce a more formal Sprint-based planning cycle for the `playground` module to accelerate experimental feature development.
