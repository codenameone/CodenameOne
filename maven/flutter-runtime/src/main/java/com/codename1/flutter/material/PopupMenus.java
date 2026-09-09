/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.material;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.BuildOwner;
import com.codename1.flutter.Element;
import com.codename1.flutter.FlutterUI;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.FlutterRootLayout;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.widgets.Column;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.layouts.BorderLayout;

import dart.core.DartList;
import dart.runtime.Funcs;

import java.util.ArrayList;
import java.util.List;

/**
 * Presents a {@link PopupMenuButton}'s menu and reports the selection — the part of
 * {@code showMenu} the button needs.
 *
 * <p>The entries come from the button's {@code itemBuilder}, which is a Dart callback
 * returning a list of {@link PopupMenuEntry}. Each selectable entry is wrapped in an
 * {@link InkWell} that dismisses the menu and fires {@code onSelected} with that entry's
 * value, so a menu behaves the way the Dart says it does rather than merely appearing.</p>
 *
 * <p>The menu itself is a CN1 dialog holding a Flutter subtree, the same arrangement
 * {@link Dialogs} uses for {@code showDialog} — modeless, so transpiled code continues
 * after the call exactly as it does in Dart.</p>
 */
public final class PopupMenus {

    private static com.codename1.ui.Dialog openMenu;
    private static Element openRoot;
    /** The page behind the menu, and the tint it had before we cleared it. */
    private static com.codename1.ui.Form tinted;
    private static int savedTint;

    private PopupMenus() {
    }

    /**
     * Builds and shows {@code button}'s menu. Does nothing when the button is disabled or
     * has no itemBuilder.
     */
    public static void show(BuildContext context, PopupMenuButton<?> button) {
        show(context, button, null);
    }

    /**
     * Shows the menu anchored to {@code anchor} — the button's own component.
     *
     * <p>Anchoring matters beyond neatness: a plain dialog is centred and dims the screen
     * behind it, which is a modal gesture. Flutter's menu is a small surface beside the
     * control that opened it, and CN1's popup dialog is that same shape.</p>
     */
    public static void show(BuildContext context, PopupMenuButton<?> button,
            com.codename1.ui.Component anchor) {
        if (button == null || !button.isEnabled() || button.getItemBuilder() == null) {
            return;
        }
        List<PopupMenuEntry<?>> entries = entriesOf(context, button);
        if (entries.isEmpty()) {
            return;
        }
        MenuWidget menu = new MenuWidget(button, entries);
        if (!Display.isInitialized()) {
            // headless: mount the tree so the bookkeeping is testable, with no dialog
            openRoot = FlutterUI.mount(menu, new RenderHost(), new BuildOwner());
            return;
        }
        dismiss();
        com.codename1.ui.Dialog d = new com.codename1.ui.Dialog(new BorderLayout());
        d.setDisposeWhenPointerOutOfBounds(true);
        Container c = FlutterUI.wrap(menu);
        d.add(BorderLayout.CENTER, c);
        openMenu = d;
        openRoot = ((FlutterRootLayout) c.getLayout()).host().rootElement();
        if (anchor != null) {
            // Beside the button — Flutter's menu, and CN1's popup dialog. The popup's own
            // chrome is dropped: CN1 draws a speech-bubble arrow and dims the screen behind
            // it, and a Material menu does neither. The Material surface underneath supplies
            // the rounded card.
            stripPopupChrome(d);
            d.showPopupDialog(anchor);
        } else {
            // Modeless, like showDialog: the caller keeps running, as the Dart expects.
            d.showPacked(BorderLayout.NORTH, false);
        }
    }

    /**
     * Removes the arrow border and the dimming a CN1 popup dialog brings with it, so what
     * shows is the Material surface and nothing else.
     */
    private static void stripPopupChrome(com.codename1.ui.Dialog d) {
        try {
            // The UIID is deliberately left alone: showPopupDialog derives the popup's
            // surface from it, and renaming it away takes the card with the arrow.
            d.setBlurBackgroundRadius(-1);
            d.getContentPane().getAllStyles().setPadding(0, 0, 0, 0);
            d.getContentPane().getAllStyles().setMargin(0, 0, 0, 0);
            // No arrow: CN1 draws a speech bubble pointing at the anchor, and a Material
            // menu is a plain rounded card. The arrow rides on the border, so replacing the
            // border with a rounded one removes the point and keeps the surface.
            d.getDialogStyle().setBorder(com.codename1.ui.plaf.RoundRectBorder.create()
                    .cornerRadius(1f)
                    .shadowOpacity(30));
            // And no scrim: a dialog TINTS the page behind it, which reads as modal, while
            // a menu is a light touch. The tint lives on the page rather than on us, so it
            // is cleared there and put back when the menu closes.
            tinted = com.codename1.ui.Display.getInstance().getCurrent();
            if (tinted != null) {
                savedTint = tinted.getTintColor();
                tinted.setTintColor(0);
            }
        } catch (Throwable t) {
            // chrome is cosmetic; a themed popup still works
        }
    }

