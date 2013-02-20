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
package com.codename1.ui.util;

import com.codename1.analytics.AnalyticsService;
import com.codename1.ui.Button;
import com.codename1.ui.CheckBox;
import com.codename1.ui.ComboBox;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.List;
import com.codename1.ui.RadioButton;
import com.codename1.ui.Slider;
import com.codename1.ui.Tabs;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.animations.Transition;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.DataChangedListener;
import com.codename1.ui.events.FocusListener;
import com.codename1.ui.events.SelectionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.list.CellRenderer;
import com.codename1.ui.list.ContainerList;
import com.codename1.ui.list.DefaultListModel;
import com.codename1.ui.list.GenericListCellRenderer;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.spinner.BaseSpinner;
import com.codename1.ui.table.TableLayout;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * The UI builder can create a user interface based on the UI designed in the
 * resource editor and allows us to bind to said UI. Notice that if a Component
 * was used in the GUI that is not a part of the com.codename1.ui package (even a
 * Component from sub packages such as table or tree) it MUST be registered
 * before loading a GUI!
 *
 * @author Shai Almog
 */
public class UIBuilder {
    /**
     * A key in the form state hashtable used in the back command navigation
     */
    public static final String FORM_STATE_KEY_NAME = "$name";

    /**
     * A key in the form state hashtable used in the back command navigation
     */
    public static final String FORM_STATE_KEY_TITLE = "$title";

    /**
     * A key in the form state hashtable used in the back command navigation
     */
    public static final String FORM_STATE_KEY_FOCUS = "$focus";

    /**
     * A key in the form state hashtable used in the back command navigation
     */
    public static final String FORM_STATE_KEY_SELECTION = "$sel";

    /**
     * A key in the form state hashtable used in the back command navigation
     */
    private static final String FORM_STATE_KEY_CONTAINER = "$cnt";

    private static Hashtable componentRegistry;

    public static final int BACK_COMMAND_ID = 99999999;
    private static boolean blockAnalytics;

    private static final String COMMAND_ACTION = "$COMMAND_ACTION$";
    private static final String COMMAND_ARGUMENTS = "$COMMAND_ARGUMENTS$";
    private static final String TYPE_KEY = "$TYPE_NAME$";
    private static final String EMBEDDED_FORM_FLAG = "$EMBED$";
    static final int PROPERTY_CUSTOM = 1000;
    static final int PROPERTY_TEXT = 1;
    static final int PROPERTY_ALIGNMENT = 2;
    static final int PROPERTY_LAYOUT = 3;
    static final int PROPERTY_LABEL_FOR = 4;
    static final int PROPERTY_PREFERRED_WIDTH = 5;
    static final int PROPERTY_PREFERRED_HEIGHT = 6;
    static final int PROPERTY_NEXT_FOCUS_UP = 7;
    static final int PROPERTY_NEXT_FOCUS_DOWN = 8;
    static final int PROPERTY_NEXT_FOCUS_LEFT = 9;
    static final int PROPERTY_NEXT_FOCUS_RIGHT = 10;
    static final int PROPERTY_UIID = 11;
    static final int PROPERTY_FOCUSABLE = 14;
    static final int PROPERTY_ENABLED = 15;
    static final int PROPERTY_SCROLL_VISIBLE = 16;
    static final int PROPERTY_ICON = 17;
    static final int PROPERTY_GAP = 18;
    static final int PROPERTY_VERTICAL_ALIGNMENT = 19;
    static final int PROPERTY_TEXT_POSITION = 20;
    static final int PROPERTY_NAME = 21;
    static final int PROPERTY_LAYOUT_CONSTRAINT = 22;
    static final int PROPERTY_TITLE = 23;
    static final int PROPERTY_COMPONENTS = 24;
    static final int PROPERTY_COLUMNS = 25;
    static final int PROPERTY_ROWS = 26;
    static final int PROPERTY_HINT = 27;
    static final int PROPERTY_ITEM_GAP = 28;
    static final int PROPERTY_CYCLIC_FOCUS = 32;
    static final int PROPERTY_SCROLLABLE_X = 33;
    static final int PROPERTY_SCROLLABLE_Y = 34;
    static final int PROPERTY_NEXT_FORM = 35;
    static final int PROPERTY_RADIO_GROUP = 36;
    static final int PROPERTY_SELECTED = 37;

    static final int PROPERTY_LIST_ITEMS_LEGACY = 29;
    static final int PROPERTY_LIST_ITEMS = 38;
    static final int PROPERTY_LIST_RENDERER = 39;
    static final int PROPERTY_BASE_FORM = 40;
    static final int PROPERTY_ROLLOVER_ICON = 41;
    static final int PROPERTY_PRESSED_ICON = 42;
    static final int PROPERTY_RTL = 43;
    static final int PROPERTY_INFINITE = 44;
    static final int PROPERTY_PROGRESS = 45;
    static final int PROPERTY_VERTICAL = 46;
    static final int PROPERTY_EDITABLE = 47;
    static final int PROPERTY_INCREMENTS = 48;
    static final int PROPERTY_RENDER_PERCENTAGE_ON_TOP = 49;
    static final int PROPERTY_MAX_VALUE = 50;
    static final int PROPERTY_MIN_VALUE = 51;
    static final int PROPERTY_DIALOG_UIID = 52;
    static final int PROPERTY_LIST_FIXED = 53;
    static final int PROPERTY_LIST_ORIENTATION = 54;
    static final int PROPERTY_LEAD_COMPONENT = 55;
    static final int PROPERTY_TAB_PLACEMENT = 56;
    static final int PROPERTY_TAB_TEXT_POSITION = 57;
    static final int PROPERTY_TOGGLE_BUTTON = 58;
    static final int PROPERTY_DISABLED_ICON = 60;
    static final int PROPERTY_EMBED = 61;
    static final int PROPERTY_HINT_ICON = 62;
    static final int PROPERTY_SLIDER_THUMB = 63;
    static final int PROPERTY_DIALOG_POSITION = 64;
    static final int PROPERTY_TEXT_AREA_GROW = 65;
    static final int PROPERTY_COMMANDS_LEGACY = 30;
    static final int PROPERTY_COMMAND_LEGACY = 31;
    static final int PROPERTY_COMMANDS = 67;
    static final int PROPERTY_COMMAND = 66;
    static final int PROPERTY_TEXT_CONSTRAINT = 68;
    static final int PROPERTY_TEXT_MAX_LENGTH = 69;
    static final int PROPERTY_TENSILE_DRAG_ENABLED = 70;
    static final int PROPERTY_TACTILE_TOUCH = 71;
    static final int PROPERTY_SNAP_TO_GRID = 72;
    static final int PROPERTY_FLATTEN = 73;
    static final int PROPERTY_DISPOSE_WHEN_POINTER_OUT = 74;

    static final int PROPERTY_CLOUD_BOUND_PROPERTY = 75;
    static final int PROPERTY_CLOUD_DESTINATION_PROPERTY = 76;

    static final int LAYOUT_BOX_X = 5002;
    static final int LAYOUT_BOX_Y = 5003;
    static final int LAYOUT_GRID = 5006;
    static final int LAYOUT_TABLE = 5007;

    static final int LAYOUT_BORDER_LEGACY = 5001;
    static final int LAYOUT_BORDER_ANOTHER_LEGACY = 5008;
    static final int LAYOUT_BORDER = 5010;
    static final int LAYOUT_FLOW_LEGACY = 5004;
    static final int LAYOUT_FLOW = 5009;
    static final int LAYOUT_LAYERED = 5011;

    /**
     * Enables blocking analytics in the UIBuilder, this is useful for the designer tool.
     * @return the blockAnalytics
     */
    public static boolean isBlockAnalytics() {
        return blockAnalytics;
    }

    /**
     * Enables blocking analytics in the UIBuilder, this is useful for the designer tool.
     * @param aBlockAnalytics the blockAnalytics to set
     */
    public static void setBlockAnalytics(boolean aBlockAnalytics) {
        blockAnalytics = aBlockAnalytics;
    }

    private String resourceFilePath;
    private Resources resourceFile;
    private Hashtable localCommandListeners;
    private EventDispatcher globalCommandListeners;
    private Hashtable localComponentListeners;
    private boolean keepResourcesInRam = Display.getInstance().getProperty("cacheResFile", "false").equals("true");

    // used by the resource editor
    static boolean ignorBaseForm;

    private Vector baseFormNavigationStack = new Vector();
    private Vector backCommands;

    /**
     * When reaching the home form the navigation stack is cleared
     */
    private String homeForm;

    private static Hashtable getComponentRegistry() {
        if(componentRegistry == null) {
            componentRegistry = new Hashtable();
            componentRegistry.put("Button", Button.class);
            componentRegistry.put("Calendar", com.codename1.ui.Calendar.class);
            componentRegistry.put("CheckBox", CheckBox.class);
            componentRegistry.put("ComboBox", ComboBox.class);
            componentRegistry.put("Container", Container.class);
            componentRegistry.put("Dialog", Dialog.class);
            componentRegistry.put("Form", Form.class);
            componentRegistry.put("Label", Label.class);
            componentRegistry.put("List", List.class);
            componentRegistry.put("RadioButton", RadioButton.class);
            componentRegistry.put("Slider", Slider.class);
            componentRegistry.put("Tabs", Tabs.class);
            componentRegistry.put("TextArea", TextArea.class);
            componentRegistry.put("TextField", TextField.class);
            componentRegistry.put("EmbeddedContainer", EmbeddedContainer.class);
        }
        return componentRegistry;
    }

    /**
     * Seamlessly inserts a back command to all the forms
     * 
     * @param back true to automatically add a back command
     */
    public void setBackCommandEnabled(boolean back) {
        if(back) {
            if(baseFormNavigationStack == null) {
                baseFormNavigationStack = new Vector();
            }
        } else {
            baseFormNavigationStack = null;
        }
    }

    /**
     * Removes a navigation frame from the stack, this is useful in case you
     * want to go back to a form in the middle of the navigation stack.
     */
    protected void popNavigationStack() {
        if(baseFormNavigationStack != null && baseFormNavigationStack.size() > 0) {
            baseFormNavigationStack.removeElementAt(baseFormNavigationStack.size() - 1);
        }
    }
    
