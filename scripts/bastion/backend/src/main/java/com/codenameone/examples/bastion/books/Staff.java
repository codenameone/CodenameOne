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
package com.codenameone.examples.bastion.books;

import com.codenameone.examples.bastion.Caller;
import com.codenameone.examples.bastion.Refused;
import com.codenameone.examples.bastion.Roles;
import com.codenameone.examples.bastion.audit.Audit;

import java.io.IOException;

/// Who may read the books, and the record that they did.
///
/// The API chain already limits `/api/books/**` and `/api/audit/**` to ADMIN
/// and AUDITOR; each endpoint asks again here, as the admin endpoints do,
/// because a rule is a line in another file.
public final class Staff {
    private Staff() {
    }

    /// The caller, when they are an ADMIN or an AUDITOR; 403 otherwise.
    public static String name() {
        String name = Caller.name();
        if (!Caller.hasRole(Roles.ADMIN) && !Caller.hasRole(Roles.AUDITOR)) {
            throw Refused.with(403, "staff-only", "Staff only");
        }
        return name;
    }

    /// The caller, when they are an ADMIN -- an auditor reads and never
    /// operates, which is the point of having one.
    public static String admin() {
        String name = Caller.name();
        if (!Caller.hasRole(Roles.ADMIN)) {
            throw Refused.with(403, "administrators", "Administrators only");
        }
        return name;
    }

    /// [#name], with the read appended to the audit chain before anything
    /// is answered: what was asked, by whom, from where. A read that cannot
    /// be recorded is not served.
    public static String read(Audit audit, String what, String detail) throws IOException {
        String who = name();
        audit.append(who, "STAFF_READ", what, detail == null ? "" : detail);
        return who;
    }

    /// An ISO-8601 UTC timestamp, for exports a spreadsheet reads.
    public static String iso(long ms) {
        long days = Books.day(ms) / Books.DAY;
        long rest = ms - days * Books.DAY;
        // Civil from days (Howard Hinnant's algorithm), proleptic Gregorian.
        long z = days + 719468;
        long era = (z >= 0 ? z : z - 146096) / 146097;
        long doe = z - era * 146097;
        long yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365;
        long y = yoe + era * 400;
        long doy = doe - (365 * yoe + yoe / 4 - yoe / 100);
        long mp = (5 * doy + 2) / 153;
        long d = doy - (153 * mp + 2) / 5 + 1;
        long m = mp < 10 ? mp + 3 : mp - 9;
        if (m <= 2) {
            y++;
        }
        StringBuilder b = new StringBuilder();
        b.append(y).append('-').append(two(m)).append('-').append(two(d)).append('T')
                .append(two(rest / 3600000)).append(':').append(two(rest / 60000 % 60))
                .append(':').append(two(rest / 1000 % 60)).append('.');
        long millis = rest % 1000;
        b.append(millis < 100 ? "0" : "").append(millis < 10 ? "0" : "").append(millis)
                .append('Z');
        return b.toString();
    }

    private static String two(long v) {
        return v < 10 ? "0" + v : Long.toString(v);
    }
}
