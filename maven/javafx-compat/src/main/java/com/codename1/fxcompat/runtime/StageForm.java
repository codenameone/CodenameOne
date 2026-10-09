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
package com.codename1.fxcompat.runtime;

import com.codename1.ui.Command;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.layouts.BorderLayout;

import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;

/// A stage shown as a Codename One form.
///
/// The scene fills the content pane. The primary stage has no title
/// area: its scene is the whole screen, as it is the whole window on a
/// desktop. A further stage shown as a form has a title and a back
/// command; back asks the stage to close, which returns to the form that
/// was showing before it.
///
/// Pointer and key events enter the scene here before Codename One sees
/// them; see [SceneInput].
public final class StageForm extends Form implements StageHost {

    private final HostCore core;
    private final boolean primary;
    private Form previous;
    /// The display size the last whole paint of this form was made for.
    private int paintedWidth = -1;
    private int paintedHeight = -1;
    private int paintedAgain;
    /// The size the application gave the stage before showing it, or NaN.
    private final double askedWidth;
    private final double askedHeight;
    private final Runnable again = new Runnable() {
        @Override
        public void run() {
            repaint();
        }
    };

    /// Creates the form of a window.
    public StageForm(Window window, boolean primary) {
        super(new BorderLayout());
        this.primary = primary;
        // Read before the scene is attached: from then on the window
        // reports the size the form gave it, not the one it was asked for.
        this.askedWidth = window.getWidth();
        this.askedHeight = window.getHeight();
        this.core = new HostCore(window, getContentPane(), this);
        if (primary) {
            if (getToolbar() != null) {
                getToolbar().hideToolbar();
            }
            Container titleArea = getTitleArea();
            if (titleArea != null) {
                titleArea.setHidden(true);
            }
        } else {
            setBackCommand(new Command("Back") {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    core.window().cn1CloseRequested();
                }
            });
        }
        stageChanged();
    }

    /// Returns the window this form shows.
    public Window window() {
        return core.window();
    }

    @Override
    public void pointerPressed(int x, int y) {
        if (!SceneInput.pressed(core.scene(), core.rootPeer(), x, y)) {
            super.pointerPressed(x, y);
        }
    }

    @Override
    public void pointerDragged(int x, int y) {
        if (!SceneInput.dragged(core.scene(), core.rootPeer(), x, y)) {
            super.pointerDragged(x, y);
        }
    }

    /// The drag of a pointer as the display delivers it. A form has two
    /// overloads and the display calls this one, which is an
    /// implementation of its own and never reaches the one above: with
    /// only that one overridden no drag got to the scene on a device,
    /// while a press and a release, which a form routes through the plain
    /// overloads, did. More than one pointer is a gesture of Codename
    /// One's.
    @Override
    public void pointerDragged(int[] x, int[] y) {
        if (x != null && y != null && x.length == 1 && y.length == 1
                && SceneInput.dragged(core.scene(), core.rootPeer(), x[0], y[0])) {
            return;
        }
        super.pointerDragged(x, y);
    }

    @Override
    public void pointerReleased(int x, int y) {
        if (!SceneInput.released(core.scene(), core.rootPeer(), x, y)) {
            super.pointerReleased(x, y);
        }
    }

    @Override
    public void pointerHover(int[] x, int[] y) {
        if (x == null || y == null || x.length == 0 || y.length == 0
                || !SceneInput.hover(core.scene(), core.rootPeer(), x[0], y[0])) {
            super.pointerHover(x, y);
        }
    }

    @Override
    public void longPointerPress(int x, int y) {
        if (!SceneInput.longPress(core.scene(), core.rootPeer(), x, y)) {
            super.longPointerPress(x, y);
        }
    }

    @Override
    public void keyPressed(int keyCode) {
        if (!SceneInput.keyPressed(core.scene(), keyCode)) {
            super.keyPressed(keyCode);
        }
    }

    @Override
    public void keyReleased(int keyCode) {
        if (!SceneInput.keyReleased(core.scene(), keyCode)) {
            super.keyReleased(keyCode);
        }
    }

    @Override
    public void sceneChanged() {
        core.sceneChanged();
    }

    @Override
    public void rootChanged() {
        core.sceneChanged();
        repaint();
    }

    @Override
    public void requestPulse() {
        core.requestPulse();
    }

    @Override
    public void block() {
        core.block();
    }

    @Override
    public void showCursor(int cursor) {
        setEnableCursors(true);
        core.showCursor(cursor);
    }

    @Override
    public final void stageChanged() {
        Window window = core.window();
        if (!primary && window instanceof Stage) {
            String title = ((Stage) window).getTitle();
            setTitle(title == null ? "" : title);
        }
        nativeTitle();
    }

    /// Hands the title of the stage to the window of the operating
    /// system, on a port that keeps the title of a form there.
    ///
    /// The primary stage has no title area, so its title had nowhere to
    /// go and the window kept the port's own name. Its form is given the
    /// title only where the port shows it on the window, never where it
    /// would draw a title strip; and since Codename One pushes a title
    /// that is set on the form showing, and not the title a form already
    /// has when it is shown, the push is asked for here.
    private void nativeTitle() {
        if (!Display.isInitialized()) {
            return;
        }
        Display d = Display.getInstance();
        if (!d.isNativeTitle()) {
            return;
        }
        Window window = core.window();
        if (primary && window instanceof Stage) {
            String title = ((Stage) window).getTitle();
            String wanted = title == null ? "" : title;
            if (!wanted.equals(getTitle())) {
                setTitle(wanted);
            }
        }
        if (d.getCurrent() == this) {
            d.refreshNativeTitle();
        }
    }

    @Override
    public void boundsRequested() {
    }

    /// Gives the application's own window the size its primary stage
    /// asks for: the stage's, or else the size its scene was created
    /// with or prefers, as a stage with a window of its own is sized.
    /// A stage shown over a form that is not a stage's is a guest in
    /// that window and leaves its size alone.
    private void sizeWindow() {
        Display d = Display.getInstance();
        boolean own = previous == null || previous instanceof StageForm;
        if (!primary || !own || !d.isDesktop()) {
            return;
        }
        Scene scene = core.scene();
        double width = askedWidth;
        double height = askedHeight;
        if (Double.isNaN(width) && scene != null) {
            width = scene.cn1InitialWidth() >= 0 ? scene.cn1InitialWidth()
                    : (scene.getRoot() == null ? 0 : scene.getRoot().prefWidth(-1));
        }
        if (Double.isNaN(height) && scene != null) {
            height = scene.cn1InitialHeight() >= 0 ? scene.cn1InitialHeight()
                    : (scene.getRoot() == null ? 0 : scene.getRoot().prefHeight(-1));
        }
        if (width > 0 && height > 0) {
            d.setWindowSize(Units.sizeToPixels(width), Units.sizeToPixels(height));
        }
    }

    /// Paints the form, and asks for one more paint when the display has
    /// a size that no paint of this form was made for yet.
    ///
    /// A port that draws into a buffer of the window's size may replace
    /// the buffer when the frame ends: the native Linux port applies a
    /// resize it has recorded right after the flush, and shows a buffer
    /// nothing has drawn on, until an input event makes something paint.
    /// The frame after that flush is drawn on the new buffer. It costs
    /// one paint for each size the display takes.
    @Override
    public void paint(com.codename1.ui.Graphics g) {
        super.paint(g);
        if (!Display.isInitialized()) {
            return;
        }
        Display d = Display.getInstance();
        int w = d.getDisplayWidth();
        int h = d.getDisplayHeight();
        if (w != paintedWidth || h != paintedHeight) {
            paintedWidth = w;
            paintedHeight = h;
            paintedAgain++;
            d.callSerially(again);
        }
    }

    /// How many times a paint asked for another one; see [#paint].
    public int cn1PaintedAgain() {
        return paintedAgain;
    }

    @Override
    public void layoutContainer() {
        super.layoutContainer();
        // Form's constructor lays itself out when the theme installs a global
        // toolbar, which is before this class's own fields exist.
        if (core != null) {
            core.sized();
        }
    }

    @Override
    public void open() {
        if (Display.isInitialized()) {
            Form current = Display.getInstance().getCurrent();
            previous = current == this ? null : current;
            if (current != this) {
                sizeWindow();
            }
            show();
            nativeTitle();
        }
        core.sized();
    }

    @Override
    public void close() {
        core.markClosed();
        if (!primary && previous != null && Display.isInitialized() && Display.getInstance().getCurrent() == this) {
            previous.showBack();
        }
        previous = null;
    }

    @Override
    public void toFront() {
        if (Display.isInitialized() && Display.getInstance().getCurrent() != this) {
            show();
        }
    }
}
