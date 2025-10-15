#!/usr/bin/env python3
"""Compare CN1 screenshot outputs against stored references."""

from __future__ import annotations

import argparse
import base64
import io
import json
import pathlib
import shutil
import struct
import subprocess
import sys
import tempfile
import zlib
from dataclasses import dataclass
from typing import Dict, Iterable, List, Optional, Tuple

try:
    from PIL import Image  # type: ignore
except Exception:  # pragma: no cover - optional dependency
    Image = None

MAX_COMMENT_BASE64 = 60_000
JPEG_QUALITY_CANDIDATES = (70, 60, 50, 40, 30, 20, 10)

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


@dataclass
class CommentPayload:
    base64: Optional[str]
    base64_length: int
    mime: str
    codec: str
    quality: Optional[int] = None
    omitted_reason: Optional[str] = None
    note: Optional[str] = None
    data: Optional[bytes] = None


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


def _prepare_pillow_image(image: PNGImage):
    if Image is None:
        raise RuntimeError("Pillow is not available")
    mode_map = {0: "L", 2: "RGB", 4: "LA", 6: "RGBA"}
    mode = mode_map.get(image.color_type)
    if mode is None:
        raise PNGError(f"Unsupported PNG color type for conversion: {image.color_type}")
    pil_img = Image.frombytes(mode, (image.width, image.height), image.pixels)
    if pil_img.mode == "LA":
        pil_img = pil_img.convert("RGBA")
    if pil_img.mode == "RGBA":
        background = Image.new("RGB", pil_img.size, (255, 255, 255))
        alpha = pil_img.split()[-1]
        background.paste(pil_img.convert("RGB"), mask=alpha)
        pil_img = background
    elif pil_img.mode != "RGB":
        pil_img = pil_img.convert("RGB")
    return pil_img


def _encode_comment_jpeg(image: PNGImage, quality: int) -> bytes:
    pil_img = _prepare_pillow_image(image)
    buffer = io.BytesIO()
    try:
        pil_img.save(buffer, format="JPEG", quality=quality, optimize=True)
    except OSError:
        buffer = io.BytesIO()
        pil_img.save(buffer, format="JPEG", quality=quality)
    return buffer.getvalue()


def _build_png_payload(image: PNGImage) -> bytes:
    return _encode_png(
        image.width,
        image.height,
        image.bit_depth,
        image.color_type,
        image.bytes_per_pixel,
        image.pixels,
    )


def build_comment_payload(image: PNGImage, max_length: int = MAX_COMMENT_BASE64) -> CommentPayload:
    note: Optional[str] = None
    if Image is not None:
        last_encoded: Optional[str] = None
        last_length: int = 0
        last_bytes: Optional[bytes] = None
        last_quality: Optional[int] = None
        for quality in JPEG_QUALITY_CANDIDATES:
            try:
                jpeg_bytes = _encode_comment_jpeg(image, quality)
            except Exception as exc:  # pragma: no cover - defensive
                note = f"JPEG encode failed at quality {quality}: {exc}"
                continue
            encoded = base64.b64encode(jpeg_bytes).decode("ascii")
            last_encoded = encoded
            last_length = len(encoded)
            last_bytes = jpeg_bytes
            last_quality = quality
            if len(encoded) <= max_length:
                return CommentPayload(
                    base64=encoded,
                    base64_length=len(encoded),
                    mime="image/jpeg",
                    codec="jpeg",
                    quality=quality,
                    omitted_reason=None,
                    note=note,
                    data=jpeg_bytes,
                )
        if last_bytes is not None:
            return CommentPayload(
                base64=None,
                base64_length=last_length,
                mime="image/jpeg",
                codec="jpeg",
                quality=last_quality,
                omitted_reason="too_large",
                note=note,
                data=last_bytes,
            )
        note = note or "JPEG conversion unavailable"
    else:
        # Attempt an external conversion using ImageMagick/GraphicsMagick if
        # Pillow isn't present on the runner. This keeps the previews JPEG-based
        # while avoiding large dependencies in the workflow environment.
        cli_payload = _build_comment_payload_via_cli(image, max_length)
        if cli_payload is not None:
            return cli_payload
        note = "Pillow library not available; falling back to PNG previews."

    png_bytes = _build_png_payload(image)
    encoded = base64.b64encode(png_bytes).decode("ascii")
    if len(encoded) <= max_length:
        return CommentPayload(
            base64=encoded,
            base64_length=len(encoded),
            mime="image/png",
            codec="png",
            quality=None,
            omitted_reason=None,
            note=note,
            data=png_bytes,
        )
    return CommentPayload(
        base64=None,
        base64_length=len(encoded),
        mime="image/png",
        codec="png",
        quality=None,
        omitted_reason="too_large",
        note=note,
        data=png_bytes,
    )


