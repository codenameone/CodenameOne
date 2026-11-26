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
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.Painter;
import com.codename1.ui.animations.Animation;
import com.codename1.ui.animations.Motion;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.layouts.FlowLayout;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Ui;

/// Small pieces the shopper's screens share: stars,
/// prices, a tap of feedback, and the flight of a product into the cart.
final class ShopUi {
    private ShopUi() {
    }

    /// Five stars for `rating` (stars times ten) and the count of reviews;
    /// nothing at all without reviews.
    static Container stars(int rating, int count) {
        Container row = new Container(new FlowLayout(Component.LEFT, Component.CENTER));
        row.setUIID("ShopStars");
        if (count <= 0) {
            return row;
        }
        for (int iter = 0; iter < 5; iter++) {
            int filled = rating - iter * 10;
            char icon = filled >= 8 ? FontImage.MATERIAL_STAR
                    : filled >= 3 ? FontImage.MATERIAL_STAR_HALF : FontImage.MATERIAL_STAR_BORDER;
            Label star = new Label("", "ShopStar");
            FontImage.setMaterialIcon(star, icon, 2.4f);
            row.add(star);
        }
        row.add(Ui.plain((rating / 10) + "." + (rating % 10) + " (" + count + ")", "SvMutedSmall"));
        return row;
    }

    /// A light tap under the finger, where the device has a motor and is not
    /// a desktop.
    /// Says a product went into the cart, with a "View cart" action, over
    /// the foot of the form; it leaves by itself after a few seconds.
    static void added(String title) {
        final Form form = CN.getCurrentForm();
        if (form == null) {
            Ui.say(Lang.tr("Added {0} to your cart", title));
            return;
        }
        final Container layer = form.getLayeredPane(ShopUi.class, true);
        layer.setLayout(new com.codename1.ui.layouts.BorderLayout());
        layer.removeAll();
        // One line: a long title ends in an ellipsis rather than growing the bar.
        Label text = new Label(Lang.tr("Added {0} to your cart", title), "CartToastText");
        text.setEndsWith3Points(true);
        final com.codename1.ui.Button view = new com.codename1.ui.Button(Lang.tr("View cart"));
        view.setUIID("CartToastAction");
        view.setName("toast-view-cart");
        final Container toast = com.codename1.ui.layouts.BorderLayout.centerEastWest(text, view, null);
        toast.setUIID("CartToast");
        view.addActionListener(e -> {
            layer.removeAll();
            form.repaint();
            ShopSections.show(com.codenameone.examples.bazaar.ui.Sections.CART);
        });
        layer.add(com.codename1.ui.layouts.BorderLayout.SOUTH, toast);
        form.revalidate();
        com.codename1.ui.util.UITimer.timer(3500, false, form, () -> {
            if (toast.getParent() == layer) {
                layer.removeAll();
                form.revalidate();
            }
        });
    }

    static void tap() {
        if (!CN.isDesktop() && !CN.isSimulator()) {
            Display.getInstance().vibrate(12);
        }
    }

