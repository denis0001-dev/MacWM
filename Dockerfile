FROM ubuntu:24.10

# Install dependencies with BuildKit cache mounts
RUN --mount=type=cache,target=/var/cache/apt \
    --mount=type=cache,target=/var/lib/apt \
    apt-get update && apt-get install -y \
    build-essential \
    cmake \
    pkg-config \
    libx11-dev \
    libpng-dev \
    libsdl2-dev \
    libsdl2-image-dev \
    x11-apps \
    xterm \
    gdbserver \
    xinit \
    xserver-xephyr \
    fltk1.3-dev \
    && rm -rf /var/lib/apt/lists/*

# Set up workdir
WORKDIR /app

# Copy source
COPY . .

# Build in debug mode
RUN mkdir -p build \
    && cmake -DCMAKE_BUILD_TYPE=Debug -S . -B build \
    && cmake --build build

# Xephyr
RUN echo $(whereis -b Xephyr | sed -E 's/^.*: ?//') > .xephyr && \
    if [ -z "$(cat .xephyr)" ] || [ ! -e "$(cat .xephyr)" ]; then \
        echo "Xephyr doesn't exist, file:" && \
        cat .xephyr || true && \
        exit 1; \
    fi

ARG DEBUG=true
ENV DEBUGGING=$DEBUG

# Default command: run entry.sh to set up X11 and launch the app
CMD xinit ./xinitrc -- \
        "$(cat .xephyr)" \
            :100 \
            -ac \
            -screen 1280x800 \
            -host-cursor