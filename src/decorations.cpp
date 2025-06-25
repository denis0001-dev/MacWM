#include "decorations.hpp"
#include <X11/Xutil.h>

Decoration::Decoration(Display* dpy, Window client, const std::string& title)
    : dpy(dpy), client_win(client), title(title)
{
    XWindowAttributes attr;
    XGetWindowAttributes(dpy, client, &attr);

    // Create frame window
    frame_win = XCreateSimpleWindow(
        dpy, DefaultRootWindow(dpy),
        attr.x, attr.y,
        attr.width, attr.height + titlebar_height,
        border_width, BlackPixel(dpy, 0), WhitePixel(dpy, 0)
    );

    // Select events for frame
    XSelectInput(dpy, frame_win, ExposureMask | ButtonPressMask | ButtonReleaseMask | PointerMotionMask);

    // Reparent client into frame
    XReparentWindow(dpy, client, frame_win, 0, titlebar_height);

    // Map frame (and client)
    XMapWindow(dpy, frame_win);
    XMapWindow(dpy, client);
}

Decoration::~Decoration() {
    XDestroyWindow(dpy, frame_win);
}

void Decoration::draw() {
    // Draw titlebar (simple filled rectangle + text)
    GC gc = XCreateGC(dpy, frame_win, 0, nullptr);
    XSetForeground(dpy, gc, BlackPixel(dpy, 0));
    XFillRectangle(dpy, frame_win, gc, 0, 0, 400, titlebar_height); // width is placeholder
    XSetForeground(dpy, gc, WhitePixel(dpy, 0));
    XDrawString(dpy, frame_win, gc, 4, 16, title.c_str(), title.length());
    XFreeGC(dpy, gc);
}

void Decoration::on_button_press(const XButtonEvent& ev) {
    if (ev.y < titlebar_height) {
        // Start dragging
        dragging = true;
        drag_start_x = ev.x_root;
        drag_start_y = ev.y_root;
        XWindowAttributes attr;
        XGetWindowAttributes(dpy, frame_win, &attr);
        win_start_x = attr.x;
        win_start_y = attr.y;
    }
}

void Decoration::on_motion_notify(const XMotionEvent& ev) {
    if (dragging) {
        int dx = ev.x_root - drag_start_x;
        int dy = ev.y_root - drag_start_y;
        XMoveWindow(dpy, frame_win, win_start_x + dx, win_start_y + dy);
    }
} 