package io.github.hokkenikuatsu.audiostorage.application

import io.github.hokkenikuatsu.audiostorage.application.fake.FailingAudioConverter
import io.github.hokkenikuatsu.audiostorage.application.fake.FakeAudioConverter
import io.github.hokkenikuatsu.audiostorage.application.fake.FakeRecordingRepository
import io.github.hokkenikuatsu.audiostorage.application.fake.FakeRecordingStorage
import io.github.hokkenikuatsu.audiostorage.application.port.AudioConversionException
import io.github.hokkenikuatsu.audiostorage.domain.AudioFormat
import io.github.hokkenikuatsu.audiostorage.domain.PhraseId
import io.github.hokkenikuatsu.audiostorage.domain.RecordingKey
import io.github.hokkenikuatsu.audiostorage.domain.RecordingPath
import io.github.hokkenikuatsu.audiostorage.domain.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class GetRecordingUseCaseTest :
    FunSpec({
        val key = RecordingKey(UserId(1), PhraseId(1))
        val path = RecordingPath("1/1/1.wav")

        // 組 (1, 1) に録音が保存された DB
        fun repositoryWithRecording() = FakeRecordingRepository().apply { paths[key] = path }

        fun useCase(
            repository: FakeRecordingRepository = repositoryWithRecording(),
            converter: FakeAudioConverter = FakeAudioConverter(),
        ) = GetRecordingUseCase(repository, FakeRecordingStorage(), converter)

        context("REQ-2.1 録音を送受信用のフォーマットに変換して返す") {
            test("REQ-2.1 組に保存された録音を m4a に変換して返す") {
                val converter = FakeAudioConverter()

                val result = useCase(converter = converter).get("1", "1", "m4a")

                result.shouldBeInstanceOf<GetRecordingResult.Found>()
                result.format shouldBe AudioFormat.M4A
                result.content shouldBe FakeAudioConverter.TRANSFER_CONTENT
                converter.transferInputs shouldBe listOf(FakeRecordingStorage.ROOT.resolve(path.value))
            }
        }

        context("REQ-2.2 録音がない") {
            test("REQ-2.2 組に録音が保存されていなければ、録音がないとして拒み、変換しない") {
                val converter = FakeAudioConverter()

                useCase(FakeRecordingRepository(), converter).get("1", "1", "m4a") shouldBe
                    GetRecordingResult.Rejected(RecordingError.RECORDING_NOT_FOUND)
                converter.transferInputs.shouldBeEmpty()
            }
        }

        context("REQ-2.3 対応していないフォーマット") {
            listOf("wav", "mp3", "M4A", "").forEach { format ->
                test("REQ-2.3 フォーマットが「$format」なら、対応していないフォーマットとして拒み、変換しない") {
                    val converter = FakeAudioConverter()

                    useCase(converter = converter).get("1", "1", format) shouldBe
                        GetRecordingResult.Rejected(RecordingError.UNSUPPORTED_FORMAT)
                    converter.transferInputs.shouldBeEmpty()
                }
            }

            test("REQ-2.3 録音がなくても、フォーマットが違えば対応していないフォーマットとして拒む") {
                useCase(FakeRecordingRepository()).get("1", "1", "wav") shouldBe
                    GetRecordingResult.Rejected(RecordingError.UNSUPPORTED_FORMAT)
            }
        }

        context("REQ-2.4 読み出しや変換の失敗") {
            test("REQ-2.4 変換に失敗したら、例外を上げる") {
                shouldThrow<AudioConversionException> {
                    useCase(converter = FailingAudioConverter()).get("1", "1", "m4a")
                }
            }
        }

        context("REQ-3.1 録音を扱う前に ID を確かめる") {
            listOf("2" to "1", "1" to "2").forEach { (userId, phraseId) ->
                test("REQ-3.1 ユーザー ID が $userId、フレーズ ID が $phraseId で見つからなければ、録音を変換しない") {
                    val converter = FakeAudioConverter()

                    val result = useCase(converter = converter).get(userId, phraseId, "m4a")

                    result.shouldBeInstanceOf<GetRecordingResult.Rejected>()
                    converter.transferInputs.shouldBeEmpty()
                }
            }

            test("REQ-3.1 ID が見つからなければ、フォーマットが違っても ID の誤りとして拒む") {
                useCase().get("2", "1", "wav") shouldBe GetRecordingResult.Rejected(RecordingError.USER_NOT_FOUND)
            }
        }

        context("REQ-3.2 ユーザーが見つからない") {
            listOf("2", "abc").forEach { userId ->
                test("REQ-3.2 ユーザー ID が $userId なら、ユーザーが見つからないとして拒む") {
                    useCase().get(userId, "1", "m4a") shouldBe
                        GetRecordingResult.Rejected(RecordingError.USER_NOT_FOUND)
                }
            }
        }

        context("REQ-3.3 フレーズが見つからない") {
            listOf("2", "abc").forEach { phraseId ->
                test("REQ-3.3 フレーズ ID が $phraseId なら、フレーズが見つからないとして拒む") {
                    useCase().get("1", phraseId, "m4a") shouldBe
                        GetRecordingResult.Rejected(RecordingError.PHRASE_NOT_FOUND)
                }
            }
        }
    })
