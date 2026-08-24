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
package com.codename1.ui;

import com.codename1.impl.WindowManager;
import com.codename1.io.Log;
import com.codename1.ui.animations.Animation;
import com.codename1.ui.animations.Transition;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.PointerEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.WindowEvent;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.EventDispatcher;

import java.util.ArrayList;
import java.util.HashMap;

/// A separate native operating system window, with its own Codename One component
/// hierarchy inside it.
///
/// A `Window` is the desktop counterpart of `Form`. The application's main surface
/// stays a `Form` and keeps behaving exactly as it always has; a `Window` is an
/// *additional* top level, rendered into its own native window, with its own focus
/// owner, its own animations and its own dirty region.
///
/// ```java
/// if (Desktop.isSupported()) {
///     Window w = new Window("Inspector", new BorderLayout());
///     w.add(BorderLayout.CENTER, new Label("Hello from a second window"));
///     w.setWindowSize(400, 300);
///     w.show();
/// }
/// ```
///
/// A `Window` is not a `Form`, so `Component#getComponentForm()` returns null for the
/// components inside one. Code that has to work in both places should ask for
/// `Component#getTopLevelContainer()` instead.
///
/// Windows exist only where the platform has a windowing system. Every constructor
/// throws `UnsupportedOperationException` when it does not, so guard with
/// `Desktop#isSupported()`. There is deliberately no silent fallback to showing a
/// `Form`: a window that quietly is not a window produces layout and lifecycle bugs
/// that are far harder to find than an exception on the first line.
///
/// @author Shai Almog
public class Window extends Container implements TopLevelContainer {

    /// Closing the window disposes it and releases the native window. The default.
    public static final int DISPOSE_ON_CLOSE = 0;

    /// Closing the window hides it, leaving it able to be shown again.
    public static final int HIDE_ON_CLOSE = 1;

    /// Closing the window does nothing, leaving the application to call
    /// `#dispose()` itself from a close listener.
    public static final int DO_NOTHING_ON_CLOSE = 2;

    /// The window does not block input to any other window.
    public static final int MODALITY_NONE = 0;

    /// The window blocks input to the window that owns it.
    public static final int MODALITY_WINDOW = 1;

    /// The window blocks input to every other window and to the main form.
    public static final int MODALITY_APPLICATION = 2;

    private final int windowId;
    private Object nativePeer;
    private com.codename1.impl.PaintSurface paintSurface;

    /// This window's drag-activation filter. Owned here rather than in a table keyed
    /// by window id on the implementation, so it dies with the window and nothing
    /// caps how many windows can be dragged at once.
    private final com.codename1.impl.PointerDragActivation dragActivation =
            new com.codename1.impl.PointerDragActivation();
    private Graphics windowGraphics;
    /// Set as soon as dispose() begins, so re-entering it is a no-op.
    private boolean disposing;
    /// True while internalPaintImpl is running, so paint() does not repeat the
    /// background it has already drawn. Mirrors the guard Form carries.
    private boolean inInternalPaint;
    /// Published under Display.lock once teardown is complete; showModal waits on it,
    /// and isWindowDisposed() reads it under the same monitor rather than relying on
    /// volatile, so the write is visible to a parked caller on any thread.
    private boolean disposed;
    private boolean nativeVisible;
    /// Set while this window holds a modal blocker, so it is pushed and popped once.
    private boolean modalRegistered;
    /// Set while the platform has the window minimized, which is not the same as
    /// hidden: it is still open, and still modal if it was.
    private boolean iconified;

    /// Mirrors Form's flag of the same name, read from the same property.
    private final boolean revalidateFromRoot =
            "true".equals(CN.getProperty("Form.revalidateFromRoot", "true"));

    /// Held while a caller owns the right to start an animation; see
    /// `#grabAnimationLock()`.
    private boolean animationLock;

    /// Vibration length for a tactile touch, in milliseconds; -1 until read from the
    /// look and feel, the same value `Form` uses so a window feels like the rest of
    /// the application.
    private int tactileTouchDuration = -1;

    private final Container contentPane;
    private Container layeredPane;
    private Container windowLayeredPane;
    private Painter glassPane;

    private ArrayList<Component> componentsAwaitingRelease;
    private Component focused;
    private Component dragged;
    private Component pressedCmp;
    private Object currentPointerPress;
    private int initialPressX;
    private int initialPressY;
    private boolean cyclicFocus = true;

    /// This window's own gesture state.
    ///
    /// Fields, not entries in a table keyed by window id. The table version had to
    /// lease a slot on the first event and hand it back on the last, and every way of
    /// getting that wrong is a real defect: a window disposed mid-press never returned
    /// its slot, and once the fixed number of slots was gone the drag filter silently
    /// switched off for every window. Held here, the state is created with the window
    /// and collected with it, and none of those failures can be expressed.
    private PointerDragHistory dragHistory;
    private boolean dragOccured;
    /// A contact is down in this window and has not been released or dragged yet, and
    /// where it went down. In this window's coordinates, which is why it cannot be a
    /// global: the coordinates of two windows are not comparable.
    private boolean selectionPressed;
    private int selectionPressedX;
    private int selectionPressedY;

    private final AnimationManager animMananger = new AnimationManager(this);
    private final ArrayList<Animation> animatableComponents = new ArrayList<Animation>();
    private final ArrayList<Animation> internalAnimatableComponents = new ArrayList<Animation>();
    private final ArrayList<Container> revalidateQueue = new ArrayList<Container>();
    private final ArrayList<Container> pendingRevalidateQueue = new ArrayList<Container>();

    private UIManager uiManager;
    private VirtualInputDevice currentInputDevice;
    private final TextSelection textSelection = new TextSelection(this);
    private boolean enableCursors;

    private HashMap<Integer, ArrayList<ActionListener>> keyListeners;
    private final ArrayList<Command> commands = new ArrayList<Command>();
    private final EventDispatcher commandListeners = new EventDispatcher();
    private final EventDispatcher showListeners = new EventDispatcher();
    private final EventDispatcher sizeChangedListeners = new EventDispatcher();
    private final EventDispatcher closeListeners = new EventDispatcher();
    private final EventDispatcher windowListeners = new EventDispatcher();

    private String pendingTitle = "";
    private int pendingX;
    private int pendingY;
    /// Whether the application chose a position. A negative coordinate is a perfectly
    /// ordinary one -- a monitor to the left of or above the primary display has a
    /// negative origin -- so it cannot double as "no position was asked for", or a
    /// window restored onto such a monitor would be centred on the primary one instead.
    private boolean pendingPositionSet;
    private int pendingWidth = 400;
    private int pendingHeight = 300;
    private boolean decorated = true;
    private boolean resizable = true;
    private boolean alwaysOnTop;
    private boolean utilityWindow;
    private Image windowIcon;
    private Dimension minimumWindowSize;
    private int closeOperation = DISPOSE_ON_CLOSE;
    private int modalityType = MODALITY_NONE;
    private TopLevelContainer ownerWindow;
    private Monitor currentMonitor;

    /// Creates a window whose content is laid out with a `FlowLayout`.
    public Window() {
        this(null, new FlowLayout());
    }

    /// Creates a window with the given content layout.
    ///
    /// #### Parameters
    ///
    /// - `contentPaneLayout`: the layout for the content pane
    public Window(Layout contentPaneLayout) {
        this(null, contentPaneLayout);
    }

    /// Creates a window with the given title, laid out with a `FlowLayout`.
    ///
    /// #### Parameters
    ///
    /// - `title`: the window title
    public Window(String title) {
        this(title, new FlowLayout());
    }

    /// Creates a window with the given title and content layout.
    ///
    /// #### Parameters
    ///
    /// - `title`: the window title
    ///
    /// - `contentPaneLayout`: the layout for the content pane
    public Window(String title, Layout contentPaneLayout) {
        super(new BorderLayout());
        // Fail here rather than at show(): a developer who guessed wrong about the
        // platform finds out on the line that constructed the window.
        TopLevelSupport.requireMultiWindow();
        windowId = Desktop.getInstance().nextWindowId();
        setSafeAreaRoot(true);
        // A window is a top level surface, so it takes the styles a theme already
        // defines for one. Naming these "Window" and "WindowContentPane" instead
        // would leave every theme written before desktop windows existed with no
        // entry for them, and an unstyled top level paints nothing at all -- the
        // window would come up as an unpainted rectangle. A theme or an application
        // that wants windows to look different from forms sets its own UIID.
        setUIID("Form");
        setVisible(false);
        contentPane = new Container(contentPaneLayout);
        contentPane.setUIID("ContentPane");
        // The same default a Form's content pane gets. Without it, content taller than
        // the window is simply clipped and unreachable, and identical content moved
        // from a Form to a Window silently stopped scrolling unless the application
        // knew to opt in. A BorderLayout content pane ignores this, as it does on a
        // Form -- setScrollableY forces false for one.
        contentPane.setScrollableY(true);
        // No title area and no toolbar. A window's title is its native chrome, drawn
        // by the platform, so a second one inside the content would be a mobile idiom
        // in a desktop window -- and it would eat content space to duplicate what the
        // title bar already says.
        super.addComponent(BorderLayout.CENTER, contentPane);
        if (title != null) {
            setTitle(title);
        }
        setWidth(pendingWidth);
        setHeight(pendingHeight);
        // Hardcoded for the same reason Form hardcodes it: a top level surface has
        // nothing behind it, so a translucent one shows whatever the raster happened
        // to contain.
        getStyle().setBgTransparency(0xFF);
    }

    // ---- identity -------------------------------------------------------------

    /// Returns the framework assigned id of this window.
    ///
    /// This is the id a port stores at creation and echoes back on every event, so it
    /// is also how a window is looked up from
    /// `Desktop#windowById(int)`.
    ///
    /// #### Returns
    ///
    /// the window id
    public int getWindowId() {
        return windowId;
    }

    Object getNativePeer() {
        return nativePeer;
    }

    com.codename1.impl.PointerDragActivation getDragActivation() {
        return dragActivation;
    }

    com.codename1.impl.PaintSurface getPaintSurface() {
        return paintSurface;
    }

    /// Drops everything queued on this window's surface, if it has one yet. A window
    /// that was never shown has no surface, and callers below can be reached in that
    /// state -- hide() most obviously. The handle-based API this replaced tolerated a
    /// null surface, and dropping that tolerance would turn those into a failure.
    private void clearPaintSurface() {
        if (paintSurface != null) {
            paintSurface.clear();
        }
    }

    Graphics getWindowGraphics() {
        return windowGraphics;
    }

    private static WindowManager manager() {
        return Display.impl.getWindowManager();
    }

    private void requireLive() {
        if (disposing) {
            throw new IllegalStateException("This Window has been disposed");
        }
    }

    // ---- TopLevelContainer ------------------------------------------------------

    /// {@inheritDoc}
    @Override
    public Container asContainer() {
        return this;
    }

    /// {@inheritDoc}
    @Override
    public TopLevelContainer getTopLevelContainer() {
        if (getParent() != null) {
            return super.getTopLevelContainer();
        }
        return this;
    }

    /// {@inheritDoc}
    @Override
    public Container getContentPane() {
        return contentPane;
    }

    /// {@inheritDoc}
    @Override
    public Container getLayeredPane() {
        return getLayeredPane(null, false);
    }

    /// {@inheritDoc}
    @Override
    public Container getLayeredPane(Class c, boolean top) {
        return TopLevelSupport.layeredPane(getLayeredPaneImpl(), c, top);
    }

    /// {@inheritDoc}
    @Override
    public Container getLayeredPane(Class c, int zIndex) {
        return TopLevelSupport.layeredPane(getLayeredPaneImpl(), c, zIndex);
    }

    private Container getLayeredPaneImpl() {
        if (layeredPane == null) {
            layeredPane = new Container(new LayeredLayout());
            Container parent = contentPane.wrapInLayeredPane();
            layeredPane.add(new Container());
            parent.addComponent(layeredPane);
            revalidateWithAnimationSafety();
        }
        return layeredPane;
    }

    /// {@inheritDoc}
    ///
    /// The name mirrors `Form#getFormLayeredPane(java.lang.Class, boolean)` on
    /// purpose: `Sheet`, `InteractionDialog` and `ToastBar` attach through this
    /// method, and renaming it for windows would fork them.
    @Override
    public Container getFormLayeredPane(Class c, boolean top) {
        if (windowLayeredPane == null) {
            windowLayeredPane = new Container(new LayeredLayout()) {
                @Override
                protected void paintBackground(Graphics g) {
                    if (getComponentCount() > 0 && super.isVisible()) {
                        super.setVisible(false);
                        // Clear inInternalPaint across this call so the window paints its
                        // background too. This runs while the window is mid-paint, and
                        // paint() skips the background in that state -- correctly, for the
                        // window's own pass, which has already drawn it. This pass is a
                        // different thing: it is the backdrop for whatever sits in this
                        // pane, so it has to be a whole window, background included.
                        // Without it the children were redrawn straight over the pixels
                        // already on screen. Anything opaque repainted its own background
                        // and hid that, but the title area is transparent, so its text was
                        // composited over the identical text underneath and came out
                        // heavier -- the one visible symptom of a window being drawn twice.
                        boolean wasInInternalPaint = inInternalPaint;
                        inInternalPaint = false;
                        try {
                            Window.this.paint(g);
                        } finally {
                            inInternalPaint = wasInInternalPaint;
                            super.setVisible(true);
                        }
                    }
                }
            };
            windowLayeredPane.setShouldLayout(false);
            super.addComponent(BorderLayout.OVERLAY, windowLayeredPane);
            windowLayeredPane.setWidth(getWidth());
            windowLayeredPane.setHeight(getHeight());
        }
        // The whole window overlay has its layout disabled, exactly as Form's does, so
        // nothing sizes the layers inside it. Form assigns each one the top level's
        // size at creation; without the same here every layer stays at zero and the
        // overlays that attach through this method -- Sheet, InteractionDialog,
        // ToastBar -- have no area to render into. Applied on every call rather than
        // only at creation, so a layer created before a resize is corrected too.
        Container layer = TopLevelSupport.layeredPane(windowLayeredPane, c, top);
        layer.setShouldLayout(false);
        layer.setWidth(getWidth());
        layer.setHeight(getHeight());
        return layer;
    }

    Container getWindowLayeredPaneIfExists() {
        return windowLayeredPane;
    }

    /// {@inheritDoc}
    @Override
    public Painter getGlassPane() {
        return glassPane;
    }

    /// {@inheritDoc}
    @Override
    public void setGlassPane(Painter glassPane) {
        this.glassPane = glassPane;
        repaint();
    }

    /// {@inheritDoc}
    @Override
    public String getTitle() {
        return pendingTitle;
    }

    @Override
    public void setTitle(final String title) {
        // Straight to the platform. The window's title is the one the OS draws in its
        // title bar; there is no in-content label to keep in step with it.
        pendingTitle = title;
        onPeer(new Runnable() {
            @Override
            public void run() {
                manager().setTitle(nativePeer, title);
            }
        });
    }

    /// {@inheritDoc}
    @Override
    public void addCommand(Command cmd) {
        commands.add(cmd);
        publishCommands();
    }

    /// {@inheritDoc}
    @Override
    public void removeCommand(Command cmd) {
        commands.remove(cmd);
        publishCommands();
    }

    /// {@inheritDoc}
    @Override
    public void removeAllCommands() {
        commands.clear();
        publishCommands();
    }

