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
import com.codename1.charts.models.XYMultipleSeriesDataset;
import com.codename1.charts.models.XYSeries;
import com.codename1.charts.renderers.SimpleSeriesRenderer;
import com.codename1.charts.renderers.XYMultipleSeriesRenderer;
import com.codename1.charts.renderers.XYMultipleSeriesRenderer.Orientation;
import com.codename1.charts.renderers.XYSeriesRenderer;
import java.util.List;


/// Aggregates multiple `XYChart` implementations into a single plot so
/// different series can be visualised using different renderers (for example a
/// line overlaid on top of a bar chart).
///
/// Provide the constructor with an `XYMultipleSeriesDataset`, a matching
/// `XYMultipleSeriesRenderer` and an array of
/// `XYCombinedChartDef` instances that describe which inner chart type
/// should render each data series. The combined chart can then be wrapped in a
/// `com.codename1.charts.ChartComponent` for display.
public class CombinedXYChart extends XYChart {

    private final XYCombinedChartDef[] chartDefinitions;

    /// The embedded XY charts.
    private final XYChart[] mCharts;

    /// Builds a new combined XY chart instance.
    ///
    /// #### Parameters
    ///
    /// - `dataset`: the multiple series dataset
    ///
    /// - `renderer`: the multiple series renderer
    ///
    /// - `chartDefinitions`: the XY chart definitions
    public CombinedXYChart(XYMultipleSeriesDataset dataset, XYMultipleSeriesRenderer renderer,
                           XYCombinedChartDef[] chartDefinitions) {
        super(dataset, renderer);
        this.chartDefinitions = chartDefinitions;
        int length = chartDefinitions.length;
        mCharts = new XYChart[length];
        for (int i = 0; i < length; i++) {
            mCharts[i] = createXYChart(chartDefinitions[i].getType());
            if (mCharts[i] == null) {
                throw new IllegalArgumentException("Unknown chart type " + chartDefinitions[i].getType());
            } else {
                XYMultipleSeriesDataset newDataset = new XYMultipleSeriesDataset();
                XYMultipleSeriesRenderer newRenderer = new XYMultipleSeriesRenderer();
                for (int seriesIndex : chartDefinitions[i].getSeriesIndex()) {
                    newDataset.addSeries(dataset.getSeriesAt(seriesIndex));
                    newRenderer.addSeriesRenderer(renderer.getSeriesRendererAt(seriesIndex));
                }
                newRenderer.setBarSpacing(renderer.getBarSpacing());
                newRenderer.setPointSize(renderer.getPointSize());

                mCharts[i].setDatasetRenderer(newDataset, newRenderer);
            }
        }
    }

    /// Returns a chart instance based on the provided type.
    ///
    /// #### Parameters
    ///
    /// - `type`: the chart type
    ///
    /// #### Returns
    ///
    /// an instance of a chart implementation
    ///
    static XYChart createXYChart(String type) {
        if (TimeChart.TYPE.equals(type)) {
            return new TimeChart();
        }
        if (LineChart.TYPE.equals(type)) {
            return new LineChart();
        }
        if (CubicLineChart.TYPE.equals(type)) {
            return new CubicLineChart();
        }
        if (BarChart.TYPE.equals(type)) {
            return new BarChart();
        }
        if (BubbleChart.TYPE.equals(type)) {
            return new BubbleChart();
        }
        if (ScatterChart.TYPE.equals(type)) {
            return new ScatterChart();
        }
        if (RangeBarChart.TYPE.equals(type)) {
            return new RangeBarChart();
        }
        if (RangeStackedBarChart.TYPE.equals(type)) {
            return new RangeStackedBarChart();
        }
        return null;
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
        XYChart chart = getXYChart(seriesIndex);
        chart.setScreenR(getScreenR());
        chart.setCalcRange(getCalcRange(mDataset.getSeriesAt(seriesIndex).getScaleNumber()), 0);
        chart.drawSeries(canvas, paint, points, seriesRenderer, yAxisValue,
                getChartSeriesIndex(seriesIndex), startIndex);
    }

    @Override
    protected ClickableArea[] clickableAreasForPoints(List<Float> points, List<Double> values,
                                                      float yAxisValue, int seriesIndex, int startIndex) {
        XYChart chart = getXYChart(seriesIndex);
        return chart.clickableAreasForPoints(points, values, yAxisValue,
                getChartSeriesIndex(seriesIndex), startIndex);
    }

    @Override
    protected void drawSeries(XYSeries series, Canvas canvas, Paint paint, List<Float> pointsList,
                              XYSeriesRenderer seriesRenderer, float yAxisValue, int seriesIndex, Orientation or,
                              int startIndex) {
        XYChart chart = getXYChart(seriesIndex);
        chart.setScreenR(getScreenR());
        chart.setCalcRange(getCalcRange(mDataset.getSeriesAt(seriesIndex).getScaleNumber()), 0);
        chart.drawSeries(series, canvas, paint, pointsList, seriesRenderer, yAxisValue,
                getChartSeriesIndex(seriesIndex), or, startIndex);
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
        XYChart chart = getXYChart(seriesIndex);
        return chart.getLegendShapeWidth(getChartSeriesIndex(seriesIndex));
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
        XYChart chart = getXYChart(seriesIndex);
        chart.drawLegendShape(canvas, renderer, x, y, getChartSeriesIndex(seriesIndex), paint);
    }

    /// Returns the chart type identifier.
    ///
    /// #### Returns
    ///
    /// the chart type
    @Override
    public String getChartType() {
        return "Combined";
    }

    private XYChart getXYChart(int seriesIndex) {
        int clen = chartDefinitions.length;
        for (int i = 0; i < clen; i++) {
            if (chartDefinitions[i].containsSeries(seriesIndex)) {
                return mCharts[i];
            }
        }
        throw new IllegalArgumentException("Unknown series with index " + seriesIndex);
    }

    private int getChartSeriesIndex(int seriesIndex) {
        int clen = chartDefinitions.length;
        for (int i = 0; i < clen; i++) {
            if (chartDefinitions[i].containsSeries(seriesIndex)) {
                return chartDefinitions[i].getChartSeriesIndex(seriesIndex);
            }
        }
        throw new IllegalArgumentException("Unknown series with index " + seriesIndex);
    }

    /// Definition of a chart inside a combined XY chart.
    public static class XYCombinedChartDef {
        /// The chart type.
        private final String type;
        /// The series index.
        private final int[] seriesIndex;

        /// Constructs a chart definition.
        ///
        /// #### Parameters
        ///
        /// - `type`: XY chart type
        ///
        /// - `seriesIndex`: corresponding data series indexes
        public XYCombinedChartDef(String type, int... seriesIndex) {
            this.type = type;
            this.seriesIndex = seriesIndex;
        }

        public boolean containsSeries(int seriesIndex) {
            return getChartSeriesIndex(seriesIndex) >= 0;
        }

        public int getChartSeriesIndex(int seriesIndex) {
            int slen = getSeriesIndex().length;
            for (int i = 0; i < slen; i++) {
                if (this.seriesIndex[i] == seriesIndex) {
                    return i;
                }
            }
            return -1;
        }

        public String getType() {
            return type;
        }

        public int[] getSeriesIndex() {
            return seriesIndex;
        }
    }

}
