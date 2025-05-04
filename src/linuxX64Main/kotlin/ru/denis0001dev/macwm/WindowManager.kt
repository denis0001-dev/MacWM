@file:OptIn(ExperimentalForeignApi::class)

package ru.denis0001dev.macwm;

import kotlinx.cinterop.*
import xorg.lib.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.system.exitProcess
import kotlin.math.max
import kotlin.test.assertNotNull

typealias XDisplay = Display

// Implementation of a window manager for an X screen.
class WindowManager private constructor(display: CPointer<XDisplay>) { // Private constructor

    // Handle to the underlying Xlib Display struct.
    private val display_: CPointer<Display> = assertNotNull(display, "display is not null")
    // Handle to root window.
    private val root_: Window = DefaultRootWindow(display_)
    // Maps top-level windows to their frame windows.
    private val clients_: MutableMap<Window, Window> = mutableMapOf()
    private var wallpaper: Window? = null

    // The cursor position at the start of a window move/resize.
    private var drag_start_pos_: Position<Int> = Position(-1, -1)
    // The position of the affected window at the start of a window
    // move/resize.
    private var drag_start_frame_pos_: Position<Int> = Position(-1, -1)
    // The size of the affected window at the start of a window move/resize.
    private var drag_start_frame_size_: Size<Int> = Size(-1, -1)

    // Atom constants.
    private val WM_PROTOCOLS: Atom
    private val WM_DELETE_WINDOW: Atom

    // Nested class for ButtonWindows (as declared in header, though unused in logic)
    private data class ButtonWindows(
        val close: Window,
        val minimize: Window,
        val maximize: Window
    )
    private val button_windows_: MutableMap<Window, ButtonWindows> = mutableMapOf()


    init {
        // Initialize Atoms in the constructor body (equivalent to C++ initializer list)
        WM_PROTOCOLS = XUtils.internAtom(display_, "WM_PROTOCOLS", false)
        WM_DELETE_WINDOW = XUtils.internAtom(display_, "WM_DELETE_WINDOW", false)
    }

    // Destructor equivalent (Kotlin/Native uses GC, but explicit cleanup is needed for X resources)
    fun close() { // Renamed from C++ destructor convention
        XCloseDisplay(display_)
    }

