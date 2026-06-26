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
package com.codename1.impl.ios.sim.shell;

import com.codename1.impl.ios.sim.bridge.BridgeRegistry;
import com.codename1.impl.ios.sim.bridge.ToolsBridge;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Style;

import java.util.HashMap;
import java.util.Map;

/**
 * The simulator shell - a Codename One application running in the PARENT
 * universe whose UI is the simulator chrome: the device skin framing the
 * isolated app's screen, plus tool panels (the network monitor is the first)
 * all rendered through the same native pipeline. No Swing, no AWT.
 *
 * <p>The skin component deliberately clips the screen rectangle OUT of its
 * own painting, so shell repaints never erase the isolated app's output -
 * the persistent screen texture keeps both regions.</p>
 */
public class SimulatorShell {
    private Image skinImage;
    private int screenX;
    private int screenY;
    private int screenW;
    private int screenH;
    private SkinComponent skinComponent;
    private Container networkList;
    private final Map<Integer, Label> networkRows = new HashMap<Integer, Label>();
    private Form shellForm;
    private Container toolsPanel;
    private int toolsWidth;
    private int networkCondition = BridgeRegistry.NETWORK_REGULAR;
    private String skinsDir;
    private String currentSkinPath;
    private SkinSwitcher skinSwitcher;

    /** Implemented by the launcher: reloads the given skin and reapplies it. */
    public interface SkinSwitcher {
        void switchTo(String skinPath);
    }

    /**
     * Provides the skins directory listed in the Skins menu, the active skin
     * and the launcher callback performing the switch.
     */
    public void setSkinContext(String skinsDir, String currentSkinPath, SkinSwitcher switcher) {
        this.skinsDir = skinsDir;
        this.currentSkinPath = currentSkinPath;
        this.skinSwitcher = switcher;
    }

    /**
     * @param skinImage the device skin (already decoded through the bridge)
     * @param screenX screen rectangle inside the skin image
     */
    public SimulatorShell(Image skinImage, int screenX, int screenY, int screenW, int screenH) {
        this.skinImage = skinImage;
        this.screenX = screenX;
        this.screenY = screenY;
        this.screenW = screenW;
        this.screenH = screenH;
    }

    /**
     * The skin frame: paints the device image but clips the screen rectangle
     * out, leaving the isolated app's pixels untouched.
     */
    class SkinComponent extends Component {
        protected Dimension calcPreferredSize() {
            return new Dimension(activeSkin().getWidth(), activeSkin().getHeight());
        }

        public void paint(Graphics g) {
            Image skin = activeSkin();
            int sx = activeScreenX();
            int sy = activeScreenY();
            int sw = activeScreenW();
            int sh = activeScreenH();
            // paint in LOCAL coordinates - the graphics is already
            // translated into the parent's space; absolute coords would
            // double-count ancestors (invisible when the window fit the
            // skin exactly, glaring inside a centered scroller)
            int ax = getX();
            int ay = getY();
            int[] clip = g.getClip();
            // four bands around the screen rect: above, below, left, right
            paintBand(g, clip, skin, ax, ay, 0, 0, skin.getWidth(), sy);
            paintBand(g, clip, skin, ax, ay, 0, sy + sh, skin.getWidth(), skin.getHeight() - sy - sh);
            paintBand(g, clip, skin, ax, ay, 0, sy, sx, sh);
            paintBand(g, clip, skin, ax, ay, sx + sw, sy, skin.getWidth() - sx - sw, sh);
            if (paused) {
                // the app's flushes are gated while paused, so this banner
                // over the screen region survives until Resume App
                g.setClip(clip[0], clip[1], clip[2], clip[3]);
                g.clipRect(ax + sx, ay + sy, sw, sh);
                g.setColor(0x000000);
                g.setAlpha(140);
                g.fillRect(ax + sx, ay + sy, sw, sh);
                g.setAlpha(255);
                g.setColor(0xffffff);
                String msg = "App Paused";
                com.codename1.ui.Font f = g.getFont();
                g.drawString(msg, ax + sx + (sw - f.stringWidth(msg)) / 2,
                        ay + sy + (sh - f.getHeight()) / 2);
            }
            g.setClip(clip);
        }

        private void paintBand(Graphics g, int[] clip, Image skin, int ax, int ay,
                int x, int y, int w, int h) {
            if (w <= 0 || h <= 0) {
                return;
            }
            // INTERSECT with the incoming paint clip - replacing it let the
            // oversized zoom skin smear over the sidebar during scrolling
            g.setClip(clip[0], clip[1], clip[2], clip[3]);
            g.clipRect(ax + x, ay + y, w, h);
            g.drawImage(skin, ax, ay);
        }
    }

    /** optional landscape variant for the Rotate command */
    private Image landscapeSkinImage;
    private int landscapeScreenX, landscapeScreenY, landscapeScreenW, landscapeScreenH;
    private boolean portraitOrientation = true;
    private Runnable orientationChanged;

    /**
     * Provides the landscape skin so the Rotate command can switch
     * orientations.
     */
    public void setLandscapeSkin(Image img, int sx, int sy, int sw, int sh) {
        landscapeSkinImage = img;
        landscapeScreenX = sx;
        landscapeScreenY = sy;
        landscapeScreenW = sw;
        landscapeScreenH = sh;
    }

    /**
     * Invoked after any chrome layout change (rotate, skin switch, monitor
     * toggle) - the launcher resizes the window and rebinds the app bridge to
     * the new screen rectangle.
     */
    public void setOrientationChangedCallback(Runnable r) {
        orientationChanged = r;
    }

    /**
     * Applies a freshly loaded skin (the Skins menu): swaps the images and
     * screen rectangles, resets to portrait and triggers the launcher
     * relayout. Pass null landscape parameters when the skin has none.
     */
    public void applySkin(String skinPath, Image skin, int sx, int sy, int sw, int sh,
            Image lskin, int lx, int ly, int lw, int lh) {
        currentSkinPath = skinPath;
        skinImage = skin;
        screenX = sx;
        screenY = sy;
        screenW = sw;
        screenH = sh;
        landscapeSkinImage = lskin;
        landscapeScreenX = lx;
        landscapeScreenY = ly;
        landscapeScreenW = lw;
        landscapeScreenH = lh;
        // keep the current orientation across zoom/resize re-applies; only
        // fall back to portrait when the skin has no landscape variant
        if (lskin == null) {
            portraitOrientation = true;
        }
        skinComponent.setShouldCalcPreferredSize(true);
        refreshMenu();
        if (orientationChanged != null) {
            orientationChanged.run();
        }
    }

    private Image activeSkin() {
        return portraitOrientation || landscapeSkinImage == null ? skinImage : landscapeSkinImage;
    }

    private int activeScreenX() {
        return portraitOrientation || landscapeSkinImage == null ? screenX : landscapeScreenX;
    }

    private int activeScreenY() {
        return portraitOrientation || landscapeSkinImage == null ? screenY : landscapeScreenY;
    }

    private int activeScreenW() {
        return portraitOrientation || landscapeSkinImage == null ? screenW : landscapeScreenW;
    }

    private int activeScreenH() {
        return portraitOrientation || landscapeSkinImage == null ? screenH : landscapeScreenH;
    }

