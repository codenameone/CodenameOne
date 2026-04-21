package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.Sheet;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.util.UITimer;

/// Pure-Java isolation reproduction of the Sheet panel-missing-bg bug.
/// Shows a plain Sheet with two labels inside and dumps the resolved Style
/// (bgColor, bgTransparency, backgroundType, border class) via
/// System.out so we can see whether the theme's Sheet UIID is actually
/// assigning a non-zero bgTransparency and a RoundRectBorder on the JS port.
/// If it isn't, the RoundRectBorder.paintBorderBackground fillShape branch
/// never fires and the Sheet paints transparent over the 30% black backdrop.
public class SheetIsolationScreenshotTest extends BaseTest {
    @Override
    public boolean runTest() {
        Form host = new Form("Sheet Host", BoxLayout.y()) {
            @Override
            protected void onShowCompleted() {
                Sheet s = new Sheet(null, "Details");
                s.getContentPane().add(new Label("Sheet content"));
                s.getContentPane().add(new Button("Primary Action"));

                Style unsel = s.getUnselectedStyle();
                com.codename1.ui.plaf.Border b = unsel.getBorder();
                System.out.println("CN1SS:DIAG:sheet-isolation"
                        + " uiid=" + s.getUIID()
                        + " bgColor=" + Integer.toHexString(unsel.getBgColor())
                        + " bgTransparency=" + (unsel.getBgTransparency() & 0xff)
                        + " bgType=" + unsel.getBackgroundType()
                        + " bgImage=" + (unsel.getBgImage() == null ? "null" : "set")
                        + " borderClass=" + (b == null ? "null" : b.getClass().getName())
                        + " padT=" + unsel.getPaddingTop()
                        + " padB=" + unsel.getPaddingBottom()
                        + " padL=" + unsel.getPaddingLeftNoRTL()
                        + " padR=" + unsel.getPaddingRightNoRTL());

                s.show(0);
                // Give the sheet a moment to settle, then screenshot.
                UITimer.timer(400, false, this, () -> {
                    Cn1ssDeviceRunnerHelper.emitCurrentFormScreenshot("sheet-isolation", SheetIsolationScreenshotTest.this::done);
                });
            }
        };
        host.add(new Label("(sheet should slide up over this)"));
        host.show();
        return true;
    }
}
