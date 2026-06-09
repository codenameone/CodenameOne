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
                                compositeGLPeers(target, loc[0], loc[1]);
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
            // Any unexpected issue -> fallback
            Log.e(t);
            tryFallbackDraw(w, h);
        }
    }

    private void tryFallbackDraw(int w, int h) {
        try {
            final Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            final Canvas canvas = new Canvas(bmp);

            // Get the parent container that holds both the CodenameOneSurface and PeerComponents
            View viewToDraw = (View)view;
            android.view.ViewParent parent = viewToDraw.getParent();

            // If the view has a parent (relativeLayout), draw the parent to include PeerComponents
            // Otherwise fall back to drawing just the view
            if (parent instanceof View) {
                View parentView = (View) parent;
                // Save the parent's current position
                final int[] parentLoc = new int[2];
                parentView.getLocationInWindow(parentLoc);
                final int[] viewLoc = new int[2];
                viewToDraw.getLocationInWindow(viewLoc);

                // Calculate offset between view and parent
                final int offsetX = viewLoc[0] - parentLoc[0];
                final int offsetY = viewLoc[1] - parentLoc[1];

                // Translate canvas to align view content correctly
                canvas.translate(-offsetX, -offsetY);

                // Draw the parent view hierarchy (includes PeerComponents as siblings)
                parentView.draw(canvas);
                compositeGLPeers(bmp, viewLoc[0], viewLoc[1]);
            } else {
                // Fallback: draw only the view if no parent found
                viewToDraw.draw(canvas);
                final int[] viewLoc = new int[2];
                viewToDraw.getLocationInWindow(viewLoc);
                compositeGLPeers(bmp, viewLoc[0], viewLoc[1]);
            }

            postSuccess(bmp);
        } catch (Throwable t) {
            Log.e(t);
            postError(t);
        }
    }

    /// Composites the most recent GPU-read-back frame of every live GL peer onto
    /// the captured screenshot. `originX`/`originY` are the window coordinates of
    /// the captured bitmap's top-left corner, so peer positions (also in window
    /// coordinates) can be made relative to it.
    private void compositeGLPeers(Bitmap target, int originX, int originY) {
        java.util.List<AndroidGLSurface> peers;
        synchronized (AndroidGLSurface.ACTIVE) {
            peers = new java.util.ArrayList<AndroidGLSurface>(AndroidGLSurface.ACTIVE);
        }
        if (peers.isEmpty()) {
            return;
        }
        Canvas canvas = new Canvas(target);
        for (AndroidGLSurface peer : peers) {
            try {
                // Only composite peers on the form currently on screen. isShown()
                // can still be true for a beat while a previous form is torn down,
                // which bled e.g. the 3D animation frame into DesktopMode's capture.
                if (!peer.isShown() || !peer.isOnCurrentForm()) {
                    continue;
                }
                Bitmap frame = peer.getLastFrame();
                if (frame == null) {
                    continue;
                }
                int[] ploc = new int[2];
                peer.getLocationInWindow(ploc);
                int dx = ploc[0] - originX;
                int dy = ploc[1] - originY;
                android.graphics.Rect dst = new android.graphics.Rect(
                        dx, dy, dx + peer.getWidth(), dy + peer.getHeight());
                canvas.drawBitmap(frame, null, dst, null);
            } catch (Throwable t) {
                Log.e(t);
            }
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