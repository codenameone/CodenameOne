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

import java.awt.Toolkit;

import javax.swing.UIManager;

/**
 * Provides methods related to the runtime environment.
 */
public class OS {

  private static final boolean osIsMacOsX;
  private static final boolean osIsWindows;
  private static final boolean osIsWindowsXP;
  private static final boolean osIsWindows2003;
  private static final boolean osIsWindowsVista;
  private static final boolean osIsLinux;

  static {
    String os = System.getProperty("os.name").toLowerCase();

    osIsMacOsX = "mac os x".equals(os);
    osIsWindows = os.indexOf("windows") != -1;
    osIsWindowsXP = "windows xp".equals(os);
    osIsWindows2003 = "windows 2003".equals(os);
    osIsWindowsVista = "windows vista".equals(os);
    osIsLinux = os != null && os.indexOf("linux") != -1;
  }

  /**
   * @return true if this VM is running on Mac OS X
   */
  public static boolean isMacOSX() {
    return osIsMacOsX;
  }

  /**
   * @return true if this VM is running on Windows
   */
  public static boolean isWindows() {
    return osIsWindows;
  }

  /**
   * @return true if this VM is running on Windows XP
   */
  public static boolean isWindowsXP() {
    return osIsWindowsXP;
  }

  /**
   * @return true if this VM is running on Windows 2003
   */
  public static boolean isWindows2003() {
    return osIsWindows2003;
  }

  /**
   * @return true if this VM is running on Windows Vista
   */
  public static boolean isWindowsVista() {
    return osIsWindowsVista;
  }
  
  /**
   * @return true if this VM is running on a Linux distribution
   */
  public static boolean isLinux() {
    return osIsLinux;
  }

  /**
   * @return true if the VM is running Windows and the Java
   *         application is rendered using XP Visual Styles.
   */
  public static boolean isUsingWindowsVisualStyles() {
    if (!isWindows()) {
      return false;
    }

    boolean xpthemeActive = Boolean.TRUE.equals(Toolkit.getDefaultToolkit()
        .getDesktopProperty("win.xpstyle.themeActive"));
    if (!xpthemeActive) {
      return false;
    } else {
      try {
        return System.getProperty("swing.noxp") == null;
      } catch (RuntimeException e) {
        return true;
      }
    }
  }

  /**
   * Returns the name of the current Windows visual style.
   * <ul>
   * <li>it looks for a property name "win.xpstyle.name" in UIManager and if not found
   * <li>it queries the win.xpstyle.colorName desktop property ({@link Toolkit#getDesktopProperty(java.lang.String)})
   * </ul>
   * 
   * @return the name of the current Windows visual style if any. 
   */
  public static String getWindowsVisualStyle() {
    String style = UIManager.getString("win.xpstyle.name");
    if (style == null) {
      // guess the name of the current XPStyle
      // (win.xpstyle.colorName property found in awt_DesktopProperties.cpp in
      // JDK source)
      style = (String)Toolkit.getDefaultToolkit().getDesktopProperty(
        "win.xpstyle.colorName");
    }
    return style;
  }
  
}