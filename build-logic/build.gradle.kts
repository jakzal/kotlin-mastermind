plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    val kotlinVersion = "1.9.10"
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
}
