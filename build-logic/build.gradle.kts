plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    val kotlinVersion = "2.1.10"
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
}
