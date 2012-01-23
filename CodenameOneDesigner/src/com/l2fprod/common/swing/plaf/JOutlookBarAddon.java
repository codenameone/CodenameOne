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
package com.l2fprod.common.swing.plaf;

import com.l2fprod.common.swing.JOutlookBar;
import com.l2fprod.common.swing.border.FourLineBorder;
import com.l2fprod.common.swing.plaf.basic.BasicOutlookButtonUI;
import com.l2fprod.common.swing.plaf.windows.WindowsOutlookBarUI;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;

/**
 * JOutlookBarAddon. <br>
 *  
 */
public class JOutlookBarAddon extends AbstractComponentAddon {

  public JOutlookBarAddon() {
    super("JOutlookBar");
  }

  protected void addBasicDefaults(LookAndFeelAddons addon, List defaults) {
    Color barBackground = new Color(167, 166, 170);
    
    // border for the bar and the tab buttons
    Border outlookBarButtonBorder;
    Border outlookBarBorder;
    {
      Color background = UIManager.getColor("Button.background");
      if (background == null) {
        // try control
        background = UIManager.getColor("control");
      }
      if (background == null) {
        background = new Color(238,238,238);
      }
      Color color1 = background.brighter();
      Color color2 = background.darker();

      outlookBarButtonBorder = new WindowsOutlookBarUI.WindowsTabButtonBorder(
      color1, color2);
    outlookBarButtonBorder = new CompoundBorder(outlookBarButtonBorder,
      BorderFactory.createEmptyBorder(3, 3, 3, 3));
    
    	outlookBarBorder = new FourLineBorder(color2, color2, color1, color1);
    }
    
    // border for buttons inside a JOutlookBar
    Border outlookButtonBorder;
    {
      Color color1 = barBackground.brighter();
      Color color2 = barBackground.darker();

      outlookButtonBorder = new BasicOutlookButtonUI.OutlookButtonBorder(
        color1, color2);
      outlookButtonBorder = new CompoundBorder(outlookButtonBorder,
        BorderFactory.createEmptyBorder(3, 3, 3, 3));
    }    

    defaults.addAll(Arrays.asList(new Object[] {
      JOutlookBar.UI_CLASS_ID,
      WindowsOutlookBarUI.class.getName(),
      "OutlookButtonUI",
      BasicOutlookButtonUI.class.getName(),
      "OutlookBar.background",
      barBackground,
      "OutlookBar.border",
      outlookBarBorder,
      "OutlookBar.tabButtonBorder",
      outlookBarButtonBorder,
      "OutlookButton.border",
      outlookButtonBorder,
      "OutlookBar.tabAlignment",
      new Integer(SwingConstants.CENTER),
    }));
  }

}