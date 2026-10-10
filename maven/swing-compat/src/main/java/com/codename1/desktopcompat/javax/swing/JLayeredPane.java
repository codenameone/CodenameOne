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
package com.codename1.desktopcompat.javax.swing;

import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Container;
import com.codename1.desktopcompat.javax.accessibility.Accessible;
import java.util.ArrayList;
import java.util.HashMap;

/// A container whose children overlap in layers: a child in a higher
/// layer is painted over, and takes the pointer before, a child in a
/// lower one, and within a layer the child at the lower position is on
/// top. It has no layout manager; children keep the bounds they are
/// given.
public class JLayeredPane extends JComponent implements Accessible {

    public static final Integer DEFAULT_LAYER = Integer.valueOf(0);
    public static final Integer PALETTE_LAYER = Integer.valueOf(100);
    public static final Integer MODAL_LAYER = Integer.valueOf(200);
    public static final Integer POPUP_LAYER = Integer.valueOf(300);
    public static final Integer DRAG_LAYER = Integer.valueOf(400);
    public static final Integer FRAME_CONTENT_LAYER = Integer.valueOf(-30000);
    public static final String LAYER_PROPERTY = "layeredContainerLayer";

    private final HashMap<Component, Integer> layers = new HashMap<Component, Integer>();

    public JLayeredPane() {
        setLayout(null);
    }

    @Override
    protected void addImpl(Component comp, Object constraints, int index) {
        int layer;
        if (constraints instanceof Integer) {
            layer = ((Integer) constraints).intValue();
            setLayer(comp, layer);
        } else {
            layer = getLayer(comp);
        }
        super.addImpl(comp, constraints, insertIndexForLayer(comp, layer, index));
        comp.validate();
        comp.repaint();
    }

    @Override
    public void remove(int index) {
        Component c = getComponent(index);
        super.remove(index);
        if (!(c instanceof JComponent)) {
            layers.remove(c);
        }
    }

    @Override
    public void removeAll() {
        super.removeAll();
        layers.clear();
    }

    public boolean isOptimizedDrawingEnabled() {
        return false;
    }

    /// Records the layer of a component that is not in a layered pane
    /// yet.
    public static void putLayer(JComponent c, int layer) {
        c.putClientProperty(LAYER_PROPERTY, Integer.valueOf(layer));
    }

    public static int getLayer(JComponent c) {
        Object i = c.getClientProperty(LAYER_PROPERTY);
        return i instanceof Integer ? ((Integer) i).intValue() : DEFAULT_LAYER.intValue();
    }

    public static JLayeredPane getLayeredPaneAbove(Component c) {
        if (c == null) {
            return null;
        }
        for (Container p = c.getParent(); p != null; p = p.getParent()) {
            if (p instanceof JLayeredPane) {
                return (JLayeredPane) p;
            }
        }
        return null;
    }

    public void setLayer(Component c, int layer) {
        setLayer(c, layer, -1);
    }

    public void setLayer(Component c, int layer, int position) {
        if (c.getParent() == this && layer == getLayer(c) && position == getPosition(c)) {
            return;
        }
        if (c instanceof JComponent) {
            ((JComponent) c).putClientProperty(LAYER_PROPERTY, Integer.valueOf(layer));
        } else {
            layers.put(c, Integer.valueOf(layer));
        }
        if (c.getParent() != this) {
            return;
        }
        setComponentZOrder(c, insertIndexForLayer(c, layer, position));
        repaint();
    }

    public int getLayer(Component c) {
        Object i = c instanceof JComponent ? ((JComponent) c).getClientProperty(LAYER_PROPERTY) : layers.get(c);
        return i instanceof Integer ? ((Integer) i).intValue() : DEFAULT_LAYER.intValue();
    }

    public int getIndexOf(Component c) {
        return getComponentZOrder(c);
    }

    public void moveToFront(Component c) {
        setPosition(c, 0);
    }

    public void moveToBack(Component c) {
        setPosition(c, -1);
    }

    public void setPosition(Component c, int position) {
        setLayer(c, getLayer(c), position);
    }

    /// The position of the component within its layer, 0 being on top;
    /// -1 when it is not a child.
    public int getPosition(Component c) {
        int index = getIndexOf(c);
        if (index < 0) {
            return -1;
        }
        int layer = getLayer(c);
        int pos = 0;
        for (int i = index - 1; i >= 0; i--) {
            if (getLayer(getComponent(i)) == layer) {
                pos++;
            } else {
                break;
            }
        }
        return pos;
    }

    public int highestLayer() {
        return getComponentCount() > 0 ? getLayer(getComponent(0)) : 0;
    }

    public int lowestLayer() {
        int n = getComponentCount();
        return n > 0 ? getLayer(getComponent(n - 1)) : 0;
    }

    public int getComponentCountInLayer(int layer) {
        int count = 0;
        for (int i = 0; i < getComponentCount(); i++) {
            if (getLayer(getComponent(i)) == layer) {
                count++;
            }
        }
        return count;
    }

    public Component[] getComponentsInLayer(int layer) {
        ArrayList<Component> l = new ArrayList<Component>();
        for (int i = 0; i < getComponentCount(); i++) {
            Component c = getComponent(i);
            if (getLayer(c) == layer) {
                l.add(c);
            }
        }
        return l.toArray(new Component[l.size()]);
    }

    protected int insertIndexForLayer(int layer, int position) {
        return insertIndexForLayer(null, layer, position);
    }

    /// The child index that puts a component at `position` of `layer`,
    /// counting the children without `comp` itself.
    private int insertIndexForLayer(Component comp, int layer, int position) {
        ArrayList<Component> list = new ArrayList<Component>();
        for (int i = 0; i < getComponentCount(); i++) {
            Component c = getComponent(i);
            if (c != comp) {
                list.add(c);
            }
        }
        int n = list.size();
        int start = -1;
        int end = -1;
        for (int i = 0; i < n; i++) {
            int cur = getLayer(list.get(i));
            if (start == -1 && cur == layer) {
                start = i;
            }
            if (cur < layer) {
                if (i == 0) {
                    start = 0;
                }
                end = i;
                break;
            }
        }
        if (start == -1 && end == -1) {
            return n;
        }
        if (end == -1) {
            end = n;
        }
        if (start == -1) {
            start = end;
        }
        if (position > -1 && start + position <= end) {
            return start + position;
        }
        return end;
    }

    @Override
    protected String paramString() {
        return super.paramString() + ",layers";
    }
}
