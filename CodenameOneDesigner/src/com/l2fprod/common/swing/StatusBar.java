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

import java.awt.Color;
import java.awt.Component;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.UIResource;

/**
 * StatusBar. <BR>A status bar is made of multiple zones. A zone can be any
 * JComponent.
 */
public class StatusBar extends JComponent {

  /**
   * The key used to identified the default zone
   */
  public final static String DEFAULT_ZONE = "default";

  private Hashtable idToZones;
  private Border zoneBorder;

  /**
   * Construct a new StatusBar
   *  
   */
  public StatusBar() {
    setLayout(LookAndFeelTweaks.createHorizontalPercentLayout());
    idToZones = new Hashtable();
    setZoneBorder(BorderFactory.createLineBorder(Color.lightGray));
  }

  public void setZoneBorder(Border border) {
    zoneBorder = border;
  }

  /**
   * Adds a new zone in the StatusBar
   * 
   * @param id
   * @param zone
   * @param constraints one of the constraint support by the
   *          {@link com.l2fprod.common.swing.PercentLayout}
   */
  public void addZone(String id, Component zone, String constraints) {
    // is there already a zone with this id?
    Component previousZone = getZone(id);
    if (previousZone != null) {
      remove(previousZone);
      idToZones.remove(id);
    }

    if (zone instanceof JComponent) {
      JComponent jc = (JComponent)zone;
      if (jc.getBorder() == null || jc.getBorder() instanceof UIResource) {
        if (jc instanceof JLabel) {
          jc.setBorder(
            new CompoundBorder(zoneBorder, new EmptyBorder(0, 2, 0, 2)));
          ((JLabel)jc).setText(" ");
        } else {
          jc.setBorder(zoneBorder);
        }
      }
    }

    add(zone, constraints);
    idToZones.put(id, zone);
  }

  public Component getZone(String id) {
    return (Component)idToZones.get(id);
  }

  /**
   * For example:
   * 
   * <code>
   *  setZones(new String[]{"A","B"},
   * 					 new JComponent[]{new JLabel(), new JLabel()},
   * 					 new String[]{"33%","*"});
   * </code>
   * 
   * would construct a new status bar with two zones (two JLabels)
   * named A and B, the first zone A will occupy 33 percents of the
   * overall size of the status bar and B the left space.
   * 
   * @param ids a value of type 'String[]'
   * @param zones a value of type 'JComponent[]'
   * @param constraints a value of type 'String[]'
   */
  public void setZones(String[] ids, Component[] zones, String[] constraints) {
    removeAll();
    idToZones.clear();
    for (int i = 0, c = zones.length; i < c; i++) {
      addZone(ids[i], zones[i], constraints[i]);
    }
  }

}
