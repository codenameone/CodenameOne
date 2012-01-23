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

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.border.AbstractBorder;

/**
 * ButtonBorder. <br>
 *  
 */
public class ButtonBorder extends AbstractBorder {

  public void paintBorder(Component c, Graphics g, int x, int y, int width,
    int height) {
    if (c instanceof AbstractButton) {
      AbstractButton b = (AbstractButton)c;
      ButtonModel model = b.getModel();

      boolean isPressed;
      boolean isRollover;
      boolean isEnabled;

      isPressed = model.isPressed() && model.isArmed();
      isRollover = b.isRolloverEnabled() && model.isRollover();
      isEnabled = b.isEnabled();

      if (!isEnabled) {
        paintDisabled(b, g, x, y, width, height);
      } else {
        if (isPressed) {
          paintPressed(b, g, x, y, width, height);
        } else if (isRollover) {
          paintRollover(b, g, x, y, width, height);
        } else {
          paintNormal(b, g, x, y, width, height);
        }
      }
    }
  }

  protected void paintNormal(AbstractButton b, Graphics g, int x, int y,
    int width, int height) {}

  protected void paintDisabled(AbstractButton b, Graphics g, int x, int y,
    int width, int height) {}

  protected void paintRollover(AbstractButton b, Graphics g, int x, int y,
    int width, int height) {}

  protected void paintPressed(AbstractButton b, Graphics g, int x, int y,
    int width, int height) {}

  public Insets getBorderInsets(Component c) {
    return getBorderInsets(c, new Insets(0, 0, 0, 0));
  }

  public Insets getBorderInsets(Component c, Insets insets) {
    return insets;
  }

}