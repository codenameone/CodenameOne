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
package com.codename1.backend.annotations;

/// What a [Transactional] method does about a transaction that is already
/// open on the calling thread. Spring's meanings, one for one.
public enum Propagation {
    /// Joins the open transaction, or opens one. The default.
    REQUIRED,
    /// Joins the open transaction, or runs without one.
    SUPPORTS,
    /// Joins the open transaction, and refuses to run without one.
    MANDATORY,
    /// Suspends the open transaction, if any, and opens its own on another
    /// connection. It commits or rolls back independently of the outer one.
    REQUIRES_NEW,
    /// Suspends the open transaction, if any, and runs without one.
    NOT_SUPPORTED,
    /// Refuses to run inside a transaction.
    NEVER,
    /// Runs in a savepoint of the open transaction, which a failure rolls back
    /// to without failing the outer transaction; opens one when none is open.
    NESTED;
}
