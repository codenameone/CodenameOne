# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Create portable serialized representations of Python objects.

The stream is the one CPython writes: `dumps` produces the same bytes for
the same value under protocols 0 to 5 (the default is 5), and `loads` reads
every opcode CPython emits, including the framed protocol 4 and 5 streams.

Supported values: None, bool, int (of any size), float, str, bytes,
bytearray, tuple, list, dict, set, frozenset, complex, range, classes and
functions by name, and instances of user classes through `__reduce_ex__`,
`__reduce__`, `__getnewargs__`, `__getnewargs_ex__`, `__getstate__` and
`__setstate__`, `__slots__`, dataclasses and enums. Shared and recursive
references are kept through the memo. `persistent_id` / `persistent_load`,
`reducer_override`, `dispatch_table` and `copyreg` extension codes work.

Not supported: out of band buffers (`PickleBuffer`, `buffer_callback`,
`buffers`) and `__reduce_ex__` of a subclass of a built-in container under
protocols 0 and 1. A class or function is found on load through
`sys.modules`, so its module has to be part of the program: a module the
program never imports cannot be named by a pickle (the program is compiled
ahead of time and nothing imports a module by a computed name).
`PickleError` subclasses are raised as in CPython; the messages carry no
"when serializing" notes.
"""

import sys
import builtins
import io
import copyreg
import functools
from struct import pack, unpack

__all__ = ["PickleError", "PicklingError", "UnpicklingError", "Pickler", "Unpickler", "dump", "dumps",
           "load", "loads", "HIGHEST_PROTOCOL", "DEFAULT_PROTOCOL", "format_version", "compatible_formats",
           "encode_long", "decode_long", "whichmodule"]

format_version = "4.0"
compatible_formats = ["1.0", "1.1", "1.2", "1.3", "2.0", "3.0", "4.0", "5.0"]
HIGHEST_PROTOCOL = 5
DEFAULT_PROTOCOL = 5


class PickleError(Exception):
    pass


class PicklingError(PickleError):
    pass


class UnpicklingError(PickleError):
    pass


PickleError.__module__ = "pickle"
PicklingError.__module__ = "pickle"
UnpicklingError.__module__ = "pickle"

bytes_types = (bytes, bytearray)

MARK = b'('
STOP = b'.'
POP = b'0'
POP_MARK = b'1'
DUP = b'2'
FLOAT = b'F'
INT = b'I'
BININT = b'J'
BININT1 = b'K'
LONG = b'L'
BININT2 = b'M'
NONE = b'N'
PERSID = b'P'
BINPERSID = b'Q'
REDUCE = b'R'
STRING = b'S'
BINSTRING = b'T'
SHORT_BINSTRING = b'U'
UNICODE = b'V'
BINUNICODE = b'X'
APPEND = b'a'
BUILD = b'b'
GLOBAL = b'c'
DICT = b'd'
EMPTY_DICT = b'}'
APPENDS = b'e'
GET = b'g'
BINGET = b'h'
INST = b'i'
LONG_BINGET = b'j'
LIST = b'l'
EMPTY_LIST = b']'
OBJ = b'o'
PUT = b'p'
BINPUT = b'q'
LONG_BINPUT = b'r'
SETITEM = b's'
TUPLE = b't'
EMPTY_TUPLE = b')'
SETITEMS = b'u'
BINFLOAT = b'G'
TRUE = b'I01\n'
FALSE = b'I00\n'
PROTO = b'\x80'
NEWOBJ = b'\x81'
EXT1 = b'\x82'
EXT2 = b'\x83'
EXT4 = b'\x84'
TUPLE1 = b'\x85'
TUPLE2 = b'\x86'
TUPLE3 = b'\x87'
NEWTRUE = b'\x88'
NEWFALSE = b'\x89'
LONG1 = b'\x8a'
LONG4 = b'\x8b'
BINBYTES = b'B'
SHORT_BINBYTES = b'C'
SHORT_BINUNICODE = b'\x8c'
BINUNICODE8 = b'\x8d'
BINBYTES8 = b'\x8e'
EMPTY_SET = b'\x8f'
ADDITEMS = b'\x90'
FROZENSET = b'\x91'
NEWOBJ_EX = b'\x92'
STACK_GLOBAL = b'\x93'
MEMOIZE = b'\x94'
FRAME = b'\x95'
BYTEARRAY8 = b'\x96'
NEXT_BUFFER = b'\x97'
READONLY_BUFFER = b'\x98'

_tuplesize2code = [EMPTY_TUPLE, TUPLE1, TUPLE2, TUPLE3]

_NoValue = object()

# What fix_imports maps for protocols below 3: the Python 2 module names.
_EXCEPTION_NAMES = (
    "ArithmeticError AssertionError AttributeError BaseException BufferError BytesWarning DeprecationWarning "
    "EOFError EnvironmentError Exception FloatingPointError FutureWarning GeneratorExit IOError ImportError "
    "ImportWarning IndentationError IndexError KeyError KeyboardInterrupt LookupError MemoryError NameError "
    "NotImplementedError OSError OverflowError PendingDeprecationWarning ReferenceError RuntimeError "
    "RuntimeWarning StopIteration SyntaxError SyntaxWarning SystemError SystemExit TabError TypeError "
    "UnboundLocalError UnicodeDecodeError UnicodeEncodeError UnicodeError UnicodeTranslateError "
    "UnicodeWarning UserWarning ValueError Warning ZeroDivisionError").split()
_OS_ERRORS = ("BrokenPipeError ChildProcessError ConnectionAbortedError ConnectionError ConnectionRefusedError "
              "ConnectionResetError FileExistsError FileNotFoundError InterruptedError IsADirectoryError "
              "NotADirectoryError PermissionError ProcessLookupError TimeoutError").split()
_NAME_FIX = {"range": "xrange", "chr": "unichr", "str": "unicode", "int": "long"}
_NAME_FIX_MODULE = {"zip": "itertools:izip", "map": "itertools:imap", "filter": "itertools:ifilter"}
_IMPORT_FIX = {"builtins": "__builtin__", "copyreg": "copy_reg", "queue": "Queue", "socketserver": "SocketServer",
               "configparser": "ConfigParser", "reprlib": "repr", "_thread": "thread"}
_UNFIX_IMPORT = {}
for _k in _IMPORT_FIX:
    _UNFIX_IMPORT[_IMPORT_FIX[_k]] = _k
_UNFIX_NAME = {}
for _k in _NAME_FIX:
    _UNFIX_NAME[("__builtin__", _NAME_FIX[_k])] = ("builtins", _k)
_UNFIX_NAME[("itertools", "izip")] = ("builtins", "zip")
_UNFIX_NAME[("itertools", "imap")] = ("builtins", "map")
_UNFIX_NAME[("itertools", "ifilter")] = ("builtins", "filter")
_UNFIX_NAME[("__builtin__", "basestring")] = ("builtins", "str")
_UNFIX_NAME[("exceptions", "StandardError")] = ("builtins", "Exception")
for _k in _EXCEPTION_NAMES:
    _UNFIX_NAME[("exceptions", _k)] = ("builtins", _k)


def _fix_global(module_name, name):
    """The Python 2 name of a builtins/stdlib global, for protocols 0 to 2."""
    if module_name == "builtins":
        if name in _NAME_FIX:
            return "__builtin__", _NAME_FIX[name]
        if name in _NAME_FIX_MODULE:
            m, n = _NAME_FIX_MODULE[name].split(":")
            return m, n
        if name in _EXCEPTION_NAMES:
            return "exceptions", name
        if name in _OS_ERRORS:
            return "exceptions", "OSError"
        if name == "ModuleNotFoundError":
            return "exceptions", "ImportError"
    if module_name in _IMPORT_FIX:
        return _IMPORT_FIX[module_name], name
    return module_name, name


def _codecs_encode(obj, encoding="utf-8", errors="strict"):
    """What `_codecs.encode` is for a str written by protocols 0 to 2."""
    return obj.encode(encoding, errors)


_codecs_encode.__module__ = "_codecs"
_codecs_encode.__name__ = "encode"
_codecs_encode.__qualname__ = "encode"


def encode_long(x):
    """Encode a long to a two's complement little-endian binary string.

    0 is a special case, returning an empty string, to save a byte in the
    LONG1 pickling context."""
    if x == 0:
        return b''
    nbytes = (x.bit_length() >> 3) + 1
    result = x.to_bytes(nbytes, byteorder='little', signed=True)
    if x < 0 and nbytes > 1:
        if result[-1] == 0xff and (result[-2] & 0x80) != 0:
            result = result[:-1]
    return result


def decode_long(data):
    """Decode a long from a two's complement little-endian binary string."""
    return int.from_bytes(data, byteorder='little', signed=True)


