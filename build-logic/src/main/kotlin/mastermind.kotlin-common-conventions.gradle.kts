val useTestFixtures = project.projectDir.resolve("src/testFixtures").isDirectory

plugins {
    kotlin("jvm")
    id("java-test-fixtures") apply false
}

if (useTestFixtures) project.plugins.apply("java-test-fixtures")

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation("io.arrow-kt:arrow-core:2.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")
    testImplementation(platform("org.junit:junit-bom:5.12.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    if (useTestFixtures) {
        testFixturesImplementation("io.arrow-kt:arrow-core:2.0.1")
        testFixturesImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")
        testFixturesImplementation(platform("org.junit:junit-bom:5.12.0"))
        testFixturesImplementation("org.junit.jupiter:junit-jupiter")
    }
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
}
