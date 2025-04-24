package mastermind.testkit.testcontainers.eventstoredb

import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

private const val defaultImageName = "ghcr.io/kurrent-io/eventstore:24.2.0-alpine"

class EventStoreDbContainer<SELF : EventStoreDbContainer<SELF>> :
    GenericContainer<SELF>(DockerImageName.parse(defaultImageName)) {

    init {
        addExposedPort(2113)
        addEnv("EVENTSTORE_INSECURE", "true")
    }

    val connectionString: String
        get() = String.format("esdb://localhost:%d?tls=false", getMappedPort(2113))
}