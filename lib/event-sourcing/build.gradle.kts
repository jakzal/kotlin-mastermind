plugins {
    id("mastermind.kotlin-common-conventions")
}

dependencies {
    implementation(project(":lib:journal"))
    testImplementation(project(":lib:testkit:assertions"))
}
