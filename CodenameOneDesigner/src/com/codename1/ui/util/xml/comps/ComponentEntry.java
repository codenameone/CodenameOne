/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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

package com.codename1.ui.util.xml.comps;

import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * XML representation for a component in the UI tree
 *
 * @author Shai Almog
 */
@XmlRootElement(name="component")
@XmlAccessorType(XmlAccessType.FIELD)
public class ComponentEntry {
    @XmlAttribute
    private String name;

    @XmlElement
    private Custom[] custom;
    
    @XmlElement 
    private LayoutConstraint layoutConstraint;
    
    @XmlElement 
    private ComponentEntry[] component;

    @XmlElement
    private StringEntry[] stringItem;
    
    @XmlElement
    private MapItems[] mapItems;
    
    @XmlAttribute
    private String type;

    @XmlAttribute
    private String baseForm;

    @XmlAttribute
    private String cloudBoundProperty;

    @XmlAttribute
    private String cloudDestinationProperty;

    @XmlAttribute
    private String embed;

    @XmlAttribute
    private String uiid;

    @XmlAttribute
    private Boolean focusable;

    @XmlAttribute
    private Boolean enabled;

    @XmlAttribute
    private Boolean rtl;

    @XmlAttribute
    private Boolean scrollVisible;

    @XmlAttribute
    private Boolean tensileDragEnabled;

    @XmlAttribute
    private Boolean tactileTouch;

    @XmlAttribute
    private Boolean snapToGrid;

    @XmlAttribute
    private Boolean flatten;

    @XmlAttribute
    private Boolean scrollableX;

    @XmlAttribute
    private Boolean scrollableY;

    @XmlAttribute
    private Integer tabPlacement;

    @XmlAttribute
    private Integer tabTextPosition;

    @XmlAttribute
    private String tabTitle;

    @XmlAttribute
    private String layout;

    @XmlAttribute
    private Boolean flowLayoutFillRows;

    @XmlAttribute
    private Integer flowLayoutAlign;

    @XmlAttribute
    private Integer flowLayoutValign;
    
    @XmlAttribute
    private Boolean borderLayoutAbsoluteCenter;

    @XmlAttribute
    private String borderLayoutSwapNorth;
    
    @XmlAttribute
    private String borderLayoutSwapEast;

    @XmlAttribute
    private String borderLayoutSwapWest;

    @XmlAttribute
    private String borderLayoutSwapSouth;

    @XmlAttribute
    private String borderLayoutSwapCenter;

    @XmlAttribute
    private Integer gridLayoutRows;

    @XmlAttribute
    private Integer gridLayoutColumns;

    @XmlAttribute
    private String boxLayoutAxis;

    @XmlAttribute
    private Integer tableLayoutRows;

    @XmlAttribute
    private Integer tableLayoutColumns;

    @XmlAttribute
    private String nextForm;

    @XmlAttribute
    private String title;

    @XmlAttribute
    private Boolean cyclicFocus;

    @XmlAttribute
    private String dialogUIID;

    @XmlAttribute
    private Boolean disposeWhenPointerOutOfBounds;

    @XmlAttribute
    private String dialogPosition;

    @XmlElement
    private CommandEntry[] command;

    @XmlAttribute
    private String selectedRenderer;
    
    @XmlAttribute
    private String unselectedRenderer;

    @XmlAttribute
    private String selectedRendererEven;

    @XmlAttribute
    private String unselectedRendererEven;

    @XmlAttribute
    private String text;

    @XmlAttribute
    private Integer alignment;

    @XmlAttribute
    private String icon;

    @XmlAttribute
    private String rolloverIcon;
    
    @XmlAttribute
    private String pressedIcon;
    
    @XmlAttribute
    private String disabledIcon;

    @XmlAttribute
    private Boolean toggle;

    @XmlAttribute
    private Boolean editable;

    @XmlAttribute
    private Boolean infinite;

    @XmlAttribute
    private String thumbImage;

    @XmlAttribute
    private Integer progress;

    @XmlAttribute
    private Boolean vertical;

    @XmlAttribute
    private Integer increments;

    @XmlAttribute
    private Integer maxValue;

    @XmlAttribute
    private Integer minValue;

