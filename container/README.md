# Container

Build the AOSP build container with

    docker build -t aosp .

Run the container with

    docker run --rm -it -v </path/to/aosp-root>:/src/aosp aosp

Replace `</path/to/aosp-root>` with your AOSP source code directory.

*Note*: On Ubuntu use `sudo` to get root privileges for the docker daemon.
