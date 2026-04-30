package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.components.StickyHeaderContainer;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Label;
import com.codename1.ui.animations.Motion;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Style;

/// Visualises [com.codename1.components.StickyHeaderContainer] across a scroll
/// sweep that exercises every state: no header pinned, the first header pinned
/// at rest, mid-section scrolling under the pinned header, the push-out
/// transition where the next section's header displaces the current one, and
/// finally a different header pinned at the top. The capture drives the
/// container's scroll position directly and re-paints the host form for each
/// frame.
public class StickyHeaderScreenshotTest extends AbstractAnimationScreenshotTest {
    private static final int SECTION_COUNT = 5;
    private static final int ITEMS_PER_SECTION = 4;
    private static final int[] HEADER_COLORS = {
            0x118ab2, 0xef476f, 0x06d6a0, 0xffd166, 0x8338ec
    };

    private Form host;
    private StickyHeaderContainer sticky;
    private Motion scrollMotion;

    @Override
    public boolean runTest() throws Exception {
        if ("HTML5".equals(Display.getInstance().getPlatformName())) {
            // The JS port truncates the 6-frame composite stream when chunked
            // through console logging, so the reassembled PNG is missing bytes
            // and the screenshot decoder rejects it. Skip on HTML5; iOS,
            // Android and JavaSE still cover the visual contract.
            System.out.println("CN1SS:INFO:test=StickyHeaderScreenshotTest status=SKIPPED reason=js-port-chunk-truncation");
            done();
            return true;
        }
        return super.runTest();
    }

    @Override
    protected int getAnimationDurationMillis() {
        return 900;
    }

    @Override
    protected void prepareCapture(int frameWidth, int frameHeight) {
        super.prepareCapture(frameWidth, frameHeight);
        host = new Form("Sticky Headers");
        host.setLayout(new BorderLayout());
        host.setWidth(frameWidth);
        host.setHeight(frameHeight);
        host.setVisible(true);

        sticky = new StickyHeaderContainer();
        Style ss = sticky.getAllStyles();
        ss.setBgColor(0xf3f4f6);
        ss.setBgTransparency(255);
        ss.setPadding(0, 0, 0, 0);
        ss.setMargin(0, 0, 0, 0);

        Style scrollerStyle = sticky.getScrollContainer().getAllStyles();
        scrollerStyle.setBgColor(0xf3f4f6);
        scrollerStyle.setBgTransparency(255);
        scrollerStyle.setPadding(0, 0, 0, 0);
        scrollerStyle.setMargin(0, 0, 0, 0);

        for (int s = 0; s < SECTION_COUNT; s++) {
            Label header = makeHeader("Section " + (char) ('A' + s), HEADER_COLORS[s]);
            Container content = new Container(BoxLayout.y());
            Style cs = content.getAllStyles();
            cs.setBgColor(0xffffff);
            cs.setBgTransparency(255);
            cs.setPadding(0, 0, 0, 0);
            cs.setMargin(0, 0, 0, 0);
            for (int i = 0; i < ITEMS_PER_SECTION; i++) {
                Label item = new Label(((char) ('A' + s)) + " - row " + (i + 1));
                Style is = item.getAllStyles();
                is.setBgColor(0xffffff);
                is.setBgTransparency(255);
                is.setFgColor(0x111827);
                is.setPadding(10, 10, 14, 14);
                is.setMargin(0, 0, 0, 0);
                content.add(item);
            }
            sticky.addSection(header, content);
        }

        host.add(BorderLayout.CENTER, sticky);
        host.layoutContainer();
        sticky.layoutContainer();
        sticky.getScrollContainer().layoutContainer();

        int contentHeight = sticky.getScrollContainer().getScrollDimension().getHeight();
        int viewportHeight = sticky.getScrollContainer().getHeight();
        int maxScroll = Math.max(0, contentHeight - viewportHeight);
        scrollMotion = Motion.createEaseInOutMotion(0, maxScroll, getAnimationDurationMillis());
        scrollMotion.start();
    }

    private static Label makeHeader(String text, int color) {
        Label l = new Label(text);
        Style s = l.getAllStyles();
        s.setBgColor(color);
        s.setBgTransparency(255);
        s.setFgColor(0xffffff);
        s.setPadding(12, 12, 18, 18);
        s.setMargin(0, 0, 0, 0);
        return l;
    }

    @Override
    protected void renderFrame(Graphics g, int width, int height, double progress, int frameIndex) {
        sticky.setScrollPosition(scrollMotion.getValue());
        host.paintComponent(g, true);
    }

    @Override
    protected void finishCapture() {
        host = null;
        sticky = null;
        scrollMotion = null;
        super.finishCapture();
    }
}
