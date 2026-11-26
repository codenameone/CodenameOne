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
package com.codenameone.examples.bazaar.ops;

import com.codenameone.examples.bazaar.api.TipPresetDto;

import java.util.List;

/// The online checkout's tip, worked out the same way by the server and the
/// app. A tip is a share of the goods after discounts (or a fixed amount);
/// it is never discounted, never taxed and stays outside the order's total,
/// so it is added to what the card is charged and nothing else.
public final class Tips {
    /// The most presets a store may offer.
    public static final int MAX_PRESETS = 4;
    /// The largest percentage preset, 100%.
    public static final int MAX_PERCENT_BP = 10000;

    private Tips() {
    }

    /// What `preset` comes to on `baseCents` of goods: a percentage rounded
    /// half up, or the fixed amount. Never negative.
    public static long amount(TipPresetDto preset, long baseCents) {
        if (preset == null || baseCents < 0) {
            return 0;
        }
        if (preset.fixedCents() > 0) {
            return preset.fixedCents();
        }
        if (preset.percentBp() <= 0) {
            return 0;
        }
        return (baseCents * preset.percentBp() + 5000L) / 10000L;
    }

    /// The tip for choice `choice` (0 none, 1 the first preset) of `presets`.
    /// Answers -1 for a choice that is not on offer.
    public static long forChoice(List<TipPresetDto> presets, int choice, long baseCents) {
        if (choice == 0) {
            return 0;
        }
        if (presets == null || choice < 0 || choice > presets.size()) {
            return -1;
        }
        return amount(presets.get(choice - 1), baseCents);
    }

    /// A percentage preset as the shopper reads it: `15%`, `12.5%`. Null for
    /// a fixed preset, which the caller formats in its currency.
    public static String percentLabel(TipPresetDto preset) {
        if (preset == null || preset.fixedCents() > 0 || preset.percentBp() <= 0) {
            return null;
        }
        int bp = preset.percentBp();
        StringBuilder b = new StringBuilder();
        b.append(bp / 100);
        int rest = bp % 100;
        if (rest != 0) {
            b.append('.');
            if (rest % 10 == 0) {
                b.append(rest / 10);
            } else {
                if (rest < 10) {
                    b.append('0');
                }
                b.append(rest);
            }
        }
        return b.append('%').toString();
    }
}
