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
package com.codename1.ui.spinner;

import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.List;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.list.DefaultListCellRenderer;
import com.codename1.ui.list.ListCellRenderer;
import com.codename1.ui.list.ListModel;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import java.util.Calendar;
import java.util.Date;

/**
 * A spinner allows us to select a numeric, date or time value using the arrow keys
 * in a similar way to a list or a combo box.
 *
 * @author Shai Almog
 */
public class Spinner extends List {
    /**
     * Value for create date renderer represnting Day-Month-4 Digit Year
     */
    public static final int DATE_FORMAT_DD_MM_YYYY = 1;

    /**
     * Value for create date renderer represnting Month-Day-4 Digit Year
     */
    public static final int DATE_FORMAT_MM_DD_YYYY = 2;

    /**
     * Value for create date renderer represnting Day-Month-2 Digit Year
     */
    public static final int DATE_FORMAT_DD_MM_YY = 11;

    /**
     * Value for create date renderer represnting Month-Day-2 Digit Year
     */
    public static final int DATE_FORMAT_MM_DD_YY = 12;

    /**
     * The image appearing on the side of the spinner widget to indicate its "spinnability"
     */
    private static Image spinnerHandle;

    private long lastKeyInteraction = -1;
    private TextField quickType = new TextField();
    private boolean monthFirst;
    private int currentInputAlign = LEFT;
    private static int inputSkipDelay = 2000;
    private Style overlayStyle;

    /**
     * Creates a new time spinner instance, time is an integer represented in seconds
     * since mindnight
     *
     * @param min lowest value allowed in seconds since midnight
     * @param max maximum value allowed in seconds since midnight
     * @param currentValue the starting value in seconds since midnight
     * @param step increments in the spinner (in seconds)
     * @param twentyFourHours show the value as 24 hour values or AM/PM
     * @param showSeconds show the value of the seconds as well or hide it
     * @return new spinner instance
     */
    public static Spinner createTime(int min, int max, int currentValue, int step, boolean twentyFourHours, boolean showSeconds) {
        Spinner s = new Spinner(new SpinnerNumberModel(min, max, currentValue, step),
                DateTimeRenderer.createTimeRenderer(twentyFourHours, showSeconds));
        return s;
    }


    /**
     * Creates a new date spinner instance
     *
     * @param min lowest value allowed
     * @param max maximum value allowed
     * @param currentValue the starting value for the mode
     * @param separatorChar character to separate the entries during rendering
     * @param format formatting type for the field
     * @return new spinner instance
     */
    public static Spinner createDate(long min, long max, long currentValue, char separatorChar, int format) {
        Spinner s = new Spinner(new SpinnerDateModel(min, max, currentValue), DateTimeRenderer.createDateRenderer(separatorChar, format));
        s.monthFirst = format == DATE_FORMAT_MM_DD_YY || format == DATE_FORMAT_MM_DD_YYYY;
        return s;
    }

    /**
     * Creates a new numeric spinner instance
     *
     * @param min lowest value allowed
     * @param max maximum value allowed
     * @param currentValue the starting value for the mode
     * @param step the value by which we increment the entries in the model
     * @return new spinner instance
     */
    public static Spinner create(int min, int max, int currentValue, int step) {
        return new Spinner(new SpinnerNumberModel(min, max, currentValue, step), new DefaultListCellRenderer(false));
    }


    /**
     * Creates a new numeric spinner instance
     *
     * @param min lowest value allowed
     * @param max maximum value allowed
     * @param currentValue the starting value for the mode
     * @param step the value by which we increment the entries in the model
     * @return new spinner instance
     */
    public static Spinner create(double min, double max, double currentValue, double step) {
        return new Spinner(new SpinnerNumberModel(min, max, currentValue, step), new DefaultListCellRenderer(false) {
            public Component getListCellRendererComponent(List list, Object value, int index, boolean isSelected) {
                if(value != null && value instanceof Double) {
                    // round the number in the spinner to two digits
                    double d = ((Double)value).doubleValue();
                    long l = (long)d;
                    long r = (long)(d * 100);
                    r %= 100;
                    value = "" + l + "." + r;
                }
                return super.getListCellRendererComponent(list, value, index, isSelected);
            }
        });
    }

