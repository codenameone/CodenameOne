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

import java.awt.Component;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

/**
 * ComboBoxPropertyEditor. <br>
 *  
 */
public class ComboBoxPropertyEditor extends AbstractPropertyEditor {

  private Object oldValue;
  private Icon[] icons;
  
  public ComboBoxPropertyEditor() {
    editor = new JComboBox() {
      public void setSelectedItem(Object anObject) {
        oldValue = getSelectedItem();
        super.setSelectedItem(anObject);
      }
    };
    
    final JComboBox combo = (JComboBox)editor;
    
    combo.setRenderer(new Renderer());
    combo.addPopupMenuListener(new PopupMenuListener() {
      public void popupMenuCanceled(PopupMenuEvent e) {}
      public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
        ComboBoxPropertyEditor.this.firePropertyChange(oldValue,
          combo.getSelectedItem());
      }
      public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}
    });
    combo.addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
          ComboBoxPropertyEditor.this.firePropertyChange(oldValue,
            combo.getSelectedItem());          
        }
      }
    });
    combo.setSelectedIndex(-1);
  }

  public Object getValue() {
    Object selected = ((JComboBox)editor).getSelectedItem();
    if (selected instanceof Value) {
      return ((Value)selected).value;
    } else {
      return selected;
    }
  }

  public void setValue(Object value) {
    JComboBox combo = (JComboBox)editor;
    Object current = null;
    int index = -1;
    for (int i = 0, c = combo.getModel().getSize(); i < c; i++) {
      current = combo.getModel().getElementAt(i);
      if (value == current || (current != null && current.equals(value))) {
        index = i;
        break;
      }
    }
    ((JComboBox)editor).setSelectedIndex(index);
  }

  public void setAvailableValues(Object[] values) {
    ((JComboBox)editor).setModel(new DefaultComboBoxModel(values));
  }

  public void setAvailableIcons(Icon[] icons) {
    this.icons = icons;
  }
  
  public class Renderer extends DefaultListCellRenderer {
    public Component getListCellRendererComponent(
    JList list,
    Object value,
    int index,
    boolean isSelected,
    boolean cellHasFocus) {
      Component component = super.getListCellRendererComponent(
          list,
          (value instanceof Value) ? ((Value)value).visualValue : value,
          index,
          isSelected,
          cellHasFocus);
      if (icons != null && index >= 0 && component instanceof JLabel)
        ((JLabel)component).setIcon(icons[index]);
      return component;
    }
  }

  public static final class Value {
    private Object value;
    private Object visualValue;
    public Value(Object value, Object visualValue) {
      this.value = value;
      this.visualValue = visualValue;
    }
    public boolean equals(Object o) {
      if (o == this)
        return true;
      if (value == o || (value != null && value.equals(o)))
        return true;
      return false;
    }
    public int hashCode() {
      return value==null?0:value.hashCode();
    }
  }
}
