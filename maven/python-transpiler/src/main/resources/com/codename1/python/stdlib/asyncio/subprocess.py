# Copyright (c) 2001 Python Software Foundation; All Rights Reserved
# Licensed under the Python Software Foundation License Version 2; the full
# text is in maven/python-transpiler/third-party/cpython/LICENSE.txt.
#
# Derived from CPython's Lib/asyncio/subprocess.py. Changes: rewritten for the Codename One
# Python runtime, trimmed to what the transpiler supports and reorganised;
# the C accelerators and platform-specific branches are left out.
"""asyncio.subprocess: the API, over the same rule as subprocess. A mobile
application cannot start another program, so create_subprocess_exec and
create_subprocess_shell raise FileNotFoundError, as CPython does for a
command that is not installed. The classes are here for libraries that
subclass or annotate with them."""

from subprocess import DEVNULL, PIPE, STDOUT  # noqa: F401

from asyncio._transports import Protocol, StreamReader, SubprocessProtocol

__all__ = ["create_subprocess_exec", "create_subprocess_shell", "Process", "PIPE", "STDOUT", "DEVNULL"]


class SubprocessStreamProtocol(Protocol, SubprocessProtocol):
    def __init__(self, limit, loop):
        self._limit = limit
        self._loop = loop
        self.stdin = self.stdout = self.stderr = None
        self._transport = None

    def connection_made(self, transport):
        self._transport = transport
        if transport.get_pipe_transport(1):
            self.stdout = StreamReader(limit=self._limit, loop=self._loop)
        if transport.get_pipe_transport(2):
            self.stderr = StreamReader(limit=self._limit, loop=self._loop)

    def pipe_data_received(self, fd, data):
        reader = self.stdout if fd == 1 else self.stderr if fd == 2 else None
        if reader is not None:
            reader.feed_data(data)

    def pipe_connection_lost(self, fd, exc):
        reader = self.stdout if fd == 1 else self.stderr if fd == 2 else None
        if reader is not None:
            if exc is None:
                reader.feed_eof()
            else:
                reader.set_exception(exc)


class Process:
    def __init__(self, transport, protocol, loop):
        self._transport = transport
        self._protocol = protocol
        self._loop = loop
        self.stdin = protocol.stdin
        self.stdout = protocol.stdout
        self.stderr = protocol.stderr
        self.pid = transport.get_pid()

    @property
    def returncode(self):
        return self._transport.get_returncode()

    def send_signal(self, signal):
        self._transport.send_signal(signal)

    def terminate(self):
        self._transport.terminate()

    def kill(self):
        self._transport.kill()


def _no_process(program):
    return FileNotFoundError(2, "No such file or directory: %r (an application cannot start programs)"
                             % (program,))


async def create_subprocess_exec(program, *args, stdin=None, stdout=None, stderr=None, limit=2 ** 16, **kwds):
    raise _no_process(program)


async def create_subprocess_shell(cmd, stdin=None, stdout=None, stderr=None, limit=2 ** 16, **kwds):
    raise _no_process(cmd)
