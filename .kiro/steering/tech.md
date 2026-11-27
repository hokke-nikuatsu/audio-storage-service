# Technology Stack

## Core Technologies

- **Language**: Kotlin 2.3 系
- **Framework**: Spring Boot 4.1。リクエストは Spring MVC（ブロッキング）で処理し、WebFlux やコルーチンは使わない
- **Runtime**: Java 25

## Development Standards

### Code Quality

- 書き方は ktlint で、質（関数の長さ、分岐の複雑さ、命名規則など）は detekt 2.0 で確かめる。どちらも Gradle のプラグインとしてバージョンを固定する
- Kotlin のコンパイラの警告はエラーにする（`allWarningsAsErrors`）

## Development Environment

### Required Tools

- ビルドは Gradle 9 系の Kotlin DSL（`build.gradle.kts`）で書き、Gradle Wrapper で実行する
- JDK のバージョンは Gradle の toolchain で `build.gradle.kts` に書き、手元に入れずに Gradle に取ってこさせる。mise では管理しない

## Key Technical Decisions

判断の理由は ADR にある。

- 言語とフレームワーク: [ADR-0007](../../docs/adr/0007-use-kotlin-and-spring-boot-mvc.md)
- ビルドツールと JDK の固定: [ADR-0008](../../docs/adr/0008-build-with-gradle-and-pin-jdk-by-toolchain.md)
- 静的解析: [ADR-0009](../../docs/adr/0009-check-kotlin-with-ktlint-and-detekt.md)
