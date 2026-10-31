# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Message digests: MD5 (RFC 1321), SHA-1 and the SHA-2 family (FIPS 180-4)."""

__all__ = ["md5", "sha1", "sha224", "sha256", "sha384", "sha512", "new", "algorithms_available",
           "algorithms_guaranteed", "pbkdf2_hmac", "file_digest"]

algorithms_guaranteed = frozenset(["md5", "sha1", "sha224", "sha256", "sha384", "sha512"])
algorithms_available = algorithms_guaranteed

_M32 = 0xFFFFFFFF
_M64 = 0xFFFFFFFFFFFFFFFF


def _rotl32(x, n):
    return ((x << n) | (x >> (32 - n))) & _M32


def _rotr32(x, n):
    return ((x >> n) | (x << (32 - n))) & _M32


def _rotr64(x, n):
    return ((x >> n) | (x << (64 - n))) & _M64


def _as_bytes(data):
    if isinstance(data, str):
        raise TypeError("Strings must be encoded before hashing")
    return bytes(data)


class _Hash:
    """Buffers input into blocks and hands each full block to `_compress`."""

    def __init__(self, data=b""):
        self._buf = b""
        self._count = 0
        self._h = list(self._IV)
        if data:
            self.update(data)

    def update(self, data):
        data = _as_bytes(data)
        self._count += len(data)
        buf = self._buf + data
        bs = self.block_size
        n = len(buf) - len(buf) % bs
        for i in range(0, n, bs):
            self._compress(buf[i:i + bs])
        self._buf = buf[n:]

    def copy(self):
        c = type(self).__new__(type(self))
        c._buf = self._buf
        c._count = self._count
        c._h = list(self._h)
        return c

    def _padded(self, length_bytes, order):
        bs = self.block_size
        bits = self._count * 8
        tail = self._buf + b"\x80"
        pad = (bs - length_bytes - len(tail)) % bs
        return tail + b"\x00" * pad + bits.to_bytes(length_bytes, order)

    def digest(self):
        c = self.copy()
        tail = c._padded(self._LEN_BYTES, self._ORDER)
        for i in range(0, len(tail), self.block_size):
            c._compress(tail[i:i + self.block_size])
        w = self._WORD
        out = b"".join(v.to_bytes(w, self._ORDER) for v in c._h)
        return out[:self.digest_size]

    def hexdigest(self):
        return self.digest().hex()


class _MD5(_Hash):
    name = "md5"
    digest_size = 16
    block_size = 64
    _IV = (0x67452301, 0xEFCDAB89, 0x98BADCFE, 0x10325476)
    _LEN_BYTES = 8
    _ORDER = "little"
    _WORD = 4
    _S = (7, 12, 17, 22) * 4 + (5, 9, 14, 20) * 4 + (4, 11, 16, 23) * 4 + (6, 10, 15, 21) * 4
    _K = (0xd76aa478, 0xe8c7b756, 0x242070db, 0xc1bdceee, 0xf57c0faf, 0x4787c62a, 0xa8304613,
          0xfd469501, 0x698098d8, 0x8b44f7af, 0xffff5bb1, 0x895cd7be, 0x6b901122, 0xfd987193,
          0xa679438e, 0x49b40821, 0xf61e2562, 0xc040b340, 0x265e5a51, 0xe9b6c7aa, 0xd62f105d,
          0x02441453, 0xd8a1e681, 0xe7d3fbc8, 0x21e1cde6, 0xc33707d6, 0xf4d50d87, 0x455a14ed,
          0xa9e3e905, 0xfcefa3f8, 0x676f02d9, 0x8d2a4c8a, 0xfffa3942, 0x8771f681, 0x6d9d6122,
          0xfde5380c, 0xa4beea44, 0x4bdecfa9, 0xf6bb4b60, 0xbebfbc70, 0x289b7ec6, 0xeaa127fa,
          0xd4ef3085, 0x04881d05, 0xd9d4d039, 0xe6db99e5, 0x1fa27cf8, 0xc4ac5665, 0xf4292244,
          0x432aff97, 0xab9423a7, 0xfc93a039, 0x655b59c3, 0x8f0ccc92, 0xffeff47d, 0x85845dd1,
          0x6fa87e4f, 0xfe2ce6e0, 0xa3014314, 0x4e0811a1, 0xf7537e82, 0xbd3af235, 0x2ad7d2bb,
          0xeb86d391)

    def _compress(self, block):
        m = [int.from_bytes(block[i:i + 4], "little") for i in range(0, 64, 4)]
        a, b, c, d = self._h
        for i in range(64):
            if i < 16:
                f = (b & c) | (~b & d)
                g = i
            elif i < 32:
                f = (d & b) | (~d & c)
                g = (5 * i + 1) % 16
            elif i < 48:
                f = b ^ c ^ d
                g = (3 * i + 5) % 16
            else:
                f = c ^ (b | (~d & _M32))
                g = (7 * i) % 16
            f = (f + a + self._K[i] + m[g]) & _M32
            a = d
            d = c
            c = b
            b = (b + _rotl32(f, self._S[i])) & _M32
        h = self._h
        self._h = [(h[0] + a) & _M32, (h[1] + b) & _M32, (h[2] + c) & _M32, (h[3] + d) & _M32]