    // The entry point to this class. Enters the main event loop.
    fun run(): Nothing { // Use Nothing for [[noreturn]]
        // 1. Initialization.
        //   a. Select events on root window. Use a special error handler so we can
        //   exit gracefully if another window manager is already running.
        runBlocking { // Use runBlocking for mutex access in a simple way here
            wm_detected_mutex_.withLock {
                wm_detected_ = false
                XSetErrorHandler(staticCFunction(::onWMDetected))
                // Direct Xlib call replacing XUtils::selectInput
                XSelectInput(display_, root_, (SubstructureRedirectMask or SubstructureNotifyMask).toLong())
                XSync(display_, False)
                if (wm_detected_) {
                    println("Detected another window manager on display ${XDisplayString(display_).toKString()}")
                    exitProcess(1)
                }
            }
        }
        //   b. Set error handler.
        XSetErrorHandler(staticCFunction(::onXError))
        //   c. Grab X server to prevent windows from changing under us.
        // Direct Xlib call replacing XUtils::grabServer
        XGrabServer(display_)
        //   d. Reparent existing top-level windows.
        //     i. Query existing top-level windows.
        memScoped {
            val returned_root = alloc<WindowVar>()
            val returned_parent = alloc<WindowVar>()
            val top_level_windows_ptr = alloc<CPointerVar<WindowVar>>()
            val num_top_level_windows = alloc<UIntVar>()

            // Direct Xlib call replacing XUtils::queryTree
            val queryTreeStatus = XQueryTree(display_, root_, returned_root.ptr, returned_parent.ptr,
                top_level_windows_ptr.ptr, num_top_level_windows.ptr)
            Assert.check(queryTreeStatus != 0, "XQueryTree failed")
            Assert.check_eq(returned_root.value, root_, "returned_root == root_")

            val top_level_windows = top_level_windows_ptr.value
            val numWindows = num_top_level_windows.value.toInt()

            //     ii. Frame each top-level window.
            if (top_level_windows != null) {
                for (i in 0 until numWindows) {
                    val currentWindow = top_level_windows[i]
                    println("[$i] window $currentWindow")
                    frame(currentWindow, true, i == 0)
                }
            }
            if (numWindows == 0) {
                println("No top-level windows found.")
            }
            //     iii. Free top-level window array.
            if (top_level_windows != null) {
                XFree(top_level_windows)
            }
        } // End memScoped for XQueryTree results

        //   e. Ungrab X server.
        // Direct Xlib call replacing XUtils::ungrabServer
        XUngrabServer(display_)

        // 2. Main event loop.
        memScoped { // Scope for XEvent allocation
            val e = alloc<XEvent>()
            while (true) {
                // 1. Get next event.
                XNextEvent(display_, e.ptr)
                println("Received event: ${toString(e)}")
                println("Top level windows: ${clients_.size}")

                // 2. Dispatch event.
                when (e.type) {
                    CreateNotify -> onCreateNotify(e.xcreatewindow)
                    DestroyNotify -> onDestroyNotify(e.xdestroywindow)
                    ReparentNotify -> onReparentNotify(e.xreparent)
                    MapNotify -> onMapNotify(e.xmap)
                    UnmapNotify -> onUnmapNotify(e.xunmap)
                    ConfigureNotify -> onConfigureNotify(e.xconfigure)
                    MapRequest -> onMapRequest(e.xmaprequest)
                    ConfigureRequest -> onConfigureRequest(e.xconfigurerequest)
                    ButtonPress -> onButtonPress(e.xbutton)
                    ButtonRelease -> onButtonRelease(e.xbutton)
                    MotionNotify -> {
                        // Skip any already pending motion events.
                        // Need a temporary event structure for XCheckTypedWindowEvent
                        val dummyEvent = alloc<XEvent>()
                        while (XCheckTypedWindowEvent(display_, e.xmotion.window, MotionNotify, dummyEvent.ptr) == True) {
                            // Discard the event read by XCheckTypedWindowEvent by doing nothing
                            // Update 'e' with the last motion event if needed, or just process the one we have.
                            // The C++ code seems to implicitly use the *last* event fetched by the loop condition.
                            // Let's copy the last fetched event into 'e' to mimic this.
                            // NOTE: This might not be a perfect 1:1 if XCheckTypedWindowEvent modifies the passed event ptr.
                            // A safer approach might be to re-read the event if the loop ran.
                            // However, the C++ code *reassigns* 'e' inside the loop condition.
                            // Let's try assigning the dummyEvent back to e.
                            // WARNING: This memory management might be tricky. Let's stick to the C++ logic's apparent outcome:
                            // process the *last* MotionNotify event received for that window.
                            // The C++ `XCheckTypedWindowEvent(..., &e)` modifies `e` directly.
                            // So the loop effectively discards all but the last one, which remains in `e`.
                            // Let's try passing e.ptr directly.
                        }
                        // Re-check the loop condition logic. The C++ code uses `XCheckTypedWindowEvent(..., &e)`.
                        // This means `e` is overwritten inside the loop. The loop continues as long as there are
                        // MotionNotify events. When the loop finishes, `e` holds the *last* MotionNotify event.
                        // So the Kotlin equivalent should pass `e.ptr` to the check function.
                        while (XCheckTypedWindowEvent(display_, e.xmotion.window, MotionNotify, e.ptr) == True) { }
                        onMotionNotify(e.xmotion)
                    }
                    KeyPress -> onKeyPress(e.xkey)
                    KeyRelease -> onKeyRelease(e.xkey)
                    else -> onUnknownEvent(e)
                }
            }
        } // End memScoped for XEvent
        // Code here is unreachable because run() returns Nothing
    } // End run()

