/**
 * @PROJECT.FULLNAME@ @VERSION@ License.
 *
 * Copyright @YEAR@ L2FProd.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.l2fprod.common.swing.border;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.border.Border;

/**
 * FourLineBorder. <br>
 * 
 */
public class FourLineBorder implements Border {

  private Color top;
  private Color left;
  private Color bottom;
  private Color right;

  public FourLineBorder(Color top, Color left, Color bottom, Color right) {
    this.top = top;
    this.left = left;
    this.bottom = bottom;
    this.right = right;
  }

  public Insets getBorderInsets(Component c) {
    return new Insets(top == null?0:1, left == null?0:1, bottom == null?0:1,
      right == null?0:1);
  }

  public boolean isBorderOpaque() {
    return true;
  }

  public void paintBorder(Component c, Graphics g, int x, int y, int width,
    int height) {
    if (bottom != null) {
      g.setColor(bottom);
      g.drawLine(x, y + height - 1, x + width - 1, y + height - 1);
    }

    if (right != null) {
      g.setColor(right);
      g.drawLine(x + width - 1, y, x + width - 1, y + height - 1);
    }

    if (top != null) {
      g.setColor(top);
      g.drawLine(x, y, x + width - 1, y);
    }

    if (left != null) {
      g.setColor(left);
      g.drawLine(x, y, x, y + height - 1);
    }
  }

}