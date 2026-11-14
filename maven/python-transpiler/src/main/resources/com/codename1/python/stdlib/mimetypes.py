# Copyright (c) 2001 Python Software Foundation; All Rights Reserved
# Licensed under the Python Software Foundation License Version 2; the full
# text is in maven/python-transpiler/third-party/cpython/LICENSE.txt.
#
# Derived from CPython's Lib/mimetypes.py. Changes: rewritten for the Codename One
# Python runtime, trimmed to what the transpiler supports and reorganised;
# the C accelerators and platform-specific branches are left out.
"""mimetypes: guess a MIME type from a file name and back.

The tables are built in; a device has no mime.types file to read, so
knownfiles is empty and init() only reads the files it is given."""

import os as _os

__all__ = ["knownfiles", "inited", "MimeTypes", "guess_type", "guess_file_type", "guess_all_extensions",
           "guess_extension", "add_type", "init", "read_mime_types", "suffix_map", "encodings_map", "types_map",
           "common_types"]

knownfiles = []
inited = False
_db = None

_suffix_map_default = {
    ".svgz": ".svg.gz", ".tgz": ".tar.gz", ".taz": ".tar.gz", ".tz": ".tar.gz", ".tbz2": ".tar.bz2",
    ".txz": ".tar.xz",
}

_encodings_map_default = {
    ".gz": "gzip", ".Z": "compress", ".bz2": "bzip2", ".xz": "xz", ".br": "br",
}

