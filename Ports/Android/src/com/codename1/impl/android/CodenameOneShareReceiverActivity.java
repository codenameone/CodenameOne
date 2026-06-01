/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.impl.android;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import com.codename1.share.SharedContent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/// Receives content shared into the app from other apps through the Android share sheet
/// (ACTION_SEND / ACTION_SEND_MULTIPLE). Stream payloads are copied into app storage so the
/// app sees a stable file path, a `com.codename1.share.SharedContent` is built and delivered
/// to the running app's `onReceivedSharedContent`, then the main activity is brought forward.
///
/// This activity is registered with the appropriate intent filter by the build (see the
/// android.shareFilter build hint).
public class CodenameOneShareReceiverActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            handleShareIntent(getIntent());
        } catch (Throwable t) {
            com.codename1.io.Log.e(t);
        }
        launchMainActivity();
        finish();
    }

    private void handleShareIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        String action = intent.getAction();
        String type = intent.getType();
        SharedContent.Builder b = SharedContent.builder();
        String subject = intent.getStringExtra(Intent.EXTRA_SUBJECT);
        if (subject != null) {
            b.subject(subject);
        }
        boolean any = false;

        if (Intent.ACTION_SEND.equals(action)) {
            CharSequence text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
            if (text != null) {
                addTextOrUrl(b, text.toString());
                any = true;
            }
            Uri stream = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (stream != null) {
                any = addStream(b, stream, type) || any;
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            ArrayList<Uri> streams = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            if (streams != null) {
                for (Uri stream : streams) {
                    any = addStream(b, stream, type) || any;
                }
            }
        }

        if (any) {
            AndroidImplementation.deliverSharedContent(b.build());
        }
    }

    private void addTextOrUrl(SharedContent.Builder b, String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            b.addUrl(trimmed);
        } else {
            b.addText(text);
        }
    }

    private boolean addStream(SharedContent.Builder b, Uri uri, String type) {
        InputStream in = null;
        OutputStream os = null;
        try {
            String mime = type;
            if (mime == null) {
                mime = getContentResolver().getType(uri);
            }
            String name = queryDisplayName(uri);
            File dir = new File(getFilesDir(), "shared");
            if (!dir.exists() && !dir.mkdirs()) {
                com.codename1.io.Log.p("Failed to create shared content directory " + dir.getAbsolutePath());
                return false;
            }
            File out = new File(dir, name);
            in = getContentResolver().openInputStream(uri);
            if (in == null) {
                return false;
            }
            os = new FileOutputStream(out);
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) > 0) {
                os.write(buf, 0, r);
            }
            os.close();
            os = null;
            String path = "file://" + out.getAbsolutePath();
            if (mime != null && mime.startsWith("image/")) {
                b.addImage(mime, path, name);
            } else {
                b.addFile(mime, path, name);
            }
            return true;
        } catch (Throwable t) {
            com.codename1.io.Log.e(t);
            return false;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignore) {
                }
            }
            if (os != null) {
                try {
                    os.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

    private String queryDisplayName(Uri uri) {
        String name = uri.getLastPathSegment();
        try {
            android.database.Cursor c = getContentResolver().query(uri, null, null, null, null);
            if (c != null) {
                try {
                    int idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (idx >= 0 && c.moveToFirst()) {
                        String dn = c.getString(idx);
                        if (dn != null) {
                            name = dn;
                        }
                    }
                } finally {
                    c.close();
                }
            }
        } catch (Throwable ignore) {
        }
        if (name == null || name.length() == 0) {
            name = "shared-" + System.currentTimeMillis();
        }
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private void launchMainActivity() {
        try {
            Intent launch = getPackageManager().getLaunchIntentForPackage(getApplicationInfo().packageName);
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(launch);
            }
        } catch (Throwable t) {
            com.codename1.io.Log.e(t);
        }
    }
}