    /**
     * Pops the navigation stack until it finds form name and the back button will match form name
     * if form name isn't in the stack this method will fail
     * @param formName the name of the form to navigate back to.
     */
    protected void setBackDestination(String formName) {
        if(baseFormNavigationStack != null) {
            while(baseFormNavigationStack.size() > 0) {
                Hashtable h = (Hashtable)baseFormNavigationStack.elementAt(baseFormNavigationStack.size() - 1);
                if(formName.equalsIgnoreCase((String)h.get(FORM_STATE_KEY_NAME))) {
                    break;
                }
                baseFormNavigationStack.removeElementAt(baseFormNavigationStack.size() - 1);
            }
        }
    }
    
    private Vector getFormNavigationStackForComponent(Component c) {
        if(baseFormNavigationStack == null) {
            return null;
        }
        if(c == null) {
            return baseFormNavigationStack;
        }
        Component root = getRootAncestor(c);
        if(root.getParent() instanceof EmbeddedContainer) {
            Vector nav = (Vector)root.getParent().getClientProperty("$baseNav");
            if(nav == null) {
                nav = new Vector();
                root.getParent().putClientProperty("$baseNav", nav);
            }
            return nav;
        }
        return baseFormNavigationStack;
    }

    /**
     * Seamlessly inserts a back command to all the forms
     *
     * @return true if a back command is automatically added
     */
    public boolean isBackCommandEnabled() {
        return baseFormNavigationStack != null;
    }

    /**
     * This method  allows the UIBuilder to package a smaller portion of Codename One into the JAR
     * and add support for additional 3rd party components to the GUI builder. Components
     * must be registered using their UIID name, by default all the content of com.codename1.ui is
     * registered however subpackages and 3rd party components are not.
     * Registeration is essential for obfuscation to work properly!
     *
     * @param name the name of the component (UIID)
     * @param cmp the class for the given component
     */
    public static void registerCustomComponent(String name, Class cmp) {
        getComponentRegistry().put(name, cmp);
    }

    /**
     * Invokes the analytics service if it is enabled and if 
     * @param page the page visited
     * @param referrer  the source page
     */
    protected void analyticsCallback(String page, String referrer) {
        if(!isBlockAnalytics() && AnalyticsService.isEnabled()) {
            AnalyticsService.visit(page, referrer);
        }
    }
    
    /**
     * Creates the container defined under the given name in the res file
     *
     * @param resPath the path to the res file containing the UI widget
     * @param resourceName the name of the widget in the res file
     * @return a Codename One container instance
     */
    public Container createContainer(String resPath, String resourceName) {
        if(this.resourceFilePath == null || (!this.resourceFilePath.equals(resPath))) {
            resourceFile = null;
        } 
        setResourceFilePath(resPath);
        return createContainer(fetchResourceFile(), resourceName);
    }

    /**
     * Creates the container defined under the given name in the res file
     *
     * @param res the res file containing the UI widget
     * @param resourceName the name of the widget in the res file
     * @return a Codename One container instance
     */
    public Container createContainer(Resources res, String resourceName) {
        return createContainer(res, resourceName, null);
    }

    private Container createContainer(Resources res, String resourceName, EmbeddedContainer parentContainer) {
        onCreateRoot(resourceName);
        DataInputStream in = new DataInputStream(res.getUi(resourceName));
        try {
            Hashtable h = null;
            if(localComponentListeners != null) {
                h = (Hashtable)localComponentListeners.get(resourceName);
            }
            Container c = (Container)createComponent(in, null, null, res, h, parentContainer);
            c.setName(resourceName);
            postCreateComponents(in, c, res);

            // try to be smart about initializing the home form
            if(homeForm == null) {
                if(c instanceof Form) {
                    String nextForm = (String)c.getClientProperty("%next_form%");
                    if(nextForm != null) {
                        homeForm = nextForm;
                    } else {
                        homeForm = resourceName;
                    }
                }
            }

            return c;
        } catch (Exception ex) {
            // If this happens its probably a serious bug
            ex.printStackTrace();
            return null;
        }
    }

    private void readCommand(DataInputStream in, Component c, Container parent, Resources res, boolean legacy) throws IOException {
        String commandName = in.readUTF();
        String commandImageName = in.readUTF();
        String rollover = null;
        String pressed = null;
        String disabled = null;
        if(!legacy) {
            rollover = in.readUTF();
            pressed = in.readUTF();
            disabled = in.readUTF();
        }
        int commandId = in.readInt();
        String commandAction = in.readUTF();
        String commandArgument = "";
        if(commandAction.equals("$Execute")) {
            commandArgument = in.readUTF();
        }
        boolean isBack = in.readBoolean();
        Command cmd = createCommandImpl(commandName, res.getImage(commandImageName), commandId, commandAction, isBack, commandArgument);
        if(rollover != null && rollover.length() > 0) {
            cmd.setRolloverIcon(res.getImage(rollover));
        }
        if(pressed != null && pressed.length() > 0) {
            cmd.setPressedIcon(res.getImage(pressed));
        }
        if(disabled != null && disabled.length() > 0) {
            cmd.setPressedIcon(res.getImage(pressed));
        }
        if(isBack) {
            Form f = c.getComponentForm();
            if(f != null) {
                f.setBackCommand(cmd);
            } else {
                if(backCommands == null) {
                    backCommands = new Vector();
                }
                backCommands.addElement(cmd);
            }
        }
        Button btn;
        if(c instanceof Container) {
            btn = (Button)((Container)c).getLeadComponent();
        } else {
            btn = ((Button)c);
        }
        btn.setCommand(cmd);

        // prevent duplicate action handling only in the case of a component form
        // the embeded component doesn't have a global command listener since it has
        // no menu
        if(c.getComponentForm() != null) {
            btn.removeActionListener(getFormListenerInstance(parent, null));
        }
        cmd.putClientProperty(COMMAND_ARGUMENTS, commandArgument);
        cmd.putClientProperty(COMMAND_ACTION, commandAction);
        if(commandAction.length() > 0 && resourceFilePath == null || isKeepResourcesInRam()) {
            resourceFile = res;
        }
    }

    /**
     * Invoked after the components were created to allow properties that require the entire
     * tree to exist to update the component. This is useful for properties that point
     * at other components.
     */
    private void postCreateComponents(DataInputStream in, Container parent, Resources res) throws Exception {
        // finds the component whose properties need to update
        String name = in.readUTF();
        Component lastComponent = null;
        while(name.length() > 0) {
            if(lastComponent == null || !lastComponent.getName().equals(name)) {
                lastComponent = findByName(name, parent);
            }
            Component c = lastComponent;
            int property = in.readInt();
            modifyingProperty(c, property);

            switch(property) {
                case PROPERTY_COMMAND_LEGACY: {
                    readCommand(in, c, parent, res, true);
                    break;
                }
                case PROPERTY_COMMAND: {
                    readCommand(in, c, parent, res, false);
                    break;
                }
                case PROPERTY_LABEL_FOR:
                    c.setLabelForComponent((Label)findByName(in.readUTF(), parent));
                    break;
                case PROPERTY_LEAD_COMPONENT:
                    ((Container)c).setLeadComponent(findByName(in.readUTF(), parent));
                    break;
                case PROPERTY_NEXT_FOCUS_UP:
                    c.setNextFocusUp(findByName(in.readUTF(), parent));
                    break;
                case PROPERTY_NEXT_FOCUS_DOWN:
                    c.setNextFocusDown(findByName(in.readUTF(), parent));
                    break;
                case PROPERTY_NEXT_FOCUS_LEFT:
                    c.setNextFocusLeft(findByName(in.readUTF(), parent));
                    break;
                case PROPERTY_NEXT_FOCUS_RIGHT:
                    c.setNextFocusRight(findByName(in.readUTF(), parent));
                    break;
            }

            name = in.readUTF();
        }
    }

    /**
     * Finds the given component by its name
     * 
     * @param name the name of the component as defined in the resource editor
     * @param rootComponent the root container
     * @return the component matching the given name or null if its not found
     */
    public Component findByName(String name, Component rootComponent) {
        Component c = (Component)rootComponent.getClientProperty("%" + name + "%");
        if(c == null) {
            Container newRoot = getRootAncestor(rootComponent);
            if(newRoot != null && rootComponent != newRoot) {
                return findByName(name, newRoot);
            }
        }
        return c;
    }

    /**
     * Finds the given component by its name
     * 
     * @param name the name of the component as defined in the resource editor
     * @param rootComponent the root container
     * @return the component matching the given name or null if its not found
     */
    public Component findByName(String name, Container rootComponent) {
        Component c = (Component)rootComponent.getClientProperty("%" + name + "%");
        if(c == null) {
            Container newRoot = getRootAncestor(rootComponent);
            if(newRoot != null && rootComponent != newRoot) {
                return findByName(name, newRoot);
            }
        }
        return c;
    }

    /**
     * This method can be overriden to create custom components in a custom way, the component
     * type is a shorthand for the component name and not the full name of the class.
     * By default this method returns null which indicates Codename One should try to reolve the component
     * on its own.
     * 
     * @param componentType the type of the component from the UI builder
     * @param cls assumed component class based on the component registry
     * @return a new component instance or null
     */
    protected Component createComponentInstance(String componentType, Class cls) {
        return null;
    }

    /**
     * Callback to allow binding custom logic/listeners to a component after its major properties were set
     * (notice that not all properties or the full hierarchy will be available at this stage). This
     * is the perfect place to bind models/renderers etc. to components.
     *
     * @param cmp the component
     */
    protected void postCreateComponent(Component cmp) {
    }

    /**
     * Binds the given listener object to the component, this works seamlessly for 
     * common Codename One events but might be an issue with custom components and custom
     * listener types so this method can be overloaded to add support for such cases.
     *
     * @param cmp the component to bind the listener to
     * @param listener the listener object
     */
    protected void bindListenerToComponent(Component cmp, Object listener) {
        if(cmp instanceof Container) {
            cmp = ((Container)cmp).getLeadComponent();
        }
        if(listener instanceof FocusListener) {
            cmp.addFocusListener((FocusListener)listener);
            return;
        }
        if(listener instanceof ActionListener) {
            if(cmp instanceof Button) {
                ((Button)cmp).addActionListener((ActionListener)listener);
                return;
            }
            if(cmp instanceof List) {
                ((List)cmp).addActionListener((ActionListener)listener);
                return;
            }
            if(cmp instanceof ContainerList) {
                ((ContainerList)cmp).addActionListener((ActionListener)listener);
                return;
            }
            if(cmp instanceof com.codename1.ui.Calendar) {
                ((com.codename1.ui.Calendar)cmp).addActionListener((ActionListener)listener);
                return;
            }
            ((TextArea)cmp).addActionListener((ActionListener)listener);
            return;
        }
        if(listener instanceof DataChangedListener) {
            if(cmp instanceof TextField) {
                ((TextField)cmp).addDataChangeListener((DataChangedListener)listener);
                return;
            }
            ((Slider)cmp).addDataChangedListener((DataChangedListener)listener);
            return;
        }
        if(listener instanceof SelectionListener) {
            if(cmp instanceof List) {
                ((List)cmp).addSelectionListener((SelectionListener)listener);
                return;
            }
            ((Slider)cmp).addDataChangedListener((DataChangedListener)listener);
            return;
        }
    }

