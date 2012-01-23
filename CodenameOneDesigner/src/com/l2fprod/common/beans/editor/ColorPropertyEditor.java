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
package com.l2fprod.common.beans.editor;

import com.l2fprod.common.swing.ComponentFactory;
import com.l2fprod.common.swing.PercentLayout;
import com.l2fprod.common.swing.renderer.ColorCellRenderer;
import com.l2fprod.common.util.ResourceManager;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JPanel;

/**
 * ColorPropertyEditor. <br>
 *  
 */
public class ColorPropertyEditor extends AbstractPropertyEditor {

  private ColorCellRenderer label;
  private JButton button;
  private Color color;

  public ColorPropertyEditor() {
    editor = new JPanel(new PercentLayout(PercentLayout.HORIZONTAL, 0));
    ((JPanel)editor).add("*", label = new ColorCellRenderer());
    label.setOpaque(false);
    ((JPanel)editor).add(button = ComponentFactory.Helper.getFactory()
      .createMiniButton());
    button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        selectColor();
      }
    });
    ((JPanel)editor).add(button = ComponentFactory.Helper.getFactory()
      .createMiniButton());
    button.setText("X");
    button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        selectNull();
      }
    });
    ((JPanel)editor).setOpaque(false);
  }

  public Object getValue() {
    return color;
  }

  public void setValue(Object value) {
    color = (Color)value;
    label.setValue(color);
  }

  protected void selectColor() {
    ResourceManager rm = ResourceManager.all(FilePropertyEditor.class);
    String title = rm.getString("ColorPropertyEditor.title");
    Color selectedColor = JColorChooser.showDialog(editor, title, color);

    if (selectedColor != null) {
      Color oldColor = color;
      Color newColor = selectedColor;
      label.setValue(newColor);
      color = newColor;
      firePropertyChange(oldColor, newColor);
    }
  }

  protected void selectNull() {
    Color oldColor = color;
    label.setValue(null);
    color = null;
    firePropertyChange(oldColor, null);
  }

  public static class AsInt extends ColorPropertyEditor {
    public void setValue(Object arg0) {
      if (arg0 instanceof Integer) {
        super.setValue(new Color(((Integer)arg0).intValue()));
      } else {
        super.setValue(arg0);
      }
    }
    
    public Object getValue() {
      Object value = super.getValue();
      if (value == null) {
        return null;
      } else {
        return new Integer(((Color)value).getRGB());
      }
    }
    
    protected void firePropertyChange(Object oldValue, Object newValue) {
      if (oldValue instanceof Color) {
        oldValue = new Integer(((Color)oldValue).getRGB());
      }
      if (newValue instanceof Color) {
        newValue = new Integer(((Color)newValue).getRGB());
      }
      super.firePropertyChange(oldValue, newValue);
    }
  }
}
