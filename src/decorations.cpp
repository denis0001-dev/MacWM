#include "decorations.hpp"
#include <X11/Xutil.h>
#include <X11/cursorfont.h>
#include <X11/extensions/shape.h>
#include <vector>
#include <utility>
#include <iostream>

// Helper: RGB to pixel
static unsigned long rgb(Display* dpy, const int r, const int g, const int b) {
    XColor color;
    color.red = r << 8; color.green = g << 8; color.blue = b << 8;
    color.flags = DoRed | DoGreen | DoBlue;
    XAllocColor(dpy, DefaultColormap(dpy, 0), &color);
    return color.pixel;
}

// Helper: Create a rounded rectangle mask for window shaping
static Pixmap create_rounded_mask(Display* dpy, const Window win, const int width, const int height, int radius) {
    const Pixmap mask = XCreatePixmap(dpy, win, width, height, 1);
    // ReSharper disable CppLocalVariableMayBeConst
    GC gc = XCreateGC(dpy, mask, 0, nullptr);
    XSetForeground(dpy, gc, 0);
    XFillRectangle(dpy, mask, gc, 0, 0, width, height);
    XSetForeground(dpy, gc, 1);
    // Draw corners
    XFillArc(dpy, mask, gc, 0, 0, 2*radius, 2*radius, 0, 360*64); // top-left
    XFillArc(dpy, mask, gc, width-2*radius-1, 0, 2*radius, 2*radius, 0, 360*64); // top-right
    XFillArc(dpy, mask, gc, 0, height-2*radius-1, 2*radius, 2*radius, 0, 360*64); // bottom-left
    XFillArc(dpy, mask, gc, width-2*radius-1, height-2*radius-1, 2*radius, 2*radius, 0, 360*64); // bottom-right
    // Draw edges and center
    XFillRectangle(dpy, mask, gc, radius, 0, width-2*radius, height);
    XFillRectangle(dpy, mask, gc, 0, radius, width, height-2*radius);
    XFreeGC(dpy, gc);
    return mask;
}

Decoration::Decoration(Display* dpy, Window client, std::string  title)
    : dpy(dpy), client_win(client), title(std::move(title))
{
    std::cerr << "[Decoration] Creating for client_win=" << client << std::endl;
    XWindowAttributes attr;
    XGetWindowAttributes(dpy, client, &attr);

    // Fetch the real window title
    XTextProperty prop;
    std::string win_title = "Window";
    if (XGetWMName(dpy, client, &prop) && prop.value) {
        win_title = reinterpret_cast<char*>(prop.value);
        XFree(prop.value);
    }
    title = win_title;

    int frame_w = attr.width;
    int frame_h = attr.height + titlebar_height;

    // Create frame window with correct size
    frame_win = XCreateSimpleWindow(
        dpy, DefaultRootWindow(dpy),
        attr.x, attr.y,
        frame_w, frame_h,
        border_width, BlackPixel(dpy, 0), WhitePixel(dpy, 0)
    );
    std::cerr << "[Decoration] Created frame_win=" << frame_win << std::endl;

    // Set rounded corners using X Shape extension
    int radius = 16; // Adjust for more/less roundness
    Pixmap mask = create_rounded_mask(dpy, frame_win, frame_w, frame_h, radius);
    std::cerr << "[Decoration] Created mask=" << mask << std::endl;
    XShapeCombineMask(dpy, frame_win, ShapeBounding, 0, 0, mask, ShapeSet);
    XFreePixmap(dpy, mask);
    std::cerr << "[Decoration] Freed mask=" << mask << std::endl;

    // Create cursors once
    resize_cursor = XCreateFontCursor(dpy, XC_bottom_right_corner);
    move_cursor = XCreateFontCursor(dpy, XC_fleur);
    normal_cursor = XCreateFontCursor(dpy, XC_left_ptr);
    std::cerr << "[Decoration] Created cursors: resize=" << resize_cursor << ", move=" << move_cursor << ", normal=" << normal_cursor << std::endl;
    XDefineCursor(dpy, frame_win, normal_cursor);

    // Select events for frame
    XSelectInput(dpy, frame_win, ExposureMask | ButtonPressMask | ButtonReleaseMask | PointerMotionMask);

    // Reparent client at (0, titlebar_height)
    XReparentWindow(dpy, client, frame_win, 0, titlebar_height);
    std::cerr << "[Decoration] Reparented client_win=" << client << " into frame_win=" << frame_win << std::endl;

    // Map frame, then client
    XMapWindow(dpy, frame_win);
    XRaiseWindow(dpy, frame_win);
    XMapWindow(dpy, client);
    std::cerr << "[Decoration] Mapped frame_win and client_win" << std::endl;
}

Decoration::~Decoration() {
    std::cerr << "[Decoration] Destroying frame_win=" << frame_win << ", client_win=" << client_win << std::endl;
    XFreeCursor(dpy, resize_cursor);
    XFreeCursor(dpy, move_cursor);
    XFreeCursor(dpy, normal_cursor);
    std::cerr << "[Decoration] Freed cursors" << std::endl;
    XDestroyWindow(dpy, frame_win);
    std::cerr << "[Decoration] Destroyed frame_win=" << frame_win << std::endl;
}

