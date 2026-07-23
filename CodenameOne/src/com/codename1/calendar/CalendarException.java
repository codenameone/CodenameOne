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
package com.codename1.calendar;

/// Exception returned by asynchronous calendar operations.
public class CalendarException extends Exception {

    private final CalendarError error;

    private final int responseCode;

    private final String responseBody;

    public CalendarException(CalendarError error, String message) {
        this(error, message, 0, null, null);
    }

    public CalendarException(CalendarError error, String message, Throwable cause) {
        this(error, message, 0, null, cause);
    }

    public CalendarException(CalendarError error, String message, int responseCode, Throwable cause) {
        this(error, message, responseCode, null, cause);
    }

    public CalendarException(CalendarError error, String message, int responseCode,
            String responseBody, Throwable cause) {
        super(message, cause);
        this.error = error == null ? CalendarError.UNKNOWN : error;
        this.responseCode = responseCode;
        this.responseBody = responseBody;
    }

    public CalendarError getError() {
        return error;
    }

    public int getResponseCode() {
        return responseCode;
    }

    /// Returns the provider response body for an HTTP failure, if one was
    /// available. This is kept separate from the user-facing exception message.
    public String getResponseBody() {
        return responseBody;
    }
}
