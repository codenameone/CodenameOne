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

/// The prices an admin set. One row, with the id 1, once they have set any.
@Entity(table = "wl_pricing")
public class Pricing {
    @Id(autoIncrement = false)
    @Column(name = "id", nullable = false)
    public int id;

    @Column(name = "base_cents", nullable = false)
    public long baseCents;

    @Column(name = "per_km_cents", nullable = false)
    public long perKmCents;

    @Column(name = "per_minute_cents", nullable = false)
    public long perMinuteCents;

    @Column(name = "minimum_cents", nullable = false)
    public long minimumCents;

    @Column(name = "fee_percent", nullable = false)
    public double feePercent;

    @Column(name = "commission_percent", nullable = false)
    public double commissionPercent;

    @Column(name = "surge", nullable = false)
    public double surge;

    @Column(name = "comfort", nullable = false)
    public double comfort;

    @Column(name = "xl", nullable = false)
    public double xl;

    @Column(name = "updated_at", nullable = false)
    public long updatedAt;

    @Column(name = "updated_by", nullable = false)
    public String updatedBy = "";

    public Pricing() {
    }
}
