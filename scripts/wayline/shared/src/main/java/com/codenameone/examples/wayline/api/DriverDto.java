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
package com.codenameone.examples.wayline.api;

import com.codename1.annotations.Mapped;

/// A driver, for the admin's fleet view and for the driver's own status.
@Mapped
public class DriverDto {
    public String username;
    public String displayName;
    public String vehicle;
    public String plate;
    public boolean online;
    public double lat;
    public double lng;
    public double heading;
    public double rating;
    public long lastSeen;
    public int rides;
    /// Whether the driving application was approved. Only an approved driver can
    /// go on line.
    public boolean approved;
    /// `standard`, `comfort` or `xl`: the kind of ride this car is offered.
    public String product;
    public String gender;
    public boolean accessible;
    /// The share of the offers this driver was sent that they took, 0 to 1; 1
    /// before any was sent.
    public double acceptanceRate;
    public boolean flagged;

    public DriverDto() {
    }
}
