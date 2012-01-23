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

import com.l2fprod.common.beans.ExtendedPropertyDescriptor;

import java.beans.PropertyDescriptor;
import java.beans.PropertyVetoException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * PropertyDescriptorAdapter.<br>
 *
 */
class PropertyDescriptorAdapter extends AbstractProperty {

  private PropertyDescriptor descriptor;
  
  public PropertyDescriptorAdapter() {
    super();
  }
  
  public PropertyDescriptorAdapter(PropertyDescriptor descriptor) {
    this();
    setDescriptor(descriptor);
  }

  public void setDescriptor(PropertyDescriptor descriptor) {
    this.descriptor = descriptor;
  }
  
  public PropertyDescriptor getDescriptor() {
    return descriptor;
  }
  
  public String getName() {
    return descriptor.getName();
  }
  
  public String getDisplayName() {
    return descriptor.getDisplayName();
  }
  
  public String getShortDescription() {
    return descriptor.getShortDescription();
  }

  public Class getType() {
    return descriptor.getPropertyType();
  }

  public Object clone() {
    PropertyDescriptorAdapter clone = new PropertyDescriptorAdapter(descriptor);
    clone.setValue(getValue());
    return clone;
  }
  
  public void readFromObject(Object object) {
    try {
      Method method = descriptor.getReadMethod();
      if (method != null) {
        setValue(method.invoke(object, null));
      }
    } catch (Exception e) {
      String message = "Got exception when reading property " + getName();
      if (object == null) {
        message += ", object was 'null'";
      } else {
        message += ", object was " + String.valueOf(object);
      }
      throw new RuntimeException(message, e);
    }
  }
  
  public void writeToObject(Object object) {
    try {
      Method method = descriptor.getWriteMethod();
      if (method != null) {
        method.invoke(object, new Object[]{getValue()});
      }
    } catch (Exception e) {
      // let PropertyVetoException go to the upper level without logging
      if (e instanceof InvocationTargetException &&
        ((InvocationTargetException)e).getTargetException() instanceof PropertyVetoException) {
        throw new RuntimeException(((InvocationTargetException)e).getTargetException());
      }

      String message = "Got exception when writing property " + getName();
      if (object == null) {
        message += ", object was 'null'";
      } else {
        message += ", object was " + String.valueOf(object);
      }
      throw new RuntimeException(message, e);
    }
  }
  
  public boolean isEditable() {
    return descriptor.getWriteMethod() != null;
  }

  public String getCategory() {
    if (descriptor instanceof ExtendedPropertyDescriptor) {
      return ((ExtendedPropertyDescriptor)descriptor).getCategory();
    } else {
      return null;
    }
  }
  
}
