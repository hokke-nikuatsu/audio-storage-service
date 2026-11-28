package io.github.hokkenikuatsu.audiostorage.bootstrap

import com.zaxxer.hikari.HikariDataSource
import io.github.hokkenikuatsu.audiostorage.application.port.RecordingRepository
import io.github.hokkenikuatsu.audiostorage.domain.PhraseId
import io.github.hokkenikuatsu.audiostorage.domain.UserId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.nio.file.Files
import javax.sql.DataSource

@SpringBootTest
class AudioStorageApplicationTest(
    private val context: ApplicationContext,
) : FunSpec({
        context("REQ-4.2 初期データの ID が登録された状態で受け付ける") {
            test("REQ-4.2 起動すると、初期データのユーザー ID とフレーズ ID 1〜3 が DB に入っている") {
                val repository = context.getBean(RecordingRepository::class.java)
                (1L..3L).forEach { id ->
                    repository.userExists(UserId(id)) shouldBe true
                    repository.phraseExists(PhraseId(id)) shouldBe true
                }
            }
        }

        test("SQLite への接続は1本で、外部キーの検査が有効になっている") {
            val dataSource = context.getBean(DataSource::class.java)
            dataSource.unwrap(HikariDataSource::class.java).maximumPoolSize shouldBe 1
            dataSource.connection.use { connection ->
                connection.createStatement().use { statement ->
                    statement.executeQuery("PRAGMA foreign_keys").use { result ->
                        result.next()
                        result.getInt(1) shouldBe 1
                    }
                }
            }
        }
    }) {
    companion object {
        // 起動するたびに空の DB と保存先から始める
        private val dataDir = Files.createTempDirectory("audio-storage-test")

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("audio-storage.data-dir") { dataDir.toString() }
        }
    }
}
