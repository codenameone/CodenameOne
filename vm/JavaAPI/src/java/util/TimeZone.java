/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package java.util;
/**
 * TimeZone represents a time zone offset, and also figures out daylight savings.
 * Typically, you get a TimeZone using getDefault which creates a TimeZone based on the time zone where the program is running. For example, for a program running in Japan, getDefault creates a TimeZone object based on Japanese Standard Time.
 * You can also get a TimeZone using getTimeZone along with a time zone ID. For instance, the time zone ID for the Pacific Standard Time zone is "PST". So, you can get a PST TimeZone object with:
 * This class is a pure subset of the java.util.TimeZone class in JDK 1.3.
 * The only time zone ID that is required to be supported is "GMT".
 * Apart from the methods and variables being subset, the semantics of the getTimeZone() method may also be subset: custom IDs such as "GMT-8:00" are not required to be supported.
 * Version: CLDC 1.1 02/01/2002 (Based on JDK 1.3) See Also:Calendar, Date
 */
public abstract class TimeZone{
    /**
     * The short display name style, such as {@code PDT}. Requests for this
     * style may yield GMT offsets like {@code GMT-08:00}.
     */
    public static final int SHORT = 0;
    
    /**
     * The long display name style, such as {@code Pacific Daylight Time}.
     * Requests for this style may yield GMT offsets like {@code GMT-08:00}.
     */
    public static final int LONG = 1;
    
    static final TimeZone GMT = new SimpleTimeZone(0, "GMT"); // Greenwich Mean Time
    
    private static TimeZone defaultTimeZone;
    
    private String ID;

    public TimeZone(){         
    }

    void setID(String id) {
        ID = id;
    }
    
    /**
     * Gets all the available IDs supported.
     */
    public static java.lang.String[] getAvailableIDs(){
        String i = getTimezoneId();
        if(i.equals("GMT")) {
            return new String[] {"GMT"};//ZoneInfoDB.getAvailableIDs();
        } else {
            return new String[] {"GMT", i};
        }
    }

    /// Resolves the offset for calendar fields expressed in local *standard* time,
    /// which is what java.util.TimeZone.getOffset(era, year, month, day, dayOfWeek,
    /// millis) documents and what every caller here passes: GregorianCalendar
    /// decomposes a local time, DateUtil and both SimpleDateFormats read the fields
    /// off a Calendar in the zone.
    ///
    /// The natives answer a different question -- "what is the offset at the instant
    /// these UTC fields denote" -- and feeding local fields to them straight through
    /// mixed the two frames. America/New_York at 2020-03-08 02:30 standard time is
    /// UTC-04:00, but read as 02:30 UTC it lands the previous evening and answers
    /// UTC-05:00.
    ///
    /// Converting here rather than in each port's native keeps the natives' actual
    /// contract intact and fixes every port at once: the instant the fields denote is
    /// (fields read as UTC) minus the raw offset, and the natives are then asked
    /// about that instant in the UTC fields they expect.
    static int offsetForLocalStandardFields(String id, int rawOffset, int era, int year,
                                            int month, int day, int timeOfDayMillis) {
        int isoYear = era > 0 ? year : 1 - year;
        long fieldsAsUtc = daysFromCivil(isoYear, month + 1, day) * 86400000L + timeOfDayMillis;
        long instant = fieldsAsUtc - rawOffset;
        long epochDay = floorDiv(instant, 86400000L);
        int millisOfDay = (int) (instant - epochDay * 86400000L);
        int[] civil = civilFromDays(epochDay);
        return getTimezoneOffset(id, civil[0], civil[1], civil[2], millisOfDay);
    }

    private static long floorDiv(long value, long divisor) {
        long q = value / divisor;
        if ((value % divisor != 0) && ((value < 0) != (divisor < 0))) {
            q--;
        }
        return q;
    }

