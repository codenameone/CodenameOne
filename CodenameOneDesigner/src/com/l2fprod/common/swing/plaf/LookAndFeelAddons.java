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
package com.l2fprod.common.swing.plaf;

import com.l2fprod.common.swing.plaf.aqua.AquaLookAndFeelAddons;
import com.l2fprod.common.swing.plaf.metal.MetalLookAndFeelAddons;
import com.l2fprod.common.swing.plaf.windows.WindowsClassicLookAndFeelAddons;
import com.l2fprod.common.swing.plaf.windows.WindowsLookAndFeelAddons;
import com.l2fprod.common.util.OS;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.metal.MetalLookAndFeel;

/**
 * Provides additional pluggable UI for new components added by the
 * library. By default, the library uses the pluggable UI returned by
 * {@link #getBestMatchAddonClassName()}.
 * <p>
 * The default addon can be configured using the
 * <code>swing.addon</code> system property as follow:
 * <ul>
 * <li>on the command line,
 * <code>java -Dswing.addon=ADDONCLASSNAME ...</code></li>
 * <li>at runtime and before using the library components
 * <code>System.getProperties().put("swing.addon", ADDONCLASSNAME);</code>
 * </li>
 * </ul>
 * <p>
 * The addon can also be installed directly by calling the
 * {@link #setAddon(String)}method. For example, to install the
 * Windows addons, add the following statement
 * <code>LookAndFeelAddons.setAddon("com.l2fprod.common.swing.plaf.windows.WindowsLookAndFeelAddons");</code>.
 * 
 * @author <a href="mailto:fred@L2FProd.com">Frederic Lavigne</a> 
 */
public class LookAndFeelAddons {

  private static List contributedComponents = new ArrayList();

  /**
   * Key used to ensure the current UIManager has been populated by the
   * LookAndFeelAddons.
   */
  private static final Object APPCONTEXT_INITIALIZED = new Object();

  private static boolean trackingChanges = false;
  private static PropertyChangeListener changeListener;    

  static {
    // load the default addon
    String addonClassname = getBestMatchAddonClassName();
    try {
      addonClassname = System.getProperty("swing.addon", addonClassname);
    } catch (SecurityException e) {
      // security exception may arise in Java Web Start
    }

    try {
      setAddon(addonClassname);
      setTrackingLookAndFeelChanges(true);      
    } catch (InstantiationException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }
  }

  private static LookAndFeelAddons currentAddon;

  public void initialize() {
    for (Iterator iter = contributedComponents.iterator(); iter.hasNext();) {
      ComponentAddon addon = (ComponentAddon)iter.next();
      addon.initialize(this);
    }
  }

  public void uninitialize() {
    for (Iterator iter = contributedComponents.iterator(); iter.hasNext();) {
      ComponentAddon addon = (ComponentAddon)iter.next();
      addon.uninitialize(this);
    }
  }

  /**
   * Adds the given defaults in UIManager.
   * 
   * Note: the values are added only if they do not exist in the existing look
   * and feel defaults. This makes it possible for look and feel implementors to
   * override library defaults.
   * 
   * Note: the array is traversed in reverse order. If a key is found twice in
   * the array, the key/value with the highest position in the array gets
   * precedence over the other key in the array
   * 
   * @param keysAndValues
   */
  public void loadDefaults(Object[] keysAndValues) {    
    // Go in reverse order so the most recent keys get added first...
    for (int i = keysAndValues.length - 2; i >= 0; i = i - 2) {
      if (UIManager.getLookAndFeelDefaults().get(keysAndValues[i]) == null) {
        UIManager.getLookAndFeelDefaults().put(keysAndValues[i], keysAndValues[i + 1]);
      }
    }
  }

  public void unloadDefaults(Object[] keysAndValues) {
    for (int i = 0, c = keysAndValues.length; i < c; i = i + 2) {
      UIManager.getLookAndFeelDefaults().put(keysAndValues[i], null);
    }
  }

  public static void setAddon(String addonClassName)
    throws InstantiationException, IllegalAccessException,
    ClassNotFoundException {
    setAddon(Class.forName(addonClassName));
  }

  public static void setAddon(Class addonClass) throws InstantiationException,
    IllegalAccessException {
    LookAndFeelAddons addon = (LookAndFeelAddons)addonClass.newInstance();
    setAddon(addon);
  }
   
  public static void setAddon(LookAndFeelAddons addon) {
    if (currentAddon != null) {
      currentAddon.uninitialize();
    }

    addon.initialize();
    currentAddon = addon;    
    UIManager.put(APPCONTEXT_INITIALIZED, Boolean.TRUE);
  }

  public static LookAndFeelAddons getAddon() {
    return currentAddon;
  }

  /**
   * Based on the current look and feel (as returned by
   * <code>UIManager.getLookAndFeel()</code>), this method returns
   * the name of the closest <code>LookAndFeelAddons</code> to use.
   * 
   * @return the addon matching the currently installed look and feel
   */
  public static String getBestMatchAddonClassName() {
    String lnf = UIManager.getLookAndFeel().getClass().getName();
    String addon;
    if (UIManager.getCrossPlatformLookAndFeelClassName().equals(lnf)) {
      addon = MetalLookAndFeelAddons.class.getName();
    } else if (UIManager.getSystemLookAndFeelClassName().equals(lnf)) {
      addon = getSystemAddonClassName();
    } else if ("com.sun.java.swing.plaf.windows.WindowsLookAndFeel".equals(lnf) ||
      "com.jgoodies.looks.windows.WindowsLookAndFeel".equals(lnf)) {
      if (OS.isUsingWindowsVisualStyles()) {
        addon = WindowsLookAndFeelAddons.class.getName();
      } else {
        addon = WindowsClassicLookAndFeelAddons.class.getName();
      }
    } else if ("com.sun.java.swing.plaf.windows.WindowsClassicLookAndFeel"
      .equals(lnf)) {
      addon = WindowsClassicLookAndFeelAddons.class.getName();
    } else if (UIManager.getLookAndFeel() instanceof MetalLookAndFeel) {
      // for JGoodies and other sub-l&fs of Metal
      addon = MetalLookAndFeelAddons.class.getName();
    } else {
      addon = getSystemAddonClassName();
    }
    return addon;
  }

