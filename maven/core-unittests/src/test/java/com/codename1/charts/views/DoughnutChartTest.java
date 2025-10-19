package com.codename1.charts.views;

import com.codename1.charts.compat.Paint;
import com.codename1.charts.models.MultipleCategorySeries;
import com.codename1.charts.renderers.DefaultRenderer;
import com.codename1.charts.renderers.SimpleSeriesRenderer;
import com.codename1.charts.util.ColorUtil;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    public void testDrawLegendShapeUsesShrinkingStep() throws Exception {
        DoughnutChart chart = createChart();
        Field stepField = DoughnutChart.class.getDeclaredField("mStep");
        stepField.setAccessible(true);
        stepField.setInt(chart, 6);

        ChartTestUtils.RecordingCanvas canvas = ChartTestUtils.allocateInstance(ChartTestUtils.RecordingCanvas.class).prepare();
        chart.drawLegendShape(canvas, new SimpleSeriesRenderer(), 5f, 8f, 0, new Paint());
        assertEquals(1, canvas.circles.size());
        float[] circle = canvas.circles.get(0);
        assertEquals(10f + 5f - 5f, circle[0], 1e-6f); // center x adjusts by SHAPE_WIDTH - new step
        assertEquals(8f, circle[1], 1e-6f);
        assertEquals(5f, circle[2], 1e-6f);
        assertEquals(5, stepField.getInt(chart));
    }

    @Test
    public void testDrawCreatesArcsForEachSlice() {
        DoughnutChart chart = createChart();
        ChartTestUtils.RecordingCanvas canvas = ChartTestUtils.allocateInstance(ChartTestUtils.RecordingCanvas.class).prepare();
        Paint paint = new Paint();
        chart.draw(canvas, 0, 0, 200, 200, paint);
        // Two categories with two entries each -> each category draws 2 slice arcs and one inner arc
        assertEquals(6, canvas.arcs.size());
        assertTrue(canvas.arcs.stream().anyMatch(a -> Math.abs(a.sweepAngle - 360f) < 1e-3));
        assertEquals("Title", canvas.texts.get(0).text);
    }
}
