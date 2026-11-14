# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""asyncio.futures: the names libraries import from it by that path.
Futures themselves are in asyncio."""

from asyncio import CancelledError, Future, InvalidStateError, future_add_to_awaited_by  # noqa: F401
from asyncio import future_discard_from_awaited_by  # noqa: F401


def isfuture(obj):
    return isinstance(obj, Future)


__all__ = ["CancelledError", "Future", "InvalidStateError", "isfuture"]
