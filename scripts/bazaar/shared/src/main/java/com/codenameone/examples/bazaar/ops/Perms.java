/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bazaar.ops;

/// Granular staff permissions: per area, none, read or write.
///
/// A permission set is stored as its grants, `orders=W;products=R`, one
/// entry per area that is not NONE. An area missing from the text is NONE,
/// and unknown areas or levels are ignored, so a set written by a newer
/// version reads safely.
///
/// [#defaults] maps each legacy role to the set it amounts to. A staff
/// member with no set assigned is still judged by the role exactly as
/// before; the defaults are what the editor starts a new set from and what
/// "effective permissions" shows.
public final class Perms {
    public static final String ORDERS = "orders";
    public static final String PRODUCTS = "products";
    public static final String INVENTORY = "inventory";
    public static final String CUSTOMERS = "customers";
    public static final String MARKETING = "marketing";
    public static final String REPORTS = "reports";
    public static final String SETTINGS = "settings";
    public static final String POS = "pos";
    /// Every area, in the editor's order.
    public static final String[] AREAS = {ORDERS, PRODUCTS, INVENTORY, CUSTOMERS, MARKETING,
        REPORTS, SETTINGS, POS};
    public static final char NONE = 'N';
    public static final char READ = 'R';
    public static final char WRITE = 'W';

    private Perms() {
    }

    /// Whether `area` is one of [#AREAS].
    public static boolean isArea(String area) {
        for (int iter = 0; iter < AREAS.length; iter++) {
            if (AREAS[iter].equals(area)) {
                return true;
            }
        }
        return false;
    }

    /// The level `grants` gives `area`.
    public static char level(String grants, String area) {
        if (grants == null || area == null) {
            return NONE;
        }
        int start = 0;
        while (start < grants.length()) {
            int end = grants.indexOf(';', start);
            if (end < 0) {
                end = grants.length();
            }
            int eq = grants.indexOf('=', start);
            if (eq > start && eq < end - 1 && grants.substring(start, eq).trim().equals(area)) {
                char c = grants.charAt(eq + 1);
                return c == WRITE || c == READ ? c : NONE;
            }
            start = end + 1;
        }
        return NONE;
    }

    /// Whether `grants` allows reading (or, when `write`, changing) `area`.
    public static boolean allows(String grants, String area, boolean write) {
        char l = level(grants, area);
        return write ? l == WRITE : l == WRITE || l == READ;
    }

    /// Grants text for levels in [#AREAS] order; anything not R or W is NONE.
    public static String format(char[] levels) {
        StringBuilder sb = new StringBuilder();
        for (int iter = 0; iter < AREAS.length && levels != null && iter < levels.length; iter++) {
            char c = levels[iter];
            if (c == READ || c == WRITE) {
                if (sb.length() > 0) {
                    sb.append(';');
                }
                sb.append(AREAS[iter]).append('=').append(c);
            }
        }
        return sb.toString();
    }

    /// The same grants, cleaned: known areas only, in order.
    public static String normalize(String grants) {
        char[] l = new char[AREAS.length];
        for (int iter = 0; iter < AREAS.length; iter++) {
            l[iter] = level(grants, AREAS[iter]);
        }
        return format(l);
    }

    /// The set a legacy role amounts to: OWNER and MANAGER write everything;
    /// FULFILMENT writes orders and inventory and reads products; CASHIER
    /// works the register, reads products and customers.
    public static String defaults(String role) {
        if ("OWNER".equals(role) || "MANAGER".equals(role)) {
            return format(new char[] {WRITE, WRITE, WRITE, WRITE, WRITE, WRITE, WRITE, WRITE});
        }
        if ("FULFILMENT".equals(role)) {
            return format(new char[] {WRITE, READ, WRITE, NONE, NONE, NONE, NONE, NONE});
        }
        if ("CASHIER".equals(role)) {
            return format(new char[] {READ, READ, NONE, READ, NONE, NONE, NONE, WRITE});
        }
        return "";
    }
}
