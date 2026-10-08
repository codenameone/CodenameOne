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
package com.codename1.desktopcompat.java.awt;

import com.codename1.desktopcompat.java.awt.event.ContainerEvent;
import com.codename1.desktopcompat.java.awt.event.ContainerListener;
import com.codename1.desktopcompat.rt.ContainerPeer;
import com.codename1.desktopcompat.rt.G2D;
import com.codename1.desktopcompat.rt.Peer;
import java.util.ArrayList;

/// A component that holds other components and lays them out with a
/// layout manager.
///
/// The peers of the children are children of this container's peer, in
/// reverse order, so that the first child is painted on top as it is on the
/// desktop.
public class Container extends Component {

    private final ArrayList<Component> children = new ArrayList<Component>();
    private LayoutManager layoutMgr;
    private boolean cn1Validating;
    private FocusTraversalPolicy focusPolicy;
    private ArrayList<ContainerListener> containerListeners;

    public Container() {
    }

    @Override
    protected com.codename1.ui.Component cn1CreatePeer() {
        return new ContainerPeer(this);
    }

    @Override
    protected void cn1PeerCreated() {
        super.cn1PeerCreated();
        com.codename1.ui.Component p = cn1PeerOrNull();
        if (p instanceof ContainerPeer) {
            ContainerPeer cp = (ContainerPeer) p;
            for (int i = children.size() - 1; i >= 0; i--) {
                cp.addComponent(children.get(i).cn1Peer());
            }
        }
    }

    /// Paints the children: through the peers when `g` is the graphics
    /// this container's peer is being painted with, else one by one into
    /// `g`, the last child first.
    protected void cn1PaintChildren(Graphics g) {
        com.codename1.ui.Component p = cn1PeerOrNull();
        if (p instanceof Peer && g instanceof G2D && ((Peer) p).support().isPainting((G2D) g)) {
            ((Peer) p).support().paintChildren((G2D) g);
            return;
        }
        for (int i = children.size() - 1; i >= 0; i--) {
            Component c = children.get(i);
            if (c.isVisible()) {
                Graphics cg = g.create(c.getX(), c.getY(), c.getWidth(), c.getHeight());
                try {
                    c.paint(cg);
                } finally {
                    cg.dispose();
                }
            }
        }
    }

    public int getComponentCount() {
        return children.size();
    }

    public Component getComponent(int n) {
        if (n < 0 || n >= children.size()) {
            throw new ArrayIndexOutOfBoundsException("No such child: " + n);
        }
        return children.get(n);
    }

    public Component[] getComponents() {
        return children.toArray(new Component[children.size()]);
    }

    public Insets getInsets() {
        return new Insets(0, 0, 0, 0);
    }

    public Component add(Component comp) {
        addImpl(comp, null, -1);
        return comp;
    }

    public Component add(String name, Component comp) {
        addImpl(comp, name, -1);
        return comp;
    }

    public Component add(Component comp, int index) {
        addImpl(comp, null, index);
        return comp;
    }

    public void add(Component comp, Object constraints) {
        addImpl(comp, constraints, -1);
    }

    public void add(Component comp, Object constraints, int index) {
        addImpl(comp, constraints, index);
    }

    protected void addImpl(Component comp, Object constraints, int index) {
        if (index > children.size() || index < -1) {
            throw new IllegalArgumentException("illegal component position");
        }
        if (comp == this) {
            throw new IllegalArgumentException("adding container to itself");
        }
        if (comp instanceof Container) {
            for (Container c = this; c != null; c = c.getParent()) {
                if (c == comp) {
                    throw new IllegalArgumentException("adding container's parent to itself");
                }
            }
        }
        if (comp instanceof Window) {
            throw new IllegalArgumentException("adding a window to a container");
        }
        Container old = comp.getParent();
        if (old != null) {
            old.remove(comp);
            if (old == this && index > children.size()) {
                index = children.size();
            }
        }
        int at = index == -1 ? children.size() : index;
        children.add(at, comp);
        comp.cn1SetParent(this);
        com.codename1.ui.Component p = cn1PeerOrNull();
        if (p instanceof ContainerPeer) {
            ((ContainerPeer) p).addComponent(children.size() - 1 - at, comp.cn1Peer());
        }
        if (layoutMgr != null) {
            if (layoutMgr instanceof LayoutManager2) {
                ((LayoutManager2) layoutMgr).addLayoutComponent(comp, constraints);
            } else if (constraints instanceof String) {
                layoutMgr.addLayoutComponent((String) constraints, comp);
            }
        }
        invalidate();
        if (isDisplayable()) {
            comp.addNotify();
        }
        if (containerListeners != null && !containerListeners.isEmpty()) {
            dispatchEvent(new ContainerEvent(this, ContainerEvent.COMPONENT_ADDED, comp));
        }
    }

