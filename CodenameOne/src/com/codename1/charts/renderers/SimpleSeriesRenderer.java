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
package com.codename1.charts.renderers;

import com.codename1.charts.util.NumberFormat;



/**
 * A simple series renderer.
 */
public class SimpleSeriesRenderer  {
  /** The series color. */
  private int mColor =0x0000ff;
  /** The stroke style. */
  private BasicStroke mStroke;
  /** If gradient is enabled. */
  private boolean mGradientEnabled = false;
  /** The gradient start value. */
  private double mGradientStartValue;
  /** The gradient start color. */
  private int mGradientStartColor;
  /** The gradient stop value. */
  private double mGradientStopValue;
  /** The gradient stop color. */
  private int mGradientStopColor;
  /** If the legend item for this renderer is visible. */
  private boolean mShowLegendItem = true;
  /** If this is a highlighted slice (pie chart displays slice as exploded). */
  private boolean mHighlighted;
  /** If the bounding points to the first and last visible ones should be displayed. */
  private boolean mDisplayBoundingPoints = true;
  /** The chart values format. */
  private NumberFormat mChartValuesFormat;

  /**
   * Returns the series color.
   * 
   * @return the series color
   */
  public int getColor() {
    return mColor;
  }

  /**
   * Sets the series color.
   * 
   * @param color the series color
   */
  public void setColor(int color) {
    mColor = color;
  }

  /**
   * Returns the stroke style.
   * 
   * @return the stroke style
   */
  public BasicStroke getStroke() {
    return mStroke;
  }

  /**
   * Sets the stroke style.
   * 
   * @param stroke the stroke style
   */
  public void setStroke(BasicStroke stroke) {
    mStroke = stroke;
  }

  /**
   * Returns the gradient is enabled value.
   * 
   * @return the gradient enabled
   */
  public boolean isGradientEnabled() {
    return mGradientEnabled;
  }

  /**
   * Sets the gradient enabled value.
   * 
   * @param enabled the gradient enabled
   */
  public void setGradientEnabled(boolean enabled) {
    mGradientEnabled = enabled;
  }

  /**
   * Returns the gradient start value.
   * 
   * @return the gradient start value
   */
  public double getGradientStartValue() {
    return mGradientStartValue;
  }

  /**
   * Returns the gradient start color.
   * 
   * @return the gradient start color
   */
  public int getGradientStartColor() {
    return mGradientStartColor;
  }

  /**
   * Sets the gradient start value and color.
   * 
   * @param start the gradient start value
   * @param color the gradient start color
   */
  public void setGradientStart(double start, int color) {
    mGradientStartValue = start;
    mGradientStartColor = color;
  }

  /**
   * Returns the gradient stop value.
   * 
   * @return the gradient stop value
   */
  public double getGradientStopValue() {
    return mGradientStopValue;
  }

  /**
   * Returns the gradient stop color.
   * 
   * @return the gradient stop color
   */
  public int getGradientStopColor() {
    return mGradientStopColor;
  }

  /**
   * Sets the gradient stop value and color.
   * 
   * @param start the gradient stop value
   * @param color the gradient stop color
   */
  public void setGradientStop(double start, int color) {
    mGradientStopValue = start;
    mGradientStopColor = color;
  }

  /**
   * Returns if the legend item for this renderer should be visible.
   * 
   * @return the visibility flag for the legend item for this renderer
   */
  public boolean isShowLegendItem() {
    return mShowLegendItem;
  }

  /**
   * Sets if the legend item for this renderer should be visible.
   * 
   * @param showLegend the visibility flag for the legend item for this renderer
   */
  public void setShowLegendItem(boolean showLegend) {
    mShowLegendItem = showLegend;
  }

  /**
   * Returns if the item is displayed highlighted.
   * 
   * @return the highlighted flag for the item for this renderer
   */
  public boolean isHighlighted() {
    return mHighlighted;
  }

  /**
   * Sets if the item for this renderer should be highlighted. Pie chart is supported for now.
   * 
   * @param highlighted the highlighted flag for the item for this renderer
   */
  public void setHighlighted(boolean highlighted) {
    mHighlighted = highlighted;
  }

  /**
   * Returns if the bounding points of the first and last visible ones should be displayed.
   * 
   * @return the bounding points display
   */
  public boolean isDisplayBoundingPoints() {
    return mDisplayBoundingPoints;
  }

  /**
   * Sets if the bounding points of the first and last visible ones should be displayed.
   * 
   * @param display the bounding points display
   */
  public void setDisplayBoundingPoints(boolean display) {
    mDisplayBoundingPoints = display;
  }

  /**
   * Returns the number format for displaying chart values.
   * 
   * @return the number format for chart values
   */
   public NumberFormat getChartValuesFormat() {
    return mChartValuesFormat;
  }

  /**
   * Sets the number format for displaying chart values.
   * 
   * @param format the number format for chart values
   */
   public void setChartValuesFormat(NumberFormat format) {
    mChartValuesFormat = format;
  }

}
