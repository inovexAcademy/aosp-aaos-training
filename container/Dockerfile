FROM ubuntu:24.04

ENV DEBIAN_FRONTEND noninteractive

RUN apt-get update && apt-get install -qy --no-install-recommends \
     # Packages from https://source.android.com/setup/build/initializing \
     repo \
     git-core gnupg flex bison build-essential zip curl zlib1g-dev \
     libc6-dev-i386 x11proto-core-dev libx11-dev lib32z1-dev libgl1-mesa-dev \
     libxml2-utils xsltproc unzip fontconfig \
     # Fix curl download error: "curl: (77) error setting certificate verify locations:" \
     openssl ca-certificates \
     # Fix: Can't locate XML/Simple.pm in @INC \
     libxml-simple-perl \
     # '/bin/sh: 1: bc: not found' and openssl for kernel after chaning to 20.04 \
     bc libssl-dev \
     python3 \
     # utils \
     # ninja: to execute ninja by hand \
     xxd less ninja-build rsync neovim \
     # Kernel build \
     cpio lz4 \
     # Cuttlefish
     git devscripts config-package-dev debhelper-compat golang curl wget \
     devscripts equivs \
	clang pkg-config libfmt-dev libgflags-dev libjsoncpp-dev \
	libcurl4-openssl-dev libgoogle-glog-dev libgtest-dev \
	libxml2-dev uuid-dev libprotobuf-dev libprotobuf-c-dev protobuf-compiler libz3-dev \
  && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

# Install bazel
RUN apt install apt-transport-https curl gnupg -qy \
	&& curl -fsSL https://bazel.build/bazel-release.pub.gpg | gpg --dearmor >bazel-archive-keyring.gpg \
	&& mv bazel-archive-keyring.gpg /usr/share/keyrings \
	&& echo "deb [arch=amd64 signed-by=/usr/share/keyrings/bazel-archive-keyring.gpg] https://storage.googleapis.com/bazel-apt stable jdk1.8" | tee /etc/apt/sources.list.d/bazel.list \
	&& apt update && apt install bazel

# Install cuttlefish environment
RUN mkdir /tools && cd /tools && git clone https://github.com/google/android-cuttlefish && cd android-cuttlefish \
    && cd base && debuild -i -us -uc -b -d && cd .. \
    && cd frontend && debuild -i -us -uc -b -d && cd .. \
    && (dpkg -i ./cuttlefish-base_*_*64.deb || apt-get install -fqy) \
    && (dpkg -i ./cuttlefish-user_*_*64.deb || apt-get install -fqy)

WORKDIR  /src/aosp
