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

/// Turns place names and addresses into coordinates, and coordinates back
/// into addresses.
///
/// [com.codename1.maps.geocoding.NominatimGeocoder] speaks Nominatim's JSON
/// API, which a Codename One backend answers from an OpenStreetMap extract
/// (`com.codename1.backend.osm.OsmGeocoding`), as do Nominatim installations
/// and several hosted services. Results are
/// [com.codename1.maps.geocoding.Place] objects delivered to a
/// [com.codename1.maps.geocoding.GeocodeCallback] on the event dispatch
/// thread.
///
/// Results built from OpenStreetMap data must be credited to
/// "OpenStreetMap contributors" where the user sees them.
package com.codename1.maps.geocoding;
