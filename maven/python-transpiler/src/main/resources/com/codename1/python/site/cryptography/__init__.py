# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""cryptography: the symmetric cipher subset, over Codename One's own AES.

Only `hazmat.primitives.ciphers` with AES in CBC and ECB modes is provided
(what pdfminer needs to read encrypted PDFs). The block cipher itself is
`com.codename1.security.Cipher`, which each port backs with its platform
implementation."""

__version__ = "44.0.0"
