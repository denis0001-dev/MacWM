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
    void on_motion_notify(const XMotionEvent& ev) const;

private:
    Display* dpy;
    Window client_win;
    Window frame_win;
    std::string title;
    int drag_start_x = 0, drag_start_y = 0;
    int win_start_x = 0, win_start_y = 0;
    bool dragging = false;
    static constexpr int border_width = 2;
    static constexpr int titlebar_height = 24;
}; 