    /**
     * Builds and shows the shell UI; returns the skin component so the
     * launcher can compute the app screen position after layout.
     */
    public Component show() {
        // the simulator menu is plain Codename One commands rendered as the
        // platform's native menu bar
        Display.getInstance().setNativeCommands(true);
        BorderLayout shellLayout = new BorderLayout();
        // true centering in both axes so the skin never drifts past an edge
        shellLayout.setCenterBehavior(BorderLayout.CENTER_BEHAVIOR_CENTER);
        Form shell = new Form(shellLayout);
        shellForm = shell;
        // the native window already has a title bar; the CN1 title area is
        // redundant chrome that stole height from the skin
        shell.getTitleArea().setUIID("Container");
        shell.getTitleArea().setPreferredSize(new Dimension(0, 0));
        // the chrome background deliberately never paints over the app's
        // screen rectangle: shell animations (toasts etc.) would otherwise
        // blank the app region every frame until the recomposite catches up,
        // which reads as heavy flicker
        shell.getContentPane().getAllStyles().setBgTransparency(0);
        chromeBands = new com.codename1.ui.Painter() {
            public void paint(Graphics g, com.codename1.ui.geom.Rectangle rect) {
                g.setColor(0x2b2b2b);
                int x = rect.getX();
                int y = rect.getY();
                int w = rect.getSize().getWidth();
                int h = rect.getSize().getHeight();
                if (skinComponent == null || skinComponent.getWidth() == 0) {
                    g.fillRect(x, y, w, h);
                    return;
                }
                int[] r = getAppScreenRect();
                // four bands around the app screen rect
                if (r[1] > y) {
                    g.fillRect(x, y, w, r[1] - y);
                }
                if (r[1] + r[3] < y + h) {
                    g.fillRect(x, r[1] + r[3], w, y + h - r[1] - r[3]);
                }
                if (r[0] > x) {
                    g.fillRect(x, r[1], r[0] - x, r[3]);
                }
                if (r[0] + r[2] < x + w) {
                    g.fillRect(r[0] + r[2], r[1], x + w - r[0] - r[2], r[3]);
                }
            }
        };
        shell.getContentPane().getAllStyles().setBgPainter(chromeBands);

        skinComponent = new SkinComponent();
        // everything overlapping the app screen must be transparent so shell
        // repaints disturb the app region as little as possible (the
        // repaint-request channel recomposites the app afterwards)
        skinComponent.getAllStyles().setBgTransparency(0);
        // the skin sits in a viewport that centers it normally and scrolls
        // X/Y in zoom mode (100% scale)
        com.codename1.ui.layouts.FlowLayout centerFlow =
                new com.codename1.ui.layouts.FlowLayout(Component.CENTER);
        centerFlow.setValign(Component.CENTER);
        skinScroller = new ZoomScroller(centerFlow);
        skinScroller.getAllStyles().setBgTransparency(0);
        // the scroller repaints anything its scrolling exposes (otherwise
        // stale pixels linger past the skin) and never overshoots the edges
        skinScroller.getAllStyles().setBgPainter(chromeBands);
        skinScroller.setTensileDragEnabled(false);
        skinScroller.setScrollVisible(true);
        skinScroller.add(skinComponent);
        skinScroller.addScrollListener(new com.codename1.ui.events.ScrollListener() {
            public void scrollChanged(int scrollX, int scrollY, int oldscrollX, int oldscrollY) {
                ((ZoomScroller) skinScroller).clampScroll();
                // the app screen rectangle moved inside the window - rebind
                // the app universe's render/input offsets
                if (scrollRebind != null) {
                    scrollRebind.run();
                }
            }
        });
        shell.add(BorderLayout.CENTER, skinScroller);

        // Reserve the native title bar's height at the very top so the skin and
        // the tool sidebar don't render up underneath it (the Catalyst window is
        // full-size-content). Solid dark to match the dark title bar, which the
        // OS overlays on top of this strip. Height is the title bar in surface
        // pixels (~28pt x 2 on retina); overridable for other scales.
        Container titleInset = new Container();
        titleInset.setPreferredH(Integer.getInteger("cn1.sim.titlebar.px", 56).intValue());
        Style tis = titleInset.getAllStyles();
        tis.setBgColor(0x2b2b2b);
        tis.setBgTransparency(255);
        shell.add(BorderLayout.NORTH, titleInset);

        buildSidebar();
        // no sidebar until a tool is enabled from the menu

        // a static status strip instead of overlay toasts: it never paints
        // over the skin or the app, so messages cannot cause repaint fights
        statusBar = new Label("");
        applyFont(statusBar, sidebarFont());
        Style sbs = statusBar.getAllStyles();
        sbs.setFgColor(0xffffff);
        sbs.setBgColor(0x1d1d1d);
        sbs.setBgTransparency(255);
        applyCompactPadding(statusBar, sbPadV(), sbPadH(), 0, 0);
        statusBar.setTickerEnabled(false);
        // collapsed when idle so it leaves no dead strip at the window bottom;
        // toast() grows it only while a message shows
        statusBar.setPreferredH(0);
        shell.add(BorderLayout.SOUTH, statusBar);

        installCommands(shell);
        shell.show();
        installToolsBridge();
        return skinComponent;
    }

    private Label statusBar;
    private com.codename1.ui.Painter chromeBands;

    /**
     * @return the height the bottom status strip adds to the chrome - the
     * launcher includes it when sizing the window
     */
    public int getChromeBottomHeight() {
        return statusBar == null ? 0 : statusBar.getPreferredH();
    }

    /**
     * Resizes the relay window to exactly frame the current skin plus the tool
     * sidebar - trimming the landscape room the window reserves by default.
     * Sizes are in relay-surface pixels; the relay converts to points. One-shot
     * (user/skin-state triggered), never on the resize event, so no feedback.
     */
    public void fitWindowToSkin() {
        if (windowControl == null) {
            System.err.println("cn1sim: fitWindowToSkin - windowControl is null");
            return;
        }
        int w = activeSkin().getWidth() + getDesiredToolsWidth() + 16;
        int h = activeSkin().getHeight() + getChromeBottomHeight();
        System.err.println("cn1sim: fitWindowToSkin -> " + w + "x" + h
                + " (skin " + activeSkin().getWidth() + "x" + activeSkin().getHeight()
                + " tools " + getDesiredToolsWidth() + ")");
        windowControl.setWindowSize(w, h);
    }

    /**
     * @return the window rectangle of the skin viewport - the region the app
     * universe may paint into (zoom-mode scrolling pushes the app rectangle
     * partially outside it)
     */
    public int[] getViewportRect() {
        return new int[]{skinScroller.getAbsoluteX(), skinScroller.getAbsoluteY(),
                skinScroller.getWidth(), skinScroller.getHeight()};
    }

    private Container skinScroller;
    private Runnable scrollRebind;

    /**
     * The zoom viewport: exposes programmatic scrolling so trackpad /
     * mouse-wheel events can pan the 100%-scale skin.
     */
    private class ZoomScroller extends Container {
        ZoomScroller(com.codename1.ui.layouts.Layout l) {
            super(l);
        }

        private int maxScrollX() {
            return Math.max(0, getScrollDimension().getWidth() - getWidth());
        }

        private int maxScrollY() {
            return Math.max(0, getScrollDimension().getHeight() - getHeight());
        }

        // hard clamp: drag scrolling and animations route through these, so
        // the viewport can never overshoot the skin (no page-size overscroll)
        protected void setScrollX(int x) {
            super.setScrollX(Math.max(0, Math.min(maxScrollX(), x)));
        }

        protected void setScrollY(int y) {
            super.setScrollY(Math.max(0, Math.min(maxScrollY(), y)));
        }

        void scrollBy(int dx, int dy) {
            if (!isScrollableX() && !isScrollableY()) {
                return;
            }
            setScrollX(getScrollX() + dx);
            setScrollY(getScrollY() + dy);
            repaint();
        }

        private boolean clamping;

        /**
         * CN1 drag scrolling writes the scroll offsets directly (bypassing
         * the setter overrides) - snap back whenever it overshoots.
         */
        void clampScroll() {
            if (clamping) {
                return;
            }
            clamping = true;
            try {
                int mx = maxScrollX();
                int my = maxScrollY();
                int sx = getScrollX();
                int sy = getScrollY();
                if (sx < 0 || sx > mx) {
                    super.setScrollX(Math.max(0, Math.min(mx, sx)));
                }
                if (sy < 0 || sy > my) {
                    super.setScrollY(Math.max(0, Math.min(my, sy)));
                }
            } finally {
                clamping = false;
            }
        }
    }

    /**
     * Pans the zoom viewport (trackpad / mouse-wheel scrolling).
     */
    public void scrollViewport(int dx, int dy) {
        ((ZoomScroller) skinScroller).scrollBy(dx, dy);
    }

    /**
     * Installed by the launcher: re-binds the app universe's offsets after
     * the screen rectangle moved (zoom-mode scrolling).
     */
    /**
     * Zoom toggle: 100% scale inside a fixed window, scrollable in both axes
     * (the classic simulator's zoom semantics). Shared by the menu command and
     * the dev test hooks.
     */
    public void toggleZoom() {
        if (zoomToggle != null) {
            zoomed = !zoomed;
            skinScroller.setScrollableX(zoomed);
            skinScroller.setScrollableY(zoomed);
            zoomToggle.run();
            refreshMenu();
        }
    }

    /** Programmatic zoom-viewport scroll for headless rendering verification. */
    public void devScrollBy(int dx, int dy) {
        if (skinScroller instanceof ZoomScroller) {
            ((ZoomScroller) skinScroller).scrollBy(dx, dy);
        }
    }

    public void setScrollRebind(Runnable r) {
        scrollRebind = r;
    }

    /**
     * A foldable sidebar tool: a clickable header that collapses/expands the
     * content, present in the sidebar only while its menu item is enabled.
     */
    private class ToolSection {
        final String title;
        final Container content;
        final Container root;
        final com.codename1.ui.Button header;
        boolean enabled;
        boolean folded;

