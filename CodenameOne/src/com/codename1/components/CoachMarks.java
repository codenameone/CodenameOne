/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.components;

import com.codename1.io.Preferences;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Font;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Label;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.plaf.RoundRectBorder;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;

import java.util.ArrayList;
import java.util.List;

/// Coach marks (also called a feature tour, product walkthrough, onboarding
/// overlay, spotlight or showcase view): the screen is dimmed, one component at
/// a time is lit through a cut-out, and a bubble next to it says what it does.
///
/// ```java
/// new CoachMarks(form)
///         .add(searchButton, "Search", "Find any order by number or name.")
///         .add(filterChip, "Filters", "Narrow the list to what is still open.")
///         .add(fab, "New order", "Start an order from here.")
///         .showOnce("orders-tour");
/// ```
///
/// A tap on the dim or on "Next" moves to the next step, "Skip" ends the tour,
/// and the last step's button reads "Done". `#showOnce(String)` shows the tour
/// the first time only and remembers that in `Preferences`, which is what an
/// onboarding tour usually wants; `#show()` shows it every time. A tour of one
/// step is a single feature highlight.
///
/// While the tour is up, touches go to it and not to the form under it. Each
/// step is announced to screen readers.
///
/// UIIDs: `CoachMarkOverlay` (its background colour and transparency are the
/// dim), `CoachMarkBubble`, `CoachMarkTitle`, `CoachMarkText`, `CoachMarkStep`
/// and `CoachMarkButton`. A theme that does not style them gets a light or dark
/// bubble to match the platform's appearance. The button labels are localized
/// through the keys `CoachMarkNext`, `CoachMarkDone` and `CoachMarkSkip`.
///
/// This replaces the deprecated [com.codename1.ui.util.GlassTutorial].
public class CoachMarks {
    private static final String PREFERENCE_PREFIX = "cn1.coachMarks.";
    private static final int DEFAULT_DIM = 0x000000;
    private static final int DEFAULT_DIM_ALPHA = 0xa6;

    private final Form form;
    private final List<Step> steps = new ArrayList<Step>();
    private Runnable onFinish;
    private boolean skippable = true;
    private int current = -1;
    private Overlay overlay;
    private ActionListener pressListener;
    private ActionListener releaseListener;

    /// A tour over a form. Add steps with `#add(Component, String, String)`.
    ///
    /// #### Parameters
    ///
    /// - `form`: the form whose components the steps point at
    public CoachMarks(Form form) {
        this.form = form;
    }

    /// Adds a step that lights a component.
    ///
    /// #### Parameters
    ///
    /// - `target`: the component to light; it must be on the form when its
    ///   step shows
    ///
    /// - `title`: a short title, or null
    ///
    /// - `text`: what the component does
    ///
    /// #### Returns
    ///
    /// this, for chaining
    public CoachMarks add(Component target, String title, String text) {
        steps.add(new Step(target, title, text));
        return this;
    }

    /// Runs code when the tour ends, whether it was finished or skipped.
    ///
    /// #### Parameters
    ///
    /// - `onFinish`: the code to run, or null
    ///
    /// #### Returns
    ///
    /// this, for chaining
    public CoachMarks setOnFinish(Runnable onFinish) {
        this.onFinish = onFinish;
        return this;
    }

    /// Whether the bubble offers "Skip". On by default.
    ///
    /// #### Parameters
    ///
    /// - `skippable`: false to hide the skip button
    ///
    /// #### Returns
    ///
    /// this, for chaining
    public CoachMarks setSkippable(boolean skippable) {
        this.skippable = skippable;
        return this;
    }

    /// The number of steps.
    ///
    /// #### Returns
    ///
    /// the step count
    public int getStepCount() {
        return steps.size();
    }

    /// The step on screen.
    ///
    /// #### Returns
    ///
    /// the zero based step, or -1 when the tour is not showing
    public int getCurrentStep() {
        return current;
    }

    /// Whether the tour is on screen.
    ///
    /// #### Returns
    ///
    /// true while it is showing
    public boolean isShowing() {
        return overlay != null;
    }

    /// Shows the tour the first time this is called with a key, and does nothing
    /// on later calls, across restarts of the app. The key is stored when the
    /// tour starts, so a tour that is skipped is not shown again either.
    ///
    /// #### Parameters
    ///
    /// - `key`: a name for this tour, unique within the app
    ///
    /// #### Returns
    ///
    /// true if the tour was shown
    public boolean showOnce(String key) {
        String pref = PREFERENCE_PREFIX + key;
        if (Preferences.get(pref, false)) {
            return false;
        }
        show();
        if (overlay == null) {
            return false;
        }
        Preferences.set(pref, true);
        return true;
    }

