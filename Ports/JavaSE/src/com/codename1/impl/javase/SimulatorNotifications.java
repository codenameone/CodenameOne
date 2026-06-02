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
package com.codename1.impl.javase;

import com.codename1.notifications.LocalNotification;
import com.codename1.notifications.NotificationChannelBuilder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;

/// Renders local notifications in the JavaSE simulator using a stack of small Swing
/// windows shown over the top-right of the simulator. Each window shows the channel name,
/// title, body, optional image and progress bar, and a button per action (with an inline
/// text field for quick-reply actions). Tapping the body or an action routes back into the
/// running app through `JavaSEPort#dispatchLocalNotification`.
class SimulatorNotifications {

    private static final Map<String, JWindow> ACTIVE = new LinkedHashMap<String, JWindow>();
    private static JLabel foregroundServiceLabel;

    private SimulatorNotifications() {
    }

    static void show(final JavaSEPort port, final LocalNotification notif) {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        if (port.isMinimized()) {
            // when minimized the simulator window is hidden; fall back to the system tray
            port.showTrayNotification(notif);
            return;
        }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                buildAndShow(port, notif);
            }
        });
    }

    private static void buildAndShow(final JavaSEPort port, final LocalNotification notif) {
        dismissInternal(notif.getId());

        final JWindow w = new JWindow();
        JPanel root = new JPanel(new BorderLayout(8, 4));
        root.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 60, 60)),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        root.setBackground(new Color(250, 250, 250));

        // channel line
        NotificationChannelBuilder ch = port.getSimulatorChannel(notif.getChannelId());
        String chName = ch != null ? ch.getName() : notif.getChannelId();
        if (chName != null) {
            JLabel chLabel = new JLabel(chName);
            chLabel.setForeground(new Color(120, 120, 120));
            chLabel.setFont(chLabel.getFont().deriveFont(Font.PLAIN, 10f));
            root.add(chLabel, BorderLayout.NORTH);
        }

        // image thumbnail on the left
        String img = notif.getAlertImage();
        if (img != null && img.length() > 0) {
            try {
                File f = resolveImage(img);
                if (f != null && f.exists()) {
                    Image scaled = new ImageIcon(f.getAbsolutePath()).getImage()
                            .getScaledInstance(48, 48, Image.SCALE_SMOOTH);
                    root.add(new JLabel(new ImageIcon(scaled)), BorderLayout.WEST);
                }
            } catch (Exception ignore) {
            }
        }

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        JLabel title = new JLabel(notif.getAlertTitle() == null ? "" : notif.getAlertTitle());
        title.setFont(title.getFont().deriveFont(Font.BOLD, 13f));
        center.add(title);

        String bodyText = notif.getAlertBody();
        if (notif.getMessagingStyle() != null && !notif.getMessagingStyle().getMessages().isEmpty()) {
            StringBuilder sb = new StringBuilder("<html>");
            for (LocalNotification.MessagingStyle.Message m : notif.getMessagingStyle().getMessages()) {
                String sender = m.getSenderName() == null ? notif.getMessagingStyle().getSelfDisplayName() : m.getSenderName();
                sb.append("<b>").append(escape(sender)).append(":</b> ").append(escape(m.getText())).append("<br>");
            }
            sb.append("</html>");
            bodyText = sb.toString();
        }
        if (bodyText != null) {
            JLabel body = new JLabel(bodyText);
            body.setFont(body.getFont().deriveFont(Font.PLAIN, 12f));
            center.add(body);
        }

        if (notif.getProgressMax() > 0 || notif.isProgressIndeterminate()) {
            JProgressBar bar = new JProgressBar(0, Math.max(1, notif.getProgressMax()));
            if (notif.isProgressIndeterminate()) {
                bar.setIndeterminate(true);
            } else {
                bar.setValue(notif.getProgress());
            }
            bar.setPreferredSize(new Dimension(220, 12));
            center.add(Box.createVerticalStrut(4));
            center.add(bar);
        }

        // action buttons
        if (!notif.getActions().isEmpty()) {
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
            actions.setOpaque(false);
            for (final LocalNotification.Action a : notif.getActions()) {
                final JButton b = new JButton(a.getTitle());
                b.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if (a.isTextInput()) {
                            promptForReply(port, notif.getId(), a, w);
                        } else {
                            port.dispatchLocalNotification(notif.getId(), a.getId(), a.getTitle(), null);
                            dismissInternal(notif.getId());
                        }
                    }
                });
                actions.add(b);
            }
            center.add(Box.createVerticalStrut(4));
            center.add(actions);
        }

        root.add(center, BorderLayout.CENTER);

        // tapping the body launches the notification
        JButton tap = new JButton("Open");
        tap.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                port.dispatchLocalNotification(notif.getId(), null, null, null);
                dismissInternal(notif.getId());
            }
        });
        JButton close = new JButton("x");
        close.setMargin(new java.awt.Insets(0, 4, 0, 4));
        close.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dismissInternal(notif.getId());
            }
        });
        JPanel east = new JPanel();
        east.setOpaque(false);
        east.setLayout(new BoxLayout(east, BoxLayout.Y_AXIS));
        east.add(close);
        east.add(Box.createVerticalGlue());
        east.add(tap);
        root.add(east, BorderLayout.EAST);

        w.setContentPane(root);
        w.pack();
        if (w.getWidth() > 360) {
            w.setSize(360, w.getHeight());
        }
        w.setAlwaysOnTop(true);
        positionWindow(w);
        w.setVisible(true);
        ACTIVE.put(notif.getId(), w);
    }

    private static void promptForReply(final JavaSEPort port, final String id, final LocalNotification.Action a, JWindow parent) {
        final JWindow reply = new JWindow();
        JPanel p = new JPanel(new BorderLayout(4, 4));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 60, 60)),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        final JTextField field = new JTextField(20);
        if (a.getTextInputPlaceholder() != null) {
            field.setToolTipText(a.getTextInputPlaceholder());
        }
        JButton send = new JButton(a.getTextInputButtonText() == null ? "Send" : a.getTextInputButtonText());
        ActionListener submit = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                port.dispatchLocalNotification(id, a.getId(), a.getTitle(), field.getText());
                reply.dispose();
                dismissInternal(id);
            }
        };
        send.addActionListener(submit);
        field.addActionListener(submit);
        p.add(new JLabel(a.getTitle()), BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        p.add(send, BorderLayout.EAST);
        reply.setContentPane(p);
        reply.pack();
        reply.setLocationRelativeTo(parent);
        reply.setAlwaysOnTop(true);
        reply.setVisible(true);
        field.requestFocusInWindow();
    }

    static void dismiss(final String id) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                dismissInternal(id);
            }
        });
    }

    private static void dismissInternal(String id) {
        JWindow w = ACTIVE.remove(id);
        if (w != null) {
            w.dispose();
        }
        relayout();
    }

    private static void positionWindow(JWindow w) {
        relayout();
        if (!w.isVisible()) {
            Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
            int y = screen.y + 20 + ACTIVE.size() * (w.getHeight() + 8);
            w.setLocation(screen.x + screen.width - w.getWidth() - 20, y);
        }
    }

    private static void relayout() {
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        int y = screen.y + 20;
        for (JWindow w : ACTIVE.values()) {
            w.setLocation(screen.x + screen.width - w.getWidth() - 20, y);
            y += w.getHeight() + 8;
        }
    }

    private static File resolveImage(String path) {
        File f = new File(path);
        if (f.exists()) {
            return f;
        }
        // try relative to the src/native roots commonly used in projects
        String[] roots = {"src/main/resources", "src", "native/javase", "."};
        for (String r : roots) {
            File candidate = new File(r, path);
            if (candidate.exists()) {
                return candidate;
            }
        }
        return null;
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // ---- foreground service status indicator (wired to the Simulate menu) ----

    static void setForegroundServiceLabel(JLabel label) {
        foregroundServiceLabel = label;
    }

    static void setForegroundServiceStatus(final String status) {
        final JLabel l = foregroundServiceLabel;
        if (l == null) {
            return;
        }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                l.setText(status == null ? "Foreground service: stopped" : "Foreground service: " + status);
            }
        });
    }
}
