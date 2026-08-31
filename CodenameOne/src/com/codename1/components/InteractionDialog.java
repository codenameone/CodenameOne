/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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

import com.codename1.ui.CN;
import com.codename1.ui.AbstractDialog;
import com.codename1.ui.Button;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Desktop;
import com.codename1.ui.Dialog;
import com.codename1.ui.TopLevelContainer;
import com.codename1.ui.Window;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.WindowEvent;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.plaf.Border;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.animations.Transition;

/// Unlike a regular dialog the interaction dialog only looks like a dialog,
/// it resides in the layered pane and can be used to implement features where
/// interaction with the background form is still required.
///
/// Since this code is designed for interaction all "dialogs" created thru here are
/// modless and never block.
///
/// ```java
/// InteractionDialog dlg = new InteractionDialog("Hello");
/// dlg.setLayout(new BorderLayout());
/// dlg.add(BorderLayout.CENTER, new Label("Hello Dialog"));
/// Button close = new Button("Close");
/// close.addActionListener((ee) -> dlg.dispose());
/// dlg.addComponent(BorderLayout.SOUTH, close);
/// Dimension pre = dlg.getContentPane().getPreferredSize();
/// dlg.show(0, 0, Display.getInstance().getDisplayWidth() - (pre.getWidth() + pre.getWidth() / 6), 0);
/// ```
///
/// @author Shai Almog
public class InteractionDialog extends Container implements AbstractDialog {
    private static final Runnable BLOCKING_SLEEP = new BlockingSleepRunnable();

    private static class BlockingSleepRunnable implements Runnable {
        @Override
        public void run() {
            com.codename1.io.Util.sleep(10);
        }
    }

    private final Label title = new Label();
    private final Container titleArea = new Container(new BorderLayout());
    private final Container dialogBody = new Container(new BorderLayout());
    private final Container contentPane;
    private boolean animateShow = true;
    private boolean repositionAnimation = true;
    private boolean disposed;
    private boolean disposeWhenPointerOutOfBounds;
    private int animationSpeed = -1;
    private Runnable showAnimationSetup;
    private Runnable disposeAnimationSetup;
    private boolean titleCentered = Dialog.isDefaultTitleCentered();

    /// Whether the interaction dialog uses the form layered pane of the regular layered pane
    private boolean formMode;

    /// Opt-in "special mode" (see `#setStackable(boolean)`) that makes dispose remove
    /// only this dialog's own component from the shared layered pane instead of clearing
    /// the whole layer.
    private static boolean stackable;

    /// Records the `formMode` value used by the most recent `#show(int, int, int, int)`
    /// so dispose cleans up the matching layered pane even if `formMode` is toggled in
    /// between (e.g. by `#showPopupDialog(Component)`).
    private boolean shownInFormMode;

    private boolean pressedOutOfBounds;
    private ActionListener pressedListener;
    private ActionListener releasedListener;
    private Command lastCommandPressed;

    /// Default constructor with no title
    public InteractionDialog() {
        super(new BorderLayout());
        contentPane = new Container();
        init();
    }

    /// Default constructor with layout
    ///
    /// #### Parameters
    ///
    /// - `l`: layout
    public InteractionDialog(Layout l) {
        super(new BorderLayout());
        contentPane = new Container(l);
        init();
    }

    /// Constructor with dialog title
    ///
    /// #### Parameters
    ///
    /// - `title`: the title of the dialog
    public InteractionDialog(String title) {
        super(new BorderLayout());
        contentPane = new Container();
        this.title.setText(title);
        init();
    }


    /// Constructor with dialog title
    ///
    /// #### Parameters
    ///
    /// - `title`: the title of the dialog
    ///
    /// - `l`: the layout for the content pane
    public InteractionDialog(String title, Layout l) {
        super(new BorderLayout());
        contentPane = new Container(l);
        this.title.setText(title);
        init();
    }

    private void init() {
        setUIIDFinal("Dialog");
        title.setUIID("DialogTitle");
        contentPane.setUIID("DialogContentPane");
        dialogBody.setUIID("Container");
        titleArea.addComponent(BorderLayout.CENTER, title);
        updateTitleLayout();
        setGrabsPointerEvents(true);
    }

    @Override
    protected void initComponent() {
        super.initComponent();
        installPointerOutOfBoundsListeners();
    }

    /// This flag indicates if the dialog should be disposed if a pointer
    /// released event occurred out of the dialog content.
    ///
    /// #### Returns
    ///
    /// true if the dialog should dispose
    public boolean isDisposeWhenPointerOutOfBounds() {
        return disposeWhenPointerOutOfBounds;
    }

    /// This flag indicates if the dialog should be disposed if a pointer
    /// released event occurred out of the dialog content.
    ///
    /// #### Parameters
    ///
    /// - `disposeWhenPointerOutOfBounds`
    public void setDisposeWhenPointerOutOfBounds(boolean disposeWhenPointerOutOfBounds) {
        this.disposeWhenPointerOutOfBounds = disposeWhenPointerOutOfBounds;
    }

    /// Returns the body of the interaction dialog
    ///
    /// #### Returns
    ///
    /// the container where the elements of the interaction dialog are added.
    public Container getContentPane() {
        return contentPane;
    }

    /// {@inheritDoc}
    @Override
    public void setScrollable(boolean scrollable) {
        getContentPane().setScrollable(scrollable);
    }

    /// {@inheritDoc}
    @Override
    public Layout getLayout() {
        return contentPane.getLayout();
    }

    /// {@inheritDoc}
    @Override
    public void setLayout(Layout layout) {
        contentPane.setLayout(layout);
    }

    /// Gets this dialog title text.
    public String getTitle() {
        return title.getText();
    }

    /// Sets this dialog title text.
    ///
    /// #### Parameters
    ///
    /// - `title`: the title text.
    public void setTitle(String title) {
        this.title.setText(title);
        // Onto the operating system's title bar too while this dialog is backed by a
        // window of its own, exactly as Dialog does. The window took a copy when it was
        // created and this label is hidden behind the native title bar, so without this
        // a setTitle from initNativeWindow or after a modeless show would change
        // nothing the user can see.
        if (nativeWindow != null && !nativeWindow.isWindowDisposed()) {
            nativeWindow.setTitle(title == null ? "" : title);
        }
    }

    /// {@inheritDoc}
    @Override
    public void addComponent(Component cmp) {
        contentPane.addComponent(cmp);
    }

    /// {@inheritDoc}
    @Override
    public void addComponent(Object constraints, Component cmp) {
        contentPane.addComponent(constraints, cmp);
    }

    /// {@inheritDoc}
    @Override
    public void addComponent(int index, Object constraints, Component cmp) {
        contentPane.addComponent(index, constraints, cmp);
    }

    /// {@inheritDoc}
    @Override
    public void addComponent(int index, Component cmp) {
        contentPane.addComponent(index, cmp);
    }

    /// {@inheritDoc}
    @Override
    public void removeAll() {
        contentPane.removeAll();
    }

    /// {@inheritDoc}
    @Override
    public void removeComponent(Component cmp) {
        contentPane.removeComponent(cmp);
    }

    /// Gets the label component used to display the title.
    ///
    /// #### Returns
    ///
    /// The title label component.
    public Label getTitleComponent() {
        return title;
    }

    /// Returns whether this interaction dialog places its title in the absolute
    /// center with the body below it.
    ///
    /// #### Returns
    ///
    /// true when the centered title layout is active
    public boolean isTitleCentered() {
        return titleCentered;
    }

    /// Places the title in the absolute center with the body below it. Passing
    /// false restores the traditional title-at-top layout.
    ///
    /// #### Parameters
    ///
    /// - `titleCentered`: true to use the centered title layout
    public void setTitleCentered(boolean titleCentered) {
        if (this.titleCentered == titleCentered) {
            return;
        }
        this.titleCentered = titleCentered;
        updateTitleLayout();
        revalidate();
    }

    private void updateTitleLayout() {
        dialogBody.removeAll();
        BorderLayout titleLayout = (BorderLayout) titleArea.getLayout();
        if (titleCentered) {
            titleArea.setUIID(getUIManager().getThemeConstant(
                    "dlgCenteredTitleUIID", "Container"));
            titleLayout.setCenterBehavior(BorderLayout.CENTER_BEHAVIOR_CENTER_ABSOLUTE);
            dialogBody.addComponent(BorderLayout.CENTER, titleArea);
            dialogBody.addComponent(BorderLayout.SOUTH, contentPane);
        } else {
            titleArea.setUIID("Container");
            titleLayout.setCenterBehavior(BorderLayout.CENTER_BEHAVIOR_SCALE);
            dialogBody.addComponent(BorderLayout.NORTH, titleArea);
            dialogBody.addComponent(BorderLayout.CENTER, contentPane);
        }
        if (dialogBody.getParent() == null) {
            super.addComponent(BorderLayout.CENTER, dialogBody);
        }
    }

