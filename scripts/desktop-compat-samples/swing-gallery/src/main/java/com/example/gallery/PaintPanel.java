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
package com.example.gallery;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * Custom painting with Graphics2D, and free-hand drawing with the mouse.
 */
public class PaintPanel extends JPanel {

    public PaintPanel() {
        super(new BorderLayout());
        Canvas canvas = new Canvas();
        JButton clear = new JButton("Clear drawing");
        clear.addActionListener(e -> canvas.clear());
        JPanel bottom = new JPanel();
        bottom.add(clear);
        add(canvas, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    private static class Canvas extends JComponent {

        private final List<List<Point>> strokes = new ArrayList<>();
        private final BufferedImage checker = createChecker();
        private List<Point> current;

        Canvas() {
            setPreferredSize(new Dimension(600, 400));
            MouseAdapter mouse = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    current = new ArrayList<>();
                    current.add(e.getPoint());
                    strokes.add(current);
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (current != null) {
                        current.add(e.getPoint());
                        repaint();
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    current = null;
                }
            };
            addMouseListener(mouse);
            addMouseMotionListener(mouse);
        }

        void clear() {
            strokes.clear();
            repaint();
        }

        /** An image painted once, off screen, and drawn on every repaint. */
        private static BufferedImage createChecker() {
            BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 8; x++) {
                    g.setColor((x + y) % 2 == 0 ? new Color(0x355c7d) : new Color(0xf8b195));
                    g.fillRect(x * 8, y * 8, 8, 8);
                }
            }
            g.dispose();
            return image;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();

            g.setPaint(new GradientPaint(0, 0, new Color(0xfdfbfb), 0, h, new Color(0xdfe9f3)));
            g.fillRect(0, 0, w, h);

            g.setColor(new Color(0x6c5b7b));
            g.fill(new Ellipse2D.Double(20, 20, 90, 60));
            g.setColor(new Color(0xc06c84));
            g.fill(new RoundRectangle2D.Double(130, 20, 90, 60, 18, 18));

            Path2D triangle = new Path2D.Double();
            triangle.moveTo(285, 20);
            triangle.lineTo(330, 80);
            triangle.lineTo(240, 80);
            triangle.closePath();
            g.setColor(new Color(0xf67280));
            g.fill(triangle);

            g.setColor(Color.DARK_GRAY);
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f,
                    new float[] {8f, 6f}, 0f));
            g.draw(new Line2D.Double(20, 100, 330, 100));
            g.setStroke(new BasicStroke(3f));
            g.draw(new Ellipse2D.Double(20, 20, 90, 60));

            AffineTransform saved = g.getTransform();
            g.rotate(Math.toRadians(-20), 420, 60);
            g.setColor(new Color(0x355c7d));
            g.fillRect(380, 40, 80, 40);
            g.setTransform(saved);

            g.drawImage(checker, 20, 120, null);
            g.drawImage(checker, 100, 120, 32, 32, null);

            String text = "Graphics2D";
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
            FontMetrics metrics = g.getFontMetrics();
            int textWidth = metrics.stringWidth(text);
            int baseline = 150 + metrics.getAscent();
            g.setColor(new Color(0x355c7d));
            g.drawString(text, w - textWidth - 20, baseline);
            g.drawLine(w - textWidth - 20, baseline + 2, w - 20, baseline + 2);

            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (List<Point> stroke : strokes) {
                for (int i = 1; i < stroke.size(); i++) {
                    Point a = stroke.get(i - 1);
                    Point b = stroke.get(i);
                    g.drawLine(a.x, a.y, b.x, b.y);
                }
            }
            g.dispose();
        }
    }
}
