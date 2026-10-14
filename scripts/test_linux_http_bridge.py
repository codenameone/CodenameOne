#!/usr/bin/env python3
"""Compile/run the real Linux HTTP bridge without the VM or a display.

Requires a C compiler, libcurl headers, pkg-config, Python 3 and openssl. The
binary deliberately links without libcurl. --build-only DIR exports it for
cross-distro checks; --binary PATH runs an exported binary on another distro.
Only Java string/array allocation and GC parking are stubbed.
"""
import argparse
import http.server
import os
from pathlib import Path
import shlex
import ssl
import subprocess
import tempfile
import threading

REPO = Path(__file__).resolve().parent.parent
GLOBALS = r'''
#include <stdint.h>
typedef void* JAVA_OBJECT;
typedef int JAVA_INT;
typedef int64_t JAVA_LONG;
typedef int JAVA_BOOLEAN;
#define JAVA_VOID void
#define JAVA_NULL ((void*)0)
#define CODENAME_ONE_THREAD_STATE void* threadStateData __attribute__((unused))
#define CN1_ARRAY_DATA(a) (a)
#define CN1_WRITE_BARRIER(a, b) ((void)0)
#define CN1_YIELD_THREAD ((void)0)
#define CN1_RESUME_THREAD ((void)0)
struct clazz { int unused; };
'''
HARNESS = r'''
#undef NDEBUG
#include <assert.h>
struct clazz class_array1__java_lang_String;
JAVA_OBJECT newStringFromCString(CODENAME_ONE_THREAD_STATE, const char* str) { return strdup(str); }
const char* stringToUTF8(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT str) { return str; }
char* cn1LinuxJStrDup(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT str) { return strdup(str); }
JAVA_OBJECT allocArray(CODENAME_ONE_THREAD_STATE, int n, struct clazz* type, int size, int dim) {
    (void) type; (void) dim; return calloc(n, size);
}
#define openHttp com_codename1_impl_linux_LinuxNative_httpOpen___java_lang_String_boolean_boolean_R_long
#define closeHttp com_codename1_impl_linux_LinuxNative_httpClose___long
#define responseCode com_codename1_impl_linux_LinuxNative_httpResponseCode___long_R_int
#define failure com_codename1_impl_linux_LinuxNative_httpFailure___long_R_java_lang_String
static void* openConcurrently(void* url) {
    JAVA_LONG peer = openHttp(NULL, url, 1, 0);
    assert(peer != 0);
    closeHttp(NULL, peer);
    return NULL;
}
int main(int argc, char** argv) {
    assert(argc >= 3);
    if (strcmp(argv[1], "unavailable") == 0) {
        assert(openHttp(NULL, argv[2], 1, 0) == 0);
        assert(openHttp(NULL, argv[2], 1, 0) == 0);
        return 0;
    }
    pthread_t workers[8];
    for (int i = 0; i < 8; i++) assert(!pthread_create(&workers[i], NULL, openConcurrently, argv[2]));
    for (int i = 0; i < 8; i++) assert(!pthread_join(workers[i], NULL));
    JAVA_LONG peer = openHttp(NULL, argv[2], 1, 1);
    assert(peer != 0);
    if (argc == 4) {
        /* Trust only this test's CA; production TLS defaults stay untouched. */
        assert(curl_easy_setopt(((CN1Http*)(intptr_t)peer)->easy, CURLOPT_CAINFO, argv[3]) == CURLE_OK);
    }
    com_codename1_impl_linux_LinuxNative_httpSetConnectTimeout___long_int(NULL, peer, 2000);
    com_codename1_impl_linux_LinuxNative_httpSetReadTimeout___long_int(NULL, peer, 2000);
    com_codename1_impl_linux_LinuxNative_httpSetHeader___long_java_lang_String_java_lang_String(
        NULL, peer, "X-CN1-Test", "bridge");
    if (strcmp(argv[1], "post") == 0) {
        com_codename1_impl_linux_LinuxNative_httpSetMethod___long_boolean(NULL, peer, 1);
        assert(com_codename1_impl_linux_LinuxNative_httpWriteBody___long_byte_1ARRAY_int_int_R_int(
            NULL, peer, "payload", 0, 7) == 7);
    }
    int status = responseCode(NULL, peer);
    char* error = failure(NULL, peer);
    if (strcmp(argv[1], "reject-tls") == 0) {
        assert(status == 0 && error != NULL);
        assert(strstr(error, "certificate") != NULL);
        assert(((CN1Http*)(intptr_t)peer)->respLen == 0);
        free(error);
    } else {
        if (error) fprintf(stderr, "HTTP bridge failure: %s\n", error);
        assert(status == 200 && error == NULL);
        char bytes[32] = {0};
        assert(com_codename1_impl_linux_LinuxNative_httpReadBody___long_byte_1ARRAY_int_int_R_int(
            NULL, peer, bytes, 0, sizeof(bytes)) == 6);
        assert(strcmp(bytes, "bridge") == 0);
        assert(com_codename1_impl_linux_LinuxNative_httpReadBody___long_byte_1ARRAY_int_int_R_int(
            NULL, peer, bytes, 0, sizeof(bytes)) == -1);
    }
    closeHttp(NULL, peer);
    return 0;
}
'''


