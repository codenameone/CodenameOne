# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Object-oriented paths over the application's file system (POSIX flavour)."""

import os
import io as _io
from os import path as _p

__all__ = ["PurePath", "PurePosixPath", "Path", "PosixPath", "UnsupportedOperation"]

UnsupportedOperation = _io.UnsupportedOperation


class PurePath:
    __slots__ = ("_raw", "_str", "_parts_cache")

    def __new__(cls, *args):
        if cls is PurePath:
            cls = PurePosixPath
        return object.__new__(cls)

    def __init__(self, *args):
        segs = []
        for a in args:
            if isinstance(a, PurePath):
                segs.append(str(a))
            else:
                a = os.fspath(a)
                if not isinstance(a, str):
                    raise TypeError("argument should be a str or an os.PathLike object where "
                                    "__fspath__ returns a str, not %r" % type(a).__name__)
                segs.append(a)
        self._raw = _p.join(*segs) if segs else ""
        self._str = None
        self._parts_cache = None

    def with_segments(self, *args):
        return type(self)(*args)

    def _split(self):
        if self._parts_cache is None:
            raw = self._raw
            root = ""
            if raw.startswith("/"):
                root = "//" if raw.startswith("//") and not raw.startswith("///") else "/"
            parts = [x for x in raw.split("/") if x and x != "."]
            self._parts_cache = (root, parts)
        return self._parts_cache

    def __str__(self):
        if self._str is None:
            root, parts = self._split()
            self._str = (root + "/".join(parts)) or "."
        return self._str

    def __fspath__(self):
        return str(self)

    def as_posix(self):
        return str(self)

    def __bytes__(self):
        return os.fsencode(str(self))

    def __repr__(self):
        return "%s(%r)" % (type(self).__name__, self.as_posix())

    def as_uri(self):
        if not self.is_absolute():
            raise ValueError("relative path can't be expressed as a file URI")
        return "file://" + str(self)

    def __hash__(self):
        return hash(str(self))

    def __eq__(self, other):
        if not isinstance(other, PurePath):
            return NotImplemented
        return str(self) == str(other)

    def _key(self, other):
        if not isinstance(other, PurePath):
            raise TypeError
        return self.parts, other.parts

    def __lt__(self, other):
        if not isinstance(other, PurePath):
            return NotImplemented
        return self.parts < other.parts

    def __le__(self, other):
        if not isinstance(other, PurePath):
            return NotImplemented
        return self.parts <= other.parts

    def __gt__(self, other):
        if not isinstance(other, PurePath):
            return NotImplemented
        return self.parts > other.parts

    def __ge__(self, other):
        if not isinstance(other, PurePath):
            return NotImplemented
        return self.parts >= other.parts

    def __truediv__(self, key):
        try:
            return self.with_segments(self, key)
        except TypeError:
            return NotImplemented

    def __rtruediv__(self, key):
        try:
            return self.with_segments(key, self)
        except TypeError:
            return NotImplemented

    def joinpath(self, *segments):
        return self.with_segments(self, *segments)

    @property
    def drive(self):
        return ""

    @property
    def root(self):
        return self._split()[0]

    @property
    def anchor(self):
        return self.root

    @property
    def parts(self):
        root, parts = self._split()
        return tuple([root] + parts) if root else tuple(parts)

    @property
    def name(self):
        parts = self._split()[1]
        return parts[-1] if parts else ""

    @property
    def suffix(self):
        name = self.name
        i = name.rfind(".") if name != ".." else -1
        if 0 < i:
            return name[i:]
        return ""

    @property
    def suffixes(self):
        name = self.name
        name = name.lstrip(".")
        return ["." + s for s in name.split(".")[1:]]

    @property
    def stem(self):
        name = self.name
        i = name.rfind(".") if name != ".." else -1
        if 0 < i:
            return name[:i]
        return name

    @property
    def parent(self):
        root, parts = self._split()
        if not parts:
            return self
        return self.with_segments(root + "/".join(parts[:-1]) or ".")

    @property
    def parents(self):
        out = []
        p = self
        while True:
            q = p.parent
            if q == p:
                break
            out.append(q)
            p = q
        return tuple(out)

    def with_name(self, name):
        if not self.name:
            raise ValueError("%r has an empty name" % (self,))
        if not name or "/" in name or name == ".":
            raise ValueError("Invalid name %r" % (name,))
        return self.parent / name if self.parent != self or self.root else self.with_segments(name)

    def with_stem(self, stem):
        return self.with_name(stem + self.suffix)

    def with_suffix(self, suffix):
        if suffix and (not suffix.startswith(".") or suffix == "." or "/" in suffix):
            raise ValueError("Invalid suffix %r" % (suffix,))
        if not self.name:
            raise ValueError("%r has an empty name" % (self,))
        return self.with_name(self.stem + suffix)

    def is_absolute(self):
        return bool(self.root)

    def is_reserved(self):
        return False

    def is_relative_to(self, other):
        other = self.with_segments(other)
        return other == self or other in self.parents

    def relative_to(self, other, *, walk_up=False):
        other = self.with_segments(other)
        if self.is_absolute() != other.is_absolute():
            raise ValueError("%r and %r have different anchors" % (str(self), str(other)))
        mine = self.parts
        theirs = other.parts
        n = 0
        while n < len(mine) and n < len(theirs) and mine[n] == theirs[n]:
            n += 1
        if n < len(theirs):
            if not walk_up:
                raise ValueError("%r is not in the subpath of %r" % (str(self), str(other)))
            if ".." in theirs[n:]:
                raise ValueError("'..' segment in %r cannot be walked" % (str(other),))
        rest = [".."] * (len(theirs) - n) + list(mine[n:])
        return self.with_segments(*rest) if rest else self.with_segments("")

    def match(self, pattern, *, case_sensitive=None):
        pat = self.with_segments(pattern)
        pparts = pat.parts
        mparts = self.parts
        if not pparts:
            raise ValueError("empty pattern")
        if pat.is_absolute():
            if len(pparts) != len(mparts):
                return False
        elif len(pparts) > len(mparts):
            return False
        for a, b in zip(reversed(mparts), reversed(pparts)):
            if not _fnmatch(a, b):
                return False
        return True

    def full_match(self, pattern, *, case_sensitive=None):
        pparts = self.with_segments(pattern).parts
        return _match_parts(list(self.parts), list(pparts))


