import org.jetbrains.registerDokkaArtifactPublication

plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
    id("org.jetbrains.conventions.maven-publish")
}

dependencies {
    compileOnly(projects.core)

    implementation(projects.plugins.base)
    implementation(projects.plugins.gfm)

    implementation(kotlin("reflect"))

    testImplementation(projects.core.testApi)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
}

registerDokkaArtifactPublication("jekyllPlugin") {
    artifactId = "jekyll-plugin"
}
