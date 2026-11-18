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

/// Marks the `String` field that holds the tenant a row belongs to.
///
/// On the backend a managed session writes the current tenant into this field
/// on every insert and adds it to the condition of every statement it issues
/// for the entity: finds, criteria and JPQL queries, counts, bulk updates and
/// deletes, and the update and delete of a single entity. A session with no
/// current tenant refuses to touch the entity at all rather than read every
/// tenant's rows. Native SQL is never rewritten.
///
/// On the client there is no current tenant and the field is an ordinary
/// column.
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
@com.codename1.impl.SharedWithBackend
public @interface TenantId {
}
