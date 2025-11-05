package com.codename1.impl.android;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.PixelCopy;
import android.view.View;

import com.codename1.impl.android.AndroidNativeUtil;
import com.codename1.io.Log;
import com.codename1.ui.Display;
import com.codename1.ui.Image;
import com.codename1.util.SuccessCallback;

class AndroidScreenshotTask implements Runnable {

    private final CodenameOneSurface view;
    private final Activity activity;
    private final SuccessCallback<Image> callback;

    AndroidScreenshotTask(CodenameOneSurface view, Activity activity, SuccessCallback<Image> callback) {
        this.view = view;
        this.activity = activity;
        this.callback = callback;
    }

    public void run() {
        final int w = view.getViewWidth();
        final int h = view.getViewHeight();

        if (w <= 0 || h <= 0) {
            postError(new IllegalStateException("View not laid out yet"));
            return;
        }

        if (Build.VERSION.SDK_INT >= 26) {
            tryPixelCopy(w, h);
            return;
        }

        // Pre-Oreo: fallback to drawing the view
        tryFallbackDraw(w, h);
    }

    private void tryPixelCopy(final int w, final int h) {
        try {
            final Bitmap target = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            final int[] loc = new int[2];
            ((View)view).getLocationInWindow(loc);

            final android.graphics.Rect src = new android.graphics.Rect(
                    loc[0], loc[1], loc[0] + w, loc[1] + h
            );

            PixelCopy.request(
                    activity.getWindow(),
                    src,
                    target,
                    new PixelCopy.OnPixelCopyFinishedListener() {
                        @Override
                        public void onPixelCopyFinished(int copyResult) {
                            if (copyResult == PixelCopy.SUCCESS) {
                                postSuccess(target);
                            } else {
                                // Fallback if PixelCopy fails (e.g., transient surface state)
                                tryFallbackDraw(w, h);
                            }
                        }
                    },
                    new Handler(Looper.getMainLooper())
            );
        } catch (Throwable t) {
            // Any unexpected issue → fallback
            Log.e(t);
            tryFallbackDraw(w, h);
        }
    }

    private void tryFallbackDraw(int w, int h) {
        try {
            final Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            final Canvas canvas = new Canvas(bmp);
            // Draw the view hierarchy (includes background + children)
            ((View)view).draw(canvas);
            postSuccess(bmp);
        } catch (Throwable t) {
            Log.e(t);
            postError(t);
        }
    }

    private void postSuccess(final Bitmap bmp) {
        if (callback == null) return;
        final Image img = Image.createImage(bmp);
        Display.getInstance().callSerially(new Runnable() {
            @Override public void run() {
                callback.onSucess(img);
            }
        });
    }

    private void postError(final Throwable t) {
        Log.e(t);
    }
}