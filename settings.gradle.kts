rootProject.name = "mastermind"

pluginManagement {
    includeBuild("build-logic")
}

include("game")
include("lib:testkit:assertions")
include("lib:journal")
include("lib:event-sourcing")
