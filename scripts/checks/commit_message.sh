#!/usr/bin/env bash
# main にないコミットのメッセージが、コミットの規約の書式に沿っているかを確かめる。
# 要約が動詞で言い切っているかは機械で判定できないので、レビューで見る。
set -euo pipefail

SUBJECT_PATTERN='^(add|fix|change|refactor|docs): [^ ]'

# プロセス置換では git rev-list の失敗で止まらないので、いったん変数に受ける。
# main を取り込むマージコミットは Git が書くメッセージなので対象にしない
commits=$(git rev-list --no-merges origin/main..HEAD)

failed=0
for sha in $commits; do
  message=$(git log -1 --format=%B "$sha")
  subject=$(sed -n 1p <<< "$message")
  second_line=$(sed -n 2p <<< "$message")
  if [[ ! "$subject" =~ $SUBJECT_PATTERN ]]; then
    echo "エラー: コミット ${sha:0:7} の1行目（${subject}）が「{prefix}: {要約}」の形でないか、prefix が規約にない。根拠: docs/conventions/workflow.md の「コミット」"
    failed=1
  fi
  if [[ -n "$second_line" ]]; then
    echo "エラー: コミット ${sha:0:7} の1行目のあとが空行でない。根拠: docs/conventions/workflow.md の「コミット」"
    failed=1
  fi
done

exit "$failed"
