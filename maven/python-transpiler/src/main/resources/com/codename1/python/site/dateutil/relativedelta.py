# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""relativedelta: calendar-aware differences and offsets between dates."""

import datetime

from dateutil._common import weekday, isleap, monthrange, MO, TU, WE, TH, FR, SA, SU

__all__ = ["relativedelta", "MO", "TU", "WE", "TH", "FR", "SA", "SU"]


def _sign(x):
    return -1 if x < 0 else 1


class relativedelta:
    """A difference expressed in calendar units, with optional absolute replacements."""

    def __init__(self, dt1=None, dt2=None, years=0, months=0, days=0, leapdays=0, weeks=0, hours=0, minutes=0,
                 seconds=0, microseconds=0, year=None, month=None, day=None, weekday=None, yearday=None,
                 nlyearday=None, hour=None, minute=None, second=None, microsecond=None):
        if dt1 and dt2:
            if not isinstance(dt1, datetime.date) or not isinstance(dt2, datetime.date):
                raise TypeError("relativedelta only diffs datetime/date")
            if not isinstance(dt1, datetime.datetime):
                dt1 = datetime.datetime.fromordinal(dt1.toordinal())
            if not isinstance(dt2, datetime.datetime):
                dt2 = datetime.datetime.fromordinal(dt2.toordinal())
            self.years = 0
            self.months = 0
            self.days = 0
            self.leapdays = 0
            self.hours = 0
            self.minutes = 0
            self.seconds = 0
            self.microseconds = 0
            self.year = None
            self.month = None
            self.day = None
            self.weekday = None
            self.hour = None
            self.minute = None
            self.second = None
            self.microsecond = None
            self._has_time = 0
            months = (dt1.year - dt2.year) * 12 + (dt1.month - dt2.month)
            self._set_months(months)
            dtm = self.__radd__(dt2)
            if dt1 < dt2:
                increment = 1
                while dt1 > dtm:
                    months += increment
                    self._set_months(months)
                    dtm = self.__radd__(dt2)
            else:
                increment = -1
                while dt1 < dtm:
                    months += increment
                    self._set_months(months)
                    dtm = self.__radd__(dt2)
            delta = dt1 - dtm
            self.seconds = delta.seconds + delta.days * 86400
            self.microseconds = delta.microseconds
        else:
            if isinstance(dt1, datetime.timedelta) or isinstance(dt2, datetime.timedelta):
                raise TypeError("relativedelta does not take timedelta positionally")
            if any(x is not None and x != int(x) for x in (years, months)):
                raise ValueError("Non-integer years and months are ambiguous and not currently supported.")
            self.years = int(years)
            self.months = int(months)
            self.days = days + weeks * 7
            self.leapdays = leapdays
            self.hours = hours
            self.minutes = minutes
            self.seconds = seconds
            self.microseconds = microseconds
            self.year = year
            self.month = month
            self.day = day
            self.hour = hour
            self.minute = minute
            self.second = second
            self.microsecond = microsecond
            if isinstance(weekday, int):
                weekday = _weekday_class(weekday)
            self.weekday = weekday
            yday = 0
            if nlyearday:
                yday = nlyearday
            elif yearday:
                yday = yearday
                if yearday > 59:
                    self.leapdays = -1
            if yday:
                ydayidx = [31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334, 366]
                found = False
                for idx in range(12):
                    if yday <= ydayidx[idx]:
                        self.month = idx + 1
                        if idx == 0:
                            self.day = yday
                        else:
                            self.day = yday - ydayidx[idx - 1]
                        found = True
                        break
                if not found:
                    raise ValueError("invalid year day (%d)" % yday)
        self._fix()

    def _fix(self):
        if abs(self.microseconds) > 999999:
            s = _sign(self.microseconds)
            div, mod = divmod(self.microseconds * s, 1000000)
            self.microseconds = mod * s
            self.seconds += div * s
        if abs(self.seconds) > 59:
            s = _sign(self.seconds)
            div, mod = divmod(self.seconds * s, 60)
            self.seconds = mod * s
            self.minutes += div * s
        if abs(self.minutes) > 59:
            s = _sign(self.minutes)
            div, mod = divmod(self.minutes * s, 60)
            self.minutes = mod * s
            self.hours += div * s
        if abs(self.hours) > 23:
            s = _sign(self.hours)
            div, mod = divmod(self.hours * s, 24)
            self.hours = mod * s
            self.days += div * s
        if abs(self.months) > 11:
            s = _sign(self.months)
            div, mod = divmod(self.months * s, 12)
            self.months = mod * s
            self.years += div * s
        if (self.hours or self.minutes or self.seconds or self.microseconds or self.hour is not None or
                self.minute is not None or self.second is not None or self.microsecond is not None):
            self._has_time = 1
        else:
            self._has_time = 0

    @property
    def weeks(self):
        return int(self.days / 7.0)

    @weeks.setter
    def weeks(self, value):
        self.days = self.days - (self.weeks * 7) + value * 7

    def _set_months(self, months):
        self.months = months
        if abs(self.months) > 11:
            s = _sign(self.months)
            div, mod = divmod(self.months * s, 12)
            self.months = mod * s
            self.years = div * s
        else:
            self.years = 0

    def normalized(self):
        days = int(self.days)
        hours_f = round(self.hours + 24 * (self.days - days), 11)
        hours = int(hours_f)
        minutes_f = round(self.minutes + 60 * (hours_f - hours), 10)
        minutes = int(minutes_f)
        seconds_f = round(self.seconds + 60 * (minutes_f - minutes), 8)
        seconds = int(seconds_f)
        microseconds = round(self.microseconds + 1e6 * (seconds_f - seconds))
        return self.__class__(years=self.years, months=self.months, days=days, hours=hours, minutes=minutes,
                              seconds=seconds, microseconds=microseconds, leapdays=self.leapdays, year=self.year,
                              month=self.month, day=self.day, weekday=self.weekday, hour=self.hour,
                              minute=self.minute, second=self.second, microsecond=self.microsecond)

    def __add__(self, other):
        if isinstance(other, relativedelta):
            return self.__class__(years=other.years + self.years, months=other.months + self.months,
                                  days=other.days + self.days, hours=other.hours + self.hours,
                                  minutes=other.minutes + self.minutes, seconds=other.seconds + self.seconds,
                                  microseconds=other.microseconds + self.microseconds,
                                  leapdays=other.leapdays or self.leapdays,
                                  year=(other.year if other.year is not None else self.year),
                                  month=(other.month if other.month is not None else self.month),
                                  day=(other.day if other.day is not None else self.day),
                                  weekday=(other.weekday if other.weekday is not None else self.weekday),
                                  hour=(other.hour if other.hour is not None else self.hour),
                                  minute=(other.minute if other.minute is not None else self.minute),
                                  second=(other.second if other.second is not None else self.second),
                                  microsecond=(other.microsecond if other.microsecond is not None
                                               else self.microsecond))
        if isinstance(other, datetime.timedelta):
            return self.__class__(years=self.years, months=self.months, days=self.days + other.days,
                                  hours=self.hours, minutes=self.minutes, seconds=self.seconds + other.seconds,
                                  microseconds=self.microseconds + other.microseconds, leapdays=self.leapdays,
                                  year=self.year, month=self.month, day=self.day, weekday=self.weekday,
                                  hour=self.hour, minute=self.minute, second=self.second,
                                  microsecond=self.microsecond)
        if not isinstance(other, datetime.date):
            return NotImplemented
        if self._has_time and not isinstance(other, datetime.datetime):
            other = datetime.datetime.fromordinal(other.toordinal())
        year = (self.year or other.year) + self.years
        month = self.month or other.month
        if self.months:
            month += self.months
            if month > 12:
                year += 1
                month -= 12
            elif month < 1:
                year -= 1
                month += 12
        day = min(monthrange(year, month)[1], self.day or other.day)
        repl = {"year": year, "month": month, "day": day}
        for attr in ("hour", "minute", "second", "microsecond"):
            value = getattr(self, attr)
            if value is not None:
                repl[attr] = value
        days = self.days
        if self.leapdays and month > 2 and isleap(year):
            days += self.leapdays
        ret = other.replace(**repl) + datetime.timedelta(days=days, hours=self.hours, minutes=self.minutes,
                                                          seconds=self.seconds, microseconds=self.microseconds)
        if self.weekday:
            wd = self.weekday.weekday
            nth = self.weekday.n or 1
            jumpdays = (abs(nth) - 1) * 7
            if nth > 0:
                jumpdays += (7 - ret.weekday() + wd) % 7
            else:
                jumpdays += (ret.weekday() - wd) % 7
                jumpdays *= -1
            ret += datetime.timedelta(days=jumpdays)
        return ret

    def __radd__(self, other):
        return self.__add__(other)

    def __rsub__(self, other):
        return self.__neg__().__radd__(other)

    def __sub__(self, other):
        if not isinstance(other, relativedelta):
            return NotImplemented
        return self.__class__(years=self.years - other.years, months=self.months - other.months,
                              days=self.days - other.days, hours=self.hours - other.hours,
                              minutes=self.minutes - other.minutes, seconds=self.seconds - other.seconds,
                              microseconds=self.microseconds - other.microseconds,
                              leapdays=self.leapdays or other.leapdays,
                              year=(self.year if self.year is not None else other.year),
                              month=(self.month if self.month is not None else other.month),
                              day=(self.day if self.day is not None else other.day),
                              weekday=(self.weekday if self.weekday is not None else other.weekday),
                              hour=(self.hour if self.hour is not None else other.hour),
                              minute=(self.minute if self.minute is not None else other.minute),
                              second=(self.second if self.second is not None else other.second),
                              microsecond=(self.microsecond if self.microsecond is not None
                                           else other.microsecond))

    def __abs__(self):
        return self.__class__(years=abs(self.years), months=abs(self.months), days=abs(self.days),
                              hours=abs(self.hours), minutes=abs(self.minutes), seconds=abs(self.seconds),
                              microseconds=abs(self.microseconds), leapdays=self.leapdays, year=self.year,
                              month=self.month, day=self.day, weekday=self.weekday, hour=self.hour,
                              minute=self.minute, second=self.second, microsecond=self.microsecond)

    def __neg__(self):
        return self.__class__(years=-self.years, months=-self.months, days=-self.days, hours=-self.hours,
                              minutes=-self.minutes, seconds=-self.seconds, microseconds=-self.microseconds,
                              leapdays=self.leapdays, year=self.year, month=self.month, day=self.day,
                              weekday=self.weekday, hour=self.hour, minute=self.minute, second=self.second,
                              microsecond=self.microsecond)

    def __bool__(self):
        return not (not self.years and not self.months and not self.days and not self.hours and
                    not self.minutes and not self.seconds and not self.microseconds and not self.leapdays and
                    self.year is None and self.month is None and self.day is None and self.weekday is None and
                    self.hour is None and self.minute is None and self.second is None and
                    self.microsecond is None)

    def __mul__(self, other):
        try:
            f = float(other)
        except (TypeError, ValueError):
            return NotImplemented
        return self.__class__(years=int(self.years * f), months=int(self.months * f), days=int(self.days * f),
                              hours=int(self.hours * f), minutes=int(self.minutes * f),
                              seconds=int(self.seconds * f), microseconds=int(self.microseconds * f),
                              leapdays=self.leapdays, year=self.year, month=self.month, day=self.day,
                              weekday=self.weekday, hour=self.hour, minute=self.minute, second=self.second,
                              microsecond=self.microsecond)

    def __rmul__(self, other):
        return self.__mul__(other)

    def __truediv__(self, other):
        try:
            return self.__mul__(1 / float(other))
        except TypeError:
            return NotImplemented

    def __eq__(self, other):
        if not isinstance(other, relativedelta):
            return NotImplemented
        if self.weekday or other.weekday:
            if not self.weekday or not other.weekday:
                return False
            if self.weekday.weekday != other.weekday.weekday:
                return False
            n1 = self.weekday.n
            n2 = other.weekday.n
            if n1 != n2 and not ((not n1 or n1 == 1) and (not n2 or n2 == 1)):
                return False
        return (self.years == other.years and self.months == other.months and self.days == other.days and
                self.hours == other.hours and self.minutes == other.minutes and
                self.seconds == other.seconds and self.microseconds == other.microseconds and
                self.leapdays == other.leapdays and self.year == other.year and self.month == other.month and
                self.day == other.day and self.hour == other.hour and self.minute == other.minute and
                self.second == other.second and self.microsecond == other.microsecond)

    def __ne__(self, other):
        r = self.__eq__(other)
        if r is NotImplemented:
            return r
        return not r

    def __hash__(self):
        return hash((self.weekday, self.years, self.months, self.days, self.hours, self.minutes, self.seconds,
                     self.microseconds, self.leapdays, self.year, self.month, self.day, self.hour, self.minute,
                     self.second, self.microsecond))

    def __repr__(self):
        parts = []
        for attr in ("years", "months", "days", "leapdays", "hours", "minutes", "seconds", "microseconds"):
            value = getattr(self, attr)
            if value:
                parts.append("%s=%+g" % (attr, value))
        for attr in ("year", "month", "day", "weekday", "hour", "minute", "second", "microsecond"):
            value = getattr(self, attr)
            if value is not None:
                parts.append("%s=%s" % (attr, repr(value)))
        return "%s(%s)" % (self.__class__.__name__, ", ".join(parts))


def _weekday_class(n):
    return weekday(n)
