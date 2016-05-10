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

import com.codename1.analytics.AnalyticsService;
import com.codename1.impl.javase.JavaSEPortWithSVGSupport;
import com.l2fprod.common.swing.JOutlookBar;
import com.codename1.ui.resource.util.BlockingAction;
import com.codename1.ui.resource.util.QuitAction;
import com.codename1.ui.EditorFont;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.plaf.Accessor;
import com.codename1.ui.plaf.Border;
import com.codename1.tools.resourcebuilder.ThemeTaskConstants;
import com.codename1.ui.util.EditableResources;
import com.codename1.ui.Font;
import com.codename1.ui.animations.AnimationAccessor;
import com.codename1.ui.animations.Timeline;
import com.codename1.impl.javase.SVG;
import com.codename1.ui.Display;
import com.codename1.ui.animations.AnimationObject;
import com.codename1.ui.util.Resources;
import com.codename1.ui.util.UIBuilder;
import com.codename1.ui.util.UIBuilderOverride;
import java.awt.Component;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ComboBoxModel;
import javax.swing.JTree;
import javax.swing.event.ChangeEvent;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import javax.swing.Icon;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.FileDialog;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import java.util.prefs.Preferences;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ProgressMonitor;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.event.ChangeListener;
import javax.swing.tree.TreePath;
import org.jdesktop.swingx.JXComboBox;

/**
 * The application's main frame UI modeled around the app framework logic
 *
 * @author Shai Almog
 */
public class ResourceEditorView extends FrameView {
    //private final static Hashtable<String, String> TYPE_MAPPING;
    //private RemoveResourceAction removeResourceAction = new RemoveResourceAction();
    private NewResourceAction newResourceAction = new NewResourceAction();
    private LoadResourceFileAction loadResourceFileAction = new LoadResourceFileAction();
    private SaveResourceFileAction saveResourceFileAction = new SaveResourceFileAction();
    private SaveResourceFileAsAction saveResourceFileAsAction = new SaveResourceFileAsAction();
    private SaveResourceFileAsNoSVGAction saveResourceFileAsNoSVGAction = new SaveResourceFileAsNoSVGAction();
    private ExportResourceFileAction exportResourceFileAction = new ExportResourceFileAction();
    private UndoAction undoAction = new UndoAction();
    private RedoAction redoAction = new RedoAction();
    private HelpAction helpAction = new HelpAction();
    private static final String IMAGE_DIR = "/com/codename1/designer/resources/";
        
    private static EditableResources loadedResources = new EditableResources();
    private Properties projectGeneratorSettings;
    private static String manualIDESettings;
    private List<String> recentFiles = new ArrayList<String>();
    private static File loadedFile;
    private File fileToLoad;
    private static File baseResDir;
    
    private String selectedResource = null;
    private HorizontalList themeList;
    private HorizontalList imageList;
    private HorizontalList imageListMain;
    private HorizontalList imageListSVG;
    private HorizontalList imageListMulti;
    private HorizontalList imageListTimeline;
    //private HorizontalList animationList;
    private HorizontalList fontList;
    private HorizontalList dataList;
    private HorizontalList l10nList;
    private HorizontalList uiList;
    private JXComboBox platformOverrides = new JXComboBox(new String[] {
            "[Base Resource]",
            "iOS (iPhone/iPod & iPad)",
            "iPhone/iPod",
            "iPad",
            "Android (phones & tablets)",
            "Android Phones",
            "Android Tablets",
            "Windows Phone 7",
            "RIM (Blackberry)",
            "J2ME",
            "Tablet (any)",
            "Phone (any)",
        });
    
    private static final String[] OVERRIDE_NAMES = {
            null,
            "ios",
            "iphone",
            "ipad",
            "android",
            "android-phone",
            "android-tab",
            "win",
            "rim",
            "me",
            "tablet",
            "phone",
    };
    
    
    public static File getLoadedFile() {
        return loadedFile;
    }

    public static File getBaseDir(){
        return baseResDir;
    }
    
