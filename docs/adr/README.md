# ADR 一覧

技術的な判断の理由を残す。1つの判断につき1ファイルで、[MADR](https://adr.github.io/madr/) 4.0.0 の minimal 版に、不可逆度・見直し条件・反映先を加えた[テンプレート](template.md)で書く。

- ADR は判断した時点の記録で、書き換えない。判断を変えるときは新しい ADR を作り、古い ADR の status だけを変える
  - 判断を丸ごと変えるとき: 古い ADR を `superseded by ADR-NNNN` にする
  - 判断の一部だけを変えるとき: 古い ADR を `amended by ADR-NNNN` にし、新しい ADR の front matter の `amends` に古い ADR を書く。古い ADR のほかの決定は有効なまま
- 1つの ADR には1つの判断だけを書く
- 長さは60行以内を目安とし、80行を上限とする
- 現時点の正は、作業の進め方の規約（`docs/conventions/`）、コードに関する規約（steering）、仕様（spec）に置く。ADR はその理由だけを持つ
- 一覧の表は手で書かず、`mise exec -- python3 scripts/checks/doc_consistency.py --fix` で各 ADR の front matter と H1 から作る

<!-- adr-index:begin -->
| 番号 | タイトル | 状態 | 日付 |
|---|---|---|---|
| [0001](0001-spec-driven-ai-development-with-human-judgment.md) | cc-sdd による仕様駆動で、AI が実装・レビューし、人が判断する開発プロセスにする | amended by ADR-0004 | 2026-11-23 |
| [0002](0002-tune-cc-sdd-for-human-readable-specs.md) | cc-sdd はテンプレートを主にチューニングし、design.md を人が読める長さと構成に保つ | amended by ADR-0004, ADR-0006 | 2026-11-23 |
| [0003](0003-separate-entry-points-from-conventions.md) | 入口のファイルは案内だけにし、規約は作業の進め方とコードで置き場を分ける | accepted | 2026-11-23 |
| [0004](0004-human-approves-requirements-design-and-tasks.md) | cc-sdd の spec は、要件・設計・タスクの3つを人が承認する | accepted | 2026-11-23 |
| [0005](0005-run-same-checks-locally-and-in-ci.md) | チェックは手元のフックと CI で同じコマンドを走らせ、CI の結果を1つのジョブにまとめる | accepted | 2026-11-26 |
| [0006](0006-override-cc-sdd-skills-only-where-they-conflict.md) | cc-sdd の SKILL.md は、リポジトリの規約と直接ぶつかる箇所に限り1行単位で書き換える | accepted | 2026-11-27 |
| [0007](0007-use-kotlin-and-spring-boot-mvc.md) | サービスは Kotlin と Spring Boot で作り、リクエストは Spring MVC で処理する | accepted | 2026-11-27 |
| [0008](0008-build-with-gradle-and-pin-jdk-by-toolchain.md) | ビルドは Gradle の Kotlin DSL で書き、JDK は Gradle の toolchain で固定する | accepted | 2026-11-27 |
| [0009](0009-check-kotlin-with-ktlint-and-detekt.md) | Kotlin のコードは ktlint と detekt 2.0 で確かめ、コンパイラの警告をエラーにする | accepted | 2026-11-27 |
| [0010](0010-separate-core-from-io-by-ports-and-adapters.md) | 業務の手順と外の世界をポートとアダプタで分ける | accepted | 2026-11-27 |
| [0011](0011-enforce-layer-dependencies-with-gradle-modules.md) | 層の依存の向きは、層ごとの Gradle のモジュールで強制する | accepted | 2026-11-27 |
| [0012](0012-transfer-m4a-and-store-wav.md) | 送受信は m4a（AAC）、保存は WAV（PCM 16 ビット）にする | accepted | 2026-11-27 |
| [0013](0013-convert-audio-by-running-ffmpeg.md) | 音声の変換は ffmpeg のコマンドを直接起動して行い、形式は ffprobe で確かめる | accepted | 2026-11-27 |
| [0014](0014-name-each-saved-file-uniquely.md) | 録音のファイルは保存のたびに一意な名前で書き、DB を切り替えてから古いファイルを消す | accepted | 2026-11-27 |
| [0015](0015-use-sqlite-embedded-in-service.md) | データベースはサービスに組み込む SQLite にする | accepted | 2026-11-27 |
| [0016](0016-access-database-with-jooq.md) | データベースへのアクセスは、スキーマから生成した jOOQ のコードで行う | accepted | 2026-11-27 |
| [0017](0017-migrate-schema-with-flyway.md) | スキーマは Flyway の SQL のマイグレーションで管理し、初期データは別の場所に置く | accepted | 2026-11-27 |
<!-- adr-index:end -->
