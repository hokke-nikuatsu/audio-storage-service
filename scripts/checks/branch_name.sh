#!/usr/bin/env bash
# ブランチ名が、ブランチの規約の書式に沿っているかを確かめる。
set -euo pipefail

NAME_PATTERN='^(feature|bugfix|test|docs|chore)/[a-z0-9]+(-[a-z0-9]+)*$'
MAX_LENGTH=40

# CI の pull_request では detached HEAD になるので、PR のブランチ名を GitHub Actions の環境変数から取る
branch="${GITHUB_HEAD_REF:-$(git branch --show-current)}"

failed=0
if [[ ! "$branch" =~ $NAME_PATTERN ]]; then
  echo "エラー: ブランチ名（${branch}）が「{prefix}/{英語のケバブケース}」の形でないか、prefix が規約にない。根拠: docs/conventions/workflow.md の「ブランチ」"
  failed=1
fi
if (( ${#branch} > MAX_LENGTH )); then
  echo "エラー: ブランチ名（${branch}）が ${MAX_LENGTH} 文字を超えている。根拠: docs/conventions/workflow.md の「ブランチ」"
  failed=1
fi

exit "$failed"
