plugins {
    id("mastermind.kotlin-common-conventions")
}

dependencies {
    testImplementation(project(":lib:testkit:assertions"))
    testFixturesImplementation(project(":lib:testkit:assertions"))
}
