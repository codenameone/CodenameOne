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
package com.codename1.ui.layouts;

import com.codename1.io.Log;
import com.codename1.io.Util;
import com.codename1.l10n.L10NManager;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.layouts.LayeredLayout.LayeredLayoutConstraint.Inset;
import com.codename1.ui.plaf.Style;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

/// The `LayeredLayout` places the components in order one on top of the
/// other and sizes them all to the size of the largest component. This is useful
/// when trying to create an overlay on top of an existing component. E.g. an "x"
/// button to allow removing the component as shown here
///
/// The code to generate this UI is slightly complex and contains very little
/// relevant pieces. The only truly relevant piece the last line of code:
///
/// ```java
/// Form hi = new Form("Layered Layout");
/// int w = Math.min(Display.getInstance().getDisplayWidth(), Display.getInstance().getDisplayHeight());
/// Button settingsLabel = new Button("");
/// Style settingsStyle = settingsLabel.getAllStyles();
/// settingsStyle.setFgColor(0xff);
/// settingsStyle.setBorder(null);
/// settingsStyle.setBgColor(0xff00);
/// settingsStyle.setBgTransparency(255);
/// settingsStyle.setFont(settingsLabel.getUnselectedStyle().getFont().derive(w / 3, Font.STYLE_PLAIN));
/// FontImage.setMaterialIcon(settingsLabel, FontImage.MATERIAL_SETTINGS);
///
/// Button close = new Button("");
/// close.setUIID("Container");
/// close.getAllStyles().setFgColor(0xff0000);
/// FontImage.setMaterialIcon(close, FontImage.MATERIAL_CLOSE);
/// hi.add(LayeredLayout.encloseIn(settingsLabel,
///         FlowLayout.encloseRight(close)));
/// ```*
///
/// We are doing three distinct things here:
///
/// .
///
/// -  We are adding a layered layout to the form.
///
/// -  We are creating a layered layout and placing two components within. This
/// would be the equivalent of just creating a `LayeredLaout`
/// `com.codename1.ui.Container` and invoking `add` twice.
/// .*
/// -  We use
/// https://www.codenameone.com/javadoc/com/codename1/ui/layouts/FlowLayout.html[FlowLayout]
/// to position the `X` close button in the right position.
///
/// A common use case for `LayeredLayout` is the iOS carousel effect which
/// we can achieve by combing the `LayeredLayout` with
/// `com.codename1.ui.Tabs`.
///
/// ```java
/// Form hi = new Form("Swipe Tabs", new LayeredLayout());
/// Tabs t = new Tabs();
/// t.hideTabs();
///
/// Style s = UIManager.getInstance().getComponentStyle("Button");
/// FontImage radioEmptyImage = FontImage.createMaterial(FontImage.MATERIAL_RADIO_BUTTON_UNCHECKED, s);
/// FontImage radioFullImage = FontImage.createMaterial(FontImage.MATERIAL_RADIO_BUTTON_CHECKED, s);
/// ((DefaultLookAndFeel)UIManager.getInstance().getLookAndFeel()).setRadioButtonImages(radioFullImage, radioEmptyImage, radioFullImage, radioEmptyImage);
///
/// Container container1 = BoxLayout.encloseY(new Label("Swipe the tab to see more"),
///         new Label("You can put anything here"));
/// t.addTab("Tab1", container1);
/// t.addTab("Tab2", new SpanLabel("Some text directly in the tab"));
///
/// RadioButton firstTab = new RadioButton("");
/// RadioButton secondTab = new RadioButton("");
/// firstTab.setUIID("Container");
/// secondTab.setUIID("Container");
/// new ButtonGroup(firstTab, secondTab);
/// firstTab.setSelected(true);
/// Container tabsFlow = FlowLayout.encloseCenter(firstTab, secondTab);
///
/// hi.add(t);
/// hi.add(BorderLayout.south(tabsFlow));
///
/// t.addSelectionListener((i1, i2) -> {
///     switch(i2) {
///         case 0:
///             if(!firstTab.isSelected()) {
///                 firstTab.setSelected(true);
///             }
///             break;
///         case 1:
///             if(!secondTab.isSelected()) {
///                 secondTab.setSelected(true);
///             }
///             break;
///      }
/// });
/// ```
///
/// Notice that the layered layout sizes all components to the exact same size
/// one on top of the other. It usually requires that we use another container
/// within; in order to position the components correctly.
///
/// Forms have a built in layered layout that you can access via
/// `getLayeredPane()`, this allows you to overlay elements on top of the content
/// pane.
///
/// The layered pane is used internally by components such as `com.codename1.components.InteractionDialog`,
/// `com.codename1.u./AutoCompleteTextField` etc.
///
/// Warning: Placing native widgets within a layered layout is problematic due to
/// the behavior of peer components. Sample of peer components include the
/// `com.codename1.ui.BrowserComponent`, video playback etc.
///
/// Insets
///
/// This layout optionally supports insets for laying out its children. Use of insets can allow you to
/// achieve precise placement of components while adjusting properly to screen resizing.
///
/// Insets may be either fixed or flexible.  Fixed insets may be specified in pixels (`#UNIT_PIXELS`),
/// millimetres (`#UNIT_DIPS`), or percentage (`#UNIT_PERCENT`).  Insets may also be specified as just "auto" (`#UNIT_AUTO`),
/// in which case it is considered to be flexible (it will adapt to the component size and other insets).
///
/// Insets may also be anchored to a "reference component" so that it will always be measured from that reference component.
///
/// Insets Example
///
/// Adding a button to the top right of the parent:
///
/// ```java
/// `Container cnt = new Container(new LayeredLayout());
/// LayeredLayout ll = (LayeredLayout)cnt.getLayout();
/// Button btn = new Button("My Button");
/// cnt.add(btn);
/// ll.setInsets(btn, "0 0 auto auto");
///     // NOTE: Insets are expressed in same order as "margin" in CSS.  Clockwise starting on top.`
/// ```
///
/// Changing top inset to 2mm, and right inset to 1mm:
///
/// ```java
/// `ll.setInsets(btn, "2mm 1mm auto auto");`
/// ```
///
/// Using percentage insets:
///
/// ```java
/// `ll.setInsets(btn, "25% 25% auto auto");`
/// ```
///
/// **NOTE:** When using percent units, the percentage is always in terms of the "reference box" of the component.
/// The "reference box" is the bounding rectangle from which the insets are measured.  If none of the insets
/// is anchored to a reference component, then the bounding box will simply be the inner bounds of the parent container (i.e.
/// the bounds of the inside padding in the container.
///
/// **Using "auto" insets**
///
/// An "auto" inset is an inset that is flexible.  If all 4 insets are set to auto, then the component will tend to the
/// centre of the parent component, and its size will be the component's preferred size (though the size will be bounded by the size of the component's reference box).
/// If one inset is fixed, and the opposite inset is "auto", then the fixed inset and the component's preferred size will dictate the'
/// calculated size of the inset.
///
/// Reference Components
///
/// Insets may also have reference componnents.  E.g. If you want a button to be anchored to the right side of a search field, you could
/// make the button's left inset "reference" the text field.  This would be achieved as follows:
///
/// ```java
/// `Container cnt = new Container(new LayeredLayout());
/// LayeredLayout ll = (LayeredLayout)cnt.getLayout();
/// TextField searchField = new TextField();
/// Button btn = new Button("Search");
/// cnt.add(searchField).add(btn);
/// ll
///   .setInsets(searchField, "1mm auto auto auto")
///   .setInsets(btn, "0 auto auto 0")
///   .setReferenceComponentLeft(btn, searchField, 1f)
///   .setReferenceComponentTop(btn, searchField, 0);`
/// ```
///
/// In the above example we set the search field to be anchored to the top of its parent (1mm inset),
/// but for all other insets to be auto.  This will result it being centered horizontally in its parent.  We then
/// anchor the button to the left and top of the search field so that the top and left insets of button will always be
/// calculated relative to the position of searchField.  In particular since the button has top and left insets of 0,
/// the button will always be placed just to the right of the search field, with its top edge aligned with the top edge
/// of search field.
///
/// **Reference Positions**
///
/// The second parameter of `setReferenceComponentLeft(btn, searchField, 1f)` is the reference position and it dictates
/// which edge of the reference component (searchField) the inset should be anchored to.  A value of 1 indicates that
/// it should anchor to the opposite side of the inset  (e.g. in this case it is the "left" inset we are setting, so the 1
/// value dictates that it is anchored to the "right" side of the text field.  A value of 0 indicates that it should anchor
/// to the same side as the inset.  This is why we used 0 in the subsequent call to `.setReferenceComponentTop(btn, searchField, 0);`,
/// because we want to anchor the "top" inset of button to the "top" edge of searchField.
///
/// @author Shai Almog
///
/// #### See also
///
/// - com.codename1.ui.Form#getLayeredPane()
///
/// - com.codename1.ui.Form#getLayeredPane(java.lang.Class, boolean)
///
/// - com.codename1.ui.Form#setGlassPane(com.codename1.ui.Painter)
public class LayeredLayout extends Layout {

    /// Unit used for insets.  Millimetres.
    ///
    /// #### See also
    ///
    /// - Inset#unit(byte)
    ///
    /// - Inset#changeUnits(byte)
    public static final byte UNIT_DIPS = Style.UNIT_TYPE_DIPS;

    /// Unit used for insets.  Pixels.
    ///
    /// #### See also
    ///
    /// - Inset#unit(byte)
    ///
    /// - Inset#changeUnits(byte)
    public static final byte UNIT_PIXELS = Style.UNIT_TYPE_PIXELS;

    /// Unit used for insets.  Percent.
    ///
    /// #### See also
    ///
    /// - Inset#unit(byte)
    ///
    /// - Inset#changeUnits(byte)
    public static final byte UNIT_PERCENT = Style.UNIT_TYPE_SCREEN_PERCENTAGE;

    /// Unit used for insets.  Auto.  Auto unit type for an inset indicates the
    /// the inset will be automatically determined at layout time.
    ///
    /// #### See also
    ///
    /// - Inset#unit(byte)
    ///
    /// - Inset#changeUnits(byte)
    public static final byte UNIT_AUTO = 100;

    /// Unit used for insets.  Baseline.  Baseline unit type for an inset indicates
    /// the inset will be aligned with the baseline of the reference component.  This only
    /// makes sense for the top inset.  The height will automatically become the preferred
    /// height and the bottom inset will become "auto" if the top inset uses the baseline unit.
    public static final byte UNIT_BASELINE = 101;

    /// Temp collection to keep track of which components in the container
    /// have been laid out.
    private final HashSet<Component> tmpLaidOut = new HashSet<Component>();

    /// The preferred height in MM of this layout which serves as a sort of minimum
    /// height even when the components in the layout don't demand space.
    ///
    /// The actual preferred height will be the max of this value and the
    /// calculated preferred height based on the container's children.
    private float preferredHeightMM;

    /// The preferred width (in MM) of this layout which serves as a sort of minimum
    /// width even when the components in the layout don't demand space.
    ///
    /// The actual preferred width will be the max of this value and the
    /// calculated preferred width based on the container's children.
    private float preferredWidthMM;

    /// Shorthand for Container.encloseIn(new LayeredLayout(), cmps);
    ///
    /// #### Parameters
    ///
    /// - `cmps`: the components to add to a new layered layout container
    ///
    /// #### Returns
    ///
    /// a newly created layered layout
    public static Container encloseIn(Component... cmps) {
        return Container.encloseIn(new LayeredLayout(), cmps);
    }

    private static int getOuterHeight(Component cmp) {
        Style s = cmp.getStyle();
        return cmp.getHeight() + s.getVerticalMargins();
    }

    private static int getOuterPreferredH(Component cmp) {
        Style s = cmp.getStyle();
        return cmp.getPreferredH() + s.getVerticalMargins();
    }

    private static int getOuterWidth(Component cmp) {
        Style s = cmp.getStyle();
        return cmp.getWidth() + s.getHorizontalMargins();
    }

    private static int getOuterPreferredW(Component cmp) {
        Style s = cmp.getStyle();
        return cmp.getPreferredW() + s.getHorizontalMargins();
    }

    private static int getOuterX(Component cmp) {
        return cmp.getX() - cmp.getStyle().getMarginLeftNoRTL();
    }

    private static int getOuterY(Component cmp) {
        return cmp.getY() - cmp.getStyle().getMarginTop();
    }

    
   
    /*
    // We don't see to use this right now so commenting out.  However
    // it is conceivable that we might want to reintroduce this ability later, so
    // I'm leaving the code here.
    private void uninstallConstraint(Component cmp) {
        LayeredLayoutConstraint constraint = (LayeredLayoutConstraint)getComponentConstraint(cmp);
        if (constraint != null) {
            constraint.cmp = null;
        }
        cmp.putClientProperty("$$LayeredLayoutConstraint", null);
    }
    */

    /// Sets the preferred size of this layout in MM.  This serves as a minimum
    /// size that will be returned by calcPreferredSize().
    ///
    /// #### Parameters
    ///
    /// - `width`: The preferred width in MM.
    ///
    /// - `height`: The preferred height in MM.
    public void setPreferredSizeMM(float width, float height) {
        this.preferredHeightMM = height;
        this.preferredWidthMM = width;
    }

    /// The preferred height in MM of this layout which serves as a sort of minimum
    /// height even when the components in the layout don't demand space.
    ///
    /// The actual preferred height will be the max of this value and the
    /// calculated preferred height based on the container's children.
    public float getPreferredHeightMM() {
        return preferredHeightMM;
    }

    /// Sets the preferred height of this layout in MM.
    ///
    /// #### Parameters
    ///
    /// - `mm`
    public void setPreferredHeightMM(float mm) {
        preferredHeightMM = mm;
    }

    /// The preferred width (in MM) of this layout which serves as a sort of minimum
    /// width even when the components in the layout don't demand space.
    ///
    /// The actual preferred width will be the max of this value and the
    /// calculated preferred width based on the container's children.
    public float getPreferredWidthMM() {
        return preferredWidthMM;
    }

    /// Sets the preferred width of this layout in MM.
    ///
    /// #### Parameters
    ///
    /// - `mm`
    public void setPreferredWidthMM(float mm) {
        preferredWidthMM = mm;
    }

    @Override
    public void addLayoutComponent(Object value, Component comp, Container c) {
        if (value instanceof LayeredLayoutConstraint.Inset) {
            value = ((LayeredLayoutConstraint.Inset) value).constraint();
        }
        if (value instanceof LayeredLayoutConstraint) {

            installConstraint((LayeredLayoutConstraint) value, comp);
        }
    }

    /// Wraps `#getComponentConstraint(com.codename1.ui.Component)` and casts it
    /// directly to `LayeredLayoutConstraint`.
    ///
    /// #### Parameters
    ///
    /// - `cmp`: The component whose constraint we want to retrieve.
    ///
    /// #### Returns
    ///
    /// The layered layout constraint for this component.
    public LayeredLayoutConstraint getLayeredLayoutConstraint(Component cmp) {
        return (LayeredLayoutConstraint) getComponentConstraint(cmp);
    }

    /// Installs the given constraint in the provided component.
    ///
    /// #### Parameters
    ///
    /// - `constraint`
    ///
    /// - `cmp`
    private LayeredLayoutConstraint installConstraint(LayeredLayoutConstraint constraint, Component cmp) {

        if (constraint.outer() != this || (constraint.cmp != null && constraint.cmp != cmp)) { //NOPMD CompareObjectsWithEquals
            LayeredLayoutConstraint tmp = createConstraint();
            constraint.copyTo(tmp);
            constraint = tmp;
        }
        constraint.cmp = cmp;
        cmp.putClientProperty("$$LayeredLayoutConstraint", constraint);
        return constraint;
    }

    /// Makes a copy of the given constraint.
    ///
    /// #### Parameters
    ///
    /// - `constraint`: The constraint to copy.
    ///
    /// #### Returns
    ///
    /// The copied constraint.
    @Override
    public Object cloneConstraint(Object constraint) {
        if (constraint instanceof LayeredLayoutConstraint) {
            return ((LayeredLayoutConstraint) constraint).copy();
        }
        return super.cloneConstraint(constraint);
    }

