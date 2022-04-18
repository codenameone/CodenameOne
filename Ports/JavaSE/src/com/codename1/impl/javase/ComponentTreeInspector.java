/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.impl.javase;

import com.codename1.impl.javase.simulator.PropertyDetailsPanel;
import com.codename1.impl.javase.simulator.SelectableAction;
import com.codename1.impl.javase.util.SwingUtils;
import javax.swing.*;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Painter;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.list.ContainerList;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventObject;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;

/**
 *
 * @author shannah
 */
public class ComponentTreeInspector extends JPanel {
    private List<String> themePaths = new ArrayList<String>();
    private List<String> themeNames = new ArrayList<String>();
    private boolean simulatorRightClickEnabled = true;
    private JFrame frame;
    private PropertyDetailsPanel propertyDetailsPanel;

    public boolean isSimulatorRightClickEnabled() {
        return simulatorRightClickEnabled;
    }

    public void setSimulatorRightClickEnabled(boolean simulatorRightClickEnabled) {
        this.simulatorRightClickEnabled = simulatorRightClickEnabled;
    }

    class SelectedComponentGlassPane implements Painter {
        Component cmp;
        public SelectedComponentGlassPane(Component cmp) {
            this.cmp = cmp;
        }

        @Override
        public void paint(Graphics g, Rectangle rect) {
            g.setAlpha(30);
            g.setColor(0xff0000);
            
            g.fillRect(cmp.getAbsoluteX(), cmp.getAbsoluteY(), cmp.getWidth(), cmp.getHeight());
            
            g.setAlpha(255);
        }
        
    }
    
    public JFrame showInFrame() {
        if (frame == null) {
            frame = new JFrame("Component Inspector");
            frame.getContentPane().setLayout(new BorderLayout());
            
            frame.getContentPane().add(this, BorderLayout.CENTER);
            frame.pack();
            frame.setLocationByPlatform(true);
            
        }
        refreshComponentTree();
        loadThemes();
        frame.setVisible(true);
        return frame;
    }
    
    public void dispose() {
        if (frame != null) {
            frame.dispose();
            frame = null;
        }
    }

    
    
    
    public javax.swing.JComponent removeComponentTree() {
        jScrollPane1.getParent().remove(jScrollPane1);
        return jScrollPane1;
    }
    
    private Component currentComponent; 
    /** Creates new form ComponentTreeInspector */
    public ComponentTreeInspector() {
        setLayout(new BorderLayout());
        propertyDetailsPanel = new PropertyDetailsPanel();
        initComponents();

        themes.removeAllItems();
        refreshComponentTree();
        componentUIID.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                updateUiid();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateUiid();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateUiid();
            }

