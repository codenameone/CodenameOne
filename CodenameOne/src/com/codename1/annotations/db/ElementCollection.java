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
package com.codename1.annotations.db;
import java.lang.annotation.*;
/// Mapping for an owned collection of scalar values.
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
@com.codename1.impl.SharedWithBackend
public @interface ElementCollection {
    /// Controls relationship initialization.
    /// @return fetch policy; EAGER for to-one and LAZY for collections by default
    FetchType fetch() default FetchType.LAZY;
    /// Names the scalar value column in the collection table.
    /// @return column name; element_value by default
    String column() default "element_value";
    /// Names the column that stores each element's position, which keys the
    /// table together with the owner and keeps a List's order and duplicates.
    ///
    /// An empty name maps a `Set` to a table with no position column, keyed by
    /// the owner and the value -- the layout JPA providers create, so a table
    /// one of them made can be adopted as it is. Such a Set cannot hold null,
    /// and a text value is limited to 255 characters, like any text key.
    /// @return column name; element_position by default, empty for none
    String positionColumn() default "element_position";
}
