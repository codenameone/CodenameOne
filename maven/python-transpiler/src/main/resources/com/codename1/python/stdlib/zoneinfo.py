# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""IANA time zones from the platform's own time zone database.

Offsets come from the device (java.util.TimeZone, which is the operating
system's zone data on iOS and Android), so no tzdata package is shipped.
Abbreviations are not part of what every platform reports: the common
zones' are known here, and any other zone is named by its offset ("+03"),
as tzdata itself names zones that have no abbreviation.
"""

import _cn1tz
from datetime import datetime, timedelta, timezone, tzinfo

__all__ = ["ZoneInfo", "ZoneInfoNotFoundError", "InvalidTZPathError", "reset_tzpath", "available_timezones",
           "TZPATH"]

TZPATH = ()


class ZoneInfoNotFoundError(KeyError):
    pass


class InvalidTZPathError(ValueError):
    pass


def reset_tzpath(to=None):
    pass


def available_timezones():
    return set(k for k in _cn1tz.available() if "/" in k or k == "UTC")


# (standard, daylight) abbreviations of common zones
_ABBREV = {
    "UTC": ("UTC", "UTC"), "Etc/UTC": ("UTC", "UTC"), "GMT": ("GMT", "GMT"), "Etc/GMT": ("GMT", "GMT"),
    "America/New_York": ("EST", "EDT"), "America/Detroit": ("EST", "EDT"), "America/Toronto": ("EST", "EDT"),
    "America/Chicago": ("CST", "CDT"), "America/Winnipeg": ("CST", "CDT"), "America/Mexico_City": ("CST", "CDT"),
    "America/Denver": ("MST", "MDT"), "America/Edmonton": ("MST", "MDT"), "America/Phoenix": ("MST", "MST"),
    "America/Los_Angeles": ("PST", "PDT"), "America/Vancouver": ("PST", "PDT"), "America/Tijuana": ("PST", "PDT"),
    "America/Anchorage": ("AKST", "AKDT"), "Pacific/Honolulu": ("HST", "HST"), "America/Halifax": ("AST", "ADT"),
    "America/St_Johns": ("NST", "NDT"), "America/Puerto_Rico": ("AST", "AST"),
    "Europe/London": ("GMT", "BST"), "Europe/Dublin": ("GMT", "IST"), "Europe/Lisbon": ("WET", "WEST"),
    "Atlantic/Canary": ("WET", "WEST"),
    "Europe/Paris": ("CET", "CEST"), "Europe/Berlin": ("CET", "CEST"), "Europe/Madrid": ("CET", "CEST"),
    "Europe/Rome": ("CET", "CEST"), "Europe/Amsterdam": ("CET", "CEST"), "Europe/Brussels": ("CET", "CEST"),
    "Europe/Vienna": ("CET", "CEST"), "Europe/Zurich": ("CET", "CEST"), "Europe/Stockholm": ("CET", "CEST"),
    "Europe/Oslo": ("CET", "CEST"), "Europe/Copenhagen": ("CET", "CEST"), "Europe/Prague": ("CET", "CEST"),
    "Europe/Warsaw": ("CET", "CEST"), "Europe/Budapest": ("CET", "CEST"), "Europe/Belgrade": ("CET", "CEST"),
    "Africa/Lagos": ("WAT", "WAT"),
    "Europe/Athens": ("EET", "EEST"), "Europe/Helsinki": ("EET", "EEST"), "Europe/Kiev": ("EET", "EEST"),
    "Europe/Kyiv": ("EET", "EEST"), "Europe/Bucharest": ("EET", "EEST"), "Europe/Sofia": ("EET", "EEST"),
    "Africa/Cairo": ("EET", "EEST"), "Asia/Jerusalem": ("IST", "IDT"), "Europe/Moscow": ("MSK", "MSK"),
    "Africa/Johannesburg": ("SAST", "SAST"), "Africa/Nairobi": ("EAT", "EAT"),
    "Asia/Kolkata": ("IST", "IST"), "Asia/Calcutta": ("IST", "IST"), "Asia/Karachi": ("PKT", "PKT"),
    "Asia/Shanghai": ("CST", "CST"), "Asia/Hong_Kong": ("HKT", "HKT"), "Asia/Taipei": ("CST", "CST"),
    "Asia/Manila": ("PST", "PST"), "Asia/Jakarta": ("WIB", "WIB"),
    "Asia/Tokyo": ("JST", "JST"), "Asia/Seoul": ("KST", "KST"),
    "Australia/Sydney": ("AEST", "AEDT"), "Australia/Melbourne": ("AEST", "AEDT"),
    "Australia/Hobart": ("AEST", "AEDT"), "Australia/Brisbane": ("AEST", "AEST"),
    "Australia/Adelaide": ("ACST", "ACDT"), "Australia/Darwin": ("ACST", "ACST"),
    "Australia/Perth": ("AWST", "AWST"), "Pacific/Auckland": ("NZST", "NZDT"),
}

_EPOCH = datetime(1970, 1, 1)
_DAY = 86400


def _offset_name(seconds):
    sign = "+" if seconds >= 0 else "-"
    seconds = abs(seconds)
    h, rem = divmod(seconds, 3600)
    m = rem // 60
    return "%s%02d" % (sign, h) if m == 0 else "%s%02d%02d" % (sign, h, m)


class ZoneInfo(tzinfo):
    _cache = {}

    def __new__(cls, key):
        z = cls._cache.get(key)
        if z is None:
            z = cls.no_cache(key)
            cls._cache[key] = z
        return z

    @classmethod
    def no_cache(cls, key):
        if not isinstance(key, str):
            raise TypeError("key must be a str")
        if not key or key.startswith("/") or ".." in key.split("/") or not _cn1tz.known(key):
            raise ZoneInfoNotFoundError("No time zone found with key %s" % key)
        self = object.__new__(cls)
        self._key = key
        self._raw = _cn1tz.raw(key)
        return self

    @classmethod
    def clear_cache(cls, *, only_keys=None):
        if only_keys is None:
            cls._cache.clear()
        else:
            for k in only_keys:
                cls._cache.pop(k, None)

    @classmethod
    def from_file(cls, fobj, key=None):
        raise ValueError("zone files are not read: the platform's zone data is used")

    @property
    def key(self):
        return self._key

    def __str__(self):
        return self._key

    def __repr__(self):
        return "zoneinfo.ZoneInfo(key=%r)" % self._key

    def __reduce__(self):
        return (self.__class__, (self._key,))

    def _at(self, utc_seconds):
        return _cn1tz.offset(self._key, utc_seconds)

    def _wall_offset(self, dt):
        """The offset in seconds at the wall time dt, honouring dt.fold."""
        naive = dt.replace(tzinfo=None, fold=0)
        w = (naive - _EPOCH) // timedelta(seconds=1)
        before = self._at(w - _DAY)
        after = self._at(w + _DAY)
        if before == after:
            return self._at(w - before)
        ok_before = self._at(w - before) == before
        ok_after = self._at(w - after) == after
        if ok_before and ok_after:
            return after if dt.fold else before
        if ok_before:
            return before
        if ok_after:
            return after
        return after if dt.fold else before

    def utcoffset(self, dt):
        if dt is None:
            return None
        return timedelta(seconds=self._wall_offset(dt))

    def dst(self, dt):
        if dt is None:
            return None
        off = self._wall_offset(dt)
        return timedelta(seconds=max(0, off - self._std_near(dt, off)))

    def _std_near(self, dt, off):
        # the standard offset is the smaller of the year's two offsets
        naive = dt.replace(tzinfo=None, fold=0)
        w = (naive - _EPOCH) // timedelta(seconds=1)
        jan = self._at(w - (naive.timetuple().tm_yday - 1) * _DAY)
        jul = self._at(w + (182 - naive.timetuple().tm_yday) * _DAY)
        return min(jan, jul, off)

    def tzname(self, dt):
        if dt is None:
            return None
        off = self._wall_offset(dt)
        names = _ABBREV.get(self._key)
        if names is not None:
            return names[1] if off > self._std_near(dt, off) else names[0]
        return _offset_name(off)

    def fromutc(self, dt):
        if not isinstance(dt, datetime):
            raise TypeError("fromutc() requires a datetime argument")
        if dt.tzinfo is not self:
            raise ValueError("dt.tzinfo is not self")
        naive = dt.replace(tzinfo=None)
        u = (naive - _EPOCH) // timedelta(seconds=1)
        off = self._at(u)
        local = dt + timedelta(seconds=off)
        # the second of two wall times that repeat is fold=1
        earlier = self._at(u - _DAY)
        if earlier > off and self._at(u - (earlier - off)) == earlier:
            local = local.replace(fold=1)
        return local
