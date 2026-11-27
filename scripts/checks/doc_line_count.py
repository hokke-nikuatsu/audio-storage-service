#!/usr/bin/env python3
"""ADR と design.md の行数を確かめる。目安を超えたら警告し、上限を超えたら失敗する。

design.md は、人がレビューする前半（`# Implementation References` の見出しより上）だけを数える。
見出しがなければ全体を数える。
"""

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]

# (対象の glob, 目安, 上限, 根拠のドキュメント, 前半だけを数えるか)。行数は根拠のドキュメントに合わせる
RULES = [
    ("docs/adr/[0-9][0-9][0-9][0-9]-*.md", 60, 80, "docs/adr/README.md", False),
    (".kiro/specs/*/design.md", 150, 200, ".kiro/settings/templates/specs/design.md（ADR-0021）", True),
]
SEPARATOR = "# Implementation References"


def count_lines(path: Path, review_part_only: bool) -> int:
    lines = path.read_text(encoding="utf-8").splitlines()
    if review_part_only and SEPARATOR in lines:
        return lines.index(SEPARATOR)
    return len(lines)


def main() -> int:
    failed = False
    for pattern, target, limit, basis, review_part_only in RULES:
        for path in sorted(ROOT.glob(pattern)):
            lines = count_lines(path, review_part_only)
            scope = f"の前半（`{SEPARATOR}` より上）" if review_part_only else ""
            name = f"{path.relative_to(ROOT)} {scope}"
            if lines > limit:
                failed = True
                print(f"エラー: {name}は{lines}行で、上限の{limit}行を超えている。根拠: {basis}")
            elif lines > target:
                print(f"警告: {name}は{lines}行で、目安の{target}行を超えている。根拠: {basis}")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
