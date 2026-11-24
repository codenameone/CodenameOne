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
package com.codenameone.examples.bazaar.shop;

import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.animations.Motion;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import com.codenameone.examples.bazaar.api.FulfillmentSummaryDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.List;

/// The top of an order's screen: where the parcel is on a drawn route from
/// the store to the door, and the six stages from placed to delivered.
///
/// Built again whenever the order changes. Given the stage the shopper last
/// saw, it moves the parcel along the route from there, pops the new stage's
/// tick and drops in the banner for it -- and celebrates Delivered -- once it
/// is on screen, so an event that arrives over the live socket is seen to
/// happen rather than simply appearing.
final class TrackerView {
    private static final char[] ICONS = {FontImage.MATERIAL_RECEIPT_LONG,
        FontImage.MATERIAL_CREDIT_CARD, FontImage.MATERIAL_INVENTORY_2,
        FontImage.MATERIAL_LOCAL_SHIPPING, FontImage.MATERIAL_DIRECTIONS_BIKE,
        FontImage.MATERIAL_HOME};

    private TrackerView() {
    }

    /// The tracker of `order`, animated from stage `before` (the stage now
    /// for no animation, -2 for an order seen for the first time).
    static Container create(final OrderDto order, final int before) {
        final int now = TrackStages.reached(order);
        final Label[] ticks = new Label[TrackStages.LABELS.length];
        final boolean moved = before >= TrackStages.PLACED && now > before;
        Container box = new Container(BoxLayout.y()) {
            private boolean played;

            @Override
            protected void initComponent() {
                super.initComponent();
                if (played || !moved) {
                    return;
                }
                played = true;
                final Form form = getComponentForm();
                CN.callSerially(() -> {
                    if (ticks[now] != null) {
                        ShopFx.pop(ticks[now]);
                    }
                    String banner = TrackStages.banner(before, now);
                    if (banner != null && form != null) {
                        ShopFx.banner(form, ICONS[now], banner,
                                Lang.tr("Order #{0}", String.valueOf(order.number())));
                    }
                    if (now == TrackStages.DELIVERED && form != null) {
                        ShopFx.confetti(form);
                    }
                });
            }
        };
        box.setName("tracker");
        Container hero = new Container(BoxLayout.y());
        hero.setUIID("ShopTrackHero");
        hero.setName(now == TrackStages.DELIVERED ? "track-delivered" : "track-stage-" + now);
        hero.add(Ui.label(TrackStages.LABELS[Math.max(0, now)], now == TrackStages.DELIVERED
                ? "ShopTrackTitleDone" : "ShopTrackTitle"));
        hero.add(Ui.plain(subtitle(order, now), "ShopTrackSub"));
        int from = TrackStages.progress(moved ? before : now);
        hero.add(new Route(from, TrackStages.progress(now), now));
        box.add(hero);

        Container steps = new Container(BoxLayout.y());
        steps.setUIID("ShopTrackSteps");
        long[] times = TrackStages.times(order);
        int last = TrackStages.LABELS.length - 1;
        for (int iter = 0; iter <= last; iter++) {
            boolean done = iter <= now;
            boolean current = iter == now;
            Container row = new Container(new BorderLayout());
            row.setUIID("ShopTrackStep");
            row.setName("track-step-" + iter);
            Container rail = new Container(new BorderLayout());
            rail.setUIID("ShopScanRail");
            Label tick = new Label("", current ? "ShopTrackTickNow" : done ? "ShopTrackTick"
                    : "ShopTrackTickTodo");
            FontImage.setMaterialIcon(tick, done ? FontImage.MATERIAL_CHECK_CIRCLE : ICONS[iter],
                    3.4f);
            ticks[iter] = tick;
            rail.add(BorderLayout.NORTH, tick);
            if (iter < last) {
                // The connector down to the next dot: solid where the order
                // has been, the track's colour where it has yet to go.
                rail.add(BorderLayout.CENTER, new Connector(iter < now));
            }
            row.add(BorderLayout.WEST, rail);
            Container words = new Container(BoxLayout.y());
            words.setUIID("ShopTrackWords");
            words.add(Ui.plain(Lang.tr(TrackStages.LABELS[iter]), current ? "ShopTrackStepNow"
                    : done ? "ShopTrackStepDone" : "ShopTrackStepTodo"));
            if (done && times[iter] > 0) {
                words.add(Ui.plain(Ui.when(times[iter]), "ShopTrackDetail"));
            }
            String carrier = iter == TrackStages.SHIPPED && done ? carrier(order) : null;
            if (carrier != null) {
                words.add(Ui.plain(carrier, "ShopTrackDetail"));
            }
            if (iter == TrackStages.DELIVERED && !done) {
                String eta = eta(now);
                if (eta != null) {
                    Label l = Ui.plain(eta, "ShopTrackEta");
                    l.setName("track-eta");
                    words.add(l);
                }
            }
            row.add(BorderLayout.CENTER, words);
            steps.add(row);
        }
        box.add(steps);
        return box;
    }

