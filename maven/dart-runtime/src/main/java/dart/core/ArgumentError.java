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

import dart.runtime.DartRuntime;

/**
 * Dart's ArgumentError.
 */
public class ArgumentError extends RuntimeException {
    private final Object invalidValue;
    private final boolean hasValue;
    private final String name;

    public ArgumentError(String message) {
        this(null, false, null, message);
    }

    /// Dart's `ArgumentError([message, name])`: no value is to blame.
    public ArgumentError(String message, String name) {
        this(null, false, name, message);
    }

    public ArgumentError(Object value, String name, String message) {
        this(value, true, name, message);
    }

    protected ArgumentError(Object value, boolean hasValue, String name, String message) {
        super(message);
        this.invalidValue = value;
        this.hasValue = hasValue;
        this.name = name;
    }

    public static ArgumentError value(Object value, String name, String message) {
        return new ArgumentError(value, name, message);
    }

    public Object invalidValue() {
        return invalidValue;
    }

    public String name() {
        return name;
    }

    /// Dart's `message` getter: the message the error was built with, not its text.
    public String dartMessage() {
        return getMessage();
    }

    /// The text Dart's toString starts with.
    protected String errorName() {
        return hasValue ? "Invalid argument" : "Invalid argument(s)";
    }

    /// What Dart's toString puts between the message and the value.
    protected String errorExplanation() {
        return "";
    }

    @Override
    public String toString() {
        // Dart's ArgumentError.toString, which RangeError and IndexError share.
        String prefix = errorName() + (name == null ? "" : " (" + name + ")")
                + (getMessage() == null ? "" : ": " + getMessage());
        if (!hasValue) {
            return prefix;
        }
        return prefix + errorExplanation() + ": " + DartRuntime.str(invalidValue);
    }
}
