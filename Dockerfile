# syntax=docker/dockerfile:1

# ビルドの段
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY . .
# このイメージには ffmpeg がなく、変換のテストが失敗するので、テストは流さずに jar だけを作る
# Gradle 本体と依存はキャッシュに置き、2回目からのビルドで取り直さない
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon :bootstrap:bootJar \
    && cp bootstrap/build/libs/bootstrap.jar /app.jar

# 実行の段
FROM eclipse-temurin:25-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends ffmpeg \
    && rm -rf /var/lib/apt/lists/*
# アップロードされたファイルを ffmpeg に読ませるので、乗っ取られたときの被害を小さくするため root で動かさない。
# app ユーザーが /data に書けるようにする。ボリュームは作られるときにこのディレクトリの持ち主を引き継ぐので、VOLUME より前に変える
RUN useradd --system --no-create-home app \
    && mkdir /data \
    && chown app:app /data
VOLUME /data
COPY --from=build /app.jar /app/app.jar
USER app
EXPOSE 8080
# sqlite-jdbc が、同梱する SQLite の本体（C のライブラリ）を読み込むことを許す。許さないと、起動のたびに Java が警告を出す
ENTRYPOINT ["java", "--enable-native-access=ALL-UNNAMED", "-jar", "/app/app.jar"]