    private int resolveAnimationSpeed() {
        if (animationSpeed >= 0) {
            return animationSpeed;
        }
        return getUIManager().getThemeConstant("interactionDialogSpeedInt", 400);
    }

    /// The top level this dialog appears on when it is shown.
    ///
    /// A dialog is not attached to anything at the moment `#show(int, int, int, int)`
    /// runs, so it cannot resolve its own host the way an attached component can. Left
    /// unset it uses the current `Form`, which is the historical behaviour and the
    /// right answer for an application with one window. Set it to put the dialog on a
    /// `Window` instead: without it the dialog is added to the main
    /// form's layered pane, so it appears on the main window while the window that
    /// asked for it is merely dimmed -- and in an application with no form at all
    /// there is nothing to resolve and showing it fails.
    ///
    /// #### Parameters
    ///
    /// - `host`: the top level to show on, or null for the current form
    /// Whether this dialog is backed by a real operating system window.
    ///
    /// Resolved when the dialog is shown: what `#setNativeWindowMode(boolean)` was
    /// told, else `Dialog#isDefaultNativeWindowMode()` and the theme constant behind
    /// it. A platform with no windowing system ignores all of it and shows the dialog
    /// on its host's layered pane as before.
    ///
    /// #### Returns
    ///
    /// true when this dialog asks for its own window
    public boolean isNativeWindowMode() {
        if (nativeWindowMode != null) {
            return nativeWindowMode.booleanValue();
        }
        return Dialog.isDefaultNativeWindowMode();
    }

    /// Sets whether this dialog is backed by a real operating system window.
    ///
    /// Takes effect the next time the dialog is shown.
    ///
    /// #### Parameters
    ///
    /// - `nativeWindowMode`: true to open this dialog in its own window
    public void setNativeWindowMode(boolean nativeWindowMode) {
        this.nativeWindowMode = Boolean.valueOf(nativeWindowMode);
    }

    /// The window backing this dialog while it is showing.
    ///
    /// #### Returns
    ///
    /// the window, or null when the dialog is not in one
    public Window getNativeWindow() {
        return nativeWindow;
    }

    /// Called once the window backing this dialog has been configured and before it is
    /// shown, so an application can adjust it.
    ///
    /// #### Parameters
    ///
    /// - `w`: the window about to be shown
    protected void initNativeWindow(Window w) {
    }

    /// Whether this showing opens a real operating system window.
    ///
    /// An anchored popup never does: it points at a rectangle in its host's coordinate
    /// space, and a separate window neither shares that space nor sees the click that
    /// is meant to dismiss it.
    ///
    /// #### Returns
    ///
    /// true to open a window of its own
    private boolean usesNativeWindow() {
        return Desktop.isSupported() && !inPopupShow && isNativeWindowMode();
    }

    /// True while an anchored popup is being shown.
    private boolean inPopupShow;

    /// Shows this dialog as a real operating system window.
    ///
    /// #### Parameters
    ///
    /// - `modal`: whether to park the caller until the dialog goes
    private void showInNativeWindow(boolean modal) {
        // Showing again without disposing first is something the lightweight path
        // tolerates, so this one has to as well. Overwriting the field left the first
        // window on screen and empty -- the payload having moved into the second --
        // while its own close and dispose bridges went on acting on this dialog, whose
        // window was now the second one: closing the abandoned window tore down the
        // showing the user was actually looking at.
        Window previous = nativeWindow;
        if (previous != null) {
            finishNativeShowing();
            if (!previous.isWindowDisposed()) {
                previous.dispose();
            }
            disposed = false;
        }
        TopLevelContainer host = resolveHost();
        Window w = new Window(
                getTitle() == null ? "" : getTitle(), new BorderLayout());
        nativeWindow = w;
        if (host != null) {
            w.setOwnerWindow(host);
        }
        w.setCloseOperation(Window.DO_NOTHING_ON_CLOSE);
        w.setResizable(false);
        w.setDecorated(true);
        w.getContentPane().setScrollableY(false);
        Style unselectedStyle = getUnselectedStyle();
        unselectedStyle.setMarginUnit(Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS,
                Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS);
        unselectedStyle.setMargin(TOP, 0);
        unselectedStyle.setMargin(BOTTOM, 0);
        unselectedStyle.setMargin(LEFT, 0);
        unselectedStyle.setMargin(RIGHT, 0);
        remove();
        w.getContentPane().addComponent(BorderLayout.CENTER, this);
        w.addCloseListener(new NativeCloseBridge(this));
        w.addWindowListener(new NativeDisposeBridge(this));
        initNativeWindow(w);
        hideOwnTitleIfDecorated(w);
        revalidate();
        if (modal) {
            w.setModalityType(Window.MODALITY_WINDOW);
        }
        // Shown before it is sized: the size that matters is the drawable, and a window
        // cannot report how much of its frame is chrome until the platform has made
        // one. Sizing first asked for a frame the size of the content and left the
        // dialog clipped by the height of the title bar.
        w.show();
        // Onto the owner's display first, for the reason the Dialog path does it: the
        // conversions below and inside setWindowContentSize read the window's own
        // monitor, so sizing before the move measured against whichever display the
        // platform opened it on.
        placeNativeWindow(w, host);
        int cw = Math.max(1, getPreferredW());
        int ch = Math.max(1, getPreferredH());
        // Clamped to the monitor, as the Dialog path is. The window is not resizable and
        // is centred, so content that prefers more than the screen -- a large image, a
        // long line that does not wrap -- put its own controls off both edges at once,
        // where nothing can reach them. Never null: Desktop reports a single monitor
        // covering the display even where there is no windowing system. In device pixels
        // rather than desktop coordinates: compared raw, the cap on a scaled display came
        // out at half what it should be.
        Rectangle work = w.getWorkAreaInPixels();
        if (work.getWidth() > 0) {
            cw = Math.min(cw, work.getWidth() * 9 / 10);
        }
        if (work.getHeight() > 0) {
            ch = Math.min(ch, work.getHeight() * 9 / 10);
        }
        w.setWindowContentSize(cw, ch);
        // Centred again at the size it ended up rather than the one it opened with.
        placeNativeWindow(w, host);
        startPendingTimeout(w);
        if (modal) {
            // Idempotent for a window already on screen: it takes the blocker and parks
            // the caller without showing anything twice.
            w.showModal();
            // Disposed rather than just detached, for the reason the Dialog path is:
            // hiding the window ends the modal wait too, and finishNativeShowing() alone
            // would leave the peer registered with its owner once per showing.
            if (!w.isWindowDisposed()) {
                w.dispose();
            }
            finishNativeShowing();
        }
    }

    /// Ends a showing that is still up in the representation this one is not using.
    ///
    /// The mode is read when the dialog is shown, so a caller that changes it while the
    /// dialog is up and shows again would otherwise leave both alive. Going to the
    /// lightweight layer reparented the payload and left the operating system window on
    /// screen and empty, with `nativeWindow` still set -- so the next `#dispose()` took
    /// the native path and never tore the layer down. Going the other way pulled a live
    /// lightweight dialog out of its layer while it still counted as showing, so the
    /// pointer listeners it had given its host were never reclaimed.
    ///
    /// `#dispose()` already knows how to end either one, so this only decides whether
    /// there is a mismatched showing to end.
    ///
    /// #### Parameters
    ///
    /// - `wantsNative`: true when this showing is about to open its own window
    private void finishShowingInOtherMode(boolean wantsNative) {
        boolean showingNative = nativeWindow != null;
        boolean showingLightweight = !showingNative && getParent() != null;
        if ((showingNative && !wantsNative) || (showingLightweight && wantsNative)) {
            dispose();
        }
    }

    /// Puts a native-window dialog on its owner's display, centred.
    private static void placeNativeWindow(Window w, TopLevelContainer host) {
        if (host != null) {
            w.centerOn(host);
        } else {
            w.centerOnDesktop();
        }
    }

    /// Hides the dialog's own title while the platform draws one in the window chrome.
    ///
    /// Without this a titled dialog in a decorated window shows the same text twice:
    /// once in the native title bar and once in the payload underneath it.
    ///
    /// #### Parameters
    ///
    /// - `w`: the window about to be shown
    private void hideOwnTitleIfDecorated(Window w) {
        if (!w.isDecorated() || nativeTitleHidden) {
            return;
        }
        nativeTitleHidden = true;
        // What they were, not what they usually are: an application is free to suppress
        // the title before showing, and this dialog is reusable, so putting both back
        // visible would resurrect title UI that was deliberately hidden.
        titleAreaWasVisible = titleArea.isVisible();
        titleWasVisible = title.isVisible();
        titleArea.setVisible(false);
        title.setVisible(false);
    }

    /// Puts the dialog's own title back.
    private void restoreOwnTitle() {
        if (!nativeTitleHidden) {
            return;
        }
        nativeTitleHidden = false;
        titleArea.setVisible(titleAreaWasVisible);
        title.setVisible(titleWasVisible);
    }

