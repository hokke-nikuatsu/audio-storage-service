# チェックの規約

機械で確かめられる規約を、プッシュの前と CI で確かめる仕組みの規約。判断の理由は [ADR-0005](../adr/0005-run-same-checks-locally-and-in-ci.md) にある。

## 準備

ツールは [mise](https://mise.jdx.dev/) で入れ、Git のフックは lefthook で入れる。

```sh
mise install
mise exec -- lefthook install
```

## 流れ

- プッシュの前に、lefthook が [lefthook.yml](../../lefthook.yml) の `pre-push` のチェックを行う
- PR では、CI が同じチェックを `lefthook run pre-push` で行う
- CI の結果は `ci-status` ジョブにまとめる。main のルールセットは `ci-status` だけを必須にし、最新の main を取り込んだ状態で通っていなければマージできないようにする

## チェックの追加

- チェックのコマンドは `lefthook.yml` にだけ書く。手元で確かめられないチェックだけを [CI](../../.github/workflows/ci.yml) のジョブにし、`ci-status` の `needs` に加える
- 既存のツールがあればそれを使い、バージョンを [.mise.toml](../../.mise.toml) で固定して、`lefthook.yml` からは `mise exec --` を付けて呼ぶ
- 既存のツールがない独自のルールは `scripts/checks/` に置く。簡単なもの（著者、タイトル、ブランチ名など）はシェルで、ドキュメントの検査は Python の標準ライブラリだけで書き、YAML のように標準ライブラリで読めない書式は、`.mise.toml` で固定した既存のツール（yq）に読ませる
- 失敗したときのメッセージには、根拠のドキュメントを示す。既存のツールは出力を変えられないので、`lefthook.yml` の `fail_text` で、次の節を示す

## 既存のツールが失敗したとき

- actionlint、shellcheck: 出力の指摘に従って直す。shellcheck の指摘は、出力にある `SC` で始まる番号のページに理由と直し方がある
- lychee: 出力に示されたリンク先のファイルや見出しを直す。見出しを変えたなら、その見出しへのリンクも新しい見出しに合わせる
- gitleaks: 混入した値を、まず発行元で無効にする。そのうえで、値を含むコミットを作り直して履歴から消す。後のコミットで消すだけでは履歴に残り、チェックも通らない。誤検知なら、出力の `Fingerprint` を `.gitleaksignore` に足す