    public void remove(int index) {
        Component comp = getComponent(index);
        if (comp.isDisplayable()) {
            comp.removeNotify();
        }
        if (layoutMgr != null) {
            layoutMgr.removeLayoutComponent(comp);
        }
        children.remove(index);
        comp.cn1SetParent(null);
        com.codename1.ui.Component p = cn1PeerOrNull();
        com.codename1.ui.Component cp = comp.cn1PeerOrNull();
        if (p instanceof ContainerPeer && cp != null) {
            ((ContainerPeer) p).removeComponent(cp);
        }
        invalidate();
        if (containerListeners != null && !containerListeners.isEmpty()) {
            dispatchEvent(new ContainerEvent(this, ContainerEvent.COMPONENT_REMOVED, comp));
        }
    }

    public void remove(Component comp) {
        int i = children.indexOf(comp);
        if (i >= 0) {
            remove(i);
        }
    }

    public void removeAll() {
        while (!children.isEmpty()) {
            remove(children.size() - 1);
        }
    }

    public int getComponentZOrder(Component comp) {
        return children.indexOf(comp);
    }

    public void setComponentZOrder(Component comp, int index) {
        int i = children.indexOf(comp);
        if (i < 0) {
            addImpl(comp, null, index);
            return;
        }
        if (i == index) {
            return;
        }
        children.remove(i);
        children.add(index, comp);
        com.codename1.ui.Component p = cn1PeerOrNull();
        com.codename1.ui.Component cp = comp.cn1PeerOrNull();
        if (p instanceof ContainerPeer && cp != null) {
            ((ContainerPeer) p).removeComponent(cp);
            ((ContainerPeer) p).addComponent(children.size() - 1 - index, cp);
        }
        invalidate();
    }

    public LayoutManager getLayout() {
        return layoutMgr;
    }

    public void setLayout(LayoutManager mgr) {
        layoutMgr = mgr;
        invalidate();
    }

    @Override
    public void doLayout() {
        if (layoutMgr != null) {
            layoutMgr.layoutContainer(this);
        }
    }

    public boolean isValidateRoot() {
        return false;
    }

    @Override
    public void invalidate() {
        if (layoutMgr instanceof LayoutManager2) {
            ((LayoutManager2) layoutMgr).invalidateLayout(this);
        }
        super.invalidate();
    }

    @Override
    public void validate() {
        if (!cn1Valid() && !cn1Validating) {
            validateTree();
            repaint();
        }
    }

    /// Lays this container out and then its invalid children.
    ///
    /// A pass that is asked for while this container is in the middle of
    /// one does nothing. A layout may ask for something to be scrolled
    /// into view, and showing a rectangle validates the viewport first so
    /// that it scrolls within the view's new size: without this the
    /// viewport's layout would start the viewport's layout, without end.
    /// The pass under way reaches everything the second one would have.
    protected void validateTree() {
        if (cn1Validating) {
            return;
        }
        cn1Validating = true;
        try {
            if (!cn1Valid()) {
                doLayout();
                for (int i = 0; i < children.size(); i++) {
                    Component c = children.get(i);
                    if (c instanceof Container) {
                        if (!c.cn1Valid()) {
                            ((Container) c).validateTree();
                        }
                    } else {
                        c.validate();
                    }
                }
            }
            cn1SetValid(true);
        } finally {
            cn1Validating = false;
        }
    }

    @Override
    public void setFont(Font f) {
        super.setFont(f);
        invalidateTree();
    }

    private void invalidateTree() {
        for (int i = 0; i < children.size(); i++) {
            Component c = children.get(i);
            if (c instanceof Container) {
                ((Container) c).invalidateTree();
            } else {
                c.cn1SetValid(false);
            }
        }
        cn1SetValid(false);
    }

