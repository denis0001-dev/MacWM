@file:OptIn(ExperimentalForeignApi::class)
package ru.denis0001dev.macwm;

// Dependencies and Imports
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.memScoped
import platform.posix.F_OK
import platform.posix.access
import platform.posix.fflush
import platform.posix.fprintf
import platform.posix.stderr
import xorg.constants.ButtonPress
import xorg.constants.ButtonRelease
import xorg.constants.CWBorderWidth
import xorg.constants.CWHeight
import xorg.constants.CWSibling
import xorg.constants.CWStackMode
import xorg.constants.CWWidth
import xorg.constants.CWX
import xorg.constants.CWY
import xorg.constants.ConfigureNotify
import xorg.constants.ConfigureRequest
import xorg.constants.CreateNotify
import xorg.constants.DestroyNotify
import xorg.constants.KeyPress
import xorg.constants.KeyRelease
import xorg.constants.LASTEvent
import xorg.constants.MapNotify
import xorg.constants.MapRequest
import xorg.constants.MotionNotify
import xorg.constants.ReparentNotify
import xorg.constants.UnmapNotify
import xorg.lib.Window
import xorg.lib.XEvent

// UTIL SOURCE util.cpp + HEADER util.hpp Merged

// Global constants replacing macros
const val RED: Int = 0xff0000 // Note: X11 colors are often ULong, but C++ used int literal
const val BLACK: Int = 0x0000ff // Note: X11 colors are often ULong, but C++ used int literal


fun errln(c: Any?) {
    // Print to stderr using POSIX fprintf
    memScoped {
        val message = c.toString() + "\n"
        fprintf(stderr, "%s", message)
        fflush(stderr)
    }
}

// Represents a 2D size.
data class Size<T>(
    var width: T,
    var height: T
) {
    // No default constructor needed like C++, Kotlin provides if all args have defaults (none here)
    // Constructor Size(T w, T h) is the primary constructor

    // [[nodiscard]] is not directly applicable in Kotlin, but function purity can be inferred or documented.
    override fun toString() = "${width}x${height}"
}

// Outputs a Size<T> as a string to a std::ostream.
// Kotlin uses toString() typically, but we can provide an extension function
// if needed, though direct stream insertion isn't the Kotlin way.
// The C++ operator<< is usually replaced by implementing toString() or specific formatters.
// We already have toString() in the class.

// Represents a 2D position.
data class Position<T>(
    var x: T,
    var y: T
) {
    // No default constructor needed like C++

    // [[nodiscard]] is not directly applicable in Kotlin
    override fun toString() = "($x, $y)"
}

// Represents a 2D vector.
data class Vector2D<T>(
    var x: T,
    var y: T
) {
    // No default constructor needed like C++

    // [[nodiscard]] is not directly applicable in Kotlin
    override fun toString() = "($x, $y)"
}

// Outputs a Position<T> as a string to a std::ostream.
// Similar comment as for Size<T> regarding operator<<

// Outputs a Vector2D<T> as a string to a std::ostream.
// Similar comment as for Size<T> regarding operator<<


// Position operators.
// Assuming T supports minus operation
operator fun <T: Number> Position<T>.minus(b: Position<T>): Vector2D<Double> {
    // Kotlin requires explicit type handling for arithmetic. Common type is Double.
    // If T is guaranteed to be Int, specific overloads can be made.
    // This generic version assumes conversion to Double is acceptable.
    return Vector2D<Double>(this.x.toDouble() - b.x.toDouble(), this.y.toDouble() - b.y.toDouble())
}
// Specific overload for Int
operator fun Position<Int>.minus(b: Position<Int>): Vector2D<Int> {
    return Vector2D<Int>(this.x - b.x, this.y - b.y)
}

// Assuming T supports plus operation
operator fun <T: Number> Position<T>.plus(v: Vector2D<T>): Position<Double> {
    return Position<Double>(this.x.toDouble() + v.x.toDouble(), this.y.toDouble() + v.y.toDouble())
}
// Specific overload for Int
operator fun Position<Int>.plus(v: Vector2D<Int>): Position<Int> {
    return Position<Int>(this.x + v.x, this.y + v.y)
}

