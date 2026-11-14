# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""cryptography.hazmat.primitives.ciphers.modes: CBC and ECB."""


class CBC:
    name = "CBC"

    def __init__(self, initialization_vector):
        initialization_vector = bytes(initialization_vector)
        if len(initialization_vector) != 16:
            raise ValueError("Invalid IV size (%d) for CBC." % len(initialization_vector))
        self.initialization_vector = initialization_vector


class ECB:
    name = "ECB"
