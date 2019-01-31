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
package com.codename1.util;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Utility class for working with dates and timezones.
 * @author shannah
 */
public class DateUtil {
    private final TimeZone tz;
    
    /**
     * Constructor for timezone.
     * @param tz Timezone
     */
    public DateUtil(TimeZone tz) {
        this.tz = tz;
    }
    
    /**
     * Returns the earliest of a set of dates.
     * @param dates
     * @return The earliest of a set of dates.
     * @since 6.0
     */
    public static Date min(Date... dates) {
        int len = dates.length;
        if (len == 0) return null;
        Date out = null;
        for (int i=0; i<len; i++) {
            if (dates[i] == null) {
                continue;
            }
            if (out == null || dates[i].getTime() < out.getTime()) {
                out = dates[i];
            }
        }
        return out;
    }
    
    /**
     * Compares two dates.
     * @param d1 A date
     * @param d2 A date
     * @return -1 if first date is earlier.  1 if first date is later.  0 if they are the same.
     * @since 6.0
     */
    public static int compare(Date d1, Date d2) {
        if (d1 == null) return d2 == null ? 0 : -1;
        if (d2 == null) return 1;
        if (d1.getTime() < d2.getTime()) {
            return -1;
        } else if (d1.getTime() > d2.getTime()) {
            return 1;
        }
        return 0;
    }
    
    /**
     * Returns the latest of a set of dates.
     * @param dates
     * @return The latest of a set of dates.
     * @since 6.0
     */
    public static Date max(Date... dates) {
        int len = dates.length;
        if (len == 0) return null;
        Date out = null;
        for (int i=0; i<len; i++) {
            if (dates[i] == null) {
                continue;
            }
            if (out == null || dates[i].getTime() > out.getTime()) {
                out = dates[i];
            }
        }
        return out;
    }
    
    
    /**
     * Creates DateUtil object in default timezone.
     */
    public DateUtil() {
        this(TimeZone.getDefault());
    }
    
    /**
     * Gets the offset from GMT in milliseconds for the given date.  
     * @param date The date at which the offset is calculated.
     * @return Millisecond offset from GMT in the current timezone for the given date.
     */
    public int getOffset(long date) {
        Calendar cal = Calendar.getInstance(tz);
        cal.setTime(new Date(date));
        Calendar calStartOfDay = Calendar.getInstance(tz);
        calStartOfDay.setTime(new Date(date));
        calStartOfDay.set(Calendar.MILLISECOND, 0);
        calStartOfDay.set(Calendar.SECOND, 0);
        calStartOfDay.set(Calendar.HOUR_OF_DAY, 0);
        calStartOfDay.set(Calendar.MINUTE, 0);
        
        
        return tz.getOffset(1, 
                cal.get(Calendar.YEAR), 
                cal.get(Calendar.MONTH), 
                cal.get(Calendar.DAY_OF_MONTH), 
                cal.get(Calendar.DAY_OF_WEEK), 
                (int)(cal.getTime().getTime() - calStartOfDay.getTime().getTime())
        );
    }
    
    /**
     * Checks whether the given date is in daylight savings time for the given date.
     * @param date
     * @return 
     */
    public boolean inDaylightTime(Date date) {
        return tz.useDaylightTime() && getOffset(date.getTime()) != tz.getRawOffset();
    }
    
    /**
     * Gets the date in "time ago" format.  E.g. "Just now", or "1 day ago", etc..
     * @param date The date
     * @return String representing how long ago from now the given date is.
     * @since 6.0
     */
    public String getTimeAgo(Date date) {
        if (date == null) {
            return "N/A";
        }
        long time_ago = date.getTime() / 1000l;
        
        long cur_time =  new Date().getTime() / 1000l;
        long time_elapsed = cur_time - time_ago;
        long seconds = time_elapsed;
        // Seconds
        if (seconds <= 60) {
            return "Just now";
        } //Minutes
        else {
            int minutes = Math.round(time_elapsed / 60);

            if (minutes <= 60) {
                if (minutes == 1) {
                    return "a minute ago";
                } else {
                    return minutes + " minutes ago";
                }
            } //Hours
            else {
                int hours = Math.round(time_elapsed / 3600);
                if (hours <= 24) {
                    if (hours == 1) {
                        return "An hour ago";
                    } else {
                        return hours + " hrs ago";
                    }
                } //Days
                else {
                    int days = Math.round(time_elapsed / 86400);
                    if (days <= 7) {
                        if (days == 1) {
                            return "Yesterday";
                        } else {
                            return days + " days ago";
                        }
                    } //Weeks
                    else {
                        int weeks = Math.round(time_elapsed / 604800);
                        if (weeks <= 4.3) {
                            if (weeks == 1) {
                                return "A week ago";
                            } else {
                                return weeks + " weeks ago";
                            }
                        } //Months
                        else {
                            int months = Math.round(time_elapsed / 2600640);
                            if (months <= 12) {
                                if (months == 1) {
                                    return "A month ago";
                                } else {
                                    return months + " months ago";
                                }
                            } //Years
                            else {
                                int years = Math.round(time_elapsed / 31207680);
                                if (years == 1) {
                                    return "One year ago";
                                } else {
                                    return years + " years ago";
                                }
                            }
                        }
                    }
                }
            }
        }

    }
    
}
