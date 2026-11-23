# 開発の進め方の規約

開発の進め方（ドキュメントの置き場、開発の流れ、要件 ID、ブランチ）の現時点の正。判断の理由は [docs/adr/](../adr/README.md) にある。

## ドキュメントの置き場

| 種類 | 置き場 | 書くこと |
|---|---|---|
| 作業の進め方の規約 | `docs/conventions/` | ドキュメントの置き場、開発の流れ、要件 ID、ブランチ、コミット、PR |
| コードに関する規約 | `.kiro/steering/` | 技術スタック、アーキテクチャ、コーディング規約 |
| 仕様 | `.kiro/specs/<feature>/` | 要件（requirements.md）、設計（design.md）、タスク（tasks.md） |
| 判断の理由 | `docs/adr/` | 1つの判断につき1ファイル |
| AI の入口 | `AGENTS.md`、`CLAUDE.md` | 言語（応答と成果物）と、上の場所への案内 |
| 人の入口 | `README.md` | 概要、起動方法、ドキュメントへのリンク |

- 同じことを2か所に書かない。現時点の正は規約・steering・spec に置き、ADR には判断の理由だけを書く
- ADR は書き換えない。判断を変えるときは新しい ADR で置き換える
- コードに経緯を書かない。経緯は ADR と PR に残す
- ドキュメントを追加したら、`README.md` にリンクを足す

## 開発の流れ

cc-sdd による仕様駆動で進める。要件・設計・タスクは人が承認してから次へ進み、`-y` で承認を飛ばさない。

1. 要件: `/kiro-spec-init` → `/kiro-spec-requirements` → 人が requirements.md を承認する
2. 設計: 技術選定を1つずつ決めて `/adr` で記録し、steering に反映する → `/kiro-spec-design` → 人が design.md を承認する
3. タスク: `/kiro-spec-tasks` → 人が tasks.md を承認する（1タスクが1つの PR に収まる粒度か、依存関係に沿った順番か、すべての要件がどれかのタスクに入っているか）
4. 実装: タスクごとにブランチを切り、`/kiro-impl <feature> <タスク番号>` で1タスクずつ実装して PR を出す

人が承認する spec は requirements.md、design.md、tasks.md の3つで、spec.json と research.md はレビューの対象にしない。

## 要件 ID

- cc-sdd の文書の中では、数字の ID（`1.2`）をそのまま使う
- cc-sdd の文書の外（テスト名、PR、ADR、コミット）では `REQ-1.2` と書き、requirements.md の `1.2` を指す

## ブランチ

- 名前は `{prefix}/{内容を表す英語のケバブケース}` とし、40文字以内にする（例: `feature/upload-audio`）
- main から切る。main には PR を経由してだけ入れる

| prefix | 用途 |
|---|---|
| `feature` | 機能の開発 |
| `bugfix` | 不具合の修正 |
| `test` | テストだけの変更 |
| `docs` | ドキュメントだけの変更 |
| `chore` | 設定、ツール、依存関係などの変更 |
