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

import com.l2fprod.common.propertysheet.PropertySheetTableModel.Item;
import com.l2fprod.common.swing.HeaderlessColumnResizer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyEditor;

import javax.swing.AbstractAction;
import javax.swing.CellEditor;
import javax.swing.Icon;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

/**
 * A table which allows the editing of Properties through
 * PropertyEditors. The PropertyEditors can be changed by using the
 * PropertyEditorRegistry.
 */
public class PropertySheetTable extends JTable {
  
  private static final int HOTSPOT_SIZE = 18;
  
  private static final String TREE_EXPANDED_ICON_KEY = "Tree.expandedIcon";
  private static final String TREE_COLLAPSED_ICON_KEY = "Tree.collapsedIcon";
  private static final String TABLE_BACKGROUND_COLOR_KEY = "Table.background";
  private static final String TABLE_FOREGROUND_COLOR_KEY = "Table.foreground";
  private static final String TABLE_SELECTED_BACKGROUND_COLOR_KEY = "Table.selectionBackground";
  private static final String TABLE_SELECTED_FOREGROUND_COLOR_KEY = "Table.selectionForeground";
  private static final String PANEL_BACKGROUND_COLOR_KEY = "Panel.background";

  private PropertyEditorFactory editorFactory;
  private PropertyRendererFactory rendererFactory;

  private TableCellRenderer nameRenderer;  
  
  private boolean wantsExtraIndent = false;
  
  /**
   * Cancel editing when editing row is changed
   */
  private TableModelListener cancelEditing;

  // Colors used by renderers
  private Color categoryBackground;
  private Color categoryForeground;
  private Color propertyBackground;
  private Color propertyForeground;
  private Color selectedPropertyBackground;
  private Color selectedPropertyForeground;
  private Color selectedCategoryBackground;
  private Color selectedCategoryForeground;
  
  public PropertySheetTable() {
    this(new PropertySheetTableModel());
  }