    // Frames a top-level window.
    private fun frame(w: Window, beforeWindowManager: Boolean, isWallpaperCandidate: Boolean) {
        var wallpaper = isWallpaperCandidate // Shadowing the class member intentionally here as per C++ logic

        if (clients_.isEmpty()) {
            wallpaper = true
        }

        // Visual properties of the frame to create.
        val BORDER_WIDTH: UInt = if (wallpaper) 0u else 1u
        val BORDER_COLOR: ULong = if (wallpaper) 0x000000uL else 0xCCCCCCuL
        val BG_COLOR: ULong = 0xE6E6E6uL
        val TITLE_BAR_HEIGHT: UInt = 22u

        if (wallpaper) {
            this.wallpaper = w // Assign to the class member
            println("This window is the wallpaper")
        }

        Assert.check(!clients_.containsKey(w), "!clients_.containsKey(w)")

        // Direct Xlib call replacing XUtils::getWindowAttributes
        val x_window_attrs = XUtils.getWindowAttributes(display_, w)

        if (beforeWindowManager) {
            if (x_window_attrs.override_redirect || x_window_attrs.map_state != IsViewable) {
                return
            }
        }

        // Direct Xlib call replacing XUtils::createSimpleWindow
        val frame = XCreateSimpleWindow(
            display_,
            root_,
            x_window_attrs.x,
            x_window_attrs.y,
            x_window_attrs.width.toUInt(),
            x_window_attrs.height.toUInt() + TITLE_BAR_HEIGHT,
            BORDER_WIDTH,
            BORDER_COLOR,
            BG_COLOR)

        // Direct Xlib call replacing XUtils::selectInput
        XSelectInput(display_, frame, (SubstructureRedirectMask or SubstructureNotifyMask).toLong())
        // Direct Xlib call replacing XUtils::addToSaveSet
        XAddToSaveSet(display_, w)
        // Direct Xlib call replacing XUtils::reparentWindow
        XReparentWindow(display_, w, frame, 0, TITLE_BAR_HEIGHT.toInt())
        // Direct Xlib call replacing XUtils::mapWindow
        XMapWindow(display_, frame)

        clients_[w] = frame

        // Create title bar
        // Direct Xlib call replacing XUtils::createSimpleWindow
        val title_bar = XCreateSimpleWindow(
            display_,
            frame,
            0, 0,
            x_window_attrs.width.toUInt(),
            TITLE_BAR_HEIGHT,
            0u, // border width
            0uL, // border color
            BG_COLOR)

        // Map title bar and buttons
        // Direct Xlib call replacing XUtils::mapWindow
        XMapWindow(display_, title_bar)

        if (!wallpaper) {
            // Direct Xlib call replacing XUtils::grabButton
            XGrabButton(display_, Button1.toUInt(), Mod1Mask.toUInt(), w,
                False, // owner_events
                (ButtonPressMask or ButtonReleaseMask or ButtonMotionMask).toUInt(),
                GrabModeAsync, GrabModeAsync, None, None)

            // Direct Xlib call replacing XUtils::grabButton
            XGrabButton(display_, Button3.toUInt(), Mod1Mask.toUInt(), w,
                False, // owner_events
                (ButtonPressMask or ButtonReleaseMask or ButtonMotionMask).toUInt(),
                GrabModeAsync, GrabModeAsync, None, None)

            // Direct Xlib call replacing XUtils::grabKey
            XGrabKey(display_, XKeysymToKeycode(display_, XK_F4.toULong()).toInt(), Mod1Mask.toUInt(),
                w, False, GrabModeAsync, GrabModeAsync)
        }

        // Direct Xlib call replacing XUtils::grabKey
        XGrabKey(display_, XKeysymToKeycode(display_, XK_Tab.toULong()).toInt(), Mod1Mask.toUInt(),
            w, False, GrabModeAsync, GrabModeAsync)

        println("Framed window $w [$frame]")
    }

    // Unframes a client window.
    private fun unframe(w: Window) {
        Assert.check(clients_.containsKey(w), "clients_.containsKey(w)")

        // We reverse the steps taken in Frame().
        val frame = clients_[w]!! // Use !! as check ensures it exists
        // 1. Unmap frame.
        // Direct Xlib call replacing XUtils::unmapWindow
        XUnmapWindow(display_, frame)
        // 2. Reparent client window.
        // Direct Xlib call replacing XUtils::reparentWindow
        // Get root window geometry to place the unframed window correctly?
        // The C++ code places it at (0,0) relative to root.
        XReparentWindow(display_, w, root_, 0, 0)
        // 3. Remove client window from save set, as it is now unrelated to us.
        // Direct Xlib call replacing XUtils::removeFromSaveSet
        XRemoveFromSaveSet(display_, w)
        // 4. Destroy frame.
        // Direct Xlib call replacing XUtils::destroyWindow
        XDestroyWindow(display_, frame)
        // 5. Drop reference to frame handle.
        clients_.remove(w)

        println("Unframed window $w [$frame]")
    }

