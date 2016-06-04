/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package java.util;
/**
 * Calendar is an abstract base class for converting between a Date object and a set of integer fields such as YEAR, MONTH, DAY, HOUR, and so on. (A Date object represents a specific instant in time with millisecond precision. See Date for information about the Date class.)
 * Subclasses of Calendar interpret a Date according to the rules of a specific calendar system.
 * Like other locale-sensitive classes, Calendar provides a class method, getInstance, for getting a generally useful object of this type.
 * A Calendar object can produce all the time field values needed to implement the date-time formatting for a particular language and calendar style (for example, Japanese-Gregorian, Japanese-Traditional).
 * When computing a Date from time fields, there may be insufficient information to compute the Date (such as only year and month but no day in the month).
 * Insufficient information. The calendar will use default information to specify the missing fields. This may vary by calendar; for the Gregorian calendar, the default for a field is the same as that of the start of the epoch: i.e., YEAR = 1970, MONTH = JANUARY, DATE = 1, etc. Note: The ambiguity in interpretation of what day midnight belongs to, is resolved as so: midnight "belongs" to the following day. 23:59 on Dec 31, 1969 00:00 on Jan 1, 1970. 12:00 PM is midday, and 12:00 AM is midnight. 11:59 PM on Jan 1 12:00 AM on Jan 2 12:01 AM on Jan 2. 11:59 AM on Mar 10 12:00 PM on Mar 10 12:01 PM on Mar 10. 24:00 or greater are invalid. Hours greater than 12 are invalid in AM/PM mode. Setting the time will never change the date.
 * If equivalent times are entered in AM/PM or 24 hour mode, equality will be determined by the actual time rather than the entered time.
 * This class has been subset for J2ME based on the JDK 1.3 Calendar class. Many methods and variables have been pruned, and other methods simplified, in an effort to reduce the size of this class.
 * Version: CLDC 1.1 02/01/2002 (based on JDK 1.3) See Also:Date, TimeZone
 */
public abstract class Calendar{
    /**
     * Value of the AM_PM field indicating the period of the day from midnight to just before noon.
     * See Also:Constant Field Values
     */
    public static final int AM=0;

    /**
     * Field number for get and set indicating whether the HOUR is before or after noon. E.g., at 10:04:15.250 PM the AM_PM is PM.
     * See Also:AM, PM, HOUR, Constant Field Values
     */
    public static final int AM_PM=9;

    /**
     * Value of the MONTH field indicating the fourth month of the year.
     * See Also:Constant Field Values
     */
    public static final int APRIL=3;

    /**
     * Value of the MONTH field indicating the eighth month of the year.
     * See Also:Constant Field Values
     */
    public static final int AUGUST=7;

    /**
     * Field number for get and set indicating the day of the month. This is a synonym for DAY_OF_MONTH.
     * See Also:DAY_OF_MONTH, Constant Field Values
     */
    public static final int DATE=5;

    /**
     * Field number for get and set indicating the day of the month. This is a synonym for DATE.
     * See Also:DATE, Constant Field Values
     */
    public static final int DAY_OF_MONTH=5;

    /**
     * Field number for get and set indicating the day of the week.
     * See Also:Constant Field Values
     */
    public static final int DAY_OF_WEEK=7;

    /**
     * Value of the MONTH field indicating the twelfth month of the year.
     * See Also:Constant Field Values
     */
    public static final int DECEMBER=11;

    /**
     * Value of the MONTH field indicating the second month of the year.
     * See Also:Constant Field Values
     */
    public static final int FEBRUARY=1;

    /**
     * The field values for the currently set time for this calendar.
     */
    protected int[] fields;

    /**
     * Value of the DAY_OF_WEEK field indicating Friday.
     * See Also:Constant Field Values
     */
    public static final int FRIDAY=6;

    /**
     * Field number for get and set indicating the hour of the morning or afternoon. HOUR is used for the 12-hour clock. E.g., at 10:04:15.250 PM the HOUR is 10.
     * See Also:AM_PM, HOUR_OF_DAY, Constant Field Values
     */
    public static final int HOUR=10;

    /**
     * Field number for get and set indicating the hour of the day. HOUR_OF_DAY is used for the 24-hour clock. E.g., at 10:04:15.250 PM the HOUR_OF_DAY is 22.
     * See Also:Constant Field Values
     */
    public static final int HOUR_OF_DAY=11;

    /**
     * The flags which tell if a specified time field for the calendar is set. This is an array of FIELD_COUNT booleans,
     */
    protected boolean[] isSet;

    /**
     * Value of the MONTH field indicating the first month of the year.
     * See Also:Constant Field Values
     */
    public static final int JANUARY=0;

    /**
     * Value of the MONTH field indicating the seventh month of the year.
     * See Also:Constant Field Values
     */
    public static final int JULY=6;

    /**
     * Value of the MONTH field indicating the sixth month of the year.
     * See Also:Constant Field Values
     */
    public static final int JUNE=5;

    /**
     * Value of the MONTH field indicating the third month of the year.
     * See Also:Constant Field Values
     */
    public static final int MARCH=2;

    /**
     * Value of the MONTH field indicating the fifth month of the year.
     * See Also:Constant Field Values
     */
    public static final int MAY=4;

    /**
     * Field number for get and set indicating the millisecond within the second. E.g., at 10:04:15.250 PM the MILLISECOND is 250.
     * See Also:Constant Field Values
     */
    public static final int MILLISECOND=14;

