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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentFactory;

import com.codename1.compat.testing.MainThreadRule;
import org.junit.Rule;
import org.junit.Test;

/// A fragment launches for a result, and its activity is recreated for a
/// configuration change while the launched activity runs. The result comes
/// back to the replacement, and must reach the replacement fragment's
/// callback -- not be dropped for want of a registration, nor handed to a
/// launcher the activity registered after the fragments were restored.
public class FragmentLauncherRecreationTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    /// A fragment with one launcher, declared in a field initializer.
    public static final class Picker extends androidx.fragment.app.Fragment {
        ActivityResult received;
        final ActivityResultLauncher<Intent> launcher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        received = result;
                    }
                });
    }

    /// Registers a launcher of its own in `onCreate`, after the fragments are
    /// restored, and records the request code of every launch.
    static final class Host extends FragmentActivity {
        int lastRequestCode = -1;
        ActivityResult ownResult;
        private android.view.LayoutInflater inflater;

        @Override
        public android.view.LayoutInflater getLayoutInflater() {
            if (inflater == null) {
                inflater = new com.codename1.androidcompat.runtime.PhoneLayoutInflater(this);
            }
            return inflater;
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    new ActivityResultCallback<ActivityResult>() {
                        @Override
                        public void onActivityResult(ActivityResult result) {
                            ownResult = result;
                        }
                    });
        }

        @Override
        protected void onDestroy() {
            super.onDestroy();
        }

        @Override
        protected void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
        }

        @Override
        public void startActivityForResult(Intent intent, int requestCode) {
            lastRequestCode = requestCode;
        }

        @Override
        public void startActivityForResult(Intent intent, int requestCode, Bundle options) {
            lastRequestCode = requestCode;
        }
    }

    private static void resume(Activity a) {
        a.hostsDispatchActivityCreated();
        a.hostsDispatchStart();
        a.hostsDispatchResume();
    }

    @Test
    public void theResultReachesTheRecreatedFragment() {
        Host a = new Host();
        a.onCreate(null);
        resume(a);
        Picker picker = new Picker();
        a.getSupportFragmentManager().beginTransaction().add(picker, "picker").commitNow();
        picker.launcher.launch(new Intent("com.example.PICK"));
        int code = a.lastRequestCode;
        assertTrue("the launch did not reach the activity", code >= 0);

        Bundle saved = new Bundle();
        Activity base = a;
        base.hostsDispatchPause();
        a.onSaveInstanceState(saved);
        base.hostsDispatchStop();
        Object nonConfig = a.onRetainNonConfigurationInstance();
        base.mChangingConfigurations = true;
        base.hostsDispatchDestroy();
        a.onDestroy();

        Host b = new Host();
        ((Activity) b).mLastNonConfigurationInstance = nonConfig;
        b.getSupportFragmentManager().setFragmentFactory(new FragmentFactory() {
            @Override
            public androidx.fragment.app.Fragment instantiate(ClassLoader classLoader, String className) {
                return new Picker();
            }
        });
        b.onCreate(saved);
        resume(b);
        Picker restored = (Picker) b.getSupportFragmentManager().findFragmentByTag("picker");
        assertNotNull(restored);

        ((Activity) b).dispatchActivityResult(code, Activity.RESULT_OK, null);
        assertNull("the fragment's result went to the activity's own launcher", b.ownResult);
        assertNotNull("the result never reached the recreated fragment", restored.received);
        assertEquals(Activity.RESULT_OK, restored.received.getResultCode());
    }
}
