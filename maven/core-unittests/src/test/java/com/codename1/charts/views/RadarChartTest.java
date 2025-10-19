package com.codename1.charts.views;

import com.codename1.charts.compat.Paint;
import com.codename1.charts.models.AreaSeries;
import com.codename1.charts.models.CategorySeries;
import com.codename1.charts.renderers.DefaultRenderer;
import com.codename1.charts.renderers.SimpleSeriesRenderer;
import com.codename1.charts.util.ColorUtil;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RadarChartTest {
    private RadarChart createChart(AreaSeries dataset) {
        DefaultRenderer renderer = new DefaultRenderer();
        renderer.setLabelsTextSize(12f);
        renderer.setLegendTextSize(10f);
        renderer.setStartAngle(0f);
        renderer.setApplyBackgroundColor(true);
        renderer.setBackgroundColor(ColorUtil.WHITE);
        renderer.setScale(1f);

        int seriesCount = dataset.getSeriesCount();
        for (int i = 0; i < Math.max(seriesCount, 1); i++) {
            SimpleSeriesRenderer rendererEntry = new SimpleSeriesRenderer();
            rendererEntry.setColor(ColorUtil.argb(255, 50 * (i + 1), 100, 150));
            renderer.addSeriesRenderer(rendererEntry);
        }
        return new RadarChart(dataset, renderer);
    }

    private AreaSeries createDataset(int categories, int seriesCount) {
        AreaSeries area = new AreaSeries();
        for (int s = 0; s < seriesCount; s++) {
            CategorySeries series = new CategorySeries("Series" + s);
            for (int c = 0; c < categories; c++) {
                series.add("C" + c, 0.2 + 0.1 * (s + c));
            }
            area.addSeries(series);
        }
        return area;
    }

    @Test
    public void testLegendShapeWidthConstant() {
        RadarChart chart = createChart(createDataset(3, 1));
        assertEquals(10, chart.getLegendShapeWidth(0));
    }

    @Test
    public void testDrawLegendShapeUsesCurrentStepBeforeDecrement() throws Exception {
        RadarChart chart = createChart(createDataset(3, 1));
        Field stepField = RadarChart.class.getDeclaredField("mStep");
        stepField.setAccessible(true);
        stepField.setInt(chart, 4);
        ChartTestUtils.RecordingCanvas canvas = ChartTestUtils.allocateInstance(ChartTestUtils.RecordingCanvas.class).prepare();
        chart.drawLegendShape(canvas, new SimpleSeriesRenderer(), 6f, 9f, 0, new Paint());
        assertEquals(1, canvas.circles.size());
        float[] circle = canvas.circles.get(0);
        assertEquals(10f + 6f - 4f, circle[0], 1e-6f);
        assertEquals(9f, circle[1], 1e-6f);
        assertEquals(4f, circle[2], 1e-6f);
        assertEquals(3, stepField.getInt(chart));
    }

    @Test
    public void testDrawWithInsufficientCategoriesDoesNothing() {
        RadarChart chart = createChart(createDataset(2, 1));
        ChartTestUtils.RecordingCanvas canvas = ChartTestUtils.allocateInstance(ChartTestUtils.RecordingCanvas.class).prepare();
        chart.draw(canvas, 0, 0, 200, 200, new Paint());
        assertTrue(canvas.lines.isEmpty());
        assertTrue(canvas.arcs.isEmpty());
    }

    @Test
    public void testDrawProducesWebAndArea() {
        RadarChart chart = createChart(createDataset(4, 2));
        ChartTestUtils.RecordingCanvas canvas = ChartTestUtils.allocateInstance(ChartTestUtils.RecordingCanvas.class).prepare();
        chart.draw(canvas, 0, 0, 220, 220, new Paint());
        assertTrue(canvas.lines.size() > 0);
        assertTrue(canvas.texts.size() >= 4);
    }
}
