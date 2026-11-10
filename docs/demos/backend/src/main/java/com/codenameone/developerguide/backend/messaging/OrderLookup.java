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
package com.codenameone.developerguide.backend.messaging;

import com.codename1.backend.annotations.CacheEvict;
import com.codename1.backend.annotations.Cacheable;
import com.codename1.backend.annotations.Component;
import com.codenameone.developerguide.backend.orders.Order;

// tag::backend-cache[]
@Component
public class OrderLookup {
    @Cacheable(value = "orders", key = "#id")
    public Order find(long id) {
        return load(id);
    }

    @CacheEvict(value = "orders", key = "#order.id")
    public void update(Order order) {
        // write the change; the next find() reads it again
    }
// end::backend-cache[]

    private Order load(long id) {
        Order o = new Order();
        o.id = id;
        return o;
    }
}