    public static File getTemporarySaveOfCurrentFile() {
        try {
            File tmp = File.createTempFile("Temp", ".res");
            tmp.deleteOnExit();

            FileOutputStream tempOut = new FileOutputStream(tmp);
            
            loadedResources.save(tempOut);

            tempOut.close();
            
            return tmp;
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }
    
    public ResourceEditorView(SingleFrameApplication app, File fileToLoad) {
        super(app);
        ToolTipManager.sharedInstance().setInitialDelay(0);
        ToolTipManager.sharedInstance().setDismissDelay(10000);
        UIBuilder.setBlockAnalytics(true);
        AnalyticsService.init("MO-27921432-2", "designer.codenameone.com");
        AnalyticsService.visit("Startup", "");
        QuitAction.INSTANCE.setResource(loadedResources);
        initComponents();
                
        jMenu7.add(new AbstractAction("560") {
            @Override
            public void actionPerformed(ActionEvent e) {
                removeDPI(Display.DENSITY_560);
            }
        });
        
        jMenu7.add(new AbstractAction("2HD") {
            @Override
            public void actionPerformed(ActionEvent e) {
                removeDPI(Display.DENSITY_2HD);
            }
        });
        
        jMenu7.add(new AbstractAction("4k") {
            @Override
            public void actionPerformed(ActionEvent e) {
                removeDPI(Display.DENSITY_4K);
            }
        });
        
        livePreviewUI.setSelected(Preferences.userNodeForPackage(getClass()).getBoolean("LivePreview", false));
        boolean isXMLEnabled = Preferences.userNodeForPackage(getClass()).getBoolean("XMLFileMode", true);
        EditableResources.setXMLEnabled(isXMLEnabled);
        enableXMLTeamMode.setSelected(isXMLEnabled);
        
        initNativeTheme();
        LocalServer.startServer(mainPanel);
        if(ResourceEditorApp.IS_MAC) {
            fileMenu.remove(exitMenuItem);
            fileMenu.remove(jSeparator1);
            helpMenu.remove(jSeparator8);
            helpMenu.remove(about);
        }
        
        themeList = new HorizontalList(loadedResources, this);
        imageList = new HorizontalList(loadedResources, this, 40) {
            @Override
            public Icon getIconImage(String current) {
                return new CodenameOneImageIcon(loadedResources.getImage(current), getSettingsIconWidth(), getSettingsIconHeight());
            }

            @Override
            public String[] getEntries() {
                return loadedResources.getImageResourceNames();
            }
        };
        imageListMain = new HorizontalList(loadedResources, this, 40) {
            @Override
            public Icon getIconImage(String current) {
                return new CodenameOneImageIcon(loadedResources.getImage(current), getSettingsIconWidth(), getSettingsIconHeight());
            }

            @Override
            public String[] getEntries() {
                List<String> images = new ArrayList<String>();
                for(String i : loadedResources.getImageResourceNames()) {
                    com.codename1.ui.Image img = loadedResources.getImage(i);
                    if(img != null && (isImageInBorder(i) || isImageInTimeline(i))) {
                        continue;
                    }
                    images.add(i);
                }
                String[] result = new String[images.size()];
                images.toArray(result);
                return result;
            }
        };
        imageListSVG = new HorizontalList(loadedResources, this, 40) {
            @Override
            public Icon getIconImage(String current) {
                return new CodenameOneImageIcon(loadedResources.getImage(current), getSettingsIconWidth(), getSettingsIconHeight());
            }

            @Override
            public String[] getEntries() {
                List<String> images = new ArrayList<String>();
                for(String i : loadedResources.getImageResourceNames()) {
                    com.codename1.ui.Image img = loadedResources.getImage(i);
                    if(img != null && img.isSVG()) {
                        images.add(i);
                    }
                }
                String[] result = new String[images.size()];
                images.toArray(result);
                return result;
            }
        };
        imageListMulti = new HorizontalList(loadedResources, this, 40) {
            @Override
            public Icon getIconImage(String current) {
                return new CodenameOneImageIcon(loadedResources.getImage(current), getSettingsIconWidth(), getSettingsIconHeight());
            }

            @Override
            public String[] getEntries() {
                List<String> images = new ArrayList<String>();
                for(String i : loadedResources.getImageResourceNames()) {
                    com.codename1.ui.Image img = loadedResources.getImage(i);
                    if(img != null && img != loadedResources.getResourceObject(i)) {
                        images.add(i);
                    }
                }
                String[] result = new String[images.size()];
                images.toArray(result);
                return result;
            }
        };
        imageListTimeline = new HorizontalList(loadedResources, this, 40) {
            @Override
            public Icon getIconImage(String current) {
                return new CodenameOneImageIcon(loadedResources.getImage(current), getSettingsIconWidth(), getSettingsIconHeight());
            }

            @Override
            public String[] getEntries() {
                List<String> images = new ArrayList<String>();
                for(String i : loadedResources.getImageResourceNames()) {
                    com.codename1.ui.Image img = loadedResources.getImage(i);
                    if(img != null && img instanceof com.codename1.ui.animations.Timeline) {
                        images.add(i);
                    }
                }
                String[] result = new String[images.size()];
                images.toArray(result);
                return result;
            }
        };
        fontList = new HorizontalList(loadedResources, this) {
            @Override
            public Icon getIconImage(final String current) {
                return new Icon() {
                    public void paintIcon(java.awt.Component c, java.awt.Graphics g, int x, int y) {
                        try {
                            com.codename1.ui.Font f = getRes().getFont(current);
                            Constructor con = com.codename1.ui.Graphics.class.getDeclaredConstructor(Object.class);
                            con.setAccessible(true);
                            com.codename1.ui.Graphics codenameOneG = (com.codename1.ui.Graphics) con.newInstance(g.create());
                            codenameOneG.setColor(0);
                            if(f != null) {
                                codenameOneG.setFont(f);
                            }
                            codenameOneG.drawString(current, x, y);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }

                    public int getIconWidth() {
                        EditableResources res = getRes();
                        com.codename1.ui.Font f = res.getFont(current);
                        if(f == null) {
                            return 0;
                        }
                        return f.stringWidth(current);
                    }

                    public int getIconHeight() {
                        EditableResources res = getRes();
                        com.codename1.ui.Font f = res.getFont(current);
                        if(f == null) {
                            return 0;
                        }
                        return f.getHeight();
                    }
                };
            }

            @Override
            public String[] getEntries() {
                return getRes().getFontResourceNames();
            }
        };
        dataList = new HorizontalList(loadedResources, this) {
            @Override
            public Icon getIconImage(String current) {
                return null;
            }

            @Override
            public String[] getEntries() {
                return loadedResources.getDataResourceNames();
            }
        };
        uiList = new HorizontalList(loadedResources, this) {
            @Override
            public Icon getIconImage(String current) {
                UIBuilder uib = new UIBuilderOverride();
                try {
                    com.codename1.ui.Component cmp = uib.createContainer(loadedResources, current);
                    if(cmp instanceof com.codename1.ui.Dialog){
                        return new ImageIcon(ResourceEditorApp.class.getResource("/D.png"));
                    }else if(cmp instanceof com.codename1.ui.Form){
                        return new ImageIcon(ResourceEditorApp.class.getResource("/F.png"));                
                    }else{
                        return new ImageIcon(ResourceEditorApp.class.getResource("/C.png"));                                
                    }
                } catch (Exception e) {
                }
                return null;
            }

            @Override
            public String[] getEntries() {
                return loadedResources.getUIResourceNames();
            }
        };

        l10nList = new HorizontalList(loadedResources, this) {
            @Override
            public Icon getIconImage(String current) {
                return null;
            }

            @Override
            public String[] getEntries() {
                return loadedResources.getL10NResourceNames();
            }
        };
        themeScroll.setViewportView(themeList);
        imageScroll.setViewportView(imageList);
        mainImages.setViewportView(imageListMain);
        svgImages.setViewportView(imageListSVG);
        multiImages.setViewportView(imageListMulti);
        timelineImages.setViewportView(imageListTimeline);
        //animationScroll.setViewportView(animationList);
        fontsScroll.setViewportView(fontList);
        dataScroll.setViewportView(dataList);
        userInterfaceScroll.setViewportView(uiList);
        localizationScroll.setViewportView(l10nList);
        
        addActionToToolbar(newResourceAction);
        addActionToToolbar(loadResourceFileAction);
        addActionToToolbar(saveResourceFileAction);
        addActionToToolbar(saveResourceFileAsAction);
        addActionToToolbar(helpAction);
        toolbar.addSeparator();
        addActionToToolbar(QuitAction.INSTANCE); 
        toolbar.addSeparator();
        JLabel over = new JLabel("Override In Platform: ");
        toolbar.add(over);
        over.setToolTipText("<html><body>Works only when a resource is associated with a project.<br>"
                + "Sets a sub resource which is overlayed on top of the<br>"
                + "current resources to replace resources for a specific<br>"
                + "platform. E.g. allows adapting icons for a convention<br>"
                + "of a specific OS.");
        toolbar.add(over);
        platformOverrides.setEnabled(projectGeneratorSettings != null);
        toolbar.add(platformOverrides);
        platformOverrides.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                if(loadedFile == null && platformOverrides.getSelectedIndex() > 0) {
                    JOptionPane.showMessageDialog(mainPanel, "You must use a resource file placed in the src folder of a Codename One project", "Override", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if(loadedResources.isModified() && loadedResources.isOverrideMode()) {
                    int r = JOptionPane.showConfirmDialog(mainPanel, "Changing the overlay mode with unsaved changes might cause you to lose these changes.\n"
                            + "Do you want to save your changes?", "", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
                    if(r == JOptionPane.YES_OPTION) {
                        saveResourceFileAction.actionPerformed(new ActionEvent(saveMenuItem, 0, "save"));
                    } else {
                        if(r != JOptionPane.NO_OPTION) {
                            return;
                        }
                    }
                }
                File f = getPlatformOverrideFile();
                if(f != null) {
                    EditableResources platformOverrideResource = new EditableResources();
                    if(f.exists()) {
                        try {
                            platformOverrideResource.openFile(new FileInputStream(f));
                        } catch (IOException ex) {
                            ex.printStackTrace();
                            JOptionPane.showMessageDialog(mainPanel, "Error Loading File: " + ex, "IO Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                    loadedResources.setOverrideMode(platformOverrideResource, f);
                } else {
                    loadedResources.setOverrideMode(null, null);
                }
                refreshAll();
                setSelectedResource(selectedResource);
            }
        });

        String recentFileString = Preferences.userNodeForPackage(getClass()).get("recentFiles", null);
        if(recentFileString != null) {
            for(String f : recentFileString.split(";")) {
                if(new File(f).exists()) {
                    recentFiles.add(f);
                }
            }
        }

        loadedResources.setOnChange(new Runnable() {
            public void run() {
                undoAction.setEnabled(loadedResources.isUndoable());
                redoAction.setEnabled(loadedResources.isRedoable());
                saveResourceFileAsNoSVGAction.setEnabled(false);
                if(loadedResources != null) {
                    for(String s : loadedResources.getImageResourceNames()) {
                        com.codename1.ui.Image i = loadedResources.getImage(s);
                        if(i != null && i.isSVG()) {
                            saveResourceFileAsNoSVGAction.setEnabled(true);
                            return;
                        }
                    }
                }
            }
        });
        
        refreshRecentMenu();
        loadedResources.clear();
        loadedFile = null;
        getFrame().setTitle("Untitled - Codename One Designer");
        if(fileToLoad != null) {
            final File f = fileToLoad;
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    ResourceEditorView.this.fileToLoad = f;
                    loadResourceFileAction.actionPerformed(null);
                    getFrame().setTitle(f.getName() + " - Codename One Designer");
                }
            });
        }
        updateLoadedFile();
        //animationScroll.getViewport().setOpaque(false);
        dataScroll.getViewport().setOpaque(false);
        fontsScroll.getViewport().setOpaque(false);
        imageScroll.getViewport().setOpaque(false);
        mainImages.getViewport().setOpaque(false);
        svgImages.getViewport().setOpaque(false);
        multiImages.getViewport().setOpaque(false);
        timelineImages.getViewport().setOpaque(false);
        //jScrollPane2.getViewport().setOpaque(false);
        jScrollPane3.getViewport().setOpaque(false);
        localizationScroll.getViewport().setOpaque(false);
        themeScroll.getViewport().setOpaque(false);
        mainPanel.setOpaque(false);
        
        ButtonGroup bGroup = new ButtonGroup();
        String currentLF = UIManager.getLookAndFeel().getClass().getName();
        crossPlatformLFMenu.setSelected(currentLF.equals(UIManager.getCrossPlatformLookAndFeelClassName()));
        systemLFMenu.setSelected(currentLF.equals(UIManager.getSystemLookAndFeelClassName()));
    }

    public boolean isGuiBuilderApp(){
        if(loadedFile != null){
            File parentDir = loadedFile.getParentFile();
            File userStateMachineFile = new File(parentDir, "userclasses/StateMachine.java");
            if (userStateMachineFile.exists()) {
                return true;
            }
        }
        return false;
    }
    
    public File getPlatformOverrideFile() {
        if(loadedFile != null) {
            File overrideDir = new File(loadedFile.getParentFile().getParentFile(), "override");        
            if(!overrideDir.exists()) {
                int r = JOptionPane.showConfirmDialog(mainPanel, "The override directory doesn't exist, this feature will only work within a valid\nCodename One project, do you want to continue?", "No Override Folder",JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
                if(r != JOptionPane.YES_OPTION) {
                    return null;
                }
                overrideDir.mkdirs();
            }
            if(platformOverrides.getSelectedIndex() > 0) {
                String name = loadedFile.getName();
                name = name.substring(0, name.length() - 4);
                return new File(overrideDir, name + "_" + OVERRIDE_NAMES[platformOverrides.getSelectedIndex()] + ".ovr");
            }
        } 
        return null;
    }
    
    private void addActionToToolbar(Action a) {
        JButton b = toolbar.add(a);
        b.getAccessibleContext().setAccessibleName((String)a.getValue(Action.NAME));
        b.getAccessibleContext().setAccessibleDescription((String)a.getValue(Action.NAME));
    }

    public Properties getProjectGeneratorSettings() {
        return projectGeneratorSettings;
    }

    private void refreshAll() {
        themeList.refresh();
        imageList.refresh();
        imageListMain.refresh();
        imageListMulti.refresh();
        imageListSVG.refresh();
        imageListTimeline.refresh();
        //animationList.refresh();
        fontList.refresh();
        dataList.refresh();
        uiList.refresh();
        l10nList.refresh();

    }

    public void setSelectedResource(String selectedResource) {
        // this might occur if the user chose to create a resource and then pressed
        // cancel on the file chooser dialog, just don't do anything...
        if(selectedResource == null || loadedResources.getResourceObject(selectedResource) == null) {
            return;
        }
        AnalyticsService.visit(loadedResources.getResourceTypeAsString(selectedResource), loadedResources.getResourceTypeAsString(this.selectedResource));
        this.selectedResource = selectedResource;
        resourceEditor.removeAll();
        if(selectedResource != null) {
            // tree tries to restore selection sometimes with a non-existing resource:
            for(String s : loadedResources.getResourceNames()) {
                if(s.equals(selectedResource)) {
                    BaseForm b = (BaseForm)loadedResources.getResourceEditor(selectedResource, ResourceEditorView.this);
                    if(loadedResources.isOverrideMode() && !loadedResources.isOverridenResource(selectedResource)) {
                        b.setOverrideMode(true, mainPanel);
                    }
                    resourceEditor.add(BorderLayout.CENTER, b);
                    resourceEditor.revalidate();
                    resourceEditor.repaint();
                    //removeResourceAction.setEnabled(true);

                    // set the selected type to none
                    //selectedResourceType = null;
                    break;
                }                                
            }
            //removeAnimation.setEnabled(loadedResources.isAnimation(selectedResource));
        } 
        refreshAll();
        resourceEditor.repaint();
    }
    
    public String getSelectedResource() {
        return selectedResource;
    }
    
    /**
     * Invoked by the "..." button in the add theme entry dialog, allows us to add
     * an image on the fly while working on a theme
     */
    public void addNewImageWizard() {
        AddResourceDialog addResource = new AddResourceDialog(loadedResources, AddResourceDialog.IMAGE);
        
        if(JOptionPane.OK_OPTION == 
            JOptionPane.showConfirmDialog(mainPanel, addResource, "Add Image", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE)) {
            if(addResource.checkName(loadedResources)) {
                JOptionPane.showMessageDialog(mainPanel, "A resource with that name already exists", "Add Image", JOptionPane.ERROR_MESSAGE);
                addNewImageWizard();
                return;
            }
            
            // show the image editing dialog...
            ImageRGBEditor image = new ImageRGBEditor(loadedResources, null, this);
            image.setImage(com.codename1.ui.Image.createImage(5, 5));
            if(JOptionPane.OK_OPTION == 
                JOptionPane.showConfirmDialog(mainPanel, image, "Add Image", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE)) {
                loadedResources.setImage(addResource.getResourceName(), image.getImage());
            }
        }
    }

    /**
     * Invoked by the "..." button in the add theme entry dialog, allows us to add
     * a font on the fly while working on a theme
     */
    public void addNewFontWizard() {
        AddResourceDialog addResource = new AddResourceDialog(loadedResources, AddResourceDialog.FONT);
        
        if(JOptionPane.OK_OPTION == 
            JOptionPane.showConfirmDialog(mainPanel, addResource, "Add Font", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE)) {
            if(addResource.checkName(loadedResources)) {
                JOptionPane.showMessageDialog(mainPanel, "A resource with that name already exists", "Add Font", JOptionPane.ERROR_MESSAGE);
                addNewFontWizard();
                return;
            }
            
            // show the image editing dialog...
            FontEditor font = new FontEditor(loadedResources,
                        new EditorFont(com.codename1.ui.Font.createSystemFont(com.codename1.ui.Font.FACE_SYSTEM, com.codename1.ui.Font.STYLE_PLAIN, com.codename1.ui.Font.SIZE_MEDIUM),
                            null, "Arial-plain-12", true, RenderingHints.VALUE_TEXT_ANTIALIAS_ON,
                            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789.,;:!/\\*()[]{}|#$%^&<>?'\"+- "),
                        addResource.getResourceName()
                    );
            font.setFactoryCreation(true);
            if(JOptionPane.OK_OPTION == 
                JOptionPane.showConfirmDialog(mainPanel, font, "Add Font", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE)) {
                loadedResources.setFont(addResource.getResourceName(), font.createFont());
            }
        }
    }
    
    private void refreshRecentMenu() {
        recentMenu.removeAll();
        for(String file : recentFiles) {
            final File currentFile = new File(file);
            if(currentFile.exists()) {
                JMenuItem menuItem = new JMenuItem(currentFile.getName());
                recentMenu.add(menuItem);
                menuItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        fileToLoad = currentFile;
                        loadResourceFileAction.actionPerformed(null);
                    }
                });
            }
        }
        String recentFileString = "";
        for(String f : recentFiles) {
            recentFileString += f + ";";
        }
        Preferences.userNodeForPackage(getClass()).put("recentFiles", recentFileString);
    }
    
    
    private void updateLoadedFile() {
        if(ResourceEditorApp.IS_MAC) {
            for(java.awt.Window w : java.awt.Frame.getWindows()) {
                if(w instanceof JFrame) {
                    ((JFrame)w).getRootPane().putClientProperty("Window.documentFile", loadedFile);
                }
            }
        }
    }
    

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainPanel = new javax.swing.JPanel();
        jSplitPane1 = new javax.swing.JSplitPane();
        treeArea = new javax.swing.JPanel();
        jTabbedPane1 = new JOutlookBar();
        themePanel = new javax.swing.JPanel();
        addTheme = new javax.swing.JButton();
        themeScroll = new javax.swing.JScrollPane();
        jPanel1 = new javax.swing.JPanel();
        addUserInterface = new javax.swing.JButton();
        userInterfaceScroll = new javax.swing.JScrollPane();
        jPanel7 = new javax.swing.JPanel();
        mainImages = new javax.swing.JScrollPane();
        addImageMain = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        imageScroll = new javax.swing.JScrollPane();
        addImageAll = new javax.swing.JButton();
        jPanel11 = new javax.swing.JPanel();
        multiImages = new javax.swing.JScrollPane();
        addImageMulti = new javax.swing.JButton();
        jPanel9 = new javax.swing.JPanel();
        svgImages = new javax.swing.JScrollPane();
        addImageSVG = new javax.swing.JButton();
        jPanel10 = new javax.swing.JPanel();
        timelineImages = new javax.swing.JScrollPane();
        addImageTimeline = new javax.swing.JButton();
        addNewTimeline = new org.jdesktop.swingx.JXButton();
        jPanel4 = new javax.swing.JPanel();
        addFont = new javax.swing.JButton();
        fontsScroll = new javax.swing.JScrollPane();
        jPanel5 = new javax.swing.JPanel();
        addL10N = new javax.swing.JButton();
        localizationScroll = new javax.swing.JScrollPane();
        jPanel6 = new javax.swing.JPanel();
        addData = new javax.swing.JButton();
        dataScroll = new javax.swing.JScrollPane();
        resourceEditor = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        license = new javax.swing.JTextArea();
        jSeparator3 = new javax.swing.JSeparator();
        jToolBar1 = new javax.swing.JToolBar();
        menuBar = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        newMenuItem = new javax.swing.JMenuItem();
        openMenuItem = new javax.swing.JMenuItem();
        recentMenu = new javax.swing.JMenu();
        saveMenuItem = new javax.swing.JMenuItem();
        saveAsMenuItem = new javax.swing.JMenuItem();
        resPassword = new javax.swing.JMenuItem();
        importRes = new javax.swing.JMenuItem();
        exportRes = new javax.swing.JMenuItem();
        setupNetbeans = new javax.swing.JMenuItem();
        lookAndFeelMenu = new javax.swing.JMenu();
        systemLFMenu = new javax.swing.JRadioButtonMenuItem();
        crossPlatformLFMenu = new javax.swing.JRadioButtonMenuItem();
        jSeparator6 = new javax.swing.JSeparator();
        checkerboardColors = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JSeparator();
        enableXMLTeamMode = new javax.swing.JCheckBoxMenuItem();
        jSeparator1 = new javax.swing.JSeparator();
        exitMenuItem = new javax.swing.JMenuItem();
        editMenu = new javax.swing.JMenu();
        renameItem = new javax.swing.JMenuItem();
        duplicateItem = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JSeparator();
        undoItem = new javax.swing.JMenuItem();
        redoItem = new javax.swing.JMenuItem();
        jMenu8 = new javax.swing.JMenu();
        signup = new javax.swing.JMenuItem();
        login = new javax.swing.JMenuItem();
        livePreviewUI = new javax.swing.JCheckBoxMenuItem();
        jMenu6 = new javax.swing.JMenu();
        resetNetbeansSettings = new javax.swing.JMenuItem();
        jMenu1 = new javax.swing.JMenu();
        jMenu10 = new javax.swing.JMenu();
        iosNativeTheme = new javax.swing.JRadioButtonMenuItem();
        ios7NativeTheme = new javax.swing.JCheckBoxMenuItem();
        jMenu3 = new javax.swing.JMenu();
        android2NativeTheme = new javax.swing.JRadioButtonMenuItem();
        jRadioButtonMenuItem4 = new javax.swing.JRadioButtonMenuItem();
        blackberryNativeTheme = new javax.swing.JRadioButtonMenuItem();
        winNativeTheme = new javax.swing.JRadioButtonMenuItem();
        jMenu9 = new javax.swing.JMenu();
        jRadioButtonMenuItem3 = new javax.swing.JRadioButtonMenuItem();
        jRadioButtonMenuItem2 = new javax.swing.JRadioButtonMenuItem();
        jRadioButtonMenuItem1 = new javax.swing.JRadioButtonMenuItem();
        customNativeTheme = new javax.swing.JRadioButtonMenuItem();
        jMenu4 = new javax.swing.JMenu();
        addMultiImages = new javax.swing.JMenuItem();
        quickMultiImages = new javax.swing.JMenuItem();
        addImages = new javax.swing.JMenuItem();
        addSVGImages = new javax.swing.JMenuItem();
        deleteUnusedImages = new javax.swing.JMenuItem();
        imageSizes = new javax.swing.JMenuItem();
        imageBorderWizardMenuItem = new javax.swing.JMenuItem();
        jMenu5 = new javax.swing.JMenu();
        pulsateEffect = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        findMultiImages = new javax.swing.JMenuItem();
        launchOptiPng = new javax.swing.JMenuItem();
        import9Patch = new javax.swing.JMenuItem();
        jMenu7 = new javax.swing.JMenu();
        removeDPIHD = new javax.swing.JMenuItem();
        removeDPIVeryHigh = new javax.swing.JMenuItem();
        removeDPIHigh = new javax.swing.JMenuItem();
        removeDPIMedium = new javax.swing.JMenuItem();
        removeDPILow = new javax.swing.JMenuItem();
        removeDPIVeryLow = new javax.swing.JMenuItem();
        helpMenu = new javax.swing.JMenu();
        jSeparator8 = new javax.swing.JSeparator();
        about = new javax.swing.JMenuItem();
        toolbar = new javax.swing.JToolBar();
        buttonGroup1 = new javax.swing.ButtonGroup();
        svgGroup = new javax.swing.ButtonGroup();
        nativeThemeButtonGroup = new javax.swing.ButtonGroup();

        FormListener formListener = new FormListener();

        mainPanel.setName("mainPanel"); // NOI18N
        mainPanel.setOpaque(false);
        mainPanel.setLayout(new java.awt.BorderLayout());

        jSplitPane1.setBorder(null);
        jSplitPane1.setDividerLocation(200);
        jSplitPane1.setResizeWeight(0.1);
        jSplitPane1.setName("jSplitPane1"); // NOI18N
        jSplitPane1.setOneTouchExpandable(true);
        jSplitPane1.setOpaque(false);

        treeArea.setName("treeArea"); // NOI18N
        treeArea.setOpaque(false);

        jTabbedPane1.setName("jTabbedPane1"); // NOI18N

        themePanel.setName("themePanel"); // NOI18N
        themePanel.setOpaque(false);

        addTheme.setMnemonic('+');
        addTheme.setText("Add A New Theme");
        addTheme.setToolTipText("Add Theme");
        addTheme.setName("addTheme"); // NOI18N
        addTheme.addActionListener(formListener);

        themeScroll.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        themeScroll.setName("themeScroll"); // NOI18N
        themeScroll.setOpaque(false);

        org.jdesktop.layout.GroupLayout themePanelLayout = new org.jdesktop.layout.GroupLayout(themePanel);
        themePanel.setLayout(themePanelLayout);
        themePanelLayout.setHorizontalGroup(
            themePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(themeScroll, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 179, Short.MAX_VALUE)
            .add(themePanelLayout.createSequentialGroup()
                .add(addTheme)
                .addContainerGap(20, Short.MAX_VALUE))
        );
        themePanelLayout.setVerticalGroup(
            themePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(themePanelLayout.createSequentialGroup()
                .add(addTheme)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(themeScroll, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 350, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Themes", new javax.swing.ImageIcon(getClass().getResource("/com/codename1/designer/resources/theme.png")), themePanel, "Themes"); // NOI18N

        jPanel1.setName("jPanel1"); // NOI18N
        jPanel1.setOpaque(false);

        addUserInterface.setText("Add A New GUI Element");
        addUserInterface.setName("addUserInterface"); // NOI18N
        addUserInterface.addActionListener(formListener);

        userInterfaceScroll.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        userInterfaceScroll.setName("userInterfaceScroll"); // NOI18N
        userInterfaceScroll.setOpaque(false);

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(userInterfaceScroll, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 202, Short.MAX_VALUE)
            .add(jPanel1Layout.createSequentialGroup()
                .add(addUserInterface, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 185, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .add(addUserInterface)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(userInterfaceScroll, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 350, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("GUI Builder", new javax.swing.ImageIcon(getClass().getResource("/com/codename1/designer/resources/GUIBuilder.png")), jPanel1, "GUI Builder"); // NOI18N

        jPanel7.setName("jPanel7"); // NOI18N

        mainImages.setName("mainImages"); // NOI18N

        addImageMain.setMnemonic('I');
        addImageMain.setText("Add Images");
        addImageMain.setToolTipText("Add Image");
        addImageMain.setName("addImageMain"); // NOI18N
        addImageMain.addActionListener(formListener);

        org.jdesktop.layout.GroupLayout jPanel7Layout = new org.jdesktop.layout.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel7Layout.createSequentialGroup()
                .add(addImageMain)
                .addContainerGap(61, Short.MAX_VALUE))
            .add(mainImages, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 179, Short.MAX_VALUE)
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel7Layout.createSequentialGroup()
                .add(addImageMain)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(mainImages, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 350, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Main Images", new javax.swing.ImageIcon(getClass().getResource("/com/codename1/designer/resources/images.png")), jPanel7); // NOI18N

        jPanel3.setName("jPanel3"); // NOI18N

        imageScroll.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        imageScroll.setName("imageScroll"); // NOI18N
        imageScroll.setOpaque(false);

        addImageAll.setMnemonic('I');
        addImageAll.setText("Add Images");
        addImageAll.setToolTipText("Add Image");
        addImageAll.setName("addImageAll"); // NOI18N
        addImageAll.addActionListener(formListener);

        org.jdesktop.layout.GroupLayout jPanel3Layout = new org.jdesktop.layout.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel3Layout.createSequentialGroup()
                .add(addImageAll)
                .addContainerGap(61, Short.MAX_VALUE))
            .add(imageScroll, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 179, Short.MAX_VALUE)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel3Layout.createSequentialGroup()
                .add(addImageAll)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(imageScroll, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 350, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("All Images", new javax.swing.ImageIcon(getClass().getResource("/com/codename1/designer/resources/images.png")), jPanel3); // NOI18N

        jPanel11.setName("jPanel11"); // NOI18N

        multiImages.setName("multiImages"); // NOI18N

        addImageMulti.setMnemonic('I');
        addImageMulti.setText("Add Images");
        addImageMulti.setToolTipText("Add Image");
        addImageMulti.setName("addImageMulti"); // NOI18N
        addImageMulti.addActionListener(formListener);

        org.jdesktop.layout.GroupLayout jPanel11Layout = new org.jdesktop.layout.GroupLayout(jPanel11);
        jPanel11.setLayout(jPanel11Layout);
        jPanel11Layout.setHorizontalGroup(
            jPanel11Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel11Layout.createSequentialGroup()
                .add(addImageMulti)
                .addContainerGap())
            .add(multiImages, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 179, Short.MAX_VALUE)
        );
        jPanel11Layout.setVerticalGroup(
            jPanel11Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel11Layout.createSequentialGroup()
                .add(addImageMulti)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(multiImages, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 350, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Multi-Images", new javax.swing.ImageIcon(getClass().getResource("/com/codename1/designer/resources/images.png")), jPanel11); // NOI18N

        jPanel9.setName("jPanel9"); // NOI18N

        svgImages.setName("svgImages"); // NOI18N

        addImageSVG.setMnemonic('I');
        addImageSVG.setText("Add Images");
        addImageSVG.setToolTipText("Add Image");
        addImageSVG.setName("addImageSVG"); // NOI18N
        addImageSVG.addActionListener(formListener);

        org.jdesktop.layout.GroupLayout jPanel9Layout = new org.jdesktop.layout.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(
            jPanel9Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel9Layout.createSequentialGroup()
                .add(addImageSVG)
                .addContainerGap())
            .add(svgImages, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 179, Short.MAX_VALUE)
        );
        jPanel9Layout.setVerticalGroup(
            jPanel9Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel9Layout.createSequentialGroup()
                .add(addImageSVG)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(svgImages, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 350, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("SVG Images", new javax.swing.ImageIcon(getClass().getResource("/com/codename1/designer/resources/images.png")), jPanel9); // NOI18N

        jPanel10.setName("jPanel10"); // NOI18N

        timelineImages.setName("timelineImages"); // NOI18N

        addImageTimeline.setMnemonic('I');
        addImageTimeline.setText("Add Animation");
        addImageTimeline.setToolTipText("Add Image");
        addImageTimeline.setName("addImageTimeline"); // NOI18N
        addImageTimeline.addActionListener(formListener);

        addNewTimeline.setText("Add New");
        addNewTimeline.setName("addNewTimeline"); // NOI18N
        addNewTimeline.addActionListener(formListener);

        org.jdesktop.layout.GroupLayout jPanel10Layout = new org.jdesktop.layout.GroupLayout(jPanel10);
        jPanel10.setLayout(jPanel10Layout);
        jPanel10Layout.setHorizontalGroup(
            jPanel10Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel10Layout.createSequentialGroup()
                .add(addImageTimeline)
                .addContainerGap())
            .add(jPanel10Layout.createSequentialGroup()
                .add(addNewTimeline, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
            .add(timelineImages, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 179, Short.MAX_VALUE)
        );

        jPanel10Layout.linkSize(new java.awt.Component[] {addImageTimeline, addNewTimeline}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        jPanel10Layout.setVerticalGroup(
            jPanel10Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel10Layout.createSequentialGroup()
                .add(addImageTimeline)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(addNewTimeline, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(timelineImages, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 321, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Timeline Images", new javax.swing.ImageIcon(getClass().getResource("/com/codename1/designer/resources/images.png")), jPanel10); // NOI18N

        jPanel4.setName("jPanel4"); // NOI18N
        jPanel4.setOpaque(false);

        addFont.setMnemonic('+');
        addFont.setText("Add Font");
        addFont.setToolTipText("Add Font");
        addFont.setName("addFont"); // NOI18N
        addFont.addActionListener(formListener);

        fontsScroll.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        fontsScroll.setName("fontsScroll"); // NOI18N
        fontsScroll.setOpaque(false);

        org.jdesktop.layout.GroupLayout jPanel4Layout = new org.jdesktop.layout.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(fontsScroll, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 179, Short.MAX_VALUE)
            .add(jPanel4Layout.createSequentialGroup()
                .add(addFont)
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel4Layout.createSequentialGroup()
                .add(addFont)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(fontsScroll, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 350, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Fonts", new javax.swing.ImageIcon(getClass().getResource("/com/codename1/designer/resources/font.png")), jPanel4, "Fonts"); // NOI18N

        jPanel5.setName("jPanel5"); // NOI18N
        jPanel5.setOpaque(false);

        addL10N.setMnemonic('+');
        addL10N.setText("Add Resource Bundle");
        addL10N.setToolTipText("Add Localization");
        addL10N.setName("addL10N"); // NOI18N
        addL10N.addActionListener(formListener);

        localizationScroll.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        localizationScroll.setName("localizationScroll"); // NOI18N
        localizationScroll.setOpaque(false);

        org.jdesktop.layout.GroupLayout jPanel5Layout = new org.jdesktop.layout.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(localizationScroll, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 193, Short.MAX_VALUE)
            .add(jPanel5Layout.createSequentialGroup()
                .add(addL10N)
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel5Layout.createSequentialGroup()
                .add(addL10N)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(localizationScroll, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 350, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Localization", new javax.swing.ImageIcon(getClass().getResource("/com/codename1/designer/resources/localization.png")), jPanel5, "Localization"); // NOI18N

        jPanel6.setName("jPanel6"); // NOI18N
        jPanel6.setOpaque(false);

        addData.setMnemonic('+');
        addData.setText("Add Data File");
        addData.setToolTipText("Add Data");
        addData.setName("addData"); // NOI18N
        addData.addActionListener(formListener);

        dataScroll.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        dataScroll.setName("dataScroll"); // NOI18N
        dataScroll.setOpaque(false);

        org.jdesktop.layout.GroupLayout jPanel6Layout = new org.jdesktop.layout.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(dataScroll, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 179, Short.MAX_VALUE)
            .add(jPanel6Layout.createSequentialGroup()
                .add(addData)
                .addContainerGap())
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel6Layout.createSequentialGroup()
                .add(addData)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(dataScroll, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 350, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Data", new javax.swing.ImageIcon(getClass().getResource("/com/codename1/designer/resources/database.png")), jPanel6, "Data"); // NOI18N

        org.jdesktop.layout.GroupLayout treeAreaLayout = new org.jdesktop.layout.GroupLayout(treeArea);
        treeArea.setLayout(treeAreaLayout);
        treeAreaLayout.setHorizontalGroup(
            treeAreaLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jTabbedPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
        );
        treeAreaLayout.setVerticalGroup(
            treeAreaLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jTabbedPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 434, Short.MAX_VALUE)
        );

        jTabbedPane1.getAccessibleContext().setAccessibleName("Resources");
        jTabbedPane1.getAccessibleContext().setAccessibleDescription("Resources");

        jSplitPane1.setLeftComponent(treeArea);

        resourceEditor.setMinimumSize(new java.awt.Dimension(400, 400));
        resourceEditor.setName("resourceEditor"); // NOI18N
        resourceEditor.setOpaque(false);
        resourceEditor.setLayout(new java.awt.BorderLayout());

        jScrollPane3.setName("jScrollPane3"); // NOI18N

        license.setColumns(20);
        license.setEditable(false);
        license.setFont(new java.awt.Font("Arial", 0, 12));
        license.setLineWrap(true);
        license.setRows(5);
        license.setWrapStyleWord(true);
        license.setName("license"); // NOI18N
        jScrollPane3.setViewportView(license);
        license.getAccessibleContext().setAccessibleName("License");
        license.getAccessibleContext().setAccessibleDescription("License");

        resourceEditor.add(jScrollPane3, java.awt.BorderLayout.CENTER);

        jSeparator3.setName("jSeparator3"); // NOI18N
        resourceEditor.add(jSeparator3, java.awt.BorderLayout.PAGE_START);

        jToolBar1.setRollover(true);
        jToolBar1.setName("jToolBar1"); // NOI18N
        resourceEditor.add(jToolBar1, java.awt.BorderLayout.LINE_END);

        jSplitPane1.setRightComponent(resourceEditor);

        mainPanel.add(jSplitPane1, java.awt.BorderLayout.CENTER);

        menuBar.setName("menuBar"); // NOI18N

        fileMenu.setMnemonic('F');
        fileMenu.setText("File");
        fileMenu.setName("fileMenu"); // NOI18N

        newMenuItem.setAction(newResourceAction);
        newMenuItem.setMnemonic('N');
        newMenuItem.setText("New");
        newMenuItem.setName("newMenuItem"); // NOI18N
        fileMenu.add(newMenuItem);

        openMenuItem.setAction(loadResourceFileAction);
        openMenuItem.setMnemonic('O');
        openMenuItem.setText("Open");
        openMenuItem.setName("openMenuItem"); // NOI18N
        fileMenu.add(openMenuItem);

        recentMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/codename1/designer/resources/recent1.png"))); // NOI18N
        recentMenu.setMnemonic('R');
        recentMenu.setText("Recent");
        recentMenu.setName("recentMenu"); // NOI18N
        fileMenu.add(recentMenu);

        saveMenuItem.setAction(saveResourceFileAction);
        saveMenuItem.setMnemonic('S');
        saveMenuItem.setText("Save");
        saveMenuItem.setName("saveMenuItem"); // NOI18N
        fileMenu.add(saveMenuItem);

        saveAsMenuItem.setAction(saveResourceFileAsAction);
        saveAsMenuItem.setMnemonic('A');
        saveAsMenuItem.setText("Save As...");
        saveAsMenuItem.setName("saveAsMenuItem"); // NOI18N
        fileMenu.add(saveAsMenuItem);

        resPassword.setText("Set Password");
        resPassword.setName("resPassword"); // NOI18N
        resPassword.addActionListener(formListener);
        fileMenu.add(resPassword);

        importRes.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_I, java.awt.event.InputEvent.SHIFT_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        importRes.setMnemonic('I');
        importRes.setText("Import");
        importRes.setName("importRes"); // NOI18N
        importRes.addActionListener(formListener);
        fileMenu.add(importRes);

        exportRes.setAction(exportResourceFileAction);
        exportRes.setMnemonic('E');
        exportRes.setText("Export...");
        exportRes.setName("exportRes"); // NOI18N
        fileMenu.add(exportRes);

        setupNetbeans.setText("Setup Netbeans");
        setupNetbeans.setName("setupNetbeans"); // NOI18N
        setupNetbeans.addActionListener(formListener);
        fileMenu.add(setupNetbeans);

        lookAndFeelMenu.setMnemonic('L');
        lookAndFeelMenu.setText("Look & Feel");
        lookAndFeelMenu.setName("lookAndFeelMenu"); // NOI18N

        buttonGroup1.add(systemLFMenu);
        systemLFMenu.setMnemonic('S');
        systemLFMenu.setText("System");
        systemLFMenu.setName("systemLFMenu"); // NOI18N
        systemLFMenu.addActionListener(formListener);
        lookAndFeelMenu.add(systemLFMenu);

        buttonGroup1.add(crossPlatformLFMenu);
        crossPlatformLFMenu.setMnemonic('C');
        crossPlatformLFMenu.setText("Cross Platform");
        crossPlatformLFMenu.setName("crossPlatformLFMenu"); // NOI18N
        crossPlatformLFMenu.addActionListener(formListener);
        lookAndFeelMenu.add(crossPlatformLFMenu);

        jSeparator6.setName("jSeparator6"); // NOI18N
        lookAndFeelMenu.add(jSeparator6);

        checkerboardColors.setText("Checkerboard Color");
        checkerboardColors.setName("checkerboardColors"); // NOI18N
        checkerboardColors.addActionListener(formListener);
        lookAndFeelMenu.add(checkerboardColors);

        jSeparator4.setName("jSeparator4"); // NOI18N
        lookAndFeelMenu.add(jSeparator4);

        fileMenu.add(lookAndFeelMenu);

        enableXMLTeamMode.setText("XML Team Mode");
        enableXMLTeamMode.setName("enableXMLTeamMode"); // NOI18N
        enableXMLTeamMode.addActionListener(formListener);
        fileMenu.add(enableXMLTeamMode);

        jSeparator1.setName("jSeparator1"); // NOI18N
        fileMenu.add(jSeparator1);

        exitMenuItem.setAction(QuitAction.INSTANCE);
        exitMenuItem.setMnemonic('X');
        exitMenuItem.setText("Exit");
        exitMenuItem.setName("exitMenuItem"); // NOI18N
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        editMenu.setMnemonic('E');
        editMenu.setText("Edit");
        editMenu.setName("editMenu"); // NOI18N

        renameItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        renameItem.setMnemonic('R');
        renameItem.setText("Rename");
        renameItem.setName("renameItem"); // NOI18N
        renameItem.addActionListener(formListener);
        editMenu.add(renameItem);

        duplicateItem.setText("Duplicate");
        duplicateItem.setName("duplicateItem"); // NOI18N
        duplicateItem.addActionListener(formListener);
        editMenu.add(duplicateItem);

        jSeparator2.setName("jSeparator2"); // NOI18N
        editMenu.add(jSeparator2);

        undoItem.setAction(undoAction);
        undoItem.setMnemonic('U');
        undoItem.setText("Undo");
        undoItem.setName("undoItem"); // NOI18N
        editMenu.add(undoItem);

        redoItem.setAction(redoAction);
        redoItem.setMnemonic('E');
        redoItem.setText("Redo");
        redoItem.setName("redoItem"); // NOI18N
        editMenu.add(redoItem);

        menuBar.add(editMenu);

        jMenu8.setText("Codename One");
        jMenu8.setName("jMenu8"); // NOI18N

        signup.setText("Signup");
        signup.setName("signup"); // NOI18N
        signup.addActionListener(formListener);
        jMenu8.add(signup);

        login.setText("Login");
        login.setName("login"); // NOI18N
        login.addActionListener(formListener);
        jMenu8.add(login);

        livePreviewUI.setText("Live Preview");
        livePreviewUI.setName("livePreviewUI"); // NOI18N
        livePreviewUI.addActionListener(formListener);
        jMenu8.add(livePreviewUI);

        jMenu6.setText("Advanced");
        jMenu6.setName("jMenu6"); // NOI18N

        resetNetbeansSettings.setText("Reset Netbeans Settings");
        resetNetbeansSettings.setName("resetNetbeansSettings"); // NOI18N
        resetNetbeansSettings.addActionListener(formListener);
        jMenu6.add(resetNetbeansSettings);

        jMenu8.add(jMenu6);

        menuBar.add(jMenu8);

        jMenu1.setText("Native Theme");
        jMenu1.setName("jMenu1"); // NOI18N

        jMenu10.setText("iOS");
        jMenu10.setName("jMenu10"); // NOI18N

        nativeThemeButtonGroup.add(iosNativeTheme);
        iosNativeTheme.setSelected(true);
        iosNativeTheme.setText("iOS 6");
        iosNativeTheme.setName("iosNativeTheme"); // NOI18N
        iosNativeTheme.addActionListener(formListener);
        jMenu10.add(iosNativeTheme);

        nativeThemeButtonGroup.add(ios7NativeTheme);
        ios7NativeTheme.setText("iOS 7");
        ios7NativeTheme.setName("ios7NativeTheme"); // NOI18N
        ios7NativeTheme.addActionListener(formListener);
        jMenu10.add(ios7NativeTheme);

        jMenu1.add(jMenu10);

        jMenu3.setText("Android");

        nativeThemeButtonGroup.add(android2NativeTheme);
        android2NativeTheme.setText("Gingerbread (2.3.x)");
        android2NativeTheme.setName("android2NativeTheme"); // NOI18N
        android2NativeTheme.addActionListener(formListener);
        jMenu3.add(android2NativeTheme);

        nativeThemeButtonGroup.add(jRadioButtonMenuItem4);
        jRadioButtonMenuItem4.setText("ICS (3.x or newer/Tablets)");
        jRadioButtonMenuItem4.setName("jRadioButtonMenuItem4"); // NOI18N
        jRadioButtonMenuItem4.addActionListener(formListener);
        jMenu3.add(jRadioButtonMenuItem4);

        jMenu1.add(jMenu3);

        nativeThemeButtonGroup.add(blackberryNativeTheme);
        blackberryNativeTheme.setText("Blackberry Theme");
        blackberryNativeTheme.setName("blackberryNativeTheme"); // NOI18N
        blackberryNativeTheme.addActionListener(formListener);
        jMenu1.add(blackberryNativeTheme);

        nativeThemeButtonGroup.add(winNativeTheme);
        winNativeTheme.setText("Windows Phone Theme");
        winNativeTheme.setName("winNativeTheme"); // NOI18N
        winNativeTheme.addActionListener(formListener);
        jMenu1.add(winNativeTheme);

        jMenu9.setText("J2ME - Nokia");

        nativeThemeButtonGroup.add(jRadioButtonMenuItem3);
        jRadioButtonMenuItem3.setText("Nokia Asha");
        jRadioButtonMenuItem3.setName("jRadioButtonMenuItem3"); // NOI18N
        jRadioButtonMenuItem3.addActionListener(formListener);
        jMenu9.add(jRadioButtonMenuItem3);

        nativeThemeButtonGroup.add(jRadioButtonMenuItem2);
        jRadioButtonMenuItem2.setText("Nokia Touch");
        jRadioButtonMenuItem2.setName("jRadioButtonMenuItem2"); // NOI18N
        jRadioButtonMenuItem2.addActionListener(formListener);
        jMenu9.add(jRadioButtonMenuItem2);

        nativeThemeButtonGroup.add(jRadioButtonMenuItem1);
        jRadioButtonMenuItem1.setText("Nokia None Touch");
        jRadioButtonMenuItem1.setName("jRadioButtonMenuItem1"); // NOI18N
        jRadioButtonMenuItem1.addActionListener(formListener);
        jMenu9.add(jRadioButtonMenuItem1);

        jMenu1.add(jMenu9);

        nativeThemeButtonGroup.add(customNativeTheme);
        customNativeTheme.setText("Custom Theme");
        customNativeTheme.setName("customNativeTheme"); // NOI18N
        customNativeTheme.addActionListener(formListener);
        jMenu1.add(customNativeTheme);

        menuBar.add(jMenu1);

        jMenu4.setText("Images");
        jMenu4.setName("jMenu4"); // NOI18N

        addMultiImages.setText("Add Multi Images");
        addMultiImages.setName("addMultiImages"); // NOI18N
        addMultiImages.addActionListener(formListener);
        jMenu4.add(addMultiImages);

        quickMultiImages.setText("Quick Add Multi Images");
        quickMultiImages.setName("quickMultiImages"); // NOI18N
        quickMultiImages.addActionListener(formListener);
        jMenu4.add(quickMultiImages);

        addImages.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ADD, java.awt.event.InputEvent.CTRL_MASK));
        addImages.setMnemonic('A');
        addImages.setText("Add Images");
        addImages.setName("addImages"); // NOI18N
        addImages.addActionListener(formListener);
        jMenu4.add(addImages);

        addSVGImages.setText("Add SVG Images");
        addSVGImages.setName("addSVGImages"); // NOI18N
        addSVGImages.addActionListener(formListener);
        jMenu4.add(addSVGImages);

        deleteUnusedImages.setText("Delete Unused Images");
        deleteUnusedImages.setName("deleteUnusedImages"); // NOI18N
        deleteUnusedImages.addActionListener(formListener);
        jMenu4.add(deleteUnusedImages);

        imageSizes.setText("Image Sizes (KB)");
        imageSizes.setName("imageSizes"); // NOI18N
        imageSizes.addActionListener(formListener);
        jMenu4.add(imageSizes);

        imageBorderWizardMenuItem.setText("Image Border Wizard");
        imageBorderWizardMenuItem.setName("imageBorderWizardMenuItem"); // NOI18N
        imageBorderWizardMenuItem.addActionListener(formListener);
        jMenu4.add(imageBorderWizardMenuItem);

        jMenu5.setText("Effects");
        jMenu5.setName("jMenu5"); // NOI18N

        pulsateEffect.setText("Pulsate");
        pulsateEffect.setName("pulsateEffect"); // NOI18N
        pulsateEffect.addActionListener(formListener);
        jMenu5.add(pulsateEffect);

        jMenu4.add(jMenu5);

        jMenu2.setText("Advanced");
        jMenu2.setName("jMenu2"); // NOI18N

        findMultiImages.setText("Find Multi Images");
        findMultiImages.setName("findMultiImages"); // NOI18N
        findMultiImages.addActionListener(formListener);
        jMenu2.add(findMultiImages);

        launchOptiPng.setText("Launch OptiPng");
        launchOptiPng.setName("launchOptiPng"); // NOI18N
        launchOptiPng.addActionListener(formListener);
        jMenu2.add(launchOptiPng);

        import9Patch.setText("Import 9-Patch");
        import9Patch.setName("import9Patch"); // NOI18N
        import9Patch.addActionListener(formListener);
        jMenu2.add(import9Patch);

        jMenu7.setText("Remove DPI");
        jMenu7.setName("jMenu7"); // NOI18N

        removeDPIHD.setText("HD");
        removeDPIHD.setName("removeDPIHD"); // NOI18N
        removeDPIHD.addActionListener(formListener);
        jMenu7.add(removeDPIHD);

        removeDPIVeryHigh.setText("Very High DPI");
        removeDPIVeryHigh.setName("removeDPIVeryHigh"); // NOI18N
        removeDPIVeryHigh.addActionListener(formListener);
        jMenu7.add(removeDPIVeryHigh);

        removeDPIHigh.setText("High");
        removeDPIHigh.setName("removeDPIHigh"); // NOI18N
        removeDPIHigh.addActionListener(formListener);
        jMenu7.add(removeDPIHigh);

        removeDPIMedium.setText("Medium");
        removeDPIMedium.setName("removeDPIMedium"); // NOI18N
        removeDPIMedium.addActionListener(formListener);
        jMenu7.add(removeDPIMedium);

        removeDPILow.setText("Low");
        removeDPILow.setName("removeDPILow"); // NOI18N
        removeDPILow.addActionListener(formListener);
        jMenu7.add(removeDPILow);

        removeDPIVeryLow.setText("Very Low");
        removeDPIVeryLow.setName("removeDPIVeryLow"); // NOI18N
        removeDPIVeryLow.addActionListener(formListener);
        jMenu7.add(removeDPIVeryLow);

        jMenu2.add(jMenu7);

        jMenu4.add(jMenu2);

        menuBar.add(jMenu4);

        helpMenu.setMnemonic('H');
        helpMenu.setText("Help");
        helpMenu.setName("helpMenu"); // NOI18N

        jSeparator8.setName("jSeparator8"); // NOI18N
        helpMenu.add(jSeparator8);

        about.setText("About");
        about.setName("about"); // NOI18N
        about.addActionListener(formListener);
        helpMenu.add(about);

        menuBar.add(helpMenu);

        toolbar.setRollover(true);
        toolbar.setName("toolbar"); // NOI18N

        setComponent(mainPanel);
        setMenuBar(menuBar);
        setToolBar(toolbar);
    }

    // Code for dispatching events from components to event handlers.

    private class FormListener implements java.awt.event.ActionListener {
        FormListener() {}
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            if (evt.getSource() == addTheme) {
                ResourceEditorView.this.addThemeActionPerformed(evt);
            }
            else if (evt.getSource() == addUserInterface) {
                ResourceEditorView.this.addUserInterfaceActionPerformed(evt);
            }
            else if (evt.getSource() == addImageMain) {
                ResourceEditorView.this.addImageMainActionPerformed(evt);
            }
            else if (evt.getSource() == addImageAll) {
                ResourceEditorView.this.addImageAllActionPerformed(evt);
            }
            else if (evt.getSource() == addImageMulti) {
                ResourceEditorView.this.addImageMultiActionPerformed(evt);
            }
            else if (evt.getSource() == addImageSVG) {
                ResourceEditorView.this.addImageSVGActionPerformed(evt);
            }
            else if (evt.getSource() == addImageTimeline) {
                ResourceEditorView.this.addImageTimelineActionPerformed(evt);
            }
            else if (evt.getSource() == addNewTimeline) {
                ResourceEditorView.this.addNewTimelineActionPerformed(evt);
            }
            else if (evt.getSource() == addFont) {
                ResourceEditorView.this.addFontActionPerformed(evt);
            }
            else if (evt.getSource() == addL10N) {
                ResourceEditorView.this.addL10NActionPerformed(evt);
            }
            else if (evt.getSource() == addData) {
                ResourceEditorView.this.addDataActionPerformed(evt);
            }
            else if (evt.getSource() == resPassword) {
                ResourceEditorView.this.resPasswordActionPerformed(evt);
            }
            else if (evt.getSource() == importRes) {
                ResourceEditorView.this.importResActionPerformed(evt);
            }
            else if (evt.getSource() == setupNetbeans) {
                ResourceEditorView.this.setupNetbeansActionPerformed(evt);
            }
            else if (evt.getSource() == systemLFMenu) {
                ResourceEditorView.this.systemLFMenuActionPerformed(evt);
            }
            else if (evt.getSource() == crossPlatformLFMenu) {
                ResourceEditorView.this.crossPlatformLFMenuActionPerformed(evt);
            }
            else if (evt.getSource() == checkerboardColors) {
                ResourceEditorView.this.checkerboardColorsActionPerformed(evt);
            }
            else if (evt.getSource() == enableXMLTeamMode) {
                ResourceEditorView.this.enableXMLTeamModeActionPerformed(evt);
            }
            else if (evt.getSource() == renameItem) {
                ResourceEditorView.this.renameItemActionPerformed(evt);
            }
            else if (evt.getSource() == duplicateItem) {
                ResourceEditorView.this.duplicateItemActionPerformed(evt);
            }
            else if (evt.getSource() == signup) {
                ResourceEditorView.this.signupActionPerformed(evt);
            }
            else if (evt.getSource() == login) {
                ResourceEditorView.this.loginActionPerformed(evt);
            }
            else if (evt.getSource() == livePreviewUI) {
                ResourceEditorView.this.livePreviewUIActionPerformed(evt);
            }
            else if (evt.getSource() == resetNetbeansSettings) {
                ResourceEditorView.this.resetNetbeansSettingsActionPerformed(evt);
            }
            else if (evt.getSource() == iosNativeTheme) {
                ResourceEditorView.this.iosNativeThemeActionPerformed(evt);
            }
            else if (evt.getSource() == ios7NativeTheme) {
                ResourceEditorView.this.ios7NativeThemeActionPerformed(evt);
            }
            else if (evt.getSource() == android2NativeTheme) {
                ResourceEditorView.this.android2NativeThemeActionPerformed(evt);
            }
            else if (evt.getSource() == jRadioButtonMenuItem4) {
                ResourceEditorView.this.jRadioButtonMenuItem4ActionPerformed(evt);
            }
            else if (evt.getSource() == blackberryNativeTheme) {
                ResourceEditorView.this.blackberryNativeThemeActionPerformed(evt);
            }
            else if (evt.getSource() == winNativeTheme) {
                ResourceEditorView.this.winNativeThemeActionPerformed(evt);
            }
            else if (evt.getSource() == jRadioButtonMenuItem3) {
                ResourceEditorView.this.jRadioButtonMenuItem3ActionPerformed(evt);
            }
            else if (evt.getSource() == jRadioButtonMenuItem2) {
                ResourceEditorView.this.jRadioButtonMenuItem2ActionPerformed(evt);
            }
            else if (evt.getSource() == jRadioButtonMenuItem1) {
                ResourceEditorView.this.jRadioButtonMenuItem1ActionPerformed(evt);
            }
            else if (evt.getSource() == customNativeTheme) {
                ResourceEditorView.this.customNativeThemeActionPerformed(evt);
            }
            else if (evt.getSource() == addMultiImages) {
                ResourceEditorView.this.addMultiImagesActionPerformed(evt);
            }
            else if (evt.getSource() == quickMultiImages) {
                ResourceEditorView.this.quickMultiImagesActionPerformed(evt);
            }
            else if (evt.getSource() == addImages) {
                ResourceEditorView.this.addImagesActionPerformed(evt);
            }
            else if (evt.getSource() == addSVGImages) {
                ResourceEditorView.this.addSVGImagesActionPerformed(evt);
            }
            else if (evt.getSource() == deleteUnusedImages) {
                ResourceEditorView.this.deleteUnusedImagesActionPerformed(evt);
            }
            else if (evt.getSource() == imageSizes) {
                ResourceEditorView.this.imageSizesActionPerformed(evt);
            }
            else if (evt.getSource() == imageBorderWizardMenuItem) {
                ResourceEditorView.this.imageBorderWizardMenuItemActionPerformed(evt);
            }
            else if (evt.getSource() == pulsateEffect) {
                ResourceEditorView.this.pulsateEffectActionPerformed(evt);
            }
            else if (evt.getSource() == findMultiImages) {
                ResourceEditorView.this.findMultiImagesActionPerformed(evt);
            }
            else if (evt.getSource() == launchOptiPng) {
                ResourceEditorView.this.launchOptiPngActionPerformed(evt);
            }
            else if (evt.getSource() == import9Patch) {
                ResourceEditorView.this.import9PatchActionPerformed(evt);
            }
            else if (evt.getSource() == removeDPIHD) {
                ResourceEditorView.this.removeDPIHDActionPerformed(evt);
            }
            else if (evt.getSource() == removeDPIVeryHigh) {
                ResourceEditorView.this.removeDPIVeryHighActionPerformed(evt);
            }
            else if (evt.getSource() == removeDPIHigh) {
                ResourceEditorView.this.removeDPIHighActionPerformed(evt);
            }
            else if (evt.getSource() == removeDPIMedium) {
                ResourceEditorView.this.removeDPIMediumActionPerformed(evt);
            }
            else if (evt.getSource() == removeDPILow) {
                ResourceEditorView.this.removeDPILowActionPerformed(evt);
            }
            else if (evt.getSource() == removeDPIVeryLow) {
                ResourceEditorView.this.removeDPIVeryLowActionPerformed(evt);
            }
            else if (evt.getSource() == about) {
                ResourceEditorView.this.aboutActionPerformed(evt);
            }
        }
    }// </editor-fold>//GEN-END:initComponents

    public EditableResources getLoadedResources() {
        return loadedResources;
    }
    
private void addThemeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addThemeActionPerformed
    showAddThemeResourceDialog();
}//GEN-LAST:event_addThemeActionPerformed

private void addFontActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addFontActionPerformed
    showAddResourceDialog(AddResourceDialog.FONT);
}//GEN-LAST:event_addFontActionPerformed

private void addL10NActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addL10NActionPerformed
    showAddResourceDialog(AddResourceDialog.LOCALIZATION);
}//GEN-LAST:event_addL10NActionPerformed

private void addDataActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addDataActionPerformed
    //showAddResourceDialog(AddResourceDialog.DATA);
    DataEditor dataEditor = new DataEditor(loadedResources, "Data");
    dataEditor.selectDataFile(this);    
}//GEN-LAST:event_addDataActionPerformed

    private void removeSelection(String s) {
        // remove the resource
        //if(JOptionPane.showConfirmDialog(mainPanel, "Are you sure you want to remove " + s + "?", "Are You Sure?",
        //        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
            loadedResources.remove(s);

            // remove the resource editor and update the view
            resourceEditor.removeAll();
            resourceEditor.revalidate();
            resourceEditor.repaint();
            if(selectedResource == s) {
                setSelectedResource(null);
            }
        //}
    }

    private void removeImageOrAnimation(String resourceToRemove) {
        Object resourceValue = loadedResources.getImage(resourceToRemove);
        if(!loadedResources.isOverrideMode()) {
            for(String themeName : loadedResources.getThemeResourceNames()) {
                Hashtable theme = loadedResources.getTheme(themeName);
                if(theme.values().contains(resourceValue)) {
                    JOptionPane.showMessageDialog(mainPanel, "Image is in use by the theme" + 
                        "\nYou must remove it from the theme first", "Image In Use", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                // we need to check the existance of image borders to replace images there...
                for(Object v : theme.values()) {
                    if(v instanceof Border) {
                        Border b = (Border)v;
                        // BORDER_TYPE_IMAGE
                        if(Accessor.getType(b) == 8) {
                            com.codename1.ui.Image[] images = Accessor.getImages(b);
                            for(int i = 0 ; i < images.length ; i++) {
                                if(images[i] == resourceValue) {
                                    JOptionPane.showMessageDialog(mainPanel, "Image is in use by the theme in a border" +
                                        "\nYou must remove it from the theme first", "Image In Use", JOptionPane.ERROR_MESSAGE);
                                    return;
                                }
                            }
                        }
                    }
                }
            }

            // check if a timeline is making use of said image and replace it
            for(String image : loadedResources.getImageResourceNames()) {
                com.codename1.ui.Image current = loadedResources.getImage(image);
                if(current instanceof com.codename1.ui.animations.Timeline) {
                    com.codename1.ui.animations.Timeline time = (com.codename1.ui.animations.Timeline)current;
                    for(int iter = 0 ; iter < time.getAnimationCount() ; iter++) {
                        com.codename1.ui.animations.AnimationObject o = time.getAnimation(iter);
                        if(AnimationAccessor.getImage(o) == resourceValue) {
                            JOptionPane.showMessageDialog(mainPanel, "Image is in use by a timeline: " + image, "Image In Use", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    }
                }
            }
            if(isInUse(loadedResources.getImage(resourceToRemove))) {
                JOptionPane.showMessageDialog(mainPanel, "Image is in use in the resource file", "Image In Use", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        removeSelection(resourceToRemove);
        imageList.refresh();
        imageListMain.refresh();
        imageListSVG.refresh();
        imageListMulti.refresh();
        imageListTimeline.refresh();
    }

    public void genericRemoveElement(String element) {
        Object value = loadedResources.getResourceObject(element);
        if(value instanceof com.codename1.ui.Image || value instanceof EditableResources.MultiImage) {
            removeImageOrAnimation(element);
            return;
        } 
        if(value instanceof com.codename1.ui.Font) {
            removeFont(element);
            return;
        }
        if(Arrays.asList(loadedResources.getUIResourceNames()).contains(element)) {
            String usedBy = getUiResourceInUse(element);
            if(usedBy != null) {
                JOptionPane.showMessageDialog(mainPanel, element + " is used by " + usedBy, "Resource In Use", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        removeSelection(element);
        themeList.refresh();
        dataList.refresh();
        l10nList.refresh();
        uiList.refresh();
    }

    private void removeFont(String f) {
        Object resourceValue = loadedResources.getFont(f);
        for(String themeName : loadedResources.getThemeResourceNames()) {
            Hashtable theme = loadedResources.getTheme(themeName);
            if(theme.values().contains(resourceValue)) {
                JOptionPane.showMessageDialog(mainPanel, "Font is in use by the theme: " + 
                    "\nYou must remove it from the theme first", "Font In Use", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        removeSelection(f);
        fontList.refresh();
    }
    
private void renameItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_renameItemActionPerformed
    if(selectedResource != null && loadedResources.containsResource(selectedResource)) {
        if(Arrays.asList(loadedResources.getUIResourceNames()).contains(selectedResource)) {
            String usedBy = getUiResourceInUse(selectedResource);
            if(usedBy != null) {
                JOptionPane.showMessageDialog(mainPanel, selectedResource + " is used by " + usedBy, "Resource In Use", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        Box rename = new Box(BoxLayout.X_AXIS);
        rename.add(new JLabel("New Name: "));
        JTextField field = new JTextField(selectedResource, 20);
        rename.add(Box.createHorizontalStrut(3));
        rename.add(field);
        int result = JOptionPane.showConfirmDialog(mainPanel, rename, "Rename", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if(result == JOptionPane.OK_OPTION) {
            String val = field.getText();
            if(!val.equals(selectedResource)) {
                if(loadedResources.containsResource(val)) {
                    JOptionPane.showMessageDialog(mainPanel, "An Element By This Name Already Exists", "Rename", JOptionPane.ERROR_MESSAGE);
                    renameItemActionPerformed(evt);
                    return;
                }
                loadedResources.renameEntry(selectedResource, val);
                setSelectedResource(val);
            }
        }
    } else {
        JOptionPane.showMessageDialog(mainPanel, "An Element Must Be Selected", "Rename", JOptionPane.ERROR_MESSAGE);
    }
}//GEN-LAST:event_renameItemActionPerformed

private void addImagesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addImagesActionPerformed
    new ImageRGBEditor(loadedResources, null, this).selectFiles();
}//GEN-LAST:event_addImagesActionPerformed

private void systemLFMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_systemLFMenuActionPerformed
        String plaf = UIManager.getSystemLookAndFeelClassName();
        updatePLAF(plaf);    
}//GEN-LAST:event_systemLFMenuActionPerformed

    private void updatePLAF(String plaf) {
        try {
            Preferences.userNodeForPackage(ResourceEditorView.class).put("plaf", plaf);
            UIManager.setLookAndFeel(plaf);
            SwingUtilities.updateComponentTreeUI(SwingUtilities.windowForComponent(mainPanel));
        } catch(Exception e) {
            e.printStackTrace();
        }        
    }

private void crossPlatformLFMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_crossPlatformLFMenuActionPerformed
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    updatePLAF(info.getClassName());
                    return;
                }
            }
        } catch (Exception e) {
            // If Nimbus is not available, you can set the GUI to another look and feel.
        }
        String plaf = UIManager.getCrossPlatformLookAndFeelClassName();
        updatePLAF(plaf);    
}//GEN-LAST:event_crossPlatformLFMenuActionPerformed

private void addUserInterfaceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addUserInterfaceActionPerformed
    showAddUiResourceDialog();
}//GEN-LAST:event_addUserInterfaceActionPerformed

private void checkDuplicateResourcesLoop(EditableResources r, String[] loadedResourcesArray, String[] rArray, String dialogTitle, String resourceTypeName) {
    checkDuplicateResourcesLoop(r, loadedResourcesArray, rArray, dialogTitle, resourceTypeName, true, mainPanel);
}

public void checkDuplicateResourcesLoop(EditableResources r, String[] loadedResourcesArray, String[] rArray, String dialogTitle, String resourceTypeName, boolean forceRename) {
    checkDuplicateResourcesLoop(r, loadedResourcesArray, rArray, dialogTitle, resourceTypeName, forceRename, mainPanel);
}

public static void checkDuplicateResourcesLoop(EditableResources r, String[] loadedResourcesArray, String[] rArray, String dialogTitle, String resourceTypeName, boolean forceRename, java.awt.Component cmp) {
    for(String e : rArray) {
        checkDuplicateResources(r, loadedResourcesArray, rArray, dialogTitle, resourceTypeName, e, forceRename, cmp);
    }
}

private static void checkDuplicateResources(EditableResources r, String[] loadedResourcesArray, String[] rArray, String dialogTitle, String resourceTypeName, String entryName, boolean forceRename, java.awt.Component cmp) {
    if(hasStringInArray(loadedResourcesArray, entryName)) {
        if(!forceRename) {
            int val = JOptionPane.showConfirmDialog(cmp, resourceTypeName + entryName + " already defined in resources.\nDo you want to \"auto rename\"?", dialogTitle,
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if(val != JOptionPane.YES_OPTION) {
                return;
            }
        }
        int index = 1;
        while(hasStringInArray(loadedResourcesArray, entryName + index) ||
                hasStringInArray(rArray, entryName + index)) {
            index++;
        }
        r.renameEntry(entryName, entryName + index);
    }

}

    public void importResourceStream(InputStream is) throws IOException {
        EditableResources r = new EditableResources();
        r.openFile(is);
        checkDuplicateResourcesLoop(r, loadedResources.getThemeResourceNames(),
                r.getThemeResourceNames(), "Rename Theme", "Theme ");

        // load all the themes so rename will work properly on images and won't conflict
        for(String t : r.getThemeResourceNames()) {
            r.getTheme(t);
        }

        checkDuplicateResourcesLoop(r, loadedResources.getImageResourceNames(),
                r.getImageResourceNames(), "Rename Image", "Image ");
        checkDuplicateResourcesLoop(r, loadedResources.getL10NResourceNames(),
                r.getL10NResourceNames(), "Rename Localization", "Localization ");
        checkDuplicateResourcesLoop(r, loadedResources.getDataResourceNames(),
                r.getDataResourceNames(), "Rename Data", "Data ");
        checkDuplicateResourcesLoop(r, loadedResources.getUIResourceNames(),
                r.getUIResourceNames(), "Rename GUI", "GUI ");
        checkDuplicateResourcesLoop(r, loadedResources.getFontResourceNames(),
                r.getFontResourceNames(), "Rename Font", "Font ");

        for (String s : r.getImageResourceNames()) {
            if(r.isMultiImage(s)) {
                loadedResources.setMultiImage(s, (EditableResources.MultiImage)r.getResourceObject(s));
            } else {
                loadedResources.setImage(s, r.getImage(s));
            }
        }
        for (String s : r.getL10NResourceNames()) {
            loadedResources.setL10N(s, (Hashtable)r.getResourceObject(s));
        }
        for (String s : r.getDataResourceNames()) {
            loadedResources.setData(s, (byte[])r.getResourceObject(s));
        }
        for (String s : r.getUIResourceNames()) {
            loadedResources.setUi(s, (byte[])r.getResourceObject(s));
        }
        for (String s : r.getFontResourceNames()) {
            loadedResources.setFont(s, r.getFont(s));
        }
        for (String s : r.getThemeResourceNames()) {
            loadedResources.setTheme(s, r.getTheme(s));
        }

    }

private void importResActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importResActionPerformed
    if(loadedResources != null) {
        File[] files = showOpenFileChooser();
        if(files != null) {
                InputStream is = null;
                try {
                    File selection = files[0];
                    is = new FileInputStream(selection);
                    importResourceStream(is);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(mainPanel, "Error importing file", "IO Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    try {
                        is.close();
                    } catch (IOException ex) {}
                }
        }
    }
}//GEN-LAST:event_importResActionPerformed

    private static void removeCommandDups(final Map<String, Integer> commandMap, int commandId) {
        for(String currentKey : commandMap.keySet()) {
            int c = commandMap.get(currentKey);
            if(c == commandId) {
                commandMap.remove(currentKey);

                // this prevents a concurrent modification exception by restarting the loop
                removeCommandDups(commandMap, commandId);
                return;
            }
        }
    }

    private static void initCommandMapAndNameToClassLookup(final Map<String, String> nameToClassLookup,
        final Map<String, Integer> commandMap, final List<Integer> unhandledCommands,
        final List<String[]> actionComponentNames, final Map<String, String> allComponents) {
        // register the proper handlers for the component types used
        UIBuilderOverride.registerCustom();
        PickMIDlet.getCustomComponents();
        for(String currentResourceName : loadedResources.getUIResourceNames()) {
            final String currentName = currentResourceName;
            UIBuilder b = new UIBuilder() {
                protected com.codename1.ui.Command createCommand(String commandName, com.codename1.ui.Image icon, int commandId, String action) {
                    if(unhandledCommands != null) {
                        if(action == null) {
                            unhandledCommands.add(commandId);
                        }
                    }

                    // we already have that command id...
                    if(commandMap.values().contains(commandId)) {
                        removeCommandDups(commandMap, commandId);
                    }
                    if(commandName == null || commandName.length() == 0) {
                        commandName = "Command" + commandId;
                    }
                    commandName = normalizeFormName(currentName) +
                                normalizeFormName(commandName);
                    commandMap.put(commandName, commandId);
                    return super.createCommand(commandName, icon, commandId, action);
                }
                public boolean caseInsensitiveContainsKey(String s) {
                    return caseInsensitiveKey(s) != null;
                }
                public String caseInsensitiveKey(String s) {
                    for(String k : allComponents.keySet()) {
                        if(k.equalsIgnoreCase(s)) {
                            return k;
                        }
                    }
                    return null;
                }
                public void postCreateComponent(com.codename1.ui.Component cmp) {
                    if(allComponents != null) {
                        String name = cmp.getName();
                        String componentClass = cmp.getClass().getName();
                        if(allComponents.containsKey(name)) {
                            if(!componentClass.equals(allComponents.get(name))) {
                                allComponents.put(name, "com.codename1.ui.Component");
                            } else {
                                allComponents.put(name, componentClass);
                            }
                        } else {
                            if(!caseInsensitiveContainsKey(name)) {
                                allComponents.put(name, componentClass);
                            }
                        }
                    }
                    com.codename1.ui.Component actual = cmp;
                    if(cmp instanceof com.codename1.ui.Container) {
                        actual = ((com.codename1.ui.Container)cmp).getLeadComponent();
                        if(actual == null) {
                            actual = cmp;
                        }
                    }
                    if(actionComponentNames != null &&
                            (actual instanceof com.codename1.ui.Button || actual instanceof com.codename1.ui.List ||
                            actual instanceof com.codename1.ui.list.ContainerList || actual instanceof com.codename1.ui.TextArea ||
                            actual instanceof com.codename1.ui.Calendar)) {
                        if(actual instanceof com.codename1.ui.Button) {
                            if(((com.codename1.ui.Button)actual).getCommand() != null) {
                                return;
                            }
                        }
                        String componentName = cmp.getName();
                        for(String[] arr : actionComponentNames) {
                            if(arr[0].equals(componentName) && arr[1].equals(currentName)) {
                                return;
                            }
                        }
                        actionComponentNames.add(new String[] {componentName, currentName});
                    }
                }
                protected com.codename1.ui.Component createComponentInstance(String componentType, Class cls) {
                    if(cls.getName().startsWith("com.codename1.ui.")) {
                        // subpackage of CodenameOne should be registered
                        if(cls.getName().lastIndexOf(".") > 15) {
                            nameToClassLookup.put(componentType, cls.getName());
                        }
                    } else {
                        nameToClassLookup.put(componentType, cls.getName());
                    }
                    return null;
                }
            };
            b.createContainer(loadedResources, currentResourceName);
        }
    }

    private File pickJavaSourceLocation() {
       File[] files = showOpenFileChooser("Java Source File", ".java");
       if(files == null) {
           return null;
       }
       File destFile = files[0];
       if(!destFile.getName().contains(".")) {
           destFile = new File(destFile.getParentFile().getAbsoluteFile(), destFile.getName() + ".java");
       }
       if(destFile.exists()) {
           int r = JOptionPane.showConfirmDialog(mainPanel, "The file already exists do you want to overwrite it?", "Are You Sure?", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
           if(r == JOptionPane.CANCEL_OPTION) {
               return null;
           }
           if(r == JOptionPane.NO_OPTION) {
               return pickJavaSourceLocation();
           }
       }
       return destFile;
    }

    public static String normalizeFormName(String formName) {
        StringBuilder dest = new StringBuilder();
        char previous = '_';
        for(int iter = 0 ; iter < formName.length() ; iter++) {
            char current = formName.charAt(iter);
            if(current == '_' || current == ' ') {
                previous = '_';
                continue;
            }
            if(current >= 'a' && current <= 'z' || current >= 'A' && current <= 'Z' || current >= '0' && current <= '9') {
                if(previous == '_' || previous == ' ') {
                    dest.append(Character.toUpperCase(current));
                } else {
                    dest.append(current);
                }
                previous = current;
            } else {
                previous = '_';
            }
        }
        return dest.toString();
    }

    /**
     * Converts a command upper case underscored string to cammel case e.g.: MYFORM_MY_COMMAND to MyFormMyCommand
     */
    private String camelCaseCommandName(String cmd) {
        StringBuilder dest = new StringBuilder();
        char previous = '_';
        for(int iter = 0 ; iter < cmd.length() ; iter++) {
            char current = cmd.charAt(iter);
            if(current == '_' || current == ' ') {
                previous = '_';
                continue;
            }
            if(current >= 'a' && current <= 'z' || current >= 'A' && current <= 'Z' || current >= '0' && current <= '9') {
                if(previous == '_' || previous == ' ') {
                    dest.append(Character.toUpperCase(current));
                } else {
                    dest.append(Character.toLowerCase(current));
                }
                previous = current;
            } else {
                previous = '_';
            }
        }
        return dest.toString();
    }

    String pickMainScreenForm() {
        if(loadedResources.getUIResourceNames() == null || loadedResources.getUIResourceNames().length < 1) {
            JOptionPane.showMessageDialog(mainPanel, "You must have a UI builder entry for this feature", "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        String[] arr = new String[loadedResources.getUIResourceNames().length];
        System.arraycopy(loadedResources.getUIResourceNames(), 0, arr, 0, arr.length);
        Arrays.sort(arr, String.CASE_INSENSITIVE_ORDER);
        JComboBox main = new JComboBox(arr);
        String lastPick = Preferences.userNodeForPackage(ResourceEditorView.class).get("lastMainScreenPick", null);
        if(lastPick != null) {
            for(int iter = 0 ; iter < arr.length ; iter++) {
                if(lastPick.equals(arr[iter])) {
                    main.setSelectedIndex(iter);
                    break;
                }
            }
        }
        JOptionPane.showMessageDialog(mainPanel, main, "Please Pick Main Screen", JOptionPane.PLAIN_MESSAGE);
        return (String)main.getSelectedItem();
    }

    String generateStateMachineCode(String uiResourceName, File destFile, boolean promptUserForPackageName) {
        return generateStateMachineCodeImpl(uiResourceName, destFile, promptUserForPackageName, loadedResources, mainPanel);
    }
    
    public static String generateStateMachineCodeEx(String uiResourceName, File destFile, boolean promptUserForPackageName, EditableResources load, java.awt.Component errorParent) {
        loadedResources = load;
        return generateStateMachineCodeImpl(uiResourceName, destFile, promptUserForPackageName, load, errorParent);
    }
    
    private static String generateStateMachineCodeImpl(String uiResourceName, File destFile, boolean promptUserForPackageName, EditableResources loadResources, java.awt.Component errorParent) {
        String packageString = "";
        File currentFile = destFile;
        while(currentFile.getParent() != null) {
            String shortName = currentFile.getParentFile().getName();
            if(shortName.equalsIgnoreCase("src")) {
                break;
            }
            if(shortName.indexOf(':') > -1 || shortName.length() == 0) {
                break;
            }
            if(shortName.equalsIgnoreCase("org") ||
                    shortName.equalsIgnoreCase("com") ||
                    shortName.equalsIgnoreCase("net") ||
                    shortName.equalsIgnoreCase("gov")) {
                if(packageString.length() > 0) {
                    packageString = shortName + "." + packageString;
                } else {
                    packageString = shortName;
                }
                break;
            }
            if(packageString.length() > 0) {
                packageString = shortName + "." + packageString;
            } else {
                packageString = shortName;
            }
            currentFile = currentFile.getParentFile();
        }

        final Map<String, String> nameToClassLookup = new HashMap<String, String>();
        final Map<String, Integer> commandMap = new HashMap<String, Integer>();
        final List<Integer> unhandledCommands = new ArrayList<Integer>();
        final List<String[]> actionComponents = new ArrayList<String[]>();
        final Map<String, String> allComponents = new HashMap<String, String>();
        initCommandMapAndNameToClassLookup(nameToClassLookup, commandMap, unhandledCommands, actionComponents, allComponents);

        // list all the .ovr files and add them to the nameToClassLookup 
        if(loadedFile != null && loadedFile.getParentFile() != null) {
            File overrideDir = new File(loadedFile.getParentFile().getParentFile(), "override");  
            if(overrideDir.exists()) {
                File[] ovrFiles = overrideDir.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File file, String string) {
                        return string.endsWith(".ovr");
                    }
                });
                for(File ovr : ovrFiles) {
                    try {
                        EditableResources er = EditableResources.open(new FileInputStream(ovr));
                        for(String currentResourceName : er.getUIResourceNames()) {
                            UIBuilder b = new UIBuilder() {
                                protected com.codename1.ui.Component createComponentInstance(String componentType, Class cls) {
                                    if(cls.getName().startsWith("com.codename1.ui.")) {
                                        // subpackage of CodenameOne should be registered
                                        if(cls.getName().lastIndexOf(".") > 15) {
                                            nameToClassLookup.put(componentType, cls.getName());
                                        }
                                    } else {
                                        nameToClassLookup.put(componentType, cls.getName());
                                    }
                                    return null;
                                }
                            };
                            b.createContainer(er, currentResourceName);
                        }
                    } catch(IOException ioErr) {
                        ioErr.printStackTrace();
                    }                
                }
            }
        }
        
        if(promptUserForPackageName) {
            JTextField packageName = new JTextField(packageString);
            JOptionPane.showMessageDialog(errorParent, packageName, "Please Pick The Package Name", JOptionPane.PLAIN_MESSAGE);
            packageString = packageName.getText();
        }
        List<String> createdMethodNames = new ArrayList<String>();
        try {
            Writer w = new FileWriter(destFile);
            w.write("/**\n");
            w.write(" * This class contains generated code from the Codename One Designer, DO NOT MODIFY!\n");
            w.write(" * This class is designed for subclassing that way the code generator can overwrite it\n");
            w.write(" * anytime without erasing your changes which should exist in a subclass!\n");
            w.write(" * For details about this file and how it works please read this blog post:\n");
            w.write(" * http://codenameone.blogspot.com/2010/10/ui-builder-class-how-to-actually-use.html\n");
            w.write("*/\n");
            if(packageString.length() > 0) {
                w.write("package " + packageString + ";\n\n");
            }
            String className = destFile.getName().substring(0, destFile.getName().indexOf('.'));
            boolean hasIo = false;
            for(String currentName : nameToClassLookup.keySet()) {
                if(nameToClassLookup.get(currentName).indexOf("com.codename1.ui.io") > -1) {
                    hasIo = true;
                    break;
                }
            }
            w.write("import com.codename1.ui.*;\n");
            w.write("import com.codename1.ui.util.*;\n");
            w.write("import com.codename1.ui.plaf.*;\n");
            w.write("import java.util.Hashtable;\n");
            if(hasIo) {
                w.write("import com.codename1.ui.io.*;\n");
                w.write("import com.codename1.components*;\n");
            }
            w.write("import com.codename1.ui.events.*;\n\n");
            w.write("public abstract class " + className  +
                    " extends UIBuilder {\n");
            w.write("    private Container aboutToShowThisContainer;\n");

            w.write("    /**\n");
            w.write("     * this method should be used to initialize variables instead of\n");
            w.write("     * the constructor/class scope to avoid race conditions\n");
            w.write("     */\n");
            w.write("    /**\n    * @deprecated use the version that accepts a resource as an argument instead\n    \n**/\n");
            w.write("    protected void initVars() {}\n\n");
            w.write("    protected void initVars(Resources res) {}\n\n");
            w.write("    public " + className + "(Resources res, String resPath, boolean loadTheme) {\n");
            w.write("        startApp(res, resPath, loadTheme);\n");
            w.write("    }\n\n");
            w.write("    public Container startApp(Resources res, String resPath, boolean loadTheme) {\n");
            w.write("        initVars();\n");
            if(hasIo) {
                w.write("        NetworkManager.getInstance().start();\n");
            }
            for(String currentName : nameToClassLookup.keySet()) {
                w.write("        UIBuilder.registerCustomComponent(\"" + currentName + "\", " +
                        nameToClassLookup.get(currentName) + ".class);\n");
            }
            w.write("        if(loadTheme) {\n");
            w.write("            if(res == null) {\n");
            w.write("                try {\n");
            w.write("                    if(resPath.endsWith(\".res\")) {\n");
            w.write("                        res = Resources.open(resPath);\n");
            w.write("                        System.out.println(\"Warning: you should construct the state machine without the .res extension to allow theme overlays\");\n");
            w.write("                    } else {\n");
            w.write("                        res = Resources.openLayered(resPath);\n");
            w.write("                    }\n");
            w.write("                } catch(java.io.IOException err) { err.printStackTrace(); }\n");
            w.write("            }\n");
            w.write("            initTheme(res);\n");
            w.write("        }\n");
            w.write("        if(res != null) {\n");
            w.write("            setResourceFilePath(resPath);\n");
            w.write("            setResourceFile(res);\n");
            w.write("            initVars(res);\n");
            w.write("            return showForm(getFirstFormName(), null);\n");
            w.write("        } else {\n");
            w.write("            Form f = (Form)createContainer(resPath, getFirstFormName());\n");
            w.write("            initVars(fetchResourceFile());\n");
            w.write("            beforeShow(f);\n");
            w.write("            f.show();\n");
            w.write("            postShow(f);\n");
            w.write("            return f;\n");
            w.write("        }\n");
            w.write("    }\n\n");
            w.write("    protected String getFirstFormName() {\n");
            w.write("        return \"" + uiResourceName + "\";\n");
            w.write("    }\n\n");
            w.write("    public Container createWidget(Resources res, String resPath, boolean loadTheme) {\n");
            w.write("        initVars();\n");
            if(hasIo) {
                w.write("        NetworkManager.getInstance().start();\n");
            }
            for(String currentName : nameToClassLookup.keySet()) {
                w.write("        UIBuilder.registerCustomComponent(\"" + currentName + "\", " +
                        nameToClassLookup.get(currentName) + ".class);\n");
            }
            w.write("        if(loadTheme) {\n");
            w.write("            if(res == null) {\n");
            w.write("                try {\n");
            w.write("                    res = Resources.openLayered(resPath);\n");
            w.write("                } catch(java.io.IOException err) { err.printStackTrace(); }\n");
            w.write("            }\n");
            w.write("            initTheme(res);\n");
            w.write("        }\n");
            w.write("        return createContainer(resPath, \"" + uiResourceName + "\");\n");
            w.write("    }\n\n");

            w.write("    protected void initTheme(Resources res) {\n");
            w.write("            String[] themes = res.getThemeResourceNames();\n");
            w.write("            if(themes != null && themes.length > 0) {\n");
            w.write("                UIManager.getInstance().setThemeProps(res.getTheme(themes[0]));\n");
            w.write("            }\n");
            w.write("    }\n\n");


            w.write("    public " + className + "() {\n");
            w.write("    }\n\n");

            w.write("    public " + className + "(String resPath) {\n");
            w.write("        this(null, resPath, true);\n");
            w.write("    }\n\n");

            w.write("    public " + className + "(Resources res) {\n");
            w.write("        this(res, null, true);\n");
            w.write("    }\n\n");

            w.write("    public " + className + "(String resPath, boolean loadTheme) {\n");
            w.write("        this(null, resPath, loadTheme);\n");
            w.write("    }\n\n");

            w.write("    public " + className + "(Resources res, boolean loadTheme) {\n");
            w.write("        this(res, null, loadTheme);\n");
            w.write("    }\n\n");

            for(String componentName : allComponents.keySet()) {
                String componentType = allComponents.get(componentName);
                String methodName = " find" + normalizeFormName(componentName);

                // an edge case where a space preceds a number in a component name and the same name
                // exists without a space might trigger this situation and thus code that won't compile
                if(!createdMethodNames.contains(methodName)) {
                    if(componentType.equals("com.codename1.ui.Form") || componentType.equals("com.codename1.ui.Dialog")) {
                        continue;
                    }
                    createdMethodNames.add(methodName);
                    w.write("    public " + componentType + methodName + "(Component root) {\n");
                    w.write("        return (" + componentType + ")" + "findByName(\"" + componentName + "\", root);\n");
                    w.write("    }\n\n");
                    w.write("    public " + componentType + methodName + "() {\n");
                    w.write("        " + componentType + " cmp = (" + componentType + ")" + "findByName(\"" + componentName + "\", Display.getInstance().getCurrent());\n");
                    w.write("        if(cmp == null && aboutToShowThisContainer != null) {\n");
                    w.write("            cmp = (" + componentType + ")" + "findByName(\"" + componentName + "\", aboutToShowThisContainer);\n");
                    w.write("        }\n");
                    w.write("        return cmp;\n");
                    w.write("    }\n\n");
                }
            }

            if(commandMap.size() > 0) {
                for(String key : commandMap.keySet()) {
                    w.write("    public static final int COMMAND_" + key + " = " + commandMap.get(key) + ";\n");
                }
                w.write("\n");
                StringBuilder methodSwitch = new StringBuilder("    protected void processCommand(ActionEvent ev, Command cmd) {\n        switch(cmd.getId()) {\n");
                for(String key : commandMap.keySet()) {
                    String camelCase = "on" + key;
                    boolean isAbstract = unhandledCommands.contains(commandMap.get(key));
                    if(isAbstract) {
                        w.write("    protected abstract void ");
                        w.write(camelCase);
                        w.write("();\n\n");
                    } else {
                        w.write("    protected boolean ");
                        w.write(camelCase);
                        w.write("() {\n        return false;\n    }\n\n");
                    }

                    methodSwitch.append("            case COMMAND_");
                    methodSwitch.append(key);
                    methodSwitch.append(":\n");
                    methodSwitch.append("                ");
                    if(isAbstract) {
                        methodSwitch.append(camelCase);
                        methodSwitch.append("();\n                break;\n\n");
                    } else {
                        methodSwitch.append("if(");
                        methodSwitch.append(camelCase);
                        methodSwitch.append("()) {\n                    ev.consume();\n                    return;\n                }\n                break;\n\n");
                    }
                }
                methodSwitch.append("        }\n        if(ev.getComponent() != null) {\n            handleComponentAction(ev.getComponent(), ev);\n        }\n    }\n\n");
                w.write(methodSwitch.toString());
            }

            writeFormCallbackCode(w, "    protected void exitForm(Form f) {\n", "f.getName()", "exit", "f", "Form f");
            writeFormCallbackCode(w, "    protected void beforeShow(Form f) {\n    aboutToShowThisContainer = f;\n", 
                    "f.getName()", "before", "f", "Form f");
            writeFormCallbackCode(w, "    protected void beforeShowContainer(Container c) {\n        aboutToShowThisContainer = c;\n", 
                    "c.getName()", "beforeContainer", "c", "Container c");
            writeFormCallbackCode(w, "    protected void postShow(Form f) {\n", "f.getName()", "post", "f", "Form f");
            writeFormCallbackCode(w, "    protected void postShowContainer(Container c) {\n", "c.getName()", "postContainer", "c", "Container c");
            writeFormCallbackCode(w, "    protected void onCreateRoot(String rootName) {\n", "rootName", "onCreate", "", "");
            writeFormCallbackCode(w, "    protected Hashtable getFormState(Form f) {\n        Hashtable h = super.getFormState(f);\n", 
                    "f.getName()", "getState", "f, h", "Form f, Hashtable h", "return h;");
            writeFormCallbackCode(w, "    protected void setFormState(Form f, Hashtable state) {\n        super.setFormState(f, state);\n", 
                    "f.getName()", "setState", "f, state", "Form f, Hashtable state");

            List<String> listComponents = new ArrayList<String>();
            for(String currentName : allComponents.keySet()) {
                String value = allComponents.get(currentName);
                if(value.equals("com.codename1.ui.List") || value.equals("com.codename1.ui.ComboBox") ||
                        value.equals("com.codename1.ui.list.MultiList") ||
                        value.equals("com.codename1.ui.Calendar")) {
                    listComponents.add(currentName);
                }
            }

            List<String> containerListComponents = new ArrayList<String>();
            for(String currentName : allComponents.keySet()) {
                String value = allComponents.get(currentName);
                if(value.equals("com.codename1.ui.list.ContainerList")) {
                    containerListComponents.add(currentName);
                }
            }

            if(listComponents.size() > 0) {
                w.write("    protected boolean setListModel(List cmp) {\n");
                w.write("        String listName = cmp.getName();\n");
                for(String listName : listComponents) {
                    w.write("        if(\"");
                    w.write(listName);
                    w.write("\".equals(listName)) {\n");
                    w.write("            return initListModel");
                    w.write(normalizeFormName(listName));
                    w.write("(cmp);\n        }\n");
                }
                w.write("        return super.setListModel(cmp);\n    }\n\n");
                for(String listName : listComponents) {
                    w.write("    protected boolean initListModel");
                    w.write(normalizeFormName(listName));
                    w.write("(List cmp) {\n");
                    w.write("        return false;\n    }\n\n");
                }
            }

            if(containerListComponents.size() > 0) {
                w.write("    protected boolean setListModel(com.codename1.ui.list.ContainerList cmp) {\n");
                w.write("        String listName = cmp.getName();\n");
                for(String listName : containerListComponents) {
                    w.write("        if(\"");
                    w.write(listName);
                    w.write("\".equals(listName)) {\n");
                    w.write("            return initListModel");
                    w.write(normalizeFormName(listName));
                    w.write("(cmp);\n        }\n");
                }
                w.write("        return super.setListModel(cmp);\n    }\n\n");
                for(String listName : containerListComponents) {
                    w.write("    protected boolean initListModel");
                    w.write(normalizeFormName(listName));
                    w.write("(com.codename1.ui.list.ContainerList cmp) {\n");
                    w.write("        return false;\n    }\n\n");
                }
            }

            if(actionComponents.size() > 0) {
                Object lastFormName = null;
                StringBuilder methods = new StringBuilder();
                w.write("    protected void handleComponentAction(Component c, ActionEvent event) {\n");
                w.write("        Container rootContainerAncestor = getRootAncestor(c);\n");
                w.write("        if(rootContainerAncestor == null) return;\n");
                w.write("        String rootContainerName = rootContainerAncestor.getName();\n");
                w.write("        Container leadParentContainer = c.getParent().getLeadParent();\n");
                w.write("        if(leadParentContainer != null && leadParentContainer.getClass() != Container.class) {\n");
                w.write("            c = c.getParent().getLeadParent();\n");
                w.write("        }\n");
                w.write("        if(rootContainerName == null) return;\n");
                for(String[] currentCmp : actionComponents) {
                    if(lastFormName != currentCmp[1]) {
                        if(lastFormName != null) {
                            w.write("        }\n");
                        }
                        w.write("        if(rootContainerName.equals(\"");
                        w.write(currentCmp[1]);
                        w.write("\")) {\n");
                        lastFormName = currentCmp[1];
                    }
                    w.write("            if(\"");
                    w.write(currentCmp[0]);
                    w.write("\".equals(c.getName())) {\n");
                    String methodName = "on" + normalizeFormName(currentCmp[1]) +
                            "_" + normalizeFormName(currentCmp[0]) + "Action";
                    w.write("                ");
                    w.write(methodName);
                    w.write("(c, event);\n");
                    w.write("                return;\n");
                    w.write("            }\n");

                    methods.append("      protected void ");
                    methods.append(methodName);
                    methods.append("(Component c, ActionEvent event) {\n      }\n\n");
                }
                w.write("        }\n    }\n\n");
                w.write(methods.toString());
            }

            w.write("}\n");
            w.close();
        } catch(IOException ioErr) {
            ioErr.printStackTrace();
            JOptionPane.showMessageDialog(errorParent, "IO Error: " + ioErr, "IO Error", JOptionPane.ERROR_MESSAGE);
        }
        return packageString;
    }

    private static void writeFormCallbackCode(Writer w, String methodSig, String getString, String prefix, String args, String argDefinition) throws IOException {
        writeFormCallbackCode(w, methodSig, getString, prefix, args, argDefinition, "return;");
    }
    
    private static void writeFormCallbackCode(Writer w, String methodSig, String getString, String prefix, String args, String argDefinition, String returnStatement) throws IOException {
        w.write(methodSig);
        for(String ui : loadedResources.getUIResourceNames()) {
            w.write("        if(\"");
            w.write(ui);
            w.write("\".equals(");
            w.write(getString);
            w.write(")) {\n");
            w.write("            ");
            w.write(prefix);
            w.write(normalizeFormName(ui));
            w.write("(");
            w.write(args);
            w.write(");\n");    
            w.write("            aboutToShowThisContainer = null;\n");
            w.write("            ");
            w.write(returnStatement);
            w.write("\n        }\n\n");
        }
        w.write("            ");
        w.write(returnStatement);
        w.write("\n");
        w.write("    }\n\n");
        for(String ui : loadedResources.getUIResourceNames()) {
            w.write("\n    protected void ");
            w.write(prefix);
            w.write(normalizeFormName(ui));
            w.write("(");
            w.write(argDefinition);
            w.write(") {\n");
            w.write("    }\n\n");
        }
    }
    
private void deleteUnusedImagesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteUnusedImagesActionPerformed
    Vector<String> images = new Vector<String>();
    for(String img : loadedResources.getImageResourceNames()) {
        if(!isInUse(img)) {
            images.add(img);
        }
    }
    if(images.size() > 0) {
        if(loadedFile != null && loadedFile.getParentFile().getName().equals("src")) {
            File source = new File(loadedFile.getParentFile(), "userclasses/StateMachine.java");
            if(source.exists()) {
                byte[] data = new byte[(int)source.length()];
                try {
                    DataInputStream di = new DataInputStream(new FileInputStream(source));
                    di.readFully(data);
                    String s = new String(data);
                    Vector<String> notUsedInCode = new Vector<String>();
                    for(String img : images) {
                        if(!s.contains("\"" + img + "\"")) {
                            notUsedInCode.add(img);
                        }
                    }
                    if(notUsedInCode.size() == 0) {
                        return;
                    }
                    images = notUsedInCode;
                } catch(Exception err) {
                    err.printStackTrace();
                }
            }
        }
        Collections.sort(images);
        JList imgs = new JList(images);
        imgs.setSelectionInterval(0, images.size());
        int result = JOptionPane.showConfirmDialog(mainPanel, new JScrollPane(imgs), "Press OK To Delete Selected Images", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if(result == JOptionPane.OK_OPTION) {
            for(int iter = 0 ; iter < images.size() ; iter++) {
                if(imgs.isSelectedIndex(iter)) {
                    loadedResources.remove((String)images.elementAt(iter));
                }
            }
        }
    }
}//GEN-LAST:event_deleteUnusedImagesActionPerformed

private void duplicateItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_duplicateItemActionPerformed
    if(selectedResource != null && loadedResources.containsResource(selectedResource)) {
        Box rename = new Box(BoxLayout.X_AXIS);
        rename.add(new JLabel("New Name: "));
        JTextField field = new JTextField(selectedResource, 20);
        rename.add(Box.createHorizontalStrut(3));
        rename.add(field);
        int result = JOptionPane.showConfirmDialog(mainPanel, rename, "Duplicate", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if(result == JOptionPane.OK_OPTION) {
            String val = field.getText();
            if(loadedResources.containsResource(val)) {
                JOptionPane.showMessageDialog(mainPanel, "An Element By This Name Already Exists", "Rename", JOptionPane.ERROR_MESSAGE);
                duplicateItemActionPerformed(evt);
                return;
            }

            try {
                // this effectively creates a new instance of the object
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                boolean m = loadedResources.isModified();
                loadedResources.save(bo);
                if(m) {
                    loadedResources.setModified();
                }
                bo.close();
                EditableResources r = new EditableResources();
                r.openFile(new ByteArrayInputStream(bo.toByteArray()));
                loadedResources.addResourceObjectDuplicate(selectedResource, val, r.getResourceObject(selectedResource));
                setSelectedResource(val);
            } catch(IOException err) {
                err.printStackTrace();
            }
        }
    } else {
        JOptionPane.showMessageDialog(mainPanel, "An Element Must Be Selected", "Rename", JOptionPane.ERROR_MESSAGE);
    }
}//GEN-LAST:event_duplicateItemActionPerformed

//private GenerateHelper helper = new GenerateHelper();

private static boolean configureNetbeans() {
    String node = Preferences.userNodeForPackage(ResourceEditorView.class).get("netbeansInstall", null);
    if(node == null) {
        File f = new File("c:\\Program Files");
        if(f.exists()) {
            File[] files = f.listFiles(new java.io.FileFilter() {
                    public boolean accept(File pathname) {
                        return pathname.getName().toLowerCase().contains("netbeans");
                    }
                });
            if(files.length > 0) {
                for(File current : files) {
                    File test = new File(current, "bin/netbeans.exe");
                    if(test.exists()) {
                        node = test.getAbsolutePath();
                        break;
                    }
                }
            }
        }
    }
    if(node != null) {
        Preferences.userNodeForPackage(ResourceEditorView.class).put("lastDir", node);
    }
    File[] result = showOpenFileChooser("Netbeans Executable", "");
    if(result != null) {
        if(ResourceEditorApp.IS_MAC) {
            if(result[0].isDirectory()) {
                String p = result[0].getAbsolutePath() + "/Contents/MacOS/netbeans";
                Preferences.userNodeForPackage(ResourceEditorView.class).put("netbeansInstall", p);
            } else {
                String p = result[0].getAbsolutePath();
                Preferences.userNodeForPackage(ResourceEditorView.class).put("netbeansInstall", p);
            }
        } else {
            Preferences.userNodeForPackage(ResourceEditorView.class).put("netbeansInstall", result[0].getAbsolutePath());
        }
        return true;
    }
    return false;
}

/**
 * Creates a sorted image combo box that includes image previews. The combo box
 * can be searched by typing a letter even when images are used for the values...
 */
public static void initImagesComboBox(JComboBox cb, final EditableResources res, boolean asString, final boolean includeNull) {
    initImagesComboBox(cb, res, asString, includeNull, false);
}

/**
 * Creates a sorted image combo box that includes image previews. The combo box
 * can be searched by typing a letter even when images are used for the values...
 */
public static void initImagesComboBox(JComboBox cb, final EditableResources res, boolean asString, final boolean includeNull, boolean blockTimelines) {
    String[] imgs = res.getImageResourceNames();
    if(blockTimelines) {
        List<String> nonT = new ArrayList<String>();
        for(String c : imgs) {
            if(!(res.getImage(c) instanceof Timeline)) {
                nonT.add(c);
            }
        }
        imgs = new String[nonT.size()];
        nonT.toArray(imgs);
    }
    final String[] images = imgs;
    Arrays.sort(images, String.CASE_INSENSITIVE_ORDER);
    if(asString) {
        if(includeNull) {
            String[] n = new String[images.length + 1];
            System.arraycopy(images, 0, n, 1, images.length);
            cb.setModel(new DefaultComboBoxModel(n));
        } else {
            cb.setModel(new DefaultComboBoxModel(images));
        }
        cb.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                boolean n = false;
                if(value == null) {
                    value = "[null]";
                    n = true;
                }
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if(!n) {
                    setIcon(new CodenameOneImageIcon(res.getImage((String)value), 24, 24));
                } else {
                    setIcon(null);
                }
                return this;
            }
        });
    } else {
        int offset = 0;
        com.codename1.ui.Image[] arr;
        if(includeNull) {
            arr = new com.codename1.ui.Image[images.length + 1];
            offset++;
        } else {
            arr = new com.codename1.ui.Image[images.length];
        }
        for(String c : images) {
            arr[offset] = res.getImage(c);
            offset++;
        }
        cb.setModel(new DefaultComboBoxModel(arr));
        cb.setKeySelectionManager(new JComboBox.KeySelectionManager() {
            private String current;
            private long lastPress;
                public int selectionForKey(char aKey, ComboBoxModel aModel) {
                    long t = System.currentTimeMillis();
                    aKey = Character.toLowerCase(aKey);
                    if(t - lastPress < 800) {
                        current += aKey;
                    } else {
                        current = "" + aKey;
                    }
                    lastPress = t;
                    for(int iter = 0 ; iter < images.length ; iter++) {
                        if(images[iter].toLowerCase().startsWith(current)) {
                            if(includeNull) {
                                return iter + 1;
                            }
                            return iter;
                        }
                    }
                    return -1;
                }
            });
        cb.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                com.codename1.ui.Image i = (com.codename1.ui.Image)value;
                if(value == null) {
                    value = "[null]";
                } else {
                    value = res.findId(value);
                }
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if(i != null) {
                    setIcon(new CodenameOneImageIcon(i, 24, 24));
                } else {
                    setIcon(null);
                }
                return this;
            }
        });
    }
}

private void setupNetbeansActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_setupNetbeansActionPerformed
    configureNetbeans();
}//GEN-LAST:event_setupNetbeansActionPerformed

private void addSVGImagesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addSVGImagesActionPerformed
    new ImageSVGEditor(loadedResources, null, this).selectFiles();
}//GEN-LAST:event_addSVGImagesActionPerformed

public static void helpVideo(String url) {
    try {
        Desktop.getDesktop().browse(new URI(url));
    } catch(Throwable ioErr) {
        ioErr.printStackTrace();
    }
}

private void addMultiImagesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addMultiImagesActionPerformed
    new AddAndScaleMultiImage().selectFiles(mainPanel, loadedResources);
}//GEN-LAST:event_addMultiImagesActionPerformed

private void pulsateEffectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pulsateEffectActionPerformed
    new PulsateEditor().pulsateWizard(loadedResources, mainPanel);
}//GEN-LAST:event_pulsateEffectActionPerformed

private void imageSizesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_imageSizesActionPerformed
    class ImageSize {
        String name;
        int size;
    }
    int total = 0;
    Vector images = new Vector();
    for(String imageName : loadedResources.getImageResourceNames()) {
        com.codename1.ui.Image img = loadedResources.getImage(imageName);
        ImageSize size = new ImageSize();
        size.name = imageName;
        Object o = loadedResources.getResourceObject(imageName);

        // special case for multi image which can be all of the internal images...
        if(o instanceof EditableResources.MultiImage) {
            for(Object c : ((EditableResources.MultiImage)o).getInternalImages()) {
                size.size += ((com.codename1.ui.EncodedImage)c).getImageData().length;
            }
            images.add(size);
        } else {
            if(img instanceof com.codename1.ui.EncodedImage) {
                size.size = ((com.codename1.ui.EncodedImage)img).getImageData().length;
                images.add(size);
            } else {
                if(img.isSVG()) {
                    SVG s = (SVG)img.getSVGDocument();
                    size.size = s.getSvgData().length;
                    images.add(size);
                }
            }
        }
        total += size.size;
    }
    Collections.sort(images, new Comparator() {
            public int compare(Object o1, Object o2) {
                ImageSize i1 = (ImageSize)o1;
                ImageSize i2 = (ImageSize)o2;
                return i2.size - i1.size;
            }
        });
    JPanel p = new JPanel(new java.awt.BorderLayout());

    JList list = new JList(images);
    p.add(java.awt.BorderLayout.NORTH, new JLabel("Total " + (total / 1024) + "kb in " +
            loadedResources.getImageResourceNames().length + " images"));
    p.add(java.awt.BorderLayout.CENTER, new JScrollPane(list));
    list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                ImageSize s = (ImageSize)value;
                value = s.name + " " + (s.size / 1024) + "kb (" + s.size + "b)";
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
    });
    JOptionPane.showMessageDialog(mainPanel, p, "Sizes", JOptionPane.PLAIN_MESSAGE);
}//GEN-LAST:event_imageSizesActionPerformed

private void resPasswordActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resPasswordActionPerformed
    showPasswordDialog("");
}//GEN-LAST:event_resPasswordActionPerformed


    static void delTree(File dir) {
        for(File f : dir.listFiles()) {
            if(f.isDirectory()) {
                delTree(f);
            } else {
                f.delete();
            }
        }
    }


    private void checkerboardColorsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkerboardColorsActionPerformed
        CheckerBoardColorCalibration c = new CheckerBoardColorCalibration((JFrame)SwingUtilities.windowForComponent(mainPanel), true);
        c.pack();
        c.setVisible(true);
    }//GEN-LAST:event_checkerboardColorsActionPerformed

    private void resetNetbeansSettingsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetNetbeansSettingsActionPerformed
            Preferences.userNodeForPackage(ResourceEditorView.class).remove("netbeansInstall");
    }//GEN-LAST:event_resetNetbeansSettingsActionPerformed

    private void findMultiImagesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_findMultiImagesActionPerformed
        new FindMultiImages(mainPanel, loadedResources);
    }//GEN-LAST:event_findMultiImagesActionPerformed

private boolean configureOptiPNG() {
    String node = Preferences.userNodeForPackage(ResourceEditorView.class).get("optiPng", null);
    if(node == null || !new File(node).exists()) {
        JOptionPane.showMessageDialog(mainPanel, "Please select the OptiPng executable in the following dialog\nOptiPng can be downloaded from http://optipng.sourceforge.net/", "Select OptiPNG", JOptionPane.INFORMATION_MESSAGE);
        File[] result = showOpenFileChooser("OptiPng Executable", "exe", "app");
        if(result != null) {
            Preferences.userNodeForPackage(ResourceEditorView.class).put("optiPng", result[0].getAbsolutePath());
            return true;
        }
    } else {
        return true;
    }
    return false;
}

    private com.codename1.ui.EncodedImage optimize(com.codename1.ui.EncodedImage img, String exe) {
        try {
            File tmp = File.createTempFile("encodedImage", ".png");
            FileOutputStream f = new FileOutputStream(tmp);
            f.write(img.getImageData());
            f.close();
            final Process p = new ProcessBuilder(exe, "-o7", tmp.getAbsolutePath()).redirectErrorStream(true).start();
            final InputStream stream = p.getInputStream();
            final boolean[] running = new boolean[] {true};
            final boolean[] destroyed = new boolean[] {false};
            new Thread() {
                public void run() {
                    try {
                        int i = stream.read();
                        while(i > -1) {
                            System.out.print((char)i);
                            i = stream.read();
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }.start();
            // timeout
            new Thread() {
                public void run() {
                    long t = System.currentTimeMillis();
                    while(running[0]) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ex) {
                        }
                        if(System.currentTimeMillis() - t > 4000) {
                            destroyed[0] = true;
                            p.destroy();
                        }
                    }
                }
            }.start();
            p.waitFor();
            running[0] = false;
            if(destroyed[0]) {
                return null;
            }
            DataInputStream input = new DataInputStream(new FileInputStream(tmp));
            byte[] data = new byte[(int)tmp.length()];
            input.read(data);
            input.close();
            tmp.delete();
            return EncodedImage.create(data);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private void launchOptiPngActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_launchOptiPngActionPerformed
        if(loadedResources != null && configureOptiPNG()) {
            final ProgressMonitor pm = new ProgressMonitor(mainPanel, "Processing Images", "", 0, loadedResources.getImageResourceNames().length);
            new Thread() {
                public void run() {
                    String node = Preferences.userNodeForPackage(ResourceEditorView.class).get("optiPng", null);
                    int prog = 0;
                    for(String imageName : loadedResources.getImageResourceNames()) {
                        if(pm.isCanceled()) {
                            pm.close();
                            return;
                        }
                        pm.setProgress(prog);
                        prog++;
                        pm.setNote(imageName);
                        Object image = loadedResources.getImage(imageName);
                        if(image instanceof com.codename1.ui.EncodedImage) {
                            if(loadedResources.getResourceObject(imageName) != image) {
                                // multi-image...
                               EditableResources.MultiImage multi = (EditableResources.MultiImage)loadedResources.getResourceObject(imageName);
                               EditableResources.MultiImage n = new EditableResources.MultiImage();
                               EncodedImage[] arr = new EncodedImage[multi.getInternalImages().length];
                               for(int iter = 0 ; iter < multi.getInternalImages().length ; iter++) {
                                    EncodedImage current = optimize(multi.getInternalImages()[iter], node);
                                    if(current != null) {
                                        arr[iter] = current;
                                    } else {
                                        arr[iter] = multi.getInternalImages()[iter];
                                    }
                               }
                               n.setInternalImages(arr);
                               n.setDpi(multi.getDpi());
                               loadedResources.setMultiImage(imageName, n);
                            } else {
                                EncodedImage current = optimize((EncodedImage)image, node);
                                if(current != null) {
                                    loadedResources.setImage(imageName, current);
                                }
                            }
                        }
                    }
                    pm.close();
                }
            }.start();
        }
    }//GEN-LAST:event_launchOptiPngActionPerformed

    void aboutActionPerformed() {
        aboutActionPerformed(null);
    }
    
    private void aboutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutActionPerformed
        new About(mainPanel);
    }//GEN-LAST:event_aboutActionPerformed

private void signupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_signupActionPerformed
        try {
            Desktop.getDesktop().browse(new URI("http://www.codenameone.com/build-server.html"));
        } catch (Exception ex) {
            ex.printStackTrace();
        }

}//GEN-LAST:event_signupActionPerformed

private void loginActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loginActionPerformed
    new LoginDialog(mainPanel);
}//GEN-LAST:event_loginActionPerformed

private void addImageMainActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addImageMainActionPerformed
    new ImageRGBEditor(loadedResources, null, this).selectFiles();
}//GEN-LAST:event_addImageMainActionPerformed

private void addImageAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addImageAllActionPerformed
    new ImageRGBEditor(loadedResources, null, this).selectFiles();
}//GEN-LAST:event_addImageAllActionPerformed

