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
package com.codename1.ui;

import com.codename1.ui.animations.Animation;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.plaf.UIManager;

/// The contract shared by the two things that can sit at the root of a Codename One
/// component hierarchy: `Form`, which fills the application's main surface, and
/// `Window`, which is a separate native operating system window on the desktop.
///
/// Code that needs to work against "whatever top level I happen to be in" should
/// resolve it with `Component#getTopLevelContainer()` and talk to it through this
/// interface, rather than through `Component#getComponentForm()`. The latter keeps
/// its original meaning and returns `null` for a component hosted inside a `Window`.
///
/// Members that belong to a `Component` or a `Container` are deliberately absent --
/// reach them through `#asContainer()` instead. So are the parts of `Form` that model
/// mobile navigation, such as form transitions, the back command and the menu bar;
/// those have no meaning for a desktop window.
///
/// @author Shai Almog
public interface TopLevelContainer {

    /// Returns this top level as a `Container`.
    ///
    /// A Java interface cannot extend a class, so without this a `TopLevelContainer`
    /// reference could not be handed to anything expecting a `Component`.
    ///
    /// #### Returns
    ///
    /// this instance, as a `Container`
    /// Records a component that is waiting for a pointer release, so the top level can
    /// release it if the gesture ends somewhere else.
    ///
    /// #### Parameters
    ///
    /// - `c`: the component awaiting a release
    <C extends Component> void addComponentAwaitingRelease(C c);

    /// Stops tracking a component that was waiting for a pointer release.
    ///
    /// #### Parameters
    ///
    /// - `c`: the component to stop tracking
    <C extends Component> void removeComponentAwaitingRelease(C c);

    /// Drops every component waiting for a pointer release, used when a gesture is
    /// taken over by something else -- a pull to refresh, for instance.
    void clearComponentsAwaitingRelease();

    Container asContainer();

    // ---- content and structure ------------------------------------------------

    /// Returns the container holding the application content of this top level.
    ///
    /// #### Returns
    ///
    /// the content pane
    Container getContentPane();

    /// Returns the area reserved for the title and its surrounding chrome.
    ///
    /// #### Returns
    ///
    /// the title area container
    Container getTitleArea();

    /// Returns the layered pane covering the content area, creating it if needed.
    ///
    /// #### Returns
    ///
    /// the layered pane
    Container getLayeredPane();

    /// Returns the layer belonging to the given class within the content-area
    /// layered pane, creating it if needed.
    ///
    /// #### Parameters
    ///
    /// - `c`: the class owning the layer
    ///
    /// - `top`: true to place the layer above the existing layers
    ///
    /// #### Returns
    ///
    /// the layer for the given class
    Container getLayeredPane(Class c, boolean top);

    /// Returns the layer belonging to the given class within the content-area
    /// layered pane at an explicit depth, creating it if needed.
    ///
    /// #### Parameters
    ///
    /// - `c`: the class owning the layer
    ///
    /// - `zIndex`: the depth at which the layer should sit
    ///
    /// #### Returns
    ///
    /// the layer for the given class
    Container getLayeredPane(Class c, int zIndex);

    /// Returns the layer belonging to the given class within the layered pane that
    /// spans the whole top level, including the title area, creating it if needed.
    ///
    /// #### Parameters
    ///
    /// - `c`: the class owning the layer
    ///
    /// - `top`: true to place the layer above the existing layers
    ///
    /// #### Returns
    ///
    /// the layer for the given class
    Container getFormLayeredPane(Class c, boolean top);

    /// Returns the painter drawn above everything else in this top level.
    ///
    /// #### Returns
    ///
    /// the glass pane painter, or null when none is installed
    Painter getGlassPane();

    /// Sets the painter drawn above everything else in this top level.
    ///
    /// #### Parameters
    ///
    /// - `glassPane`: the painter to install, or null to remove the current one
    void setGlassPane(Painter glassPane);

    // ---- title ----------------------------------------------------------------

