package com.macwm2

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cValue
import kotlinx.coroutines.*
import platform.posix.*
import wayland.client.core.wlDisplay
import wayland.client.core.wl_display_connect
import wayland.client.core.wl_display_create
import wayland.client.core.wl_display_disconnect
import wayland.client.core.wl_display_dispatch
import kotlin.native.concurrent.*

class Compositor {
    @OptIn(ExperimentalForeignApi::class)
    private var display: CPointer<wlDisplay>? = null
    private var running = true
    private val scope = CoroutineScope(Dispatchers.Default + Job())

    @OptIn(ExperimentalForeignApi::class)
    fun start() {
        // Initialize Wayland display
        display = wl_display_connect(null)
        if (display == null) {
            println("Failed to create Wayland display")
            return
        }
        this.display = display

        // Create event loop
        scope.launch {
            while (running) {
                wl_display_dispatch(display)
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    fun stop() {
        running = false
        scope.cancel()
        wl_display_disconnect(display)
    }
}