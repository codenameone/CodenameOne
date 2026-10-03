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
package com.codename1.androidcompat.runtime;

import android.app.Activity;
import android.app.ActivityThread;
import android.util.TypedValue;
import android.view.MenuItem;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.Toolbar;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.plaf.Style;

import java.util.ArrayList;
import java.util.List;

/// The form showing one activity: the decor view fills it, the action bar is
/// its toolbar and the options menu becomes toolbar commands.
public final class ActivityForm extends Form {

    private final Activity activity;
    private final List<Command> menuCommands = new ArrayList<Command>();
    private Command upCommand;
    private Command overflowCommand;
    private final List<MenuImpl.Item> overflowItems = new ArrayList<MenuImpl.Item>();

    public ActivityForm(Activity activity, Component decorPeer) {
        super(new BorderLayout());
        this.activity = activity;
        setScrollable(false);
        setScrollableY(false);
        Style s = getContentPane().getAllStyles();
        s.setPadding(0, 0, 0, 0);
        s.setMargin(0, 0, 0, 0);
        s.setBgTransparency(0);
        getAllStyles().setPadding(0, 0, 0, 0);
        // An Android window's content sits inside the system bars unless the
        // application asks for edge-to-edge drawing; the safe area is the
        // Codename One equivalent of those insets.
        getContentPane().setSafeArea(true);
        add(BorderLayout.CENTER, decorPeer);
    }

    private int statusBarColor;

    /// The color painted behind the status bar when the activity has no
    /// action bar of Codename One's (with one, the toolbar covers that area).
    /// Zero alpha paints nothing.
    public void setStatusBarColor(int argb) {
        if (statusBarColor != argb) {
            statusBarColor = argb;
            repaint();
        }
    }

    @Override
    public void paintBackground(com.codename1.ui.Graphics g) {
        super.paintBackground(g);
        int alpha = (statusBarColor >>> 24) & 0xff;
        if (alpha == 0) {
            return;
        }
        com.codename1.ui.geom.Rectangle safe = getSafeArea();
        int top = safe == null ? 0 : safe.getY() - getAbsoluteY();
        if (top <= 0) {
            return;
        }
        int oldColor = g.getColor();
        int oldAlpha = g.getAlpha();
        g.setColor(statusBarColor & 0xffffff);
        g.setAlpha(alpha);
        g.fillRect(getX(), getY(), getWidth(), top);
        g.setAlpha(oldAlpha);
        g.setColor(oldColor);
    }

    public Activity getActivity() {
        return activity;
    }

    @Override
    protected void sizeChanged(int w, int h) {
        super.sizeChanged(w, h);
        if (ResourceManager.get().refresh() && ActivityThread.getTopActivity() == activity) {
            ActivityThread.onConfigurationChanged(ResourceManager.get().configuration());
        }
    }

