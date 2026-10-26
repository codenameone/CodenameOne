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
package androidx.room;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Names and types a column.
///
/// Kept in the class files: the Room step of the Android remap reads it there
/// and writes the code AndroidX Room's annotation processor would.
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.CLASS)
public @interface ColumnInfo {
    String INHERIT_FIELD_NAME = "[field-name]";
    int UNDEFINED = 1;
    int TEXT = 2;
    int INTEGER = 3;
    int REAL = 4;
    int BLOB = 5;
    int UNSPECIFIED = 1;
    int BINARY = 2;
    int NOCASE = 3;
    int RTRIM = 4;
    int LOCALIZED = 5;
    int UNICODE = 6;
    String VALUE_UNSPECIFIED = "[value-unspecified]";

    String name() default INHERIT_FIELD_NAME;
    int typeAffinity() default UNDEFINED;
    boolean index() default false;
    int collate() default UNSPECIFIED;
    String defaultValue() default VALUE_UNSPECIFIED;
}
