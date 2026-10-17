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
package com.codenameone.examples.savor.customer;

import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Graphics;
import com.codename1.ui.Label;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codenameone.examples.savor.api.OrderState;
import com.codenameone.examples.savor.api.RestaurantSummaryDto;
import com.codenameone.examples.savor.ui.Lang;
import com.codenameone.examples.savor.ui.Ui;

/// What the customer's screens share: the cuisines, a restaurant as a card, and
/// an order's state in words.
public final class Food {
    /// The cuisines of the home screen's row, in its order: the server's
    /// `DemoRestaurants.CUISINES`, which a restaurant is filed under.
    public static final String[] CUISINES = {"Pizza", "Burgers", "Sushi", "Chicken", "Kebab",
        "Rice", "Steak", "Tacos", "Salads", "Donuts", "Dessert", "Coffee"};
    private static final char[] ICONS = {FontImage.MATERIAL_LOCAL_PIZZA,
        FontImage.MATERIAL_LUNCH_DINING, FontImage.MATERIAL_SET_MEAL,
        FontImage.MATERIAL_DINNER_DINING, FontImage.MATERIAL_KEBAB_DINING,
        FontImage.MATERIAL_RICE_BOWL, FontImage.MATERIAL_OUTDOOR_GRILL, FontImage.MATERIAL_TAPAS,
        FontImage.MATERIAL_ECO, FontImage.MATERIAL_BAKERY_DINING, FontImage.MATERIAL_ICECREAM,
        FontImage.MATERIAL_LOCAL_CAFE};

    private Food() {
    }

    /// The icon of `cuisine`, or a plate for one the row does not have.
    public static char icon(String cuisine) {
        for (int iter = 0; iter < CUISINES.length; iter++) {
            if (CUISINES[iter].equalsIgnoreCase(cuisine)) {
                return ICONS[iter];
            }
        }
        return FontImage.MATERIAL_RESTAURANT;
    }

    /// A photo's place: the cuisine's icon on a tinted ground until the photo
    /// comes, and for good when there is none.
    public static Label photo(String photoId, String cuisine, String uiid, float widthMM,
            float heightMM) {
        Label photo = new Cover(uiid, cover(cuisine));
        // Light on the cover's dark ground, in either theme.
        photo.getAllStyles().setFgColor(COVER_INK);
        Ui.icon(photo, icon(cuisine), Math.min(heightMM, widthMM) * 0.42f);
        photo.setAlignment(Component.CENTER);
        int width = CN.convertToPixels(widthMM);
        int height = CN.convertToPixels(heightMM);
        photo.setPreferredW(width);
        photo.setPreferredH(height);
        Pictures.into(photo, photoId, width, height);
        return photo;
    }

    /// Two deep tones per cuisine, top to bottom, for the cover a restaurant
    /// or a dish has until it has a photograph: dark and warm, the way a
    /// menu's own photography is lit, rather than a pale tint behind an icon.
    private static final int[][] COVERS = {
        {0x7a2e1d, 0x2b1410}, {0x6b3f1f, 0x24160d}, {0x1f3d4a, 0x0e1a20},
        {0x6e4a1c, 0x251a0c}, {0x5c2a2a, 0x1f0f10}, {0x3d4a2a, 0x151a0f},
        {0x4a2418, 0x170c09}, {0x6b5317, 0x241c08}, {0x2f4f35, 0x0f1c12},
        {0x6a3350, 0x22111b}, {0x4d2e4a, 0x1a0f19}, {0x4a3424, 0x1a120c}};

    private static final int COVER_INK = 0xf3e7d3;

    private static int[] cover(String cuisine) {
        for (int iter = 0; iter < CUISINES.length; iter++) {
            if (CUISINES[iter].equalsIgnoreCase(cuisine)) {
                return COVERS[iter % COVERS.length];
            }
        }
        return new int[] {0x33302c, 0x121110};
    }

    /// A photo's place that paints its cover itself while it shows an icon,
    /// and steps aside once [Pictures] has given it the photograph.
    private static final class Cover extends Label {
        private final int[] tones;

        Cover(String uiid, int[] tones) {
            super("", uiid);
            this.tones = tones;
        }

        @Override
        public void paint(Graphics g) {
            if (getIcon() instanceof FontImage) {
                int x = getX();
                int y = getY();
                int w = getWidth();
                int h = getHeight();
                int[] clip = g.getClip();
                // A hero runs edge to edge; everything else is a card.
                boolean rounded = !"SvHeroPhoto".equals(getUIID()) && g.isShapeClipSupported();
                if (rounded) {
                    float r = CN.convertToPixels(2f);
                    GeneralPath shape = new GeneralPath();
                    shape.moveTo(x + r, y);
                    shape.lineTo(x + w - r, y);
                    shape.quadTo(x + w, y, x + w, y + r);
                    shape.lineTo(x + w, y + h - r);
                    shape.quadTo(x + w, y + h, x + w - r, y + h);
                    shape.lineTo(x + r, y + h);
                    shape.quadTo(x, y + h, x, y + h - r);
                    shape.lineTo(x, y + r);
                    shape.quadTo(x, y, x + r, y);
                    shape.closePath();
                    g.setClip(shape);
                }
                g.fillLinearGradient(tones[0], tones[1], x, y, w, h, false);
                g.setClip(clip);
            }
            super.paint(g);
        }
    }

