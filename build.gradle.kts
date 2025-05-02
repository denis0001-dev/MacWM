plugins {
    kotlin("multiplatform") version "1.9.20"
}

repositories {
    mavenCentral()
}

kotlin {
    linuxX64 {
        binaries {
            executable {
                entryPoint = "main"
            }
        }
        compilations.getByName("main") {
            cinterops {
                val `wayland-client` by creating {
                    defFile = file("src/nativeInterop/cinterop/wayland-client.def")
                }
                val `wayland-client-core` by creating {
                    defFile = file("src/nativeInterop/cinterop/wayland-client-core.def")
                }
                val `wayland-client-protocol` by creating {
                    defFile = file("src/nativeInterop/cinterop/wayland-client-protocol.def")
                }
                val `wayland-cursor` by creating {
                    defFile = file("src/nativeInterop/cinterop/wayland-cursor.def")
                }
                val `wayland-egl` by creating {
                    defFile = file("src/nativeInterop/cinterop/wayland-egl.def")
                }
                val `wayland-egl-core` by creating {
                    defFile = file("src/nativeInterop/cinterop/wayland-egl-core.def")
                }
                val `wayland-util` by creating {
                    defFile = file("src/nativeInterop/cinterop/wayland-util.def")
                }
                val `wayland-version` by creating {
                    defFile = file("src/nativeInterop/cinterop/wayland-version.def")
                }
            }
        }
    }

    sourceSets {
        val linuxX64Main by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")
            }
        }
    }
} 