package com.codenameone.fidelity.nativeref;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.view.PixelCopy;
import android.view.View;
import android.widget.FrameLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Standalone native-reference capture app for the Android fidelity suite: the
 * Android counterpart of NativeRef.swift. Renders each reference Material 3
 * widget in a REAL window and captures it with PixelCopy (so elevation
 * shadows, bars and pressed state-layers come out exactly as a user sees
 * them), writing the PNGs to the app's external files dir where
 * scripts/build-android-native-ref.sh pulls and commits them as the
 * android-m3 golden set.
 *
 * The golden-set contract: capture on the SAME emulator profile the fidelity
 * CI uses (API level + 160dpi -- see scripts-fidelity.yml). Tile pixel sizes
 * derive from mm exactly like the CN1 side (mm * dpi / 25.4), so at 160dpi a
 * 60mm tile is the same 377px on both sides.
 */
public class MainActivity extends Activity {
    private static final String TAG = "NativeRef";
    private static final String[] APPEARANCES = {"light", "dark"};

    private static final class Spec {
        final String component;
        final String kind;
        final String[] states;
        final float wMM;
        final float hMM;
        final String text;

        Spec(String component, String kind, String[] states, float wMM, float hMM, String text) {
            this.component = component;
            this.kind = kind;
            this.states = states;
            this.wMM = wMM;
            this.hMM = hMM;
            this.text = text;
        }
    }

    // Keep in sync with fidelity-tests.yaml (ids, native_android kinds, states,
    // tile sizes) -- this table IS the Android native reference set.
    private static final Spec[] SPECS = {
        new Spec("Button", "material_button_filled", new String[]{"normal", "pressed", "disabled"}, 60, 14, "Default"),
        new Spec("RaisedButton", "material_button_tonal", new String[]{"normal", "pressed", "disabled"}, 60, 14, "Raised"),
        new Spec("FlatButton", "material_button_outlined", new String[]{"normal", "pressed"}, 60, 14, "Flat"),
        new Spec("TextField", "material_textinput", new String[]{"normal", "disabled"}, 60, 14, "Hello"),
        new Spec("CheckBox", "material_checkbox", new String[]{"normal", "selected", "disabled"}, 60, 14, "Enabled"),
        new Spec("RadioButton", "material_radio", new String[]{"normal", "selected", "disabled"}, 60, 14, "Option"),
        new Spec("Switch", "material_switch", new String[]{"normal", "selected", "disabled"}, 60, 14, ""),
        new Spec("Slider", "material_slider", new String[]{"normal", "disabled"}, 60, 14, ""),
        new Spec("ProgressBar", "material_progress_linear", new String[]{"normal"}, 60, 14, ""),
        new Spec("Tabs", "material_tablayout", new String[]{"normal"}, 60, 16, ""),
        new Spec("Toolbar", "material_toolbar", new String[]{"normal"}, 60, 16, "Title"),
        new Spec("Dialog", "material_alert_view", new String[]{"normal"}, 60, 40, "Message"),
        new Spec("FloatingActionButton", "material_fab", new String[]{"normal", "pressed"}, 20, 20, ""),
    };

    private static final class Job {
        final Spec spec;
        final String state;
        final String appearance;

        Job(Spec spec, String state, String appearance) {
            this.spec = spec;
            this.state = state;
            this.appearance = appearance;
        }
    }

