# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Parsing of date and time strings: a forgiving parse() and a strict isoparse()."""

import datetime
import re as _re

from dateutil import tz as _tz
from dateutil._common import monthrange
from dateutil.relativedelta import relativedelta

__all__ = ["parse", "parser", "parserinfo", "isoparse", "isoparser", "ParserError", "UnknownTimezoneWarning"]


class ParserError(ValueError):
    """A string could not be turned into a date."""

    def __str__(self):
        try:
            return self.args[0] % self.args[1:]
        except (TypeError, IndexError):
            return super(ParserError, self).__str__()

    def __repr__(self):
        return "%s(%s)" % (self.__class__.__name__, ", ".join(repr(a) for a in self.args))


class UnknownTimezoneWarning(RuntimeWarning):
    """A time zone name could not be resolved."""


class parserinfo:
    """Accepted for API compatibility; the default English tables are always used."""

    def __init__(self, dayfirst=False, yearfirst=False):
        self.dayfirst = dayfirst
        self.yearfirst = yearfirst


_MONTHS = (("jan", "january"), ("feb", "february"), ("mar", "march"), ("apr", "april"), ("may", "may"),
           ("jun", "june"), ("jul", "july"), ("aug", "august"), ("sep", "september"), ("oct", "october"),
           ("nov", "november"), ("dec", "december"))
_WEEKDAYS = (("mon", "monday"), ("tue", "tuesday"), ("wed", "wednesday"), ("thu", "thursday"), ("fri", "friday"),
             ("sat", "saturday"), ("sun", "sunday"))
_MONTH_INDEX = {}
for _i in range(12):
    _MONTH_INDEX[_MONTHS[_i][0]] = _i + 1
    _MONTH_INDEX[_MONTHS[_i][1]] = _i + 1
_MONTH_INDEX["sept"] = 9
_WEEKDAY_INDEX = {}
for _i in range(7):
    _WEEKDAY_INDEX[_WEEKDAYS[_i][0]] = _i
    _WEEKDAY_INDEX[_WEEKDAYS[_i][1]] = _i
_AM = ("am", "a")
_PM = ("pm", "p")
_UTC = ("utc", "gmt", "z")
_JUMP = (" ", ".", ",", ";", "-", "/", "'", "at", "on", "and", "ad", "m", "t", "of", "st", "nd", "rd", "th")
_TOKEN = _re.compile(r"\d+|[A-Za-z]+|\s+|.")


class _Result:
    def __init__(self):
        self.hour = None
        self.minute = None
        self.second = None
        self.microsecond = None
        self.ampm = None
        self.weekday = None
        self.tzname = None
        self.tzoffset = None
        self.any = False


def _tokens(s):
    out = []
    for t in _TOKEN.findall(s):
        if t.isspace():
            out.append(" ")
        else:
            out.append(t)
    return out


def _is_num(t):
    return t.isdigit()


def _is_alpha(t):
    return t.isalpha()


def _two_digit_year(y):
    if y >= 100:
        return y
    now = datetime.datetime.now().year
    century = now // 100 * 100
    y += century
    if y >= now + 50:
        y -= 100
    elif y < now - 50:
        y += 100
    return y