// Commutative plus
operator fun <T: Number> Vector2D<T>.plus(a: Position<T>): Position<Double> {
    return Position<Double>(a.x.toDouble() + this.x.toDouble(), a.y.toDouble() + this.y.toDouble())
}
// Specific overload for Int
operator fun Vector2D<Int>.plus(a: Position<Int>): Position<Int> {
    return Position<Int>(a.x + this.x, a.y + this.y)
}


// Assuming T supports minus operation
operator fun <T: Number> Position<T>.minus(v: Vector2D<T>): Position<Double> {
    return Position<Double>(this.x.toDouble() - v.x.toDouble(), this.y.toDouble() - v.y.toDouble())
}
// Specific overload for Int
operator fun Position<Int>.minus(v: Vector2D<Int>): Position<Int> {
    return Position<Int>(this.x - v.x, this.y - v.y)
}


// Size operators.
// Assuming T supports minus operation
operator fun <T: Number> Size<T>.minus(b: Size<T>): Vector2D<Double> {
    return Vector2D<Double>(this.width.toDouble() - b.width.toDouble(), this.height.toDouble() - b.height.toDouble())
}
// Specific overload for Int
operator fun Size<Int>.minus(b: Size<Int>): Vector2D<Int> {
    return Vector2D<Int>(this.width - b.width, this.height - b.height)
}

// Assuming T supports plus operation
operator fun <T: Number> Size<T>.plus(v: Vector2D<T>): Size<Double> {
    return Size<Double>(this.width.toDouble() + v.x.toDouble(), this.height.toDouble() + v.y.toDouble())
}
// Specific overload for Int
operator fun Size<Int>.plus(v: Vector2D<Int>): Size<Int> {
    return Size<Int>(this.width + v.x, this.height + v.y)
}

// Commutative plus
operator fun <T: Number> Vector2D<T>.plus(a: Size<T>): Size<Double> {
    return Size<Double>(a.width.toDouble() + this.x.toDouble(), a.height.toDouble() + this.y.toDouble())
}
// Specific overload for Int
operator fun Vector2D<Int>.plus(a: Size<Int>): Size<Int> {
    return Size<Int>(a.width + this.x, a.height + this.y)
}

// Assuming T supports minus operation
operator fun <T: Number> Size<T>.minus(v: Vector2D<T>): Size<Double> {
    return Size<Double>(this.width.toDouble() - v.x.toDouble(), this.height.toDouble() - v.y.toDouble())
}
// Specific overload for Int
operator fun Size<Int>.minus(v: Vector2D<Int>): Size<Int> {
    return Size<Int>(this.width - v.x, this.height - v.y)
}

// Returns a string representation of a built-in type that we already have
// ostream support for.
// Kotlin's standard toString() handles this for most types.
// Special handling for Boolean to match C++ `<< bool`.
fun <T> toString(x: T): String {
    return when (x) {
        is Boolean -> if (x) "1" else "0" // Match C++ bool output to stream (often 1 or 0)
        // If "true"/"false" is desired (like C++ with boolalpha), use x.toString()
        is Window -> "0x${x.toULong().toString(16)}" // Format Window (ULong) as hex
        else -> x.toString() // Default Kotlin toString()
    }
}

// Overload for Boolean specifically if "true"/"false" is needed instead of 1/0
fun toString(x: Boolean): String {
    return if (x) "true" else "false" // Explicitly "true" or "false"
}


