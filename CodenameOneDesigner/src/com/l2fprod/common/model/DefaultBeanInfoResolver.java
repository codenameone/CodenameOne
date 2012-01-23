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
package com.l2fprod.common.model;

import com.l2fprod.common.beans.BeanInfoResolver;

import java.beans.BeanInfo;

/**
 * DefaultBeanInfoResolver. <br>
 *  
 */
public class DefaultBeanInfoResolver implements BeanInfoResolver {

  public DefaultBeanInfoResolver() {
    super();
  }

  public BeanInfo getBeanInfo(Object object) {
    if (object == null) {
      return null;
    }

    return getBeanInfo(object.getClass());
  }

  public BeanInfo getBeanInfo(Class clazz) {
    if (clazz == null) {
      return null;
    }

    String classname = clazz.getName();

    // look for .impl.basic., remove it and call getBeanInfo(class)
    int index = classname.indexOf(".impl.basic");
    if (index != -1 && classname.endsWith("Basic")) {
      classname =
        classname.substring(0, index)
          + classname.substring(
            index + ".impl.basic".length(),
            classname.lastIndexOf("Basic"));
      try {
        return getBeanInfo(Class.forName(classname));
      } catch (ClassNotFoundException e) {
        return null;
      }
    } else {
      try {
        BeanInfo beanInfo =
          (BeanInfo)Class.forName(classname + "BeanInfo").newInstance();
        return beanInfo;
      } catch (Exception e) {
        return null;
      }
    }
  }

}
