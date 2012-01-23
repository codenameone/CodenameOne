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
import com.l2fprod.common.util.converter.ConverterRegistry;
import com.l2fprod.common.util.converter.NumberConverters;

import java.text.NumberFormat;

import javax.swing.JFormattedTextField;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;

/**
 * Base editor for numbers. <br>
 */
public class NumberPropertyEditor extends AbstractPropertyEditor {

  private final Class type;
  private Object lastGoodValue;
  
  public NumberPropertyEditor(Class type) {
    if (!Number.class.isAssignableFrom(type)) {
      throw new IllegalArgumentException("type must be a subclass of Number");
    }

    editor = new JFormattedTextField();
    this.type = type;
    ((JFormattedTextField)editor).setValue(getDefaultValue());
    ((JFormattedTextField)editor).setBorder(LookAndFeelTweaks.EMPTY_BORDER);

    // use a custom formatter to have numbers with up to 64 decimals
    NumberFormat format = NumberConverters.getDefaultFormat();

    ((JFormattedTextField) editor).setFormatterFactory(
        new DefaultFormatterFactory(new NumberFormatter(format))
    );
  }

  public Object getValue() {
    String text = ((JTextField)editor).getText();
    if (text == null || text.trim().length() == 0) {
      return getDefaultValue();
    }
    
    // allow comma or colon
    text = text.replace(',', '.');
    
    // collect all numbers from this textfield
    StringBuffer number = new StringBuffer();
    number.ensureCapacity(text.length());
    for (int i = 0, c = text.length(); i < c; i++) {
      char character = text.charAt(i);
      if ('.' == character || '-' == character
        || (Double.class.equals(type) && 'E' == character)
        || (Float.class.equals(type) && 'E' == character)
        || Character.isDigit(character)) {
        number.append(character);
      } else if (' ' == character) {
        continue;
      } else {
        break;
      }
    }
  
    try {
      lastGoodValue = ConverterRegistry.instance().convert(type,
        number.toString());      
    } catch (Exception e) {
      UIManager.getLookAndFeel().provideErrorFeedback(editor);
    }
    
    return lastGoodValue;
  }

  public void setValue(Object value) {
    if (value instanceof Number) {
      ((JFormattedTextField)editor).setText(value.toString());
    } else {
      ((JFormattedTextField)editor).setValue(getDefaultValue());
    }
    lastGoodValue = value;
  }

  private Object getDefaultValue() {
    try {
      return type.getConstructor(new Class[] {String.class}).newInstance(
        new Object[] {"0"});
    } catch (Exception e) {
      // will not happen
      throw new RuntimeException(e);
    }
  }

}