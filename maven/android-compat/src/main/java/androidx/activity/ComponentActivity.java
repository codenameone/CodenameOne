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
package androidx.activity;

import android.app.Activity;
import android.app.ActivityThread;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultCaller;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.lifecycle.HasDefaultViewModelProviderFactory;
import androidx.lifecycle.SavedStateViewModelFactory;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStore;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.savedstate.SavedStateRegistry;
import androidx.savedstate.SavedStateRegistryOwner;

import com.codename1.ui.CN;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;

import java.util.HashMap;
import java.util.Map;

/// The AndroidX base activity: a `Lifecycle`, view models that survive a
/// configuration change, saved state, the back dispatcher and the Activity
/// Result API over `android.app.Activity`.
///
/// AndroidX's `FragmentActivity` (support fragments) sits between this class
/// and `AppCompatActivity`; it belongs to the AndroidX fragment library.
public class ComponentActivity extends Activity
        implements LifecycleOwner, OnBackPressedDispatcherOwner, ActivityResultCaller, ViewModelStoreOwner,
        HasDefaultViewModelProviderFactory, SavedStateRegistryOwner {

    /// What the activity hands its next instance across a configuration
    /// change: its view models, and the application's own object.
    static final class NonConfigurationInstances {
        Object custom;
        ViewModelStore viewModelStore;
    }

    private ViewModelStore mViewModelStore;
    private ViewModelProvider.Factory mDefaultFactory;
    private final SavedStateRegistry mSavedStateRegistry = new SavedStateRegistry();

    private final LifecycleRegistry mLifecycleRegistry = new LifecycleRegistry(this);
    private final OnBackPressedDispatcher mOnBackPressedDispatcher = new OnBackPressedDispatcher(new Runnable() {
        @Override
        public void run() {
            ComponentActivity.super.onBackPressed();
        }
    });
    private final Map<Integer, Registration<?, ?>> mRegistrations = new HashMap<Integer, Registration<?, ?>>();
    private int mNextRequestCode = 0xA000;
    /// Keyed registrations take codes from 0x8000 up, below the ordered ones.
    private static final int KEYED_CODE_BASE = 0x8000;
    private static final int KEYED_CODES = 0x2000;

    public ComponentActivity() {
    }

    @Override
    public Lifecycle getLifecycle() {
        return mLifecycleRegistry;
    }

    @Override
    public OnBackPressedDispatcher getOnBackPressedDispatcher() {
        return mOnBackPressedDispatcher;
    }

    // ------------------------------------------------------------ lifecycle

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mSavedStateRegistry.performRestore(savedInstanceState);
        super.onCreate(savedInstanceState);
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mSavedStateRegistry.performSave(outState);
    }

    // ------------------------------------------------------------ view models

    @Override
    public ViewModelStore getViewModelStore() {
        if (mViewModelStore == null) {
            Object last = getLastNonConfigurationInstance();
            if (last instanceof NonConfigurationInstances) {
                mViewModelStore = ((NonConfigurationInstances) last).viewModelStore;
            }
            if (mViewModelStore == null) {
                mViewModelStore = new ViewModelStore();
            }
        }
        return mViewModelStore;
    }

    @Override
    public ViewModelProvider.Factory getDefaultViewModelProviderFactory() {
        if (mDefaultFactory == null) {
            mDefaultFactory = new SavedStateViewModelFactory(getApplication(), this,
                    getIntent() != null ? getIntent().getExtras() : null);
        }
        return mDefaultFactory;
    }

    @Override
    public SavedStateRegistry getSavedStateRegistry() {
        return mSavedStateRegistry;
    }

    /// Carries the view models, and [#onRetainCustomNonConfigurationInstance()],
    /// to the instance that replaces this one after a configuration change.
    /// Final, as in AndroidX.
    @Override
    public final Object onRetainNonConfigurationInstance() {
        Object custom = onRetainCustomNonConfigurationInstance();
        ViewModelStore store = mViewModelStore;
        if (store == null) {
            Object last = getLastNonConfigurationInstance();
            if (last instanceof NonConfigurationInstances) {
                store = ((NonConfigurationInstances) last).viewModelStore;
            }
        }
        if (store == null && custom == null) {
            return null;
        }
        NonConfigurationInstances nci = new NonConfigurationInstances();
        nci.custom = custom;
        nci.viewModelStore = store;
        return nci;
    }

    /// Deprecated in AndroidX in favour of view models; kept for applications
    /// that still use it.
    public Object onRetainCustomNonConfigurationInstance() {
        return null;
    }

    public Object getLastCustomNonConfigurationInstance() {
        Object last = getLastNonConfigurationInstance();
        return last instanceof NonConfigurationInstances ? ((NonConfigurationInstances) last).custom : null;
    }

    @Override
    protected void onStart() {
        super.onStart();
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
    }

    @Override
    protected void onPause() {
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);
        super.onPause();
    }

    @Override
    protected void onStop() {
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (mLifecycleRegistry.getCurrentState() != Lifecycle.State.INITIALIZED) {
            mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);
        }
        if (mViewModelStore != null && !isChangingConfigurations()) {
            mViewModelStore.clear();
        }
        super.onDestroy();
    }

    /// The dispatcher's callbacks first, then the activity's own behaviour
    /// (the fragment back stack, then finishing).
    @Override
    public void onBackPressed() {
        mOnBackPressedDispatcher.onBackPressed();
    }

    // ------------------------------------------------------------ results

    private final class Registration<I, O> extends ActivityResultLauncher<I> {
        final int requestCode;
        final ActivityResultContract<I, O> contract;
        final ActivityResultCallback<O> callback;

        Registration(int requestCode, ActivityResultContract<I, O> contract, ActivityResultCallback<O> callback) {
            this.requestCode = requestCode;
            this.contract = contract;
            this.callback = callback;
        }

        @Override
        public void launch(I input, Object options) {
            ActivityResultContract.SynchronousResult<O> sync = contract.getSynchronousResult(ComponentActivity.this, input);
            if (sync != null) {
                // Posted to the main queue, as AndroidX does, so the callback
                // runs after launch() returns like every other result, never
                // inside the caller's own code. A launcher unregistered in
                // the meantime no longer hears it.
                final O value = sync.getValue();
                new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        if (mRegistrations.get(Integer.valueOf(requestCode)) == Registration.this) {
                            callback.onActivityResult(value);
                        }
                    }
                });
                return;
            }
            Intent intent = contract.createIntent(ComponentActivity.this, input);
            String action = intent.getAction();
            if (ActivityResultContracts.RequestMultiplePermissions.ACTION_REQUEST_PERMISSIONS.equals(action)) {
                String[] permissions = intent.getStringArrayExtra(
                        ActivityResultContracts.RequestMultiplePermissions.EXTRA_PERMISSIONS);
                requestPermissions(permissions == null ? new String[0] : permissions, requestCode);
            } else if (Intent.ACTION_GET_CONTENT.equals(action)) {
                pickContent(requestCode, intent.getType());
            } else {
                startActivityForResult(intent, requestCode);
            }
        }

        void deliver(int resultCode, Intent data) {
            callback.onActivityResult(contract.parseResult(resultCode, data));
        }

        @Override
        public void unregister() {
            mRegistrations.remove(Integer.valueOf(requestCode));
        }

        @Override
        public ActivityResultContract<I, ?> getContract() {
            return contract;
        }
    }

    @Override
    public final <I, O> ActivityResultLauncher<I> registerForActivityResult(ActivityResultContract<I, O> contract,
                                                                            ActivityResultCallback<O> callback) {
        int code = mNextRequestCode++;
        Registration<I, O> r = new Registration<I, O>(code, contract, callback);
        mRegistrations.put(Integer.valueOf(code), r);
        return r;
    }

    /// Registers under a request code derived from `key` rather than from
    /// the registration order, for the runtime's fragments. A fragment
    /// recreated with its activity registers at a different point of the
    /// replacement's start-up than the original did -- during
    /// `super.onCreate`, before launchers the activity registers after it --
    /// so an order-allotted code would hand a pending result to whichever
    /// callback now holds it. The same key gives the same code in every
    /// instance, unless two live keys share a slot, when the later one
    /// takes the next free code.
    public final <I, O> ActivityResultLauncher<I> registerForActivityResult(String key,
            ActivityResultContract<I, O> contract, ActivityResultCallback<O> callback) {
        int slot = (key.hashCode() & 0x7fffffff) % KEYED_CODES;
        int code = KEYED_CODE_BASE + slot;
        for (int i = 0; i < KEYED_CODES && mRegistrations.containsKey(Integer.valueOf(code)); i++) {
            slot = (slot + 1) % KEYED_CODES;
            code = KEYED_CODE_BASE + slot;
        }
        Registration<I, O> r = new Registration<I, O>(code, contract, callback);
        mRegistrations.put(Integer.valueOf(code), r);
        return r;
    }

    /// Content picking through the platform's gallery: images, videos, or
    /// anything for other MIME types. The result goes to whatever is
    /// registered under `requestCode` when the picker returns, as
    /// `onActivityResult` does, so a launcher unregistered while the gallery
    /// was open hears nothing. When the activity was recreated meanwhile
    /// (rotated with the gallery open) that is the replacement's
    /// registration, made under the same code; one finished for good hears
    /// nothing.
    private void pickContent(final int requestCode, String mimeType) {
        int type = CN.GALLERY_ALL;
        if (mimeType != null && mimeType.regionMatches(true, 0, "image/", 0, 6)) {
            type = CN.GALLERY_IMAGE;
        } else if (mimeType != null && mimeType.regionMatches(true, 0, "video/", 0, 6)) {
            type = CN.GALLERY_VIDEO;
        }
        CN.openGallery(new ActionListener<ActionEvent>() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                Activity current = ActivityThread.currentInstance(ComponentActivity.this);
                if (!(current instanceof ComponentActivity)) {
                    return;
                }
                Registration<?, ?> r =
                        ((ComponentActivity) current).mRegistrations.get(Integer.valueOf(requestCode));
                if (r == null) {
                    return;
                }
                Object source = evt == null ? null : evt.getSource();
                if (source instanceof String) {
                    Intent data = new Intent();
                    data.setData(Uri.parse((String) source));
                    r.deliver(RESULT_OK, data);
                } else {
                    r.deliver(RESULT_CANCELED, null);
                }
            }
        }, type);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Registration<?, ?> r = mRegistrations.get(Integer.valueOf(requestCode));
        if (r != null) {
            r.deliver(resultCode, data);
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Registration<?, ?> r = mRegistrations.get(Integer.valueOf(requestCode));
        if (r != null) {
            Intent data = new Intent();
            data.putExtra(ActivityResultContracts.RequestMultiplePermissions.EXTRA_PERMISSIONS, permissions);
            data.putExtra(ActivityResultContracts.RequestMultiplePermissions.EXTRA_PERMISSION_GRANT_RESULTS,
                    grantResults);
            r.deliver(RESULT_OK, data);
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
