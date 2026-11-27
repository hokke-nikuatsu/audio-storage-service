package io.github.hokkenikuatsu.audiostorage.application

import io.github.hokkenikuatsu.audiostorage.application.port.AudioConverter
import io.github.hokkenikuatsu.audiostorage.application.port.RecordingRepository
import io.github.hokkenikuatsu.audiostorage.application.port.RecordingStorage
import io.github.hokkenikuatsu.audiostorage.application.port.StorageConversion
import io.github.hokkenikuatsu.audiostorage.domain.RecordingKey
import org.springframework.stereotype.Service
import java.io.InputStream

@Service
class SaveRecordingUseCase(
    private val repository: RecordingRepository,
    private val storage: RecordingStorage,
    private val converter: AudioConverter,
) {
    private val keyResolver = RecordingKeyResolver(repository)

    fun save(
        userId: String,
        phraseId: String,
        audio: InputStream,
    ): SaveRecordingResult =
        when (val resolution = keyResolver.resolve(userId, phraseId)) {
            is RecordingKeyResolution.Rejected -> SaveRecordingResult.Rejected(resolution.error)
            is RecordingKeyResolution.Resolved -> save(resolution.key, audio)
        }

    // 新しいファイルに変換して書き、DB のパスを切り替えてから、古いファイルを消す。
    // 切り替えまで進まなければ（拒んだとき、例外が上がったとき）、新しいファイルを消す
    private fun save(
        key: RecordingKey,
        audio: InputStream,
    ): SaveRecordingResult {
        val newPath = storage.allocate(key)
        var replaced = false
        return try {
            when (converter.toStorageFormat(audio, storage.resolve(newPath))) {
                StorageConversion.Rejected -> {
                    SaveRecordingResult.Rejected(RecordingError.UNSUPPORTED_AUDIO)
                }

                StorageConversion.TooLarge -> {
                    SaveRecordingResult.Rejected(RecordingError.AUDIO_FILE_TOO_LARGE)
                }

                StorageConversion.Converted -> {
                    val oldPath = repository.replacePath(key, newPath)
                    replaced = true
                    if (oldPath == null) {
                        SaveRecordingResult.Created
                    } else {
                        storage.delete(oldPath)
                        SaveRecordingResult.Replaced
                    }
                }
            }
        } finally {
            if (!replaced) {
                storage.delete(newPath)
            }
        }
    }
}

sealed interface SaveRecordingResult {
    data object Created : SaveRecordingResult

    data object Replaced : SaveRecordingResult

    data class Rejected(
        val error: RecordingError,
    ) : SaveRecordingResult
}
