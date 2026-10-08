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

import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Font;
import com.codename1.desktopcompat.java.awt.FontMetrics;
import com.codename1.desktopcompat.java.awt.Insets;
import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.javax.swing.event.ChangeEvent;
import com.codename1.desktopcompat.javax.swing.event.ChangeListener;
import com.codename1.desktopcompat.rt.Fonts;
import com.codename1.desktopcompat.rt.Icons;
import com.codename1.desktopcompat.rt.MiniHtml;
import com.codename1.desktopcompat.rt.TabbedPanePeer;
import com.codename1.desktopcompat.rt.Units;
import java.util.ArrayList;

/// Pages behind a row of tabs, one page showing at a time.
///
/// The tabs are Codename One tab buttons, so they look and respond to
/// touch as the platform's tabs do; the pages are ordinary children of
/// this component, all given the area beside the tabs, with only the
/// selected one visible. Selection goes through the model and fires one
/// change event per change, whether the application or the user made it.
///
/// Not supported: the tab layout policy is recorded and the tabs always
/// scroll when they do not fit; a tab component is not shown in the tab,
/// though a `JLabel` given as one lends the tab its text and icon; tool
/// tips, mnemonics and the per tab colors and disabled icons are recorded
/// only; HTML titles are shown without their tags.
public class JTabbedPane extends JComponent implements SwingConstants {

    public static final int WRAP_TAB_LAYOUT = 0;
    public static final int SCROLL_TAB_LAYOUT = 1;

    protected int tabPlacement = TOP;
    protected SingleSelectionModel model;
    protected ChangeEvent changeEvent;

    private final ArrayList<Page> pages = new ArrayList<Page>();
    private final ModelHandler handler = new ModelHandler();
    private int tabLayoutPolicy;
    private Component visComp;

    /// What is known about one tab.
    private static final class Page {
        String title;
        Icon icon;
        Icon disabledIcon;
        Component component;
        Component tabComponent;
        String tip;
        boolean enabled = true;
        Color background;
        Color foreground;
        int mnemonic = -1;
        int mnemonicIndex = -1;
        Icon nativeIconFor;
        com.codename1.ui.Image nativeIcon;
    }

    public JTabbedPane() {
        this(TOP, WRAP_TAB_LAYOUT);
    }

    public JTabbedPane(int tabPlacement) {
        this(tabPlacement, WRAP_TAB_LAYOUT);
    }

    public JTabbedPane(int tabPlacement, int tabLayoutPolicy) {
        setTabPlacement(tabPlacement);
        setTabLayoutPolicy(tabLayoutPolicy);
        setModel(new DefaultSingleSelectionModel());
    }

    // ------------------------------------------------------------ peer

    @Override
    protected com.codename1.ui.Component cn1CreatePeer() {
        return new TabbedPanePeer(this, new TabbedPanePeer.Listener() {
            @Override
            public void tabChosen(int index) {
                if (index >= 0 && index < pages.size() && pages.get(index).enabled) {
                    setSelectedIndex(index);
                }
            }
        });
    }

    @Override
    protected void cn1PeerCreated() {
        super.cn1PeerCreated();
        com.codename1.ui.Component p = cn1PeerOrNull();
        if (p instanceof TabbedPanePeer) {
            ((TabbedPanePeer) p).attachStrip();
        }
        syncTabs();
    }

    private TabbedPanePeer peer() {
        com.codename1.ui.Component p = cn1PeerOrNull();
        return p instanceof TabbedPanePeer ? (TabbedPanePeer) p : null;
    }

    private String shownTitle(Page page) {
        String t = page.title;
        if (page.tabComponent instanceof JLabel) {
            String lt = ((JLabel) page.tabComponent).getText();
            if (lt != null) {
                t = lt;
            }
        }
        return t == null ? "" : MiniHtml.singleLine(t);
    }

