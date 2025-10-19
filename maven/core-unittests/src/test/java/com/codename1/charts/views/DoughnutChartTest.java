package com.codename1.charts.views;

import com.codename1.charts.models.MultipleCategorySeries;
import com.codename1.charts.renderers.DefaultRenderer;
import com.codename1.charts.renderers.SimpleSeriesRenderer;
import com.codename1.charts.util.ColorUtil;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DoughnutChartTest {
    private DoughnutChart createChart() {
        MultipleCategorySeries dataset = new MultipleCategorySeries("donut");
        dataset.add("Cat1", new String[]{"A", "B"}, new double[]{1, 2});
        dataset.add("Cat2", new String[]{"C", "D"}, new double[]{3, 4});

        DefaultRenderer renderer = new DefaultRenderer();
        SimpleSeriesRenderer r1 = new SimpleSeriesRenderer();
        r1.setColor(ColorUtil.BLUE);
        SimpleSeriesRenderer r2 = new SimpleSeriesRenderer();
        r2.setColor(ColorUtil.GREEN);
        renderer.addSeriesRenderer(r1);
        renderer.addSeriesRenderer(r2);
        renderer.setFitLegend(true);
        renderer.setChartTitle("Title");
        renderer.setLabelsTextSize(12f);
        renderer.setLegendTextSize(10f);
        renderer.setApplyBackgroundColor(true);
        renderer.setBackgroundColor(ColorUtil.WHITE);
        renderer.setScale(1f);
        return new DoughnutChart(dataset, renderer);
    }

    @Test
    public void testLegendShapeWidthConstant() {
        DoughnutChart chart = createChart();
        assertEquals(10, chart.getLegendShapeWidth(0));
    }
}
