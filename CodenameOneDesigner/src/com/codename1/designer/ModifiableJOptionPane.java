/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.designer;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.HeadlessException;
import javax.swing.*;

public class ModifiableJOptionPane extends JOptionPane {

    private boolean resizable;

    public ModifiableJOptionPane() {
        super();
    }

    /**
     * @param message
     */
    public ModifiableJOptionPane(Object message) {
        super(message);
    }

    /**
     * @param message
     * @param messageType
     */
    public ModifiableJOptionPane(Object message, int messageType) {
        super(message, messageType);
    }

    /**
     * @param message
     * @param messageType
     * @param optionType
     */
    public ModifiableJOptionPane(Object message, int messageType, int optionType) {
        super(message, messageType, optionType);
    }

    /**
     * @param message
     * @param messageType
     * @param optionType
     * @param icon
     */
    public ModifiableJOptionPane(Object message, int messageType, int optionType,
            Icon icon) {
        super(message, messageType, optionType, icon);
    }

    /**
     * @param message
     * @param messageType
     * @param optionType
     * @param icon
     * @param options
     */
    public ModifiableJOptionPane(Object message, int messageType, int optionType,
            Icon icon, Object[] options) {
        super(message, messageType, optionType, icon, options);
    }

    /**
     * @param message
     * @param messageType
     * @param optionType
     * @param icon
     * @param options
     * @param initialValue
     */
    public ModifiableJOptionPane(Object message, int messageType, int optionType,
            Icon icon, Object[] options, Object initialValue) {
        super(message, messageType, optionType, icon, options, initialValue);
    }

    /**
     * @see javax.swing.JOptionPane#createDialog(java.awt.Component,
     * java.lang.String)
     */
    public JDialog createDialog(Component parentComponent, String title)
            throws HeadlessException {
        JDialog dialog = super.createDialog(parentComponent, title);
        dialog.setResizable(isResizable());
        return dialog;
    }

    /**
     * @see javax.swing.JOptionPane#createInternalFrame(java.awt.Component,
     * java.lang.String)
     */
    public JInternalFrame createInternalFrame(Component parentComponent,
            String title) {
        JInternalFrame frame = super.createInternalFrame(parentComponent, title);
        frame.setMaximizable(true);
        frame.setResizable(isResizable());
        return frame;
    }

    public void setResizable(boolean b) {
        this.resizable = b;
    }

    public boolean isResizable() {
        return resizable;
    }

    public static int showConfirmDialog(Component parent, Object msg, String title) {
        //((JComponent)msg).setPreferredSize(new Dimension(800, 600));
        ModifiableJOptionPane mo = new ModifiableJOptionPane(msg);
        mo.setOptionType(OK_CANCEL_OPTION);
        mo.setMessageType(PLAIN_MESSAGE);
        JDialog d = mo.createDialog(parent, title);
        //this can probably be commented out, but first we should check the effect on Mac
        //d.setSize(new Dimension(800, 700));
        d.pack();
        d.setResizable(true);
        mo.selectInitialValue();
        d.show();
        d.dispose();

        Object selectedValue = mo.getValue();

        if (selectedValue == null) {
            return CLOSED_OPTION;
        }
        if (selectedValue instanceof Integer) {
            return ((Integer) selectedValue).intValue();
        }
        return CLOSED_OPTION;

    }
    
    public static void main(String [] args){
        JFrame f = new JFrame("aaaaa");
        f.setVisible(true);
        showConfirmDialog(f, new JLabel("aaaaaa"), "Hello");
    
    }
}