    /// Days since 1970-01-01 for a proleptic Gregorian date (Howard Hinnant's
    /// civil-from-days inverse). Integer only, so it is exact for every year the
    /// callers can produce.
    private static long daysFromCivil(int y, int m, int d) {
        int adjusted = y - (m <= 2 ? 1 : 0);
        long era = (adjusted >= 0 ? adjusted : adjusted - 399) / 400;
        int yoe = (int) (adjusted - era * 400);
        int doy = (153 * (m + (m > 2 ? -3 : 9)) + 2) / 5 + d - 1;
        int doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
        return era * 146097L + doe - 719468L;
    }

    /// Inverse of daysFromCivil: { year, month 1-12, day }.
    private static int[] civilFromDays(long z) {
        long shifted = z + 719468L;
        long era = (shifted >= 0 ? shifted : shifted - 146096) / 146097;
        long doe = shifted - era * 146097;
        long yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365;
        long y = yoe + era * 400;
        long doy = doe - (365 * yoe + yoe / 4 - yoe / 100);
        long mp = (5 * doy + 2) / 153;
        long d = doy - (153 * mp + 2) / 5 + 1;
        long m = mp + (mp < 10 ? 3 : -9);
        return new int[] { (int) (y + (m <= 2 ? 1 : 0)), (int) m, (int) d };
    }

    private static native String getTimezoneId();
    private static native int getTimezoneOffset(String name, int year, int month, int day, int timeOfDayMillis);
    private static native int getTimezoneRawOffset(String name);
    private static native boolean isTimezoneDST(String name, long millis);

    private static long getJuly1() {
        long july1_2017 = 1498867200000l;
        long now = System.currentTimeMillis();
        long july1Ish = july1_2017;
        int i=1;
        while (july1Ish < now) {
            july1Ish += 31536000000l;
            if (i % 4  == 0) {
                july1Ish += 86400000l; // add a day for leap year every 4 years
            }
            i++;
        }
        return july1Ish;
    }
    
    private static long getDec30() {
        long dec30_2016 = 1483056000000l;
        long now = System.currentTimeMillis();
        long dec30Ish = dec30_2016;
        int i=1;
        while (dec30Ish < now) {
            dec30Ish += 31536000000l;
            if (i % 4  == 0) {
                dec30Ish += 86400000l; // add a day for leap year every 4 years
            }
            i++;
        }
        return dec30Ish;
    }
    
    /**
     * Gets the default TimeZone for this host. The source of the default TimeZone may vary with implementation.
     */
    public static java.util.TimeZone getDefault(){
        if (defaultTimeZone == null) {
            final String tzone = getTimezoneId();
            defaultTimeZone = new TimeZone() {
                @Override
                public int getOffset(int era, int year, int month, int day, int dayOfWeek, int timeOfDayMillis) {
                    return offsetForLocalStandardFields(tzone, getTimezoneRawOffset(tzone),
                            era, year, month, day, timeOfDayMillis);
                }

                @Override
                public int getRawOffset() {
                    return getTimezoneRawOffset(tzone);
                }

                boolean inDaylightTime(Date time) {
                    return isTimezoneDST(tzone, time.getTime());
                }

                @Override
                public boolean useDaylightTime() {
                    return isTimezoneDST(tzone, getDec30()) != isTimezoneDST(tzone, getJuly1()); // 26 weeks
                }
            };
            defaultTimeZone.ID = tzone;
        }
        return defaultTimeZone;
    }

    public static void setDefault(TimeZone timezone) {
        defaultTimeZone = timezone;
    }
    
    

    int getDSTSavings() {
        return useDaylightTime() ? 3600000 : 0;
    }
    
    
    boolean inDaylightTime(Date time) {
        return false;
    }
    
    /**
     * Gets the ID of this time zone.
     */
    public java.lang.String getID(){
        return ID;
    }

    /**
     * Gets offset, for current date, modified in case of daylight savings. This is the offset to add *to* GMT to get local time. Gets the time zone offset, for current date, modified in case of daylight savings. This is the offset to add *to* GMT to get local time. Assume that the start and end month are distinct. This method may return incorrect results for rules that start at the end of February (e.g., last Sunday in February) or the beginning of March (e.g., March 1).
     */
    public abstract int getOffset(int era, int year, int month, int day, int dayOfWeek, int millis);

    /**
     * Gets the GMT offset for this time zone.
     */
    public abstract int getRawOffset();

