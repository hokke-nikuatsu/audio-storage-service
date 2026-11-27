package io.github.hokkenikuatsu.audiostorage.application.port

import io.github.hokkenikuatsu.audiostorage.domain.PhraseId
import io.github.hokkenikuatsu.audiostorage.domain.RecordingKey
import io.github.hokkenikuatsu.audiostorage.domain.RecordingPath
import io.github.hokkenikuatsu.audiostorage.domain.UserId

interface RecordingRepository {
    fun userExists(id: UserId): Boolean

    fun phraseExists(id: PhraseId): Boolean

    fun findPath(key: RecordingKey): RecordingPath?

    // 1つのトランザクションでパスを切り替え、それまでのパスを返す（新規なら null）
    fun replacePath(
        key: RecordingKey,
        path: RecordingPath,
    ): RecordingPath?
}