    void initBaseForm(String formName) {
    }

    private Container findEmptyContainer(Container c) {
        int count = c.getComponentCount();
        if(count == 0) {
            return c;
        }
        for(int iter = 0 ; iter < count ; iter++) {
            Component x = c.getComponentAt(iter);
            if(x instanceof Container) {
                Container current = findEmptyContainer((Container)x);
                if(current != null) {
                    return current;
                }
            }
        }
        return null;
    }

    /**
     * Allows a subclass to set the list model for the given component
     * 
     * @param cmp the list whose model may be set
     * @return true if a model was set by this method
     */
    protected boolean setListModel(List cmp) {
        return false;
    }

    private void readCommands(DataInputStream in, Component cmp, Resources res, boolean legacy) throws IOException {
        int commandCount = in.readInt();
        final String[] commandActions = new String[commandCount];
        final Command[] commands = new Command[commandCount];
        final String[] commandArguments = new String[commandCount];
        boolean hasAction = false;
        for(int iter = 0 ; iter < commandCount ; iter++) {
            String commandName = in.readUTF();
            String commandImageName = in.readUTF();
            String rollover = null;
            String pressed = null;
            String disabled = null;
            if(!legacy) {
                rollover = in.readUTF();
                pressed = in.readUTF();
                disabled = in.readUTF();
            }
            int commandId = in.readInt();
            commandActions[iter] = in.readUTF();
            if(commandActions[iter].length() > 0) {
                hasAction = true;
            }
            boolean isBack = in.readBoolean();
            commandArguments[iter] = "";
            if(commandActions[iter].equals("$Execute")) {
                commandArguments[iter] = in.readUTF();
            }
            commands[iter] = createCommandImpl(commandName, res.getImage(commandImageName), commandId, commandActions[iter], isBack, commandArguments[iter]);
            if(rollover != null && rollover.length() > 0) {
                commands[iter].setRolloverIcon(res.getImage(rollover));
            }
            if(pressed != null && pressed.length() > 0) {
                commands[iter].setPressedIcon(res.getImage(pressed));
            }
            if(disabled != null && disabled.length() > 0) {
                commands[iter].setPressedIcon(res.getImage(pressed));
            }
            if(isBack) {
                ((Form)cmp).setBackCommand(commands[iter]);
            }
            // trigger listener creation if this is the only command in the form
            getFormListenerInstance(((Form)cmp), null);

            ((Form)cmp).addCommand(commands[iter]);
        }
        if(hasAction) {
            for(int iter = 0 ; iter < commands.length ; iter++) {
                commands[iter].putClientProperty(COMMAND_ARGUMENTS, commandArguments[iter]);
                commands[iter].putClientProperty(COMMAND_ACTION, commandActions[iter]);
            }
            if(resourceFilePath == null || isKeepResourcesInRam()) {
                resourceFile = res;
            }
        }
    }

    private Object[] readObjectArrayForListModel(DataInputStream in, Resources res) throws IOException {
        Object[] elements = new Object[in.readInt()];
        for(int iter = 0 ; iter < elements.length ; iter++) {
            switch(in.readByte()) {
                case 1: // String
                    elements[iter] = in.readUTF();
                    break;
                case 2: // hashtable of Strings
                    int hashSize = in.readInt();
                    Hashtable val = new Hashtable();
                    elements[iter] = val;
                    for(int i = 0 ; i < hashSize ; i++) {
                        int type = in.readInt();
                        if(type == 1) { // String
                            String key = in.readUTF();
                            Object value = in.readUTF();
                            if(key.equals("$navigation")) {
                                Command cmd = createCommandImpl((String)value, null, -1, (String)value, false, "");
                                cmd.putClientProperty(COMMAND_ACTION, (String)value);
                                value = cmd;
                            }
                            val.put(key, value);
                        } else {
                            val.put(in.readUTF(), res.getImage(in.readUTF()));
                        }
                    }
                    break;
            }
        }
        return elements;
    }

    private GenericListCellRenderer readRendererer(Resources res, DataInputStream in) throws IOException {
        int rendererComponentCount = in.readByte();
        String f = in.readUTF();
        String s = in.readUTF();
        if(rendererComponentCount == 2) {
            Component selected = createContainer(res, f);
            Component unselected = createContainer(res, s);
            GenericListCellRenderer g = new GenericListCellRenderer(selected, unselected);
            g.setFisheye(!f.equals(s));
            return g;
        } else {
            Component selected = createContainer(res, f);
            Component unselected = createContainer(res, s);
            Component even = createContainer(res, in.readUTF());
            Component evenU = createContainer(res, in.readUTF());
            GenericListCellRenderer g = new GenericListCellRenderer(selected, unselected, even, evenU);
            g.setFisheye(!f.equals(s));
            return g;
        }
    }
    
