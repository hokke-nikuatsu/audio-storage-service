package io.github.hokkenikuatsu.audiostorage.application.fake

import io.github.hokkenikuatsu.audiostorage.application.port.RecordingRepository
import io.github.hokkenikuatsu.audiostorage.domain.PhraseId
import io.github.hokkenikuatsu.audiostorage.domain.RecordingKey
import io.github.hokkenikuatsu.audiostorage.domain.RecordingPath
import io.github.hokkenikuatsu.audiostorage.domain.UserId

// 登録された ID と、組ごとの録音のパスをメモリに持つ。テストは paths を読み書きして、DB の状態を用意し、確かめる。
// paths に組がなければ、その組には録音のパスがない
open class FakeRecordingRepository(
    private val userIds: Set<Long> = setOf(1),
    private val phraseIds: Set<Long> = setOf(1),
) : RecordingRepository {
    val paths = mutableMapOf<RecordingKey, RecordingPath>()

    override fun userExists(id: UserId): Boolean = id.value in userIds

    override fun phraseExists(id: PhraseId): Boolean = id.value in phraseIds

    override fun findPath(key: RecordingKey): RecordingPath? = paths[key]

    override fun replacePath(
        key: RecordingKey,
        path: RecordingPath,
    ): RecordingPath? = paths.put(key, path)
}

// パスの切り替えだけが失敗し、paths を変えない。DB の切り替えの失敗を再現する
class FailingReplacePathRecordingRepository(
    userIds: Set<Long> = setOf(1),
    phraseIds: Set<Long> = setOf(1),
) : FakeRecordingRepository(userIds, phraseIds) {
    override fun replacePath(
        key: RecordingKey,
        path: RecordingPath,
    ): RecordingPath? = throw IllegalStateException("パスの切り替えに失敗した")
}
