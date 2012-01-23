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
package com.l2fprod.common.swing;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.text.JTextComponent;
import javax.swing.text.View;
import javax.swing.text.html.HTMLDocument;

/**
 * LookAndFeelTweaks. <br>
 * 
 */
public class LookAndFeelTweaks {

  public final static Border PANEL_BORDER = BorderFactory.createEmptyBorder(3,
      3, 3, 3);

  public final static Border WINDOW_BORDER = BorderFactory.createEmptyBorder(4,
      10, 10, 10);

  public final static Border EMPTY_BORDER = BorderFactory.createEmptyBorder();

  public static void tweak() {
    Object listFont = UIManager.get("List.font");
    UIManager.put("Table.font", listFont);
    UIManager.put("ToolTip.font", listFont);
    UIManager.put("TextField.font", listFont);
    UIManager.put("FormattedTextField.font", listFont);
    UIManager.put("Viewport.background", "Table.background");
  }

  public static PercentLayout createVerticalPercentLayout() {
    return new PercentLayout(PercentLayout.VERTICAL, 8);
  }

  public static PercentLayout createHorizontalPercentLayout() {
    return new PercentLayout(PercentLayout.HORIZONTAL, 8);
  }

  public static ButtonAreaLayout createButtonAreaLayout() {
    return new ButtonAreaLayout(6);
  }

  public static BorderLayout createBorderLayout() {
    return new BorderLayout(8, 8);
  }

  public static void setBorder(JComponent component) {
    if (component instanceof JPanel) {
      component.setBorder(PANEL_BORDER);
    }
  }

  public static void setBorderLayout(Container container) {
    container.setLayout(new BorderLayout(3, 3));
  }

  public static void makeBold(JComponent component) {
    component.setFont(component.getFont().deriveFont(Font.BOLD));
  }

  public static void makeMultilineLabel(JTextComponent area) {
    area.setFont(UIManager.getFont("Label.font"));
    area.setEditable(false);
    area.setOpaque(false);
    if (area instanceof JTextArea) {
      ((JTextArea) area).setWrapStyleWord(true);
      ((JTextArea) area).setLineWrap(true);
    }
  }

  public static void htmlize(JComponent component) {
    htmlize(component, UIManager.getFont("Button.font"));
  }

  public static void htmlize(JComponent component, Font font) {
    String stylesheet = "body { margin-top: 0; margin-bottom: 0; margin-left: 0; margin-right: 0; font-family: "
        + font.getName()
        + "; font-size: "
        + font.getSize()
        + "pt;	}"
        + "a, p, li { margin-top: 0; margin-bottom: 0; margin-left: 0; margin-right: 0; font-family: "
        + font.getName() + "; font-size: " + font.getSize() + "pt;	}";

    try {
      HTMLDocument doc = null;
      if (component instanceof JEditorPane) {
        if (((JEditorPane) component).getDocument() instanceof HTMLDocument) {
          doc = (HTMLDocument) ((JEditorPane) component).getDocument();
        }
      } else {
        View v = (View) component
            .getClientProperty(javax.swing.plaf.basic.BasicHTML.propertyKey);
        if (v != null && v.getDocument() instanceof HTMLDocument) {
          doc = (HTMLDocument) v.getDocument();
        }
      }
      if (doc != null) {
        doc.getStyleSheet().loadRules(new java.io.StringReader(stylesheet),
            null);
      } // end of if (doc != null)
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static Border addMargin(Border border) {
    return new CompoundBorder(border, PANEL_BORDER);
  }

}
