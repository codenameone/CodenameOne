/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
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
 * Please contact Codename One through http://www.codenameone.com/ if you 
 * need additional information or have any questions.
 */

package java.util;

import java.text.DateFormat;

/**
 * The class Date represents a specific instant in time, with millisecond precision.
 * This class has been subset for the J2ME based on the JDK 1.3 Date class. Many methods and variables have been pruned, and other methods simplified, in an effort to reduce the size of this class.
 * Although the Date class is intended to reflect coordinated universal time (UTC), it may not do so exactly, depending on the host environment of the Java Virtual Machine. Nearly all modern operating systems assume that 1 day = 24x60x60 = 86400 seconds in all cases. In UTC, however, about once every year or two there is an extra second, called a "leap second." The leap second is always added as the last second of the day, and always on December 31 or June 30. For example, the last minute of the year 1995 was 61 seconds long, thanks to an added leap second. Most computer clocks are not accurate enough to be able to reflect the leap-second distinction.
 * Version: CLDC 1.1 03/13/2002 (Based on JDK 1.3) See Also:TimeZone, Calendar
 */
public class Date{
    private long date;
    /**
     * Allocates a Date object and initializes it to represent the current time specified number of milliseconds since the standard base time known as "the epoch", namely January 1, 1970, 00:00:00 GMT.
     * See Also:System.currentTimeMillis()
     */
    public Date(){
        this(System.currentTimeMillis());
    }

    /**
     * Allocates a Date object and initializes it to represent the specified number of milliseconds since the standard base time known as "the epoch", namely January 1, 1970, 00:00:00 GMT.
     * Parameters:date - the milliseconds since January 1, 1970, 00:00:00 GMT.See Also:System.currentTimeMillis()
     */
    public Date(long date){
         this.date = date;
    }

    /**
     * Compares two dates for equality. The result is true if and only if the argument is not null and is a Date object that represents the same point in time, to the millisecond, as this object.
     * Thus, two Date objects are equal if and only if the getTime method returns the same long value for both.
     */
    public boolean equals(java.lang.Object obj){
        return obj instanceof Date && ((Date)obj).date == date; 
    }

    /**
     * Returns the number of milliseconds since January 1, 1970, 00:00:00 GMT represented by this Date object.
     */
    public long getTime(){
        return date;
    }

    /**
     * Returns a hash code value for this object. The result is the exclusive OR of the two halves of the primitive long value returned by the
     * method. That is, the hash code is the value of the expression: (int)(this.getTime()^(this.getTime() >>> 32))
     */
    public int hashCode(){
        return (int)date; 
    }

    /**
     * Sets this Date object to represent a point in time that is time milliseconds after January 1, 1970 00:00:00 GMT.
     */
    public void setTime(long time){
        this.date = time;
    }

    /**
     * Converts this Date object to a String of the form: dow mon dd hh:mm:ss zzz yyyy where: dow is the day of the week (Sun, Mon, Tue, Wed, Thu, Fri, Sat). mon is the month (Jan, Feb, Mar, Apr, May, Jun, Jul, Aug, Sep, Oct, Nov, Dec). dd is the day of the month (01 through 31), as two decimal digits. hh is the hour of the day (00 through 23), as two decimal digits. mm is the minute within the hour (00 through 59), as two decimal digits. ss is the second within the minute (00 through 61, as two decimal digits. zzz is the time zone (and may reflect daylight savings time). If time zone information is not available, then zzz is empty - that is, it consists of no characters at all. yyyy is the year, as four decimal digits.
     */
    public java.lang.String toString(){
        return DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG).format(this);
    }

}
