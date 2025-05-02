#include "x_utils.hpp"
#include "assert.hpp"
#include "logging.hpp"

Window XUtils::createSimpleWindow(
    Display* display,
    const Window parent,
    const int x,
    const int y,
    const unsigned int width,
    const unsigned int height,
    const unsigned int border_width,
    const unsigned long border_color,
    const unsigned long background_color
) {
    return XCreateSimpleWindow(
        display,
        parent,
        x,
        y,
        width,
        height,
        border_width,
        border_color,
        background_color
    );
}

void XUtils::mapWindow(Display* display, const Window window) {
    XMapWindow(display, window);
}

void XUtils::unmapWindow(Display* display, const Window window) {
    XUnmapWindow(display, window);
}

void XUtils::destroyWindow(Display* display, const Window window) {
    XDestroyWindow(display, window);
}

void XUtils::reparentWindow(Display* display, const Window window, const Window parent, const int x, const int y) {
    XReparentWindow(display, window, parent, x, y);
}

XWindowAttributes XUtils::getWindowAttributes(Display* display, const Window window) {
    XWindowAttributes attrs;
    assert::check(XGetWindowAttributes(display, window, &attrs), "XGetWindowAttributes failed");
    return attrs;
}

void XUtils::configureWindow(
    Display* display,
    const Window window,
    const int x,
    const int y,
    const int width,
    const int height
) {
    XWindowChanges changes;
    changes.x = x;
    changes.y = y;
    changes.width = width;
    changes.height = height;
    XConfigureWindow(display, window, CWX | CWY | CWWidth | CWHeight, &changes);
}

void XUtils::moveWindow(Display* display, const Window window, const int x, const int y) {
    XMoveWindow(display, window, x, y);
}

void XUtils::resizeWindow(
    Display* display,
    const Window window,
    const unsigned int width,
    const unsigned int height
) {
    XResizeWindow(display, window, width, height);
}

void XUtils::moveResizeWindow(
    Display* display,
    const Window window,
    const int x,
    const int y,
    const unsigned int width,
    const unsigned int height
) {
    XMoveResizeWindow(display, window, x, y, width, height);
}

void XUtils::selectInput(Display* display, const Window window, const long event_mask) {
    XSelectInput(display, window, event_mask);
}

void XUtils::grabButton(
    Display* display,
    const unsigned int button,
    const unsigned int modifiers,
    const Window grab_window,
    const bool owner_events,
    const unsigned int event_mask,
    const int pointer_mode,
    const int keyboard_mode,
    const Window confine_to,
    const Cursor cursor
) {
    XGrabButton(display, button, modifiers, grab_window, owner_events,
               event_mask, pointer_mode, keyboard_mode, confine_to, cursor);
}

void XUtils::grabKey(
    Display* display,
    const int keycode,
    const unsigned int modifiers,
    const Window grab_window,
    const bool owner_events,
    const int pointer_mode,
    const int keyboard_mode
) {
    XGrabKey(display, keycode, modifiers, grab_window, owner_events,
            pointer_mode, keyboard_mode);
}

void XUtils::raiseWindow(Display* display, const Window window) {
    XRaiseWindow(display, window);
}

void XUtils::iconifyWindow(Display* display, const Window window, const int screen) {
    XIconifyWindow(display, window, screen);
}

void XUtils::setInputFocus(Display* display, const Window window, const int revert_to, const Time time) {
    XSetInputFocus(display, window, revert_to, time);
}

Atom XUtils::internAtom(Display* display, const std::string& name, const bool only_if_exists) {
    return XInternAtom(display, name.c_str(), only_if_exists);
}

bool XUtils::getWMProtocols(Display* display, const Window window, Atom** protocols, int* count) {
    return XGetWMProtocols(display, window, protocols, count);
}

void XUtils::sendClientMessage(
    Display* display,
    const Window window,
    const Atom message_type,
    const long data0,
    const long data1,
    const long data2,
    const long data3,
    const long data4
) {
    XEvent msg = {};
    msg.xclient.type = ClientMessage;
    msg.xclient.message_type = message_type;
    msg.xclient.window = window;
    msg.xclient.format = 32;
    msg.xclient.data.l[0] = data0;
    msg.xclient.data.l[1] = data1;
    msg.xclient.data.l[2] = data2;
    msg.xclient.data.l[3] = data3;
    msg.xclient.data.l[4] = data4;
    assert::check(XSendEvent(display, window, false, 0, &msg), "XSendEvent failed");
}

std::string XUtils::getErrorText(Display* display, const int error_code) {
    constexpr int MAX_ERROR_TEXT_LENGTH = 1024;
    char error_text[MAX_ERROR_TEXT_LENGTH];
    XGetErrorText(display, error_code, error_text, sizeof(error_text));
    return { error_text };
}

std::string XUtils::getRequestCodeName(const unsigned char request_code) {
    return XRequestCodeToString(request_code);
}

void XUtils::grabServer(Display* display) {
    XGrabServer(display);
}

void XUtils::ungrabServer(Display* display) {
    XUngrabServer(display);
}

bool XUtils::queryTree(
    Display* display,
    const Window window,
    Window* root_return,
    Window* parent_return,
    Window** children_return,
    unsigned int* nchildren_return
) {
    return XQueryTree(display, window, root_return, parent_return,
                     children_return, nchildren_return);
}

void XUtils::addToSaveSet(Display* display, const Window window) {
    XAddToSaveSet(display, window);
}

void XUtils::removeFromSaveSet(Display* display, const Window window) {
    XRemoveFromSaveSet(display, window);
}

// Window XUtils::createButton(
//     Display* display,
//     const Window parent,
//     const int x,
//     const int y,
//     const unsigned long color,
//     const int size
// ) {
//     const Window button = XCreateSimpleWindow(display, parent, x, y, size, size, 0, 0, color);
//
//     // Create a pixmap to define the shape of the button
//     const Pixmap shape_mask = XCreatePixmap(display, button, size, size, 1);
//     // ReSharper disable CppLocalVariableMayBeConst
//     GC shape_gc = XCreateGC(display, shape_mask, 0, nullptr);
//     XSetForeground(display, shape_gc, 0);
//     XFillRectangle(display, shape_mask, shape_gc, 0, 0, size, size);
//     XSetForeground(display, shape_gc, 1);
//     XFillArc(display, shape_mask, shape_gc, 0, 0, size, size, 0, 360 * 64);
//     XShapeCombineMask(display, button, ShapeBounding, 0, 0, shape_mask, ShapeSet);
//     XFreePixmap(display, shape_mask);
//     XFreeGC(display, shape_gc);
//
//     XMapWindow(display, button);
//     return button;
// }