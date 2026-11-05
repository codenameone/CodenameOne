# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""The attrs exceptions under the attrs package name."""

from attr.exceptions import (AttrsAttributeNotFoundError, DefaultAlreadySetError, FrozenAttributeError,
                             FrozenError, FrozenInstanceError, NotAnAttrsClassError, NotCallableError,
                             PythonTooOldError, UnannotatedAttributeError)

__all__ = ["AttrsAttributeNotFoundError", "DefaultAlreadySetError", "FrozenAttributeError", "FrozenError",
           "FrozenInstanceError", "NotAnAttrsClassError", "NotCallableError", "PythonTooOldError",
           "UnannotatedAttributeError"]
