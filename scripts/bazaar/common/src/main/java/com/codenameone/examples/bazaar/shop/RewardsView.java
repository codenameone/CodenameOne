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

import com.codename1.appkit.Net;
import com.codename1.io.Preferences;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Label;
import com.codename1.ui.animations.Motion;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.ProductCardDto;
import com.codenameone.examples.bazaar.api.ProductPageDto;
import com.codenameone.examples.bazaar.api.RewardsDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.ProductArt;
import com.codenameone.examples.bazaar.ui.Ui;

/// The shopper's rewards on screen: the tier ring, points and badges on the
/// Account tab, the points earned and any badge unlocked after checkout, and
/// the daily drop on the home page. The numbers are the server's
/// ([RewardsDto]); the rules are [Rewards].
final class RewardsView {
    private static final char[] BADGE_ICONS = {FontImage.MATERIAL_SHOPPING_BAG,
        FontImage.MATERIAL_RATE_REVIEW, FontImage.MATERIAL_WORKSPACE_PREMIUM,
        FontImage.MATERIAL_EXPLORE};
    private static final String SEEN_BADGES = "bazaar.rewards.badges";
    private static final String DROP_DAY = "bazaar.drop.day";
    private static final String DROP_STREAK = "bazaar.drop.streak";

    private RewardsView() {
    }

    private static String points(long n) {
        return Lang.tr("{0} pts", String.valueOf(n));
    }

    /// The rewards card of the Account tab, filled when the answer comes. A
    /// badge earned since the tab last showed is unlocked with a pop.
    static Container account() {
        final Container card = new Container(BoxLayout.y());
        card.setUIID("ShopRewards");
        card.setName("rewards");
        card.add(new com.codenameone.examples.bazaar.ui.Shimmer(2, "SvSkeleton"));
        Api.rewards().mine("", Net.to(r -> fillAccount(card, r), (status, message) ->
                card.setHidden(true)));
        return card;
    }

    private static void fillAccount(Container card, final RewardsDto r) {
        card.removeAll();
        int fill = Rewards.tierProgress(r.qualifying(), r.toNextTier());
        Container top = new Container(new BorderLayout());
        // The tier is earned by spend, so the ring (a share of it) and the lines
        // under the tier are all in money; points are the balance to spend,
        // and sit apart on their own row so the two never read as one unit.
        Ring ring = new Ring(fill, null);
        top.add(BorderLayout.WEST, ring);
        Container words = new Container(BoxLayout.y());
        words.setUIID("ShopRewardsWords");
        words.add(Ui.plain(r.tierName() == null || r.tierName().length() == 0
                ? Lang.tr("Member") : r.tierName(), "ShopRewardsTierB"));
        // Tier and points are the best store's own: each store runs its own
        // programme, so the card names it rather than adding stores up.
        if (r.storeName() != null && r.storeName().length() > 0) {
            Label at = Ui.plain(Lang.tr("at {0}", r.storeName()), "ShopRewardsSpent");
            at.setName("rewards-store");
            words.add(at);
        }
        boolean hasNext = r.nextTierName() != null && r.nextTierName().length() > 0
                && r.toNextTier() > 0;
        words.add(Ui.text(hasNext
                ? Lang.tr("Spend {0} more to reach {1}", Money.whole(r.toNextTier() + 99),
                        r.nextTierName())
                : Lang.tr("Top tier: every perk is yours"), "ShopRewardsNextB"));
        words.add(Ui.plain(Lang.tr("{0} spent this year", Money.whole(r.qualifying())),
                "ShopRewardsSpent"));
        top.add(BorderLayout.CENTER, words);
        card.add(top);
        Container balance = new Container(new BorderLayout());
        balance.setUIID("ShopRewardsBalance");
        Label star = new Label("", "ShopPointsIcon");
        FontImage.setMaterialIcon(star, FontImage.MATERIAL_STARS, 3.4f);
        balance.add(BorderLayout.WEST, star);
        Container spend = new Container(BoxLayout.y());
        // The balance is the best store's alone (RewardsDto): every store
        // runs its own programme and points are never summed, so the label
        // names the store rather than reading as a total.
        boolean named = r.storeName() != null && r.storeName().length() > 0;
        spend.add(Ui.plain(named ? Lang.tr("Points to spend at {0}", r.storeName())
                : Lang.tr("Points to spend"), "ShopRewardsBalanceLabel"));
        long others = Rewards.otherPoints(r);
        if (others > 0) {
            Label more = Ui.plain(Lang.tr("+{0} pts at other stores", String.valueOf(others)),
                    "ShopRewardsSpent");
            more.setName("rewards-other-stores");
            spend.add(more);
        }
        balance.add(BorderLayout.CENTER, spend);
        final Label pts = Ui.plain(points(0), "ShopRewardsBalancePts");
        pts.setName("rewards-points");
        balance.add(BorderLayout.EAST, pts);
        card.add(balance);

        boolean[] have = Rewards.badges(r);
        int before = Preferences.get(SEEN_BADGES, -1);
        int mask = 0;
        for (int iter = 0; iter < have.length; iter++) {
            mask |= have[iter] ? 1 << iter : 0;
        }
        Preferences.set(SEEN_BADGES, mask);
        Container badges = new Container(new GridLayout(1, Rewards.TITLES.length));
        badges.setUIID("ShopBadges");
        final Label[] icons = new Label[have.length];
        for (int iter = 0; iter < have.length; iter++) {
            Container b = new Container(BoxLayout.y());
            b.setUIID("ShopBadgeTile");
            b.setName("badge-" + iter + (have[iter] ? "-on" : "-off"));
            Label icon = new Label("", have[iter] ? "ShopBadgeOn" : "ShopBadgeOff");
            FontImage.setMaterialIcon(icon, have[iter] ? BADGE_ICONS[iter]
                    : FontImage.MATERIAL_LOCK, 4.4f);
            icons[iter] = icon;
            b.add(com.codename1.ui.layouts.FlowLayout.encloseCenter(icon));
            b.add(Ui.label(Rewards.TITLES[iter], "ShopBadgeName"));
            if (have[iter]) {
                b.add(Ui.label(Lang.tr("Unlocked"), "ShopBadgeHint"));
            } else {
                // Wraps rather than cutting the hint off in a narrow tile.
                com.codename1.components.SpanLabel hint = Ui.text(
                        Lang.tr(Rewards.SHORT_HINTS[iter]), "ShopBadgeHint");
                hint.setTextBlockAlign(Component.CENTER);
                b.add(hint);
                String count = Rewards.badgeCount(iter, r.orders(), r.reviews(), r.stores());
                if (count != null) {
                    Label c = Ui.plain(count, "ShopBadgeCount");
                    c.setName("badge-count-" + iter);
                    b.add(c);
                }
            }
            badges.add(b);
        }
        card.add(badges);
        Ui.relayout(card);
        ShopFx.countUp(pts, 0, r.points(), 400, RewardsView::points);
        // Newly unlocked since the last look: pop it, with a burst over it.
        final int fresh = before < 0 ? -1 : Rewards.unlocked(bits(before, have.length), have);
        if (fresh >= 0) {
            CN.callSerially(() -> {
                ShopFx.pop(icons[fresh]);
                Form f = icons[fresh].getComponentForm();
                if (f != null) {
                    ShopFx.burst(f, icons[fresh], Lang.tr("Unlocked"));
                }
            });
        }
    }

