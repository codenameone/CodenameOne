/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
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
 * Please contact Codename One through http://www.codenameone.com/ if you 
 * need additional information or have any questions.
 */
package com.codename1.impl.javase;

import com.codename1.testing.TestUtils;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.Toolbar;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.SelectionListener;
import com.codename1.ui.list.ListModel;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.JOptionPane;

/**
 * The test recorder monitors Codename One and automatically creates a unit test
 * matching user actions.
 *
 * @author Shai Almog
 */
public class TestRecorder extends javax.swing.JFrame {
    private Form currentForm;
    private long waitTimer = 0;
    private int pointerPressedX, pointerPressedY;
    private boolean dragged;
    private int screenshots = 0;
    private static final String INITIAL_CODE = "package __PACKAGE_NAME__;\n\n" +
            "import com.codename1.testing.AbstractTest;\n\n" +
            "import com.codename1.ui.Display;\n\n" +
            "public class __TEST_NAME__ extends AbstractTest {\n" +
            "    public boolean runTest() throws Exception {\n";
    private static final String CLOSING_CODE = "        return true;\n" +
            "    }\n" +
            "}\n";
    private String generatedCode = "";
    
    /** Creates new form TestRecorder */
    public TestRecorder() {
        initComponents();
        File f = new File("codenameone_settings.properties");
        if (!f.exists()) {
            saveRecording.setEnabled(false);
        }
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                Form f = Display.getInstance().getCurrent();
                if(f != null) {
                    bindForm(f);
                }
                JavaSEPort.addFormChangeListener(new com.codename1.ui.events.ActionListener() {
                    Form oldForm;
                    int counter = 1;
                    
                    private String getFormTitle(Form f) {
                        String s = null;
                        if(f.getToolbar() != null) {
                            Component cmp = f.getToolbar().getTitleComponent();
                            if(cmp instanceof Label) {
                                s = ((Label)cmp).getText();
                            }
                        } else {
                            s =f.getTitle();
                        }
                        if(s == null) {
                            return "";
                        }
                        return s;
                    }
                    
                    public void actionPerformed(ActionEvent evt) {
                        Form newForm = Display.getInstance().getCurrent();
                        if(isRecording()) {
                            if(newForm.getName() != null && newForm.getName().length() > 0) {
                                generatedCode += "        waitForFormName(\"" + newForm.getName() + "\");\n";
                            } else {
                                String ft = getFormTitle(newForm);
                                String oldT = "";
                                if(oldForm != null) {
                                    oldT = getFormTitle(oldForm);
                                }
                                if(ft.equals(oldT) || ft.length() == 0) {
                                    generatedCode += "        waitForUnnamedForm();\n";
                                } else {
                                    generatedCode += "        waitForFormTitle(\"" + ft + "\");\n";
                                }
                                generatedCode += "        Display.getInstance().getCurrent().setName(\"Form_"  + counter + "\");\n";
                                Display.getInstance().getCurrent().setName("Form_"  + counter);
                                counter++;
                            }
                            updateTestCode();
                            resetWaitTimer();
                        }
                        bindForm(newForm);
                        oldForm = newForm;
                    }
                });
            }
        });
    }
    
    /**
     * Adds a wait to simulate user delay
     */
    void addWaitStatement() {
        if(isRecording()) {
            long t = System.currentTimeMillis();
            long d = t - waitTimer;
            generatedCode += "        waitFor(" + d + ");\n";
            waitTimer = t;
        }
    }

    private boolean isRecording() {
        return recording.isSelected();
    }
    
    /**
     * The current event requires no waiting
     */
    void resetWaitTimer() {
        waitTimer = System.currentTimeMillis();
    }
    
    private void updateTestCode() {
        script.setText((INITIAL_CODE.
                replace("__PACKAGE_NAME__", testsPackage.getText()) + 
                generatedCode + CLOSING_CODE).replace("__TEST_NAME__", testName.getText()));
    }
    
    private String toGameKeyConstant(int k) {
        switch(k) {
            case Display.GAME_DOWN:
                return "Display.GAME_DOWN";
            case Display.GAME_LEFT:
                return "Display.GAME_LEFT";
            case Display.GAME_RIGHT:
                return "Display.GAME_RIGHT";
            case Display.GAME_UP:
                return "Display.GAME_UP";
            case Display.GAME_FIRE:
                return "Display.GAME_FIRE";
        }
        return null;
    }
    
    void eventKeyPressed(int k) {
        if(isRecording()) {
            resetWaitTimer();
            int g = Display.getInstance().getGameAction(k);
            if(g <= 0) {
                generatedCode += "        keyPress(" + k + ");\n";
            } else {

                generatedCode += "        gameKeyPress(" + toGameKeyConstant(g) + ");\n";
            }
            updateTestCode();
        }
    }

    void eventKeyReleased(int k) {
        if(isRecording()) {
            
            addWaitStatement();
            int g = Display.getInstance().getGameAction(k);
            if(g <= 0) {
                generatedCode += "        keyPress(" + k + ");\n";
            } else {
                generatedCode += "        gameKeyPress(" + toGameKeyConstant(g) + ");\n";
            }
            updateTestCode();
        }
    }
    
    void eventPointerPressed(int x, int y) {
        pointerPressedX = x;
        pointerPressedY = y;
        dragged = false;
        resetWaitTimer();
    }

    private com.codename1.ui.Component getCodenameOneComponentAt(int x, int y) {
        return Display.getInstance().getCurrent().getComponentAt(x, y);
    }
        
    /**
     * Creates a path object that reaches the component
     */
    private String getPathToComponent(com.codename1.ui.Component cmp) {
        com.codename1.ui.Container contentPane = cmp.getComponentForm().getContentPane();
        List<Integer> l = new ArrayList<Integer>();
        while(cmp != contentPane) {
            com.codename1.ui.Container parent = cmp.getParent();
            if(parent == null) {
                return "(String)null";
            }
            l.add(0, parent.getComponentIndex(cmp));
            cmp = parent;
        }
        StringBuilder response = new StringBuilder("new int[]{");
        Iterator<Integer> iter = l.iterator();
        while(iter.hasNext()) {
            response.append(iter.next());
            if(iter.hasNext()) {
                response.append(", ");
            } else {
                response.append("}");
            }
        }
        return response.toString();
    }
    
    private String generatePointerEventArguments(int x, int y) {
        com.codename1.ui.Component cmp = getCodenameOneComponentAt(x, y);
        if(cmp.getParent() instanceof Form) {
            cmp = cmp.getParent();
        }
        
        String componentName = cmp.getName();
        if(componentName == null) {
            componentName = getPathToComponent(cmp);
        } else {
            componentName = "\"" + componentName + "\"";
        }
        float actualX = ((float)x - cmp.getAbsoluteX()) / ((float)cmp.getWidth());
        float actualY = ((float)y - cmp.getAbsoluteY()) / ((float)cmp.getHeight());
        return "(" + actualX + "f, " + actualY + "f, " + componentName + ");\n"; 
    }
    
    void eventPointerDragged(int x, int y) {
        if(isRecording()) {
            if(dragToScroll.isSelected()) {
                dragged = true;
            } else {
                if(!dragged) {
                    generatedCode += "        pointerPress" + generatePointerEventArguments(pointerPressedX, pointerPressedY);
                }
                dragged = true;
                addWaitStatement();
                generatedCode += "        pointerDrag" + generatePointerEventArguments(x, y);
                updateTestCode();
            }
        }
    }
    
    interface ComponentVisitor {
        public void visit(com.codename1.ui.Component c);
    }
    
    private void visitComponents(com.codename1.ui.Container cnt, ComponentVisitor v) {
        v.visit(cnt);
        int count = cnt.getComponentCount();
        for(int iter = 0 ; iter < count ; iter++) {
            com.codename1.ui.Component current = cnt.getComponentAt(iter);
            if(current instanceof com.codename1.ui.Container) {
                visitComponents((com.codename1.ui.Container)current, v);
            } else {
                v.visit(current);
            }
        }
    }
    
    private com.codename1.ui.Component findLowestVisibleComponent() {
        Form f = Display.getInstance().getCurrent();
        final com.codename1.ui.Component[] cmp = new com.codename1.ui.Component[1];
        visitComponents(f.getContentPane(), new ComponentVisitor() {
            int lowest = -1;
            
            public void visit(Component c) {
                int abY = c.getAbsoluteY();
                if(abY > lowest && abY + c.getHeight() < Display.getInstance().getDisplayHeight()) {
                    lowest = abY;
                    cmp[0] = c;
                }
            }
        });
        return cmp[0];
    }
    
    private com.codename1.ui.Component findHighestVisibleComponent() {
        Form f = Display.getInstance().getCurrent();
        final com.codename1.ui.Component[] cmp = new com.codename1.ui.Component[1];
        visitComponents(f.getContentPane(), new ComponentVisitor() {
            int highest = Display.getInstance().getDisplayHeight();
            
            public void visit(Component c) {
                int abY = c.getAbsoluteY();
                if(abY < highest && abY >= 0) {
                    highest = abY;
                    cmp[0] = c;
                }
            }
        });
        return cmp[0];
    }
    
    private void bindListListener(final com.codename1.ui.Component cmp, final ListModel m) {
        if(cmp.getClientProperty("CN1$listenerBound") == null) {
            cmp.putClientProperty("CN1$listenerBound", Boolean.TRUE);
            m.addSelectionListener(new SelectionListener() {
                public void selectionChanged(int oldSelected, int newSelected) {
                    generatedCode += "        selectInList(" + getPathOrName(cmp) + ", " + newSelected + ");\n";
                    updateTestCode();
                }
            });
        }
    }
    
    private boolean isListComponent(com.codename1.ui.Component cmp) {
        if(cmp instanceof com.codename1.ui.List) {
            if(cmp.getParent() instanceof com.codename1.ui.spinner.GenericSpinner) {
                cmp = cmp.getParent();
            } else {
                bindListListener(cmp, ((com.codename1.ui.List)cmp).getModel());
                return true;
            }
        }
        if(cmp instanceof com.codename1.ui.list.ContainerList) {
            bindListListener(cmp, ((com.codename1.ui.list.ContainerList)cmp).getModel());
            return true;
        }
        if(cmp instanceof com.codename1.ui.spinner.GenericSpinner) {
            bindListListener(cmp, ((com.codename1.ui.spinner.GenericSpinner)cmp).getModel());
            return true;
        }
        return false;
    }
    
    public boolean isToolbarComponent(Component cmp) {
        while(cmp.getParent() != null) {
            cmp = cmp.getParent();
            if(cmp instanceof Toolbar) {
                return true;
            }
        }
        return false;
    }
    
    void eventPointerReleased(int x, int y) {
        if(isRecording()) {
            com.codename1.ui.Component cmp = Display.getInstance().getCurrent().getComponentAt(x, y);
            if(isListComponent(cmp)) {
                return;
            }
            if(dragged) {
                if(dragToScroll.isSelected()) {
                    com.codename1.ui.Component scrollTo;
                    if(y > pointerPressedY) {
                        scrollTo = findLowestVisibleComponent();
                    } else {
                        scrollTo = findHighestVisibleComponent();
                    }
                    if(scrollTo != null && scrollTo != Display.getInstance().getCurrent() && scrollTo != Display.getInstance().getCurrent().getContentPane()) {
                        String name = scrollTo.getName();
                        if(name != null) {
                            generatedCode += "        ensureVisible(\"" + name + "\");\n";
                        } else {
                            String pp = getPathToComponent(scrollTo);
                            if(pp == null) {
                                return;
                            }
                            generatedCode += "        ensureVisible(" + pp + ");\n";
                        }
                        updateTestCode();
                    }
                } else {
                    addWaitStatement();
                    generatedCode += "        pointerRelease" + generatePointerEventArguments(x, y);
                    updateTestCode();
                }
            } else {
                if(isToolbarComponent(cmp)) {
                    if(cmp instanceof com.codename1.ui.Button) {
                        Command cmd = ((com.codename1.ui.Button)cmp).getCommand();
                        if(cmd != null) {
                            int offset = 0;
                            Command[] commands = TestUtils.getToolbarCommands();
                            for(Command c : commands) {
                                if(c == cmd) {
                                    generatedCode += "        assertEqual(getToolbarCommands().length, " + commands.length + ");\n";
                                    generatedCode += "        executeToolbarCommandAtOffset(" + offset + ");\n";
                                    updateTestCode();
                                    return;
                                }
                                offset++;
                            }
                        } else {
                            if(cmp.getUIID().equals("MenuButton")) {
                                // side menu button
                                generatedCode += "        showSidemenu();\n";
                                updateTestCode();
                                return;
                            }
                        }
                    }
                }
                
                if(cmp instanceof com.codename1.ui.Button) {
                    com.codename1.ui.Button btn = (com.codename1.ui.Button)cmp;
                    
                    // special case for back command on iOS
                    if(btn.getCommand() != null && btn.getCommand() == Display.getInstance().getCurrent().getBackCommand()) {
                        generatedCode += "        goBack();\n";
                    } else {
                        if(btn.getName() != null && btn.getName().length() > 0) {
                            generatedCode += "        clickButtonByName(\"" + btn.getName() + "\");\n";
                        } else {
                            if(btn.getText() != null && btn.getText().length() > 0) {
                                generatedCode += "        clickButtonByLabel(\"" + btn.getText() + "\");\n";
                            } else {
                                String pp = getPathToComponent(cmp);
                                if(pp == null || pp.equals("(String)null")) {
                                    return;
                                }
                                generatedCode += "        clickButtonByPath(" + pp + ");\n";
                            }
                        }
                    }
                    updateTestCode();
                    return;
                }
                if(cmp instanceof com.codename1.ui.TextArea) {
                    // ignore this, its probably initiating edit which we will capture soon
                    return;
                }
                generatedCode += "        pointerPress" + generatePointerEventArguments(pointerPressedX, pointerPressedY);
                addWaitStatement();
                generatedCode += "        pointerRelease" + generatePointerEventArguments(x, y);
                updateTestCode();
            }
        }
    }

    private String getPathOrName(com.codename1.ui.Component cmp) {
        if(cmp.getName() == null || TestUtils.findByName(cmp.getName()) != cmp) {
            return getPathToComponent(cmp);
        }
        return "\"" + cmp.getName() + "\"";
        
    }
    
    void editTextFieldCompleted(com.codename1.ui.Component cmp, String text) {
        generatedCode += "        setText(" + getPathOrName(cmp) + ", \"" + text + "\");\n";
    }
    
    private void bindForm(Form current) {
        currentForm = current;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jToolBar1 = new javax.swing.JToolBar();
        recording = new javax.swing.JToggleButton();
        saveRecording = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        script = new javax.swing.JTextArea();
        jLabel1 = new javax.swing.JLabel();
        testName = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        testsPackage = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        assertTitle = new javax.swing.JButton();
        assertLabels = new javax.swing.JButton();
        assertTextAreas = new javax.swing.JButton();
        screenshotTest = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        dragToScroll = new javax.swing.JCheckBox();

        FormListener formListener = new FormListener();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Test Recorder");

        jToolBar1.setRollover(true);

        recording.setIcon(new javax.swing.ImageIcon(getClass().getResource("/realvista_videoproduction_record_48.png"))); // NOI18N
        recording.setToolTipText("Start/pause recording");
        recording.setFocusable(false);
        recording.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        recording.setIconTextGap(0);
        recording.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/realvista_videoproduction_pause_48.png"))); // NOI18N
        recording.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        recording.addActionListener(formListener);
        jToolBar1.add(recording);

        saveRecording.setIcon(new javax.swing.ImageIcon(getClass().getResource("/realvista_computergadgets_floppy_disk_48.png"))); // NOI18N
        saveRecording.setBorder(null);
        saveRecording.setFocusable(false);
        saveRecording.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        saveRecording.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        saveRecording.addActionListener(formListener);
        jToolBar1.add(saveRecording);

        getContentPane().add(jToolBar1, java.awt.BorderLayout.PAGE_START);

        script.setColumns(20);
        script.setEditable(false);
        script.setRows(5);
        jScrollPane1.setViewportView(script);

        jLabel1.setText("Test Name");

        testName.setText("UnnamedTest");

        jLabel2.setText("Package");

        testsPackage.setText("tests");

        jLabel3.setText("Asserts");

        assertTitle.setText("Title");
        assertTitle.setToolTipText("Insert an assert statement to the value of the current form title");
        assertTitle.addActionListener(formListener);

        assertLabels.setText("Labels");
        assertLabels.setToolTipText("Insert assert statements with the value all the labels in the form");
        assertLabels.addActionListener(formListener);

        assertTextAreas.setText("Text Areas");
        assertTextAreas.setToolTipText("Insert assert statements with the values of all the text areas in the form");
        assertTextAreas.addActionListener(formListener);

        screenshotTest.setText("Screenshot");
        screenshotTest.setToolTipText("<html><body>\nGenerate an automated screenshot test for repeated tests<br>\nnotice that this might break since versions are never guaranteed<br>\nto be identical");
        screenshotTest.addActionListener(formListener);

        jLabel4.setText("Drag To Scroll");
        jLabel4.setToolTipText("<html><body>\nWhen checked drag operations are automatically converted to a scroll command<br />\nto issue verborse drag operations (to test features such as drag and drop) just<br />\nuncheck this flag");

        dragToScroll.setSelected(true);
        dragToScroll.setToolTipText("<html><body>\nWhen checked drag operations are automatically converted to a scroll command<br />\nto issue verborse drag operations (to test features such as drag and drop) just<br />\nuncheck this flag");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2)
                    .addComponent(jLabel3)
                    .addComponent(jLabel4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(dragToScroll)
                        .addGap(360, 360, 360))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(assertTextAreas)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(screenshotTest)
                        .addGap(158, 158, 158))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(assertTitle)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(assertLabels)
                        .addGap(223, 223, 223))
                    .addComponent(testsPackage, javax.swing.GroupLayout.DEFAULT_SIZE, 455, Short.MAX_VALUE)
                    .addComponent(testName, javax.swing.GroupLayout.DEFAULT_SIZE, 455, Short.MAX_VALUE))
                .addContainerGap())
            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 594, Short.MAX_VALUE))
        );

        jPanel1Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel1, jLabel2, jLabel3, jLabel4});

        jPanel1Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {assertLabels, assertTextAreas, assertTitle, screenshotTest});

        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(testName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(testsPackage, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(assertTitle)
                    .addComponent(assertLabels))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(assertTextAreas)
                    .addComponent(screenshotTest))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(dragToScroll)
                    .addComponent(jLabel4))
                .addContainerGap(240, Short.MAX_VALUE))
            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                    .addGap(168, 168, 168)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 231, Short.MAX_VALUE)))
        );

        getContentPane().add(jPanel1, java.awt.BorderLayout.CENTER);

        pack();
    }

    // Code for dispatching events from components to event handlers.

    private class FormListener implements java.awt.event.ActionListener {
        FormListener() {}
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            if (evt.getSource() == saveRecording) {
                TestRecorder.this.saveRecordingActionPerformed(evt);
            }
            else if (evt.getSource() == recording) {
                TestRecorder.this.recordingActionPerformed(evt);
            }
            else if (evt.getSource() == assertTitle) {
                TestRecorder.this.assertTitleActionPerformed(evt);
            }
            else if (evt.getSource() == assertLabels) {
                TestRecorder.this.assertLabelsActionPerformed(evt);
            }
            else if (evt.getSource() == assertTextAreas) {
                TestRecorder.this.assertTextAreasActionPerformed(evt);
            }
            else if (evt.getSource() == screenshotTest) {
                TestRecorder.this.screenshotTestActionPerformed(evt);
            }
        }
    }// </editor-fold>//GEN-END:initComponents

