package io.github.hokkenikuatsu.audiostorage.application

import io.github.hokkenikuatsu.audiostorage.application.fake.FakeRecordingRepository
import io.github.hokkenikuatsu.audiostorage.domain.PhraseId
import io.github.hokkenikuatsu.audiostorage.domain.RecordingKey
import io.github.hokkenikuatsu.audiostorage.domain.UserId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class RecordingKeyResolverTest :
    FunSpec({
        val resolver =
            RecordingKeyResolver(
                FakeRecordingRepository(userIds = setOf(1, Long.MAX_VALUE), phraseIds = setOf(1, Long.MAX_VALUE)),
            )

        // 見つからない扱いになる ID と、その理由
        val unknownIds =
            listOf(
                "abc" to "数字ではない",
                "0" to "先頭が 0",
                "9223372036854775808" to "Long を超える",
                "2" to "登録されていない",
            )

        context("REQ-3.1 録音を扱う前に ID を確かめる") {
            test("REQ-3.1 登録されたユーザー ID とフレーズ ID を、録音の組にする") {
                resolver.resolve("1", "1") shouldBe
                    RecordingKeyResolution.Resolved(RecordingKey(UserId(1), PhraseId(1)))
            }

            test("REQ-3.1 Long に収まる最大の ID を受け付ける") {
                resolver.resolve("9223372036854775807", "9223372036854775807") shouldBe
                    RecordingKeyResolution.Resolved(RecordingKey(UserId(Long.MAX_VALUE), PhraseId(Long.MAX_VALUE)))
            }
        }

        context("REQ-3.2 ユーザーが見つからない") {
            unknownIds.forEach { (raw, reason) ->
                test("REQ-3.2 ユーザー ID が$reason（$raw）なら、ユーザーが見つからない") {
                    resolver.resolve(raw, "1") shouldBe RecordingKeyResolution.Rejected(RecordingError.USER_NOT_FOUND)
                }
            }

            test("REQ-3.2 ユーザー ID とフレーズ ID の両方が見つからなければ、ユーザーが見つからない") {
                resolver.resolve("abc", "abc") shouldBe RecordingKeyResolution.Rejected(RecordingError.USER_NOT_FOUND)
            }
        }

        context("REQ-3.3 フレーズが見つからない") {
            unknownIds.forEach { (raw, reason) ->
                test("REQ-3.3 フレーズ ID が$reason（$raw）なら、フレーズが見つからない") {
                    resolver.resolve("1", raw) shouldBe RecordingKeyResolution.Rejected(RecordingError.PHRASE_NOT_FOUND)
                }
            }
        }
    })
