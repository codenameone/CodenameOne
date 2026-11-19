# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""ssl: contexts and their settings, for code that builds one to hand on.

TLS is the platform's: the HTTP stack verifies certificates against the
device's trust store, so a context here records what was asked of it and is
accepted by the HTTP clients, but cannot wrap a socket.
"""

import enum as _enum
import socket as _socket

OPENSSL_VERSION = "Codename One platform TLS"
OPENSSL_VERSION_INFO = (3, 0, 0, 0, 0)
HAS_SNI = True
HAS_ALPN = True
HAS_NEVER_CHECK_COMMON_NAME = True


class VerifyMode(_enum.IntEnum):
    CERT_NONE = 0
    CERT_OPTIONAL = 1
    CERT_REQUIRED = 2


CERT_NONE = VerifyMode.CERT_NONE
CERT_OPTIONAL = VerifyMode.CERT_OPTIONAL
CERT_REQUIRED = VerifyMode.CERT_REQUIRED


class VerifyFlags(_enum.IntFlag):
    VERIFY_DEFAULT = 0
    VERIFY_CRL_CHECK_LEAF = 4
    VERIFY_CRL_CHECK_CHAIN = 12
    VERIFY_X509_STRICT = 32
    VERIFY_X509_TRUSTED_FIRST = 32768
    VERIFY_X509_PARTIAL_CHAIN = 524288


VERIFY_DEFAULT = VerifyFlags.VERIFY_DEFAULT
VERIFY_X509_STRICT = VerifyFlags.VERIFY_X509_STRICT
VERIFY_X509_PARTIAL_CHAIN = VerifyFlags.VERIFY_X509_PARTIAL_CHAIN
VERIFY_X509_TRUSTED_FIRST = VerifyFlags.VERIFY_X509_TRUSTED_FIRST


class _SSLMethod(_enum.IntEnum):
    PROTOCOL_TLS = 2
    PROTOCOL_TLS_CLIENT = 16
    PROTOCOL_TLS_SERVER = 17


PROTOCOL_TLS = _SSLMethod.PROTOCOL_TLS
PROTOCOL_SSLv23 = PROTOCOL_TLS
PROTOCOL_TLS_CLIENT = _SSLMethod.PROTOCOL_TLS_CLIENT
PROTOCOL_TLS_SERVER = _SSLMethod.PROTOCOL_TLS_SERVER


class Options(_enum.IntFlag):
    OP_ALL = 0x80000054
    OP_NO_SSLv2 = 0
    OP_NO_SSLv3 = 0x2000000
    OP_NO_TLSv1 = 0x4000000
    OP_NO_TLSv1_1 = 0x10000000
    OP_NO_TLSv1_2 = 0x8000000
    OP_NO_TLSv1_3 = 0x20000000
    OP_NO_COMPRESSION = 0x20000
    OP_NO_RENEGOTIATION = 0x40000000


OP_ALL = Options.OP_ALL
OP_NO_SSLv2 = Options.OP_NO_SSLv2
OP_NO_SSLv3 = Options.OP_NO_SSLv3
OP_NO_TLSv1 = Options.OP_NO_TLSv1
OP_NO_TLSv1_1 = Options.OP_NO_TLSv1_1
OP_NO_TLSv1_2 = Options.OP_NO_TLSv1_2
OP_NO_TLSv1_3 = Options.OP_NO_TLSv1_3
OP_NO_COMPRESSION = Options.OP_NO_COMPRESSION
OP_NO_RENEGOTIATION = Options.OP_NO_RENEGOTIATION


class TLSVersion(_enum.IntEnum):
    MINIMUM_SUPPORTED = -2
    SSLv3 = 768
    TLSv1 = 769
    TLSv1_1 = 770
    TLSv1_2 = 771
    TLSv1_3 = 772
    MAXIMUM_SUPPORTED = -1


class Purpose(_enum.Enum):
    SERVER_AUTH = "1.3.6.1.5.5.7.3.1"
    CLIENT_AUTH = "1.3.6.1.5.5.7.3.2"


class SSLError(OSError):
    pass


class SSLZeroReturnError(SSLError):
    pass


class SSLWantReadError(SSLError):
    pass


class SSLWantWriteError(SSLError):
    pass


class SSLSyscallError(SSLError):
    pass


class SSLEOFError(SSLError):
    pass


class SSLCertVerificationError(SSLError, ValueError):
    pass


CertificateError = SSLCertVerificationError


class SSLContext:
    def __init__(self, protocol=PROTOCOL_TLS_CLIENT):
        self.protocol = protocol
        client = protocol in (PROTOCOL_TLS_CLIENT,)
        self.verify_mode = CERT_REQUIRED if client else CERT_NONE
        self.check_hostname = client
        self.verify_flags = VERIFY_DEFAULT
        self.options = Options.OP_ALL | Options.OP_NO_SSLv3
        self.minimum_version = TLSVersion.MINIMUM_SUPPORTED
        self.maximum_version = TLSVersion.MAXIMUM_SUPPORTED
        self.post_handshake_auth = False
        self.keylog_filename = None
        self._alpn = []
        self._ca = []
        self._chain = None

    def load_default_certs(self, purpose=Purpose.SERVER_AUTH):
        pass

    def load_verify_locations(self, cafile=None, capath=None, cadata=None):
        self._ca.append((cafile, capath, cadata))

    def load_cert_chain(self, certfile, keyfile=None, password=None):
        self._chain = (certfile, keyfile)

    def set_default_verify_paths(self):
        pass

    def set_alpn_protocols(self, protocols):
        self._alpn = list(protocols)

    def set_ciphers(self, ciphers):
        pass

    def get_ciphers(self):
        return []

    def set_npn_protocols(self, protocols):
        pass

    def get_ca_certs(self, binary_form=False):
        return []

    def cert_store_stats(self):
        return {"x509": 0, "crl": 0, "x509_ca": 0}

    def wrap_socket(self, sock, server_side=False, do_handshake_on_connect=True,
                    suppress_ragged_eofs=True, server_hostname=None, session=None):
        raise SSLError("TLS over a raw socket is not available; the HTTP client handles TLS")

    def wrap_bio(self, incoming, outgoing, server_side=False, server_hostname=None, session=None):
        raise SSLError("memory BIO TLS is not available; the HTTP client handles TLS")


class MemoryBIO:
    def __init__(self):
        self._buf = b""
        self.eof = False

    def write(self, b):
        self._buf += bytes(b)
        return len(b)

    def read(self, n=-1):
        if n < 0:
            n = len(self._buf)
        r, self._buf = self._buf[:n], self._buf[n:]
        return r

    def write_eof(self):
        self.eof = True

    @property
    def pending(self):
        return len(self._buf)


class SSLSocket(_socket.socket):
    pass


class SSLObject:
    pass


def create_default_context(purpose=Purpose.SERVER_AUTH, *, cafile=None, capath=None, cadata=None):
    ctx = SSLContext(PROTOCOL_TLS_CLIENT if purpose == Purpose.SERVER_AUTH else PROTOCOL_TLS_SERVER)
    if cafile or capath or cadata:
        ctx.load_verify_locations(cafile, capath, cadata)
    return ctx


_create_default_https_context = create_default_context


def _create_unverified_context(*args, **kwargs):
    ctx = SSLContext(PROTOCOL_TLS_CLIENT)
    ctx.check_hostname = False
    ctx.verify_mode = CERT_NONE
    return ctx


class DefaultVerifyPaths:
    cafile = None
    capath = None
    openssl_cafile_env = "SSL_CERT_FILE"
    openssl_cafile = ""
    openssl_capath_env = "SSL_CERT_DIR"
    openssl_capath = ""


def get_default_verify_paths():
    return DefaultVerifyPaths()