        ToolSection(String title, Container content) {
            this.title = title;
            this.content = content;
            content.getAllStyles().setBgColor(0x1c1c1e);
            content.getAllStyles().setBgTransparency(255);
            applyCompactPadding(content, sbPadV(), sbPadH(), 0, sbMarginH());
            header = new com.codename1.ui.Button("[-] " + title);
            applyFont(header, sidebarBoldFont());
            applyCompactPadding(header, sbPadV() + 1, sbPadH(), sbMarginV(), sbMarginH());
            Style hs = header.getAllStyles();
            hs.setFgColor(0xffffff);
            hs.setBgColor(0x2c2c2e);
            hs.setBgTransparency(255);
            hs.setBorder(sidebarBorder());
            hs.setAlignment(Component.LEFT);
            header.addActionListener(new com.codename1.ui.events.ActionListener() {
                public void actionPerformed(com.codename1.ui.events.ActionEvent e) {
                    setFolded(!folded);
                }
            });
            root = new Container(BoxLayout.y());
            root.add(header);
            root.add(content);
        }

        void setFolded(boolean f) {
            folded = f;
            header.setText((folded ? "[+] " : "[-] ") + title);
            // physically remove the content so the layout reclaims its space
            if (folded) {
                root.removeComponent(content);
            } else if (content.getParent() == null) {
                root.add(content);
            }
            if (toolsPanel != null) {
                toolsPanel.revalidate();
            }
        }
    }

    private ToolSection networkSection;
    private ToolSection locationSection;
    private ToolSection appArgSection;
    private ToolSection pushSection;
    private ToolSection inspectorSection;
    private Container inspectorList;
    private Container inspectorDetail;
    private com.codename1.ui.Label selectedInspectRow;
    private ToolSection perfSection;
    private Label perfFpsLabel;
    private Label perfMemLabel;
    private ToolSection recorderSection;
    private Label recStatusLabel;
    private PushSender pushSender;

    /** Asks the app universe for its current component tree (inspector). */
    private void requestInspect() {
        com.codename1.impl.ios.sim.bridge.ChildControl c = BridgeRegistry.getChildControl();
        if (c != null) {
            c.control("inspect");
        }
    }

    /** Implemented by the launcher: delivers a simulated push to the app. */
    public interface PushSender {
        void sendPush(String payload);
    }

    public void setPushSender(PushSender p) {
        pushSender = p;
    }
    private com.codename1.ui.TextField latField;
    private com.codename1.ui.TextField lngField;
    private com.codename1.ui.TextField appArgField;

    /**
     * Builds the tool sections; the sidebar itself only appears once a tool
     * is enabled from the menu.
     */
    private void buildSidebar() {
        toolsPanel = new Container(BoxLayout.y());
        toolsPanel.setScrollableY(true);
        // iOS grouped-list backdrop behind the sections
        toolsPanel.getAllStyles().setBgColor(0x000000);
        toolsPanel.getAllStyles().setBgTransparency(255);
        toolsPanel.getAllStyles().setPadding(0, 0, 0, 0);

        networkList = new Container(BoxLayout.y());
        networkList.setPreferredH(220);
        networkList.setScrollableY(true);
        Container netContent = new Container(BoxLayout.y());
        netContent.add(networkList);
        networkSection = new ToolSection("Network Monitor", netContent);

        Container loc = new Container(BoxLayout.y());
        latField = sidebarField(String.valueOf(BridgeRegistry.getSimulatedLatitude()));
        lngField = sidebarField(String.valueOf(BridgeRegistry.getSimulatedLongitude()));
        loc.add(sidebarLabel("Latitude"));
        loc.add(latField);
        loc.add(sidebarLabel("Longitude"));
        loc.add(lngField);
        com.codename1.ui.Button applyLoc = sidebarButton("Apply Location");
        applyLoc.addActionListener(new com.codename1.ui.events.ActionListener() {
            public void actionPerformed(com.codename1.ui.events.ActionEvent e) {
                try {
                    BridgeRegistry.setSimulatedLocation(
                            Double.parseDouble(latField.getText().trim()),
                            Double.parseDouble(lngField.getText().trim()));
                    toast("Simulated location applied");
                } catch (NumberFormatException ex) {
                    toast("Invalid coordinates");
                }
            }
        });
        loc.add(applyLoc);
        locationSection = new ToolSection("Location Simulation", loc);

        Container arg = new Container(BoxLayout.y());
        appArgField = sidebarField("");
        arg.add(sidebarLabel("Argument"));
        arg.add(appArgField);
        com.codename1.ui.Button sendArg = sidebarButton("Send (restarts app)");
        sendArg.addActionListener(new com.codename1.ui.events.ActionListener() {
            public void actionPerformed(com.codename1.ui.events.ActionEvent e) {
                if (appController != null) {
                    appController.pauseApp();
                }
                com.codename1.impl.ios.sim.bridge.BridgeRegistry.setAppArg(appArgField.getText());
                if (appController != null) {
                    appController.resumeApp();
                }
                toast("App restarted with argument");
            }
        });
        arg.add(sendArg);
        appArgSection = new ToolSection("App Argument", arg);

        Container push = new Container(BoxLayout.y());
        final com.codename1.ui.TextField pushField = sidebarField("");
        push.add(sidebarLabel("Payload"));
        push.add(pushField);
        com.codename1.ui.Button sendPush = sidebarButton("Send Push");
        sendPush.addActionListener(new com.codename1.ui.events.ActionListener() {
            public void actionPerformed(com.codename1.ui.events.ActionEvent e) {
                if (pushSender != null) {
                    pushSender.sendPush(pushField.getText());
                    toast("Push delivered to the app");
                }
            }
        });
        push.add(sendPush);
        pushSection = new ToolSection("Push Simulation", push);

        inspectorList = new Container(BoxLayout.y());
        inspectorList.setScrollableY(true);
        inspectorList.setPreferredH(300);
        Container inspContent = new Container(BoxLayout.y());
        com.codename1.ui.Button refreshInsp = sidebarButton("Refresh tree");
        refreshInsp.addActionListener(new com.codename1.ui.events.ActionListener() {
            public void actionPerformed(com.codename1.ui.events.ActionEvent e) {
                requestInspect();
            }
        });
        inspContent.add(refreshInsp);
        inspContent.add(inspectorList);
        // tap a tree row to highlight the live component + show its properties
        inspectorDetail = new Container(BoxLayout.y());
        inspectorDetail.setScrollableY(true);
        inspectorDetail.setPreferredH(160);
        inspContent.add(inspectorDetail);
        inspectorSection = new ToolSection("Component Inspector", inspContent);

        Container perf = new Container(BoxLayout.y());
        perfFpsLabel = sidebarLabel("FPS: --");
        perfMemLabel = sidebarLabel("Heap: --");
        perf.add(perfFpsLabel);
        perf.add(perfMemLabel);
        perfSection = new ToolSection("Performance Monitor", perf);

        Container rec = new Container(BoxLayout.y());
        recStatusLabel = sidebarLabel("0 events");
        final com.codename1.ui.Button recBtn = sidebarButton("Record");
        final com.codename1.ui.Button playBtn = sidebarButton("Play");
        recBtn.addActionListener(new com.codename1.ui.events.ActionListener() {
            public void actionPerformed(com.codename1.ui.events.ActionEvent e) {
                com.codename1.impl.ios.sim.bridge.RecorderControl r =
                        BridgeRegistry.getRecorderControl();
                if (r == null) {
                    return;
                }
                if (r.isRecording()) {
                    r.stop();
                    recBtn.setText("Record");
                    recStatusLabel.setText(r.count() + " events");
                } else {
                    r.start();
                    recBtn.setText("Stop");
                    recStatusLabel.setText("recording...");
                }
                recStatusLabel.getParent().revalidate();
            }
        });
        playBtn.addActionListener(new com.codename1.ui.events.ActionListener() {
            public void actionPerformed(com.codename1.ui.events.ActionEvent e) {
                com.codename1.impl.ios.sim.bridge.RecorderControl r =
                        BridgeRegistry.getRecorderControl();
                if (r != null) {
                    r.play();
                    toast("Replaying " + r.count() + " events");
                }
            }
        });
        rec.add(recStatusLabel);
        rec.add(recBtn);
        rec.add(playBtn);
        recorderSection = new ToolSection("Test Recorder", rec);

        if (toolsWidth <= 0) {
            toolsWidth = Math.max(260, Display.getInstance().getDisplayWidth() / 4);
        }
        toolsPanel.setPreferredW(toolsWidth);
    }

    private boolean anySectionEnabled() {
        return networkSection.enabled || locationSection.enabled
                || appArgSection.enabled || pushSection.enabled
                || inspectorSection.enabled || perfSection.enabled
                || recorderSection.enabled;
    }

