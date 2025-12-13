package com.codename1.charts.views;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.charts.models.CategorySeries;
import com.codename1.charts.renderers.DialRenderer;
import com.codename1.charts.compat.Canvas;
import com.codename1.charts.compat.Paint;
import com.codename1.ui.Image;
import com.codename1.ui.Graphics;
import org.junit.jupiter.api.Assertions;

public class DialChartTest extends UITestBase {

    @FormTest
    public void testDialChart() {
        CategorySeries dataset = new CategorySeries("Dial");
        dataset.add("Speed", 80);

        DialRenderer renderer = new DialRenderer();
        DialChart chart = new DialChart(dataset, renderer);

        Image img = Image.createImage(200, 200, 0xFFFFFFFF);
        Graphics g = img.getGraphics();
        Canvas canvas = new Canvas();
        canvas.g = g;
        Paint paint = new Paint();

        chart.draw(canvas, 0, 0, 200, 200, paint);
    }
}
