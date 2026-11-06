import datetime
from datetime import date, datetime as dt, timedelta, timezone

from dateutil import tz
from dateutil.parser import parse, isoparse, ParserError
from dateutil.relativedelta import relativedelta, MO, TU, WE, TH, FR, SA, SU
from dateutil.rrule import (rrule, rruleset, rrulestr, YEARLY, MONTHLY, WEEKLY, DAILY, HOURLY, MINUTELY,
                            SECONDLY)


def show(label, fn):
    try:
        print(label, "->", fn())
    except Exception as e:
        print(label, "raised", type(e).__name__)


print("== relativedelta")
base = dt(2024, 1, 31, 10, 30, 15)
print(base + relativedelta(months=1))
print(base + relativedelta(months=+13, days=-2))
print(base + relativedelta(years=1, month=2, day=29))
print(base - relativedelta(months=2))
print(base + relativedelta(hours=30, minutes=-45))
print(base + relativedelta(weeks=2))
print(base + relativedelta(day=1))
print(base + relativedelta(day=31, months=+1))
print(base + relativedelta(hour=0, minute=0, second=0))
print(date(2024, 3, 15) + relativedelta(days=10))
print(date(2024, 3, 15) + relativedelta(hours=5))
print(base + relativedelta(weekday=MO))
print(base + relativedelta(weekday=MO(+2)))
print(base + relativedelta(weekday=FR(-1)))
print(dt(2024, 3, 15) + relativedelta(weekday=FR))
print(dt(2024, 3, 1) + relativedelta(day=31, weekday=FR(-1)))
print(dt(2024, 1, 1) + relativedelta(yearday=60), dt(2023, 1, 1) + relativedelta(nlyearday=60))
print(dt(2023, 1, 1) + relativedelta(yearday=60))
print(date(2024, 2, 29) + relativedelta(years=1), date(2024, 2, 29) + relativedelta(years=-4))
print(dt(2024, 5, 1) + timedelta(days=3) + relativedelta(months=1))
print(relativedelta(months=1) + dt(2024, 1, 15))
print(relativedelta(dt(2024, 3, 15), dt(2023, 1, 10)))
print(relativedelta(dt(2023, 1, 10), dt(2024, 3, 15)))
print(relativedelta(dt(2024, 3, 31, 12, 0, 1, 500), dt(2024, 2, 29, 1, 2, 3, 4)))
print(relativedelta(date(2024, 12, 25), date(2000, 2, 29)))
r1 = relativedelta(years=1, months=2, days=3, hours=4)
r2 = relativedelta(months=5, days=-10, seconds=3700)
print(r1, r2)
print(r1 + r2, r1 - r2, -r1, abs(relativedelta(days=-3, hours=-2)))
print(r1 * 2, 2 * r1, r1 / 2)
print(relativedelta(months=14, days=40, hours=50, minutes=130, seconds=-4000))
print(relativedelta(day=3, month=4) + relativedelta(years=1), relativedelta(day=3) + relativedelta(day=4))
print(r1 == relativedelta(years=1, months=2, days=3, hours=4), r1 != r2, r1 == 5)
print(bool(relativedelta()), bool(relativedelta(days=1)), bool(relativedelta(day=1)))
print(relativedelta(days=15).weeks, relativedelta(weeks=3).days, relativedelta(days=-10).weeks)
print(relativedelta(days=1.5).normalized(), relativedelta(hours=1.25).normalized())
print(relativedelta(weekday=MO(+1)) == relativedelta(weekday=MO), relativedelta(weekday=MO(2)) == relativedelta(weekday=MO))
print(repr(MO), repr(FR(-1)), repr(SU(+3)), MO == MO(+1), MO(+1) == MO(1))
show("frac months", lambda: relativedelta(months=1.5))
show("bad yday", lambda: relativedelta(yearday=400))
show("weekday0", lambda: MO(0))
show("add int", lambda: relativedelta(days=1) + 5)
print(relativedelta(years=-1, months=-3, days=-4), relativedelta(microseconds=1500000))
print(hash(relativedelta(days=1)) == hash(relativedelta(days=1)))
print(relativedelta(dt(2024, 1, 31), dt(2023, 12, 31)), relativedelta(dt(2024, 2, 29), dt(2023, 2, 28)))
print(dt(2020, 2, 29) + relativedelta(years=1), dt(2020, 2, 29) + relativedelta(years=4))
print(dt(2024, 12, 31) + relativedelta(months=2), dt(2024, 1, 1) - relativedelta(months=1))