// Returns a string describing an X event for debugging purposes.
// Note: Needs XEvent definition from xorg.lib
fun toString(e: XEvent): String {
    // ReSharper disable CppVariableCanBeMadeConstexpr -> Not applicable in Kotlin
    // Use a private top-level val for the array
    val X_EVENT_TYPE_NAMES = arrayOf(
        "",                 // 0
        "",                 // 1
        "KeyPress",         // 2
        "KeyRelease",       // 3
        "ButtonPress",      // 4
        "ButtonRelease",    // 5
        "MotionNotify",     // 6
        "EnterNotify",      // 7
        "LeaveNotify",      // 8
        "FocusIn",          // 9
        "FocusOut",         // 10
        "KeymapNotify",     // 11
        "Expose",           // 12
        "GraphicsExpose",   // 13
        "NoExpose",         // 14
        "VisibilityNotify", // 15
        "CreateNotify",     // 16
        "DestroyNotify",    // 17
        "UnmapNotify",      // 18
        "MapNotify",        // 19
        "MapRequest",       // 20
        "ReparentNotify",   // 21
        "ConfigureNotify",  // 22
        "ConfigureRequest", // 23
        "GravityNotify",    // 24
        "ResizeRequest",    // 25
        "CirculateNotify",  // 26
        "CirculateRequest", // 27
        "PropertyNotify",   // 28
        "SelectionClear",   // 29
        "SelectionRequest", // 30
        "SelectionNotify",  // 31
        "ColormapNotify",   // 32
        "ClientMessage",    // 33
        "MappingNotify",    // 34
        "GenericEvent"      // 35 - Note: C++ used GeneralEvent, Xlib uses GenericEvent
        // LASTEvent is typically 36, so indices up to 35 are valid.
    )

    val eventType = e.type
    if (eventType < 2 || eventType >= LASTEvent) { // LASTEvent should be defined in xorg.lib
        // Use Kotlin string building
        return "Unknown ($eventType)"
    }

    // 1. Compile properties we care about.
    val properties = mutableListOf<Pair<String, String>>()
    // Use 'when' instead of 'switch'
    // Accessing union members in C requires specific syntax depending on the cinterop tool.
    // Assuming direct access like e.xcreatewindow is possible.
    when (eventType) {
        CreateNotify -> {
            val ev = e.xcreatewindow // Assuming cinterop provides direct access
            properties.add("window" to toString(ev.window))
            properties.add("parent" to toString(ev.parent))
            properties.add("size" to Size(ev.width, ev.height).toString())
            properties.add("position" to Position(ev.x, ev.y).toString())
            properties.add("border_width" to toString(ev.border_width))
            // Xlib Bool is typically int, 0 for False, non-zero for True
            properties.add("override_redirect" to toString(ev.override_redirect != 0))
        }
        DestroyNotify -> {
            val ev = e.xdestroywindow
            properties.add("window" to toString(ev.window))
        }
        MapNotify -> {
            val ev = e.xmap
            properties.add("window" to toString(ev.window))
            properties.add("event" to toString(ev.event)) // event is a Window ID
            properties.add("override_redirect" to toString(ev.override_redirect != 0))
        }
        UnmapNotify -> {
            val ev = e.xunmap
            properties.add("window" to toString(ev.window))
            properties.add("event" to toString(ev.event)) // event is a Window ID
            properties.add("from_configure" to toString(ev.from_configure != 0))
        }
        ConfigureNotify -> {
            val ev = e.xconfigure
            properties.add("window" to toString(ev.window))
            properties.add("size" to Size(ev.width, ev.height).toString())
            properties.add("position" to Position(ev.x, ev.y).toString())
            properties.add("border_width" to toString(ev.border_width))
            properties.add("override_redirect" to toString(ev.override_redirect != 0))
        }
        ReparentNotify -> {
            val ev = e.xreparent
            properties.add("window" to toString(ev.window))
            properties.add("parent" to toString(ev.parent))
            properties.add("position" to Position(ev.x, ev.y).toString())
            properties.add("override_redirect" to toString(ev.override_redirect != 0))
        }
        MapRequest -> {
            val ev = e.xmaprequest
            properties.add("window" to toString(ev.window))
        }
        ConfigureRequest -> {
            val ev = e.xconfigurerequest
            properties.add("window" to toString(ev.window))
            properties.add("parent" to toString(ev.parent))
            // value_mask is unsigned long, use ULong in Kotlin
            properties.add("value_mask" to XConfigureWindowValueMasktoString(ev.value_mask.toULong()))
            properties.add("position" to Position(ev.x, ev.y).toString())
            properties.add("size" to Size(ev.width, ev.height).toString())
            properties.add("border_width" to toString(ev.border_width))
        }
        ButtonPress, ButtonRelease -> {
            val ev = e.xbutton
            properties.add("window" to toString(ev.window))
            properties.add("button" to toString(ev.button)) // button is unsigned int
            properties.add("position_root" to Position(ev.x_root, ev.y_root).toString())
        }
        MotionNotify -> {
            val ev = e.xmotion
            properties.add("window" to toString(ev.window))
            properties.add("position_root" to Position(ev.x_root, ev.y_root).toString())
            properties.add("state" to toString(ev.state)) // state is unsigned int
            properties.add("time" to toString(ev.time))   // time is Time (unsigned long)
        }
        KeyPress, KeyRelease -> {
            val ev = e.xkey
            properties.add("window" to toString(ev.window))
            properties.add("state" to toString(ev.state))     // state is unsigned int
            properties.add("keycode" to toString(ev.keycode)) // keycode is unsigned int
        }
        else -> {
            // No properties are printed for unused events.
        }
    }

    // 2. Build final string.
    val propertiesString = properties
        .joinToString {
            "${it.first}: ${it.second}"
        }

    // Use safe indexing for the event type name
    val eventName = if (eventType >= 0 && eventType < X_EVENT_TYPE_NAMES.size) {
        X_EVENT_TYPE_NAMES[eventType]
    } else {
        "InvalidEventType" // Should not happen due to earlier check
    }

    return "$eventName { $propertiesString }"
}

