package org.worldcubeassociation.tnoodle.core.model.wcif

import org.worldcubeassociation.tnoodle.core.serial.SingletonIntEncoder
import java.time.Duration

data class AttemptResult(val value: Int) {
    val isSkipped
        get() = this.value == ATTEMPT_SKIPPED

    val isDnf
        get() = this.value == ATTEMPT_DNF

    val isDns
        get() = this.value == ATTEMPT_DNS

    val asDuration
        get() = Duration.ofMillis(this.value * CENTISECONDS_TO_MILLISECONDS)

    val asFmcAverage
        get() = this.value / 100f

    companion object : SingletonIntEncoder<AttemptResult>("AttemptResult") {
        const val ATTEMPT_SKIPPED = 0
        const val ATTEMPT_DNF = -1
        const val ATTEMPT_DNS = -2

        const val CENTISECONDS_TO_MILLISECONDS = 10L

        override fun encodeInstance(instance: AttemptResult) = instance.value
        override fun makeInstance(deserialized: Int) = AttemptResult(deserialized)
    }
}
