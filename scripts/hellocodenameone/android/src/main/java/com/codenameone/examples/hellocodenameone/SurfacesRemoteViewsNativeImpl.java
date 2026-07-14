package com.codenameone.examples.hellocodenameone;

import android.content.Context;
import android.content.res.Configuration;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RemoteViews;

import com.codename1.impl.android.AndroidImplementation;
import com.codename1.impl.android.AndroidNativeUtil;
import com.codename1.impl.android.surfaces.CN1SurfaceRenderer;
import com.codename1.impl.android.surfaces.CN1SurfaceStore;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Android implementation of {@link SurfacesRemoteViewsNative}: exercises the REAL widget
 * lowering in-process. The timeline is read back from the same CN1SurfaceStore file the
 * home-screen provider reads, rendered through CN1SurfaceRenderer into RemoteViews exactly
 * like CN1WidgetProvider does, and applied to real views (RemoteViews.apply) so the full
 * inflation path of the pre-baked cn1_surface_* layout resources runs. Light and dark render
 * through createConfigurationContext so both color resolutions are on screen at once,
 * mirroring the light+dark tiles of SurfacesRasterizerScreenshotTest.
 */
public class SurfacesRemoteViewsNativeImpl {
    private static final String TAG = "CN1SS";

    public View createWidgetView(final String kindId, final int widthPx, final int heightPx) {
        final View[] result = new View[1];
        // Views inflate on the UI thread like every other peer the port creates.
        AndroidImplementation.runOnUiThreadAndBlock(new Runnable() {
            public void run() {
                try {
                    result[0] = buildWidgetColumn(kindId, widthPx, heightPx);
                } catch (Throwable t) {
                    android.util.Log.e(TAG, "SurfacesRemoteViews native render failed", t);
                }
            }
        });
        return result[0];
    }

    public String probeTimerRender(final String timelineJson) {
        final String[] result = new String[1];
        AndroidImplementation.runOnUiThreadAndBlock(new Runnable() {
            public void run() {
                try {
                    Context ctx = AndroidNativeUtil.getContext();
                    JSONObject doc = new JSONObject(timelineJson);
                    JSONObject layout = pickLayout(doc);
                    JSONObject state = pickState(doc);
                    if (layout == null) {
                        result[0] = "error:no layout in timeline JSON";
                        return;
                    }
                    RemoteViews rv = CN1SurfaceRenderer.render(ctx, layout, state,
                            "cn1ss_probe", null);
                    if (rv == null) {
                        result[0] = "error:render returned null";
                        return;
                    }
                    // apply too: the Chronometer lowering only fully executes on inflation
                    View v = rv.apply(ctx, new FrameLayout(ctx));
                    result[0] = v == null ? "error:apply returned null" : "ok";
                } catch (Throwable t) {
                    result[0] = "error:" + t;
                }
            }
        });
        return result[0];
    }

    public boolean isSupported() {
        return true;
    }

    private View buildWidgetColumn(String kindId, int widthPx, int heightPx) throws Exception {
        Context ctx = AndroidNativeUtil.getContext();
        String json = CN1SurfaceStore.readWidgetTimeline(ctx, kindId);
        if (json == null) {
            android.util.Log.e(TAG, "No published timeline for kind " + kindId);
            return null;
        }
        JSONObject doc = new JSONObject(json);
        JSONObject layout = pickLayout(doc);
        JSONObject state = pickState(doc);
        if (layout == null) {
            android.util.Log.e(TAG, "No layout in published timeline for kind " + kindId);
            return null;
        }
        java.io.File imagesDir = CN1SurfaceStore.kindDir(ctx, kindId);
        LinearLayout column = new LinearLayout(ctx);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER);
        // the same neutral backdrop the rasterizer screenshot uses
        column.setBackgroundColor(0xff606060);
        int gap = Math.max(8, heightPx / 20);
        boolean[] darks = {false, true};
        for (boolean dark : darks) {
            Context themed = themedContext(ctx, dark);
            RemoteViews rv = CN1SurfaceRenderer.render(themed, layout, state, kindId,
                    imagesDir);
            FrameLayout host = new FrameLayout(themed);
            View widget = rv.apply(themed, host);
            host.addView(widget, new FrameLayout.LayoutParams(widthPx, heightPx));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(widthPx, heightPx);
            if (dark) {
                lp.topMargin = gap;
            }
            column.addView(host, lp);
        }
        return column;
    }

    /** A context whose uiMode night flag is forced, so color resolution picks that palette. */
    private static Context themedContext(Context ctx, boolean dark) {
        Configuration config = new Configuration(ctx.getResources().getConfiguration());
        config.uiMode = (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK)
                | (dark ? Configuration.UI_MODE_NIGHT_YES : Configuration.UI_MODE_NIGHT_NO);
        return ctx.createConfigurationContext(config);
    }

    /** Mirrors CN1WidgetProvider's bucket selection for the suite's medium-sized render. */
    private static JSONObject pickLayout(JSONObject doc) {
        JSONObject layouts = doc.optJSONObject("layouts");
        if (layouts == null) {
            return null;
        }
        JSONObject layout = layouts.optJSONObject("medium");
        if (layout == null) {
            layout = layouts.optJSONObject("default");
        }
        if (layout == null && layouts.keys().hasNext()) {
            layout = layouts.optJSONObject((String) layouts.keys().next());
        }
        return layout;
    }

    /** The first entry's state: the suite publishes a single already-active entry. */
    private static JSONObject pickState(JSONObject doc) {
        JSONArray entries = doc.optJSONArray("entries");
        if (entries == null || entries.length() == 0) {
            return null;
        }
        JSONObject entry = entries.optJSONObject(0);
        return entry == null ? null : entry.optJSONObject("state");
    }
}