    /// True while the dialog's own title is hidden because the window draws one.
    private boolean nativeTitleHidden;

    /// Whether the title area was visible before the native window hid it.
    private boolean titleAreaWasVisible;

    /// Whether the title label was visible before the native window hid it.
    private boolean titleWasVisible;

    /// Takes this dialog back out of its window. Idempotent, and on the event dispatch
    /// thread.
    void finishNativeShowing() {
        if (!CN.isEdt()) {
            CN.callSeriallyAndWait(new Runnable() {
                @Override
                public void run() {
                    finishNativeShowing();
                }
            });
            return;
        }
        if (nativeWindow == null) {
            return;
        }
        nativeWindow = null;
        disposed = true;
        // This showing is over too, and it did not come through dispose(): the window
        // was disposed from outside -- an owner cascade, getNativeWindow().dispose(), or
        // a modal window being hidden. The clock outlives the window it was armed
        // under, so leaving it armed here lets it close a later showing.
        retireArmedTimeout();
        restoreOwnTitle();
        if (getParent() != null) {
            remove();
        }
    }

    /// The user activated the window's own close control.
    void nativeCloseRequested(ActionEvent evt) {
        evt.consume();
        dispose();
    }

    /// Routes the window's close control back into the dialog.
    private static final class NativeCloseBridge implements ActionListener {
        private final InteractionDialog dlg;

        NativeCloseBridge(InteractionDialog dlg) {
            this.dlg = dlg;
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            dlg.nativeCloseRequested(evt);
        }
    }

    /// Tears the dialog down however its window died.
    private static final class NativeDisposeBridge implements ActionListener {
        private final InteractionDialog dlg;

        NativeDisposeBridge(InteractionDialog dlg) {
            this.dlg = dlg;
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            if (evt instanceof WindowEvent
                    && ((WindowEvent) evt).getType()
                        == WindowEvent.Type.Disposed) {
                dlg.finishNativeShowing();
            }
        }
    }

    public void setTopLevelHost(TopLevelContainer host) {
        this.hostTopLevel = host;
        // An explicit choice replaces an inferred one outright, and there is no longer
        // an earlier host worth restoring.
        this.hostTopLevelInferred = false;
        this.hostTopLevelBeforeInference = null;
    }

    /// Returns the top level set with `#setTopLevelHost(TopLevelContainer)`.
    ///
    /// #### Returns
    ///
    /// the explicit host, or null when none was set
    public TopLevelContainer getTopLevelHost() {
        return hostTopLevel;
    }

    /// The top level to operate on: the explicit host, else the one this dialog is
    /// already attached to, else the current form.
    ///
    /// #### Returns
    ///
    /// the host top level, or null when there is none
    private TopLevelContainer resolveHost() {
        if (hostTopLevel != null) {
            return hostTopLevel;
        }
        TopLevelContainer attached = getTopLevelContainer();
        if (attached != null) {
            return attached;
        }
        // The top level the user is in, not the current form. Falling back to the form
        // put a dialog opened from a focused window onto the main surface, owned by it
        // and blocking it, while the window it came from stayed live. This is the
        // fallback AbstractDialog documents and the one Dialog already uses.
        return CN.getCurrentTopLevel();
    }

    private TopLevelContainer hostTopLevel;

    /// What this dialog was told about native window mode, or null when it was not
    /// told. A `Boolean` rather than a boolean so "unset" is distinguishable from off.
    private Boolean nativeWindowMode;

    /// The window backing this dialog, non-null for exactly one native mode showing.
    private Window nativeWindow;

    /// A timeout set before the dialog was shown, waiting for a host to bind to.
    private long pendingTimeout;

    /// True while `#hostTopLevel` holds a host worked out from a popup's anchor rather
    /// than one the application asked for. Such a host belongs to that one showing: it
    /// is the anchor's top level, and the next showing may well be somewhere else.
    private boolean hostTopLevelInferred;

    /// The host that was in force before a popup inferred one, put back when the popup
    /// goes away.
    private TopLevelContainer hostTopLevelBeforeInference;

    /// Drops a host inferred from a popup's anchor and restores whatever was set before
    /// it.
    ///
    /// Without this the inferred host outlived the popup in the same field the explicit
    /// API writes to, so showing the same dialog again through `#show(int, int, int,
    /// int)` put it back on the window the popup happened to be anchored in. If that
    /// window had since been disposed the dialog went into a hierarchy attached to
    /// nothing and simply never appeared.
    private void releaseInferredHost() {
        if (hostTopLevelInferred) {
            hostTopLevel = hostTopLevelBeforeInference;
            hostTopLevelBeforeInference = null;
            hostTopLevelInferred = false;
        }
    }

    private void cleanupLayer(TopLevelContainer f) {
        if (stackable) {
            // Stackable mode: several InteractionDialogs can share the class
            // layer at once (layered by show() order). Tearing the whole layer
            // down here would wipe the sibling dialogs that are still showing
            // (#5193). Remove the shared layer only once the last dialog has
            // left it, so it neither nukes siblings nor lingers empty. Use the
            // mode captured at show() time so we clean the pane the dialog was
            // actually added to even if formMode changed in the meantime.
            Container c = shownInFormMode
                    ? f.getFormLayeredPane(InteractionDialog.class, true)
                    : f.getLayeredPane(InteractionDialog.class, true);
            if (c.getComponentCount() == 0) {
                c.remove();
            }
            return;
        }
        if (formMode) {
            Container c = f.getFormLayeredPane(InteractionDialog.class, true);
            c.removeAll();
            c.remove();
        }
    }

    private Container getLayeredPane(TopLevelContainer f) {
        //return f.getLayeredPane();
        Container c;
        if (formMode) {
            c = f.getFormLayeredPane(InteractionDialog.class, true);
        } else {
            c = f.getLayeredPane(InteractionDialog.class, true);
        }
        if (!(c.getLayout() instanceof LayeredLayout)) {
            c.setLayout(new LayeredLayout());
        }

        return c;
    }

    /// {@inheritDoc}
    @Override
    protected void deinitialize() {
        super.deinitialize();
        if (disposed) {
            TopLevelContainer f = getTopLevelContainer();
            if (f != null) {
                if (pressedListener != null) {
                    f.asContainer().removePointerPressedListener(pressedListener);
                }
                if (releasedListener != null) {
                    f.asContainer().removePointerReleasedListener(releasedListener);
                }
                Container pp = getLayeredPane(f);
                Container p = getParent();
                remove();
                if (p.getComponentCount() == 0) {
                    p.remove();
                }
                //pp.removeAll();
                pp.revalidateLater();
                cleanupLayer(f);
            }
        }
    }

    public void resize(final int top, final int bottom, final int left, final int right) {
        if (!disposed) {
            final TopLevelContainer f = resolveHost();
            if (f == null) {
                return;
            }

            Style unselectedStyle = getUnselectedStyle();

            unselectedStyle.setMargin(TOP, Math.max(0, top));
            unselectedStyle.setMargin(BOTTOM, Math.max(0, bottom));
            unselectedStyle.setMargin(LEFT, Math.max(0, left));
            unselectedStyle.setMargin(RIGHT, Math.max(0, right));
            unselectedStyle.setMarginUnit(Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS);

            getParent().setX(getX());
            getParent().setY(getY());
            setX(0);
            setY(0);
            getParent().setWidth(getWidth());
            getParent().setHeight(getHeight());

            getLayeredPane(f).animateLayout(resolveAnimationSpeed());
        }
    }

