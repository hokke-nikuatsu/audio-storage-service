# audio-storage-service

ユーザーと練習フレーズに紐づく音声ファイルを受け取り、変換して保存し、取り出せるバックエンドサービス。

## 起動方法

Docker（Compose を含む）だけで起動できる。JDK や ffmpeg を手元に入れる必要はない。

```sh
docker compose up --build
```

`http://localhost/` で受け付ける。ホストの 80 番が使われているときは、`compose.yaml` の `ports` のホスト側の番号を変える。初回はイメージの中で Gradle のビルドが走るため、数分かかる。

DB のファイルと録音のファイルは、ボリュームに置くので、コンテナを作り直しても残る。止めるときは `docker compose down`、データも消すときは `docker compose down -v` を使う。

Compose を使わないときは、次の手順で起動する。

```sh
docker build -t audio-storage-service .
docker run --rm -p 127.0.0.1:80:8080 -v audio-storage-data:/data audio-storage-service
```

## curl の例

起動したときに、有効な ID として、ユーザー ID とフレーズ ID のそれぞれ 1〜3 が登録されている。送受信する音声は m4a（MP4 のコンテナに AAC）で、サーバーは WAV に変換して保存する。試すための m4a は `testdata/audio/eagle-has-landed.m4a` にあり、次のようにコピーすると、課題の手順のとおりに試せる。

```sh
cp testdata/audio/eagle-has-landed.m4a ./test_audio_file_1.m4a
```

音声ファイルを保存する。初めての保存は 201、同じユーザーとフレーズへの2回目からの保存は、前の録音を置き換えて 204 を返す。

```sh
curl --request POST 'http://localhost/audio/user/1/phrase/1' \
  --form 'audio_file=@"./test_audio_file_1.m4a"'
```

音声ファイルを取得する。保存した録音を m4a に変換して 200 で返す。

```sh
curl --request GET 'http://localhost/audio/user/1/phrase/1/m4a' -o './test_response_file_1_1.m4a'
```

ID やファイルの検証で拒んだときは、ステータスと、原因を表す `code` を持つ JSON を返す（例: 登録されていないユーザー ID なら 404 と `USER_NOT_FOUND`）。

## ドキュメント

ドキュメントの一覧と役割は、[ドキュメントの置き場](docs/conventions/workflow.md#ドキュメントの置き場)にある。
