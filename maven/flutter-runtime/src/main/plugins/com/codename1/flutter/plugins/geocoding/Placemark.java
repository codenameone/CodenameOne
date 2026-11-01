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
package com.codename1.flutter.plugins.geocoding;

/** geocoding's {@code Placemark}: the address parts found for coordinates. */
public class Placemark {
    private String name;
    private String street;
    private String isoCountryCode;
    private String country;
    private String postalCode;
    private String administrativeArea;
    private String subAdministrativeArea;
    private String locality;
    private String subLocality;
    private String thoroughfare;
    private String subThoroughfare;

    public Placemark() {
    }

    public void name(String v) {
        name = v;
    }

    public void street(String v) {
        street = v;
    }

    public void isoCountryCode(String v) {
        isoCountryCode = v;
    }

    public void country(String v) {
        country = v;
    }

    public void postalCode(String v) {
        postalCode = v;
    }

    public void administrativeArea(String v) {
        administrativeArea = v;
    }

    public void subAdministrativeArea(String v) {
        subAdministrativeArea = v;
    }

    public void locality(String v) {
        locality = v;
    }

    public void subLocality(String v) {
        subLocality = v;
    }

    public void thoroughfare(String v) {
        thoroughfare = v;
    }

    public void subThoroughfare(String v) {
        subThoroughfare = v;
    }

    public Placemark(String name, String street, String isoCountryCode, String country, String postalCode,
            String administrativeArea, String subAdministrativeArea, String locality, String subLocality,
            String thoroughfare, String subThoroughfare) {
        this.name = name;
        this.street = street;
        this.isoCountryCode = isoCountryCode;
        this.country = country;
        this.postalCode = postalCode;
        this.administrativeArea = administrativeArea;
        this.subAdministrativeArea = subAdministrativeArea;
        this.locality = locality;
        this.subLocality = subLocality;
        this.thoroughfare = thoroughfare;
        this.subThoroughfare = subThoroughfare;
    }

    public String name() {
        return name;
    }

    public String street() {
        return street;
    }

    public String isoCountryCode() {
        return isoCountryCode;
    }

    public String country() {
        return country;
    }

    public String postalCode() {
        return postalCode;
    }

    public String administrativeArea() {
        return administrativeArea;
    }

    public String subAdministrativeArea() {
        return subAdministrativeArea;
    }

    public String locality() {
        return locality;
    }

    public String subLocality() {
        return subLocality;
    }

    public String thoroughfare() {
        return thoroughfare;
    }

    public String subThoroughfare() {
        return subThoroughfare;
    }

    @Override
    public String toString() {
        return "Name: " + name + ", Street: " + street + ", ISO Country Code: " + isoCountryCode
                + ", Country: " + country + ", Postal code: " + postalCode
                + ", Administrative area: " + administrativeArea
                + ", Subadministrative area: " + subAdministrativeArea + ", Locality: " + locality
                + ", Sublocality: " + subLocality + ", Thoroughfare: " + thoroughfare
                + ", Subthoroughfare: " + subThoroughfare;
    }
}
