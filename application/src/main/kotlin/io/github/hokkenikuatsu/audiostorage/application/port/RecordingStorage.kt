package io.github.hokkenikuatsu.audiostorage.application.port

import io.github.hokkenikuatsu.audiostorage.domain.RecordingKey
import io.github.hokkenikuatsu.audiostorage.domain.RecordingPath
import java.nio.file.Path

interface RecordingStorage {
    // 組のディレクトリを作り、まだ使われていないパスを返す
    fun allocate(key: RecordingKey): RecordingPath

    fun resolve(path: RecordingPath): Path

    // ファイルがなくても、消せなくても例外を上げない（消せなければ警告のログを残す）
    fun delete(path: RecordingPath)
}
