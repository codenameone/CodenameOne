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
package com.l2fprod.common.propertysheet;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyEditor;
import java.util.EventObject;

import javax.swing.AbstractCellEditor;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellEditor;
import javax.swing.tree.TreeCellEditor;

/**
 * Allows to use any PropertyEditor as a Table or Tree cell editor. <br>
 */
public class CellEditorAdapter
  extends AbstractCellEditor
  implements TableCellEditor, TreeCellEditor {

  protected PropertyEditor editor;
  protected int clickCountToStart = 1;

  class CommitEditing implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      stopCellEditing();
    }
  }

  class CancelEditing implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      CellEditorAdapter.this.cancelCellEditing();
    }
  }

  /**
   * Select all text when focus gained, deselect when focus lost.
   */
  class SelectOnFocus implements FocusListener {
    public void focusGained(final FocusEvent e) {
      if (! (e.getSource() instanceof JTextField))
        return;
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          ((JTextField) e.getSource()).selectAll();
        }
      });
    }
    public void focusLost(final FocusEvent e) {
      if (! (e.getSource() instanceof JTextField))
        return;
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          ((JTextField) e.getSource()).select(0, 0);
        }
      });
    }
  }
  
  public CellEditorAdapter(PropertyEditor editor) {
    this.editor = editor;
    Component component = editor.getCustomEditor();
    if (component instanceof JTextField) {
      JTextField field = (JTextField)component;
      field.addFocusListener(new SelectOnFocus());
      field.addActionListener(new CommitEditing());
      field.registerKeyboardAction(
        new CancelEditing(),
        KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
        JComponent.WHEN_FOCUSED);
    }

    // when the editor notifies a change, commit the changes
    editor.addPropertyChangeListener(new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent evt) {       
        stopCellEditing();
      }
    });
  }
  
  public Component getTreeCellEditorComponent(
    JTree tree,
    Object value,
    boolean selected,
    boolean expanded,
    boolean leaf,
    int row) {
    return getEditor(value);
  }

  public Component getTableCellEditorComponent(
    JTable table,
    Object value,
    boolean selected,
    int row,
    int column) {
    return getEditor(value);
  }

  public void setClickCountToStart(int count) {
    clickCountToStart = count;
  }

  public int getClickCountToStart() {
    return clickCountToStart;
  }

  public Object getCellEditorValue() {
    return editor.getValue();
  }

  public boolean isCellEditable(EventObject event) {
    if (event instanceof MouseEvent) {
      return ((MouseEvent)event).getClickCount() >= clickCountToStart;
    }
    return true;
  }

  public boolean shouldSelectCell(EventObject event) {
    return true;
  }

  public boolean stopCellEditing() {
    fireEditingStopped();
    return true;
  }

  public void cancelCellEditing() {
    fireEditingCanceled();
  }

  private Component getEditor(Object value) {
    editor.setValue(value);

    final Component cellEditor = editor.getCustomEditor();

    // request focus later so the editor can be used to enter value as soon as
    // made visible
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        cellEditor.requestFocus();
      }
    });

    return cellEditor;
  }

}
