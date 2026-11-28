package io.github.hokkenikuatsu.audiostorage.web

import io.github.hokkenikuatsu.audiostorage.application.RecordingError
import io.github.hokkenikuatsu.audiostorage.application.port.AudioConversionException
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.springframework.http.HttpMethod
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.method.annotation.ExceptionHandlerMethodResolver
import org.springframework.web.multipart.MaxUploadSizeExceededException
import org.springframework.web.multipart.MultipartException
import org.springframework.web.multipart.support.MissingServletRequestPartException

class ErrorHandlerTest :
    FunSpec({
        val handler = ErrorHandler()
        val resolver = ExceptionHandlerMethodResolver(ErrorHandler::class.java)

        // Spring と同じく、例外に最も近い型の @ExceptionHandler を選んで呼ぶ
        @Suppress("UNCHECKED_CAST")
        fun handle(e: Exception): ResponseEntity<ProblemDetail> {
            val method = resolver.resolveMethod(e).shouldNotBeNull()
            val args = if (method.parameterCount == 0) emptyArray() else arrayOf<Any>(e)
            return method.invoke(handler, *args) as ResponseEntity<ProblemDetail>
        }

        fun ResponseEntity<ProblemDetail>.shouldBeProblem(
            status: Int,
            code: String,
        ) {
            statusCode.value() shouldBe status
            val problem = body.shouldNotBeNull()
            problem.status shouldBe status
            problem.properties?.get("code") shouldBe code
        }

        fun rejected(error: RecordingError) = handle(RecordingRejectedException(error))

        context("REQ-1.4 音声ファイルがない") {
            test("REQ-1.4 audio_file のパートがなければ、400 と MISSING_AUDIO_FILE を返す") {
                handle(MissingServletRequestPartException("audio_file")).shouldBeProblem(400, "MISSING_AUDIO_FILE")
            }

            test("REQ-1.4 multipart ではない要求なら、400 と MISSING_AUDIO_FILE を返す") {
                handle(MultipartException("not a multipart request")).shouldBeProblem(400, "MISSING_AUDIO_FILE")
            }
        }

        context("REQ-1.5 受け付けられないファイル") {
            test("REQ-1.5 m4a として読めなければ、415 と UNSUPPORTED_AUDIO を返す") {
                rejected(RecordingError.UNSUPPORTED_AUDIO).shouldBeProblem(415, "UNSUPPORTED_AUDIO")
            }
        }

        context("REQ-1.6 ファイルが大きすぎる") {
            test("REQ-1.6 ファイルが multipart の上限を超えたら、413 と AUDIO_FILE_TOO_LARGE を返す") {
                handle(MaxUploadSizeExceededException(10L * 1024 * 1024)).shouldBeProblem(413, "AUDIO_FILE_TOO_LARGE")
            }

            test("REQ-1.6 変換後の WAV が上限を超えたら、413 と AUDIO_FILE_TOO_LARGE を返す") {
                rejected(RecordingError.AUDIO_FILE_TOO_LARGE).shouldBeProblem(413, "AUDIO_FILE_TOO_LARGE")
            }
        }

        context("REQ-1.7 変換や保存の途中の失敗") {
            test("REQ-1.7 変換に失敗したら、500 と INTERNAL_ERROR を返し、原因を本文に書かない") {
                val response = handle(AudioConversionException("ffmpeg が終了コード 1 で失敗した"))

                response.shouldBeProblem(500, "INTERNAL_ERROR")
                response.body?.detail?.contains("ffmpeg") shouldBe false
            }
        }

        context("REQ-2.2 録音がない") {
            test("REQ-2.2 組に録音がなければ、404 と RECORDING_NOT_FOUND を返す") {
                rejected(RecordingError.RECORDING_NOT_FOUND).shouldBeProblem(404, "RECORDING_NOT_FOUND")
            }
        }

        context("REQ-2.3 対応していないフォーマット") {
            test("REQ-2.3 フォーマットが m4a でなければ、400 と UNSUPPORTED_FORMAT を返す") {
                rejected(RecordingError.UNSUPPORTED_FORMAT).shouldBeProblem(400, "UNSUPPORTED_FORMAT")
            }
        }

        context("REQ-2.4 読み出しや変換の失敗") {
            test("REQ-2.4 想定外の例外が上がったら、500 と INTERNAL_ERROR を返す") {
                handle(IllegalStateException("想定外")).shouldBeProblem(500, "INTERNAL_ERROR")
            }
        }

        context("REQ-3.2 ユーザーが見つからない") {
            test("REQ-3.2 ユーザー ID が登録されていなければ、404 と USER_NOT_FOUND を返す") {
                rejected(RecordingError.USER_NOT_FOUND).shouldBeProblem(404, "USER_NOT_FOUND")
            }
        }

        context("REQ-3.3 フレーズが見つからない") {
            test("REQ-3.3 フレーズ ID が登録されていなければ、404 と PHRASE_NOT_FOUND を返す") {
                rejected(RecordingError.PHRASE_NOT_FOUND).shouldBeProblem(404, "PHRASE_NOT_FOUND")
            }
        }

        test("Spring がステータスを決めている例外は、そのステータスと本文で返す") {
            val response = handle(HttpRequestMethodNotSupportedException("DELETE", listOf("GET")))

            response.statusCode.value() shouldBe 405
            response.headers.allow shouldBe setOf(HttpMethod.GET)
        }
    })