def _build_comment_payload_via_cli(
    image: PNGImage, max_length: int
) -> Optional[CommentPayload]:
    """Attempt to generate a JPEG preview using an external CLI."""

    converters = _detect_cli_converters()
    if not converters:
        return None

    png_bytes = _build_png_payload(image)

    with tempfile.TemporaryDirectory(prefix="cn1ss-cli-jpeg-") as tmp_dir:
        tmp_dir_path = pathlib.Path(tmp_dir)
        src = tmp_dir_path / "input.png"
        dst = tmp_dir_path / "preview.jpg"
        src.write_bytes(png_bytes)

        last_encoded: Optional[str] = None
        last_length = 0
        last_quality: Optional[int] = None
        last_data: Optional[bytes] = None
        last_error: Optional[str] = None

        for quality in JPEG_QUALITY_CANDIDATES:
            for converter in converters:
                try:
                    _run_cli_converter(converter, src, dst, quality)
                except RuntimeError as exc:
                    last_error = str(exc)
                    continue
                if not dst.exists():
                    last_error = "CLI converter did not create JPEG output"
                    continue
                data = dst.read_bytes()
                encoded = base64.b64encode(data).decode("ascii")
                last_encoded = encoded
                last_length = len(encoded)
                last_quality = quality
                last_data = data
                if len(encoded) <= max_length:
                    return CommentPayload(
                        base64=encoded,
                        base64_length=len(encoded),
                        mime="image/jpeg",
                        codec="jpeg",
                        quality=quality,
                        omitted_reason=None,
                        note=f"JPEG preview generated via {converter[0]}",
                        data=data,
                    )
                break  # try next quality once any converter succeeded

        if last_encoded is not None:
            note = ""
            if last_error:
                note = last_error
            return CommentPayload(
                base64=None,
                base64_length=last_length,
                mime="image/jpeg",
                codec="jpeg",
                quality=last_quality,
                omitted_reason="too_large",
                note=(note or f"JPEG preview generated via {converters[0][0]}")
                if converters
                else note,
                data=last_data,
            )

    return None


def _record_comment_payload(
    record: Dict[str, object],
    payload: CommentPayload,
    default_name: str,
    preview_dir: Optional[pathlib.Path],
) -> None:
    if payload.base64 is not None:
        record["base64"] = payload.base64
    else:
        record.update(
            {"base64_omitted": payload.omitted_reason, "base64_length": payload.base64_length}
        )
    record.update({
        "base64_mime": payload.mime,
        "base64_codec": payload.codec,
    })
    if payload.quality is not None:
        record["base64_quality"] = payload.quality
    if payload.note:
        record["base64_note"] = payload.note

    if preview_dir is None or payload.data is None:
        return

    preview_dir.mkdir(parents=True, exist_ok=True)
    suffix = ".jpg" if payload.mime == "image/jpeg" else ".png"
    base_name = _slugify(default_name.rsplit(".", 1)[0] or "preview")
    preview_path = preview_dir / f"{base_name}{suffix}"
    preview_path.write_bytes(payload.data)
    record["preview"] = {
        "path": str(preview_path),
        "name": preview_path.name,
        "mime": payload.mime,
        "codec": payload.codec,
        "quality": payload.quality,
        "note": payload.note,
    }


