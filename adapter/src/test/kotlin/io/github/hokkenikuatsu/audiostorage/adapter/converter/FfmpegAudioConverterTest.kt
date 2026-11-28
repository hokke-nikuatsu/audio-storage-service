package io.github.hokkenikuatsu.audiostorage.adapter.converter

import io.github.hokkenikuatsu.audiostorage.application.port.AudioConversionException
import io.github.hokkenikuatsu.audiostorage.application.port.StorageConversion
import io.github.hokkenikuatsu.audiostorage.domain.AudioFormat
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.paths.shouldNotExist
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import kotlin.io.path.inputStream
import kotlin.io.path.listDirectoryEntries

// テスト用の音声の置き場所。Gradle はモジュールのディレクトリでテストを走らせる。ファイルは scripts/testdata/generate-audio.sh で作る
private val AUDIO_DIR: Path = Path.of("..", "testdata", "audio")

// 受け付ける m4a（MP4 のコンテナに AAC の音声を入れたもの）
private val ACCEPTED_M4A: Path = AUDIO_DIR.resolve("eagle-has-landed.m4a")

class FfmpegAudioConverterTest :
    FunSpec({
        val workDirectory = tempdir().toPath()
        val outputDirectory = tempdir().toPath()

        fun converter(
            timeout: Duration = Duration.ofSeconds(30),
            maxStorageBytes: Long = 200_000_000,
        ) = FfmpegAudioConverter(
            ffmpeg = "ffmpeg",
            ffprobe = "ffprobe",
            timeout = timeout,
            maxStorageBytes = maxStorageBytes,
            workDirectory = workDirectory,
        )

        fun newOutput(): Path = Files.createTempFile(outputDirectory, "recording-", ".wav").also(Files::delete)

        fun toStorageFormat(
            input: Path,
            output: Path = newOutput(),
            converter: FfmpegAudioConverter = converter(),
        ): StorageConversion = input.inputStream().use { converter.toStorageFormat(it, output) }

        afterTest {
            // 一時ファイルは、成功しても失敗しても変換の中で消す
            workDirectory.listDirectoryEntries().shouldBeEmpty()
        }

        context("REQ-1.1 m4a を WAV にして保存する") {
            test("REQ-1.1 受け付ける m4a を、サンプリングレートとチャンネル数を保った WAV にする") {
                val output = newOutput()

                toStorageFormat(ACCEPTED_M4A, output) shouldBe StorageConversion.Converted

                val source = probe(ACCEPTED_M4A)
                val wav = probe(output)
                wav.getValue("format_name") shouldBe "wav"
                wav.getValue("codec_name") shouldBe "pcm_s16le"
                wav.getValue("sample_rate") shouldBe source.getValue("sample_rate")
                wav.getValue("channels") shouldBe source.getValue("channels")
            }
        }

        context("REQ-1.5 送受信用フォーマットの音声として読めないファイルを拒む") {
            listOf(
                "not-audio.m4a" to "テキストを m4a と名付けたもの",
                "eagle-has-landed.wav" to "WAV",
                "eagle-has-landed.mp3" to "MP3",
                "eagle-has-landed-alac.m4a" to "AAC ではない ALAC の m4a",
                "eagle-has-landed-truncated.m4a" to "途中で切れた m4a",
            ).forEach { (file, description) ->
                test("REQ-1.5 $description（$file）を拒み、WAV を書かない") {
                    val output = newOutput()

                    toStorageFormat(AUDIO_DIR.resolve(file), output) shouldBe StorageConversion.Rejected

                    output.shouldNotExist()
                }
            }
        }

        context("REQ-1.6 大きすぎるファイルを拒む") {
            test("REQ-1.6 変換後の WAV が上限に達したら、上限超過にする") {
                toStorageFormat(ACCEPTED_M4A, converter = converter(maxStorageBytes = 1_000)) shouldBe
                    StorageConversion.TooLarge
            }
        }

        context("REQ-2.1 保存した録音を m4a にして返す") {
            test("REQ-2.1 WAV を、サンプリングレートとチャンネル数を保った AAC の m4a にする") {
                val wav = newOutput()
                toStorageFormat(ACCEPTED_M4A, wav)

                val content = converter().toTransferFormat(wav, AudioFormat.M4A)

                val m4a = Files.write(outputDirectory.resolve("transferred.m4a"), content)

                val source = probe(ACCEPTED_M4A)
                val transferred = probe(m4a)
                transferred.getValue("format_name") shouldContain "mp4"
                transferred.getValue("codec_name") shouldBe "aac"
                transferred.getValue("sample_rate") shouldBe source.getValue("sample_rate")
                transferred.getValue("channels") shouldBe source.getValue("channels")
            }
        }

        context("REQ-2.4 変換の失敗") {
            test("REQ-2.4 WAV として読めないファイルの変換は、変換の失敗の例外にする") {
                shouldThrow<AudioConversionException> {
                    converter().toTransferFormat(AUDIO_DIR.resolve("not-audio.m4a"), AudioFormat.M4A)
                }
            }

            test("REQ-2.4 タイムアウトまでに終わらなければ、変換の失敗の例外にする") {
                shouldThrow<AudioConversionException> {
                    toStorageFormat(ACCEPTED_M4A, converter = converter(timeout = Duration.ZERO))
                }
            }
        }
    })

// ffprobe で、コンテナの形式と最初の音声のストリームの中身を読む
private fun probe(file: Path): Map<String, String> {
    val process =
        ProcessBuilder(
            "ffprobe",
            "-v",
            "error",
            "-select_streams",
            "a:0",
            "-show_entries",
            "format=format_name:stream=codec_name,sample_rate,channels",
            "-of",
            "default=nw=1",
            file.toString(),
        ).redirectErrorStream(true).start()
    val output = process.inputStream.bufferedReader().use { it.readText() }
    process.waitFor() shouldBe 0
    return output.lines().filter { "=" in it }.associate { it.substringBefore("=") to it.substringAfter("=") }
}
