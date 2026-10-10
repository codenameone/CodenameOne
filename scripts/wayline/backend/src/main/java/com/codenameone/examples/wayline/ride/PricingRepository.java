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
package com.codenameone.examples.wayline.ride;

import com.codename1.backend.annotations.Component;
import com.codename1.orm.session.Session;
import com.codenameone.examples.wayline.domain.Pricing;

/// Where the admin's prices are kept: one row, or none while the prices are
/// still the settings'.
@Component
public class PricingRepository {
    private static final Integer ONLY = Integer.valueOf(1);

    private final Session session;

    public PricingRepository(Session session) {
        this.session = session;
    }

    /// The prices the admin set, or null when they have set none.
    public Pricing find() {
        return session.find(Pricing.class, ONLY);
    }

    /// Stores the admin's prices for the first time, as the one row there is.
    /// Changing them afterwards needs no call: the transaction writes what
    /// changed in the row [#find] answered.
    public void add(Pricing pricing) {
        pricing.id = ONLY.intValue();
        session.persist(pricing);
    }
}
