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
package com.codename1.components;

import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Graphics;
import com.codename1.ui.Tabs;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.SelectionListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.util.EventDispatcher;

/// A row of page dots (a dots indicator or pager indicator) that shows which
/// page of a pager, carousel or onboarding flow is on screen. The current page
/// is drawn as a wider pill; tapping a dot selects its page.
///
/// `Carousel` includes one. Use it on its own with swipeable `Tabs`:
///
/// ```java
/// Tabs pages = new Tabs();
/// pages.hideTabs();
/// pages.addTab("", page1);
/// pages.addTab("", page2);
/// PageIndicator dots = new PageIndicator();
/// dots.bind(pages);
/// form.add(BorderLayout.CENTER, pages).add(BorderLayout.SOUTH, dots);
/// ```
///
/// The dots are drawn in the foreground colour of the `PageIndicator` UIID, the
/// current one at full opacity and the rest faded, so they suit light and dark
/// themes without styling.
public class PageIndicator extends Component {
    private static final float DOT_MM = 1.6f;
    private static final float GAP_MM = 1.6f;
    private static final int FADED_ALPHA = 0x59;

    private int pageCount;
    private int currentPage;
    private final EventDispatcher listeners = new EventDispatcher();

    /// An indicator with no pages; set the count with `#setPageCount(int)` or
    /// `#bind(Tabs)`.
    public PageIndicator() {
        setUIID("PageIndicator");
        setFocusable(false);
    }

    /// An indicator for a number of pages.
    ///
    /// #### Parameters
    ///
    /// - `pageCount`: how many pages there are
    public PageIndicator(int pageCount) {
        this();
        this.pageCount = Math.max(0, pageCount);
    }

    /// Keeps this indicator in step with swipeable tabs: the count follows the
    /// number of tabs when they change selection, a swipe moves the dot, and a
    /// tap on a dot slides to that tab.
    ///
    /// #### Parameters
    ///
    /// - `tabs`: the tabs to follow
    public void bind(final Tabs tabs) {
        setPageCount(tabs.getTabCount());
        setCurrentPage(tabs.getSelectedIndex());
        tabs.addSelectionListener(new SelectionListener() {
            @Override
            public void selectionChanged(int oldSelected, int newSelected) {
                setPageCount(tabs.getTabCount());
                setCurrentPage(newSelected);
            }
        });
        addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                if (currentPage < tabs.getTabCount()) {
                    tabs.setSelectedIndex(currentPage, true);
                }
            }
        });
    }

    /// The number of dots.
    ///
    /// #### Returns
    ///
    /// the page count
    public int getPageCount() {
        return pageCount;
    }

    /// Sets the number of dots.
    ///
    /// #### Parameters
    ///
    /// - `pageCount`: how many pages there are
    public void setPageCount(int pageCount) {
        pageCount = Math.max(0, pageCount);
        if (pageCount != this.pageCount) {
            this.pageCount = pageCount;
            if (currentPage >= pageCount) {
                currentPage = Math.max(0, pageCount - 1);
            }
            setShouldCalcPreferredSize(true);
            repaint();
        }
    }

    /// The page drawn as current.
    ///
    /// #### Returns
    ///
    /// the zero based current page
    public int getCurrentPage() {
        return currentPage;
    }

    /// Sets the page drawn as current. Does not fire the action listeners;
    /// they hear only taps.
    ///
    /// #### Parameters
    ///
    /// - `currentPage`: the zero based page
    public void setCurrentPage(int currentPage) {
        if (currentPage >= 0 && currentPage < pageCount && currentPage != this.currentPage) {
            this.currentPage = currentPage;
            repaint();
        }
    }

    /// Listens to taps that select a page; `#getCurrentPage()` holds the page
    /// tapped.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener
    public void addActionListener(ActionListener l) {
        listeners.addListener(l);
    }

    /// Stops a listener from hearing taps.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener
    public void removeActionListener(ActionListener l) {
        listeners.removeListener(l);
    }

    @Override
    protected Dimension calcPreferredSize() {
        Style style = getStyle();
        int dot = CN.convertToPixels(DOT_MM);
        int w = rowWidth();
        // Taller than a dot so a finger can hit it.
        int h = Math.max(dot, CN.convertToPixels(6f));
        return new Dimension(w + style.getHorizontalPadding(), h + style.getVerticalPadding());
    }

    /// The width of the dots: a pill three dots wide for the current page.
    private int rowWidth() {
        if (pageCount == 0) {
            return 0;
        }
        int dot = CN.convertToPixels(DOT_MM);
        int gap = CN.convertToPixels(GAP_MM);
        return (pageCount - 1) * (dot + gap) + dot * 3;
    }

    @Override
    public void paint(Graphics g) {
        if (pageCount == 0) {
            return;
        }
        Style style = getStyle();
        int dot = CN.convertToPixels(DOT_MM);
        int gap = CN.convertToPixels(GAP_MM);
        int inner = getWidth() - style.getHorizontalPadding();
        int x = getX() + style.getPaddingLeftNoRTL() + (inner - rowWidth()) / 2;
        int y = getY() + style.getPaddingTop() + (getHeight() - style.getVerticalPadding() - dot) / 2;
        int alpha = g.getAlpha();
        int color = g.getColor();
        boolean aa = g.isAntiAliased();
        g.setAntiAliased(true);
        g.setColor(dotColor(style));
        for (int iter = 0; iter < pageCount; iter++) {
            int page = isRTL() ? pageCount - 1 - iter : iter;
            boolean current = page == currentPage;
            int w = current ? dot * 3 : dot;
            g.setAlpha(current ? alpha : FADED_ALPHA * alpha / 255);
            g.fillRoundRect(x, y, w, dot, dot, dot);
            x += w + gap;
        }
        g.setAlpha(alpha);
        g.setColor(color);
        g.setAntiAliased(aa);
    }

    private int dotColor(Style style) {
        if (getUIManager().isThemeStyleDefined(getUIID())) {
            return style.getFgColor();
        }
        return getUIManager().getComponentStyle("Label").getFgColor();
    }

    @Override
    public void pointerReleased(int x, int y) {
        super.pointerReleased(x, y);
        int page = pageAt(x);
        if (page >= 0 && page != currentPage) {
            currentPage = page;
            repaint();
            listeners.fireActionEvent(new ActionEvent(this, ActionEvent.Type.Other, x, y));
        }
    }

    /// The page whose dot is nearest an x coordinate, or -1 outside the row.
    int pageAt(int px) {
        if (pageCount == 0) {
            return -1;
        }
        Style style = getStyle();
        int dot = CN.convertToPixels(DOT_MM);
        int gap = CN.convertToPixels(GAP_MM);
        int inner = getWidth() - style.getHorizontalPadding();
        int x = getAbsoluteX() + style.getPaddingLeftNoRTL() + (inner - rowWidth()) / 2;
        for (int iter = 0; iter < pageCount; iter++) {
            int page = isRTL() ? pageCount - 1 - iter : iter;
            int w = page == currentPage ? dot * 3 : dot;
            if (px >= x - gap / 2 && px < x + w + gap - gap / 2) {
                return page;
            }
            x += w + gap;
        }
        return -1;
    }
}
