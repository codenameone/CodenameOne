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
package com.l2fprod.common.util;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Manages application resources. <br>
 */
public class ResourceManager {

  static Map nameToRM = new HashMap();

  private ResourceBundle bundle;

  /**
   * Gets the ResourceManager associated with <code>clazz</code>.
   * It looks for a ResourceBundle named against the class name plus
   * the string "RB". For example, for the com.mypackage.Main, the
   * ResourceBundle com.mypackage.MainRB will be looked up.
   * 
   * @param clazz
   * @return the ResourceManager associated with the class
   */
  public static ResourceManager get(Class clazz) {
    String bundleName = clazz.getName() + "RB";
    return get(bundleName);
  }

  /**
   * Gets the ResourceManager with the given name.
   * 
   * @param bundleName
   * @return the ResourceManager with the given name.
   */
  public static ResourceManager get(String bundleName) {
    ResourceManager rm = (ResourceManager)nameToRM.get(bundleName);
    if (rm == null) {
      ResourceBundle rb = ResourceBundle.getBundle(bundleName);
      rm = new ResourceManager(rb);
      nameToRM.put(bundleName, rm);
    }
    return rm;
  }

  /**
   * @param clazz
   * @return the "AllRB" in the class package
   */
  public static ResourceManager all(Class clazz) {
    return get(getPackage(clazz) + ".AllRB");
  }

  /**
   * Gets the default ResourceManager. This is equivalent to
   * <code>all(ResourceManager.class)</code>. It returns the
   * ResourceManager named "AllRB" located in the same package
   * ResourceManager class (i.e com.l2fprod.common.util.AllRB).
   * 
   * @return the default ResourceManager
   */
  public static ResourceManager common() {
    return all(ResourceManager.class);
  }

  /**
   * @return the default ResourceManager for ui specific resources.
   */
  public static ResourceManager ui() {
    return get("com.l2fprod.common.swing.AllRB");
  }
  
  /**
   * Resolves any references to a resource bundle contained in
   * <code>rbAndProperty</code>. To reference a resource bundle
   * inside a property use <code>${com.package.FileRB:key}</code>,
   * this will look for <code>key</code> in the ResourceBundle
   * <code>com.package.FileRB</code>.
   * 
   * @param rbAndProperty
   * @return the resolved resource or rbAndProperty if no resource was
   *         found
   */
  public static String resolve(String rbAndProperty) {
    return common().resolve0(rbAndProperty);
  }

  /**
   * Same as {@link #resolve(String)} but once the value as been
   * resolved, a MessageFormatter is applied with the given
   * <code>args</code>.
   * 
   * @param rbAndProperty
   * @param args
   * @return the value for the resource parametrized by args
   */
  public static String resolve(String rbAndProperty, Object[] args) {
    String value = common().resolve0(rbAndProperty);
    return MessageFormat.format(value, args);
  }

  /**
   * Can't be directly constructed
   * 
   * @param bundle
   */
  private ResourceManager(ResourceBundle bundle) {
    this.bundle = bundle;
  }

  /**
   * Gets the String associated with <code>key</code> after having
   * resolved any nested keys ({@link #resolve(String)}).
   * 
   * @param key the key to lookup
   * @return the String associated with <code>key</code>
   */
  public String getString(String key) {
    return resolve0(String.valueOf(bundle.getObject(key)));
  }

  /**
   * Gets the String associated with <code>key</code> after having
   * resolved any nested keys ({@link #resolve(String)}) and applied
   * a formatter using the given <code>args</code>.
   * 
   * @param key the key to lookup
   * @param args the arguments to pass to the formatter
   * @return the String associated with <code>key</code>
   */
  public String getString(String key, Object[] args) {
    String value = getString(key);
    return MessageFormat.format(value, args);
  }

  /**
   * Gets the first character of the String associated with
   * <code>key</code>.
   * 
   * @param key the key to lookup
   * @return the first character of the String associated with
   *         <code>key</code>.
   */
  public char getChar(String key) {
    String s = getString(key);
    if (s == null || s.trim().length() == 0) {
      return (char)0;
    } else {
      return s.charAt(0);
    }
  }

  private String resolve0(String property) {
    String result = property;
    if (property != null) {
      int index = property.indexOf("${");
      if (index != -1) {
        int endIndex = property.indexOf("}", index);
        String sub = property.substring(index + 2, endIndex);
        // check if sub contains a reference to another RB, key
        int colon = sub.indexOf(":");
        if (colon != -1) {
          String rbName = sub.substring(0, colon);
          String keyName = sub.substring(colon + 1);
          sub = get(rbName).getString(keyName);
        } else {
          // it's a regular nested property
          sub = getString(sub);
        }
        result = property.substring(0, index) + sub
            + resolve0(property.substring(endIndex + 1));
      }
    }
    return result;
  }

  private static String getPackage(Class clazz) {
    String pck = clazz.getName();
    int index = pck.lastIndexOf('.');
    if (index != -1) {
      pck = pck.substring(0, index);
    } else {
      pck = "";
    }
    return pck;
  }

}