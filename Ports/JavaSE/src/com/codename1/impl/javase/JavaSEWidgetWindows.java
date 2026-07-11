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
package com.codename1.impl.javase;

import com.codename1.surfaces.SurfaceRasterizer;
import com.codename1.surfaces.Surfaces;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.MenuItem;
import java.awt.Point;
import java.awt.PopupMenu;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.TrayIcon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Desktop floating widgets: in a desktop (non-simulator) build every pinned widget kind gets a
 * frameless, always-on-top {@link JWindow} that renders the kind's published timeline through
 * {@link SurfaceRasterizer} -- the desktop equivalent of a home-screen widget. Windows drag
 * anywhere to move, offer a right-click menu (size selection, remove) and keep rounded corners
 * via {@code setShape}. Geometry and the pinned set persist in {@link Preferences} (debounced,
 * following the simulator {@code AppFrame} pattern) so widgets restore where the user left them
 * on the next run. Widgets are process-bound in this version: they exist while the app process
 * runs (a tray keep-alive mode is a possible follow-up).
 *
 * <p>A running live activity docks a black pill window at the top-center of the primary screen
 * (start / update / end), mirroring the Dynamic Island.</p>
 *
 * <p>Clicks inside a rasterized action rectangle focus the main app window and dispatch through
 * {@code Surfaces.dispatchAction}; clicks elsewhere start a window drag. Rasterization always
 * happens on the Codename One EDT via {@link JavaSEWidgetBridge#renderAsync}; only blits touch
 * the AWT thread.</p>
 */
class JavaSEWidgetWindows implements JavaSEWidgetBridge.Listener {
    private static final int SCALE = 2;
    private static final int CORNER = 24;
    private static final String[] SIZE_NAMES = {"small", "medium", "large"};
    private static final int[] SIZE_W = {158, 338, 338};
    private static final int[] SIZE_H = {158, 158, 354};
    private static final int PILL_W = 250;
    private static final int PILL_H = 36;

    private final JavaSEWidgetBridge bridge;
    private final JFrame mainWindow;
    private final Map<String, WidgetWindow> windows = new LinkedHashMap<String, WidgetWindow>();
    private final Preferences prefs = Preferences.userNodeForPackage(JavaSEWidgetWindows.class);
    private final String prefPrefix;
    private Timer saveTimer;
    private PillWindow pillWindow;
    private TrayIcon trayIcon;

    JavaSEWidgetWindows(JavaSEWidgetBridge bridge, JFrame mainWindow) {
        this.bridge = bridge;
        this.mainWindow = mainWindow;
        String mainClass = System.getProperty("MainClass");
        this.prefPrefix = "cn1.surfaces." + (mainClass == null ? "app" : mainClass) + ".";
        bridge.addListener(this);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                restorePinned();
            }
        });
    }

    // --- pin management ------------------------------------------------------------

    /** Pins a floating widget for a kind (one instance per kind). Any thread. */
    void pinWidget(final String kindId, final String sizeName) {
        if (kindId == null) {
            return;
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                WidgetWindow w = windows.get(kindId);
                if (w == null) {
                    w = new WidgetWindow(kindId, normalizeSize(sizeName), null);
                    synchronized (windows) {
                        // mutations happen on the AWT thread; the lock only publishes them
                        // safely to getPinnedCount which may run on the CN1 EDT
                        windows.put(kindId, w);
                    }
                    w.setVisible(true);
                    w.requestRender();
                } else if (sizeName != null) {
                    w.setSizeName(normalizeSize(sizeName));
                }
                savePinnedLater();
            }
        });
    }

    /** Removes a pinned widget window. AWT thread. */
    void unpinWidget(String kindId) {
        WidgetWindow w;
        synchronized (windows) {
            w = windows.remove(kindId);
        }
        if (w != null) {
            w.close();
        }
        savePinnedLater();
    }

    /** The number of pinned instances of a kind (0 or 1 in this version). */
    int getPinnedCount(String kindId) {
        synchronized (windows) {
            return windows.containsKey(kindId) ? 1 : 0;
        }
    }

    /**
     * Hooks the "Add widget: ..." items into the port's persistent tray icon. The port only
     * creates its tray icon lazily (for desktop notifications), so this is called when that
     * happens; until then {@link JavaSEWidgetBridge#pinWidget} is the pinning entry point.
     */
    void installTrayMenu(final TrayIcon tray) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                trayIcon = tray;
                refreshTrayMenu();
            }
        });
    }

    private void refreshTrayMenu() {
        if (trayIcon == null) {
            return;
        }
        PopupMenu menu = new PopupMenu();
        for (final String kindId : bridge.getKindIds()) {
            MenuItem item = new MenuItem("Add widget: " + bridge.getKindDisplayName(kindId));
            item.addActionListener(new java.awt.event.ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    pinWidget(kindId, null);
                }
            });
            menu.add(item);
        }
        trayIcon.setPopupMenu(menu);
    }

    private void restorePinned() {
        String pinned = prefs.get(prefPrefix + "pinned", "");
        if (pinned.length() == 0) {
            return;
        }
        for (String entry : pinned.split(";")) {
            int sep = entry.indexOf('|');
            if (sep <= 0) {
                continue;
            }
            String kindId = entry.substring(0, sep);
            String sizeName = normalizeSize(entry.substring(sep + 1));
            if (!windows.containsKey(kindId)) {
                Point location = loadLocation(kindId);
                WidgetWindow w = new WidgetWindow(kindId, sizeName, location);
                synchronized (windows) {
                    windows.put(kindId, w);
                }
                w.setVisible(true);
                w.requestRender();
            }
        }
    }

    private Point loadLocation(String kindId) {
        String geom = prefs.get(prefPrefix + "geom." + kindId, null);
        if (geom == null) {
            return null;
        }
        int comma = geom.indexOf(',');
        if (comma <= 0) {
            return null;
        }
        try {
            return new Point(Integer.parseInt(geom.substring(0, comma)),
                    Integer.parseInt(geom.substring(comma + 1)));
        } catch (NumberFormatException err) {
            return null;
        }
    }

    /** Debounced persistence of the pinned set + geometry (AppFrame pattern). */
    private void savePinnedLater() {
        if (saveTimer != null) {
            saveTimer.restart();
            return;
        }
        saveTimer = new Timer(500, new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                savePinnedNow();
            }
        });
        saveTimer.setRepeats(false);
        saveTimer.start();
    }

    private void savePinnedNow() {
        StringBuilder sb = new StringBuilder();
        for (WidgetWindow w : windows.values()) {
            if (sb.length() > 0) {
                sb.append(';');
            }
            sb.append(w.kindId).append('|').append(w.sizeName);
            prefs.put(prefPrefix + "geom." + w.kindId, w.getX() + "," + w.getY());
        }
        prefs.put(prefPrefix + "pinned", sb.toString());
    }

    private static String normalizeSize(String sizeName) {
        for (String s : SIZE_NAMES) {
            if (s.equals(sizeName)) {
                return s;
            }
        }
        return "small";
    }

    private static int sizeIndex(String sizeName) {
        for (int i = 0; i < SIZE_NAMES.length; i++) {
            if (SIZE_NAMES[i].equals(sizeName)) {
                return i;
            }
        }
        return 0;
    }

    private void focusMainWindow() {
        if (mainWindow != null) {
            mainWindow.setState(java.awt.Frame.NORMAL);
            mainWindow.toFront();
            mainWindow.requestFocus();
        }
    }

    // --- JavaSEWidgetBridge.Listener (may fire on the CN1 EDT) -------------------------

    @Override
    public void widgetKindRegistered(String kindId) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                refreshTrayMenu();
            }
        });
    }

    @Override
    public void widgetTimelinePublished(final String kindId) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                WidgetWindow w = windows.get(kindId);
                if (w != null) {
                    w.requestRender();
                }
            }
        });
    }

    @Override
    public void widgetsReloaded(final String kindId) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                for (WidgetWindow w : new ArrayList<WidgetWindow>(windows.values())) {
                    if (kindId == null || kindId.equals(w.kindId)) {
                        w.requestRender();
                    }
                }
            }
        });
    }

    @Override
    public void liveActivityStarted(final String activityId) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (pillWindow != null) {
                    pillWindow.close();
                }
                pillWindow = new PillWindow(activityId);
                pillWindow.setVisible(true);
                pillWindow.requestRender();
            }
        });
    }

    @Override
    public void liveActivityUpdated(final String activityId) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (pillWindow != null && activityId.equals(pillWindow.activityId)) {
                    pillWindow.requestRender();
                }
            }
        });
    }

    @Override
    public void liveActivityEnded(final String activityId, final boolean dismissImmediately) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (pillWindow == null || !activityId.equals(pillWindow.activityId)) {
                    return;
                }
                final PillWindow closing = pillWindow;
                pillWindow = null;
                if (dismissImmediately) {
                    closing.close();
                } else {
                    // linger briefly on the final state before dismissing, like the platforms do
                    Timer linger = new Timer(3000, new java.awt.event.ActionListener() {
                        @Override
                        public void actionPerformed(java.awt.event.ActionEvent e) {
                            closing.close();
                        }
                    });
                    linger.setRepeats(false);
                    linger.start();
                }
            }
        });
    }

    // --- the surface windows -------------------------------------------------------

    /** Shared behavior of the floating surface windows: blitting, dragging, click dispatch. */
    private abstract class SurfaceWindow extends JWindow {
        BufferedImage image;
        List<SurfaceRasterizer.ActionRect> actions =
                new ArrayList<SurfaceRasterizer.ActionRect>();
        Timer refreshTimer;
        private Point dragOffset;
        private boolean dragged;

        SurfaceWindow() {
            setAlwaysOnTop(true);
            final JPanel panel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    paintSurface((Graphics2D) g);
                }
            };
            panel.setOpaque(false);
            setContentPane(panel);
            setBackground(new Color(0, 0, 0, 0));
            MouseAdapter mouse = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    dragged = false;
                    dragOffset = e.getPoint();
                    if (e.isPopupTrigger()) {
                        showContextMenu(e);
                    }
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (dragOffset != null) {
                        dragged = true;
                        setLocation(e.getXOnScreen() - dragOffset.x,
                                e.getYOnScreen() - dragOffset.y);
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        showContextMenu(e);
                        return;
                    }
                    if (dragged) {
                        moved();
                        return;
                    }
                    int px = e.getX() * SCALE;
                    int py = e.getY() * SCALE;
                    for (SurfaceRasterizer.ActionRect r : actions) {
                        if (px >= r.getX() && px < r.getX() + r.getWidth()
                                && py >= r.getY() && py < r.getY() + r.getHeight()) {
                            focusMainWindow();
                            // Surfaces.dispatchAction marshals to the CN1 EDT itself
                            Surfaces.dispatchAction(actionSource(), r.getActionId(),
                                    r.getParams());
                            return;
                        }
                    }
                }
            };
            panel.addMouseListener(mouse);
            panel.addMouseMotionListener(mouse);
        }

        abstract void paintSurface(Graphics2D g2);

        abstract String actionSource();

        abstract void requestRender();

        void showContextMenu(MouseEvent e) {
        }

        void moved() {
        }

        void showImage(BufferedImage img, List<SurfaceRasterizer.ActionRect> actionRects,
                long nextTickMillis, long nextFlipMillis) {
            this.image = img;
            this.actions = actionRects;
            repaint();
            long now = System.currentTimeMillis();
            long due = nextTickMillis;
            if (nextFlipMillis > 0 && (due == 0 || nextFlipMillis < due)) {
                due = nextFlipMillis;
            }
            if (refreshTimer != null) {
                refreshTimer.stop();
                refreshTimer = null;
            }
            if (due > 0 && isVisible()) {
                refreshTimer = new Timer((int) Math.max(50, due - now),
                        new java.awt.event.ActionListener() {
                            @Override
                            public void actionPerformed(java.awt.event.ActionEvent e) {
                                requestRender();
                            }
                        });
                refreshTimer.setRepeats(false);
                refreshTimer.start();
            }
        }

        void close() {
            if (refreshTimer != null) {
                refreshTimer.stop();
                refreshTimer = null;
            }
            dispose();
        }
    }

    /** A pinned desktop widget rendering one kind's published timeline. */
    private final class WidgetWindow extends SurfaceWindow {
        final String kindId;
        String sizeName;

        WidgetWindow(String kindId, String sizeName, Point location) {
            this.kindId = kindId;
            this.sizeName = sizeName;
            applyWindowSize();
            if (location != null) {
                setLocation(clampToScreen(location));
            } else {
                Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment()
                        .getMaximumWindowBounds();
                setLocation(screen.x + screen.width - getWidth() - 40, screen.y + 60);
            }
        }

        void setSizeName(String sizeName) {
            this.sizeName = sizeName;
            applyWindowSize();
            requestRender();
            savePinnedLater();
        }

        private void applyWindowSize() {
            int index = sizeIndex(sizeName);
            setSize(SIZE_W[index], SIZE_H[index]);
            setShape(new RoundRectangle2D.Float(0, 0, SIZE_W[index], SIZE_H[index],
                    CORNER, CORNER));
        }

        private Point clampToScreen(Point p) {
            Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getMaximumWindowBounds();
            int x = Math.max(screen.x, Math.min(p.x, screen.x + screen.width - getWidth()));
            int y = Math.max(screen.y, Math.min(p.y, screen.y + screen.height - getHeight()));
            return new Point(x, y);
        }

        @Override
        void requestRender() {
            int index = sizeIndex(sizeName);
            final Map<String, Object> doc = bridge.getTimelineDoc(kindId);
            final long now = System.currentTimeMillis();
            Map<String, Object> layout = SurfaceRasterizer.layoutForSize(doc, sizeName);
            if (layout == null) {
                showImage(null, new ArrayList<SurfaceRasterizer.ActionRect>(), 0, 0);
                return;
            }
            Map<String, Object> entry = SurfaceRasterizer.currentEntry(doc, now);
            Map<String, Object> state = entry == null ? new HashMap<String, Object>()
                    : asMap(entry.get("state"));
            JavaSEWidgetBridge.renderAsync(layout, state, bridge.getKindImages(kindId),
                    SIZE_W[index], SIZE_H[index], SCALE, isSystemDark(),
                    new JavaSEWidgetBridge.RenderCallback() {
                        @Override
                        public void rendered(BufferedImage img,
                                List<SurfaceRasterizer.ActionRect> actionRects,
                                long nextTickMillis) {
                            showImage(img, actionRects, nextTickMillis,
                                    SurfaceRasterizer.nextEntryFlip(doc,
                                            System.currentTimeMillis()));
                        }
                    });
        }

        @Override
        String actionSource() {
            return kindId;
        }

        @Override
        void moved() {
            savePinnedLater();
        }

        @Override
        void showContextMenu(MouseEvent e) {
            JPopupMenu menu = new JPopupMenu();
            ButtonGroup group = new ButtonGroup();
            for (final String s : SIZE_NAMES) {
                JRadioButtonMenuItem item = new JRadioButtonMenuItem(
                        Character.toUpperCase(s.charAt(0)) + s.substring(1), s.equals(sizeName));
                item.addActionListener(new java.awt.event.ActionListener() {
                    @Override
                    public void actionPerformed(java.awt.event.ActionEvent ev) {
                        setSizeName(s);
                    }
                });
                group.add(item);
                menu.add(item);
            }
            menu.addSeparator();
            JMenuItem remove = new JMenuItem("Remove");
            remove.addActionListener(new java.awt.event.ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent ev) {
                    unpinWidget(kindId);
                }
            });
            menu.add(remove);
            menu.show(e.getComponent(), e.getX(), e.getY());
        }

        @Override
        void paintSurface(Graphics2D g2) {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(isSystemDark() ? new Color(0x1C1C1E) : Color.WHITE);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), CORNER, CORNER);
            if (image != null) {
                g2.drawImage(image, 0, 0, getWidth(), getHeight(), null);
            } else {
                g2.setColor(isSystemDark() ? Color.WHITE : new Color(0x1C1C1E));
                String name = bridge.getKindDisplayName(kindId);
                int w = g2.getFontMetrics().stringWidth(name);
                g2.drawString(name, (getWidth() - w) / 2, getHeight() / 2);
            }
        }
    }

    /** The live activity pill docked at the top-center of the primary screen. */
    private final class PillWindow extends SurfaceWindow {
        final String activityId;

        PillWindow(String activityId) {
            this.activityId = activityId;
            setSize(PILL_W, PILL_H);
            setShape(new RoundRectangle2D.Float(0, 0, PILL_W, PILL_H, PILL_H, PILL_H));
            Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getMaximumWindowBounds();
            setLocation(screen.x + (screen.width - PILL_W) / 2, screen.y + 8);
        }

        @Override
        void requestRender() {
            JavaSEWidgetBridge.LiveActivityRecord rec = bridge.getLiveActivity(activityId);
            if (rec == null) {
                // ended: the final state was already applied to the record before removal, keep
                // whatever pixels we have during the linger period
                return;
            }
            Map<String, Object> pillNode = SimulatorWidgets.buildPillNode(rec.getDescriptor());
            JavaSEWidgetBridge.renderAsync(pillNode, rec.getState(), rec.getImages(),
                    PILL_W, PILL_H, SCALE, true, new JavaSEWidgetBridge.RenderCallback() {
                        @Override
                        public void rendered(BufferedImage img,
                                List<SurfaceRasterizer.ActionRect> actionRects,
                                long nextTickMillis) {
                            showImage(img, actionRects, nextTickMillis, 0);
                        }
                    });
        }

        @Override
        String actionSource() {
            JavaSEWidgetBridge.LiveActivityRecord rec = bridge.getLiveActivity(activityId);
            Object type = rec == null ? null : rec.getDescriptor().get("type");
            return type instanceof String ? (String) type : activityId;
        }

        @Override
        void paintSurface(Graphics2D g2) {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.BLACK);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            if (image != null) {
                g2.drawImage(image, 0, 0, getWidth(), getHeight(), null);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object o) {
        return o instanceof Map ? (Map<String, Object>) o : new HashMap<String, Object>();
    }

    /** Follows the simulator's dark-mode flag when available; defaults to light. */
    private static boolean isSystemDark() {
        try {
            com.codename1.ui.Display d = com.codename1.ui.Display.getInstance();
            Boolean dark = d.isDarkMode();
            return dark != null && dark.booleanValue();
        } catch (Throwable t) {
            return false;
        }
    }
}
