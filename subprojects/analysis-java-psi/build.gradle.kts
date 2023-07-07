plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
}

dependencies {
    compileOnly(projects.core)

    api(libs.intellij.java.psi.api)

    implementation(projects.subprojects.analysisMarkdownJb)

    implementation(libs.intellij.java.psi.impl)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.jsoup)
}
