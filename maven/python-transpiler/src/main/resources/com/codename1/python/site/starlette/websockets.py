# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Server-side websockets without ASGI.

Every connection runs as a task on one shared asyncio event loop, which a
daemon thread runs. The platform server and the test client call in from
their own threads: ``_Session.deliver`` hands a client message to the loop
with ``call_soon_threadsafe``, and the endpoint's ``receive`` awaits an
``asyncio.Queue``. The endpoint can therefore await anything that really
suspends -- timers, other tasks, ``wait_for`` -- and send from any task.
Messages it sends reach the session's sink on the loop thread.
"""
import asyncio
import enum
import json as _json
import threading

from starlette.datastructures import URL
from starlette.requests import HTTPConnection


class WebSocketState(enum.Enum):
    CONNECTING = 0
    CONNECTED = 1
    DISCONNECTED = 2
    RESPONSE = 3


class WebSocketDisconnect(Exception):
    def __init__(self, code=1000, reason=None):
        Exception.__init__(self)
        self.code = code
        self.reason = reason or ""


class WebSocket(HTTPConnection):
    def __init__(self, path, query, headers, session, app=None):
        HTTPConnection.__init__(self, "GET", path, query, headers, b"", app)
        self.scope = {"type": "websocket", "path": path}
        self._session = session
        self.client_state = WebSocketState.CONNECTING
        self.application_state = WebSocketState.CONNECTING
        # Set while a pure ASGI middleware wraps this connection: the
        # receive/send it handed the app inside it, which the endpoint's
        # receive and send must go through.
        self._receive_hook = None
        self._send_hook = None

    @property
    def url(self):
        host = self.headers.get("host", "testserver")
        return URL("ws://" + host + self._path + ("?" + self._query if self._query else ""))

    async def receive(self):
        if self._receive_hook is not None:
            return await self._receive_hook()
        return await self._raw_receive()

    async def _raw_receive(self):
        if self.client_state == WebSocketState.DISCONNECTED:
            raise RuntimeError('Cannot call "receive" once a disconnect message has been received.')
        m = await self._session.inbox.get()
        if m["type"] == "websocket.disconnect":
            self.client_state = WebSocketState.DISCONNECTED
        return m

    async def send(self, message):
        if self._send_hook is not None:
            await self._send_hook(message)
        else:
            await self._raw_send(message)

    async def _raw_send(self, message):
        t = message["type"]
        if self.application_state == WebSocketState.DISCONNECTED:
            raise RuntimeError('Cannot call "send" once a close message has been sent.')
        if t == "websocket.accept":
            self.application_state = WebSocketState.CONNECTED
            self.client_state = WebSocketState.CONNECTED
        elif t == "websocket.close":
            self.application_state = WebSocketState.DISCONNECTED
        elif self.application_state != WebSocketState.CONNECTED:
            raise RuntimeError('WebSocket is not connected. Need to call "accept" first.')
        self._session.outgoing(message)

    async def accept(self, subprotocol=None, headers=None):
        if self.client_state == WebSocketState.CONNECTING:
            await self.receive()
        await self.send({"type": "websocket.accept", "subprotocol": subprotocol, "headers": headers or []})

    def _raise_on_disconnect(self, m):
        if m["type"] == "websocket.disconnect":
            raise WebSocketDisconnect(m.get("code", 1000), m.get("reason"))

    def _check_connected(self):
        if self.application_state != WebSocketState.CONNECTED:
            raise RuntimeError('WebSocket is not connected. Need to call "accept" first.')

    async def receive_text(self):
        self._check_connected()
        m = await self.receive()
        self._raise_on_disconnect(m)
        return m["text"]

    async def receive_bytes(self):
        self._check_connected()
        m = await self.receive()
        self._raise_on_disconnect(m)
        return m["bytes"]

    async def receive_json(self, mode="text"):
        self._check_connected()
        m = await self.receive()
        self._raise_on_disconnect(m)
        if mode == "text":
            return _json.loads(m["text"])
        return _json.loads(m["bytes"].decode("utf-8"))

    async def iter_text(self):
        try:
            while True:
                yield await self.receive_text()
        except WebSocketDisconnect:
            pass

    async def iter_bytes(self):
        try:
            while True:
                yield await self.receive_bytes()
        except WebSocketDisconnect:
            pass

    async def iter_json(self):
        try:
            while True:
                yield await self.receive_json()
        except WebSocketDisconnect:
            pass

    async def send_text(self, data):
        await self.send({"type": "websocket.send", "text": data})

    async def send_bytes(self, data):
        await self.send({"type": "websocket.send", "bytes": bytes(data)})

    async def send_json(self, data, mode="text"):
        text = _json.dumps(data, separators=(",", ":"), ensure_ascii=False)
        if mode == "text":
            await self.send({"type": "websocket.send", "text": text})
        else:
            await self.send({"type": "websocket.send", "bytes": text.encode("utf-8")})

    async def close(self, code=1000, reason=None):
        await self.send({"type": "websocket.close", "code": code, "reason": reason or ""})


_loop = None
_loop_lock = threading.Lock()


def _ws_loop():
    """The event loop every websocket connection runs on, started on first use."""
    global _loop
    with _loop_lock:
        if _loop is None:
            loop = asyncio.new_event_loop()
            loop._inbox()
            t = threading.Thread(target=loop.run_forever, name="websockets", daemon=True)
            t.start()
            _loop = loop
        return _loop


class _Session:
    """One connection: the queue the endpoint reads and the task it runs in.

    ``sink(message)`` receives every message the endpoint sends, on the loop
    thread. ``deliver`` queues a client message from any thread.
    ``on_done(session)``, when given, runs on the loop thread once the
    endpoint has finished; ``error`` then holds what it raised, if anything.
    """

    def __init__(self, sink, on_done=None):
        self.sink = sink
        self.on_done = on_done
        self.inbox = None
        self.task = None
        self.done = False
        self.error = None
        self.loop = _ws_loop()
        self.finished = threading.Event()

    def outgoing(self, message):
        self.sink(message)

    def start(self, coro):
        self.loop.call_soon_threadsafe(self._begin, coro)

    def _begin(self, coro):
        self.inbox = asyncio.Queue()
        self.inbox.put_nowait({"type": "websocket.connect"})
        self.task = self.loop.create_task(coro)
        self.task.add_done_callback(self._end)

    def _end(self, task):
        if not task.cancelled():
            e = task.exception()
            if e is not None:
                self.error = e
        self.done = True
        try:
            if self.on_done is not None:
                self.on_done(self)
        finally:
            self.finished.set()

    def deliver(self, message):
        self.loop.call_soon_threadsafe(self._put, message)

    def _put(self, message):
        if not self.done:
            self.inbox.put_nowait(message)

    def cancel(self):
        """Cancels the endpoint task from any thread."""
        def stop():
            if self.task is not None and not self.done:
                self.task.cancel()
        self.loop.call_soon_threadsafe(stop)

    def wait(self, timeout=None):
        return self.finished.wait(timeout)
