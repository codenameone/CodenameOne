# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""cryptography.hazmat.primitives.ciphers: Cipher, algorithms.AES, modes.CBC/ECB.

A context collects its input and runs the platform cipher once in
finalize(); update() returns whole blocks only after finalize, which is
legal for the API (update may buffer) and what every caller that ends with
finalize() expects. Padding is the caller's, as in the real package:
the transformations used are the NoPadding ones.
"""

import _cn1aes

from . import algorithms, modes

__all__ = ["Cipher", "algorithms", "modes"]


class _Context:
    def __init__(self, cipher, encrypt):
        self._cipher = cipher
        self._encrypt = encrypt
        self._buf = bytearray()
        self._done = False

    def update(self, data):
        if self._done:
            raise ValueError("Context was already finalized.")
        self._buf += data
        return b""

    def update_into(self, data, buf):
        raise NotImplementedError("update_into is not supported")

    def finalize(self):
        if self._done:
            raise ValueError("Context was already finalized.")
        self._done = True
        data = bytes(self._buf)
        if len(data) % 16:
            raise ValueError("The length of the provided data is not a multiple of the block length.")
        if not data:
            return b""
        mode = self._cipher.mode
        key = self._cipher.algorithm.key
        if isinstance(mode, modes.ECB):
            # ECB is CBC with a zero IV, one block at a time.
            out = bytearray()
            zero = b"\0" * 16
            for i in range(0, len(data), 16):
                block = data[i:i + 16]
                if self._encrypt:
                    out += _cn1aes.cbc_encrypt(key, zero, block)
                else:
                    out += _cn1aes.cbc_decrypt(key, zero, block)
            return bytes(out)
        iv = mode.initialization_vector
        if self._encrypt:
            return _cn1aes.cbc_encrypt(key, iv, data)
        return _cn1aes.cbc_decrypt(key, iv, data)


class Cipher:
    def __init__(self, algorithm, mode, backend=None):
        if not isinstance(algorithm, algorithms.AES):
            raise NotImplementedError("only AES is supported")
        if not isinstance(mode, (modes.CBC, modes.ECB)):
            raise NotImplementedError("only CBC and ECB modes are supported")
        self.algorithm = algorithm
        self.mode = mode

    def encryptor(self):
        return _Context(self, True)

    def decryptor(self):
        return _Context(self, False)
