#!/usr/bin/env python3
"""Fixture servers for tests/web-mtls-h2.sh.

Two HTTPS servers, both REQUIRING a client certificate signed by the fixture CA:

  --h1 PORT  HTTP/1.1 only (ALPN offers http/1.1 and nothing else)
  --h2 PORT  HTTP/2 only   (ALPN offers h2), on the python h2 library

Each answers "cn=NAME conn=PORT proto=VERSION": the verified client
certificate's common name, the client's source port and the protocol spoken.
/slow waits three seconds first. The HTTP/1.1 server also answers /count with
how many requests it has handled, /count excluded -- which is how the check
proves a request refused for want of HTTP/2 never reached it.
"""
import argparse
import http.server
import socket
import ssl
import threading
import time

COUNT = [0]


def context(d, alpn):
    ctx = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
    ctx.load_cert_chain(d + "/server.crt", d + "/server.key")
    ctx.load_verify_locations(d + "/ca.crt")
    ctx.verify_mode = ssl.CERT_REQUIRED
    ctx.set_alpn_protocols(alpn)
    return ctx


def common_name(sock):
    cert = sock.getpeercert() or {}
    for rdn in cert.get("subject", ()):
        for key, value in rdn:
            if key == "commonName":
                return value
    return "?"


class H1(http.server.BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def do_GET(self):
        if self.path != "/count":
            COUNT[0] += 1
        if self.path == "/slow":
            time.sleep(3)
        if self.path == "/count":
            body = "count=%d" % COUNT[0]
        else:
            body = "cn=%s conn=%d proto=HTTP/1.1" % (common_name(self.connection),
                                                     self.client_address[1])
        data = body.encode()
        self.send_response(200)
        self.send_header("Content-Type", "text/plain")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    do_POST = do_GET

    def log_message(self, *args):
        pass


def serve_h1(port, d):
    server = http.server.ThreadingHTTPServer(("0.0.0.0", port), H1)
    server.socket = context(d, ["http/1.1"]).wrap_socket(server.socket, server_side=True)
    server.serve_forever()


def h2_connection(raw, ctx):
    import h2.config
    import h2.connection
    import h2.events
    try:
        tls = ctx.wrap_socket(raw, server_side=True)
    except (ssl.SSLError, OSError):
        raw.close()
        return
    conn = h2.connection.H2Connection(config=h2.config.H2Configuration(client_side=False))
    conn.initiate_connection()
    tls.sendall(conn.data_to_send())
    cn = common_name(tls)
    port = tls.getpeername()[1]
    try:
        while True:
            data = tls.recv(65535)
            if not data:
                break
            for event in conn.receive_data(data):
                if isinstance(event, h2.events.RequestReceived):
                    path = dict((k.decode() if isinstance(k, bytes) else k,
                                 v.decode() if isinstance(v, bytes) else v)
                                for k, v in event.headers).get(":path", "/")
                    if path == "/slow":
                        time.sleep(3)
                    body = ("cn=%s conn=%d proto=HTTP/2" % (cn, port)).encode()
                    conn.send_headers(event.stream_id, [(":status", "200"),
                                                        ("content-type", "text/plain"),
                                                        ("content-length", str(len(body)))])
                    conn.send_data(event.stream_id, body, end_stream=True)
            tls.sendall(conn.data_to_send())
    except Exception:
        pass
    finally:
        tls.close()


def serve_h2(port, d):
    ctx = context(d, ["h2"])
    listener = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    listener.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    listener.bind(("0.0.0.0", port))
    listener.listen(16)
    while True:
        raw, _ = listener.accept()
        threading.Thread(target=h2_connection, args=(raw, ctx), daemon=True).start()


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--dir", required=True)
    parser.add_argument("--h1", type=int, required=True)
    parser.add_argument("--h2", type=int, required=True)
    args = parser.parse_args()
    threading.Thread(target=serve_h1, args=(args.h1, args.dir), daemon=True).start()
    serve_h2(args.h2, args.dir)
