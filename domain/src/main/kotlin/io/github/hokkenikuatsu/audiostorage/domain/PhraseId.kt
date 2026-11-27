package io.github.hokkenikuatsu.audiostorage.domain

@JvmInline
value class PhraseId(
    val value: Long,
) {
    companion object {
        // 形式が正しくなければ null を返す
        fun parse(raw: String): PhraseId? = parseIdValue(raw)?.let(::PhraseId)
    }
}
