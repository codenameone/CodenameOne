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
package com.codenameone.examples.wayline.account;

import com.codename1.backend.annotations.Component;
import com.codename1.orm.session.Session;
import com.codenameone.examples.wayline.domain.DriverApplication;
import com.codenameone.examples.wayline.domain.DriverDocument;

/// What the demo accounts write that nobody using the server could: an
/// application to drive dated in the past, and documents that were accepted
/// without anybody having looked at them.
///
/// Only [DemoAccounts] uses it. Everything an account can really do to an
/// application goes through the service that owns applications.
@Component
public class DemoAccountRepository {
    private final Session session;

    public DemoAccountRepository(Session session) {
        this.session = session;
    }

    /// An account's application to drive, or null when it has none.
    public DriverApplication application(String username) {
        return session.find(DriverApplication.class, username);
    }

    public void add(DriverDocument document) {
        session.persist(document);
    }
}
