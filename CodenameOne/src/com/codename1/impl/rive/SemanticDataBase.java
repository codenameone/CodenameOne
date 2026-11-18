/*
 * MIT License
 *
 * Copyright (c) 2020 Rive
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Java rewrite of rive-runtime for Codename One.
 * Codename One modifications Copyright (c) 2026 Codename One.
 */
package com.codename1.impl.rive;

/// Generated from rive-runtime's `SemanticDataBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `SemanticData`.
public abstract class SemanticDataBase extends Component {
    public static final int TYPE_KEY = 668;
    public static final int ROLE_PROPERTY_KEY = 982;
    public static final int LABEL_PROPERTY_KEY = 983;
    public static final int VALUE_PROPERTY_KEY = 984;
    public static final int HINT_PROPERTY_KEY = 985;
    public static final int HEADING_LEVEL_PROPERTY_KEY = 986;
    public static final int TRAIT_FLAGS_PROPERTY_KEY = 987;
    public static final int STATE_FLAGS_PROPERTY_KEY = 988;
    public static final int IS_EXPANDABLE_PROPERTY_KEY = 989;
    public static final int IS_SELECTABLE_PROPERTY_KEY = 990;
    public static final int IS_CHECKABLE_PROPERTY_KEY = 991;
    public static final int IS_TOGGLEABLE_PROPERTY_KEY = 992;
    public static final int IS_REQUIRABLE_PROPERTY_KEY = 993;
    public static final int IS_ENABLABLE_PROPERTY_KEY = 994;
    public static final int IS_FOCUSABLE_PROPERTY_KEY = 995;
    public static final int IS_EXPANDED_PROPERTY_KEY = 996;
    public static final int IS_SELECTED_PROPERTY_KEY = 997;
    public static final int IS_CHECKED_PROPERTY_KEY = 998;
    public static final int IS_TOGGLED_PROPERTY_KEY = 1000;
    public static final int IS_REQUIRED_PROPERTY_KEY = 1001;
    public static final int IS_DISABLED_PROPERTY_KEY = 1002;
    public static final int IS_FOCUSED_PROPERTY_KEY = 1003;
    public static final int IS_HIDDEN_PROPERTY_KEY = 1004;
    public static final int IS_LIVE_REGION_PROPERTY_KEY = 1005;
    public static final int IS_READ_ONLY_PROPERTY_KEY = 1006;
    public static final int IS_MODAL_PROPERTY_KEY = 1007;
    public static final int IS_OBSCURED_PROPERTY_KEY = 1008;
    public static final int IS_MULTILINE_PROPERTY_KEY = 1009;

    protected int mRole = 0;
    protected String mLabel = "";
    protected String mValue = "";
    protected String mHint = "";
    protected int mHeadingLevel = 0;
    protected int mTraitFlags = 0;
    protected int mStateFlags = 0;
    protected boolean mIsExpandable = false;
    protected boolean mIsSelectable = false;
    protected boolean mIsCheckable = false;
    protected boolean mIsToggleable = false;
    protected boolean mIsRequirable = false;
    protected boolean mIsEnablable = false;
    protected boolean mIsFocusable = false;
    protected boolean mIsExpanded = false;
    protected boolean mIsSelected = false;
    protected int mIsChecked = 0;
    protected boolean mIsToggled = false;
    protected boolean mIsRequired = false;
    protected boolean mIsDisabled = false;
    protected boolean mIsFocused = false;
    protected boolean mIsHidden = false;
    protected boolean mIsLiveRegion = false;
    protected boolean mIsReadOnly = false;
    protected boolean mIsModal = false;
    protected boolean mIsObscured = false;
    protected boolean mIsMultiline = false;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public int getRole() {
        return mRole;
    }

    public void setRole(int value) {
        if (mRole == value) {
            return;
        }
        mRole = value;
        roleChanged();
    }

    protected void roleChanged() {
    }

    public String getLabel() {
        return mLabel;
    }

    public void setLabel(String value) {
        if ((mLabel == null ? value == null : mLabel.equals(value))) {
            return;
        }
        mLabel = value;
        labelChanged();
    }

