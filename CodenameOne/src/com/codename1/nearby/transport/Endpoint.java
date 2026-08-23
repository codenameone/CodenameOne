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
package com.codename1.nearby.transport;

/// Another device seen advertising the same service id.
///
/// An endpoint id is only meaningful for as long as the endpoint is visible
/// -- both platforms mint a fresh one per discovery session -- so persist
/// nothing from here. To recognise a device across sessions, use
/// `com.codename1.nearby.companion` or a name the app chooses itself.
public final class Endpoint {

    private final String id;
    private final String name;
    private final String serviceId;

    /// Ports construct these; application code receives them through
    /// [TransportListener].
    ///
    /// #### Parameters
    ///
    /// - `id`: the platform's endpoint id
    /// - `name`: the name the peer advertised
    /// - `serviceId`: the service both ends agreed on
    public Endpoint(String id, String name, String serviceId) {
        this.id = id;
        this.name = name == null ? "" : name;
        this.serviceId = serviceId == null ? "" : serviceId;
    }

    /// The endpoint id, which every other call in this package takes.
    /// Valid only while this endpoint is visible.
    public String getId() {
        return id;
    }

    /// The name the peer advertised itself under. Never null.
    public String getName() {
        return name;
    }

    /// The service id this endpoint was found under. Never null.
    public String getServiceId() {
        return serviceId;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Endpoint)) {
            return false;
        }
        Endpoint e = (Endpoint) o;
        return id == null ? e.id == null : id.equals(e.id);
    }

    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }

    public String toString() {
        return "Endpoint[" + id + ", " + name + "]";
    }
}
