# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""magika: content-type detection with Magika's result shape.

Google's Magika runs a neural model through onnxruntime, which no
Codename One target ships. This answers the same questions from file
signatures and a few text heuristics instead: container formats are told
apart by what is inside them (the parts of a ZIP, the streams of an OLE
file), and text by its first bytes. A content it cannot place is reported
as "unknown", which callers already treat as "no opinion"."""

import io as _io
import json as _json
import os as _os

__version__ = "0.6.0"

__all__ = ["Magika", "MagikaResult", "MagikaPrediction", "ContentTypeInfo", "ContentTypeLabel",
           "PredictionMode", "Status"]


class Status:
    OK = "ok"
    FILE_NOT_FOUND_ERROR = "file_not_found_error"
    PERMISSION_ERROR = "permission_error"
    UNKNOWN = "unknown"


class PredictionMode:
    BEST_GUESS = "best_guess"
    MEDIUM_CONFIDENCE = "medium_confidence"
    HIGH_CONFIDENCE = "high_confidence"


class ContentTypeInfo:
    def __init__(self, label, mime_type, group, description, extensions, is_text):
        self.label = label
        self.mime_type = mime_type
        self.group = group
        self.description = description
        self.extensions = extensions
        self.is_text = is_text

    @property
    def ct_label(self):
        return self.label

    def __repr__(self):
        return "ContentTypeInfo(label=%r, mime_type=%r, group=%r, description=%r, extensions=%r, is_text=%r)" % (
            self.label, self.mime_type, self.group, self.description, self.extensions, self.is_text)


_TYPES = {}


def _t(label, mime, group, description, exts, is_text):
    _TYPES[label] = ContentTypeInfo(label, mime, group, description, exts, is_text)


_t("unknown", "application/octet-stream", "unknown", "Unknown binary data", [], False)
_t("empty", "inode/x-empty", "inode", "Empty file", [], False)
_t("txt", "text/plain", "text", "Generic text document", ["txt"], True)
_t("pdf", "application/pdf", "document", "PDF document", ["pdf"], False)
_t("zip", "application/zip", "archive", "ZIP archive data", ["zip"], False)
_t("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "document",
   "Microsoft Word 2007+ document", ["docx", "docm"], False)
_t("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "document",
   "Microsoft Excel 2007+ spreadsheet", ["xlsx", "xlsm"], False)
_t("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation", "document",
   "Microsoft PowerPoint 2007+ document", ["pptx", "pptm"], False)
_t("epub", "application/epub+zip", "document", "EPUB document", ["epub"], False)
_t("jar", "application/java-archive", "archive", "Java archive data (JAR)", ["jar"], False)
_t("apk", "application/vnd.android.package-archive", "executable", "Android package", ["apk"], False)
_t("odt", "application/vnd.oasis.opendocument.text", "document", "OpenDocument Text", ["odt"], False)
_t("ods", "application/vnd.oasis.opendocument.spreadsheet", "document", "OpenDocument Spreadsheet", ["ods"], False)
_t("odp", "application/vnd.oasis.opendocument.presentation", "document", "OpenDocument Presentation", ["odp"],
   False)
_t("doc", "application/msword", "document", "Microsoft Word CDF document", ["doc"], False)
_t("xls", "application/vnd.ms-excel", "document", "Microsoft Excel CDF document", ["xls"], False)
_t("ppt", "application/vnd.ms-powerpoint", "document", "Microsoft PowerPoint CDF document", ["ppt"], False)
_t("outlook", "application/vnd.ms-outlook", "application", "MS Outlook Message", [], False)
_t("png", "image/png", "image", "PNG image data", ["png"], False)
_t("jpeg", "image/jpeg", "image", "JPEG image data", ["jpg", "jpeg"], False)
_t("gif", "image/gif", "image", "GIF image data", ["gif"], False)
_t("webp", "image/webp", "image", "WebP media file", ["webp"], False)
_t("bmp", "image/bmp", "image", "BMP image data", ["bmp"], False)
_t("tiff", "image/tiff", "image", "TIFF image data", ["tiff", "tif"], False)
_t("heif", "image/heif", "image", "HEIF image data", ["heif", "heic"], False)
_t("svg", "image/svg+xml", "image", "SVG Scalable Vector Graphics image data", ["svg"], True)
_t("wav", "audio/x-wav", "audio", "Waveform Audio file (WAV)", ["wav"], False)
_t("mp3", "audio/mpeg", "audio", "MP3 media file", ["mp3"], False)
_t("mp4", "video/mp4", "video", "MP4 media file", ["mp4"], False)
_t("ogg", "audio/ogg", "audio", "Ogg data", ["ogg"], False)
_t("flac", "audio/flac", "audio", "FLAC audio bitstream data", ["flac"], False)
_t("gzip", "application/gzip", "archive", "gzip compressed data", ["gz", "gzip"], False)
_t("html", "text/html", "code", "HTML document", ["html", "htm", "xhtml", "xht"], True)
_t("xml", "text/xml", "code", "XML document", ["xml"], True)
_t("rss", "application/rss+xml", "text", "RSS feed", ["rss"], True)
_t("json", "application/json", "code", "JSON document", ["json"], True)
_t("jsonl", "application/json", "code", "JSONL document", ["jsonl"], True)
_t("ipynb", "application/json", "code", "Jupyter notebook", ["ipynb"], True)
_t("csv", "text/csv", "code", "CSV document", ["csv"], True)
_t("markdown", "text/markdown", "text", "Markdown document", ["md", "markdown"], True)
_t("python", "text/x-python", "code", "Python source", ["py", "pyi"], True)
_t("rtf", "text/rtf", "text", "Rich Text Format document", ["rtf"], True)


class ContentTypeLabel:
    UNKNOWN = "unknown"
    EMPTY = "empty"
    TXT = "txt"
    PDF = "pdf"
    ZIP = "zip"
    DOCX = "docx"
    XLSX = "xlsx"
    PPTX = "pptx"
    EPUB = "epub"
    HTML = "html"
    XML = "xml"
    JSON = "json"
    CSV = "csv"
    MARKDOWN = "markdown"
    PNG = "png"
    JPEG = "jpeg"


class MagikaPrediction:
    def __init__(self, dl, output, score, overwrite_reason="none"):
        self.dl = dl
        self.output = output
        self.score = score
        self.overwrite_reason = overwrite_reason


class MagikaResult:
    def __init__(self, path, status, prediction=None):
        self.path = path
        self.status = status
        self._prediction = prediction

    @property
    def ok(self):
        return self.status == Status.OK

    @property
    def prediction(self):
        if self._prediction is None:
            raise ValueError("prediction is not available: status is %s" % self.status)
        return self._prediction

    @property
    def dl(self):
        return self.prediction.dl

    @property
    def output(self):
        return self.prediction.output

    @property
    def score(self):
        return self.prediction.score


_SAMPLE = 64 * 1024


def _zip_names(data, stream):
    import zipfile
    try:
        src = stream if stream is not None else _io.BytesIO(data)
        with zipfile.ZipFile(src) as z:
            names = z.namelist()
            mimetype = None
            if "mimetype" in names:
                mimetype = z.read("mimetype").strip()
            return names, mimetype
    except Exception:
        return None, None


def _classify_zip(data, stream):
    names, mimetype = _zip_names(data, stream)
    if names is None:
        return "zip"
    if mimetype == b"application/epub+zip":
        return "epub"
    if mimetype in (b"application/vnd.oasis.opendocument.text", ):
        return "odt"
    if mimetype in (b"application/vnd.oasis.opendocument.spreadsheet", ):
        return "ods"
    if mimetype in (b"application/vnd.oasis.opendocument.presentation", ):
        return "odp"
    has = set(n.split("/", 1)[0] for n in names)
    if "[Content_Types].xml" in names:
        if "word" in has:
            return "docx"
        if "xl" in has:
            return "xlsx"
        if "ppt" in has:
            return "pptx"
    if "AndroidManifest.xml" in names and "classes.dex" in names:
        return "apk"
    if "META-INF/MANIFEST.MF" in names:
        return "jar"
    return "zip"


def _utf16(name):
    return name.encode("utf-16-le")


def _classify_ole(data):
    if _utf16("__substg1.0_") in data or _utf16("__properties_version1.0") in data:
        return "outlook"
    if _utf16("WordDocument") in data:
        return "doc"
    if _utf16("Workbook") in data or _utf16("Book") in data:
        return "xls"
    if _utf16("PowerPoint Document") in data:
        return "ppt"
    return "unknown"


def _decode_text(data):
    if data.startswith(b"\xef\xbb\xbf"):
        data = data[3:]
    try:
        return data.decode("utf-8")
    except UnicodeDecodeError as e:
        if e.start >= len(data) - 3 and len(data) >= _SAMPLE:
            return data[:e.start].decode("utf-8")
    try:
        import charset_normalizer
        best = charset_normalizer.from_bytes(data).best()
        if best is not None:
            return str(best)
    except Exception:
        pass
    # Text in an encoding this runtime cannot decode (Shift-JIS, cp1252) is
    # still text: no NUL and few control bytes. Latin-1 maps every byte, so
    # the ASCII structure a classifier looks at (commas, tags) survives.
    if b"\0" not in data:
        ctl = sum(1 for b in data if b < 32 and b not in (9, 10, 12, 13))
        if ctl * 100 <= len(data):
            return data.decode("latin-1")
    return None


def _classify_text(text, truncated):
    s = text.lstrip()
    low = s[:2048].lower()
    if not s:
        return "txt"
    if s[0] in "{[":
        try:
            obj = _json.loads(text)
            if isinstance(obj, dict) and "cells" in obj and "nbformat" in obj:
                return "ipynb"
            return "json"
        except ValueError:
            if truncated:
                return "json"
            lines = [ln for ln in text.splitlines() if ln.strip()]
            ok = 0
            for ln in lines[:20]:
                try:
                    _json.loads(ln)
                    ok += 1
                except ValueError:
                    pass
            if lines and ok == min(len(lines), 20):
                return "jsonl"
    if low.startswith("{\\rtf"):
        return "rtf"
    if "<!doctype html" in low or "<html" in low:
        return "html"
    if low.startswith("<"):
        # Magika's model labels an RSS or Atom document plain xml, and
        # markitdown's own sniffing tells the feed apart.
        if "<svg" in low:
            return "svg"
        if low.startswith("<?xml") or low.startswith("<"):
            if "<body" in low or "<div" in low or "<p>" in low or "<head" in low:
                return "html"
            return "xml"
    lines = text.splitlines()
    if len(lines) >= 2:
        for sep in (",", "\t", ";"):
            counts = [ln.count(sep) for ln in lines[:20] if ln.strip()]
            if len(counts) >= 2 and counts[0] > 0 and all(c == counts[0] for c in counts):
                return "csv"
    md = 0
    for ln in lines[:200]:
        t = ln.lstrip()
        if t.startswith("#") and t[1:2] in (" ", "#"):
            md += 2
        elif t.startswith("```") or t.startswith("- ") or t.startswith("* ") or t.startswith("> "):
            md += 1
        elif "](" in t and "[" in t:
            md += 1
    if md >= 3:
        return "markdown"
    py = 0
    for ln in lines[:200]:
        t = ln.strip()
        if t.startswith("def ") or t.startswith("import ") or t.startswith("from ") or t.startswith("class "):
            py += 1
    if py >= 3:
        return "python"
    return "txt"


def _classify(data, stream=None, truncated=False):
    if not data:
        return "empty"
    if data.startswith(b"%PDF-"):
        return "pdf"
    if data.startswith(b"PK\x03\x04") or data.startswith(b"PK\x05\x06"):
        return _classify_zip(data, stream)
    if data.startswith(b"\xd0\xcf\x11\xe0\xa1\xb1\x1a\xe1"):
        return _classify_ole(data)
    if data.startswith(b"\x89PNG\r\n\x1a\n"):
        return "png"
    if data.startswith(b"\xff\xd8\xff"):
        return "jpeg"
    if data.startswith(b"GIF87a") or data.startswith(b"GIF89a"):
        return "gif"
    if data[:4] == b"RIFF" and data[8:12] == b"WEBP":
        return "webp"
    if data[:4] == b"RIFF" and data[8:12] == b"WAVE":
        return "wav"
    if data.startswith(b"BM") and len(data) > 14:
        return "bmp"
    if data.startswith(b"II*\x00") or data.startswith(b"MM\x00*"):
        return "tiff"
    if data[4:8] == b"ftyp":
        brand = data[8:12]
        if brand in (b"heic", b"heix", b"mif1", b"msf1", b"heim", b"heis"):
            return "heif"
        return "mp4"
    if data.startswith(b"ID3") or (len(data) > 1 and data[0] == 0xFF and (data[1] & 0xE0) == 0xE0):
        return "mp3"
    if data.startswith(b"OggS"):
        return "ogg"
    if data.startswith(b"fLaC"):
        return "flac"
    if data.startswith(b"\x1f\x8b"):
        return "gzip"
    sample = data[:_SAMPLE]
    if b"\x00" in sample and not (sample.startswith(b"\xff\xfe") or sample.startswith(b"\xfe\xff")):
        return "unknown"
    text = _decode_text(sample)
    if text is None:
        return "unknown"
    return _classify_text(text, truncated or len(data) > _SAMPLE)


def _result(path, label):
    info = _TYPES[label]
    return MagikaResult(path, Status.OK, MagikaPrediction(info, info, 1.0 if label != "unknown" else 0.0))


class Magika:
    def __init__(self, model_dir=None, prediction_mode=PredictionMode.HIGH_CONFIDENCE, no_dereference=False,
                 verbose=False, debug=False, use_colors=False):
        self._prediction_mode = prediction_mode

    def get_model_name(self):
        return "signatures"

    def get_module_version(self):
        return __version__

    def identify_bytes(self, content):
        if not isinstance(content, (bytes, bytearray)):
            raise TypeError("Input content should be of type 'bytes'.")
        return _result("-", _classify(bytes(content)))

    def identify_stream(self, stream):
        pos = stream.tell()
        try:
            data = stream.read()
        finally:
            stream.seek(pos)
        if data[:4] == b"PK\x03\x04" or data[:4] == b"PK\x05\x06":
            return _result("-", _classify_zip(data, _io.BytesIO(data)))
        return _result("-", _classify(data))

    def identify_path(self, path):
        path = _os.fspath(path)
        if not _os.path.exists(path):
            return MagikaResult(path, Status.FILE_NOT_FOUND_ERROR)
        if _os.path.isdir(path):
            info = ContentTypeInfo("directory", "inode/directory", "inode", "A directory", [], False)
            return MagikaResult(path, Status.OK, MagikaPrediction(info, info, 1.0))
        with open(path, "rb") as f:
            data = f.read()
        r = _result(path, _classify(data))
        return r

    def identify_paths(self, paths):
        return [self.identify_path(p) for p in paths]

    def identify_bytes_list(self, contents):
        return [self.identify_bytes(c) for c in contents]

    def get_output_content_types(self):
        return list(_TYPES.keys())

    def get_model_content_types(self):
        return list(_TYPES.keys())
