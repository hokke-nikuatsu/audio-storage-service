# Project Structure

## Organization Philosophy

業務の手順（内側）と、DB・ディスク・変換・HTTP といった外の世界（外側）を分ける。内側は外の世界に求めるものをインターフェース（ポート）として宣言し、外側がそれを実装する（アダプタ）。依存は常に外から内へ向く。

層ごとに Gradle のモジュールを分け、使ってよいモジュールだけを依存に書く。依存の向きに反する import はコンパイルエラーになる。依存してよいモジュールの表は `build-logic` に1か所だけ書き、表にない依存（テストの依存を含む）はビルドで失敗させる。

## Directory Patterns

### domain
**Location**: `/domain/`  
**Purpose**: ユーザー ID、フレーズ ID、録音のような値と、それにまつわる規則。ほかのモジュールにも、外部のライブラリにも依存しない  
**Example**: `UserId`、`PhraseId`

### application
**Location**: `/application/`  
**Purpose**: 保存と取得の手順（ユースケース）と、手順が外の世界に求めるポート。依存してよいのは `domain` と、Spring の `spring-context`（`@Service`）だけ  
**Example**: 保存のユースケース、`AudioConverter` のようなポート

### adapter
**Location**: `/adapter/`  
**Purpose**: `application` のポートの実装。DB、ディスク、変換は、この中のパッケージで分ける。依存してよいのは `application` と `domain`  
**Example**: ffmpeg で変換する `AudioConverter` の実装

### web
**Location**: `/web/`  
**Purpose**: HTTP の入口。リクエストを受けてユースケースを呼び、結果とエラーを HTTP の応答にする。依存してよいのは `application` と `domain`  
**Example**: 保存と取得の Controller、エラーの応答

### bootstrap
**Location**: `/bootstrap/`  
**Purpose**: Spring Boot を起動し、すべてのモジュールの部品を組み立てる。設定ファイルもここに置く。組み立て以外の処理は書かない  
**Example**: `main` 関数、`application.yml`

### build-logic
**Location**: `/build-logic/`  
**Purpose**: モジュールに共通のビルドの設定（Kotlin、toolchain、ktlint、detekt）を convention plugin にまとめる。依存してよいモジュールの表と、その検査もここに置く

## Code Organization Principles

- ポートは、外の世界との境目（DB、ディスク、変換）にだけ置く。Controller から呼ばれる側の入力のポートは置かず、`web` はユースケースのクラスを直接呼ぶ
- `adapter` と `web` は互いに依存しない
- ほかのモジュールへの依存は `implementation` で宣言し、`api` は `application` から `domain` への依存だけに使う
- モジュールの外から使わないクラスは `internal` にする

判断の理由: [ADR-0010](../../docs/adr/0010-separate-core-from-io-by-ports-and-adapters.md)、[ADR-0011](../../docs/adr/0011-enforce-layer-dependencies-with-gradle-modules.md)