class _SHA1(_Hash):
    name = "sha1"
    digest_size = 20
    block_size = 64
    _IV = (0x67452301, 0xEFCDAB89, 0x98BADCFE, 0x10325476, 0xC3D2E1F0)
    _LEN_BYTES = 8
    _ORDER = "big"
    _WORD = 4

    def _compress(self, block):
        w = [int.from_bytes(block[i:i + 4], "big") for i in range(0, 64, 4)]
        for i in range(16, 80):
            w.append(_rotl32(w[i - 3] ^ w[i - 8] ^ w[i - 14] ^ w[i - 16], 1))
        a, b, c, d, e = self._h
        for i in range(80):
            if i < 20:
                f = (b & c) | (~b & d)
                k = 0x5A827999
            elif i < 40:
                f = b ^ c ^ d
                k = 0x6ED9EBA1
            elif i < 60:
                f = (b & c) | (b & d) | (c & d)
                k = 0x8F1BBCDC
            else:
                f = b ^ c ^ d
                k = 0xCA62C1D6
            t = (_rotl32(a, 5) + (f & _M32) + e + k + w[i]) & _M32
            e = d
            d = c
            c = _rotl32(b, 30)
            b = a
            a = t
        self._h = [(x + y) & _M32 for x, y in zip(self._h, (a, b, c, d, e))]


_K256 = (0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4,
         0xab1c5ed5, 0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe,
         0x9bdc06a7, 0xc19bf174, 0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f,
         0x4a7484aa, 0x5cb0a9dc, 0x76f988da, 0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7,
         0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967, 0x27b70a85, 0x2e1b2138, 0x4d2c6dfc,
         0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85, 0xa2bfe8a1, 0xa81a664b,
         0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070, 0x19a4c116,
         0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
         0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7,
         0xc67178f2)


class _SHA256(_Hash):
    name = "sha256"
    digest_size = 32
    block_size = 64
    _IV = (0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a, 0x510e527f, 0x9b05688c, 0x1f83d9ab,
           0x5be0cd19)
    _LEN_BYTES = 8
    _ORDER = "big"
    _WORD = 4

    def _compress(self, block):
        w = [int.from_bytes(block[i:i + 4], "big") for i in range(0, 64, 4)]
        for i in range(16, 64):
            x = w[i - 15]
            y = w[i - 2]
            s0 = _rotr32(x, 7) ^ _rotr32(x, 18) ^ (x >> 3)
            s1 = _rotr32(y, 17) ^ _rotr32(y, 19) ^ (y >> 10)
            w.append((w[i - 16] + s0 + w[i - 7] + s1) & _M32)
        a, b, c, d, e, f, g, h = self._h
        for i in range(64):
            s1 = _rotr32(e, 6) ^ _rotr32(e, 11) ^ _rotr32(e, 25)
            ch = (e & f) ^ (~e & g)
            t1 = (h + s1 + ch + _K256[i] + w[i]) & _M32
            s0 = _rotr32(a, 2) ^ _rotr32(a, 13) ^ _rotr32(a, 22)
            maj = (a & b) ^ (a & c) ^ (b & c)
            t2 = (s0 + maj) & _M32
            h = g
            g = f
            f = e
            e = (d + t1) & _M32
            d = c
            c = b
            b = a
            a = (t1 + t2) & _M32
        self._h = [(x + y) & _M32 for x, y in zip(self._h, (a, b, c, d, e, f, g, h))]


