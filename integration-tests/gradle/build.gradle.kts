import org.jetbrains.dependsOnMavenLocalPublication

plugins {
    id("org.jetbrains.conventions.dokka-integration-test")
}

dependencies {
    implementation(project(":integration-tests"))
    implementation(kotlin("stdlib"))
    implementation(kotlin("test-junit"))
    implementation(gradleTestKit())

    val jsoup_version: String by project
    implementation("org.jsoup:jsoup:$jsoup_version")
}

tasks.integrationTest {
    val dokka_version: String by project
    environment("DOKKA_VERSION", dokka_version)
    inputs.dir(file("projects"))
    dependsOnMavenLocalPublication()

    javaLauncher.set(javaToolchains.launcherFor {
        // kotlinx.coroutines requires Java 11+
        languageVersion.set(dokkaBuild.testJavaLauncherVersion.map {
            maxOf(it, JavaLanguageVersion.of(11))
        })
    })
}
