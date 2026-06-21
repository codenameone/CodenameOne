/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
package com.codename1.maps.spi;

import java.util.ArrayList;
import java.util.List;

/// The registry through which build-injected [MapProvider] implementations
/// advertise themselves to the core [com.codename1.maps.NativeMap] component.
///
/// Core never registers a provider itself. The build, when a `maps.provider`
/// hint selects one, injects the provider implementation into the app and
/// weaves a `register(...)` call into the generated startup code (the same way
/// optional features such as push messaging are wired in). With no provider
/// injected the registry is empty and `NativeMap` renders the vector fallback.
public final class MapProviderRegistry {

    private static final List PROVIDERS = new ArrayList();
    private static List preferredOrder;

    private MapProviderRegistry() {
    }

    /// Registers a provider. Called by build-injected startup code. Repeated
    /// registration of the same provider id replaces the earlier instance.
    public static synchronized void register(MapProvider provider) {
        if (provider == null) {
            return;
        }
        for (int i = 0; i < PROVIDERS.size(); i++) {
            MapProvider existing = (MapProvider) PROVIDERS.get(i);
            if (existing.getId() != null && existing.getId().equals(provider.getId())) {
                PROVIDERS.set(i, provider);
                return;
            }
        }
        PROVIDERS.add(provider);
    }

    /// Sets the ordered provider fallback chain by id, e.g.
    /// `setProviderOrder("google", "huawei", "web")` to try Google Maps, then
    /// Huawei Map Kit, then a web-SDK map, before the pure-vector fallback. The
    /// special ids `"vector"`/`"none"` terminate the chain (force the vector
    /// fallback even if a later provider is available). Any provider not named
    /// in a non-terminated chain is still used as a last resort if available.
    /// Build hints (`maps.providers` / per-platform `<platform>.maps.providers`)
    /// set this at startup; call it from app code to override.
    public static synchronized void setProviderOrder(String[] ids) {
        if (ids == null || ids.length == 0) {
            preferredOrder = null;
            return;
        }
        List order = new ArrayList();
        for (String id : ids) {
            if (id != null && id.length() > 0) {
                order.add(id);
            }
        }
        preferredOrder = order.isEmpty() ? null : order;
    }

    /// Hints which single provider id to prefer (then any available). Kept for
    /// convenience; equivalent to a one-element [#setProviderOrder(String[])].
    public static synchronized void setPreferredProvider(String id) {
        setProviderOrder(new String[]{id});
    }

    /// Returns the provider that should back a new native map by walking the
    /// configured fallback chain (preferred ids first, honoring a `vector`
    /// terminator), then any remaining available provider, or `null` when none
    /// can render right now (the caller then uses the vector fallback).
    public static synchronized MapProvider getProvider() {
        if (preferredOrder != null) {
            for (Object idObj : preferredOrder) {
                String id = (String) idObj;
                if ("vector".equals(id) || "none".equals(id)) {
                    return null;
                }
                MapProvider p = availableById(id);
                if (p != null) {
                    return p;
                }
            }
        }
        for (Object prov : PROVIDERS) {
            MapProvider p = (MapProvider) prov;
            if (safeAvailable(p)) {
                return p;
            }
        }
        return null;
    }

    private static MapProvider availableById(String id) {
        for (Object prov : PROVIDERS) {
            MapProvider p = (MapProvider) prov;
            if (id != null && id.equals(p.getId()) && safeAvailable(p)) {
                return p;
            }
        }
        return null;
    }

    /// Whether any registered provider can render on this device right now.
    public static synchronized boolean hasProvider() {
        return getProvider() != null;
    }

    private static boolean safeAvailable(MapProvider p) {
        try {
            return p.isAvailable();
        } catch (Throwable t) {
            // A provider whose native side failed to initialize (e.g. missing
            // Play Services) must not break map creation -- treat as absent.
            return false;
        }
    }
}
