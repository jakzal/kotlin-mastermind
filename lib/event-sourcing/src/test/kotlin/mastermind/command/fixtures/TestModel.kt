package mastermind.command.fixtures

data class TestCommand(val id: String)

data class TestEvent(val id: String)

data class TestError(val cause: String)

data class TestState(val history: List<String>)