    /**
     * Field number for get and set indicating the minute within the hour. E.g., at 10:04:15.250 PM the MINUTE is 4.
     * See Also:Constant Field Values
     */
    public static final int MINUTE=12;

    /**
     * Value of the DAY_OF_WEEK field indicating Monday.
     * See Also:Constant Field Values
     */
    public static final int MONDAY=2;

    /**
     * Field number for get and set indicating the month. This is a calendar-specific value.
     * See Also:Constant Field Values
     */
    public static final int MONTH=2;

    /**
     * Value of the MONTH field indicating the eleventh month of the year.
     * See Also:Constant Field Values
     */
    public static final int NOVEMBER=10;

    /**
     * Value of the MONTH field indicating the tenth month of the year.
     * See Also:Constant Field Values
     */
    public static final int OCTOBER=9;

    /**
     * Value of the AM_PM field indicating the period of the day from noon to just before midnight.
     * See Also:Constant Field Values
     */
    public static final int PM=1;

    /**
     * Value of the DAY_OF_WEEK field indicating Saturday.
     * See Also:Constant Field Values
     */
    public static final int SATURDAY=7;

    /**
     * Field number for get and set indicating the second within the minute. E.g., at 10:04:15.250 PM the SECOND is 15.
     * See Also:Constant Field Values
     */
    public static final int SECOND=13;

    /**
     * Value of the MONTH field indicating the ninth month of the year.
     * See Also:Constant Field Values
     */
    public static final int SEPTEMBER=8;

    /**
     * Value of the DAY_OF_WEEK field indicating Sunday.
     * See Also:Constant Field Values
     */
    public static final int SUNDAY=1;

    /**
     * Value of the DAY_OF_WEEK field indicating Thursday.
     * See Also:Constant Field Values
     */
    public static final int THURSDAY=5;

    /**
     * The currently set time for this calendar, expressed in milliseconds after January 1, 1970, 0:00:00 GMT.
     */
    protected long time;

    /**
     * Value of the DAY_OF_WEEK field indicating Tuesday.
     * See Also:Constant Field Values
     */
    public static final int TUESDAY=3;

    /**
     * Value of the DAY_OF_WEEK field indicating Wednesday.
     * See Also:Constant Field Values
     */
    public static final int WEDNESDAY=4;

    /**
     * Field number for get and set indicating the year. This is a calendar-specific value.
     * See Also:Constant Field Values
     */
    public static final int YEAR=1;

    /**
     * Constructs a Calendar with the default time zone.
     * See Also:TimeZone.getDefault()
     */
    protected Calendar(){
         //TODO codavaj!!
    }

    /**
     * Compares the time field records. Equivalent to comparing result of conversion to UTC.
     */
    public boolean after(java.lang.Object when){
        return false; //TODO codavaj!!
    }

    /**
     * Compares the time field records. Equivalent to comparing result of conversion to UTC.
     */
    public boolean before(java.lang.Object when){
        return false; //TODO codavaj!!
    }

    /**
     * Converts the current millisecond time value time to field values in fields[]. This allows you to sync up the time field values with a new time that is set for the calendar.
     */
    protected abstract void computeFields();

    /**
     * Converts the current field values in fields[] to the millisecond time value time.
     */
    protected abstract void computeTime();

    /**
     * Compares this calendar to the specified object. The result is true if and only if the argument is not null and is a Calendar object that represents the same calendar as this object.
     */
    public boolean equals(java.lang.Object obj){
        return false; //TODO codavaj!!
    }

    /**
     * Gets the value for a given time field.
     */
    public final int get(int field){
        return 0; //TODO codavaj!!
    }

    /**
     * Gets a calendar using the default time zone.
     */
    public static java.util.Calendar getInstance(){
        return null; //TODO codavaj!!
    }

    /**
     * Gets a calendar using the specified time zone.
     */
    public static java.util.Calendar getInstance(java.util.TimeZone zone){
        return null; //TODO codavaj!!
    }

    /**
     * Gets this Calendar's current time.
     */
    public final java.util.Date getTime(){
        return null; //TODO codavaj!!
    }

    /**
     * Gets this Calendar's current time as a long expressed in milliseconds after January 1, 1970, 0:00:00 GMT (the epoch).
     */
    protected long getTimeInMillis(){
        return 0l; //TODO codavaj!!
    }

    /**
     * Gets the time zone.
     */
    public java.util.TimeZone getTimeZone(){
        return null; //TODO codavaj!!
    }

    /**
     * Sets the time field with the given value.
     */
    public final void set(int field, int value){
        return; //TODO codavaj!!
    }

    /**
     * Adds the specified amount to a {@code Calendar} field.
     * 
     * @param field
     *            the {@code Calendar} field to modify.
     * @param value
     *            the amount to add to the field.
     * @throws IllegalArgumentException
     *                if {@code field} is {@code DST_OFFSET} or {@code
     *                ZONE_OFFSET}.
     */
    public final void add(int field, int value) {
    }
    
    /**
     * Sets this Calendar's current time with the given Date.
     * Note: Calling setTime() with Date(Long.MAX_VALUE) or Date(Long.MIN_VALUE) may yield incorrect field values from get().
     */
    public final void setTime(java.util.Date date){
        return; //TODO codavaj!!
    }

    /**
     * Sets this Calendar's current time from the given long value.
     */
    protected void setTimeInMillis(long millis){
        return; //TODO codavaj!!
    }

    /**
     * Sets the time zone with the given time zone value.
     */
    public void setTimeZone(java.util.TimeZone value){
        return; //TODO codavaj!!
    }

}
