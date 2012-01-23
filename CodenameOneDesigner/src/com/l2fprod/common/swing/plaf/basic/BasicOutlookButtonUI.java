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
package com.l2fprod.common.swing.plaf.basic;

import com.l2fprod.common.swing.border.ButtonBorder;
import com.l2fprod.common.swing.border.FourLineBorder;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.LookAndFeel;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicButtonUI;

/**
 * Mimics Outlook button look. <br>
 */
public class BasicOutlookButtonUI extends BasicButtonUI {

  public static ComponentUI createUI(JComponent c) {
    return new BasicOutlookButtonUI();
  }

  protected void installDefaults(AbstractButton b) {
    super.installDefaults(b);

    b.setRolloverEnabled(true);
    b.setOpaque(false);
    b.setHorizontalTextPosition(JButton.CENTER);
    b.setVerticalTextPosition(JButton.BOTTOM);

    LookAndFeel.installBorder(b, "OutlookButton.border");    
  }

  protected void paintButtonPressed(Graphics g, AbstractButton b) {
    setTextShiftOffset();
  }

  public static class OutlookButtonBorder extends ButtonBorder {
    FourLineBorder rolloverBorder;
    FourLineBorder pressedBorder;
    public OutlookButtonBorder(Color color1, Color color2) {
      rolloverBorder = new FourLineBorder(color1, color1, color2, color2);
      pressedBorder = new FourLineBorder(color2, color2, color1, color1);      
    }
    protected void paintRollover(AbstractButton b, Graphics g, int x, int y,
      int width, int height) {
      rolloverBorder.paintBorder(b, g, x, y, width, height);
    }
    protected void paintPressed(AbstractButton b, Graphics g, int x, int y,
      int width, int height) {
      pressedBorder.paintBorder(b, g, x, y, width, height);
    }    
    public Insets getBorderInsets(Component c) {
      return rolloverBorder.getBorderInsets(c);
    }
  }
 
}