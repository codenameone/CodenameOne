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
    
    
}
