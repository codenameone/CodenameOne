/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */

package com.codename1.designer;

import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.impl.javase.JavaSEPortWithSVGSupport;
import com.codename1.ui.plaf.Accessor;
import com.codename1.ui.util.EditableResources;
import com.codename1.ui.util.UIBuilderOverride;
import java.io.IOException;
import java.util.jar.Attributes.Name;
import java.awt.BorderLayout;
import java.awt.Component;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.prefs.Preferences;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * UI and logic allowing us to pick the MIDlet which will appear in the preview
 * pane
 *
 * @author  Shai Almog
 */
public class PickMIDlet extends javax.swing.JPanel {
    private static CustomComponent[] customComponents;

    /** Creates new form PickMIDlet */
    public PickMIDlet() {
        initComponents();
        midletPicker.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                if(value == null) {
                    value = "";
                } else {
                    String s = (String)value;
                    value = s.substring(0, s.indexOf(','));
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });
        Preferences pref = Preferences.userNodeForPackage(ResourceEditorView.class);
        jarFile.setText(pref.get("jar", ""));
        updateMIDletList();
        midletPicker.setSelectedItem(pref.get("midlet", null));
        customComponents = null;
    }
    
    public static void showPickMIDletDialog(JComponent parent) {
        JDialog dlg = new JDialog((JFrame)SwingUtilities.getWindowAncestor(parent));
        dlg.setTitle("Pick MIDlet");
        dlg.getContentPane().setLayout(new BorderLayout());
        dlg.getContentPane().add(BorderLayout.CENTER, new PickMIDlet());
        dlg.setLocationByPlatform(true);
        dlg.pack();
        dlg.setVisible(true);
    }


    public static CustomComponent[] getCustomComponents() {
        if(customComponents == null) {
            Preferences pref = Preferences.userNodeForPackage(ResourceEditorView.class);
            String jar = pref.get("jar", null);
            if(jar != null) {
                try {
                    File jarFile = new File(jar);
                    URLClassLoader cl = URLClassLoader.newInstance(new URL[]{jarFile.toURI().toURL()}, PickMIDlet.class.getClassLoader());
                    JarFile zip = new JarFile(jarFile);
                    Enumeration<JarEntry> en = zip.entries();
                    List<CustomComponent> customList = new ArrayList<CustomComponent>();
                    while(en.hasMoreElements()) {
                        JarEntry e = en.nextElement();
                        String className = e.getName().replace("/", ".");
                        if(className.endsWith(".class") && !className.startsWith("com.codename1.ui.") && className.indexOf('$') < 0) {
                            className = className.substring(0, className.length() - 6);
                            try {
                                CustomComponent current = new CustomComponent();
                                current.setCls(cl.loadClass(className));
                                if(!current.getCls().getName().startsWith("com.codename1.ui")) {
                                    // check that it has a default constructor
                                    com.codename1.ui.Component cmp = (com.codename1.ui.Component)current.getCls().newInstance();
                                    current.setClassName(current.getCls().getName());
                                    current.setCodenameOneBaseClass(current.getCls().getName());
                                    current.setType(cmp.getUIID());
                                    customList.add(current);
                                    UIBuilderOverride.registerCustomComponent(cmp.getUIID(), current.getCls());
                                }
                            } catch (Throwable ex) {
                            }
                        }
                    }
                    customComponents = new CustomComponent[customList.size()];
                    customList.toArray(customComponents);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(JFrame.getFrames()[0], "IO Error when accessing MIDlet JAR File: " + ex, "IO Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
        return customComponents;
    }

    public static void resetSettings() {
        Preferences pref = Preferences.userNodeForPackage(ResourceEditorView.class);
        pref.remove("jar");
        pref.remove("midlet");
    }
    
    public static void startMIDlet(Hashtable themeHash) {
        Accessor.setTheme(themeHash);
        Preferences pref = Preferences.userNodeForPackage(ResourceEditorView.class);
        String jar = pref.get("jar", null);
        String midlet = pref.get("midlet", null);
        if(jar != null && midlet != null) {
            File jarFile = new File(jar);
            if(jarFile.exists()) {
                try {
                    URLClassLoader cl = URLClassLoader.newInstance(new URL[]{jarFile.toURI().toURL()}, PickMIDlet.class.getClassLoader());
                    StringTokenizer tokenizer = new StringTokenizer((String) midlet, " ,");
                    String s = tokenizer.nextToken();
                    while(tokenizer.hasMoreTokens()) {
                        s = tokenizer.nextToken();
                    }
                    Class cls = cl.loadClass(s);
                    JavaSEPortWithSVGSupport.setClassLoader(cls);
                    EditableResources.setResourcesClassLoader(cls);
                    Accessor.setTheme(themeHash);
                    
                    Object app = cls.newInstance();
                    
                    JarFile zip = new JarFile(jarFile);
                    Attributes m = zip.getManifest().getMainAttributes();
                    
                    cls.getDeclaredMethod("init", new Class[] {Object.class}).invoke(app, new Object[] {null});
                    cls.getDeclaredMethod("start", new Class[0]).invoke(app, new Object[0]);
                    
                    // there might be an ongoing transition and the form.show() method is serial
                    Display.getInstance().callSerially(new Runnable() {
                        public void run() {
                            if(Display.getInstance().getCurrent() != null) {
                                Display.getInstance().getCurrent().refreshTheme();
                                return;
                            } else {
                                new Thread() {
                                    public void run() {
                                        try {
                                            sleep(100);
                                        } catch (InterruptedException ex) {
                                            ex.printStackTrace();
                                        }
                                        Display.getInstance().callSerially(this);
                                    }
                                }.start();
                            }
                        }
                    });
                    return;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
        
        JavaSEPortWithSVGSupport.setClassLoader(PickMIDlet.class);
        LiveDemo l = new LiveDemo();
        l.init(null);
        l.start();
        Accessor.setTheme(themeHash);
        Form current = Display.getInstance().getCurrent();
        if(current != null) {
            current.refreshTheme();
        }
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel2 = new javax.swing.JLabel();
        jarFile = new javax.swing.JTextField();
        pickJarFile = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        midletPicker = new javax.swing.JComboBox();
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextPane1 = new javax.swing.JTextPane();

        FormListener formListener = new FormListener();

        setName("Form"); // NOI18N

        jLabel2.setText("JAR File");
        jLabel2.setName("jLabel2"); // NOI18N

        jarFile.setEditable(false);
        jarFile.setName("jarFile"); // NOI18N

        pickJarFile.setText("...");
        pickJarFile.setName("pickJarFile"); // NOI18N
        pickJarFile.addActionListener(formListener);

        jLabel3.setText("MIDlet");
        jLabel3.setName("jLabel3"); // NOI18N

        midletPicker.setName("midletPicker"); // NOI18N

        okButton.setText("OK");
        okButton.setEnabled(false);
        okButton.setName("okButton"); // NOI18N
        okButton.addActionListener(formListener);

        cancelButton.setText("Cancel");
        cancelButton.setName("cancelButton"); // NOI18N
        cancelButton.addActionListener(formListener);

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        jTextPane1.setContentType("text/html");
        jTextPane1.setEditable(false);
        jTextPane1.setText("<html>\r\n  <head>\r\n\r\n  </head>\r\n  <body>\r\n    <p style=\"margin-top: 0\">\r\n      Allows selecting an Application to execute within the preview window of the theme view when\ncreating a theme for a specific Application. The preview might not reflect the full set of changes \nfrom the theme but would provide a good estimate for a designer. This application can also expose\nadditional custom components to the GUI designer view simply by creating valid Codename One component\nsubclasses with default constructors.\r\n    </p>\r\n    <p>\n    The picked Application MUST NOT be obfuscated, this allows the Codename One Designer to replace the \nApplications LWUIT version with its own. \n    </p>\n    <p>\n    To debug pick Application run the Codename One Designer with a console or from command line and look at the\nerrors printed out when opening a theme.\n    </p>\n  </body>\r\n</html>\r\n"); // NOI18N
        jTextPane1.setName("jTextPane1"); // NOI18N
        jScrollPane1.setViewportView(jTextPane1);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel2)
                    .add(jLabel3))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(midletPicker, 0, 274, Short.MAX_VALUE)
                    .add(jarFile, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 274, Short.MAX_VALUE)
                    .add(layout.createSequentialGroup()
                        .add(okButton)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(cancelButton)))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(pickJarFile)
                .addContainerGap())
            .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 452, Short.MAX_VALUE)
        );

        layout.linkSize(new java.awt.Component[] {cancelButton, okButton}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel2)
                    .add(jarFile, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(pickJarFile))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel3)
                    .add(midletPicker, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(okButton)
                    .add(cancelButton))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 247, Short.MAX_VALUE))
        );
    }

    // Code for dispatching events from components to event handlers.

    private class FormListener implements java.awt.event.ActionListener {
        FormListener() {}
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            if (evt.getSource() == pickJarFile) {
                PickMIDlet.this.pickJarFileActionPerformed(evt);
            }
            else if (evt.getSource() == okButton) {
                PickMIDlet.this.okButtonActionPerformed(evt);
            }
            else if (evt.getSource() == cancelButton) {
                PickMIDlet.this.cancelButtonActionPerformed(evt);
            }
        }
    }// </editor-fold>//GEN-END:initComponents

    private void updateMIDletList() {
        String j = jarFile.getText();
        if(j != null && j.length() > 0) {
            File file = new File(j);
            if(file.exists() && file.isFile()) {
                try {
                    JarFile zip = new JarFile(file);
                    Attributes m = zip.getManifest().getMainAttributes();
                    Vector v = new Vector();
                    for (int i = 1 ; m.getValue("MIDlet-" + i) != null ; i++) {
                        v.addElement(m.getValue("MIDlet-" + i));
                    }
                    midletPicker.setModel(new DefaultComboBoxModel(v));
                } catch (IOException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this, "An IO Error occured while accessing\n" +
                        "the MIDlet JAR:\n" + ex.getMessage(), "IO Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private void selectFile(JTextField field, String type) {
        JFileChooser c = ResourceEditorView.createFileChooser("*" + type, type);
        if(JFileChooser.APPROVE_OPTION == c.showDialog(this, "Select")) {
            File f = c.getSelectedFile();
            field.setText(f.getAbsolutePath());
            Preferences.userNodeForPackage(ResourceEditorView.class).put("lastDir", f.getParentFile().getAbsolutePath());
        }
        updateMIDletList();
        if(midletPicker.getModel().getSize() > 0) {
            midletPicker.setSelectedIndex(0);
        }
        updateOKEnabled();
    }

    private void updateOKEnabled() {
        if(new File(jarFile.getText()).exists()) {
            okButton.setEnabled(true);
        }
    }

private void pickJarFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pickJarFileActionPerformed
    selectFile(jarFile, ".jar");
}//GEN-LAST:event_pickJarFileActionPerformed

private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
    SwingUtilities.windowForComponent(this).dispose();
    Preferences pref = Preferences.userNodeForPackage(ResourceEditorView.class);
    pref.put("jar", jarFile.getText());
    pref.put("midlet", (String)midletPicker.getSelectedItem());
}//GEN-LAST:event_okButtonActionPerformed

private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
    SwingUtilities.windowForComponent(this).dispose();
}//GEN-LAST:event_cancelButtonActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancelButton;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextPane jTextPane1;
    private javax.swing.JTextField jarFile;
    private javax.swing.JComboBox midletPicker;
    private javax.swing.JButton okButton;
    private javax.swing.JButton pickJarFile;
    // End of variables declaration//GEN-END:variables

}
