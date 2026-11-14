# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""asyncio's protocol and transport interfaces, and the stream reader and
writer over them, as CPython defines them (asyncio.protocols,
asyncio.transports, asyncio.streams).

The interfaces are what a library subclasses or annotates with. This
runtime's event loop opens no sockets of its own -- network I/O goes
through Codename One's networking, on threads -- so nothing here hands a
library a transport; a StreamReader fed by the caller works as it does
under CPython."""


class BaseProtocol:
    __slots__ = ()

    def connection_made(self, transport):
        pass

    def connection_lost(self, exc):
        pass

    def pause_writing(self):
        pass

    def resume_writing(self):
        pass


class Protocol(BaseProtocol):
    __slots__ = ()

    def data_received(self, data):
        pass

    def eof_received(self):
        pass


class BufferedProtocol(BaseProtocol):
    __slots__ = ()

    def get_buffer(self, sizehint):
        raise NotImplementedError

    def buffer_updated(self, nbytes):
        raise NotImplementedError

    def eof_received(self):
        pass


class DatagramProtocol(BaseProtocol):
    __slots__ = ()

    def datagram_received(self, data, addr):
        pass

    def error_received(self, exc):
        pass


class SubprocessProtocol(BaseProtocol):
    __slots__ = ()

    def pipe_data_received(self, fd, data):
        pass

    def pipe_connection_lost(self, fd, exc):
        pass

    def process_exited(self):
        pass


class BaseTransport:
    __slots__ = ("_extra",)

    def __init__(self, extra=None):
        if extra is None:
            extra = {}
        self._extra = extra

    def get_extra_info(self, name, default=None):
        return self._extra.get(name, default)

    def is_closing(self):
        raise NotImplementedError

    def close(self):
        raise NotImplementedError

    def set_protocol(self, protocol):
        raise NotImplementedError

    def get_protocol(self):
        raise NotImplementedError


class ReadTransport(BaseTransport):
    __slots__ = ()

    def is_reading(self):
        raise NotImplementedError

    def pause_reading(self):
        raise NotImplementedError

    def resume_reading(self):
        raise NotImplementedError


class WriteTransport(BaseTransport):
    __slots__ = ()

    def set_write_buffer_limits(self, high=None, low=None):
        raise NotImplementedError

    def get_write_buffer_size(self):
        raise NotImplementedError

    def get_write_buffer_limits(self):
        raise NotImplementedError

    def write(self, data):
        raise NotImplementedError

    def writelines(self, list_of_data):
        self.write(b"".join(list_of_data))

    def write_eof(self):
        raise NotImplementedError

    def can_write_eof(self):
        raise NotImplementedError

    def abort(self):
        raise NotImplementedError


class Transport(ReadTransport, WriteTransport):
    __slots__ = ()


class DatagramTransport(BaseTransport):
    __slots__ = ()

    def sendto(self, data, addr=None):
        raise NotImplementedError

    def abort(self):
        raise NotImplementedError


class SubprocessTransport(BaseTransport):
    __slots__ = ()

    def get_pid(self):
        raise NotImplementedError

    def get_returncode(self):
        raise NotImplementedError

    def get_pipe_transport(self, fd):
        raise NotImplementedError

    def send_signal(self, signal):
        raise NotImplementedError

    def terminate(self):
        raise NotImplementedError

    def kill(self):
        raise NotImplementedError


class IncompleteReadError(EOFError):
    def __init__(self, partial, expected):
        r_expected = "undefined" if expected is None else repr(expected)
        super().__init__("%d bytes read on a total of %s expected bytes" % (len(partial), r_expected))
        self.partial = partial
        self.expected = expected


class LimitOverrunError(Exception):
    def __init__(self, message, consumed):
        super().__init__(message)
        self.consumed = consumed


_DEFAULT_LIMIT = 2 ** 16


class StreamReader:
    """Bytes fed by feed_data() and read by coroutines, as CPython's."""

    def __init__(self, limit=_DEFAULT_LIMIT, loop=None):
        if limit <= 0:
            raise ValueError("Limit cannot be <= 0")
        self._limit = limit
        self._loop = loop
        self._buffer = bytearray()
        self._eof = False
        self._waiter = None
        self._exception = None
        self._transport = None

    def exception(self):
        return self._exception

    def set_exception(self, exc):
        self._exception = exc
        self._wake()

    def set_transport(self, transport):
        self._transport = transport

    def _wake(self):
        w = self._waiter
        if w is not None:
            self._waiter = None
            if not w.done():
                w.set_result(None)

    def feed_eof(self):
        self._eof = True
        self._wake()

    def at_eof(self):
        return self._eof and not self._buffer

    def feed_data(self, data):
        if not data:
            return
        self._buffer.extend(data)
        self._wake()

    async def _wait_for_data(self, func_name):
        if self._waiter is not None:
            raise RuntimeError("%s() called while another coroutine is already waiting for incoming data"
                               % func_name)
        import asyncio
        self._waiter = asyncio.get_running_loop().create_future()
        try:
            await self._waiter
        finally:
            self._waiter = None

    async def readline(self):
        try:
            return await self.readuntil(b"\n")
        except IncompleteReadError as e:
            return e.partial

    async def readuntil(self, separator=b"\n"):
        if not separator:
            raise ValueError("Separator should be at least one-byte string")
        while True:
            if self._exception is not None:
                raise self._exception
            i = self._buffer.find(separator)
            if i >= 0:
                end = i + len(separator)
                if end > self._limit:
                    raise LimitOverrunError("Separator is found, but chunk is longer than limit", i)
                chunk = bytes(self._buffer[:end])
                del self._buffer[:end]
                return chunk
            if len(self._buffer) > self._limit:
                raise LimitOverrunError("Separator is not found, and chunk exceed the limit",
                                        len(self._buffer))
            if self._eof:
                chunk = bytes(self._buffer)
                self._buffer.clear()
                raise IncompleteReadError(chunk, None)
            await self._wait_for_data("readuntil")

    async def read(self, n=-1):
        if self._exception is not None:
            raise self._exception
        if n == 0:
            return b""
        if n < 0:
            while not self._eof:
                await self._wait_for_data("read")
                if self._exception is not None:
                    raise self._exception
            data = bytes(self._buffer)
            self._buffer.clear()
            return data
        while not self._buffer and not self._eof:
            await self._wait_for_data("read")
            if self._exception is not None:
                raise self._exception
        data = bytes(self._buffer[:n])
        del self._buffer[:n]
        return data

    async def readexactly(self, n):
        if n < 0:
            raise ValueError("readexactly size can not be less than zero")
        while len(self._buffer) < n:
            if self._exception is not None:
                raise self._exception
            if self._eof:
                incomplete = bytes(self._buffer)
                self._buffer.clear()
                raise IncompleteReadError(incomplete, n)
            await self._wait_for_data("readexactly")
        data = bytes(self._buffer[:n])
        del self._buffer[:n]
        return data

    def __aiter__(self):
        return self

    async def __anext__(self):
        val = await self.readline()
        if val == b"":
            raise StopAsyncIteration
        return val


class StreamWriter:
    """Writes to a transport, as CPython's."""

    def __init__(self, transport, protocol, reader, loop):
        self._transport = transport
        self._protocol = protocol
        self._reader = reader
        self._loop = loop

    @property
    def transport(self):
        return self._transport

    def write(self, data):
        self._transport.write(data)

    def writelines(self, data):
        self._transport.writelines(data)

    def write_eof(self):
        return self._transport.write_eof()

    def can_write_eof(self):
        return self._transport.can_write_eof()

    def close(self):
        return self._transport.close()

    def is_closing(self):
        return self._transport.is_closing()

    async def wait_closed(self):
        pass

    def get_extra_info(self, name, default=None):
        return self._transport.get_extra_info(name, default)

    async def drain(self):
        if self._reader is not None and self._reader.exception() is not None:
            raise self._reader.exception()
