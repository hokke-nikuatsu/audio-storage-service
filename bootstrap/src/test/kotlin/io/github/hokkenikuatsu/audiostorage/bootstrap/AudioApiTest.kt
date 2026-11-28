package io.github.hokkenikuatsu.audiostorage.bootstrap

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.env.Environment
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.web.client.RestClient
import tools.jackson.databind.json.JsonMapper
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readBytes

// テスト用の音声の置き場所。テストは bootstrap/ をカレントディレクトリにして走るので、1つ上からたどる
private val AUDIO_DIR: Path = Path.of("..", "testdata", "audio")

// 受け付ける m4a（MP4 のコンテナに AAC の音声を入れたもの）
private val ACCEPTED_M4A: Path = AUDIO_DIR.resolve("eagle-has-landed.m4a")

private class Response(
    val status: Int,
    val contentType: MediaType?,
    val body: ByteArray,
) {
    fun code(): String? =
        JsonMapper
            .shared()
            .readTree(body)
            .get("code")
            ?.asString()
}

// Spring Boot を起動し、保存と取得の要求を HTTP で送って確かめる
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AudioApiTest(
    private val environment: Environment,
) : FunSpec({
        val client = RestClient.create("http://localhost:${environment.getRequiredProperty("local.server.port")}")

        fun send(request: RestClient.RequestHeadersSpec<*>): Response =
            request.exchange { _, response ->
                Response(response.statusCode.value(), response.headers.contentType, response.body.readAllBytes())
            }

        // curl -F "audio_file=@{file}" と同じく、audio_file のフィールドにファイルを入れて送る
        fun save(
            userId: String,
            phraseId: String,
            content: ByteArray,
            field: String = "audio_file",
        ): Response {
            val form = MultipartBodyBuilder().apply { part(field, content).filename("recording.m4a") }.build()
            return send(
                client
                    .post()
                    .uri("/audio/user/{user_id}/phrase/{phrase_id}", userId, phraseId)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(form),
            )
        }

        fun get(
            userId: String,
            phraseId: String,
            format: String,
        ): Response =
            send(
                client
                    .get()
                    .uri("/audio/user/{user_id}/phrase/{phrase_id}/{audio_format}", userId, phraseId, format),
            )

        fun savedFiles(
            userId: Int,
            phraseId: Int,
        ): List<Path> = dataDir.resolve("recordings/$userId/$phraseId").listDirectoryEntries()

        val m4a = ACCEPTED_M4A.readBytes()

        context("REQ-1.1 m4a を WAV にして保存し、組に記録する") {
            test("REQ-1.1 m4a を保存すると、組のディレクトリに WAV を書く") {
                save("1", "1", m4a).status shouldBe 201

                val files = savedFiles(1, 1)
                files shouldHaveSize 1
                files
                    .single()
                    .readBytes()
                    .copyOfRange(0, 4)
                    .decodeToString() shouldBe "RIFF"
            }
        }

        context("REQ-1.2 保存の成功を返す") {
            test("REQ-1.2 保存に成功すると、本文なしの 201 を返す") {
                val response = save("2", "3", m4a)

                response.status shouldBe 201
                response.body.size shouldBe 0
            }
        }

        context("REQ-5.2 audio_file のフィールドで受け取る") {
            test("REQ-5.2 audio_file のフィールドで送ると、受け付ける") {
                save("3", "1", m4a, field = "audio_file").status shouldBe 201
            }

            test("REQ-5.2 audio_file ではない名前のフィールドで送ると、400 と MISSING_AUDIO_FILE を返す") {
                val response = save("3", "3", m4a, field = "file")

                response.status shouldBe 400
                response.code() shouldBe "MISSING_AUDIO_FILE"
            }
        }

        context("REQ-2.1 WAV を m4a にして返す") {
            test("REQ-2.1 保存した録音を、m4a で取得できる") {
                save("1", "2", m4a).status shouldBe 201

                val response = get("1", "2", "m4a")

                response.status shouldBe 200
                response.contentType shouldBe MediaType("audio", "mp4")
                // MP4 のファイルは、5〜8バイト目が ftyp になる
                response.body.copyOfRange(4, 8).decodeToString() shouldBe "ftyp"
            }
        }

        context("REQ-1.3 既存の録音を置き換える") {
            test("REQ-1.3 録音のある組に保存すると、204 を返し、古いファイルを消す") {
                save("2", "2", m4a).status shouldBe 201
                val before = savedFiles(2, 2)

                save("2", "2", m4a).status shouldBe 204

                val after = savedFiles(2, 2)
                after shouldHaveSize 1
                after.single() shouldNotBe before.single()
            }
        }

        context("REQ-4.2 初期データの ID が登録された状態で受け付ける") {
            // ユーザー ID とフレーズ ID の 1〜3 を、ほかのテストと重ならない組で1度ずつ使う
            listOf(1 to 3, 2 to 1, 3 to 2).forEach { (userId, phraseId) ->
                test("REQ-4.2 ユーザー ID $userId とフレーズ ID $phraseId で、保存と取得ができる") {
                    save("$userId", "$phraseId", m4a).status shouldBe 201
                    get("$userId", "$phraseId", "m4a").status shouldBe 200
                }
            }
        }

        context("REQ-1.4 音声ファイルがない") {
            test("REQ-1.4 空のファイルを送ると、400 と MISSING_AUDIO_FILE を返す") {
                val response = save("1", "1", ByteArray(0))

                response.status shouldBe 400
                response.code() shouldBe "MISSING_AUDIO_FILE"
            }
        }

        context("REQ-1.5 受け付けられないファイル") {
            test("REQ-1.5 m4a として読めないファイルを送ると、415 と UNSUPPORTED_AUDIO を返す") {
                val response = save("1", "1", AUDIO_DIR.resolve("not-audio.m4a").readBytes())

                response.status shouldBe 415
                response.code() shouldBe "UNSUPPORTED_AUDIO"
            }
        }

        context("REQ-1.6 ファイルが大きすぎる") {
            test("REQ-1.6 10MB を超えるファイルを送ると、413 と AUDIO_FILE_TOO_LARGE を返す") {
                val response = save("1", "1", ByteArray(10 * 1024 * 1024 + 1))

                response.status shouldBe 413
                response.code() shouldBe "AUDIO_FILE_TOO_LARGE"
            }
        }

        context("REQ-2.3 対応していないフォーマット") {
            test("REQ-2.3 m4a ではないフォーマットを指定すると、400 と UNSUPPORTED_FORMAT を返す") {
                val response = get("1", "1", "wav")

                response.status shouldBe 400
                response.code() shouldBe "UNSUPPORTED_FORMAT"
            }
        }

        context("REQ-3.2 ユーザーが見つからない") {
            test("REQ-3.2 登録されていないユーザー ID に保存すると、404 と USER_NOT_FOUND を返す") {
                val response = save("9", "1", m4a)

                response.status shouldBe 404
                response.code() shouldBe "USER_NOT_FOUND"
            }
        }

        test("存在しないパスは、Spring の 404 を code なしで返す") {
            val response = send(client.get().uri("/audio"))

            response.status shouldBe 404
            response.code() shouldBe null
        }
    }) {
    companion object {
        // 起動するたびに空の DB と保存先から始める
        private val dataDir = Files.createTempDirectory("audio-storage-api-test")

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("audio-storage.data-dir") { dataDir.toString() }
        }
    }
}