    /**
     * Creates a new spinner instance with the given spinner model
     *
     * @param spinner model such as SpinnerDateModel or SpinnerNumberModel
     */
    private Spinner(ListModel model, ListCellRenderer rendererInstance) {
        super(model);
        setRenderer(rendererInstance);
        setUIID("Spinner");
        setFixedSelection(FIXED_CENTER);
        setOrientation(VERTICAL);
        setInputOnFocus(false);
        setIsScrollVisible(false);
        DefaultListCellRenderer render = ((DefaultListCellRenderer) super.getRenderer());
        render.setRTL(false);
        render.setShowNumbers(false);
        render.setUIID("SpinnerRenderer");
        Component bgFocus = render.getListFocusComponent(this);
        bgFocus.getSelectedStyle().setBgTransparency(0);
        bgFocus.getUnselectedStyle().setBgTransparency(0);

        quickType.setReplaceMenu(false);
        quickType.setInputModeOrder(new String[]{"123"});
        quickType.setFocus(true);
        quickType.setRTL(false);
        quickType.setAlignment(LEFT);
        quickType.setConstraint(TextField.NUMERIC);
        setIgnoreFocusComponentWhenUnfocused(true);

        setRenderingPrototype(model.getItemAt(model.getSize() - 1));

        if (getRenderer() instanceof DateTimeRenderer) {
            quickType.setColumns(2);
        }
        overlayStyle = getUIManager().getComponentStyle("SpinnerOverlay");
        installDefaultPainter(overlayStyle);
    }

    /**
     * @inheritDoc
     */
    protected void initComponent() {        
        getComponentForm().registerAnimated(this);
    }

    /**
     * @inheritDoc
     */
    protected void deinitialize() {
        getComponentForm().deregisterAnimated(this);
    }

    
    /**
     * @inheritDoc
     */
    protected Dimension calcScrollSize() {
        return super.calcPreferredSize();
    }

    /**
     * @inheritDoc
     */
    protected Dimension calcPreferredSize() {
        int boxWidth = 0;
        int verticalPadding = getStyle().getPadding(false, Component.TOP) + getStyle().getPadding(false, Component.BOTTOM);
        int horizontalPadding = getStyle().getPadding(isRTL(), Component.RIGHT) + getStyle().getPadding(isRTL(), Component.LEFT);
        Object prototype = getRenderingPrototype();
        int selectedHeight;
        ListCellRenderer renderer = getRenderer();
        Component cmp;
        if (prototype != null) {
            cmp = renderer.getListCellRendererComponent(this, prototype, 0, true);
        } else {
            if (getModel().getSize() > 0) {
                cmp = renderer.getListCellRendererComponent(this, getModel().getItemAt(0), 0, true);
            } else {
                cmp = renderer.getListCellRendererComponent(this, null, 0, true);
            }
        }

        selectedHeight = cmp.getPreferredH();

        if(spinnerHandle != null) {
            if(spinnerHandle.getHeight() > selectedHeight) {
                selectedHeight = spinnerHandle.getHeight();
            }
            boxWidth += spinnerHandle.getWidth();
        }

        Dimension d;
        if(Display.getInstance().isTouchScreenDevice()) {
            d = new Dimension(cmp.getPreferredW() + boxWidth + horizontalPadding, (selectedHeight * 3 + verticalPadding));
        } else {
            d = new Dimension(cmp.getPreferredW() + boxWidth + horizontalPadding, (selectedHeight + verticalPadding));
        }
        Style style = getStyle();
        if(style.getBorder() != null) {
            d.setWidth(Math.max(style.getBorder().getMinimumWidth(), d.getWidth()));
            d.setHeight(Math.max(style.getBorder().getMinimumHeight(), d.getHeight()));
        }
        if(overlayStyle.getBorder() != null) {
            d.setWidth(Math.max(overlayStyle.getBorder().getMinimumWidth(), d.getWidth()));
            d.setHeight(Math.max(overlayStyle.getBorder().getMinimumHeight(), d.getHeight()));
        }
        return d;
    }

    /**
     * @inheritDoc
     */
    public void keyPressed(int code) {
        int game = Display.getInstance().getGameAction(code);
        if (game > 0) {
            super.keyPressed(code);
        } else {
            quickType.keyPressed(code);
            lastKeyInteraction = System.currentTimeMillis();
        }
    }

    /**
     * Set the value of the spinner to a number or a date based on the spinner type
     *
     * @param o a number or a date
     */
    public void setValue(Object o) {
        ListModel m = getModel();
        if (m instanceof SpinnerDateModel) {
            ((SpinnerDateModel) m).setValue((Date) o);
        } else {
            ((SpinnerNumberModel) m).setValue(o);
        }
    }

