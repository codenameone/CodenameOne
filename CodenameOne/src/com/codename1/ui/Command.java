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

import com.codename1.compat.java.util.Objects;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;

import java.util.HashMap;

/// The Command class provides a useful extension to the ActionListener
/// interface in cases where the same functionality may be accessed by several controls.
public class Command implements ActionListener<ActionEvent> {
    private boolean disposesDialog = true;
    private Image icon;
    private char materialIcon;
    private Font iconFont;
    private Image pressedIcon;
    private Image rolloverIcon;
    private Image disabledIcon;
    private String command;
    private boolean enabled = true;
    private float iconGapMM = -1;
    private float materialIconSize = -1;

    /// Simplifies code dealing with commands allowing them to be used in switch statements
    /// more easily
    private int commandId;

    private HashMap<String, Object> clientProperties;

    /// Creates a new instance of Command
    ///
    /// #### Parameters
    ///
    /// - `command`: the string that will be placed on the Soft buttons\Menu
    public Command(String command) {
        this.command = command;
    }

    /// Creates a new instance of Command
    ///
    /// #### Parameters
    ///
    /// - `command`: the string that will be placed on the Soft buttons\Menu
    ///
    /// - `icon`: the icon representing the command
    public Command(String command, Image icon) {
        this.command = command;
        this.icon = icon;
    }


    /// Creates a new instance of Command
    ///
    /// #### Parameters
    ///
    /// - `command`: the string that will be placed on the Soft buttons\Menu
    ///
    /// - `id`: @param id      user defined ID for a command simplifying switch statement code
    /// working with a command
    public Command(String command, int id) {
        this.command = command;
        this.commandId = id;
    }

    /// Creates a new instance of Command
    ///
    /// #### Parameters
    ///
    /// - `command`: the string that will be placed on the Soft buttons\Menu
    ///
    /// - `icon`: the icon representing the command
    ///
    /// - `id`: @param id      user defined ID for a command simplifying switch statement code
    /// working with a command
    public Command(String command, Image icon, int id) {
        this.command = command;
        this.commandId = id;
        this.icon = icon;
    }

    /// Creates a new command instance that encapsulates the action listener and details, the main
    /// value of this approach is in our ability to write commands using the shorthand lambda syntax
    /// of Java 8.
    ///
    /// #### Parameters
    ///
    /// - `name`: the name/title of the command
    ///
    /// - `icon`: the icon for the command
    ///
    /// - `ev`: the even handler
    ///
    /// #### Returns
    ///
    /// a newly created Command instance
    public static Command create(String name, Image icon, final ActionListener ev) {
        Command cmd = new Command(name) {
            @Override
            public void actionPerformed(ActionEvent evt) {
                ev.actionPerformed(evt);
            }
        };
        cmd.setIcon(icon);
        return cmd;
    }

    /// Creates a new command instance that encapsulates the action listener and details, the main
    /// value of this approach is in our ability to write commands using the shorthand lambda syntax
    /// of Java 8.
    ///
    /// #### Parameters
    ///
    /// - `name`: the name/title of the command
    ///
    /// - `icon`: the icon for the command
    ///
    /// - `ev`: the even handler
    ///
    /// #### Returns
    ///
    /// a newly created Command instance
    ///
    public static Command createMaterial(String name, char icon, final ActionListener ev) {
        Command cmd = new Command(name) {
            @Override
            public void actionPerformed(ActionEvent evt) {
                ev.actionPerformed(evt);
            }
        };
        cmd.setMaterialIcon(icon);
        return cmd;
    }

    /// Return the command ID
    ///
    /// #### Returns
    ///
    /// the command ID
    public int getId() {
        return commandId;
    }

    /// gets the Command Name
    ///
    /// #### Returns
    ///
    /// the Command name
    public String getCommandName() {
        return command;
    }

    /// sets the Command name
    ///
    /// #### Parameters
    ///
    /// - `command`
    public void setCommandName(String command) {
        this.command = command;
    }

