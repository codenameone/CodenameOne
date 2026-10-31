# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Time access and conversions.

The clocks are the device's; the local zone is the device's default
zone, asked for its offset at each instant (daylight saving included).
"""

import _time

time = _time.time
time_ns = _time.time_ns
perf_counter = _time.perf_counter
perf_counter_ns = _time.perf_counter_ns
monotonic = _time.monotonic
sleep = _time.sleep


def monotonic_ns():
    return perf_counter_ns()


def process_time():
    return perf_counter()


def process_time_ns():
    return perf_counter_ns()


def thread_time():
    return perf_counter()


_DAYS = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
_MONTHS = ["January", "February", "March", "April", "May", "June", "July", "August", "September",
           "October", "November", "December"]


class struct_time(tuple):
    n_sequence_fields = 9
    n_fields = 11
    n_unnamed_fields = 0

    def __new__(cls, seq, zone=None, gmtoff=None):
        seq = tuple(seq)
        if len(seq) < 9:
            raise TypeError("time.struct_time() takes an at least 9-sequence (%d-sequence given)" % len(seq))
        if len(seq) > 11:
            raise TypeError("time.struct_time() takes an at most 11-sequence (%d-sequence given)" % len(seq))
        self = tuple.__new__(cls, seq[:9])
        self._zone = seq[9] if len(seq) > 9 else zone
        self._gmtoff = seq[10] if len(seq) > 10 else gmtoff
        return self

    tm_year = property(lambda self: self[0])
    tm_mon = property(lambda self: self[1])
    tm_mday = property(lambda self: self[2])
    tm_hour = property(lambda self: self[3])
    tm_min = property(lambda self: self[4])
    tm_sec = property(lambda self: self[5])
    tm_wday = property(lambda self: self[6])
    tm_yday = property(lambda self: self[7])
    tm_isdst = property(lambda self: self[8])
    tm_zone = property(lambda self: self._zone)
    tm_gmtoff = property(lambda self: self._gmtoff)

    def __repr__(self):
        return ("time.struct_time(tm_year=%d, tm_mon=%d, tm_mday=%d, tm_hour=%d, tm_min=%d, "
                "tm_sec=%d, tm_wday=%d, tm_yday=%d, tm_isdst=%d)" % tuple(self))

    def __reduce__(self):
        return (struct_time, (tuple(self),))


def _days_from_civil(y, m, d):
    y -= m <= 2
    era = (y if y >= 0 else y - 399) // 400
    yoe = y - era * 400
    doy = (153 * (m + (-3 if m > 2 else 9)) + 2) // 5 + d - 1
    doe = yoe * 365 + yoe // 4 - yoe // 100 + doy
    return era * 146097 + doe - 719468


def _civil_from_days(z):
    z += 719468
    era = (z if z >= 0 else z - 146096) // 146097
    doe = z - era * 146097
    yoe = (doe - doe // 1460 + doe // 36524 - doe // 146096) // 365
    y = yoe + era * 400
    doy = doe - (365 * yoe + yoe // 4 - yoe // 100)
    mp = (5 * doy + 2) // 153
    d = doy - (153 * mp + 2) // 5 + 1
    m = mp + 3 if mp < 10 else mp - 9
    return y + (m <= 2), m, d


def _leap(y):
    return y % 4 == 0 and (y % 100 != 0 or y % 400 == 0)


def _yday(y, m, d):
    return _days_from_civil(y, m, d) - _days_from_civil(y, 1, 1) + 1


def _tm(secs, off, zone, isdst):
    secs = int(secs // 1) + off
    days = secs // 86400
    rem = secs - days * 86400
    y, m, d = _civil_from_days(days)
    return struct_time((y, m, d, rem // 3600, rem // 60 % 60, rem % 60, (days + 3) % 7, _yday(y, m, d), isdst),
                       zone, off)


def _secs(secs):
    if secs is None:
        return time()
    if not isinstance(secs, (int, float)):
        raise TypeError("'%s' object cannot be interpreted as an integer" % type(secs).__name__)
    return secs


def gmtime(secs=None):
    return _tm(_secs(secs), 0, "UTC", 0)


def localtime(secs=None):
    secs = _secs(secs)
    off = _time.utcoffset(secs)
    return _tm(secs, off, _zone_name(off), 1 if off != timezone * -1 else 0)


def _zone_name(off):
    return tzname[0] if off == -timezone else tzname[1]


def mktime(t):
    if len(t) != 9 and not isinstance(t, struct_time):
        raise TypeError("mktime(): illegal time tuple argument")
    y, mo, d, h, mi, s = t[0], t[1], t[2], t[3], t[4], t[5]
    local = _days_from_civil(y, 1, 1) + _yday_norm(y, mo, d) - 1
    secs = local * 86400 + h * 3600 + mi * 60 + s
    off = _time.utcoffset(secs)
    off = _time.utcoffset(secs - off)
    return float(secs - off)


def _yday_norm(y, mo, d):
    y += (mo - 1) // 12
    mo = (mo - 1) % 12 + 1
    return _days_from_civil(y, mo, 1) - _days_from_civil(y, 1, 1) + d


def _now_offset():
    return _time.utcoffset(time())


def _jan_offset():
    y = gmtime()[0]
    return _time.utcoffset(_days_from_civil(y, 1, 1) * 86400.0)


def _jul_offset():
    y = gmtime()[0]
    return _time.utcoffset(_days_from_civil(y, 7, 1) * 86400.0)


_jan = _jan_offset()
_jul = _jul_offset()
timezone = -min(_jan, _jul)
altzone = -max(_jan, _jul)
daylight = 1 if _jan != _jul else 0


def _abbrev(off):
    if off == 0:
        return "UTC"
    sign = "+" if off >= 0 else "-"
    off = abs(off)
    h, m = off // 3600, off // 60 % 60
    return "%s%02d%s" % (sign, h, "%02d" % m if m else "")


tzname = (_abbrev(-timezone), _abbrev(-altzone))


def asctime(t=None):
    if t is None:
        t = localtime()
    return "%s %s %2d %02d:%02d:%02d %d" % (_DAYS[t[6]][:3], _MONTHS[t[1] - 1][:3], t[2], t[3], t[4], t[5], t[0])


def ctime(secs=None):
    return asctime(localtime(secs))


def strftime(format, t=None):
    if t is None:
        t = localtime()
    return _format(format, t, None, None, 0)


def _format(fmt, t, zone, offset, micro):
    """`fmt` rendered for time tuple `t`; `offset` is seconds east of UTC
    or None, and `zone` a name or None."""
    if zone is None and isinstance(t, struct_time):
        zone = t.tm_zone
        if offset is None:
            offset = t.tm_gmtoff
    out = []
    i = 0
    n = len(fmt)
    while i < n:
        c = fmt[i]
        if c != "%" or i + 1 >= n:
            out.append(c)
            i += 1
            continue
        k = fmt[i + 1]
        i += 2
        y, mo, d, h, mi, s, wd, yd = t[0], t[1], t[2], t[3], t[4], t[5], t[6], t[7]
        if k == "Y":
            out.append(str(y) if y >= 1000 else "%04d" % y)
        elif k == "y":
            out.append("%02d" % (y % 100))
        elif k == "C":
            out.append("%02d" % (y // 100))
        elif k == "m":
            out.append("%02d" % mo)
        elif k == "d":
            out.append("%02d" % d)
        elif k == "e":
            out.append("%2d" % d)
        elif k == "H":
            out.append("%02d" % h)
        elif k == "I":
            out.append("%02d" % ((h + 11) % 12 + 1))
        elif k == "M":
            out.append("%02d" % mi)
        elif k == "S":
            out.append("%02d" % s)
        elif k == "f":
            out.append("%06d" % micro)
        elif k == "p":
            out.append("AM" if h < 12 else "PM")
        elif k == "a":
            out.append(_DAYS[wd][:3])
        elif k == "A":
            out.append(_DAYS[wd])
        elif k == "b" or k == "h":
            out.append(_MONTHS[mo - 1][:3])
        elif k == "B":
            out.append(_MONTHS[mo - 1])
        elif k == "w":
            out.append(str((wd + 1) % 7))
        elif k == "u":
            out.append(str(wd + 1))
        elif k == "j":
            out.append("%03d" % yd)
        elif k == "U":
            out.append("%02d" % ((yd + 6 - (wd + 1) % 7) // 7))
        elif k == "W":
            out.append("%02d" % ((yd + 6 - wd) // 7))
        elif k in "GVg":
            iy, iw, _ = _iso(y, mo, d)
            out.append(str(iy) if k == "G" else ("%02d" % iw if k == "V" else "%02d" % (iy % 100)))
        elif k == "c":
            out.append(_format("%a %b %e %H:%M:%S %Y", t, zone, offset, micro))
        elif k == "x":
            out.append(_format("%m/%d/%y", t, zone, offset, micro))
        elif k == "X":
            out.append(_format("%H:%M:%S", t, zone, offset, micro))
        elif k == "D":
            out.append(_format("%m/%d/%y", t, zone, offset, micro))
        elif k == "F":
            out.append(_format("%Y-%m-%d", t, zone, offset, micro))
        elif k == "T":
            out.append(_format("%H:%M:%S", t, zone, offset, micro))
        elif k == "R":
            out.append(_format("%H:%M", t, zone, offset, micro))
        elif k == "r":
            out.append(_format("%I:%M:%S %p", t, zone, offset, micro))
        elif k == "n":
            out.append("\n")
        elif k == "t":
            out.append("\t")
        elif k == "z":
            if offset is not None:
                sign = "+" if offset >= 0 else "-"
                o = abs(offset)
                hh, rest = divmod(o, 3600)
                mm, ss = divmod(rest, 60)
                part = "%s%02d%02d" % (sign, hh, mm)
                if ss:
                    part += "%02d" % int(ss)
                    us = int(round((ss - int(ss)) * 1000000)) if isinstance(ss, float) else 0
                    if us:
                        part += ".%06d" % us
                out.append(part)
        elif k == "Z":
            if zone is not None:
                out.append(zone)
        elif k == "%":
            out.append("%")
        else:
            out.append("%" + k)
    return "".join(out)


def _iso(y, m, d):
    days = _days_from_civil(y, m, d)
    wd = (days + 3) % 7
    thursday = days - wd + 3
    iy = _civil_from_days(thursday)[0]
    week1 = _days_from_civil(iy, 1, 4)
    week1 -= (week1 + 3) % 7
    return iy, (days - week1) // 7 + 1, wd + 1


def strptime(string, format="%a %b %d %H:%M:%S %Y"):
    import _strptime
    return _strptime._strptime_time(string, format)


def get_clock_info(name):
    raise ValueError("unknown clock")
