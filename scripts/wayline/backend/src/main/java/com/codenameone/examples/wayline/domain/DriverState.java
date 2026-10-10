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
package com.codenameone.examples.wayline.domain;

import com.codename1.annotations.Column;
import com.codename1.annotations.Entity;
import com.codename1.annotations.Id;

/// Where a driver's car is, whether they are on line, and what riders made of them.
@Entity(table = "wl_driver")
public class DriverState {
    @Id(autoIncrement = false)
    @Column(name = "username", nullable = false)
    public String username = "";

    @Column(name = "online", nullable = false)
    public boolean online;

    @Column(name = "lat", nullable = false)
    public double lat;

    @Column(name = "lng", nullable = false)
    public double lng;

    @Column(name = "heading", nullable = false)
    public double heading;

    @Column(name = "last_seen", nullable = false)
    public long lastSeen;

    @Column(name = "rating_sum", nullable = false)
    public long ratingSum;

    @Column(name = "rating_count", nullable = false)
    public long ratingCount;

    public DriverState() {
    }

    /// The mean of the stars given, or 5 before any were.
    public double rating() {
        return ratingCount == 0 ? 5d : Math.round(ratingSum * 10d / ratingCount) / 10d;
    }
}
