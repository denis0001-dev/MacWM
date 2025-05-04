@file:OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
@file:Suppress("unused")

package ru.denis0001dev.macwm;

import kotlinx.cinterop.*
import xorg.constants.CWHeight
import xorg.constants.CWWidth
import xorg.constants.CWX
import xorg.constants.CWY
import xorg.constants.ClientMessage
import xorg.lib.* // Assuming Xlib bindings are generated in this package
import kotlin.experimental.ExperimentalNativeApi

object XUtils {
    fun createSimpleWindow(
        display: CPointer<Display>?,
        parent: Window,
        x: Int,
        y: Int,
        width: UInt,
        height: UInt,
        border_width: UInt,
        border_color: ULong,
        background_color: ULong
    ): Window {
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
        )
    }

    fun mapWindow(display: CPointer<Display>?, window: Window) {
        XMapWindow(display, window)
    }

    fun unmapWindow(display: CPointer<Display>?, window: Window) {
        XUnmapWindow(display, window)
    }

    fun destroyWindow(display: CPointer<Display>?, window: Window) {
        XDestroyWindow(display, window)
    }

    fun reparentWindow(display: CPointer<Display>?, window: Window, parent: Window, x: Int, y: Int) {
        XReparentWindow(display, window, parent, x, y)
    }

    fun getWindowAttributes(display: CPointer<Display>?, window: Window): XWindowAttributes {
        memScoped {
            val attrs = alloc<XWindowAttributes>()
            // Note: XGetWindowAttributes returns Status (int), non-zero on success.
            assert(XGetWindowAttributes(display, window, attrs.ptr) != 0) {
                "XGetWindowAttributes failed"
            }
            return attrs.readValue().useContents { this }
        }
    }

    fun configureWindow(
        display: CPointer<Display>?,
        window: Window,
        x: Int,
        y: Int,
        width: Int,
        height: Int
    ) {
        memScoped {
            val changes = alloc<XWindowChanges>()
            changes.x = x
            changes.y = y
            changes.width = width
            changes.height = height
            XConfigureWindow(display, window, (CWX or CWY or CWWidth or CWHeight).convert(), changes.ptr)
        }
    }

    fun moveWindow(display: CPointer<Display>?, window: Window, x: Int, y: Int) {
        XMoveWindow(display, window, x, y)
    }

    fun resizeWindow(
        display: CPointer<Display>?,
        window: Window,
        width: UInt,
        height: UInt
    ) {
        XResizeWindow(display, window, width, height)
    }

    fun moveResizeWindow(
        display: CPointer<Display>?,
        window: Window,
        x: Int,
        y: Int,
        width: UInt,
        height: UInt
    ) {
        XMoveResizeWindow(display, window, x, y, width, height)
    }

    fun selectInput(display: CPointer<Display>?, window: Window, event_mask: Long) {
        XSelectInput(display, window, event_mask)
    }

    fun grabButton(
        display: CPointer<Display>?,
        button: UInt,
        modifiers: UInt,
        grab_window: Window,
        owner_events: Boolean,
        event_mask: UInt,
        pointer_mode: Int,
        keyboard_mode: Int,
        confine_to: Window,
        cursor: Cursor
    ) {
        XGrabButton(display, button, modifiers, grab_window, owner_events.compareTo(false), // Convert Boolean to Int (0 or 1)
            event_mask, pointer_mode, keyboard_mode, confine_to, cursor)
    }

    fun grabKey(
        display: CPointer<Display>?,
        keycode: Int,
        modifiers: UInt,
        grab_window: Window,
        owner_events: Boolean,
        pointer_mode: Int,
        keyboard_mode: Int
    ) {
        XGrabKey(display, keycode, modifiers, grab_window, owner_events.compareTo(false), // Convert Boolean to Int (0 or 1)
            pointer_mode, keyboard_mode)
    }

    fun raiseWindow(display: CPointer<Display>?, window: Window) {
        XRaiseWindow(display, window)
    }

    fun iconifyWindow(display: CPointer<Display>?, window: Window, screen: Int) {
        XIconifyWindow(display, window, screen)
    }

    fun setInputFocus(display: CPointer<Display>?, window: Window, revert_to: Int, time: Time) {
        XSetInputFocus(display, window, revert_to, time)
    }

    fun internAtom(display: CPointer<Display>?, name: String, only_if_exists: Boolean): Atom {
        return XInternAtom(display, name, only_if_exists.compareTo(false)) // Convert Boolean to Int (0 or 1)
    }

    // Note: The caller is responsible for freeing the memory allocated by Xlib for 'protocols' using XFree
    // if the function returns true and 'protocols' is populated.
    // FIXME error
    fun getWMProtocols(display: CPointer<Display>?, window: Window, protocols: CPointer<CPointerVar<Atom>>?, count: CPointer<IntVar>?): Boolean {
        // XGetWMProtocols returns Status (int), non-zero on success.
        return XGetWMProtocols(display, window, protocols, count) != 0
    }

    fun sendClientMessage(
        display: CPointer<Display>?,
        window: Window, // Destination window
        message_type: Atom,
        data0: Long,
        data1: Long,
        data2: Long,
        data3: Long,
        data4: Long
    ) {
        memScoped {
            val msg = alloc<XEvent>()
            msg.type = ClientMessage // Set the event type

            // Populate the xclient structure within the XEvent union
            msg.xclient.type = ClientMessage // Also set type within xclient
            msg.xclient.serial = 0u // Typically 0 unless tracking replies
            msg.xclient.send_event = True // Indicate it's a sent event
            msg.xclient.display = display // Display the event originated from (can be null)
            msg.xclient.window = window // The *target* window for the message
            msg.xclient.message_type = message_type
            msg.xclient.format = 32 // Data format (8, 16, or 32 bits)

            // Access the data union (assuming 32-bit format, use 'l' array)
            msg.xclient.data.l[0] = data0
            msg.xclient.data.l[1] = data1
            msg.xclient.data.l[2] = data2
            msg.xclient.data.l[3] = data3
            msg.xclient.data.l[4] = data4

            // Send the event
            // The third argument 'propagate' is false.
            // The fourth argument 'event_mask' is 0 (or NoEventMask) as ClientMessage is not maskable this way.
            // The destination window for XSendEvent should be the *recipient* window,
            // which is often the root window or the target application's window depending on the protocol.
            // The C++ code uses 'window' (the parameter) as the destination, assuming it's the correct target.
            // Note: XSendEvent returns Status (int), non-zero on success.
            assert(XSendEvent(display, window, False, 0L /* NoEventMask */, msg.ptr) != 0) {
                "XSendEvent failed"
            }
        }
    }


    fun getErrorText(display: CPointer<Display>?, error_code: Int): String {
        val MAX_ERROR_TEXT_LENGTH = 1024
        memScoped {
            val error_text_buffer = allocArray<ByteVar>(MAX_ERROR_TEXT_LENGTH)
            XGetErrorText(display, error_code, error_text_buffer, MAX_ERROR_TEXT_LENGTH)
            return error_text_buffer.toKString()
        }
    }

    fun getRequestCodeName(request_code: UByte): String? {
        // XRequestCodeToString returns char* which might be null
        return XRequestCodeToString(request_code)
    }

    fun grabServer(display: CPointer<Display>?) {
        XGrabServer(display)
    }

    fun ungrabServer(display: CPointer<Display>?) {
        XUngrabServer(display)
    }

    // Note: The caller is responsible for freeing the memory allocated by Xlib for 'children_return' using XFree
    // if the function returns true and 'children_return' is populated.
    // FIXME error
    fun queryTree(
        display: CPointer<Display>?,
        window: Window,
        root_return: CPointer<WindowVar>?,
        parent_return: CPointer<WindowVar>?,
        children_return: CPointer<CPointerVar<Window>>?,
        nchildren_return: CPointer<UIntVar>?
    ): Boolean {
        // XQueryTree returns Status (int), non-zero on success.
        return XQueryTree(
            display,
            window,
            root_return,
            parent_return,
            children_return,
            nchildren_return
        ) != 0
    }

    fun addToSaveSet(display: CPointer<Display>?, window: Window) {
        XAddToSaveSet(display, window)
    }

    fun removeFromSaveSet(display: CPointer<Display>?, window: Window) {
        XRemoveFromSaveSet(display, window)
    }
}