def _resolve(ymd, dayfirst, yearfirst):
    """Decide which of up to three numbers or month names is the year, month and day."""
    y = None
    m = None
    d = None
    n = len(ymd)
    if n == 0:
        return y, m, d
    mi = -1
    for i in range(n):
        if ymd[i][2]:
            mi = i
    if mi >= 0:
        m = ymd[mi][0]
        rest = [ymd[i] for i in range(n) if i != mi]
        if len(rest) == 0:
            pass
        elif len(rest) == 1:
            v = rest[0]
            if v[1] > 2 or v[0] > 31:
                y = v[0]
            else:
                d = v[0]
        elif len(rest) == 2:
            a = rest[0]
            b = rest[1]
            if mi == 0:
                if yearfirst and not (a[1] <= 2 and a[0] <= 31 and b[1] > 2):
                    y = a[0]
                    d = b[0]
                elif a[1] > 2 or a[0] > 31:
                    y = a[0]
                    d = b[0]
                else:
                    d = a[0]
                    y = b[0]
            else:
                if a[1] > 2 or a[0] > 31:
                    y = a[0]
                    d = b[0]
                else:
                    d = a[0]
                    y = b[0]
        else:
            raise ValueError("too many numbers")
        return y, m, d
    if n == 1:
        v = ymd[0]
        if v[1] > 2 or v[0] > 31:
            y = v[0]
        else:
            d = v[0]
        return y, m, d
    if n == 2:
        a = ymd[0]
        b = ymd[1]
        if a[1] > 2 or a[0] > 31:
            y = a[0]
            m = b[0]
        elif b[1] > 2 or b[0] > 31:
            m = a[0]
            y = b[0]
        elif a[0] > 12:
            d = a[0]
            m = b[0]
        elif b[0] > 12:
            m = a[0]
            d = b[0]
        elif dayfirst:
            d = a[0]
            m = b[0]
        else:
            m = a[0]
            d = b[0]
        return y, m, d
    if n > 3:
        raise ValueError("too many numbers")
    a = ymd[0]
    b = ymd[1]
    c = ymd[2]
    if a[1] > 2 or a[0] > 31:
        y = a[0]
        if dayfirst and not yearfirst and b[0] <= 31 and c[0] <= 12:
            d = b[0]
            m = c[0]
        else:
            m = b[0]
            d = c[0]
    elif yearfirst and a[0] <= 99 and b[0] <= 12:
        y = a[0]
        if dayfirst:
            d = b[0]
            m = c[0]
        else:
            m = b[0]
            d = c[0]
    elif dayfirst or (a[0] > 12 and b[0] <= 12):
        d = a[0]
        m = b[0]
        y = c[0]
    else:
        m = a[0]
        d = b[0]
        y = c[0]
        if b[0] > 12 and a[0] <= 12:
            pass
    return y, m, d


