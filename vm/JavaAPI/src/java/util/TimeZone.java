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
                    return getTimezoneOffset(tzone, year, month + 1, day, timeOfDayMillis);
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
        if(ID != null && ID.equalsIgnoreCase("gmt")) {
            return GMT;
        }
        TimeZone custom = customTimeZone(ID);
        if (custom != null) {
            return custom;
        }
        if (ID.equalsIgnoreCase(getTimezoneId())) {
            return getDefault();
        } else {
            TimeZone out = new TimeZone() {
                @Override
                public int getOffset(int era, int year, int month, int day, int dayOfWeek, int timeOfDayMillis) {
                    return getTimezoneOffset(ID, year, month + 1, day, timeOfDayMillis);
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
            return new SimpleTimeZone(0, ID);
        }
        char sign = ID.charAt(index);
        if (sign == 'Z' && index + 1 == ID.length()) {
            return new SimpleTimeZone(0, ID);
        }
        if (sign != '+' && sign != '-') {
            return null;
        }
        String digits = ID.substring(index + 1);
        int colon = digits.indexOf(':');
        String hourPart = colon < 0 ? digits : digits.substring(0, colon);
        String rest = colon < 0 ? "" : digits.substring(colon + 1);
        String minutePart = "0";
        String secondPart = "0";
        if (colon < 0) {
            // The colon-less forms are hh, hhmm and hhmmss.
            if (digits.length() == 4 || digits.length() == 6) {
                hourPart = digits.substring(0, 2);
                minutePart = digits.substring(2, 4);
                secondPart = digits.length() == 6 ? digits.substring(4, 6) : "0";
            }
        } else {
            int secondColon = rest.indexOf(':');
            minutePart = secondColon < 0 ? rest : rest.substring(0, secondColon);
            secondPart = secondColon < 0 ? "0" : rest.substring(secondColon + 1);
        }
        int hours;
        int minutes;
        int seconds;
        try {
            hours = Integer.parseInt(hourPart);
            minutes = Integer.parseInt(minutePart);
            seconds = Integer.parseInt(secondPart);
        } catch (NumberFormatException notCustom) {
            return null;
        }
        if (hours < 0 || hours > 23 || minutes < 0 || minutes > 59 || seconds < 0 || seconds > 59) {
            return null;
        }
        int offset = ((hours * 60 + minutes) * 60 + seconds) * 1000;
        return new SimpleTimeZone(sign == '-' ? -offset : offset, ID);
    }

    /**
     * Queries if this time zone uses Daylight Savings Time.
     */
    public abstract boolean useDaylightTime();

}
