package com.macwm2

import kotlinx.coroutines.*
import platform.posix.*
import wayland.client.protocol.wl_surface_commit
import wayland.client.protocol.wl_surface_destroy

class Window(
    private val compositor: Compositor,
    private val width: Int,
    private val height: Int,
    private val title: String
) {
    private var surface: Int = 0
    private var xdgSurface: Int = 0
    private var xdgToplevel: Int = 0

    fun create() {
        // Create Wayland surface
        surface = wl_compositor_create_surface(compositor.getCompositor())
        if (surface == 0) {
            println("Failed to create surface")
            return
        }

        // Create XDG surface
        xdgSurface = xdg_wm_base_get_xdg_surface(compositor.getWmBase(), surface)
        if (xdgSurface == 0) {
            println("Failed to create XDG surface")
            return
        }

        // Create XDG toplevel
        xdgToplevel = xdg_surface_get_toplevel(xdgSurface)
        if (xdgToplevel == 0) {
            println("Failed to create XDG toplevel")
            return
        }

        // Set window title
        xdg_toplevel_set_title(xdgToplevel, title)

        // Commit the surface
        wl_surface_commit(surface)
    }

    fun destroy() {
        if (xdgToplevel != 0) xdg_toplevel_destroy(xdgToplevel)
        if (xdgSurface != 0) xdg_surface_destroy(xdgSurface)
        if (surface != 0) wl_surface_destroy(surface)
    }
}