    protected void labelChanged() {
    }

    public String getValue() {
        return mValue;
    }

    public void setValue(String value) {
        if ((mValue == null ? value == null : mValue.equals(value))) {
            return;
        }
        mValue = value;
        valueChanged();
    }

    protected void valueChanged() {
    }

    public String getHint() {
        return mHint;
    }

    public void setHint(String value) {
        if ((mHint == null ? value == null : mHint.equals(value))) {
            return;
        }
        mHint = value;
        hintChanged();
    }

    protected void hintChanged() {
    }

    public int getHeadingLevel() {
        return mHeadingLevel;
    }

    public void setHeadingLevel(int value) {
        if (mHeadingLevel == value) {
            return;
        }
        mHeadingLevel = value;
        headingLevelChanged();
    }

    protected void headingLevelChanged() {
    }

    public int getTraitFlags() {
        return mTraitFlags;
    }

    public void setTraitFlags(int value) {
        if (mTraitFlags == value) {
            return;
        }
        mTraitFlags = value;
        traitFlagsChanged();
    }

    protected void traitFlagsChanged() {
    }

    public int getStateFlags() {
        return mStateFlags;
    }

    public void setStateFlags(int value) {
        if (mStateFlags == value) {
            return;
        }
        mStateFlags = value;
        stateFlagsChanged();
    }

    protected void stateFlagsChanged() {
    }

    public boolean isExpandable() {
        return mIsExpandable;
    }

    public void setIsExpandable(boolean value) {
        if (mIsExpandable == value) {
            return;
        }
        mIsExpandable = value;
        isExpandableChanged();
    }

    protected void isExpandableChanged() {
    }

    public boolean isSelectable() {
        return mIsSelectable;
    }

    public void setIsSelectable(boolean value) {
        if (mIsSelectable == value) {
            return;
        }
        mIsSelectable = value;
        isSelectableChanged();
    }

    protected void isSelectableChanged() {
    }

    public boolean isCheckable() {
        return mIsCheckable;
    }

    public void setIsCheckable(boolean value) {
        if (mIsCheckable == value) {
            return;
        }
        mIsCheckable = value;
        isCheckableChanged();
    }

    protected void isCheckableChanged() {
    }

    public boolean isToggleable() {
        return mIsToggleable;
    }

    public void setIsToggleable(boolean value) {
        if (mIsToggleable == value) {
            return;
        }
        mIsToggleable = value;
        isToggleableChanged();
    }

    protected void isToggleableChanged() {
    }

    public boolean isRequirable() {
        return mIsRequirable;
    }

    public void setIsRequirable(boolean value) {
        if (mIsRequirable == value) {
            return;
        }
        mIsRequirable = value;
        isRequirableChanged();
    }

    protected void isRequirableChanged() {
    }

    public boolean isEnablable() {
        return mIsEnablable;
    }

    public void setIsEnablable(boolean value) {
        if (mIsEnablable == value) {
            return;
        }
        mIsEnablable = value;
        isEnablableChanged();
    }

    protected void isEnablableChanged() {
    }

    public boolean isFocusable() {
        return mIsFocusable;
    }

    public void setIsFocusable(boolean value) {
        if (mIsFocusable == value) {
            return;
        }
        mIsFocusable = value;
        isFocusableChanged();
    }

    protected void isFocusableChanged() {
    }

    public boolean isExpanded() {
        return mIsExpanded;
    }

    public void setIsExpanded(boolean value) {
        if (mIsExpanded == value) {
            return;
        }
        mIsExpanded = value;
        isExpandedChanged();
    }

    protected void isExpandedChanged() {
    }

    public boolean isSelected() {
        return mIsSelected;
    }

    public void setIsSelected(boolean value) {
        if (mIsSelected == value) {
            return;
        }
        mIsSelected = value;
        isSelectedChanged();
    }

    protected void isSelectedChanged() {
    }

    public int getIsChecked() {
        return mIsChecked;
    }

    public void setIsChecked(int value) {
        if (mIsChecked == value) {
            return;
        }
        mIsChecked = value;
        isCheckedChanged();
    }

