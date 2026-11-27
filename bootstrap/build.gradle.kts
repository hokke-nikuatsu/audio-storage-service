plugins {
    id("spring-boot-conventions")
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":application"))
    implementation(project(":adapter"))
    implementation(project(":web"))
    implementation(libs.spring.boot.starter)
    implementation(libs.kotlin.reflect)

    testImplementation(libs.spring.boot.test)
    testImplementation(libs.kotest.extensions.spring)
}
