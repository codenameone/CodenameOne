# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.

"""getpass: read a password from the terminal without showing it."""
import os as _os
import _cn1term

__all__ = ["getpass", "getuser", "GetPassWarning"]


class GetPassWarning(UserWarning):
    pass


def getpass(prompt="Password: ", stream=None):
    """Prompts and reads a line with echo off. Ctrl-C raises
    KeyboardInterrupt. When standard input is not a terminal the line is
    read from it as typed, with a GetPassWarning, as CPython does.

    The prompt is drawn the console's way -- its question styled, the typed
    characters shown as asterisks -- rather than printed verbatim with
    nothing echoed: a trailing colon and spaces are dropped because the
    console adds its own."""
    if not _cn1term.isatty(0):
        import warnings
        warnings.warn("Can not control echo on the terminal.", GetPassWarning, stacklevel=2)
        import sys
        stream = stream or sys.stderr
        print("Warning: Password input may be echoed.", file=stream)
        # The prompt goes to the stream too, never stdout: under
        # `echo pw | tool > out.json` stdout is the tool's output.
        stream.write(prompt)
        stream.flush()
        line = _cn1term.read_line("")
        if line is None:
            raise EOFError
        return line
    question = prompt.rstrip()
    if question.endswith(":"):
        question = question[:-1]
    return _cn1term.password(question)


def getuser():
    for name in ("LOGNAME", "USER", "LNAME", "USERNAME"):
        user = _os.getenv(name)
        if user:
            return user
    raise OSError("No username set in the environment")
