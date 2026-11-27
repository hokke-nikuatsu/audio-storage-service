package io.github.hokkenikuatsu.audiostorage.bootstrap

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext

@SpringBootTest
class AudioStorageApplicationTest(
    private val context: ApplicationContext,
) : FunSpec({
        test("Spring のコンテキストが起動する") {
            context.getBean(AudioStorageApplication::class.java).shouldNotBeNull()
        }
    })
