# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Operating system services an application has: its files and environment.

Files are the application's own (`FileSystemStorage`); the working
directory starts at the application's home. There are no processes,
users, file descriptors or links.
"""

import _cn1fs
import io as _io
from os import path

name = "posix"
sep = path.sep
altsep = path.altsep
extsep = path.extsep
pathsep = path.pathsep
curdir = path.curdir
pardir = path.pardir
defpath = path.defpath
devnull = path.devnull
linesep = "\n"
SEEK_SET = 0
SEEK_CUR = 1
SEEK_END = 2
F_OK = 0
R_OK = 4
W_OK = 2
X_OK = 1

environ = _cn1fs.environ()
_unset = set()
error = OSError


def getenv(key, default=None):
    if key in environ:
        return environ[key]
    # ParparVM cannot enumerate the environment, so environ may lack a
    # variable that is still readable by name.
    v = None if key in _unset else _cn1fs.getenv(key)
    return default if v is None else v


def putenv(key, value):
    environ[key] = value
    _unset.discard(key)


def unsetenv(key):
    environ.pop(key, None)
    _unset.add(key)


class PathLike:
    def __fspath__(self):
        raise NotImplementedError

    @classmethod
    def __subclasshook__(cls, C):
        return hasattr(C, "__fspath__") or NotImplemented


def fspath(p):
    if isinstance(p, (str, bytes)):
        return p
    f = getattr(type(p), "__fspath__", None)
    if f is None:
        raise TypeError("expected str, bytes or os.PathLike object, not " + type(p).__name__)
    r = f(p)
    if not isinstance(r, (str, bytes)):
        raise TypeError("expected %s.__fspath__() to return str or bytes, not %s"
                        % (type(p).__name__, type(r).__name__))
    return r


def fsencode(filename):
    filename = fspath(filename)
    return filename.encode("utf-8") if isinstance(filename, str) else filename


def fsdecode(filename):
    filename = fspath(filename)
    return filename.decode("utf-8") if isinstance(filename, bytes) else filename


def getcwd():
    return _io._getcwd()


def getcwdb():
    return getcwd().encode("utf-8")


def _abs(p):
    return _io._abspath(_io._fspath(p))


def chdir(p):
    a = _abs(p)
    if not _cn1fs.exists(a):
        raise FileNotFoundError(2, "No such file or directory", fspath(p))
    if not _cn1fs.isdir(a):
        raise NotADirectoryError(20, "Not a directory", fspath(p))
    _io._setcwd(a)


def listdir(p="."):
    a = _abs(p)
    if not _cn1fs.exists(a):
        raise FileNotFoundError(2, "No such file or directory", fspath(p))
    if not _cn1fs.isdir(a):
        raise NotADirectoryError(20, "Not a directory", fspath(p))
    names = _cn1fs.listdir(a)
    if isinstance(p, bytes):
        return [n.encode("utf-8") for n in names]
    return names


def mkdir(p, mode=0o777, *, dir_fd=None):
    a = _abs(p)
    if _cn1fs.exists(a):
        raise FileExistsError(17, "File exists", fspath(p))
    parent = a.rsplit("/", 1)[0] or "/"
    if not _cn1fs.isdir(parent):
        raise FileNotFoundError(2, "No such file or directory", fspath(p))
    _cn1fs.mkdir(a)


def makedirs(name, mode=0o777, exist_ok=False):
    a = _abs(name)
    if _cn1fs.exists(a):
        if exist_ok and _cn1fs.isdir(a):
            return
        raise FileExistsError(17, "File exists", fspath(name))
    head = a.rsplit("/", 1)[0] or "/"
    if not _cn1fs.exists(head):
        makedirs(head, mode, True)
    _cn1fs.mkdir(a)


def remove(p, *, dir_fd=None):
    a = _abs(p)
    if not _cn1fs.exists(a):
        raise FileNotFoundError(2, "No such file or directory", fspath(p))
    if _cn1fs.isdir(a):
        raise IsADirectoryError(21, "Is a directory", fspath(p))
    _cn1fs.remove(a)


unlink = remove


def rmdir(p, *, dir_fd=None):
    a = _abs(p)
    if not _cn1fs.exists(a):
        raise FileNotFoundError(2, "No such file or directory", fspath(p))
    if not _cn1fs.isdir(a):
        raise NotADirectoryError(20, "Not a directory", fspath(p))
    if _cn1fs.listdir(a):
        raise OSError(39, "Directory not empty", fspath(p))
    _cn1fs.remove(a)


def removedirs(name):
    rmdir(name)
    head, tail = path.split(name)
    if not tail:
        head, tail = path.split(head)
    while head and tail:
        try:
            rmdir(head)
        except OSError:
            break
        head, tail = path.split(head)


def rename(src, dst, *, src_dir_fd=None, dst_dir_fd=None):
    a = _abs(src)
    b = _abs(dst)
    if not _cn1fs.exists(a):
        raise FileNotFoundError(2, "No such file or directory", fspath(src), None, fspath(dst))
    if _cn1fs.isdir(b):
        raise IsADirectoryError(21, "Is a directory", fspath(src), None, fspath(dst))
    _cn1fs.rename(a, b)


replace = rename


def renames(old, new):
    head, tail = path.split(new)
    if head and tail and not path.exists(head):
        makedirs(head)
    rename(old, new)
    head, tail = path.split(old)
    if head and tail:
        try:
            removedirs(head)
        except OSError:
            pass


class stat_result(tuple):
    n_sequence_fields = 10

    def __new__(cls, seq):
        return tuple.__new__(cls, seq)

    st_mode = property(lambda self: self[0])
    st_ino = property(lambda self: self[1])
    st_dev = property(lambda self: self[2])
    st_nlink = property(lambda self: self[3])
    st_uid = property(lambda self: self[4])
    st_gid = property(lambda self: self[5])
    st_size = property(lambda self: self[6])
    st_atime = property(lambda self: self[10])
    st_mtime = property(lambda self: self[10])
    st_ctime = property(lambda self: self[10])

    def __repr__(self):
        return ("os.stat_result(st_mode=%d, st_ino=0, st_dev=0, st_nlink=1, st_uid=0, st_gid=0, "
                "st_size=%d, st_atime=%d, st_mtime=%d, st_ctime=%d)"
                % (self[0], self[6], self[7], self[8], self[9]))


def stat(p, *, dir_fd=None, follow_symlinks=True):
    a = _abs(p)
    if not _cn1fs.exists(a):
        raise FileNotFoundError(2, "No such file or directory", fspath(p))
    d = _cn1fs.isdir(a)
    t = _cn1fs.mtime(a)
    size = 0 if d else _cn1fs.size(a)
    it = int(t)
    return stat_result((0o40755 if d else 0o100644, 0, 0, 1, 0, 0, size, it, it, it, t))


lstat = stat


def access(p, mode, *, dir_fd=None, effective_ids=False, follow_symlinks=True):
    return path.exists(p)


class DirEntry:
    def __init__(self, parent, name):
        self.name = name
        self.path = path.join(parent, name)

    def is_dir(self, *, follow_symlinks=True):
        return path.isdir(self.path)

    def is_file(self, *, follow_symlinks=True):
        return path.isfile(self.path)

    def is_symlink(self):
        return False

    def stat(self, *, follow_symlinks=True):
        return stat(self.path)

    def inode(self):
        return 0

    def __fspath__(self):
        return self.path

    def __repr__(self):
        return "<DirEntry %r>" % self.name


class _ScandirIterator:
    def __init__(self, entries):
        self._it = iter(entries)

    def __iter__(self):
        return self

    def __next__(self):
        return next(self._it)

    def __enter__(self):
        return self

    def __exit__(self, *a):
        self._it = iter(())

    def close(self):
        self._it = iter(())


def scandir(p="."):
    parent = _io._fspath(p)
    return _ScandirIterator([DirEntry(parent, n) for n in listdir(parent)])


def walk(top, topdown=True, onerror=None, followlinks=False):
    top = fspath(top)
    try:
        names = listdir(top)
    except OSError as err:
        if onerror is not None:
            onerror(err)
        return
    dirs = []
    nondirs = []
    for n in names:
        if path.isdir(path.join(top, n)):
            dirs.append(n)
        else:
            nondirs.append(n)
    if topdown:
        yield top, dirs, nondirs
        for d in dirs:
            yield from walk(path.join(top, d), topdown, onerror, followlinks)
    else:
        for d in dirs:
            yield from walk(path.join(top, d), topdown, onerror, followlinks)
        yield top, dirs, nondirs


def cpu_count():
    return 1


def process_cpu_count():
    return 1


def getpid():
    return 1


def get_terminal_size(fd=1):
    return terminal_size((80, 24))


class terminal_size(tuple):
    columns = property(lambda self: self[0])
    lines = property(lambda self: self[1])


def urandom(n):
    import random as _random
    return _random.SystemRandom().randbytes(n)


def strerror(code):
    return {2: "No such file or directory", 17: "File exists", 20: "Not a directory",
            21: "Is a directory", 39: "Directory not empty"}.get(code, "Unknown error %d" % code)
