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
package com.codename1.desktopcompat.javax.swing;

import com.codename1.desktopcompat.java.awt.BorderLayout;
import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Container;
import com.codename1.desktopcompat.java.awt.Dialog;
import com.codename1.desktopcompat.java.awt.FontMetrics;
import com.codename1.desktopcompat.java.awt.Frame;
import com.codename1.desktopcompat.java.awt.Graphics;
import com.codename1.desktopcompat.java.awt.Window;
import com.codename1.desktopcompat.java.awt.event.ActionEvent;
import com.codename1.desktopcompat.java.awt.event.ActionListener;
import com.codename1.desktopcompat.java.awt.event.WindowAdapter;
import com.codename1.desktopcompat.java.awt.event.WindowEvent;
import com.codename1.desktopcompat.javax.accessibility.Accessible;

/// `javax.swing.JOptionPane`: the standard message, confirmation, input
/// and option dialogs.
///
/// Every `show...Dialog` method blocks its caller until the user answered,
/// as on the desktop, while events keep running. On a phone the dialog
/// floats over the current form at its packed size.
///
/// The pane lays itself out when [#createDialog(Component, String)] is
/// called: the message to the right of the icon of the message type, the
/// text field or the combo box of an input dialog below the message, and a
/// row of buttons at the bottom. A set of selection values is always
/// offered in a combo box, however many there are. The internal-frame
/// variants are not provided.
public class JOptionPane extends JComponent implements Accessible {

    public static final Object UNINITIALIZED_VALUE = "uninitializedValue";

    public static final int DEFAULT_OPTION = -1;
    public static final int YES_NO_OPTION = 0;
    public static final int YES_NO_CANCEL_OPTION = 1;
    public static final int OK_CANCEL_OPTION = 2;

    public static final int YES_OPTION = 0;
    public static final int NO_OPTION = 1;
    public static final int CANCEL_OPTION = 2;
    public static final int OK_OPTION = 0;
    public static final int CLOSED_OPTION = -1;

    public static final int ERROR_MESSAGE = 0;
    public static final int INFORMATION_MESSAGE = 1;
    public static final int WARNING_MESSAGE = 2;
    public static final int QUESTION_MESSAGE = 3;
    public static final int PLAIN_MESSAGE = -1;

    public static final String ICON_PROPERTY = "icon";
    public static final String MESSAGE_PROPERTY = "message";
    public static final String VALUE_PROPERTY = "value";
    public static final String OPTIONS_PROPERTY = "options";
    public static final String INITIAL_VALUE_PROPERTY = "initialValue";
    public static final String MESSAGE_TYPE_PROPERTY = "messageType";
    public static final String OPTION_TYPE_PROPERTY = "optionType";
    public static final String SELECTION_VALUES_PROPERTY = "selectionValues";
    public static final String INITIAL_SELECTION_VALUE_PROPERTY = "initialSelectionValue";
    public static final String INPUT_VALUE_PROPERTY = "inputValue";
    public static final String WANTS_INPUT_PROPERTY = "wantsInput";

    private static Frame rootFrame;

    protected Icon icon;
    protected Object message;
    protected Object[] options;
    protected Object initialValue;
    protected int messageType;
    protected int optionType;
    protected Object value;
    protected Object[] selectionValues;
    protected Object inputValue;
    protected Object initialSelectionValue;
    protected boolean wantsInput;

    private JDialog shownIn;
    private JButton defaultChoice;
    private Component initialFocus;
    private JTextField inputField;
    private JComboBox<Object> inputChoice;

    public JOptionPane() {
        this("JOptionPane message");
    }

    public JOptionPane(Object message) {
        this(message, PLAIN_MESSAGE);
    }

    public JOptionPane(Object message, int messageType) {
        this(message, messageType, DEFAULT_OPTION);
    }

    public JOptionPane(Object message, int messageType, int optionType) {
        this(message, messageType, optionType, null);
    }

    public JOptionPane(Object message, int messageType, int optionType, Icon icon) {
        this(message, messageType, optionType, icon, null);
    }

    public JOptionPane(Object message, int messageType, int optionType, Icon icon, Object[] options) {
        this(message, messageType, optionType, icon, options, null);
    }