    /// Hands the current command list to the port so it can put them wherever this
    /// platform shows a window's commands -- a native menu bar on the window's own
    /// frame, where one exists.
    ///
    /// Without this the list was private bookkeeping: nothing consumed it, so a command
    /// added to a window was never displayed and never activated, while the same call
    /// on a `Form` works. A port with no command surface leaves them undisplayed, and
    /// `#dispatchCommand(Command, ActionEvent)` remains the programmatic path.
    private void publishCommands() {
        if (nativePeer == null) {
            return;
        }
        manager().setCommands(nativePeer,
                commands.toArray(new Command[commands.size()]));
    }

    /// {@inheritDoc}
    @Override
    public int getCommandCount() {
        return commands.size();
    }

    /// {@inheritDoc}
    @Override
    public Command getCommand(int index) {
        return commands.get(index);
    }

    /// {@inheritDoc}
    @Override
    public void addCommandListener(ActionListener l) {
        commandListeners.addListener(l);
    }

    /// {@inheritDoc}
    @Override
    public void removeCommandListener(ActionListener l) {
        commandListeners.removeListener(l);
    }

    /// Dispatches a command to the listeners registered on this window.
    ///
    /// #### Parameters
    ///
    /// - `cmd`: the command that was activated
    ///
    /// - `ev`: the event describing the activation
    /// Notifies this window's command listeners of an activation whose command action
    /// has already run.
    ///
    /// The counterpart of `Form`'s no-recurse dispatch, and needed for the same reason:
    /// a `Button` backed by a `Command` invokes the command itself and then tells its
    /// top level, which must not invoke it again.
    ///
    /// #### Parameters
    ///
    /// - `cmd`: the command that was activated
    ///
    /// - `ev`: the event describing the activation
    void dispatchCommandNoRecurse(Command cmd, ActionEvent ev) {
        if (cmd == null || ev.isConsumed()) {
            return;
        }
        commandListeners.fireActionEvent(ev);
    }

    /// Adds a component to the window's own border layout, outside the content pane,
    /// which is where structural furniture such as a permanent side menu belongs. The
    /// counterpart of `Form`'s form-level add.
    ///
    /// #### Parameters
    ///
    /// - `constraints`: the layout constraint
    ///
    /// - `cmp`: the component to add
    @Override
    final void addComponentToTopLevel(Object constraints, Component cmp) {
        super.addComponent(constraints, cmp);
    }

    /// Removes a component previously added with
    /// `#addComponentToTopLevel(Object, Component)`.
    ///
    /// #### Parameters
    ///
    /// - `cmp`: the component to remove
    @Override
    void removeComponentFromTopLevel(Component cmp) {
        super.removeComponent(cmp);
    }

    @Override
    boolean isNativeWindow() {
        return true;
    }

    @Override
    boolean isTopLevelShowing() {
        return isWindowShowing();
    }

    @Override
    Object topLevelNativePeer() {
        return nativePeer;
    }

    @Override
    void commandActivatedFromList(Command cmd, ActionEvent ev) {
        dispatchCommandNoRecurse(cmd, ev);
    }

    @Override
    void commandActivatedFromComponent(Command cmd, ActionEvent ev) {
        dispatchCommandNoRecurse(cmd, ev);
    }

    @Override
    boolean prefersPortraitLayout(boolean deviceBias) {
        return getHeight() >= getWidth();
    }

    @Override
    public void dispatchCommand(Command cmd, ActionEvent ev) {
        cmd.actionPerformed(ev);
        if (!ev.isConsumed()) {
            commandListeners.fireActionEvent(ev);
        }
    }

    // ---- animation ---------------------------------------------------------------

    /// {@inheritDoc}
    @Override
    public AnimationManager getAnimationManager() {
        return animMananger;
    }

    /// {@inheritDoc}
    @Override
    public void registerAnimated(Animation cmp) {
        if (!animatableComponents.contains(cmp)) {
            animatableComponents.add(cmp);
            repaint();
        }
    }

    /// {@inheritDoc}
    @Override
    public void deregisterAnimated(Animation cmp) {
        animatableComponents.remove(cmp);
    }

    /// {@inheritDoc}
    @Override
    void registerAnimatedInternal(Animation cmp) {
        // The component's own flag has to move with the list, exactly as Form does it:
        // deregisterAnimatedInternal returns early when the flag is clear, so leaving
        // it unset makes the removal a no-op and the component stays registered for
        // good. A fading scrollbar is enough to do it, and hasAnimations() then never
        // goes false again -- the event dispatch thread stops being able to sleep.
        if (cmp instanceof Component) {
            Component c = (Component) cmp;
            if (c.internalRegisteredAnimated) {
                return;
            }
            c.internalRegisteredAnimated = true;
        }
        if (!internalAnimatableComponents.contains(cmp)) {
            internalAnimatableComponents.add(cmp);
            repaint();
        }
    }

    /// {@inheritDoc}
    @Override
    void deregisterAnimatedInternal(Animation cmp) {
        if (cmp instanceof Component) {
            Component c = (Component) cmp;
            if (!c.internalRegisteredAnimated) {
                return;
            }
            c.internalRegisteredAnimated = false;
        }
        internalAnimatableComponents.remove(cmp);
    }

    /// {@inheritDoc}
    @Override
    public boolean grabAnimationLock() {
        // A real lock, as Form keeps: whether an animation happens to be running is
        // not the same question as whether this caller now owns the right to start
        // one. Returning isAnimating() inverted the contract -- callers acquired the
        // "lock" precisely when something else was already animating, and failed to
        // acquire it when the window was idle.
        if (animationLock) {
            return false;
        }
        animationLock = true;
        return true;
    }

    /// {@inheritDoc}
    @Override
    public void releaseAnimationLock() {
        // Simply drops the lock. The previous version handed null to
        // flushAnimation, which either invoked it immediately (an NPE on the spot
        // when nothing was animating) or queued it for updateAnimations to invoke
        // later (an NPE on the event dispatch thread when the queue drained).
        animationLock = false;
    }

    boolean hasAnimations() {
        return !animatableComponents.isEmpty()
                || !internalAnimatableComponents.isEmpty()
                || animMananger.isAnimating();
    }

    void repaintAnimations() {
        if (Display.getInstance().isEdt()) {
            loopAnimations(animatableComponents, null);
            // Excluding what the public list already animated, exactly as Form does.
            // A component can sit in both -- an explicitly animated scrollable whose
            // fading scrollbar is also running -- and animating it twice per frame
            // advances its motion at double speed and repeats any side effect.
            loopAnimations(internalAnimatableComponents, animatableComponents);
            animMananger.updateAnimations();
        }
    }

    private void loopAnimations(ArrayList<Animation> v, ArrayList<Animation> notIn) {
        // iterate by index and re-read the size: animate() may deregister itself
        for (int iter = 0; iter < v.size(); iter++) { // NOPMD ForLoopCanBeForeach
            Animation an = v.get(iter);
            if (an != null && (notIn == null || !notIn.contains(an)) && an.animate()) {
                if (an instanceof Component) {
                    Rectangle rect = ((Component) an).getDirtyRegion();
                    if (rect != null) {
                        Dimension d = rect.getSize();
                        ((Component) an).repaint(rect.getX(), rect.getY(), d.getWidth(), d.getHeight());
                    } else {
                        ((Component) an).repaint();
                    }
                } else {
                    repaintAnimation(an);
                }
            }
        }
    }

    private void repaintAnimation(Animation a) {
        if (paintSurface != null) {
            paintSurface.repaint(a);
        }
    }

    // ---- revalidate queue ------------------------------------------------------------

    /// {@inheritDoc}
    @Override
    void revalidateLater(Container cnt) {
        synchronized (pendingRevalidateQueue) {
            for (Container c : pendingRevalidateQueue) {
                if (c == cnt || c.contains(cnt)) { //NOPMD CompareObjectsWithEquals
                    return;
                }
            }
            pendingRevalidateQueue.add(cnt);
        }
        repaint();
    }

    /// {@inheritDoc}
    @Override
    void removeFromRevalidateQueue(Container cnt) {
        synchronized (pendingRevalidateQueue) {
            pendingRevalidateQueue.remove(cnt);
        }
    }

    /// {@inheritDoc}
    @Override
    void flushRevalidateQueue() {
        synchronized (pendingRevalidateQueue) {
            if (pendingRevalidateQueue.isEmpty()) {
                return;
            }
            revalidateQueue.addAll(pendingRevalidateQueue);
            pendingRevalidateQueue.clear();
        }
        int len = revalidateQueue.size();
        for (int i = 0; i < len; i++) {
            revalidateQueue.get(i).revalidateWithAnimationSafetyInternal(false);
        }
        revalidateQueue.clear();
    }

    /// {@inheritDoc}
    @Override
    boolean isRevalidateFromRoot() {
        // The same property Form honours. Hardcoding true ignored an application that
        // had turned it off, so a window revalidated from the root while its forms
        // did not.
        return revalidateFromRoot;
    }

    // ---- focus -----------------------------------------------------------------------

    /// {@inheritDoc}
    @Override
    public Component getFocused() {
        return focused;
    }

    /// {@inheritDoc}
    @Override
    public void setFocused(Component focused) {
        if (this.focused == focused) { //NOPMD CompareObjectsWithEquals
            return;
        }
        Component oldFocus = this.focused;
        this.focused = focused;
        boolean triggerRevalidate = false;
        if (oldFocus != null) {
            triggerRevalidate = changeFocusState(oldFocus, false);
            // No repaint when a revalidate is coming: the window repaints it.
            if (!triggerRevalidate && oldFocus.getParent() != null) {
                oldFocus.repaint();
            }
        }
        // A listener may change focus again from inside the notification, which must
        // not be undone here.
        if (focused != null && this.focused == focused) { //NOPMD CompareObjectsWithEquals
            triggerRevalidate = changeFocusState(focused, true) || triggerRevalidate;
            if (!triggerRevalidate) {
                focused.repaint();
            }
        }
        if (triggerRevalidate) {
            revalidateLater();
        }
    }

    /// Runs the full focus lifecycle for a component, the same way `Form` does.
    ///
    /// Toggling the focus flag and repainting is not enough: the notifications are
    /// what components build their behaviour on. `TextArea.focusGainedInternal()`
    /// enables its input handling there, so without this an arrow key traversed away
    /// from a text field in a window instead of moving the caret inside it.
    ///
    /// Returns true when the selected and unselected styles differ enough to change
    /// the preferred size, so the caller revalidates instead of repainting.
    private boolean changeFocusState(Component cmp, boolean gained) {
        boolean trigger = false;
        Style selected = cmp.getSelectedStyle();
        Style unselected = cmp.getUnselectedStyle();
        // Different selected styling is a good hint the preferred size moves with it.
        if (!selected.getFont().equals(unselected.getFont())
                || selected.getPaddingTop() != unselected.getPaddingTop()
                || selected.getPaddingBottom() != unselected.getPaddingBottom()
                || selected.getPaddingRight(isRTL()) != unselected.getPaddingRight(isRTL())
                || selected.getPaddingLeft(isRTL()) != unselected.getPaddingLeft(isRTL())
                || selected.getMarginTop() != unselected.getMarginTop()
                || selected.getMarginBottom() != unselected.getMarginBottom()
                || selected.getMarginRight(isRTL()) != unselected.getMarginRight(isRTL())
                || selected.getMarginLeft(isRTL()) != unselected.getMarginLeft(isRTL())) {
            trigger = true;
        }
        int prefW = 0;
        int prefH = 0;
        if (trigger) {
            Dimension d = cmp.getPreferredSize();
            prefW = d.getWidth();
            prefH = d.getHeight();
        }

        if (gained) {
            cmp.setFocus(true);
            cmp.fireFocusGained();
        } else {
            cmp.setFocus(false);
            cmp.fireFocusLost();
        }

        // The styles can differ without the preferred size actually moving, so only
        // revalidate when it really did. Form had this test inverted and is fixed to
        // match; getting it wrong drops the revalidate in exactly the case that needs
        // one.
        if (trigger) {
            cmp.setShouldCalcPreferredSize(true);
            Dimension d = cmp.getPreferredSize();
            if (prefW == d.getWidth() && prefH == d.getHeight()) {
                cmp.setShouldCalcPreferredSize(false);
                trigger = false;
            }
        }

        return trigger;
    }

    /// {@inheritDoc}
    @Override
    void setFocusedInternal(Component focused) {
        if (this.focused != null) {
            this.focused.setFocus(false);
        }
        this.focused = focused;
        if (focused != null) {
            focused.setFocus(true);
        }
    }

    /// {@inheritDoc}
    @Override
    void requestFocus(Component cmp) {
        if (cmp.isFocusable() && contains(cmp)) {
            scrollComponentToVisible(cmp);
            setFocused(cmp);
        }
    }

    /// {@inheritDoc}
    @Override
    public boolean isCyclicFocus() {
        return cyclicFocus;
    }

    /// {@inheritDoc}
    @Override
    public void setCyclicFocus(boolean cyclicFocus) {
        this.cyclicFocus = cyclicFocus;
    }

    /// {@inheritDoc}
    @Override
    public boolean isSingleFocusMode() {
        // Computed as Form computes it, rather than hardcoded. Single focus mode
        // changes key handling -- with one focusable there is nothing to traverse to,
        // so the arrow keys belong to the component -- and returning a constant made
        // a one-control window behave differently from the identical Form.
        return countFocusables(getActualPane()) + countFocusables(windowLayeredPane) < 2;
    }

    /// Focusable components in a subtree, used by `#isSingleFocusMode()`.
    private static int countFocusables(Container root) {
        if (root == null) {
            return 0;
        }
        int count = 0;
        int len = root.getComponentCount();
        for (int iter = 0; iter < len; iter++) {
            Component c = root.getComponentAt(iter);
            if (c instanceof Container) {
                count += countFocusables((Container) c);
            }
            if (c.isFocusable()) {
                count++;
            }
            if (count > 1) {
                // Only the "fewer than two" answer matters; stop early.
                return count;
            }
        }
        return count;
    }

    /// {@inheritDoc}
    @Override
    public Form.TabIterator getTabIterator(Component start) {
        return Form.buildTabIterator(this, start);
    }

    /// {@inheritDoc}
    @Override
    public void scrollComponentToVisible(Component c) {
        Container parent = c.getParent();
        while (parent != null) {
            if (parent.isScrollable()) {
                parent.scrollComponentToVisible(c);
                return;
            }
            parent = parent.getParent();
        }
    }

    /// {@inheritDoc}
    @Override
    public void addKeyListener(int keyCode, ActionListener listener) {
        if (keyListeners == null) {
            keyListeners = new HashMap<Integer, ArrayList<ActionListener>>();
        }
        Integer code = Integer.valueOf(keyCode);
        ArrayList<ActionListener> l = keyListeners.get(code);
        if (l == null) {
            l = new ArrayList<ActionListener>();
            keyListeners.put(code, l);
        }
        if (!l.contains(listener)) {
            l.add(listener);
        }
    }

    /// {@inheritDoc}
    @Override
    public void removeKeyListener(int keyCode, ActionListener listener) {
        if (keyListeners == null) {
            return;
        }
        ArrayList<ActionListener> l = keyListeners.get(Integer.valueOf(keyCode));
        if (l != null) {
            l.remove(listener);
        }
    }

    // ---- editing --------------------------------------------------------------------

    /// {@inheritDoc}
    @Override
    public boolean isEditing() {
        Component c = findCurrentlyEditingComponent();
        return c != null && c.isEditing();
    }

    /// {@inheritDoc}
    @Override
    public void stopEditing(Runnable onFinish) {
        Component c = findCurrentlyEditingComponent();
        if (c != null) {
            c.stopEditing(onFinish);
        } else if (onFinish != null) {
            onFinish.run();
        }
    }

