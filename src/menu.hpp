#pragma once
#include <X11/Xlib.h>

class Menu {
public:
    Menu(Display* dpy, Window root);
    void on_button_press(const XButtonEvent& ev);
    void on_expose(const XExposeEvent& ev);
private:
    Display* dpy;
    Window menu_win;
}; 