    /// This method shows the form as a modal alert allowing us to produce a behavior
    /// of an alert/dialog box. This method will block the calling thread even if the
    /// calling thread is the EDT. Notice that this method will not release the block
    /// until dispose is called even if show() from another form is called!
    ///
    /// Modal dialogs Allow the forms "content" to "hang in mid air" this is especially useful for
    /// dialogs where you would want the underlying form to "peek" from behind the
    /// form.
    ///
    /// #### Parameters
    ///
    /// - `top`: space in pixels between the top of the screen and the form
    ///
    /// - `bottom`: space in pixels between the bottom of the screen and the form
    ///
    /// - `left`: space in pixels between the left of the screen and the form
    ///
    /// - `right`: space in pixels between the right of the screen and the form
    public void show(int top, int bottom, int left, int right) {
        getUnselectedStyle().setOpacity(255);
        boolean wantsNative = usesNativeWindow();
        finishShowingInOtherMode(wantsNative);
        disposed = false;
        if (wantsNative) {
            showInNativeWindow(false);
            return;
        }
        TopLevelContainer f = resolveHost();
        if (f == null) {
            return;
        }
        shownInFormMode = formMode;
        startPendingTimeout();
        Style unselectedStyle = getUnselectedStyle();

        unselectedStyle.setMargin(TOP, top);
        unselectedStyle.setMargin(BOTTOM, bottom);
        unselectedStyle.setMargin(LEFT, left);
        unselectedStyle.setMargin(RIGHT, right);
        unselectedStyle.setMarginUnit(Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS);

        // might occur when showing the dialog twice...
        remove();

        // We issue a revalidate in case this is the first time the layered pane
        // appears in the form.  Without this, the "show" animation won't work
        // the first time.
        getLayeredPane(f).revalidate();

        getLayeredPane(f).addComponent(BorderLayout.center(this));
        if (animateShow) {
            if (showAnimationSetup != null) {
                showAnimationSetup.run();
            } else if (repositionAnimation) {
                int x = left + (f.asContainer().getWidth() - right - left) / 2;
                int y = top + (f.asContainer().getHeight() - bottom - top) / 2;
                getParent().setX(x);
                getParent().setY(y);
                getParent().setWidth(1);
                getParent().setHeight(1);
            } else {
                getParent().setX(getX());
                getParent().setY(getY());
                setX(0);
                setY(0);
                getParent().setWidth(getWidth());
                getParent().setHeight(getHeight());
            }
            getLayeredPane(f).animateLayout(resolveAnimationSpeed());
        } else {
            //getLayeredPane(f).revalidate();
            f.asContainer().revalidateWithAnimationSafety();
        }
        /*
        Form f = Display.getInstance().getCurrent();
        f.getLayeredPane().setLayout(new BorderLayout());
        getUnselectedStyle().setMargin(TOP, top);
        getUnselectedStyle().setMargin(BOTTOM, bottom);
        getUnselectedStyle().setMargin(LEFT, left);
        getUnselectedStyle().setMargin(RIGHT, right);
        getUnselectedStyle().setMarginUnit(new byte[] {Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS});
        f.getLayeredPane().addComponent(BorderLayout.CENTER, this);
        if(animateShow) {
            int x = left + (f.getWidth() - right - left) / 2;
            int y = top + (f.getHeight() - bottom - top) / 2;
            setX(x);
            setY(y);
            setWidth(1);
            setHeight(1);
            f.getLayeredPane().animateLayout(400);
        } else {
            f.getLayeredPane().revalidate();
        }
        */
    }

    /// Removes the interaction dialog from view
    @Override
    public void dispose() {
        disposed = true;
        retireArmedTimeout();
        releaseInferredHost();
        if (nativeWindow != null) {
            // The window fires Disposed, which is what takes the dialog back out. None
            // of the layered pane teardown below applies -- there is no layer.
            nativeWindow.dispose();
            return;
        }
        Container p = getParent();
        if (p != null) {
            TopLevelContainer f = p.getTopLevelContainer();
            if (f != null) {
                if (animateShow) {
                    if (disposeAnimationSetup != null) {
                        disposeAnimationSetup.run();
                    } else if (repositionAnimation) {
                        setX(getX() + getWidth() / 2);
                        setY(getY() + getHeight() / 2);
                        setWidth(1);
                        setHeight(1);
                    }
                    p.animateUnlayoutAndWait(resolveAnimationSpeed(), 100);
                }
                Container pp = getLayeredPane(f);
                remove();
                if (p.getComponentCount() == 0) {
                    p.remove();
                }
                //p.remove();
                //pp.removeAll();

                pp.revalidate();
                cleanupLayer(f);
                // remove() above triggers the recursive deinitialize()
                // path which already runs cleanupLayer() and detaches the
                // layered pane wrapper, so by the time pp.revalidate()
                // runs pp has no Form in its parent chain and never
                // bubbles a repaint up to the form. The animateShow path
                // is masked because the animation itself drives a paint
                // cycle. Without it, dispose() can leave the old dialog
                // pixels on screen until something else (scroll, hover)
                // forces a redraw (#5067). Force a form-level revalidate
                // so the next paint cycle clears those pixels.
                f.asContainer().revalidateWithAnimationSafety();
            } else {
                p.remove();
            }
        }
    }

    /// Removes the interaction dialog from view with an animation to the left
    public void disposeToTheLeft() {
        disposeTo(Component.LEFT);
    }

    /// Removes the interaction dialog from view with an animation to the left.
    ///
    /// #### Parameters
    ///
    /// - `onFinish`: Callback called when dispose animation is complete.
    public void disposeToTheLeft(Runnable onFinish) {
        disposeTo(Component.LEFT, onFinish);
    }

    /// Removes the interaction dialog from view with an animation to the bottom
    public void disposeToTheBottom() {
        disposeTo(Component.BOTTOM);
    }

    /// Removes the interaction dialog from view with an animation to the bottom
    ///
    /// #### Parameters
    ///
    /// - `onFinish`: Callback called when dispose animation is complete.
    public void disposeToTheBottom(Runnable onFinish) {
        disposeTo(Component.BOTTOM, onFinish);
    }

    /// Removes the interaction dialog from view with an animation to the top
    public void disposeToTheTop() {
        disposeTo(Component.TOP);
    }

    /// Removes the interaction dialog from view with an animation to the top.
    ///
    /// #### Parameters
    ///
    /// - `onFinish`: Callback called when dispose animation is complete.
    public void disposeToTheTop(Runnable onFinish) {
        disposeTo(Component.TOP, onFinish);
    }

    /// Removes the interaction dialog from view with an animation to the right
    public void disposeToTheRight() {
        disposeTo(Component.RIGHT);
    }

    /// Removes the interaction dialog from view with an animation to the right.
    ///
    /// #### Parameters
    ///
    /// - `onFinish`: Callback called when dispose animation is complete.
    public void disposeToTheRight(Runnable onFinish) {
        disposeTo(Component.RIGHT, onFinish);
    }

    private void disposeTo(int direction) {
        disposeTo(direction, null);
    }

    private void disposeTo(int direction, final Runnable onFinish) {
        disposed = true;
        releaseInferredHost();
        if (nativeWindow != null) {
            // There is no layered pane to slide out of, and no direction that means
            // anything for a window the platform draws. Animating the payload here
            // would leave the operating system window on screen and, for a modal
            // showDialog(), its caller parked on a window that never goes away.
            nativeWindow.dispose();
            if (onFinish != null) {
                onFinish.run();
            }
            return;
        }
        final Container p = getParent();
        if (p != null) {
            final TopLevelContainer f = p.getTopLevelContainer();
            if (f != null) {
                switch (direction) {
                    case Component.LEFT:
                        setX(-getWidth());
                        break;
                    case Component.TOP:
                        setY(-getHeight());
                        break;
                    case Component.RIGHT:
                        // Off the edge of the host, not of the main display. A window
                        // larger than the main surface left this target still inside
                        // the window, so the dialog sat there until it was removed
                        // outright instead of animating out.
                        setX(f.asContainer().getWidth());
                        break;
                    case Component.BOTTOM:
                        setY(f.asContainer().getHeight());
                        break;
                    default:
                        break;
                }

                if (animateShow) {
                    p.animateUnlayout(resolveAnimationSpeed(), 255, new Runnable() {
                        @Override
                        public void run() {
                            if (p.getParent() != null) {
                                Container pp = getLayeredPane(f);
                                remove();
                                p.remove();
                                if (!stackable) {
                                    // In stackable mode removeAll() would wipe
                                    // the other dialogs sharing this layer; the
                                    // remove()/p.remove() above already detached
                                    // just this dialog (#5193).
                                    pp.removeAll();
                                }
                                pp.revalidate();
                                cleanupLayer(f);
                            }
                            if (onFinish != null) {
                                onFinish.run();
                            }
                        }
                    });
                } else {
                    p.revalidate();
                    Container pp = getLayeredPane(f);
                    remove();
                    p.remove();
                    if (!stackable) {
                        // See the animated branch above: removeAll() would
                        // discard sibling dialogs sharing this layer (#5193).
                        pp.removeAll();
                    }
                    pp.revalidate();
                    if (stackable) {
                        // Unlike the animated branch, this path never called
                        // cleanupLayer(). With removeAll() now skipped we must
                        // still tear the shared layer down once it is empty so
                        // layers don't accumulate (#5193).
                        cleanupLayer(f);
                    }
                    if (onFinish != null) {
                        onFinish.run();
                    }
                }
            } else {
                remove();
                if (onFinish != null) {
                    onFinish.run();
                }
            }
        }
    }

    /// Will return true if the dialog is currently showing
    ///
    /// #### Returns
    ///
    /// true if showing
    public boolean isShowing() {
        return getParent() != null;
    }

    /// Indicates whether show/dispose should be animated or not. When true (the default)
    /// the dialog animates into view on `#show(int, int, int, int)` and out on
    /// `#dispose()` over `interactionDialogSpeedInt` (default 400ms). When false, both
    /// transitions are immediate. This flag also gates `#isRepositionAnimation()`:
    /// the grow/shrink behavior of `repositionAnimation` only takes effect when
    /// `animateShow` is true.
    ///
    /// #### Returns
    ///
    /// the animateShow
    public boolean isAnimateShow() {
        return animateShow;
    }

