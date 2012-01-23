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

import java.beans.BeanDescriptor;
import java.util.MissingResourceException;

/**
 * DefaultBeanDescriptor. <br>
 *  
 */
final class DefaultBeanDescriptor extends BeanDescriptor {

  private String displayName;

  public DefaultBeanDescriptor(BaseBeanInfo beanInfo) {
    super(beanInfo.getType());
    try {
      setDisplayName(beanInfo.getResources().getString("beanName"));
    } catch (MissingResourceException e) {
      // this resource is not mandatory
    }
    try {
      setShortDescription(beanInfo.getResources().getString("beanDescription"));
    } catch (MissingResourceException e) {
      // this resource is not mandatory
    }
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String p_name) {
    displayName = p_name;
  }

}
