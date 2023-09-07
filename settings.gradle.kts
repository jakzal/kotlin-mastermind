rootProject.name = "mastermind"

pluginManagement {
    includeBuild("build-logic")
}

include("game")
include("lib:testkit:assertions")
include("lib:testkit:testcontainers-eventstoredb")
include("lib:journal")
include("lib:journal-eventstoredb")
include("lib:event-sourcing")
