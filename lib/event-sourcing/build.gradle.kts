plugins {
    id("mastermind.kotlin-common-conventions")
}

dependencies {
    implementation(project(":lib:eventstore"))
    testImplementation(project(":lib:testkit:assertions"))
}
