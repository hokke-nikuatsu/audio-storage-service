package io.github.hokkenikuatsu.audiostorage.bootstrap

import io.github.hokkenikuatsu.audiostorage.adapter.converter.FfmpegAudioConverter
import io.github.hokkenikuatsu.audiostorage.adapter.db.JooqRecordingRepository
import io.github.hokkenikuatsu.audiostorage.adapter.storage.LocalRecordingStorage
import org.jooq.DSLContext
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

// Spring に依存しないアダプタに、設定値と jOOQ を渡して Bean にする
@Configuration
@EnableConfigurationProperties(AudioStorageProperties::class)
class AdapterConfiguration(
    private val properties: AudioStorageProperties,
) {
    @Bean
    fun recordingRepository(dsl: DSLContext) = JooqRecordingRepository(dsl)

    @Bean
    fun recordingStorage() = LocalRecordingStorage(properties.storageRoot)

    @Bean
    fun audioConverter() =
        FfmpegAudioConverter(
            ffmpeg = properties.ffmpeg,
            ffprobe = properties.ffprobe,
            timeout = properties.conversionTimeout,
            maxStorageBytes = properties.maxStorageSize.toBytes(),
        )
}
