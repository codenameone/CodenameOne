# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Clean-room subset of markupsafe: Markup and escape."""

__all__ = ["Markup", "escape", "escape_silent", "soft_str"]


def _esc(s):
    return (s.replace("&", "&amp;").replace(">", "&gt;").replace("<", "&lt;")
            .replace("'", "&#39;").replace('"', "&#34;"))


class Markup(str):
    """A string that is already safe to insert into HTML."""

    def __new__(cls, base="", encoding=None, errors="strict"):
        if hasattr(base, "__html__"):
            base = base.__html__()
        if encoding is not None:
            base = base.decode(encoding, errors)
        return str.__new__(cls, base)

    def __html__(self):
        return self

    def __add__(self, other):
        if isinstance(other, str) or hasattr(other, "__html__"):
            return Markup(str(self) + str(escape(other)))
        return NotImplemented

    def __radd__(self, other):
        if isinstance(other, str) or hasattr(other, "__html__"):
            return Markup(str(escape(other)) + str(self))
        return NotImplemented

    def join(self, seq):
        return Markup(str.join(self, [str(escape(s)) for s in seq]))

    def upper(self):
        return Markup(str.upper(self))

    def lower(self):
        return Markup(str.lower(self))

    def title(self):
        return Markup(str.title(self))

    def strip(self, chars=None):
        return Markup(str.strip(self, chars))

    def format(self, *args, **kwargs):
        args = [escape(a) for a in args]
        kwargs = dict((k, escape(v)) for k, v in kwargs.items())
        return Markup(str.format(self, *args, **kwargs))

    def __mod__(self, arg):
        if isinstance(arg, tuple):
            arg = tuple(escape(a) for a in arg)
        else:
            arg = escape(arg)
        return Markup(str.__mod__(self, arg))

    def __repr__(self):
        return "Markup(" + str.__repr__(self) + ")"

    def striptags(self):
        out = []
        depth = 0
        for ch in str(self):
            if ch == "<":
                depth += 1
            elif ch == ">" and depth:
                depth -= 1
            elif not depth:
                out.append(ch)
        return " ".join("".join(out).split())

    def unescape(self):
        s = str(self)
        for a, b in (("&lt;", "<"), ("&gt;", ">"), ("&#34;", '"'), ("&#39;", "'"),
                     ("&quot;", '"'), ("&amp;", "&")):
            s = s.replace(a, b)
        return s


def escape(s):
    if hasattr(s, "__html__"):
        return Markup(s.__html__())
    return Markup(_esc(str(s)))


def escape_silent(s):
    if s is None:
        return Markup()
    return escape(s)


def soft_str(s):
    if not isinstance(s, str):
        return str(s)
    return s
