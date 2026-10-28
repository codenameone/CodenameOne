# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Clean-room subset of uvicorn: run an app through the platform server."""


class _Connection:
    """One upgraded connection, as the platform server drives it."""

    def __init__(self, session):
        self.session = session

    def _check(self):
        s = self.session
        if s.error is not None:
            e = s.error
            s.error = None
            raise e

    def text(self, value):
        if not self.session.done:
            self.session.deliver({"type": "websocket.receive", "text": value})
        self._check()

    def binary(self, value):
        if not self.session.done:
            self.session.deliver({"type": "websocket.receive", "bytes": bytes(value)})
        self._check()

    def closed(self, code, reason):
        if not self.session.done:
            self.session.deliver({"type": "websocket.disconnect", "code": code, "reason": reason})
        self._check()


def _websockets(app):
    def open_(path, query, headers, send):
        def sink(m):
            t = m["type"]
            if t == "websocket.accept":
                send("accept", m.get("subprotocol"), None)
            elif t == "websocket.send":
                if m.get("text") is not None:
                    send("text", m["text"], None)
                else:
                    send("bytes", m["bytes"], None)
            elif t == "websocket.close":
                send("close", m.get("code", 1000), m.get("reason") or "")
        conn = _Connection(app.websocket_open(path, query, headers, sink))
        conn._check()
        return conn
    return open_


def run(app, host="127.0.0.1", port=8000, **kw):
    import _cn1server
    if hasattr(app, "websocket_open"):
        _cn1server.serve(app.dispatch, host, port, _websockets(app))
    else:
        _cn1server.serve(app.dispatch, host, port)
