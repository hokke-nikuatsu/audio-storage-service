package io.github.hokkenikuatsu.audiostorage.adapter.db

import io.github.hokkenikuatsu.audiostorage.adapter.db.jooq.tables.references.PHRASES
import io.github.hokkenikuatsu.audiostorage.adapter.db.jooq.tables.references.RECORDINGS
import io.github.hokkenikuatsu.audiostorage.adapter.db.jooq.tables.references.USERS
import io.github.hokkenikuatsu.audiostorage.application.port.RecordingRepository
import io.github.hokkenikuatsu.audiostorage.domain.PhraseId
import io.github.hokkenikuatsu.audiostorage.domain.RecordingKey
import io.github.hokkenikuatsu.audiostorage.domain.RecordingPath
import io.github.hokkenikuatsu.audiostorage.domain.UserId
import org.jooq.DSLContext

// SQLite で、有効な ID の有無を確かめ、組ごとの録音のパスを読み書きする
class JooqRecordingRepository(
    private val dsl: DSLContext,
) : RecordingRepository {
    override fun userExists(id: UserId): Boolean = dsl.fetchExists(USERS, USERS.ID.eq(id.value))

    override fun phraseExists(id: PhraseId): Boolean = dsl.fetchExists(PHRASES, PHRASES.ID.eq(id.value))

    override fun findPath(key: RecordingKey): RecordingPath? = dsl.selectPath(key)

    override fun replacePath(
        key: RecordingKey,
        path: RecordingPath,
    ): RecordingPath? =
        dsl.transactionResult { configuration ->
            val transaction = configuration.dsl()
            transaction.selectPath(key).also {
                transaction
                    .insertInto(RECORDINGS)
                    .set(RECORDINGS.USER_ID, key.userId.value)
                    .set(RECORDINGS.PHRASE_ID, key.phraseId.value)
                    .set(RECORDINGS.FILE_PATH, path.value)
                    .onConflict(RECORDINGS.USER_ID, RECORDINGS.PHRASE_ID)
                    .doUpdate()
                    .set(RECORDINGS.FILE_PATH, path.value)
                    .execute()
            }
        }

    private fun DSLContext.selectPath(key: RecordingKey): RecordingPath? =
        select(RECORDINGS.FILE_PATH)
            .from(RECORDINGS)
            .where(RECORDINGS.USER_ID.eq(key.userId.value), RECORDINGS.PHRASE_ID.eq(key.phraseId.value))
            .fetchOne(RECORDINGS.FILE_PATH)
            ?.let(::RecordingPath)
}
