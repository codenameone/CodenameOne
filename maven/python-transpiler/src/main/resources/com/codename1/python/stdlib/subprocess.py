# Copyright (c) 2001 Python Software Foundation; All Rights Reserved
# Licensed under the Python Software Foundation License Version 2; the full
# text is in maven/python-transpiler/third-party/cpython/LICENSE.txt.
#
# Derived from CPython's Lib/subprocess.py. Changes: rewritten for the Codename One
# Python runtime, trimmed to what the transpiler supports and reorganised;
# the C accelerators and platform-specific branches are left out.
#
# subprocess - Subprocesses with accessible I/O streams
#
# For more information about this module, see PEP 324.
#
# Copyright (c) 2003-2005 by Peter Astrand <astrand@lysator.liu.se>
#
# Licensed to PSF under a Contributor Agreement.

"""subprocess: the API and its errors. A mobile application cannot start
another program, so Popen (and run, call, check_output over it) raises
FileNotFoundError, as CPython does for a command that is not installed;
code that probes for an optional tool takes its fallback."""

PIPE = -1
STDOUT = -2
DEVNULL = -3


class SubprocessError(Exception):
    pass


class CalledProcessError(SubprocessError):
    def __init__(self, returncode, cmd, output=None, stderr=None):
        self.returncode = returncode
        self.cmd = cmd
        self.output = output
        self.stderr = stderr

    @property
    def stdout(self):
        return self.output

    def __str__(self):
        return "Command '%s' returned non-zero exit status %d." % (self.cmd, self.returncode)


class TimeoutExpired(SubprocessError):
    def __init__(self, cmd, timeout, output=None, stderr=None):
        self.cmd = cmd
        self.timeout = timeout
        self.output = output
        self.stderr = stderr

    def __str__(self):
        return "Command '%s' timed out after %s seconds" % (self.cmd, self.timeout)


class CompletedProcess:
    def __init__(self, args, returncode, stdout=None, stderr=None):
        self.args = args
        self.returncode = returncode
        self.stdout = stdout
        self.stderr = stderr

    def check_returncode(self):
        if self.returncode:
            raise CalledProcessError(self.returncode, self.args, self.stdout, self.stderr)


def _name(args):
    if isinstance(args, (str, bytes)):
        return args.split()[0] if args.split() else args
    return list(args)[0] if args else ""


class Popen:
    def __init__(self, args, *a, **kw):
        self.args = args
        raise FileNotFoundError(2, "No such file or directory: %r (a mobile application cannot run programs)"
                                % (_name(args),))


def run(*popenargs, input=None, capture_output=False, timeout=None, check=False, **kwargs):
    Popen(*popenargs, **kwargs)


def call(*popenargs, timeout=None, **kwargs):
    Popen(*popenargs, **kwargs)


def check_call(*popenargs, **kwargs):
    Popen(*popenargs, **kwargs)


def check_output(*popenargs, timeout=None, **kwargs):
    Popen(*popenargs, **kwargs)


def getstatusoutput(cmd):
    return (127, "/bin/sh: %s: command not found" % (_name(cmd),))


def getoutput(cmd):
    return getstatusoutput(cmd)[1]


def list2cmdline(seq):
    out = []
    for a in seq:
        if not a or " " in a or "\t" in a:
            out.append('"' + a.replace('"', '\\"') + '"')
        else:
            out.append(a)
    return " ".join(out)
