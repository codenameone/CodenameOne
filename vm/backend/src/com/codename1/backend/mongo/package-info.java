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

/// A MongoDB client speaking the wire protocol (`OP_MSG`) directly, with
/// SCRAM-SHA-256 authentication and TLS.
///
/// [MongoClient] opens and pools connections; [MongoCollection] has the CRUD
/// operations. Documents are `Map`s -- [Bson] keeps their key order, which
/// MongoDB requires of a command -- holding strings, numbers, booleans,
/// `java.util.Date`, `byte[]`, [ObjectId], lists and nested maps.
/// [Filters] and [Updates] build the query and update documents.
///
/// Not supported: `mongodb+srv://` seed lists (they need DNS SRV lookups this
/// runtime does not have; list the hosts instead), replica-set failover (the
/// first host that answers is used for everything), SCRAM-SHA-1, X.509 and
/// Kerberos authentication, sessions and multi-document transactions.
package com.codename1.backend.mongo;
