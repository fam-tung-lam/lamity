package io.kotest.provided

import io.kotest.core.config.AbstractProjectConfig
import kotlin.time.Duration.Companion.seconds

/** Sets shared Kotest defaults for this module's test suite. */
class ProjectConfig : AbstractProjectConfig() {
    override val timeout = 30.seconds
    override val coroutineTestScope: Boolean = true
    override val globalAssertSoftly: Boolean = true
}