    /// {@inheritDoc}
    @Override
    public Component findCurrentlyEditingComponent() {
        return findCurrentlyEditingComponent(this);
    }

    private static Component findCurrentlyEditingComponent(Container root) {
        int len = root.getComponentCount();
        for (int iter = 0; iter < len; iter++) {
            Component c = root.getComponentAt(iter);
            if (c.isEditing()) {
                return c;
            }
            if (c instanceof Container) {
                Component inner = findCurrentlyEditingComponent((Container) c);
                if (inner != null) {
                    return inner;
                }
            }
        }
        return null;
    }

    /// {@inheritDoc}
    @Override
    public VirtualInputDevice getCurrentInputDevice() {
        return currentInputDevice;
    }

    /// {@inheritDoc}
    @Override
    public void setCurrentInputDevice(VirtualInputDevice device) throws Exception {
        if (currentInputDevice != null) {
            currentInputDevice.close();
        }
        currentInputDevice = device;
    }

    // ---- theme and metrics -----------------------------------------------------------

    /// {@inheritDoc}
    @Override
    public UIManager getUIManager() {
        return uiManager != null ? uiManager : UIManager.getInstance();
    }

    /// {@inheritDoc}
    @Override
    public void setUIManager(UIManager uiManager) {
        this.uiManager = uiManager;
        refreshTheme(false);
    }

    /// {@inheritDoc}
    ///
    /// A desktop window has no notch or rounded corner to avoid, so the safe area is
    /// the whole window.
    @Override
    public Rectangle getSafeArea() {
        return new Rectangle(0, 0, getWidth(), getHeight());
    }

    /// {@inheritDoc}
    ///
    /// Always zero: a desktop window has no virtual keyboard overlaying it.
    @Override
    public int getInvisibleAreaUnderVKB() {
        return 0;
    }

    /// {@inheritDoc}
    @Override
    public int getDragRegionStatus(int x, int y) {
        // A decorated window is dragged by its native title bar, and an undecorated one
        // draws whatever chrome it wants inside its own content -- so nothing here is
        // a drag handle by default. This used to answer "draggable" whenever a toolbar
        // was installed, which was the mobile title bar standing in for a title bar the
        // platform already provides.
        return Component.DRAG_REGION_NOT_DRAGGABLE;
    }

    /// {@inheritDoc}
    @Override
    public boolean isEnableCursors() {
        return enableCursors;
    }

    /// {@inheritDoc}
    @Override
    public void setEnableCursors(boolean e) {
        enableCursors = e;
    }

    /// {@inheritDoc}
    @Override
    public TextSelection getTextSelection() {
        return textSelection;
    }

    // ---- native window attributes -------------------------------------------------------

    /// Sets whether the user may resize this window.
    ///
    /// #### Parameters
    ///
    /// - `resizable`: true to allow resizing
    public void setResizable(final boolean resizable) {
        this.resizable = resizable;
        onPeer(new Runnable() {
            @Override
            public void run() {
                manager().setResizable(nativePeer, resizable);
            }
        });
    }

