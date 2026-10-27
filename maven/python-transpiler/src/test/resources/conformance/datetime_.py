# datetime, time and _strptime, kept independent of the local zone.
import datetime as dt
from datetime import date, datetime, time, timedelta, timezone
import time as tm

td = timedelta(days=1, hours=2, minutes=3, seconds=4.5, milliseconds=6, microseconds=7)
print(td, repr(td), td.days, td.seconds, td.microseconds, td.total_seconds())
print(timedelta(), repr(timedelta()), -timedelta(seconds=1), repr(-timedelta(seconds=1)), timedelta(weeks=2))
print(timedelta(hours=-25), timedelta(days=3) / 2, timedelta(days=3) / timedelta(hours=5), timedelta(days=3) // 5)
print(timedelta(seconds=10) * 2.5, 3 * timedelta(minutes=1), timedelta(seconds=7) % timedelta(seconds=3),
      divmod(timedelta(seconds=7), timedelta(seconds=3)), abs(timedelta(-1)))
print(timedelta(microseconds=0.5), timedelta(microseconds=1.5), timedelta(seconds=1 / 3))
print(timedelta(1) > timedelta(0), timedelta(hours=24) == timedelta(1), bool(timedelta(0)), timedelta.max, timedelta.min)
try:
    timedelta(days=10**9)
except OverflowError as e:
    print(e)

d = date(2024, 2, 29)
print(d, repr(d), d.year, d.month, d.day, d.weekday(), d.isoweekday(), d.toordinal(), d.isocalendar())
print(d.isocalendar().week, tuple(d.isocalendar()), d + timedelta(days=1), d - timedelta(days=60), d - date(2023, 1, 1))
print(date.fromordinal(1), date.fromordinal(738000), date.fromisoformat("2021-12-31"), date.fromisoformat("20211231"),
      date.fromisoformat("2021-W01-1"), date.fromisocalendar(2020, 53, 7))
print(d.replace(day=1), d.ctime(), d.strftime("%A %d %B %Y, day %j, week %U/%W, %a %b %y %m"), format(d, "%Y"), f"{d}")
print(d.timetuple(), date.min, date.max, date(2020, 1, 1) < date(2020, 1, 2), date(2020, 1, 1) == date(2020, 1, 1))
for args in [(2023, 2, 29), (0, 1, 1), (2020, 13, 1)]:
    try:
        date(*args)
    except ValueError as e:
        print(e)
try:
    date.fromisoformat("2021-1-1")
except ValueError as e:
    print(e)

t = time(13, 5, 9, 1500)
print(t, repr(t), repr(time(1, 2)), repr(time(1, 2, 3)), t.isoformat("milliseconds"), t.isoformat("minutes"),
      t.strftime("%I:%M:%S %p %f"), time.fromisoformat("10:11:12.5"), time.fromisoformat("T101112"))
tz = timezone(timedelta(hours=5, minutes=30))
print(time(1, tzinfo=tz), repr(time(1, tzinfo=tz)), time(1, tzinfo=tz).utcoffset(), time(1, tzinfo=tz).tzname())
print(time(1, tzinfo=timezone.utc) == time(6, 30, tzinfo=tz), time(0) < time(1), time.min, time.max)
try:
    time(1) < time(1, tzinfo=tz)
except TypeError as e:
    print(e)

print(tz, repr(tz), repr(timezone.utc), timezone.utc, repr(timezone(timedelta(hours=-3), "BRT")),
      timezone(timedelta(0)) is timezone.utc, timezone(timedelta(minutes=-90)), dt.UTC is timezone.utc)
try:
    timezone(timedelta(hours=24))
except ValueError as e:
    print(e)

x = datetime(2024, 3, 10, 14, 30, 15, 250000)
print(x, repr(x), x.date(), x.time(), x.isoformat(), x.isoformat(" ", "seconds"), x.ctime(), x.weekday())
print(repr(datetime(2020, 1, 1)), repr(datetime(2020, 1, 1, 5)), repr(datetime(2020, 1, 1, 0, 0, 7)))
a = datetime(2024, 1, 1, 12, tzinfo=timezone.utc)
b = datetime(2024, 1, 1, 17, 30, tzinfo=tz)
print(a, repr(a), a == b, a - b, b.astimezone(timezone.utc), a.astimezone(tz), a.timestamp(), b.utcoffset())
print(datetime.fromtimestamp(0, timezone.utc), datetime.fromtimestamp(1700000000.5, tz),
      datetime.fromtimestamp(86400 * 365.25, timezone.utc).isoformat())
print(datetime(2024, 1, 31) + timedelta(days=1), datetime(2024, 1, 1) - datetime(2023, 12, 25, 6),
      datetime(2024, 1, 1) - timedelta(seconds=1))
print(datetime.fromisoformat("2024-05-06T07:08:09.123+02:00"), datetime.fromisoformat("2024-05-06 07:08"),
      datetime.fromisoformat("2024-05-06"), datetime.fromisoformat("20240506T070809Z"),
      datetime.fromisoformat("2024-05-06T24:00"))
print(datetime.combine(date(2020, 5, 5), time(8, 9, tzinfo=tz)), datetime(2020, 5, 5, tzinfo=tz).strftime("%z %Z %H"),
      datetime(2020, 5, 5).strftime("[%z][%Z]"))
print(datetime.strptime("2023-07-04 09:15:30", "%Y-%m-%d %H:%M:%S"), datetime.strptime("4 Jul 23 3PM", "%d %b %y %I%p"),
      datetime.strptime("2023-07-04T09:15:30.5+0130", "%Y-%m-%dT%H:%M:%S.%f%z"), date.strptime("2023/12/01", "%Y/%m/%d"))
print(datetime.strptime("2024 060", "%Y %j"), datetime.strptime("2024 10 1", "%Y %W %w"))
try:
    datetime.strptime("2023-07-04x", "%Y-%m-%d")
except ValueError as e:
    print(e)
try:
    datetime.strptime("July", "%Y")
except ValueError as e:
    print(e)
try:
    a < datetime(2024, 1, 1)
except TypeError as e:
    print(e)
print(a == datetime(2024, 1, 1, 12), date(2024, 1, 1) == datetime(2024, 1, 1), x.replace(year=2000, tzinfo=tz))
print(x.timetuple(), a.utctimetuple(), f"{x:%H:%M}", "{:%Y}".format(x), sorted([x, datetime(2000, 1, 1)])[0])
print(datetime.min, datetime.max, isinstance(x, date), {a: 1}[b])
print(tm.gmtime(0), tm.gmtime(1700000000), tm.strftime("%Y-%m-%d %H:%M:%S", tm.gmtime(1234567890)))
print(tm.asctime(tm.gmtime(0)), tm.struct_time((2020, 1, 2, 3, 4, 5, 3, 2, 0)).tm_mday, tm.gmtime(0).tm_wday)
print(tm.strptime("2020-02-03", "%Y-%m-%d")[:8])
print(type(tm.time()).__name__, tm.monotonic() <= tm.monotonic())
lt = datetime.now()
print(abs((datetime.fromtimestamp(lt.timestamp()) - lt).total_seconds()) < 1e-3, abs(tm.mktime(tm.localtime(1e9)) - 1e9) < 1)
print(datetime.fromtimestamp(1e9).astimezone().astimezone(timezone.utc), datetime.fromtimestamp(1e9, timezone.utc))
