#!/usr/bin/env python3
"""Exercise ZipTestServer over real sockets; --http2 also tests the native transport.

The fixture lives in vm/tests/src/test/resources/com/codename1/tools/translator/zipserver.
Run it on the JVM or package that directory with CN1_BACKEND_DEMO. No third-party
Python modules are required. Downloads go to the supplied artifact directory.
"""
import argparse
import http.client
import socket
import ssl
import struct
import subprocess
import time
import zipfile
from pathlib import Path


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('port', type=int)
    parser.add_argument('artifacts', type=Path)
    parser.add_argument('--http2', action='store_true')
    parser.add_argument('--ca', type=Path, help='Trust the TLS fixture certificate at this path')
    args = parser.parse_args()
    args.artifacts.mkdir(parents=True, exist_ok=True)

    def connect(protocol="http/1.1"):
        sock = socket.create_connection(('127.0.0.1', args.port), timeout=20)
        if args.ca:
            context = ssl.create_default_context(cafile=str(args.ca))
            context.set_alpn_protocols([protocol])
            sock = context.wrap_socket(sock, server_hostname='localhost')
        return sock

    def request(method, path):
        if args.ca:
            conn = http.client.HTTPSConnection('localhost', args.port, timeout=30,
                                              context=ssl.create_default_context(cafile=str(args.ca)))
        else:
            conn = http.client.HTTPConnection('127.0.0.1', args.port, timeout=30)
        conn.request(method, path)
        return conn, conn.getresponse()

    def metrics():
        conn, response = request('GET', '/metrics')
        try:
            assert response.status == 200
            return tuple(map(int, response.read().split()))
        finally:
            conn.close()

    def cleaned(before, cancelled=False):
        deadline = time.monotonic() + 15
        while time.monotonic() < deadline:
            current = metrics()
            if current[0] > before[0] and current[0] == current[1]:
                if cancelled:
                    assert current[2] - before[2] < 131072, 'Cancellation consumed the entire 1 GiB source'
                return
            time.sleep(.05)
        raise AssertionError(('Source leaked', before, current))

    def verify(path, size):
        with zipfile.ZipFile(path) as archive:
            assert archive.namelist() == ['project/', 'project/run.sh', 'project/empty', 'project/caf\u00e9.bin']
            for entry, mode in zip(archive.infolist(), [0o40755, 0o100755, 0o100644, 0o100644]):
                assert entry.create_system == 3 and entry.external_attr >> 16 == mode
                assert entry.flag_bits & 0x800
                assert entry.extra == b'UT\x05\x00\x01\x00\x00\x00\x00'
            assert archive.getinfo('project/caf\u00e9.bin').file_size == size
            assert archive.getinfo('project/empty').file_size == 0
            # Read with bounded memory and verify CRC all the way to EOF.
            for entry in archive.infolist():
                with archive.open(entry) as source:
                    while source.read(65536):
                        pass
        subprocess.run(['unzip', '-tq', str(path)], check=True)
        extracted = path.parent / (path.stem + '-extracted')
        subprocess.run(['unzip', '-oq', str(path), '-d', str(extracted)], check=True)
        result = subprocess.check_output([str(extracted / 'project/run.sh')])
        assert result.strip() == b'ZIP_STREAM_OK'

    deadline = time.monotonic() + 60
    while True:
        try:
            metrics()
            break
        except OSError:
            if time.monotonic() > deadline:
                raise
            time.sleep(.1)

    before = metrics()
    # Reuse a connection: HEAD/204 must not start a source or corrupt the next response.
    conn, response = request('HEAD', '/project.zip')
    assert response.status == 200 and response.getheader('Content-Length') is None
    assert response.read() == b''
    conn.request('GET', '/no-body')
    response = conn.getresponse()
    assert response.status == 204 and response.read() == b''
    conn.request('GET', '/metrics')
    response = conn.getresponse()
    assert tuple(map(int, response.read().split())) == before
    conn.close()

    for name, size in [('project', 1024), ('large', 64 * 1024 * 1024)]:
        path = args.artifacts / (name + '.zip')
        conn, response = request('GET', '/' + name + '.zip')
        assert response.status == 200 and response.chunked
        assert response.getheader('Content-Disposition') == 'attachment; filename=project.zip'
        with path.open('wb') as output:
            while True:
                data = response.read(65536)
                if not data:
                    break
                output.write(data)
        conn.close()
        verify(path, size)
    before = metrics()
    conn, response = request('GET', '/cancel.zip')
    assert len(response.read(65536)) == 65536
    # Close both response and socket so the server sees cancellation promptly.
    response.close()
    conn.close()
    cleaned(before, True)

    conn, response = request('GET', '/fail')
    try:
        response.read()
        raise AssertionError('Failed callback emitted a successful response terminator')
    except http.client.IncompleteRead:
        pass
    finally:
        conn.close()

    # HTTP/1.0 cannot use chunked framing and must close even when keep-alive is requested.
    with connect() as sock:
        sock.sendall(b'GET /project.zip HTTP/1.0\r\nHost: localhost\r\nConnection: keep-alive\r\n\r\n')
        response = http.client.HTTPResponse(sock)
        response.begin()
        assert not response.chunked and response.getheader('Content-Length') is None
        assert response.getheader('Connection') == 'close'
        assert response.read().startswith(b'PK')

    if args.http2:
        def frame(sock, kind, flags, stream, payload=b''):
            sock.sendall(len(payload).to_bytes(3, 'big') + bytes([kind, flags])
                         + struct.pack('>I', stream) + payload)

        def receive(sock):
            def exact(size):
                data = b''
                while len(data) < size:
                    part = sock.recv(size - len(data))
                    if not part:
                        raise EOFError('HTTP/2 connection closed')
                    data += part
                return data
            head = exact(9)
            return head[3], head[4], int.from_bytes(head[5:], 'big'), exact(int.from_bytes(head[:3], 'big'))

        def h2start(path, window=65535, method=b"\x82"):
            sock = connect("h2")
            sock.sendall(b'PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n')
            frame(sock, 4, 0, 0, struct.pack('>HI', 4, window))
            # HPACK: indexed GET/http, literal :path and :authority, without Huffman encoding.
            encoded = path.encode('ascii')
            frame(sock, 1, 5, 1, method + b'\x86\x04' + bytes([len(encoded)]) + encoded
                  + b'\x01\x09localhost')
            return sock

        before = metrics()
        for route, method in [('/project.zip', b'\x02\x04HEAD'), ('/no-body', b'\x82')]:
            with h2start(route, method=method) as sock:
                while True:
                    kind, flags, stream, data = receive(sock)
                    if kind == 4 and not flags & 1:
                        frame(sock, 4, 1, 0)
                    assert kind != 0, 'HEAD/204 emitted HTTP/2 DATA'
                    if stream == 1 and flags & 1:
                        break
        assert metrics() == before
        path = args.artifacts / 'http2.zip'
        with h2start('/large.zip', 1024) as sock, path.open('wb') as output:
            total = 0
            while True:
                kind, flags, stream, data = receive(sock)
                if kind == 4 and not flags & 1:
                    frame(sock, 4, 1, 0)
                if kind == 0 and stream == 1:
                    output.write(data)
                    total += len(data)
                    if total == len(data):
                        # Hold the window shut briefly: no producer-sized native buffer may accumulate.
                        time.sleep(.1)
                    if data:
                        frame(sock, 8, 0, 0, struct.pack('>I', len(data)))
                        frame(sock, 8, 0, 1, struct.pack('>I', len(data)))
                    if flags & 1:
                        break
                assert kind not in (3, 7), ('Unexpected reset/GOAWAY', kind, data)
        verify(path, 64 * 1024 * 1024)
        before = metrics()
        with h2start('/cancel.zip', 1024) as sock:
            while True:
                kind, flags, stream, data = receive(sock)
                if kind == 4 and not flags & 1:
                    frame(sock, 4, 1, 0)
                if kind == 0 and stream == 1:
                    frame(sock, 3, 0, 1, struct.pack('>I', 8))  # CANCEL, without closing the socket first.
                    cleaned(before, True)
                    # A stream reset must leave the connection usable for another request.
                    frame(sock, 1, 5, 3, b'\x82\x86\x04\x08/metrics\x01\x09localhost')
                    while True:
                        kind, flags, stream, data = receive(sock)
                        assert kind != 7, 'Stream cancellation closed the whole connection'
                        if stream == 3 and flags & 1:
                            break
                    break
        print('HTTP/2 flow control and RST_STREAM cleanup passed')
    assert metrics()[0] == metrics()[1]
    print('ZIP_STREAMING_OK', args.artifacts)


if __name__ == '__main__':
    main()
