#include <cstdlib>
#include <iostream>
#include "logging.hpp"
#include "window_manager.hpp"

using std::unique_ptr;
using std::cout;
using std::endl;

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
// ReSharper disable CppDFAConstantFunctionResult
int main(int argc, char **argv) {
    logging::init();
    log(logging::WARNING, "Testing logging");

    if (const unique_ptr<WindowManager> wm = WindowManager::create()) {
        wm->run();
        // ReSharper disable CppDFAUnreachableCode
        return EXIT_SUCCESS;
    }
    cout << "Failed to initialize window manager." << endl;
    return EXIT_FAILURE;
}