private void addImageSVGActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addImageSVGActionPerformed
    new ImageSVGEditor(loadedResources, null, this).selectFiles();
}//GEN-LAST:event_addImageSVGActionPerformed

private void addImageMultiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addImageMultiActionPerformed
    new AddAndScaleMultiImage().selectFiles(mainPanel, loadedResources);
}//GEN-LAST:event_addImageMultiActionPerformed

private void addImageTimelineActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addImageTimelineActionPerformed
    TimelineEditor.selectFile(this, loadedResources, null);
}//GEN-LAST:event_addImageTimelineActionPerformed

private void imageBorderWizardMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_imageBorderWizardMenuItemActionPerformed
    String themeName = null;
    if(selectedResource != null) {
        Object o = loadedResources.getResourceObject(selectedResource);
        if(!(o instanceof Hashtable)) {
            JOptionPane.showMessageDialog(mainPanel, "A theme must be selected to use this feature", "Image Border Wizard", JOptionPane.ERROR_MESSAGE);
            return;
        }
        themeName = selectedResource;
    } 
    ImageBorderWizardTabbedPane iw = new ImageBorderWizardTabbedPane(loadedResources, themeName);
    JDialog dlg = new JDialog(SwingUtilities.windowForComponent(mainPanel), "Border Wizard");
    dlg.setLayout(new java.awt.BorderLayout());
    dlg.add(java.awt.BorderLayout.CENTER, iw);
    dlg.pack();
    dlg.setLocationRelativeTo(mainPanel);
    dlg.setModal(true);
    dlg.setVisible(true);
    setSelectedResource(selectedResource);
}//GEN-LAST:event_imageBorderWizardMenuItemActionPerformed

    private void setNativeTheme(String file, boolean local) {
        try {
            InputStream i;
            if(local) {
                i = getClass().getResourceAsStream(file);
            } else {
                i = new FileInputStream(file);
            }
            EditableResources er = new EditableResources();
            er.openFile(i);
            JavaSEPortWithSVGSupport.setNativeTheme(er);
            JavaSEPortWithSVGSupport.setShowEDTWarnings(false);
            JavaSEPortWithSVGSupport.setShowEDTViolationStacks(false);
            i.close();
            Preferences p = Preferences.userNodeForPackage(getClass());
            p.put("nativeCN1Theme", file);
            p.putBoolean("nativeCN1Local", local);
            if(selectedResource != null) {
                setSelectedResource(selectedResource);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(mainPanel, "Error " + ex, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void initNativeTheme() {
        Preferences p = Preferences.userNodeForPackage(getClass());
        String t = p.get("nativeCN1Theme", "/iPhoneTheme.res");
        boolean local = p.getBoolean("nativeCN1Local", true);
        setNativeTheme(t, local);
        if(local) {
            if(t.equals("/iPhoneTheme.res")) {
                iosNativeTheme.setSelected(true);
                return;
            } 
            if(t.equals("/iOS7Theme.res")) {
                ios7NativeTheme.setSelected(true);
                return;
            } 
            if(t.equals("/androidTheme.res")) {
                android2NativeTheme.setSelected(true);
                return;
            } 
            if(t.equals("/blackberry_theme.res")) {
                blackberryNativeTheme.setSelected(true);
                return;
            } 
            if(t.equals("/winTheme.res")) {
                winNativeTheme.setSelected(true);
                return;
            } 
        } else {
            customNativeTheme.setSelected(true);
        }
    }

private void iosNativeThemeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_iosNativeThemeActionPerformed
        setNativeTheme("/iPhoneTheme.res", true);
}//GEN-LAST:event_iosNativeThemeActionPerformed

private void android2NativeThemeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_android2NativeThemeActionPerformed
        setNativeTheme("/androidTheme.res", true);
}//GEN-LAST:event_android2NativeThemeActionPerformed

private void blackberryNativeThemeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_blackberryNativeThemeActionPerformed
        setNativeTheme("/blackberry_theme.res", true);
}//GEN-LAST:event_blackberryNativeThemeActionPerformed

private void customNativeThemeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_customNativeThemeActionPerformed
        File[] f = showOpenFileChooser(false, "Resource Files (*.res)", ".res");
        if(f != null && f.length > 0) {
            setNativeTheme(f[0].getAbsolutePath(), false);
        }
}//GEN-LAST:event_customNativeThemeActionPerformed

