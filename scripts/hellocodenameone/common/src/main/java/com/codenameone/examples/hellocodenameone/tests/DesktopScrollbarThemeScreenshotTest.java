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
package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Label;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.Layout;

/**
 * <p>The desktop scrollbar in the three states that distinguish it from the mobile one:
 * idle, pointer over the thumb, and thumb being dragged.</p>
 *
 * <p>This is the change's headline behaviour and it had no capture anywhere. Before this
 * PR the three desktop themes derived their scrollbar from the mobile theme -- a thumb
 * that fades in, fades out, and cannot be grabbed -- while the mobile themes carried the
 * full desktop treatment. The rules are now the right way round, and a scrollbar with a
 * reserved gutter and a hover highlight is what proves it.</p>
 *
 * <p>The states are painted rather than performed. {@code isVScrollThumbHover} and
 * {@code isVScrollThumbGrabbed} are what the look and feel consults, so overriding them is
 * not a shortcut around the real code path -- it IS the real code path, entered at the
 * only point a screenshot harness can reach without a live pointer. The same probe scores
 * the {@code DesktopScrollBar} rows in the fidelity suite against the real WinUI, AppKit
 * and GTK scrollbars, so the two suites are asking one question in two ways: fidelity
 * asks whether it looks like the platform's, this asks whether it still looks like itself
 * after a change.</p>
 *
 * <p>The thumb proportions are fixed at the top four tenths of the track, matching the
 * fidelity probe, because a thumb whose size follows some scrollable content would move
 * whenever that content changed and every golden would churn for a reason unrelated to
 * the scrollbar.</p>
 */
public class DesktopScrollbarThemeScreenshotTest extends DualAppearanceBaseTest {

    @Override
    protected String baseName() {
        return "DesktopScrollbarTheme";
    }

    @Override
    protected Layout newLayout() {
        return BoxLayout.y();
    }

    @Override
    protected void populate(Form form, String suffix) {
        form.add(new Label("Idle"));
        form.add(track(false, false));
        form.add(new Label("Pointer over the thumb"));
        form.add(track(true, false));
        form.add(new Label("Thumb dragged"));
        com.codename1.ui.Component grabbed = track(false, true);
        form.add(grabbed);
        annotateComponent(grabbed, "DesktopScrollThumb.pressed inside the DesktopScroll gutter");
    }

    private static com.codename1.ui.Component track(boolean hover, boolean grabbed) {
        return new ScrollBarProbe(hover, grabbed);
    }

    /**
     * Paints the vertical scrollbar the look and feel would paint for a component in the
     * given thumb state, at its themed gutter width.
     */
    private static final class ScrollBarProbe extends Container {
        private final boolean hover;
        private final boolean grabbed;

        ScrollBarProbe(boolean hover, boolean grabbed) {
            this.hover = hover;
            this.grabbed = grabbed;
            setUIID("Container");
            getAllStyles().setMargin(0, 0, 0, 0);
            getAllStyles().setPadding(0, 0, 0, 0);
            getAllStyles().setBgTransparency(0);
        }

        @Override
        public boolean isVScrollThumbHover() {
            return hover;
        }

        @Override
        public boolean isVScrollThumbGrabbed() {
            return grabbed;
        }

        @Override
        protected Dimension calcPreferredSize() {
            // The gutter width is itself part of what is being captured -- it is
            // DesktopScroll's padding plus margin -- so it has to be asked for rather than
            // written here. The height is a plain strip tall enough to show a thumb that
            // covers part of a track.
            return new Dimension(
                    getUIManager().getLookAndFeel().getVerticalScrollWidth(),
                    com.codename1.ui.Display.getInstance().convertToPixels(18, false));
        }

        @Override
        public void paint(Graphics g) {
            getUIManager().getLookAndFeel().drawVerticalScroll(g, this, 0f, 0.4f);
        }
    }
}
