PROJECTDIR = $(shell realpath .)
INDIR = $(PROJECTDIR)/src
OUTDIR = $(PROJECTDIR)/build

XEPHYR = $(shell whereis -b Xephyr | sed -E 's/^.*: ?//')

ifndef XEPHYR
	$(error "Xephyr not found!")
endif

all: cmake compile run

cmake:
	cmake -S $(PROJECTDIR)/ -B $(OUTDIR)/

compile:
	cd $(OUTDIR); \
	make; \
	cd $(PROJECTDIR)

run:
	# Check that Xephyr exists
	if [ -z "$(XEPHYR)" ]; then \
		echo "Xephyr not found!"; \
		exit 0; \
  	fi; \
  	export DEBUGGING=${DEBUG}; \
  	xinit $(PROJECTDIR)/xinitrc -- \
        "$(XEPHYR)" \
            :100 \
            -ac \
            -screen 1280x800 \
            -host-cursor

clean:
	rm -rf $(OUTDIR)