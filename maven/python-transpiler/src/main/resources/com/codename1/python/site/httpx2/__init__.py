# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""httpx2, the name the httpx 2 series is published under, as the httpx
module itself: the same API over Codename One's HTTP client."""
from httpx import *  # noqa: F401,F403
from httpx import __version__, _transport  # noqa: F401
from httpx import (Auth, BaseTransport, AsyncBaseTransport, HTTPTransport, AsyncHTTPTransport,  # noqa: F401
                   MockTransport, Limits, Proxy, FunctionAuth, StreamConsumed, StreamClosed, ResponseNotRead,
                   RequestNotRead, LocalProtocolError, RemoteProtocolError, ProxyError, WriteError, CloseError)
from httpx2 import _config  # noqa: F401
