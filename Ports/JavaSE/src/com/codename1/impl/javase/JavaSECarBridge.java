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

import com.codename1.car.Car;
import com.codename1.car.CarAction;
import com.codename1.car.CarActionListener;
import com.codename1.car.CarActionStrip;
import com.codename1.car.CarContext;
import com.codename1.car.CarGridItem;
import com.codename1.car.CarGridTemplate;
import com.codename1.car.CarListTemplate;
import com.codename1.car.CarMessageTemplate;
import com.codename1.car.CarNavigationTemplate;
import com.codename1.car.CarNowPlayingTemplate;
import com.codename1.car.CarPaneTemplate;
import com.codename1.car.CarRow;
import com.codename1.car.CarScreen;
import com.codename1.car.CarSection;
import com.codename1.car.CarTemplate;
import com.codename1.car.spi.CarBridge;
import com.codename1.ui.Display;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;

/**
 * A simulator-only {@link CarBridge} that renders the portable {@code com.codename1.car} template
 * tree into a Swing "head unit" window so developers can see and click through their CarPlay /
 * Android Auto experience locally -- without an iOS Simulator CarPlay session or the Android Auto
 * Desktop Head Unit. It is created and torn down from the simulator's <b>Car</b> menu.
 *
 * <p>This is a debugging aid, not a pixel-accurate emulation: it approximates the two head-unit
 * styles (a dark CarPlay-ish blue theme and an Android-Auto-ish green theme), renders the same
 * fixed template catalogue the real platforms allow, and routes row / grid / action selections back
 * to the application's {@link CarActionListener}s on the Codename One EDT, so navigation
 * ({@link CarContext#pushScreen}/{@link CarContext#popScreen}) works exactly as it would in a car.</p>
 */
public class JavaSECarBridge implements CarBridge {

    /** Head-unit style. */
    public enum Style { CARPLAY, ANDROID_AUTO }

    private final Style style;
    private final Color bg;
    private final Color panel;
    private final Color accent;
    private final Color textPrimary = new Color(0xEDEDED);
    private final Color textSecondary = new Color(0x9A9CA2);

    private JFrame frame;
    private JPanel header;
    private JPanel content;
    private JLabel titleLabel;
    private JButton backButton;
    private JLabel toast;
    private Timer toastTimer;

    private final List<CarScreen> stack = new ArrayList<CarScreen>();
    private volatile boolean connected;

    public JavaSECarBridge(Style style) {
        this.style = style;
        if (style == Style.CARPLAY) {
            bg = new Color(0x0C0E12);
            panel = new Color(0x1A1C21);
            accent = new Color(0x52A8FF);
        } else {
            bg = new Color(0x14161A);
            panel = new Color(0x1E2126);
            accent = new Color(0x7EC882);
        }
    }

