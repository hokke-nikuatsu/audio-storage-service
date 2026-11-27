package io.github.hokkenikuatsu.audiostorage.adapter.converter

import io.github.hokkenikuatsu.audiostorage.application.port.AudioConversionException
import io.github.hokkenikuatsu.audiostorage.application.port.AudioConverter
import io.github.hokkenikuatsu.audiostorage.application.port.StorageConversion
import io.github.hokkenikuatsu.audiostorage.domain.AudioFormat
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.io.path.readText

// ffmpeg と ffprobe のコマンドを起動して変換する。ffmpeg とのファイルの受け渡しは、作業用のディレクトリの一時ファイルで行う
class FfmpegAudioConverter(
    private val ffmpeg: String,
    private val ffprobe: String,
    // 1回の起動ごとのタイムアウト
    private val timeout: Duration,
    // 変換後の WAV の大きさの上限
    private val maxStorageBytes: Long,
    // 一時ファイルを置くディレクトリ。変換ごとにこの下にディレクトリを作り、変換の中で消す
    private val workDirectory: Path = Path.of(System.getProperty("java.io.tmpdir")),
) : AudioConverter {
    override fun toStorageFormat(
        input: InputStream,
        output: Path,
    ): StorageConversion =
        inWorkDirectory { work ->
            val source = work.resolve("input.m4a")
            Files.copy(input, source)
            if (!isAac(work, source)) return@inWorkDirectory StorageConversion.Rejected

            // サンプリングレートとチャンネル数は指定せず、元の録音のまま保つ。-fs で出力が上限に達したら書くのをやめる
            convert(
                work,
                listOf("-f", "mov", "-i", source.toString(), "-map", "0:a:0", "-c:a", "pcm_s16le"),
                listOf("-fs", maxStorageBytes.toString(), "-f", "wav", output.toString()),
            )
            if (Files.size(output) >= maxStorageBytes) StorageConversion.TooLarge else StorageConversion.Converted
        }

    override fun toTransferFormat(
        input: Path,
        format: AudioFormat,
    ): ByteArray =
        inWorkDirectory { work ->
            val target = work.resolve("output.${format.extension}")
            val encoding =
                when (format) {
                    AudioFormat.M4A -> listOf("-c:a", "aac", "-f", "mp4")
                }
            convert(work, listOf("-f", "wav", "-i", input.toString()), encoding + target.toString())
            Files.readAllBytes(target)
        }

    // 入力の形式を mov に固定し、プレイリストのような外部を読みにいく形式を受け付けない。最初の音声のストリームが AAC かを確かめる
    private fun isAac(
        work: Path,
        source: Path,
    ): Boolean {
        val probe =
            run(
                work,
                listOf(ffprobe, "-v", "error", "-f", "mov", "-select_streams", "a:0") +
                    listOf("-show_entries", "stream=codec_name", "-of", "default=nw=1:nk=1", source.toString()),
            )
        return probe.exitCode == 0 && probe.stdout.trim() == "aac"
    }

    private fun convert(
        work: Path,
        input: List<String>,
        output: List<String>,
    ) {
        val result = run(work, listOf(ffmpeg, "-nostdin", "-v", "error", "-y") + input + output)
        if (result.exitCode != 0) {
            throw AudioConversionException("ffmpeg が終了コード ${result.exitCode} で失敗した: ${result.stderr.trim()}")
        }
    }

    private fun run(
        work: Path,
        command: List<String>,
    ): ProcessResult {
        // 出力をパイプで受けると、読み終えるまでプロセスが止まりうるので、ファイルに書かせる
        val stdout = work.resolve("stdout.log")
        val stderr = work.resolve("stderr.log")
        val process =
            ProcessBuilder(command)
                .redirectOutput(stdout.toFile())
                .redirectError(stderr.toFile())
                .start()
        val finished =
            try {
                process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)
            } catch (e: InterruptedException) {
                process.destroyForcibly()
                Thread.currentThread().interrupt()
                throw AudioConversionException("${command.first()} の終了を待つあいだに割り込まれた", e)
            }
        if (!finished) {
            process.destroyForcibly().waitFor()
            throw AudioConversionException("${command.first()} が $timeout 以内に終わらなかった")
        }
        return ProcessResult(process.exitValue(), stdout.readText(), stderr.readText())
    }

    // 変換ごとに作業用のディレクトリを作り、成功しても失敗しても消す。ファイルとプロセスの入出力の失敗は、変換の失敗にする
    private fun <T> inWorkDirectory(block: (Path) -> T): T =
        try {
            val work = Files.createTempDirectory(workDirectory, "audio-conversion-")
            try {
                block(work)
            } finally {
                work.toFile().deleteRecursively()
            }
        } catch (e: IOException) {
            throw AudioConversionException("変換の一時ファイルかプロセスの入出力に失敗した", e)
        }

    private class ProcessResult(
        val exitCode: Int,
        val stdout: String,
        val stderr: String,
    )
}
