package io.github.hokkenikuatsu.audiostorage.application.port

import io.github.hokkenikuatsu.audiostorage.domain.AudioFormat
import java.io.InputStream
import java.nio.file.Path

interface AudioConverter {
    // m4a（MP4 のコンテナと AAC）でなければ Rejected、変換後の WAV が上限を超えれば TooLarge。変換の失敗は AudioConversionException
    fun toStorageFormat(
        input: InputStream,
        output: Path,
    ): StorageConversion

    // 変換の失敗は AudioConversionException
    fun toTransferFormat(
        input: Path,
        format: AudioFormat,
    ): ByteArray
}

sealed interface StorageConversion {
    data object Converted : StorageConversion

    data object Rejected : StorageConversion

    data object TooLarge : StorageConversion
}

class AudioConversionException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
