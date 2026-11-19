# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""readline: the history and completion API, kept in memory.

An application has no terminal to edit lines in, so nothing here changes
how input() reads; the module exists so a program that configures readline
when it starts -- most command-line tools do -- imports and runs unchanged.
History is a list the program can add to and read back, and history files
are read and written as plain lines.
"""

_history = []
_length = -1
_completer = None
_delims = " \t\n`~!@#$%^&*()-=+[{]}\\|;:'\",<>/?"
_hooks = {}

__all__ = [
    "add_history", "clear_history", "get_begidx", "get_completer", "get_completer_delims", "get_completion_type",
    "get_current_history_length", "get_endidx", "get_history_item", "get_history_length", "get_line_buffer",
    "insert_text", "parse_and_bind", "read_history_file", "read_init_file", "redisplay", "remove_history_item",
    "replace_history_item", "set_auto_history", "set_completer", "set_completer_delims",
    "set_completion_display_matches_hook", "set_history_length", "set_pre_input_hook", "set_startup_hook",
    "write_history_file", "append_history_file",
]

backend = "editline"


def parse_and_bind(string):
    pass


def read_init_file(filename=None):
    pass


def get_line_buffer():
    return ""


def insert_text(string):
    pass


def redisplay():
    pass


def _trim():
    if _length >= 0 and len(_history) > _length:
        del _history[:len(_history) - _length]


def read_history_file(filename=None):
    import os
    with open(filename or os.path.expanduser("~/.history"), encoding="utf-8") as f:
        _history.extend(line.rstrip("\n") for line in f)
    _trim()


def write_history_file(filename=None):
    import os
    with open(filename or os.path.expanduser("~/.history"), "w", encoding="utf-8") as f:
        for line in _history:
            f.write(line + "\n")


def append_history_file(nelements, filename=None):
    import os
    with open(filename or os.path.expanduser("~/.history"), "a", encoding="utf-8") as f:
        for line in _history[-nelements:] if nelements else []:
            f.write(line + "\n")


def get_history_length():
    return _length


def set_history_length(length):
    global _length
    _length = length
    _trim()


def get_current_history_length():
    return len(_history)


def get_history_item(index):
    # one-based, as GNU readline's
    return _history[index - 1] if 1 <= index <= len(_history) else None


def remove_history_item(pos):
    if not 0 <= pos < len(_history):
        raise ValueError("No history item at position %d" % pos)
    del _history[pos]


def replace_history_item(pos, line):
    if not 0 <= pos < len(_history):
        raise ValueError("No history item at position %d" % pos)
    _history[pos] = line


def add_history(line):
    _history.append(line)
    _trim()


def clear_history():
    del _history[:]


def set_auto_history(enabled):
    pass


def set_startup_hook(function=None):
    _hooks["startup"] = function


def set_pre_input_hook(function=None):
    _hooks["pre_input"] = function


def set_completer(function=None):
    global _completer
    _completer = function


def get_completer():
    return _completer


def get_completion_type():
    return 0


def get_begidx():
    return 0


def get_endidx():
    return 0


def set_completer_delims(string):
    global _delims
    _delims = string


def get_completer_delims():
    return _delims


def set_completion_display_matches_hook(function=None):
    _hooks["display"] = function
