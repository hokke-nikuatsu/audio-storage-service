package io.github.hokkenikuatsu.audiostorage.domain

// 送受信用のフォーマット
enum class AudioFormat(
    val extension: String,
) {
    M4A("m4a"),
    ;

    companion object {
        // パスの文字列から読み取る。小文字の拡張子だけを受け付け、対応していなければ null を返す
        fun parse(raw: String): AudioFormat? = entries.find { it.extension == raw }
    }
}