class parser:
    """A parser object; parse() the module function uses a shared instance."""

    def __init__(self, info=None):
        self.info = info if info is not None else parserinfo()

    def parse(self, timestr, default=None, ignoretz=False, tzinfos=None, **kwargs):
        dayfirst = kwargs.get("dayfirst", None)
        yearfirst = kwargs.get("yearfirst", None)
        fuzzy = kwargs.get("fuzzy", False)
        if dayfirst is None:
            dayfirst = self.info.dayfirst
        if yearfirst is None:
            yearfirst = self.info.yearfirst
        if not isinstance(timestr, str):
            raise TypeError("Parser must be called with a string")
        if default is None:
            default = datetime.datetime.now().replace(hour=0, minute=0, second=0, microsecond=0)
        res, ymd = self._parse(timestr, dayfirst, yearfirst, fuzzy)
        if res is None:
            raise ParserError("Unknown string format: %s", timestr)
        if not res.any and not ymd:
            raise ParserError("String does not contain a date: %s", timestr)
        try:
            ret = self._build(res, ymd, default, dayfirst, yearfirst)
        except ValueError as e:
            raise ParserError(str(e) + ": %s", timestr)
        except OverflowError as e:
            raise ParserError(str(e) + ": %s", timestr)
        if not ignoretz:
            ret = self._attach_tz(ret, res, tzinfos)
        return ret

    def _parse(self, timestr, dayfirst, yearfirst, fuzzy):
        toks = _tokens(timestr)
        n = len(toks)
        res = _Result()
        ymd = []
        i = 0
        while i < n:
            t = toks[i]
            if _is_num(t):
                i = self._number(toks, i, res, ymd)
                if i < 0:
                    return None, None
                continue
            if _is_alpha(t):
                low = t.lower()
                if low in _MONTH_INDEX:
                    ymd.append([_MONTH_INDEX[low], 0, True])
                    res.any = True
                elif low in _WEEKDAY_INDEX:
                    res.weekday = _WEEKDAY_INDEX[low]
                    res.any = True
                elif low in _AM or low in _PM:
                    if res.hour is None:
                        if fuzzy:
                            i += 1
                            continue
                        return None, None
                    if low in _PM:
                        if res.hour < 12:
                            res.hour += 12
                    elif res.hour == 12:
                        res.hour = 0
                    res.ampm = low
                elif low in _UTC:
                    res.tzname = t.upper() if len(t) > 1 else "UTC"
                    if low == "z":
                        res.tzname = "Z"
                    res.tzoffset = 0
                    res.any = True
                elif low in _JUMP:
                    pass
                elif res.hour is not None and res.tzname is None and t.isupper() and 3 <= len(t) <= 5:
                    res.tzname = t
                    res.any = True
                elif fuzzy:
                    pass
                else:
                    return None, None
                i += 1
                continue
            if (t == "+" or t == "-") and res.hour is not None and res.tzoffset is None and i + 1 < n and \
                    _is_num(toks[i + 1]):
                j = self._offset(toks, i, res)
                if j < 0:
                    return None, None
                i = j
                continue
            if t in _JUMP or fuzzy:
                i += 1
                continue
            return None, None
        return res, ymd

    def _offset(self, toks, i, res):
        n = len(toks)
        sign = -1 if toks[i] == "-" else 1
        digits = toks[i + 1]
        j = i + 2
        if len(digits) == 4:
            hh = int(digits[:2])
            mm = int(digits[2:])
        elif len(digits) <= 2:
            hh = int(digits)
            mm = 0
            if j + 1 < n and toks[j] == ":" and _is_num(toks[j + 1]) and len(toks[j + 1]) == 2:
                mm = int(toks[j + 1])
                j += 2
        else:
            return -1
        res.tzoffset = sign * (hh * 3600 + mm * 60)
        res.any = True
        return j

    def _number(self, toks, i, res, ymd):
        n = len(toks)
        t = toks[i]
        L = len(t)
        res.any = True
        if i + 1 < n and toks[i + 1] == ":" and res.hour is None and L <= 2:
            return self._time(toks, i, res)
        # hour followed by am/pm
        j = i + 1
        if j < n and toks[j] == " ":
            j += 1
        if j < n and L <= 2 and res.hour is None and _is_alpha(toks[j]) and toks[j].lower() in ("am", "pm", "a", "p"):
            res.hour = int(t)
            res.minute = 0
            return j
        # date with separators
        if i + 2 < n and toks[i + 1] in ("-", "/", ".") and _is_num(toks[i + 2]) and res.hour is None:
            sep = toks[i + 1]
            vals = [t]
            j = i + 1
            while len(vals) < 3 and j + 1 < n and toks[j] == sep and _is_num(toks[j + 1]):
                vals.append(toks[j + 1])
                j += 2
            for v in vals:
                ymd.append([int(v), len(v), False])
            return j
        if L == 8:
            ymd.append([int(t[:4]), 4, False])
            ymd.append([int(t[4:6]), 2, False])
            ymd.append([int(t[6:]), 2, False])
            return i + 1
        if L in (12, 14):
            ymd.append([int(t[:4]), 4, False])
            ymd.append([int(t[4:6]), 2, False])
            ymd.append([int(t[6:8]), 2, False])
            res.hour = int(t[8:10])
            res.minute = int(t[10:12])
            if L == 14:
                res.second = int(t[12:])
            return i + 1
        if L == 6 and not ymd:
            ymd.append([int(t[:2]), 2, False])
            ymd.append([int(t[2:4]), 2, False])
            ymd.append([int(t[4:]), 2, False])
            return i + 1
        if L == 6 or (L == 4 and ymd and res.hour is None and i > 0 and toks[i - 1] == "T"):
            if res.hour is None:
                res.hour = int(t[:2])
                res.minute = int(t[2:4])
                if L == 6:
                    res.second = int(t[4:])
                return i + 1
            return -1
        if L > 4 and L != 6:
            return -1
        ymd.append([int(t), L, False])
        return i + 1

    def _time(self, toks, i, res):
        n = len(toks)
        res.hour = int(toks[i])
        i += 2
        if i >= n or not _is_num(toks[i]):
            return -1
        res.minute = int(toks[i])
        i += 1
        if i + 1 < n and toks[i] == ":" and _is_num(toks[i + 1]):
            res.second = int(toks[i + 1])
            i += 2
            if i + 1 < n and (toks[i] == "." or toks[i] == ",") and _is_num(toks[i + 1]):
                res.microsecond = int((toks[i + 1] + "000000")[:6])
                i += 2
        return i

    def _build(self, res, ymd, default, dayfirst, yearfirst):
        y, m, d = _resolve(ymd, dayfirst, yearfirst)
        if y is not None:
            y = _two_digit_year(y)
        year = y if y is not None else default.year
        month = m if m is not None else default.month
        if d is not None:
            day = d
        else:
            day = min(default.day, monthrange(year, month)[1])
        repl = {"year": year, "month": month, "day": day}
        if res.hour is not None:
            repl["hour"] = res.hour
        if res.minute is not None:
            repl["minute"] = res.minute
        if res.second is not None:
            repl["second"] = res.second
        if res.microsecond is not None:
            repl["microsecond"] = res.microsecond
        if res.hour is not None and res.minute is None:
            repl["minute"] = 0
        ret = default.replace(**repl)
        if res.weekday is not None and d is None:
            ret = ret + relativedelta(weekday=res.weekday)
        return ret

    def _attach_tz(self, ret, res, tzinfos):
        if res.tzname is None and res.tzoffset is None:
            return ret
        tzname = res.tzname
        offset = res.tzoffset
        if tzinfos is not None and tzname is not None:
            tzdata = None
            if callable(tzinfos):
                tzdata = tzinfos(tzname, offset)
            elif tzname in tzinfos:
                tzdata = tzinfos[tzname]
            if tzdata is not None:
                if isinstance(tzdata, datetime.tzinfo):
                    return ret.replace(tzinfo=tzdata)
                if isinstance(tzdata, int):
                    return ret.replace(tzinfo=_tz.tzoffset(tzname, tzdata))
                if isinstance(tzdata, str):
                    z = _tz.gettz(tzdata)
                    if z is not None:
                        return ret.replace(tzinfo=z)
                raise ValueError("Offset must be tzinfo subclass, tz string, or int offset.")
        if tzname in ("UTC", "GMT", "Z") or offset == 0:
            return ret.replace(tzinfo=_tz.UTC)
        if offset is not None:
            return ret.replace(tzinfo=_tz.tzoffset(tzname, offset))
        return ret