    /**
     * Enables/disables a tool: rebuilds the sidebar's section stack and adds
     * or removes the sidebar itself, resizing the window via the launcher
     * relayout callback.
     */
    private void toggleSection(ToolSection s) {
        boolean hadSidebar = anySectionEnabled();
        s.enabled = !s.enabled;
        toolsPanel.removeAll();
        ToolSection[] all = {networkSection, locationSection, appArgSection, pushSection,
                inspectorSection, perfSection, recorderSection};
        for (ToolSection t : all) {
            if (t.enabled) {
                toolsPanel.add(t.root);
            }
        }
        boolean hasSidebar = anySectionEnabled();
        if (hasSidebar && !hadSidebar) {
            shellForm.add(BorderLayout.EAST, toolsPanel);
        } else if (!hasSidebar && hadSidebar) {
            shellForm.removeComponent(toolsPanel);
        }
        refreshMenu();
        if (orientationChanged != null) {
            orientationChanged.run();
        }
    }

    private Label sidebarLabel(String text) {
        Label l = new Label(text);
        applyFont(l, sidebarFont());
        applyCompactPadding(l, sbPadV(), sbPadH(), sbMarginV(), 0);
        Style s = l.getAllStyles();
        s.setFgColor(0x8e8e93);
        s.setBgTransparency(0);
        l.setTickerEnabled(false);
        return l;
    }

    /**
     * Sidebar widgets style themselves completely - the chrome must render
     * identically no matter which theme the universes use (themed borders
     * need shape support and historically vanished).
     */
    private com.codename1.ui.TextField sidebarField(String text) {
        com.codename1.ui.TextField f = new com.codename1.ui.TextField(text);
        applyFont(f, sidebarFont());
        applyCompactPadding(f, sbPadV(), sbPadH(), sbMarginV(), sbMarginH());
        Style s = f.getAllStyles();
        s.setBgColor(0x2c2c2e);
        s.setBgTransparency(255);
        s.setFgColor(0xffffff);
        s.setBorder(sidebarBorder());
        return f;
    }

    // The native theme's component padding is sized for touch (mm/DIPs), which
    // is huge on a desktop tool panel. Force PIXEL-unit padding/margin on every
    // style mode (getAllStyles + DIPs left it touch-sized).
    private void applyCompactPadding(Component c, int padV, int padH, int marginV, int marginH) {
        Style[] modes = {c.getUnselectedStyle(), c.getSelectedStyle(),
                c.getPressedStyle(), c.getDisabledStyle()};
        for (Style s : modes) {
            s.setPaddingUnitTop(Style.UNIT_TYPE_PIXELS);
            s.setPaddingUnitBottom(Style.UNIT_TYPE_PIXELS);
            s.setPaddingUnitLeft(Style.UNIT_TYPE_PIXELS);
            s.setPaddingUnitRight(Style.UNIT_TYPE_PIXELS);
            s.setPadding(padV, padV, padH, padH);
            s.setMarginUnitTop(Style.UNIT_TYPE_PIXELS);
            s.setMarginUnitBottom(Style.UNIT_TYPE_PIXELS);
            s.setMarginUnitLeft(Style.UNIT_TYPE_PIXELS);
            s.setMarginUnitRight(Style.UNIT_TYPE_PIXELS);
            s.setMargin(marginV, marginV, marginH, marginH);
        }
    }

    // RoundRectBorder.cornerRadius is in MILLIMETERS; on the relay's ~790-PPI
    // surface 1.4mm becomes ~50px, and the border's getMinimumHeight() (=
    // shadowSpread + 2*radius) then forces every field/button ~130px tall no
    // matter how small the font/padding. Derive a small PIXEL radius (and zero
    // the unused shadow spread) so the border imposes only a ~12px floor.
    private com.codename1.ui.plaf.RoundRectBorder sidebarBorder() {
        int perMm = Math.max(1, Display.getInstance().convertToPixels(1f));
        float radiusMm = 6f / perMm;
        return com.codename1.ui.plaf.RoundRectBorder.create()
                .shadowSpread(0).cornerRadius(radiusMm);
    }

    /**
     * Fixed sidebar text size in surface pixels (the relay surface is ~2x
     * retina, so this reads as ~half its value in points). Overridable for
     * tuning. Padding/margins derive from it so the whole panel scales
     * together and stays stable when the window is fitted/resized.
     */
    private int sidebarFontPx() {
        return Integer.getInteger("cn1.sim.sidebar.fontpx", 31).intValue();
    }

    private int sbPadV() {
        return Math.max(3, sidebarFontPx() / 5);
    }

    private int sbPadH() {
        return Math.max(6, sidebarFontPx() / 3);
    }

    private int sbMarginV() {
        return Math.max(2, sidebarFontPx() / 9);
    }

    private int sbMarginH() {
        return Math.max(3, sidebarFontPx() / 7);
    }

    // small SF-style fonts so the side tools read like an iOS settings panel
    // rather than the oversized default native-theme font
    private com.codename1.ui.Font sbFont;
    private com.codename1.ui.Font sbBoldFont;

    private com.codename1.ui.Font sidebarFont() {
        if (sbFont == null) {
            sbFont = makeSidebarFont("native:MainRegular", com.codename1.ui.Font.STYLE_PLAIN);
        }
        return sbFont;
    }

    private com.codename1.ui.Font sidebarBoldFont() {
        if (sbBoldFont == null) {
            sbBoldFont = makeSidebarFont("native:MainBold", com.codename1.ui.Font.STYLE_BOLD);
        }
        return sbBoldFont;
    }

    /** Sets a font on every style mode (getAllStyles misses the field/button render style). */
    private void applyFont(Component c, com.codename1.ui.Font f) {
        c.getUnselectedStyle().setFont(f);
        c.getSelectedStyle().setFont(f);
        c.getPressedStyle().setFont(f);
        c.getDisabledStyle().setFont(f);
    }

    private com.codename1.ui.Font makeSidebarFont(String face, int style) {
        try {
            // A FIXED surface-pixel size, not a fraction of the live display
            // width: the relay surface is ~2x retina, so 26px reads as ~13pt
            // regardless of how large or small (fitted) the window is. Sizing
            // off getDisplayWidth() collapsed the font to the 11px floor on a
            // fitted window, making it unreadable.
            int px = sidebarFontPx();
            return com.codename1.ui.Font.createTrueTypeFont(face, face).derive(px, style);
        } catch (Throwable t) {
            return com.codename1.ui.Font.createSystemFont(
                    com.codename1.ui.Font.FACE_SYSTEM, style, com.codename1.ui.Font.SIZE_SMALL);
        }
    }

    private com.codename1.ui.Button sidebarButton(String text) {
        com.codename1.ui.Button b = new com.codename1.ui.Button(text);
        applyFont(b, sidebarFont());
        applyCompactPadding(b, sbPadV(), sbPadH(), sbMarginV(), sbMarginH());
        Style s = b.getAllStyles();
        s.setBgColor(0x0a84ff);
        s.setBgTransparency(255);
        s.setFgColor(0xffffff);
        s.setAlignment(Component.CENTER);
        s.setBorder(sidebarBorder());
        b.setTickerEnabled(false);
        return b;
    }

    /** Sets the width reserved for the tools side panel (launcher-computed). */
    public void setToolsWidth(int w) {
        toolsWidth = w;
    }

    /**
     * @return the width the chrome currently needs beside the skin - zero
     * when no tool section is enabled (no sidebar at all)
     */
    public int getDesiredToolsWidth() {
        return anySectionEnabled() ? toolsWidth : 0;
    }

    /**
     * @return the width left for the skin once the sidebar takes its slice -
     * the skin scale is clamped to this so it never overflows under the panel.
     */
    public int getAvailableSkinWidth() {
        return Display.getInstance().getDisplayWidth() - getDesiredToolsWidth() - 16;
    }

    /** Implemented by the launcher: drives the app's pause/resume lifecycle. */
    public interface AppController {
        void pauseApp();

        void resumeApp();
    }

    /**
     * Implemented by the launcher: native window controls on the relay. Sizes
     * are in relay-surface pixels; the relay converts to points.
     */
    public interface WindowControl {
        void setAlwaysOnTop(boolean onTop);

        void setWindowSize(int width, int height);
    }

    private WindowControl windowControl;

    public void setWindowControl(WindowControl w) {
        windowControl = w;
    }

    private AppController appController;
    private boolean alwaysOnTop;
    private boolean paused;
    private String debugEdtMode = "none";
    private boolean zoomed;
    private Runnable zoomToggle;
    private String nativeThemeKey = "auto";

    public void setAppController(AppController c) {
        appController = c;
    }

    /**
     * Pushes the menu commands straight to the shell universe's implementation.
     * The launcher wires this to the live shell impl reference; we bypass the
     * Form's auto-push (which doesn't reach the shell impl across the host /
     * universe classloader boundary) so the native menu bar always installs.
     */
    public interface MenuPusher {
        void push(java.util.Vector commands);
    }

