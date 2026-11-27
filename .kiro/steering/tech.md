# Technology Stack

## Core Technologies

- **Language**: Kotlin 2.3 系
- **Framework**: Spring Boot 4.1。リクエストは Spring MVC（ブロッキング）で処理し、WebFlux やコルーチンは使わない
- **Runtime**: Java 25
- **Database**: SQLite（ファイル）。サービスのプロセスに組み込み、外部キーの検査は接続ごとに有効にする
- **Database Access**: jOOQ。マイグレーションを流した一時的な SQLite のファイルからコードを生成し、生成したコードと jOOQ への依存は `adapter` にだけ置く

## Audio Formats

- **送受信用**: m4a（MP4 のコンテナに AAC）。POST で受け付け、GET の `{audio_format}` で指定できるのは `m4a` だけ
- **保存用**: WAV（PCM 16 ビット）。サンプリングレートとチャンネル数は元の録音のまま保つ
- **変換**: ffmpeg のコマンドを `ProcessBuilder` で起動し、一時ファイルで受け渡す。変換の前に ffprobe で、コンテナが MP4、コーデックが AAC であることを確かめる
- **保存先**: 録音は `{user_id}/{phrase_id}/{UUID}.wav` に保存のたびに一意な名前で書く。置き換えは「新しいファイルを書く → DB のパスを切り替える → 古いファイルを消す」の順に行い、DB の切り替えが失敗したら新しいファイルを消す。DB には、設定で持つルートのディレクトリからの相対パスを書く

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
- アーキテクチャとモジュールの分け方: `structure.md`
- 音声フォーマット: [ADR-0012](../../docs/adr/0012-transfer-m4a-and-store-wav.md)
- 音声の変換: [ADR-0013](../../docs/adr/0013-convert-audio-by-running-ffmpeg.md)
- 録音のファイルの名前と置き換えの順番: [ADR-0014](../../docs/adr/0014-name-each-saved-file-uniquely.md)
- データベース: [ADR-0015](../../docs/adr/0015-use-sqlite-embedded-in-service.md)
- データベースへのアクセス: [ADR-0016](../../docs/adr/0016-access-database-with-jooq.md)
