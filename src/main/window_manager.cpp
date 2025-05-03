// ReSharper disable CppMemberFunctionMayBeStatic
#include "window_manager.hpp"
#include "assert.hpp"
#include "logging.hpp"
#include "x_utils.hpp"

using namespace std;

bool WindowManager::wm_detected_;
mutex WindowManager::wm_detected_mutex_;

unique_ptr<WindowManager> WindowManager::create(const string& display_str) {
    // 1. Open X display.
    const char* display_c_str = display_str.empty() ? nullptr : display_str.c_str();
    Display* display = XOpenDisplay(display_c_str);
    if (display == nullptr) {
        println("Failed to open X display " << XDisplayName(display_c_str));
        return nullptr;
    }
    // 2. Construct WindowManager instance.
    return unique_ptr<WindowManager>(new WindowManager(display));
}

WindowManager::WindowManager(Display* display)
    : display_(assert::check_not_null(display, "display is not null")),
      root_(DefaultRootWindow(display_)),
      wallpaper(),
      WM_PROTOCOLS(XUtils::internAtom(display_, "WM_PROTOCOLS", false)),
      WM_DELETE_WINDOW(XUtils::internAtom(display_, "WM_DELETE_WINDOW", false)) {
}

WindowManager::~WindowManager() {
    XCloseDisplay(display_);
}

[[noreturn]] void WindowManager::run() {
    // 1. Initialization.
    //   a. Select events on root window. Use a special error handler so we can
    //   exit gracefully if another window manager is already running.
    {
        std::lock_guard<mutex> lock(wm_detected_mutex_);

        wm_detected_ = false;
        XSetErrorHandler(&WindowManager::onWMDetected);
        XUtils::selectInput(display_, root_, SubstructureRedirectMask | SubstructureNotifyMask);
        XSync(display_, false);
        if (wm_detected_) {
            println("Detected another window manager on display " << XDisplayString(display_));
            exit(1);
        }
    }
    //   b. Set error handler.
    XSetErrorHandler(&WindowManager::onXError);
    //   c. Grab X server to prevent windows from changing under us.
    XUtils::grabServer(display_);
    //   d. Reparent existing top-level windows.
    //     i. Query existing top-level windows.
    Window returned_root, returned_parent;
    Window *top_level_windows;
    unsigned int num_top_level_windows;
    assert::check(XUtils::queryTree(display_, root_, &returned_root, &returned_parent,
                                   &top_level_windows, &num_top_level_windows),
                 "XQueryTree failed");
    assert::check_eq(returned_root, root_, "returned_root == root_");

    //     ii. Frame each top-level window.
    for (unsigned int i = 0; i < num_top_level_windows; ++i) {
        println("[" << i << "] window " << top_level_windows[i]);
        frame(top_level_windows[i], true, i == 0);
    }
    if (num_top_level_windows == 0) {
        println("No top-level windows found.");
    }
    //     iii. Free top-level window array.
    XFree(top_level_windows);
    //   e. Ungrab X server.
    XUtils::ungrabServer(display_);

    // 2. Main event loop.
    while (true) {
        // 1. Get next event.
        XEvent e;
        XNextEvent(display_, &e);
        println("Received event: " << toString(e));
        println("Top level windows: " << clients_.size());

        // 2. Dispatch event.
        switch (e.type) {
            case CreateNotify:
                onCreateNotify(e.xcreatewindow);
                break;
            case DestroyNotify:
                onDestroyNotify(e.xdestroywindow);
                break;
            case ReparentNotify:
                onReparentNotify(e.xreparent);
                break;
            case MapNotify:
                onMapNotify(e.xmap);
                break;
            case UnmapNotify:
                onUnmapNotify(e.xunmap);
                break;
            case ConfigureNotify:
                onConfigureNotify(e.xconfigure);
                break;
            case MapRequest:
                onMapRequest(e.xmaprequest);
                break;
            case ConfigureRequest:
                onConfigureRequest(e.xconfigurerequest);
                break;
            case ButtonPress:
                onButtonPress(e.xbutton);
                break;
            case ButtonRelease:
                onButtonRelease(e.xbutton);
                break;
            case MotionNotify:
                // Skip any already pending motion events.
                while (XCheckTypedWindowEvent(display_, e.xmotion.window, MotionNotify, &e)) {
                }
                onMotionNotify(e.xmotion);
                break;
            case KeyPress:
                onKeyPress(e.xkey);
                break;
            case KeyRelease:
                onKeyRelease(e.xkey);
                break;
            default:
                onUnknownEvent(e);
        }
    }
    // ReSharper disable CppDFAUnreachableCode
}

