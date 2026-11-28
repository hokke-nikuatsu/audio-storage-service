package io.github.hokkenikuatsu.audiostorage.web

import io.github.hokkenikuatsu.audiostorage.application.RecordingError
import org.apache.commons.logging.LogFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.ErrorResponse
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.multipart.MaxUploadSizeExceededException
import org.springframework.web.multipart.MultipartException
import org.springframework.web.multipart.support.MissingServletRequestPartException

// 拒んだ理由と例外を、code を持つエラーの JSON（ProblemDetail）にする。エラーの JSON はここでだけ作る
@RestControllerAdvice
internal class ErrorHandler {
    @ExceptionHandler
    fun handleRejected(e: RecordingRejectedException): ResponseEntity<ProblemDetail> = rejected(e.error)

    // multipart ではない要求は、Spring の既定では 500 になるので、ファイルがないことにする
    @ExceptionHandler(MissingServletRequestPartException::class, MultipartException::class)
    fun handleMissingAudioFile(): ResponseEntity<ProblemDetail> =
        problem(HttpStatus.BAD_REQUEST, "MISSING_AUDIO_FILE", "$AUDIO_FILE のフィールドに音声ファイルがありません")

    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handleTooLarge(): ResponseEntity<ProblemDetail> = rejected(RecordingError.AUDIO_FILE_TOO_LARGE)

    // Spring がステータスを決めている例外（存在しないパスやメソッドなど）は、そのまま返す。それ以外は原因をログにだけ書く
    @ExceptionHandler
    fun handleUnexpected(e: Exception): ResponseEntity<ProblemDetail> {
        if (e is ErrorResponse) {
            return ResponseEntity.status(e.statusCode).headers(e.headers).body(e.body)
        }
        logger.error("要求の処理に失敗した", e)
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "サーバーで処理に失敗しました")
    }

    private fun rejected(error: RecordingError) = problem(error.status(), error.name, error.detail())

    private fun RecordingError.status(): HttpStatus =
        when (this) {
            RecordingError.USER_NOT_FOUND -> HttpStatus.NOT_FOUND
            RecordingError.PHRASE_NOT_FOUND -> HttpStatus.NOT_FOUND
            RecordingError.RECORDING_NOT_FOUND -> HttpStatus.NOT_FOUND
            RecordingError.UNSUPPORTED_AUDIO -> HttpStatus.UNSUPPORTED_MEDIA_TYPE
            RecordingError.AUDIO_FILE_TOO_LARGE -> HttpStatus.CONTENT_TOO_LARGE
            RecordingError.UNSUPPORTED_FORMAT -> HttpStatus.BAD_REQUEST
        }

    private fun RecordingError.detail(): String =
        when (this) {
            RecordingError.USER_NOT_FOUND -> "ユーザーが登録されていません"
            RecordingError.PHRASE_NOT_FOUND -> "フレーズが登録されていません"
            RecordingError.RECORDING_NOT_FOUND -> "録音が保存されていません"
            RecordingError.UNSUPPORTED_AUDIO -> "m4a（MP4 のコンテナに AAC の音声）として読めません"
            RecordingError.AUDIO_FILE_TOO_LARGE -> "音声ファイルが大きすぎます"
            RecordingError.UNSUPPORTED_FORMAT -> "対応していないフォーマットです。m4a だけに対応しています"
        }

    private fun problem(
        status: HttpStatus,
        code: String,
        detail: String,
    ): ResponseEntity<ProblemDetail> {
        val problem = ProblemDetail.forStatusAndDetail(status, detail).apply { setProperty("code", code) }
        return ResponseEntity.status(status).body(problem)
    }

    private companion object {
        private val logger = LogFactory.getLog(ErrorHandler::class.java)
    }
}

// ユースケースが要求を拒んだことを、Controller から ErrorHandler に伝える
internal class RecordingRejectedException(
    val error: RecordingError,
) : RuntimeException(error.name)
