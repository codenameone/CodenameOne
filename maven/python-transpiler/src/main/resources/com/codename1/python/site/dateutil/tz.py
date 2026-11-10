# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Time zones: UTC, fixed offsets, the local zone and simple UTC+N strings.

Zones that need the tz database (named regions with daylight saving rules) are
not available; gettz answers None for them.
"""

import datetime
import re as _re
import time as _time

__all__ = ["tzutc", "tzoffset", "tzlocal", "tzstr", "gettz", "UTC", "datetime_exists", "datetime_ambiguous",
           "resolve_imaginary"]

ZERO = datetime.timedelta(0)


class tzutc(datetime.tzinfo):
    """The UTC zone; every instance is the same object."""
    _instance = None

    def __new__(cls):
        if cls._instance is None:
            cls._instance = datetime.tzinfo.__new__(cls)
        return cls._instance

    def utcoffset(self, dt):
        return ZERO

    def dst(self, dt):
        return ZERO

    def tzname(self, dt):
        return "UTC"

    def is_ambiguous(self, dt):
        return False

    def fromutc(self, dt):
        return dt

    def __eq__(self, other):
        if isinstance(other, tzutc):
            return True
        if isinstance(other, tzoffset):
            return other._offset == ZERO
        return NotImplemented

    def __ne__(self, other):
        r = self.__eq__(other)
        if r is NotImplemented:
            return r
        return not r

    def __hash__(self):
        return hash(ZERO)

    def __repr__(self):
        return "tzutc()"


UTC = tzutc()


class tzoffset(datetime.tzinfo):
    """A zone with a fixed offset from UTC, given in seconds or as a timedelta."""
    _cache = {}

    def __new__(cls, name, offset):
        if isinstance(offset, datetime.timedelta):
            secs = int(offset.total_seconds())
        else:
            secs = int(offset)
        key = (name, secs)
        inst = cls._cache.get(key)
        if inst is None:
            inst = datetime.tzinfo.__new__(cls)
            inst._name = name
            inst._offset = datetime.timedelta(seconds=secs)
            cls._cache[key] = inst
        return inst

    def __init__(self, name, offset):
        pass

    def utcoffset(self, dt):
        return self._offset

    def dst(self, dt):
        return ZERO

    def tzname(self, dt):
        return self._name

    def is_ambiguous(self, dt):
        return False

    def fromutc(self, dt):
        return dt + self._offset

    def __eq__(self, other):
        if isinstance(other, tzoffset):
            return self._offset == other._offset
        if isinstance(other, tzutc):
            return self._offset == ZERO
        return NotImplemented

    def __ne__(self, other):
        r = self.__eq__(other)
        if r is NotImplemented:
            return r
        return not r

    def __hash__(self):
        return hash(self._offset)

    def __repr__(self):
        return "%s(%s, %d)" % (self.__class__.__name__, repr(self._name), int(self._offset.total_seconds()))


class tzlocal(datetime.tzinfo):
    """The zone of the machine, from the time module's view of it."""

    def __init__(self):
        std = -getattr(_time, "timezone", 0)
        self._std_offset = datetime.timedelta(seconds=std)
        if getattr(_time, "daylight", 0):
            self._dst_offset = datetime.timedelta(seconds=-getattr(_time, "altzone", -std))
        else:
            self._dst_offset = self._std_offset
        self._dst_saved = self._dst_offset - self._std_offset
        self._hasdst = bool(self._dst_saved)
        names = getattr(_time, "tzname", ("UTC", "UTC"))
        self._tznames = names

    def utcoffset(self, dt):
        if dt is None and self._hasdst:
            return None
        if self._isdst(dt):
            return self._dst_offset
        return self._std_offset

    def dst(self, dt):
        if dt is None and self._hasdst:
            return None
        if self._isdst(dt):
            return self._dst_saved
        return ZERO

    def tzname(self, dt):
        return self._tznames[1 if self._isdst(dt) else 0]

    def is_ambiguous(self, dt):
        return False

    def _isdst(self, dt):
        if not self._hasdst or dt is None:
            return False
        try:
            stamp = (dt.replace(tzinfo=None) - datetime.datetime(1970, 1, 1)).total_seconds() - \
                self._std_offset.total_seconds()
            return bool(_time.localtime(stamp).tm_isdst > 0)
        except (OverflowError, ValueError, OSError, AttributeError):
            return False

    def __eq__(self, other):
        if isinstance(other, tzlocal):
            return self._std_offset == other._std_offset and self._dst_offset == other._dst_offset
        if isinstance(other, tzutc):
            return not self._hasdst and self._std_offset == ZERO
        if isinstance(other, tzoffset):
            return not self._hasdst and self._std_offset == other._offset
        return NotImplemented

    def __ne__(self, other):
        r = self.__eq__(other)
        if r is NotImplemented:
            return r
        return not r

    def __hash__(self):
        return hash((self._std_offset, self._dst_offset))

    def __repr__(self):
        return "tzlocal()"


_TZSTR = _re.compile(r"^([A-Za-z]{3,})?([+-])(\d{1,2})(?::?(\d{2}))?$")


class tzstr(datetime.tzinfo):
    """A zone from a string such as UTC+3 or GMT-05:30 (without daylight saving)."""

    def __init__(self, s):
        m = _TZSTR.match(s)
        if m is None:
            raise ValueError("unknown string format")
        self._s = s
        self._std_abbr = m.group(1) or ""
        secs = int(m.group(3)) * 3600 + int(m.group(4) or 0) * 60
        if m.group(2) == "-":
            secs = -secs
        self._offset = datetime.timedelta(seconds=secs)

    def utcoffset(self, dt):
        return self._offset

    def dst(self, dt):
        return ZERO

    def tzname(self, dt):
        return self._std_abbr

    def is_ambiguous(self, dt):
        return False

    def fromutc(self, dt):
        return dt + self._offset

    def __eq__(self, other):
        if not isinstance(other, tzstr):
            return NotImplemented
        return self._s == other._s

    def __ne__(self, other):
        r = self.__eq__(other)
        if r is NotImplemented:
            return r
        return not r

    def __hash__(self):
        return hash(self._s)

    def __repr__(self):
        return "%s(%s)" % (self.__class__.__name__, repr(self._s))


def gettz(name=None):
    """Return a zone for a name, or None when it is not known."""
    if not name:
        return tzlocal()
    if name in ("GMT", "UTC"):
        return UTC
    if name in getattr(_time, "tzname", ()):
        return tzlocal()
    if _TZSTR.match(name):
        return tzstr(name)
    return None


def datetime_exists(dt, tz=None):
    if tz is None:
        if dt.tzinfo is None:
            raise ValueError("Datetime is naive and no time zone provided.")
        tz = dt.tzinfo
    dt = dt.replace(tzinfo=None)
    dt_rt = dt.replace(tzinfo=tz).astimezone(UTC).astimezone(tz)
    dt_rt = dt_rt.replace(tzinfo=None)
    return dt == dt_rt


def datetime_ambiguous(dt, tz=None):
    if tz is None:
        if dt.tzinfo is None:
            raise ValueError("Ambiguous naive datetime and no time zone provided.")
        tz = dt.tzinfo
    is_ambiguous = getattr(tz, "is_ambiguous", None)
    if is_ambiguous is not None:
        return is_ambiguous(dt)
    return False


def resolve_imaginary(dt):
    if dt.tzinfo is not None and not datetime_exists(dt):
        curr_offset = (dt + datetime.timedelta(hours=24)).utcoffset()
        old_offset = (dt - datetime.timedelta(hours=24)).utcoffset()
        dt += curr_offset - old_offset
    return dt
