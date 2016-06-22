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
package com.codename1.charts.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;


import com.codename1.charts.util.MathHelper;


/**
 * An XY series encapsulates values for XY charts like line, time, area,
 * scatter... charts.
 */
public class XYSeries{
  /** The series title. */
  private String mTitle;
  /** A map to contain values for X and Y axes and index for each bundle */
  private final IndexXYMap<Double, Double> mXY = new IndexXYMap<Double, Double>();
  /** The minimum value for the X axis. */
  private double mMinX = MathHelper.NULL_VALUE;
  /** The maximum value for the X axis. */
  private double mMaxX = -MathHelper.NULL_VALUE;
  /** The minimum value for the Y axis. */
  private double mMinY = MathHelper.NULL_VALUE;
  /** The maximum value for the Y axis. */
  private double mMaxY = -MathHelper.NULL_VALUE;
  /** The scale number for this series. */
  private final int mScaleNumber;
  /** Contains the annotations. */
  private List<String> mAnnotations = new ArrayList<String>();
  /** A map contain a (x,y) value for each String annotation. */
  private final IndexXYMap<Double, Double> mStringXY = new IndexXYMap<Double, Double>();

  /**
   * Builds a new XY series.
   * 
   * @param title the series title.
   */
  public XYSeries(String title) {
    this(title, 0);
  }

  /**
   * Builds a new XY series.
   * 
   * @param title the series title.
   * @param scaleNumber the series scale number
   */
  public XYSeries(String title, int scaleNumber) {
    mTitle = title;
    mScaleNumber = scaleNumber;
    initRange();
  }

  public int getScaleNumber() {
    return mScaleNumber;
  }

  /**
   * Initializes the range for both axes.
   */
  private void initRange() {
    mMinX = MathHelper.NULL_VALUE;
    mMaxX = -MathHelper.NULL_VALUE;
    mMinY = MathHelper.NULL_VALUE;
    mMaxY = -MathHelper.NULL_VALUE;
    int length = getItemCount();
    for (int k = 0; k < length; k++) {
      double x = getX(k);
      double y = getY(k);
      updateRange(x, y);
    }
    
    int i=0;
  }

  /**
   * Updates the range on both axes.
   * 
   * @param x the new x value
   * @param y the new y value
   */
  private void updateRange(double x, double y) {
    mMinX = Math.min(mMinX, x);
    mMaxX = Math.max(mMaxX, x);
    mMinY = Math.min(mMinY, y);
    mMaxY = Math.max(mMaxY, y);
    
   
  }
  

  /**
   * Returns the series title.
   * 
   * @return the series title
   */
  public String getTitle() {
    return mTitle;
  }

  /**
   * Sets the series title.
   * 
   * @param title the series title
   */
  public void setTitle(String title) {
    mTitle = title;
  }

  /**
   * Adds a new value to the series.
   * 
   * @param x the value for the X axis
   * @param y the value for the Y axis
   */
  public synchronized void add(double x, double y) {
    while (mXY.get(x) != null) {
      // add a very small value to x such as data points sharing the same x will
      // still be added
      x += getPadding(x);
    }
    mXY.put(x, y);
    updateRange(x, y);
  }

  /**
   * Adds a new value to the series at the specified index.
   * 
   * @param index the index to be added the data to
   * @param x the value for the X axis
   * @param y the value for the Y axis
   */
  public synchronized void add(int index, double x, double y) {
    while (mXY.get(x) != null) {
      // add a very small value to x such as data points sharing the same x will
      // still be added
      x += getPadding(x);
    }
    mXY.put(index, x, y);
    updateRange(x, y);
  }

  protected double getPadding(double x) {
    return ulp(x);
  }
  
  private static double ulp(double value) {
        long bits = Double.doubleToLongBits(value);
        if ((bits & 0x7FF0000000000000L) == 0x7FF0000000000000L) { // if x is not finite
          if ((bits & 0x000FFFFFFFFFFFFFL) != 0x0 ) { // if x is a NaN
            return value;  // I did not force the sign bit here with NaNs.
            } 
          return Double.longBitsToDouble(0x7FF0000000000000L); // Positive Infinity;
          }
        bits &= 0x7FFFFFFFFFFFFFFFL; // make positive
        if (bits == 0x7FEFFFFFFFFFFFFL) { // if x == max_double (notice the _E_)
          return Double.longBitsToDouble(bits) - Double.longBitsToDouble(bits - 1);
        }
        double nextValue = Double.longBitsToDouble(bits + 1);
        double result = nextValue - value;
        return result;
}