class _SHA224(_SHA256):
    name = "sha224"
    digest_size = 28
    _IV = (0xc1059ed8, 0x367cd507, 0x3070dd17, 0xf70e5939, 0xffc00b31, 0x68581511, 0x64f98fa7,
           0xbefa4fa4)


_K512 = (0x428a2f98d728ae22, 0x7137449123ef65cd, 0xb5c0fbcfec4d3b2f, 0xe9b5dba58189dbbc,
         0x3956c25bf348b538, 0x59f111f1b605d019, 0x923f82a4af194f9b, 0xab1c5ed5da6d8118,
         0xd807aa98a3030242, 0x12835b0145706fbe, 0x243185be4ee4b28c, 0x550c7dc3d5ffb4e2,
         0x72be5d74f27b896f, 0x80deb1fe3b1696b1, 0x9bdc06a725c71235, 0xc19bf174cf692694,
         0xe49b69c19ef14ad2, 0xefbe4786384f25e3, 0x0fc19dc68b8cd5b5, 0x240ca1cc77ac9c65,
         0x2de92c6f592b0275, 0x4a7484aa6ea6e483, 0x5cb0a9dcbd41fbd4, 0x76f988da831153b5,
         0x983e5152ee66dfab, 0xa831c66d2db43210, 0xb00327c898fb213f, 0xbf597fc7beef0ee4,
         0xc6e00bf33da88fc2, 0xd5a79147930aa725, 0x06ca6351e003826f, 0x142929670a0e6e70,
         0x27b70a8546d22ffc, 0x2e1b21385c26c926, 0x4d2c6dfc5ac42aed, 0x53380d139d95b3df,
         0x650a73548baf63de, 0x766a0abb3c77b2a8, 0x81c2c92e47edaee6, 0x92722c851482353b,
         0xa2bfe8a14cf10364, 0xa81a664bbc423001, 0xc24b8b70d0f89791, 0xc76c51a30654be30,
         0xd192e819d6ef5218, 0xd69906245565a910, 0xf40e35855771202a, 0x106aa07032bbd1b8,
         0x19a4c116b8d2d0c8, 0x1e376c085141ab53, 0x2748774cdf8eeb99, 0x34b0bcb5e19b48a8,
         0x391c0cb3c5c95a63, 0x4ed8aa4ae3418acb, 0x5b9cca4f7763e373, 0x682e6ff3d6b2b8a3,
         0x748f82ee5defb2fc, 0x78a5636f43172f60, 0x84c87814a1f0ab72, 0x8cc702081a6439ec,
         0x90befffa23631e28, 0xa4506cebde82bde9, 0xbef9a3f7b2c67915, 0xc67178f2e372532b,
         0xca273eceea26619c, 0xd186b8c721c0c207, 0xeada7dd6cde0eb1e, 0xf57d4f7fee6ed178,
         0x06f067aa72176fba, 0x0a637dc5a2c898a6, 0x113f9804bef90dae, 0x1b710b35131c471b,
         0x28db77f523047d84, 0x32caab7b40c72493, 0x3c9ebe0a15c9bebc, 0x431d67c49c100d4c,
         0x4cc5d4becb3e42b6, 0x597f299cfc657e2a, 0x5fcb6fab3ad6faec, 0x6c44198c4a475817)


