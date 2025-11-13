package com.codenameone.examples.hellocodenameone.tests;

import android.content.Context;
import android.net.Uri;

import androidx.core.content.FileProvider;

import com.codename1.impl.android.AndroidNativeUtil;

import java.io.File;

/**
 * Android implementation that exposes a FileProvider backed content URI for
 * the generated sample audio file so the media APIs can play it using the same
 * code paths as user-selected media.
 */
public class MediaPlaybackNativeImpl implements MediaPlaybackNative {
    @Override
    public boolean isSupported() {
        return true;
    }

    @Override
    public String resolveContentUri(String absolutePath) {
        if (absolutePath == null) {
            return null;
        }
        Context context = AndroidNativeUtil.getContext();
        if (context == null) {
            return absolutePath;
        }
        File file = new File(absolutePath);
        if (!file.exists()) {
            return absolutePath;
        }
        try {
            Uri uri = FileProvider.getUriForFile(context,
                    context.getPackageName() + ".provider", file);
            return uri != null ? uri.toString() : absolutePath;
        } catch (IllegalArgumentException ex) {
            com.codename1.io.Log.e(ex);
            return absolutePath;
        }
    }
}