  /**
   * Removes an existing value from the series.
   * 
   * @param index the index in the series of the value to remove
   */
  public synchronized void remove(int index) {
    XYEntry<Double, Double> removedEntry = mXY.removeByIndex(index);
    double removedX = removedEntry.getKey();
    double removedY = removedEntry.getValue();
    if (removedX == mMinX || removedX == mMaxX || removedY == mMinY || removedY == mMaxY) {
      initRange();
    }
  }

  /**
   * Removes all the existing values and annotations from the series.
   */
  public synchronized void clear() {
    clearAnnotations();
    clearSeriesValues();
  }

  /**
   * Removes all the existing values from the series but annotations.
   */
  public synchronized void clearSeriesValues() {
    mXY.clear();
    initRange();
  }

  /**
   * Removes all the existing annotations from the series.
   */
  public synchronized void clearAnnotations() {
    mStringXY.clear();
  }

  /**
   * Returns the current values that are used for drawing the series.
   * 
   * @return the XY map
   */
  public synchronized IndexXYMap<Double, Double> getXYMap() {
    return mXY;
  }

  /**
   * Returns the X axis value at the specified index.
   * 
   * @param index the index
   * @return the X value
   */
  public synchronized double getX(int index) {
    return mXY.getXByIndex(index);
  }

  /**
   * Returns the Y axis value at the specified index.
   * 
   * @param index the index
   * @return the Y value
   */
  public synchronized double getY(int index) {
    return mXY.getYByIndex(index);
  }

  /**
   * Add an String at (x,y) coordinates
   * 
   * @param annotation String text
   * @param x
   * @param y
   */
  public void addAnnotation(String annotation, double x, double y) {
    mAnnotations.add(annotation);
    while (mStringXY.get(x) != null) {
      x += getPadding(x);
    }
    mStringXY.put(x, y);
  }

  /**
   * Add an String at (x,y) coordinates
   * 
   * @param annotation String text
   * @param index the index to add the annotation to
   * @param x
   * @param y
   */
  public void addAnnotation(String annotation, int index, double x, double y) {
    mAnnotations.add(index, annotation);
    while (mStringXY.get(x) != null) {
      x += getPadding(x);
    }
    mStringXY.put(x, y);
  }

  /**
   * Remove an String at index
   * 
   * @param index
   */
  public void removeAnnotation(int index) {
    mAnnotations.remove(index);
    mStringXY.removeByIndex(index);
  }

  /**
   * Get X coordinate of the annotation at index
   * 
   * @param index the index in the annotations list
   * @return the corresponding annotation X value
   */
  public double getAnnotationX(int index) {
    return mStringXY.getXByIndex(index);
  }

  /**
   * Get Y coordinate of the annotation at index
   * 
   * @param index the index in the annotations list
   * @return the corresponding annotation Y value
   */
  public double getAnnotationY(int index) {
    return mStringXY.getYByIndex(index);
  }

  /**
   * Get the annotations count
   * 
   * @return the annotations count
   */
  public int getAnnotationCount() {
    return mAnnotations.size();
  }

  /**
   * Get the String at index
   * 
   * @param index
   * @return String
   */
  public String getAnnotationAt(int index) {
    return mAnnotations.get(index);
  }

  /**
   * Returns submap of x and y values according to the given start and end
   * 
   * @param start start x value
   * @param stop stop x value
   * @param beforeAfterPoints if the points before and after the first and last
   *          visible ones must be displayed
   * @return a submap of x and y values
   */
  public synchronized SortedMap<Double, Double> getRange(double start, double stop,
      boolean beforeAfterPoints) {
    if (beforeAfterPoints) {
      // we need to add one point before the start and one point after the end
      // (if there are any)
      // to ensure that line doesn't end before the end of the screen

      // this would be simply: start = mXY.lowerKey(start) but NavigableMap is
      // available since API 9
      SortedMap<Double, Double> headMap = mXY.headMap(start);
      if (!headMap.isEmpty()) {
        start = headMap.lastKey();
      }

      // this would be simply: end = mXY.higherKey(end) but NavigableMap is
      // available since API 9
      // so we have to do this hack in order to support older versions
      SortedMap<Double, Double> tailMap = mXY.tailMap(stop);
      if (!tailMap.isEmpty()) {
        Iterator<Double> tailIterator = tailMap.keySet().iterator();
        Double next = tailIterator.next();
        if (tailIterator.hasNext()) {
          stop = tailIterator.next();
        } else {
          stop += next;
        }
      }
    }
    if (start <= stop) {
      return mXY.subMap(start, stop);
    } else {
      return new TreeMap<Double, Double>();
    }
  }

