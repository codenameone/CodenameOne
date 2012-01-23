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

import com.l2fprod.common.beans.editor.FixedButton;

import javax.swing.JButton;
import javax.swing.JComboBox;

public interface ComponentFactory {

  JButton createMiniButton();

  JComboBox createComboBox();

  public static class Helper {

    static ComponentFactory factory = new DefaultComponentFactory();

    public static ComponentFactory getFactory() {
      return factory;
    }
    
    public static void setFactory(ComponentFactory factory) {
      Helper.factory = factory;
    }
  }

  public static class DefaultComponentFactory implements ComponentFactory {
    public JButton createMiniButton() {
      return new FixedButton();
    }
    public JComboBox createComboBox() {
      return new JComboBox();
    }
  }
}