    /**
     * Gets the TimeZone for the given ID.
     */
    public static java.util.TimeZone getTimeZone(final java.lang.String ID){
        if (ID == null) {
            // Fail here rather than three statements down, where the first
            // unguarded equalsIgnoreCase used to throw from the middle of the
            // method. NullPointerException is the right answer rather than GMT:
            // the JDK throws for a null ID and reserves GMT for an ID it merely
            // cannot parse, and the JavaSE and Android ports reach that JDK
            // behaviour directly -- so answering GMT here would put this port
            // out of step with them.
            throw new NullPointerException("ID");
        }
        if(ID.equalsIgnoreCase("gmt")) {
            return GMT;
        }
        TimeZone custom = customTimeZone(ID);
        if (custom != null) {
            return custom;
        }
        if (isOffsetIdAttempt(ID)) {
            // A malformed offset ID resolves to GMT, as it does on every other
            // port. Handing it to the platform instead was actively harmful:
            // the POSIX tz syntax the natives speak reads the sign the other way
            // round, so "UTC+5" came back as five hours *west* -- a ten-hour
            // error against JavaSE for the same string.
            return GMT;
        }
        if (ID.equalsIgnoreCase(getTimezoneId())) {
            return getDefault();
        } else {
            TimeZone out = new TimeZone() {
                @Override
                public int getOffset(int era, int year, int month, int day, int dayOfWeek, int timeOfDayMillis) {
                    return offsetForLocalStandardFields(ID, getTimezoneRawOffset(ID),
                            era, year, month, day, timeOfDayMillis);
                }

                @Override
                public int getRawOffset() {
                    return getTimezoneRawOffset(ID);
                }

                boolean inDaylightTime(Date time) {
                    return isTimezoneDST(ID, time.getTime());
                }

                @Override
                public boolean useDaylightTime() {
                    return isTimezoneDST(ID, getDec30()) != isTimezoneDST(ID, getJuly1()); // 26 weeks
                }
                
                public boolean equals(Object tz) {
                    return (tz instanceof TimeZone && ID.equalsIgnoreCase(((TimeZone)tz).ID));
                }

                public int hashCode() {
                    return ID.hashCode();
                }
                
            };
            out.ID = ID;
            return out;
        }
    }

