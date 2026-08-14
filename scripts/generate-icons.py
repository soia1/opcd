#!/usr/bin/env python3
"""Generate minimal OPCD Android launcher icons without external image libraries."""

import os
import struct
import zlib

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MIPMAP_DIR = os.path.join(BASE_DIR, "android", "app", "src", "main", "res")

SIZES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

BACKGROUND = (0x26, 0x26, 0x26)      # dark grey
ACCENT = (0x03, 0xDA, 0xC5)          # teal


def chunk(type_name, data):
    type_bytes = type_name.encode("ascii")
    return (
        struct.pack(">I", len(data))
        + type_bytes
        + data
        + struct.pack(">I", zlib.crc32(type_bytes + data) & 0xFFFFFFFF)
    )


def make_png(size, rounded=False):
    """Create a minimal RGBA PNG of the given size."""
    raw = bytearray()
    radius = (size * 0.4) if rounded else None
    center = size / 2.0

    for y in range(size):
        raw.append(0)  # filter byte: None
        for x in range(size):
            if rounded:
                dist = ((x + 0.5 - center) ** 2 + (y + 0.5 - center) ** 2) ** 0.5
                if dist > radius:
                    raw.extend([0, 0, 0, 0])
                    continue
            # Simple circle/dot accent in the center.
            dot_dist = ((x + 0.5 - center) ** 2 + (y + 0.5 - center) ** 2) ** 0.5
            if dot_dist < size * 0.18:
                raw.extend(ACCENT + (0xFF,))
            else:
                raw.extend(BACKGROUND + (0xFF,))

    compressed = zlib.compress(bytes(raw), 9)

    header = struct.pack(">IIBBBBB",
        size, size,
        8,   # bit depth
        6,   # color type: RGBA
        0, 0, 0
    )

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk("IHDR", header)
    png += chunk("IDAT", compressed)
    png += chunk("IEND", b"")
    return png


def main():
    for folder, size in SIZES.items():
        path = os.path.join(MIPMAP_DIR, folder)
        os.makedirs(path, exist_ok=True)
        for name, rounded in [("ic_launcher.png", False), ("ic_launcher_round.png", True)]:
            with open(os.path.join(path, name), "wb") as f:
                f.write(make_png(size, rounded=rounded))
            print(f"Generated {folder}/{name} ({size}x{size})")


if __name__ == "__main__":
    main()
