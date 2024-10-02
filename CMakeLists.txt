cmake_minimum_required(VERSION 3.30)
project(MacWM)

find_package(PkgConfig REQUIRED)
pkg_check_modules(X11 REQUIRED x11)
pkg_check_modules(GLOG REQUIRED)
pkg_check_modules(GLOG REQUIRED libglog)
find_package(PNG REQUIRED)

include_directories(${X11_INCLUDE_DIRS})
include_directories(${GLOG_INCLUDE_DIRS})

add_executable(macwm src/main.cpp src/util.cpp src/window_manager.cpp)
target_compile_options(macwm PRIVATE -Wall -g -std=c++1y)
target_compile_definitions(macwm PRIVATE GLOG_USE_GLOG_EXPORT)

target_link_libraries(macwm ${X11_LIBRARIES} ${GLOG_LIBRARIES})

set(CMAKE_RUNTIME_OUTPUT_DIRECTORY_<CONFIG>  ${CMAKE_BINARY_DIR})
set(EXECUTABLE_OUTPUT_PATH ${CMAKE_BINARY_DIR} CACHE PATH "Build directory" FORCE)