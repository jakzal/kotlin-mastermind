plugins {
    id("mastermind.kotlin-common-conventions")
}

dependencies {
    implementation(project(":lib:eventstore"))
    implementation("com.eventstore:db-client-java:5.4.4")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.+")
    testImplementation(testFixtures(project(":lib:eventstore")))
    testImplementation(project(":lib:testkit:testcontainers-eventstoredb"))
    testImplementation(platform("org.testcontainers:testcontainers-bom:1.20.6"))
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
}
