plugins {
    id("mastermind.kotlin-common-conventions")
}

dependencies {
    implementation(project(":lib:journal"))
    implementation("com.eventstore:db-client-java:4.2.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.+")
    testImplementation(testFixtures(project(":lib:journal")))
    testImplementation(project(":lib:testkit:testcontainers-eventstoredb"))
    testImplementation(platform("org.testcontainers:testcontainers-bom:1.19.2"))
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
}