void Decoration::draw() const {
    std::cerr << "[Decoration] draw() for frame_win=" << frame_win << std::endl;
    XWindowAttributes attr;
    XGetWindowAttributes(dpy, frame_win, &attr);
    int frame_w = attr.width;
    GC gc = XCreateGC(dpy, frame_win, 0, nullptr);
    // Draw a nice blue titlebar
    unsigned long blue = rgb(dpy, 52, 120, 246);
    XSetForeground(dpy, gc, blue);
    XFillRectangle(dpy, frame_win, gc, 0, 0, frame_w, titlebar_height);
    // Draw Mac-like buttons
    int btn_radius = 8, btn_y = titlebar_height / 2, spacing = 8;
    int btns_width = 3 * (btn_radius * 2 + spacing);
    int text_x = btns_width + spacing * 2; // Start text after buttons
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
    XDrawString(dpy, frame_win, gc, text_x, 16, title.c_str(), title.length());
    XFreeGC(dpy, gc);
    std::cerr << "[Decoration] draw() done for frame_win=" << frame_win << std::endl;
}

bool Decoration::is_on_border(const int x, const int y) const {
    int width, height;
    Window root_return;
    int x_return, y_return;
    unsigned int border_width, depth;
    XGetGeometry(
        dpy,
        frame_win,
        &root_return,
        &x_return,
        &y_return,
        reinterpret_cast<unsigned int *>(&width),
        reinterpret_cast<unsigned int *>(&height),
        &border_width,
        &depth
    );
    constexpr int border = 8; // px
    const bool on_border = (x >= width - border && y >= height - border);
    std::cerr << "[Decoration] is_on_border(" << x << "," << y << ") = " << on_border << std::endl;
    return on_border;
}

void Decoration::on_button_press(const XButtonEvent& ev) {
    std::cerr << "[Decoration] on_button_press at (" << ev.x << "," << ev.y << ")" << std::endl;
    constexpr int btn_y = titlebar_height / 2;
    // Mac-like button hit detection
    for (int i = 0; i < 3; ++i) {
        constexpr int spacing = 8;
        constexpr int btn_radius = 8;
        const int btn_x = spacing + i * (btn_radius * 2 + spacing) + btn_radius;
        const int dx = ev.x - btn_x;
        const int dy = ev.y - btn_y;
        if (dx*dx + dy*dy <= btn_radius*btn_radius) {
            pressed_button = i+1; // 1=close, 2=min, 3=max
            std::cerr << "[Decoration] Button " << pressed_button << " pressed" << std::endl;
            return;
        }
    }
    if (is_on_border(ev.x, ev.y)) {
        resizing = true;
        resize_start_x = ev.x_root;
        resize_start_y = ev.y_root;
        XWindowAttributes attr;
        XGetWindowAttributes(dpy, frame_win, &attr);
        win_start_w = attr.width;
        win_start_h = attr.height;
        std::cerr << "[Decoration] Start resizing at (" << ev.x_root << "," << ev.y_root << ")" << std::endl;
        return;
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
        std::cerr << "[Decoration] Start dragging at (" << ev.x_root << "," << ev.y_root << ")" << std::endl;
    }
}

void Decoration::on_motion_notify(const XMotionEvent& ev) const {
    // Change cursor based on position
    if (is_on_border(ev.x, ev.y) || resizing) {
        XDefineCursor(dpy, frame_win, resize_cursor);
    } else if (dragging) {
        XDefineCursor(dpy, frame_win, move_cursor);
    } else {
        XDefineCursor(dpy, frame_win, normal_cursor);
    }
    if (resizing) {
        const int dx = ev.x_root - resize_start_x;
        const int dy = ev.y_root - resize_start_y;
        const int new_w = std::max(100, win_start_w + dx);
        const int new_h = std::max(50, win_start_h + dy);
        std::cerr << "[Decoration] Resizing to (" << new_w << "," << new_h << ")" << std::endl;
        XResizeWindow(dpy, frame_win, new_w, new_h);
        XResizeWindow(dpy, client_win, new_w, new_h - titlebar_height);
        return;
    }
    if (dragging) {
        const int dx = ev.x_root - drag_start_x;
        const int dy = ev.y_root - drag_start_y;
        std::cerr << "[Decoration] Dragging to (" << win_start_x + dx << "," << win_start_y + dy << ")" << std::endl;
        XMoveWindow(dpy, frame_win, win_start_x + dx, win_start_y + dy);
    }
}

void Decoration::on_button_release(const XButtonEvent& ev) {
    std::cerr << "[Decoration] on_button_release at (" << ev.x << "," << ev.y << ")" << std::endl;
    resizing = false;
    dragging = false;
    constexpr int btn_y = titlebar_height / 2;
    // Mac-like button hit detection (same as press)
    for (int i = 0; i < 3; ++i) {
        constexpr int spacing = 8;
        constexpr int btn_radius = 8;
        const int btn_x = spacing + i * (btn_radius*2 + spacing) + btn_radius;
        const int dx = ev.x - btn_x;
        const int dy = ev.y - btn_y;
        if (dx*dx + dy*dy <= btn_radius*btn_radius && pressed_button == i+1) {
            if (i == 0) {
                std::cerr << "[Decoration] Close button released" << std::endl;
                XDestroyWindow(dpy, client_win);
            } else if (i == 1) {
                std::cerr << "[Decoration] Minimize button released" << std::endl;
                XUnmapWindow(dpy, client_win);
            } else if (i == 2) {
                std::cerr << "[Decoration] Maximize button released" << std::endl;
                const Screen* scr = DefaultScreenOfDisplay(dpy);
                XMoveResizeWindow(dpy, frame_win, 10, 10, scr->width*0.9, scr->height*0.9);
                XMoveResizeWindow(dpy, client_win, 0, titlebar_height, scr->width*0.9, scr->height*0.9 - titlebar_height);
            }
        }
    }
    pressed_button = 0;
} 