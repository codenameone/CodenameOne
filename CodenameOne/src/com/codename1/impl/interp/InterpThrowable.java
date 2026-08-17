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
package com.codename1.impl.interp;

/// Carries a throwable raised by interpreted code across real Java frames.
///
/// Interpreted code can throw an interpreted object -- an instance of a user
/// class extending `Exception` -- which is not a `java.lang.Throwable` and so
/// cannot be thrown directly. This wraps it, along with the interpreted stack
/// at the point it was raised, since a real stack trace would show the
/// interpreter's frames rather than the user's source lines.
///
/// @author Shai Almog
public final class InterpThrowable extends RuntimeException {
    private final Object thrown;
    private final String[] interpretedStack;

    InterpThrowable(Object thrown, String[] interpretedStack) {
        super(describe(thrown));
        this.thrown = thrown;
        this.interpretedStack = interpretedStack == null ? new String[0] : interpretedStack;
    }

    private static String describe(Object thrown) {
        if (thrown instanceof Throwable) {
            Throwable t = (Throwable) thrown;
            String msg = t.getMessage();
            return t.getClass().getName() + (msg == null ? "" : ": " + msg);
        }
        if (thrown instanceof InterpObject) {
            return ((InterpObject) thrown).getType().getName().replace('/', '.');
        }
        return String.valueOf(thrown);
    }

    /// The object that was thrown: a real `Throwable` when it came from the
    /// host, an [InterpObject] when interpreted code threw one of its own.
    public Object getThrown() {
        return thrown;
    }

    /// The interpreted call stack at the throw, innermost first, formatted as
    /// `Class.method(File:line)`.
    public String[] getInterpretedStack() {
        return interpretedStack;
    }

    /// The interpreted stack rendered the way a Java stack trace reads, so it
    /// can be shown on device or sent to the desktop for the IDE to linkify.
    public String getInterpretedStackTrace() {
        StringBuffer sb = new StringBuffer(getMessage());
        for (String frame : interpretedStack) {
            sb.append("\n\tat ").append(frame);
        }
        return sb.toString();
    }
}