    private Object readCustomPropertyValue(DataInputStream in, Class type, Resources res, String name) throws IOException {
        if(type == String.class) {
            return in.readUTF();
        }

        if(type == com.codename1.impl.CodenameOneImplementation.getStringArrayClass()) {
            String[] result = new String[in.readInt()];
            for(int i = 0 ; i < result.length ; i++) {
                result[i] = in.readUTF();
            }
            return result;
        }

        if(type == com.codename1.impl.CodenameOneImplementation.getStringArray2DClass()) {
            String[][] result = new String[in.readInt()][];
            for(int i = 0 ; i < result.length ; i++) {
                result[i] = new String[in.readInt()];
                for(int j = 0 ; j < result[i].length ; j++) {
                    result[i][j] = in.readUTF();
                }
            }
            return result;
        }

        if(type == Integer.class) {
            return new Integer(in.readInt());
        }

        if(type == Long.class) {
            return new Long(in.readLong());
        }

        if(type == Double.class) {
            return new Double(in.readDouble());
        }

        if(type == Float.class) {
            return new Float(in.readFloat());
        }

        if(type == Byte.class) {
            return new Byte(in.readByte());
        }

        if(type == Boolean.class) {
            if(in.readBoolean()) {
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        }

        if(type == com.codename1.impl.CodenameOneImplementation.getImageArrayClass()) {
            Image[] result = new Image[in.readInt()];
            for(int i = 0 ; i < result.length ; i++) {
                result[i] = res.getImage(in.readUTF());
            }
            return result;
        }

        if(type == Image.class) {
            return res.getImage(in.readUTF());
        }
        
        if(type == Container.class) {
            // resource might have been removed we need to fail gracefully
            String[] uiNames = res.getUIResourceNames();
            String currentName = in.readUTF();
            for(int iter = 0 ; iter < uiNames.length ; iter++) {
                if(uiNames[iter].equals(currentName)) {
                    return createContainer(res, currentName);
                }
            }
            return null;
        }

        if(type == CellRenderer.class) {
            return readRendererer(res, in);
        }

        if(type.isArray()) {
            return readObjectArrayForListModel(in, res);
        }
        
        if(type == Date.class) {
            boolean b = in.readBoolean();
            if(b) {
                return new Date(in.readInt());
            }
        }
        
        return new Character(in.readChar());
    }

    private Component createComponent(DataInputStream in, Container parent, Container root, Resources res, Hashtable componentListeners, EmbeddedContainer embedded) throws Exception {
        String name = in.readUTF();
        int property = in.readInt();

        // special case for the base form
        if(property == PROPERTY_BASE_FORM) {
            String baseFormName = name;
            initBaseForm(baseFormName);
            if(!ignorBaseForm) {
                Form base = (Form)createContainer(res, baseFormName);
                Container destination = (Container)findByName("destination", base);
                
                // try finding an appropriate empty container if no "fixed" destination is defined
                if(destination == null) {
                    destination = findEmptyContainer(base.getContentPane());
                    if(destination == null) {
                        System.out.println("Couldn't find appropriate 'destination' container in base form: " + baseFormName);
                        return null;
                    }
                }
                root = base;
                Component cmp = createComponent(in, destination, root, res, componentListeners, embedded);
                if(destination.getLayout() instanceof BorderLayout) {
                    destination.addComponent(BorderLayout.CENTER, cmp);
                } else {
                    destination.addComponent(cmp);
                }
                return root;
            } else {
                name = in.readUTF();
                property = in.readInt();
            }
        }
        Class c = (Class)getComponentRegistry().get(name);
        Component cmp = createComponentInstance(name, c);
        if(cmp == null) {
            if(c == null) {
                throw new RuntimeException("Component not found use UIBuilder.registerCustomComponent(" + name + ", class);");
            }
            cmp = (Component)c.newInstance();
        }

        if(componentListeners != null) {
            Object listeners = componentListeners.get(name);
            if(listeners != null) {
                if(listeners instanceof Vector) {
                    Vector v = (Vector)listeners;
                    for(int iter = 0 ; iter < v.size() ; iter++) {
                        bindListenerToComponent(cmp, v.elementAt(iter));
                    }
                } else {
                    bindListenerToComponent(cmp, listeners);
                }
            }
        }

        Component actualLead = cmp;
        if(actualLead instanceof Container) {
            Container cnt = (Container)actualLead;
            actualLead = cnt.getLeadComponent();
            if(actualLead == null) {
                actualLead = cmp;
            }
        }
        if(actualLead instanceof Button) {
            ActionListener l = getFormListenerInstance(root, embedded);
            if(l != null) {
                ((Button)actualLead).addActionListener(l);
            }
        } else {
            if(actualLead instanceof TextArea) {
                ActionListener l = getFormListenerInstance(root, embedded);
                if(l != null) {
                    ((TextArea)actualLead).addActionListener(l);
                }
            } else {
                if(actualLead instanceof List) {
                    ActionListener l = getFormListenerInstance(root, embedded);
                    if(l != null) {
                        ((List)actualLead).addActionListener(l);
                    }
                } else {
                    if(actualLead instanceof ContainerList) {
                        ActionListener l = getFormListenerInstance(root, embedded);
                        if(l != null) {
                            ((ContainerList)actualLead).addActionListener(l);
                        }
                    } else {
                        if(actualLead instanceof com.codename1.ui.Calendar) {
                            ActionListener l = getFormListenerInstance(root, embedded);
                            if(l != null) {
                                ((com.codename1.ui.Calendar)actualLead).addActionListener(l);
                            }
                        }                    
                    }
                }
            }
        }

        cmp.putClientProperty(TYPE_KEY, name);
        if(root == null) {
            root = (Container)cmp;
        }
        while(property != -1) {
            modifyingProperty(cmp, property);
            switch(property) {
                case PROPERTY_CUSTOM:
                    String customPropertyName = in.readUTF();
                    modifyingCustomProperty(cmp, customPropertyName);
                    boolean isNull = in.readBoolean();
                    if(isNull) {
                        cmp.setPropertyValue(customPropertyName, null);
                        break;
                    }
                    String[] propertyNames = cmp.getPropertyNames();
                    for(int iter = 0 ; iter < propertyNames.length ; iter++) {
                        if(propertyNames[iter].equals(customPropertyName)) {
                            Class type = cmp.getPropertyTypes()[iter];
                            Object value = readCustomPropertyValue(in, type, res, propertyNames[iter]);
                            cmp.setPropertyValue(customPropertyName, value);
                            break;
                        }
                    }
                    break;

                case PROPERTY_EMBED:
                    root.putClientProperty(EMBEDDED_FORM_FLAG, "");
                    ((EmbeddedContainer)cmp).setEmbed(in.readUTF());
                    Container embed = createContainer(res, ((EmbeddedContainer)cmp).getEmbed(), (EmbeddedContainer)cmp);
                    if(embed != null) {
                        if(embed instanceof Form) {
                            embed = formToContainer((Form)embed);
                        }
                        ((EmbeddedContainer)cmp).addComponent(BorderLayout.CENTER, embed);
                        
                        // this isn't exactly the "right thing" but its the best we can do to make all
                        // use cases work
                        beforeShowContainer(embed);
                        postShowContainer(embed);
                    }
                    break;

                case PROPERTY_TOGGLE_BUTTON:
                    ((Button)cmp).setToggle(in.readBoolean());
                    break;

                case PROPERTY_RADIO_GROUP:
                    ((RadioButton)cmp).setGroup(in.readUTF());
                    break;

                case PROPERTY_SELECTED:
                    boolean isSelected = in.readBoolean();
                    if(cmp instanceof RadioButton) {
                        ((RadioButton)cmp).setSelected(isSelected);
                    } else {
                        ((CheckBox)cmp).setSelected(isSelected);
                    }
                    break;

                case PROPERTY_SCROLLABLE_X:
                    ((Container)cmp).setScrollableX(in.readBoolean());
                    break;

                case PROPERTY_SCROLLABLE_Y:
                    ((Container)cmp).setScrollableY(in.readBoolean());
                    break;

                case PROPERTY_TENSILE_DRAG_ENABLED:
                    cmp.setTensileDragEnabled(in.readBoolean());
                    break;

                case PROPERTY_TACTILE_TOUCH:
                    cmp.setTactileTouch(in.readBoolean());
                    break;

                case PROPERTY_SNAP_TO_GRID:
                    cmp.setSnapToGrid(in.readBoolean());
                    break;

                case PROPERTY_FLATTEN:
                    cmp.setFlatten(in.readBoolean());
                    break;

                case PROPERTY_TEXT:
                    if(cmp instanceof Label) {
                        ((Label)cmp).setText(in.readUTF());
                    } else {
                        ((TextArea)cmp).setText(in.readUTF());
                    }
                    break;

                case PROPERTY_TEXT_MAX_LENGTH:
                    ((TextArea)cmp).setMaxSize(in.readInt());
                    break;

                case PROPERTY_TEXT_CONSTRAINT:
                    ((TextArea)cmp).setConstraint(in.readInt());
                    if(cmp instanceof TextField) {
                        int cons = ((TextArea)cmp).getConstraint();
                        if((cons & TextArea.NUMERIC) == TextArea.NUMERIC) {
                            ((TextField)cmp).setInputModeOrder(new String[]{"123"});
                        }
                    }
                    break;

                case PROPERTY_ALIGNMENT:
                    if(cmp instanceof Label) {
                        ((Label)cmp).setAlignment(in.readInt());
                    } else {
                        ((TextArea)cmp).setAlignment(in.readInt());
                    }
                    break;

                case PROPERTY_TEXT_AREA_GROW:
                    ((TextArea)cmp).setGrowByContent(in.readBoolean());
                    break;

                case PROPERTY_LAYOUT:
                    Layout layout = null;
                    switch(in.readShort()) {
                        case LAYOUT_BORDER_LEGACY:
                            layout = new BorderLayout();
                            break;
                        case LAYOUT_BORDER_ANOTHER_LEGACY: {
                            BorderLayout b = new BorderLayout();
                            if(in.readBoolean()) {
                                b.defineLandscapeSwap(BorderLayout.NORTH, in.readUTF());
                            }
                            if(in.readBoolean()) {
                                b.defineLandscapeSwap(BorderLayout.EAST, in.readUTF());
                            }
                            if(in.readBoolean()) {
                                b.defineLandscapeSwap(BorderLayout.WEST, in.readUTF());
                            }
                            if(in.readBoolean()) {
                                b.defineLandscapeSwap(BorderLayout.SOUTH, in.readUTF());
                            }
                            if(in.readBoolean()) {
                                b.defineLandscapeSwap(BorderLayout.CENTER, in.readUTF());
                            }
                            layout = b;
                            break;
                        }
                        case LAYOUT_BORDER: {
                            BorderLayout b = new BorderLayout();
                            if(in.readBoolean()) {
                                b.defineLandscapeSwap(BorderLayout.NORTH, in.readUTF());
                            }
                            if(in.readBoolean()) {
                                b.defineLandscapeSwap(BorderLayout.EAST, in.readUTF());
                            }
                            if(in.readBoolean()) {
                                b.defineLandscapeSwap(BorderLayout.WEST, in.readUTF());
                            }
                            if(in.readBoolean()) {
                                b.defineLandscapeSwap(BorderLayout.SOUTH, in.readUTF());
                            }
                            if(in.readBoolean()) {
                                b.defineLandscapeSwap(BorderLayout.CENTER, in.readUTF());
                            }
                            b.setAbsoluteCenter(in.readBoolean());
                            layout = b;
                            break;
                        }
                        case LAYOUT_BOX_X:
                            layout = new BoxLayout(BoxLayout.X_AXIS);
                            break;
                        case LAYOUT_BOX_Y:
                            layout = new BoxLayout(BoxLayout.Y_AXIS);
                            break;
                        case LAYOUT_FLOW_LEGACY:
                            layout = new FlowLayout();
                            break;
                        case LAYOUT_FLOW:
                            FlowLayout f = new FlowLayout();
                            f.setFillRows(in.readBoolean());
                            f.setAlign(in.readInt());
                            f.setValign(in.readInt());
                            layout = f;
                            break;
                        case LAYOUT_LAYERED:
                            layout = new LayeredLayout();
                            break;
                        case LAYOUT_GRID:
                            layout = new GridLayout(in.readInt(), in.readInt());
                            break;
                        case LAYOUT_TABLE:
                            layout = new TableLayout(in.readInt(), in.readInt());
                            break;
                    }
                    ((Container)cmp).setLayout(layout);
                    break;

                case PROPERTY_TAB_PLACEMENT:
                    ((Tabs)cmp).setTabPlacement(in.readInt());
                    break;

                case PROPERTY_TAB_TEXT_POSITION:
                    ((Tabs)cmp).setTabTextPosition(in.readInt());
                    break;

                case PROPERTY_PREFERRED_WIDTH:
                    cmp.setPreferredW(in.readInt());
                    break;

                case PROPERTY_PREFERRED_HEIGHT:
                    cmp.setPreferredH(in.readInt());
                    break;

                case PROPERTY_UIID:
                    cmp.setUIID(in.readUTF());
                    break;

                case PROPERTY_DIALOG_UIID:
                    ((Dialog)cmp).setDialogUIID(in.readUTF());
                    break;

                case PROPERTY_DISPOSE_WHEN_POINTER_OUT:
                    ((Dialog)cmp).setDisposeWhenPointerOutOfBounds(in.readBoolean());
                    break;
                    
                case PROPERTY_CLOUD_BOUND_PROPERTY:
                    cmp.setCloudBoundProperty(in.readUTF());
                    break;
                    
                case PROPERTY_CLOUD_DESTINATION_PROPERTY:
                    cmp.setCloudDestinationProperty(in.readUTF());
                    break;

                case PROPERTY_DIALOG_POSITION:
                    String pos = in.readUTF();
                    if(pos.length() > 0) {
                        ((Dialog)cmp).setDialogPosition(pos);
                    }
                    break;

                case PROPERTY_FOCUSABLE:
                    cmp.setFocusable(in.readBoolean());
                    break;

                case PROPERTY_ENABLED:
                    cmp.setEnabled(in.readBoolean());
                    break;

                case PROPERTY_SCROLL_VISIBLE:
                    cmp.setScrollVisible(in.readBoolean());
                    break;

                case PROPERTY_ICON:
                    ((Label)cmp).setIcon(res.getImage(in.readUTF()));
                    break;

                case PROPERTY_ROLLOVER_ICON:
                    ((Button)cmp).setRolloverIcon(res.getImage(in.readUTF()));
                    break;

                case PROPERTY_PRESSED_ICON:
                    ((Button)cmp).setPressedIcon(res.getImage(in.readUTF()));
                    break;

                case PROPERTY_DISABLED_ICON:
                    ((Button)cmp).setDisabledIcon(res.getImage(in.readUTF()));
                    break;

                case PROPERTY_GAP:
                    ((Label)cmp).setGap(in.readInt());
                    break;

                case PROPERTY_VERTICAL_ALIGNMENT:
                    if(cmp instanceof TextArea) {
                        ((TextArea)cmp).setVerticalAlignment(in.readInt());
                    } else {
                        ((Label)cmp).setVerticalAlignment(in.readInt());
                    }
                    break;

                case PROPERTY_TEXT_POSITION:
                    ((Label)cmp).setTextPosition(in.readInt());
                    break;

                case PROPERTY_NAME:
                    String componentName = in.readUTF();
                    cmp.setName(componentName);
                    root.putClientProperty("%" + componentName + "%", cmp);
                    break;

                case PROPERTY_LAYOUT_CONSTRAINT:
                    if(parent.getLayout() instanceof BorderLayout) {
                        cmp.putClientProperty("layoutConstraint", in.readUTF());
                    } else {
                        TableLayout tl = (TableLayout)parent.getLayout();
                        TableLayout.Constraint con = tl.createConstraint(in.readInt(), in.readInt());
                        con.setHeightPercentage(in.readInt());
                        con.setWidthPercentage(in.readInt());
                        con.setHorizontalAlign(in.readInt());
                        con.setHorizontalSpan(in.readInt());
                        con.setVerticalAlign(in.readInt());
                        con.setVerticalSpan(in.readInt());
                        cmp.putClientProperty("layoutConstraint", con);
                    }
                    break;

                case PROPERTY_TITLE:
                    ((Form)cmp).setTitle(in.readUTF());
                    break;

                case PROPERTY_COMPONENTS:
                    int componentCount = in.readInt();
                    if(cmp instanceof Tabs) {
                        for(int iter = 0 ; iter < componentCount ; iter++) {
                            String tab = in.readUTF();
                            Component child = createComponent(in, (Container)cmp, root, res, componentListeners, embedded);
                            ((Tabs)cmp).addTab(tab, child);
                        }
                    } else {
                        for(int iter = 0 ; iter < componentCount ; iter++) {
                            Component child = createComponent(in, (Container)cmp, root, res, componentListeners, embedded);
                            Object con = child.getClientProperty("layoutConstraint");
                            if(con != null) {
                                ((Container)cmp).addComponent(con, child);
                            } else {
                                ((Container)cmp).addComponent(child);
                            }
                        }
                    }
                    break;

                case PROPERTY_COLUMNS:
                    ((TextArea)cmp).setColumns(in.readInt());
                    break;

                case PROPERTY_ROWS:
                    ((TextArea)cmp).setRows(in.readInt());
                    break;

                case PROPERTY_HINT:
                    if(cmp instanceof List) {
                        ((List)cmp).setHint(in.readUTF());
                    } else {
                        ((TextArea)cmp).setHint(in.readUTF());
                    }
                    break;

                case PROPERTY_HINT_ICON:
                    if(cmp instanceof List) {
                        ((List)cmp).setHintIcon(res.getImage(in.readUTF()));
                    } else {
                        ((TextArea)cmp).setHintIcon(res.getImage(in.readUTF()));
                    }
                    break;

                case PROPERTY_ITEM_GAP:
                    ((List)cmp).setItemGap(in.readInt());
                    break;

                case PROPERTY_LIST_FIXED:
                    ((List)cmp).setFixedSelection(in.readInt());
                    break;

                case PROPERTY_LIST_ORIENTATION:
                    ((List)cmp).setOrientation(in.readInt());
                    break;

                case PROPERTY_LIST_ITEMS_LEGACY:
                    String[] items = new String[in.readInt()];
                    for(int iter = 0 ; iter < items.length ; iter++) {
                        items[iter] = in.readUTF();
                    }
                    if(!setListModel(((List)cmp))) {
                        ((List)cmp).setModel(new DefaultListModel(items));
                    }
                    break;

                case PROPERTY_LIST_ITEMS:
                    Object[] elements = readObjectArrayForListModel(in, res);
                    if(!setListModel(((List)cmp))) {
                        ((List)cmp).setModel(new DefaultListModel(elements));
                    }
                    break;

                case PROPERTY_LIST_RENDERER:
                    if(cmp instanceof ContainerList) {
                        ((ContainerList)cmp).setRenderer(readRendererer(res, in));
                    } else {
                        ((List)cmp).setRenderer(readRendererer(res, in));
                    }
                    break;

                case PROPERTY_NEXT_FORM:
                    String nextForm = in.readUTF();
                    cmp.putClientProperty("%next_form%", nextForm);
                    if(resourceFilePath == null || isKeepResourcesInRam()) {
                        resourceFile = res;
                    }
                    ((Form)root).addShowListener(new FormListener((Form)root, nextForm));
                    break;

                case PROPERTY_COMMANDS:
                    readCommands(in, cmp, res, false);
                    break;
                case PROPERTY_COMMANDS_LEGACY:
                    readCommands(in, cmp, res, true);
                    break;
                    
                case PROPERTY_CYCLIC_FOCUS:
                    ((Form)cmp).setCyclicFocus(in.readBoolean());
                    break;
                    
                case PROPERTY_RTL:
                    cmp.setRTL(in.readBoolean());
                    break;

                case PROPERTY_SLIDER_THUMB:
                    ((Slider)cmp).setThumbImage(res.getImage(in.readUTF()));
                    break;

                case PROPERTY_INFINITE:
                    ((Slider)cmp).setInfinite(in.readBoolean());
                    break;

                case PROPERTY_PROGRESS:
                    ((Slider)cmp).setProgress(in.readInt());
                    break;

                case PROPERTY_VERTICAL:
                    ((Slider)cmp).setVertical(in.readBoolean());
                    break;

                case PROPERTY_EDITABLE:
                    if(cmp instanceof TextArea) {
                        ((TextArea)cmp).setEditable(in.readBoolean());
                    } else {
                        ((Slider)cmp).setEditable(in.readBoolean());
                    }
                    break;

                case PROPERTY_INCREMENTS:
                    ((Slider)cmp).setIncrements(in.readInt());
                    break;

                case PROPERTY_RENDER_PERCENTAGE_ON_TOP:
                    ((Slider)cmp).setRenderPercentageOnTop(in.readBoolean());
                    break;

                case PROPERTY_MAX_VALUE:
                    ((Slider)cmp).setMaxValue(in.readInt());
                    break;

                case PROPERTY_MIN_VALUE:
                    ((Slider)cmp).setMinValue(in.readInt());
                    break;
            }

            property = in.readInt();
        }
        postCreateComponent(cmp);
        return cmp;
    }

    // for internal use in the resource editor
    void modifyingProperty(Component c, int p) {
    }

    // for internal use in the resource editor
    void modifyingCustomProperty(Component c, String name) {
    }
    
    /**
     * Creates a command instance. This method is invoked by the loading code and
     * can be overriden to create a subclass of the Command class.
     *
     * @param commandName the label on the command
     * @param icon the icon for the command
     * @param commandId the id of the command
     * @param action the action assigned to the command if such an action is defined
     * @return a new command instance
     */
    protected Command createCommand(String commandName, Image icon, int commandId, String action) {
        return new Command(commandName, icon, commandId);
    }

    Command createCommandImpl(String commandName, Image icon, int commandId, String action, boolean isBack, String argument) {
        return createCommand(commandName, icon, commandId, action);
    }

    /**
     * This method may be overriden by subclasses to provide a way to dynamically load
     * a resource file. Normally the navigation feature of the UIBuilder requires the resource
     * file present in RAM. However, that might be expensive to maintain.
     * By implementing this method and replacing the storeResourceFile() with an empty
     * implementation the resource file storage can be done strictly in RAM.
     *
     * @return the instance of the resource file
     */
    protected Resources fetchResourceFile() {
        try {
            if (resourceFile != null) {
                return resourceFile;
            }
            String p = getResourceFilePath();
            if(p.indexOf('.') > -1) {
                return Resources.open(p);
            }
            return Resources.openLayered(p);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * Allows the navigation code to avoid storing the resource file and lets the GC
     * remove it from memory when its not in use
     *
     * @return the resourceFilePath
     */
    public String getResourceFilePath() {
        return resourceFilePath;
    }

    /**
     * Allows the navigation code to avoid storing the resource file and lets the GC
     * remove it from memory when its not in use
     * 
     * @param resourceFilePath the resourceFilePath to set
     */
    public void setResourceFilePath(String resourceFilePath) {
        this.resourceFilePath = resourceFilePath;
        if(resourceFilePath != null) {
            resourceFile = null;
        }
    }

    /**
     * Sets the resource file if keep in rum or no path is defined
     * 
     * @param res the resource file
     */
    protected void setResourceFile(Resources res) {
        if(keepResourcesInRam || resourceFilePath == null) {
            this.resourceFile = res;
        }
    }

    /**
     * Invoked to process a given command before naviation or any other internal
     * processing occurs. The event can be consumed to prevent further processing.
     * 
     * @param ev the action event source of the command
     * @param cmd the command to process
     */
    protected void processCommand(ActionEvent ev, Command cmd) {
    }

    private void processCommandImpl(ActionEvent ev, Command cmd) {
        processCommand(ev, cmd);
        if(ev.isConsumed()) {
            return;
        }
        if(globalCommandListeners != null) {
            globalCommandListeners.fireActionEvent(ev);
            if(ev.isConsumed()) {
                return;
            }
        }

        if(localCommandListeners != null) {
            Form f = Display.getInstance().getCurrent();
            EventDispatcher e = (EventDispatcher)localCommandListeners.get(f.getName());
            if(e != null) {
                e.fireActionEvent(ev);
            }
        }
    }

    /**
     * Adds a command listener that would be bound to all forms in the GUI seamlessly
     *
     * @param l the listener to bind
     */
    public void addCommandListener(ActionListener l) {
        if(globalCommandListeners == null) {
            globalCommandListeners = new EventDispatcher();
        }
        globalCommandListeners.addListener(l);
    }

    /**
     * Removes a command listener
     *
     * @param l the listener to remove
     */
    public void removeCommandListener(ActionListener l) {
        if(globalCommandListeners == null) {
            return;
        }
        globalCommandListeners.removeListener(l);
    }

    /**
     * Adds a component listener that would be bound when a UI for this form is created.
     * Notice that this method is only effective before the form was created and would do
     * nothing for an existing form
     *
     * @param formName the name of the form to which the listener should be bound
     * @param componentName the name of the component to bind to
     * @param listener the listener to bind, common listener types are supported
     */
    public void addComponentListener(String formName, String componentName, Object listener) {
        if(localComponentListeners == null) {
            localComponentListeners = new Hashtable();
            Hashtable formListeners = new Hashtable();
            formListeners.put(componentName, listener);
            localComponentListeners.put(formName, formListeners);
            return;
        }
        Hashtable formListeners = (Hashtable)localComponentListeners.get(formName);
        if(formListeners == null) {
            formListeners = new Hashtable();
            formListeners.put(componentName, listener);
            localComponentListeners.put(formName, formListeners);
            return;
        }
        Object currentListeners = formListeners.get(componentName);
        if(currentListeners == null) {
            formListeners.put(componentName, listener);
        } else {
            if(currentListeners instanceof Vector) {
                ((Vector)currentListeners).addElement(listener);
            } else {
                Vector v = new Vector();
                v.addElement(currentListeners);
                v.addElement(listener);
                formListeners.put(componentName, v);
            }
        }
    }

    /**
     * Removes a component listener bound to a specific component
     *
     * @param formName the name of the form 
     * @param componentName the name of the component 
     * @param listener the listener instance
     */
    public void removeComponentListener(String formName, String componentName, Object listener) {
        if(localComponentListeners == null) {
            return;
        }
        Hashtable formListeners = (Hashtable)localComponentListeners.get(formName);
        if(formListeners == null) {
            return;
        }
        Object currentListeners = formListeners.get(componentName);
        if(currentListeners == null) {
            return;
        } else {
            if(currentListeners instanceof Vector) {
                ((Vector)currentListeners).removeElement(listener);
                if(((Vector)currentListeners).size() == 0) {
                    formListeners.remove(componentName);
                }
            } else {
                formListeners.remove(componentName);
            }
        }
    }

    /**
     * Adds a command listener to be invoked for commands on a specific form
     * 
     * @param formName the name of the form to which the listener should be bound
     * @param l the listener to bind
     */
    public void addCommandListener(String formName, ActionListener l) {
        if(localCommandListeners == null) {
            localCommandListeners = new Hashtable();
        }
        EventDispatcher d = (EventDispatcher)localCommandListeners.get(formName);
        if(d == null) {
            d = new EventDispatcher();
            localCommandListeners.put(formName, d);
        }
        d.addListener(l);
    }

    /**
     * Removes a command listener on a specific form
     *
     * @param formName the name of the form
     * @param l the listener to remove
     */
    public void removeCommandListener(String formName, ActionListener l) {
        if(localCommandListeners == null) {
            return;
        }
        EventDispatcher d = (EventDispatcher)localCommandListeners.get(formName);
        if(d == null) {
            return;
        }
        d.removeListener(l);
    }

    /**
     * This method is invoked for every component to which an action event listener can be bound
     * and delivers the event data for the given component seamlessly. 
     * 
     * @param c the component broadcasting the event
     * @param event the event meta data 
     */
    protected void handleComponentAction(Component c, ActionEvent event) {
    }

    private FormListener getFormListenerInstance(Component cmp, EmbeddedContainer embedded) {
        if(embedded != null) {
            FormListener fc = (FormListener)embedded.getClientProperty("!FormListener!");
            if(fc != null) {
                return fc;
            }
            fc = new FormListener();
            embedded.putClientProperty("!FormListener!", fc);
            return fc;
        }
        Form f = cmp.getComponentForm();
        if(f == null) {
            return null;
        }
        FormListener fc = (FormListener)f.getClientProperty("!FormListener!");
        if(fc != null) {
            return fc;
        }
        fc = new FormListener();
        f.putClientProperty("!FormListener!", fc);
        f.addCommandListener(fc);
        return fc;
    }

    /**
     * Warning: This method is invoked OFF the EDT and is intended for usage with asynchronous
     * command processing. This method is invoked when the UI indicates that an operation
     * should occur in the background. To finish the processing of the operation within the
     * EDT one should overide the postAsyncCommand() method.
     *
     * @param cmd the command requiring background processing
     * @param sourceEvent the triggering event
     */
    protected void asyncCommandProcess(Command cmd, ActionEvent sourceEvent) {
    }

    /**
     * This method is invoked in conjunction with asyncCommandProcess after the 
     * command was handled asynchroniously on the separate thread. Here Codename One
     * code can be execute to update the UI with the results from the separate thread.
     * 
     * @param cmd the command
     * @param sourceEvent the source event
     */
    protected void postAsyncCommand(Command cmd, ActionEvent sourceEvent) {
    }

    /**
     * <b>Warning:</b> this method is invoked on a separate thread. 
     * This method is invoked when a next form property is defined, this property
     * indicates a background process for a form of a transitional nature should
     * take place (e.g. splash screen, IO etc.) after which the next form should be shown.
     * After this method completes the next form is shown.
     * 
     * @param f the form for which the background thread was constructed, notice
     * that most methods are not threadsafe and one should use callSerially* in this
     * method when mutating the form.
     * @return if false is returned from this method navigation should not proceed
     * to that given form
     */
    protected boolean processBackground(Form f) {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        return true;
    }

    /**
     * Returns the state of the current form which we are about to leave as part
     * of the navigation logic. When a back command will return to this form the
     * state would be restored using setFormState.
     * The default implementation of this method restores focus and list selection.
     * You can add arbitrary keys to the form state, keys starting with a $ sign
     * are reserved for the UIBuilder base class use.
     *
     * @param f the form whose state should be preserved
     * @return arbitrary state object
     */
    protected Hashtable getFormState(Form f) {
        Component c = f.getFocused();
        Hashtable h = new Hashtable();
        h.put(FORM_STATE_KEY_NAME, f.getName());
        if(c != null) {
            if(c instanceof List) {
                h.put(FORM_STATE_KEY_SELECTION, new Integer(((List)c).getSelectedIndex()));
            }
            if(c.getName() != null) {
                h.put(FORM_STATE_KEY_FOCUS, c.getName());
            }
            if(f.getTitle() != null) {
                h.put(FORM_STATE_KEY_TITLE, f.getTitle());
            }
        }
        storeComponentState(f, h);
        return h;
    }

    private void restoreComponentState(Component c, Hashtable destination) {
        if(shouldAutoStoreState()) {
            Enumeration e = destination.keys();
            while(e.hasMoreElements()) {
                String currentKey = (String)e.nextElement();
                Component cmp = findByName(currentKey, c);
                if(cmp != null) {
                    Object value = destination.get(currentKey);
                    if(value instanceof Integer) {
                        if(cmp instanceof List) {
                            ((List)cmp).setSelectedIndex(((Integer)value).intValue());
                            continue;
                        }
                        if(cmp instanceof Tabs) {
                            ((Tabs)cmp).setSelectedIndex(((Integer)value).intValue());
                        }
                    }
                }
            }
        }
    }
    
    private void storeComponentState(Component c, Hashtable destination) {
        if(shouldAutoStoreState()) {
            storeComponentStateImpl(c, destination);
        }
    }
        
    private void storeComponentStateImpl(Component c, Hashtable destination) {
        if(c instanceof Tabs) {
            destination.put(c.getName(), new Integer(((Tabs)c).getSelectedIndex()));
        }
        if(c instanceof Container) {
            Container cnt = (Container)c;
            int count = cnt.getComponentCount();
            for(int iter = 0 ; iter < count ; iter++) {
                storeComponentStateImpl(cnt.getComponentAt(iter), destination);
            }
            return;
        }
        if(c.getName() == null || destination.containsKey(c.getName()) || c.getClientProperty("CN1IgnoreStore") != null) {
            return;
        }
        if(c instanceof List) {
            destination.put(c.getName(), new Integer(((List)c).getSelectedIndex()));
            return;
        }
    }
    
    /**
     * Indicates whether the UIBuilder should try storing states for forms on its own
     * by seeking lists, tabs and other statefull elements and keeping their selection
     * @return true to handle state automatically, false otherwise
     */
    protected boolean shouldAutoStoreState() {
        return true;
    }
    
    /**
     * Sets the state of the current form to which we are returing as part
     * of the navigation logic. When a back command is pressed this form
     * state should be restored, it was obtained via getFormState.
     * The default implementation of this method restores focus and list selection.
     *
     * @param f the form whose state should be preserved
     * @param state arbitrary state object
     */
    protected void setFormState(Form f, Hashtable state) {
        setContainerStateImpl(f, state);
    }

    private boolean isParentOf(Container cnt, Component c) {
        while(c != null) {
            if(c == cnt) {
                return true;
            }
            c = c.getParent();
        }
        return false;
    }

    /**
     * This method is the container navigation equivalent of getFormState() see
     * that method for details.
     * @param cnt the container
     * @return the state
     */
    protected Hashtable getContainerState(Container cnt) {
        Component c = null;
        Form parentForm = cnt.getComponentForm();
        if(parentForm != null) {
            c = parentForm.getFocused();
        }
        Hashtable h = new Hashtable();
        h.put(FORM_STATE_KEY_NAME, cnt.getName());
        h.put(FORM_STATE_KEY_CONTAINER, "");
        if(c != null && isParentOf(cnt, c)) {
            if(c instanceof List) {
                h.put(FORM_STATE_KEY_SELECTION, new Integer(((List)c).getSelectedIndex()));
            }
            if(c.getName() != null) {
                h.put(FORM_STATE_KEY_FOCUS, c.getName());
            }
            return h;
        }
        storeComponentState(cnt, h);
        return h;
    }

    private void setContainerStateImpl(Container cnt, Hashtable state) {
        if(state != null) {
            restoreComponentState(cnt, state);
            String cmpName = (String)state.get(FORM_STATE_KEY_FOCUS);
            if(cmpName == null) {
                return;
            }
            Component c = findByName(cmpName, cnt);
            if(c != null) {
                c.requestFocus();
                if(c instanceof List) {
                    Integer i = (Integer)state.get(FORM_STATE_KEY_SELECTION);
                    if(i != null) {
                        ((List)c).setSelectedIndex(i.intValue());
                    }
                }
            }
        }
    }

    /**
     * This method is the container navigation equivalent of setFormState() see
     * that method for details.
     * 
     * @param cnt the container
     * @param state the state
     */
    protected void setContainerState(Container cnt, Hashtable state) {
        setContainerStateImpl(cnt, state);
    }

    /**
     * When reaching the home form the navigation stack is cleared
     * @return the homeForm
     */
    public String getHomeForm() {
        return homeForm;
    }

    /**
     * When reaching the home form the navigation stack is cleared
     * @param homeForm the homeForm to set
     */
    public void setHomeForm(String homeForm) {
        this.homeForm = homeForm;
    }

    /**
     * This method effectively pops the form navigation stack and goes back
     * to the previous form if back navigation is enabled and there is
     * a previous form.
     */
    public void back() {
        back(null);
    }

    /**
     * This method effectively pops the form navigation stack and goes back
     * to the previous form if back navigation is enabled and there is
     * a previous form.
     *
     * @param sourceComponent the component that triggered the back command which effectively
     * allows us to find the EmbeddedContainer for a case of container navigation. Null
     * can be used if not applicable.
     */
    public void back(Component sourceComponent) {
        Vector formNavigationStack = getFormNavigationStackForComponent(sourceComponent);
        if(formNavigationStack != null && formNavigationStack.size() > 0) {
            Hashtable h = (Hashtable)formNavigationStack.elementAt(formNavigationStack.size() - 1);
            if(h.containsKey(FORM_STATE_KEY_CONTAINER)) {
                Form currentForm = Display.getInstance().getCurrent();
                if(currentForm != null) {
                    exitForm(currentForm);
                }
            }
            String formName = (String)h.get(FORM_STATE_KEY_NAME);
            if(!h.containsKey(FORM_STATE_KEY_CONTAINER)) {
                Form f = (Form)createContainer(fetchResourceFile(), formName);
                initBackForm(f);
                beforeShow(f);
                f.showBack();
                postShowImpl(f);
            } else {
                showContainerImpl(formName, null, sourceComponent, true);
            }
        }
    }

    private String previousFormName(Form f) {
        Vector formNavigationStack = getFormNavigationStackForComponent(f);
        if(formNavigationStack != null && formNavigationStack.size() > 0) {
            Hashtable h = (Hashtable)formNavigationStack.elementAt(formNavigationStack.size() - 1);
            return (String)h.get(FORM_STATE_KEY_NAME);
        }
        return null;
    }
    
    /**
     * Returns the text for the back command string. This can be controlled in the theme by the "backUsesTitleBool" constant
     * 
     * @param previousFormTitle the title of the previous form
     * @return the string for the back command inserted implicitly
     */
    protected String getBackCommandText(String previousFormTitle) {
        if(UIManager.getInstance().isThemeConstant("backUsesTitleBool", false)) {
            return previousFormTitle;
        }
        return "Back";
    }
    
    private void initBackForm(Form f) {
        Vector formNavigationStack = baseFormNavigationStack;
        if(formNavigationStack != null && formNavigationStack.size() > 0) {
            setFormState(f, (Hashtable)formNavigationStack.elementAt(formNavigationStack.size() - 1));
            formNavigationStack.removeElementAt(formNavigationStack.size() - 1);
            if(formNavigationStack.size() > 0) {
                Hashtable previous = (Hashtable)formNavigationStack.elementAt(formNavigationStack.size() - 1);
                String commandAction = (String)previous.get(FORM_STATE_KEY_NAME);
                Command backCommand = createCommandImpl(getBackCommandText((String)previous.get(FORM_STATE_KEY_TITLE)), null,
                        BACK_COMMAND_ID, commandAction, true, "");
                f.addCommand(backCommand, f.getCommandCount());
                f.setBackCommand(backCommand);
                
                // trigger listener creation if this is the only command in the form
                getFormListenerInstance(f, null);

                backCommand.putClientProperty(COMMAND_ARGUMENTS, "");
                backCommand.putClientProperty(COMMAND_ACTION, commandAction);
            }
        }
    }

    private void initBackContainer(Container cnt, Form destForm, Vector formNavigationStack) {
        setContainerState(cnt, (Hashtable)formNavigationStack.elementAt(formNavigationStack.size() - 1));
        formNavigationStack.removeElementAt(formNavigationStack.size() - 1);
        if(formNavigationStack.size() > 0) {
            Hashtable previous = (Hashtable)formNavigationStack.elementAt(formNavigationStack.size() - 1);
            String commandAction = (String)previous.get(FORM_STATE_KEY_NAME);
            Command backCommand = createCommandImpl(getBackCommandText((String)previous.get(FORM_STATE_KEY_TITLE)), null,
                    BACK_COMMAND_ID, commandAction, true, "");
            destForm.setBackCommand(backCommand);
            backCommand.putClientProperty(COMMAND_ARGUMENTS, "");
            backCommand.putClientProperty(COMMAND_ACTION, commandAction);
        }
    }

    /**
     * This method is equivalent to the internal navigation behavior, it adds
     * functionality such as the back command into the given form resource and
     * shows it. If the source command is the back command the showBack() method
     * will run.
     * Notice that container navigation (none-form) doesn't support the back() method
     * or the form stack. However a command marked as back command will be respected.
     *
     * @param resourceName the name of the resource for the form to show
     * @param sourceCommand the command of the resource (may be null)
     * @param sourceComponent the component that activated the show (may be null)
     * @return the container thats being shown, notice that you can still manipulate
     * some states of the container before it actually appears
     */
    public Container showContainer(String resourceName, Command sourceCommand, Component sourceComponent) {
        return showContainerImpl(resourceName, sourceCommand, sourceComponent, false);
    }


    /**
     * Useful tool to refresh the current state of a container shown using show container
     * without pushing another instance to the back stack
     * 
     * @param cnt the container thats embedded into the application
     */
    public void reloadContainer(Component cnt) {
        Container newCnt = createContainer(fetchResourceFile(), cnt.getName(), (EmbeddedContainer)cnt.getParent());
        beforeShowContainer(newCnt);
        cnt.getParent().replace(cnt, newCnt, null);
        postShowContainer(newCnt);
    }


    /**
     * Useful tool to refresh the current state of a form shown using show form
     * without pushing another instance to the back stack
     */
    public void reloadForm() {
        Form currentForm = Display.getInstance().getCurrent();
        Command backCommand = currentForm.getBackCommand(); 
        Form newForm = (Form)createContainer(fetchResourceFile(), currentForm.getName());

        if (backCommand != null) {
            newForm.setBackCommand(backCommand);
            for(int iter = 0 ; iter < currentForm.getCommandCount() ; iter++) {
                if(backCommand == currentForm.getCommand(iter)) {
                    newForm.addCommand(backCommand, newForm.getCommandCount());
                    break;
                }
            }
        }
        
        beforeShow(newForm);
        Transition tin = newForm.getTransitionInAnimator();
        Transition tout = newForm.getTransitionOutAnimator();
        currentForm.setTransitionInAnimator(CommonTransitions.createEmpty());
        currentForm.setTransitionOutAnimator(CommonTransitions.createEmpty());
        newForm.setTransitionInAnimator(CommonTransitions.createEmpty());
        newForm.setTransitionOutAnimator(CommonTransitions.createEmpty());
        newForm.layoutContainer();
        newForm.show();
        postShowImpl(newForm);
        newForm.setTransitionInAnimator(tin);
        newForm.setTransitionOutAnimator(tout);
    }

    private Container showContainerImpl(String resourceName, Command sourceCommand, Component sourceComponent, boolean forceBack) {
        if(sourceComponent != null) {
            Form currentForm = sourceComponent.getComponentForm();

            // avoid the overhead of searching if no embedding is used
            if(currentForm.getClientProperty(EMBEDDED_FORM_FLAG) != null) {
                Container destContainer = sourceComponent.getParent();
                while(!(destContainer instanceof EmbeddedContainer || destContainer instanceof Form)) {
                    // race condition, container was already removed by someone else
                    if(destContainer == null) {
                        return null;
                    }
                    destContainer = destContainer.getParent();
                }
                if(destContainer instanceof EmbeddedContainer) {
                    Container cnt = createContainer(fetchResourceFile(), resourceName, (EmbeddedContainer)destContainer);
                    if(cnt instanceof Form) {
                        //Form f = (Form)cnt;
                        //cnt = formToContainer(f);
                        showForm((Form)cnt, sourceCommand, sourceComponent);
                        return cnt;
                    }
                    
                    Component fromCmp = destContainer.getComponentAt(0);

                    // This seems to be no longer necessary now that we have the replaceAndWait version that drops events
                    // block the user from the ability to press the button twice by mistake
                    //fromCmp.setEnabled(false);

                    boolean isBack = forceBack;
                    Transition t = fromCmp.getUIManager().getLookAndFeel().getDefaultFormTransitionOut();
                    if(forceBack) {
                        initBackContainer(cnt, destContainer.getComponentForm(), getFormNavigationStackForComponent(sourceComponent));
                        t = t.copy(true);
                    } else {
                        if(sourceCommand != null) {
                            if(t != null && backCommands != null && backCommands.contains(sourceCommand) || Display.getInstance().getCurrent().getBackCommand() == sourceCommand) {
                                isBack = true;
                                t = t.copy(true);
                            }
                        }
                    }

                    // create a back command if supported
                    String commandAction = cnt.getName();
                    Vector formNavigationStack = getFormNavigationStackForComponent(fromCmp);
                    if(formNavigationStack != null && !isBack && allowBackTo(commandAction) && !isSameBackDestination((Container)fromCmp, cnt)) {
                        // trigger listener creation if this is the only command in the form
                        getFormListenerInstance(destContainer.getComponentForm(), null);
                        formNavigationStack.addElement(getContainerState((com.codename1.ui.Container)fromCmp));
                    }

                    beforeShowContainer(cnt);
                    destContainer.replaceAndWait(fromCmp, cnt, t, true);
                    postShowContainer(cnt);
                    return cnt;
                } else {
                    Container cnt = createContainer(fetchResourceFile(), resourceName);
                    showForm((Form)cnt, sourceCommand, sourceComponent);
                    return cnt;
                }
            }
        }
        Container cnt = createContainer(fetchResourceFile(), resourceName);
        if(cnt instanceof Form) {
            showForm((Form)cnt, sourceCommand, sourceComponent);
        } else {
            Form f = new Form();
            f.setLayout(new BorderLayout());
            f.addComponent(BorderLayout.CENTER, cnt);
            f.setName("Form" + cnt.getName());
            showForm(f, sourceCommand, sourceComponent);
        }
        return cnt;
    }

    private Container formToContainer(Form f) {
        Container cnt = new Container(f.getContentPane().getLayout());
        if(f.getContentPane().getLayout() instanceof BorderLayout ||
                f.getContentPane().getLayout() instanceof TableLayout) {
            while(f.getContentPane().getComponentCount() > 0) {
                Component src = f.getContentPane().getComponentAt(0);
                Object o = f.getContentPane().getLayout().getComponentConstraint(src);
                f.getContentPane().removeComponent(src);
                cnt.addComponent(o, src);
            }
        } else {
            while(f.getContentPane().getComponentCount() > 0) {
                Component src = f.getContentPane().getComponentAt(0);
                f.getContentPane().removeComponent(src);
                cnt.addComponent(src);
            }
        }
        return cnt;
    }

    private void showForm(Form f, Command sourceCommand, Component sourceComponent) {
        if(Display.getInstance().getCurrent() instanceof Dialog) {
            ((Dialog)Display.getInstance().getCurrent()).dispose();
        }
        Vector formNavigationStack = baseFormNavigationStack;
        if(sourceCommand != null && Display.getInstance().getCurrent().getBackCommand() == sourceCommand) {
            Form currentForm = Display.getInstance().getCurrent();
            if(currentForm != null) {
                exitForm(currentForm);
            }
            if(formNavigationStack != null && formNavigationStack.size() > 0) {
                String name = f.getName();
                if(name != null && name.equals(homeForm)) {
                    if(formNavigationStack.size() > 0) {
                        setFormState(f, (Hashtable)formNavigationStack.elementAt(formNavigationStack.size() - 1));
                    }
                    formNavigationStack.clear();
                } else {
                    initBackForm(f);
                }
            }
            beforeShow(f);
            f.showBack();
            postShowImpl(f);
        } else {
            Form currentForm = Display.getInstance().getCurrent();
            if(currentForm != null) {
                exitForm(currentForm);
            }
            if(formNavigationStack != null && !(f instanceof Dialog) && !f.getName().equals(homeForm)) {
                if(currentForm != null) {
                    String nextForm = (String)f.getClientProperty("%next_form%");

                    // don't add back commands to transitional forms
                    if(nextForm == null) {
                        String commandAction = currentForm.getName();
                        if(allowBackTo(commandAction) && f.getBackCommand() == null) {
                            Command backCommand;
                            if(isSameBackDestination(currentForm, f)) {
                                backCommand = currentForm.getBackCommand();
                            } else {
                                backCommand = createCommandImpl(getBackCommandText(currentForm.getTitle()), null,
                                    BACK_COMMAND_ID, commandAction, true, "");
                                backCommand.putClientProperty(COMMAND_ARGUMENTS, "");
                                backCommand.putClientProperty(COMMAND_ACTION, commandAction);
                            }
                            f.addCommand(backCommand, f.getCommandCount());
                            f.setBackCommand(backCommand);

                            // trigger listener creation if this is the only command in the form
                            getFormListenerInstance(f, null);
                            formNavigationStack.addElement(getFormState(Display.getInstance().getCurrent()));
                        }
                    }
                }
            }
            if(f instanceof Dialog) {
                beforeShow(f);
                if(sourceComponent != null) {
                    // we are cheating with the post show here since we are using a modal
                    // dialog to prevent the "double clicking button" problem by using
                    // a modal dialog
                    sourceComponent.setEnabled(false);
                    postShowImpl(f);
                    f.show();
                    sourceComponent.setEnabled(true);
                    exitForm(f);
                } else {
                    ((Dialog)f).showModeless();
                    postShowImpl(f);
                }
            } else {
                beforeShow(f);
                f.show();
                postShowImpl(f);
            }
        }
    }

    /**
     * Indicates whether a back command to this form should be generated automatically when
     * leaving said form.
     *
     * @param formName the name of the form
     * @return true to autogenerate and add a back command to the destination form
     */
    protected boolean allowBackTo(String formName) {
        return true;
    }

    /**
     * When navigating from one form/container to another we sometimes might not want the
     * back command to return to the previous container/form but rather to the one before
     * source. A good example would be a "refresh" command or a toggle button that changes
     * the form state.
     * 
     * @param source the form or container we are leaving
     * @param destination the container or form we are navigating to
     * @return false if we want a standard back button to source, true if we want to use
     * the same back button as the one in source
     */
    protected boolean isSameBackDestination(Container source, Container destination) {
        return source.getName() == null || destination.getName() == null || 
                source.getName().equals(destination.getName());
    }

    /**
     * This method is equivalent to the internal navigation behavior, it adds 
     * functionality such as the back command into the given form resource and
     * shows it. If the source command is the back command the showBack() method
     * will run.
     * 
     * @param resourceName the name of the resource for the form to show
     * @param sourceCommand the command of the resource (may be null)
     * @return the form thats being shown, notice that you can still manipulate
     * some states of the form before it actually appears
     */
    public Form showForm(String resourceName, Command sourceCommand) {
        Form f = (Form)createContainer(fetchResourceFile(), resourceName);
        showForm(f, sourceCommand, null);
        return f;
    }

    /**
     * This method allows binding an action that should occur before leaving the given
     * form, e.g. memory cleanup
     *
     * @param f the form being left
     */
    protected void exitForm(Form f) {
    }

    /**
     * This method allows binding an action that should occur before showing the given
     * form
     *
     * @param f the form about to be shown
     */
    protected void beforeShow(Form f) {
    }

    /**
     * This method allows binding an action that should occur immediately after showing the given
     * form
     *
     * @param f the form that was just shown
     */
    private void postShowImpl(Form f) {
        postShow(f);
        analyticsCallback(f.getName(), previousFormName(f));
    }

    /**
     * This method allows binding an action that should occur immediately after showing the given
     * form
     *
     * @param f the form that was just shown
     */
    protected void postShow(Form f) {
    }

    /**
     * This method allows binding an action that should occur before showing the given
     * container
     *
     * @param c the container about to be shown
     */
    protected void beforeShowContainer(Container c) {
    }

    /**
     * This method allows binding an action that should occur immediately after showing the given
     * container
     *
     * @param c the container that was just shown
     */
    protected void postShowContainer(Container c) {
    }

    /**
     * This method allows binding logic that should occur before creating the root object
     * e.g. a case where a created form needs data fetched for it.
     * 
     * @param rootName the name of the root to be created from the resource file
     */
    protected void onCreateRoot(String rootName) {
    }

    /**
     * Indicates that the UIBuilder should cache resources in memory and never release them.
     * This is useful with small resource files or high RAM devices since it saves the cost
     * of constantly fetching the res file from the jar whenever moving between forms.
     * This can be toggled in the properties (e.g. jad) using the flag: cacheResFile (true/false)
     * which defaults to false.
     *
     * @return the keepResourcesInRam
     */
    public boolean isKeepResourcesInRam() {
        return keepResourcesInRam;
    }

    /**
     * Indicates that the UIBuilder should cache resources in memory and never release them.
     * This is useful with small resource files or high RAM devices since it saves the cost
     * of constantly fetching the res file from the jar whenever moving between forms.
     * This can be toggled in the properties (e.g. jad) using the flag: cacheResFile (true/false)
     * which defaults to false.
     * 
     * @param keepResourcesInRam the keepResourcesInRam to set
     */
    public void setKeepResourcesInRam(boolean keepResourcesInRam) {
        this.keepResourcesInRam = keepResourcesInRam;
    }

    /**
     * Returns either the parent form or the component bellow the embedded container
     * above c.
     * 
     * @param c the component whose root ancestor we should find
     * @return the root
     */
    protected Container getRootAncestor(Component c)  {
        while(c.getParent() != null && !(c.getParent() instanceof EmbeddedContainer)) {
            c = c.getParent();
        }
        return (Container)c;
    }

    class FormListener implements ActionListener, Runnable {
        private Command currentAction;
        private ActionEvent currentActionEvent;
        private Form destForm;
        private String nextForm;

        public FormListener() {
        }

        public FormListener(Form destForm, String nextForm) {
            this.destForm = destForm;
            this.nextForm = nextForm;
        }

        public FormListener(Command currentAction, ActionEvent currentActionEvent, Form destForm) {
            this.currentAction = currentAction;
            this.currentActionEvent = currentActionEvent;
            this.destForm = destForm;
        }

        private void waitForForm(Form f) {
            while(Display.getInstance().getCurrent() != f) {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }

            Display.getInstance().callSerially(this);
        }

        public void run() {
            if(currentAction != null) {
                if(Display.getInstance().isEdt()) {
                    postAsyncCommand(currentAction, currentActionEvent);
                } else {
                    asyncCommandProcess(currentAction, currentActionEvent);

                    // wait for the destination form to appear before moving back into the Codename One thread
                    waitForForm(destForm);
                }
            } else {
                if(Display.getInstance().isEdt()) {
                    if(Display.getInstance().getCurrent() != null) {
                        exitForm(Display.getInstance().getCurrent());
                    }
                    Form f = (Form)createContainer(fetchResourceFile(), nextForm);
                    beforeShow(f);
                    f.show();
                    postShowImpl(f);
                } else {
                    if(processBackground(destForm)) {
                        waitForForm(destForm);
                    }
                }
            }
        }

        public void actionPerformed(ActionEvent evt) {
            Command cmd = evt.getCommand();
            if(cmd == null) {
                // this is a show listener we should spawn the background thread
                if(evt.getSource() instanceof Form) {
                    // prevent a dialog from triggerring the background processing again
                    ((Form)evt.getSource()).removeShowListener(this);
                    Display.getInstance().startThread(this, "UIBuilder Async").start();
                    return;
                }

                handleComponentAction((Component)evt.getSource(), evt);
                return;
            }

            processCommandImpl(evt, cmd);
            if(evt.isConsumed()) {
                return;
            }
            String action = (String)cmd.getClientProperty(COMMAND_ACTION);
            if(action != null && action.length() > 0) {
                if(action.equals("$Minimize")) {
                    Display.getInstance().minimizeApplication();
                    return;
                }
                if(action.equals("$Exit")) {
                    Display.getInstance().exitApplication();
                    return;
                }
                if(action.equals("$Execute")) {
                    Display.getInstance().execute((String)cmd.getClientProperty(COMMAND_ARGUMENTS));
                    return;
                }
                if(action.equals("$Back")) {
                    back(evt.getComponent());
                    return;
                }

                if(action.startsWith("!")) {
                    action = action.substring(1);
                    Form currentForm = Display.getInstance().getCurrent();
                    if(currentForm != null) {
                        exitForm(currentForm);
                    }
                    int pos = action.indexOf(';');
                    String firstScreen = action.substring(0, pos);
                    String nextScreen = action.substring(pos + 1, action.length());
                    Form f = (Form)createContainer(fetchResourceFile(), firstScreen);
                    beforeShow(f);
                    if(Display.getInstance().getCurrent().getBackCommand() == cmd) {
                        f.showBack();
                    } else {
                        f.show();
                    }
                    postShowImpl(f);
                    Display.getInstance().startThread(new FormListener(f, nextScreen), "UIBuilder Next Form").start();
                    return;
                }

                if(action.startsWith("@")) {
                    action = action.substring(1);
                    Form currentForm = Display.getInstance().getCurrent();
                    if(currentForm != null) {
                        exitForm(currentForm);
                    }
                    Form f = (Form)createContainer(fetchResourceFile(), action);
                    beforeShow(f);
                    if(Display.getInstance().getCurrent().getBackCommand() == cmd) {
                        f.showBack();
                    } else {
                        f.show();
                    }
                    postShowImpl(f);
                    Display.getInstance().startThread(new FormListener(cmd, evt, f), "UIBuilder @").start();
                    return;
                }

                evt.consume();
                showContainer(action, cmd, evt.getComponent());
            }
        }
    }
}