DEFAULTPARSER = parser()


def parse(timestr, parserinfo=None, **kwargs):
    if parserinfo is not None:
        return parser(parserinfo).parse(timestr, **kwargs)
    return DEFAULTPARSER.parse(timestr, **kwargs)


# --------------------------------------------------------------------------------------------------------------
# ISO 8601

_ISO_TZ = _re.compile(r"^(.*?)(Z|[+-]\d{2}(?::?\d{2})?)$")


class isoparser:
    """A strict ISO 8601 parser."""

    def __init__(self, sep=None):
        if sep is not None:
            if len(sep) != 1 or sep.isdigit():
                raise ValueError("Separator must be a single, non-numeric ASCII character")
        self._sep = sep

    def isoparse(self, dt_str):
        if not isinstance(dt_str, str):
            raise TypeError("ISO string must be a string")
        date_str, time_str = self._split(dt_str)
        components = self._date_components(date_str)
        if time_str is None:
            return datetime.datetime(components[0], components[1], components[2])
        hh, mm, ss, us, tzinfo, next_day = self._time_components(time_str)
        dt = datetime.datetime(components[0], components[1], components[2], hh, mm, ss, us, tzinfo)
        if next_day:
            dt += datetime.timedelta(days=1)
        return dt

    def parse_isodate(self, datestr):
        c = self._date_components(datestr)
        return datetime.date(c[0], c[1], c[2])

    def parse_isotime(self, timestr):
        hh, mm, ss, us, tzinfo, next_day = self._time_components(timestr)
        if next_day:
            hh = 0
        return datetime.time(hh, mm, ss, us, tzinfo)

    def _split(self, s):
        # The date part ends before the first separator character.
        m = _re.match(r"^(\d{4}-?W\d{2}(?:-?\d)?|\d{4}-\d{2}-\d{2}|\d{8}|\d{4}-\d{2}|\d{4})(.*)$", s)
        if m is None:
            raise ValueError("ISO string too short")
        date_str = m.group(1)
        rest = m.group(2)
        if rest == "":
            return date_str, None
        if self._sep is not None and rest[0] != self._sep:
            raise ValueError("Invalid character found when parsing ISO string")
        if len(rest) < 2:
            raise ValueError("Incomplete time component")
        return date_str, rest[1:]

    def _date_components(self, s):
        m = _re.match(r"^(\d{4})-?W(\d{2})(?:-?(\d))?$", s)
        if m is not None:
            year = int(m.group(1))
            week = int(m.group(2))
            dow = int(m.group(3)) if m.group(3) else 1
            if week < 1 or week > 53:
                raise ValueError("Invalid week: " + str(week))
            if dow < 1 or dow > 7:
                raise ValueError("Invalid weekday: " + str(dow) + " (must be 1-7)")
            jan4 = datetime.date(year, 1, 4)
            week1_monday = jan4 - datetime.timedelta(days=jan4.weekday())
            d = week1_monday + datetime.timedelta(days=(week - 1) * 7 + dow - 1)
            return d.year, d.month, d.day
        m = _re.match(r"^(\d{4})(?:-(\d{2})(?:-(\d{2}))?)?$", s)
        if m is None:
            m = _re.match(r"^(\d{4})(\d{2})(\d{2})$", s)
        if m is None:
            raise ValueError("Invalid ISO date: " + s)
        year = int(m.group(1))
        month = int(m.group(2)) if m.group(2) else 1
        day = int(m.group(3)) if m.group(3) else 1
        return year, month, day

    def _time_components(self, s):
        tzinfo = None
        m = _ISO_TZ.match(s)
        body = s
        if m is not None:
            body = m.group(1)
            z = m.group(2)
            if z == "Z":
                tzinfo = _tz.UTC
            else:
                sign = -1 if z[0] == "-" else 1
                digits = z[1:].replace(":", "")
                hh = int(digits[:2])
                mm = int(digits[2:]) if len(digits) > 2 else 0
                if hh > 23 or mm > 59:
                    raise ValueError("Invalid time zone offset")
                off = sign * (hh * 3600 + mm * 60)
                tzinfo = _tz.UTC if off == 0 else _tz.tzoffset(None, off)
        mm_ = _re.match(r"^(\d{2})(?::(\d{2})(?::(\d{2})(?:[.,](\d+))?)?)?$", body)
        if mm_ is None:
            mm_ = _re.match(r"^(\d{2})(?:(\d{2})(?:(\d{2})(?:[.,](\d+))?)?)?$", body)
        if mm_ is None:
            raise ValueError("Invalid ISO time: " + s)
        hh = int(mm_.group(1))
        mi = int(mm_.group(2)) if mm_.group(2) else 0
        ss = int(mm_.group(3)) if mm_.group(3) else 0
        us = int((mm_.group(4) + "000000")[:6]) if mm_.group(4) else 0
        next_day = False
        if hh == 24:
            if mi != 0 or ss != 0 or us != 0:
                raise ValueError("Hour may only be 24 at 24:00:00.000")
            hh = 0
            next_day = True
        if hh > 23 or mi > 59 or ss > 59:
            raise ValueError("Invalid time component")
        return hh, mi, ss, us, tzinfo, next_day


DEFAULT_ISOPARSER = isoparser()


def isoparse(dt_str):
    return DEFAULT_ISOPARSER.isoparse(dt_str)
