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

import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.geom.Shape;


/**
 * Holds An PieChart Segment
 */
public class PieSegment  {
  private float mStartAngle;

  private float mEndAngle;

  private int mDataIndex;

  private float mValue;
  

  public PieSegment(int dataIndex, float value, float startAngle, float angle) {
    mStartAngle = startAngle;
    mEndAngle = angle + startAngle;
    mDataIndex = dataIndex;
    mValue = value;
  }

  /**
   * Checks if angle falls in segment.
   * 
   * @param angle
   * @return true if in segment, false otherwise.
   */
  public boolean isInSegment(double angle) {
    if (angle >= mStartAngle && angle <= mEndAngle) {
      return true;
    }
    double cAngle = angle % 360;
    double startAngle = mStartAngle;
    double stopAngle = mEndAngle;
    while (stopAngle > 360) {
      startAngle -= 360;
      stopAngle -= 360;
    }
    return cAngle >= startAngle && cAngle <= stopAngle;
  }

  protected float getStartAngle() {
    return mStartAngle;
  }

  protected float getEndAngle() {
    return mEndAngle;
  }

  protected int getDataIndex() {
    return mDataIndex;
  }

  protected float getValue() {
    return mValue;
  }

  public String toString() {
    return "mDataIndex=" + mDataIndex + ",mValue=" + mValue + ",mStartAngle=" + mStartAngle
        + ",mEndAngle=" + mEndAngle;
  }
  
  public Shape getShape(float cX, float cY, float radius){
      GeneralPath out = new GeneralPath();
      out.moveTo(cX, cY);
      out.lineTo(cX + radius * Math.cos(Math.toRadians(mStartAngle)) , cY + radius *  Math.sin(Math.toRadians(mStartAngle)));
      out.arcTo(cX, cY, cX + radius * Math.cos(Math.toRadians(mEndAngle)), cY + radius*Math.sin(Math.toRadians(mEndAngle)));
      out.closePath();
      return out;
  }

}
