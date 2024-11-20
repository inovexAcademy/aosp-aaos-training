# Container

Build the AOSP build container with

    docker build -t aosp .

Run the container with

    docker run --rm -it -v </path/to/aosp-root>:/src/aosp aosp
