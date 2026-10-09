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
package android.widget;

import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

/// A list adapter with fixed header and footer views around another adapter,
/// which is what `ListView.addHeaderView` installs.
public class HeaderViewListAdapter implements WrapperListAdapter, Filterable {

    private final ListAdapter mAdapter;
    final ArrayList<ListView.FixedViewInfo> mHeaderViewInfos;
    final ArrayList<ListView.FixedViewInfo> mFooterViewInfos;

    public HeaderViewListAdapter(ArrayList<ListView.FixedViewInfo> headerViewInfos,
                                 ArrayList<ListView.FixedViewInfo> footerViewInfos, ListAdapter adapter) {
        mAdapter = adapter;
        mHeaderViewInfos = headerViewInfos == null ? new ArrayList<ListView.FixedViewInfo>() : headerViewInfos;
        mFooterViewInfos = footerViewInfos == null ? new ArrayList<ListView.FixedViewInfo>() : footerViewInfos;
    }

    public int getHeadersCount() {
        return mHeaderViewInfos.size();
    }

    public int getFootersCount() {
        return mFooterViewInfos.size();
    }

    @Override
    public boolean isEmpty() {
        return mAdapter == null || mAdapter.isEmpty();
    }

    public boolean removeHeader(View v) {
        for (int i = 0; i < mHeaderViewInfos.size(); i++) {
            if (mHeaderViewInfos.get(i).view == v) {
                mHeaderViewInfos.remove(i);
                return true;
            }
        }
        return false;
    }

    public boolean removeFooter(View v) {
        for (int i = 0; i < mFooterViewInfos.size(); i++) {
            if (mFooterViewInfos.get(i).view == v) {
                mFooterViewInfos.remove(i);
                return true;
            }
        }
        return false;
    }

    private int adapterCount() {
        return mAdapter == null ? 0 : mAdapter.getCount();
    }

    @Override
    public int getCount() {
        return getHeadersCount() + adapterCount() + getFootersCount();
    }

    @Override
    public boolean areAllItemsEnabled() {
        return mAdapter == null || mAdapter.areAllItemsEnabled();
    }

    @Override
    public boolean isEnabled(int position) {
        int numHeaders = getHeadersCount();
        if (position < numHeaders) {
            return mHeaderViewInfos.get(position).isSelectable;
        }
        int adjPosition = position - numHeaders;
        if (adjPosition < adapterCount()) {
            return mAdapter.isEnabled(adjPosition);
        }
        return mFooterViewInfos.get(adjPosition - adapterCount()).isSelectable;
    }

    @Override
    public Object getItem(int position) {
        int numHeaders = getHeadersCount();
        if (position < numHeaders) {
            return mHeaderViewInfos.get(position).data;
        }
        int adjPosition = position - numHeaders;
        if (adjPosition < adapterCount()) {
            return mAdapter.getItem(adjPosition);
        }
        return mFooterViewInfos.get(adjPosition - adapterCount()).data;
    }

    @Override
    public long getItemId(int position) {
        int adjPosition = position - getHeadersCount();
        if (mAdapter != null && adjPosition >= 0 && adjPosition < adapterCount()) {
            return mAdapter.getItemId(adjPosition);
        }
        return -1;
    }

    @Override
    public boolean hasStableIds() {
        return mAdapter != null && mAdapter.hasStableIds();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int numHeaders = getHeadersCount();
        if (position < numHeaders) {
            return mHeaderViewInfos.get(position).view;
        }
        int adjPosition = position - numHeaders;
        if (adjPosition < adapterCount()) {
            return mAdapter.getView(adjPosition, convertView, parent);
        }
        return mFooterViewInfos.get(adjPosition - adapterCount()).view;
    }

    @Override
    public int getItemViewType(int position) {
        int adjPosition = position - getHeadersCount();
        if (mAdapter != null && adjPosition >= 0 && adjPosition < adapterCount()) {
            return mAdapter.getItemViewType(adjPosition);
        }
        return AdapterView.ITEM_VIEW_TYPE_HEADER_OR_FOOTER;
    }

    @Override
    public int getViewTypeCount() {
        return mAdapter == null ? 1 : mAdapter.getViewTypeCount();
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        if (mAdapter != null) {
            mAdapter.registerDataSetObserver(observer);
        }
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        if (mAdapter != null) {
            mAdapter.unregisterDataSetObserver(observer);
        }
    }

    @Override
    public Filter getFilter() {
        return mAdapter instanceof Filterable ? ((Filterable) mAdapter).getFilter() : null;
    }

    @Override
    public ListAdapter getWrappedAdapter() {
        return mAdapter;
    }
}
