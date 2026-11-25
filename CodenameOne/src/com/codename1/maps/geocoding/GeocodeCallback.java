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
package com.codename1.maps.geocoding;

import java.util.List;

/// Receives the outcome of a geocoding request. Exactly one method is invoked
/// per request, always on the Codename One event dispatch thread.
public interface GeocodeCallback {

    /// Delivers the places found, best first. The list is empty when the
    /// service understood the request and knows no matching place.
    ///
    /// #### Parameters
    ///
    /// - `places`: the [Place] results, never `null`
    void placesFound(List places);

    /// Reports that the request failed: the network failed, the service
    /// answered an error status, or the answer could not be read.
    ///
    /// #### Parameters
    ///
    /// - `message`: a human readable explanation, never `null`
    ///
    /// - `error`: the underlying exception when one was thrown, or `null`
    void geocodeFailed(String message, Throwable error);
}
