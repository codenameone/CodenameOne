#!/usr/bin/env python3
"""Drives a running Python backend sample over real sockets, stdlib only.

    e2e_client.py fastapi|flask <port>

Exits non-zero, naming the check, on the first failure.
"""
import base64
import json
import os
import socket
import struct
import sys
import threading
import time
import urllib.error
import urllib.request

HOST = "127.0.0.1"
PORT = 0
FAILED = []


def check(name, cond, detail=""):
    print(("ok   " if cond else "FAIL ") + name + ("" if cond else "  -- " + str(detail)))
    if not cond:
        FAILED.append(name)


def http(method, path, body=None, headers=None):
    req = urllib.request.Request("http://%s:%d%s" % (HOST, PORT, path), data=body,
                                 method=method, headers=headers or {})
    try:
        with urllib.request.urlopen(req, timeout=10) as r:
            return r.status, r.headers, r.read()
    except urllib.error.HTTPError as e:
        return e.code, e.headers, e.read()


def timed_stream(path):
    """The (seconds, bytes) of each read of a streamed body, raw HTTP/1.1."""
    s = socket.create_connection((HOST, PORT), timeout=10)
    s.sendall(("GET %s HTTP/1.1\r\nHost: x\r\nConnection: close\r\n\r\n" % path).encode())
    start = time.time()
    data = b""
    arrivals = []
    while True:
        b = s.recv(65536)
        if not b:
            break
        data += b
        arrivals.append((time.time() - start, b))
    s.close()
    head, _, body = data.partition(b"\r\n\r\n")
    return head.decode("latin-1"), dechunk(body), arrivals


def dechunk(body):
    out = b""
    while body:
        line, _, rest = body.partition(b"\r\n")
        n = int(line.split(b";")[0], 16)
        if n == 0:
            break
        out += rest[:n]
        body = rest[n + 2:]
    return out


def first_arrival_of(arrivals, needle):
    seen = b""
    for t, b in arrivals:
        seen += b
        if needle in seen:
            return t
    return None


class WS:
    """A minimal RFC 6455 client: masked frames out, unmasked in."""

    def __init__(self, path, protocols=None):
        self.sock = socket.create_connection((HOST, PORT), timeout=10)
        key = base64.b64encode(os.urandom(16)).decode()
        lines = ["GET %s HTTP/1.1" % path, "Host: %s:%d" % (HOST, PORT), "Upgrade: websocket",
                 "Connection: Upgrade", "Sec-WebSocket-Key: " + key, "Sec-WebSocket-Version: 13"]
        if protocols:
            lines.append("Sec-WebSocket-Protocol: " + protocols)
        self.sock.sendall(("\r\n".join(lines) + "\r\n\r\n").encode())
        self.buf = b""
        while b"\r\n\r\n" not in self.buf:
            b = self.sock.recv(4096)
            if not b:
                break
            self.buf += b
        head, _, self.buf = self.buf.partition(b"\r\n\r\n")
        rows = head.decode("latin-1").split("\r\n")
        self.status = int(rows[0].split()[1])
        self.headers = {}
        for r in rows[1:]:
            k, _, v = r.partition(":")
            self.headers[k.strip().lower()] = v.strip()
        if self.status != 101:
            n = int(self.headers.get("content-length", "0"))
            while len(self.buf) < n:
                b = self.sock.recv(4096)
                if not b:
                    break
                self.buf += b
            self.body = self.buf[:n]

    def _read(self, n):
        while len(self.buf) < n:
            b = self.sock.recv(4096)
            if not b:
                raise EOFError("connection closed")
            self.buf += b
        out, self.buf = self.buf[:n], self.buf[n:]
        return out

    def send(self, text, opcode=1):
        payload = text.encode() if isinstance(text, str) else text
        mask = os.urandom(4)
        n = len(payload)
        if n < 126:
            hdr = struct.pack("!BB", 0x80 | opcode, 0x80 | n)
        else:
            hdr = struct.pack("!BBH", 0x80 | opcode, 0x80 | 126, n)
        self.sock.sendall(hdr + mask + bytes(c ^ mask[i % 4] for i, c in enumerate(payload)))

    def recv(self):
        """(opcode, payload) of the next frame."""
        b0, b1 = self._read(2)
        n = b1 & 0x7F
        if n == 126:
            n = struct.unpack("!H", self._read(2))[0]
        elif n == 127:
            n = struct.unpack("!Q", self._read(8))[0]
        return b0 & 0x0F, self._read(n)

    def recv_text(self):
        op, p = self.recv()
        if op == 8:
            raise EOFError("close %d" % (struct.unpack("!H", p[:2])[0] if len(p) >= 2 else 1005))
        return p.decode()

    def close(self):
        try:
            self.send(struct.pack("!H", 1000), opcode=8)
        except OSError:
            pass
        self.sock.close()


