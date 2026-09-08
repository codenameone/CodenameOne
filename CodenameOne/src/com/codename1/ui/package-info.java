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

/// Main widget package containing the component/container "composite" similar
/// both in terminology and design to Swing/AWT.
///
/// Component/Container Relationship
///
/// Containers can be nested one within the other to form elaborate UI's. Containers use
/// `com.codename1.ui.layouts` to arrange the components within. This is important
/// as it allows a container can adapt to changing resolution, DPI, orientation, font size etc.
///
/// A container doesn't implicitly reflow its elements and in that regard follows the direction of AWT/Swing. As
/// a result the layout can be animated to create a flowing effect for UI changes. This also provides improved
/// performance as a bonus. See this sample of `Container` animation:
///
/// ```java
/// Form hi = new Form("Layout Animations", new BoxLayout(BoxLayout.Y_AXIS));
/// Button fall = new Button("Fall");
/// fall.addActionListener((e) -> {
///     for(int iter = 0 ; iter < 10 ; iter++) {
///         Label b = new Label ("Label " + iter);
///         b.setWidth(fall.getWidth());
///         b.setHeight(fall.getHeight());
///         b.setY(-fall.getHeight());
///         hi.add(b);
///     }
///     hi.getContentPane().animateLayout(20000);
/// });
/// hi.add(fall);
/// ```
///
/// You can learn more about layout managers `com.codename1.ui.layouts` and about
/// event handling `com.codename1.ui.events`.
///
/// Component Gallery
///
/// The component gallery below isn't complete or exhaustive but it should give you a sense of the
/// types of widgets available within Codename One in a glance.
///
/// ### AutoCompleteTextField
///
/// ![](https://www.codenameone.com/developer-guide/img/components-autocomplete.png)
/// `com.codename1.ui.AutoCompleteTextField` provides suggestions as you type into the text
/// field
///
///
/// ### BrowserComponent
///
/// ![](https://www.codenameone.com/developer-guide/img/components-browsercomponent.png)
/// `com.codename1.ui.BrowserComponent` allows us to embed an OS native browser into the app and
/// connect to its JavaScript runtime!
///
///
/// ### Button
///
/// ![](https://www.codenameone.com/developer-guide/img/generated/components-button-ios.png)
/// `com.codename1.ui.Button` allows us to bind events to a click
///
/// ### Link Button
///
/// ![](https://www.codenameone.com/developer-guide/img/generated/components-link-button.png)
/// `com.codename1.ui.Button` can also be used as a hyperlink
///
/// ### Calendar
///
/// ![](https://www.codenameone.com/developer-guide/img/components-calendar.png)
/// `com.codename1.ui.Calendar` presents a visual date picker. Notice that we recommend using
/// the
/// `com.codename1.ui.spinner.Picker` class which is superior when running on the device for most use cases.
///
///
/// ### CheckBox
///
/// ![](https://www.codenameone.com/developer-guide/img/generated/components-radiobutton-checkbox.png)
/// `com.codename1.ui.CheckBox` provides a check flag to tick on/off.
/// `com.codename1.ui.RadioButton` provides an exclusive check marking that only applies to one radio within
/// the group.
/// Both can also appear as toggle buttons
///
///
/// ### ComboBox
///
/// ![](https://www.codenameone.com/developer-guide/img/components-combobox.png)
/// `com.codename1.ui.ComboBox` is a list with a single visible entry that can popup the full
/// list. Notice that we recommend using the
/// `com.codename1.ui.spinner.Picker` class which is superior when running on the device for most use cases
///
///
/// ### Command
///
/// ![](https://www.codenameone.com/developer-guide/img/generated/components-toolbar.png)
/// `com.codename1.ui.Command` & `com.codename1.ui.Toolbar` provide deep customization
/// of the title area and allow us to place elements in the side menu (hamburger), overflow menu etc.
///
///
/// ### ComponentGroup
///
/// ![](https://www.codenameone.com/developer-guide/img/components-componentgroup.png)
/// `com.codename1.ui.ComponentGroup` allows us to group components together in a a group and
/// manipulate
/// their UIID's.
///
///
/// ### Dialog
///
/// ![](https://www.codenameone.com/developer-guide/img/components-dialog-modal-south.png)
/// `com.codename1.ui.Dialog` allows us to notify/ask the user in a modal/modless way.
///
/// ### InfiniteContainer
///
/// ![](https://www.codenameone.com/developer-guide/img/components-infinitescrolladapter.png)
/// `com.codename1.ui.InfiniteContainer` & `com.codename1.components.InfiniteScrollAdapter`
/// implement a `com.codename1.ui.Container` that can dynamically fetch more data
///
///
/// ### Label
///
/// ![](https://www.codenameone.com/developer-guide/img/generated/components-label-text-position.png)
/// `com.codename1.ui.Label` displays text and/or icons to the user
///
/// ### List
///
/// ![](https://www.codenameone.com/developer-guide/img/components-generic-list-cell-renderer.png)
/// `com.codename1.ui.List` a list of items, this is a rather elaborate component to work with!
/// We often
/// recommend just using `com.codename1.ui.Container`, `com.codename1.ui.InfiniteContainer` or
/// `com.codename1.components.InfiniteScrollAdapter`
///
///
/// ### MultiList
///
/// ![](https://www.codenameone.com/developer-guide/img/graphics-urlimage-multilist.png)
/// `com.codename1.ui.list.MultiList` a list that is a bit simpler to work with than List `com.codename1.ui.List` although
/// our recommendation to use something else still applies
///
///
/// ### Slider
///
/// ![](https://www.codenameone.com/developer-guide/img/components-slider.png)
/// `com.codename1.ui.Slider` allows us to indicate progress or allows the user to drag a bar to
/// indicate
/// volume (as in quantity)
///
///
/// ### SwipeableContainer
///
/// ![](https://www.codenameone.com/developer-guide/img/components-swipablecontainer.png)
/// `com.codename1.ui.SwipeableContainer` enables side swipe gesture to expose additional
/// functionality
///
///
/// ### Tabs
///
/// ![](https://www.codenameone.com/developer-guide/img/components-tabs.png)
/// `com.codename1.ui.Tabs` places components/containers into tabbable entries, allows swiping
/// between choices thru touch
///
///
/// ### Carousel
///
/// ![](https://www.codenameone.com/developer-guide/img/components-tabs-swipe1.png)
/// `com.codename1.ui.Tabs` can also be used as a swipe carousel
///
/// ### TextArea/Field
///
/// ![](https://www.codenameone.com/developer-guide/img/components-text-component.png)
/// `com.codename1.ui.TextArea` & `com.codename1.ui.TextField` allow for user input
/// via
/// the keyboard (virtual or otherwise)
///
///
/// ### TextComponent
///
/// ![](https://www.codenameone.com/img/blog/pixel-perfect-text-field-android-codenameone-font.png)
/// `com.codename1.ui.TextComponent` & `com.codename1.ui.PickerComponent` wrap the
/// text field and picker respectively and adapt them better to iOS/Android conventions
///
///
/// ### Table
///
/// ![](https://www.codenameone.com/developer-guide/img/generated/components-table-pinstripe.png)
/// `com.codename1.ui.table.Table` displays optionally editable tabular data to the user
///
/// ### Tree
///
/// ![](https://www.codenameone.com/developer-guide/img/components-tree-xml.png)
/// `com.codename1.ui.tree.Tree` displays data in a tree like hierarchy
///
/// ### ChartComponent
///
/// ![](https://www.codenameone.com/developer-guide/img/range_bar_chart.png)
/// `com.codename1.charts.ChartComponent` can embed a wide range of visualization aids and
/// animations into your app
///
///
/// ### ImageViewer
///
/// ![](https://www.codenameone.com/developer-guide/img/components-imageviewer-dynamic.png)
/// `com.codename1.components.ImageViewer` swipe, pinch to zoom and pan images
///
/// ### InfiniteProgress
///
/// ![](https://www.codenameone.com/developer-guide/img/infinite-progress.png)
/// `com.codename1.components.InfiniteProgress` provides a constantly spinning component
///
/// ### InteractionDialog
///
/// ![](https://www.codenameone.com/developer-guide/img/components-interaction-dialog.png)
/// `com.codename1.components.InteractionDialog` an "always on top" `com.codename1.ui.Dialog`
/// used for non-form modal UI patterns. You can make static `Dialog.show(...)` use
/// `InteractionDialog` by default with the theme constant `defaultInteractionDialogModeBool`
/// or via `com.codename1.ui.Dialog#setDefaultInteractionDialogMode(boolean)`.
///
///
/// ### MediaPlayer
///
/// ![](https://www.codenameone.com/developer-guide/img/components-mediaplayer.png)
/// `com.codename1.components.MediaPlayer` allows playing media including video coupled with the
/// `com.codename1.media.MediaManager`
///
///
/// ### MultiButton
///
/// ![](https://www.codenameone.com/developer-guide/img/generated/components-multibutton.png)
/// `com.codename1.components.MultiButton` is much more than a button
///
/// ### OnOffSwitch
///
/// ![](https://www.codenameone.com/developer-guide/img/components-onoffswitch.png)
/// `com.codename1.components.OnOffSwitch` allows us to toggle a state similar to the `com.codename1.ui.CheckBox`
/// but with a more modern look
///
///
/// ### ShareButton
///
/// ![](https://www.codenameone.com/developer-guide/img/components-sharebutton-android.png)
/// `com.codename1.components.ShareButton` provides native "social share" functionality
///
/// ### SpanLabel
///
/// ![](https://www.codenameone.com/developer-guide/img/generated/components-spanlabel.png)
/// `com.codename1.components.SpanLabel` a text label that "seamlessly" breaks lines
///
/// ### SpanButton
///
/// ![](https://www.codenameone.com/developer-guide/img/generated/components-spanbutton.png)
/// `com.codename1.components.SpanButton` a button that "seamlessly" breaks lines
///
/// ### Picker (Date)
///
/// ![](https://www.codenameone.com/developer-guide/img/components-picker-date-android.png)
/// `com.codename1.ui.spinner.Picker` allows us to show an OS native picker UI (Date Picker)
///
///
/// ### Picker (Time)
///
/// ![](https://www.codenameone.com/developer-guide/img/components-picker-time-android.png)
/// `com.codename1.ui.spinner.Picker` allows us to show an OS native picker UI (Time Picker)
///
///
/// ### ToastBar
///
/// ![](https://www.codenameone.com/developer-guide/img/components-statusbar.png)
/// `com.codename1.components.ToastBar` shows a non-obtrusive notice on the bottom of the `Form`
///
///
/// ### SignatureComponent
///
/// ![](https://www.codenameone.com/developer-guide/img/generated/components-signature2.png)
/// `com.codename1.components.SignatureComponent` shows a dialog that allows the user to "sign"
/// using the touch screen
///
///
/// ### Accordion
///
/// ![](https://www.codenameone.com/developer-guide/img/components-accordion.png)
/// `com.codename1.components.Accordion` displays collapsible content panels
///
/// ### FloatingHint
///
/// ![](https://www.codenameone.com/developer-guide/img/generated/components-floatinghint.png)
/// `com.codename1.components.FloatingHint` animates the text field hint into a label on top of
/// the text field and visa versa
///
///
/// ### FloatingActionButton
///
/// ![](https://www.codenameone.com/img/blog/floating-action.png)
/// `com.codename1.components.FloatingActionButton` hovers over the UI presenting a default
/// action
package com.codename1.ui;

// The image URLs above are /developer-guide/img/..., which is where these files
// are actually published: scripts/website/build.sh rsyncs docs/developer-guide/
// to the site's static/developer-guide/, preserving subdirectories, so a figure
// under img/generated/ serves from /developer-guide/img/generated/. They used to
// be written the other way round, /img/developer-guide/..., a path nothing in
// this repository produces. Renaming or moving a file under docs/developer-guide/img
// therefore does change these URLs, and they have to move with it.

