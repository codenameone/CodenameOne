#!/usr/bin/env python3
"""Compare CN1 screenshot outputs against stored references."""

from __future__ import annotations

import argparse
import base64
import json
import pathlib
import struct
import sys
import zlib
from dataclasses import dataclass
from typing import Dict, Iterable, List, Tuple

MAX_COMMENT_BASE64 = 40_000

PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"


class PNGError(Exception):
    """Raised when a PNG cannot be parsed."""


@dataclass
class PNGImage:
    width: int
    height: int
    bit_depth: int
    color_type: int
    pixels: bytes
    bytes_per_pixel: int


def _read_chunks(path: pathlib.Path) -> Iterable[Tuple[bytes, bytes]]:
    data = path.read_bytes()
    if not data.startswith(PNG_SIGNATURE):
        raise PNGError(f"{path} is not a PNG file (missing signature)")
    offset = len(PNG_SIGNATURE)
    length = len(data)
    while offset + 8 <= length:
        chunk_len = int.from_bytes(data[offset : offset + 4], "big")
        chunk_type = data[offset + 4 : offset + 8]
        offset += 8
        if offset + chunk_len + 4 > length:
            raise PNGError("PNG chunk truncated before CRC")
        chunk_data = data[offset : offset + chunk_len]
        offset += chunk_len + 4  # skip data + CRC
        yield chunk_type, chunk_data
        if chunk_type == b"IEND":
            break


def _bytes_per_pixel(bit_depth: int, color_type: int) -> int:
    if bit_depth != 8:
        raise PNGError(f"Unsupported bit depth: {bit_depth}")
    if color_type == 0:  # greyscale
        return 1
    if color_type == 2:  # RGB
        return 3
    if color_type == 4:  # greyscale + alpha
        return 2
    if color_type == 6:  # RGBA
        return 4
    raise PNGError(f"Unsupported color type: {color_type}")


def _paeth_predict(a: int, b: int, c: int) -> int:
    p = a + b - c
    pa = abs(p - a)
    pb = abs(p - b)
    pc = abs(p - c)
    if pa <= pb and pa <= pc:
        return a
    if pb <= pc:
        return b
    return c