    /// Returns the title text.
    ///
    /// #### Returns
    ///
    /// the title
    String getTitle();

    /// Sets the title text.
    ///
    /// #### Parameters
    ///
    /// - `title`: the title to display
    void setTitle(String title);

    // ---- toolbar and commands --------------------------------------------------

    /// Returns the toolbar installed in this top level.
    ///
    /// #### Returns
    ///
    /// the toolbar, or null when none is installed
    Toolbar getToolbar();

    /// Installs a toolbar in this top level.
    ///
    /// #### Parameters
    ///
    /// - `toolbar`: the toolbar to install
    void setToolbar(Toolbar toolbar);

    /// Adds a command to this top level.
    ///
    /// #### Parameters
    ///
    /// - `cmd`: the command to add
    void addCommand(Command cmd);

    /// Removes a command from this top level.
    ///
    /// #### Parameters
    ///
    /// - `cmd`: the command to remove
    void removeCommand(Command cmd);

    /// Removes every command from this top level.
    void removeAllCommands();

    /// Returns the number of commands.
    ///
    /// #### Returns
    ///
    /// the command count
    int getCommandCount();

    /// Returns the command at the given offset.
    ///
    /// #### Parameters
    ///
    /// - `index`: the offset of the command
    ///
    /// #### Returns
    ///
    /// the command at that offset
    Command getCommand(int index);

    /// Adds a listener notified when a command is activated.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener to add
    void addCommandListener(ActionListener l);

    /// Removes a previously added command listener.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener to remove
    void removeCommandListener(ActionListener l);

    // ---- animation -------------------------------------------------------------

    /// Returns the animation manager coordinating mutations of this top level.
    ///
    /// #### Returns
    ///
    /// the animation manager
    AnimationManager getAnimationManager();

    /// Registers an animation that is invoked on every frame of this top level.
    ///
    /// #### Parameters
    ///
    /// - `cmp`: the animation to register
    void registerAnimated(Animation cmp);

    /// Removes a previously registered animation.
    ///
    /// #### Parameters
    ///
    /// - `cmp`: the animation to remove
    void deregisterAnimated(Animation cmp);

    /// Takes the animation lock, blocking until no animation is in progress.
    ///
    /// #### Returns
    ///
    /// true if the lock was taken
    boolean grabAnimationLock();

    /// Releases a previously taken animation lock.
    void releaseAnimationLock();

    // ---- focus ------------------------------------------------------------------

    /// Returns the component currently owning focus.
    ///
    /// #### Returns
    ///
    /// the focus owner, or null when nothing is focused
    Component getFocused();

    /// Moves focus to the given component.
    ///
    /// #### Parameters
    ///
    /// - `focused`: the component that should take focus
    void setFocused(Component focused);

    /// Returns true when focus traversal wraps around at the edges.
    ///
    /// #### Returns
    ///
    /// true if focus is cyclic
    boolean isCyclicFocus();

    /// Sets whether focus traversal wraps around at the edges.
    ///
    /// #### Parameters
    ///
    /// - `cyclicFocus`: true to make focus cyclic
    void setCyclicFocus(boolean cyclicFocus);

    /// Returns true when only one component in this top level can take focus.
    ///
    /// #### Returns
    ///
    /// true if this is a single focus top level
    boolean isSingleFocusMode();

    /// Returns an iterator walking the components in traversal order.
    ///
    /// #### Parameters
    ///
    /// - `start`: the component to start from
    ///
    /// #### Returns
    ///
    /// the traversal iterator
    Form.TabIterator getTabIterator(Component start);

    /// Scrolls so that the given component becomes visible.
    ///
    /// #### Parameters
    ///
    /// - `c`: the component to reveal
    void scrollComponentToVisible(Component c);

    /// Adds a key binding scoped to this top level.
    ///
    /// #### Parameters
    ///
    /// - `keyCode`: the key code to bind
    ///
    /// - `listener`: the listener invoked for that key
    void addKeyListener(int keyCode, ActionListener listener);

