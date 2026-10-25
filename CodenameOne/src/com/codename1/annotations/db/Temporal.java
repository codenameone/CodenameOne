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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Stores a `java.util.Date`, `java.time.Instant`, `LocalDate` or
/// `LocalDateTime` field in a native date or timestamp column instead of the
/// portable default, a number in an integer column: epoch milliseconds, or
/// epoch days for a `LocalDate`. A `LocalDate` takes `DATE` and the other
/// java.time types take `TIMESTAMP`; a `LocalDateTime` is written as its wall
/// clock, with no zone applied.
///
/// Opt-in, because the two are different columns: an existing integer column
/// keeps its milliseconds and an existing native one needs this annotation.
/// Neither is converted into the other. A mapping that disagrees with the
/// column it finds is reported by `Session.schemaReport()` as unsupported, and
/// a backend refuses to start on it.
///
/// ```java
/// @Temporal(TemporalType.TIMESTAMP)
/// private Date createdAt;
/// ```
///
/// Semantics, the same on every engine:
///
/// - Values are written and read in **UTC**. The backend pins its own
///   connections to UTC, so the server's or the column's time zone setting does
///   not change what is stored. A `TIMESTAMP WITH TIME ZONE` column is read with
///   the offset the server sends.
/// - Precision is the **millisecond**, which is all a Date holds. A column with
///   finer precision has the extra digits dropped on read; one with coarser
///   precision rounds or truncates on write, as that engine does.
/// - On SQLite, which has no native date type, the value is text in the form
///   `2026-10-16 09:30:00.000`, which sorts in time order. Rows written in a
///   different textual form are read when they are ISO 8601, but compare as text
///   in queries.
///
/// Applies to entity fields only; identifier, version and element collection
/// values keep the portable storage, and the annotation cannot be combined
/// with [Convert].
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
@com.codename1.impl.SharedWithBackend
public @interface Temporal {
    /// The native column type.
    /// @return `TIMESTAMP` or `DATE`
    TemporalType value() default TemporalType.TIMESTAMP;
}
