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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * The simulator's Widgets preview window (pattern: {@link JavaSECarBridge}'s auxiliary head-unit
 * window): a kind list on the left, a size selector and light/dark toggle on top, and a canvas
 * that blits the {@link SurfaceRasterizer} output of the currently published timeline. Timeline
 * entry flips auto-advance on schedule and countdown content re-renders at the rasterizer's tick
 * cadence, so developers watch exactly what a home-screen widget would do -- without leaving the
 * simulator.
 *
 * <p>A mock Dynamic Island pill plus an expanded card at the bottom render running live
 * activities (start / update / end from {@code com.codename1.surfaces.LiveActivity}).</p>
 *
 * <p>Thread rule: rasterization always runs on the Codename One EDT
 * ({@link JavaSEWidgetBridge#renderAsync}); this class only blits on the AWT thread. Clicks map
 * through the rasterizer's action rectangles into
 * {@code com.codename1.surfaces.Surfaces.dispatchAction(...)}.</p>
 */
class SimulatorWidgets implements JavaSEWidgetBridge.Listener {
    /** Rasterization scale: 2x logical points for retina crispness. */
    private static final int SCALE = 2;
    private static final int PILL_W = 250;
    private static final int PILL_H = 36;
    private static final int EXPANDED_W = 350;
    private static final int EXPANDED_H = 160;

    private static final String[] SIZE_NAMES = {"small", "medium", "large"};
    private static final int[] SIZE_W = {158, 338, 338};
    private static final int[] SIZE_H = {158, 158, 354};

    private static SimulatorWidgets instance;

    private final JavaSEWidgetBridge bridge;
    private JFrame frame;
    private JList<String> kindList;
    private final DefaultListModel<String> kindModel = new DefaultListModel<String>();
    private final List<String> kindIds = new ArrayList<String>();
    private JComboBox<String> sizeCombo;
    private JCheckBox darkToggle;
    private SurfacePanel widgetPanel;
    private JLabel timelineLabel;
    private SurfacePanel pillPanel;
    private SurfacePanel expandedPanel;
    private JLabel activityLabel;
    private Timer refreshTimer;
    private Timer activityTimer;

    /** Opens (or focuses) the preview window. Call on the AWT thread. */
    static void showWindow(JavaSEWidgetBridge bridge, JFrame owner) {
        if (instance == null) {
            instance = new SimulatorWidgets(bridge);
            instance.open(owner);
        } else {
            instance.frame.setVisible(true);
            instance.frame.toFront();
            instance.refreshKinds();
            instance.requestRender();
        }
    }

    private SimulatorWidgets(JavaSEWidgetBridge bridge) {
        this.bridge = bridge;
    }

    private void open(JFrame owner) {
        frame = new JFrame("Widgets");
        frame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);

        kindList = new JList<String>(kindModel);
        kindList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        kindList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    requestRender();
                }
            }
        });
        JScrollPane kindScroll = new JScrollPane(kindList);
        kindScroll.setPreferredSize(new Dimension(180, 200));
        kindScroll.setBorder(BorderFactory.createTitledBorder("Widget kinds"));

        sizeCombo = new JComboBox<String>(new String[] {"Small", "Medium", "Large"});
        sizeCombo.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                requestRender();
            }
        });
        darkToggle = new JCheckBox("Dark mode");
        darkToggle.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                requestRender();
                requestActivityRender();
            }
        });
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.add(new JLabel("Size:"));
        toolbar.add(sizeCombo);
        toolbar.add(darkToggle);

        widgetPanel = new SurfacePanel(SIZE_W[1], SIZE_H[1], 20, true);
        widgetPanel.setSourceLookup(new SourceLookup() {
            @Override
            public String sourceId() {
                return selectedKindId();
            }
        });
        timelineLabel = new JLabel("No timeline published yet", javax.swing.SwingConstants.CENTER);

        JPanel center = new JPanel(new BorderLayout());
        center.add(toolbar, BorderLayout.NORTH);
        JPanel canvasWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
        canvasWrap.add(widgetPanel);
        center.add(canvasWrap, BorderLayout.CENTER);
        center.add(timelineLabel, BorderLayout.SOUTH);

        // mock Dynamic Island: a dark pill (compact leading/trailing regions) + expanded card
        pillPanel = new SurfacePanel(PILL_W, PILL_H, PILL_H, false);
        pillPanel.setPillBackground(true);
        expandedPanel = new SurfacePanel(EXPANDED_W, EXPANDED_H, 24, false);
        activityLabel = new JLabel("No live activity running", javax.swing.SwingConstants.CENTER);
        JPanel island = new JPanel();
        island.setLayout(new BoxLayout(island, BoxLayout.Y_AXIS));
        island.setBorder(BorderFactory.createTitledBorder("Live activity / Dynamic Island"));
        JPanel pillWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 4));
        pillWrap.add(pillPanel);
        JPanel expandedWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 4));
        expandedWrap.add(expandedPanel);
        island.add(activityLabel);
        island.add(pillWrap);
        island.add(expandedWrap);

        frame.getContentPane().setLayout(new BorderLayout(8, 8));
        frame.getContentPane().add(kindScroll, BorderLayout.WEST);
        frame.getContentPane().add(center, BorderLayout.CENTER);
        frame.getContentPane().add(island, BorderLayout.SOUTH);
        frame.pack();
        frame.setSize(Math.max(frame.getWidth(), 660), Math.max(frame.getHeight(), 720));
        frame.setLocationRelativeTo(owner);
        frame.setVisible(true);

        bridge.addListener(this);
        refreshKinds();
        requestRender();
        requestActivityRender();
    }

    // --- JavaSEWidgetBridge.Listener (may fire on the CN1 EDT) --------------------

    @Override
    public void widgetKindRegistered(final String kindId) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                refreshKinds();
            }
        });
    }

    @Override
    public void widgetTimelinePublished(final String kindId) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                refreshKinds();
                if (kindId != null && kindId.equals(selectedKindId())) {
                    requestRender();
                }
            }
        });
    }

    @Override
    public void widgetsReloaded(String kindId) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                requestRender();
            }
        });
    }

    @Override
    public void liveActivityStarted(String activityId) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                requestActivityRender();
            }
        });
    }

    @Override
    public void liveActivityUpdated(String activityId) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                requestActivityRender();
            }
        });
    }

    @Override
    public void liveActivityEnded(String activityId, boolean dismissImmediately) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                requestActivityRender();
            }
        });
    }

    // --- widget rendering ----------------------------------------------------------

    private void refreshKinds() {
        String selected = selectedKindId();
        kindIds.clear();
        kindIds.addAll(bridge.getKindIds());
        kindModel.clear();
        int selectIndex = kindIds.isEmpty() ? -1 : 0;
        for (int i = 0; i < kindIds.size(); i++) {
            String id = kindIds.get(i);
            kindModel.addElement(bridge.getKindDisplayName(id));
            if (id.equals(selected)) {
                selectIndex = i;
            }
        }
        if (selectIndex >= 0) {
            kindList.setSelectedIndex(selectIndex);
        }
    }

    private String selectedKindId() {
        int index = kindList == null ? -1 : kindList.getSelectedIndex();
        return index >= 0 && index < kindIds.size() ? kindIds.get(index) : null;
    }

    private String selectedSizeName() {
        int index = Math.max(0, sizeCombo.getSelectedIndex());
        return SIZE_NAMES[index];
    }

    /** Re-rasterizes the selected kind at the selected size; safe to call from any thread. */
    private void requestRender() {
        if (frame == null || !frame.isVisible()) {
            return;
        }
        final String kindId = selectedKindId();
        final String sizeName = selectedSizeName();
        final int sizeIndex = Math.max(0, sizeCombo.getSelectedIndex());
        final boolean dark = darkToggle.isSelected();
        final Map<String, Object> doc = kindId == null ? null : bridge.getTimelineDoc(kindId);
        final long now = System.currentTimeMillis();
        final Map<String, Object> layout = SurfaceRasterizer.layoutForSize(doc, sizeName);
        widgetPanel.setLogicalSize(SIZE_W[sizeIndex], SIZE_H[sizeIndex]);
        if (layout == null) {
            widgetPanel.showImage(null, new ArrayList<SurfaceRasterizer.ActionRect>());
            updateTimelineLabel(doc, now);
            return;
        }
        Map<String, Object> entry = SurfaceRasterizer.currentEntry(doc, now);
        Map<String, Object> state = entry == null ? new HashMap<String, Object>()
                : asMap(entry.get("state"));
        JavaSEWidgetBridge.renderAsync(layout, state, bridge.getKindImages(kindId),
                SIZE_W[sizeIndex], SIZE_H[sizeIndex], SCALE, dark,
                new JavaSEWidgetBridge.RenderCallback() {
                    @Override
                    public void rendered(BufferedImage image,
                            List<SurfaceRasterizer.ActionRect> actions, long nextTickMillis) {
                        widgetPanel.showImage(image, actions);
                        updateTimelineLabel(doc, System.currentTimeMillis());
                        scheduleRefresh(doc, nextTickMillis);
                    }
                });
    }

    private void updateTimelineLabel(Map<String, Object> doc, long now) {
        if (doc == null) {
            timelineLabel.setText("No timeline published yet -- call Surfaces.publish(...)");
            return;
        }
        List<?> entries = doc.get("entries") instanceof List ? (List<?>) doc.get("entries")
                : new ArrayList<Object>();
        Map<String, Object> current = SurfaceRasterizer.currentEntry(doc, now);
        int index = current == null ? -1 : entries.indexOf(current);
        long flip = SurfaceRasterizer.nextEntryFlip(doc, now);
        StringBuilder sb = new StringBuilder();
        sb.append("Timeline: entry ").append(index + 1).append(" of ").append(entries.size());
        if (flip > 0) {
            sb.append(", next flip in ").append(Math.max(0, (flip - now) / 1000)).append("s");
        }
        timelineLabel.setText(sb.toString());
    }

    /** Arms one swing timer at the earlier of the rasterizer tick and the next entry flip. */
    private void scheduleRefresh(Map<String, Object> doc, long nextTickMillis) {
        long now = System.currentTimeMillis();
        long due = nextTickMillis;
        long flip = SurfaceRasterizer.nextEntryFlip(doc, now);
        if (flip > 0 && (due == 0 || flip < due)) {
            due = flip;
        }
        if (refreshTimer != null) {
            refreshTimer.stop();
            refreshTimer = null;
        }
        if (due <= 0 || !frame.isVisible()) {
            return;
        }
        refreshTimer = new Timer((int) Math.max(50, due - now), new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                requestRender();
            }
        });
        refreshTimer.setRepeats(false);
        refreshTimer.start();
    }

    // --- live activity rendering ------------------------------------------------------

    private void requestActivityRender() {
        List<String> ids = bridge.getLiveActivityIds();
        String activityId = ids.isEmpty() ? null : ids.get(ids.size() - 1);
        final JavaSEWidgetBridge.LiveActivityRecord rec =
                activityId == null ? null : bridge.getLiveActivity(activityId);
        if (activityTimer != null) {
            activityTimer.stop();
            activityTimer = null;
        }
        if (rec == null) {
            activityLabel.setText("No live activity running");
            pillPanel.showImage(null, new ArrayList<SurfaceRasterizer.ActionRect>());
            expandedPanel.showImage(null, new ArrayList<SurfaceRasterizer.ActionRect>());
            return;
        }
        Object type = rec.getDescriptor().get("type");
        activityLabel.setText("Live activity: " + (type instanceof String ? (String) type : "?"));
        pillPanel.setSourceLookupValue(type instanceof String ? (String) type : null);
        expandedPanel.setSourceLookupValue(type instanceof String ? (String) type : null);
        Map<String, Object> pillNode = buildPillNode(rec.getDescriptor());
        // the island pill is always dark, matching the hardware cutout it hugs
        JavaSEWidgetBridge.renderAsync(pillNode, rec.getState(), rec.getImages(),
                PILL_W, PILL_H, SCALE, true, new JavaSEWidgetBridge.RenderCallback() {
                    @Override
                    public void rendered(BufferedImage image,
                            List<SurfaceRasterizer.ActionRect> actions, long nextTickMillis) {
                        pillPanel.showImage(image, actions);
                        scheduleActivityRefresh(nextTickMillis);
                    }
                });
        Map<String, Object> content = asMap(rec.getDescriptor().get("content"));
        if (!content.isEmpty()) {
            JavaSEWidgetBridge.renderAsync(content, rec.getState(), rec.getImages(),
                    EXPANDED_W, EXPANDED_H, SCALE, darkToggle.isSelected(),
                    new JavaSEWidgetBridge.RenderCallback() {
                        @Override
                        public void rendered(BufferedImage image,
                                List<SurfaceRasterizer.ActionRect> actions, long nextTickMillis) {
                            expandedPanel.showImage(image, actions);
                            scheduleActivityRefresh(nextTickMillis);
                        }
                    });
        }
    }

    private void scheduleActivityRefresh(long nextTickMillis) {
        long now = System.currentTimeMillis();
        if (nextTickMillis <= 0 || !frame.isVisible()) {
            return;
        }
        if (activityTimer != null && activityTimer.isRunning()) {
            return;
        }
        activityTimer = new Timer((int) Math.max(50, nextTickMillis - now),
                new java.awt.event.ActionListener() {
                    @Override
                    public void actionPerformed(java.awt.event.ActionEvent e) {
                        requestActivityRender();
                    }
                });
        activityTimer.setRepeats(false);
        activityTimer.start();
    }

    /**
     * Synthesizes the compact Dynamic Island pill layout: compact leading and trailing regions
     * pushed apart by a spacer, falling back to the minimal region or the activity type name.
     */
    static Map<String, Object> buildPillNode(Map<String, Object> descriptor) {
        Map<String, Object> island = asMap(descriptor.get("island"));
        Object leading = island.get("compactLeading");
        Object trailing = island.get("compactTrailing");
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("t", "row");
        List<Object> pad = new ArrayList<Object>();
        pad.add(Integer.valueOf(4));
        pad.add(Integer.valueOf(14));
        pad.add(Integer.valueOf(4));
        pad.add(Integer.valueOf(14));
        row.put("pad", pad);
        row.put("spacing", Integer.valueOf(8));
        List<Object> children = new ArrayList<Object>();
        if (leading instanceof Map || trailing instanceof Map) {
            if (leading instanceof Map) {
                children.add(leading);
            }
            Map<String, Object> spacer = new LinkedHashMap<String, Object>();
            spacer.put("t", "spacer");
            children.add(spacer);
            if (trailing instanceof Map) {
                children.add(trailing);
            }
        } else if (island.get("minimal") instanceof Map) {
            children.add(island.get("minimal"));
        } else {
            Map<String, Object> label = new LinkedHashMap<String, Object>();
            label.put("t", "text");
            Object type = descriptor.get("type");
            label.put("text", type instanceof String ? type : "live activity");
            label.put("size", Integer.valueOf(12));
            children.add(label);
        }
        row.put("ch", children);
        return row;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object o) {
        return o instanceof Map ? (Map<String, Object>) o : new HashMap<String, Object>();
    }

    // --- the blit panel -------------------------------------------------------------

    private interface SourceLookup {
        String sourceId();
    }

    /**
     * Paints the rasterized ARGB output at logical size (the backing image is {@code SCALE}x for
     * retina crispness) and maps clicks through the scaled action rectangles into
     * {@code Surfaces.dispatchAction}.
     */
    private static final class SurfacePanel extends JPanel {
        private BufferedImage image;
        private List<SurfaceRasterizer.ActionRect> actions =
                new ArrayList<SurfaceRasterizer.ActionRect>();
        private int logicalWidth;
        private int logicalHeight;
        private final boolean checkerBackdrop;
        private boolean pillBackground;
        private SourceLookup sourceLookup;
        private String sourceLookupValue;

        SurfacePanel(int logicalWidth, int logicalHeight, int cornerRadius,
                boolean checkerBackdrop) {
            this.logicalWidth = logicalWidth;
            this.logicalHeight = logicalHeight;
            this.checkerBackdrop = checkerBackdrop;
            setPreferredSize(new Dimension(logicalWidth, logicalHeight));
            setOpaque(false);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    dispatchClick(e.getX(), e.getY());
                }
            });
        }

        void setSourceLookup(SourceLookup lookup) {
            this.sourceLookup = lookup;
        }

        void setSourceLookupValue(String value) {
            this.sourceLookupValue = value;
        }

        void setPillBackground(boolean pill) {
            this.pillBackground = pill;
        }

        void setLogicalSize(int w, int h) {
            if (w != logicalWidth || h != logicalHeight) {
                logicalWidth = w;
                logicalHeight = h;
                setPreferredSize(new Dimension(w, h));
                revalidate();
            }
        }

        void showImage(BufferedImage img, List<SurfaceRasterizer.ActionRect> actionRects) {
            this.image = img;
            this.actions = actionRects;
            setCursor(Cursor.getPredefinedCursor(
                    actionRects.isEmpty() ? Cursor.DEFAULT_CURSOR : Cursor.HAND_CURSOR));
            repaint();
        }

        private void dispatchClick(int x, int y) {
            // the click arrives in logical pixels; the action rects are in SCALE-d image pixels
            int px = x * SCALE;
            int py = y * SCALE;
            for (SurfaceRasterizer.ActionRect r : actions) {
                if (px >= r.getX() && px < r.getX() + r.getWidth()
                        && py >= r.getY() && py < r.getY() + r.getHeight()) {
                    String source = sourceLookupValue != null ? sourceLookupValue
                            : (sourceLookup == null ? null : sourceLookup.sourceId());
                    // Surfaces.dispatchAction marshals to the CN1 EDT itself
                    Surfaces.dispatchAction(source, r.getActionId(), r.getParams());
                    return;
                }
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            if (checkerBackdrop) {
                g2.setColor(new Color(0xEDEDED));
                g2.fillRoundRect(0, 0, logicalWidth, logicalHeight, 20, 20);
            }
            if (pillBackground) {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.BLACK);
                g2.fillRoundRect(0, 0, logicalWidth, logicalHeight, logicalHeight, logicalHeight);
            }
            if (image != null) {
                g2.drawImage(image, 0, 0, logicalWidth, logicalHeight, null);
            }
            g2.dispose();
        }
    }
}
