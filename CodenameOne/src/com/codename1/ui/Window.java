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
import com.codename1.ui.events.ActionEvent;
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
    private Object paintSurface;
    private Graphics windowGraphics;
    /// Set as soon as dispose() begins, so re-entering it is a no-op.
    private boolean disposing;
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

    private final Container contentPane;
    private final Container titleArea = new Container(new BorderLayout());
    private final Label title = new Label("", "Title");
    private Container layeredPane;
    private Container windowLayeredPane;
    private Painter glassPane;
    private Toolbar toolbar;

    private ArrayList<Component> componentsAwaitingRelease;
    private Component focused;
    private Component dragged;
    private Component pressedCmp;
    private Object currentPointerPress;
    private int initialPressX;
    private int initialPressY;
    private boolean cyclicFocus = true;

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
        titleArea.setUIID("TitleArea");
        titleArea.addComponent(BorderLayout.CENTER, this.title);
        super.addComponent(BorderLayout.NORTH, titleArea);
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

    Object getPaintSurface() {
        return paintSurface;
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
    public Container getTitleArea() {
        return titleArea;
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
                        Window.this.paint(g);
                        super.setVisible(true);
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
        return title.getText();
    }

    /// {@inheritDoc}
    ///
    /// Sets both the Codename One title component and the native window title.
    @Override
    public void setTitle(String title) {
        this.title.setText(title);
        pendingTitle = title;
        if (nativePeer != null) {
            manager().setTitle(nativePeer, title);
        }
    }

    /// {@inheritDoc}
    @Override
    public Toolbar getToolbar() {
        return toolbar;
    }

    /// {@inheritDoc}
    @Override
    public void setToolbar(Toolbar toolbar) {
        this.toolbar = toolbar;
        titleArea.removeAll();
        titleArea.setLayout(new BorderLayout());
        titleArea.addComponent(BorderLayout.CENTER, toolbar);
    }

    /// {@inheritDoc}
    @Override
    public void addCommand(Command cmd) {
        commands.add(cmd);
    }

    /// {@inheritDoc}
    @Override
    public void removeCommand(Command cmd) {
        commands.remove(cmd);
    }

    /// {@inheritDoc}
    @Override
    public void removeAllCommands() {
        commands.clear();
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
        return animMananger.isAnimating();
    }

    /// {@inheritDoc}
    @Override
    public void releaseAnimationLock() {
        animMananger.flushAnimation(null);
    }

    boolean hasAnimations() {
        return !animatableComponents.isEmpty()
                || !internalAnimatableComponents.isEmpty()
                || animMananger.isAnimating();
    }

    void repaintAnimations() {
        if (Display.getInstance().isEdt()) {
            loopAnimations(animatableComponents);
            loopAnimations(internalAnimatableComponents);
            animMananger.updateAnimations();
        }
    }

    private void loopAnimations(ArrayList<Animation> v) {
        // iterate by index and re-read the size: animate() may deregister itself
        for (int iter = 0; iter < v.size(); iter++) { // NOPMD ForLoopCanBeForeach
            Animation an = v.get(iter);
            if (an != null && an.animate()) {
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
            Display.impl.repaintWindow(paintSurface, a);
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
        return true;
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
        return false;
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
        if (toolbar != null && !decorated) {
            return Component.DRAG_REGION_LIKELY_DRAG_XY;
        }
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
    public void setResizable(boolean resizable) {
        this.resizable = resizable;
        if (nativePeer != null) {
            manager().setResizable(nativePeer, resizable);
        }
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
    public void setDecorated(boolean decorated) {
        this.decorated = decorated;
        if (nativePeer != null) {
            manager().setDecorated(nativePeer, decorated);
        }
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
    public void setAlwaysOnTop(boolean alwaysOnTop) {
        this.alwaysOnTop = alwaysOnTop;
        if (nativePeer != null) {
            manager().setAlwaysOnTop(nativePeer, alwaysOnTop);
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
    public void setUtilityWindow(boolean utility) {
        this.utilityWindow = utility;
        if (nativePeer != null) {
            manager().setUtilityWindow(nativePeer, utility);
        }
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
    public void setWindowIcon(Image icon) {
        this.windowIcon = icon;
        if (nativePeer != null) {
            manager().setIcon(nativePeer, icon);
        }
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

    private void setWindowBounds(int x, int y, int w, int h) {
        pendingX = x;
        pendingY = y;
        pendingPositionSet = true;
        pendingWidth = w;
        pendingHeight = h;
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
    public void setWindowSize(int width, int height) {
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
    public void setWindowLocation(int x, int y) {
        Rectangle current = getWindowBounds();
        setWindowBounds(x, y, current.getWidth(), current.getHeight());
    }

    /// Sets the smallest size the user may resize this window to.
    ///
    /// #### Parameters
    ///
    /// - `d`: the minimum size
    public void setMinimumWindowSize(Dimension d) {
        minimumWindowSize = d;
        if (nativePeer != null) {
            manager().setMinimumSize(nativePeer,
                    d == null ? 0 : d.getWidth(), d == null ? 0 : d.getHeight());
        }
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
    public void centerOn(TopLevelContainer other) {
        if (other instanceof Window) {
            Rectangle o = ((Window) other).getWindowBounds();
            Rectangle b = getWindowBounds();
            setWindowLocation(o.getX() + (o.getWidth() - b.getWidth()) / 2,
                    o.getY() + (o.getHeight() - b.getHeight()) / 2);
            return;
        }
        centerOnDesktop();
    }

    /// Minimizes this window.
    public void minimize() {
        if (nativePeer != null) {
            manager().minimize(nativePeer);
        }
    }

    /// Restores this window from a minimized state.
    public void restore() {
        if (nativePeer != null) {
            manager().restore(nativePeer);
        }
    }

    /// Toggles this window between maximized and its previous size.
    public void toggleMaximize() {
        if (nativePeer != null) {
            manager().toggleMaximize(nativePeer);
        }
    }

    /// Raises this window and gives it keyboard focus.
    public void requestWindowFocus() {
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
        Display.impl.clearPaintSurface(paintSurface);
        paintedOnce = false;
        repaint();
        return true;
    }

    /// Invoked by the framework when the platform reports that the user moved this
    /// window. Nothing needs re-laying out -- only the position changed -- so this
    /// just reports it.
    void moved() {
        fireWindowEvent(WindowEvent.Type.Moved);
    }

    /// Invoked by the framework when the platform reports that this window has moved
    /// to a monitor with different characteristics. Re-reads the scale and lays the
    /// hierarchy out again, since preferred sizes computed at the old scale are stale.
    void monitorChanged() {
        currentMonitor = Desktop.getInstance().getMonitorFor(this);
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
        WindowManager wm = manager();
        if (nativePeer == null) {
            if (ownerWindow instanceof Window && ((Window) ownerWindow).nativePeer == null) {
                // Showing a window whose owner has not been shown yet would create the
                // child against the wrong native owner, permanently: every port fixes
                // the relation at creation. Create the owner's native window first.
                ((Window) ownerWindow).show();
            }
            Object parentPeer = ownerWindow instanceof Window
                    ? ((Window) ownerWindow).nativePeer : null;
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
            windowGraphics = Display.getInstance().createWindowGraphics(this);
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
        nativeVisible = true;
        acquireModal();
        // Only meaningful when this window is *not* modal, which is why acquireModal()
        // above cannot cover it: a window shown while someone else's application modal
        // is up registers no blocker of its own, so no port ever hears about the new
        // peer. Ports enable a native window by default, leaving its title bar live --
        // focusable, movable, closable -- underneath a modal that is supposed to be
        // blocking it. Recomputed here, before the peer is mapped, so the window is
        // never briefly interactive.
        Display.getInstance().syncNativeModalBlocking();
        wm.show(nativePeer);
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
            releaseModal();
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
        Display.getInstance().pushModalWindow(this);
        manager().setModal(nativePeer, true,
                modalityType == MODALITY_APPLICATION, ownerPeer());
    }

    /// The native peer of the window this one blocks, or null when it blocks the
    /// application's main window. A port implements modality by disabling that
    /// window, so it has to be told which one.
    private Object ownerPeer() {
        return ownerWindow instanceof Window ? ((Window) ownerWindow).nativePeer : null;
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
        Display.getInstance().popModalWindow(this);
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
        if (type != MODALITY_NONE && nativeVisible) {
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
            Display.impl.clearPaintSurface(paintSurface);
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
        if (nativePeer != null) {
            WindowManager wm = manager();
            wm.hide(nativePeer);
            wm.dispose(nativePeer);
        }
        // dropping the surface also drops anything queued on it, so a disposed
        // window cannot pin its component tree
        Display.impl.disposePaintSurface(paintSurface);
        paintSurface = null;
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
        fireWindowEvent(WindowEvent.Type.Hidden);
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
        if (isVisible() && paintSurface != null) {
            Display.impl.repaintWindow(paintSurface, cmp);
        }
    }

    /// {@inheritDoc}
    @Override
    public void paint(Graphics g) {
        paintComponentBackground(g);
        super.paint(g);
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
            Display.impl.clearPaintSurface(paintSurface);
            // The frames painted so far were painted at the old size, so anything
            // waiting on hasPaintedOnce() has to wait again rather than capture a
            // surface that is half old content and half unpainted.
            paintedOnce = false;
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
        // The title area is a sibling of the content pane, not inside it, so a point
        // in the north region resolves to nothing without this -- and a Toolbar or a
        // button placed there, which is exactly how an undecorated window draws its
        // own chrome, would never receive a press.
        if (titleArea.contains(x, y) && titleArea.getComponentCount() > 0) {
            return titleArea;
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
        initialPressX = x;
        initialPressY = y;
        currentPointerPress = new Object();
        dragged = null;
        Component cmp = resolveComponentAt(x, y);
        // Gated exactly as Form.pointerPressed gates it. Many components -- Button
        // among them -- override pointerPressed without checking isEnabled
        // themselves, relying on the top level never to call them, so dispatching
        // unconditionally left a disabled button entering its pressed state and
        // firing its action on release. Leaving pressedCmp null for a disabled
        // component also keeps the drag and release paths off it, since both start
        // from pressedCmp.
        if (cmp != null && cmp.isEnabled()) {
            pressedCmp = cmp;
            LeadUtil.pointerPressed(cmp, x, y);
            if (cmp.isFocusable()) {
                setFocused(cmp);
            }
        } else {
            pressedCmp = null;
        }
    }

    /// {@inheritDoc}
    @Override
    public void pointerDragged(int x, int y) {
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
        autoRelease(x[0], y[0]);
        Component target = dragged != null ? dragged : pressedCmp;
        if (target != null) {
            LeadUtil.pointerDragged(target, x, y);
        }
    }


    /// {@inheritDoc}
    @Override
    public void pointerReleased(int x, int y) {
        Component target = dragged != null ? dragged : pressedCmp;
        if (target != null) {
            LeadUtil.pointerReleased(target, x, y);
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
            // Recorded separately from an explicit hide(): a minimized window is still
            // open, and still modal if it was, so this must not read as "the modal is
            // over" -- see isModalFinished().
            iconified = true;
            // Nothing paints a window that is not on screen, so anything queued on its
            // surface would sit there keeping hasPendingPaints() true.
            Display.impl.clearPaintSurface(paintSurface);
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
    @Override
    public void removeComponent(Component cmp) {
        contentPane.removeComponent(cmp);
    }

    /// {@inheritDoc}
    @Override
    public void removeAll() {
        contentPane.removeAll();
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
}
