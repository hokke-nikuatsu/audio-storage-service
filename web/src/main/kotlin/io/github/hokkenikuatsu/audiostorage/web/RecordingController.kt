package io.github.hokkenikuatsu.audiostorage.web

import io.github.hokkenikuatsu.audiostorage.application.GetRecordingResult
import io.github.hokkenikuatsu.audiostorage.application.GetRecordingUseCase
import io.github.hokkenikuatsu.audiostorage.application.SaveRecordingResult
import io.github.hokkenikuatsu.audiostorage.application.SaveRecordingUseCase
import io.github.hokkenikuatsu.audiostorage.domain.AudioFormat
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.support.MissingServletRequestPartException

internal const val AUDIO_FILE = "audio_file"

// 録音の保存と取得の入口。拒んだ理由は RecordingRejectedException にして ErrorHandler に任せる
@RestController
@RequestMapping("/audio/user/{user_id}/phrase/{phrase_id}")
internal class RecordingController(
    private val saveRecording: SaveRecordingUseCase,
    private val getRecording: GetRecordingUseCase,
) {
    // consumes を付けると multipart ではない要求が 415 になるので付けず、ErrorHandler でファイルがないことにする
    @PostMapping
    fun save(
        @PathVariable("user_id") userId: String,
        @PathVariable("phrase_id") phraseId: String,
        @RequestPart(AUDIO_FILE) audioFile: MultipartFile,
    ): ResponseEntity<Unit> {
        if (audioFile.isEmpty) {
            throw MissingServletRequestPartException(AUDIO_FILE)
        }
        return when (val result = audioFile.inputStream.use { saveRecording.save(userId, phraseId, it) }) {
            SaveRecordingResult.Created -> ResponseEntity.status(HttpStatus.CREATED).build()
            SaveRecordingResult.Replaced -> ResponseEntity.noContent().build()
            is SaveRecordingResult.Rejected -> throw RecordingRejectedException(result.error)
        }
    }

    @GetMapping("/{audio_format}")
    fun get(
        @PathVariable("user_id") userId: String,
        @PathVariable("phrase_id") phraseId: String,
        @PathVariable("audio_format") audioFormat: String,
    ): ResponseEntity<ByteArray> =
        when (val result = getRecording.get(userId, phraseId, audioFormat)) {
            is GetRecordingResult.Found -> {
                ResponseEntity.ok().contentType(result.format.mediaType()).body(result.content)
            }

            is GetRecordingResult.Rejected -> {
                throw RecordingRejectedException(result.error)
            }
        }

    private fun AudioFormat.mediaType(): MediaType =
        when (this) {
            AudioFormat.M4A -> MediaType("audio", "mp4")
        }
}
