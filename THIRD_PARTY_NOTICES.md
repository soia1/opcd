# Third-Party Notices

OPCD Android bundles prebuilt native libraries from third-party projects
inside the APK (`android/app/src/main/jniLibs/arm64-v8a/`). They are loaded
and executed at runtime as separate processes; they are **not** linked into
OPCD Android's Java/Kotlin code. Each library retains its own license.

## Bundled third-party libraries

| Library | Upstream | License | Source |
|---|---|---|---|
| `libproot.so`, `libproot-loader.so` | [proot-me/proot](https://github.com/proot-me/proot) (Termux fork) | **GPL-2.0-only** | https://packages.termux.dev/apt/termux-main/pool/main/p/proot/proot_5.1.107.90_aarch64.deb |
| `libtalloc.so` | [Samba talloc](https://gitlab.com/samba-team/samba) | **LGPL-2.1-or-later** | https://packages.termux.dev/apt/termux-main/pool/main/libt/libtalloc/libtalloc_2.4.3_aarch64.deb |
| `libandroid-shmem.so` | termux | **Apache-2.0** | https://packages.termux.dev/apt/termux-main/pool/main/liba/libandroid-shmem/libandroid-shmem_0.7_aarch64.deb |

## OPCD's own native library

| Library | License | Source |
|---|---|---|
| `libopcd-exec.so` | **MIT** (same as OPCD Android) | `android/app/src/main/cpp/opcd_exec_shim.c` |

Built from C source by gradle's `externalNativeBuild` (Android NDK,
clang). Source is in this repository under `app/src/main/cpp/`; no
binary blobs are committed (the NDK build is reproducible).

## License compatibility notes

OPCD Android's own code is **MIT-licensed** (`LICENSE`). The bundled
libraries are aggregated with it as standalone binaries invoked via
`Runtime.exec()`; they are not statically or dynamically linked into OPCD
Android's code. This is **mere aggregation** under:

- **GPL-2.0 §3** ("The mere aggregation of another work not based on the
  Program with the Program ... on a volume of a storage or distribution
  medium does not bring the other work under the scope of this License").
- **LGPL-2.1 §5** (mere aggregation exception).

Therefore:

- GPL-2.0 `proot` does not extend its copyleft to OPCD Android's MIT code.
- LGPL-2.1 `talloc` is used as a dynamic dependency of `proot` only and
  satisfies LGPL's requirement that the user be able to relink with a
  modified talloc (`jniLibs/arm64-v8a/libtalloc.so` is extractable from
  the APK; `patchelf --replace-needed` could substitute it).
- Apache-2.0 `libandroid-shmem` is MIT-compatible.

## Source availability

All bundled libraries are open-source. To obtain matching source code:

```
# proot + loader
https://github.com/termux/proot  (Termux-maintained fork)
https://github.com/proot-me/proot  (upstream)

# talloc (Samba)
https://gitlab.com/samba-team/samba
```

## Notices required by the licenses

- **GPL-2.0**: This file constitutes the accompanying "written offer" for
  source code of the GPL-licensed portions, valid for at least three years
  from distribution. Sources are also permanently available at the URLs
  above.
- **Apache-2.0**: Each `Apache-2.0` file in the bundled libraries retains
  its own `NOTICE` file inside the upstream repository.