    private static boolean[] bits(int mask, int n) {
        boolean[] out = new boolean[n];
        for (int iter = 0; iter < n; iter++) {
            out[iter] = (mask & (1 << iter)) != 0;
        }
        return out;
    }

    /// After checkout: the points this order earns, counted up with a burst,
    /// and the badge it unlocked (the first order's welcome among them)
    /// revealed under it.
    static Container placed(final OrderDto order) {
        final Container card = new Container(BoxLayout.y());
        card.setUIID("ShopRewardsPlaced");
        card.setName("order-rewards");
        Api.rewards().mine(order.id(), Net.to(r -> {
            if (r.orderPoints() <= 0 && r.orders() == 0) {
                card.setHidden(true);
                Ui.relayout(card);
                return;
            }
            Container row = new Container(new BorderLayout());
            Label star = new Label("", "ShopPointsIcon");
            FontImage.setMaterialIcon(star, FontImage.MATERIAL_STARS, 5f);
            row.add(BorderLayout.WEST, star);
            Container words = new Container(BoxLayout.y());
            final Label earned = Ui.plain("+" + points(0), "ShopPointsEarned");
            earned.setName("points-earned");
            words.add(earned);
            words.add(Ui.text(Lang.tr("Added when your order ships. Balance after: {0}",
                    points(r.orderBalance())), "ShopPointsNote"));
            row.add(BorderLayout.CENTER, words);
            card.add(row);
            boolean[] now = Rewards.badges(r);
            boolean[] was = Rewards.badges(Math.max(0, r.orders() - 1), r.reviews(),
                    r.orders() <= 1 ? 0 : r.stores());
            final int unlocked = Rewards.unlocked(was, now);
            final Container reveal = new Container(new BorderLayout());
            if (unlocked >= 0) {
                reveal.setUIID("ShopReveal");
                reveal.setName("reward-reveal");
                Label icon = new Label("", "ShopBadgeOn");
                FontImage.setMaterialIcon(icon, BADGE_ICONS[unlocked], 6f);
                reveal.add(BorderLayout.WEST, icon);
                Container w = new Container(BoxLayout.y());
                w.add(Ui.plain(Lang.tr("Badge unlocked"), "ShopRevealKicker"));
                w.add(Ui.plain(Rewards.TITLES[unlocked], "ShopRevealTitle"));
                w.add(Ui.text(unlocked == Rewards.FIRST_ORDER
                        ? Lang.tr("Welcome to Bazaar. Your first order earns double points.")
                        : Lang.tr(Rewards.HINTS[unlocked]), "ShopRevealText"));
                reveal.add(BorderLayout.CENTER, w);
                reveal.setVisible(false);
                card.add(reveal);
            }
            Ui.relayout(card);
            ShopFx.countUp(earned, 0, r.orderPoints(), 400, n -> "+" + points(n));
            final Form f = card.getComponentForm();
            if (f != null) {
                ShopFx.burst(f, star, "+" + points(r.orderPoints()));
                if (unlocked >= 0) {
                    // The reveal follows the count, a beat later.
                    CN.setTimeout(500, () -> {
                        reveal.setVisible(true);
                        card.animateLayoutFade(300, 0);
                        ShopFx.confetti(f);
                    });
                }
            }
        }, (status, message) -> {
            card.setHidden(true);
            Ui.relayout(card);
        }));
        return card;
    }

