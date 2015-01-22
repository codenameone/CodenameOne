/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.charts.views;

import com.codename1.ui.geom.Rectangle2D;

/**
 *
 * @author shannah
 */
class PkgUtils {
    static Rectangle2D makeRect(double x1, double y1, double x2, double y2){
      return new Rectangle2D(x1, y1, x2-x1, y2-y1);
  }
}
