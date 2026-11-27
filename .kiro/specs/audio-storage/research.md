# Research & Design Decisions

## Summary
- **Feature**: `audio-storage`
- **Discovery Scope**: New Feature（コードのない状態から作る。技術選定は ADR-0007〜ADR-0020 で済んでいる）
- **Key Findings**:
  - 置き換えの順番（ADR-0014）を守るには、DB の「古いパスの読み出しと切り替え」を1つのトランザクションにし、SQLite への接続を1本にすれば足りる
  - ffmpeg に利用者のファイルを読ませるので、入力の形式を固定しないと、プレイリストの形式を使った外部への読み込みを許してしまう
  - 上限を超えたアップロードに 413 を返すには、Spring の設定だけでなく Tomcat の読み捨ての上限も考える必要がある

## Research Log

Spring Boot 4.1 が管理するバージョンは Flyway 12.4.0、jOOQ 3.21.9、sqlite-jdbc 3.53.4.0、HikariCP 7.0.2。

### Flyway と jOOQ を Spring Boot 4 で使う
- **Context**: マイグレーションと初期データを起動時に流し、jOOQ のトランザクションを使う（ADR-0016、ADR-0017）
- **Sources Consulted**: [Spring Boot: data initialization](https://docs.spring.io/spring-boot/how-to/data-initialization.html)、[Modularizing Spring Boot](https://spring.io/blog/2025/10/28/modularizing-spring-boot)、[JooqAutoConfiguration](https://github.com/spring-projects/spring-boot/blob/4.1.x/module/spring-boot-jooq/src/main/java/org/springframework/boot/jooq/autoconfigure/JooqAutoConfiguration.java)、[Flyway: SQLite](https://documentation.red-gate.com/fd/sqlite-277579331.html)、[Flyway: repeatable migrations](https://documentation.red-gate.com/fd/repeatable-migrations-273973335.html)
- **Findings**:
  - SQLite への対応は `flyway-core` に入っている。Boot 4 では、Flyway と jOOQ の自動設定にそれぞれの starter が要る
  - jOOQ の自動設定は `SpringTransactionProvider` を用意するので、`DSLContext.transactionResult {}` は Spring のトランザクションとして動く。外側に `@Transactional` があると savepoint になる
  - `spring.flyway.locations` には複数の場所を書ける。`R__` は、すべての `V__` の後に流れる
  - SQLite では、マイグレーションの中でトランザクションを制御する文を使えない
- **Implications**: `bootstrap` に2つの starter を入れる。`replacePath` は外側に `@Transactional` を置かず、jOOQ のトランザクションだけで囲む。初期データは `db/seed/R__insert_valid_ids.sql` にする

### SQLite から jOOQ のコードを生成する
- **Context**: マイグレーションを流した SQLite からコードを生成する（ADR-0016）
- **Sources Consulted**: [jOOQ support matrix](https://www.jooq.org/download/support-matrix)、[jOOQ codegen-gradle](https://www.jooq.org/doc/latest/manual/code-generation/codegen-gradle/)、[Komga の build.gradle.kts](https://github.com/gotson/komga/blob/master/komga/build.gradle.kts)、[flyway#4107](https://github.com/flyway/flyway/issues/4107)
- **Findings**:
  - SQLite は jOOQ のオープンソース版で使える
  - SQL のファイルを直接読む DDLDatabase は、H2 に流してから生成するので、SQLite の構文とずれる恐れがある
  - Flyway の Gradle プラグインで `build/` の下に SQLite のファイルを作り、jOOQ の `SQLiteDatabase` から生成する構成が、Gradle 9 で動いている例がある（Komga。ただし jOOQ のプラグインは公式のものではない）
  - Flyway の Gradle プラグインは、configuration cache が無効になりやすい問題が残っている
- **Implications**: 「Flyway の Gradle プラグイン → 公式の jOOQ のプラグイン（`SQLiteDatabase`）」の構成にし、最初の実装のタスクで動くことを確かめる。configuration cache で困ったら、そのタスクだけ対象から外す

### sqlite-jdbc の設定
- **Sources Consulted**: [sqlite-jdbc USAGE](https://github.com/xerial/sqlite-jdbc/blob/master/USAGE.md)、[sqlite-jdbc README](https://github.com/xerial/sqlite-jdbc/blob/master/README.adoc)
- **Findings**: 外部キーの検査は URL のパラメータ（`?foreign_keys=true`）で接続ごとに有効にできる。ネイティブのライブラリは jar に入っていて、glibc の x86_64 と arm64 で動く。接続のプールの大きさを 1 にすることを勧める公式の記述は見つからなかった
- **Implications**: プールの上限を 1 にする（Design Decisions）。2本目の接続を取る処理（`REQUIRES_NEW`）は使わない

### multipart の上限とエラー
- **Sources Consulted**: [MaxUploadSizeExceededException](https://github.com/spring-projects/spring-framework/blob/7.0.x/spring-web/src/main/java/org/springframework/web/multipart/MaxUploadSizeExceededException.java)、[RequestPartMethodArgumentResolver](https://github.com/spring-projects/spring-framework/blob/7.0.x/spring-webmvc/src/main/java/org/springframework/web/servlet/mvc/method/annotation/RequestPartMethodArgumentResolver.java)、[Tomcat 11 HTTP Connector](https://tomcat.apache.org/tomcat-11.0-doc/config/http.html)、[TomcatServerProperties](https://github.com/spring-projects/spring-boot/blob/4.1.x/module/spring-boot-tomcat/src/main/java/org/springframework/boot/tomcat/autoconfigure/TomcatServerProperties.java)
- **Findings**:
  - 上限を超えると `MaxUploadSizeExceededException` になり、Spring の `ResponseEntityExceptionHandler` が 413 にする
  - Tomcat は、断ったアップロードの残りを `maxSwallowSize`（既定 2MB）まで読み捨て、超えると応答を書かずに接続を切る。クライアントには 413 ではなく接続のリセットが見える。Boot では `server.tomcat.max-swallow-size` で変えられる
  - パートが足りないときは `MissingServletRequestPartException`（400）。multipart ではない要求は `MultipartException` になり、`ResponseEntityExceptionHandler` の対象ではないので、そのままでは 500 になる
- **Implications**: `server.tomcat.max-swallow-size` を 100MB に広げる。`MultipartException` を `ErrorHandler` で 400 にする

### ffmpeg に利用者のファイルを読ませる危険
- **Sources Consulted**: [CVE-2016-1898](https://launchpad.net/bugs/cve/2016-1898)、[CVE-2023-6601](https://bugzilla.redhat.com/show_bug.cgi?id=2253172)、[ffmpeg-avi-m3u-xbin](https://github.com/neex/ffmpeg-avi-m3u-xbin)、[ffmpeg-formats](https://ffmpeg.org/ffmpeg-formats.html)、[mov.c（n6.1.1）](https://github.com/FFmpeg/FFmpeg/blob/n6.1.1/libavformat/mov.c)
- **Findings**:
  - HLS のプレイリストを音声や動画に見せかけると、ffmpeg に外部の URL やローカルのファイルを読ませられる。`-protocol_whitelist file` では、ローカルのファイルの読み出しは防げない
  - mov の外部参照を読む設定（`enable_drefs`）は、既定で無効
  - mov の demuxer の名前は `mov,mp4,m4a,3gp,3g2,mj2` で、ffprobe の `format_name` もこの値になる
  - Ubuntu 24.04 の ffmpeg は 6.1.1
  - ボイスメモの「ロスレス」は ALAC になる（Apple の公式の文書は見つからず、コミュニティの情報）
- **Implications**: ffprobe と ffmpeg の入力の形式を `-f mov` で固定し、hls などの demuxer を使わせない。ffprobe では、`format_name` と最初の音声のストリームの `codec_name=aac` を確かめる

### Kotest と Spring
- **Sources Consulted**: [Kotest Spring extension](https://kotest.io/docs/extensions/spring.html)、[kotest#5669](https://github.com/kotest/kotest/issues/5669)
- **Findings**: 最新は 6.2.7 で、Kotlin 2.2 以上で使える。Spring の拡張は `io.kotest:kotest-extensions-spring` として本体と同じバージョンで出ている。Kotest 6 は `io.kotest.provided.ProjectConfig` という名前のクラスだけを設定として読む。Spring Framework 7 で動くかの記述はない
- **Implications**: `bootstrap` のテストに `io.kotest.provided.ProjectConfig` を置く。Kotlin 2.3 と Spring Boot 4.1 で動くかは、最初の実装のタスクで確かめる（ADR-0020 の見直し条件）

## Architecture Pattern Evaluation

パターンは ADR-0010、ADR-0011 で決めている。設計で決めたのは、ポートの切り方だけ。

| Option | Description | Strengths | Risks / Limitations | Notes |
|--------|-------------|-----------|---------------------|-------|
| DB のポートを1つにする | ID の確認と録音のパスを `RecordingRepository` にまとめる | ADR-0019 のとおりポートが3つで済み、フェイクも3つ | ポートの名前と、ユーザーとフレーズの確認が少しずれる | 採用 |
| ID の確認を別のポートにする | `UserRepository`、`PhraseRepository` を分ける | 名前が中身と一致する | 同じ DB にポートとフェイクが2つ増える。ID の確認は存在の確認だけ | 不採用 |

## Design Decisions

### Decision: 生の ID の確認をユースケースで行う
- **Context**: 形式の正しくない ID も「見つからない」として扱う（3.2、3.3）
- **Alternatives Considered**:
  1. `web` で `UserId` に読み替え、失敗したら `web` が 404 を返す
  2. ユースケースが文字列を受け取り、`RecordingKeyResolver` で形式と登録の両方を確かめる
- **Selected Approach**: 2
- **Rationale**: 「見つからない」の判断が1か所にまとまり、形式の誤りも単体テストで網羅できる
- **Trade-offs**: ユースケースの引数が型の弱い文字列になる

### Decision: 業務の結果は sealed の型、外の世界の失敗は例外
- **Context**: 400、404、415 と 500 を分ける
- **Selected Approach**: ユースケースは `Created`、`Replaced`、`Rejected(RecordingError)` などの sealed の型を返し、DB・ディスク・変換の失敗は例外で上げる。`web` は `RecordingError` からステータスへの対応を1か所に持つ
- **Rationale**: 業務の分岐は型で網羅でき、想定外の失敗は1つの例外の処理で 500 にできる
- **Trade-offs**: ユースケースの中で、失敗したときの後始末（新しいファイルの削除）を try と catch で書く必要がある

### Decision: 変換したファイルを最終の置き場所に直接書く
- **Context**: 変換の出力をどこに書き、だれが消すか
- **Alternatives Considered**:
  1. 変換は一時ファイルに書き、ストレージが最終の場所に移す
  2. ストレージが割り当てたパスに、変換が直接書く
- **Selected Approach**: 2。失敗したら、ユースケースが割り当てたパスを消す
- **Rationale**: DB が指すまでそのファイルはどこからも使われないので、途中まで書かれても害がない。移す手順と、一時ファイルの持ち主の取り決めが要らない
- **Trade-offs**: ffprobe で拒むときにも、組のディレクトリが作られる

### Decision: 取得の m4a はメモリに載せて返す
- **Context**: m4a は目次を末尾に置くため、変換は一時ファイルを経由する（ADR-0013）
- **Selected Approach**: `toTransferFormat` は `ByteArray` を返し、一時ファイルは変換の中で消す
- **Rationale**: 上限 10MB の m4a から戻すので、大きくても 10MB 前後に収まる。一時ファイルを応答の後に消す仕組みが要らない
- **Trade-offs**: 同時の取得が多いとメモリを使う。本番運用なら、変換した m4a をストリームで返すか、キャッシュする

### Decision: アダプタを Spring に依存させない
- **Context**: アダプタを Bean にする方法
- **Selected Approach**: アダプタは普通のクラスにし、`bootstrap` の設定クラスで、設定値と jOOQ の `DSLContext` を渡して作る
- **Rationale**: `adapter` の外部のライブラリが jOOQ と SLF4J だけになり、結合テストで Spring を起動せずに作れる
- **Trade-offs**: `bootstrap` の設定クラスに、アダプタごとの組み立てを書く

### Decision: SQLite への接続を1本にする
- **Context**: 同じ組への保存が重なっても、`replacePath` が返す古いパスを取り違えない
- **Alternatives Considered**:
  1. 接続のプールの上限を 1 にし、DB への処理をすべて1列に並べる
  2. WAL と `busy_timeout`、`transaction_mode=IMMEDIATE` を組み合わせ、複数の接続で書き込みを待たせる
- **Selected Approach**: 1
- **Rationale**: 変換は DB の外で行うので、DB の処理は短く、1列に並べても待ちは小さい。設定が1つで済み、ロックの待ちを考えなくてよい
- **Trade-offs**: 2本目の接続を取る処理（`REQUIRES_NEW`）を書くと行き詰まる。本番運用なら、複数の書き込みを扱える DB に移す

### Decision: 成功の応答は 201 と 204
- **Context**: 1.2 は「成功を示す応答」だけを求めている
- **Selected Approach**: 新規は 201、置き換えは 204 にし、本文は返さない。リソースを置く・置き換えるときの HTTP の慣例（RFC 9110 の PUT）に合わせる
- **Trade-offs**: `curl` には何も表示されない。README に `-i` を付けた例を書く

### Decision: 上限は 10MB
- **Context**: 1.6 の上限の値
- **Rationale**: AAC の 128kbps で 1 分は約 1MB。練習フレーズは数秒から十数秒なので、10MB あれば余裕があり、WAV にしても約 100MB に収まる

## Synthesis

- **Generalization**: 保存と取得は、どちらも「ID を確かめて組にする」から始まるので、`RecordingKeyResolver` にまとめた。エラーは `RecordingError` の1つの列挙にし、`web` の対応を1か所にした。変換のポートは、取得するフォーマットを引数に取り、フォーマットが増えても形が変わらないようにした（実装は m4a だけ）
- **Build vs. Adopt**: エラーの本文は Spring の `ProblemDetail`（RFC 9457）を使い、ファイルの有無と大きさの検査は Spring の multipart の仕組みに任せる。変換は ffmpeg に任せる（ADR-0013）
- **Simplification**: 入力のポート、`updated_at` などの列、ヘルスチェックの仕組み（Actuator）、ローカルで `bootRun` するための設定は入れない。起動は Docker を正とし、テストは一時ディレクトリを設定で渡す

## Risks & Mitigations
- 取得の途中で同じ組の保存が古いファイルを消すと、取得が 500 になる — 受け入れる。本番運用なら、古いファイルの削除を遅らせる
- Kotest、detekt 2.0 のアルファ版、Flyway と jOOQ のコード生成の組み合わせが、Kotlin 2.3 と Spring Boot 4.1 と Gradle 9 で動くかが確かめられていない — Gradle のプロジェクトを作る最初のタスクで確かめ、動かなければ ADR の見直し条件に従う
- 古いファイルの削除の前にプロセスが落ちると、ファイルが残る — 受け入れる（ADR-0014）
- ffmpeg が止まらない入力 — 1回の起動に 30 秒のタイムアウトを設け、超えたらプロセスを止めて 500 にする
- ホストとイメージの ffmpeg のバージョンの違い — Docker での手の確認で拾う（ADR-0019）

## References
- ADR-0010〜ADR-0020 — 技術選定とその理由
- [RFC 9457 Problem Details for HTTP APIs](https://www.rfc-editor.org/rfc/rfc9457) — エラーの本文
- [RFC 9110 HTTP Semantics](https://www.rfc-editor.org/rfc/rfc9110) — 201、204、413、415 の意味
