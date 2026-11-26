#!/usr/bin/env bash
# main にないコミットの著者とコミッターが、個人のアカウントのメールアドレスかを確かめる。
# リポジトリは公開しているので、ほかのアカウントのメールアドレスがコミットに残らないようにする。
set -euo pipefail

# 個人のアカウントのメールアドレス。GitHub の画面で作るコミットは、noreply のアドレスで記録される
ALLOWED_EMAILS=(
  "hokke.nikuatsu126@gmail.com"
  "107485201+hokke-nikuatsu@users.noreply.github.com"
  "noreply@github.com"
)

is_allowed() {
  local email
  for email in "${ALLOWED_EMAILS[@]}"; do
    [[ "$1" == "$email" ]] && return 0
  done
  return 1
}

failed=0
while IFS=$'\t' read -r sha author committer; do
  if ! is_allowed "$author" || ! is_allowed "$committer"; then
    echo "エラー: コミット ${sha} の著者（${author}）かコミッター（${committer}）が、個人のアカウントのメールアドレスではない。根拠: docs/conventions/workflow.md の「コミット」"
    failed=1
  fi
done < <(git log --format='%h%x09%ae%x09%ce' origin/main..HEAD)

exit "$failed"
