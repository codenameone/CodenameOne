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
package androidx.lifecycle;

import androidx.arch.core.util.Function;

/// Live data derived from other live data.
public final class Transformations {

    private Transformations() {
    }

    /// A live data holding `mapFunction` applied to each value of `source`.
    public static <X, Y> LiveData<Y> map(LiveData<X> source, final Function<X, Y> mapFunction) {
        final MediatorLiveData<Y> result = new MediatorLiveData<Y>();
        result.addSource(source, new Observer<X>() {
            @Override
            public void onChanged(X x) {
                result.setValue(mapFunction.apply(x));
            }
        });
        return result;
    }

    /// A live data following the live data `switchMapFunction` answers for
    /// each value of `source`.
    public static <X, Y> LiveData<Y> switchMap(LiveData<X> source,
                                               final Function<X, LiveData<Y>> switchMapFunction) {
        final MediatorLiveData<Y> result = new MediatorLiveData<Y>();
        result.addSource(source, new Observer<X>() {
            private LiveData<Y> mSource;

            @Override
            public void onChanged(X x) {
                LiveData<Y> newLiveData = switchMapFunction.apply(x);
                if (mSource == newLiveData) {
                    return;
                }
                if (mSource != null) {
                    result.removeSource(mSource);
                }
                mSource = newLiveData;
                if (mSource != null) {
                    result.addSource(mSource, new Observer<Y>() {
                        @Override
                        public void onChanged(Y y) {
                            result.setValue(y);
                        }
                    });
                }
            }
        });
        return result;
    }

    /// A live data passing on only the values of `source` that differ from
    /// the last one.
    public static <X> LiveData<X> distinctUntilChanged(LiveData<X> source) {
        final MediatorLiveData<X> result = new MediatorLiveData<X>();
        result.addSource(source, new Observer<X>() {
            private boolean mFirstTime = true;

            @Override
            public void onChanged(X current) {
                X previous = result.getValue();
                if (mFirstTime || (previous == null && current != null)
                        || (previous != null && !previous.equals(current))) {
                    mFirstTime = false;
                    result.setValue(current);
                }
            }
        });
        return result;
    }
}