            private void updateUiid() {
                final String uiidText = componentUIID.getText();
                if(currentComponent != null && uiidText.length() > 0) {
                    Display.getInstance().callSerially(new Runnable() {
                        @Override
                        public void run() {
                            if(currentComponent != null && !Objects.equals(currentComponent.getUIID(), uiidText)) {
                                currentComponent.setUIID(uiidText);
                                Display.getInstance().getCurrent().revalidate();
                            }
                        }
                    });
                }  
            }
        });
        
        final JPopupMenu contextMenu = new JPopupMenu();
        JMenuItem printComponent = new JMenuItem("Print to Console");
        printComponent.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                com.codename1.ui.CN.callSerially(new Runnable() {
                    public void run() {
                        if (currentComponent != null) {
                            JavaSEPort.dumpComponentProperties(currentComponent);
                        }
                    }
                });
                
            }
            
        });
        contextMenu.add(printComponent);

        //if (currentComponent instanceof Container) {
            JMenuItem revalidate = new JMenuItem("Revalidate");
            revalidate.addActionListener(new java.awt.event.ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    com.codename1.ui.CN.callSerially(new Runnable() {
                        public void run() {
                            if (currentComponent instanceof Container) {
                                ((Container)currentComponent).revalidate();
                            }
                        }
                    });

                }

            });
            contextMenu.add(revalidate);
        //}
        
        componentTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {

                    int row = componentTree.getClosestRowForLocation(e.getX(), e.getY());
                    componentTree.setSelectionRow(row);
                    contextMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
            
        });
        
        componentTree.setCellRenderer(new DefaultTreeCellRenderer() {

            @Override
            public java.awt.Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                Component t = (Component)value;
                String newVal = t.getClass().getSimpleName();
                if(t.getName() != null) {
                    newVal += "[" + t.getName() + "]";
                } else {
                    newVal += "[Unnamed]";
                }
                newVal += ", " + t.getUIID();
                return super.getTreeCellRendererComponent(tree, newVal, sel, expanded, leaf, row, hasFocus);
            }
            
        });
        
        componentTree.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                if(e.getPath() != null) {
                    Form parentForm = Display.getInstance().getCurrent();
                    Component c = (Component)e.getPath().getLastPathComponent();
                    if(c != null) {
                        if(parentForm.getGlassPane() == null) {
                            parentForm.setGlassPane(new SelectedComponentGlassPane(c));
                        } else {
                            if(parentForm.getGlassPane() instanceof SelectedComponentGlassPane) {
                                SelectedComponentGlassPane s = (SelectedComponentGlassPane)parentForm.getGlassPane();
                                if(s.cmp != c) {
                                    s.cmp = c;
                                    parentForm.repaint();
                                }
                            }
                        }
                        setCurrentComponent(c);
                    } else {
                        if(parentForm.getGlassPane() != null && 
                            parentForm.getGlassPane() instanceof SelectedComponentGlassPane) {
                            parentForm.setGlassPane(null);
                            parentForm.repaint();
                        }
                        
                    }
                }
            }
        });
        //pack();
        //setLocationByPlatform(true);
        //setVisible(true);
    }
    
    private void loadThemes() {
        if (!Display.isInitialized()) return;
        java.util.List<File> resFiles = new ArrayList<File>();
        
        File[] tmpFiles = JavaSEPort.instance.getSourceResourcesDir().listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getName().endsWith(".res");
            }
        });
        if (tmpFiles != null) {
            resFiles.addAll(Arrays.asList(tmpFiles));
        }
        themes.removeAllItems();
        themePaths.clear();
        themeNames.clear();
        
        for(File r : resFiles) {
            try {
                Resources rr = Resources.open("/" + r.getName());
                for(String themeName : rr.getThemeResourceNames()) {
                    themes.addItem(r.getName() + " - " + themeName);
                    themePaths.add(r.getAbsolutePath());
                    themeNames.add(themeName);
                }
            } catch(IOException err) {
                err.printStackTrace();
            }
        }
        
    }

    private void refreshComponentTree() {
        TreePath tp = componentTree.getSelectionPath();
        ComponentTreeModel cm = new ComponentTreeModel(Display.getInstance().getCurrent());
        componentTree.setModel(cm);
        componentTree.setSelectionPath(tp);
        
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jSplitPane1 = new javax.swing.JSplitPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        componentTree = new javax.swing.JTree();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        componentClass = new javax.swing.JTextField();
        componentName = new javax.swing.JTextField();
        componentUIID = new javax.swing.JTextField();
        componentSelected = new javax.swing.JCheckBox();
        jLabel5 = new javax.swing.JLabel();
        layout = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        constraint = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        coordinates = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        preferredSize = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        padding = new javax.swing.JTextField();
        margin = new javax.swing.JTextField();
        unselected = new javax.swing.JButton();
        themes = new javax.swing.JComboBox();
        //jToolBar1 = new javax.swing.JToolBar();
        //refreshTree = new javax.swing.JButton();
       // validate = new javax.swing.JButton();

        FormListener formListener = new FormListener();

        //setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        //setTitle("Component Tree Inspector");

        jScrollPane1.setViewportView(componentTree);

        jSplitPane1.setLeftComponent(jScrollPane1);

        jLabel1.setText("Class");

        jLabel2.setText("Name");

        jLabel3.setText("UIID");

        jLabel4.setText("Selected");

        componentClass.setEditable(false);

        componentName.setEditable(false);

        componentSelected.setEnabled(false);

        jLabel5.setText("Layout");

        layout.setEditable(false);

        jLabel6.setText("Constraint");
        jLabel6.setToolTipText("Layout Constraint (for border layout)");

        constraint.setEditable(false);
        constraint.setToolTipText("Layout Constraint (for border layout)");

        jLabel7.setText("Coordinates");

        coordinates.setEditable(false);

        jLabel8.setText("Preferred Size");

        preferredSize.setEditable(false);

        jLabel9.setText("Padding");

        jLabel10.setText("Margin");

        padding.setEditable(false);

        margin.setEditable(false);

        unselected.setText("Edit");
        unselected.addActionListener(formListener);

        themes.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel5)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel4)
                            .addComponent(jLabel3)
                            .addComponent(jLabel2)
                            .addComponent(jLabel1)
                            .addComponent(jLabel6)
                            .addComponent(jLabel7)
                            .addComponent(jLabel8)
                            .addComponent(jLabel9)
                            .addComponent(jLabel10))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(componentClass)
                            .addComponent(componentName)
                            .addComponent(layout)
                            .addComponent(constraint)
                            .addComponent(coordinates)
                            .addComponent(preferredSize)
                            .addComponent(padding)
                            .addComponent(margin)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(componentSelected)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(componentUIID, javax.swing.GroupLayout.PREFERRED_SIZE, 192, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(themes, 0, 162, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(unselected, javax.swing.GroupLayout.PREFERRED_SIZE, 94, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(componentClass, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(componentName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(componentUIID, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(unselected)
                    .addComponent(themes, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel4)
                    .addComponent(componentSelected))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(layout, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(constraint, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(coordinates, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(preferredSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(padding, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10)
                    .addComponent(margin, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(33, Short.MAX_VALUE))
        );

        jSplitPane1.setRightComponent(jPanel1);

        add(jSplitPane1, java.awt.BorderLayout.CENTER);



    }

    

private void refreshTreeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_refreshTreeActionPerformed
    refreshComponentTree();
    loadThemes();
}//GEN-LAST:event_refreshTreeActionPerformed

    private void unselectedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_unselectedActionPerformed
        editStyle();
    }//GEN-LAST:event_unselectedActionPerformed

    private void showErrorMessage(final String message, final String title) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JOptionPane.showMessageDialog(ComponentTreeInspector.this, message, title, JOptionPane.ERROR_MESSAGE);
            }
        });
        
    }

    private void showInfoMessage(final String message, final String title) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JOptionPane.showMessageDialog(ComponentTreeInspector.this, message, title, JOptionPane.PLAIN_MESSAGE);
            }
        });
        
    }

    private void validateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_validateActionPerformed
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                final Form f = Display.getInstance().getCurrent();
                ArrayList<Container> scrollables = new ArrayList<Container>();
                findScrollableContainers(f.getContentPane(), scrollables);
                if(scrollables.size() > 1) {
                    for(final Container cnt : scrollables) {
                        for(final Container child : scrollables) {
                            if(cnt != child) {
                                if(isChildOf(cnt, child)) {
                                    String message = "Nested scrollable containers detected: ";
                                    if(cnt == f.getContentPane()) {
                                        message += "\nContent pane is scrollable";
                                    } else {
                                        message += "\nScrollable container named: " + cnt.getName();
                                    }
                                    if(child == f.getContentPane()) {
                                        message += "\nContent pane is scrollable";
                                    } else {
                                        message += "\nScrollable container named: " + child.getName();
                                    }
                                    showErrorMessage(message, "Nested Scrollables");
                                    return;
                                }
                            }
                        }
                    }
                }
                showInfoMessage("Validation finished without an error", "Validation");
            }
        });
    }//GEN-LAST:event_validateActionPerformed
    
    public boolean isChildOf(Container cnt, Component cmp) {
        while(cmp.getParent() != null) {
            cmp = cmp.getParent();
            if(cmp == cnt) {
                return true;
            }
        }
        return false;
    }
    
    private void findScrollableContainers(Container cnt, List<Container> response) {
        try {
            Method m = Container.class.getDeclaredMethod("scrollableYFlag");
            m.setAccessible(true);
            Boolean res = (Boolean)m.invoke(cnt);
            if(res) {
                response.add(cnt);
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        for(Component c : cnt) {
            if(c instanceof Container) {
                findScrollableContainers((Container)c, response);
            }
        }
    }
    
    private void editStyle() {
        File cn1dir = new File(System.getProperty("user.home"), ".codenameone");
        if(!cn1dir.exists()) {
            JOptionPane.showMessageDialog(this, "Please open the designer once by opening the theme.res file", "Error Opening Designer", JOptionPane.ERROR_MESSAGE);
            return;
        }
        File resourceEditor = new File(cn1dir, "designer_1.jar");
        if(!resourceEditor.exists()) {
            resourceEditor = new File(cn1dir, "designer.jar");
        }
        if(!resourceEditor.exists()) {
            JOptionPane.showMessageDialog(this, "Please open the designer once by opening the theme.res file", "Error Opening Designer", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        File javaBin = new File(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java.exe");
        if(!javaBin.exists()) {
            javaBin = new File(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
        }
        final File javaExe = javaBin;
        final File resourceEditorFinal = resourceEditor;
        final String themeFile = themePaths.get(themes.getSelectedIndex());
        if (themeFile.endsWith(".css.res")) {
            File srcDir = new File(themeFile).getParentFile();
            File projectDir = srcDir.getParentFile();
            File cssDir = new File(projectDir, "css");
            String cssThemeFileName = new File(themeFile).getName();
            int lastDot = cssThemeFileName.lastIndexOf(".");
            if (lastDot >= 0) {
                cssThemeFileName = cssThemeFileName.substring(0, lastDot);
            }
            File cssThemeFile = new File(cssDir, cssThemeFileName);
            System.out.println("Trying to open "+cssThemeFile);
            if (cssThemeFile.exists()) {
                try {
                    Desktop.getDesktop().edit(cssThemeFile);
                } catch (Throwable t) {
                    t.printStackTrace();
                    
                }
                return;
            }
        }
        final String themeName = themeNames.get(themes.getSelectedIndex());
        final String uiid = componentUIID.getText();
        
        new Thread() {
            @Override
            public void run() {
                try {
                    ProcessBuilder pb = new ProcessBuilder(javaExe.getAbsolutePath(), "-jar", 
                                    resourceEditorFinal.getAbsolutePath(), "-style", themeFile, uiid, themeName).inheritIO();
                    Process proc = pb.start();
                    proc.waitFor();
                    Display.getInstance().callSerially(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Resources res = Resources.open(new FileInputStream(themeFile));
                                UIManager.getInstance().addThemeProps(res.getTheme(themeName));
                                Display.getInstance().getCurrent().refreshTheme();
                                Display.getInstance().getCurrent().revalidate();
                            } catch(Exception err) {
                                err.printStackTrace();
                            }
                        }
                    });
                } catch(Exception err) {
                    err.printStackTrace();
                }
            }
        }.start();
    }
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField componentClass;
    private javax.swing.JTextField componentName;
    private javax.swing.JCheckBox componentSelected;
    private javax.swing.JTree componentTree;
    private javax.swing.JTextField componentUIID;
    private javax.swing.JTextField constraint;
    private javax.swing.JTextField coordinates;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSplitPane jSplitPane1;
    //private javax.swing.JToolBar jToolBar1;
    private javax.swing.JTextField layout;
    private javax.swing.JTextField margin;
    private javax.swing.JTextField padding;
    private javax.swing.JTextField preferredSize;
    //private javax.swing.JButton refreshTree;
    private javax.swing.JComboBox themes;
    private javax.swing.JButton unselected;
    private javax.swing.JButton validate;
    // End of variables declaration//GEN-END:variables
    
    // Code for dispatching events from components to event handlers.

    private class FormListener implements java.awt.event.ActionListener {
        FormListener() {}
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            if (evt.getSource() == unselected) {
                ComponentTreeInspector.this.unselectedActionPerformed(evt);
            }
            //else if (evt.getSource() == refreshTree) {
            //    ComponentTreeInspector.this.refreshTreeActionPerformed(evt);
            //}
            else if (evt.getSource() == validate) {
                ComponentTreeInspector.this.validateActionPerformed(evt);
            }
        }
    }// </editor-fold>



    public class RefreshAction extends AbstractAction {

        RefreshAction() {
            super("", SwingUtils.getImageIcon(ComponentTreeInspector.class.getResource("refresh.png"), 24, 24));
            setToolTipText("Refresh comonent tree");
        }



        @Override
        public void actionPerformed(ActionEvent e) {
            ComponentTreeInspector.this.refreshComponentTree();
            loadThemes();
        }
    }

    public class ValidateAction extends AbstractAction {

        ValidateAction() {
            super("", SwingUtils.getImageIcon(ComponentTreeInspector.class.getResource("baseline_rule_black_24dp.png"), 24, 24));
            putValue(SHORT_DESCRIPTION, "Validate component tree and report problems.");

            
        }



        @Override
        public void actionPerformed(ActionEvent e) {
            ComponentTreeInspector.this.validateActionPerformed(e);
        }
    }
    
    private ImageIcon getToggleInspectSimulatorIcon() {
        if (simulatorRightClickEnabled) {
            return SwingUtils.getImageIcon(JavaSEPort.class.getResource("arrow_24_black.png"), 24, 24);
        } else {
            return SwingUtils.getImageIcon(JavaSEPort.class.getResource("arrow_24_disabled.png"), 24, 24);
        }
    }

    public class ToggleInspectSimulatorAction extends AbstractAction {

        ToggleInspectSimulatorAction() {
            super("", getToggleInspectSimulatorIcon());

            //putValue(SELECTED_KEY, simulatorRightClickEnabled);
            setShortDescription();
        }

        private void setShortDescription() {
            if (simulatorRightClickEnabled) {
                putValue(SHORT_DESCRIPTION, "'Right-click' in simulator to inspect elements is currently enabled.  Click to disable.");
            } else {
                putValue(SHORT_DESCRIPTION, "Enable 'Right-click' in simulator to inspect elements");
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            simulatorRightClickEnabled = !simulatorRightClickEnabled;
            //putValue(SELECTED_KEY, simulatorRightClickEnabled);
            putValue(SMALL_ICON, getToggleInspectSimulatorIcon());
            putValue(LARGE_ICON_KEY, getToggleInspectSimulatorIcon());
            setShortDescription();
        }
    }
    
    public void inspectComponent(com.codename1.ui.Component cmp) {
        ComponentTreeInspector.this.refreshComponentTree();
        loadThemes();
        TreePath path = ((ComponentTreeModel)componentTree.getModel()).createPathToComponent(cmp);
        componentTree.setSelectionPath(path);
        componentTree.scrollPathToVisible(path);
        
    }

    public void setCurrentComponent(Component c) {
        currentComponent = c;
        propertyDetailsPanel.setCurrentComponent(c);
        componentClass.setText(c.getClass().getName());
        componentName.setText("" + c.getName());
        componentUIID.setText("" + c.getUIID());
        componentSelected.setSelected(c.hasFocus());
        coordinates.setText("x: " + c.getX() + " y: " + c.getY() + " absX: " + c.getAbsoluteX()+ " absY: " + c.getAbsoluteY() +
                " Width: " + c.getWidth() + " Height: " + c.getHeight());
        preferredSize.setText(c.getPreferredW() + ", " + c.getPreferredH());
        padding.setText("Top: " + c.getStyle().getPadding(Component.TOP) + " Bottom: " + c.getStyle().getPadding(Component.BOTTOM)
                + " Left: " + c.getStyle().getPadding(Component.LEFT) + " Right: " + c.getStyle().getPadding(Component.RIGHT));
        margin.setText("Top: " + c.getStyle().getMargin(Component.TOP) + " Bottom: " + c.getStyle().getMargin(Component.BOTTOM)
                + " Left: " + c.getStyle().getMargin(Component.LEFT) + " Right: " + c.getStyle().getMargin(Component.RIGHT));
        if(c instanceof com.codename1.ui.Container) {
            layout.setText(((com.codename1.ui.Container)c).getLayout().getClass().getSimpleName());
        } else {
            layout.setText("");
        }
        com.codename1.ui.Container parent = c.getParent();
        constraint.setText("");
        if(parent != null) {
            Object o = parent.getLayout().getComponentConstraint(c);
            if(o != null) {
                constraint.setText(o.toString());
            }
        }
    }

    public PropertyDetailsPanel getPropertyDetailsPanel() {
        return propertyDetailsPanel;
    }

    public Component getCurrentComponent() {
        return currentComponent;
    }

}