print("== parser")
D = dt(2000, 1, 1)
for s in ["2024-03-15", "2024-03-15T10:30:00", "2024-03-15 10:30:00.123456", "2024-03-15T10:30:00Z",
          "2024-03-15T10:30:00+02:00", "2024-03-15T10:30:00-0530", "2024-03-15T10:30", "20240315", "20240315T103000",
          "March 15, 2024", "15 March 2024", "Mar 15 2024 3:45 PM", "3/15/2024", "2024/03/15", "15.03.2024",
          "Fri, 15 Mar 2024 10:30:00 GMT", "10:30", "10:30:45", "3pm", "3 PM", "12 AM", "12:15 pm", "15 Mar",
          "Mar 2024", "2024", "Friday", "Sat 3rd March 2024", "1st January 2025", "2024-03-15 10:30 PM",
          "Dec 25 10:00", "2024-03-15T10:30:00 EST"]:
    try:
        r = parse(s, default=D)
        print(repr(s), "->", r.isoformat(), repr(r.tzinfo))
    except Exception as e:
        print(repr(s), "raised", type(e).__name__)
print(parse("01/02/03", default=D), parse("01/02/03", dayfirst=True, default=D),
      parse("01/02/03", yearfirst=True, default=D), parse("01/02/03", dayfirst=True, yearfirst=True, default=D))
print(parse("13/02/2024", default=D), parse("02/13/2024", default=D), parse("2/3/2024", dayfirst=True, default=D))
print(parse("2024-03-15 10:30 UTC", ignoretz=True, default=D))
print(parse("10:30 +0100", default=D).utcoffset(), parse("10:30 -03:30", default=D).utcoffset())
print(repr(parse("10:30 EST", default=D, tzinfos={"EST": -18000}).tzinfo))
print(repr(parse("10:30 PST", default=D, tzinfos={"PST": tz.tzoffset("PST", -28800)}).tzinfo))
print(repr(parse("10:30 XYZ", default=D, tzinfos=lambda name, offset: 3600).tzinfo))
print(parse("Feb 30 2023 foo", default=D, fuzzy=True) if False else "skip")
print(parse("Today is January 5th, 2021 at 4pm", default=D, fuzzy=True))
print(parse("2024-02-10", default=dt(1999, 12, 31, 5, 6, 7)))
print(parse("10:00", default=dt(1999, 12, 31, 5, 6, 7)))
print(parse("Feb", default=dt(1999, 12, 31)))
for s in ["not a date", "", "2024-13-45", "25:00", "Feb 30 2024"]:
    try:
        parse(s, default=D)
        print(repr(s), "parsed")
    except ValueError as e:
        print(repr(s), "ValueError", isinstance(e, ParserError))
show("bad type", lambda: parse(5))
print(str(ParserError("Unknown string format: %s", "x")))
print(parse("2024-03-15T10:30:00.5", default=D).microsecond, parse("2024-03-15T10:30:00,25", default=D).microsecond)

print("== isoparse")
for s in ["2024", "2024-03", "2024-03-15", "20240315", "2024-W11-5", "2024W115", "2024-W11", "2024-03-15T10",
          "2024-03-15T10:30", "2024-03-15T10:30:45", "2024-03-15T10:30:45.123456", "2024-03-15T10:30:45.1234567",
          "2024-03-15T10:30:45Z", "2024-03-15T10:30:45+05:30", "2024-03-15T10:30:45-0800", "2024-03-15T10:30:45+00:00",
          "2024-03-15T103045", "2024-03-15T24:00", "2024-03-15 10:30", "2024-03-15T10:30:45+05", "20240315T103045Z"]:
    try:
        r = isoparse(s)
        print(repr(s), "->", r.isoformat(), repr(r.tzinfo))
    except Exception as e:
        print(repr(s), "raised", type(e).__name__)
for s in ["2024-3-15", "2024-03-15T25:00", "2024-13-01", "hello", "2024-03-15T10:30:45+25:00", "", "2024-W54"]:
    try:
        isoparse(s)
        print(repr(s), "parsed")
    except ValueError:
        print(repr(s), "ValueError")
show("iso type", lambda: isoparse(5))

print("== tz")
u = tz.tzutc()
print(u, repr(u), u is tz.tzutc(), u is tz.UTC, u.utcoffset(None), u.tzname(None), u.dst(None))
o = tz.tzoffset("IST", 19800)
print(o, repr(o), o.utcoffset(None), o.tzname(None), o is tz.tzoffset("IST", timedelta(seconds=19800)))
print(repr(tz.tzoffset(None, -3600)), tz.tzoffset(None, 0) == tz.tzutc(), tz.tzutc() == tz.tzoffset(None, 0),
      tz.tzoffset(None, 60) == tz.tzoffset("x", 60), tz.tzoffset(None, 60) != tz.tzoffset(None, 61))