    /// Indicates whether show/dispose should be animated or not. When true (the default)
    /// the dialog animates into view on `#show(int, int, int, int)` and out on
    /// `#dispose()` over `interactionDialogSpeedInt` (default 400ms). When false, both
    /// transitions are immediate. This flag also gates `#setRepositionAnimation(boolean)`:
    /// the grow/shrink behavior of `repositionAnimation` only takes effect when
    /// `animateShow` is true.
    ///
    /// #### Parameters
    ///
    /// - `animateShow`: the animateShow to set
    public void setAnimateShow(boolean animateShow) {
        this.animateShow = animateShow;
    }

    /// Duration in milliseconds used by the show, dispose and resize animations.
    /// When set to a non-negative value this overrides the
    /// `interactionDialogSpeedInt` theme constant. The default is -1 which means
    /// "defer to the theme constant" (which itself defaults to 400ms).
    ///
    /// #### Returns
    ///
    /// the animation speed in ms, or -1 if the theme constant is used
    public int getAnimationSpeed() {
        return animationSpeed;
    }

    /// Sets the duration in milliseconds used by the show, dispose and resize
    /// animations, overriding the `interactionDialogSpeedInt` theme constant. Pass
    /// any value &lt; 0 (typically -1) to revert to the theme constant.
    ///
    /// #### Parameters
    ///
    /// - `animationSpeed`: animation duration in ms, or a value &lt; 0 to defer to the theme constant
    public void setAnimationSpeed(int animationSpeed) {
        this.animationSpeed = animationSpeed;
    }

    /// Callback invoked just before the show animation runs to position the dialog
    /// parent at the animation start state. When set, this replaces the default
    /// `#setRepositionAnimation(boolean)` behavior (grow from a 1x1 point at the
    /// center, or stay at full size). Inside the callback, manipulate
    /// `getParent()` bounds (`setX`/`setY`/`setWidth`/`setHeight`) to define
    /// where the dialog should animate from. The animation will then interpolate
    /// the layered pane layout to the dialog's final bounds. Pass `null` (the
    /// default) to use the built-in show animation.
    ///
    /// This callback only fires when `#isAnimateShow()` is true.
    ///
    /// This hook is the recommended workaround when using popup dialogs that
    /// render a pointing-arrow border (`#showPopupDialog(com.codename1.ui.Component)`).
    /// With the built-in "grow from 1x1" animation the dialog is too small for
    /// the arrow image to render until the animation completes; providing a
    /// translate-from-edge setup keeps the dialog at full size for the entire
    /// animation so the arrow is visible throughout. For example, to slide in
    /// from off-screen below:
    ///
    /// ```java
    /// dlg.setShowAnimationSetup(() -> {
    ///     Container parent = dlg.getParent();
    ///     parent.setY(Display.getInstance().getDisplayHeight());
    /// });
    /// ```
    ///
    /// #### Returns
    ///
    /// the show animation setup callback or null
    public Runnable getShowAnimationSetup() {
        return showAnimationSetup;
    }

    /// Sets a callback that positions the dialog parent at the animation start
    /// state, overriding the default show animation. See `#getShowAnimationSetup()`
    /// for details.
    ///
    /// #### Parameters
    ///
    /// - `showAnimationSetup`: callback or null to use the built-in show animation
    public void setShowAnimationSetup(Runnable showAnimationSetup) {
        this.showAnimationSetup = showAnimationSetup;
    }

    /// Callback invoked just before the dispose animation runs to position the
    /// dialog at the animation end state. When set, this replaces the default
    /// `#setRepositionAnimation(boolean)` behavior (shrink to a 1x1 point at the
    /// dialog center). Inside the callback, manipulate the dialog bounds
    /// (`setX`/`setY`/`setWidth`/`setHeight`) to define where the dialog should
    /// animate to. Pass `null` (the default) to use the built-in dispose
    /// animation.
    ///
    /// This callback only fires when `#isAnimateShow()` is true.
    ///
    /// #### Returns
    ///
    /// the dispose animation setup callback or null
    public Runnable getDisposeAnimationSetup() {
        return disposeAnimationSetup;
    }

    /// Sets a callback that positions the dialog at the animation end state,
    /// overriding the default dispose animation. See `#getDisposeAnimationSetup()`
    /// for details.
    ///
    /// #### Parameters
    ///
    /// - `disposeAnimationSetup`: callback or null to use the built-in dispose animation
    public void setDisposeAnimationSetup(Runnable disposeAnimationSetup) {
        this.disposeAnimationSetup = disposeAnimationSetup;
    }

