import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.string.shouldContain

class ModuleDependencyRulesTest :
    FunSpec({
        context("表のとおりの依存は通す") {
            withData(
                nameFn = { (from, to, configuration) -> "$from から $to への $configuration" },
                Triple(":application", ":domain", "api"),
                Triple(":adapter", ":application", "implementation"),
                Triple(":adapter", ":domain", "testImplementation"),
                Triple(":web", ":application", "implementation"),
                Triple(":bootstrap", ":adapter", "implementation"),
                Triple(":bootstrap", ":web", "implementation"),
            ) { (from, to, configuration) ->
                moduleDependencyViolation(from, to, configuration).shouldBeNull()
            }
        }

        context("表にない依存は止める") {
            withData(
                nameFn = { (from, to, configuration) -> "$from から $to への $configuration" },
                Triple(":domain", ":application", "implementation"),
                Triple(":application", ":adapter", "implementation"),
                Triple(":application", ":adapter", "testImplementation"),
                Triple(":adapter", ":web", "implementation"),
                Triple(":web", ":adapter", "implementation"),
            ) { (from, to, configuration) ->
                moduleDependencyViolation(from, to, configuration).shouldNotBeNull() shouldContain "表にない"
            }
        }

        test("api は application から domain への依存にだけ使える") {
            moduleDependencyViolation(":adapter", ":application", "api").shouldNotBeNull() shouldContain "api"
            moduleDependencyViolation(":adapter", ":application", "compileOnlyApi").shouldNotBeNull() shouldContain "api"
        }

        test("表にないモジュールからの依存は止める") {
            moduleDependencyViolation(":other", ":domain", "implementation").shouldNotBeNull() shouldContain ":other"
        }
    })
