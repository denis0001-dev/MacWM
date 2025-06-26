#include <thread>

#include "window_manager.hpp"

int main() {
    constexpr std::chrono::milliseconds timespan(1000 * 5); // or whatever
    std::this_thread::sleep_for(timespan);
    WindowManager wm;
    wm.run();
    return 0;
}