# The preferred extension of each type comes first, so guess_extension
# answers with it.
_types_map_default = [
    (".js", "text/javascript"), (".mjs", "text/javascript"),
    (".epub", "application/epub+zip"), (".gz", "application/gzip"),
    (".json", "application/json"), (".webmanifest", "application/manifest+json"),
    (".doc", "application/msword"), (".dot", "application/msword"), (".wiz", "application/msword"),
    (".nq", "application/n-quads"), (".nt", "application/n-triples"),
    (".bin", "application/octet-stream"), (".a", "application/octet-stream"), (".dll", "application/octet-stream"),
    (".exe", "application/octet-stream"), (".o", "application/octet-stream"), (".obj", "application/octet-stream"),
    (".so", "application/octet-stream"), (".oda", "application/oda"), (".ogx", "application/ogg"),
    (".pdf", "application/pdf"), (".p7c", "application/pkcs7-mime"), (".ps", "application/postscript"),
    (".ai", "application/postscript"), (".eps", "application/postscript"),
    (".trig", "application/trig"), (".m3u", "application/vnd.apple.mpegurl"),
    (".m3u8", "application/vnd.apple.mpegurl"), (".xls", "application/vnd.ms-excel"),
    (".xlb", "application/vnd.ms-excel"), (".eot", "application/vnd.ms-fontobject"),
    (".ppt", "application/vnd.ms-powerpoint"), (".pot", "application/vnd.ms-powerpoint"),
    (".ppa", "application/vnd.ms-powerpoint"), (".pps", "application/vnd.ms-powerpoint"),
    (".pwz", "application/vnd.ms-powerpoint"),
    (".odg", "application/vnd.oasis.opendocument.graphics"),
    (".odp", "application/vnd.oasis.opendocument.presentation"),
    (".ods", "application/vnd.oasis.opendocument.spreadsheet"),
    (".odt", "application/vnd.oasis.opendocument.text"),
    (".pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
    (".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    (".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    (".rar", "application/vnd.rar"), (".wasm", "application/wasm"), (".7z", "application/x-7z-compressed"),
    (".bcpio", "application/x-bcpio"), (".cpio", "application/x-cpio"), (".csh", "application/x-csh"),
    (".deb", "application/x-debian-package"), (".dvi", "application/x-dvi"), (".gtar", "application/x-gtar"),
    (".hdf", "application/x-hdf"), (".h5", "application/x-hdf5"), (".latex", "application/x-latex"),
    (".mif", "application/x-mif"), (".cdf", "application/x-netcdf"), (".nc", "application/x-netcdf"),
    (".p12", "application/x-pkcs12"), (".php", "application/x-httpd-php"), (".pfx", "application/x-pkcs12"),
    (".ram", "application/x-pn-realaudio"), (".pyc", "application/x-python-code"),
    (".pyo", "application/x-python-code"), (".rpm", "application/x-rpm"), (".sh", "application/x-sh"),
    (".shar", "application/x-shar"), (".swf", "application/x-shockwave-flash"),
    (".sv4cpio", "application/x-sv4cpio"), (".sv4crc", "application/x-sv4crc"), (".tar", "application/x-tar"),
    (".tcl", "application/x-tcl"), (".tex", "application/x-tex"), (".texi", "application/x-texinfo"),
    (".texinfo", "application/x-texinfo"), (".roff", "application/x-troff"), (".t", "application/x-troff"),
    (".tr", "application/x-troff"), (".man", "application/x-troff-man"), (".me", "application/x-troff-me"),
    (".ms", "application/x-troff-ms"), (".ustar", "application/x-ustar"), (".src", "application/x-wais-source"),
    (".xsl", "application/xml"), (".rdf", "application/xml"), (".wsdl", "application/xml"),
    (".xpdl", "application/xml"), (".yaml", "application/yaml"), (".yml", "application/yaml"),
    (".zip", "application/zip"),
    (".3gp", "audio/3gpp"), (".3gpp", "audio/3gpp"), (".3g2", "audio/3gpp2"), (".3gpp2", "audio/3gpp2"),
    (".aac", "audio/aac"), (".adts", "audio/aac"), (".loas", "audio/aac"), (".ass", "audio/aac"),
    (".au", "audio/basic"), (".snd", "audio/basic"), (".flac", "audio/flac"), (".mka", "audio/matroska"),
    (".m4a", "audio/mp4"), (".mp3", "audio/mpeg"), (".mp2", "audio/mpeg"), (".ogg", "audio/ogg"),
    (".opus", "audio/opus"), (".aif", "audio/x-aiff"), (".aifc", "audio/x-aiff"), (".aiff", "audio/x-aiff"),
    (".ra", "audio/x-pn-realaudio"), (".wav", "audio/vnd.wave"), (".otf", "font/otf"), (".ttf", "font/ttf"),
    (".weba", "audio/webm"), (".woff", "font/woff"), (".woff2", "font/woff2"),
    (".avif", "image/avif"), (".bmp", "image/bmp"), (".emf", "image/emf"), (".fits", "image/fits"),
    (".g3", "image/g3fax"), (".gif", "image/gif"), (".ief", "image/ief"), (".jp2", "image/jp2"),
    (".jpg", "image/jpeg"), (".jpe", "image/jpeg"), (".jpeg", "image/jpeg"),
    (".jpm", "image/jpm"), (".jpx", "image/jpx"), (".heic", "image/heic"), (".heif", "image/heif"),
    (".png", "image/png"), (".svg", "image/svg+xml"), (".t38", "image/t38"), (".tiff", "image/tiff"),
    (".tif", "image/tiff"), (".tfx", "image/tiff-fx"), (".ico", "image/vnd.microsoft.icon"),
    (".webp", "image/webp"), (".wmf", "image/wmf"), (".ras", "image/x-cmu-raster"),
    (".pnm", "image/x-portable-anymap"), (".pbm", "image/x-portable-bitmap"),
    (".pgm", "image/x-portable-graymap"), (".ppm", "image/x-portable-pixmap"), (".rgb", "image/x-rgb"),
    (".xbm", "image/x-xbitmap"), (".xpm", "image/x-xpixmap"), (".xwd", "image/x-xwindowdump"),
    (".eml", "message/rfc822"), (".mht", "message/rfc822"), (".mhtml", "message/rfc822"),
    (".nws", "message/rfc822"), (".gltf", "model/gltf+json"), (".glb", "model/gltf-binary"),
    (".stl", "model/stl"), (".css", "text/css"), (".csv", "text/csv"), (".html", "text/html"),
    (".htm", "text/html"), (".md", "text/markdown"), (".markdown", "text/markdown"), (".n3", "text/n3"),
    (".txt", "text/plain"), (".bat", "text/plain"), (".c", "text/plain"), (".h", "text/plain"),
    (".ksh", "text/plain"), (".pl", "text/plain"), (".srt", "text/plain"), (".rtx", "text/richtext"),
    (".rtf", "text/rtf"), (".tsv", "text/tab-separated-values"), (".vtt", "text/vtt"), (".py", "text/x-python"),
    (".rst", "text/x-rst"), (".etx", "text/x-setext"), (".sgm", "text/x-sgml"), (".sgml", "text/x-sgml"),
    (".vcf", "text/x-vcard"), (".xml", "text/xml"), (".mkv", "video/matroska"), (".mk3d", "video/matroska-3d"),
    (".mp4", "video/mp4"), (".mpeg", "video/mpeg"), (".m1v", "video/mpeg"), (".mpa", "video/mpeg"),
    (".mpe", "video/mpeg"), (".mpg", "video/mpeg"), (".ogv", "video/ogg"), (".mov", "video/quicktime"),
    (".qt", "video/quicktime"), (".webm", "video/webm"), (".avi", "video/vnd.avi"), (".m4v", "video/x-m4v"),
    (".wmv", "video/x-ms-wmv"), (".movie", "video/x-sgi-movie"),
]

_common_types_default = [
    (".rtf", "application/rtf"), (".apk", "application/vnd.android.package-archive"),
    (".midi", "audio/midi"), (".mid", "audio/midi"), (".jpg", "image/jpg"), (".pict", "image/pict"),
    (".pct", "image/pict"), (".pic", "image/pict"), (".webp", "image/webp"), (".xul", "text/xul"),
]


class MimeTypes:
    def __init__(self, filenames=(), strict=True):
        self.encodings_map = dict(_encodings_map_default)
        self.suffix_map = dict(_suffix_map_default)
        self.types_map = ({}, {})
        self.types_map_inv = ({}, {})
        for ext, t in _types_map_default:
            self.add_type(t, ext, True)
        for ext, t in _common_types_default:
            self.add_type(t, ext, False)
        for name in filenames:
            self.read(name, strict)

    def add_type(self, type, ext, strict=True):
        if ext and not ext.startswith("."):
            raise ValueError("Extensions should start with a '.' or be empty: %r" % ext)
        if not type:
            return
        self.types_map[strict][ext] = type
        exts = self.types_map_inv[strict].setdefault(type, [])
        if ext not in exts:
            exts.append(ext)

    def guess_type(self, url, strict=True):
        url = _os.fspath(url)
        lower = url[:5].lower() if isinstance(url, str) else ""
        if lower.startswith("data:"):
            comma = url.find(",")
            if comma < 0:
                return None, None
            semi = url.find(";", 0, comma)
            if semi >= 0:
                type = url[5:semi]
            else:
                type = url[5:comma]
            if "=" in type or "/" not in type:
                type = "text/plain"
            return type, None
        scheme_end = url.find(":")
        if scheme_end > 1 and "/" not in url[:scheme_end] and "\\" not in url[:scheme_end]:
            rest = url[scheme_end + 1:]
            for c in "?#":
                k = rest.find(c)
                if k >= 0:
                    rest = rest[:k]
            url = rest
        return self._guess_file_type(url, strict, _posix_splitext)

    def guess_file_type(self, path, strict=True):
        path = _os.fsdecode(path) if isinstance(path, bytes) else _os.fspath(path)
        path = _os.path.splitdrive(path)[1]
        return self._guess_file_type(path, strict, _os.path.splitext)

    def _guess_file_type(self, path, strict, splitext):
        base, ext = splitext(path)
        while (ext_lower := ext.lower()) in self.suffix_map:
            base, ext = splitext(base + self.suffix_map[ext_lower])
        if ext in self.encodings_map:
            encoding = self.encodings_map[ext]
            base, ext = splitext(base)
        else:
            encoding = None
        ext = ext.lower()
        types_map = self.types_map[True]
        if ext in types_map:
            return types_map[ext], encoding
        if strict:
            return None, encoding
        types_map = self.types_map[False]
        if ext in types_map:
            return types_map[ext], encoding
        return None, encoding

    def guess_all_extensions(self, type, strict=True):
        type = type.lower()
        extensions = list(self.types_map_inv[True].get(type, []))
        if not strict:
            for ext in self.types_map_inv[False].get(type, []):
                if ext not in extensions:
                    extensions.append(ext)
        return extensions

    def guess_extension(self, type, strict=True):
        extensions = self.guess_all_extensions(type, strict)
        if not extensions:
            return None
        return extensions[0]

    def read(self, filename, strict=True):
        with open(filename, encoding="utf-8") as fp:
            self.readfp(fp, strict)

    def readfp(self, fp, strict=True):
        for line in fp:
            words = line.split()
            for i in range(len(words)):
                if words[i][0] == "#":
                    del words[i:]
                    break
            if not words:
                continue
            type, suffixes = words[0], words[1:]
            for suff in suffixes:
                self.add_type(type, "." + suff, strict)


def _posix_splitext(p):
    sep = p.rfind("/")
    dot = p.rfind(".")
    if dot > sep:
        i = sep + 1
        while i < dot:
            if p[i] != ".":
                return p[:dot], p[dot:]
            i += 1
    return p, p[:0]


def _get_db():
    global _db, inited
    if _db is None:
        _db = MimeTypes()
        inited = True
        _sync()
    return _db


def _sync():
    global suffix_map, encodings_map, types_map, common_types
    suffix_map = _db.suffix_map
    encodings_map = _db.encodings_map
    types_map = _db.types_map[True]
    common_types = _db.types_map[False]


def guess_type(url, strict=True):
    return _get_db().guess_type(url, strict)


def guess_file_type(path, *, strict=True):
    return _get_db().guess_file_type(path, strict)


def guess_all_extensions(type, strict=True):
    return _get_db().guess_all_extensions(type, strict)


def guess_extension(type, strict=True):
    return _get_db().guess_extension(type, strict)


def add_type(type, ext, strict=True):
    return _get_db().add_type(type, ext, strict)


def init(files=None):
    global _db, inited
    db = MimeTypes()
    for f in (files or ()):
        if _os.path.isfile(f):
            db.read(f)
    _db = db
    inited = True
    _sync()


def read_mime_types(file):
    try:
        f = open(file, encoding="utf-8")
    except OSError:
        return None
    with f:
        db = MimeTypes()
        db.readfp(f, True)
        return db.types_map[True]


_get_db()
