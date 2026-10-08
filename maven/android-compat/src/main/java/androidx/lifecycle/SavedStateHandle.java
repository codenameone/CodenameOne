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

import android.os.Bundle;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/// Key-value state a view model keeps with it, readable as live data. The
/// handle lives as long as its view model, which an activity or fragment
/// gets back after a configuration change; it starts from the owner's
/// arguments when the view model is created by the default factory.
public final class SavedStateHandle {

    private final Map<String, Object> mRegular = new HashMap<String, Object>();
    private final Map<String, SavingStateLiveData<?>> mLiveDatas = new HashMap<String, SavingStateLiveData<?>>();

    public SavedStateHandle() {
    }

    public SavedStateHandle(Map<String, Object> initialState) {
        mRegular.putAll(initialState);
    }

    /// Runtime use: a handle holding `defaultState`'s entries, then
    /// `restoredState`'s.
    public static SavedStateHandle createHandle(Bundle restoredState, Bundle defaultState) {
        SavedStateHandle h = new SavedStateHandle();
        if (defaultState != null) {
            for (String key : defaultState.keySet()) {
                h.mRegular.put(key, defaultState.get(key));
            }
        }
        if (restoredState != null) {
            for (String key : restoredState.keySet()) {
                h.mRegular.put(key, restoredState.get(key));
            }
        }
        return h;
    }

    public boolean contains(String key) {
        return mRegular.containsKey(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) mRegular.get(key);
    }

    public <T> void set(String key, T value) {
        mRegular.put(key, value);
        SavingStateLiveData<?> live = mLiveDatas.get(key);
        if (live != null) {
            live.setValueRaw(value);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T remove(String key) {
        T latest = (T) mRegular.remove(key);
        SavingStateLiveData<?> live = mLiveDatas.remove(key);
        if (live != null) {
            live.detach();
        }
        return latest;
    }

    public Set<String> keys() {
        Set<String> all = new HashSet<String>(mRegular.keySet());
        all.addAll(mLiveDatas.keySet());
        return all;
    }

    public <T> MutableLiveData<T> getLiveData(String key) {
        return getLiveDataInternal(key, false, null);
    }

    public <T> MutableLiveData<T> getLiveData(String key, T initialValue) {
        return getLiveDataInternal(key, true, initialValue);
    }

    @SuppressWarnings("unchecked")
    private <T> MutableLiveData<T> getLiveDataInternal(String key, boolean hasInitial, T initialValue) {
        SavingStateLiveData<?> existing = mLiveDatas.get(key);
        if (existing != null) {
            return (MutableLiveData<T>) existing;
        }
        SavingStateLiveData<T> live;
        if (mRegular.containsKey(key)) {
            live = new SavingStateLiveData<T>(this, key, (T) mRegular.get(key));
        } else if (hasInitial) {
            mRegular.put(key, initialValue);
            live = new SavingStateLiveData<T>(this, key, initialValue);
        } else {
            live = new SavingStateLiveData<T>(this, key);
        }
        mLiveDatas.put(key, live);
        return live;
    }

    static final class SavingStateLiveData<T> extends MutableLiveData<T> {
        private SavedStateHandle mHandle;
        private final String mKey;

        SavingStateLiveData(SavedStateHandle handle, String key, T value) {
            super(value);
            mHandle = handle;
            mKey = key;
        }

        SavingStateLiveData(SavedStateHandle handle, String key) {
            super();
            mHandle = handle;
            mKey = key;
        }

        @Override
        public void setValue(T value) {
            if (mHandle != null) {
                mHandle.mRegular.put(mKey, value);
            }
            super.setValue(value);
        }

        @SuppressWarnings("unchecked")
        void setValueRaw(Object value) {
            super.setValue((T) value);
        }

        void detach() {
            mHandle = null;
        }
    }
}
