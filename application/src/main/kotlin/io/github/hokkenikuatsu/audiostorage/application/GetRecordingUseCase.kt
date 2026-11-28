package io.github.hokkenikuatsu.audiostorage.application

import io.github.hokkenikuatsu.audiostorage.application.port.AudioConverter
import io.github.hokkenikuatsu.audiostorage.application.port.RecordingRepository
import io.github.hokkenikuatsu.audiostorage.application.port.RecordingStorage
import io.github.hokkenikuatsu.audiostorage.domain.AudioFormat
import io.github.hokkenikuatsu.audiostorage.domain.RecordingKey
import org.springframework.stereotype.Service

// 取得の手順。ID、フォーマット、録音の有無の順に確かめ、保存した録音を送受信用のフォーマットに変換して返す
@Service
class GetRecordingUseCase(
    private val repository: RecordingRepository,
    private val storage: RecordingStorage,
    private val converter: AudioConverter,
) {
    private val keyResolver = RecordingKeyResolver(repository)

    // 読み出しや変換の失敗は、AudioConversionException などの例外で上がる
    fun get(
        userId: String,
        phraseId: String,
        format: String,
    ): GetRecordingResult =
        when (val resolution = keyResolver.resolve(userId, phraseId)) {
            is RecordingKeyResolution.Resolved -> get(resolution.key, format)
            is RecordingKeyResolution.Rejected -> GetRecordingResult.Rejected(resolution.error)
        }

    private fun get(
        key: RecordingKey,
        format: String,
    ): GetRecordingResult {
        val audioFormat = AudioFormat.parse(format)
        val input = audioFormat?.let { repository.findPath(key) }?.let(storage::resolve)
        return when {
            audioFormat == null -> GetRecordingResult.Rejected(RecordingError.UNSUPPORTED_FORMAT)
            input == null -> GetRecordingResult.Rejected(RecordingError.RECORDING_NOT_FOUND)
            else -> GetRecordingResult.Found(audioFormat, converter.toTransferFormat(input, audioFormat))
        }
    }
}

sealed interface GetRecordingResult {
    class Found(
        val format: AudioFormat,
        val content: ByteArray,
    ) : GetRecordingResult

    data class Rejected(
        val error: RecordingError,
    ) : GetRecordingResult
}
