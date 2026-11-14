# Copyright (c) 2001 Python Software Foundation; All Rights Reserved
# Licensed under the Python Software Foundation License Version 2; the full
# text is in maven/python-transpiler/third-party/cpython/LICENSE.txt.
#
# Derived from CPython's Lib/calendar.py. Changes: rewritten for the Codename One
# Python runtime, trimmed to what the transpiler supports and reorganised;
# the C accelerators and platform-specific branches are left out.
"""calendar: leap years, month lengths, weekdays, timegm and month grids,
with English day and month names. The text and HTML calendars are not
provided."""

import datetime as _dt
import enum as _enum


class IllegalMonthError(ValueError):
    def __init__(self, month):
        self.month = month

    def __str__(self):
        return "bad month number %r; must be 1-12" % (self.month,)


class IllegalWeekdayError(ValueError):
    def __init__(self, weekday):
        self.weekday = weekday

    def __str__(self):
        return "bad weekday number %r; must be 0 (Monday) to 6 (Sunday)" % (self.weekday,)


class Day(_enum.IntEnum):
    MONDAY = 0
    TUESDAY = 1
    WEDNESDAY = 2
    THURSDAY = 3
    FRIDAY = 4
    SATURDAY = 5
    SUNDAY = 6


class Month(_enum.IntEnum):
    JANUARY = 1
    FEBRUARY = 2
    MARCH = 3
    APRIL = 4
    MAY = 5
    JUNE = 6
    JULY = 7
    AUGUST = 8
    SEPTEMBER = 9
    OCTOBER = 10
    NOVEMBER = 11
    DECEMBER = 12


MONDAY = Day.MONDAY
TUESDAY = Day.TUESDAY
WEDNESDAY = Day.WEDNESDAY
THURSDAY = Day.THURSDAY
FRIDAY = Day.FRIDAY
SATURDAY = Day.SATURDAY
SUNDAY = Day.SUNDAY
JANUARY = Month.JANUARY
FEBRUARY = Month.FEBRUARY

mdays = [0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31]
day_name = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
day_abbr = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"]
month_name = ["", "January", "February", "March", "April", "May", "June", "July", "August", "September",
              "October", "November", "December"]
month_abbr = ["", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"]

_firstweekday = 0


def isleap(year):
    return year % 4 == 0 and (year % 100 != 0 or year % 400 == 0)


def leapdays(y1, y2):
    y1 -= 1
    y2 -= 1
    return (y2 // 4 - y1 // 4) - (y2 // 100 - y1 // 100) + (y2 // 400 - y1 // 400)


def weekday(year, month, day):
    if not 1 <= year <= 9999:
        year = 2000 + year % 400
    return Day(_dt.date(year, month, day).weekday())


def _monthlen(year, month):
    return mdays[month] + (month == 2 and isleap(year))


def monthrange(year, month):
    if not 1 <= month <= 12:
        raise IllegalMonthError(month)
    return weekday(year, month, 1), _monthlen(year, month)


def timegm(tuple):
    year, month, day, hour, minute, second = tuple[:6]
    days = _dt.date(year, month, 1).toordinal() - _dt.date(1970, 1, 1).toordinal() + day - 1
    return ((days * 24 + hour) * 60 + minute) * 60 + second


def firstweekday():
    return _firstweekday


def setfirstweekday(firstweekday):
    global _firstweekday
    if not MONDAY <= firstweekday <= SUNDAY:
        raise IllegalWeekdayError(firstweekday)
    _firstweekday = firstweekday


class Calendar:
    def __init__(self, firstweekday=0):
        self.firstweekday = firstweekday

    def iterweekdays(self):
        for i in range(self.firstweekday, self.firstweekday + 7):
            yield i % 7

    def itermonthdays3(self, year, month):
        first = _dt.date(year, month, 1)
        before = (first.weekday() - self.firstweekday) % 7
        start = first - _dt.timedelta(days=before)
        n = _monthlen(year, month)
        after = (self.firstweekday - (first.weekday() + n)) % 7
        for i in range(before + n + after):
            d = start + _dt.timedelta(days=i)
            yield d.year, d.month, d.day

    def itermonthdates(self, year, month):
        for y, m, d in self.itermonthdays3(year, month):
            yield _dt.date(y, m, d)

    def itermonthdays(self, year, month):
        for y, m, d in self.itermonthdays3(year, month):
            yield d if m == month else 0

    def itermonthdays2(self, year, month):
        for i, d in enumerate(self.itermonthdays(year, month), self.firstweekday):
            yield d, i % 7

    def monthdatescalendar(self, year, month):
        dates = list(self.itermonthdates(year, month))
        return [dates[i:i + 7] for i in range(0, len(dates), 7)]

    def monthdayscalendar(self, year, month):
        days = list(self.itermonthdays(year, month))
        return [days[i:i + 7] for i in range(0, len(days), 7)]

    def monthdays2calendar(self, year, month):
        days = list(self.itermonthdays2(year, month))
        return [days[i:i + 7] for i in range(0, len(days), 7)]


def monthcalendar(year, month):
    return Calendar(_firstweekday).monthdayscalendar(year, month)
