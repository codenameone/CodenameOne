package java.time;

import com.codename1.impl.time.TimeZoneSupport;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

final class DateTimeSupport {
    static final long MILLIS_PER_SECOND = 1000L;
    static final long MILLIS_PER_DAY = 86400000L;
    static final long SECONDS_PER_DAY = 86400L;
    static final long NANOS_PER_SECOND = 1000000000L;
    static final long NANOS_PER_MILLI = 1000000L;
    static final long NANOS_PER_DAY = 86400000000000L;

    private static final long DAYS_0000_TO_1970 = 719528L;

    private DateTimeSupport() {
    }

    public static int floorDiv(int x, int y) {
        int r = x / y;
        if ((x ^ y) < 0 && (r * y != x)) {
            r--;
        }
        return r;
    }

    public static long floorDiv(long x, long y) {
        long r = x / y;
        if ((x ^ y) < 0 && (r * y != x)) {
            r--;
        }
        return r;
    }

    public static int floorMod(int x, int y) {
        return x - floorDiv(x, y) * y;
    }

    public static long floorMod(long x, long y) {
        return x - floorDiv(x, y) * y;
    }

    public static boolean isLeapYear(int year) {
        return ((year & 3) == 0) && ((year % 100) != 0 || (year % 400) == 0);
    }

    public static int lengthOfMonth(int year, int month) {
        switch (month) {
            case 2:
                return isLeapYear(year) ? 29 : 28;
            case 4:
            case 6:
            case 9:
            case 11:
                return 30;
            default:
                return 31;
        }
    }

    public static long toEpochDay(int year, int month, int dayOfMonth) {
        long y = year;
        long m = month;
        long total = 365L * y;
        if (y >= 0) {
            total += (y + 3) / 4 - (y + 99) / 100 + (y + 399) / 400;
        } else {
            total -= y / -4 - y / -100 + y / -400;
        }
        total += ((367 * m - 362) / 12);
        total += dayOfMonth - 1;
        if (m > 2) {
            total--;
            if (!isLeapYear(year)) {
                total--;
            }
        }
        return total - DAYS_0000_TO_1970;
    }

    public static int[] epochDayToDate(long epochDay) {
        long zeroDay = epochDay + DAYS_0000_TO_1970;
        zeroDay -= 60;
        long adjust = 0;
        if (zeroDay < 0) {
            long adjustCycles = (zeroDay + 1) / 146097 - 1;
            adjust = adjustCycles * 400;
            zeroDay += -adjustCycles * 146097;
        }
        long yearEst = (400 * zeroDay + 591) / 146097;
        long doyEst = zeroDay - (365 * yearEst + yearEst / 4 - yearEst / 100 + yearEst / 400);
        if (doyEst < 0) {
            yearEst--;
            doyEst = zeroDay - (365 * yearEst + yearEst / 4 - yearEst / 100 + yearEst / 400);
        }
        yearEst += adjust;
        int marchDoy0 = (int) doyEst;
        int marchMonth0 = (marchDoy0 * 5 + 2) / 153;
        int month = (marchMonth0 + 2) % 12 + 1;
        int day = marchDoy0 - (marchMonth0 * 306 + 5) / 10 + 1;
        yearEst += marchMonth0 / 10;
        return new int[] { (int) yearEst, month, day };
    }

