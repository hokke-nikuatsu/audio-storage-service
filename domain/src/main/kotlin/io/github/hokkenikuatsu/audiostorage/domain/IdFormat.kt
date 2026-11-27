package io.github.hokkenikuatsu.audiostorage.domain

// ユーザー ID とフレーズ ID に共通の形式。先頭が 0 ではない数字だけの並びで、Long に収まるもの
private val ID_PATTERN = Regex("[1-9][0-9]*")

// 形式が正しければ値を、正しくなければ null を返す
internal fun parseIdValue(raw: String): Long? = if (ID_PATTERN.matches(raw)) raw.toLongOrNull() else null
