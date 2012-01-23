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

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Comparator;

/**
 * ExtendedPropertyDescriptor. <br>
 *  
 */
public class ExtendedPropertyDescriptor extends PropertyDescriptor {

  private Class tableCellRendererClass = null;
  private String category = "";

  public ExtendedPropertyDescriptor(String propertyName, Class beanClass)
    throws IntrospectionException {
    super(propertyName, beanClass);
  }

  public ExtendedPropertyDescriptor(
    String propertyName,
    Method getter,
    Method setter)
    throws IntrospectionException {
    super(propertyName, getter, setter);
  }

  public ExtendedPropertyDescriptor(
    String propertyName,
    Class beanClass,
    String getterName,
    String setterName)
    throws IntrospectionException {
    super(propertyName, beanClass, getterName, setterName);
  }

  /**
   * Sets this property category
   * 
   * @param category
   * @return this property for chaining calls.
   */
  public ExtendedPropertyDescriptor setCategory(String category) {
    this.category = category;
    return this;
  }

  /**
   * @return the category in which this property belongs
   */
  public String getCategory() {
    return category;
  }

  /**
   * Force this property to be readonly
   * 
   * @return this property for chaining calls.
   */
  public ExtendedPropertyDescriptor setReadOnly() {
    try {
      setWriteMethod(null);
    } catch (IntrospectionException e) {
      e.printStackTrace();
    }
    return this;
  }


  /**
   * You can associate a special tablecellrenderer with a particular
   * Property. If set to null default renderer will be used.
   *
   * @param tableCellRendererClass
   */
  public void setPropertyTableRendererClass(Class tableCellRendererClass) {
    this.tableCellRendererClass = tableCellRendererClass;
  }

  /**
   * @return null or a custom TableCellRenderer-Class for this property
   */
  public Class getPropertyTableRendererClass() {
    return (this.tableCellRendererClass);
  }

  public static ExtendedPropertyDescriptor newPropertyDescriptor(
    String propertyName,
    Class beanClass)
    throws IntrospectionException {
    // the same initialization phase as in the PropertyDescriptor
    Method readMethod = BeanUtils.getReadMethod(beanClass, propertyName);
    Method writeMethod = null;

    if (readMethod == null) {
      throw new IntrospectionException(
        "No getter for property "
          + propertyName
          + " in class "
          + beanClass.getName());
    }

    writeMethod =
      BeanUtils.getWriteMethod(
        beanClass,
        propertyName,
        readMethod.getReturnType());

    return new ExtendedPropertyDescriptor(
      propertyName,
      readMethod,
      writeMethod);
  }

  public static final Comparator BY_CATEGORY_COMPARATOR = new Comparator() {
    public int compare(Object o1, Object o2) {
      PropertyDescriptor desc1 = (PropertyDescriptor)o1;
      PropertyDescriptor desc2 = (PropertyDescriptor)o2;

      if (desc1 == null && desc2 == null) {
        return 0;
      } else if (desc1 != null && desc2 == null) {
        return 1;
      } else if (desc1 == null && desc2 != null) {
        return -1;
      } else {
        if (desc1 instanceof ExtendedPropertyDescriptor
          && !(desc2 instanceof ExtendedPropertyDescriptor)) {
          return -1;
        } else if (
          !(desc1 instanceof ExtendedPropertyDescriptor)
            && desc2 instanceof ExtendedPropertyDescriptor) {
          return 1;
        } else if (
          !(desc1 instanceof ExtendedPropertyDescriptor)
            && !(desc2 instanceof ExtendedPropertyDescriptor)) {
          return String.CASE_INSENSITIVE_ORDER.compare(
            desc1.getDisplayName(),
            desc2.getDisplayName());
        } else {
          int category =
            String.CASE_INSENSITIVE_ORDER.compare(
              ((ExtendedPropertyDescriptor)desc1).getCategory() == null
                ? ""
                : ((ExtendedPropertyDescriptor)desc1).getCategory(),
              ((ExtendedPropertyDescriptor)desc2).getCategory() == null
                ? ""
                : ((ExtendedPropertyDescriptor)desc2).getCategory());
          if (category == 0) {
            return String.CASE_INSENSITIVE_ORDER.compare(
              desc1.getDisplayName(),
              desc2.getDisplayName());
          } else {
            return category;
          }
        }
      }
    }
  };

}
