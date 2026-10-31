# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Recurrence rules (RFC 5545): rrule, rruleset and rrulestr.

Not implemented: byeaster, byweekno with a week start other than Monday, and
the parsing of time zone identifiers inside rrulestr input.
"""

import datetime

from dateutil import tz as _tz
from dateutil._common import weekday, monthrange, isleap, MO, TU, WE, TH, FR, SA, SU

__all__ = ["rrule", "rruleset", "rrulestr", "YEARLY", "MONTHLY", "WEEKLY", "DAILY", "HOURLY", "MINUTELY",
           "SECONDLY", "MO", "TU", "WE", "TH", "FR", "SA", "SU"]

YEARLY, MONTHLY, WEEKLY, DAILY, HOURLY, MINUTELY, SECONDLY = range(7)
_FREQ_NAMES = ("YEARLY", "MONTHLY", "WEEKLY", "DAILY", "HOURLY", "MINUTELY", "SECONDLY")
_ONE_DAY = datetime.timedelta(days=1)


def _as_list(value):
    if value is None:
        return None
    if isinstance(value, (list, tuple, set, frozenset)):
        return list(value)
    return [value]


def _as_weekdays(value):
    out = []
    for w in _as_list(value):
        if isinstance(w, int):
            out.append(weekday(w))
        else:
            out.append(w)
    return out


class rrulebase:
    """Shared behaviour of rrule and rruleset: iteration, indexing, before/after/between."""

    def __iter__(self):
        return self._iter()

    def __getitem__(self, item):
        if isinstance(item, slice):
            out = []
            start = item.start
            stop = item.stop
            step = item.step if item.step is not None else 1
            if (start is not None and start < 0) or (stop is not None and stop < 0) or step < 0:
                return list(self._iter())[item]
            idx = 0
            if start is None:
                start = 0
            for v in self._iter():
                if stop is not None and idx >= stop:
                    break
                if idx >= start and (idx - start) % step == 0:
                    out.append(v)
                idx += 1
            return out
        if item < 0:
            return list(self._iter())[item]
        idx = 0
        for v in self._iter():
            if idx == item:
                return v
            idx += 1
        raise IndexError("list index out of range")

    def __contains__(self, item):
        for v in self._iter():
            if v == item:
                return True
            if v > item:
                break
        return False

    def count(self):
        total = 0
        for _ in self._iter():
            total += 1
        return total

    def before(self, dt, inc=False):
        last = None
        for i in self._iter():
            if i > dt or (not inc and i == dt):
                break
            last = i
        return last

    def after(self, dt, inc=False):
        for i in self._iter():
            if i > dt or (inc and i == dt):
                return i
        return None

    def xafter(self, dt, count=None, inc=False):
        n = 0
        for i in self._iter():
            if i > dt or (inc and i == dt):
                if count is not None and n >= count:
                    return
                n += 1
                yield i

    def between(self, after, before, inc=False, count=1):
        out = []
        for i in self._iter():
            if i > before or (not inc and i == before):
                break
            if i > after or (inc and i == after):
                out.append(i)
        return out


class rrule(rrulebase):
    """A recurrence rule."""

    def __init__(self, freq, dtstart=None, interval=1, wkst=None, count=None, until=None, bysetpos=None,
                 bymonth=None, bymonthday=None, byyearday=None, byeaster=None, byweekno=None, byweekday=None,
                 byhour=None, byminute=None, bysecond=None, cache=False):
        if dtstart is None:
            dtstart = datetime.datetime.now().replace(microsecond=0)
        elif not isinstance(dtstart, datetime.datetime):
            dtstart = datetime.datetime.fromordinal(dtstart.toordinal())
        else:
            dtstart = dtstart.replace(microsecond=0)
        self._dtstart = dtstart
        self._tzinfo = dtstart.tzinfo
        self._freq = freq
        self._interval = interval
        self._count = count
        if until is not None and not isinstance(until, datetime.datetime):
            until = datetime.datetime.fromordinal(until.toordinal())
        if self._count is not None and until is not None:
            until = None if False else until
        self._until = until
        if wkst is None:
            self._wkst = 0
        elif isinstance(wkst, int):
            self._wkst = wkst
        else:
            self._wkst = wkst.weekday
        if interval < 1:
            raise ValueError("interval must be positive")
        self._original_rule = {}
        self._bysetpos = _as_list(bysetpos)
        if bysetpos is not None:
            for p in self._bysetpos:
                if p == 0 or not -366 <= p <= 366:
                    raise ValueError("bysetpos must be between 1 and 366, or between -366 and -1")
        if byeaster is not None:
            raise ValueError("byeaster is not supported")
        if byweekno is None and byyearday is None and bymonthday is None and byweekday is None:
            if freq == YEARLY:
                if bymonth is None:
                    bymonth = dtstart.month
                    self._original_rule["bymonth"] = None
                bymonthday = dtstart.day
                self._original_rule["bymonthday"] = None
            elif freq == MONTHLY:
                bymonthday = dtstart.day
                self._original_rule["bymonthday"] = None
            elif freq == WEEKLY:
                byweekday = dtstart.weekday()
                self._original_rule["byweekday"] = None
        self._bymonth = _as_list(bymonth)
        self._bymonthday = _as_list(bymonthday)
        self._byyearday = _as_list(byyearday)
        self._byweekno = _as_list(byweekno)
        if byweekday is None:
            self._byweekday = None
            self._bynweekday = None
        else:
            plain = []
            nth = []
            for w in _as_weekdays(byweekday):
                if w.n is None or w.n == 0 or freq > MONTHLY:
                    plain.append(w.weekday)
                else:
                    nth.append((w.weekday, w.n))
            self._byweekday = plain if plain else None
            self._bynweekday = nth if nth else None
        self._byhour = None if byhour is None else sorted(set(_as_list(byhour)))
        self._byminute = None if byminute is None else sorted(set(_as_list(byminute)))
        self._bysecond = None if bysecond is None else sorted(set(_as_list(bysecond)))
        for key, val in (("bymonth", self._bymonth), ("bymonthday", self._bymonthday),
                         ("byyearday", self._byyearday), ("byweekno", self._byweekno)):
            if key not in self._original_rule:
                self._original_rule[key] = val
        if "byweekday" not in self._original_rule:
            self._original_rule["byweekday"] = _as_weekdays(byweekday) if byweekday is not None else None
        self._original_rule["bysetpos"] = self._bysetpos
        self._original_rule["byhour"] = self._byhour if byhour is not None else None
        self._original_rule["byminute"] = self._byminute if byminute is not None else None
        self._original_rule["bysecond"] = self._bysecond if bysecond is not None else None
        self._kwargs = dict(freq=freq, dtstart=dtstart, interval=interval, wkst=wkst, count=count, until=until,
                            bysetpos=bysetpos, bymonth=bymonth, bymonthday=bymonthday, byyearday=byyearday,
                            byweekno=byweekno, byweekday=byweekday, byhour=byhour, byminute=byminute,
                            bysecond=bysecond)

    def replace(self, **kwargs):
        new = dict(self._kwargs)
        for k in ("bymonth", "bymonthday", "byweekday"):
            if self._original_rule.get(k, 1) is None and new.get(k) is not None:
                new[k] = None
        new.update(kwargs)
        return rrule(**new)

    # ---- day selection

    def _day_matches(self, d, year_len, month_len, yday):
        if self._bymonth is not None and d.month not in self._bymonth:
            return False
        if self._bymonthday is not None:
            ok = False
            for md in self._bymonthday:
                if md == d.day or (md < 0 and md == d.day - month_len - 1):
                    ok = True
            if not ok:
                return False
        if self._byyearday is not None:
            ok = False
            for yd in self._byyearday:
                if yd == yday or (yd < 0 and yd == yday - year_len - 1):
                    ok = True
            if not ok:
                return False
        if self._byweekno is not None:
            wk = d.isocalendar()[1]
            last = datetime.date(d.isocalendar()[0], 12, 28).isocalendar()[1]
            ok = False
            for w in self._byweekno:
                if w == wk or (w < 0 and w == wk - last - 1):
                    ok = True
            if not ok:
                return False
        if self._byweekday is not None or self._bynweekday is not None:
            wd = d.weekday()
            ok = self._byweekday is not None and wd in self._byweekday
            if not ok and self._bynweekday is not None:
                month_scope = self._freq == MONTHLY or (self._freq == YEARLY and self._bymonth is not None)
                if month_scope:
                    k = d.day - 1
                    total = month_len
                else:
                    k = yday - 1
                    total = year_len
                for w, n in self._bynweekday:
                    if w != wd:
                        continue
                    if n > 0 and k // 7 + 1 == n:
                        ok = True
                    elif n < 0 and (total - 1 - k) // 7 + 1 == -n:
                        ok = True
            if not ok:
                return False
        return True

    def _days_of(self, first, last):
        """Dates from first to last inclusive that pass the date filters."""
        out = []
        d = first
        cur_year = -1
        year_len = 365
        month_len = 30
        cur_month = -1
        while d <= last:
            if d.year != cur_year:
                cur_year = d.year
                year_len = 366 if isleap(cur_year) else 365
            if d.month != cur_month or True:
                month_len = monthrange(d.year, d.month)[1]
            yday = (d - datetime.date(d.year, 1, 1)).days + 1
            if self._day_matches(d, year_len, month_len, yday):
                out.append(d)
            if d == last:
                break
            d = d + _ONE_DAY
        return out

    def _times(self, hour, minute):
        dt = self._dtstart
        freq = self._freq
        if freq >= HOURLY:
            if self._byhour is not None and freq == HOURLY and hour not in self._byhour:
                return []
            if freq >= MINUTELY and self._byminute is not None and freq == MINUTELY and \
                    minute not in self._byminute:
                return []
        out = []
        if freq < HOURLY:
            hours = self._byhour if self._byhour is not None else [dt.hour]
            minutes = self._byminute if self._byminute is not None else [dt.minute]
            seconds = self._bysecond if self._bysecond is not None else [dt.second]
            for h in hours:
                for m in minutes:
                    for s in seconds:
                        out.append((h, m, s))
        elif freq == HOURLY:
            minutes = self._byminute if self._byminute is not None else [dt.minute]
            seconds = self._bysecond if self._bysecond is not None else [dt.second]
            for m in minutes:
                for s in seconds:
                    out.append((hour, m, s))
        elif freq == MINUTELY:
            seconds = self._bysecond if self._bysecond is not None else [dt.second]
            for s in seconds:
                out.append((hour, minute, s))
        return out

    def _iter(self):
        dt = self._dtstart
        freq = self._freq
        interval = self._interval
        count = self._count
        until = self._until
        tzinfo = self._tzinfo
        if until is not None and tzinfo is not None and until.tzinfo is None:
            until = until.replace(tzinfo=_tz.UTC)
        produced = 0
        year = dt.year
        month = dt.month
        cursor = dt
        week_start = dt.date() - datetime.timedelta(days=(dt.weekday() - self._wkst) % 7)
        day = dt.date()
        while True:
            if year > 9999:
                return
            # The period's dates and times.
            if freq == YEARLY:
                days = self._days_of(datetime.date(year, 1, 1), datetime.date(year, 12, 31))
                times = self._times(0, 0)
            elif freq == MONTHLY:
                days = self._days_of(datetime.date(year, month, 1),
                                     datetime.date(year, month, monthrange(year, month)[1]))
                times = self._times(0, 0)
            elif freq == WEEKLY:
                days = self._days_of(week_start, week_start + datetime.timedelta(days=6))
                times = self._times(0, 0)
            elif freq == DAILY:
                days = self._days_of(day, day)
                times = self._times(0, 0)
            else:
                days = self._days_of(cursor.date(), cursor.date())
                if freq == HOURLY:
                    times = self._times(cursor.hour, 0)
                elif freq == MINUTELY:
                    times = self._times(cursor.hour, cursor.minute)
                else:
                    times = []
                    ok = ((self._byhour is None or cursor.hour in self._byhour) and
                          (self._byminute is None or cursor.minute in self._byminute) and
                          (self._bysecond is None or cursor.second in self._bysecond))
                    if ok:
                        times = [(cursor.hour, cursor.minute, cursor.second)]
                if freq == MINUTELY and self._byhour is not None and cursor.hour not in self._byhour:
                    times = []
                if freq == HOURLY and self._byhour is not None and cursor.hour not in self._byhour:
                    times = []
                if freq == SECONDLY and not days:
                    times = []
            cands = []
            for d in days:
                for h, mi, s in times:
                    cands.append(datetime.datetime(d.year, d.month, d.day, h, mi, s, 0, tzinfo))
            cands.sort()
            if self._bysetpos is not None and cands:
                picked = []
                total = len(cands)
                for p in self._bysetpos:
                    idx = p - 1 if p > 0 else total + p
                    if 0 <= idx < total and cands[idx] not in picked:
                        picked.append(cands[idx])
                picked.sort()
                cands = picked
            for c in cands:
                if c < dt:
                    continue
                if until is not None and c > until:
                    return
                produced += 1
                yield c
                if count is not None and produced >= count:
                    return
            # Next period.
            try:
                if freq == YEARLY:
                    year += interval
                elif freq == MONTHLY:
                    total = year * 12 + month - 1 + interval
                    year = total // 12
                    month = total % 12 + 1
                elif freq == WEEKLY:
                    week_start = week_start + datetime.timedelta(days=7 * interval)
                    year = week_start.year
                elif freq == DAILY:
                    day = day + datetime.timedelta(days=interval)
                    year = day.year
                elif freq == HOURLY:
                    cursor = cursor + datetime.timedelta(hours=interval)
                    year = cursor.year
                elif freq == MINUTELY:
                    cursor = cursor + datetime.timedelta(minutes=interval)
                    year = cursor.year
                else:
                    cursor = cursor + datetime.timedelta(seconds=interval)
                    year = cursor.year
            except OverflowError:
                return

    def __str__(self):
        output = []
        h, m, s = [None] * 3
        if self._dtstart:
            output.append(self._dtstart.strftime("DTSTART:%Y%m%dT%H%M%S"))
        parts = ["FREQ=" + _FREQ_NAMES[self._freq]]
        if self._interval != 1:
            parts.append("INTERVAL=" + str(self._interval))
        if self._wkst:
            parts.append("WKST=" + repr(weekday(self._wkst)))
        if self._count is not None:
            parts.append("COUNT=" + str(self._count))
        if self._until:
            parts.append(self._until.strftime("UNTIL=%Y%m%dT%H%M%S"))
        orig = self._original_rule
        names = (("bysetpos", "BYSETPOS"), ("bymonth", "BYMONTH"), ("bymonthday", "BYMONTHDAY"),
                 ("byyearday", "BYYEARDAY"), ("byweekno", "BYWEEKNO"), ("byweekday", "BYDAY"),
                 ("byhour", "BYHOUR"), ("byminute", "BYMINUTE"), ("bysecond", "BYSECOND"))
        for key, label in names:
            value = orig.get(key)
            if value is None:
                continue
            if key == "byweekday":
                parts.append(label + "=" + ",".join(repr(w) for w in value))
            else:
                parts.append(label + "=" + ",".join(str(v) for v in value))
        output.append("RRULE:" + ";".join(parts))
        return "\n".join(output)


def _merge(iterators):
    heads = []
    for it in iterators:
        try:
            heads.append([next(it), it])
        except StopIteration:
            pass
    while heads:
        mi = 0
        for i in range(1, len(heads)):
            if heads[i][0] < heads[mi][0]:
                mi = i
        v = heads[mi][0]
        yield v
        try:
            heads[mi][0] = next(heads[mi][1])
        except StopIteration:
            del heads[mi]


class rruleset(rrulebase):
    """A set of rules and dates, with exclusions."""

    def __init__(self, cache=False):
        self._rrule = []
        self._rdate = []
        self._exrule = []
        self._exdate = []

    def rrule(self, rrule):
        self._rrule.append(rrule)

    def rdate(self, rdate):
        self._rdate.append(rdate)

    def exrule(self, exrule):
        self._exrule.append(exrule)

    def exdate(self, exdate):
        self._exdate.append(exdate)

    def _iter(self):
        inc = [iter(sorted(self._rdate))] + [iter(r) for r in self._rrule]
        exc = [iter(sorted(self._exdate))] + [iter(r) for r in self._exrule]
        ex = _merge(exc)
        exh = next(ex, None)
        last = None
        for d in _merge(inc):
            if last is not None and d == last:
                continue
            last = d
            while exh is not None and exh < d:
                exh = next(ex, None)
            if exh is not None and exh == d:
                continue
            yield d


_WEEKDAY_CODES = {"MO": 0, "TU": 1, "WE": 2, "TH": 3, "FR": 4, "SA": 5, "SU": 6}


def _parse_dt(text, tzinfo=None):
    text = text.strip()
    z = text.endswith("Z")
    if z:
        text = text[:-1]
    if "T" in text:
        d, t = text.split("T")
        t = (t + "000000")[:6]
        v = datetime.datetime(int(d[:4]), int(d[4:6]), int(d[6:8]), int(t[:2]), int(t[2:4]), int(t[4:6]))
    else:
        v = datetime.datetime(int(text[:4]), int(text[4:6]), int(text[6:8]))
    if z:
        v = v.replace(tzinfo=_tz.UTC)
    elif tzinfo is not None:
        v = v.replace(tzinfo=tzinfo)
    return v


def _rule_kwargs(body, tzinfo):
    kw = {}
    for part in body.split(";"):
        if not part:
            continue
        name, value = part.split("=", 1)
        name = name.upper()
        if name == "FREQ":
            kw["freq"] = _FREQ_NAMES.index(value.upper())
        elif name == "INTERVAL":
            kw["interval"] = int(value)
        elif name == "COUNT":
            kw["count"] = int(value)
        elif name == "UNTIL":
            kw["until"] = _parse_dt(value)
        elif name == "WKST":
            kw["wkst"] = _WEEKDAY_CODES[value.upper()]
        elif name == "BYDAY" or name == "BYWEEKDAY":
            days = []
            for item in value.split(","):
                code = item[-2:].upper()
                n = item[:-2]
                days.append(weekday(_WEEKDAY_CODES[code], int(n) if n else None))
            kw["byweekday"] = days
        else:
            key = name.lower()
            if key in ("bymonth", "bymonthday", "byyearday", "byweekno", "byhour", "byminute", "bysecond",
                       "bysetpos"):
                kw[key] = [int(v) for v in value.split(",")]
            else:
                raise ValueError("unknown parameter name: " + name)
    return kw


def rrulestr(s, **kwargs):
    """Parse an RFC 5545 text into an rrule, or an rruleset when it holds more than one rule."""
    dtstart = kwargs.get("dtstart")
    tzinfo = kwargs.get("tzinfo")
    forceset = kwargs.get("forceset", False)
    unfold = kwargs.get("unfold", False)
    lines = [ln.strip() for ln in s.replace("\r", "").split("\n") if ln.strip()]
    if unfold:
        merged = []
        for ln in s.replace("\r", "").split("\n"):
            if ln.startswith(" ") and merged:
                merged[-1] += ln[1:]
            elif ln.strip():
                merged.append(ln.strip())
        lines = merged
    rules = []
    exrules = []
    rdates = []
    exdates = []
    for ln in lines:
        if ":" not in ln:
            rules.append(_rule_kwargs(ln, tzinfo))
            continue
        name, value = ln.split(":", 1)
        name = name.split(";")[0].upper()
        if name == "DTSTART":
            dtstart = _parse_dt(value, tzinfo)
        elif name == "RRULE":
            rules.append(_rule_kwargs(value, tzinfo))
        elif name == "EXRULE":
            exrules.append(_rule_kwargs(value, tzinfo))
        elif name == "RDATE":
            for v in value.split(","):
                rdates.append(_parse_dt(v, tzinfo))
        elif name == "EXDATE":
            for v in value.split(","):
                exdates.append(_parse_dt(v, tzinfo))
        else:
            raise ValueError("unsupported property: " + name)
    if (not forceset and len(rules) == 1 and not exrules and not rdates and not exdates):
        return rrule(dtstart=dtstart, **rules[0])
    rset = rruleset()
    for kw in rules:
        rset.rrule(rrule(dtstart=dtstart, **kw))
    for kw in exrules:
        rset.exrule(rrule(dtstart=dtstart, **kw))
    for d in rdates:
        rset.rdate(d)
    for d in exdates:
        rset.exdate(d)
    return rset
