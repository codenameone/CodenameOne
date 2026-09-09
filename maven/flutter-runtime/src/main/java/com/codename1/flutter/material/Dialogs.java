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
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.layouts.BorderLayout;

import dart.runtime.Funcs;

import java.util.ArrayList;
import java.util.List;

/**
 * Host class for Dart's top-level {@code showDialog} function. The built
 * widget tree (typically an {@link AlertDialog}) mounts as a Flutter subtree
 * inside a CN1 {@link com.codename1.ui.Dialog} shown MODELESSLY
 * ({@code showPacked(..., false)}) — transpiled code continues to run after
 * the {@code showDialog} call, exactly like Dart's non-awaited Future.
 *
 * <p>A static dialog stack records open dialogs;
 * {@code Navigator.pop(context)} consults it first, so a TextButton action
 * that pops dismisses the dialog (Flutter's dialogs-are-routes behavior).
 * Tap-outside dismissal is deliberately disabled in M3 to keep the stack
 * authoritative.</p>
 *
 * <p>Headless (no Display): the widget tree still mounts (builder runs, the
 * stack is maintained) with no CN1 dialog — unit-testable bookkeeping.</p>
 */
public final class Dialogs {

    private static final List<DialogEntry> dialogStack = new ArrayList<DialogEntry>();

    private Dialogs() {
    }

    public static void showDialog(BuildContext context, Funcs.Func1<BuildContext, Widget> builder) {
        DialogWidget rootWidget = new DialogWidget(builder);
        DialogEntry e = new DialogEntry();
        if (Display.isInitialized()) {
            com.codename1.ui.Dialog d = new com.codename1.ui.Dialog(new BorderLayout());
            d.setDisposeWhenPointerOutOfBounds(false);
            Container c = FlutterUI.wrap(rootWidget);
            d.add(BorderLayout.CENTER, c);
            e.dialog = d;
            e.root = ((FlutterRootLayout) c.getLayout()).host().rootElement();
            dialogStack.add(e);
            // modeless: returns immediately, the calling code keeps running
            d.showPacked(BorderLayout.CENTER, false);
        } else {
            RenderHost host = new RenderHost();
            e.root = FlutterUI.mount(rootWidget, host, new BuildOwner());
            dialogStack.add(e);
        }
    }

    /**
     * Dismisses the topmost open dialog. Returns false when none is open —
     * the caller (Navigator.pop) then pops a route instead.
     */
    public static boolean popTopDialog() {
        if (dialogStack.isEmpty()) {
            return false;
        }
        DialogEntry e = dialogStack.remove(dialogStack.size() - 1);
        if (e.root != null) {
            FlutterUI.unmountTree(e.root);
        }
        if (e.dialog != null) {
            e.dialog.dispose();
        }
        return true;
    }

    /**
     * The number of dialogs currently open.
     */
    public static int openDialogCount() {
        return dialogStack.size();
    }

    /**
     * Test / hot-restart hook: forgets all open dialogs without disposing.
     */
    public static void reset() {
        dialogStack.clear();
    }

    private static final class DialogEntry {
        com.codename1.ui.Dialog dialog;
        Element root;
    }

    /**
     * Adapter mounting the dialog's WidgetBuilder as a subtree root; the
     * builder runs during the first build with an in-tree BuildContext.
     */
    static final class DialogWidget extends StatelessWidget {

        private final Funcs.Func1<BuildContext, Widget> builder;

        DialogWidget(Funcs.Func1<BuildContext, Widget> builder) {
            this.builder = builder;
        }

        @Override
        public Widget build(BuildContext context) {
            return builder == null ? null : builder.call(context);
        }
    }
}
