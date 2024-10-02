#ifndef WINDOW_MANAGER_HPP
#define WINDOW_MANAGER_HPP

extern "C" {
#include <X11/Xlib.h>
}
#include <memory>
#include <mutex>
#include <string>
#include <unordered_map>
#include "util.hpp"

// Implementation of a window manager for an X screen.
class WindowManager {
 public:
  // Creates a WindowManager instance for the X display/screen specified by the
  // argument string, or if unspecified, the DISPLAY environment variable. On
  // failure, returns nullptr.
   static std::unique_ptr<WindowManager> create(
      const std::string& display_str = std::string());

  ~WindowManager();

  // The entry point to this class. Enters the main event loop.
  void run();

 private:

  // Invoked internally by Create().
  explicit WindowManager(Display* display);
  // Frames a top-level window.
  void frame(Window w, bool beforeWindowManager);
  // Unframes a client window.
  void unframe(Window w);

  // Event handlers.
  void onCreateNotify(const XCreateWindowEvent& e);
  void onDestroyNotify(const XDestroyWindowEvent& e);
  void onReparentNotify(const XReparentEvent& e);
  void onMapNotify(const XMapEvent& e);
  void onUnmapNotify(const XUnmapEvent& e);
  void onConfigureNotify(const XConfigureEvent& e);
  void onMapRequest(const XMapRequestEvent& e);
  void onConfigureRequest(const XConfigureRequestEvent& e);
  void onButtonPress(const XButtonEvent& e);
  void onButtonRelease(const XButtonEvent& e);
  void onMotionNotify(const XMotionEvent& e);
  void onKeyPress(const XKeyEvent& e);
  void onKeyRelease(const XKeyEvent& e);

  // Xlib error handler. It must be static as its address is passed to Xlib.
  static int onXError(Display* display, XErrorEvent* e);
  // Xlib error handler used to determine whether another window manager is
  // running. It is set as the error handler right before selecting substructure
  // redirection mask on the root window, so it is invoked if and only if
  // another window manager is running. It must be static as its address is
  // passed to Xlib.
  static int onWMDetected(Display* display, XErrorEvent* e);

  void onUnknownEvent(int eventNumber);

  // Whether an existing window manager has been detected. Set by OnWMDetected,
  // and hence must be static.
  static bool wm_detected_;
  // A mutex for protecting wm_detected_. It's not strictly speaking needed as
  // this program is single threaded, but better safe than sorry.
  static std::mutex wm_detected_mutex_;

  // Handle to the underlying Xlib Display struct.
  Display* display_;
  // Handle to root window.
  const Window root_;
  // Maps top-level windows to their frame windows.
  std::unordered_map<Window, Window> clients_;

  // The cursor position at the start of a window move/resize.
  Position<int> drag_start_pos_ = Position<int>(-1, -1);
  // The position of the affected window at the start of a window
  // move/resize.
  Position<int> drag_start_frame_pos_ = Position<int>(-1, -1);
  // The size of the affected window at the start of a window move/resize.
  Size<int> drag_start_frame_size_ = Size<int>(-1, -1);

  // PNG *wallpaper_image_;

  // Atom constants.
  const Atom WM_PROTOCOLS;
  const Atom WM_DELETE_WINDOW;
};

#endif