    /**
     * Resolves a custom fixed-offset ID -- {@code GMT+2}, {@code GMT-05:00},
     * {@code UTC+01:30} -- to a zone with that raw offset and no daylight
     * saving, exactly as {@code java.util.TimeZone} documents them.
     *
     * These must never reach the host time zone database. A POSIX {@code TZ}
     * value inverts the sign of its offset, so handing {@code "GMT-05:00"} to
     * {@code tzset()} produced UTC+5 -- java.time converts a ZoneOffset to
     * exactly this form, so every OffsetDateTime formatted through a pattern
     * came out shifted by twice its offset. Windows is worse: its C runtime
     * cannot parse the form at all.
     *
     * @return the fixed-offset zone, or null when {@code ID} is not a custom ID
     */
    private static TimeZone customTimeZone(String ID) {
        if (ID == null) {
            return null;
        }
        int index;
        if (ID.regionMatches(true, 0, "GMT", 0, 3)) {
            index = 3;
        } else if (ID.regionMatches(true, 0, "UTC", 0, 3)) {
            index = 3;
        } else if (ID.regionMatches(true, 0, "UT", 0, 2)) {
            index = 2;
        } else {
            return null;
        }
        if (index >= ID.length()) {
            // Exact uppercase "UTC" is the only bare spelling that names a zone of
            // its own. "UT", "utc", "ut" and the rest take the unknown-ID fallback
            // on JavaSE and Android -- verified identical on JDK 17 and 25 -- so
            // keeping the caller's spelling here made getID() and equals() differ
            // across ports while the offset agreed.
            if ("UTC".equals(ID)) {
                return new SimpleTimeZone(0, ID);
            }
            return GMT;
        }
        char sign = ID.charAt(index);
        if (sign == 'Z' && index + 1 == ID.length()) {
            // "GMTZ" / "UTCZ" / "UTZ" name no zone. Manufacturing a SimpleTimeZone
            // that keeps the spelling made getID() and equals() disagree with
            // JavaSE and Android, which take the unknown-ID fallback and answer a
            // zone whose ID is "GMT" -- the raw offset matched, so the difference
            // only surfaced in a serialized configuration or an ID comparison.
            return GMT;
        }
        if (sign != '+' && sign != '-') {
            return null;
        }
        // Only "GMT" takes an offset suffix. java.util.TimeZone defines the
        // custom-ID syntax on GMT alone, so JavaSE and Android answer plain GMT
        // for "UTC+5" / "UT+5"; accepting them here gave the same ID a different
        // offset depending on the port.
        if (index != 3 || !ID.startsWith("GMT")) {
            return null;
        }
        String digits = ID.substring(index + 1);
        String hourPart;
        String minutePart;
        int colon = digits.indexOf(':');
        if (colon < 0) {
            // Colon-less forms are h, hh, hmm and hhmm only. The five- and
            // six-digit forms this used to accept ("GMT+013000") are not custom
            // IDs at all -- the JDK falls back to GMT for them.
            int length = digits.length();
            if (length == 1 || length == 2) {
                hourPart = digits;
                minutePart = "0";
            } else if (length == 3 || length == 4) {
                hourPart = digits.substring(0, length - 2);
                minutePart = digits.substring(length - 2);
            } else {
                return null;
            }
        } else {
            String rest = digits.substring(colon + 1);
            hourPart = digits.substring(0, colon);
            // No seconds field. java.util.TimeZone documents the custom syntax
            // as GMT Sign Hours [: Minutes] and nothing more, and JDKs disagree
            // in practice -- "GMT+01:30:00" is GMT on 17 and GMT+01:30 on 25 --
            // so honouring it would make this port track whichever JDK the
            // JavaSE side happened to run. The documented form is the contract.
            if (rest.indexOf(':') >= 0) {
                return null;
            }
            minutePart = rest;
            // Hours may be one or two digits, minutes must be exactly two --
            // "GMT+1:2" is not a custom ID, and treating it as UTC+01:02 put
            // this port an hour and two minutes away from every other one.
            if (hourPart.length() < 1 || hourPart.length() > 2 || minutePart.length() != 2) {
                return null;
            }
        }
        if (!isDigits(hourPart) || !isDigits(minutePart)) {
            return null;
        }
        int hours;
        int minutes;
        try {
            hours = Integer.parseInt(hourPart);
            minutes = Integer.parseInt(minutePart);
        } catch (NumberFormatException notCustom) {
            return null;
        }
        if (hours > 23 || minutes > 59) {
            return null;
        }
        int offset = (hours * 60 + minutes) * 60 * 1000;
        // Normalized like the JDK's, so the same custom ID reports the same
        // getID() on every port rather than echoing whichever spelling was used.
        String canonical = "GMT" + sign
                + (hours < 10 ? "0" : "") + hours + ":"
                + (minutes < 10 ? "0" : "") + minutes;
        return new SimpleTimeZone(sign == '-' ? -offset : offset, canonical);
    }

    /**
     * True when the ID reads as an attempt at a GMT/UT/UTC offset ID, well
     * formed or not. Those never name a zone in the platform database, so a
     * malformed one is GMT rather than something for the natives to guess at.
     */
    private static boolean isOffsetIdAttempt(String ID) {
        int index;
        if (ID.regionMatches(true, 0, "GMT", 0, 3) || ID.regionMatches(true, 0, "UTC", 0, 3)) {
            index = 3;
        } else if (ID.regionMatches(true, 0, "UT", 0, 2)) {
            index = 2;
        } else {
            return false;
        }
        if (index >= ID.length()) {
            return false;
        }
        char sign = ID.charAt(index);
        return sign == '+' || sign == '-';
    }

    /** True when every character is an ASCII digit and there is at least one. */
    private static boolean isDigits(String value) {
        if (value.length() == 0) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }


    /**
     * Queries if this time zone uses Daylight Savings Time.
     */
    public abstract boolean useDaylightTime();

}
