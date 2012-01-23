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
import java.io.Serializable;

/**
 * Property. <br>Component of a PropertySheet, based on the
 * java.beans.PropertyDescriptor for easy wrapping of beans in PropertySheet.
 */
public interface Property extends Serializable, Cloneable {

  public String getName();
  
  public String getDisplayName();
  
  public String getShortDescription();
  
  public Class getType();

  public Object getValue();
  
  public void setValue(Object value);
  
  public boolean isEditable();
  
  public String getCategory();

  public void readFromObject(Object object);
  
  public void writeToObject(Object object);
  
  public void addPropertyChangeListener(PropertyChangeListener listener);
  
  public void removePropertyChangeListener(PropertyChangeListener listener);

  public Object clone();
  
  public Property getParentProperty();
  
  public Property[] getSubProperties();
}
