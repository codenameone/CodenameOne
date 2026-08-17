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
    /// Published under Display.lock once teardown is complete; showModal waits on it.
    private volatile boolean disposed;
    private boolean nativeVisible;

    private final Container contentPane;
    private final Container titleArea = new Container(new BorderLayout());
    private final Label title = new Label("", "Title");
    private Container layeredPane;
    private Container windowLayeredPane;
    private Painter glassPane;
    private Toolbar toolbar;

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
    private int pendingX = -1;
    private int pendingY = -1;
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
        return TopLevelSupport.layeredPane(windowLayeredPane, c, top);
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
        if (!internalAnimatableComponents.contains(cmp)) {
            internalAnimatableComponents.add(cmp);
            repaint();
        }
    }

    /// {@inheritDoc}
    @Override
    void deregisterAnimatedInternal(Animation cmp) {
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
        for (int iter = 0; iter < v.size(); iter++) {
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
        Component old = this.focused;
        setFocusedInternal(focused);
        if (old != null) {
            old.repaint();
        }
        if (focused != null) {
            focused.repaint();
        }
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
            Object parentPeer = ownerWindow instanceof Window
                    ? ((Window) ownerWindow).nativePeer : null;
            nativePeer = wm.createWindow(windowId, pendingTitle, pendingX, pendingY,
                    pendingWidth, pendingHeight, decorated, resizable, parentPeer);
            paintSurface = Display.impl.createPaintSurface(nativePeer);
            windowGraphics = Display.getInstance().createWindowGraphics(this);
            if (windowIcon != null) {
                wm.setIcon(nativePeer, windowIcon);
            }
            if (alwaysOnTop) {
                wm.setAlwaysOnTop(nativePeer, true);
            }
            if (modalityType != MODALITY_NONE) {
                wm.setModal(nativePeer, true);
            }
        }
        Desktop.getInstance().registerWindow(this);
        setVisible(true);
        sizeChangedInternal(wm.getWidth(nativePeer), wm.getHeight(nativePeer));
        initFocused();
        nativeVisible = true;
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
        show();
        Display.getInstance().pushModalWindow(this);
        try {
            Display.getInstance().invokeAndBlock(new Runnable() {
                @Override
                public void run() {
                    while (!isWindowDisposed()) {
                        synchronized (Display.lock) {
                            if (isWindowDisposed()) {
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
            Display.getInstance().popModalWindow(this);
        }
    }

    /// Sets how this window blocks input to the others.
    ///
    /// #### Parameters
    ///
    /// - `type`: one of `#MODALITY_NONE`, `#MODALITY_WINDOW` or `#MODALITY_APPLICATION`
    public void setModalityType(int type) {
        modalityType = type;
        if (nativePeer != null) {
            manager().setModal(nativePeer, type != MODALITY_NONE);
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
        if (nativePeer != null && nativeVisible) {
            nativeVisible = false;
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
        disposing = true;
        nativeVisible = false;
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
        closeListeners.fireActionEvent(new ActionEvent(this));
        fireWindowEvent(WindowEvent.Type.Hidden);
    }

    /// Indicates whether this window has been disposed.
    ///
    /// #### Returns
    ///
    /// true once `#dispose()` has run
    public boolean isWindowDisposed() {
        return disposed;
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

    private volatile boolean paintedOnce;

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
        if (nativeImage == null) {
            return null;
        }
        return Image.createImage(nativeImage);
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
        int oldWidth = getWidth();
        int oldHeight = getHeight();
        setSize(new Dimension(w, h));
        setShouldCalcPreferredSize(true);
        if (windowLayeredPane != null) {
            windowLayeredPane.setWidth(w);
            windowLayeredPane.setHeight(h);
        }
        doLayout();
        if (oldWidth != w || oldHeight != h) {
            sizeChangedListeners.fireActionEvent(new ActionEvent(this, w, h));
            fireWindowEvent(WindowEvent.Type.Resized);
        }
        repaint();
    }

    /// {@inheritDoc}
    @Override
    Container getActualPane() {
        if (windowLayeredPane != null) {
            return windowLayeredPane;
        }
        return contentPane;
    }

    // ---- pointer dispatch ---------------------------------------------------------

    /// {@inheritDoc}
    ///
    /// A `Container` has no hit testing of its own -- `Form` does that work itself --
    /// so a `Window` has to as well, or a press would never reach the component under
    /// it. This is the same walk `Form` performs, without the title area and menu bar
    /// special cases a window has no equivalent of.
    @Override
    public void pointerPressed(int x, int y) {
        initialPressX = x;
        initialPressY = y;
        currentPointerPress = new Object();
        dragged = null;
        Component cmp = resolveComponentAt(x, y);
        pressedCmp = cmp;
        if (cmp != null) {
            LeadUtil.pointerPressed(cmp, x, y);
            if (cmp.isFocusable()) {
                setFocused(cmp);
            }
        }
    }

    /// {@inheritDoc}
    @Override
    public void pointerDragged(int x, int y) {
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

    private Component resolveComponentAt(int x, int y) {
        Component cmp = getActualPane().getComponentAt(x, y);
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
