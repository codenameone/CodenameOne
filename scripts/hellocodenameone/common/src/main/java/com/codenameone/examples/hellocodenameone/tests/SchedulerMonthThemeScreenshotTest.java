package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.scheduler.Scheduler;

public class SchedulerMonthThemeScreenshotTest extends DualAppearanceBaseTest {
    @Override
    protected String baseName() {
        return "SchedulerMonthTheme";
    }

    @Override
    protected Layout newLayout() {
        return new BorderLayout();
    }

    @Override
    protected void populate(Form form, String suffix) {
        form.add(BorderLayout.CENTER, SchedulingFixtures.scheduler(Scheduler.View.MONTH));
    }
}
