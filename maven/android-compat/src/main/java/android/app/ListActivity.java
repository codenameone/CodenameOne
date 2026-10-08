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
package android.app;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;

/// An activity showing a list. Without a content view of its own it uses a
/// full-screen `ListView`; a custom layout needs a `ListView` whose id is
/// `android.R.id.list`, and may have an `android.R.id.empty` view shown
/// while the list is empty.
public class ListActivity extends Activity {

    protected ListAdapter mAdapter;
    protected ListView mList;
    private boolean mFinishedStart;

    private final AdapterView.OnItemClickListener mOnClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
            onListItemClick((ListView) parent, v, position, id);
        }
    };

    public ListActivity() {
    }

    protected void onListItemClick(ListView l, View v, int position, long id) {
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        ensureList();
        super.onRestoreInstanceState(state);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onContentChanged() {
        super.onContentChanged();
        View emptyView = findViewById(android.R.id.empty);
        View list = findViewById(android.R.id.list);
        if (!(list instanceof ListView)) {
            throw new RuntimeException("Your content must have a ListView whose id attribute is 'android.R.id.list'");
        }
        mList = (ListView) list;
        if (emptyView != null) {
            mList.setEmptyView(emptyView);
        }
        mList.setOnItemClickListener(mOnClickListener);
        if (mFinishedStart) {
            setListAdapter(mAdapter);
        }
        mFinishedStart = true;
    }

    public void setListAdapter(ListAdapter adapter) {
        ensureList();
        mAdapter = adapter;
        mList.setAdapter(adapter);
    }

    public void setSelection(int position) {
        mList.setSelection(position);
    }

    public int getSelectedItemPosition() {
        return mList.getSelectedItemPosition();
    }

    public long getSelectedItemId() {
        return mList.getSelectedItemId();
    }

    public ListView getListView() {
        ensureList();
        return mList;
    }

    public ListAdapter getListAdapter() {
        return mAdapter;
    }

    private void ensureList() {
        if (mList != null) {
            return;
        }
        setContentView(android.R.layout.list_content_simple);
    }
}