    /// Forgets that a tour was shown by `#showOnce(String)`, so it shows again,
    /// for a "show the tips again" setting.
    ///
    /// #### Parameters
    ///
    /// - `key`: the key the tour was shown with
    public static void resetShowOnce(String key) {
        Preferences.delete(PREFERENCE_PREFIX + key);
    }

    /// Shows the tour from its first step. Does nothing if it has no steps or is
    /// already showing.
    public void show() {
        if (steps.isEmpty() || overlay != null) {
            return;
        }
        overlay = new Overlay();
        pressListener = new ConsumeListener();
        releaseListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                evt.consume();
                released(evt.getX(), evt.getY());
            }
        };
        form.addPointerPressedListener(pressListener);
        form.addPointerDraggedListener(pressListener);
        form.addPointerReleasedListener(releaseListener);
        // The layer starts with a flow layout that is never laid out again,
        // which left the overlay 0x0 when the tour began before the form was
        // shown. The layer is this class's own, so make it fill-the-parent.
        Container layer = form.getFormLayeredPane(CoachMarks.class, true);
        layer.setLayout(new LayeredLayout());
        layer.add(overlay);
        current = -1;
        next();
    }

    /// Moves to the next step, or ends the tour after the last one.
    public void next() {
        if (overlay == null) {
            return;
        }
        current++;
        if (current >= steps.size()) {
            dismiss();
            return;
        }
        Step step = steps.get(current);
        Container scrollable = step.target.getParent();
        while (scrollable != null && !scrollable.isScrollableY() && !scrollable.isScrollableX()) {
            scrollable = scrollable.getParent();
        }
        if (scrollable != null) {
            scrollable.scrollComponentToVisible(step.target);
        }
        overlay.showStep(step);
        String spoken = step.title == null ? step.text : step.title + ". " + step.text;
        CN.announceForAccessibility(overlay, spoken);
    }

    /// Ends the tour and runs the code given to `#setOnFinish(Runnable)`.
    public void dismiss() {
        if (overlay == null) {
            return;
        }
        form.removePointerPressedListener(pressListener);
        form.removePointerDraggedListener(pressListener);
        form.removePointerReleasedListener(releaseListener);
        Overlay o = overlay;
        overlay = null;
        current = -1;
        Container parent = o.getParent();
        if (parent != null) {
            parent.removeComponent(o);
            parent.repaint();
        }
        if (onFinish != null) {
            onFinish.run();
        }
    }

    /// A release while the tour is up: the bubble's buttons act, a release
    /// elsewhere in the bubble does nothing, and one on the dim moves on.
    private void released(int x, int y) {
        if (overlay == null) {
            return;
        }
        if (overlay.skip.getParent() != null && overlay.skip.contains(x, y)) {
            dismiss();
        } else if (overlay.next.contains(x, y) || !overlay.bubble.contains(x, y)) {
            next();
        }
    }

    /// Swallows the presses and drags that land while the tour is up.
    private static final class ConsumeListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent evt) {
            evt.consume();
        }
    }

    private static final class Step {
        final Component target;
        final String title;
        final String text;

        Step(Component target, String title, String text) {
            this.target = target;
            this.title = title;
            this.text = text;
        }
    }

    /// Styles a component in code when the theme does not define its UIID.
    private static void fallback(Component c, String uiid, int fg, int bg, boolean opaque) {
        c.setUIID(uiid);
        if (UIManager.getInstance().isThemeStyleDefined(uiid)) {
            return;
        }
        Style s = c.getAllStyles();
        s.setFgColor(fg);
        s.setBgTransparency(opaque ? 255 : 0);
        s.setBgColor(bg);
        s.setMargin(0, 0, 0, 0);
    }

    /// The dim, the cut-out and the bubble, laid over the whole form.
    private final class Overlay extends Container {
        final Container bubble = new Container(BoxLayout.y());
        final SpanLabel title = new SpanLabel();
        final SpanLabel text = new SpanLabel();
        final Label counter = new Label();
        final Button skip = new Button();
        final Button next = new Button();
        Step step;
        int[] laidOutHole;

        @Override
        protected void initComponent() {
            super.initComponent();
            // Fill the layer even when it was sized before this was added.
            Container parent = getParent();
            if (parent != null && (getWidth() != parent.getWidth() || getHeight() != parent.getHeight())) {
                setX(0);
                setY(0);
                setWidth(parent.getWidth());
                setHeight(parent.getHeight());
                setShouldLayout(true);
                revalidateLater();
            }
        }

        @Override
        protected void initUnselectedStyle(Style unselectedStyle) {
            super.initUnselectedStyle(unselectedStyle);
            // Unthemed, the UIID's default style is opaque white; paint()
            // draws the dim itself.
            if (unselectedStyle != null && !getUIManager().isThemeStyleDefined(getUIID())) {
                unselectedStyle.setBgTransparency(0);
            }
        }

        Overlay() {
            setLayout(new BubbleLayout());
            setUIID("CoachMarkOverlay");
            boolean dark = Boolean.TRUE.equals(CN.isDarkMode());
            int fg = dark ? 0xf2f2f2 : 0x1c1c1e;
            int bg = dark ? 0x2c2c2e : 0xffffff;
            int accent = dark ? 0x64a8ff : 0x0a66d6;
            fallback(bubble, "CoachMarkBubble", fg, bg, true);
            if (!UIManager.getInstance().isThemeStyleDefined("CoachMarkBubble")) {
                Style s = bubble.getAllStyles();
                s.setBorder(RoundRectBorder.create().cornerRadius(3f).shadowOpacity(60));
                // Millimetres, so the padding clears the border's shadow
                // inset at every density; dips were too thin on high density
                // screens and the text spilled past the drawn bubble.
                s.setPaddingUnit(Style.UNIT_TYPE_MM);
                s.setPadding(3f, 3f, 3.5f, 3.5f);
            }
            title.setTextUIID("CoachMarkTitle");
            text.setTextUIID("CoachMarkText");
            title.setUIID("Container");
            text.setUIID("Container");
            fallback(title.getTextComponent(), "CoachMarkTitle", fg, bg, false);
            fallback(text.getTextComponent(), "CoachMarkText", fg, bg, false);
            if (!UIManager.getInstance().isThemeStyleDefined("CoachMarkTitle")) {
                Style s = title.getTextComponent().getAllStyles();
                Font f = s.getFont();
                s.setFont(f.isTTFNativeFont() ? f.derive(f.getPixelSize() * 1.15f, Font.STYLE_BOLD)
                        : Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM));
            }
            fallback(counter, "CoachMarkStep", fg, bg, false);
            fallback(skip, "CoachMarkButton", fg, bg, false);
            fallback(next, "CoachMarkButton", accent, bg, false);
            UIManager ui = UIManager.getInstance();
            skip.setText(ui.localize("CoachMarkSkip", "Skip"));
            Container buttons = FlowLayout.encloseRight(skip, next);
            Container row = BorderLayout.centerEastWest(null, buttons, counter);
            row.setUIID("Container");
            bubble.add(title).add(text).add(row);
            add(bubble);
            // Taps reach these through released(); the press is consumed so a
            // tap never fires them twice. A key, D-pad or screen reader
            // activation fires the button itself and lands here.
            next.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    CoachMarks.this.next();
                }
            });
            skip.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    dismiss();
                }
            });
        }

        @Override
        protected void deinitialize() {
            super.deinitialize();
            // The form went away (back, another form shown): end the tour
            // rather than leave it, and its pointer listeners, on the form.
            // A Dialog is a Form too, so showing one deinitializes this form
            // while the tour should survive it. Decide once the change has
            // settled: still off screen and no Dialog on top means the user
            // really left.
            CN.callSerially(new Runnable() {
                @Override
                public void run() {
                    if (overlay == Overlay.this && !isInitialized() //NOPMD CompareObjectsWithEquals - identity: is this overlay the one showing
                            && !(CN.getCurrentForm() instanceof Dialog)) {
                        dismiss();
                    }
                }
            });
        }

        void showStep(Step step) {
            this.step = step;
            title.setText(step.title == null ? "" : step.title);
            title.setHidden(step.title == null);
            text.setText(step.text == null ? "" : step.text);
            int count = steps.size();
            counter.setText(count > 1 ? (current + 1) + " / " + count : "");
            UIManager ui = UIManager.getInstance();
            next.setText(current == count - 1 ? ui.localize("CoachMarkDone", "Done")
                    : ui.localize("CoachMarkNext", "Next"));
            if (skippable && current < count - 1) {
                skip.setHidden(false);
            } else {
                skip.setHidden(true);
            }
            setShouldCalcPreferredSize(true);
            revalidateLater();
            repaint();
        }

        /// The lit area: the target's bounds, a little larger, relative to
        /// this overlay. Null when the target is not on screen.
        int[] hole() {
            if (step == null || step.target.getComponentForm() == null) {
                return null;
            }
            Component t = step.target;
            if (t.getWidth() <= 0 || t.getHeight() <= 0) {
                return null;
            }
            int pad = CN.convertToPixels(1.5f);
            int x = t.getAbsoluteX() - getAbsoluteX() + getX() - pad;
            int y = t.getAbsoluteY() - getAbsoluteY() + getY() - pad;
            return new int[]{x, y, t.getWidth() + pad * 2, t.getHeight() + pad * 2};
        }

        @Override
        public void paint(Graphics g) {
            int alpha = g.getAlpha();
            int color = g.getColor();
            Style s = getStyle();
            boolean themed = UIManager.getInstance().isThemeStyleDefined("CoachMarkOverlay");
            int dim = themed ? s.getBgColor() : DEFAULT_DIM;
            int dimAlpha = themed ? (s.getBgTransparency() & 0xff) : DEFAULT_DIM_ALPHA;
            g.setColor(dim);
            g.setAlpha(dimAlpha * alpha / 255);
            int x = getX();
            int y = getY();
            int w = getWidth();
            int h = getHeight();
            int[] hole = hole();
            if (!sameHole(hole, laidOutHole)) {
                // The target moved since the bubble was placed (a smooth
                // scroll into view, a rotation); place it again.
                laidOutHole = hole;
                setShouldLayout(true);
                revalidateLater();
            }
            if (hole == null) {
                g.fillRect(x, y, w, h);
            } else {
                // Four rectangles around the hole; the lit target shows through.
                g.fillRect(x, y, w, Math.max(0, hole[1] - y));
                g.fillRect(x, hole[1] + hole[3], w, Math.max(0, y + h - hole[1] - hole[3]));
                g.fillRect(x, hole[1], Math.max(0, hole[0] - x), hole[3]);
                g.fillRect(hole[0] + hole[2], hole[1], Math.max(0, x + w - hole[0] - hole[2]), hole[3]);
                boolean aa = g.isAntiAliased();
                g.setAntiAliased(true);
                g.setColor(0xffffff);
                g.setAlpha(0xcc * alpha / 255);
                int arc = CN.convertToPixels(2f);
                g.drawRoundRect(hole[0], hole[1], hole[2] - 1, hole[3] - 1, arc, arc);
                g.setAntiAliased(aa);
            }
            g.setAlpha(alpha);
            g.setColor(color);
            super.paint(g);
        }
    }

    private static boolean sameHole(int[] a, int[] b) {
        if (a == null || b == null) {
            return a == null && b == null;
        }
        return a[0] == b[0] && a[1] == b[1] && a[2] == b[2] && a[3] == b[3];
    }

    /// Places the bubble under the lit target when there is room, otherwise
    /// over it, centred on it and kept inside the overlay.
    private static final class BubbleLayout extends Layout {
        @Override
        public void layoutContainer(Container parent) {
            if (!(parent instanceof Overlay) || parent.getComponentCount() == 0) {
                return;
            }
            Overlay o = (Overlay) parent;
            Container bubble = o.bubble;
            int margin = CN.convertToPixels(4f);
            int gap = CN.convertToPixels(2.5f);
            int w = Math.min(parent.getWidth() - margin * 2, CN.convertToPixels(80f));
            bubble.setWidth(w);
            bubble.setShouldCalcPreferredSize(true);
            int h = bubble.getPreferredH();
            int[] hole = o.hole();
            o.laidOutHole = hole;
            int x;
            int y;
            if (hole == null) {
                x = (parent.getWidth() - w) / 2;
                y = (parent.getHeight() - h) / 2;
            } else {
                int top = hole[1] - parent.getY();
                int bottom = top + hole[3];
                x = hole[0] - parent.getX() + hole[2] / 2 - w / 2;
                if (parent.getHeight() - bottom - gap >= h + margin || top < parent.getHeight() / 2) {
                    y = bottom + gap;
                } else {
                    y = top - gap - h;
                }
            }
            x = Math.max(margin, Math.min(x, parent.getWidth() - margin - w));
            y = Math.max(margin, Math.min(y, parent.getHeight() - margin - h));
            bubble.setX(x);
            bubble.setY(y);
            bubble.setWidth(w);
            bubble.setHeight(h);
            bubble.layoutContainer();
        }

        @Override
        public Dimension getPreferredSize(Container parent) {
            return new Dimension(parent.getWidth(), parent.getHeight());
        }
    }
}