def _unfilter(width: int, height: int, bpp: int, raw: bytes) -> bytes:
    stride = width * bpp
    expected = height * (stride + 1)
    if len(raw) != expected:
        raise PNGError("PNG IDAT payload has unexpected length")
    result = bytearray(height * stride)
    in_offset = 0
    out_offset = 0
    for row in range(height):
        filter_type = raw[in_offset]
        in_offset += 1
        row_data = bytearray(raw[in_offset : in_offset + stride])
        in_offset += stride
        if filter_type == 0:  # None
            pass
        elif filter_type == 1:  # Sub
            for i in range(stride):
                left = row_data[i - bpp] if i >= bpp else 0
                row_data[i] = (row_data[i] + left) & 0xFF
        elif filter_type == 2:  # Up
            for i in range(stride):
                up = result[out_offset - stride + i] if row > 0 else 0
                row_data[i] = (row_data[i] + up) & 0xFF
        elif filter_type == 3:  # Average
            for i in range(stride):
                left = row_data[i - bpp] if i >= bpp else 0
                up = result[out_offset - stride + i] if row > 0 else 0
                row_data[i] = (row_data[i] + ((left + up) // 2)) & 0xFF
        elif filter_type == 4:  # Paeth
            for i in range(stride):
                left = row_data[i - bpp] if i >= bpp else 0
                up = result[out_offset - stride + i] if row > 0 else 0
                up_left = result[out_offset - stride + i - bpp] if (row > 0 and i >= bpp) else 0
                row_data[i] = (row_data[i] + _paeth_predict(left, up, up_left)) & 0xFF
        else:
            raise PNGError(f"Unsupported PNG filter type: {filter_type}")
        result[out_offset : out_offset + stride] = row_data
        out_offset += stride
    return bytes(result)


def load_png(path: pathlib.Path) -> PNGImage:
    ihdr = None
    idat_chunks: List[bytes] = []
    for chunk_type, chunk_data in _read_chunks(path):
        if chunk_type == b"IHDR":
            if ihdr is not None:
                raise PNGError("Duplicate IHDR chunk")
            if len(chunk_data) != 13:
                raise PNGError("Invalid IHDR length")
            width = int.from_bytes(chunk_data[0:4], "big")
            height = int.from_bytes(chunk_data[4:8], "big")
            bit_depth = chunk_data[8]
            color_type = chunk_data[9]
            # compression (10), filter (11), interlace (12) must be default values
            if chunk_data[10] != 0 or chunk_data[11] != 0:
                raise PNGError("Unsupported PNG compression or filter method")
            if chunk_data[12] not in (0, 1):
                raise PNGError("Unsupported PNG interlace method")
            ihdr = (width, height, bit_depth, color_type, chunk_data[12])
        elif chunk_type == b"IDAT":
            idat_chunks.append(chunk_data)
        elif chunk_type == b"IEND":
            break
        else:
            # Ancillary chunks are ignored (metadata)
            continue

    if ihdr is None:
        raise PNGError("Missing IHDR chunk")
    if not idat_chunks:
        raise PNGError("Missing IDAT data")

    width, height, bit_depth, color_type, interlace = ihdr
    if interlace != 0:
        raise PNGError("Interlaced PNGs are not supported")

    bpp = _bytes_per_pixel(bit_depth, color_type)
    compressed = b"".join(idat_chunks)
    try:
        raw = zlib.decompress(compressed)
    except Exception as exc:  # pragma: no cover - defensive
        raise PNGError(f"Failed to decompress IDAT data: {exc}") from exc

    pixels = _unfilter(width, height, bpp, raw)
    return PNGImage(width, height, bit_depth, color_type, pixels, bpp)


def compare_images(expected: PNGImage, actual: PNGImage) -> Dict[str, bool]:
    equal = (
        expected.width == actual.width
        and expected.height == actual.height
        and expected.bit_depth == actual.bit_depth
        and expected.color_type == actual.color_type
        and expected.pixels == actual.pixels
    )
    return {
        "equal": equal,
        "width": actual.width,
        "height": actual.height,
        "bit_depth": actual.bit_depth,
        "color_type": actual.color_type,
    }


def _encode_png(width: int, height: int, bit_depth: int, color_type: int, bpp: int, pixels: bytes) -> bytes:
    import zlib as _zlib

    if len(pixels) != width * height * bpp:
        raise PNGError("Pixel buffer length does not match dimensions")

    def chunk(tag: bytes, payload: bytes) -> bytes:
        crc = _zlib.crc32(tag + payload) & 0xFFFFFFFF
        return (
            len(payload).to_bytes(4, "big")
            + tag
            + payload
            + crc.to_bytes(4, "big")
        )

    raw = bytearray()
    stride = width * bpp
    for row in range(height):
        raw.append(0)
        start = row * stride
        raw.extend(pixels[start : start + stride])

    ihdr = struct.pack(
        ">IIBBBBB",
        width,
        height,
        bit_depth,
        color_type,
        0,
        0,
        0,
    )

    compressed = _zlib.compress(bytes(raw))
    return b"".join(
        [PNG_SIGNATURE, chunk(b"IHDR", ihdr), chunk(b"IDAT", compressed), chunk(b"IEND", b"")]
    )


def _downscale_half(width: int, height: int, bpp: int, pixels: bytes) -> Tuple[int, int, bytes]:
    new_width = max(1, (width + 1) // 2)
    new_height = max(1, (height + 1) // 2)
    new_pixels = bytearray(new_width * new_height * bpp)

    for ny in range(new_height):
        for nx in range(new_width):
            accum = [0] * bpp
            samples = 0
            for dy in (0, 1):
                sy = min(height - 1, ny * 2 + dy)
                for dx in (0, 1):
                    sx = min(width - 1, nx * 2 + dx)
                    src_index = (sy * width + sx) * bpp
                    for channel in range(bpp):
                        accum[channel] += pixels[src_index + channel]
                    samples += 1
            dst_index = (ny * new_width + nx) * bpp
            for channel in range(bpp):
                new_pixels[dst_index + channel] = accum[channel] // samples

    return new_width, new_height, bytes(new_pixels)


def build_preview_base64(image: PNGImage, max_length: int = MAX_COMMENT_BASE64) -> str:
    width = image.width
    height = image.height
    bpp = image.bytes_per_pixel
    pixels = image.pixels

    while True:
        png_bytes = _encode_png(width, height, image.bit_depth, image.color_type, bpp, pixels)
        encoded = base64.b64encode(png_bytes).decode("ascii")
        if len(encoded) <= max_length or width <= 1 or height <= 1:
            return encoded
        if image.color_type not in {0, 2, 4, 6}:
            return encoded
        width, height, pixels = _downscale_half(width, height, bpp, pixels)


def build_results(reference_dir: pathlib.Path, actual_entries: List[Tuple[str, pathlib.Path]], emit_base64: bool) -> Dict[str, List[Dict[str, object]]]:
    results: List[Dict[str, object]] = []
    for test_name, actual_path in actual_entries:
        expected_path = reference_dir / f"{test_name}.png"
        record: Dict[str, object] = {
            "test": test_name,
            "actual_path": str(actual_path),
            "expected_path": str(expected_path),
        }
        if not actual_path.exists():
            record.update({"status": "missing_actual", "message": "Actual screenshot not found"})
        elif not expected_path.exists():
            record.update({"status": "missing_expected"})
            if emit_base64:
                try:
                    record["base64"] = build_preview_base64(load_png(actual_path))
                except Exception:
                    record["base64"] = base64.b64encode(actual_path.read_bytes()).decode("ascii")
        else:
            try:
                actual_img = load_png(actual_path)
                expected_img = load_png(expected_path)
                outcome = compare_images(expected_img, actual_img)
            except Exception as exc:
                record.update({"status": "error", "message": str(exc)})
            else:
                if outcome["equal"]:
                    record.update({"status": "equal"})
                else:
                    record.update({"status": "different", "details": outcome})
                    if emit_base64:
                        try:
                            record["base64"] = build_preview_base64(actual_img)
                        except Exception:
                            record["base64"] = base64.b64encode(actual_path.read_bytes()).decode("ascii")
        results.append(record)
    return {"results": results}


def parse_args(argv: List[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--reference-dir", required=True, type=pathlib.Path)
    parser.add_argument("--emit-base64", action="store_true", help="Include base64 payloads for updated screenshots")
    parser.add_argument("--actual", action="append", default=[], help="Mapping of test=path to evaluate")
    return parser.parse_args(argv)


def main(argv: List[str] | None = None) -> int:
    args = parse_args(argv)
    reference_dir: pathlib.Path = args.reference_dir
    actual_entries: List[Tuple[str, pathlib.Path]] = []
    for item in args.actual:
        if "=" not in item:
            print(f"Invalid --actual value: {item}", file=sys.stderr)
            return 2
        name, path_str = item.split("=", 1)
        actual_entries.append((name, pathlib.Path(path_str)))

    payload = build_results(reference_dir, actual_entries, bool(args.emit_base64))
    json.dump(payload, sys.stdout)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
