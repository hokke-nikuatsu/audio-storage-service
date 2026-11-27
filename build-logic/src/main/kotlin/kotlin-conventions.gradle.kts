// すべてのモジュールに共通のビルドの設定。モジュールの build.gradle.kts には、そのモジュールに固有のことだけを書く

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("dev.detekt")
    id("org.jlleitschuh.gradle.ktlint")
}

val libs = the<VersionCatalogsExtension>().named("libs")

fun library(alias: String) = libs.findLibrary(alias).get()

kotlin {
    jvmToolchain(25)
    compilerOptions {
        allWarningsAsErrors = true
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

detekt {
    buildUponDefaultConfig = true
}

ktlint {
    version = libs.findVersion("ktlint").get().requiredVersion
}

dependencies {
    // ライブラリのバージョンは Spring Boot の BOM に任せる。BOM は制約だけを足し、ライブラリそのものは入れない
    implementation(platform(library("spring-boot-dependencies")))
    testImplementation(library("kotest-runner-junit5"))
    testImplementation(library("kotest-assertions-core"))
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

checkModuleDependencies()
