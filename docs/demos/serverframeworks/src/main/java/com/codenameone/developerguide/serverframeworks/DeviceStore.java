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
// Guide samples for server stacks that are not Codename One. This directory is
// NOT a Maven module and nothing builds it: the guide's snippet validator skips
// includes under src/main/java, so these carry no dependencies and are read
// rather than compiled.

package com.codenameone.developerguide.serverframeworks;

/**
 * The application's own device table, as both receivers use it. Shown as an
 * interface because the digest says nothing about how you store keys; what it
 * does require is that the deduplication marker and the deletion commit
 * together. Scope the store to the configured organization and enforce a unique
 * eventKey. Concurrent duplicate inserts must roll back the transaction.
 */
public interface DeviceStore {

    boolean alreadyApplied(String eventKey);

    // Delete by normalized provider + bare token/web endpoint, never by the
    // original prefixed Push.getPushKey(). Backfill these columns first.
    void removeTarget(String provider, String target);

    void markApplied(String eventKey);
}