    private static String subtitle(OrderDto order, int now) {
        FulfillmentSummaryDto f = parcel(order);
        switch (now) {
            case TrackStages.PLACED:
                return Lang.tr("Waiting for the payment to clear");
            case TrackStages.PAID:
                return Lang.tr("The store is getting it ready");
            case TrackStages.PACKED:
                return Lang.tr("Packed and waiting for the carrier");
            case TrackStages.DELIVERED:
                return Lang.tr("Left at your door. Enjoy!");
            default:
                String c = now == TrackStages.OUT_FOR_DELIVERY ? eta(now) : carrier(order);
                if (c != null) {
                    return c;
                }
                return Lang.tr(now == TrackStages.OUT_FOR_DELIVERY ? "Arriving today"
                        : "On its way");
        }
    }

    /// The parcel's carrier and number as a shopper reads them, or null.
    private static String carrier(OrderDto order) {
        FulfillmentSummaryDto f = parcel(order);
        if (f == null) {
            return null;
        }
        String name = TrackStages.carrierName(f.carrier());
        String number = TrackStages.trackingNumber(f.trackingNumber());
        if (number.length() == 0) {
            return name.length() == 0 ? null : name;
        }
        return name.length() == 0 ? number : name + Ui.DOT + number;
    }

    /// "Arriving today, 4-6 pm" for an order at stage `now`, or null.
    static String eta(int now) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTime(new java.util.Date());
        int[] w = TrackStages.window(now, c.get(java.util.Calendar.HOUR_OF_DAY));
        if (w == null) {
            return null;
        }
        String span = TrackStages.span(w[0], w[1]);
        if (w[2] == 0) {
            return Lang.tr("Arriving today, {0}", span);
        }
        if (w[2] == 1) {
            return Lang.tr("Arriving tomorrow, {0}", span);
        }
        return Lang.tr("Arriving in {0} days", String.valueOf(w[2]));
    }

    private static FulfillmentSummaryDto parcel(OrderDto order) {
        List<FulfillmentSummaryDto> fs = order.fulfillments();
        if (fs == null) {
            return null;
        }
        for (int iter = 0; iter < fs.size(); iter++) {
            if (!"CANCELLED".equals(fs.get(iter).status())) {
                return fs.get(iter);
            }
        }
        return null;
    }

    /// The vertical line between two step dots.
    static final class Connector extends Component {
        private final boolean done;

        Connector(boolean done) {
            this.done = done;
            setUIID("ShopTrackLink");
        }

        @Override
        protected Dimension calcPreferredSize() {
            return new Dimension(CN.convertToPixels(3.4f), CN.convertToPixels(2));
        }

        @Override
        public void paint(Graphics g) {
            Style st = UIManager.getInstance().getComponentStyle(done ? "ShopRouteDone"
                    : "ShopRouteLine");
            int w = Math.max(2, CN.convertToPixels(0.5f));
            int gap = CN.convertToPixels(0.6f);
            g.setColor(st.getFgColor());
            g.fillRect(getX() + (getWidth() - w) / 2, getY() + gap, w,
                    Math.max(0, getHeight() - gap * 2));
        }
    }

    /// The route from the store to the door, drawn: a gentle arc, done up to
    /// the parcel, the parcel on it, a stop for every stage. Moves the parcel
    /// from `from` to `to` (thousandths) in 400 ms once it is on screen.
    static final class Route extends Component {
        private final int from;
        private final int to;
        private final int stage;
        private Motion motion;

        Route(int from, int to, int stage) {
            this.from = from;
            this.to = to;
            this.stage = stage;
            setUIID("ShopRoute");
            setName("track-route");
        }

        @Override
        protected Dimension calcPreferredSize() {
            return new Dimension(CN.convertToPixels(60), CN.convertToPixels(24));
        }

        @Override
        protected void initComponent() {
            super.initComponent();
            if (from != to && motion == null) {
                motion = Motion.createEaseInOutMotion(from, to, 400);
                motion.start();
                getComponentForm().registerAnimated(this);
            }
        }

        @Override
        protected void deinitialize() {
            Form f = getComponentForm();
            if (f != null) {
                f.deregisterAnimated(this);
            }
            super.deinitialize();
        }

        @Override
        public boolean animate() {
            if (motion == null) {
                return false;
            }
            if (motion.isFinished()) {
                getComponentForm().deregisterAnimated(this);
                repaint();
                return false;
            }
            return true;
        }

        private int at() {
            return motion == null ? to : motion.getValue();
        }

        @Override
        public void paint(Graphics g) {
            Style line = UIManager.getInstance().getComponentStyle("ShopRouteLine");
            Style done = UIManager.getInstance().getComponentStyle("ShopRouteDone");
            // Wide enough for the marker's halo at either end.
            int pad = CN.convertToPixels(7);
            int x0 = getX() + pad;
            int x1 = getX() + getWidth() - pad;
            int base = getY() + getHeight() * 3 / 4;
            int lift = getHeight() / 2;
            boolean aa = g.isAntiAliased();
            g.setAntiAliased(true);
            int thick = Math.max(2, CN.convertToPixels(0.7f));
            int progress = at();
            // The route as round dots: close together (a smooth line) where the
            // parcel has been, spaced out (dotted) where it has yet to go.
            int steps = 240;
            int fine = Math.max(1, thick / 2);
            for (int iter = 0; iter <= steps; iter++) {
                int t = iter * 1000 / steps;
                boolean isDone = t <= progress;
                if (!isDone && iter % 6 != 0) {
                    continue;
                }
                int x = x0 + (x1 - x0) * t / 1000;
                int y = base - (int) (lift * Math.sin(Math.PI * t / 1000.0));
                int d = isDone ? thick : fine * 2;
                g.setColor(isDone ? done.getFgColor() : line.getFgColor());
                g.fillArc(x - d / 2, y - d / 2, d, d, 0, 360);
            }
            int dot = CN.convertToPixels(1.6f);
            for (int s = 1; s < TrackStages.LABELS.length - 1; s++) {
                int t = TrackStages.progress(s);
                int x = x0 + (x1 - x0) * t / 1000;
                int y = base - (int) (lift * Math.sin(Math.PI * t / 1000.0));
                g.setColor(t <= progress ? done.getFgColor() : line.getFgColor());
                g.fillArc(x - dot / 2, y - dot / 2, dot, dot, 0, 360);
            }
            Image store = FontImage.createMaterial(FontImage.MATERIAL_STOREFRONT, done, 3.6f);
            Image home = FontImage.createMaterial(FontImage.MATERIAL_HOME,
                    stage == TrackStages.DELIVERED ? done : line, 3.6f);
            g.drawImage(store, x0 - store.getWidth() / 2, base - store.getHeight() - dot);
            g.drawImage(home, x1 - home.getWidth() / 2, base - home.getHeight() - dot);
            // The parcel: a disc in the accent with the van on it.
            int x = x0 + (x1 - x0) * progress / 1000;
            int y = base - (int) (lift * Math.sin(Math.PI * progress / 1000.0));
            int r = CN.convertToPixels(7);
            Style marker = UIManager.getInstance().getComponentStyle("ShopRouteMarker");
            g.setColor(marker.getBgColor());
            int old = g.getAlpha();
            g.setAlpha(60);
            g.fillArc(x - r * 3 / 4, y - r * 3 / 4, r * 3 / 2, r * 3 / 2, 0, 360);
            g.setAlpha(old);
            g.fillArc(x - r / 2, y - r / 2, r, r, 0, 360);
            Image van = FontImage.createMaterial(stage == TrackStages.DELIVERED
                    ? FontImage.MATERIAL_CHECK : FontImage.MATERIAL_LOCAL_SHIPPING, marker, 2.8f);
            g.drawImage(van, x - van.getWidth() / 2, y - van.getHeight() / 2);
            g.setAntiAliased(aa);
        }
    }
}
