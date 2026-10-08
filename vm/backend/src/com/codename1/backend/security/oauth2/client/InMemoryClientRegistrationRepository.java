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
package com.codename1.backend.security.oauth2.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Registrations given when the server starts.
///
/// ```java
/// @Bean
/// ClientRegistrationRepository providers() {
///     return new InMemoryClientRegistrationRepository(
///             CommonOAuth2Provider.GOOGLE.getBuilder("google")
///                     .clientId(id).clientSecret(secret).build());
/// }
/// ```
public final class InMemoryClientRegistrationRepository implements ClientRegistrationRepository {
    private final Map<String, ClientRegistration> registrations =
            new LinkedHashMap<String, ClientRegistration>();

    public InMemoryClientRegistrationRepository(ClientRegistration... registrations) {
        this(java.util.Arrays.asList(registrations));
    }

    public InMemoryClientRegistrationRepository(List<ClientRegistration> registrations) {
        if (registrations == null || registrations.isEmpty()) {
            throw new IllegalArgumentException("At least one registration is required");
        }
        for (ClientRegistration registration : registrations) {
            if (this.registrations.put(registration.getRegistrationId(), registration) != null) {
                throw new IllegalArgumentException("Two registrations have the id "
                        + registration.getRegistrationId());
            }
        }
    }

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        return registrations.get(registrationId);
    }

    /// Every registration, in the order given.
    public List<ClientRegistration> getRegistrations() {
        return Collections.unmodifiableList(new ArrayList<ClientRegistration>(
                registrations.values()));
    }
}