    /// Gets the LayeredLayoutConstraint associated with the given component.
    ///
    /// May return null if there is no constraint.
    ///
    /// #### Parameters
    ///
    /// - `comp`
    @Override
    public Object getComponentConstraint(Component comp) {
        return comp.getClientProperty("$$LayeredLayoutConstraint");
    }

    /// Creates a default layered layout constraint.  Default constraint
    /// has zero insets on all four sides.
    ///
    /// #### Parameters
    ///
    /// - `constraint`
    public LayeredLayoutConstraint createConstraint(String constraint) {
        return new LayeredLayoutConstraint().setInsets(constraint);
    }

    /// If the given component already has a LayeredLayoutConstraint, then this
    /// will return it. Otherwise it will create a constraint, install it in cmp
    /// and return the constraint for inspection or manipulation.
    ///
    /// #### Parameters
    ///
    /// - `cmp`: The component whose constraint we wish to retrieve.
    ///
    /// #### Returns
    ///
    /// The constraint for a given component.
    public LayeredLayoutConstraint getOrCreateConstraint(Component cmp) {
        LayeredLayoutConstraint constraint = (LayeredLayoutConstraint) getComponentConstraint(cmp);
        if (constraint == null) {
            //System.out.println("Constraint is null... creating a new one");
            constraint = createConstraint();
            constraint = installConstraint(constraint, cmp);
        }
        return constraint;
    }

    /// Gets an `Inset` associated with the provided component
    ///
    /// #### Parameters
    ///
    /// - `cmp`: The component whose inset we wish to retrieve.
    ///
    /// - `side`: @param side The side of the inset.  One of `Component#TOP`, `Component#LEFT`, `Component#BOTTOM`
    ///             or `Component#RIGHT`.
    ///
    /// #### Returns
    ///
    /// The `Inset` for the given side of the component.
    public Inset getInset(Component cmp, int side) {
        return getOrCreateConstraint(cmp).insets[side];
    }

    /// Returns the insets for the given component as a string.  This can return the
    /// insets in one of two formats depending on the value of the withLabels
    /// parameter.
    ///
    /// #### Parameters
    ///
    /// - `cmp`: The component whose insets we wish to retrieve.
    ///
    /// - `withLabels`: @param withLabels If false, then this returns a string of the format `"top right bottom left"`
    ///                   e.g `"2mm 2mm 2mm 2mm"`.  If true, then it will be formatted like CSS properties: `"top:2mm; right:2mm; bottom:2mm; left:2mm"`.
    ///
    /// #### Returns
    ///
    /// @return The insets associated with cmp as a string. Each inset will include the unit.   E.g.:
    ///
    /// - 2mm = 2 millimetres/dips
    /// - 2px = 2 pixels
    /// - 25% = 25%
    /// - auto = Flexible inset
    public String getInsetsAsString(Component cmp, boolean withLabels) {
        return getOrCreateConstraint(cmp).getInsetsAsString(withLabels);
    }

    /// Gets the top inset as a string. Return value will include the unit, so the following
    /// are possible values:
    ///
    /// - 2mm = 2 millimetres
    ///
    /// - 2px = 2 pixels
    ///
    /// - 25% = 25%
    ///
    /// - auto = Flexible Inset
    ///
    /// #### Parameters
    ///
    /// - `cmp`: The component whose inset we wish to retrieve.
    ///
    /// #### Returns
    ///
    /// The inset formatted as a string with the unit abbreviation ("mm", "px", or "%") suffixed.
    public String getTopInsetAsString(Component cmp) {
        return getOrCreateConstraint(cmp).top().getValueAsString();
    }

    /// Gets the bottom inset as a string. Return value will include the unit, so the following
    /// are possible values:
    ///
    /// - 2mm = 2 millimetres
    ///
    /// - 2px = 2 pixels
    ///
    /// - 25% = 25%
    ///
    /// - auto = Flexible Inset
    ///
    /// #### Parameters
    ///
    /// - `cmp`: The component whose inset we wish to retrieve.
    ///
    /// #### Returns
    ///
    /// The inset formatted as a string with the unit abbreviation ("mm", "px", or "%") suffixed.
    public String getBottomInsetAsString(Component cmp) {
        return getOrCreateConstraint(cmp).bottom().getValueAsString();
    }

    /// Gets the left inset as a string. Return value will include the unit, so the following
    /// are possible values:
    ///
    /// - 2mm = 2 millimetres
    ///
    /// - 2px = 2 pixels
    ///
    /// - 25% = 25%
    ///
    /// - auto = Flexible Inset
    ///
    /// #### Parameters
    ///
    /// - `cmp`: The component whose inset we wish to retrieve.
    ///
    /// #### Returns
    ///
    /// The inset formatted as a string with the unit abbreviation ("mm", "px", or "%") suffixed.
    public String getLeftInsetAsString(Component cmp) {
        return getOrCreateConstraint(cmp).left().getValueAsString();
    }

    /// Gets the right inset as a string. Return value will include the unit, so the following
    /// are possible values:
    ///
    /// - 2mm = 2 millimetres
    ///
    /// - 2px = 2 pixels
    ///
    /// - 25% = 25%
    ///
    /// - auto = Flexible Inset
    ///
    /// #### Parameters
    ///
    /// - `cmp`: The component whose inset we wish to retrieve.
    ///
    /// #### Returns
    ///
    /// The inset formatted as a string with the unit abbreviation ("mm", "px", or "%") suffixed.
    public String getRightInsetAsString(Component cmp) {
        return getOrCreateConstraint(cmp).right().getValueAsString();
    }

    /// Sets the insets for the component cmp to the values specified in insets.
    ///
    /// #### Parameters
    ///
    /// - `cmp`: The component whose insets we wish to set.
    ///
    /// - `insets`: @param insets The insets expressed as a string.  See `LayeredLayoutConstraint#setInsets(java.lang.String)` for
    ///               details on the format of this parameter.
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    ///
    /// #### See also
    ///
    /// - @see LayeredLayoutConstraint#setInsets(java.lang.String) For details on the insets parameter
    /// format.
    public LayeredLayout setInsets(Component cmp, String insets) {
        getOrCreateConstraint(cmp).setInsets(insets);
        return this;
    }

    /// Sets the top inset for this component to the prescribed value.
    ///
    /// #### Parameters
    ///
    /// - `cmp`: The component whose inset we wish to set.
    ///
    /// - `inset`: @param inset The inset value, including unit.  Units are Percent (%), Millimetres (mm), Pixels (px), and "auto".  E.g. the
    ///              following insets values would all be acceptable:
    ///
    ///
    ///
    ///
    ///
    /// - `"2mm"` = 2 millimetres
    ///
    /// - `"2px"` = 2 pixels
    ///
    /// - `"25%"` = 25 percent.
    ///
    /// - `"auto"` = Flexible inset
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public LayeredLayout setInsetTop(Component cmp, String inset) {
        getOrCreateConstraint(cmp).top().setValue(inset);
        return this;
    }

    /// Sets the top inset for this component to the prescribed value.
    ///
    /// #### Parameters
    ///
    /// - `cmp`: The component whose inset we wish to set.
    ///
    /// - `inset`: @param inset The inset value, including unit.  Units are Percent (%), Millimetres (mm), Pixels (px), and "auto".  E.g. the
    ///              following insets values would all be acceptable:
    ///
    ///
    ///
    ///
    ///
    /// - `"2mm"` = 2 millimetres
    ///
    /// - `"2px"` = 2 pixels
    ///
    /// - `"25%"` = 25 percent.
    ///
    /// - `"auto"` = Flexible inset
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public LayeredLayout setInsetBottom(Component cmp, String inset) {
        getOrCreateConstraint(cmp).bottom().setValue(inset);
        return this;
    }

    /// Sets the left inset for this component to the prescribed value.
    ///
    /// #### Parameters
    ///
    /// - `cmp`: The component whose inset we wish to set.
    ///
    /// - `inset`: @param inset The inset value, including unit.  Units are Percent (%), Millimetres (mm), Pixels (px), and "auto".  E.g. the
    ///              following insets values would all be acceptable:
    ///
    ///
    ///
    ///
    ///
    /// - `"2mm"` = 2 millimetres
    ///
    /// - `"2px"` = 2 pixels
    ///
    /// - `"25%"` = 25 percent.
    ///
    /// - `"auto"` = Flexible inset
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public LayeredLayout setInsetLeft(Component cmp, String inset) {
        getOrCreateConstraint(cmp).left().setValue(inset);
        return this;
    }

    /// Sets the right inset for this component to the prescribed value.
    ///
    /// #### Parameters
    ///
    /// - `cmp`: The component whose inset we wish to set.
    ///
    /// - `inset`: @param inset The inset value, including unit.  Units are Percent (%), Millimetres (mm), Pixels (px), and "auto".  E.g. the
    ///              following insets values would all be acceptable:
    ///
    ///
    ///
    ///
    ///
    /// - `"2mm"` = 2 millimetres
    ///
    /// - `"2px"` = 2 pixels
    ///
    /// - `"25%"` = 25 percent.
    ///
    /// - `"auto"` = Flexible inset
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public LayeredLayout setInsetRight(Component cmp, String inset) {
        getOrCreateConstraint(cmp).right().setValue(inset);
        return this;
    }

    /// Sets the reference components for the insets of cmp. See `LayeredLayoutConstraint#setReferenceComponents(com.codename1.ui.Component...)`
    /// for a full description of the parameters.
    ///
    /// #### Parameters
    ///
    /// - `cmp`: The component whose reference components we wish to check.
    ///
    /// - `referenceComponents`: @param referenceComponents The reference components.  This var arg may contain 1 to 4 values.  See `LayeredLayoutConstraint#setReferenceComponents(com.codename1.ui.Component...)`
    ///                            for a full description.
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public LayeredLayout setReferenceComponents(Component cmp, Component... referenceComponents) {
        getOrCreateConstraint(cmp).setReferenceComponents(referenceComponents);
        return this;
    }

    /// Sets the reference components for this component as a string of 1 to 4 component indices separated by spaces. An
    /// index of -1 indicates no reference for the corresponding inset.  See `java.lang.String)`
    /// for a description of the refs parameter.
    ///
    /// #### Parameters
    ///
    /// - `cmp`: The component whose references we're setting.
    ///
    /// - `refs`: Reference components as a string of component indices in the parent.
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public LayeredLayout setReferenceComponents(Component cmp, String refs) {
        getOrCreateConstraint(cmp).setReferenceComponentIndices(cmp.getParent(), refs);
        return this;
    }

    /// Sets the reference component for the top inset of the given component.
    ///
    /// #### Parameters
    ///
    /// - `cmp`: The component whose insets we are manipulating.
    ///
    /// - `referenceComponent`: The component to anchor the inset to.
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public LayeredLayout setReferenceComponentTop(Component cmp, Component referenceComponent) {
        getOrCreateConstraint(cmp).top().referenceComponent(referenceComponent);
        return this;
    }

    /// Sets the reference component for the bottom inset of the given component.
    ///
    /// #### Parameters
    ///
    /// - `cmp`: The component whose insets we are manipulating.
    ///
    /// - `referenceComponent`: The component to anchor the inset to.
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public LayeredLayout setReferenceComponentBottom(Component cmp, Component referenceComponent) {
        getOrCreateConstraint(cmp).bottom().referenceComponent(referenceComponent);
        return this;
    }

    /// Sets the reference component for the left inset of the given component.
    ///
    /// #### Parameters
    ///
    /// - `cmp`: The component whose insets we are manipulating.
    ///
    /// - `referenceComponent`: The component to anchor the inset to.
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public LayeredLayout setReferenceComponentLeft(Component cmp, Component referenceComponent) {
        getOrCreateConstraint(cmp).left().referenceComponent(referenceComponent);
        return this;
    }

    /// Sets the reference component for the right inset of the given component.
    ///
    /// #### Parameters
    ///
    /// - `cmp`: The component whose insets we are manipulating.
    ///
    /// - `referenceComponent`: The component to anchor the inset to.
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public LayeredLayout setReferenceComponentRight(Component cmp, Component referenceComponent) {
        getOrCreateConstraint(cmp).top().referenceComponent(referenceComponent);
        return this;
    }

    /// Sets the reference positions for reference components.  See `LayeredLayoutConstraint#setReferencePositions(float...)`
    /// for a description of the parameters.
    ///
    /// #### Parameters
    ///
    /// - `cmp`: The component whose insets we are manipulating.
    ///
    /// - `referencePositions`: @param referencePositions The reference positions for the reference components. See `LayeredLayoutConstraint#setReferencePositions(float...)`
    ///                           for a full description of this parameter.
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public LayeredLayout setReferencePositions(Component cmp, float... referencePositions) {
        getOrCreateConstraint(cmp).setReferencePositions(referencePositions);
        return this;
    }

    /// Sets the reference positions for reference components.  See `LayeredLayoutConstraint#setReferencePositions(float...)`
    /// for a description of the parameters.
    ///
    /// #### Parameters
    ///
    /// - `cmp`: The component whose insets we are manipulating.
    ///
    /// - `positions`: @param positions The reference positions for the reference components. See `LayeredLayoutConstraint#setReferencePositions(float...)`
    ///                  for a full description of this parameter.
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public LayeredLayout setReferencePositions(Component cmp, String positions) {
        getOrCreateConstraint(cmp).setReferencePositions(positions);
        return this;
    }

    /// Sets the top inset reference position.  Only applicable if the top inset has a reference
    /// component specified.
    ///
    /// #### Parameters
    ///
    /// - `cmp`: The component whose insets were are manipulating.
    ///
    /// - `position`: @param position The position.  See `LayeredLayoutConstraint#setReferencePositions(float...)` for a full
    ///                 description of the possible values here.
    public LayeredLayout setReferencePositionTop(Component cmp, float position) {
        getOrCreateConstraint(cmp).top().referencePosition(position);
        return this;
    }

    /// Sets the reference component for the top inset of the given component.
    ///
    /// #### Parameters
    ///
    /// - `cmp`: The component whose insets we are manipulating.
    ///
    /// - `referenceComponent`: The component to which the inset should be anchored.
    ///
    /// - `position`: @param position           The position of the reference anchor.  See `LayeredLayoutConstraint#setReferencePositions(float...)`
    ///                           for a full description of reference positions.
    public LayeredLayout setReferenceComponentTop(Component cmp, Component referenceComponent, float position) {
        getOrCreateConstraint(cmp).top().referenceComponent(referenceComponent).referencePosition(position);
        return this;
    }

    /// Sets the bottom inset reference position.  Only applicable if the top inset has a reference
    /// component specified.
    ///
    /// #### Parameters
    ///
    /// - `cmp`: The component whose insets were are manipulating.
    ///
    /// - `position`: @param position The position.  See `LayeredLayoutConstraint#setReferencePositions(float...)` for a full
    ///                 description of the possible values here.
    public LayeredLayout setReferencePositionBottom(Component cmp, float position) {
        getOrCreateConstraint(cmp).bottom().referencePosition(position);
        return this;
    }

    /// Sets the reference component for the bottom inset of the given component.
    ///
    /// #### Parameters
    ///
    /// - `cmp`: The component whose insets we are manipulating.
    ///
    /// - `referenceComponent`: The component to which the inset should be anchored.
    ///
    /// - `position`: @param position           The position of the reference anchor.  See `LayeredLayoutConstraint#setReferencePositions(float...)`
    ///                           for a full description of reference positions.
    public LayeredLayout setReferenceComponentBottom(Component cmp, Component referenceComponent, float position) {
        getOrCreateConstraint(cmp).bottom().referenceComponent(referenceComponent).referencePosition(position);
        return this;
    }

    /// Sets the left inset reference position.  Only applicable if the top inset has a reference
    /// component specified.
    ///
    /// #### Parameters
    ///
    /// - `cmp`: The component whose insets were are manipulating.
    ///
    /// - `position`: @param position The position.  See `LayeredLayoutConstraint#setReferencePositions(float...)` for a full
    ///                 description of the possible values here.
    public LayeredLayout setReferencePositionLeft(Component cmp, float position) {
        getOrCreateConstraint(cmp).left().referencePosition(position);
        return this;
    }

