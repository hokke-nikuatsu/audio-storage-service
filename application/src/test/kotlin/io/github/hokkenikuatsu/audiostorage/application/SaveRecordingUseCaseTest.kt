package io.github.hokkenikuatsu.audiostorage.application

import io.github.hokkenikuatsu.audiostorage.application.fake.FailingAudioConverter
import io.github.hokkenikuatsu.audiostorage.application.fake.FailingReplacePathRecordingRepository
import io.github.hokkenikuatsu.audiostorage.application.fake.FakeAudioConverter
import io.github.hokkenikuatsu.audiostorage.application.fake.FakeRecordingRepository
import io.github.hokkenikuatsu.audiostorage.application.fake.FakeRecordingStorage
import io.github.hokkenikuatsu.audiostorage.application.fake.RejectingAudioConverter
import io.github.hokkenikuatsu.audiostorage.application.fake.TooLargeAudioConverter
import io.github.hokkenikuatsu.audiostorage.application.port.AudioConversionException
import io.github.hokkenikuatsu.audiostorage.application.port.AudioConverter
import io.github.hokkenikuatsu.audiostorage.application.port.RecordingRepository
import io.github.hokkenikuatsu.audiostorage.domain.PhraseId
import io.github.hokkenikuatsu.audiostorage.domain.RecordingKey
import io.github.hokkenikuatsu.audiostorage.domain.RecordingPath
import io.github.hokkenikuatsu.audiostorage.domain.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import java.io.ByteArrayInputStream
import java.io.InputStream

class SaveRecordingUseCaseTest :
    FunSpec({
        val key = RecordingKey(UserId(1), PhraseId(1))
        val oldPath = RecordingPath("1/1/old.wav")

        fun audio(): InputStream = ByteArrayInputStream("m4a".toByteArray())

        fun useCase(
            repository: RecordingRepository,
            storage: FakeRecordingStorage,
            converter: AudioConverter = FakeAudioConverter(),
        ) = SaveRecordingUseCase(repository, storage, converter)

        context("REQ-1.1 m4a を WAV にして保存し、組に記録する") {
            test("REQ-1.1 録音のない組に保存すると、新しいファイルに変換して書き、組にそのパスを記録して、新規として返す") {
                val repository = FakeRecordingRepository()
                val storage = FakeRecordingStorage()
                val converter = FakeAudioConverter()

                useCase(repository, storage, converter).save("1", "1", audio()) shouldBe SaveRecordingResult.Created

                val newPath = storage.allocated.single()
                converter.storageOutputs shouldBe listOf(storage.resolve(newPath))
                repository.paths shouldBe mapOf(key to newPath)
                storage.deleted.shouldBeEmpty()
            }
        }

        context("REQ-1.3 既存の録音を置き換える") {
            test("REQ-1.3 録音のある組に保存すると、組のパスを新しいファイルに切り替えて古いファイルを消し、置き換えとして返す") {
                val repository = FakeRecordingRepository().apply { paths[key] = oldPath }
                val storage = FakeRecordingStorage()

                useCase(repository, storage).save("1", "1", audio()) shouldBe SaveRecordingResult.Replaced

                val newPath = storage.allocated.single()
                repository.paths shouldBe mapOf(key to newPath)
                storage.deleted shouldBe listOf(oldPath)
            }
        }

        context("REQ-1.5 m4a として読めないファイルを拒む") {
            test("REQ-1.5 変換が m4a ではないとすれば、新しいファイルを消し、DB のパスを変えずに拒む") {
                val repository = FakeRecordingRepository().apply { paths[key] = oldPath }
                val storage = FakeRecordingStorage()

                useCase(repository, storage, RejectingAudioConverter()).save("1", "1", audio()) shouldBe
                    SaveRecordingResult.Rejected(RecordingError.UNSUPPORTED_AUDIO)

                repository.paths shouldBe mapOf(key to oldPath)
                storage.deleted shouldBe storage.allocated
            }
        }

        context("REQ-1.6 大きすぎるファイルを拒む") {
            test("REQ-1.6 変換後の WAV が上限を超えれば、新しいファイルを消し、DB のパスを変えずに拒む") {
                val repository = FakeRecordingRepository().apply { paths[key] = oldPath }
                val storage = FakeRecordingStorage()

                useCase(repository, storage, TooLargeAudioConverter()).save("1", "1", audio()) shouldBe
                    SaveRecordingResult.Rejected(RecordingError.AUDIO_FILE_TOO_LARGE)

                repository.paths shouldBe mapOf(key to oldPath)
                storage.deleted shouldBe storage.allocated
            }
        }

        context("REQ-1.7 失敗しても既存の録音を保つ") {
            test("REQ-1.7 変換に失敗すれば、新しいファイルを消し、DB のパスを変えずに例外を上げる") {
                val repository = FakeRecordingRepository().apply { paths[key] = oldPath }
                val storage = FakeRecordingStorage()

                shouldThrow<AudioConversionException> {
                    useCase(repository, storage, FailingAudioConverter()).save("1", "1", audio())
                }

                repository.paths shouldBe mapOf(key to oldPath)
                storage.deleted shouldBe storage.allocated
            }

            test("REQ-1.7 DB のパスの切り替えに失敗すれば、新しいファイルを消し、古いファイルを残して例外を上げる") {
                val repository = FailingReplacePathRecordingRepository().apply { paths[key] = oldPath }
                val storage = FakeRecordingStorage()

                shouldThrow<IllegalStateException> {
                    useCase(repository, storage).save("1", "1", audio())
                }

                repository.paths shouldBe mapOf(key to oldPath)
                storage.deleted shouldBe storage.allocated
            }
        }

        context("REQ-3.1 録音を扱う前に ID を確かめる") {
            listOf(
                Triple("ユーザー", "abc" to "1", RecordingError.USER_NOT_FOUND),
                Triple("ユーザー", "2" to "1", RecordingError.USER_NOT_FOUND),
                Triple("フレーズ", "1" to "abc", RecordingError.PHRASE_NOT_FOUND),
                Triple("フレーズ", "1" to "2", RecordingError.PHRASE_NOT_FOUND),
            ).forEach { (target, ids, error) ->
                val (userId, phraseId) = ids
                test("REQ-3.1 ${target}が見つからなければ（ユーザー ID $userId、フレーズ ID $phraseId）、変換と保存のポートを呼ばずに拒む") {
                    val repository = FakeRecordingRepository()
                    val storage = FakeRecordingStorage()
                    val converter = FakeAudioConverter()

                    useCase(repository, storage, converter).save(userId, phraseId, audio()) shouldBe
                        SaveRecordingResult.Rejected(error)

                    storage.allocated.shouldBeEmpty()
                    converter.storageOutputs.shouldBeEmpty()
                    repository.paths shouldBe emptyMap()
                }
            }
        }
    })
