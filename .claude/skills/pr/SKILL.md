---
name: pr
description: 今のブランチの PR を、PR テンプレートに沿った本文で作る。引数に update を付けると、既存の PR の本文を今の差分に合わせて書き直す。
disable-model-invocation: true
argument-hint: "[update]"
allowed-tools: Read, Glob, Grep, Bash(git status:*), Bash(git log:*), Bash(git diff:*), Bash(git rev-parse:*), Bash(git branch:*), Bash(git fetch:*), Bash(gh api user:*), Bash(gh repo view:*), Bash(gh auth status:*), Bash(gh auth switch:*), Bash(gh pr view:*), Bash(gh pr create:*), Bash(gh pr edit:*)
---

# PR を作る・本文を更新する

PR の規約は `docs/conventions/workflow.md` の「PR」、本文の見出しは `.github/pull_request_template.md` を正とし、このファイルに規約や見出しを複製しない。

## 前提

- **PR を作る前と本文を書き換える前に、必ず案を人に見せて承認を得る。** 承認なしに `gh pr create` や `gh pr edit` を実行しない
- 本文は差分とコミットから分かる事実だけで書く。差分にない変更や、人が判断していないことを書かない
- テンプレートの見出しは消さず、順番も変えない。書くことがない見出しは「なし」とする
- AI が本文を書いた場合は、本文の末尾に `🤖 Generated with [Claude Code](https://claude.com/claude-code)` を付ける

## PR を作る（引数なし）

1. `docs/conventions/workflow.md` の「PR」と `.github/pull_request_template.md` を読む
2. 今のブランチを確かめる。main の上にいる、未コミットの変更がある、またはリモートにプッシュされていない場合は、止まって人に伝える
3. `git fetch` してから、`origin/main` との差分（`git diff origin/main...HEAD`）とコミット（`git log origin/main..HEAD`）を読む。ブランチは main から切る規約なので、作るときの向き先は main にする
4. タイトルと本文の案を書く
   - タイトルは日本語で、「〜する」と動詞で言い切る
   - 「参照」のリンクは、`git rev-parse HEAD` のコミット SHA で固定した完全な URL（`https://github.com/<owner>/<repo>/blob/<SHA>/<path>`）にする。行を指すときは `?plain=1#L<行>` を付ける
   - 差分が規約の目安（1,000行未満、実装は300行程度）を超える場合は、案と一緒に人に伝える
   - 「AI レビュー結果」には `.review/<ブランチ名>.md`（`/` は `-` に置き換える）の内容を入れる。ファイルがなければ「未実施」と書き、結果に書かれた HEAD が今の HEAD と違えば、結果が古いことを書き添える。どちらの場合も人に伝える
   - 「人の判断」は、人から判断を伝えられていなければ表を空のまま残す
5. 案を人に見せ、承認を得る
6. GitHub のアカウントを合わせる
   - 今のアカウント（`gh api user --jq .login`）と、リポジトリのオーナー（`gh repo view --json owner --jq .owner.login`）を記録する
   - 2つが違えば `gh auth switch --user <オーナー>` で切り替える。オーナーのアカウントでログインしていなければ、止まって人に伝える
7. `gh pr create --base main --assignee @me --title <タイトル> --body-file -` で作る
8. `gh pr view --json number,url,author,assignees` で、作成者とアサインがオーナーになっていることを確かめる
9. 手順6で切り替えた場合は、記録したアカウントに `gh auth switch` で戻す
10. PR の URL と、確かめた結果を人に伝える

## 本文を更新する（`update`）

1. `gh pr view --json number,title,body,baseRefName` で今の本文と、PR の向き先のブランチを読む
2. 規約とテンプレートを読み、`git fetch` してから、PR の向き先との差分（`git diff origin/<向き先>...HEAD`）とコミット（`git log origin/<向き先>..HEAD`）を読む。main と決め打ちしない
3. 今の本文と差分、`.review/<ブランチ名>.md` を突き合わせ、食い違う箇所（変更内容の漏れ、古くなった記述、古い AI レビュー結果、行数など）を洗い出す。人から判断を伝えられていれば「人の判断」に書き写す。「参照」のリンクは、今の HEAD のコミット SHA で固定した URL に置き換える
4. 直した本文の案を、変える箇所が分かる形で人に見せ、承認を得る。食い違いがなければ、そう伝えて終わる
5. 「PR を作る」の手順6と同じくアカウントを合わせ、`gh pr edit <番号> --body-file -` で更新する
6. 更新後の本文を `gh pr view` で読み、反映されたことを確かめる。切り替えたアカウントは戻す

## してはいけないこと

- コミット、プッシュ、マージをしない
- PR のタイトルや本文を、人の承認なしに作成・変更しない
