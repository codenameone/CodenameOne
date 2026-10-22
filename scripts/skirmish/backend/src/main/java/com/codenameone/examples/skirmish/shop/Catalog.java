/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.skirmish.shop;

import com.codenameone.examples.skirmish.api.ShopItemDto;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// Everything the shop sells. Cosmetic only: nothing here changes a card, so a
/// purchase can never decide a match.
///
/// The `sku` of a real-money item must match the product id configured in App
/// Store Connect and the Play Console.
// tag::skirmish-catalog[]
public final class Catalog {
    public static final String GEMS = "gems";
    public static final String ARENA = "arena";
    public static final String EMOTES = "emotes";
    public static final String PASS = "pass";

    public static final String DEFAULT_ARENA = "arena-meadow";
    public static final String DEFAULT_EMOTES = "emotes-classic";
    public static final String ROYAL_PASS = "royal-pass";

    private static final List<ShopItemDto> ITEMS;

    static {
        List<ShopItemDto> items = new ArrayList<ShopItemDto>();
        items.add(new ShopItemDto("gems-80", "Pouch of 80 gems", GEMS, 0, 0, "skirmish.gems_80"));
        items.add(new ShopItemDto("gems-500", "Chest of 500 gems", GEMS, 0, 0, "skirmish.gems_500"));
        items.add(new ShopItemDto(ROYAL_PASS, "Royal Pass: the golden arena", PASS, 0, 0,
                "skirmish.royal_pass"));
        items.add(new ShopItemDto("arena-desert", "Desert arena", ARENA, 500, 0, ""));
        items.add(new ShopItemDto("arena-frost", "Frost arena", ARENA, 0, 60, ""));
        items.add(new ShopItemDto("emotes-taunt", "Taunt emotes", EMOTES, 0, 30, ""));
        ITEMS = Collections.unmodifiableList(items);
    }

    private Catalog() {
    }

    public static List<ShopItemDto> items() {
        return ITEMS;
    }

    public static ShopItemDto item(String id) {
        for (ShopItemDto i : ITEMS) {
            if (i.id().equals(id)) {
                return i;
            }
        }
        return null;
    }

    public static ShopItemDto bySku(String sku) {
        for (ShopItemDto i : ITEMS) {
            if (i.sku().length() > 0 && i.sku().equals(sku)) {
                return i;
            }
        }
        return null;
    }

    /// Gems a gem pack grants.
    public static int gems(ShopItemDto item) {
        if ("gems-80".equals(item.id())) {
            return 80;
        }
        if ("gems-500".equals(item.id())) {
            return 500;
        }
        return 0;
    }

    /// The arena skin an owned item unlocks, or null.
    public static String arenaOf(String itemId) {
        if (ROYAL_PASS.equals(itemId)) {
            return "arena-royal";
        }
        return itemId.startsWith("arena-") ? itemId : null;
    }
}
// end::skirmish-catalog[]
