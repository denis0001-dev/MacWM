#pragma once
#include <X11/Xlib.h>
#include <string>

class Decoration {
public:
    Decoration(Display* dpy, Window client, std::string title);
    ~Decoration();

    Window frame() const { return frame_win; }
    Window client() const { return client_win; }
    void draw() const;
    void on_button_press(const XButtonEvent& ev);
    void on_button_release(const XButtonEvent& ev);
    void on_motion_notify(const XMotionEvent& ev) const;
    bool is_on_border(int x, int y) const;

private:
    Display* dpy;
    Window client_win;
    Window frame_win;
    std::string title;
    int drag_start_x = 0, drag_start_y = 0;
    int win_start_x = 0, win_start_y = 0;
    bool dragging = false;
    int pressed_button = 0; // 1=close, 2=min, 3=max
    static constexpr int border_width = 2;
    static constexpr int titlebar_height = 24;
    // Resizing state
    mutable bool resizing = false;
    mutable int resize_start_x = 0, resize_start_y = 0;
    mutable int win_start_w = 0, win_start_h = 0;
    // Cursors
    mutable Cursor resize_cursor = 0;
    mutable Cursor move_cursor = 0;
    mutable Cursor normal_cursor = 0;
}; 