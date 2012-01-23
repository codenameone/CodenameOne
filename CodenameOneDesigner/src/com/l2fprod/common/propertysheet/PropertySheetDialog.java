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
package com.l2fprod.common.propertysheet;

import com.l2fprod.common.swing.BaseDialog;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.HeadlessException;

/**
 * PropertySheetDialog. <br>
 *  
 */
public class PropertySheetDialog extends BaseDialog {

  public PropertySheetDialog() throws HeadlessException {
    super();
  }

  public PropertySheetDialog(Dialog owner) throws HeadlessException {
    super(owner);
  }

  public PropertySheetDialog(Dialog owner, boolean modal)
    throws HeadlessException {
    super(owner, modal);
  }

  public PropertySheetDialog(Frame owner) throws HeadlessException {
    super(owner);
  }

  public PropertySheetDialog(Frame owner, boolean modal)
    throws HeadlessException {
    super(owner, modal);
  }

  public PropertySheetDialog(Dialog owner, String title)
    throws HeadlessException {
    super(owner, title);
  }

  public PropertySheetDialog(Dialog owner, String title, boolean modal)
    throws HeadlessException {
    super(owner, title, modal);
  }

  public PropertySheetDialog(Frame owner, String title)
    throws HeadlessException {
    super(owner, title);
  }

  public PropertySheetDialog(Frame owner, String title, boolean modal)
    throws HeadlessException {
    super(owner, title, modal);
  }

  public PropertySheetDialog(
    Dialog owner,
    String title,
    boolean modal,
    GraphicsConfiguration gc)
    throws HeadlessException {
    super(owner, title, modal, gc);
  }

  public PropertySheetDialog(
    Frame owner,
    String title,
    boolean modal,
    GraphicsConfiguration gc) {
    super(owner, title, modal, gc);
  }
  
}
