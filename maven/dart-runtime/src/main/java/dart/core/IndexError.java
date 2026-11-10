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
 * Dart's IndexError: an index was outside the valid indices of an indexable object.
 * Its text is RangeError's "index" form, and RangeError.index builds one, as Dart's
 * factory does.
 */
public class IndexError extends RangeError {
    private final Object indexable;
    private final long length;

    public IndexError(long index, Object indexable, String name, String message, Long length) {
        this(index, indexable, name, message, lengthOf(indexable, length));
    }

    private IndexError(long index, Object indexable, String name, String message, long length) {
        super("index", Long.valueOf(index), Long.valueOf(0), Long.valueOf(length - 1), name,
                message == null ? "Index out of range" : message);
        this.indexable = indexable;
        this.length = length;
    }

    /** Dart's {@code IndexError.withLength(index, length, {indexable, name, message})}. */
    public static IndexError withLength(long index, long length, Object indexable, String name, String message) {
        return new IndexError(index, indexable, name, message, Long.valueOf(length));
    }

    private static long lengthOf(Object indexable, Long length) {
        if (length != null) {
            return length.longValue();
        }
        if (indexable instanceof java.util.Collection) {
            return ((java.util.Collection<?>) indexable).size();
        }
        if (indexable instanceof String) {
            return ((String) indexable).length();
        }
        return 0;
    }

    public Object indexable() {
        return indexable;
    }

    public long length() {
        return length;
    }
}
