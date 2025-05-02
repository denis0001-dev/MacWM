package com.macwm2

fun main() {
    val compositor = Compositor()
    val dock = Dock(compositor)

    try {
        // Start the compositor
        compositor.start()

        // Create and show the dock
        dock.create()

        // Create a test window
        val window = Window(compositor, 800, 600, "Test Window")
        window.create()

        // Keep the program running
        while (true) {
            // Main event loop
            Thread.sleep(100)
        }
    } catch (e: Exception) {
        println("Error: ${e.message}")
    } finally {
        dock.destroy()
        compositor.stop()
    }
} 