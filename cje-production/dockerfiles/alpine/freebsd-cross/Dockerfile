# syntax=docker/dockerfile:1

ARG BASE_IMAGE_NAME="alpine"
ARG BASE_IMAGE_TAG="3.19"

ARG FREEBSD_VERSION_MAJOR="14"
ARG FREEBSD_VERSION_MINOR="2"
ARG FREEBSD_TARGET="amd64"
ARG FREEBSD_TARGET_ARCH="amd64"
ARG FREEBSD_VERSION="${FREEBSD_VERSION_MAJOR}.${FREEBSD_VERSION_MINOR}-RELEASE"
ARG FREEBSD_SYSROOT="/freebsd"
ARG FREEBSD_PKG_VERSION="1.21.3"
ARG FREEBSD_PKG_ABI="FreeBSD:${FREEBSD_VERSION_MAJOR}:${FREEBSD_TARGET_ARCH}"
ARG FREEBSD_PKG_JAVA="openjdk21"
ARG CLANG_TARGET_ARCH="x86_64"
ARG CLANG_LINKS_TARGET="${CLANG_TARGET_ARCH}-unknown-freebsd${FREEBSD_VERSION_MAJOR}"

FROM ${BASE_IMAGE_NAME}:${BASE_IMAGE_TAG}

# Export the build arguments
ARG FREEBSD_VERSION_MAJOR
ARG FREEBSD_VERSION_MINOR
ARG FREEBSD_TARGET
ARG FREEBSD_TARGET_ARCH
ARG FREEBSD_VERSION
ARG FREEBSD_SYSROOT
ARG FREEBSD_PKG_VERSION
ARG FREEBSD_PKG_ABI
ARG FREEBSD_PKG_JAVA
ARG CLANG_TARGET_ARCH
ARG CLANG_LINKS_TARGET

### HOST DEPENDENCIES ###

# Install various build tools and dependencies
RUN apk add --quiet --no-cache --no-progress \
      bash \
      curl \
      clang \
      gcc \
      git \
      pkgconf \
      make \
      autoconf \
      automake \
      libtool \
      musl-dev \
      xz-dev \
      bzip2-dev \
      zlib-dev \
      zstd-dev \
      lz4-dev \
      expat-dev \
      acl-dev \
      fts-dev \
      libbsd-dev \
      openssl-dev \
      libarchive-dev \
      libarchive-tools

### FreeBSD's PKG INSTALLATION on HOST ###

# Download, build and install the FreeBSD `pkg` tool
RUN mkdir /pkg && \
    curl -L https://github.com/freebsd/pkg/archive/refs/tags/${FREEBSD_PKG_VERSION}.tar.gz | \
      bsdtar -xf - -C /pkg

RUN cd /pkg/pkg-* && \
    ln -sf clang /usr/bin/cc && cc --version && \
    export CFLAGS="-Wno-cpp -Wno-switch -D__BEGIN_DECLS='' -D__END_DECLS='' -DDEFFILEMODE='S_IRUSR|S_IWUSR|S_IRGRP|S_IWGRP|S_IROTH|S_IWOTH' -D__GLIBC__" && \
    export LDFLAGS="-lfts" && \
    ./configure --quiet --with-libarchive.pc && \
    touch /usr/include/sys/unistd.h && \
    touch /usr/include/sys/sysctl.h && \
    sed -i'' -e '/#include "pkg.h"/i#include <bsd/stdlib.h>' libpkg/pkg_jobs_conflicts.c && \
    sed -i'' -e '/#include.*cdefs.h>/i#include <fcntl.h>' libpkg/flags.c && \
    sed -i'' -e '/#include <stdio.h>/i#include <stdarg.h>' libpkg/xmalloc.h && \
    make && mkdir -p /usr/local/etc && make install && \
    cd / && rm -rf /pkg /usr/local/sbin/pkg2ng && \
    unset CFLAGS LDFLAGS

### FREEBSD BASE INSTALLATION ###

# Download the FreeBSD base and install the libraries, include files and package keys etc.
RUN mkdir ${FREEBSD_SYSROOT} && \
    curl -L https://download.freebsd.org/ftp/releases/${FREEBSD_TARGET}/${FREEBSD_TARGET_ARCH}/${FREEBSD_VERSION}/base.txz | \
	bsdtar -xf - -C ${FREEBSD_SYSROOT} ./lib ./usr/lib ./usr/libdata ./usr/include ./usr/share/keys ./etc

### FreeBSD's PKG CONFIGURATION ###

# Configure and update `pkg`
# (usage: pkg install ...)
ENV PKG_ROOTDIR ${FREEBSD_SYSROOT}
RUN mkdir -p ${FREEBSD_SYSROOT}/usr/local/etc && \
	  echo "ABI = \"${FREEBSD_PKG_ABI}\"; REPOS_DIR = [\"${FREEBSD_SYSROOT}/etc/pkg\"]; REPO_AUTOUPDATE = NO; RUN_SCRIPTS = NO;" > ${FREEBSD_SYSROOT}/usr/local/etc/pkg.conf
RUN ln -s ${FREEBSD_SYSROOT}/usr/share/keys /usr/share/keys
RUN mv /usr/local/sbin/pkg /usr/local/sbin/pkg.real && \
    echo "#!/bin/sh" > /usr/local/sbin/pkg && \
    echo "exec pkg.real -r ${PKG_ROOTDIR} \"\$@\"" >> /usr/local/sbin/pkg && \
    chmod +x /usr/local/sbin/pkg
RUN pkg update -f

### CLANG on HOST to cross-compile to FreeBSD target ###

# Make clang symlinks to cross-compile
# NOTE: clang++ should be able to find stdc++ (necessary for meson checks even without building any c++ code)
ADD scripts/clang-links.sh /tmp/clang-links.sh
RUN bash /tmp/clang-links.sh ${FREEBSD_SYSROOT} ${CLANG_LINKS_TARGET}
ENV CLANG=${CLANG_LINKS_TARGET}-clang
ENV CPPLANG=${CLANG_LINKS_TARGET}-clang++

### PKG-CONFIG ###

# Configure `pkg-config`
ENV PKG_CONFIG_LIBDIR ${FREEBSD_SYSROOT}/usr/libdata/pkgconfig:${FREEBSD_SYSROOT}/usr/local/libdata/pkgconfig
ENV PKG_CONFIG_SYSROOT_DIR ${FREEBSD_SYSROOT}

### FreeBSD's JDK INSTALLATION ###

RUN pkg install -y ${FREEBSD_PKG_JAVA}
ENV FREEBSD_JAVA_HOME=${FREEBSD_SYSROOT}/usr/local/${FREEBSD_PKG_JAVA}

### FreeBSD's GTK3 & GTK4 LIBRARIES' INSTALLATION ###

# Install 'cairo' & 'GLU' dependencies
RUN pkg install -y cairo libGLU

# Install the GTK3 & GTK4 packages
RUN pkg install -y gtk3
RUN pkg install -y gtk4

# Install 'webkit2' dependencies
RUN pkg install -y webkit2-gtk3 webkit2-gtk4


WORKDIR /workdir

