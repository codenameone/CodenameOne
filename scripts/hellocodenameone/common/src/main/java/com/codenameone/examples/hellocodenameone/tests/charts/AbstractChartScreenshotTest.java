package com.codenameone.examples.hellocodenameone.tests.charts;

import com.codename1.charts.ChartComponent;
import com.codename1.charts.views.AbstractChart;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.hellocodenameone.tests.BaseTest;

/// Shared scaffolding for chart screenshot tests. Each subclass returns the
/// `ChartComponent` it wants captured; this class wraps it in a deterministic
/// form so the rendered pixels are reproducible across iOS / Android / JavaSE
/// / JS pipelines and the chart-package render path -- which leans heavily
/// on `Graphics.setTransform` for the chart-coords-to-screen-coords mapping
/// -- has visual coverage.
abstract class AbstractChartScreenshotTest extends BaseTest {

    protected abstract AbstractChart buildChart();

    protected abstract String screenshotName();

    /// Subclasses can override to apply pan / zoom / setTransform configuration
    /// to the wrapping ChartComponent before it lands on the form. Default is a
    /// no-op (untransformed default rendering, which is the most common path).
    protected void configureChartComponent(ChartComponent component) {
    }

    @Override
    public boolean runTest() throws Exception {
        Form form = createForm(screenshotName(), new BorderLayout(), screenshotName());
        ChartComponent component = new ChartComponent(buildChart());
        configureChartComponent(component);
        form.add(BorderLayout.CENTER, component);
        form.show();
        return true;
    }
}
