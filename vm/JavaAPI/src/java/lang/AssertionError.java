/*
 * Copyright (c) 2015, Codename One and/or its affiliates. All rights reserved.
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
package java.lang;

public class AssertionError extends Error {
    // Every constructor used to have an empty body, so the message -- the one
    // thing an assertion failure says -- was dropped on every translated target,
    // and a failing assert reported only its class. The JDK's rules: an Object
    // message is its String value, and a Throwable one is also the cause.
    public AssertionError() {
        super();
    }

    public AssertionError(String detailMessage) {
        super(detailMessage);
    }

    // A null Object becomes the message "null", not a null message: measured on
    // JDK 8 and JDK 21, whose constructor is this(String.valueOf(detailMessage)).
    public AssertionError(Object detailMessage) {
        super(String.valueOf(detailMessage));
        if (detailMessage instanceof Throwable) {
            initCause((Throwable) detailMessage);
        }
    }

    public AssertionError(boolean detailMessage) {
        super(String.valueOf(detailMessage));
    }

    public AssertionError(char detailMessage) {
        super(String.valueOf(detailMessage));
    }

    public AssertionError(int detailMessage) {
        super(String.valueOf(detailMessage));
    }

    public AssertionError(long detailMessage) {
        super(String.valueOf(detailMessage));
    }

    public AssertionError(float detailMessage) {
        super(String.valueOf(detailMessage));
    }

    public AssertionError(double detailMessage) {
        super(String.valueOf(detailMessage));
    }

    public AssertionError(String message, Throwable cause) {
        super(message, cause);
    }
}
