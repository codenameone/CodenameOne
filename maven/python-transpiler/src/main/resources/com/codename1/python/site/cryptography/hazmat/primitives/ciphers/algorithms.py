# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""cryptography.hazmat.primitives.ciphers.algorithms: AES."""


class AES:
    name = "AES"
    block_size = 128

    def __init__(self, key):
        key = bytes(key)
        if len(key) not in (16, 24, 32):
            raise ValueError("Invalid key size (%d) for AES." % (len(key) * 8))
        self.key = key

    @property
    def key_size(self):
        return len(self.key) * 8
