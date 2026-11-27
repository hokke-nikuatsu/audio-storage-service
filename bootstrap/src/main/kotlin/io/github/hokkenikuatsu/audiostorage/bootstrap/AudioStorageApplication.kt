package io.github.hokkenikuatsu.audiostorage.bootstrap

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class AudioStorageApplication

fun main(args: Array<String>) {
    runApplication<AudioStorageApplication>(*args)
}
