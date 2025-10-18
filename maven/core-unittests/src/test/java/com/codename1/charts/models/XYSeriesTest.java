package com.codename1.charts.models;

import com.codename1.charts.util.MathHelper;
import org.junit.jupiter.api.Test;

import java.util.SortedMap;

import static org.junit.jupiter.api.Assertions.*;

class XYSeriesTest {
    @Test
    void addUpdatesRangeAndValues() {
        XYSeries series = new XYSeries("s");
        series.add(1.0, 2.0);
        series.add(3.0, 4.0);

        assertEquals(2, series.getItemCount());
        assertEquals(1.0, series.getMinX());
        assertEquals(3.0, series.getMaxX());
        assertEquals(2.0, series.getY(0));
        assertEquals(4.0, series.getY(1));
    }

    @Test
    void duplicateXValuesAreOffsetToPreserveOrder() {
        XYSeries series = new XYSeries("s");
        series.add(1.0, 2.0);
        series.add(1.0, 3.0);

        double first = series.getX(0);
        double second = series.getX(1);
        assertTrue(second > first);
    }

    @Test
    void removeRecalculatesRangeWhenRemovingExtremes() {
        XYSeries series = new XYSeries("s");
        series.add(1.0, 5.0);
        series.add(5.0, -1.0);
        series.add(3.0, 4.0);

        series.remove(0);

        assertEquals(3.0, series.getMinX());
        assertEquals(5.0, series.getMaxX());
        assertEquals(-1.0, series.getMinY());
        assertEquals(4.0, series.getMaxY());
    }

    @Test
    void clearMethodsResetState() {
        XYSeries series = new XYSeries("s");
        series.add(1.0, 2.0);
        series.add(2.0, 3.0);
        series.addAnnotation("a", 1.0, 2.0);

        series.clearSeriesValues();
        assertEquals(0, series.getItemCount());
        assertEquals(MathHelper.NULL_VALUE, series.getMinX());
        assertEquals(1, series.getAnnotationCount());

        series.clear();
        assertEquals(0, series.getAnnotationCount());
        assertEquals(MathHelper.NULL_VALUE, series.getMinX());
    }

    @Test
    void annotationLifecycleIsTracked() {
        XYSeries series = new XYSeries("s");
        series.addAnnotation("first", 0, 1.0, 2.0);
        series.addAnnotation("second", 1, 2.0, 3.0);

        assertEquals(2, series.getAnnotationCount());
        assertEquals("first", series.getAnnotationAt(0));
        assertEquals(2.0, series.getAnnotationX(1));
        assertEquals(3.0, series.getAnnotationY(1));

        series.removeAnnotation(0);
        assertEquals(1, series.getAnnotationCount());
        assertEquals("second", series.getAnnotationAt(0));
    }

    @Test
    void getRangeSupportsBeforeAfterPoints() {
        XYSeries series = new XYSeries("s");
        series.add(1.0, 1.0);
        series.add(2.0, 2.0);
        series.add(3.0, 3.0);
        series.add(4.0, 4.0);

        SortedMap<Double, Double> regularRange = series.getRange(1.5, 3.5, false);
        assertEquals(2, regularRange.size());
        assertTrue(regularRange.containsKey(2.0));
        assertTrue(regularRange.containsKey(3.0));

        SortedMap<Double, Double> expandedRange = series.getRange(2.0, 3.0, true);
        assertEquals(3, expandedRange.size());
        assertTrue(expandedRange.containsKey(1.0));
        assertTrue(expandedRange.containsKey(2.0));
        assertTrue(expandedRange.containsKey(3.0));
    }

    @Test
    void getIndexForKeyUsesSortedOrder() {
        XYSeries series = new XYSeries("s");
        series.add(5.0, 1.0);
        series.add(1.0, 2.0);
        series.add(3.0, 3.0);

        assertEquals(0, series.getIndexForKey(1.0));
        assertEquals(1, series.getIndexForKey(3.0));
        assertEquals(2, series.getIndexForKey(5.0));
    }
}
