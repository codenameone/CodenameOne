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

import java.util.List;

/// An adapter over a list that is replaced with [#submitList(List)]: the
/// difference between the lists is computed and the items animate into
/// their new places.
public abstract class ListAdapter<T, VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {

    final AsyncListDiffer<T> mDiffer;
    private final AsyncListDiffer.ListListener<T> mListener = new AsyncListDiffer.ListListener<T>() {
        @Override
        public void onCurrentListChanged(List<T> previousList, List<T> currentList) {
            ListAdapter.this.onCurrentListChanged(previousList, currentList);
        }
    };

    protected ListAdapter(DiffUtil.ItemCallback<T> diffCallback) {
        mDiffer = new AsyncListDiffer<T>(new AdapterListUpdateCallback(this),
                new AsyncDifferConfig.Builder<T>(diffCallback).build());
        mDiffer.addListListener(mListener);
    }

    protected ListAdapter(AsyncDifferConfig<T> config) {
        mDiffer = new AsyncListDiffer<T>(new AdapterListUpdateCallback(this), config);
        mDiffer.addListListener(mListener);
    }

    public void submitList(List<T> list) {
        mDiffer.submitList(list);
    }

    public void submitList(List<T> list, Runnable commitCallback) {
        mDiffer.submitList(list, commitCallback);
    }

    protected T getItem(int position) {
        return mDiffer.getCurrentList().get(position);
    }

    @Override
    public int getItemCount() {
        return mDiffer.getCurrentList().size();
    }

    public List<T> getCurrentList() {
        return mDiffer.getCurrentList();
    }

    public void onCurrentListChanged(List<T> previousList, List<T> currentList) {
    }
}
