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
package com.codename1.charts.util;

import com.codename1.l10n.ParseException;
import java.util.ArrayList;
import java.util.List;
import com.codename1.util.MathUtil;


/**
 * Utility class for math operations.
 */
public class MathHelper {
  /** A value that is used a null value. */
  public static final double NULL_VALUE = -Double.MAX_VALUE+1;
  /**
   * A number formatter to be used to make sure we have a maximum number of
   * fraction digits in the labels.
   */
  private static final NumberFormat FORMAT = NumberFormat.getNumberInstance();

  private MathHelper() {
    // empty constructor
  }

  /**
   * Calculate the minimum and maximum values out of a list of doubles.
   * 
   * @param values the input values
   * @return an array with the minimum and maximum values
   */
  public static double[] minmax(List<Double> values) {
    if (values.size() == 0) {
      return new double[2];
    }
    double min = values.get(0);
    double max = min;
    int length = values.size();
    for (int i = 1; i < length; i++) {
      double value = values.get(i);
      min = min == NULL_VALUE ? value : Math.min(min, value);
      max = max == NULL_VALUE ? value : Math.max(max, value);
    }
    return new double[] { min, max };
  }

  /**
   * Computes a reasonable set of labels for a data interval and number of
   * labels.
   * 
   * @param start start value
   * @param end final value
   * @param approxNumLabels desired number of labels
   * @return collection containing the label values
   */
  public static List<Double> getLabels(final double start, final double end,
      final int approxNumLabels) {
    List<Double> labels = new ArrayList<Double>();
    if (approxNumLabels <= 0) {
      return labels;
    }
    FORMAT.setMaximumFractionDigits(5);
    double[] labelParams = computeLabels(start, end, approxNumLabels);
    // when the start > end the inc will be negative so it will still work
    int numLabels = 1 + (int) ((labelParams[1] - labelParams[0]) / labelParams[2]);
    // we want the range to be inclusive but we don't want to blow up when
    // looping for the case where the min and max are the same. So we loop
    // on
    // numLabels not on the values.
    for (int i = 0; i < numLabels; i++) {
      double z = labelParams[0] + i * labelParams[2];
      try {
        // this way, we avoid a label value like 0.4000000000000000001 instead
        // of 0.4
        z = FORMAT.parseDouble(FORMAT.format(z));
      } catch (ParseException e) {
        // do nothing here
      }
      labels.add(z);
    }
    return labels;
  }

  /**
   * Computes a reasonable number of labels for a data range.
   * 
   * @param start start value
   * @param end final value
   * @param approxNumLabels desired number of labels
   * @return double[] array containing {start value, end value, increment}
   */
  private static double[] computeLabels(final double start, final double end,
      final int approxNumLabels) {
    if (Math.abs(start - end) < 0.0000001f) {
      return new double[] { start, start, 0 };
    }
    double s = start;
    double e = end;
    boolean switched = false;
    if (s > e) {
      switched = true;
      double tmp = s;
      s = e;
      e = tmp;
    }
    double xStep = roundUp(Math.abs(s - e) / approxNumLabels);
    // Compute x starting point so it is a multiple of xStep.
    double xStart = xStep * Math.ceil(s / xStep);
    double xEnd = xStep * Math.floor(e / xStep);
    if (switched) {
      return new double[] { xEnd, xStart, -1.0 * xStep };
    }
    return new double[] { xStart, xEnd, xStep };
  }

  /**
   * Given a number, round up to the nearest power of ten times 1, 2, or 5.
   * 
   * @param val the number, it must be strictly positive
   */
  private static double roundUp(final double val) {
    int exponent = (int) Math.floor(MathUtil.log10(val));
    double rval = val * MathUtil.pow(10, -exponent);
    if (rval > 5.0) {
      rval = 10.0;
    } else if (rval > 2.0) {
      rval = 5.0;
    } else if (rval > 1.0) {
      rval = 2.0;
    }
    rval *= MathUtil.pow(10, exponent);
    return rval;
  }

}
