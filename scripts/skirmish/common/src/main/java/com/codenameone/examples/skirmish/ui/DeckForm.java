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
package com.codenameone.examples.skirmish.ui;

import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.GridLayout;
import com.codenameone.examples.skirmish.api.DeckDto;
import com.codenameone.examples.skirmish.engage.Telemetry;
import com.codenameone.examples.skirmish.net.Api;
import com.codenameone.examples.skirmish.net.Net;
import com.codenameone.examples.skirmish.net.Session;
import com.codenameone.examples.skirmish.rules.CardDef;
import com.codenameone.examples.skirmish.rules.Cards;
import com.codenameone.examples.skirmish.rules.Deck;
import java.util.ArrayList;
import java.util.List;

/// Building a deck: tap a card to put it in or take it out. The card list,
/// the costs and what makes a deck valid all come from the rules module,
/// which the server checks the saved deck against with the same code.
// tag::skirmish-deck-form[]
public final class DeckForm {
    private final List<String> deck = new ArrayList<String>();
    private final Label summary = new Label("", "DeckSummary");
    private final Button save = Ui.wood("Save deck", "saveDeck", null);
    private final List<Button> slots = new ArrayList<Button>();

    private DeckForm() {
    }

    public static void show() {
        new DeckForm().build();
    }

    private void build() {
        if (Session.user().deck() != null) {
            deck.addAll(Session.user().deck());
        }
        Form f = Ui.form("Deck", "deck");
        Ui.back(f, Nav::home);
        f.add(summary);
        Container grid = new Container(new GridLayout(4));
        int size = Math.min(CN.convertToPixels(18),
                (Math.min(CN.convertToPixels(Ui.MAX_COLUMN_MM), CN.getDisplayWidth()) - CN.convertToPixels(12)) / 4);
        List<CardDef> all = Cards.playable();
        for (int i = 0; i < all.size(); i++) {
            final CardDef c = all.get(i);
            Button b = new Button("", CardArt.card(c.id, size), "CardSlot");
            b.setName("deck-" + c.id);
            b.setTextPosition(Component.BOTTOM);
            b.putClientProperty("card", c.id);
            b.addActionListener(e -> toggle(c.id));
            slots.add(b);
            grid.add(b);
        }
        f.add(grid);
        save.addActionListener(e -> Api.game().saveDeck(new DeckDto(new ArrayList<String>(deck)), Net.to(saved -> {
            Telemetry.deckSaved();
            Session.refreshUser(() -> Ui.later(() -> Ui.toast("Deck saved")), Ui::failed);
        }, Ui::failed)));
        f.add(save);
        refresh();
        f.show();
    }

    private void toggle(String id) {
        if (deck.contains(id)) {
            deck.remove(id);
        } else if (deck.size() < Deck.SIZE) {
            deck.add(id);
        } else {
            Ui.toast("A deck holds " + Deck.SIZE + " cards. Take one out first.");
        }
        refresh();
    }

    private void refresh() {
        int cost = 0;
        for (String id : deck) {
            cost += Cards.get(id).cost;
        }
        for (Button b : slots) {
            b.setUIID(deck.contains((String) b.getClientProperty("card")) ? "CardSlotSelected" : "CardSlot");
        }
        String avg = deck.isEmpty() ? "-" : (cost * 10 / deck.size()) / 10 + "." + (cost * 10 / deck.size()) % 10;
        summary.setText(deck.size() + " / " + Deck.SIZE + " cards   average elixir " + avg);
        save.setEnabled(Cards.isValidDeck(deck.toArray(new String[deck.size()])));
        if (summary.getComponentForm() != null) {
            summary.getComponentForm().revalidate();
        }
    }
}
// end::skirmish-deck-form[]
