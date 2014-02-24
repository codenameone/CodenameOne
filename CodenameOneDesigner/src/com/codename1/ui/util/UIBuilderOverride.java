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

import com.codename1.designer.ActionCommand;
import com.codename1.designer.UserInterfaceEditor;
import com.codename1.ui.Button;
import com.codename1.ui.CheckBox;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.List;
import com.codename1.ui.RadioButton;
import com.codename1.ui.Slider;
import com.codename1.ui.Tabs;
import com.codename1.ui.TextArea;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.list.ContainerList;
import com.codename1.ui.list.DefaultListModel;
import com.codename1.ui.list.GenericListCellRenderer;
import com.codename1.ui.table.TableLayout;
import com.codename1.ui.util.xml.Val;
import com.codename1.ui.util.xml.comps.CommandEntry;
import com.codename1.ui.util.xml.comps.ComponentEntry;
import com.codename1.ui.util.xml.comps.Custom;
import com.codename1.ui.util.xml.comps.StringEntry;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import javax.swing.JOptionPane;

/**
 * Extends the UIBuilder from CodenameOne to provide a callback on loading
 *
 * @author Shai Almog
 */
public class UIBuilderOverride extends UIBuilder {
    private String baseFormName;
    public UIBuilderOverride() {
        registerCustom();
    }

    public static void registerCustom() {
        registerCustomComponent("Table", com.codename1.ui.table.Table.class);
        registerCustomComponent("MediaPlayer", com.codename1.components.MediaPlayer.class);
        registerCustomComponent("ContainerList", com.codename1.ui.list.ContainerList.class);
        registerCustomComponent("ComponentGroup", com.codename1.ui.ComponentGroup.class);
        registerCustomComponent("Tree", com.codename1.ui.tree.Tree.class);
        registerCustomComponent("HTMLComponent", com.codename1.ui.html.HTMLComponent.class);
        registerCustomComponent("RSSReader", com.codename1.components.RSSReader.class);
        registerCustomComponent("FileTree", com.codename1.components.FileTree.class);
        registerCustomComponent("WebBrowser", com.codename1.components.WebBrowser.class);
        registerCustomComponent("NumericSpinner", com.codename1.ui.spinner.NumericSpinner.class);
        registerCustomComponent("DateSpinner", com.codename1.ui.spinner.DateSpinner.class);
        registerCustomComponent("TimeSpinner", com.codename1.ui.spinner.TimeSpinner.class);
        registerCustomComponent("DateTimeSpinner", com.codename1.ui.spinner.DateTimeSpinner.class);
        registerCustomComponent("GenericSpinner", com.codename1.ui.spinner.GenericSpinner.class);
        registerCustomComponent("LikeButton", com.codename1.facebook.ui.LikeButton.class);
        registerCustomComponent("InfiniteProgress", com.codename1.components.InfiniteProgress.class);
        registerCustomComponent("MultiButton", com.codename1.components.MultiButton.class);
        registerCustomComponent("SpanButton", com.codename1.components.SpanButton.class);
        registerCustomComponent("SpanLabel", com.codename1.components.SpanLabel.class);
        registerCustomComponent("Ads", com.codename1.components.Ads.class);
        registerCustomComponent("MapComponent", com.codename1.maps.MapComponent.class);
        registerCustomComponent("MultiList", com.codename1.ui.list.MultiList.class);
        registerCustomComponent("ShareButton", com.codename1.components.ShareButton.class);
        registerCustomComponent("OnOffSwitch", com.codename1.components.OnOffSwitch.class);
        registerCustomComponent("ImageViewer", com.codename1.components.ImageViewer.class);
        registerCustomComponent("AutoCompleteTextField", com.codename1.ui.AutoCompleteTextField.class);
        registerCustomComponent("Picker", com.codename1.ui.spinner.Picker.class);
    }

    void modifyingProperty(com.codename1.ui.Component c, int p) {
        UserInterfaceEditor.setPropertyModified(c, p);
    }

    void modifyingCustomProperty(com.codename1.ui.Component c, String name) {
        UserInterfaceEditor.setCustomPropertyModified(c, name);
    }

    public com.codename1.ui.Command createCommandImpl(String commandName, com.codename1.ui.Image icon, int commandId, String action, boolean isBack, String argument) {
        return new ActionCommand(commandName, icon, commandId, action, isBack, argument);
    }

    public static void setIgnorBaseForm(boolean b) {
        ignorBaseForm  = b;
    }

    void initBaseForm(String formName) {
        this.baseFormName = formName;
    }

    /**
     * @return the baseFormName
     */
    public String getBaseFormName() {
        return baseFormName;
    }

    /**
     * @param baseFormName the baseFormName to set
     */
    public void setBaseFormName(String baseFormName) {
        this.baseFormName = baseFormName;
    }

    protected void postCreateComponent(com.codename1.ui.Component c) {
        c.setPropertyValue("$designMode", Boolean.TRUE);
    }

