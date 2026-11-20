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
package com.codenameone.examples.papercut.shop;

import com.codenameone.examples.papercut.api.ShopItemDto;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// What the shop sells. Fixed in code rather than in a table: prices are part
/// of a release, and a catalogue the console could edit is a feature this
/// sample does not need.
public final class Catalog {
    public static final String QUILL = "QUILL";
    public static final String ERASERS = "ERASERS";
    public static final String COINS = "COINS";

    /// The paper every player owns from the start.
    public static final String FREE_QUILL = "quill-white";
    /// The consumable: two spare erasers for the next run.
    public static final String ERASERS_ITEM = "erasers-2";
    public static final int ERASERS_PER_PACK = 2;

    private static final List<ShopItemDto> ITEMS;

    static {
        List<ShopItemDto> items = new ArrayList<ShopItemDto>();
        items.add(new ShopItemDto(FREE_QUILL, "White Bond", QUILL, 0, null, "#f7f4ec"));
        items.add(new ShopItemDto("quill-kraft", "Kraft", QUILL, 150, null, "#c49a6c"));
        items.add(new ShopItemDto("quill-newsprint", "Newsprint", QUILL, 250, null, "#d9d6cf"));
        items.add(new ShopItemDto("quill-origami-red", "Origami Red", QUILL, 400, null, "#d7263d"));
        items.add(new ShopItemDto("quill-blueprint", "Blueprint", QUILL, 600, null, "#1f4e8c"));
        items.add(new ShopItemDto("quill-gold-foil", "Gold Foil", QUILL, 1200, null, "#d4af37"));
        items.add(new ShopItemDto(ERASERS_ITEM, "Two Spare Erasers", ERASERS, 40, null, null));
        items.add(new ShopItemDto("coins-small", "Pocket of Ink", COINS, 0, "papercut.coins.small", null));
        items.add(new ShopItemDto("coins-large", "Inkwell", COINS, 0, "papercut.coins.large", null));
        ITEMS = Collections.unmodifiableList(items);
    }

    private Catalog() {
    }

    public static List<ShopItemDto> items() {
        return ITEMS;
    }

    public static ShopItemDto byId(String id) {
        for (ShopItemDto item : ITEMS) {
            if (item.id().equals(id)) {
                return item;
            }
        }
        return null;
    }

    public static ShopItemDto bySku(String sku) {
        for (ShopItemDto item : ITEMS) {
            if (item.sku() != null && item.sku().equals(sku)) {
                return item;
            }
        }
        return null;
    }

    /// The ink coins a store pack grants.
    public static int coins(ShopItemDto item) {
        if ("papercut.coins.small".equals(item.sku())) {
            return 500;
        }
        if ("papercut.coins.large".equals(item.sku())) {
            return 3000;
        }
        return 0;
    }
}