    private Icon shownIcon(Page page) {
        if (page.tabComponent instanceof JLabel) {
            Icon li = ((JLabel) page.tabComponent).getIcon();
            if (li != null) {
                return li;
            }
        }
        return page.icon;
    }

    /// Makes the strip of tab buttons show the tabs as they now are.
    private void syncTabs() {
        TabbedPanePeer p = peer();
        if (p == null) {
            return;
        }
        int n = pages.size();
        String[] titles = new String[n];
        com.codename1.ui.Image[] icons = new com.codename1.ui.Image[n];
        boolean[] enabled = new boolean[n];
        for (int i = 0; i < n; i++) {
            Page page = pages.get(i);
            titles[i] = shownTitle(page);
            Icon ic = shownIcon(page);
            if (ic == null) {
                page.nativeIconFor = null;
                page.nativeIcon = null;
            } else if (page.nativeIconFor != ic || page.nativeIcon == null) {
                page.nativeIconFor = ic;
                page.nativeIcon = Icons.toNative(ic, this);
            }
            icons[i] = page.nativeIcon;
            enabled[i] = page.enabled;
        }
        p.setTabs(titles, icons, enabled, getSelectedIndex(), tabPlacement);
    }

    private void tabsChanged() {
        syncTabs();
        revalidate();
        repaint();
    }

    // ------------------------------------------------------------ model

    /// Forwards the model's change as this component's.
    private final class ModelHandler implements ChangeListener {
        @Override
        public void stateChanged(ChangeEvent e) {
            fireStateChanged();
        }
    }

    public SingleSelectionModel getModel() {
        return model;
    }

    public void setModel(SingleSelectionModel model) {
        SingleSelectionModel old = this.model;
        if (old != null) {
            old.removeChangeListener(handler);
        }
        this.model = model;
        if (model != null) {
            model.addChangeListener(handler);
        }
        firePropertyChange("model", old, model);
        showSelected();
        repaint();
    }

    public void addChangeListener(ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }

    public void removeChangeListener(ChangeListener l) {
        listenerList.remove(ChangeListener.class, l);
    }

    public ChangeListener[] getChangeListeners() {
        return listenerList.getListeners(ChangeListener.class);
    }

    /// Shows the page of the selected tab and hides the one that was
    /// showing, then tells the strip.
    private void showSelected() {
        int sel = getSelectedIndex();
        if (sel < 0 || sel >= pages.size()) {
            if (visComp != null && visComp.isVisible()) {
                visComp.setVisible(false);
            }
            visComp = null;
        } else {
            Component now = pages.get(sel).component;
            if (now != null && now != visComp) {
                if (visComp != null && visComp.isVisible()) {
                    visComp.setVisible(false);
                }
                if (!now.isVisible()) {
                    now.setVisible(true);
                }
                visComp = now;
            }
        }
        TabbedPanePeer p = peer();
        if (p != null) {
            p.select(sel);
        }
    }

    protected void fireStateChanged() {
        showSelected();
        ChangeListener[] ls = getChangeListeners();
        for (int i = ls.length - 1; i >= 0; i--) {
            if (changeEvent == null) {
                changeEvent = new ChangeEvent(this);
            }
            ls[i].stateChanged(changeEvent);
        }
        revalidate();
        repaint();
    }

    public int getSelectedIndex() {
        return model == null ? -1 : model.getSelectedIndex();
    }

    public void setSelectedIndex(int index) {
        if (index != -1) {
            checkIndex(index);
        }
        model.setSelectedIndex(index);
    }

    public Component getSelectedComponent() {
        int index = getSelectedIndex();
        if (index == -1) {
            return null;
        }
        return getComponentAt(index);
    }

