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
package com.l2fprod.common.beans.editor;

import com.l2fprod.common.util.OS;

import java.awt.Dimension;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.UIManager;

/**
 * A button with a fixed size to workaround bugs in OSX. Submitted by Hani
 * Suleiman. Hani uses an icon for the ellipsis, I've decided to hardcode the
 * dimension to 16x30 but only on Mac OS X.
 */
public final class FixedButton extends JButton {

  public FixedButton() {
    super("...");

    if (OS.isMacOSX() && UIManager.getLookAndFeel().isNativeLookAndFeel()) {
      setPreferredSize(new Dimension(16, 30));
    }
    
    setMargin(new Insets(0, 0, 0, 0));
  }

}
