plugins {
    kotlin("jvm") version "1.9.0"
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(19)
}

dependencies {
    testImplementation(platform("org.http4k:http4k-bom:5.6.1.0"))
    testImplementation("org.http4k:http4k-core")
    testImplementation("org.http4k:http4k-server-undertow")
    testImplementation("org.http4k:http4k-client-apache")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
}
