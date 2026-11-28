plugins {
    id("kotlin-conventions")
    id("org.jooq.jooq-codegen-gradle")
    id("org.flywaydb.flyway")
}

// Flyway のプラグインがマイグレーションを流すときのクラスパス。コード生成のための SQLite を作るときにだけ使う
val flywayMigration = configurations.create("flywayMigration")

// コード生成の入力。初期データ（db/seed）は生成するコードに要らないので、スキーマのマイグレーションだけを流す
val schemaMigrations = layout.projectDirectory.dir("src/main/resources/db/migration")
val codegenDatabase = layout.buildDirectory.file("jooq-codegen/schema.db")
val codegenDatabaseUrl = codegenDatabase.map { "jdbc:sqlite:${it.asFile.absolutePath}" }

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
    url = codegenDatabaseUrl.get()
    locations = arrayOf("filesystem:${schemaMigrations.asFile.absolutePath}")
}

tasks.flywayMigrate {
    inputs.dir(schemaMigrations)
    outputs.file(codegenDatabase)
    // 前のビルドの DB が残っていると、変えたマイグレーションを流せないので、毎回作り直す
    doFirst {
        codegenDatabase.get().asFile.run {
            delete()
            parentFile.mkdirs()
        }
    }
}

jooq {
    configuration {
        jdbc {
            driver = "org.sqlite.JDBC"
            url = codegenDatabaseUrl.get()
        }
        generator {
            name = "org.jooq.codegen.KotlinGenerator"
            database {
                name = "org.jooq.meta.sqlite.SQLiteDatabase"
                excludes = "flyway_schema_history"
                forcedTypes {
                    // SQLite の INTEGER は 64 ビットなので、ID を Long で扱えるようにする
                    forcedType {
                        name = "BIGINT"
                        includeTypes = "INTEGER"
                    }
                }
            }
            target {
                packageName = "io.github.hokkenikuatsu.audiostorage.adapter.db.jooq"
            }
        }
    }
}

tasks.jooqCodegen {
    dependsOn(tasks.flywayMigrate)
    // DB のファイルは jOOQ のタスクの入力にならないので、スキーマが変わったら生成し直すよう、マイグレーションを入力にする
    inputs.dir(schemaMigrations)
}

// jOOQ のプラグインは生成したコードを Java のソースにだけ加えるので、Kotlin のコンパイルにも加える
kotlin.sourceSets.main {
    kotlin.srcDir(tasks.jooqCodegen)
}

// 生成したコードは、書き方と質のチェックから外す
val generatedCode = "**/adapter/db/jooq/**"

ktlint {
    filter {
        exclude(generatedCode)
    }
}

tasks.withType<dev.detekt.gradle.Detekt>().configureEach {
    exclude(generatedCode)
}
