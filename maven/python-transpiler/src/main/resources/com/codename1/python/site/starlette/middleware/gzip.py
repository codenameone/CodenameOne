# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""GZipMiddleware: compresses a response body of at least ``minimum_size`` bytes."""
import gzip


class GZipMiddleware:
    def __init__(self, app, minimum_size=500, compresslevel=9):
        self.app = app
        self.minimum_size = minimum_size
        self.compresslevel = compresslevel

    async def __call__(self, request):
        resp = await self.app(request)
        if "content-encoding" in resp.headers:
            return resp
        ct = resp.headers.get("content-type", "")
        if ct.startswith("text/event-stream"):
            return resp
        body = resp.body
        # A response that came through BaseHTTPMiddleware is streamed under
        # Starlette, and a streamed body is compressed whatever its size.
        if len(body) < self.minimum_size and not getattr(resp, "_streamed", False):
            return resp
        # Vary is set on every response that would be compressed for a client
        # that asked, so that a cache keeps the two encodings apart.
        resp.headers.add_vary_header("Accept-Encoding")
        if "gzip" not in request.headers.get("accept-encoding", ""):
            return resp
        resp.body = gzip.compress(body, compresslevel=self.compresslevel)
        resp.headers["content-encoding"] = "gzip"
        resp.headers["content-length"] = str(len(resp.body))
        return resp
