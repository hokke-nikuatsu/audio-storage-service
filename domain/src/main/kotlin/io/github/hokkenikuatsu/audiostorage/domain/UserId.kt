package io.github.hokkenikuatsu.audiostorage.domain

@JvmInline
value class UserId(
    val value: Long,
) {
    companion object {
        // 形式が正しくなければ null を返す
        fun parse(raw: String): UserId? = parseIdValue(raw)?.let(::UserId)
    }
}
