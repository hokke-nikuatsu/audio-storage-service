# アーキテクチャ

今動いているシステムの姿を書く。いつ更新するかは [開発の進め方の規約](conventions/workflow.md#ドキュメントの置き場) にある。

## データベース

SQLite に3つのテーブルを持つ。スキーマは [V1__create_tables.sql](../adapter/src/main/resources/db/migration/V1__create_tables.sql) にある。

```mermaid
erDiagram
    users ||--o{ recordings : records
    phrases ||--o{ recordings : records
    users {
        INTEGER id PK
    }
    phrases {
        INTEGER id PK
    }
    recordings {
        INTEGER user_id PK,FK
        INTEGER phrase_id PK,FK
        TEXT file_path UK "保存先のルートからの相対パス"
    }
```

- `users` と `phrases` は有効な ID のテーブル、`recordings` はユーザー ID とフレーズ ID の組と、保存した録音のファイルのパスの対応のテーブル
- 列はすべて NOT NULL。`recordings` の主キーを `(user_id, phrase_id)` にして、組ごとに1行だけ持つ
- 初期データとして、`users` と `phrases` にそれぞれ ID 1〜3 を入れる。初期データは [R__insert_valid_ids.sql](../adapter/src/main/resources/db/seed/R__insert_valid_ids.sql) にあり、すでにある ID は飛ばして入れる

## 録音の保存

`POST /audio/user/{user_id}/phrase/{phrase_id}` は、`audio_file` のフィールドの m4a を WAV に変換してディスクに書き、DB のパスを切り替える。

```mermaid
sequenceDiagram
    participant C as web<br>RecordingController
    participant U as application<br>SaveRecordingUseCase
    participant DB as adapter DB<br>JooqRecordingRepository
    participant FS as adapter ディスク<br>LocalRecordingStorage
    participant FF as adapter ffmpeg<br>FfmpegAudioConverter
    C->>U: save
    U->>DB: userExists, phraseExists
    U->>FS: allocate
    U->>FF: toStorageFormat（ffprobe で確かめてから WAV に変換）
    alt Rejected、TooLarge、または例外
        U->>FS: delete 新しいファイル
        U-->>C: Rejected または例外
    else Converted
        U->>DB: replacePath（古いパスを読んで upsert）
        alt 例外
            U->>FS: delete 新しいファイル
            U-->>C: 例外
        else 古いパスがある
            U->>FS: delete 古いファイル
            U-->>C: Replaced（204）
        else 古いパスがない
            U-->>C: Created（201）
        end
    end
```

- 録音は、保存先のルート（`{data-dir}/recordings`）の下の `{user_id}/{phrase_id}/{UUID}.wav` に、保存のたびに新しい名前で書く。DB には、ルートからの相対パスを書く
- DB のパスを切り替えるまで、DB は古いファイルを指したままなので、途中で失敗しても既存の録音を取得できる
- `replacePath` は1つのトランザクションで行い、SQLite への接続は1本にする。古いファイルを消せなくても、警告のログを残して成功を返す
- ID が見つからないときは、`allocate` の前に `Rejected` を返して終わる（図では省略）

## 録音の取得

`GET /audio/user/{user_id}/phrase/{phrase_id}/{audio_format}` は、保存した WAV を m4a に変換して返す。

```mermaid
sequenceDiagram
    participant C as web<br>RecordingController
    participant U as application<br>GetRecordingUseCase
    participant DB as adapter DB<br>JooqRecordingRepository
    participant FS as adapter ディスク<br>LocalRecordingStorage
    participant FF as adapter ffmpeg<br>FfmpegAudioConverter
    C->>U: get
    U->>DB: userExists, phraseExists
    Note over U: audio_format が m4a かを確かめる
    U->>DB: findPath
    U->>FS: resolve
    U->>FF: toTransferFormat
    U-->>C: Found（200、audio/mp4）
```

- どの段階でも当てはまらなければ、その場で `Rejected` を返す。変換の失敗は例外で上がる。後始末はない

## 検証とエラーの分岐

上から順に確かめ、最初に当てはまったものを返す。エラーの本文は `ErrorHandler` が、RFC 9457 の Problem Details に `code` を足した JSON で作る。

```mermaid
flowchart TD
    Req[要求] --> Size{multipart の要求なら、<br>ファイルが 10MB 以下}
    Size -- いいえ --> E413a[413 AUDIO_FILE_TOO_LARGE]
    Size -- はい --> Route{パスとメソッドが<br>エンドポイントに合う}
    Route -- いいえ --> Spring[Spring の 404、405 など<br>code なし]
    Route -- POST --> File{multipart で、audio_file が<br>あり、空ではない}
    File -- いいえ --> E400a[400 MISSING_AUDIO_FILE]
    File -- はい --> PUser{ユーザー ID が登録されている}
    PUser -- いいえ --> E404u[404 USER_NOT_FOUND]
    PUser -- はい --> PPhrase{フレーズ ID が登録されている}
    PPhrase -- いいえ --> E404p[404 PHRASE_NOT_FOUND]
    PPhrase -- はい --> Probe{ffprobe で MP4 と AAC を確かめられる}
    Probe -- いいえ --> E415[415 UNSUPPORTED_AUDIO]
    Probe -- はい --> Wav{変換後の WAV が 200MB 未満}
    Wav -- いいえ --> E413b[413 AUDIO_FILE_TOO_LARGE]
    Wav -- はい --> Saved[201 新規、204 置き換え]
    Route -- GET --> GUser{ユーザー ID が登録されている}
    GUser -- いいえ --> E404u
    GUser -- はい --> GPhrase{フレーズ ID が登録されている}
    GPhrase -- いいえ --> E404p
    GPhrase -- はい --> Format{audio_format が m4a}
    Format -- いいえ --> E400f[400 UNSUPPORTED_FORMAT]
    Format -- はい --> Found{組に録音がある}
    Found -- いいえ --> E404r[404 RECORDING_NOT_FOUND]
    Found -- はい --> Ok[200 m4a]
```

- Spring は、どのエンドポイントで受けるかを決める前に multipart の本文を読む。そのため、10MB を超えるファイルを送ると、パスやメソッドが誤っていても 413 になる
- ID は、先頭が 0 ではない数字だけの並びで `Long` に収まるものだけを読み、それ以外は登録されていない ID と同じに扱う
- 図のどこでも、変換、ディスク、DB の失敗や ffmpeg のタイムアウトのような想定外の例外が上がれば、500 の `INTERNAL_ERROR` を返し、原因はログにだけ書く
- 存在しないパス（404）や、エンドポイントが受け付けないメソッド（405）は、Spring が既定のエラーの本文で返す。本文に `code` は付かない
