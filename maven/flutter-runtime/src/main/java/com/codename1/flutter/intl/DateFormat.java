/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.intl;

import com.codename1.l10n.SimpleDateFormat;
import dart.core.DateTime;

/**
 * A subset of {@code package:intl}'s DateFormat backed by CN1's
 * {@link SimpleDateFormat}. The named "skeleton" constructors map to concrete
 * patterns; {@code add_jm}/{@code add_jms} append a time component.
 */
public final class DateFormat {

    /**
     * Skeleton "field" constants from {@code package:intl}'s DateFormat, used
     * bare as a pattern, e.g. {@code DateFormat(DateFormat.WEEKDAY, locale)}.
     * Their values are ICU/{@link SimpleDateFormat}-compatible pattern strings.
     */
    public static final String WEEKDAY = "EEEE";
    public static final String MMM = "MMM";

    private String pattern;

    public DateFormat(String pattern, String locale) {
        this.pattern = pattern == null ? "M/d/yyyy" : pattern;
    }

    private static DateFormat of(String pattern) {
        return new DateFormat(pattern, null);
    }

    public static DateFormat MMMd(String locale) {
        return of("MMM d");
    }

    public static DateFormat jm(String locale) {
        return of("h:mm a");
    }

    public static DateFormat Hm(String locale) {
        return of("HH:mm");
    }

    public static DateFormat yMMM(String locale) {
        return of("MMM yyyy");
    }

    public static DateFormat yMMMMd(String locale) {
        return of("MMMM d, yyyy");
    }

    public static DateFormat yMMMd(String locale) {
        return of("MMM d, yyyy");
    }

    public static DateFormat yMd(String locale) {
        return of("M/d/yyyy");
    }

    public DateFormat add_jm() {
        pattern = pattern + " h:mm a";
        return this;
    }

    public DateFormat add_jms() {
        pattern = pattern + " h:mm:ss a";
        return this;
    }

    public String format(DateTime date) {
        if (date == null) {
            return "";
        }
        if (date.isUtc()) {
            // SimpleDateFormat always formats in the device's zone, which shifted a UTC
            // value's hour by the local offset. The same wall-clock fields as a LOCAL
            // value format as themselves.
            date = new DateTime(date.year(), date.month(), date.day(), date.hour(),
                    date.minute(), date.second(), date.millisecond(), date.microsecond());
        }
        try {
            return new SimpleDateFormat(pattern).format(date.toJavaDate());
        } catch (Throwable t) {
            return date.toJavaDate().toString();
        }
    }
}
