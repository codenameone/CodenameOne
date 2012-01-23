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

/**
 * Each new component type of the library will contribute an addon to
 * the LookAndFeelAddons. A <code>ComponentAddon</code> is the
 * equivalent of a {@link javax.swing.LookAndFeel}but focused on one
 * component. <br>
 * 
 * @author <a href="mailto:fred@L2FProd.com">Frederic Lavigne</a>
 */
public interface ComponentAddon {

  /**
   * @return the name of this addon
   */
  String getName();

  /**
   * Initializes this addon (i.e register UI classes, colors, fonts,
   * borders, any UIResource used by the component class). When
   * initializing, the addon can register different resources based on
   * the addon or the current look and feel.
   * 
   * @param addon the current addon
   */
  void initialize(LookAndFeelAddons addon);

  /**
   * Uninitializes this addon.
   * 
   * @param addon
   */
  void uninitialize(LookAndFeelAddons addon);

}