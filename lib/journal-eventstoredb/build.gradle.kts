plugins {
    id("mastermind.kotlin-common-conventions")
}

dependencies {
    implementation(project(":lib:journal"))
    implementation("com.eventstore:db-client-java:4.2.0")
}
