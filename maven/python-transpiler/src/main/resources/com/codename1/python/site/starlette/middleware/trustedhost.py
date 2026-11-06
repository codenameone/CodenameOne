# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""TrustedHostMiddleware: refuses a request whose Host header is not allowed."""
from starlette.responses import PlainTextResponse, RedirectResponse

ENFORCE_DOMAIN_WILDCARD = "Domain wildcard patterns must be like '*.example.com'."


class TrustedHostMiddleware:
    # Request based (see starlette.middleware); not wrapped as ASGI.
    _cn1_request_based = True

    def __init__(self, app, allowed_hosts=None, www_redirect=True):
        if allowed_hosts is None:
            allowed_hosts = ["*"]
        for pattern in allowed_hosts:
            assert "*" not in pattern[1:], ENFORCE_DOMAIN_WILDCARD
            if pattern.startswith("*") and pattern != "*":
                assert pattern.startswith("*."), ENFORCE_DOMAIN_WILDCARD
        self.app = app
        self.allowed_hosts = list(allowed_hosts)
        self.allow_any = "*" in allowed_hosts
        self.www_redirect = www_redirect

    async def __call__(self, request):
        if self.allow_any:
            return await self.app(request)
        host = request.headers.get("host", "").split(":")[0]
        valid = False
        www = False
        for pattern in self.allowed_hosts:
            if host == pattern or (pattern.startswith("*") and host.endswith(pattern[1:])):
                valid = True
                break
            elif "www." + host == pattern:
                www = True
        if valid:
            return await self.app(request)
        if www and self.www_redirect:
            netloc = request.headers.get("host", "")
            url = "http://www." + netloc + request._path + ("?" + request._query if request._query else "")
            return RedirectResponse(url)
        return PlainTextResponse("Invalid host header", status_code=400)
