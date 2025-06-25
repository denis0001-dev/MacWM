#include "window_manager.hpp"
#include <iostream>
#include <algorithm>

WindowManager::WindowManager() {
    dpy = XOpenDisplay(nullptr);
    if (!dpy) {
        std::cerr << "Cannot open display\n";
        exit(1);
    }
    root = DefaultRootWindow(dpy);
    XSelectInput(dpy, root, SubstructureRedirectMask | SubstructureNotifyMask | ButtonPressMask | KeyPressMask);
    menu = std::make_unique<Menu>(dpy, root);
    desktop_manager = std::make_unique<DesktopManager>(dpy, root);
    hotkey_manager = std::make_unique<HotkeyManager>(dpy, root);
}

WindowManager::~WindowManager() {
    XCloseDisplay(dpy);
}

[[noreturn]] void WindowManager::run() {
    while (true) {
        XEvent ev;
        XNextEvent(dpy, &ev);
        switch (ev.type) {
            case MapRequest: handle_map_request(ev); break;
            case DestroyNotify: handle_destroy_notify(ev); break;
            case ButtonPress: handle_button_press(ev); break;
            case MotionNotify: handle_motion_notify(ev); break;
            case KeyPress: handle_key_press(ev); break;
            case Expose: handle_expose(ev); break;
        }
    }
}

void WindowManager::handle_map_request(XEvent& ev) {
    // Create decoration for new window
    auto deco = std::make_unique<Decoration>(dpy, ev.xmaprequest.window, "Window");
    deco->draw();
    decorations.push_back(std::move(deco));
}

void WindowManager::handle_destroy_notify(const XEvent& ev) {
    // Remove decoration for destroyed window
    decorations.erase(std::remove_if(decorations.begin(), decorations.end(),
        [&](const std::unique_ptr<Decoration>& deco) {
            return deco->client() == ev.xdestroywindow.window;
        }), decorations.end());
}

void WindowManager::handle_button_press(const XEvent& ev) const {
    // Pass to decorations for move/resize or to menu
    for (auto& deco : decorations) {
        if (deco->frame() == ev.xbutton.window) {
            deco->on_button_press(ev.xbutton);
            return;
        }
    }
    menu->on_button_press(ev.xbutton);
}

void WindowManager::handle_motion_notify(const XEvent& ev) const {
    for (auto& deco : decorations) {
        deco->on_motion_notify(ev.xmotion);
    }
}

void WindowManager::handle_key_press(const XEvent& ev) const {
    hotkey_manager->on_key_press(ev.xkey);
}

void WindowManager::handle_expose(const XEvent& ev) const {
    for (auto& deco : decorations) {
        if (deco->frame() == ev.xexpose.window) {
            deco->draw();
        }
    }
    menu->on_expose(ev.xexpose);
} 