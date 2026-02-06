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
import com.codename1.charts.models.XYSeries;
import com.codename1.charts.renderers.XYMultipleSeriesRenderer;
import com.codename1.charts.renderers.XYSeriesRenderer;

import java.util.List;


/// Extends `BarChart` to support values that represent ranges rather than
/// single points. Each bar is drawn from the lower value to the upper value in
/// the dataset.
///
/// Use this chart with an `XYMultipleSeriesDataset` containing
/// `XYSeries` instances where consecutive entries form the minimum/maximum
/// pair for a category.
public class RangeBarChart extends BarChart {
    /// The chart type.
    public static final String TYPE = "RangeBar";

    RangeBarChart() {
    }

    RangeBarChart(Type type) {
        super(type);
    }

    /// Builds a new range bar chart instance.
    ///
    /// #### Parameters
    ///
    /// - `dataset`: the multiple series dataset
    ///
    /// - `renderer`: the multiple series renderer
    ///
    /// - `type`: the range bar chart type
    public RangeBarChart(XYMultipleSeriesDataset dataset, XYMultipleSeriesRenderer renderer, Type type) {
        super(dataset, renderer, type);
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
    /// - `seriesRenderer`: the series renderer
    ///
    /// - `yAxisValue`: the minimum value of the y axis
    ///
    /// - `seriesIndex`: the index of the series currently being drawn
    ///
    /// - `startIndex`: the start index of the rendering points
    @Override
    public void drawSeries(Canvas canvas, Paint paint, List<Float> points,
                           XYSeriesRenderer seriesRenderer, float yAxisValue, int seriesIndex, int startIndex) {
        int seriesNr = mDataset.getSeriesCount();
        int length = points.size();
        paint.setColor(seriesRenderer.getColor());
        paint.setStyle(Style.FILL);
        float halfDiffX = getHalfDiffX(points, length, seriesNr);
        int start = 0;
        if (startIndex > 0) {
            start = 2;
        }
        for (int i = start; i < length; i += 4) {
            if (points.size() > i + 3) {
                float xMin = points.get(i);
                float yMin = points.get(i + 1);
                // xMin = xMax
                float xMax = points.get(i + 2);
                float yMax = points.get(i + 3);
                drawBar(canvas, xMin, yMin, xMax, yMax, halfDiffX, seriesNr, seriesIndex, paint);
            }
        }
        paint.setColor(seriesRenderer.getColor());
    }

    /// The graphical representation of the series values as text.
    ///
    /// #### Parameters
    ///
    /// - `canvas`: the canvas to paint to
    ///
    /// - `series`: the series to be painted
    ///
    /// - `renderer`: the series renderer
    ///
    /// - `paint`: the paint to be used for drawing
    ///
    /// - `points`: the array of points to be used for drawing the series
    ///
    /// - `seriesIndex`: the index of the series currently being drawn
    ///
    /// - `startIndex`: the start index of the rendering points
    @Override
    protected void drawChartValuesText(Canvas canvas, XYSeries series, XYSeriesRenderer renderer,
                                       Paint paint, List<Float> points, int seriesIndex, int startIndex) {
        int seriesNr = mDataset.getSeriesCount();
        float halfDiffX = getHalfDiffX(points, points.size(), seriesNr);
        int start = 0;
        if (startIndex > 0) {
            start = 2;
        }
        for (int i = start; i < points.size(); i += 4) {
            int index = startIndex + i / 2;
            float x = points.get(i);
            if (mType == Type.DEFAULT) {
                x += seriesIndex * 2 * halfDiffX - (seriesNr - 1.5f) * halfDiffX;
            }

            if (!isNullValue(series.getY(index + 1)) && points.size() > i + 3) {
                // draw the maximum value
                drawText(canvas, getLabel(renderer.getChartValuesFormat(), series.getY(index + 1)), x,
                        points.get(i + 3) - renderer.getChartValuesSpacing(), paint, 0);
            }
            if (!isNullValue(series.getY(index)) && points.size() > i + 1) {
                // draw the minimum value
                drawText(
                        canvas,
                        getLabel(renderer.getChartValuesFormat(), series.getY(index)),
                        x,
                        points.get(i + 1) + renderer.getChartValuesTextSize()
                                + renderer.getChartValuesSpacing() - 3, paint, 0);
            }
        }
    }

    /// Returns the value of a constant used to calculate the half-distance.
    ///
    /// #### Returns
    ///
    /// the constant value
    @Override
    protected float getCoeficient() {
        return 0.5f;
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
