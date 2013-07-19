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
package net.sourceforge.retroweaver.runtime.java.util;

import java.util.Date;

/**
 *
 * @author Shai Almog
 */
public class Calendar_ {
    public static void add(java.util.Calendar c, int field, int value) {
        long mil = c.getTime().getTime();
        switch(field) {
            case java.util.Calendar.DATE:
            case java.util.Calendar.DAY_OF_YEAR:
                mil += (value * 1000 * 60 * 60 * 24);
                break;
            case java.util.Calendar.MILLISECOND:
                mil += value;
                break;
            case java.util.Calendar.MINUTE:
                mil += (value * 1000 * 60);
                break;
            case java.util.Calendar.HOUR:
                mil += (value * 1000 * 60 * 60);
                break;
            case java.util.Calendar.MONTH:
                int years = value / 12;
                int months = value % 12;
                int currentMonth = c.get(java.util.Calendar.MONTH) + months;
                if(currentMonth > java.util.Calendar.DECEMBER) {
                    years++;
                    currentMonth -= java.util.Calendar.DECEMBER;
                } else {
                    if(currentMonth < 0) {
                        years--;
                        currentMonth += java.util.Calendar.DECEMBER;
                    }
                }
                c.add(java.util.Calendar.YEAR, c.get(java.util.Calendar.YEAR) + years);
                c.add(java.util.Calendar.MONTH, currentMonth);
                return;
            case java.util.Calendar.YEAR:
                c.add(java.util.Calendar.YEAR, c.get(java.util.Calendar.YEAR) + value);
                return;
            default:
                throw new IllegalArgumentException("Invalid field type: " + field);
        }
        c.setTime(new Date(mil));
    }
}
