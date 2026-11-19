# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""email.utils: RFC 2822 dates and addresses -- what HTTP clients read a
Retry-After or Date header with, and what mail-building code formats."""

import calendar as _calendar
import datetime as _dt
import time as _time

_MONTHS = ["jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec"]
_DAYS = ["mon", "tue", "wed", "thu", "fri", "sat", "sun"]
_ZONES = {"UT": 0, "UTC": 0, "GMT": 0, "Z": 0, "AST": -400, "ADT": -300, "EST": -500, "EDT": -400,
          "CST": -600, "CDT": -500, "MST": -700, "MDT": -600, "PST": -800, "PDT": -700}


def _parsedate_tz(data):
    if not data:
        return None
    data = data.split()
    if not data:
        return None
    if data[0].endswith(",") or data[0].lower() in _DAYS:
        del data[0]
    else:
        i = data[0].rfind(",")
        if i >= 0:
            data[0] = data[0][i + 1:]
    if len(data) == 3:
        stuff = data[0].split("-")
        if len(stuff) == 3:
            data = stuff + data[1:]
    if len(data) == 4:
        s = data[3]
        i = s.find("+")
        if i == -1:
            i = s.find("-")
        if i > 0:
            data[3:] = [s[:i], s[i:]]
        else:
            data.append("")
    if len(data) < 5:
        return None
    data = data[:5]
    dd, mm, yy, tm, tz = data
    if not (dd and mm and yy):
        return None
    mm = mm.lower()
    if mm not in _MONTHS:
        dd, mm = mm, dd.lower()
        if mm not in _MONTHS:
            return None
    mm = _MONTHS.index(mm) + 1
    if mm > 12:
        mm -= 12
    if dd[-1] == ",":
        dd = dd[:-1]
    i = yy.find(":")
    if i > 0:
        yy, tm = tm, yy
    if yy[-1] == ",":
        yy = yy[:-1]
        if not yy:
            return None
    if not yy[0].isdigit():
        yy, tz = tz, yy
    if tm[-1:] == ",":
        tm = tm[:-1]
    tm = tm.split(":")
    if len(tm) == 2:
        thh, tmm = tm
        tss = "0"
    elif len(tm) == 3:
        thh, tmm, tss = tm
    elif len(tm) == 1 and "." in tm[0]:
        tm = tm[0].split(".")
        if len(tm) == 2:
            thh, tmm = tm
            tss = 0
        elif len(tm) == 3:
            thh, tmm, tss = tm
        else:
            return None
    else:
        return None
    try:
        yy = int(yy)
        dd = int(dd)
        thh = int(thh)
        tmm = int(tmm)
        tss = int(tss)
    except ValueError:
        return None
    if yy < 100:
        yy += 1900 if yy > 68 else 2000
    tzoffset = None
    tz = tz.upper()
    if tz in _ZONES:
        tzoffset = _ZONES[tz]
    else:
        try:
            tzoffset = int(tz)
        except ValueError:
            pass
        if tzoffset == 0 and tz.startswith("-"):
            tzoffset = None
    if tzoffset:
        sign = -1 if tzoffset < 0 else 1
        tzoffset = abs(tzoffset)
        tzoffset = sign * ((tzoffset // 100) * 3600 + (tzoffset % 100) * 60)
    return [yy, mm, dd, thh, tmm, tss, 0, 1, -1, tzoffset]


def parsedate_tz(data):
    t = _parsedate_tz(data)
    if isinstance(t, list):
        t = tuple(t)
    return t


def parsedate(data):
    t = parsedate_tz(data)
    return t[:9] if isinstance(t, tuple) else t


def mktime_tz(data):
    if data[9] is None:
        return _time.mktime(data[:8] + (-1,))
    return _calendar.timegm(data) - data[9]


def parsedate_to_datetime(data):
    parsed = _parsedate_tz(data)
    if parsed is None:
        raise ValueError("Invalid date value or format %r" % (str(data),))
    *dtuple, tz = parsed
    if tz is None:
        return _dt.datetime(*dtuple[:6])
    return _dt.datetime(*dtuple[:6], tzinfo=_dt.timezone(_dt.timedelta(seconds=tz)))


def _format_timetuple_and_zone(timetuple, zone):
    return "%s, %02d %s %04d %02d:%02d:%02d %s" % (
        ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"][timetuple[6]], timetuple[2],
        ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"][timetuple[1] - 1],
        timetuple[0], timetuple[3], timetuple[4], timetuple[5], zone)


def formatdate(timeval=None, localtime=False, usegmt=False):
    if timeval is None:
        timeval = _time.time()
    d = _dt.datetime.fromtimestamp(timeval, _dt.timezone.utc)
    if localtime:
        d = d.astimezone()
        usegmt = False
    return format_datetime(d, usegmt)


def format_datetime(dt, usegmt=False):
    now = dt.timetuple()
    if usegmt:
        if dt.tzinfo is None or dt.tzinfo != _dt.timezone.utc:
            raise ValueError("usegmt option requires a UTC datetime")
        zone = "GMT"
    elif dt.tzinfo is None:
        zone = "-0000"
    else:
        off = dt.utcoffset()
        secs = int(off.total_seconds())
        sign = "-" if secs < 0 else "+"
        secs = abs(secs)
        zone = "%s%02d%02d" % (sign, secs // 3600, (secs // 60) % 60)
    return _format_timetuple_and_zone(now, zone)


def localtime(dt=None, isdst=-1):
    if dt is None:
        return _dt.datetime.now(_dt.timezone.utc).astimezone()
    if dt.tzinfo is not None:
        return dt.astimezone()
    return dt.astimezone()


def quote(s):
    return s.replace("\\", "\\\\").replace('"', '\\"')


def unquote(s):
    if len(s) > 1:
        if s.startswith('"') and s.endswith('"'):
            return s[1:-1].replace("\\\\", "\\").replace('\\"', '"')
        if s.startswith("<") and s.endswith(">"):
            return s[1:-1]
    return s


_SPECIALS = '()<>@,:;."[]'


def formataddr(pair, charset="utf-8"):
    name, address = pair
    address.encode("ascii")
    if name:
        try:
            name.encode("ascii")
        except UnicodeEncodeError:
            import base64
            name = "=?%s?b?%s?=" % (charset, base64.b64encode(name.encode(charset)).decode("ascii"))
            return "%s <%s>" % (name, address)
        if any(c in _SPECIALS for c in name):
            name = '"%s"' % quote(name)
        return "%s <%s>" % (name, address)
    return address


def _split_addresses(field):
    out, cur, depth, quoted, esc = [], [], 0, False, False
    for ch in field:
        if esc:
            cur.append(ch)
            esc = False
            continue
        if ch == "\\" and quoted:
            cur.append(ch)
            esc = True
            continue
        if ch == '"':
            quoted = not quoted
        elif not quoted and ch in "<(":
            depth += 1
        elif not quoted and ch in ">)":
            depth -= 1
        elif ch == "," and not quoted and depth <= 0:
            out.append("".join(cur))
            cur = []
            continue
        cur.append(ch)
    out.append("".join(cur))
    return out


def _parse_one(text):
    text = text.strip()
    if not text:
        return ("", "")
    comment = ""
    while "(" in text and ")" in text and text.rfind(")") > text.find("("):
        a, b = text.find("("), text.rfind(")")
        comment = text[a + 1:b].strip()
        text = (text[:a] + text[b + 1:]).strip()
    if "<" in text and text.endswith(">"):
        i = text.rfind("<")
        name = unquote(text[:i].strip())
        return (name, text[i + 1:-1].strip())
    if " " in text and "@" not in text:
        return ("", "")
    return (comment, text.replace(" ", ""))


def parseaddr(addr, *, strict=True):
    if not isinstance(addr, str):
        addr = str(addr)
    parts = [p for p in _split_addresses(addr) if p.strip()]
    if len(parts) != 1:
        return ("", "") if parts else ("", "")
    return _parse_one(parts[0])


def getaddresses(fieldvalues, *, strict=True):
    out = []
    for v in fieldvalues:
        for p in _split_addresses(str(v)):
            if p.strip():
                out.append(_parse_one(p))
    return out


def make_msgid(idstring=None, domain=None):
    import random
    t = int(_time.time() * 100)
    pid = 1
    r = random.getrandbits(64)
    idstring = "" if idstring is None else "." + idstring
    return "<%d.%d.%d%s@%s>" % (t, pid, r, idstring, domain or "localhost")


def decode_rfc2231(s):
    parts = s.split("'", 2)
    if len(parts) <= 2:
        return None, None, s
    return parts


def encode_rfc2231(s, charset=None, language=None):
    from urllib.parse import quote as _q
    s = _q(s, safe="", encoding=charset or "ascii")
    if charset is None and language is None:
        return s
    return "%s'%s'%s" % (charset or "", language or "", s)


def collapse_rfc2231_value(value, errors="replace", fallback_charset="us-ascii"):
    if not isinstance(value, tuple) or len(value) != 3:
        return unquote(value)
    charset, language, text = value
    # text holds the parameter's bytes as latin-1 characters (get_params
    # decoded the percent escapes); an unknown charset gives them back as is
    try:
        return text.encode("latin-1", "replace").decode(charset or fallback_charset, errors)
    except LookupError:
        return unquote(text)