void WindowManager::frame(const Window w, const bool beforeWindowManager, bool wallpaper) {
    if (clients_.empty()) {
        wallpaper = true;
    }

    // Visual properties of the frame to create.
    const unsigned int BORDER_WIDTH = wallpaper ? 0 : 1;
    const unsigned long BORDER_COLOR = wallpaper ? 0x000000 : 0xCCCCCC;
    constexpr unsigned long BG_COLOR = 0xE6E6E6;
    constexpr unsigned int TITLE_BAR_HEIGHT = 22;

    if (wallpaper) {
        this->wallpaper = w;
        println("This window is the wallpaper");
    }

    assert::check(!clients_.count(w), "!clients_.count(w)");

    const XWindowAttributes x_window_attrs = XUtils::getWindowAttributes(display_, w);

    if (beforeWindowManager) {
        if (x_window_attrs.override_redirect || x_window_attrs.map_state != IsViewable) {
            return;
        }
    }

    const Window frame = XUtils::createSimpleWindow(
        display_,
        root_,
        x_window_attrs.x,
        x_window_attrs.y,
        x_window_attrs.width,
        x_window_attrs.height + TITLE_BAR_HEIGHT,
        BORDER_WIDTH,
        BORDER_COLOR,
        BG_COLOR);

    XUtils::selectInput(display_, frame, SubstructureRedirectMask | SubstructureNotifyMask);
    XUtils::addToSaveSet(display_, w);
    XUtils::reparentWindow(display_, w, frame, 0, TITLE_BAR_HEIGHT);
    XUtils::mapWindow(display_, frame);

    clients_[w] = frame;

    // Create title bar
    const Window title_bar = XUtils::createSimpleWindow(
        display_,
        frame,
        0, 0,
        x_window_attrs.width,
        TITLE_BAR_HEIGHT,
        0, 0, BG_COLOR);

    // Map title bar and buttons
    XUtils::mapWindow(display_, title_bar);

    if (!wallpaper) {
        XUtils::grabButton(display_, Button1, Mod1Mask, w, false,
                          ButtonPressMask | ButtonReleaseMask | ButtonMotionMask,
                          GrabModeAsync, GrabModeAsync, None, None);

        XUtils::grabButton(display_, Button3, Mod1Mask, w, false,
                          ButtonPressMask | ButtonReleaseMask | ButtonMotionMask,
                          GrabModeAsync, GrabModeAsync, None, None);

        XUtils::grabKey(display_, XKeysymToKeycode(display_, XK_F4), Mod1Mask,
                       w, false, GrabModeAsync, GrabModeAsync);
    }

    XUtils::grabKey(display_, XKeysymToKeycode(display_, XK_Tab), Mod1Mask,
                   w, false, GrabModeAsync, GrabModeAsync);

    println("Framed window " << w << " [" << frame << "]");
}

void WindowManager::unframe(const Window w) {
    assert::check(clients_.count(w), "clients_.count(w)");

    // We reverse the steps taken in Frame().
    const Window frame = clients_[w];
    // 1. Unmap frame.
    XUtils::unmapWindow(display_, frame);
    // 2. Reparent client window.
    XUtils::reparentWindow(display_, w, root_, 0, 0);
    // 3. Remove client window from save set, as it is now unrelated to us.
    XUtils::removeFromSaveSet(display_, w);
    // 4. Destroy frame.
    XUtils::destroyWindow(display_, frame);
    // 5. Drop reference to frame handle.
    clients_.erase(w);

    println("Unframed window " << w << " [" << frame << "]");
}

void WindowManager::onCreateNotify(const XCreateWindowEvent& e) {
}

void WindowManager::onDestroyNotify(const XDestroyWindowEvent& e) {
}

void WindowManager::onReparentNotify(const XReparentEvent& e) {
}

void WindowManager::onMapNotify(const XMapEvent& e) {
}

void WindowManager::onUnmapNotify(const XUnmapEvent& e) {
    if (!clients_.count(e.window)) {
        println("Ignore UnmapNotify for non-client window " << e.window);
        return;
    }

    if (e.event == root_) {
        cout << "Ignore UnmapNotify for reparented pre-existing window " << e.window << endl;
        return;
    }

    unframe(e.window);
}

void WindowManager::onConfigureNotify(const XConfigureEvent& e) {
}

void WindowManager::onMapRequest(const XMapRequestEvent& e) {
    // 1. Frame or re-frame window.
    frame(e.window, false, false);
    // 2. Actually map window.
    XUtils::mapWindow(display_, e.window);
}

void WindowManager::onConfigureRequest(const XConfigureRequestEvent& e) {
    if (clients_.count(e.window)) {
        const Window frame = clients_[e.window];
        XUtils::configureWindow(display_, frame, e.x, e.y, e.width, e.height);
        println("Resize [" << frame << "] to " << Size<int>(e.width, e.height));
    }
    XUtils::configureWindow(display_, e.window, e.x, e.y, e.width, e.height);
    println("Resize " << e.window << " to " << Size<int>(e.width, e.height));
}

