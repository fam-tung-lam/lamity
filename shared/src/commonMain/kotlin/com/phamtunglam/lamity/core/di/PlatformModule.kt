package com.phamtunglam.lamity.core.di

import org.koin.core.module.Module

/**
 * Dependencies that are provided per platform.
 */
expect fun platformModule(): Module
