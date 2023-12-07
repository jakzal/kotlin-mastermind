rootProject.name = "mastermind"

pluginManagement {
    includeBuild("build-logic")
}

include("game")
include("lib:testkit:assertions")
include("lib:testkit:dynamic-test")
include("lib:testkit:testcontainers-eventstoredb")
include("lib:eventstore")
include("lib:eventstore-eventstoredb")
include("lib:event-sourcing")
