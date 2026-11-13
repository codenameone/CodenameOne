/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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

/// `@Entity` classes stored in MongoDB or DynamoDB instead of a SQL database.
///
/// The same generated entity models the SQL session uses map an entity to a
/// document: each persistent attribute becomes a field named by its column, and
/// the identifier becomes MongoDB's `_id` or the DynamoDB table's partition key.
/// [DocumentSession] has the session operations -- `persist`, `merge`, `find`,
/// `remove` -- and `query` returns the ORM's own
/// [com.codename1.orm.session.Query], so a repository written against it moves
/// between SQL and documents unchanged.
///
/// ```java
/// DocumentSession session = DocumentSession.current();   // cn1.document.url
/// session.persist(order);
/// List<Order> open = session.query(Order.class).eq("status", "open")
///         .orderBy("created", false).limit(20).list();
/// ```
///
/// What a document store cannot do the SQL session's way is refused rather than
/// approximated:
///
/// - **No relationships.** An entity with `@ManyToOne`, `@OneToMany` or an
///   element collection is rejected when first used; keep the related id as an
///   ordinary field and query by it. `join`, `leftJoin`, `fetch` and
///   `containsElement` throw.
/// - **No numeric generated identifiers.** A generated `String` id gets a new
///   ObjectId (MongoDB) or a random 128-bit hex id (DynamoDB); a generated
///   number needs a SQL sequence.
/// - **A composite identifier has exactly two columns, one marked
///   [com.codename1.annotations.db.SortKey].** That is DynamoDB's partition and
///   sort key, and a compound `_id` in MongoDB.
/// - **No identity map or dirty tracking.** Each call goes to the store:
///   `merge` writes the whole entity, and two `find`s return two objects.
/// - **No transactions** spanning several calls. A `@Version` attribute is
///   honoured on every write, which is what keeps concurrent updates safe.
///
/// On DynamoDB a query with an equality on the partition key reads only that
/// partition, using at most one comparison on the sort key; every other query
/// scans the table. Either way sorting and paging happen in memory. A scan is
/// right for small tables and administration screens; a hot path on a large
/// table belongs on a partition key, or on
/// [com.codename1.backend.dynamo.DynamoDb#query] with a secondary index.
package com.codename1.backend.document;