    // Event handlers.
    private fun onCreateNotify(e: XCreateWindowEvent) {
        // Original C++ function is empty
    }

    private fun onDestroyNotify(e: XDestroyWindowEvent) {
        // Original C++ function is empty
    }

    private fun onReparentNotify(e: XReparentEvent) {
        // Original C++ function is empty
    }

    private fun onMapNotify(e: XMapEvent) {
        // Original C++ function is empty
    }

    private fun onUnmapNotify(e: XUnmapEvent) {
        // Use get operator which returns null if key not present
        val frame = clients_[e.window]
        if (frame == null) {
            println("Ignore UnmapNotify for non-client window ${e.window}")
            return
        }

        // The C++ checks e.event == root_. In XUnmapEvent, e.event is the window
        // that received the event (the parent from which it was unmapped).
        // If the client window (e.window) is unmapped from the root, it means
        // it was likely being reparented *by us* initially.
        // Let's adapt the check: if the event source is the root window.
        if (e.event == root_) {
            // C++ used std::cout here, maintaining it.
            print("Ignore UnmapNotify for reparented pre-existing window ${e.window}\n")
            return
        }

        unframe(e.window)
    }

    private fun onConfigureNotify(e: XConfigureEvent) {
        // Original C++ function is empty
    }

    private fun onMapRequest(e: XMapRequestEvent) {
        // 1. Frame or re-frame window.
        // Check if already framed (e.g., ConfigureRequest followed by MapRequest)
        if (!clients_.containsKey(e.window)) {
            frame(e.window, false, false)
        }
        // 2. Actually map window.
        // Direct Xlib call replacing XUtils::mapWindow
        XMapWindow(display_, e.window)
    }

    private fun onConfigureRequest(e: XConfigureRequestEvent) {
        memScoped {
            val changes = alloc<XWindowChanges>()
            var valueMask = 0L

            // Check which values are specified in the request
            if ((e.value_mask and CWX.toULong()) != 0uL) {
                changes.x = e.x
                valueMask = valueMask or CWX.toLong()
            }
            if ((e.value_mask and CWY.toULong()) != 0uL) {
                changes.y = e.y
                valueMask = valueMask or CWY.toLong()
            }
            if ((e.value_mask and CWWidth.toULong()) != 0uL) {
                changes.width = e.width
                valueMask = valueMask or CWWidth.toLong()
            }
            if ((e.value_mask and CWHeight.toULong()) != 0uL) {
                changes.height = e.height
                valueMask = valueMask or CWHeight.toLong()
            }
            if ((e.value_mask and CWBorderWidth.toULong()) != 0uL) {
                changes.border_width = e.border_width
                valueMask = valueMask or CWBorderWidth.toLong()
            }
            if ((e.value_mask and CWSibling.toULong()) != 0uL) {
                changes.sibling = e.above
                valueMask = valueMask or CWSibling.toLong()
            }
            if ((e.value_mask and CWStackMode.toULong()) != 0uL) {
                changes.stack_mode = e.detail
                valueMask = valueMask or CWStackMode.toLong()
            }

            val frame = clients_[e.window]
            if (frame != null) {
                // If we manage this window, apply changes to the frame
                // Adjust position based on frame geometry if necessary (e.g., title bar)
                // The C++ code applies x,y,width,height directly to frame and client.
                // Let's replicate that simple logic first.
                // We need to adjust height for the title bar.
                val frameChanges = alloc<XWindowChanges>().apply {
                    if ((valueMask and CWX.toLong()) != 0L) this.x = changes.x
                    if ((valueMask and CWY.toLong()) != 0L) this.y = changes.y
                    if ((valueMask and CWWidth.toLong()) != 0L) this.width = changes.width
                    if ((valueMask and CWHeight.toLong()) != 0L) this.height = changes.height + TITLE_BAR_HEIGHT.toInt() // Adjust height
                    if ((valueMask and CWBorderWidth.toLong()) != 0L) this.border_width = changes.border_width
                    if ((valueMask and CWSibling.toLong()) != 0L) this.sibling = changes.sibling
                    if ((valueMask and CWStackMode.toLong()) != 0L) this.stack_mode = changes.stack_mode
                }
                val frameValueMask = valueMask or CWHeight.toLong() // Ensure height is always included if width was

                XConfigureWindow(display_, frame, frameValueMask.toUInt(), frameChanges.ptr)
                println("Resize [$frame] to ${Size(frameChanges.width, frameChanges.height)}")

                // Also configure the client window within the frame
                // Client position is relative to frame (0, TITLE_BAR_HEIGHT)
                // Client size should match requested size
                val clientChanges = alloc<XWindowChanges>().apply {
                    // Client X/Y relative to frame is fixed, only update size/stacking
                    this.x = 0 // Always 0 relative to frame
                    this.y = TITLE_BAR_HEIGHT.toInt() // Always below title bar
                    if ((valueMask and CWWidth.toLong()) != 0L) this.width = changes.width
                    if ((valueMask and CWHeight.toLong()) != 0L) this.height = changes.height
                    if ((valueMask and CWBorderWidth.toLong()) != 0L) this.border_width = changes.border_width
                    if ((valueMask and CWSibling.toLong()) != 0L) this.sibling = changes.sibling
                    if ((valueMask and CWStackMode.toLong()) != 0L) this.stack_mode = changes.stack_mode
                }
                // Mask for client: only width, height, border, sibling, stack mode matter relative to parent
                var clientValueMask = (valueMask and (CWWidth or CWHeight or CWBorderWidth or CWSibling or CWStackMode).toLong())
                // If only position changed for the frame, we don't need to reconfigure the client window itself,
                // unless maybe stacking order changed. The C++ code configures the client regardless.
                // Let's stick to the C++ code's apparent behavior: configure the client with its new size/border/stacking.
                XConfigureWindow(display_, e.window, clientValueMask.toUInt(), clientChanges.ptr)
                println("Resize ${e.window} to ${Size(clientChanges.width, clientChanges.height)}")

            } else {
                // If not managed by us, just pass the configuration request through
                XConfigureWindow(display_, e.window, e.value_mask.toUInt(), changes.ptr)
                println("Configure (unmanaged) ${e.window} to ${Size(changes.width, changes.height)}")
            }
        } // End memScoped
    }


