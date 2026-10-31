# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""strptime for time and datetime: a format becomes a regular expression."""

import re
import time as _t

_FIELDS = {
    "d": r"(?P<d>3[01]|[12]\d|0[1-9]|[1-9]| [1-9])",
    "f": r"(?P<f>[0-9]{1,6})",
    "H": r"(?P<H>2[0-3]|[0-1]\d|\d)",
    "I": r"(?P<I>1[0-2]|0[1-9]|[1-9]| [1-9])",
    "G": r"(?P<G>\d\d\d\d)",
    "j": r"(?P<j>36[0-6]|3[0-5]\d|[12]\d\d|0[1-9]\d|00[1-9]|[1-9]\d|0[1-9]|[1-9])",
    "m": r"(?P<m>1[0-2]|0[1-9]|[1-9])",
    "M": r"(?P<M>[0-5]\d|\d)",
    "S": r"(?P<S>6[0-1]|[0-5]\d|\d)",
    "U": r"(?P<U>5[0-3]|[0-4]\d|\d)",
    "w": r"(?P<w>[0-6])",
    "u": r"(?P<u>[1-7])",
    "V": r"(?P<V>5[0-3]|0[1-9]|[1-4]\d|\d)",
    "y": r"(?P<y>\d\d)",
    "Y": r"(?P<Y>\d\d\d\d)",
    "z": r"(?P<z>[+-]\d\d:?[0-5]\d(:?[0-5]\d(\.\d{1,6})?)?|(?-i:Z))",
    "A": r"(?P<A>monday|tuesday|wednesday|thursday|friday|saturday|sunday)",
    "a": r"(?P<a>mon|tue|wed|thu|fri|sat|sun)",
    "B": r"(?P<B>january|february|march|april|may|june|july|august|september|october|november|december)",
    "b": r"(?P<b>jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)",
    "p": r"(?P<p>am|pm)",
    "Z": r"(?P<Z>utc|gmt|[a-z]{3,5}|[+-]\d\d(\d\d)?)",
    "%": "%",
}
_ALIASES = {"W": "U", "h": "b", "e": "d"}
_EXPAND = {"c": "%a %b %d %H:%M:%S %Y", "x": "%m/%d/%y", "X": "%H:%M:%S", "D": "%m/%d/%y",
           "T": "%H:%M:%S", "R": "%H:%M", "F": "%Y-%m-%d", "r": "%I:%M:%S %p"}

_cache = {}


def _pattern(fmt):
    p = _cache.get(fmt)
    if p is not None:
        return p
    out = []
    seen = set()
    i = 0
    while i < len(fmt):
        c = fmt[i]
        if c == "%" and i + 1 < len(fmt):
            k = fmt[i + 1]
            i += 2
            if k in _EXPAND:
                out.append(_pattern(_EXPAND[k]).pattern[4:-2])
                continue
            name = _ALIASES.get(k, k)
            body = _FIELDS.get(name)
            if body is None:
                raise ValueError("'%s' is a bad directive in format '%s'" % (k, fmt))
            if k == "W":
                body = body.replace("?P<U>", "?P<W>")
            if name in seen and body.startswith("(?P<"):
                body = "(?P=" + body[4:body.index(">")] + ")"
            seen.add(name)
            out.append(body)
        elif c == "%":
            raise ValueError("stray % in format '%s'" % fmt)
        elif c.isspace():
            while i < len(fmt) and fmt[i].isspace():
                i += 1
            out.append(r"\s+")
            continue
        else:
            out.append(re.escape(c))
            i += 1
    p = re.compile("(?i:" + "".join(out) + ")", re.IGNORECASE)
    _cache[fmt] = p
    return p


_MON = ["jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec"]
_DAY = ["mon", "tue", "wed", "thu", "fri", "sat", "sun"]


