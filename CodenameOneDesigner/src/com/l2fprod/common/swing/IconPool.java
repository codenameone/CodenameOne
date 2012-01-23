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
package com.l2fprod.common.swing;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 * IconPool.<br>
 *
 */
public class IconPool {

  private static IconPool iconPool = new IconPool();
  
  private Map pool;
  
  public IconPool() {
    pool = new HashMap();    
  }

  public static IconPool shared() {
    return iconPool;
  }
  
  /**
   * Gets the icon denoted by url.
   * If url is relative, it is relative to the caller.
   * 
   * @param url
   * @return an icon
   */
  public Icon get(String url) {
    StackTraceElement[] stacks = new Exception().getStackTrace();
    try {
      Class callerClazz = Class.forName(stacks[1].getClassName());
      return get(callerClazz.getResource(url));
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }
  
  public synchronized Icon get(URL url) {
    if (url == null) {
      return null;
    }
    
    Icon icon = (Icon)pool.get(url.toString());
    if (icon == null) {
      icon = new ImageIcon(url);
      pool.put(url.toString(), icon);
    }
    return icon;
  }
  
  public synchronized void clear() {
    pool.clear();
  }
  
}