    @XmlAttribute
    private Boolean renderPercentageOnTop;

    @XmlAttribute
    private String group;

    @XmlAttribute
    private Boolean selected;

    @XmlAttribute
    private Integer gap;

    @XmlAttribute
    private Integer verticalAlignment;

    @XmlAttribute
    private Integer textPosition;

    @XmlAttribute
    private Boolean growByContent;

    @XmlAttribute
    private Integer constraint;

    @XmlAttribute
    private Integer maxSize;

    @XmlAttribute
    private String hint;

    @XmlAttribute
    private String hintIcon;

    @XmlAttribute
    private Integer columns;

    @XmlAttribute
    private Integer rows;

    @XmlAttribute
    private Integer itemGap;

    @XmlAttribute
    private Integer fixedSelection;

    @XmlAttribute
    private Integer orientation;

    @XmlAttribute
    private String labelFor;
    
    @XmlAttribute
    private String leadComponent;
    
    @XmlAttribute
    private String nextFocusDown;
    
    @XmlAttribute
    private String nextFocusUp;
    
    @XmlAttribute
    private String nextFocusLeft;
    
    @XmlAttribute
    private String nextFocusRight;
    
    
    @XmlAttribute
    private String commandName;
    
    @XmlAttribute
    private String commandIcon;
    
    @XmlAttribute
    private String commandRolloverIcon;
    
    @XmlAttribute
    private String commandPressedIcon;
    
    @XmlAttribute
    private String commandDisabledIcon;
    
    @XmlAttribute
    private Integer commandId;
    
    @XmlAttribute
    private String commandAction;
    
    @XmlAttribute
    private String commandArgument;
    
    @XmlAttribute
    private Boolean commandBack;

    @XmlAttribute
    private String clientProperties;
    
    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the custom
     */
    public Custom[] getCustom() {
        return custom;
    }

