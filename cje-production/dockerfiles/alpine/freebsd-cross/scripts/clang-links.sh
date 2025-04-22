#!/usr/bin/env bash

sysroot=${1:-/freebsd}
triple=${2:-x86_64-unknown-freebsd13}

# Based on Rust's script
# https://github.com/rust-lang/rust/blob/master/src/ci/docker/scripts/freebsd-toolchain.sh
for tool in clang clang++; do
  tool_path=/usr/local/bin/${triple}-${tool}
  cat > "${tool_path}" <<EOF
#!/bin/sh
exec ${tool} --sysroot=${sysroot} "\$@" --target=${triple}
EOF
  chmod +x "${tool_path}"
done
