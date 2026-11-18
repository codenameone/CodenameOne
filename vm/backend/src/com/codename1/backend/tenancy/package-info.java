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
/// Multi-tenancy: one deployment serving many customers whose data must never
/// mix.
///
/// [TenantContext] holds the tenant of the work running on this thread.
/// [TenantFilter] sets it for each request. Two ways to keep the data apart
/// build on it, and an application can use either or both:
///
/// - **A shared table.** An entity with a
///   [com.codename1.annotations.db.TenantId] field is partitioned by the
///   managed session itself: every insert is stamped with the current tenant,
///   and every query, count, update and delete is limited to it. With no
///   tenant set the session refuses the entity instead of answering every
///   tenant's rows.
/// - **A database or schema per tenant.** [TenantDataSources] keeps one
///   [com.codename1.backend.DataSource] per tenant, opened on first use from a
///   URL template.
package com.codename1.backend.tenancy;