    /// Sets the reference component for the left inset of the given component.
    ///
    /// #### Parameters
    ///
    /// - `cmp`: The component whose insets we are manipulating.
    ///
    /// - `referenceComponent`: The component to which the inset should be anchored.
    ///
    /// - `position`: @param position           The position of the reference anchor.  See `LayeredLayoutConstraint#setReferencePositions(float...)`
    ///                           for a full description of reference positions.
    public LayeredLayout setReferenceComponentLeft(Component cmp, Component referenceComponent, float position) {
        getOrCreateConstraint(cmp).left().referenceComponent(referenceComponent).referencePosition(position);
        return this;
    }

    /// Sets the right inset reference position.  Only applicable if the top inset has a reference
    /// component specified.
    ///
    /// #### Parameters
    ///
    /// - `cmp`: The component whose insets were are manipulating.
    ///
    /// - `position`: @param position The position.  See `LayeredLayoutConstraint#setReferencePositions(float...)` for a full
    ///                 description of the possible values here.
    public LayeredLayout setReferencePositionRight(Component cmp, float position) {
        getOrCreateConstraint(cmp).right().referencePosition(position);
        return this;
    }

    /// Sets the reference component for the right inset of the given component.
    ///
    /// #### Parameters
    ///
    /// - `cmp`: The component whose insets we are manipulating.
    ///
    /// - `referenceComponent`: The component to which the inset should be anchored.
    ///
    /// - `position`: @param position           The position of the reference anchor.  See `LayeredLayoutConstraint#setReferencePositions(float...)`
    ///                           for a full description of reference positions.
    public LayeredLayout setReferenceComponentRight(Component cmp, Component referenceComponent, float position) {
        getOrCreateConstraint(cmp).right().referenceComponent(referenceComponent).referencePosition(position);
        return this;
    }

    /// See `LayeredLayoutConstraint#setPercentInsetAnchorHorizontal(float)`
    ///
    /// #### Parameters
    ///
    /// - `cmp`
    ///
    /// - `anchor`
    ///
    /// #### Returns
    ///
    /// Self for chaining
    public LayeredLayout setPercentInsetAnchorHorizontal(Component cmp, float anchor) {
        getOrCreateConstraint(cmp).setPercentInsetAnchorHorizontal(anchor);
        return this;
    }

    /// See `LayeredLayoutConstraint#setPercentInsetAnchorVertical(float)`
    ///
    /// #### Parameters
    ///
    /// - `cmp`
    ///
    /// - `anchor`
    ///
    /// #### Returns
    ///
    /// Self for chaining
    public LayeredLayout setPercentInsetAnchorVertical(Component cmp, float anchor) {
        getOrCreateConstraint(cmp).setPercentInsetAnchorVertical(anchor);
        return this;
    }

    /// See `LayeredLayoutConstraint#getPercentInsetAnchorHorizontal()`
    ///
    /// #### Parameters
    ///
    /// - `cmp`
    public float getPercentInsetAnchorHorizontal(Component cmp) {
        return getOrCreateConstraint(cmp).getPercentInsetAnchorHorizontal();
    }

    /// See `LayeredLayoutConstraint#getPercentInsetAnchorVertical()`
    ///
    /// #### Parameters
    ///
    /// - `cmp`
    public float getPercentInsetAnchorVertical(Component cmp) {
        return getOrCreateConstraint(cmp).getPercentInsetAnchorVertical();
    }

    /// {@inheritDoc}
    @Override
    public void layoutContainer(Container parent) {
        Style s = parent.getStyle();
        int top = s.getPaddingTop();
        int bottom = parent.getLayoutHeight() - parent.getBottomGap() - s.getPaddingBottom();
        int left = s.getPaddingLeft(parent.isRTL());
        int right = parent.getLayoutWidth() - parent.getSideGap() - s.getPaddingRight(parent.isRTL());

        int numOfcomponents = parent.getComponentCount();
        tmpLaidOut.clear();
        for (int i = 0; i < numOfcomponents; i++) {
            Component cmp = parent.getComponentAt(i);
            layoutComponent(parent, cmp, top, left, bottom, right);
        }

    }

    /// Lays out the specific component within the container.  This will first lay out any components that it depends on.
    ///
    /// #### Parameters
    ///
    /// - `parent`: The parent container being laid out.
    ///
    /// - `cmp`: The component being laid out.
    ///
    /// - `top`
    ///
    /// - `left`
    ///
    /// - `bottom`
    ///
    /// - `right`
    private void layoutComponent(Container parent, Component cmp, int top, int left, int bottom, int right) {
        if (tmpLaidOut.contains(cmp)) {
            return;
        }
        tmpLaidOut.add(cmp);
        LayeredLayoutConstraint constraint = (LayeredLayoutConstraint) getComponentConstraint(cmp);
        if (constraint != null) {
            constraint.fixDependencies(parent);
            for (LayeredLayoutConstraint.Inset inset : constraint.insets) {
                if (inset.referenceComponent != null && inset.referenceComponent.getParent() == parent) {
                    layoutComponent(parent, inset.referenceComponent, top, left, bottom, right);
                }
            }
        }

        Style s = cmp.getStyle();
        if (constraint != null) {
            //int innerTop = top;
            //int innerBottom = bottom;
            //left = 0;
            //right = parent.getLayoutWidth();
            int leftInset = constraint.insets[Component.LEFT].calculate(cmp, top, left, bottom, right);
            int rightInset = constraint.insets[Component.RIGHT].calculate(cmp, top, left, bottom, right);
            int topInset = constraint.insets[Component.TOP].calculate(cmp, top, left, bottom, right);
            int bottomInset = constraint.insets[Component.BOTTOM].calculate(cmp, top, left, bottom, right);
            cmp.setX(left + leftInset + s.getMarginLeft(parent.isRTL()));
            cmp.setY(top + topInset + s.getMarginTop());
            cmp.setWidth(Math.max(0, right - cmp.getX() - s.getMarginRight(parent.isRTL()) - rightInset));
            //cmp.setWidth(Math.max(0, right - left - s.getHorizontalMargins() - rightInset - leftInset));
            //cmp.setHeight(Math.max(0, bottom - top - s.getVerticalMargins() - bottomInset - topInset));
            cmp.setHeight(Math.max(0, bottom - cmp.getY() - s.getMarginBottom() - bottomInset));

        } else {

            int x = left + s.getMarginLeft(parent.isRTL());
            int y = top + s.getMarginTop();
            int w = right - left - s.getHorizontalMargins();
            int h = bottom - top - s.getVerticalMargins();

            cmp.setX(x);
            cmp.setY(y);
            cmp.setWidth(Math.max(0, w));
            cmp.setHeight(Math.max(0, h));
            //System.out.println("Component laid out "+cmp);
        }
    }

    private void calcPreferredValues(Component cmp) {
        if (tmpLaidOut.contains(cmp)) {
            return;
        }
        tmpLaidOut.add(cmp);
        LayeredLayoutConstraint constraint = (LayeredLayoutConstraint) getComponentConstraint(cmp);
        if (constraint != null) {
            constraint.fixDependencies(cmp.getParent());
            for (LayeredLayoutConstraint.Inset inset : constraint.insets) {
                if (inset.referenceComponent != null && inset.referenceComponent.getParent() == cmp.getParent()) {
                    calcPreferredValues(inset.referenceComponent);
                }
                inset.calcPreferredValue(cmp.getParent(), cmp);
            }
        }
    }

    /// {@inheritDoc}
    @Override
    public Dimension getPreferredSize(Container parent) {
        int maxWidth = 0;
        int maxHeight = 0;
        int numOfcomponents = parent.getComponentCount();
        tmpLaidOut.clear();
        boolean requiresSecondPassToCalculatePercentInsets = false;
        for (int i = 0; i < numOfcomponents; i++) {
            Component cmp = parent.getComponentAt(i);
            calcPreferredValues(cmp);
            LayeredLayoutConstraint constraint = (LayeredLayoutConstraint) getComponentConstraint(cmp);
            int vInsets = 0;
            int hInsets = 0;
            if (constraint != null) {
                if (!requiresSecondPassToCalculatePercentInsets) {
                    for (Inset ins : constraint.insets) {
                        if (ins.unit == UNIT_PERCENT && ins.referenceComponent == null) {
                            requiresSecondPassToCalculatePercentInsets = true;
                            break;
                        }
                    }
                }
                vInsets += constraint.insets[Component.TOP].preferredValue
                        + constraint.insets[Component.BOTTOM].preferredValue;
                hInsets += constraint.insets[Component.LEFT].preferredValue
                        + constraint.insets[Component.RIGHT].preferredValue;
                /*
                // Commenting all this stuff out because the calcPreferredValues() call should
                // take all of this into account already.
                Component topRef = constraint.top().getReferenceComponent();
                LayeredLayoutConstraint currConstraint = constraint;
                int maxIterations = numOfcomponents;
                int iter = 0;
                while (topRef != null) {
                    if (iter++ >= maxIterations) break;
                    vInsets += Math.max(0, topRef.getOuterPreferredH() * currConstraint.top().getReferencePosition());
                    currConstraint = getOrCreateConstraint(topRef);
                    topRef = currConstraint.top().getReferenceComponent();
                }
                Component bottomRef = constraint.bottom().getReferenceComponent();
                currConstraint = constraint;
                iter = 0;
                while (bottomRef != null) {
                    if (iter++ >= maxIterations) break;
                    vInsets += Math.max(0, bottomRef.getOuterPreferredH() * currConstraint.bottom().getReferencePosition());
                    currConstraint = getOrCreateConstraint(bottomRef);
                    bottomRef = currConstraint.bottom().getReferenceComponent();
                }

                Component leftRef = constraint.left().getReferenceComponent();
                currConstraint = constraint;
                iter = 0;
                while (leftRef != null) {
                    if (iter++ >= maxIterations) break;
                    hInsets += Math.max(0, leftRef.getOuterPreferredW() * currConstraint.left().getReferencePosition());
                    currConstraint = getOrCreateConstraint(leftRef);
                    leftRef = currConstraint.left().getReferenceComponent();
                }

                Component rightRef = constraint.right().getReferenceComponent();
                currConstraint = constraint;
                iter = 0;
                while (rightRef != null) {
                    if (iter++ >= maxIterations) break;
                    hInsets += Math.max(0, rightRef.getOuterPreferredW() * currConstraint.right().getReferencePosition());
                    currConstraint = getOrCreateConstraint(rightRef);
                    rightRef = currConstraint.right().getReferenceComponent();
                }
                */

            }
            maxHeight = Math.max(maxHeight, cmp.getPreferredH() + cmp.getStyle().getMarginTop() + cmp.getStyle().getMarginBottom() + vInsets);
            maxWidth = Math.max(maxWidth, cmp.getPreferredW() + cmp.getStyle().getMarginLeftNoRTL() + cmp.getStyle().getMarginRightNoRTL() + hInsets);

        }
        Style s = parent.getStyle();
        Dimension d = new Dimension(maxWidth + s.getPaddingLeftNoRTL() + s.getPaddingRightNoRTL(),
                maxHeight + s.getPaddingTop() + s.getPaddingBottom() + parent.getBottomGap());
        if (preferredWidthMM > 0) {
            int minW = Display.getInstance().convertToPixels(preferredWidthMM);
            if (d.getWidth() < minW) {
                d.setWidth(minW);
            }
        }

        if (preferredHeightMM > 0) {
            int minH = Display.getInstance().convertToPixels(preferredHeightMM);
            if (d.getHeight() < Display.getInstance().convertToPixels(preferredHeightMM)) {
                d.setHeight(minH);
            }
        }

        if (requiresSecondPassToCalculatePercentInsets) {
            // We will do a second pass to deal with percent unit insets
            // since these were set to have zero preferred sizes in the calculation.
            // This is still a bit of a hack as it only deals with components that
            // don't depend on any other components.  E.g. If we have a label that is
            // supposed to have a top inset of 75%.  The preferred height should then
            // be 4 times the preferred height of the label rather than just the
            // preferred height of the label itself.
            // This still doesn't deal with the case where there is another label
            // that references that label and has an inset of an additional 20%
            // Ref https://github.com/codenameone/CodenameOne/issues/2720
            float maxHRatio = 0;
            float maxWRatio = 0;
            for (int i = 0; i < numOfcomponents; i++) {
                Component cmp = parent.getComponentAt(i);
                LayeredLayoutConstraint constraint = (LayeredLayoutConstraint) getComponentConstraint(cmp);
                if (constraint != null) {
                    float hRatio = 0;
                    if (constraint.top().unit == UNIT_PERCENT && constraint.top().referenceComponent == null) {
                        hRatio += constraint.top().value / 100f;
                    }
                    if (constraint.bottom().unit == UNIT_PERCENT && constraint.bottom().referenceComponent == null) {
                        hRatio += constraint.bottom().value / 100f;
                    }
                    hRatio = Math.min(1f, hRatio);
                    maxHRatio = Math.max(maxHRatio, hRatio);

                    float wRatio = 0;
                    if (constraint.left().unit == UNIT_PERCENT && constraint.left().referenceComponent == null) {
                        wRatio += constraint.left().value / 100f;
                    }
                    if (constraint.right().unit == UNIT_PERCENT && constraint.right().referenceComponent == null) {
                        wRatio += constraint.right().value / 100f;
                    }
                    wRatio = Math.min(1f, wRatio);
                    maxWRatio = Math.max(maxWRatio, wRatio);
                }
            }
            if (maxHRatio > 0 && maxHRatio < 1) {
                d.setHeight(Math.round(d.getHeight() / (1 - maxHRatio)));
            }
            if (maxWRatio > 0 && maxWRatio < 1) {
                d.setWidth(Math.round(d.getWidth() / (1 - maxWRatio)));
            }
        }
        return d;
    }

    /// {@inheritDoc}
    @Override
    public String toString() {
        return "LayeredLayout";
    }

    /// {@inheritDoc}
    @Override
    public boolean isOverlapSupported() {
        return true;
    }

    /// {@inheritDoc}
    @Override
    public boolean obscuresPotential(Container parent) {
        return true;
    }

    /// Creates a new `LayeredLayoutConstraint`
    public LayeredLayoutConstraint createConstraint() {
        return new LayeredLayoutConstraint();
    }

    @Override
    public boolean overridesTabIndices(Container parent) {
        return true;
    }

    @Override
    protected Component[] getChildrenInTraversalOrder(Container parent) {
        java.util.List<Component> cmps = new ArrayList<Component>();
        for (Component cmp : parent) {
            cmps.add(cmp);
        }

        Collections.sort(cmps, new ChildrenInTraversalOrderComparator());
        return cmps.toArray(new Component[cmps.size()]);
    }

    private static class ChildrenInTraversalOrderComparator implements Comparator<Component> {
        @Override
        public int compare(Component o1, Component o2) {
            if (o1.getY() < o2.getY()) {
                return -1;
            } else if (o1.getY() > o2.getY()) {
                return 1;
            } else {
                if (o1.getX() < o2.getX()) {
                    return -1;
                } else if (o1.getX() > o2.getX()) {
                    return 1;
                } else {
                    return 0;
                }
            }
        }

    }

    /// A class that encapsulates the insets for a component in layered layout.
    public class LayeredLayoutConstraint {

        /// The insets for this constraint.
        private final Inset[] insets = new Inset[]{
                new Inset(Component.TOP),
                new Inset(Component.LEFT),
                new Inset(Component.BOTTOM),
                new Inset(Component.RIGHT)
        };
        /// The component that this constraint belongs to.  If you try to add
        /// this constraint to a different component, then it will cause a copy
        /// to be made rather than using the same component so that constraints
        /// to bleed into other components.
        private Component cmp;
        /// Anchors used for percentage insets
        private float percAnchorH = 0f;
        private float percAnchorV = 0f;