    /**
     * Create a component instance from XML
     */
    public Container createInstance(ComponentEntry root, EditableResources res) {
        ArrayList<Runnable> postCreateTasks = new ArrayList<Runnable>();
        Container c = (Container)createInstance(root, res, null, null, postCreateTasks);
        
        // execute tasks that must have the entire hierarchy constructed in order to work
        for(Runnable r : postCreateTasks) {
            r.run();
        }
        return c;
    }
    
    /**
     * Create a component instance from XML
     */
    private Component createInstance(final ComponentEntry root, final EditableResources res, Container rootCnt, final Container parentContainer, final ArrayList<Runnable> postCreateTasks) {
        try {
            final Component c = createComponentType(root.getType());
            if(rootCnt == null) {
                rootCnt = (Container)c;
            }
            final Container rootContainer = rootCnt;
            if(root.getBaseForm() != null) {
                c.putClientProperty("%base_form%", root.getBaseForm());
            }
            c.putClientProperty(TYPE_KEY, root.getType());
            c.setName(root.getName());
            
            String clientProps = root.getClientProperties();
            if(clientProps != null && clientProps.length() > 0) {
                String[] props = clientProps.split(",");
                StringBuilder b = new StringBuilder();
                for(String p : props) {
                    String[] keyVal = p.split("=");
                    c.putClientProperty(keyVal[0], keyVal[1]);
                    if(b.length() > 0) {
                        b.append(",");
                    }
                    b.append(keyVal[0]);
                }
                c.putClientProperty("cn1$Properties", b.toString());
            }
            
            rootContainer.putClientProperty("%" + root.getName() + "%", c);
            
            // layout must be first since we might need to rely on it later on with things such as constraints
            if(root.getLayout() != null) {
                modifyingProperty(c, PROPERTY_LAYOUT);
                Layout l;
                if(root.getLayout().equals("BorderLayout")) {
                    l = new BorderLayout();
                    if(root.isBorderLayoutAbsoluteCenter() != null) {
                        ((BorderLayout)l).setAbsoluteCenter(root.isBorderLayoutAbsoluteCenter().booleanValue());
                    }
                    if(root.getBorderLayoutSwapCenter() != null) {
                        ((BorderLayout)l).defineLandscapeSwap(BorderLayout.CENTER, root.getBorderLayoutSwapCenter());
                    }
                    if(root.getBorderLayoutSwapNorth()!= null) {
                        ((BorderLayout)l).defineLandscapeSwap(BorderLayout.NORTH, root.getBorderLayoutSwapNorth());
                    }
                    if(root.getBorderLayoutSwapSouth()!= null) {
                        ((BorderLayout)l).defineLandscapeSwap(BorderLayout.SOUTH, root.getBorderLayoutSwapSouth());
                    }
                    if(root.getBorderLayoutSwapEast()!= null) {
                        ((BorderLayout)l).defineLandscapeSwap(BorderLayout.EAST, root.getBorderLayoutSwapEast());
                    }
                    if(root.getBorderLayoutSwapWest()!= null) {
                        ((BorderLayout)l).defineLandscapeSwap(BorderLayout.WEST, root.getBorderLayoutSwapWest());
                    }
                } else {
                    if(root.getLayout().equals("FlowLayout")) {
                        l = new FlowLayout();
                        ((FlowLayout)l).setFillRows(root.isFlowLayoutFillRows());
                        ((FlowLayout)l).setAlign(root.getFlowLayoutAlign());
                        ((FlowLayout)l).setValign(root.getFlowLayoutValign());
                    } else {
                        if(root.getLayout().equals("GridLayout")) {
                            l = new GridLayout(root.getGridLayoutRows().intValue(), root.getGridLayoutColumns().intValue());
                        } else {
                            if(root.getLayout().equals("BoxLayout")) {
                                if(root.getBoxLayoutAxis().equals("X")) {
                                    l = new BoxLayout(BoxLayout.X_AXIS);
                                } else {
                                    l = new BoxLayout(BoxLayout.Y_AXIS);
                                }
                            } else {
                                if(root.getLayout().equals("TableLayout")) {
                                    l = new TableLayout(root.getTableLayoutRows(), root.getTableLayoutColumns());
                                } else {
                                    l = new LayeredLayout();
                                }
                            }
                        }
                    }
                }
                ((Container)c).setLayout(l);
            }
            
            if(parentContainer != null && root.getLayoutConstraint() != null) {
                modifyingProperty(c, PROPERTY_LAYOUT_CONSTRAINT);
                if(parentContainer.getLayout() instanceof BorderLayout) {
                    c.putClientProperty("layoutConstraint", root.getLayoutConstraint().getValue());
                } else {
                    TableLayout tl = (TableLayout)parentContainer.getLayout();
                    TableLayout.Constraint con = tl.createConstraint(root.getLayoutConstraint().getRow(), root.getLayoutConstraint().getColumn());
                    con.setHeightPercentage(root.getLayoutConstraint().getHeight());
                    con.setWidthPercentage(root.getLayoutConstraint().getWidth());
                    con.setHorizontalAlign(root.getLayoutConstraint().getAlign());
                    con.setHorizontalSpan(root.getLayoutConstraint().getSpanHorizontal());
                    con.setVerticalAlign(root.getLayoutConstraint().getValign());
                    con.setVerticalSpan(root.getLayoutConstraint().getSpanVertical());
                    c.putClientProperty("layoutConstraint", con);
                }
            }

            if(root.getEmbed() != null && root.getEmbed().length() > 0) {
                modifyingProperty(c, PROPERTY_EMBED);
                rootContainer.putClientProperty(EMBEDDED_FORM_FLAG, "");
                ((EmbeddedContainer)c).setEmbed(root.getEmbed());
                Container embed = createContainer(res, root.getEmbed(), (EmbeddedContainer)c);
                if(embed != null) {
                    if(embed instanceof Form) {
                        embed = formToContainer((Form)embed);
                    }
                    ((EmbeddedContainer)c).addComponent(BorderLayout.CENTER, embed);

                    // this isn't exactly the "right thing" but its the best we can do to make all
                    // use cases work
                    beforeShowContainer(embed);
                    postShowContainer(embed);
                }                
            }
            
            if(root.isToggle() != null) {
                modifyingProperty(c, PROPERTY_TOGGLE_BUTTON);
                ((Button)c).setToggle(root.isToggle().booleanValue());
            }
            
            if(root.getGroup() != null) {
                modifyingProperty(c, PROPERTY_RADIO_GROUP);
                ((RadioButton)c).setGroup(root.getGroup());
            }
            
            if(root.isSelected() != null) {
                modifyingProperty(c, PROPERTY_SELECTED);
                if(c instanceof RadioButton) {
                    ((RadioButton)c).setSelected(root.isSelected().booleanValue());
                } else {
                    ((CheckBox)c).setSelected(root.isSelected().booleanValue());
                }
            }
            
            if(root.isScrollableX() != null) {
                modifyingProperty(c, PROPERTY_SCROLLABLE_X);
                ((Container)c).setScrollableX(root.isScrollableX().booleanValue());
            }
            
            if(root.isScrollableY() != null) {
                modifyingProperty(c, PROPERTY_SCROLLABLE_Y);
                ((Container)c).setScrollableY(root.isScrollableY().booleanValue());
            }

            if(root.isTensileDragEnabled() != null) {
                modifyingProperty(c, PROPERTY_TENSILE_DRAG_ENABLED);
                c.setTensileDragEnabled(root.isTensileDragEnabled().booleanValue());
            }
            
            if(root.isTactileTouch()!= null) {
                modifyingProperty(c, PROPERTY_TACTILE_TOUCH);
                c.setTactileTouch(root.isTactileTouch().booleanValue());
            }
            
            if(root.isSnapToGrid()!= null) {
                modifyingProperty(c, PROPERTY_SNAP_TO_GRID);
                c.setSnapToGrid(root.isSnapToGrid().booleanValue());
            }
            
            if(root.isFlatten()!= null) {
                modifyingProperty(c, PROPERTY_FLATTEN);
                c.setFlatten(root.isFlatten().booleanValue());
            }
            
            if(root.getText() != null) {
                modifyingProperty(c, PROPERTY_TEXT);
                if(c instanceof Label) {
                    ((Label)c).setText(root.getText());
                } else {
                    ((TextArea)c).setText(root.getText());
                }
            }            
            
            if(root.getMaxSize() != null) {
                modifyingProperty(c, PROPERTY_TEXT_MAX_LENGTH);
                ((TextArea)c).setMaxSize(root.getMaxSize().intValue());
            }
            
            if(root.getConstraint() != null) {
                modifyingProperty(c, PROPERTY_TEXT_CONSTRAINT);
                ((TextArea)c).setConstraint(root.getConstraint().intValue());
            }
            
            if(root.getAlignment() != null) {
                modifyingProperty(c, PROPERTY_ALIGNMENT);
                if(c instanceof Label) {
                    ((Label)c).setAlignment(root.getAlignment().intValue());
                } else {
                    ((TextArea)c).setAlignment(root.getAlignment().intValue());
                }
            }

            if(root.isGrowByContent()!= null) {
                modifyingProperty(c, PROPERTY_TEXT_AREA_GROW);
                ((TextArea)c).setGrowByContent(root.isGrowByContent().booleanValue());
            }
            
            if(root.getTabPlacement() != null) {
                modifyingProperty(c, PROPERTY_TAB_PLACEMENT);
                ((Tabs)c).setTabPlacement(root.getTabPlacement().intValue());
            }

            if(root.getTabTextPosition() != null) {
                modifyingProperty(c, PROPERTY_TAB_TEXT_POSITION);
                ((Tabs)c).setTabTextPosition(root.getTabTextPosition().intValue());
            }
            
            if(root.getUiid() != null) {
                modifyingProperty(c, PROPERTY_UIID);
                c.setUIID(root.getUiid());
            }
                        
            if(root.getDialogUIID() != null) {
                modifyingProperty(c, PROPERTY_DIALOG_UIID);
                ((Dialog)c).setDialogUIID(root.getDialogUIID());
            }

            if(root.isDisposeWhenPointerOutOfBounds() != null) {
                modifyingProperty(c, PROPERTY_DISPOSE_WHEN_POINTER_OUT);
                ((Dialog)c).setDisposeWhenPointerOutOfBounds(root.isDisposeWhenPointerOutOfBounds());
            }

            if(root.getCloudBoundProperty() != null) {
                modifyingProperty(c, PROPERTY_CLOUD_BOUND_PROPERTY);
                c.setCloudBoundProperty(root.getCloudBoundProperty());
            }

            if(root.getCloudDestinationProperty()!= null) {
                modifyingProperty(c, PROPERTY_CLOUD_DESTINATION_PROPERTY);
                c.setCloudDestinationProperty(root.getCloudDestinationProperty());
            }
            
            if(root.getDialogPosition() != null && root.getDialogPosition().length() > 0) {
                modifyingProperty(c, PROPERTY_DIALOG_POSITION);
                ((Dialog)c).setDialogPosition(root.getDialogPosition());
            }
            
            if(root.isFocusable() != null) {
                modifyingProperty(c, PROPERTY_FOCUSABLE);
                c.setFocusable(root.isFocusable().booleanValue());
            }

            if(root.isEnabled()!= null) {
                modifyingProperty(c, PROPERTY_ENABLED);
                c.setEnabled(root.isEnabled().booleanValue());
            }
            
            if(root.isScrollVisible()!= null) {
                modifyingProperty(c, PROPERTY_SCROLL_VISIBLE);
                c.setScrollVisible(root.isScrollVisible().booleanValue());
            }
            
            if(root.getIcon() != null) {
                modifyingProperty(c, PROPERTY_ICON);
                ((Label)c).setIcon(res.getImage(root.getIcon()));
            }

            if(root.getRolloverIcon()!= null) {
                modifyingProperty(c, PROPERTY_ROLLOVER_ICON);
                ((Button)c).setRolloverIcon(res.getImage(root.getRolloverIcon()));
            }
            
            if(root.getPressedIcon()!= null) {
                modifyingProperty(c, PROPERTY_PRESSED_ICON);
                ((Button)c).setPressedIcon(res.getImage(root.getPressedIcon()));
            }
            
            if(root.getDisabledIcon()!= null) {
                modifyingProperty(c, PROPERTY_DISABLED_ICON);
                ((Button)c).setDisabledIcon(res.getImage(root.getDisabledIcon()));
            }

            if(root.getGap()!= null) {
                modifyingProperty(c, PROPERTY_GAP);
                ((Label)c).setGap(root.getGap().intValue());
            }
            
            if(root.getVerticalAlignment() != null) {
                modifyingProperty(c, PROPERTY_VERTICAL_ALIGNMENT);
                if(c instanceof Label) {
                    ((Label)c).setVerticalAlignment(root.getVerticalAlignment().intValue());
                } else {
                    ((TextArea)c).setVerticalAlignment(root.getVerticalAlignment().intValue());
                }
            }
            
            if(root.getTextPosition()!= null) {
                modifyingProperty(c, PROPERTY_TEXT_POSITION);
                ((Label)c).setTextPosition(root.getTextPosition().intValue());
            }
            
            if(root.getTitle() != null) {
                modifyingProperty(c, PROPERTY_TITLE);
                ((Form)c).setTitle(root.getTitle());
            }
            
            // components should be added when we've set everything else up
            if(root.getComponent() != null) {
                modifyingProperty(c, PROPERTY_COMPONENTS);
                if(c instanceof Tabs) {
                    for(ComponentEntry ent : root.getComponent()) {
                        Component newCmp = createInstance(ent, res, rootContainer, (Container)c, postCreateTasks);
                        ((Tabs)c).addTab(ent.getTabTitle(), newCmp);
                    }
                } else {
                    for(ComponentEntry ent : root.getComponent()) {
                        Component newCmp = createInstance(ent, res, rootContainer, (Container)c, postCreateTasks);
                        Object cons = newCmp.getClientProperty("layoutConstraint");
                        if(cons != null) {
                            modifyingProperty(c, PROPERTY_LAYOUT_CONSTRAINT);
                            ((Container)c).addComponent(cons, newCmp);
                        } else {
                            ((Container)c).addComponent(newCmp);
                        }
                    }
                }
            }
            
            if(root.getColumns() != null) {
                modifyingProperty(c, PROPERTY_COLUMNS);
                ((TextArea)c).setColumns(root.getColumns().intValue());
            }

            if(root.getRows() != null) {
                modifyingProperty(c, PROPERTY_ROWS);
                ((TextArea)c).setRows(root.getRows().intValue());
            }
            
            if(root.getHint()!= null) {
                modifyingProperty(c, PROPERTY_HINT);
                if(c instanceof List) {
                    ((List)c).setHint(root.getHint());
                } else {
                    ((TextArea)c).setHint(root.getHint());
                }
            }
            
            if(root.getHintIcon()!= null) {
                modifyingProperty(c, PROPERTY_HINT_ICON);
                if(c instanceof List) {
                    ((List)c).setHintIcon(res.getImage(root.getHint()));
                } else {
                    ((TextArea)c).setHintIcon(res.getImage(root.getHint()));
                }
            }
            
            if(root.getItemGap() != null) {
                modifyingProperty(c, PROPERTY_ITEM_GAP);
                ((List)c).setItemGap(root.getItemGap().intValue());
            }
            
            if(root.getFixedSelection() != null) {
                modifyingProperty(c, PROPERTY_LIST_FIXED);
                ((List)c).setFixedSelection(root.getFixedSelection().intValue());
            }
            
            if(root.getOrientation() != null) {
                modifyingProperty(c, PROPERTY_LIST_ORIENTATION);
                ((List)c).setOrientation(root.getOrientation().intValue());
            }
            
            if(c instanceof com.codename1.ui.List && !(c instanceof com.codename1.components.RSSReader)) {
                modifyingProperty(c, PROPERTY_LIST_ITEMS);
                if(root.getStringItem() != null && root.getStringItem().length > 0) {
                    String[] arr = new String[root.getStringItem().length];
                    for(int iter = 0 ; iter < arr.length ; iter++) {
                        arr[iter] = root.getStringItem()[iter].getValue();
                    }
                    ((List)c).setModel(new DefaultListModel<String>(arr));
                } else {
                    if(root.getMapItems() != null && root.getMapItems().length > 0) {
                        Hashtable[] arr = new Hashtable[root.getMapItems().length];
                        for(int iter = 0 ; iter < arr.length ; iter++) {
                            arr[iter] = new Hashtable();
                            if(root.getMapItems()[iter].getActionItem() != null) {
                                for(Val v : root.getMapItems()[iter].getActionItem()) {
                                    Command cmd = createCommandImpl((String)v.getValue(), null, -1, v.getValue(), false, "");
                                    cmd.putClientProperty(COMMAND_ACTION, (String)v.getValue());
                                    arr[iter].put(v.getKey(), cmd);
                                }
                            }
                            if(root.getMapItems()[iter].getStringItem()!= null) {
                                for(Val v : root.getMapItems()[iter].getActionItem()) {
                                    arr[iter].put(v.getKey(), v.getValue());
                                }
                            }
                            if(root.getMapItems()[iter].getImageItem()!= null) {
                                for(Val v : root.getMapItems()[iter].getActionItem()) {
                                    arr[iter].put(v.getKey(), res.getImage(v.getValue()));
                                }
                            }
                        }
                        ((List)c).setModel(new DefaultListModel<java.util.Map>(arr));
                    }
                }
            }
            
            if(root.getSelectedRenderer() != null) {
                modifyingProperty(c, PROPERTY_LIST_RENDERER);
                GenericListCellRenderer g;
                if(root.getSelectedRendererEven() == null) {
                    Component selected = createContainer(res, root.getSelectedRenderer());
                    Component unselected = createContainer(res, root.getUnselectedRenderer());
                    g = new GenericListCellRenderer(selected, unselected);
                    g.setFisheye(!root.getSelectedRenderer().equals(root.getUnselectedRenderer()));
                } else {
                    Component selected = createContainer(res, root.getSelectedRenderer());
                    Component unselected = createContainer(res, root.getUnselectedRenderer());
                    Component even = createContainer(res, root.getSelectedRendererEven());
                    Component evenU = createContainer(res, root.getUnselectedRendererEven());
                    g = new GenericListCellRenderer(selected, unselected, even, evenU);
                    g.setFisheye(!root.getSelectedRenderer().equals(root.getUnselectedRenderer()));
                }
                if(c instanceof ContainerList) {
                    ((ContainerList)c).setRenderer(g);
                } else {
                    ((List)c).setRenderer(g);
                }
            }
            
            if(root.getNextForm() != null && root.getNextForm().length() > 0) {
                modifyingProperty(c, PROPERTY_NEXT_FORM);
                setNextForm(c, root.getNextForm(), res, rootContainer);
            }
            
            if(root.getCommand() != null) {
                modifyingProperty(c, PROPERTY_COMMANDS);
                for(CommandEntry cmd : root.getCommand()) {
                    Command currentCommand = createCommandImpl(cmd.getName(), res.getImage(cmd.getIcon()), cmd.getId(), cmd.getAction(), cmd.isBackCommand(), cmd.getArgument());
                    if(cmd.getRolloverIcon() != null && cmd.getRolloverIcon().length() > 0) {
                        currentCommand.setRolloverIcon(res.getImage(cmd.getRolloverIcon()));
                    }
                    if(cmd.getPressedIcon()!= null && cmd.getPressedIcon().length() > 0) {
                        currentCommand.setPressedIcon(res.getImage(cmd.getPressedIcon()));
                    }
                    if(cmd.getDisabledIcon()!= null && cmd.getDisabledIcon().length() > 0) {
                        currentCommand.setDisabledIcon(res.getImage(cmd.getDisabledIcon()));
                    }
                    if(cmd.isBackCommand()) {
                        ((Form)c).setBackCommand(currentCommand);
                    }

                    ((Form)c).addCommand(currentCommand);
        
                    currentCommand.putClientProperty(COMMAND_ARGUMENTS, cmd.getArgument());
                    currentCommand.putClientProperty(COMMAND_ACTION, cmd.getAction());   
                }
            }
            
            if(root.isCyclicFocus() != null) {
                modifyingProperty(c, PROPERTY_CYCLIC_FOCUS);
                ((Form)c).setCyclicFocus(root.isCyclicFocus().booleanValue());
            }
            
            if(root.isRtl() != null) {
                modifyingProperty(c, PROPERTY_RTL);
                c.setRTL(root.isRtl().booleanValue());
            } 
            
            if(root.getThumbImage() != null) {
                modifyingProperty(c, PROPERTY_SLIDER_THUMB);
                ((Slider)c).setThumbImage(res.getImage(root.getThumbImage()));
            }

            if(root.isInfinite()!= null) {
                modifyingProperty(c, PROPERTY_INFINITE);
                ((Slider)c).setInfinite(root.isInfinite().booleanValue());
            }
            
            if(root.getProgress()!= null) {
                modifyingProperty(c, PROPERTY_PROGRESS);
                ((Slider)c).setProgress(root.getProgress().intValue());
            }
            
            if(root.isVertical()!= null) {
                modifyingProperty(c, PROPERTY_VERTICAL);
                ((Slider)c).setVertical(root.isVertical().booleanValue());
            }
            
            if(root.isEditable()!= null) {
                modifyingProperty(c, PROPERTY_EDITABLE);
                if(c instanceof TextArea) {
                    ((TextArea)c).setEditable(root.isEditable().booleanValue());
                } else {
                    ((Slider)c).setEditable(root.isEditable().booleanValue());
                }
            }

            if(root.getIncrements()!= null) {
                modifyingProperty(c, PROPERTY_INCREMENTS);
                ((Slider)c).setIncrements(root.getIncrements().intValue());
            }

            if(root.isRenderPercentageOnTop()!= null) {
                modifyingProperty(c, PROPERTY_RENDER_PERCENTAGE_ON_TOP);
                ((Slider)c).setRenderPercentageOnTop(root.isRenderPercentageOnTop().booleanValue());
            }

            if(root.getMaxValue()!= null) {
                modifyingProperty(c, PROPERTY_MAX_VALUE);
                ((Slider)c).setMaxValue(root.getMaxValue().intValue());
            }

            if(root.getMinValue()!= null) {
                modifyingProperty(c, PROPERTY_MIN_VALUE);
                ((Slider)c).setMinValue(root.getMinValue().intValue());
            }
            
            if(root.getCommandName() != null) {
                modifyingProperty(c, PROPERTY_COMMAND);
                postCreateTasks.add(new Runnable() {
                    public void run() {
                        Command cmd = createCommandImpl(root.getCommandName(), res.getImage(root.getCommandIcon()), root.getCommandId().intValue(), root.getCommandAction(), root.isCommandBack().booleanValue(), root.getCommandArgument());                        
                        if(c instanceof Container) {
                            Button b = (Button)((Container)c).getLeadComponent();
                            b.setCommand(cmd);
                            return;
                        }
                        ((Button)c).setCommand(cmd);
                    }
                });
            }

            if(root.getLabelFor() != null) {
                modifyingProperty(c, PROPERTY_LABEL_FOR);
                postCreateTasks.add(new Runnable() {
                    public void run() {
                        ((Label)c).setLabelForComponent((Label)findByName(root.getLabelFor(), rootContainer));
                    }
                });
            }

            if(root.getLeadComponent()!= null) {
                modifyingProperty(c, PROPERTY_LEAD_COMPONENT);
                postCreateTasks.add(new Runnable() {
                    public void run() {
                        ((Container)c).setLeadComponent(findByName(root.getLeadComponent(), rootContainer));
                    }
                });
            }
            
            if(root.getNextFocusUp() != null) {
                modifyingProperty(c, PROPERTY_NEXT_FOCUS_UP);
                postCreateTasks.add(new Runnable() {
                    public void run() {
                        c.setNextFocusUp(findByName(root.getNextFocusUp(), rootContainer));
                    }
                });
            }
            
            if(root.getNextFocusDown()!= null) {
                modifyingProperty(c, PROPERTY_NEXT_FOCUS_DOWN);
                postCreateTasks.add(new Runnable() {
                    public void run() {
                        c.setNextFocusDown(findByName(root.getNextFocusDown(), rootContainer));
                    }
                });
            }
            
            if(root.getNextFocusLeft()!= null) {
                modifyingProperty(c, PROPERTY_NEXT_FOCUS_LEFT);
                postCreateTasks.add(new Runnable() {
                    public void run() {
                        c.setNextFocusLeft(findByName(root.getNextFocusLeft(), rootContainer));
                    }
                });
            }

            if(root.getNextFocusRight()!= null) {
                modifyingProperty(c, PROPERTY_NEXT_FOCUS_RIGHT);
                postCreateTasks.add(new Runnable() {
                    public void run() {
                        c.setNextFocusRight(findByName(root.getNextFocusRight(), rootContainer));
                    }
                });
            }
            
            // custom settings are always last after all other properties
            if(root.getCustom() != null && root.getCustom().length > 0) {
                modifyingProperty(c, PROPERTY_CUSTOM);
                for(Custom cust : root.getCustom()) {
                    modifyingCustomProperty(c, cust.getName());
                    Object value = null;
                    Class customType = UserInterfaceEditor.getPropertyCustomType(c, cust.getName());
                    if(customType.isArray()) {
                        if(customType == String[].class) {
                            if(cust.getStr() != null) {
                                String[] arr = new String[cust.getStr().length];
                                for(int iter = 0 ; iter < arr.length ; iter++) {
                                    arr[iter] = cust.getStr()[iter].getValue();
                                }
                                c.setPropertyValue(cust.getName(), arr);
                            } else {
                                c.setPropertyValue(cust.getName(), null);
                            }
                            continue;
                        }
                     
                        if(customType == String[][].class) {
                            if(cust.getArr() != null) {
                                String[][] arr = new String[cust.getArr().length][];
                                for(int iter = 0 ; iter < arr.length ; iter++) {
                                    if(cust.getArr()[iter] != null && cust.getArr()[iter].getValue() != null) {
                                        arr[iter] = new String[cust.getArr()[iter].getValue().length];
                                        for(int inter = 0 ; inter < arr[iter].length ; inter++) {
                                            arr[iter][inter] = cust.getArr()[iter].getValue()[inter].getValue();
                                        }
                                    }
                                }
                                c.setPropertyValue(cust.getName(), arr);
                            } else {
                                c.setPropertyValue(cust.getName(), null);
                            }
                            continue;
                        }
                        
                        if(customType == com.codename1.ui.Image[].class) {
                            if(cust.getStr() != null) {
                                com.codename1.ui.Image[] arr = new com.codename1.ui.Image[cust.getStr().length];
                                for(int iter = 0 ; iter < arr.length ; iter++) {
                                    arr[iter] = res.getImage(cust.getStr()[iter].getValue());
                                }
                                c.setPropertyValue(cust.getName(), arr);
                            } else {
                                c.setPropertyValue(cust.getName(), null);
                            }
                            continue;
                        }
                        
                        if(customType == Object[].class) {
                            if(cust.getStringItem() != null) {
                                String[] arr = new String[cust.getStringItem().length];
                                for(int iter = 0 ; iter < arr.length ; iter++) {
                                    arr[iter] = cust.getStringItem()[iter].getValue();
                                }
                                c.setPropertyValue(cust.getName(), arr);
                                continue;
                            } else {
                                if(cust.getMapItems() != null) {
                                    Hashtable[] arr = new Hashtable[cust.getMapItems().length];
                                    for(int iter = 0 ; iter < arr.length ; iter++) {
                                        arr[iter] = new Hashtable();
                                        if(cust.getMapItems()[iter].getActionItem() != null) {
                                            for(Val v : cust.getMapItems()[iter].getActionItem()) {
                                                Command cmd = createCommandImpl(v.getValue(), null, -1, v.getValue(), false, "");
                                                cmd.putClientProperty(COMMAND_ACTION, v.getValue());
                                                value = cmd;
                                                arr[iter].put(v.getKey(), cmd);
                                            }
                                        }
                                        if(cust.getMapItems()[iter].getStringItem()!= null) {
                                            for(Val v : cust.getMapItems()[iter].getActionItem()) {
                                                arr[iter].put(v.getKey(), v.getValue());
                                            }
                                        }
                                        if(cust.getMapItems()[iter].getImageItem()!= null) {
                                            for(Val v : cust.getMapItems()[iter].getActionItem()) {
                                                arr[iter].put(v.getKey(), res.getImage(v.getValue()));
                                            }
                                        }
                                    }
                                    c.setPropertyValue(cust.getName(), arr);
                                    continue;
                                }
                            }
                            c.setPropertyValue(cust.getName(), null);
                            continue;
                        }
                    }
                    
                    if(customType == String.class) {
                        c.setPropertyValue(cust.getName(), cust.getValue());
                        continue;
                    }

                    if(customType == Integer.class) {
                        if(cust.getValue() != null) {
                            c.setPropertyValue(cust.getName(), Integer.valueOf(cust.getValue()));
                        } else {
                            c.setPropertyValue(cust.getName(), null);
                        }
                        continue;
                    }

                    if(customType == Long.class) {
                        if(cust.getValue() != null) {
                            c.setPropertyValue(cust.getName(), Long.valueOf(cust.getValue()));
                        } else {
                            c.setPropertyValue(cust.getName(), null);
                        }
                        continue;
                    }

                    if(customType == Double.class) {
                        if(cust.getValue() != null) {
                            c.setPropertyValue(cust.getName(), Double.valueOf(cust.getValue()));
                        } else {
                            c.setPropertyValue(cust.getName(), null);
                        }
                        continue;
                    }

                    if(customType == Date.class) {
                        if(cust.getValue() != null) {
                            c.setPropertyValue(cust.getName(), new Date(Long.parseLong(cust.getValue())));
                        } else {
                            c.setPropertyValue(cust.getName(), null);
                        }
                        continue;
                    }
                    
                    if(customType == Float.class) {
                        if(cust.getValue() != null) {
                            c.setPropertyValue(cust.getName(), Float.valueOf(cust.getValue()));
                        } else {
                            c.setPropertyValue(cust.getName(), null);
                        }
                        continue;
                    }

                    if(customType == Byte.class) {
                        if(cust.getValue() != null) {
                            c.setPropertyValue(cust.getName(), Byte.valueOf(cust.getValue()));
                        } else {
                            c.setPropertyValue(cust.getName(), null);
                        }
                        continue;
                    }

                    if(customType == Character.class) {
                        if(cust.getValue() != null && ((String)cust.getValue()).length() > 0) {
                            c.setPropertyValue(cust.getName(), new Character(((String)cust.getValue()).charAt(0)));
                        } else {
                            c.setPropertyValue(cust.getName(), null);
                        }
                        continue;
                    }

                    if(customType == Boolean.class) {
                        if(cust.getValue() != null) {
                            c.setPropertyValue(cust.getName(), Boolean.valueOf(cust.getValue()));
                        } else {
                            c.setPropertyValue(cust.getName(), null);
                        }
                        continue;
                    }

                    if(customType == com.codename1.ui.Image.class) {
                        if(cust.getValue() != null) {
                            c.setPropertyValue(cust.getName(), res.getImage(cust.getValue()));
                        } else {
                            c.setPropertyValue(cust.getName(), null);
                        }
                        continue;
                    }

                    if(customType == com.codename1.ui.Container.class) {
                        // resource might have been removed we need to fail gracefully
                        String[] uiNames = res.getUIResourceNames();
                        for(int iter = 0 ; iter < uiNames.length ; iter++) {
                            if(uiNames[iter].equals(cust.getName())) {
                                c.setPropertyValue(cust.getName(), createContainer(res, cust.getName()));
                                continue;
                            }
                        }
                        c.setPropertyValue(cust.getName(), null);
                        continue;
                    }

                    if(customType == com.codename1.ui.list.CellRenderer.class) {
                        if(cust.getUnselectedRenderer() != null) {
                            GenericListCellRenderer g;
                            if(cust.getSelectedRendererEven() == null) {
                                Component selected = createContainer(res, cust.getSelectedRenderer());
                                Component unselected = createContainer(res, cust.getUnselectedRenderer());
                                g = new GenericListCellRenderer(selected, unselected);
                                g.setFisheye(!cust.getSelectedRenderer().equals(cust.getUnselectedRenderer()));
                            } else {
                                Component selected = createContainer(res, cust.getSelectedRenderer());
                                Component unselected = createContainer(res, cust.getUnselectedRenderer());
                                Component even = createContainer(res, cust.getSelectedRendererEven());
                                Component evenU = createContainer(res, cust.getUnselectedRendererEven());
                                g = new GenericListCellRenderer(selected, unselected, even, evenU);
                                g.setFisheye(!cust.getSelectedRenderer().equals(cust.getUnselectedRenderer()));
                            }
                            c.setPropertyValue(cust.getName(), g);
                            continue;
                        }
                        c.setPropertyValue(cust.getName(), null);
                        continue;
                    }
                }
            }
            
            return c;
        } catch(Throwable t) {
            t.printStackTrace();
            JOptionPane.showMessageDialog(java.awt.Frame.getFrames()[0], "Error creating component: " + 
                    root.getName() + "\n" + t.toString() + "\ntrying to recover...", "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }
}
