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

//import com.l2fprod.common.swing.JDirectoryChooser;
import com.l2fprod.common.swing.UserPreferences;
import com.l2fprod.common.util.ResourceManager;

import java.io.File;
import java.io.IOException;

import javax.swing.JFileChooser;

/**
 * DirectoryPropertyEditor.<br>
 *
 */
public class DirectoryPropertyEditor extends FilePropertyEditor {

  protected void selectFile() {
    ResourceManager rm = ResourceManager.all(FilePropertyEditor.class);

    JFileChooser chooser = UserPreferences.getDefaultDirectoryChooser();
    
    chooser.setDialogTitle(rm.getString("DirectoryPropertyEditor.dialogTitle"));
    chooser.setApproveButtonText(
        rm.getString("DirectoryPropertyEditor.approveButtonText"));
    chooser.setApproveButtonMnemonic(
        rm.getChar("DirectoryPropertyEditor.approveButtonMnemonic"));

    File oldFile = (File)getValue();
    if (oldFile != null && oldFile.isDirectory()) {
      try {
        chooser.setCurrentDirectory(oldFile.getCanonicalFile());
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    
    if (JFileChooser.APPROVE_OPTION == chooser.showOpenDialog(editor)) {
      File newFile = chooser.getSelectedFile();
      String text = newFile.getAbsolutePath();
      textfield.setText(text);
      firePropertyChange(oldFile, newFile);
    }
  }
  
}