def _strptime(data, fmt):
    if not isinstance(data, str):
        raise TypeError("strptime() argument 1 must be str, not %s" % type(data).__name__)
    if not isinstance(fmt, str):
        raise TypeError("strptime() argument 2 must be str, not %s" % type(fmt).__name__)
    m = _pattern(fmt).match(data)
    if m is None:
        raise ValueError("time data %r does not match format %r" % (data, fmt))
    if m.end() != len(data):
        raise ValueError("unconverted data remains: %s" % data[m.end():])
    g = m.groupdict()
    year = None
    month = day = 1
    hour = minute = second = fraction = 0
    tz = -1
    gmtoff = None
    gmtoff_fraction = 0
    weekday = julian = None
    week = None
    week_start = None
    iso_year = iso_week = None
    for k, v in g.items():
        if v is None:
            continue
        if k == "y":
            year = int(v)
            year += 2000 if year <= 68 else 1900
        elif k == "Y":
            year = int(v)
        elif k == "G":
            iso_year = int(v)
        elif k == "m":
            month = int(v)
        elif k == "B" or k == "b":
            month = _MON.index(v[:3].lower()) + 1
        elif k == "d":
            day = int(v)
        elif k == "H":
            hour = int(v)
        elif k == "I":
            hour = int(v)
            ap = (g.get("p") or "").lower()
            if ap == "am":
                hour = 0 if hour == 12 else hour
            elif ap == "pm":
                hour = hour if hour == 12 else hour + 12
        elif k == "M":
            minute = int(v)
        elif k == "S":
            second = int(v)
        elif k == "f":
            fraction = int(v + "0" * (6 - len(v)))
        elif k == "A" or k == "a":
            weekday = _DAY.index(v[:3].lower())
        elif k == "w":
            weekday = (int(v) - 1) % 7
        elif k == "u":
            weekday = int(v) - 1
        elif k == "j":
            julian = int(v)
        elif k == "U" or k == "W":
            week = int(v)
            week_start = 6 if k == "U" else 0
        elif k == "V":
            iso_week = int(v)
        elif k == "z":
            if v == "Z" or v == "z":
                gmtoff = 0
            else:
                sign = -1 if v[0] == "-" else 1
                s = v[1:].replace(":", "")
                hh, mm = int(s[0:2]), int(s[2:4])
                ss = int(s[4:6]) if len(s) >= 6 else 0
                frac = s[7:] if len(s) > 7 else ""
                gmtoff = sign * (hh * 3600 + mm * 60 + ss)
                gmtoff_fraction = sign * int(frac + "0" * (6 - len(frac))) if frac else 0
        elif k == "Z":
            low = v.lower()
            if low in ("utc", "gmt"):
                tz = 0
    if year is None:
        if month == 2 and day == 29:
            year = 1904
        else:
            year = 1900
    if iso_year is not None and iso_week is not None and weekday is not None:
        import datetime
        dd = datetime.date.fromisocalendar(iso_year, iso_week, weekday + 1)
        year, month, day = dd.year, dd.month, dd.day
    elif julian is None and week is not None and weekday is not None:
        first = (_t._days_from_civil(year, 1, 1) + 3) % 7
        if week_start == 0:
            off = (7 - first) % 7
            julian = off + 1 + (week - 1) * 7 + weekday if week else 1 + weekday - first
        else:
            first = (first + 1) % 7
            wd = (weekday + 1) % 7
            off = (7 - first) % 7
            julian = off + 1 + (week - 1) * 7 + wd if week else 1 + wd - first
    if julian is not None:
        base = _t._days_from_civil(year, 1, 1) + julian - 1
        year, month, day = _t._civil_from_days(base)
    else:
        if month == 2 and day == 29 and not _t._leap(year):
            raise ValueError("day is out of range for month")
        mdays = [31, 29 if _t._leap(year) else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31]
        if day > mdays[month - 1]:
            raise ValueError("day is out of range for month")
        julian = _t._yday(year, month, day)
    weekday = (_t._days_from_civil(year, month, day) + 3) % 7
    return ((year, month, day, hour, minute, second, weekday, julian, tz, None, gmtoff),
            fraction, gmtoff_fraction)


def _strptime_time(data, fmt="%a %b %d %H:%M:%S %Y"):
    tt = _strptime(data, fmt)[0]
    return _t.struct_time(tt[:9], tt[9], tt[10])


def _strptime_datetime(cls, data, fmt="%a %b %d %H:%M:%S %Y"):
    tt, fraction, gmtoff_fraction = _strptime(data, fmt)
    args = tt[:6] + (fraction,)
    gmtoff = tt[10]
    if gmtoff is not None:
        import datetime
        delta = datetime.timedelta(seconds=gmtoff, microseconds=gmtoff_fraction)
        args += (datetime.timezone(delta),)
    return cls(*args)


def _strptime_datetime_date(cls, data, fmt):
    tt = _strptime(data, fmt)[0]
    return cls(tt[0], tt[1], tt[2])
