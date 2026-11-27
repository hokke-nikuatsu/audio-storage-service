plugins {
    id("kotlin-conventions")
}

dependencies {
    implementation(project(":application"))
    implementation(project(":domain"))
    implementation(libs.spring.webmvc)
}
