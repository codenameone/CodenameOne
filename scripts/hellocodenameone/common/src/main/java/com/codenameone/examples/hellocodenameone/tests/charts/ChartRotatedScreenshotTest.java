package com.codenameone.examples.hellocodenameone.tests.charts;

import com.codename1.charts.ChartComponent;
import com.codename1.charts.models.CategorySeries;
import com.codename1.charts.renderers.DefaultRenderer;
import com.codename1.charts.renderers.SimpleSeriesRenderer;
import com.codename1.charts.util.ColorUtil;
import com.codename1.charts.views.AbstractChart;
import com.codename1.charts.views.PieChart;
import com.codename1.ui.Transform;

/// Pie chart with a 30 degree rotation applied via ChartComponent.setTransform.
/// Rotation is the transformation most sensitive to the
/// `xTranslate`-conjugation: without conjugation the rotation would happen
/// around the screen origin (0, 0) instead of the component centre,
/// producing a wildly translated chart instead of a rotated one.
public class ChartRotatedScreenshotTest extends AbstractChartScreenshotTest {

    @Override
    protected AbstractChart buildChart() {
        CategorySeries series = new CategorySeries("Slices");
        series.add("Alpha", 30);
        series.add("Beta", 25);
        series.add("Gamma", 20);
        series.add("Delta", 15);
        series.add("Epsilon", 10);

        DefaultRenderer renderer = new DefaultRenderer();
        renderer.setLabelsTextSize(20);
        renderer.setLegendTextSize(20);
        renderer.setShowLabels(true);
        renderer.setLabelsColor(ColorUtil.BLACK);

        int[] colors = new int[]{
                ColorUtil.rgb(0xb8, 0x40, 0xa6),
                ColorUtil.rgb(0xee, 0x4a, 0x4a),
                ColorUtil.rgb(0xf2, 0xb1, 0x40),
                ColorUtil.rgb(0x4d, 0xc6, 0x8f),
                ColorUtil.rgb(0x47, 0xa1, 0xe0)
        };
        for (int color : colors) {
            SimpleSeriesRenderer r = new SimpleSeriesRenderer();
            r.setColor(color);
            renderer.addSeriesRenderer(r);
        }
        return new PieChart(series, renderer);
    }

    @Override
    protected void configureChartComponent(ChartComponent component) {
        // 30 degree rotation around component-local (250, 400). On the
        // pre-conjugation iOS Metal port the same rotation applied to
        // xTranslate-shifted vertex coordinates would rotate the chart
        // around the screen origin instead, throwing the visible pie
        // outside the screen entirely. With the new uniform conjugation
        // this rotates around the component-local anchor on every port.
        Transform t = Transform.makeIdentity();
        t.rotate((float) (Math.PI / 6.0), 250f, 400f);
        component.setTransform(t);
    }

    @Override
    protected String screenshotName() {
        return "chart-rotated-pie";
    }
}
