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

import com.l2fprod.common.swing.JOutlookBar;
import com.codename1.ui.resource.util.CodenameOneComponentWrapper;
import com.codename1.ui.CodenameOneAccessor;
import com.codename1.ui.Display;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.plaf.Accessor;
import com.codename1.ui.resource.util.SwingRenderer;
import com.codename1.ui.table.TableLayout;
import com.codename1.ui.util.EditableResources;
import com.codename1.ui.util.EmbeddedContainer;
import com.codename1.ui.util.Resources;
import com.codename1.ui.util.UIBuilderOverride;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceAdapter;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import javax.swing.AbstractAction;
import javax.swing.AbstractCellEditor;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.event.CellEditorListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.tree.DefaultXTreeCellEditor;
import org.jdesktop.swingx.tree.DefaultXTreeCellRenderer;

/**
 * Editor implementing the GUI builder functionality
 *
 * @author Shai Almog
 */
public class UserInterfaceEditor extends BaseForm {
    private javax.swing.Timer repainter;
    private static final Object PROPERTIES_DIFFER_IN_VALUE = new Object();
    private static final DataFlavor CODENAMEONE_COMPONENT_FLAVOR = new DataFlavor(com.codename1.ui.Component.class, "CodenameOne Component");
    static final int PROPERTY_CUSTOM = 1000;
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

    private static final String TYPE_KEY = "$TYPE_NAME$";

    private UIBuilderOverride builder;

    private static final String[][] PROPERTY_TOOLTIPS = {
        {"Name", "The name of the given component is used<br>to lookup the component in the code and refer<br>to it"},
        {"LabelForComponent", "Associates a label with the component to provide<br>various features such as tickering of the label when<br>the component receives focus"},
        //{"PreferredW", "Normally this property should not be customized since<br>its calculated automatically. This property indicates<br>the size in pixels requested by the component<br>from the layout."},
        //{"PreferredH", "Normally this property should not be customized since<br>its calculated automatically. This property indicates<br>the size in pixels requested by the component<br>from the layout."},
        {"NextFocusUp", "Indicates the component that should receive focus<br>after this component."},
        {"NextFocusDown", "Indicates the component that should receive focus<br>after this component."},
        {"NextFocusLeft", "Indicates the component that should receive focus<br>after this component."},
        {"NextFocusRight", "Indicates the component that should receive focus<br>after this component."},
        {"UIID", "Identifies the component in the theme allowing<br>the theme to assign a style to this component"},
        {"Focusable", "Indicates the component should/shouldn't receive<br>key focus"},
        {"Enabled", "Indicates the component is/isn't enabled<br>a disabled component is usually blocked and will<br>use the disabled style."},
        {"ScrollVisible", "Allows hiding the scrollbar for this component<br>enabling this doesn't cause the scrollbar to appear<br>if no scrolling is necessary..."},
        {"Text", "The text to render within the component"},
        //{"Alignment", "Placement of the text in the component"},
        {"Layout", "Defines how components are organized within the<br>container in a way that makes sense for<br>multiple resolutions and device rotation"},
        {"Icon", "Image to display on the component"},
        {"Gap", "Spacing between the text and icon within the component"},
        {"VerticalAlignment", "Placement of the elements within the component"},
        {"TextPosition", "The position of the text relative to the icon"},
        {"Title", "The title of the form"},
        {"Columns", "The number of columns allows the layout<br>to estimate the space required for the component"},
        {"Rows", "The number of rows allows the layout<br>to estimate the space required for the component"},
        {"Hint", "Faded out text appearing when there is no<br>actual text in the component"},
        {"HintIcon", "Icon to show when there is no<br>actual text in the component"},
        {"ItemGap", "Hardcoded gap between elements in the list"},
        {"CyclicFocus", "When true if a user clicks down on the last component<br>in the form the first component in the form will be selected"},
        {"Command", "Allows assigning an action to a button such as<br>navigation, exit minimize etc."},
        {"Scrollable X", "Indicates whether scrolling on the X axis should<br>be enabled for this container"},
        {"Scrollable Y", "Indicates whether scrolling on the Y axis should<br>be enabled for this container"},
        {"Next Form", "Allows defining a form that will show after this form<br>this causes a background process to initiate after which the<br>next form is shown, this is useful for splash screens"},
        {"Group",  "A unique name allowing radio buttons who share the<br>same group name to be associated with one another"},
        {"Selected",  "Indicates if a checkbox/radiobutton is checked"},
        {"Renderer", "The container that is responsible for drawing the items<br>in the model"},
        {"RolloverIcon",  "Icon shown when the component is selected"},
        {"PressedIcon", "Icon shown when the component is pressed"},
        {"RTL",  "Toggles Right To Left language support for this component<br>normally this is toggled globally but sometimes special<br>cases are needed"},
        {"Infinite",  "Indicates the slider should go back and forth<br>indicating an indetermined length operation"},
        {"Progress", "The amount of the slider that should appear filled"},
        {"Vertical",  "A slider may be made vertical which is sometimes<br>used for volume sliders"},
        {"Editable",  "Enables the user to change the value of the slider"},
        {"Increments", "Increase value for every key interaction<br>by the user"},
        {"RenderPercentageOnTop",  "Shows a percentage value on top of the slider"},
        {"MaxValue",  "When this value is reached the slider<br>would be full"},
        {"MinValue", "When this value is set the slider<br>would be empty"},
        {"DialogUIID",  "The UIID of the dialog is different from the<br>standard UIID which represents the entire screen<br>including transparent areas"},
        {"FixedSelection",  "Indicates the behavior of the list on<br>user key operations one of: <ol><li>None</li><li>Cyclic - when reaching last element, cycle to the first</li><li>One Element From Edge - only reaches the edge when the list is finished</li><li>Lead - selection is glued to the top</li><li>Center - selection is fixed at the center</li><li>Trail - selection is fixed at the bottom</li></ol>"},
        {"Orientation", "A list may scroll top bottom or right to left<br>this is very useful for an image list carousel UI"},
        {"LeadComponent",  "Indicates that the container is realy a compound<br>component where one of its children needs to take over the<br>management of the entire hierarchy"},
        {"TabPlacement",  "Tabs can be at the top/bottom/left/right<br>of the tabs container"},
        {"TabTextPosition", "Indicates the position of the text in the tabs<br>relatively to the icons in the tabs"},
        {"DisabledIcon",  "Icon shown on the component when its disabled"},
        {"Embed",  "Container to embed from the resource file"},
        {"Toggle", "Indicates whether this is a toggle button<br>if so the component will render like a regular button<br>when its selected the pressed style will be used though"},
        {"ThumbImage",  "Allows an image to be placed on the slider where<br>the full indicator ends"},
        {"DialogPosition",  "Positions the dialog in one of the 5<br>stanard positions North, Sounth, East, West, Center"},
        {"GrowByContent", "Indicates the text component should/shouldn't grow<br>to accomodate the content within it"},
        {"LayoutConstraint",  "Specific to the type of layout of the parent<br>border and table layouts allow customizing behavior<br>on an individual component basis"},
        {"ListItems",  "The elements within the list"},
        {"Commands", "These create the menu UI appearing on a device<br>commands allows assigning an action such as<br>navigation, exit minimize etc."},
        {"Constraint", "Allows defining limits to the text component<br>e.g. numeric only or a password field"},
        {"MaxSize", "Limits the number of characters that can be<br>typed into the text component"},
        {"TensileDragEnabled", "Enables/Disables the tensile drag, the tensile drag<br>allows a user to drag the screen out of bounds and have it<br>snap back to place"},
        {"TactileTouch", "Enables/Disables tactile touch which causes the device to vibrate when the<br>user touches this component"},
        {"SnapToGrid", "Enables/Disables snap to grid which tries to align<br>components based on an invisible lines between them"},
        {"Flatten", "Makes the component effectively opaque by blending the backgrounds into an<br>image in memory so the layer of underlying components is only<br>drawn once when this component is repainted. <b>This has a significant memory overhead</b>"},
        {"DisposeWhenPointerOutOfBounds", "Indicates whether the dialog will dispose if the user touches outside the dialog bounds"}
    };

    private static final String[] HARDCODED_COMPONENT_PROPERTIES = {
        "Name", "LabelForComponent", /*"PreferredW", "PreferredH", */"NextFocusUp",
        "NextFocusDown", "NextFocusLeft", "NextFocusRight", "UIID", "Focusable",
        "Enabled", "ScrollVisible"
    };
    private static final int[] HARDCODED_COMPONENT_PROPERTY_KEYS = {
        PROPERTY_NAME, PROPERTY_LABEL_FOR, /*PROPERTY_PREFERRED_WIDTH, PROPERTY_PREFERRED_HEIGHT, */PROPERTY_NEXT_FOCUS_UP,
        PROPERTY_NEXT_FOCUS_DOWN, PROPERTY_NEXT_FOCUS_LEFT, PROPERTY_NEXT_FOCUS_RIGHT, PROPERTY_UIID, PROPERTY_FOCUSABLE,
        PROPERTY_ENABLED, PROPERTY_SCROLL_VISIBLE
    };
    private static final Class[] HARDCODED_COMPONENT_PROPERTY_CLASSES = {
        String.class, com.codename1.ui.Component.class, /*Integer.class, Integer.class,*/ com.codename1.ui.Component.class,
        com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.Component.class, String.class, Boolean.class,
        Boolean.class, Boolean.class, String.class
    };
    private static final String[] SUPPORTED_COMPONENT_PROPERTIES = {
        "Text", /*"Alignment",*/ "Layout",
        "Icon", "Gap", "VerticalAlignment", "TextPosition", "Title",
        "Components", "Columns", "Rows", "Hint", "HintIcon",
        "ItemGap", "CyclicFocus", "Command", "Scrollable X", "Scrollable Y",
        "Next Form", "Group", "Selected", "Renderer",
        "RolloverIcon", "PressedIcon",
        "RTL", "Infinite", "Progress",
        "Vertical", "Editable", "Increments",
        "RenderPercentageOnTop", "MaxValue", "MinValue",
        "DialogUIID", "FixedSelection", "Orientation",
        "LeadComponent", "TabPlacement", "TabTextPosition",
        "DisabledIcon", "Embed", "Toggle",
        "ThumbImage", "DialogPosition", "GrowByContent",
        "Constraint", "MaxSize",
        "TensileDragEnabled", "TactileTouch", "SnapToGrid",
        "Flatten", "DisposeWhenPointerOutOfBounds", 
        "CloudBoundProperty", "CloudDestinationProperty"
    };
    private static final int[] SUPPORTED_COMPONENT_KEYS = {
        PROPERTY_TEXT, /*PROPERTY_ALIGNMENT,*/ PROPERTY_LAYOUT,
        PROPERTY_ICON, PROPERTY_GAP, PROPERTY_VERTICAL_ALIGNMENT, PROPERTY_TEXT_POSITION, PROPERTY_TITLE,
        PROPERTY_COMPONENTS, PROPERTY_COLUMNS, PROPERTY_ROWS, PROPERTY_HINT, PROPERTY_HINT_ICON,
        PROPERTY_ITEM_GAP, PROPERTY_CYCLIC_FOCUS, PROPERTY_COMMAND, PROPERTY_SCROLLABLE_X, PROPERTY_SCROLLABLE_Y,
        PROPERTY_NEXT_FORM, PROPERTY_RADIO_GROUP, PROPERTY_SELECTED, PROPERTY_LIST_RENDERER,
        PROPERTY_ROLLOVER_ICON, PROPERTY_PRESSED_ICON,
        PROPERTY_RTL, PROPERTY_INFINITE, PROPERTY_PROGRESS,
        PROPERTY_VERTICAL, PROPERTY_EDITABLE, PROPERTY_INCREMENTS,
        PROPERTY_RENDER_PERCENTAGE_ON_TOP, PROPERTY_MAX_VALUE, PROPERTY_MIN_VALUE,
        PROPERTY_DIALOG_UIID, PROPERTY_LIST_FIXED, PROPERTY_LIST_ORIENTATION,
        PROPERTY_LEAD_COMPONENT, PROPERTY_TAB_PLACEMENT, PROPERTY_TAB_TEXT_POSITION,
        PROPERTY_DISABLED_ICON, PROPERTY_EMBED, PROPERTY_TOGGLE_BUTTON,
        PROPERTY_SLIDER_THUMB, PROPERTY_DIALOG_POSITION, PROPERTY_TEXT_AREA_GROW,
        PROPERTY_TEXT_CONSTRAINT, PROPERTY_TEXT_MAX_LENGTH,
        PROPERTY_TENSILE_DRAG_ENABLED, PROPERTY_TACTILE_TOUCH, PROPERTY_SNAP_TO_GRID,
        PROPERTY_FLATTEN, PROPERTY_DISPOSE_WHEN_POINTER_OUT,
        PROPERTY_CLOUD_BOUND_PROPERTY, PROPERTY_CLOUD_DESTINATION_PROPERTY
    };
    private final Class[] SUPPORTED_COMPONENT_PROPERTY_CLASSES = {
        String.class, /*Integer.class,*/ com.codename1.ui.layouts.Layout.class,
        com.codename1.ui.Image.class, Integer.class, Integer.class, Integer.class, String.class,
        com.codename1.ui.Component[].class, Integer.class, Integer.class, String.class, com.codename1.ui.Image.class,
        Integer.class, Boolean.class, com.codename1.ui.Command.class, Boolean.class, Boolean.class,
        String.class, String.class, Boolean.class, com.codename1.ui.list.ListCellRenderer.class,
        com.codename1.ui.Image.class, com.codename1.ui.Image.class,
        Boolean.class, Boolean.class, Integer.class,
        Boolean.class, Boolean.class, Integer.class,
        Boolean.class, Integer.class, Integer.class,
        String.class, Integer.class, Integer.class,
        com.codename1.ui.Component.class, Integer.class, Integer.class,
        com.codename1.ui.Image.class, String.class, Boolean.class,
        com.codename1.ui.Image.class, String.class, Boolean.class,
        Integer.class, Integer.class,
        Boolean.class, Boolean.class, Boolean.class,
        Boolean.class, Boolean.class,
        String.class, String.class
    };
    private final int[] FAKE_COMPONENT_PROPERTY_KEYS = {
        PROPERTY_LAYOUT_CONSTRAINT, PROPERTY_LIST_ITEMS, PROPERTY_COMMANDS, PROPERTY_BASE_FORM
    };
    private final String[] FAKE_COMPONENT_PROPERTIES = {
        "LayoutConstraint", "ListItems", "Commands"
    };


    private static final Integer[] TEXT_AREA_CONSTRAINT_KEY_MAP = new Integer[] {
        com.codename1.ui.TextArea.ANY,
        com.codename1.ui.TextArea.NUMERIC,
        com.codename1.ui.TextArea.DECIMAL,
        com.codename1.ui.TextArea.PASSWORD,
        com.codename1.ui.TextArea.NUMERIC | com.codename1.ui.TextArea.PASSWORD,
        com.codename1.ui.TextArea.EMAILADDR,
        com.codename1.ui.TextArea.PHONENUMBER,
        com.codename1.ui.TextArea.URL,
        com.codename1.ui.TextArea.NON_PREDICTIVE | com.codename1.ui.TextArea.ANY,
        com.codename1.ui.TextArea.NON_PREDICTIVE | com.codename1.ui.TextArea.NUMERIC,
        com.codename1.ui.TextArea.NON_PREDICTIVE | com.codename1.ui.TextArea.EMAILADDR,
        com.codename1.ui.TextArea.NON_PREDICTIVE | com.codename1.ui.TextArea.PHONENUMBER,
        com.codename1.ui.TextArea.NON_PREDICTIVE | com.codename1.ui.TextArea.URL,
    };
    private static final String[] TEXT_AREA_CONSTRAINT_NAME_MAP = new String[] {
        "Any",
        "Numeric",
        "Decimal",
        "Password",
        "Numeric Password",
        "E-mail",
        "Phone Number",
        "URL",
        "Any - None Predictive",
        "Numeric - None Predictive",
        "E-mail - None Predictive",
        "Phone Number - None Predictive",
        "URL - None Predictive",
    };


    private static com.codename1.ui.Component[] clipboard;
    private static String copiedResourceName;
    private CustomComponent[] customComponents;
    private EditableResources res;
    private com.codename1.ui.Container containerInstance;
    private String name;
    private boolean lockForDragging = false;
    private ResourceEditorView view;
    private String userStateMachineCode;
    private long lastModifiedStateMachineCode;
    private File userStateMachineFile;

    private boolean isActualContainer(Object cmp) {
        return cmp instanceof com.codename1.ui.Container &&
                (cmp.getClass() == com.codename1.ui.Container.class || 
                cmp instanceof com.codename1.ui.Form ||
                cmp instanceof com.codename1.ui.ComponentGroup ||
                cmp instanceof com.codename1.ui.Tabs);
    }

    private Properties projectGeneratorSettings;

    private void validateLoadedStateMachineCode() {
        if(projectGeneratorSettings != null) {
            DataInputStream r = null;
            try {
                userStateMachineFile = new File(projectGeneratorSettings.getProperty("userClassAbs"));
                if(!userStateMachineFile.exists()) {
                    File parentDir = ResourceEditorView.getLoadedFile().getParentFile();
                    userStateMachineFile = new File(parentDir, "userclasses/StateMachine.java");
                    if(!userStateMachineFile.exists()) {
                        projectGeneratorSettings = null;
                        return;
                    }
                }
                r = new DataInputStream(new FileInputStream(userStateMachineFile));
                byte[] data = new byte[(int) userStateMachineFile.length()];
                r.readFully(data);
                userStateMachineCode = new String(data);
                lastModifiedStateMachineCode = userStateMachineFile.lastModified();
            } catch (IOException ex) {
                ex.printStackTrace();
            } finally {
                try {
                    r.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    /** Creates new form UserInterfaceBuilder */
    public UserInterfaceEditor(String name, EditableResources res, Properties projectGeneratorSettings, ResourceEditorView view) {
        this.view = view;
        this.projectGeneratorSettings = projectGeneratorSettings;
        if(res.isOverrideMode() && !res.isOverridenResource(name)) {
            setOverrideMode(true, view.getComponent());
        }
        validateLoadedStateMachineCode();

        // make sure that if I click a UI and have a theme but it wasn't loaded that
        // it will be applied properly and initialized. Without this the UI builder
        // acts "funny"
        if(!ThemeEditor.wasThemeLoaded()) {
            if(res.getThemeResourceNames().length > 0) {
                new ThemeEditor(res, res.getThemeResourceNames()[0], res.getTheme(res.getThemeResourceNames()[0]), view);
            } else {
                new ThemeEditor(res, "", new Hashtable(), view);
            }
        }
        builder = new UIBuilderOverride(this);
        UIBuilderOverride.setIgnorBaseForm(true);
        com.codename1.ui.plaf.Accessor.setResourceBundle(null);
        this.res = res;
        com.codename1.ui.html.DefaultDocumentRequestHandler.setResFile(res);

        this.name = name;
        initComponents();
        simulateDevice.setEnabled(projectGeneratorSettings != null);

        
        /*String themeList = Preferences.userNodeForPackage(getClass()).get("ThemeList", null);
        String themeListSelection = Preferences.userNodeForPackage(getClass()).get("ThemeListSelection", null);
        if(themeList == null) {
            if(res.getThemeResourceNames().length > 0) {
                previewTheme.setModel(new DefaultComboBoxModel(res.getThemeResourceNames()));
            }
        } else {
            Vector themeInitialNames = new Vector();
            themeInitialNames.addAll(Arrays.asList(res.getThemeResourceNames()));
            String[] tokens = themeList.split(";");
            themeInitialNames.addAll(Arrays.asList(tokens));
            previewTheme.setModel(new DefaultComboBoxModel(themeInitialNames));
            if(themeListSelection != null ) {
                if(themeListSelection.indexOf('@') < 0) {
                    for(String s : res.getThemeResourceNames()) {
                        if(s.equals(themeListSelection)) {
                            previewTheme.setSelectedItem(themeListSelection);
                            applyThemePreview(themeListSelection);
                            break;
                        }
                    }
                } else {
                    previewTheme.setSelectedItem(themeListSelection);
                    applyThemePreview(themeListSelection);
                }
            }
        }
        previewTheme.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                if(value != null && value.toString().indexOf("@") > -1) {
                    String[] t = value.toString().split("@");
                    value = new File(t[0]).getName() + ": " + t[1];
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });*/

        resourceBundle.setModel(new DefaultComboBoxModel(res.getL10NResourceNames()));
        resourceBundle.setEnabled(res.getL10NResourceNames().length > 1);
        localizeTable.setModel(new LocalizationTableModel());
        if(res.getL10NResourceNames().length > 0) {
            resourceBundle.setSelectedIndex(0);
        }
        boolean enable = true;
        if(projectGeneratorSettings == null) {
            enable = false;
        } 
        whyAreEventsDisabled.setVisible(!enable);
        bindActionEvent.setEnabled(enable);
        bindBeforeShow.setEnabled(enable);
        bindOnCreate.setEnabled(enable);
        bindExitForm.setEnabled(enable);
        bindPostShow.setEnabled(enable);
        bindListModel.setEnabled(enable);
        
        customComponents = PickMIDlet.getCustomComponents();
        com.codename1.ui.Display.init(null);
        if(res.getResourceObject(name) == null) {
            // creating new UI root component, let the user choose which one
            JComboBox pick = new JComboBox(new String[] {"Form", "Dialog", "Container"});
            JOptionPane.showMessageDialog(JFrame.getFrames()[0], pick, "Pick Root", JOptionPane.QUESTION_MESSAGE);
            switch(pick.getSelectedIndex()) {
                case 0:
                    containerInstance = new com.codename1.ui.Form();
                    containerInstance.putClientProperty(TYPE_KEY, "Form");
                    containerInstance.setName("Form 1");
                    ((com.codename1.ui.Form)containerInstance).show();
                    break;
                case 1:
                    containerInstance = new com.codename1.ui.Dialog();
                    containerInstance.putClientProperty(TYPE_KEY, "Dialog");
                    containerInstance.setName("Dialog 1");
                    ((com.codename1.ui.Dialog)containerInstance).show(0, 0, 0, 0, true, false);
                    break;
                default:
                    containerInstance = new com.codename1.ui.Container();
                    containerInstance.putClientProperty(TYPE_KEY, "Container");
                    containerInstance.setName("Container 1");
                    com.codename1.ui.Form hiddenForm = new com.codename1.ui.Form();
                    hiddenForm.setLayout(new com.codename1.ui.layouts.BorderLayout());
                    hiddenForm.addComponent(com.codename1.ui.layouts.BorderLayout.CENTER, containerInstance);
                    hiddenForm.show();
                    break;
            }
        } else {
            containerInstance = builder.createContainer(res, name);
            if(builder.getBaseFormName() != null) {
                containerInstance.putClientProperty("%base_form%", builder.getBaseFormName());
            }
            if(containerInstance instanceof com.codename1.ui.Form) {
                if(containerInstance instanceof com.codename1.ui.Dialog) {
                    ((com.codename1.ui.Dialog)containerInstance).show(0, 0, 0, 0, true, false);
                } else {
                    ((com.codename1.ui.Form)containerInstance).show();
                }
            } else {
                    com.codename1.ui.Form frm = containerInstance.getComponentForm();
                    if(frm == null) {
                        frm = new com.codename1.ui.Form();
                        frm.setLayout(new BorderLayout());
                        frm.addComponent(com.codename1.ui.layouts.BorderLayout.CENTER, containerInstance);
                    }
                    frm.show();
            }
        }

        InputMap input = componentHierarchy.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "delete");
        final AbstractAction delete = new AbstractAction("Delete") {
            {
                putValue(AbstractAction.NAME, "Delete");
            }
            public void actionPerformed(ActionEvent e) {
                deleteSelection();
            }
        };
        componentHierarchy.getActionMap().put("delete", delete);

        componentHierarchy.setEditable(true);
        componentHierarchy.setModel(new ComponentHierarchyModel(containerInstance));
        DefaultXTreeCellRenderer defRend = new DefaultXTreeCellRenderer() {
            private boolean hasBoundEvent(com.codename1.ui.Component cmp) {
                if(hasActionEventCode(cmp) || hasOnCreateCode(cmp) || hasOnListModelCode(cmp)) {
                    return true;
                }
                if(cmp instanceof com.codename1.ui.Form) {
                    com.codename1.ui.Form frm = (com.codename1.ui.Form)cmp;
                    return hasFormBeforeCode(frm) || hasFormExitCode(frm) || hasFormPostCode(frm);
                }
                if(cmp instanceof com.codename1.ui.Container) {
                    com.codename1.ui.Container frm = (com.codename1.ui.Container)cmp;
                    return hasContainerBeforeCode(frm) || hasContainerPostCode(frm);
                }
                return false;
            }

            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                String cls = value.getClass().getName().substring(value.getClass().getName().lastIndexOf('.') + 1);
                if(value instanceof com.codename1.ui.Component) {
                    com.codename1.ui.Component cmp = (com.codename1.ui.Component)value;
                    if(hasBoundEvent(cmp)) {
                        return super.getTreeCellRendererComponent(tree, "<html><body><b>" + ((com.codename1.ui.Component)value).getName() + "[" + cls + "]</b>", sel, expanded, leaf, row, hasFocus);
                    } 
                }
                return super.getTreeCellRendererComponent(tree, ((com.codename1.ui.Component)value).getName() + "[" + cls + "]", sel, expanded, leaf, row, hasFocus);
            }
        };
        componentHierarchy.setRolloverEnabled(true);
        componentHierarchy.setCellRenderer(defRend);
        componentHierarchy.setCellEditor(new DefaultXTreeCellEditor(componentHierarchy, defRend) {
            private Object value;
            public Component getTreeCellEditorComponent(JTree tree, Object value, boolean isSelected, boolean expanded, boolean leaf, int row) {
                this.value = value;
                return super.getTreeCellEditorComponent(tree, ((com.codename1.ui.Component)value).getName(), isSelected, expanded, leaf, row);
            }

            public Object getCellEditorValue() {
                return (String)super.getCellEditorValue();
            }
        });
        componentHierarchy.addHighlighter(new ColorHighlighter(HighlightPredicate.ROLLOVER_ROW, 
                null, Color.RED));      
        MouseListener rightClickListener = new MouseListener() {
            private void selectCmp(com.codename1.ui.Component cmp) {
                if(cmp == null) {
                    cmp = containerInstance;
                }
                cmp = getActualComponent(cmp);
                if(cmp == null) {
                    return;
                }
                if(componentHierarchy.getSelectionPaths() != null) {
                    for(TreePath p : componentHierarchy.getSelectionPaths()) {
                        if(p.getLastPathComponent() == cmp) {
                            return;
                        }
                    }
                }
                Object[] arr = pathToComponent(cmp);
                if(arr != null && arr.length > 0) {
                    componentHierarchy.setSelectionPath(new TreePath(arr));
                }
            }
            public void mouseClicked(MouseEvent e) {
                if(BaseForm.isRightClick(e)) {
                    if(!(e.getSource() instanceof JTree)) {
                        com.codename1.ui.Component cmp = containerInstance.getComponentAt(e.getX(), e.getY());
                        selectCmp(cmp);
                    }
                    AbstractAction deleteAction = new AbstractAction("Delete") {
                        public void actionPerformed(ActionEvent e) {
                            deleteSelection();
                        }
                    };
                    AbstractAction copyAction = new AbstractAction("Copy") {
                        public void actionPerformed(ActionEvent e) {
                            TreePath[] sels = componentHierarchy.getSelectionPaths();
                            if(sels != null) {
                                copiedResourceName = UserInterfaceEditor.this.name;
                                clipboard = new com.codename1.ui.Component[sels.length];
                                for(int iter = 0 ; iter < sels.length ; iter++) {
                                    com.codename1.ui.Component cmp = (com.codename1.ui.Component)sels[iter].getLastPathComponent();
                                    if(cmp == containerInstance) {
                                        continue;
                                    }
                                    clipboard[iter] = cmp;
                                }
                            }
                        }
                    };
                    AbstractAction cutAction = new AbstractAction("Cut") {
                        public void actionPerformed(ActionEvent e) {
                            TreePath[] sels = componentHierarchy.getSelectionPaths();
                            if(sels != null) {
                                componentHierarchy.setSelectionPath(sels[0].getParentPath());
                                ((ComponentPropertyEditorModel)properties.getModel()).setComponents(new com.codename1.ui.Component[] {
                                    (com.codename1.ui.Component)sels[0].getParentPath().getLastPathComponent()});
                                copiedResourceName = UserInterfaceEditor.this.name;
                                clipboard = new com.codename1.ui.Component[sels.length];
                                for(int iter = 0 ; iter < sels.length ; iter++) {
                                    com.codename1.ui.Component cmp = (com.codename1.ui.Component)sels[iter].getLastPathComponent();
                                    if(cmp == containerInstance) {
                                        continue;
                                    }
                                    clipboard[iter] = cmp;
                                    if(cmp.getParent().getUIID().equals("TabbedPane")) {
                                        ((com.codename1.ui.Tabs)cmp.getParent().getParent()).removeTabAt(cmp.getParent().getComponentIndex(cmp));
                                    } else {
                                        removeComponentSync(cmp.getParent(), cmp);
                                    }
                                }
                                containerInstance.revalidate();
                                uiPreview.repaint();
                                ((ComponentHierarchyModel)componentHierarchy.getModel()).fireTreeStructureChanged(new TreeModelEvent(componentHierarchy.getModel(), new TreePath(componentHierarchy.getModel().getRoot())));
                                expandAll(componentHierarchy);
                                saveUI();
                            }
                        }
                    };
                    AbstractAction pasteAction = new AbstractAction("Paste") {
                        public void actionPerformed(ActionEvent e) {
                            TreePath sel = componentHierarchy.getSelectionPath();
                            if(sel != null && clipboard != null) {
                                componentHierarchy.setSelectionPath(sel.getParentPath());
                                com.codename1.ui.Component cmp = (com.codename1.ui.Component)sel.getLastPathComponent();
                                com.codename1.ui.Container cnt;
                                if(!(cmp instanceof com.codename1.ui.Container)) {
                                    cnt = cmp.getParent();
                                } else {
                                    cnt = (com.codename1.ui.Container)cmp;
                                    if(cnt.getLeadComponent() != null) {
                                        cnt = cnt.getLeadParent().getParent();
                                    }
                                }
                                for(com.codename1.ui.Component clip : clipboard) {
                                    com.codename1.ui.Component copiedValue = copyComponent(clip);
                                    if(copiedValue != null) {
                                        clip = copiedValue;
                                    }
                                    clip.setName(findUniqueName(clip.getName()));
                                    if(cnt.getLayout() instanceof com.codename1.ui.layouts.BorderLayout) {
                                        if(cnt.getComponentCount() < 5) {
                                            cnt.addComponent(findAvailableSpotInBorderLayout(cnt), clip);
                                        }
                                    } else {
                                        cnt.addComponent(clip);
                                    }
                                }
                                containerInstance.revalidate();
                                uiPreview.repaint();
                                ((ComponentHierarchyModel)componentHierarchy.getModel()).fireTreeStructureChanged(new TreeModelEvent(componentHierarchy.getModel(), new TreePath(componentHierarchy.getModel().getRoot())));
                                expandAll(componentHierarchy);
                                saveUI();
                            }
                        }
                    };
                    class EditStyle extends AbstractAction {
                        private String prefix;
                        public EditStyle(String name, String prefix) {
                            super(name);
                            this.prefix = prefix;
                        }

                        public void actionPerformed(ActionEvent e) {
                            com.codename1.ui.Component[] cmps = getSelectedComponents();
                            if(cmps != null && cmps.length > 0)  {
                                String uiid = cmps[0].getUIID();
                                String uiidWithPrefix = uiid;
                                if(prefix != null && prefix.length() > 0) {
                                    uiidWithPrefix += "." + prefix;
                                }
                                if(UserInterfaceEditor.this.res.getThemeResourceNames().length == 0) {
                                    JOptionPane.showMessageDialog(UserInterfaceEditor.this, "You need to define a theme in order to edit styles", "No Theme Defined", JOptionPane.ERROR_MESSAGE);
                                    return;
                                }
                                String theme;
                                if(UserInterfaceEditor.this.res.getThemeResourceNames().length > 1) {
                                    JComboBox pickTheme = new JComboBox(UserInterfaceEditor.this.res.getThemeResourceNames());
                                    if(JOptionPane.showConfirmDialog(UserInterfaceEditor.this, pickTheme, "Select Theme", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
                                        return;
                                    }
                                    theme = (String)pickTheme.getSelectedItem();
                                } else {
                                    theme = UserInterfaceEditor.this.res.getThemeResourceNames()[0];
                                }

                                Hashtable h = UserInterfaceEditor.this.res.getTheme(theme);
                                boolean editing = false;
                                for(Object key : h.keySet()) {
                                    if(((String)key).startsWith(uiidWithPrefix)) {
                                        editing = true;
                                    }
                                }
                                AddThemeEntry a = new AddThemeEntry(!editing,
                                        UserInterfaceEditor.this.res, UserInterfaceEditor.this.view,
                                        new Hashtable(h), prefix, theme);
                                a.setKeyValues(uiid, prefix);
                                if(JOptionPane.showConfirmDialog(UserInterfaceEditor.this, a, "Style", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) ==
                                    JOptionPane.OK_OPTION) {
                                    Hashtable tmp = new Hashtable(h);
                                    a.updateThemeHashtable(tmp);
                                    UserInterfaceEditor.this.res.setTheme(theme, tmp);
                                }
                            }
                        }
                    }

                    JPopupMenu popup = new JPopupMenu();
                    popup.add(deleteAction);
                    popup.add(cutAction);
                    popup.add(copyAction);
                    popup.add(pasteAction);
                    JMenu encloseIn = new JMenu("Enclose In");
                    encloseIn.add(new EncloseIn("Container", ENCLOSE_IN_CONTAINER));
                    encloseIn.add(new EncloseIn("Tabs", ENCLOSE_IN_TABS));
                    encloseIn.add(new EncloseIn("Component Group", ENCLOSE_IN_COMPONENT_GROUP));
                    popup.add(encloseIn);

                    JMenu eventsMenu = new JMenu("Events");
                    popup.add(eventsMenu);
                    eventsMenu.add(new AbstractAction("Action Event") {
                        public boolean isEnabled() {
                            return bindActionEvent.isEnabled();
                        }
                        
                        @Override
                        public void actionPerformed(ActionEvent ae) {
                            bindActionEventActionPerformed(ae);
                        }
                    });
                    eventsMenu.add(new AbstractAction("On Create") {
                        public boolean isEnabled() {
                            return bindOnCreate.isEnabled();
                        }

                        @Override
                        public void actionPerformed(ActionEvent ae) {
                            bindOnCreateActionPerformed(ae);
                        }
                    });
                    eventsMenu.add(new AbstractAction("Before Show") {
                        public boolean isEnabled() {
                            return bindBeforeShow.isEnabled();
                        }

                        @Override
                        public void actionPerformed(ActionEvent ae) {
                            bindBeforeShowActionPerformed(ae);
                        }
                    });
                    eventsMenu.add(new AbstractAction("Post Show") {
                        public boolean isEnabled() {
                            return bindPostShow.isEnabled();
                        }

                        @Override
                        public void actionPerformed(ActionEvent ae) {
                            bindPostShowActionPerformed(ae);
                        }
                    });
                    eventsMenu.add(new AbstractAction("Exit Form") {
                        public boolean isEnabled() {
                            return bindExitForm.isEnabled();
                        }

                        @Override
                        public void actionPerformed(ActionEvent ae) {
                            bindExitFormActionPerformed(ae);
                        }
                    });
                    eventsMenu.add(new AbstractAction("List Model") {
                        public boolean isEnabled() {
                            return bindListModel.isEnabled();
                        }

                        @Override
                        public void actionPerformed(ActionEvent ae) {
                            bindListModelActionPerformed(ae);
                        }
                    });

                    JMenu style = new JMenu("Style");
                    popup.add(style);
                    style.add(new EditStyle("Unselected", null));
                    style.add(new EditStyle("Selected", "sel#"));
                    style.add(new EditStyle("Pressed", "press#"));
                    style.add(new EditStyle("Disabled", "dis#"));
                    popup.show((java.awt.Component)e.getSource(), e.getX(), e.getY());
                }
            }

            public void mousePressed(MouseEvent e) {
            }

            public void mouseReleased(MouseEvent e) {
            }

            public void mouseEntered(MouseEvent e) {
            }

            public void mouseExited(MouseEvent e) {
            }
        };
        componentHierarchy.addMouseListener(rightClickListener);

        expandAll(componentHierarchy);
        final CodenameOneComponentWrapper wrapper = new DraggablePreview(containerInstance);
        wrapper.addMouseListener(rightClickListener);
        uiPreview.add(wrapper);
        properties.setModel(new ComponentPropertyEditorModel());
        ((ComponentPropertyEditorModel)properties.getModel()).setComponents(new com.codename1.ui.Component[] {containerInstance});
        componentHierarchy.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                TreePath[] paths = componentHierarchy.getSelectionPaths();
                bindActionEvent.setText("Action Event");
                bindListModel.setText("List Model");
                bindOnCreate.setText("On Create");
                bindBeforeShow.setText("Before Show");
                bindPostShow.setText("Post Show");
                bindExitForm.setText("Exit Form");
                if(paths != null) {
                    com.codename1.ui.Component[] cmps = new com.codename1.ui.Component[paths.length];
                    for(int iter = 0 ; iter < paths.length ; iter++) {
                        cmps[iter] = (com.codename1.ui.Component)paths[iter].getLastPathComponent();
                    }
                    if(cmps.length == 1) {
                        switchToTab(cmps[0]);
                        com.codename1.ui.Component cmp = cmps[0];
                        if(hasActionEventCode(cmp) || hasOnCreateCode(cmp) || hasOnListModelCode(cmp)) {
                            bindActionEvent.setText("<html><body><b>Action Event");
                        }
                        if(hasOnListModelCode(cmp)) {
                            bindListModel.setText("<html><body><b>List Model");
                        }
                        if(hasOnCreateCode(cmp)) {
                            bindOnCreate.setText("<html><body><b>On Create");
                        }
                        if(cmp instanceof com.codename1.ui.Form) {
                            com.codename1.ui.Form frm = (com.codename1.ui.Form)cmp;
                            if(hasFormBeforeCode(frm)) {
                                bindBeforeShow.setText("<html><body><b>Before Show");
                            }
                            if(hasFormExitCode(frm)) {
                                bindPostShow.setText("<html><body><b>Post Show");
                            }
                            if(hasFormPostCode(frm)) {
                                bindExitForm.setText("<html><body><b>Exit Form");
                            }
                        }
                        if(cmp instanceof com.codename1.ui.Container) {
                            com.codename1.ui.Container frm = (com.codename1.ui.Container)cmp;
                            if(hasContainerBeforeCode(frm)) {
                                bindBeforeShow.setText("<html><body><b>Before Show");
                            }
                            if(hasContainerPostCode(frm)) {
                                bindBeforeShow.setText("<html><body><b>Post Show");
                            }
                        }
                    }
                    ((ComponentPropertyEditorModel)properties.getModel()).setComponents(cmps);
                    wrapper.repaint();
                }
            }
            /**
             * If the component is within a tabbed pane switch to the proper tab index
             */
            private void switchToTab(com.codename1.ui.Component c) {
                com.codename1.ui.Container p = c.getParent();
                while(p != null) {
                    // ok this is a tabbed pane
                    if(p.getUIID().equals("TabbedPane")) {
                        c.setSmoothScrolling(false);
                        p.setSmoothScrolling(false);
                        p.getParent().setSmoothScrolling(false);
                        ((com.codename1.ui.Tabs)p.getParent()).setSelectedIndex(p.getComponentIndex(c));
                        c.getComponentForm().revalidate();
                        return;
                    }
                    c = p;
                    p = c.getParent();
                }
            }
        });
        properties.setDefaultEditor(Object.class, new TableCellEditor() {
            private TableCellEditor currentEditor;
            private List<CellEditorListener> listeners = new ArrayList<CellEditorListener>();
            
            private void registerListeners() {
                for(CellEditorListener l : listeners) {
                    currentEditor.addCellEditorListener(l);
                }
            }

            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                ComponentPropertyEditorModel em = (ComponentPropertyEditorModel)properties.getModel();
                Class rowClass = em.getRowClass(row);
                if(value == PROPERTIES_DIFFER_IN_VALUE) {
                    value = em.getValueAt(row, column, true);
                }
                if(rowClass == String.class) {
                    if(getSelectedComponents()[0] instanceof com.codename1.components.WebBrowser) {
                        if(table.getValueAt(row, 0).equals("body")) {
                            currentEditor = new HTMLBodyEditor();
                            registerListeners();
                            return currentEditor.getTableCellEditorComponent(table, value, isSelected, row, column);
                        }
                    }
                    if(em.getRowId(row) == PROPERTY_CLOUD_BOUND_PROPERTY) {
                        com.codename1.ui.Component[] cmps = em.getComponents();
                        if(cmps != null) {
                            String[] values = cmps[0].getBindablePropertyNames();
                            String[] arr = new String[values.length + 1];
                            System.arraycopy(values, 0, arr, 1, values.length);
                            values = arr;
                            JComboBox cb = new JComboBox(arr);
                            cb.setRenderer(new DefaultListCellRenderer() {
                                public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                                    if(value == null || index < 0) {
                                        value = "[null]";
                                    }
                                    return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                                }
                            });
                            currentEditor = new DefaultCellEditor(cb);
                            registerListeners();
                            return currentEditor.getTableCellEditorComponent(table, value, isSelected, row, column);
                        }
                    }
                    if(em.getRowId(row) == PROPERTY_NEXT_FORM || em.getRowId(row) == PROPERTY_BASE_FORM) {
                        String[] uiNames = UserInterfaceEditor.this.res.getUIResourceNames();
                        Arrays.sort(uiNames);
                        String[] arr = new String[uiNames.length];
                        int dest = 1;
                        for(int iter = 0 ; iter < uiNames.length ; iter++) {
                            if(!uiNames[iter].equals(UserInterfaceEditor.this.name)) {
                                arr[dest] = uiNames[iter];
                                dest++;
                            }
                        }
                        JComboBox cb = new JComboBox(arr);
                        cb.setRenderer(new DefaultListCellRenderer() {
                            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                                if(value == null || index < 0) {
                                    value = "[null]";
                                }
                                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                            }
                        });
                        currentEditor = new DefaultCellEditor(cb);
                        registerListeners();
                        return currentEditor.getTableCellEditorComponent(table, value, isSelected, row, column);
                    }
                    if(em.getRowId(row) == PROPERTY_EMBED) {
                        String[] uiNames = UserInterfaceEditor.this.res.getUIResourceNames();
                        Arrays.sort(uiNames);
                        Vector<String> cmpList = new Vector<String>();
                        for(String current : uiNames) {
                            if(current.equals(UserInterfaceEditor.this.name)) {
                                continue;
                            }
                            UIBuilderOverride tempBuilder = new UIBuilderOverride(null);
                            if(tempBuilder.createContainer(UserInterfaceEditor.this.res, (String)current) instanceof com.codename1.ui.Form) {
                                continue;
                            }
                            cmpList.add(current);
                        }
                        cmpList.add(0, null);
                        JComboBox cb = new JComboBox(cmpList);
                        cb.setRenderer(new DefaultListCellRenderer() {
                            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                                if(value == null || index < 0) {
                                    value = "[null]";
                                }
                                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                            }
                        });
                        currentEditor = new DefaultCellEditor(cb);
                        registerListeners();
                        return currentEditor.getTableCellEditorComponent(table, value, isSelected, row, column);
                    }
                    if(em.getRowId(row) == PROPERTY_UIID || em.getRowId(row) == PROPERTY_DIALOG_UIID) {
                        JComboBox cb = new JComboBox();
                        AddThemeEntry.initUIIDComboBox(cb);
                        currentEditor = new DefaultCellEditor(cb);
                        registerListeners();
                        return currentEditor.getTableCellEditorComponent(table, value, isSelected, row, column);
                    }
                    currentEditor = new DefaultCellEditor(new JTextField((String)value));
                    registerListeners();
                    return currentEditor.getTableCellEditorComponent(table, value, isSelected, row, column);
                }

