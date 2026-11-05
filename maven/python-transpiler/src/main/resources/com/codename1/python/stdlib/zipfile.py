# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""ZIP archives: read, write and append, stored or deflated.

Entries are read whole into memory when opened. ZIP64 records are read
(sizes and offsets past 4 GiB), and written when an archive needs them.
Encrypted entries and bzip2/lzma compression raise NotImplementedError.
"""

import io
import os
import struct
import time
import zlib

__all__ = ["BadZipFile", "BadZipfile", "error", "ZIP_STORED", "ZIP_DEFLATED", "ZIP_BZIP2", "ZIP_LZMA",
           "is_zipfile", "ZipInfo", "ZipFile", "LargeZipFile", "Path"]


class BadZipFile(Exception):
    pass


class LargeZipFile(Exception):
    pass


error = BadZipfile = BadZipFile

ZIP_STORED = 0
ZIP_DEFLATED = 8
ZIP_BZIP2 = 12
ZIP_LZMA = 14
ZIP64_LIMIT = (1 << 31) - 1
ZIP_FILECOUNT_LIMIT = (1 << 16) - 1
ZIP_MAX_COMMENT = (1 << 16) - 1

_LOCAL = b"PK\003\004"
_CENTRAL = b"PK\001\002"
_END = b"PK\005\006"
_END64 = b"PK\006\006"
_END64_LOCATOR = b"PK\006\007"


# The upper half of code page 437, the encoding of names not flagged UTF-8.
_CP437_HIGH = "\u00c7\u00fc\u00e9\u00e2\u00e4\u00e0\u00e5\u00e7\u00ea\u00eb\u00e8\u00ef\u00ee\u00ec\u00c4\u00c5\u00c9\u00e6\u00c6\u00f4\u00f6\u00f2\u00fb\u00f9\u00ff\u00d6\u00dc\u00a2\u00a3\u00a5\u20a7\u0192\u00e1\u00ed\u00f3\u00fa\u00f1\u00d1\u00aa\u00ba\u00bf\u2310\u00ac\u00bd\u00bc\u00a1\u00ab\u00bb\u2591\u2592\u2593\u2502\u2524\u2561\u2562\u2556\u2555\u2563\u2551\u2557\u255d\u255c\u255b\u2510\u2514\u2534\u252c\u251c\u2500\u253c\u255e\u255f\u255a\u2554\u2569\u2566\u2560\u2550\u256c\u2567\u2568\u2564\u2565\u2559\u2558\u2552\u2553\u256b\u256a\u2518\u250c\u2588\u2584\u258c\u2590\u2580\u03b1\u00df\u0393\u03c0\u03a3\u03c3\u00b5\u03c4\u03a6\u0398\u03a9\u03b4\u221e\u03c6\u03b5\u2229\u2261\u00b1\u2265\u2264\u2320\u2321\u00f7\u2248\u00b0\u2219\u00b7\u221a\u207f\u00b2\u25a0\u00a0"


def _cp437(raw):
    if max(raw, default=0) < 128:
        return raw.decode("ascii")
    return "".join(chr(b) if b < 128 else _CP437_HIGH[b - 128] for b in raw)


def is_zipfile(filename):
    try:
        if hasattr(filename, "read"):
            pos = filename.tell()
            filename.seek(0)
            data = filename.read()
            filename.seek(pos)
        else:
            with open(filename, "rb") as f:
                data = f.read()
        return _find_end(data) is not None
    except OSError:
        return False


def _find_end(data):
    start = max(0, len(data) - 22 - ZIP_MAX_COMMENT)
    i = data.rfind(_END, start)
    while i >= 0:
        if i + 22 <= len(data):
            clen = struct.unpack("<H", data[i + 20:i + 22])[0]
            if i + 22 + clen == len(data):
                return i
        i = data.rfind(_END, start, i)
    return None


class ZipInfo:
    __slots__ = ("orig_filename", "filename", "date_time", "compress_type", "compress_level", "comment",
                 "extra", "create_system", "create_version", "extract_version", "reserved", "flag_bits",
                 "volume", "internal_attr", "external_attr", "header_offset", "CRC", "compress_size",
                 "file_size", "_end_offset")

    def __init__(self, filename="NoName", date_time=(1980, 1, 1, 0, 0, 0)):
        self.orig_filename = filename
        null = filename.find("\0")
        if null >= 0:
            filename = filename[:null]
        if os.sep != "/":
            filename = filename.replace(os.sep, "/")
        self.filename = filename
        self.date_time = date_time
        if date_time[0] < 1980:
            raise ValueError("ZIP does not support timestamps before 1980")
        self.compress_type = ZIP_STORED
        self.compress_level = None
        self.comment = b""
        self.extra = b""
        self.create_system = 3
        self.create_version = 20
        self.extract_version = 20
        self.reserved = 0
        self.flag_bits = 0
        self.volume = 0
        self.internal_attr = 0
        self.external_attr = 0
        self.header_offset = 0
        self.CRC = 0
        self.compress_size = 0
        self.file_size = 0
        self._end_offset = None

    def __repr__(self):
        result = ["<%s filename=%r" % (self.__class__.__name__, self.filename)]
        if self.compress_type != ZIP_STORED:
            result.append(" compress_type=%s" % {8: "deflate", 12: "bzip2", 14: "lzma"}.get(
                self.compress_type, self.compress_type))
        hi = self.external_attr >> 16
        lo = self.external_attr & 0xFFFF
        if hi:
            import stat
            result.append(" filemode=%r" % stat.filemode(hi))
        if lo:
            result.append(" external_attr=%#x" % lo)
        isdir = self.is_dir()
        if not isdir or self.file_size:
            result.append(" file_size=%r" % self.file_size)
        if ((not isdir or self.compress_size) and
                (self.compress_type != ZIP_STORED or self.file_size != self.compress_size)):
            result.append(" compress_size=%r" % self.compress_size)
        result.append(">")
        return "".join(result)

    def is_dir(self):
        return self.filename.endswith("/")

    @classmethod
    def from_file(cls, filename, arcname=None, *, strict_timestamps=True):
        st = os.stat(filename)
        isdir = os.path.isdir(filename)
        mtime = time.localtime(st.st_mtime)
        date_time = mtime[0:6]
        if not strict_timestamps and date_time[0] < 1980:
            date_time = (1980, 1, 1, 0, 0, 0)
        if arcname is None:
            arcname = filename
        arcname = os.path.normpath(os.path.splitdrive(arcname)[1])
        while arcname[0] in (os.sep, os.altsep):
            arcname = arcname[1:]
        if isdir:
            arcname += "/"
        zinfo = cls(arcname, date_time)
        zinfo.external_attr = (st.st_mode & 0xFFFF) << 16
        if isdir:
            zinfo.file_size = 0
            zinfo.external_attr |= 0x10
        else:
            zinfo.file_size = st.st_size
        return zinfo

    def _dos(self):
        dt = self.date_time
        dosdate = (dt[0] - 1980) << 9 | dt[1] << 5 | dt[2]
        dostime = dt[3] << 11 | dt[4] << 5 | (dt[5] // 2)
        return dosdate, dostime

    def _encoded_name(self):
        try:
            return self.filename.encode("ascii"), self.flag_bits
        except UnicodeEncodeError:
            return self.filename.encode("utf-8"), self.flag_bits | 0x800


def _zip64_extra(extra, info, need_size, need_csize, need_offset):
    i = 0
    while i + 4 <= len(extra):
        tp, ln = struct.unpack("<HH", extra[i:i + 4])
        if tp == 1:
            body = extra[i + 4:i + 4 + ln]
            j = 0
            if need_size:
                info.file_size = struct.unpack("<Q", body[j:j + 8])[0]
                j += 8
            if need_csize:
                info.compress_size = struct.unpack("<Q", body[j:j + 8])[0]
                j += 8
            if need_offset:
                info.header_offset = struct.unpack("<Q", body[j:j + 8])[0]
            return
        i += 4 + ln


class _ZipWriteFile(io.BufferedIOBase):
    def __init__(self, zf, zinfo):
        self._zf = zf
        self._zinfo = zinfo
        self._chunks = []
        self.closed = False

    def writable(self):
        return True

    def write(self, data):
        if self.closed:
            raise ValueError("I/O operation on closed file.")
        data = bytes(data)
        self._chunks.append(data)
        return len(data)

    def close(self):
        if self.closed:
            return
        self.closed = True
        self._zf._writing = False
        self._zf._write_entry(self._zinfo, b"".join(self._chunks))


class ZipFile:
    fp = None

    def __init__(self, file, mode="r", compression=ZIP_STORED, allowZip64=True, compresslevel=None, *,
                 strict_timestamps=True, metadata_encoding=None):
        if mode not in ("r", "w", "x", "a"):
            raise ValueError("ZipFile requires mode 'r', 'w', 'x', or 'a'")
        _check_compression(compression)
        self._allowZip64 = allowZip64
        self.debug = 0
        self.NameToInfo = {}
        self.filelist = []
        self.compression = compression
        self.compresslevel = compresslevel
        self.mode = mode
        self.pwd = None
        self._comment = b""
        self._strict_timestamps = strict_timestamps
        self.metadata_encoding = metadata_encoding
        self._writing = False
        self._modified = False
        if isinstance(file, os.PathLike):
            file = os.fspath(file)
        if isinstance(file, str):
            self._filePassed = False
            self.filename = file
            data = b""
            if mode in ("r", "a"):
                try:
                    with open(file, "rb") as f:
                        data = f.read()
                except FileNotFoundError:
                    if mode == "r":
                        raise
                    mode = "w"
            elif mode == "x" and os.path.exists(file):
                raise FileExistsError(17, "File exists", file)
            self.fp = None
        else:
            self._filePassed = True
            self.fp = file
            self.filename = getattr(file, "name", None)
            data = b""
            if mode in ("r", "a"):
                file.seek(0)
                data = file.read()
        self._data = data
        if mode == "r":
            self._read_directory()
        elif mode == "a":
            if data and _find_end(data) is not None:
                self._read_directory()
                self._out = bytearray(data[:self.start_dir])
            else:
                if data:
                    raise BadZipFile("File is not a zip file")
                self._out = bytearray(data)
            self._modified = True
        else:
            self._out = bytearray()
            self._modified = True

    def __enter__(self):
        return self

    def __exit__(self, *exc):
        self.close()

    def __repr__(self):
        result = ["<%s.%s" % (self.__class__.__module__, self.__class__.__qualname__)]
        if self.filename is not None:
            result.append(" filename=%r" % self.filename)
        result.append(" mode=%r" % self.mode if self._data is not None else " [closed]")
        result.append(">")
        return "".join(result)

    def _read_directory(self):
        data = self._data
        end = _find_end(data)
        if end is None:
            raise BadZipFile("File is not a zip file")
        (sig, disk, diskcd, count_disk, count, size_cd, offset_cd, clen) = struct.unpack(
            "<4sHHHHLLH", data[end:end + 22])
        self._comment = data[end + 22:end + 22 + clen]
        loc = end - 20
        if loc >= 0 and data[loc:loc + 4] == _END64_LOCATOR:
            off64 = struct.unpack("<Q", data[loc + 8:loc + 16])[0]
            if data[off64:off64 + 4] == _END64:
                count, size_cd, offset_cd = struct.unpack("<QQQ", data[off64 + 32:off64 + 56])
        # an archive prepended with other data (a self-extractor) is read from its real start
        concat = (end - size_cd) - offset_cd
        if loc >= 0 and data[loc:loc + 4] == _END64_LOCATOR:
            concat -= 56 + 20
        self.start_dir = offset_cd + concat
        pos = self.start_dir
        stop = pos + size_cd
        while pos < stop:
            if data[pos:pos + 4] != _CENTRAL:
                raise BadZipFile("Bad magic number for central directory")
            (sig, cver, csys, ever, esys, flags, method, t, d, crc, csize, usize, nlen, xlen, mlen,
             disk, iattr, eattr, hoff) = struct.unpack("<4sBBBBHHHHLLLHHHHHLL", data[pos:pos + 46])
            pos += 46
            raw = data[pos:pos + nlen]
            pos += nlen
            if flags & 0x800:
                name = raw.decode("utf-8")
            elif self.metadata_encoding:
                name = raw.decode(self.metadata_encoding)
            else:
                name = _cp437(raw)
            zi = ZipInfo(name)
            zi.extra = data[pos:pos + xlen]
            pos += xlen
            zi.comment = data[pos:pos + mlen]
            pos += mlen
            zi.header_offset = hoff
            zi.create_version, zi.create_system = cver, csys
            zi.extract_version, zi.reserved = ever, esys
            zi.flag_bits, zi.compress_type, zi.CRC = flags, method, crc
            zi.compress_size, zi.file_size = csize, usize
            zi.volume, zi.internal_attr, zi.external_attr = disk, iattr, eattr
            _zip64_extra(zi.extra, zi, usize == 0xFFFFFFFF, csize == 0xFFFFFFFF, hoff == 0xFFFFFFFF)
            zi.header_offset += concat
            zi.date_time = ((d >> 9) + 1980, (d >> 5) & 0xF, d & 0x1F, t >> 11, (t >> 5) & 0x3F,
                            (t & 0x1F) * 2)
            self.filelist.append(zi)
            self.NameToInfo[zi.filename] = zi

    @property
    def comment(self):
        return self._comment

    @comment.setter
    def comment(self, comment):
        if not isinstance(comment, bytes):
            raise TypeError("comment: expected bytes, got %s" % type(comment).__name__)
        self._comment = comment[:ZIP_MAX_COMMENT]
        self._modified = True

    def namelist(self):
        return [zi.filename for zi in self.filelist]

    def infolist(self):
        return list(self.filelist)

    def printdir(self, file=None):
        print("%-46s %19s %12s" % ("File Name", "Modified    ", "Size"), file=file)
        for zinfo in self.filelist:
            date = "%d-%02d-%02d %02d:%02d:%02d" % zinfo.date_time[:6]
            print("%-46s %s %12d" % (zinfo.filename, date, zinfo.file_size), file=file)

    def getinfo(self, name):
        info = self.NameToInfo.get(name)
        if info is None:
            raise KeyError("There is no item named %r in the archive" % name)
        return info

    def setpassword(self, pwd):
        self.pwd = pwd

    def _source(self):
        return self._data if self.mode == "r" else bytes(self._out)

    def _read_entry(self, zinfo):
        data = self._source()
        off = zinfo.header_offset
        if data[off:off + 4] != _LOCAL:
            raise BadZipFile("Bad magic number for file header")
        nlen, xlen = struct.unpack("<HH", data[off + 26:off + 30])
        start = off + 30 + nlen + xlen
        if zinfo.flag_bits & 0x1:
            raise NotImplementedError("That compression method is not supported")
        raw = data[start:start + zinfo.compress_size]
        if zinfo.compress_type == ZIP_STORED:
            out = raw
        elif zinfo.compress_type == ZIP_DEFLATED:
            out = zlib.decompress(raw, -15)
        else:
            raise NotImplementedError("That compression method is not supported")
        if zlib.crc32(out) != zinfo.CRC:
            raise BadZipFile("Bad CRC-32 for file %r" % zinfo.filename)
        return out

    def read(self, name, pwd=None):
        zinfo = name if isinstance(name, ZipInfo) else self.getinfo(name)
        return self._read_entry(zinfo)

    def open(self, name, mode="r", pwd=None, *, force_zip64=False):
        if mode not in ("r", "w"):
            raise ValueError('open() requires mode "r" or "w"')
        if mode == "w":
            if self.mode == "r":
                raise ValueError("write() requires mode 'w', 'x', or 'a'")
            if isinstance(name, ZipInfo):
                zinfo = name
            else:
                zinfo = ZipInfo(name, time.localtime(time.time())[:6])
                zinfo.compress_type = self.compression
                zinfo.compress_level = self.compresslevel
            if zinfo.is_dir():
                zinfo.external_attr = 0o40775 << 16
                zinfo.external_attr |= 0x10
            elif not zinfo.external_attr:
                zinfo.external_attr = 0o600 << 16
            if self._writing:
                raise ValueError("Can't write to the ZIP file while there is another write handle open on it. "
                                 "Close the first handle before opening another.")
            self._writing = True
            return _ZipWriteFile(self, zinfo)
        zinfo = name if isinstance(name, ZipInfo) else self.getinfo(name)
        f = io.BytesIO(self._read_entry(zinfo))
        f.name = zinfo.filename
        return f

    def _write_entry(self, zinfo, data):
        if zinfo.filename in self.NameToInfo:
            import warnings
            warnings.warn("Duplicate name: %r" % zinfo.filename, UserWarning, stacklevel=3)
        zinfo.file_size = len(data)
        zinfo.CRC = zlib.crc32(data)
        if zinfo.compress_type == ZIP_DEFLATED:
            level = zinfo.compress_level
            c = zlib.compressobj(-1 if level is None else level, zlib.DEFLATED, -15)
            comp = c.compress(data) + c.flush()
        elif zinfo.compress_type == ZIP_STORED:
            comp = data
        else:
            raise NotImplementedError("That compression method is not supported")
        zinfo.compress_size = len(comp)
        if zinfo.compress_type == ZIP_DEFLATED:
            zinfo.extract_version = max(zinfo.extract_version, 20)
        zinfo.header_offset = len(self._out)
        if zinfo.file_size > ZIP64_LIMIT or zinfo.header_offset > ZIP64_LIMIT:
            if not self._allowZip64:
                raise LargeZipFile("Filesize would require ZIP64 extensions")
        name, flags = zinfo._encoded_name()
        zinfo.flag_bits = flags
        dosdate, dostime = zinfo._dos()
        self._out += struct.pack("<4sHHHHHLLLHH", _LOCAL, zinfo.extract_version, zinfo.flag_bits,
                                 zinfo.compress_type, dostime, dosdate, zinfo.CRC, zinfo.compress_size,
                                 zinfo.file_size, len(name), len(zinfo.extra))
        self._out += name + zinfo.extra + comp
        self.filelist.append(zinfo)
        self.NameToInfo[zinfo.filename] = zinfo
        self._modified = True

    def write(self, filename, arcname=None, compress_type=None, compresslevel=None):
        if self.mode == "r":
            raise ValueError("write() requires mode 'w', 'x', or 'a'")
        zinfo = ZipInfo.from_file(filename, arcname, strict_timestamps=self._strict_timestamps)
        if zinfo.is_dir():
            zinfo.compress_size = 0
            zinfo.CRC = 0
            self._write_entry(zinfo, b"")
            return
        zinfo.compress_type = compress_type if compress_type is not None else self.compression
        zinfo.compress_level = compresslevel if compresslevel is not None else self.compresslevel
        with open(filename, "rb") as f:
            self._write_entry(zinfo, f.read())

    def writestr(self, zinfo_or_arcname, data, compress_type=None, compresslevel=None):
        if self.mode == "r":
            raise ValueError("write() requires mode 'w', 'x', or 'a'")
        if isinstance(data, str):
            data = data.encode("utf-8")
        if not isinstance(zinfo_or_arcname, ZipInfo):
            zinfo = ZipInfo(zinfo_or_arcname, time.localtime(time.time())[:6])
            zinfo.compress_type = self.compression
            zinfo.compress_level = self.compresslevel
            if zinfo.is_dir():
                zinfo.external_attr = 0o40775 << 16
                zinfo.external_attr |= 0x10
            else:
                zinfo.external_attr = 0o600 << 16
        else:
            zinfo = zinfo_or_arcname
        if compress_type is not None:
            zinfo.compress_type = compress_type
        if compresslevel is not None:
            zinfo.compress_level = compresslevel
        self._write_entry(zinfo, bytes(data))

    def mkdir(self, zinfo_or_directory_name, mode=511):
        if isinstance(zinfo_or_directory_name, ZipInfo):
            zinfo = zinfo_or_directory_name
        else:
            name = zinfo_or_directory_name
            if not name.endswith("/"):
                name += "/"
            zinfo = ZipInfo(name, time.localtime(time.time())[:6])
            zinfo.external_attr = ((0o40000 | mode) & 0xFFFF) << 16
            zinfo.external_attr |= 0x10
        self._write_entry(zinfo, b"")

    def testzip(self):
        for zinfo in self.filelist:
            try:
                self._read_entry(zinfo)
            except BadZipFile:
                return zinfo.filename
        return None

    def extract(self, member, path=None, pwd=None):
        if path is None:
            path = os.getcwd()
        else:
            path = os.fspath(path)
        return self._extract_member(member, path)

    def extractall(self, path=None, members=None, pwd=None):
        if members is None:
            members = self.namelist()
        if path is None:
            path = os.getcwd()
        else:
            path = os.fspath(path)
        for zipinfo in members:
            self._extract_member(zipinfo, path)

    def _extract_member(self, member, targetpath):
        if not isinstance(member, ZipInfo):
            member = self.getinfo(member)
        arcname = member.filename.replace("/", os.path.sep)
        arcname = os.path.splitdrive(arcname)[1]
        invalid = ("", os.path.curdir, os.path.pardir)
        arcname = os.path.sep.join(x for x in arcname.split(os.path.sep) if x not in invalid)
        targetpath = os.path.join(targetpath, arcname)
        targetpath = os.path.normpath(targetpath)
        upperdirs = os.path.dirname(targetpath)
        if upperdirs and not os.path.exists(upperdirs):
            os.makedirs(upperdirs)
        if member.is_dir():
            if not os.path.isdir(targetpath):
                os.mkdir(targetpath)
            return targetpath
        with open(targetpath, "wb") as target:
            target.write(self._read_entry(member))
        return targetpath

    def _central(self):
        out = bytearray()
        for zinfo in self.filelist:
            name, flags = zinfo._encoded_name()
            dosdate, dostime = zinfo._dos()
            out += struct.pack("<4sBBBBHHHHLLLHHHHHLL", _CENTRAL, zinfo.create_version, zinfo.create_system,
                               zinfo.extract_version, zinfo.reserved, flags, zinfo.compress_type, dostime,
                               dosdate, zinfo.CRC, zinfo.compress_size, zinfo.file_size, len(name),
                               len(zinfo.extra), len(zinfo.comment), 0, zinfo.internal_attr,
                               zinfo.external_attr, zinfo.header_offset)
            out += name + zinfo.extra + zinfo.comment
        return bytes(out)

    def close(self):
        if self._data is None:
            return
        if self._writing:
            raise ValueError("Can't close the ZIP file while there is an open writing handle on it. "
                             "Close the writing handle before closing the zip.")
        try:
            if self.mode in ("w", "x", "a") and self._modified:
                start = len(self._out)
                central = self._central()
                count = len(self.filelist)
                self._out += central
                self._out += struct.pack("<4sHHHHLLH", _END, 0, 0, count, count, len(central), start,
                                         len(self._comment))
                self._out += self._comment
                if self._filePassed:
                    self.fp.seek(0)
                    self.fp.write(bytes(self._out))
                    if hasattr(self.fp, "truncate"):
                        self.fp.truncate()
                else:
                    with open(self.filename, "wb") as f:
                        f.write(bytes(self._out))
        finally:
            self._data = None


def _check_compression(compression):
    if compression in (ZIP_STORED, ZIP_DEFLATED):
        return
    if compression in (ZIP_BZIP2, ZIP_LZMA):
        raise RuntimeError("Compression requires the (missing) %s module" %
                           ("bz2" if compression == ZIP_BZIP2 else "lzma"))
    raise NotImplementedError("That compression method is not supported")


class Path:
    """A path inside an archive, in the manner of pathlib."""

    def __init__(self, root, at=""):
        self.root = root if isinstance(root, ZipFile) else ZipFile(root)
        self.at = at

    @property
    def name(self):
        return self.at.rstrip("/").split("/")[-1]

    def _names(self):
        return self.root.namelist()

    def is_dir(self):
        return not self.at or self.at.endswith("/") or any(n.startswith(self.at + "/") for n in self._names())

    def is_file(self):
        return self.at in self._names() and not self.at.endswith("/")

    def exists(self):
        return self.is_dir() or self.is_file()

    def iterdir(self):
        prefix = self.at if not self.at or self.at.endswith("/") else self.at + "/"
        seen = []
        for n in self._names():
            if n.startswith(prefix) and n != prefix:
                rest = n[len(prefix):]
                child = prefix + rest.split("/")[0] + ("/" if "/" in rest else "")
                if child not in seen:
                    seen.append(child)
        return iter([Path(self.root, c) for c in seen])

    def __truediv__(self, add):
        base = self.at if not self.at or self.at.endswith("/") else self.at + "/"
        return Path(self.root, base + add)

    joinpath = __truediv__

    def read_bytes(self):
        return self.root.read(self.at)

    def read_text(self, encoding="utf-8", *args, **kwargs):
        return self.read_bytes().decode(encoding)

    def open(self, mode="r", *args, **kwargs):
        f = self.root.open(self.at)
        return io.TextIOWrapper(f, *args, **kwargs) if mode == "r" else f

    def __str__(self):
        return (self.root.filename or "") + "/" + self.at

    def __repr__(self):
        return "Path(%r, %r)" % (self.root.filename, self.at)
