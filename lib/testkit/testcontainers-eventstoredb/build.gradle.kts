plugins {
    id("mastermind.kotlin-common-conventions")
}

dependencies {
    implementation("org.testcontainers:testcontainers:1.19.0")
    testImplementation("org.testcontainers:junit-jupiter:1.19.0")
    testImplementation("com.eventstore:db-client-java:4.3.0")
    testImplementation("org.slf4j:slf4j-api:2.0.9")
}
