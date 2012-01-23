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

import com.l2fprod.common.swing.LookAndFeelTweaks;

import javax.swing.JTextField;
import javax.swing.text.JTextComponent;

/**
 * StringPropertyEditor.<br>
 *
 */
public class StringPropertyEditor extends AbstractPropertyEditor {

  public StringPropertyEditor() {
    editor = new JTextField();
    ((JTextField)editor).setBorder(LookAndFeelTweaks.EMPTY_BORDER);
  }
  
  public Object getValue() {
    return ((JTextComponent)editor).getText();
  }
  
  public void setValue(Object value) {
    if (value == null) {
      ((JTextComponent)editor).setText("");
    } else {
      ((JTextComponent)editor).setText(String.valueOf(value));
    }
  }
  
}
