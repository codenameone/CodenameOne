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
package com.codename1.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Renames or constrains an `@Entity` field's column. Optional -- when absent,
/// the column name defaults to the field name and the type is inferred from
/// the Java field type (`String -> TEXT`, `int/long -> INTEGER`,
/// `float/double -> REAL`, `boolean -> INTEGER`, `byte[] -> BLOB`,
/// `java.util.Date -> INTEGER`).
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface Column {
    /// Column name. Defaults to the field name when blank.
    String name() default "";

    /// When false the column gets a `NOT NULL` constraint at table-create time.
    boolean nullable() default true;

    /// Optional explicit SQL type. Use the SQLite type names (`TEXT`,
    /// `INTEGER`, `REAL`, `BLOB`, `NUMERIC`). When blank the processor infers
    /// the type from the field's Java type.
    String type() default "";
}
