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
package com.codename1.backend.security.saml2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// A fixed set of [RelyingPartyRegistration]s.
public final class InMemoryRelyingPartyRegistrationRepository
        implements RelyingPartyRegistrationRepository {
    private final Map<String, RelyingPartyRegistration> byId =
            new LinkedHashMap<String, RelyingPartyRegistration>();

    public InMemoryRelyingPartyRegistrationRepository(RelyingPartyRegistration... registrations) {
        this(java.util.Arrays.asList(registrations));
    }

    public InMemoryRelyingPartyRegistrationRepository(List<RelyingPartyRegistration> registrations) {
        if (registrations.isEmpty()) {
            throw new IllegalArgumentException("At least one registration is required");
        }
        for (RelyingPartyRegistration r : registrations) {
            if (byId.put(r.getRegistrationId(), r) != null) {
                throw new IllegalArgumentException("Two registrations are called "
                        + r.getRegistrationId());
            }
        }
    }

    @Override
    public RelyingPartyRegistration findByRegistrationId(String registrationId) {
        return registrationId == null ? null : byId.get(registrationId);
    }

    /// Every registration, in the order given.
    public List<RelyingPartyRegistration> getRegistrations() {
        return Collections.unmodifiableList(new ArrayList<RelyingPartyRegistration>(byId.values()));
    }
}
