/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * Use of this source code is governed by the license in the LICENSE file beside
 * this project's pom.xml.
 */
package com.codenameone.examples.papercut.run;

import com.codename1.backend.ResponseStatusException;

/// The three ways to play, and the periods a board covers.
public final class Modes {
    public static final String CLASSIC = "CLASSIC";
    public static final String FOLD = "FOLD";
    public static final String DAILY = "DAILY";

    public static final String ALL = "ALL";
    public static final String WEEK = "WEEK";
    public static final String TODAY = "TODAY";
    public static final String FRIENDS = "FRIENDS";

    private Modes() {
    }

    /// `value` as one of the modes, compared without folding case by locale.
    public static String mode(String value) {
        String[] all = {CLASSIC, FOLD, DAILY};
        for (String m : all) {
            if (m.equalsIgnoreCase(value)) {
                return m;
            }
        }
        throw new ResponseStatusException(400, "Mode is CLASSIC, FOLD or DAILY");
    }

    public static String period(String value) {
        if (value == null || value.length() == 0) {
            return ALL;
        }
        String[] all = {ALL, WEEK, TODAY, FRIENDS};
        for (String p : all) {
            if (p.equalsIgnoreCase(value)) {
                return p;
            }
        }
        throw new ResponseStatusException(400, "Period is ALL, WEEK, TODAY or FRIENDS");
    }
}