    private fun onButtonPress(e: XButtonEvent) {
        val frame = clients_[e.window]
        Assert.check(frame != null, "clients_.containsKey(e.window)")

        drag_start_pos_ = Position(e.x_root, e.y_root)

        // Direct Xlib call replacing XUtils::getWindowAttributes
        val attrs = XUtils.getWindowAttributes(display_, frame!!) // Use !! because of check
        drag_start_frame_pos_ = Position(attrs.x, attrs.y)
        // Frame size includes title bar, but C++ seems to use this directly. Let's match.
        drag_start_frame_size_ = Size(attrs.width, attrs.height)

        // Direct Xlib call replacing XUtils::raiseWindow
        XRaiseWindow(display_, frame)
    }

    private fun onButtonRelease(e: XButtonEvent) {
        // Original C++ function is empty
    }

    private fun onMotionNotify(e: XMotionEvent) {
        val frame = clients_[e.window]
        Assert.check(frame != null, "clients_.containsKey(e.window)")

        val drag_pos = Position(e.x_root, e.y_root)
        val delta = drag_pos - drag_start_pos_

        if ((e.state and Button1Mask.toUInt()) != 0u) {
            // alt + left button: Move window.
            val dest_frame_pos = drag_start_frame_pos_ + delta
            // Direct Xlib call replacing XUtils::moveWindow
            XMoveWindow(display_, frame!!, dest_frame_pos.x, dest_frame_pos.y)
        } else if ((e.state and Button3Mask.toUInt()) != 0u) {
            // alt + right button: Resize window.
            // Window dimensions cannot be negative.
            // Note: C++ uses frame size including title bar for calculation.
            val size_delta = Vector2D(
                max(delta.x, -drag_start_frame_size_.width),
                max(delta.y, -drag_start_frame_size_.height))
            val dest_frame_size = drag_start_frame_size_ + size_delta

            // Ensure minimum size (e.g., 1x1 or larger to avoid X errors)
            val final_width = max(1, dest_frame_size.width)
            // Height includes title bar, ensure client height is at least 1
            val final_height = max(1 + TITLE_BAR_HEIGHT.toInt(), dest_frame_size.height)
            val client_height = final_height - TITLE_BAR_HEIGHT.toInt()

            // 1. Resize frame.
            // Direct Xlib call replacing XUtils::resizeWindow
            XResizeWindow(display_, frame!!, final_width.toUInt(), final_height.toUInt())
            // 2. Resize client window.
            // Direct Xlib call replacing XUtils::resizeWindow
            XResizeWindow(display_, e.window, final_width.toUInt(), client_height.toUInt())
        }
    }

