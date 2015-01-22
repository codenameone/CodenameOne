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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import com.codename1.charts.compat.Canvas;
import com.codename1.charts.compat.Paint;

import com.codename1.charts.models.XYMultipleSeriesDataset;
import com.codename1.charts.models.XYSeries;
import com.codename1.charts.renderers.XYMultipleSeriesRenderer;
import java.util.Calendar;
import java.util.TimeZone;


/**
 * The time chart rendering class.
 */
public class TimeChart extends LineChart {
    /**
     * The number of milliseconds in a minute.
     */
    private static final int MILLIS_TO_MINUTES = 60000;
    
    /**
     * This is missing from the Codename One Calendar object, but required by
     * TimeZone.getOffset()
     */
    private static final int ERA = 0;
    /**
  /** The constant to identify this chart type. */
  public static final String TYPE = "Time";
  /** The number of milliseconds in a day. */
  public static final long DAY = 24 * 60 * 60 * 1000;
  /** The date format pattern to be used in formatting the X axis labels. */
  private String mDateFormat;
  /** The starting point for labels. */
  private Double mStartPoint;

  TimeChart() {
  }

  /**
   * Builds a new time chart instance.
   * 
   * @param dataset the multiple series dataset
   * @param renderer the multiple series renderer
   */
  public TimeChart(XYMultipleSeriesDataset dataset, XYMultipleSeriesRenderer renderer) {
    super(dataset, renderer);
  }

  /**
   * Returns the date format pattern to be used for formatting the X axis
   * labels.
   * 
   * @return the date format pattern for the X axis labels
   */
  public String getDateFormat() {
    return mDateFormat;
  }

  /**
   * Sets the date format pattern to be used for formatting the X axis labels.
   * 
   * @param format the date format pattern for the X axis labels. If null, an
   *          appropriate default format will be used.
   */
  public void setDateFormat(String format) {
    mDateFormat = format;
  }

  /**
   * The graphical representation of the labels on the X axis.
   * 
   * @param xLabels the X labels values
   * @param xTextLabelLocations the X text label locations
   * @param canvas the canvas to paint to
   * @param paint the paint to be used for drawing
   * @param left the left value of the labels area
   * @param top the top value of the labels area
   * @param bottom the bottom value of the labels area
   * @param xPixelsPerUnit the amount of pixels per one unit in the chart labels
   * @param minX the minimum value on the X axis in the chart
   * @param maxX the maximum value on the X axis in the chart
   */
  @Override
  protected void drawXLabels(List<Double> xLabels, Double[] xTextLabelLocations, Canvas canvas,
      Paint paint, int left, int top, int bottom, double xPixelsPerUnit, double minX, double maxX) {
    int length = xLabels.size();
    if (length > 0) {
      boolean showLabels = mRenderer.isShowLabels();
      boolean showGridY = mRenderer.isShowGridY();
      boolean showTickMarks = mRenderer.isShowTickMarks();
      DateFormat format = getDateFormat(xLabels.get(0), xLabels.get(length - 1));
      for (int i = 0; i < length; i++) {
        long label = Math.round(xLabels.get(i));
        float xLabel = (float) (left + xPixelsPerUnit * (label - minX));
        if (showLabels) {
          paint.setColor(mRenderer.getXLabelsColor());
          if (showTickMarks) {
            canvas.drawLine(xLabel, bottom, xLabel, bottom + mRenderer.getLabelsTextSize() / 3,
                paint);
          }
          drawText(canvas, format.format(new Date(label)), xLabel,
              bottom + mRenderer.getLabelsTextSize() * 4 / 3 + mRenderer.getXLabelsPadding(),
              paint, mRenderer.getXLabelsAngle());
        }
        if (showGridY) {
          paint.setColor(mRenderer.getGridColor(0));
          canvas.drawLine(xLabel, bottom, xLabel, top, paint);
        }
      }
    }
    drawXTextLabels(xTextLabelLocations, canvas, paint, true, left, top, bottom, xPixelsPerUnit,
        minX, maxX);
  }

