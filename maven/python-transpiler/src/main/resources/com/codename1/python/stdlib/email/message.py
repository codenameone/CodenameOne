# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""email.message.Message: headers in order, looked up without regard to case.

Covers what headers and simple payloads need (HTTP responses, form parts);
MIME multipart trees, policies and encoders are not provided.
"""

__all__ = ["Message", "EmailMessage"]


def _split_params(value):
    parts = []
    cur = []
    quoted = False
    for ch in value:
        if ch == '"':
            quoted = not quoted
            cur.append(ch)
        elif ch == ";" and not quoted:
            parts.append("".join(cur).strip())
            cur = []
        else:
            cur.append(ch)
    parts.append("".join(cur).strip())
    return parts


def _unquote(v):
    if len(v) > 1 and v[0] == v[-1] == '"':
        return v[1:-1].replace('\\"', '"').replace("\\\\", "\\")
    return v


class Message:
    def __init__(self, policy=None):
        self._headers = []
        self._payload = None
        self._charset = None
        self.preamble = None
        self.epilogue = None
        self.defects = []

    def __len__(self):
        return len(self._headers)

    def __contains__(self, name):
        n = name.lower()
        return any(k.lower() == n for k, _ in self._headers)

    def __iter__(self):
        return iter([k for k, _ in self._headers])

    def __getitem__(self, name):
        return self.get(name)

    def __setitem__(self, name, val):
        self._headers.append((name, val))

    def __delitem__(self, name):
        n = name.lower()
        self._headers = [(k, v) for k, v in self._headers if k.lower() != n]

    def keys(self):
        return [k for k, _ in self._headers]

    def values(self):
        return [v for _, v in self._headers]

    def items(self):
        return list(self._headers)

    def get(self, name, failobj=None):
        n = name.lower()
        for k, v in self._headers:
            if k.lower() == n:
                return v
        return failobj

    def get_all(self, name, failobj=None):
        n = name.lower()
        values = [v for k, v in self._headers if k.lower() == n]
        return values if values else failobj

    def add_header(self, _name, _value, **_params):
        parts = [] if _value is None else [_value]
        for k, v in _params.items():
            k = k.replace("_", "-")
            if v is None:
                parts.append(k)
            else:
                parts.append('%s="%s"' % (k, str(v).replace("\\", "\\\\").replace('"', '\\"')))
        self._headers.append((_name, "; ".join(parts)))

    def replace_header(self, _name, _value):
        n = _name.lower()
        for i, (k, _) in enumerate(self._headers):
            if k.lower() == n:
                self._headers[i] = (k, _value)
                return
        raise KeyError(_name)

    def get_params(self, failobj=None, header="content-type", unquote=True):
        value = self.get(header)
        if value is None:
            return failobj
        result = []
        for i, p in enumerate(_split_params(value)):
            if not p and i:
                continue
            if "=" in p and i:
                k, v = p.split("=", 1)
                result.append((k.strip().lower(), _unquote(v.strip()) if unquote else v.strip()))
            else:
                result.append((p.strip() if i == 0 else p.strip().lower(), ""))
        return result

    def get_param(self, param, failobj=None, header="content-type", unquote=True):
        params = self.get_params(None, header, unquote)
        if params is None:
            return failobj
        for k, v in params[1:]:
            if k == param.lower():
                return v
        return failobj

    def get_content_type(self):
        value = self.get("content-type")
        if value is None:
            return self.get_default_type()
        ctype = _split_params(value)[0].lower()
        if ctype.count("/") != 1:
            return "text/plain"
        return ctype

    def get_content_maintype(self):
        return self.get_content_type().split("/")[0]

    def get_content_subtype(self):
        return self.get_content_type().split("/")[1]

    def get_default_type(self):
        return "text/plain"

    def get_content_charset(self, failobj=None):
        cs = self.get_param("charset")
        return cs.lower() if cs else failobj

    def get_charsets(self, failobj=None):
        return [self.get_content_charset(failobj)]

    def get_filename(self, failobj=None):
        fn = self.get_param("filename", None, "content-disposition")
        if fn is None:
            fn = self.get_param("name", None, "content-type")
        return failobj if fn is None else fn

    def get_boundary(self, failobj=None):
        return self.get_param("boundary", failobj)

    def is_multipart(self):
        return isinstance(self._payload, list)

    def set_payload(self, payload, charset=None):
        self._payload = payload
        if charset is not None:
            self.set_charset(charset)

    def set_charset(self, charset):
        self._charset = charset
        if "content-type" not in self:
            self["Content-Type"] = 'text/plain; charset="%s"' % charset

    def get_charset(self):
        return self._charset

    def get_payload(self, i=None, decode=False):
        if i is not None:
            return self._payload[i]
        if decode and isinstance(self._payload, str):
            return self._payload.encode(self.get_content_charset("utf-8"), "surrogateescape")
        return self._payload

    def attach(self, payload):
        if self._payload is None:
            self._payload = [payload]
        else:
            self._payload.append(payload)

    def walk(self):
        yield self
        if self.is_multipart():
            for p in self._payload:
                yield from p.walk()

    def as_string(self, unixfrom=False, maxheaderlen=0, policy=None):
        out = "".join("%s: %s\n" % (k, v) for k, v in self._headers) + "\n"
        if isinstance(self._payload, str):
            out += self._payload
        return out

    def as_bytes(self, unixfrom=False, policy=None):
        return self.as_string().encode("utf-8")

    def __str__(self):
        return self.as_string()


class EmailMessage(Message):
    def get_content(self):
        p = self.get_payload(decode=True)
        if self.get_content_maintype() == "text" and isinstance(p, bytes):
            return p.decode(self.get_content_charset("utf-8"))
        return p

    def set_content(self, content, subtype="plain", charset="utf-8"):
        del self["Content-Type"]
        if isinstance(content, str):
            self["Content-Type"] = "text/%s; charset=\"%s\"" % (subtype, charset)
        self._payload = content
