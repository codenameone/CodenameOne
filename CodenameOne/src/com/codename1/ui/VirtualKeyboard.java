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
package com.codename1.ui;

import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.animations.Transition;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Rectangle;
import com.codename1.impl.VirtualKeyboardInterface;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * This class represent the Codename One Light Weight Virtual Keyboard
 * 
 * @author Chen Fishbein
 * @deprecated this is a part of legacy code, modern devices have the virtual keyboard bound to the native text field
 */
public class VirtualKeyboard extends Dialog implements VirtualKeyboardInterface{
    private static final String MARKER_COMMIT_ON_DISPOSE = "$VKB_COM$";
    private static final String MARKER_TINT_COLOR = "$VKB_TINT$";
    private static final String MARKER_VKB = "$VKB$";
    private static Transition transitionIn = CommonTransitions.createSlide(CommonTransitions.SLIDE_VERTICAL, true, 500);
    private static Transition transitionOut = CommonTransitions.createSlide(CommonTransitions.SLIDE_VERTICAL, false, 500);
    private int inputType;
    
    /**
     * This keymap represents qwerty keyboard
     */
    public static final String[][] DEFAULT_QWERTY = new String[][]{
        {"q", "w", "e", "r", "t", "y", "u", "i", "o", "p"},
        {"a", "s", "d", "f", "g", "h", "j", "k", "l"},
        {"$Shift$", "z", "x", "c", "v", "b", "n", "m", "$Delete$"},
        {"$Mode$", "$T9$", "$Space$", "$OK$"}
    };
    /**
     * This keymap represents numbers keyboard
     */
    public static final String[][] DEFAULT_NUMBERS = new String[][]{
        {"1", "2", "3",},
        {"4", "5", "6",},
        {"7", "8", "9",},
        {"*", "0", "#",},
        {"$Mode$", "$Space$", "$Delete$", "$OK$"}
    };
    /**
     * This keymap represents numbers and symbols keyboard
     */
    public static final String[][] DEFAULT_NUMBERS_SYMBOLS = new String[][]{
        {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"},
        {"-", "/", ":", ";", "(", ")", "$", "&", "@"},
        {".", ",", "?", "!", "'", "\"", "$Delete$"},
        {"$Mode$", "$Space$", "$OK$"}
    };
    /**
     * This keymap represents symbols keyboard
     */
    public static final String[][] DEFAULT_SYMBOLS = new String[][]{
        {"[", "]", "{", "}", "#", "%", "^", "*", "+", "="},
        {"_", "\\", "|", "~", "<", ">", "\u00A3", "\u00A5"},
        {":-0", ";-)", ":-)", ":-(", ":P", ":D", "$Delete$"},
        {"$Mode$", "$Space$", "$OK$"}
    };
    /**
     * The String that represent the qwerty mode.
     */
    public static final String QWERTY_MODE = "ABC";
    /**
     * The String that represent the numbers mode.
     */
    public static final String NUMBERS_MODE = "123";
    /**
     * The String that represent the numbers sybols mode.
     */
    public static final String NUMBERS_SYMBOLS_MODE = ".,123";
    /**
     * The String that represent the symbols mode.
     */
    public static final String SYMBOLS_MODE = ".,?";
    
    private static Hashtable modesMap = new Hashtable();
    private static String[] defaultInputModeOrder = {QWERTY_MODE, 
    NUMBERS_SYMBOLS_MODE, NUMBERS_MODE, SYMBOLS_MODE};

    private String currentMode = defaultInputModeOrder[0];
    private String[] inputModeOrder = defaultInputModeOrder;
        
    private TextField inputField;
    private Container buttons = new Container(new BoxLayout(BoxLayout.Y_AXIS));
    private TextPainter txtPainter = new TextPainter();
    private boolean upperCase = false;
    private Button currentButton;

    
    public static final int INSERT_CHAR = 1;
    public static final int DELETE_CHAR = 2;
    public static final int CHANGE_MODE = 3;
    public static final int SHIFT = 4;
    public static final int OK = 5;
    public static final int SPACE = 6;
    public static final int T9 = 7;
    
    private Hashtable specialButtons = new Hashtable();
    private TextArea field;
    private boolean finishedT9Edit = true;
    private String originalText;
    private boolean useSoftKeys = false;
    private static boolean showTooltips = true;
    private boolean okPressed;


    private static Class vkbClass;
    private VirtualKeyboard vkb;
    public final static String NAME = "CodenameOne_VirtualKeyboard";
    private boolean isShowing = false;

    private static Hashtable defaultInputModes = null;

    /**
     * Creates a new instance of VirtualKeyboard 
     */
    public VirtualKeyboard() {
        setLayout(new BorderLayout());
        setDialogUIID("Container");
        getContentPane().setUIID("VKB");
        setAutoDispose(false);
        setDisposeWhenPointerOutOfBounds(true);
        setTransitionInAnimator(transitionIn);
        setTransitionOutAnimator(transitionOut);
        getTitleComponent().getParent().removeComponent(getTitleComponent());
        if(showTooltips) {
            setGlassPane(txtPainter);
        }
    }

    public void setInputType(int inputType) {
        if((inputType & TextArea.NUMERIC) == TextArea.NUMERIC || 
                (inputType & TextArea.PHONENUMBER) == TextArea.PHONENUMBER) {
            setInputModeOrder(new String []{NUMBERS_MODE});
            return;
        }
        if((inputType & TextArea.DECIMAL) == TextArea.DECIMAL) {
            setInputModeOrder(new String []{NUMBERS_SYMBOLS_MODE});
            return;
        }
        setInputModeOrder(defaultInputModeOrder);
    }

    static class InputField extends TextField {
        private TextArea field;
        InputField(TextArea field) {
            this.field = field;
            setInputMode(field.getInputMode());
            setConstraint(field.getConstraint());
        }

        public boolean hasFocus() {
            return true;
        }

        public String getUIID() {
            return "VKBTextInput";
        }

        public void deleteChar() {
            super.deleteChar();
            field.setText(getText());
            if (field instanceof TextField) {
                ((TextField) field).setCursorPosition(getCursorPosition());
            }
        }

        public void setCursorPosition(int i) {
            super.setCursorPosition(i);

            // this can happen since this method is invoked from a constructor of the base class...
            if(field != null && field.getText().length() > i && field instanceof TextField) {
                ((TextField) field).setCursorPosition(i);
            }
        }

        public void setText(String t) {
            super.setText(t);

            // this can happen since this method is invoked from a constructor of the base class...
            if(field != null) {
                // mirror events into the parent
                field.setText(t);
            }
        }

        public boolean validChar(String c) {
            if (field instanceof TextField) {
                return ((TextField) field).validChar(c);
            }
            return true;
        }
    }

    /**
     * Invoked internally by the implementation to indicate the text field that will be
     * edited by the virtual keyboard
     *
     * @param field the text field instance
     */
    public void setTextField(final TextArea field) {
        this.field = field;
        removeAll();
        okPressed = false;
        if(field instanceof TextField){
            useSoftKeys = ((TextField)field).isUseSoftkeys();
            ((TextField)field).setUseSoftkeys(false);
        }
        originalText = field.getText();
        inputField = new InputField(field);
        inputField.setText(originalText);
        inputField.setCursorPosition(field.getCursorPosition());
        inputField.setConstraint(field.getConstraint());
        inputField.setInputModeOrder(new String[]{"ABC"});
        inputField.setMaxSize(field.getMaxSize());

        initModes();
        setInputType(field.getConstraint());
        initSpecialButtons();
        addComponent(BorderLayout.NORTH, inputField);
        
        buttons.getStyle().setPadding(0, 0, 0, 0);
        addComponent(BorderLayout.CENTER, buttons);
        initInputButtons(upperCase);
        inputField.setUseSoftkeys(false);
        applyRTL(false);        
    }

    /**
     * {@inheritDoc}
     */
    public void show() {
        super.showPacked(BorderLayout.SOUTH, true);
    }

    /**
     * {@inheritDoc}
     */
    protected void autoAdjust(int w, int h) {
        //if the t9 input is currently editing do not dispose dialog
        if (finishedT9Edit) {
            setTransitionOutAnimator(CommonTransitions.createEmpty());
            dispose();
        }
    }


    /**
     * init all virtual keyboard modes, such as QWERTY_MODE, NUMBERS_SYMBOLS_MODE...
     * to add an addtitional mode a developer needs to override this method and
     * add a mode by calling addInputMode method
     */
    protected void initModes() {
        addInputMode(QWERTY_MODE, DEFAULT_QWERTY);
        addInputMode(NUMBERS_SYMBOLS_MODE, DEFAULT_NUMBERS_SYMBOLS);
        addInputMode(SYMBOLS_MODE, DEFAULT_SYMBOLS);
        addInputMode(NUMBERS_MODE, DEFAULT_NUMBERS);
        if(defaultInputModes != null) {
            Enumeration e = defaultInputModes.keys();
            while(e.hasMoreElements()) {
                String key = (String)e.nextElement();
                addInputMode(key, (String[][])defaultInputModes.get(key));
            }
        }
    }


    /**
     * Sets the current virtual keyboard mode.
     * 
     * @param mode the String that represents the mode(QWERTY_MODE, 
     * SYMBOLS_MODE, ...)
     */
    protected void setCurrentMode(String mode) {
        this.currentMode = mode;
    }

    /**
     * Gets the current mode.
     * 
     * @return the String that represents the current mode(QWERTY_MODE, 
     * SYMBOLS_MODE, ...)
     */
    protected String getCurrentMode() {
        return currentMode;
    }

    private void initInputButtons(boolean upperCase) {
        buttons.removeAll();
        int largestLine = 0;
        String[][] currentKeyboardChars = (String[][]) modesMap.get(currentMode);
        for (int i = 1; i < currentKeyboardChars.length; i++) {
            if (currentKeyboardChars[i].length > currentKeyboardChars[largestLine].length) {
                largestLine = i;
            }
        }
        int length = currentKeyboardChars[largestLine].length;
        if(length == 0) {
            return;
        }
        Button dummy = createButton(new Command("dummy"), 0);
        int buttonMargins = dummy.getUnselectedStyle().getHorizontalMargins();
        Container row = null;
        int rowW = (Display.getInstance().getDisplayWidth() -
                getDialogStyle().getPaddingLeftNoRTL() -
                getDialogStyle().getPaddingRightNoRTL() -
                getDialogStyle().getMarginLeftNoRTL() -
                getDialogStyle().getMarginLeftNoRTL());
        int availableSpace = rowW - length * buttonMargins;
        int buttonSpace = (availableSpace) / length;
        int clen = currentKeyboardChars.length;
        for (int i = 0; i < clen; i++) {
            int rowWidth = rowW;
            row = new Container(new BoxLayout(BoxLayout.X_AXIS));
            row.getUnselectedStyle().setMargin(0, 0, 0, 0);
            Vector specialsButtons = new Vector();
            int cilen = currentKeyboardChars[i].length;
            for (int j = 0; j < cilen; j++) {
                String txt = currentKeyboardChars[i][j];
                Button b = null;
                if (txt.startsWith("$") && txt.endsWith("$") && txt.length() > 1) {
                    //add a special button
                    Button cmd = (Button) specialButtons.get(txt.substring(1, txt.length() - 1));
                    Command c = null;
                    int prefW = 0;
                    if(cmd != null){
                        c = cmd.getCommand();
                        int space = ((Integer) cmd.getClientProperty("space")).intValue();
                        if (space != -1) {
                            prefW = availableSpace * space / 100;
                        }
                    }
                    b = createButton(c, prefW, "VKBSpecialButton");
                    if (prefW != 0) {
                        rowWidth -= (b.getPreferredW() + buttonMargins);
                    } else {
                        //if we can't determind the size at this stage, wait until
                        //the loops ends and give the remains size to the special
                        //button
                        specialsButtons.addElement(b);
                    }
                } else {
                    if (upperCase) {
                        txt = txt.toUpperCase();
                    }
                    b = createInputButton(txt, buttonSpace);
                    rowWidth -= (b.getPreferredW() + buttonMargins);
                }
                if (currentButton != null) {
                    if (currentButton.getCommand() != null && 
                            b.getCommand() != null &&
                            currentButton.getCommand().getId() == b.getCommand().getId()) {
                        currentButton = b;
                    }
                    if (currentButton.getText().equals(b.getText())) {
                        currentButton = b;
                    }
                }
                row.addComponent(b);
            }
            int emptySpace = Math.max(rowWidth, 0);
            //if we have special buttons on the keyboard give them the size or 
            //else give the remain size to the row margins
            if (specialsButtons.size() > 0) {
                int prefW = emptySpace / specialsButtons.size();
                for (int j = 0; j < specialsButtons.size(); j++) {
                    Button special = (Button) specialsButtons.elementAt(j);
                    special.setPreferredW(prefW);
                }

            } else {
                row.getUnselectedStyle().setPadding(Component.LEFT, 0);
                row.getUnselectedStyle().setPadding(Component.RIGHT, 0);
                row.getUnselectedStyle().setMarginUnit(new byte[]{Style.UNIT_TYPE_PIXELS, 
                    Style.UNIT_TYPE_PIXELS, 
                    Style.UNIT_TYPE_PIXELS, 
                    Style.UNIT_TYPE_PIXELS});
                        row.getUnselectedStyle().setMargin(Component.LEFT, emptySpace / 2);
                row.getUnselectedStyle().setMargin(Component.RIGHT, emptySpace / 2);
            }
            buttons.addComponent(row);
        }
        applyRTL(false);
    }

    private Button createInputButton(String text, int prefSize) {
        Button b = createButton(new Command(text, INSERT_CHAR), prefSize);
        b.putClientProperty("glasspane", "true");
        return b;
    }

    private Button createButton(Command cmd, int prefSize) {
        return createButton(cmd, prefSize, "VKBButton");
    }

    private Button createButton(Command cmd, int prefSize, String uiid) {
        Button btn;
        if(cmd != null){
            btn = new Button(cmd);
        }else{
            btn = new Button();
            btn.setVisible(false);
        }
        final Button b = btn;
        b.setUIID(uiid);
        b.setEndsWith3Points(false);
        b.setAlignment(Component.CENTER);
        prefSize = Math.max(prefSize, b.getPreferredW());
        b.setPreferredW(prefSize);
        b.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                currentButton = b;
            }
        });

        return b;
    }

    /**
     * Add an input mode to the virtual keyboard
     *
     * @param mode a string that represents the identifier of the mode
     * @param inputChars 2 dimensional String array that contains buttons String
     * and special buttons (a special button is identified with $...$ marks
     * e.g: "$Space$")
     */
    public static void addDefaultInputMode(String mode, String[][] inputChars) {
        if(defaultInputModes == null) {
            defaultInputModes = new Hashtable();
        }
        defaultInputModes.put(mode, inputChars);
    }

    /**
     * Add an input mode to the virtual keyboard 
     * 
     * @param mode a string that represents the identifier of the mode
     * @param inputChars 2 dimentional String array that contains buttons String
     * and special buttons (a special button is identified with $...$ marks 
     * e.g: "$Space$")
     */
    public void addInputMode(String mode, String[][] inputChars) {
        modesMap.put(mode, inputChars);
    }

    /**
     * This method adds a special button to the virtual keyboard
     * 
     * @param key the string identifier from within the relevant input mode
     * @param cmd the Command to invoke when this button is invoked.
     */
    public void addSpecialButton(String key, Command cmd) {
        addSpecialButton(key, cmd, -1);
    }

    /**
     * This method adds a special button to the virtual keyboard
     * 
     * @param key the string identifier from within the relevant input mode
     * @param cmd the Command to invoke when this button is invoked.
     * @param space how much space in percentage from the overall row 
     * the special button should occupy
     */
    public void addSpecialButton(String key, Command cmd, int space) {
        Button b = new Button(cmd);
        b.putClientProperty("space", new Integer(space));
        specialButtons.put(key, b);
    }

    private String getNextMode(String current) {
        
        for (int i = 0; i < inputModeOrder.length - 1; i++) {
            String mode = inputModeOrder[i];
            if(mode.equals(current)){
                return inputModeOrder[i + 1];
            }
        }
        return inputModeOrder[0];
    }

    /**
     * {@inheritDoc}
     */
    public void pointerPressed(int x, int y) {
        super.pointerPressed(x, y);
        Component cmp = getComponentAt(x, y);
        if (showTooltips && cmp != null && cmp instanceof Button && cmp.getClientProperty("glasspane") != null) {
            txtPainter.showButtonOnGlasspane((Button) cmp);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void pointerDragged(int x, int y) {
        super.pointerDragged(x, y);
        Component cmp = getComponentAt(x, y);
        if (showTooltips && cmp != null && cmp instanceof Button && cmp.getClientProperty("glasspane") != null) {
            txtPainter.showButtonOnGlasspane((Button) cmp);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void pointerReleased(int x, int y) {
        if(showTooltips) {
            txtPainter.clear();
        }
        super.pointerReleased(x, y);
    }

    /**
     * This method initialize all the virtual keyboard special buttons.
     */
    protected void initSpecialButtons() {
        //specialButtons.clear();
        addSpecialButton("Shift", new Command("SH", SHIFT), 15);
        addSpecialButton("Delete", new Command("Del", DELETE_CHAR), 15);
        addSpecialButton("T9", new Command("T9", T9), 15);
        addSpecialButton("Mode", new Command(getNextMode(currentMode), CHANGE_MODE));
        addSpecialButton("Space", new Command("Space", SPACE), 50);
        addSpecialButton("OK", new Command("Ok", OK));
    }
    
    /**
     * Returns the order in which input modes are toggled
     *
     * @return the order of the input modes
     */
    public String[] getInputModeOrder() {
        return inputModeOrder;
    }

  /**
     * Sets the order in which input modes are toggled and allows disabling/hiding
     * an input mode
     * 
     * @param order the order for the input modes in this field
     */
    public void setInputModeOrder(String[] order) {
        inputModeOrder = order;
        setCurrentMode(order[0]);
    }
    
  /**
     * Returns the order in which input modes are toggled by default
     * 
     * @return the default order of the input mode
     */
    public static String[] getDefaultInputModeOrder() {
        return defaultInputModeOrder;
    }
    
  /**
     * Sets the order in which input modes are toggled by default and allows 
     * disabling/hiding an input mode
     * 
     * @param order the order for the input modes in all future created fields
     */
    public static void setDefaultInputModeOrder(String[] order) {
        defaultInputModeOrder = order;
    }

    class TextPainter implements Painter {

        private Label label = new Label();
        private boolean paint = true;

        public TextPainter() {
            label = new Label();
            label.setUIID("VKBtooltip");
        }

        public void showButtonOnGlasspane(Button button) {
            if(label.getText().equals(button.getText())){
                return;
            }
            paint = true;
            repaint(label.getAbsoluteX()-2,
                    label.getAbsoluteY()-2,
                    label.getWidth()+4,
                    label.getHeight()+4);
            label.setText(button.getText());
            label.setSize(label.getPreferredSize());
            label.setX(button.getAbsoluteX() + (button.getWidth() - label.getWidth()) / 2);
            label.setY(button.getAbsoluteY() - label.getPreferredH() * 4 / 3);
            repaint(label.getAbsoluteX()-2,
                    label.getAbsoluteY()-2,
                    label.getPreferredW()+4,
                    label.getPreferredH()+4);
        }

        public void paint(Graphics g, Rectangle rect) {
            if (paint) {
                label.paintComponent(g);
            }

        }

        private void clear() {
            paint = false;
            repaint();
        }
    }

    private void updateText(String txt) {
        field.setText(txt);
        if(field instanceof TextField){
            ((TextField)field).setCursorPosition(txt.length());
        }
        if(okPressed){
            field.fireActionEvent();
            if(field instanceof TextField){
                ((TextField)field).fireDoneEvent();
            }
            
        }
    }
    /**
     * {@inheritDoc}
     */
    protected void actionCommand(Command cmd) {
        super.actionCommand(cmd);

        switch (cmd.getId()) {
            case OK:
                okPressed = true;
                updateText(inputField.getText());
                dispose();
                break;
            case INSERT_CHAR:
                Button btn = currentButton;
                String text = btn.getText();
                if (inputField.getText().length() == 0) {
                    inputField.setText(text);
                    inputField.setCursorPosition(text.length());
                } else {
                    inputField.insertChars(text);
                }
                break;
            case SPACE:
                if (inputField.getText().length() == 0) {
                    inputField.setText(" ");
                } else {
                    inputField.insertChars(" ");
                }
                break;
            case DELETE_CHAR:
                inputField.deleteChar();
                break;
            case CHANGE_MODE:
                currentMode = getNextMode(currentMode);
                Display.getInstance().callSerially(new Runnable() {

                    public void run() {
                        initInputButtons(upperCase);
                        String next = getNextMode(currentMode);
                        currentButton.setText(next);
                        currentButton.getCommand().setCommandName(next);
                        setTransitionOutAnimator(CommonTransitions.createEmpty());
                        setTransitionInAnimator(CommonTransitions.createEmpty());
                        revalidate();
                        show();
                    }
                });
                return;
            case SHIFT:
                if (currentMode.equals(QWERTY_MODE)) {
                    upperCase = !upperCase;
                    Display.getInstance().callSerially(new Runnable() {

                        public void run() {
                            initInputButtons(upperCase);
                            revalidate();
                        }
                    });
                }
                return;
            case T9:
                finishedT9Edit = false;     
                if(field != null){
                    Display.getInstance().editString(field, field.getMaxSize(), field.getConstraint(), field.getText());
                }else{
                    Display.getInstance().editString(inputField, inputField.getMaxSize(), inputField.getConstraint(), inputField.getText());
                }
                dispose();
                finishedT9Edit = true;
        }
    }

    
    /**
     * {@inheritDoc}
     */
    public void dispose() {
        if (field != null) {
            if (!okPressed && !isCommitOnDispose(field) && finishedT9Edit) {
                field.setText(originalText);
            }
            if(field instanceof TextField){
                ((TextField)field).setUseSoftkeys(useSoftKeys);
            }
            setTransitionInAnimator(transitionIn);
            field = null;
        }
        currentMode = inputModeOrder[0];
        super.dispose();
    }

    /**
     * {@inheritDoc}
     */
    protected void onShow() {
        super.onShow();
        setTransitionOutAnimator(transitionOut);
    }
    

    /**
     * This method returns the Virtual Keyboard TextField.
     * 
     * @return the the Virtual Keyboard TextField.
     */
    protected TextField getInputField() {
        return inputField;
    }

    /**
     * Indicates whether the VKB should commit changes to the text field when the VKB
     * is closed not via the OK button. This might be useful for some situations such
     * as searches
     *
     * @param tf the text field to mark as commit on dispose
     * @param b the value of commit on dispose, true to always commit changes
     */
    public static void setCommitOnDispose(TextField tf, boolean b) {
        tf.putClientProperty(MARKER_COMMIT_ON_DISPOSE, new Boolean(b));
    }

    /**
     * This method is used to bind a specific instance of a virtual keyboard to a specific TextField.
     * For example if a specific TextField requires only numeric input consider using this method as follows:
     * 
     * TextField tf = new TextField();
     * tf.setConstraint(TextField.NUMERIC);
     * tf.setInputModeOrder(new String[]{"123"});
     * VirtualKeyboard vkb = new VirtualKeyboard();
     * vkb.setInputModeOrder(new String[]{VirtualKeyboard.NUMBERS_MODE});
     * VirtualKeyboard.bindVirtualKeyboard(tf, vkb);
     * 
     * @param t the TextField to bind a VirualKeyboard to.
     * @param vkb the binded VirualKeyboard.
     */
    public static void bindVirtualKeyboard(TextArea t, VirtualKeyboard vkb) {
        t.putClientProperty(MARKER_VKB, vkb);
    }

    /**
     *  This method returns the Textfield associated  VirtualKeyboard, 
     * see bindVirtualKeyboard(TextField tf, VirtualKeyboard vkb) method.
     * 
     * @param t a TextField.that might have an associated VirtualKeyboard instance
     * @return a VirtualKeyboard instance or null if not exists.
     */
    public static VirtualKeyboard getVirtualKeyboard(TextArea t) {
        return (VirtualKeyboard) t.getClientProperty(MARKER_VKB);
    }

    /**
     * Indicates whether the given text field should commit on dispose
     *
     * @param tf the text field
     * @return true if the text field should save the data despite the fact that it
     * was disposed in an irregular way
     */
    public static boolean isCommitOnDispose(TextArea tf) {
        Boolean b = (Boolean)tf.getClientProperty(MARKER_COMMIT_ON_DISPOSE);
        return (b != null) && b.booleanValue();
    }

    /**
     * Sets the tint color for the virtual keyboard when shown on top of this text field
     * see the form tint methods for more information
     *
     * @param tf the relevant text field
     * @param tint the tint color with an alpha channel
     */
    public static void setVKBTint(TextField tf, int tint) {
        tf.putClientProperty(MARKER_TINT_COLOR, new Integer(tint));
    }

    /**
     * The tint color for the virtual keyboard when shown on top of this text field
     * see the form tint methods for more information
     *
     * @param tf the relevant text field
     * @return  the tint color with an alpha channel
     */
    public static int getVKBTint(TextArea tf) {
        Integer v = (Integer)tf.getClientProperty(MARKER_TINT_COLOR);
        if(v != null) {
            return v.intValue();
        }
       Form current = Display.getInstance().getCurrent();
        return current.getUIManager().getLookAndFeel().getDefaultFormTintColor();
    }


    /**
     * Indicates whether tooltips should be shown when the keys in the VKB are pressed
     *
     * @return the showTooltips
     */
    public static boolean isShowTooltips() {
        return showTooltips;
    }

    /**
     * Indicates whether tooltips should be shown when the keys in the VKB are pressed
     *
     * @param aShowTooltips true to show tooltips
     */
    public static void setShowTooltips(boolean aShowTooltips) {
        showTooltips = aShowTooltips;
    }

    /**
     * The transition in for the VKB
     *
     * @return the transitionIn
     */
    public static Transition getTransitionIn() {
        return transitionIn;
    }

    /**
     * The transition in for the VKB
     *
     * @param aTransitionIn the transitionIn to set
     */
    public static void setTransitionIn(Transition aTransitionIn) {
        transitionIn = aTransitionIn;
    }

    /**
     * The transition out for the VKB
     *
     * @return the transitionOut
     */
    public static Transition getTransitionOut() {
        return transitionOut;
    }

    /**
     * The transition out for the VKB
     *
     * @param aTransitionOut the transitionOut to set
     */
    public static void setTransitionOut(Transition aTransitionOut) {
        transitionOut = aTransitionOut;
    }
    
    /**
     * Shows the virtual keyboard that is assoiciated with the displayed TextField
     * or displays the default virtual keyboard.
     * 
     * @param show it show is true open the relevant keyboard, if close dispose 
     * the displayed keyboard
     */
    public void showKeyboard(boolean show) {
        isShowing = show;
        Form current = Display.getInstance().getCurrent();
        if (show) {
            Component foc = current.getFocused();
            if(foc instanceof Container) {
                foc = ((Container)foc).getLeadComponent();
            }
            TextArea txtCmp = (TextArea) foc;
            if (txtCmp != null) {
                if(vkb != null && vkb.contains(txtCmp)){
                    return;
                }                
                vkb = VirtualKeyboard.getVirtualKeyboard(txtCmp);
                if(vkb == null){
                    vkb = createVirtualKeyboard();
                }
                
                vkb.setTextField(txtCmp);
                int oldTint = current.getTintColor();
                current.setTintColor(VirtualKeyboard.getVKBTint(txtCmp));
                boolean third = com.codename1.ui.Display.getInstance().isThirdSoftButton();
                com.codename1.ui.Display.getInstance().setThirdSoftButton(false);
                boolean qwerty = txtCmp.isQwertyInput();
                if(txtCmp instanceof TextField){
                    ((TextField) txtCmp).setQwertyInput(true);
                }
                vkb.showDialog();
                if (txtCmp instanceof TextField) {
                    ((TextField) txtCmp).setQwertyInput(qwerty);
                }
                com.codename1.ui.Display.getInstance().setThirdSoftButton(third);
                current.setTintColor(oldTint);
            }
        }
    }

    /**
     * Sets the default virtual keyboard class for the com.codename1.ui.VirtualKeyboard
     * type
     * This class is used as the default virtual keyboard class if the current
     * platform VirtualKeyboard is com.codename1.ui.VirtualKeyboard.
     * Platform VirtualKeyboard is defined here:
     * Display.getIntance().setDefaultVirtualKeyboard(VirtualKeyboardInterface vkb)
     * 
     * @param vkbClazz this class must extend VirtualKeyboard.
     */
    public static void setDefaultVirtualKeyboardClass(Class vkbClazz){
        vkbClass = vkbClazz;
    }
    
    private VirtualKeyboard createVirtualKeyboard() {
        try {
            if(vkbClass != null){
                return (VirtualKeyboard) vkbClass.newInstance();
            }else{
                return new VirtualKeyboard();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return new VirtualKeyboard();
        } 
    }

    /**
     * @see VirtualKeyboardInterface
     */
    public String getVirtualKeyboardName() {
        return NAME;
    }

    /**
     * @see VirtualKeyboardInterface
     */
    public boolean isVirtualKeyboardShowing() {
        return isShowing;
    }
    
}
