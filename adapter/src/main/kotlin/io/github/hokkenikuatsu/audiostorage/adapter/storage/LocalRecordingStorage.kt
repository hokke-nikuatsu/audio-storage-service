package io.github.hokkenikuatsu.audiostorage.adapter.storage

import io.github.hokkenikuatsu.audiostorage.application.port.RecordingStorage
import io.github.hokkenikuatsu.audiostorage.domain.RecordingKey
import io.github.hokkenikuatsu.audiostorage.domain.RecordingPath
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

// 録音を root の下に {user_id}/{phrase_id}/{UUID}.wav として置く
class LocalRecordingStorage(
    private val root: Path,
) : RecordingStorage {
    // パスは型付きの ID と UUID だけから作り、要求の文字列をそのまま使わない
    override fun allocate(key: RecordingKey): RecordingPath {
        val directory = "${key.userId.value}/${key.phraseId.value}"
        Files.createDirectories(root.resolve(directory))
        return RecordingPath("$directory/${UUID.randomUUID()}.wav")
    }

    override fun resolve(path: RecordingPath): Path = root.resolve(path.value)

    override fun delete(path: RecordingPath) {
        try {
            Files.deleteIfExists(resolve(path))
        } catch (e: IOException) {
            logger.warn("録音のファイルを消せなかった: {}", path.value, e)
        }
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(LocalRecordingStorage::class.java)
    }
}