    private MenuPusher menuPusher;

    public void setMenuPusher(MenuPusher p) {
        menuPusher = p;
    }

    /**
     * Installed by the launcher: re-applies the current skin at the zoomed /
     * unzoomed scale.
     */
    public void setZoomToggle(Runnable r) {
        zoomToggle = r;
    }

    /** @return true when the Zoom menu toggle is active */
    public boolean isZoomed() {
        return zoomed;
    }

    /**
     * The simulator menu: ordinary Codename One commands mirroring the
     * current Swing simulator's Device / Simulate / Tools / Help hierarchy
     * (the post-#5211 layout - Skin and Native Theme are Device submenus,
     * there is no separate "Simulator" menu). Form pushes the commands to the
     * implementation's setNativeCommands which builds the platform's native
     * menu bar; a ">" in the desktop-menu hint nests a submenu and a "-"
     * label is a separator. Tools the bridge does not reach yet open an
     * honest "not available yet" dialog rather than vanishing from the menu.
     */
    private void installCommands(final Form shell) {
        java.util.ArrayList<com.codename1.ui.Command> items =
                new java.util.ArrayList<com.codename1.ui.Command>();

        // --- Device: the simulated device and its window ---
        if (landscapeSkinImage != null) {
            items.add(command("Rotate", "Device", new Runnable() {
                public void run() {
                    rotate();
                }
            }));
        }
        items.add(checked(command("Zoom", "Device", new Runnable() {
            public void run() {
                toggleZoom();
            }
        }), zoomed));
        items.add(separator("Device"));
        installSkinsMenu(items);
        String[][] themes = {
                {"Auto (from build hints)", "auto"},
                {"iOS Modern (Liquid Glass)", "iOSModernTheme"},
                {"iOS 7 (Flat)", "iOS7Theme"},
                {"iPhone (Pre-Flat)", "iPhoneTheme"},
                {"Android Material", "AndroidMaterialTheme"},
                {"Android Holo Light", "android_holo_light"},
                {"Android Legacy", "androidTheme"},
                {"Use skin's embedded theme", "embedded"}};
        for (String[] theme : themes) {
            items.add(nativeThemeCommand(theme[0], theme[1]));
        }
        items.add(separator("Device"));
        items.add(command("Screenshot", "Device", new Runnable() {
            public void run() {
                saveScreenshot(false);
            }
        }));
        items.add(command("Screenshot With Skin", "Device", new Runnable() {
            public void run() {
                saveScreenshot(true);
            }
        }));
        items.add(stub("Screenshot StatusBar", "Device"));
        items.add(separator("Device"));
        items.add(command("Fit Window to Skin", "Device", new Runnable() {
            public void run() {
                fitWindowToSkin();
            }
        }));
        items.add(checked(command("Always on Top", "Device", new Runnable() {
            public void run() {
                alwaysOnTop = !alwaysOnTop;
                if (windowControl != null) {
                    windowControl.setAlwaysOnTop(alwaysOnTop);
                } else if (com.codename1.impl.ios.sim.CN1SimHost.isLoaded()) {
                    com.codename1.impl.ios.sim.CN1SimHost.setAlwaysOnTop(alwaysOnTop);
                }
                refreshMenu();
            }
        }), alwaysOnTop));
        items.add(separator("Device"));
        items.add(command("Exit", "Device", new Runnable() {
            public void run() {
                System.exit(0);
            }
        }));

        // --- Simulate: fake device state and events the app reacts to ---
        items.add(command(paused ? "Resume App" : "Pause App", "Simulate", new Runnable() {
            public void run() {
                togglePause();
            }
        }));
        items.add(checked(command("Send App Argument", "Simulate", new Runnable() {
            public void run() {
                toggleSection(appArgSection);
            }
        }), appArgSection.enabled));
        items.add(separator("Simulate"));
        items.add(checked(command("Location Simulation", "Simulate", new Runnable() {
            public void run() {
                toggleSection(locationSection);
            }
        }), locationSection.enabled));
        items.add(checked(command("Push Simulation", "Simulate", new Runnable() {
            public void run() {
                toggleSection(pushSection);
            }
        }), pushSection.enabled));
        final String bio = "Simulate>Biometric Simulation";
        items.add(checked(command("Hardware Available", bio, new Runnable() {
            public void run() {
                BridgeRegistry.setBiometricAvailable(!BridgeRegistry.isBiometricAvailable());
                refreshMenu();
            }
        }), BridgeRegistry.isBiometricAvailable()));
        items.add(separator(bio));
        items.add(checked(command("Face ID Enrolled", bio, new Runnable() {
            public void run() {
                BridgeRegistry.setBiometricFaceEnrolled(!BridgeRegistry.isBiometricFaceEnrolled());
                refreshMenu();
            }
        }), BridgeRegistry.isBiometricFaceEnrolled()));
        items.add(checked(command("Touch ID Enrolled", bio, new Runnable() {
            public void run() {
                BridgeRegistry.setBiometricTouchEnrolled(!BridgeRegistry.isBiometricTouchEnrolled());
                refreshMenu();
            }
        }), BridgeRegistry.isBiometricTouchEnrolled()));
        items.add(checked(command("Iris Enrolled", bio, new Runnable() {
            public void run() {
                BridgeRegistry.setBiometricIrisEnrolled(!BridgeRegistry.isBiometricIrisEnrolled());
                refreshMenu();
            }
        }), BridgeRegistry.isBiometricIrisEnrolled()));
        items.add(separator(bio));
        String[][] bioOutcomes = {{"Succeed", "SUCCEED"}, {"Fail", "FAIL"},
                {"Cancel", "CANCEL"}, {"Locked Out", "LOCKED_OUT"},
                {"Permanently Locked Out", "PERMANENTLY_LOCKED_OUT"},
                {"Not Enrolled", "NOT_ENROLLED"}, {"Passcode Not Set", "PASSCODE_NOT_SET"}};
        for (String[] o : bioOutcomes) {
            final String val = o[1];
            items.add(checked(command(o[0], bio + ">Next Outcome", new Runnable() {
                public void run() {
                    BridgeRegistry.setBiometricOutcome(val);
                    refreshMenu();
                }
            }), val.equals(BridgeRegistry.getBiometricOutcome())));
        }
        items.add(stub("Hardware Available", "Simulate>NFC"));
        items.add(stub("NFC Enabled", "Simulate>NFC"));
        items.add(stub("HCE Available", "Simulate>NFC"));
        items.add(separator("Simulate>NFC"));
        items.add(stub("Tap virtual tag", "Simulate>NFC"));
        items.add(stub("Set virtual tag URI...", "Simulate>NFC"));
        items.add(stub("Set virtual tag text...", "Simulate>NFC"));
        items.add(separator("Simulate>NFC"));
        items.add(stub("Send APDU to HCE service...", "Simulate>NFC"));
        items.add(stub("Deactivate HCE field", "Simulate>NFC"));
        items.add(stub("iOS Status Bar Tap", "Simulate"));
        items.add(separator("Simulate"));
        items.add(darkModeCommand("Dark Mode", "dark"));
        items.add(darkModeCommand("Light Mode", "light"));
        items.add(darkModeCommand("Auto", "auto"));
        String[] textSizes = {"Extra Small", "Small", "Medium", "Large (default)",
                "Extra Large", "Extra Extra Large", "Extra Extra Extra Large",
                "Accessibility 1", "Accessibility 2", "Accessibility 3",
                "Accessibility 4", "Accessibility 5"};
        // iOS Dynamic Type body sizes (pt) relative to Large/default = 17pt
        float[] textScales = {14f / 17f, 15f / 17f, 16f / 17f, 1.0f, 19f / 17f,
                21f / 17f, 23f / 17f, 28f / 17f, 33f / 17f, 40f / 17f,
                47f / 17f, 53f / 17f};
        for (int i = 0; i < textSizes.length; i++) {
            items.add(largerTextCommand(textSizes[i], textScales[i]));
        }
        final String iap = "Simulate>In App Purchase";
        items.add(checked(command("Manual Purchase Supported", iap, new Runnable() {
            public void run() {
                BridgeRegistry.setIapManualSupported(!BridgeRegistry.isIapManualSupported());
                refreshMenu();
            }
        }), BridgeRegistry.isIapManualSupported()));
        items.add(checked(command("Managed Purchase Supported", iap, new Runnable() {
            public void run() {
                BridgeRegistry.setIapManagedSupported(!BridgeRegistry.isIapManagedSupported());
                refreshMenu();
            }
        }), BridgeRegistry.isIapManagedSupported()));
        items.add(checked(command("Subscription Supported", iap, new Runnable() {
            public void run() {
                BridgeRegistry.setIapSubscriptionSupported(!BridgeRegistry.isIapSubscriptionSupported());
                refreshMenu();
            }
        }), BridgeRegistry.isIapSubscriptionSupported()));
        items.add(checked(command("Refund Supported", iap, new Runnable() {
            public void run() {
                BridgeRegistry.setIapRefundSupported(!BridgeRegistry.isIapRefundSupported());
                refreshMenu();
            }
        }), BridgeRegistry.isIapRefundSupported()));
        items.add(stub("Android 6 Permissions", "Simulate"));
        items.add(stub("Network available", "Simulate>Notifications and Background>Background constraints"));
        items.add(stub("Charging", "Simulate>Notifications and Background>Background constraints"));
        items.add(stub("Device idle", "Simulate>Notifications and Background>Background constraints"));
        items.add(stub("Battery not low", "Simulate>Notifications and Background>Background constraints"));
        items.add(stub("Run scheduled background work now", "Simulate>Notifications and Background"));
        items.add(stub("Show registered channels...", "Simulate>Notifications and Background"));
        items.add(separator("Simulate>Notifications and Background"));
        final String share = "Simulate>Notifications and Background";
        items.add(command("Send shared text...", share, new Runnable() {
            public void run() {
                String t = promptInput("Shared text", "Text to share into the app:");
                if (t != null) {
                    sendShare("shareText:" + t);
                }
            }
        }));
        items.add(command("Send shared URL...", share, new Runnable() {
            public void run() {
                String u = promptInput("Shared URL", "URL to share into the app:");
                if (u != null) {
                    sendShare("shareUrl:" + u);
                }
            }
        }));
        items.add(command("Send shared file...", share, new Runnable() {
            public void run() {
                String p = promptInput("Shared file", "Absolute file path to share:");
                if (p != null) {
                    sendShare("shareFile:" + p);
                }
            }
        }));

        // --- Tools: developer tools and diagnostics ---
        items.add(checked(command("Component Inspector", "Tools", new Runnable() {
            public void run() {
                toggleSection(inspectorSection);
                if (inspectorSection.enabled) {
                    requestInspect();
                }
            }
        }), inspectorSection.enabled));
        items.add(checked(command("Network Monitor", "Tools>Network", new Runnable() {
            public void run() {
                toggleSection(networkSection);
            }
        }), networkSection.enabled));
        items.add(stub("Proxy Settings", "Tools>Network"));
        items.add(separator("Tools>Network"));
        items.add(networkCommand("Regular Connection", BridgeRegistry.NETWORK_REGULAR));
        items.add(networkCommand("Slow Connection", BridgeRegistry.NETWORK_SLOW));
        items.add(networkCommand("Disconnected", BridgeRegistry.NETWORK_DISCONNECTED));
        items.add(checked(command("Performance Monitor", "Tools", new Runnable() {
            public void run() {
                toggleSection(perfSection);
                com.codename1.impl.ios.sim.bridge.ChildControl c =
                        BridgeRegistry.getChildControl();
                if (c != null) {
                    c.control(perfSection.enabled ? "perfMonitor:on" : "perfMonitor:off");
                }
            }
        }), perfSection.enabled));
        items.add(checked(command("Test Recorder", "Tools", new Runnable() {
            public void run() {
                toggleSection(recorderSection);
            }
        }), recorderSection.enabled));
        items.add(stub("Groovy Console", "Tools"));
        items.add(separator("Tools"));
        items.add(debugEdtCommand("None", "none"));
        items.add(debugEdtCommand("Light", "light"));
        items.add(debugEdtCommand("Full", "full"));
        items.add(stub("Slow Motion", "Tools"));
        items.add(stub("Debug Web Views", "Tools"));
        items.add(checked(stub("Disabled", "Tools>Hot Reload"), true));
        items.add(command("Reload Simulator", "Tools>Hot Reload", new Runnable() {
            public void run() {
                if (appController != null) {
                    // stop + start re-runs the app's start(), rebuilding its UI
                    appController.pauseApp();
                    appController.resumeApp();
                    toast("Simulator reloaded");
                }
            }
        }));
        items.add(stub("Reload Current Form (Requires CodeRAD)", "Tools>Hot Reload"));
        items.add(separator("Tools"));
        items.add(stub("Edit Build Hints...", "Tools"));
        items.add(stub("Auto Update Default Bundle", "Tools"));
        items.add(command("Clean Storage", "Tools", new Runnable() {
            public void run() {
                com.codename1.impl.ios.sim.bridge.ChildControl c = BridgeRegistry.getChildControl();
                if (c != null) {
                    c.control("cleanStorage");
                }
            }
        }));

        // --- Help ---
        items.add(urlCommand("Javadocs", "https://www.codenameone.com/javadoc/"));
        items.add(urlCommand("How Do I?", "https://www.codenameone.com/how-do-i.html"));
        items.add(urlCommand("Community Forum", "https://www.codenameone.com/discussion-forum.html"));
        items.add(separator("Help"));
        items.add(urlCommand("Build Server", "https://cloud.codenameone.com/secure/index.html"));
        items.add(separator("Help"));
        items.add(command("About", "Help", new Runnable() {
            public void run() {
                toast("Codename One Simulator - www.codenameone.com");
            }
        }));

        // MenuBar prepends each added command, so add in reverse to keep the
        // intended display order
        for (int i = items.size() - 1; i >= 0; i--) {
            shell.addCommand(items.get(i));
        }
        // explicitly push to the native menu bar: the items list is already in
        // display order and its index is what the MenuDispatcher resolves back
        if (menuPusher != null) {
            menuPusher.push(new java.util.Vector(items));
        }
    }

