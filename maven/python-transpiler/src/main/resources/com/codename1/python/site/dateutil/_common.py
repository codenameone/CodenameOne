# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Helpers shared by the dateutil modules: weekday constants and calendar arithmetic."""

_NAMES = ("MO", "TU", "WE", "TH", "FR", "SA", "SU")


class weekday:
    """A day of the week, optionally with an occurrence count: MO(+1), FR(-1)."""

    def __init__(self, weekday, n=None):
        self.weekday = weekday
        self.n = n

    def __call__(self, n):
        if n == self.n:
            return self
        return self.__class__(self.weekday, n)

    def __eq__(self, other):
        if not isinstance(other, weekday):
            return False
        return self.weekday == other.weekday and self.n == other.n

    def __ne__(self, other):
        return not self.__eq__(other)

    def __hash__(self):
        return hash((self.weekday, self.n))

    def __repr__(self):
        s = _NAMES[self.weekday]
        if not self.n:
            return s
        return "%s(%+d)" % (s, self.n)


MO, TU, WE, TH, FR, SA, SU = [weekday(i) for i in range(7)]

_MONTH_LENGTHS = (31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)


def isleap(year):
    return year % 4 == 0 and (year % 100 != 0 or year % 400 == 0)


def monthrange(year, month):
    """Return the weekday of the first day and the number of days of a month."""
    import datetime
    if month < 1 or month > 12:
        raise ValueError("bad month number " + str(month) + "; must be 1-12")
    days = _MONTH_LENGTHS[month - 1]
    if month == 2 and isleap(year):
        days = 29
    return datetime.date(year, month, 1).weekday(), days
