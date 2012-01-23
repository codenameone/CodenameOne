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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;

/**
 * AbstractProperty. <br>
 *  
 */
public abstract class AbstractProperty implements Property {

  private Object value;
  
  // PropertyChangeListeners are not serialized.
  private transient PropertyChangeSupport listeners =
    new PropertyChangeSupport(this);

  public Object getValue() {
    return value;
  }

  public Object clone() {
    AbstractProperty clone = null;
    try {
      clone = (AbstractProperty)super.clone();
      return clone;
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);      
    }
  }
  
  public void setValue(Object value) {
    Object oldValue = this.value;
    this.value = value;
    if (value != oldValue && (value == null || !value.equals(oldValue)))
      firePropertyChange(oldValue, getValue());
  }

  protected void initializeValue(Object value) {
    this.value = value;
  }

  public void addPropertyChangeListener(PropertyChangeListener listener) {
    listeners.addPropertyChangeListener(listener);
    Property[] subProperties = getSubProperties();
    if (subProperties != null)
	  for ( int i = 0; i < subProperties.length; ++i )
	    subProperties[i].addPropertyChangeListener( listener );
  }

  public void removePropertyChangeListener(PropertyChangeListener listener) {
    listeners.removePropertyChangeListener(listener);
    Property[] subProperties = getSubProperties();
    if (subProperties != null)
	  for ( int i = 0; i < subProperties.length; ++i )
	    subProperties[i].removePropertyChangeListener( listener );
  }

  protected void firePropertyChange(Object oldValue, Object newValue) {
    listeners.firePropertyChange("value", oldValue, newValue);
  }

  private void readObject(java.io.ObjectInputStream in) throws IOException,
    ClassNotFoundException {
    in.defaultReadObject();
    listeners = new PropertyChangeSupport(this);    
  }
  
  public Property getParentProperty() {
  	return null;
  }
  
  public Property[] getSubProperties() {
  	return null;
  }
}