        /// Gets the insets as a string.
        @Override
        public String toString() {
            return getInsetsAsString(true);
        }

        private LayeredLayout outer() {
            return LayeredLayout.this;
        }

        /// Recursively fixes all dependencies so that they are contained inside
        /// the provided parent.  A dependency is a "referenceComponent".
        ///
        /// #### Parameters
        ///
        /// - `parent`: The parent container within which all dependencies should reside.
        ///
        /// #### Returns
        ///
        /// Self for chaining.
        ///
        /// #### See also
        ///
        /// - #setReferenceComponents(com.codename1.ui.Component...)
        public LayeredLayoutConstraint fixDependencies(Container parent) {
            for (Inset inset : insets) {
                inset.fixDependencies(parent);
            }
            return this;
        }

        /// Checks to see if this constraint has any circular dependencies.  E.g.
        /// Component A has an inset that has Component B as a reference, which has
        /// an inset that depends on Component A.
        ///
        /// #### Parameters
        ///
        /// - `start`: The start component to check.
        ///
        /// #### Returns
        ///
        /// True this forms a circular dependency.
        public boolean hasCircularDependency(Component start) {
            return dependsOn(start);
        }

        /// Gets the inset for a particular side.
        ///
        /// #### Parameters
        ///
        /// - `inset`: @param inset One of `Component#TOP`, `Component#BOTTOM`, `Component#LEFT` or
        ///              `Component#RIGHT`.
        ///
        /// #### Returns
        ///
        /// The inset.
        public Inset getInset(int inset) {
            return insets[inset];
        }

        /// Makes a full copy of this inset.
        public LayeredLayoutConstraint copy() {
            return copyTo(new LayeredLayoutConstraint());
        }

        /// Copies the settings of this constraint into another constraint.
        ///
        /// #### Parameters
        ///
        /// - `dest`: The inset to copy to.
        ///
        /// #### Returns
        ///
        /// Self for chaining.
        public LayeredLayoutConstraint copyTo(LayeredLayoutConstraint dest) {
            for (int i = 0; i < 4; i++) {
                //Inset inset = new Inset(i);
                dest.insets[i] = insets[i].copyTo(dest.insets[i]);
            }
            return dest;
        }

        /// Returns a reference box within which insets of the given component are calculated.  If
        /// `cmp` has no reference components in any of its insets, then the resulting box will
        /// just bee the inner box of the parent (e.g. the parent's inner bounds.
        ///
        /// #### Parameters
        ///
        /// - `parent`: The parent container.
        ///
        /// - `parent`
        ///
        /// - `box`: An out parameter.  This will store the bounds of the box.
        ///
        /// #### Returns
        ///
        /// The reference box.  (This will be the same object that is passed in the box parameter.
        public Rectangle getReferenceBox(Container parent, Rectangle box) {
            return getReferenceBox(parent, null, box);

        }

        /// Returns a reference box within which insets of the given component are calculated.  If
        /// `cmp` has no reference components in any of its insets, then the resulting box will
        /// just bee the inner box of the parent (e.g. the parent's inner bounds.
        ///
        /// #### Parameters
        ///
        /// - `parent`: The parent container.
        ///
        /// - `cmp`: The component whose reference box we are obtaining.  Not used.  May be null.
        ///
        /// - `box`: An out parameter.  This will store the bounds of the box.
        ///
        /// #### Returns
        ///
        /// The reference box.  (This will be the same object that is passed in the box parameter.
        ///
        /// #### Deprecated
        ///
        /// Use `com.codename1.ui.geom.Rectangle)` instead.
        public Rectangle getReferenceBox(Container parent, Component cmp2, Rectangle box) {
            Style parentStyle = parent.getStyle();
            //Style cmpStyle = cmp.getStyle();

            if (top().getReferenceComponent() == null) {
                box.setY(parentStyle.getPaddingTop());
            } else {
                Component ref = top().getReferenceComponent();
                box.setY((int) (getOuterY(ref) + (top().getReferencePosition() * getOuterHeight(ref))));
            }

            if (left().getReferenceComponent() == null) {
                box.setX(parentStyle.getPaddingLeftNoRTL());
            } else {
                Component ref = left().getReferenceComponent();
                box.setX((int) (getOuterX(ref) + (left().getReferencePosition() * getOuterWidth(ref))));
            }

            if (right().getReferenceComponent() == null) {
                box.setWidth(parent.getLayoutWidth() - box.getX() - parentStyle.getPaddingRightNoRTL() - parent.getSideGap());
            } else {
                Component ref = right().getReferenceComponent();
                int refX = (int) (getOuterX(ref) + getOuterWidth(ref) - (right().getReferencePosition() * getOuterWidth(ref)));
                box.setWidth(refX - box.getX());
            }

            if (bottom().getReferenceComponent() == null) {
                box.setHeight(parent.getLayoutHeight() - box.getY() - parentStyle.getPaddingBottom() - parent.getBottomGap());
            } else {
                Component ref = bottom().getReferenceComponent();
                int refY = (int) (getOuterY(ref) + getOuterHeight(ref) - (bottom().getReferencePosition() * getOuterHeight(ref)));
                box.setHeight(refY - box.getY());
            }
            return box;
        }

        /// Returns a reference box within which insets of the given component are calculated.  If
        /// `cmp` has no reference components in any of its insets, then the resulting box will
        /// just bee the inner box of the parent (e.g. the parent's inner bounds.
        ///
        /// #### Parameters
        ///
        /// - `parent`: The parent container.
        ///
        /// - `cmp`: The component whose reference box we are obtaining.
        ///
        /// #### Returns
        ///
        /// The reference box.
        ///
        /// #### Deprecated
        ///
        /// Use {@link #getReferenceBox(com.codename1.ui.Container)
        public Rectangle getReferenceBox(Container parent, Component cmp) {
            return getReferenceBox(parent, cmp, new Rectangle());
        }

        /// Returns a reference box within which insets of the given component are calculated.  If
        /// `cmp` has no reference components in any of its insets, then the resulting box will
        /// just bee the inner box of the parent (e.g. the parent's inner bounds.
        ///
        /// #### Parameters
        ///
        /// - `parent`: The parent container.
        ///
        /// #### Returns
        ///
        /// The reference box.
        public Rectangle getReferenceBox(Container parent) {
            return getReferenceBox(parent, (Component) null);
        }

        /// Shifts the constraint by the specified number of pixels while maintaining the same units.  This is
        /// used mainly in the GUI builder to facilitate dragging and resizing of the component.
        ///
        /// #### Parameters
        ///
        /// - `x`: The number of pixels that the insets should be shifted on the x axis.
        ///
        /// - `y`: The number of pixels that the insets should be shifted on the y axis.
        ///
        /// - `preferMM`: @param preferMM If an inset needs to be switched from flexible to fixed, then this indicates where it will
        ///                 be changed to millimetres or pixels.  true for millimetres.
        ///
        /// - `parent`: The parent container in which calculations should be performed.
        ///
        /// #### Returns
        ///
        /// Self for chaining.
        ///
        /// #### See also
        ///
        /// - #translateMM(float, float, boolean, com.codename1.ui.Container)
        public LayeredLayoutConstraint translatePixels(int x, int y, boolean preferMM, Container parent) {
            if (y != 0) {
                if (top().getUnit() == UNIT_BASELINE) {
                    top().changeUnits(preferMM ? UNIT_DIPS : UNIT_PIXELS);
                } else if (top().isFlexible() && top().autoIsClipped) {
                    top().changeUnits(preferMM ? UNIT_DIPS : UNIT_PIXELS);
                }
                if (bottom().isFlexible() && bottom().autoIsClipped) {
                    bottom().changeUnits(preferMM ? UNIT_DIPS : UNIT_PIXELS);
                }
                if (top().isFlexible() && bottom().isFlexible()) {
                    // Both top and bottom are flexible... we need to make one of these
                    // fixed
                    if (y > 0) {
                        // we're moving it to toward the bottom, so we'll choose the bottom
                        // as an anchor point.
                        bottom().translatePixels(-y, preferMM, parent);
                    } else {
                        top().translatePixels(y, preferMM, parent);
                    }
                } else {
                    if (top().isFixed()) {
                        top().translatePixels(y, preferMM, parent);
                    }
                    if (bottom().isFixed()) {
                        bottom().translatePixels(-y, preferMM, parent);
                    }
                }
            }
            if (x != 0) {
                if (left().isFlexible() && left().autoIsClipped) {
                    left().changeUnits(preferMM ? UNIT_DIPS : UNIT_PIXELS);

                }
                if (right().isFlexible() && right().autoIsClipped) {
                    right().changeUnits(preferMM ? UNIT_DIPS : UNIT_PIXELS);
                }
                if (left().isFlexible() && right().isFlexible()) {
                    // Both top and bottom are flexible... we need to make one of these
                    // fixed
                    if (x > 0) {
                        // we're moving it to toward the bottom, so we'll choose the bottom
                        // as an anchor point.
                        right().translatePixels(-x, preferMM, parent);
                    } else {
                        left().translatePixels(x, preferMM, parent);
                    }
                } else {
                    if (left().isFixed()) {
                        left().translatePixels(x, preferMM, parent);
                    }
                    if (right().isFixed()) {
                        right().translatePixels(-x, preferMM, parent);
                    }
                }
            }
            return this;
        }

        /// Shifts the constraint by the specified number of millimetres while maintaining the same units.  This is
        /// used mainly in the GUI builder to facilitate dragging and resizing of the component.
        ///
        /// #### Parameters
        ///
        /// - `x`: The number of pixels that the insets should be shifted on the x axis.
        ///
        /// - `y`: The number of pixels that the insets should be shifted on the y axis.
        ///
        /// - `preferMM`: @param preferMM If an inset needs to be switched from flexible to fixed, then this indicates where it will
        ///                 be changed to millimetres or pixels.  true for millimetres.
        ///
        /// - `parent`: The parent container in which calculations should be performed.
        ///
        /// #### Returns
        ///
        /// Self for chaining.
        ///
        /// #### See also
        ///
        /// - #translatePixels(int, int, boolean, com.codename1.ui.Container)
        public LayeredLayoutConstraint translateMM(float x, float y, boolean preferMM, Container parent) {
            return translatePixels(Display.getInstance().convertToPixels(x), Display.getInstance().convertToPixels(y), preferMM, parent);

        }

        /// Gets the set of insets on this constraint that are fixed.  An inset is
        /// considered fixed if it's unit is NOT `#UNIT_AUTO`.
        public Collection<Inset> getFixedInsets() {
            ArrayList<Inset> out = new ArrayList<Inset>();
            for (Inset i : insets) {
                if (i.unit != UNIT_AUTO) {
                    out.add(i);
                }
            }
            return out;
        }

        /// Gets the set of insets in this constraint that are flexible.  An inset is
        /// considered flexible if it's unit is `#UNIT_AUTO`.
        public Collection<Inset> getFlexibleInsets() {
            ArrayList<Inset> out = new ArrayList<Inset>();
            for (Inset i : insets) {
                if (i.unit == UNIT_AUTO) {
                    out.add(i);
                }
            }
            return out;
        }

        /// Gets the reference positions of this constraint as a string.
        ///
        /// #### Parameters
        ///
        /// - `withLabels`: @param withLabels True to return the string in CSS format:  e.g. `"top:1.0; right:0; bottom:1.0; left:1.0"`  false
        ///                   to return as a space-delimited string of inset reference positions in the order "top right bottom left".  E.g. "1.0 0 1.0 1.0"
        ///
        /// #### Returns
        ///
        /// The reference positions as a string.
        public String getReferencePositionsAsString(boolean withLabels) {
            StringBuilder sb = new StringBuilder();
            if (withLabels) {
                sb.append("top:").append(top().referencePosition).append("; ")
                        .append("right:").append(right().referencePosition).append("; ")
                        .append("bottom:").append(bottom().referencePosition).append("; ")
                        .append("left:").append(left().referencePosition);
            } else {
                sb.append(top().referencePosition).append(" ")
                        .append(right().referencePosition).append(" ")
                        .append(bottom().referencePosition).append(" ")
                        .append(left().referencePosition);
            }
            return sb.toString();
        }

        /// Sets the reference component positions for this constraint from a string.  The string format
        /// may be either using labels following the same output format of getReferencePositionsAsString(true)
        /// or as a space-delimited string (e.g. getReferencePositionsAsString(false).  When using the label
        /// format, you may provide one or more inset values in the string.  E.g. the following are all acceptable:
        ///
        /// - top:1.0; left:0; right:0; bottom:1.0
        ///
        /// - left:0.5
        ///
        /// - left:1.0; right:0.5
        ///
        /// If you provide the positions as a space-delimited string, then they are expected to follow the same format
        /// as is used in CSS for providing [margin](https://developer.mozilla.org/en/docs/Web/CSS/margin). To summarize:
        ///
        /// `//Apply to all four sides
        /// 1.0
        ///
        /// //vertical | horizontal
        /// 1.0 0
        ///
        /// // top | horizontal | bottom
        /// 1.0 0.0 0.5
        ///
        /// // top | right | bottom | left
        /// 1.0 1.0 1.0 1.0`
        ///
        /// **Interpretation of Reference Positions:**
        ///
        /// When an inset includes a reference component, that means that the inset is "anchored" to that
        /// reference component.  I.e. An inset of 1mm is measured 1mm from the outer edge of the
        /// reference component.  By default it chooses the edge of on the *same side* as the inset.  So
        /// if this is a "left" inset, then it will measure against the "left" outer edge of the reference component.
        /// This is the meaning of a 0 value for the associated reference positions.
        ///
        /// A reference position of 1.0 will start measuring from the opposite edge.  So for a "left" inset,
        /// it will measure from the "right" outer edge of the reference component.  You can choose any real value for the
        /// reference position, and it will cause the measurement to be scaled accordingly.  E.g. 0.5. would measure
        /// from the center point of the reference component.
        ///
        /// #### Parameters
        ///
        /// - `positionsStr`: The reference positions.
        ///
        /// #### Returns
        ///
        /// Self for chaining.
        public LayeredLayoutConstraint setReferencePositions(String positionsStr) {
            positionsStr = positionsStr.trim();
            LayeredLayoutConstraint cnst = this;
            if (positionsStr.indexOf(":") != -1) {
                String[] parts = Util.split(positionsStr, ";");
                for (String part : parts) {
                    if (part.trim().length() == 0) {
                        continue;
                    }
                    String[] kv = Util.split(part, ":");
                    String key = kv[0].trim();
                    String val = kv[1].trim();
                    if ("top".equals(key)) {
                        cnst.top().referencePosition = Float.parseFloat(val);
                    } else if ("bottom".equals(key)) {
                        cnst.bottom().referencePosition = Float.parseFloat(val);
                    } else if ("left".equals(key)) {
                        cnst.left().referencePosition = Float.parseFloat(val);
                    } else if ("right".equals(key)) {
                        cnst.right().referencePosition = Float.parseFloat(val);
                    }


                }
            } else {
                String[] parts = Util.split(positionsStr, " ");
                if (parts.length == 1) {
                    float f = Float.parseFloat(parts[0]);
                    top().referencePosition = f;
                    right().referencePosition = f;
                    bottom().referencePosition = f;
                    left().referencePosition = f;

                } else if (parts.length == 2) {
                    float f0 = Float.parseFloat(parts[0]);
                    float f1 = Float.parseFloat(parts[1]);
                    top().referencePosition = f0;
                    bottom().referencePosition = f0;
                    left().referencePosition = f1;
                    right().referencePosition = f1;
                } else if (parts.length == 3) {
                    float f0 = Float.parseFloat(parts[0]);
                    float f1 = Float.parseFloat(parts[1]);
                    float f2 = Float.parseFloat(parts[2]);
                    top().referencePosition = f0;
                    left().referencePosition = f1;
                    right().referencePosition = f1;
                    bottom().referencePosition = f2;
                } else if (parts.length == 4) {
                    float f0 = Float.parseFloat(parts[0]);
                    float f1 = Float.parseFloat(parts[1]);
                    float f2 = Float.parseFloat(parts[2]);
                    float f3 = Float.parseFloat(parts[3]);
                    top().referencePosition = f0;
                    right().referencePosition = f1;
                    bottom().referencePosition = f2;
                    left().referencePosition = f3;
                }
            }
            return this;
        }

