#include "menu.hpp"
#include <X11/Xutil.h>

Menu::Menu(Display* dpy, Window root) : dpy(dpy) {
    menu_win = XCreateSimpleWindow(dpy, root, 0, 0, 400, 24, 0, BlackPixel(dpy, 0), WhitePixel(dpy, 0));
    XSelectInput(dpy, menu_win, ExposureMask | ButtonPressMask);
    XMapWindow(dpy, menu_win);
}

void Menu::on_button_press(const XButtonEvent& ev) {
    // Placeholder: handle menu clicks
}

void Menu::on_expose(const XExposeEvent& ev) {
    GC gc = XCreateGC(dpy, menu_win, 0, nullptr);
    XSetForeground(dpy, gc, BlackPixel(dpy, 0));
    XDrawString(dpy, menu_win, gc, 4, 16, "Menu", 4);
    XFreeGC(dpy, gc);
} 