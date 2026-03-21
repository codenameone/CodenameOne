package com.codename1.impl.time;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.TimeZone;

public final class TimeZoneSupport {
    private TimeZoneSupport() {
    }

    public static TimeZone toTimeZone(ZoneId zoneId) {
        if (zoneId instanceof ZoneOffset) {
            ZoneOffset offset = (ZoneOffset) zoneId;
            return TimeZone.getTimeZone(offset.getId().equals("Z") ? "GMT" : "GMT" + offset.getId());
        }
        return TimeZone.getTimeZone(zoneId.getId());
    }
}
