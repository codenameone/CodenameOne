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

import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.prefs.Preferences;

import javax.swing.DefaultButtonModel;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JRadioButton;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.event.ChangeEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.text.JTextComponent;

import com.l2fprod.common.swing.table.TableHelper;
import com.l2fprod.common.util.converter.ConverterRegistry;

/**
 * UserPreferences. <BR>
 * 
 */
public class UserPreferences {

  /**
   * Gets the default file chooser. Its current directory will be tracked and
   * restored on subsequent calls.
   * 
   * @return the default file chooser
   */
  public static JFileChooser getDefaultFileChooser() {
    return getFileChooser("default");
  }

  /**
   * Gets the default directory chooser. Its current directory will be tracked
   * and restored on subsequent calls.
   * 
   * @return the default directory chooser
   */
  public static JFileChooser getDefaultDirectoryChooser() {
    return getDirectoryChooser("default");
  }

  /**
   * Gets the file chooser with the given id. Its current directory will be
   * tracked and restored on subsequent calls.
   * 
   * @param id
   * @return the file chooser with the given id
   */
  public static JFileChooser getFileChooser(final String id) {
    JFileChooser chooser = new JFileChooser();
    track(chooser, "FileChooser." + id + ".path");
    return chooser;
  }

  /**
   * Gets the directory chooser with the given id. Its current directory will be
   * tracked and restored on subsequent calls.
   * 
   * @param id
   * @return the directory chooser with the given id
   */
  public static JFileChooser getDirectoryChooser(String id) {
    JFileChooser chooser;
    try {
      Class directoryChooserClass = Class
          .forName("com.l2fprod.common.swing.JDirectoryChooser");
      chooser = (JFileChooser) directoryChooserClass.newInstance();
    } catch (Exception e) {
      chooser = new JFileChooser();
      chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    }
    track(chooser, "DirectoryChooser." + id + ".path");
    return chooser;
  }

  private static void track(JFileChooser chooser, final String key) {
    // get the path for the given filechooser
    String path = node().get(key, null);
    if (path != null) {
      File file = new File(path);
      if (file.exists()) {
        chooser.setCurrentDirectory(file);
      }
    }

    PropertyChangeListener trackPath = new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent evt) {
        /* everytime the path change, update the preferences */
        if (evt.getNewValue() instanceof File) {
          node().put(key, ((File) evt.getNewValue()).getAbsolutePath());
        }
      }
    };

    chooser.addPropertyChangeListener(JFileChooser.DIRECTORY_CHANGED_PROPERTY,
        trackPath);
  }

  public static void track(final JRadioButton button) {
    final Preferences prefs = node().node("Buttons");
    boolean selected = prefs.getBoolean(button.getName() + ".selected", button
        .isSelected());
    ((DefaultButtonModel) button.getModel()).getGroup().setSelected(
        button.getModel(), selected);// .setSelected(selected);
    button.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        prefs.putBoolean(button.getName() + ".selected", button.isSelected());
      }
    });
  }

  /**
   * Restores the window size, position and state if possible. Tracks the window
   * size, position and state.
   * 
   * @param window
   */
  public static void track(Window window) {
    Preferences prefs = node().node("Windows");

    String bounds = prefs.get(window.getName() + ".bounds", null);
    if (bounds != null) {
      Rectangle rect = (Rectangle) ConverterRegistry.instance().convert(
          Rectangle.class, bounds);
      window.setBounds(rect);
    }

    window.addComponentListener(windowDimension);
  }

  private static ComponentListener windowDimension = new ComponentAdapter() {
    public void componentMoved(ComponentEvent e) {
      store((Window) e.getComponent());
    }

    public void componentResized(ComponentEvent e) {
      store((Window) e.getComponent());
    }

    private void store(Window w) {
      String bounds = (String) ConverterRegistry.instance().convert(
          String.class, w.getBounds());
      node().node("Windows").put(w.getName() + ".bounds", bounds);
    }
  };

  private static class TableWidthTracker implements TableColumnModelListener {

    private JTable table;

    public TableWidthTracker(JTable table) {
      this.table = table;
    }

    void saveColumnWidths() {
      try {
        Preferences prefs = node().node("Tables").node(table.getName() + ".columnWidths");
        prefs.clear();
        
        TableColumnModel model = table.getTableHeader().getColumnModel();
        for (int i = 0, c = model.getColumnCount(); i < c; i++) {
          TableColumn column = model.getColumn(i);
          prefs.putInt(table.getColumnName(i), column.getWidth());
        }
        
      } catch (Throwable e) {
        e.printStackTrace();
      }
    }

    public void columnAdded(TableColumnModelEvent event) {
      saveColumnWidths();
    }

    public void columnMarginChanged(ChangeEvent event) {
      saveColumnWidths();
    }

    public void columnMoved(TableColumnModelEvent event) {
      saveColumnWidths();
    }

    public void columnRemoved(TableColumnModelEvent event) {
      saveColumnWidths();
    }

    public void columnSelectionChanged(ListSelectionEvent event) {
      saveColumnWidths();
    }
  }

  public static void track(JTable table) {
    // first try to restore the widths
    try {
      Preferences prefs = node().node("Tables").node(table.getName() + ".columnWidths");
      
      TableColumnModel model = table.getTableHeader().getColumnModel();
      for (int i = 0, c = model.getColumnCount(); i < c; i++) {
        TableColumn column = model.getColumn(i);
        int width = prefs.getInt(table.getColumnName(i), -1);
        if (width != -1) {
          column.setPreferredWidth(width);
        }
      }
      table.getTableHeader().resizeAndRepaint();

    } catch (Throwable e) {
      e.printStackTrace();
    }

    // then plug the listener to track them
    try {
      TableHelper.addColumnModelTracker(table, new TableWidthTracker(table));
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  /**
   * Restores the text. Stores the text.
   * 
   * @param text
   */
  public static void track(JTextComponent text) {
    new TextListener(text);
  }

  private static class TextListener implements DocumentListener {
    private JTextComponent text;

    public TextListener(JTextComponent text) {
      this.text = text;
      restore();
      text.getDocument().addDocumentListener(this);
    }

    public void changedUpdate(javax.swing.event.DocumentEvent e) {
      store();
    }

    public void insertUpdate(javax.swing.event.DocumentEvent e) {
      store();
    }

    public void removeUpdate(javax.swing.event.DocumentEvent e) {
      store();
    }

    void restore() {
      Preferences prefs = node().node("JTextComponent");
      text.setText(prefs.get(text.getName(), ""));
    }

    void store() {
      Preferences prefs = node().node("JTextComponent");
      prefs.put(text.getName(), text.getText());
    }
  };

  public static void track(JSplitPane split) {
    Preferences prefs = node().node("JSplitPane");

    // restore the previous location
    int dividerLocation = prefs
        .getInt(split.getName() + ".dividerLocation", -1);
    if (dividerLocation >= 0) {
      split.setDividerLocation(dividerLocation);
    }

    // track changes
    split.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY,
        splitPaneListener);
  }

  private static PropertyChangeListener splitPaneListener = new PropertyChangeListener() {
    public void propertyChange(PropertyChangeEvent evt) {
      JSplitPane split = (JSplitPane) evt.getSource();
      node().node("JSplitPane").put(split.getName() + ".dividerLocation",
          String.valueOf(split.getDividerLocation()));
    }
  };

  /**
   * @return the Preference node where User Preferences are stored.
   */
  private static Preferences node() {
    return Preferences.userNodeForPackage(UserPreferences.class).node(
        "UserPreferences");
  }

}