  /**
   * Returns the date format pattern to be used, based on the date range.
   * 
   * @param start the start date in milliseconds
   * @param end the end date in milliseconds
   * @return the date format
   */
  private DateFormat getDateFormat(double start, double end) {
    if (mDateFormat != null) {
      SimpleDateFormat format = null;
      try {
        format = new SimpleDateFormat(mDateFormat);
        return format;
      } catch (Exception e) {
        // do nothing here
      }
    }
    DateFormat format = SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM);
    double diff = end - start;
    if (diff > DAY && diff < 5 * DAY) {
      format = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.SHORT);
    } else if (diff < DAY) {
      format = SimpleDateFormat.getTimeInstance(SimpleDateFormat.MEDIUM);
    }
    return format;
  }

  /**
   * Returns the chart type identifier.
   * 
   * @return the chart type
   */
  public String getChartType() {
    return TYPE;
  }

  @Override
  protected List<Double> getXLabels(double min, double max, int count) {
    final List<Double> result = new ArrayList<Double>();
    if (!mRenderer.isXRoundedLabels()) {
      if (mDataset.getSeriesCount() > 0) {
        XYSeries series = mDataset.getSeriesAt(0);
        int length = series.getItemCount();
        int intervalLength = 0;
        int startIndex = -1;
        for (int i = 0; i < length; i++) {
          double value = series.getX(i);
          if (min <= value && value <= max) {
            intervalLength++;
            if (startIndex < 0) {
              startIndex = i;
            }
          }
        }
        if (intervalLength < count) {
          for (int i = startIndex; i < startIndex + intervalLength; i++) {
            result.add(series.getX(i));
          }
        } else {
          float step = (float) intervalLength / count;
          int intervalCount = 0;
          for (int i = 0; i < length && intervalCount < count; i++) {
            double value = series.getX(Math.round(i * step));
            if (min <= value && value <= max) {
              result.add(value);
              intervalCount++;
            }
          }
        }
        return result;
      } else {
        return super.getXLabels(min, max, count);
      }
    }
    if (mStartPoint == null) {
        TimeZone  tz = TimeZone.getDefault();
        Calendar cal = Calendar.getInstance(tz);
        cal.setTime(new Date(Math.round(min)));
        
        
        int offset = getDSTOffset(cal);
      mStartPoint = min - (min % DAY) + DAY + offset * 60 * 60 * 1000;
    }
    if (count > 25) {
      count = 25;
    }

    final double cycleMath = (max - min) / count;
    if (cycleMath <= 0) {
      return result;
    }
    double cycle = DAY;

    if (cycleMath <= DAY) {
      while (cycleMath < cycle / 2) {
        cycle = cycle / 2;
      }
    } else {
      while (cycleMath > cycle) {
        cycle = cycle * 2;
      }
    }

    double val = mStartPoint - Math.floor((mStartPoint - min) / cycle) * cycle;
    int i = 0;
    while (val < max && i++ <= count) {
      result.add(val);
      val += cycle;
    }

    return result;
  }
  
  /**
     * Determine the number of minutes to adjust the date for local DST. This
     * should provide a historically correct value, also accounting for changes
     * in GMT offset. See TimeZone javadoc for more details.
     *
     * @param source
     * @return
     */
    int getDSTOffset(Calendar source) {
        TimeZone localTimezone = Calendar.getInstance().getTimeZone();
        int rawOffset = localTimezone.getRawOffset() / MILLIS_TO_MINUTES;
        return getOffsetInMinutes(source, localTimezone) - rawOffset;
    }

    /**
     * Get the offset from GMT for a given timezone.
     *
     * @param source
     * @param timezone
     * @return
     */
    int getOffsetInMinutes(Calendar source, TimeZone timezone) {
        return timezone.getOffset(source.get(ERA), source.get(Calendar.YEAR), source.get(Calendar.MONTH),
                source.get(Calendar.DAY_OF_MONTH), source.get(Calendar.DAY_OF_WEEK), source.get(Calendar.MILLISECOND))
                / MILLIS_TO_MINUTES;
    }
}