    /**
     * A menu item whose tool has not crossed the bridge yet - present so the
     * menu matches the classic simulator, honest about its state when used.
     */
    private com.codename1.ui.Command stub(final String label, String menu) {
        return command(label, menu, new Runnable() {
            public void run() {
                toast(label + " is not available yet in the native simulator");
            }
        });
    }

    private com.codename1.ui.Command debugEdtCommand(String label, final String mode) {
        // EDT-violation instrumentation has not crossed the bridge yet - the
        // radio group is present for parity, selecting is an honest stub
        return checked(stub(label, "Tools>Debug EDT"), debugEdtMode.equals(mode));
    }

    /**
     * Pause App / Resume App: fires the app's stop()/start() lifecycle, drops
     * its frames while paused and paints a paused banner over the screen.
     */
    private void togglePause() {
        if (appController == null) {
            return;
        }
        if (paused) {
            paused = false;
            BridgeRegistry.setAppPaused(false);
            appController.resumeApp();
            com.codename1.impl.ios.sim.bridge.InputSink sink = BridgeRegistry.getInputSink();
            if (sink != null) {
                sink.repaintRequest();
            }
        } else {
            appController.pauseApp();
            paused = true;
            BridgeRegistry.setAppPaused(true);
        }
        skinComponent.repaint();
        refreshMenu();
    }

    private String darkModeKey = "auto";

    /** The Simulate > Dark/Light Mode radio group - applied in the app universe. */
    private com.codename1.ui.Command darkModeCommand(String label, final String key) {
        return checked(command(label, "Simulate>Dark/Light Mode", new Runnable() {
            public void run() {
                darkModeKey = key;
                com.codename1.impl.ios.sim.bridge.ChildControl c = BridgeRegistry.getChildControl();
                if (c != null) {
                    c.control("dark:" + key);
                    toast(label + " applied");
                }
                refreshMenu();
            }
        }), darkModeKey.equals(key));
    }

    private float largerTextScaleSel = 1.0f;

    /** The Simulate > Larger Text radio group - scales fonts in the app universe. */
    private com.codename1.ui.Command largerTextCommand(final String label, final float scale) {
        return checked(command(label, "Simulate>Larger Text", new Runnable() {
            public void run() {
                largerTextScaleSel = scale;
                com.codename1.impl.ios.sim.bridge.ChildControl c = BridgeRegistry.getChildControl();
                if (c != null) {
                    c.control("largerText:" + scale);
                    toast("Text size: " + label);
                }
                refreshMenu();
            }
        }), Math.abs(largerTextScaleSel - scale) < 0.001f);
    }

    /** Delivers a Simulate &gt; Send shared... control command to the app universe. */
    private void sendShare(String cmd) {
        com.codename1.impl.ios.sim.bridge.ChildControl c = BridgeRegistry.getChildControl();
        if (c != null) {
            c.control(cmd);
            toast("Shared content delivered");
        }
    }

