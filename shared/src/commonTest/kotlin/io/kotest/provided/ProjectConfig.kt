package io.kotest.provided

import io.kotest.core.config.AbstractProjectConfig

class ProjectConfig : AbstractProjectConfig() {
    override val coroutineTestScope = true
    override val globalAssertSoftly = true
}
