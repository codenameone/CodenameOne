package com.codenameone.examples.hellocodenameone.tests.charts;

import com.codename1.charts.ChartComponent;
import com.codename1.charts.models.XYMultipleSeriesDataset;
import com.codename1.charts.models.XYSeries;
import com.codename1.charts.renderers.XYMultipleSeriesRenderer;
import com.codename1.charts.renderers.XYSeriesRenderer;
import com.codename1.charts.util.ColorUtil;
import com.codename1.charts.views.AbstractChart;
import com.codename1.charts.views.LineChart;
import com.codename1.ui.Transform;

/// Exercises `ChartComponent.setTransform(Transform)` -- the code path the
/// translation-conjugation refactor in core / iOS / Android / JavaSE
/// directly touches. ChartComponent's transform is documented to operate in
/// component-local coordinates ("origin at (absoluteX, absoluteY)"); this
/// test applies a non-identity scale-around-centre to verify the rendered
/// chart is centred correctly across all four ports.
public class ChartTransformScreenshotTest extends AbstractChartScreenshotTest {

    @Override
    protected AbstractChart buildChart() {
        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
        XYSeries series = new XYSeries("Latency");
        series.add(0, 12);
        series.add(1, 14);
        series.add(2, 19);
        series.add(3, 17);
        series.add(4, 24);
        series.add(5, 22);
        series.add(6, 28);
        dataset.addSeries(series);

        XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
        renderer.setLabelsTextSize(20);
        renderer.setLegendTextSize(20);
        renderer.setMargins(new int[]{36, 60, 24, 24});
        renderer.setShowGrid(true);

        XYSeriesRenderer seriesRenderer = new XYSeriesRenderer();
        seriesRenderer.setColor(ColorUtil.rgb(0x6c, 0x3a, 0xb6));
        seriesRenderer.setLineWidth(3f);
        renderer.addSeriesRenderer(seriesRenderer);

        return new LineChart(dataset, renderer);
    }

    @Override
    protected void configureChartComponent(ChartComponent component) {
        // Scale of 0.7 around the chart-component centre. ChartComponent
        // documents transforms as relative to its (absoluteX, absoluteY)
        // origin, so the centre point we anchor on is in component-local
        // coords (we use the chart's preferred-size centre approximated by
        // the transform's translate). With the platform-side conjugation in
        // Graphics.setTransform plus the matching simplification in
        // ChartComponent.paint (which dropped its own T(absX) * X *
        // T(-absX) compensation), the rendered chart should be a 0.7x
        // scaled copy of the untransformed test, centred on the component.
        Transform t = Transform.makeIdentity();
        // Anchor scale at component-local (250, 400). We don't know the
        // component's actual size here -- the screenshot dimensions are
        // platform-dependent -- but ChartComponent uses BorderLayout.CENTER
        // so it fills the form, and a fixed anchor at (250, 400) gives a
        // deterministic scaled output once the component is laid out.
        t.translate(250f, 400f);
        t.scale(0.7f, 0.7f);
        t.translate(-250f, -400f);
        component.setTransform(t);
    }

    @Override
    protected String screenshotName() {
        return "chart-transform";
    }
}