    /// `$`, `$$` or `$$$`.
    public static String priceLevel(int level) {
        StringBuilder signs = new StringBuilder();
        for (int iter = 0; iter < Math.max(1, Math.min(4, level)); iter++) {
            signs.append('$');
        }
        return signs.toString();
    }

    /// The line under a restaurant's name: rating, time, fee.
    public static String meta(double rating, long ratings, int eta, long feeCents,
            String currency) {
        String stars = ratings == 0 ? Lang.tr("New") : Ui.tenths(rating) + " (" + ratings + ")";
        return stars + Ui.DOT + Lang.tr("{0} min", String.valueOf(eta)) + Ui.DOT
                + (feeCents == 0 ? Lang.tr("Free delivery")
                        : Lang.tr("{0} delivery", Ui.money(feeCents, currency)));
    }

    /// A restaurant in a list: its photo across the top, its name, and what
    /// it costs to have it come.
    public static Container card(final RestaurantSummaryDto restaurant, String currency,
            String name, ActionListener<ActionEvent> open) {
        Container card = new Container(new BorderLayout());
        card.setUIID("SvRestaurantCard");
        Container top = new Container(new LayeredLayout());
        Label photo = photo(restaurant.photoId(), restaurant.cuisine(), "SvRestaurantPhoto",
                86f, 30f);
        top.add(photo);
        if (restaurant.favorite()) {
            Label heart = new Label("", "SvPhotoBadge");
            Ui.icon(heart, FontImage.MATERIAL_FAVORITE, 3.6f);
            top.add(heart);
            ((LayeredLayout) top.getLayout()).setInsets(heart, "1.5mm 1.5mm auto auto");
        }
        if (!restaurant.open()) {
            Label closed = new Label(Lang.tr("Closed"), "SvClosedBadge");
            top.add(closed);
            ((LayeredLayout) top.getLayout()).setInsets(closed, "auto auto 1.5mm 1.5mm");
        }
        card.add(BorderLayout.NORTH, top);
        Container words = new Container(BoxLayout.y());
        words.setUIID("SvRestaurantWords");
        Container title = new Container(new BorderLayout());
        title.add(BorderLayout.CENTER, Ui.plain(restaurant.name(), "SvRestaurantName"));
        title.add(BorderLayout.EAST, Ui.plain(priceLevel(restaurant.priceLevel()), "SvMutedSmall"));
        words.add(title);
        words.add(Ui.plain(restaurant.cuisine() + Ui.DOT + Ui.distance(restaurant.distanceMeters()),
                "SvMutedSmall"));
        words.add(Ui.text(meta(restaurant.rating(), restaurant.ratingCount(),
                restaurant.etaMinutes(), restaurant.deliveryFeeCents(), currency), "SvMetaLine"));
        card.add(BorderLayout.CENTER, words);
        return Ui.tap(card, name, open);
    }

    /// Where an order is, in a few words, for the customer.
    public static String state(OrderState state) {
        if (state == null) {
            return "";
        }
        switch (state) {
            case PLACED:
                return Lang.tr("Waiting for the restaurant");
            case ACCEPTED:
                return Lang.tr("Being prepared");
            case READY:
                return Lang.tr("Ready, waiting for the courier");
            case PICKED_UP:
                return Lang.tr("On its way");
            case DELIVERED:
                return Lang.tr("Delivered");
            case REJECTED:
                return Lang.tr("The restaurant could not take it");
            case NO_COURIERS:
                return Lang.tr("No courier was free");
            case CANCELLED_BY_CUSTOMER:
                return Lang.tr("You cancelled it");
            default:
                return Lang.tr("Cancelled");
        }
    }

    /// Which of the tracker's four steps `state` has reached: 0 placed, 1
    /// cooking, 2 on its way, 3 delivered; -1 for an order that ended without
    /// arriving.
    public static int step(OrderState state) {
        if (state == OrderState.PLACED) {
            return 0;
        }
        if (state == OrderState.ACCEPTED || state == OrderState.READY) {
            return 1;
        }
        if (state == OrderState.PICKED_UP) {
            return 2;
        }
        return state == OrderState.DELIVERED ? 3 : -1;
    }

    /// Whether the order is still coming.
    public static boolean active(OrderState state) {
        return state != null && state.isActive();
    }
}
