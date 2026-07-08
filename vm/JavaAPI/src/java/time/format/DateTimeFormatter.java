package java.time.format;

import com.codename1.impl.time.TimeZoneSupport;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class DateTimeFormatter {
    private static final int TYPE_PATTERN = 0;
    private static final int TYPE_ISO_LOCAL_DATE = 1;
    private static final int TYPE_ISO_LOCAL_TIME = 2;
    private static final int TYPE_ISO_LOCAL_DATE_TIME = 3;
    private static final int TYPE_ISO_OFFSET_DATE_TIME = 4;
    private static final int TYPE_ISO_ZONED_DATE_TIME = 5;
    private static final int TYPE_ISO_INSTANT = 6;

    public static final DateTimeFormatter ISO_LOCAL_DATE = new DateTimeFormatter(TYPE_ISO_LOCAL_DATE, null, null);
    public static final DateTimeFormatter ISO_LOCAL_TIME = new DateTimeFormatter(TYPE_ISO_LOCAL_TIME, null, null);
    public static final DateTimeFormatter ISO_LOCAL_DATE_TIME = new DateTimeFormatter(TYPE_ISO_LOCAL_DATE_TIME, null, null);
    public static final DateTimeFormatter ISO_OFFSET_DATE_TIME = new DateTimeFormatter(TYPE_ISO_OFFSET_DATE_TIME, null, null);
    public static final DateTimeFormatter ISO_ZONED_DATE_TIME = new DateTimeFormatter(TYPE_ISO_ZONED_DATE_TIME, null, null);
    public static final DateTimeFormatter ISO_INSTANT = new DateTimeFormatter(TYPE_ISO_INSTANT, null, null);

    private final int type;
    private final String pattern;
    private final Locale locale;

    private DateTimeFormatter(int type, String pattern, Locale locale) {
        this.type = type;
        this.pattern = pattern;
        this.locale = locale;
    }

    public static DateTimeFormatter ofPattern(String pattern) {
        return new DateTimeFormatter(TYPE_PATTERN, pattern, null);
    }

    public static DateTimeFormatter ofPattern(String pattern, Locale locale) {
        return new DateTimeFormatter(TYPE_PATTERN, pattern, locale);
    }

    public String format(TemporalAccessor temporal) {
        if (temporal == null) {
            throw new NullPointerException();
        }
        switch (type) {
            case TYPE_PATTERN:
                return formatPattern(temporal);
            case TYPE_ISO_LOCAL_DATE:
                return formatLocalDate((LocalDate) temporal);
            case TYPE_ISO_LOCAL_TIME:
                return formatLocalTime((LocalTime) temporal);
            case TYPE_ISO_LOCAL_DATE_TIME:
                return formatLocalDate((LocalDateTime) temporal) + "T" + formatLocalTime(((LocalDateTime) temporal).toLocalTime());
            case TYPE_ISO_OFFSET_DATE_TIME:
                return formatLocalDateTime(((OffsetDateTime) temporal).toLocalDateTime()) + formatOffset(((OffsetDateTime) temporal).getOffset());
            case TYPE_ISO_ZONED_DATE_TIME:
                ZonedDateTime zdt = (ZonedDateTime) temporal;
                return formatLocalDateTime(zdt.toLocalDateTime()) + formatOffset(zdt.getOffset()) + "[" + zdt.getZone().getId() + "]";
            case TYPE_ISO_INSTANT:
                return formatInstant((Instant) temporal);
            default:
                throw new IllegalStateException();
        }
    }

    public TemporalAccessor parse(CharSequence text) {
        switch (type) {
            case TYPE_ISO_LOCAL_DATE:
                return parseLocalDate(text.toString());
            case TYPE_ISO_LOCAL_TIME:
                return parseLocalTime(text.toString());
            case TYPE_ISO_LOCAL_DATE_TIME:
                return parseLocalDateTime(text.toString());
            case TYPE_ISO_OFFSET_DATE_TIME:
                return parseOffsetDateTime(text.toString());
            case TYPE_ISO_ZONED_DATE_TIME:
                return parseZonedDateTime(text.toString());
            case TYPE_ISO_INSTANT:
                return parseInstant(text.toString());
            case TYPE_PATTERN:
                return parseLocalDateTime(text.toString());
            default:
                throw new IllegalStateException();
        }
    }

    LocalDate parseLocalDate(String text) {
        if (type == TYPE_PATTERN) {
            ParsedPatternResult parsed = parsePattern(text, pattern, ZoneOffset.UTC, locale);
            return LocalDateTime.ofInstant(parsed.instant, ZoneOffset.UTC).toLocalDate();
        }
        if (type != TYPE_ISO_LOCAL_DATE) {
            throw new DateTimeParseException("Formatter does not produce LocalDate", text, 0);
        }
        return LocalDate.of(parseInt(text, 0, 4), parseInt(text, 5, 7), parseInt(text, 8, 10));
    }

    LocalTime parseLocalTime(String text) {
        if (type == TYPE_PATTERN) {
            ParsedPatternResult parsed = parsePattern(text, pattern, ZoneOffset.UTC, locale);
            return LocalDateTime.ofInstant(parsed.instant, ZoneOffset.UTC).toLocalTime();
        }
        if (type != TYPE_ISO_LOCAL_TIME) {
            throw new DateTimeParseException("Formatter does not produce LocalTime", text, 0);
        }
        int hour = parseInt(text, 0, 2);
        int minute = parseInt(text, 3, 5);
        int second = text.length() >= 8 ? parseInt(text, 6, 8) : 0;
        int nano = 0;
        int dot = text.indexOf('.');
        if (dot > 0) {
            String frac = text.substring(dot + 1);
            while (frac.length() < 9) {
                frac += "0";
            }
            if (frac.length() > 9) {
                frac = frac.substring(0, 9);
            }
            nano = Integer.parseInt(frac);
        }
        return LocalTime.of(hour, minute, second, nano);
    }

    LocalDateTime parseLocalDateTime(String text) {
        if (type == TYPE_PATTERN) {
            ParsedPatternResult parsed = parsePattern(text, pattern, ZoneOffset.UTC, locale);
            return LocalDateTime.ofInstant(parsed.instant, ZoneOffset.UTC);
        }
        if (type != TYPE_ISO_LOCAL_DATE_TIME) {
            throw new DateTimeParseException("Formatter does not produce LocalDateTime", text, 0);
        }
        int t = text.indexOf('T');
        return LocalDateTime.of(parseLocalDate(text.substring(0, t)), parseLocalTime(text.substring(t + 1)));
    }

    OffsetDateTime parseOffsetDateTime(String text) {
        if (type == TYPE_PATTERN) {
            ParsedPatternResult parsed = parsePattern(text, pattern, ZoneOffset.UTC, locale);
            return OffsetDateTime.ofInstant(parsed.instant, parsed.zone);
        }
        if (type != TYPE_ISO_OFFSET_DATE_TIME) {
            throw new DateTimeParseException("Formatter does not produce OffsetDateTime", text, 0);
        }
        int idx = Math.max(text.lastIndexOf('+'), text.lastIndexOf('-'));
        if (text.endsWith("Z")) {
            idx = text.length() - 1;
        }
        LocalDateTime ldt = parseLocalDateTime(text.substring(0, idx));
        ZoneOffset offset = text.endsWith("Z") ? ZoneOffset.UTC : ZoneOffset.of(text.substring(idx));
        return OffsetDateTime.of(ldt, offset);
    }

    ZonedDateTime parseZonedDateTime(String text) {
        if (type == TYPE_PATTERN) {
            ParsedPatternResult parsed = parsePattern(text, pattern, ZoneId.systemDefault(), locale);
            return ZonedDateTime.ofInstant(parsed.instant, parsed.zone);
        }
        if (type != TYPE_ISO_ZONED_DATE_TIME) {
            throw new DateTimeParseException("Formatter does not produce ZonedDateTime", text, 0);
        }
        int zoneStart = text.indexOf('[');
        int offsetIdx = Math.max(text.lastIndexOf('+'), text.lastIndexOf('-'));
        if (text.indexOf('Z') > 0 && (offsetIdx < 0 || text.indexOf('Z') < zoneStart)) {
            offsetIdx = text.indexOf('Z');
        }
        LocalDateTime ldt = parseLocalDateTime(text.substring(0, offsetIdx));
        ZoneOffset offset = text.charAt(offsetIdx) == 'Z' ? ZoneOffset.UTC : ZoneOffset.of(text.substring(offsetIdx, zoneStart));
        ZoneId zone = ZoneId.of(text.substring(zoneStart + 1, text.length() - 1));
        return ZonedDateTime.ofInstant(ldt.toInstant(offset), zone);
    }

    Instant parseInstant(String text) {
        if (type != TYPE_ISO_INSTANT) {
            throw new DateTimeParseException("Formatter does not produce Instant", text, 0);
        }
        OffsetDateTime odt = ISO_OFFSET_DATE_TIME.parseOffsetDateTime(text.endsWith("Z")
                ? text.substring(0, text.length() - 1) + "+00:00"
                : text);
        return odt.toInstant();
    }

    private String formatPattern(TemporalAccessor temporal) {
        ZoneId zone;
        Instant instant;
        if (temporal instanceof Instant) {
            instant = (Instant) temporal;
            zone = ZoneOffset.UTC;
        } else if (temporal instanceof LocalDateTime) {
            instant = ((LocalDateTime) temporal).toInstant(ZoneOffset.UTC);
            zone = ZoneOffset.UTC;
        } else if (temporal instanceof OffsetDateTime) {
            OffsetDateTime offsetDateTime = (OffsetDateTime) temporal;
            instant = offsetDateTime.toInstant();
            zone = offsetDateTime.getOffset();
        } else if (temporal instanceof ZonedDateTime) {
            ZonedDateTime zonedDateTime = (ZonedDateTime) temporal;
            instant = zonedDateTime.toInstant();
            zone = zonedDateTime.getZone();
        } else {
            throw new IllegalArgumentException("Unsupported temporal type");
        }
        SimpleDateFormat sdf = newFormat(pattern, zone, locale);
        TimeZone original = TimeZone.getDefault();
        try {
            if (zone != null) {
                TimeZone.setDefault(TimeZoneSupport.toTimeZone(zone));
            }
            return sdf.format(new Date(instant.toEpochMilli()));
        } finally {
            TimeZone.setDefault(original);
        }
    }

    private static SimpleDateFormat newFormat(String pattern, ZoneId zone, Locale locale) {
        return new SimpleDateFormat(pattern);
    }

    private static ParsedPatternResult parsePattern(String text, String pattern, ZoneId defaultZone, Locale locale) {
        TimeZone original = TimeZone.getDefault();
        try {
            if (defaultZone != null) {
                TimeZone.setDefault(TimeZoneSupport.toTimeZone(defaultZone));
            }
            SimpleDateFormat sdf = newFormat(pattern, defaultZone, locale);
            Date date = sdf.parse(text);
            Instant instant = Instant.ofEpochMilli(date.getTime());
            ZoneId zone = defaultZone == null ? ZoneOffset.UTC : defaultZone;
            return new ParsedPatternResult(instant, zone);
        } catch (ParseException err) {
            throw new DateTimeParseException(err.getMessage(), text, 0);
        } finally {
            TimeZone.setDefault(original);
        }
    }

    private static final class ParsedPatternResult {
        final Instant instant;
        final ZoneId zone;

        ParsedPatternResult(Instant instant, ZoneId zone) {
            this.instant = instant;
            this.zone = zone;
        }
    }

    private static int parseInt(String text, int start, int end) {
        return Integer.parseInt(text.substring(start, end));
    }

    private static String pad(int value, int length) {
        String s = String.valueOf(Math.abs(value));
        StringBuffer out = new StringBuffer();
        if (value < 0) {
            out.append('-');
        }
        for (int i = s.length(); i < length; i++) {
            out.append('0');
        }
        out.append(s);
        return out.toString();
    }

    private static String formatLocalDate(LocalDate date) {
        return pad(date.getYear(), 4) + "-" + pad(date.getMonthValue(), 2) + "-" + pad(date.getDayOfMonth(), 2);
    }

    private static String formatLocalTime(LocalTime time) {
        String base = pad(time.getHour(), 2) + ":" + pad(time.getMinute(), 2) + ":" + pad(time.getSecond(), 2);
        if (time.getNano() == 0) {
            return base;
        }
        String frac = String.valueOf(1000000000L + time.getNano()).substring(1);
        while (frac.endsWith("0")) {
            frac = frac.substring(0, frac.length() - 1);
        }
        return base + "." + frac;
    }

    private static String formatLocalDate(LocalDateTime dateTime) {
        return formatLocalDate(dateTime.toLocalDate());
    }

    private static String formatLocalDateTime(LocalDateTime dateTime) {
        return formatLocalDate(dateTime.toLocalDate()) + "T" + formatLocalTime(dateTime.toLocalTime());
    }

    private static String formatOffset(ZoneOffset offset) {
        return offset.getId();
    }

    private static String formatInstant(Instant instant) {
        LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        return formatLocalDateTime(dateTime) + "Z";
    }
}
