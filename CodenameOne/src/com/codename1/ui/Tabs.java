/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.ui;

import com.codename1.ui.accessibility.AccessibilityManager;
import com.codename1.ui.animations.Motion;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.FocusListener;
import com.codename1.ui.events.SelectionListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.plaf.GlassLensBlend;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.plaf.VibrancyMatrix;
import com.codename1.ui.util.EventDispatcher;

/// A component that lets the user switch between a group of components by
/// clicking on a tab with a given title and/or icon.
///
/// Tabs/components are added to a `Tabs` object by using the
/// `addTab` and `insertTab` methods.
/// A tab is represented by an index corresponding
/// to the position it was added in, where the first tab has an index equal to 0
/// and the last tab has an index equal to the tab count minus 1.
///
/// The `Tabs` uses a `SingleSelectionModel`
/// to represent the set of tab indices and the currently selected index.
/// If the tab count is greater than 0, then there will always be a selected
/// index, which by default will be initialized to the first tab.
/// If the tab count is 0, then the selected index will be -1.
///
/// A simple `Tabs` sample looks a bit like this:
///
/// ```java
/// Form hi = new Form("Tabs", new BorderLayout());
///
/// Tabs t = new Tabs();
/// Style s = UIManager.getInstance().getComponentStyle("Tab");
/// FontImage icon1 = FontImage.createMaterial(FontImage.MATERIAL_QUESTION_ANSWER, s);
///
/// Container container1 = BoxLayout.encloseY(new Label("Label1"), new Label("Label2"));
/// t.addTab("Tab1", icon1, container1);
/// t.addTab("Tab2", new SpanLabel("Some text directly in the tab"));
///
/// hi.add(BorderLayout.CENTER, t);
/// ```
///
/// The `Tabs` allows swiping on the X-axis (by default) but also on the Y-axis:
/// ![Tabs swiping on the X-axis and Y-axis](https://www.codenameone.com/img/tabs-swipe-x-y-axis.gif)
///
/// ```java
/// Form hi = new Form("Test swipe on tabs", BorderLayout.absolute());
/// Tabs tabs = new Tabs();
/// tabs.addTab("First", new Label("First tab"));
/// tabs.addTab("Second", new Label("Second tab"));
///
/// ButtonGroup btnGroup = new ButtonGroup();
/// RadioButton swipeXBtn = RadioButton.createToggle("Swipe on X-Axis", btnGroup);
/// RadioButton swipeYBtn = RadioButton.createToggle("Swipe on Y-Axis", btnGroup);
/// btnGroup.setSelected(swipeXBtn);
///
/// swipeXBtn.addActionListener(l -> tabs.setSwipeOnXAxis(true));
/// swipeYBtn.addActionListener(l -> tabs.setSwipeOnXAxis(false));
///
/// hi.add(BorderLayout.NORTH, GridLayout.encloseIn(2, swipeXBtn, swipeYBtn));
/// hi.add(BorderLayout.CENTER, tabs);
/// ```
///
/// @author Chen Fishbein
public class Tabs extends Container {
    private final Container contentPane = new Container(new TabsLayout());
    private final Container tabsContainer;
    /// Optional wrapper around `tabsContainer` whose only job is to absorb the
    /// safe-area inset when the theme opts out of internal safe-area padding
    /// via the `tabsSafeAreaBool` constant. With the wrapper present, the
    /// pill (`tabsContainer`) draws tightly while the wrapper's padding keeps
    /// the pill clear of the home indicator. `null` for legacy themes that
    /// keep the safe-area inset on the pill itself.
    private Container tabsContainerHost;
    private final ButtonGroup radioGroup = new ButtonGroup();
    private final ActionListener press;
    private final ActionListener drag;
    private final ActionListener release;
    private final TabFocusListener focusListener;
    private boolean eagerSwipeMode;
    /// Where the tabs are placed.
    private int tabPlacement;
    private Component selectedTab;
    private boolean swipeActivated = true;
    private boolean swipeOnXAxis = true;
    private Motion slideToDestMotion;
    private int initialX = -1;
    private int initialY = -1;
    private int lastX = -1;
    private int lastY = -1;
    private boolean dragStarted = false;
    private int activeComponent = 0;
    private int active = 0;
    private EventDispatcher focusListeners;
    private EventDispatcher selectionListener;
    private boolean tabsFillRows;
    private boolean tabsGridLayout;
    // Equal-width tab cells (each = row width / tab count), like a native UITabBar --
    // so a longer label can't widen its cell and shove the others over. Opt in via
    // `tabsEqualWidthBool`. Uses a non-scrolling GridLayout so cells fill the row
    // width evenly (plain grid sizes to the widest cell and overflows).
    private boolean tabsEqualWidth;
    private int textPosition = -1;
    private boolean changeTabOnFocus;
    private boolean changeTabContainerStyleOnFocus;
    private int tabsGap = 0;
    private Style originalTabsContainerUnselected;
    private Style originalTabsContainerSelected;
    private String tabUIID = "Tab";
    private boolean animateTabSelection = true;
    // A flag that is used internally to temporarily override the output of the
    // shouldBlockSideSwipe method, so that we don't block our own side swipes.
    private boolean doNotBlockSideSwipe;
    private boolean blockSwipe;
    private boolean riskySwipe;

    // ---- Animated tab indicator (Material 3 "NavigationBar" style) ----
    // Off by default. Enable with #setAnimatedIndicator(true) or the
    // `tabsAnimatedIndicatorBool` theme constant. When on, a coloured
    // underline drawn under the currently-selected tab tweens its
    // x/width between the previous and new tabs on selection change.
    private boolean animatedIndicator;
    // iOS 26 sliding selection capsule: a single Liquid Glass blob behind the
    // selected tab that slides between tabs on selection change (reuses the
    // indicator motion below). Opt in via `tabsSelectionCapsuleBool`.
    private boolean selectionCapsule;
    private int animatedIndicatorDurationMs = 200;
    private int animatedIndicatorThicknessMm = 1; // 1mm-tall underline
    private Motion indicatorAnimMotion;
    // The tab index the indicator morph is currently travelling TO. Lets a re-entrant
    // setSelectedIndex (e.g. the content-slide finishing) recognise that a morph to the
    // same tab is already in flight and NOT restart it mid-travel.
    private int indicatorTargetIndex = -1;
    // TEST-ONLY: when >=0, paintSelectionCapsule renders the morph at this fixed
    // 0..120 progress instead of the live motion (for the JavaSE capture probe).
    private int morphTestValue = -1;
    // Tab bounds at the start of the indicator animation.
    private int indicatorFromX;
    private int indicatorFromW;
    // Tab bounds at the end of the indicator animation.
    private int indicatorToX;
    private int indicatorToW;

    // ---- iOS 27 Liquid Glass selection motion (tabsMorphPreset "ios27") ----
    // The motion is MEASURED from UIKit, see TabGlassMotion. paintGlassTabs draws
    // the whole bar -- glass, grey platter, tab content and lens -- from one
    // time-based frame, and reveals an ACCENT copy of the tabs through the lens
    // (Button.GLASS_PAINT_SELECTED), so the selection colour travels with the glass
    // the way UIKit composites its SelectedContentView under a lens-shaped hole.
    private long glassMotionStart = -1;
    private int glassTargetIndex = -1;
    // Lens centre and width at the start and end of the motion, in the inner-x
    // space capsuleCellBounds answers in. Floats, so an interrupted motion restarts
    // from exactly where the lens is drawn.
    private float glassFromCenter;
    private float glassFromW;
    private float glassToCenter;
    private float glassToW;
    // Where the last press landed, tabsContainer-relative (-1 = unknown); the touch
    // glow expands from there.
    private int glassPressX = -1;
    private int glassPressY = -1;
    private int glassTouchX = -1;
    private int glassTouchY = -1;
    // The live press driving the motion (hold, scrub, release), or null for a
    // selection made in code, which plays the captured tap motion as is.
    private TabGlassGesture glassGesture;
    // Where the press driving glassGesture landed, tabsContainer-relative x.
    private int glassGesturePressX = -1;
    // TEST-ONLY: when >= 0 the glass motion is frozen this many ms after its start.
    private int glassTestElapsedMs = -1;
    private static final int GLASS_TEST_MS_PER_PERCENT = 8;
    private static final long GLASS_MAX_PRESS_MS = 60000;

    /// Creates an empty `TabbedPane` with a default
    /// tab placement of `Component.TOP`.
    public Tabs() {
        this(-1);
    }


    /// Creates an empty `TabbedPane` with the specified tab placement
    /// of either: `Component.TOP`, `Component.BOTTOM`,
    /// `Component.LEFT`, or `Component.RIGHT`.
    ///
    /// #### Parameters
    ///
    /// - `tabP`: the placement for the tabs relative to the content
    public Tabs(int tabP) {
        super(new BorderLayout());
        focusListener = new TabFocusListener();
        contentPane.setUIID("TabbedPane");
        super.addComponent(BorderLayout.CENTER, contentPane);
        // Custom Container subclass that lets us paint the animated indicator
        // on top of children (over the tab buttons' selected-state background)
        // when `animatedIndicator` is on. When the feature is off, the
        // override is a no-op extra call and visually indistinguishable from
        // a plain Container.
        tabsContainer = new Container() {
            @Override
            void paintBackgroundLayer(Graphics g) {
                if (isGlassMotion()) {
                    paintGlassBarBackground(g);
                    return;
                }
                super.paintBackgroundLayer(g);
            }

            @Override
            public void paint(Graphics g) {
                if (isGlassMotion()) {
                    layoutForPaint();
                    paintGlassTabs(g);
                    paintBottomDivider(g);
                    return;
                }
                super.paint(g);
                // The iOS 26 selection "drop" is a LENS painted OVER the bar + the
                // (black) glyphs -- it magnifies, chromatically aberrates and
                // dark->accent tints the content beneath it, so the selected blue
                // exists only inside the drop. Painted AFTER super.paint for that.
                paintSelectionCapsule(g);
                paintBottomDivider(g);
                paintAnimatedIndicator(g);
            }
        };
        // tabsSafeAreaBool=true (default): legacy / flush-bar themes keep the
        // safe-area inset as PADDING on the pill itself - the bar's
        // background reaches the screen edge with tabs sitting above the
        // home indicator.
        //
        // tabsSafeAreaBool=false (modern floating pill): the safe-area inset
        // moves to a wrapper container so the pill draws tightly and is
        // pushed up away from the indicator without extending its own
        // background into the indicator zone.
        boolean tabsSafeAreaOnPill = getUIManager().isThemeConstant("tabsSafeAreaBool", true);
        tabsContainer.setSafeArea(tabsSafeAreaOnPill);
        tabsContainer.setUIID("TabsContainer");
        tabsContainer.setScrollVisible(false);
        if (tabsSafeAreaOnPill) {
            // Legacy / flush full-width bar: the background reaches the screen
            // edges, so the bar carries no margin.
            tabsContainer.getStyle().setMargin(0, 0, 0, 0);
        } else {
            // Modern floating glass pill: KEEP the theme's TabsContainer margin
            // (e.g. iOS-modern's 0.5mm/1mm) so the pill insets from the screen
            // edges and reads as a floating capsule rather than a full-width bar.
            // Forcing the margin to 0 here defeated the float. The host below is a
            // transparent spacer that only absorbs the safe-area inset; the pill
            // itself paints the glass, so the host must not tint behind it.
            tabsContainerHost = new Container(new GlassHostLayout());
            // Dedicated UIID so a theme can tune the host (e.g. a negative bottom
            // margin to pull the floating pill closer to the home indicator).
            // Defaults to transparent so only the pill paints the glass.
            tabsContainerHost.setUIID("TabsContainerHost");
            tabsContainerHost.getStyle().setBgTransparency(0);
            tabsContainerHost.setSafeArea(true);
            tabsContainerHost.add(BorderLayout.CENTER, tabsContainer);
        }
        if (tabP == -1) {
            // Honor the tabPlacementInt theme constant when no explicit
            // placement was requested. Reading the constant here (rather
            // than only in initLaf) guarantees the value is seen even
            // when initLaf runs polymorphically from Component()'s super
            // ctor - at that point the Tabs subclass fields haven't been
            // initialised yet and writes to them are brittle.
            int themePlacement = getUIManager().getThemeConstant("tabPlacementInt", -1);
            if (themePlacement != -1) {
                tabPlacement = themePlacement;
            }
            setTabPlacement(tabPlacement);
        } else {
            setTabPlacement(tabP);
        }
        press = new SwipeListener(SwipeListener.PRESS);
        drag = new SwipeListener(SwipeListener.DRAG);
        release = new SwipeListener(SwipeListener.RELEASE);
        setUIIDFinal("Tabs");
        // Opt-in animated indicator (Material 3 NavigationBar style).
        animatedIndicator = getUIManager().isThemeConstant("tabsAnimatedIndicatorBool", false);
        selectionCapsule = getUIManager().isThemeConstant("tabsSelectionCapsuleBool", false);
        animatedIndicatorDurationMs = getUIManager().getThemeConstant("tabsAnimatedIndicatorDurationInt", 200);
        BorderLayout bd = (BorderLayout) super.getLayout();
        if (bd != null) {
            if (UIManager.getInstance().isThemeConstant("tabsOnTopBool", false)) {
                bd.setCenterBehavior(BorderLayout.CENTER_BEHAVIOR_TOTAL_BELOW);
            } else {
                bd.setCenterBehavior(BorderLayout.CENTER_BEHAVIOR_SCALE);
            }
        }

    }

    @Override
    protected boolean shouldBlockSideSwipe() {
        if (doNotBlockSideSwipe) {
            return false;
        }
        return isSwipeActivated();
    }

    private void checkTabsCanBeSeen() {
        if (UIManager.getInstance().isThemeConstant("tabsOnTopBool", false)) {
            // The whole bar's height -- the floating pill PLUS the safe-area host
            // wrapper when present -- so the last scrollable rows clear the bar
            // (the content scrolls UNDER the translucent pill, native style).
            Component bar = tabsContainerHost != null ? tabsContainerHost : tabsContainer;
            int barH = bar.getPreferredH();
            for (int iter = 0; iter < getTabCount(); iter++) {
                Component c = getTabComponentAt(iter);
                if (c.isScrollableY()) {
                    if (c.getStyle().getPaddingBottom() < barH) {
                        c.getStyle().setPadding(BOTTOM, barH);
                    }
                }
            }
        }
    }

    /// {@inheritDoc}
    @Override
    protected void initLaf(UIManager manager) {
        super.initLaf(manager);
        int tabPlace = manager.getThemeConstant("tabPlacementInt", -1);
        tabsFillRows = manager.isThemeConstant("tabsFillRowsBool", false);
        tabsGridLayout = manager.isThemeConstant("tabsGridBool", false);
        tabsEqualWidth = manager.isThemeConstant("tabsEqualWidthBool", false);
        changeTabOnFocus = manager.isThemeConstant("changeTabOnFocusBool", false);
        BorderLayout bd = (BorderLayout) super.getLayout();
        if (bd != null) {
            if (manager.isThemeConstant("tabsOnTopBool", false)) {
                if (bd.getCenterBehavior() != BorderLayout.CENTER_BEHAVIOR_TOTAL_BELOW) {
                    bd.setCenterBehavior(BorderLayout.CENTER_BEHAVIOR_TOTAL_BELOW);
                    checkTabsCanBeSeen();
                }
            } else {
                bd.setCenterBehavior(BorderLayout.CENTER_BEHAVIOR_SCALE);
            }
        }
        changeTabContainerStyleOnFocus = manager.isThemeConstant("changeTabContainerStyleOnFocusBool", false);
        // tabPlacementInt lets a theme dictate whether tabs live at TOP /
        // BOTTOM / LEFT / RIGHT. initLaf is called both during the
        // Component() super() chain (before the Tabs ctor body has
        // allocated tabsContainer) and again later when styles refresh.
        // First call: tabsContainer is null, so just stash the value in
        // the field; the ctor's setTabPlacement call at the end will
        // pick it up and move the (then-allocated) container.
        // Second call and beyond: container exists, so reparent it.
        if (tabPlace != -1) {
            if (tabsContainer == null) {
                tabPlacement = tabPlace;
            } else if (tabPlace != tabPlacement) {
                setTabPlacement(tabPlace);
            }
        }
    }

    /// {@inheritDoc}
    @Override
    void initComponentImpl() {
        super.initComponentImpl();
        TopLevelContainer frm = getTopLevelContainer();
        if (frm != null) {
            TopLevelSupport.registerAnimatedInternal(frm, this);
            if (changeTabContainerStyleOnFocus && Display.getInstance().shouldRenderSelection()) {
                Component f = frm.getFocused();
                if (f != null && f.getParent() == tabsContainer) { //NOPMD CompareObjectsWithEquals
                    initTabsContainerStyle();
                    tabsContainer.setUnselectedStyle(originalTabsContainerSelected);
                    tabsContainer.repaint();
                }
            }
        }
    }

    /// {@inheritDoc}
    @Override
    public void refreshTheme(boolean merge) {
        super.refreshTheme(merge);
        originalTabsContainerSelected = null;
        originalTabsContainerUnselected = null;
    }

    /// {@inheritDoc}
    @Override
    protected void deinitialize() {
        TopLevelContainer form = getTopLevelContainer();
        if (form != null) {
            form.asContainer().removePointerPressedListener(press);
            form.asContainer().removePointerReleasedListener(release);
            form.asContainer().removePointerDraggedListener(drag);
        }
        super.deinitialize();
    }

    /// {@inheritDoc}
    @Override
    protected void initComponent() {
        super.initComponent();
        TopLevelContainer form = getTopLevelContainer();
        if (form != null && swipeActivated) {
            form.asContainer().addPointerPressedListener(press);
            form.asContainer().addPointerReleasedListener(release);
            form.asContainer().addPointerDraggedListener(drag);
        }
    }