private void saveRecordingActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveRecordingActionPerformed
    updateTestCode();
    File test = new File("test");
    test.mkdirs();
    File tpack = new File(test, testsPackage.getText().replace('.', File.separatorChar));
    tpack.mkdirs();
    File testFile = new File(tpack, testName.getText() + ".java");
    if (testFile.exists()) {
        JOptionPane.showMessageDialog(this, "A test with the given name already exists", "Save", JOptionPane.ERROR_MESSAGE);
    } else {
            FileWriter fw = null;
            try {
                fw = new FileWriter(testFile);
                fw.write(script.getText());
                fw.close();
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "An error occured writing to the file: " + ex, "Save", JOptionPane.ERROR_MESSAGE);
            } finally {
                try {
                    fw.close();
                } catch (IOException ex) {
                }
            }
    }
}//GEN-LAST:event_saveRecordingActionPerformed

private void recordingActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_recordingActionPerformed
    if(isRecording()) {
        Form f = Display.getInstance().getCurrent();
        if(f.getName() != null && f.getTitle().length() > 0) {
            generatedCode += "        waitForFormName(\"" + f.getName() + "\");\n";
        } else {
            if(f.getTitle() != null && f.getTitle().length() > 0) {
                generatedCode += "        waitForFormTitle(\"" + f.getTitle() + "\");\n";
            }
        }
        updateTestCode();
    }
}//GEN-LAST:event_recordingActionPerformed

