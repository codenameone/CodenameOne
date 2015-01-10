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
     * This is the total number of fields in this calendar.
     */
    static final int FIELD_COUNT = 17;
    
    /**
     * Field number for {@code get} and {@code set} indicating the
     * ordinal number of the day of the week within the current month. Together
     * with the {@code DAY_OF_WEEK} field, this uniquely specifies a day
     * within a month. Unlike {@code WEEK_OF_MONTH} and
     * {@code WEEK_OF_YEAR}, this field's value does <em>not</em>
     * depend on {@code getFirstDayOfWeek()} or
     * {@code getMinimalDaysInFirstWeek()}. {@code DAY_OF_MONTH 1}
     * through {@code 7} always correspond to <code>DAY_OF_WEEK_IN_MONTH
     * 1</code>;
     * {@code 8} through {@code 15} correspond to
     * {@code DAY_OF_WEEK_IN_MONTH 2}, and so on.
     * {@code DAY_OF_WEEK_IN_MONTH 0} indicates the week before
     * {@code DAY_OF_WEEK_IN_MONTH 1}. Negative values count back from
     * the end of the month, so the last Sunday of a month is specified as
     * {@code DAY_OF_WEEK = SUNDAY, DAY_OF_WEEK_IN_MONTH = -1}. Because
     * negative values count backward they will usually be aligned differently
     * within the month than positive values. For example, if a month has 31
     * days, {@code DAY_OF_WEEK_IN_MONTH -1} will overlap
     * {@code DAY_OF_WEEK_IN_MONTH 5} and the end of {@code 4}.
     *
     * @see #DAY_OF_WEEK
     * @see #WEEK_OF_MONTH
     */
    static final int DAY_OF_WEEK_IN_MONTH = 8;

    /**
     * Field number for {@code get} and {@code set} indicating the
     * day number within the current year. The first day of the year has value
     * 1.
     */
    static final int DAY_OF_YEAR = 6;
    
    /**
     * Field number for {@code get} and {@code set} indicating the
     * week number within the current month. The first week of the month, as
     * defined by {@code getFirstDayOfWeek()} and
     * {@code getMinimalDaysInFirstWeek()}, has value 1. Subclasses
     * define the value of {@code WEEK_OF_MONTH} for days before the
     * first week of the month.
     *
     * @see #getFirstDayOfWeek
     * @see #getMinimalDaysInFirstWeek
     */
    static final int WEEK_OF_MONTH = 4;
    
    /**
     * Field number for {@code get} and {@code set} indicating the
     * week number within the current year. The first week of the year, as
     * defined by {@code getFirstDayOfWeek()} and
     * {@code getMinimalDaysInFirstWeek()}, has value 1. Subclasses
     * define the value of {@code WEEK_OF_YEAR} for days before the first
     * week of the year.
     *
     * @see #getFirstDayOfWeek
     * @see #getMinimalDaysInFirstWeek
     */
    static final int WEEK_OF_YEAR = 3;
    
    /**
     * Field number for {@code get} and {@code set} indicating the
     * era, e.g., AD or BC in the Julian calendar. This is a calendar-specific
     * value; see subclass documentation.
     *
     * @see GregorianCalendar#AD
     * @see GregorianCalendar#BC
     */
    static final int ERA = 0;

    /**
     * Field number for {@code get} and {@code set} indicating the
     * raw offset from GMT in milliseconds.
     */
    static final int ZONE_OFFSET = 15;

    /**
     * Field number for {@code get} and {@code set} indicating the
     * daylight savings offset in milliseconds.
     */
    static final int DST_OFFSET = 16;

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
    
    int lastTimeFieldSet;
    
    int lastDateFieldSet;
    private int minimalDaysInFirstWeek = 5;
    

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

    
    private boolean isTimeSet;
    boolean areFieldsSet;
    private TimeZone zone;
    
    /**
     * Constructs a Calendar with the default time zone.
     * See Also:TimeZone.getDefault()
     */
    protected Calendar(){
        // default to the current time
        zone = TimeZone.getDefault();
        fields = new int[FIELD_COUNT];
        isSet = new boolean[FIELD_COUNT];
        areFieldsSet = isTimeSet = false;
        setTimeInMillis(System.currentTimeMillis());
    }

    /**
     * Compares the time field records. Equivalent to comparing result of conversion to UTC.
     */
    public boolean after(java.lang.Object calendar){
        if (!(calendar instanceof Calendar)) {
            return false;
        }
        return getTimeInMillis() > ((Calendar) calendar).getTimeInMillis();
    }

    /**
     * Compares the time field records. Equivalent to comparing result of conversion to UTC.
     */
    public boolean before(java.lang.Object calendar){
        if (!(calendar instanceof Calendar)) {
            return false;
        }
        return getTimeInMillis() < ((Calendar) calendar).getTimeInMillis();
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
    public boolean equals(java.lang.Object object){
        if (this == object) {
            return true;
        }
        if (!(object instanceof Calendar)) {
            return false;
        }
        Calendar cal = (Calendar) object;
        return getTimeInMillis() == cal.getTimeInMillis() && getTimeZone().equals(cal.getTimeZone());
    }

    /**
     * Gets the value for a given time field.
     */
    public final int get(int field){
        complete();
        return fields[field];
    }

    void complete() {
        if (!isTimeSet) {
            computeTime();
            isTimeSet = true;
        }
        if (!areFieldsSet) {
            computeFields();
            areFieldsSet = true;
        }
    }

    /**
     * Gets a calendar using the default time zone.
     */
    public static java.util.Calendar getInstance(){
        return  new GregorianCalendar();
    }

    /**
     * Gets a calendar using the specified time zone.
     */
    public static java.util.Calendar getInstance(java.util.TimeZone zone){
        return new GregorianCalendar(zone);
    }

    /**
     * Gets this Calendar's current time.
     */
    public final java.util.Date getTime(){
        return new Date(getTimeInMillis());
    }

    /**
     * Gets this Calendar's current time as a long expressed in milliseconds after January 1, 1970, 0:00:00 GMT (the epoch).
     */
    protected long getTimeInMillis(){
        if (!isTimeSet) {
            computeTime();
            isTimeSet = true;
        }
        return time;
    }

    /**
     * Gets the time zone.
     */
    public java.util.TimeZone getTimeZone(){
        return zone;
    }

    /**
     * Sets the time field with the given value.
     */
    public final void set(int field, int value){
        fields[field] = value;
        isSet[field] = true;
        areFieldsSet = isTimeSet = false;
        if (field > MONTH && field < AM_PM) {
            lastDateFieldSet = field;
        }
        if (field == HOUR || field == HOUR_OF_DAY) {
            lastTimeFieldSet = field;
        }
        if (field == AM_PM) {
            lastTimeFieldSet = HOUR;
        }
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
        addImpl(field, value);
    }
    
    abstract void addImpl(int field, int value);
    
    /**
     * Sets this Calendar's current time with the given Date.
     * Note: Calling setTime() with Date(Long.MAX_VALUE) or Date(Long.MIN_VALUE) may yield incorrect field values from get().
     */
    public final void setTime(java.util.Date date){
        setTimeInMillis(date.getTime());
    }

    /**
     * Sets this Calendar's current time from the given long value.
     */
    protected void setTimeInMillis(long milliseconds){
        if (!isTimeSet || !areFieldsSet || time != milliseconds) {
            time = milliseconds;
            isTimeSet = true;
            areFieldsSet = false;
            complete();
        }
    }

    /**
     * Sets the time zone with the given time zone value.
     */
    public void setTimeZone(java.util.TimeZone value){
        zone = value;
        areFieldsSet = false;
    }

    int getFirstDayOfWeek() {
        return SUNDAY;
    }

    /**
     * Gets the minimal days in the first week of the year.
     *
     * @return the minimal days in the first week of the year.
     */
    public int getMinimalDaysInFirstWeek() {
        return minimalDaysInFirstWeek;
    }
}

