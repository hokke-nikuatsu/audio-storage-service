# ADR 一覧

技術的な判断の理由を残す。1つの判断につき1ファイルで、[MADR](https://adr.github.io/madr/) 4.0.0 の minimal 版に、不可逆度・見直し条件・反映先を加えた[テンプレート](template.md)で書く。

- ADR は判断した時点の記録で、書き換えない。判断を変えるときは新しい ADR を作り、古い ADR の status を `superseded by ADR-NNNN` にする
- 現時点の正（技術スタック・設計・規約）は steering と spec に置く。ADR はその理由だけを持つ

| 番号 | タイトル | 状態 | 日付 |
|---|---|---|---|
| [0001](0001-spec-driven-ai-development-with-human-judgment.md) | cc-sdd による仕様駆動で、AI が実装・レビューし、人が判断する開発プロセスにする | accepted | 2026-11-23 |
