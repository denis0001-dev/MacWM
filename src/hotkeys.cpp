#include "hotkeys.hpp"
#include <X11/keysym.h>
#include <iostream>

HotkeyManager::HotkeyManager(Display* dpy, Window root) : dpy(dpy), root(root) {}

void HotkeyManager::on_key_press(const XKeyEvent& ev) {
    KeySym sym = XLookupKeysym(const_cast<XKeyEvent*>(&ev), 0);
    if (sym == XK_F1) {
        std::cout << "F1 pressed!\n";
    }
    // Add more hotkeys as needed
} 