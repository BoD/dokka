plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
}

dependencies {
    compileOnly(projects.core)
    compileOnly(projects.subprojects.analysisKotlinApi)

    implementation(projects.subprojects.analysisMarkdownJb)
    implementation(projects.subprojects.analysisJavaPsi)
    api("org.jetbrains.intellij.deps.fastutil:intellij-deps-fastutil:8.5.8-11") // for [DokkaCompactVirtualFileSet]

    api(libs.kotlin.compiler)

    testImplementation(projects.core.contentMatcherTestUtils)
    testImplementation(projects.core.testApi)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)

    // TODO [beresnev] get rid of it
    compileOnly(libs.kotlinx.coroutines.core)
}