def streaming(path, marker_first, marker_last):
    head, body, arrivals = timed_stream(path)
    check(path + " 200", head.startswith("HTTP/1.1 200"), head.split("\r\n")[0])
    check(path + " is chunked", "transfer-encoding: chunked" in head.lower(), head)
    check(path + " has no content-length", "content-length" not in head.lower(), head)
    first = first_arrival_of(arrivals, marker_first)
    last = first_arrival_of(arrivals, marker_last)
    check(path + " first chunk before the generator finishes",
          first is not None and last is not None and first < 0.9 and last >= 1.8,
          "first %s last %s" % (first, last))
    return body


def concurrency():
    """A slow stream on one connection must not hold up a request on another."""
    result = {}

    def slow():
        result["stream"] = timed_stream("/stream")
    t = threading.Thread(target=slow)
    t.start()
    time.sleep(0.3)
    start = time.time()
    st, h, b = http("GET", "/health")
    took = time.time() - start
    stream_running = t.is_alive()
    t.join()
    check("request beside a slow stream answers", st == 200, st)
    check("request beside a slow stream is not held up",
          took < 0.5 and stream_running, "took %.2fs, stream still running %s" % (took, stream_running))
    check("the slow stream still completes",
          result.get("stream", ("", b"", []))[1] == b"chunk 0\nchunk 1\nchunk 2\n", result.get("stream"))


def background_tasks():
    """Background tasks run after the response is sent, as in Starlette."""
    http("DELETE", "/events")
    start = time.time()
    st, h, b = http("GET", "/background")
    took = time.time() - start
    check("GET /background 200", st == 200 and json.loads(b) == {"queued": True}, (st, b))
    check("response does not wait for its background task", took < 0.4, "took %.2fs" % took)
    time.sleep(1.0)
    st, h, b = http("GET", "/events")
    check("background task ran after the handler",
          json.loads(b)["events"] == ["handler", "task plain"], b)

    http("DELETE", "/events")
    head, body, arrivals = timed_stream("/background-stream")
    check("/background-stream body", body == b"bg 0\nbg 1\n", body)
    check("/background-stream ends before its task", arrivals and arrivals[-1][0] < 0.9, arrivals)
    time.sleep(1.0)
    st, h, b = http("GET", "/events")
    check("streamed background task ran after the last chunk",
          json.loads(b)["events"] == ["stream done", "task stream"], b)


