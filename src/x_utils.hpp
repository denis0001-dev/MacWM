#ifndef X_UTILS_HPP
#define X_UTILS_HPP

extern "C" {
#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include <X11/extensions/shape.h>
}

#include <string>
#include <memory>
#include <iostream>
#include "util.hpp"

class XUtils {
public:
    // Window creation and management
    static Window createSimpleWindow(Display* display, Window parent, int x, int y,
                                   unsigned int width, unsigned int height,
                                   unsigned int border_width, unsigned long border_color,
                                   unsigned long background_color);

    static void mapWindow(Display* display, Window window);
    static void unmapWindow(Display* display, Window window);
    static void destroyWindow(Display* display, Window window);
    static void reparentWindow(Display* display, Window window, Window parent, int x, int y);

    // Window attributes and geometry
    static XWindowAttributes getWindowAttributes(Display* display, Window window);
    static void configureWindow(
        Display* display,
        Window window,
        int x,
        int y,
        int width,
        int height
    );
    static void moveWindow(Display* display, Window window, int x, int y);
    static void resizeWindow(Display* display, Window window, unsigned int width, unsigned int height);
    static void moveResizeWindow(Display* display, Window window, int x, int y,
                               unsigned int width, unsigned int height);

    // Event handling
    static void selectInput(Display* display, Window window, long event_mask);
    static void grabButton(Display* display, unsigned int button, unsigned int modifiers,
                          Window grab_window, bool owner_events, unsigned int event_mask,
                          int pointer_mode, int keyboard_mode, Window confine_to, Cursor cursor);
    static void grabKey(Display* display, int keycode, unsigned int modifiers,
                       Window grab_window, bool owner_events, int pointer_mode, int keyboard_mode);

    // Window state
    static void raiseWindow(Display* display, Window window);
    static void iconifyWindow(Display* display, Window window, int screen);
    static void setInputFocus(Display* display, Window window, int revert_to, Time time);

    // Atoms and properties
    static Atom internAtom(Display* display, const std::string& name, bool only_if_exists);
    static bool getWMProtocols(Display* display, Window window, Atom** protocols, int* count);
    static void sendClientMessage(Display* display, Window window, Atom message_type,
                                long data0, long data1 = 0, long data2 = 0, long data3 = 0, long data4 = 0);

    // Error handling
    static std::string getErrorText(Display* display, int error_code);
    static std::string getRequestCodeName(unsigned char request_code);

    // Server control
    static void grabServer(Display* display);
    static void ungrabServer(Display* display);

    // Window tree operations
    static bool queryTree(Display* display, Window window, Window* root_return,
                         Window* parent_return, Window** children_return,
                         unsigned int* nchildren_return);

    // Save set operations
    static void addToSaveSet(Display* display, Window window);
    static void removeFromSaveSet(Display* display, Window window);

    // Button creation
    // static Window createButton(Display* display, Window parent, int x, int y,
    //                          unsigned long color, int size = 14);
};

#endif // X_UTILS_HPP