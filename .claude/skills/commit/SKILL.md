---
name: commit
description: 今の変更を関心ごとに分け、コミットの規約に沿ったメッセージでコミットする。コミットする前に、分け方とメッセージの案を人に見せて承認を得る。
disable-model-invocation: true
allowed-tools: Read, Bash(git status:*), Bash(git diff:*), Bash(git log:*), Bash(git branch --show-current), Bash(git config --local --get:*), Bash(git restore --staged:*), Bash(git add:*), Bash(git commit:*)
---

# 変更をコミットする

コミットの規約は `docs/conventions/workflow.md` の「コミット」を正とし、このファイルに規約を複製しない。

## 前提

- **コミットする前に、分け方とメッセージの案を人に見せて承認を得る。** 承認なしに `git commit` を実行しない
- 対象は、作業ツリーのすべての変更（ステージ済み、未ステージ、未追跡）にする。コミットしない変更があれば、案にそう書く
- メッセージの「なぜ」は、差分から分かることと、会話で人が述べた理由だけで書く。理由が分からなければ推測で書かず、人に聞く

## 手順

1. `docs/conventions/workflow.md` の「コミット」を読む
2. 前提を確かめ、満たさなければ止まって人に伝える
   - 今のブランチ（`git branch --show-current`）が空（detached HEAD）でも main でもない。main には PR を経由してだけ入れるため
   - `git config --local --get user.name` と `git config --local --get user.email` が設定されている。設定がないと、グローバルの設定の別のアカウントが著者になるため
3. `git status`、`git diff`、`git diff --cached` で変更を読み、未追跡のファイルは Read で読む。`git log -10` で直近のメッセージの書き方を確かめる
4. 関心ごとに分ける
   - 変更の箇所ごとに行った操作を挙げ、それぞれの1行目を書く。1行目が違う操作は、別のコミットにする
   - 1つのファイルに別々の関心ごとが混ざっていたら、行単位では分けず、そのことを案に書いて人に判断を求める
   - 使われる側を先にして、依存の順に並べる
5. 案を人に見せ、承認を得る。コミットごとに、含めるファイルとメッセージの全文を示す
6. 承認された案のとおりにコミットする
   - ステージ済みの変更があれば、先に `git restore --staged .` でステージを空にする
   - コミットごとに `git add <ファイル>` でステージし、`git commit -F -` にメッセージを標準入力で渡す
7. `git log --format='%h %an <%ae> %s' -<コミットの数>` で、著者とメッセージを確かめる。`git status` で残った変更があれば、人に伝える

## してはいけないこと

- プッシュしない
- `--no-verify` でフックを飛ばさない。フックで失敗したら、出力を人に伝えて止まる
- `--amend` や rebase で、既存のコミットを書き換えない
