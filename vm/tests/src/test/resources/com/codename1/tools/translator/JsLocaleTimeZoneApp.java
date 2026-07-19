/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class JsLocaleTimeZoneApp {
    public static int result;

    public static void main(String[] args) {
        int score = 0;

        Locale locale = Locale.getDefault();
        if (locale != null) {
            score |= 1;
        }
        if (locale != null && locale.getLanguage() != null && locale.getLanguage().length() > 0) {
            score |= 2;
        }
        if (locale != null && locale.getCountry() != null && locale.getCountry().length() > 0) {
            score |= 4;
        }

        TimeZone timeZone = TimeZone.getDefault();
        if (timeZone != null && timeZone.getID() != null && timeZone.getID().length() > 0) {
            score |= 8;
        }
        String[] ids = TimeZone.getAvailableIDs();
        if (ids != null && ids.length >= 1) {
            score |= 16;
        }

        int rawOffset = timeZone.getRawOffset();
        if (rawOffset >= -43200000 && rawOffset <= 50400000) {
            score |= 32;
        }

        int offset = timeZone.getOffset(1, 2024, 0, 15, 2, 12 * 60 * 60 * 1000);
        if (offset >= -43200000 && offset <= 50400000) {
            score |= 64;
        }

        Date sample = new Date(1704067200000L);
        String formatted = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(sample);
        if (formatted != null && formatted.length() > 0) {
            score |= 128;
        }

        String formattedDate = DateFormat.getDateInstance(DateFormat.SHORT).format(sample);
        if (formattedDate != null && formattedDate.length() > 0) {
            score |= 256;
        }

        TimeZone west = TimeZone.getTimeZone("GMT-05:00");
        if (west.getRawOffset() == -5 * 60 * 60 * 1000) {
            score |= 512;
        }
        if (west.getOffset(1, 2024, 0, 15, 2, 12 * 60 * 60 * 1000)
                == -5 * 60 * 60 * 1000) {
            score |= 1024;
        }

        result = score;
    }
}
