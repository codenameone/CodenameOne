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

import com.codename1.ui.Stroke;



/**
 * A descriptor for the stroke style.
 */
public class BasicStroke {
  /** The solid line style. */
  public static final BasicStroke SOLID = new BasicStroke(Stroke.CAP_BUTT, Stroke.JOIN_MITER, 4, null, 0);
  /** The dashed line style. */
  public static final BasicStroke DASHED = new BasicStroke(Stroke.CAP_ROUND, Stroke.JOIN_BEVEL, 10, new float[] {
      10, 10 }, 1);
  /** The dot line style. */
  public static final BasicStroke DOTTED = new BasicStroke(Stroke.CAP_ROUND, Stroke.JOIN_BEVEL, 5, new float[] {
      2, 10 }, 1);
  /** The stroke cap. */
  private int mCap;
  /** The stroke join. */
  private int mJoin;
  /** The stroke miter. */
  private float mMiter;
  /** The path effect intervals. */
  private float[] mIntervals;
  /** The path effect phase. */
  private float mPhase;

  /**
   * Build a new basic stroke style.
   * 
   * @param cap the stroke cap
   * @param join the stroke join
   * @param miter the stroke miter
   * @param intervals the path effect intervals
   * @param phase the path effect phase
   */
  public BasicStroke(int cap, int join, float miter, float[] intervals, float phase) {
    mCap = cap;
    mJoin = join;
    mMiter = miter;
    mIntervals = intervals;
  }

  /**
   * Returns the stroke cap.
   * 
   * @return the stroke cap
   */
  public int getCap() {
    return mCap;
  }

  /**
   * Returns the stroke join.
   * 
   * @return the stroke join
   */
  public int getJoin() {
    return mJoin;
  }

  /**
   * Returns the stroke miter.
   * 
   * @return the stroke miter
   */
  public float getMiter() {
    return mMiter;
  }

  /**
   * Returns the path effect intervals.
   * 
   * @return the path effect intervals
   */
  public float[] getIntervals() {
    return mIntervals;
  }

  /**
   * Returns the path effect phase.
   * 
   * @return the path effect phase
   */
  public float getPhase() {
    return mPhase;
  }

}