    private fun onKeyPress(e: XKeyEvent) {
        val altF4Keycode = XKeysymToKeycode(display_, XK_F4.toULong())
        val altTabKeycode = XKeysymToKeycode(display_, XK_Tab.toULong())

        if ((e.state and Mod1Mask.toUInt()) != 0u && e.keycode == altF4Keycode) {
            // alt + f4: Close window.
            val targetWindow = e.window // The window that has focus (received the key press)

            // Check if the window supports WM_DELETE_WINDOW protocol
            var supportsDelete = false
            memScoped {
                val protocolsPtr = alloc<CPointerVar<AtomVar>>()
                val count = alloc<IntVar>()
                if (XGetWMProtocols(display_, targetWindow, protocolsPtr.ptr, count.ptr) != 0) {
                    val protocols = protocolsPtr.value
                    val numProtocols = count.value
                    if (protocols != null) {
                        for (i in 0 until numProtocols) {
                            if (protocols[i] == WM_DELETE_WINDOW) {
                                supportsDelete = true
                                break
                            }
                        }
                        XFree(protocols) // Free the list returned by XGetWMProtocols
                    }
                }
            }

            if (supportsDelete) {
                println("Gracefully deleting window $targetWindow")
                // Direct Xlib call replacing XUtils::sendClientMessage
                memScoped {
                    val event = alloc<XClientMessageEvent>().apply{
                        type = ClientMessage
                        serial = 0u // Set by server
                        send_event = True
                        display = this@WindowManager.display_ // Pass the display
                        window = targetWindow
                        message_type = WM_PROTOCOLS
                        format = 32 // Data is array of longs
                        data.l[0] = WM_DELETE_WINDOW.toLong()
                        data.l[1] = CurrentTime.toLong() // Timestamp
                        // data.l[2..4] can be zero or other data as needed
                    }
                    XSendEvent(display_, targetWindow, False, NoEventMask.toLong(), event.ptr.reinterpret())
                }
            } else {
                println("Killing window $targetWindow")
                XKillClient(display_, targetWindow)
            }
        } else if ((e.state and Mod1Mask.toUInt()) != 0u && e.keycode == altTabKeycode) {
            // alt + tab: Switch window.
            if (clients_.size < 2) return // Need at least two windows to switch

            val currentWindow = e.window
            val clientEntries = clients_.entries.toList() // Get a stable list
            val currentIndex = clientEntries.indexOfFirst { it.key == currentWindow }

            if (currentIndex != -1) {
                var nextIndex = currentIndex
                var nextClientEntry : Map.Entry<Window, Window>? = null

                // Loop at most twice to find the next non-wallpaper window
                for (i in 0..1) {
                    nextIndex = (nextIndex + 1) % clientEntries.size
                    val candidateEntry = clientEntries[nextIndex]
                    // Check if the candidate key (client) or value (frame) is the wallpaper
                    if (candidateEntry.key != wallpaper && candidateEntry.value != wallpaper) {
                        nextClientEntry = candidateEntry
                        break // Found a suitable window
                    } else {
                        println("Cannot switch to wallpaper (${candidateEntry.key}), going to the next window")
                        // Continue loop
                    }
                }


                if (nextClientEntry != null) {
                    // 2. Raise and set focus.
                    // Direct Xlib call replacing XUtils::raiseWindow
                    XRaiseWindow(display_, nextClientEntry.value) // Raise the frame
                    // Direct Xlib call replacing XUtils::setInputFocus
                    XSetInputFocus(display_, nextClientEntry.key, RevertToPointerRoot, CurrentTime) // Focus the client
                } else {
                    println("Could not find a non-wallpaper window to switch to.")
                }

            } else {
                println("Current window $currentWindow not found in clients map for Alt+Tab.")
                // Optionally, just focus the first non-wallpaper client
                val firstValid = clients_.entries.firstOrNull { it.key != wallpaper && it.value != wallpaper }
                if(firstValid != null) {
                    XRaiseWindow(display_, firstValid.value)
                    XSetInputFocus(display_, firstValid.key, RevertToPointerRoot, CurrentTime)
                }
            }
        }
    }

