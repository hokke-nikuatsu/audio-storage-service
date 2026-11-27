package io.github.hokkenikuatsu.audiostorage.application

import io.github.hokkenikuatsu.audiostorage.application.port.RecordingRepository
import io.github.hokkenikuatsu.audiostorage.domain.PhraseId
import io.github.hokkenikuatsu.audiostorage.domain.RecordingKey
import io.github.hokkenikuatsu.audiostorage.domain.UserId

// 生の ID の形式と登録を確かめ、録音の組にする。ユーザー ID を先に確かめる
internal class RecordingKeyResolver(
    private val repository: RecordingRepository,
) {
    fun resolve(
        rawUserId: String,
        rawPhraseId: String,
    ): RecordingKeyResolution {
        val userId =
            UserId.parse(rawUserId)?.takeIf(repository::userExists)
                ?: return RecordingKeyResolution.Rejected(RecordingError.USER_NOT_FOUND)
        val phraseId = PhraseId.parse(rawPhraseId)?.takeIf(repository::phraseExists)
        return if (phraseId == null) {
            RecordingKeyResolution.Rejected(RecordingError.PHRASE_NOT_FOUND)
        } else {
            RecordingKeyResolution.Resolved(RecordingKey(userId, phraseId))
        }
    }
}

internal sealed interface RecordingKeyResolution {
    data class Resolved(
        val key: RecordingKey,
    ) : RecordingKeyResolution

    data class Rejected(
        val error: RecordingError,
    ) : RecordingKeyResolution
}