private void addNewTimelineActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addNewTimelineActionPerformed
    String timelineName = "Timeline ";
    int index = 1;
    while(hasStringInArray(loadedResources.getImageResourceNames(), timelineName + index)) {
        index++;
    }
    loadedResources.setImage(timelineName + index, Timeline.createTimeline(3000, new AnimationObject[0], new com.codename1.ui.geom.Dimension(300, 300)));
    setSelectedResource(timelineName + index);
}//GEN-LAST:event_addNewTimelineActionPerformed

private void import9PatchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_import9PatchActionPerformed
    if(selectedResource != null && loadedResources.getResourceObject(selectedResource) instanceof Hashtable) {
        new Import9Patch(mainPanel, loadedResources, selectedResource);
        setSelectedResource(selectedResource);
    }
}//GEN-LAST:event_import9PatchActionPerformed

private void livePreviewUIActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_livePreviewUIActionPerformed
    LivePreview.setLivePreviewEnabled(livePreviewUI.isSelected(), mainPanel, this);
    Preferences.userNodeForPackage(getClass()).putBoolean("LivePreview", livePreviewUI.isSelected());
}//GEN-LAST:event_livePreviewUIActionPerformed

    private void removeDPIHDActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeDPIHDActionPerformed
        removeDPI(Display.DENSITY_HD);
    }//GEN-LAST:event_removeDPIHDActionPerformed

    private void removeDPIVeryHighActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeDPIVeryHighActionPerformed
        removeDPI(Display.DENSITY_VERY_HIGH);
    }//GEN-LAST:event_removeDPIVeryHighActionPerformed

    private void removeDPIHighActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeDPIHighActionPerformed
        removeDPI(Display.DENSITY_HIGH);
    }//GEN-LAST:event_removeDPIHighActionPerformed

    private void removeDPIMediumActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeDPIMediumActionPerformed
        removeDPI(Display.DENSITY_MEDIUM);
    }//GEN-LAST:event_removeDPIMediumActionPerformed

    private void removeDPILowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeDPILowActionPerformed
        removeDPI(Display.DENSITY_LOW);
    }//GEN-LAST:event_removeDPILowActionPerformed

    private void removeDPIVeryLowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeDPIVeryLowActionPerformed
        removeDPI(Display.DENSITY_VERY_LOW);
    }//GEN-LAST:event_removeDPIVeryLowActionPerformed

    private void quickMultiImagesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_quickMultiImagesActionPerformed
        new AddAndScaleMultiImage().selectFilesSimpleMode(mainPanel, loadedResources);
    }//GEN-LAST:event_quickMultiImagesActionPerformed