def _getattribute(obj, dotted_path):
    for subpath in dotted_path:
        if subpath == '<locals>':
            raise AttributeError("Can't get local attribute")
        obj = getattr(obj, subpath)
    return obj


def whichmodule(obj, name):
    """Find the module an object belongs to."""
    dotted_path = name.split('.')
    module_name = getattr(obj, '__module__', None)
    if '<locals>' in dotted_path:
        raise PicklingError("Can't pickle local object %r" % (obj,))
    if module_name is None:
        builtins = sys.modules.get("builtins")
        if builtins is not None:
            try:
                if _getattribute(builtins, dotted_path) is obj:
                    return "builtins"
            except AttributeError:
                pass
        for module_name, module in list(sys.modules.items()):
            if module_name == '__main__' or module is None:
                continue
            try:
                if _getattribute(module, dotted_path) is obj:
                    return module_name
            except AttributeError:
                pass
        module_name = '__main__'
    module = sys.modules.get(module_name)
    if module is None:
        raise PicklingError("Can't pickle %r: No module named %r" % (obj, module_name))
    try:
        if _getattribute(module, dotted_path) is obj:
            return module_name
    except AttributeError:
        raise PicklingError("Can't pickle %r: it's not found as %s.%s" % (obj, module_name, name))
    raise PicklingError("Can't pickle %r: it's not the same object as %s.%s" % (obj, module_name, name))


def _has_dict(cls):
    """Whether instances of cls have a `__dict__` of their own (not only `__slots__`)."""
    for c in cls.__mro__:
        if c is object:
            continue
        if "__slots__" not in c.__dict__:
            return True
        slots = c.__dict__["__slots__"]
        if slots == "__dict__" or (not isinstance(slots, str) and "__dict__" in slots):
            return True
    return False


def _T(obj):
    cls = type(obj)
    module = cls.__module__
    if module is None or module == 'builtins' or module == '__main__':
        return cls.__qualname__
    return '%s.%s' % (module, cls.__qualname__)


def _batched(items, n):
    out = []
    cur = []
    for x in items:
        cur.append(x)
        if len(cur) == n:
            out.append(cur)
            cur = []
    if cur:
        out.append(cur)
    return out


def _raw_unicode_escape(s):
    out = bytearray()
    for ch in s:
        o = ord(ch)
        if o < 256:
            out.append(o)
        elif o < 0x10000:
            out += ("\\u%04x" % o).encode("ascii")
        else:
            out += ("\\U%08x" % o).encode("ascii")
    return bytes(out)


def _raw_unicode_unescape(data):
    s = data.decode("latin-1")
    out = []
    i = 0
    n = len(s)
    while i < n:
        ch = s[i]
        if ch == "\\" and i + 1 < n and (s[i + 1] == "u" or s[i + 1] == "U"):
            width = 4 if s[i + 1] == "u" else 8
            digits = s[i + 2:i + 2 + width]
            if len(digits) == width:
                try:
                    out.append(chr(int(digits, 16)))
                    i += 2 + width
                    continue
                except ValueError:
                    pass
            raise UnpicklingError("truncated \\uXXXX escape")
        out.append(ch)
        i += 1
    return "".join(out)


