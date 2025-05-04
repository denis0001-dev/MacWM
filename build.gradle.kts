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
                val Xlib by creating {
                    defFile = file("src/nativeInterop/cinterop/Xlib.def")
                }
                val X by creating {
                    defFile = file("src/nativeInterop/cinterop/X.def")
                }
                val Xutil by creating {
                    defFile = file("src/nativeInterop/cinterop/Xutil.def")
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