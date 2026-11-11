package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.scheduler.Gantt;

public class GanttThemeScreenshotTest extends DualAppearanceBaseTest {
    @Override
    protected String baseName() {
        return "GanttTheme";
    }

    @Override
    protected Layout newLayout() {
        return new BorderLayout();
    }

    @Override
    protected void populate(Form form, String suffix) {
        Gantt gantt = new Gantt(SchedulingFixtures.project());
        gantt.set24Hours(true);
        gantt.setCurrentTime(SchedulingFixtures.at(1, 12, 0));
        gantt.setShowCriticalPath(true);
        gantt.setLoadVisible(true);
        form.add(BorderLayout.CENTER, gantt);
    }
}
