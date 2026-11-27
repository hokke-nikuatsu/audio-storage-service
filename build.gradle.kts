plugins {
    base
    // adapter で使うプラグインを、ここでバージョンを付けて読み込む。
    // モジュールの plugins でバージョンを付けると、そのモジュールだけ別のクラスローダーになり、Kotlin のプラグインが2回読み込まれるため
    alias(libs.plugins.jooq.codegen) apply false
    alias(libs.plugins.flyway) apply false
}

// build-logic は別のビルドなので、そのテストをこのビルドの check に含める
tasks.check {
    dependsOn(gradle.includedBuild("build-logic").task(":check"))
}