    /** Builds the head-unit window. Call on the AWT thread. {@code onClose} runs when the window is
     * closed (the simulator uses it to fully disconnect the simulated head unit). */
    void openWindow(JFrame owner, final Runnable onClose) {
        connected = true;
        frame = new JFrame((style == Style.CARPLAY ? "CarPlay" : "Android Auto") + " - Simulated head unit");
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                // Closing the window disconnects the head unit (mirrors a real disconnect).
                if (onClose != null) {
                    onClose.run();
                }
            }
        });
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(bg);

        header = new JPanel(new BorderLayout());
        header.setBackground(new Color(0x0A0B0D));
        header.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        backButton = new JButton("‹ Back");
        styleHeaderButton(backButton);
        backButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                final CarContext ctx = Car.getCurrentContext();
                if (ctx != null) {
                    Display.getInstance().callSerially(new Runnable() {
                        @Override
                        public void run() {
                            ctx.popScreen();
                        }
                    });
                }
            }
        });
        backButton.setVisible(false);
        titleLabel = new JLabel("", SwingConstants.CENTER);
        titleLabel.setForeground(textPrimary);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 18f));
        JLabel badge = new JLabel(style == Style.CARPLAY ? "CarPlay" : "Android Auto", SwingConstants.RIGHT);
        badge.setForeground(accent);
        badge.setFont(badge.getFont().deriveFont(Font.BOLD, 12f));
        header.add(backButton, BorderLayout.WEST);
        header.add(titleLabel, BorderLayout.CENTER);
        header.add(badge, BorderLayout.EAST);

        content = new JPanel();
        content.setBackground(bg);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        JScrollPane scroll = new JScrollPane(content,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getViewport().setBackground(bg);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        toast = new JLabel("", SwingConstants.CENTER);
        toast.setOpaque(true);
        toast.setBackground(new Color(0x32, 0x34, 0x38));
        toast.setForeground(textPrimary);
        toast.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        toast.setVisible(false);

        root.add(header, BorderLayout.NORTH);
        root.add(scroll, BorderLayout.CENTER);
        root.add(toast, BorderLayout.SOUTH);
        frame.setContentPane(root);
        frame.setSize(960, 540);
        frame.setLocationRelativeTo(owner);
        frame.setVisible(true);
    }

    // --- CarBridge -----------------------------------------------------------

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public void pushScreen(CarScreen screen) {
        stack.add(screen);
        renderTop();
    }

    @Override
    public void popScreen() {
        if (stack.size() > 1) {
            stack.remove(stack.size() - 1);
        }
        renderTop();
    }

    @Override
    public void invalidate(CarScreen screen) {
        if (!stack.isEmpty() && stack.get(stack.size() - 1) == screen) {
            renderTop();
        }
    }

    @Override
    public void finish() {
        Car.endSession();
    }

    @Override
    public void showToast(final String message, final int durationSeconds) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (toast == null) {
                    return;
                }
                toast.setText(message);
                toast.setVisible(true);
                frame.revalidate();
                if (toastTimer != null) {
                    toastTimer.stop();
                }
                toastTimer = new Timer(Math.max(1, durationSeconds) * 1000, new java.awt.event.ActionListener() {
                    @Override
                    public void actionPerformed(java.awt.event.ActionEvent e) {
                        toast.setVisible(false);
                        frame.revalidate();
                    }
                });
                toastTimer.setRepeats(false);
                toastTimer.start();
            }
        });
    }

    @Override
    public int getListRowLimit() {
        // Representative driver-distraction caps so apps can see the limit honoured locally.
        return style == Style.CARPLAY ? 12 : 6;
    }

    @Override
    public int getGridItemLimit() {
        return 8;
    }

    /** Tears down the window; called by the simulator on disconnect. */
    void close() {
        connected = false;
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (frame != null) {
                    frame.dispose();
                    frame = null;
                }
            }
        });
    }

    // --- rendering -----------------------------------------------------------

    private void renderTop() {
        if (stack.isEmpty()) {
            return;
        }
        final CarScreen screen = stack.get(stack.size() - 1);
        final CarTemplate template = screen.dispatchCreateTemplate();
        final boolean canGoBack = stack.size() > 1;
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
            if (content == null) {
                return;
            }
            content.removeAll();
            backButton.setVisible(canGoBack);
            titleLabel.setText(titleOf(template));
            if (template instanceof CarListTemplate) {
                renderList((CarListTemplate) template);
            } else if (template instanceof CarGridTemplate) {
                renderGrid((CarGridTemplate) template);
            } else if (template instanceof CarPaneTemplate) {
                renderPane((CarPaneTemplate) template);
            } else if (template instanceof CarMessageTemplate) {
                renderMessage((CarMessageTemplate) template);
            } else if (template instanceof CarNowPlayingTemplate) {
                renderNowPlaying((CarNowPlayingTemplate) template);
            } else if (template instanceof CarNavigationTemplate) {
                renderNavigation((CarNavigationTemplate) template);
            }
            content.revalidate();
            content.repaint();
            }
        });
    }

    private String titleOf(CarTemplate t) {
        return t != null && t.getTitle() != null ? t.getTitle() : "";
    }

    private void renderList(CarListTemplate t) {
        if (t.isLoading()) {
            addCentered("Loading…");
            return;
        }
        for (CarSection section : t.getSections()) {
            if (section.getHeader() != null) {
                content.add(sectionHeader(section.getHeader()));
            }
            for (CarRow row : section.getRows()) {
                content.add(rowComponent(row));
            }
        }
    }

    private void renderGrid(CarGridTemplate t) {
        if (t.isLoading()) {
            addCentered("Loading…");
            return;
        }
        JPanel grid = new JPanel(new GridLayout(0, 4, 12, 12));
        grid.setBackground(bg);
        grid.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        for (final CarGridItem item : t.getItems()) {
            JPanel cell = new JPanel(new BorderLayout());
            cell.setBackground(panel);
            cell.setBorder(BorderFactory.createEmptyBorder(16, 8, 16, 8));
            JLabel icon = new JLabel(iconFor(item.getImage(), 64), SwingConstants.CENTER);
            JLabel label = new JLabel(safe(item.getTitle()), SwingConstants.CENTER);
            label.setForeground(textPrimary);
            cell.add(icon, BorderLayout.CENTER);
            cell.add(label, BorderLayout.SOUTH);
            makeClickable(cell, item.getOnAction());
            grid.add(cell);
        }
        content.add(grid);
    }

    private void renderPane(CarPaneTemplate t) {
        if (t.isLoading()) {
            addCentered("Loading…");
            return;
        }
        for (CarRow row : t.getRows()) {
            content.add(infoRow(safe(row.getTitle()), row.getText()));
        }
        if (!t.getActions().isEmpty()) {
            content.add(actionBar(t.getActions()));
        }
    }

    private void renderMessage(CarMessageTemplate t) {
        JPanel box = verticalBox();
        if (t.getIcon() != null) {
            JLabel icon = new JLabel(iconFor(t.getIcon(), 72), SwingConstants.CENTER);
            icon.setAlignmentX(Component.CENTER_ALIGNMENT);
            box.add(icon);
        }
        JLabel msg = new JLabel("<html><div style='text-align:center;width:600px'>" + esc(safe(t.getMessage())) + "</div></html>",
                SwingConstants.CENTER);
        msg.setForeground(textPrimary);
        msg.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(msg);
        if (!t.getActions().isEmpty()) {
            box.add(actionBar(t.getActions()));
        }
        content.add(box);
    }

    private void renderNowPlaying(CarNowPlayingTemplate t) {
        JPanel box = verticalBox();
        JLabel head = new JLabel("Now Playing", SwingConstants.CENTER);
        head.setForeground(textPrimary);
        head.setFont(head.getFont().deriveFont(Font.BOLD, 22f));
        head.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel note = new JLabel("(metadata, artwork & transport are driven by the platform media session)",
                SwingConstants.CENTER);
        note.setForeground(textSecondary);
        note.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(head);
        box.add(note);
        if (!t.getActions().isEmpty()) {
            box.add(actionBar(t.getActions()));
        }
        content.add(box);
    }

    private void renderNavigation(CarNavigationTemplate t) {
        JPanel map = new JPanel(new BorderLayout());
        map.setBackground(new Color(0x2C, 0x2F, 0x36));
        map.setPreferredSize(new Dimension(900, 320));
        JLabel mapLabel = new JLabel("🗺 map surface", SwingConstants.CENTER);
        mapLabel.setForeground(textSecondary);
        map.add(mapLabel, BorderLayout.CENTER);
        content.add(map);
        if (t.isNavigating()) {
            content.add(infoRow(safe(t.getNextManeuver()),
                    join(t.getDistanceRemaining(), t.getTimeRemaining())));
        }
        if (t.getMapActions() != null && !t.getMapActions().getActions().isEmpty()) {
            content.add(actionBar(t.getMapActions().getActions()));
        }
    }

    // --- component builders --------------------------------------------------

    private JPanel rowComponent(final CarRow row) {
        JPanel p = new JPanel(new BorderLayout(12, 0));
        p.setBackground(panel);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(4, 16, 4, 16),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        if (row.getImage() != null) {
            p.add(new JLabel(iconFor(row.getImage(), 44)), BorderLayout.WEST);
        }
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel title = new JLabel(safe(row.getTitle()));
        title.setForeground(textPrimary);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        text.add(title);
        if (row.getText() != null) {
            JLabel sub = new JLabel(row.getText());
            sub.setForeground(textSecondary);
            text.add(sub);
        }
        p.add(text, BorderLayout.CENTER);
        if (row.isBrowsable()) {
            JLabel chevron = new JLabel("›");
            chevron.setForeground(textSecondary);
            chevron.setFont(chevron.getFont().deriveFont(24f));
            p.add(chevron, BorderLayout.EAST);
        }
        makeClickable(p, row.getOnAction());
        return p;
    }

    private JPanel infoRow(String label, String value) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(panel);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(4, 16, 4, 16),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        JLabel l = new JLabel(label);
        l.setForeground(textSecondary);
        JLabel v = new JLabel(value == null ? "" : value, SwingConstants.RIGHT);
        v.setForeground(textPrimary);
        p.add(l, BorderLayout.WEST);
        p.add(v, BorderLayout.EAST);
        return p;
    }

    private JPanel actionBar(List<CarAction> actions) {
        JPanel bar = new JPanel();
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        bar.setLayout(new BoxLayout(bar, BoxLayout.X_AXIS));
        bar.setAlignmentX(Component.CENTER_ALIGNMENT);
        for (final CarAction a : actions) {
            JButton b = new JButton(safe(a.getTitle()));
            b.setForeground(Color.WHITE);
            b.setBackground(accent);
            b.setFocusPainted(false);
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.addActionListener(new java.awt.event.ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    dispatch(a.getOnAction());
                }
            });
            bar.add(b);
            bar.add(javax.swing.Box.createHorizontalStrut(10));
        }
        return bar;
    }

    private JPanel sectionHeader(String text) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(bg);
        p.setBorder(BorderFactory.createEmptyBorder(14, 18, 4, 18));
        JLabel l = new JLabel(text.toUpperCase());
        l.setForeground(accent);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 12f));
        p.add(l, BorderLayout.WEST);
        return p;
    }

    private void addCentered(String text) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setForeground(textSecondary);
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        l.setBorder(BorderFactory.createEmptyBorder(40, 0, 0, 0));
        content.add(l);
    }

    private JPanel verticalBox() {
        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBorder(BorderFactory.createEmptyBorder(40, 20, 20, 20));
        return box;
    }

    private void makeClickable(JPanel p, final CarActionListener listener) {
        p.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        p.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                dispatch(listener);
            }
        });
    }

    /** Routes a head-unit selection to the app's listener on the Codename One EDT. */
    private void dispatch(final CarActionListener listener) {
        if (listener == null) {
            return;
        }
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                CarContext ctx = Car.getCurrentContext();
                if (ctx != null) {
                    listener.actionPerformed(ctx);
                }
            }
        });
    }

    private ImageIcon iconFor(com.codename1.ui.Image img, int size) {
        if (img == null) {
            return placeholderIcon(size);
        }
        try {
            Object n = img.getImage();
            if (n instanceof BufferedImage) {
                java.awt.Image scaled = ((BufferedImage) n).getScaledInstance(size, size, java.awt.Image.SCALE_SMOOTH);
                return new ImageIcon(scaled);
            }
        } catch (Throwable ignore) {
            // fall through to the placeholder
        }
        return placeholderIcon(size);
    }

    private ImageIcon placeholderIcon(int size) {
        BufferedImage bi = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = bi.createGraphics();
        g.setColor(new Color(0x3A3E46));
        g.fillRoundRect(0, 0, size, size, 12, 12);
        g.dispose();
        return new ImageIcon(bi);
    }

    private void styleHeaderButton(JButton b) {
        b.setForeground(accent);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setFont(b.getFont().deriveFont(Font.BOLD, 14f));
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String join(String a, String b) {
        if (a == null) {
            return b == null ? "" : b;
        }
        if (b == null) {
            return a;
        }
        return a + " · " + b;
    }
}
