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
package com.codenameone.examples.skirmish.rules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// The cards a match can use: the built-in ones, followed by any a content pack
/// adds. The order is part of the simulation -- a card's index goes into the state
/// hash -- so the built-ins always come first and keep their indices, and a pack's
/// cards follow in the order the pack lists them.
public final class CardSet {
    public static final CardSet BUILTIN = new CardSet(Cards.builtins());

    private final List<CardDef> all;

    private CardSet(List<CardDef> all) {
        this.all = Collections.unmodifiableList(all);
    }

    /// The built-in cards plus `custom`. A custom card may not reuse a built-in id.
    public static CardSet withCustom(List<CardDef> custom) {
        if (custom == null || custom.isEmpty()) {
            return BUILTIN;
        }
        List<CardDef> out = new ArrayList<CardDef>(BUILTIN.all);
        for (int i = 0; i < custom.size(); i++) {
            CardDef c = custom.get(i);
            if (BUILTIN.get(c.id) != null) {
                throw new IllegalArgumentException("card id taken: " + c.id);
            }
            out.add(c);
        }
        return new CardSet(out);
    }

    public boolean isBuiltin() {
        return this == BUILTIN;
    }

    public List<CardDef> all() {
        return all;
    }

    public CardDef get(String id) {
        if (id == null) {
            return null;
        }
        for (int i = 0; i < all.size(); i++) {
            CardDef d = all.get(i);
            if (d.id.equals(id)) {
                return d;
            }
        }
        return null;
    }

    public int indexOf(String id) {
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).id.equals(id)) {
                return i;
            }
        }
        return -1;
    }

    public CardDef at(int index) {
        return all.get(index);
    }

    public List<CardDef> playable() {
        List<CardDef> out = new ArrayList<CardDef>();
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).isPlayable()) {
                out.add(all.get(i));
            }
        }
        return out;
    }

    /// Eight distinct playable cards of this set.
    public boolean isValidDeck(String[] deck) {
        if (deck == null || deck.length != Deck.SIZE) {
            return false;
        }
        for (int i = 0; i < deck.length; i++) {
            CardDef d = get(deck[i]);
            if (d == null || !d.isPlayable()) {
                return false;
            }
            for (int j = 0; j < i; j++) {
                if (deck[j].equals(deck[i])) {
                    return false;
                }
            }
        }
        return true;
    }
}