        /// Gets the reference component indexes within the provided parent container as a string.
        /// If an inset doesn't have a reference component, then the corresponding index will be -1.
        ///
        /// Use the withLabels parameter to choose whether to include labels with the indices or not.  E.g:
        ///
        /// `String indices = getReferenceComponentIndicesAsString(parent, true);
        /// // Would return something like
        /// // "top:-1; right:2; bottom:-1; left: 0"
        ///
        /// indices = getReferenceComponentIndicesAsString(parent, false);
        /// // Would return something like:
        /// // "-1 2 -1 0"  (i.e. Top Right Bottom Left)
        ///
        /// // Interpretation:
        /// //   Top inset has no reference component
        /// //   Right inset has component with index 2 (i.e. parent.getComponentIndex(rightReferenceComponent) == 2)
        /// //   Bottom inset has no reference component
        /// //   Left inset has component with index 0 as a reference component.`
        ///
        /// #### Parameters
        ///
        /// - `parent`
        ///
        /// - `withLabels`
        public String getReferenceComponentIndicesAsString(Container parent, boolean withLabels) {
            fixDependencies(parent);
            StringBuilder sb = new StringBuilder();
            if (withLabels) {
                if (top().referenceComponent != null && top().referenceComponent.getParent() != null) {
                    Component cmp = top().referenceComponent;
                    sb.append("top:").append(cmp.getParent().getComponentIndex(cmp)).append("; ");
                } else {
                    sb.append("top:-1; ");
                }
                if (right().referenceComponent != null && right().referenceComponent.getParent() != null) {
                    Component cmp = right().referenceComponent;
                    sb.append("right:").append(cmp.getParent().getComponentIndex(cmp)).append("; ");
                } else {
                    sb.append("right:-1; ");
                }
                if (bottom().referenceComponent != null && bottom().referenceComponent.getParent() != null) {
                    Component cmp = bottom().referenceComponent;
                    sb.append("bottom:").append(cmp.getParent().getComponentIndex(cmp)).append("; ");
                } else {
                    sb.append("bottom:-1; ");
                }

                if (left().referenceComponent != null && left().referenceComponent.getParent() != null) {
                    Component cmp = left().referenceComponent;
                    sb.append("left:").append(cmp.getParent().getComponentIndex(cmp)).append("; ");
                } else {
                    sb.append("left:-1");
                }
            } else {
                if (top().referenceComponent != null && top().referenceComponent.getParent() != null) {
                    Component cmp = top().referenceComponent;
                    sb.append(cmp.getParent().getComponentIndex(cmp)).append(" ");
                } else {
                    sb.append("-1 ");
                }
                if (right().referenceComponent != null && right().referenceComponent.getParent() != null) {
                    Component cmp = right().referenceComponent;
                    sb.append(cmp.getParent().getComponentIndex(cmp)).append(" ");
                } else {
                    sb.append("-1 ");
                }
                if (bottom().referenceComponent != null && bottom().referenceComponent.getParent() != null) {
                    Component cmp = bottom().referenceComponent;
                    sb.append(cmp.getParent().getComponentIndex(cmp)).append(" ");
                } else {
                    sb.append("-1 ");
                }

                if (left().referenceComponent != null && left().referenceComponent.getParent() != null) {
                    Component cmp = left().referenceComponent;
                    sb.append(cmp.getParent().getComponentIndex(cmp)).append(" ");
                } else {
                    sb.append("-1");
                }
            }

            return sb.toString();

        }

        /// Sets the reference components of the insets of this constraint as indices of the provided parent
        /// container.
        ///
        /// #### Parameters
        ///
        /// - `parent`: The parent container whose children are to be used as reference components.
        ///
        /// - `indices`: @param indices The indices to set as the reference components.
        ///
        ///
        /// The string format
        ///                may be either using labels following the same output format of cnst.getReferenceComponentIndicesAsString(true)
        ///                or as a space-delimited string (e.g. cnst.getReferenceComponentIndicesAsString(false).  When using the label
        ///                format, you may provide one or more inset values in the string.  E.g. the following are all acceptable:
        ///
        ///
        ///
        ///
        ///
        /// - top:-1; left:0; right:0; bottom:1
        ///
        /// - left:1
        ///
        /// - left:10; right:-1
        ///
        ///
        ///
        ///
        ///
        ///
        ///                If you provide the positions as a space-delimited string, then they are expected to follow the same format
        ///                as is used in CSS for providing [margin](https://developer.mozilla.org/en/docs/Web/CSS/margin). To summarize:
        ///
        ///
        ///
        ///
        ///                `//Set component at index 0 as reference for all 4 insets.
        ///                0
        ///
        ///
        ///                //vertical insets use component index 2 | horizontal insets use component index 1
        ///                2 1
        ///
        ///
        ///                // top | horizontal | bottom
        ///                -1 3 10
        ///
        ///
        ///                // top | right | bottom | left
        ///                -1 -1 -1 -1`
        ///
        ///
        ///
        ///
        ///
        /// **Note: An index of -1 means that the corresponding inset has no reference component.**
        public LayeredLayoutConstraint setReferenceComponentIndices(Container parent, String indices) {
            indices = indices.trim();
            LayeredLayoutConstraint cnst = this;
            if (indices.indexOf(":") != -1) {
                String[] parts = Util.split(indices, ";");
                for (String part : parts) {
                    if (part.trim().length() == 0) {
                        continue;
                    }
                    String[] kv = Util.split(part, ":");
                    String key = kv[0].trim();
                    String val = kv[1].trim();
                    if ("top".equals(key)) {
                        int index = Integer.parseInt(val);
                        if (index >= 0) {
                            cnst.top().referenceComponent = parent.getComponentAt(index);
                        } else {
                            cnst.top().referenceComponent = null;
                        }
                    } else if ("bottom".equals(key)) {
                        int index = Integer.parseInt(val);
                        if (index >= 0) {
                            cnst.bottom().referenceComponent = parent.getComponentAt(index);
                        } else {
                            cnst.bottom().referenceComponent = null;
                        }
                    } else if ("left".equals(key)) {
                        int index = Integer.parseInt(val);
                        if (index >= 0) {
                            cnst.left().referenceComponent = parent.getComponentAt(index);
                        } else {
                            cnst.left().referenceComponent = null;
                        }
                    } else if ("right".equals(key)) {
                        int index = Integer.parseInt(val);
                        if (index >= 0) {
                            cnst.right().referenceComponent = parent.getComponentAt(index);
                        } else {
                            cnst.right().referenceComponent = null;
                        }
                    }


                }
            } else {
                String[] parts = Util.split(indices, " ");
                if (parts.length == 1) {
                    int i0 = Integer.parseInt(parts[0]);
                    if (i0 == -1) {
                        top().referenceComponent = null;
                        right().referenceComponent = null;
                        bottom().referenceComponent = null;
                        left().referenceComponent = null;
                    } else {
                        Component cmp = parent.getComponentAt(i0);
                        top().referenceComponent = cmp;
                        right().referenceComponent = cmp;
                        bottom().referenceComponent = cmp;
                        left().referenceComponent = cmp;
                    }
                } else if (parts.length == 2) {
                    int i0 = Integer.parseInt(parts[0]);
                    int i1 = Integer.parseInt(parts[1]);
                    Component cmp = null;
                    if (i0 != -1) {
                        cmp = parent.getComponentAt(i0);
                    }
                    top().referenceComponent = cmp;
                    bottom().referenceComponent = cmp;

                    cmp = null;
                    if (i1 != -1) {
                        cmp = parent.getComponentAt(i1);
                    }
                    left().referenceComponent = cmp;
                    right().referenceComponent = cmp;
                } else if (parts.length == 3) {
                    int i0 = Integer.parseInt(parts[0]);
                    int i1 = Integer.parseInt(parts[1]);
                    int i2 = Integer.parseInt(parts[2]);
                    Component cmp = null;
                    if (i0 != -1) {
                        cmp = parent.getComponentAt(i0);
                    }
                    top().referenceComponent = cmp;
                    cmp = null;
                    if (i1 != -1) {
                        cmp = parent.getComponentAt(i1);
                    }
                    left().referenceComponent = cmp;
                    right().referenceComponent = cmp;
                    cmp = null;
                    if (i2 != -1) {
                        cmp = parent.getComponentAt(i2);
                    }
                    bottom().referenceComponent = cmp;

                } else if (parts.length == 4) {
                    int i0 = Integer.parseInt(parts[0]);
                    int i1 = Integer.parseInt(parts[1]);
                    int i2 = Integer.parseInt(parts[2]);
                    int i3 = Integer.parseInt(parts[3]);
                    Component cmp = null;
                    if (i0 != -1) {
                        cmp = parent.getComponentAt(i0);
                    }
                    top().referenceComponent = cmp;
                    cmp = null;
                    if (i1 != -1) {
                        cmp = parent.getComponentAt(i1);
                    }
                    right().referenceComponent = cmp;
                    cmp = null;
                    if (i2 != -1) {
                        cmp = parent.getComponentAt(i2);
                    }

                    bottom().referenceComponent = cmp;
                    cmp = null;
                    if (i3 != -1) {
                        cmp = parent.getComponentAt(i3);
                    }
                    left().referenceComponent = cmp;
                }
            }
            return this;
        }

        /// Gets the insets of this constraint as a string. If withLabels is true, then it
        /// will return a string of the format:
        ///
        /// top:2mm; right:0; bottom:10%; left:auto
        ///
        /// If withLabels is false then it will return a space-delimited string with
        /// the inset values ordered "top right bottom left" (the same as for CSS margins) order.
        ///
        /// #### Parameters
        ///
        /// - `withLabels`
        public String getInsetsAsString(boolean withLabels) {
            StringBuilder sb = new StringBuilder();
            if (withLabels) {
                sb.append("top:").append(top().getValueAsString()).append("; ")
                        .append("right:").append(right().getValueAsString()).append("; ")
                        .append("bottom:").append(bottom().getValueAsString()).append("; ")
                        .append("left:").append(left().getValueAsString());
            } else {
                sb.append(top().getValueAsString()).append(" ")
                        .append(right().getValueAsString()).append(" ")
                        .append(bottom().getValueAsString()).append(" ")
                        .append(left().getValueAsString());
            }
            return sb.toString();
        }

        /// Sets the reference components for the constraint.
        ///
        /// #### Parameters
        ///
        /// - `refs`: @param refs May contain 1, 2, 3, or 4 values.  If only 1 value is passed, then it is
        ///             set on all 4 insets.  If two values are passed, then the first is set on the top and bottom
        ///             insets, and the 2nd is set on the left and right insets (i.e. vertical | horizontal).
        ///             If 3 values are passed, then, they are used for top, horizontal, and bottom.
        ///             If 4 values are passed, then they are used for top, right, bottom, left (in that order).
        ///
        /// #### Returns
        ///
        /// Self for chaining.
        public LayeredLayoutConstraint setReferenceComponents(Component... refs) {
            if (refs.length == 1) {
                top().referenceComponent = refs[0];
                right().referenceComponent = refs[0];
                bottom().referenceComponent = refs[0];
                left().referenceComponent = refs[0];
            } else if (refs.length == 2) {
                top().referenceComponent = refs[0];
                bottom().referenceComponent = refs[0];
                left().referenceComponent = refs[1];
                right().referenceComponent = refs[1];
            } else if (refs.length == 3) {
                top().referenceComponent = refs[0];
                left().referenceComponent = refs[1];
                right().referenceComponent = refs[1];
                bottom().referenceComponent = refs[2];
            } else if (refs.length == 4) {
                top().referenceComponent = refs[0];
                right().referenceComponent = refs[1];
                bottom().referenceComponent = refs[2];
                left().referenceComponent = refs[3];
            }
            return this;
        }

        /// Sets the reference positions for the constraint.
        ///
        /// **Interpretation of Reference Positions:**
        ///
        /// When an inset includes a reference component, that means that the inset is "anchored" to that
        /// reference component.  I.e. An inset of 1mm is measured 1mm from the outer edge of the
        /// reference component.  By default it chooses the edge of on the *same side* as the inset.  So
        /// if this is a "left" inset, then it will measure against the "left" outer edge of the reference component.
        /// This is the meaning of a 0 value for the associated reference positions.
        ///
        /// A reference position of 1.0 will start measuring from the opposite edge.  So for a "left" inset,
        /// it will measure from the "right" outer edge of the reference component.  You can choose any real value for the
        /// reference position, and it will cause the measurement to be scaled accordingly.  E.g. 0.5. would measure
        /// from the center point of the reference component.
        ///
        /// #### Parameters
        ///
        /// - `p`: @param p May contain 1, 2, 3, or 4 values.  If only 1 value is passed, then it is
        ///          set on all 4 insets.  If two values are passed, then the first is set on the top and bottom
        ///          insets, and the 2nd is set on the left and right insets (i.e. vertical | horizontal).
        ///          If 3 values are passed, then, they are used for top, horizontal, and bottom.
        ///          If 4 values are passed, then they are used for top, right, bottom, left (in that order).
        ///
        /// #### Returns
        ///
        /// Self for chaining.
        public LayeredLayoutConstraint setReferencePositions(float... p) {
            if (p.length == 1) {
                for (Inset i : insets) {
                    i.referencePosition = p[0];
                }
            } else if (p.length == 2) {
                for (Inset i : insets) {
                    switch (i.side) {
                        case Component.TOP:
                        case Component.BOTTOM:
                            i.referencePosition = p[0];
                            break;
                        default:
                            i.referencePosition = p[1];
                    }
                }
            } else if (p.length == 3) {
                for (Inset i : insets) {
                    switch (i.side) {
                        case Component.TOP:
                            i.referencePosition = p[0];
                            break;
                        case Component.LEFT:
                        case Component.RIGHT:
                            i.referencePosition = p[1];
                            break;
                        default:
                            i.referencePosition = p[2];
                    }
                }
            } else if (p.length == 4) {
                for (Inset i : insets) {
                    switch (i.side) {
                        case Component.TOP:
                            i.referencePosition = p[0];
                            break;
                        case Component.RIGHT:
                            i.referencePosition = p[1];
                            break;
                        case Component.BOTTOM:
                            i.referencePosition = p[2];
                            break;


                        default:
                            i.referencePosition = p[3];
                    }
                }
            }
            return this;
        }

        /// Sets the insets for this constraint as a string.  The string may include labels
        /// or it may be a space delimited string of values with "top right bottom left" order.
        ///
        /// If providing as a space-delimited string of inset values, then you can provide 1, 2, 3, or 4
        /// values.  If only 1 value is passed, then it is
        /// set on all 4 insets.  If two values are passed, then the first is set on the top and bottom
        /// insets, and the 2nd is set on the left and right insets (i.e. vertical | horizontal).
        /// If 3 values are passed, then, they are used for top, horizontal, and bottom.
        /// If 4 values are passed, then they are used for top, right, bottom, left (in that order).
        ///
        /// **Example Inputs**
        ///
        ///
        /// - "0 0 0 0" = all 4 insets are zero pixels
        ///
        /// - "0 1mm" = Vertical insets are zero.  Horizontal insets are 1mm
        ///
        /// - "10% auto 20%" = Top inset is 10%.  Horizontal insets are flexible.  Bottom is 20%
        ///
        /// - "1mm 2mm 3mm 4mm" = Top=1mm, Right=2mm, Bottom=3mm, Left=4mm
        ///
        /// #### Parameters
        ///
        /// - `insetStr`
        ///
        /// #### Returns
        ///
        /// Self for chaining.
        public LayeredLayoutConstraint setInsets(String insetStr) {

            LayeredLayoutConstraint cnst = this;
            if (insetStr.indexOf(":") != -1) {
                String[] parts = Util.split(insetStr, ";");
                for (String part : parts) {
                    if (part.trim().length() == 0) {
                        continue;
                    }
                    String[] kv = Util.split(part, ":");
                    String key = kv[0].trim();
                    String val = kv[1].trim();
                    if ("top".equals(key)) {
                        cnst.top().setValue(val);
                    } else if ("bottom".equals(key)) {
                        cnst.bottom().setValue(val);
                    } else if ("left".equals(key)) {
                        cnst.left().setValue(val);
                    } else if ("right".equals(key)) {
                        cnst.right().setValue(val);
                    }


                }
            } else {
                String[] parts = Util.split(insetStr, " ");
                if (parts.length == 1) {
                    top().setValue(parts[0]);
                    right().setValue(parts[0]);
                    bottom().setValue(parts[0]);
                    left().setValue(parts[0]);

                } else if (parts.length == 2) {
                    top().setValue(parts[0]);
                    bottom().setValue(parts[0]);
                    left().setValue(parts[1]);
                    right().setValue(parts[1]);
                } else if (parts.length == 3) {
                    top().setValue(parts[0]);
                    left().setValue(parts[1]);
                    right().setValue(parts[1]);
                    bottom().setValue(parts[2]);
                } else if (parts.length == 4) {
                    top().setValue(parts[0]);
                    right().setValue(parts[1]);
                    bottom().setValue(parts[2]);
                    left().setValue(parts[3]);
                }
            }
            return this;
        }