    /** A modal text-input prompt (parity with JavaSEPort's JOptionPane input). */
    private String promptInput(String title, String message) {
        com.codename1.ui.TextField tf = new com.codename1.ui.TextField();
        com.codename1.ui.Container body = com.codename1.ui.layouts.BoxLayout.encloseY(
                new Label(message), tf);
        com.codename1.ui.Command ok = new com.codename1.ui.Command("OK");
        com.codename1.ui.Command cancel = new com.codename1.ui.Command("Cancel");
        com.codename1.ui.Command res = com.codename1.ui.Dialog.show(title, body, ok, cancel);
        return res == ok ? tf.getText() : null;
    }

    /** The Device > Native Theme radio group - applied inside the app universe. */
    private com.codename1.ui.Command nativeThemeCommand(String label, final String key) {
        return checked(command(label, "Device>Native Theme", new Runnable() {
            public void run() {
                nativeThemeKey = key;
                com.codename1.impl.ios.sim.bridge.ChildControl c = BridgeRegistry.getChildControl();
                if (c != null) {
                    c.control("nativeTheme:" + key);
                    toast("Native theme applied");
                }
                refreshMenu();
            }
        }), nativeThemeKey.equals(key));
    }

    private com.codename1.ui.util.UITimer statusClear;

    /**
     * Shows a transient message in the bottom status strip - deliberately
     * not an overlay so it can never repaint-fight with the app region.
     */
    private void toast(String message) {
        System.out.println("[cn1sim] " + message);
        if (statusBar == null) {
            return;
        }
        statusBar.setText(message);
        // accent background while a message is showing - unmissable
        statusBar.getAllStyles().setBgColor(0x3a7bd5);
        // grow the strip just for the message, then collapse it again
        statusBar.setPreferredH(sidebarFontPx() + 2 * sbPadV() + 4);
        if (shellForm != null) {
            shellForm.revalidate();
        }
        if (statusClear != null) {
            statusClear.cancel();
        }
        statusClear = new com.codename1.ui.util.UITimer(new Runnable() {
            public void run() {
                statusBar.setText("");
                statusBar.getAllStyles().setBgColor(0x1d1d1d);
                statusBar.setPreferredH(0);
                if (shellForm != null) {
                    shellForm.revalidate();
                }
                statusClear = null;
            }
        });
        statusClear.schedule(3500, false, shellForm);
    }

    /** Rebuilds the native menu (checkmark or structure changes). */
    private void refreshMenu() {
        if (shellForm != null) {
            shellForm.removeAllCommands();
            installCommands(shellForm);
        }
    }

    private com.codename1.ui.Command command(String label, String menu, final Runnable action) {
        return new com.codename1.ui.Command(label) {
            public void actionPerformed(com.codename1.ui.events.ActionEvent evt) {
                action.run();
            }
        }.setDesktopMenu(menu);
    }

    private com.codename1.ui.Command separator(String menu) {
        return new com.codename1.ui.Command("-") {
            public void actionPerformed(com.codename1.ui.events.ActionEvent evt) {
            }
        }.setDesktopMenu(menu);
    }

    private com.codename1.ui.Command checked(com.codename1.ui.Command c, boolean on) {
        if (on) {
            c.putClientProperty("cn1.sim.checked", Boolean.TRUE);
        }
        return c;
    }

    private com.codename1.ui.Command urlCommand(String label, final String url) {
        return command(label, "Help", new Runnable() {
            public void run() {
                try {
                    String os = System.getProperty("os.name", "").toLowerCase();
                    if (os.contains("win")) {
                        Runtime.getRuntime().exec(new String[]{"cmd", "/c", "start", "", url});
                    } else if (os.contains("mac")) {
                        Runtime.getRuntime().exec(new String[]{"open", url});
                    } else {
                        Runtime.getRuntime().exec(new String[]{"xdg-open", url});
                    }
                } catch (java.io.IOException ex) {
                    System.err.println("[cn1sim] failed to open " + url + ": " + ex);
                }
            }
        });
    }

    private com.codename1.ui.Command networkCommand(String label, final int condition) {
        return checked(new com.codename1.ui.Command(label) {
            public void actionPerformed(com.codename1.ui.events.ActionEvent evt) {
                networkCondition = condition;
                BridgeRegistry.setNetworkCondition(condition);
                System.out.println("[cn1sim] network condition: " + getCommandName());
                refreshMenu();
            }
        }.setDesktopMenu("Tools>Network"), networkCondition == condition);
    }

    /**
     * The Device > Skin submenu: every .skin file beside the active one, the
     * current selection checked, switching delegated to the launcher.
     */
    private void installSkinsMenu(java.util.List<com.codename1.ui.Command> items) {
        if (skinsDir == null || skinSwitcher == null) {
            return;
        }
        java.io.File[] files = new java.io.File(skinsDir).listFiles();
        if (files == null) {
            return;
        }
        java.util.Arrays.sort(files);
        for (java.io.File f : files) {
            final String path = f.getAbsolutePath();
            String name = f.getName();
            if (!name.endsWith(".skin") || !f.isFile()) {
                continue;
            }
            com.codename1.ui.Command c = command(name.substring(0, name.length() - 5),
                    "Device>Skin", new Runnable() {
                        public void run() {
                            if (!path.equals(currentSkinPath)) {
                                skinSwitcher.switchTo(path);
                            }
                        }
                    });
            items.add(checked(c, path.equals(currentSkinPath)));
        }
        items.add(separator("Device>Skin"));
        items.add(stub("More...", "Device>Skin"));
    }

    /**
     * Saves a PNG of the current frame - the app screen only ("Screenshot")
     * or the entire window including the skin ("Screenshot With Skin").
     */
    private void saveScreenshot(boolean withSkin) {
        String dir = System.getProperty("cn1.sim.screenshotDir",
                System.getProperty("user.home") + "/Desktop");
        String path = dir + "/cn1-simulator-" + System.currentTimeMillis() + ".png";
        boolean ok;
        if (withSkin) {
            ok = BridgeRegistry.getShellBridge().saveScreenshot(path, 0, 0, 0, 0);
        } else {
            int[] r = getAppScreenRect();
            ok = BridgeRegistry.getShellBridge().saveScreenshot(path, r[0], r[1], r[2], r[3]);
        }
        System.out.println("[cn1sim] screenshot " + (ok ? "saved to " : "FAILED: ") + path);
        toast(ok ? "Screenshot saved to " + path : "Screenshot failed");
    }

    /**
     * Switches orientation: swaps the skin image and screen rectangle. The
     * launcher's callback resizes the native window, relayouts this shell at
     * the new size and rebinds the app universe to the new region.
     */
    private void rotate() {
        portraitOrientation = !portraitOrientation;
        skinComponent.setShouldCalcPreferredSize(true);
        if (orientationChanged != null) {
            orientationChanged.run();
        }
    }

    /** @return the active skin width (orientation-aware) */
    public int getActiveSkinWidth() {
        return activeSkin().getWidth();
    }

    /** @return the active skin height (orientation-aware) */
    public int getActiveSkinHeight() {
        return activeSkin().getHeight();
    }

