#pragma once
#include <X11/Xlib.h>

class HotkeyManager {
public:
    HotkeyManager(Display* dpy, Window root);
    void on_key_press(const XKeyEvent& ev);
private:
    Display* dpy;
    Window root;
}; 