        /// Gets the left inset.
        ///
        /// #### Returns
        ///
        /// The left inset
        public Inset left() {
            return insets[Component.LEFT];
        }

        /// Gets the right inset.
        ///
        /// #### Returns
        ///
        /// The right inset.
        public Inset right() {
            return insets[Component.RIGHT];
        }

        /// Gets the top inset
        ///
        /// #### Returns
        ///
        /// The top inset
        public Inset top() {
            return insets[Component.TOP];
        }

        /// Gets the bottom inset.
        ///
        /// #### Returns
        ///
        /// The bottom inset
        public Inset bottom() {
            return insets[Component.BOTTOM];
        }

        /// #### Returns
        ///
        /// anchor used for left and right percentage insets
        public float getPercentInsetAnchorHorizontal() {
            return this.percAnchorH;
        }

        /// Sets the anchor used for left and right percentage insets. An anchor
        /// of 0 points to the component's edge which is on that side
        /// the inset refers to (e.g. in case of the left inset the left edge).
        /// An anchor of 1 points to the edge on the opposite side.
        /// By default 0 is used as anchor.
        ///
        /// #### Parameters
        ///
        /// - `anchor`
        ///
        /// #### Returns
        ///
        /// Self for chaining
        public LayeredLayoutConstraint setPercentInsetAnchorHorizontal(float anchor) {
            this.percAnchorH = anchor;
            return this;
        }

        /// #### Returns
        ///
        /// anchor used for top and bottom percentage insets
        public float getPercentInsetAnchorVertical() {
            return this.percAnchorV;
        }

        /// Sets the anchor used for top and bottom percentage insets. An anchor
        /// of 0 points to the component's edge which is on that side
        /// the inset refers to (e.g. in case of the top inset the top edge).
        /// An anchor of 1 points to the edge on the opposite side.
        /// By default 0 is used as anchor.
        ///
        /// #### Parameters
        ///
        /// - `anchor`
        ///
        /// #### Returns
        ///
        /// Self for chaining
        public LayeredLayoutConstraint setPercentInsetAnchorVertical(float anchor) {
            this.percAnchorV = anchor;
            return this;
        }

        /// Gets the constraint itself.
        public LayeredLayoutConstraint constraint() {
            return this;
        }

        //private Rectangle preferredBounds;

        /// Gets the dependencies (i.e. recursively gets  all reference components).
        ///
        /// #### Parameters
        ///
        /// - `deps`: A set to add the dependencies to. (An "out" parameter).
        ///
        /// #### Returns
        ///
        /// The set of dependencies.  Same as dep parameter.
        public Set<Component> getDependencies(Set<Component> deps) {
            for (Inset inset : insets) {
                inset.getDependencies(deps);
            }
            return deps;
        }

        /// Gets the dependencies (i.e. recursively gets  all reference components).
        ///
        /// #### Returns
        ///
        /// The set of dependencies.
        public Set<Component> getDependencies() {
            return getDependencies(new HashSet<Component>());
        }

        /// Checks to see if this constraint has the given component in its set of dependencies.
        ///
        /// #### Parameters
        ///
        /// - `cmp`: The component to check.
        ///
        /// #### Returns
        ///
        /// @return True if cmp is a reference component of some inset in this
        /// constraint (recursively).
        public boolean dependsOn(Component cmp) {
            return getDependencies().contains(cmp);
        }


        /// Encapsulates an inset.
        public class Inset {
            /// One of
            /// `Component#TOP`, `Component#Bottom`, `Component#LEFT`, `Component#RIGHT`
            private final int side;
            int delta;
            /// The component that is used a reference for this inset.
            /// null for the parent component.
            private Component referenceComponent;
            /// `0.0` = left/top of `#referenceComponent`.  `1.0` for bottom/right or `#referenceComponent`.
            private float referencePosition;
            /// The value of this inset. Interpreted in `#unit` units.
            private float value;
            /// The unit of this inset.
            private byte unit = UNIT_PIXELS;
            /// Caches the preferred value of this inset last time it was calculated.
            private int preferredValue;
            /// The calculated value of this inset in pixels.  This is calculated in the `int, int, int, int)`
            /// method which is only called during layout.  It will be the absolute size of the inset in pixels
            /// including all reference components.
            private int calculatedValue;
            /// The calculated base value of this inset in pixels.  This is calculated during the layout step, so
            /// this will always be the pixel "base" value the last time layout was performed.  The base value
            /// is the absolute value of the reference box inset.  E.g. if this inset has no reference component,
            /// then this will always be zero.  If there is a reference componnet, then this will be the value
            /// of the "zero" point for measuing the inset.  `#calculatedValue` - `#calculatedBaseValue` should
            /// be equal to `#value` (if value is in pixels).
            private int calculatedBaseValue;
            /// Tracks whether the size of the component was clipped during the last layout.  This will occur
            /// when the preferred size of the component would have it overflowing the reference box.  In such cases
            /// the component is "clipped" to not obtain its full preferred value.
            private boolean autoIsClipped;

            /// Creates a new inset for the given side.
            ///
            /// #### Parameters
            ///
            /// - `side`: One of `Component#TOP`, `Component#BOTTOM`, `Component#LEFT`, or `Component#RIGHT`.
            public Inset(int side) {
                this.side = side;
            }

            /// Prints this inset as a string.
            @Override
            public String toString() {
                switch (side) {
                    case Component.TOP:
                        return "top=" + getValueAsString();
                    case Component.BOTTOM:
                        return "bottom=" + getValueAsString();
                    case Component.LEFT:
                        return "left=" + getValueAsString();
                    default:
                        return "right=" + getValueAsString();
                }
            }

            /// Gets the value of this inset as a string. Values will be in the format , e.g.
            /// 2mm, 15%, 5px, auto (meaning it is flexible.
            ///
            /// #### Returns
            ///
            /// The value of this inset as a string.
            public String getValueAsString() {
                switch (unit) {
                    case UNIT_DIPS:
                        return value + "mm";
                    case UNIT_PIXELS:
                        return ((int) value) + "px";
                    case UNIT_PERCENT:
                        return value + "%";
                    case UNIT_AUTO:
                        return "auto";
                    case UNIT_BASELINE:
                        return "baseline";
                    default:
                        break;
                }
                return null;
            }

            /// Sets the value of this inset as a string.  E.g. "2mm", or "2px", or "3%", "auto", or "baseline".
            ///
            /// #### Parameters
            ///
            /// - `value`: The value of this inset.
            ///
            /// #### Returns
            ///
            /// Self for chaining.
            public Inset setValueAsString(String value) {
                setValue(value);
                return this;
            }

            /// Gets the value of this inset as a string rounding to the specified number of decimal places.
            /// Values will be in the format , e.g.
            /// 2mm, 15%, 5px, auto (meaning it is flexible.
            ///
            /// #### Returns
            ///
            /// The value of this inset as a string.
            ///
            /// #### See also
            ///
            /// - #getValueAsString()
            public String getValueAsString(int decimalPlaces) {
                L10NManager l10n = L10NManager.getInstance();

                switch (unit) {
                    case UNIT_DIPS:
                        return l10n.format(value, decimalPlaces) + "mm";
                    case UNIT_PIXELS:
                        return ((int) value) + "px";
                    case UNIT_PERCENT:
                        return l10n.format(value, decimalPlaces) + "%";
                    case UNIT_AUTO:
                        return "auto";
                    case UNIT_BASELINE:
                        return "baseline";
                    default:
                        break;
                }
                return null;
            }

            /// Fixes dependencies in this inset recursively so that all reference
            /// components are children of the given parent container.  If a reference
            /// component is not in the parent, then it will first check to find a
            /// child of parent with the same name as the reference component.
            /// Failing that, it will try to find a child of parent with the
            /// same index.
            ///
            /// If an appropriate match is found, it will replace the referenceComponent
            /// with the match.
            ///
            /// #### Parameters
            ///
            /// - `parent`: The container in which all reference components should reside.
            ///
            /// #### Returns
            ///
            /// Self for chaining.
            private Inset fixDependencies(Container parent) {
                Container refParent;
                if (referenceComponent != null && (refParent = referenceComponent.getParent()) != parent) { //NOPMD CompareObjectsWithEquals
                    // The reference component is not in this parent
                    String name = referenceComponent.getName();
                    boolean found = false;
                    if (name != null && name.length() > 0) {
                        for (Component child : parent) {
                            if (name.equals(child.getName())) {
                                referenceComponent = child;
                                found = true;
                                break;
                            }
                        }
                    }
                    if (!found && refParent != null) {
                        int index = refParent.getComponentIndex(referenceComponent);
                        if (index != -1 && parent.getComponentCount() > index) {
                            referenceComponent = parent.getComponentAt(index);
                            found = true;
                        }
                    }

                    if (found) {
                        LayeredLayoutConstraint refCnst = getOrCreateConstraint(referenceComponent);
                        refCnst.getInset(side).fixDependencies(parent);
                    }

                }
                return this;
            }

            /// Gets the left inset in this constraint.
            ///
            /// #### Returns
            ///
            /// The left inset of the constraint.
            public Inset left() {
                return constraint().left();
            }

            /// Gets the right inset in the constraint.
            ///
            /// #### Returns
            ///
            /// The right inset of the constraint.
            public Inset right() {
                return constraint().right();
            }

            /// Gets the top inset in this constraint.
            ///
            /// #### Returns
            ///
            /// The top inset in this constraint.
            public Inset top() {
                return constraint().top();
            }

            /// Gets the bottom inset in this constraint.
            ///
            /// #### Returns
            ///
            /// The bottom inset.
            public Inset bottom() {
                return constraint().bottom();
            }

            /// Gets the constraint that contains this inset.
            ///
            /// #### Returns
            ///
            /// The parent constraint of this inset.
            public LayeredLayoutConstraint constraint() {
                return LayeredLayoutConstraint.this;
            }

            /// Sets the unit for this constraint.  This doesn't perform any recalculation
            /// on the value.  Just sets the unit.
            ///
            /// #### Parameters
            ///
            /// - `unit`: The unit.  One of `#UNIT_AUTO`, `#UNIT_DIPS`, `#UNIT_PIXELS`, or `#UNIT_PERCENT`.
            ///
            /// #### Returns
            ///
            /// Self for chaining.
            ///
            /// #### See also
            ///
            /// - #setAuto()
            ///
            /// - #setDips()
            ///
            /// - #setPixels()
            ///
            /// - #setPercent()
            ///
            /// - #changeUnits(byte) To change units while recalculating the value to be effectively equivalent.
            public Inset unit(byte unit) {
                if (unit == UNIT_BASELINE && side != Component.TOP) {
                    throw new IllegalArgumentException("baseline unit can only be used on the top inset.");
                }
                this.unit = unit;
                if (unit == UNIT_BASELINE) {
                    referencePosition = 0f;
                    getOppositeInset().setAuto().referenceComponent(null).referencePosition(0f);
                }
                return this;
            }

            /// Sets the units to "auto" (i.e. makes the inset flexible).  Doesn't perform any calculations
            /// on the value.
            ///
            /// #### Returns
            ///
            /// Self for chaining.
            ///
            /// #### See also
            ///
            /// - #unit(byte)
            public Inset setAuto() {
                return unit(UNIT_AUTO);
            }

            /// Sets the units to "dips" (millimetres).  Doesn't perform any calculations on the value.
            ///
            /// #### Returns
            ///
            /// Self for chaining.
            ///
            /// #### See also
            ///
            /// - #unit(byte)
            public Inset setDips() {
                return unit(UNIT_DIPS);
            }

            /// Sets the units to percent.  Doesn't perform any calculations on the value.
            ///
            /// #### Returns
            ///
            /// Self for chaining.
            ///
            /// #### See also
            ///
            /// - #unit(byte)
            public Inset setPercent() {
                return unit(UNIT_PERCENT);
            }

            /// Sets the units to pixels.  Doesn't perform any calculations on the value.
            ///
            /// #### Returns
            ///
            /// Self for chaining.
            public Inset setPixels() {
                return unit(UNIT_PIXELS);
            }

            /// Sets the inset value to the provided number of pixels.  This will chnage the unit
            /// to pixels.
            ///
            /// #### Parameters
            ///
            /// - `px`: The pixel value of the inset.
            ///
            /// #### Returns
            ///
            /// Self for chaining.
            public Inset setPixels(int px) {
                this.value = px;
                return unit(UNIT_PIXELS);
            }

            /// Sets the inset value to the provided dips/millimetre value. This will change
            /// the unit to millimetres.
            ///
            /// #### Parameters
            ///
            /// - `dips`: The inset value in millimetres.
            ///
            /// #### Returns
            ///
            /// Self for chaining.
            public Inset setDips(float dips) {
                this.value = dips;
                return unit(UNIT_DIPS);
            }

            /// Sets the inset value in percentage.  This will change the unit to percentage.
            ///
            /// #### Parameters
            ///
            /// - `percent`: The inset value as a percentage.
            ///
            /// #### Returns
            ///
            /// Self for chaining.
            public Inset setPercent(float percent) {
                if (percent == Float.POSITIVE_INFINITY || percent == Float.NEGATIVE_INFINITY) {
                    throw new IllegalArgumentException("Attempt to set illegal percent value");
                }
                this.value = percent;
                return unit(UNIT_PERCENT);
            }

            /// Sets the reference component for this inset.
            ///
            /// #### Parameters
            ///
            /// - `cmp`: The reference component. (I.e. the component that the inset is "anchored" to).
            ///
            /// #### Returns
            ///
            /// Self for chaining.
            ///
            /// #### See also
            ///
            /// - #referencePosition(float)
            ///
            /// - LayeredLayoutConstraint#setReferenceComponents(com.codename1.ui.Component...)
            public Inset referenceComponent(Component cmp) {
                referenceComponent = cmp;
                return this;
            }

            /// Sets the reference position for this inset.  A value of 0 indicates that the inset
            /// is anchored to the same side of the reference component (e.g. right inset anchored to right edge of reference component,
            /// left inset anchored to left edge of reference component).  A value of 1 indicates that the
            /// inset is anchored to the opposite side of the reference component. E.g. right inset to left edge.
            ///
            /// #### Parameters
            ///
            /// - `position`: The reference position.
            ///
            /// #### Returns
            ///
            /// Self for chaining.
            ///
            /// #### See also
            ///
            /// - #setReferencePositions(java.lang.String)
            ///
            /// - #setReferencePositions(com.codename1.ui.Component, java.lang.String)
            ///
            /// - #setReferencePositions(com.codename1.ui.Component, float...)
            public Inset referencePosition(float position) {
                this.referencePosition = position;
                return this;
            }