void WindowManager::onButtonPress(const XButtonEvent& e) {
    assert::check(clients_.count(e.window), "clients_.count(e.window)");
    const Window frame = clients_[e.window];

    drag_start_pos_ = Position<int>(e.x_root, e.y_root);

    const XWindowAttributes attrs = XUtils::getWindowAttributes(display_, frame);
    drag_start_frame_pos_ = Position<int>(attrs.x, attrs.y);
    drag_start_frame_size_ = Size<int>(attrs.width, attrs.height);

    XUtils::raiseWindow(display_, frame);
}

void WindowManager::onButtonRelease(const XButtonEvent& e) {
}

void WindowManager::onMotionNotify(const XMotionEvent& e) {
    assert::check(clients_.count(e.window), "clients_.count(e.window)");
    const Window frame = clients_[e.window];
    const Position<int> drag_pos(e.x_root, e.y_root);
    const Vector2D<int> delta = drag_pos - drag_start_pos_;

    if (e.state & Button1Mask) {
        // alt + left button: Move window.
        const Position<int> dest_frame_pos = drag_start_frame_pos_ + delta;
        XUtils::moveWindow(display_, frame, dest_frame_pos.x, dest_frame_pos.y);
    } else if (e.state & Button3Mask) {
        // alt + right button: Resize window.
        // Window dimensions cannot be negative.
        const Vector2D<int> size_delta(
            max(delta.x, -drag_start_frame_size_.width),
            max(delta.y, -drag_start_frame_size_.height));
        const Size<int> dest_frame_size = drag_start_frame_size_ + size_delta;
        // 1. Resize frame.
        XUtils::resizeWindow(display_, frame, dest_frame_size.width, dest_frame_size.height);
        // 2. Resize client window.
        XUtils::resizeWindow(display_, e.window, dest_frame_size.width, dest_frame_size.height);
    }
}

void WindowManager::onKeyPress(const XKeyEvent& e) {
    if (e.state & Mod1Mask && e.keycode == XKeysymToKeycode(display_, XK_F4)) {
        // alt + f4: Close window.
        Atom *supported_protocols;
        int num_supported_protocols;
        if (XUtils::getWMProtocols(display_, e.window, &supported_protocols, &num_supported_protocols) &&
            (std::find(supported_protocols, supported_protocols + num_supported_protocols,
                      WM_DELETE_WINDOW) != supported_protocols + num_supported_protocols)) {
            println("Gracefully deleting window " << e.window);
            XUtils::sendClientMessage(display_, e.window, WM_PROTOCOLS, static_cast<long>(WM_DELETE_WINDOW));
        } else {
            println("Killing window " << e.window);
            XKillClient(display_, e.window);
        }
    } else if (e.state & Mod1Mask && e.keycode == XKeysymToKeycode(display_, XK_Tab)) {
        // alt + tab: Switch window.
        unordered_map<Window, Window>::iterator i;
        for (int i2 = 0; i2 < 2; i2++) {
            // 1. Find next window.
            i = clients_.find(e.window);
            assert::check(i != clients_.end(), "i != clients_.end()");
            ++i;
            if (i == clients_.end()) {
                i = clients_.begin();
            }
            if (i->first == wallpaper || i->second == wallpaper) {
                println("Cannot switch to wallpaper, going to the next window");
                continue;
            }
            break;
        }
        // 2. Raise and set focus.
        XUtils::raiseWindow(display_, i->second);
        XUtils::setInputFocus(display_, i->first, RevertToPointerRoot, CurrentTime);
    }
}

void WindowManager::onKeyRelease(const XKeyEvent& e) {
}

// ReSharper disable CppParameterMayBeConstPtrOrRef
int WindowManager::onXError(Display* display, XErrorEvent* e) {
    println("Received X error:\n"
            << "    Request: " << static_cast<int>(e->request_code)
            << " - " << XUtils::getRequestCodeName(e->request_code) << "\n"
            << "    Error code: " << static_cast<int>(e->error_code)
            << " - " << XUtils::getErrorText(display, e->error_code) << "\n"
            << "    Resource ID: " << e->resourceid);
    return 0;
}

int WindowManager::onWMDetected(Display* display, XErrorEvent* e) {
    assert::check_eq(static_cast<int>(e->error_code), BadAccess, "e->error_code == BadAccess");
    wm_detected_ = true;
    return 0;
}

void WindowManager::onUnknownEvent(const XEvent& event) { // NOLINT(*-convert-member-functions-to-static)
    println("Received unknown X event " << event.type << ", ignoring");
}