    /// Returns the icon representing the command
    ///
    /// #### Returns
    ///
    /// an icon representing the command
    public Image getIcon() {
        return icon;
    }

    /// Sets the icon for the command
    ///
    /// #### Parameters
    ///
    /// - `icon`: the new icon
    public void setIcon(Image icon) {
        this.icon = icon;
    }

    /// Returns a string representation of the object
    ///
    /// #### Returns
    ///
    /// Returns a string representation of the object
    @Override
    public String toString() {
        return getCommandName();
    }

    /// Indicates the icon that is displayed on the button when the button is in
    /// pressed state
    ///
    /// #### Returns
    ///
    /// icon used
    public Image getPressedIcon() {
        return pressedIcon;
    }

    /// Indicates the icon that is displayed on the button when the button is in
    /// pressed state
    ///
    /// #### Parameters
    ///
    /// - `pressedIcon`: icon used
    public void setPressedIcon(Image pressedIcon) {
        this.pressedIcon = pressedIcon;
    }

    /// Indicates the icon that is displayed on the button when the button is in
    /// the disabled state
    ///
    /// #### Returns
    ///
    /// icon used
    public Image getDisabledIcon() {
        return disabledIcon;
    }

    /// Indicates the icon that is displayed on the button when the button is in
    /// the disabled state
    ///
    /// #### Parameters
    ///
    /// - `disabledIcon`: icon used
    public void setDisabledIcon(Image disabledIcon) {
        this.disabledIcon = disabledIcon;
    }

    /// Indicates the icon that is displayed on the button when the button is in
    /// rolled over state
    ///
    /// #### Returns
    ///
    /// icon used
    public Image getRolloverIcon() {
        return rolloverIcon;
    }

    /// Indicates the icon that is displayed on the button when the button is in
    /// rolled over state
    ///
    /// #### Parameters
    ///
    /// - `rolloverIcon`: icon to use
    public void setRolloverIcon(Image rolloverIcon) {
        this.rolloverIcon = rolloverIcon;
    }

