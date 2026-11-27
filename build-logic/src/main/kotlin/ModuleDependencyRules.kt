import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.kotlin.dsl.withType

// モジュールごとに、依存してよいモジュールの表。依存の向きの規則はここにだけ書く（.kiro/steering/structure.md）
private val allowedModuleDependencies: Map<String, Set<String>> =
    mapOf(
        ":domain" to emptySet(),
        ":application" to setOf(":domain"),
        ":adapter" to setOf(":application", ":domain"),
        ":web" to setOf(":application", ":domain"),
        ":bootstrap" to setOf(":domain", ":application", ":adapter", ":web"),
    )

// api の依存は使う側にも推移的に見えるので、application から domain への依存にだけ使う
private val apiModuleDependencies: Set<Pair<String, String>> = setOf(":application" to ":domain")

private const val RULE_SOURCE = "根拠: .kiro/steering/structure.md、build-logic/src/main/kotlin/ModuleDependencyRules.kt"

// 依存が規則に反していれば、その理由を返す。反していなければ null を返す
fun moduleDependencyViolation(
    from: String,
    to: String,
    configuration: String,
): String? {
    val allowed = allowedModuleDependencies[from]
    return when {
        allowed == null -> "$from は、依存してよいモジュールの表にない。$RULE_SOURCE"
        to !in allowed -> "$from から $to への依存（$configuration）は、依存してよいモジュールの表にない。$RULE_SOURCE"
        configuration.isApi() && (from to to) !in apiModuleDependencies ->
            "$from から $to への依存に api（$configuration）を使っている。api は :application から :domain への依存にだけ使い、ほかは implementation で宣言する。$RULE_SOURCE"
        else -> null
    }
}

// compileOnlyApi や testFixturesApi のように、名前が Api で終わる構成も、依存を使う側に見せる
private fun String.isApi(): Boolean = this == "api" || endsWith("Api")

// モジュールの依存を宣言した時点で表と照らし、反していればビルドを失敗させる。テストの依存も対象にする
fun Project.checkModuleDependencies() {
    configurations.configureEach {
        val configuration = name
        dependencies.withType<ProjectDependency>().configureEach {
            moduleDependencyViolation(project.path, path, configuration)?.let { throw GradleException(it) }
        }
    }
}