    private final List<Job> jobs = new ArrayList<Job>();
    private int jobIndex;
    private FrameLayout root;
    private File outDir;
    private HandlerThread copyThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if ("animate".equals(getIntent().getStringExtra("mode"))) {
            // Animation-reference mode (record-android-native-anim.sh): loop a
            // REAL Material animation (switch toggle / tab indicator slide)
            // while the host records the screen. The video is the native motion
            // reference beside the deterministic CN1 morph frames.
            runAnimation(getIntent().getStringExtra("anim"),
                    getIntent().getStringExtra("appearance"));
            return;
        }
        outDir = new File(getExternalFilesDir(null), "refs");
        deleteRecursively(outDir);
        outDir.mkdirs();
        int dpi = getResources().getDisplayMetrics().densityDpi;
        if (dpi != 160) {
            Log.w(TAG, "WARNING: densityDpi=" + dpi + " (the android-m3 golden set is captured at 160);"
                    + " tile pixel sizes and dp-derived widget metrics will differ from CI");
        }
        for (Spec s : SPECS) {
            for (String appearance : APPEARANCES) {
                for (String state : s.states) {
                    jobs.add(new Job(s, state, appearance));
                }
            }
        }
        copyThread = new HandlerThread("pixelcopy");
        copyThread.start();
        root = new FrameLayout(this);
        root.setBackgroundColor(0xFF888888);
        setContentView(root);
        root.post(new Runnable() {
            public void run() {
                nextJob();
            }
        });
    }

    private void nextJob() {
        if (jobIndex >= jobs.size()) {
            done();
            return;
        }
        final Job job = jobs.get(jobIndex++);
        float dpi = getResources().getDisplayMetrics().densityDpi;
        final int w = Math.max(1, Math.round(job.spec.wMM * dpi / 25.4f));
        final int h = Math.max(1, Math.round(job.spec.hMM * dpi / 25.4f));
        View tile = RefWidgets.buildTile(this, job.spec.kind, job.state, job.appearance,
                job.spec.text, w, h);
        if (tile == null) {
            Log.e(TAG, "unknown kind " + job.spec.kind);
            nextJob();
            return;
        }
        root.removeAllViews();
        root.addView(tile, new FrameLayout.LayoutParams(w, h));
        final View tileRef = tile;
        // Lay out first, then apply the state. Static states snap their
        // drawables and capture after a short settle; pressed states hold a
        // REAL touch-down and wait for the ripple's enter animation to reach
        // its held steady state before the copy (well past the ~300ms Material
        // ripple, still under the long-press timeout side effects mattering
        // for these widgets).
        final boolean pressed = "pressed".equals(job.state);
        tile.post(new Runnable() {
            public void run() {
                RefWidgets.applyPressedIfNeeded(tileRef, job.state);
                if (!pressed) {
                    RefWidgets.jumpDrawables(tileRef);
                    tileRef.postDelayed(new Runnable() {
                        public void run() {
                            capture(job, tileRef, w, h);
                        }
                    }, 120);
                    return;
                }
                // Held press: let the ripple's enter animation play out and
                // settle into the held steady state (~300ms), then capture.
                // Do NOT jumpDrawablesToCurrentState here -- on a held
                // RippleDrawable that clears the settled state layer instead
                // of finishing it.
                tileRef.postDelayed(new Runnable() {
                    public void run() {
                        capture(job, tileRef, w, h);
                    }
                }, 700);
            }
        });
    }

    private void capture(final Job job, View tile, int w, int h) {
        int[] loc = new int[2];
        tile.getLocationInWindow(loc);
        Rect src = new Rect(loc[0], loc[1], loc[0] + w, loc[1] + h);
        final Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        final String name = job.spec.component + "_" + job.state + "_" + job.appearance + ".png";
        PixelCopy.request(getWindow(), src, bmp, new PixelCopy.OnPixelCopyFinishedListener() {
            public void onPixelCopyFinished(int result) {
                if (result == PixelCopy.SUCCESS) {
                    save(name, bmp);
                } else {
                    Log.e(TAG, "PixelCopy failed (" + result + ") for " + name);
                }
                bmp.recycle();
                runOnUiThread(new Runnable() {
                    public void run() {
                        nextJob();
                    }
                });
            }
        }, new Handler(copyThread.getLooper()));
    }

    private void save(String name, Bitmap bmp) {
        try {
            FileOutputStream fos = new FileOutputStream(new File(outDir, name));
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
            Log.i(TAG, "captured " + name);
        } catch (Throwable t) {
            Log.e(TAG, "save failed " + name, t);
        }
    }

    private void done() {
        try {
            new FileOutputStream(new File(outDir, "DONE")).close();
        } catch (Throwable ignored) {
            // the pull script also accepts the log marker below
        }
        Log.i(TAG, "NATIVEREF:DONE " + (jobIndex) + " captures in " + outDir);
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            public void run() {
                finish();
            }
        }, 500);
    }

    private void runAnimation(String anim, String appearance) {
        final String app = appearance != null ? appearance : "light";
        float dpi = getResources().getDisplayMetrics().densityDpi;
        FrameLayout host = new FrameLayout(this);
        host.setBackgroundColor(0xFF808080);   // the morph frames' flat grey
        setContentView(host);
        if ("tabs".equals(anim)) {
            final View tile = RefWidgets.buildTile(this, "material_tablayout", "normal", app, "",
                    Math.round(60 * dpi / 25.4f), Math.round(16 * dpi / 25.4f));
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    Math.round(60 * dpi / 25.4f), Math.round(16 * dpi / 25.4f),
                    android.view.Gravity.CENTER);
            host.addView(tile, lp);
            final com.google.android.material.tabs.TabLayout tabs =
                    (com.google.android.material.tabs.TabLayout) ((FrameLayout) tile).getChildAt(0);
            final Handler h = new Handler(Looper.getMainLooper());
            Log.i(TAG, "NATIVEREF:ANIMATING tabs " + app);
            h.postDelayed(new Runnable() {
                boolean toLast = true;
                public void run() {
                    tabs.selectTab(tabs.getTabAt(toLast ? tabs.getTabCount() - 1 : 0));
                    toLast = !toLast;
                    h.postDelayed(this, 1400);
                }
            }, 800);
            return;
        }
        // Default: the switch toggle (Material thumb grow + slide + track fill).
        android.view.ContextThemeWrapper ctx = new android.view.ContextThemeWrapper(this,
                "dark".equals(app) ? com.google.android.material.R.style.Theme_Material3_Dark
                        : com.google.android.material.R.style.Theme_Material3_Light);
        final com.google.android.material.materialswitch.MaterialSwitch sw =
                new com.google.android.material.materialswitch.MaterialSwitch(ctx);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                android.view.Gravity.CENTER);
        host.addView(sw, lp);
        final Handler h = new Handler(Looper.getMainLooper());
        Log.i(TAG, "NATIVEREF:ANIMATING switch " + app);
        h.postDelayed(new Runnable() {
            public void run() {
                sw.setChecked(!sw.isChecked());
                h.postDelayed(this, 1200);
            }
        }, 800);
    }

    private static void deleteRecursively(File f) {
        if (f == null || !f.exists()) {
            return;
        }
        File[] kids = f.listFiles();
        if (kids != null) {
            for (File k : kids) {
                deleteRecursively(k);
            }
        }
        f.delete();
    }
}
