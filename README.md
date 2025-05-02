# MacWM2 - macOS-like Wayland Desktop Environment

A simple Wayland desktop environment written in Kotlin/Native that tries to replicate the macOS look and feel.

## Prerequisites

- Kotlin/Native
- Wayland development libraries
- Gradle
- Linux system with Wayland support

## Building

1. Install the required Wayland development libraries:
```bash
sudo apt-get install libwayland-dev
```

2. Build the project:
```bash
./gradlew build
```

## Running

After building, you can run the desktop environment:
```bash
./gradlew run
```

## Features

- macOS-like window management
- Dock with application icons
- Title bars with window controls
- Basic window compositing

## Development

The project is structured as follows:
- `Compositor.kt`: Handles Wayland protocol interactions
- `Window.kt`: Manages windows and their decorations
- `Dock.kt`: Implements the macOS-like dock
- `Main.kt`: Entry point and main event loop

## License

MIT License 