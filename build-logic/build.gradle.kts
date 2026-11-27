plugins {
    `kotlin-dsl`
}

// モジュールに適用するプラグインを、version catalog のバージョンでこのビルドのクラスパスに入れる
fun plugin(plugin: Provider<PluginDependency>): Provider<String> =
    plugin.map { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" }

dependencies {
    implementation(plugin(libs.plugins.kotlin.jvm))
    implementation(plugin(libs.plugins.kotlin.spring))
    implementation(plugin(libs.plugins.spring.boot))
    implementation(plugin(libs.plugins.detekt))
    implementation(plugin(libs.plugins.ktlint))
}
