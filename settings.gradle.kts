rootProject.name = "mastermind"

pluginManagement {
    includeBuild("build-logic")
}

include("game")
include("lib:testkit")
include("lib:journal")
include("lib:event-sourcing")
