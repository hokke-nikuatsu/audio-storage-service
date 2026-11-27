package io.github.hokkenikuatsu.audiostorage.domain

// 録音のファイルの、保存先のルートからの相対パス
@JvmInline
value class RecordingPath(
    val value: String,
)
