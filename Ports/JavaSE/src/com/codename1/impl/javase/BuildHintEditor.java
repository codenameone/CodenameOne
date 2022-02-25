package com.codename1.impl.javase;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URI;
import java.util.*;
import java.util.List;

/**
 * A GUI editor for editing build hints.  This includes support for cn1libs to tie into it at runtime.
 *
 */
public class BuildHintEditor {
    private final JavaSEPort javaSEPort;
    private JFrame frame;
    private boolean modified;
    private final Map<String,ArrayList<Runnable>> propertyChangeListeners = new HashMap<String,ArrayList<Runnable>>();

    private final LinkedHashMap<String,BuildHintModel> buildHintModels = new LinkedHashMap<String,BuildHintModel>();
    private final LinkedHashMap<String,BuildHintGroupModel> buildHintGroupModels = new LinkedHashMap<String, BuildHintGroupModel>();

    private final Map<String,String> projectBuildHints = new HashMap<String,String>();

    public BuildHintEditor(JavaSEPort javaSEPort) {
        this.javaSEPort = javaSEPort;
    }

    private enum BuildHintValueType {
        TextField,
        TextArea,
        Checkbox,
        Select
    }

    private class BuildHintModel {
        private String name="", label="", description="", hint="";
        private BuildHintValueType type = BuildHintValueType.TextField;
        private boolean required;
        private String[] values;
        private String group;
        private String linkUrl, linkLabel;
    }

    private class BuildHintGroupModel {
        private String name = "", label = "", description = "";
        private LinkedHashMap<String,BuildHintModel> hints = new LinkedHashMap<String,BuildHintModel>();
    }

    private class BuildHintFields {
        private JTextField value;
        private JLabel label, description;
    }



    private void onPropertyChange(String hintName, Runnable r) {
        ArrayList<Runnable> listeners = propertyChangeListeners.get(hintName);
        if (listeners == null) {
            listeners = new ArrayList<Runnable>();
            propertyChangeListeners.put(hintName, listeners);
        }
        listeners.add(r);
    }

    private void loadBuildHintModels() {
        clearModified();

        buildHintModels.clear();
        buildHintGroupModels.clear();
        projectBuildHints.clear();
        projectBuildHints.putAll(javaSEPort.getProjectBuildHints());
        Properties props = System.getProperties();

        for (String propName : props.stringPropertyNames()) {
            if (propName.startsWith("codename1.arg.{{") && propName.contains("}}.")) {
                String hintName = propName.substring(propName.indexOf("{{")+2, propName.indexOf("}}.")).trim();


                String hintPropertyName = propName.substring(propName.indexOf("}}.")+3).trim();
                String propertyValue = props.getProperty(propName);

                if (hintName.startsWith("@")) {
                    // This is a group property
                    BuildHintGroupModel groupModel = buildHintGroupModels.get(hintName.substring(1));
                    if (groupModel == null) {
                        groupModel = new BuildHintGroupModel();
                        groupModel.name = hintName.substring(1);
                        groupModel.label = groupModel.name;
                        buildHintGroupModels.put(groupModel.name, groupModel);

                    }
                    if ("label".equals(hintPropertyName)) {
                        groupModel.label = propertyValue;
                    } else if ("description".equals(hintPropertyName)) {
                        groupModel.description = propertyValue;
                    }
                    continue;

                }
                BuildHintModel model;
                if (hintName.startsWith("#") && hintName.substring(1).contains("#")) {
                    String groupName = hintName.substring(1, hintName.indexOf("#", 1));
                    hintName = hintName.substring(hintName.indexOf("#", 1)+1);
                    BuildHintGroupModel groupModel = buildHintGroupModels.get(groupName);
                    if (groupModel == null) {
                        groupModel = new BuildHintGroupModel();
                        groupModel.name = groupName;
                        groupModel.label = groupModel.name;
                        buildHintGroupModels.put(groupModel.name, groupModel);
                    }

                    model = groupModel.hints.get(hintName);
                    if (model == null) {
                        model = new BuildHintModel();
                        model.name = hintName;
                        model.label = hintName;
                        groupModel.hints.put(hintName, model);
                    }
                } else {
                    model = buildHintModels.get(hintName);
                    if (model == null) {
                        model = new BuildHintModel();
                        model.name = hintName;
                        model.label = hintName;

                        buildHintModels.put(hintName, model);
                    }
                }

                if ("description".equals(hintPropertyName)) {
                    model.description = propertyValue;
                } else if ("label".equals(hintPropertyName)) {
                    model.label = propertyValue;
                } else if ("link".equals(hintPropertyName)) {
                    model.linkUrl = propertyValue;
                    model.linkLabel = "Learn more";
                    if (propertyValue.contains(" ")) {
                        model.linkUrl = propertyValue.substring(0, propertyValue.indexOf(" "));
                        model.linkLabel = propertyValue.substring(propertyValue.indexOf(" ")+1);

                    }
                } else if ("hint".equals(hintPropertyName)) {
                    model.hint = propertyValue;
                } else if ("required".equals(hintPropertyName)) {
                    model.required = "true".equalsIgnoreCase(propertyValue);
                } else if ("type".equals(hintPropertyName)) {
                    if ("textfield".equalsIgnoreCase(propertyValue)) {
                        model.type = BuildHintValueType.TextField;
                    } else if ("textarea".equalsIgnoreCase(propertyValue)) {
                        model.type = BuildHintValueType.TextArea;
                    } else if ("checkbox".equalsIgnoreCase(propertyValue)) {
                        model.type = BuildHintValueType.Checkbox;
                    } else if ("select".equalsIgnoreCase(propertyValue)) {
                        model.type = BuildHintValueType.Select;
                        String valuesString = System.getProperty("codename1.arg.{{ "+model.name+" }}.values");
                        if (valuesString != null) {
                            String separator = ""+valuesString.charAt(valuesString.length()-1);
                            ArrayList<String> values = new ArrayList<String>();
                            values.add("");
                            for (String value : valuesString.split(separator)) {
                                if (!value.trim().isEmpty()) {
                                    values.add(value);
                                }
                            }

                            model.values = values.toArray(new String[values.size()]);
                        }
                    }
                } else if ("group".equals(hintPropertyName)) {
                    BuildHintGroupModel groupModel = buildHintGroupModels.get(propertyValue);
                    model.group = hintPropertyName;
                    if (groupModel == null) {
                        groupModel = new BuildHintGroupModel();
                        groupModel.name = propertyValue;
                        groupModel.label = propertyValue;
                        buildHintGroupModels.put(groupModel.name, groupModel);
                    }
                    groupModel.hints.put(model.name, model);

                }

            }
        }

        for (String hint : projectBuildHints.keySet()) {
            BuildHintModel model = buildHintModels.get(hint);
            if (model == null) {
                model = new BuildHintModel();
                model.name = hint;
                model.label = hint;
                buildHintModels.put(hint, model);
            }
        }

    }

    private void setModified(String hint) {
        modified = true;
        ArrayList<Runnable> listeners = propertyChangeListeners.get(hint);
        if (listeners != null && !listeners.isEmpty()) {
            for (Runnable r : listeners) {
                r.run();
            }
        }
    }

    private void clearModified() {
        modified = false;
    }

    private List<BuildHintModel> getGrouplessBuildHints() {
        ArrayList<BuildHintModel> out = new ArrayList<BuildHintModel>();
        for (BuildHintModel buildHintModel : buildHintModels.values()) {
            if (buildHintModel.group == null || buildHintModel.group.isEmpty()) {
                out.add(buildHintModel);
            }
        }
        return out;
    }

    private JComponent left(Component... cmps) {
        JPanel p = new JPanel();
        decorateContainer(p);
        p.setLayout(new FlowLayout(FlowLayout.LEFT));
        for (Component cmp : cmps) {
            p.add(cmp);
        }
        return p;
    }

    private JComponent leftNoGrow(Component... cmps) {
        JPanel p = new JPanel() {
            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        decorateContainer(p);
        p.setLayout(new FlowLayout(FlowLayout.LEFT));
        for (Component cmp : cmps) {
            p.add(cmp);
        }

        return p;
    }

    private JComponent wrapRigid(Component cmp) {
        JPanel p = new JPanel() {
            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        decorateContainer(p);
        p.setLayout(new BorderLayout());
        p.add(cmp, BorderLayout.CENTER);

        return p;
    }



    private static void addChangeListenerTo(JTextComponent textField, Runnable r) {
        textField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                r.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                r.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                r.run();
            }
        });
    }

    private JComponent createHintView(final BuildHintModel model) {
        JPanel cnt = new JPanel();
        cnt.setLayout(new BorderLayout());
        JLabel label = new JLabel(model.label);
        Font labelFont = label.getFont();
        labelFont = labelFont.deriveFont(Font.BOLD);
        label.setFont(labelFont);
        cnt.add(label, BorderLayout.NORTH);
        JTextArea description = null;
        if (!model.description.isEmpty()) {
            //description = new JLabel("<html><p style='width:480px; font-size:10px'>"+model.description+"</p></html>");
            description = new JTextArea(model.description);
            description.setOpaque(false);
            description.setEditable(false);
            description.setBorder(null);
            description.setLineWrap(true);
            description.setWrapStyleWord(true);
            description.setFont(UIManager.getFont("Label.font"));
            Font descriptionFont = description.getFont();
            descriptionFont = descriptionFont.deriveFont(descriptionFont.getSize2D() * 0.75f);
            description.setFont(descriptionFont);
            description.setForeground(Color.darkGray);
            description.setBackground(UIManager.getColor("Label.background"));
            description.setAlignmentX(Component.LEFT_ALIGNMENT);
            description.setPreferredSize(new Dimension(400, 30));
            description.setMaximumSize(new Dimension(400, 200));
            //description.setPreferredSize(new Dimension(480, description.getPreferredSize().height));

        }

        JPanel examplePanel = new JPanel();
        decorateContainer(examplePanel);
        examplePanel.setLayout(new BorderLayout());
        JPanel descriptionPanel = new JPanel();
        descriptionPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        decorateContainer(descriptionPanel);
        examplePanel.add(descriptionPanel, BorderLayout.NORTH);
        if (description != null) {
            descriptionPanel.add(description);
        }

        if (model.linkLabel != null && model.linkUrl != null && !model.linkLabel.isEmpty() && !model.linkUrl.isEmpty()) {
            JButton link = new JButton(model.linkLabel);
            ActionListener l = new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        if (Desktop.isDesktopSupported()) {
                            Desktop.getDesktop().browse(new URI(model.linkUrl));
                        } else {
                            JOptionPane.showMessageDialog(frame, "Opening system web browser not supported on this platform.");
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(frame, "Failed to open page.  "+ex.getMessage(), "Failed", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            link.addActionListener(l);
            descriptionPanel.add(link);

        }
        if (model.hint != null && !model.hint.isEmpty()) {

            JPanel innerExamplePanel = new JPanel();
            decorateContainer(innerExamplePanel);
            innerExamplePanel.setLayout(new BorderLayout());
            innerExamplePanel.setBorder(new TitledBorder("Example"));

            final JTextArea example = new JTextArea(model.hint);
            example.setEditable(false);
            example.setBorder(new LineBorder(Color.gray));
            example.setWrapStyleWord(true);
            example.setLineWrap(true);
            example.setForeground(Color.darkGray);
            example.setBackground(new Color(0xff, 0xff, 0xff, 0xcc));

            Font defaultFont = UIManager.getFont("Label.font");
            defaultFont = defaultFont.deriveFont(defaultFont.getSize2D() * 0.75f);
            example.setFont(defaultFont);
            example.setColumns(30);
            example.setPreferredSize(new Dimension(400, 30));
            example.setMaximumSize(new Dimension(400, 200));

            innerExamplePanel.add(left(example), BorderLayout.CENTER);

            JButton copy = new JButton("Copy");
            ActionListener l = new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                            .setContents(new java.awt.datatransfer.StringSelection(example.getText()), null);
                }
            };
            copy.addActionListener(l);
            copy.putClientProperty("JComponent.sizeVariant", "mini");
            innerExamplePanel.add(copy, BorderLayout.EAST);
            examplePanel.add(innerExamplePanel, BorderLayout.CENTER);


        }

        cnt.add(examplePanel, BorderLayout.SOUTH);
        JComponent valueComponent;

        switch (model.type) {
            case TextField: {
                final JTextField textField = new JTextField();
                textField.setColumns(30);

                //textField.setMinimumSize(new Dimension(textField.getPreferredSize().width,30));
                textField.setText("");
                String currentValue = projectBuildHints.get(model.name);

                if (currentValue == null) {
                    //textField.setText(model.hint);
                } else {
                    textField.setText(currentValue);
                }
                textField.setPreferredSize(new Dimension(100, 30));
                textField.setMaximumSize(new Dimension(200, 30));
                Runnable l = new Runnable() {

                    @Override
                    public void run() {
                        projectBuildHints.put(model.name, textField.getText());
                        setModified(model.name);
                    }
                };
                addChangeListenerTo(textField, l);

                l = new Runnable() {

                    @Override
                    public void run() {
                        if (!Objects.equals(projectBuildHints.get(model.name), textField.getText())) {
                            textField.setText(projectBuildHints.get(model.name));
                        }
                    }
                };
                onPropertyChange(model.name, l);
                valueComponent = textField;
                break;

            }

            case TextArea: {
                final JTextArea textField = new JTextArea();
                String currentValue = projectBuildHints.get(model.name);
                if (currentValue == null) {
                    //textField.setText(model.hint);
                } else {
                    textField.setText(currentValue);
                }
                textField.setPreferredSize(new Dimension(100, 30));
                textField.setMaximumSize(new Dimension(200, 30));
                Runnable l = new Runnable() {

                    @Override
                    public void run() {
                        projectBuildHints.put(model.name, textField.getText());
                        setModified(model.name);
                    }
                };
                addChangeListenerTo(textField, l);
                l = new Runnable() {

                    @Override
                    public void run() {
                        if (!Objects.equals(projectBuildHints.get(model.name), textField.getText())) {
                            textField.setText(projectBuildHints.get(model.name));
                        }
                    }
                };
                onPropertyChange(model.name, l);
                valueComponent = textField;
                break;
            }

            case Checkbox: {
                final JCheckBox checkBox = new JCheckBox(model.label);
                if (projectBuildHints.containsKey(model.name) && "true".equals(projectBuildHints.get(model.name))) {
                    checkBox.setSelected(true);
                }

                checkBox.addChangeListener(new ChangeListener() {
                    @Override
                    public void stateChanged(ChangeEvent e) {
                        if (checkBox.isSelected()) {
                            projectBuildHints.put(model.name, "true");
                        } else {
                            projectBuildHints.put(model.name, "false");
                        }
                        setModified(model.name);
                    }
                });
                Runnable l = new Runnable() {
                    @Override
                    public void run() {
                        boolean newVal = "true".equals(projectBuildHints.get(model.name));
                        if (newVal != checkBox.isSelected()) {
                            checkBox.setSelected(newVal);
                        }
                    }
                };
                onPropertyChange(model.name, l);
                valueComponent = checkBox;
                break;

            }

            case Select: {
                if (model.values == null) {
                    model.values = new String[]{};
                }
                ArrayList<String> values = new ArrayList<String>(Arrays.asList(model.values));
                if (projectBuildHints.containsKey(model.name) && !values.contains(projectBuildHints.get(model.name))) {
                    values.add(projectBuildHints.get(model.name));
                }

                final JComboBox<String> comboBox = new JComboBox<String>(values.toArray(new String[values.size()]));
                if (projectBuildHints.containsKey(model.name)) {
                    comboBox.setSelectedItem(projectBuildHints.get(model.name));
                }
                ActionListener l = new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        projectBuildHints.put(model.name, (String)comboBox.getSelectedItem());
                        setModified(model.name);
                    }
                };
                comboBox.addActionListener(l);
                Runnable r = new Runnable() {

                    @Override
                    public void run() {
                        String newVal = projectBuildHints.get(model.name);
                        if (!Objects.equals(newVal, comboBox.getSelectedItem())) {
                            comboBox.setSelectedItem(newVal);
                        }
                    }
                };
                onPropertyChange(model.name, r);
                valueComponent = comboBox;
                break;
            }

            default: {
                throw new IllegalStateException("No type set for field "+model.name);
            }

        }

        cnt.add(valueComponent, BorderLayout.CENTER);
        decorateContainer(cnt);
        //cnt.setPreferredSize(new Dimension(480, cnt.getPreferredSize().height+20));
        //cnt.setMaximumSize(new Dimension(640, cnt.getPreferredSize().height+20));
        cnt.setBorder(new EmptyBorder(10, 10, 10,50));
        return cnt;

    }

    private JComponent decorateContainer(JComponent cmp) {
        cmp.setOpaque(false);
        return cmp;
    }

    private void buildUI() {
        propertyChangeListeners.clear();
        loadBuildHintModels();


        frame = new JFrame("Build Hint Editor");

        JTabbedPane tabs = new JTabbedPane();
        tabs.setOpaque(false);

        for (BuildHintGroupModel group : buildHintGroupModels.values()) {



            final ArrayList<JComponent> views = new ArrayList<JComponent>();
            final JPanel groupPanel = new JPanel();
            decorateContainer(groupPanel);
            groupPanel.setLayout(new BoxLayout(groupPanel, BoxLayout.Y_AXIS));

            if (group.description != null && !group.description.isEmpty()) {
                JLabel description = new JLabel("<html><p style='width:480px'>"+group.description+"</p></html>");
                groupPanel.add(left(description));
            }
            for (BuildHintModel hintModel : group.hints.values()) {
                JComponent view = createHintView(hintModel);
                groupPanel.add(wrapRigid(view));
                view.putClientProperty("keywords", hintModel.name+" "+hintModel.label+" "+hintModel.description);
                views.add(view);
            }
            groupPanel.add(decorateContainer((JComponent)Box.createVerticalGlue()));

            final JTextField filter = new JTextField();
            filter.putClientProperty("JTextField.variant", "search");
            Runnable l = new Runnable() {

                @Override
                public void run() {
                    String[] words = filter.getText().split(" ");
                    boolean noSearch = filter.getText().trim().isEmpty();
                    for (JComponent view : views) {
                        String keywords = (String)view.getClientProperty("keywords");
                        if (keywords == null) continue;
                        String lcKeywords = keywords.toLowerCase();

                        boolean found = noSearch;
                        if (!found) {
                            for (String word : words) {
                                if (word.trim().isEmpty()) continue;
                                if (lcKeywords.contains(word.toLowerCase())) {
                                    found = true;
                                    break;
                                }
                            }
                        }

                        view.setVisible(found);

                    }
                    groupPanel.revalidate();
                }
            };
            addChangeListenerTo(filter, l);

            JScrollPane groupScroller = new JScrollPane(groupPanel);
            decorateContainer(groupScroller);
            decorateContainer(groupScroller.getViewport());
            groupScroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

            JPanel groupWrapper = new JPanel();
            groupWrapper.setLayout(new BorderLayout());
            decorateContainer(groupWrapper);
            groupWrapper.add(groupScroller, BorderLayout.CENTER);
            groupWrapper.add(filter, BorderLayout.NORTH);
            tabs.add(group.label, groupWrapper);
        }


        final JPanel grouplessPanel = new JPanel();
        decorateContainer(grouplessPanel);
        grouplessPanel.setLayout(new BoxLayout(grouplessPanel, BoxLayout.Y_AXIS));
        final ArrayList<JComponent> grouplessViews = new ArrayList<JComponent>();

        for (BuildHintModel hintModel : getGrouplessBuildHints()) {
            JComponent view = createHintView(hintModel);
            grouplessPanel.add(wrapRigid(view));
            view.putClientProperty("keywords", hintModel.name+ " " + hintModel.label + " " + hintModel.description);
            grouplessViews.add(view);
        }
        grouplessPanel.add(decorateContainer((JComponent)Box.createVerticalGlue()));


        final JTextField filter = new JTextField();
        filter.putClientProperty("JTextField.variant", "search");
        Runnable l = new Runnable() {

            @Override
            public void run() {
                String[] words = filter.getText().split(" ");
                boolean noSearch = filter.getText().trim().isEmpty();
                for (JComponent view : grouplessViews) {
                    String keywords = (String)view.getClientProperty("keywords");
                    if (keywords == null) continue;
                    String lcKeywords = keywords.toLowerCase();

                    boolean found = noSearch;
                    if (!found) {
                        for (String word : words) {
                            if (word.trim().isEmpty()) continue;
                            if (lcKeywords.contains(word.toLowerCase())) {
                                found = true;
                                break;
                            }
                        }
                    }

                    view.setVisible(found);

                }
                grouplessPanel.revalidate();
            }
        };
        addChangeListenerTo(filter,l);

        JScrollPane grouplessScroller = new JScrollPane(grouplessPanel);
        decorateContainer(grouplessScroller.getViewport());
        decorateContainer(grouplessScroller);
        grouplessScroller.setBorder(new EmptyBorder(0,0,0,0));
        String grouplessLabel = buildHintGroupModels.isEmpty() ? "Build Hints" : "Other";

        JPanel grouplessWrapper = new JPanel();
        grouplessWrapper.setLayout(new BorderLayout());
        decorateContainer(grouplessWrapper);
        grouplessWrapper.add(grouplessScroller, BorderLayout.CENTER);
        grouplessWrapper.add(filter, BorderLayout.NORTH);
        tabs.add(grouplessLabel, grouplessWrapper);


        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(tabs, BorderLayout.CENTER);

        JButton cancel = new JButton("Cancel");
        JButton apply = new JButton("Apply");
        ActionListener actionListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                save();
            }
        };
        apply.addActionListener(actionListener);
        actionListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                close();
            }
        };
        cancel.addActionListener(actionListener);

        JButton save = new JButton("Save");
        actionListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                save();
                close();
            }
        };
        save.addActionListener(actionListener);

        JPanel buttons = new JPanel();
        buttons.setLayout(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancel);
        buttons.add(apply);
        buttons.add(save);

        frame.getContentPane().add(buttons, BorderLayout.SOUTH);

        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                close();
            }
        });



    }

    private void save() {
        Map<String,String> oldHints = javaSEPort.getProjectBuildHints();
        for (String hint : projectBuildHints.keySet()) {
            if (!oldHints.containsKey(hint) || !hint.equals(oldHints.get(hint))) {
                javaSEPort.setProjectBuildHint(hint, projectBuildHints.get(hint));
            }
        }
        clearModified();
    }

    private void close() {
        if (modified) {
            int result = JOptionPane.showConfirmDialog(frame, "Save changes to build hints before closing?", "Save Changes?", JOptionPane.YES_NO_CANCEL_OPTION);
            switch (result) {
                case JOptionPane.CANCEL_OPTION:
                    return;
                case JOptionPane.NO_OPTION:
                    frame.dispose();
                    break;
                case JOptionPane.YES_OPTION:
                    save();
                    frame.dispose();
                    break;
            }
        } else {
            frame.dispose();
        }
    }

    public void show() {
        buildUI();
        frame.getContentPane().setPreferredSize(new Dimension(640, 480));
        frame.pack();
        frame.setLocationRelativeTo(javaSEPort.canvas);
        frame.setVisible(true);
    }


}
