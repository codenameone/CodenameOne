/// Copyright (C) 2009 - 2013 SC 4ViewSoft SRL
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
/// http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
package com.codename1.charts.views;

import com.codename1.charts.compat.Canvas;
import com.codename1.charts.compat.Paint;
import com.codename1.charts.compat.Paint.Style;
import com.codename1.charts.models.XYMultipleSeriesDataset;
import com.codename1.charts.models.XYValueSeries;
import com.codename1.charts.renderers.SimpleSeriesRenderer;
import com.codename1.charts.renderers.XYMultipleSeriesRenderer;
import com.codename1.charts.renderers.XYSeriesRenderer;

import java.util.List;


/// Displays series of `XYValueSeries` entries as proportional bubbles on
/// top of an `XYChart` plot.
///
/// Combine the chart with an `XYMultipleSeriesDataset` that contains one
/// or more `XYValueSeries` instances and supply an
/// `XYMultipleSeriesRenderer`. The resulting `BubbleChart` can be
/// wrapped in a `com.codename1.charts.ChartComponent` to embed it inside a
/// Codename One UI.
public class BubbleChart extends XYChart {
    /// The constant to identify this chart type.
    public static final String TYPE = "Bubble";
    /// The legend shape width.
    private static final int SHAPE_WIDTH = 10;
    /// The minimum bubble size.
    private static final int MIN_BUBBLE_SIZE = 2;
    /// The maximum bubble size.
    private static final int MAX_BUBBLE_SIZE = 20;

    BubbleChart() {
    }

    /// Builds a new bubble chart instance.
    ///
    /// #### Parameters
    ///
    /// - `dataset`: the multiple series dataset
    ///
    /// - `renderer`: the multiple series renderer
    public BubbleChart(XYMultipleSeriesDataset dataset, XYMultipleSeriesRenderer renderer) {
        super(dataset, renderer);
    }

    /// The graphical representation of a series.
    ///
    /// #### Parameters
    ///
    /// - `canvas`: the canvas to paint to
    ///
    /// - `paint`: the paint to be used for drawing
    ///
    /// - `points`: the array of points to be used for drawing the series
    ///
    /// - `renderer`: the series renderer
    ///
    /// - `yAxisValue`: the minimum value of the y-axis
    ///
    /// - `seriesIndex`: the index of the series currently being drawn
    ///
    /// - `startIndex`: the start index of the rendering points
    @Override
    public void drawSeries(Canvas canvas, Paint paint, List<Float> points,
                           XYSeriesRenderer renderer, float yAxisValue, int seriesIndex, int startIndex) {
        paint.setColor(renderer.getColor());
        paint.setStyle(Style.FILL);
        int length = points.size();
        XYValueSeries series = (XYValueSeries) mDataset.getSeriesAt(seriesIndex);
        double max = series.getMaxValue();
        double coef = MAX_BUBBLE_SIZE / max;
        for (int i = 0; i < length; i += 2) {
            double size = series.getValue(startIndex + i / 2) * coef + MIN_BUBBLE_SIZE;
            drawCircle(canvas, paint, points.get(i), points.get(i + 1), (float) size);
        }
    }

    @Override
    protected ClickableArea[] clickableAreasForPoints(List<Float> points, List<Double> values,
                                                      float yAxisValue, int seriesIndex, int startIndex) {
        int length = points.size();
        XYValueSeries series = (XYValueSeries) mDataset.getSeriesAt(seriesIndex);
        double max = series.getMaxValue();
        double coef = MAX_BUBBLE_SIZE / max;
        ClickableArea[] ret = new ClickableArea[length / 2];
        for (int i = 0; i < length; i += 2) {
            double size = series.getValue(startIndex + i / 2) * coef + MIN_BUBBLE_SIZE;
            ret[i / 2] = new ClickableArea(PkgUtils.makeRect(points.get(i) - (float) size, points.get(i + 1)
                    - (float) size, points.get(i) + (float) size, points.get(i + 1) + (float) size),
                    values.get(i), values.get(i + 1));
        }
        return ret;
    }

    /// Returns the legend shape width.
    ///
    /// #### Parameters
    ///
    /// - `seriesIndex`: the series index
    ///
    /// #### Returns
    ///
    /// the legend shape width
    @Override
    public int getLegendShapeWidth(int seriesIndex) {
        return SHAPE_WIDTH;
    }

    /// The graphical representation of the legend shape.
    ///
    /// #### Parameters
    ///
    /// - `canvas`: the canvas to paint to
    ///
    /// - `renderer`: the series renderer
    ///
    /// - `x`: the x value of the point the shape should be drawn at
    ///
    /// - `y`: the y value of the point the shape should be drawn at
    ///
    /// - `seriesIndex`: the series index
    ///
    /// - `paint`: the paint to be used for drawing
    @Override
    public void drawLegendShape(Canvas canvas, SimpleSeriesRenderer renderer, float x, float y,
                                int seriesIndex, Paint paint) {
        paint.setStyle(Style.FILL);
        drawCircle(canvas, paint, x + SHAPE_WIDTH, y, 3);
    }

    /// The graphical representation of a circle point shape.
    ///
    /// #### Parameters
    ///
    /// - `canvas`: the canvas to paint to
    ///
    /// - `paint`: the paint to be used for drawing
    ///
    /// - `x`: the x value of the point the shape should be drawn at
    ///
    /// - `y`: the y value of the point the shape should be drawn at
    ///
    /// - `radius`: the bubble radius
    private void drawCircle(Canvas canvas, Paint paint, float x, float y, float radius) {
        canvas.drawCircle(x, y, radius, paint);
    }

    /// Returns the chart type identifier.
    ///
    /// #### Returns
    ///
    /// the chart type
    @Override
    public String getChartType() {
        return TYPE;
    }

}
