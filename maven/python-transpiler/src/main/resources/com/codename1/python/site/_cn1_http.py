# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""What requests and httpx share: headers, bodies, URLs and the transport.

Requests go through _cn1http, the platform's own HTTP client (Codename
One's ConnectionRequest in an application), so proxies, certificates and
the network indicator behave as they do for the rest of the application.
"""

import _cn1http
import base64 as _base64
import json as _json
import time as _time
from urllib.parse import urlencode, urlsplit, urlunsplit


class Headers:
    """Case-insensitive headers that keep the order and spelling given."""

    def __init__(self, data=None):
        self._items = []
        if data is not None:
            self.update(data)

    def update(self, data):
        if isinstance(data, Headers):
            data = data.multi_items()
        elif hasattr(data, "items"):
            data = list(data.items())
        for k, v in data:
            self[k] = v

    def multi_items(self):
        return list(self._items)

    def add(self, key, value):
        self._items.append((key, value))

    def get_list(self, key):
        lk = key.lower()
        return [v for k, v in self._items if k.lower() == lk]

    def __getitem__(self, key):
        values = self.get_list(key)
        if not values:
            raise KeyError(key)
        return ", ".join(values)

    def get(self, key, default=None):
        values = self.get_list(key)
        return ", ".join(values) if values else default

    def __setitem__(self, key, value):
        lk = key.lower()
        kept = []
        placed = False
        for k, v in self._items:
            if k.lower() == lk:
                if not placed:
                    kept.append((key, value))
                    placed = True
            else:
                kept.append((k, v))
        if not placed:
            kept.append((key, value))
        self._items = kept

    def __delitem__(self, key):
        lk = key.lower()
        if not any(k.lower() == lk for k, _ in self._items):
            raise KeyError(key)
        self._items = [(k, v) for k, v in self._items if k.lower() != lk]

    def __contains__(self, key):
        lk = key.lower()
        return any(k.lower() == lk for k, _ in self._items)

    def keys(self):
        seen = []
        for k, _ in self._items:
            if k.lower() not in [s.lower() for s in seen]:
                seen.append(k)
        return seen

    def values(self):
        return [self[k] for k in self.keys()]

    def items(self):
        return [(k, self[k]) for k in self.keys()]

    def __iter__(self):
        return iter(self.keys())

    def __len__(self):
        return len(self.keys())

    def copy(self):
        return type(self)(self.multi_items())

    def setdefault(self, key, default=None):
        if key not in self:
            self[key] = default
        return self[key]

    def pop(self, key, *default):
        if key in self:
            v = self[key]
            del self[key]
            return v
        if default:
            return default[0]
        raise KeyError(key)

    def lower_items(self):
        return [(k.lower(), v) for k, v in self.items()]

    def __eq__(self, other):
        if isinstance(other, Headers):
            other = dict(other.lower_items())
        elif hasattr(other, "items"):
            other = {k.lower(): v for k, v in other.items()}
        else:
            return NotImplemented
        return dict(self.lower_items()) == other

    def __repr__(self):
        return repr(dict(self.items()))


def with_params(url, params):
    """`url` with `params` (a mapping or pairs; a list value repeats, None
    drops the key) added to its query."""
    if not params:
        return url
    if isinstance(params, (str, bytes)):
        extra = params.decode("utf-8") if isinstance(params, bytes) else params
    else:
        pairs = params.items() if hasattr(params, "items") else params
        flat = []
        for k, v in pairs:
            if v is None:
                continue
            if isinstance(v, (list, tuple)):
                for item in v:
                    flat.append((k, item))
            else:
                flat.append((k, v))
        extra = urlencode(flat)
    if not extra:
        return url
    parts = urlsplit(url)
    query = parts.query + "&" + extra if parts.query else extra
    return urlunsplit((parts.scheme, parts.netloc, parts.path, query, parts.fragment))


def form_body(data):
    pairs = data.items() if hasattr(data, "items") else data
    flat = []
    for k, v in pairs:
        if isinstance(v, (list, tuple)):
            for item in v:
                flat.append((k, item))
        elif v is not None:
            flat.append((k, v))
    return urlencode(flat).encode("ascii")


def basic_auth(user, password):
    if isinstance(user, str):
        user = user.encode("latin-1")
    if isinstance(password, str):
        password = password.encode("latin-1")
    return "Basic " + _base64.b64encode(user + b":" + password).decode("ascii")


def charset(content_type):
    """The charset parameter of a Content-Type, or None."""
    if not content_type:
        return None
    for part in content_type.split(";")[1:]:
        k, _, v = part.strip().partition("=")
        if k.strip().lower() == "charset":
            return v.strip().strip("'\"") or None
    return None


def send(method, url, headers, body, timeout, follow_redirects):
    """Sends a request; answers (status, reason, url, [(name, value)], body,
    seconds). Raises OSError when no response arrived."""
    start = _time.monotonic()
    status, reason, final_url, pairs, content = _cn1http.request(method, url, headers.multi_items(), body,
                                                                 timeout, follow_redirects)
    return status, reason, final_url, pairs, content, _time.monotonic() - start


def json_bytes(obj, compact):
    if compact:
        return _json.dumps(obj, ensure_ascii=False, separators=(",", ":"), allow_nan=False).encode("utf-8")
    return _json.dumps(obj, allow_nan=False).encode("utf-8")


def is_timeout(message):
    m = (message or "").lower()
    return "timed out" in m or "timeout" in m
