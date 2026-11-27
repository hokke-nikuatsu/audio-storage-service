package io.github.hokkenikuatsu.audiostorage.application

import io.github.hokkenikuatsu.audiostorage.domain.AudioFormat

sealed interface GetRecordingResult {
    class Found(
        val format: AudioFormat,
        val content: ByteArray,
    ) : GetRecordingResult

    data class Rejected(
        val error: RecordingError,
    ) : GetRecordingResult
}
