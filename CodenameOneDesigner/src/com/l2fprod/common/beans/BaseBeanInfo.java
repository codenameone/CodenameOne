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
package com.l2fprod.common.beans;

import com.l2fprod.common.util.ResourceManager;

import java.awt.Image;
import java.beans.BeanDescriptor;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.MissingResourceException;

/**
 * A convenient class to build beaninfos by adding and removing
 * properties. <br>
 */
public class BaseBeanInfo extends SimpleBeanInfo {

  private Class type;

  private BeanDescriptor beanDescriptor;

  private List properties = new ArrayList(0);

  public BaseBeanInfo(Class type) {
    this.type = type;
  }

  public final Class getType() {
    return type;
  }

  public ResourceManager getResources() {
    return ResourceManager.get(getType());
  }

  public BeanDescriptor getBeanDescriptor() {
    if (beanDescriptor == null) {
      beanDescriptor = new DefaultBeanDescriptor(this);
    }
    return beanDescriptor;
  }

  public PropertyDescriptor[] getPropertyDescriptors() {
    return (PropertyDescriptor[])properties.toArray(new PropertyDescriptor[0]);
  }

  public int getPropertyDescriptorCount() {
    return properties.size();
  }

  public PropertyDescriptor getPropertyDescriptor(int index) {
    return (PropertyDescriptor)properties.get(index);
  }

  protected PropertyDescriptor addPropertyDescriptor(PropertyDescriptor property) {
    properties.add(property);
    return property;
  }

  public ExtendedPropertyDescriptor addProperty(String propertyName) {
    ExtendedPropertyDescriptor descriptor;
    try {
      if (propertyName == null || propertyName.trim().length() == 0) {
        throw new IntrospectionException("bad property name");
      }

      descriptor = ExtendedPropertyDescriptor
       .newPropertyDescriptor(propertyName, getType());
      
      try {
        descriptor.setDisplayName(getResources().getString(propertyName));
      } catch (MissingResourceException e) {
        // ignore, the resource may not be provided
      }
      try {
        descriptor.setShortDescription(
          getResources().getString(
            propertyName + ".shortDescription"));
      } catch (MissingResourceException e) {
        // ignore, the resource may not be provided
      }
      addPropertyDescriptor(descriptor);
      return descriptor;
    } catch (IntrospectionException e) {
      throw new RuntimeException(e);
    }
  }
  
  /**
   * Removes the first occurrence of the property named <code>propertyName</code>
   * @param propertyName
   * @return the removed PropertyDescriptor or null if not found.
   */
  public PropertyDescriptor removeProperty(String propertyName) {
    if (propertyName == null) {
      throw new IllegalArgumentException("Property name can not be null");
    }
    for (Iterator iter = properties.iterator(); iter.hasNext();) {
      PropertyDescriptor property = (PropertyDescriptor)iter.next();
      if (propertyName.equals(property.getName())) {
        // remove the property from the list
        iter.remove();
        return property;
      }
    }
    return null;
  }
  
  /**
   * Get the icon for displaying this bean.
   * 
   * @param kind Kind of icon.
   * @return Image for bean, or null if none.
   */
  public Image getIcon(int kind) {
    return null;
  }

  /**
   * Return a text describing the object.
   * 
   * @param value an <code>Object</code> value
   * @return a text describing the object.
   */
  public String getText(Object value) {
    return value.toString();
  }

  /**
   * Return a text describing briefly the object. The text will be used
   * whereever a explanation is needed to give to the user
   * 
   * @param value an <code>Object</code> value
   * @return a <code>String</code> value
   */
  public String getDescription(Object value) {
    return getText(value);
  }

  /**
   * Return a text describing the object. The text will be displayed in a
   * tooltip.
   * 
   * @param value an <code>Object</code> value
   * @return a <code>String</code> value
   */
  public String getToolTipText(Object value) {
    return getText(value);
  }

}