            /// Sets the value of this inset.  The interpretation of the value will depend on the `#unit`.
            /// If the unit is `#UNIT_DIPS`, then this value is interpreted in millimetres, etc..
            ///
            /// #### Parameters
            ///
            /// - `value`: The value to set this inset to.
            ///
            /// #### Returns
            ///
            /// Self for chaining.
            public Inset value(float value) {
                this.value = value;
                return this;
            }

            /// Gets the side of this inset. One of `Component#TOP`, `Component#Bottom`, `Component#LEFT`, `Component#RIGHT`
            ///
            /// #### Returns
            ///
            /// The side of this inset.  One of `Component#TOP`, `Component#Bottom`, `Component#LEFT`, `Component#RIGHT`
            public int getSide() {
                return side;
            }

            /// Gets the reference component for this inset.
            ///
            /// #### Returns
            ///
            /// The reference component for this inset.
            ///
            /// #### See also
            ///
            /// - #referenceComponent(com.codename1.ui.Component)
            public Component getReferenceComponent() {
                return referenceComponent;
            }

            /// Gets the reference position for this inset.
            ///
            /// #### Returns
            ///
            /// The reference position for this inset.
            public float getReferencePosition() {
                return referencePosition;
            }

            /// Calculate the preferred value of this inset.
            ///
            /// #### Parameters
            ///
            /// - `parent`: The parent container.
            ///
            /// - `cmp`: The component
            ///
            /// #### Returns
            ///
            /// The preferred value of this inset in pixels.
            public int calcPreferredValue(Container parent, Component cmp) {

                if (referenceComponent == null) {
                    // There is no reference component for this inset so we measure
                    // against the parent component directly.
                    switch (unit) {
                        case UNIT_PIXELS:
                            preferredValue = (int) value;
                            break;
                        case UNIT_DIPS:
                            preferredValue = Display.getInstance().convertToPixels(value);
                            break;
                        case UNIT_PERCENT:
                        case UNIT_AUTO:
                        case UNIT_BASELINE:
                            preferredValue = 0;
                            break;
                        default:
                            throw new RuntimeException("Invalid unit " + unit);
                    }
                    return preferredValue;
                } else {
                    // There is a reference component so we need to add our own value
                    // to the base inset of the reference component.
                    LayeredLayoutConstraint refCnst = (LayeredLayoutConstraint) getComponentConstraint(referenceComponent);
                    int baseValue = 0;
                    if (refCnst != null) {
                        baseValue = refCnst.insets[side].preferredValue;
                    }

                    // We should have already calculated the preferred size of the
                    // reference component.
                    //Dimension refPreferredSize = referenceComponent.getPreferredSize();
                    int refPreferredH = getOuterPreferredH(referenceComponent);
                    int refPreferredW = getOuterPreferredW(referenceComponent);

                    if (referencePosition != 0) {
                        // If the inset is not in reference to the edge of the component
                        // then we need to adjust the base value accordingly.
                        switch (side) {
                            case Component.TOP:
                            case Component.BOTTOM:
                                baseValue += ((float) refPreferredH) * referencePosition;
                                break;
                            default:
                                baseValue += ((float) refPreferredW) * referencePosition;
                        }
                    }

                    // Now we add our own value to the base value.
                    switch (unit) {
                        case UNIT_PIXELS:
                            preferredValue = baseValue + (int) value;
                            break;
                        case UNIT_DIPS:
                            preferredValue = baseValue + Display.getInstance().convertToPixels(value);
                            break;
                        case UNIT_PERCENT:
                        case UNIT_AUTO:
                            preferredValue = baseValue;
                            break;
                        case UNIT_BASELINE: {
                            Style rs = referenceComponent.getStyle();
                            Style s = cmp.getStyle();
                            preferredValue = baseValue + (referenceComponent.getPreferredH() - cmp.getPreferredH()) / 2
                                    + (rs.getFont().getAscent() - s.getFont().getAscent())
                                    + (rs.getPaddingTop() - s.getPaddingTop());
                            break;
                        }
                        default:
                            throw new RuntimeException("Invalid unit " + unit);
                    }
                    return preferredValue;
                }
            }

            /// Calculates the "base" value off of which the inset's value should be calculated.
            ///
            /// #### Parameters
            ///
            /// - `top`: The top "y" coordinate within the parent container from which insets are measured.
            ///
            /// - `left`: The left "x" coordinate within the parent container from which insets are measured.
            ///
            /// - `bottom`: The bottom "y" coordinate within the parent container from which insets are measured.
            ///
            /// - `right`: The right "x" coordinate within the parent container from which insets are measured.
            private int calcBaseValue(int top, int left, int bottom, int right) { //, int paddingTop, int paddingLeft, int paddingBottom, int paddingRight) {
                int h = bottom - top;
                int w = right - left;
                int baseValue = 0;
                if (referenceComponent != null) {
                    switch (side) {
                        case Component.TOP:
                            baseValue = getOuterY(referenceComponent) + (int) (getOuterHeight(referenceComponent) * referencePosition) - top;
                            break;
                        case Component.BOTTOM:
                            baseValue = (bottom - getOuterHeight(referenceComponent) - getOuterY(referenceComponent)) + (int) (getOuterHeight(referenceComponent) * referencePosition);
                            break;
                        case Component.LEFT:
                            baseValue = getOuterX(referenceComponent) + (int) (getOuterWidth(referenceComponent) * referencePosition) - left;
                            break;
                        default:
                            baseValue = (right - getOuterWidth(referenceComponent) - getOuterX(referenceComponent)) + (int) (getOuterWidth(referenceComponent) * referencePosition);
                            break;
                    }
                    calculatedBaseValue = baseValue;
                    return baseValue;
                }

                if (referencePosition != 0) {
                    switch (side) {
                        case Component.TOP:
                        case Component.BOTTOM:
                            baseValue = (int) (h * referencePosition);
                            break;
                        case Component.LEFT:
                        case Component.RIGHT:
                            baseValue = (int) (w * referencePosition);
                            break;
                        default:
                            throw new RuntimeException("Illegal side for inset: " + side);
                    }
                }
                calculatedBaseValue = baseValue;
                return baseValue;
            }

            /// True if this is top or bottom.
            private boolean isVerticalInset() {
                return side == Component.TOP || side == Component.BOTTOM;
            }

            /// True if this is left or right.
            private boolean isHorizontalInset() {
                return side == Component.LEFT || side == Component.RIGHT;
            }


            /// Calculates the actual value of this inset.  This is used inside `com.codename1.ui.Component, int, int, int, int)`.
            ///
            /// #### Parameters
            ///
            /// - `cmp`: The component.
            ///
            /// - `top`
            ///
            /// - `left`
            ///
            /// - `bottom`
            ///
            /// - `right`
            ///
            /// #### Returns
            ///
            /// The actual value of this inset.
            private int calculate(Component cmp, int top, int left, int bottom, int right) {
                if (side == Component.BOTTOM && getOppositeInset().unit == UNIT_BASELINE) {
                    unit = UNIT_AUTO;
                }
                int w = right - left;
                int h = bottom - top;
                int baseValue = calcBaseValue(top, left, bottom, right);
                autoIsClipped = false;
                switch (unit) {
                    case UNIT_PIXELS:
                        calculatedValue = baseValue + (int) value;
                        break;
                    case UNIT_DIPS:
                        calculatedValue = baseValue + Display.getInstance().convertToPixels(value);
                        break;
                    case UNIT_PERCENT: {
                        Inset oppositeInset = getOppositeInset();

                        int oppositeBaseValue = oppositeInset.calcBaseValue(top, left, bottom, right);
                        if (isVerticalInset()) {
                            float anchorV = LayeredLayoutConstraint.this.getPercentInsetAnchorVertical();
                            calculatedValue = (int) (baseValue + (h - oppositeBaseValue - baseValue) * value / 100f
                                    - (anchorV != 0 ? (getOuterPreferredH(cmp) * anchorV) : 0));
                        } else {
                            float anchorH = LayeredLayoutConstraint.this.getPercentInsetAnchorHorizontal();
                            calculatedValue = (int) (baseValue + (w - oppositeBaseValue - baseValue) * value / 100f
                                    - (anchorH != 0 ? (getOuterPreferredW(cmp) * anchorH) : 0));
                        }
                        break;
                    }
                    case UNIT_BASELINE: {
                        if (getReferenceComponent() == null) {
                            calculatedValue = baseValue;
                        } else {
                            Component ref = getReferenceComponent();
                            Style rs = ref.getStyle();
                            Style s = cmp.getStyle();
                            Font rf = rs.getFont();
                            Font sf = s.getFont();
                            int ra = rf == null || sf == null ? 0 : rf.getAscent();
                            int sa = rf == null || sf == null ? 0 : sf.getAscent();
                            calculatedValue = baseValue + (ref.getHeight() - cmp.getPreferredH()) / 2
                                    + (rs.getPaddingTop() - s.getPaddingTop())
                                    + (rs.getMarginTop() - s.getMarginTop())
                                    + (ra - sa);
                        }

                        break;
                    }
                    case UNIT_AUTO: {
                        Inset oppositeInset = getOppositeInset();
                        int oppositeBaseValue = oppositeInset.calcBaseValue(top, left, bottom, right);

                        if (oppositeInset.unit == UNIT_AUTO) {

                            if (isVerticalInset()) {
                                if (cmp.getPreferredH() <= 0) {
                                    calculatedValue = baseValue;
                                    autoIsClipped = true;
                                } else {
                                    calculatedValue = baseValue + (h - oppositeBaseValue - baseValue - getOuterPreferredH(cmp)) / 2;
                                }
                            } else {
                                if (cmp.getPreferredW() <= 0) {
                                    calculatedValue = baseValue;
                                    autoIsClipped = true;
                                } else {
                                    calculatedValue = baseValue + (w - oppositeBaseValue - baseValue - getOuterPreferredW(cmp)) / 2;
                                }
                            }
                            if (calculatedValue < 0) {
                                autoIsClipped = true;
                            }
                            calculatedValue = Math.max(0, calculatedValue);
                        } else {
                            if (isVerticalInset()) {
                                if (cmp.getPreferredH() <= 0) {
                                    calculatedValue = baseValue;
                                    autoIsClipped = true;
                                } else {
                                    calculatedValue = h - oppositeInset.calculate(cmp, top, left, bottom, right) - getOuterPreferredH(cmp);
                                }

                            } else {
                                if (cmp.getPreferredW() <= 0) {
                                    calculatedValue = baseValue;
                                    autoIsClipped = true;
                                } else {
                                    calculatedValue = w - oppositeInset.calculate(cmp, top, left, bottom, right) - getOuterPreferredW(cmp);
                                }
                            }
                            if (calculatedValue < 0) {
                                autoIsClipped = true;
                            }
                            calculatedValue = Math.max(0, calculatedValue);
                        }
                        break;
                    }
                    default:
                        throw new RuntimeException("Invalid unit " + unit);
                }
                delta = 0;
                return calculatedValue;
            }

            /// Recursively gets all of the reference components of this inset.
            ///
            /// #### Parameters
            ///
            /// - `deps`: An "out" parameter.  The set that will hold the dependencies.
            ///
            /// #### Returns
            ///
            /// The set of all reference components (crawled recursively of this inset.
            public Set<Component> getDependencies(Set<Component> deps) {
                if (referenceComponent != null) {
                    if (deps.contains(referenceComponent)) {
                        return deps;
                    }
                    deps.add(referenceComponent);
                    getOrCreateConstraint(referenceComponent).getDependencies(deps);
                }
                return deps;
            }

            /// Recursively gets all of the reference components of this inset.
            ///
            /// #### Returns
            ///
            /// The set of all reference components (crawled recursively of this inset.
            public Set<Component> getDependencies() {
                return getDependencies(new HashSet<Component>());
            }

            /// Gets the opposite inset of this inset within its parent constraint.  E.g. if this is the
            /// left inset, it will get the associated right inset.
            ///
            /// #### Returns
            ///
            /// The opposite inset.
            public Inset getOppositeInset() {
                LayeredLayoutConstraint cnst = LayeredLayoutConstraint.this;
                if (cnst != null) {
                    int oppSide = 0;
                    switch (side) {
                        case Component.TOP:
                            oppSide = Component.BOTTOM;
                            break;
                        case Component.BOTTOM:
                            oppSide = Component.TOP;
                            break;
                        case Component.LEFT:
                            oppSide = Component.RIGHT;
                            break;
                        default:
                            oppSide = Component.LEFT;
                            break;

                    }
                    return cnst.insets[oppSide];
                }
                return null;
            }

            /// Sets the value of this inset.  E.g. "2mm", "1px", "25%", or "auto".
            ///
            /// #### Parameters
            ///
            /// - `val`
            private void setValue(String val) {
                int pos = val.indexOf("mm");
                if (pos != -1) {
                    this.setDips(Float.parseFloat(val.substring(0, pos)));
                    return;
                }

                pos = val.indexOf("px");
                if (pos != -1) {
                    this.setPixels(Integer.parseInt(val.substring(0, pos)));
                    return;
                }

                pos = val.indexOf("%");
                if (pos != -1) {
                    this.setPercent(Float.parseFloat(val.substring(0, pos)));
                    return;
                }

                if ("auto".equals(val)) {
                    this.setAuto();
                    return;
                }

                if ("baseline".equals(val)) {
                    this.unit(UNIT_BASELINE);
                    return;
                }

                this.setPixels(Integer.parseInt(val));
            }

            /// Copies this inset into another inset.
            ///
            /// #### Parameters
            ///
            /// - `dest`: The inset to copy to.
            ///
            /// #### Returns
            ///
            /// The copied inset.
            public Inset copyTo(Inset dest) {
                dest.autoIsClipped = autoIsClipped;
                dest.calculatedValue = calculatedValue;
                dest.delta = delta;
                dest.calculatedBaseValue = calculatedBaseValue;
                dest.preferredValue = preferredValue;

                dest.value = value;
                dest.unit = unit;
                // We won't copy the side since that allows us to f things up
                //dest.side = side;
                dest.referenceComponent = referenceComponent;
                dest.referencePosition = referencePosition;
                return dest;
            }

            /// Copies this inset to the corresponding inset of the provided constraint.
            ///
            /// #### Parameters
            ///
            /// - `dest`: The constraint to copy the inset into.
            ///
            /// #### Returns
            ///
            /// The corresponding inset in dest that we copied the inset into.
            public Inset copyTo(LayeredLayoutConstraint dest) {
                copyTo(dest.insets[side]);
                return dest.insets[side];
            }

            /// Copies this inset into the corresponding inset of the provided component.
            ///
            /// #### Parameters
            ///
            /// - `cmp`: The component that we are copying the inset into.
            ///
            /// #### Returns
            ///
            /// The copied inset.
            public Inset copyTo(Component cmp) {
                copyTo(getOrCreateConstraint(cmp));
                return this;
            }

            /// Creates a copy of this inset.
            public Inset copy() {
                return copyTo(new Inset(side));
            }

            /// Gets the unit of this inset.
            ///
            /// #### Returns
            ///
            /// One of `#UNIT_AUTO`, `#UNIT_DIPS`, `#UNIT_PIXELS`, or `#UNIT_PERCENT`.
            public byte getUnit() {
                return unit;
            }

            /// Checks if this is a fixed inset. An inset is considered "fixed" if its unit is not `#UNIT_AUTO`
            ///
            /// #### Returns
            ///
            /// True if the inset is fixed.
            public boolean isFixed() {
                return unit != UNIT_AUTO;
            }

            /// Gets the current value of this inset in millimetres.  If the inset uses a different unit, then
            /// this will calculate the corresponding value.
            public float getCurrentValueMM() {
                if (unit == UNIT_DIPS) {
                    return value;
                } else if (unit == UNIT_PIXELS) {
                    float pixelsPerDip = Display.getInstance().convertToPixels(1000) / 1000f;
                    return value / pixelsPerDip;
                } else {
                    // In both auto and percent cases, we'll use the existing calculated value as our base
                    float pixelsPerDip = Display.getInstance().convertToPixels(1000) / 1000f;
                    int calc = calculatedValue;
                    //System.out.println("Calculated value of side "+side+" = "+calc);
                    //new RuntimeException("Foobar").printStackTrace();
                    if (referenceComponent != null) {
                        calc -= calculatedBaseValue;
                    }
                    float out = calc / pixelsPerDip;
                    //System.out.println("calc="+out+"mm");
                    return out + (delta / pixelsPerDip);

                }
            }

