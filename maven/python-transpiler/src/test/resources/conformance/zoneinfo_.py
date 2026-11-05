from datetime import datetime, timedelta, timezone
from zoneinfo import ZoneInfo, ZoneInfoNotFoundError

ny = ZoneInfo("America/New_York")
print(ny, repr(ny), ny.key, ZoneInfo("America/New_York") is ny)
for zone in ("America/New_York", "Europe/London", "Europe/Paris", "Asia/Tokyo", "Australia/Sydney",
             "Asia/Kolkata", "America/Los_Angeles", "UTC"):
    z = ZoneInfo(zone)
    for when in (datetime(2024, 1, 15, 12, 0), datetime(2024, 7, 15, 12, 0)):
        dt = when.replace(tzinfo=z)
        print(zone, dt.isoformat(), dt.utcoffset(), dt.dst(), dt.tzname())

# conversion from UTC across a transition
utc = datetime(2024, 3, 10, 6, 30, tzinfo=timezone.utc)
for h in range(4):
    t = (utc + timedelta(hours=h)).astimezone(ny)
    print("spring", t.isoformat(), t.tzname(), t.fold)
utc = datetime(2024, 11, 3, 4, 30, tzinfo=timezone.utc)
for h in range(4):
    t = (utc + timedelta(hours=h)).astimezone(ny)
    print("fall", t.isoformat(), t.tzname(), t.fold)

# ambiguous and missing wall times
amb = datetime(2024, 11, 3, 1, 30, tzinfo=ny)
print("ambiguous", amb.utcoffset(), amb.replace(fold=1).utcoffset(), amb.tzname(), amb.replace(fold=1).tzname())
gap = datetime(2024, 3, 10, 2, 30, tzinfo=ny)
print("gap", gap.utcoffset(), gap.replace(fold=1).utcoffset())
print("to utc", amb.astimezone(timezone.utc), amb.replace(fold=1).astimezone(timezone.utc))

# arithmetic and comparison
a = datetime(2024, 6, 1, 9, 0, tzinfo=ZoneInfo("Europe/Paris"))
b = datetime(2024, 6, 1, 16, 0, tzinfo=ZoneInfo("Asia/Tokyo"))
print(b - a, a < b, a.astimezone(ZoneInfo("Asia/Tokyo")))
print(datetime(2024, 1, 1, tzinfo=ZoneInfo("UTC")).timestamp())

try:
    ZoneInfo("Not/AZone")
except ZoneInfoNotFoundError as e:
    print("not found", type(e).__name__, isinstance(e, KeyError))