  /**
   * Gets the addon best suited for the operating system where the
   * virtual machine is running.
   * 
   * @return the addon matching the native operating system platform.
   */
  public static String getSystemAddonClassName() {
    String addon = WindowsClassicLookAndFeelAddons.class.getName();

    if (OS.isMacOSX()) {
      addon = AquaLookAndFeelAddons.class.getName();
    } else if (OS.isWindows()) {
      // see whether of not visual styles are used
      if (OS.isUsingWindowsVisualStyles()) {
        addon = WindowsLookAndFeelAddons.class.getName();
      } else {
        addon = WindowsClassicLookAndFeelAddons.class.getName();
      }
    }

    return addon;
  }

  /**
   * Each new component added by the library will contribute its
   * default UI classes, colors and fonts to the LookAndFeelAddons.
   * See {@link ComponentAddon}.
   * 
   * @param component
   */
  public static void contribute(ComponentAddon component) {
    contributedComponents.add(component);

    if (currentAddon != null) {
      // make sure to initialize any addons added after the
      // LookAndFeelAddons has been installed
      component.initialize(currentAddon);
    }
  }

  /**
   * Removes the contribution of the given addon
   * 
   * @param component
   */
  public static void uncontribute(ComponentAddon component) {
    contributedComponents.remove(component);
    
    if (currentAddon != null) {
      component.uninitialize(currentAddon);
    }
  }

  /**
   * Workaround for IDE mixing up with classloaders and Applets environments.
   * Consider this method as API private. It must not be called directly.
   * 
   * @param component
   * @param expectedUIClass
   * @return an instance of expectedUIClass 
   */
  public static ComponentUI getUI(JComponent component, Class expectedUIClass) {
    maybeInitialize();

    // solve issue with ClassLoader not able to find classes
    String uiClassname = (String)UIManager.get(component.getUIClassID());
    try {
      Class uiClass = Class.forName(uiClassname);
      UIManager.put(uiClassname, uiClass);
    } catch (Exception e) {
      // we ignore the ClassNotFoundException
    }
    
    ComponentUI ui = UIManager.getUI(component);
    
    if (expectedUIClass.isInstance(ui)) {
      return ui;
    } else {
      String realUI = ui.getClass().getName();
      Class realUIClass;
      try {
        realUIClass = expectedUIClass.getClassLoader()
        .loadClass(realUI);
      } catch (ClassNotFoundException e) {
        throw new RuntimeException("Failed to load class " + realUI, e);
      }
      Method createUIMethod = null;
      try {
        createUIMethod = realUIClass.getMethod("createUI", new Class[]{JComponent.class});
      } catch (NoSuchMethodException e1) {
        throw new RuntimeException("Class " + realUI + " has no method createUI(JComponent)");
      }
      try {
        return (ComponentUI)createUIMethod.invoke(null, new Object[]{component});
      } catch (Exception e2) {
        throw new RuntimeException("Failed to invoke " + realUI + "#createUI(JComponent)");
      }
    }
  }
  
  /**
   * With applets, if you reload the current applet, the UIManager will be
   * reinitialized (entries previously added by LookAndFeelAddons will be
   * removed) but the addon will not reinitialize because addon initialize
   * itself through the static block in components and the classes do not get
   * reloaded. This means component.updateUI will fail because it will not find
   * its UI.
   * 
   * This method ensures LookAndFeelAddons get re-initialized if needed. It must
   * be called in every component updateUI methods.
   */
  private static synchronized void maybeInitialize() {
    if (currentAddon != null) {
      // this is to ensure "UIManager#maybeInitialize" gets called and the
      // LAFState initialized
      UIManager.getLookAndFeelDefaults();
      
      if (!UIManager.getBoolean(APPCONTEXT_INITIALIZED)) {
        setAddon(currentAddon);
      }
    }
  }
  
  //
  // TRACKING OF THE CURRENT LOOK AND FEEL
  //  
  private static class UpdateAddon implements PropertyChangeListener {
    public void propertyChange(PropertyChangeEvent evt) {
      try {
        setAddon(getBestMatchAddonClassName());
      } catch (Exception e) {
        // should not happen
        throw new RuntimeException(e);
      }
    }
  }
  
  /**
   * If true, everytime the Swing look and feel is changed, the addon which
   * best matches the current look and feel will be automatically selected.
   * 
   * @param tracking
   *          true to automatically update the addon, false to not automatically
   *          track the addon. Defaults to false.
   * @see #getBestMatchAddonClassName()
   */
  public static synchronized void setTrackingLookAndFeelChanges(boolean tracking) {
    if (trackingChanges != tracking) {
      if (tracking) {
        if (changeListener == null) {
          changeListener = new UpdateAddon();
        }
        UIManager.addPropertyChangeListener(changeListener);
      } else {
        if (changeListener != null) {
          UIManager.removePropertyChangeListener(changeListener);
        }
        changeListener = null;
      }
      trackingChanges = tracking;
    }
  }
  
  /**
   * @return true if the addon will be automatically change to match the current
   *         look and feel
   * @see #setTrackingLookAndFeelChanges(boolean)
   */
  public static synchronized boolean isTrackingLookAndFeelChanges() {
    return trackingChanges;
  }
  
}