    /// {@inheritDoc}
    @Override
    public boolean animate() {
        boolean b = super.animate();
        if (glassMotionStart >= 0) {
            // One repaint of the bar per tick while the measured motion runs, then ONE
            // more once it is over so the resting frame replaces the last animated one.
            // Not `b = true`: that asks the form to repaint the whole Tabs as well, so
            // every frame of the motion was painted twice.
            tabsContainer.repaint();
            long elapsed = System.currentTimeMillis() - glassMotionStart;
            if (glassGesture != null && glassGesture.getUpS() < 0 && elapsed > GLASS_MAX_PRESS_MS) {
                // The release never reached us (listeners detached mid-press):
                // let the lens settle rather than hold it lifted forever.
                glassGesture.up(elapsed / 1000f);
            }
            boolean over = glassGesture != null ? glassGesture.isFinished(elapsed / 1000f)
                    : elapsed >= TabGlassMotion.durationMs();
            if (over) {
                glassMotionStart = -1;
                glassTargetIndex = -1;
                glassGesture = null;
                deregisterAnimatedInternal();
            }
        }
        // Indicator-animation tick: redraw the tab bar each frame while
        // the motion is in flight. We let the existing super.animate /
        // slide motion control deregistration; the indicator motion is
        // cheap enough to run alongside without coordination.
        if (indicatorAnimMotion != null) {
            if (indicatorAnimMotion.isFinished()) {
                indicatorAnimMotion = null;
                indicatorTargetIndex = -1;
                // Paint ONE more frame now that the morph is over so the SETTLED capsule
                // (animating==false -> a clean, un-stretched pill at the target) replaces the
                // last in-flight frame. Without this the final animated frame -- often still
                // elongated/overshot, especially at a low frame rate -- lingered until the
                // next unrelated repaint ("settle is stretched too far right, recovers on
                // repaint").
                tabsContainer.repaint();
                b = true;
                // The morph drove the registration (possibly past a shorter content-slide);
                // release it now that it is done (no-op if a slide is still in flight).
                deregisterAnimatedInternal();
            } else {
                tabsContainer.repaint();
                b = true;
            }
        }
        if (slideToDestMotion != null) {
            if (swipeOnXAxis) {
                int motionX = slideToDestMotion.getValue();
                final int size = contentPane.getComponentCount();
                int tabWidth = contentPane.getWidth() - tabsGap * 2;
                for (int i = 0; i < size; i++) {
                    int xOffset;
                    if (isRTL()) {
                        xOffset = (size - i) * tabWidth;
                        xOffset -= ((size - active) * tabWidth);
                    } else {
                        xOffset = i * tabWidth;
                        xOffset -= (active * tabWidth);
                    }
                    xOffset += motionX;
                    Component component = contentPane.getComponentAt(i);
                    component.setX(xOffset);
                }
            } else {
                int motionY = slideToDestMotion.getValue();
                final int size = contentPane.getComponentCount();
                int tabHeight = contentPane.getHeight() - tabsGap * 2;
                for (int i = 0; i < size; i++) {
                    int yOffset;
                    yOffset = i * tabHeight;
                    yOffset -= (active * tabHeight);
                    yOffset += motionY;
                    Component component = contentPane.getComponentAt(i);
                    component.setY(yOffset);
                }
            }
            if (slideToDestMotion.isFinished()) {
                for (int i = 0; i < contentPane.getComponentCount(); i++) {
                    Component component = contentPane.getComponentAt(i);
                    component.paintLockRelease();
                }
                slideToDestMotion = null;
                setEnableLayoutOnPaint(true);
                deregisterAnimatedInternal();
                setSelectedIndex(active);
            }
            return true;
        }
        return b;
    }

    @Override
    void deregisterAnimatedInternal() {
        // Only stop ticking the Tabs animation when BOTH the content-slide AND the
        // indicator morph are done. Previously a finished 200ms slide deregistered the
        // animation while the 550ms morph was still in flight, freezing the drop mid-travel.
        if ((slideToDestMotion == null || slideToDestMotion.isFinished())
                && (indicatorAnimMotion == null || indicatorAnimMotion.isFinished())
                && glassMotionStart < 0) {
            TopLevelContainer f = getTopLevelContainer();
            if (f != null) {
                TopLevelSupport.deregisterAnimatedInternal(f, this);
            }
        }
    }

    /// Invokes set text position on the given tab, the tab should be a toggle button radio by default but
    /// can be anything
    ///
    /// #### Parameters
    ///
    /// - `tabComponent`: the component representing the tab
    ///
    /// - `textPosition`: the text position
    protected void setTextPosition(Component tabComponent, int textPosition) {
        ((Button) tabComponent).setTextPosition(textPosition);
    }

    /// Returns The position of the text relative to the icon
    ///
    /// #### Returns
    ///
    /// The position of the text relative to the icon, one of: LEFT, RIGHT, BOTTOM, TOP
    ///
    /// #### See also
    ///
    /// - #LEFT
    ///
    /// - #RIGHT
    ///
    /// - #BOTTOM
    ///
    /// - #TOP
    public int getTabTextPosition() {
        return textPosition;
    }

    /// Sets the position of the text relative to the icon if exists
    ///
    /// #### Parameters
    ///
    /// - `textPosition`: alignment value (LEFT, RIGHT, BOTTOM or TOP)
    ///
    /// #### See also
    ///
    /// - #LEFT
    ///
    /// - #RIGHT
    ///
    /// - #BOTTOM
    ///
    /// - #TOP
    public void setTabTextPosition(int textPosition) {
        if (textPosition != LEFT && textPosition != RIGHT && textPosition != BOTTOM && textPosition != TOP) {
            throw new IllegalArgumentException("Text position can't be set to " + textPosition);
        }
        this.textPosition = textPosition;
        for (int iter = 0; iter < getTabCount(); iter++) {
            setTextPosition(tabsContainer.getComponentAt(iter), textPosition);
        }
    }

    /// Adds a `component`
    /// represented by a `title` and/or `icon`,
    /// either of which can be `null`.
    /// Cover method for `insertTab`.
    ///
    /// #### Parameters
    ///
    /// - `title`: the title to be displayed in this tab
    ///
    /// - `icon`: the icon to be displayed in this tab
    ///
    /// - `component`: the component to be displayed when this tab is clicked
    ///
    /// #### See also
    ///
    /// - #insertTab
    ///
    /// - #removeTabAt
    public void addTab(String title, Image icon, Component component) {
        insertTab(title, icon, component, tabsContainer.getComponentCount());
    }

    /// Adds a `component`
    /// represented by a `title` and/or `icon`,
    /// either of which can be `null`.
    /// Cover method for `insertTab`.
    ///
    /// #### Parameters
    ///
    /// - `title`: the title to be displayed in this tab
    ///
    /// - `icon`: the icon to be displayed in this tab
    ///
    /// - `pressedIcon`: the icon shown when the tab is selected
    ///
    /// - `component`: the component to be displayed when this tab is clicked
    ///
    /// #### Returns
    ///
    /// this so these calls can be chained
    ///
    /// #### See also
    ///
    /// - #insertTab
    ///
    /// - #removeTabAt
    public Tabs addTab(String title, Image icon, Image pressedIcon, Component component) {
        int index = tabsContainer.getComponentCount();
        insertTab(title, icon, component, index);
        setTabSelectedIcon(index, pressedIcon);
        return this;
    }

    /// Adds a `component`
    /// represented by a `title` and/or `icon`,
    /// either of which can be `null`.
    /// Cover method for `insertTab`.
    ///
    /// #### Parameters
    ///
    /// - `title`: the title to be displayed in this tab
    ///
    /// - `materialIcon`: one of the material design icon constants from `com.codename1.ui.FontImage`
    ///
    /// - `iconSize`: icon size in millimeters
    ///
    /// - `component`: the component to be displayed when this tab is clicked
    ///
    /// #### Returns
    ///
    /// this so these calls can be chained
    ///
    /// #### See also
    ///
    /// - #insertTab
    ///
    /// - #removeTabAt
    public Tabs addTab(String title, char materialIcon, float iconSize, Component component) {
        insertTab(title, materialIcon, FontImage.getMaterialDesignFont(), iconSize, component,
                tabsContainer.getComponentCount());
        return this;
    }

    /// Adds a `component`
    /// represented by a `title` and/or `icon`,
    /// either of which can be `null`.
    /// Cover method for `insertTab`.
    ///
    /// #### Parameters
    ///
    /// - `title`: the title to be displayed in this tab
    ///
    /// - `icon`: an icon from the font
    ///
    /// - `font`: the font for the icon
    ///
    /// - `iconSize`: icon size in millimeters
    ///
    /// - `component`: the component to be displayed when this tab is clicked
    ///
    /// #### Returns
    ///
    /// this so these calls can be chained
    ///
    /// #### See also
    ///
    /// - #insertTab
    ///
    /// - #removeTabAt
    public Tabs addTab(String title, char icon, Font font, float iconSize, Component component) {
        int index = tabsContainer.getComponentCount();
        insertTab(title, icon, font, iconSize, component, index);
        return this;
    }

    /// Adds a `component`
    /// represented by a `title` and no `icon`.
    /// Cover method for `insertTab`.
    ///
    /// #### Parameters
    ///
    /// - `title`: the title to be displayed in this tab
    ///
    /// - `component`: the component to be displayed when this tab is clicked
    ///
    /// #### See also
    ///
    /// - #insertTab
    ///
    /// - #removeTabAt
    public void addTab(String title, Component component) {
        insertTab(title, null, component, tabsContainer.getComponentCount());
    }

    /// Adds a `component`
    /// represented by a `button`.
    /// Cover method for `insertTab`.
    /// The Button styling will be associated with "Tab" UIID.
    ///
    /// #### Parameters
    ///
    /// - `tab`: represents the tab on top
    ///
    /// - `component`: the component to be displayed when this tab is clicked
    ///
    /// #### Deprecated
    ///
    /// should use radio button as an argument
    ///
    /// #### See also
    ///
    /// - #insertTab
    ///
    /// - #removeTabAt
    public void addTab(Button tab, Component component) {
        insertTab(tab, component, tabsContainer.getComponentCount());
    }

    private Component createTabImpl(RadioButton b) {
        radioGroup.add(b);
        b.setToggle(true);
        b.setTextPosition(BOTTOM);
        if (radioGroup.getButtonCount() == 1) {
            b.setSelected(true);
        }
        if (textPosition != -1) {
            b.setTextPosition(textPosition);
        }

        if (b.getIcon() == null && !getUIManager().isThemeConstant("TabEnableAutoImageBool", true)) {
            Image d = getUIManager().getThemeImageConstant("TabUnselectedImage");
            if (d != null) {
                b.setIcon(d);
                d = getUIManager().getThemeImageConstant("TabSelectedImage");
                if (d != null) {
                    b.setRolloverIcon(d);
                    b.setPressedIcon(d);
                }
            }
        }
        return b;
    }

    /// Creates a tab component by default this is a RadioButton but subclasses can use this to return anything
    ///
    /// #### Parameters
    ///
    /// - `title`: the title of the tab
    ///
    /// - `icon`: an icon from the font
    ///
    /// - `font`: the font for the icon
    ///
    /// #### Returns
    ///
    /// component instance
    protected Component createTab(String title, Font font, char icon, float size) {
        RadioButton b = new RadioButton(title != null ? title : "");
        if (tabUIID != null) {
            b.setUIID(tabUIID);
        }
        applyTabIconUIID(b);
        b.setFontIcon(font, icon, size);
        createTabImpl(b);
        return b;
    }

    /// Detaches the tab's icon style from the Button's selection-state styles.
    /// FontImage.setIcon copies the Button's unselected/selected/pressed styles
    /// to render four icon variants - which means the icon image carries the
    /// Button's bgColor and bgTransparency. With a `cn1-pill-border` selected
    /// background, that produces a visible square fill behind the glyph that
    /// doesn't follow the pill's rounded shape. Reading `tabIconUIID` from the
    /// theme lets a theme route the icon styling to a separate UIID
    /// (typically `TabIcon`) where it can be declared transparent. Themes that
    /// don't define the constant get the legacy behavior unchanged.
    private void applyTabIconUIID(Component b) {
        String iconUiid = getUIManager().getThemeConstant("tabIconUIID", null);
        if (iconUiid != null && iconUiid.length() > 0 && b instanceof Label) {
            ((Label) b).setIconUIID(iconUiid);
        }
    }

    /// Creates a tab component by default this is a RadioButton but subclasses can use this to return anything
    ///
    /// #### Parameters
    ///
    /// - `title`: the title of the tab
    ///
    /// - `icon`: the icon of the tab
    ///
    /// #### Returns
    ///
    /// component instance
    protected Component createTab(String title, Image icon) {
        RadioButton b = new RadioButton(title != null ? title : "", icon);
        applyTabIconUIID(b);
        createTabImpl(b);
        return b;
    }

    /// Inserts a `component`, at `index`,
    /// represented by a `title` and/or `icon`,
    /// either of which may be `null`.
    /// Uses java.util.Vector internally, see `insertElementAt`
    /// for details of insertion conventions.
    ///
    /// #### Parameters
    ///
    /// - `title`: the title to be displayed in this tab
    ///
    /// - `icon`: the icon to be displayed in this tab
    ///
    /// - `component`: The component to be displayed when this tab is clicked.
    ///
    /// - `index`: the position to insert this new tab
    ///
    /// #### See also
    ///
    /// - #addTab
    ///
    /// - #removeTabAt
    public void insertTab(String title, Image icon, Component component,
                          int index) {
        Component b = createTab(title != null ? title : "", icon);
        insertTab(b, component, index);
    }

    /// Inserts a `component`, at `index`,
    /// represented by a `title` and/or `icon`,
    /// either of which may be `null`.
    /// Uses java.util.Vector internally, see `insertElementAt`
    /// for details of insertion conventions.
    ///
    /// #### Parameters
    ///
    /// - `title`: the title to be displayed in this tab
    ///
    /// - `icon`: an icon from the font
    ///
    /// - `font`: the font for the icon
    ///
    /// - `component`: The component to be displayed when this tab is clicked.
    ///
    /// - `index`: the position to insert this new tab
    ///
    /// #### See also
    ///
    /// - #addTab
    ///
    /// - #removeTabAt
    public void insertTab(String title, char icon, Font font, float iconSize, Component component,
                          int index) {
        Component b = createTab(title != null ? title : "", font, icon, iconSize);
        insertTab(b, component, index);
    }

    /// Inserts a `component`, at `index`,
    /// represented by a `button`
    /// Uses java.util.Vector internally, see `insertElementAt`
    /// for details of insertion conventions.
    /// The Button styling will be associated with "Tab" UIID.
    ///
    /// #### Parameters
    ///
    /// - `tab`: represents the tab on top
    ///
    /// - `component`: The component to be displayed when this tab is clicked.
    ///
    /// - `index`: the position to insert this new tab
    ///
    /// #### Deprecated
    ///
    /// should use radio button as an argument
    ///
    /// #### See also
    ///
    /// - #addTab
    ///
    /// - #removeTabAt
    public void insertTab(Component tab, Component component,
                          int index) {
        checkIndex(index);
        if (component == null) {
            return;
        }
        final Component b = tab;
        if (tabUIID != null) {
            b.setUIID(tabUIID);
        }

        b.addFocusListener(focusListener);

        bindTabActionListener(b, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {

                if (selectedTab != null) {
                    if (tabUIID != null) {
                        selectedTab.setUIID(tabUIID);
                    }
                    if (!animateTabSelection) {
                        selectedTab.setShouldCalcPreferredSize(true);
                        selectedTab.repaint();
                    }
                    int previousSelectedIndex = tabsContainer.getComponentIndex(selectedTab);

                    // this might happen if a tab was removed
                    if (previousSelectedIndex != -1) {
                        Component previousContent = contentPane.getComponentAt(previousSelectedIndex);
                        if (previousContent instanceof Container) {
                            ((Container) previousContent).setBlockFocus(true);
                        }
                    }
                }
                if (active != tabsContainer.getComponentIndex(b)) {
                    active = tabsContainer.getComponentIndex(b);
                    Component content = contentPane.getComponentAt(active);
                    if (content instanceof Container) {
                        ((Container) content).setBlockFocus(false);
                    }
                    setSelectedIndex(active, animateTabSelection);
                    initTabsFocus();
                    selectedTab = b;
                    if (!animateTabSelection) {
                        selectedTab.setShouldCalcPreferredSize(true);
                        tabsContainer.revalidateLater();
                    }
                    tabsContainer.scrollComponentToVisible(selectedTab);
                }
            }
        });

        if (component instanceof Container) {
            ((Container) component).setBlockFocus(true);
        }

