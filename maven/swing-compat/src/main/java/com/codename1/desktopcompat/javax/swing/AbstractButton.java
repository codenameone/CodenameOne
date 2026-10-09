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

import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Insets;
import com.codename1.desktopcompat.java.awt.ItemSelectable;
import com.codename1.desktopcompat.java.awt.event.ActionEvent;
import com.codename1.desktopcompat.java.awt.event.ActionListener;
import com.codename1.desktopcompat.java.awt.event.ItemEvent;
import com.codename1.desktopcompat.java.awt.event.ItemListener;
import com.codename1.desktopcompat.javax.swing.event.ChangeEvent;
import com.codename1.desktopcompat.javax.swing.event.ChangeListener;
import com.codename1.desktopcompat.rt.Align;
import com.codename1.desktopcompat.rt.Icons;
import com.codename1.desktopcompat.rt.MiniHtml;
import com.codename1.desktopcompat.rt.Peer;
import com.codename1.desktopcompat.rt.Units;

/// The base of the buttons: text, icon, a button model and the action,
/// item and change listeners.
///
/// The peer is a Codename One button of some kind. A click on it runs
/// `doClick()`, which drives the model exactly as a mouse click drives it
/// on the desktop, so listeners see the same events in the same order.
///
/// The pressed, disabled, rollover and selected icons, the alignment, the
/// text position, the gap between icon and text and the margin are pushed
/// to the Codename One button, each once the application sets it; until
/// then the theme decides. Codename One places text on one of the four
/// sides of the icon, so text centered over the icon is placed after it.
/// A button that does not paint its border or fill its content area loses
/// the theme's border and background. HTML text is shown without its
/// tags, on one line. The mnemonic is recorded only, and components added
/// to a button are not shown.
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
    private MiniHtml.Document html;
    private boolean focusPainted = true;
    private boolean contentAreaFilled = true;
    private boolean rolloverEnabled;
    private Icon pressedIcon;
    private Icon selectedIcon;
    private Icon disabledIcon;
    private Icon disabledSelectedIcon;
    private Icon rolloverIcon;
    private Icon rolloverSelectedIcon;
    private int iconTextGap = 4;
    private boolean iconTextGapSet;
    private boolean horizontalAlignmentSet;
    private boolean verticalAlignmentSet;
    private boolean textPositionSet;
    private int displayedMnemonicIndex = -1;
    private boolean hideActionText;
    private boolean restyled;
    private final Icon[] iconKeys = new Icon[5];
    private final com.codename1.ui.Image[] iconImages = new com.codename1.ui.Image[5];

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
        restyle();
        sync();
    }

    private void sync() {
        com.codename1.ui.Component p = cn1PeerOrNull();
        if (p instanceof com.codename1.ui.Button) {
            com.codename1.ui.Button b = (com.codename1.ui.Button) p;
            b.setText(text == null || htmlOverlay() ? "" : MiniHtml.singleLine(text));
            boolean selected = model != null && model.isSelected();
            b.setIcon(nativeIcon(0, selected && selectedIcon != null ? selectedIcon : icon));
            b.setPressedIcon(nativeIcon(1, pressedIcon));
            b.setRolloverIcon(nativeIcon(2, selected && rolloverSelectedIcon != null ? rolloverSelectedIcon
                    : rolloverIcon));
            b.setDisabledIcon(nativeIcon(3, selected && disabledSelectedIcon != null ? disabledSelectedIcon
                    : disabledIcon));
            // A Codename One toggle shows this one while it is selected.
            b.setRolloverPressedIcon(nativeIcon(4, selectedIcon));
            if (horizontalAlignmentSet) {
                b.getAllStyles().setAlignment(Align.horizontal(horizontalAlignment));
            }
            if (verticalAlignmentSet) {
                b.setVerticalAlignment(Align.vertical(verticalAlignment));
            }
            if (textPositionSet) {
                b.setTextPosition(Align.textPosition(horizontalTextPosition, verticalTextPosition));
            }
            if (iconTextGapSet) {
                b.setGap(Units.toDevice(iconTextGap));
            }
            if (p instanceof com.codename1.ui.CheckBox) {
                ((com.codename1.ui.CheckBox) p).setSelected(selected);
            } else if (p instanceof com.codename1.ui.RadioButton) {
                ((com.codename1.ui.RadioButton) p).setSelected(selected);
            }
        }
    }

    /// The image of an icon, converted once for as long as the icon in
    /// that slot stays the same.
    private com.codename1.ui.Image nativeIcon(int slot, Icon i) {
        if (i == null) {
            iconKeys[slot] = null;
            iconImages[slot] = null;
            return null;
        }
        if (iconKeys[slot] != i || iconImages[slot] == null) {
            iconKeys[slot] = i;
            iconImages[slot] = Icons.toNative(i, this);
        }
        return iconImages[slot];
    }

    /// Pushes the margin and the border and content area switches to the
    /// peer's styles, starting from the theme's whenever one was undone.
    private void restyle() {
        com.codename1.ui.Component p = cn1PeerOrNull();
        if (!(p instanceof com.codename1.ui.Button)) {
            return;
        }
        com.codename1.ui.Button b = (com.codename1.ui.Button) p;
        boolean checkMark = (p instanceof com.codename1.ui.CheckBox || p instanceof com.codename1.ui.RadioButton)
                && !b.isToggle();
        boolean noBorder = !borderPainted && !checkMark;
        boolean noFill = !contentAreaFilled && !checkMark;
        if (margin == null && !noBorder && !noFill && !restyled) {
            return;
        }
        if (restyled) {
            b.setUIID(b.getUIID());
            if (p instanceof Peer) {
                ((Peer) p).support().applyStyle();
            }
        }
        restyled = margin != null || noBorder || noFill;
        com.codename1.ui.plaf.Style s = b.getAllStyles();
        if (margin != null) {
            byte px = com.codename1.ui.plaf.Style.UNIT_TYPE_PIXELS;
            s.setPaddingUnit(px, px, px, px);
            s.setPadding(Units.toDevice(margin.top), Units.toDevice(margin.bottom), Units.toDevice(margin.left),
                    Units.toDevice(margin.right));
        }
        com.codename1.ui.plaf.Border border = b.getUnselectedStyle().getBorder();
        if (noBorder || (noFill && border != null && border.isBackgroundPainter())) {
            s.setBorder(com.codename1.ui.plaf.Border.createEmpty());
        }
        if (noFill) {
            s.setBgTransparency(0);
        }
    }

    private void changed() {
        html = null;
        sync();
        revalidate();
        repaint();
    }

    // ------------------------------------------------------------ html

    /// Whether the text is HTML that is drawn here, over the native
    /// button, with its colors, sizes and styles: for a button without an
    /// icon that is not a check box or radio button. The others show the
    /// text of the HTML on one line, in the widget's own style, because
    /// only the widget knows where it puts text beside a mark or an icon.
    private boolean htmlOverlay() {
        if (!MiniHtml.isHtml(text) || icon != null) {
            return false;
        }
        com.codename1.ui.Component p = cn1PeerOrNull();
        if (!(p instanceof com.codename1.ui.Button)) {
            return false;
        }
        boolean mark = p instanceof com.codename1.ui.CheckBox || p instanceof com.codename1.ui.RadioButton;
        return !mark || ((com.codename1.ui.Button) p).isToggle();
    }

    private MiniHtml.Document htmlDocument() {
        if (html == null) {
            html = MiniHtml.parse(text);
        }
        return html;
    }

    /// The font HTML text starts from: the one that was set, else the
    /// font the theme gives the button.
    private com.codename1.desktopcompat.java.awt.Font htmlFont() {
        com.codename1.ui.Component p = cn1PeerOrNull();
        if (!isFontSet() && p != null && p.getStyle().getFont() != null) {
            return com.codename1.desktopcompat.rt.Fonts.fromNative(p.getStyle().getFont());
        }
        return getFont();
    }

    @Override
    protected Dimension cn1NativePreferredSize() {
        Dimension d = super.cn1NativePreferredSize();
        if (d == null || !htmlOverlay()) {
            return d;
        }
        com.codename1.ui.plaf.Style st = cn1Peer().getStyle();
        Dimension t = MiniHtml.preferredSize(htmlDocument(), htmlFont());
        int padV = Units.toLogicalCeil(st.getPaddingTop()) + Units.toLogicalCeil(st.getPaddingBottom());
        return new Dimension(d.width + t.width, Math.max(d.height, t.height + padV));
    }

    /// The preferred size, except that a button whose text is HTML can
    /// grow as wide as it is let. That is the desktop's doing -- the size
    /// the view of an HTML text can take has no limit -- and layouts that
    /// hand out spare room by maximum size, a box among them, give it to
    /// such a button. The limit is the largest `int` and not the largest
    /// `short` that glue has, so beside glue the button takes all of it.
    @Override
    public Dimension getMaximumSize() {
        Dimension d = super.getMaximumSize();
        if (!isMaximumSizeSet() && MiniHtml.isHtml(text) && d != null) {
            return new Dimension(Integer.MAX_VALUE, d.height);
        }
        return d;
    }

    @Override
    protected void paintComponent(com.codename1.desktopcompat.java.awt.Graphics g) {
        super.paintComponent(g);
        if (!htmlOverlay()) {
            return;
        }
        com.codename1.ui.plaf.Style st = cn1Peer().getStyle();
        int left = Units.toLogicalCeil(st.getPaddingLeftNoRTL());
        int right = Units.toLogicalCeil(st.getPaddingRightNoRTL());
        int top = Units.toLogicalCeil(st.getPaddingTop());
        int bottom = Units.toLogicalCeil(st.getPaddingBottom());
        com.codename1.desktopcompat.java.awt.Font f = htmlFont();
        MiniHtml.Document doc = htmlDocument();
        Dimension t = MiniHtml.preferredSize(doc, f);
        int availW = Math.max(0, getWidth() - left - right);
        int availH = Math.max(0, getHeight() - top - bottom);
        int align = horizontalAlignmentSet ? horizontalAlignment : CENTER;
        int x = left;
        int lineAlign = MiniHtml.ALIGN_LEFT;
        if (align == CENTER) {
            x += (availW - t.width) / 2;
            lineAlign = MiniHtml.ALIGN_CENTER;
        } else if (align == RIGHT || align == TRAILING) {
            x += availW - t.width;
            lineAlign = MiniHtml.ALIGN_RIGHT;
        }
        int y = top + (availH - t.height) / 2;
        g.setFont(f);
        com.codename1.desktopcompat.java.awt.Color fg = isForegroundSet() ? getForeground()
                : new com.codename1.desktopcompat.java.awt.Color(st.getFgColor() & 0xffffff);
        if (!isEnabled()) {
            fg = com.codename1.desktopcompat.java.awt.Color.GRAY;
        }
        g.setColor(fg);
        MiniHtml.paint(g, doc, x, y, t.width, lineAlign);
    }

    private void styleChanged() {
        restyle();
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
        Insets old = margin;
        margin = m == null ? null : new Insets(m.top, m.left, m.bottom, m.right);
        firePropertyChange("margin", old, margin);
        if (old == null ? margin != null : !old.equals(margin)) {
            styleChanged();
        }
    }

    public boolean isBorderPainted() {
        return borderPainted;
    }

    public void setBorderPainted(boolean b) {
        boolean old = borderPainted;
        borderPainted = b;
        firePropertyChange("borderPainted", old, b);
        if (old != b) {
            styleChanged();
        }
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
        boolean old = contentAreaFilled;
        contentAreaFilled = b;
        firePropertyChange("contentAreaFilled", old, b);
        if (old != b) {
            styleChanged();
        }
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
        if (alignment != LEFT && alignment != CENTER && alignment != RIGHT && alignment != LEADING && alignment != TRAILING) {
            throw new IllegalArgumentException("horizontalAlignment");
        }
        int old = horizontalAlignment;
        horizontalAlignment = alignment;
        horizontalAlignmentSet = true;
        firePropertyChange("horizontalAlignment", old, alignment);
        changed();
    }

    public int getVerticalAlignment() {
        return verticalAlignment;
    }

    public void setVerticalAlignment(int alignment) {
        if (alignment != TOP && alignment != CENTER && alignment != BOTTOM) {
            throw new IllegalArgumentException("verticalAlignment");
        }
        int old = verticalAlignment;
        verticalAlignment = alignment;
        verticalAlignmentSet = true;
        firePropertyChange("verticalAlignment", old, alignment);
        changed();
    }

    public int getHorizontalTextPosition() {
        return horizontalTextPosition;
    }

    public void setHorizontalTextPosition(int textPosition) {
        if (textPosition != LEFT && textPosition != CENTER && textPosition != RIGHT && textPosition != LEADING && textPosition != TRAILING) {
            throw new IllegalArgumentException("horizontalTextPosition");
        }
        int old = horizontalTextPosition;
        horizontalTextPosition = textPosition;
        textPositionSet = true;
        firePropertyChange("horizontalTextPosition", old, textPosition);
        changed();
    }

    public int getVerticalTextPosition() {
        return verticalTextPosition;
    }

    public void setVerticalTextPosition(int textPosition) {
        if (textPosition != TOP && textPosition != CENTER && textPosition != BOTTOM) {
            throw new IllegalArgumentException("verticalTextPosition");
        }
        int old = verticalTextPosition;
        verticalTextPosition = textPosition;
        textPositionSet = true;
        firePropertyChange("verticalTextPosition", old, textPosition);
        changed();
    }

    public Icon getPressedIcon() {
        return pressedIcon;
    }

    public void setPressedIcon(Icon pressedIcon) {
        Icon old = this.pressedIcon;
        this.pressedIcon = pressedIcon;
        firePropertyChange("pressedIcon", old, pressedIcon);
        if (old != pressedIcon) {
            changed();
        }
    }

    public Icon getSelectedIcon() {
        return selectedIcon;
    }

    public void setSelectedIcon(Icon selectedIcon) {
        Icon old = this.selectedIcon;
        this.selectedIcon = selectedIcon;
        firePropertyChange("selectedIcon", old, selectedIcon);
        if (old != selectedIcon) {
            changed();
        }
    }

    public Icon getRolloverIcon() {
        return rolloverIcon;
    }

    public void setRolloverIcon(Icon rolloverIcon) {
        Icon old = this.rolloverIcon;
        this.rolloverIcon = rolloverIcon;
        firePropertyChange("rolloverIcon", old, rolloverIcon);
        setRolloverEnabled(true);
        if (old != rolloverIcon) {
            changed();
        }
    }

    public Icon getRolloverSelectedIcon() {
        return rolloverSelectedIcon;
    }

    public void setRolloverSelectedIcon(Icon rolloverSelectedIcon) {
        Icon old = this.rolloverSelectedIcon;
        this.rolloverSelectedIcon = rolloverSelectedIcon;
        firePropertyChange("rolloverSelectedIcon", old, rolloverSelectedIcon);
        setRolloverEnabled(true);
        if (old != rolloverSelectedIcon) {
            changed();
        }
    }

    /// The icon set with `setDisabledIcon`, or `null`: none is made from
    /// the default icon, the Codename One button dims it.
    public Icon getDisabledIcon() {
        return disabledIcon;
    }

    public void setDisabledIcon(Icon disabledIcon) {
        Icon old = this.disabledIcon;
        this.disabledIcon = disabledIcon;
        firePropertyChange("disabledIcon", old, disabledIcon);
        if (old != disabledIcon) {
            changed();
        }
    }

    public Icon getDisabledSelectedIcon() {
        return disabledSelectedIcon;
    }

    public void setDisabledSelectedIcon(Icon disabledSelectedIcon) {
        Icon old = this.disabledSelectedIcon;
        this.disabledSelectedIcon = disabledSelectedIcon;
        firePropertyChange("disabledSelectedIcon", old, disabledSelectedIcon);
        if (old != disabledSelectedIcon) {
            changed();
        }
    }

    public int getIconTextGap() {
        return iconTextGap;
    }

    public void setIconTextGap(int iconTextGap) {
        int old = this.iconTextGap;
        this.iconTextGap = iconTextGap;
        iconTextGapSet = true;
        firePropertyChange("iconTextGap", old, iconTextGap);
        if (old != iconTextGap) {
            changed();
        }
    }

    public int getDisplayedMnemonicIndex() {
        return displayedMnemonicIndex;
    }

    /// Recorded only: no character is underlined.
    public void setDisplayedMnemonicIndex(int index) {
        int old = displayedMnemonicIndex;
        if (index == -1) {
            displayedMnemonicIndex = -1;
        } else {
            int length = text == null ? 0 : text.length();
            if (index < -1 || index >= length) {
                throw new IllegalArgumentException("index == " + index);
            }
            displayedMnemonicIndex = index;
        }
        firePropertyChange("displayedMnemonicIndex", old, index);
    }

    public boolean getHideActionText() {
        return hideActionText;
    }

    /// Recorded only: `setAction` takes the action's name either way.
    public void setHideActionText(boolean hideActionText) {
        boolean old = this.hideActionText;
        this.hideActionText = hideActionText;
        firePropertyChange("hideActionText", old, hideActionText);
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
