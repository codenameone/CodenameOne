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
package com.codenameone.developerguide.backend;

import com.codename1.backend.Cache;

public final class CacheSnippets {
    /// What the loader below stands in for: a slow lookup in another service.
    public static final class Price {
        final String sku;

        Price(String sku) {
            this.sku = sku;
        }
    }

    static Price fetchPrice(String sku) {
        return new Price(sku);
    }

    // tag::backend-cache[]
    private static final Cache<String, Price> PRICES = Cache.<String, Price>builder("prices")
            .maximumSize(10000)                  // or maximumWeight(bytes, weigher)
            .expireAfterWriteMillis(60000)
            .removalListener((sku, price, cause) -> {
                if (cause == Cache.SIZE) {
                    System.out.println("evicted " + sku);
                }
            })
            .build();

    public static Price price(String sku) {
        // One load per SKU, however many requests ask at once.
        return PRICES.get(sku, CacheSnippets::fetchPrice);
    }

    public static void priceChanged(String sku) {
        PRICES.invalidate(sku);
    }
    // end::backend-cache[]

    private CacheSnippets() {
    }
}
