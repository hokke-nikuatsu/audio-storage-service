plugins {
    id("kotlin-conventions")
    id("org.jooq.jooq-codegen-gradle")
    id("org.flywaydb.flyway")
}

// Flyway のプラグインがマイグレーションを流すときのクラスパス。コード生成のための SQLite を作るときにだけ使う
val flywayMigration by configurations.creating

dependencies {
    implementation(project(":application"))
    implementation(project(":domain"))
    implementation(libs.jooq)
    implementation(libs.slf4j.api)

    jooqCodegen(platform(libs.spring.boot.dependencies))
    jooqCodegen(libs.sqlite.jdbc)
    flywayMigration(platform(libs.spring.boot.dependencies))
    flywayMigration(libs.sqlite.jdbc)
    flywayMigration(libs.flyway.core)

    testImplementation(libs.sqlite.jdbc)
    testImplementation(libs.flyway.core)
}

flyway {
    configurations = arrayOf(flywayMigration.name)
}