    /// Finds the component named `name` under `root`, depth first.
    static Component find(Container root, String name) {
        if (root == null) {
            return null;
        }
        for (int iter = 0; iter < root.getComponentCount(); iter++) {
            Component c = root.getComponentAt(iter);
            if (name.equals(c.getName())) {
                return c;
            }
            if (c instanceof Container) {
                Component found = find((Container) c, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /// Shows the number of units in the cart on the cart tab or sidebar entry
    /// of whatever home screen is up.
    static void badge(Form home) {
        if (home == null) {
            return;
        }
        int units = ShopState.units();
        String text = units <= 0 ? null : units > 99 ? "99+" : String.valueOf(units);
        String[] names = {"tab-cart", "nav-cart"};
        for (String n : names) {
            Component c = find(home.getContentPane(), n);
            if (c instanceof Label) {
                ((Label) c).setBadgeText(text);
                ((Label) c).setBadgeUIID("ShopBadge");
                if (text != null && !text.equals(c.getClientProperty("bz.badge"))
                        && c.getClientProperty("bz.badge") != null) {
                    ShopFx.pop((Label) c);
                }
                c.putClientProperty("bz.badge", text == null ? "" : text);
                c.repaint();
            }
        }
    }

    /// Flies `image` from `from` to the cart entry of `form`, shrinking as it
    /// goes, then runs `done`. Where there is no cart entry on screen (a
    /// pushed product page on a phone), `target` is used, or nothing flies.
    static void fly(final Form form, Image image, Component from, Component target,
            final Runnable done) {
        Component to = find(form.getContentPane(), "tab-cart");
        if (to == null) {
            to = find(form.getContentPane(), "nav-cart");
        }
        if (to == null) {
            to = target;
        }
        if (to == null || image == null || from == null) {
            if (done != null) {
                done.run();
            }
            return;
        }
        final int size = Math.min(from.getWidth(), from.getHeight());
        final int sx = from.getAbsoluteX() + (from.getWidth() - size) / 2;
        final int sy = from.getAbsoluteY() + (from.getHeight() - size) / 2;
        final int tx = to.getAbsoluteX() + to.getWidth() / 2;
        final int ty = to.getAbsoluteY() + to.getHeight() / 2;
        final Image picture = image.scaled(Math.max(1, size), Math.max(1, size));
        final Motion motion = Motion.createEaseInOutMotion(0, 1000, 520);
        final Painter previous = form.getGlassPane();
        final Animation[] holder = new Animation[1];
        Painter painter = (Graphics g, Rectangle rect) -> {
            if (previous != null) {
                previous.paint(g, rect);
            }
            int t = motion.getValue();
            int x = sx + (tx - sx) * t / 1000;
            // An arc: rises a little before it drops into the cart.
            int lift = (int) (Math.sin(Math.PI * t / 1000.0) * size * 0.6);
            int y = sy + (ty - sy) * t / 1000 - lift;
            int s = Math.max(4, size - (size - CN.convertToPixels(4)) * t / 1000);
            int alpha = 255 - 120 * t / 1000;
            int old = g.getAlpha();
            g.setAlpha(alpha);
            g.drawImage(picture, x - s / 2 + size / 2 * (1000 - t) / 1000, y - s / 2 + size / 2 * (1000 - t) / 1000, s, s);
            g.setAlpha(old);
        };
        form.setGlassPane(painter);
        holder[0] = new Animation() {
            @Override
            public boolean animate() {
                if (motion.isFinished()) {
                    form.deregisterAnimated(this);
                    form.setGlassPane(previous);
                    form.repaint();
                    if (done != null) {
                        done.run();
                    }
                    return false;
                }
                // The glass pane is the form's: the whole form is drawn again.
                form.repaint();
                return true;
            }

            @Override
            public void paint(Graphics g) {
            }
        };
        motion.start();
        form.registerAnimated(holder[0]);
    }

    /// What a load into a bordered `into` ends with when it fails: the user
    /// is told, and `into` shows what went wrong and a way to ask again.
    static com.codename1.appkit.Net.Failed failed(final Container into, final Runnable retry) {
        return (status, message) -> {
            Ui.fail(status, message);
            into.removeAll();
            Container page = Ui.page();
            page.add(Ui.empty(FontImage.MATERIAL_CLOUD_OFF, message, null));
            if (retry != null) {
                page.add(FlowLayout.encloseCenter(Ui.link("Try again", "retry", e -> retry.run())));
            }
            into.add(com.codename1.ui.layouts.BorderLayout.CENTER, page);
            Ui.relayout(into);
        };
    }

    /// "{0} left at {1}".
    static String left(int count, String where) {
        return Lang.tr("{0} left at {1}", String.valueOf(count), where);
    }

    /// The coloured segment over a step of a timeline. An empty label measures
    /// nothing, so the bar the steps were drawn with never showed: this one
    /// asks for its height.
    static Component stepBar(boolean done) {
        Container bar = new Container() {
            @Override
            protected Dimension calcPreferredSize() {
                return new Dimension(CN.convertToPixels(4), CN.convertToPixels(1));
            }
        };
        bar.setUIID(done ? "SvStepBarDone" : "SvStepBar");
        return bar;
    }
}
