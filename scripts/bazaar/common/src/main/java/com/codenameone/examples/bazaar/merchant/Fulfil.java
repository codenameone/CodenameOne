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
package com.codenameone.examples.bazaar.merchant;

import com.codename1.kanban.DefaultKanbanModel;
import com.codename1.kanban.KanbanCard;
import com.codename1.kanban.KanbanColumn;
import com.codenameone.examples.bazaar.api.BoardColumnDto;
import com.codenameone.examples.bazaar.api.OpenLineDto;
import com.codenameone.examples.bazaar.api.OrderCardDto;
import com.codenameone.examples.bazaar.api.RateQuoteDto;
import com.codenameone.examples.bazaar.api.UnfulfilledDto;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The fulfilment board's logic, apart from any screen: the server's columns
/// as a Kanban model, which moves the server accepts, how old a card is, the
/// carrier quotes in the order they are offered and the pick list a column
/// comes to. Nothing here needs a display.
final class Fulfil {
    static final String TO_PACK = "TO_PACK";
    static final String PACKED = "PACKED";
    static final String SHIPPED = "SHIPPED";
    static final String DELIVERED = "DELIVERED";
    /// The columns, left to right, as the server names them.
    static final String[] COLUMNS = {TO_PACK, PACKED, SHIPPED, DELIVERED};
    static final String[] TITLES = {"To pack", "Packed", "Shipped", "Delivered"};

    private Fulfil() {
    }

    /// Where a column is on the board, -1 for one the app does not know.
    static int indexOf(String column) {
        for (int iter = 0; iter < COLUMNS.length; iter++) {
            if (COLUMNS[iter].equalsIgnoreCase(column)) {
                return iter;
            }
        }
        return -1;
    }

    /// The column after `column`, or null when it is the last or unknown.
    static String next(String column) {
        int at = indexOf(column);
        return at < 0 || at + 1 >= COLUMNS.length ? null : COLUMNS[at + 1];
    }

    /// Whether the server takes a card from one column to the other: one
    /// column to the right, and nothing else.
    static boolean canMove(String from, String to) {
        int a = indexOf(from);
        int b = indexOf(to);
        return a >= 0 && b == a + 1;
    }

    /// The board as a Kanban model: the four columns in their order whatever
    /// order the server sent them in, a column the app does not know after
    /// them, and a card per order. A card's tag is the location its lines are
    /// picked from, when `locations` knows it.
    static DefaultKanbanModel model(List<BoardColumnDto> columns, Map<String, String> locations,
            long now) {
        DefaultKanbanModel model = new DefaultKanbanModel();
        List<BoardColumnDto> ordered = ordered(columns);
        for (int iter = 0; iter < ordered.size(); iter++) {
            BoardColumnDto c = ordered.get(iter);
            model.addColumn(new KanbanColumn(c.id(), c.title() == null ? c.id() : c.title()));
            for (int at = 0; c.cards() != null && at < c.cards().size(); at++) {
                OrderCardDto o = c.cards().get(at);
                model.addCard(c.id(), card(o, locations, now));
            }
        }
        return model;
    }

    /// The server's columns in board order.
    static List<BoardColumnDto> ordered(List<BoardColumnDto> columns) {
        List<BoardColumnDto> out = new ArrayList<BoardColumnDto>();
        if (columns == null) {
            return out;
        }
        for (int iter = 0; iter < COLUMNS.length; iter++) {
            for (int at = 0; at < columns.size(); at++) {
                if (COLUMNS[iter].equalsIgnoreCase(columns.get(at).id())) {
                    out.add(columns.get(at));
                }
            }
        }
        for (int at = 0; at < columns.size(); at++) {
            if (indexOf(columns.get(at).id()) < 0) {
                out.add(columns.get(at));
            }
        }
        return out;
    }

    static KanbanCard card(OrderCardDto o, Map<String, String> locations, long now) {
        KanbanCard card = new KanbanCard(o.orderId(), "#" + o.number() + "  "
                + (o.customer() == null ? "" : o.customer()));
        card.setDescription(items(o.items()) + "  " + age(o.createdAt(), now)
                + (o.trackingNumber() == null || o.trackingNumber().length() == 0 ? ""
                        : "  " + o.trackingNumber()));
        card.setBadge(age(o.createdAt(), now));
        String where = locations == null ? null : locations.get(o.orderId());
        if (where != null && where.length() > 0) {
            card.addTag(where);
        }
        card.setUserData(o);
        return card;
    }

    static String items(int n) {
        return n == 1 ? "1 item" : n + " items";
    }

    /// How long ago an order came in: minutes, hours, then days.
    static String age(long createdAt, long now) {
        if (createdAt <= 0) {
            return "";
        }
        long minutes = Math.max(0, now - createdAt) / 60000L;
        if (minutes < 60) {
            return minutes + "m";
        }
        long hours = minutes / 60;
        if (hours < 48) {
            return hours + "h";
        }
        return (hours / 24) + "d";
    }

