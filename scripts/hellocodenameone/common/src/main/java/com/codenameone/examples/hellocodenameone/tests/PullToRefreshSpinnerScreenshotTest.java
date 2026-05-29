package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;

import java.util.Hashtable;

/// Captures the modern pull-to-refresh arc spinner across six frames during
/// its continuous-spin phase. The painter only takes the modern path when
/// `pullToRefreshModernBool` is true, so the test layers that constant onto
/// the active theme (the same flag the new iOS / Android native themes set
/// by default). `modernSpinStartTime` reads
/// [com.codename1.ui.animations.AnimationTime] (which the harness advances
/// per frame), so each cell renders the arc at a different rotation angle.
public class PullToRefreshSpinnerScreenshotTest extends AbstractAnimationScreenshotTest {
    private Form host;
    private Container scrollHost;

    @Override
    protected int getAnimationDurationMillis() {
        // One full ~360 deg/sec sweep = 2000ms (startAngle ticks elapsed/2).
        return 2000;
    }

    @Override
    protected void prepareCapture(int frameWidth, int frameHeight) {
        super.prepareCapture(frameWidth, frameHeight);

        // Overlay the modern theme constants so DefaultLookAndFeel
        // picks the arc-spinner path. `addThemeProps` keeps the rest
        // of the theme untouched and matches what the iOS Modern /
        // Android Material native themes ship by default.
        Hashtable<String, Object> overlay = new Hashtable<String, Object>();
        overlay.put("@pullToRefreshModernBool", "true");
        UIManager.getInstance().addThemeProps(overlay);

        host = new Form("PullToRefresh", new BorderLayout());
        host.setWidth(frameWidth);
        host.setHeight(frameHeight);
        host.setVisible(true);
        Style cps = host.getContentPane().getAllStyles();
        cps.setBgColor(0xf0f4f8);
        cps.setBgTransparency(255);

        scrollHost = new Container(BoxLayout.y());
        scrollHost.setScrollableY(true);
        scrollHost.addPullToRefresh(new Runnable() {
            @Override
            public void run() {
                // never invoked by the test
            }
        });
        for (int i = 0; i < 12; i++) {
            scrollHost.add(new Label("Row " + i));
        }
        host.add(BorderLayout.CENTER, scrollHost);
        host.layoutContainer();

        // Pin the container in the "task running" state so the painter
        // draws the continuous-spin arc.
        scrollHost.putClientProperty("$pullToRelease", "updating");
    }

    @Override
    protected void renderFrame(Graphics g, int width, int height, double progress, int frameIndex) {
        // The host form's paint chain renders the modern arc spinner
        // because the container carries `$pullToRelease=updating`. A
        // second explicit drawPullToRefresh call would stack a duplicate
        // indicator at a different y (the painter offsets by the host's
        // title-bar height during the regular paint and skips that offset
        // when called directly), so leave the form to render it once.
        host.paintComponent(g, true);
    }

    @Override
    protected void finishCapture() {
        // Reset the theme so the next test in the suite isn't carrying
        // the modern flag.
        UIManager.getInstance().refreshTheme();
        host = null;
        scrollHost = null;
        super.finishCapture();
    }
}
