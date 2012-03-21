/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.contacts;

/**
 * This class represents a Contact Address
 * 
 * @author Chen
 */
public class Address {

    private String streetAddress;
    private String locality;
    private String region;
    private String postalCode;
    private String country;

    /**
     * Empty Constructor
     */
    public Address() {
    }

    /**
     * Gets Address Country
     * @return 
     */
    public String getCountry() {
        return country;
    }

    /**
     * Gets Address Locality
     * @return 
     */
    public String getLocality() {
        return locality;
    }

    /**
     * Gets Address Postal Code
     * @return 
     */
    public String getPostalCode() {
        return postalCode;
    }

    /**
     * Gets Address Region
     * @return 
     */
    public String getRegion() {
        return region;
    }

    /**
     * Gets Address Street
     * @return 
     */
    public String getStreetAddress() {
        return streetAddress;
    }

    /**
     * Sets Address Country
     */
    public void setCountry(String country) {
        this.country = country;
    }

    /**
     * Sets Address Locality
     */
    public void setLocality(String locality) {
        this.locality = locality;
    }

    /**
     * Sets Address Postal Code
     */
    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    /**
     * Sets Address Region
     */
    public void setRegion(String region) {
        this.region = region;
    }

    /**
     * Sets Address street
     */
    public void setStreetAddress(String streetAddress) {
        this.streetAddress = streetAddress;
    }

    
}
