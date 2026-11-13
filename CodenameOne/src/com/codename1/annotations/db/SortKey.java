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

/// Marks the sort key of a two-column composite identifier, for a document store.
///
/// A DynamoDB table is keyed by a partition key and, optionally, a sort key; the
/// items of one partition are stored in sort-key order and read together. An
/// entity says which identifier column is which by marking one of exactly two
/// `@Id` columns (or two components of an `@EmbeddedId`) with this annotation;
/// the other is the partition key. MongoDB stores the pair as a compound `_id`.
///
/// ```java
/// @Entity
/// public class Message {
///     @Id(autoIncrement = false) String channel;
///     @Id(autoIncrement = false) @SortKey long sentAt;
///     String text;
/// }
/// ```
///
/// A SQL session ignores it: the pair is the table's composite primary key there.
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
@com.codename1.impl.SharedWithBackend
public @interface SortKey {
}
