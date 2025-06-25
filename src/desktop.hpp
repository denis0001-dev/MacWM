#pragma once
#include <X11/Xlib.h>
#include <vector>

class DesktopManager {
public:
    DesktopManager(Display* dpy, Window root);
    void switch_to(int desktop);
private:
    Display* dpy;
    Window root;
    int current_desktop = 0;
    std::vector<std::vector<Window>> desktops;
}; 