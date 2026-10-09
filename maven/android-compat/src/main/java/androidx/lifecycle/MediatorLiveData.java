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

import java.util.ArrayList;
import java.util.List;

/// A [LiveData] fed by other live data: each source is observed while this
/// one has active observers.
public class MediatorLiveData<T> extends MutableLiveData<T> {

    private static final class Source<V> implements Observer<V> {
        final LiveData<V> mLiveData;
        final Observer<? super V> mObserver;
        int mVersion = LiveData.START_VERSION;

        Source(LiveData<V> liveData, Observer<? super V> observer) {
            mLiveData = liveData;
            mObserver = observer;
        }

        void plug() {
            mLiveData.observeForever(this);
        }

        void unplug() {
            mLiveData.removeObserver(this);
        }

        @Override
        public void onChanged(V v) {
            if (mVersion != mLiveData.getVersion()) {
                mVersion = mLiveData.getVersion();
                mObserver.onChanged(v);
            }
        }
    }

    private final List<Source<?>> mSources = new ArrayList<Source<?>>();

    public MediatorLiveData() {
        super();
    }

    public MediatorLiveData(T value) {
        super(value);
    }

    public <S> void addSource(LiveData<S> source, Observer<? super S> onChanged) {
        if (source == null) {
            throw new NullPointerException("source cannot be null");
        }
        for (Source<?> s : mSources) {
            if (s.mLiveData == source) {
                if (s.mObserver != onChanged) {
                    throw new IllegalArgumentException("This source was already added with the different observer");
                }
                return;
            }
        }
        Source<S> e = new Source<S>(source, onChanged);
        mSources.add(e);
        if (hasActiveObservers()) {
            e.plug();
        }
    }

    public <S> void removeSource(LiveData<S> toRemove) {
        for (int i = 0; i < mSources.size(); i++) {
            Source<?> s = mSources.get(i);
            if (s.mLiveData == toRemove) {
                mSources.remove(i);
                s.unplug();
                return;
            }
        }
    }

    @Override
    protected void onActive() {
        for (Source<?> s : new ArrayList<Source<?>>(mSources)) {
            // An earlier source can synchronously remove this source
            // (for example when switchMap changes its inner LiveData).
            if (hasActiveObservers() && mSources.contains(s)) {
                s.plug();
            }
        }
    }

    @Override
    protected void onInactive() {
        for (Source<?> s : new ArrayList<Source<?>>(mSources)) {
            s.unplug();
        }
    }
}
