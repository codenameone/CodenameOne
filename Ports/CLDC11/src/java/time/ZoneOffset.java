package java.time;

public final class ZoneOffset extends ZoneId {
    public static final ZoneOffset UTC = new ZoneOffset(0);

    private final int totalSeconds;

    private ZoneOffset(int totalSeconds) {
        super(buildId(totalSeconds));
        this.totalSeconds = totalSeconds;
    }

    public static ZoneOffset of(String offsetId) {
        if (offsetId == null) {
            throw new NullPointerException();
        }
        if ("Z".equals(offsetId)) {
            return UTC;
        }
        String text = offsetId;
        char sign = text.charAt(0);
        if (sign != '+' && sign != '-') {
            throw new IllegalArgumentException("Invalid offset: " + offsetId);
        }
        text = text.substring(1);
        int hours;
        int minutes = 0;
        if (text.indexOf(':') > 0) {
            String[] parts = split(text, ':');
            hours = Integer.parseInt(parts[0]);
            minutes = Integer.parseInt(parts[1]);
        } else if (text.length() == 2) {
            hours = Integer.parseInt(text);
        } else if (text.length() == 4) {
            hours = Integer.parseInt(text.substring(0, 2));
            minutes = Integer.parseInt(text.substring(2));
        } else {
            throw new IllegalArgumentException("Invalid offset: " + offsetId);
        }
        int total = hours * 3600 + minutes * 60;
        if (sign == '-') {
            total = -total;
        }
        return ofTotalSeconds(total);
    }

    private static String[] split(String value, char ch) {
        int pos = value.indexOf(ch);
        return new String[] { value.substring(0, pos), value.substring(pos + 1) };
    }

    public static ZoneOffset ofHours(int hours) {
        return ofTotalSeconds(hours * 3600);
    }

    public static ZoneOffset ofHoursMinutes(int hours, int minutes) {
        int sign = hours < 0 || minutes < 0 ? -1 : 1;
        return ofTotalSeconds(hours * 3600 + sign * Math.abs(minutes) * 60);
    }

    public static ZoneOffset ofTotalSeconds(int totalSeconds) {
        if (totalSeconds == 0) {
            return UTC;
        }
        return new ZoneOffset(totalSeconds);
    }

    public int getTotalSeconds() {
        return totalSeconds;
    }

    private static String buildId(int totalSeconds) {
        if (totalSeconds == 0) {
            return "Z";
        }
        int abs = Math.abs(totalSeconds);
        int hours = abs / 3600;
        int minutes = (abs % 3600) / 60;
        StringBuffer sb = new StringBuffer();
        sb.append(totalSeconds < 0 ? '-' : '+');
        if (hours < 10) {
            sb.append('0');
        }
        sb.append(hours);
        sb.append(':');
        if (minutes < 10) {
            sb.append('0');
        }
        sb.append(minutes);
        return sb.toString();
    }
}
