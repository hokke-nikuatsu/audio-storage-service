plugins {
    id("kotlin-conventions")
}

dependencies {
    api(project(":domain"))
    implementation(libs.spring.context)
}
