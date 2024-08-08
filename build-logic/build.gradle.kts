plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    val kotlinVersion = "2.0.0"
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
}
