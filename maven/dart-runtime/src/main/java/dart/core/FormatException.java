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
 * Dart's FormatException (int.parse / double.parse failures etc.).
 *
 * <p>Dart code may also declare its own format errors with {@code implements
 * FormatException}; the transpiler makes such a class extend this one, so it is caught
 * by {@code on FormatException}. Its {@code message}, {@code source} and {@code offset}
 * getters override these accessors, and {@link #getMessage()} (what {@code e.message}
 * reads) follows the override.
 */
public class FormatException extends RuntimeException {
    private final String message;
    private final Object source;
    private final Long offset;

    public FormatException() {
        this("", null, null);
    }

    public FormatException(String message) {
        this(message, null, null);
    }

    public FormatException(String message, Object source, Long offset) {
        super(message);
        this.message = message;
        this.source = source;
        this.offset = offset;
    }

    public String get$message() {
        return message;
    }

    public Object get$source() {
        return source;
    }

    public Long get$offset() {
        return offset;
    }

    // A subclass's `final String message` field surfaces as an override of get$message(),
    // an explicit `get source => ...` as an override of source(): each name delegates to
    // the other, so whichever one the subclass overrides is what every reader sees.
    public String message() {
        return get$message();
    }

    public Object source() {
        return get$source();
    }

    public Long offset() {
        return get$offset();
    }

    @Override
    public String getMessage() {
        return message();
    }

    @Override
    public String toString() {
        return "FormatException: " + getMessage();
    }
}
