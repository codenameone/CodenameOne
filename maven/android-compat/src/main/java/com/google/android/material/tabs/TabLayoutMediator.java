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
package com.google.android.material.tabs;

import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

/// Links a [TabLayout] to a [ViewPager2]: one tab per page, configured by a
/// [TabConfigurationStrategy]; selecting a tab moves the pager, dragging the
/// pager moves the indicator, and the tabs are rebuilt when the adapter's
/// data changes (unless `autoRefresh` is off).
public final class TabLayoutMediator {

    /// Sets up the tab at `position`: its text, icon or custom view.
    public interface TabConfigurationStrategy {
        void onConfigureTab(TabLayout.Tab tab, int position);
    }

    private final TabLayout mTabLayout;
    private final ViewPager2 mViewPager;
    private final boolean mAutoRefresh;
    private final boolean mSmoothScroll;
    private final TabConfigurationStrategy mStrategy;
    private RecyclerView.Adapter<?> mAdapter;
    private boolean mAttached;
    private PagerCallback mPageCallback;
    private TabLayout.OnTabSelectedListener mTabListener;
    private RecyclerView.AdapterDataObserver mObserver;

    public TabLayoutMediator(TabLayout tabLayout, ViewPager2 viewPager, TabConfigurationStrategy strategy) {
        this(tabLayout, viewPager, true, strategy);
    }

    public TabLayoutMediator(TabLayout tabLayout, ViewPager2 viewPager, boolean autoRefresh,
            TabConfigurationStrategy strategy) {
        this(tabLayout, viewPager, autoRefresh, true, strategy);
    }

    public TabLayoutMediator(TabLayout tabLayout, ViewPager2 viewPager, boolean autoRefresh, boolean smoothScroll,
            TabConfigurationStrategy strategy) {
        mTabLayout = tabLayout;
        mViewPager = viewPager;
        mAutoRefresh = autoRefresh;
        mSmoothScroll = smoothScroll;
        mStrategy = strategy;
    }

    /// Connects the two. The pager must already have its adapter.
    public void attach() {
        if (mAttached) {
            throw new IllegalStateException("TabLayoutMediator is already attached");
        }
        mAdapter = mViewPager.getAdapter();
        if (mAdapter == null) {
            throw new IllegalStateException("TabLayoutMediator attached before ViewPager2 has an adapter");
        }
        mAttached = true;
        mPageCallback = new PagerCallback(mTabLayout);
        mViewPager.registerOnPageChangeCallback(mPageCallback);
        mTabListener = new TabListener(mViewPager, mSmoothScroll);
        mTabLayout.addOnTabSelectedListener(mTabListener);
        if (mAutoRefresh) {
            mObserver = new RecyclerView.AdapterDataObserver() {
                @Override
                public void onChanged() {
                    populateTabsFromPagerAdapter();
                }

                @Override
                public void onItemRangeChanged(int positionStart, int itemCount) {
                    populateTabsFromPagerAdapter();
                }

                @Override
                public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
                    populateTabsFromPagerAdapter();
                }

                @Override
                public void onItemRangeInserted(int positionStart, int itemCount) {
                    populateTabsFromPagerAdapter();
                }

                @Override
                public void onItemRangeRemoved(int positionStart, int itemCount) {
                    populateTabsFromPagerAdapter();
                }

                @Override
                public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
                    populateTabsFromPagerAdapter();
                }
            };
            mAdapter.registerAdapterDataObserver(mObserver);
        }
        populateTabsFromPagerAdapter();
        mTabLayout.setScrollPosition(mViewPager.getCurrentItem(), 0f, true);
    }

    /// Disconnects the two and removes the tabs.
    public void detach() {
        if (mAutoRefresh && mAdapter != null && mObserver != null) {
            mAdapter.unregisterAdapterDataObserver(mObserver);
            mObserver = null;
        }
        if (mTabListener != null) {
            mTabLayout.removeOnTabSelectedListener(mTabListener);
        }
        if (mPageCallback != null) {
            mViewPager.unregisterOnPageChangeCallback(mPageCallback);
        }
        mTabListener = null;
        mPageCallback = null;
        mAdapter = null;
        mAttached = false;
    }

    public boolean isAttached() {
        return mAttached;
    }

    void populateTabsFromPagerAdapter() {
        mTabLayout.removeAllTabs();
        if (mAdapter == null) {
            return;
        }
        int count = mAdapter.getItemCount();
        for (int i = 0; i < count; i++) {
            TabLayout.Tab tab = mTabLayout.newTab();
            mStrategy.onConfigureTab(tab, i);
            mTabLayout.addTab(tab, false);
        }
        if (count > 0) {
            int last = mTabLayout.getTabCount() - 1;
            int current = Math.min(mViewPager.getCurrentItem(), last);
            if (current != mTabLayout.getSelectedTabPosition()) {
                mTabLayout.selectTab(mTabLayout.getTabAt(current));
            }
        }
    }

    /// Follows the pager: the indicator tracks a drag, and the settled page
    /// selects its tab.
    private static final class PagerCallback extends ViewPager2.OnPageChangeCallback {
        private final TabLayout mTabs;
        private int mPreviousState = ViewPager2.SCROLL_STATE_IDLE;
        private int mState = ViewPager2.SCROLL_STATE_IDLE;

        PagerCallback(TabLayout tabs) {
            mTabs = tabs;
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            mPreviousState = mState;
            mState = state;
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            boolean updateText = mState != ViewPager2.SCROLL_STATE_SETTLING
                    || mPreviousState == ViewPager2.SCROLL_STATE_DRAGGING;
            boolean updateIndicator = !(mState == ViewPager2.SCROLL_STATE_SETTLING
                    && mPreviousState == ViewPager2.SCROLL_STATE_IDLE);
            mTabs.setScrollPosition(position, positionOffset, updateText, updateIndicator);
        }

        @Override
        public void onPageSelected(int position) {
            if (mTabs.getSelectedTabPosition() != position && position < mTabs.getTabCount()) {
                boolean updateIndicator = mState == ViewPager2.SCROLL_STATE_IDLE
                        || (mState == ViewPager2.SCROLL_STATE_SETTLING
                        && mPreviousState == ViewPager2.SCROLL_STATE_IDLE);
                mTabs.selectTab(mTabs.getTabAt(position), updateIndicator);
            }
        }
    }

    /// Follows the tabs: a selected tab moves the pager to its page.
    private static final class TabListener implements TabLayout.OnTabSelectedListener {
        private final ViewPager2 mPager;
        private final boolean mSmooth;

        TabListener(ViewPager2 pager, boolean smooth) {
            mPager = pager;
            mSmooth = smooth;
        }

        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            mPager.setCurrentItem(tab.getPosition(), mSmooth);
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {
        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {
        }
    }
}
