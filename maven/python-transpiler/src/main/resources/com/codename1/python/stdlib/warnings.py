# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Warning control.

A translated program has no frames to inspect, so a warning is reported
against the file "sys" at line 1, as CPython reports a warning whose
stacklevel runs past the top of the stack; the module registry used by
the "default" and "module" actions is therefore one per program.
"""

import sys
import re

__all__ = ["warn", "warn_explicit", "showwarning", "formatwarning", "filterwarnings", "simplefilter",
           "resetwarnings", "catch_warnings", "deprecated"]

filters = []
defaultaction = "default"
onceregistry = {}
_registry = {}
_filters_version = 1

_ACTIONS = ("error", "ignore", "always", "all", "default", "module", "once")


def _filters_mutated():
    global _filters_version
    _filters_version += 1
    _registry.clear()


def _check_action(action):
    if action not in _ACTIONS:
        raise ValueError("invalid action: %r" % (action,))


def filterwarnings(action, message="", category=Warning, module="", lineno=0, append=False):
    _check_action(action)
    if not isinstance(message, str):
        raise TypeError("message must be a string")
    if not (isinstance(category, type) and issubclass(category, Warning)):
        raise TypeError("category must be a Warning subclass")
    if not isinstance(module, str):
        raise TypeError("module must be a string")
    if not isinstance(lineno, int) or lineno < 0:
        raise ValueError("lineno must be an int >= 0")
    msg = re.compile(message, re.I) if message else None
    mod = re.compile(module) if module else None
    _add_filter(action, msg, category, mod, lineno, append=append)


def simplefilter(action, category=Warning, lineno=0, append=False):
    _check_action(action)
    if not isinstance(lineno, int) or lineno < 0:
        raise ValueError("lineno must be an int >= 0")
    _add_filter(action, None, category, None, lineno, append=append)


def _add_filter(*item, append):
    if not append:
        try:
            filters.remove(item)
        except ValueError:
            pass
        filters.insert(0, item)
    elif item not in filters:
        filters.append(item)
    _filters_mutated()


def resetwarnings():
    filters[:] = []
    _filters_mutated()


class WarningMessage:
    _WARNING_DETAILS = ("message", "category", "filename", "lineno", "file", "line", "source")

    def __init__(self, message, category, filename, lineno, file=None, line=None, source=None):
        self.message = message
        self.category = category
        self.filename = filename
        self.lineno = lineno
        self.file = file
        self.line = line
        self.source = source
        self._category_name = category.__name__ if category else None

    def __str__(self):
        return ("{message : %r, category : %r, filename : %r, lineno : %s, line : %r}"
                % (self.message, self._category_name, self.filename, self.lineno, self.line))


def formatwarning(message, category, filename, lineno, line=None):
    s = "%s:%s: %s: %s\n" % (filename, lineno, category.__name__, message)
    if line:
        s += "  %s\n" % line.strip()
    return s


def _showwarnmsg_impl(msg):
    f = msg.file
    if f is None:
        f = sys.stderr
        if f is None:
            return
    text = formatwarning(msg.message, msg.category, msg.filename, msg.lineno, msg.line)
    try:
        f.write(text)
    except OSError:
        pass


def showwarning(message, category, filename, lineno, file=None, line=None):
    _showwarnmsg_impl(WarningMessage(message, category, filename, lineno, file, line))


_showwarning_orig = showwarning


def _showwarnmsg(msg):
    sw = globals().get("showwarning")
    if sw is not _showwarning_orig:
        if not callable(sw):
            raise TypeError("warnings.showwarning() must be set to a function or method")
        sw(msg.message, msg.category, msg.filename, msg.lineno, msg.file, msg.line)
        return
    _showwarnmsg_impl(msg)


def warn(message, category=None, stacklevel=1, source=None, *, skip_file_prefixes=()):
    if isinstance(message, Warning):
        category = message.__class__
    if category is None:
        category = UserWarning
    if not (isinstance(category, type) and issubclass(category, Warning)):
        raise TypeError("category must be a Warning subclass, not '%s'" % type(category).__name__)
    warn_explicit(message, category, "sys", 1, "sys", _registry, source=source)


def warn_explicit(message, category, filename, lineno, module=None, registry=None, module_globals=None,
                  source=None):
    lineno = int(lineno)
    if module is None:
        module = filename or "<unknown>"
        if module[-3:].lower() == ".py":
            module = module[:-3]
    if registry is None:
        registry = {}
    if registry.get("version", 0) != _filters_version:
        registry.clear()
        registry["version"] = _filters_version
    if isinstance(message, Warning):
        text = str(message)
        category = message.__class__
    else:
        text = message
        message = category(message)
    key = (text, category, lineno)
    if registry.get(key):
        return
    for item in filters:
        action, msg, cat, mod, ln = item
        if ((msg is None or msg.match(text)) and issubclass(category, cat)
                and (mod is None or mod.match(module)) and (ln == 0 or lineno == ln)):
            break
    else:
        action = defaultaction
    if action == "ignore":
        return
    if action == "error":
        raise message
    if action == "once":
        registry[key] = 1
        oncekey = (text, category)
        if onceregistry.get(oncekey):
            return
        onceregistry[oncekey] = 1
    elif action in ("always", "all"):
        pass
    elif action == "module":
        registry[key] = 1
        altkey = (text, category, 0)
        if registry.get(altkey):
            return
        registry[altkey] = 1
    elif action == "default":
        registry[key] = 1
    else:
        raise RuntimeError("Unrecognized action (%r) in warnings.filters:\n %s" % (action, item))
    _showwarnmsg(WarningMessage(message, category, filename, lineno, source=source))


class catch_warnings:
    def __init__(self, *, record=False, module=None, action=None, category=Warning, lineno=0, append=False):
        self._record = record
        self._entered = False
        self._module = sys.modules["warnings"] if module is None else module
        if action is None:
            self._filter = None
        else:
            self._filter = (action, category, lineno, append)

    def __repr__(self):
        args = []
        if self._record:
            args.append("record=True")
        return "%s(%s)" % (type(self).__name__, ", ".join(args))

    def __enter__(self):
        if self._entered:
            raise RuntimeError("Cannot enter %r twice" % self)
        self._entered = True
        global showwarning
        self._filters = filters[:]
        self._showwarning = showwarning
        self._onceregistry = dict(onceregistry)
        _filters_mutated()
        if self._filter is not None:
            simplefilter(*self._filter)
        if self._record:
            log = []

            def _record(message, category, filename, lineno, file=None, line=None):
                log.append(WarningMessage(message, category, filename, lineno, file, line))

            showwarning = _record
            return log
        return None

    def __exit__(self, *exc_info):
        if not self._entered:
            raise RuntimeError("Cannot exit %r without entering first" % self)
        global showwarning
        filters[:] = self._filters
        showwarning = self._showwarning
        onceregistry.clear()
        onceregistry.update(self._onceregistry)
        _filters_mutated()


class deprecated:
    def __init__(self, message, /, *, category=DeprecationWarning, stacklevel=1):
        if not isinstance(message, str):
            raise TypeError("Expected an object of type str for 'message', not %r" % (type(message).__name__,))
        self.message = message
        self.category = category
        self.stacklevel = stacklevel

    def __call__(self, arg, /):
        msg = self.message
        category = self.category
        if isinstance(arg, type):
            original_init = arg.__init__

            def __init__(self, *args, **kwargs):
                warn(msg, category=category)
                original_init(self, *args, **kwargs)

            arg.__init__ = __init__
            arg.__deprecated__ = msg
            return arg
        if callable(arg):
            def wrapper(*args, **kwargs):
                warn(msg, category=category)
                return arg(*args, **kwargs)

            try:
                wrapper.__name__ = arg.__name__
                wrapper.__doc__ = arg.__doc__
            except AttributeError:
                pass
            wrapper.__deprecated__ = msg
            return wrapper
        raise TypeError("@deprecated decorator with non-None category must be applied to a class or "
                        "callable, not %r" % (arg,))


filters[:] = [("default", None, DeprecationWarning, re.compile("__main__"), 0),
              ("ignore", None, DeprecationWarning, None, 0),
              ("ignore", None, PendingDeprecationWarning, None, 0),
              ("ignore", None, ImportWarning, None, 0),
              ("ignore", None, ResourceWarning, None, 0)]
