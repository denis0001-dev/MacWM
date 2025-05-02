@file:OptIn(ExperimentalForeignApi::class)

package com.macwm2

import kotlinx.cinterop.ExperimentalForeignApi
import wayland.client.protocol.wl_compositor_create_surface
import wayland.client.protocol.wl_surface_destroy

class Dock(private val compositor: Compositor) {
    private val items = mutableListOf<DockItem>()
    private var surface: Int = 0
    private var width: Int = 0
    private var height: Int = 0

    fun create() {
        // Create dock surface
        surface = wl_compositor_create_surface(compositor.getCompositor())
        if (surface == 0) {
            println("Failed to create dock surface")
            return
        }

        // Set dock dimensions
        width = 800
        height = 60

        // Create initial dock items
        addDefaultItems()
    }

    private fun addDefaultItems() {
        // Add some default dock items
        items.add(DockItem("Terminal", "/usr/share/icons/hicolor/48x48/apps/terminal.png"))
        items.add(DockItem("Browser", "/usr/share/icons/hicolor/48x48/apps/firefox.png"))
        items.add(DockItem("Settings", "/usr/share/icons/hicolor/48x48/apps/settings.png"))
    }

    fun destroy() {
        if (surface != 0) {
            wl_surface_destroy(surface)
        }
    }
}

data class DockItem(
    val name: String,
    val iconPath: String
) 