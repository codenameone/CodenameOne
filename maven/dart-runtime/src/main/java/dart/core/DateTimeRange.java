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
package dart.core;

/**
 * Dart's {@code DateTimeRange}: an inclusive-start, inclusive-end pair of
 * {@link DateTime} instants used by the Material date-range picker.
 */
public final class DateTimeRange {

    private DateTime start;
    private DateTime end;

    /** Named-parameter constructor {@code DateTimeRange({start, end})}. */
    public DateTimeRange(DateTime start, DateTime end) {
        this.start = start;
        this.end = end;
    }

    public DateTime start() {
        return start;
    }

    public DateTime end() {
        return end;
    }

    public Duration duration() {
        return end.difference(start);
    }

    /**
     * Flutter's {@code DateTimeRange ==}: equal endpoints. Inherited identity made a
     * rebuilt, unchanged range differ from the one a controlled date-range picker
     * already held, and two equal ranges separate keys of a map or set.
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DateTimeRange)) {
            return false;
        }
        DateTimeRange r = (DateTimeRange) o;
        return (start == null ? r.start == null : start.equals(r.start))
                && (end == null ? r.end == null : end.equals(r.end));
    }

    @Override
    public int hashCode() {
        return 31 * (start == null ? 0 : start.hashCode()) + (end == null ? 0 : end.hashCode());
    }
}