private void winNativeThemeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_winNativeThemeActionPerformed
        setNativeTheme("/winTheme.res", true);
}//GEN-LAST:event_winNativeThemeActionPerformed

private void jRadioButtonMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButtonMenuItem2ActionPerformed
        setNativeTheme("/nokia_touch_theme.res", true);
}//GEN-LAST:event_jRadioButtonMenuItem2ActionPerformed

private void jRadioButtonMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButtonMenuItem1ActionPerformed
    setNativeTheme("/nokia_non_touch_theme.res", true);
}//GEN-LAST:event_jRadioButtonMenuItem1ActionPerformed

private void jRadioButtonMenuItem3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButtonMenuItem3ActionPerformed
    setNativeTheme("/nokia_asha_theme.res", true);
}//GEN-LAST:event_jRadioButtonMenuItem3ActionPerformed

private void jRadioButtonMenuItem4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButtonMenuItem4ActionPerformed
    setNativeTheme("/android_holo_light.res", true);
}//GEN-LAST:event_jRadioButtonMenuItem4ActionPerformed

private void ios7NativeThemeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ios7NativeThemeActionPerformed
    setNativeTheme("/iOS7Theme.res", true);
}//GEN-LAST:event_ios7NativeThemeActionPerformed

private void enableXMLTeamModeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_enableXMLTeamModeActionPerformed
    Preferences.userNodeForPackage(getClass()).putBoolean("XMLFileMode", enableXMLTeamMode.isSelected());
    EditableResources.setXMLEnabled(enableXMLTeamMode.isSelected());
}//GEN-LAST:event_enableXMLTeamModeActionPerformed

    private void removeMultiEntry(String name, EditableResources.MultiImage multi, int dpi) {
        int[] dpis = multi.getDpi();
        
        // don't remove the last DPI!
        if(dpis.length == 1) {
            return;
        }
        com.codename1.ui.EncodedImage[] imgs = multi.getInternalImages();
        for(int iter = 0 ; iter < dpis.length ; iter++) {
            if(dpis[iter] == dpi) {
                com.codename1.ui.EncodedImage[] newImages = new com.codename1.ui.EncodedImage[imgs.length - 1];
                int[] newDpis = new int[dpis.length - 1];
                int originalOffset = 0;
                for(int x = 0 ; x < newImages.length ; x++) {
                    if(originalOffset == iter) {
                        originalOffset++;
                    }
                    newImages[x] = imgs[originalOffset];
                    newDpis[x] = dpis[originalOffset];
                    originalOffset++;
                }

                multi = new EditableResources.MultiImage();
                multi.setDpi(newDpis);
                multi.setInternalImages(newImages);
                loadedResources.setMultiImage(name, multi);
                return;
            }
        }        
    }
    
    private void removeDPI(int dpi) {
        if(loadedResources == null) {
            return;
        }
        for(String s : loadedResources.getImageResourceNames()) {
            Object potentialMultiImage = loadedResources.getResourceObject(s);
            if(potentialMultiImage instanceof EditableResources.MultiImage) {
                EditableResources.MultiImage multi = (EditableResources.MultiImage)potentialMultiImage;
                removeMultiEntry(s, multi, dpi);
            }
        }
    }
    private void buildFilenameMap(File baseDir, Map<String, List<File>> map) {
        File[] f = baseDir.listFiles();
        for(File current : f) {
            if(current.isDirectory()) {
                buildFilenameMap(baseDir, map);
                continue;
            }
            String name = current.getName();
            List<File> files = map.get(name);
            if(files == null) {
                files = new ArrayList<File>();
                map.put(name, files);
            }
            files.add(current);
        }
    }

