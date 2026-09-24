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

    /**
     * Shows the dialog and returns a future that completes when it closes, with the
     * value handed to {@code Navigator.pop} -- null when it closed any other way. It
     * returned nothing, so {@code final ok = await showDialog(...)} could not be written
     * and code after an awaited dialog ran while it was still open.
     */
    public static dart.async.Future<Object> showDialog(BuildContext context, Funcs.Func1<BuildContext, Widget> builder) {
        return present(builder, BorderLayout.CENTER, false);
    }

    /**
     * A sheet that rises from the bottom edge, full width -- the shape of a modal popup
     * rather than of a dialog.
     *
     * <p>Presenting one as a centred, packed dialog is not a near miss. A sheet states a
     * height and leaves its width to the presentation, so packing sized it to its
     * content: the Cupertino picker demo's 216-high sheet came up as a tall narrow strip
     * down the middle of the screen instead of a full-width wheel along the bottom.</p>
     *
     * <p>Dismissible by touching outside it, as the reference is by default.</p>
     */
    public static dart.async.Future<Object> showModalPopup(BuildContext context,
            Funcs.Func1<BuildContext, Widget> builder) {
        return present(builder, BorderLayout.SOUTH, true, true);
    }

    private static dart.async.Future<Object> present(Funcs.Func1<BuildContext, Widget> builder, String position,
            boolean dismissOnOutsideTouch) {
        return present(builder, position, dismissOnOutsideTouch, false);
    }

    private static dart.async.Future<Object> present(Funcs.Func1<BuildContext, Widget> builder, String position,
            boolean dismissOnOutsideTouch, boolean stretch) {
        DialogWidget rootWidget = new DialogWidget(builder);
        final DialogEntry e = new DialogEntry();
        if (Display.isInitialized()) {
            // Codename One disposes a dialog itself when it is touched outside its bounds,
            // which is how a modal popup is dismissed. That path never reached this stack:
            // the entry stayed, so the next Navigator.pop consumed it instead of popping
            // the page, and the dismissed popup's element tree stayed mounted. Every way
            // the dialog closes now goes through close().
            com.codename1.ui.Dialog d = new com.codename1.ui.Dialog(new BorderLayout()) {
                @Override
                public void dispose() {
                    super.dispose();
                    close(e);
                }
            };
            d.setDisposeWhenPointerOutOfBounds(dismissOnOutsideTouch);
            Container c = FlutterUI.wrap(rootWidget);
            d.add(BorderLayout.CENTER, c);
            e.dialog = d;
            e.root = ((FlutterRootLayout) c.getLayout()).host().rootElement();
            dialogStack.add(e);
            // modeless: returns immediately, the calling code keeps running.
            //
            // STRETCHED for a sheet: packing sizes both axes to the content, and a sheet
            // states only its height -- so a 216-high picker came up as a tall narrow
            // strip instead of spanning the screen.
            if (stretch) {
                d.showStretched(position, false);
            } else {
                d.showPacked(position, false);
            }
        } else {
            RenderHost host = new RenderHost();
            e.root = FlutterUI.mount(rootWidget, host, new BuildOwner());
            dialogStack.add(e);
        }
        return e.result.future();
    }

    /**
     * Dismisses the topmost open dialog. Returns false when none is open —
     * the caller (Navigator.pop) then pops a route instead.
     */
    public static boolean popTopDialog() {
        return popTopDialog(null);
    }

    /** {@link #popTopDialog()} completing the dialog's future with {@code result}. */
    public static boolean popTopDialog(Object result) {
        if (dialogStack.isEmpty()) {
            return false;
        }
        DialogEntry e = dialogStack.get(dialogStack.size() - 1);
        e.popResult = result;
        close(e);
        if (e.dialog != null) {
            e.dialog.dispose();
        }
        return true;
    }

    /** Takes a dialog off the stack and unmounts its subtree -- once, however it closed. */
    private static void close(DialogEntry e) {
        if (e.closed) {
            return;
        }
        e.closed = true;
        dialogStack.remove(e);
        if (e.root != null) {
            FlutterUI.unmountTree(e.root);
        }
        e.result.complete(e.popResult);
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
        boolean closed;
        /** What Navigator.pop handed back; null when the dialog closed another way. */
        Object popResult;
        final dart.async.Completer<Object> result = new dart.async.Completer<Object>();
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