d1 = dt(2024, 3, 15, 12, 0, tzinfo=o)
print(d1, d1.isoformat(), d1.utcoffset(), d1.tzname(), d1.astimezone(u), d1.astimezone(tz.tzoffset(None, -18000)))
print(dt(2024, 3, 15, 12, 0, tzinfo=u).astimezone(o), dt(2024, 3, 15, 23, 0, tzinfo=u).astimezone(o).date())
print(d1 == dt(2024, 3, 15, 6, 30, tzinfo=u), d1 - dt(2024, 3, 15, 12, 0, tzinfo=u))
print(dt(2024, 1, 1, tzinfo=u).timestamp(), dt(2024, 1, 1, tzinfo=o).timestamp())
print(tz.gettz("UTC") is not None, tz.gettz("GMT") is not None, repr(tz.gettz("UTC+3")), tz.gettz("UTC+3").utcoffset(None),
      tz.gettz("GMT-05:30").utcoffset(None), tz.gettz("No/Such_Zone"))
print(repr(tz.tzlocal()), isinstance(tz.tzlocal(), datetime.tzinfo))
print(dt(2024, 3, 15, 12, tzinfo=tz.gettz("UTC+3")).isoformat())
print(tz.datetime_exists(dt(2024, 3, 15, 12, tzinfo=o)), tz.datetime_ambiguous(dt(2024, 3, 15, 12, tzinfo=o)))
print(dt(2024, 3, 15, 12, tzinfo=timezone.utc) == dt(2024, 3, 15, 12, tzinfo=u), dt.now(u).tzinfo is u)
print(dt.fromtimestamp(86400, u), dt.fromtimestamp(86400, o), dt(2024, 6, 1, 12, tzinfo=u).replace(tzinfo=o))

print("== rrule")
S = dt(2024, 1, 31, 9, 30)
print(list(rrule(DAILY, dtstart=S, count=4)))
print(list(rrule(DAILY, dtstart=S, interval=3, count=4)))
print(list(rrule(WEEKLY, dtstart=S, count=4)))
print(list(rrule(WEEKLY, dtstart=S, interval=2, count=3, byweekday=(MO, FR))))
print(list(rrule(MONTHLY, dtstart=S, count=5)))
print(list(rrule(MONTHLY, dtstart=S, count=4, bymonthday=(1, 15, -1))))
print(list(rrule(MONTHLY, dtstart=S, count=4, byweekday=FR(+1))))
print(list(rrule(MONTHLY, dtstart=S, count=4, byweekday=(MO(-1), TU(2)))))
print(list(rrule(MONTHLY, dtstart=S, count=6, byweekday=(MO, TU, WE, TH, FR), bysetpos=-1)))
print(list(rrule(MONTHLY, dtstart=S, count=3, byweekday=SA(+2), interval=2)))
print(list(rrule(YEARLY, dtstart=S, count=3)))
print(list(rrule(YEARLY, dtstart=dt(2024, 2, 29), count=3)))
print(list(rrule(YEARLY, dtstart=S, count=4, bymonth=(2, 6), bymonthday=10)))
print(list(rrule(YEARLY, dtstart=S, count=3, bymonth=11, byweekday=TH(+4))))
print(list(rrule(YEARLY, dtstart=S, count=3, interval=5)))
print(list(rrule(YEARLY, dtstart=S, count=3, byyearday=(1, -1))))
print(list(rrule(YEARLY, dtstart=S, count=3, byweekno=20, byweekday=MO)))
print(list(rrule(DAILY, dtstart=S, until=dt(2024, 2, 3, 9, 30))))
print(list(rrule(DAILY, dtstart=S, until=dt(2024, 2, 3, 9, 29))))
print(list(rrule(DAILY, dtstart=S, count=3, byhour=(8, 18), byminute=(0, 30))))
print(list(rrule(DAILY, dtstart=S, count=5, bymonth=2, byweekday=(SA, SU))))
print(list(rrule(HOURLY, dtstart=S, count=4)))
print(list(rrule(HOURLY, dtstart=S, count=5, interval=7)))
print(list(rrule(HOURLY, dtstart=S, count=4, byhour=(12, 13), byminute=(0, 45))))
print(list(rrule(MINUTELY, dtstart=S, count=3, interval=20)))
print(list(rrule(MINUTELY, dtstart=S, count=4, byminute=(0, 15))))
print(list(rrule(SECONDLY, dtstart=S, count=3, interval=45)))
print(list(rrule(SECONDLY, dtstart=S, count=3, bysecond=(0, 30))))
print(list(rrule(MONTHLY, dtstart=S, count=3, bymonthday=31)))
print(list(rrule(WEEKLY, dtstart=S, count=3, wkst=SU, byweekday=(SU, MO))))
print(list(rrule(DAILY, dtstart=S, count=3, byweekday=0)))
print(list(rrule(DAILY, dtstart=date(2024, 5, 1), count=2)))
r = rrule(WEEKLY, dtstart=S, byweekday=(MO, WE, FR))
print(r[0], r[4], r[-1] if False else "", r[1:4], r[::3] if False else "", r[2:8:2])
print(r.after(dt(2024, 2, 10)), r.after(dt(2024, 2, 9, 9, 30)), r.after(dt(2024, 2, 9, 9, 30), inc=True))
print(r.before(dt(2024, 2, 10)), r.before(dt(2024, 2, 9, 9, 30)), r.before(dt(2024, 2, 9, 9, 30), inc=True))
print(r.before(dt(2024, 1, 1)), r.between(dt(2024, 2, 1), dt(2024, 2, 9)),
      r.between(dt(2024, 2, 5, 9, 30), dt(2024, 2, 9, 9, 30), inc=True))
