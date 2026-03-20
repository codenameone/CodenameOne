package java.time;

import java.util.TimeZone;

public class ZoneId {
    private final String id;

    ZoneId(String id) {
        this.id = id;
    }

    public static ZoneId of(String zoneId) {
        if (zoneId == null) {
            throw new NullPointerException();
        }
        if ("Z".equals(zoneId) || "UTC".equals(zoneId) || "GMT".equals(zoneId)) {
            return ZoneOffset.UTC;
        }
        if (zoneId.startsWith("+") || zoneId.startsWith("-")) {
            return ZoneOffset.of(zoneId);
        }
        if (zoneId.startsWith("UTC+") || zoneId.startsWith("UTC-")) {
            return ZoneOffset.of(zoneId.substring(3));
        }
        if (zoneId.startsWith("GMT+") || zoneId.startsWith("GMT-")) {
            return ZoneOffset.of(zoneId.substring(3));
        }
        return new ZoneId(zoneId);
    }

    public static ZoneId systemDefault() {
        return of(TimeZone.getDefault().getID());
    }

    public String getId() {
        return id;
    }

    TimeZone toTimeZone() {
        if (this instanceof ZoneOffset) {
            ZoneOffset offset = (ZoneOffset) this;
            return TimeZone.getTimeZone(offset.getId().equals("Z") ? "GMT" : "GMT" + offset.getId());
        }
        return TimeZone.getTimeZone(id);
    }

    public boolean equals(Object obj) {
        return obj instanceof ZoneId && id.equals(((ZoneId) obj).id);
    }

    public int hashCode() {
        return id.hashCode();
    }

    public String toString() {
        return id;
    }
}
