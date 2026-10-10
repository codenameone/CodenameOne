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

/// A saved card: what the provider calls it and what to show for it. The
/// number itself is never here.
@Entity(table = "wl_payment_method")
public class PaymentMethod {
    @Id(autoIncrement = false)
    @Column(name = "id", nullable = false)
    public String id = "";

    @Column(name = "username", nullable = false)
    public String username = "";

    @Column(name = "brand", nullable = false)
    public String brand = "";

    @Column(name = "last4", nullable = false)
    public String last4 = "";

    @Column(name = "exp_month", nullable = false)
    public int expMonth;

    @Column(name = "exp_year", nullable = false)
    public int expYear;

    @Column(name = "token", nullable = false)
    public String token = "";

    @Column(name = "is_default", nullable = false)
    public boolean preferred;

    @Column(name = "created_at", nullable = false)
    public long createdAt;

    public PaymentMethod() {
    }
}