private void assertTitleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_assertTitleActionPerformed
    Form f = Display.getInstance().getCurrent();
    generatedCode += "        assertTitle(\"" + f.getTitle().replace("\n", "\\n") + "\");\n";    
    updateTestCode();    
}//GEN-LAST:event_assertTitleActionPerformed

private void assertLabelsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_assertLabelsActionPerformed
    Form f = Display.getInstance().getCurrent();
    visitComponents(f.getContentPane(), new ComponentVisitor() {
        public void visit(Component c) {
            if(c instanceof com.codename1.ui.Label) {
                com.codename1.ui.Label lbl = (com.codename1.ui.Label)c;
                String labelText = "null";
                if(lbl.getText() != null) {
                    labelText = "\"" + lbl.getText().replace("\n", "\\n") + "\"";
                }
                if(lbl.getName() != null) {
                    generatedCode += "        assertLabel(" + getPathOrName(lbl) + ", " + labelText + ");\n";
                } else {
                    generatedCode += "        assertLabel(" + labelText + ");\n";                    
                }
            }
        }
    });
    updateTestCode();    
}//GEN-LAST:event_assertLabelsActionPerformed

private void assertTextAreasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_assertTextAreasActionPerformed
    Form f = Display.getInstance().getCurrent();
    visitComponents(f.getContentPane(), new ComponentVisitor() {
        public void visit(Component c) {
            if(c instanceof com.codename1.ui.TextArea) {
                com.codename1.ui.TextArea lbl = (com.codename1.ui.TextArea)c;
                String labelText = "null";
                if(lbl.getText() != null) {
                    labelText = "\"" + lbl.getText().replace("\n", "\\n") + "\"";
                }
                if(lbl.getName() != null) {
                    generatedCode += "        assertTextArea(" + getPathOrName(lbl) + ", " + labelText + ");\n";
                } else {
                    generatedCode += "        assertTextArea(" + labelText + ");\n";
                }
            }
        }
    });
    updateTestCode();    
}//GEN-LAST:event_assertTextAreasActionPerformed

private void screenshotTestActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_screenshotTestActionPerformed
    screenshots++;
    generatedCode += "        screenshotTest(\"__TEST_NAME___" + screenshots + "\");\n";
    updateTestCode();    
}//GEN-LAST:event_screenshotTestActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton assertLabels;
    private javax.swing.JButton assertTextAreas;
    private javax.swing.JButton assertTitle;
    private javax.swing.JCheckBox dragToScroll;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JToggleButton recording;
    private javax.swing.JButton saveRecording;
    private javax.swing.JButton screenshotTest;
    private javax.swing.JTextArea script;
    private javax.swing.JTextField testName;
    private javax.swing.JTextField testsPackage;
    // End of variables declaration//GEN-END:variables
}