private void showPasswordDialog(String password) {
    JPanel pass = new JPanel(new java.awt.BorderLayout());
    JLabel lbl = new JLabel("<html><body>After setting the password saving the resource will<br>" +
                                        "prevent you from opening it without the password!<br>" +
                                        "<b>Passwords cannot be recovered!</b> We HIGHLY recommend you<br>" +
                                        "use \"Save As\" and maintain a backup without a password!<br>" +
                                        "To remove a password from the file just set both strings<br>" +
                                        "to an empty password.");
    pass.add(lbl, BorderLayout.NORTH);
    JPanel grid = new JPanel(new java.awt.GridLayout(2, 2));
    JPasswordField p1 = new JPasswordField(password);
    JPasswordField p2 = new JPasswordField();
    grid.add(new JLabel("Password"));
    grid.add(p1);
    grid.add(new JLabel("Confirm"));
    grid.add(p2);
    pass.add(grid, BorderLayout.CENTER);
    int v = JOptionPane.showConfirmDialog(mainPanel, pass, "Set Password", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
    if(v != JOptionPane.OK_OPTION) {
        return;
    }
    String ptext = p1.getText();
    for(char c : ptext.toCharArray()) {
        if(c > 127) {
            v = JOptionPane.showConfirmDialog(mainPanel, "Password must use only ascii characters, retry?", "Set Password", JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE);
            if(v == JOptionPane.YES_OPTION) {
                showPasswordDialog(ptext);
            }
            return;
        }
    }
    if(ptext.length() < 3 && ptext.length() != 0){
        v = JOptionPane.showConfirmDialog(mainPanel, "Password length must be more than 2 characters, retry?", "Set Password", JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE);
        if(v == JOptionPane.YES_OPTION) {
            showPasswordDialog(ptext);
        }
        return;
    }
    if(!ptext.equals(p2.getText())){
        v = JOptionPane.showConfirmDialog(mainPanel, "Password & confirmation don't match, retry?", "Set Password", JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE);
        if(v == JOptionPane.YES_OPTION) {
            showPasswordDialog(ptext);
        }
        return;
    }
    EditableResources.setCurrentPassword(ptext);
}

public boolean isNetBeans() {
    File nbProject = new File(loadedFile.getParentFile().getParentFile(), "nbproject");
    File idea = new File(loadedFile.getParentFile().getParentFile(), ".idea");
    return nbProject.exists() && !idea.exists();
}

/**
 * Opens the given file in an IDE (currently netbeans) on the given line number unless the line number is
 * less than 0
 */
public static void openInIDE(File f, int lineNumber) {
    // Check if this is a NetBeans project
    File nbProject = new File(loadedFile.getParentFile().getParentFile(), "nbproject");
    File idea = new File(loadedFile.getParentFile().getParentFile(), ".idea");
    boolean isIdea = idea.exists();
    
    // this is an eclipse project
    if(!nbProject.exists() || isIdea) {
        FileOutputStream fs = null;
        try {
            String userDir = System.getProperty("user.home");
            File resEditorLoc = new File(userDir,".codenameone");
            fs = new FileOutputStream(new File(resEditorLoc, "open.txt"));
            fs.write((f.getAbsolutePath() + ":" + lineNumber).getBytes());
            fs.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                fs.close();
            } catch (IOException ex) {
            }
        }
        if(isIdea) {
            f.setLastModified(System.currentTimeMillis());
        }
        return;
    }
    
    String node = Preferences.userNodeForPackage(ResourceEditorView.class).get("netbeansInstall", null);
    if(manualIDESettings != null) {
        node = manualIDESettings;
    } else {
        if(node == null) {
            if(!configureNetbeans()) {
                return;
            }
            node = Preferences.userNodeForPackage(ResourceEditorView.class).get("netbeansInstall", null);
        }
    }
    try {
        String arg = f.getAbsolutePath();
        if(lineNumber > -1) {
            arg += ":" + lineNumber;
        }
        Runtime.getRuntime().exec(new String[] {
            node,
            "--open",
            arg
        });
    } catch(Exception err) {
        err.printStackTrace();
        JOptionPane.showMessageDialog(JFrame.getFrames()[0], "Error opening Netbeans: " + err, "Error", JOptionPane.ERROR_MESSAGE);
    }
}



    public static void createMIDletZip(InputStream sourceZip, File destination,
			 InputStream append, String appendName) throws IOException {

		byte[] buf = new byte[1024];

		ZipInputStream zin = new ZipInputStream(sourceZip);
		ZipOutputStream out = new ZipOutputStream(new FileOutputStream(destination));

		ZipEntry entry = zin.getNextEntry();
		while (entry != null) {
			String name = entry.getName();

            // Add ZIP entry to output stream.
            out.putNextEntry(new ZipEntry(name));

            // Transfer bytes from the ZIP file to the output file
            int len;
            while ((len = zin.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
			entry = zin.getNextEntry();
		}

		// Close the streams
		zin.close();

        // Add ZIP entry to output stream.
        out.putNextEntry(new ZipEntry(appendName));
        // Transfer bytes from the file to the ZIP file
        int len;
        while ((len = append.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        // Complete the entry
        out.closeEntry();
        append.close();

        // Complete the ZIP file
		out.close();
	}


    private String getUiResourceInUse(final String element) {
        for(String res : loadedResources.getUIResourceNames()) {
            if(!res.equals(element)) {
                if(isUiResourceInUse(element, res)) {
                    return res;
                }
            }
        }
        return null;
    }
    
    private boolean isUiResourceInUse(final String element, String resource) {
        final boolean[] flag = new boolean[1];
        UIBuilder uib = new UIBuilder() {
            public com.codename1.ui.Container createContainer(Resources res, String resourceName) {
                flag[0] = flag[0] || element.equals(resourceName);
                return super.createContainer(res, resourceName);
            }
        };
        uib.createContainer(loadedResources, resource);
        return flag[0];
    }
    
    /**
     * Returns true if the given image is used by a theme or timeline animation,
     * false otherwise.
     */
    private boolean isInUse(String imageName) {
        Object multi = loadedResources.getResourceObject(imageName);
        if(multi instanceof EditableResources.MultiImage) {
            EditableResources.MultiImage m = (EditableResources.MultiImage)multi;
            for(com.codename1.ui.Image i : m.getInternalImages()) {
                if(isInUse(i)) {
                    return true;
                }
            }
            return false;
        }
        com.codename1.ui.Image resourceValue = loadedResources.getImage(imageName);
        return isInUse(resourceValue);
    }

    private boolean isImageInTimeline(String name) {
        com.codename1.ui.Image resourceValue = loadedResources.getImage(name);
        // check if a timeline is making use of said image and replace it
        for(String image : loadedResources.getImageResourceNames()) {
            com.codename1.ui.Image current = loadedResources.getImage(image);
            if(current instanceof com.codename1.ui.animations.Timeline) {
                com.codename1.ui.animations.Timeline time = (com.codename1.ui.animations.Timeline)current;
                for(int iter = 0 ; iter < time.getAnimationCount() ; iter++) {
                    com.codename1.ui.animations.AnimationObject o = time.getAnimation(iter);
                    if(AnimationAccessor.getImage(o) == resourceValue) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    private boolean isImageInBorder(String name) {
        com.codename1.ui.Image resourceValue = loadedResources.getImage(name);
        for(String themeName : loadedResources.getThemeResourceNames()) {
            Hashtable theme = loadedResources.getTheme(themeName);
            // we need to check the existance of image borders to replace images there...
            for(Object v : theme.values()) {
                if(v instanceof Border) {
                    Border b = (Border)v;
                    // BORDER_TYPE_IMAGE
                    if(Accessor.getType(b) == Accessor.TYPE_IMAGE || Accessor.getType(b) == Accessor.TYPE_IMAGE_HORIZONTAL ||
                            Accessor.getType(b) == Accessor.TYPE_IMAGE_VERTICAL || Accessor.getType(b) == Accessor.TYPE_IMAGE_SCALED) {
                        com.codename1.ui.Image[] images = Accessor.getImages(b);
                        for(int i = 0 ; i < images.length ; i++) {
                            if(images[i] == resourceValue) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean isInUse(com.codename1.ui.Image resourceValue) {
        for(String themeName : loadedResources.getThemeResourceNames()) {
            Hashtable theme = loadedResources.getTheme(themeName);
            if(theme.values().contains(resourceValue)) {
                return true;
            }
            // we need to check the existance of image borders to replace images there...
            for(Object v : theme.values()) {
                if(v instanceof Border) {
                    Border b = (Border)v;
                    // BORDER_TYPE_IMAGE
                    if(Accessor.getType(b) == Accessor.TYPE_IMAGE || Accessor.getType(b) == Accessor.TYPE_IMAGE_HORIZONTAL ||
                            Accessor.getType(b) == Accessor.TYPE_IMAGE_VERTICAL || Accessor.getType(b) == Accessor.TYPE_IMAGE_SCALED) {
                        com.codename1.ui.Image[] images = Accessor.getImages(b);
                        for(int i = 0 ; i < images.length ; i++) {
                            if(images[i] == resourceValue) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        // check if a timeline is making use of said image and replace it
        for(String image : loadedResources.getImageResourceNames()) {
            com.codename1.ui.Image current = loadedResources.getImage(image);
            if(current instanceof com.codename1.ui.animations.Timeline) {
                com.codename1.ui.animations.Timeline time = (com.codename1.ui.animations.Timeline)current;
                for(int iter = 0 ; iter < time.getAnimationCount() ; iter++) {
                    com.codename1.ui.animations.AnimationObject o = time.getAnimation(iter);
                    if(AnimationAccessor.getImage(o) == resourceValue) {
                        return true;
                    }
                }
            }
        }

        // check if a UI resource is making use of the image
        UIBuilderOverride builder = new UIBuilderOverride();
        for(String uiResource : loadedResources.getUIResourceNames()) {
            com.codename1.ui.Container c = builder.createContainer(loadedResources, uiResource);
            if(findImageInContainer(c, resourceValue)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isImageUsedInCommand(com.codename1.ui.Command cmd, com.codename1.ui.Image i) {
            if(cmd == null) {
                return false;
            }
            if(cmd.getPressedIcon() == i) {
                return true;
            }
            if(cmd.getRolloverIcon() == i) {
                return true;
            }
            if(cmd.getDisabledIcon() == i) {
                return true;
            }
            return false;
    }

    public static boolean findImageInContainer(com.codename1.ui.Container c, com.codename1.ui.Image i) {
        if(c instanceof com.codename1.ui.Form) {
            com.codename1.ui.Form frm = ((com.codename1.ui.Form)c);
            for(int cmdIter = 0 ; cmdIter < frm.getCommandCount() ; cmdIter++) {
                com.codename1.ui.Command cmd = frm.getCommand(cmdIter);
                if(isImageUsedInCommand(cmd, i)) {
                    return true;
                }
            }
            if(isImageUsedInCommand(frm.getBackCommand(), i)) {
                return true;
            }
            if(isImageUsedInCommand(frm.getClearCommand(), i)) {
                return true;
            }
        }
        for(int iter = 0 ; iter < c.getComponentCount() ; iter++) {
            com.codename1.ui.Component current = c.getComponentAt(iter);
            if(current instanceof com.codename1.ui.Label) {
                if(((com.codename1.ui.Label)current).getIcon() == i) {
                    return true;
                }
                if(current instanceof com.codename1.ui.Button) {
                    if(((com.codename1.ui.Button)current).getPressedIcon() == i) {
                        return true;
                    }
                    if(((com.codename1.ui.Button)current).getRolloverIcon() == i) {
                        return true;
                    }
                    if(((com.codename1.ui.Button)current).getDisabledIcon() == i) {
                        return true;
                    }
                } else {
                    if(current instanceof com.codename1.ui.Slider) {
                        if(((com.codename1.ui.Slider)current).getThumbImage() == i) {
                            return true;
                        }
                    }
                }
            }
            if(current instanceof com.codename1.ui.List) {
                com.codename1.ui.list.ListModel model = ((com.codename1.ui.List)current).getModel();
                for(int entry = 0 ; entry < model.getSize() ; entry++) {
                    Object o = model.getItemAt(entry);
                    if(o instanceof Hashtable) {
                        for(Object val : ((Hashtable)o).values()) {
                            if(val == i) {
                                return true;
                            }
                        }
                    }
                }
            }
            if(current instanceof com.codename1.ui.Container) {
                if(findImageInContainer((com.codename1.ui.Container)current, i)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void refreshSelection(String sel) {
        if(sel != null) {
            if(loadedResources.containsResource(sel)) {
                setSelectedResource(sel);
            } else {
                setSelectedResource(null);
            }
        }
    }

    private static boolean hasStringInArray(String[] s, String val) {
        for(String current : s) {
            if(current.equalsIgnoreCase(val)) {
                return true;
            }
        }
        return false;
    }

    public void showAddResourceDialog(int type) {
        AddResourceDialog addResource = new AddResourceDialog(loadedResources, type, false);

        if(JOptionPane.OK_OPTION == 
            JOptionPane.showConfirmDialog(mainPanel, addResource, "Select Name", 
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE)) {
            addResource.addResource(loadedResources, this);
        }
    }

    public String showAddImageResourceDialog() {
        AddImageResource addResource = new AddImageResource(mainPanel, loadedResources);

        if(addResource.isOK()) {
            return addResource.addResource(loadedResources, this);
        }
        return null;
    }

    public String showAddThemeResourceDialog() {
        AddThemeResource addResource = new AddThemeResource(mainPanel, loadedResources);

        if(addResource.isOkPressed()) {
            return addResource.addResource(loadedResources, this);
        }
        return null;
    }

    public String showAddUiResourceDialog() {
        
        if(!isGuiBuilderApp()){
            JOptionPane.showMessageDialog(mainPanel, "Notice, this is not a visual project type.");
        }
        
        AddUIResource addResource = new AddUIResource(mainPanel, loadedResources);

        if(addResource.isOkPressed()) {
            return addResource.addResource(loadedResources, this);
        }
        return null;
    }

    public static JFileChooser createFileChooser(final String label, final String... type) {
        String dir = Preferences.userNodeForPackage(ResourceEditorView.class).get("lastDir", System.getProperty("user.home"));
        JFileChooser chooser = new JFileChooser(dir);
        chooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                if(f.isDirectory()) {
                    return true;
                }
                for(String s : type) {
                    if(f.getName().toLowerCase().endsWith(s)) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public String getDescription() {
                return label;
            }
        });
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(false);
        return chooser;
    }
    
    public static File[] showOpenFileChooser() {
        return showOpenFileChooser(false, "Resource Files (*.res)", ".res");
    }

    public static File[] showOpenFileChooser(final String label, final String... type) {
        return showOpenFileChooser(false, label, type);
    }

    public static File[] showOpenFileChooser(boolean multi, final String label, final String... type) {
        return showFileChooser(multi, true, false, null, label, type);
    }

    public static File[] showOpenFileChooserWithTitle(String dialogTitle, boolean dir, final String label, final String... type) {
        return showFileChooser(false, true, dir, dialogTitle, label, type);
    }

    public static File[] showSaveDirFileChooserWithTitle(String dialogTitle, final String label, final String... type) {
        return showFileChooser(false, false, true, dialogTitle, label, type);
    }

    public static File[] showSaveDirFileChooser() {
        return showSaveDirFileChooser("Directories", "");
    }

    public static File[] showSaveFileChooser() {
        return showSaveFileChooser("All Files", "");
    }

    public static File[] showSaveDirFileChooser(final String label, final String... type) {
        return showFileChooser(false, false, true, null, label, type);
    }

    public static File[] showSaveFileChooser(final String label, final String... type) {
        return showFileChooser(false, false, false, null, label, type);
    }

    public static File[] showFileChooser(boolean multi, boolean open, boolean dir, String dialogTitle, final String label, final String... type) {
        if(!dir && multi && ResourceEditorApp.IS_MAC) {
            FileDialog fd = new FileDialog(java.awt.Frame.getFrames()[0]);

            // check if we can hack Java 7
            try {
                Method m = fd.getClass().getMethod("setMultipleMode", Boolean.TYPE);
                m.invoke(fd, true);

                fd.setFilenameFilter(new FilenameFilter() {

                    public boolean accept(File dir, String name) {
                        name = name.toLowerCase();
                        for(String t : type) {
                            if(name.endsWith(t)) {
                                return true;
                            }
                        }
                        return  false;

                    }
                });

                if(open) {
                    fd.setMode(FileDialog.LOAD);
                } else {
                    fd.setMode(FileDialog.SAVE);
                }
                if(dialogTitle != null) {
                    fd.setTitle(dialogTitle);
                }
                String defaultDir = Preferences.userNodeForPackage(ResourceEditorView.class).get("lastDir", System.getProperty("user.home"));
                fd.setDirectory(defaultDir);
                fd.pack();
                fd.setLocationByPlatform(true);
                fd.setVisible(true);

                m = fd.getClass().getMethod("getFiles");
                File[] files = (File[])m.invoke(fd);
                
                if(files != null && files.length > 0) {
                    Preferences.userNodeForPackage(ResourceEditorView.class).put("lastDir", files[0].getAbsolutePath());
                    return files;
                }
                return null;            
            } catch(Throwable t) {
                // failed... 
                t.printStackTrace();
            }
        }
        if(ResourceEditorApp.IS_MAC && !dir && !multi) {
            // on Mac we prefer the native AWT file chooser which is far superior
            FileDialog fd = new FileDialog(java.awt.Frame.getFrames()[0]);

            fd.setFilenameFilter(new FilenameFilter() {

                public boolean accept(File dir, String name) {
                    name = name.toLowerCase();
                    for(String t : type) {
                        if(name.endsWith(t)) {
                            return true;
                        }
                    }
                    return  false;

                }
            });
            
            if(open) {
                fd.setMode(FileDialog.LOAD);
            } else {
                fd.setMode(FileDialog.SAVE);
            }
            if(dialogTitle != null) {
                fd.setTitle(dialogTitle);
            }
            String defaultDir = Preferences.userNodeForPackage(ResourceEditorView.class).get("lastDir", System.getProperty("user.home"));
            fd.setDirectory(defaultDir);
            fd.pack();
            fd.setLocationByPlatform(true);
            fd.setVisible(true);
            if(fd.getFile() != null) {
                File selection = new File(fd.getDirectory(), fd.getFile());
                Preferences.userNodeForPackage(ResourceEditorView.class).put("lastDir", selection.getAbsolutePath());
                return new File[] {selection};
            }
            return null;            
        }
        JFileChooser c = createFileChooser(label, type);
        c.setMultiSelectionEnabled(multi);
        if(dialogTitle != null) {
            c.setDialogTitle(dialogTitle);
        }
        for(String t : type) {
            if(t.endsWith("jpg") || t.endsWith("gif") || t.endsWith("png") || t.endsWith("svg")) {
                new PreviewPane(c);
                break;
            }
        }
        if(dir) {
            c.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        } else {
            c.setFileSelectionMode(JFileChooser.FILES_ONLY);
        }
        if(open) {
            if(c.showOpenDialog(JFrame.getFrames()[0]) != JFileChooser.APPROVE_OPTION) {
                return null;
            }
        } else {
            if(c.showSaveDialog(JFrame.getFrames()[0]) != JFileChooser.APPROVE_OPTION) {
                return null;
            } 
        }
        Preferences.userNodeForPackage(ResourceEditorView.class).put("lastDir", c.getSelectedFile().getParentFile().getAbsolutePath());
        if(multi) {
            return c.getSelectedFiles();
        } else {
            return new File[] {c.getSelectedFile()};
        }
    }

    void addToRecentMenu(File selection) {
        recentFiles.remove(selection.getAbsolutePath());
        recentFiles.add(0, selection.getAbsolutePath());
        if(recentFiles.size() > 10) {
            recentFiles.remove(recentFiles.size() -  1);
        }
        refreshRecentMenu();
    }

    private class LoadResourceFileAction extends BlockingAction {
        private File selection;
        private Object result;
        private boolean canceled;
        
        public LoadResourceFileAction() {
            EditableResources.setCurrentPassword("");
            putValue(NAME, "Open");
            putValue(SHORT_DESCRIPTION, "Open");
            putValue(SMALL_ICON, new ImageIcon(getClass().getResource(IMAGE_DIR + "open.png")));
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        }
        
        public void start() {
            HorizontalList.setBlockRefeshWhileLoading(true);
            // prevent a load from overwriting the current data
            if(loadedResources != null && loadedResources.isModified()) {
                if(JOptionPane.showConfirmDialog(mainPanel, "File was modified, do you want to discard changes?", 
                    "Loading File", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION) {
                    canceled = true;
                    return;
                }
            }
            resourceEditor.removeAll();
            resourceEditor.revalidate();
            resourceEditor.repaint();
            if(fileToLoad == null) {
                File[] files = showOpenFileChooser();
                if(files != null) {
                    selection = files[0];
                }
            } else {
                selection = fileToLoad;
                fileToLoad = null;
            }
        }
        
        public void exectute() {
            if(canceled) {
                return;
            }
            platformOverrides.setSelectedIndex(0);
            if(selection != null) {
                try {
                    baseResDir = new File(selection.getParentFile().getParentFile(), "src");
                    JavaSEPortWithSVGSupport.setBaseResourceDir(baseResDir);
                    loadedFile = selection;

                    loadedResources.openFileWithXMLSupport(selection);
                    //loadedResources.openFile(new FileInputStream(selection));
                    File codenameone_settings = new File(selection.getParentFile().getParentFile(), "codenameone_settings.properties");
                    if(codenameone_settings.exists()) {
                        projectGeneratorSettings = new Properties();
                        InputStream i = new FileInputStream(codenameone_settings);
                        projectGeneratorSettings.load(i);
                        i.close();
                        if(selection.getName().equals(projectGeneratorSettings.getProperty("guiResource", null)) &&
                                projectGeneratorSettings.getProperty("userClass") != null) {
                            projectGeneratorSettings.put("userClassAbs",
                                    new File(codenameone_settings.getParentFile(), projectGeneratorSettings.getProperty("userClass")).getAbsolutePath());
                            if(projectGeneratorSettings.containsKey("netbeans")) {
                                manualIDESettings = projectGeneratorSettings.getProperty("netbeans");
                            }
                        } else {
                            projectGeneratorSettings = null;                        
                        }
                    } else {
                        projectGeneratorSettings = null;
                    }
                    addToRecentMenu(selection);
                    updateLoadedFile();
                    Preferences.userNodeForPackage(getClass()).put("lastDir", selection.getParentFile().getAbsolutePath());
                    result = loadedResources;
                    platformOverrides.setEnabled(true);
                    return;
                } catch (Exception ex) {
                    loadedFile = null;
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(mainPanel, "Fatal Error Loading the Resource File: " + ex, "Error", JOptionPane.ERROR_MESSAGE);
                    result = ex;
                    return;
                }
            }
            result = null;
        }
        
        public void afterComplete() {
            HorizontalList.setBlockRefeshWhileLoading(false);
            loadedResources.fireTreeNodeAdded(null, -1);
            selection = null;
            if(canceled) {
                canceled = false;
                return;
            }
            // Runs on the EDT.  Update the GUI based on
            // the result computed by doInBackground().
            if(result == null) {
                return;
            }
            if(result instanceof Exception) {
                // present the user with an error dialog
                JOptionPane.showMessageDialog(mainPanel, "An error occured while trying to load the file:\n" + result, 
                    "IO Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            getFrame().setTitle(loadedFile.getName() + " - Codename One Designer");
            treeArea.revalidate();
            livePreviewUIActionPerformed(null);
            LivePreview.updateServer(mainPanel);
            // expand the entire tree
            /*for (int i=0; i<resourceTree.getRowCount(); i++)
                resourceTree.expandRow(i);*/
        }
    }

    public void setNewMainForm(String mainForm) {
        Properties p = projectGeneratorSettings;
        if(p != null) {
            p.setProperty("mainForm", mainForm);
            File codenameone_settings = new File(ResourceEditorView.getLoadedFile().getParentFile().getParentFile(), "codenameone_settings.properties");
            if(codenameone_settings.exists()) {
                OutputStream o = null;
                try {
                    o = new FileOutputStream(codenameone_settings);
                    projectGeneratorSettings.store(o, "");
                    o.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(mainPanel, "Error In Saving Settings: " + ex, "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    try {
                        o.close();
                    } catch (IOException ex) {
                    }
                }
            }
        }
    }
    
    void setLoadedFile(File loadedFile) {
        this.loadedFile = loadedFile;
        updateLoadedFile();
        getFrame().setTitle(loadedFile.getName() + " - Codename One Designer");
    }

    private class SaveResourceFileAction extends BlockingAction {
        boolean dialogCanceled;
        
        public SaveResourceFileAction() {
            putValue(NAME, "Save");
            putValue(SHORT_DESCRIPTION, "Save");
            putValue(SMALL_ICON, new ImageIcon(getClass().getResource(IMAGE_DIR + "save.png")));
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        }
        
        public void start() {
            checkFile();
        }
        
        protected void checkFile() {
            dialogCanceled = false;
            if(loadedFile == null) {
                File[] files = showSaveFileChooser();
                if(files != null) {
                    loadedFile = files[0];
                    updateLoadedFile();
                    if(loadedFile.exists()) {
                        if(JOptionPane.showConfirmDialog(mainPanel, "File Already Exists, do you want to overwrite this file?", 
                            "File Exists", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.NO_OPTION) {
                            loadedFile = null;
                            checkFile();
                            updateLoadedFile();
                            return;
                        }
                    }
                    if(loadedFile.getName().indexOf('.') == -1) {
                        loadedFile = new File(loadedFile.getAbsolutePath() + ".res");
                        updateLoadedFile();
                    }
                } else {
                    dialogCanceled = true;
                }
            }
        }

        public void exectute() {
            if(!dialogCanceled) {
                FileOutputStream fileOut = null;
                FileOutputStream tempOut = null;
                FileInputStream tempIn = null;
                
                try {
                    // check if the file exists or not
                    if (loadedFile.exists()) {
                        // if the file exists, create a temporary file
                        File tempFile = File.createTempFile("_restmpfile_", null);
                        
                        // make sure the temp file is deleted when the application ends
                        tempFile.deleteOnExit();
                        
                        // save to the temp file
                        tempOut = new FileOutputStream(tempFile);
                        loadedResources.save(tempOut);
                        
                        // the save was successful, close the output stream and open an 
                        // input stream
                        tempOut.close();
                        tempIn = new FileInputStream(tempFile);
                        
                        // open a new output stream for the file
                        fileOut = new FileOutputStream(loadedFile);
                        
                        // copy the temp file to the real file
                        while (tempIn.available() > 0) {
                            // get the number of available bytes
                            int num = Math.min(tempIn.available(), 4096);
                            
                            // create an array to contain them
                            byte[] arr = new byte[num];
                            
                            // read the bytes from the temp file
                            tempIn.read(arr);
                            
                            // write the bytes to the real file
                            fileOut.write(arr);                            
                        }
                    } else {                       
                        // otherwise, simply save the file
                        fileOut = new FileOutputStream(loadedFile);
                        loadedResources.save(fileOut);
                    }
                    loadedResources.saveXML(loadedFile);
                    getFrame().setTitle(loadedFile.getName() + " - Codename One Designer");

                    // generate the code for the resource editor
                    if(projectGeneratorSettings != null) {
                        File f = new File(loadedFile.getParentFile().getParentFile(), projectGeneratorSettings.getProperty("baseClass"));
                        if(f.exists()) {
                            String formName = projectGeneratorSettings.getProperty("mainForm");
                            if(loadedResources.getResourceObject(formName) == null) {
                                String[] arr = loadedResources.getUIResourceNames();
                                if(arr.length > 0) {
                                    JPanel panel = new JPanel();
                                    JComboBox combo = new JComboBox(loadedResources.getUIResourceNames());
                                    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
                                    panel.add(new JLabel("The main form is missing"));
                                    panel.add(new JLabel("Please pick a new main form"));
                                    panel.add(combo);
                                    int result = JOptionPane.showConfirmDialog(mainPanel, panel, "Main Form Missing", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                                    if(result == JOptionPane.OK_OPTION) {
                                        setNewMainForm((String)combo.getSelectedItem());
                                    }
                                }
                            }
                            generateStateMachineCode(formName, f, false);
                        }
                        platformOverrides.setEnabled(true);
                    }
                    livePreviewUIActionPerformed(null);
                    LivePreview.updateServer(mainPanel);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(mainPanel, "Error saving to file: " + ex.toString(), "IO Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    closeSilent(fileOut);
                    closeSilent(tempOut);
                    closeSilent(tempIn);
                }
            }
        }
        
        private void closeSilent(Object o) {
            try {
                if (o != null) {
                    if(o instanceof OutputStream) {
                        ((OutputStream)o).close();
                    } else {
                        ((InputStream)o).close();
                    }
                }
            } catch (IOException ex) {}
        }
    }
    
    private class SaveResourceFileAsAction extends SaveResourceFileAction {
        File oldLoadedFile;
        SaveResourceFileAsAction() {
            putValue(NAME, "Save As...");
            putValue(SHORT_DESCRIPTION, "Save As...");
            putValue(SMALL_ICON, new ImageIcon(getClass().getResource(IMAGE_DIR + "saveas_1.png")));
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | KeyEvent.SHIFT_DOWN_MASK));
        }

        @Override
        protected void checkFile() {
            oldLoadedFile = loadedFile;
            loadedFile = null;
            super.checkFile();
        }

        @Override
        public void exectute() {
            super.exectute();
            if(dialogCanceled) {
                loadedFile = oldLoadedFile;
            }
            updateLoadedFile();
        }
    }

    private class SaveResourceFileAsNoSVGAction extends SaveResourceFileAsAction {
        SaveResourceFileAsNoSVGAction() {
            putValue(NAME, "Save Copy Without SVG...");
            putValue(SHORT_DESCRIPTION, "Save a Resource Copy Without SVG Files (only fallbacks)");
            putValue(SMALL_ICON, new ImageIcon(getClass().getResource(IMAGE_DIR + "saveas_1.png")));
            //putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | KeyEvent.SHIFT_DOWN_MASK));
        }

        @Override
        public void exectute() {
            if(!dialogCanceled) {
                // strip SVG...
                loadedResources.setIgnoreSVGMode(true);
                boolean pngMode = loadedResources.isIgnorePNGMode();
                loadedResources.setIgnorePNGMode(false);
                File oldLoadedFile = loadedFile;

                super.exectute();

                loadedFile = oldLoadedFile;
                updateLoadedFile();
                // restore default state and original file name...
                loadedResources.setIgnoreSVGMode(false);
                loadedResources.setIgnorePNGMode(pngMode);
                getFrame().setTitle(loadedFile.getName() + " - Codename One Designer");
            } 
        }
    }

    private static String generateSystemString(Font f) {
        StringBuilder font = new StringBuilder();
        if((f.getFace() & Font.FACE_MONOSPACE) != 0) {
            font.append("FACE_MONOSPACE | ");
        } else {
            if((f.getFace() & Font.FACE_PROPORTIONAL) != 0) {
                font.append("FACE_PROPORTIONAL | ");
            } else {
                font.append("FACE_SYSTEM | ");
            }
        }
        if((f.getStyle() & Font.STYLE_BOLD) != 0) {
            font.append("STYLE_BOLD | ");
        } else {
            if((f.getStyle() & Font.STYLE_ITALIC) != 0) {
                font.append("STYLE_ITALIC | ");
            } else {
                font.append("STYLE_PLAIN | ");
            }
        }
        if((f.getSize() & Font.SIZE_LARGE) != 0) {
            font.append("SIZE_LARGE");
        } else {
            if((f.getSize() & Font.SIZE_SMALL) != 0) {
                font.append("SIZE_SMALL");
            } else {
                font.append("SIZE_MEDIUM");
            }
        }
        return font.toString();
    }

    private class ExportResourceFileAction extends BlockingAction {
        private File destDir;
        ExportResourceFileAction() {
            putValue(NAME, "Export...");
            putValue(SHORT_DESCRIPTION, "Export...");
            //putValue(SMALL_ICON, new ImageIcon(getClass().getResource(IMAGE_DIR + "saveas_1.png")));
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_X, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | KeyEvent.ALT_DOWN_MASK));
        }

        @Override
        public void start() {
            destDir = null;
            File[] fileResults = showSaveDirFileChooserWithTitle("Export", "Directory", "");
            if(fileResults != null) {
                File result = fileResults[0];
                String[] files = result.list();
                if(files != null && files.length > 0) {
                    int i = JOptionPane.showConfirmDialog(mainPanel, "The directory is not empty do you want to proceed?", "Export", JOptionPane.YES_NO_OPTION);
                    if(i != JOptionPane.YES_OPTION) {
                        return;
                    }
                }
                destDir = result;
            }
        }

        @Override
        public void exectute() {
            if(destDir == null) {
                return;
            }
            try {
                StringBuilder buildXML = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<project name=\"Export\" default=\"generateResource\" basedir=\".\">\n" +
                    "<target name=\"generateResource\">\n<taskdef classpath=\"editor.jar\" " +
                    "classname=\"com.codename1.tools.resourcebuilder.CodenameOneTask\" name=\"build\" />\n    <build dest=\"output.res\">\n"
                );

                if(loadedResources.getFontResourceNames().length > 0) {
                    for(String s : loadedResources.getFontResourceNames()) {
                        if(loadedResources.getFont(s) instanceof EditorFont) {
                            EditorFont f = (EditorFont)loadedResources.getFont(s);
                            buildXML.append("   <font ");
                            buildXML.append("name=\"");
                            buildXML.append(s);
                            buildXML.append("\" ");

                            buildXML.append("system=\"");
                            buildXML.append(generateSystemString(f.getSystemFallback()));
                            buildXML.append("\" ");

                            buildXML.append("createBitmap=\"");
                            buildXML.append(f.isIncludesBitmap());
                            buildXML.append("\" ");
                            
                            if(f.isIncludesBitmap()) {
                                buildXML.append("charset=\"");
                                buildXML.append(toXMLString(f.getBitmapFont().getCharset()));
                                buildXML.append("\" ");
                            }

                            buildXML.append("logicalName=\"");
                            buildXML.append(f.getLookupFont());
                            buildXML.append("\" ");

                            buildXML.append("/>\n");
                        }
                    }
                }
                if(loadedResources.getDataResourceNames().length > 0) {
                    File dataDir = new File(destDir, "data");
                    dataDir.mkdir();
                    for(String s : loadedResources.getDataResourceNames()) {
                        buildXML.append("   <data file=\"data/");
                        buildXML.append(s);
                        buildXML.append("\" name=\"");
                        buildXML.append(s);
                        buildXML.append("\" />\n");
                        FileOutputStream o = new FileOutputStream(new File(dataDir, s));
                        o.write(loadedResources.getDataByteArray(s));
                        o.close();
                    }
                }

                if(loadedResources.getImageResourceNames().length > 0) {
                    File imageDir = new File(destDir, "image");
                    imageDir.mkdir();
                    for(String s : loadedResources.getImageResourceNames()) {
                        Object potentialMultiImage = loadedResources.getResourceObject(s);
                        if(potentialMultiImage instanceof EditableResources.MultiImage) {
                            EditableResources.MultiImage multi = (EditableResources.MultiImage)potentialMultiImage;
                            for(int iter = 0 ; iter < multi.getInternalImages().length ; iter++) {
                                com.codename1.ui.EncodedImage c = multi.getInternalImages()[iter];
                                String label = "_veryLow.png";
                                switch(multi.getDpi()[iter]) {
                                    case com.codename1.ui.Display.DENSITY_4K:
                                        label = "_4k.png";
                                        break;
                                    case com.codename1.ui.Display.DENSITY_2HD:
                                        label = "_2hd.png";
                                        break;
                                    case com.codename1.ui.Display.DENSITY_560:
                                        label = "_560.png";
                                        break;
                                    case com.codename1.ui.Display.DENSITY_HD:
                                        label = "_hd.png";
                                        break;
                                    case com.codename1.ui.Display.DENSITY_HIGH:
                                        label = "_high.png";
                                        break;
                                    case com.codename1.ui.Display.DENSITY_LOW:
                                        label = "_low.png";
                                        break;
                                    case com.codename1.ui.Display.DENSITY_MEDIUM:
                                        label = "_medium.png";
                                        break;
                                    case com.codename1.ui.Display.DENSITY_VERY_HIGH:
                                        label = "_veryHigh.png";
                                        break;
                                }
                                FileOutputStream o = new FileOutputStream(new File(imageDir, s + label));
                                DataOutputStream d = new DataOutputStream(o);
                                byte[] data = c.getImageData();
                                d.write(data);
                                o.close();

                                // we don't yet support multi images in the ant task, convert them to RGB images
                                if(iter == 0) {
                                    buildXML.append("   <image file=\"image/");
                                    buildXML.append(s);
                                    buildXML.append(label);
                                    buildXML.append("\" name=\"");
                                    buildXML.append(s);
                                    buildXML.append("\" />\n");
                                }
                            }
                            continue;
                        }
                        FileOutputStream o = new FileOutputStream(new File(imageDir, s));
                        com.codename1.ui.Image image = loadedResources.getImage(s);
                        if(image instanceof EncodedImage) {
                            DataOutputStream d = new DataOutputStream(o);
                            byte[] data = ((EncodedImage)image).getImageData();
                            d.write(data);
                        } else {
                            BufferedImage buffer = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
                            buffer.setRGB(0, 0, image.getWidth(), image.getHeight(), image.getRGB(), 0, image.getWidth());
                            ImageIO.write(buffer, "png", o);
                        }
                        o.close();
                        buildXML.append("   <image file=\"image/");
                        buildXML.append(s);
                        buildXML.append("\" name=\"");
                        buildXML.append(s);
                        buildXML.append("\" />\n");
                    }
                }

                if(loadedResources.getL10NResourceNames().length > 0) {
                    File l10nDir = new File(destDir, "l10n");
                    l10nDir.mkdir();
                    for(String s : loadedResources.getL10NResourceNames()) {
                        buildXML.append("   <l10n name=\"");
                        buildXML.append(s);
                        buildXML.append("\">");
                        Iterator i = loadedResources.getLocales(s);
                        File bundleDir = new File(l10nDir, s);
                        bundleDir.mkdir();
                        while(i.hasNext()) {
                            String language = (String)i.next();
                            Hashtable h = loadedResources.getL10N(s, language);
                            Properties p = new Properties();
                            p.putAll(h);
                            FileOutputStream o = new FileOutputStream(new File(bundleDir, language));
                            p.store(o, "Exported by the Codename One Designer");
                            o.close();
                            buildXML.append("       <locale file=\"l10n/");
                            buildXML.append(s);
                            buildXML.append("/");
                            buildXML.append(language);
                            buildXML.append("\" name=\"");
                            buildXML.append(s);
                            buildXML.append("\" />\n");
                        }
                        buildXML.append("</l10n>");
                    }
                }

                if(loadedResources.getThemeResourceNames().length > 0) {
                    int generatedId = 1;
                    File themeDir = new File(destDir, "theme");
                    themeDir.mkdir();
                    for(String s : loadedResources.getThemeResourceNames()) {
                        Properties p = new Properties();

                        Hashtable h = loadedResources.getTheme(s);
                        for(Object e : h.keySet()) {
                            String key = (String)e;
                            Object value = h.get(e);

                            if(key.indexOf("padding") > -1 || key.indexOf("margin") > -1) {
                                p.setProperty(key, (String)value);
                                continue;
                            }
                            if(key.indexOf("Color") > -1) {
                                // if this is a bg/fgSelection color we need to fix this to the
                                // new syntax...
                                if(key.indexOf("Selection") > -1) {
                                    int pointPos = key.indexOf('.');
                                    if(pointPos > -1) {
                                        key = key.substring(0, pointPos) + ".sel#" + key.substring(pointPos + 1).replace("Selection", "");
                                    } else {
                                        key = "sel#" + key.replace("Selection", "");
                                    }
                                }
                                p.setProperty(key, (String)value);
                                continue;
                            }
                            if(key.indexOf("border") > -1) {
                                p.setProperty(key, borderToString((com.codename1.ui.plaf.Border)value));
                                continue;
                            }
                            if(key.indexOf("font") > -1) {
                                String f = findResourceName(loadedResources, value);
                                // actual new resource type of font
                                if(f != null) {
                                    p.setProperty(key, f);
                                } else {
                                    // legacy system font that should be created in the Ant task
                                    com.codename1.ui.Font font = (com.codename1.ui.Font)value;
                                    buildXML.append("   <font system=\"");
                                    if(font instanceof EditorFont) {
                                        buildXML.append(generateSystemString(((EditorFont)font).getSystemFallback()));
                                    } else {
                                        buildXML.append(generateSystemString(font));
                                    }
                                    buildXML.append("\" name=\"");
                                    f = "gen" + generatedId;
                                    buildXML.append(f);
                                    generatedId++;
                                    buildXML.append("\" />\n");
                                    p.setProperty(key, f);
                                }
                                continue;
                            }

                            if(key.indexOf("bgImage") > -1) {
                                p.setProperty(key, findResourceName(loadedResources, value));
                                continue;
                            }

                            if(key.indexOf("transparency") > -1) {
                                p.setProperty(key, value.toString());
                                continue;
                            }

                            if(key.indexOf("bgType") > -1) {
                                byte v = ((Number)value).byteValue();
                                for(int i = 0 ; i < AddThemeEntry.BACKGROUND_VALUES.length ; i++) {
                                    if(AddThemeEntry.BACKGROUND_VALUES[i] == v) {
                                        p.setProperty(key, AddThemeEntry.BACKGROUND_STRINGS[i]);
                                    }
                                }
                                continue;
                            }
                            if(key.indexOf("bgGradient") > -1) {
                                Object[] v = (Object[])value;
                                if(v.length < 3) {
                                    p.setProperty(key, v[0] + "," + v[1]);
                                } else {
                                    p.setProperty(key, Integer.toHexString(((Number)v[0]).intValue()) + "," +
                                            Integer.toHexString(((Number)v[1]).intValue()) +
                                            "," + v[2] + "," + v[3] + "," + v[4]);
                                }
                                continue;
                            }
                        }

                        FileOutputStream o = new FileOutputStream(new File(themeDir, s));
                        p.store(o, "Exported by the Codename One Designer");
                        o.close();
                        buildXML.append("   <theme file=\"theme/");
                        buildXML.append(s);
                        buildXML.append("\" name=\"");
                        buildXML.append(s);
                        buildXML.append("\" />\n");
                    }
                }
                buildXML.append("   </build>\n</target>\n</project>\n");
                FileOutputStream buildXMLFile = new FileOutputStream(new File(destDir, "build.xml"));
                buildXMLFile.write(buildXML.toString().getBytes());
                buildXMLFile.close();
            } catch(IOException ioErr) {
                ioErr.printStackTrace();
                JOptionPane.showMessageDialog(mainPanel, "There was an IO error while exporting: " + ioErr, "IO Exception", JOptionPane.ERROR_MESSAGE);
            }            
        }

        private String toXMLString(String s) {
            return s.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\"", "&quot;");
        }

        private String findResourceName(EditableResources e, Object res) {
            for(String name : e.getResourceNames()) {
                if(e.getResourceObject(name) == res) {
                    return name;
                }
            }
            return null;
        }

        private String borderToString(Border border) {
            int type = Accessor.getType(border);
            switch(type) {
                case ThemeTaskConstants.TYPE_EMPTY:
                    return "EMPTY";
                case ThemeTaskConstants.TYPE_LINE:
                    // use theme colors?
                    if(Accessor.isThemeColors(border)) {
                        return "LINE(" + Accessor.getThickness(border) + ")";
                    } else {
                        return "LINE(" + Accessor.getThickness(border) + "," +
                                Integer.toHexString(Accessor.getColorA(border)) + ")";
                    }
                case ThemeTaskConstants.TYPE_ROUNDED:
                    // use theme colors?
                    if(Accessor.isThemeColors(border)) {
                        return "ROUNDED(" + Accessor.getArcWidth(border) + "," +
                                Accessor.getArcHeight(border) + ")";
                    } else {
                        return "ROUNDED(" + Accessor.getArcWidth(border) + "," +
                                Accessor.getArcHeight(border) + "," +
                                Integer.toHexString(Accessor.getColorA(border)) + ")";
                    }
                case ThemeTaskConstants.TYPE_ETCHED_RAISED:
                    if(Accessor.isThemeColors(border)) {
                        return "ETCHED_RAISED()";
                    } else {
                        return "ETCHED_RAISED(" + Integer.toHexString(Accessor.getColorA(border)) + "," +
                                Integer.toHexString(Accessor.getColorB(border)) + ")";
                    }
                case ThemeTaskConstants.TYPE_ETCHED_LOWERED:
                    if(Accessor.isThemeColors(border)) {
                        return "ETCHED_LOWERED()";
                    } else {
                        return "ETCHED_LOWERED(" + Integer.toHexString(Accessor.getColorA(border)) + "," +
                                Integer.toHexString(Accessor.getColorB(border)) + ")";
                    }
                case ThemeTaskConstants.TYPE_BEVEL_LOWERED:
                    if(Accessor.isThemeColors(border)) {
                        return "BEVEL_LOWERED()";
                    } else {
                        return "BEVEL_LOWERED(" + Integer.toHexString(Accessor.getColorA(border)) + "," +
                                Integer.toHexString(Accessor.getColorB(border)) + "," +
                                Integer.toHexString(Accessor.getColorC(border)) + "," +
                                Integer.toHexString(Accessor.getColorD(border)) + ")";
                    }
                case ThemeTaskConstants.TYPE_BEVEL_RAISED:
                    if(Accessor.isThemeColors(border)) {
                        return "BEVEL_RAISED()";
                    } else {
                        return "BEVEL_RAISED(" + Integer.toHexString(Accessor.getColorA(border)) + "," +
                                Integer.toHexString(Accessor.getColorB(border)) + "," +
                                Integer.toHexString(Accessor.getColorC(border)) + "," +
                                Integer.toHexString(Accessor.getColorD(border)) + ")";
                    }
                case ThemeTaskConstants.TYPE_IMAGE:
                    Object[] images = Accessor.getImages(border);
                    List<String> imageNames = new ArrayList<String>();
                    String borderStr = "IMAGE(";
                    for(int iter = 0 ; iter < images.length ; iter++) {
                        if(images[iter] != null) {
                            String current = findResourceName(loadedResources, images[iter]);
                            if(current != null && !imageNames.contains(current)) {
                                if(imageNames.size() == 0) {
                                    borderStr += current;
                                } else {
                                    borderStr += "," + current;
                                }
                                imageNames.add(current);
                            }
                        }
                    }
                    return borderStr + ")";
            }
            return null;
        }
    }
    
    class NewResourceAction extends AbstractAction {
        public NewResourceAction() {
            putValue(NAME, "New");
            putValue(SHORT_DESCRIPTION, "New");
            putValue(DEFAULT, "New");
            putValue(SMALL_ICON, new ImageIcon(getClass().getResource(IMAGE_DIR + "new.png")));
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        }
        
        public void actionPerformed(ActionEvent e) {
            if(loadedResources != null && loadedResources.isModified()) {
                if(JOptionPane.showConfirmDialog(mainPanel, "File was modified, you will lose your changes!\n" +
                        "Are you sure?", "New Resource", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            EditableResources.setCurrentPassword("");
            loadedResources.clear();
            loadedFile = null;
            updateLoadedFile();
            projectGeneratorSettings = null;
            platformOverrides.setEnabled(false);
            refreshAll();
            getFrame().setTitle("Untitled - Codename One Designer");

            // remove the resource editor and update the view
            resourceEditor.removeAll();
            resourceEditor.revalidate();
            resourceEditor.repaint();

            // notify about tree selection removal
            //removeResourceAction.setEnabled(false);
        }
    }
    
    private class HelpAction extends BlockingAction {
        public HelpAction() {
            putValue(NAME, "Help");
            putValue(SHORT_DESCRIPTION, "Help");
            putValue(SMALL_ICON, new ImageIcon(getClass().getResource(IMAGE_DIR + "help.png")));
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F1, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        }

        @Override
        public void exectute() {
            try {
                // create a temporary file for the resource editory PDF
                File f = File.createTempFile("CodenameOneDesigner", ".pdf");
                FileOutputStream out = new FileOutputStream(f);
                InputStream input = getClass().getResourceAsStream("/CodenameOne-Designer.pdf");
                byte[] buffer = new byte[65536];
                int size = input.read(buffer);
                while(size > -1) {
                    out.write(buffer, 0, size);
                    size = input.read(buffer);
                }
                out.close();
                f.deleteOnExit();
                try {
                    Desktop.getDesktop().open(f);
                } catch(Throwable err) {
                    // desktop class isn't available in Java 5...
                    JOptionPane.showMessageDialog(mainPanel, "Help is only available with a Java 6 or newer VM\nit requires Acrobat reader", "Help", JOptionPane.ERROR_MESSAGE);
                }
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(mainPanel, "Error creating help file: \n" + ex, "IO Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private class RedoAction extends AbstractAction {
        public RedoAction() {
            putValue(NAME, "Redo");
            putValue(SHORT_DESCRIPTION, "Redo");
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Y, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        }

        public void actionPerformed(ActionEvent ev) {
            refreshSelection(loadedResources.redo());
        }
    }

    private class UndoAction extends AbstractAction {
        public UndoAction() {
            putValue(NAME, "Undo");
            putValue(SHORT_DESCRIPTION, "Undo");
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        }

        public void actionPerformed(ActionEvent ev) {
            refreshSelection(loadedResources.undo());
        }
    }
    
    /**
     * Allow the user to edit resource names in the tree
     */
    class EditableTree extends JTree {
        public EditableTree() {
            // TODO: Fix this...
            setEditable(false);
        }
        
        public boolean isPathEditable(TreePath path) {
            Object value = path.getLastPathComponent();
            return !(value instanceof EditableResources.Node);
        }
    }
    

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem about;
    private javax.swing.JButton addData;
    private javax.swing.JButton addFont;
    private javax.swing.JButton addImageAll;
    private javax.swing.JButton addImageMain;
    private javax.swing.JButton addImageMulti;
    private javax.swing.JButton addImageSVG;
    private javax.swing.JButton addImageTimeline;
    private javax.swing.JMenuItem addImages;
    private javax.swing.JButton addL10N;
    private javax.swing.JMenuItem addMultiImages;
    private org.jdesktop.swingx.JXButton addNewTimeline;
    private javax.swing.JMenuItem addSVGImages;
    private javax.swing.JButton addTheme;
    private javax.swing.JButton addUserInterface;
    private javax.swing.JRadioButtonMenuItem android2NativeTheme;
    private javax.swing.JRadioButtonMenuItem blackberryNativeTheme;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JMenuItem checkerboardColors;
    private javax.swing.JRadioButtonMenuItem crossPlatformLFMenu;
    private javax.swing.JRadioButtonMenuItem customNativeTheme;
    private javax.swing.JScrollPane dataScroll;
    private javax.swing.JMenuItem deleteUnusedImages;
    private javax.swing.JMenuItem duplicateItem;
    private javax.swing.JMenu editMenu;
    private javax.swing.JCheckBoxMenuItem enableXMLTeamMode;
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JMenuItem exportRes;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JMenuItem findMultiImages;
    private javax.swing.JScrollPane fontsScroll;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JMenuItem imageBorderWizardMenuItem;
    private javax.swing.JScrollPane imageScroll;
    private javax.swing.JMenuItem imageSizes;
    private javax.swing.JMenuItem import9Patch;
    private javax.swing.JMenuItem importRes;
    private javax.swing.JCheckBoxMenuItem ios7NativeTheme;
    private javax.swing.JRadioButtonMenuItem iosNativeTheme;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu10;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenu jMenu4;
    private javax.swing.JMenu jMenu5;
    private javax.swing.JMenu jMenu6;
    private javax.swing.JMenu jMenu7;
    private javax.swing.JMenu jMenu8;
    private javax.swing.JMenu jMenu9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JRadioButtonMenuItem jRadioButtonMenuItem1;
    private javax.swing.JRadioButtonMenuItem jRadioButtonMenuItem2;
    private javax.swing.JRadioButtonMenuItem jRadioButtonMenuItem3;
    private javax.swing.JRadioButtonMenuItem jRadioButtonMenuItem4;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JSeparator jSeparator6;
    private javax.swing.JSeparator jSeparator8;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JMenuItem launchOptiPng;
    private javax.swing.JTextArea license;
    private javax.swing.JCheckBoxMenuItem livePreviewUI;
    private javax.swing.JScrollPane localizationScroll;
    private javax.swing.JMenuItem login;
    private javax.swing.JMenu lookAndFeelMenu;
    private javax.swing.JScrollPane mainImages;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JScrollPane multiImages;
    private javax.swing.ButtonGroup nativeThemeButtonGroup;
    private javax.swing.JMenuItem newMenuItem;
    private javax.swing.JMenuItem openMenuItem;
    private javax.swing.JMenuItem pulsateEffect;
    private javax.swing.JMenuItem quickMultiImages;
    private javax.swing.JMenu recentMenu;
    private javax.swing.JMenuItem redoItem;
    private javax.swing.JMenuItem removeDPIHD;
    private javax.swing.JMenuItem removeDPIHigh;
    private javax.swing.JMenuItem removeDPILow;
    private javax.swing.JMenuItem removeDPIMedium;
    private javax.swing.JMenuItem removeDPIVeryHigh;
    private javax.swing.JMenuItem removeDPIVeryLow;
    private javax.swing.JMenuItem renameItem;
    private javax.swing.JMenuItem resPassword;
    private javax.swing.JMenuItem resetNetbeansSettings;
    private javax.swing.JPanel resourceEditor;
    private javax.swing.JMenuItem saveAsMenuItem;
    private javax.swing.JMenuItem saveMenuItem;
    private javax.swing.JMenuItem setupNetbeans;
    private javax.swing.JMenuItem signup;
    private javax.swing.ButtonGroup svgGroup;
    private javax.swing.JScrollPane svgImages;
    private javax.swing.JRadioButtonMenuItem systemLFMenu;
    private javax.swing.JPanel themePanel;
    private javax.swing.JScrollPane themeScroll;
    private javax.swing.JScrollPane timelineImages;
    private javax.swing.JToolBar toolbar;
    private javax.swing.JPanel treeArea;
    private javax.swing.JMenuItem undoItem;
    private javax.swing.JScrollPane userInterfaceScroll;
    private javax.swing.JRadioButtonMenuItem winNativeTheme;
    // End of variables declaration//GEN-END:variables
}