def fastapi():
    st, h, b = http("GET", "/health")
    check("GET /health 200", st == 200, st)
    check("GET /health body", json.loads(b) == {"ok": True}, b)
    check("http middleware header", h.get("x-handled-by") == "timing", dict(h))
    check("pure ASGI middleware header", h.get("x-asgi") == "stamped", dict(h))
    st, h, b = http("POST", "/messages", json.dumps({"text": "hi"}).encode(),
                    {"Content-Type": "application/json"})
    check("POST /messages 201", st == 201, (st, b))
    st, h, b = http("GET", "/messages")
    check("GET /messages", st == 200 and json.loads(b) == {"messages": ["hi"]}, b)
    st, h, b = http("GET", "/blocked")
    check("middleware 401", st == 401 and b"blocked" in b, (st, b))
    st, h, b = http("GET", "/nope")
    check("404", st == 404, st)

    body = streaming("/stream", b"chunk 0", b"chunk 2")
    check("/stream body", body == b"chunk 0\nchunk 1\nchunk 2\n", body)
    body = streaming("/astream", b"async 0", b"async 2")
    check("/astream body", body == b"async 0\nasync 1\nasync 2\n", body)

    ws = WS("/ws/echo")
    check("ws echo 101", ws.status == 101, ws.status)
    ws.send("hello")
    check("ws echo reply", ws.recv_text() == "echo: hello")
    ws.send("again")
    check("ws echo second reply", ws.recv_text() == "echo: again")
    ws.close()

    ws = WS("/ws/ticker")
    check("ws ticker 101", ws.status == 101, ws.status)
    ticks = [ws.recv_text() for _ in range(3)]
    check("ws ticker background sender", ticks == ["tick 0", "tick 1", "tick 2"], ticks)
    ws.send("stop")
    closed = None
    try:
        for _ in range(20):
            ws.recv_text()
    except EOFError as e:
        closed = str(e)
    check("ws ticker closes 1000 on stop", closed == "close 1000", closed)
    ws.sock.close()

    ws = WS("/ws/forbidden")
    check("ws close-before-accept is 403", ws.status == 403, ws.status)
    ws.sock.close()

    ws = WS("/ws/private")
    check("ws ASGI middleware refuses 401", ws.status == 401, ws.status)
    check("ws refusal body", getattr(ws, "body", b"") == b"no token", getattr(ws, "body", None))
    ws.sock.close()
    ws = WS("/ws/private?token=secret")
    check("ws private with token 101", ws.status == 101, ws.status)
    check("ws private welcome", ws.recv_text() == "welcome")
    ws.sock.close()

    ws = WS("/ws/proto", protocols="chat.v0, chat.v1")
    check("ws proto 101", ws.status == 101, ws.status)
    check("ws subprotocol negotiated", ws.headers.get("sec-websocket-protocol") == "chat.v1", ws.headers)
    check("ws accept header", ws.headers.get("x-room") == "lobby", ws.headers)
    check("ws proto message", ws.recv_text() == "proto ok")
    ws.sock.close()

    ws = WS("/ws/missing")
    check("ws unknown route refused", ws.status == 403, ws.status)
    ws.sock.close()

    st, h, b = http("GET", "/health")
    check("still serving after websockets", st == 200, st)

    concurrency()
    background_tasks()


def flask():
    st, h, b = http("GET", "/")
    check("GET / answers", st in (200, 404), st)
    body = streaming("/stream?who=bob", b"bob 0", b"bob 2")
    check("flask /stream body with request context", body == b"bob 0\nbob 1\nbob 2\n", body)
    st, h, b = http("GET", "/badge.png?who=bob")
    check("flask /badge.png is a 240x64 PNG", st == 200 and b[:8] == b"\x89PNG\r\n\x1a\n"
          and struct.unpack(">II", b[16:24]) == (240, 64), (st, b[:24]))
    st, h, b = http("GET", "/badge.jpg?who=bob")
    check("flask /badge.jpg is a JPEG", st == 200 and b[:2] == b"\xff\xd8" and b[-2:] == b"\xff\xd9", (st, b[:4]))
    st, h, b = http("GET", "/badge.json?who=bob")
    info = json.loads(b) if st == 200 else {}
    box = info.get("bbox") or [0, 0, 0, 0]
    check("flask /badge.json text inked in the middle", st == 200 and info.get("name") == ["Aileron", "Regular"]
          and 60 < box[0] < 120 < box[2] < 180 and 14 < box[1] < 32 < box[3] < 50
          and abs(info.get("length", 0) - (box[2] - box[0])) < 6, info)


def main():
    global PORT
    which, PORT = sys.argv[1], int(sys.argv[2])
    {"fastapi": fastapi, "flask": flask}[which]()
    if FAILED:
        print("%d check(s) failed: %s" % (len(FAILED), ", ".join(FAILED)))
        sys.exit(1)
    print("all checks passed")


if __name__ == "__main__":
    main()
