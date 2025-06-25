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
    [[noreturn]] void run();

private:
    Display* dpy;
    Window root;
    std::vector<std::unique_ptr<Decoration>> decorations;
    std::unique_ptr<Menu> menu;
    std::unique_ptr<DesktopManager> desktop_manager;
    std::unique_ptr<HotkeyManager> hotkey_manager;

    void handle_map_request(XEvent& ev);
    void handle_destroy_notify(const XEvent& ev);
    void handle_button_press(const XEvent& ev) const;
    void handle_motion_notify(const XEvent& ev) const;
    void handle_key_press(const XEvent& ev) const;
    void handle_expose(const XEvent& ev) const;
    // ... more event handlers as needed
}; 