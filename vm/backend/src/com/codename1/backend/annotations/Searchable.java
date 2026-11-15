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
package com.codename1.backend.annotations;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Keeps a full-text search index in step with an entity on the server.
///
/// After a transaction commits, every entity of this type it persisted or
/// updated is written to the index, and every one it removed is deleted from
/// it, through the server's search engine (`cn1.search.url`: Elasticsearch,
/// OpenSearch, or the application's own database). A transaction that rolls
/// back changes nothing in the index.
///
/// The search document holds the entity's persistent fields, keyed by column
/// name. When any field is marked [SearchField] only those fields are indexed,
/// which keeps a password hash or a token out of a search cluster.
///
/// ```java
/// @Entity @Searchable(index = "products")
/// public class Product {
///     @Id public long id;
///     @SearchField public String name;
///     @SearchField public String description;
///     String supplierCode;
/// }
/// ```
///
/// A bulk `update` or `delete` statement changes rows, not entities, and is not
/// reflected in the index. On a device this annotation has no effect.
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Searchable {
    /// The index name: lowercase letters, digits, `-` and `_`, not starting
    /// with `-` or `_`. Empty means the entity's table name.
    String index() default "";
}