                if(rowClass == Boolean.class) {
                    JCheckBox c = new JCheckBox();
                    c.setSelected(value != null && (((Boolean)value).booleanValue()));
                    currentEditor = new DefaultCellEditor(c);
                    registerListeners();
                    return currentEditor.getTableCellEditorComponent(table, value, isSelected, row, column);
                }

                if(rowClass == com.codename1.ui.Command.class) {
                    currentEditor = new CommandTableEditor();
                    registerListeners();
                    return currentEditor.getTableCellEditorComponent(table, value, isSelected, row, column);
                }

                if(rowClass == com.codename1.ui.Container.class) {
                    String[] uiElements = UserInterfaceEditor.this.res.getUIResourceNames();
                    Arrays.sort(uiElements, String.CASE_INSENSITIVE_ORDER);
                    String[] nu = new String[uiElements.length + 1];
                    System.arraycopy(uiElements, 0, nu, 1, uiElements.length);
                    JComboBox cb = new JComboBox(nu);
                    currentEditor = new DefaultCellEditor(cb) {
                        @Override
                        public Object getCellEditorValue() {
                            Object o = super.getCellEditorValue();
                            if(o != null && o instanceof String) {
                                UIBuilderOverride tempBuilder = new UIBuilderOverride(null);
                                o = tempBuilder.createContainer(UserInterfaceEditor.this.res, (String)o);
                            }
                            return o;
                        }

                        @Override
                        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                            if(value != null && value instanceof com.codename1.ui.Component) {
                                value = ((com.codename1.ui.Component)value).getName();
                            }
                            return super.getTableCellEditorComponent(table, value, isSelected, row, column);
                        }

                    };
                    registerListeners();
                    return currentEditor.getTableCellEditorComponent(table, value, isSelected, row, column);
                }

                if(rowClass == com.codename1.ui.Command[].class) {
                    currentEditor = new CommandArrayEditor(true, false);
                    registerListeners();
                    return currentEditor.getTableCellEditorComponent(table, value, isSelected, row, column);
                }

                if(rowClass == com.codename1.ui.list.ListCellRenderer.class || rowClass == com.codename1.ui.list.CellRenderer.class) {
                    currentEditor = new ListRendererTableEditor();
                    registerListeners();
                    return currentEditor.getTableCellEditorComponent(table, value, isSelected, row, column);
                }

                if(rowClass == com.codename1.ui.Image[].class) {
                    currentEditor = new ImageArrayEditor();
                    registerListeners();
                    return currentEditor.getTableCellEditorComponent(table, value, isSelected, row, column);
                }

                if(rowClass == Date.class) {
                    currentEditor = new DateEditor();
                    registerListeners();
                    return currentEditor.getTableCellEditorComponent(table, value, isSelected, row, column);
                }

                if(rowClass == String[].class) {
                    currentEditor = new CommandArrayEditor(false, false);
                    registerListeners();
                    return currentEditor.getTableCellEditorComponent(table, value, isSelected, row, column);
                }

                if(rowClass == Object[].class) {
                    currentEditor = new CommandArrayEditor(false, false, true);
                    registerListeners();
                    return currentEditor.getTableCellEditorComponent(table, value, isSelected, row, column);
                }

                if(rowClass == String[][].class) {
                    currentEditor = new CommandArrayEditor(false, true);
                    registerListeners();
                    return currentEditor.getTableCellEditorComponent(table, value, isSelected, row, column);
                }

                if(rowClass == com.codename1.ui.Component.class) {
                    final Vector cmps = new Vector();
                    cmps.addElement(null);
                    if(containerInstance instanceof com.codename1.ui.Form) {
                        findAllChildren(((com.codename1.ui.Form)containerInstance).getContentPane(), cmps);
                    } else {
                        findAllChildren(containerInstance, cmps);
                    }
                    Collections.sort(cmps, new Comparator() {
                        public int compare(Object o1, Object o2) {
                            if(o1 == null) {
                                if(o2 == null) {
                                    return 0;
                                }
                                return -1;
                            }
                            if(o2 == null) {
                                return 1;
                            }
                            com.codename1.ui.Component c1 = (com.codename1.ui.Component)o1;
                            com.codename1.ui.Component c2 = (com.codename1.ui.Component)o2;
                            return String.CASE_INSENSITIVE_ORDER.compare(c1.getName(), c2.getName());
                        }
                    });
                    JComboBox jc = new JComboBox(cmps);
                    jc.setRenderer(new DefaultListCellRenderer() {
                        @Override
                        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                            if(value == null) {
                                value = "[null]";
                            } else {
                                value = ((com.codename1.ui.Component)value).getName();
                            }
                            return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                        }
                    });
                    jc.setKeySelectionManager(new JComboBox.KeySelectionManager() {
                        private String current;
                        private long lastPress;
                            public int selectionForKey(char aKey, ComboBoxModel aModel) {
                                long t = System.currentTimeMillis();
                                aKey = Character.toLowerCase(aKey);
                                if(t - lastPress < 800) {
                                    current += aKey;
                                } else {
                                    current = "" + aKey;
                                }
                                lastPress = t;
                                for(int iter = 0 ; iter < cmps.size() ; iter++) {
                                    com.codename1.ui.Component c = (com.codename1.ui.Component)cmps.get(iter);
                                    if(c != null && c.getName().toLowerCase().startsWith(current)) {
                                        return iter;
                                    }
                                }
                                return -1;
                            }
                        });
                    jc.setSelectedItem(value);
                    currentEditor = new DefaultCellEditor(jc);
                    registerListeners();
                    return currentEditor.getTableCellEditorComponent(table, value, isSelected, row, column);
                }

                if(rowClass == com.codename1.ui.layouts.Layout.class) {
                    // TODO: Add support for flow layout center
                    currentEditor = new LayoutManagerEditor();
                    registerListeners();
                    return currentEditor.getTableCellEditorComponent(table, value, isSelected, row, column);
                }

                if(rowClass == com.codename1.ui.Image.class) {
                    JComboBox jc = new JComboBox();
                    ResourceEditorView.initImagesComboBox(jc, UserInterfaceEditor.this.res, false, true);
                    jc.setSelectedItem(value);
                    currentEditor = new DefaultCellEditor(jc);
                    registerListeners();
                    return currentEditor.getTableCellEditorComponent(table, value, isSelected, row, column);
                }

                if(rowClass == com.codename1.ui.table.TableLayout.Constraint.class) {
                    final com.codename1.ui.Component comp = (com.codename1.ui.Component)getSelectedComponents()[0];
                    final com.codename1.ui.Container cnt = comp.getParent();
                    if(cnt.getLayout() instanceof com.codename1.ui.table.TableLayout) {
                        currentEditor = new TableConstraintEditor();
                        registerListeners();
                        return currentEditor.getTableCellEditorComponent(table, value, isSelected, row, column);
                    }
                }

                if(rowClass == Object.class) {
                    if(em.getRowId(row) == PROPERTY_LAYOUT_CONSTRAINT) {
                        final JComboBox jc = new JComboBox(new String[] {
                            com.codename1.ui.layouts.BorderLayout.CENTER,
                            com.codename1.ui.layouts.BorderLayout.NORTH,
                            com.codename1.ui.layouts.BorderLayout.SOUTH,
                            com.codename1.ui.layouts.BorderLayout.EAST,
                            com.codename1.ui.layouts.BorderLayout.WEST
                        });

                        jc.setSelectedItem(value);
                        currentEditor = new DefaultCellEditor(jc);
                        registerListeners();
                        return currentEditor.getTableCellEditorComponent(table, value, isSelected, row, column);
                    }
                }

                if(rowClass == Double.class) {
                    currentEditor = new DefaultCellEditor(new JFormattedTextField(value)) {
                        @Override
                        public Object getCellEditorValue() {
                            return Double.valueOf((String)super.getCellEditorValue());
                        }
                    };
                    registerListeners();
                    return currentEditor.getTableCellEditorComponent(table, value, isSelected, row, column);
                }
                
                if(rowClass == Integer.class) {
                    int id = em.getRowId(row);
                    if(id == PROPERTY_ALIGNMENT || id == PROPERTY_TEXT_POSITION || 
                            id == PROPERTY_VERTICAL_ALIGNMENT || id == PROPERTY_LIST_FIXED ||
                            id == PROPERTY_LIST_ORIENTATION || id == PROPERTY_TAB_PLACEMENT ||
                            id == PROPERTY_TAB_TEXT_POSITION || id == PROPERTY_TEXT_CONSTRAINT) {
                        Integer[] intArr = null;
                        String[] strArr = null;
                        switch(id) {
                            case PROPERTY_LIST_FIXED:
                                intArr = new Integer[] {com.codename1.ui.List.FIXED_NONE,
                                            com.codename1.ui.List.FIXED_NONE_CYCLIC,
                                            com.codename1.ui.List.FIXED_NONE_ONE_ELEMENT_MARGIN_FROM_EDGE,
                                            com.codename1.ui.List.FIXED_LEAD,
                                            com.codename1.ui.List.FIXED_CENTER,
                                            com.codename1.ui.List.FIXED_TRAIL};
                                strArr = new String[] {"None",
                                                "Cyclic",
                                                "One Element From Edge",
                                                "Lead",
                                                "Center",
                                                "Trail"};
                                break;
                            case PROPERTY_LIST_ORIENTATION:
                                intArr = new Integer[] {com.codename1.ui.List.HORIZONTAL,
                                            com.codename1.ui.List.VERTICAL};
                                strArr = new String[] {"Horizontal",
                                                "Vertical"};
                                break;
                            case PROPERTY_TEXT_CONSTRAINT:
                                intArr = TEXT_AREA_CONSTRAINT_KEY_MAP;
                                strArr = TEXT_AREA_CONSTRAINT_NAME_MAP;
                                break;
                            case PROPERTY_ALIGNMENT:
                                intArr = new Integer[] {com.codename1.ui.Label.LEFT, com.codename1.ui.Label.RIGHT, com.codename1.ui.Label.CENTER};
                                strArr = new String[] {"Left", "Right", "Center"};
                                break;
                            case PROPERTY_TAB_PLACEMENT:
                            case PROPERTY_TAB_TEXT_POSITION:
                            case PROPERTY_TEXT_POSITION:
                                intArr = new Integer[] {com.codename1.ui.Label.LEFT, com.codename1.ui.Label.RIGHT,
                                    com.codename1.ui.Label.BOTTOM, com.codename1.ui.Label.TOP};
                                strArr = new String[] {"Left", "Right", "Bottom", "Top"};
                                break;
                            case PROPERTY_VERTICAL_ALIGNMENT:
                                intArr = new Integer[] {com.codename1.ui.Label.TOP, com.codename1.ui.Label.BOTTOM, com.codename1.ui.Label.CENTER};
                                strArr = new String[] {"Top", "Bottom", "Center"};
                                break;
                        }
                        final Integer[] io = intArr;
                        final String[] so = strArr;
                        JComboBox jc = new JComboBox(io);
                        jc.setRenderer(new DefaultListCellRenderer() {
                            @Override
                            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                                if(index < 0 || value == null) {
                                    return super.getListCellRendererComponent(list, "[null]", index, isSelected, cellHasFocus);
                                }
                                return super.getListCellRendererComponent(list, so[index], index, isSelected, cellHasFocus);
                            }
                        });
                        jc.setSelectedItem(value);
                        currentEditor = new DefaultCellEditor(jc);
                        registerListeners();
                        return currentEditor.getTableCellEditorComponent(table, value, isSelected, row, column);
                    }
                    if(value == null) {
                        value = new Integer(0);
                    }
                    currentEditor = new DefaultCellEditor(new JFormattedTextField(value)) {
                        @Override
                        public Object getCellEditorValue() {
                            return Integer.valueOf((String)super.getCellEditorValue());
                        }
                    };
                    registerListeners();
                    return currentEditor.getTableCellEditorComponent(table, value, isSelected, row, column);
                }

                /*if(rowClass == com.codename1.ui.Command[].class) {
                    com.codename1.ui.Command[] commands = (com.codename1.ui.Command[])value;
                    String valueStr = "[";
                    for(int iter = 0 ; iter < commands.length ; iter++) {
                        valueStr += commands[iter].toString();
                        if(iter < commands.length - 1) {
                            valueStr += ", ";
                        }
                    }
                    valueStr += "]";
                    return super.getTableCellRendererComponent(table, valueStr, isSelected, hasFocus, row, column);
                }

                if(rowClass == String[].class) {
                    String[] s = (String[])value;
                    String valueStr = "[";
                    for(int iter = 0 ; iter < s.length ; iter++) {
                        valueStr += s[iter];
                        if(iter < s.length - 1) {
                            valueStr += ", ";
                        }
                    }
                    valueStr += "]";
                    return super.getTableCellRendererComponent(table, valueStr, isSelected, hasFocus, row, column);
                }*/


                return null;
            }

            private void findAllChildren(com.codename1.ui.Container c, Vector result) {
                // skip the root pane
                if(!(c.getParent() instanceof com.codename1.ui.Form) && c.getName() != null) {
                    result.add(c);
                }
                for(int iter = 0 ; iter < c.getComponentCount() ; iter++) {
                    com.codename1.ui.Component current = c.getComponentAt(iter);
                    if(isActualContainer(current)) {
                        findAllChildren((com.codename1.ui.Container)current, result);
                    } else {
                        if(current.getName() != null) {
                            result.add(current);
                        }
                    }
                }
            }

            public Object getCellEditorValue() {
                return currentEditor.getCellEditorValue();
            }

            public boolean isCellEditable(EventObject anEvent) {
                return true;
            }

            public boolean shouldSelectCell(EventObject anEvent) {
                return currentEditor.shouldSelectCell(anEvent);
            }

            public boolean stopCellEditing() {
                return currentEditor.stopCellEditing();
            }

            public void cancelCellEditing() {
                currentEditor.cancelCellEditing();
            }

            public void addCellEditorListener(CellEditorListener l) {
                if(currentEditor != null) {
                    currentEditor.addCellEditorListener(l);
                }
                if(!listeners.contains(l)) {
                    listeners.add(l);
                }
            }

            public void removeCellEditorListener(CellEditorListener l) {
                if(currentEditor != null) {
                    currentEditor.removeCellEditorListener(l);
                }
                listeners.remove(l);
            }
        });
        properties.setDefaultRenderer(String.class, new SwingRenderer() {
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                ComponentPropertyEditorModel m = (ComponentPropertyEditorModel)table.getModel();
                if(m.cmps.length == 1 && isPropertyModified(m.cmps[0], m.getRowId(row))) {
                    setFont(getFont().deriveFont(java.awt.Font.BOLD));
                } else {
                    setFont(getFont().deriveFont(java.awt.Font.PLAIN));
                }
                setToolTipText(null);
                for(String[] entry : PROPERTY_TOOLTIPS) {
                    if(entry[0].equals(value)) {
                        String t = "<html><body><p><b>" + entry[0] + "</b><br>" + entry[1];
                        setToolTipText(t);
                        break;
                    }
                }
                return this;
            }
        });
        properties.setDefaultRenderer(Object.class, new SwingRenderer() {
            private JCheckBox chk = new JCheckBox();
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                if(value == PROPERTIES_DIFFER_IN_VALUE) {
                    setBackground(Color.GRAY);
                    return super.getTableCellRendererComponent(table, "[Differing Values]", isSelected, hasFocus, row, column);
                } else {
                    if(!table.getModel().isCellEditable(row, column)) {
                        setBackground(Color.LIGHT_GRAY);
                    } else {
                        setBackground(Color.WHITE);
                    }
                }

                // find the tooltip
                String name = (String)table.getModel().getValueAt(row, 0);
                setToolTipText(null);
                chk.setToolTipText(null);
                for(String[] entry : PROPERTY_TOOLTIPS) {
                    if(entry[0].equals(name)) {
                        String t = "<html><body><p><b>" + name + "</b><br>" + entry[1];
                        setToolTipText(t);
                        chk.setToolTipText(t);
                        break;
                    }
                }

                if(value == null) {
                    return super.getTableCellRendererComponent(table, "[null]", isSelected, hasFocus, row, column);
                }

                ComponentPropertyEditorModel em = (ComponentPropertyEditorModel)properties.getModel();
                Class rowClass = em.getRowClass(row);
                if(rowClass == String.class) {
                    return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                }
                if(rowClass == Integer.class) {
                    super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    switch(em.getRowId(row)) {
                        case PROPERTY_LIST_FIXED:
                            switch(((Number)value).intValue()) {
                                case com.codename1.ui.List.FIXED_NONE:
                                    setText("None");
                                    return this;
                                case com.codename1.ui.List.FIXED_NONE_CYCLIC:
                                    setText("Cyclic");
                                    return this;
                                case com.codename1.ui.List.FIXED_NONE_ONE_ELEMENT_MARGIN_FROM_EDGE:
                                    setText("One Element From Edge");
                                    return this;
                                case com.codename1.ui.List.FIXED_LEAD:
                                    setText("Lead");
                                    return this;
                                case com.codename1.ui.List.FIXED_CENTER:
                                    setText("Center");
                                    return this;
                                case com.codename1.ui.List.FIXED_TRAIL:
                                    setText("Trail");
                                    return this;
                            }
                            break;
                        case PROPERTY_TEXT_CONSTRAINT:
                            for(int iter = 0 ; iter < TEXT_AREA_CONSTRAINT_KEY_MAP.length ; iter++) {
                                if(((Number)value).intValue() == TEXT_AREA_CONSTRAINT_KEY_MAP[iter].intValue()) {
                                    setText(TEXT_AREA_CONSTRAINT_NAME_MAP[iter]);
                                    break;
                                }
                            }
                            break;
                        case PROPERTY_LIST_ORIENTATION:
                            if(com.codename1.ui.List.HORIZONTAL == ((Number)value).intValue()) {
                                setText("Horizontal");
                            } else {
                                setText("Vertical");
                            }
                            return this;
                        case PROPERTY_ALIGNMENT:
                        case PROPERTY_TEXT_POSITION:
                        case PROPERTY_VERTICAL_ALIGNMENT:
                            switch(((Number)value).intValue()) {
                                case com.codename1.ui.Label.LEFT:
                                    setText("Left");
                                    return this;
                                case com.codename1.ui.Label.RIGHT:
                                    setText("Right");
                                    return this;
                                case com.codename1.ui.Label.TOP:
                                    setText("Top");
                                    return this;
                                case com.codename1.ui.Label.BOTTOM:
                                    setText("Bottom");
                                    return this;
                                case com.codename1.ui.Label.CENTER:
                                    setText("Center");
                                    return this;
                            }
                    }

                    return this;
                }

                if(rowClass == com.codename1.ui.list.ListCellRenderer.class || rowClass == com.codename1.ui.list.CellRenderer.class) {
                    if(value instanceof com.codename1.ui.list.GenericListCellRenderer) {
                        com.codename1.ui.list.GenericListCellRenderer g = (com.codename1.ui.list.GenericListCellRenderer)value;
                        if(g.getSelectedEven() == null) {
                            value = g.getSelected().getName() + ", " + g.getUnselected().getName();
                        } else {
                            value = g.getSelected().getName() + ", " + g.getUnselected().getName() +
                                    ", " + g.getSelectedEven().getName() + ", " +
                                    g.getUnselectedEven().getName();
                        }

                        return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    } else {
                        return super.getTableCellRendererComponent(table, "[Default]", isSelected, hasFocus, row, column);
                    }
                }

                if(rowClass == com.codename1.ui.layouts.Layout.class) {
                    if(value instanceof com.codename1.ui.layouts.BoxLayout) {
                        com.codename1.ui.layouts.BoxLayout b = (com.codename1.ui.layouts.BoxLayout)value;
                        if(b.getAxis() == com.codename1.ui.layouts.BoxLayout.X_AXIS) {
                            value = "BoxLayout X";
                        } else {
                            value = "BoxLayout Y";
                        }
                    } else {
                        value = value.getClass().getSimpleName();
                    }
                    return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                }

                if(rowClass == com.codename1.ui.Image.class) {
                    return super.getTableCellRendererComponent(table, UserInterfaceEditor.this.res.findId(value), isSelected, hasFocus, row, column);
                }

                if(rowClass == com.codename1.ui.Command[].class) {
                    com.codename1.ui.Command[] commands = (com.codename1.ui.Command[])value;
                    String valueStr = "[";
                    for(int iter = 0 ; iter < commands.length ; iter++) {
                        valueStr += commands[iter].toString();
                        if(iter < commands.length - 1) {
                            valueStr += ", ";
                        }
                    }
                    valueStr += "]";
                    return super.getTableCellRendererComponent(table, valueStr, isSelected, hasFocus, row, column);
                }

                if(rowClass == String[].class || rowClass == Object[].class || rowClass == com.codename1.ui.Image[].class) {
                    Object[] s = (Object[])value;
                    String valueStr = "[";
                    for(int iter = 0 ; iter < s.length ; iter++) {
                        if(s[iter] instanceof com.codename1.ui.Image) {
                            valueStr += UserInterfaceEditor.this.res.findId(s[iter]);
                        } else {
                            valueStr += s[iter];
                        }
                        if(iter < s.length - 1) {
                            valueStr += ", ";
                        }
                    }
                    valueStr += "]";
                    return super.getTableCellRendererComponent(table, valueStr, isSelected, hasFocus, row, column);
                }
                if(rowClass == String[][].class) {
                    String[] s = ((String[][])value)[0];
                    String valueStr = "[";
                    for(int iter = 0 ; iter < s.length ; iter++) {
                        valueStr += s[iter];
                        if(iter < s.length - 1) {
                            valueStr += ", ";
                        }
                    }
                    valueStr += "]";
                    return super.getTableCellRendererComponent(table, valueStr, isSelected, hasFocus, row, column);
                }

                if(rowClass == com.codename1.ui.Component.class) {
                    com.codename1.ui.Component val = (com.codename1.ui.Component)value;
                    if(val == null) {
                        value = "[null]";
                    } else {
                        value = val.getName();
                    }
                    return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                }

                if(rowClass.isArray()) {
                    return super.getTableCellRendererComponent(table, Arrays.toString((Object[])value), isSelected, hasFocus, row, column);
                }

                if(rowClass == Boolean.class) {
                    chk.setSelected(((Boolean)value).booleanValue());
                    updateComponentSelectedState(chk, isSelected, table, row, column, hasFocus);
                    return chk;
                }

                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }

        });

        TransferHandler h = new TransferHandler() {
            @Override
            public boolean importData(TransferSupport support) {
                try {
                    String tabTitle = "tab";
                    TreePath path = ((JTree.DropLocation)support.getDropLocation()).getPath();
                    if(path == null) {
                        return false;
                    }
                    com.codename1.ui.Component assumedParent = (com.codename1.ui.Component)path.getLastPathComponent();
                    com.codename1.ui.Container parent;
                    int index = ((JTree.DropLocation)support.getDropLocation()).getChildIndex();
                    boolean addedOrExisting = true;
                    if(isActualContainer(assumedParent)) {
                        parent = (com.codename1.ui.Container)assumedParent;
                        if(index < 0) {
                            index = 0;
                        }
                    } else {
                        index = assumedParent.getParent().getComponentIndex(assumedParent);
                        parent = assumedParent.getParent();
                    }
                    com.codename1.ui.Component cmp = (com.codename1.ui.Component)support.getTransferable().getTransferData(CODENAMEONE_COMPONENT_FLAVOR);
                    
                    if(cmp instanceof com.codename1.ui.Form) {
                        return false;
                    }
                    
                    if(parent != null) {
                        com.codename1.ui.Component tmp = parent.getParent();
                        while(tmp != null) {
                            if(tmp == cmp) {
                                return false;
                            }
                            tmp = tmp.getParent();
                        }
                    }
                    Object constraint = null;
                    com.codename1.ui.Container contentPane = parent;
                    if(parent instanceof com.codename1.ui.Form) {
                        contentPane = ((com.codename1.ui.Form)parent).getContentPane();
                    }
                    if(cmp.getParent() != null) {
                        // if we are dragging the component downwards the index of the component will change...
                        if(contentPane == cmp.getParent()) {
                            if(contentPane.getComponentIndex(cmp) < index) {
                                index--;
                            }
                        }
                        constraint = contentPane.getLayout().getComponentConstraint(cmp);
                        if(cmp.getParent() != null && cmp.getParent().getParent() instanceof com.codename1.ui.Tabs) {
                            int tabOldIndex = cmp.getParent().getComponentIndex(cmp);
                            tabTitle = ((com.codename1.ui.Tabs)cmp.getParent().getParent()).getTabTitle(tabOldIndex);
                            ((com.codename1.ui.Tabs)cmp.getParent().getParent()).removeTabAt(tabOldIndex);
                        } else {
                            removeComponentSync(cmp.getParent(), cmp);
                        }

                        if(constraint == null && parent.getLayout() instanceof com.codename1.ui.layouts.BorderLayout) {
                            constraint = findAvailableSpotInBorderLayout(contentPane);
                        }
                    } else {
                        addedOrExisting = false;
                        if(contentPane.getClass() == com.codename1.ui.Container.class && contentPane.getLayout() instanceof com.codename1.ui.layouts.BorderLayout) {
                            if(contentPane.getComponentCount() > 4) {
                                return false;
                            }
                            constraint = findAvailableSpotInBorderLayout(contentPane);
                        }
                    }

                    if(parent instanceof com.codename1.ui.Tabs) {
                        if(index > -1 && addedOrExisting) {
                            ((com.codename1.ui.Tabs)parent).insertTab(tabTitle, null, cmp, index);
                        } else {
                            ((com.codename1.ui.Tabs)parent).addTab("Tab", cmp);
                        }
                    } else {
                        if(constraint != null) {
                            parent.addComponent(index, constraint, cmp);
                        } else {
                            parent.addComponent(index, cmp);
                        }
                    }

                    // we need to re-add all the components to the table layout since order only
                    // affects the layout initially
                    reflowTableLayout(parent);

                    containerInstance.revalidate();
                    parent.revalidate();
                    ((ComponentHierarchyModel)componentHierarchy.getModel()).fireTreeStructureChanged(
                            new TreeModelEvent(componentHierarchy.getModel(),
                                new TreePath(componentHierarchy.getModel().getRoot())));
                    uiPreview.repaint();
                    expandAll(componentHierarchy);
                    saveUI();

                    return true;
                } catch(Exception e) {
                    e.printStackTrace();
                }
                return false;
            }

            @Override
            protected Transferable createTransferable(JComponent c) {
                Object ts;
                if(c == componentHierarchy) {
                    ts = componentHierarchy.getSelectionPath().getLastPathComponent();
                } else {
                    // JButton from the palette
                    ts = new com.codename1.ui.Button("Button");
                    findName("Button", (com.codename1.ui.Component)ts);
                }
                final Object o = ts;
                return new CodenameOneComponentTransferable((com.codename1.ui.Component)o);
            }

            @Override
            public int getSourceActions(JComponent c) {
                if(c instanceof JButton) {
                    return COPY;
                }
                return MOVE;
            }

            @Override
            public Icon getVisualRepresentation(Transferable t) {
                try {
                    com.codename1.ui.Component cmp = (com.codename1.ui.Component)t.getTransferData(CODENAMEONE_COMPONENT_FLAVOR);
                    if(cmp.getWidth() <= 0 || cmp.getHeight() <= 0) {
                        cmp.setWidth(cmp.getPreferredW());
                        cmp.setHeight(cmp.getPreferredH());
                    }
                    com.codename1.ui.Image img = com.codename1.ui.Image.createImage(cmp.getWidth(), cmp.getHeight());
                    cmp.paintComponent(img.getGraphics());
                    return new CodenameOneImageIcon(img);
                } catch(Exception e) {
                    e.printStackTrace();
                }
                return super.getVisualRepresentation(t);
            }

            public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
                for(DataFlavor f : transferFlavors) {
                    if(f == CODENAMEONE_COMPONENT_FLAVOR) {
                        return true;
                    }
                }
                return false;
            }
        };
        componentHierarchy.setTransferHandler(h);
        makeDraggable(codenameOneButton, com.codename1.ui.Button.class, "Button", null);
        makeDraggable(codenameOneCheckBox, com.codename1.ui.CheckBox.class, "CheckBox", null);
        makeDraggable(codenameOneComboBox, com.codename1.ui.ComboBox.class, "ComboBox", null);
        makeDraggable(codenameOneContainer, com.codename1.ui.Container.class, "Container", null);
        makeDraggable(codenameOneLabel, com.codename1.ui.Label.class, "Label", null);
        makeDraggable(codenameOneList, com.codename1.ui.List.class, "List", null);
        makeDraggable(codenameOneRadioButton, com.codename1.ui.RadioButton.class, "RadioButton", null);
        makeDraggable(codenameOneSlider, com.codename1.ui.Slider.class, "Slider", null);
        makeDraggable(codenameOneTextArea, com.codename1.ui.TextArea.class, "TextArea", null);
        makeDraggable(codenameOneTextField, com.codename1.ui.TextField.class, "TextField", null);
        makeDraggable(codenameOneTabs, com.codename1.ui.Tabs.class, "Tabs", null);
        makeDraggable(codenameOneTable, com.codename1.ui.table.Table.class, "Table", null);
        makeDraggable(codenameOneMediaPlayer, com.codename1.components.MediaPlayer.class, "MediaPlayer", null);
        makeDraggable(codenameOneContainerList, com.codename1.ui.list.ContainerList.class, "ContainerList", null);
        makeDraggable(codenameOneComponentGroup, com.codename1.ui.ComponentGroup.class, "ComponentGroup", null);
        makeDraggable(codenameOneTree, com.codename1.ui.tree.Tree.class, "Tree", null);
        makeDraggable(codenameOneHTMLComponent, com.codename1.components.WebBrowser.class, "WebBrowser", null);
        makeDraggable(rssReader, com.codename1.components.RSSReader.class, "RSSReader", null);
        makeDraggable(fileTree, com.codename1.components.FileTree.class, "FileTree", null);
        makeDraggable(embedContainer, EmbeddedContainer.class, "EmbeddedContainer", null);
        makeDraggable(codenameOneNumericSpinner, com.codename1.ui.spinner.NumericSpinner.class, "NumericSpinner", null);
        makeDraggable(codenameOneDateSpinner, com.codename1.ui.spinner.DateSpinner.class, "DateSpinner", null);
        makeDraggable(codenameOneTimeSpinner, com.codename1.ui.spinner.TimeSpinner.class, "TimeSpinner", null);
        makeDraggable(codenameOneDateTimeSpinner, com.codename1.ui.spinner.DateTimeSpinner.class, "DateTimeSpinner", null);
        makeDraggable(codenameOneGenericSpinner, com.codename1.ui.spinner.GenericSpinner.class, "GenericSpinner", null);
        makeDraggable(codenameOneLikeButton, com.codename1.facebook.ui.LikeButton.class, "LikeButton", null);
        makeDraggable(codenameOneInfiniteProgress, com.codename1.components.InfiniteProgress.class, "InfiniteProgress", null);
        makeDraggable(codenameOneMultiButton, com.codename1.components.MultiButton.class, "MultiButton", null);
        makeDraggable(codenameOneSpanButton, com.codename1.components.SpanButton.class, "SpanButton", null);
        makeDraggable(codenameOneSpanLabel, com.codename1.components.SpanLabel.class, "SpanLabel", null);
        makeDraggable(codenameOneAds, com.codename1.components.Ads.class, "Ads", null);
        makeDraggable(codenameOneMap, com.codename1.maps.MapComponent.class, "MapComponent", null);
        makeDraggable(codenameOneMultiList, com.codename1.ui.list.MultiList.class, "MultiList", null);
        makeDraggable(codenameOneShare, com.codename1.components.ShareButton.class, "ShareButton", null);
        makeDraggable(codenameOneCalendar, com.codename1.ui.Calendar.class, "Calendar", null);
        makeDraggable(codenameOneOnOffSwitch, com.codename1.components.OnOffSwitch.class, "OnOffSwitch", null);
        makeDraggable(codenameOneImageViewer, com.codename1.components.ImageViewer.class, "ImageViewer", null);
        makeDraggable(codenameOneAutoCompleteTextField, com.codename1.ui.AutoCompleteTextField.class, "AutoCompleteTextField", null);

        if(customComponents != null) {
            for(CustomComponent currentCmp : customComponents) {
                createCustomComponentButton(currentCmp);
            }
        }

        // add a custom component for every GUI element that is not a form
        UIBuilderOverride.setIgnorBaseForm(false);
        UIBuilderOverride tempBuilder = new UIBuilderOverride(null);
        for(String uiResourceName : res.getUIResourceNames()) {
            if(uiResourceName.equals(name)) {
                continue;
            }
            if(!(tempBuilder.createContainer(res, uiResourceName) instanceof com.codename1.ui.Form)) {
                createCustomComponentButton(new CustomComponent(true, uiResourceName));
            }
        }
        tempBuilder = null;
        UIBuilderOverride.setIgnorBaseForm(true);
        
        for(int iter = 0 ; iter < coreComponents.getComponentCount() ; iter++) {
            JComponent c = (JComponent)coreComponents.getComponent(iter);
            c.setMaximumSize(c.getPreferredSize());
        }
        coreComponents.setMaximumSize(new java.awt.Dimension(coreComponents.getMaximumSize().width, 
                coreComponents.getPreferredSize().height));
        for(int iter = 0 ; iter < codenameOneExtraComponents.getComponentCount() ; iter++) {
            JComponent c = (JComponent)codenameOneExtraComponents.getComponent(iter);
            c.setMaximumSize(c.getPreferredSize());
        }
        codenameOneExtraComponents.setMaximumSize(new java.awt.Dimension(codenameOneExtraComponents.getMaximumSize().width,
                codenameOneExtraComponents.getPreferredSize().height));
        for(int iter = 0 ; iter < codenameOneIOComponents.getComponentCount() ; iter++) {
            JComponent c = (JComponent)codenameOneIOComponents.getComponent(iter);
            c.setMaximumSize(c.getPreferredSize());
        }
        codenameOneIOComponents.setMaximumSize(new java.awt.Dimension(codenameOneIOComponents.getMaximumSize().width,
                codenameOneIOComponents.getPreferredSize().height));
        for(int iter = 0 ; iter < userComponents.getComponentCount() ; iter++) {
            JComponent c = (JComponent)userComponents.getComponent(iter);
            c.setMaximumSize(c.getPreferredSize());
        }
        userComponents.setMaximumSize(new java.awt.Dimension(userComponents.getMaximumSize().width,
                userComponents.getPreferredSize().height));

        if(res.getResourceObject(name) == null) {
            res.setUi(name, persistContainer(containerInstance));
        }

        repainter = new javax.swing.Timer(1500, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if(SwingUtilities.windowForComponent(UserInterfaceEditor.this) == null) {
                    repainter.stop();
                    return;
                }
                uiPreview.repaint();
            }
        });
        repainter.setRepeats(true);
        repainter.start();
    }

    void deleteSelection() {
        TreePath[] sels = componentHierarchy.getSelectionPaths();
        if(sels != null) {
            componentHierarchy.setSelectionPath(new TreePath(containerInstance));
            for(TreePath sel : sels) {
                com.codename1.ui.Component cmp = (com.codename1.ui.Component)sel.getLastPathComponent();
                if(cmp == containerInstance) {
                    continue;
                }
                ((ComponentPropertyEditorModel)properties.getModel()).setComponents(new com.codename1.ui.Component[] {cmp.getParent()});
                if(cmp != null) {
                    if(cmp.getParent().getUIID().equals("TabbedPane")) {
                        ((com.codename1.ui.Tabs)cmp.getParent().getParent()).removeTabAt(cmp.getParent().getComponentIndex(cmp));
                    } else {
                        removeComponentSync(cmp.getParent(), cmp);
                    }
                    containerInstance.revalidate();
                    uiPreview.repaint();
                    ((ComponentHierarchyModel)componentHierarchy.getModel()).fireTreeStructureChanged(new TreeModelEvent(componentHierarchy.getModel(), new TreePath(componentHierarchy.getModel().getRoot())));
                    expandAll(componentHierarchy);
                    saveUI();
                }
            }
        }
    }

    private void saveUI() {
        try {
            res.setUi(UserInterfaceEditor.this.name, persistContainer(containerInstance));
            if(builder.createContainer(res, name) == null) {
                JOptionPane.showMessageDialog(this, "GUI Builder Error, undoing...");
                res.undo();
            }
        } catch(Throwable t) {
            JOptionPane.showMessageDialog(this, "GUI Builder Error, undoing...");
            res.undo();
            t.printStackTrace();
        }
    }

    private void removeComponentSync(final com.codename1.ui.Container cnt, final com.codename1.ui.Component cmp) {
        if(com.codename1.ui.Display.getInstance().isEdt()) {
            cnt.removeComponent(cmp);
            return;
        }
        com.codename1.ui.Display.getInstance().callSeriallyAndWait(new Runnable() {
            public void run() {
                cnt.removeComponent(cmp);
            }
        });
    }

    /**
     * Creates a copy of the given component and its hierarchy
     */
    private com.codename1.ui.Component copyComponent(com.codename1.ui.Component cmp) {
        com.codename1.ui.Container cnt = builder.createContainer(res, copiedResourceName);
        com.codename1.ui.Component dest = builder.findByName(cmp.getName(), cnt);
        if(dest != null && dest.getParent() != null) {
            removeComponentSync(dest.getParent(), dest);
        }
        return dest;
    }

    private void initializeComponentText(com.codename1.ui.Component cmp) {
        if(cmp instanceof com.codename1.ui.Label) {
            ((com.codename1.ui.Label)cmp).setText(cmp.getName());
            setPropertyModified(cmp, PROPERTY_TEXT);
        } else {
            if(cmp instanceof com.codename1.ui.TextArea) {
                ((com.codename1.ui.TextArea)cmp).setText(cmp.getName());
                setPropertyModified(cmp, PROPERTY_TEXT);
            } else {
                if(cmp instanceof com.codename1.ui.List && !(cmp instanceof com.codename1.ui.list.MultiList)) {
                    if(!(cmp instanceof com.codename1.components.RSSReader)) {
                        ((com.codename1.ui.List)cmp).setModel(new com.codename1.ui.list.DefaultListModel(
                                new Object[] {"Item 1", "Item 2", "Item 3"}));
                        setPropertyModified(cmp, PROPERTY_LIST_ITEMS);
                    }
                } else {
                    if(cmp instanceof com.codename1.components.WebBrowser) {
                        ((com.codename1.components.WebBrowser)cmp).setPage("<html><head><title>Web Component</title></head><body>To use the HTML component you need to set a page or assign a jar/res URL in the code</body></html>", null);
                        setCustomPropertyModified(cmp, "body");
                    }
                }
            }
        }
    }

    private void expandAll(JTree tree) {
        int row = 0;
        while (row < tree.getRowCount()) {
            tree.expandRow(row);
            row++;
        }
    }

    private void makeDraggable(final JButton b, final Class codenameOneClass, final String namePrefix, final CustomComponent custom) {
        DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(b,
                TransferHandler.MOVE, new DragGestureListener() {
            public void dragGestureRecognized(DragGestureEvent dge) {
                lockForDragging = true;
                try {
                    com.codename1.ui.Component cmp;
                    if(custom != null && custom.isUiResource()) {
                        UIBuilderOverride u = new UIBuilderOverride(UserInterfaceEditor.this);
                        cmp = u.createContainer(res, custom.getType());
                        if(b.getIcon() != null) {
                            DragSource.getDefaultDragSource().startDrag(dge, DragSource.DefaultMoveDrop,
                                    ((ImageIcon)b.getIcon()).getImage(), new Point(0, 0),
                                    new CodenameOneComponentTransferable(cmp), new DragSourceAdapter() {
                                });
                        } else {
                            DragSource.getDefaultDragSource().startDrag(dge, DragSource.DefaultMoveDrop,
                                new CodenameOneComponentTransferable(cmp), new DragSourceAdapter() {
                                });
                        }
                        return;
                    }
                    if(codenameOneClass == com.codename1.ui.Component.class) {
                        // special case for custom component which has a protected constructor
                        cmp = new com.codename1.ui.Component() {};
                    } else {
                        cmp = (com.codename1.ui.Component)codenameOneClass.newInstance();
                    }
                    cmp.putClientProperty(TYPE_KEY, namePrefix);
                    cmp.setName(findUniqueName(namePrefix));
                    initializeComponentText(cmp);
                    if(custom != null) {
                        cmp.putClientProperty("CustomComponent", custom);
                        cmp.putClientProperty(TYPE_KEY, custom.getType());
                    }
                    DragSource.getDefaultDragSource().startDrag(dge, DragSource.DefaultMoveDrop,
                            new CodenameOneComponentTransferable(cmp), new DragSourceAdapter() {
                            });
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private com.codename1.ui.Component[] getSelectedComponents() {
        return ((ComponentPropertyEditorModel)properties.getModel()).cmps;
    }

    private List<com.codename1.ui.Command> getListOfAllCommands() {
        return getListOfAllCommands(res);
    }

    private static List<com.codename1.ui.Command> getListOfAllCommands(EditableResources res) {
        final List<com.codename1.ui.Command> response = new ArrayList<com.codename1.ui.Command>();
        UIBuilderOverride tempBuilder = new UIBuilderOverride(null) {
            public com.codename1.ui.Command createCommandImpl(String commandName, com.codename1.ui.Image icon, int commandId, String action, boolean isBack, String arg) {
                com.codename1.ui.Command c = super.createCommandImpl(commandName, icon, commandId, action, isBack, arg);
                if(!response.contains(c)) {
                    response.add(c);
                }
                return c;
            }
        };
        for(String uiResourceName : res.getUIResourceNames()) {
            tempBuilder.createContainer(res, uiResourceName);
        }
        Collections.sort(response, new Comparator<com.codename1.ui.Command>() {
            public int compare(com.codename1.ui.Command o1, com.codename1.ui.Command o2) {
                if(o1 == null) {
                    if(o2 == null) {
                        return 0;
                    }
                    return -1;
                }
                if(o2 == null) {
                    return 1;
                }
                return String.CASE_INSENSITIVE_ORDER.compare(o1.getCommandName(), o2.getCommandName());
            }
        });
        return response;
    }

    class CommandArrayEditor extends AbstractCellEditor implements TableCellEditor, ActionListener {
        private JButton button;
        private ArrayEditorDialog ed;
        private Object[] currentValue;
        private boolean command;
        private boolean array2D;
        private boolean objectArray;

        public CommandArrayEditor(boolean command, boolean array2D, boolean objectArray) {
            this(command, array2D);
            this.objectArray = objectArray;
        }
        
        public CommandArrayEditor(boolean command, boolean array2D) {
            this.command = command;
            this.array2D = array2D;
            button = new JButton("...");
            button.addActionListener(this);
            button.setBorderPainted(false);
        }

        public void actionPerformed(ActionEvent e) {
            if(command) {
                ed = new ArrayEditorDialog(UserInterfaceEditor.this, res, currentValue, "Commands", "/help/commands.html") {
                    protected Object edit(Object o) {
                        ActionCommand cmd = (ActionCommand)o;
                        if(cmd == null) {
                            cmd = new ActionCommand("", null, ActionCommand.getCommandUniqueId(), null, false, "");
                        }
                        CommandEditor cmdEdit = new CommandEditor(cmd, res, UserInterfaceEditor.this.name, getListOfAllCommands(), projectGeneratorSettings, getLanguageLevel() > 4);
                        if(showEditDialog(cmdEdit)) {
                            return cmdEdit.getResult();
                        }
                        return o;
                    }
                };
            } else {
                if(array2D) {
                    ed = new ArrayEditorDialog(UserInterfaceEditor.this, res, currentValue, "2D Array Of Strings", "/help/2dArrayOfStrings.html") {
                        protected Object edit(Object o) {
                            String[] arr = (String[])o;
                            if(arr == null) {
                                arr = new String[0];
                            }
                            ArrayEditorDialog arrEdit = new ArrayEditorDialog(this, res, arr, "String", "/help/ArrayOfStrings.html");
                            if(arrEdit.isOK()) {
                                List l = arrEdit.getResult();
                                String[] res = new String[l.size()];
                                l.toArray(res);
                                return res;
                            }
                            return o;
                        }
                    };
                } else {
                    if(objectArray) {
                        ed = new ArrayEditorDialog(UserInterfaceEditor.this, res, currentValue, "Hashtable", "/help/Hashtable.html") {
                            protected Object edit(Object o) {
                                HashtableEditor h = new HashtableEditor(res, o, getSelectedComponents()[0]);
                                if(showEditDialog(h)) {
                                    return h.getResult();
                                }
                                return o;
                            }
                        };
                    } else {
                        ed = new ArrayEditorDialog(UserInterfaceEditor.this, res, currentValue, "Strings", "/help/ArrayOfStrings.html");
                    }
                }
            }
            if(ed.isOK()) {
                List result = ed.getResult();
                if(command) {
                    currentValue = new ActionCommand[result.size()];
                } else {
                    if(array2D) {
                        currentValue = new String[result.size()][];
                    } else {
                        if(objectArray) {
                            currentValue = new Object[result.size()];
                        } else {
                            currentValue = new String[result.size()];
                        }
                    }
                }
                result.toArray(currentValue);
            } else {
                fireEditingCanceled();
                return;
            }
            fireEditingStopped();
        }

        public Object getCellEditorValue() {
            return currentValue;
        }

        public Component getTableCellEditorComponent(JTable table,
                                                     Object value,
                                                     boolean isSelected,
                                                     int row,
                                                     int column) {
            currentValue = (Object[])value;
            return button;
        }
    }

    class ImageArrayEditor extends AbstractCellEditor implements TableCellEditor, ActionListener {
        private JButton button;
        private ArrayEditor ed;
        private Object[] currentValue;

        public ImageArrayEditor() {
            button = new JButton("...");
            button.addActionListener(this);
            button.setBorderPainted(false);
        }

        public void actionPerformed(ActionEvent e) {
            ed = new ArrayEditor(res, currentValue) {
                protected Object edit(Object o) {
                    JComboBox cb = new JComboBox();
                    ResourceEditorView.initImagesComboBox(cb, res, false, true);
                    if(o != null) {
                        if(o instanceof com.codename1.ui.Image) {
                            cb.setSelectedItem(o);
                        } else {
                            cb.setSelectedItem(res.getImage((String)o));
                        }
                    }
                    if(JOptionPane.showConfirmDialog(this, cb, "Select", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
                        return cb.getSelectedItem();
                    } else {
                        return o;
                    }
                }
            };
            if(JOptionPane.showConfirmDialog(UserInterfaceEditor.this, ed, "Edit", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
                List result = ed.getResult();
                currentValue = new com.codename1.ui.Image[result.size()];
                result.toArray(currentValue);
                fireEditingStopped();
                return;
            }
            fireEditingCanceled();
        }

        public Object getCellEditorValue() {
            return currentValue;
        }

        public Component getTableCellEditorComponent(JTable table,
                                                     Object value,
                                                     boolean isSelected,
                                                     int row,
                                                     int column) {
            currentValue = (Object[])value;
            return button;
        }
    }


    class TableConstraintEditor extends AbstractCellEditor implements TableCellEditor, ActionListener {
        private JButton button;
        private TableLayoutConstraintEditor ed;
        private Object currentValue;

        public TableConstraintEditor() {
            button = new JButton("...");
            button.addActionListener(this);
            button.setBorderPainted(false);
        }

        public void actionPerformed(ActionEvent e) {
            com.codename1.ui.Container cnt = ((com.codename1.ui.Component)getSelectedComponents()[0]).getParent();
            ed = new TableLayoutConstraintEditor((com.codename1.ui.table.TableLayout)cnt.getLayout(),
                    (com.codename1.ui.table.TableLayout.Constraint)currentValue);
            if(JOptionPane.showConfirmDialog(UserInterfaceEditor.this, ed, "Edit", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE)
                    != JOptionPane.OK_OPTION) {
                fireEditingCanceled();
                return;
            }
            currentValue = ed.getResult();
            try {
                fireEditingStopped();
            } catch(Exception err) {
                err.printStackTrace();
                JOptionPane.showMessageDialog(UserInterfaceEditor.this, "The constraint has failed on the table: " + err.toString(), "Error", JOptionPane.ERROR_MESSAGE);
                actionPerformed(e);
            }
        }

        public Object getCellEditorValue() {
            return currentValue;
        }

        public Component getTableCellEditorComponent(JTable table,
                                                     Object value,
                                                     boolean isSelected,
                                                     int row,
                                                     int column) {
            currentValue = value;
            return button;
        }
    }

    class ListRendererTableEditor extends AbstractCellEditor implements TableCellEditor, ActionListener {
        private JButton button;
        private ListRendererEditor ed;
        private Object currentValue;

        public ListRendererTableEditor() {
            button = new JButton("...");
            button.addActionListener(this);
            button.setBorderPainted(false);
        }

        public void actionPerformed(ActionEvent e) {
            com.codename1.ui.Component list = getSelectedComponents()[0];
            ed = new ListRendererEditor(res, list, UserInterfaceEditor.this.name);
            if(JOptionPane.showConfirmDialog(UserInterfaceEditor.this, ed, "Edit", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE)
                    != JOptionPane.OK_OPTION) {
                fireEditingCanceled();
                return;
            }
            currentValue = ed.getResult();
            fireEditingStopped();
        }

        public Object getCellEditorValue() {
            return currentValue;
        }

        public Component getTableCellEditorComponent(JTable table,
                                                     Object value,
                                                     boolean isSelected,
                                                     int row,
                                                     int column) {
            currentValue = value;
            return button;
        }
    }

    class DateEditor extends AbstractCellEditor implements TableCellEditor, ActionListener {
        private JButton button;
        private Object currentValue;

        public DateEditor() {
            button = new JButton("...");
            button.addActionListener(this);
            button.setBorderPainted(false);
        }

        public void actionPerformed(ActionEvent e) {
            javax.swing.JPanel grid = new javax.swing.JPanel(new java.awt.GridLayout(3, 2));
            grid.add(new JLabel("Day"));
            Object[] days = new Object[31];
            for(int iter = 0 ; iter < 31 ; iter++) {
                days[iter] = new Integer(iter + 1);
            }
            JComboBox day = new JComboBox(days);
            grid.add(day);
            grid.add(new JLabel("Month"));
            Object[] months = new Object[31];
            for(int iter = 0 ; iter < 12 ; iter++) {
                months[iter] = new Integer(iter + 1);
            }
            JComboBox month = new JComboBox(months);
            grid.add(month);
            grid.add(new JLabel("Year"));
            Object[] years = new Object[5000];
            for(int iter = 0 ; iter < 5000 ; iter++) {
                years[iter] = new Integer(iter + 1);
            }
            JComboBox year = new JComboBox(years);
            grid.add(year);
            
            Date currentDate = (Date)currentValue;
            if(currentDate == null) {
                currentDate = new Date();
            }
            day.setSelectedItem(new Integer(currentDate.getDate()));
            month.setSelectedItem(new Integer(currentDate.getMonth() + 1));
            year.setSelectedItem(new Integer(currentDate.getYear() + 1900));
            
            if(JOptionPane.showConfirmDialog(UserInterfaceEditor.this, grid, "Edit", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE)
                    != JOptionPane.OK_OPTION) {
                fireEditingCanceled();
                return;
            }
            currentValue = new Date(((Number)year.getSelectedItem()).intValue(), ((Number)month.getSelectedItem()).intValue(),
                    ((Number)day.getSelectedItem()).intValue());
            fireEditingStopped();
        }

        public Object getCellEditorValue() {
            return currentValue;
        }

        public Component getTableCellEditorComponent(JTable table,
                                                     Object value,
                                                     boolean isSelected,
                                                     int row,
                                                     int column) {
            currentValue = value;
            return button;
        }
    }

    class CommandTableEditor extends AbstractCellEditor implements TableCellEditor, ActionListener {
        private JButton button;
        private CommandEditor ed;
        private Object currentValue;

        public CommandTableEditor() {
            button = new JButton("...");
            button.addActionListener(this);
            button.setBorderPainted(false);
        }

        public void actionPerformed(ActionEvent e) {
            com.codename1.ui.Component selectedCMP = getSelectedComponents()[0];
            if(selectedCMP instanceof com.codename1.ui.Container) {
                selectedCMP = ((com.codename1.ui.Container)selectedCMP).getLeadComponent();
            }
            com.codename1.ui.Button b = (com.codename1.ui.Button)selectedCMP;
            ActionCommand a = (ActionCommand)b.getCommand();
            if(a == null) {
                a = new ActionCommand(b.getText(), b.getIcon(), ActionCommand.getCommandUniqueId(), null, false, "");
            }
            ed = new CommandEditor(a, res, UserInterfaceEditor.this.name, getListOfAllCommands(), projectGeneratorSettings, getLanguageLevel() > 4);
            if(JOptionPane.showConfirmDialog(UserInterfaceEditor.this, ed, "Edit", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE)
                    != JOptionPane.OK_OPTION) {
                fireEditingCanceled();
                return;
            }
            currentValue = ed.getResult();
            if(((ActionCommand)currentValue).isBackCommand()) {
                b.getComponentForm().setBackCommand(((ActionCommand)currentValue));
            }
            fireEditingStopped();
        }

        public Object getCellEditorValue() {
            return currentValue;
        }

        public Component getTableCellEditorComponent(JTable table,
                                                     Object value,
                                                     boolean isSelected,
                                                     int row,
                                                     int column) {
            currentValue = value;
            return button;
        }
    }

    class HTMLBodyEditor extends AbstractCellEditor implements TableCellEditor, ActionListener {
        private JButton button;
        private HTMLEditor ed;
        private String currentValue;

        public HTMLBodyEditor() {
            button = new JButton("...");
            button.addActionListener(this);
            button.setBorderPainted(false);
        }

        public void actionPerformed(ActionEvent e) {
            ed = new HTMLEditor(res, currentValue);
            ed.setPreferredSize(new Dimension(700, 430));
            if(JOptionPane.showConfirmDialog(UserInterfaceEditor.this, ed, "Edit", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE)
                    != JOptionPane.OK_OPTION) {
                fireEditingCanceled();
                return;
            }
            currentValue = ed.getResult();
            fireEditingStopped();
        }

        public Object getCellEditorValue() {
            return currentValue;
        }

        public Component getTableCellEditorComponent(JTable table,
                                                     Object value,
                                                     boolean isSelected,
                                                     int row,
                                                     int column) {
            currentValue = (String)value;
            return button;
        }
    }

    class LayoutManagerEditor extends AbstractCellEditor implements TableCellEditor, ActionListener {
        private JButton button;
        private LayoutEditor ed;
        private Object currentValue;

        public LayoutManagerEditor() {
            button = new JButton("...");
            button.addActionListener(this);
            button.setBorderPainted(false);
        }

        public void actionPerformed(ActionEvent e) {
            com.codename1.ui.Container cnt = (com.codename1.ui.Container)getSelectedComponents()[0];
            ed = new LayoutEditor(cnt);
            if(JOptionPane.showConfirmDialog(UserInterfaceEditor.this, ed, "Edit", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE)
                    != JOptionPane.OK_OPTION) {
                fireEditingCanceled();
                return;
            }
            currentValue = ed.getResult();
            fireEditingStopped();
        }

        public Object getCellEditorValue() {
            return currentValue;
        }

        public Component getTableCellEditorComponent(JTable table,
                                                     Object value,
                                                     boolean isSelected,
                                                     int row,
                                                     int column) {
            currentValue = value;
            return button;
        }
    }

    public void addCustomComponent(String componentType, String className, String codenameOneType) {
        /*CustomComponent c = new CustomComponent();
        c.setType(componentType);
        c.setClassName(className);
        c.setCodenameOneBaseClass(codenameOneType);
        customComponents.add(c);
        createCustomComponentButton(c);*/
    }

    private void createCustomComponentButton(final CustomComponent c) {
        try {
            final JButton b = new JButton(c.getType());
            b.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jdesktop/swingx/resources/placeholder32.png"))); 
            b.setHorizontalAlignment(SwingConstants.LEFT);
            b.setBorder(null);
            userComponents.add(b);
            b.putClientProperty("CustomComponent", c);
            final Class codenameOneBaseClass = c.getCls();
            makeDraggable(b, codenameOneBaseClass, c.getType(), c);
            b.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if(lockForDragging) {
                        lockForDragging = false;
                        return;
                    }
                    try {
                        if(c.isUiResource()) {
                            UIBuilderOverride u = new UIBuilderOverride(UserInterfaceEditor.this);
                            com.codename1.ui.Component cmp = u.createContainer(res, c.getType());
                            String t = (String)cmp.getClientProperty(TYPE_KEY);
                            if(t == null) {
                                cmp.putClientProperty(TYPE_KEY, c.getType());
                                t = c.getType();
                            }
                            addComponentToContainer(cmp, t);
                            return;
                        }
                        com.codename1.ui.Component cmp = (com.codename1.ui.Component)codenameOneBaseClass.newInstance();
                        cmp.putClientProperty("CustomComponent", c);
                        cmp.putClientProperty(TYPE_KEY, c.getType());
                        initializeComponentText(cmp);
                        addComponentToContainer(cmp, c.getType());
                    } catch(Exception err) {
                        err.printStackTrace();
                        JOptionPane.showMessageDialog(UserInterfaceEditor.this, err.getClass().getName() + ": " + err, "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
            /*b.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if(BaseForm.isRightClick(e)) {
                        JPopupMenu p = new JPopupMenu();
                        AbstractAction deleteAction = new AbstractAction("Delete") {
                            public void actionPerformed(ActionEvent e) {
                                componentPalette.remove(b);
                                componentPalette.revalidate();
                                customComponents.remove(c);
                                res.setUi(name, persistContainer(containerInstance));
                            }
                        };
                        p.add(deleteAction);
                        p.show(b, e.getPoint().x, e.getPoint().y);
                    }
                }
            });*/
        } catch(Exception err) {
            err.printStackTrace();
            JOptionPane.showMessageDialog(UserInterfaceEditor.this, err.getClass().getName() + ": " + err, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public byte[] persistContainer(com.codename1.ui.Container c) {
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream d = new DataOutputStream(b);

            /*d.writeInt(customComponents.size());
            for(CustomComponent current : customComponents) {
                d.writeUTF(current.getType());
                d.writeUTF(current.getClassName());
                d.writeUTF(current.getCodenameOneBaseClass());
            }*/

            persistComponent(c, d);

            postCreateComponent(c, d);

            // end of post create
            d.writeUTF("");

            d.close();
            return b.toByteArray();
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private int getInt(String fieldName, Class c, Object o) {
        try {
            Field m = c.getDeclaredField(fieldName);
            m.setAccessible(true);
            return m.getInt(o);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return -1;
    }

    public void postCreateComponent(com.codename1.ui.Component cmp, DataOutputStream out) throws IOException {
        if(isPropertyModified(cmp, PROPERTY_COMMAND) || isPropertyModified(cmp, PROPERTY_COMMAND_LEGACY)) {
            out.writeUTF(cmp.getName());
            out.writeInt(PROPERTY_COMMAND);
            ActionCommand cmd;
            if(cmp instanceof com.codename1.ui.Container) {
                cmd = (ActionCommand)((com.codename1.ui.Button) ((com.codename1.ui.Container)cmp).getLeadComponent()).getCommand();
            } else {
                cmd = (ActionCommand)((com.codename1.ui.Button)cmp).getCommand();
            }
            out.writeUTF(cmd.getCommandName());
            if(cmd.getIcon() != null) {
                out.writeUTF(res.findId(cmd.getIcon()));
            } else {
                out.writeUTF("");
            }
            if(cmd.getRolloverIcon() != null) {
                out.writeUTF(res.findId(cmd.getRolloverIcon()));
            } else {
                out.writeUTF("");
            }
            if(cmd.getPressedIcon() != null) {
                out.writeUTF(res.findId(cmd.getPressedIcon()));
            } else {
                out.writeUTF("");
            }
            if(cmd.getDisabledIcon() != null) {
                out.writeUTF(res.findId(cmd.getDisabledIcon()));
            } else {
                out.writeUTF("");
            }
            out.writeInt(cmd.getId());
            if(cmd.getAction() != null) {
                out.writeUTF(cmd.getAction());
                if(cmd.getAction().equals("$Execute")) {
                    out.writeUTF(cmd.getArgument());
                }
            } else {
                out.writeUTF("");
            }
            out.writeBoolean(cmp.getComponentForm().getBackCommand() == cmd || cmd.isBackCommand());
        }
        if(isPropertyModified(cmp, PROPERTY_LABEL_FOR)) {
            if(cmp.getLabelForComponent() != null) {
                out.writeUTF(cmp.getName());
                out.writeInt(PROPERTY_LABEL_FOR);
                out.writeUTF(cmp.getLabelForComponent().getName());
            }
        }
        if(isPropertyModified(cmp, PROPERTY_LEAD_COMPONENT) && ((com.codename1.ui.Container)cmp).getLeadComponent() != null) {
            out.writeUTF(cmp.getName());
            out.writeInt(PROPERTY_LEAD_COMPONENT);
            out.writeUTF(((com.codename1.ui.Container)cmp).getLeadComponent().getName());
        }
        if(isPropertyModified(cmp, PROPERTY_NEXT_FOCUS_DOWN) && cmp.getNextFocusDown() != null) {
            out.writeUTF(cmp.getName());
            out.writeInt(PROPERTY_NEXT_FOCUS_DOWN);
            out.writeUTF(cmp.getNextFocusDown().getName());
        }
        if(isPropertyModified(cmp, PROPERTY_NEXT_FOCUS_UP) && cmp.getNextFocusUp() != null) {
            out.writeUTF(cmp.getName());
            out.writeInt(PROPERTY_NEXT_FOCUS_UP);
            out.writeUTF(cmp.getNextFocusUp().getName());
        }
        if(isPropertyModified(cmp, PROPERTY_NEXT_FOCUS_LEFT) && cmp.getNextFocusLeft() != null) {
            out.writeUTF(cmp.getName());
            out.writeInt(PROPERTY_NEXT_FOCUS_LEFT);
            out.writeUTF(cmp.getNextFocusLeft().getName());
        }
        if(isPropertyModified(cmp, PROPERTY_NEXT_FOCUS_RIGHT) && cmp.getNextFocusRight() != null) {
            out.writeUTF(cmp.getName());
            out.writeInt(PROPERTY_NEXT_FOCUS_RIGHT);
            out.writeUTF(cmp.getNextFocusRight().getName());
        }
        if(isActualContainer(cmp)) {
            com.codename1.ui.Container c = (com.codename1.ui.Container)cmp;
            for(int iter = 0 ; iter < c.getComponentCount() ; iter++) {
                postCreateComponent(c.getComponentAt(iter), out);
            }
        }
    }
    
    private boolean hasBackCommand(com.codename1.ui.Form frm, com.codename1.ui.Command cmd) {
        if(frm.getBackCommand() != null) {
            for(int iter = frm.getCommandCount() - 1 ; iter >= 0 ; iter--) {
                if(frm.getCommand(iter) == frm.getBackCommand()) {
                    return true;
                }
            }
        }
        return false;
    }

    public void persistComponent(com.codename1.ui.Component cmp, DataOutputStream out) throws IOException {
        if(cmp.getClientProperty("%base_form%") != null) {
            out.writeUTF((String)cmp.getClientProperty("%base_form%"));
            out.writeInt(PROPERTY_BASE_FORM);
        }
        out.writeUTF((String)cmp.getClientProperty(TYPE_KEY));
        out.writeInt(PROPERTY_NAME);
        if(cmp.getName() != null) {
            out.writeUTF(cmp.getName());
        } else {
            out.writeUTF("");
        }
        if(cmp.getCloudBoundProperty() != null) {
            out.writeInt(PROPERTY_CLOUD_BOUND_PROPERTY);
            out.writeUTF(cmp.getCloudBoundProperty());
        }
        if(cmp.getCloudDestinationProperty() != null) {
            out.writeInt(PROPERTY_CLOUD_DESTINATION_PROPERTY);
            out.writeUTF(cmp.getCloudDestinationProperty());
        }
        if(isActualContainer(cmp) || cmp instanceof com.codename1.ui.list.ContainerList) {
            com.codename1.ui.Container cnt = (com.codename1.ui.Container)cmp;
            if(isPropertyModified(cnt, PROPERTY_SCROLLABLE_X)) {
                out.writeInt(PROPERTY_SCROLLABLE_X);
                out.writeBoolean(CodenameOneAccessor.isScrollableX(cnt));
            }
            if(isPropertyModified(cnt, PROPERTY_SCROLLABLE_Y)) {
                out.writeInt(PROPERTY_SCROLLABLE_Y);
                out.writeBoolean(CodenameOneAccessor.isScrollableY(cnt));
            }
            if(cmp instanceof com.codename1.ui.Tabs) {
                com.codename1.ui.Tabs tab = (com.codename1.ui.Tabs)cmp;
                out.writeInt(PROPERTY_COMPONENTS);
                out.writeInt(tab.getTabCount());
                for(int iter = 0 ; iter < tab.getTabCount() ; iter++) {
                    out.writeUTF(tab.getTabTitle(iter));
                    persistComponent(tab.getTabComponentAt(iter), out);
                }

                if(isPropertyModified(cmp, PROPERTY_TAB_PLACEMENT)) {
                    out.writeInt(PROPERTY_TAB_PLACEMENT);
                    out.writeInt(((com.codename1.ui.Tabs)cmp).getTabPlacement());
                }
                if(isPropertyModified(cmp, PROPERTY_TAB_TEXT_POSITION)) {
                    out.writeInt(PROPERTY_TAB_TEXT_POSITION);
                    out.writeInt(((com.codename1.ui.Tabs)cmp).getTabTextPosition());
                }
            } else {
                if(isPropertyModified(cmp, PROPERTY_LAYOUT)) {
                    com.codename1.ui.layouts.Layout l = cnt.getLayout();
                    out.writeInt(PROPERTY_LAYOUT);
                    if(l instanceof com.codename1.ui.layouts.FlowLayout) {
                        out.writeShort(LAYOUT_FLOW);
                        com.codename1.ui.layouts.FlowLayout f = (com.codename1.ui.layouts.FlowLayout)l;
                        out.writeBoolean(f.isFillRows());
                        out.writeInt(f.getAlign());
                        out.writeInt(f.getValign());
                    } else {
                        if(l instanceof com.codename1.ui.layouts.BorderLayout) {
                            out.writeShort(LAYOUT_BORDER);
                            com.codename1.ui.layouts.BorderLayout b = (com.codename1.ui.layouts.BorderLayout)l;
                            String north = b.getLandscapeSwap(com.codename1.ui.layouts.BorderLayout.NORTH);
                            String east = b.getLandscapeSwap(com.codename1.ui.layouts.BorderLayout.EAST);
                            String west = b.getLandscapeSwap(com.codename1.ui.layouts.BorderLayout.WEST);
                            String south = b.getLandscapeSwap(com.codename1.ui.layouts.BorderLayout.SOUTH);
                            String center = b.getLandscapeSwap(com.codename1.ui.layouts.BorderLayout.CENTER);
                            out.writeBoolean(north != null);
                            if(north != null) {
                                out.writeUTF(north);
                            }
                            out.writeBoolean(east != null);
                            if(east != null) {
                                out.writeUTF(east);
                            }
                            out.writeBoolean(west != null);
                            if(west != null) {
                                out.writeUTF(west);
                            }
                            out.writeBoolean(south != null);
                            if(south != null) {
                                out.writeUTF(south);
                            }
                            out.writeBoolean(center != null);
                            if(center != null) {
                                out.writeUTF(center);
                            }
                            out.writeBoolean(b.isAbsoluteCenter());
                        } else {
                            if(l instanceof com.codename1.ui.layouts.GridLayout) {
                                out.writeShort(LAYOUT_GRID);
                                out.writeInt(((com.codename1.ui.layouts.GridLayout)l).getRows());
                                out.writeInt(((com.codename1.ui.layouts.GridLayout)l).getColumns());
                            } else {
                                if(l instanceof com.codename1.ui.layouts.BoxLayout) {
                                    if(getInt("axis", l.getClass(), l) == com.codename1.ui.layouts.BoxLayout.X_AXIS) {
                                        out.writeShort(LAYOUT_BOX_X);
                                    } else {
                                        out.writeShort(LAYOUT_BOX_Y);
                                    }
                                } else {
                                    if(l instanceof com.codename1.ui.table.TableLayout) {
                                        out.writeShort(LAYOUT_TABLE);
                                        out.writeInt(((com.codename1.ui.table.TableLayout)l).getRows());
                                        out.writeInt(((com.codename1.ui.table.TableLayout)l).getColumns());
                                    } else {
                                        if(l instanceof com.codename1.ui.layouts.LayeredLayout) {
                                            out.writeShort(LAYOUT_LAYERED);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if(cmp instanceof com.codename1.ui.Form) {
                    com.codename1.ui.Form frm = (com.codename1.ui.Form)cmp;
                    out.writeInt(PROPERTY_COMPONENTS);
                    out.writeInt(frm.getContentPane().getComponentCount());
                    for(int iter = 0 ; iter < frm.getContentPane().getComponentCount() ; iter++) {
                        persistComponent(frm.getContentPane().getComponentAt(iter), out);
                    }

                    if(isPropertyModified(cmp, PROPERTY_NEXT_FORM) && frm.getClientProperty("%next_form%") != null) {
                        out.writeInt(PROPERTY_NEXT_FORM);
                        out.writeUTF((String)frm.getClientProperty("%next_form%"));
                    }
                    if(isPropertyModified(cmp, PROPERTY_TITLE)) {
                        out.writeInt(PROPERTY_TITLE);
                        out.writeUTF(frm.getTitle());
                    }
                    if(isPropertyModified(cmp, PROPERTY_CYCLIC_FOCUS)) {
                        out.writeInt(PROPERTY_CYCLIC_FOCUS);
                        out.writeBoolean(frm.isCyclicFocus());
                    }
                    if(isPropertyModified(cmp, PROPERTY_DIALOG_UIID) && cmp instanceof com.codename1.ui.Dialog) {
                        com.codename1.ui.Dialog dlg = (com.codename1.ui.Dialog)cmp;
                        out.writeInt(PROPERTY_DIALOG_UIID);
                        out.writeUTF(dlg.getDialogUIID());
                    }
                    if(isPropertyModified(cmp, PROPERTY_DISPOSE_WHEN_POINTER_OUT) && cmp instanceof com.codename1.ui.Dialog) {
                        com.codename1.ui.Dialog dlg = (com.codename1.ui.Dialog)cmp;
                        out.writeInt(PROPERTY_DISPOSE_WHEN_POINTER_OUT);
                        out.writeBoolean(dlg.isDisposeWhenPointerOutOfBounds());
                    }
                    if(isPropertyModified(cmp, PROPERTY_DIALOG_POSITION) && cmp instanceof com.codename1.ui.Dialog) {
                        com.codename1.ui.Dialog dlg = (com.codename1.ui.Dialog)cmp;
                        if(dlg.getDialogPosition() != null) {
                            out.writeInt(PROPERTY_DIALOG_POSITION);
                            out.writeUTF(dlg.getDialogPosition());
                        }
                    }

                    if(frm.getCommandCount() > 0 || frm.getBackCommand() != null) {
                        if(isPropertyModified(cmp, PROPERTY_COMMANDS) || isPropertyModified(cmp, PROPERTY_COMMANDS_LEGACY)) {
                            out.writeInt(PROPERTY_COMMANDS);
                            if(frm.getBackCommand() != null && !hasBackCommand(frm, frm.getBackCommand())) {
                                out.writeInt(frm.getCommandCount() + 1);
                                ActionCommand cmd = (ActionCommand)frm.getBackCommand();
                                out.writeUTF(cmd.getCommandName());
                                if(cmd.getIcon() != null) {
                                    out.writeUTF(res.findId(cmd.getIcon()));
                                } else {
                                    out.writeUTF("");
                                }
                                if(cmd.getRolloverIcon() != null) {
                                    out.writeUTF(res.findId(cmd.getRolloverIcon()));
                                } else {
                                    out.writeUTF("");
                                }
                                if(cmd.getPressedIcon() != null) {
                                    out.writeUTF(res.findId(cmd.getPressedIcon()));
                                } else {
                                    out.writeUTF("");
                                }
                                if(cmd.getDisabledIcon() != null) {
                                    out.writeUTF(res.findId(cmd.getDisabledIcon()));
                                } else {
                                    out.writeUTF("");
                                }
                                out.writeInt(cmd.getId());
                                if(cmd.getAction() != null) {
                                    out.writeUTF(cmd.getAction());
                                    if(cmd.getAction().equals("$Execute")) {
                                        out.writeUTF(cmd.getArgument());
                                    }
                                } else {
                                    out.writeUTF("");
                                }
                                out.writeBoolean(frm.getBackCommand() == cmd);
                            } else {
                                out.writeInt(frm.getCommandCount());
                            }
                            for(int iter = frm.getCommandCount() - 1 ; iter >= 0 ; iter--) {
                                ActionCommand cmd = (ActionCommand)frm.getCommand(iter);
                                out.writeUTF(cmd.getCommandName());
                                if(cmd.getIcon() != null) {
                                    out.writeUTF(res.findId(cmd.getIcon()));
                                } else {
                                    out.writeUTF("");
                                }
                                if(cmd.getRolloverIcon() != null) {
                                    out.writeUTF(res.findId(cmd.getRolloverIcon()));
                                } else {
                                    out.writeUTF("");
                                }
                                if(cmd.getPressedIcon() != null) {
                                    out.writeUTF(res.findId(cmd.getPressedIcon()));
                                } else {
                                    out.writeUTF("");
                                }
                                if(cmd.getDisabledIcon() != null) {
                                    out.writeUTF(res.findId(cmd.getDisabledIcon()));
                                } else {
                                    out.writeUTF("");
                                }
                                out.writeInt(cmd.getId());
                                if(cmd.getAction() != null) {
                                    out.writeUTF(cmd.getAction());
                                    if(cmd.getAction().equals("$Execute")) {
                                        out.writeUTF(cmd.getArgument());
                                    }
                                } else {
                                    out.writeUTF("");
                                }
                                out.writeBoolean(frm.getBackCommand() == cmd);
                            }
                        }
                    }
                } else {
                    if(!(cmp instanceof com.codename1.ui.list.ContainerList)) {
                        out.writeInt(PROPERTY_COMPONENTS);
                        out.writeInt(cnt.getComponentCount());
                        for(int iter = 0 ; iter < cnt.getComponentCount() ; iter++) {
                            persistComponent(cnt.getComponentAt(iter), out);
                        }
                    } else {
                        com.codename1.ui.list.ContainerList lst = ((com.codename1.ui.list.ContainerList)cmp);
                        if(isPropertyModified(cmp, PROPERTY_LIST_RENDERER) && lst.getRenderer() instanceof com.codename1.ui.list.GenericListCellRenderer) {
                            out.writeInt(PROPERTY_LIST_RENDERER);
                            com.codename1.ui.list.GenericListCellRenderer g = (com.codename1.ui.list.GenericListCellRenderer)lst.getRenderer();
                            if(g.getSelectedEven() == null) {
                                out.writeByte(2);
                                out.writeUTF(g.getSelected().getName());
                                out.writeUTF(g.getUnselected().getName());
                            } else {
                                out.writeByte(4);
                                out.writeUTF(g.getSelected().getName());
                                out.writeUTF(g.getUnselected().getName());
                                out.writeUTF(g.getSelectedEven().getName());
                                out.writeUTF(g.getUnselectedEven().getName());
                            }
                        }                        
                    }
                }
            }
        } else {
            if(cmp instanceof com.codename1.ui.Label) {
                com.codename1.ui.Label lbl = (com.codename1.ui.Label)cmp;
                out.writeInt(PROPERTY_TEXT);
                out.writeUTF(lbl.getText());
                if(isPropertyModified(cmp, PROPERTY_ALIGNMENT)) {
                    out.writeInt(PROPERTY_ALIGNMENT);
                    out.writeInt(lbl.getAlignment());
                }
                if(isPropertyModified(cmp, PROPERTY_ICON)) {
                    if(lbl.getIcon() != null) {
                        out.writeInt(PROPERTY_ICON);
                        out.writeUTF(res.findId(lbl.getIcon()));
                    } 
                }
                if(lbl instanceof com.codename1.ui.Button) {
                    com.codename1.ui.Button button = (com.codename1.ui.Button)lbl;
                    if(isPropertyModified(cmp, PROPERTY_ROLLOVER_ICON)) {
                        if(button.getRolloverIcon() != null) {
                            out.writeInt(PROPERTY_ROLLOVER_ICON);
                            out.writeUTF(res.findId(button.getRolloverIcon()));
                        }
                    }
                    if(isPropertyModified(cmp, PROPERTY_PRESSED_ICON)) {
                        if(button.getPressedIcon() != null) {
                            out.writeInt(PROPERTY_PRESSED_ICON);
                            out.writeUTF(res.findId(button.getPressedIcon()));
                        }
                    }
                    if(isPropertyModified(cmp, PROPERTY_DISABLED_ICON)) {
                        if(button.getDisabledIcon() != null) {
                            out.writeInt(PROPERTY_DISABLED_ICON);
                            out.writeUTF(res.findId(button.getDisabledIcon()));
                        }
                    }
                    if(isPropertyModified(cmp, PROPERTY_TOGGLE_BUTTON)) {
                        out.writeInt(PROPERTY_TOGGLE_BUTTON);
                        out.writeBoolean(((com.codename1.ui.Button)cmp).isToggle());
                    }
                } else {
                    if(lbl instanceof com.codename1.ui.Slider) {
                        com.codename1.ui.Slider sld = (com.codename1.ui.Slider)lbl;
                        if(isPropertyModified(cmp, PROPERTY_EDITABLE)) {
                            out.writeInt(PROPERTY_EDITABLE);
                            out.writeBoolean(sld.isEditable());
                        }
                        if(isPropertyModified(cmp, PROPERTY_INFINITE)) {
                            out.writeInt(PROPERTY_INFINITE);
                            out.writeBoolean(sld.isInfinite());
                        }
                        if(isPropertyModified(cmp, PROPERTY_SLIDER_THUMB) && sld.getThumbImage() != null) {
                            out.writeInt(PROPERTY_SLIDER_THUMB);
                            out.writeUTF(res.findId(sld.getThumbImage()));
                        }
                        if(isPropertyModified(cmp, PROPERTY_PROGRESS)) {
                            out.writeInt(PROPERTY_PROGRESS);
                            out.writeInt(sld.getProgress());
                        }
                        if(isPropertyModified(cmp, PROPERTY_VERTICAL)) {
                            out.writeInt(PROPERTY_VERTICAL);
                            out.writeBoolean(sld.isVertical());
                        }
                        if(isPropertyModified(cmp, PROPERTY_INCREMENTS)) {
                            out.writeInt(PROPERTY_INCREMENTS);
                            out.writeInt(sld.getIncrements());
                        }
                        if(isPropertyModified(cmp, PROPERTY_MAX_VALUE)) {
                            out.writeInt(PROPERTY_MAX_VALUE);
                            out.writeInt(sld.getMaxValue());
                        }
                        if(isPropertyModified(cmp, PROPERTY_MIN_VALUE)) {
                            out.writeInt(PROPERTY_MIN_VALUE);
                            out.writeInt(sld.getMinValue());
                        }
                        if(isPropertyModified(cmp, PROPERTY_RENDER_PERCENTAGE_ON_TOP)) {
                            out.writeInt(PROPERTY_RENDER_PERCENTAGE_ON_TOP);
                            out.writeBoolean(sld.isRenderPercentageOnTop());
                        }
                    }
                }
                if(isPropertyModified(cmp, PROPERTY_RADIO_GROUP)) {
                    out.writeInt(PROPERTY_RADIO_GROUP);
                    out.writeUTF(((com.codename1.ui.RadioButton)cmp).getGroup());
                }
                if(isPropertyModified(cmp, PROPERTY_SELECTED)) {
                    out.writeInt(PROPERTY_SELECTED);
                    out.writeBoolean(((com.codename1.ui.Button)cmp).isSelected());
                }
                if(isPropertyModified(cmp, PROPERTY_GAP)) {
                    out.writeInt(PROPERTY_GAP);
                    out.writeInt(lbl.getGap());
                }
                if(isPropertyModified(cmp, PROPERTY_VERTICAL_ALIGNMENT)) {
                    out.writeInt(PROPERTY_VERTICAL_ALIGNMENT);
                    out.writeInt(lbl.getVerticalAlignment());
                }
                if(isPropertyModified(cmp, PROPERTY_TEXT_POSITION)) {
                    out.writeInt(PROPERTY_TEXT_POSITION);
                    out.writeInt(lbl.getTextPosition());
                }
            } else {
                if(cmp instanceof com.codename1.ui.TextArea) {
                    com.codename1.ui.TextArea txt = (com.codename1.ui.TextArea)cmp;
                    if(isPropertyModified(cmp, PROPERTY_VERTICAL_ALIGNMENT)) {
                        out.writeInt(PROPERTY_VERTICAL_ALIGNMENT);
                        out.writeInt(txt.getVerticalAlignment());
                    }
                    if(isPropertyModified(cmp, PROPERTY_TEXT)) {
                        out.writeInt(PROPERTY_TEXT);
                        out.writeUTF(txt.getText());
                    }
                    if(isPropertyModified(cmp, PROPERTY_TEXT_AREA_GROW)) {
                        out.writeInt(PROPERTY_TEXT_AREA_GROW);
                        out.writeBoolean(txt.isGrowByContent());
                    }
                    if(isPropertyModified(cmp, PROPERTY_TEXT_CONSTRAINT)) {
                        out.writeInt(PROPERTY_TEXT_CONSTRAINT);
                        out.writeInt(txt.getConstraint());
                    }
                    if(isPropertyModified(cmp, PROPERTY_TEXT_MAX_LENGTH)) {
                        out.writeInt(PROPERTY_TEXT_MAX_LENGTH);
                        out.writeInt(txt.getMaxSize());
                    }
                    if(isPropertyModified(cmp, PROPERTY_EDITABLE)) {
                        out.writeInt(PROPERTY_EDITABLE);
                        out.writeBoolean(txt.isEditable());
                    }
                    if(isPropertyModified(cmp, PROPERTY_ALIGNMENT)) {
                        out.writeInt(PROPERTY_ALIGNMENT);
                        out.writeInt(txt.getAlignment());
                    }
                    if(isPropertyModified(cmp, PROPERTY_HINT)) {
                        out.writeInt(PROPERTY_HINT);
                        out.writeUTF(txt.getHint());
                    }
                    if(isPropertyModified(cmp, PROPERTY_HINT_ICON) && txt.getHintIcon() != null) {
                        out.writeInt(PROPERTY_HINT_ICON);
                        out.writeUTF(res.findId(txt.getHintIcon()));
                    }
                    if(isPropertyModified(cmp, PROPERTY_COLUMNS)) {
                        out.writeInt(PROPERTY_COLUMNS);
                        out.writeInt(txt.getColumns());
                    }
                    if(isPropertyModified(cmp, PROPERTY_ROWS)) {
                        out.writeInt(PROPERTY_ROWS);
                        out.writeInt(txt.getRows());
                    }
                } else {
                    if(cmp instanceof com.codename1.ui.List) {
                        com.codename1.ui.List lst = (com.codename1.ui.List)cmp;
                        if(isPropertyModified(cmp, PROPERTY_ITEM_GAP)) {
                            out.writeInt(PROPERTY_ITEM_GAP);
                            out.writeInt(lst.getItemGap());
                        }

                        if(isPropertyModified(cmp, PROPERTY_LIST_FIXED)) {
                            out.writeInt(PROPERTY_LIST_FIXED);
                            out.writeInt(lst.getFixedSelection());
                        }

                        if(isPropertyModified(cmp, PROPERTY_LIST_ORIENTATION)) {
                            out.writeInt(PROPERTY_LIST_ORIENTATION);
                            out.writeInt(lst.getOrientation());
                        }
                        
                        if(isPropertyModified(cmp, PROPERTY_HINT)) {
                            out.writeInt(PROPERTY_HINT);
                            out.writeUTF(lst.getHint());
                        }
                        if(isPropertyModified(cmp, PROPERTY_HINT_ICON) && lst.getHintIcon() != null) {
                            out.writeInt(PROPERTY_HINT_ICON);
                            out.writeUTF(res.findId(lst.getHintIcon()));
                        }

                        if(isPropertyModified(cmp, PROPERTY_LIST_RENDERER) && lst.getRenderer() instanceof com.codename1.ui.list.GenericListCellRenderer) {
                            out.writeInt(PROPERTY_LIST_RENDERER);
                            com.codename1.ui.list.GenericListCellRenderer g = (com.codename1.ui.list.GenericListCellRenderer)lst.getRenderer();
                            if(g.getSelectedEven() == null) {
                                out.writeByte(2);
                                out.writeUTF(g.getSelected().getName());
                                out.writeUTF(g.getUnselected().getName());
                            } else {
                                out.writeByte(4);
                                out.writeUTF(g.getSelected().getName());
                                out.writeUTF(g.getUnselected().getName());
                                out.writeUTF(g.getSelectedEven().getName());
                                out.writeUTF(g.getUnselectedEven().getName());
                            }
                        }

                        if(!(cmp instanceof com.codename1.components.RSSReader)) {
                            out.writeInt(PROPERTY_LIST_ITEMS);
                            out.writeInt(lst.getModel().getSize());
                            for(int iter = 0 ; iter < lst.getModel().getSize() ; iter++) {
                                Object o = lst.getModel().getItemAt(iter);
                                if(o instanceof String) {
                                    out.writeByte(1);
                                    out.writeUTF((String)o);
                                } else {
                                    out.writeByte(2);
                                    Hashtable h = (Hashtable)o;
                                    out.writeInt(h.size());
                                    for(Object key : h.keySet()) {
                                        Object val = h.get(key);
                                        if(val instanceof com.codename1.ui.Image) {
                                            out.writeInt(2);
                                            out.writeUTF((String)key);
                                            out.writeUTF(res.findId(val));
                                        } else {
                                            out.writeInt(1);
                                            out.writeUTF((String)key);
                                            if(val instanceof ActionCommand) {
                                                out.writeUTF(((ActionCommand)val).getAction());
                                            } else {
                                                out.writeUTF((String)val);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if(isPropertyModified(cmp, PROPERTY_LAYOUT_CONSTRAINT) || 
                (cmp.getParent() != null && cmp.getParent().getLayout() instanceof com.codename1.ui.layouts.BorderLayout)) {
            if(cmp.getParent() != null && cmp != containerInstance && cmp.getClientProperty("%base_form%") == null) {
                com.codename1.ui.layouts.Layout l = cmp.getParent().getLayout();
                if(l instanceof com.codename1.ui.layouts.BorderLayout) {
                    out.writeInt(PROPERTY_LAYOUT_CONSTRAINT);
                    out.writeUTF((String)l.getComponentConstraint(cmp));
                } else {
                    if(l instanceof com.codename1.ui.table.TableLayout) {
                        out.writeInt(PROPERTY_LAYOUT_CONSTRAINT);
                        com.codename1.ui.table.TableLayout.Constraint con = (com.codename1.ui.table.TableLayout.Constraint)l.getComponentConstraint(cmp);
                        out.writeInt(getInt("row", con.getClass(), con));
                        out.writeInt(getInt("column", con.getClass(), con));
                        out.writeInt(getInt("height", con.getClass(), con));
                        out.writeInt(getInt("width", con.getClass(), con));
                        out.writeInt(getInt("align", con.getClass(), con));
                        out.writeInt(getInt("spanHorizontal", con.getClass(), con));
                        out.writeInt(getInt("valign", con.getClass(), con));
                        out.writeInt(getInt("spanVertical", con.getClass(), con));
                    }
                }
            }
        }

        if(isPropertyModified(cmp, PROPERTY_EMBED)) {
            out.writeInt(PROPERTY_EMBED);
            out.writeUTF(((EmbeddedContainer)cmp).getEmbed());
        }

        if(isPropertyModified(cmp, PROPERTY_UIID)) {
            out.writeInt(PROPERTY_UIID);
            out.writeUTF(cmp.getUIID());
        }
        if(isPropertyModified(cmp, PROPERTY_FOCUSABLE)) {
            out.writeInt(PROPERTY_FOCUSABLE);
            out.writeBoolean(cmp.isFocusable());
        }
        if(isPropertyModified(cmp, PROPERTY_ENABLED)) {
            out.writeInt(PROPERTY_ENABLED);
            out.writeBoolean(cmp.isEnabled());
        }
        if(isPropertyModified(cmp, PROPERTY_RTL)) {
            out.writeInt(PROPERTY_RTL);
            out.writeBoolean(cmp.isRTL());
        }
        if(isPropertyModified(cmp, PROPERTY_SCROLL_VISIBLE)) {
            out.writeInt(PROPERTY_SCROLL_VISIBLE);
            out.writeBoolean(cmp.isScrollVisible());
        }
        
        if(isPropertyModified(cmp, PROPERTY_PREFERRED_WIDTH)) {
            out.writeInt(PROPERTY_PREFERRED_WIDTH);
            out.writeInt(cmp.getPreferredW());
        }

        if(isPropertyModified(cmp, PROPERTY_PREFERRED_HEIGHT)) {
            out.writeInt(PROPERTY_PREFERRED_HEIGHT);
            out.writeInt(cmp.getPreferredH());
        }
        if(isPropertyModified(cmp, PROPERTY_TENSILE_DRAG_ENABLED)) {
            out.writeInt(PROPERTY_TENSILE_DRAG_ENABLED);
            out.writeBoolean(cmp.isTensileDragEnabled());
        }
        if(isPropertyModified(cmp, PROPERTY_TACTILE_TOUCH)) {
            out.writeInt(PROPERTY_TACTILE_TOUCH);
            out.writeBoolean(cmp.isTactileTouch());
        }
        if(isPropertyModified(cmp, PROPERTY_SNAP_TO_GRID)) {
            out.writeInt(PROPERTY_SNAP_TO_GRID);
            out.writeBoolean(cmp.isSnapToGrid());
        }
        if(isPropertyModified(cmp, PROPERTY_FLATTEN)) {
            out.writeInt(PROPERTY_FLATTEN);
            out.writeBoolean(cmp.isFlatten());
        }

        if(isPropertyModified(cmp, PROPERTY_CUSTOM)) {
            for(String propName : cmp.getPropertyNames()) {
                if(isCustomPropertyModified(cmp, propName) && !propName.startsWith("$")) {
                    out.writeInt(PROPERTY_CUSTOM);
                    out.writeUTF(propName);
                    Class type = getPropertyCustomType(cmp, propName);
                    Object value = cmp.getPropertyValue(propName);
                    if(value == null) {
                        out.writeBoolean(true);
                        continue;
                    }
                    out.writeBoolean(false);
                    if(type == String.class) {
                        out.writeUTF((String)value);
                        continue;
                    }

                    if(type == String[].class) {
                        String[] result = (String[])value;
                        out.writeInt(result.length);
                        for(int i = 0 ; i < result.length ; i++) {
                            out.writeUTF(result[i]);
                        }
                        continue;
                    }

                    if(type == String[][].class) {
                        String[][] result = (String[][])value;
                        out.writeInt(result.length);
                        for(int i = 0 ; i < result.length ; i++) {
                            out.writeInt(result[i].length);
                            for(int j = 0 ; j < result[i].length ; j++) {
                                out.writeUTF(result[i][j]);
                            }
                        }
                        continue;
                    }

                    if(type == Integer.class) {
                        out.writeInt(((Number)value).intValue());
                        continue;
                    }

                    if(type == Long.class) {
                        out.writeLong(((Number)value).longValue());
                        continue;
                    }

                    if(type == Double.class) {
                        out.writeDouble(((Number)value).doubleValue());
                        continue;
                    }

                    if(type == Date.class) {
                        if(value == null) {
                            out.writeBoolean(false);
                            continue;
                        }
                        out.writeBoolean(true);
                        out.writeLong(((Date)value).getTime());
                        continue;
                    }
                    
                    if(type == Float.class) {
                        out.writeFloat(((Number)value).floatValue());
                        continue;
                    }

                    if(type == Byte.class) {
                        out.writeByte(((Number)value).byteValue());
                        continue;
                    }

                    if(type == Boolean.class) {
                        out.writeBoolean(((Boolean)value).booleanValue());
                        continue;
                    }

                    if(type == com.codename1.ui.Image[].class) {
                        com.codename1.ui.Image[] result = (com.codename1.ui.Image[])value;
                        out.writeInt(result.length);
                        for(int i = 0 ; i < result.length ; i++) {
                            if(result[i] == null) {
                                out.writeUTF("");
                            } else {
                                String id = res.findId(result[i]);
                                if(id == null) {
                                   out.writeUTF("");
                                } else {
                                   out.writeUTF(id);
                                }
                            }
                        }
                        continue;
                    }

                    if(type == com.codename1.ui.Image.class) {
                        com.codename1.ui.Image result = (com.codename1.ui.Image)value;
                        if(result == null) {
                            out.writeUTF("");
                        } else {
                            String id = res.findId(result);
                            if(id == null) {
                               out.writeUTF("");
                            } else {
                               out.writeUTF(id);
                            }
                        }
                        continue;
                    }

                    if(type == com.codename1.ui.Container.class) {
                        out.writeUTF(((com.codename1.ui.Container)value).getName());
                        continue;
                    }

                    if(type == com.codename1.ui.list.CellRenderer.class) {
                        com.codename1.ui.list.GenericListCellRenderer g = (com.codename1.ui.list.GenericListCellRenderer)value;
                        if(g.getSelectedEven() == null) {
                            out.writeByte(2);
                            out.writeUTF(g.getSelected().getName());
                            out.writeUTF(g.getUnselected().getName());
                        } else {
                            out.writeByte(4);
                            out.writeUTF(g.getSelected().getName());
                            out.writeUTF(g.getUnselected().getName());
                            out.writeUTF(g.getSelectedEven().getName());
                            out.writeUTF(g.getUnselectedEven().getName());
                        }
                        continue;
                    }

                    if(type == Object[].class) {
                        Object[] arr = (Object[])value;
                        out.writeInt(arr.length);
                        for(int iter = 0 ; iter < arr.length ; iter++) {
                            Object o = arr[iter];
                            if(o instanceof String) {
                                out.writeByte(1);
                                out.writeUTF((String)o);
                            } else {
                                out.writeByte(2);
                                Hashtable h = (Hashtable)o;
                                out.writeInt(h.size());
                                for(Object key : h.keySet()) {
                                    Object val = h.get(key);
                                    if(val instanceof com.codename1.ui.Image) {
                                        out.writeInt(2);
                                        out.writeUTF((String)key);
                                        out.writeUTF(res.findId(val));
                                    } else {
                                        out.writeInt(1);
                                        out.writeUTF((String)key);
                                        out.writeUTF((String)val);
                                    }
                                }
                            }
                        }
                        continue;
                    }
                    
                    // none of the above then its a char
                    out.writeChar(((Character)value).charValue());
                }
            }
        }

        out.writeInt(-1);
    }

    private Class getPropertyCustomType(com.codename1.ui.Component cmp, String name) {
        String[] names = cmp.getPropertyNames();
        for(int iter = 0 ; iter < names.length ; iter++) {
            if(name.equals(names[iter])) {
                return cmp.getPropertyTypes()[iter];
            }
        }
        return null;
    }

    public void setPropertyModified(com.codename1.ui.Component cmp, int propertyId) {
        List<Integer> lst = (List<Integer>)cmp.getClientProperty("$modified$");
        if(lst == null) {
            lst = new ArrayList<Integer>();
            cmp.putClientProperty("$modified$", lst);
        }
        lst.add(propertyId);
    }

    private void clearPropertyModification(com.codename1.ui.Component cmp, int propertyId, String name) {
        if(propertyId == PROPERTY_CUSTOM) {
            List<String> lst = (List<String>)cmp.getClientProperty("$custom_modified$");
            if(lst != null) {
                lst.remove(name);
            }
            return;
        }
        List<Integer> lst = (List<Integer>)cmp.getClientProperty("$modified$");
        if(lst != null) {
            lst.remove(new Integer(propertyId));
        }
    }

    private boolean isPropertyModified(com.codename1.ui.Component cmp, int propertyId) {
        List<Integer> lst = (List<Integer>)cmp.getClientProperty("$modified$");
        return lst != null && lst.contains(propertyId);
    }

    public void setCustomPropertyModified(com.codename1.ui.Component cmp, String name) {
        List<String> lst = (List<String>)cmp.getClientProperty("$custom_modified$");
        if(lst == null) {
            lst = new ArrayList<String>();
            cmp.putClientProperty("$custom_modified$", lst);
        }
        lst.add(name);
    }

    private boolean isCustomPropertyModified(com.codename1.ui.Component cmp, String name) {
        List<String> lst = (List<String>)cmp.getClientProperty("$custom_modified$");
        return lst != null && lst.contains(name);
    }

    class LocalizationTableModel implements TableModel {
        private List<TableModelListener> listener = new ArrayList<TableModelListener>();
        private String[] COLUMN_NAMES = {"Locale", "Translation"};
        private String bundle;
        private List<String> rows = new ArrayList<String>();
        private List<String> values = new ArrayList<String>();

        public void setText(String text) {
            rows.clear();
            values.clear();
            if(text != null && text.length() > 0) {
                bundle = (String)resourceBundle.getSelectedItem();
                if(bundle != null) {
                    rows.add("Key");
                    values.add(text);
                    Enumeration e = res.listL10NLocales(bundle);
                    while(e.hasMoreElements()) {
                        String key = (String)e.nextElement();
                        rows.add(key);
                        String value = (String)res.getL10N(bundle, key).get(text);
                        values.add(value);
                    }
                }
            }
            fireTableModelListener(new TableModelEvent(this));
        }

        public int getRowCount() {
            return rows.size();
        }

        public int getColumnCount() {
            return COLUMN_NAMES.length;
        }

        public String getColumnName(int columnIndex) {
            return COLUMN_NAMES[columnIndex];
        }

        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 1 && rowIndex > 0;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            if(columnIndex == 0) {
                return rows.get(rowIndex);
            }
            return values.get(rowIndex);
        }

        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            String key = (String)getValueAt(rowIndex, 0);
            res.setLocaleProperty(bundle, key, (String)getValueAt(0, 1), aValue);
            values.set(rowIndex, (String)aValue);
        }

        public void addTableModelListener(TableModelListener l) {
            listener.add(l);
        }

        public void removeTableModelListener(TableModelListener l) {
            listener.remove(l);
        }
        
        private void fireTableModelListener(TableModelEvent e) {
            for(TableModelListener l : listener) {
                l.tableChanged(e);
            }
        }

    }

    class ComponentPropertyEditorModel implements TableModel {
        private final String[] COLUMNS = {"Name", "Value"};
        private com.codename1.ui.Component[] cmps;
        private List<TableModelListener> listener = new ArrayList<TableModelListener>();

        private List<String> propertyNames = new ArrayList<String>();
        private List<Method> propertyGetters = new ArrayList<Method>();
        private List<Method> propertySetters = new ArrayList<Method>();
        private List<Class> propertyClasses = new ArrayList<Class>();
        private List<Integer> propertyIds = new ArrayList<Integer>();
        private List<String> customProperties = new ArrayList<String>();

        public com.codename1.ui.Component[] getComponents() {
            return cmps;
        }
        
        private void refreshLocaleTable() {
            LocalizationTableModel lt = (LocalizationTableModel)localizeTable.getModel();
            if(cmps.length == 1 && cmps[0] instanceof com.codename1.ui.Label) {
                lt.setText(((com.codename1.ui.Label)cmps[0]).getText());
            } else {
                lt.setText(null);
            }
        }

        public void setComponents(com.codename1.ui.Component[] cmps) {
            this.cmps = cmps;

            refreshLocaleTable();

            propertyClasses.clear();
            propertyGetters.clear();
            propertySetters.clear();
            propertyNames.clear();
            propertyIds.clear();
            customProperties.clear();
            for(int iter = 0 ; iter < HARDCODED_COMPONENT_PROPERTIES.length ; iter++) {
                Method setter = getSetter(cmps, HARDCODED_COMPONENT_PROPERTIES[iter]);
                if(setter != null) {
                    Method getter = getGetter(cmps, HARDCODED_COMPONENT_PROPERTIES[iter]);
                    if(getter != null) {
                        propertyNames.add(HARDCODED_COMPONENT_PROPERTIES[iter]);
                        propertyIds.add(HARDCODED_COMPONENT_PROPERTY_KEYS[iter]);
                        propertyGetters.add(getter);
                        propertySetters.add(setter);
                        propertyClasses.add(HARDCODED_COMPONENT_PROPERTY_CLASSES[iter]);
                    }
                }
            }
            for(int iter = 0 ; iter < SUPPORTED_COMPONENT_PROPERTIES.length ; iter++) {
                Method setter = getSetter(cmps, SUPPORTED_COMPONENT_PROPERTIES[iter]);
                if(setter != null) {
                    Method getter = getGetter(cmps, SUPPORTED_COMPONENT_PROPERTIES[iter]);
                    if(getter != null) {
                        propertyNames.add(SUPPORTED_COMPONENT_PROPERTIES[iter]);
                        propertyIds.add(SUPPORTED_COMPONENT_KEYS[iter]);
                        propertyGetters.add(getter);
                        propertySetters.add(setter);
                        propertyClasses.add(SUPPORTED_COMPONENT_PROPERTY_CLASSES[iter]);
                    }
                }
            }
            if(cmps.length == 1) {
                com.codename1.ui.Component cmp = cmps[0];
                String[] custom = cmp.getPropertyNames();
                if(custom != null) {
                    for(String s : custom) {
                        customProperties.add(s);
                        propertyNames.add(s);
                        propertyIds.add(PROPERTY_CUSTOM);
                    }
                }

                if(cmp instanceof com.codename1.ui.Container) {
                    propertyNames.add("Scrollable X");
                    propertyIds.add(PROPERTY_SCROLLABLE_X);
                    propertyClasses.add(Boolean.class);
                    propertyNames.add("Scrollable Y");
                    propertyIds.add(PROPERTY_SCROLLABLE_Y);
                    propertyClasses.add(Boolean.class);
                }
                if(cmp instanceof com.codename1.ui.Form) {
                    propertyNames.add("Commands");
                    propertyIds.add(PROPERTY_COMMANDS);
                    propertyClasses.add(com.codename1.ui.Command.class);
                    propertyNames.add("Next Form");
                    propertyIds.add(PROPERTY_NEXT_FORM);
                    propertyClasses.add(com.codename1.ui.Form.class);
                } else {
                    propertyNames.add("LayoutConstraint");
                    propertyIds.add(PROPERTY_LAYOUT_CONSTRAINT);
                    propertyClasses.add(Object.class);
                }

                // root container is not a form or dialog so it can derive one
                if(!(cmp instanceof com.codename1.ui.Form) && cmp == containerInstance) {
                    propertyNames.add("Derive");
                    propertyIds.add(PROPERTY_BASE_FORM);
                    propertyClasses.add(com.codename1.ui.Form.class);
                }

                if(cmp instanceof com.codename1.ui.List && !(cmp instanceof com.codename1.components.RSSReader)) {
                    propertyNames.add("ListItems");
                    propertyIds.add(PROPERTY_LIST_ITEMS);
                    propertyClasses.add(String.class);
                }
            }
            fireTableModelListener(new TableModelEvent(this, -1, -1));
        }

        private Method getGetter(com.codename1.ui.Component[] cmps, String property) {
            Method m = null;
            for(com.codename1.ui.Component c : cmps) {
                Method currentMethod = getGetter(c.getClass(), property);
                if(m == null) {
                    if(currentMethod == null) {
                        return null;
                    }
                    m = currentMethod;
                } else {
                    if(currentMethod == null) {
                        return null;
                    }
                }
            }
            return m;
        }

        private Method getSetter(com.codename1.ui.Component[] cmps, String property) {
            Method m = null;
            for(com.codename1.ui.Component c : cmps) {
                Method currentMethod = getSetter(c.getClass(), property);
                if(m == null) {
                    if(currentMethod == null) {
                        return null;
                    }
                    m = currentMethod;
                } else {
                    if(currentMethod == null) {
                        return null;
                    }
                }
            }
            return m;
        }

        private Object invoke(Method m, Object instance, Object[] args) throws Exception {
            try {
                return m.invoke(instance, args);
            } catch(Throwable t) {
                // this could be because the method is from another class try to lookup a new method
                return instance.getClass().getMethod(m.getName(), m.getParameterTypes()).invoke(instance, args);
            }
        }

        private Method getSetter(Class cls, String property) {
            String setter = "set" + property;
            for(Method m : cls.getMethods()) {
                if(m.getName().equals(setter) && m.getParameterTypes().length == 1) {
                    return m;
                }
            }
            return null;
        }

        private Method getGetter(Class cls, String property) {
            String getter = "get" + property;
            String getterIs = "is" + property;
            for(Method m : cls.getMethods()) {
                if(m.getName().equals(getter) || m.getName().equals(getterIs)) {
                    Class[] t = m.getParameterTypes();
                    if(t == null || t.length == 0) {
                        return m;
                    }
                }
            }
            return null;
        }

        private void fireTableModelListener(TableModelEvent e) {
            for(TableModelListener l : listener) {
                l.tableChanged(e);
            }
        }

        public int getRowCount() {
            return propertyNames.size();
        }

        public int getColumnCount() {
            return 2;
        }

        public String getColumnName(int columnIndex) {
            return COLUMNS[columnIndex];
        }

        public int getRowId(int row) {
            return propertyIds.get(row);
        }

        public Class getRowClass(int row) {
            switch(propertyIds.get(row)) {
                case PROPERTY_COMMANDS:
                case PROPERTY_COMMANDS_LEGACY:
                    return com.codename1.ui.Command[].class;

                case PROPERTY_LAYOUT_CONSTRAINT:
                    if(cmps[0].getParent().getLayout() instanceof BorderLayout) {
                        return Object.class;
                    } else {
                        return TableLayout.Constraint.class;
                    }
                case PROPERTY_NEXT_FORM:
                    return String.class;

                case PROPERTY_BASE_FORM:
                    return String.class;

                case PROPERTY_LIST_ITEMS:
                    return Object[].class;

                case PROPERTY_CUSTOM:
                    return cmps[0].getPropertyTypes()[customProperties.indexOf(propertyNames.get(row))];
                    
                case PROPERTY_SCROLLABLE_X:
                case PROPERTY_SCROLLABLE_Y:
                    return Boolean.class;
            }
            if(propertyClasses.size() > row) {
                return propertyClasses.get(row);
            }
            return null;
        }

        public Class<?> getColumnClass(int columnIndex) {
            if(columnIndex == 0) {
                return String.class;
            }
            return Object.class;
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            if(columnIndex == 1) {
                // block the editability of some properties that aren't relevant
                // for complex components
                switch(propertyIds.get(rowIndex)) {
                    case PROPERTY_CUSTOM:
                        return !((String)getValueAt(rowIndex, 0)).startsWith("$");
                        
                    case PROPERTY_ICON:
                    case PROPERTY_ROLLOVER_ICON:
                    case PROPERTY_DISABLED_ICON:
                    case PROPERTY_PRESSED_ICON:
                    case PROPERTY_TEXT:
                        for(int iter = 0 ; iter < cmps.length ; iter++) {
                            if(cmps[iter] instanceof com.codename1.ui.Button && ((com.codename1.ui.Button)cmps[iter]).getCommand() != null) {
                                return false;
                            }
                        }
                        return true;

                    case PROPERTY_NEXT_FOCUS_DOWN:
                    case PROPERTY_NEXT_FOCUS_RIGHT:
                    case PROPERTY_NEXT_FOCUS_LEFT:
                    case PROPERTY_NEXT_FOCUS_UP:
                        return cmps.length == 1 && cmps[0].isFocusable();
                    case PROPERTY_LEAD_COMPONENT:
                    case PROPERTY_LAYOUT: 
                        // block layout from everything that isn't a form (dialog) or container
                        // so people won't mistakenly change the layout of a table or an HTMLComponent
                        return isStandardContainerSelected();
                }
                return true;
            }
            return false;
        }

        private boolean isStandardContainerSelected() {
            for(com.codename1.ui.Component current : cmps) {
                if(!(current.getClass() == com.codename1.ui.Container.class ||
                        current instanceof com.codename1.ui.Form ||
                        current instanceof com.codename1.ui.list.ContainerList)) {
                    return false;
                }
            }
            return true;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            return getValueAt(rowIndex, columnIndex, false);
        }
        
        public Object getValueAt(int rowIndex, int columnIndex, boolean noDiffer) {
            if(columnIndex == 0) {
                return propertyNames.get(rowIndex);
            }
            try {
                if(propertyGetters.size() <= rowIndex) {
                    switch(propertyIds.get(rowIndex)) {
                        case PROPERTY_COMMANDS:
                        case PROPERTY_COMMANDS_LEGACY:
                            com.codename1.ui.Form form = (com.codename1.ui.Form)cmps[0];
                            ActionCommand[] cmds = new ActionCommand[form.getCommandCount()];
                            for(int iter = 0 ; iter < cmds.length ; iter++) {
                                cmds[iter] = (ActionCommand)form.getCommand(iter);
                            }
                            return cmds;
                            
                        case PROPERTY_LAYOUT_CONSTRAINT:
                            if(cmps[0] == null || cmps[0].getParent() == null) {
                                return null;
                            }
                            return cmps[0].getParent().getLayout().getComponentConstraint(cmps[0]);

                        case PROPERTY_NEXT_FORM:
                            return cmps[0].getClientProperty("%next_form%");

                        case PROPERTY_BASE_FORM:
                            return cmps[0].getClientProperty("%base_form%");

                        case PROPERTY_SCROLLABLE_X:
                            boolean valX = CodenameOneAccessor.isScrollableX((com.codename1.ui.Container)cmps[0]);
                            if(noDiffer) {
                                return valX;
                            }
                            for(com.codename1.ui.Component cmp : cmps) {
                                if(valX != CodenameOneAccessor.isScrollableX((com.codename1.ui.Container)cmp)) {
                                    return PROPERTIES_DIFFER_IN_VALUE;
                                }
                            }
                            return valX;

                        case PROPERTY_SCROLLABLE_Y:
                            boolean valY = CodenameOneAccessor.isScrollableY((com.codename1.ui.Container)cmps[0]);
                            if(noDiffer) {
                                return valY;
                            }
                            for(com.codename1.ui.Component cmp : cmps) {
                                if(valY != CodenameOneAccessor.isScrollableY((com.codename1.ui.Container)cmp)) {
                                    return PROPERTIES_DIFFER_IN_VALUE;
                                }
                            }
                            return valY;

                        case PROPERTY_LIST_ITEMS:
                            Object[] vals = new Object[((com.codename1.ui.List)cmps[0]).getModel().getSize()];
                            for(int iter = 0 ; iter < vals.length ; iter++) {
                                vals[iter] = ((com.codename1.ui.List)cmps[0]).getModel().getItemAt(iter);
                            }
                            return vals;
                        case PROPERTY_CUSTOM:
                            return cmps[0].getPropertyValue(propertyNames.get(rowIndex));
                    }
                }
                Method g = propertyGetters.get(rowIndex);
                if(cmps.length > 1) {
                    Object val = invoke(g, cmps[0], new Object[0]);
                    if(noDiffer) {
                        return val;
                    }
                    if(val == null) {
                        for(int iter = 1 ; iter < cmps.length ; iter++) {
                            Object x = invoke(g, cmps[iter], new Object[0]);
                            if(x != null) {
                                return PROPERTIES_DIFFER_IN_VALUE;
                            }
                        }
                    } else {
                        for(int iter = 1 ; iter < cmps.length ; iter++) {
                            Object x = invoke(g, cmps[iter], new Object[0]);
                            if(!val.equals(x)) {
                                return PROPERTIES_DIFFER_IN_VALUE;
                            }
                        }
                    }
                    return val;
                } else {
                    return invoke(g, cmps[0], new Object[0]);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                return null;
            }
        }

        public void setValueAt(final Object aValue, final int rowIndex, final int columnIndex) {
            Display.getInstance().callSerially(new Runnable() {
                @Override
                public void run() {
                    try {
                        // prevent a property from being marked as modified when it is not
                        Object oldValue = getValueAt(rowIndex, columnIndex);
                        if(oldValue == aValue) {
                            return;
                        }
                        if(oldValue != null && oldValue.equals(aValue)) {
                            return;
                        }

                        int propertyId = propertyIds.get(rowIndex);
                        for(com.codename1.ui.Component cmp : cmps) {
                            setPropertyModified(cmp, propertyId);
                        }
                        switch(propertyId) {
                            case PROPERTY_COMMANDS:
                            case PROPERTY_COMMANDS_LEGACY:
                                com.codename1.ui.Form form = (com.codename1.ui.Form)cmps[0];
                                com.codename1.ui.Command[] cmds = (com.codename1.ui.Command[])aValue;
                                form.removeAllCommands();
                                form.setBackCommand(null);
                                for(int iter = cmds.length - 1 ; iter >= 0 ; iter--) {
                                    form.addCommand(cmds[iter]);
                                    if(cmds[iter] instanceof ActionCommand) {
                                        if(((ActionCommand)cmds[iter]).isBackCommand()) {
                                            form.setBackCommand(cmds[iter]);
                                        }
                                    }
                                }
                                properties.repaint();
                                return;

                            case PROPERTY_SCROLLABLE_X:
                                for(com.codename1.ui.Component cmp : cmps) {
                                    CodenameOneAccessor.setScrollableX((com.codename1.ui.Container)cmp, ((Boolean)aValue).booleanValue());
                                }
                                properties.repaint();
                                return;

                            case PROPERTY_SCROLLABLE_Y:
                                for(com.codename1.ui.Component cmp : cmps) {
                                    CodenameOneAccessor.setScrollableY((com.codename1.ui.Container)cmp, ((Boolean)aValue).booleanValue());
                                }
                                properties.repaint();
                                return;

                            case PROPERTY_NEXT_FORM:
                                cmps[0].putClientProperty("%next_form%", aValue);
                                properties.repaint();
                                return;

                            case PROPERTY_BASE_FORM:
                                cmps[0].putClientProperty("%base_form%", aValue);
                                properties.repaint();
                                return;

                            case PROPERTY_LAYOUT_CONSTRAINT:
                                com.codename1.ui.Container parent = cmps[0].getParent();
                                int index = parent.getComponentIndex(cmps[0]);
                                if(parent.getLayout() instanceof com.codename1.ui.layouts.BorderLayout) {
                                    Object oldConstraint = parent.getLayout().getComponentConstraint(cmps[0]);
                                    removeComponentSync(parent, cmps[0]);
                                    for(int iter = 0 ; iter < parent.getComponentCount() ; iter++) {
                                        com.codename1.ui.Component current = parent.getComponentAt(iter);
                                        Object constraint = parent.getLayout().getComponentConstraint(current);
                                        if(constraint != null && constraint.equals(aValue)) {
                                            removeComponentSync(parent, current);
                                            parent.addComponent(iter, oldConstraint, current);
                                            break;
                                        }
                                    }
                                } else {
                                    // for a table layout we want to remove and re-add all the components on
                                    // a constraint change to allow the table to reflow properly
                                    com.codename1.ui.Container cnt = (com.codename1.ui.Container)cmps[0].getParent();
                                    if(cnt instanceof com.codename1.ui.Form) {
                                        cnt = ((com.codename1.ui.Form)cnt).getContentPane();
                                    }
                                    List<com.codename1.ui.Component> cmpsList = new ArrayList<com.codename1.ui.Component>();
                                    List cons = new ArrayList();
                                    for(int iter = 0 ; iter < cnt.getComponentCount() ; iter++) {
                                        com.codename1.ui.Component currentCmp = cnt.getComponentAt(iter);
                                        cmpsList.add(currentCmp);
                                        if(currentCmp == cmps[0]) {
                                            cons.add(aValue);
                                        } else {
                                            cons.add(cnt.getLayout().getComponentConstraint(currentCmp));
                                        }
                                    }
                                    cnt.removeAll();
                                    for(int iter = 0 ; iter < cmpsList.size() ; iter++) {
                                        com.codename1.ui.Component currentCmp = cmpsList.get(iter);
                                        Object constraint = cons.get(iter);

                                        if(constraint != null) {
                                            cnt.addComponent(constraint, currentCmp);
                                        } else {
                                            cnt.addComponent(currentCmp);
                                        }
                                    }
                                    properties.repaint();
                                    return;
                                }
                                try {
                                    parent.addComponent(index, aValue, cmps[0]);
                                } catch(Exception err) {
                                    removeComponentSync(parent, cmps[0]);
                                    err.printStackTrace();
                                    parent.addComponent(index, oldValue, cmps[0]);
                                    JOptionPane.showMessageDialog(UserInterfaceEditor.this, "Error positioning component: " + err,
                                            "Error", JOptionPane.ERROR_MESSAGE);
                                }
                                properties.repaint();
                                return;

                            case PROPERTY_LIST_ITEMS:
                                ((com.codename1.ui.List)cmps[0]).setModel(new com.codename1.ui.list.DefaultListModel((Object[])aValue));
                                properties.repaint();
                                return;

                            case PROPERTY_CUSTOM:
                                String customName = propertyNames.get(rowIndex);
                                setCustomPropertyModified(cmps[0], customName);
                                String errorCode = cmps[0].setPropertyValue(customName, aValue);
                                if(errorCode != null) {
                                    JOptionPane.showMessageDialog(UserInterfaceEditor.this, errorCode, "Error", JOptionPane.ERROR_MESSAGE);
                                }
                                properties.repaint();
                                return;
                        }

                        for(com.codename1.ui.Component cmp : cmps) {
                            invoke(propertySetters.get(rowIndex), cmp, new Object[]{ aValue });
                        }

                        if(propertyId == PROPERTY_LAYOUT) {
                            if(aValue instanceof com.codename1.ui.layouts.BorderLayout) {
                                // assign a layout constraint to all the components and revalidate
                                // try to maintain existing constraints if some of the components already had them
                                com.codename1.ui.Container cnt = (com.codename1.ui.Container)cmps[0];
                                if(cnt instanceof com.codename1.ui.Form) {
                                    cnt = ((com.codename1.ui.Form)cnt).getContentPane();
                                }
                                List availableConstraints = new ArrayList();
                                availableConstraints.add(com.codename1.ui.layouts.BorderLayout.NORTH);
                                availableConstraints.add(com.codename1.ui.layouts.BorderLayout.SOUTH);
                                availableConstraints.add(com.codename1.ui.layouts.BorderLayout.CENTER);
                                availableConstraints.add(com.codename1.ui.layouts.BorderLayout.EAST);
                                availableConstraints.add(com.codename1.ui.layouts.BorderLayout.WEST);
                                List<com.codename1.ui.Component> cmps = new ArrayList<com.codename1.ui.Component>();
                                for(int iter = 0 ; iter < cnt.getComponentCount() ; iter++) {
                                    Object o = cnt.getLayout().getComponentConstraint(cnt.getComponentAt(iter));
                                    if(o != null && availableConstraints.contains(o)) {
                                        availableConstraints.remove(o);
                                    } else {
                                        cmps.add(cnt.getComponentAt(iter));
                                    }
                                }
                                for(com.codename1.ui.Component current : cmps) {
                                    int i = cnt.getComponentIndex(current);
                                    removeComponentSync(cnt, current);
                                    cnt.addComponent(i, availableConstraints.get(0), current);
                                    availableConstraints.remove(0);
                                }
                            } 
                        }

                        cmps[0].getComponentForm().revalidate();
                        refreshLocaleTable();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    } finally {
                        if(propertyIds.get(rowIndex).intValue() == PROPERTY_NAME) {
                            ((ComponentHierarchyModel)componentHierarchy.getModel()).fireTreeStructureChanged(new TreeModelEvent(componentHierarchy.getModel(), new TreePath(componentHierarchy.getModel().getRoot())));
                            expandAll(componentHierarchy);
                        }
                        uiPreview.repaint();
                        saveUI();
                    }
                    properties.repaint();
                }
            });
        }

        public void addTableModelListener(TableModelListener l) {
            listener.add(l);
        }

        public void removeTableModelListener(TableModelListener l) {
            listener.remove(l);
        }

    }


    class ComponentHierarchyModel implements TreeModel {
        private List<TreeModelListener> listeners = new ArrayList<TreeModelListener>();
        private com.codename1.ui.Container codenameOneContainer;

        public ComponentHierarchyModel(com.codename1.ui.Container codenameOneContainer) {
            this.codenameOneContainer = codenameOneContainer;
        }

        public Object getRoot() {
            return codenameOneContainer;
        }

        public Object getChild(Object parent, int index) {
            if(parent instanceof com.codename1.ui.Form) {
                return ((com.codename1.ui.Form)parent).getContentPane().getComponentAt(index);
            }
            if(parent instanceof com.codename1.ui.Tabs) {
                return ((com.codename1.ui.Tabs)parent).getTabComponentAt(index);
            }
            return ((com.codename1.ui.Container)parent).getComponentAt(index);
        }

        public int getChildCount(Object parent) {
            if(parent instanceof com.codename1.ui.Form) {
                return ((com.codename1.ui.Form)parent).getContentPane().getComponentCount();
            }
            if(parent instanceof com.codename1.ui.Tabs) {
                return ((com.codename1.ui.Tabs)parent).getTabCount();
            }
            if(parent instanceof com.codename1.ui.Container) {
                return ((com.codename1.ui.Container)parent).getComponentCount();
            }
            return 0;
        }

        public boolean isLeaf(Object node) {
            return !(isActualContainer(node));
        }

        public void valueForPathChanged(TreePath path, Object newValue) {
            ((com.codename1.ui.Component)path.getLastPathComponent()).setName((String)newValue);
            setPropertyModified((com.codename1.ui.Component)path.getLastPathComponent(), PROPERTY_NAME);
            ((ComponentPropertyEditorModel)properties.getModel()).setComponents(new com.codename1.ui.Component[] {(com.codename1.ui.Component)path.getLastPathComponent()});
            saveUI();
        }

        public int getIndexOfChild(Object parent, Object child) {
            com.codename1.ui.Container p = (com.codename1.ui.Container)parent;
            if(parent instanceof com.codename1.ui.Form) {
                p = ((com.codename1.ui.Form)parent).getContentPane();
            }
            if(parent instanceof com.codename1.ui.Tabs) {
                p = ((com.codename1.ui.Tabs)parent).getContentPane();
            }
            for(int iter = 0 ; iter < p.getComponentCount() ; iter++) {
                if(p.getComponentAt(iter) == child) {
                    return iter;
                }
            }
            return -1;
        }

        public void fireTreeStructureChanged(TreeModelEvent e) {
            TreePath[] paths = componentHierarchy.getSelectionPaths();
            for(TreeModelListener l : listeners) {
                l.treeStructureChanged(e);
            }
            componentHierarchy.setSelectionPaths(paths);
        }

        public void fireTreeNodesInserted(TreeModelEvent e) {
            for(TreeModelListener l : listeners) {
                l.treeNodesInserted(e);
            }
        }

        public void addTreeModelListener(TreeModelListener l) {
            listeners.add(l);
        }

        public void removeTreeModelListener(TreeModelListener l) {
            listeners.remove(l);
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

        arrangeLeftRight = new javax.swing.JSplitPane();
        jSplitPane2 = new javax.swing.JSplitPane();
        jSplitPane1 = new javax.swing.JSplitPane();
        leftSidePanel = new javax.swing.JPanel();
        jScrollPane6 = new javax.swing.JScrollPane();
        componentHierarchy = new org.jdesktop.swingx.JXTree();
        jPanel2 = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        propertyAndEventTabs = new javax.swing.JTabbedPane();
        jScrollPane2 = new javax.swing.JScrollPane();
        properties = new javax.swing.JTable();
        jPanel4 = new javax.swing.JPanel();
        bindActionEvent = new javax.swing.JButton();
        bindOnCreate = new javax.swing.JButton();
        bindBeforeShow = new javax.swing.JButton();
        bindPostShow = new javax.swing.JButton();
        bindExitForm = new javax.swing.JButton();
        bindListModel = new javax.swing.JButton();
        whyAreEventsDisabled = new org.jdesktop.swingx.JXButton();
        jPanel3 = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        localizeTable = new javax.swing.JTable();
        resourceBundle = new javax.swing.JComboBox();
        jPanel9 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        simulateDevice = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        initialForm = new javax.swing.JButton();
        jScrollPane5 = new javax.swing.JScrollPane();
        help = new javax.swing.JTextPane();
        palettePanel = new javax.swing.JScrollPane();
        componentPalette = new javax.swing.JPanel();
        jTabbedPane1 = new JOutlookBar();
        jPanel1 = new javax.swing.JPanel();
        coreComponents = new javax.swing.JPanel();
        codenameOneLabel = new javax.swing.JButton();
        codenameOneSpanLabel = new javax.swing.JButton();
        codenameOneButton = new javax.swing.JButton();
        codenameOneMultiButton = new javax.swing.JButton();
        codenameOneSpanButton = new javax.swing.JButton();
        codenameOneCheckBox = new javax.swing.JButton();
        codenameOneRadioButton = new javax.swing.JButton();
        codenameOneComboBox = new javax.swing.JButton();
        codenameOneList = new javax.swing.JButton();
        codenameOneMultiList = new javax.swing.JButton();
        codenameOneTextArea = new javax.swing.JButton();
        codenameOneTextField = new javax.swing.JButton();
        codenameOneAutoCompleteTextField = new javax.swing.JButton();
        codenameOneSlider = new javax.swing.JButton();
        codenameOneContainer = new javax.swing.JButton();
        codenameOneTabs = new javax.swing.JButton();
        embedContainer = new javax.swing.JButton();
        codenameOneCalendar = new javax.swing.JButton();
        jPanel6 = new javax.swing.JPanel();
        codenameOneExtraComponents = new javax.swing.JPanel();
        codenameOneTable = new javax.swing.JButton();
        codenameOneTree = new javax.swing.JButton();
        codenameOneHTMLComponent = new javax.swing.JButton();
        codenameOneContainerList = new javax.swing.JButton();
        codenameOneComponentGroup = new javax.swing.JButton();
        codenameOneMediaPlayer = new javax.swing.JButton();
        codenameOneNumericSpinner = new javax.swing.JButton();
        codenameOneDateSpinner = new javax.swing.JButton();
        codenameOneTimeSpinner = new javax.swing.JButton();
        codenameOneDateTimeSpinner = new javax.swing.JButton();
        codenameOneGenericSpinner = new javax.swing.JButton();
        codenameOneLikeButton = new javax.swing.JButton();
        codenameOneInfiniteProgress = new javax.swing.JButton();
        codenameOneAds = new javax.swing.JButton();
        codenameOneMap = new javax.swing.JButton();
        codenameOneShare = new javax.swing.JButton();
        codenameOneOnOffSwitch = new javax.swing.JButton();
        codenameOneImageViewer = new javax.swing.JButton();
        jPanel7 = new javax.swing.JPanel();
        codenameOneIOComponents = new javax.swing.JPanel();
        rssReader = new javax.swing.JButton();
        fileTree = new javax.swing.JButton();
        jPanel8 = new javax.swing.JPanel();
        userComponents = new javax.swing.JPanel();
        uiPreview = new javax.swing.JPanel();

        FormListener formListener = new FormListener();

        setLayout(new java.awt.BorderLayout());

        arrangeLeftRight.setDividerLocation(450);
        arrangeLeftRight.setResizeWeight(0.5);
        arrangeLeftRight.setName("arrangeLeftRight"); // NOI18N
        arrangeLeftRight.setOneTouchExpandable(true);

        jSplitPane2.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        jSplitPane2.setResizeWeight(0.5);
        jSplitPane2.setName("jSplitPane2"); // NOI18N
        jSplitPane2.setOneTouchExpandable(true);

        jSplitPane1.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        jSplitPane1.setName("jSplitPane1"); // NOI18N
        jSplitPane1.setOneTouchExpandable(true);

        leftSidePanel.setName("leftSidePanel"); // NOI18N
        leftSidePanel.setLayout(new java.awt.BorderLayout());

        jScrollPane6.setName("jScrollPane6"); // NOI18N

        componentHierarchy.setDragEnabled(true);
        componentHierarchy.setName("componentHierarchy"); // NOI18N
        jScrollPane6.setViewportView(componentHierarchy);

        leftSidePanel.add(jScrollPane6, java.awt.BorderLayout.CENTER);

        jPanel2.setName("jPanel2"); // NOI18N
        jPanel2.setLayout(new java.awt.BorderLayout());

        jPanel5.setName("jPanel5"); // NOI18N
        jPanel5.setLayout(new java.awt.BorderLayout());
        jPanel2.add(jPanel5, java.awt.BorderLayout.CENTER);

        leftSidePanel.add(jPanel2, java.awt.BorderLayout.NORTH);

        jSplitPane1.setBottomComponent(leftSidePanel);

        propertyAndEventTabs.setName("propertyAndEventTabs"); // NOI18N

        jScrollPane2.setName("jScrollPane2"); // NOI18N

        properties.setName("properties"); // NOI18N
        properties.addMouseListener(formListener);
        jScrollPane2.setViewportView(properties);

        propertyAndEventTabs.addTab("Properties", jScrollPane2);

        jPanel4.setName("jPanel4"); // NOI18N

        bindActionEvent.setText("Action Event");
        bindActionEvent.setName("bindActionEvent"); // NOI18N
        bindActionEvent.addActionListener(formListener);

        bindOnCreate.setText("onCreate");
        bindOnCreate.setName("bindOnCreate"); // NOI18N
        bindOnCreate.addActionListener(formListener);

        bindBeforeShow.setText("Before Show");
        bindBeforeShow.setName("bindBeforeShow"); // NOI18N
        bindBeforeShow.addActionListener(formListener);

        bindPostShow.setText("Post Show");
        bindPostShow.setName("bindPostShow"); // NOI18N
        bindPostShow.addActionListener(formListener);

        bindExitForm.setText("Exit Form");
        bindExitForm.setName("bindExitForm"); // NOI18N
        bindExitForm.addActionListener(formListener);

        bindListModel.setText("List Model");
        bindListModel.setName("bindListModel"); // NOI18N
        bindListModel.addActionListener(formListener);

        whyAreEventsDisabled.setText("Why Are Events Disabled?");
        whyAreEventsDisabled.setName("whyAreEventsDisabled"); // NOI18N
        whyAreEventsDisabled.addActionListener(formListener);

        org.jdesktop.layout.GroupLayout jPanel4Layout = new org.jdesktop.layout.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(bindOnCreate, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 558, Short.MAX_VALUE)
                    .add(bindActionEvent, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 558, Short.MAX_VALUE)
                    .add(bindBeforeShow, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 558, Short.MAX_VALUE)
                    .add(bindPostShow, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 558, Short.MAX_VALUE)
                    .add(bindExitForm, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 558, Short.MAX_VALUE)
                    .add(bindListModel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 558, Short.MAX_VALUE)
                    .add(whyAreEventsDisabled, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 407, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel4Layout.createSequentialGroup()
                .add(whyAreEventsDisabled, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(bindActionEvent)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(bindOnCreate)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(bindBeforeShow)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(bindPostShow)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(bindExitForm)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(bindListModel)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        propertyAndEventTabs.addTab("Events", jPanel4);

        jPanel3.setName("jPanel3"); // NOI18N
        jPanel3.setLayout(new java.awt.BorderLayout());

        jScrollPane4.setName("jScrollPane4"); // NOI18N

        localizeTable.setName("localizeTable"); // NOI18N
        jScrollPane4.setViewportView(localizeTable);

        jPanel3.add(jScrollPane4, java.awt.BorderLayout.CENTER);

        resourceBundle.setToolTipText("Resource Bundle");
        resourceBundle.setName("resourceBundle"); // NOI18N
        resourceBundle.addActionListener(formListener);
        jPanel3.add(resourceBundle, java.awt.BorderLayout.PAGE_START);

        propertyAndEventTabs.addTab("Localize", jPanel3);

        jPanel9.setName("jPanel9"); // NOI18N

        jLabel3.setText("Simulate Device");
        jLabel3.setName("jLabel3"); // NOI18N

        simulateDevice.setText("...");
        simulateDevice.setName("simulateDevice"); // NOI18N
        simulateDevice.addActionListener(formListener);

        jLabel5.setText("Set As Main Form");
        jLabel5.setName("jLabel5"); // NOI18N

        initialForm.setText("Initial Form");
        initialForm.setToolTipText("<html>Makes this form into the first form shown<br>when the application loads if applicable");
        initialForm.setName("initialForm"); // NOI18N
        initialForm.addActionListener(formListener);

        org.jdesktop.layout.GroupLayout jPanel9Layout = new org.jdesktop.layout.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(
            jPanel9Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel9Layout.createSequentialGroup()
                .add(jPanel9Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel5)
                    .add(jLabel3))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jPanel9Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                    .add(simulateDevice, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(initialForm, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .add(280, 280, 280))
        );
        jPanel9Layout.setVerticalGroup(
            jPanel9Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel9Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel9Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel3)
                    .add(simulateDevice))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel9Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel5)
                    .add(initialForm))
                .addContainerGap(175, Short.MAX_VALUE))
        );

        propertyAndEventTabs.addTab("Preview & Misc", jPanel9);

        jScrollPane5.setName("jScrollPane5"); // NOI18N

        help.setContentType("text/html"); // NOI18N
        help.setEditable(false);
        help.setText("<html>\r\n  <head>\r\n\r\n  </head>\r\n  <body>\r\n    <p style=\"margin-top: 0\">\r\n      \rTo use the GUI builder drag components from the component palette at the bottom to either\nthe component tree on the right or into the actual UI. Components are arranged in a hierarchy \nwithin containers, you can nest containers and components to create all forms of elaborate UI's.\nTo determine how components are arranged within a Container you need to determine the layout\nmanager of the container (click the layout field in the Properties section while a container is \nselected, there is more documentation on layouts there).\n    </p>\r\n    <p>\n      Attributes of a component can be customized when its selected in the tree or in the UI by \nediting the properties tab content. Components can be dragged and rearranged both within the\nUI preview and within the tree, a right click (meta-click) menu also exists to delete/copy/paste etc.\nthe existing components. To change the appearance of a component you need to work with\na theme, to apply a change only to a specific component you can change its UIID attribute and\nedit that UIID in the theme (read more about UIID's in the theme section).\n    </p>\n    <p>\n      Navigation between forms in the GUI builder is possible with commands, in order to view the resulting UI \nthe theme can be selected (select the UI from the Preview Options area). In order for events to be mappable\n via the GUI builder use the generate netbeans project functionality in the Application menu.\n    </p>\n  </body>\r\n</html>\r\n"); // NOI18N
        help.setName("help"); // NOI18N
        jScrollPane5.setViewportView(help);

        propertyAndEventTabs.addTab("Help", jScrollPane5);

        jSplitPane1.setTopComponent(propertyAndEventTabs);

        jSplitPane2.setRightComponent(jSplitPane1);

        palettePanel.setMaximumSize(new java.awt.Dimension(32766, 32766));
        palettePanel.setMinimumSize(new java.awt.Dimension(50, 50));
        palettePanel.setName("palettePanel"); // NOI18N

        componentPalette.setName("componentPalette"); // NOI18N
        componentPalette.addMouseListener(formListener);
        componentPalette.setLayout(new java.awt.BorderLayout());

        jTabbedPane1.setName("jTabbedPane1"); // NOI18N

        jPanel1.setName("jPanel1"); // NOI18N
        jPanel1.setLayout(new java.awt.BorderLayout());

        coreComponents.setName("coreComponents"); // NOI18N
        coreComponents.setLayout(new java.awt.GridLayout(9, 2));

        codenameOneLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jdesktop/swingx/resources/JXLabel32.png"))); // NOI18N
        codenameOneLabel.setText("Label");
        codenameOneLabel.setToolTipText("<html><body><b>Label</b><br> <p>Can represent either text or an image or both, the image will not be scaled and its easy to define<br> the arrangement between the list and the icon of the label. A label spans only one line and when<br> it has no more available space it ends with \"...\" by default (this can be disabled). A label supports<br> advanced features such as tickering but that is only enabled automatically when it receives focus,<br> the label is not focusable by default.</p> </body> </html>"); // NOI18N
        codenameOneLabel.setBorder(null);
        codenameOneLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        codenameOneLabel.setName("codenameOneLabel"); // NOI18N
        codenameOneLabel.addActionListener(formListener);
        coreComponents.add(codenameOneLabel);

        codenameOneSpanLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jdesktop/swingx/resources/JXLabel32.png"))); // NOI18N
        codenameOneSpanLabel.setText("Span Label");
        codenameOneSpanLabel.setToolTipText("<html><body><b>Label</b><br> <p>A label that automatically breaks lines when running out of space<br>\nNotice that this is expensive and more complex to layout hence it is<br>\nrecommended to use a Label unless you really need this functionality.</p> </body> </html>"); // NOI18N
        codenameOneSpanLabel.setBorder(null);
        codenameOneSpanLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        codenameOneSpanLabel.setName("codenameOneSpanLabel"); // NOI18N
        codenameOneSpanLabel.addActionListener(formListener);
        coreComponents.add(codenameOneSpanLabel);

        codenameOneButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jdesktop/swingx/resources/JXButton32.png"))); // NOI18N
        codenameOneButton.setText("Button");
        codenameOneButton.setToolTipText("<html><body><b>Button</b><br> \n<p>\nIs derived from Label and thus has all its capabilities, besides those button also offers the<br>\nability to be clicked to perform an action or command. It is a focusable component by default<br>\nand it includes additional icons for different states.<br>\nButtons have a pressed style (when pressed) and by default have a border around them.\n</p> </body> </html>"); // NOI18N
        codenameOneButton.setBorder(null);
        codenameOneButton.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        codenameOneButton.setName("codenameOneButton"); // NOI18N
        codenameOneButton.addActionListener(formListener);
        coreComponents.add(codenameOneButton);

        codenameOneMultiButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jdesktop/swingx/resources/JXButton32.png"))); // NOI18N
        codenameOneMultiButton.setText("Multi-Button");
        codenameOneMultiButton.setToolTipText("<html><body><b>MultiButton</b><br> \n<p>\nA Complex button like component allowing multi-line<br>\ninput as well as elaborate functionality. It is based on <br>\na container with a lead component within.</p> </body> </html>"); // NOI18N
        codenameOneMultiButton.setBorder(null);
        codenameOneMultiButton.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        codenameOneMultiButton.setName("codenameOneMultiButton"); // NOI18N
        codenameOneMultiButton.addActionListener(formListener);
        coreComponents.add(codenameOneMultiButton);

        codenameOneSpanButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jdesktop/swingx/resources/JXButton32.png"))); // NOI18N
        codenameOneSpanButton.setText("Span-Button");
        codenameOneSpanButton.setToolTipText("<html><body><b>SpanButton</b><br> \n<p>\nA button that can span multiple lines<br>\nsimilarly to a text area component.</p> </body> </html>"); // NOI18N
        codenameOneSpanButton.setBorder(null);
        codenameOneSpanButton.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        codenameOneSpanButton.setName("codenameOneSpanButton"); // NOI18N
        codenameOneSpanButton.addActionListener(formListener);
        coreComponents.add(codenameOneSpanButton);

        codenameOneCheckBox.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jdesktop/swingx/resources/placeholder32.png"))); // NOI18N
        codenameOneCheckBox.setText("Check Box");
        codenameOneCheckBox.setToolTipText("<html><body><b>CheckBox</b><br> \n<p>\nIs derived from Button and thus has all its capabilities, besides those a checkbox also has a<br>\ncheckmark next to it by default and has a state indicating whether it is selected or not.<br>\nA checkbox can be marked as a toggle button at which point it will not draw the checkbox<br>\nmark.<br>\nThe graphics for the checkbox drawing can be replaced in the theme using the constants tab.\n</p> </body> </html>"); // NOI18N
        codenameOneCheckBox.setBorder(null);
        codenameOneCheckBox.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        codenameOneCheckBox.setName("codenameOneCheckBox"); // NOI18N
        codenameOneCheckBox.addActionListener(formListener);
        coreComponents.add(codenameOneCheckBox);

        codenameOneRadioButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jdesktop/swingx/resources/placeholder32.png"))); // NOI18N
        codenameOneRadioButton.setText("Radio Button");
        codenameOneRadioButton.setToolTipText("<html><body><b>RadioButton</b><br> \n<p>\nIdentical to checkbox (derived from Button) with a somewhat different type of marking drawn next<br>\nto it. The major difference is that a radio button belongs to a group (specified by name in the<br>\nproperties) this group indicates exclusivity. Only one member of the radio group may be selected<br>\nat once and when another member gets selected an old member loses selection.\n</p> </body> </html>"); // NOI18N
        codenameOneRadioButton.setBorder(null);
        codenameOneRadioButton.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        codenameOneRadioButton.setName("codenameOneRadioButton"); // NOI18N
        codenameOneRadioButton.addActionListener(formListener);
        coreComponents.add(codenameOneRadioButton);

        codenameOneComboBox.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jdesktop/swingx/resources/JXCollapsiblePane32.png"))); // NOI18N
        codenameOneComboBox.setText("Combo Box");
        codenameOneComboBox.setToolTipText("<html><body><b>ComboBox</b><br> \n<p>\nWhile a combo box is based on a list and carries most of its unique features and abilities its<br>\npurpose and appearance are often quite different. A combo box allows picking one element<br>\nand its UI spans a single row with a popup that opens to display the options.<br>\nNotice that the arrow next to the combo box is customizable via the theme constants.\n</p> </body> </html>"); // NOI18N
        codenameOneComboBox.setBorder(null);
        codenameOneComboBox.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        codenameOneComboBox.setName("codenameOneComboBox"); // NOI18N
        codenameOneComboBox.addActionListener(formListener);
        coreComponents.add(codenameOneComboBox);

        codenameOneList.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jdesktop/swingx/resources/JXTaskPaneContainer32.png"))); // NOI18N
        codenameOneList.setText("List");
        codenameOneList.setToolTipText("<html><body><b>List</b><br> \n<p>\nOne of the most elaborate components in Codename One, the list contains arbitrary items which can<br>\nbe \"rendered\" in unique ways. A list can be laid out horizontally or vertically and its content<br>\ncan be easily generated programmatically or via the model in the properties table.<br>\nTo customize the way the list shows its elements you need to set up a renderer for the list.\n</p> </body> </html>"); // NOI18N
        codenameOneList.setBorder(null);
        codenameOneList.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        codenameOneList.setName("codenameOneList"); // NOI18N
        codenameOneList.addActionListener(formListener);
        coreComponents.add(codenameOneList);

        codenameOneMultiList.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jdesktop/swingx/resources/JXTaskPaneContainer32.png"))); // NOI18N
        codenameOneMultiList.setText("Multi-List");
        codenameOneMultiList.setToolTipText("<html><body><b>Multi-List</b><br> \n<p>\nA list component with a multi-button renderer by default aleviating the need of setting a renderer.<br>\n</p> </body> </html>"); // NOI18N
        codenameOneMultiList.setBorder(null);
        codenameOneMultiList.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        codenameOneMultiList.setName("codenameOneMultiList"); // NOI18N
        codenameOneMultiList.addActionListener(formListener);
        coreComponents.add(codenameOneMultiList);

        codenameOneTextArea.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jdesktop/swingx/resources/placeholder32.png"))); // NOI18N
        codenameOneTextArea.setText("Text Area");
        codenameOneTextArea.setToolTipText("<html><body><b>TextArea</b><br> \n<p>\nAllows viewing text that potentially spans multiple lines and can potentially resize automatically<br>\nbased on the amount of text. TextArea optionally allows editing the text but only by going<br>\nto a separate native editor to complete the editing. This can have some advantages on most<br>\ndevices since text input is unique and complex.\n</p> </body> </html>"); // NOI18N
        codenameOneTextArea.setBorder(null);
        codenameOneTextArea.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        codenameOneTextArea.setName("codenameOneTextArea"); // NOI18N
        codenameOneTextArea.addActionListener(formListener);
        coreComponents.add(codenameOneTextArea);

        codenameOneTextField.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jdesktop/swingx/resources/placeholder32.png"))); // NOI18N
        codenameOneTextField.setText("Text Field");
        codenameOneTextField.setToolTipText("<html><body><b>TextField</b><br> \n<p>\nBased on the text area but designed for \"in place\" editing with a cursor and everything involved<br>\nin that. The text field is far more customizable than the text area but also far more complex.\n</p> </body> </html>"); // NOI18N
        codenameOneTextField.setBorder(null);
        codenameOneTextField.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        codenameOneTextField.setName("codenameOneTextField"); // NOI18N
        codenameOneTextField.addActionListener(formListener);
        coreComponents.add(codenameOneTextField);

        codenameOneAutoCompleteTextField.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jdesktop/swingx/resources/placeholder32.png"))); // NOI18N
        codenameOneAutoCompleteTextField.setText("Auto Complete");
        codenameOneAutoCompleteTextField.setToolTipText("<html><body><b>Auto Complete TextField</b><br> \n<p>\nA TextField that shows a completion popup as you type into it and allows you to pick from a<br>\nset of entries\n</p> </body> </html>"); // NOI18N
        codenameOneAutoCompleteTextField.setBorder(null);
        codenameOneAutoCompleteTextField.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        codenameOneAutoCompleteTextField.setName("codenameOneAutoCompleteTextField"); // NOI18N
        codenameOneAutoCompleteTextField.addActionListener(formListener);
        coreComponents.add(codenameOneAutoCompleteTextField);

        codenameOneSlider.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jdesktop/swingx/resources/JXTitledSeparator32.png"))); // NOI18N
        codenameOneSlider.setText("Slider");
        codenameOneSlider.setToolTipText("<html><body><b>Slider</b><br> \n<p>\nA slider provides a bar that can fill up in a similar way to common UI's used for progress<br>\nindication, volume control etc.<br>\nA slider can be editable (for cases such as volume control) or not and it can render progress<br>\npercentage on top. The slider features two separate styles one for the empty slider and<br>\nanother for the full slider. \n</p> </body> </html>"); // NOI18N
        codenameOneSlider.setBorder(null);
        codenameOneSlider.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        codenameOneSlider.setName("codenameOneSlider"); // NOI18N
        codenameOneSlider.addActionListener(formListener);
        coreComponents.add(codenameOneSlider);

        codenameOneContainer.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jdesktop/swingx/resources/JXPanel32.png"))); // NOI18N
        codenameOneContainer.setText("Container");
        codenameOneContainer.setToolTipText("<html><body><b>Container</b><br> \n<p>\nContainer is a component type that contains other components in a layout. Since a container<br>\nitself is a component containers can be easily nested. To customize the way containers arrange<br>\ntheir components the layout property  may be used.\n</p> </body> </html>"); // NOI18N
        codenameOneContainer.setBorder(null);
        codenameOneContainer.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        codenameOneContainer.setName("codenameOneContainer"); // NOI18N
        codenameOneContainer.addActionListener(formListener);
        coreComponents.add(codenameOneContainer);

        codenameOneTabs.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jdesktop/swingx/resources/placeholder32.png"))); // NOI18N
        codenameOneTabs.setText("Tabs");
        codenameOneTabs.setToolTipText("<html><body><b>Tabs</b><br> \n<p>\nTabs is a type of container that arranges the components/containers within it in named tabs<br>\nallowing the user to page or swipe between them. \n</p> </body> </html>"); // NOI18N
        codenameOneTabs.setBorder(null);
        codenameOneTabs.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        codenameOneTabs.setName("codenameOneTabs"); // NOI18N
        codenameOneTabs.addActionListener(formListener);
        coreComponents.add(codenameOneTabs);

        embedContainer.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jdesktop/swingx/resources/JXPanel32-mono.png"))); // NOI18N
        embedContainer.setText("Embed");
        embedContainer.setToolTipText("<html><body><b>EmbeddedContainer</b><br> \n<p>\nAllows embedding a Container defined within resource file into the current UI.\n</p> </body> </html>"); // NOI18N
        embedContainer.setBorder(null);
        embedContainer.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        embedContainer.setName("embedContainer"); // NOI18N
        embedContainer.addActionListener(formListener);
        coreComponents.add(embedContainer);

        codenameOneCalendar.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jdesktop/swingx/resources/placeholder32.png"))); // NOI18N
        codenameOneCalendar.setText("Calendar");
        codenameOneCalendar.setToolTipText("<html><body><b>Calendar</b><br> \n<p>\nThe calendar component contains a month view and ability to select a specific day within<br>\nsaid view. It is very good for feature phones but on touch devices you might be better off<br>\nusing a date spinner.\n</p> </body> </html>"); // NOI18N
        codenameOneCalendar.setBorder(null);
        codenameOneCalendar.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        codenameOneCalendar.setName("codenameOneCalendar"); // NOI18N
        codenameOneCalendar.addActionListener(formListener);
        coreComponents.add(codenameOneCalendar);

        jPanel1.add(coreComponents, java.awt.BorderLayout.NORTH);

        jTabbedPane1.addTab("Core Components", jPanel1);

        jPanel6.setName("jPanel6"); // NOI18N
        jPanel6.setLayout(new java.awt.BorderLayout());

        codenameOneExtraComponents.setName("codenameOneExtraComponents"); // NOI18N
        codenameOneExtraComponents.setLayout(new java.awt.GridLayout(0, 2));

        codenameOneTable.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jdesktop/swingx/resources/placeholder32.png"))); // NOI18N
        codenameOneTable.setText("Table");
        codenameOneTable.setToolTipText("<html><body><b>Table</b><br> \n<p>\nA table component allowing the display and editing of tabular data\n</p> </body> </html>"); // NOI18N
        codenameOneTable.setBorder(null);
        codenameOneTable.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        codenameOneTable.setName("codenameOneTable"); // NOI18N
        codenameOneTable.addActionListener(formListener);
        codenameOneExtraComponents.add(codenameOneTable);

        codenameOneTree.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jdesktop/swingx/resources/placeholder32.png"))); // NOI18N
        codenameOneTree.setText("Tree");
        codenameOneTree.setToolTipText("<html><body><b>Tree</b><br> \n<p>\nAn expandable tree component\n</p> </body> </html>"); // NOI18N
        codenameOneTree.setBorder(null);
        codenameOneTree.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        codenameOneTree.setName("codenameOneTree"); // NOI18N
        codenameOneTree.addActionListener(formListener);
        codenameOneExtraComponents.add(codenameOneTree);

        codenameOneHTMLComponent.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jdesktop/swingx/resources/placeholder32.png"))); // NOI18N
        codenameOneHTMLComponent.setText("Web View");
        codenameOneHTMLComponent.setToolTipText("<html><body><b>WebBrowser</b><br> \n<p>\nBrowser component that allows viewing HTML and optionally uses the platform native browser component if available\n</p> </body> </html>"); // NOI18N
        codenameOneHTMLComponent.setBorder(null);
        codenameOneHTMLComponent.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        codenameOneHTMLComponent.setName("codenameOneHTMLComponent"); // NOI18N
        codenameOneHTMLComponent.addActionListener(formListener);
        codenameOneExtraComponents.add(codenameOneHTMLComponent);

        codenameOneContainerList.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jdesktop/swingx/resources/placeholder32.png"))); // NOI18N
        codenameOneContainerList.setText("ContainerList");
        codenameOneContainerList.setToolTipText("<html><body><b>ContainerList</b><br> \n<p>\nA container that acts like a List, providing a model and renderer approach but doesn't<br>\nenable component addition. This allows mapping list functionality and model to a Container<br>\nand using standard Codename One layout managers to arrange the content of the container.\n</p> </body> </html>"); // NOI18N
        codenameOneContainerList.setBorder(null);
        codenameOneContainerList.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        codenameOneContainerList.setName("codenameOneContainerList"); // NOI18N
        codenameOneContainerList.addActionListener(formListener);
        codenameOneExtraComponents.add(codenameOneContainerList);

        codenameOneComponentGroup.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jdesktop/swingx/resources/placeholder32.png"))); // NOI18N
        codenameOneComponentGroup.setText("Component Group");
        codenameOneComponentGroup.setToolTipText("<html><body><b>ComponentGroup</b><br> \n<p>\nA component group is a container that applies the given UIID to a set of components within it<br>\nwhile appending First/Last/Only to the UIID appropriately. This is useful to create some user interfaces<br>\nwhere the first/last element should have different UIID's (e.g. rounded edges).<br>\n<b>This feature is disabled by default!!!</b> It will have no effect on your code unless you explicitly<br>\ndefine the theme constant ComponentGroupBool, this allows themes that aren't interested in this<br>\neffect to remain much simpler.<br>\n</p> </body> </html>"); // NOI18N
        codenameOneComponentGroup.setBorder(null);
        codenameOneComponentGroup.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        codenameOneComponentGroup.setName("codenameOneComponentGroup"); // NOI18N
        codenameOneComponentGroup.addActionListener(formListener);
        codenameOneExtraComponents.add(codenameOneComponentGroup);

        codenameOneMediaPlayer.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jdesktop/swingx/resources/placeholder32.png"))); // NOI18N
        codenameOneMediaPlayer.setText("Media Player");
        codenameOneMediaPlayer.setToolTipText("<html><body><b>MediaPlayer</b><br> \n<p>\nA video playback component.<br>\n</p> </body> </html>"); // NOI18N
        codenameOneMediaPlayer.setBorder(null);
        codenameOneMediaPlayer.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        codenameOneMediaPlayer.setName("codenameOneMediaPlayer"); // NOI18N
        codenameOneMediaPlayer.addActionListener(formListener);
        codenameOneExtraComponents.add(codenameOneMediaPlayer);

        codenameOneNumericSpinner.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jdesktop/swingx/resources/placeholder32.png"))); // NOI18N
        codenameOneNumericSpinner.setText("Numeric Spinner");
        codenameOneNumericSpinner.setToolTipText("<html><body><b>Numeric Spinner</b><br> \n<p>\nAn iOS like spinner component.<br>\n</p> </body> </html>"); // NOI18N
        codenameOneNumericSpinner.setBorder(null);
        codenameOneNumericSpinner.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        codenameOneNumericSpinner.setName("codenameOneNumericSpinner"); // NOI18N
        codenameOneNumericSpinner.addActionListener(formListener);
        codenameOneExtraComponents.add(codenameOneNumericSpinner);

        codenameOneDateSpinner.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jdesktop/swingx/resources/placeholder32.png"))); // NOI18N
        codenameOneDateSpinner.setText("Date Spinner");
        codenameOneDateSpinner.setToolTipText("<html><body><b>Date Spinner</b><br> \n<p>\nAn iOS like spinner component.<br>\n</p> </body> </html>"); // NOI18N
        codenameOneDateSpinner.setBorder(null);
        codenameOneDateSpinner.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        codenameOneDateSpinner.setName("codenameOneDateSpinner"); // NOI18N
        codenameOneDateSpinner.addActionListener(formListener);
        codenameOneExtraComponents.add(codenameOneDateSpinner);

        codenameOneTimeSpinner.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jdesktop/swingx/resources/placeholder32.png"))); // NOI18N
        codenameOneTimeSpinner.setText("Time Spinner");
        codenameOneTimeSpinner.setToolTipText("<html><body><b>Time Spinner</b><br> \n<p>\nAn iOS like spinner component.<br>\n</p> </body> </html>"); // NOI18N
        codenameOneTimeSpinner.setBorder(null);
        codenameOneTimeSpinner.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        codenameOneTimeSpinner.setName("codenameOneTimeSpinner"); // NOI18N
        codenameOneTimeSpinner.addActionListener(formListener);
        codenameOneExtraComponents.add(codenameOneTimeSpinner);

        codenameOneDateTimeSpinner.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jdesktop/swingx/resources/placeholder32.png"))); // NOI18N
        codenameOneDateTimeSpinner.setText("Date & Time Spinner");
        codenameOneDateTimeSpinner.setToolTipText("<html><body><b>Date &amp; Time Spinner</b><br> \n<p>\nAn iOS like spinner component.<br>\n</p> </body> </html>"); // NOI18N
        codenameOneDateTimeSpinner.setBorder(null);
        codenameOneDateTimeSpinner.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        codenameOneDateTimeSpinner.setName("codenameOneDateTimeSpinner"); // NOI18N
        codenameOneDateTimeSpinner.addActionListener(formListener);
        codenameOneExtraComponents.add(codenameOneDateTimeSpinner);

        codenameOneGenericSpinner.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jdesktop/swingx/resources/placeholder32.png"))); // NOI18N
        codenameOneGenericSpinner.setText("Generic Spinner");
        codenameOneGenericSpinner.setToolTipText("<html><body><b>Generic Spinner</b><br> \n<p>\nA spinner component that shows arbitrary data<br>\n</p> </body> </html>"); // NOI18N
        codenameOneGenericSpinner.setBorder(null);
        codenameOneGenericSpinner.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        codenameOneGenericSpinner.setName("codenameOneGenericSpinner"); // NOI18N
        codenameOneGenericSpinner.addActionListener(formListener);
        codenameOneExtraComponents.add(codenameOneGenericSpinner);

        codenameOneLikeButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jdesktop/swingx/resources/JXButton32.png"))); // NOI18N
        codenameOneLikeButton.setText("Facebook Like Button");
        codenameOneLikeButton.setToolTipText("<html><body><b>LikeButton</b><br> \n<p>\nSimple Facebook like button</p> </body> </html>"); // NOI18N
        codenameOneLikeButton.setBorder(null);
        codenameOneLikeButton.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        codenameOneLikeButton.setName("codenameOneLikeButton"); // NOI18N
        codenameOneLikeButton.addActionListener(formListener);
        codenameOneExtraComponents.add(codenameOneLikeButton);

        codenameOneInfiniteProgress.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jdesktop/swingx/resources/placeholder32.png"))); // NOI18N
        codenameOneInfiniteProgress.setText("Infinite Progress");
        codenameOneInfiniteProgress.setToolTipText("<html><body><b>Infinite Progress</b><br> \n<p>\nA constantly spinning wheel component indicating progress<br>\n</p> </body> </html>"); // NOI18N
        codenameOneInfiniteProgress.setBorder(null);
        codenameOneInfiniteProgress.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        codenameOneInfiniteProgress.setName("codenameOneInfiniteProgress"); // NOI18N
        codenameOneInfiniteProgress.addActionListener(formListener);
        codenameOneExtraComponents.add(codenameOneInfiniteProgress);

        codenameOneAds.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jdesktop/swingx/resources/placeholder32.png"))); // NOI18N
        codenameOneAds.setText("Ads");
        codenameOneAds.setToolTipText("<html><body><b>Ads</b><br> \n<p>\nGeneric pluggable ad component.<br>\n</p> </body> </html>"); // NOI18N
        codenameOneAds.setBorder(null);
        codenameOneAds.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        codenameOneAds.setName("codenameOneAds"); // NOI18N
        codenameOneAds.addActionListener(formListener);
        codenameOneExtraComponents.add(codenameOneAds);

        codenameOneMap.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jdesktop/swingx/resources/placeholder32.png"))); // NOI18N
        codenameOneMap.setText("Map");
        codenameOneMap.setToolTipText("<html><body><b>Map</b><br> \n<p>\nDisplays a user navigatable Map on the screen.<br>\n</p> </body> </html>"); // NOI18N
        codenameOneMap.setBorder(null);
        codenameOneMap.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        codenameOneMap.setName("codenameOneMap"); // NOI18N
        codenameOneMap.addActionListener(formListener);
        codenameOneExtraComponents.add(codenameOneMap);

        codenameOneShare.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jdesktop/swingx/resources/JXButton32.png"))); // NOI18N
        codenameOneShare.setText("Share Button");
        codenameOneShare.setToolTipText("<html><body><b>Share Button</b><br> \n<p>\nSocial share button</p> </body> </html>"); // NOI18N
        codenameOneShare.setBorder(null);
        codenameOneShare.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        codenameOneShare.setName("codenameOneShare"); // NOI18N
        codenameOneShare.addActionListener(formListener);
        codenameOneExtraComponents.add(codenameOneShare);

        codenameOneOnOffSwitch.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jdesktop/swingx/resources/JXButton32.png"))); // NOI18N
        codenameOneOnOffSwitch.setText("On/Off Switch");
        codenameOneOnOffSwitch.setToolTipText("<html><body><b>On/Off Switch</b><br> \n<p>\nAn iOS on/off switch component</p> </body> </html>"); // NOI18N
        codenameOneOnOffSwitch.setBorder(null);
        codenameOneOnOffSwitch.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        codenameOneOnOffSwitch.setName("codenameOneOnOffSwitch"); // NOI18N
        codenameOneOnOffSwitch.addActionListener(formListener);
        codenameOneExtraComponents.add(codenameOneOnOffSwitch);

        codenameOneImageViewer.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jdesktop/swingx/resources/placeholder32.png"))); // NOI18N
        codenameOneImageViewer.setText("Image Viewer");
        codenameOneImageViewer.setToolTipText("<html><body><b>ImageViewer</b><br> \n<p>\nControl allowing the user to view/pinch and optionally swipe between images\n</p> </body> </html>"); // NOI18N
        codenameOneImageViewer.setBorder(null);
        codenameOneImageViewer.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        codenameOneImageViewer.setName("codenameOneImageViewer"); // NOI18N
        codenameOneImageViewer.addActionListener(formListener);
        codenameOneExtraComponents.add(codenameOneImageViewer);

        jPanel6.add(codenameOneExtraComponents, java.awt.BorderLayout.NORTH);

        jTabbedPane1.addTab("Additional Components", jPanel6);

        jPanel7.setName("jPanel7"); // NOI18N
        jPanel7.setLayout(new java.awt.BorderLayout());

        codenameOneIOComponents.setName("codenameOneIOComponents"); // NOI18N
        codenameOneIOComponents.setLayout(new java.awt.GridLayout(0, 2));

        rssReader.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jdesktop/swingx/resources/placeholder32.png"))); // NOI18N
        rssReader.setText("RSS Reader");
        rssReader.setBorder(null);
        rssReader.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        rssReader.setName("rssReader"); // NOI18N
        rssReader.addActionListener(formListener);
        codenameOneIOComponents.add(rssReader);

        fileTree.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jdesktop/swingx/resources/placeholder32.png"))); // NOI18N
        fileTree.setText("File Tree");
        fileTree.setBorder(null);
        fileTree.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        fileTree.setName("fileTree"); // NOI18N
        fileTree.addActionListener(formListener);
        codenameOneIOComponents.add(fileTree);

        jPanel7.add(codenameOneIOComponents, java.awt.BorderLayout.NORTH);

        jTabbedPane1.addTab("Input/Output", jPanel7);

        jPanel8.setName("jPanel8"); // NOI18N
        jPanel8.setLayout(new java.awt.BorderLayout());

        userComponents.setName("userComponents"); // NOI18N
        userComponents.setLayout(new java.awt.GridLayout(0, 2));
        jPanel8.add(userComponents, java.awt.BorderLayout.NORTH);

        jTabbedPane1.addTab("User Defined", jPanel8);

        componentPalette.add(jTabbedPane1, java.awt.BorderLayout.CENTER);

        palettePanel.setViewportView(componentPalette);

        jSplitPane2.setLeftComponent(palettePanel);

        arrangeLeftRight.setLeftComponent(jSplitPane2);

        uiPreview.setMaximumSize(new java.awt.Dimension(2147483646, 2147483646));
        uiPreview.setMinimumSize(new java.awt.Dimension(50, 50));
        uiPreview.setName("uiPreview"); // NOI18N
        uiPreview.setPreferredSize(new java.awt.Dimension(400, 400));
        uiPreview.setLayout(new java.awt.BorderLayout());
        arrangeLeftRight.setRightComponent(uiPreview);

        add(arrangeLeftRight, java.awt.BorderLayout.CENTER);
    }

    // Code for dispatching events from components to event handlers.

    private class FormListener implements java.awt.event.ActionListener, java.awt.event.MouseListener {
        FormListener() {}
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            if (evt.getSource() == bindActionEvent) {
                UserInterfaceEditor.this.bindActionEventActionPerformed(evt);
            }
            else if (evt.getSource() == bindOnCreate) {
                UserInterfaceEditor.this.bindOnCreateActionPerformed(evt);
            }
            else if (evt.getSource() == bindBeforeShow) {
                UserInterfaceEditor.this.bindBeforeShowActionPerformed(evt);
            }
            else if (evt.getSource() == bindPostShow) {
                UserInterfaceEditor.this.bindPostShowActionPerformed(evt);
            }
            else if (evt.getSource() == bindExitForm) {
                UserInterfaceEditor.this.bindExitFormActionPerformed(evt);
            }
            else if (evt.getSource() == bindListModel) {
                UserInterfaceEditor.this.bindListModelActionPerformed(evt);
            }
            else if (evt.getSource() == whyAreEventsDisabled) {
                UserInterfaceEditor.this.whyAreEventsDisabledActionPerformed(evt);
            }
            else if (evt.getSource() == resourceBundle) {
                UserInterfaceEditor.this.resourceBundleActionPerformed(evt);
            }
            else if (evt.getSource() == simulateDevice) {
                UserInterfaceEditor.this.simulateDeviceActionPerformed(evt);
            }
            else if (evt.getSource() == initialForm) {
                UserInterfaceEditor.this.initialFormActionPerformed(evt);
            }
            else if (evt.getSource() == codenameOneLabel) {
                UserInterfaceEditor.this.codenameOneLabelActionPerformed(evt);
            }
            else if (evt.getSource() == codenameOneSpanLabel) {
                UserInterfaceEditor.this.codenameOneSpanLabelActionPerformed(evt);
            }
            else if (evt.getSource() == codenameOneButton) {
                UserInterfaceEditor.this.codenameOneButtonActionPerformed(evt);
            }
            else if (evt.getSource() == codenameOneMultiButton) {
                UserInterfaceEditor.this.codenameOneMultiButtonActionPerformed(evt);
            }
            else if (evt.getSource() == codenameOneSpanButton) {
                UserInterfaceEditor.this.codenameOneSpanButtonActionPerformed(evt);
            }
            else if (evt.getSource() == codenameOneCheckBox) {
                UserInterfaceEditor.this.codenameOneCheckBoxActionPerformed(evt);
            }
            else if (evt.getSource() == codenameOneRadioButton) {
                UserInterfaceEditor.this.codenameOneRadioButtonActionPerformed(evt);
            }
            else if (evt.getSource() == codenameOneComboBox) {
                UserInterfaceEditor.this.codenameOneComboBoxActionPerformed(evt);
            }
            else if (evt.getSource() == codenameOneList) {
                UserInterfaceEditor.this.codenameOneListActionPerformed(evt);
            }
            else if (evt.getSource() == codenameOneMultiList) {
                UserInterfaceEditor.this.codenameOneMultiListActionPerformed(evt);
            }
            else if (evt.getSource() == codenameOneTextArea) {
                UserInterfaceEditor.this.codenameOneTextAreaActionPerformed(evt);
            }
            else if (evt.getSource() == codenameOneTextField) {
                UserInterfaceEditor.this.codenameOneTextFieldActionPerformed(evt);
            }
            else if (evt.getSource() == codenameOneSlider) {
                UserInterfaceEditor.this.codenameOneSliderActionPerformed(evt);
            }
            else if (evt.getSource() == codenameOneContainer) {
                UserInterfaceEditor.this.codenameOneContainerActionPerformed(evt);
            }
            else if (evt.getSource() == codenameOneTabs) {
                UserInterfaceEditor.this.codenameOneTabsActionPerformed(evt);
            }
            else if (evt.getSource() == embedContainer) {
                UserInterfaceEditor.this.embedContainerActionPerformed(evt);
            }
            else if (evt.getSource() == codenameOneCalendar) {
                UserInterfaceEditor.this.codenameOneCalendarActionPerformed(evt);
            }
            else if (evt.getSource() == codenameOneTable) {
                UserInterfaceEditor.this.codenameOneTableActionPerformed(evt);
            }
            else if (evt.getSource() == codenameOneTree) {
                UserInterfaceEditor.this.codenameOneTreeActionPerformed(evt);
            }
            else if (evt.getSource() == codenameOneHTMLComponent) {
                UserInterfaceEditor.this.codenameOneHTMLComponentActionPerformed(evt);
            }
            else if (evt.getSource() == codenameOneContainerList) {
                UserInterfaceEditor.this.codenameOneContainerListActionPerformed(evt);
            }
            else if (evt.getSource() == codenameOneComponentGroup) {
                UserInterfaceEditor.this.codenameOneComponentGroupActionPerformed(evt);
            }
            else if (evt.getSource() == codenameOneMediaPlayer) {
                UserInterfaceEditor.this.codenameOneMediaPlayerActionPerformed(evt);
            }
            else if (evt.getSource() == codenameOneNumericSpinner) {
                UserInterfaceEditor.this.codenameOneNumericSpinnerActionPerformed(evt);
            }
            else if (evt.getSource() == codenameOneDateSpinner) {
                UserInterfaceEditor.this.codenameOneDateSpinnerActionPerformed(evt);
            }
            else if (evt.getSource() == codenameOneTimeSpinner) {
                UserInterfaceEditor.this.codenameOneTimeSpinnerActionPerformed(evt);
            }
            else if (evt.getSource() == codenameOneDateTimeSpinner) {
                UserInterfaceEditor.this.codenameOneDateTimeSpinnerActionPerformed(evt);
            }
            else if (evt.getSource() == codenameOneGenericSpinner) {
                UserInterfaceEditor.this.codenameOneGenericSpinnerActionPerformed(evt);
            }
            else if (evt.getSource() == codenameOneLikeButton) {
                UserInterfaceEditor.this.codenameOneLikeButtonActionPerformed(evt);
            }
            else if (evt.getSource() == codenameOneInfiniteProgress) {
                UserInterfaceEditor.this.codenameOneInfiniteProgressActionPerformed(evt);
            }
            else if (evt.getSource() == codenameOneAds) {
                UserInterfaceEditor.this.codenameOneAdsActionPerformed(evt);
            }
            else if (evt.getSource() == codenameOneMap) {
                UserInterfaceEditor.this.codenameOneMapActionPerformed(evt);
            }
            else if (evt.getSource() == codenameOneShare) {
                UserInterfaceEditor.this.codenameOneShareActionPerformed(evt);
            }
            else if (evt.getSource() == codenameOneOnOffSwitch) {
                UserInterfaceEditor.this.codenameOneOnOffSwitchActionPerformed(evt);
            }
            else if (evt.getSource() == codenameOneImageViewer) {
                UserInterfaceEditor.this.codenameOneImageViewerActionPerformed(evt);
            }
            else if (evt.getSource() == rssReader) {
                UserInterfaceEditor.this.rssReaderActionPerformed(evt);
            }
            else if (evt.getSource() == fileTree) {
                UserInterfaceEditor.this.fileTreeActionPerformed(evt);
            }
            else if (evt.getSource() == codenameOneAutoCompleteTextField) {
                UserInterfaceEditor.this.codenameOneAutoCompleteTextFieldActionPerformed(evt);
            }
        }

        public void mouseClicked(java.awt.event.MouseEvent evt) {
            if (evt.getSource() == properties) {
                UserInterfaceEditor.this.propertiesMouseClicked(evt);
            }
            else if (evt.getSource() == componentPalette) {
                UserInterfaceEditor.this.componentPaletteMouseClicked(evt);
            }
        }

        public void mouseEntered(java.awt.event.MouseEvent evt) {
        }

        public void mouseExited(java.awt.event.MouseEvent evt) {
        }

        public void mousePressed(java.awt.event.MouseEvent evt) {
        }

        public void mouseReleased(java.awt.event.MouseEvent evt) {
        }
    }// </editor-fold>//GEN-END:initComponents

    private void codenameOneLabelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_codenameOneLabelActionPerformed
        if(lockForDragging) {
            lockForDragging = false;
            return;
        }
        addComponentToContainer(new com.codename1.ui.Label("Label"), "Label");
    }//GEN-LAST:event_codenameOneLabelActionPerformed

    private void codenameOneButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_codenameOneButtonActionPerformed
        if(lockForDragging) {
            lockForDragging = false;
            return; 
        }
        addComponentToContainer(new com.codename1.ui.Button("Button"), "Button");
    }//GEN-LAST:event_codenameOneButtonActionPerformed

    private void codenameOneCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_codenameOneCheckBoxActionPerformed
        if(lockForDragging) {
            lockForDragging = false;
            return;
        }
        addComponentToContainer(new com.codename1.ui.CheckBox("CheckBox"), "CheckBox");
    }//GEN-LAST:event_codenameOneCheckBoxActionPerformed

    private void codenameOneRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_codenameOneRadioButtonActionPerformed
        if(lockForDragging) {
            lockForDragging = false;
            return;
        }
        addComponentToContainer(new com.codename1.ui.RadioButton("RadioButton"), "RadioButton");
    }//GEN-LAST:event_codenameOneRadioButtonActionPerformed

    private void codenameOneComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_codenameOneComboBoxActionPerformed
        if(lockForDragging) {
            lockForDragging = false;
            return;
        }
        addComponentToContainer(new com.codename1.ui.ComboBox(new Object[] {"Item 1", "Item 2", "Item 3"}), "ComboBox");
    }//GEN-LAST:event_codenameOneComboBoxActionPerformed

    private void codenameOneListActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_codenameOneListActionPerformed
        if(lockForDragging) {
            lockForDragging = false;
            return;
        }
        addComponentToContainer(new com.codename1.ui.List(new Object[] {"Item 1", "Item 2", "Item 3"}), "List");
    }//GEN-LAST:event_codenameOneListActionPerformed

    private void codenameOneTextAreaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_codenameOneTextAreaActionPerformed
        if(lockForDragging) {
            lockForDragging = false;
            return;
        }
        addComponentToContainer(new com.codename1.ui.TextArea("TextArea"), "TextArea");
    }//GEN-LAST:event_codenameOneTextAreaActionPerformed

    private void codenameOneTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_codenameOneTextFieldActionPerformed
        if(lockForDragging) {
            lockForDragging = false;
            return;
        }
        addComponentToContainer(new com.codename1.ui.TextField("TextField"), "TextField");
    }//GEN-LAST:event_codenameOneTextFieldActionPerformed

    private void codenameOneSliderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_codenameOneSliderActionPerformed
        if(lockForDragging) {
            lockForDragging = false;
            return;
        }
        addComponentToContainer(new com.codename1.ui.Slider(), "Slider");
    }//GEN-LAST:event_codenameOneSliderActionPerformed

    private void codenameOneContainerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_codenameOneContainerActionPerformed
        if(lockForDragging) {
            lockForDragging = false;
            return;
        }
        addComponentToContainer(new com.codename1.ui.Container(), "Container");
    }//GEN-LAST:event_codenameOneContainerActionPerformed

    private void componentPaletteMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_componentPaletteMouseClicked

    }//GEN-LAST:event_componentPaletteMouseClicked

    private void codenameOneTableActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_codenameOneTableActionPerformed
        if(lockForDragging) {
            lockForDragging = false;
            return;
        }
        addComponentToContainer(new com.codename1.ui.table.Table(), "Table");
    }//GEN-LAST:event_codenameOneTableActionPerformed

    private void codenameOneTreeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_codenameOneTreeActionPerformed
        if(lockForDragging) {
            lockForDragging = false;
            return;
        }
        addComponentToContainer(new com.codename1.ui.tree.Tree(), "Tree");
    }//GEN-LAST:event_codenameOneTreeActionPerformed

    private void codenameOneHTMLComponentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_codenameOneHTMLComponentActionPerformed
        if(lockForDragging) {
            lockForDragging = false;
            return;
        }
        addComponentToContainer(new com.codename1.components.WebBrowser(), "WebBrowser");
}//GEN-LAST:event_codenameOneHTMLComponentActionPerformed

    private void codenameOneTabsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_codenameOneTabsActionPerformed
        if(lockForDragging) {
            lockForDragging = false;
            return;
        }
        addComponentToContainer(new com.codename1.ui.Tabs(), "Tabs");
    }//GEN-LAST:event_codenameOneTabsActionPerformed

    private void propertiesMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_propertiesMouseClicked
        if(BaseForm.isRightClick(evt)) {
            AbstractAction clearModified = new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    int row = properties.getSelectedRow();
                    if(row > -1) {
                        int propertyId = ((ComponentPropertyEditorModel)properties.getModel()).getRowId(row);
                        String propertyName = (String)properties.getValueAt(row, 0);
                        clearPropertyModification(getSelectedComponents()[0], propertyId, propertyName);
                        saveUI();
                    }
                }
            };
            clearModified.putValue(AbstractAction.NAME, "Clear Modified Flag");
            JPopupMenu popup = new JPopupMenu();
            popup.add(clearModified);
            popup.show(properties, evt.getX(), evt.getY());
        }
    }//GEN-LAST:event_propertiesMouseClicked

    private boolean hasActionEvent(Class c) {
        for(Method m : c.getMethods()) {
            if(m.getName().equals("addActionListener")) {
                return true;
            }
        }
        return false;
    }

    private boolean hasListModel(Class c) {
        for(Method m : c.getMethods()) {
            if(m.getName().equals("setModel")) {
                Class[] cls = m.getParameterTypes();
                if(cls.length == 1 && cls[0] == com.codename1.ui.list.ListModel.class) {
                    return true;
                }
            }
        }
        return false;
    }

    public static int charIndexToFileLine(int index, String str) {
        int line = 0;
        for(int iter = 0 ; iter < index ; iter++) {
            if(str.charAt(iter) == '\n') {
                line++;
            }
        }
        return line;
    }

    private String getRootComponentName(com.codename1.ui.Component c) {
        while(c.getParent() != null && c.getParent().getName() != null) {
            c = c.getParent();
        }
        return c.getName();
    }

    private boolean hasActionEventCode(com.codename1.ui.Component c) {
        if(projectGeneratorSettings == null || c.getName() == null) {
            return false;
        }
        validateLoadedStateMachineCode();
        if(containerInstance.getName() == null || c.getName() == null) {
            return false;
        }
        String methodName = "on" + ResourceEditorView.normalizeFormName(containerInstance.getName()) +
                            "_" + ResourceEditorView.normalizeFormName(c.getName()) + "Action";
        return userStateMachineCode.indexOf(methodName) > -1;
    }

    private boolean hasFormBeforeCode(com.codename1.ui.Form c) {
        if(projectGeneratorSettings == null || c.getName() == null) {
            return false;
        }
        validateLoadedStateMachineCode();
        String methodName = "before" + ResourceEditorView.normalizeFormName(c.getName());
        return userStateMachineCode.indexOf(methodName) > -1;
    }

    private boolean hasFormPostCode(com.codename1.ui.Form c) {
        if(projectGeneratorSettings == null || c.getName() == null) {
            return false;
        }
        validateLoadedStateMachineCode();
        String methodName = "post" + ResourceEditorView.normalizeFormName(c.getName());
        return userStateMachineCode.indexOf(methodName) > -1;
    }

    private boolean hasFormExitCode(com.codename1.ui.Form c) {
        if(projectGeneratorSettings == null || c.getName() == null) {
            return false;
        }
        validateLoadedStateMachineCode();
        String methodName = "exit" + ResourceEditorView.normalizeFormName(c.getName());
        return userStateMachineCode.indexOf(methodName) > -1;
    }

    private boolean hasContainerPostCode(com.codename1.ui.Container c) {
        if(projectGeneratorSettings == null || c.getName() == null) {
            return false;
        }
        validateLoadedStateMachineCode();
        String methodName = "postContainer" + ResourceEditorView.normalizeFormName(c.getName());
        return userStateMachineCode.indexOf(methodName) > -1;
    }

    private boolean hasContainerBeforeCode(com.codename1.ui.Container c) {
        if(projectGeneratorSettings == null || c.getName() == null) {
            return false;
        }
        validateLoadedStateMachineCode();
        String methodName = "beforeContainer" + ResourceEditorView.normalizeFormName(c.getName());
        return userStateMachineCode.indexOf(methodName) > -1;
    }

    private boolean hasOnCreateCode(com.codename1.ui.Component c) {
        if(projectGeneratorSettings == null || c.getName() == null) {
            return false;
        }
        validateLoadedStateMachineCode();
        String methodName = "onCreate" + ResourceEditorView.normalizeFormName(c.getName());
        return userStateMachineCode.indexOf(methodName) > -1;
    }

    private boolean hasOnListModelCode(com.codename1.ui.Component c) {
        if(projectGeneratorSettings == null || c.getName() == null) {
            return false;
        }
        validateLoadedStateMachineCode();
        String methodName = "initListModel" + ResourceEditorView.normalizeFormName(c.getName());
        return userStateMachineCode.indexOf(methodName) > -1;
    }


    private int getLanguageLevel() {
        try {
            return Integer.parseInt(view.getProjectGeneratorSettings().getProperty("codename1.languageLevel", "1"));
        } catch(Throwable t) {
            return 1;
        }
    }
    
    private void bindActionEventActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bindActionEventActionPerformed
        try {
            File destFile = new File(projectGeneratorSettings.getProperty("userClassAbs"));
            if(!destFile.exists()) {
                JOptionPane.showMessageDialog(this, "File not found:\n" + destFile.getAbsolutePath(), "File Not Found", JOptionPane.ERROR_MESSAGE);
                return;
            }
            DataInputStream input = new DataInputStream(new FileInputStream(destFile));
            byte[] data = new byte[(int)destFile.length()];
            input.readFully(data);
            input.close();
            String fileContent = new String(data);
            int line = -1;
            boolean modified = false;
            for(com.codename1.ui.Component c : getSelectedComponents()) {
                if(!hasActionEvent(c.getClass())) {
                    continue;
                }
                String methodName = "on" + ResourceEditorView.normalizeFormName(containerInstance.getName()) +
                            "_" + ResourceEditorView.normalizeFormName(c.getName()) + "Action";
                int pos = fileContent.indexOf("void " + methodName + "(");
                if(pos > -1) {
                    line = charIndexToFileLine(pos, fileContent);
                } else {
                    modified = true;

                    // assuming one class per file...
                    pos = fileContent.lastIndexOf('}');

                    line = charIndexToFileLine(pos, fileContent) + 4;
                    if(getLanguageLevel() > 4) {
                        fileContent = fileContent.substring(0, pos) +
                                "\n    @Override\n" +
                                "    protected void " + methodName + "(Component c, ActionEvent event) {\n\n" +
                                "    \n" +
                                "    }\n" +
                                fileContent.substring(pos);
                    } else {
                        fileContent = fileContent.substring(0, pos) +
                                "\n    protected void " + methodName + "(Component c, ActionEvent event) {\n" +
                                "        // If the resource file changes the names of components this call will break notifying you that you should fix the code\n" +
                                "        super." + methodName +"(c, event);\n" +
                                "    \n" +
                                "    }\n" +
                                fileContent.substring(pos);
                    }
                }
                if(modified) {
                    Writer output = new FileWriter(destFile);
                    output.write(fileContent);
                    output.close();
                }
                ResourceEditorView.openInIDE(destFile, line);
            }
        } catch(IOException err) {
            err.printStackTrace();
            JOptionPane.showMessageDialog(this, "An IO exception occured: " + err, "IO Exception", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_bindActionEventActionPerformed

    private void onFormBindMethod(String prefix, String args, String argsDefinition) {
        try {
            File destFile = new File(projectGeneratorSettings.getProperty("userClassAbs"));
            if(!destFile.exists()) {
                JOptionPane.showMessageDialog(this, "File not found:\n" + destFile.getAbsolutePath(), "File Not Found", JOptionPane.ERROR_MESSAGE);
                return;
            }
            DataInputStream input = new DataInputStream(new FileInputStream(destFile));
            byte[] data = new byte[(int)destFile.length()];
            input.readFully(data);
            input.close();
            String fileContent = new String(data);
            int line = -1;
            String methodName = prefix + ResourceEditorView.normalizeFormName(containerInstance.getName());
            int pos = fileContent.indexOf("void " + methodName + "(");
            if(pos > -1) {
                line = charIndexToFileLine(pos, fileContent);
            } else {
                // assuming one class per file...
                pos = fileContent.lastIndexOf('}');

                line = charIndexToFileLine(pos, fileContent) + 4;
                if(getLanguageLevel() > 4) {
                    fileContent = fileContent.substring(0, pos) +
                            "\n    @Override\n" +
                            "    protected void " + methodName + "(" + argsDefinition + ") {\n" +
                            "    \n" +
                            "    }\n" +
                            fileContent.substring(pos);
                } else {
                    fileContent = fileContent.substring(0, pos) +
                            "\n    protected void " + methodName + "(" + argsDefinition + ") {\n" +
                            "        // If the resource file changes the names of components this call will break notifying you that you should fix the code\n" +
                            "        super." + methodName +"(" + args + ");\n" +
                            "    \n" +
                            "    }\n" +
                            fileContent.substring(pos);
                }
                Writer output = new FileWriter(destFile);
                output.write(fileContent);
                output.close();
            }
            ResourceEditorView.openInIDE(destFile, line);
        } catch(IOException err) {
            err.printStackTrace();
            JOptionPane.showMessageDialog(this, "An IO exception occured: " + err, "IO Exception", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void bindOnCreateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bindOnCreateActionPerformed
        onFormBindMethod("onCreate", "", "");
    }//GEN-LAST:event_bindOnCreateActionPerformed

    private void bindBeforeShowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bindBeforeShowActionPerformed
        if(containerInstance instanceof com.codename1.ui.Form) {
            onFormBindMethod("before", "f", "Form f");
        } else {
            onFormBindMethod("beforeContainer", "c", "Container c");
        }
    }//GEN-LAST:event_bindBeforeShowActionPerformed

    private void bindPostShowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bindPostShowActionPerformed
        if(containerInstance instanceof com.codename1.ui.Form) {
            onFormBindMethod("post", "f", "Form f");
        } else {
            onFormBindMethod("postContainer", "c", "Container c");
        }
    }//GEN-LAST:event_bindPostShowActionPerformed

    private void rssReaderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rssReaderActionPerformed
        if(lockForDragging) {
            lockForDragging = false;
            return;
        }
        addComponentToContainer(new com.codename1.components.RSSReader(), "RSSReader");
}//GEN-LAST:event_rssReaderActionPerformed

    private void fileTreeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fileTreeActionPerformed
        if(lockForDragging) {
            lockForDragging = false;
            return;
        }
        addComponentToContainer(new com.codename1.components.FileTree(), "FileTree");
}//GEN-LAST:event_fileTreeActionPerformed

    private void embedContainerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_embedContainerActionPerformed
        if(lockForDragging) {
            lockForDragging = false;
            return;
        }
        addComponentToContainer(new EmbeddedContainer(), "EmbeddedContainer");
    }//GEN-LAST:event_embedContainerActionPerformed

    private void resourceBundleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resourceBundleActionPerformed
        LocalizationTableModel t = (LocalizationTableModel)localizeTable.getModel();
        if(t.getRowCount() > 0) {
            // trigger a refresh
            t.setText((String)t.getValueAt(0, 1));
        }
    }//GEN-LAST:event_resourceBundleActionPerformed

    private void bindExitFormActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bindExitFormActionPerformed
        if(containerInstance instanceof com.codename1.ui.Form) {
            onFormBindMethod("exit", "f", "Form f");
        }
    }//GEN-LAST:event_bindExitFormActionPerformed

    private void bindListModelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bindListModelActionPerformed
        try {
            File destFile = new File(projectGeneratorSettings.getProperty("userClassAbs"));
            if(!destFile.exists()) {
                JOptionPane.showMessageDialog(this, "File not found:\n" + destFile.getAbsolutePath(), "File Not Found", JOptionPane.ERROR_MESSAGE);
                return;
            }
            DataInputStream input = new DataInputStream(new FileInputStream(destFile));
            byte[] data = new byte[(int)destFile.length()];
            input.readFully(data);
            input.close();
            String fileContent = new String(data);
            int line = -1;
            boolean modified = false;
            for(com.codename1.ui.Component c : getSelectedComponents()) {
                if(!hasListModel(c.getClass())) {
                    continue;
                }
                String methodName = "initListModel" + ResourceEditorView.normalizeFormName(c.getName());
                int pos = fileContent.indexOf("boolean " + methodName + "(");
                if(pos > -1) {
                    line = charIndexToFileLine(pos, fileContent);
                } else {
                    modified = true;

                    // assuming one class per file...
                    pos = fileContent.lastIndexOf('}');

                    line = charIndexToFileLine(pos, fileContent) + 4;
                    String type = "List";
                    if(c instanceof com.codename1.ui.list.ContainerList) {
                        type = "com.codename1.ui.list.ContainerList";
                    }
                    if(getLanguageLevel() > 4) {
                        fileContent = fileContent.substring(0, pos) +
                                "\n    @Override\n" +
                                "    protected boolean " + methodName + "(" + type + " cmp) {\n" +
                                "        cmp.setModel(new com.codename1.ui.list.DefaultListModel(new String[] {\"Item 1\", \"Item 2\", \"Item 3\"}));\n" +
                                "        return true;\n" +
                                "    }\n" +
                                fileContent.substring(pos);
                    } else {
                        fileContent = fileContent.substring(0, pos) +
                                "\n    protected boolean " + methodName + "(" + type + " cmp) {\n" +
                                "        // If the resource file changes the names of components this call will break notifying you that you should fix the code\n" +
                                "        super." + methodName +"(cmp);\n" +
                                "        cmp.setModel(new com.codename1.ui.list.DefaultListModel(new String[] {\"Item 1\", \"Item 2\", \"Item 3\"}));\n" +
                                "        return true;\n" +
                                "    }\n" +
                                fileContent.substring(pos);
                    }
                }
                if(modified) {
                    Writer output = new FileWriter(destFile);
                    output.write(fileContent);
                    output.close();
                }
                ResourceEditorView.openInIDE(destFile, line);
            }
        } catch(IOException err) {
            err.printStackTrace();
            JOptionPane.showMessageDialog(this, "An IO exception occured: " + err, "IO Exception", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_bindListModelActionPerformed

    private void codenameOneContainerListActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_codenameOneContainerListActionPerformed
        if(lockForDragging) {
            lockForDragging = false;
            return;
        }
        addComponentToContainer(new com.codename1.ui.list.ContainerList(), "ContainerList");
}//GEN-LAST:event_codenameOneContainerListActionPerformed

    private void codenameOneComponentGroupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_codenameOneComponentGroupActionPerformed
        if(lockForDragging) {
            lockForDragging = false;
            return;
        }
        addComponentToContainer(new com.codename1.ui.ComponentGroup(), "ComponentGroup");
}//GEN-LAST:event_codenameOneComponentGroupActionPerformed

private void whyAreEventsDisabledActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_whyAreEventsDisabledActionPerformed
    JOptionPane.showMessageDialog(this, 
            "Event buttons are only enabled when the Codename one creator tool\n"
            + "can locate your source directory and the properties file residing\n"
            + "above the source directory. This structure is generated when you\n"
            + "generate an IDE project from the file menu or open a resource file\n"
            + "that resides under this hierarchy.\n"
            + "A common mistake is to generate an IDE project and then reopen the\n"
            + "original resource file. Notice that once you generate a project you\n"
            + "must always use the new version of the resource file saved within the\n"
            + "project directory.", "Why Are Events Disabled?", JOptionPane.PLAIN_MESSAGE);
}//GEN-LAST:event_whyAreEventsDisabledActionPerformed

private void simulateDeviceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_simulateDeviceActionPerformed
    String theme = null;
    PreviewInSimulator.execute(this, theme, ResourceEditorView.getTemporarySaveOfCurrentFile(), name);
}//GEN-LAST:event_simulateDeviceActionPerformed

private void codenameOneMediaPlayerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_codenameOneMediaPlayerActionPerformed
        if(lockForDragging) {
            lockForDragging = false;
            return;
        }
        addComponentToContainer(new com.codename1.components.MediaPlayer(), "MediaPlayer");
}//GEN-LAST:event_codenameOneMediaPlayerActionPerformed

private void initialFormActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_initialFormActionPerformed
    view.setNewMainForm(name);
}//GEN-LAST:event_initialFormActionPerformed

private void codenameOneNumericSpinnerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_codenameOneNumericSpinnerActionPerformed
        if(lockForDragging) {
            lockForDragging = false;
            return; 
        }
        addComponentToContainer(new com.codename1.ui.spinner.NumericSpinner(), "NumericSpinner");
}//GEN-LAST:event_codenameOneNumericSpinnerActionPerformed

private void codenameOneDateSpinnerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_codenameOneDateSpinnerActionPerformed
        if(lockForDragging) {
            lockForDragging = false;
            return; 
        }
        addComponentToContainer(new com.codename1.ui.spinner.DateSpinner(), "DateSpinner");
}//GEN-LAST:event_codenameOneDateSpinnerActionPerformed

private void codenameOneTimeSpinnerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_codenameOneTimeSpinnerActionPerformed
        if(lockForDragging) {
            lockForDragging = false;
            return; 
        }
        addComponentToContainer(new com.codename1.ui.spinner.TimeSpinner(), "TimeSpinner");
}//GEN-LAST:event_codenameOneTimeSpinnerActionPerformed

private void codenameOneDateTimeSpinnerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_codenameOneDateTimeSpinnerActionPerformed
        if(lockForDragging) {
            lockForDragging = false;
            return; 
        }
        addComponentToContainer(new com.codename1.ui.spinner.DateTimeSpinner(), "DateTimeSpinner");

}//GEN-LAST:event_codenameOneDateTimeSpinnerActionPerformed

private void codenameOneGenericSpinnerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_codenameOneGenericSpinnerActionPerformed
        if(lockForDragging) {
            lockForDragging = false;
            return; 
        }
        addComponentToContainer(new com.codename1.ui.spinner.GenericSpinner(), "GenericSpinner");
}//GEN-LAST:event_codenameOneGenericSpinnerActionPerformed

    private void codenameOneMultiButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_codenameOneMultiButtonActionPerformed
        if(lockForDragging) {
            lockForDragging = false;
            return; 
        }
        addComponentToContainer(new com.codename1.components.MultiButton(), "MultiButton");
    }//GEN-LAST:event_codenameOneMultiButtonActionPerformed

    private void codenameOneLikeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_codenameOneLikeButtonActionPerformed
        if(lockForDragging) {
            lockForDragging = false;
            return; 
        }
        addComponentToContainer(new com.codename1.facebook.ui.LikeButton(), "LikeButton");
    }//GEN-LAST:event_codenameOneLikeButtonActionPerformed

    private void codenameOneInfiniteProgressActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_codenameOneInfiniteProgressActionPerformed
        if(lockForDragging) {
            lockForDragging = false;
            return; 
        }
        addComponentToContainer(new com.codename1.components.InfiniteProgress(), "InfiniteProgress");
    }//GEN-LAST:event_codenameOneInfiniteProgressActionPerformed

    private void codenameOneAdsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_codenameOneAdsActionPerformed
        if(lockForDragging) {
            lockForDragging = false;
            return; 
        }
        addComponentToContainer(new com.codename1.components.Ads(), "Ads");
    }//GEN-LAST:event_codenameOneAdsActionPerformed

    private void codenameOneMapActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_codenameOneMapActionPerformed
        if(lockForDragging) {
            lockForDragging = false;
            return; 
        }
        addComponentToContainer(new com.codename1.maps.MapComponent(), "MapComponent");
    }//GEN-LAST:event_codenameOneMapActionPerformed

    private void codenameOneMultiListActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_codenameOneMultiListActionPerformed
        if(lockForDragging) {
            lockForDragging = false;
            return; 
        }
        addComponentToContainer(new com.codename1.ui.list.MultiList(), "MultiList");
    }//GEN-LAST:event_codenameOneMultiListActionPerformed

    private void codenameOneShareActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_codenameOneShareActionPerformed
        if(lockForDragging) {
            lockForDragging = false;
            return; 
        }
        addComponentToContainer(new com.codename1.components.ShareButton(), "ShareButton");
    }//GEN-LAST:event_codenameOneShareActionPerformed

    private void codenameOneCalendarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_codenameOneCalendarActionPerformed
        if(lockForDragging) {
            lockForDragging = false;
            return; 
        }
        addComponentToContainer(new com.codename1.ui.Calendar(), "Calendar");
    }//GEN-LAST:event_codenameOneCalendarActionPerformed

private void codenameOneOnOffSwitchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_codenameOneOnOffSwitchActionPerformed
        if(lockForDragging) {
            lockForDragging = false;
            return; 
        }
        addComponentToContainer(new com.codename1.components.OnOffSwitch(), "OnOffSwitch");
}//GEN-LAST:event_codenameOneOnOffSwitchActionPerformed

private void codenameOneSpanButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_codenameOneSpanButtonActionPerformed
        if(lockForDragging) {
            lockForDragging = false;
            return; 
        }
        addComponentToContainer(new com.codename1.components.SpanButton(), "SpanButton");
}//GEN-LAST:event_codenameOneSpanButtonActionPerformed

    private void codenameOneImageViewerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_codenameOneImageViewerActionPerformed
        if(lockForDragging) {
            lockForDragging = false;
            return; 
        }
        addComponentToContainer(new com.codename1.components.ImageViewer(), "ImageViewer");
    }//GEN-LAST:event_codenameOneImageViewerActionPerformed

    private void codenameOneSpanLabelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_codenameOneSpanLabelActionPerformed
        if(lockForDragging) {
            lockForDragging = false;
            return; 
        }
        addComponentToContainer(new com.codename1.components.SpanLabel(), "SpanLabel");
    }//GEN-LAST:event_codenameOneSpanLabelActionPerformed

    private void codenameOneAutoCompleteTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_codenameOneAutoCompleteTextFieldActionPerformed
        if(lockForDragging) {
            lockForDragging = false;
            return; 
        }
        addComponentToContainer(new com.codename1.ui.AutoCompleteTextField(), "AutoCompleteTextField");
    }//GEN-LAST:event_codenameOneAutoCompleteTextFieldActionPerformed


    private String findUniqueName(String prefix) {
        // try prefix first
        if(!findName(prefix, containerInstance)) {
            return prefix;
        }
        int counter = 1;
        String actual = prefix + counter;
        while(findName(actual, containerInstance)) {
            counter++;
            actual = prefix + counter;
        }
        return actual;
    }

    private boolean findName(String s, com.codename1.ui.Component cmp) {
        if(s.equalsIgnoreCase(cmp.getName())) {
            return true;
        }
        if(isActualContainer(cmp)) {
            com.codename1.ui.Container c = (com.codename1.ui.Container)cmp;
            for(int iter = 0 ; iter < c.getComponentCount() ; iter++) {
                if(findName(s, c.getComponentAt(iter))) {
                    return true;
                }
            }
        }
        return false;
    }

    private void addComponentToContainer(com.codename1.ui.Component c, String name) {
        if(c instanceof com.codename1.ui.Label || c instanceof com.codename1.ui.TextArea) {
            setPropertyModified(c, PROPERTY_TEXT);
        }
        c.putClientProperty(TYPE_KEY, name);
        c.setName(findUniqueName(name));
        com.codename1.ui.Container destContainer = containerInstance;
        if(componentHierarchy.getSelectionPath() != null) {
            com.codename1.ui.Component cmp = (com.codename1.ui.Component)componentHierarchy.getSelectionPath().getLastPathComponent();
            if(isActualContainer(cmp)) {
                destContainer = (com.codename1.ui.Container)cmp;
            } else {
                destContainer = cmp.getParent();
            }
        }
        if(destContainer instanceof com.codename1.ui.Tabs) {
            ((com.codename1.ui.Tabs)destContainer).addTab("Tab", c);
        } else {
            if(destContainer.getLayout() instanceof com.codename1.ui.layouts.BorderLayout) {
                destContainer.addComponent(findAvailableSpotInBorderLayout(destContainer), c);
            } else {
                destContainer.addComponent(c);
            }
        }
        containerInstance.revalidate();
        destContainer.revalidate();
        /*int[] index = new int[] {componentHierarchy.getModel().getIndexOfChild(c.getParent(), c)};
        Object[] children = new Object[] {c};
        c = c.getParent();
        Object[] path = pathToComponent(c);
        if(path != null && path.length > 0) {
            ((ComponentHierarchyModel)componentHierarchy.getModel()).fireTreeNodesInserted(
                    new TreeModelEvent(componentHierarchy.getModel(), new TreePath(path), index, children));
            uiPreview.repaint();
            saveUI();
        }*/
        ((ComponentHierarchyModel)componentHierarchy.getModel()).fireTreeStructureChanged(
                new TreeModelEvent(componentHierarchy.getModel(),
                    new TreePath(componentHierarchy.getModel().getRoot())));
        uiPreview.repaint();
        saveUI();
    }

    private Object[] pathToComponent(com.codename1.ui.Component c) {
        List<com.codename1.ui.Component> path = new ArrayList<com.codename1.ui.Component>();
        while(c != null) {
            // skip the content pane which is hidden in the hierarchy
            if(!(c.getParent() instanceof com.codename1.ui.Form || c.getParent() instanceof com.codename1.ui.Tabs)) {
                path.add(0, c);
            }

            c = c.getParent();
        }
        com.codename1.ui.Component[] pathArray = new com.codename1.ui.Component[path.size()];
        path.toArray(pathArray);
        return pathArray;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JSplitPane arrangeLeftRight;
    private javax.swing.JButton bindActionEvent;
    private javax.swing.JButton bindBeforeShow;
    private javax.swing.JButton bindExitForm;
    private javax.swing.JButton bindListModel;
    private javax.swing.JButton bindOnCreate;
    private javax.swing.JButton bindPostShow;
    private javax.swing.JButton codenameOneAds;
    private javax.swing.JButton codenameOneAutoCompleteTextField;
    private javax.swing.JButton codenameOneButton;
    private javax.swing.JButton codenameOneCalendar;
    private javax.swing.JButton codenameOneCheckBox;
    private javax.swing.JButton codenameOneComboBox;
    private javax.swing.JButton codenameOneComponentGroup;
    private javax.swing.JButton codenameOneContainer;
    private javax.swing.JButton codenameOneContainerList;
    private javax.swing.JButton codenameOneDateSpinner;
    private javax.swing.JButton codenameOneDateTimeSpinner;
    private javax.swing.JPanel codenameOneExtraComponents;
    private javax.swing.JButton codenameOneGenericSpinner;
    private javax.swing.JButton codenameOneHTMLComponent;
    private javax.swing.JPanel codenameOneIOComponents;
    private javax.swing.JButton codenameOneImageViewer;
    private javax.swing.JButton codenameOneInfiniteProgress;
    private javax.swing.JButton codenameOneLabel;
    private javax.swing.JButton codenameOneLikeButton;
    private javax.swing.JButton codenameOneList;
    private javax.swing.JButton codenameOneMap;
    private javax.swing.JButton codenameOneMediaPlayer;
    private javax.swing.JButton codenameOneMultiButton;
    private javax.swing.JButton codenameOneMultiList;
    private javax.swing.JButton codenameOneNumericSpinner;
    private javax.swing.JButton codenameOneOnOffSwitch;
    private javax.swing.JButton codenameOneRadioButton;
    private javax.swing.JButton codenameOneShare;
    private javax.swing.JButton codenameOneSlider;
    private javax.swing.JButton codenameOneSpanButton;
    private javax.swing.JButton codenameOneSpanLabel;
    private javax.swing.JButton codenameOneTable;
    private javax.swing.JButton codenameOneTabs;
    private javax.swing.JButton codenameOneTextArea;
    private javax.swing.JButton codenameOneTextField;
    private javax.swing.JButton codenameOneTimeSpinner;
    private javax.swing.JButton codenameOneTree;
    private org.jdesktop.swingx.JXTree componentHierarchy;
    private javax.swing.JPanel componentPalette;
    private javax.swing.JPanel coreComponents;
    private javax.swing.JButton embedContainer;
    private javax.swing.JButton fileTree;
    private javax.swing.JTextPane help;
    private javax.swing.JButton initialForm;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JSplitPane jSplitPane2;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JPanel leftSidePanel;
    private javax.swing.JTable localizeTable;
    private javax.swing.JScrollPane palettePanel;
    private javax.swing.JTable properties;
    private javax.swing.JTabbedPane propertyAndEventTabs;
    private javax.swing.JComboBox resourceBundle;
    private javax.swing.JButton rssReader;
    private javax.swing.JButton simulateDevice;
    private javax.swing.JPanel uiPreview;
    private javax.swing.JPanel userComponents;
    private org.jdesktop.swingx.JXButton whyAreEventsDisabled;
    // End of variables declaration//GEN-END:variables

    class DraggablePreview extends CodenameOneComponentWrapper implements MouseListener {
        private com.codename1.ui.Component draggedComponent;
        private Rectangle markerPosition;
        public DraggablePreview(com.codename1.ui.Component c) {
            super(c, true);
            addMouseListener(this);
            setDropTarget(new DropTarget(this, new DropTargetListener() {
                private com.codename1.ui.Label marker = new com.codename1.ui.Label();
                public void dragEnter(DropTargetDragEvent dtde) {
                }

                public void dragOver(DropTargetDragEvent dtde) {
                    try {
                        draggedComponent = (com.codename1.ui.Component)dtde.getTransferable().getTransferData(CODENAMEONE_COMPONENT_FLAVOR);
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                    try {
                        marker.setPreferredSize(draggedComponent.getPreferredSize());
                        marker.getStyle().setBgColor(0xff0000);
                        marker.getStyle().setBgTransparency(0x7f);
                        com.codename1.ui.Component c = containerInstance.getComponentAt(dtde.getLocation().x, dtde.getLocation().y);
                        if(c == null) {
                            c = containerInstance;
                        }
                        com.codename1.ui.Container dest;
                        int index;
                        //System.out.println("dragOver " + c.getClass().getName() + " uiid: " + c.getUIID() + " actual: " + isActualContainer(c) + " getActualComponent(c): " + getActualComponent(c));
                        if(isActualContainer(c)) {
                            dest = (com.codename1.ui.Container)c;
                            index = dest.getComponentCount();
                            //dest = (com.codename1.ui.Container)getActualComponent(c);
                        } else {
                            c = getActualComponent(c);
                            if(c.getClass() == com.codename1.ui.Container.class || c.getClass() == com.codename1.ui.Form.class ||
                                    c.getClass() == com.codename1.ui.Tabs.class || c.getClass() == com.codename1.ui.ComponentGroup.class) {
                                dest = (com.codename1.ui.Container)c;
                            } else {
                                dest = c.getParent();
                            }
                            index = Math.max(0, Math.min(dest.getComponentIndex(c), dest.getComponentCount()));
                        }
                        if(dest.getLayout() instanceof com.codename1.ui.layouts.BorderLayout) {
                            com.codename1.ui.layouts.BorderLayout bl = (com.codename1.ui.layouts.BorderLayout)dest.getLayout();
                            String constraint = getBorderLayoutDropLocation(dest, dtde.getLocation());
                            if(constraint == BorderLayout.CENTER) {
                                if(bl.getCenter() != null) {
                                    markerPosition = new Rectangle(bl.getCenter().getAbsoluteX(), bl.getCenter().getAbsoluteY(),
                                            bl.getCenter().getWidth(), bl.getCenter().getHeight());
                                    repaint();
                                    return;
                                }
                            } else {
                                if(constraint == BorderLayout.WEST) {
                                    if(bl.getWest() != null) {
                                        markerPosition = new Rectangle(bl.getWest().getAbsoluteX(), bl.getWest().getAbsoluteY(),
                                                bl.getWest().getWidth(), bl.getWest().getHeight());
                                        repaint();
                                        return;
                                    }
                                } else {
                                    if(constraint == BorderLayout.EAST) {
                                        if(bl.getEast() != null) {
                                            markerPosition = new Rectangle(bl.getEast().getAbsoluteX(), bl.getEast().getAbsoluteY(),
                                                    bl.getEast().getWidth(), bl.getEast().getHeight());
                                            repaint();
                                            return;
                                        }
                                    } else {
                                        if(constraint == BorderLayout.SOUTH) {
                                            if(bl.getSouth() != null) {
                                                markerPosition = new Rectangle(bl.getSouth().getAbsoluteX(), bl.getSouth().getAbsoluteY(),
                                                        bl.getSouth().getWidth(), bl.getSouth().getHeight());
                                                repaint();
                                                return;
                                            }
                                        } else {
                                            if(bl.getNorth() != null) {
                                                markerPosition = new Rectangle(bl.getNorth().getAbsoluteX(), bl.getNorth().getAbsoluteY(),
                                                        bl.getNorth().getWidth(), bl.getNorth().getHeight());
                                                repaint();
                                                return;
                                            }
                                        }
                                    }
                                }
                            }
                            dest.addComponent(constraint, marker);
                        } else {
                            dest.addComponent(index, marker);
                            reflowTableLayout(dest);
                        }
                        containerInstance.revalidate();
                        markerPosition = new Rectangle(marker.getAbsoluteX(), marker.getAbsoluteY(),
                                marker.getWidth(), marker.getHeight());
                        removeComponentSync(dest, marker);
                        containerInstance.revalidate();
                        repaint();
                    } finally {
                        if(marker.getParent() != null) {
                            removeComponentSync(marker.getParent(), marker);
                            containerInstance.revalidate();
                            repaint();
                        }
                    }
                }

                public void dropActionChanged(DropTargetDragEvent dtde) {
                }

                public void dragExit(DropTargetEvent dte) {
                    containerInstance.revalidate();
                    markerPosition = null;
                    draggedComponent = null;
                    repaint();
                }

                public void drop(DropTargetDropEvent dtde) {
                    if(draggedComponent == null) {
                        return;
                    }
                    markerPosition = null;
                    com.codename1.ui.Component c = containerInstance.getComponentAt(dtde.getLocation().x, dtde.getLocation().y);
                    if(c == null) {
                        c = containerInstance;
                    }
                    com.codename1.ui.Container dest;
                    int index;
                    //System.out.println("drop " + c.getClass().getName() + " uiid: " + c.getUIID() + " actual: " + isActualContainer(c) + " getActualComponent(c): " + getActualComponent(c));
                    if(isActualContainer(c)) {
                        dest = (com.codename1.ui.Container)c;
                        index = dest.getComponentCount();
                        dest = (com.codename1.ui.Container)getActualComponent(c);
                    } else {
                        c = getActualComponent(c);
                        if(c.getClass() == com.codename1.ui.Container.class || c.getClass() == com.codename1.ui.Form.class ||
                                c.getClass() == com.codename1.ui.Tabs.class || c.getClass() == com.codename1.ui.ComponentGroup.class) {
                            dest = (com.codename1.ui.Container)c;
                        } else {
                            dest = c.getParent();
                        }
                        index = Math.max(0, Math.min(dest.getComponentIndex(c), dest.getComponentCount()));
                    }
                    com.codename1.ui.Component tmpCmp = dest;
                    while(tmpCmp != null) {
                        if(tmpCmp == draggedComponent) {
                            return;
                        }
                        tmpCmp = tmpCmp.getParent();
                    }

                    if(dest.getLayout() instanceof com.codename1.ui.layouts.BorderLayout) {
                        com.codename1.ui.layouts.BorderLayout bl = (com.codename1.ui.layouts.BorderLayout)dest.getLayout();
                        String constraint = getBorderLayoutDropLocation(dest, dtde.getLocation());
                        if(draggedComponent.getParent() != null) {
                            if(draggedComponent.getParent() != dest) {
                                // we don't have room
                                if(dest.getComponentCount() > 4) {
                                    repaint();
                                    return;
                                }
                            } else {
                                if(getComponentAtConstraint(constraint, bl) != null) {
                                    String old = (String)bl.getComponentConstraint(draggedComponent);
                                    com.codename1.ui.Component at = getComponentAtConstraint(constraint, bl);

                                    if(at == draggedComponent) {
                                        repaint();
                                        return;
                                    }

                                    removeComponentSync(dest, at);
                                    removeComponentSync(dest, draggedComponent);
                                    dest.addComponent(old, at);
                                    dest.addComponent(constraint, draggedComponent);
                                    containerInstance.revalidate();
                                    repaint();
                                    return;
                                }
                            }
                            removeComponentSync(draggedComponent.getParent(), draggedComponent);
                        } else {
                            // we don't have room
                            if(dest.getComponentCount() > 4) {
                                repaint();
                                return;
                            }
                            if(getComponentAtConstraint(constraint, bl) != null) {
                                com.codename1.ui.Component cmp = getComponentAtConstraint(constraint, bl);

                                // find an empty spot to move the existing component to
                                if(bl.getEast() == null) {
                                    removeComponentSync(dest, cmp);
                                    dest.addComponent(com.codename1.ui.layouts.BorderLayout.EAST, cmp);
                                } else {
                                    if(bl.getWest() == null) {
                                        removeComponentSync(dest, cmp);
                                        dest.addComponent(com.codename1.ui.layouts.BorderLayout.WEST, cmp);
                                    } else {
                                        if(bl.getSouth() == null) {
                                            removeComponentSync(dest, cmp);
                                            dest.addComponent(com.codename1.ui.layouts.BorderLayout.SOUTH, cmp);
                                        } else {
                                            if(bl.getNorth() == null) {
                                                removeComponentSync(dest, cmp);
                                                dest.addComponent(com.codename1.ui.layouts.BorderLayout.NORTH, cmp);
                                            } else {
                                                removeComponentSync(dest, cmp);
                                                dest.addComponent(com.codename1.ui.layouts.BorderLayout.CENTER, cmp);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        dest.addComponent(constraint, draggedComponent);
                    } else {
                        if(draggedComponent.getParent() != null) {
                            if(draggedComponent.getParent() == dest) {
                                int currentIndex = dest.getComponentIndex(draggedComponent);
                                if(currentIndex < index) {
                                    index--;
                                }
                            }
                            removeComponentSync(draggedComponent.getParent(), draggedComponent);
                        }

                        dest.addComponent(index, draggedComponent);
                        reflowTableLayout(dest);
                    }

                    draggedComponent.getComponentForm().revalidate();
                    dest.revalidate();
                    ((ComponentHierarchyModel)componentHierarchy.getModel()).fireTreeStructureChanged(
                            new TreeModelEvent(componentHierarchy.getModel(),
                                new TreePath(componentHierarchy.getModel().getRoot())));
                    repaint();
                    saveUI();
                    draggedComponent = null;
                }
            }));
            DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(this,
                    TransferHandler.MOVE, new DragGestureListener() {
                public void dragGestureRecognized(DragGestureEvent dge) {
                    try {
                        com.codename1.ui.Component cmp = getActualComponent((com.codename1.ui.Component)containerInstance.getComponentAt(dge.getDragOrigin().x, dge.getDragOrigin().y));
                        if(cmp == containerInstance || cmp instanceof com.codename1.ui.Form ||
                                cmp instanceof com.codename1.ui.MenuBar ||
                                cmp.getUIID().equals("Title") ||
                                cmp.getUIID().equals("TitleArea") ||
                                cmp.getUIID().equals("SoftButton") ||
                                cmp.getParent() instanceof com.codename1.ui.MenuBar) {
                            return;
                        }

                        DragSource.getDefaultDragSource().startDrag(dge, DragSource.DefaultMoveDrop,
                                new CodenameOneComponentTransferable(cmp), new DragSourceAdapter() {
                                });
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }


        private String getBorderLayoutDropLocation(com.codename1.ui.Container dest, Point l) {
            Rectangle rect = new Rectangle(dest.getAbsoluteX(), dest.getAbsoluteY(), dest.getWidth(), dest.getHeight());
            Rectangle left = new Rectangle(rect.x, rect.y, rect.width / 5, rect.height);
            Rectangle right = new Rectangle(rect.x + rect.width - rect.width / 5, rect.y, rect.width / 5, rect.height);
            Rectangle top = new Rectangle(rect.x, rect.y, rect.width, rect.height / 5);
            Rectangle bottom = new Rectangle(rect.x, rect.y + rect.height - rect.height / 5, rect.width, rect.height / 5);
            if(left.contains(l)) {
                return com.codename1.ui.layouts.BorderLayout.WEST;
            } else {
                if(right.contains(l)) {
                    return com.codename1.ui.layouts.BorderLayout.EAST;
                } else {
                    if(top.contains(l)) {
                        return com.codename1.ui.layouts.BorderLayout.NORTH;
                    } else {
                        if(bottom.contains(l)) {
                            return com.codename1.ui.layouts.BorderLayout.SOUTH;
                        } else {
                            return com.codename1.ui.layouts.BorderLayout.CENTER;
                        }
                    }
                }
            }
        }

        public void mouseClicked(MouseEvent e) {
            com.codename1.ui.Component cmp = containerInstance.getComponentAt(e.getX(), e.getY());
            if(cmp == null) {
                cmp = containerInstance;
            } else {
                // special case for easily selecting a tab in a tabbed pane
                if(cmp instanceof com.codename1.ui.Button && cmp.getUIID().equals("Tab")) {
                    int i = cmp.getParent().getComponentIndex(cmp);
                    if(i > -1) {
                        com.codename1.ui.Tabs t = (com.codename1.ui.Tabs)cmp.getParent().getParent();
                        t.setSelectedIndex(i);
                        componentHierarchy.setSelectionPath(new TreePath(pathToComponent(t.getTabComponentAt(i))));
                    }
                    return;
                }
            }
            boolean title = "Title".equals(cmp.getUIID());
            cmp = getActualComponent(cmp);
            if(cmp != null) {
                Object[] arr = pathToComponent(cmp);
                if(arr == null || arr.length == 0) {
                    return;
                }
                if(e.isMetaDown() || e.isControlDown()) {
                    componentHierarchy.addSelectionPath(new TreePath(arr));
                } else {
                    componentHierarchy.setSelectionPath(new TreePath(arr));
                }
            }
            if(e.getClickCount() == 2) {
                if(cmp instanceof com.codename1.ui.Label || cmp instanceof com.codename1.ui.TextArea || title) {
                    final com.codename1.ui.Component finalComponent = cmp;
                    final JTextField f = new JTextField();
                    if(title) {
                        cmp = cmp.getComponentForm().getTitleComponent();
                    }
                    if(cmp instanceof com.codename1.ui.Label) {
                        f.setText(((com.codename1.ui.Label)cmp).getText());
                    } else {
                        f.setText(((com.codename1.ui.TextArea)cmp).getText());
                    }
                    f.selectAll();
                    f.setPreferredSize(new java.awt.Dimension(cmp.getWidth(), cmp.getHeight()));
                    f.setSize(new java.awt.Dimension(cmp.getWidth(), cmp.getHeight()));
                    f.setLocation(cmp.getAbsoluteX(), cmp.getAbsoluteY());
                    add(f);
                    f.requestFocus();
                    f.addFocusListener(new FocusListener() {
                        public void focusGained(FocusEvent e) {
                        }

                        public void focusLost(FocusEvent e) {
                            remove(f);
                            updateComponentText(f, finalComponent);
                        }
                    });
                    f.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            remove(f);
                            updateComponentText(f, finalComponent);
                        }
                    });
                }
            }
        }

        private void updateComponentText(JTextField f, com.codename1.ui.Component finalComponent) {
            if(finalComponent instanceof com.codename1.ui.Label) {
                ((com.codename1.ui.Label)finalComponent).setText(f.getText());
                if(finalComponent instanceof com.codename1.ui.Button) {
                    // update the command as well...
                    if(((com.codename1.ui.Button)finalComponent).getCommand() != null) {
                        ((com.codename1.ui.Button)finalComponent).getCommand().setCommandName(f.getText());
                    }
                }
            } else {
                if(finalComponent instanceof com.codename1.ui.Form) {
                    ((com.codename1.ui.Form)finalComponent).setTitle(f.getText());
                } else {
                    ((com.codename1.ui.TextArea)finalComponent).setText(f.getText());
                }
            }
            setPropertyModified(finalComponent, PROPERTY_TEXT);
            ((ComponentPropertyEditorModel)properties.getModel()).setComponents(new com.codename1.ui.Component[] {finalComponent});
            saveUI();
        }

        public void mousePressed(MouseEvent e) {
            com.codename1.ui.Component cmp = containerInstance.getComponentAt(e.getX(), e.getY());
            if(cmp == null) {
                cmp = containerInstance;
            }
            draggedComponent = getActualComponent(cmp);
        }

        public void mouseReleased(MouseEvent e) {}

        public void mouseEntered(MouseEvent e) {}

        public void mouseExited(MouseEvent e) {}

        public void paint(Graphics g) {
            super.paint(g);
            for(com.codename1.ui.Component cmp : getSelectedComponents()) {
                if(!(cmp instanceof com.codename1.ui.Form || cmp.getParent() instanceof com.codename1.ui.Form)) {
                    Graphics2D g2d = (Graphics2D)g.create();
                    g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2d.setColor(Color.BLUE);
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
                    g2d.drawRect(cmp.getAbsoluteX(), cmp.getAbsoluteY(), cmp.getWidth(), cmp.getHeight());
                }
            }
            if(markerPosition != null) {
                Graphics2D g2d = (Graphics2D)g.create();
                g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2d.setColor(Color.RED);
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
                g2d.fillRect(markerPosition.x, markerPosition.y, markerPosition.width, markerPosition.height);
            }
        }
    }

    class CodenameOneComponentTransferable implements Transferable {
        private com.codename1.ui.Component c;
        public CodenameOneComponentTransferable(com.codename1.ui.Component c) {
            this.c = c;
        }

        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[] {CODENAMEONE_COMPONENT_FLAVOR};
        }

        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return CODENAMEONE_COMPONENT_FLAVOR == flavor;
        }

        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
            return c;
        }
    };

    private static final String[] BORDER_LAYOUT_CONSTRAINTS = {
        com.codename1.ui.layouts.BorderLayout.CENTER,
        com.codename1.ui.layouts.BorderLayout.NORTH,
        com.codename1.ui.layouts.BorderLayout.SOUTH,
        com.codename1.ui.layouts.BorderLayout.EAST,
        com.codename1.ui.layouts.BorderLayout.WEST
    };
    private String findAvailableSpotInBorderLayout(com.codename1.ui.Container c) {
        com.codename1.ui.layouts.BorderLayout bl = (com.codename1.ui.layouts.BorderLayout)c.getLayout();
        for(String con : BORDER_LAYOUT_CONSTRAINTS) {
            if(getComponentAtConstraint(con, bl) == null) {
                return con;
            }
        }
        return null;
    }

    private static com.codename1.ui.Component getComponentAtConstraint(String constraint, com.codename1.ui.layouts.BorderLayout bl) {
        if(constraint == BorderLayout.WEST) {
            return bl.getWest();
        } else {
            if(constraint == BorderLayout.EAST) {
                return bl.getEast();
            } else {
                if(constraint == BorderLayout.SOUTH) {
                    return bl.getSouth();
                } else {
                    if(constraint == BorderLayout.NORTH) {
                        return bl.getNorth();
                    }
                    return bl.getCenter();
                }
            }
        }
    }

    private void reflowTableLayout(com.codename1.ui.Container parent) {
        // we need to re-add all the components to the table layout since order only
        // affects the layout initially
        if(parent.getLayout() instanceof com.codename1.ui.table.TableLayout) {
            List<com.codename1.ui.Component> cmpList = new ArrayList<com.codename1.ui.Component>();
            List constraintList = new ArrayList();
            for(int iter = 0 ; iter < parent.getComponentCount() ; iter++) {
                com.codename1.ui.Component currentCmp = parent.getComponentAt(iter);
                cmpList.add(currentCmp);
                constraintList.add(parent.getLayout().getComponentConstraint(currentCmp));
            }
            parent.removeAll();
            for(int iter = 0 ; iter < cmpList.size() ; iter++) {
                parent.addComponent(constraintList.get(iter), cmpList.get(iter));
            }
        }
    }

    /**
     * Checks that the component is not a child of tree/table or HTMLComponent
     * in which case it returns the actual component. This avoids dropping within these components.
     */
    public com.codename1.ui.Component getActualComponent(com.codename1.ui.Component c) {
        com.codename1.ui.Component cmp = c;

        while(cmp != null) {
            if(cmp instanceof com.codename1.ui.Container && cmp.getClass() != com.codename1.ui.Container.class
                    && !(cmp instanceof com.codename1.ui.Form) && !(cmp instanceof com.codename1.ui.Tabs) &&
                    !(cmp instanceof com.codename1.ui.ComponentGroup)) {
                return cmp;
            }
            
            if(cmp.getUIID() == null) {
                cmp = cmp.getParent();
                continue;
            }
            // prevent a drop on the tab area of the tabs component
            if(cmp.getUIID().equals("TabsContainer")) {
                return cmp.getParent();
            }
            // prevent a drop on the title area
            if(cmp.getUIID().equals("TitleArea")) {
                return cmp.getParent();
            }
            cmp = cmp.getParent();
        }
        if(c.getParent() instanceof com.codename1.ui.Tabs) {
            return (com.codename1.ui.Tabs)c.getParent();
        }
        return c;
    }

    private static final int ENCLOSE_IN_TABS = 1;
    private static final int ENCLOSE_IN_CONTAINER = 2;
    private static final int ENCLOSE_IN_COMPONENT_GROUP = 3;
    class EncloseIn extends AbstractAction {
        private int tabs;
        public EncloseIn(String name, int tabs) {
            super(name);
            this.tabs = tabs;

            // only allow enclosing components that already share a single parent to prevent complex cases where a user tries
            // to enclose both a child and its parent
            com.codename1.ui.Component[] cmps = getSelectedComponents();
            if(cmps != null && cmps.length > 0)  {
                com.codename1.ui.Container parent = cmps[0].getParent();
                for(com.codename1.ui.Component c : cmps) {
                    if(parent != c.getParent()) {
                        setEnabled(false);
                        return;
                    }
                }
                setEnabled(true);
                return;
            }
            setEnabled(false);
        }

        public void actionPerformed(ActionEvent e) {
            com.codename1.ui.Component[] cmps = getSelectedComponents();
            if(cmps != null && cmps.length > 0)  {
                com.codename1.ui.Container parent = cmps[0].getParent();
                switch(tabs) {
                        case ENCLOSE_IN_TABS:
                                com.codename1.ui.Tabs newTabs = new com.codename1.ui.Tabs();
                                newTabs.putClientProperty(TYPE_KEY, "Tabs");
                                newTabs.setName(findUniqueName("Tabs"));
                                parent.replace(cmps[0], newTabs, null);
                                newTabs.addTab("Tab0", cmps[0]);
                                for(int iter = 1 ; iter < cmps.length ; iter++)  {
                                    parent.removeComponent(cmps[iter]);
                                    newTabs.addTab("Tab" + iter, cmps[iter]);
                                }
                            break;
                        
                        case ENCLOSE_IN_CONTAINER: {
                            com.codename1.ui.Container cnt = new com.codename1.ui.Container();
                            cnt.putClientProperty(TYPE_KEY, "Container");
                            cnt.setName(findUniqueName("Container"));
                            parent.replace(cmps[0], cnt, null);
                            cnt.addComponent(cmps[0]);
                            for(int iter = 1 ; iter < cmps.length ; iter++)  {
                                parent.removeComponent(cmps[iter]);
                                cnt.addComponent(cmps[iter]);
                            }
                            break;
                        }

                        case ENCLOSE_IN_COMPONENT_GROUP: {
                            com.codename1.ui.ComponentGroup cnt = new com.codename1.ui.ComponentGroup();
                            cnt.putClientProperty(TYPE_KEY, "ComponentGroup");
                            cnt.setName(findUniqueName("ComponentGroup"));
                            parent.replace(cmps[0], cnt, null);
                            cnt.addComponent(cmps[0]);
                            for(int iter = 1 ; iter < cmps.length ; iter++)  {
                                parent.removeComponent(cmps[iter]);
                                cnt.addComponent(cmps[iter]);
                            }
                            break;
                        }
                }
                containerInstance.revalidate();
                ((ComponentHierarchyModel)componentHierarchy.getModel()).fireTreeStructureChanged(
                        new TreeModelEvent(componentHierarchy.getModel(), new TreePath(containerInstance)));
                uiPreview.repaint();
                saveUI();
            }
        }
    }

    private void applyThemePreview(String name) {
        if(name.indexOf('@') > -1) {
            FileInputStream f = null;
            try {
                String[] t = name.split("@");
                f = new FileInputStream(t[0]);
                Resources r = Resources.open(f);
                if(r.getTheme(t[1]) != null) {
                    Accessor.setTheme(r.getTheme(t[1]));
                } else {
                    Accessor.setTheme(r.getTheme(r.getThemeResourceNames()[0]));
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            } finally {
                try {
                    f.close();
                } catch (Exception ex) {}
            }
        } else {
            Accessor.setTheme(res.getTheme(name));
        }
    }
}
