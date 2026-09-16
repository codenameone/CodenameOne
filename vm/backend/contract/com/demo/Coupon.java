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
package com.demo;

import com.codename1.annotations.Entity;
import com.codename1.annotations.Id;

/**
 * An entity whose key the APPLICATION assigns, and a string at that.
 *
 * <p>The shape a voucher code or an externally issued identifier takes, and the
 * one the three engines declare most differently: SQLite and PostgreSQL store it
 * as unbounded text, while MySQL cannot index an unbounded column at all and
 * needs a VARCHAR with a length. Nothing exercised that column against a real
 * MySQL server until this class existed -- the same gap that let a char field
 * which could not be inserted on PostgreSQL go unnoticed.
 */
@Entity(table = "cn1_coupons")
public class Coupon {
    /** Assigned, not generated: no engine generates a string key. */
    @Id(autoIncrement = false)
    public String code;

    public int discount;

    public Coupon() {
    }
}
