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
package androidx.activity.result.contract;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;

import androidx.activity.result.ActivityResult;

import java.util.LinkedHashMap;
import java.util.Map;

/// The standard contracts.
public final class ActivityResultContracts {

    private ActivityResultContracts() {
    }

    /// Starts an activity and answers its result code and data.
    public static final class StartActivityForResult extends ActivityResultContract<Intent, ActivityResult> {
        public static final String EXTRA_ACTIVITY_OPTIONS_BUNDLE =
                "androidx.activity.result.contract.extra.ACTIVITY_OPTIONS_BUNDLE";

        @Override
        public Intent createIntent(Context context, Intent input) {
            return input;
        }

        @Override
        public ActivityResult parseResult(int resultCode, Intent intent) {
            return new ActivityResult(resultCode, intent);
        }
    }

    /// Requests several permissions and answers each one's grant.
    public static final class RequestMultiplePermissions
            extends ActivityResultContract<String[], Map<String, Boolean>> {
        public static final String ACTION_REQUEST_PERMISSIONS =
                "androidx.activity.result.contract.action.REQUEST_PERMISSIONS";
        public static final String EXTRA_PERMISSIONS = "androidx.activity.result.contract.extra.PERMISSIONS";
        public static final String EXTRA_PERMISSION_GRANT_RESULTS =
                "androidx.activity.result.contract.extra.PERMISSION_GRANT_RESULTS";

        @Override
        public Intent createIntent(Context context, String[] input) {
            return new Intent(ACTION_REQUEST_PERMISSIONS).putExtra(EXTRA_PERMISSIONS, input);
        }

        @Override
        public SynchronousResult<Map<String, Boolean>> getSynchronousResult(Context context, String[] input) {
            if (input == null || input.length == 0) {
                return new SynchronousResult<Map<String, Boolean>>(new LinkedHashMap<String, Boolean>());
            }
            Map<String, Boolean> granted = new LinkedHashMap<String, Boolean>();
            for (String p : input) {
                if (context.checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) {
                    return null;
                }
                granted.put(p, Boolean.TRUE);
            }
            return new SynchronousResult<Map<String, Boolean>>(granted);
        }

        @Override
        public Map<String, Boolean> parseResult(int resultCode, Intent intent) {
            Map<String, Boolean> out = new LinkedHashMap<String, Boolean>();
            if (resultCode != Activity.RESULT_OK || intent == null) {
                return out;
            }
            String[] permissions = intent.getStringArrayExtra(EXTRA_PERMISSIONS);
            int[] grants = intent.getIntArrayExtra(EXTRA_PERMISSION_GRANT_RESULTS);
            if (permissions == null || grants == null) {
                return out;
            }
            for (int i = 0; i < permissions.length && i < grants.length; i++) {
                out.put(permissions[i], Boolean.valueOf(grants[i] == PackageManager.PERMISSION_GRANTED));
            }
            return out;
        }
    }

    /// Requests one permission and answers whether it was granted.
    public static final class RequestPermission extends ActivityResultContract<String, Boolean> {
        @Override
        public Intent createIntent(Context context, String input) {
            return new RequestMultiplePermissions().createIntent(context, new String[] {input});
        }

        @Override
        public SynchronousResult<Boolean> getSynchronousResult(Context context, String input) {
            if (input != null && context.checkSelfPermission(input) == PackageManager.PERMISSION_GRANTED) {
                return new SynchronousResult<Boolean>(Boolean.TRUE);
            }
            return null;
        }

        @Override
        public Boolean parseResult(int resultCode, Intent intent) {
            if (resultCode != Activity.RESULT_OK || intent == null) {
                return Boolean.FALSE;
            }
            int[] grants = intent.getIntArrayExtra(RequestMultiplePermissions.EXTRA_PERMISSION_GRANT_RESULTS);
            if (grants == null) {
                return Boolean.FALSE;
            }
            for (int g : grants) {
                if (g == PackageManager.PERMISSION_GRANTED) {
                    return Boolean.TRUE;
                }
            }
            return Boolean.FALSE;
        }
    }

    /// Picks content of a MIME type (`image/*`) and answers its `Uri`, or
    /// null when the user cancels.
    public static class GetContent extends ActivityResultContract<String, Uri> {
        @Override
        public Intent createIntent(Context context, String input) {
            return new Intent(Intent.ACTION_GET_CONTENT).addCategory(Intent.CATEGORY_OPENABLE).setType(input);
        }

        @Override
        public Uri parseResult(int resultCode, Intent intent) {
            return intent == null || resultCode != Activity.RESULT_OK ? null : intent.getData();
        }
    }
}