    /// Shows or hides the up arrow for `setDisplayHomeAsUpEnabled`.
    public void setUpButton(boolean show, int color) {
        Toolbar tb = getToolbar();
        if (tb == null) {
            return;
        }
        if (upCommand != null) {
            tb.removeCommand(upCommand);
            upCommand = null;
        }
        if (show) {
            upCommand = new Command("") {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    ActivityThread.onHomePressed(activity);
                }
            };
            Style st = new Style();
            st.setFgColor(color & 0xffffff);
            upCommand.setIcon(FontImage.createMaterial(FontImage.MATERIAL_ARROW_BACK, st, 3.5f));
            tb.addCommandToLeftBar(upCommand);
        }
    }

    /// Mirrors the options menu onto the toolbar: action items with an icon
    /// (or `always`) on the right bar, the rest in the overflow menu.
    public void showMenu(MenuImpl menu, boolean show) {
        Toolbar tb = getToolbar();
        if (tb == null) {
            return;
        }
        for (Command c : menuCommands) {
            tb.removeCommand(c);
        }
        menuCommands.clear();
        overflowItems.clear();
        overflowCommand = null;
        if (show) {
            int color = titleColor();
            int iconSize = Display.getInstance().convertToPixels(6f);
            int actions = 0;
            for (final MenuImpl.Item item : menu.visibleItems()) {
                CharSequence title = item.getTitle();
                Command c = new ItemCommand(title == null ? "" : title.toString(), item);
                c.setEnabled(item.isEnabled());
                int show2 = item.getShowAsAction();
                boolean asAction = (show2 & MenuItem.SHOW_AS_ACTION_ALWAYS) != 0
                        || ((show2 & MenuItem.SHOW_AS_ACTION_IF_ROOM) != 0 && actions < 3);
                if (asAction) {
                    if (item.getIcon() != null) {
                        // Drawn as it is, with the drawable's own tint: the
                        // action bar does not recolor menu icons (an icon tint
                        // list set on the item is already in the drawable).
                        Image img = DrawableImages.toImage(item.getIcon(), iconSize, 0);
                        c.setIcon(img);
                        if ((show2 & MenuItem.SHOW_AS_ACTION_WITH_TEXT) == 0) {
                            c.setCommandName("");
                        }
                    }
                    tb.addCommandToRightBar(c);
                    actions++;
                    menuCommands.add(c);
                } else {
                    overflowItems.add(item);
                }
            }
            if (!overflowItems.isEmpty()) {
                // Android's own overflow button and popup, rather than the
                // toolbar's: the menu then looks and is positioned like the
                // action bar's.
                overflowCommand = new Command("") {
                    @Override
                    public void actionPerformed(ActionEvent evt) {
                        openOverflow();
                    }
                };
                Style st = new Style();
                st.setFgColor(color & 0xffffff);
                st.setBgTransparency(0);
                overflowCommand.setIcon(FontImage.createMaterial(FontImage.MATERIAL_MORE_VERT, st, 3.5f));
                tb.addCommandToRightBar(overflowCommand);
                menuCommands.add(overflowCommand);
            }
        }
        styleToolbar(titleColor());
        revalidate();
    }

    /// Gives the toolbar Android's action bar look: a left-aligned 20sp
    /// medium title and flat icon buttons in the title color, over the
    /// theme's primary color. Re-applied after every change, because the
    /// toolbar rebuilds its components.
    public void styleToolbar(int titleColor) {
        Toolbar tb = getToolbar();
        if (tb == null) {
            return;
        }
        tb.setTitleCentered(false);
        float sp = ResourceManager.get().metrics().scaledDensity;
        float dp = ResourceManager.get().metrics().density;
        com.codename1.ui.Font titleFont = FontCache.font(android.graphics.Typeface.create("sans-serif-medium",
                android.graphics.Typeface.NORMAL), 20 * sp);
        styleTree(tb, titleColor & 0xffffff, titleFont, Math.round(12 * dp), Math.round(48 * dp));
        Style ts = tb.getAllStyles();
        ts.setPaddingUnit(Style.UNIT_TYPE_PIXELS);
        ts.setPadding(0, 0, Math.round(4 * dp), Math.round(4 * dp));
        ts.setBorder(com.codename1.ui.plaf.Border.createEmpty());
        tb.revalidate();
    }

    private static void styleTree(com.codename1.ui.Container c, int fg, com.codename1.ui.Font titleFont, int pad,
                                  int barHeight) {
        for (int i = 0; i < c.getComponentCount(); i++) {
            Component cmp = c.getComponentAt(i);
            if (cmp instanceof com.codename1.ui.Button) {
                Style s = cmp.getAllStyles();
                s.setBgTransparency(0);
                s.setBgImage(null);
                s.setBorder(com.codename1.ui.plaf.Border.createEmpty());
                s.setFgColor(fg);
                s.setPaddingUnit(Style.UNIT_TYPE_PIXELS);
                s.setPadding(pad, pad, pad, pad);
                s.setMargin(0, 0, 0, 0);
            } else if (cmp instanceof com.codename1.ui.Label) {
                Style s = cmp.getAllStyles();
                s.setFgColor(fg);
                s.setFont(titleFont);
                s.setAlignment(Component.LEFT);
                s.setBgTransparency(0);
                s.setBorder(com.codename1.ui.plaf.Border.createEmpty());
                s.setPaddingUnit(Style.UNIT_TYPE_PIXELS);
                // As tall as an icon button, so a bar with no actions is
                // still an Android action bar's height.
                int v = Math.max(0, (barHeight - titleFont.getHeight()) / 2);
                s.setPadding(v, v, pad + pad / 3, 0);
            } else if (cmp instanceof com.codename1.ui.Container) {
                cmp.getAllStyles().setBgTransparency(0);
                cmp.getAllStyles().setBorder(com.codename1.ui.plaf.Border.createEmpty());
                styleTree((com.codename1.ui.Container) cmp, fg, titleFont, pad, barHeight);
            }
        }
    }

    /// Opens the overflow menu under its button, as `openOptionsMenu` and
    /// the button itself do.
    public void openOverflow() {
        Toolbar tb = getToolbar();
        if (tb == null || overflowCommand == null || overflowItems.isEmpty()) {
            return;
        }
        Component button = tb.findCommandComponent(overflowCommand);
        if (button == null) {
            return;
        }
        MenuPopup.show(activity, new ArrayList<MenuImpl.Item>(overflowItems), new ComponentAnchor(activity, button),
                android.view.Gravity.END, android.R.attr.actionOverflowMenuStyle, 0, null);
    }

    private int titleColor() {
        TypedValue tv = new TypedValue();
        if (activity.getTheme().resolveAttribute(android.R.attr.textColorPrimaryInverse, tv, true)
                && tv.type >= TypedValue.TYPE_FIRST_COLOR_INT && tv.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            return tv.data;
        }
        return 0xffffffff;
    }

    /// A toolbar command that runs an options-menu item.
    private static final class ItemCommand extends Command {
        private final MenuImpl.Item item;

        ItemCommand(String name, MenuImpl.Item item) {
            super(name);
            this.item = item;
        }

        /// Command compares names; two menu items may share a title, and the
        /// toolbar must remove exactly the one asked for.
        @Override
        public boolean equals(Object o) {
            return o == this;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(this);
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            item.invoke();
        }
    }
}
