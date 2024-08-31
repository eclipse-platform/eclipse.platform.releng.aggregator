# ***freebsd-cross*** Docker image

***This Docker container is taken from [docker-freebsd-cross](https://github.com/chirontt/docker-freebsd-cross), with the removal of `meson` support.***

An Alpine-based Docker image for cross-compiling to any version of FreeBSD (amd64/arm64) using `clang`.

- Supports building images for `aarch64` & `x86_64` platforms
- C/C++ cross-compilers are available via the `CLANG` and `CPPLANG` env variables
- Allows FreeBSD's `pkg` dependency installation
- Configures `pkgconf` (`pkg-config`)
- GTK3 & GTK4 libraries for FreeBSD are installed in the image
- OpenJDK 21 for FreeBSD is also installed in the image, and is available via the `FREEBSD_JAVA_HOME` env variable.

# Usage

## FreeBSD cross builds for default `x86_64` architecture

### Build Docker image for `x86_64` on Linux/x86_64

First, build the image for default `x86_64` architecture, on a Linux/x86_64 box:

```
> docker build -t "freebsd-cross-x86_64" .
```

### FreeBSD/x86_64 cross build for Eclipse SWT natives

Then, at the root of [eclipse.platform.swt](https://github.com/eclipse-platform/eclipse.platform.swt) 's working directory,
execute the following commands:

```
> cd bundles/org.eclipse.swt
> java -Dws=gtk -Darch=x86_64 build-scripts/CollectSources.java -nativeSources \
    ../../binaries/org.eclipse.swt.gtk.freebsd.x86_64/target/natives-build-temp
> cd ../../binaries/org.eclipse.swt.gtk.freebsd.x86_64
> docker run --rm --volume $(pwd):/workdir -it "freebsd-cross-x86_64" /bin/sh -c \
    'cd target/natives-build-temp && \
    export OS=FreeBSD MODEL=x86_64 SWT_JAVA_HOME=$FREEBSD_JAVA_HOME OUTPUT_DIR=../.. CC=$CLANG && \
    ./build.sh install'
```

### FreeBSD/x86_64 cross build for Eclipse Equinox's launcher natives

At the root of [equinox](https://github.com/eclipse-equinox/equinox) 's working directory,
execute the following command:

```
> docker run --rm --volume $(pwd)/features/org.eclipse.equinox.executable.feature/library:/workdir \
    --volume $(pwd)/../equinox.binaries:/output -it "freebsd-cross-x86_64" /bin/sh -c \
    'cd gtk && \
    export JAVA_HOME=$FREEBSD_JAVA_HOME BINARIES_DIR=/output CC=$CLANG && \
    ./build.sh install -ws gtk -os freebsd -arch x86_64 -java $FREEBSD_JAVA_HOME'
```

### FreeBSD/x86_64 cross build for Eclipse Plaform's file system natives

At the root of [eclipse.platform](https://github.com/eclipse-platform/eclipse.platform) 's working directory,
execute the following command:

```
> docker run --rm --volume $(pwd):/workdir -it "freebsd-cross-x86_64" /bin/sh -c \
    'cd resources/bundles/org.eclipse.core.filesystem/natives/unix/freebsd && \
    export OS_ARCH=x86_64 JAVA_HOME=$FREEBSD_JAVA_HOME OUTPUT_DIR=../.. CC=$CLANG && \
    make install'
```

## FreeBSD cross builds for `aarch64` architecture

### Build Docker image for `aarch64` on Linux/aarch64

First, build the image for `aarch64` architecture, on a Linux/aarch64 box:

```
> docker build -t "freebsd-cross-aarch64" \
    --build-arg FREEBSD_TARGET=arm64 \
    --build-arg FREEBSD_TARGET_ARCH=aarch64 \
    --build-arg CLANG_TARGET_ARCH=aarch64 .
```

### FreeBSD/aarch64 cross build for Eclipse SWT natives

Then, at the root of [eclipse.platform.swt](https://github.com/eclipse-platform/eclipse.platform.swt) 's working directory,
execute the following commands:

```
> cd bundles/org.eclipse.swt
> java -Dws=gtk -Darch=aarch64 build-scripts/CollectSources.java -nativeSources \
    ../../binaries/org.eclipse.swt.gtk.freebsd.aarch64/target/natives-build-temp
> cd ../../binaries/org.eclipse.swt.gtk.freebsd.aarch64
> docker run --rm --volume $(pwd):/workdir -it "freebsd-cross-aarch64" /bin/sh -c \
    'cd target/natives-build-temp && \
    export OS=FreeBSD MODEL=aarch64 SWT_JAVA_HOME=$FREEBSD_JAVA_HOME OUTPUT_DIR=../.. CC=$CLANG && \
    ./build.sh install'
```

### FreeBSD/aarch64 cross build for Eclipse Equinox's launcher natives

At the root of [equinox](https://github.com/eclipse-equinox/equinox) 's working directory,
execute the following command:

```
> docker run --rm --volume $(pwd)/features/org.eclipse.equinox.executable.feature/library:/workdir \
    --volume $(pwd)/../equinox.binaries:/output -it "freebsd-cross-aarch64" /bin/sh -c \
    'cd gtk && \
    export JAVA_HOME=$FREEBSD_JAVA_HOME BINARIES_DIR=/output CC=$CLANG && \
    ./build.sh install -ws gtk -os freebsd -arch aarch64 -java $FREEBSD_JAVA_HOME'
```

### FreeBSD/aarch64 cross build for Eclipse Plaform's file system natives

At the root of [eclipse.platform](https://github.com/eclipse-platform/eclipse.platform) 's working directory,
execute the following command:

```
> docker run --rm --volume $(pwd):/workdir -it "freebsd-cross-aarch64" /bin/sh -c \
    'cd resources/bundles/org.eclipse.core.filesystem/natives/unix/freebsd && \
    export OS_ARCH=aarch64 JAVA_HOME=$FREEBSD_JAVA_HOME OUTPUT_DIR=../.. CC=$CLANG && \
    make install'
```

