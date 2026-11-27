#!/usr/bin/env python3
"""2か所に書いている情報が食い違っていないかを確かめる。

- ADR の一覧の表と、各 ADR の front matter・H1
- ADR どうしの番号と status、amends と amended by の対応
- lefthook.yml の pre-push のジョブと、レビューの指摘しない範囲の表

YAML は yq で読む。
"""

import json
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ADR_DIR = ROOT / "docs/adr"
ADR_INDEX = ADR_DIR / "README.md"
LEFTHOOK = ROOT / "lefthook.yml"
REVIEW = ROOT / "docs/conventions/review.md"

ADR_REF = r"ADR-\d{4}"
# status の値は docs/adr/template.md に合わせる
STATUS = re.compile(rf"proposed|accepted|deprecated|(superseded|amended) by {ADR_REF}(, {ADR_REF})*")
# 指摘しない範囲の表で、lefthook のジョブを示す書き方
LEFTHOOK_ROW = re.compile(r"^(\S+)（`lefthook\.yml` の `pre-push`）$")

errors: list[str] = []


def error(message: str, basis: str) -> None:
    errors.append(f"エラー: {message}。根拠: {basis}")


def yaml(path: Path, basis: str, *options: str) -> dict:
    """YAML を yq で読む。YAML として壊れていればエラーにして、空として扱う。"""
    result = subprocess.run(["yq", *options, "-o=json", str(path)], capture_output=True, text=True)
    if result.returncode != 0:
        error(f"{path.relative_to(ROOT)} を YAML として読めない（{result.stderr.strip()}）", basis)
        return {}
    return json.loads(result.stdout) or {}


def table_rows(text: str) -> list[list[str]]:
    """Markdown の表の本文の行を、セルのリストにする。見出しと区切りの行は除く。"""
    rows: list[list[str]] = []
    for line in text.splitlines():
        if re.fullmatch(r"\|[-:| ]+\|", line.rstrip()):
            rows.pop()
        elif line.startswith("|"):
            rows.append([cell.strip() for cell in line.strip("|").split("|")])
    return rows


def read_adr(path: Path) -> dict[str, str]:
    meta = {key: str(value) for key, value in yaml(path, "docs/adr/template.md", "--front-matter=extract").items()}
    h1 = re.search(r"^# ADR-(\d{4}): (.+)$", path.read_text(encoding="utf-8"), re.MULTILINE)
    meta["h1_number"], meta["title"] = h1.groups() if h1 else ("", "")
    return meta


def check_adrs() -> None:
    adrs = {}
    for path in sorted(ADR_DIR.glob("[0-9][0-9][0-9][0-9]-*.md")):
        number = path.name[:4]
        if number in adrs:
            error(f"ADR-{number} のファイルが複数ある", "docs/adr/README.md")
        adrs[number] = read_adr(path) | {"file": path.name}

    for number, adr in adrs.items():
        name = f"docs/adr/{adr['file']}"
        if adr["h1_number"] != number:
            error(f"{name} の H1 の番号（ADR-{adr['h1_number']}）がファイル名と違う", "docs/adr/template.md")
        status = adr.get("status", "")
        if not STATUS.fullmatch(status):
            error(f"{name} の status（{status}）がテンプレートにない値", "docs/adr/template.md")
        for ref in re.findall(r"\d{4}", status):
            if ref not in adrs:
                error(f"{name} の status が、存在しない ADR-{ref} を指している", "docs/adr/README.md")
        for ref in re.findall(r"\d{4}", adr.get("amends", "")):
            amended = adrs.get(ref)
            if amended is None:
                error(f"{name} の amends が、存在しない ADR-{ref} を指している", "docs/adr/README.md")
            elif not re.fullmatch(rf"amended by .*ADR-{number}\b.*", amended.get("status", "")):
                error(f"{name} は ADR-{ref} を amends しているが、ADR-{ref} の status が amended by ADR-{number} になっていない", "docs/adr/README.md")
        if status.startswith("amended by"):
            for ref in re.findall(r"\d{4}", status):
                if number not in re.findall(r"\d{4}", adrs.get(ref, {}).get("amends", "")):
                    error(f"{name} は ADR-{ref} に amended by されているが、ADR-{ref} の amends に ADR-{number} がない", "docs/adr/README.md")

    listed = set()
    for row in table_rows(ADR_INDEX.read_text(encoding="utf-8")):
        if len(row) != 4:
            error(f"ADR の一覧の行（{row[0]}）の列の数が4つでない", "docs/adr/README.md")
            continue
        link = re.fullmatch(r"\[(\d{4})\]\((\d{4})-.+\.md\)", row[0])
        if not link or link.group(1) != link.group(2):
            error(f"ADR の一覧の行（{row[0]}）の番号がリンク先のファイル名と違う", "docs/adr/README.md")
            continue
        number = link.group(1)
        listed.add(number)
        adr = adrs.get(number)
        if adr is None:
            error(f"ADR の一覧にある ADR-{number} のファイルがない", "docs/adr/README.md")
            continue
        for label, actual, expected in [
            ("タイトル", row[1], adr["title"]),
            ("状態", row[2], adr.get("status", "")),
            ("日付", row[3], adr.get("date", "")),
        ]:
            if actual != expected:
                error(f"ADR の一覧の ADR-{number} の{label}（{actual}）が、ADR のファイル（{expected}）と違う", "docs/adr/README.md")
    for number in sorted(adrs.keys() - listed):
        error(f"ADR-{number} が ADR の一覧にない", "docs/adr/README.md")


def check_review_exclusions() -> None:
    jobs = yaml(LEFTHOOK, "docs/conventions/checks.md").get("pre-push", {}).get("jobs", [])
    names = set()
    for job in jobs:
        if "name" in job:
            names.add(job["name"])
        else:
            error(f"lefthook.yml の pre-push に name のないジョブ（{job.get('run', '')}）がある", "docs/conventions/review.md の「指摘しない範囲」")
    rows = set()
    for row in table_rows(REVIEW.read_text(encoding="utf-8")):
        match = LEFTHOOK_ROW.fullmatch(row[-1])
        if match:
            rows.add(match.group(1))
    for name in sorted(names - rows):
        error(f"lefthook.yml の pre-push のジョブ {name} が、レビューの指摘しない範囲の表にない", "docs/conventions/review.md の「指摘しない範囲」")
    for name in sorted(rows - names):
        error(f"レビューの指摘しない範囲の表にある {name} が、lefthook.yml の pre-push のジョブにない", "docs/conventions/review.md の「指摘しない範囲」")


def main() -> int:
    check_adrs()
    check_review_exclusions()
    for message in errors:
        print(message)
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