    /// Carrier quotes as they are offered: cheapest first, then the faster
    /// of two at one price, then by name so the order is stable.
    static List<RateQuoteDto> sortRates(Collection<RateQuoteDto> rates) {
        List<RateQuoteDto> out = new ArrayList<RateQuoteDto>();
        if (rates == null) {
            return out;
        }
        for (RateQuoteDto r : rates) {
            if (r == null) {
                continue;
            }
            int at = 0;
            while (at < out.size() && compare(out.get(at), r) <= 0) {
                at++;
            }
            out.add(at, r);
        }
        return out;
    }

    private static int compare(RateQuoteDto a, RateQuoteDto b) {
        if (a.cents() != b.cents()) {
            return a.cents() < b.cents() ? -1 : 1;
        }
        if (a.days() != b.days()) {
            return a.days() < b.days() ? -1 : 1;
        }
        String x = a.title() == null ? "" : a.title();
        String y = b.title() == null ? "" : b.title();
        return x.compareTo(y);
    }

    /// The fastest of the quotes, or null when there are none.
    static RateQuoteDto fastest(List<RateQuoteDto> rates) {
        RateQuoteDto best = null;
        for (int iter = 0; rates != null && iter < rates.size(); iter++) {
            RateQuoteDto r = rates.get(iter);
            if (best == null || r.days() < best.days()
                    || (r.days() == best.days() && r.cents() < best.cents())) {
                best = r;
            }
        }
        return best;
    }

    /// For each order in the queue, the name of the location its open lines
    /// are held at -- the first one that names a location.
    static Map<String, String> locationsOf(List<UnfulfilledDto> queue, Map<String, String> names) {
        Map<String, String> out = new HashMap<String, String>();
        for (int iter = 0; queue != null && iter < queue.size(); iter++) {
            UnfulfilledDto u = queue.get(iter);
            for (int at = 0; u.lines() != null && at < u.lines().size(); at++) {
                String id = u.lines().get(at).locationId();
                if (id != null && id.length() > 0) {
                    String name = names == null ? null : names.get(id);
                    out.put(u.orderId(), name == null ? id : name);
                    break;
                }
            }
        }
        return out;
    }

    /// One row of a pick list: a SKU at a location, how many in all, and the
    /// orders they go to.
    static final class PickLine {
        final String location;
        final String sku;
        final String title;
        int quantity;
        final List<Integer> orders = new ArrayList<Integer>();

        PickLine(String location, String sku, String title) {
            this.location = location;
            this.sku = sku;
            this.title = title;
        }
    }

    /// What has to be picked for the orders in `orderIds` (every order in the
    /// queue when it is null): each SKU once per location, its open
    /// quantities added up across the orders, sorted by location and then
    /// SKU so the picker walks the shelves once. A line without a SKU is
    /// told apart by its title.
    static List<PickLine> pickList(List<UnfulfilledDto> queue, Collection<String> orderIds,
            Map<String, String> locationNames) {
        Map<String, PickLine> byKey = new HashMap<String, PickLine>();
        List<PickLine> out = new ArrayList<PickLine>();
        for (int iter = 0; queue != null && iter < queue.size(); iter++) {
            UnfulfilledDto u = queue.get(iter);
            if (orderIds != null && !orderIds.contains(u.orderId())) {
                continue;
            }
            for (int at = 0; u.lines() != null && at < u.lines().size(); at++) {
                OpenLineDto l = u.lines().get(at);
                if (l.open() <= 0) {
                    continue;
                }
                String location = l.locationId() == null ? "" : l.locationId();
                String named = locationNames == null ? null : locationNames.get(location);
                if (named != null) {
                    location = named;
                }
                String sku = l.sku() == null ? "" : l.sku();
                String title = l.title() == null ? "" : l.title();
                if (l.variantTitle() != null && l.variantTitle().length() > 0
                        && !"Default".equals(l.variantTitle())) {
                    title = title + " / " + l.variantTitle();
                }
                String key = location + "\n" + (sku.length() > 0 ? sku : "\n" + title);
                PickLine p = byKey.get(key);
                if (p == null) {
                    p = new PickLine(location, sku, title);
                    byKey.put(key, p);
                    insert(out, p);
                }
                p.quantity += l.open();
                Integer n = Integer.valueOf(u.number());
                if (!p.orders.contains(n)) {
                    p.orders.add(n);
                }
            }
        }
        return out;
    }

    private static void insert(List<PickLine> out, PickLine p) {
        int at = 0;
        while (at < out.size() && before(out.get(at), p)) {
            at++;
        }
        out.add(at, p);
    }

    private static boolean before(PickLine a, PickLine b) {
        int c = a.location.compareTo(b.location);
        if (c != 0) {
            return c < 0;
        }
        c = a.sku.compareTo(b.sku);
        if (c != 0) {
            return c < 0;
        }
        return a.title.compareTo(b.title) <= 0;
    }

    /// The total units of a pick list.
    static int units(List<PickLine> lines) {
        int n = 0;
        for (int iter = 0; iter < lines.size(); iter++) {
            n += lines.get(iter).quantity;
        }
        return n;
    }
}
