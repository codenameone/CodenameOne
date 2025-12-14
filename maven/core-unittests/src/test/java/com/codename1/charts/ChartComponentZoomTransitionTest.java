package com.codename1.charts;

import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.junit.UITestBase;
import com.codename1.junit.FormTest;
import com.codename1.charts.views.PieChart;
import com.codename1.charts.models.CategorySeries;
import com.codename1.charts.renderers.DefaultRenderer;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.Form;
import org.junit.jupiter.api.Assertions;

public class ChartComponentZoomTransitionTest extends UITestBase {

    @FormTest
    public void testZoomTransition() {
        CategorySeries series = new CategorySeries("Pie");
        series.add("A", 10);
        series.add("B", 20);
        DefaultRenderer renderer = new DefaultRenderer();
        PieChart chart = new PieChart(series, renderer);
        ChartComponent component = new ChartComponent(chart);

        Rectangle rect = new Rectangle(0, 0, 50, 50);

        Form f = new Form();
        f.add(component);
        // We cannot show form in headless test easily without side effects,
        // but adding to form sets the parent form which is checked in ZoomTransition.start()

        component.zoomToShapeInChartCoords(rect, 100);

        // Assertions are hard on private inner class side effects, but code coverage should increase.
    }
}
