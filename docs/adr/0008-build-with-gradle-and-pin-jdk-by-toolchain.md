---
status: accepted
date: 2026-11-27
irreversibility: 中
---

# ADR-0008: ビルドは Gradle の Kotlin DSL で書き、JDK は Gradle の toolchain で固定する

## 背景と課題

Kotlin と Spring Boot のサービス（ADR-0007）をビルドするツールと、ビルドに使う JDK をそろえる方法を決める必要がある。手元のマシンと Docker の中で、同じ JDK でビルドできるようにしたい。

## 検討した選択肢

### Gradle（Kotlin DSL）

- 良い点: ビルドの設定も Kotlin で書け、IDE の補完が効く。Kotlin と Spring の公式のガイドの多くが Gradle を前提にしている。toolchain でビルドに使う JDK のバージョンを設定に書け、手元になければ自動で取ってこられる
- 悪い点: 設定の自由度が高い分、書き方が揺れやすい。最初のビルドが遅い

### Maven

- 良い点: XML で宣言するだけなので、書き方が揺れない
- 悪い点: Kotlin のコンパイラプラグイン（Spring 用の `allopen` など）の設定が冗長になる

### JDK を mise で固定する

- 良い点: チェック用のツールと同じ仕組みでバージョンをそろえられる
- 悪い点: mise は手元と CI でチェック用のツールをそろえるために入れたもので（ADR-0005）、Docker の中のビルドでは使わない。Docker の中では別の方法で JDK を決めることになり、固定する場所が2つになる

## 決定

「Gradle（Kotlin DSL）」を選び、JDK は Gradle の toolchain で固定する。ビルドの設定を Kotlin で書け、JDK のバージョンを `build.gradle.kts` の1か所に書けば、手元でも Docker の中でも同じ JDK でビルドできるため。

## 影響

- 良い点: Gradle Wrapper と toolchain により、手元に Gradle と JDK を入れなくてもビルドできる
- 受け入れたトレードオフ: 最初のビルドで Gradle と JDK のダウンロードに時間がかかる。設定の書き方は、規約とレビューでそろえる必要がある
- 本番運用なら: 依存関係のバージョンを version catalog にまとめ、Renovate などで定期的に更新する

## 見直し条件

手元や CI で、toolchain が JDK を自動でダウンロードできずにビルドが失敗するようになったら、JDK を前もって入れておき、自動のダウンロードを切る

## 反映先

- `.kiro/steering/tech.md`