    protected void isCheckedChanged() {
    }

    public boolean isToggled() {
        return mIsToggled;
    }

    public void setIsToggled(boolean value) {
        if (mIsToggled == value) {
            return;
        }
        mIsToggled = value;
        isToggledChanged();
    }

    protected void isToggledChanged() {
    }

    public boolean isRequired() {
        return mIsRequired;
    }

    public void setIsRequired(boolean value) {
        if (mIsRequired == value) {
            return;
        }
        mIsRequired = value;
        isRequiredChanged();
    }

    protected void isRequiredChanged() {
    }

    public boolean isDisabled() {
        return mIsDisabled;
    }

    public void setIsDisabled(boolean value) {
        if (mIsDisabled == value) {
            return;
        }
        mIsDisabled = value;
        isDisabledChanged();
    }

    protected void isDisabledChanged() {
    }

    public boolean isFocused() {
        return mIsFocused;
    }

    public void setIsFocused(boolean value) {
        if (mIsFocused == value) {
            return;
        }
        mIsFocused = value;
        isFocusedChanged();
    }

    protected void isFocusedChanged() {
    }

    public boolean isHidden() {
        return mIsHidden;
    }

    public void setIsHidden(boolean value) {
        if (mIsHidden == value) {
            return;
        }
        mIsHidden = value;
        isHiddenChanged();
    }

    protected void isHiddenChanged() {
    }

    public boolean isLiveRegion() {
        return mIsLiveRegion;
    }

    public void setIsLiveRegion(boolean value) {
        if (mIsLiveRegion == value) {
            return;
        }
        mIsLiveRegion = value;
        isLiveRegionChanged();
    }

    protected void isLiveRegionChanged() {
    }

    public boolean isReadOnly() {
        return mIsReadOnly;
    }

    public void setIsReadOnly(boolean value) {
        if (mIsReadOnly == value) {
            return;
        }
        mIsReadOnly = value;
        isReadOnlyChanged();
    }

    protected void isReadOnlyChanged() {
    }

    public boolean isModal() {
        return mIsModal;
    }

    public void setIsModal(boolean value) {
        if (mIsModal == value) {
            return;
        }
        mIsModal = value;
        isModalChanged();
    }

    protected void isModalChanged() {
    }

    public boolean isObscured() {
        return mIsObscured;
    }

    public void setIsObscured(boolean value) {
        if (mIsObscured == value) {
            return;
        }
        mIsObscured = value;
        isObscuredChanged();
    }

    protected void isObscuredChanged() {
    }

    public boolean isMultiline() {
        return mIsMultiline;
    }

    public void setIsMultiline(boolean value) {
        if (mIsMultiline == value) {
            return;
        }
        mIsMultiline = value;
        isMultilineChanged();
    }

    protected void isMultilineChanged() {
    }

    @Override
    public boolean deserializeBool(int propertyKey, boolean value) {
        switch (propertyKey) {
            case IS_EXPANDABLE_PROPERTY_KEY:
                mIsExpandable = value;
                return true;
            case IS_SELECTABLE_PROPERTY_KEY:
                mIsSelectable = value;
                return true;
            case IS_CHECKABLE_PROPERTY_KEY:
                mIsCheckable = value;
                return true;
            case IS_TOGGLEABLE_PROPERTY_KEY:
                mIsToggleable = value;
                return true;
            case IS_REQUIRABLE_PROPERTY_KEY:
                mIsRequirable = value;
                return true;
            case IS_ENABLABLE_PROPERTY_KEY:
                mIsEnablable = value;
                return true;
            case IS_FOCUSABLE_PROPERTY_KEY:
                mIsFocusable = value;
                return true;
            case IS_EXPANDED_PROPERTY_KEY:
                mIsExpanded = value;
                return true;
            case IS_SELECTED_PROPERTY_KEY:
                mIsSelected = value;
                return true;
            case IS_TOGGLED_PROPERTY_KEY:
                mIsToggled = value;
                return true;
            case IS_REQUIRED_PROPERTY_KEY:
                mIsRequired = value;
                return true;
            case IS_DISABLED_PROPERTY_KEY:
                mIsDisabled = value;
                return true;
            case IS_FOCUSED_PROPERTY_KEY:
                mIsFocused = value;
                return true;
            case IS_HIDDEN_PROPERTY_KEY:
                mIsHidden = value;
                return true;
            case IS_LIVE_REGION_PROPERTY_KEY:
                mIsLiveRegion = value;
                return true;
            case IS_READ_ONLY_PROPERTY_KEY:
                mIsReadOnly = value;
                return true;
            case IS_MODAL_PROPERTY_KEY:
                mIsModal = value;
                return true;
            case IS_OBSCURED_PROPERTY_KEY:
                mIsObscured = value;
                return true;
            case IS_MULTILINE_PROPERTY_KEY:
                mIsMultiline = value;
                return true;
            default:
                return super.deserializeBool(propertyKey, value);
        }
    }

