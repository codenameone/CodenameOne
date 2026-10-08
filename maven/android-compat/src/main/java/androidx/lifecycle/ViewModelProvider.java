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

import android.app.Application;

import com.codename1.androidcompat.runtime.ViewModelFactory;

/// Gets an owner's view model of a class, creating it the first time.
///
/// View models are created without reflection: the build generates a
/// factory from the application's compiled classes for every public,
/// concrete `ViewModel` with a public constructor taking nothing, an
/// `Application`, a `SavedStateHandle`, or an `Application` and a
/// `SavedStateHandle`. A factory of the application's own is used as given.
public class ViewModelProvider {

    private static final String DEFAULT_KEY = "androidx.lifecycle.ViewModelProvider.DefaultKey";

    /// Creates view models.
    public interface Factory {
        <T extends ViewModel> T create(Class<T> modelClass);
    }

    /// A factory notified when a view model is handed out again.
    public static class OnRequeryFactory {
        public void onRequery(ViewModel viewModel) {
        }
    }

    /// Creates view models through their no-argument constructor.
    public static class NewInstanceFactory implements Factory {
        private static NewInstanceFactory sInstance;

        public static NewInstanceFactory getInstance() {
            if (sInstance == null) {
                sInstance = new NewInstanceFactory();
            }
            return sInstance;
        }

        @Override
        public <T extends ViewModel> T create(Class<T> modelClass) {
            return createGenerated(modelClass, null, false, null);
        }
    }

    /// Creates view models that take the application, and the others as
    /// [NewInstanceFactory] does.
    public static class AndroidViewModelFactory extends NewInstanceFactory {
        private static AndroidViewModelFactory sAppInstance;
        private final Application mApplication;

        public AndroidViewModelFactory(Application application) {
            mApplication = application;
        }

        public static AndroidViewModelFactory getInstance(Application application) {
            if (sAppInstance == null || sAppInstance.mApplication != application) {
                sAppInstance = new AndroidViewModelFactory(application);
            }
            return sAppInstance;
        }

        @Override
        public <T extends ViewModel> T create(Class<T> modelClass) {
            return createGenerated(modelClass, mApplication, false, null);
        }
    }

    @SuppressWarnings("unchecked")
    static <T extends ViewModel> T createGenerated(Class<T> modelClass, Application app, boolean savedState,
                                                   android.os.Bundle defaultArgs) {
        Object o = ViewModelFactory.create(modelClass.getName(), app,
                savedState ? SavedStateHandle.createHandle(null, defaultArgs) : null);
        if (!(o instanceof ViewModel)) {
            throw new RuntimeException("Cannot create an instance of " + modelClass.getName()
                    + ": it must be public, concrete, and have a public constructor taking nothing, an Application,"
                    + " a SavedStateHandle, or an Application and a SavedStateHandle");
        }
        return (T) o;
    }

    private final ViewModelStore mStore;
    private final Factory mFactory;

    public ViewModelProvider(ViewModelStoreOwner owner) {
        this(owner.getViewModelStore(), defaultFactory(owner));
    }

    public ViewModelProvider(ViewModelStoreOwner owner, Factory factory) {
        this(owner.getViewModelStore(), factory);
    }

    public ViewModelProvider(ViewModelStore store, Factory factory) {
        mStore = store;
        mFactory = factory;
    }

    private static Factory defaultFactory(ViewModelStoreOwner owner) {
        if (owner instanceof HasDefaultViewModelProviderFactory) {
            return ((HasDefaultViewModelProviderFactory) owner).getDefaultViewModelProviderFactory();
        }
        return NewInstanceFactory.getInstance();
    }

    public <T extends ViewModel> T get(Class<T> modelClass) {
        String name = modelClass.getName();
        return get(DEFAULT_KEY + ":" + name, modelClass);
    }

    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T get(String key, Class<T> modelClass) {
        ViewModel existing = mStore.get(key);
        if (existing != null && modelClass.getName().equals(existing.getClass().getName())) {
            if (mFactory instanceof OnRequeryFactory) {
                ((OnRequeryFactory) mFactory).onRequery(existing);
            }
            return (T) existing;
        }
        T vm = mFactory.create(modelClass);
        mStore.put(key, vm);
        return vm;
    }
}