    public static void checkDate(int year, int month, int day) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Invalid month: " + month);
        }
        int maxDay = lengthOfMonth(year, month);
        if (day < 1 || day > maxDay) {
            throw new IllegalArgumentException("Invalid day: " + day);
        }
    }

    public static void checkTime(int hour, int minute, int second, int nano) {
        if (hour < 0 || hour > 23) {
            throw new IllegalArgumentException("Invalid hour: " + hour);
        }
        if (minute < 0 || minute > 59) {
            throw new IllegalArgumentException("Invalid minute: " + minute);
        }
        if (second < 0 || second > 59) {
            throw new IllegalArgumentException("Invalid second: " + second);
        }
        if (nano < 0 || nano >= NANOS_PER_SECOND) {
            throw new IllegalArgumentException("Invalid nano: " + nano);
        }
    }

    public static long toEpochSecond(LocalDate date, LocalTime time, ZoneOffset offset) {
        long days = date.toEpochDay();
        long secs = days * SECONDS_PER_DAY + time.toSecondOfDay();
        return secs - offset.getTotalSeconds();
    }

    public static int millisOfSecond(int nano) {
        return nano / 1000000;
    }

    public static Calendar newCalendar(TimeZone tz) {
        Calendar out = Calendar.getInstance(tz);
        return out;
    }

    public static Instant instantFromCalendar(Calendar cal, int nano) {
        long millis = cal.getTime().getTime();
        long epochSecond = floorDiv(millis, 1000L);
        int nanoAdj = (int) (floorMod(millis, 1000L) * NANOS_PER_MILLI) + nano % 1000000;
        if (nanoAdj >= NANOS_PER_SECOND) {
            epochSecond++;
            nanoAdj -= NANOS_PER_SECOND;
        }
        return Instant.ofEpochSecond(epochSecond, nanoAdj);
    }

    public static Calendar calendarFromLocalDateTime(LocalDate date, LocalTime time, TimeZone tz) {
        Calendar cal = newCalendar(tz);
        cal.set(Calendar.YEAR, date.getYear());
        cal.set(Calendar.MONTH, date.getMonthValue() - 1);
        cal.set(Calendar.DAY_OF_MONTH, date.getDayOfMonth());
        cal.set(Calendar.HOUR_OF_DAY, time.getHour());
        cal.set(Calendar.MINUTE, time.getMinute());
        cal.set(Calendar.SECOND, time.getSecond());
        cal.set(Calendar.MILLISECOND, millisOfSecond(time.getNano()));
        cal.getTime();
        return cal;
    }

    public static LocalDateTime localDateTimeFromInstant(Instant instant, ZoneId zone) {
        ZoneOffset offset = offsetFromInstant(instant, zone);
        long localSecond = instant.getEpochSecond() + offset.getTotalSeconds();
        long epochDay = floorDiv(localSecond, SECONDS_PER_DAY);
        int secondOfDay = (int) floorMod(localSecond, SECONDS_PER_DAY);
        LocalDate date = LocalDate.ofEpochDay(epochDay);
        LocalTime time = LocalTime.ofNanoOfDay(secondOfDay * NANOS_PER_SECOND + instant.getNano());
        return LocalDateTime.of(date, time);
    }

    public static ZoneOffset offsetFromInstant(Instant instant, ZoneId zone) {
        TimeZone tz = TimeZoneSupport.toTimeZone(zone);
        Calendar cal = newCalendar(TimeZone.getTimeZone("GMT"));
        cal.setTime(new Date(instant.toEpochMilli()));
        int offsetMillis = tz.getOffset(
                1,
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH),
                cal.get(Calendar.DAY_OF_WEEK),
                ((cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)) * 60 + cal.get(Calendar.SECOND)) * 1000
                        + cal.get(Calendar.MILLISECOND));
        return ZoneOffset.ofTotalSeconds(offsetMillis / 1000);
    }

    public static SimpleDateFormat newFormat(String pattern, ZoneId zone, Locale locale) {
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        return sdf;
    }

    public static String formatPattern(String pattern, TemporalCarrier carrier, Locale locale) {
        ZoneId zone = carrier.getZoneForFormatting();
        SimpleDateFormat sdf = newFormat(pattern, zone, locale);
        TimeZone original = TimeZone.getDefault();
        try {
            if (zone != null) {
                TimeZone.setDefault(TimeZoneSupport.toTimeZone(zone));
            }
            return sdf.format(new Date(carrier.toInstant().toEpochMilli()));
        } finally {
            TimeZone.setDefault(original);
        }
    }

    public static ParsedPatternResult parsePattern(String text, String pattern, ZoneId defaultZone, Locale locale) {
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
            throw new java.time.format.DateTimeParseException(err.getMessage(), text, 0);
        } finally {
            TimeZone.setDefault(original);
        }
    }

    public interface TemporalCarrier {
        Instant toInstant();
        ZoneId getZoneForFormatting();
    }

    public static final class ParsedPatternResult {
        public final Instant instant;
        public final ZoneId zone;

        ParsedPatternResult(Instant instant, ZoneId zone) {
            this.instant = instant;
            this.zone = zone;
        }
    }
}