def _unescape_string(raw):
    """Python 2 repr() string literal escapes, as 'S' writes them."""
    out = bytearray()
    i = 0
    n = len(raw)
    simple = {ord("n"): 10, ord("r"): 13, ord("t"): 9, ord("a"): 7, ord("b"): 8, ord("f"): 12, ord("v"): 11,
              ord("\\"): 92, ord("'"): 39, ord('"'): 34}
    while i < n:
        c = raw[i]
        if c != 92:
            out.append(c)
            i += 1
            continue
        i += 1
        if i >= n:
            raise ValueError("Trailing \\ in string")
        c = raw[i]
        if c in simple:
            out.append(simple[c])
            i += 1
        elif c == ord("x"):
            out.append(int(raw[i + 1:i + 3].decode("ascii"), 16))
            i += 3
        elif 48 <= c <= 55:
            j = i
            while j < n and j < i + 3 and 48 <= raw[j] <= 55:
                j += 1
            out.append(int(raw[i:j].decode("ascii"), 8) & 255)
            i = j
        else:
            out.append(92)
            out.append(c)
            i += 1
    return bytes(out)


class _Stop(Exception):
    def __init__(self, value):
        Exception.__init__(self)
        self.value = value


class _Framer:
    _FRAME_SIZE_MIN = 4
    _FRAME_SIZE_TARGET = 64 * 1024

    def __init__(self, file_write):
        self.file_write = file_write
        self.current_frame = None

    def start_framing(self):
        self.current_frame = bytearray()

    def end_framing(self):
        if self.current_frame is not None and len(self.current_frame) > 0:
            self.commit_frame(True)
            self.current_frame = None

    def commit_frame(self, force=False):
        f = self.current_frame
        if f is not None:
            if len(f) >= self._FRAME_SIZE_TARGET or force:
                data = bytes(f)
                if len(data) >= self._FRAME_SIZE_MIN:
                    self.file_write(FRAME + pack("<Q", len(data)))
                self.file_write(data)
                self.current_frame = bytearray()

    def write(self, data):
        if self.current_frame is not None:
            self.current_frame += data
        else:
            return self.file_write(data)

    def write_large_bytes(self, header, payload):
        if self.current_frame is not None:
            self.commit_frame(True)
        self.file_write(header)
        self.file_write(payload)


def _is_type_named(t, *names):
    return t.__name__ in names