  public PropertySheetTable(PropertySheetTableModel dm) {
    super(dm);
    initDefaultColors();

    // select only one property at a time
    getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    // hide the table header, we do not need it
    Dimension nullSize = new Dimension(0, 0);
    getTableHeader().setPreferredSize(nullSize);
    getTableHeader().setMinimumSize(nullSize);
    getTableHeader().setMaximumSize(nullSize);
    getTableHeader().setVisible(false);

    // table header not being visible, make sure we can still resize the columns
    new HeaderlessColumnResizer(this);

    // default renderers and editors
    setRendererFactory(new PropertyRendererRegistry());
    setEditorFactory(new PropertyEditorRegistry());
    
    nameRenderer = new NameRenderer();
    
    // force the JTable to commit the edit when it losts focus
    putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
    
    // only full rows can be selected
    setColumnSelectionAllowed(false);
    setRowSelectionAllowed(true);

    // replace the edit action to always trigger the editing of the value column
    getActionMap().put("startEditing", new StartEditingAction());
    
    // ensure navigating with "TAB" moves to the next row
    getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0),
      "selectNextRowCell");
    getInputMap().put(
      KeyStroke.getKeyStroke(KeyEvent.VK_TAB, KeyEvent.SHIFT_DOWN_MASK),
      "selectPreviousRowCell");
    
    // allow category toggle with SPACE and mouse
    getActionMap().put("toggle", new ToggleAction());
    getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0),
      "toggle");    
    addMouseListener(new ToggleMouseHandler());
  }

  /**
   * Initializes the default set of colors used by the PropertySheetTable.
   * 
   * @see #categoryBackground
   * @see #categoryForeground
   * @see #selectedCategoryBackground
   * @see #selectedCategoryForeground
   * @see #propertyBackground
   * @see #propertyForeground
   * @see #selectedPropertyBackground
   * @see #selectedPropertyForeground
   */
  private void initDefaultColors() {    
    this.categoryBackground = UIManager.getColor(PANEL_BACKGROUND_COLOR_KEY);
    this.categoryForeground = UIManager.getColor(TABLE_FOREGROUND_COLOR_KEY).darker().darker().darker();
    
    this.selectedCategoryBackground = categoryBackground.darker();
    this.selectedCategoryForeground = categoryForeground;
    
    this.propertyBackground = UIManager.getColor(TABLE_BACKGROUND_COLOR_KEY);
    this.propertyForeground = UIManager.getColor(TABLE_FOREGROUND_COLOR_KEY);
    
    this.selectedPropertyBackground = UIManager
      .getColor(TABLE_SELECTED_BACKGROUND_COLOR_KEY);
    this.selectedPropertyForeground = UIManager
      .getColor(TABLE_SELECTED_FOREGROUND_COLOR_KEY);
    
    setGridColor(categoryBackground);
  }
    
  
  public Color getCategoryBackground() {
    return categoryBackground;
  }

  /**
   * Sets the color used to paint a Category background.
   * 
   * @param categoryBackground
   */
  public void setCategoryBackground(Color categoryBackground) {
    this.categoryBackground = categoryBackground;
    repaint();
  }

  public Color getCategoryForeground() {
    return categoryForeground;
  }

  /**
   * Sets the color used to paint a Category foreground.
   * 
   * @param categoryForeground
   */
  public void setCategoryForeground(Color categoryForeground) {
    this.categoryForeground = categoryForeground;
    repaint();
  }

  public Color getSelectedCategoryBackground() {
    return selectedCategoryBackground;
  }

  /**
   * Sets the color used to paint a selected/focused Category background.
   * 
   * @param selectedCategoryBackground
   */
  public void setSelectedCategoryBackground(Color selectedCategoryBackground) {
    this.selectedCategoryBackground = selectedCategoryBackground;
    repaint();
  }

  public Color getSelectedCategoryForeground() {
    return selectedCategoryForeground;
  }

  /**
   * Sets the color used to paint a selected/focused Category foreground.
   * 
   * @param selectedCategoryForeground
   */
  public void setSelectedCategoryForeground(Color selectedCategoryForeground) {
    this.selectedCategoryForeground = selectedCategoryForeground;
    repaint();
  }

  public Color getPropertyBackground() {
    return propertyBackground;
  }

  /**
   * Sets the color used to paint a Property background.
   * 
   * @param propertyBackground
   */
  public void setPropertyBackground(Color propertyBackground) {
    this.propertyBackground = propertyBackground;
    repaint();
  }

  public Color getPropertyForeground() {
    return propertyForeground;
  }

  /**
   * Sets the color used to paint a Property foreground.
   * 
   * @param propertyForeground
   */
  public void setPropertyForeground(Color propertyForeground) {
    this.propertyForeground = propertyForeground;
    repaint();
  }

  public Color getSelectedPropertyBackground() {
    return selectedPropertyBackground;
  }

  /**
   * Sets the color used to paint a selected/focused Property background.
   * 
   * @param selectedPropertyBackground
   */
  public void setSelectedPropertyBackground(Color selectedPropertyBackground) {
    this.selectedPropertyBackground = selectedPropertyBackground;
    repaint();
  }

  public Color getSelectedPropertyForeground() {
    return selectedPropertyForeground;
  }

  /**
   * Sets the color used to paint a selected/focused Property foreground.
   * 
   * @param selectedPropertyForeground
   */
  public void setSelectedPropertyForeground(Color selectedPropertyForeground) {
    this.selectedPropertyForeground = selectedPropertyForeground;
    repaint();
  }

  public void setEditorFactory(PropertyEditorFactory factory) {
    editorFactory = factory;
  }

  public final PropertyEditorFactory getEditorFactory() {
    return editorFactory;
  }

  /**
   * @param registry
   * @deprecated use {@link #setEditorFactory(PropertyEditorFactory)}
   */
  public void setEditorRegistry(PropertyEditorRegistry registry) {
    setEditorFactory(registry);
  }

  /**
   * @deprecated use {@link #getEditorFactory()}
   * @throws ClassCastException if the current editor factory is not a
   *           PropertyEditorRegistry
   */
  public PropertyEditorRegistry getEditorRegistry() {
    return (PropertyEditorRegistry) editorFactory;
  }

  public void setRendererFactory(PropertyRendererFactory factory) {
    rendererFactory = factory;
  }

  public PropertyRendererFactory getRendererFactory() {
    return rendererFactory;
  }

  /**
   * @deprecated use {@link #setRendererFactory(PropertyRendererFactory)}
   * @param registry
   */
  public void setRendererRegistry(PropertyRendererRegistry registry) {
    setRendererFactory(registry);
  }

  /**
   * @deprecated use {@link #getRendererFactory()}
   * @throws ClassCastException if the current renderer factory is not a
   *           PropertyRendererRegistry
   */
  public PropertyRendererRegistry getRendererRegistry() {
    return (PropertyRendererRegistry) getRendererFactory();
  }

  /* (non-Javadoc)
   * @see javax.swing.JTable#isCellEditable(int, int)
   */
  public boolean isCellEditable(int row, int column) {
    // names are not editable
    if (column == 0) { return false; }

    PropertySheetTableModel.Item item = getSheetModel().getPropertySheetElement(row);
    return item.isProperty() && item.getProperty().isEditable();
  }

  /**
   * Gets the CellEditor for the given row and column. It uses the
   * editor registry to find a suitable editor for the property.
   * @see javax.swing.JTable#getCellEditor(int, int)
   */
  public TableCellEditor getCellEditor(int row, int column) {
    if (column == 0) { return null; }

    Item item = getSheetModel().getPropertySheetElement(row);
    if (!item.isProperty())
      return null;
    
    TableCellEditor result = null;
    Property propery = item.getProperty();
    PropertyEditor editor = getEditorFactory().createPropertyEditor(propery);
    if (editor != null)
      result = new CellEditorAdapter(editor);

    return result;
  }

  /* (non-Javadoc)
   * @see javax.swing.JTable#getCellRenderer(int, int)
   */
  public TableCellRenderer getCellRenderer(int row, int column) {
    PropertySheetTableModel.Item item = getSheetModel()
      .getPropertySheetElement(row);

    switch (column) {
      case PropertySheetTableModel.NAME_COLUMN:
        // name column gets a custom renderer
        return nameRenderer;

      case PropertySheetTableModel.VALUE_COLUMN: {
        if (!item.isProperty())
          return nameRenderer;

        // property value column gets the renderer from the factory
        Property property = item.getProperty();
        TableCellRenderer renderer = getRendererFactory().createTableCellRenderer(property);
        if (renderer == null)
          renderer = getCellRenderer(property.getType());
        return renderer;
      }
      default:
        // when will this happen, given the above?
        return super.getCellRenderer(row, column);
    }
  }

  /**
   * Helper method to lookup a cell renderer based on type.
   * @param type the type for which a renderer should be found
   * @return a renderer for the given object type
   */
  private TableCellRenderer getCellRenderer(Class type) {
    // try to create one from the factory
    TableCellRenderer renderer = getRendererFactory().createTableCellRenderer(type);

    // if that fails, recursively try again with the superclass
    if (renderer == null && type != null)
      renderer = getCellRenderer(type.getSuperclass());

    // if that fails, just use the default Object renderer
    if (renderer == null)
      renderer = super.getDefaultRenderer(Object.class);

    return renderer;
  }

  public final PropertySheetTableModel getSheetModel() {
    return (PropertySheetTableModel) getModel();
  }

  /**
   * Overriden
   * <li>to prevent the cell focus rect to be painted
   * <li>to disable ({@link Component#setEnabled(boolean)} the renderer if the
   * Property is not editable
   */
  public Component prepareRenderer(TableCellRenderer renderer, int row,
    int column) {
    Object value = getValueAt(row, column);
    boolean isSelected = isCellSelected(row, column);
    Component component = renderer.getTableCellRendererComponent(this, value,
      isSelected, false, row, column);
    
    PropertySheetTableModel.Item item = getSheetModel()
      .getPropertySheetElement(row);
    if (item.isProperty()) {
      component.setEnabled(item.getProperty().isEditable());
    }
    return component;
  }

  /**
   * Overriden to register a listener on the model. This listener ensures
   * editing is cancelled when editing row is being changed.
   * 
   * @see javax.swing.JTable#setModel(javax.swing.table.TableModel)
   * @throws IllegalArgumentException
   *           if dataModel is not a {@link PropertySheetTableModel}
   */
  public void setModel(TableModel newModel) {
    if (!(newModel instanceof PropertySheetTableModel)) {
      throw new IllegalArgumentException("dataModel must be of type "
          + PropertySheetTableModel.class.getName());
    }

    if (cancelEditing == null) {
      cancelEditing = new CancelEditing();
    }

    TableModel oldModel = getModel();
    if (oldModel != null) {
      oldModel.removeTableModelListener(cancelEditing);
    }
    super.setModel(newModel);
    newModel.addTableModelListener(cancelEditing);

    // ensure the "value" column can not be resized
    getColumnModel().getColumn(1).setResizable(false);
  }

  /**
   * @see #setWantsExtraIndent(boolean)
   */
  public boolean getWantsExtraIndent() {
    return wantsExtraIndent;
  }

  /**
   * By default, properties with children are painted with the same indent level
   * as other properties and categories. When nested properties exist within the
   * set of properties, the end-user might be confused by the category and
   * property handles. Sets this property to true to add an extra indent level
   * to properties.
   * 
   * @param wantsExtraIndent
   */
  public void setWantsExtraIndent(boolean wantsExtraIndent) {
    this.wantsExtraIndent = wantsExtraIndent;
    repaint();
  }
  
  /**
   * Ensures the table uses the full height of its parent
   * {@link javax.swing.JViewport}.
   */
  public boolean getScrollableTracksViewportHeight() {
    return getPreferredSize().height < getParent().getHeight();
  }
  
  /**
   * Commits on-going cell editing 
   */
  public void commitEditing() {
    TableCellEditor editor = getCellEditor();
    if (editor != null) {
      editor.stopCellEditing();
    }    
  }

  /**
   * Cancels on-going cell editing 
   */
  public void cancelEditing() {
    TableCellEditor editor = getCellEditor();
    if (editor != null) {
      editor.cancelCellEditing();
    }    
  }

  /**
   * Cancels the cell editing if any update happens while modifying a value.
   */
  private class CancelEditing implements TableModelListener {
    public void tableChanged(TableModelEvent e) {
      // in case the table changes for the following reasons:
      // * the editing row has changed
      // * the editing row was removed
      // * all rows were changed
      // * rows were added
      //
      // it is better to cancel the editing of the row as our editor
      // may no longer be the right one. It happens when you play with
      // the sorting while having the focus in one editor.
      if (e.getType() == TableModelEvent.UPDATE) {
        int first = e.getFirstRow();
        int last = e.getLastRow();
        int editingRow = PropertySheetTable.this.getEditingRow();

        TableCellEditor editor = PropertySheetTable.this.getCellEditor();
        if (editor != null && first <= editingRow && editingRow <= last) {
          editor.cancelCellEditing();
        }
      }
    }
  }

  /**
   * Starts value cell editing even if value cell does not have the focus but
   * only if row is selected.
   */
  private static class StartEditingAction extends AbstractAction {
    public void actionPerformed(ActionEvent e) {
      JTable table = (JTable)e.getSource();
      if (!table.hasFocus()) {
        CellEditor cellEditor = table.getCellEditor();
        if (cellEditor != null && !cellEditor.stopCellEditing()) { return; }
        table.requestFocus();
        return;
      }
      ListSelectionModel rsm = table.getSelectionModel();
      int anchorRow = rsm.getAnchorSelectionIndex();
      table.editCellAt(anchorRow, PropertySheetTableModel.VALUE_COLUMN);
      Component editorComp = table.getEditorComponent();
      if (editorComp != null) {
        editorComp.requestFocus();
      }
    }
  }

  /**
   * Toggles the state of a row between expanded/collapsed. Works only for rows
   * with "toggle" knob.
   */
  private class ToggleAction extends AbstractAction {
    public void actionPerformed(ActionEvent e) {      
      int row = PropertySheetTable.this.getSelectedRow();
      Item item = PropertySheetTable.this.getSheetModel()
        .getPropertySheetElement(row);
      item.toggle();
      PropertySheetTable.this.addRowSelectionInterval(row, row);
    }
    public boolean isEnabled() {
      int row = PropertySheetTable.this.getSelectedRow();
      if (row != -1) {
        Item item = PropertySheetTable.this.getSheetModel()
          .getPropertySheetElement(row);        
        return item.hasToggle();
      } else {
        return false;
      }
    }
  }

  /**
   * @see ToggleAction
   */
  private static class ToggleMouseHandler extends MouseAdapter {
    public void mouseReleased(MouseEvent event) {
      PropertySheetTable table = (PropertySheetTable) event.getComponent();
      int row = table.rowAtPoint(event.getPoint());
      int column = table.columnAtPoint(event.getPoint());
      if (row != -1 && column == 0) {
        // if we clicked on an Item, see if we clicked on its hotspot
        Item item = table.getSheetModel().getPropertySheetElement(row);        
        int x = event.getX() - getIndent(table, item);
        if (x > 0 && x < HOTSPOT_SIZE)
          item.toggle();
      }
    }
  }

  /**
   * Calculates the required left indent for a given item, given its type and
   * its hierarchy level.
   */
  static int getIndent(PropertySheetTable table, Item item) {
    int indent = 0;
    
    if (item.isProperty()) {
      // it is a property, it has no parent or a category, and no child
      if ((item.getParent() == null || !item.getParent().isProperty())
        && !item.hasToggle()) {
        indent = table.getWantsExtraIndent()?HOTSPOT_SIZE:0;
      } else {
        // it is a property with children
        if (item.hasToggle()) {
          indent = item.getDepth() * HOTSPOT_SIZE;
        } else {          
          indent = (item.getDepth() + 1) * HOTSPOT_SIZE;
        }        
      }
      
      if (table.getSheetModel().getMode() == PropertySheet.VIEW_AS_CATEGORIES
        && table.getWantsExtraIndent()) {
        indent += HOTSPOT_SIZE;
      }

    } else {
      // category has no indent
      indent = 0;
    }    
    return indent;
  }
  
  /**
   * Paints the border around the name cell. It handles the indent from the left
   * side and the painting of the toggle knob.
   */
  private static class CellBorder implements Border {
    
    private int indentWidth; // space before hotspot
    private boolean showToggle;
    private boolean toggleState;
    private Icon expandedIcon;
    private Icon collapsedIcon;
    private Insets insets = new Insets(1, 0, 1, 1);
    private boolean isProperty;
    
    public CellBorder() {
      expandedIcon = (Icon)UIManager.get(TREE_EXPANDED_ICON_KEY);
      collapsedIcon = (Icon)UIManager.get(TREE_COLLAPSED_ICON_KEY);
      if (expandedIcon == null) {
        expandedIcon = new ExpandedIcon();
      }
      if (collapsedIcon == null) {
        collapsedIcon = new CollapsedIcon();
      }
    }

    public void configure(PropertySheetTable table, Item item) {      
      isProperty = item.isProperty();      
      toggleState =  item.isVisible();
      showToggle = item.hasToggle();
      
      indentWidth = getIndent(table, item);      
      insets.left = indentWidth + (showToggle?HOTSPOT_SIZE:0) + 2;;
    }
    
    public Insets getBorderInsets(Component c) {
      return insets;
    }

    public void paintBorder(Component c, Graphics g, int x, int y, int width,
        int height) {      
      if (!isProperty) {
        Color oldColor = g.getColor();      
        g.setColor(c.getBackground());
        g.fillRect(x, y, x + HOTSPOT_SIZE - 2, y + height);
        g.setColor(oldColor);
      }
      
      if (showToggle) {
        Icon drawIcon = (toggleState ? expandedIcon : collapsedIcon);
        drawIcon.paintIcon(c, g,
          x + indentWidth + (HOTSPOT_SIZE - 2 - drawIcon.getIconWidth()) / 2,
          y + (height - drawIcon.getIconHeight()) / 2);
      }
    }

    public boolean isBorderOpaque() {
      return true;
    }
    
  }

  private static class ExpandedIcon implements Icon {
    public void paintIcon(Component c, Graphics g, int x, int y) {
      Color backgroundColor = c.getBackground();

      if (backgroundColor != null)
        g.setColor(backgroundColor);
      else g.setColor(Color.white);
      g.fillRect(x, y, 8, 8);
      g.setColor(Color.gray);
      g.drawRect(x, y, 8, 8);
      g.setColor(Color.black);
      g.drawLine(x + 2, y + 4, x + (6), y + 4);
    }
    public int getIconWidth() {
      return 9;
    }
    public int getIconHeight() {
      return 9;
    }
  }

  private static class CollapsedIcon extends ExpandedIcon {
    public void paintIcon(Component c, Graphics g, int x, int y) {
      super.paintIcon(c, g, x, y);
      g.drawLine(x + 4, y + 2, x + 4, y + 6);
    }
  }

  /**
   * A {@link TableCellRenderer} for property names.
   */
  private class NameRenderer extends DefaultTableCellRenderer {

    private CellBorder border;
    
    public NameRenderer() {
      border = new CellBorder();
    }
    
    private Color getForeground(boolean isProperty, boolean isSelected) {
      return (isProperty ? (isSelected ? selectedPropertyForeground : propertyForeground) :
        (isSelected ? selectedCategoryForeground : categoryForeground));
    }

    private Color getBackground(boolean isProperty, boolean isSelected) {
      return (isProperty ? (isSelected ? selectedPropertyBackground : propertyBackground) :
        (isSelected ? selectedCategoryBackground : categoryBackground));
    }

    public Component getTableCellRendererComponent(JTable table, Object value,
        boolean isSelected, boolean hasFocus, int row, int column) {
      super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
      PropertySheetTableModel.Item item = (Item) value;

      // shortcut if we are painting the category column
      if (column == PropertySheetTableModel.VALUE_COLUMN && !item.isProperty()) {
        setBackground(getBackground(item.isProperty(), isSelected));
        setText("");
        return this;
      }
      
      setBorder(border);

      // configure the border
      border.configure((PropertySheetTable)table, item);
      
      setBackground(getBackground(item.isProperty(), isSelected));
      setForeground(getForeground(item.isProperty(), isSelected));
      
      setEnabled(isSelected || !item.isProperty() ? true : item.getProperty().isEditable());
      setText(item.getName());

      return this;
    }
  }

}