    /// Removes a previously added key binding.
    ///
    /// #### Parameters
    ///
    /// - `keyCode`: the bound key code
    ///
    /// - `listener`: the listener to remove
    void removeKeyListener(int keyCode, ActionListener listener);

    // ---- editing -----------------------------------------------------------------

    /// Returns true when a component in this top level is being edited.
    ///
    /// #### Returns
    ///
    /// true if editing is in progress
    boolean isEditing();

    /// Stops the in-progress edit and invokes the callback once it has finished.
    ///
    /// #### Parameters
    ///
    /// - `onFinish`: invoked once editing has stopped
    void stopEditing(Runnable onFinish);

    /// Returns the component currently being edited.
    ///
    /// #### Returns
    ///
    /// the edited component, or null when nothing is being edited
    Component findCurrentlyEditingComponent();

    /// Returns the virtual input device currently open for this top level.
    ///
    /// #### Returns
    ///
    /// the open input device, or null when none is open
    VirtualInputDevice getCurrentInputDevice();

    /// Opens a virtual input device, closing whichever one was open before it.
    ///
    /// #### Parameters
    ///
    /// - `device`: the device to open, or null to close the current one
    ///
    /// #### Throws
    ///
    /// - `Exception`: if the previously open device failed to close
    void setCurrentInputDevice(VirtualInputDevice device) throws Exception;

    // ---- theme and metrics ----------------------------------------------------------

    /// Returns the theme manager used to style this top level.
    ///
    /// #### Returns
    ///
    /// the UI manager
    UIManager getUIManager();

    /// Sets the theme manager used to style this top level.
    ///
    /// #### Parameters
    ///
    /// - `uiManager`: the UI manager to use
    void setUIManager(UIManager uiManager);

    /// Returns the region of this top level that is guaranteed not to be obscured
    /// by system chrome such as a notch or a rounded corner.
    ///
    /// #### Returns
    ///
    /// the safe area rectangle
    Rectangle getSafeArea();

    /// Returns the height hidden behind the virtual keyboard, which is zero on a
    /// platform without one.
    ///
    /// #### Returns
    ///
    /// the obscured height in pixels
    int getInvisibleAreaUnderVKB();

    /// Indicates whether the given coordinate begins a drag of the whole top level
    /// rather than of a component inside it.
    ///
    /// #### Parameters
    ///
    /// - `x`: the x coordinate
    ///
    /// - `y`: the y coordinate
    ///
    /// #### Returns
    ///
    /// the drag region status for that coordinate
    int getDragRegionStatus(int x, int y);

    /// Returns true when components may change the mouse cursor.
    ///
    /// #### Returns
    ///
    /// true if cursors are enabled
    boolean isEnableCursors();

    /// Sets whether components may change the mouse cursor.
    ///
    /// #### Parameters
    ///
    /// - `e`: true to enable cursors
    void setEnableCursors(boolean e);

    /// Returns the text selection support for this top level.
    ///
    /// #### Returns
    ///
    /// the text selection
    TextSelection getTextSelection();

    // ---- lifecycle -------------------------------------------------------------------

    /// Makes this top level visible.
    void show();

    /// Adds a listener notified whenever this top level is shown.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener to add
    void addShowListener(ActionListener l);

    /// Removes a previously added show listener.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener to remove
    void removeShowListener(ActionListener l);

    /// Adds a listener notified whenever this top level changes size.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener to add
    void addSizeChangedListener(ActionListener l);

    /// Removes a previously added size changed listener.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener to remove
    void removeSizeChangedListener(ActionListener l);

    /// Dispatches a command to this top level's command handling, which is how a
    /// component that holds a `Command` triggers it without knowing whether it
    /// lives in a `Form` or a `Window`.
    ///
    /// #### Parameters
    ///
    /// - `cmd`: the command to dispatch
    ///
    /// - `ev`: the event to dispatch
    void dispatchCommand(Command cmd, ActionEvent ev);
}
