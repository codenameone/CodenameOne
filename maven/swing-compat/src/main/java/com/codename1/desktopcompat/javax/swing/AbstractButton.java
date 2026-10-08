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

import com.codename1.desktopcompat.java.awt.Insets;
import com.codename1.desktopcompat.java.awt.ItemSelectable;
import com.codename1.desktopcompat.java.awt.event.ActionEvent;
import com.codename1.desktopcompat.java.awt.event.ActionListener;
import com.codename1.desktopcompat.java.awt.event.ItemEvent;
import com.codename1.desktopcompat.java.awt.event.ItemListener;
import com.codename1.desktopcompat.javax.swing.event.ChangeEvent;
import com.codename1.desktopcompat.javax.swing.event.ChangeListener;
import com.codename1.desktopcompat.rt.Icons;

/// The base of the buttons: text, icon, a button model and the action,
/// item and change listeners.
///
/// The peer is a Codename One button of some kind. A click on it runs
/// `doClick()`, which drives the model exactly as a mouse click drives it
/// on the desktop, so listeners see the same events in the same order. The
/// margin, the mnemonic, the alignment and text position properties and the
/// rollover, pressed and selected icons are recorded only; components
/// added to a button are not shown.
public abstract class AbstractButton extends JComponent implements ItemSelectable, SwingConstants {

    protected ButtonModel model;
    protected ChangeEvent changeEvent;

    private final Handler handler = new Handler();
    private String text = "";
    private Icon icon;
    private Insets margin;
    private Action action;
    private int mnemonic;
    private int horizontalAlignment = CENTER;
    private int verticalAlignment = CENTER;
    private int horizontalTextPosition = TRAILING;
    private int verticalTextPosition = CENTER;
    private boolean borderPainted = true;
    private boolean focusPainted = true;
    private boolean contentAreaFilled = true;
    private boolean rolloverEnabled;

    public AbstractButton() {
    }

    protected void init(String text, Icon icon) {
        if (text != null) {
            setText(text);
        }
        if (icon != null) {
            setIcon(icon);
        }
        setAlignmentX(LEFT_ALIGNMENT);
        setAlignmentY(CENTER_ALIGNMENT);
    }

    @Override
    protected void cn1PeerCreated() {
        super.cn1PeerCreated();
        com.codename1.ui.Component p = cn1PeerOrNull();
        if (p instanceof com.codename1.ui.Button) {
            ((com.codename1.ui.Button) p).addActionListener(
                    new com.codename1.ui.events.ActionListener<com.codename1.ui.events.ActionEvent>() {
                        @Override
                        public void actionPerformed(com.codename1.ui.events.ActionEvent evt) {
                            doClick();
                        }
                    });
        }
        sync();
    }

    private void sync() {
        com.codename1.ui.Component p = cn1PeerOrNull();
        if (p instanceof com.codename1.ui.Button) {
            com.codename1.ui.Button b = (com.codename1.ui.Button) p;
            b.setText(text == null ? "" : text);
            b.setIcon(Icons.toNative(icon, this));
            boolean selected = model != null && model.isSelected();
            if (p instanceof com.codename1.ui.CheckBox) {
                ((com.codename1.ui.CheckBox) p).setSelected(selected);
            } else if (p instanceof com.codename1.ui.RadioButton) {
                ((com.codename1.ui.RadioButton) p).setSelected(selected);
            }
        }
    }

    private void changed() {
        sync();
        revalidate();
        repaint();
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        String old = this.text;
        this.text = text;
        firePropertyChange("text", old, text);
        if (old == null ? text != null : !old.equals(text)) {
            changed();
        }
    }

    public Icon getIcon() {
        return icon;
    }

    public void setIcon(Icon defaultIcon) {
        Icon old = icon;
        icon = defaultIcon;
        firePropertyChange("icon", old, defaultIcon);
        if (old != defaultIcon) {
            changed();
        }
    }

    public ButtonModel getModel() {
        return model;
    }

    public void setModel(ButtonModel newModel) {
        ButtonModel old = model;
        if (old != null) {
            old.removeChangeListener(handler);
            old.removeActionListener(handler);
            old.removeItemListener(handler);
        }
        model = newModel;
        if (newModel != null) {
            newModel.addChangeListener(handler);
            newModel.addActionListener(handler);
            newModel.addItemListener(handler);
        }
        firePropertyChange("model", old, newModel);
        if (newModel != old) {
            changed();
        }
    }

    public boolean isSelected() {
        return model.isSelected();
    }

    public void setSelected(boolean b) {
        model.setSelected(b);
    }

    /// Clicks the button: arms and presses the model, then releases it,
    /// which notifies the action listeners.
    public void doClick() {
        doClick(68);
    }

    /// As `doClick()`; the button is not shown pressed for `pressTime`.
    public void doClick(int pressTime) {
        model.setArmed(true);
        model.setPressed(true);
        model.setPressed(false);
        model.setArmed(false);
    }

    @Override
    public void setEnabled(boolean b) {
        super.setEnabled(b);
        model.setEnabled(b);
    }

    public String getActionCommand() {
        String ac = model.getActionCommand();
        return ac == null ? getText() : ac;
    }

    public void setActionCommand(String actionCommand) {
        model.setActionCommand(actionCommand);
    }

    public Action getAction() {
        return action;
    }

    /// Takes the action's name as the text, its enabled state, and makes
    /// it an action listener. Later changes of the action's properties are
    /// not followed.
    public void setAction(Action a) {
        Action old = action;
        if (old == a) {
            return;
        }
        if (old != null) {
            removeActionListener(old);
        }
        action = a;
        if (a != null) {
            Object name = a.getValue(Action.NAME);
            if (name instanceof String) {
                setText((String) name);
            }
            Object cmd = a.getValue(Action.ACTION_COMMAND_KEY);
            if (cmd instanceof String) {
                setActionCommand((String) cmd);
            }
            setEnabled(a.isEnabled());
            addActionListener(a);
        }
        firePropertyChange("action", old, a);
    }

    public void addActionListener(ActionListener l) {
        listenerList.add(ActionListener.class, l);
    }

    public void removeActionListener(ActionListener l) {
        listenerList.remove(ActionListener.class, l);
    }

    public ActionListener[] getActionListeners() {
        return listenerList.getListeners(ActionListener.class);
    }

    public void addChangeListener(ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }

    public void removeChangeListener(ChangeListener l) {
        listenerList.remove(ChangeListener.class, l);
    }

    public ChangeListener[] getChangeListeners() {
        return listenerList.getListeners(ChangeListener.class);
    }

    @Override
    public void addItemListener(ItemListener l) {
        listenerList.add(ItemListener.class, l);
    }

    @Override
    public void removeItemListener(ItemListener l) {
        listenerList.remove(ItemListener.class, l);
    }

    public ItemListener[] getItemListeners() {
        return listenerList.getListeners(ItemListener.class);
    }

    @Override
    public Object[] getSelectedObjects() {
        if (!isSelected()) {
            return null;
        }
        return new Object[]{getText()};
    }

    protected void fireActionPerformed(ActionEvent event) {
        ActionListener[] ls = getActionListeners();
        ActionEvent e = null;
        for (int i = ls.length - 1; i >= 0; i--) {
            if (e == null) {
                String cmd = event.getActionCommand();
                e = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, cmd == null ? getActionCommand() : cmd,
                        event.getWhen(), event.getModifiers());
            }
            ls[i].actionPerformed(e);
        }
    }

    protected void fireStateChanged() {
        ChangeListener[] ls = getChangeListeners();
        for (int i = ls.length - 1; i >= 0; i--) {
            if (changeEvent == null) {
                changeEvent = new ChangeEvent(this);
            }
            ls[i].stateChanged(changeEvent);
        }
    }

    protected void fireItemStateChanged(ItemEvent event) {
        ItemListener[] ls = getItemListeners();
        ItemEvent e = null;
        for (int i = ls.length - 1; i >= 0; i--) {
            if (e == null) {
                e = new ItemEvent(this, ItemEvent.ITEM_STATE_CHANGED, this, event.getStateChange());
            }
            ls[i].itemStateChanged(e);
        }
    }

    public int getMnemonic() {
        return mnemonic;
    }

    public void setMnemonic(int mnemonic) {
        this.mnemonic = mnemonic;
        model.setMnemonic(mnemonic);
    }

    public void setMnemonic(char mnemonic) {
        int vk = mnemonic;
        if (vk >= 'a' && vk <= 'z') {
            vk -= 'a' - 'A';
        }
        setMnemonic(vk);
    }

    public Insets getMargin() {
        return margin == null ? null : new Insets(margin.top, margin.left, margin.bottom, margin.right);
    }

    public void setMargin(Insets m) {
        margin = m == null ? null : new Insets(m.top, m.left, m.bottom, m.right);
    }

    public boolean isBorderPainted() {
        return borderPainted;
    }

    public void setBorderPainted(boolean b) {
        borderPainted = b;
    }

    public boolean isFocusPainted() {
        return focusPainted;
    }

    public void setFocusPainted(boolean b) {
        focusPainted = b;
    }

    public boolean isContentAreaFilled() {
        return contentAreaFilled;
    }

    public void setContentAreaFilled(boolean b) {
        contentAreaFilled = b;
    }

    public boolean isRolloverEnabled() {
        return rolloverEnabled;
    }

    public void setRolloverEnabled(boolean b) {
        rolloverEnabled = b;
    }

    public int getHorizontalAlignment() {
        return horizontalAlignment;
    }

    public void setHorizontalAlignment(int alignment) {
        horizontalAlignment = alignment;
    }

    public int getVerticalAlignment() {
        return verticalAlignment;
    }

    public void setVerticalAlignment(int alignment) {
        verticalAlignment = alignment;
    }

    public int getHorizontalTextPosition() {
        return horizontalTextPosition;
    }

    public void setHorizontalTextPosition(int textPosition) {
        horizontalTextPosition = textPosition;
    }

    public int getVerticalTextPosition() {
        return verticalTextPosition;
    }

    public void setVerticalTextPosition(int textPosition) {
        verticalTextPosition = textPosition;
    }

    @Override
    protected String paramString() {
        return super.paramString() + ",text=" + text;
    }

    /// Forwards the model's events as this button's.
    private final class Handler implements ChangeListener, ActionListener, ItemListener {

        @Override
        public void stateChanged(ChangeEvent e) {
            sync();
            fireStateChanged();
            repaint();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            fireActionPerformed(e);
        }

        @Override
        public void itemStateChanged(ItemEvent e) {
            fireItemStateChanged(e);
        }
    }
}