class _SHA512(_Hash):
    name = "sha512"
    digest_size = 64
    block_size = 128
    _IV = (0x6a09e667f3bcc908, 0xbb67ae8584caa73b, 0x3c6ef372fe94f82b, 0xa54ff53a5f1d36f1,
           0x510e527fade682d1, 0x9b05688c2b3e6c1f, 0x1f83d9abfb41bd6b, 0x5be0cd19137e2179)
    _LEN_BYTES = 16
    _ORDER = "big"
    _WORD = 8

    def _compress(self, block):
        w = [int.from_bytes(block[i:i + 8], "big") for i in range(0, 128, 8)]
        for i in range(16, 80):
            x = w[i - 15]
            y = w[i - 2]
            s0 = _rotr64(x, 1) ^ _rotr64(x, 8) ^ (x >> 7)
            s1 = _rotr64(y, 19) ^ _rotr64(y, 61) ^ (y >> 6)
            w.append((w[i - 16] + s0 + w[i - 7] + s1) & _M64)
        a, b, c, d, e, f, g, h = self._h
        for i in range(80):
            s1 = _rotr64(e, 14) ^ _rotr64(e, 18) ^ _rotr64(e, 41)
            ch = (e & f) ^ (~e & g)
            t1 = (h + s1 + ch + _K512[i] + w[i]) & _M64
            s0 = _rotr64(a, 28) ^ _rotr64(a, 34) ^ _rotr64(a, 39)
            maj = (a & b) ^ (a & c) ^ (b & c)
            t2 = (s0 + maj) & _M64
            h = g
            g = f
            f = e
            e = (d + t1) & _M64
            d = c
            c = b
            b = a
            a = (t1 + t2) & _M64
        self._h = [(x + y) & _M64 for x, y in zip(self._h, (a, b, c, d, e, f, g, h))]


class _SHA384(_SHA512):
    name = "sha384"
    digest_size = 48
    _IV = (0xcbbb9d5dc1059ed8, 0x629a292a367cd507, 0x9159015a3070dd17, 0x152fecd8f70e5939,
           0x67332667ffc00b31, 0x8eb44a8768581511, 0xdb0c2e0d64f98fa7, 0x47b5481dbefa4fa4)


_BY_NAME = {"md5": _MD5, "sha1": _SHA1, "sha224": _SHA224, "sha256": _SHA256,
            "sha384": _SHA384, "sha512": _SHA512}


def md5(data=b"", *, usedforsecurity=True):
    return _MD5(data)


def sha1(data=b"", *, usedforsecurity=True):
    return _SHA1(data)


def sha224(data=b"", *, usedforsecurity=True):
    return _SHA224(data)


def sha256(data=b"", *, usedforsecurity=True):
    return _SHA256(data)


def sha384(data=b"", *, usedforsecurity=True):
    return _SHA384(data)


def sha512(data=b"", *, usedforsecurity=True):
    return _SHA512(data)


def new(name, data=b"", **kwargs):
    cls = _BY_NAME.get(name.lower().replace("-", "")) if isinstance(name, str) else None
    if cls is None:
        raise ValueError("unsupported hash type " + str(name))
    return cls(data)


def file_digest(fileobj, digest, /):
    h = new(digest) if isinstance(digest, str) else digest()
    while True:
        chunk = fileobj.read(65536)
        if not chunk:
            return h
        h.update(chunk)


def _hmac(cls, key, msg):
    bs = cls.block_size
    if len(key) > bs:
        key = cls(key).digest()
    key = key + b"\x00" * (bs - len(key))
    inner = cls(bytes(b ^ 0x36 for b in key))
    inner.update(msg)
    outer = cls(bytes(b ^ 0x5C for b in key))
    outer.update(inner.digest())
    return outer.digest()


def pbkdf2_hmac(hash_name, password, salt, iterations, dklen=None):
    cls = new(hash_name).__class__
    password = _as_bytes(password)
    salt = _as_bytes(salt)
    if iterations < 1:
        raise ValueError("iteration value must be greater than 0.")
    if dklen is None:
        dklen = cls.digest_size
    out = b""
    block = 1
    while len(out) < dklen:
        u = _hmac(cls, password, salt + block.to_bytes(4, "big"))
        acc = int.from_bytes(u, "big")
        for _ in range(iterations - 1):
            u = _hmac(cls, password, u)
            acc ^= int.from_bytes(u, "big")
        out += acc.to_bytes(cls.digest_size, "big")
        block += 1
    return out[:dklen]