// Returns a string describing an X window configuration value mask.
// value_mask is unsigned long in C, so ULong in Kotlin.
fun XConfigureWindowValueMasktoString(value_mask: ULong): String {
    val masks = mutableListOf<String>()
    // Use ULong constants from xorg.lib, assuming they exist
    // Need to ensure the constants like CWX are ULong or compatible
    if ((value_mask and CWX.toULong()) != 0UL) { // CWX etc. are likely Int defines, convert to ULong
        masks.add("X")
    }
    if ((value_mask and CWY.toULong()) != 0UL) {
        masks.add("Y")
    }
    if ((value_mask and CWWidth.toULong()) != 0UL) {
        masks.add("Width")
    }
    if ((value_mask and CWHeight.toULong()) != 0UL) {
        masks.add("Height")
    }
    if ((value_mask and CWBorderWidth.toULong()) != 0UL) {
        masks.add("BorderWidth")
    }
    if ((value_mask and CWSibling.toULong()) != 0UL) {
        masks.add("Sibling")
    }
    if ((value_mask and CWStackMode.toULong()) != 0UL) {
        masks.add("StackMode")
    }
    return masks.joinToString("|")
}

// Returns the name of an X request code.
// request_code is unsigned char in C, so UByte in Kotlin.
fun XRequestCodeToString(request_code: UByte): String {
    // ReSharper disable CppVariableCanBeMadeConstexpr -> Not applicable
    // Use a private top-level val for the array
    val X_REQUEST_CODE_NAMES = arrayOf(
        "",                        // 0
        "CreateWindow",            // 1
        "ChangeWindowAttributes",  // 2
        "GetWindowAttributes",     // 3
        "DestroyWindow",           // 4
        "DestroySubwindows",       // 5
        "ChangeSaveSet",           // 6
        "ReparentWindow",          // 7
        "MapWindow",               // 8
        "MapSubwindows",           // 9
        "UnmapWindow",             // 10
        "UnmapSubwindows",         // 11
        "ConfigureWindow",         // 12
        "CirculateWindow",         // 13
        "GetGeometry",             // 14
        "QueryTree",               // 15
        "InternAtom",              // 16
        "GetAtomName",             // 17
        "ChangeProperty",          // 18
        "DeleteProperty",          // 19
        "GetProperty",             // 20
        "ListProperties",          // 21
        "SetSelectionOwner",       // 22
        "GetSelectionOwner",       // 23
        "ConvertSelection",        // 24
        "SendEvent",               // 25
        "GrabPointer",             // 26
        "UngrabPointer",           // 27
        "GrabButton",              // 28
        "UngrabButton",            // 29
        "ChangeActivePointerGrab", // 30
        "GrabKeyboard",            // 31
        "UngrabKeyboard",          // 32
        "GrabKey",                 // 33
        "UngrabKey",               // 34
        "AllowEvents",             // 35
        "GrabServer",              // 36
        "UngrabServer",            // 37
        "QueryPointer",            // 38
        "GetMotionEvents",         // 39
        "TranslateCoords",         // 40
        "WarpPointer",             // 41
        "SetInputFocus",           // 42
        "GetInputFocus",           // 43
        "QueryKeymap",             // 44
        "OpenFont",                // 45
        "CloseFont",               // 46
        "QueryFont",               // 47
        "QueryTextExtents",        // 48
        "ListFonts",               // 49
        "ListFontsWithInfo",       // 50
        "SetFontPath",             // 51
        "GetFontPath",             // 52
        "CreatePixmap",            // 53
        "FreePixmap",              // 54
        "CreateGC",                // 55
        "ChangeGC",                // 56
        "CopyGC",                  // 57
        "SetDashes",               // 58
        "SetClipRectangles",       // 59
        "FreeGC",                  // 60
        "ClearArea",               // 61
        "CopyArea",                // 62
        "CopyPlane",               // 63
        "PolyPoint",               // 64
        "PolyLine",                // 65
        "PolySegment",             // 66
        "PolyRectangle",           // 67
        "PolyArc",                 // 68
        "FillPoly",                // 69
        "PolyFillRectangle",       // 70
        "PolyFillArc",             // 71
        "PutImage",                // 72
        "GetImage",                // 73
        "PolyText8",               // 74
        "PolyText16",              // 75
        "ImageText8",              // 76
        "ImageText16",             // 77
        "CreateColormap",          // 78
        "FreeColormap",            // 79
        "CopyColormapAndFree",     // 80
        "InstallColormap",         // 81
        "UninstallColormap",       // 82
        "ListInstalledColormaps",  // 83
        "AllocColor",              // 84
        "AllocNamedColor",         // 85
        "AllocColorCells",         // 86
        "AllocColorPlanes",        // 87
        "FreeColors",              // 88
        "StoreColors",             // 89
        "StoreNamedColor",         // 90
        "QueryColors",             // 91
        "LookupColor",             // 92
        "CreateCursor",            // 93
        "CreateGlyphCursor",       // 94
        "FreeCursor",              // 95
        "RecolorCursor",           // 96
        "QueryBestSize",           // 97
        "QueryExtension",          // 98
        "ListExtensions",          // 99
        "ChangeKeyboardMapping",   // 100
        "GetKeyboardMapping",      // 101
        "ChangeKeyboardControl",   // 102
        "GetKeyboardControl",      // 103
        "Bell",                    // 104
        "ChangePointerControl",    // 105
        "GetPointerControl",       // 106
        "SetScreenSaver",          // 107
        "GetScreenSaver",          // 108
        "ChangeHosts",             // 109
        "ListHosts",               // 110
        "SetAccessControl",        // 111
        "SetCloseDownMode",        // 112
        "KillClient",              // 113
        "RotateProperties",        // 114
        "ForceScreenSaver",        // 115
        "SetPointerMapping",       // 116
        "GetPointerMapping",       // 117
        "SetModifierMapping",      // 118
        "GetModifierMapping",      // 119
        // Indices 120-127 are potentially undefined or reserved
        "", "", "", "", "", "", "", "", // 120-127
        "NoOperation"              // 127 in some contexts, but often listed after GetModifierMapping
        // Let's assume the C++ array size implies indices up to 127 are potentially valid access points.
        // The original C++ array seems to stop at index 119 (NoOperation is often 127, but listed last here).
        // We need to be careful about array bounds. Let's assume the C++ array was implicitly sized.
        // Adding placeholders up to 127 for safety if NoOperation is indeed 127.
        // If NoOperation is 120, adjust accordingly. Assuming C++ array had 128 elements based on common X request codes.
        // Let's stick to the literal C++ definition which seems to end at index 119.
        // If request_code > 119, it will go out of bounds in C++. Let's replicate that potential issue,
        // or add bounds checking. Adding bounds check for robustness.
    )
    val index = request_code.toInt()
    return if (index >= 0 && index < X_REQUEST_CODE_NAMES.size && X_REQUEST_CODE_NAMES[index].isNotEmpty()) {
        X_REQUEST_CODE_NAMES[index]
    } else {
        "UnknownRequestCode($index)" // Provide more info than just empty string or crash
    }
}

// Checks if a file exists using POSIX access function.
fun file_exists(file_path: String): Boolean {
    // Use platform.posix.access to check file existence
    // R_OK checks read permission, F_OK checks existence. F_OK is more direct.
    return access(file_path, F_OK) == 0
}

// Checks if a file path string ends with ".png".
fun file_ends_with_png(file_path: String): Boolean {
    val extension = ".png"
    // Use Kotlin's built-in endsWith function
    return file_path.endsWith(extension)
    // The C++ code's comparison logic is exactly what endsWith does.
    // The NOLINT comment is not relevant to Kotlin.
}

// #endif is not needed in Kotlin