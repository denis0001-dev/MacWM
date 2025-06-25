#pragma once
#include <X11/Xlib.h>
#include <vector>
#include <memory>
#include "decorations.hpp"
#include "menu.hpp"
#include "desktop.hpp"
#include "hotkeys.hpp"

class WindowManager {
public:
    WindowManager();
    ~WindowManager();
    void run();

private:
    Display* dpy;
    Window root;
    std::vector<std::unique_ptr<Decoration>> decorations;
    std::unique_ptr<Menu> menu;
    std::unique_ptr<DesktopManager> desktop_manager;
    std::unique_ptr<HotkeyManager> hotkey_manager;

    void handle_map_request(XEvent& ev);
    void handle_destroy_notify(XEvent& ev);
    void handle_button_press(XEvent& ev);
    void handle_motion_notify(XEvent& ev);
    void handle_key_press(XEvent& ev);
    void handle_expose(XEvent& ev);
    // ... more event handlers as needed
}; 