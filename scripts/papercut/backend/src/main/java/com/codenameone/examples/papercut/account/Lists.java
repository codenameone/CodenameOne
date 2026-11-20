/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * Use of this source code is governed by the license in the LICENSE file beside
 * this project's pom.xml.
 */
package com.codenameone.examples.papercut.account;

import java.util.ArrayList;
import java.util.List;

/// Comma-separated id lists, the way a profile stores its owned items.
public final class Lists {
    private Lists() {
    }

    public static String join(String[] items) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(items[i]);
        }
        return sb.toString();
    }

    public static String join(List<String> items) {
        return join(items.toArray(new String[items.size()]));
    }

    public static List<String> split(String csv) {
        List<String> out = new ArrayList<String>();
        if (csv == null || csv.length() == 0) {
            return out;
        }
        int start = 0;
        for (int i = 0; i <= csv.length(); i++) {
            if (i == csv.length() || csv.charAt(i) == ',') {
                if (i > start) {
                    out.add(csv.substring(start, i));
                }
                start = i + 1;
            }
        }
        return out;
    }

    /// Like [#split], but keeps empty fields, so positions line up: a row's
    /// player list has an empty entry for each bot.
    public static List<String> fields(String csv) {
        List<String> out = new ArrayList<String>();
        if (csv == null || csv.length() == 0) {
            return out;
        }
        int start = 0;
        for (int i = 0; i <= csv.length(); i++) {
            if (i == csv.length() || csv.charAt(i) == ',') {
                out.add(csv.substring(start, i));
                start = i + 1;
            }
        }
        return out;
    }

    public static boolean contains(String csv, String id) {
        return split(csv).contains(id);
    }
}