def _load_external_preview_payload(
    test_name: str, preview_dir: pathlib.Path
) -> Optional[CommentPayload]:
    slug = _slugify(test_name)
    candidates = (
        (preview_dir / f"{slug}.jpg", "image/jpeg", "jpeg"),
        (preview_dir / f"{slug}.jpeg", "image/jpeg", "jpeg"),
        (preview_dir / f"{slug}.png", "image/png", "png"),
    )
    for path, mime, codec in candidates:
        if not path.exists():
            continue
        data = path.read_bytes()
        encoded = base64.b64encode(data).decode("ascii")
        note = "Preview provided by instrumentation"
        if len(encoded) <= MAX_COMMENT_BASE64:
            return CommentPayload(
                base64=encoded,
                base64_length=len(encoded),
                mime=mime,
                codec=codec,
                quality=None,
                omitted_reason=None,
                note=note,
                data=data,
            )
        return CommentPayload(
            base64=None,
            base64_length=len(encoded),
            mime=mime,
            codec=codec,
            quality=None,
            omitted_reason="too_large",
            note=note,
            data=data,
        )
    return None


def _detect_cli_converters() -> List[Tuple[str, ...]]:
    """Return a list of available CLI converters (command tuples)."""

    candidates: List[Tuple[str, ...]] = []
    for cmd in (("magick", "convert"), ("convert",)):
        if shutil.which(cmd[0]):
            candidates.append(cmd)
    return candidates


def _run_cli_converter(
    command: Tuple[str, ...], src: pathlib.Path, dst: pathlib.Path, quality: int
) -> None:
    """Execute the CLI converter."""

    if not command:
        raise RuntimeError("No converter command provided")

    cmd = list(command)
    if len(cmd) == 2 and cmd[0] == "magick":
        # magick convert <in> -quality <q> <out>
        cmd.extend([str(src), "-quality", str(quality), str(dst)])
    else:
        # convert <in> -quality <q> <out>
        cmd.extend([str(src), "-quality", str(quality), str(dst)])

    result = subprocess.run(
        cmd,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        check=False,
        text=True,
    )
    if result.returncode != 0:
        raise RuntimeError(
            f"{' '.join(command)} exited with {result.returncode}: {result.stderr.strip()}"
        )


def _slugify(name: str) -> str:
    return "".join(ch if ch.isalnum() else "_" for ch in name)


def build_results(
    reference_dir: pathlib.Path,
    actual_entries: List[Tuple[str, pathlib.Path]],
    emit_base64: bool,
    preview_dir: Optional[pathlib.Path] = None,
) -> Dict[str, List[Dict[str, object]]]:
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
                payload = None
                if preview_dir is not None:
                    payload = _load_external_preview_payload(test_name, preview_dir)
                if payload is None:
                    payload = build_comment_payload(load_png(actual_path))
                _record_comment_payload(record, payload, actual_path.name, preview_dir)
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
                        payload = None
                        if preview_dir is not None:
                            payload = _load_external_preview_payload(test_name, preview_dir)
                        if payload is None:
                            payload = build_comment_payload(actual_img)
                        _record_comment_payload(record, payload, actual_path.name, preview_dir)
        results.append(record)
    return {"results": results}


def parse_args(argv: List[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--reference-dir", required=True, type=pathlib.Path)
    parser.add_argument("--emit-base64", action="store_true", help="Include base64 payloads for updated screenshots")
    parser.add_argument("--preview-dir", type=pathlib.Path, help="Directory to store generated preview images")
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

    preview_dir = args.preview_dir
    payload = build_results(reference_dir, actual_entries, bool(args.emit_base64), preview_dir)
    json.dump(payload, sys.stdout)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