            /// Gets the current value of this inset in pixels.  If the inset uses a different unit
            /// then this will calculate the corresponding value.
            ///
            /// #### Returns
            ///
            /// The value of this inset in pixels.
            public int getCurrentValuePx() {
                if (unit == UNIT_DIPS) {
                    return Display.getInstance().convertToPixels(value);
                } else if (unit == UNIT_PIXELS) {
                    return (int) value;
                } else {
                    // In both auto and percent cases, we'll use the existing calculated value as our source.
                    int calc = calculatedValue;
                    if (referenceComponent != null) {

                        calc -= calculatedBaseValue;
                    }
                    return calc + delta;
                }
            }

            /// True if this is a vertical inset (top or bottom).
            public boolean isVertical() {
                return side == Component.TOP || side == Component.BOTTOM;
            }

            /// True if this is a horizontal inset (left or right).
            public boolean isHorizontal() {
                return side == Component.LEFT || side == Component.RIGHT;
            }

            /// Changes the units of this inset, and updates the value to remain
            /// the same as the current value.
            ///
            /// #### Parameters
            ///
            /// - `unit`: The unit.  One of `#UNIT_AUTO`, `#UNIT_DIPS`, `#UNIT_PIXELS`, or `#UNIT_PERCENT`.
            ///
            /// #### Returns
            ///
            /// Self for chaining.
            ///
            /// #### Deprecated
            ///
            /// Use `com.codename1.ui.Container)`
            public Inset changeUnits(byte unit) {
                return changeUnits(unit, cmp);

            }

            /// Changes the units of this inset, and updates the value to remain
            /// the same as the current value.
            ///
            /// #### Parameters
            ///
            /// - `unit`: The unit.  One of `#UNIT_AUTO`, `#UNIT_DIPS`, `#UNIT_PIXELS`, or `#UNIT_PERCENT`.
            ///
            /// - `cmp`: The component for which the inset is applying.
            ///
            /// #### Returns
            ///
            /// Self for chaining.
            ///
            /// #### Deprecated
            ///
            /// Use `com.codename1.ui.Container)`
            public Inset changeUnits(byte unit, Component cmp) {
                return changeUnitsTo(unit, cmp == null ? null : cmp.getParent());
            }

            /// Changes the units of this inset, and updates the value to remain
            /// the same as the current value.
            ///
            /// #### Parameters
            ///
            /// - `unit`: The unit.  One of `#UNIT_AUTO`, `#UNIT_DIPS`, `#UNIT_PIXELS`, or `#UNIT_PERCENT`.
            ///
            /// - `parent`: The container in which the layout applies.
            ///
            /// #### Returns
            ///
            /// Self for chaining.
            public Inset changeUnitsTo(byte unit, Container parent) {
                if (unit != this.unit) {
                    if (unit == UNIT_PIXELS) {
                        setPixels(getCurrentValuePx());
                    } else if (unit == UNIT_DIPS) {
                        setDips(getCurrentValueMM());
                    } else if (unit == UNIT_PERCENT) {
                        try {
                            if (parent != null) {
                                Rectangle refBox = constraint().getReferenceBox(parent);

                                setPercent(getCurrentValuePx() * 100f / (isVertical() ? refBox.getHeight() : refBox.getWidth()));
                            } else {
                                throw new IllegalArgumentException("Cannot change unit to percent without specifying the target component.");
                            }
                        } catch (IllegalArgumentException ex) {
                            Log.p("Unable to calculate percentage because height or width is zero.  Setting to 100%");
                            setPercent(100f);
                        }
                    } else if (unit == UNIT_BASELINE) {
                        if (side != Component.TOP) {
                            throw new IllegalArgumentException("Baseline unit only allowed on top inset");
                        }
                        getOppositeInset().changeUnitsTo(UNIT_AUTO, parent);
                        unit(unit);
                    } else {
                        unit(unit);
                    }
                }
                return this;
            }


            public Inset changeUnitsTo(byte unit, Rectangle refBox) {
                if (unit != this.unit) {
                    if (unit == UNIT_PIXELS) {
                        setPixels(getCurrentValuePx());
                    } else if (unit == UNIT_DIPS) {
                        setDips(getCurrentValueMM());
                    } else if (unit == UNIT_PERCENT) {
                        try {
                            setPercent(getCurrentValuePx() * 100f / (isVertical() ? refBox.getHeight() : refBox.getWidth()));
                        } catch (IllegalArgumentException ex) {
                            Log.p(ex.getMessage());
                            setPercent(100f);
                        }
                    } else if (unit == UNIT_BASELINE) {
                        if (side != Component.TOP) {
                            throw new IllegalArgumentException("Baseline unit only allowed on top inset");
                        }
                        getOppositeInset().changeUnitsTo(UNIT_AUTO, refBox);
                        unit(unit);
                    } else {
                        unit(unit);
                    }
                }
                return this;
            }

            /// Changes the reference component, while updating the value to remain in the same
            /// absolute position.
            ///
            /// #### Parameters
            ///
            /// - `parent`: The parent container.
            ///
            /// - `newRef`: The new reference component.
            ///
            /// - `pos`: The reference position.
            ///
            /// #### Returns
            ///
            /// Self for chaining.
            public Inset changeReference(Container parent, Component newRef, float pos) {
                if (newRef != null) {
                    LayeredLayoutConstraint refCnst = getOrCreateConstraint(newRef);
                    if (cmp != null && refCnst.dependsOn(cmp)) {
                        throw new IllegalArgumentException("Attempted to set a reference that would produce a circular dependency in LayeredLayout");
                    }
                }
                //if (isFlexible()) {
                //    // This could potentially affect the opposite inset if it is a percentage
                //    referenceComponent(newRef).referencePosition(pos);
                //} else {
                if (newRef != referenceComponent || com.codename1.util.MathUtil.compare(pos, referencePosition) != 0) { //NOPMD CompareObjectsWithEquals
                    // This may potentially affect both this inset
                    // and the opposite inset if it is either flexible or
                    // percent.

                    if (unit == UNIT_BASELINE) {
                        changeUnitsTo(UNIT_DIPS, parent);
                    }

                    byte restoreUnit = -1;
                    if (unit == UNIT_PERCENT || isFlexible()) {
                        restoreUnit = unit;
                        changeUnitsTo(UNIT_DIPS, parent);
                    }
                    byte oppRestoreUnit = -1;
                    if (getOppositeInset().unit == UNIT_PERCENT || isFlexible()) {
                        oppRestoreUnit = getOppositeInset().unit;
                        getOppositeInset().changeUnitsTo(UNIT_DIPS, parent);
                    }
                    LayeredLayoutConstraint cpy = constraint().copy();
                    cpy.insets[side].referenceComponent(newRef).referencePosition(pos);

                    //Container parent = context.getParent();


                    Style s = parent.getStyle();
                    int top = s.getPaddingTop();
                    int bottom = parent.getLayoutHeight() - parent.getBottomGap() - s.getPaddingBottom();
                    int left = s.getPaddingLeft(parent.isRTL());
                    int right = parent.getLayoutWidth() - parent.getSideGap() - s.getPaddingRight(parent.isRTL());
                    int newBase = cpy.insets[side].calcBaseValue(top, left, bottom, right);
                    int oldBase = calcBaseValue(top, left, bottom, right);


                    referenceComponent(newRef).referencePosition(pos);
                    calculatedBaseValue += (newBase - oldBase);
                    calculatedValue += (newBase - oldBase);
                    if (getOppositeInset().isFlexible()) {
                        getOppositeInset().delta -= (newBase - oldBase);
                    }

                    translatePixels(oldBase - newBase, true, parent);
                    if (restoreUnit >= 0) {
                        changeUnitsTo(restoreUnit, parent);
                    }
                    if (oppRestoreUnit >= 0) {
                        getOppositeInset().changeUnitsTo(oppRestoreUnit, parent);
                    }
                }
                //}

                return this;


            }

            /// Checks if this is a flexible inset.  An inset is considered flexible if its unit is `#UNIT_AUTO`.
            ///
            /// #### Returns
            ///
            /// True if this is a flexible inset.
            ///
            /// #### See also
            ///
            /// - #isFixed()
            public boolean isFlexible() {
                return unit == UNIT_AUTO;
            }

            /// Returns the total inset of this inset when applied to the given component.
            /// This will calculate and sum all of the insets of reference components to
            /// get the total inset in pixels from the parent component.
            ///
            /// #### Parameters
            ///
            /// - `cmp`: The component context.
            ///
            /// #### Returns
            ///
            /// The total inset in pixels from the parent.
            public int getAbsolutePixels(Component cmp) {
                Container parent = cmp.getParent();
                Style s = parent.getStyle();
                int top = s.getPaddingTop();
                int bottom = parent.getLayoutHeight() - parent.getBottomGap() - s.getPaddingBottom();
                int left = s.getPaddingLeft(parent.isRTL());
                int right = parent.getLayoutWidth() - parent.getSideGap() - s.getPaddingRight(parent.isRTL());
                int baseValue = calcBaseValue(top, left, bottom, right);
                //Rectangle baseRect = getReferenceBox(cmp.getParent(), cmp);

                switch (unit) {
                    case UNIT_PIXELS:
                        return baseValue + (int) value;
                    case UNIT_DIPS:
                        return baseValue + Display.getInstance().convertToPixels(value);
                    case UNIT_PERCENT: {

                        Rectangle baseRect = getReferenceBox(parent, cmp);
                        //System.out.println("Baserect is "+baseRect+" baseValue="+baseValue+" for percent "+value);
                        return (int) (baseValue + (isHorizontalInset() ? baseRect.getWidth() : baseRect.getHeight()) * value / 100f);
                    }
                    case UNIT_BASELINE: {
                        Component ref = getReferenceComponent();
                        if (ref == null) {
                            return baseValue;
                        } else {
                            Style rs = ref.getStyle();
                            Style cs = cmp.getStyle();
                            Font rf = rs.getFont();
                            Font cf = cs.getFont();
                            int ra = rf == null || cf == null ? 0 : rf.getAscent();
                            int ca = rf == null || cf == null ? 0 : cf.getAscent();
                            return baseValue + (ref.getHeight() - cmp.getPreferredH()) / 2
                                    + (rs.getPaddingTop() - cs.getPaddingTop())
                                    + (rs.getMarginTop() - cs.getMarginTop())
                                    + (ra - ca);
                        }
                    }
                    case UNIT_AUTO: {
                        Inset oppositeInset = getOppositeInset();
                        if (oppositeInset.unit == UNIT_AUTO) {
                            Rectangle baseRect = getReferenceBox(parent, cmp);
                            // they're both auto,
                            //int oppositeBase = oppositeInset.calcBaseValue(top, left, bottom, right);
                            if (isVerticalInset()) {
                                return (baseRect.getHeight() - getOuterPreferredH(cmp)) / 2;
                            } else {
                                return (baseRect.getWidth() - getOuterPreferredW(cmp)) / 2;
                            }
                        } else {
                            if (isVerticalInset()) {
                                return bottom - top - oppositeInset.getAbsolutePixels(cmp) - baseValue - getOuterPreferredH(cmp);
                            } else {
                                //System.out.println("Checking opposite inset for value");
                                return right - left - oppositeInset.getAbsolutePixels(cmp) - baseValue - getOuterPreferredW(cmp);
                            }
                        }
                    }
                    default:
                        throw new RuntimeException("Illegal state in inset.  Unknown unit " + unit);

                }

            }

            /// Translates the inset by delta pixels.
            ///
            /// #### Parameters
            ///
            /// - `delta`: Pixels to translate this inset by.
            ///
            /// - `preferMM`: If this is a flexible inset, then translating it will require changing it to fixed.  true to use millimetres.  false to use pixels.
            ///
            /// - `parent`: The parent container used for calculating equivalent percent if this is a percent inset.
            ///
            /// #### Returns
            ///
            /// Self for chaining.
            ///
            /// #### See also
            ///
            /// - #translateMM(float, boolean, com.codename1.ui.Container)
            public Inset translatePixels(int delta, boolean preferMM, Container parent) {

                switch (unit) {
                    case UNIT_PIXELS:
                        value += delta;
                        break;
                    case UNIT_DIPS: {
                        float pixelsPerDip = Display.getInstance().convertToPixels(1000) / 1000f;
                        //System.out.println("Old dips for side "+side+" = "+value);
                        value += (delta / pixelsPerDip);
                        //System.out.println("New dips for side "+side+" = "+value);
                        break;
                    }
                    case UNIT_PERCENT: {
                        //Container parent = cmp.getParent();
                        //Style parentStyle = parent.getStyle();
                        Style s = parent.getStyle();
                        int top = s.getPaddingTop();
                        int bottom = parent.getLayoutHeight() - parent.getBottomGap() - s.getPaddingBottom();
                        int left = s.getPaddingLeft(parent.isRTL());
                        int right = parent.getLayoutWidth() - parent.getSideGap() - s.getPaddingRight(parent.isRTL());
                        int baseValue = calculatedBaseValue;
                        int oppositeBaseValue = getOppositeInset().calculatedBaseValue;
                        if (isVerticalInset()) {
                            float relH = bottom - top - baseValue - oppositeBaseValue;
                            if (Math.abs(relH) < 1f) {
                                return this;
                            }
                            float percentDelta = delta / relH * 100f;
                            if (com.codename1.util.MathUtil.compare(percentDelta, Float.NEGATIVE_INFINITY) == 0 || com.codename1.util.MathUtil.compare(percentDelta, Float.POSITIVE_INFINITY) == 0) {
                                percentDelta = 0f;
                            }
                            value += percentDelta;

                        } else {
                            float relH = right - left - baseValue - oppositeBaseValue;
                            //System.out.println("relH="+relH+" delta="+delta);
                            if (Math.abs(relH) < 1f) {
                                return this;
                            }
                            float percentDelta = delta / relH * 100f;
                            //System.out.println("percentDelta="+percentDelta);
                            if (com.codename1.util.MathUtil.compare(percentDelta, Float.NEGATIVE_INFINITY) == 0 || com.codename1.util.MathUtil.compare(percentDelta, Float.POSITIVE_INFINITY) == 0) {
                                percentDelta = 0f;
                            }
                            value += percentDelta;
                            //System.out.println("Value="+value);
                        }
                        break;

                    }
                    case UNIT_BASELINE: {
                        changeUnitsTo(UNIT_DIPS, parent);
                        return translatePixels(delta, preferMM, parent);
                    }
                    case UNIT_AUTO: {
                        // If this is auto then we'll need to make it fixed... but we'll start
                        // by making it fixed
                        unit = preferMM ? UNIT_DIPS : UNIT_PIXELS;
                        if (unit == UNIT_PIXELS) {
                            value = calculatedValue + delta - calculatedBaseValue;
                        } else {
                            float pixelsPerDip = Display.getInstance().convertToPixels(1000) / 1000f;
                            value = (calculatedValue + delta - calculatedBaseValue) / pixelsPerDip;
                        }
                        break;
                    }
                    default:
                        break;

                }
                //calculatedValue += delta;

                this.delta += delta;
                if (getOppositeInset().isFlexible()) {
                    getOppositeInset().delta -= delta;
                }
                return this;
            }

            /// Translates the inset by delta millimetres.
            ///
            /// #### Parameters
            ///
            /// - `delta`: Pixels to translate this inset by.
            ///
            /// - `preferMM`: If this is a flexible inset, then translating it will require changing it to fixed.  true to use millimetres.  false to use pixels.
            ///
            /// - `parent`: The parent container used for calculating equivalent percent if this is a percent inset.
            ///
            /// #### Returns
            ///
            /// Self for chaining.
            public Inset translateMM(float delta, boolean preferMM, Container parent) {
                return translatePixels(Display.getInstance().convertToPixels(delta), preferMM, parent);

            }

        }

    }
}
