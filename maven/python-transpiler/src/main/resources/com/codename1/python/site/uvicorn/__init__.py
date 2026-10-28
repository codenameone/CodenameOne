# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Clean-room subset of uvicorn: run an app through the platform server."""


class _Connection:
    """One upgraded connection, as the platform server drives it.

    The endpoint runs as a task on the shared websocket loop; these methods
    are called on the server's threads and only hand the message over.
    """

    def __init__(self, session):
        self.session = session

    def text(self, value):
        self.session.deliver({"type": "websocket.receive", "text": value})

    def binary(self, value):
        self.session.deliver({"type": "websocket.receive", "bytes": bytes(value)})

    def closed(self, code, reason):
        self.session.deliver({"type": "websocket.disconnect", "code": code, "reason": reason})


def _websockets(app):
    def open_(path, query, headers, send):
        state = {"accepted": False, "closed": False}

        def sink(m):
            t = m["type"]
            if t == "websocket.accept":
                state["accepted"] = True
                send("accept", m.get("subprotocol"), None)
            elif t == "websocket.send":
                if m.get("text") is not None:
                    send("text", m["text"], None)
                else:
                    send("bytes", m["bytes"], None)
            elif t == "websocket.close":
                state["closed"] = True
                send("close", m.get("code", 1000), m.get("reason") or "")
            elif t == "websocket.http.response":
                state["closed"] = True
                send("deny", m["status"], None)

        def done(session):
            e = session.error
            if e is not None:
                session.error = None
                import traceback
                traceback.print_exception(e)
            if not state["closed"]:
                state["closed"] = True
                if state["accepted"]:
                    send("close", 1011 if e is not None else 1000, "")
                else:
                    send("deny", 500 if e is not None else 403, None)

        return _Connection(app.websocket_open(path, query, headers, sink, done))
    return open_


def run(app, host="127.0.0.1", port=8000, **kw):
    import _cn1server
    if hasattr(app, "websocket_open"):
        _cn1server.serve(app.dispatch, host, port, _websockets(app))
    else:
        _cn1server.serve(app.dispatch, host, port)