    /**
     * Returns the value of the spinner to a number or a date based on the spinner type
     *
     * @return a number or a date
     */
    public Object getValue() {
        ListModel m = getModel();
        if (m instanceof SpinnerDateModel) {
            return ((SpinnerDateModel) m).getValue();
        }
        return ((SpinnerNumberModel) m).getValue();
    }

    /**
     * @inheritDoc
     */
    public void keyReleased(int code) {
        int game = Display.getInstance().getGameAction(code);
        if (game > 0) {
            super.keyReleased(code);
        } else {
            try {
                quickType.keyReleased(code);
                lastKeyInteraction = System.currentTimeMillis();
                String t = quickType.getText();
                if(t.length() == 0) {
                    return;
                }

                if (getRenderer() instanceof DateTimeRenderer) {
                    // is this is a time input or a date input?
                    if(getModel() instanceof SpinnerNumberModel) {
                        int time = ((Integer)getValue()).intValue();
                        int seconds = time % 60;
                        int minutes = time / 60;
                        int hours = minutes / 60;
                        minutes %= 60;

                        switch (currentInputAlign) {
                            case LEFT:
                                hours = Integer.parseInt(t);
                                if(((DateTimeRenderer)getRenderer()).isTwentyFourHours()) {
                                    if(hours > 24) {
                                        return;
                                    }
                                } else {
                                    if(hours > 12) {
                                        return;
                                    }
                                }
                                break;
                            case CENTER:
                                minutes = Integer.parseInt(t);
                                if(minutes > 59) {
                                    return;
                                }
                                break;
                            case RIGHT:
                                seconds = Integer.parseInt(t);
                                if(seconds > 59) {
                                    return;
                                }
                                break;
                        }

                        int actual = seconds + minutes * 60 + hours * 60 * 60;
                        setValue(new Integer(actual));

                        // update the spinner positioning if we have two characters
                        if (quickType.getText().length() > 1) {
                            quickType.setText("");
                            switch (currentInputAlign) {
                                case LEFT:
                                    currentInputAlign = CENTER;
                                    break;
                                case CENTER:
                                    if(((DateTimeRenderer)getRenderer()).isShowSeconds()) {
                                        currentInputAlign = RIGHT;
                                    } else {
                                        currentInputAlign = LEFT;
                                        lastKeyInteraction = -1;
                                    }
                                    break;
                                case RIGHT:
                                    currentInputAlign = LEFT;
                                    lastKeyInteraction = -1;
                                    break;
                            }
                        }
                    } else {
                        Calendar c = Calendar.getInstance();
                        c.setTime((Date) getValue());

                        switch (currentInputAlign) {
                            case LEFT:
                                if (monthFirst) {
                                    c.set(Calendar.MONTH, Integer.parseInt(t) - 1);
                                } else {
                                    c.set(Calendar.DAY_OF_MONTH, Integer.parseInt(t));
                                }
                                break;
                            case CENTER:
                                if (monthFirst) {
                                    c.set(Calendar.DAY_OF_MONTH, Integer.parseInt(t));
                                } else {
                                    c.set(Calendar.MONTH, Integer.parseInt(t) - 1);
                                }
                                break;
                            case RIGHT:
                                int y = c.get(Calendar.YEAR);
                                c.set(Calendar.YEAR, y - (y % 100) + Integer.parseInt(t));
                                break;
                        }
                        setValue(c.getTime());

                        // update the spinner positioning if we have two characters
                        if (quickType.getText().length() > 1) {
                            quickType.setText("");
                            switch (currentInputAlign) {
                                case LEFT:
                                    currentInputAlign = CENTER;
                                    break;
                                case CENTER:
                                    currentInputAlign = RIGHT;
                                    break;
                                case RIGHT:
                                    currentInputAlign = LEFT;
                                    lastKeyInteraction = -1;
                                    break;
                            }
                        }
                    }
                    return;
                } else {
                    SpinnerNumberModel n = (SpinnerNumberModel) getModel();
                    if (n.realValues) {
                        double val = Double.parseDouble(t);
                        if(val > ((SpinnerNumberModel)getModel()).getMax() ||
                                val < ((SpinnerNumberModel)getModel()).getMin()) {
                            return;
                        }
                        setValue(new Double(val));
                    } else {
                        int val = Integer.parseInt(t);
                        if(val > ((SpinnerNumberModel)getModel()).getMax() ||
                                val < ((SpinnerNumberModel)getModel()).getMin()) {
                            return;
                        }
                        setValue(new Integer(val));
                    }
                }

                int modelSize = getModel().getSize();
                for (int iter = 0; iter < modelSize; iter++) {
                    String v = getModel().getItemAt(iter).toString();
                    if (v.startsWith(t)) {
                        setSelectedIndex(iter);
                        return;
                    }
                }
            // easier to ignore exceptions than build "proper" error handling
            } catch(IllegalArgumentException ignore) {
                ignore.printStackTrace();
            } 
        }
    }

