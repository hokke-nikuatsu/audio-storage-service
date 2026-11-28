plugins {
    id("spring-boot-conventions")
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":application"))
    implementation(project(":adapter"))
    implementation(project(":web"))
    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.spring.boot.starter.jooq)
    implementation(libs.spring.boot.starter.flyway)
    implementation(libs.kotlin.reflect)
    runtimeOnly(libs.sqlite.jdbc)

    testImplementation(libs.spring.boot.test)
    testImplementation(libs.kotest.extensions.spring)
}
