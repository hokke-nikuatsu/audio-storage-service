---
name: adr
description: 会話で人が決めた技術的な判断を、docs/adr/ に次の番号の ADR として記録し、一覧を更新する。判断を変える場合は古い ADR を置き換え済み、または一部変更済みにする。
disable-model-invocation: true
argument-hint: "<判断の短い説明>"
allowed-tools: Read, Write, Edit, Glob, Bash(ls:*), Bash(date:*)
---

# ADR を記録する

会話で人が決めた判断を ADR として残す。書式は `docs/adr/template.md`、一覧は `docs/adr/README.md` を正とし、このファイルに書式を複製しない。

## 前提

- **人が決めたことだけを書く。** 会話の中に人の決定がなければ、ADR を書かずに「何が未決か」を返して止まる。AI が選択肢を選んで決定として書いてはいけない
- 検討した選択肢と理由は、会話で挙がったものを使う。会話にない選択肢や理由を足すときは、足したことを人に伝える
- 1つの ADR には1つの判断だけを書く。決定に別々に変わりうる判断が複数入りそうなら、ADR を分けるよう人に提案する
- ADR は判断の理由だけを持つ。現時点の構成や規約の説明は規約（`docs/conventions/`）・steering・spec に書き、ADR には書かない

## 手順

1. `docs/adr/template.md` と `docs/adr/README.md` を読む
2. 番号を決める。`docs/adr/` にある `NNNN-*.md` の最大の番号に1を足し、4桁にする（最初は `0001`）
3. ファイル名を `NNNN-<英語のケバブケースのスラッグ>.md` にする（例: `0012-use-flyway-for-migrations.md`）。タイトルは日本語で書く
4. テンプレートを埋める
   - `date` は今日の日付（`date +%Y-%m-%d`）
   - `irreversibility` は、後から変えるときのコストで判断する。判断に迷ったら人に確認する
   - `## 影響` の「本番運用なら」には、今回の実装の範囲を超えて本番ではどうするかを書く。該当しなければ行ごと消す
   - `## 見直し条件` は「規模が大きくなったら」のような曖昧な書き方をせず、観測できる条件で書く
   - 60行以内を目安にする
5. 既存の ADR の判断を変える場合は、古い ADR の front matter の `status` だけを変える。古い ADR の本文は書き換えない
   - 判断を丸ごと変える: `superseded by ADR-NNNN`
   - 判断の一部だけを変える: `amended by ADR-NNNN`。新しい ADR の front matter に `amends: ADR-NNNN` を書き、決定に「どの判断を変え、ほかは有効なまま」かを書く
6. `docs/adr/README.md` の表に1行足す。置き換えた・一部を変えた ADR があれば、その行の状態も更新する
7. 書いた ADR を人に見せ、`## 反映先` に挙げた規約・steering・spec への反映がまだなら、どこに何を書くかを提案する

## してはいけないこと

- コミットしない（コミットは `/commit` で行う）
- 既存の ADR の本文を書き換えない（status の変更だけ許す）
