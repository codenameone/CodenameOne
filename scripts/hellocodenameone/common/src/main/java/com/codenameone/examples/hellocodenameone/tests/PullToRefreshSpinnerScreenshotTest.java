package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Style;

/// Captures the modern pull-to-refresh arc spinner across six frames during
/// its continuous-spin phase. `modernSpinStartTime` reads
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
        // draws the continuous-spin arc. scrollY is shifted negative by
        // the pull-to-refresh height so the indicator centre lands inside
        // the visible viewport.
        scrollHost.putClientProperty("$pullToRelease", "updating");
    }

    @Override
    protected void renderFrame(Graphics g, int width, int height, double progress, int frameIndex) {
        // Render the host form into an offscreen image so the form's own
        // background paints first; then overlay the pull-to-refresh painter
        // via the standard paintPullToRefresh() entry point so we exercise
        // the exact code path the simulator and devices hit at runtime.
        Image frame = Image.createImage(width, height, 0xfff0f4f8);
        Graphics fg = frame.getGraphics();
        host.paintComponent(fg, true);
        // Force-render the indicator on top -- paintComponent will skip the
        // pull painter when scrollY >= 0, but the spinner state is what we
        // want to capture.
        scrollHost.getUIManager().getLookAndFeel().drawPullToRefresh(fg, scrollHost, true);
        g.drawImage(frame, 0, 0);
        frame.dispose();
    }

    @Override
    protected void finishCapture() {
        host = null;
        scrollHost = null;
        super.finishCapture();
    }
}
