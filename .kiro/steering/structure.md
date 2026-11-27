# Project Structure

## Organization Philosophy

業務の手順（内側）と、DB・ディスク・変換・HTTP といった外の世界（外側）を分ける。内側は外の世界に求めるものをインターフェース（ポート）として宣言し、外側がそれを実装する（アダプタ）。依存は常に外から内へ向く。

## Directory Patterns

### domain
**Purpose**: ユーザー ID、フレーズ ID、録音のような値と、それにまつわる規則。ほかの層にも、Spring にも依存しない
**Example**: `UserId`、`PhraseId`

### application
**Purpose**: 保存と取得の手順（ユースケース）と、手順が外の世界に求めるポート。Spring は `@Service` と `@Transactional` だけを使い、Spring Web と JDBC は使わない
**Example**: 保存のユースケース、`AudioConverter` のようなポート

### adapter
**Purpose**: `application` のポートの実装。DB、ディスク、変換は、この中のパッケージで分ける
**Example**: ffmpeg で変換する `AudioConverter` の実装

### web
**Purpose**: HTTP の入口。リクエストを受けてユースケースを呼び、結果とエラーを HTTP の応答にする
**Example**: 保存と取得の Controller、エラーの応答

## Code Organization Principles

- ポートは、外の世界との境目（DB、ディスク、変換）にだけ置く。Controller から呼ばれる側の入力のポートは置かず、`web` はユースケースのクラスを直接呼ぶ

判断の理由: [ADR-0010](../../docs/adr/0010-separate-core-from-io-by-ports-and-adapters.md)
