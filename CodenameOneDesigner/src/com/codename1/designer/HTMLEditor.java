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

import com.codename1.ui.resource.util.CodenameOneComponentWrapper;
import com.codename1.ui.util.EditableResources;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.TextAction;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

/**
 * Tool allowing editing of HTML, this is leveraged by the localization and the
 * UI builder code.
 *
 * @author Shai Almog
 */
public class HTMLEditor extends javax.swing.JPanel {
    private com.codename1.ui.html.HTMLComponent htmlComponent;
    private EditableResources res;
    /** Creates new form HTMLEditor */
    public HTMLEditor(EditableResources res, String htmlText) {
        initComponents();
        this.res = res;
        htmlComponent = new com.codename1.ui.html.HTMLComponent();
        htmlComponent.setBodyText(htmlText, "UTF-8");
        final CodenameOneComponentWrapper wrapper = new CodenameOneComponentWrapper(htmlComponent);
        uiPreview.add(java.awt.BorderLayout.CENTER, wrapper);
        wysiwyg.setText(htmlText);
        source.setText(htmlText);
        Listener l = new Listener();
        wysiwyg.getDocument().addDocumentListener(l);
        source.getDocument().addDocumentListener(l);
        JButton b = jToolBar1.add(new StyledEditorKit.BoldAction());
        b.setText("<html><body><b>B</b></body></html>");
        JButton i = jToolBar1.add(new StyledEditorKit.ItalicAction());
        i.setText("<html><body><i>I</i></body></html>");
        JButton u = jToolBar1.add(new StyledEditorKit.UnderlineAction());
        u.setText("<html><body><u>U</u></body></html>");
        jToolBar1.add(new InsertImageAction());
    }

    class InsertImageAction extends TextAction {

        /**
         * Creates this object with the appropriate identifier.
         */
        public InsertImageAction() {
            super("Image");
        }

        /**
         * The operation to perform when this action is triggered.
         *
         * @param e the action event
         */
        public void actionPerformed(ActionEvent e) {
            JTextComponent target = getTextComponent(e);
            if ((target != null) && (e != null)) {
                String[] temp = res.getImageResourceNames();
                Arrays.sort(temp, String.CASE_INSENSITIVE_ORDER);
                JComboBox jc = new JComboBox(temp);
                final com.codename1.ui.Image img = res.getImage((String)jc.getSelectedItem());
                JOptionPane.showMessageDialog(HTMLEditor.this, jc, "Pick", JOptionPane.PLAIN_MESSAGE);
                if ((! target.isEditable()) || (! target.isEnabled()) && img != null) {
                    UIManager.getLookAndFeel().provideErrorFeedback(target);
                    return;
                }
                try {
                    ((HTMLEditorKit)wysiwyg.getEditorKit()).insertHTML((HTMLDocument)wysiwyg.getDocument(),
                            wysiwyg.getCaret().getDot(), "<img width=\"" + img.getWidth() + "\" height=\"" + img.getHeight() + "\" src=\"local://"  + jc.getSelectedItem()+ "\" />", 0, 0,
                            HTML.Tag.IMG);
                    //target.getDocument().insertString(target.getSelectionStart(), "<img src=\"local://" + jc.getSelectedItem() + "\" />", null);
                    //target.replaceSelection("<img src=\"local://" + jc.getSelectedItem() + "\" />");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }



    public String getResult() {
        return source.getText();
    }

    class Listener implements DocumentListener {
        private JTextComponent lock;
        public void insertUpdate(DocumentEvent e) {
            update(e);
        }

        public void removeUpdate(DocumentEvent e) {
            update(e);
        }

        public void changedUpdate(DocumentEvent e) {
            update(e);
        }

        private void update(DocumentEvent e) {
            if(lock == null) {
                if(e.getDocument() == source.getDocument()) {
                    lock = source;
                    wysiwyg.setText(source.getText());
                } else {
                    lock = wysiwyg;
                    source.setText(wysiwyg.getText());
                }
                htmlComponent.setBodyText(source.getText(), "UTF-8");
                uiPreview.repaint();
                lock = null;
            }
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

        jSplitPane1 = new javax.swing.JSplitPane();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        wysiwyg = new javax.swing.JTextPane();
        jToolBar1 = new javax.swing.JToolBar();
        jScrollPane2 = new javax.swing.JScrollPane();
        source = new javax.swing.JTextArea();
        uiPreview = new javax.swing.JPanel();

        setLayout(new java.awt.BorderLayout());

        jSplitPane1.setResizeWeight(0.5);
        jSplitPane1.setName("jSplitPane1"); // NOI18N

        jTabbedPane1.setName("jTabbedPane1"); // NOI18N

        jPanel1.setName("jPanel1"); // NOI18N
        jPanel1.setLayout(new java.awt.BorderLayout());

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        wysiwyg.setContentType("text/html");
        wysiwyg.setName("wysiwyg"); // NOI18N
        jScrollPane1.setViewportView(wysiwyg);

        jPanel1.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        jToolBar1.setRollover(true);
        jToolBar1.setName("jToolBar1"); // NOI18N
        jPanel1.add(jToolBar1, java.awt.BorderLayout.PAGE_START);

        jTabbedPane1.addTab("HTML", jPanel1);

        jScrollPane2.setName("jScrollPane2"); // NOI18N

        source.setColumns(20);
        source.setRows(5);
        source.setName("source"); // NOI18N
        jScrollPane2.setViewportView(source);

        jTabbedPane1.addTab("Source", jScrollPane2);

        jSplitPane1.setLeftComponent(jTabbedPane1);

        uiPreview.setName("uiPreview"); // NOI18N
        uiPreview.setLayout(new java.awt.BorderLayout());
        jSplitPane1.setRightComponent(uiPreview);

        add(jSplitPane1, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JTextArea source;
    private javax.swing.JPanel uiPreview;
    private javax.swing.JTextPane wysiwyg;
    // End of variables declaration//GEN-END:variables

}
