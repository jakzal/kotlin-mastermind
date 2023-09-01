plugins {
    id("mastermind.kotlin-common-conventions")
}

dependencies {
    implementation(platform("org.http4k:http4k-bom:5.6.1.0"))
    implementation("org.http4k:http4k-core")
    implementation("org.http4k:http4k-server-undertow")
    implementation("org.http4k:http4k-format-jackson")
    testImplementation("org.http4k:http4k-client-apache")
    implementation("io.arrow-kt:arrow-core:1.2.0")
}
