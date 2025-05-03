#include <iostream>
#include <thread>
#include <SDL2/SDL.h>
#include <SDL2/SDL_image.h>

#define USAGE "Usage" << argv[0] << " <image path>\n"
#define init SDL_Init(SDL_INIT_VIDEO) == 0
#define ifnotinit if(!init)
#define window(img) \
    SDL_CreateWindow("", SDL_WINDOWPOS_UNDEFINED, SDL_WINDOWPOS_UNDEFINED, \
    img->w, img->h, SDL_WINDOW_SHOWN)
#define renderer(win) SDL_CreateRenderer(win, -1, 0)
#define texture(rend, img) SDL_CreateTextureFromSurface(rend, img)
#define renderImg(rend, texture) SDL_RenderClear(rend); \
    SDL_RenderCopy(rend, texture, nullptr, nullptr); \
    SDL_RenderPresent(rend)
#define quit(texture, rend, win) SDL_DestroyTexture(texture); \
    SDL_DestroyRenderer(rend); \
    SDL_DestroyWindow(win); \
    SDL_Quit(); \
    return 0
#define ifnotimg(imgvar, file) SDL_Surface* imgvar = IMG_Load(file); \
    if (imgvar == nullptr)
#define ifnotwindow(winvar, img) SDL_Window* winvar = window(img); \
    if (winvar == nullptr)
#define ifnotrenderer(rendvar, win) SDL_Renderer* rendvar = renderer(win); \
    if (rendvar == nullptr)
#define ifnottexture(texturevar, rend, img) SDL_Texture* texturevar = texture(rend, img); \
    if (texturevar == nullptr)
#define eventLoop(eventvar, donevar) SDL_Event eventvar; \
    bool donevar = false; \
    while (!done)
#define sleep(val) std::this_thread::sleep_for(val)
#define DELAY 5ms

using namespace std::chrono_literals;
int main(const int argc, char* argv[]) {
    if (argc != 2) {
        std::cout << USAGE;
        return 1;
    }
    // Initialize SDL
    ifnotinit {
        std::cout << "Error initializing SDL: " << SDL_GetError() << std::endl;
        return 1;
    }
    // Load the image
    ifnotimg(image, argv[1]) {
        std::cout << "Error loading image: " << SDL_GetError() << std::endl;
        return 1;
    }
    // Create a window
    ifnotwindow(window, image) {
        std::cout << "Error creating window: " << SDL_GetError() << std::endl;
        return 1;
    }
    // Create a renderer
    ifnotrenderer(renderer, window) {
        std::cout << "Error creating renderer: " << SDL_GetError() << std::endl;
        return 1;
    }
    // Copy the image to the renderer
    ifnottexture(texture, renderer, image) {
        std::cout << "Error creating texture: " << SDL_GetError() << std::endl;
        return 1;
    }

    // Render the texture to the window
    renderImg(renderer, texture);

    // Wait for the window to be closed
    eventLoop(event, done) {
        // Render the texture to the window
        renderImg(renderer, texture);
        while (SDL_PollEvent(&event)) {
            if (event.type == SDL_WINDOWEVENT_CLOSE) {
                done = true;
            }
        }
        sleep(DELAY);
    }

    // Clean up
    quit(texture, renderer, window);
}