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

import com.l2fprod.common.beans.BeanUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * DefaultProperty. <br>
 *  
 */
public class DefaultProperty extends AbstractProperty {

  private String name;
  private String displayName;
  private String shortDescription;
  private Class type;
  private boolean editable = true;
  private String category;
  private Property parent;
  private List subProperties = new ArrayList();

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getShortDescription() {
    return shortDescription;
  }

  public void setShortDescription(String shortDescription) {
    this.shortDescription = shortDescription;
  }

  public Class getType() {
    return type;
  }

  public void setType(Class type) {
    this.type = type;
  }

  public boolean isEditable() {
    return editable;
  }

  public void setEditable(boolean editable) {
    this.editable = editable;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  /**
   * Reads the value of this Property from the given object. It uses reflection
   * and looks for a method starting with "is" or "get" followed by the
   * capitalized Property name.
   */
  public void readFromObject(Object object) {
    try {
      Method method = BeanUtils.getReadMethod(object.getClass(), getName());
      if (method != null) {
    	Object value = method.invoke(object, null);
        initializeValue(value); // avoid updating parent or firing property change
        if (value != null) {
          for (Iterator iter = subProperties.iterator(); iter.hasNext();) {
            Property subProperty = (Property)iter.next();
            subProperty.readFromObject(value);
          }
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Writes the value of the Property to the given object. It uses reflection
   * and looks for a method starting with "set" followed by the capitalized
   * Property name and with one parameter with the same type as the Property.
   */
  public void writeToObject(Object object) {
    try {
      Method method =
        BeanUtils.getWriteMethod(object.getClass(), getName(), getType());
      if (method != null) {
        method.invoke(object, new Object[] { getValue()});
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  /* (non-Javadoc)
   * @see com.l2fprod.common.propertysheet.Property#setValue(java.lang.Object)
   */
  public void setValue( Object value ) {
	super.setValue( value );
	if ( parent != null ) {
	  Object parentValue = parent.getValue();
	  if (parentValue != null) {
        writeToObject(parentValue);
        parent.setValue(parentValue);
	  }
	}
    if (value != null) {
        for (Iterator iter = subProperties.iterator(); iter.hasNext();) {
          Property subProperty = (Property)iter.next();
          subProperty.readFromObject(value);
        }
      }
  }

  public int hashCode() {
    return 28 + ((name != null)?name.hashCode():3)
      + ((displayName != null)?displayName.hashCode():94)
      + ((shortDescription != null)?shortDescription.hashCode():394)
      + ((category != null)?category.hashCode():34)
      + ((type != null)?type.hashCode():39)
      + Boolean.valueOf(editable).hashCode();
  }
      
  /**
   * Compares two DefaultProperty objects. Two DefaultProperty objects are equal
   * if they are the same object or if their name, display name, short
   * description, category, type and editable property are the same. Note the
   * property value is not considered in the implementation.
   */
  public boolean equals(Object other) {
    if (other == null || getClass() != other.getClass()) {
      return false;
    }
          
    if (other == this) {
      return true;
    }
          
    DefaultProperty dp = (DefaultProperty) other;
    
    return compare(name, dp.name) &&
      compare(displayName, dp.displayName) &&
      compare(shortDescription, dp.shortDescription) &&
      compare(category, dp.category) &&
      compare(type, dp.type) &&
      editable == dp.editable;
  }
      
  private boolean compare(Object o1, Object o2) {
    return (o1 != null)? o1.equals(o2) : o2 == null;
  }
      
  public String toString() {
    return "name=" + getName() + ", displayName=" + getDisplayName()
      + ", type=" + getType() + ", category=" + getCategory() + ", editable="
      + isEditable() + ", value=" + getValue();
  }

  public Property getParentProperty() {
  	return parent;
  }
  
  public void setParentProperty( Property parent ) {
    this.parent = parent;
  }
  
  public Property[] getSubProperties() {
  	return (Property[]) subProperties.toArray( new Property[subProperties.size()] );
  }
  
  public void clearSubProperties() {
	for (Iterator iter = this.subProperties.iterator(); iter.hasNext();) {
		Property subProp = (Property) iter.next();
		if (subProp instanceof DefaultProperty)
			((DefaultProperty)subProp).setParentProperty(null);
	}
	this.subProperties.clear();
  }
  
  public void addSubProperties( Collection subProperties ) {
	this.subProperties.addAll( subProperties );
	for (Iterator iter = this.subProperties.iterator(); iter.hasNext();) {
		Property subProp = (Property) iter.next();
		if (subProp instanceof DefaultProperty)
			((DefaultProperty)subProp).setParentProperty(this);
	}
  }
  
  public void addSubProperties( Property[] subProperties ) {
	this.addSubProperties( Arrays.asList( subProperties ) );
  }
  
  public void addSubProperty( Property subProperty ) {
	this.subProperties.add( subProperty );
	if (subProperty instanceof DefaultProperty)
		((DefaultProperty)subProperty).setParentProperty(this);
  }
}