    public void setSelectedComponent(Component c) {
        int index = indexOfComponent(c);
        if (index == -1) {
            throw new IllegalArgumentException("component not found in tabbed pane");
        }
        setSelectedIndex(index);
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= pages.size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Tab count: " + pages.size());
        }
    }

    // ------------------------------------------------------------ tabs

    public void insertTab(String title, Icon icon, Component component, String tip, int index) {
        int newIndex = index;
        int removeIndex = indexOfComponent(component);
        if (component != null && removeIndex != -1) {
            removeTabAt(removeIndex);
            if (newIndex > removeIndex) {
                newIndex--;
            }
        }
        int selectedIndex = getSelectedIndex();
        Page page = new Page();
        page.title = title != null ? title : "";
        page.icon = icon;
        page.component = component;
        page.tip = tip;
        pages.add(newIndex, page);
        if (component != null) {
            addImpl(component, null, -1);
            component.setVisible(false);
        } else {
            firePropertyChange("indexForNullComponent", -1, index);
        }
        if (pages.size() == 1) {
            setSelectedIndex(0);
        }
        if (selectedIndex >= newIndex) {
            model.setSelectedIndex(selectedIndex + 1);
        }
        tabsChanged();
    }

    public void addTab(String title, Icon icon, Component component, String tip) {
        insertTab(title, icon, component, tip, pages.size());
    }

    public void addTab(String title, Icon icon, Component component) {
        insertTab(title, icon, component, null, pages.size());
    }

    public void addTab(String title, Component component) {
        insertTab(title, null, component, null, pages.size());
    }

    @Override
    public Component add(Component component) {
        addTab(component.getName(), component);
        return component;
    }

    @Override
    public Component add(String title, Component component) {
        addTab(title, component);
        return component;
    }

    @Override
    public Component add(Component component, int index) {
        insertTab(component.getName(), null, component, null, index == -1 ? getTabCount() : index);
        return component;
    }

    @Override
    public void add(Component component, Object constraints) {
        if (constraints instanceof String) {
            addTab((String) constraints, component);
        } else if (constraints instanceof Icon) {
            addTab(null, (Icon) constraints, component);
        } else {
            add(component);
        }
    }

    @Override
    public void add(Component component, Object constraints, int index) {
        Icon icon = constraints instanceof Icon ? (Icon) constraints : null;
        String title = constraints instanceof String ? (String) constraints : null;
        insertTab(title, icon, component, null, index == -1 ? getTabCount() : index);
    }

    public void removeTabAt(int index) {
        checkIndex(index);
        Component component = getComponentAt(index);
        int selected = getSelectedIndex();
        if (component == visComp) {
            visComp = null;
        }
        pages.remove(index);
        if (selected > index) {
            model.setSelectedIndex(selected - 1);
        } else if (selected >= getTabCount()) {
            model.setSelectedIndex(selected - 1);
        } else if (index == selected) {
            fireStateChanged();
        }
        if (component != null) {
            for (int i = getComponentCount() - 1; i >= 0; i--) {
                if (getComponent(i) == component) {
                    super.remove(i);
                    component.setVisible(true);
                    break;
                }
            }
        }
        tabsChanged();
    }

    @Override
    public void remove(Component component) {
        int index = indexOfComponent(component);
        if (index != -1) {
            removeTabAt(index);
        } else {
            for (int i = 0; i < getComponentCount(); i++) {
                if (getComponent(i) == component) {
                    super.remove(i);
                    break;
                }
            }
        }
    }

    @Override
    public void remove(int index) {
        removeTabAt(index);
    }

    @Override
    public void removeAll() {
        model.setSelectedIndex(-1);
        int tabCount = getTabCount();
        while (tabCount-- > 0) {
            removeTabAt(tabCount);
        }
    }

    public int getTabCount() {
        return pages.size();
    }

    /// Always 1: the tabs are in one row or column, which scrolls.
    public int getTabRunCount() {
        return pages.isEmpty() ? 0 : 1;
    }

    public String getTitleAt(int index) {
        return pages.get(index).title;
    }

    public void setTitleAt(int index, String title) {
        Page page = pages.get(index);
        String old = page.title;
        page.title = title;
        if (title == null ? old != null : !title.equals(old)) {
            firePropertyChange("indexForTitle", -1, index);
            tabsChanged();
        }
    }

    public Icon getIconAt(int index) {
        return pages.get(index).icon;
    }

    public void setIconAt(int index, Icon icon) {
        Page page = pages.get(index);
        Icon old = page.icon;
        page.icon = icon;
        if (old != icon) {
            tabsChanged();
        }
    }

    public Icon getDisabledIconAt(int index) {
        return pages.get(index).disabledIcon;
    }

    /// Recorded only: a disabled tab dims the icon it has.
    public void setDisabledIconAt(int index, Icon disabledIcon) {
        pages.get(index).disabledIcon = disabledIcon;
    }

    public String getToolTipTextAt(int index) {
        return pages.get(index).tip;
    }

    /// Recorded only; no tool tip is shown.
    public void setToolTipTextAt(int index, String toolTipText) {
        pages.get(index).tip = toolTipText;
    }

    public Color getBackgroundAt(int index) {
        Color c = pages.get(index).background;
        return c != null ? c : getBackground();
    }

    /// Recorded only: the theme colors the tabs.
    public void setBackgroundAt(int index, Color background) {
        pages.get(index).background = background;
    }

    public Color getForegroundAt(int index) {
        Color c = pages.get(index).foreground;
        return c != null ? c : getForeground();
    }

    /// Recorded only: the theme colors the tabs.
    public void setForegroundAt(int index, Color foreground) {
        pages.get(index).foreground = foreground;
    }

    public boolean isEnabledAt(int index) {
        return pages.get(index).enabled;
    }

    public void setEnabledAt(int index, boolean enabled) {
        Page page = pages.get(index);
        if (page.enabled != enabled) {
            page.enabled = enabled;
            tabsChanged();
        }
    }

    public int getMnemonicAt(int tabIndex) {
        checkIndex(tabIndex);
        return pages.get(tabIndex).mnemonic;
    }

    /// Recorded only.
    public void setMnemonicAt(int tabIndex, int mnemonic) {
        checkIndex(tabIndex);
        pages.get(tabIndex).mnemonic = mnemonic;
        firePropertyChange("mnemonicAt", null, null);
    }

    public int getDisplayedMnemonicIndexAt(int tabIndex) {
        checkIndex(tabIndex);
        return pages.get(tabIndex).mnemonicIndex;
    }

    /// Recorded only.
    public void setDisplayedMnemonicIndexAt(int tabIndex, int mnemonicIndex) {
        checkIndex(tabIndex);
        pages.get(tabIndex).mnemonicIndex = mnemonicIndex;
    }

    public Component getComponentAt(int index) {
        return pages.get(index).component;
    }

    public void setComponentAt(int index, Component component) {
        Page page = pages.get(index);
        if (component != page.component) {
            if (page.component != null) {
                for (int i = getComponentCount() - 1; i >= 0; i--) {
                    if (getComponent(i) == page.component) {
                        super.remove(i);
                        break;
                    }
                }
            }
            page.component = component;
            boolean selectedPage = getSelectedIndex() == index;
            if (selectedPage) {
                visComp = component;
            }
            if (component != null) {
                component.setVisible(selectedPage);
                addImpl(component, null, -1);
            } else {
                repaint();
            }
            revalidate();
        }
    }

    public Component getTabComponentAt(int index) {
        return pages.get(index).tabComponent;
    }

    /// Records the component. It is not shown in the tab; a `JLabel`
    /// lends the tab its text and icon as they are when this is called.
    public void setTabComponentAt(int index, Component component) {
        if (component != null && indexOfComponent(component) != -1) {
            throw new IllegalArgumentException("Component is already added to this JTabbedPane");
        }
        Component old = getTabComponentAt(index);
        if (component != old) {
            int tabComponentIndex = indexOfTabComponent(component);
            if (tabComponentIndex != -1) {
                setTabComponentAt(tabComponentIndex, null);
            }
            pages.get(index).tabComponent = component;
            firePropertyChange("indexForTabComponent", -1, index);
            tabsChanged();
        }
    }

    public int indexOfTab(String title) {
        for (int i = 0; i < pages.size(); i++) {
            String t = pages.get(i).title;
            if (t == null ? title == null : t.equals(title)) {
                return i;
            }
        }
        return -1;
    }

    public int indexOfTab(Icon icon) {
        for (int i = 0; i < pages.size(); i++) {
            Icon tabIcon = pages.get(i).icon;
            if ((tabIcon != null && tabIcon.equals(icon)) || (tabIcon == null && icon == null)) {
                return i;
            }
        }
        return -1;
    }

    public int indexOfComponent(Component component) {
        for (int i = 0; i < pages.size(); i++) {
            Component c = pages.get(i).component;
            if ((c != null && c.equals(component)) || (c == null && component == null)) {
                return i;
            }
        }
        return -1;
    }

    public int indexOfTabComponent(Component tabComponent) {
        for (int i = 0; i < pages.size(); i++) {
            if (pages.get(i).tabComponent == tabComponent) {
                return i;
            }
        }
        return -1;
    }

    /// The bounds of a tab's button, or `null` before the component is
    /// displayed.
    public Rectangle getBoundsAt(int index) {
        checkIndex(index);
        TabbedPanePeer p = peer();
        if (p == null || index >= p.tabCount()) {
            return null;
        }
        int[] b = p.tabBounds(index);
        int x = Units.toLogical(b[0]);
        int y = Units.toLogical(b[1]);
        return new Rectangle(x, y, Units.toLogicalCeil(b[0] + b[2]) - x, Units.toLogicalCeil(b[1] + b[3]) - y);
    }

    public int indexAtLocation(int x, int y) {
        TabbedPanePeer p = peer();
        if (p == null) {
            return -1;
        }
        int dx = Units.toDevice(x);
        int dy = Units.toDevice(y);
        for (int i = 0; i < p.tabCount(); i++) {
            int[] b = p.tabBounds(i);
            if (dx >= b[0] && dx < b[0] + b[2] && dy >= b[1] && dy < b[1] + b[3]) {
                return i;
            }
        }
        return -1;
    }

    // ------------------------------------------------------------ placement

    public int getTabPlacement() {
        return tabPlacement;
    }

    public void setTabPlacement(int tabPlacement) {
        if (tabPlacement != TOP && tabPlacement != LEFT && tabPlacement != BOTTOM && tabPlacement != RIGHT) {
            throw new IllegalArgumentException("illegal tab placement: must be TOP, BOTTOM, LEFT, or RIGHT");
        }
        if (this.tabPlacement != tabPlacement) {
            int old = this.tabPlacement;
            this.tabPlacement = tabPlacement;
            firePropertyChange("tabPlacement", old, tabPlacement);
            tabsChanged();
        }
    }

    public int getTabLayoutPolicy() {
        return tabLayoutPolicy;
    }

    /// Recorded only: tabs that do not fit scroll.
    public void setTabLayoutPolicy(int tabLayoutPolicy) {
        if (tabLayoutPolicy != WRAP_TAB_LAYOUT && tabLayoutPolicy != SCROLL_TAB_LAYOUT) {
            throw new IllegalArgumentException("illegal tab layout policy: must be WRAP_TAB_LAYOUT or SCROLL_TAB_LAYOUT");
        }
        if (this.tabLayoutPolicy != tabLayoutPolicy) {
            int old = this.tabLayoutPolicy;
            this.tabLayoutPolicy = tabLayoutPolicy;
            firePropertyChange("tabLayoutPolicy", old, tabLayoutPolicy);
        }
    }

    // ------------------------------------------------------------ layout

    private boolean across() {
        return tabPlacement == TOP || tabPlacement == BOTTOM;
    }

    /// The size the strip of tabs wants, in logical pixels: the Codename
    /// One strip's, or an estimate from the font before a display exists.
    private Dimension stripSize() {
        if (pages.isEmpty()) {
            return new Dimension(0, 0);
        }
        if (com.codename1.ui.Display.isInitialized()) {
            com.codename1.ui.Component cp = cn1Peer();
            if (cp instanceof TabbedPanePeer) {
                com.codename1.ui.geom.Dimension d = ((TabbedPanePeer) cp).stripPreferredSize();
                return new Dimension(Units.toLogicalCeil(d.getWidth()), Units.toLogicalCeil(d.getHeight()));
            }
        }
        Font f = getFont();
        FontMetrics fm = Fonts.metrics(f != null ? f : Fonts.defaultFont());
        int w = 0;
        int h = 0;
        for (int i = 0; i < pages.size(); i++) {
            Page page = pages.get(i);
            int tw = fm.stringWidth(shownTitle(page)) + 16;
            int th = fm.getHeight() + 8;
            Icon ic = shownIcon(page);
            if (ic != null) {
                tw += ic.getIconWidth() + 4;
                th = Math.max(th, ic.getIconHeight() + 8);
            }
            if (across()) {
                w += tw;
                h = Math.max(h, th);
            } else {
                w = Math.max(w, tw);
                h += th;
            }
        }
        return new Dimension(w, h);
    }

    private Dimension size(boolean minimum) {
        int pw = 0;
        int ph = 0;
        for (int i = 0; i < pages.size(); i++) {
            Component c = pages.get(i).component;
            if (c != null) {
                Dimension d = minimum ? c.getMinimumSize() : c.getPreferredSize();
                pw = Math.max(pw, d.width);
                ph = Math.max(ph, d.height);
            }
        }
        Dimension strip = stripSize();
        Insets in = getInsets();
        int w;
        int h;
        if (across()) {
            w = minimum ? pw : Math.max(pw, strip.width);
            h = ph + strip.height;
        } else {
            w = pw + strip.width;
            h = minimum ? ph : Math.max(ph, strip.height);
        }
        return new Dimension(w + in.left + in.right, h + in.top + in.bottom);
    }

    /// The strip's preferred size along the side it is on plus the
    /// largest preferred size among the pages.
    @Override
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) {
            return super.getPreferredSize();
        }
        return size(false);
    }

    @Override
    public Dimension getMinimumSize() {
        if (isMinimumSizeSet()) {
            return super.getMinimumSize();
        }
        return size(true);
    }

    /// Places the strip on its side and gives every page the rest.
    @Override
    public void doLayout() {
        Insets in = getInsets();
        int x = in.left;
        int y = in.top;
        int w = Math.max(0, getWidth() - in.left - in.right);
        int h = Math.max(0, getHeight() - in.top - in.bottom);
        Dimension strip = stripSize();
        int sx = x;
        int sy = y;
        int sw = w;
        int sh = h;
        int cx = x;
        int cy = y;
        int cw = w;
        int ch = h;
        switch (tabPlacement) {
            case BOTTOM:
                sh = Math.min(strip.height, h);
                sy = y + h - sh;
                ch = h - sh;
                break;
            case LEFT:
                sw = Math.min(strip.width, w);
                cx = x + sw;
                cw = w - sw;
                break;
            case RIGHT:
                sw = Math.min(strip.width, w);
                sx = x + w - sw;
                cw = w - sw;
                break;
            default:
                sh = Math.min(strip.height, h);
                cy = y + sh;
                ch = h - sh;
                break;
        }
        TabbedPanePeer p = peer();
        if (p != null) {
            p.placeStrip(Units.toDevice(sx), Units.toDevice(sy), Units.toDeviceSize(sx, sw),
                    Units.toDeviceSize(sy, sh));
        }
        for (int i = 0; i < getComponentCount(); i++) {
            getComponent(i).setBounds(cx, cy, cw, ch);
        }
    }

    @Override
    protected String paramString() {
        return super.paramString() + ",tabPlacement=" + tabPlacement;
    }
}