class Pickler:
    """This takes a binary file for writing a pickle data stream.

    The optional *protocol* argument tells the pickler to use the given
    protocol; supported protocols are 0 to 5. The default is 5. A negative
    protocol selects the highest one. The *file* needs a write() method
    that accepts one bytes argument."""

    _BATCHSIZE = 1000

    def __init__(self, file, protocol=None, *, fix_imports=True, buffer_callback=None):
        if protocol is None:
            protocol = DEFAULT_PROTOCOL
        if protocol < 0:
            protocol = HIGHEST_PROTOCOL
        elif not 0 <= protocol <= HIGHEST_PROTOCOL:
            raise ValueError("pickle protocol must be <= %d" % HIGHEST_PROTOCOL)
        if buffer_callback is not None:
            raise ValueError("buffer_callback is not supported")
        try:
            self._file_write = file.write
        except AttributeError:
            raise TypeError("file must have a 'write' attribute")
        self.framer = _Framer(self._file_write)
        self.write = self.framer.write
        self._write_large_bytes = self.framer.write_large_bytes
        self.memo = {}
        self.proto = int(protocol)
        self.bin = protocol >= 1
        self.fast = 0
        self.fix_imports = fix_imports and protocol < 3

    def clear_memo(self):
        self.memo.clear()

    def dump(self, obj):
        """Write a pickled representation of obj to the open file."""
        if self.proto >= 2:
            self.write(PROTO + pack("<B", self.proto))
        if self.proto >= 4:
            self.framer.start_framing()
        self.save(obj)
        self.write(STOP)
        self.framer.end_framing()

    def memoize(self, obj):
        if self.fast:
            return
        idx = len(self.memo)
        self.write(self.put(idx))
        self.memo[id(obj)] = (idx, obj)

    def put(self, idx):
        if self.proto >= 4:
            return MEMOIZE
        elif self.bin:
            if idx < 256:
                return BINPUT + pack("<B", idx)
            else:
                return LONG_BINPUT + pack("<I", idx)
        else:
            return PUT + repr(idx).encode("ascii") + b'\n'

    def get(self, i):
        if self.bin:
            if i < 256:
                return BINGET + pack("<B", i)
            else:
                return LONG_BINGET + pack("<I", i)
        return GET + repr(i).encode("ascii") + b'\n'

    def persistent_id(self, obj):
        return None

    def save(self, obj, save_persistent_id=True):
        self.framer.commit_frame()
        if save_persistent_id:
            pid = self.persistent_id(obj)
            if pid is not None:
                self.save_pers(pid)
                return
        x = self.memo.get(id(obj))
        if x is not None:
            self.write(self.get(x[0]))
            return
        rv = NotImplemented
        reduce = getattr(self, "reducer_override", _NoValue)
        if reduce is not _NoValue:
            rv = reduce(obj)
        if rv is NotImplemented:
            t = type(obj)
            if t is _NewRef:
                location = obj.location()
                self._write_global(location[0], location[1], obj)
                return
            if self._save_exact(t, obj):
                return
            table = getattr(self, 'dispatch_table', copyreg.dispatch_table)
            reduce = table.get(t, _NoValue)
            if reduce is not _NoValue:
                rv = reduce(obj)
            else:
                if isinstance(obj, type):
                    self.save_global(obj)
                    return
                rv = self._reduce_object(obj)
        if isinstance(rv, str):
            self.save_global(obj, rv)
            return
        if not isinstance(rv, tuple):
            raise PicklingError('__reduce__ must return a string or tuple, not %s' % _T(rv))
        n = len(rv)
        if not (2 <= n <= 6):
            raise PicklingError("tuple returned by __reduce__ must contain 2 through 6 elements")
        rv = rv + (None,) * (6 - n)
        self.save_reduce(rv[0], rv[1], rv[2], rv[3], rv[4], rv[5], obj)

    def _save_exact(self, t, obj):
        """The types written by value; a subclass of one is reduced instead."""
        if obj is None:
            self.write(NONE)
        elif t is bool:
            self.save_bool(obj)
        elif t is int:
            self.save_long(obj)
        elif t is float:
            self.save_float(obj)
        elif t is str:
            self.save_str(obj)
        elif t is bytes:
            self.save_bytes(obj)
        elif t is bytearray:
            self.save_bytearray(obj)
        elif t is tuple:
            self.save_tuple(obj)
        elif t is list:
            self.save_list(obj)
        elif t is dict:
            self.save_dict(obj)
        elif t is set:
            self.save_set(obj)
        elif t is frozenset:
            self.save_frozenset(obj)
        elif t is type:
            self.save_type(obj)
        else:
            name = t.__name__
            if name == "function" or name == "builtin_function_or_method":
                self.save_global(obj)
            else:
                return False
        return True

    def _reduce_object(self, obj):
        """What `__reduce_ex__(proto)` answers for obj."""
        t = type(obj)
        # A class's own __reduce_ex__ or __reduce__ wins; object's default
        # (copyreg._reduce_ex) comes after the built-in kinds below.
        if getattr(t, "__reduce_ex__", None) is not object.__reduce_ex__:
            return obj.__reduce_ex__(self.proto)
        if getattr(t, "__reduce__", None) is not object.__reduce__:
            return obj.__reduce__()
        if isinstance(obj, BaseException):
            state = {}
            extra = getattr(obj, "__dict__", None)
            if extra:
                for k in extra:
                    if k != "args":
                        state[k] = extra[k]
            if state:
                return (t, obj.args, state)
            return (t, obj.args)
        name = t.__name__
        if t is complex:
            return (complex, (obj.real, obj.imag))
        if t is range:
            # The runtime's range has no start/stop/step attributes; its repr is exact.
            bounds = [int(p) for p in repr(obj)[6:-1].split(", ")]
            if len(bounds) == 2:
                bounds.append(1)
            return (range, tuple(bounds))
        if t is slice:
            return (slice, (obj.start, obj.stop, obj.step))
        if name == "method":
            return (getattr, (obj.__self__, obj.__func__.__name__))
        if name == "module" or name == "generator" or name == "coroutine":
            raise PicklingError("cannot pickle %r object" % name)
        return copyreg._reduce_ex(obj, self.proto)

    def save_pers(self, pid):
        if self.bin:
            self.save(pid, False)
            self.write(BINPERSID)
        else:
            try:
                self.write(PERSID + str(pid).encode("ascii") + b'\n')
            except UnicodeEncodeError:
                raise PicklingError("persistent IDs in protocol 0 must be ASCII strings")

    def save_reduce(self, func, args, state=None, listitems=None, dictitems=None, state_setter=None, obj=None):
        if not callable(func):
            raise PicklingError("first item of the tuple returned by __reduce__ must be callable, not %s" % _T(func))
        if not isinstance(args, tuple):
            raise PicklingError("second item of the tuple returned by __reduce__ must be a tuple, not %s" % _T(args))
        save = self.save
        write = self.write
        func_name = getattr(func, "__name__", "")
        if self.proto >= 2 and func_name == "__newobj_ex__":
            cls = args[0]
            cargs = args[1]
            kwargs = args[2]
            if obj is not None and cls is not obj.__class__:
                raise PicklingError("first argument to __newobj_ex__() must be %r, not %r" % (obj.__class__, cls))
            if self.proto >= 4:
                save(cls)
                save(cargs)
                save(kwargs)
                write(NEWOBJ_EX)
            else:
                save(_NewObjExPartial(cls, cargs, kwargs))
                save(())
                write(REDUCE)
        elif self.proto >= 2 and func_name == "__newobj__":
            cls = args[0]
            if obj is not None and cls is not obj.__class__:
                raise PicklingError("first argument to __newobj__() must be %r, not %r" % (obj.__class__, cls))
            args = args[1:]
            save(cls)
            save(args)
            write(NEWOBJ)
        else:
            save(func)
            save(args)
            write(REDUCE)
        if obj is not None:
            if id(obj) in self.memo:
                write(POP + self.get(self.memo[id(obj)][0]))
            else:
                self.memoize(obj)
        if listitems is not None:
            self._batch_appends(listitems, obj)
        if dictitems is not None:
            self._batch_setitems(dictitems, obj)
        if state is not None:
            if state_setter is None:
                save(state)
                write(BUILD)
            else:
                save(state_setter)
                save(obj)
                save(state)
                write(TUPLE2)
                write(REDUCE)
                write(POP)

    def save_bool(self, obj):
        if self.proto >= 2:
            self.write(NEWTRUE if obj else NEWFALSE)
        else:
            self.write(TRUE if obj else FALSE)

    def save_long(self, obj):
        if self.bin:
            if obj >= 0:
                if obj <= 0xff:
                    self.write(BININT1 + pack("<B", obj))
                    return
                if obj <= 0xffff:
                    self.write(BININT2 + pack("<H", obj))
                    return
            if -0x80000000 <= obj <= 0x7fffffff:
                self.write(BININT + pack("<i", obj))
                return
        if self.proto >= 2:
            encoded = encode_long(obj)
            n = len(encoded)
            if n < 256:
                self.write(LONG1 + pack("<B", n) + encoded)
            else:
                self.write(LONG4 + pack("<i", n) + encoded)
            return
        if -0x80000000 <= obj <= 0x7fffffff:
            self.write(INT + repr(obj).encode("ascii") + b'\n')
        else:
            self.write(LONG + repr(obj).encode("ascii") + b'L\n')

    def save_float(self, obj):
        if self.bin:
            self.write(BINFLOAT + pack('>d', obj))
        else:
            self.write(FLOAT + repr(obj).encode("ascii") + b'\n')

    def _save_bytes_no_memo(self, obj):
        n = len(obj)
        if n <= 0xff:
            self.write(SHORT_BINBYTES + pack("<B", n) + obj)
        elif n > 0xffffffff and self.proto >= 4:
            self._write_large_bytes(BINBYTES8 + pack("<Q", n), obj)
        elif n >= self.framer._FRAME_SIZE_TARGET:
            self._write_large_bytes(BINBYTES + pack("<I", n), obj)
        else:
            self.write(BINBYTES + pack("<I", n) + obj)

    def save_bytes(self, obj):
        if self.proto < 3:
            if not obj:
                self.save_reduce(bytes, (), None, None, None, None, obj)
            else:
                self.save_reduce(_codecs_encode, (obj.decode('latin-1'), 'latin1'), None, None, None, None, obj)
            return
        self._save_bytes_no_memo(obj)
        self.memoize(obj)

    def _save_bytearray_no_memo(self, obj):
        n = len(obj)
        if n >= self.framer._FRAME_SIZE_TARGET:
            self._write_large_bytes(BYTEARRAY8 + pack("<Q", n), bytes(obj))
        else:
            self.write(BYTEARRAY8 + pack("<Q", n) + bytes(obj))

    def save_bytearray(self, obj):
        if self.proto < 5:
            if not obj:
                self.save_reduce(bytearray, (), None, None, None, None, obj)
            else:
                self.save_reduce(bytearray, (bytes(obj),), None, None, None, None, obj)
            return
        self._save_bytearray_no_memo(obj)
        self.memoize(obj)

    def save_str(self, obj):
        if self.bin:
            encoded = obj.encode('utf-8')
            n = len(encoded)
            if n <= 0xff and self.proto >= 4:
                self.write(SHORT_BINUNICODE + pack("<B", n) + encoded)
            elif n > 0xffffffff and self.proto >= 4:
                self._write_large_bytes(BINUNICODE8 + pack("<Q", n), encoded)
            elif n >= self.framer._FRAME_SIZE_TARGET:
                self._write_large_bytes(BINUNICODE + pack("<I", n), encoded)
            else:
                self.write(BINUNICODE + pack("<I", n) + encoded)
        else:
            tmp = obj.replace("\\", "\\u005c")
            tmp = tmp.replace("\0", "\\u0000")
            tmp = tmp.replace("\n", "\\u000a")
            tmp = tmp.replace("\r", "\\u000d")
            tmp = tmp.replace("\x1a", "\\u001a")
            self.write(UNICODE + _raw_unicode_escape(tmp) + b'\n')
        self.memoize(obj)

    def save_tuple(self, obj):
        if not obj:
            if self.bin:
                self.write(EMPTY_TUPLE)
            else:
                self.write(MARK + TUPLE)
            return
        n = len(obj)
        save = self.save
        memo = self.memo
        if n <= 3 and self.proto >= 2:
            for element in obj:
                save(element)
            if id(obj) in memo:
                get = self.get(memo[id(obj)][0])
                self.write(POP * n + get)
            else:
                self.write(_tuplesize2code[n])
                self.memoize(obj)
            return
        write = self.write
        write(MARK)
        for element in obj:
            save(element)
        if id(obj) in memo:
            get = self.get(memo[id(obj)][0])
            if self.bin:
                write(POP_MARK + get)
            else:
                write(POP * (n + 1) + get)
            return
        write(TUPLE)
        self.memoize(obj)

    def save_list(self, obj):
        if self.bin:
            self.write(EMPTY_LIST)
        else:
            self.write(MARK + LIST)
        self.memoize(obj)
        self._batch_appends(obj, obj)

    def _batch_appends(self, items, obj):
        save = self.save
        write = self.write
        if not self.bin:
            for x in items:
                save(x)
                write(APPEND)
            return
        for batch in _batched(items, self._BATCHSIZE):
            if len(batch) != 1:
                write(MARK)
                for x in batch:
                    save(x)
                write(APPENDS)
            else:
                save(batch[0])
                write(APPEND)

    def save_dict(self, obj):
        if self.bin:
            self.write(EMPTY_DICT)
        else:
            self.write(MARK + DICT)
        self.memoize(obj)
        self._batch_setitems(obj.items(), obj)

    def _batch_setitems(self, items, obj):
        save = self.save
        write = self.write
        if not self.bin:
            for k, v in items:
                save(k)
                save(v)
                write(SETITEM)
            return
        for batch in _batched(items, self._BATCHSIZE):
            if len(batch) != 1:
                write(MARK)
                for kv in batch:
                    save(kv[0])
                    save(kv[1])
                write(SETITEMS)
            else:
                save(batch[0][0])
                save(batch[0][1])
                write(SETITEM)

    def save_set(self, obj):
        save = self.save
        write = self.write
        if self.proto < 4:
            self.save_reduce(set, (list(obj),), None, None, None, None, obj)
            return
        write(EMPTY_SET)
        self.memoize(obj)
        for batch in _batched(obj, self._BATCHSIZE):
            write(MARK)
            for item in batch:
                save(item)
            write(ADDITEMS)

    def save_frozenset(self, obj):
        save = self.save
        write = self.write
        if self.proto < 4:
            self.save_reduce(frozenset, (list(obj),), None, None, None, None, obj)
            return
        write(MARK)
        for item in obj:
            save(item)
        if id(obj) in self.memo:
            write(POP_MARK + self.get(self.memo[id(obj)][0]))
            return
        write(FROZENSET)
        self.memoize(obj)

    def save_global(self, obj, name=None):
        write = self.write
        if obj is _codecs_encode:
            module_name = "_codecs"
            name = "encode"
        else:
            if name is None:
                name = getattr(obj, '__qualname__', None)
                if name is None:
                    name = obj.__name__
            module_name = whichmodule(obj, name)
            module = sys.modules.get(module_name)
            if module is None:
                raise PicklingError("Can't pickle %r: No module named %r" % (obj, module_name))
            try:
                obj2 = _getattribute(module, name.split('.'))
            except AttributeError:
                raise PicklingError("Can't pickle %r: it's not found as %s.%s" % (obj, module_name, name))
            if obj2 is not obj:
                raise PicklingError("Can't pickle %r: it's not the same object as %s.%s" % (obj, module_name, name))
        self._write_global(module_name, name, obj)

    def _write_global(self, module_name, name, obj):
        write = self.write
        if self.proto >= 2:
            code = copyreg._extension_registry.get((module_name, name), _NoValue)
            if code is not _NoValue:
                if code <= 0xff:
                    write(EXT1 + pack("<B", code))
                elif code <= 0xffff:
                    write(EXT2 + pack("<H", code))
                else:
                    write(EXT4 + pack("<i", code))
                return
        if self.proto >= 4:
            self.save(module_name)
            self.save(name)
            write(STACK_GLOBAL)
        elif '.' in name:
            dotted_path = name.split('.')
            name = dotted_path.pop(0)
            for attrname in dotted_path:
                self.save(getattr)
                if self.proto < 2:
                    write(MARK)
            self._save_toplevel_by_name(module_name, name)
            for attrname in dotted_path:
                self.save(attrname)
                if self.proto < 2:
                    write(TUPLE)
                else:
                    write(TUPLE2)
                write(REDUCE)
        else:
            self._save_toplevel_by_name(module_name, name)
        self.memoize(obj)

    def _save_toplevel_by_name(self, module_name, name):
        if self.proto >= 3:
            encoding = "utf-8"
        else:
            if self.fix_imports:
                module_name, name = _fix_global(module_name, name)
            encoding = "ascii"
        try:
            self.write(GLOBAL + module_name.encode(encoding) + b'\n')
        except UnicodeEncodeError:
            raise PicklingError("can't pickle module identifier %r using pickle protocol %d" % (module_name, self.proto))
        try:
            self.write(name.encode(encoding) + b'\n')
        except UnicodeEncodeError:
            raise PicklingError("can't pickle global identifier %r using pickle protocol %d" % (name, self.proto))

    def save_type(self, obj):
        if obj is type(None):
            return self.save_reduce(type, (None,), None, None, None, None, obj)
        elif obj is type(NotImplemented):
            return self.save_reduce(type, (NotImplemented,), None, None, None, None, obj)
        elif obj is type(...):
            return self.save_reduce(type, (...,), None, None, None, None, obj)
        return self.save_global(obj)


