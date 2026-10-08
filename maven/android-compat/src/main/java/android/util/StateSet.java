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
package android.util;

/// Matching of drawable state sets: a spec matches when every positive state
/// in it is present and no negated (negative id) state is.
public final class StateSet {

    public static final int[] WILD_CARD = new int[0];
    public static final int[] NOTHING = new int[] {0};

    private StateSet() {
    }

    public static boolean isWildCard(int[] stateSetOrSpec) {
        return stateSetOrSpec.length == 0 || stateSetOrSpec[0] == 0;
    }

    public static boolean stateSetMatches(int[] stateSpec, int[] stateSet) {
        if (stateSet == null) {
            return stateSpec == null || isWildCard(stateSpec);
        }
        for (int spec : stateSpec) {
            if (spec == 0) {
                return true;
            }
            boolean mustMatch = spec > 0;
            int s = mustMatch ? spec : -spec;
            boolean found = false;
            for (int st : stateSet) {
                if (st == 0) {
                    break;
                }
                if (st == s) {
                    found = true;
                    break;
                }
            }
            if (found != mustMatch) {
                return false;
            }
        }
        return true;
    }

    public static boolean stateSetMatches(int[] stateSpec, int state) {
        return stateSetMatches(stateSpec, new int[] {state});
    }

    public static int[] trimStateSet(int[] states, int newSize) {
        if (states.length == newSize) {
            return states;
        }
        int[] r = new int[newSize];
        System.arraycopy(states, 0, r, 0, newSize);
        return r;
    }
}
