package io.github.hokkenikuatsu.audiostorage.application

sealed interface SaveRecordingResult {
    data object Created : SaveRecordingResult

    data object Replaced : SaveRecordingResult

    data class Rejected(
        val error: RecordingError,
    ) : SaveRecordingResult
}
