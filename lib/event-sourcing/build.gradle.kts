project.ext.set("useArrow", true)

plugins {
    id("mastermind.kotlin-common-conventions")
}

dependencies {
    implementation(project(":lib:journal"))
}
