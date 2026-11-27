package io.github.hokkenikuatsu.audiostorage.application.fake

import io.github.hokkenikuatsu.audiostorage.application.port.AudioConversionException
import io.github.hokkenikuatsu.audiostorage.application.port.AudioConverter
import io.github.hokkenikuatsu.audiostorage.application.port.StorageConversion
import io.github.hokkenikuatsu.audiostorage.domain.AudioFormat
import java.io.InputStream
import java.nio.file.Path

// どの入力も m4a として受け付け、変換に成功する。変換に渡された保存先と読み出し元を、呼ばれた順に記録する
open class FakeAudioConverter : AudioConverter {
    val storageOutputs = mutableListOf<Path>()
    val transferInputs = mutableListOf<Path>()

    override fun toStorageFormat(
        input: InputStream,
        output: Path,
    ): StorageConversion {
        storageOutputs.add(output)
        return storageConversion()
    }

    override fun toTransferFormat(
        input: Path,
        format: AudioFormat,
    ): ByteArray {
        transferInputs.add(input)
        return transferContent()
    }

    protected open fun storageConversion(): StorageConversion = StorageConversion.Converted

    protected open fun transferContent(): ByteArray = TRANSFER_CONTENT

    companion object {
        // 送受信用のフォーマットに変換した結果として返す中身
        val TRANSFER_CONTENT: ByteArray = "m4a".toByteArray()
    }
}

// 入力を m4a ではないとして拒む
class RejectingAudioConverter : FakeAudioConverter() {
    override fun storageConversion(): StorageConversion = StorageConversion.Rejected
}

// 変換後の WAV が上限を超えたとする
class TooLargeAudioConverter : FakeAudioConverter() {
    override fun storageConversion(): StorageConversion = StorageConversion.TooLarge
}

// 保存用への変換も送受信用への変換も、例外で失敗する
class FailingAudioConverter : FakeAudioConverter() {
    override fun storageConversion(): StorageConversion = throw AudioConversionException("保存用のフォーマットへの変換に失敗した")

    override fun transferContent(): ByteArray = throw AudioConversionException("送受信用のフォーマットへの変換に失敗した")
}
