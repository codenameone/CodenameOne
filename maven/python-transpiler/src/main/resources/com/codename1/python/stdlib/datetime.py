# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Dates, times, durations and fixed-offset time zones.

Dates are proleptic Gregorian. Local time is the device's default zone,
asked for its UTC offset at each instant.
"""

import time as _time
import _time as _clock

__all__ = ["date", "datetime", "time", "timedelta", "timezone", "tzinfo", "MINYEAR", "MAXYEAR", "UTC"]

MINYEAR = 1
MAXYEAR = 9999
_MAXORDINAL = 3652059
_EPOCH_ORDINAL = 719163
_DIM = [0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31]


def _is_leap(y):
    return y % 4 == 0 and (y % 100 != 0 or y % 400 == 0)


def _days_in_month(y, m):
    return 29 if m == 2 and _is_leap(y) else _DIM[m]


def _ymd2ord(y, m, d):
    return _time._days_from_civil(y, m, d) + _EPOCH_ORDINAL


def _ord2ymd(n):
    return _time._civil_from_days(n - _EPOCH_ORDINAL)


def _cmp(x, y):
    return 0 if x == y else (1 if x > y else -1)


def _name(obj):
    t = type(obj)
    return "%s.%s" % (t.__module__, t.__qualname__)


def _index(v, what):
    if isinstance(v, bool) or not isinstance(v, int):
        if isinstance(v, float):
            raise TypeError("'float' object cannot be interpreted as an integer")
        try:
            return v.__index__()
        except AttributeError:
            raise TypeError("'%s' object cannot be interpreted as an integer" % type(v).__name__)
    return v


def _check_date(year, month, day):
    year = _index(year, "year")
    month = _index(month, "month")
    day = _index(day, "day")
    if not MINYEAR <= year <= MAXYEAR:
        raise ValueError("year must be in %d..%d, not %d" % (MINYEAR, MAXYEAR, year))
    if not 1 <= month <= 12:
        raise ValueError("month must be in 1..12, not %d" % month)
    dim = _days_in_month(year, month)
    if not 1 <= day <= dim:
        raise ValueError("day %d must be in range 1..%d for month %d in year %d" % (day, dim, month, year))
    return year, month, day


def _check_time(hour, minute, second, microsecond, fold):
    hour = _index(hour, "hour")
    minute = _index(minute, "minute")
    second = _index(second, "second")
    microsecond = _index(microsecond, "microsecond")
    if not 0 <= hour <= 23:
        raise ValueError("hour must be in 0..23")
    if not 0 <= minute <= 59:
        raise ValueError("minute must be in 0..59")
    if not 0 <= second <= 59:
        raise ValueError("second must be in 0..59")
    if not 0 <= microsecond <= 999999:
        raise ValueError("microsecond must be in 0..999999")
    if fold not in (0, 1):
        raise ValueError("fold must be either 0 or 1")
    return hour, minute, second, microsecond, fold


def _check_tzinfo(tz):
    if tz is not None and not isinstance(tz, tzinfo):
        raise TypeError("tzinfo argument must be None or of a tzinfo subclass, not type '%s'"
                        % type(tz).__name__)


def _check_offset(name, offset):
    if offset is None:
        return
    if not isinstance(offset, timedelta):
        raise TypeError("tzinfo.%s() must return None or timedelta, not '%s'" % (name, type(offset).__name__))
    if not -timedelta(1) < offset < timedelta(1):
        raise ValueError("offset must be a timedelta strictly between -timedelta(hours=24) and "
                         "timedelta(hours=24), not %r." % (offset,))


def _format_time(hh, mm, ss, us, timespec="auto"):
    if timespec == "auto":
        timespec = "microseconds" if us else "seconds"
    elif timespec == "milliseconds":
        us //= 1000
    if timespec == "hours":
        return "%02d" % hh
    if timespec == "minutes":
        return "%02d:%02d" % (hh, mm)
    if timespec == "seconds":
        return "%02d:%02d:%02d" % (hh, mm, ss)
    if timespec == "milliseconds":
        return "%02d:%02d:%02d.%03d" % (hh, mm, ss, us)
    if timespec == "microseconds":
        return "%02d:%02d:%02d.%06d" % (hh, mm, ss, us)
    raise ValueError("Unknown timespec value")


def _format_offset(off, sep=":"):
    if off is None:
        return ""
    if off.days < 0:
        sign = "-"
        off = -off
    else:
        sign = "+"
    hh, mm = divmod(off, timedelta(hours=1))
    mm, ss = divmod(mm, timedelta(minutes=1))
    s = "%s%02d%s%02d" % (sign, hh, sep, mm)
    if ss or ss.microseconds:
        s += "%s%02d" % (sep, ss.seconds)
        if ss.microseconds:
            s += ".%06d" % ss.microseconds
    return s


def _round_half_even(x):
    return int(round(x))


class timedelta:
    __slots__ = ("_days", "_seconds", "_microseconds", "_hashcode")

    def __new__(cls, days=0, seconds=0, microseconds=0, milliseconds=0, minutes=0, hours=0, weeks=0):
        exact = 0
        inexact = 0.0
        for v, scale in ((days, 86400000000), (seconds, 1000000), (microseconds, 1),
                         (milliseconds, 1000), (minutes, 60000000), (hours, 3600000000),
                         (weeks, 604800000000)):
            if isinstance(v, bool) or isinstance(v, int):
                exact += v * scale
            elif isinstance(v, float):
                ip = int(v)
                exact += ip * scale
                inexact += (v - ip) * scale
            else:
                raise TypeError("unsupported type for timedelta %s component: %s"
                                % (_component(scale), type(v).__name__))
        whole = int(inexact)
        total = exact + whole
        frac = inexact - whole
        if frac > 0.5 or (frac == 0.5 and total % 2 == 1):
            total += 1
        elif frac < -0.5 or (frac == -0.5 and total % 2 == 1):
            total -= 1
        return cls._from_us(total)

    @classmethod
    def _from_us(cls, total):
        d, rem = divmod(total, 86400000000)
        s, us = divmod(rem, 1000000)
        if abs(d) > 999999999:
            raise OverflowError("days=%d; must have magnitude <= 999999999" % d)
        self = object.__new__(cls)
        self._days = d
        self._seconds = s
        self._microseconds = us
        self._hashcode = -1
        return self

    def _us(self):
        return (self._days * 86400 + self._seconds) * 1000000 + self._microseconds

    @property
    def days(self):
        return self._days

    @property
    def seconds(self):
        return self._seconds

    @property
    def microseconds(self):
        return self._microseconds

    def total_seconds(self):
        return self._us() / 10 ** 6

    def __repr__(self):
        args = []
        if self._days:
            args.append("days=%d" % self._days)
        if self._seconds:
            args.append("seconds=%d" % self._seconds)
        if self._microseconds:
            args.append("microseconds=%d" % self._microseconds)
        if not args:
            args.append("0")
        return "%s(%s)" % (_name(self), ", ".join(args))

    def __str__(self):
        mm, ss = divmod(self._seconds, 60)
        hh, mm = divmod(mm, 60)
        s = "%d:%02d:%02d" % (hh, mm, ss)
        if self._days:
            s = "%d day%s, " % (self._days, "" if abs(self._days) == 1 else "s") + s
        if self._microseconds:
            s += ".%06d" % self._microseconds
        return s

    def __add__(self, other):
        if isinstance(other, timedelta):
            return timedelta._from_us(self._us() + other._us())
        return NotImplemented

    __radd__ = __add__

    def __sub__(self, other):
        if isinstance(other, timedelta):
            return timedelta._from_us(self._us() - other._us())
        return NotImplemented

    def __rsub__(self, other):
        if isinstance(other, timedelta):
            return -self + other
        return NotImplemented

    def __neg__(self):
        return timedelta._from_us(-self._us())

    def __pos__(self):
        return self

    def __abs__(self):
        return -self if self._days < 0 else self

    def __mul__(self, other):
        if isinstance(other, int):
            return timedelta._from_us(self._us() * other)
        if isinstance(other, float):
            a, b = other.as_integer_ratio()
            return timedelta._from_us(_div_even(self._us() * a, b))
        return NotImplemented

    __rmul__ = __mul__

    def __floordiv__(self, other):
        if isinstance(other, timedelta):
            return self._us() // other._us()
        if isinstance(other, int):
            return timedelta._from_us(self._us() // other)
        return NotImplemented

    def __truediv__(self, other):
        if isinstance(other, timedelta):
            return self._us() / other._us()
        if isinstance(other, int):
            return timedelta._from_us(_div_even(self._us(), other))
        if isinstance(other, float):
            a, b = other.as_integer_ratio()
            return timedelta._from_us(_div_even(b * self._us(), a))
        return NotImplemented

    def __mod__(self, other):
        if isinstance(other, timedelta):
            return timedelta._from_us(self._us() % other._us())
        return NotImplemented

    def __divmod__(self, other):
        if isinstance(other, timedelta):
            q, r = divmod(self._us(), other._us())
            return q, timedelta._from_us(r)
        return NotImplemented

    def __eq__(self, other):
        if isinstance(other, timedelta):
            return self._us() == other._us()
        return NotImplemented

    def __lt__(self, other):
        if isinstance(other, timedelta):
            return self._us() < other._us()
        return NotImplemented

    def __le__(self, other):
        if isinstance(other, timedelta):
            return self._us() <= other._us()
        return NotImplemented

    def __gt__(self, other):
        if isinstance(other, timedelta):
            return self._us() > other._us()
        return NotImplemented

    def __ge__(self, other):
        if isinstance(other, timedelta):
            return self._us() >= other._us()
        return NotImplemented

    def __hash__(self):
        if self._hashcode == -1:
            self._hashcode = hash((self._days, self._seconds, self._microseconds))
        return self._hashcode

    def __bool__(self):
        return self._days != 0 or self._seconds != 0 or self._microseconds != 0

    def __reduce__(self):
        return (type(self), (self._days, self._seconds, self._microseconds))


def _component(scale):
    return {86400000000: "days", 1000000: "seconds", 1: "microseconds", 1000: "milliseconds",
            60000000: "minutes", 3600000000: "hours", 604800000000: "weeks"}[scale]


def _div_even(a, b):
    q, r = divmod(a, b)
    r2 = r * 2
    if b > 0:
        if r2 > b or (r2 == b and q % 2 == 1):
            q += 1
    else:
        if r2 < b or (r2 == b and q % 2 == 1):
            q += 1
    return q


timedelta.min = timedelta(-999999999)
timedelta.max = timedelta(days=999999999, hours=23, minutes=59, seconds=59, microseconds=999999)
timedelta.resolution = timedelta(microseconds=1)


class date:
    __slots__ = ("_year", "_month", "_day", "_hashcode")

    def __new__(cls, year, month=None, day=None):
        year, month, day = _check_date(year, month, day)
        self = object.__new__(cls)
        self._year = year
        self._month = month
        self._day = day
        self._hashcode = -1
        return self

    @classmethod
    def fromtimestamp(cls, t):
        y, m, d, hh, mm, ss, us = _local_fields(t)
        return cls(y, m, d)

    @classmethod
    def today(cls):
        return cls.fromtimestamp(_time.time())

    @classmethod
    def fromordinal(cls, n):
        if not 1 <= n <= _MAXORDINAL:
            raise ValueError("ordinal must be in 1..%d" % _MAXORDINAL)
        y, m, d = _ord2ymd(n)
        return cls(y, m, d)

    @classmethod
    def fromisoformat(cls, s):
        if not isinstance(s, str):
            raise TypeError("fromisoformat: argument must be str")
        try:
            ymd, n = _parse_date(s)
            if n != len(s):
                raise ValueError
        except (ValueError, IndexError):
            raise ValueError("Invalid isoformat string: %r" % (s,))
        return cls(*ymd)

    @classmethod
    def fromisocalendar(cls, year, week, day):
        return cls(*_isocal_to_ymd(year, week, day))

    @classmethod
    def strptime(cls, date_string, format):
        import _strptime
        return _strptime._strptime_datetime_date(cls, date_string, format)

    @property
    def year(self):
        return self._year

    @property
    def month(self):
        return self._month

    @property
    def day(self):
        return self._day

    def __repr__(self):
        return "%s(%d, %d, %d)" % (_name(self), self._year, self._month, self._day)

    def isoformat(self):
        return "%04d-%02d-%02d" % (self._year, self._month, self._day)

    __str__ = isoformat

    def ctime(self):
        wd = self.toordinal() % 7 or 7
        return "%s %s %2d 00:00:00 %04d" % (_time._DAYS[wd - 1][:3], _time._MONTHS[self._month - 1][:3],
                                            self._day, self._year)

    def strftime(self, format):
        return _time._format(format, self._tuple(0, 0, 0), None, None, 0)

    def __format__(self, fmt):
        if not isinstance(fmt, str):
            raise TypeError("must be str, not %s" % type(fmt).__name__)
        return self.strftime(fmt) if fmt else str(self)

    def _tuple(self, hh, mm, ss):
        return (self._year, self._month, self._day, hh, mm, ss, self.weekday(),
                _time._yday(self._year, self._month, self._day), -1)

    def timetuple(self):
        return _time.struct_time(self._tuple(0, 0, 0))

    def toordinal(self):
        return _ymd2ord(self._year, self._month, self._day)

    def replace(self, year=None, month=None, day=None):
        return type(self)(self._year if year is None else year, self._month if month is None else month,
                          self._day if day is None else day)

    __replace__ = replace

    def weekday(self):
        return (self.toordinal() + 6) % 7

    def isoweekday(self):
        return self.toordinal() % 7 or 7

    def isocalendar(self):
        y, w, d = _time._iso(self._year, self._month, self._day)
        return IsoCalendarDate(y, w, d)

    def _cmpkey(self):
        return (self._year, self._month, self._day)

    def _plain(self, other):
        return isinstance(other, date) and not isinstance(other, datetime) and not isinstance(self, datetime)

    def __eq__(self, other):
        if self._plain(other):
            return self._cmpkey() == other._cmpkey()
        return NotImplemented

    def __lt__(self, other):
        if self._plain(other):
            return self._cmpkey() < other._cmpkey()
        return NotImplemented

    def __le__(self, other):
        if self._plain(other):
            return self._cmpkey() <= other._cmpkey()
        return NotImplemented

    def __gt__(self, other):
        if self._plain(other):
            return self._cmpkey() > other._cmpkey()
        return NotImplemented

    def __ge__(self, other):
        if self._plain(other):
            return self._cmpkey() >= other._cmpkey()
        return NotImplemented

    def __hash__(self):
        if self._hashcode == -1:
            self._hashcode = hash(self._cmpkey())
        return self._hashcode

    def __add__(self, other):
        if isinstance(other, timedelta):
            o = self.toordinal() + other.days
            if 0 < o <= _MAXORDINAL:
                return type(self).fromordinal(o)
            raise OverflowError("result out of range")
        return NotImplemented

    __radd__ = __add__

    def __sub__(self, other):
        if isinstance(other, timedelta):
            return self + timedelta(-other.days)
        if isinstance(other, date) and not isinstance(other, datetime):
            return timedelta(self.toordinal() - other.toordinal())
        return NotImplemented

    def __reduce__(self):
        return (type(self), (self._year, self._month, self._day))


date.min = date(1, 1, 1)
date.max = date(9999, 12, 31)
date.resolution = timedelta(days=1)


class IsoCalendarDate(tuple):
    def __new__(cls, year, week, weekday):
        return tuple.__new__(cls, (year, week, weekday))

    year = property(lambda self: self[0])
    week = property(lambda self: self[1])
    weekday = property(lambda self: self[2])

    def __repr__(self):
        return "datetime.IsoCalendarDate(year=%d, week=%d, weekday=%d)" % tuple(self)

    def __reduce__(self):
        return (tuple, (tuple(self),))


def _isocal_to_ymd(year, week, day):
    if not MINYEAR <= year <= MAXYEAR:
        raise ValueError("year %d is out of range" % year)
    if not 0 < week < 53:
        out = True
        if week == 53:
            first = _ymd2ord(year, 1, 1) % 7
            if first == 4 or (first == 3 and _is_leap(year)):
                out = False
        if out:
            raise ValueError("Invalid week: %d" % week)
    if not 0 < day < 8:
        raise ValueError("Invalid weekday: %d (range is [1, 7])" % day)
    first = _ymd2ord(year, 1, 1)
    wd = (first + 6) % 7
    week1 = first - wd
    if wd > 3:
        week1 += 7
    return _ord2ymd(week1 + (week - 1) * 7 + day - 1)


class tzinfo:
    __slots__ = ()

    def tzname(self, dt):
        raise NotImplementedError("tzinfo subclass must override tzname()")

    def utcoffset(self, dt):
        raise NotImplementedError("tzinfo subclass must override utcoffset()")

    def dst(self, dt):
        raise NotImplementedError("tzinfo subclass must override dst()")

    def fromutc(self, dt):
        if not isinstance(dt, datetime):
            raise TypeError("fromutc() requires a datetime argument")
        if dt.tzinfo is not self:
            raise ValueError("dt.tzinfo is not self")
        dtoff = dt.utcoffset()
        if dtoff is None:
            raise ValueError("fromutc() requires a non-None utcoffset() result")
        dtdst = dt.dst()
        if dtdst is None:
            raise ValueError("fromutc() requires a non-None dst() result")
        delta = dtoff - dtdst
        if delta:
            dt += delta
            dtdst = dt.dst()
            if dtdst is None:
                raise ValueError("fromutc(): dt.dst gave inconsistent results; cannot convert")
        return dt + dtdst


class timezone(tzinfo):
    __slots__ = ("_offset", "_name")

    _Omitted = object()

    def __new__(cls, offset, name=_Omitted):
        if not isinstance(offset, timedelta):
            raise TypeError("timezone() argument 1 must be datetime.timedelta, not %s" % type(offset).__name__)
        if name is cls._Omitted:
            if not offset:
                return cls.utc
            name = None
        elif not isinstance(name, str):
            raise TypeError("timezone() argument 2 must be str, not %s" % type(name).__name__)
        if not cls._minoffset <= offset <= cls._maxoffset:
            raise ValueError("offset must be a timedelta strictly between -timedelta(hours=24) and "
                             "timedelta(hours=24), not %r" % (offset,))
        return cls._create(offset, name)

    @classmethod
    def _create(cls, offset, name=None):
        self = tzinfo.__new__(cls)
        self._offset = offset
        self._name = name
        return self

    def __eq__(self, other):
        if isinstance(other, timezone):
            return self._offset == other._offset
        return NotImplemented

    def __hash__(self):
        return hash(self._offset)

    def __repr__(self):
        if self is self.utc:
            return "datetime.timezone.utc"
        if self._name is None:
            return "%s(%r)" % (_name(self), self._offset)
        return "%s(%r, %r)" % (_name(self), self._offset, self._name)

    def __str__(self):
        return self.tzname(None)

    def utcoffset(self, dt):
        if isinstance(dt, datetime) or dt is None:
            return self._offset
        raise TypeError("utcoffset() argument must be a datetime instance or None")

    def tzname(self, dt):
        if isinstance(dt, datetime) or dt is None:
            if self._name is None:
                return self._name_from_offset(self._offset)
            return self._name
        raise TypeError("tzname() argument must be a datetime instance or None")

    def dst(self, dt):
        if isinstance(dt, datetime) or dt is None:
            return None
        raise TypeError("dst() argument must be a datetime instance or None")

    def fromutc(self, dt):
        if isinstance(dt, datetime):
            if dt.tzinfo is not self:
                raise ValueError("fromutc: dt.tzinfo is not self")
            return dt + self._offset
        raise TypeError("fromutc() argument must be a datetime instance or None")

    @staticmethod
    def _name_from_offset(delta):
        if not delta:
            return "UTC"
        return "UTC" + _format_offset(delta)

    def __getinitargs__(self):
        if self._name is None:
            return (self._offset,)
        return (self._offset, self._name)


timezone._maxoffset = timedelta(hours=24, microseconds=-1)
timezone._minoffset = -timezone._maxoffset
timezone.utc = timezone._create(timedelta(0))
timezone.min = timezone._create(-timedelta(hours=23, minutes=59))
timezone.max = timezone._create(timedelta(hours=23, minutes=59))
UTC = timezone.utc


class time:
    __slots__ = ("_hour", "_minute", "_second", "_microsecond", "_tzinfo", "_fold", "_hashcode")

    def __new__(cls, hour=0, minute=0, second=0, microsecond=0, tzinfo=None, *, fold=0):
        hour, minute, second, microsecond, fold = _check_time(hour, minute, second, microsecond, fold)
        _check_tzinfo(tzinfo)
        self = object.__new__(cls)
        self._hour = hour
        self._minute = minute
        self._second = second
        self._microsecond = microsecond
        self._tzinfo = tzinfo
        self._fold = fold
        self._hashcode = -1
        return self

    hour = property(lambda self: self._hour)
    minute = property(lambda self: self._minute)
    second = property(lambda self: self._second)
    microsecond = property(lambda self: self._microsecond)
    tzinfo = property(lambda self: self._tzinfo)
    fold = property(lambda self: self._fold)

    @classmethod
    def fromisoformat(cls, s):
        if not isinstance(s, str):
            raise TypeError("fromisoformat: argument must be str")
        try:
            t = s[1:] if s[:1] == "T" else s
            fields, n = _parse_time(t, 0)
            if n != len(t):
                raise ValueError
        except (ValueError, IndexError):
            raise ValueError("Invalid isoformat string: %r" % (s,))
        return cls(*fields)

    @classmethod
    def strptime(cls, date_string, format):
        import _strptime
        d = _strptime._strptime_datetime(datetime, date_string, format)
        return d.timetz()

    def _off(self):
        if self._tzinfo is None:
            return None
        off = self._tzinfo.utcoffset(None)
        _check_offset("utcoffset", off)
        return off

    def utcoffset(self):
        return self._off()

    def tzname(self):
        if self._tzinfo is None:
            return None
        return self._tzinfo.tzname(None)

    def dst(self):
        if self._tzinfo is None:
            return None
        off = self._tzinfo.dst(None)
        _check_offset("dst", off)
        return off

    def _cmp(self, other):
        a = self._off()
        b = other._off()
        if (a is None) != (b is None):
            return None
        x = (self._hour, self._minute, self._second, self._microsecond)
        y = (other._hour, other._minute, other._second, other._microsecond)
        if a is not None and a != b:
            ma = self._hour * 60 + self._minute - a // timedelta(minutes=1)
            mb = other._hour * 60 + other._minute - b // timedelta(minutes=1)
            x = (ma, self._second, self._microsecond)
            y = (mb, other._second, other._microsecond)
        return _cmp(x, y)

    def __eq__(self, other):
        if isinstance(other, time):
            c = self._cmp(other)
            return c == 0 if c is not None else False
        return NotImplemented

    def _order(self, other):
        c = self._cmp(other)
        if c is None:
            raise TypeError("can't compare offset-naive and offset-aware times")
        return c

    def __lt__(self, other):
        if isinstance(other, time):
            return self._order(other) < 0
        return NotImplemented

    def __le__(self, other):
        if isinstance(other, time):
            return self._order(other) <= 0
        return NotImplemented

    def __gt__(self, other):
        if isinstance(other, time):
            return self._order(other) > 0
        return NotImplemented

    def __ge__(self, other):
        if isinstance(other, time):
            return self._order(other) >= 0
        return NotImplemented

    def __hash__(self):
        if self._hashcode == -1:
            off = self._off()
            if not off:
                self._hashcode = hash((self._hour, self._minute, self._second, self._microsecond))
            else:
                h, m = divmod(timedelta(hours=self._hour, minutes=self._minute) - off, timedelta(hours=1))
                self._hashcode = hash((h, m // timedelta(minutes=1), self._second, self._microsecond))
        return self._hashcode

    def isoformat(self, timespec="auto"):
        s = _format_time(self._hour, self._minute, self._second, self._microsecond, timespec)
        return s + _format_offset(self._off())

    __str__ = isoformat

    def __repr__(self):
        if self._microsecond:
            s = ", %d, %d" % (self._second, self._microsecond)
        elif self._second:
            s = ", %d" % self._second
        else:
            s = ""
        s = "%s(%d, %d%s)" % (_name(self), self._hour, self._minute, s)
        if self._tzinfo is not None:
            s = s[:-1] + ", tzinfo=%r)" % self._tzinfo
        if self._fold:
            s = s[:-1] + ", fold=1)"
        return s

    def strftime(self, format):
        tt = (1900, 1, 1, self._hour, self._minute, self._second, 0, 1, -1)
        off = self._off()
        return _time._format(format, tt, self.tzname() if self._tzinfo is not None else None,
                             None if off is None else off.total_seconds(), self._microsecond)

    def __format__(self, fmt):
        if not isinstance(fmt, str):
            raise TypeError("must be str, not %s" % type(fmt).__name__)
        return self.strftime(fmt) if fmt else str(self)

    def replace(self, hour=None, minute=None, second=None, microsecond=None, tzinfo=True, *, fold=None):
        return type(self)(self._hour if hour is None else hour, self._minute if minute is None else minute,
                          self._second if second is None else second,
                          self._microsecond if microsecond is None else microsecond,
                          self._tzinfo if tzinfo is True else tzinfo, fold=self._fold if fold is None else fold)

    __replace__ = replace

    def __bool__(self):
        return True

    def __reduce__(self):
        return (type(self), (self._hour, self._minute, self._second, self._microsecond, self._tzinfo))


time.min = time(0, 0, 0)
time.max = time(23, 59, 59, 999999)
time.resolution = timedelta(microseconds=1)


def _local_fields(t):
    if isinstance(t, bool) or not isinstance(t, (int, float)):
        raise TypeError("'%s' object cannot be interpreted as an integer" % type(t).__name__)
    return _fields(t, _clock.utcoffset(t))


def _fields(t, off):
    if isinstance(t, int):
        secs = t
        us = 0
    else:
        secs = int(t // 1)
        us = _round_half_even((t - secs) * 1000000)
        if us >= 1000000:
            secs += 1
            us -= 1000000
    secs += off
    days, rem = divmod(secs, 86400)
    y, m, d = _time._civil_from_days(days)
    if not MINYEAR <= y <= MAXYEAR:
        raise ValueError("year %d is out of range" % y)
    return y, m, d, rem // 3600, rem // 60 % 60, rem % 60, us


class datetime(date):
    __slots__ = ("_hour", "_minute", "_second", "_microsecond", "_tzinfo", "_fold")

    def __new__(cls, year, month=None, day=None, hour=0, minute=0, second=0, microsecond=0, tzinfo=None,
                *, fold=0):
        year, month, day = _check_date(year, month, day)
        hour, minute, second, microsecond, fold = _check_time(hour, minute, second, microsecond, fold)
        _check_tzinfo(tzinfo)
        self = object.__new__(cls)
        self._year = year
        self._month = month
        self._day = day
        self._hour = hour
        self._minute = minute
        self._second = second
        self._microsecond = microsecond
        self._tzinfo = tzinfo
        self._fold = fold
        self._hashcode = -1
        return self

    hour = property(lambda self: self._hour)
    minute = property(lambda self: self._minute)
    second = property(lambda self: self._second)
    microsecond = property(lambda self: self._microsecond)
    tzinfo = property(lambda self: self._tzinfo)
    fold = property(lambda self: self._fold)

    @classmethod
    def fromtimestamp(cls, t, tz=None):
        _check_tzinfo(tz)
        if tz is None:
            return cls(*_local_fields(t))
        y, m, d, hh, mm, ss, us = _fields(t, 0)
        return tz.fromutc(cls(y, m, d, hh, mm, ss, us, tz))

    @classmethod
    def utcfromtimestamp(cls, t):
        return cls(*_fields(t, 0))

    @classmethod
    def now(cls, tz=None):
        return cls.fromtimestamp(_time.time(), tz)

    @classmethod
    def utcnow(cls):
        return cls.utcfromtimestamp(_time.time())

    @classmethod
    def combine(cls, date_, time_, tzinfo=True):
        if not isinstance(date_, date):
            raise TypeError("combine() argument 1 must be datetime.date, not %s" % type(date_).__name__)
        if not isinstance(time_, time):
            raise TypeError("combine() argument 2 must be datetime.time, not %s" % type(time_).__name__)
        if tzinfo is True:
            tzinfo = time_.tzinfo
        return cls(date_.year, date_.month, date_.day, time_.hour, time_.minute, time_.second,
                   time_.microsecond, tzinfo, fold=time_.fold)

    @classmethod
    def fromisoformat(cls, s):
        if not isinstance(s, str):
            raise TypeError("fromisoformat: argument must be str")
        try:
            ymd, n = _parse_date(s)
            tfields = (0, 0, 0, 0, None)
            if n < len(s):
                n += 1
                tfields, m = _parse_time(s, n)
                if m != len(s):
                    raise ValueError
        except (ValueError, IndexError):
            raise ValueError("Invalid isoformat string: %r" % (s,))
        hh, mi, ss, us, tz = tfields
        extra = 0
        if hh == 24:
            hh = 0
            extra = 1
        r = cls(ymd[0], ymd[1], ymd[2], hh, mi, ss, us, tz)
        return r + timedelta(days=extra) if extra else r

    @classmethod
    def strptime(cls, date_string, format):
        import _strptime
        return _strptime._strptime_datetime(cls, date_string, format)

    def date(self):
        return date(self._year, self._month, self._day)

    def time(self):
        return time(self._hour, self._minute, self._second, self._microsecond, fold=self._fold)

    def timetz(self):
        return time(self._hour, self._minute, self._second, self._microsecond, self._tzinfo, fold=self._fold)

    def _off(self):
        if self._tzinfo is None:
            return None
        off = self._tzinfo.utcoffset(self)
        _check_offset("utcoffset", off)
        return off

    def utcoffset(self):
        return self._off()

    def tzname(self):
        if self._tzinfo is None:
            return None
        name = self._tzinfo.tzname(self)
        if name is not None and not isinstance(name, str):
            raise TypeError("tzinfo.tzname() must return None or a string, not '%s'" % type(name).__name__)
        return name

    def dst(self):
        if self._tzinfo is None:
            return None
        off = self._tzinfo.dst(self)
        _check_offset("dst", off)
        return off

    def _local_us(self):
        return ((self.toordinal() - _EPOCH_ORDINAL) * 86400 + self._hour * 3600 + self._minute * 60
                + self._second) * 1000000 + self._microsecond

    def _naive_epoch(self):
        local = (self.toordinal() - _EPOCH_ORDINAL) * 86400 + self._hour * 3600 + self._minute * 60 + self._second
        off = _clock.utcoffset(local)
        off = _clock.utcoffset(local - off)
        return local - off

    def timestamp(self):
        off = self._off()
        if off is None:
            return self._naive_epoch() + self._microsecond / 1e6
        return (self._local_us() - off._us()) / 10 ** 6

    def timetuple(self):
        dst = self.dst()
        isdst = -1 if dst is None else (1 if dst else 0)
        return _time.struct_time(self._tuple(self._hour, self._minute, self._second)[:8] + (isdst,))

    def utctimetuple(self):
        off = self._off()
        d = self - off if off else self
        return _time.struct_time(d._tuple(d._hour, d._minute, d._second)[:8] + (0,))

    def astimezone(self, tz=None):
        if tz is None:
            ts = self.timestamp()
            off = _clock.utcoffset(ts)
            tz = timezone(timedelta(seconds=off), _time._abbrev(off))
        elif not isinstance(tz, tzinfo):
            raise TypeError("tzinfo argument must be None or of a tzinfo subclass, not type '%s'"
                            % type(tz).__name__)
        mytz = self._tzinfo
        if mytz is None:
            off = timedelta(seconds=(self._local_us() // 1000000) - self._naive_epoch())
            mytz = timezone(off)
            myoff = off
        else:
            if tz is mytz:
                return self
            myoff = self._off()
            if myoff is None:
                return self.replace(tzinfo=None).astimezone(tz)
        utc = (self - myoff).replace(tzinfo=tz)
        return tz.fromutc(utc)

    def ctime(self):
        wd = self.toordinal() % 7 or 7
        return "%s %s %2d %02d:%02d:%02d %04d" % (_time._DAYS[wd - 1][:3], _time._MONTHS[self._month - 1][:3],
                                                  self._day, self._hour, self._minute, self._second, self._year)

    def isoformat(self, sep="T", timespec="auto"):
        return ("%04d-%02d-%02d%s" % (self._year, self._month, self._day, sep)
                + _format_time(self._hour, self._minute, self._second, self._microsecond, timespec)
                + _format_offset(self._off()))

    def __str__(self):
        return self.isoformat(sep=" ")

    def __repr__(self):
        L = [self._year, self._month, self._day, self._hour, self._minute, self._second, self._microsecond]
        if L[-1] == 0:
            del L[-1]
        if L[-1] == 0:
            del L[-1]
        s = "%s(%s)" % (_name(self), ", ".join(map(str, L)))
        if self._tzinfo is not None:
            s = s[:-1] + ", tzinfo=%r)" % self._tzinfo
        if self._fold:
            s = s[:-1] + ", fold=1)"
        return s

    def strftime(self, format):
        off = self._off()
        name = None
        if self._tzinfo is not None:
            name = self.tzname()
            if name is None:
                name = ""
        tt = self._tuple(self._hour, self._minute, self._second)
        return _time._format(format, tt, name, None if off is None else off.days * 86400 + off.seconds
                             + (off.microseconds / 1e6 if off.microseconds else 0), self._microsecond)

    def replace(self, year=None, month=None, day=None, hour=None, minute=None, second=None, microsecond=None,
                tzinfo=True, *, fold=None):
        return type(self)(self._year if year is None else year, self._month if month is None else month,
                          self._day if day is None else day, self._hour if hour is None else hour,
                          self._minute if minute is None else minute,
                          self._second if second is None else second,
                          self._microsecond if microsecond is None else microsecond,
                          self._tzinfo if tzinfo is True else tzinfo, fold=self._fold if fold is None else fold)

    __replace__ = replace

    def _cmp_dt(self, other, allow_mixed):
        mytz = self._tzinfo
        ottz = other._tzinfo
        myoff = otoff = None
        if mytz is ottz:
            base = True
        else:
            myoff = self._off()
            otoff = other._off()
            base = myoff == otoff
        if base:
            return _cmp(self._local_us(), other._local_us())
        if myoff is None or otoff is None:
            if allow_mixed:
                return 2
            raise TypeError("can't compare offset-naive and offset-aware datetimes")
        return _cmp(self._local_us() - myoff._us(), other._local_us() - otoff._us())

    def __eq__(self, other):
        if isinstance(other, datetime):
            return self._cmp_dt(other, True) == 0
        return NotImplemented

    def __lt__(self, other):
        if isinstance(other, datetime):
            return self._cmp_dt(other, False) < 0
        return NotImplemented

    def __le__(self, other):
        if isinstance(other, datetime):
            return self._cmp_dt(other, False) <= 0
        return NotImplemented

    def __gt__(self, other):
        if isinstance(other, datetime):
            return self._cmp_dt(other, False) > 0
        return NotImplemented

    def __ge__(self, other):
        if isinstance(other, datetime):
            return self._cmp_dt(other, False) >= 0
        return NotImplemented

    def __hash__(self):
        if self._hashcode == -1:
            off = self._off()
            us = self._local_us() - (off._us() if off else 0)
            self._hashcode = hash(us)
        return self._hashcode

    def __add__(self, other):
        if not isinstance(other, timedelta):
            return NotImplemented
        total = self._local_us() + other._us()
        days, rem = divmod(total, 86400000000)
        o = days + _EPOCH_ORDINAL
        if not 0 < o <= _MAXORDINAL:
            raise OverflowError("date value out of range")
        y, m, d = _ord2ymd(o)
        s, us = divmod(rem, 1000000)
        return type(self).combine(date(y, m, d), time(s // 3600, s // 60 % 60, s % 60, us, self._tzinfo))

    __radd__ = __add__

    def __sub__(self, other):
        if not isinstance(other, datetime):
            if isinstance(other, timedelta):
                return self + -other
            return NotImplemented
        a = self._local_us()
        b = other._local_us()
        if self._tzinfo is other._tzinfo:
            return timedelta._from_us(a - b)
        myoff = self._off()
        otoff = other._off()
        if myoff == otoff:
            return timedelta._from_us(a - b)
        if myoff is None or otoff is None:
            raise TypeError("can't subtract offset-naive and offset-aware datetimes")
        return timedelta._from_us((a - myoff._us()) - (b - otoff._us()))

    def __reduce__(self):
        return (type(self), (self._year, self._month, self._day, self._hour, self._minute, self._second,
                             self._microsecond, self._tzinfo))


datetime.min = datetime(1, 1, 1)
datetime.max = datetime(9999, 12, 31, 23, 59, 59, 999999)
datetime.resolution = timedelta(microseconds=1)


def _digits(s, i, n):
    part = s[i:i + n]
    if len(part) != n or not part.isdigit() or not part.isascii():
        raise ValueError
    return int(part)


def _parse_date(s):
    """(year, month, day) and the index after the date part of `s`."""
    year = _digits(s, 0, 4)
    if s[4:5] == "-":
        if s[5:6] == "W":
            week = _digits(s, 6, 2)
            n = 8
            day = 1
            if s[8:9] == "-":
                day = _digits(s, 9, 1)
                n = 10
            return _isocal_to_ymd(year, week, day), n
        month = _digits(s, 5, 2)
        if s[7:8] != "-":
            raise ValueError
        return (year, month, _digits(s, 8, 2)), 10
    if s[4:5] == "W":
        week = _digits(s, 5, 2)
        n = 7
        day = 1
        if s[7:8].isdigit():
            day = _digits(s, 7, 1)
            n = 8
        return _isocal_to_ymd(year, week, day), n
    return (year, _digits(s, 4, 2), _digits(s, 6, 2)), 8


def _parse_time(s, i):
    """(hour, minute, second, microsecond, tzinfo) parsed at s[i:]."""
    hh = _digits(s, i, 2)
    i += 2
    mi = ss = us = 0
    colon = s[i:i + 1] == ":"
    if i < len(s) and s[i] not in "+-Z":
        if colon:
            i += 1
        mi = _digits(s, i, 2)
        i += 2
        if i < len(s) and s[i] not in "+-Z":
            if colon:
                if s[i] != ":":
                    raise ValueError
                i += 1
            ss = _digits(s, i, 2)
            i += 2
            if i < len(s) and s[i] in ".,":
                i += 1
                j = i
                while j < len(s) and s[j].isdigit():
                    j += 1
                frac = s[i:j]
                if not frac:
                    raise ValueError
                us = int((frac + "00000")[:6])
                i = j
    tz = None
    if i < len(s):
        c = s[i]
        if c == "Z":
            tz = timezone.utc
            i += 1
        elif c in "+-":
            i += 1
            (oh, om, os_, ous, _), _n = _parse_time(s, i)
            i = len(s)
            off = timedelta(hours=oh, minutes=om, seconds=os_, microseconds=ous)
            if c == "-":
                off = -off
            tz = timezone(off)
        else:
            raise ValueError
    if hh == 24:
        if mi or ss or us:
            raise ValueError
    return (hh, mi, ss, us, tz), i
