package io.github.hokkenikuatsu.audiostorage.adapter.db

import io.github.hokkenikuatsu.audiostorage.adapter.db.jooq.tables.references.RECORDINGS
import io.github.hokkenikuatsu.audiostorage.domain.PhraseId
import io.github.hokkenikuatsu.audiostorage.domain.RecordingKey
import io.github.hokkenikuatsu.audiostorage.domain.RecordingPath
import io.github.hokkenikuatsu.audiostorage.domain.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempfile
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.flywaydb.core.Flyway
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.exception.DataAccessException
import org.jooq.impl.DSL
import org.sqlite.SQLiteDataSource

class JooqRecordingRepositoryTest :
    FunSpec({
        // テストごとに、スキーマと初期データのマイグレーションを流した一時的な SQLite を作る
        fun newDatabase(): DSLContext {
            val dataSource =
                SQLiteDataSource().apply {
                    url = "jdbc:sqlite:${tempfile(suffix = ".db").absolutePath}"
                    setEnforceForeignKeys(true)
                }
            Flyway
                .configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration", "classpath:db/seed")
                .load()
                .migrate()
            return DSL.using(dataSource, SQLDialect.SQLITE)
        }

        val key = RecordingKey(UserId(1), PhraseId(1))
        val otherKey = RecordingKey(UserId(2), PhraseId(1))

        context("REQ-4.2 初期データの ID が登録された状態で受け付ける") {
            test("REQ-4.2 初期データのユーザー ID とフレーズ ID 1〜3 は存在する") {
                val repository = JooqRecordingRepository(newDatabase())
                (1L..3L).forEach { id ->
                    repository.userExists(UserId(id)) shouldBe true
                    repository.phraseExists(PhraseId(id)) shouldBe true
                }
            }

            test("REQ-4.2 初期データにない ID は存在しない") {
                val repository = JooqRecordingRepository(newDatabase())
                listOf(4L, Long.MAX_VALUE).forEach { id ->
                    repository.userExists(UserId(id)) shouldBe false
                    repository.phraseExists(PhraseId(id)) shouldBe false
                }
            }
        }

        context("REQ-5.4 組と保存したファイルのパスを記録する") {
            test("REQ-5.4 録音のない組には、パスがない") {
                JooqRecordingRepository(newDatabase()).findPath(key).shouldBeNull()
            }
        }

        context("REQ-1.1 録音を保存し、組に紐づけて記録する") {
            test("REQ-1.1 新しい組のパスを記録し、それまでのパスがないことを返す") {
                val repository = JooqRecordingRepository(newDatabase())

                repository.replacePath(key, RecordingPath("1/1/a.wav")).shouldBeNull()

                repository.findPath(key) shouldBe RecordingPath("1/1/a.wav")
            }
        }

        context("REQ-1.3 既存の録音を置き換える") {
            test("REQ-1.3 切り替えは古いパスを返し、組ごとに1行を保つ") {
                val database = newDatabase()
                val repository = JooqRecordingRepository(database)
                repository.replacePath(key, RecordingPath("1/1/a.wav"))
                repository.replacePath(otherKey, RecordingPath("2/1/a.wav"))

                repository.replacePath(key, RecordingPath("1/1/b.wav")) shouldBe RecordingPath("1/1/a.wav")

                repository.findPath(key) shouldBe RecordingPath("1/1/b.wav")
                repository.findPath(otherKey) shouldBe RecordingPath("2/1/a.wav")
                database.fetchCount(RECORDINGS) shouldBe 2
            }
        }

        context("REQ-1.7 失敗しても既存の録音を保つ") {
            test("REQ-1.7 切り替えが失敗したら、組のパスは変わらない") {
                val repository = JooqRecordingRepository(newDatabase())
                repository.replacePath(key, RecordingPath("1/1/a.wav"))
                repository.replacePath(otherKey, RecordingPath("2/1/a.wav"))

                // ほかの組が使っているパスへの切り替えは、パスの一意制約で失敗する
                shouldThrow<DataAccessException> {
                    repository.replacePath(otherKey, RecordingPath("1/1/a.wav"))
                }

                repository.findPath(otherKey) shouldBe RecordingPath("2/1/a.wav")
            }
        }
    })
