package io.github.hokkenikuatsu.audiostorage.bootstrap

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

// application のユースケースと web の Controller も Bean にするため、すべてのモジュールのパッケージを読む
@SpringBootApplication(scanBasePackages = ["io.github.hokkenikuatsu.audiostorage"])
class AudioStorageApplication

fun main(args: Array<String>) {
    runApplication<AudioStorageApplication>(*args)
}
