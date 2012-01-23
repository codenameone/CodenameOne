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
import com.l2fprod.common.swing.LookAndFeelTweaks;
import com.l2fprod.common.swing.PercentLayout;
import com.l2fprod.common.swing.UserPreferences;
import com.l2fprod.common.util.ResourceManager;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.TransferHandler;

/**
 * FilePropertyEditor. <br>
 *  
 */
public class FilePropertyEditor extends AbstractPropertyEditor {

  protected JTextField textfield;
  private JButton button;
  private JButton cancelButton;
    
  public FilePropertyEditor() {
    this(true);
  }
  
  public FilePropertyEditor(boolean asTableEditor) {
    editor = new JPanel(new PercentLayout(PercentLayout.HORIZONTAL, 0)) {
      public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        textfield.setEnabled(enabled);
        button.setEnabled(enabled);
        cancelButton.setEnabled(enabled);
      }
    };
    ((JPanel)editor).add("*", textfield = new JTextField());
    ((JPanel)editor).add(button = ComponentFactory.Helper.getFactory()
      .createMiniButton());
    if (asTableEditor) {
      textfield.setBorder(LookAndFeelTweaks.EMPTY_BORDER);
    }    
    button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        selectFile();
      }
    });
    ((JPanel)editor).add(cancelButton = ComponentFactory.Helper.getFactory()
      .createMiniButton());
    cancelButton.setText("X");
    cancelButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        selectNull();
      }
    });
    textfield.setTransferHandler(new FileTransferHandler());
  }

  class FileTransferHandler extends TransferHandler {    
    public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
      for (int i = 0, c = transferFlavors.length; i < c; i++) {
        if (transferFlavors[i].equals(DataFlavor.javaFileListFlavor)) {
          return true;
        }
      }
      return false;
    }
    public boolean importData(JComponent comp, Transferable t) {
      try {
        List list = (List)t.getTransferData(DataFlavor.javaFileListFlavor);
        if (list.size() > 0) {
          File oldFile = (File)getValue();
          File newFile = (File)list.get(0);
          String text = newFile.getAbsolutePath();
          textfield.setText(text);
          firePropertyChange(oldFile, newFile);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      return true;
    }
  }
  
  public Object getValue() {
    if ("".equals(textfield.getText().trim())) {
      return null;
    } else {
      return new File(textfield.getText());
    }
  }

  public void setValue(Object value) {
    if (value instanceof File) {
      textfield.setText(((File)value).getAbsolutePath());
    } else {
      textfield.setText("");
    }
  }

  protected void selectFile() {
    ResourceManager rm = ResourceManager.all(FilePropertyEditor.class);

    JFileChooser chooser = UserPreferences.getDefaultFileChooser();
    chooser.setDialogTitle(rm.getString("FilePropertyEditor.dialogTitle"));
    chooser.setApproveButtonText(
      rm.getString("FilePropertyEditor.approveButtonText"));
    chooser.setApproveButtonMnemonic(
      rm.getChar("FilePropertyEditor.approveButtonMnemonic"));
    customizeFileChooser(chooser);
    
    if (JFileChooser.APPROVE_OPTION == chooser.showOpenDialog(editor)) {
      File oldFile = (File)getValue();
      File newFile = chooser.getSelectedFile();
      String text = newFile.getAbsolutePath();
      textfield.setText(text);
      firePropertyChange(oldFile, newFile);
    }
  }

  /**
   * Placeholder for subclasses to customize the JFileChooser shown to select a
   * file.
   * 
   * @param chooser
   */
  protected void customizeFileChooser(JFileChooser chooser) {    
  }
  
  protected void selectNull() {
    Object oldFile = getValue();
    textfield.setText("");
    firePropertyChange(oldFile, null);
  }
  
}