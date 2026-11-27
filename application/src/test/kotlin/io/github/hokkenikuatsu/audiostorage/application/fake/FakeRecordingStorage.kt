package io.github.hokkenikuatsu.audiostorage.application.fake

import io.github.hokkenikuatsu.audiostorage.application.port.RecordingStorage
import io.github.hokkenikuatsu.audiostorage.domain.RecordingKey
import io.github.hokkenikuatsu.audiostorage.domain.RecordingPath
import java.nio.file.Path

// 割り当てと削除を呼ばれた順に記録し、ディスクには触らない
class FakeRecordingStorage : RecordingStorage {
    val allocated = mutableListOf<RecordingPath>()
    val deleted = mutableListOf<RecordingPath>()

    // 割り当てたまま消していないパス
    val remaining: List<RecordingPath>
        get() = allocated - deleted.toSet()

    override fun allocate(key: RecordingKey): RecordingPath =
        RecordingPath("${key.userId.value}/${key.phraseId.value}/${allocated.size + 1}.wav").also { allocated += it }

    override fun resolve(path: RecordingPath): Path = ROOT.resolve(path.value)

    override fun delete(path: RecordingPath) {
        deleted += path
    }

    companion object {
        val ROOT: Path = Path.of("/fake-storage")
    }
}
