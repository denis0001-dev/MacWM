cmake_minimum_required(VERSION 3.10)
project(MacWMWallpaper)

find_package(SDL2 REQUIRED)
find_package(SDL2_image REQUIRED)

add_executable(wallpaper main.cpp)
target_compile_options(wallpaper PRIVATE -Wall -g)
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY_<CONFIG>  ${CMAKE_BINARY_DIR})
set(EXECUTABLE_OUTPUT_PATH ${CMAKE_BINARY_DIR} CACHE PATH "Build directory" FORCE)

target_link_libraries(wallpaper ${SDL2_LIBRARIES} ${SDL2_IMAGE_LIBRARIES})
target_link_options(wallpaper PRIVATE -lSDL2_image)