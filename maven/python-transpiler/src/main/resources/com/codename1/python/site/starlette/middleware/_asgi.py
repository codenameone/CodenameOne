# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Pure ASGI middleware over the request-based stack.

The stack here passes request objects, not ASGI messages. A user-written
middleware of the shape ``M(app)`` with ``async __call__(scope, receive,
send)`` still works: :func:`wrap` builds the ASGI scope from the request,
hands the middleware an ASGI ``app`` that runs the rest of the stack, and
turns what the middleware sends back into a response. A websocket keeps
its connection object; the middleware's ``receive`` and ``send`` are
installed on it for as long as the app inside runs.
"""
from starlette.responses import Response


def _latin1(v):
    return v if isinstance(v, bytes) else str(v).encode("latin-1")


def _text(v):
    return v.decode("latin-1") if isinstance(v, (bytes, bytearray)) else str(v)


def _scope(conn):
    kind = conn.scope.get("type", "http")
    scope = {
        "type": kind,
        "asgi": {"version": "3.0", "spec_version": "2.4"},
        "http_version": "1.1",
        "scheme": "ws" if kind == "websocket" else "http",
        "path": conn._path,
        "raw_path": conn._path.encode("utf-8"),
        "query_string": conn._query.encode("latin-1"),
        "root_path": "",
        "headers": list(conn.headers.raw),
        "client": conn.client,
        "server": ("testserver", 80),
        "state": {},
        "app": conn.app,
        "_cn1_conn": conn,
    }
    if kind == "websocket":
        protos = conn.headers.get("sec-websocket-protocol")
        scope["subprotocols"] = [p.strip() for p in protos.split(",")] if protos else []
    else:
        scope["method"] = conn.method
    return scope


def _sync_headers(conn, scope):
    # A middleware may hand the app a scope with other headers.
    from starlette.datastructures import Headers
    hs = scope.get("headers")
    if hs is not None:
        conn.headers = Headers([(_text(k), _text(v)) for k, v in hs])


def _inner(app):
    """The request-based ``app`` as an ASGI app."""
    direct = getattr(app, "_cn1_asgi_app", None)
    if direct is not None:
        # Two ASGI middlewares in a row talk ASGI to each other directly, so
        # the outer one sees the inner one's messages as they are sent.
        return direct

    async def asgi(scope, receive, send):
        conn = scope["_cn1_conn"]
        _sync_headers(conn, scope)
        if scope["type"] == "websocket":
            saved = (conn._receive_hook, conn._send_hook)
            conn._receive_hook, conn._send_hook = receive, send
            try:
                resp = await app(conn)
            finally:
                conn._receive_hook, conn._send_hook = saved
            from starlette.websockets import WebSocketState
            if isinstance(resp, Response) and conn.application_state == WebSocketState.CONNECTING:
                # A refusal from a request-based middleware inside.
                await send({"type": "websocket.http.response.start", "status": resp.status_code,
                            "headers": resp.headers.raw})
                await send({"type": "websocket.http.response.body", "body": resp.body})
            return
        body = b""
        while True:
            m = await receive()
            if m["type"] != "http.request":
                break
            body += m.get("body", b"")
            if not m.get("more_body", False):
                break
        conn._body = body
        resp = await app(conn)
        conn._cn1_inner_response = resp
        await send({"type": "http.response.start", "status": resp.status_code,
                    "headers": resp.headers.raw})
        if getattr(resp, "_iterator", None) is not None:
            # A streamed body is offered unread: a middleware that passes the
            # message on leaves it to stream to the client chunk by chunk,
            # and one that reads message["body"] gets the whole of it.
            await send(_StreamBody(resp))
            return
        await send({"type": "http.response.body", "body": resp.body})
    return asgi


class _Pending:
    """The body of a streamed response nobody has read yet."""

    def __init__(self, resp):
        self.resp = resp


class _StreamBody(dict):
    def __init__(self, resp):
        dict.__init__(self, type="http.response.body", body=_Pending(resp), more_body=False)

    def _real(self, key, value):
        if key == "body" and isinstance(value, _Pending):
            value = value.resp.body
            dict.__setitem__(self, "body", value)
        return value

    def __getitem__(self, key):
        return self._real(key, dict.__getitem__(self, key))

    def get(self, key, default=None):
        return self._real(key, dict.get(self, key, default))


def wrap(cls, app, args, kwargs):
    """``cls`` (a pure ASGI middleware) around the request-based ``app``."""
    mw = cls(_inner(app), *args, **kwargs)

    async def call(conn):
        scope = _scope(conn)
        if scope["type"] == "websocket":
            await _websocket(mw, conn, scope)
            return None
        return await _http(mw, conn, scope)
    call._cn1_asgi_app = mw
    return call


async def _http(mw, conn, scope):
    state = {"read": False}

    async def receive():
        if state["read"]:
            return {"type": "http.disconnect"}
        state["read"] = True
        return {"type": "http.request", "body": conn._body, "more_body": False}

    out = {"status": None, "headers": [], "body": b"", "stream": None}

    async def send(m):
        t = m["type"]
        if t == "http.response.start":
            out["status"] = m["status"]
            out["headers"] = list(m.get("headers") or [])
        elif t == "http.response.body":
            raw = dict.get(m, "body", b"") if isinstance(m, dict) else m.get("body", b"")
            if isinstance(raw, _Pending) and not out["body"]:
                # Passed on unread: the inner stream goes out as it is.
                out["stream"] = raw.resp
            else:
                out["body"] += bytes(m.get("body", b""))

    await mw(scope, receive, send)
    if out["status"] is None:
        raise RuntimeError("No response returned.")
    stream = out["stream"]
    if stream is not None and getattr(stream, "_iterator", None) is not None:
        stream.status_code = out["status"]
        del stream.headers._list[:]
        for k, v in out["headers"]:
            stream.headers.append(_text(k), _text(v))
        return stream
    resp = Response(out["body"], status_code=out["status"])
    del resp.headers._list[:]
    for k, v in out["headers"]:
        resp.headers.append(_text(k), _text(v))
    inner = getattr(conn, "_cn1_inner_response", None)
    if inner is not None:
        resp.background = inner.background
    return resp


async def _websocket(mw, conn, scope):
    from starlette.websockets import WebSocketState
    receive = conn._receive_hook or conn._raw_receive
    outermost = conn._send_hook is None
    raw_send = conn._send_hook or conn._raw_send
    denial = {}

    async def send(m):
        t = m["type"]
        if t == "websocket.http.response.start":
            denial["status"] = m["status"]
            denial["headers"] = [(_text(k), _text(v)) for k, v in m.get("headers") or []]
            denial["body"] = b""
            return
        if t == "websocket.http.response.body":
            denial["body"] += bytes(m.get("body", b""))
            if not m.get("more_body", False) and outermost \
                    and conn.application_state == WebSocketState.CONNECTING:
                conn.application_state = WebSocketState.DISCONNECTED
                conn._session.outgoing({"type": "websocket.http.response", "status": denial["status"],
                                        "headers": denial["headers"], "body": denial["body"]})
            elif not m.get("more_body", False):
                await raw_send({"type": "websocket.http.response.start", "status": denial["status"],
                                "headers": denial["headers"]})
                await raw_send({"type": "websocket.http.response.body", "body": denial["body"]})
            return
        await raw_send(m)

    await mw(scope, receive, send)
