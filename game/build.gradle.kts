plugins {
    id("mastermind.kotlin-common-conventions")
}

dependencies {
    implementation(project(":lib:journal"))
    implementation(project(":lib:event-sourcing"))
    implementation(platform("org.http4k:http4k-bom:5.7.4.0"))
    implementation("org.http4k:http4k-core")
    implementation("org.http4k:http4k-server-undertow")
    implementation("org.http4k:http4k-format-jackson")
    testImplementation("org.http4k:http4k-client-apache")
    testImplementation(project(":lib:testkit:assertions"))
}