    private fun onKeyRelease(e: XKeyEvent) {
        // Original C++ function is empty
    }

    private fun onUnknownEvent(event: XEvent) {
        println("Received unknown X event ${event.type}, ignoring")
    }


    companion object {
        // Whether an existing window manager has been detected. Set by OnWMDetected,
        // and hence must be static (in companion object).
        @Volatile // Ensure visibility across potential threads, though Xlib is typically single-threaded per connection
        private var wm_detected_: Boolean = false
        // A mutex for protecting wm_detected_. It's not strictly speaking needed as
        // this program is single threaded, but better safe than sorry.
        // Using kotlinx.coroutines.sync.Mutex
        private val wm_detected_mutex_ = Mutex()

        // Creates a WindowManager instance for the X display/screen specified by the
        // argument string, or if unspecified, the DISPLAY environment variable. On
        // failure, returns nullptr.
        fun create(display_str: String = ""): WindowManager? { // Return nullable WindowManager
            // 1. Open X display.
            val name = if (display_str.isEmpty()) null else display_str
            val display = XOpenDisplay(name)
            if (display == null) {
                // Use XDisplayName to get the default display name if display_c_str was null
                val displayName = XDisplayName(name)?.toKString() ?: "<unknown>"
                println("Failed to open X display $displayName")
                return null
            }
            // 2. Construct WindowManager instance.
            // Use private constructor
            return WindowManager(display)
        }

        // Xlib error handler. It must be static (in companion object) as its address is passed to Xlib.
        // Needs to match the XErrorHandler type: typedef int (*XErrorHandler)(Display*, XErrorEvent*);
        // Use CPointer<Display>? and CPointer<XErrorEvent>? for nullable pointers if appropriate,
        // but Xlib usually passes valid pointers here.
        private fun onXError(display: CPointer<Display>?, e: CPointer<XErrorEvent>?): Int {
            if (e == null) return 0 // Should not happen based on Xlib spec
            val event = e.pointed // Dereference the pointer

            println("Received X error:\n" +
                    "    Request: ${event.request_code}" +
                    " - ${XUtils.getRequestCodeName(event.request_code)}\n" +
                    "    Error code: ${event.error_code}" +
                    " - ${XUtils.getErrorText(display, event.error_code)}\n" +
                    "    Resource ID: ${event.resourceid}")
            // Must return 0.
            return 0
        }

        // Xlib error handler used to determine whether another window manager is
        // running. It is set as the error handler right before selecting substructure
        // redirection mask on the root window, so it is invoked if and only if
        // another window manager is running. It must be static (in companion object) as its address is
        // passed to Xlib.
        private fun onWMDetected(display: CPointer<Display>?, e: CPointer<XErrorEvent>?): Int {
            if (e == null) return 0
            val event = e.pointed
            // Check if the error code is BadAccess, as expected when another WM exists.
            // Need to define BadAccess constant or use its value. From X.h: #define BadAccess 10
            val badAccessCode: UByte = 10u
            Assert.check_eq(event.error_code, badAccessCode, "e->error_code == BadAccess")
            wm_detected_ = true
            // Must return 0.
            return 0
        }
    } // End companion object
} // End WindowManager class

// Helper function needed for runBlocking in run()
expect fun runBlocking(block: suspend () -> Unit)
// Implementations for different platforms (e.g., using kotlinx.coroutines)
// For Native:
// import kotlinx.coroutines.*
// actual fun runBlocking(block: suspend () -> Unit): Unit = kotlinx.coroutines.runBlocking { block() }

// Example Usage (Optional Main Function)
// fun main() {
//     val wm = WindowManager.create()
//     if (wm == null) {
//         println("Failed to create WindowManager")
//         exitProcess(1)
//     }
//     try {
//         wm.run() // Enters the infinite loop
//     } finally {
//         // This part might not be reached if run() never returns,
//         // but good practice for potential future changes.
//         // In this specific [[noreturn]] case, close() might need
//         // to be called via shutdown hooks if necessary.
//         wm.close()
//     }
// }