    /**
     * @inheritDoc
     */
    public boolean isRTL() {
        // Since spinner is numeric it shouldn't be affected by RTL and should naturally be right aligned
        return false;
    }

    /**
     * @inheritDoc
     */
    public void setRTL(boolean rtl) {
        // Since spinner is numeric it shouldn't be affected by RTL and should naturally be right aligned
    }

    /**
     * @inheritDoc
     */
    public void setUIID(String id) {
        super.setUIID(id);
        overlayStyle = getUIManager().getComponentStyle(id + "Overlay");
        installDefaultPainter(overlayStyle);
    }

    /**
     * @inheritDoc
     */
    public void refreshTheme(boolean merge) {
        super.refreshTheme(merge);
        overlayStyle = getUIManager().getComponentStyle(getUIID() + "Overlay");
        installDefaultPainter(overlayStyle);
    }
    
    /**
     * @inheritDoc
     */
    public void paint(Graphics g) {
        super.paint(g);
        if(overlayStyle.getBorder() != null) {
            overlayStyle.getBorder().paintBorderBackground(g, this);
            overlayStyle.getBorder().paint(g, this);
        } else {
            overlayStyle.getBgPainter().paint(g, getBounds());
        }

        if(spinnerHandle != null) {
            Style s = getStyle();
            g.drawImage(spinnerHandle, getX() + getWidth() - spinnerHandle.getWidth() - s.getPadding(isRTL(), LEFT) - s.getPadding(isRTL(), RIGHT),
                    getY() + s.getPadding(false, TOP));
        }
        if (System.currentTimeMillis() - inputSkipDelay < lastKeyInteraction || quickType.isPendingCommit()) {
            quickType.setWidth(Math.min(getWidth(), quickType.getPreferredW()));
            quickType.setHeight(Math.min(getHeight(), quickType.getPreferredH()));
            Style s = quickType.getStyle();
            quickType.setY(getScrollY() + getY());

            // positioning based on date/time
            if (getRenderer() instanceof DateTimeRenderer) {
                switch (currentInputAlign) {
                    case LEFT:
                        quickType.setX(getX());
                        break;
                    case RIGHT:
                        quickType.setX(getX() + quickType.getStyle().getFont().charWidth(TextArea.getWidestChar()) * 4 + s.getMargin(false, RIGHT));
                        break;
                    default:
                        quickType.setX(getX() + quickType.getStyle().getFont().charWidth(TextArea.getWidestChar()) * 2 + s.getMargin(false, RIGHT));
                        break;
                }
            } else {
                quickType.setX(getX());
            }
            quickType.paintComponent(g, true);
        }
    }

    /**
     * @inheritDoc
     */
    public boolean animate() {
        boolean val = super.animate();
        if (lastKeyInteraction != -1) {
            quickType.animate();
            if (System.currentTimeMillis() - inputSkipDelay > lastKeyInteraction && !quickType.isPendingCommit()) {
                lastKeyInteraction = -1;
                quickType.clear();
                currentInputAlign = LEFT;
            }
            return true;
        }
        return val;
    }

    /**
     * The image appearing on the side of the spinner widget to indicate its "spinnability"
     *
     * @return the spinnerHandle
     */
    public static Image getSpinnerHandle() {
        return spinnerHandle;
    }

    /**
     * The image appearing on the side of the spinner widget to indicate its "spinnability"
     *
     * @param aSpinnerHandle the spinnerHandle to set
     */
    public static void setSpinnerHandle(Image aSpinnerHandle) {
        spinnerHandle = aSpinnerHandle;
    }

    /**
     * Indicates the time after which the skip input area for entering spinner values manually will disappear
     *
     * @return the inputSkipDelay
     */
    public static int getInputSkipDelay() {
        return inputSkipDelay;
    }

    /**
     * Indicates the time after which the skip input area for entering spinner values manually will disappear
     *
     * @param aInputSkipDelay the time for disappearing
     */
    public static void setInputSkipDelay(int aInputSkipDelay) {
        inputSkipDelay = aInputSkipDelay;
    }
}
