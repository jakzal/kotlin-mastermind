plugins {
    id("mastermind.kotlin-common-conventions")
}

dependencies {
    implementation(project(":lib:eventstore"))
    implementation(project(":lib:eventstore-eventstoredb"))
    implementation(project(":lib:event-sourcing"))
    implementation(platform("org.http4k:http4k-bom:5.26.1.0"))
    implementation("org.http4k:http4k-core")
    implementation("org.http4k:http4k-config")
    implementation("org.http4k:http4k-server-undertow")
    implementation("org.http4k:http4k-format-jackson")
    implementation("org.http4k:http4k-cloudnative")
    testImplementation("org.http4k:http4k-client-apache")
    testImplementation(project(":lib:testkit:acceptance"))
    testImplementation(project(":lib:testkit:assertions"))
    testImplementation(project(":lib:testkit:dynamic-test"))
}
