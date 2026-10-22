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
package dart.async;

/// `dart:async`'s TimeoutException: an asynchronous result did not arrive within
/// the time allowed. `duration` is the limit that expired, or null.
public class TimeoutException extends RuntimeException {
    private final Object duration;

    public TimeoutException(String message, Object duration) {
        super(message);
        this.duration = duration;
    }

    public String message() {
        return getMessage();
    }

    public Object duration() {
        return duration;
    }

    @Override
    public String toString() {
        String m = getMessage();
        return m == null ? "TimeoutException" : "TimeoutException: " + m;
    }
}