    @Override
    public boolean setBoolProperty(int propertyKey, boolean value) {
        switch (propertyKey) {
            case IS_EXPANDABLE_PROPERTY_KEY:
                setIsExpandable(value);
                return true;
            case IS_SELECTABLE_PROPERTY_KEY:
                setIsSelectable(value);
                return true;
            case IS_CHECKABLE_PROPERTY_KEY:
                setIsCheckable(value);
                return true;
            case IS_TOGGLEABLE_PROPERTY_KEY:
                setIsToggleable(value);
                return true;
            case IS_REQUIRABLE_PROPERTY_KEY:
                setIsRequirable(value);
                return true;
            case IS_ENABLABLE_PROPERTY_KEY:
                setIsEnablable(value);
                return true;
            case IS_FOCUSABLE_PROPERTY_KEY:
                setIsFocusable(value);
                return true;
            case IS_EXPANDED_PROPERTY_KEY:
                setIsExpanded(value);
                return true;
            case IS_SELECTED_PROPERTY_KEY:
                setIsSelected(value);
                return true;
            case IS_TOGGLED_PROPERTY_KEY:
                setIsToggled(value);
                return true;
            case IS_REQUIRED_PROPERTY_KEY:
                setIsRequired(value);
                return true;
            case IS_DISABLED_PROPERTY_KEY:
                setIsDisabled(value);
                return true;
            case IS_FOCUSED_PROPERTY_KEY:
                setIsFocused(value);
                return true;
            case IS_HIDDEN_PROPERTY_KEY:
                setIsHidden(value);
                return true;
            case IS_LIVE_REGION_PROPERTY_KEY:
                setIsLiveRegion(value);
                return true;
            case IS_READ_ONLY_PROPERTY_KEY:
                setIsReadOnly(value);
                return true;
            case IS_MODAL_PROPERTY_KEY:
                setIsModal(value);
                return true;
            case IS_OBSCURED_PROPERTY_KEY:
                setIsObscured(value);
                return true;
            case IS_MULTILINE_PROPERTY_KEY:
                setIsMultiline(value);
                return true;
            default:
                return super.setBoolProperty(propertyKey, value);
        }
    }

