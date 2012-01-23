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

import java.lang.reflect.Method;

/**
 * BeanUtils. <br>
 *  
 */
public class BeanUtils {

  private BeanUtils() {
  }

  public static Method getReadMethod(Class clazz, String propertyName) {
    Method readMethod = null;
    String base = capitalize(propertyName);

    // Since there can be multiple setter methods but only one getter
    // method, find the getter method first so that you know what the
    // property type is. For booleans, there can be "is" and "get"
    // methods. If an "is" method exists, this is the official
    // reader method so look for this one first.
    try {
      readMethod = clazz.getMethod("is" + base, null);
    } catch (Exception getterExc) {
      try {
        // no "is" method, so look for a "get" method.
        readMethod = clazz.getMethod("get" + base, null);
      } catch (Exception e) {
        // no is and no get, we will return null
      }
    }

    return readMethod;
  }

  public static Method getWriteMethod(
    Class clazz,
    String propertyName,
    Class propertyType) {
    Method writeMethod = null;
    String base = capitalize(propertyName);

    Class params[] = { propertyType };
    try {
      writeMethod = clazz.getMethod("set" + base, params);
    } catch (Exception e) {
      // no write method
    }

    return writeMethod;
  }

  private static String capitalize(String s) {
    if (s.length() == 0) {
      return s;
    } else {
      char chars[] = s.toCharArray();
      chars[0] = Character.toUpperCase(chars[0]);
      return String.valueOf(chars);
    }
  }

}
