# AGENTS.md

このリポジトリで作業する AI エージェント向けの入口。言語と、作業の前に読むものを示す。作業の規約の本文は、案内した先に書いてある。

## 言語

- 応答は日本語で書く。英語のエラーメッセージなどを引用するときは、原文のまま引用したうえで日本語で説明する
- ドキュメント、コードコメント、コミットメッセージ、PR は日本語で書く
- コードの識別子は英語で書く
- cc-sdd の spec は、見出しをテンプレートのまま英語にし、本文を日本語で書く

## 作業の前に読むもの

それぞれのドキュメントに何が書いてあるかは、[docs/conventions/workflow.md](docs/conventions/workflow.md) の「ドキュメントの置き場」にある。

| ドキュメント | 読むタイミング |
|---|---|
| [docs/conventions/workflow.md](docs/conventions/workflow.md) | すべての作業の前 |
| `.kiro/steering/` | コードを読み書きする作業の前 |
| `.kiro/specs/<feature>/` | その機能の spec や実装の作業の前 |
| [docs/adr/](docs/adr/README.md) | 判断の理由を確かめるとき |
