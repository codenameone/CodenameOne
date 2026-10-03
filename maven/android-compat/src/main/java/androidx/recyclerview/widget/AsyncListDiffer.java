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
package androidx.recyclerview.widget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// Keeps a list for an adapter and, when a new list is submitted, notifies
/// the adapter of the difference. The diff runs synchronously on the UI
/// thread when [#submitList(List)] is called; the commit callback runs once
/// the adapter has been notified, as on Android.
public class AsyncListDiffer<T> {

    /// Told when the current list changes.
    public interface ListListener<T> {
        void onCurrentListChanged(List<T> previousList, List<T> currentList);
    }

    private final ListUpdateCallback mUpdateCallback;
    final AsyncDifferConfig<T> mConfig;
    private final List<ListListener<T>> mListeners = new ArrayList<ListListener<T>>();
    private List<T> mList;
    private List<T> mReadOnlyList = Collections.emptyList();
    int mMaxScheduledGeneration;

    public AsyncListDiffer(RecyclerView.Adapter adapter, DiffUtil.ItemCallback<T> diffCallback) {
        this(new AdapterListUpdateCallback(adapter), new AsyncDifferConfig.Builder<T>(diffCallback).build());
    }

    public AsyncListDiffer(ListUpdateCallback listUpdateCallback, AsyncDifferConfig<T> config) {
        mUpdateCallback = listUpdateCallback;
        mConfig = config;
    }

    public List<T> getCurrentList() {
        return mReadOnlyList;
    }

    public void submitList(List<T> newList) {
        submitList(newList, null);
    }

    public void submitList(final List<T> newList, Runnable commitCallback) {
        mMaxScheduledGeneration++;
        if (newList == mList) {
            if (commitCallback != null) {
                commitCallback.run();
            }
            return;
        }
        List<T> previousList = mReadOnlyList;
        if (newList == null) {
            int countRemoved = mList == null ? 0 : mList.size();
            mList = null;
            mReadOnlyList = Collections.emptyList();
            if (countRemoved > 0) {
                mUpdateCallback.onRemoved(0, countRemoved);
            }
            onCurrentListChanged(previousList, commitCallback);
            return;
        }
        if (mList == null) {
            mList = newList;
            mReadOnlyList = new RecyclerView.UnmodifiableList<T>(newList);
            mUpdateCallback.onInserted(0, newList.size());
            onCurrentListChanged(previousList, commitCallback);
            return;
        }
        final List<T> oldList = mList;
        final DiffUtil.ItemCallback<T> itemCallback = mConfig.getDiffCallback();
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return oldList.size();
            }

            @Override
            public int getNewListSize() {
                return newList.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                T oldItem = oldList.get(oldItemPosition);
                T newItem = newList.get(newItemPosition);
                if (oldItem != null && newItem != null) {
                    return itemCallback.areItemsTheSame(oldItem, newItem);
                }
                return oldItem == null && newItem == null;
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                T oldItem = oldList.get(oldItemPosition);
                T newItem = newList.get(newItemPosition);
                if (oldItem != null && newItem != null) {
                    return itemCallback.areContentsTheSame(oldItem, newItem);
                }
                return oldItem == null && newItem == null;
            }

            @Override
            public Object getChangePayload(int oldItemPosition, int newItemPosition) {
                T oldItem = oldList.get(oldItemPosition);
                T newItem = newList.get(newItemPosition);
                if (oldItem != null && newItem != null) {
                    return itemCallback.getChangePayload(oldItem, newItem);
                }
                return null;
            }
        });
        mList = newList;
        mReadOnlyList = new RecyclerView.UnmodifiableList<T>(newList);
        result.dispatchUpdatesTo(mUpdateCallback);
        onCurrentListChanged(previousList, commitCallback);
    }

    private void onCurrentListChanged(List<T> previousList, Runnable commitCallback) {
        for (ListListener<T> listener : new ArrayList<ListListener<T>>(mListeners)) {
            listener.onCurrentListChanged(previousList, mReadOnlyList);
        }
        if (commitCallback != null) {
            commitCallback.run();
        }
    }

    public void addListListener(ListListener<T> listener) {
        mListeners.add(listener);
    }

    public void removeListListener(ListListener<T> listener) {
        mListeners.remove(listener);
    }
}
