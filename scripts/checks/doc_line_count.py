#!/usr/bin/env python3
"""ADR と design.md の行数を確かめる。目安を超えたら警告し、上限を超えたら失敗する。"""

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]

# (対象の glob, 目安, 上限, 根拠のドキュメント)。行数は根拠のドキュメントに合わせる
RULES = [
    ("docs/adr/[0-9][0-9][0-9][0-9]-*.md", 60, 80, "docs/adr/README.md"),
    (".kiro/specs/*/design.md", 200, 250, ".kiro/settings/templates/specs/design.md（ADR-0002）"),
]


def main() -> int:
    failed = False
    for pattern, target, limit, basis in RULES:
        for path in sorted(ROOT.glob(pattern)):
            lines = len(path.read_text(encoding="utf-8").splitlines())
            name = path.relative_to(ROOT)
            if lines > limit:
                failed = True
                print(f"エラー: {name} は{lines}行で、上限の{limit}行を超えている。根拠: {basis}")
            elif lines > target:
                print(f"警告: {name} は{lines}行で、目安の{target}行を超えている。根拠: {basis}")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
