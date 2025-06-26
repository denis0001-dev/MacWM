CXX = g++
CXXFLAGS = -Wall -g -std=c++1y $(shell pkg-config --cflags x11) -I/opt/X11/include
LDFLAGS = $(shell pkg-config --libs x11) -lXext
SRC_DIR = src
OBJ_DIR = build/obj
BIN_DIR = build/bin
SRC = $(wildcard $(SRC_DIR)/*.cpp)
OBJ = $(patsubst $(SRC_DIR)/%.cpp,$(OBJ_DIR)/%.o,$(SRC))
BIN = $(BIN_DIR)/flwm
XEPHYR = $(shell whereis -b Xephyr | sed -E 's/^.*: ?//')

all: $(BIN)

$(OBJ_DIR):
	mkdir -p $(OBJ_DIR)

$(BIN_DIR):
	mkdir -p $(BIN_DIR)

$(OBJ_DIR)/%.o: $(SRC_DIR)/%.cpp | $(OBJ_DIR)
	$(CXX) $(CXXFLAGS) -c $< -o $@

$(BIN): $(OBJ) | $(BIN_DIR)
	$(CXX) $(CXXFLAGS) $^ -o $@ $(LDFLAGS)

compile: $(BIN)

run: compile
	# Check that Xephyr exists
	if [ -z "$(XEPHYR)" ]; then \
		echo "Xephyr not found!"; \
		exit 0; \
  	fi; \
	xinit ./xinitrc -- \
        "$(XEPHYR)" \
            :100 \
            -ac \
            -screen 1280x800 \
            -host-cursor

clean:
	rm -rf build