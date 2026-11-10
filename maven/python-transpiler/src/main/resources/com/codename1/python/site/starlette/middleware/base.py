# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""BaseHTTPMiddleware: ``dispatch(request, call_next)``."""


class BaseHTTPMiddleware:
    # Request based (see starlette.middleware); not wrapped as ASGI.
    _cn1_request_based = True

    def __init__(self, app, dispatch=None):
        self.app = app
        self.dispatch_func = self.dispatch if dispatch is None else dispatch

    async def call_next(self, request):
        resp = await self.app(request)
        resp._streamed = True
        return resp

    async def __call__(self, request):
        if request.scope.get("type") == "websocket":
            # Starlette's middleware passes a websocket scope straight through.
            return await self.app(request)
        return await self.dispatch_func(request, self.call_next)

    async def dispatch(self, request, call_next):
        raise NotImplementedError()