    private void installPointerOutOfBoundsListeners() {

        final TopLevelContainer f = getTopLevelContainer();
        if (f != null) {
            if (pressedListener == null) {
                pressedListener = new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent evt) {
                        if (disposed) {
                            f.asContainer().removePointerPressedListener(pressedListener);
                            f.asContainer().removePointerReleasedListener(releasedListener);
                            return;
                        }
                        pressedOutOfBounds = disposeWhenPointerOutOfBounds &&
                                !getContentPane().containsOrOwns(evt.getX(), evt.getY()) &&
                                !getTitleComponent().containsOrOwns(evt.getX(), evt.getY())
                        ;
                        if (pressedOutOfBounds && disposeWhenPointerOutOfBounds) {
                            evt.consume();
                        }
                    }
                };
            }
            if (releasedListener == null) {
                releasedListener = new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent evt) {
                        if (disposed) {
                            f.asContainer().removePointerPressedListener(pressedListener);
                            f.asContainer().removePointerReleasedListener(releasedListener);
                            return;
                        }
                        if (disposeWhenPointerOutOfBounds &&
                                pressedOutOfBounds &&
                                !getContentPane().containsOrOwns(evt.getX(), evt.getY()) &&
                                !getTitleComponent().containsOrOwns(evt.getX(), evt.getY())) {
                            evt.consume();
                            f.asContainer().removePointerPressedListener(pressedListener);
                            f.asContainer().removePointerReleasedListener(releasedListener);
                            dispose();
                        }
                    }
                };
            }
            f.asContainer().addPointerPressedListener(pressedListener);
            f.asContainer().addPointerReleasedListener(releasedListener);

        }
    }

    /// A popup dialog is shown with the context of a component and  its selection. You should use `#setDisposeWhenPointerOutOfBounds(boolean)` to make it dispose
    /// when the user clicks outside the bounds of the popup. It can optionally provide an arrow in the theme to point at the context component. The popup
    /// dialog has the PopupDialog style by default.
    ///
    /// #### Parameters
    ///
    /// - `c`: the context component which is used to position the dialog and can also be pointed at
    public void showPopupDialog(Component c) {
        showPopupDialog(c, Display.getInstance().isPortrait());
    }

    /// A popup dialog is shown with the context of a component and  its selection. You should use `#setDisposeWhenPointerOutOfBounds(boolean)` to make it dispose
    /// when the user clicks outside the bounds of the popup. It can optionally provide an arrow in the theme to point at the context component. The popup
    /// dialog has the PopupDialog style by default.
    ///
    /// #### Parameters
    ///
    /// - `c`: the context component which is used to position the dialog and can also be pointed at
    ///
    /// - `bias`: directional bias value. This parameter is not supported.
    ///
    /// #### Deprecated
    ///
    /// @deprecated The `bias` parameter is not supported for `InteractionDialog` popups. Use `#showPopupDialog(Component)` instead.
    public void showPopupDialog(Component c, boolean bias) {
        if (c == null) {
            throw new IllegalArgumentException("Component cannot be null");
        }
        TopLevelContainer f = c.getTopLevelContainer(); // PMD Fix: BrokenNullCheck
        if (f != null && !formMode && !f.getContentPane().contains(c)) {
            setFormMode(true);
        }
        // The popup is anchored to c, and the rectangle below is in c's top level's
        // coordinate space, so that top level is the surface it has to appear on --
        // this overrides any host set earlier rather than deferring to it. Without it
        // the delegation below resolved the current form, so a popup requested for a
        // component in a window opened over the main window instead, at coordinates
        // that mean nothing there.
        if (f != null) {
            if (!hostTopLevelInferred) {
                hostTopLevelBeforeInference = hostTopLevel;
            }
            hostTopLevel = f;
            hostTopLevelInferred = true;
        }
        disposed = false;
        getUnselectedStyle().setOpacity(255);
        Rectangle componentPos = c.getSelectedRect();
        componentPos.setX(componentPos.getX() - c.getScrollX());
        componentPos.setY(componentPos.getY() - c.getScrollY());
        setOwner(c);
        showPopupDialog(componentPos);
    }

    /// A popup dialog is shown with the context of a component and  its selection. You should use `#setDisposeWhenPointerOutOfBounds(boolean)` to make it dispose
    /// when the user clicks outside the bounds of the popup.  It can optionally provide an arrow in the theme to point at the context component. The popup
    /// dialog has the PopupDialog style by default.
    ///
    /// #### Parameters
    ///
    /// - `rect`: the screen rectangle to which the popup should point
    public void showPopupDialog(Rectangle rect) {
        showPopupDialogImpl(rect, Display.getInstance().isPortrait());
    }

    /// A popup dialog is shown with the context of a component and  its selection. You should use `#setDisposeWhenPointerOutOfBounds(boolean)` to make it dispose
    /// when the user clicks outside the bounds of the popup.  It can optionally provide an arrow in the theme to point at the context component. The popup
    /// dialog has the PopupDialog style by default.
    ///
    /// #### Parameters
    ///
    /// - `rect`: the screen rectangle to which the popup should point
    ///
    /// - `bias`: directional bias value. This parameter is not supported.
    ///
    /// #### Deprecated
    ///
    /// @deprecated The `bias` parameter is not supported for `InteractionDialog` popups. Use `#showPopupDialog(Rectangle)` instead.
    public void showPopupDialog(Rectangle rect, boolean bias) {
        showPopupDialog(rect);
    }

    private void showPopupDialogImpl(Rectangle rect, boolean bias) {
        inPopupShow = true;
        try {
            showPopupDialogBody(rect, bias);
        } finally {
            inPopupShow = false;
        }
    }

    /// The body of `#showPopupDialogImpl(Rectangle, boolean)`.
    ///
    /// Split out so the popup flag is cleared however the showing ends. An anchored
    /// popup never opens an operating system window: the rectangle it points at is in
    /// its host's coordinate space, and nothing exposes where a window's drawable
    /// begins on the desktop.
    ///
    /// #### Parameters
    ///
    /// - `rect`: the rectangle to point at
    ///
    /// - `bias`: the portrait placement bias
    private void showPopupDialogBody(Rectangle rect, boolean bias) {
        if (rect == null) {
            throw new IllegalArgumentException("rect cannot be null");
        }
        TopLevelContainer f = resolveHost();
        if (f == null) {
            return;
        }
        startPendingTimeout();
        Rectangle origRect = rect;
        rect = new Rectangle(rect);
        rect.setX(rect.getX() - getLayeredPane(f).getAbsoluteX());
        rect.setY(rect.getY() - getLayeredPane(f).getAbsoluteY());
        disposed = false;
        pressedOutOfBounds = false;
        getUnselectedStyle().setOpacity(255);
        if ("Dialog".equals(getUIID())) {
            setUIID("PopupDialog");
            if ("DialogTitle".equals(getTitleComponent().getUIID())) {
                getTitleComponent().setUIID("PopupDialogTitle");
            }
            getContentPane().setUIID("PopupContentPane");
        }

        Label title = getTitleComponent();

        UIManager manager = getUIManager();

        String dialogTitle = title.getText();

        // hide the title if no text is there to allow the styles of the dialog title to disappear, we need this code here since otherwise the
        // preferred size logic of the dialog won't work with large title borders
        if ((dialogTitle == null || dialogTitle.length() == 0) && manager.isThemeConstant("hideEmptyTitleBool", true)) {
            boolean b = getTitle().length() > 0;
            titleArea.setVisible(b);
            getTitleComponent().setVisible(b);
            if (!b && manager.isThemeConstant("shrinkPopupTitleBool", true)) {
                getTitleComponent().setPreferredSize(new Dimension(0, 0));
                getTitleComponent().getStyle().setBorder(null);
                titleArea.setPreferredSize(new Dimension(0, 0));
            }
        }

        // allows a text area to recalculate its preferred size if embedded within a dialog
        revalidate();

        Style contentPaneStyle = getStyle(); // PMD Fix: UnusedLocalVariable removed redundant contentPane reference

        if (manager.isThemeConstant(getUIID() + "ArrowBool", false)) {
            Image t = manager.getThemeImageConstant(getUIID() + "ArrowTopImage");
            Image b = manager.getThemeImageConstant(getUIID() + "ArrowBottomImage");
            Image l = manager.getThemeImageConstant(getUIID() + "ArrowLeftImage");
            Image r = manager.getThemeImageConstant(getUIID() + "ArrowRightImage");
            Border border = contentPaneStyle.getBorder();
            if (border != null) {
                border.setImageBorderSpecialTile(t, b, l, r, rect);
            }
        } else {
            Border border = contentPaneStyle.getBorder();
            if (border != null) {
                border.setTrackComponent(origRect);
            }
        }
        calcPreferredSize();
        int prefHeight = getPreferredH();
        int prefWidth = getPreferredW();
        if (contentPaneStyle.getBorder() != null) {
            prefWidth = Math.max(contentPaneStyle.getBorder().getMinimumWidth(), prefWidth);
            prefHeight = Math.max(contentPaneStyle.getBorder().getMinimumHeight(), prefHeight);
        }


        // Layered-pane parent can be momentarily detached when a previous
        // formMode dialog was disposed (cleanupLayer removes the inner pane
        // from its parent while the form may still be mid-animation, see
        // #5069). Fall back to display dimensions instead of NPE'ing.
        Container layeredParent = getLayeredPane(f).getParent();
        int availableHeight = CN.getDisplayHeight();
        int availableWidth = CN.getDisplayWidth();
        if (layeredParent != null) {
            if (layeredParent.getHeight() != 0) {
                availableHeight = layeredParent.getHeight();
            }
            if (layeredParent.getWidth() != 0) {
                availableWidth = layeredParent.getWidth();
            }
        }
        int width = Math.min(availableWidth, prefWidth);
        setWidth(width);
        setShouldCalcPreferredSize(true);
        revalidate();
        prefHeight = getPreferredH();

        int x = 0;
        int y = 0;

        // A window has no device orientation, so its shape is what decides which
        // placement algorithm applies. Taking Display.isPortrait() there measured the
        // main surface and could open the popup on the wrong side of its anchor. A
        // Form keeps the device orientation it was given.
        boolean showPortrait = f instanceof Window
                ? f.asContainer().getHeight() >= f.asContainer().getWidth()
                : bias;

        // if we don't have enough space then disregard device orientation
        if (showPortrait) {
            if (availableHeight < prefHeight && availableHeight < (availableWidth - rect.getWidth()) / 2) {
                showPortrait = false;
            }
        } else {
            if (availableWidth < prefWidth && availableHeight / 2 > availableWidth - rect.getWidth()) {
                showPortrait = true;
            } else if (prefWidth >= rect.getX()
                    && prefWidth >= availableWidth - rect.getX() - rect.getWidth()) {
                // Landscape placement below picks the side of the rect with
                // room for a side-by-side popup. When the rect spans (or
                // nearly spans) the full available width -- e.g. a Picker in
                // a Y-axis BoxLayout row -- neither side has room and the
                // "popup left" else branch computes width = max(0,
                // rect.getX()) = 0. The dialog then renders zero-width and
                // looks invisible while still consuming the click (#4991).
                // Fall back to portrait-style placement (centered
                // horizontally, popping above or below the rect).
                showPortrait = true;
            }
        }


        if (showPortrait) {
            if (width < availableWidth) {
                int idealX = rect.getX() - width / 2 + rect.getSize().getWidth() / 2;

                // if the ideal position is less than 0 just use 0
                if (idealX > 0) {
                    // if the idealX is too far to the right just align to the right
                    if (idealX + width > availableWidth) {
                        x = availableWidth - width;
                    } else {
                        x = idealX;
                    }
                }
            }
            // Pick the side of the rect (above vs. below) the popup goes
            // on. The original logic chose purely by which half of the
            // screen the rect sat in, which placed the popup ON TOP of
            // the rect whenever it straddled the midline -- the symptom
            // in #5028 (popup covers target) and #5029 (CSSBorder.Arrow
            // can't pick a consistent direction so the tip renders on
            // the wrong edge). Prefer whichever side fits the popup's
            // preferred height, falling back to the larger side. The
            // historical "over the rect" branches are kept as a last
            // resort for the degenerate case where neither side has any
            // room at all.
            int spaceAbove = Math.max(0, rect.getY());
            int spaceBelow = Math.max(0, availableHeight - rect.getY() - rect.getHeight());
            boolean placeBelow;
            if (spaceBelow >= prefHeight) {
                placeBelow = true;
            } else if (spaceAbove >= prefHeight) {
                placeBelow = false;
            } else if (spaceBelow >= spaceAbove) {
                placeBelow = spaceBelow > 0;
            } else {
                placeBelow = false;
            }
            if (placeBelow && spaceBelow > 0) {
                // popup downwards
                y = rect.getY() + rect.getHeight();
                // Grow the dialog by the arrow inset so the content keeps its full
                // preferred height; otherwise the arrow space is taken out of the
                // content pane and the last lines get clipped/scrollable (#5154).
                int arrowInset = padOrientation(contentPaneStyle, TOP, 1);
                int height = Math.min(prefHeight + arrowInset, spaceBelow);
                show(Math.max(0, y), Math.max(0, availableHeight - height - y),
                        Math.max(0, x), Math.max(0, availableWidth - width - x));
                padOrientation(contentPaneStyle, TOP, -1);
            } else if (!placeBelow && spaceAbove > 0) {
                // popup upwards
                int arrowInset = padOrientation(contentPaneStyle, BOTTOM, 1);
                int height = Math.min(prefHeight + arrowInset, spaceAbove);
                y = rect.getY() - height;
                show(y, Math.max(0, availableHeight - rect.getY()), x, Math.max(0, availableWidth - width - x));
                padOrientation(contentPaneStyle, BOTTOM, -1);
            } else if (rect.getY() < availableHeight / 2) {
                // popup over aligned with top of rect, but inset a few
                // mm. Fallback for the truly degenerate case where the
                // rect fills the viewport top-to-bottom.
                y = rect.getY() + CN.convertToPixels(3);

                int arrowInset = padOrientation(contentPaneStyle, BOTTOM, 1);
                int height = Math.min(prefHeight + arrowInset, availableHeight - y);
                show(y, Math.max(0, availableHeight - height - y),
                        Math.max(0, x), Math.max(0, availableWidth - width - x));
                padOrientation(contentPaneStyle, BOTTOM, -1);
            } else {
                // popup over aligned with bottom of rect but inset a few mm
                int arrowInset = padOrientation(contentPaneStyle, TOP, 1);
                int height = prefHeight + arrowInset;
                y = Math.max(0, rect.getY() + rect.getHeight() - CN.convertToPixels(3) - height);
                show(y, Math.max(0, availableHeight - height - y),
                        Math.max(0, x), Math.max(0, availableWidth - width - x));
                padOrientation(contentPaneStyle, TOP, -1);
            }
        } else {
            int height = Math.min(prefHeight, availableHeight);
            if (height < availableHeight) {
                int idealY = rect.getY() - height / 2 + rect.getSize().getHeight() / 2;

                // if the ideal position is less than 0 just use 0
                if (idealY > 0) {
                    // if the idealY is too far up just align to the top
                    if (idealY + height > availableHeight) {
                        y = availableHeight - height;
                    } else {
                        y = idealY;
                    }
                }
            }

            if (prefWidth < availableWidth - rect.getX() - rect.getWidth()) {
                // popup right
                x = rect.getX() + rect.getWidth();


                width = Math.min(prefWidth, availableWidth - x);
                show(y, availableHeight - height - y, Math.max(0, x), Math.max(0, availableWidth - width - x));
            } else if (prefWidth < rect.getX()) {
                x = rect.getX() - prefWidth;
                width = prefWidth;
                show(y, availableHeight - height - y, Math.max(0, x), Math.max(0, availableWidth - width - x));
            } else {
                // popup left
                width = Math.min(prefWidth, availableWidth - (availableWidth - rect.getX()));
                x = rect.getX() - width;
                show(y, availableHeight - height - y, Math.max(0, x), Math.max(0, availableWidth - width - x));
            }
        }
    }


    /// Adjusts the padding of the dialog style on a single edge to reserve room
    /// for the pointing arrow, returning the actual pixel delta that was applied.
    /// Callers add this delta to the dialog height so the dialog grows by the
    /// arrow thickness instead of the arrow space being stolen from the content
    /// pane (see #5154).
    private int padOrientation(Style s, int orientation, int padding) {
        int before = s.getPadding(isRTL(), orientation);
        byte[] b = s.getPaddingUnit();
        byte unit = b == null ? Style.UNIT_TYPE_PIXELS : s.getPaddingUnit()[orientation];
        if (unit != Style.UNIT_TYPE_DIPS) {
            padding = Display.getInstance().convertToPixels(padding);
        }
        s.setPadding(orientation, s.getPaddingFloatValue(isRTL(),
                orientation) + padding);
        return s.getPadding(isRTL(), orientation) - before;
    }

    /// Returns the uiid of the dialog
    ///
    /// #### Returns
    ///
    /// the uiid of the dialog
    public String getDialogUIID() {
        return getContentPane().getUIID();
    }

    /// Simple setter to set the Dialog uiid
    ///
    /// #### Parameters
    ///
    /// - `uiid`: the id for the dialog
    public void setDialogUIID(String uiid) {
        getContentPane().setUIID(uiid);
    }

    /// Simple getter to get the Dialog Style
    ///
    /// #### Returns
    ///
    /// the style of the dialog
    public Style getDialogStyle() {
        return getContentPane().getStyle();
    }

    /// Controls the "grow from center / shrink to center" effect used by the
    /// show/dispose animation. When true (the default), `#show(int, int, int, int)`
    /// collapses the dialog to a 1x1 point at the center of its target bounds and
    /// the layered pane's animation interpolates from that point out to the full
    /// dialog size. `#dispose()` performs the reverse, shrinking the dialog to a
    /// point before removal. When false, the dialog keeps its full size for the
    /// duration of the animation (only the layered pane's layout transition runs,
    /// which is typically not visible for a dialog whose bounds do not change).
    ///
    /// This flag has no effect when `#isAnimateShow()` is false, since the
    /// show/dispose animation is skipped entirely in that case.
    ///
    /// #### Returns
    ///
    /// the repositionAnimation
    public boolean isRepositionAnimation() {
        return repositionAnimation;
    }

    /// Controls the "grow from center / shrink to center" effect used by the
    /// show/dispose animation. When true (the default), `#show(int, int, int, int)`
    /// collapses the dialog to a 1x1 point at the center of its target bounds and
    /// the layered pane's animation interpolates from that point out to the full
    /// dialog size. `#dispose()` performs the reverse, shrinking the dialog to a
    /// point before removal. When false, the dialog keeps its full size for the
    /// duration of the animation (only the layered pane's layout transition runs,
    /// which is typically not visible for a dialog whose bounds do not change).
    ///
    /// This flag has no effect when `#isAnimateShow()` is false, since the
    /// show/dispose animation is skipped entirely in that case.
    ///
    /// #### Parameters
    ///
    /// - `repositionAnimation`: the repositionAnimation to set
    public void setRepositionAnimation(boolean repositionAnimation) {
        this.repositionAnimation = repositionAnimation;
    }

    /// Selects which layered pane hosts the dialog.
    ///
    /// When false (the default), the dialog is added to `Form#getLayeredPane()`,
    /// which sits above the form's content pane but below the title area, side
    /// menu, and status bar. This is the right choice for most dialogs that
    /// interact with content pane components, and is the historical behavior of
    /// `InteractionDialog`.
    ///
    /// When true, the dialog is added to `Form#getFormLayeredPane(Class, boolean)`,
    /// which sits above the entire form including the title area and side menu.
    /// Use this when the dialog needs to overlay or point at a component that
    /// lives outside the content pane (for example a title bar button or an item
    /// in the side menu). `#showPopupDialog(Component)` enables this
    /// automatically when it detects that the target component is not inside the
    /// form's content pane.
    ///
    /// In short, leave this at the default unless you observe the dialog being
    /// clipped by the title/side menu or you are pointing at a component outside
    /// the content pane.
    ///
    /// #### Returns
    ///
    /// the formMode
    public boolean isFormMode() {
        return formMode;
    }

    /// Selects which layered pane hosts the dialog.
    ///
    /// When false (the default), the dialog is added to `Form#getLayeredPane()`,
    /// which sits above the form's content pane but below the title area, side
    /// menu, and status bar. This is the right choice for most dialogs that
    /// interact with content pane components, and is the historical behavior of
    /// `InteractionDialog`.
    ///
    /// When true, the dialog is added to `Form#getFormLayeredPane(Class, boolean)`,
    /// which sits above the entire form including the title area and side menu.
    /// Use this when the dialog needs to overlay or point at a component that
    /// lives outside the content pane (for example a title bar button or an item
    /// in the side menu). `#showPopupDialog(Component)` enables this
    /// automatically when it detects that the target component is not inside the
    /// form's content pane.
    ///
    /// In short, leave this at the default unless you observe the dialog being
    /// clipped by the title/side menu or you are pointing at a component outside
    /// the content pane.
    ///
    /// #### Parameters
    ///
    /// - `formMode`: the formMode to set
    public void setFormMode(boolean formMode) {
        this.formMode = formMode;
    }

    /// Whether `InteractionDialog` is in the global "stackable" mode. See
    /// `#setStackable(boolean)`.
    ///
    /// #### Returns
    ///
    /// true if stackable mode is enabled
    public static boolean isStackable() {
        return stackable;
    }

    /// Opt-in robustness mode for applications that show several
    /// `InteractionDialog`s at the same time (for example a step-by-step
    /// walkthrough that highlights different components).
    ///
    /// All `InteractionDialog` instances share a single layered pane keyed by
    /// the class. In the default (historical) behavior, disposing one dialog
    /// clears that whole layer (`removeAll()` / layer removal), which also wipes
    /// any sibling dialog still showing in it -- so when dialogs overlap one of
    /// them can silently fail to appear (#5193). When stackable mode is enabled
    /// dispose removes only the disposed dialog's own component; remaining
    /// dialogs stay visible, layered by the order in which `#show(int, int, int, int)`
    /// was called (later shows render on top). The shared layer container is
    /// removed only once it becomes empty, so layers do not accumulate as
    /// dialogs come and go.
    ///
    /// This is a global, app-wide setting (it must be on for every dialog that
    /// participates) and defaults to false to preserve backwards compatibility.
    ///
    /// #### Parameters
    ///
    /// - `stackable`: true to enable stackable/concurrent dialog support
    public static void setStackable(boolean stackable) {
        InteractionDialog.stackable = stackable;
    }

    /// {@inheritDoc}
    @Override
    public void setDialogType(int dialogType) {
        // no-op for InteractionDialog. Dialog sounds are specific to Dialog/Form internals.
    }

    /// No-op for `InteractionDialog`. Transitions are not supported; the show
    /// and dispose animations are governed by `#setAnimateShow(boolean)` and
    /// `#setRepositionAnimation(boolean)` and run on the host layered pane
    /// rather than as a Form-level transition. The method is provided only to
    /// satisfy the `AbstractDialog` contract.
    @Override
    public void setTransitions(Transition transition) {
    }

    /// {@inheritDoc}
    @Override
    public void configureCommands(Command[] cmds, boolean commandsAsButtons) {
        if (cmds == null || cmds.length == 0) {
            return;
        }
        UIManager manager = UIManager.getInstance();
        Container buttonArea;
        boolean commandGrid = manager.isThemeConstant("dlgCommandGridBool", false);
        if (commandGrid) {
            buttonArea = new Container(new GridLayout(1, cmds.length));
        } else {
            buttonArea = new Container(new FlowLayout(CENTER));
        }
        buttonArea.setUIID("DialogCommandArea");
        if (commandGrid) {
            // Native command grids are dialog chrome, not padded body content.
            // Preserve the theme's top spacing while extending the grid and
            // separator to the other three card edges.
            getAllStyles().setPadding(0, 0, 0, 0);
            Style commandAreaStyle = buttonArea.getAllStyles();
            commandAreaStyle.setPadding(LEFT, 0);
            commandAreaStyle.setPadding(RIGHT, 0);
            commandAreaStyle.setPadding(BOTTOM, 0);
        }
        String uiid = manager.getThemeConstant("dlgButtonCommandUIID", null);
        String lineColor = manager.getThemeConstant(
                Boolean.TRUE.equals(Display.getInstance().isDarkMode())
                        ? "dlgInvisibleButtonsDark" : "dlgInvisibleButtons",
                manager.getThemeConstant("dlgInvisibleButtons", null));
        if (cmds.length > 3) {
            lineColor = null;
        }
        int largest = Integer.parseInt(manager.getThemeConstant("dlgCommandButtonSizeInt", "0"));
        for (int iter = 0; iter < cmds.length; iter++) {
            final Command command = cmds[iter];
            Button b = new Button(command);
            if (uiid != null) {
                b.setUIID(uiid);
            }
            if (Button.isCapsTextDefault()) {
                b.setCapsText(true);
            }
            largest = Math.max(b.getPreferredW(), largest);
            if (lineColor != null && lineColor.length() > 0) {
                int color = Integer.parseInt(lineColor, 16);
                Border border;
                if (iter < cmds.length - 1) {
                    border = Border.createCompoundBorder(Border.createLineBorder(1, color), null,
                            null, Border.createLineBorder(1, color));
                } else {
                    border = Border.createCompoundBorder(Border.createLineBorder(1, color), null,
                            null, null);
                }
                b.getUnselectedStyle().setBorder(border);
                b.getSelectedStyle().setBorder(border);
                b.getPressedStyle().setBorder(border);
            }
            b.addActionListener(new ActionListener<ActionEvent>() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    lastCommandPressed = command;
                    dispose();
                }
            });
            buttonArea.addComponent(b);
        }
        for (int iter = 0; iter < cmds.length; iter++) {
            buttonArea.getComponentAt(iter).setPreferredW(largest);
        }
        buttonArea.getComponentAt(0).requestFocus();
        // Commands are dialog chrome, not body content. Keeping this in the
        // outer SOUTH slot lets the command grid span the card edge-to-edge
        // regardless of DialogContentPane padding.
        super.addComponent(BorderLayout.SOUTH, buttonArea);
    }

    /// {@inheritDoc}
    @Override
    public void setDefaultCommand(Command defaultCommand) {
    }

    /// {@inheritDoc}
    @Override
    public void setTimeout(long timeout) {
        if (timeout <= 0) {
            pendingTimeout = 0;
            return;
        }
        // Recorded and started when the dialog is shown, not here. A timeout set before
        // showing has no host to bind to yet: resolveHost() answers the current form,
        // which is the wrong one for a popup that later resolves to a window -- and if
        // that form is replaced its animations stop, so the dialog never times out. In
        // an application with no form at all it answers null, which threw.
        pendingTimeout = timeout;
        if (isShowing()) {
            startPendingTimeout();
        }
    }

    /// Binds the pending timeout to the host the dialog is actually on.
    private void startPendingTimeout() {
        // The window whenever there is one. setTimeout() can be called after a modeless
        // dialog is already up and reaches this, and an explicit setTopLevelHost() wins
        // in resolveHost() -- so the timer went onto the owner form, which stops being
        // animated as soon as navigation replaces it, and the dialog never closed.
        startPendingTimeout(nativeWindow != null ? nativeWindow : resolveHost());
    }

    /// Starts the pending timeout against a named surface.
    ///
    /// The surface decides whether there is a timeout at all -- there is nothing to
    /// close while the dialog is not up -- but it does not drive the clock. That
    /// distinction is the fix: a timer ticked by the surface it is registered on stops
    /// whenever that surface stops being painted, and a modal `showDialog()` whose
    /// timeout is the thing that releases the caller then blocks for as long as the
    /// window stays minimized, which for a window never restored is for good.
    ///
    /// #### Parameters
    ///
    /// - `host`: the surface to register the timeout on, may be null
    private void startPendingTimeout(TopLevelContainer host) {
        if (pendingTimeout <= 0) {
            return;
        }
        if (host == null) {
            return;
        }
        int millis = (int) pendingTimeout;
        pendingTimeout = 0;
        // Which showing armed this clock. A UITimer dies with the surface it is
        // registered on, so it could not outlive the dialog; this one can, and without
        // the token a dialog disposed and then shown again inside the original timeout
        // would be closed by the previous showing's timer.
        final int token = ++timeoutGeneration;
        // Not tied to the surface. A UITimer is driven by the painting of the top level
        // it is registered on, so a window that is minimized or hidden stops the clock
        // -- and a modal dialog whose timeout is what releases the caller then holds it
        // for as long as the window stays down, which for a window never restored is
        // for good. The host still decides whether there is a timeout at all; it just
        // does not have to be painted for it to arrive.
        Display.getInstance().setTimeout(millis, new Runnable() {
            @Override
            public void run() {
                if (token == timeoutGeneration) {
                    dispose();
                }
            }
        });
    }

    /// Identifies the showing a timeout was armed under. Bumped by every arming and by
    /// every path that ends a showing, so a clock that outlives the showing it belongs
    /// to is discarded rather than closing whatever is on screen by then.
    private int timeoutGeneration;

    /// Retires whatever timeout is currently armed.
    ///
    /// Every path that ends a showing has to call this, not just `dispose()`: the clock
    /// is deliberately not tied to the surface, so it survives the window being torn
    /// down. Bumping twice is harmless -- the counter only ever has to stop matching.
    private void retireArmedTimeout() {
        timeoutGeneration++;
    }

    /// Shows this interaction dialog and blocks until it is disposed.
    @Override
    public Command showDialog() {
        // The host's dimensions, not the display's. These margins centre the dialog,
        // and show() below places it on the host -- so measuring the main surface
        // centred it in the wrong coordinate space, and on a window smaller than the
        // display the margins could exceed the host outright and leave the dialog
        // clipped or off screen.
        if (usesNativeWindow()) {
            // Real window modality rather than the polling loop below: the framework
            // blocks input for it and the caller is parked properly instead of waking
            // every ten milliseconds to ask again.
            finishShowingInOtherMode(true);
            disposed = false;
            getUnselectedStyle().setOpacity(255);
            showInNativeWindow(true);
            return lastCommandPressed;
        }
        TopLevelContainer host = resolveHost();
        int width = host == null
                ? Display.getInstance().getDisplayWidth() : host.asContainer().getWidth();
        int height = host == null
                ? Display.getInstance().getDisplayHeight() : host.asContainer().getHeight();
        revalidate();
        int prefWidth = Math.min(width, getPreferredW());
        int prefHeight = Math.min(height, getPreferredH());
        int leftRight = Math.max(0, (width - prefWidth) / 2);
        int topBottom = Math.max(0, (height - prefHeight) / 2);
        show(topBottom, topBottom, leftRight, leftRight);
        while (isShowing()) {
            CN.invokeAndBlock(BLOCKING_SLEEP);
        }
        return lastCommandPressed;
    }
}
