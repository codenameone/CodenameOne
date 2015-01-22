/**
 * Copyright (C) 2009 - 2013 SC 4ViewSoft SRL
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.codename1.charts.views;

import java.util.List;
import com.codename1.charts.compat.Canvas;
import com.codename1.charts.compat.Paint;

import com.codename1.charts.models.XYMultipleSeriesDataset;
import com.codename1.charts.models.XYSeries;
import com.codename1.charts.renderers.SimpleSeriesRenderer;
import com.codename1.charts.renderers.XYMultipleSeriesRenderer;
import com.codename1.charts.renderers.XYMultipleSeriesRenderer.Orientation;
import com.codename1.charts.renderers.XYSeriesRenderer;



/**
 * The combined XY chart rendering class.
 */
public class CombinedXYChart extends XYChart {

  private XYCombinedChartDef[] chartDefinitions;

  /** The embedded XY charts. */
  private XYChart[] mCharts;

  /** The supported charts for being combined. */
  private Class<?>[] xyChartTypes = new Class<?>[] { TimeChart.class, LineChart.class,
      CubicLineChart.class, BarChart.class, BubbleChart.class, ScatterChart.class,
      RangeBarChart.class, RangeStackedBarChart.class };

  /**
   * Builds a new combined XY chart instance.
   * 
   * @param dataset the multiple series dataset
   * @param renderer the multiple series renderer
   * @param chartDefinitions the XY chart definitions
   */
  public CombinedXYChart(XYMultipleSeriesDataset dataset, XYMultipleSeriesRenderer renderer,
      XYCombinedChartDef[] chartDefinitions) {
    super(dataset, renderer);
    this.chartDefinitions = chartDefinitions;
    int length = chartDefinitions.length;
    mCharts = new XYChart[length];
    for (int i = 0; i < length; i++) {
      try {
        mCharts[i] = getXYChart(chartDefinitions[i].getType());
      } catch (Exception e) {
        // ignore
      }
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

  /**
   * Returns a chart instance based on the provided type.
   * 
   * @param type the chart type
   * @return an instance of a chart implementation
   * @throws IllegalAccessException
   * @throws InstantiationException
   */
  private XYChart getXYChart(String type) throws IllegalAccessException, InstantiationException {
    XYChart chart = null;
    int length = xyChartTypes.length;
    for (int i = 0; i < length && chart == null; i++) {
      XYChart newChart = (XYChart) xyChartTypes[i].newInstance();
      if (type.equals(newChart.getChartType())) {
        chart = newChart;
      }
    }
    return chart;
  }

  /**
   * The graphical representation of a series.
   * 
   * @param canvas the canvas to paint to
   * @param paint the paint to be used for drawing
   * @param points the array of points to be used for drawing the series
   * @param seriesRenderer the series renderer
   * @param yAxisValue the minimum value of the y axis
   * @param seriesIndex the index of the series currently being drawn
   * @param startIndex the start index of the rendering points
   */
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

  /**
   * Returns the legend shape width.
   * 
   * @param seriesIndex the series index
   * @return the legend shape width
   */
  public int getLegendShapeWidth(int seriesIndex) {
    XYChart chart = getXYChart(seriesIndex);
    return chart.getLegendShapeWidth(getChartSeriesIndex(seriesIndex));
  }

  /**
   * The graphical representation of the legend shape.
   * 
   * @param canvas the canvas to paint to
   * @param renderer the series renderer
   * @param x the x value of the point the shape should be drawn at
   * @param y the y value of the point the shape should be drawn at
   * @param seriesIndex the series index
   * @param paint the paint to be used for drawing
   */
  public void drawLegendShape(Canvas canvas, SimpleSeriesRenderer renderer, float x, float y,
      int seriesIndex, Paint paint) {
    XYChart chart = getXYChart(seriesIndex);
    chart.drawLegendShape(canvas, renderer, x, y, getChartSeriesIndex(seriesIndex), paint);
  }

  /**
   * Returns the chart type identifier.
   * 
   * @return the chart type
   */
  public String getChartType() {
    return "Combined";
  }

  private XYChart getXYChart(int seriesIndex) {
    for (int i = 0; i < chartDefinitions.length; i++) {
      if (chartDefinitions[i].containsSeries(seriesIndex)) {
        return mCharts[i];
      }
    }
    throw new IllegalArgumentException("Unknown series with index " + seriesIndex);
  }

  private int getChartSeriesIndex(int seriesIndex) {
    for (int i = 0; i < chartDefinitions.length; i++) {
      if (chartDefinitions[i].containsSeries(seriesIndex)) {
        return chartDefinitions[i].getChartSeriesIndex(seriesIndex);
      }
    }
    throw new IllegalArgumentException("Unknown series with index " + seriesIndex);
  }

  /**
   * Definition of a chart inside a combined XY chart.
   */
  public static class XYCombinedChartDef {
    /** The chart type. */
    private String type;
    /** The series index. */
    private int[] seriesIndex;

    /**
     * Constructs a chart definition.
     * 
     * @param type XY chart type
     * @param seriesIndex corresponding data series indexes
     */
    public XYCombinedChartDef(String type, int... seriesIndex) {
      this.type = type;
      this.seriesIndex = seriesIndex;
    }

    public boolean containsSeries(int seriesIndex) {
      return getChartSeriesIndex(seriesIndex) >= 0;
    }

    public int getChartSeriesIndex(int seriesIndex) {
      for (int i = 0; i < getSeriesIndex().length; i++) {
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
