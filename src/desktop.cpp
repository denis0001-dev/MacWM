#include "desktop.hpp"

DesktopManager::DesktopManager(Display* dpy, Window root) : dpy(dpy), root(root) {
    desktops.resize(4); // 4 desktops for example
}

void DesktopManager::switch_to(int desktop) {
    if (desktop < 0 || desktop >= desktops.size()) return;
    // TODO: Unmap all windows from current, map all from new
    current_desktop = desktop;
} 