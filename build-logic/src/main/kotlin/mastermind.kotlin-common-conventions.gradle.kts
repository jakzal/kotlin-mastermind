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
    implementation("io.arrow-kt:arrow-core:1.2.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation(platform("org.junit:junit-bom:5.10.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    if (useTestFixtures) {
        testFixturesImplementation("io.arrow-kt:arrow-core:1.2.4")
        testFixturesImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
        testFixturesImplementation(platform("org.junit:junit-bom:5.10.3"))
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
