plugins {
    id("kotlin-conventions")
}

dependencies {
    implementation(project(":application"))
    implementation(project(":domain"))
    implementation(libs.spring.webmvc)
    // Spring の multipart の例外が Servlet の例外を継承している。実行時は bootstrap の Tomcat が持つので、コンパイルとテストにだけ入れる
    compileOnly(libs.jakarta.servlet.api)
    testImplementation(libs.jakarta.servlet.api)
}