def build(directory):
    directory.mkdir(parents=True, exist_ok=True)
    (directory / 'cn1_globals.h').write_text(GLOBALS)
    source = REPO / 'Ports/LinuxPort/nativeSources/cn1_linux_net.c'
    (directory / 'http.c').write_text('#include "' + str(source) + '"\n' + HARNESS)
    flags = shlex.split(subprocess.check_output(['pkg-config', '--cflags', 'libcurl'], text=True))
    binary = directory / 'http-bridge'
    subprocess.run(shlex.split(os.environ.get('CC', 'cc')) +
                   ['-std=c11', '-O2', '-Wall', '-Wextra', '-Werror', '-pthread',
                    '-I' + str(directory), str(directory / 'http.c'), '-o', str(binary), '-ldl'] + flags,
                   check=True)
    dynamic = subprocess.check_output(['readelf', '-dW', str(binary)], text=True)
    symbols = subprocess.check_output(['readelf', '-sW', str(binary)], text=True)
    assert 'libcurl' not in dynamic, dynamic
    assert 'CURL_OPENSSL' not in symbols, symbols
    # A present but incompatible library must fail cleanly, with one diagnostic.
    bad = directory / 'incompatible'
    bad.mkdir(exist_ok=True)
    (bad / 'curl.c').write_text('void* curl_easy_init(void) { return 0; }\n')
    subprocess.run(shlex.split(os.environ.get('CC', 'cc')) +
                   ['-shared', '-fPIC', str(bad / 'curl.c'), '-o', str(bad / 'libcurl.so.4')], check=True)
    env = dict(os.environ, LD_LIBRARY_PATH=str(bad))
    result = subprocess.run([str(binary), 'unavailable', 'http://127.0.0.1/'],
                            env=env, text=True, capture_output=True, check=True, timeout=10)
    assert result.stderr.count('libcurl.so.4 is missing') == 1, result.stderr
    # Also exercise dlopen failure without changing the system library.
    broken = directory / 'unloadable'
    broken.mkdir(exist_ok=True)
    (broken / 'libcurl.so.4').write_text('not an ELF library\n')
    env = dict(os.environ, LD_LIBRARY_PATH=str(broken))
    result = subprocess.run([str(binary), 'unavailable', 'http://127.0.0.1/'],
                            env=env, text=True, capture_output=True, check=True, timeout=10)
    assert result.stderr.count('unable to load libcurl.so.4') == 1, result.stderr
    print('PASS: no libcurl linkage/version tags; incompatible/unloadable libraries fail cleanly')
    return binary


class Handler(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        self.respond()

    def do_POST(self):
        assert self.rfile.read(int(self.headers['Content-Length'])) == b'payload'
        self.respond()

    def respond(self):
        assert self.headers['X-CN1-Test'] == 'bridge'
        self.send_response(200)
        self.send_header('Content-Length', '6')
        self.end_headers()
        self.wfile.write(b'bridge')

    def log_message(self, *args):
        pass


def run(binary, directory):
    cert, key = directory / 'cert.pem', directory / 'key.pem'
    subprocess.run(['openssl', 'req', '-x509', '-newkey', 'rsa:2048', '-nodes',
                    '-keyout', str(key), '-out', str(cert), '-days', '1',
                    '-subj', '/CN=localhost', '-addext', 'subjectAltName=IP:127.0.0.1'],
                   check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    for tls in (False, True):
        server = http.server.ThreadingHTTPServer(('127.0.0.1', 0), Handler)
        if tls:
            context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
            context.load_cert_chain(cert, key)
            server.socket = context.wrap_socket(server.socket, server_side=True)
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        try:
            url = ('https' if tls else 'http') + '://127.0.0.1:' + str(server.server_port) + '/'
            for method in ('get', 'post'):
                args = [str(binary), method, url] + ([str(cert)] if tls else [])
                result = subprocess.run(args, check=True, capture_output=True, text=True, timeout=15)
                assert not result.stderr, result.stderr
            if tls:
                result = subprocess.run([str(binary), 'reject-tls', url], check=True,
                                        capture_output=True, text=True, timeout=15)
                assert not result.stderr, result.stderr
        finally:
            server.shutdown()
            server.server_close()
            thread.join()
    print('PASS: concurrent initialization, HTTP/HTTPS GET/POST, untrusted TLS rejection')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--build-only', type=Path)
    parser.add_argument('--binary', type=Path)
    args = parser.parse_args()
    if args.build_only:
        build(args.build_only.resolve())
    else:
        with tempfile.TemporaryDirectory() as temporary:
            directory = Path(temporary)
            run(args.binary.resolve() if args.binary else build(directory), directory)
