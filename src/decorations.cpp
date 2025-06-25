#include "decorations.hpp"
#include <X11/Xutil.h>
#include <X11/cursorfont.h>
#include <vector>
#include <utility>

// Helper: RGB to pixel
static unsigned long rgb(Display* dpy, int r, int g, int b) {
    XColor color;
    color.red = r << 8; color.green = g << 8; color.blue = b << 8;
    color.flags = DoRed | DoGreen | DoBlue;
    XAllocColor(dpy, DefaultColormap(dpy, 0), &color);
    return color.pixel;
}

Decoration::Decoration(Display* dpy, Window client, std::string  title)
    : dpy(dpy), client_win(client), title(std::move(title))
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

    // Set a visible cursor
    Cursor cursor = XCreateFontCursor(dpy, XC_left_ptr);
    XDefineCursor(dpy, frame_win, cursor);

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

void Decoration::draw() const {
    GC gc = XCreateGC(dpy, frame_win, 0, nullptr);
    // Draw a nice blue titlebar
    unsigned long blue = rgb(dpy, 52, 120, 246);
    XSetForeground(dpy, gc, blue);
    XFillRectangle(dpy, frame_win, gc, 0, 0, 400, titlebar_height); // width is placeholder

    // Draw Mac-like buttons
    int btn_radius = 8, btn_y = titlebar_height / 2, spacing = 8;
    std::vector<unsigned long> colors = {
        rgb(dpy, 255, 92, 92),   // red
        rgb(dpy, 255, 189, 46),  // yellow
        rgb(dpy, 0, 202, 78)     // green
    };
    for (int i = 0; i < 3; ++i) {
        int btn_x = spacing + i * (btn_radius*2 + spacing);
        XSetForeground(dpy, gc, colors[i]);
        XFillArc(dpy, frame_win, gc, btn_x, btn_y - btn_radius, btn_radius*2, btn_radius*2, 0, 360*64);
    }

    // Draw title text
    XSetForeground(dpy, gc, WhitePixel(dpy, 0));
    XDrawString(dpy, frame_win, gc, 48, 16, title.c_str(), title.length());
    XFreeGC(dpy, gc);
}

void Decoration::on_button_press(const XButtonEvent& ev) {
    // Mac-like button hit detection
    int btn_radius = 8, btn_y = titlebar_height / 2, spacing = 8;
    for (int i = 0; i < 3; ++i) {
        int btn_x = spacing + i * (btn_radius*2 + spacing) + btn_radius;
        int dx = ev.x - btn_x;
        int dy = ev.y - btn_y;
        if (dx*dx + dy*dy <= btn_radius*btn_radius) {
            pressed_button = i+1; // 1=close, 2=min, 3=max
            return;
        }
    }
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

void Decoration::on_button_release(const XButtonEvent& ev) {
    dragging = false;
    // Mac-like button hit detection (same as press)
    int btn_radius = 8, btn_y = titlebar_height / 2, spacing = 8;
    for (int i = 0; i < 3; ++i) {
        int btn_x = spacing + i * (btn_radius*2 + spacing) + btn_radius;
        int dx = ev.x - btn_x;
        int dy = ev.y - btn_y;
        if (dx*dx + dy*dy <= btn_radius*btn_radius && pressed_button == i+1) {
            if (i == 0) {
                // Close
                XDestroyWindow(dpy, client_win);
            } else if (i == 1) {
                // Minimize
                XUnmapWindow(dpy, client_win);
            } else if (i == 2) {
                // Maximize (toggle)
                Screen* scr = DefaultScreenOfDisplay(dpy);
                XMoveResizeWindow(dpy, frame_win, 10, 10, scr->width*0.9, scr->height*0.9);
                XMoveResizeWindow(dpy, client_win, 0, titlebar_height, scr->width*0.9, scr->height*0.9 - titlebar_height);
            }
        }
    }
    pressed_button = 0;
}

void Decoration::on_motion_notify(const XMotionEvent& ev) const {
    if (dragging) {
        const int dx = ev.x_root - drag_start_x;
        const int dy = ev.y_root - drag_start_y;
        XMoveWindow(dpy, frame_win, win_start_x + dx, win_start_y + dy);
    }
} 