    public JOptionPane(Object message, int messageType, int optionType, Icon icon, Object[] options,
            Object initialValue) {
        this.message = message;
        this.options = options == null ? null : copy(options);
        this.initialValue = initialValue;
        this.icon = icon;
        this.messageType = messageType;
        this.optionType = optionType;
        this.value = UNINITIALIZED_VALUE;
        this.inputValue = UNINITIALIZED_VALUE;
    }

    /// Identity, not equality: the sentinel is one particular object,
    /// and a value that merely reads the same is an answer.
    private static boolean same(Object a, Object b) {
        return a == b;
    }

    private static Object[] copy(Object[] a) {
        Object[] r = new Object[a.length];
        System.arraycopy(a, 0, r, 0, a.length);
        return r;
    }

    private static String text(String key, String fallback) {
        String s = UIManager.getString(key);
        return s == null ? fallback : s;
    }

    // ---- the static dialogs ----

    public static String showInputDialog(Object message) {
        return showInputDialog(null, message);
    }

    public static String showInputDialog(Object message, Object initialSelectionValue) {
        return showInputDialog(null, message, initialSelectionValue);
    }

    public static String showInputDialog(Component parentComponent, Object message) {
        return showInputDialog(parentComponent, message, text("OptionPane.inputDialogTitle", "Input"),
                QUESTION_MESSAGE);
    }

    public static String showInputDialog(Component parentComponent, Object message, Object initialSelectionValue) {
        Object v = showInputDialog(parentComponent, message, text("OptionPane.inputDialogTitle", "Input"),
                QUESTION_MESSAGE, null, null, initialSelectionValue);
        return v instanceof String ? (String) v : null;
    }

    public static String showInputDialog(Component parentComponent, Object message, String title, int messageType) {
        Object v = showInputDialog(parentComponent, message, title, messageType, null, null, null);
        return v instanceof String ? (String) v : null;
    }

    public static Object showInputDialog(Component parentComponent, Object message, String title, int messageType,
            Icon icon, Object[] selectionValues, Object initialSelectionValue) {
        JOptionPane pane = new JOptionPane(message, messageType, OK_CANCEL_OPTION, icon, null, null);
        pane.setWantsInput(true);
        pane.setSelectionValues(selectionValues);
        pane.setInitialSelectionValue(initialSelectionValue);
        JDialog dialog = pane.createDialog(parentComponent, title);
        pane.selectInitialValue();
        dialog.setVisible(true);
        dialog.dispose();
        Object v = pane.getInputValue();
        return same(v, UNINITIALIZED_VALUE) ? null : v;
    }

    public static void showMessageDialog(Component parentComponent, Object message) {
        showMessageDialog(parentComponent, message, text("OptionPane.messageDialogTitle", "Message"),
                INFORMATION_MESSAGE);
    }

    public static void showMessageDialog(Component parentComponent, Object message, String title, int messageType) {
        showMessageDialog(parentComponent, message, title, messageType, null);
    }

    public static void showMessageDialog(Component parentComponent, Object message, String title, int messageType,
            Icon icon) {
        showOptionDialog(parentComponent, message, title, DEFAULT_OPTION, messageType, icon, null, null);
    }

    public static int showConfirmDialog(Component parentComponent, Object message) {
        return showConfirmDialog(parentComponent, message, text("OptionPane.titleText", "Select an Option"),
                YES_NO_CANCEL_OPTION);
    }

    public static int showConfirmDialog(Component parentComponent, Object message, String title, int optionType) {
        return showConfirmDialog(parentComponent, message, title, optionType, QUESTION_MESSAGE);
    }

    public static int showConfirmDialog(Component parentComponent, Object message, String title, int optionType,
            int messageType) {
        return showConfirmDialog(parentComponent, message, title, optionType, messageType, null);
    }

    public static int showConfirmDialog(Component parentComponent, Object message, String title, int optionType,
            int messageType, Icon icon) {
        return showOptionDialog(parentComponent, message, title, optionType, messageType, icon, null, null);
    }

    public static int showOptionDialog(Component parentComponent, Object message, String title, int optionType,
            int messageType, Icon icon, Object[] options, Object initialValue) {
        JOptionPane pane = new JOptionPane(message, messageType, optionType, icon, options, initialValue);
        JDialog dialog = pane.createDialog(parentComponent, title);
        pane.selectInitialValue();
        dialog.setVisible(true);
        dialog.dispose();
        Object chosen = pane.getValue();
        if (chosen == null) {
            return CLOSED_OPTION;
        }
        if (options == null) {
            return chosen instanceof Integer ? ((Integer) chosen).intValue() : CLOSED_OPTION;
        }
        for (int i = 0; i < options.length; i++) {
            if (options[i] != null && options[i].equals(chosen)) {
                return i;
            }
        }
        return CLOSED_OPTION;
    }

