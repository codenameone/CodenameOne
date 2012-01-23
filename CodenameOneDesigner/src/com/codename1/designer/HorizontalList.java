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

package com.codename1.designer;

import com.codename1.ui.resource.util.WrappingLayout;
import com.codename1.ui.util.EditableResources;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.prefs.Preferences;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.JToolTip;
import javax.swing.SwingConstants;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import org.jdesktop.swingx.VerticalLayout;

/**
 * Simple abstraction for elements within the resource editor as a horizontal list
 * with the appropriate preview
 *
 * @author Shai Almog
 */
public class HorizontalList extends JPanel {
    private static boolean blockRefeshWhileLoading;
    private static final ImageIcon X_ICON;
    private static final ImageIcon TICK_ICON;
    private static final ImageIcon DELETE_ICON;
    
    static {
        X_ICON = new ImageIcon(ResourceEditorApp.class.getResource("/x.png"));
        TICK_ICON = new ImageIcon(ResourceEditorApp.class.getResource("/tick.png"));
        DELETE_ICON = new ImageIcon(ResourceEditorApp.class.getResource("/delete.png"));
    };

    /**
     * @return the blockRefeshWhileLoading
     */
    public static boolean isBlockRefeshWhileLoading() {
        return blockRefeshWhileLoading;
    }

    /**
     * @param aBlockRefeshWhileLoading the blockRefeshWhileLoading to set
     */
    public static void setBlockRefeshWhileLoading(boolean aBlockRefeshWhileLoading) {
        blockRefeshWhileLoading = aBlockRefeshWhileLoading;
    }
    private EditableResources res;
    private static ButtonGroup group = new ButtonGroup();
    private ResourceEditorView view;
    private static int iconWidth;
    private static int iconHeight;
    
    static {
        iconWidth = Preferences.userNodeForPackage(HorizontalList.class).getInt("previewIconWidth", 24);
        iconHeight = Preferences.userNodeForPackage(HorizontalList.class).getInt("previewIconHeight", 24);
    }
    