print(dt(2024, 2, 5, 9, 30) in r, dt(2024, 2, 6, 9, 30) in r)
rc = rrule(DAILY, dtstart=S, count=10, bymonth=(1, 2))
print(rc.count(), len(list(rc)), rc.count())
print(rrule(DAILY, dtstart=S, until=dt(2024, 2, 5)).count())
print(str(rrule(WEEKLY, dtstart=S, count=4, byweekday=(MO, FR), interval=2)))
print(str(rrule(MONTHLY, dtstart=S, until=dt(2025, 1, 1), bymonthday=(1, -1), wkst=SU)))
print(str(rrule(YEARLY, dtstart=S, bymonth=(1, 2), byhour=9, bysetpos=1, count=2)))
print(list(rrule(DAILY, dtstart=S, count=3).replace(count=2)))
show("bad setpos", lambda: rrule(DAILY, bysetpos=0))
tzd = dt(2024, 3, 1, 8, 0, tzinfo=tz.tzoffset(None, 3600))
print([x.isoformat() for x in rrule(DAILY, dtstart=tzd, count=2)])
far = rrule(YEARLY, dtstart=dt(9998, 1, 1))
print(list(far))

print("== rruleset")
rs = rruleset()
rs.rrule(rrule(DAILY, dtstart=S, count=6))
rs.exdate(dt(2024, 2, 2, 9, 30))
rs.rdate(dt(2024, 2, 20, 12, 0))
rs.rdate(dt(2024, 2, 1, 9, 30))
rs.exrule(rrule(DAILY, dtstart=S, count=1))
print(list(rs))
print(rs[0], rs[-1] if False else "", rs.after(dt(2024, 2, 3)), rs.before(dt(2024, 2, 10)), rs.count())
rs2 = rruleset()
rs2.rrule(rrule(WEEKLY, dtstart=S, byweekday=MO, count=3))
rs2.rrule(rrule(WEEKLY, dtstart=S, byweekday=(MO, TU), count=3))
rs2.exrule(rrule(WEEKLY, dtstart=S, byweekday=TU, count=1))
print(list(rs2), dt(2024, 2, 5, 9, 30) in rs2)
print(list(rruleset()))

print("== rrulestr")
print(list(rrulestr("FREQ=DAILY;COUNT=3", dtstart=S)))
print(list(rrulestr("RRULE:FREQ=WEEKLY;BYDAY=MO,FR;COUNT=4", dtstart=S)))
print(list(rrulestr("DTSTART:20240101T080000\nRRULE:FREQ=MONTHLY;BYDAY=2TU;COUNT=3")))
print(list(rrulestr("DTSTART:20240101T080000\nRRULE:FREQ=MONTHLY;BYDAY=-1FR;INTERVAL=2;COUNT=3")))
print(list(rrulestr("DTSTART:20240101T080000\nRRULE:FREQ=DAILY;UNTIL=20240104T080000")))
print(list(rrulestr("DTSTART:20240101T080000\nRRULE:FREQ=DAILY;COUNT=5\nEXDATE:20240102T080000,20240104T080000")))
print(type(rrulestr("FREQ=DAILY;COUNT=2", dtstart=S)).__name__,
      type(rrulestr("FREQ=DAILY;COUNT=2", dtstart=S, forceset=True)).__name__)
show("rrulestr bad", lambda: rrulestr("FREQ=DAILY;BOGUS=1", dtstart=S))
