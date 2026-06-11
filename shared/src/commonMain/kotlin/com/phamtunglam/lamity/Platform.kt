package com.phamtunglam.lamity

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform