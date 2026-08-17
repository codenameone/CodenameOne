/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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

import com.codename1.intents.AppEntity;
import com.codename1.intents.IntentCompletion;
import com.codename1.intents.IntentDeclaration;
import com.codename1.intents.IntentParameterInfo;
import com.codename1.intents.IntentParameterType;
import com.codename1.intents.IntentResult;
import com.codename1.intents.IntentSource;
import com.codename1.intents.Intents;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/// The simulator's Intents window: run any capability the application declares,
/// with the parameters the platform would supply, and see exactly what comes
/// back.
///
/// What makes this trustworthy is that it invents nothing. The list is
/// [Intents#getDeclarations()], which is the build-time generated table a device
/// reads. The parameter fields come from those declarations. Entity choices come
/// from the application's real `EntityQuery` methods, so disambiguation is
/// genuinely exercised rather than mimed. And Run calls
/// [Intents#dispatchInvocation], the same entry point the iOS and Android ports
/// call -- nothing here is a shortcut around the real path, so a bug in
/// marshalling, coercion or the deadline shows up here rather than on a device.
class SimulatorIntents {

    private static SimulatorIntents instance;

    private JFrame frame;
    private final JavaSEIntentBridge bridge;
    private final DefaultListModel intentModel = new DefaultListModel();
    private final JList intentList = new JList(intentModel);
    private final JPanel paramPanel = new JPanel(new GridBagLayout());
    private final JTextArea output = new JTextArea(8, 40);
    private final JCheckBox headless = new JCheckBox("Run headless (no UI on screen)", true);
    private final JComboBox sourceBox = new JComboBox(new DefaultComboBoxModel(new String[]{
            "VOICE", "SPOTLIGHT", "SHORTCUT", "WIDGET", "IN_APP"}));
    private final Map<String, JComponent0> editors = new LinkedHashMap<String, JComponent0>();
    private List<IntentDeclaration> declarations = new ArrayList<IntentDeclaration>();

    private SimulatorIntents(JavaSEIntentBridge bridge) {
        this.bridge = bridge;
    }

    static void showWindow(JavaSEIntentBridge bridge, JFrame owner) {
        if (instance == null) {
            instance = new SimulatorIntents(bridge);
        }
        instance.open(owner);
    }

    private void open(JFrame owner) {
        if (frame != null) {
            frame.setVisible(true);
            frame.toFront();
            refresh();
            return;
        }
        frame = new JFrame("Intents");
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

        intentList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        intentList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    buildParameterForm();
                }
            }
        });

        JPanel right = new JPanel(new BorderLayout(4, 4));
        right.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel options = new JPanel();
        options.setLayout(new BoxLayout(options, BoxLayout.Y_AXIS));
        options.add(headless);
        JPanel sourceRow = new JPanel(new BorderLayout(4, 0));
        sourceRow.add(new JLabel("Invoked by:"), BorderLayout.WEST);
        sourceRow.add(sourceBox, BorderLayout.CENTER);
        options.add(sourceRow);

        JPanel top = new JPanel(new BorderLayout(4, 4));
        top.add(new JScrollPane(paramPanel), BorderLayout.CENTER);
        top.add(options, BorderLayout.SOUTH);

        JButton run = new JButton("Run");
        run.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                run();
            }
        });
        JButton donate = new JButton("Donate");
        donate.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                IntentDeclaration d = selected();
                if (d != null) {
                    Intents.donate(d.getId(), collectParameters(d));
                    append("Donated " + d.getId());
                }
            }
        });
        JPanel buttons = new JPanel();
        buttons.add(donate);
        buttons.add(run);

        output.setEditable(false);
        JPanel bottom = new JPanel(new BorderLayout(4, 4));
        bottom.add(buttons, BorderLayout.NORTH);
        bottom.add(new JScrollPane(output), BorderLayout.CENTER);

        right.add(top, BorderLayout.CENTER);
        right.add(bottom, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(intentList), right);
        split.setDividerLocation(220);
        frame.getContentPane().add(split, BorderLayout.CENTER);
        frame.setPreferredSize(new Dimension(760, 520));
        frame.pack();
        if (owner != null) {
            frame.setLocationRelativeTo(owner);
        }
        refresh();
        frame.setVisible(true);
    }

    private void refresh() {
        declarations = new ArrayList<IntentDeclaration>(Intents.getDeclarations());
        intentModel.clear();
        for (IntentDeclaration d : declarations) {
            intentModel.addElement(d.getTitle() + "  (" + d.getId() + ")");
        }
        if (declarations.isEmpty()) {
            output.setText("This application declares no intents.\n\n"
                    + "Declare one with @AppIntent on a public static method and rebuild;\n"
                    + "the list is the build-time generated table, so it shows exactly what\n"
                    + "would ship.");
        } else {
            intentList.setSelectedIndex(0);
        }
    }

    private IntentDeclaration selected() {
        int i = intentList.getSelectedIndex();
        return i < 0 || i >= declarations.size() ? null : declarations.get(i);
    }

    private void buildParameterForm() {
        paramPanel.removeAll();
        editors.clear();
        IntentDeclaration d = selected();
        if (d == null) {
            paramPanel.revalidate();
            paramPanel.repaint();
            return;
        }
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 3, 3, 3);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        int row = 0;

        c.gridx = 0;
        c.gridy = row++;
        c.gridwidth = 2;
        c.weightx = 1;
        StringBuilder header = new StringBuilder("<html><b>").append(d.getTitle()).append("</b>");
        if (d.getDescription().length() > 0) {
            header.append("<br>").append(d.getDescription());
        }
        if (!d.getPhrases().isEmpty()) {
            header.append("<br><i>").append(d.getPhrases().get(0)).append("</i>");
        }
        if (d.isDestructive()) {
            header.append("<br><b>destructive</b> — the platform confirms before running this");
        }
        paramPanel.add(new JLabel(header.append("</html>").toString()), c);
        c.gridwidth = 1;

        for (IntentParameterInfo p : d.getParameters()) {
            c.gridx = 0;
            c.gridy = row;
            c.weightx = 0;
            paramPanel.add(new JLabel(p.getTitle() + (p.isRequired() ? " *" : "")), c);
            c.gridx = 1;
            c.weightx = 1;
            JComponent0 editor = editorFor(p);
            editors.put(p.getName(), editor);
            paramPanel.add(editor.component(), c);
            row++;
        }
        c.gridx = 0;
        c.gridy = row;
        c.weighty = 1;
        paramPanel.add(Box.createGlue(), c);
        paramPanel.revalidate();
        paramPanel.repaint();
    }

    /// Builds the right control for a parameter.
    ///
    /// An entity parameter becomes a picker filled from the application's real
    /// `SUGGESTED` query, because that is precisely what the platform does
    /// before it calls a handler -- mocking it here would hide the case where
    /// the query is missing, slow, or returns nothing.
    private JComponent0 editorFor(IntentParameterInfo p) {
        if (p.getType() == IntentParameterType.ENTITY) {
            List<AppEntity> options = Intents.queryEntities(p.getEntityType(), "suggested", null);
            if (options.isEmpty()) {
                JTextField field = new JTextField();
                field.setToolTipText("No suggested entities: the " + p.getEntityType()
                        + " type declares no SUGGESTED query, or it returned nothing."
                        + " Type an id instead.");
                return new TextEditor(field);
            }
            String[] labels = new String[options.size()];
            String[] ids = new String[options.size()];
            for (int i = 0; i < options.size(); i++) {
                AppEntity e = options.get(i);
                labels[i] = e.getTitle() == null ? e.getId() : e.getTitle() + "  (" + e.getId() + ")";
                ids[i] = e.getId();
            }
            return new ChoiceEditor(new JComboBox(new DefaultComboBoxModel(labels)), ids);
        }
        if (!p.getOptions().isEmpty()) {
            List<String> choices = new ArrayList<String>();
            boolean omittable = !p.isRequired()
                    && (p.getDefaultValue() == null || p.getDefaultValue().length() == 0);
            if (omittable) {
                // A combo always has a selection, so without this the form submits the first
                // option and the window quietly exercises a request a device might never send:
                // an optional parameter with no default can simply be absent, and then the
                // handler receives null. The blank entry is what "absent" looks like here.
                choices.add("");
            }
            choices.addAll(p.getOptions());
            String[] opts = choices.toArray(new String[choices.size()]);
            return new ChoiceEditor(new JComboBox(new DefaultComboBoxModel(opts)), opts);
        }
        if (p.getType() == IntentParameterType.BOOLEAN) {
            // Seeded from the declaration. An unchecked box always submits false, and because
            // the key is then present the generated coercion never applies the declared
            // default -- so pressing Run without touching the form exercised the opposite value
            // from the same invocation on a device, which is the one thing this window exists
            // to reproduce faithfully. Read the same way the runtime reads it.
            return new BooleanEditor(new JCheckBox("", isTrue(p.getDefaultValue())));
        }
        JTextField field = new JTextField(
                p.getDefaultValue() == null ? "" : p.getDefaultValue());
        return new TextEditor(field);
    }

    /// A declared boolean default, read the way the generated coercion reads a supplied one.
    private static boolean isTrue(String declared) {
        String v = declared == null ? "" : declared.trim();
        return "true".equalsIgnoreCase(v) || "1".equals(v);
    }

    private Map<String, Object> collectParameters(IntentDeclaration d) {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        for (IntentParameterInfo p : d.getParameters()) {
            JComponent0 editor = editors.get(p.getName());
            if (editor == null) {
                continue;
            }
            Object value = editor.value();
            if (value != null && !"".equals(value)) {
                params.put(p.getName(), value);
            }
        }
        return params;
    }

    private void run() {
        final IntentDeclaration d = selected();
        if (d == null) {
            return;
        }
        Map<String, Object> params = collectParameters(d);
        for (IntentParameterInfo p : d.getParameters()) {
            if (p.isRequired() && !params.containsKey(p.getName())) {
                append("Missing required parameter \"" + p.getName() + "\".\n"
                        + "On a device the platform would ask: " + p.getTitle());
                return;
            }
        }
        output.setText("Running " + d.getId() + "…\n");
        IntentSource source = IntentSource.valueOf((String) sourceBox.getSelectedItem());
        Intents.dispatchInvocation(d.getId(), params, source, headless.isSelected(),
                new IntentCompletion() {
                    public void onIntentResult(final IntentResult result) {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                append(describe(result));
                            }
                        });
                    }
                });
    }

    private static String describe(IntentResult r) {
        if (r == null) {
            return "no result";
        }
        StringBuilder sb = new StringBuilder();
        if (r.isFailed()) {
            sb.append("FAILED: ").append(r.getErrorMessage());
            return sb.toString();
        }
        sb.append("OK");
        if (r.getValue() != null) {
            sb.append("\nvalue:   ").append(r.getValue());
        }
        if (r.getDialog() != null) {
            sb.append("\nspoken:  ").append(r.getDialog());
        }
        if (r.getOpenUrl() != null) {
            sb.append("\nopens:   ").append(r.getOpenUrl());
        }
        if (r.getEntity() != null) {
            sb.append("\nentity:  ").append(r.getEntity());
        }
        if (r.getSnippet() != null) {
            sb.append("\nsnippet: present (rendered natively on a device)");
        }
        return sb.toString();
    }

    private void append(String text) {
        output.append(text);
        output.append("\n");
    }

    // ------------------------------------------------------------------
    // Tiny editor abstraction, so collectParameters does not switch on widgets
    // ------------------------------------------------------------------

    private interface JComponent0 {
        javax.swing.JComponent component();

        Object value();
    }

    private static final class TextEditor implements JComponent0 {
        private final JTextField field;

        TextEditor(JTextField field) {
            this.field = field;
        }

        public javax.swing.JComponent component() {
            return field;
        }

        public Object value() {
            return field.getText();
        }
    }

    private static final class BooleanEditor implements JComponent0 {
        private final JCheckBox box;

        BooleanEditor(JCheckBox box) {
            this.box = box;
        }

        public javax.swing.JComponent component() {
            return box;
        }

        public Object value() {
            return Boolean.valueOf(box.isSelected());
        }
    }

    /// A combo whose visible labels and submitted values differ, which is what an
    /// entity picker needs: the user reads a title, the handler receives an id.
    private static final class ChoiceEditor implements JComponent0 {
        private final JComboBox box;
        private final String[] values;

        ChoiceEditor(JComboBox box, String[] values) {
            this.box = box;
            this.values = values;
        }

        public javax.swing.JComponent component() {
            return box;
        }

        public Object value() {
            int i = box.getSelectedIndex();
            return i < 0 || i >= values.length ? null : values[i];
        }
    }
}