    public static Frame getFrameForComponent(Component parentComponent) {
        for (Component c = parentComponent; c != null; c = c.getParent()) {
            if (c instanceof Frame) {
                return (Frame) c;
            }
        }
        return getRootFrame();
    }

    public static void setRootFrame(Frame newRootFrame) {
        rootFrame = newRootFrame;
    }

    /// The frame dialogs without a parent belong to. It is never shown.
    public static Frame getRootFrame() {
        if (rootFrame == null) {
            rootFrame = new Frame();
        }
        return rootFrame;
    }

    // ---- the dialog ----

    public JDialog createDialog(String title) {
        return createDialog(null, title);
    }

    public JDialog createDialog(Component parentComponent, String title) {
        Window owner = null;
        if (parentComponent instanceof Window) {
            owner = (Window) parentComponent;
        } else if (parentComponent != null) {
            owner = SwingUtilities.getWindowAncestor(parentComponent);
        }
        shownIn = null;
        setValue(UNINITIALIZED_VALUE);
        build();
        JDialog dialog = new JDialog(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
        Container content = dialog.getContentPane();
        content.setLayout(new BorderLayout());
        content.add(this, BorderLayout.CENTER);
        dialog.setResizable(false);
        dialog.getRootPane().setDefaultButton(defaultChoice);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                setValue(null);
            }
        });
        dialog.pack();
        dialog.setLocationRelativeTo(parentComponent);
        shownIn = dialog;
        return dialog;
    }

    /// Lays out the message, the input component and the buttons from the
    /// properties as they are now.
    private void build() {
        removeAll();
        defaultChoice = null;
        initialFocus = null;
        inputField = null;
        inputChoice = null;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        Icon shown = icon != null ? icon : typeIcon(messageType);
        if (shown != null) {
            JPanel side = new JPanel(new BorderLayout());
            side.add(new JLabel(shown), BorderLayout.NORTH);
            add(side, BorderLayout.WEST);
        }
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        addMessage(body, message);
        if (wantsInput) {
            if (selectionValues != null) {
                inputChoice = new JComboBox<Object>(copy(selectionValues));
                if (initialSelectionValue != null) {
                    inputChoice.setSelectedItem(initialSelectionValue);
                }
                inputChoice.setAlignmentX(0f);
                body.add(inputChoice);
                initialFocus = inputChoice;
            } else {
                inputField = new JTextField(20);
                if (initialSelectionValue != null) {
                    inputField.setText(String.valueOf(initialSelectionValue));
                }
                inputField.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        choose(Integer.valueOf(OK_OPTION));
                    }
                });
                inputField.setAlignmentX(0f);
                body.add(inputField);
                initialFocus = inputField;
            }
        }
        add(body, BorderLayout.CENTER);
        JPanel buttons = new JPanel();
        if (options != null) {
            for (int i = 0; i < options.length; i++) {
                final Object option = options[i];
                if (option instanceof Component) {
                    // A component among the options answers by itself,
                    // calling setValue when it is used.
                    buttons.add((Component) option);
                    continue;
                }
                JButton b = option instanceof Icon ? new JButton((Icon) option) : new JButton(String.valueOf(option));
                b.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        choose(option);
                    }
                });
                buttons.add(b);
                if (defaultChoice == null && option != null && option.equals(initialValue)) {
                    defaultChoice = b;
                }
            }
        } else if (optionType == YES_NO_OPTION) {
            standard(buttons, text("OptionPane.yesButtonText", "Yes"), YES_OPTION);
            standard(buttons, text("OptionPane.noButtonText", "No"), NO_OPTION);
        } else if (optionType == YES_NO_CANCEL_OPTION) {
            standard(buttons, text("OptionPane.yesButtonText", "Yes"), YES_OPTION);
            standard(buttons, text("OptionPane.noButtonText", "No"), NO_OPTION);
            standard(buttons, text("OptionPane.cancelButtonText", "Cancel"), CANCEL_OPTION);
        } else if (optionType == OK_CANCEL_OPTION) {
            standard(buttons, text("OptionPane.okButtonText", "OK"), OK_OPTION);
            standard(buttons, text("OptionPane.cancelButtonText", "Cancel"), CANCEL_OPTION);
        } else {
            standard(buttons, text("OptionPane.okButtonText", "OK"), OK_OPTION);
        }
        add(buttons, BorderLayout.SOUTH);
    }

    private void standard(JPanel buttons, String label, final int option) {
        JButton b = new JButton(label);
        b.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                choose(Integer.valueOf(option));
            }
        });
        buttons.add(b);
        Integer boxed = Integer.valueOf(option);
        if (boxed.equals(initialValue) || defaultChoice == null && initialValue == null) {
            defaultChoice = b;
        }
    }

    /// The user picked `choice`: take what was typed or selected when the
    /// dialog asks for input and it was confirmed, then answer.
    private void choose(Object choice) {
        if (wantsInput && options == null) {
            if (choice instanceof Integer && ((Integer) choice).intValue() == OK_OPTION) {
                if (inputChoice != null) {
                    setInputValue(inputChoice.getSelectedItem());
                } else if (inputField != null) {
                    setInputValue(inputField.getText());
                }
            } else {
                setInputValue(UNINITIALIZED_VALUE);
            }
        }
        setValue(choice);
    }

    private static void addMessage(JPanel body, Object msg) {
        if (msg == null) {
            return;
        }
        if (msg instanceof Component) {
            if (msg instanceof JComponent) {
                ((JComponent) msg).setAlignmentX(0f);
            }
            body.add((Component) msg);
        } else if (msg instanceof Object[]) {
            Object[] parts = (Object[]) msg;
            for (int i = 0; i < parts.length; i++) {
                addMessage(body, parts[i]);
            }
        } else if (msg instanceof Icon) {
            JLabel l = new JLabel((Icon) msg);
            l.setAlignmentX(0f);
            body.add(l);
        } else {
            String s = String.valueOf(msg);
            int from = 0;
            while (from <= s.length()) {
                int nl = s.indexOf('\n', from);
                String line = nl < 0 ? s.substring(from) : s.substring(from, nl);
                JLabel l = new JLabel(line.length() == 0 ? " " : line);
                l.setAlignmentX(0f);
                body.add(l);
                if (nl < 0) {
                    break;
                }
                from = nl + 1;
            }
        }
    }

    private static Icon typeIcon(int type) {
        String key;
        switch (type) {
            case ERROR_MESSAGE:
                key = "OptionPane.errorIcon";
                break;
            case INFORMATION_MESSAGE:
                key = "OptionPane.informationIcon";
                break;
            case WARNING_MESSAGE:
                key = "OptionPane.warningIcon";
                break;
            case QUESTION_MESSAGE:
                key = "OptionPane.questionIcon";
                break;
            default:
                return null;
        }
        Icon i = UIManager.getIcon(key);
        return i != null ? i : new TypeIcon(type);
    }

    /// The icon of a message type when the look and feel has none: a
    /// colored disc with the sign of the type on it.
    private static final class TypeIcon implements Icon {
        private static final int SIZE = 32;
        private final int type;

        TypeIcon(int type) {
            this.type = type;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            String sign;
            int rgb;
            if (type == ERROR_MESSAGE) {
                sign = "x";
                rgb = 0xC62828;
            } else if (type == WARNING_MESSAGE) {
                sign = "!";
                rgb = 0xEF6C00;
            } else if (type == QUESTION_MESSAGE) {
                sign = "?";
                rgb = 0x2E7D32;
            } else {
                sign = "i";
                rgb = 0x1565C0;
            }
            g.setColor(new Color(rgb));
            g.fillOval(x, y, SIZE, SIZE);
            g.setColor(new Color(0xFFFFFF));
            FontMetrics fm = g.getFontMetrics();
            int w = fm == null ? 0 : fm.stringWidth(sign);
            int asc = fm == null ? 0 : fm.getAscent();
            int desc = fm == null ? 0 : fm.getDescent();
            g.drawString(sign, x + (SIZE - w) / 2, y + (SIZE + asc - desc) / 2);
        }

        @Override
        public int getIconWidth() {
            return SIZE;
        }

        @Override
        public int getIconHeight() {
            return SIZE;
        }
    }

    // ---- properties ----

    public void setMessage(Object newMessage) {
        Object old = message;
        message = newMessage;
        firePropertyChange(MESSAGE_PROPERTY, old, message);
    }

    public Object getMessage() {
        return message;
    }

    public void setIcon(Icon newIcon) {
        Icon old = icon;
        icon = newIcon;
        firePropertyChange(ICON_PROPERTY, old, icon);
    }

    public Icon getIcon() {
        return icon;
    }

    /// Sets what the user chose. While the pane's dialog is showing, this
    /// closes it, which is what lets a blocked caller go on.
    public void setValue(Object newValue) {
        Object old = value;
        value = newValue;
        firePropertyChange(VALUE_PROPERTY, old, value);
        JDialog d = shownIn;
        if (d != null && !same(newValue, UNINITIALIZED_VALUE) && d.isVisible()) {
            d.setVisible(false);
        }
    }

    public Object getValue() {
        return value;
    }

    public void setOptions(Object[] newOptions) {
        Object[] old = options;
        options = newOptions == null ? null : copy(newOptions);
        firePropertyChange(OPTIONS_PROPERTY, old, options);
    }

    public Object[] getOptions() {
        return options == null ? null : copy(options);
    }

    public void setInitialValue(Object newInitialValue) {
        Object old = initialValue;
        initialValue = newInitialValue;
        firePropertyChange(INITIAL_VALUE_PROPERTY, old, initialValue);
    }

    public Object getInitialValue() {
        return initialValue;
    }

    public void setMessageType(int newType) {
        if (newType != ERROR_MESSAGE && newType != INFORMATION_MESSAGE && newType != WARNING_MESSAGE
                && newType != QUESTION_MESSAGE && newType != PLAIN_MESSAGE) {
            throw new RuntimeException("JOptionPane: type must be one of JOptionPane.ERROR_MESSAGE, "
                    + "JOptionPane.INFORMATION_MESSAGE, JOptionPane.WARNING_MESSAGE, "
                    + "JOptionPane.QUESTION_MESSAGE or JOptionPane.PLAIN_MESSAGE");
        }
        int old = messageType;
        messageType = newType;
        firePropertyChange(MESSAGE_TYPE_PROPERTY, old, messageType);
    }

    public int getMessageType() {
        return messageType;
    }

    public void setOptionType(int newType) {
        if (newType != DEFAULT_OPTION && newType != YES_NO_OPTION && newType != YES_NO_CANCEL_OPTION
                && newType != OK_CANCEL_OPTION) {
            throw new RuntimeException("JOptionPane: option type must be one of JOptionPane.DEFAULT_OPTION, "
                    + "JOptionPane.YES_NO_OPTION, JOptionPane.YES_NO_CANCEL_OPTION or "
                    + "JOptionPane.OK_CANCEL_OPTION");
        }
        int old = optionType;
        optionType = newType;
        firePropertyChange(OPTION_TYPE_PROPERTY, old, optionType);
    }

    public int getOptionType() {
        return optionType;
    }

    public void setSelectionValues(Object[] newValues) {
        Object[] old = selectionValues;
        selectionValues = newValues == null ? null : copy(newValues);
        firePropertyChange(SELECTION_VALUES_PROPERTY, old, selectionValues);
        if (selectionValues != null) {
            setWantsInput(true);
        }
    }

    public Object[] getSelectionValues() {
        return selectionValues == null ? null : copy(selectionValues);
    }

    public void setInitialSelectionValue(Object newValue) {
        Object old = initialSelectionValue;
        initialSelectionValue = newValue;
        firePropertyChange(INITIAL_SELECTION_VALUE_PROPERTY, old, newValue);
    }

    public Object getInitialSelectionValue() {
        return initialSelectionValue;
    }

    public void setInputValue(Object newValue) {
        Object old = inputValue;
        inputValue = newValue;
        firePropertyChange(INPUT_VALUE_PROPERTY, old, newValue);
    }

    public Object getInputValue() {
        return inputValue;
    }

    public int getMaxCharactersPerLineCount() {
        return Integer.MAX_VALUE;
    }

    public void setWantsInput(boolean newValue) {
        boolean old = wantsInput;
        wantsInput = newValue;
        firePropertyChange(WANTS_INPUT_PROPERTY, old, newValue);
    }

    public boolean getWantsInput() {
        return wantsInput;
    }

    /// Gives the focus to the input component, or to the default button.
    public void selectInitialValue() {
        Component c = initialFocus != null ? initialFocus : defaultChoice;
        if (c != null) {
            c.requestFocusInWindow();
        }
    }
}
