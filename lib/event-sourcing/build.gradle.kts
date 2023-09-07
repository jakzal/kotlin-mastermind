plugins {
    id("mastermind.kotlin-common-conventions")
}

dependencies {
    implementation(project(":lib:journal"))
    implementation("io.arrow-kt:arrow-core:1.2.1")
}