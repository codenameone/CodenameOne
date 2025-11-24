package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Component;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;

abstract class AbstractGraphicsScreenshotTest extends BaseTest {
    protected abstract Component createContent();

    protected abstract String screenshotName();

    @Override
    public boolean runTest() throws Exception {
        Form form = createForm("Graphics", new BorderLayout(), screenshotName());
        form.add(BorderLayout.CENTER, createContent());
        form.show();
        return true;
    }
}
