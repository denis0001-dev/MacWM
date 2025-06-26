FROM ubuntu:24.10

# 1. Install dependencies
RUN --mount=type=cache,target=/var/cache/apt \
    --mount=type=cache,target=/var/lib/apt \
    apt-get update && apt-get install -y \
    build-essential \
    pkg-config \
    libfltk1.3-dev \
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

# 2. Args and env variables
ARG DEBUG=true
ENV DEBUG=$DEBUG

# 3. Final command
WORKDIR /app
CMD ["make", "run"]