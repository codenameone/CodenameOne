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
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Tabs;
import com.codename1.ui.TopLevelContainer;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.SelectionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.util.UITimer;

/// A carousel (also called an image slider, view pager, banner rotator or
/// onboarding pager): pages the user swipes between, with page dots under them
/// and optional auto play.
///
/// ```java
/// Carousel carousel = new Carousel();
/// carousel.addPage(new ScaleImageLabel(banner1));
/// carousel.addPage(new ScaleImageLabel(banner2));
/// carousel.addPage(new ScaleImageLabel(banner3));
/// carousel.setAutoPlay(4000);
/// form.add(carousel);
/// carousel.addSelectionListener((oldPage, newPage) -> track(newPage));
/// ```
///
/// It is swipeable `Tabs` with the tab bar hidden plus a [PageIndicator], so
/// it swipes, snaps and mirrors for right to left languages as `Tabs` does.
/// `#getTabs()` and `#getIndicator()` give access to both for anything this
/// class does not wrap.
///
/// Auto play advances one page per interval while the carousel is on screen,
/// waits a full interval after the user touches it, and wraps from the last
/// page to the first when `#setLoop(boolean)` is on (the default).
///
/// UIIDs: [Carousel] for the container, [PageIndicator] for the dots.
public class Carousel extends Container {
    private final Tabs tabs = new Tabs();
    private final PageIndicator indicator = new PageIndicator();
    private String indicatorPosition = BorderLayout.SOUTH;
    private int autoPlay;
    private boolean loop = true;
    private long lastTouch;
    private UITimer timer;
    private Form touchForm;
    /// A press lands on the deepest component, never on this container, so
    /// presses are heard on the form and kept when they fall inside it.
    private final ActionListener touchListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent evt) {
            if (contains(evt.getX(), evt.getY())) {
                lastTouch = System.currentTimeMillis();
            }
        }
    };

    /// An empty carousel; add pages with `#addPage(Component)`.
    public Carousel() {
        super(new BorderLayout());
        setUIID("Carousel");
        tabs.hideTabs();
        tabs.setSwipeActivated(true);
        tabs.getContentPane().setUIID("Container");
        tabs.setUIID("Container");
        indicator.bind(tabs);
        tabs.addSelectionListener(new SelectionListener() {
            @Override
            public void selectionChanged(int oldSelected, int newSelected) {
                // No lastTouch update here: Tabs fires this when an auto play
                // slide ends, which would push every other tick back. User
                // swipes and taps start with a press, which the form pointer
                // listener records.
                announce(newSelected);
            }
        });
        add(BorderLayout.CENTER, tabs);
        add(indicatorPosition, indicator);
    }

    /// Adds a page at the end.
    ///
    /// #### Parameters
    ///
    /// - `page`: the page's content
    ///
    /// #### Returns
    ///
    /// this, for chaining
    public Carousel addPage(Component page) {
        tabs.addTab("", page);
        indicator.setPageCount(tabs.getTabCount());
        return this;
    }

    /// Removes a page.
    ///
    /// #### Parameters
    ///
    /// - `index`: the zero based page
    public void removePage(int index) {
        // Tabs.removeTabAt always steps back one page; keep the page the user
        // is looking at unless that is the one removed.
        int current = tabs.getSelectedIndex();
        tabs.removeTabAt(index);
        if (index > current && current < tabs.getTabCount()) {
            tabs.setSelectedIndex(current);
        }
        indicator.setPageCount(tabs.getTabCount());
        indicator.setCurrentPage(tabs.getSelectedIndex());
    }

    /// The number of pages.
    ///
    /// #### Returns
    ///
    /// the page count
    public int getPageCount() {
        return tabs.getTabCount();
    }

    /// The page on screen.
    ///
    /// #### Returns
    ///
    /// the zero based page
    public int getSelectedIndex() {
        return tabs.getSelectedIndex();
    }

    /// Moves to a page.
    ///
    /// #### Parameters
    ///
    /// - `index`: the zero based page
    ///
    /// - `animate`: true to slide there, false to jump
    public void setSelectedIndex(int index, boolean animate) {
        tabs.setSelectedIndex(index, animate);
        indicator.setCurrentPage(index);
    }

    /// Listens to page changes, from swipes, taps on the dots, auto play and
    /// `#setSelectedIndex(int, boolean)`.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener, told the previous page and the new one
    public void addSelectionListener(SelectionListener l) {
        tabs.addSelectionListener(l);
    }

    /// Stops a listener from hearing page changes.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener
    public void removeSelectionListener(SelectionListener l) {
        tabs.removeSelectionListener(l);
    }

    /// Advances the pages on a timer.
    ///
    /// #### Parameters
    ///
    /// - `millis`: the time each page stays on screen; 0 or less turns auto play
    ///   off
    public void setAutoPlay(int millis) {
        autoPlay = Math.max(0, millis);
        stopTimer();
        if (isInitialized()) {
            startTimer();
        }
    }

    /// The auto play interval.
    ///
    /// #### Returns
    ///
    /// milliseconds per page, 0 when auto play is off
    public int getAutoPlay() {
        return autoPlay;
    }

    /// Whether auto play wraps from the last page to the first. On by default;
    /// off, auto play stops at the last page.
    ///
    /// #### Parameters
    ///
    /// - `loop`: true to wrap
    public void setLoop(boolean loop) {
        this.loop = loop;
    }

    /// Whether auto play wraps from the last page to the first.
    ///
    /// #### Returns
    ///
    /// true if it wraps
    public boolean isLoop() {
        return loop;
    }

    /// Where the dots go: `BorderLayout.SOUTH` (the default), `BorderLayout.NORTH`,
    /// or null to hide them.
    ///
    /// #### Parameters
    ///
    /// - `position`: a `BorderLayout` constraint, or null
    public void setIndicatorPosition(String position) {
        if (indicator.getParent() != null) {
            removeComponent(indicator);
        }
        indicatorPosition = position;
        if (position != null) {
            add(position, indicator);
        }
        revalidateLater();
    }

    /// The swipeable tabs that hold the pages.
    ///
    /// #### Returns
    ///
    /// the tabs
    public Tabs getTabs() {
        return tabs;
    }

    /// The page dots.
    ///
    /// #### Returns
    ///
    /// the indicator
    public PageIndicator getIndicator() {
        return indicator;
    }

    @Override
    protected void initComponent() {
        super.initComponent();
        touchForm = getComponentForm();
        if (touchForm != null) {
            touchForm.addPointerPressedListener(touchListener);
        }
        startTimer();
    }

    @Override
    protected void deinitialize() {
        stopTimer();
        if (touchForm != null) {
            touchForm.removePointerPressedListener(touchListener);
            touchForm = null;
        }
        super.deinitialize();
    }

    private void startTimer() {
        if (autoPlay <= 0 || timer != null) {
            return;
        }
        TopLevelContainer top = getTopLevelContainer();
        if (top == null) {
            return;
        }
        lastTouch = System.currentTimeMillis();
        timer = UITimer.timer(autoPlay, true, top, new Runnable() {
            @Override
            public void run() {
                tick();
            }
        });
    }

    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    /// One auto play step: skipped while the user has touched the carousel
    /// within the last interval, so it never moves under a finger.
    void tick() {
        int count = tabs.getTabCount();
        if (count < 2 || System.currentTimeMillis() - lastTouch < autoPlay - 50) {
            return;
        }
        // The indicator moves at once; the tabs commit their index only when
        // the slide ends, so they would still name the page being left.
        int next = indicator.getCurrentPage() + 1;
        if (next >= count) {
            if (!loop) {
                return;
            }
            next = 0;
        }
        setSelectedIndex(next, true);
    }

    private void announce(int page) {
        if (isInitialized()) {
            CN.announceForAccessibility(this, "Page " + (page + 1) + " of " + tabs.getTabCount());
        }
    }
}
