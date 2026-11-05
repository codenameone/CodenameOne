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
package com.codenameone.examples.splat.shop;

import com.codenameone.examples.splat.api.ShopItemDto;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// Everything the shop sells. The markers are side-grades, not upgrades -- each
/// trades rate of fire against accuracy and range in the rules' own balance, and
/// bot matches are played with all three -- so a purchase never decides a match.
/// The rest is paint colour and emotes.
///
/// The `sku` of a real-money item must match the product id configured in App
/// Store Connect and the Play Console.
// tag::splat-catalog[]
public final class Catalog {
    public static final String GEMS = "gems";
    public static final String OUTFIT = "outfit";
    public static final String RUNNER = "runner";
    public static final String EMOTES = "emotes";
    public static final String PASS = "pass";

    public static final String DEFAULT_OUTFIT = "paint-classic";
    public static final String FREE_RUNNER = "semi";
    public static final String DEFAULT_EMOTES = "emotes-classic";
    public static final String GOLD_PASS = "gold-pass";

    private static final List<ShopItemDto> ITEMS;

    static {
        List<ShopItemDto> items = new ArrayList<ShopItemDto>();
        items.add(new ShopItemDto("gems-80", "Pouch of 80 gems", GEMS, 0, 0, "splat.gems_80"));
        items.add(new ShopItemDto("gems-500", "Crate of 500 gems", GEMS, 0, 0, "splat.gems_500"));
        items.add(new ShopItemDto(GOLD_PASS, "Gold Pass: paint that is somehow gold", PASS, 0, 0, "splat.gold_pass"));
        items.add(new ShopItemDto("pump", "The Purist (pump marker)", RUNNER, 500, 0, ""));
        items.add(new ShopItemDto("ramp", "The Ramping Dad (full auto)", RUNNER, 0, 60, ""));
        items.add(new ShopItemDto("paint-neon", "Neon paint, visible from space", OUTFIT, 400, 0, ""));
        items.add(new ShopItemDto("paint-glitter", "Glitter paint (banned in 3 states)", OUTFIT, 0, 40, ""));
        items.add(new ShopItemDto("emotes-taunt", "Dispute emotes: 'that was a bounce!'", EMOTES, 0, 30, ""));
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

    /// The paint an owned item unlocks, or null.
    public static String outfitOf(String itemId) {
        if (GOLD_PASS.equals(itemId)) {
            return "paint-gold";
        }
        return itemId.startsWith("paint-") ? itemId : null;
    }

    /// Whether `id` names a marker: the free one, or one the shop sells.
    public static boolean isRunner(String id) {
        if (FREE_RUNNER.equals(id)) {
            return true;
        }
        ShopItemDto item = item(id);
        return item != null && RUNNER.equals(item.kind());
    }
}
// end::splat-catalog[]