  public int getIndexForKey(double key) {
    return mXY.getIndexForKey(key);
  }

  /**
   * Returns the series item count.
   * 
   * @return the series item count
   */
  public synchronized int getItemCount() {
    return mXY.size();
  }

  /**
   * Returns the minimum value on the X axis.
   * 
   * @return the X axis minimum value
   */
  public double getMinX() {
    return mMinX;
  }

  /**
   * Returns the minimum value on the Y axis.
   * 
   * @return the Y axis minimum value
   */
  public double getMinY() {
    return mMinY;
  }

  /**
   * Returns the maximum value on the X axis.
   * 
   * @return the X axis maximum value
   */
  public double getMaxX() {
    return mMaxX;
  }

  /**
   * Returns the maximum value on the Y axis.
   * 
   * @return the Y axis maximum value
   */
  public double getMaxY() {
    return mMaxY;
  }
  
  /**
 * This class requires sorted x values
 */
private static class IndexXYMap<K, V> extends TreeMap<K, V> {
  private final List<K> indexList = new ArrayList<K>();

  private double maxXDifference = 0;
  private boolean sorted = false;
  
  public IndexXYMap() {
    super();
  }

  public V put(K key, V value) {
    indexList.add(key);
    sorted = false;
    updateMaxXDifference();
    return super.put(key, value);
  }

  public V put(int index, K key, V value) {
    indexList.add(index, key);
    sorted = false;
    updateMaxXDifference();
    return super.put(key, value);
  }

  private void updateMaxXDifference() {
    if (indexList.size() < 2) {
      maxXDifference = 0;
      return;
    }

    if (Math.abs((Double) indexList.get(indexList.size() - 1)
        - (Double) indexList.get(indexList.size() - 2)) > maxXDifference)
      maxXDifference = Math.abs((Double) indexList.get(indexList.size() - 1)
          - (Double) indexList.get(indexList.size() - 2));
  }

  public double getMaxXDifference() {
    return maxXDifference;
  }

  public void clear() {
    updateMaxXDifference();
    super.clear();
    indexList.clear();
  }

  /**
   * Returns X-value according to the given index
   * 
   * @param index
   * @return the X value
   */
  public K getXByIndex(int index) {
    return indexList.get(index);
  }

  /**
   * Returns Y-value according to the given index
   * 
   * @param index
   * @return the Y value
   */
  public V getYByIndex(int index) {
    K key = indexList.get(index);
    return this.get(key);
  }

  /**
   * Returns XY-entry according to the given index
   * 
   * @param index
   * @return the X and Y values
   */
  public XYEntry<K, V> getByIndex(int index) {
    K key = indexList.get(index);
    return new XYEntry<K, V>(key, this.get(key));
  }

  /**
   * Removes entry from map by index
   * 
   * @param index
   */
  public XYEntry<K, V> removeByIndex(int index) {
    K key = indexList.remove(index);
    return new XYEntry<K, V>(key, this.remove(key));
  }

  public int getIndexForKey(K key) {
    if (!sorted){
        Collections.sort(indexList, null);
        sorted = true;
    }
    int out = Collections.binarySearch(indexList, key, null);
    return out;
  }
}
}

/**
 * A map entry value encapsulating an XY point.
 */
class XYEntry<K, V> implements Map.Entry<K, V> {
  private final K key;
  
  private V value;

  public XYEntry(K key, V value) {
    this.key = key;
    this.value = value;
  }

  public K getKey() {
    return key;
  }

  public V getValue() {
    return value;
  }

  public V setValue(V object) {
    this.value = object;
    return this.value;
  }
}
