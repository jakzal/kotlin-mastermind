plugins {
    id("mastermind.kotlin-common-conventions")
}

dependencies {
    implementation(platform("org.testcontainers:testcontainers-bom:1.20.1"))
    implementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("com.eventstore:db-client-java:5.4.4")
    testImplementation("org.slf4j:slf4j-api:2.0.14")
}