class _NewRef:
    """Stands for `cls.__new__`, which CPython pickles as a global named `Owner.__new__`."""

    def __init__(self, cls):
        self.cls = cls

    def location(self):
        for c in self.cls.__mro__:
            if c is not object and "__new__" in c.__dict__:
                return c.__module__, c.__qualname__ + ".__new__"
        return "builtins", "object.__new__"


class _NewObjExPartial:
    """`partial(cls.__new__, cls, *args, **kwargs)`, what protocols 2 and 3 write for `__newobj_ex__`."""

    def __init__(self, cls, args, kwargs):
        self.ref = _NewRef(cls)
        self.cls = cls
        self.args = args
        self.kwargs = kwargs

    def __reduce__(self):
        return (functools.partial, (self.ref,), (self.ref, (self.cls,) + tuple(self.args), self.kwargs or None, None))


class Unpickler:
    """This takes a binary file for reading a pickle data stream.

    The protocol version is detected automatically. The *file* needs read()
    and readline() methods returning bytes."""

    def __init__(self, file, *, fix_imports=True, encoding="ASCII", errors="strict", buffers=None):
        if buffers is not None:
            raise ValueError("buffers is not supported")
        self._file_readline = file.readline
        self._file_read = file.read
        self.memo = {}
        self.encoding = encoding
        self.errors = errors
        self.proto = 0
        self.fix_imports = fix_imports
        self.stack = []
        self.metastack = []

    def read(self, n):
        data = self._file_read(n)
        if len(data) != n:
            raise UnpicklingError("pickle data was truncated")
        return data

    def readline(self):
        data = self._file_readline()
        if not data or data[-1] != 10:
            raise UnpicklingError("pickle data was truncated")
        return data

    def persistent_load(self, pid):
        raise UnpicklingError("unsupported persistent id encountered")

    def pop_mark(self):
        items = self.stack
        self.stack = self.metastack.pop()
        return items

    def load(self):
        """Read a pickled object representation from the open file."""
        self.metastack = []
        self.stack = []
        self.proto = 0
        while True:
            key = self._file_read(1)
            if not key:
                raise EOFError("Ran out of input")
            if self._step(key[0]):
                return self.stack.pop()

    def _step(self, op):
        """Run one opcode; True once STOP has been read."""
        stack = self.stack
        if op == 46:  # STOP
            return True
        elif op == 0x80:  # PROTO
            proto = self.read(1)[0]
            if not 0 <= proto <= HIGHEST_PROTOCOL:
                raise ValueError("unsupported pickle protocol: %d" % proto)
            self.proto = proto
        elif op == 0x95:  # FRAME: the frames only wrap the stream
            frame_size = unpack('<Q', self.read(8))[0]
            if frame_size > sys.maxsize:
                raise ValueError("frame size > sys.maxsize: %d" % frame_size)
        elif op == 78:  # NONE
            stack.append(None)
        elif op == 0x89:
            stack.append(False)
        elif op == 0x88:
            stack.append(True)
        elif op == 73:  # INT
            data = self.readline()
            if data == b'01\n':
                stack.append(True)
            elif data == b'00\n':
                stack.append(False)
            else:
                stack.append(int(data.decode("ascii")))
        elif op == 74:  # BININT
            stack.append(unpack('<i', self.read(4))[0])
        elif op == 75:  # BININT1
            stack.append(self.read(1)[0])
        elif op == 77:  # BININT2
            stack.append(unpack('<H', self.read(2))[0])
        elif op == 76:  # LONG
            val = self.readline()[:-1]
            if val and val[-1] == 76:
                val = val[:-1]
            stack.append(int(val.decode("ascii"), 0))
        elif op == 0x8a:  # LONG1
            n = self.read(1)[0]
            stack.append(decode_long(self.read(n)))
        elif op == 0x8b:  # LONG4
            n = unpack('<i', self.read(4))[0]
            if n < 0:
                raise UnpicklingError("LONG pickle has negative byte count")
            stack.append(decode_long(self.read(n)))
        elif op == 70:  # FLOAT
            stack.append(float(self.readline()[:-1].decode("ascii")))
        elif op == 71:  # BINFLOAT
            stack.append(unpack('>d', self.read(8))[0])
        elif op == 83:  # STRING
            data = self.readline()[:-1]
            if len(data) >= 2 and data[0] == data[-1] and (data[0] == 34 or data[0] == 39):
                data = data[1:-1]
            else:
                raise UnpicklingError("the STRING opcode argument must be quoted")
            stack.append(self._decode_string(_unescape_string(data)))
        elif op == 84:  # BINSTRING
            n = unpack('<i', self.read(4))[0]
            if n < 0:
                raise UnpicklingError("BINSTRING pickle has negative byte count")
            stack.append(self._decode_string(self.read(n)))
        elif op == 85:  # SHORT_BINSTRING
            stack.append(self._decode_string(self.read(self.read(1)[0])))
        elif op == 66:  # BINBYTES
            n = unpack('<I', self.read(4))[0]
            stack.append(self.read(n))
        elif op == 67:  # SHORT_BINBYTES
            stack.append(self.read(self.read(1)[0]))
        elif op == 0x8e:  # BINBYTES8
            n = unpack('<Q', self.read(8))[0]
            stack.append(self.read(n))
        elif op == 0x96:  # BYTEARRAY8
            n = unpack('<Q', self.read(8))[0]
            stack.append(bytearray(self.read(n)))
        elif op == 86:  # UNICODE
            stack.append(_raw_unicode_unescape(self.readline()[:-1]))
        elif op == 88:  # BINUNICODE
            n = unpack('<I', self.read(4))[0]
            stack.append(self.read(n).decode('utf-8'))
        elif op == 0x8d:  # BINUNICODE8
            n = unpack('<Q', self.read(8))[0]
            stack.append(self.read(n).decode('utf-8'))
        elif op == 0x8c:  # SHORT_BINUNICODE
            stack.append(self.read(self.read(1)[0]).decode('utf-8'))
        elif op == 41:  # EMPTY_TUPLE
            stack.append(())
        elif op == 116:  # TUPLE
            items = self.pop_mark()
            self.stack.append(tuple(items))
        elif op == 0x85:  # TUPLE1
            stack[-1] = (stack[-1],)
        elif op == 0x86:  # TUPLE2
            stack[-2:] = [(stack[-2], stack[-1])]
        elif op == 0x87:  # TUPLE3
            stack[-3:] = [(stack[-3], stack[-2], stack[-1])]
        elif op == 93:  # EMPTY_LIST
            stack.append([])
        elif op == 125:  # EMPTY_DICT
            stack.append({})
        elif op == 0x8f:  # EMPTY_SET
            stack.append(set())
        elif op == 0x91:  # FROZENSET
            items = self.pop_mark()
            self.stack.append(frozenset(items))
        elif op == 108:  # LIST
            items = self.pop_mark()
            self.stack.append(items)
        elif op == 100:  # DICT
            items = self.pop_mark()
            d = {}
            i = 0
            while i < len(items):
                d[items[i]] = items[i + 1]
                i += 2
            self.stack.append(d)
        elif op == 40:  # MARK
            self.metastack.append(stack)
            self.stack = []
        elif op == 48:  # POP
            if stack:
                stack.pop()
            else:
                self.pop_mark()
        elif op == 49:  # POP_MARK
            self.pop_mark()
        elif op == 50:  # DUP
            stack.append(stack[-1])
        elif op == 97:  # APPEND
            value = stack.pop()
            stack[-1].append(value)
        elif op == 101:  # APPENDS
            items = self.pop_mark()
            self.stack[-1].extend(items)
        elif op == 115:  # SETITEM
            value = stack.pop()
            key = stack.pop()
            stack[-1][key] = value
        elif op == 117:  # SETITEMS
            items = self.pop_mark()
            target = self.stack[-1]
            i = 0
            while i < len(items):
                target[items[i]] = items[i + 1]
                i += 2
        elif op == 0x90:  # ADDITEMS
            items = self.pop_mark()
            target = self.stack[-1]
            if isinstance(target, set):
                target.update(items)
            else:
                for item in items:
                    target.add(item)
        elif op == 103:  # GET
            i = int(self.readline()[:-1].decode("ascii"))
            self._get(i)
        elif op == 104:  # BINGET
            self._get(self.read(1)[0])
        elif op == 106:  # LONG_BINGET
            self._get(unpack('<I', self.read(4))[0])
        elif op == 112:  # PUT
            i = int(self.readline()[:-1].decode("ascii"))
            if i < 0:
                raise ValueError("negative PUT argument")
            self.memo[i] = stack[-1]
        elif op == 113:  # BINPUT
            self.memo[self.read(1)[0]] = stack[-1]
        elif op == 114:  # LONG_BINPUT
            i = unpack('<I', self.read(4))[0]
            self.memo[i] = stack[-1]
        elif op == 0x94:  # MEMOIZE
            self.memo[len(self.memo)] = stack[-1]
        elif op == 99:  # GLOBAL
            module = self.readline()[:-1].decode("utf-8")
            name = self.readline()[:-1].decode("utf-8")
            stack.append(self.find_class(module, name))
        elif op == 0x93:  # STACK_GLOBAL
            name = stack.pop()
            module = stack.pop()
            if type(name) is not str or type(module) is not str:
                raise UnpicklingError("STACK_GLOBAL requires str")
            stack.append(self.find_class(module, name))
        elif op == 0x82 or op == 0x83 or op == 0x84:  # EXT1, EXT2, EXT4
            if op == 0x82:
                code = self.read(1)[0]
            elif op == 0x83:
                code = unpack('<H', self.read(2))[0]
            else:
                code = unpack('<i', self.read(4))[0]
            stack.append(self._get_extension(code))
        elif op == 82:  # REDUCE
            args = stack.pop()
            func = stack[-1]
            stack[-1] = func(*args)
        elif op == 0x81:  # NEWOBJ
            args = stack.pop()
            cls = stack.pop()
            stack.append(cls.__new__(cls, *args))
        elif op == 0x92:  # NEWOBJ_EX
            kwargs = stack.pop()
            args = stack.pop()
            cls = stack.pop()
            stack.append(cls.__new__(cls, *args, **kwargs))
        elif op == 105:  # INST
            module = self.readline()[:-1].decode("ascii")
            name = self.readline()[:-1].decode("ascii")
            klass = self.find_class(module, name)
            self._instantiate(klass, self.pop_mark())
        elif op == 111:  # OBJ
            args = self.pop_mark()
            cls = args.pop(0)
            self._instantiate(cls, args)
        elif op == 98:  # BUILD
            state = stack.pop()
            inst = stack[-1]
            setstate = getattr(inst, "__setstate__", _NoValue)
            if setstate is not _NoValue:
                setstate(state)
                return False
            slotstate = None
            if isinstance(state, tuple) and len(state) == 2:
                state, slotstate = state
            if state:
                d = inst.__dict__
                for k in state:
                    d[k] = state[k]
            if slotstate:
                for k in slotstate:
                    setattr(inst, k, slotstate[k])
        elif op == 80:  # PERSID
            pid = self.readline()[:-1].decode("ascii")
            stack.append(self.persistent_load(pid))
        elif op == 81:  # BINPERSID
            pid = stack.pop()
            stack.append(self.persistent_load(pid))
        elif op == 0x97 or op == 0x98:  # NEXT_BUFFER, READONLY_BUFFER
            raise UnpicklingError("pickle stream refers to out-of-band data but no *buffers* argument was given")
        else:
            raise UnpicklingError("invalid load key, %r." % (bytes([op]),))
        return False

    def _get(self, i):
        try:
            self.stack.append(self.memo[i])
        except KeyError:
            raise UnpicklingError("Memo value not found at index %d" % i)

    def _decode_string(self, value):
        if self.encoding == "bytes":
            return value
        return value.decode(self.encoding, self.errors)

    def _instantiate(self, klass, args):
        if args or not isinstance(klass, type) or hasattr(klass, "__getinitargs__"):
            value = klass(*args)
        else:
            value = klass.__new__(klass)
        self.stack.append(value)

    def _get_extension(self, code):
        nil = []
        obj = copyreg._extension_cache.get(code, nil)
        if obj is not nil:
            return obj
        key = copyreg._inverted_registry.get(code)
        if not key:
            if code <= 0:
                raise UnpicklingError("EXT specifies code <= 0")
            raise ValueError("unregistered extension code %d" % code)
        obj = self.find_class(key[0], key[1])
        copyreg._extension_cache[code] = obj
        return obj

    def find_class(self, module, name):
        """Return the object `name` of `module`; override to restrict it."""
        if self.proto < 3 and self.fix_imports:
            key = (module, name)
            if key in _UNFIX_NAME:
                module, name = _UNFIX_NAME[key]
            elif module in _UNFIX_IMPORT:
                module = _UNFIX_IMPORT[module]
            elif module == "exceptions" and name in _OS_ERRORS:
                module = "builtins"
        if module == "_codecs" and name == "encode":
            return _codecs_encode
        mod = sys.modules.get(module)
        if mod is None:
            raise ModuleNotFoundError("No module named %r" % module)
        if self.proto >= 4:
            return _getattribute(mod, name.split('.'))
        return getattr(mod, name)


def _dump(obj, file, protocol=None, *, fix_imports=True, buffer_callback=None):
    Pickler(file, protocol, fix_imports=fix_imports, buffer_callback=buffer_callback).dump(obj)


def _dumps(obj, protocol=None, *, fix_imports=True, buffer_callback=None):
    f = io.BytesIO()
    Pickler(f, protocol, fix_imports=fix_imports, buffer_callback=buffer_callback).dump(obj)
    return f.getvalue()


def _load(file, *, fix_imports=True, encoding="ASCII", errors="strict", buffers=None):
    return Unpickler(file, fix_imports=fix_imports, encoding=encoding, errors=errors, buffers=buffers).load()


def _loads(s, *, fix_imports=True, encoding="ASCII", errors="strict", buffers=None):
    if isinstance(s, str):
        raise TypeError("Can't load pickle from unicode string")
    file = io.BytesIO(s)
    return Unpickler(file, fix_imports=fix_imports, encoding=encoding, errors=errors, buffers=buffers).load()


dump = _dump
dumps = _dumps
load = _load
loads = _loads