    /// Runs an operation against this window's native peer on the event dispatch
    /// thread, checking the peer is still there when it gets there.
    ///
    /// Every attribute setter needs this. The ports resolve a peer to a slot in a
    /// native table on whatever thread calls them, and a slot is reused once its
    /// window is disposed -- so a setter called from a background thread can land on
    /// whichever window took the slot, or race the teardown freeing it. The field each
    /// setter keeps is assigned on the calling thread, so its getter stays consistent
    /// with what the caller asked for; only the platform call is deferred.
    ///
    /// #### Parameters
    ///
    /// - `op`: the platform call, which may assume a non-null peer
    private void onPeer(final Runnable op) {
        if (nativePeer == null) {
            return;
        }
        if (Display.getInstance().isEdt()) {
            op.run();
            return;
        }
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                if (nativePeer != null) {
                    op.run();
                }
            }
        });
    }

    /// Indicates whether the user may resize this window.
    ///
    /// #### Returns
    ///
    /// true if the window is resizable
    public boolean isResizable() {
        return resizable;
    }

    /// Sets whether the platform draws a title bar and border for this window.
    ///
    /// An undecorated window paired with a `Toolbar` is how an application draws its
    /// own chrome.
    ///
    /// #### Parameters
    ///
    /// - `decorated`: true for native decorations
    public void setDecorated(final boolean decorated) {
        this.decorated = decorated;
        onPeer(new Runnable() {
            @Override
            public void run() {
                manager().setDecorated(nativePeer, decorated);
            }
        });
    }

    /// Indicates whether the platform draws this window's chrome.
    ///
    /// #### Returns
    ///
    /// true if the window is natively decorated
    public boolean isDecorated() {
        return decorated;
    }

    /// Keeps this window above the application's other windows.
    ///
    /// #### Parameters
    ///
    /// - `alwaysOnTop`: true to float the window
    public void setAlwaysOnTop(final boolean alwaysOnTop) {
        this.alwaysOnTop = alwaysOnTop;
        if (nativePeer != null) {
            // The field is set above on the calling thread so a getter stays
            // consistent, but the SPI call is marshalled: the ports resolve the peer to
            // a slot on whatever thread calls them, which races an EDT disposal.
            if (Display.getInstance().isEdt()) {
                manager().setAlwaysOnTop(nativePeer, alwaysOnTop);
            } else {
                Display.getInstance().callSerially(new Runnable() {
                    @Override
                    public void run() {
                        if (nativePeer != null) {
                            manager().setAlwaysOnTop(nativePeer, alwaysOnTop);
                        }
                    }
                });
            }
        }
    }

    /// Indicates whether this window floats above the others.
    ///
    /// #### Returns
    ///
    /// true if the window is always on top
    public boolean isAlwaysOnTop() {
        return alwaysOnTop;
    }

    /// Marks this window as a palette or tool window, which the platform typically
    /// keeps out of the task bar.
    ///
    /// #### Parameters
    ///
    /// - `utility`: true for a utility window
    public void setUtilityWindow(final boolean utility) {
        this.utilityWindow = utility;
        onPeer(new Runnable() {
            @Override
            public void run() {
                manager().setUtilityWindow(nativePeer, utility);
            }
        });
    }

    /// Indicates whether this is a utility window.
    ///
    /// #### Returns
    ///
    /// true for a utility window
    public boolean isUtilityWindow() {
        return utilityWindow;
    }

    /// Sets the icon the platform shows for this window.
    ///
    /// #### Parameters
    ///
    /// - `icon`: the icon to display
    public void setWindowIcon(final Image icon) {
        this.windowIcon = icon;
        onPeer(new Runnable() {
            @Override
            public void run() {
                manager().setIcon(nativePeer, icon);
            }
        });
    }

    /// Returns the icon the platform shows for this window.
    ///
    /// #### Returns
    ///
    /// the window icon, or null when none was set
    public Image getWindowIcon() {
        return windowIcon;
    }

    // ---- geometry -------------------------------------------------------------------

    /// Returns this window's bounds in desktop coordinates, including any native
    /// chrome.
    ///
    /// This is a different coordinate space from `Component#getWidth()` and
    /// `Component#getHeight()`, which report the Codename One content size.
    ///
    /// #### Returns
    ///
    /// the native window bounds
    public Rectangle getWindowBounds() {
        if (nativePeer == null) {
            return new Rectangle(pendingX, pendingY, pendingWidth, pendingHeight);
        }
        int[] out = manager().getBounds(nativePeer, new int[4]);
        return new Rectangle(out[0], out[1], out[2], out[3]);
    }

    /// Moves and resizes this window.
    ///
    /// #### Parameters
    ///
    /// - `r`: the new bounds in desktop coordinates
    public void setWindowBounds(Rectangle r) {
        setWindowBounds(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    private void setWindowBounds(final int x, final int y, final int w, final int h) {
        // Marshalled exactly as show(), hide() and dispose() are, and as the developer
        // guide promises for moving a window. Without it a background caller mutated
        // the pending geometry and the cached monitor while the event dispatch thread
        // was reading them, and drove the window manager concurrently with the
        // platform callbacks that report the result.
        if (!Display.getInstance().isEdt()) {
            Display.getInstance().callSerially(new Runnable() {
                @Override
                public void run() {
                    setWindowBounds(x, y, w, h);
                }
            });
            return;
        }
        pendingX = x;
        pendingY = y;
        pendingPositionSet = true;
        pendingWidth = w;
        pendingHeight = h;
        // A move can land the window on a different display, so the cached monitor no
        // longer answers for it. Without this the cache stood until the port's
        // monitor-change callback arrived, and that callback is queued back to the
        // event dispatch thread: a centerOnDesktop(), getScale() or getDensity() in
        // the same turn still read the old display, and centring right after a move
        // to another monitor put the window back on the one it came from.
        currentMonitor = null;
        if (nativePeer != null) {
            manager().setBounds(nativePeer, x, y, w, h);
        }
    }

    /// Resizes this window, leaving its position alone.
    ///
    /// #### Parameters
    ///
    /// - `width`: the new width
    ///
    /// - `height`: the new height
    public void setWindowSize(final int width, final int height) {
        // As setWindowBounds: the no-peer branch below writes the pending size, which
        // the event dispatch thread reads when the window is shown.
        if (!Display.getInstance().isEdt()) {
            Display.getInstance().callSerially(new Runnable() {
                @Override
                public void run() {
                    setWindowSize(width, height);
                }
            });
            return;
        }
        if (nativePeer == null) {
            // Only the size. Routing through setWindowBounds before the window exists
            // would hand the port the placeholder (0,0) as though the application had
            // chosen it, and every port then skips the window manager's own
            // placement -- which is the opposite of what this method promises.
            pendingWidth = width;
            pendingHeight = height;
            return;
        }
        Rectangle current = getWindowBounds();
        setWindowBounds(current.getX(), current.getY(), width, height);
    }

    /// Moves this window, leaving its size alone.
    ///
    /// #### Parameters
    ///
    /// - `x`: the new x position in desktop coordinates
    ///
    /// - `y`: the new y position in desktop coordinates
    public void setWindowLocation(final int x, final int y) {
        // The read has to happen on the event dispatch thread with the write, not
        // before it. setWindowBounds marshals itself, but reading the bounds out here
        // first meant a background caller that resized and then moved queued the move
        // carrying the *old* size -- so the event dispatch thread applied the resize
        // and then silently undid it.
        if (!Display.getInstance().isEdt()) {
            Display.getInstance().callSerially(new Runnable() {
                @Override
                public void run() {
                    setWindowLocation(x, y);
                }
            });
            return;
        }
        Rectangle current = getWindowBounds();
        setWindowBounds(x, y, current.getWidth(), current.getHeight());
    }

    /// Sets the smallest size the user may resize this window to.
    ///
    /// #### Parameters
    ///
    /// - `d`: the minimum size
    public void setMinimumWindowSize(final Dimension d) {
        minimumWindowSize = d;
        onPeer(new Runnable() {
            @Override
            public void run() {
                manager().setMinimumSize(nativePeer,
                        d == null ? 0 : d.getWidth(), d == null ? 0 : d.getHeight());
            }
        });
    }

    /// Returns the smallest size the user may resize this window to.
    ///
    /// #### Returns
    ///
    /// the minimum size, or null when none was set
    public Dimension getMinimumWindowSize() {
        return minimumWindowSize;
    }

    /// Centres this window on the work area of the monitor it sits on, so it does not
    /// land under the task bar or dock.
    public void centerOnDesktop() {
        // The whole calculation runs on the event dispatch thread, not just the move at
        // the end. setWindowLocation marshals itself, but the reads above it did not, so
        // a background caller that resized and then centred computed the centre from the
        // size the window had *before* the queued resize -- and the event dispatch thread
        // then applied the resize followed by a location centred for the old size. The
        // same trap setWindowLocation itself documents.
        if (!Display.getInstance().isEdt()) {
            Display.getInstance().callSerially(new Runnable() {
                @Override
                public void run() {
                    centerOnDesktop();
                }
            });
            return;
        }
        Rectangle work = getMonitor().getWorkArea();
        Rectangle b = getWindowBounds();
        setWindowLocation(work.getX() + (work.getWidth() - b.getWidth()) / 2,
                work.getY() + (work.getHeight() - b.getHeight()) / 2);
    }

    /// Centres this window over another top level.
    ///
    /// #### Parameters
    ///
    /// - `other`: the top level to centre over
    public void centerOn(final TopLevelContainer other) {
        // Marshalled as a whole for the same reason as centerOnDesktop: this reads both
        // windows' bounds and only then moves, so computing off the event dispatch
        // thread centres against geometry a queued resize is about to invalidate.
        if (!Display.getInstance().isEdt()) {
            Display.getInstance().callSerially(new Runnable() {
                @Override
                public void run() {
                    centerOn(other);
                }
            });
            return;
        }
        Rectangle o = null;
        if (other instanceof Window) {
            o = ((Window) other).getWindowBounds();
        } else if (other != null) {
            // A Form lives in the application's main native window, so centre over
            // that. Falling through to centerOnDesktop() centred on the monitor's work
            // area instead, which is a different place whenever the main window has
            // been moved, maximized or simply does not fill the screen -- and this
            // method's contract is to centre over the top level it was given.
            o = mainWindowBounds();
        }
        if (o == null) {
            centerOnDesktop();
            return;
        }
        Rectangle b = getWindowBounds();
        setWindowLocation(o.getX() + (o.getWidth() - b.getWidth()) / 2,
                o.getY() + (o.getHeight() - b.getHeight()) / 2);
    }

    /// The application's main native window in desktop coordinates, or null when the
    /// port cannot report it.
    private Rectangle mainWindowBounds() {
        WindowManager wm = Display.impl == null ? null : Display.impl.getWindowManager();
        if (wm == null) {
            return null;
        }
        int[] b = wm.getMainWindowBounds(new int[4]);
        if (b == null || b[2] <= 0 || b[3] <= 0) {
            return null;
        }
        return new Rectangle(b[0], b[1], b[2], b[3]);
    }

    /// Minimizes this window.
    public void minimize() {
        // Marshalled like show(), hide() and dispose(). The window manager SPI is
        // defined on the event dispatch thread, and the ports take it literally: the
        // Windows one resolves the peer to a slot index on the calling thread and hands
        // that index to the native layer, so a call from a background thread can read a
        // slot an EDT disposal is tearing down. The developer guide also promises this
        // is marshalled for the caller.
        if (!Display.getInstance().isEdt()) {
            Display.getInstance().callSerially(new Runnable() {
                @Override
                public void run() {
                    minimize();
                }
            });
            return;
        }
        if (nativePeer != null) {
            manager().minimize(nativePeer);
        }
    }

    /// Restores this window from a minimized state.
    ///
    /// A window the application hid is not minimized and is not brought back by this.
    /// `#hide()` leaves the peer alive with the hierarchy invisible, so handing that
    /// peer to the platform's restore puts the native window back on screen while the
    /// framework still counts it as hidden -- and nothing ever repaints it, because the
    /// paint loop skips a window that is not showing. The result is a blank or stale
    /// window that `#isWindowShowing()` denies is there. Bringing a hidden window back
    /// is `#show()`'s job, which restores the whole lifecycle rather than just the
    /// native state.
    public void restore() {
        // Marshalled as a whole, not just the native call. showOwnerChain() below may
        // queue the owner's show(), and a background caller would then hand the child to
        // the platform's restore first -- putting it back on screen ahead of its owner,
        // or letting the window system suppress it while the framework counts it back.
        // It also kept a WindowManager call off the event dispatch thread, which is the
        // only context the SPI is defined in.
        if (!Display.getInstance().isEdt()) {
            Display.getInstance().callSerially(new Runnable() {
                @Override
                public void run() {
                    restore();
                }
            });
            return;
        }
        // Deliberately not "iconified only": between minimize() and the platform
        // reporting it, a window is still nativeVisible, and an application that
        // minimizes and immediately restores has to get its window back.
        if (!nativeVisible && !iconified) {
            return;
        }
        // Same reason show() does it: bringing a window back while its owner is away
        // puts it on screen without the owner, or lets the window system suppress it
        // while the framework counts it back. The owner chain comes first.
        showOwnerChain();
        if (nativePeer != null) {
            manager().restore(nativePeer);
        }
    }

    /// The platform refused to create this window's native surface, so it will never
    /// appear.
    ///
    /// Deliberately not routed through `#hideNotify()`, which is the minimize path:
    /// that keeps the modal registration on purpose, because a minimized window is
    /// still open. A modal window that never appeared would then go on blocking input
    /// to every other window while `#showModal()` waited for a window nobody can see.
    /// This releases modality the way an explicit `#hide()` does.
    ///
    /// The window stays registered rather than being disposed, so the application's
    /// object survives and a later `#show()` can ask the platform again.
    void activationFailed() {
        if (!nativeVisible && !iconified) {
            return;
        }
        nativeVisible = false;
        iconified = false;
        cancelPendingInput();
        releaseModal();
        setVisible(false);
        clearPaintSurface();
        fireWindowEvent(WindowEvent.Type.Hidden);
    }

    /// Brings any owner above this window back before this one goes on screen.
    ///
    /// An owned window cannot be on screen without its owner, and an owner the
    /// application hid has to come back through its own lifecycle: a port can map the
    /// native window, but only `#show()` makes the component hierarchy visible again
    /// and reacquires the modality that `#hide()` released, so restoring it natively
    /// alone would leave an unpainted, non-interactive window that no longer blocks
    /// input.
    ///
    /// A minimized owner is included: only one port restored one of those itself, so
    /// everywhere else the child was mapped against an owner still minimized --
    /// appearing without it, or suppressed by the window system while the framework
    /// counted it visible and took its modal blocker, which strands an application
    /// modal with all input blocked.
    ///
    /// `show()` recurses into this, so a whole hidden chain comes back furthest owner
    /// first. The owner chain cannot cycle -- `#setOwnerWindow(TopLevelContainer)`
    /// rejects that when the relation is established.
    private void showOwnerChain() {
        if (ownerWindow instanceof Window) {
            Window owner = (Window) ownerWindow;
            if (!owner.isWindowShowing()) {
                owner.show();
            }
        }
    }

    /// Toggles this window between maximized and its previous size.
    public void toggleMaximize() {
        // Marshalled like show(), hide() and dispose(). The window manager SPI is
        // defined on the event dispatch thread, and the ports take it literally: the
        // Windows one resolves the peer to a slot index on the calling thread and hands
        // that index to the native layer, so a call from a background thread can read a
        // slot an EDT disposal is tearing down. The developer guide also promises this
        // is marshalled for the caller.
        if (!Display.getInstance().isEdt()) {
            Display.getInstance().callSerially(new Runnable() {
                @Override
                public void run() {
                    toggleMaximize();
                }
            });
            return;
        }
        if (nativePeer != null) {
            manager().toggleMaximize(nativePeer);
        }
    }

    /// Raises this window and gives it keyboard focus.
    public void requestWindowFocus() {
        // Marshalled like show(), hide() and dispose(). The window manager SPI is
        // defined on the event dispatch thread, and the ports take it literally: the
        // Windows one resolves the peer to a slot index on the calling thread and hands
        // that index to the native layer, so a call from a background thread can read a
        // slot an EDT disposal is tearing down. The developer guide also promises this
        // is marshalled for the caller.
        if (!Display.getInstance().isEdt()) {
            Display.getInstance().callSerially(new Runnable() {
                @Override
                public void run() {
                    requestWindowFocus();
                }
            });
            return;
        }
        if (nativePeer != null) {
            manager().requestFocus(nativePeer);
        }
    }

    /// Indicates whether this window currently holds keyboard focus.
    ///
    /// #### Returns
    ///
    /// true if this window is focused
    public boolean isWindowFocused() {
        return Desktop.getInstance().getFocusedWindow() == this; //NOPMD CompareObjectsWithEquals
    }

    // ---- monitor and density --------------------------------------------------------

    /// Returns the monitor this window currently sits on.
    ///
    /// #### Returns
    ///
    /// the monitor showing this window
    public Monitor getMonitor() {
        if (nativePeer == null) {
            // No peer to ask yet. Answer from the location the application requested
            // rather than caching the primary monitor: centerOnDesktop() would
            // otherwise recentre a pre-positioned window back onto the primary
            // display, and the cached fallback would leave scale and density stale
            // once the window did appear.
            if (pendingPositionSet) {
                return Desktop.getInstance().getMonitorAt(pendingX, pendingY);
            }
            return Desktop.getInstance().getPrimaryMonitor();
        }
        if (currentMonitor == null) {
            currentMonitor = Desktop.getInstance().getMonitorFor(this);
        }
        return currentMonitor;
    }

    /// Returns the density of the monitor this window sits on, which on a mixed
    /// resolution desktop is not necessarily the density `Display` reports.
    ///
    /// #### Returns
    ///
    /// the density constant for this window's monitor
    public int getDensity() {
        return getMonitor().getDensity();
    }

    /// Returns the backing scale of the monitor this window sits on.
    ///
    /// #### Returns
    ///
    /// the scale factor for this window's monitor
    public double getScale() {
        return getMonitor().getScale();
    }

    /// Asks the port to rebuild this window's native surface after the platform
    /// destroyed it unasked. See `com.codename1.impl.WindowManager#reopen(Object)`.
    boolean reopenNativeSurface() {
        if (nativePeer == null || disposing) {
            return false;
        }
        if (!manager().reopen(nativePeer)) {
            return false;
        }
        // The surface is being rebuilt, so nothing painted so far survives.
        clearPaintSurface();
        paintedOnce = false;
        repaint();
        return true;
    }

    /// Invoked by the framework when the platform reports that the user moved this
    /// window. Nothing needs re-laying out -- only the position changed -- so this
    /// just reports it.
    void moved() {
        rememberNativeBounds();
        // Dropped before the event, not after it. getMonitor() answers from a lazy
        // cache, and a move is exactly what invalidates it -- so a Moved listener
        // asking getMonitor(), getScale() or getDensity() was told which monitor the
        // window had been on before it moved. The cache is otherwise refreshed only by
        // the monitor-changed notification, which the ports queue *after* this one, and
        // nothing tells the application to ask again in between.
        //
        // Dropped rather than recomputed, so a move nobody asks about costs nothing.
        currentMonitor = null;
        fireWindowEvent(WindowEvent.Type.Moved);
    }

    /// Copies the peer's current geometry into the fields `#getWindowBounds()` falls
    /// back on once the peer is gone.
    ///
    /// Without this the fallback answered with whatever the application last
    /// *requested*, so a window the user had dragged or resized reported its original
    /// position and size in the terminal `Hidden` and `Disposed` events -- and a
    /// listener persisting geometry across runs restored the wrong rectangle.
    private void rememberNativeBounds() {
        if (nativePeer == null) {
            return;
        }
        int[] out = manager().getBounds(nativePeer, new int[4]);
        if (out[2] > 0 && out[3] > 0) {
            pendingX = out[0];
            pendingY = out[1];
            pendingPositionSet = true;
            pendingWidth = out[2];
            pendingHeight = out[3];
        }
    }

    /// Invoked by the framework when the platform reports that this window has moved
    /// to a monitor with different characteristics. Re-reads the scale and lays the
    /// hierarchy out again, since preferred sizes computed at the old scale are stale.
    void monitorChanged() {
        currentMonitor = Desktop.getInstance().getMonitorFor(this);
        // Re-read the drawable size rather than laying out at the one we already had.
        // A move between monitors of different backing scale changes how many device
        // pixels the same window is worth without any logical resize accompanying it,
        // so the port reports a new size while this window still believes the old one:
        // the hierarchy went on laying out and painting at the previous scale into a
        // buffer sized for the new one, which clips the content or leaves part of it
        // blank.
        if (nativePeer != null) {
            WindowManager wm = manager();
            int nativeWidth = wm.getWidth(nativePeer);
            int nativeHeight = wm.getHeight(nativePeer);
            if (nativeWidth > 0 && nativeHeight > 0
                    && (nativeWidth != getWidth() || nativeHeight != getHeight())) {
                sizeChangedInternal(nativeWidth, nativeHeight);
                repaint();
                return;
            }
        }
        setShouldCalcPreferredSize(true);
        revalidateWithAnimationSafety();
        repaint();
    }

    // ---- lifecycle ---------------------------------------------------------------------

    /// Shows this window, creating the native window the first time it is called.
    @Override
    public void show() {
        requireLive();
        if (!Display.getInstance().isEdt()) {
            Display.getInstance().callSerially(new Runnable() {
                @Override
                public void run() {
                    show();
                }
            });
            return;
        }
        if (nativePeer != null && nativeVisible) {
            // Already on screen. Everything below is a transition -- taking the modal
            // blocker, mapping the peer, firing Shown -- and repeating it for a window
            // that never left fires a second Shown at listeners doing initialization or
            // persistence, which is one showing as far as the user is concerned.
            //
            // showModal() calls this before its wait, so a window shown this way and
            // then made modal still parks its caller: the wait is the caller's, not
            // something this method does.
            return;
        }
        WindowManager wm = manager();
        showOwnerChain();
        if (nativePeer == null) {
            if (ownerWindow instanceof Window && ((Window) ownerWindow).nativePeer == null) {
                // Showing a window whose owner has not been shown yet would create the
                // child against the wrong native owner, permanently: every port fixes
                // the relation at creation. Create the owner's native window first.
                ((Window) ownerWindow).show();
            }
            Object parentPeer = ownerPeer();
            // A null peer means two different things -- no owner at all, or an owner
            // that is the application's main form -- and a port has to tell them
            // apart: one is a top level window, the other is a child of the main one.
            boolean ownedByMainWindow = parentPeer == null && ownerWindow != null;
            Object peer = wm.createWindow(windowId, pendingTitle, pendingX, pendingY,
                    pendingWidth, pendingHeight, decorated, resizable, parentPeer,
                    pendingPositionSet, ownedByMainWindow);
            if (peer == null) {
                // Every port has a bounded native window table. Continuing here would
                // register a window that paints through null graphics forever, which
                // surfaces far away from the call that asked for one window too many.
                throw new IllegalStateException(
                        "the platform could not create a native window for " + getTitle());
            }
            nativePeer = peer;
            paintSurface = Display.impl.createPaintSurface(nativePeer);
            windowGraphics = Desktop.getInstance().createWindowGraphics(this);
            if (windowIcon != null) {
                wm.setIcon(nativePeer, windowIcon);
            }
            if (alwaysOnTop) {
                wm.setAlwaysOnTop(nativePeer, true);
            }
            if (utilityWindow) {
                wm.setUtilityWindow(nativePeer, true);
            }
            if (minimumWindowSize != null) {
                wm.setMinimumSize(nativePeer, minimumWindowSize.getWidth(),
                        minimumWindowSize.getHeight());
            }
        }
        Desktop.getInstance().registerWindow(this);
        // Commands added before the peer existed have not reached the port yet.
        publishCommands();
        setVisible(true);
        // A port that creates its native window asynchronously reports zero until it
        // exists. Keep the requested size until a real one is delivered, rather than
        // collapsing the window to nothing and laying out against that.
        int nativeWidth = wm.getWidth(nativePeer);
        int nativeHeight = wm.getHeight(nativePeer);
        if (nativeWidth > 0 && nativeHeight > 0) {
            sizeChangedInternal(nativeWidth, nativeHeight);
        }
        // Same hierarchy initialization Display.setCurrent() performs for a Form.
        // Without it every component added before show() stays uninitialized, so
        // initComponent() never runs, the look and feel is never bound and native
        // peers are never attached. It has to happen before layout, since a peer
        // reports a preferred size only once it exists.
        if (!isInitialized()) {
            initComponentImpl();
        }
        revalidateWithAnimationSafety();
        initFocused();
        // Whether this is bringing a hidden window back, which decides whether what it
        // painted before still stands for what it shows now.
        boolean wasHidden = !nativeVisible;
        nativeVisible = true;
        if (wasHidden) {
            // Its surface was dropped when it went away, and its components were free
            // to change while nothing was painting it. The repaint below fills it in
            // again, but until that runs the raster is the one from before the hide --
            // so anything waiting on hasPaintedOnce() has to wait for the new content
            // rather than capture the old. The resize and surface-reopen paths reset
            // this for the same reason.
            paintedOnce = false;
        }
        // A window being shown is by definition no longer minimized. Only hide() and
        // showNotify() cleared this before, so restoring an iconified window through
        // show() left it marked iconified while it was on screen.
        boolean wasIconified = iconified;
        iconified = false;
        acquireModal();
        // Only meaningful when this window is *not* modal, which is why acquireModal()
        // above cannot cover it: a window shown while someone else's application modal
        // is up registers no blocker of its own, so no port ever hears about the new
        // peer. Ports enable a native window by default, leaving its title bar live --
        // focusable, movable, closable -- underneath a modal that is supposed to be
        // blocking it. Recomputed here, before the peer is mapped, so the window is
        // never briefly interactive.
        Desktop.getInstance().syncNativeModalBlocking();
        wm.show(nativePeer);
        if (wasIconified) {
            // Mapping a window does not clear its iconic state: AWT's setVisible(true)
            // and Win32's SW_SHOW both leave it minimized, and only the dedicated
            // restore path (Frame.NORMAL, SW_RESTORE) brings it back. Without this the
            // framework counted the window restored -- and, when it was an owner,
            // mapped the child and took its modal blocker -- while the platform still
            // had the window in the dock or taskbar.
            wm.restore(nativePeer);
        }
        showListeners.fireActionEvent(new ActionEvent(this));
        fireWindowEvent(WindowEvent.Type.Shown);
        repaint();
        Display.getInstance().wakeEdt();
    }

    /// Shows this window and blocks the calling code until it is disposed.
    ///
    /// This uses the same mechanism as a modal `Dialog`: the caller is parked while
    /// the event dispatch thread keeps running, so every other window carries on
    /// painting and animating. Input to the windows this one blocks is dropped by the
    /// framework, so modality behaves the same way on every platform whether or not
    /// the platform implements its own.
    public void showModal() {
        if (modalityType == MODALITY_NONE) {
            modalityType = MODALITY_APPLICATION;
        }
        // show() registers the blocker, since a window shown any other way with a
        // modality type set has to block too.
        //
        // From a background thread show() only queues its work and returns, so the
        // wait below would find the window not visible yet, decide the modal was
        // already over and return before it ever appeared. Wait for the show to
        // actually happen first.
        if (Display.getInstance().isEdt()) {
            show();
        } else {
            Display.getInstance().callSeriallyAndWait(new Runnable() {
                @Override
                public void run() {
                    show();
                }
            });
        }
        try {
            Display.getInstance().invokeAndBlock(new Runnable() {
                @Override
                public void run() {
                    // Hidden counts as over, not only disposed: HIDE_ON_CLOSE means the
                    // user closed the window without destroying it, and parking the
                    // caller for a window nobody can see again is a hang.
                    while (!isModalFinished()) {
                        synchronized (Display.lock) {
                            if (isModalFinished()) {
                                break;
                            }
                            try {
                                Display.lock.wait(40);
                            } catch (InterruptedException err) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                    }
                }
            });
        } finally {
            // Only when the wait ended because the window did. The loop above also
            // breaks on an interrupt, and the window is still on screen then -- so
            // releasing here left a modal window visible with input flowing to the
            // windows behind it, which is the one thing a modal must not do.
            //
            // Nothing leaks by keeping it: the blocker belongs to the window, and
            // hide(), dispose(), activationFailed() and setModalityType() all release
            // it when the window is really finished with.
            if (isModalFinished()) {
                releaseModal();
            }
        }
    }

    /// True once a modal window has stopped being modal, either because it was
    /// disposed or because it was hidden.
    ///
    /// Minimizing is deliberately not either of those. It also clears `nativeVisible`,
    /// but the window is still open and still modal, and treating it as finished would
    /// end the wait and drop the blocker -- so restoring the window would leave a modal
    /// window on screen with input flowing to the windows behind it.
    private boolean isModalFinished() {
        return isWindowDisposed() || (!nativeVisible && !iconified);
    }

    /// Takes this window's modal blocker, both the framework one and the native flag.
    ///
    /// A window shown with a modality type set blocks exactly as one shown through
    /// `#showModal()` does; the only difference between them is that showModal() also
    /// parks the caller. Acquiring here rather than only there is what makes the
    /// framework's input blocking agree with the platform's own modal state.
    ///
    /// The two always move together and exactly once, because a port may implement
    /// the native flag by disabling another window -- Win32 does -- and an unbalanced
    /// pair leaves that window disabled for good.
    private void acquireModal() {
        if (modalRegistered || modalityType == MODALITY_NONE || nativePeer == null) {
            return;
        }
        modalRegistered = true;
        Desktop.getInstance().pushModalWindow(this);
        manager().setModal(nativePeer, true,
                modalityType == MODALITY_APPLICATION, ownerPeer());
    }

    /// The native peer of the window this one blocks, or null when it blocks the
    /// application's main window. A port implements modality by disabling that
    /// window, so it has to be told which one.
    private Object ownerPeer() {
        return ownerWindow == null ? null : ownerWindow.asContainer().topLevelNativePeer();
    }

    /// Drops this window's modal blocker, both the framework one and the native flag.
    /// Called from `#showModal()` and from `#dispose()`, so a modal window released
    /// either way stops blocking -- a native modal on Windows disables the owner's
    /// HWND, and leaving that in place makes the application unusable.
    private void releaseModal() {
        if (!modalRegistered) {
            return;
        }
        modalRegistered = false;
        Desktop.getInstance().popModalWindow(this);
        if (nativePeer != null) {
            manager().setModal(nativePeer, false,
                    modalityType == MODALITY_APPLICATION, ownerPeer());
        }
    }

    /// Sets how this window blocks input to the others.
    ///
    /// #### Parameters
    ///
    /// - `type`: one of `#MODALITY_NONE`, `#MODALITY_WINDOW` or `#MODALITY_APPLICATION`
    public void setModalityType(int type) {
        // Released under the *old* scope before the type changes, because that is the
        // scope the port was told about. Releasing afterwards would hand the port the
        // new one and undo the wrong block -- on Windows, switching an application
        // modal to window modal would re-enable an owner rather than decrement the
        // main window's disable count, leaving the main window disabled for good.
        releaseModal();
        modalityType = type;
        // iconified counts as live here, exactly as it does in isModalFinished() and
        // in hideNotify(): a minimized window is still open and still modal. Testing
        // nativeVisible alone released the old blocker and never took the new one, and
        // showNotify() does not reacquire on restore -- so a modality change made
        // while minimized left the window visibly non-modal while getModalityType()
        // still reported the mode that was asked for.
        if (type != MODALITY_NONE && (nativeVisible || iconified)) {
            acquireModal();
        }
    }

    /// Returns how this window blocks input to the others.
    ///
    /// #### Returns
    ///
    /// the modality type
    public int getModalityType() {
        return modalityType;
    }

    /// Hides this window without destroying it, so it can be shown again.
    public void hide() {
        if (!Display.getInstance().isEdt()) {
            // Marshalled exactly as show() and dispose() are: changing visibility,
            // mutating the modal and paint registries and firing listeners from a
            // background thread would race the event dispatch thread while it is
            // painting this window or dispatching input to it.
            Display.getInstance().callSerially(new Runnable() {
                @Override
                public void run() {
                    hide();
                }
            });
            return;
        }
        // iconified counts as still shown here: the platform took the window off
        // screen, but the application asking to hide it still has to release the
        // native window, drop the modal blocker and unpark showModal().
        if (nativePeer != null && (nativeVisible || iconified)) {
            nativeVisible = false;
            iconified = false;
            // A key handler can hide its own window, and the window stays registered
            // while hidden -- so a repeat or long press armed by the press that got
            // us here would go on firing into a component tree the user cannot see,
            // and keep the event dispatch thread awake. The key-up may never arrive
            // either, once the native window has lost focus.
            cancelPendingInput();
            // A window the user can no longer reach must not go on blocking the ones
            // behind it. Without this a modal hidden through HIDE_ON_CLOSE stays at the
            // top of the modal stack -- and where the platform implements modality
            // natively, keeps the owner's native input disabled too.
            releaseModal();
            // A hidden window is not painted, so anything its components queue would
            // sit on its surface forever -- and hasPendingPaints() seeing that queue
            // keeps the event dispatch thread awake spinning on work it will never
            // drain. Marking the hierarchy invisible stops components enqueuing, and
            // clearing the surface drops whatever was queued before this call.
            setVisible(false);
            clearPaintSurface();
            manager().hide(nativePeer);
            fireWindowEvent(WindowEvent.Type.Hidden);
        }
    }

    /// Indicates whether this window is currently mapped on screen.
    ///
    /// #### Returns
    ///
    /// true if the window is showing
    public boolean isWindowShowing() {
        return nativeVisible && !disposing;
    }

    /// Destroys this window and releases the native window behind it. Calling this
    /// more than once is harmless.
    public void dispose() {
        if (disposing) {
            return;
        }
        if (!Display.getInstance().isEdt()) {
            // Marshalled exactly as show() is. Tearing the hierarchy down, firing the
            // window events and mutating the desktop and paint registries from a
            // background thread would race the event dispatch thread while it is
            // painting this very window or dispatching input to it.
            Display.getInstance().callSerially(new Runnable() {
                @Override
                public void run() {
                    dispose();
                }
            });
            return;
        }
        disposing = true;
        // Whether this dispose is the thing actually taking the window off screen.
        // hide() and activationFailed() each report Hidden themselves and leave the
        // window invisible, and a window may never have been shown at all -- so the
        // terminal Hidden below would either repeat a transition that already happened
        // or announce one that never did. Listeners persist geometry and run teardown
        // off that event, so a spurious one is not free.
        boolean wasOnScreen = nativeVisible || iconified;
        nativeVisible = false;
        // An owned window cannot outlive its owner: the platform would leave it open
        // with no owner behind it, and it would keep painting. Snapshot first -- each
        // dispose deregisters, which mutates the registry being walked.
        for (Window each : Desktop.getInstance().windowsOwnedBy(this)) {
            each.dispose();
        }
        releaseModal();
        Desktop.getInstance().deregisterWindow(this);
        Display.getInstance().windowDisposed(this);
        deinitializeImpl();
        if (currentInputDevice != null) {
            try {
                currentInputDevice.close();
            } catch (Exception err) {
                Log.e(err);
            }
            currentInputDevice = null;
        }
        // Same cleanup the hide and minimize paths owe: a window disposed mid-gesture
        // leaves a hidden drag component and a latched pressed component behind, and
        // windowDisposed below only forgets the framework's records.
        cancelPendingInput();
        // Before the native window is destroyed, not merely before the Java reference
        // is cleared: the terminal Hidden and Disposed events below report bounds, and
        // every port tears the slot down inside dispose() -- Win32 destroys it
        // synchronously through SendMessage, Linux waits for its destroy, Catalyst
        // memsets the slot -- so a read afterwards answers with zeros and leaves the
        // stale requested rectangle in place.
        rememberNativeBounds();
        if (nativePeer != null) {
            WindowManager wm = manager();
            wm.hide(nativePeer);
            wm.dispose(nativePeer);
        }
        // dropping the surface also drops anything queued on it, so a disposed
        // window cannot pin its component tree
        if (paintSurface != null) {
            paintSurface.dispose();
            paintSurface = null;
        }
        nativePeer = null;
        windowGraphics = null;
        // showModal parks on Display.lock and wakes on this flag, so publish it under
        // the very monitor the waiter is blocked on
        synchronized (Display.lock) {
            disposed = true;
            Display.lock.notifyAll();
        }
        // Deliberately NOT closeListeners: those are the vetoable close *request*, and
        // a native close with DISPOSE_ON_CLOSE has already fired them once. Firing
        // them again would run a listener's save or cleanup work twice for one user
        // close, and a listener consuming the second event could not veto anything
        // because the window is already gone.
        if (wasOnScreen) {
            fireWindowEvent(WindowEvent.Type.Hidden);
        }
        fireWindowEvent(WindowEvent.Type.Disposed);
    }

    /// Indicates whether this window has been disposed.
    ///
    /// #### Returns
    ///
    /// true once `#dispose()` has run
    public boolean isWindowDisposed() {
        synchronized (Display.lock) {
            return disposed;
        }
    }

    /// Indicates whether this window has completed at least one paint cycle, and so
    /// whether its content -- rather than an empty surface -- is what a capture would
    /// return.
    ///
    /// A window's raster exists from the moment it is shown, so capturing before the
    /// first paint yields a blank frame of the right size rather than a failure. Test
    /// and tooling code that wants the content should wait on this.
    ///
    /// #### Returns
    ///
    /// true once the window has painted
    public boolean hasPaintedOnce() {
        return paintedOnce;
    }

    /// Invoked by the framework once a paint cycle for this window has completed.
    void markPainted() {
        paintedOnce = true;
    }

    /// Written by the paint loop and read by whatever is waiting for content, both on
    /// the event dispatch thread, so no cross thread publication is involved.
    private boolean paintedOnce;

    /// Captures this window's current contents.
    ///
    /// The ordinary `Display#screenshot(com.codename1.util.SuccessCallback)` can only
    /// see the application's main surface, so a window has to be captured through the
    /// window manager instead. This is what the windowed screenshot tests use.
    ///
    /// #### Returns
    ///
    /// an image of the window, or null when the port cannot capture one
    public Image capture() {
        if (disposing || nativePeer == null) {
            return null;
        }
        Object nativeImage = manager().capture(nativePeer);
        if (nativeImage != null) {
            return Image.createImage(nativeImage);
        }
        // A port that cannot read its own window back still owes a capture, so render
        // the hierarchy again at the window's current size. This is the same content
        // the window is showing rather than a readback of the pixels on screen, so a
        // port that can read back should -- that is the version that would also catch
        // the window and its raster disagreeing.
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return null;
        }
        Image img = Image.createImage(w, h);
        paintComponent(img.getGraphics(), true);
        return img;
    }

    /// Sets what happens when the user closes this window through the platform's own
    /// close control.
    ///
    /// #### Parameters
    ///
    /// - `op`: one of `#DISPOSE_ON_CLOSE`, `#HIDE_ON_CLOSE` or `#DO_NOTHING_ON_CLOSE`
    public void setCloseOperation(int op) {
        closeOperation = op;
    }

    /// Returns what happens when the user closes this window.
    ///
    /// #### Returns
    ///
    /// the close operation
    public int getCloseOperation() {
        return closeOperation;
    }

    /// Invoked by the framework when the user activates the platform's close control.
    void closeRequested() {
        ActionEvent evt = new ActionEvent(this);
        closeListeners.fireActionEvent(evt);
        if (evt.isConsumed()) {
            return;
        }
        switch (closeOperation) {
            case HIDE_ON_CLOSE:
                hide();
                break;
            case DO_NOTHING_ON_CLOSE:
                break;
            default:
                dispose();
                break;
        }
    }

    /// Sets the top level that owns this window. An owned window stays above its
    /// owner and is disposed with it.
    ///
    /// The name avoids `setOwner`, which `Component` already uses for an unrelated
    /// hit testing mechanism.
    ///
    /// #### Parameters
    ///
    /// - `owner`: the owning top level
    public void setOwnerWindow(TopLevelContainer owner) {
        if (nativePeer != null) {
            // The native ownership relation is established when the window is created
            // -- the owner HWND on Windows, the transient parent on GTK, the owner
            // passed to the JDialog on Java SE -- and none of those can be re-pointed
            // afterwards. Silently keeping the old one while this field claimed
            // otherwise would also strand a modal blocker on the previous owner, since
            // that is the window the port was told to disable.
            throw new IllegalStateException(
                    "the owner has to be set before the window is shown");
        }
        // A cycle here is not caught anywhere downstream: show() creates an unshown
        // owner's native window first, so a window that owns itself -- directly or
        // round a longer chain -- recurses through show() until the stack runs out,
        // before either peer exists. A StackOverflowError names none of the windows
        // involved, so reject the relation at the point it is described.
        TopLevelContainer probe = owner;
        while (probe instanceof Window) {
            if (probe == this) { //NOPMD CompareObjectsWithEquals
                throw new IllegalArgumentException(
                        "a window cannot own itself, directly or through its owner chain");
            }
            probe = ((Window) probe).ownerWindow;
        }
        this.ownerWindow = owner;
    }

    /// Returns the top level that owns this window.
    ///
    /// #### Returns
    ///
    /// the owner, or null when the window is unowned
    public TopLevelContainer getOwnerWindow() {
        return ownerWindow;
    }

    // ---- listeners -----------------------------------------------------------------

    /// {@inheritDoc}
    @Override
    public void addShowListener(ActionListener l) {
        showListeners.addListener(l);
    }

    /// {@inheritDoc}
    @Override
    public void removeShowListener(ActionListener l) {
        showListeners.removeListener(l);
    }

    /// {@inheritDoc}
    @Override
    public void addSizeChangedListener(ActionListener l) {
        sizeChangedListeners.addListener(l);
    }

    /// {@inheritDoc}
    @Override
    public void removeSizeChangedListener(ActionListener l) {
        sizeChangedListeners.removeListener(l);
    }

    /// Adds a listener notified when the user tries to close this window. Consuming
    /// the event vetoes the close.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener to add
    public void addCloseListener(ActionListener l) {
        closeListeners.addListener(l);
    }

    /// Removes a previously added close listener.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener to remove
    public void removeCloseListener(ActionListener l) {
        closeListeners.removeListener(l);
    }

    /// Adds a listener notified when this window is shown, hidden, moved or resized.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener to add
    public void addWindowListener(ActionListener l) {
        windowListeners.addListener(l);
    }

    /// Removes a previously added window listener.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener to remove
    public void removeWindowListener(ActionListener l) {
        windowListeners.removeListener(l);
    }

    private void fireWindowEvent(WindowEvent.Type type) {
        WindowEvent evt = new WindowEvent(this, type, getWindowBounds());
        windowListeners.fireActionEvent(evt);
        Desktop.getInstance().fireWindowEvent(evt);
    }

    // ---- painting and layout ------------------------------------------------------------

    /// {@inheritDoc}
    ///
    /// Routes the repaint into this window's own dirty queue rather than the main
    /// surface's, which is what keeps two windows from repainting each other.
    @Override
    void repaint(Component cmp) {
        if (getParent() != null) {
            super.repaint(cmp);
            return;
        }
        // An elevated component's shadow is drawn by its surface's elevated pane, not
        // by the component, and the shadow is larger than the component. Queueing the
        // component alone left the old shadow pixels on screen until something else
        // happened to repaint the surface. Form.repaint(Component) redirects the same
        // way; this is the window's copy of it.
        if (cmp.hasElevation()) {
            Container surface = cmp.findSurface();
            if (surface != null) {
                surface.repaint(cmp.getAbsoluteX() + cmp.calculateShadowOffsetX(24),
                        cmp.getAbsoluteY() + cmp.calculateShadowOffsetY(24),
                        cmp.calculateShadowWidth(24), cmp.calculateShadowHeight(24));
                return;
            }
        }
        // nativeVisible as well as isVisible(): a minimized window keeps its
        // hierarchy visible on purpose, so a model update or explicit repaint would
        // otherwise queue paint work that can never drain -- paintOpenWindows skips
        // a window that is not showing while hasPendingPaints() still counts its
        // queue, which spins the event dispatch thread until the window is restored.
        // Nothing is lost: showNotify repaints in full on restore.
        if (isVisible() && nativeVisible && paintSurface != null) {
            paintSurface.repaint(cmp);
        }
    }

    /// {@inheritDoc}
    @Override
    public void paint(Graphics g) {
        // internalPaintImpl has already painted the background by the time it invokes
        // this, so painting it again ran a custom background painter twice per frame
        // and composited a translucent one on top of itself. Form carries the same
        // guard for the same reason.
        if (!inInternalPaint) {
            paintComponentBackground(g);
        }
        super.paint(g);
    }

    @Override
    void internalPaintImpl(Graphics g, boolean paintIntersects) {
        inInternalPaint = true;
        super.internalPaintImpl(g, paintIntersects);
        inInternalPaint = false;
    }

    /// {@inheritDoc}
    @Override
    void paintGlassImpl(Graphics g) {
        if (getParent() != null) {
            super.paintGlassImpl(g);
            return;
        }
        if (glassPane != null) {
            int tx = g.getTranslateX();
            int ty = g.getTranslateY();
            g.translate(-tx, -ty);
            glassPane.paint(g, getBounds());
            g.translate(tx, ty);
        }
    }

    /// {@inheritDoc}
    @Override
    public int getSideGap() {
        if (getParent() == null) {
            return 0;
        }
        return super.getSideGap();
    }

    /// {@inheritDoc}
    @Override
    void sizeChangedInternal(int w, int h) {
        // Deliberately no clamp against the minimum size here. That minimum is native
        // geometry, including the platform's chrome, while these are the content
        // dimensions -- so comparing them mixes two coordinate spaces, and on a
        // decorated window it laid the hierarchy out larger than the canvas it is
        // drawn into, clipping controls and putting hit testing out of step with what
        // is on screen. The constraint belongs to the platform, which applies it to
        // the frame it owns; every desktop port implements it.
        int oldWidth = getWidth();
        int oldHeight = getHeight();
        setSize(new Dimension(w, h));
        setShouldCalcPreferredSize(true);
        if (windowLayeredPane != null) {
            windowLayeredPane.setWidth(w);
            windowLayeredPane.setHeight(h);
            // Its layout is disabled, so its layers do not follow it by themselves.
            java.util.List<Component> layers = windowLayeredPane.getChildrenAsList(true);
            int layerCount = layers.size();
            for (int iter = 0; iter < layerCount; iter++) {
                Component layer = layers.get(iter);
                layer.setWidth(w);
                layer.setHeight(h);
            }
        }
        doLayout();
        if (oldWidth != w || oldHeight != h) {
            // Anything already queued was computed against the old geometry, and a
            // port that reallocates its buffer on resize would paint those stale
            // rectangles into a fresh one -- leaving the rest of the new, larger
            // surface unpainted. Drop them and repaint the whole window instead.
            clearPaintSurface();
            // The frames painted so far were painted at the old size, so anything
            // waiting on hasPaintedOnce() has to wait again rather than capture a
            // surface that is half old content and half unpainted.
            paintedOnce = false;
            // As in moved(): keep the fallback geometry current so a window resized by
            // the user still reports its real size once the peer is gone.
            rememberNativeBounds();
            sizeChangedListeners.fireActionEvent(new ActionEvent(this, w, h));
            fireWindowEvent(WindowEvent.Type.Resized);
        }
        repaint();
    }

    /// {@inheritDoc}
    ///
    /// Matches `Form`: once the content pane has been wrapped in a layered pane the
    /// wrapper is the pane, since the content is no longer a direct child. The whole
    /// window overlay is deliberately not returned here -- see
    /// `#getActualPane(int, int)`.
    @Override
    Container getActualPane() {
        if (layeredPane != null) {
            return layeredPane.getParent();
        }
        return contentPane;
    }

    /// The pane a pointer at the given point should be dispatched into.
    ///
    /// The whole window overlay covers the window, so making it the hit testing root
    /// whenever it exists would swallow every click -- including over empty parts of
    /// it -- and leave the content and the title unresponsive for as long as anything
    /// had ever installed a layer. `Form` solves this by consulting the overlay only
    /// where it has something interactive, and this does the same.
    private Container getActualPane(int x, int y) {
        if (windowLayeredPane != null && windowLayeredPane.getResponderAt(x, y) != null) {
            return windowLayeredPane;
        }
        return getActualPane();
    }

    // ---- pointer dispatch ---------------------------------------------------------

    /// {@inheritDoc}
    ///
    /// A `Container` has no hit testing of its own -- `Form` does that work itself --
    /// so a `Window` has to as well, or a press would never reach the component under
    /// it. This is the same walk `Form` performs, without the menu bar special case a
    /// window has no equivalent of.
    @Override
    public void pointerPressed(int x, int y) {
        // A secondary (right / stylus barrel) press is a context menu request first,
        // exactly as on a Form. Without this a right click in a window never reached
        // the component's context menu listener, and an unconsumed right press could
        // then activate the component as an ordinary click.
        if (Display.getInstance().getPointerButton() == PointerEvent.BUTTON_SECONDARY) {
            Component ctxCmp = resolveComponentAt(x, y);
            if (ctxCmp != null && ctxCmp.fireContextMenu(x, y)) {
                return;
            }
        }
        // Surfaced at the top level so addStylusListener fires regardless of which
        // component is under the pen, exactly as Form does it.
        if (Display.getInstance().isStylusPointer()) {
            Component stylusCmp = resolveComponentAt(x, y);
            if (stylusCmp != null) {
                stylusCmp.fireStylusEvent(ActionEvent.Type.PointerPressed, x, y);
            }
        }
        // Listeners registered on the window itself can consume the event. They run
        // *after* the context menu and stylus dispatches above, which is Form's
        // order: a consuming pressed listener must not be able to suppress a right
        // click's context menu or a pen's stylus event.
        // Without this block at all, addPointerPressedListener on a Window never
        // fired -- and material pull to refresh broke with it, since Component
        // installs its refresh listeners on the top level.
        // Recorded before the listeners run, which is Form's order and matters for the
        // same reason the framework records its own press before dispatching: a pressed
        // listener can enter a nested event loop -- showModal() does -- and the matching
        // physical release is then processed inside it. With the handle created
        // afterwards that nested release found no gesture to clear, and this method then
        // installed a fresh press whose release had already happened, leaving the
        // component latched until some later gesture freed it.
        initialPressX = x;
        initialPressY = y;
        currentPointerPress = new Object();
        dragged = null;
        if (pointerPressedListeners != null && pointerPressedListeners.hasListeners()) {
            ActionEvent e = new ActionEvent(this, ActionEvent.Type.PointerPressed, x, y);
            pointerPressedListeners.fireActionEvent(e);
            if (e.isConsumed()) {
                return;
            }
        }
        // A press dismisses any tooltip so it cannot linger over a drag image or be
        // stranded when the gesture rebuilds the UI, as on a Form.
        if (TooltipManager.getInstance() != null) {
            TooltipManager.getInstance().clearTooltip();
        }
        Component cmp = resolveComponentAt(x, y);
        // Gated exactly as Form.pointerPressed gates it. Many components -- Button
        // among them -- override pointerPressed without checking isEnabled
        // themselves, relying on the top level never to call them, so dispatching
        // unconditionally left a disabled button entering its pressed state and
        // firing its action on release. Leaving pressedCmp null for a disabled
        // component also keeps the drag and release paths off it, since both start
        // from pressedCmp.
        if (cmp != null && isCurrentlyScrolling(cmp)) {
            // A press landing on a container that is still gliding stops the scroll and
            // hands the gesture to the user, which is what Form.resumeDragAfterScrolling
            // does (issue #2352). Stopping the motion and returning was only half of it:
            // pressedCmp stayed null, so every drag packet in the same physical gesture
            // had no target and the user could not take the scroll over without lifting
            // and pressing again.
            cancelScrolling(cmp);
            cmp.initDragAndDrop(x, y);
            // The component the scroll is handed to, so the rest of this physical
            // gesture has a target. Form gets there differently -- its drag path
            // re-resolves the component under the pointer when it has no pressed one --
            // but this window's drag path dispatches through pressedCmp, and giving it
            // the same fallback changed routing for every gesture, not just this one.
            pressedCmp = cmp;
            pointerPressedAgainDuringDrag = true;
            // Re-entered through this window rather than Display.pointerDragged(), which
            // Form uses: that one is the main surface's path and would deliver the drag
            // to the current Form instead of here.
            Desktop.getInstance().windowPointerDragged(getWindowId(),
                    new int[] { x }, new int[] { y });
            return;
        }
        if (cmp != null && cmp.isEnabled()) {
            pressedCmp = cmp;
            // Drag and drop has to be primed on the press, as Form does in every one
            // of its dispatch branches: Component.pointerDragged checks
            // dragAndDropInitialized and silently does nothing without it, so a
            // draggable component simply could not be dragged inside a window.
            cmp.initDragAndDrop(x, y);
            if (!cmp.isDragAndDropInitialized()) {
                Container draggableCnt = cmp.getParent();
                while (draggableCnt != null && !draggableCnt.isDraggable()) {
                    draggableCnt = draggableCnt.getParent();
                }
                if (draggableCnt != null && draggableCnt.isDraggable()
                        && !(draggableCnt instanceof TopLevelContainer)) {
                    draggableCnt.initDragAndDrop(x, y);
                }
            }
            LeadUtil.pointerPressed(cmp, x, y);
            // Not while a wheel gesture is being synthesized: dragWheelStep disables
            // only the deepest hit component, so a focusable lead parent resolved
            // here would still take focus and merely scrolling over a lead-based
            // control would steal the keyboard. Form and LeadUtil both guard on this.
            if (cmp.isFocusable() && !Display.impl.isScrollWheeling()) {
                setFocused(cmp);
            }
            tactileTouchVibe(x, y, cmp);
        } else {
            pressedCmp = null;
        }
    }

    /// {@inheritDoc}
    @Override
    public void pointerDragged(int x, int y) {
        if (Display.getInstance().isStylusPointer()) {
            Component stylusCmp = resolveComponentAt(x, y);
            if (stylusCmp != null) {
                stylusCmp.fireStylusEvent(ActionEvent.Type.PointerDrag, x, y);
            }
        }
        // Read and cleared here, exactly as Form does: the flag describes the drag that
        // took a momentum scroll over, and leaving it set would tell every later drag in
        // the session that it too continued out of a glide.
        boolean pressedDuringDrag = pointerPressedAgainDuringDrag;
        pointerPressedAgainDuringDrag = false;
        if (pointerDraggedListeners != null && pointerDraggedListeners.hasListeners()) {
            ActionEvent e = new ActionEvent(this, ActionEvent.Type.PointerDrag, x, y);
            e.setPointerPressedDuringDrag(pressedDuringDrag);
            pointerDraggedListeners.fireActionEvent(e);
            if (e.isConsumed()) {
                return;
            }
        }
        autoRelease(x, y);
        Component target = dragged != null ? dragged : pressedCmp;
        if (target != null) {
            LeadUtil.pointerDragged(target, x, y);
        }
    }

    /// {@inheritDoc}
    ///
    /// The multi pointer form, which is how a pinch reaches the component under the
    /// fingers. Without it `Component`'s version runs instead: it tests the pinch on
    /// the window itself and then collapses the event to a single coordinate, so the
    /// pressed child gets an ordinary one-finger drag and never its `pinch` callbacks.
    @Override
    public void pointerDragged(int[] x, int[] y) {
        // The same listener block the scalar overload runs. Adding it there only
        // meant a gesture stopped notifying window listeners the moment it became
        // multi touch, which is where pull to refresh loses its updates.
        if (pointerDraggedListeners != null && pointerDraggedListeners.hasListeners()) {
            ActionEvent e = new ActionEvent(this, ActionEvent.Type.PointerDrag, x[0], y[0]);
            // Reported but not cleared here, which is what Form's multi-pointer path
            // does -- the scalar path above owns the reset.
            e.setPointerPressedDuringDrag(pointerPressedAgainDuringDrag);
            pointerDraggedListeners.fireActionEvent(e);
            if (e.isConsumed()) {
                return;
            }
        }
        autoRelease(x[0], y[0]);
        Component target = dragged != null ? dragged : pressedCmp;
        if (target != null) {
            LeadUtil.pointerDragged(target, x, y);
        }
    }


    /// {@inheritDoc}
    @Override
    public void pointerReleased(int x, int y) {
        if (Display.getInstance().isStylusPointer()) {
            Component stylusCmp = resolveComponentAt(x, y);
            if (stylusCmp != null) {
                stylusCmp.fireStylusEvent(ActionEvent.Type.PointerReleased, x, y);
            }
        }
        // The token identifying *this* gesture. A release handler may enter
        // invokeAndBlock, whose nested event loop can dispatch a fresh press in this
        // same window before the handler returns; tearing down unconditionally then
        // erases the replacement gesture's target rather than this one's.
        final Object releasing = currentPointerPress;
        // Captured before the listeners run, not after. A listener can enter
        // invokeAndBlock, whose nested loop dispatches a fresh press in this window
        // and replaces these fields; resolving the target afterwards released the
        // *replacement* gesture's component, activating it with no native release of
        // its own.
        final Component releasingDragged = dragged;
        final Component releasingPressed = pressedCmp;
        if (pointerReleasedListeners != null && pointerReleasedListeners.hasListeners()) {
            ActionEvent e = new ActionEvent(this, ActionEvent.Type.PointerReleased, x, y);
            pointerReleasedListeners.fireActionEvent(e);
            if (e.isConsumed()) {
                // A drag that was actually activated still has to be finished, or the
                // component stays hidden and the drop never runs -- Form does the
                // same on its consumed path.
                if (releasingDragged != null && releasingDragged.isDragAndDropInitialized()) {
                    LeadUtil.dragFinished(releasingDragged, x, y);
                }
                // Still cleared: the gesture is over regardless of who handled it,
                // and leaving these set would strand the next press.
                endGesture(releasing);
                return;
            }
        }
        Component target = releasingDragged != null ? releasingDragged : releasingPressed;
        if (target != null) {
            if (releasingDragged != null && releasingDragged.isDragAndDropInitialized()) {
                // An activated drag ends with dragFinished, not pointerReleased.
                // Component hides the component when the drag activates and only
                // dragFinishedImpl restores it, clears the top level's dragged
                // component and runs the drop callbacks -- so releasing through the
                // ordinary path left the component invisible and the drop unfinished.
                LeadUtil.dragFinished(releasingDragged, x, y);
            } else {
                LeadUtil.pointerReleased(target, x, y);
            }
        }
        endGesture(releasing);
    }

    /// Clears the pressed state for the gesture identified by `token`, and only that
    /// gesture. A newer press installed during nested dispatch carries a different
    /// token and is left alone.
    private void endGesture(Object token) {
        if (currentPointerPress != token) { //NOPMD CompareObjectsWithEquals
            return;
        }
        pressedCmp = null;
        dragged = null;
        currentPointerPress = null;
    }

    /// Cancels a press once the pointer leaves the pressed component's release
    /// radius, the same way `Form` does it.
    ///
    /// Without this a button pressed in a window, dragged outside it and released
    /// still fired its action: the window kept forwarding to the pressed component
    /// and nothing ever cancelled the press. The list this consumes was here from
    /// the start but nothing filled it, because `Button` registered through
    /// `Component#getComponentForm()`, which is null inside a window.
    private void autoRelease(int x, int y) {
        if (componentsAwaitingRelease != null && componentsAwaitingRelease.size() == 1) {
            // special case allowing drag within a button
            Component atXY = LeadUtil.leadParentImpl(getComponentAt(x, y));
            Component pendingC = componentsAwaitingRelease.get(0);
            if (pendingC != null) {
                pendingC = LeadUtil.leadParentImpl(pendingC);
            }
            Component pendingCLead = LeadUtil.leadComponentImpl(pendingC);
            if (atXY != pendingC) { //NOPMD CompareObjectsWithEquals
                if (pendingCLead instanceof ReleasableComponent) {
                    ReleasableComponent rc = (ReleasableComponent) pendingCLead;
                    int relRadius = rc.getReleaseRadius();
                    if (relRadius > 0) {
                        Rectangle r = new Rectangle(
                                pendingC.getAbsoluteX() - relRadius,
                                pendingC.getAbsoluteY() - relRadius,
                                pendingC.getWidth() + relRadius * 2,
                                pendingC.getHeight() + relRadius * 2
                        );
                        if (!r.contains(x, y)) {
                            componentsAwaitingRelease = null;
                            LeadUtil.dragInitiated(pendingC);
                        }
                        return;
                    }
                    componentsAwaitingRelease = null;
                    LeadUtil.dragInitiated(pendingC);
                }
            } else if (pendingCLead instanceof ReleasableComponent
                    && ((ReleasableComponent) pendingCLead).isAutoRelease()) {
                componentsAwaitingRelease = null;
                LeadUtil.dragInitiated(pendingC);
            }
        }
    }

    /// {@inheritDoc}
    ///
    /// The keyboard counterpart of `#longPointerPress(int, int)`, and broken the same
    /// way: `Display` dispatches a long key press to the top level, `Component`'s
    /// implementation is empty, so holding a key inside a window reached nothing.
    /// Found by checking what else shares that dispatch site rather than waiting for
    /// it to be reported.
    @Override
    protected void longKeyPress(int keyCode) {
        if (focused != null && focused.getTopLevelContainer() == this) { //NOPMD CompareObjectsWithEquals
            focused.longKeyPress(keyCode);
        }
    }

    /// {@inheritDoc}
    ///
    /// `Component`'s implementation only fires listeners attached to this window, so
    /// without this a long press on a button inside a window reached nothing --
    /// neither the component nor its context menu.
    @Override
    public void longPointerPress(int x, int y) {
        // Listeners registered on the window itself run first and can consume the
        // gesture, the same order Form uses. Forwarding to the child without this
        // silently dropped every addLongPressListener attached to the window.
        if (longPressListeners != null && longPressListeners.hasListeners()) {
            ActionEvent ev = new ActionEvent(this, ActionEvent.Type.LongPointerPress, x, y);
            longPressListeners.fireActionEvent(ev);
            if (ev.isConsumed()) {
                return;
            }
        }
        // A long press is the touch equivalent of a right click, so it is a context
        // menu request next, exactly as on a Form.
        Component ctxCmp = resolveComponentAt(x, y);
        if (ctxCmp != null && ctxCmp.fireContextMenu(x, y)) {
            return;
        }
        Component target = pressedCmp != null ? pressedCmp : focused;
        if (target != null && target.contains(x, y)
                && target.getTopLevelContainer() == this) { //NOPMD CompareObjectsWithEquals
            LeadUtil.longPointerPress(target, x, y);
        }
    }

    /// This window's key-repeat and long-press timers. Fields, for the same reason the
    /// gesture state is: a window that goes away takes them with it, and nothing has to
    /// remember to hand a slot back.
    private boolean keyRepeatArmed;
    private boolean keyLongPressArmed;
    private int keyRepeatValue;
    private long keyRepeatNext;
    private long keyLongPressStart;
    private boolean longPointerArmed;
    private int longPointerX;
    private int longPointerY;
    private long longPointerStart;

    /// The container in this window that accepted the current press, so its release
    /// reaches the same place. A field rather than an entry in a table keyed by window,
    /// for the same reason as everything else here.
    private Container pointerPressTarget;

    void rememberPointerPress(Container target) {
        pointerPressTarget = target;
    }

    /// Returns the pending press target and clears it, so a release consumes it.
    Container takePointerPressTarget() {
        Container out = pointerPressTarget;
        pointerPressTarget = null;
        return out;
    }

    boolean hasPointerPressTarget() {
        return pointerPressTarget != null;
    }

    /// Arms key repeat and long key press for a press this window accepted.
    void chargeKeyRepeat(int keyCode, boolean armed, long now, long firstRepeatAt) {
        keyRepeatArmed = armed;
        keyLongPressArmed = armed;
        keyRepeatValue = keyCode;
        keyLongPressStart = now;
        keyRepeatNext = firstRepeatAt;
    }

    /// Cancels repeat for one key code, used when that key's release arrived
    /// somewhere else.
    void cancelKeyRepeatForCode(int keyCode) {
        if (keyRepeatValue == keyCode) {
            cancelKeyRepeat();
        }
    }

    void cancelKeyRepeat() {
        keyRepeatArmed = false;
        keyLongPressArmed = false;
    }

    boolean hasKeyRepeatArmed() {
        return keyRepeatArmed || keyLongPressArmed;
    }

    /// True while both are still pending, which is what tells the event loop it may
    /// not go to sleep yet.
    boolean hasKeyRepeatAndLongPressArmed() {
        return keyRepeatArmed && keyLongPressArmed;
    }

    /// Arms the long pointer press for a press this window accepted.
    void chargeLongPointerPress(int x, int y) {
        longPointerArmed = true;
        longPointerX = x;
        longPointerY = y;
        longPointerStart = System.currentTimeMillis();
    }

    void cancelLongPointerPress() {
        longPointerArmed = false;
    }

    boolean hasLongPointerArmed() {
        return longPointerArmed;
    }

    /// Fires whichever of this window's timers are due. Called once per paint pass
    /// from the event loop, with the loop's clock so every surface agrees on the time.
    void serviceInputTimers(long now, int longPressInterval) {
        if (!nativeVisible || disposing) {
            return;
        }
        // A window the user cannot reach must not go on receiving the repeats and long
        // presses a still-held key armed before it was blocked. The routing helper this
        // replaced made the same check, and dropping it here would have delivered input
        // to a window sitting behind a modal.
        if (Desktop.getInstance().isWindowInputBlocked(getWindowId())) {
            return;
        }
        if (keyRepeatArmed && keyRepeatNext <= now) {
            keyRepeated(keyRepeatValue);
            int keyRepeatNextIntervalTime = 10;
            keyRepeatNext = now + keyRepeatNextIntervalTime;
        }
        if (keyLongPressArmed && longPressInterval <= now - keyLongPressStart) {
            keyLongPressArmed = false;
            longKeyPress(keyRepeatValue);
        }
        if (longPointerArmed && longPressInterval <= now - longPointerStart) {
            longPointerArmed = false;
            longPointerPress(longPointerX, longPointerY);
        }
    }

    /// The recent pointer path in this window, created on first use because a window
    /// that never sees a drag has no reason to hold the ring.
    PointerDragHistory dragHistory() {
        if (dragHistory == null) {
            dragHistory = Display.getInstance().newDragHistory();
        }
        return dragHistory;
    }

    /// Records a position in the current gesture.
    void recordDrag(int x, int y, int timestamp) {
        dragHistory().record(x, y, timestamp);
    }

    /// Forgets the previous gesture so a new press does not fling with its speed.
    void resetDragHistory() {
        if (dragHistory != null) {
            dragHistory.reset();
        }
    }

    /// Whether a drag happened during the gesture currently in this window.
    boolean hasDragOccured() {
        return dragOccured;
    }

    void setDragOccured(boolean value) {
        dragOccured = value;
    }

    /// The fling speed of the gesture in this window. Named apart from
    /// Component.getDragSpeed(boolean), which this class inherits and which means the
    /// speed of the component's own drag.
    float windowDragSpeed(boolean yAxis) {
        return dragHistory().speed(Display.impl, yAxis);
    }

    /// Records a press that has not been released or dragged, with the point it went
    /// down at, for the pureTouch selection test.
    void setSelectionPressed(boolean value, int x, int y) {
        selectionPressed = value;
        if (value) {
            selectionPressedX = x;
            selectionPressedY = y;
        }
    }

    /// Whether this window holds a press that should still show selection on the
    /// given component. The component is tested against this window's own press
    /// coordinates -- window coordinates are window relative, so another window's
    /// pointer position is not merely the wrong point but a point in a different
    /// space.
    @Override
    boolean showsSelectionFor(Component c) {
        return selectionPressed && c.contains(selectionPressedX, selectionPressedY);
    }

    /// Whether this window holds a press at all, which is what the component-less
    /// selection query asks.
    boolean hasSelectionPressed() {
        return selectionPressed;
    }

    /// Stops any glide still running in the pressed component's ancestors, so the
    /// press can take the scroll over. The counterpart of `isCurrentlyScrolling`,
    /// ported from `Form.cancelScrolling`.
    private void cancelScrolling(Component cmp) {
        Container parent = cmp.getParent();
        while (parent != null) {
            if (parent.draggedMotionX != null || parent.draggedMotionY != null) {
                parent.draggedMotionX = null;
                parent.draggedMotionY = null;
            }
            parent = parent.getParent();
        }
    }

    /// Set when a press landed on a still-gliding container and took the scroll over.
    /// Reported to drag listeners the way `Form` reports it, so a listener can tell a
    /// fresh drag from one that continued out of a momentum scroll.
    private boolean pointerPressedAgainDuringDrag;

    /// Whether any ancestor of the pressed component is still gliding from a
    /// previous drag. Ported from `Form`, which swallows the press in that case so a
    /// tap stops the scroll instead of starting an interaction.
    private boolean isCurrentlyScrolling(Component cmp) {
        Container parent = cmp.getParent();
        while (parent != null) {
            if (parent.draggedMotionX != null || parent.draggedMotionY != null) {
                return true;
            }
            parent = parent.getParent();
        }
        return false;
    }

    /// Haptic feedback for a press on a component that asks for it, as `Form` does.
    private void tactileTouchVibe(int x, int y, Component cmp) {
        if (tactileTouchDuration < 0) {
            // Resolved on first use rather than in a constructor: the look and feel a
            // window should follow is the one in effect when it is interacted with.
            tactileTouchDuration = getUIManager().getLookAndFeel().getTactileTouchDuration();
        }
        if (tactileTouchDuration > 0 && cmp.isTactileTouch(x, y)) {
            Display.getInstance().vibrate(tactileTouchDuration);
        }
    }

    /// Ends every gesture in flight because the window has left the user's reach.
    ///
    /// Called from every path that does that -- `#hide()`, a native minimize through
    /// `#hideNotify()`, `#dispose()`, and losing focus to another application --
    /// rather than from whichever one was last reported. Losing focus counts: the
    /// key-up goes to whoever has focus now, so a held key would otherwise repeat
    /// here forever. A window that goes away mid-gesture leaves three kinds of state
    /// behind, and all three have to be undone together:
    ///
    /// an activated drag and drop, whose component `Component` has already hidden and
    /// which only `dragFinishedImpl` restores; a pressed component, latched in its
    /// pressed state with no release coming; and the framework's own recorded targets
    /// and timers, which otherwise keep firing into a tree nobody can see.
    void cancelPendingInput() {
        if (dragged != null && dragged.isDragAndDropInitialized()) {
            // Finished outside the window so no drop target is found: the user never
            // completed the drag, the window simply went away. This still restores
            // the component's visibility and clears the drag flags.
            LeadUtil.dragFinished(dragged, -1, -1);
        }
        // dragInitiated is the existing "ended without completing" primitive -- it
        // resets the pressed state without firing the action.
        if (pressedCmp != null) {
            LeadUtil.dragInitiated(pressedCmp);
        }
        if (focused != null && focused != pressedCmp) { //NOPMD CompareObjectsWithEquals
            LeadUtil.dragInitiated(focused);
        }
        pressedCmp = null;
        dragged = null;
        currentPointerPress = null;
        Display.getInstance().windowInputCancelled(this);
    }

    private Component resolveComponentAt(int x, int y) {
        Component cmp = getActualPane(x, y).getComponentAt(x, y);
        while (cmp != null && cmp.isIgnorePointerEvents()) {
            cmp = cmp.getParent();
        }
        if (cmp == null) {
            return null;
        }
        return LeadUtil.leadParentImpl(cmp);
    }

    /// {@inheritDoc}
    @Override
    Object getCurrentPointerPress() {
        return currentPointerPress;
    }

    /// {@inheritDoc}
    @Override
    int getInitialPressX() {
        return initialPressX;
    }

    /// {@inheritDoc}
    @Override
    int getInitialPressY() {
        return initialPressY;
    }

    /// {@inheritDoc}
    @Override
    Component getDraggedComponent() {
        return dragged;
    }

    /// {@inheritDoc}
    @Override
    void setDraggedComponent(Component dragged) {
        this.dragged = LeadUtil.leadParentImpl(dragged);
    }

    private void initFocused() {
        if (focused == null) {
            Component first = getActualPane().findFirstFocusable();
            if (first != null) {
                setFocused(first);
            }
        }
    }

    /// {@inheritDoc}
    ///
    /// `Component`'s implementation is empty, so without this a window would receive
    /// hover events and drop them: no tooltips, and no hover state on the components
    /// under the pointer.
    @Override
    public void pointerHover(int[] x, int[] y) {
        if (dragged != null) {
            LeadUtil.pointerHover(dragged, x, y);
            return;
        }
        Component cmp = resolveComponentAt(x[0], y[0]);
        if (cmp != null) {
            LeadUtil.pointerHover(cmp, x, y);
            // Deliberately no TooltipManager call. It schedules only when
            // getComponentForm() is non-null and displays through InteractionDialog on
            // the current form, so from a window it would either do nothing or put the
            // tooltip on the main window. It is listed with the other form-coupled
            // overlays in the developer guide rather than half-wired here.
        }
    }

    /// {@inheritDoc}
    @Override
    public void pointerHoverReleased(int[] x, int[] y) {
        Component cmp = resolveComponentAt(x[0], y[0]);
        if (cmp != null) {
            LeadUtil.pointerHoverReleased(cmp, x, y);
        }
    }

    /// {@inheritDoc}
    @Override
    public void pointerHoverPressed(int[] x, int[] y) {
        Component cmp = resolveComponentAt(x[0], y[0]);
        if (cmp != null) {
            LeadUtil.pointerHoverPressed(cmp, x, y);
        }
    }

    // ---- native visibility ----------------------------------------------------------

    /// {@inheritDoc}
    ///
    /// The platform telling us the window is no longer on screen -- minimized, or
    /// hidden by the window manager. `Container`'s implementation is inert, which
    /// would leave the window counted as visible: it would keep being painted, its
    /// animations would keep the event dispatch thread awake, and a minimized window
    /// that animates would spin the thread forever.
    @Override
    void hideNotify() {
        super.hideNotify();
        if (nativeVisible) {
            nativeVisible = false;
            // Native minimization arrives here rather than through hide(), and the
            // window stays registered either way, so the same cleanup is owed: a held
            // key would otherwise keep repeating into the hidden tree, and the
            // platform may never deliver the release once focus is gone.
            cancelPendingInput();
            // Recorded separately from an explicit hide(): a minimized window is still
            // open, and still modal if it was, so this must not read as "the modal is
            // over" -- see isModalFinished().
            iconified = true;
            // Nothing paints a window that is not on screen, so anything queued on its
            // surface would sit there keeping hasPendingPaints() true.
            clearPaintSurface();
            fireWindowEvent(WindowEvent.Type.Minimized);
        }
    }

    /// {@inheritDoc}
    ///
    /// The platform telling us the window is on screen again. Painting resumes from
    /// here, so the window is repainted in full rather than waiting for something to
    /// dirty it.
    @Override
    void showNotify() {
        super.showNotify();
        if (!nativeVisible && !disposing && nativePeer != null) {
            nativeVisible = true;
            iconified = false;
            fireWindowEvent(WindowEvent.Type.Restored);
            repaint();
            Display.getInstance().wakeEdt();
        }
    }

    /// {@inheritDoc}
    @Override
    public <C extends Component> void addComponentAwaitingRelease(C c) {
        if (componentsAwaitingRelease == null) {
            componentsAwaitingRelease = new ArrayList<Component>();
        }
        componentsAwaitingRelease.add(c);
    }

    /// {@inheritDoc}
    @Override
    public <C extends Component> void removeComponentAwaitingRelease(C c) {
        if (componentsAwaitingRelease != null) {
            componentsAwaitingRelease.remove(c);
        }
    }

    /// {@inheritDoc}
    @Override
    public void clearComponentsAwaitingRelease() {
        if (componentsAwaitingRelease != null) {
            componentsAwaitingRelease.clear();
        }
    }

    // ---- key dispatch --------------------------------------------------------------

    /// {@inheritDoc}
    ///
    /// A window dispatches keys itself, exactly as `Form` does. Inheriting
    /// `Container`'s handler instead only forwards to a lead component, so the focused
    /// component would never see a key, arrow traversal would not work and nothing
    /// registered through `#addKeyListener(int, ActionListener)` would ever fire.
    ///
    /// This is the same shape as `Form#keyPressed(int)` minus the menu bar, which a
    /// window does not have: commands reach the desktop menu instead.
    @Override
    public void keyPressed(int keyCode) {
        int game = Display.getInstance().getGameAction(keyCode);
        if (focused != null) {
            if (focused.isEnabled()) {
                focused.keyPressed(keyCode);
            }
            if (focused.handlesInput()) {
                return;
            }
            if (focused.getTopLevelContainer() == this) { //NOPMD CompareObjectsWithEquals
                updateWindowFocus(game);
            } else {
                focused = null;
                initFocused();
            }
        } else {
            initFocused();
            if (focused == null) {
                getContentPane().moveScrollTowards(game, null);
            }
        }
    }

    /// {@inheritDoc}
    @Override
    public void keyReleased(int keyCode) {
        if (focused != null && focused.getTopLevelContainer() == this //NOPMD CompareObjectsWithEquals
                && focused.isEnabled()) {
            focused.keyReleased(keyCode);
        }
        fireKeyEvent(keyCode);
    }

    /// {@inheritDoc}
    @Override
    public void keyRepeated(int keyCode) {
        if (focused == null) {
            keyPressed(keyCode);
            keyReleased(keyCode);
            return;
        }
        if (focused.isEnabled()) {
            focused.keyRepeated(keyCode);
        }
        int game = Display.getInstance().getGameAction(keyCode);
        if (!focused.handlesInput()
                && (game == Display.GAME_DOWN || game == Display.GAME_UP
                    || game == Display.GAME_LEFT || game == Display.GAME_RIGHT)) {
            keyPressed(keyCode);
            keyReleased(keyCode);
        }
    }

    private void fireKeyEvent(int keyCode) {
        if (keyListeners == null) {
            return;
        }
        ArrayList<ActionListener> listeners = keyListeners.get(Integer.valueOf(keyCode));
        if (listeners == null) {
            return;
        }
        ActionEvent evt = new ActionEvent(this, keyCode);
        int len = listeners.size();
        for (int iter = 0; iter < len; iter++) {
            listeners.get(iter).actionPerformed(evt);
            if (evt.isConsumed()) {
                return;
            }
        }
    }

    /// The component below the focus owner, honouring an explicit
    /// `Component#getNextFocusDown()` before scanning by position, exactly as a `Form`
    /// does.
    ///
    /// `Container`'s versions of these four answer null, which is right for an
    /// ordinary container and wrong for a top level: every arrow key in a window
    /// resolved through them and moved focus nowhere, so a window could not be
    /// navigated from the keyboard at all.
    @Override
    Component findNextFocusDown() {
        if (focused != null) {
            if (focused.getNextFocusDown() != null) {
                return focused.getNextFocusDown();
            }
            return findNextFocusVertical(true);
        }
        return null;
    }

    /// The counterpart to `#findNextFocusDown()`.
    @Override
    Component findNextFocusUp() {
        if (focused != null) {
            if (focused.getNextFocusUp() != null) {
                return focused.getNextFocusUp();
            }
            return findNextFocusVertical(false);
        }
        return null;
    }

    /// The component right of the focus owner, honouring an explicit
    /// `Component#getNextFocusRight()` before scanning by position.
    @Override
    Component findNextFocusRight() {
        if (focused != null) {
            if (focused.getNextFocusRight() != null) {
                return focused.getNextFocusRight();
            }
            return findNextFocusHorizontal(true);
        }
        return null;
    }

    /// The counterpart to `#findNextFocusRight()`.
    @Override
    Component findNextFocusLeft() {
        if (focused != null) {
            if (focused.getNextFocusLeft() != null) {
                return focused.getNextFocusLeft();
            }
            return findNextFocusHorizontal(false);
        }
        return null;
    }

    /// Scans this window for the next focusable component above or below the focus
    /// owner, through the shared traversal `Form` uses.
    ///
    /// The layered pane is searched first when there is one, so a component in an
    /// overlay takes focus before one underneath it, and `isCyclicFocus()` wraps to
    /// the far end when nothing lies in the direction asked for.
    ///
    /// #### Parameters
    ///
    /// - `down`: true for the next component below, false for above
    ///
    /// #### Returns
    ///
    /// the next focusable component, or null
    private Component findNextFocusVertical(boolean down) {
        Component c;
        if (layeredPane != null) {
            c = TopLevelSupport.findNextFocusVertical(focused, null, layeredPane, down);
            if (c != null) {
                return c;
            }
        }
        Container actual = getActualPane();
        c = TopLevelSupport.findNextFocusVertical(focused, null, actual, down);
        if (c != null) {
            return c;
        }
        if (isCyclicFocus()) {
            c = TopLevelSupport.findNextFocusVertical(focused, null, actual, !down);
            if (c != null) {
                Component current = TopLevelSupport.findNextFocusVertical(c, null, actual, !down);
                while (current != null) {
                    c = current;
                    current = TopLevelSupport.findNextFocusVertical(c, null, actual, !down);
                }
                return c;
            }
        }
        return null;
    }

    /// The horizontal counterpart to `#findNextFocusVertical(boolean)`.
    ///
    /// #### Parameters
    ///
    /// - `right`: true for the next component to the right, false for the left
    ///
    /// #### Returns
    ///
    /// the next focusable component, or null
    private Component findNextFocusHorizontal(boolean right) {
        Component c;
        if (layeredPane != null) {
            c = TopLevelSupport.findNextFocusHorizontal(focused, null, layeredPane, right);
            if (c != null) {
                return c;
            }
        }
        Container actual = getActualPane();
        c = TopLevelSupport.findNextFocusHorizontal(focused, null, actual, right);
        if (c != null) {
            return c;
        }
        if (isCyclicFocus()) {
            c = TopLevelSupport.findNextFocusHorizontal(focused, null, actual, !right);
            if (c != null) {
                Component current = TopLevelSupport.findNextFocusHorizontal(c, null, actual, !right);
                while (current != null) {
                    c = current;
                    current = TopLevelSupport.findNextFocusHorizontal(c, null, actual, !right);
                }
                return c;
            }
        }
        return null;
    }

    /// Moves focus in the direction of an arrow key, mirroring `Form`'s traversal.
    private void updateWindowFocus(int gameAction) {
        Component next = null;
        switch (gameAction) {
            case Display.GAME_DOWN:
                next = findNextFocusDown();
                break;
            case Display.GAME_UP:
                next = findNextFocusUp();
                break;
            case Display.GAME_RIGHT:
                next = findNextFocusRight();
                break;
            case Display.GAME_LEFT:
                next = findNextFocusLeft();
                break;
            default:
                return;
        }
        if (next != null) {
            setFocused(next);
            scrollComponentToVisible(next);
        }
    }

    // ---- content delegation ------------------------------------------------------------

    /// {@inheritDoc}
    ///
    /// Adds to the content pane, mirroring `Form`, so `window.add(cmp)` means
    /// `window.getContentPane().add(cmp)`. Container's add() is final and routes
    /// through here, so overriding addComponent covers both.
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
    ///
    /// The indexed overloads need delegating too. They are separate methods rather
    /// than paths through the two above, so without these an indexed add put the
    /// component in the window root beside the title area and the content pane --
    /// where the root's own BorderLayout would place it, and where
    /// `#getContentPane()` cannot see it.
    @Override
    public void addComponent(int index, Component cmp) {
        contentPane.addComponent(index, cmp);
    }

    /// {@inheritDoc}
    @Override
    public void addComponent(int index, Object constraints, Component cmp) {
        contentPane.addComponent(index, constraints, cmp);
    }

    /// {@inheritDoc}
    @Override
    public void removeComponent(Component cmp) {
        contentPane.removeComponent(cmp);
    }

    /// {@inheritDoc}
    @Override
    public void removeAll() {
        contentPane.removeAll();
    }

    // The animation and replace family, delegated for the same reason add() is: the
    // application's components live in the content pane, so animating or searching the
    // window root would animate the title area along with them and look for children
    // that are not there. Form delegates every one of these.

    /// {@inheritDoc}
    @Override
    public int getComponentIndex(Component cmp) {
        return contentPane.getComponentIndex(cmp);
    }

    /// {@inheritDoc}
    @Override
    public void replace(Component current, Component next, Transition t) {
        contentPane.replace(current, next, t);
    }

    /// {@inheritDoc}
    @Override
    public void replaceAndWait(Component current, Component next, Transition t) {
        contentPane.replaceAndWait(current, next, t);
    }

    /// {@inheritDoc}
    @Override
    public void animateLayout(int duration) {
        contentPane.animateLayout(duration);
    }

    /// {@inheritDoc}
    @Override
    public void animateLayoutAndWait(int duration) {
        contentPane.animateLayoutAndWait(duration);
    }

    /// {@inheritDoc}
    @Override
    public void animateLayoutFade(int duration, int startingOpacity) {
        contentPane.animateLayoutFade(duration, startingOpacity);
    }

    /// {@inheritDoc}
    @Override
    public void animateLayoutFadeAndWait(int duration, int startingOpacity) {
        contentPane.animateLayoutFadeAndWait(duration, startingOpacity);
    }

    /// {@inheritDoc}
    @Override
    public void animateHierarchy(int duration) {
        contentPane.animateHierarchy(duration);
    }

    /// {@inheritDoc}
    @Override
    public void animateHierarchyAndWait(int duration) {
        contentPane.animateHierarchyAndWait(duration);
    }

    /// {@inheritDoc}
    @Override
    public void animateHierarchyFade(int duration, int startingOpacity) {
        contentPane.animateHierarchyFade(duration, startingOpacity);
    }

    /// {@inheritDoc}
    @Override
    public void animateHierarchyFadeAndWait(int duration, int startingOpacity) {
        contentPane.animateHierarchyFadeAndWait(duration, startingOpacity);
    }

    /// {@inheritDoc}
    @Override
    public void animateUnlayout(int duration, int opacity, Runnable callback) {
        contentPane.animateUnlayout(duration, opacity, callback);
    }

    /// {@inheritDoc}
    @Override
    public void animateUnlayoutAndWait(int duration, int opacity) {
        contentPane.animateUnlayoutAndWait(duration, opacity);
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

    /// {@inheritDoc}
    @Override
    public boolean isScrollable() {
        return contentPane.isScrollable();
    }

    /// {@inheritDoc}
    @Override
    public void setScrollable(boolean scrollable) {
        contentPane.setScrollable(scrollable);
    }

    // The rest of the scrolling surface, delegated for the same reason isScrollable is:
    // the content pane scrolls, not the window root, which is a fixed BorderLayout
    // holding the title area and the content. Without these, window.setScrollableY(true)
    // set the flag on the root -- where nothing reads it -- while the identical call on
    // a Form reached the content pane, so code moved from a Form to a Window silently
    // stopped scrolling.

    /// {@inheritDoc}
    ///
    /// Forwarded to the content pane as well as the window root: the application's
    /// layout runs in the content pane, so setting it on the root alone left
    /// directional layouts and alignment reversed while `isRTL()` reported true.
    @Override
    public void setRTL(boolean r) {
        super.setRTL(r);
        contentPane.setRTL(r);
    }

    /// {@inheritDoc}
    @Override
    public boolean isScrollableX() {
        return contentPane.isScrollableX();
    }

    /// {@inheritDoc}
    @Override
    public void setScrollableX(boolean scrollableX) {
        contentPane.setScrollableX(scrollableX);
    }

    /// {@inheritDoc}
    @Override
    public boolean isScrollableY() {
        return contentPane.isScrollableY();
    }

    /// {@inheritDoc}
    @Override
    public void setScrollableY(boolean scrollableY) {
        contentPane.setScrollableY(scrollableY);
    }

    /// {@inheritDoc}
    @Override
    public boolean isScrollVisible() {
        return contentPane.isScrollVisible();
    }

    /// {@inheritDoc}
    @Override
    public void setScrollVisible(boolean scrollVisible) {
        contentPane.setScrollVisible(scrollVisible);
    }

    /// {@inheritDoc}
    @Override
    public boolean isSmoothScrolling() {
        return contentPane.isSmoothScrolling();
    }

    /// {@inheritDoc}
    @Override
    public void setSmoothScrolling(boolean smoothScrolling) {
        // Null-checked as Form does: Component's constructor reaches this before the
        // content pane exists.
        if (contentPane != null) {
            contentPane.setSmoothScrolling(smoothScrolling);
        }
    }

    /// {@inheritDoc}
    @Override
    public int getScrollAnimationSpeed() {
        return contentPane.getScrollAnimationSpeed();
    }

    /// {@inheritDoc}
    @Override
    public void setScrollAnimationSpeed(int animationSpeed) {
        contentPane.setScrollAnimationSpeed(animationSpeed);
    }

    /// {@inheritDoc}
    @Override
    public boolean isAlwaysTensile() {
        return contentPane.isAlwaysTensile();
    }

    /// {@inheritDoc}
    @Override
    public void setAlwaysTensile(boolean alwaysTensile) {
        contentPane.setAlwaysTensile(alwaysTensile);
    }
}
