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

import com.codename1.maps.LatLng;

/// One geocoding result: a named place, an address or a street, and where
/// it is.
public final class Place {
    private final String name;
    private final String displayName;
    private final LatLng location;
    private final String category;
    private final String type;
    private final String houseNumber;
    private final String road;
    private final String city;
    private final String postcode;

    /// Creates a result. Every string may be empty but none is `null`.
    ///
    /// #### Parameters
    ///
    /// - `name`: the place's own name, empty for a bare address
    /// - `displayName`: the full line to show the user
    /// - `location`: where the place is
    /// - `category`: the OpenStreetMap key that classifies it, `amenity` or `highway` for example
    /// - `type`: that key's value, `cafe` or `residential` for example
    /// - `houseNumber`: the house number, when the place has an address
    /// - `road`: the street of the address
    /// - `city`: the city of the address
    /// - `postcode`: the postal code of the address
    public Place(String name, String displayName, LatLng location, String category, String type,
                 String houseNumber, String road, String city, String postcode) {
        this.name = orEmpty(name);
        this.displayName = orEmpty(displayName);
        this.location = location;
        this.category = orEmpty(category);
        this.type = orEmpty(type);
        this.houseNumber = orEmpty(houseNumber);
        this.road = orEmpty(road);
        this.city = orEmpty(city);
        this.postcode = orEmpty(postcode);
    }

    private static String orEmpty(String s) {
        return s == null ? "" : s;
    }

    /// The place's own name, empty for a bare address.
    public String getName() {
        return name;
    }

    /// The full line to show the user, name and address together.
    public String getDisplayName() {
        return displayName;
    }

    /// Where the place is.
    public LatLng getLocation() {
        return location;
    }

    /// The OpenStreetMap key that classifies the place: `amenity`, `shop`,
    /// `place`, `highway` and so on.
    public String getCategory() {
        return category;
    }

    /// The value of [#getCategory()]'s key: `cafe`, `city`, `residential`.
    public String getType() {
        return type;
    }

    /// The house number of the address, or empty.
    public String getHouseNumber() {
        return houseNumber;
    }

    /// The street of the address, or empty.
    public String getRoad() {
        return road;
    }

    /// The city of the address, or empty.
    public String getCity() {
        return city;
    }

    /// The postal code of the address, or empty.
    public String getPostcode() {
        return postcode;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
