package mastermind.testkit.testcontainers.eventstoredb

import org.testcontainers.utility.DockerImageName
import org.testcontainers.utility.ImageNameSubstitutor

private const val ARM_IMAGE = "ghcr.io/kurrent-io/eventstore:24.2.0-alpha-arm64v8"

private val isRunOnAppleSilicon: Boolean
    get() = "aarch64" == System.getProperty("os.arch", "unknown")

/**
 * This is required until the arm image is officially published.
 * See https://github.com/EventStore/EventStore/issues/2380#issuecomment-1527744520
 */
class EventStoreDbImageNameSubstitutor : ImageNameSubstitutor() {
    override fun apply(original: DockerImageName): DockerImageName =
        if (original.isEventStoreImage() && isRunOnAppleSilicon) DockerImageName.parse(ARM_IMAGE)
        else original

    override fun getDescription(): String = "Substitutes EventStoreDB image name if run on Apple silicon."
}

private fun DockerImageName.isEventStoreImage(): Boolean = "ghcr.io/kurrent-io/eventstore" == unversionedPart
