# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""asyncio.base_events: the names libraries import from it by that path.
The event loop itself is in asyncio."""

from asyncio import BaseEventLoop, _run_until_complete_cb  # noqa: F401

__all__ = ["BaseEventLoop"]
