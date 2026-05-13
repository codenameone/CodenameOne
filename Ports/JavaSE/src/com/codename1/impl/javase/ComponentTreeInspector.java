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
        final String uiid = componentUIID.getText();
        final int themeIdx = themes.getSelectedIndex();

        // First try to open the CSS source in the user's IDE at the UIID's line. This is the
        // modern path: cn1 8.x projects are CSS-first, the .res is generated. We fall through to
        // the legacy designer launch only when there's no CSS source to point at.
        File cssThemeFile = locateCssThemeFile(themeIdx);
        if (cssThemeFile != null && cssThemeFile.exists()) {
            if (openCssInIde(cssThemeFile, uiid)) {
                return;
            }
        }

        if (themeIdx < 0 || themeIdx >= themePaths.size()) {
            JOptionPane.showMessageDialog(this,
                    "No theme is currently selected.",
                    "Edit Style", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Prefer the version-pinned designer jar that the Maven plugin pulled into m2.
        // Fallback to the legacy ~/.codenameone/designer_*.jar files (managed by UpdateCodenameOne).
        File resourceEditor = null;
        if (System.getProperty("codename1.designer.jar", null) != null) {
            resourceEditor = new File(System.getProperty("codename1.designer.jar"));
        }
        if (resourceEditor == null || !resourceEditor.exists()) {
            File m2Designer = com.codename1.impl.javase.util.MavenUtils.findDesignerJarInM2();
            if (m2Designer != null) {
                resourceEditor = m2Designer;
            }
        }
        if (resourceEditor == null || !resourceEditor.exists()) {
            File cn1dir = new File(System.getProperty("user.home"), ".codenameone");
            if(!cn1dir.exists()) {
                JOptionPane.showMessageDialog(this, "Please open the designer once by opening the theme.res file", "Error Opening Designer", JOptionPane.ERROR_MESSAGE);
                return;
            }
            resourceEditor = new File(cn1dir, "designer_1.jar");
            if(!resourceEditor.exists()) {
                resourceEditor = new File(cn1dir, "designer.jar");
            }
            if(!resourceEditor.exists()) {
                JOptionPane.showMessageDialog(this, "Please open the designer once by opening the theme.res file", "Error Opening Designer", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        File javaBin = new File(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java.exe");
        if(!javaBin.exists()) {
            javaBin = new File(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
        }
        final File javaExe = javaBin;
        final File resourceEditorFinal = resourceEditor;
        final String themeFile = themePaths.get(themeIdx);
        final String themeName = themeNames.get(themeIdx);
        
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

    /**
     * Resolves the {@code theme.css} source that backs the currently selected {@code *.css.res}
     * theme, or — when no theme is selected — falls back to the conventional
     * {@code ${project}/css/theme.css} location used by cn1 Maven projects.
     */
    private File locateCssThemeFile(int themeIdx) {
        if (themeIdx >= 0 && themeIdx < themePaths.size()) {
            String themeFile = themePaths.get(themeIdx);
            if (themeFile.endsWith(".css.res")) {
                File resFile = new File(themeFile);
                File srcDir = resFile.getParentFile();
                File projectDir = srcDir == null ? null : srcDir.getParentFile();
                if (projectDir != null) {
                    String name = resFile.getName();
                    int dot = name.lastIndexOf('.');
                    if (dot >= 0) {
                        name = name.substring(0, dot);
                    }
                    File cssThemeFile = new File(new File(projectDir, "css"), name);
                    if (cssThemeFile.exists()) {
                        return cssThemeFile;
                    }
                }
            }
        }
        File defaultCss = new File(JavaSEPort.getCWD(), "css" + File.separator + "theme.css");
        if (defaultCss.exists()) {
            return defaultCss;
        }
        return null;
    }

    /**
     * Opens {@code cssFile} in the user's IDE, jumping to the first selector that matches
     * {@code uiid}. Detects the IDE from project marker directories ({@code .idea},
     * {@code .vscode}, {@code nbproject}, {@code .project}) and falls back through a chain of
     * known launcher locations. When everything fails, falls back to
     * {@link Desktop#edit(File)} so the file still opens (just without a line jump).
     *
     * @return {@code true} if any opener — IDE-specific or Desktop fallback — was launched.
     */
    private boolean openCssInIde(File cssFile, String uiid) {
        int line = findUIIDLine(cssFile, uiid);
        File projectRoot = findProjectRoot(cssFile);

        String override = System.getProperty("codename1.ide.command");
        if (override != null && !override.trim().isEmpty()) {
            if (runIdeTemplate(override, cssFile, line)) {
                return true;
            }
        }

        if (projectRoot != null) {
            if (new File(projectRoot, ".idea").isDirectory() || hasFileWithExtension(projectRoot, ".iml")) {
                if (tryLaunch(intellijLaunchers(), new String[] {"--line", "{line}", "{file}"}, cssFile, line)) {
                    return true;
                }
            }
            if (new File(projectRoot, ".vscode").isDirectory()) {
                if (tryLaunch(vscodeLaunchers(), new String[] {"-g", "{file}:{line}"}, cssFile, line)) {
                    return true;
                }
            }
            if (new File(projectRoot, "nbproject").isDirectory()) {
                if (tryLaunch(netbeansLaunchers(), new String[] {"--open", "{file}:{line}"}, cssFile, line)) {
                    return true;
                }
            }
            // Eclipse (.project + .classpath) has no usable line-jump CLI; fall through to Desktop.
        }

        try {
            Desktop.getDesktop().edit(cssFile);
            return true;
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return false;
    }

    /**
     * Walks up from {@code start} looking for the project root, identified by a {@code pom.xml}
     * or any of the IDE marker directories. Returns {@code null} when no marker is found.
     */
    private File findProjectRoot(File start) {
        File dir = start.isDirectory() ? start : start.getParentFile();
        while (dir != null) {
            if (new File(dir, "pom.xml").isFile()
                    || new File(dir, ".idea").isDirectory()
                    || new File(dir, ".vscode").isDirectory()
                    || new File(dir, "nbproject").isDirectory()
                    || new File(dir, ".project").isFile()
                    || hasFileWithExtension(dir, ".iml")) {
                return dir;
            }
            dir = dir.getParentFile();
        }
        return null;
    }

    private boolean hasFileWithExtension(File dir, final String ext) {
        File[] matches = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isFile() && pathname.getName().endsWith(ext);
            }
        });
        return matches != null && matches.length > 0;
    }

    /**
     * Scans {@code cssFile} for the first selector that names {@code uiid}, matching common
     * forms: {@code UIID {}, UIID:pressed {}, Other, UIID {}}. Returns 1 when no match is found
     * so the IDE still opens the file at the top.
     */
    private int findUIIDLine(File cssFile, String uiid) {
        if (uiid == null || uiid.isEmpty()) {
            return 1;
        }
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "(^|[\\s,])" + java.util.regex.Pattern.quote(uiid) + "\\s*([{:,]|$)");
        java.io.BufferedReader r = null;
        try {
            r = new java.io.BufferedReader(new java.io.InputStreamReader(
                    new FileInputStream(cssFile), "UTF-8"));
            String line;
            int n = 0;
            while ((line = r.readLine()) != null) {
                n++;
                // Skip pure comment lines to reduce noise; we don't attempt full CSS parsing.
                String trimmed = line.trim();
                if (trimmed.startsWith("/*") || trimmed.startsWith("//") || trimmed.startsWith("*")) {
                    continue;
                }
                if (p.matcher(line).find()) {
                    return n;
                }
            }
        } catch (IOException ignored) {
        } finally {
            if (r != null) {
                try { r.close(); } catch (IOException ignored) {}
            }
        }
        return 1;
    }

    private boolean tryLaunch(List<String> candidates, String[] argsTemplate, File file, int line) {
        for (String c : candidates) {
            File resolved = resolveLauncher(c);
            if (resolved == null) {
                continue;
            }
            List<String> cmd = new ArrayList<String>();
            cmd.add(resolved.getAbsolutePath());
            for (String t : argsTemplate) {
                cmd.add(t.replace("{file}", file.getAbsolutePath())
                         .replace("{line}", String.valueOf(line)));
            }
            try {
                new ProcessBuilder(cmd).inheritIO().start();
                return true;
            } catch (IOException ignored) {
                // Try the next candidate.
            }
        }
        return false;
    }

    private boolean runIdeTemplate(String template, File file, int line) {
        String expanded = template.replace("{file}", file.getAbsolutePath())
                                  .replace("{line}", String.valueOf(line));
        List<String> tokens = splitCommandLine(expanded);
        if (tokens.isEmpty()) {
            return false;
        }
        try {
            new ProcessBuilder(tokens).inheritIO().start();
            return true;
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private List<String> splitCommandLine(String s) {
        // Minimal shell-style splitter: respects single and double quotes, treats backslash as
        // an escape inside double quotes. Sufficient for paths with spaces on macOS/Linux/Windows.
        List<String> out = new ArrayList<String>();
        StringBuilder cur = new StringBuilder();
        boolean inSingle = false, inDouble = false;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (inSingle) {
                if (ch == '\'') { inSingle = false; } else { cur.append(ch); }
            } else if (inDouble) {
                if (ch == '\\' && i + 1 < s.length()) { cur.append(s.charAt(++i)); }
                else if (ch == '"') { inDouble = false; }
                else { cur.append(ch); }
            } else if (ch == '\'') {
                inSingle = true;
            } else if (ch == '"') {
                inDouble = true;
            } else if (Character.isWhitespace(ch)) {
                if (cur.length() > 0) { out.add(cur.toString()); cur.setLength(0); }
            } else {
                cur.append(ch);
            }
        }
        if (cur.length() > 0) {
            out.add(cur.toString());
        }
        return out;
    }

    private File resolveLauncher(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        File f = new File(name);
        if (f.isAbsolute()) {
            return f.canExecute() ? f : null;
        }
        String path = System.getenv("PATH");
        if (path == null) {
            return null;
        }
        boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
        String[] exts = windows ? new String[] {"", ".exe", ".cmd", ".bat"} : new String[] {""};
        for (String dir : path.split(java.util.regex.Pattern.quote(File.pathSeparator))) {
            for (String ext : exts) {
                File candidate = new File(dir, name + ext);
                if (candidate.isFile() && candidate.canExecute()) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private List<String> intellijLaunchers() {
        // Tried in order. Includes Toolbox-managed scripts and stock app-bundle launcher paths
        // so cn1 simulator can find the IDE even when its CLI shim isn't on PATH (common on
        // macOS GUI sessions).
        List<String> out = new ArrayList<String>();
        out.add("idea");
        out.add("idea-ce");
        out.add("idea-ultimate");
        out.add("studio");
        String os = System.getProperty("os.name", "").toLowerCase();
        String home = System.getProperty("user.home", "");
        if (os.contains("mac")) {
            out.add(home + "/Library/Application Support/JetBrains/Toolbox/scripts/idea");
            out.add(home + "/Library/Application Support/JetBrains/Toolbox/scripts/studio");
            out.add("/Applications/IntelliJ IDEA.app/Contents/MacOS/idea");
            out.add("/Applications/IntelliJ IDEA CE.app/Contents/MacOS/idea");
            out.add("/Applications/IntelliJ IDEA Ultimate.app/Contents/MacOS/idea");
            out.add("/Applications/Android Studio.app/Contents/MacOS/studio");
        } else if (os.contains("win")) {
            String localAppData = System.getenv("LOCALAPPDATA");
            if (localAppData != null) {
                out.add(localAppData + "\\JetBrains\\Toolbox\\scripts\\idea.cmd");
                out.add(localAppData + "\\JetBrains\\Toolbox\\scripts\\studio.cmd");
            }
        } else {
            out.add(home + "/.local/share/JetBrains/Toolbox/scripts/idea");
            out.add(home + "/.local/share/JetBrains/Toolbox/scripts/studio");
            out.add("/snap/bin/intellij-idea-community");
            out.add("/snap/bin/intellij-idea-ultimate");
            out.add("/snap/bin/android-studio");
        }
        return out;
    }

    private List<String> vscodeLaunchers() {
        List<String> out = new ArrayList<String>();
        out.add("code");
        out.add("code-insiders");
        out.add("cursor");
        out.add("codium");
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("mac")) {
            out.add("/Applications/Visual Studio Code.app/Contents/Resources/app/bin/code");
            out.add("/Applications/Visual Studio Code - Insiders.app/Contents/Resources/app/bin/code");
            out.add("/Applications/Cursor.app/Contents/Resources/app/bin/code");
            out.add("/Applications/VSCodium.app/Contents/Resources/app/bin/codium");
        } else if (os.contains("win")) {
            String localAppData = System.getenv("LOCALAPPDATA");
            if (localAppData != null) {
                out.add(localAppData + "\\Programs\\Microsoft VS Code\\bin\\code.cmd");
                out.add(localAppData + "\\Programs\\Microsoft VS Code Insiders\\bin\\code-insiders.cmd");
                out.add(localAppData + "\\Programs\\cursor\\resources\\app\\bin\\code.cmd");
            }
        } else {
            out.add("/usr/share/code/bin/code");
            out.add("/snap/bin/code");
        }
        return out;
    }

    private List<String> netbeansLaunchers() {
        List<String> out = new ArrayList<String>();
        out.add("netbeans");
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("mac")) {
            File apps = new File("/Applications");
            File[] children = apps.exists() ? apps.listFiles() : null;
            if (children != null) {
                for (File c : children) {
                    if (c.getName().startsWith("NetBeans") || c.getName().startsWith("Apache NetBeans")) {
                        File launcher = new File(c, "Contents/MacOS/netbeans");
                        if (launcher.isFile()) {
                            out.add(launcher.getAbsolutePath());
                        }
                    }
                }
            }
        } else if (os.contains("win")) {
            String programFiles = System.getenv("ProgramFiles");
            if (programFiles != null) {
                out.add(programFiles + "\\NetBeans\\bin\\netbeans64.exe");
                out.add(programFiles + "\\NetBeans\\bin\\netbeans.exe");
            }
        } else {
            out.add("/usr/local/netbeans/bin/netbeans");
            out.add("/snap/bin/netbeans");
        }
        return out;
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
