package ru.denis0001dev.macwm;

import platform.posix.EXIT_FAILURE
import platform.posix.EXIT_SUCCESS
import platform.posix.exit
import ru.denis0001dev.macwm.Logging.Level

/**
 * @brief Main function of the application.
 *
 * This function initializes Google's logging system and attempts to create a WindowManager instance.
 * If successful, it runs the WindowManager. If not, it prints an error message and exits with a failure status.
 *
 * @param argc The number of command-line arguments.
 * @param argv The array of command-line arguments.
 *
 * @return EXIT_SUCCESS if the WindowManager is successfully created and run, otherwise, EXIT_FAILURE.
 */
fun main() {
    Logging.init();
    Logging.log(Level.WARNING, "Testing logging");

    val wm = WindowManager.create()

    wm?.let {
        it.run();
        exit(EXIT_SUCCESS)
    }
    println("Failed to initialize window manager.");
    exit(EXIT_FAILURE)
}
