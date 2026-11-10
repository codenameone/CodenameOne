# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Middleware over the pure dispatch core.

There is no ASGI here: an "app" a middleware wraps is an async callable
taking a request and answering a response. Every middleware class takes
that app first and implements ``async __call__(request)``; the application
builds the stack with the last one added outermost, as Starlette does.
Websocket connections pass through it too, with the websocket as the
request. A middleware without ``_cn1_request_based`` is taken to be pure
ASGI (``__call__(scope, receive, send)``) and adapted by ``_asgi``.
"""


class Middleware:
    def __init__(self, cls, *args, **kwargs):
        self.cls = cls
        self.args = args
        self.kwargs = kwargs

    def __iter__(self):
        return iter((self.cls, self.args, self.kwargs))

    def __repr__(self):
        return "Middleware(%s)" % getattr(self.cls, "__name__", repr(self.cls))
