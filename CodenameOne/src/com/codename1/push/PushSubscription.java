/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.push;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// Opaque native registration material produced by a push transport.
public final class PushSubscription {
    private final String transportId;
    private final String token;
    private final String platform;
    private final String installationId;
    private final long expiresAt;
    private final List<String> capabilities;

    public PushSubscription(String transportId, String token, String platform,
            String installationId, long expiresAt, List<String> capabilities) {
        if (transportId == null || token == null) {
            throw new IllegalArgumentException("transportId and token are required");
        }
        this.transportId = transportId;
        this.token = token;
        this.platform = platform;
        this.installationId = installationId;
        this.expiresAt = expiresAt;
        this.capabilities = capabilities == null
                ? Collections.<String>emptyList()
                : Collections.unmodifiableList(new ArrayList<String>(capabilities));
    }

    public String getTransportId() { return transportId; }
    public String getToken() { return token; }
    public String getPlatform() { return platform; }
    public String getInstallationId() { return installationId; }
    public long getExpiresAt() { return expiresAt; }
    public List<String> getCapabilities() { return capabilities; }
}
