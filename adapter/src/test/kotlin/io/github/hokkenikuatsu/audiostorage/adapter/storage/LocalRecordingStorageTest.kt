package io.github.hokkenikuatsu.audiostorage.adapter.storage

import io.github.hokkenikuatsu.audiostorage.domain.PhraseId
import io.github.hokkenikuatsu.audiostorage.domain.RecordingKey
import io.github.hokkenikuatsu.audiostorage.domain.RecordingPath
import io.github.hokkenikuatsu.audiostorage.domain.UserId
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.collections.shouldNotContainDuplicates
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import java.nio.file.Files

class LocalRecordingStorageTest :
    FunSpec({
        val root = tempdir().toPath()
        val storage = LocalRecordingStorage(root)
        val key = RecordingKey(UserId(1), PhraseId(2))

        context("REQ-5.3 録音をサーバーのディスクにファイルとして保存する") {
            test("REQ-5.3 組のディレクトリの下に、UUID のファイル名のパスを割り当て、そのディレクトリを作る") {
                val path = storage.allocate(key)

                path.value shouldMatch Regex("1/2/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.wav")
                Files.isDirectory(root.resolve("1/2")) shouldBe true
            }

            test("REQ-5.3 同じ組でも、割り当てるたびに違うパスを返す") {
                List(3) { storage.allocate(key) }.shouldNotContainDuplicates()
            }

            test("REQ-5.3 相対パスを、保存先のルートの下のパスにする") {
                storage.resolve(RecordingPath("1/2/a.wav")) shouldBe root.resolve("1/2/a.wav")
            }

            test("REQ-5.3 割り当てたパスのファイルを消す") {
                val path = storage.allocate(key)
                Files.writeString(storage.resolve(path), "audio")

                storage.delete(path)

                Files.exists(storage.resolve(path)) shouldBe false
            }

            test("REQ-5.3 ないファイルの削除は失敗しない") {
                storage.delete(RecordingPath("1/2/missing.wav"))
            }

            test("REQ-5.3 消せないファイルの削除も失敗しない") {
                // 中身のあるディレクトリは消せない
                val path = storage.allocate(key)
                Files.createDirectories(storage.resolve(path).resolve("child"))

                storage.delete(path)

                Files.exists(storage.resolve(path)) shouldBe true
            }
        }
    })
