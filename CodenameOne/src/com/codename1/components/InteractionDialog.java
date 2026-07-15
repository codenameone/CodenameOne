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
import com.codename1.ui.Dialog;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
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
import com.codename1.ui.util.UITimer;

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

    private void cleanupLayer(Form f) {
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

    private Container getLayeredPane(Form f) {
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
            Form f = getComponentForm();
            if (f != null) {
                if (pressedListener != null) {
                    f.removePointerPressedListener(pressedListener);
                }
                if (releasedListener != null) {
                    f.removePointerReleasedListener(releasedListener);
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
            final Form f = Display.getInstance().getCurrent();

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
        disposed = false;
        Form f = Display.getInstance().getCurrent();
        shownInFormMode = formMode;
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
                int x = left + (f.getWidth() - right - left) / 2;
                int y = top + (f.getHeight() - bottom - top) / 2;
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
            f.revalidateWithAnimationSafety();
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
        Container p = getParent();
        if (p != null) {
            Form f = p.getComponentForm();
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
                f.revalidateWithAnimationSafety();
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
        final Container p = getParent();
        if (p != null) {
            final Form f = p.getComponentForm();
            if (f != null) {
                switch (direction) {
                    case Component.LEFT:
                        setX(-getWidth());
                        break;
                    case Component.TOP:
                        setY(-getHeight());
                        break;
                    case Component.RIGHT:
                        setX(Display.getInstance().getDisplayWidth());
                        break;
                    case Component.BOTTOM:
                        setY(Display.getInstance().getDisplayHeight());
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

        final Form f = getComponentForm();
        if (f != null) {
            if (pressedListener == null) {
                pressedListener = new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent evt) {
                        if (disposed) {
                            f.removePointerPressedListener(pressedListener);
                            f.removePointerReleasedListener(releasedListener);
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
                            f.removePointerPressedListener(pressedListener);
                            f.removePointerReleasedListener(releasedListener);
                            return;
                        }
                        if (disposeWhenPointerOutOfBounds &&
                                pressedOutOfBounds &&
                                !getContentPane().containsOrOwns(evt.getX(), evt.getY()) &&
                                !getTitleComponent().containsOrOwns(evt.getX(), evt.getY())) {
                            evt.consume();
                            f.removePointerPressedListener(pressedListener);
                            f.removePointerReleasedListener(releasedListener);
                            dispose();
                        }
                    }
                };
            }
            f.addPointerPressedListener(pressedListener);
            f.addPointerReleasedListener(releasedListener);

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
        Form f = c.getComponentForm(); // PMD Fix: BrokenNullCheck
        if (f != null && !formMode && !f.getContentPane().contains(c)) {
            setFormMode(true);
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
        if (rect == null) {
            throw new IllegalArgumentException("rect cannot be null");
        }
        Form f = Display.getInstance().getCurrent();
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

        boolean showPortrait = bias;

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
            return;
        }
        UITimer.timer((int) timeout, false, Display.getInstance().getCurrent(), new Runnable() {
            @Override
            public void run() {
                dispose();
            }
        });
    }

    /// Shows this interaction dialog and blocks until it is disposed.
    @Override
    public Command showDialog() {
        int width = Display.getInstance().getDisplayWidth();
        int height = Display.getInstance().getDisplayHeight();
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