    /**
     * Wires the child universe's network instrumentation into the CN1 list.
     */
    private void installToolsBridge() {
        BridgeRegistry.setToolsBridge(new ToolsBridge() {
            public void networkConnect(final int id, final String url) {
                Display.getInstance().callSerially(new Runnable() {
                    public void run() {
                        Label row = new Label("GET " + abbreviate(url));
                        styleRow(row, 0xcccccc);
                        networkRows.put(id, row);
                        networkList.add(row);
                        networkList.revalidate();
                    }
                });
            }

            public void networkMethod(final int id, final String method) {
                Display.getInstance().callSerially(new Runnable() {
                    public void run() {
                        Label row = networkRows.get(id);
                        if (row != null) {
                            String t = row.getText();
                            int sp = t.indexOf(' ');
                            row.setText(method + (sp >= 0 ? t.substring(sp) : ""));
                            networkList.repaint();
                        }
                    }
                });
            }

            public void networkResponse(final int id, final int code, final int contentLength) {
                Display.getInstance().callSerially(new Runnable() {
                    public void run() {
                        Label row = networkRows.get(id);
                        if (row != null) {
                            row.setText(row.getText() + " -> " + code);
                            styleRow(row, code >= 200 && code < 400 ? 0x66cc66 : 0xcc6666);
                            networkList.revalidate();
                        }
                    }
                });
            }

            public void perfStats(final int fps, final int usedKb, final int totalKb) {
                Display.getInstance().callSerially(new Runnable() {
                    public void run() {
                        if (perfFpsLabel != null) {
                            // setText repaints just the label region; do NOT
                            // revalidate the form - a full relayout repaints the
                            // chrome, which forces the app universe to recomposite
                            // its whole screen (a per-second flicker on the shared
                            // surface). Fixed-width labels never need relayout.
                            perfFpsLabel.setText("FPS: " + fps);
                            perfMemLabel.setText("Heap: " + (usedKb / 1024) + " / "
                                    + (totalKb / 1024) + " MB");
                        }
                    }
                });
            }

            public void inspectorTree(final String tree) {
                Display.getInstance().callSerially(new Runnable() {
                    public void run() {
                        if (inspectorList == null) {
                            return;
                        }
                        inspectorList.removeAll();
                        selectedInspectRow = null;
                        String t = tree == null ? "" : tree;
                        int start = 0;
                        int len = t.length();
                        while (start <= len) {
                            int nl = t.indexOf('\n', start);
                            String line = (nl < 0) ? t.substring(start) : t.substring(start, nl);
                            if (line.length() > 0) {
                                addInspectorRow(line);
                            }
                            if (nl < 0) {
                                break;
                            }
                            start = nl + 1;
                        }
                        inspectorList.revalidate();
                    }
                });
            }

            public void inspectorDetail(final String detail) {
                Display.getInstance().callSerially(new Runnable() {
                    public void run() {
                        if (inspectorDetail == null) {
                            return;
                        }
                        inspectorDetail.removeAll();
                        String d = detail == null ? "" : detail;
                        int start = 0;
                        int len = d.length();
                        while (start <= len) {
                            int nl = d.indexOf('\n', start);
                            String line = (nl < 0) ? d.substring(start) : d.substring(start, nl);
                            if (line.startsWith("UIID: ")) {
                                inspectorDetail.add(editableDetailRow(
                                        "UIID", "uiid", line.substring("UIID: ".length())));
                            } else if (line.startsWith("Text: ")) {
                                inspectorDetail.add(editableDetailRow(
                                        "Text", "text", line.substring("Text: ".length())));
                            } else {
                                Label row = new Label(line);
                                Style s = row.getAllStyles();
                                s.setFgColor(0xbfe0ff);
                                s.setBgTransparency(0);
                                s.setPadding(0, 0, 1, 1);
                                s.setFont(monoFont());
                                inspectorDetail.add(row);
                            }
                            if (nl < 0) {
                                break;
                            }
                            start = nl + 1;
                        }
                        inspectorDetail.revalidate();
                    }
                });
            }
        });
    }

    /**
     * An editable inspector-detail row: a mono label prefix plus a TextField
     * that pushes {@code inspectSet:<prop>=<value>} to the app universe on
     * commit (action / focus loss), producing a live change in the app UI.
     */
    private Container editableDetailRow(String labelText, final String prop, String value) {
        Container row = new Container(new com.codename1.ui.layouts.BorderLayout());
        row.getAllStyles().setBgTransparency(0);
        row.getAllStyles().setPadding(0, 0, 1, 1);
        Label prefix = new Label(labelText + ": ");
        Style ls = prefix.getAllStyles();
        ls.setFgColor(0x9fc8ff);
        ls.setBgTransparency(0);
        ls.setPadding(0, 0, 0, 0);
        ls.setFont(monoFont());
        final com.codename1.ui.TextField tf = new com.codename1.ui.TextField(value == null ? "" : value);
        tf.setSingleLineTextArea(true);
        Style ts = tf.getAllStyles();
        ts.setFgColor(0xffffff);
        ts.setBgColor(0x1e1e1e);
        ts.setBgTransparency(255);
        ts.setPadding(2, 2, 4, 4);
        ts.setMargin(0, 0, 0, 0);
        ts.setFont(monoFont());
        com.codename1.ui.events.ActionListener commit = new com.codename1.ui.events.ActionListener() {
            public void actionPerformed(com.codename1.ui.events.ActionEvent e) {
                com.codename1.impl.ios.sim.bridge.ChildControl c = BridgeRegistry.getChildControl();
                if (c != null) {
                    c.control("inspectSet:" + prop + "=" + tf.getText());
                }
            }
        };
        tf.addActionListener(commit);
        tf.setDoneListener(commit);
        row.add(com.codename1.ui.layouts.BorderLayout.WEST, prefix);
        row.add(com.codename1.ui.layouts.BorderLayout.CENTER, tf);
        return row;
    }

    /** Parses an {@code id\tdepth\tlabel} tree line into a tappable row. */
    private void addInspectorRow(String line) {
        int t1 = line.indexOf('\t');
        int t2 = t1 >= 0 ? line.indexOf('\t', t1 + 1) : -1;
        if (t2 <= t1) {
            return;
        }
        final String id = line.substring(0, t1);
        int depth = 0;
        try {
            depth = Integer.parseInt(line.substring(t1 + 1, t2));
        } catch (NumberFormatException ex) {
            depth = 0;
        }
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            text.append("  ");
        }
        text.append(line.substring(t2 + 1));
        final Label row = new Label(text.toString());
        Style s = row.getAllStyles();
        s.setFgColor(depth == 0 ? 0x66ccff : 0xdddddd);
        s.setBgColor(0x3a7bd5);
        s.setBgTransparency(0);
        s.setPadding(1, 1, 1, 1);
        s.setFont(monoFont());
        row.addPointerReleasedListener(new com.codename1.ui.events.ActionListener() {
            public void actionPerformed(com.codename1.ui.events.ActionEvent e) {
                if (selectedInspectRow != null) {
                    selectedInspectRow.getAllStyles().setBgTransparency(0);
                    selectedInspectRow.repaint();
                }
                selectedInspectRow = row;
                row.getAllStyles().setBgTransparency(150);
                row.repaint();
                com.codename1.impl.ios.sim.bridge.ChildControl c = BridgeRegistry.getChildControl();
                if (c != null) {
                    c.control("inspectSelect:" + id);
                }
            }
        });
        inspectorList.add(row);
    }

    private com.codename1.ui.Font monoFont;

    private com.codename1.ui.Font monoFont() {
        if (monoFont == null) {
            try {
                int px = Math.max(11, sidebarFontPx() - 4);
                monoFont = com.codename1.ui.Font.createTrueTypeFont(
                        "native:MainRegular", "native:MainRegular")
                        .derive(px, com.codename1.ui.Font.STYLE_PLAIN);
            } catch (Throwable t) {
                monoFont = com.codename1.ui.Font.createSystemFont(
                        com.codename1.ui.Font.FACE_MONOSPACE,
                        com.codename1.ui.Font.STYLE_PLAIN, com.codename1.ui.Font.SIZE_SMALL);
            }
        }
        return monoFont;
    }

    private void styleRow(Label row, int color) {
        Style s = row.getAllStyles();
        s.setFgColor(color);
        s.setBgColor(0x1d1d1d);
        s.setBgTransparency(255);
    }

    private static String abbreviate(String url) {
        if (url.length() > 38) {
            return url.substring(0, 35) + "...";
        }
        return url;
    }

    /* unscaled crops of the skins' screen rectangles - transparent except the
     * rounded corner bezels; composited natively on top of the app pixels */
    private Image portraitOverlayCrop;
    private Image landscapeOverlayCrop;

    /**
     * Provides the overlay crops (cut from the UNSCALED skin at the unscaled
     * screen rectangle - the native side scales while drawing).
     */
    public void setOverlayCrops(Image portrait, Image landscape) {
        portraitOverlayCrop = portrait;
        landscapeOverlayCrop = landscape;
    }

    /**
     * Re-installs the native screen overlay for the current orientation and
     * layout; call whenever the app screen rectangle moves.
     */
    public void updateScreenOverlay() {
        Image crop = portraitOrientation || landscapeOverlayCrop == null
                ? portraitOverlayCrop : landscapeOverlayCrop;
        int[] r = getAppScreenRect();
        int[] vp = getViewportRect();
        // clip = the rect's visible intersection with the viewport
        int cx1 = Math.max(r[0], vp[0]);
        int cy1 = Math.max(r[1], vp[1]);
        int cx2 = Math.min(r[0] + r[2], vp[0] + vp[2]);
        int cy2 = Math.min(r[1] + r[3], vp[1] + vp[3]);
        if (!com.codename1.impl.ios.sim.CN1SimHost.isLoaded()) {
            // RPC mode has no native overlay layer (rounded-corner bezel);
            // the skin frame painted by the shell suffices
            return;
        }
        long peer = crop == null ? 0
                : com.codename1.impl.ios.sim.child.BridgedSimImplementation.imagePeer(crop.getImage());
        com.codename1.impl.ios.sim.CN1SimHost.setScreenOverlay(peer, r[0], r[1], r[2], r[3],
                cx1, cy1, Math.max(0, cx2 - cx1), Math.max(0, cy2 - cy1));
    }

    /**
     * @return the app screen rectangle in window coordinates (valid after
     * the shell form finished its first layout)
     */
    public int[] getAppScreenRect() {
        return new int[]{
                skinComponent.getAbsoluteX() + activeScreenX(),
                skinComponent.getAbsoluteY() + activeScreenY(),
                activeScreenW(),
                activeScreenH()
        };
    }
}