def _fnmatch(name, pat):
    import fnmatch
    return fnmatch.fnmatchcase(name, pat)


def _match_parts(names, pats):
    if not pats:
        return not names
    if pats[0] == "**":
        for i in range(len(names) + 1):
            if _match_parts(names[i:], pats[1:]):
                return True
        return False
    if not names:
        return False
    return _fnmatch(names[0], pats[0]) and _match_parts(names[1:], pats[1:])


class PurePosixPath(PurePath):
    __slots__ = ()


class Path(PurePath):
    __slots__ = ()

    def __new__(cls, *args, **kwargs):
        if cls is Path:
            cls = PosixPath
        return object.__new__(cls)

    @classmethod
    def cwd(cls):
        return cls(os.getcwd())

    @classmethod
    def home(cls):
        return cls(_p.expanduser("~"))

    def absolute(self):
        if self.is_absolute():
            return self
        return self.with_segments(os.getcwd(), self)

    def resolve(self, strict=False):
        r = self.with_segments(_p.normpath(str(self.absolute())))
        if strict and not r.exists():
            raise FileNotFoundError(2, "No such file or directory", str(self))
        return r

    def expanduser(self):
        return self.with_segments(_p.expanduser(str(self)))

    def stat(self, *, follow_symlinks=True):
        return os.stat(self)

    lstat = stat

    def exists(self, *, follow_symlinks=True):
        return _p.exists(self)

    def is_dir(self, *, follow_symlinks=True):
        return _p.isdir(self)

    def is_file(self, *, follow_symlinks=True):
        return _p.isfile(self)

    def is_symlink(self):
        return False

    def is_mount(self):
        return _p.ismount(self)

    def samefile(self, other):
        return _p.samefile(self, other)

    def open(self, mode="r", buffering=-1, encoding=None, errors=None, newline=None):
        return _io.open(self, mode, buffering, encoding, errors, newline)

    def read_text(self, encoding=None, errors=None, newline=None):
        with self.open("r", encoding=encoding, errors=errors, newline=newline) as f:
            return f.read()

    def read_bytes(self):
        with self.open("rb") as f:
            return f.read()

    def write_text(self, data, encoding=None, errors=None, newline=None):
        if not isinstance(data, str):
            raise TypeError("data must be str, not %s" % type(data).__name__)
        with self.open("w", encoding=encoding, errors=errors, newline=newline) as f:
            return f.write(data)

    def write_bytes(self, data):
        data = bytes(memoryview(data)) if not isinstance(data, bytes) else data
        with self.open("wb") as f:
            return f.write(data)

    def iterdir(self):
        for name in os.listdir(self):
            yield self / name

    def glob(self, pattern, *, case_sensitive=None, recurse_symlinks=False):
        pats = [x for x in pattern.split("/") if x and x != "."]
        if not pats:
            raise ValueError("Unacceptable pattern: %r" % (pattern,))
        yield from self._glob(pats)

    def _glob(self, pats):
        head = pats[0]
        rest = pats[1:]
        if head == "**":
            for d in self._walk_dirs():
                if rest:
                    yield from d._glob(rest)
                else:
                    yield d
            return
        if head == "..":
            p = self / ".."
            if rest:
                yield from p._glob(rest)
            else:
                yield p
            return
        if not self.is_dir():
            return
        for name in sorted(os.listdir(self)):
            if _fnmatch(name, head):
                child = self / name
                if rest:
                    if child.is_dir():
                        yield from child._glob(rest)
                else:
                    yield child

    def _walk_dirs(self):
        yield self
        if self.is_dir():
            for name in sorted(os.listdir(self)):
                child = self / name
                if child.is_dir():
                    yield from child._walk_dirs()

    def rglob(self, pattern, *, case_sensitive=None, recurse_symlinks=False):
        return self.glob("**/" + pattern)

    def walk(self, top_down=True, on_error=None, follow_symlinks=False):
        for root, dirs, files in os.walk(self, top_down, on_error):
            yield self.with_segments(root), dirs, files

    def touch(self, mode=0o666, exist_ok=True):
        if self.exists():
            if not exist_ok:
                raise FileExistsError(17, "File exists", str(self))
            return
        with self.open("wb"):
            pass

    def mkdir(self, mode=0o777, parents=False, exist_ok=False):
        try:
            if parents:
                os.makedirs(self, mode, exist_ok)
            else:
                os.mkdir(self, mode)
        except FileExistsError:
            if not exist_ok or not self.is_dir():
                raise

    def unlink(self, missing_ok=False):
        try:
            os.unlink(self)
        except FileNotFoundError:
            if not missing_ok:
                raise

    def rmdir(self):
        os.rmdir(self)

    def rename(self, target):
        os.rename(self, target)
        return self.with_segments(target)

    def replace(self, target):
        os.replace(self, target)
        return self.with_segments(target)

    def owner(self):
        raise UnsupportedOperation("owner() is not supported")

    def group(self):
        raise UnsupportedOperation("group() is not supported")

    def chmod(self, mode, *, follow_symlinks=True):
        pass


class PosixPath(Path, PurePosixPath):
    __slots__ = ()
