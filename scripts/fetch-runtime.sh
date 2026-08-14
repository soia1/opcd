#!/bin/sh
# Fetch all native runtime libraries for OPCD Android and place them in
# android/app/src/main/jniLibs/arm64-v8a/ so that the Android package manager
# extracts them into nativeLibraryDir at install time. That is the only
# app-owned storage from which SELinux allows execve() on Android 10+
# (the app_data_file neverallow on execute_no_trans for targetSdk>=29).
#
# Bundled binaries (all aarch64):
#   libproot.so           GPL-2.0   dynamic proot (Termux fork 5.1.107.90)
#   libproot-loader.so    GPL-2.0   static proot loader (pre-staged, avoids
#                                   the kernel rejecting a freshly-written
#                                   loader file in app_data_file)
#   libtalloc.so          LGPL-2.1  runtime dep of proot (renamed+SONAME-rewritten
#                                   from libtalloc.so.2 so AGP packages it)
#   libandroid-shmem.so   Apache-2.0 runtime dep of proot
#
# After fetching, libproot.so's RUNPATH is rewritten from the Termux default
# to $ORIGIN so its two deps resolve from the same directory.
#
# Note: libopcd-exec.so is NOT fetched here. It is built from C source at
# build time via gradle's externalNativeBuild (see app/src/main/cpp/).
#
# Usage:
#   ./scripts/fetch-runtime.sh
set -e

MIRROR="https://packages.termux.dev/apt/termux-main/pool/main"

PROOT_DEB="$MIRROR/p/proot/proot_5.1.107.90_aarch64.deb"
TALLOC_DEB="$MIRROR/libt/libtalloc/libtalloc_2.4.3_aarch64.deb"
SHMEM_DEB="$MIRROR/liba/libandroid-shmem/libandroid-shmem_0.7_aarch64.deb"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
DEST_DIR="$SCRIPT_DIR/../android/app/src/main/jniLibs/arm64-v8a"
mkdir -p "$DEST_DIR"

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

fetch() {
    # fetch <url> <inner_path_in_deb> <dest_file>
    url="$1"; inner="$2"; out="$3"
    base="$(basename "$url" | sed 's/%3A/:/')"
    deb="$TMP/$base"
    echo "fetch  $url"
    if command -v curl >/dev/null 2>&1; then
        curl -fsSL "$url" -o "$deb"
    else
        wget -q "$url" -O "$deb"
    fi
    work="$TMP/extract"
    rm -rf "$work"; mkdir -p "$work"
    (cd "$work" && ar x "$deb")
    if [ -f "$work/data.tar.xz" ]; then
        tar -xJf "$work/data.tar.xz" -C "$work"
    elif [ -f "$work/data.tar.gz" ]; then
        tar -xzf "$work/data.tar.gz" -C "$work"
    elif [ -f "$work/data.tar.zst" ]; then
        tar --use-compress-program=unzstd -xf "$work/data.tar.zst" -C "$work"
    else
        echo "ERR: unknown data tarball in $deb" >&2
        ls -la "$work" >&2
        exit 1
    fi
    cp "$work/$inner" "$out"
    chmod +x "$out"
}

# proot (provides both the main binary and the loader)
fetch "$PROOT_DEB" "data/data/com.termux/files/usr/bin/proot"          "$DEST_DIR/libproot.so"
fetch "$PROOT_DEB" "data/data/com.termux/files/usr/libexec/proot/loader" "$DEST_DIR/libproot-loader.so"

# proot runtime deps
# NOTE: libtalloc is fetched as libtalloc.so.2.4.3 but renamed to libtalloc.so
# and re-SONAME'd. Reason: AGP's native-lib packaging only includes files
# matching *.so from jniLibs/ -- libtalloc.so.2 ends in .2, not .so, so AGP
# silently skips it and it never lands in nativeLibraryDir. Renaming it to
# libtalloc.so, rewriting its SONAME, and rewriting libproot.so's NEEDED to
# match makes both AGP and the Android dynamic linker happy.
fetch "$TALLOC_DEB" "data/data/com.termux/files/usr/lib/libtalloc.so.2.4.3" "$DEST_DIR/libtalloc.so.x"
fetch "$SHMEM_DEB"  "data/data/com.termux/files/usr/lib/libandroid-shmem.so" "$DEST_DIR/libandroid-shmem.so"
mv "$DEST_DIR/libtalloc.so.x" "$DEST_DIR/libtalloc.so"

# Rewrite libproot.so's RUNPATH so it looks for its NEEDED libs in the same
# directory as itself ($ORIGIN) instead of /data/data/com.termux/files/usr/lib.
# Also rewrite NEEDED libtalloc.so.2 -> libtalloc.so to match the renamed lib.
if command -v patchelf >/dev/null 2>&1; then
    patchelf --set-rpath '$ORIGIN' "$DEST_DIR/libproot.so"
    patchelf --replace-needed libtalloc.so.2 libtalloc.so "$DEST_DIR/libproot.so"
    patchelf --set-soname  libtalloc.so "$DEST_DIR/libtalloc.so"
else
    echo "WARN: patchelf not installed; libproot.so deps will not resolve at runtime." >&2
fi

echo
echo "Installed runtime libs in $DEST_DIR:"
ls -l "$DEST_DIR"