    /// compare two commands
    ///
    /// #### Parameters
    ///
    /// - `obj`: a Command Object to compare
    ///
    /// #### Returns
    ///
    /// true if the obj has the same command name
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Command)) {
            return false;
        }
        if (((Command) obj).command == null) {
            return obj.getClass() == getClass() && command == null &&
                    ((Command) obj).icon == icon && ((Command) obj).commandId == commandId && //NOPMD CompareObjectsWithEquals
                    ((Command) obj).materialIcon == materialIcon && com.codename1.util.MathUtil.compare(((Command) obj).materialIconSize, materialIconSize) == 0 &&
                    (Objects.equals(clientProperties, ((Command) obj).clientProperties));
        } else {
            return obj.getClass() == getClass() && ((Command) obj).command.equals(command) &&
                    ((Command) obj).icon == icon && ((Command) obj).commandId == commandId && //NOPMD CompareObjectsWithEquals
                    ((Command) obj).materialIcon == materialIcon && com.codename1.util.MathUtil.compare(((Command) obj).materialIconSize, materialIconSize) == 0 &&
                    (Objects.equals(clientProperties, ((Command) obj).clientProperties));
        }
    }

    /// Allows storing commands in a vector/hashtable
    ///
    /// #### Returns
    ///
    /// unique hashcode for the command class
    @Override
    public int hashCode() {
        return getClass().hashCode() + commandId;
    }

    /// This method is called when the soft button/Menu item is clicked
    ///
    /// #### Parameters
    ///
    /// - `evt`: the Event Object
    @Override
    public void actionPerformed(ActionEvent evt) {
    }

    /// Indicates whether this command causes the dialog to dispose implicitly, defaults to true
    public boolean isDisposesDialog() {
        return disposesDialog;
    }

    /// Indicates whether this command causes the dialog to dispose implicitly, defaults to true
    public void setDisposesDialog(boolean disposesDialog) {
        this.disposesDialog = disposesDialog;
    }

    /// Allows disabling/enabling the command
    ///
    /// #### Returns
    ///
    /// the enabled
    public boolean isEnabled() {
        return enabled;
    }

    /// Allows disabling/enabling the command
    ///
    /// #### Parameters
    ///
    /// - `enabled`: the enabled to set
    public void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            Display.refreshNativeMenuIfPublished();
        }
    }

    /// The client properties are a useful way to associate meta-data with a command
    /// without subclassing
    ///
    /// #### Parameters
    ///
    /// - `key`: an arbitrary user key
    ///
    /// #### Returns
    ///
    /// an arbitrary user object
    public Object getClientProperty(String key) {
        if (clientProperties != null) {
            return clientProperties.get(key);
        }
        return null;
    }

    /// The client properties are a useful way to associate meta-data with a command
    /// without sub classing
    ///
    /// #### Parameters
    ///
    /// - `key`: an arbitrary user key
    ///
    /// - `value`: an arbitrary user object, null to remove
    public void putClientProperty(String key, Object value) {
        if (clientProperties == null) {
            clientProperties = new HashMap<String, Object>();
        }
        if (value == null) {
            clientProperties.remove(key);
        } else {
            clientProperties.put(key, value);
        }
    }

    /// Client property key carrying the desktop native-menu placement hint for this command.
    /// When a desktop app bridges its commands to the native menu bar (title-bar mode
    /// {@code native}/{@code custom}), the value selects which menu the command appears under.
    /// Recognized values (case-insensitive): `#DESKTOP_MENU_APP`, `#DESKTOP_MENU_ABOUT`,
    /// `#DESKTOP_MENU_PREFERENCES`, `#DESKTOP_MENU_QUIT`, `#DESKTOP_MENU_FILE`,
    /// `#DESKTOP_MENU_EDIT`, `#DESKTOP_MENU_VIEW`, `#DESKTOP_MENU_WINDOW`, `#DESKTOP_MENU_HELP`.
    /// Any other value becomes a top-level menu with that literal title. Commands without the
    /// hint are grouped under a default application-commands menu.
    ///
    /// The value is a path: {@code "File/Open Recent"} puts the command in an "Open Recent"
    /// submenu of the File menu, created on demand, and deeper paths nest further. The first
    /// segment keeps the meaning above. Write {@code //} for a slash that is part of a title.
    public static final String DESKTOP_MENU = "DesktopMenu";

    /// Standard desktop menu placement: the application menu (macOS app menu / a leading menu).
    public static final String DESKTOP_MENU_APP = "App";
    /// Standard desktop menu placement: an "About" item, conventionally in the application menu.
    public static final String DESKTOP_MENU_ABOUT = "About";
    /// Standard desktop menu placement: a "Preferences"/"Settings" item in the application menu.
    public static final String DESKTOP_MENU_PREFERENCES = "Preferences";
    /// Standard desktop menu placement: a "Quit"/"Exit" item in the application menu.
    public static final String DESKTOP_MENU_QUIT = "Quit";
    /// Standard desktop menu placement: the File menu.
    public static final String DESKTOP_MENU_FILE = "File";
    /// Standard desktop menu placement: the Edit menu.
    public static final String DESKTOP_MENU_EDIT = "Edit";
    /// Standard desktop menu placement: the View menu.
    public static final String DESKTOP_MENU_VIEW = "View";
    /// Standard desktop menu placement: the Window menu.
    public static final String DESKTOP_MENU_WINDOW = "Window";
    /// Standard desktop menu placement: the Help menu.
    public static final String DESKTOP_MENU_HELP = "Help";

    /// Sets the desktop native-menu placement hint for this command. See `#DESKTOP_MENU`.
    ///
    /// #### Parameters
    ///
    /// - `menu`: one of the {@code DESKTOP_MENU_*} constants or a custom top-level menu title
    ///
    /// #### Returns
    ///
    /// this command, for chaining
    public Command setDesktopMenu(String menu) {
        putClientProperty(DESKTOP_MENU, menu);
        return this;
    }

    /// Returns the desktop native-menu placement hint, or null when unset. See `#DESKTOP_MENU`.
    ///
    /// #### Returns
    ///
    /// the placement hint or null
    public String getDesktopMenu() {
        Object o = getClientProperty(DESKTOP_MENU);
        return o == null ? null : o.toString();
    }

    /// Client property key carrying the separator group of this command within its desktop menu
    /// (as an {@code Integer}), see `#setDesktopMenuGroup(int)`.
    public static final String DESKTOP_MENU_GROUP = "DesktopMenuGroup";

    /// Client property key marking a command as a separator line in the desktop menu, see
    /// `#createDesktopSeparator(String)`.
    public static final String DESKTOP_SEPARATOR = "DesktopSeparator";

    /// Client property key carrying the checked state of a check or radio desktop menu item
    /// (as a {@code Boolean}). Its presence makes the item checkable, see
    /// `#setDesktopChecked(boolean)`.
    public static final String DESKTOP_CHECKED = "DesktopChecked";

    /// Client property key carrying the name of the radio group of a desktop menu item, see
    /// `#setDesktopRadioGroup(String)`.
    public static final String DESKTOP_RADIO_GROUP = "DesktopRadioGroup";

    /// Places this command in a separator group within its desktop menu. Items of one menu are
    /// ordered by group, keeping the order they were added in within a group, and the native
    /// menu draws a separator line wherever the group changes. Commands default to group 0.
    ///
    /// #### Parameters
    ///
    /// - `group`: the group number, lower groups come first
    ///
    /// #### Returns
    ///
    /// this command, for chaining
    public Command setDesktopMenuGroup(int group) {
        putClientProperty(DESKTOP_MENU_GROUP, Integer.valueOf(group));
        return this;
    }

    /// Returns the separator group set with `#setDesktopMenuGroup(int)`, 0 when unset.
    ///
    /// #### Returns
    ///
    /// the group number
    public int getDesktopMenuGroup() {
        Object o = getClientProperty(DESKTOP_MENU_GROUP);
        return o instanceof Integer ? ((Integer) o).intValue() : 0;
    }

    private static int separatorCounter;

    /// Creates a command that shows as a separator line in a desktop native menu, at the
    /// position it is added in. It has no label and never runs. Where groups already
    /// separate the items (`#setDesktopMenuGroup(int)`) it is not needed.
    ///
    /// #### Parameters
    ///
    /// - `menu`: the menu path the separator goes in, see `#setDesktopMenu(String)`
    ///
    /// #### Returns
    ///
    /// the separator command
    public static Command createDesktopSeparator(String menu) {
        Command c = new Command("-");
        // A distinct value per separator: Command.equals compares client properties, and the
        // de-duplication that gathers the published commands would otherwise fold every
        // separator of a menu into the first one.
        c.putClientProperty(DESKTOP_SEPARATOR, Integer.valueOf(separatorCounter++));
        c.setDesktopMenu(menu);
        return c;
    }

    /// True when this command was created by `#createDesktopSeparator(String)`.
    ///
    /// #### Returns
    ///
    /// true for a separator
    public boolean isDesktopSeparator() {
        return getClientProperty(DESKTOP_SEPARATOR) != null;
    }

    /// Makes this command a check item in the desktop native menu and sets its state. When
    /// the user picks the item the state flips before the command runs, so the action reads
    /// the new value from `#isDesktopChecked()`. Changing the state while the command is in
    /// the native menu updates the menu.
    ///
    /// #### Parameters
    ///
    /// - `checked`: the checked state
    ///
    /// #### Returns
    ///
    /// this command, for chaining
    public Command setDesktopChecked(boolean checked) {
        Object old = getClientProperty(DESKTOP_CHECKED);
        Boolean b = checked ? Boolean.TRUE : Boolean.FALSE;
        if (b.equals(old)) {
            return this;
        }
        putClientProperty(DESKTOP_CHECKED, b);
        if (checked) {
            clearRadioSiblings();
        }
        Display.refreshNativeMenuIfPublished();
        return this;
    }

    /// True when this command is a check or radio item, see `#setDesktopChecked(boolean)`.
    ///
    /// #### Returns
    ///
    /// true when the item shows a check state
    public boolean isDesktopCheckable() {
        return getClientProperty(DESKTOP_CHECKED) != null || getDesktopRadioGroup() != null;
    }

    /// The checked state of a check or radio item, false when the command is not checkable.
    ///
    /// #### Returns
    ///
    /// the checked state
    public boolean isDesktopChecked() {
        return Boolean.TRUE.equals(getClientProperty(DESKTOP_CHECKED));
    }

    /// Makes this command a radio item of the named group in the desktop native menu. At most
    /// one command of a group is checked: checking one, from the menu or with
    /// `#setDesktopChecked(boolean)`, unchecks the others. Picking the checked item again
    /// leaves it checked.
    ///
    /// #### Parameters
    ///
    /// - `group`: the radio group name, null to make this an ordinary item again
    ///
    /// #### Returns
    ///
    /// this command, for chaining
    public Command setDesktopRadioGroup(String group) {
        putClientProperty(DESKTOP_RADIO_GROUP, group);
        if (group != null) {
            if (getClientProperty(DESKTOP_CHECKED) == null) {
                putClientProperty(DESKTOP_CHECKED, Boolean.FALSE);
            } else if (isDesktopChecked()) {
                clearRadioSiblings();
            }
        }
        return this;
    }

    /// Returns the radio group set with `#setDesktopRadioGroup(String)`, or null.
    ///
    /// #### Returns
    ///
    /// the radio group name or null
    public String getDesktopRadioGroup() {
        Object o = getClientProperty(DESKTOP_RADIO_GROUP);
        return o instanceof String ? (String) o : null;
    }

    private void clearRadioSiblings() {
        String group = getDesktopRadioGroup();
        if (group == null) {
            return;
        }
        // The group is found among the commands the current form publishes rather than in a
        // registry of its own: a static registry would hold every command, and through their
        // listeners every form, for the life of the application.
        java.util.Vector members = Display.getInstance().getCurrentNativeMenuCommands();
        for (int i = 0; i < members.size(); i++) {
            Object o = members.elementAt(i);
            if (!(o instanceof Command)) {
                continue;
            }
            Command c = (Command) o;
            if (c != this && group.equals(c.getDesktopRadioGroup()) && c.isDesktopChecked()) { //NOPMD CompareObjectsWithEquals
                c.putClientProperty(DESKTOP_CHECKED, Boolean.FALSE);
            }
        }
    }

    /// Client property key carrying the upper-cased character of the desktop keyboard
    /// accelerator (as an {@code Integer}), see `#setDesktopShortcut(char, int)`.
    public static final String DESKTOP_SHORTCUT_KEY = "DesktopShortcutKey";

    /// Client property key carrying the modifier bit-mask of the desktop keyboard accelerator
    /// (as an {@code Integer}), see `#setDesktopShortcut(char, int)`.
    public static final String DESKTOP_SHORTCUT_MODIFIERS = "DesktopShortcutModifiers";

    /// Accelerator modifier flag for the platform's primary command key: Command on macOS,
    /// Control on Windows/Linux. This is the modifier used by the vast majority of menu
    /// shortcuts so it's the default.
    public static final int DESKTOP_SHORTCUT_MODIFIER_PRIMARY = 1;

    /// Accelerator modifier flag for the Shift key.
    public static final int DESKTOP_SHORTCUT_MODIFIER_SHIFT = 2;

    /// Accelerator modifier flag for the Alt/Option key.
    public static final int DESKTOP_SHORTCUT_MODIFIER_ALT = 4;

    /// Assigns a keyboard accelerator to this command for the desktop native menu bar using the
    /// platform's primary modifier (Command on macOS, Control elsewhere). For example
    /// {@code setDesktopShortcut('S')} produces Cmd+S / Ctrl+S next to the menu item. The hint is
    /// inert on platforms without a native menu bar, so it's safe to set unconditionally.
    ///
    /// #### Parameters
    ///
    /// - `keyChar`: the accelerator character (case-insensitive)
    ///
    /// #### Returns
    ///
    /// this command, for chaining
    public Command setDesktopShortcut(char keyChar) {
        return setDesktopShortcut(keyChar, DESKTOP_SHORTCUT_MODIFIER_PRIMARY);
    }

    /// Assigns a keyboard accelerator to this command for the desktop native menu bar. See
    /// `#setDesktopShortcut(char)` for the common case.
    ///
    /// #### Parameters
    ///
    /// - `keyChar`: the accelerator character (case-insensitive)
    ///
    /// - `modifiers`: a bit-mask of `#DESKTOP_SHORTCUT_MODIFIER_PRIMARY`,
    /// `#DESKTOP_SHORTCUT_MODIFIER_SHIFT` and `#DESKTOP_SHORTCUT_MODIFIER_ALT`
    ///
    /// #### Returns
    ///
    /// this command, for chaining
    public Command setDesktopShortcut(char keyChar, int modifiers) {
        putClientProperty(DESKTOP_SHORTCUT_KEY, Integer.valueOf(Character.toUpperCase(keyChar)));
        putClientProperty(DESKTOP_SHORTCUT_MODIFIERS, Integer.valueOf(modifiers));
        return this;
    }

    /// Returns the upper-cased accelerator character assigned via `#setDesktopShortcut(char, int)`,
    /// or 0 when no accelerator is set.
    ///
    /// #### Returns
    ///
    /// the accelerator character or 0
    public int getDesktopShortcutKeyChar() {
        Object o = getClientProperty(DESKTOP_SHORTCUT_KEY);
        return o == null ? 0 : ((Integer) o).intValue();
    }

    /// Returns the accelerator modifier bit-mask assigned via `#setDesktopShortcut(char, int)`,
    /// or 0 when no accelerator is set.
    ///
    /// #### Returns
    ///
    /// the modifier bit-mask
    public int getDesktopShortcutModifiers() {
        Object o = getClientProperty(DESKTOP_SHORTCUT_MODIFIERS);
        return o == null ? 0 : ((Integer) o).intValue();
    }

    /// #### Returns
    ///
    /// the materialIcon
    public char getMaterialIcon() {
        return materialIcon;
    }

    /// #### Parameters
    ///
    /// - `materialIcon`: the materialIcon to set
    public void setMaterialIcon(char materialIcon) {
        this.materialIcon = materialIcon;
    }

    /// The gap between the text and the icon in millimeters or -1 for default
    ///
    /// #### Returns
    ///
    /// the iconGapMM
    public float getIconGapMM() {
        return iconGapMM;
    }

    /// The gap between the text and the icon in millimeters or -1 for default
    ///
    /// #### Parameters
    ///
    /// - `iconGapMM`: the iconGapMM to set
    public void setIconGapMM(float iconGapMM) {
        this.iconGapMM = iconGapMM;
    }

    /// #### Returns
    ///
    /// the materialIconSize
    public float getMaterialIconSize() {
        return materialIconSize;
    }

    /// #### Parameters
    ///
    /// - `materialIconSize`: the materialIconSize to set
    public void setMaterialIconSize(float materialIconSize) {
        this.materialIconSize = materialIconSize;
    }

    /// #### Returns
    ///
    /// the set iconFont or null if none defined (meaning for material icons instead of the system default MaterialDesign font
    public Font getIconFont() {
        return iconFont;
    }

    /// #### Parameters
    ///
    /// - `iconFont`: use iconFont for material icons instead of the system default MaterialDesign font
    public void setIconFont(Font iconFont) {
        this.iconFont = iconFont;
    }

}