    @Override
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet() || layoutMgr == null) {
            return super.getPreferredSize();
        }
        return layoutMgr.preferredLayoutSize(this);
    }

    @Override
    public Dimension getMinimumSize() {
        if (isMinimumSizeSet() || layoutMgr == null) {
            return super.getMinimumSize();
        }
        return layoutMgr.minimumLayoutSize(this);
    }

    @Override
    public Dimension getMaximumSize() {
        if (!isMaximumSizeSet() && layoutMgr instanceof LayoutManager2) {
            return ((LayoutManager2) layoutMgr).maximumLayoutSize(this);
        }
        return super.getMaximumSize();
    }

    @Override
    public float getAlignmentX() {
        if (layoutMgr instanceof LayoutManager2) {
            return ((LayoutManager2) layoutMgr).getLayoutAlignmentX(this);
        }
        return super.getAlignmentX();
    }

    @Override
    public float getAlignmentY() {
        if (layoutMgr instanceof LayoutManager2) {
            return ((LayoutManager2) layoutMgr).getLayoutAlignmentY(this);
        }
        return super.getAlignmentY();
    }

    @Override
    public void paint(Graphics g) {
        cn1PaintChildren(g);
    }

    @Override
    public void update(Graphics g) {
        paint(g);
    }

    public void paintComponents(Graphics g) {
        cn1PaintChildren(g);
    }

    @Override
    public Component getComponentAt(int x, int y) {
        if (!contains(x, y)) {
            return null;
        }
        for (int i = 0; i < children.size(); i++) {
            Component c = children.get(i);
            if (c.contains(x - c.getX(), y - c.getY())) {
                return c;
            }
        }
        return this;
    }

    public Component findComponentAt(int x, int y) {
        if (!contains(x, y) || !isVisible()) {
            return null;
        }
        for (int i = 0; i < children.size(); i++) {
            Component c = children.get(i);
            if (!c.isVisible()) {
                continue;
            }
            int cx = x - c.getX();
            int cy = y - c.getY();
            if (c instanceof Container) {
                Component found = ((Container) c).findComponentAt(cx, cy);
                if (found != null) {
                    return found;
                }
            } else if (c.contains(cx, cy)) {
                return c;
            }
        }
        return this;
    }

    public Component findComponentAt(Point p) {
        return findComponentAt(p.x, p.y);
    }

    @Override
    public void setCursor(Cursor cursor) {
        super.setCursor(cursor);
        cursorChanged();
    }

    private void cursorChanged() {
        for (int i = 0; i < children.size(); i++) {
            Component c = children.get(i);
            if (!c.isCursorSet()) {
                c.cn1ApplyCursor();
                if (c instanceof Container) {
                    ((Container) c).cursorChanged();
                }
            }
        }
    }

    /// The order the tab key walks this container's components in, or
    /// `null` when none was set and the keyboard focus manager's default
    /// applies. Only the policy of a window is consulted.
    public FocusTraversalPolicy getFocusTraversalPolicy() {
        return focusPolicy;
    }

    public void setFocusTraversalPolicy(FocusTraversalPolicy policy) {
        FocusTraversalPolicy old = focusPolicy;
        focusPolicy = policy;
        firePropertyChange("focusTraversalPolicy", old, policy);
    }

    public boolean isFocusTraversalPolicySet() {
        return focusPolicy != null;
    }

    public boolean isAncestorOf(Component c) {
        for (Container p = c == null ? null : c.getParent(); p != null; p = p.getParent()) {
            if (p == this) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        for (int i = 0; i < children.size(); i++) {
            children.get(i).addNotify();
        }
    }

    @Override
    public void removeNotify() {
        for (int i = children.size() - 1; i >= 0 && i < children.size(); i--) {
            children.get(i).removeNotify();
        }
        super.removeNotify();
    }

    public void addContainerListener(ContainerListener l) {
        if (l != null) {
            if (containerListeners == null) {
                containerListeners = new ArrayList<ContainerListener>();
            }
            containerListeners.add(l);
        }
    }

    public void removeContainerListener(ContainerListener l) {
        if (containerListeners != null) {
            containerListeners.remove(l);
        }
    }

    public ContainerListener[] getContainerListeners() {
        return containerListeners == null ? new ContainerListener[0]
                : containerListeners.toArray(new ContainerListener[containerListeners.size()]);
    }

    @Override
    protected void processEvent(AWTEvent e) {
        if (e instanceof ContainerEvent) {
            processContainerEvent((ContainerEvent) e);
        } else {
            super.processEvent(e);
        }
    }

    protected void processContainerEvent(ContainerEvent e) {
        ContainerListener[] ls = getContainerListeners();
        for (int i = 0; i < ls.length; i++) {
            if (e.getID() == ContainerEvent.COMPONENT_ADDED) {
                ls[i].componentAdded(e);
            } else {
                ls[i].componentRemoved(e);
            }
        }
    }

    @Override
    protected String paramString() {
        String s = super.paramString();
        if (layoutMgr != null) {
            s += ",layout=" + layoutMgr.getClass().getName();
        }
        return s;
    }
}
