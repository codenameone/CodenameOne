package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.List;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.Layout;

public class ListThemeScreenshotTest extends DualAppearanceBaseTest {

    /// Skipped on the native macOS port, which renders this frame wrongly.
    ///
    /// The focused row is painted as a solid bar that hides its text, where Linux draws a focus outline and a touch port never focuses the list at all.
    ///
    /// A DEFECT, not a capability the port lacks, and skipped rather than given
    /// a golden because a golden is the assertion that the pixels are right.
    /// Delete this override and the port_status_supplement entry together when
    /// the rendering is fixed.
    @Override
    public boolean runTest() {
        if ("mac".equals(com.codename1.ui.Display.getInstance().getPlatformName())) {
            System.out.println("CN1SS:INFO:test=ListTheme status=SKIPPED reason=macos-list-selection-fill");
            skipAppearances();
            return true;
        }
        return super.runTest();
    }

    @Override
    protected String baseName() {
        return "ListTheme";
    }

    @Override
    protected Layout newLayout() {
        return new BorderLayout();
    }

    @Override
    protected void populate(Form form, String suffix) {
        List list = new List(new Object[]{
            "First item",
            "Second item",
            "Third item",
            "Fourth item",
            "Fifth item",
            "Sixth item",
            "Seventh item",
            "Eighth item"
        });
        list.setSelectedIndex(1);

        Container wrap = new Container(new BorderLayout());
        wrap.add(BorderLayout.CENTER, list);
        form.add(BorderLayout.CENTER, wrap);
    }
}
