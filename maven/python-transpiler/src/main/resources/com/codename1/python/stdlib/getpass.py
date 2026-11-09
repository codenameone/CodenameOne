# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""getpass: there is no terminal to read a password from on a device, so
getpass raises, as CPython does without one; ask through a dialog."""


class GetPassWarning(UserWarning):
    pass


def getpass(prompt="Password: ", stream=None):
    raise OSError("no terminal to read a password from; use a Codename One dialog")


def getuser():
    import os
    for name in ("LOGNAME", "USER", "LNAME", "USERNAME"):
        u = os.environ.get(name)
        if u:
            return u
    return "mobile"
