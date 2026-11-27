// adapter で使うプラグインを、ここでバージョンを付けて読み込む。
// モジュールの plugins でバージョンを付けると、そのモジュールだけ別のクラスローダーになり、Kotlin のプラグインが2回読み込まれるため
plugins {
    alias(libs.plugins.jooq.codegen) apply false
    alias(libs.plugins.flyway) apply false
}
