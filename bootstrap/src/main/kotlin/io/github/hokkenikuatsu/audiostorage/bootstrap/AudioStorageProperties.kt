package io.github.hokkenikuatsu.audiostorage.bootstrap

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.util.unit.DataSize
import java.nio.file.Path
import java.time.Duration

@ConfigurationProperties("audio-storage")
data class AudioStorageProperties(
    // 録音のファイルを置くディレクトリ
    val storageRoot: Path,
    val ffmpeg: String,
    val ffprobe: String,
    // ffmpeg と ffprobe の1回の起動ごとのタイムアウト
    val conversionTimeout: Duration,
    // 変換後の WAV の大きさの上限
    val maxStorageSize: DataSize,
)
