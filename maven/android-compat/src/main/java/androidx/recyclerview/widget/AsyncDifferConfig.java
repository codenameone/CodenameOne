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

/// What an [AsyncListDiffer] compares items with.
///
/// Android computes the diff on a background executor; here it runs on the
/// UI thread when the list is submitted (Codename One's runtime has no
/// executors), so there is no `setBackgroundThreadExecutor`.
public final class AsyncDifferConfig<T> {

    private final DiffUtil.ItemCallback<T> mDiffCallback;

    AsyncDifferConfig(DiffUtil.ItemCallback<T> diffCallback) {
        mDiffCallback = diffCallback;
    }

    public DiffUtil.ItemCallback<T> getDiffCallback() {
        return mDiffCallback;
    }

    /// Builds an [AsyncDifferConfig].
    public static final class Builder<T> {
        private final DiffUtil.ItemCallback<T> mDiffCallback;

        public Builder(DiffUtil.ItemCallback<T> diffCallback) {
            mDiffCallback = diffCallback;
        }

        public AsyncDifferConfig<T> build() {
            return new AsyncDifferConfig<T>(mDiffCallback);
        }
    }
}
