package io.github.hokkenikuatsu.audiostorage.application

// 保存と取得で、要求を拒む理由
enum class RecordingError {
    USER_NOT_FOUND,
    PHRASE_NOT_FOUND,
    UNSUPPORTED_AUDIO,
    AUDIO_FILE_TOO_LARGE,
    UNSUPPORTED_FORMAT,
    RECORDING_NOT_FOUND,
}