    @Override
    public boolean getBoolProperty(int propertyKey) {
        switch (propertyKey) {
            case IS_EXPANDABLE_PROPERTY_KEY:
                return mIsExpandable;
            case IS_SELECTABLE_PROPERTY_KEY:
                return mIsSelectable;
            case IS_CHECKABLE_PROPERTY_KEY:
                return mIsCheckable;
            case IS_TOGGLEABLE_PROPERTY_KEY:
                return mIsToggleable;
            case IS_REQUIRABLE_PROPERTY_KEY:
                return mIsRequirable;
            case IS_ENABLABLE_PROPERTY_KEY:
                return mIsEnablable;
            case IS_FOCUSABLE_PROPERTY_KEY:
                return mIsFocusable;
            case IS_EXPANDED_PROPERTY_KEY:
                return mIsExpanded;
            case IS_SELECTED_PROPERTY_KEY:
                return mIsSelected;
            case IS_TOGGLED_PROPERTY_KEY:
                return mIsToggled;
            case IS_REQUIRED_PROPERTY_KEY:
                return mIsRequired;
            case IS_DISABLED_PROPERTY_KEY:
                return mIsDisabled;
            case IS_FOCUSED_PROPERTY_KEY:
                return mIsFocused;
            case IS_HIDDEN_PROPERTY_KEY:
                return mIsHidden;
            case IS_LIVE_REGION_PROPERTY_KEY:
                return mIsLiveRegion;
            case IS_READ_ONLY_PROPERTY_KEY:
                return mIsReadOnly;
            case IS_MODAL_PROPERTY_KEY:
                return mIsModal;
            case IS_OBSCURED_PROPERTY_KEY:
                return mIsObscured;
            case IS_MULTILINE_PROPERTY_KEY:
                return mIsMultiline;
            default:
                return super.getBoolProperty(propertyKey);
        }
    }

    @Override
    public boolean deserializeString(int propertyKey, String value) {
        switch (propertyKey) {
            case LABEL_PROPERTY_KEY:
                mLabel = value;
                return true;
            case VALUE_PROPERTY_KEY:
                mValue = value;
                return true;
            case HINT_PROPERTY_KEY:
                mHint = value;
                return true;
            default:
                return super.deserializeString(propertyKey, value);
        }
    }

    @Override
    public boolean setStringProperty(int propertyKey, String value) {
        switch (propertyKey) {
            case LABEL_PROPERTY_KEY:
                setLabel(value);
                return true;
            case VALUE_PROPERTY_KEY:
                setValue(value);
                return true;
            case HINT_PROPERTY_KEY:
                setHint(value);
                return true;
            default:
                return super.setStringProperty(propertyKey, value);
        }
    }

    @Override
    public String getStringProperty(int propertyKey) {
        switch (propertyKey) {
            case LABEL_PROPERTY_KEY:
                return mLabel;
            case VALUE_PROPERTY_KEY:
                return mValue;
            case HINT_PROPERTY_KEY:
                return mHint;
            default:
                return super.getStringProperty(propertyKey);
        }
    }

    @Override
    public boolean deserializeUint(int propertyKey, int value) {
        switch (propertyKey) {
            case ROLE_PROPERTY_KEY:
                mRole = value;
                return true;
            case HEADING_LEVEL_PROPERTY_KEY:
                mHeadingLevel = value;
                return true;
            case TRAIT_FLAGS_PROPERTY_KEY:
                mTraitFlags = value;
                return true;
            case STATE_FLAGS_PROPERTY_KEY:
                mStateFlags = value;
                return true;
            case IS_CHECKED_PROPERTY_KEY:
                mIsChecked = value;
                return true;
            default:
                return super.deserializeUint(propertyKey, value);
        }
    }

    @Override
    public boolean setUintProperty(int propertyKey, int value) {
        switch (propertyKey) {
            case ROLE_PROPERTY_KEY:
                setRole(value);
                return true;
            case HEADING_LEVEL_PROPERTY_KEY:
                setHeadingLevel(value);
                return true;
            case TRAIT_FLAGS_PROPERTY_KEY:
                setTraitFlags(value);
                return true;
            case STATE_FLAGS_PROPERTY_KEY:
                setStateFlags(value);
                return true;
            case IS_CHECKED_PROPERTY_KEY:
                setIsChecked(value);
                return true;
            default:
                return super.setUintProperty(propertyKey, value);
        }
    }

    @Override
    public int getUintProperty(int propertyKey) {
        switch (propertyKey) {
            case ROLE_PROPERTY_KEY:
                return mRole;
            case HEADING_LEVEL_PROPERTY_KEY:
                return mHeadingLevel;
            case TRAIT_FLAGS_PROPERTY_KEY:
                return mTraitFlags;
            case STATE_FLAGS_PROPERTY_KEY:
                return mStateFlags;
            case IS_CHECKED_PROPERTY_KEY:
                return mIsChecked;
            default:
                return super.getUintProperty(propertyKey);
        }
    }
}
