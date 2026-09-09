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
        super(message);
        this.invalidValue = null;
        this.hasValue = false;
        this.name = null;
    }

    public ArgumentError(Object value, String name, String message) {
        super(message);
        this.invalidValue = value;
        this.hasValue = true;
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Invalid argument");
        if (name != null) {
            sb.append(" (").append(name).append(")");
        }
        if (getMessage() != null) {
            sb.append(": ").append(getMessage());
        }
        if (hasValue) {
            sb.append(": ").append(DartRuntime.str(invalidValue));
        }
        return sb.toString();
    }
}