    /// The daily drop: one product a day from the feed, with the streak of
    /// days in a row the shopper opened it. `into` is filled and shown.
    static void dailyDrop(final Container into, ProductPageDto feed) {
        into.removeAll();
        if (feed == null || feed.items() == null || feed.items().isEmpty()) {
            into.setHidden(true);
            return;
        }
        long today = System.currentTimeMillis() / 86400000L;
        int at = Rewards.dropIndex(today, feed.items().size());
        final ProductCardDto p = feed.items().get(at);
        long last = Preferences.get(DROP_DAY, 0L);
        int streak = Rewards.streak(last, today, Preferences.get(DROP_STREAK, 0));
        Preferences.set(DROP_DAY, today);
        Preferences.set(DROP_STREAK, streak);
        Container card = new Container(new BorderLayout());
        card.setUIID("ShopDrop");
        int side = CN.convertToPixels(22);
        card.add(BorderLayout.WEST, ProductArt.label(p.id(), p.title(), p.productType(),
                p.photoId(), side, side, "ShopDropPhoto"));
        Container words = new Container(BoxLayout.y());
        words.setUIID("ShopDropWords");
        Label kicker = Ui.plain(Lang.tr("Today's drop"), "ShopDropKicker");
        words.add(kicker);
        words.add(Ui.text(p.title(), "ShopDropTitle"));
        words.add(Ui.plain(ShopState.money(p.storeId(), p.minPriceCents()), "ShopDropPrice"));
        // One short line at every streak; the long day-one sentence was cut off.
        Label fire = Ui.plain(Lang.tr("{0}-day streak", String.valueOf(streak)),
                "ShopDropStreak");
        FontImage.setMaterialIcon(fire, FontImage.MATERIAL_LOCAL_FIRE_DEPARTMENT, 3f);
        words.add(fire);
        card.add(BorderLayout.CENTER, words);
        into.add(Ui.tap(card, "daily-drop", e -> ProductCard.open(p)));
        into.setHidden(false);
        Ui.relayout(into);
    }

    /// The tier ring: a track and an arc that fills to `fill` thousandths in
    /// 400 ms once it is on screen.
    static final class Ring extends Component {
        private final int fill;
        private final String figure;
        private Motion motion;

        Ring(int fill, String figure) {
            this.fill = fill;
            this.figure = figure;
            setUIID("ShopRing");
            setName("tier-ring");
        }

        @Override
        protected Dimension calcPreferredSize() {
            int s = CN.convertToPixels(19);
            return new Dimension(s, s);
        }

        @Override
        protected void initComponent() {
            super.initComponent();
            if (motion == null && fill > 0) {
                motion = Motion.createEaseOutMotion(0, fill, 400);
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
            if (motion != null && motion.isFinished()) {
                getComponentForm().deregisterAnimated(this);
                repaint();
                return false;
            }
            return motion != null;
        }

        @Override
        public void paint(Graphics g) {
            Style track = UIManager.getInstance().getComponentStyle("ShopRingTrack");
            Style arc = UIManager.getInstance().getComponentStyle("ShopRingFill");
            int pad = CN.convertToPixels(1.5f);
            int s = Math.min(getWidth(), getHeight()) - pad * 2;
            int x = getX() + (getWidth() - s) / 2;
            int y = getY() + (getHeight() - s) / 2;
            int thick = CN.convertToPixels(2.2f);
            boolean aa = g.isAntiAliased();
            g.setAntiAliased(true);
            g.setColor(track.getFgColor());
            g.fillArc(x, y, s, s, 0, 360);
            int at = motion == null ? fill : motion.getValue();
            g.setColor(arc.getFgColor());
            g.fillArc(x, y, s, s, 90, -(int) (360L * at / 1000));
            g.setColor(getStyle().getBgColor());
            g.fillArc(x + thick, y + thick, s - thick * 2, s - thick * 2, 0, 360);
            String pct = figure != null ? figure : (at / 10) + "%";
            g.setColor(arc.getFgColor());
            g.setFont(arc.getFont());
            g.drawString(pct, x + (s - arc.getFont().stringWidth(pct)) / 2,
                    y + (s - arc.getFont().getHeight()) / 2);
            g.setAntiAliased(aa);
        }
    }
}
