# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Server-side websockets without ASGI.

The endpoint coroutine is driven by ``_Session`` one step at a time rather
than by an event loop: whenever it waits for a message that has not arrived,
it yields a ``_Suspend`` and the session returns to its caller (the test
client, or the platform server's frame callback). A message delivered later
resumes it. An endpoint that awaits anything else that really suspends -- a
timer, another task -- is not supported by this driver.
"""
import enum
import json as _json

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


class _Suspend:
    def __await__(self):
        yield self


class WebSocket(HTTPConnection):
    def __init__(self, path, query, headers, session, app=None):
        HTTPConnection.__init__(self, "GET", path, query, headers, b"", app)
        self.scope = {"type": "websocket", "path": path}
        self._session = session
        self.client_state = WebSocketState.CONNECTING
        self.application_state = WebSocketState.CONNECTING

    @property
    def url(self):
        host = self.headers.get("host", "testserver")
        return URL("ws://" + host + self._path + ("?" + self._query if self._query else ""))

    async def receive(self):
        if self.client_state == WebSocketState.DISCONNECTED:
            raise RuntimeError('Cannot call "receive" once a disconnect message has been received.')
        inbox = self._session.inbox
        while not inbox:
            await _Suspend()
        m = inbox.pop(0)
        if m["type"] == "websocket.disconnect":
            self.client_state = WebSocketState.DISCONNECTED
        return m

    async def send(self, message):
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


class _Session:
    """One connection: the inbox the endpoint reads and the coroutine it runs in.

    ``sink(message)`` receives every message the endpoint sends. ``deliver``
    queues a client message and runs the endpoint until it waits again.
    """

    def __init__(self, sink):
        self.inbox = [{"type": "websocket.connect"}]
        self.sink = sink
        self.coro = None
        self.done = False
        self.error = None

    def outgoing(self, message):
        self.sink(message)

    def start(self, coro):
        self.coro = coro
        self.pump()

    def deliver(self, message):
        self.inbox.append(message)
        self.pump()

    def pump(self):
        while not self.done:
            try:
                y = self.coro.send(None)
            except StopIteration:
                self.done = True
                return
            except BaseException as e:  # noqa
                self.done = True
                self.error = e
                return
            if isinstance(y, _Suspend) and not self.inbox:
                return
