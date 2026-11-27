package io.github.hokkenikuatsu.audiostorage.domain

// 録音を見分ける、ユーザー ID とフレーズ ID の組
data class RecordingKey(
    val userId: UserId,
    val phraseId: PhraseId,
)
