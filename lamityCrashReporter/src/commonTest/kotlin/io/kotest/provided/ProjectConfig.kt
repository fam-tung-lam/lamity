package io.kotest.provided

import io.kotest.core.config.AbstractProjectConfig

class ProjectConfig : AbstractProjectConfig() {
    override val globalAssertSoftly = true
}