    /** Puts back the tint the page had before the menu covered it. */
    private static void restoreTint() {
        if (tinted != null) {
            try {
                tinted.setTintColor(savedTint);
            } catch (Throwable t) {
                // best effort: a wrong tint is better than a failed dismiss
            }
            tinted = null;
        }
    }

    /** Closes the open menu, if any. */
    public static void dismiss() {
        restoreTint();
        if (openRoot != null) {
            FlutterUI.unmountTree(openRoot);
            openRoot = null;
        }
        if (openMenu != null) {
            openMenu.dispose();
            openMenu = null;
        }
    }

    /** Whether a menu is currently open — test and hot-restart hook. */
    public static boolean isOpen() {
        return openRoot != null;
    }

    /** Runs the itemBuilder and collects whatever entries it produced. */
    @SuppressWarnings("unchecked")
    private static List<PopupMenuEntry<?>> entriesOf(BuildContext context,
            PopupMenuButton<?> button) {
        List<PopupMenuEntry<?>> out = new ArrayList<PopupMenuEntry<?>>();
        Object built;
        try {
            built = button.getItemBuilder().call(context);
        } catch (Throwable t) {
            com.codename1.flutter.FlutterErrorReport.unimplemented("PopupMenuButton",
                    "itemBuilder failed: " + t);
            return out;
        }
        if (built instanceof Iterable) {
            for (Object o : (Iterable<Object>) built) {
                if (o instanceof PopupMenuEntry) {
                    out.add((PopupMenuEntry<?>) o);
                }
            }
        }
        return out;
    }

    /**
     * The menu surface: a Material card holding one row per entry.
     */
    static final class MenuWidget extends StatelessWidget {

        private final PopupMenuButton<?> button;
        private final List<PopupMenuEntry<?>> entries;

        MenuWidget(PopupMenuButton<?> button, List<PopupMenuEntry<?>> entries) {
            this.button = button;
            this.entries = entries;
        }

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public Widget build(BuildContext context) {
            DartList<Widget> rows = new DartList<Widget>();
            for (int i = 0; i < entries.size(); i++) {
                final PopupMenuEntry<?> entry = entries.get(i);
                if (entry instanceof PopupMenuDivider) {
                    rows.add(new PopupMenuDivider());
                    continue;
                }
                if (!(entry instanceof PopupMenuItem)) {
                    continue;
                }
                final PopupMenuItem item = (PopupMenuItem) entry;
                // Material menu item metrics: 48lp tall, 16lp either side, start-aligned.
                com.codename1.flutter.widgets.Container box =
                        new com.codename1.flutter.widgets.Container();
                box.padding(com.codename1.flutter.EdgeInsets.symmetric(0, 16));
                box.height(48);
                box.alignment(com.codename1.flutter.AlignmentDirectional.centerStart);
                box.child(item.getChild());
                InkWell row = new InkWell();
                row.child(box);
                if (item.isEnabled()) {
                    row.onTap(new Funcs.VoidFunc0() {
                        @Override
                        public void call() {
                            select(item);
                        }
                    });
                }
                rows.add(row);
            }
            Column col = new Column();
            col.children(rows);
            col.mainAxisSize(com.codename1.flutter.MainAxisSize.min);
            Material surface = new Material();
            surface.child(col);
            return surface;
        }

        /** Dismiss first, then report — the order Flutter's menu route uses. */
        @SuppressWarnings({"unchecked", "rawtypes"})
        private void select(PopupMenuItem item) {
            dismiss();
            Funcs.VoidFunc0 tap = item.getOnTap();
            if (tap != null) {
                tap.call();
            }
            Funcs.VoidFunc1 onSelected = ((PopupMenuButton) button).getOnSelected();
            if (onSelected != null && item.getValue() != null) {
                onSelected.call(item.getValue());
            }
        }
    }
}
