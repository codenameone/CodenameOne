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
package com.codename1.impl.orm;

import com.codename1.orm.session.PersistenceException;

import java.io.IOException;
import java.util.Date;

@com.codename1.impl.SharedWithBackend
final class Elements {
    private Elements() {
    }
    static int kind(Class type) {
        if (type == String.class) {
            return Attribute.TEXT;
        }
        if (type == Double.class || type == Float.class) {
            return Attribute.REAL;
        }
        return Attribute.BIGINT;
    }
    static Object read(Class type, Object value) {
        try {
            if (type == String.class) {
                return Values.asString(value);
            }
            if (type == Integer.class) {
                return Values.asIntObject(value);
            }
            if (type == Long.class) {
                return Values.asLongObject(value);
            }
            if (type == Short.class) {
                return Values.asShortObject(value);
            }
            if (type == Byte.class) {
                return Values.asByteObject(value);
            }
            if (type == Double.class) {
                return Values.asDoubleObject(value);
            }
            if (type == Float.class) {
                return Values.asFloatObject(value);
            }
            if (type == Boolean.class) {
                return Values.asBooleanObject(value);
            }
            if (type == Character.class) {
                return Values.asCodeUnitObject(value);
            }
            if (type == Date.class) {
                return Values.asDate(value);
            }
            throw new PersistenceException("Unsupported element type: " + type.getName());
        } catch (IOException error) {
            throw new PersistenceException(error.getMessage(), error);
        }
    }
}