    private ActionListener listener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            DeletableToggle del = (DeletableToggle)e.getSource();
            String text = del.getText();
            // special case for font
            if(text.equals("")) {
                text = ((JToggleButton)e.getSource()).getToolTipText();
            }
            if(del.doubleCharge) {
                if(res.isOverrideMode()) {
                    if(res.isOverridenResource(text)) {
                        view.genericRemoveElement(text);
                    } else {
                        res.overrideResource(text);
                        view.setSelectedResource(view.getSelectedResource());
                    }
                } else {
                    view.genericRemoveElement(text);
                }
            } else {
                view.setSelectedResource(text);
            }
        }
    };

    public HorizontalList(EditableResources res, ResourceEditorView view) {
        this(res, view, -1);
    }
    
    public HorizontalList(EditableResources res, ResourceEditorView view, int maxButtonWidth) {
        this.res = res;
        this.view = view;
        setOpaque(false);
        //setLayout(new VerticalLayout());
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        //setLayout(new WrappingLayout(maxButtonWidth));
        res.addTreeModelListener(new TreeModelListener() {
            public void treeNodesChanged(TreeModelEvent e) {
                refresh();
            }

            public void treeNodesInserted(TreeModelEvent e) {
                refresh();
            }

            public void treeNodesRemoved(TreeModelEvent e) {
                refresh();
            }

            public void treeStructureChanged(TreeModelEvent e) {
                refresh();
            }
        });
    }
    
    public void refresh() {
        if(blockRefeshWhileLoading) {
            return;
        }
        for(java.awt.Component cmp : getComponents()) {
            remove(cmp);
            group.remove((JToggleButton)cmp);
        }
        String[] entries = getEntries();
        Arrays.sort(entries, String.CASE_INSENSITIVE_ORDER);
        initLayout(entries.length);
        for(String current : entries) {
            JToggleButton button = createButton(current);
            add(button);
            String selection = view.getSelectedResource();
            if(selection != null && selection.equals(current)) {
                button.setSelected(true);
            }
            button.addActionListener(listener);
            group.add(button);
        }
        revalidate();
        repaint();
    }

    protected void initLayout(int count) {
    }
    
    protected EditableResources getRes() {
        return res;
    }

    class DeletableToggle extends JToggleButton implements MouseMotionListener, MouseListener {
        private boolean charged = false;
        private boolean doubleCharge = false;
        public DeletableToggle(String label, Icon img) {
            super(label, img);
            addMouseMotionListener(this);
            addMouseListener(this);
            if(res.isOverrideMode()) {
                setToolTipText("<html><body>Tick the options to determine if the resource<br>is overriden for this platform");
            }
        }
        
        public java.awt.Dimension getPreferredSize() {
            java.awt.Dimension d = super.getPreferredSize();
            return new java.awt.Dimension(180, d.height);
        }

        public java.awt.Dimension getMinimumSize() {
            return getPreferredSize();
        }

        public java.awt.Dimension getMaximumSize() {
            return getPreferredSize();
        }

        public void paintComponent(java.awt.Graphics g) {
            if(res.isOverrideMode()) {
                if(res.isOverridenResource(getText())) {
                    super.paintComponent(g);
                    if(charged) {
                        DELETE_ICON.paintIcon(this, g, getWidth() - X_ICON.getIconWidth(), 0);
                    } else {
                        Graphics2D g2d = (Graphics2D)g.create();
                        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f));
                        DELETE_ICON.paintIcon(this, g2d, getWidth() - X_ICON.getIconWidth(), 0);
                    }                    
                } else {
                    Graphics2D g2d = (Graphics2D)g.create();
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f));
                    super.paintComponent(g2d);
                    if(charged) {
                        TICK_ICON.paintIcon(this, g, getWidth() - X_ICON.getIconWidth(), 0);
                    } else {
                        TICK_ICON.paintIcon(this, g2d, getWidth() - X_ICON.getIconWidth(), 0);
                    }                    
                }
            } else {
                super.paintComponent(g);
                if(charged) {
                    X_ICON.paintIcon(this, g, getWidth() - X_ICON.getIconWidth(), 0);
                } else {
                    Graphics2D g2d = (Graphics2D)g.create();
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f));
                    X_ICON.paintIcon(this, g2d, getWidth() - X_ICON.getIconWidth(), 0);
                }
            }
        }

        public void mouseDragged(MouseEvent me) {
        }

        public void mouseMoved(MouseEvent me) {
            int x = me.getX();
            int y = me.getY();
            boolean newCharged = x > getWidth() - X_ICON.getIconWidth() && y <= X_ICON.getIconHeight();
            if(charged != newCharged) {
                charged = newCharged;
                repaint();
            }
        }

        public void mouseClicked(MouseEvent me) {
        }

        public void mousePressed(MouseEvent me) {
            doubleCharge = false;
            if(charged) {
                int x = me.getX();
                int y = me.getY();
                doubleCharge = x > getWidth() - X_ICON.getIconWidth() && y <= X_ICON.getIconHeight();
            }
        }

        public void mouseReleased(MouseEvent me) {
            if(doubleCharge) {
                int x = me.getX();
                int y = me.getY();
                doubleCharge = x > getWidth() - X_ICON.getIconWidth() && y <= X_ICON.getIconHeight();
            }
        }

        public void mouseEntered(MouseEvent me) {
        }

        public void mouseExited(MouseEvent me) {
            charged = false;
            repaint();
        }
    }
    
    protected JToggleButton createButton(String label) {
        JToggleButton button = new DeletableToggle(label, getIconImage(label));
        button.setToolTipText(label);
        button.setRolloverEnabled(true);
        //button.setI
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setBorderPainted(ResourceEditorApp.IS_MAC);
        return button;
    }
    
    public Icon getIconImage(final String current) {
        return new Icon() {
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Hashtable h = res.getTheme(current);
                if(h != null) {
                    com.codename1.ui.Image bgImage = (com.codename1.ui.Image)h.get("Form.bgImage");
                    if(bgImage != null) {
                        int[] rgb = bgImage.scaled(getIconWidth(), getIconHeight()).getRGB();
                        BufferedImage i = new BufferedImage(getIconWidth(), getIconHeight(), BufferedImage.TYPE_INT_ARGB);
                        i.setRGB(0, 0, getIconWidth(), getIconHeight(), rgb, 0, getIconWidth());
                        g.drawImage(i, x, y, null);
                    } else {
                        final String bgColor = (String)res.getTheme(current).get("bgColor");
                        if(bgColor != null) {
                            Color col = new Color(Integer.decode("0x" + bgColor));
                            g.setColor(col);
                            g.fillRect(x, y, getIconWidth(), getIconHeight());
                        }
                    }
                }
            }

            public int getIconWidth() {
                return getSettingsIconWidth();
            }

            public int getIconHeight() {
                return getSettingsIconHeight();
            }
        };
    }
    
    public String[] getEntries() {
        return res.getThemeResourceNames();
    }
    
    public static int getSettingsIconWidth() {
        return iconWidth;
    }

    public static int getSettingsIconHeight() {
        return iconHeight;
    }

    public static void setSettingsIconWidth(int v) {
        iconWidth = v;
        Preferences.userNodeForPackage(HorizontalList.class).putInt("previewIconWidth", iconWidth);
    }

    public static void setSettingsIconHeight(int v) {
        iconHeight = v;
        Preferences.userNodeForPackage(HorizontalList.class).putInt("previewIconHeight", iconHeight);
    }
}
