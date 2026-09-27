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
/// Database index over mapped field names, declared on an entity.
@Retention(RetentionPolicy.CLASS)
@Target({})
@com.codename1.impl.SharedWithBackend
public @interface Index {
    /// Names the database index.
    /// @return index name; empty by default to request a derived name
    String name() default "";
    /// Lists Java attribute names in index order, not SQL column names.
    /// @return indexed fields
    String[] fields();
    /// Requires indexed value combinations to be unique.
    /// @return true for a unique index; false by default
    boolean unique() default false;
}