    /**
     * @param custom the custom to set
     */
    public void setCustom(Custom[] custom) {
        this.custom = custom;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the baseForm
     */
    public String getBaseForm() {
        return baseForm;
    }

    /**
     * @param baseForm the baseForm to set
     */
    public void setBaseForm(String baseForm) {
        this.baseForm = baseForm;
    }

    /**
     * @return the cloudBoundProperty
     */
    public String getCloudBoundProperty() {
        return cloudBoundProperty;
    }

    /**
     * @param cloudBoundProperty the cloudBoundProperty to set
     */
    public void setCloudBoundProperty(String cloudBoundProperty) {
        this.cloudBoundProperty = cloudBoundProperty;
    }

    /**
     * @return the cloudDestinationProperty
     */
    public String getCloudDestinationProperty() {
        return cloudDestinationProperty;
    }

    /**
     * @param cloudDestinationProperty the cloudDestinationProperty to set
     */
    public void setCloudDestinationProperty(String cloudDestinationProperty) {
        this.cloudDestinationProperty = cloudDestinationProperty;
    }

    /**
     * @return the embed
     */
    public String getEmbed() {
        return embed;
    }

    /**
     * @param embed the embed to set
     */
    public void setEmbed(String embed) {
        this.embed = embed;
    }

    /**
     * @return the uiid
     */
    public String getUiid() {
        return uiid;
    }

    /**
     * @param uiid the uiid to set
     */
    public void setUiid(String uiid) {
        this.uiid = uiid;
    }

    /**
     * @return the focusable
     */
    public Boolean isFocusable() {
        return focusable;
    }

    /**
     * @param focusable the focusable to set
     */
    public void setFocusable(Boolean focusable) {
        this.focusable = focusable;
    }

    /**
     * @return the enabled
     */
    public Boolean isEnabled() {
        return enabled;
    }

    /**
     * @param enabled the enabled to set
     */
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @return the rtl
     */
    public Boolean isRtl() {
        return rtl;
    }

    /**
     * @param rtl the rtl to set
     */
    public void setRtl(Boolean rtl) {
        this.rtl = rtl;
    }

    /**
     * @return the scrollVisible
     */
    public Boolean isScrollVisible() {
        return scrollVisible;
    }

    /**
     * @param scrollVisible the scrollVisible to set
     */
    public void setScrollVisible(Boolean scrollVisible) {
        this.scrollVisible = scrollVisible;
    }

    /**
     * @return the tensileDragEnabled
     */
    public Boolean isTensileDragEnabled() {
        return tensileDragEnabled;
    }

    /**
     * @param tensileDragEnabled the tensileDragEnabled to set
     */
    public void setTensileDragEnabled(Boolean tensileDragEnabled) {
        this.tensileDragEnabled = tensileDragEnabled;
    }

    /**
     * @return the tactileTouch
     */
    public Boolean isTactileTouch() {
        return tactileTouch;
    }

    /**
     * @param tactileTouch the tactileTouch to set
     */
    public void setTactileTouch(Boolean tactileTouch) {
        this.tactileTouch = tactileTouch;
    }

    /**
     * @return the snapToGrid
     */
    public Boolean isSnapToGrid() {
        return snapToGrid;
    }

    /**
     * @param snapToGrid the snapToGrid to set
     */
    public void setSnapToGrid(Boolean snapToGrid) {
        this.snapToGrid = snapToGrid;
    }

    /**
     * @return the flatten
     */
    public Boolean isFlatten() {
        return flatten;
    }

    /**
     * @param flatten the flatten to set
     */
    public void setFlatten(Boolean flatten) {
        this.flatten = flatten;
    }

    /**
     * @return the scrollableX
     */
    public Boolean isScrollableX() {
        return scrollableX;
    }

    /**
     * @param scrollableX the scrollableX to set
     */
    public void setScrollableX(Boolean scrollableX) {
        this.scrollableX = scrollableX;
    }

    /**
     * @return the scrollableY
     */
    public Boolean isScrollableY() {
        return scrollableY;
    }

    /**
     * @param scrollableY the scrollableY to set
     */
    public void setScrollableY(Boolean scrollableY) {
        this.scrollableY = scrollableY;
    }

    /**
     * @return the tabPlacement
     */
    public Integer getTabPlacement() {
        return tabPlacement;
    }

    /**
     * @param tabPlacement the tabPlacement to set
     */
    public void setTabPlacement(Integer tabPlacement) {
        this.tabPlacement = tabPlacement;
    }

    /**
     * @return the tabTextPosition
     */
    public Integer getTabTextPosition() {
        return tabTextPosition;
    }

    /**
     * @param tabTextPosition the tabTextPosition to set
     */
    public void setTabTextPosition(Integer tabTextPosition) {
        this.tabTextPosition = tabTextPosition;
    }

    /**
     * @return the tabTitle
     */
    public String getTabTitle() {
        return tabTitle;
    }

    /**
     * @param tabTitle the tabTitle to set
     */
    public void setTabTitle(String tabTitle) {
        this.tabTitle = tabTitle;
    }

    /**
     * @return the layout
     */
    public String getLayout() {
        return layout;
    }

    /**
     * @param layout the layout to set
     */
    public void setLayout(String layout) {
        this.layout = layout;
    }

    /**
     * @return the flowLayoutFillRows
     */
    public Boolean isFlowLayoutFillRows() {
        return flowLayoutFillRows;
    }

    /**
     * @param flowLayoutFillRows the flowLayoutFillRows to set
     */
    public void setFlowLayoutFillRows(Boolean flowLayoutFillRows) {
        this.flowLayoutFillRows = flowLayoutFillRows;
    }

    /**
     * @return the flowLayoutAlign
     */
    public Integer getFlowLayoutAlign() {
        return flowLayoutAlign;
    }

    /**
     * @param flowLayoutAlign the flowLayoutAlign to set
     */
    public void setFlowLayoutAlign(Integer flowLayoutAlign) {
        this.flowLayoutAlign = flowLayoutAlign;
    }

    /**
     * @return the flowLayoutValign
     */
    public Integer getFlowLayoutValign() {
        return flowLayoutValign;
    }

    /**
     * @param flowLayoutValign the flowLayoutValign to set
     */
    public void setFlowLayoutValign(Integer flowLayoutValign) {
        this.flowLayoutValign = flowLayoutValign;
    }

    /**
     * @return the borderLayoutAbsoluteCenter
     */
    public Boolean isBorderLayoutAbsoluteCenter() {
        return borderLayoutAbsoluteCenter;
    }

    /**
     * @param borderLayoutAbsoluteCenter the borderLayoutAbsoluteCenter to set
     */
    public void setBorderLayoutAbsoluteCenter(Boolean borderLayoutAbsoluteCenter) {
        this.borderLayoutAbsoluteCenter = borderLayoutAbsoluteCenter;
    }

    /**
     * @return the borderLayoutSwapNorth
     */
    public String getBorderLayoutSwapNorth() {
        return borderLayoutSwapNorth;
    }

    /**
     * @param borderLayoutSwapNorth the borderLayoutSwapNorth to set
     */
    public void setBorderLayoutSwapNorth(String borderLayoutSwapNorth) {
        this.borderLayoutSwapNorth = borderLayoutSwapNorth;
    }

    /**
     * @return the borderLayoutSwapEast
     */
    public String getBorderLayoutSwapEast() {
        return borderLayoutSwapEast;
    }

    /**
     * @param borderLayoutSwapEast the borderLayoutSwapEast to set
     */
    public void setBorderLayoutSwapEast(String borderLayoutSwapEast) {
        this.borderLayoutSwapEast = borderLayoutSwapEast;
    }

    /**
     * @return the borderLayoutSwapWest
     */
    public String getBorderLayoutSwapWest() {
        return borderLayoutSwapWest;
    }

    /**
     * @param borderLayoutSwapWest the borderLayoutSwapWest to set
     */
    public void setBorderLayoutSwapWest(String borderLayoutSwapWest) {
        this.borderLayoutSwapWest = borderLayoutSwapWest;
    }

    /**
     * @return the borderLayoutSwapSouth
     */
    public String getBorderLayoutSwapSouth() {
        return borderLayoutSwapSouth;
    }

    /**
     * @param borderLayoutSwapSouth the borderLayoutSwapSouth to set
     */
    public void setBorderLayoutSwapSouth(String borderLayoutSwapSouth) {
        this.borderLayoutSwapSouth = borderLayoutSwapSouth;
    }

    /**
     * @return the borderLayoutSwapCenter
     */
    public String getBorderLayoutSwapCenter() {
        return borderLayoutSwapCenter;
    }

    /**
     * @param borderLayoutSwapCenter the borderLayoutSwapCenter to set
     */
    public void setBorderLayoutSwapCenter(String borderLayoutSwapCenter) {
        this.borderLayoutSwapCenter = borderLayoutSwapCenter;
    }

    /**
     * @return the gridLayoutRows
     */
    public Integer getGridLayoutRows() {
        return gridLayoutRows;
    }

    /**
     * @param gridLayoutRows the gridLayoutRows to set
     */
    public void setGridLayoutRows(Integer gridLayoutRows) {
        this.gridLayoutRows = gridLayoutRows;
    }

    /**
     * @return the gridLayoutColumns
     */
    public Integer getGridLayoutColumns() {
        return gridLayoutColumns;
    }

    /**
     * @param gridLayoutColumns the gridLayoutColumns to set
     */
    public void setGridLayoutColumns(Integer gridLayoutColumns) {
        this.gridLayoutColumns = gridLayoutColumns;
    }

    /**
     * @return the boxLayoutAxis
     */
    public String getBoxLayoutAxis() {
        return boxLayoutAxis;
    }

    /**
     * @param boxLayoutAxis the boxLayoutAxis to set
     */
    public void setBoxLayoutAxis(String boxLayoutAxis) {
        this.boxLayoutAxis = boxLayoutAxis;
    }

    /**
     * @return the tableLayoutRows
     */
    public Integer getTableLayoutRows() {
        return tableLayoutRows;
    }

    /**
     * @param tableLayoutRows the tableLayoutRows to set
     */
    public void setTableLayoutRows(Integer tableLayoutRows) {
        this.tableLayoutRows = tableLayoutRows;
    }

    /**
     * @return the tableLayoutColumns
     */
    public Integer getTableLayoutColumns() {
        return tableLayoutColumns;
    }

    /**
     * @param tableLayoutColumns the tableLayoutColumns to set
     */
    public void setTableLayoutColumns(Integer tableLayoutColumns) {
        this.tableLayoutColumns = tableLayoutColumns;
    }

    /**
     * @return the nextForm
     */
    public String getNextForm() {
        return nextForm;
    }

    /**
     * @param nextForm the nextForm to set
     */
    public void setNextForm(String nextForm) {
        this.nextForm = nextForm;
    }

    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return the cyclicFocus
     */
    public Boolean isCyclicFocus() {
        return cyclicFocus;
    }

    /**
     * @param cyclicFocus the cyclicFocus to set
     */
    public void setCyclicFocus(Boolean cyclicFocus) {
        this.cyclicFocus = cyclicFocus;
    }

    /**
     * @return the dialogUIID
     */
    public String getDialogUIID() {
        return dialogUIID;
    }

    /**
     * @param dialogUIID the dialogUIID to set
     */
    public void setDialogUIID(String dialogUIID) {
        this.dialogUIID = dialogUIID;
    }

    /**
     * @return the disposeWhenPointerOutOfBounds
     */
    public Boolean isDisposeWhenPointerOutOfBounds() {
        return disposeWhenPointerOutOfBounds;
    }

    /**
     * @param disposeWhenPointerOutOfBounds the disposeWhenPointerOutOfBounds to set
     */
    public void setDisposeWhenPointerOutOfBounds(Boolean disposeWhenPointerOutOfBounds) {
        this.disposeWhenPointerOutOfBounds = disposeWhenPointerOutOfBounds;
    }

    /**
     * @return the dialogPosition
     */
    public String getDialogPosition() {
        return dialogPosition;
    }

    /**
     * @param dialogPosition the dialogPosition to set
     */
    public void setDialogPosition(String dialogPosition) {
        this.dialogPosition = dialogPosition;
    }

    /**
     * @return the command
     */
    public CommandEntry[] getCommand() {
        return command;
    }

    /**
     * @param command the command to set
     */
    public void setCommand(CommandEntry[] command) {
        this.command = command;
    }

    /**
     * @return the selectedRenderer
     */
    public String getSelectedRenderer() {
        return selectedRenderer;
    }

    /**
     * @param selectedRenderer the selectedRenderer to set
     */
    public void setSelectedRenderer(String selectedRenderer) {
        this.selectedRenderer = selectedRenderer;
    }

    /**
     * @return the unselectedRenderer
     */
    public String getUnselectedRenderer() {
        return unselectedRenderer;
    }

    /**
     * @param unselectedRenderer the unselectedRenderer to set
     */
    public void setUnselectedRenderer(String unselectedRenderer) {
        this.unselectedRenderer = unselectedRenderer;
    }

    /**
     * @return the selectedRendererEven
     */
    public String getSelectedRendererEven() {
        return selectedRendererEven;
    }

    /**
     * @param selectedRendererEven the selectedRendererEven to set
     */
    public void setSelectedRendererEven(String selectedRendererEven) {
        this.selectedRendererEven = selectedRendererEven;
    }

    /**
     * @return the unselectedRendererEven
     */
    public String getUnselectedRendererEven() {
        return unselectedRendererEven;
    }

    /**
     * @param unselectedRendererEven the unselectedRendererEven to set
     */
    public void setUnselectedRendererEven(String unselectedRendererEven) {
        this.unselectedRendererEven = unselectedRendererEven;
    }

    /**
     * @return the text
     */
    public String getText() {
        return text;
    }

    /**
     * @param text the text to set
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * @return the alignment
     */
    public Integer getAlignment() {
        return alignment;
    }

    /**
     * @param alignment the alignment to set
     */
    public void setAlignment(Integer alignment) {
        this.alignment = alignment;
    }

    /**
     * @return the icon
     */
    public String getIcon() {
        return icon;
    }

    /**
     * @param icon the icon to set
     */
    public void setIcon(String icon) {
        this.icon = icon;
    }

    /**
     * @return the rolloverIcon
     */
    public String getRolloverIcon() {
        return rolloverIcon;
    }

    /**
     * @param rolloverIcon the rolloverIcon to set
     */
    public void setRolloverIcon(String rolloverIcon) {
        this.rolloverIcon = rolloverIcon;
    }

    /**
     * @return the pressedIcon
     */
    public String getPressedIcon() {
        return pressedIcon;
    }

    /**
     * @param pressedIcon the pressedIcon to set
     */
    public void setPressedIcon(String pressedIcon) {
        this.pressedIcon = pressedIcon;
    }

    /**
     * @return the disabledIcon
     */
    public String getDisabledIcon() {
        return disabledIcon;
    }

    /**
     * @param disabledIcon the disabledIcon to set
     */
    public void setDisabledIcon(String disabledIcon) {
        this.disabledIcon = disabledIcon;
    }

    /**
     * @return the toggle
     */
    public Boolean isToggle() {
        return toggle;
    }

    /**
     * @param toggle the toggle to set
     */
    public void setToggle(Boolean toggle) {
        this.toggle = toggle;
    }

    /**
     * @return the editable
     */
    public Boolean isEditable() {
        return editable;
    }

    /**
     * @param editable the editable to set
     */
    public void setEditable(Boolean editable) {
        this.editable = editable;
    }

    /**
     * @return the infinite
     */
    public Boolean isInfinite() {
        return infinite;
    }

    /**
     * @param infinite the infinite to set
     */
    public void setInfinite(Boolean infinite) {
        this.infinite = infinite;
    }

    /**
     * @return the thumbImage
     */
    public String getThumbImage() {
        return thumbImage;
    }

    /**
     * @param thumbImage the thumbImage to set
     */
    public void setThumbImage(String thumbImage) {
        this.thumbImage = thumbImage;
    }

    /**
     * @return the progress
     */
    public Integer getProgress() {
        return progress;
    }

    /**
     * @param progress the progress to set
     */
    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    /**
     * @return the vertical
     */
    public Boolean isVertical() {
        return vertical;
    }

    /**
     * @param vertical the vertical to set
     */
    public void setVertical(Boolean vertical) {
        this.vertical = vertical;
    }

    /**
     * @return the increments
     */
    public Integer getIncrements() {
        return increments;
    }

    /**
     * @param increments the increments to set
     */
    public void setIncrements(Integer increments) {
        this.increments = increments;
    }

    /**
     * @return the maxValue
     */
    public Integer getMaxValue() {
        return maxValue;
    }

    /**
     * @param maxValue the maxValue to set
     */
    public void setMaxValue(Integer maxValue) {
        this.maxValue = maxValue;
    }

    /**
     * @return the minValue
     */
    public Integer getMinValue() {
        return minValue;
    }

    /**
     * @param minValue the minValue to set
     */
    public void setMinValue(Integer minValue) {
        this.minValue = minValue;
    }

    /**
     * @return the renderPercentageOnTop
     */
    public Boolean isRenderPercentageOnTop() {
        return renderPercentageOnTop;
    }

    /**
     * @param renderPercentageOnTop the renderPercentageOnTop to set
     */
    public void setRenderPercentageOnTop(Boolean renderPercentageOnTop) {
        this.renderPercentageOnTop = renderPercentageOnTop;
    }

    /**
     * @return the group
     */
    public String getGroup() {
        return group;
    }

    /**
     * @param group the group to set
     */
    public void setGroup(String group) {
        this.group = group;
    }

    /**
     * @return the selected
     */
    public Boolean isSelected() {
        return selected;
    }

    /**
     * @param selected the selected to set
     */
    public void setSelected(Boolean selected) {
        this.selected = selected;
    }

    /**
     * @return the gap
     */
    public Integer getGap() {
        return gap;
    }

    /**
     * @param gap the gap to set
     */
    public void setGap(Integer gap) {
        this.gap = gap;
    }

    /**
     * @return the verticalAlignment
     */
    public Integer getVerticalAlignment() {
        return verticalAlignment;
    }

    /**
     * @param verticalAlignment the verticalAlignment to set
     */
    public void setVerticalAlignment(Integer verticalAlignment) {
        this.verticalAlignment = verticalAlignment;
    }

    /**
     * @return the textPosition
     */
    public Integer getTextPosition() {
        return textPosition;
    }

    /**
     * @param textPosition the textPosition to set
     */
    public void setTextPosition(Integer textPosition) {
        this.textPosition = textPosition;
    }

    /**
     * @return the growByContent
     */
    public Boolean isGrowByContent() {
        return growByContent;
    }

    /**
     * @param growByContent the growByContent to set
     */
    public void setGrowByContent(Boolean growByContent) {
        this.growByContent = growByContent;
    }

    /**
     * @return the constraint
     */
    public Integer getConstraint() {
        return constraint;
    }

    /**
     * @param constraint the constraint to set
     */
    public void setConstraint(Integer constraint) {
        this.constraint = constraint;
    }

    /**
     * @return the maxSize
     */
    public Integer getMaxSize() {
        return maxSize;
    }

    /**
     * @param maxSize the maxSize to set
     */
    public void setMaxSize(Integer maxSize) {
        this.maxSize = maxSize;
    }

    /**
     * @return the hint
     */
    public String getHint() {
        return hint;
    }

    /**
     * @param hint the hint to set
     */
    public void setHint(String hint) {
        this.hint = hint;
    }

    /**
     * @return the hintIcon
     */
    public String getHintIcon() {
        return hintIcon;
    }

    /**
     * @param hintIcon the hintIcon to set
     */
    public void setHintIcon(String hintIcon) {
        this.hintIcon = hintIcon;
    }

    /**
     * @return the columns
     */
    public Integer getColumns() {
        return columns;
    }

    /**
     * @param columns the columns to set
     */
    public void setColumns(Integer columns) {
        this.columns = columns;
    }

    /**
     * @return the rows
     */
    public Integer getRows() {
        return rows;
    }

    /**
     * @param rows the rows to set
     */
    public void setRows(Integer rows) {
        this.rows = rows;
    }

    /**
     * @return the itemGap
     */
    public Integer getItemGap() {
        return itemGap;
    }

    /**
     * @param itemGap the itemGap to set
     */
    public void setItemGap(Integer itemGap) {
        this.itemGap = itemGap;
    }

    /**
     * @return the fixedSelection
     */
    public Integer getFixedSelection() {
        return fixedSelection;
    }

    /**
     * @param fixedSelection the fixedSelection to set
     */
    public void setFixedSelection(Integer fixedSelection) {
        this.fixedSelection = fixedSelection;
    }

    /**
     * @return the orientation
     */
    public Integer getOrientation() {
        return orientation;
    }

    /**
     * @param orientation the orientation to set
     */
    public void setOrientation(Integer orientation) {
        this.orientation = orientation;
    }

    /**
     * @return the labelFor
     */
    public String getLabelFor() {
        return labelFor;
    }

    /**
     * @param labelFor the labelFor to set
     */
    public void setLabelFor(String labelFor) {
        this.labelFor = labelFor;
    }

    /**
     * @return the leadComponent
     */
    public String getLeadComponent() {
        return leadComponent;
    }

    /**
     * @param leadComponent the leadComponent to set
     */
    public void setLeadComponent(String leadComponent) {
        this.leadComponent = leadComponent;
    }

    /**
     * @return the nextFocusDown
     */
    public String getNextFocusDown() {
        return nextFocusDown;
    }

    /**
     * @param nextFocusDown the nextFocusDown to set
     */
    public void setNextFocusDown(String nextFocusDown) {
        this.nextFocusDown = nextFocusDown;
    }

    /**
     * @return the nextFocusUp
     */
    public String getNextFocusUp() {
        return nextFocusUp;
    }

    /**
     * @param nextFocusUp the nextFocusUp to set
     */
    public void setNextFocusUp(String nextFocusUp) {
        this.nextFocusUp = nextFocusUp;
    }

    /**
     * @return the nextFocusLeft
     */
    public String getNextFocusLeft() {
        return nextFocusLeft;
    }

    /**
     * @param nextFocusLeft the nextFocusLeft to set
     */
    public void setNextFocusLeft(String nextFocusLeft) {
        this.nextFocusLeft = nextFocusLeft;
    }

    /**
     * @return the nextFocusRight
     */
    public String getNextFocusRight() {
        return nextFocusRight;
    }

    /**
     * @param nextFocusRight the nextFocusRight to set
     */
    public void setNextFocusRight(String nextFocusRight) {
        this.nextFocusRight = nextFocusRight;
    }

    /**
     * @return the commandName
     */
    public String getCommandName() {
        return commandName;
    }

    /**
     * @param commandName the commandName to set
     */
    public void setCommandName(String commandName) {
        this.commandName = commandName;
    }

    /**
     * @return the commandIcon
     */
    public String getCommandIcon() {
        return commandIcon;
    }

    /**
     * @param commandIcon the commandIcon to set
     */
    public void setCommandIcon(String commandIcon) {
        this.commandIcon = commandIcon;
    }

    /**
     * @return the commandRolloverIcon
     */
    public String getCommandRolloverIcon() {
        return commandRolloverIcon;
    }

    /**
     * @param commandRolloverIcon the commandRolloverIcon to set
     */
    public void setCommandRolloverIcon(String commandRolloverIcon) {
        this.commandRolloverIcon = commandRolloverIcon;
    }

    /**
     * @return the commandPressedIcon
     */
    public String getCommandPressedIcon() {
        return commandPressedIcon;
    }

    /**
     * @param commandPressedIcon the commandPressedIcon to set
     */
    public void setCommandPressedIcon(String commandPressedIcon) {
        this.commandPressedIcon = commandPressedIcon;
    }

    /**
     * @return the commandDisabledIcon
     */
    public String getCommandDisabledIcon() {
        return commandDisabledIcon;
    }

    /**
     * @param commandDisabledIcon the commandDisabledIcon to set
     */
    public void setCommandDisabledIcon(String commandDisabledIcon) {
        this.commandDisabledIcon = commandDisabledIcon;
    }

    /**
     * @return the commandId
     */
    public Integer getCommandId() {
        return commandId;
    }

    /**
     * @param commandId the commandId to set
     */
    public void setCommandId(Integer commandId) {
        this.commandId = commandId;
    }

    /**
     * @return the commandAction
     */
    public String getCommandAction() {
        return commandAction;
    }

    /**
     * @param commandAction the commandAction to set
     */
    public void setCommandAction(String commandAction) {
        this.commandAction = commandAction;
    }

    /**
     * @return the commandArgument
     */
    public String getCommandArgument() {
        return commandArgument;
    }

    /**
     * @param commandArgument the commandArgument to set
     */
    public void setCommandArgument(String commandArgument) {
        this.commandArgument = commandArgument;
    }

    /**
     * @return the commandBack
     */
    public Boolean isCommandBack() {
        return commandBack;
    }

    /**
     * @param commandBack the commandBack to set
     */
    public void setCommandBack(Boolean commandBack) {
        this.commandBack = commandBack;
    }

    /**
     * @return the layoutConstraint
     */
    public LayoutConstraint getLayoutConstraint() {
        return layoutConstraint;
    }

    /**
     * @param layoutConstraint the layoutConstraint to set
     */
    public void setLayoutConstraint(LayoutConstraint layoutConstraint) {
        this.layoutConstraint = layoutConstraint;
    }

    /**
     * @return the component
     */
    public ComponentEntry[] getComponent() {
        return component;
    }

    /**
     * @param component the component to set
     */
    public void setComponent(ComponentEntry[] component) {
        this.component = component;
    }

    /**
     * @return the stringItem
     */
    public StringEntry[] getStringItem() {
        return stringItem;
    }

    /**
     * @param stringItem the stringItem to set
     */
    public void setStringItem(StringEntry[] stringItem) {
        this.stringItem = stringItem;
    }

    /**
     * @return the mapItems
     */
    public MapItems[] getMapItems() {
        return mapItems;
    }

    /**
     * @param mapItems the mapItems to set
     */
    public void setMapItems(MapItems[] mapItems) {
        this.mapItems = mapItems;
    }
    
    public void findEmbeddedDependencies(List<String> result) {
        if(type.equals("EmbeddedContainer")) {
            result.add(embed);
        }
        
        if(component != null) {
            for(ComponentEntry c : component) {
                c.findEmbeddedDependencies(result);
            }
        }
    }

    /**
     * @return the clientProperties
     */
    public String getClientProperties() {
        return clientProperties;
    }

    /**
     * @param clientProperties the clientProperties to set
     */
    public void setClientProperties(String clientProperties) {
        this.clientProperties = clientProperties;
    }
}
