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
package com.l2fprod.common.swing.renderer;

import com.l2fprod.common.model.DefaultObjectRenderer;
import com.l2fprod.common.model.ObjectRenderer;

import java.awt.Color;
import java.awt.Component;
import java.awt.SystemColor;

import javax.swing.Icon;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * DefaultCellRenderer.<br>
 *
 */
public class DefaultCellRenderer
  extends DefaultTableCellRenderer
  implements ListCellRenderer {

	private ObjectRenderer objectRenderer = new DefaultObjectRenderer();

	private Color oddBackgroundColor = SystemColor.window;
	private Color evenBackgroundColor = SystemColor.window;
  private boolean showOddAndEvenRows = true;
	
	public void setOddBackgroundColor(Color c) {
	  oddBackgroundColor = c;
	}
	
	public void setEvenBackgroundColor(Color c) {
	  evenBackgroundColor = c;
	}
	
	public void setShowOddAndEvenRows(boolean b) {
	  showOddAndEvenRows = b;
	}
	
	public Component getListCellRendererComponent(JList list, Object value,
	  int index, boolean isSelected, boolean cellHasFocus) {
	  	
	  setBorder(null);
	  
		if (isSelected) {
			setBackground(list.getSelectionBackground());
			setForeground(list.getSelectionForeground());
		} else {
			setBackground(list.getBackground());
			setForeground(list.getForeground());
		}
		
    setValue(value);
    
		return this;
	}
	
	public Component getTableCellRendererComponent(
		JTable table,
		Object value,
		boolean isSelected,
		boolean hasFocus,
		int row,
		int column) {
		super.getTableCellRendererComponent(
			table,
			value,
			isSelected,
			hasFocus,
			row,
			column);

		if (showOddAndEvenRows && !isSelected) {
		  if (row % 2 == 0) {
		    setBackground(oddBackgroundColor);
		  } else {
		    setBackground(evenBackgroundColor);
		  }
		}
		
    setValue(value);
    
		return this;
	}

  public void setValue(Object value) {
    String text = convertToString(value);
    Icon icon = convertToIcon(value);
    
    setText(text==null?"":text);
    setIcon(icon);
    setDisabledIcon(icon);
  }
  
  protected String convertToString(Object value) {
    return objectRenderer.getText(value);    
  }
  
  protected Icon convertToIcon(Object value) {
    return null;
  }
  
}