        tabsContainer.addComponent(index, b);
        contentPane.addComponent(index, component);
        setTabsLayout(tabPlacement);
        if (tabsContainer.getComponentCount() == 1) {
            selectedTab = tabsContainer.getComponentAt(0);
            if (component instanceof Container) {
                ((Container) component).setBlockFocus(false);
            }
            initTabsFocus();
        }
        checkTabsCanBeSeen();
    }

    /// Binds an action listener to the tab component. this method should be used when overriding
    /// createTab
    ///
    /// #### Parameters
    ///
    /// - `tab`: the tab component
    ///
    /// - `l`: the listener
    protected void bindTabActionListener(Component tab, ActionListener l) {
        ((Button) tab).addActionListener(l);
    }

    /// Updates the information about the tab details
    ///
    /// #### Parameters
    ///
    /// - `title`: the title to be displayed in this tab
    ///
    /// - `icon`: the icon to be displayed in this tab
    ///
    /// - `index`: the position to insert this new tab
    public void setTabTitle(String title, Image icon, int index) {
        checkIndex(index);
        setTabTitle(tabsContainer.getComponentAt(index), title, icon);
    }

    /// Updates the tabs title . This method should be used when overriding
    /// createTab
    ///
    /// #### Parameters
    ///
    /// - `tab`: the tab component
    ///
    /// - `title`: the title
    ///
    /// - `icon`: the new icon
    protected void setTabTitle(Component tab, String title, Image icon) {
        Button b = (Button) tab;
        b.setText(title);
        b.setIcon(icon);
    }

    /// Returns the title of the tab at the given index
    ///
    /// #### Parameters
    ///
    /// - `index`: index for the tab
    ///
    /// #### Returns
    ///
    /// label of the tab at the given index
    public String getTabTitle(int index) {
        checkIndex(index);
        return getTabTitle(tabsContainer.getComponentAt(index));
    }

    /// Returns the title of the tab component. This method should be used when overriding
    /// createTab
    ///
    /// #### Parameters
    ///
    /// - `tab`: the tab component
    ///
    /// #### Returns
    ///
    /// label of the tab
    protected String getTabTitle(Component tab) {
        return ((Button) tab).getText();
    }

    /// Returns the icon of the tab component. This method should be used when overriding
    /// createTab
    ///
    /// #### Parameters
    ///
    /// - `tab`: the tab component
    ///
    /// #### Returns
    ///
    /// icon of the tab
    protected Image getTabIcon(Component tab) {
        return ((Button) tab).getIcon();
    }

    /// Returns the icon of the tab at the given index
    ///
    /// #### Parameters
    ///
    /// - `index`: index for the tab
    ///
    /// #### Returns
    ///
    /// icon of the tab at the given index
    public Image getTabIcon(int index) {
        checkIndex(index);
        return getTabIcon(tabsContainer.getComponentAt(index));
    }

    /// Returns the selected icon of the tab component. This method should be used when overriding
    /// createTab
    ///
    /// #### Parameters
    ///
    /// - `tab`: the tab component
    ///
    /// #### Returns
    ///
    /// icon of the tab
    protected Image getTabSelectedIcon(Component tab) {
        return ((Button) tab).getPressedIcon();
    }

    /// Returns the icon of the tab at the given index
    ///
    /// #### Parameters
    ///
    /// - `index`: index for the tab
    ///
    /// #### Returns
    ///
    /// icon of the tab at the given index
    public Image getTabSelectedIcon(int index) {
        checkIndex(index);
        return getTabSelectedIcon(tabsContainer.getComponentAt(index));
    }

    /// Sets the selected icon of the tab at the given index
    ///
    /// #### Parameters
    ///
    /// - `index`: index for the tab
    ///
    /// - `icon`: of the tab at the given index
    public void setTabSelectedIcon(int index, Image icon) {
        checkIndex(index);
        setTabSelectedIcon(tabsContainer.getComponentAt(index), icon);
    }

    /// Sets the selected icon of the tab. This method should be used when overriding
    /// createTab
    ///
    /// #### Parameters
    ///
    /// - `tab`: the tab component
    ///
    /// - `icon`: of the tab
    protected void setTabSelectedIcon(Component tab, Image icon) {
        ((Button) tab).setPressedIcon(icon);
    }

    /// Removes the tab at `index`.
    /// After the component associated with `index` is removed,
    /// its visibility is reset to true to ensure it will be visible
    /// if added to other containers.
    ///
    /// #### Parameters
    ///
    /// - `index`: the index of the tab to be removed
    ///
    /// #### Throws
    ///
    /// - `IndexOutOfBoundsException`: @throws IndexOutOfBoundsException if index is out of range
    /// (index = tab count)
    ///
    /// #### See also
    ///
    /// - #addTab
    ///
    /// - #insertTab
    public void removeTabAt(int index) {
        checkIndex(index);
        int act = activeComponent - 1;
        act = Math.max(act, 0);
        setSelectedIndex(act);
        Component key = tabsContainer.getComponentAt(index);
        tabsContainer.removeComponent(key);
        Component content = contentPane.getComponentAt(index);
        contentPane.removeComponent(content);
        setTabsLayout(tabPlacement);
    }

    /// Returns the tab at `index`.
    ///
    /// #### Parameters
    ///
    /// - `index`: the index of the tab to be removed
    ///
    /// #### Returns
    ///
    /// the component at the given tab location
    ///
    /// #### Throws
    ///
    /// - `IndexOutOfBoundsException`: @throws IndexOutOfBoundsException if index is out of range
    /// (index = tab count)
    ///
    /// #### See also
    ///
    /// - #addTab
    ///
    /// - #insertTab
    public Component getTabComponentAt(int index) {
        checkIndex(index);
        return contentPane.getComponentAt(index);
    }

    private void checkIndex(int index) {
        if (index < 0 || index > tabsContainer.getComponentCount()) {
            throw new IndexOutOfBoundsException("Index: " + index);
        }
    }

    /// Returns the index of the tab for the specified component.
    /// Returns -1 if there is no tab for this component.
    ///
    /// #### Parameters
    ///
    /// - `component`: the component for the tab
    ///
    /// #### Returns
    ///
    /// @return the first tab which matches this component, or -1
    /// if there is no tab for this component
    public int indexOfComponent(Component component) {
        return contentPane.getComponentIndex(component);
    }

    /// Returns the number of tabs in this `tabbedpane`.
    ///
    /// #### Returns
    ///
    /// an integer specifying the number of tabbed pages
    public int getTabCount() {
        return tabsContainer.getComponentCount();
    }

    /// Returns the currently selected index for this tabbedpane.
    /// Returns -1 if there is no currently selected tab.
    ///
    /// #### Returns
    ///
    /// the index of the selected tab
    public int getSelectedIndex() {
        if (tabsContainer != null) {
            return activeComponent;
        }
        return -1;
    }

    /// Sets the selected index for this tabbedpane. The index must be a valid
    /// tab index.
    ///
    /// #### Parameters
    ///
    /// - `index`: the index to be selected
    ///
    /// #### Throws
    ///
    /// - `IndexOutOfBoundsException`: @throws IndexOutOfBoundsException if index is out of range
    /// (index = tab count)
    public void setSelectedIndex(int index) {
        setSelectedIndex(index, false);
    }

    /// Returns the component associated with the tab at the given index
    ///
    /// #### Returns
    ///
    /// the component is now showing in the tabbed pane
    public Component getSelectedComponent() {
        int i = getSelectedIndex();
        if (i == -1) {
            return null;
        }
        return getTabComponentAt(i);
    }

    /// Adds a focus listener to the tabs buttons
    ///
    /// #### Parameters
    ///
    /// - `listener`: FocusListener
    ///
    /// #### Deprecated
    ///
    /// use addSelectionListener instead
    public void addTabsFocusListener(FocusListener listener) {
        if (focusListeners == null) {
            focusListeners = new EventDispatcher();
        }
        focusListeners.addListener(listener);
    }

    /// Removes a foucs Listener from the tabs buttons
    ///
    /// #### Parameters
    ///
    /// - `listener`: FocusListener
    ///
    /// #### Deprecated
    ///
    /// use addSelectionListener instead
    public void removeTabsFocusListener(FocusListener listener) {
        if (focusListeners != null) {
            focusListeners.removeListener(listener);
        }
    }

    /// Adds a selection listener to the tabs.
    ///
    /// #### Parameters
    ///
    /// - `listener`: SelectionListener
    public void addSelectionListener(SelectionListener listener) {
        if (selectionListener == null) {
            selectionListener = new EventDispatcher();
        }
        selectionListener.addListener(listener);
    }

    /// Removes a selection Listener from the tabs
    ///
    /// #### Parameters
    ///
    /// - `listener`: SelectionListener
    public void removeSelectionListener(SelectionListener listener) {
        if (selectionListener != null) {
            selectionListener.removeListener(listener);
        }
    }

    /// {@inheritDoc}
    @Override
    public String toString() {
        String className = getClass().getName();
        className = className.substring(className.lastIndexOf('.') + 1);
        return className + "[x=" + getX() + " y=" + getY() + " width=" +
                getWidth() + " height=" + getHeight() + ", tab placement = " +
                tabPlacement + ", tab count = " + getTabCount() +
                ", selected index = " + getSelectedIndex() + "]";
    }

    /// Returns the placement of the tabs for this tabbedpane.
    ///
    /// #### Returns
    ///
    /// the tab placement value
    ///
    /// #### See also
    ///
    /// - #setTabPlacement
    public int getTabPlacement() {
        return tabPlacement;
    }

    /// Sets the tab placement for this tabbedpane.
    /// Possible values are:
    ///
    /// - `Component.TOP`
    ///
    /// - `Component.BOTTOM`
    ///
    /// - `Component.LEFT`
    ///
    /// - `Component.RIGHT`
    ///
    /// The default value, if not set, is `Component.TOP`.
    ///
    /// #### Parameters
    ///
    /// - `tabPlacement`: the placement for the tabs relative to the content
    public void setTabPlacement(int tabPlacement) {
        if (tabPlacement != TOP && tabPlacement != LEFT &&
                tabPlacement != BOTTOM && tabPlacement != RIGHT) {
            throw new IllegalArgumentException("illegal tab placement: must be TOP, BOTTOM, LEFT, or RIGHT");
        }
        Container slotComponent = tabsContainerHost != null ? tabsContainerHost : tabsContainer;
        if (this.tabPlacement == tabPlacement && slotComponent.getParent() == null && isInitialized()) {
            return;
        }
        this.tabPlacement = tabPlacement;
        removeComponent(slotComponent);

        setTabsLayout(tabPlacement);

        if (tabPlacement == TOP) {
            super.addComponent(BorderLayout.NORTH, slotComponent);
        } else if (tabPlacement == BOTTOM) {
            super.addComponent(BorderLayout.SOUTH, slotComponent);
        } else if (tabPlacement == LEFT) {
            super.addComponent(BorderLayout.WEST, slotComponent);
        } else { // RIGHT
            super.addComponent(BorderLayout.EAST, slotComponent);
        }

        initTabsFocus();

        tabsContainer.setShouldCalcPreferredSize(true);
        contentPane.setShouldCalcPreferredSize(true);

        revalidateLater();
    }

    /// This method retrieves the Tabs content pane
    ///
    /// #### Returns
    ///
    /// the content pane Container
    public Container getContentPane() {
        return contentPane;
    }

    /// This method retrieves the Tabs buttons Container
    ///
    /// #### Returns
    ///
    /// the Tabs Container
    public Container getTabsContainer() {
        return tabsContainer;
    }

    /// Sets the currently selected index in the tabs component
    ///
    /// #### Parameters
    ///
    /// - `index`: the index for the tab starting with tab 0.
    ///
    /// - `slideToSelected`: @param slideToSelected true to animate the transition to the new selection
    /// false to just move immediately
    public void setSelectedIndex(int index, boolean slideToSelected) {
        if (index < 0 || index >= tabsContainer.getComponentCount()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Tab count: " + tabsContainer.getComponentCount());
        }
        if (index == activeComponent) {
            return;
        }
        accessibilityChanged(AccessibilityManager.CHANGE_STATE | AccessibilityManager.CHANGE_PANE);
        // Snapshot the current tab bounds *before* we mutate state, so the
        // animated indicator can tween from where it visibly is to the new
        // selection's bounds.
        startIndicatorAnimation(activeComponent, index);

        TopLevelContainer form = getTopLevelContainer();
        if (slideToSelected && form != null) {
            int end;
            int start;
            if (swipeOnXAxis) {
                end = contentPane.getComponentAt(activeComponent).getX();
                start = contentPane.getComponentAt(index).getX();
            } else {
                end = contentPane.getComponentAt(activeComponent).getY();
                start = contentPane.getComponentAt(index).getY();
            }
            slideToDestMotion = createTabSlideMotion(start, end);
            slideToDestMotion.start();
            TopLevelSupport.registerAnimatedInternal(form, this);
            active = index;
        } else {
            if (selectionListener != null) {
                selectionListener.fireSelectionEvent(activeComponent, index);
            }
            activeComponent = index;
            selectTab(tabsContainer.getComponentAt(index));
            int offset = 0;
            for (Component c : contentPane) {
                c.setLightweightMode(offset != index);
                offset++;
            }
            revalidateLater();
        }
    }

    /// Invoked to select a specific tab, this method should be overriden for subclasses overriding createTab
    ///
    /// #### Parameters
    ///
    /// - `tab`: the tab
    protected void selectTab(Component tab) {
        Button b = (Button) tab;
        b.fireClicked();
        b.requestFocus();
    }

    /// Enables the Material 3 sliding-underline indicator (off by default).
    /// When on, selection changes tween the indicator from the old tab's
    /// bounds to the new tab's bounds over `tabsAnimatedIndicatorDurationInt`
    /// milliseconds (default 200, ease-in-out cubic).
    ///
    /// Color is taken from the `TabIndicator` UIID's foreground color when
    /// it exists, otherwise from the currently-selected tab's foreground color.
    /// Thickness is 1mm; override with the `tabsAnimatedIndicatorThicknessMm`
    /// theme constant (in millimeters).
    public void setAnimatedIndicator(boolean enable) {
        this.animatedIndicator = enable;
        // First frame: snap the indicator to the currently-selected tab so
        // it appears immediately on enable rather than on the next change.
        if (enable && tabsContainer.getComponentCount() > 0) {
            Component active = tabsContainer.getComponentAt(activeComponent);
            indicatorFromX = active.getX();
            indicatorFromW = active.getWidth();
            indicatorToX = indicatorFromX;
            indicatorToW = indicatorFromW;
        }
        tabsContainer.repaint();
    }

    /// Returns whether the animated tab indicator is enabled. See
    /// `#setAnimatedIndicator(boolean)`.
    public boolean isAnimatedIndicator() {
        return animatedIndicator;
    }

    /// Duration in milliseconds of the selection morph -- both the Material
    /// underline tween and the iOS 26 Liquid Glass selection-capsule spring.
    /// Defaults to the `tabsAnimatedIndicatorDurationInt` theme constant.
    public void setAnimatedIndicatorDuration(int durationMs) {
        this.animatedIndicatorDurationMs = durationMs;
    }

    /// Returns the selection-morph duration in milliseconds. See
    /// `#setAnimatedIndicatorDuration(int)`.
    public int getAnimatedIndicatorDuration() {
        return animatedIndicatorDurationMs;
    }

    private void startIndicatorAnimation(int fromIndex, int toIndex) {
        if ((!animatedIndicator && !selectionCapsule) || tabsContainer == null) {
            return;
        }
        if (fromIndex < 0 || fromIndex >= tabsContainer.getComponentCount()
                || toIndex < 0 || toIndex >= tabsContainer.getComponentCount()) {
            return;
        }
        if (isGlassMotion()) {
            startGlassMotion(fromIndex, toIndex);
            return;
        }
        Component fromTab = tabsContainer.getComponentAt(fromIndex);
        Component toTab = tabsContainer.getComponentAt(toIndex);
        // The selection capsule fills the whole CELL (and hugs the pill edge for the
        // first/last tab); the Material underline tracks the tab's own bounds. Pick the
        // matching geometry so the resting and animated positions agree.
        int[] from = new int[2];
        int[] to = new int[2];
        if (selectionCapsule) {
            int cInset = selectionCapsuleInsetPx();
            capsuleCellBounds(fromIndex, cInset, from);
            capsuleCellBounds(toIndex, cInset, to);
        } else {
            from[0] = fromTab.getX(); from[1] = fromTab.getWidth();
            to[0] = toTab.getX(); to[1] = toTab.getWidth();
        }
        // If a motion is already in flight, start from the *current*
        // interpolated position, not from the previous tab -- otherwise
        // rapid double-clicks jump back to a stale baseline.
        if (indicatorAnimMotion != null && !indicatorAnimMotion.isFinished()) {
            if (toIndex == indicatorTargetIndex) {
                // A morph to this SAME tab is already running. The content-slide finishing
                // re-invokes setSelectedIndex(active) to finalise the selection; restarting
                // the morph here froze/jumped the drop mid-travel ("stops before the end").
                // Let the in-flight morph run to completion instead.
                return;
            }
            int v = indicatorAnimMotion.getValue();
            indicatorFromX = indicatorFromX + ((indicatorToX - indicatorFromX) * v / 100);
            indicatorFromW = indicatorFromW + ((indicatorToW - indicatorFromW) * v / 100);
        } else {
            indicatorFromX = from[0];
            indicatorFromW = from[1];
        }
        indicatorToX = to[0];
        indicatorToW = to[1];
        indicatorTargetIndex = toIndex;
        // LINEAR-TIME motion: the value is the morph timeline 0..100 and
        // paintSelectionCapsule derives the spring position (springEaseTabs, an
        // ease-out-back overshoot) AND the height/squash envelopes from it -- so the
        // bubble stays tall while travelling and compresses at the stop. (The non-glass
        // Material underline path reads the same value as a plain position fraction.)
        indicatorAnimMotion = Motion.createLinearMotion(0, 100, animatedIndicatorDurationMs);
        indicatorAnimMotion.start();
        TopLevelContainer f = getTopLevelContainer();
        if (f != null) {
            TopLevelSupport.registerAnimatedInternal(f, this);
        }
    }

    /// Material 3 tab strips carry a full-width hairline divider along the bottom
    /// edge of the tab row (the surfaceVariant outline separating the bar from the
    /// content below). A CSS `border-bottom` cannot be relied on here -- the tab
    /// row is a custom Container whose painting path does not surface the underline
    /// border -- so themes opt in via the `tabsBottomDividerBool` constant and we
    /// paint it directly. The colour comes from the `TabsDivider` UIID's background
    /// (so it tracks light/dark automatically, like `TabIndicator`);
    /// `tabsBottomDividerThicknessMm` (default 0.15mm) sets the line weight.
    void paintBottomDivider(Graphics g) {
        if (!getUIManager().isThemeConstant("tabsBottomDividerBool", false)) {
            return;
        }
        int color = getUIManager().getComponentStyle("TabsDivider").getBgColor();
        float thickMm = 0.15f;
        try {
            thickMm = Float.parseFloat(getUIManager().getThemeConstant("tabsBottomDividerThicknessMm", "0.15"));
        } catch (NumberFormatException ignore) {
            // malformed constant -> keep the 0.15mm default
        }
        int thickness = Display.getInstance().convertToPixels(thickMm);
        if (thickness < 1) {
            thickness = 1;
        }
        int oldColor = g.getColor();
        int oldAlpha = g.getAlpha();
        g.setColor(color);
        g.setAlpha(255);
        int y = tabsContainer.getY() + tabsContainer.getHeight() - thickness;
        g.fillRect(tabsContainer.getX(), y, tabsContainer.getWidth(), thickness);
        g.setColor(oldColor);
        g.setAlpha(oldAlpha);
    }

    // Position easing (springEaseTabs) + smoothstep (lensSmooth) now live in the pure
    // TabSelectionMorph model (springEase / smooth) so the morph math is unit-testable.

    /// The thin frost rim left around the selection capsule (tabSelInsetMm, default a hair).
    private int selectionCapsuleInsetPx() {
        if (isGlassMotion()) {
            return Math.round(glassPx(glassConstantPt("tabsGlassInsetPt", GLASS_INSET_PT)));
        }
        float insetMm = 0.1f;
        String iv = getUIManager().getThemeConstant("tabSelInsetMm", null);
        if (iv != null) {
            try {
                insetMm = Float.parseFloat(iv.trim());
            } catch (NumberFormatException ignore) {
                insetMm = 0.1f;   // malformed constant -> keep the hairline default
            }
        }
        return Display.getInstance().convertToPixels(insetMm);
    }

    /// Horizontal bounds of the selection capsule for tab `index`, in the
    /// tabsContainer inner coordinate space (add getInnerX()). The capsule fills the
    /// whole CELL -- midpoint-to-midpoint between neighbours -- and hugs the pill's
    /// outer edge (minus the thin rim) for the first/last tab, like a native UITabBar.
    /// out[0]=x, out[1]=w.
    private void capsuleCellBounds(int index, int inset, int[] out) {
        int n = tabsContainer.getComponentCount();
        Component t = tabsContainer.getComponentAt(index);
        int padLeft = tabsContainer.getInnerX() - tabsContainer.getX();
        if (tabsContainer.getLayout() instanceof GlassTabsLayout) {
            // UITabBar's resting lens: the tab's own button bounds, centred on the
            // tab, not the cell between its neighbours.
            float centre = t.getX() + t.getWidth() / 2f;
            float half = glassLensWidth / 2f;
            out[0] = Math.round(centre - half) - padLeft;
            out[1] = Math.round(centre + half) - Math.round(centre - half);
            return;
        }
        int padRight = (tabsContainer.getX() + tabsContainer.getWidth())
                - (tabsContainer.getInnerX() + tabsContainer.getInnerWidth());
        int left;
        int right;
        // Tab.getX() is tabsContainer-relative (it INCLUDES the container's left
        // padding). paintSelectionCapsule adds getInnerX() (which also includes that
        // padding), so the midpoint branches must subtract padLeft to land in the
        // same inner-x space as the first/last branches -- otherwise every non-first
        // tab's capsule drifts right by padLeft (the first tab compensated, hiding it).
        // With the glass motion the container's padding also holds the reserved motion
        // slack, which lies OUTSIDE the visible pill.
        int slack = glassSlackXPx();
        if (index <= 0) {
            left = -padLeft + slack + inset;               // pill outer left edge
        } else {
            Component p = tabsContainer.getComponentAt(index - 1);
            left = (p.getX() + p.getWidth() + t.getX()) / 2 - padLeft;   // midpoint to previous tab
        }
        if (index >= n - 1) {
            right = tabsContainer.getInnerWidth() + padRight - slack - inset;   // pill outer right edge
        } else {
            Component nx = tabsContainer.getComponentAt(index + 1);
            right = (t.getX() + t.getWidth() + nx.getX()) / 2 - padLeft;          // midpoint to next tab
        }
        out[0] = left;
        out[1] = right - left;
    }

    /// TEST-ONLY hook: render the selection morph frozen at a fixed progress
    /// (`value` 0..120, where 100 is the target and &gt;100 is the settle overshoot)
    /// travelling from `fromIndex` to `toIndex`, so a JavaSE probe can capture exact
    /// frames of the animation without racing the real-time motion. Pass `value` &lt; 0
    /// to clear and resume normal behaviour.
    public void setMorphTestState(int fromIndex, int toIndex, int value) {
        if (tabsContainer == null || tabsContainer.getComponentCount() == 0) {
            return;
        }
        if (isGlassMotion()) {
            // The measured ios27 motion is time based: progress 0..100 maps onto its
            // first 800 ms (lift, travel, arrival squash), so a progress-frame
            // capture spec keeps working; setGlassMotionTestTime takes exact times.
            setGlassMotionTestTime(fromIndex, toIndex, value < 0 ? -1 : value * GLASS_TEST_MS_PER_PERCENT);
            return;
        }
        if (value >= 0) {
            int cInset = selectionCapsuleInsetPx();
            int[] from = new int[2];
            int[] to = new int[2];
            capsuleCellBounds(fromIndex, cInset, from);
            capsuleCellBounds(toIndex, cInset, to);
            indicatorFromX = from[0];
            indicatorFromW = from[1];
            indicatorToX = to[0];
            indicatorToW = to[1];
            activeComponent = toIndex;
        }
        morphTestValue = value;
        tabsContainer.repaint();
    }

    /// TEST-ONLY hook for the measured iOS 27 motion (`tabsMorphPreset: "ios27"`):
    /// freezes the selection change from `fromIndex` to `toIndex` exactly
    /// `elapsedMs` milliseconds after it started, so a capture tool can render
    /// deterministic frames of the motion (lens, bar scale, platter, accent content
    /// and glow). Pass `elapsedMs` &lt; 0 to clear and resume normal behaviour. Has no
    /// effect unless the theme selects the ios27 motion.
    ///
    /// #### Parameters
    ///
    /// - `fromIndex`: the tab the selection leaves
    ///
    /// - `toIndex`: the tab the selection moves to
    ///
    /// - `elapsedMs`: milliseconds since the selection started, or &lt; 0 to clear
    public void setGlassMotionTestTime(int fromIndex, int toIndex, int elapsedMs) {
        if (tabsContainer == null || tabsContainer.getComponentCount() == 0 || !isGlassMotion()) {
            return;
        }
        if (elapsedMs >= 0) {
            int inset = selectionCapsuleInsetPx();
            int[] cb = new int[2];
            capsuleCellBounds(fromIndex, inset, cb);
            glassFromCenter = cb[0] + cb[1] / 2f;
            glassFromW = cb[1];
            capsuleCellBounds(toIndex, inset, cb);
            glassToCenter = cb[0] + cb[1] / 2f;
            glassToW = cb[1];
            activeComponent = toIndex;
        }
        glassTestElapsedMs = elapsedMs;
        tabsContainer.repaint();
    }

    /// True when the theme selects the measured iOS 27 Liquid Glass selection motion
    /// (`tabsMorphPreset: "ios27"` together with `tabsSelectionCapsuleBool`).
    boolean isGlassMotion() {
        return selectionCapsule && tabsContainer != null
                && "ios27".equals(getUIManager().getThemeConstant("tabsMorphPreset", "ios26"));
    }

    /// The motion slack the theme reserved around the pill, in pixels. The bar
    /// scales, and the lifted lens overshoots its ends by up to ~11 pt, so the
    /// theme widens TabsContainer's padding by this much (and narrows its margin to
    /// match) and names the amount here: the pill is the bounds minus the slack,
    /// and the whole motion stays inside the component's own repaint region.
    private int glassSlackXPx() {
        return isGlassMotion() ? mmConstantPx("tabsGlassSlackXMm") : 0;
    }

    private int glassSlackYPx() {
        return isGlassMotion() ? mmConstantPx("tabsGlassSlackYMm") : 0;
    }

    private int mmConstantPx(String name) {
        String v = getUIManager().getThemeConstant(name, null);
        if (v == null) {
            return 0;
        }
        try {
            return Display.getInstance().convertToPixels(Float.parseFloat(v.trim()));
        } catch (NumberFormatException ignore) {
            return 0;   // malformed constant -> no slack
        }
    }

    /// Remembers where a press landed on the tab bar, so the touch glow of the
    /// selection it triggers expands from the finger like the native one.
    void recordGlassPress(int x, int y) {
        if (tabsContainer != null && tabsContainer.visibleBoundsContains(x, y)) {
            glassPressX = x - tabsContainer.getAbsoluteX();
            glassPressY = y - tabsContainer.getAbsoluteY();
            // UIKit moves the lens on touch-DOWN; the selection itself still happens
            // on release, and finds this motion already in flight to its target.
            if (isGlassMotion()) {
                Component target = tabsContainer.getComponentAt(x, y);
                int idx = target == null ? -1 : tabsContainer.getComponentIndex(target);
                if (idx >= 0) {
                    // A press on the selected tab lifts and pulses the lens in place.
                    int pressX = glassPressX;
                    startGlassMotion(activeComponent, idx, true);
                    glassGesturePressX = pressX;
                }
            }
        } else {
            glassPressX = -1;
            glassPressY = -1;
        }
    }

    /// True while the glass motion is travelling to tab `index` (package-private,
    /// for tests).
    boolean isGlassMotionRunningTo(int index) {
        return glassMotionStart >= 0 && glassTargetIndex == index;
    }

    /// Points to pixels for the glass geometry (the pill height over the native
    /// bar's 62 pt).
    private float glassPtPx() {
        int ph = tabsContainer.getHeight() - 2 * glassSlackYPx();
        return ph > 0 ? ph / TabGlassMotion.BAR_HEIGHT_PT : 1f;
    }

    /// The finger moved along the bar during a glass press: once it has travelled
    /// far enough the lens follows it (a scrub).
    void recordGlassDrag(int x) {
        TabGlassGesture gg = glassGesture;
        if (gg == null || glassMotionStart < 0 || gg.getUpS() >= 0 || glassGesturePressX < 0) {
            return;
        }
        float s = (System.currentTimeMillis() - glassMotionStart) / 1000f;
        int rel = x - tabsContainer.getAbsoluteX();
        if (!gg.isScrubbing()) {
            if (Math.abs(rel - glassGesturePressX) < Display.getInstance().convertToPixels(1.5f)) {
                return;
            }
            int inset = selectionCapsuleInsetPx();
            int[] cb = new int[2];
            int n = tabsContainer.getComponentCount();
            capsuleCellBounds(0, inset, cb);
            float first = cb[0] + cb[1] / 2f;
            capsuleCellBounds(n - 1, inset, cb);
            float last = cb[0] + cb[1] / 2f;
            float ptPx = glassPtPx();
            gg.startScrub(s, first / ptPx, last / ptPx);
        }
        gg.finger(s, fingerInnerX(rel) / glassPtPx());
    }

    /// A tabsContainer-relative x in the inner-x space of capsuleCellBounds.
    private float fingerInnerX(int rel) {
        return rel + tabsContainer.getX() - tabsContainer.getInnerX();
    }

    /// Touch-up of a glass press. A scrub selects the tab nearest the finger (a
    /// tie keeps the current tab) and the lens settles there.
    void recordGlassRelease(int x) {
        TabGlassGesture gg = glassGesture;
        if (gg == null || glassMotionStart < 0 || gg.getUpS() >= 0) {
            return;
        }
        float s = (System.currentTimeMillis() - glassMotionStart) / 1000f;
        if (!gg.isScrubbing()) {
            gg.up(s);
            return;
        }
        float ptPx = glassPtPx();
        float fx = fingerInnerX(x - tabsContainer.getAbsoluteX());
        gg.finger(s, fx / ptPx);
        // The tab whose cell holds the finger -- for equal cells, the centre nearest
        // it (natively a finger exactly between two keeps the current tab).
        int inset = selectionCapsuleInsetPx();
        int[] cb = new int[2];
        int n = tabsContainer.getComponentCount();
        int best = n - 1;
        for (int i = 0; i < n; i++) {
            capsuleCellBounds(i, inset, cb);
            float right = cb[0] + cb[1];
            if (fx < right || i == n - 1) {
                best = i;
                if (fx == right && i == activeComponent - 1) {
                    best = activeComponent;
                }
                break;
            }
        }
        capsuleCellBounds(best, inset, cb);
        glassToCenter = cb[0] + cb[1] / 2f;
        glassToW = cb[1];
        glassTargetIndex = best;
        gg.settleTo(glassToCenter / ptPx);
        gg.up(s);
        if (best != activeComponent && best >= 0) {
            setSelectedIndex(best);
        }
    }

    /// After a release has been processed: a press that started the lens towards a
    /// tab but did not select it (released elsewhere, turned into a swipe) sends the
    /// lens back to the selected tab.
    void checkGlassPressOutcome() {
        if (glassGesture != null && glassGesture.isScrubbing()) {
            return;
        }
        if (glassMotionStart >= 0 && glassTargetIndex >= 0 && glassTargetIndex != activeComponent
                && (slideToDestMotion == null || active != glassTargetIndex)) {
            startGlassMotion(glassTargetIndex, activeComponent);
        }
    }

    private void startGlassMotion(int fromIndex, int toIndex) {
        startGlassMotion(fromIndex, toIndex, false);
    }

    /// `press` starts a live gesture (hold, scrub) instead of the captured tap.
    private void startGlassMotion(int fromIndex, int toIndex, boolean press) {
        if (!press && glassMotionStart >= 0 && toIndex == glassTargetIndex) {
            // Same target already in flight (the content slide re-selecting it); let
            // it finish rather than restarting the lift.
            return;
        }
        int inset = selectionCapsuleInsetPx();
        int[] cb = new int[2];
        if (glassMotionStart >= 0) {
            // Interrupted: restart from where the lens is drawn right now.
            GlassFrame f = glassFrame();
            glassFromCenter = f.centerInner;
            glassFromW = f.restWidth;
        } else {
            capsuleCellBounds(fromIndex, inset, cb);
            glassFromCenter = cb[0] + cb[1] / 2f;
            glassFromW = cb[1];
        }
        capsuleCellBounds(toIndex, inset, cb);
        glassToCenter = cb[0] + cb[1] / 2f;
        glassToW = cb[1];
        glassTargetIndex = toIndex;
        if (press) {
            float ptPx = glassPtPx();
            glassGesture = new TabGlassGesture(glassFromCenter / ptPx, glassToCenter / ptPx);
        } else {
            glassGesture = null;
        }
        glassTouchX = glassPressX;
        glassTouchY = glassPressY;
        glassPressX = -1;
        glassPressY = -1;
        glassMotionStart = System.currentTimeMillis();
        TopLevelContainer f = getTopLevelContainer();
        if (f != null) {
            TopLevelSupport.registerAnimatedInternal(f, this);
        }
        tabsContainer.repaint();
    }

    /// One resolved frame of the glass selection, in tabsContainer PARENT
    /// coordinates (the space getX()/getY() answer in), before the bar scale.
    static final class GlassFrame {
        TabGlassMotion motion;
        float ptPx;
        int pillX;
        int pillY;
        int pillW;
        int pillH;
        float barScale;
        float pivotX;
        float pivotY;
        float centerInner;   // lens centre in the capsuleCellBounds inner-x space
        float restWidth;     // resting lens width at this point of the travel
        float lensX;
        float lensY;
        float lensW;
        float lensH;
        float glowX;
        float glowY;
    }

    private GlassFrame glassFrame() {
        GlassFrame f = new GlassFrame();
        Container tc = tabsContainer;
        int sx = glassSlackXPx();
        int sy = glassSlackYPx();
        f.pillX = tc.getX() + sx;
        f.pillY = tc.getY() + sy;
        f.pillW = tc.getWidth() - 2 * sx;
        f.pillH = tc.getHeight() - 2 * sy;
        // Native geometry is expressed in points on a 62 pt tall bar; deriving the
        // scale from the pill makes the motion follow the theme's bar height.
        f.ptPx = f.pillH > 0 ? f.pillH / TabGlassMotion.BAR_HEIGHT_PT : 1f;
        int inset = selectionCapsuleInsetPx();
        float restH = f.pillH - 2 * inset;
        boolean live = glassTestElapsedMs >= 0 || glassMotionStart >= 0;
        float fromC;
        float fromW;
        float toC;
        float toW;
        if (live) {
            fromC = glassFromCenter;
            fromW = glassFromW;
            toC = glassToCenter;
            toW = glassToW;
        } else {
            int[] cb = new int[2];
            int idx = activeComponent < 0 ? 0 : Math.min(activeComponent, tc.getComponentCount() - 1);
            capsuleCellBounds(idx, inset, cb);
            fromC = cb[0] + cb[1] / 2f;
            fromW = cb[1];
            toC = fromC;
            toW = fromW;
        }
        float travelPt = (toC - fromC) / f.ptPx;
        TabGlassMotion m;
        float gestureCentre = Float.NaN;
        if (glassTestElapsedMs >= 0) {
            m = TabGlassMotion.at(glassTestElapsedMs, travelPt);
        } else if (glassMotionStart >= 0 && glassGesture != null) {
            float[] c = new float[1];
            m = glassGesture.at((System.currentTimeMillis() - glassMotionStart) / 1000f, c);
            if (glassGesture.isScrubbing()) {
                gestureCentre = c[0] * f.ptPx;
            }
        } else if (glassMotionStart >= 0) {
            m = TabGlassMotion.at(System.currentTimeMillis() - glassMotionStart, travelPt);
        } else {
            m = TabGlassMotion.rest();
        }
        f.motion = m;
        float p = m.position;
        float pw = p < 0 ? 0 : (p > 1 ? 1 : p);
        f.centerInner = fromC + (toC - fromC) * p;
        f.restWidth = fromW + (toW - fromW) * pw;
        if (gestureCentre == gestureCentre) {
            // Scrubbing: the lens is wherever the finger has pulled it.
            f.centerInner = gestureCentre;
            f.restWidth = toW;
        }
        float lift = TabGlassMotion.LIFT_PT * f.ptPx * m.lift;
        f.lensW = (f.restWidth + lift) * m.scaleX;
        f.lensH = (restH + lift) * m.scaleY;
        float cx = tc.getInnerX() + f.centerInner + m.leadPt * f.ptPx;
        float cy = f.pillY + f.pillH / 2f;
        f.lensX = cx - f.lensW / 2f;
        f.lensY = cy - f.lensH / 2f;
        f.barScale = f.pillW > 0 ? 1f + m.barGrowPt * f.ptPx / f.pillW : 1f;
        f.pivotX = f.pillX + f.pillW / 2f;
        f.pivotY = cy;
        f.glowX = glassTouchX >= 0 ? tc.getX() + glassTouchX : tc.getInnerX() + toC;
        if (gestureCentre == gestureCentre) {
            // A scrub's glow rides the lens, not the spot the finger first touched.
            f.glowX = cx;
        }
        f.glowY = glassTouchY >= 0 ? tc.getY() + glassTouchY : cy;
        return f;
    }

    /// Scales a coordinate about a pivot (the bar pulse, for the ops that draw
    /// straight into the frame and so cannot ride the Graphics transform).
    private static float scaleAbout(float v, float pivot, float scale) {
        return pivot + (v - pivot) * scale;
    }

    /// The bar's own glass and fill, drawn into the (scaled) pill instead of the
    /// slack-padded bounds.
    void paintGlassBarBackground(Graphics g) {
        GlassFrame f = glassFrame();
        float s = f.barScale;
        int x = Math.round(scaleAbout(f.pillX, f.pivotX, s));
        int y = Math.round(scaleAbout(f.pillY, f.pivotY, s));
        int r = Math.round(scaleAbout(f.pillX + f.pillW, f.pivotX, s));
        int b = Math.round(scaleAbout(f.pillY + f.pillH, f.pivotY, s));
        tabsContainer.paintBackgroundLayerAt(g, x, y, r - x, b - y);
        paintGlassBarRim(g, x, y, r - x, b - y, f.ptPx * f.barScale);
    }

    /// Relative strength of each row of the specular rim, from the edge inwards.
    private static final float[] GLASS_RIM_FALLOFF = {1f, 0.7f, 0.43f, 0.2f};

    /// The selection platter's colour matrix, read back from the native bar
    /// (the colorMatrix filter UIKit puts on the platter's backdrop layer): a 1.2
    /// gain less a share of the Rec. 709 luminance, `M = 1.2 * I - k * 1 * w^T`,
    /// plus an offset -- k 0.33 and offset -0.07 on the dark bar, 0.07 and -0.2 on
    /// the light one. Every entry UIKit reports matches within 1.4e-4 (under 0.04 of
    /// a colour level), which is as far as UIKit's own rows agree with each other. It
    /// turns the dark bar's 134 grey into 98.8 and the light bar's 195 into 169.4,
    /// the measured platter.
    private static final float[] GLASS_PLATTER_DARK = platterMatrix(0.33f, -0.07f);
    private static final float[] GLASS_PLATTER_LIGHT = platterMatrix(0.07f, -0.2f);

    /// Rows r, g, b of [r, g, b, offset] for `1.2 * I - luminanceShare * 1 * w^T + offset`.
    static float[] platterMatrix(float luminanceShare, float offset) {
        float[] w = {0.2126f, 0.7152f, 0.0722f};
        float[] m = new float[12];
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                m[r * 4 + c] = (r == c ? 1.2f : 0f) - luminanceShare * w[c];
            }
            m[r * 4 + 3] = offset;
        }
        return m;
    }

    /// The lifted native lens, measured over flat grey and colour backdrops (lossless
    /// screenshots of a held press, motion probe): a circular-bevel rim of the height
    /// UIKit gives its displacement effect (11.2 pt) that pulls the bar edge in by
    /// 3 pt at 8 pt depth; a one-pixel ring at x0.70; a rim highlight of +49 then +19
    /// levels on the top and bottom, +14 at the ends; interior brightening of +4
    /// (light) / +10 (dark) over the pressed bar; a shadow band 4 pt below at -7
    /// levels. Points, converted per frame; see GlassLensBlend for the model.
    private static final float GLASS_LENS_BEVEL_PT = 11.2f;
    private static final float GLASS_LENS_MAX_SHIFT_PT = 30f;
    private static final float GLASS_LENS_DISPERSION = 0.08f;
    private static final float GLASS_LENS_OUTLINE = 0.30f;
    private static final float GLASS_LENS_OUTLINE_PT = 0.5f;
    private static final float GLASS_LENS_RIM = 22 / 255f;
    private static final float GLASS_LENS_RIM_VERTICAL = 40 / 255f;
    private static final float GLASS_LENS_RIM_PT = 0.6f;
    private static final float GLASS_LENS_END_SHADE = 0.05f;
    private static final float GLASS_LENS_END_SHADE_PT = 8f;
    private static final float GLASS_LENS_BRIGHT_LIGHT = 4 / 255f;
    private static final float GLASS_LENS_BRIGHT_DARK = 10 / 255f;
    private static final float GLASS_LENS_SHADOW = 0.055f;
    private static final float GLASS_LENS_SHADOW_PT = 4f;

    private float[] glassLensOptics(boolean dark) {
        float[] o = new float[GlassLensBlend.COUNT];
        o[GlassLensBlend.BEVEL] = glassPx(GLASS_LENS_BEVEL_PT);
        o[GlassLensBlend.MAX_SHIFT] = glassPx(GLASS_LENS_MAX_SHIFT_PT);
        o[GlassLensBlend.DISPERSION] = GLASS_LENS_DISPERSION;
        o[GlassLensBlend.OUTLINE] = GLASS_LENS_OUTLINE;
        o[GlassLensBlend.OUTLINE_WIDTH] = glassPx(GLASS_LENS_OUTLINE_PT);
        o[GlassLensBlend.RIM_LIGHT] = GLASS_LENS_RIM;
        o[GlassLensBlend.RIM_LIGHT_VERTICAL] = GLASS_LENS_RIM_VERTICAL;
        o[GlassLensBlend.RIM_WIDTH] = glassPx(GLASS_LENS_RIM_PT);
        o[GlassLensBlend.END_SHADE] = GLASS_LENS_END_SHADE;
        o[GlassLensBlend.END_SHADE_WIDTH] = glassPx(GLASS_LENS_END_SHADE_PT);
        o[GlassLensBlend.BRIGHTNESS] = dark ? GLASS_LENS_BRIGHT_DARK : GLASS_LENS_BRIGHT_LIGHT;
        o[GlassLensBlend.SHADOW] = GLASS_LENS_SHADOW;
        o[GlassLensBlend.SHADOW_WIDTH] = glassPx(GLASS_LENS_SHADOW_PT);
        return o;
    }

    /// Coverage masks for the vibrant content, reused between frames.
    private Image glassMaskDefault;
    private Image glassMaskSelected;

    /// The native iOS 27 bar is lit along its top and bottom edges: over flat grey
    /// the outermost rows read +42/+29/+18/+7 levels (dark) and +36/+26/+17/+8
    /// (light) above the interior, while its sides carry the dark outline instead
    /// (GlassRecipe.getOutline). Drawn as white rows that fade inwards over about
    /// 1.3 pt, only along the straight top and bottom runs and the upper and lower
    /// quarters of the rounded ends.
    private void paintGlassBarRim(Graphics g, int x, int y, int w, int h, float ptPx) {
        int fg = tabsContainer.getStyle().getFgColor();
        boolean dark = 0.2126f * ((fg >> 16) & 0xff) + 0.7152f * ((fg >> 8) & 0xff) + 0.0722f * (fg & 0xff) > 128;
        int peak = getUIManager().getThemeConstant(dark ? "tabGlassSpecularDarkAlphaInt" : "tabGlassSpecularAlphaInt", 0);
        if (peak <= 0 || w <= 0 || h <= 0) {
            return;
        }
        int oldColor = g.getColor();
        int oldAlpha = g.getAlpha();
        boolean aa = g.isAntiAliased();
        g.setAntiAliased(true);
        g.setColor(0xffffff);
        // Rows are about a third of a point each, like the native @3x pixels.
        int step = Math.max(1, Math.round(ptPx / 3f));
        int d = h;
        for (int i = 0; i < GLASS_RIM_FALLOFF.length; i++) {
            int alpha = Math.round(peak * GLASS_RIM_FALLOFF[i]);
            if (alpha <= 0) {
                continue;
            }
            g.setAlpha(alpha);
            int inset = i * step;
            int ix = x + inset;
            int iy = y + inset;
            int iw = w - 2 * inset;
            int ih = h - 2 * inset;
            int dd = d - 2 * inset;
            if (iw <= dd || ih <= 0) {
                break;
            }
            for (int t = 0; t < step; t++) {
                // straight top and bottom runs
                g.drawLine(ix + dd / 2, iy + t, ix + iw - dd / 2, iy + t);
                g.drawLine(ix + dd / 2, iy + ih - 1 - t, ix + iw - dd / 2, iy + ih - 1 - t);
                // upper and lower quarters of both rounded ends (45 degrees either side
                // of vertical, where the edge normal is mostly vertical)
                int ax = ix + t;
                int ay = iy + t;
                int aw = dd - 2 * t;
                g.drawArc(ax, ay, aw, aw, 90, 45);
                g.drawArc(ax, ay, aw, aw, 225, 45);
                g.drawArc(ix + iw - dd + t, ay, aw, aw, 45, 45);
                g.drawArc(ix + iw - dd + t, ay, aw, aw, 270, 45);
            }
        }
        g.setAntiAliased(aa);
        g.setColor(oldColor);
        g.setAlpha(oldAlpha);
    }

    /// Paints the tab content and the selection for the ios27 motion. Order, bottom
    /// to top, as UIKit composites it: grey platter under the lens, the tabs as
    /// UNSELECTED content everywhere outside the lens, the tabs as SELECTED (accent)
    /// content inside the lens magnified by the lift, then the lens rim and the
    /// touch glow. Everything except the rim and glow rides one transform that
    /// scales the whole bar about its centre.
    void paintGlassTabs(Graphics g) {
        Container tc = tabsContainer;
        GlassFrame f = glassFrame();
        TabGlassMotion m = f.motion;
        int fg = tc.getStyle().getFgColor();
        boolean dark = 0.2126f * ((fg >> 16) & 0xff) + 0.7152f * ((fg >> 8) & 0xff) + 0.0722f * (fg & 0xff) > 128;
        boolean vibrant = g.isColorMatrixRegionSupported();
        if (vibrant && m.platterOpacity > 0) {
            // The native platter is the bar's own glass seen through a colour matrix
            // (a CABackdropLayer filter), not a translucent fill; it fades out while
            // the lens is lifted.
            float s = f.barScale;
            int px = Math.round(scaleAbout(f.lensX, f.pivotX, s));
            int py = Math.round(scaleAbout(f.lensY, f.pivotY, s));
            int pr = Math.round(scaleAbout(f.lensX + f.lensW, f.pivotX, s));
            int pb = Math.round(scaleAbout(f.lensY + f.lensH, f.pivotY, s));
            g.colorMatrixRegion(px, py, pr - px, pb - py, dark ? GLASS_PLATTER_DARK : GLASS_PLATTER_LIGHT, null, -1,
                    m.platterOpacity);
        }
        if (vibrant && m.lift > 0) {
            // While pressed the whole native bar brightens by ~11 levels (measured over
            // flat grey in both appearances: 196 -> 207 light, 136 -> 147 dark).
            float s = f.barScale;
            int bx = Math.round(scaleAbout(f.pillX, f.pivotX, s));
            int by = Math.round(scaleAbout(f.pillY, f.pivotY, s));
            int br = Math.round(scaleAbout(f.pillX + f.pillW, f.pivotX, s));
            int bb = Math.round(scaleAbout(f.pillY + f.pillH, f.pivotY, s));
            float lift = getUIManager().getThemeConstant("tabGlassPressLiftInt", 11) / 255f;
            float[] brighten = {1, 0, 0, lift, 0, 1, 0, lift, 0, 0, 1, lift};
            g.colorMatrixRegion(bx, by, br - bx, bb - by, brighten, null, -1, m.lift);
        }
        boolean xf = f.barScale != 1f && g.isTransformSupported();
        Transform saved = null;
        if (xf) {
            saved = g.getTransform();
            Transform t = Transform.makeTranslation(f.pivotX, f.pivotY);
            t.scale(f.barScale, f.barScale);
            t.translate(-f.pivotX, -f.pivotY);
            g.transform(t);
        }
        boolean aa = g.isAntiAliased();
        int oldColor = g.getColor();
        int oldAlpha = g.getAlpha();
        g.setAntiAliased(true);

        // Selection platter fallback for ports without colorMatrixRegion; a dark bar
        // has its own platter constants, falling back to the light ones.
        UIManager uim = getUIManager();
        int pillColor = uim.getThemeConstant("tabSelPillColorInt", 0x767680);
        int pillAlpha = uim.getThemeConstant("tabSelPillAlphaInt", 34);
        if (dark) {
            pillColor = uim.getThemeConstant("tabSelPillColorDarkInt", pillColor);
            pillAlpha = uim.getThemeConstant("tabSelPillAlphaDarkInt", pillAlpha);
        }
        int platterAlpha = Math.round(pillAlpha * m.platterOpacity);
        int lx = Math.round(f.lensX);
        int ly = Math.round(f.lensY);
        int lw = Math.round(f.lensX + f.lensW) - lx;
        int lh = Math.round(f.lensY + f.lensH) - ly;
        int radius = Math.min(lw, lh);
        if (!vibrant && platterAlpha > 0 && lw > 0 && lh > 0) {
            g.setColor(pillColor);
            g.setAlpha(platterAlpha);
            g.fillRoundRect(lx, ly, lw, lh, radius, radius);
        }
        // The lifted lens is clear glass and reads BRIGHTER than the bar around it
        // (+8 levels light, +11 dark over flat grey on the native bar).
        int lensLight = Math.round(uim.getThemeConstant("tabGlassLensLightAlphaInt", 28) * m.lift);
        if (lensLight > 0 && lw > 0 && lh > 0 && !g.isGlassLensRegionSupported()) {
            g.setColor(0xffffff);
            g.setAlpha(lensLight);
            g.fillRoundRect(lx, ly, lw, lh, radius, radius);
        }
        g.setColor(oldColor);
        g.setAlpha(oldAlpha);

        // Content, in tabsContainer-relative coordinates like Container.paint.
        int ox = tc.getX();
        int oy = tc.getY();
        if (!vibrant) {
            g.translate(ox, oy);
            paintGlassContent(g, g, lx - ox, ly - oy, lw, lh, m.contentScale);
            g.translate(-ox, -oy);
        }

        g.setAntiAliased(aa);
        if (xf) {
            g.setTransform(saved);
        }
        if (vibrant) {
            paintGlassContentVibrant(g, f, dark, lx - ox, ly - oy, lw, lh);
        }
        if (m.lift > 0 && g.isGlassLensRegionSupported()) {
            float s = f.barScale;
            int gx = Math.round(scaleAbout(f.lensX, f.pivotX, s));
            int gy = Math.round(scaleAbout(f.lensY, f.pivotY, s));
            int gr = Math.round(scaleAbout(f.lensX + f.lensW, f.pivotX, s));
            int gb = Math.round(scaleAbout(f.lensY + f.lensH, f.pivotY, s));
            g.glassLensRegion(gx, gy, gr - gx, gb - gy, -1, glassLensOptics(dark), m.lift);
        }
        paintGlassRimAndGlow(g, f);
        g.setColor(oldColor);
        g.setAlpha(oldAlpha);
    }

    /// Paints every tab twice through complementary clips: unselected outside the
    /// lens capsule, selected (accent, scaled by `contentScale` about each tab's
    /// centre) inside it. The capsule's round ends are tiled with thin horizontal
    /// bands -- a staircase whose steps are about a point tall -- because the
    /// complement of a capsule is not convex and a native shape clip only handles
    /// convex polygons; plain rectangles are exact on every port. Bands only cover
    /// the rows the tab content occupies, and a tab is only painted into a band it
    /// intersects.
    private void paintGlassContent(Graphics g, Graphics sel, int lx, int ly, int lw, int lh, float contentScale) {
        Container tc = tabsContainer;
        int n = tc.getComponentCount();
        int w = tc.getWidth();
        // Rows holding tab content.
        int top = Integer.MAX_VALUE;
        int bottom = Integer.MIN_VALUE;
        for (int i = 0; i < n; i++) {
            Component c = tc.getComponentAt(i);
            top = Math.min(top, c.getY());
            bottom = Math.max(bottom, c.getY() + c.getHeight());
        }
        if (top >= bottom) {
            return;
        }
        int lensTop = Math.max(top, ly);
        int lensBottom = Math.min(bottom, ly + lh);
        // Above and below the lens: unselected, full width.
        paintGlassRegion(g, 0, top, w, lensTop - top, Button.GLASS_PAINT_DEFAULT, 1f);
        paintGlassRegion(g, 0, lensBottom, w, bottom - lensBottom, Button.GLASS_PAINT_DEFAULT, 1f);
        if (lensBottom > lensTop) {
            // Left and right of the lens box: unselected.
            paintGlassRegion(g, 0, lensTop, lx, lensBottom - lensTop, Button.GLASS_PAINT_DEFAULT, 1f);
            paintGlassRegion(g, lx + lw, lensTop, w - lx - lw, lensBottom - lensTop, Button.GLASS_PAINT_DEFAULT, 1f);
            float r = Math.min(lw, lh) / 2f;
            float mid = ly + lh / 2f;
            float step = Math.max(1f, tc.getHeight() / 62f);
            int y = lensTop;
            while (y < lensBottom) {
                int inset = capsuleInset(y + 0.5f, mid, r, step);
                int y2 = y + 1;
                while (y2 < lensBottom && capsuleInset(y2 + 0.5f, mid, r, step) == inset) {
                    y2++;
                }
                int bh = y2 - y;
                if (inset > 0) {
                    paintGlassRegion(g, lx, y, inset, bh, Button.GLASS_PAINT_DEFAULT, 1f);
                    paintGlassRegion(g, lx + lw - inset, y, inset, bh, Button.GLASS_PAINT_DEFAULT, 1f);
                }
                paintGlassRegion(sel, lx + inset, y, lw - 2 * inset, bh, Button.GLASS_PAINT_SELECTED, contentScale);
                y = y2;
            }
        }
    }

    /// The native tab content is VIBRANT: each glyph pixel is a colour matrix of
    /// whatever lies behind it, weighted by the glyph's coverage (see
    /// VibrancyMatrix), so the unselected labels and the accent through the lens
    /// pick up the glass under them instead of being flat paint. The content is
    /// painted into two coverage masks -- unselected outside the lens, selected
    /// (accent, magnified) inside it -- in the bar's scaled frame, and each mask
    /// then drives Graphics.colorMatrixRegion with the matrix for its tint.
    private void paintGlassContentVibrant(Graphics g, GlassFrame f, boolean dark, int lx, int ly, int lw, int lh) {
        Container tc = tabsContainer;
        int mw = tc.getWidth();
        int mh = tc.getHeight();
        if (mw <= 0 || mh <= 0) {
            return;
        }
        // The masks are the bar's own size, unscaled, so they are allocated once;
        // colorMatrixRegion stretches them over the pulsing bar.
        // Each tab is painted once per mask; then the lens capsule is cut out of the
        // unselected mask and everything outside it out of the selected one. One
        // mask is finished before the other is touched (see cutGlassCapsule).
        glassMaskDefault = clearedGlassMask(glassMaskDefault, mw, mh);
        Graphics dg = glassMaskDefault.getGraphics();
        paintGlassMaskTabs(dg, Button.GLASS_PAINT_DEFAULT, 1f);
        cutGlassCapsule(dg, lx, ly, lw, lh, mw, mh, false);
        glassMaskSelected = clearedGlassMask(glassMaskSelected, mw, mh);
        Graphics sg = glassMaskSelected.getGraphics();
        paintGlassMaskTabs(sg, Button.GLASS_PAINT_SELECTED, f.motion.contentScale);
        cutGlassCapsule(sg, lx, ly, lw, lh, mw, mh, true);
        float s = f.barScale;
        int rx = Math.round(scaleAbout(tc.getX(), f.pivotX, s));
        int ry = Math.round(scaleAbout(tc.getY(), f.pivotY, s));
        int rr = Math.round(scaleAbout(tc.getX() + mw, f.pivotX, s));
        int rb = Math.round(scaleAbout(tc.getY() + mh, f.pivotY, s));
        int defaultTint = glassContentTint(Button.GLASS_PAINT_DEFAULT);
        int selectedTint = glassContentTint(Button.GLASS_PAINT_SELECTED);
        g.colorMatrixRegion(rx, ry, rr - rx, rb - ry, VibrancyMatrix.forTint(defaultTint, dark), glassMaskDefault, 0, 1f);
        g.colorMatrixRegion(rx, ry, rr - rx, rb - ry, VibrancyMatrix.forTint(selectedTint, dark), glassMaskSelected, 0, 1f);
    }

    /// Paints every tab once into a mask, in the given glass paint state, each
    /// scaled by `scale` about its own centre (the accent copy's magnification).
    private void paintGlassMaskTabs(Graphics mg, int state, float scale) {
        Container tc = tabsContainer;
        int n = tc.getComponentCount();
        for (int i = 0; i < n; i++) {
            Component c = tc.getComponentAt(i);
            Button b = c instanceof Button ? (Button) c : null;
            int oldState = b == null ? 0 : b.glassPaintState;
            if (b != null) {
                b.glassPaintState = state;
            }
            boolean xf = scale != 1f && mg.isTransformSupported();
            if (xf) {
                float px = c.getX() + c.getWidth() / 2f;
                float py = c.getY() + c.getHeight() / 2f;
                Transform t = Transform.makeTranslation(px, py);
                t.scale(scale, scale);
                t.translate(-px, -py);
                mg.setTransform(t);
            }
            try {
                mg.setClip(0, 0, tc.getWidth(), tc.getHeight());
                c.paintInternal(mg, false);
            } finally {
                if (xf) {
                    mg.setTransform(Transform.makeIdentity());
                }
                if (b != null) {
                    b.glassPaintState = oldState;
                }
            }
        }
    }

    /// Clears the lens capsule (lx, ly, lw, lh, tabsContainer coordinates) out of
    /// a mask, or with `keepInside` everything but the capsule: whole rectangles
    /// around the lens box, then one strip per pixel row across the round ends.
    /// Callers finish one mask before touching the other -- on ports that render
    /// images on the GPU, every switch between two images' graphics closes one
    /// drawing pass and opens another.
    private static void cutGlassCapsule(Graphics g, int lx, int ly, int lw, int lh, int mw, int mh,
            boolean keepInside) {
        g.setClip(0, 0, mw, mh);
        if (lw <= 0 || lh <= 0) {
            if (keepInside) {
                g.clearRect(0, 0, mw, mh);
            }
            return;
        }
        if (keepInside) {
            g.clearRect(0, 0, mw, Math.max(0, ly));
            g.clearRect(0, ly + lh, mw, Math.max(0, mh - ly - lh));
            g.clearRect(0, ly, Math.max(0, lx), lh);
            g.clearRect(lx + lw, ly, Math.max(0, mw - lx - lw), lh);
        }
        cutCapsuleRows(g, lx, ly, lw, lh, keepInside);
    }

    /// Per pixel row of the capsule, clears either its two round-end slivers
    /// (`ends`) or its interior.
    private static void cutCapsuleRows(Graphics g, int lx, int ly, int lw, int lh, boolean ends) {
        float r = Math.min(lw, lh) / 2f;
        float mid = ly + lh / 2f;
        int y = ly;
        while (y < ly + lh) {
            int inset = capsuleInset(y + 0.5f, mid, r, 1f);
            int y2 = y + 1;
            while (y2 < ly + lh && capsuleInset(y2 + 0.5f, mid, r, 1f) == inset) {
                y2++;
            }
            if (ends) {
                if (inset > 0) {
                    g.clearRect(lx, y, inset, y2 - y);
                    g.clearRect(lx + lw - inset, y, inset, y2 - y);
                }
            } else if (lw - 2 * inset > 0) {
                g.clearRect(lx + inset, y, lw - 2 * inset, y2 - y);
            }
            y = y2;
        }
    }

    /// A transparent mask of the given size, reusing the previous frame's when it fits.
    private static Image clearedGlassMask(Image mask, int w, int h) {
        if (mask == null || mask.getWidth() != w || mask.getHeight() != h) {
            return Image.createImage(w, h, 0);
        }
        Graphics mg = mask.getGraphics();
        if (mg.isTransformSupported()) {
            mg.setTransform(Transform.makeIdentity());
        }
        mg.setClip(0, 0, w, h);
        mg.clearRect(0, 0, w, h);
        return mask;
    }

    /// The foreground colour the tab content is painted with in the given glass
    /// paint state (the unselected label colour, or the accent).
    private int glassContentTint(int state) {
        Container tc = tabsContainer;
        int n = tc.getComponentCount();
        for (int i = 0; i < n; i++) {
            Component c = tc.getComponentAt(i);
            if (c instanceof Button) {
                Button b = (Button) c;
                int old = b.glassPaintState;
                b.glassPaintState = state;
                try {
                    return b.getStyle().getFgColor();
                } finally {
                    b.glassPaintState = old;
                }
            }
        }
        return state == Button.GLASS_PAINT_SELECTED ? 0x0091ff : 0;
    }

    /// Horizontal inset of a capsule of radius `r` from its bounding box at row
    /// `y`, quantized to `step` pixels so neighbouring rows merge into one band.
    private static int capsuleInset(float y, float mid, float r, float step) {
        float dy = Math.abs(y - mid);
        float inset = dy >= r ? r : r - (float) Math.sqrt(r * r - dy * dy);
        return Math.round(Math.round(inset / step) * step);
    }

    private void paintGlassRegion(Graphics g, int x, int y, int w, int h, int state, float scale) {
        if (w <= 0 || h <= 0) {
            return;
        }
        int clipX = g.getClipX();
        int clipY = g.getClipY();
        int clipW = g.getClipWidth();
        int clipH = g.getClipHeight();
        try {
            paintGlassRegionClipped(g, x, y, w, h, state, scale, clipX, clipY, clipW, clipH);
        } finally {
            g.setClip(clipX, clipY, clipW, clipH);
        }
    }

    private void paintGlassRegionClipped(Graphics g, int x, int y, int w, int h, int state, float scale,
            int clipX, int clipY, int clipW, int clipH) {
        Container tc = tabsContainer;
        g.clipRect(x, y, w, h);
        if (g.getClipWidth() <= 0 || g.getClipHeight() <= 0) {
            return;
        }
        int n = tc.getComponentCount();
        for (int i = 0; i < n; i++) {
            Component c = tc.getComponentAt(i);
            int grow = scale == 1f ? 0 : Math.round(Math.max(c.getWidth(), c.getHeight()) * (scale - 1f) / 2f);
            if (c.getX() - grow >= x + w || c.getX() + c.getWidth() + grow <= x
                    || c.getY() - grow >= y + h || c.getY() + c.getHeight() + grow <= y) {
                continue;
            }
            Button b = c instanceof Button ? (Button) c : null;
            int oldState = b == null ? 0 : b.glassPaintState;
            if (b != null) {
                b.glassPaintState = state;
            }
            Transform saved = null;
            boolean xf = scale != 1f && g.isTransformSupported();
            if (xf) {
                saved = g.getTransform();
                float px = c.getX() + c.getWidth() / 2f;
                float py = c.getY() + c.getHeight() / 2f;
                Transform t = Transform.makeTranslation(px, py);
                t.scale(scale, scale);
                t.translate(-px, -py);
                g.transform(t);
            }
            try {
                c.paintInternal(Display.impl.getComponentScreenGraphics(tc, g), false);
            } finally {
                if (xf) {
                    g.setTransform(saved);
                }
                if (b != null) {
                    b.glassPaintState = oldState;
                }
            }
            g.setClip(clipX, clipY, clipW, clipH);
            g.clipRect(x, y, w, h);
        }
    }

    /// The lifted lens's bright rim and the touch glow. Both are drawn in the frame
    /// directly (manually scaled by the bar pulse) after the transformed content.
    private void paintGlassRimAndGlow(Graphics g, GlassFrame f) {
        TabGlassMotion m = f.motion;
        float s = f.barScale;
        boolean aa = g.isAntiAliased();
        g.setAntiAliased(true);
        if (m.lift > 0.01f && !g.isGlassLensRegionSupported()) {
            float x = scaleAbout(f.lensX, f.pivotX, s);
            float y = scaleAbout(f.lensY, f.pivotY, s);
            float w = f.lensW * s;
            float h = f.lensH * s;
            // Without glassLensRegion: an approximation of the native edge -- a thin
            // dark ring on the boundary and a bright rim just inside it.
            int rimAlpha = Math.round(getUIManager().getThemeConstant("tabGlassRimAlphaInt", 150) * m.lift);
            int edgeAlpha = Math.round(getUIManager().getThemeConstant("tabGlassEdgeAlphaInt", 110) * m.lift);
            int ix = Math.round(x);
            int iy = Math.round(y);
            int iw = Math.round(x + w) - ix;
            int ih = Math.round(y + h) - iy;
            int rr = Math.min(iw, ih);
            if (edgeAlpha > 0) {
                g.setColor(0x000000);
                g.setAlpha(edgeAlpha);
                g.drawRoundRect(ix, iy, iw - 1, ih - 1, rr, rr);
            }
            if (rimAlpha > 0 && iw > 4 && ih > 4) {
                g.setColor(0xffffff);
                g.setAlpha(rimAlpha);
                g.drawRoundRect(ix + 1, iy + 1, iw - 3, ih - 3, rr - 2, rr - 2);
            }
        }
        if (m.glowOpacity > 0.003f) {
            float d = TabGlassMotion.GLOW_DIAMETER_PT * m.glowScale * f.ptPx * s;
            int alpha = Math.round(255 * m.glowOpacity * getUIManager().getThemeConstant("tabGlassGlowPct", 40) / 100f);
            if (alpha > 0) {
                int ocx = g.getClipX();
                int ocy = g.getClipY();
                int ocw = g.getClipWidth();
                int och = g.getClipHeight();
                int px = Math.round(scaleAbout(f.pillX, f.pivotX, s));
                int py = Math.round(scaleAbout(f.pillY, f.pivotY, s));
                g.clipRect(px, py, Math.round(f.pillW * s), Math.round(f.pillH * s));
                float gx = scaleAbout(f.glowX, f.pivotX, s);
                float gy = scaleAbout(f.glowY, f.pivotY, s);
                // A soft disc: nested translucent discs whose alphas sum to `alpha` at
                // the centre and fall off linearly to nothing at the rim.
                g.setColor(0xffffff);
                int rings = 16;
                int painted = 0;
                for (int i = rings; i >= 1; i--) {
                    // cumulative alpha at ring i is alpha * (rings - i + 1) / rings
                    int target = alpha * (rings - i + 1) / rings;
                    int a = target - painted;
                    if (a <= 0) {
                        continue;
                    }
                    painted = target;
                    g.setAlpha(a);
                    float rd = d / 2f * i / rings;
                    int dd = Math.round(rd * 2);
                    g.fillArc(Math.round(gx - rd), Math.round(gy - rd), dd, dd, 0, 360);
                }
                g.setClip(ocx, ocy, ocw, och);
            }
        }
        g.setAntiAliased(aa);
    }

    /// The iOS "selected cell" background: a subtle grey capsule kept at bar height
    /// (the lens drop, drawn over it, bulges taller). Travels + elongates with the
    /// drop. systemFill grey so it reads neutral, not blue. Alpha via tabSelPillAlphaInt.
    private void drawSelectionPill(Graphics g, int capX, int capY, int w, int capH, float bump) {
        int pillInset = capH * 7 / 100;                 // pill a hair shorter than the lens
        int py = capY + pillInset;
        int ph = capH - 2 * pillInset;
        if (ph <= 0) {
            return;
        }
        // FADE the grey pill out as the drop travels: the settled "selected cell"
        // background is grey, but MID-FLIGHT the bubble is pure transparent glass
        // (otherwise the grey shows through the gap between tabs as an empty blob).
        int baseAlpha = getUIManager().getThemeConstant("tabSelPillAlphaInt", 34);
        int alpha = (int) (baseAlpha * (1f - 0.85f * bump));
        if (alpha <= 0) {
            return;
        }
        int oldC = g.getColor();
        int oldA = g.getAlpha();
        boolean aa = g.isAntiAliased();
        g.setAntiAliased(true);
        g.setColor(getUIManager().getThemeConstant("tabSelPillColorInt", 0x767680));
        g.setAlpha(alpha);
        g.fillRoundRect(capX, py, w, ph, ph, ph);
        g.setAntiAliased(aa);
        g.setColor(oldC);
        g.setAlpha(oldA);
    }

    /// Draws the iOS 26 sliding selection capsule -- a single Liquid Glass blob
    /// behind the selected tab that tweens between tabs on selection change (reusing
    /// the indicator motion). Painted BEHIND the tab content so the icon/label sit on
    /// top. Opt in with `tabsSelectionCapsuleBool`. The glass material is rendered via
    /// Graphics.glassRegion when `glassMaterialBool` is set (iOS); other platforms get
    /// a translucent rounded-capsule fallback. The selected tab's own background must
    /// be transparent so only this single capsule shows.
    void paintSelectionCapsule(Graphics g) {
        if (!selectionCapsule || tabsContainer == null || tabsContainer.getComponentCount() == 0) {
            return;
        }
        if (activeComponent < 0 || activeComponent >= tabsContainer.getComponentCount()) {
            return;
        }
        // The selection capsule should fill (almost) the full pill height like native --
        // a large inset leaves a bright bar-frost band above/below it (reads as a
        // separate inset pill / "ring"). Tunable via tabSelInsetMm (default a hair).
        int inset = selectionCapsuleInsetPx();
        // Bar vertical geometry: span the OUTER pill height (not the padded inner box) so
        // the capsule reaches the pill edge like native -- using getInnerY()/Height()
        // leaves the bar's padding as a bright frost band above/below (the visible "ring").
        int padTopPx = tabsContainer.getInnerY() - tabsContainer.getY();
        int padBotPx = (tabsContainer.getY() + tabsContainer.getHeight())
                - (tabsContainer.getInnerY() + tabsContainer.getInnerHeight());
        int innerX = tabsContainer.getInnerX();
        int capYBase = tabsContainer.getInnerY() - padTopPx + inset;
        int capHBase = tabsContainer.getInnerHeight() + padTopPx + padBotPx - 2 * inset;
        if (capHBase <= 0) {
            return;
        }

        // Source/target cell bounds (inner-x space). morphTestValue (>=0) renders a fixed
        // LINEAR-TIME progress for the probe; otherwise the live motion drives t. When not
        // animating we settle by asking the model for t=1 with from==to==the active cell.
        int fromX;
        int fromW;
        int toX;
        int toW;
        float t;
        if (morphTestValue >= 0 || indicatorAnimMotion != null) {
            int v = morphTestValue >= 0 ? morphTestValue : indicatorAnimMotion.getValue();
            t = (v < 0 ? 0 : (v > 100 ? 100 : v)) / 100f;
            fromX = indicatorFromX;
            fromW = indicatorFromW;
            toX = indicatorToX;
            toW = indicatorToW;
        } else {
            int[] cb = new int[2];
            capsuleCellBounds(activeComponent, inset, cb);
            fromX = cb[0];
            fromW = cb[1];
            toX = cb[0];
            toW = cb[1];
            t = 1f;
        }

        // Whole-bar extent (paint space) for the grow pass.
        int nTabs = tabsContainer.getComponentCount();
        int[] cb0 = new int[2];
        int[] cbN = new int[2];
        capsuleCellBounds(0, inset, cb0);
        capsuleCellBounds(nTabs - 1, inset, cbN);
        int barLeftX = innerX + cb0[0];
        int barRightX = innerX + cbN[0] + cbN[1];

        // The whole frame -- pill rect, lens rect + params, bar-grow rect -- is produced by
        // the pure, unit-tested TabSelectionMorph model so the motion can be validated
        // deterministically (see TabSelectionMorphTest / the fidelity animation-frame probe).
        TabSelectionMorph m = TabSelectionMorph.compute(t, fromX, fromW, toX, toW,
                innerX, capYBase, capHBase, barLeftX, barRightX, morphTokens());
        if (m.capW <= 0 || m.capH <= 0) {
            return;
        }

        // Dark/light by the bar's fg luma -- the TabsContainer fg is distinguishable
        // (text-secondary), unlike the accent-blue selected-tab fg.
        int fg = tabsContainer.getStyle().getFgColor();
        int fgLuma = (int) (0.2126f * ((fg >> 16) & 0xff) + 0.7152f * ((fg >> 8) & 0xff) + 0.0722f * (fg & 0xff));
        boolean dark = fgLuma > 128;
        if (getUIManager().isThemeConstant("glassMaterialBool", false)) {
            // iOS 26 selection DROP: a subtle grey selection PILL at bar height plus a glass
            // LENS painted OVER the (dark) glyphs that magnifies + chromatically aberrates +
            // dark->accent tints the content beneath, so the blue exists ONLY inside the
            // drop. A brief WHOLE-BAR GROW (uniform magnify, no tint) swells the bar at the
            // very start. All rects/params come from the morph model above.
            if (m.barGrow) {
                g.lensRegion(m.barGrowX, m.barGrowY, m.barGrowW, m.barGrowH,
                        -1f, m.barGrowMag, 0f, 0x000000, 0f);
            }
            drawSelectionPill(g, m.capX, m.capY, m.capW, m.capH, m.flight);
            // Accent supplied by the lens in LIGHT mode only: the keying tints DARK
            // pixels toward the accent, which is right over a light frost (the
            // deliberately-dark glyphs turn blue) but floods a dark bar solid blue,
            // because everything under the drop is dark there. On dark bars the
            // glyphs carry the accent directly (theme) and the lens keeps only its
            // magnify/aberration optics.
            int tint = getUIManager().getThemeConstant("tabSelLensTintColorInt", 0x0a84ff);
            g.lensRegion(m.lensX, m.lensY, m.lensW, m.lensH, -1f, m.magnify, m.aberration, tint, dark ? 0f : m.tintStrength);
            return;
        }
        // Non-glass platforms: a translucent rounded capsule.
        int oldA = g.getAlpha();
        int oldC = g.getColor();
        boolean aa = g.isAntiAliased();
        g.setAntiAliased(true);
        g.setColor(dark ? 0x8e8e93 : 0xffffff);
        g.setAlpha(dark ? 120 : 205);
        g.fillRoundRect(m.capX, m.capY, m.capW, m.capH, m.capH, m.capH);
        g.setAntiAliased(aa);
        g.setColor(oldC);
        g.setAlpha(oldA);
    }

    /// Resolves the selection-morph tokens from the theme's HIGH-LEVEL controls
    /// only (review: fewer, coherent morph knobs): tabsMorphPreset picks a named
    /// envelope set inside the motion model ("ios26" default / "subtle"),
    /// tabsMorphLensIntensityPct scales the lens optics around the preset (100 =
    /// as authored) and tabsMorphSpringPct scales the settle overshoot (100 =
    /// preset bounce, 0 = plain stop). Duration remains
    /// tabsAnimatedIndicatorDurationInt. The mm lengths carried by the preset
    /// are converted to px here so the model stays Display-free.
    private TabSelectionMorph.Tokens morphTokens() {
        UIManager uim = getUIManager();
        TabSelectionMorph.Tokens tk = TabSelectionMorph.Tokens.preset(
                uim.getThemeConstant("tabsMorphPreset", "ios26"));
        tk.scaleLensIntensity(uim.getThemeConstant("tabsMorphLensIntensityPct", 100) / 100f);
        tk.spring = uim.getThemeConstant("tabsMorphSpringPct", 100) / 100f;
        tk.liftPx = Display.getInstance().convertToPixels(tk.liftMm);
        tk.downBiasPx = Display.getInstance().convertToPixels(tk.downBiasMm);
        return tk;
    }

    /// Draws the animated indicator inside `tabsContainer`'s paint flow. Called
    /// from the inner `Container` subclass installed as `tabsContainer`.
    void paintAnimatedIndicator(Graphics g) {
        if (!animatedIndicator || tabsContainer.getComponentCount() == 0) {
            return;
        }
        int x;
        int w;
        if (indicatorAnimMotion != null) {
            int v = indicatorAnimMotion.getValue();    // 0..100
            x = indicatorFromX + ((indicatorToX - indicatorFromX) * v / 100);
            w = indicatorFromW + ((indicatorToW - indicatorFromW) * v / 100);
        } else {
            // At rest: pin to the currently-selected tab.
            Component active = tabsContainer.getComponentAt(activeComponent);
            x = active.getX();
            w = active.getWidth();
        }
        // Read as a FLOAT mm value: the Material 3 indicator is ~0.45mm, but an
        // int read truncates fractional millimetres (and a non-integer constant
        // like "0.45" fails int parsing, silently falling back to the 1mm default
        // -- a ~2x-too-thick indicator). Parse the string form so sub-mm
        // thicknesses survive.
        float thicknessMm = animatedIndicatorThicknessMm;
        try {
            thicknessMm = Float.parseFloat(getUIManager().getThemeConstant(
                    "tabsAnimatedIndicatorThicknessMm", String.valueOf(animatedIndicatorThicknessMm)));
        } catch (NumberFormatException ignore) {
            // malformed constant -> keep the default indicator thickness
        }
        int thickness = Display.getInstance().convertToPixels(thicknessMm);
        // Use TabIndicator UIID color when its fg is set; otherwise pull
        // from the selected tab's foreground. `getComponentStyle(...)`
        // never returns null -- it synthesises an empty Style if no
        // matching UIID exists -- so a `null` check on the result would
        // be redundant.
        int color;
        Style indicatorStyle = getUIManager().getComponentStyle("TabIndicator");
        if (indicatorStyle.getFgColor() != 0) {
            color = indicatorStyle.getFgColor();
        } else {
            Component active = tabsContainer.getComponentAt(activeComponent);
            color = active.getSelectedStyle().getFgColor();
        }
        int oldAlpha = g.getAlpha();
        int oldColor = g.getColor();
        g.setColor(color);
        g.setAlpha(255);
        int y = tabsContainer.getInnerY() + tabsContainer.getInnerHeight() - thickness;
        // Material 3 draws the active indicator as a SHORT rounded pill matching the
        // selected tab's LABEL width (not the full tab cell). Opt in with
        // tabsIndicatorPillBool; legacy themes keep the full-width square line.
        int indX = tabsContainer.getInnerX() + x;
        int indW = w;
        boolean pill = getUIManager().isThemeConstant("tabsIndicatorPillBool", false);
        if (pill) {
            Component active = tabsContainer.getComponentAt(activeComponent);
            if (active instanceof Button) {
                Button ab = (Button) active;
                // stringWidth is the glyph ADVANCE, a few px wider than the visible
                // ink; Material's indicator matches the ink width, so trim a hair.
                int textW = ab.getStyle().getFont().stringWidth(ab.getText())
                        - Display.getInstance().convertToPixels(0.45f);
                if (textW > 0 && textW < w) {
                    indW = textW;
                    indX = tabsContainer.getInnerX() + x + (w - indW) / 2;
                }
            }
            boolean priorAa = g.isAntiAliased();
            g.setAntiAliased(true);
            g.fillRoundRect(indX, y, indW, thickness, thickness, thickness);
            g.setAntiAliased(priorAa);
        } else {
            g.fillRect(indX, y, indW, thickness);
        }
        g.setColor(oldColor);
        g.setAlpha(oldAlpha);
    }

    /// Hide the tabs bar
    public void hideTabs() {
        removeComponent(tabsContainerHost != null ? tabsContainerHost : tabsContainer);
        revalidateLater();
    }

    /// Show the tabs bar if it was hidden
    public void showTabs() {
        int tp = tabPlacement;
        tabPlacement = -1;
        setTabPlacement(tp);
        revalidateLater();
    }

    /// Returns true if the swipe between tabs is activated, this is relevant for
    /// touch devices only
    ///
    /// #### Returns
    ///
    /// swipe activated flag
    public boolean isSwipeActivated() {
        return swipeActivated;
    }

    /// Setter method for swipe mode
    ///
    /// #### Parameters
    ///
    /// - `swipeActivated`
    public void setSwipeActivated(boolean swipeActivated) {
        if (this.swipeActivated != swipeActivated) {
            this.swipeActivated = swipeActivated;
            if (isInitialized()) {
                TopLevelContainer form = getTopLevelContainer();
                if (form != null) {
                    if (swipeActivated) {
                        form.asContainer().addPointerPressedListener(press);
                        form.asContainer().addPointerReleasedListener(release);
                        form.asContainer().addPointerDraggedListener(drag);
                    } else {
                        form.asContainer().removePointerPressedListener(press);
                        form.asContainer().removePointerReleasedListener(release);
                        form.asContainer().removePointerDraggedListener(drag);
                    }
                }
            }
        }
    }

    private void initTabsFocus() {
        for (int i = 0; i < tabsContainer.getComponentCount(); i++) {
            initTabFocus(tabsContainer.getComponentAt(i), contentPane.getComponentAt(activeComponent));
        }

    }

    private void initTabFocus(Component tab, Component content) {
        if (content.isFocusable()) {
            tab.setFocusable(true);
            return;
        }

        if (content instanceof Container) {
            Component focus = ((Container) content).findFirstFocusable();
            if (focus != null) {
                tab.setFocusable(true);
            }
        }

    }

    /// Indicates that a tab should change when the focus changes without the user physically pressing a button
    ///
    /// #### Returns
    ///
    /// the changeTabOnFocus
    public boolean isChangeTabOnFocus() {
        return changeTabOnFocus;
    }

    /// Indicates that a tab should change when the focus changes without the user physically pressing a button
    ///
    /// #### Parameters
    ///
    /// - `changeTabOnFocus`: the changeTabOnFocus to set
    public void setChangeTabOnFocus(boolean changeTabOnFocus) {
        this.changeTabOnFocus = changeTabOnFocus;
    }

    /// Indicates that the tabs container should have its style changed to the selected style when one of the tabs has focus
    /// this allows incorporating it into the theme of the application
    ///
    /// #### Returns
    ///
    /// the changeTabContainerStyleOnFocus
    public boolean isChangeTabContainerStyleOnFocus() {
        return changeTabContainerStyleOnFocus;
    }

    /// Indicates that the tabs container should have its style changed to the selected style when one of the tabs has focus
    /// this allows incorporating it into the theme of the application
    ///
    /// #### Parameters
    ///
    /// - `changeTabContainerStyleOnFocus`: the changeTabContainerStyleOnFocus to set
    public void setChangeTabContainerStyleOnFocus(boolean changeTabContainerStyleOnFocus) {
        this.changeTabContainerStyleOnFocus = changeTabContainerStyleOnFocus;
    }

    /// This method allows setting the Tabs content pane spacing (right and left),
    /// This can be used to create an effect where the selected tab is smaller
    /// and the right and left tabs are visible on the sides
    ///
    /// #### Parameters
    ///
    /// - `tabsGap`: @param tabsGap the gap on the sides of the content in pixels, the value must
    /// be positive.
    public void setTabsContentGap(int tabsGap) {
        if (tabsGap < 0) {
            throw new IllegalArgumentException("gap must be positive");
        }
        this.tabsGap = tabsGap;
    }

    private void setTabsLayout(int tabPlacement) {
        if ((tabPlacement == TOP || tabPlacement == BOTTOM) && isGlassMotion()) {
            tabsContainer.setLayout(new GlassTabsLayout());
            tabsContainer.setScrollableX(false);
            tabsContainer.setScrollableY(false);
            return;
        }
        if (tabPlacement == TOP || tabPlacement == BOTTOM) {
            // Equal-width cells filling the row (native UITabBar even spacing): a
            // NON-scrolling GridLayout divides the row width into equal columns, so a
            // longer label can't widen its cell. (A scrolling grid sizes to the widest
            // cell and overflows; fill-rows leaves cells content-sized.)
            if (tabsEqualWidth) {
                tabsContainer.setLayout(new GridLayout(1, Math.max(1, getTabCount())));
                tabsContainer.setScrollableX(false);
                tabsContainer.setScrollableY(false);
                return;
            }
            if (tabsFillRows) {
                FlowLayout f = new FlowLayout();
                f.setFillRows(true);
                tabsContainer.setLayout(f);
            } else {
                if (tabsGridLayout) {
                    tabsContainer.setLayout(new GridLayout(1, Math.max(1, getTabCount())));
                } else {
                    tabsContainer.setLayout(new BoxLayout(BoxLayout.X_AXIS));
                }
            }
            tabsContainer.setScrollableX(true);
            tabsContainer.setScrollableY(false);
        } else { // LEFT Or RIGHT
            if (tabsGridLayout) {
                tabsContainer.setLayout(new GridLayout(Math.max(1, getTabCount()), 1));
            } else {
                tabsContainer.setLayout(new BoxLayout(BoxLayout.Y_AXIS));
            }
            tabsContainer.setScrollableX(false);
            tabsContainer.setScrollableY(true);
        }
    }

    /// The UIID for a tab component which defaults to Tab
    ///
    /// #### Returns
    ///
    /// the tabUIID
    public String getTabUIID() {
        return tabUIID;
    }

    /// The UIID for a tab button which defaults to Tab.
    /// Tab buttons used to have two separate styles for selected and unselected. This was later consolidated so
    /// the tabs behave as a single toggle button (radio button) however one thing that remained is a call to
    /// `setUIID` that is implicitly made to restore the original "Tab" style.
    ///
    /// Effectively Tabs invokes the `setUIID` call on the Tab switch so if you want to manipulate
    /// the tab UIID manually (have one red and one green tab) this is a problem..
    ///
    /// To enable such code add all the tabs then just just invoke `setTabUIID(null)` to disable
    /// this behavior.
    ///
    /// #### Parameters
    ///
    /// - `tabUIID`: the tabUIID to set
    public void setTabUIID(String tabUIID) {
        this.tabUIID = tabUIID;
    }

    /// Allows marking tabs as swipe "eager" which instantly triggers swipe on movement
    /// rather than threshold the swipe.
    ///
    /// #### Returns
    ///
    /// the eagerSwipeMode
    public boolean isEagerSwipeMode() {
        return eagerSwipeMode;
    }

    /// Allows marking tabs as swipe "eager" which instantly triggers swipe on movement
    /// rather than threshold the swipe.
    ///
    /// #### Parameters
    ///
    /// - `eagerSwipeMode`: the eagerSwipeMode to set
    public void setEagerSwipeMode(boolean eagerSwipeMode) {
        this.eagerSwipeMode = eagerSwipeMode;
    }

    /// Indicates whether clicking on a tab button should result in an animation to the selected tab or an immediate switch
    ///
    /// #### Returns
    ///
    /// the animateTabSelection
    public boolean isAnimateTabSelection() {
        return animateTabSelection;
    }

    /// Indicates whether clicking on a tab button should result in an animation to the selected tab or an immediate switch
    ///
    /// #### Parameters
    ///
    /// - `animateTabSelection`: the animateTabSelection to set
    public void setAnimateTabSelection(boolean animateTabSelection) {
        this.animateTabSelection = animateTabSelection;
    }

    void initTabsContainerStyle() {
        if (originalTabsContainerSelected == null) {
            originalTabsContainerSelected = tabsContainer.getSelectedStyle();
            originalTabsContainerUnselected = tabsContainer.getUnselectedStyle();
        }
    }

    /// Allows developers to customize the motion object for the slide effect
    /// to provide a linear slide effect. You can use the `tabsSlideSpeedInt`
    /// theme constant to define the time in milliseconds between releasing the swiped
    /// tab and reaching the next tab. This currently defaults to 200.
    ///
    /// #### Parameters
    ///
    /// - `start`: start position
    ///
    /// - `end`: end position for the motion
    ///
    /// #### Returns
    ///
    /// the motion object
    protected Motion createTabSlideMotion(int start, int end) {
        return Motion.createSplineMotion(start, end, getUIManager().getThemeConstant("tabsSlideSpeedInt", 200));
    }

    /// Returns `true` if the swipe is on the X-Axis, `false` if the swipe is on the Y-Axis.
    ///
    /// #### Returns
    ///
    /// swipe direction flag
    public boolean isSwipeOnXAxis() {
        return swipeOnXAxis;
    }

    /// It defaults to `true`; you can set it to `false` for use cases like the
    /// one discussed here:
    /// [Realize a set of Containers that are browsable with a finger, like a deck of cards](https://new.reddit.com/r/cn1/comments/quq7yo/realize_a_set_of_containers_that_are_browsable/)
    ///
    /// Example of usage ([demo video](https://youtu.be/9CxqFGOYAU0)):
    ///
    /// ```java
    /// Form hi = new Form("Test swipe on tabs", BorderLayout.absolute());
    /// Tabs tabs = new Tabs();
    /// ButtonGroup btnGroup = new ButtonGroup();
    /// Button swipeXBtn = RadioButton.createToggle("Swipe on X-Axis", btnGroup);
    /// Button swipeYBtn = RadioButton.createToggle("Swipe on Y-Axis", btnGroup);
    /// btnGroup.setSelected(0);
    ///
    /// swipeXBtn.addActionListener(l -> {
    ///     tabs.setSwipeOnXAxis(true);
    /// });
    ///
    /// swipeYBtn.addActionListener(l -> {
    ///     tabs.setSwipeOnXAxis(false);
    /// });
    ///
    /// hi.add(BorderLayout.NORTH, GridLayout.encloseIn(2, swipeXBtn, swipeYBtn));
    ///
    /// //tabs.hideTabs();
    /// hi.add(BorderLayout.CENTER, tabs);
    ///
    /// List cards = new ArrayList<>();
    /// for (int i=0; i<20; i++) {
    ///     Container card = new Container(BoxLayout.y());
    ///     card.getAllStyles().setBorder(Border.createLineBorder(CN.convertToPixels(1)/5, 0));
    ///     card.addAll(FlowLayout.encloseCenter(new Label(FontImage.createMaterial(FontImage.MATERIAL_PERSON, "Label", 50.0f))), new Label("Card " + i));
    ///     cards.add(card);
    ///     tabs.addTab("tab " + i, card);
    /// }
    ///
    /// hi.show();
    /// ```
    ///
    /// #### Parameters
    ///
    /// - `b`: `true` to set the swipe on the X-Axis, `false` to set the swipe on the Y-Axis
    ///
    public void setSwipeOnXAxis(boolean b) {
        if (swipeOnXAxis != b) {
            swipeOnXAxis = b;
            contentPane.setShouldCalcPreferredSize(true);
            revalidateLater();
        }
    }

    /// {@inheritDoc}
    @Override
    public String[] getPropertyNames() {
        return new String[]{"titles", "icons", "selectedIcons"};
    }

    /// {@inheritDoc}
    @Override
    public Class[] getPropertyTypes() {
        return new Class[]{com.codename1.impl.CodenameOneImplementation.getStringArrayClass(),
                com.codename1.impl.CodenameOneImplementation.getImageArrayClass(),
                com.codename1.impl.CodenameOneImplementation.getImageArrayClass()};
    }

    /// {@inheritDoc}
    @Override
    public String[] getPropertyTypeNames() {
        return new String[]{"String[]", "Image[]", "Image[]"};
    }

    /// {@inheritDoc}
    @Override
    public Object getPropertyValue(String name) {
        if ("titles".equals(name)) {
            String[] t = new String[getTabCount()];
            for (int iter = 0; iter < t.length; iter++) {
                t[iter] = getTabTitle(iter);
            }
            return t;
        }
        if ("icons".equals(name)) {
            Image[] t = new Image[getTabCount()];
            for (int iter = 0; iter < t.length; iter++) {
                t[iter] = getTabIcon(iter);
            }
            return t;
        }
        if ("selectedIcons".equals(name)) {
            Image[] t = new Image[getTabCount()];
            for (int iter = 0; iter < t.length; iter++) {
                t[iter] = getTabSelectedIcon(iter);
            }
            return t;
        }
        return null;
    }

    /// {@inheritDoc}
    @Override
    public String setPropertyValue(String name, Object value) {
        if ("titles".equals(name)) {
            String[] t = (String[]) value;
            for (int iter = 0; iter < Math.min(getTabCount(), t.length); iter++) {
                setTabTitle(t[iter], getTabIcon(iter), iter);
            }
            return null;
        }
        if ("icons".equals(name)) {
            Image[] t = (Image[]) value;
            if (t == null) {
                for (int iter = 0; iter < getTabCount(); iter++) {
                    setTabTitle(getTabTitle(iter), null, iter);
                }
            } else {
                for (int iter = 0; iter < Math.min(getTabCount(), t.length); iter++) {
                    setTabTitle(getTabTitle(iter), t[iter], iter);
                }
            }
            return null;
        }
        if ("selectedIcons".equals(name)) {
            Image[] t = (Image[]) value;
            for (int iter = 0; iter < Math.min(getTabCount(), t.length); iter++) {
                setTabSelectedIcon(iter, t[iter]);
            }
            return null;
        }
        return super.setPropertyValue(name, value);
    }

    // ---- iOS 27 floating tab bar geometry (tabsMorphPreset ios27), in points ----
    // Read off a real UITabBarController (the motion probe's layer log): the pill
    // is 62 pt tall with its lower edge 21 pt above the screen's, and as wide as its
    // tabs need -- 86 pt a tab plus 16 -- but never closer than 21 pt to the sides.
    // Each tab's button is 54 pt tall, 4 pt inside the pill, and is where the lens
    // rests: 94 pt wide at the natural pitch, never narrower than 84.05 pt, which is
    // why the buttons of a crowded bar overlap. A theme may override each number.
    static final float GLASS_BAR_PT = 62f;
    static final float GLASS_PITCH_PT = 86f;
    static final float GLASS_INSET_PT = 4f;
    static final float GLASS_MIN_LENS_PT = 84.05f;
    static final float GLASS_EDGE_PT = 21f;
    // Width of the resting lens, set by GlassTabsLayout.
    private float glassLensWidth;

    private float glassConstantPt(String name, float def) {
        String v = getUIManager().getThemeConstant(name, null);
        if (v == null) {
            return def;
        }
        try {
            return Float.parseFloat(v.trim());
        } catch (NumberFormatException ignore) {
            return def;
        }
    }

    /// Points to pixels: the screen's own scale where the port knows it (UIScreen
    /// on iOS), else through the physical size of a 1/163 inch point.
    static float glassPx(float pt) {
        float ratio = Display.getInstance().getDevicePixelRatio();
        if (ratio <= 0) {
            ratio = Display.getInstance().convertToPixels(10f) / 10f * 25.4f / 163f;
        }
        return pt * ratio;
    }

    /// The pill width the tabs ask for, in pixels (before the side limit).
    private float glassNaturalPillWidth(int n) {
        return glassPx(n * glassConstantPt("tabsGlassPitchPt", GLASS_PITCH_PT)
                + 4 * glassConstantPt("tabsGlassInsetPt", GLASS_INSET_PT));
    }

    /// Places the tabs inside the iOS 27 pill like UITabBar: equal pitch, each
    /// button centred on its tab and as wide as the resting lens.
    class GlassTabsLayout extends Layout {
        @Override
        public void layoutContainer(Container parent) {
            int n = parent.getComponentCount();
            if (n == 0) {
                return;
            }
            int sx = glassSlackXPx();
            int sy = glassSlackYPx();
            float pillX = sx;
            float pillW = parent.getWidth() - 2 * sx;
            float pillH = parent.getHeight() - 2 * sy;
            float inset = glassPx(glassConstantPt("tabsGlassInsetPt", GLASS_INSET_PT));
            float span = pillW - 2 * inset;
            float lensW;
            float pitch;
            if (n == 1) {
                lensW = span;
                pitch = 0;
            } else {
                pitch = (span - 2 * inset) / n;
                lensW = pitch + 2 * inset;
                float minLens = glassPx(glassConstantPt("tabsGlassMinLensPt", GLASS_MIN_LENS_PT));
                if (lensW < minLens) {
                    lensW = Math.min(minLens, span);
                    pitch = (span - lensW) / (n - 1);
                }
            }
            glassLensWidth = lensW;
            int top = Math.round(sy + inset);
            int h = Math.max(0, Math.round(pillH - 2 * inset));
            for (int i = 0; i < n; i++) {
                Component c = parent.getComponentAt(i);
                float centre = pillX + inset + lensW / 2f + i * pitch;
                int left = Math.round(centre - lensW / 2f);
                c.setX(left);
                c.setY(top);
                c.setWidth(Math.round(centre + lensW / 2f) - left);
                c.setHeight(h);
            }
        }

        @Override
        public Dimension getPreferredSize(Container parent) {
            return new Dimension(Math.round(glassNaturalPillWidth(parent.getComponentCount())) + 2 * glassSlackXPx(),
                    Math.round(glassPx(glassConstantPt("tabsGlassBarPt", GLASS_BAR_PT))) + 2 * glassSlackYPx());
        }
    }

    /// The wrapper around the floating tab bar. With the iOS 27 motion it sizes
    /// and places the pill the way UITabBar does (centred, as wide as its tabs,
    /// its lower edge a fixed distance above the bottom -- inside the home
    /// indicator's safe area, as native draws it); otherwise it is a BorderLayout.
    class GlassHostLayout extends BorderLayout {
        @Override
        public void layoutContainer(Container parent) {
            if (!isGlassMotion() || tabsContainer == null || tabsContainer.getParent() != parent) { //NOPMD CompareObjectsWithEquals
                super.layoutContainer(parent);
                return;
            }
            int sx = glassSlackXPx();
            int sy = glassSlackYPx();
            float edge = glassPx(glassConstantPt("tabsGlassEdgePt", GLASS_EDGE_PT));
            float barH = glassPx(glassConstantPt("tabsGlassBarPt", GLASS_BAR_PT));
            float pillW = Math.min(glassNaturalPillWidth(tabsContainer.getComponentCount()),
                    parent.getWidth() - 2 * edge);
            int w = Math.round(pillW) + 2 * sx;
            int h = Math.round(barH) + 2 * sy;
            tabsContainer.setX((parent.getWidth() - w) / 2);
            tabsContainer.setY(Math.round(parent.getHeight() - edge - barH) - sy);
            tabsContainer.setWidth(w);
            tabsContainer.setHeight(h);
        }

        @Override
        public Dimension getPreferredSize(Container parent) {
            if (!isGlassMotion() || tabsContainer == null) {
                return super.getPreferredSize(parent);
            }
            float edge = glassPx(glassConstantPt("tabsGlassEdgePt", GLASS_EDGE_PT));
            float barH = glassPx(glassConstantPt("tabsGlassBarPt", GLASS_BAR_PT));
            return new Dimension(tabsContainer.getPreferredW(), Math.round(barH + edge) + glassSlackYPx());
        }
    }

    class TabsLayout extends Layout {

        @Override
        public void layoutContainer(Container parent) {
            final int size = parent.getComponentCount();

            int tabWidth = parent.getWidth() - tabsGap * 2;
            int tabHeight = parent.getHeight() - tabsGap * 2;

            if (swipeOnXAxis) {
                for (int i = 0; i < size; i++) {
                    int xOffset;
                    if (parent.isRTL()) {
                        xOffset = (size - i) * tabWidth + tabsGap;
                        xOffset -= ((size - activeComponent) * tabWidth);
                    } else {
                        xOffset = i * tabWidth + tabsGap;
                        xOffset -= (activeComponent * tabWidth);
                    }
                    Component component = parent.getComponentAt(i);
                    component.setX(component.getStyle().getMarginLeftNoRTL() + xOffset);
                    component.setY(component.getStyle().getMarginTop());
                    component.setWidth(tabWidth - component.getStyle().getHorizontalMargins());
                    component.setHeight(parent.getHeight() - component.getStyle().getVerticalMargins());
                }
            } else {
                for (int i = 0; i < size; i++) {
                    int yOffset;
                    yOffset = i * tabHeight + tabsGap;
                    yOffset -= (activeComponent * tabHeight);
                    Component component = parent.getComponentAt(i);
                    component.setX(component.getStyle().getMarginLeftNoRTL());
                    component.setY(component.getStyle().getMarginTop() + yOffset);
                    component.setWidth(tabWidth - component.getStyle().getHorizontalMargins());
                    component.setHeight(parent.getHeight() - component.getStyle().getVerticalMargins());
                }
            }

        }

        @Override
        public Dimension getPreferredSize(Container parent) {
            // fill
            Dimension dim = new Dimension(0, 0);
            dim.setWidth(parent.getWidth() + parent.getStyle().getPaddingLeftNoRTL()
                    + parent.getStyle().getPaddingRightNoRTL());
            dim.setHeight(parent.getHeight() + parent.getStyle().getPaddingTop()
                    + parent.getStyle().getPaddingBottom());
            int compCount = contentPane.getComponentCount();
            for (int iter = 0; iter < compCount; iter++) {
                Dimension d = contentPane.getComponentAt(iter).getPreferredSizeWithMargin();
                dim.setWidth(Math.max(d.getWidth(), dim.getWidth()));
                dim.setHeight(Math.max(d.getHeight(), dim.getHeight()));
            }
            return dim;
        }
    }

    class TabFocusListener implements FocusListener {

        @Override
        public void focusGained(Component cmp) {
            if (focusListeners != null) {
                focusListeners.fireFocus(cmp);
            }
            if (Display.getInstance().shouldRenderSelection()) {
                if (isChangeTabOnFocus()) {
                    if (!((Button) cmp).isSelected()) {
                        cmp.fireClicked();
                    }
                }
                if (changeTabContainerStyleOnFocus) {
                    initTabsContainerStyle();
                    tabsContainer.setUnselectedStyle(originalTabsContainerSelected);
                    tabsContainer.repaint();
                }
            }
        }


        @Override
        public void focusLost(Component cmp) {
            if (focusListeners != null) {
                focusListeners.fireFocus(cmp);
            }
            if (changeTabContainerStyleOnFocus) {
                initTabsContainerStyle();
                tabsContainer.setUnselectedStyle(originalTabsContainerUnselected);
                tabsContainer.repaint();
            }
        }

    }

    class SwipeListener implements ActionListener {

        private final static int PRESS = 0;
        private final static int DRAG = 1;
        private final static int RELEASE = 2;
        private final int type;


        public SwipeListener(int type) {
            this.type = type;
        }


        @Override
        public void actionPerformed(ActionEvent evt) {
            if (type == PRESS) {
                recordGlassPress(evt.getX(), evt.getY());
            } else if (type == DRAG) {
                recordGlassDrag(evt.getX());
            } else if (type == RELEASE && glassMotionStart >= 0) {
                recordGlassRelease(evt.getX());
                CN.callSerially(new Runnable() {
                    @Override
                    public void run() {
                        checkGlassPressOutcome();
                    }
                });
            }

            if (getComponentCount() == 0 || !swipeActivated || slideToDestMotion != null) {
                return;
            }
            final int x = evt.getX();
            final int y = evt.getY();
            switch (type) {
                case PRESS: {
                    blockSwipe = false;
                    riskySwipe = false;
                    if (!isEventBlockedByHigherComponent(evt) && contentPane.visibleBoundsContains(x, y)) {
                        Component testCmp = contentPane.getComponentAt(x, y);
                        if (testCmp != null && testCmp != contentPane) { //NOPMD CompareObjectsWithEquals
                            doNotBlockSideSwipe = true;
                            try {
                                while (testCmp != null && testCmp != contentPane) { //NOPMD CompareObjectsWithEquals
                                    if (testCmp.shouldBlockSideSwipe()) {
                                        lastX = -1;
                                        lastY = -1;
                                        initialX = -1;
                                        initialY = -1;
                                        blockSwipe = true;
                                        return;
                                    }
                                    if (testCmp.isScrollable()) {
                                        if (swipeOnXAxis) {
                                            if (testCmp.isScrollableX()) {
                                                // we need to block swipe since the user is trying to scroll a component
                                                lastX = -1;
                                                initialX = -1;
                                                blockSwipe = true;
                                                return;
                                            }

                                            // scrollable Y component, we want to make side scrolling
                                            // slightly harder so it doesn't bother the vertical swipe
                                            riskySwipe = true;
                                            break;
                                        } else {
                                            if (testCmp.isScrollableY()) {
                                                // we need to block swipe since the user is trying to scroll a component
                                                lastY = -1;
                                                initialY = -1;
                                                blockSwipe = true;
                                                return;
                                            }

                                            // scrollable X component, we want to make side scrolling
                                            // slightly harder so it doesn't bother the vertical swipe
                                            riskySwipe = true;
                                            break;
                                        }
                                    }
                                    testCmp = testCmp.getParent();
                                }
                            } finally {
                                doNotBlockSideSwipe = false;
                            }
                        }
                        lastX = x;
                        lastY = y;
                        initialX = x;
                        initialY = y;
                    } else {
                        lastX = -1;
                        lastY = -1;
                        initialX = -1;
                        initialY = -1;
                        blockSwipe = true;
                    }
                    dragStarted = false;
                    break;
                }
                case DRAG: {
                    if (blockSwipe) {
                        return;
                    }
                    if (!dragStarted) {
                        if (isEagerSwipeMode()) {
                            dragStarted = true;
                        } else {
                            if (riskySwipe) {
                                if (swipeOnXAxis && Math.abs(x - initialX) < Math.abs(y - initialY)) {
                                    return;
                                }
                                if (!swipeOnXAxis && Math.abs(x - initialX) > Math.abs(y - initialY)) {
                                    return;
                                }
                                // give heavier weight when we have two axis swipe
                                if (swipeOnXAxis) {
                                    dragStarted = Math.abs(x - initialX) > (contentPane.getWidth() / 5);
                                } else {
                                    dragStarted = Math.abs(y - initialY) > (contentPane.getHeight() / 5);
                                }
                            } else {
                                // start drag not imediately, giving components some sort
                                // of weight.
                                if (swipeOnXAxis) {
                                    dragStarted = Math.abs(x - initialX) > (contentPane.getWidth() / 8);
                                } else {
                                    dragStarted = Math.abs(y - initialY) > (contentPane.getHeight() / 8);
                                }
                                if (dragStarted && swipeOnXAxis) {
                                    int diff = x - initialX;
                                    if (shouldBlockSideSwipeLeft() && diff < 0 ||
                                            shouldBlockSideSwipeRight() && diff > 0) {
                                        lastX = -1;
                                        initialX = -1;
                                        initialY = -1;
                                        blockSwipe = true;
                                        dragStarted = false;
                                        return;
                                    }
                                }
                                TopLevelContainer parent = getTopLevelContainer();
                                // A tab can be removed in response to the same pointer gesture
                                // (e.g. an inspector rebuild).  Its global swipe listener may still
                                // receive the queued drag after deinitialization.
                                if (parent != null) {
                                    parent.clearComponentsAwaitingRelease();
                                }
                            }
                        }
                    }
                    if (swipeOnXAxis && initialX != -1 && contentPane.contains(x, y)) {
                        int diffX = x - lastX;
                        if (diffX != 0 && dragStarted) {
                            lastX += diffX;
                            final int size = contentPane.getComponentCount();
                            for (int i = 0; i < size; i++) {
                                Component component = contentPane.getComponentAt(i);
                                component.setX(component.getX() + diffX);
                                component.paintLock(false);
                            }
                            setEnableLayoutOnPaint(false);
                            repaint();
                        }
                    }
                    if (!swipeOnXAxis && initialY != -1 && contentPane.contains(x, y)) {
                        int diffY = y - lastY;
                        if (diffY != 0 && dragStarted) {
                            lastY += diffY;
                            final int size = contentPane.getComponentCount();
                            for (int i = 0; i < size; i++) {
                                Component component = contentPane.getComponentAt(i);
                                component.setY(component.getY() + diffY);
                                component.paintLock(false);
                            }
                            setEnableLayoutOnPaint(false);
                            repaint();
                        }
                    }
                    break;
                }
                case RELEASE: {
                    if (changeTabContainerStyleOnFocus) {
                        initTabsContainerStyle();
                        tabsContainer.setUnselectedStyle(originalTabsContainerUnselected);
                        tabsContainer.repaint();
                    }
                    if (blockSwipe) {
                        return;
                    }
                    if (swipeOnXAxis && initialX != -1) {
                        int diff = x - initialX;
                        if (diff != 0 && dragStarted) {
                            if (Math.abs(diff) > contentPane.getWidth() / 6) {
                                if (isRTL()) {
                                    diff *= -1;
                                }
                                if (diff > 0) {
                                    active = activeComponent - 1;
                                    if (active < 0) {
                                        active = 0;
                                    }
                                } else {
                                    active = activeComponent + 1;
                                    if (active >= contentPane.getComponentCount()) {
                                        active = contentPane.getComponentCount() - 1;
                                    }
                                }
                            }
                            int start = contentPane.getComponentAt(active).getX();
                            int end = tabsGap;
                            slideToDestMotion = createTabSlideMotion(start, end);
                            slideToDestMotion.start();
                            TopLevelContainer form = getTopLevelContainer();
                            if (form != null) {
                                TopLevelSupport.registerAnimatedInternal(form, Tabs.this);
                            }
                            evt.consume();
                        }
                    }
                    if (!swipeOnXAxis && initialY != -1) {
                        int diff = y - initialY;
                        if (diff != 0 && dragStarted) {
                            if (Math.abs(diff) > contentPane.getHeight() / 6) {
                                if (diff > 0) {
                                    active = activeComponent - 1;
                                    if (active < 0) {
                                        active = 0;
                                    }
                                } else {
                                    active = activeComponent + 1;
                                    if (active >= contentPane.getComponentCount()) {
                                        active = contentPane.getComponentCount() - 1;
                                    }
                                }
                            }
                            int start = contentPane.getComponentAt(active).getX();
                            int end = tabsGap;
                            slideToDestMotion = createTabSlideMotion(start, end);
                            slideToDestMotion.start();
                            TopLevelContainer form = getTopLevelContainer();
                            if (form != null) {
                                TopLevelSupport.registerAnimatedInternal(form, Tabs.this);
                            }
                            evt.consume();
                        }
                    }
                    lastX = -1;
                    lastY = -1;
                    initialX = -1;
                    initialY = -1;
                    dragStarted = false;
                    break;
                }
                default:
                    break;
            }
        }

        private boolean isEventBlockedByHigherComponent(ActionEvent evt) {
            final int x = evt.getX();
            final int y = evt.getY();
            // These coordinates are local to the surface the tabs live on, so the hit
            // test has to run against that surface. Resolving the current form meant a
            // window's swipe was tested against an unrelated main-form component at the
            // same coordinates, which set blockSwipe and made the swipe do nothing.
            final TopLevelContainer top = getTopLevelContainer();
            if (top == null) {
                return false;
            }
            final Component targetComponent = top.asContainer().getComponentAt(x, y);
            return !contentPane.equals(targetComponent) && !contentPane.contains(targetComponent);
        }
    }
}
