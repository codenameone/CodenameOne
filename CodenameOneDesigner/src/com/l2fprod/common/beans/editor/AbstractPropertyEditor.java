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
import java.awt.Graphics;
import java.awt.Rectangle;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyEditor;

/**
 * AbstractPropertyEditor. <br>
 *  
 */
public class AbstractPropertyEditor implements PropertyEditor {

  protected Component editor;
  private PropertyChangeSupport listeners = new PropertyChangeSupport(this);

  public boolean isPaintable() {
    return false;
  }

  public boolean supportsCustomEditor() {
    return false;
  }

  public Component getCustomEditor() {
    return editor;
  }

  public void addPropertyChangeListener(PropertyChangeListener listener) {
    listeners.addPropertyChangeListener(listener);
  }

  public void removePropertyChangeListener(PropertyChangeListener listener) {
    listeners.removePropertyChangeListener(listener);
  }

  protected void firePropertyChange(Object oldValue, Object newValue) {
    listeners.firePropertyChange("value", oldValue, newValue);
  }
    
  public Object getValue() {
    return null;
  }

  public void setValue(Object value) {
  }

  public String getAsText() {
    return null;
  }

  public String getJavaInitializationString() {
    return null;
  }

  public String[] getTags() {
    return null;
  }

  public void setAsText(String text) throws IllegalArgumentException {
  }

  public void paintValue(Graphics gfx, Rectangle box) {
  }

}
