cmake_minimum_required(VERSION 3.28)
project(MacWM)

# Compiler is g++
set(CMAKE_CXX_COMPILER "/usr/bin/g++" CACHE STRING "C++ compiler")

# Make the executables go into the 'build' folder
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY_<CONFIG>  ${CMAKE_BINARY_DIR})
set(EXECUTABLE_OUTPUT_PATH ${CMAKE_BINARY_DIR} CACHE PATH "Build directory" FORCE)

# Window manager
find_package(PkgConfig REQUIRED)
pkg_check_modules(X11 REQUIRED x11)
find_package(PNG REQUIRED)

add_executable(macwm src/main.cpp src/util.cpp src/window_manager.cpp src/logging.cpp src/assert.cpp src/x_utils.cpp)
target_compile_options(macwm PRIVATE -Wall -g -std=c++1y)
target_compile_definitions(macwm PRIVATE)
target_link_options(macwm PRIVATE -Iext)

target_link_libraries(macwm ${X11_LIBRARIES} ${X11_Xext_LIB})

# Wallpaper
find_package(SDL2 REQUIRED)
find_package(SDL2_image REQUIRED)

# Add custom target to compile wallpaper with SDL2 and SDL2_image
add_custom_target(wallpaper ALL
  COMMAND ${CMAKE_CXX_COMPILER} -o ./build/wallpaper ./src_wallpaper/main.cpp `sdl2-config --cflags --libs` -lSDL2_image
  WORKING_DIRECTORY ${CMAKE_SOURCE_DIR}
)
