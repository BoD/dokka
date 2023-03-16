import org.jetbrains.dependsOnMavenLocalPublication

plugins {
    id("org.jetbrains.conventions.dokka-integration-test")
    id("org.jetbrains.conventions.maven-cli-setup")
}

dependencies {
    implementation(project(":integration-tests"))
    implementation(kotlin("stdlib"))
    implementation(kotlin("test-junit"))
}

tasks.integrationTest {
    dependsOnMavenLocalPublication()

    dependsOn(tasks.installMavenBinary)
    val mvn = setupMavenProperties.mvn
    inputs.file(mvn)

    val dokka_version: String by project
    environment("DOKKA_VERSION", dokka_version)
    doFirst("workaround for https://github.com/gradle/gradle/issues/24267") {
        environment("MVN_BINARY_PATH", mvn.get().asFile.invariantSeparatorsPath)
    }
}
