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
import java.util.HashSet;
import java.util.Set;

/**
 * <p>
 * The {@code LayeredLayout} places the components in order one on top of the
 * other and sizes them all to the size of the largest component. This is useful
 * when trying to create an overlay on top of an existing component. E.g. an "x"
 * button to allow removing the component as shown here</p>
 *
 * <img src="https://www.codenameone.com/img/developer-guide/layered-layout.png" alt="The X on this button was placed there using the layered layout code below" />
 *
 * <p>
 * The code to generate this UI is slightly complex and contains very little
 * relevant pieces. The only truly relevant piece the last line of code:</p>
 *
 * <script src="https://gist.github.com/codenameone/d0491ce08ce6b889bbd5.js"></script>*
 *
 *
 * <p>
 * We are doing three distinct things here:</p>
 * <ul>
 * .
 * <li> We are adding a layered layout to the form.</li>
 * <li> We are creating a layered layout and placing two components within. This
 * would be the equivalent of just creating a {@code LayeredLaout}
 * {@link com.codename1.ui.Container} and invoking `add` twice.</li>
 * .* <li> We use
 * https://www.codenameone.com/javadoc/com/codename1/ui/layouts/FlowLayout.html[FlowLayout]
 * to position the `X` close button in the right position.</li>
 * </ul>
 *
 * <p>
 * A common use case for {@code LayeredLayout} is the iOS carousel effect which
 * we can achieve by combing the {@code LayeredLayout} with
 * {@link com.codename1.ui.Tabs}.
 * </p>
 * <script src="https://gist.github.com/codenameone/e981c3f91f98f1515987.js"></script>
 * <img src="https://www.codenameone.com/img/developer-guide/components-tabs-swipe1.png" alt="Tabs carousel page 1" />
 *
 * <p>
 * Notice that the layered layout sizes all components to the exact same size
 * one on top of the other. It usually requires that we use another container
 * within; in order to position the components correctly.<br />
 *
 * Forms have a built in layered layout that you can access via
 * `getLayeredPane()`, this allows you to overlay elements on top of the content
 * pane.<br />
 *
 * The layered pane is used internally by components such as {@link com.codename1.components.InteractionDialog},
 * {@link com.codename1.u./AutoCompleteTextField} etc.
 * </p>
 * <p>
 * Warning: Placing native widgets within a layered layout is problematic due to
 * the behavior of peer components. Sample of peer components include the
 * {@link com.codename1.ui.BrowserComponent}, video playback etc.
 * </p>
 * 
 * <h3>Insets</h3>
 * 
 * <p>This layout optionally supports insets for laying out its children. Use of insets can allow you to 
 * achieve precise placement of components while adjusting properly to screen resizing.</p>
 * 
 * <p>Insets may be either fixed or flexible.  Fixed insets may be specified in pixels ({@link #UNIT_PIXELS}), 
 * millimetres ({@link #UNIT_DIPS}), or percentage ({@link #UNIT_PERCENT}).  Insets may also be specified as just "auto" ({@link #UNIT_AUTO}),
 * in which case it is considered to be flexible (it will adapt to the component size and other insets).</p>
 * 
 * <p>Insets may also be anchored to a "reference component" so that it will always be measured from that reference component.</p>
 * 
 * <h4>Insets Example</h4>
 * 
 * <p>Adding a button to the top right of the parent:</p>
 * <pre>
 * {@code
 * Container cnt = new Container(new LayeredLayout());
 * LayeredLayout ll = (LayeredLayout)cnt.getLayout();
 * Button btn = new Button("My Button");
 * cnt.add(btn);
 * ll.setInsets(btn, "0 0 auto auto");
 *     // NOTE: Insets are expressed in same order as "margin" in CSS.  Clockwise starting on top.
 * }
 * </pre>
 * 
 * <p>Changing top inset to 2mm, and right inset to 1mm:</p>
 * <pre>{@code ll.setInsets(btn, "2mm 1mm auto auto");}</pre>
 * 
 * <p>Using percentage insets:</p>
 * <pre>{@code ll.setInsets(btn, "25% 25% auto auto");}</pre>
 * 
 * <p><strong>NOTE:</strong> When using percent units, the percentage is always in terms of the "reference box" of the component.
 * The "reference box" is the bounding rectangle from which the insets are measured.  If none of the insets 
 * is anchored to a reference component, then the bounding box will simply be the inner bounds of the parent container (i.e.
 * the bounds of the inside padding in the container.</p>
 * 
 * <p><strong>Using "auto" insets</strong></p>
 * <p>An "auto" inset is an inset that is flexible.  If all 4 insets are set to auto, then the component will tend to the
 * centre of the parent component, and its size will be the component's preferred size (though the size will be bounded by the size of the component's reference box).
 * If one inset is fixed, and the opposite inset is "auto", then the fixed inset and the component's preferred size will dictate the'
 * calculated size of the inset.
 * </p>
 * 
 * <h4>Reference Components</h4>
 * 
 * <p>Insets may also have reference componnents.  E.g. If you want a button to be anchored to the right side of a search field, you could 
 * make the button's left inset "reference" the text field.  This would be achieved as follows:
 * 
 * <pre>
 * {@code
 * Container cnt = new Container(new LayeredLayout());
 * LayeredLayout ll = (LayeredLayout)cnt.getLayout();
 * TextField searchField = new TextField();
 * Button btn = new Button("Search");
 * cnt.add(searchField).add(btn);
 * ll
 *   .setInsets(searchField, "1mm auto auto auto")
 *   .setInsets(btn, "0 auto auto 0")
 *   .setReferenceComponentLeft(btn, searchField, 1f)
 *   .setReferenceComponentTop(btn, searchField, 0);
 * }
 * </pre>
 * 
 * <p>In the above example we set the search field to be anchored to the top of its parent (1mm inset),
 * but for all other insets to be auto.  This will result it being centered horizontally in its parent.  We then
 * anchor the button to the left and top of the search field so that the top and left insets of button will always be
 * calculated relative to the position of searchField.  In particular since the button has top and left insets of 0,
 * the button will always be placed just to the right of the search field, with its top edge aligned with the top edge
 * of search field.</p>
 * 
 * <p><strong>Reference Positions</strong></p>
 * <p>The second parameter of {@code setReferenceComponentLeft(btn, searchField, 1f)} is the reference position and it dictates
 * which edge of the reference component ({@literal searchField}) the inset should be anchored to.  A value of {@literal 1} indicates that
 * it should anchor to the opposite side of the inset  (e.g. in this case it is the "left" inset we are setting, so the {@literal 1}
 * value dictates that it is anchored to the "right" side of the text field.  A value of {@literal 0} indicates that it should anchor
 * to the same side as the inset.  This is why we used {@literal 0} in the subsequent call to {@code .setReferenceComponentTop(btn, searchField, 0);},
 * because we want to anchor the "top" inset of {@literal button} to the "top" edge of {@literal searchField}.</p>
 * 
 * 
 * 
 *
 * @see com.codename1.ui.Form#getLayeredPane()
 * @see com.codename1.ui.Form#getLayeredPane(java.lang.Class, boolean)
 * @see com.codename1.ui.Form#setGlassPane(com.codename1.ui.Painter)
 * @author Shai Almog
 */
public class LayeredLayout extends Layout {

    /**
     * Unit used for insets.  Millimetres.
     * 
     * @see Inset#unit(byte) 
     * @see Inset#changeUnits(byte) 
     */
    public static final byte UNIT_DIPS = Style.UNIT_TYPE_DIPS;
    
    /**
     * Unit used for insets.  Pixels.
     * 
     * @see Inset#unit(byte) 
     * @see Inset#changeUnits(byte) 
     */
    public static final byte UNIT_PIXELS = Style.UNIT_TYPE_PIXELS;
    
    /**
     * Unit used for insets.  Percent.
     * 
     * @see Inset#unit(byte) 
     * @see Inset#changeUnits(byte) 
     */
    public static final byte UNIT_PERCENT = Style.UNIT_TYPE_SCREEN_PERCENTAGE;
    
    /**
     * Unit used for insets.  Auto.  Auto unit type for an inset indicates the 
     * the inset will be automatically determined at layout time.
     * 
     * @see Inset#unit(byte) 
     * @see Inset#changeUnits(byte) 
     */
    public static final byte UNIT_AUTO = 100;
    
    /**
     * Unit used for insets.  Baseline.  Baseline unit type for an inset indicates
     * the inset will be aligned with the baseline of the reference component.  This only
     * makes sense for the top inset.  The height will automatically become the preferred
     * height and the bottom inset will become "auto" if the top inset uses the baseline unit.
     */
    public static final byte UNIT_BASELINE = 101;

    /**
     * Temp collection to keep track of which components in the container
     * have been laid out.
     */
    private HashSet<Component> tmpLaidOut = new HashSet<Component>();
    
    /**
     * The preferred height in MM of this layout which serves as a sort of minimum
     * height even when the components in the layout don't demand space.
     * 
     * <p>The actual preferred height will be the max of this value and the 
     * calculated preferred height based on the container's children.<p>
     */
    private float preferredHeightMM;
    
    /**
     * The preferred width (in MM) of this layout which serves as a sort of minimum
     * width even when the components in the layout don't demand space.
     * 
     * <p>The actual preferred width will be the max of this value and the 
     * calculated preferred width based on the container's children.<p>
     */
    private float preferredWidthMM;
    
    
    /**
     * Sets the preferred size of this layout in MM.  This serves as a minimum
     * size that will be returned by calcPreferredSize().
     * @param width The preferred width in MM.
     * @param height The preferred height in MM.
     */
    public void setPreferredSizeMM(float width, float height) {
        this.preferredHeightMM = height;
        this.preferredWidthMM = width;
    }
    
    /**
     * The preferred height in MM of this layout which serves as a sort of minimum
     * height even when the components in the layout don't demand space.
     * 
     * <p>The actual preferred height will be the max of this value and the 
     * calculated preferred height based on the container's children.<p>
     */
    public float getPreferredHeightMM() {
        return preferredHeightMM;
    }
    
    /**
     * Sets the preferred height of this layout in MM.
     * @param mm 
     */
    public void setPreferredHeightMM(float mm) {
        preferredHeightMM = mm;
    }
    
    /**
     * Sets the preferred width of this layout in MM.
     * @param mm 
     */
    public void setPreferredWidthMM(float mm) {
        preferredWidthMM = mm;
    }
    /**
     * The preferred width (in MM) of this layout which serves as a sort of minimum
     * width even when the components in the layout don't demand space.
     * 
     * <p>The actual preferred width will be the max of this value and the 
     * calculated preferred width based on the container's children.<p>
     */
    public float getPreferredWidthMM() {
        return preferredWidthMM;
    }

    @Override
    public void addLayoutComponent(Object value, Component comp, Container c) {
        if (value instanceof LayeredLayoutConstraint.Inset) {
            value = ((LayeredLayoutConstraint.Inset)value).constraint();
        }
        if (value instanceof LayeredLayoutConstraint) {
            
            installConstraint((LayeredLayoutConstraint)value, comp);
        }
    }
    
    /**
     * Wraps {@link #getComponentConstraint(com.codename1.ui.Component) } and casts it
     * directly to {@link LayeredLayoutConstraint}.
     * @param cmp The component whose constraint we want to retrieve.
     * @return The layered layout constraint for this component.
     */
    public LayeredLayoutConstraint getLayeredLayoutConstraint(Component cmp) {
        return (LayeredLayoutConstraint)getComponentConstraint(cmp);
    }
    
    /**
     * Installs the given constraint in the provided component.
     * @param constraint
     * @param cmp
     * @return 
     */
    private LayeredLayoutConstraint installConstraint(LayeredLayoutConstraint constraint, Component cmp) {
        
        if (constraint.outer() != this || (constraint.cmp != null && constraint.cmp != cmp)) {
            LayeredLayoutConstraint tmp = createConstraint();
            constraint.copyTo(tmp);
            constraint = tmp;
        }
        constraint.cmp = cmp;
        cmp.putClientProperty("$$LayeredLayoutConstraint", constraint);
        return constraint;
    }

    /**
     * Makes a copy of the given constraint.
     * @param constraint The constraint to copy.
     * @return The copied constraint.
     */
    @Override
    public Object cloneConstraint(Object constraint) {
        if (constraint instanceof LayeredLayoutConstraint) {
            return ((LayeredLayoutConstraint)constraint).copy();
        }
        return super.cloneConstraint(constraint);
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

    /**
     * Gets the LayeredLayoutConstraint associated with the given component.
     * 
     * May return null if there is no constraint.
     * @param comp
     * @return 
     */
    @Override
    public Object getComponentConstraint(Component comp) {
        return comp.getClientProperty("$$LayeredLayoutConstraint");
    }

    /**
     * Creates a default layered layout constraint.  Default constraint
     * has zero insets on all four sides.
     * @param constraint
     * @return 
     */
    public LayeredLayoutConstraint createConstraint(String constraint) {
        return new LayeredLayoutConstraint().setInsets(constraint);
    }
    
    /**
     * If the given component already has a LayeredLayoutConstraint, then this 
     * will return it. Otherwise it will create a constraint, install it in {@literal cmp}
     * and return the constraint for inspection or manipulation.
     * @param cmp The component whose constraint we wish to retrieve.
     * @return The constraint for a given component.
     */
    public LayeredLayoutConstraint getOrCreateConstraint(Component cmp) {
        LayeredLayoutConstraint constraint = (LayeredLayoutConstraint)getComponentConstraint(cmp);
        if (constraint == null) {
            //System.out.println("Constraint is null... creating a new one");
            constraint = createConstraint();
            constraint = installConstraint(constraint, cmp);
        }
        return constraint;
    }
    
    /**
     * Gets an {@link Inset} associated with the provided component
     * @param cmp The component whose inset we wish to retrieve.
     * @param side The side of the inset.  One of {@link Component#TOP}, {@link Component#LEFT}, {@link Component#BOTTOM} 
     * or {@link Component#RIGHT}.
     * @return The {@link Inset} for the given side of the component.
     */
    public Inset getInset(Component cmp, int side) {
        return getOrCreateConstraint(cmp).insets[side];
    }
    
    /**
     * Returns the insets for the given component as a string.  This can return the 
     * insets in one of two formats depending on the value of the {@literal withLabels} 
     * parameter.
     * @param cmp The component whose insets we wish to retrieve.
     * @param withLabels If {@literal false}, then this returns a string of the format {@code "top right bottom left"}
     * e.g {@code "2mm 2mm 2mm 2mm"}.  If {@literal true}, then it will be formatted like CSS properties: {@code "top:2mm; right:2mm; bottom:2mm; left:2mm"}. 
     * @return The insets associated with {@literal cmp} as a string. Each inset will include the unit.   E.g.:
     * <ul><li>{@literal 2mm} = 2 millimetres/dips</li><li>{@literal 2px} = 2 pixels</li><li>{@literal 25%} = 25%</li><li>{@literal auto} = Flexible inset</li></ul>
     * 
     */
    public String getInsetsAsString(Component cmp, boolean withLabels) {
        return getOrCreateConstraint(cmp).getInsetsAsString(withLabels);
    }
    
    /**
     * Gets the top inset as a string. Return value will include the unit, so the following
     * are possible values:
     * <p>
     * <ul><li>{@literal 2mm} = 2 millimetres</li>
     * <li>{@literal 2px} = 2 pixels</li>
     * <li>{@literal 25%} = 25%</li>
     * <li>{@literal auto} = Flexible Inset</li>
     * </ul>
     * </p>
     * @param cmp The component whose inset we wish to retrieve.
     * @return The inset formatted as a string with the unit abbreviation ("mm", "px", or "%") suffixed.
     */
    public String getTopInsetAsString(Component cmp) {
        return getOrCreateConstraint(cmp).top().getValueAsString();
    }
    
    /**
     * Gets the bottom inset as a string. Return value will include the unit, so the following
     * are possible values:
     * <p>
     * <ul><li>{@literal 2mm} = 2 millimetres</li>
     * <li>{@literal 2px} = 2 pixels</li>
     * <li>{@literal 25%} = 25%</li>
     * <li>{@literal auto} = Flexible Inset</li>
     * </ul>
     * </p>
     * @param cmp The component whose inset we wish to retrieve.
     * @return The inset formatted as a string with the unit abbreviation ("mm", "px", or "%") suffixed.
     */
    public String getBottomInsetAsString(Component cmp) {
        return getOrCreateConstraint(cmp).bottom().getValueAsString();
    }
    
    /**
     * Gets the left inset as a string. Return value will include the unit, so the following
     * are possible values:
     * <p>
     * <ul><li>{@literal 2mm} = 2 millimetres</li>
     * <li>{@literal 2px} = 2 pixels</li>
     * <li>{@literal 25%} = 25%</li>
     * <li>{@literal auto} = Flexible Inset</li>
     * </ul>
     * </p>
     * @param cmp The component whose inset we wish to retrieve.
     * @return The inset formatted as a string with the unit abbreviation ("mm", "px", or "%") suffixed.
     */
    public String getLeftInsetAsString(Component cmp) {
        return getOrCreateConstraint(cmp).left().getValueAsString();
    }
    
    /**
     * Gets the right inset as a string. Return value will include the unit, so the following
     * are possible values:
     * <p>
     * <ul><li>{@literal 2mm} = 2 millimetres</li>
     * <li>{@literal 2px} = 2 pixels</li>
     * <li>{@literal 25%} = 25%</li>
     * <li>{@literal auto} = Flexible Inset</li>
     * </ul>
     * </p>
     * @param cmp The component whose inset we wish to retrieve.
     * @return The inset formatted as a string with the unit abbreviation ("mm", "px", or "%") suffixed.
     */
    public String getRightInsetAsString(Component cmp) {
        return getOrCreateConstraint(cmp).right().getValueAsString();
    }
    
    /**
     * Sets the insets for the component {@literal cmp} to the values specified in {@literal insets}.
     * @param cmp The component whose insets we wish to set.
     * @param insets The insets expressed as a string.  See {@link LayeredLayoutConstraint#setInsets(java.lang.String) } for 
     * details on the format of this parameter.
     * @return Self for chaining.
     * @see LayeredLayoutConstraint#setInsets(java.lang.String) For details on the {@literal insets} parameter
     * format.
     */
    public LayeredLayout setInsets(Component cmp, String insets) {
        getOrCreateConstraint(cmp).setInsets(insets);
        return this;
    }
    
    /**
     * Sets the top inset for this component to the prescribed value.
     * @param cmp The component whose inset we wish to set.
     * @param inset The inset value, including unit.  Units are Percent (%), Millimetres (mm), Pixels (px), and "auto".  E.g. the 
     * following insets values would all be acceptable:
     * <p>
     * <ul>
     *   <li>{@code "2mm"} = 2 millimetres</li>
     *   <li>{@code "2px"} = 2 pixels</li>
     *   <li>{@code "25%"} = 25 percent.</li>
     *   <li>{@code "auto"} = Flexible inset</li>
     * </ul>
     * </p>
     * @return Self for chaining.
     */
    public LayeredLayout setInsetTop(Component cmp, String inset) {
        getOrCreateConstraint(cmp).top().setValue(inset);
        return this;
    }
    
    /**
     * Sets the top inset for this component to the prescribed value.
     * @param cmp The component whose inset we wish to set.
     * @param inset The inset value, including unit.  Units are Percent (%), Millimetres (mm), Pixels (px), and "auto".  E.g. the 
     * following insets values would all be acceptable:
     * <p>
     * <ul>
     *   <li>{@code "2mm"} = 2 millimetres</li>
     *   <li>{@code "2px"} = 2 pixels</li>
     *   <li>{@code "25%"} = 25 percent.</li>
     *   <li>{@code "auto"} = Flexible inset</li>
     * </ul>
     * </p>
     * @return Self for chaining.
     */
    public LayeredLayout setInsetBottom(Component cmp, String inset) {
        getOrCreateConstraint(cmp).bottom().setValue(inset);
        return this;
    }
    
    /**
     * Sets the left inset for this component to the prescribed value.
     * @param cmp The component whose inset we wish to set.
     * @param inset The inset value, including unit.  Units are Percent (%), Millimetres (mm), Pixels (px), and "auto".  E.g. the 
     * following insets values would all be acceptable:
     * <p>
     * <ul>
     *   <li>{@code "2mm"} = 2 millimetres</li>
     *   <li>{@code "2px"} = 2 pixels</li>
     *   <li>{@code "25%"} = 25 percent.</li>
     *   <li>{@code "auto"} = Flexible inset</li>
     * </ul>
     * </p>
     * @return Self for chaining.
     */
    public LayeredLayout setInsetLeft(Component cmp, String inset) {
       getOrCreateConstraint(cmp).left().setValue(inset);
       return this;
    }
    
    /**
     * Sets the right inset for this component to the prescribed value.
     * @param cmp The component whose inset we wish to set.
     * @param inset The inset value, including unit.  Units are Percent (%), Millimetres (mm), Pixels (px), and "auto".  E.g. the 
     * following insets values would all be acceptable:
     * <p>
     * <ul>
     *   <li>{@code "2mm"} = 2 millimetres</li>
     *   <li>{@code "2px"} = 2 pixels</li>
     *   <li>{@code "25%"} = 25 percent.</li>
     *   <li>{@code "auto"} = Flexible inset</li>
     * </ul>
     * </p>
     * @return Self for chaining.
     */
    public LayeredLayout setInsetRight(Component cmp, String inset) {
       getOrCreateConstraint(cmp).right().setValue(inset);
       return this;
    }
    
    
    /**
     * Sets the reference components for the insets of {@literal cmp}. See {@link LayeredLayoutConstraint#setReferenceComponents(com.codename1.ui.Component...) }
     * for a full description of the parameters.
     * @param cmp The component whose reference components we wish to check.
     * @param referenceComponents The reference components.  This var arg may contain 1 to 4 values.  See {@link LayeredLayoutConstraint#setReferenceComponents(com.codename1.ui.Component...) } 
     * for a full description.
     * @return Self for chaining.
     */
    public LayeredLayout setReferenceComponents(Component cmp, Component... referenceComponents) {
        getOrCreateConstraint(cmp).setReferenceComponents(referenceComponents);
        return this;
    }
    
    /**
     * Sets the reference components for this component as a string of 1 to 4 component indices separated by spaces. An 
     * index of {@literal -1} indicates no reference for the corresponding inset.  See {@link LayeredLayoutConstraint#setReferenceComponentIndices(com.codename1.ui.Container, java.lang.String) }
     * for a description of the {@literal refs} parameter.
     * @param cmp The component whose references we're setting.
     * @param refs Reference components as a string of component indices in the parent.
     * @return Self for chaining.
     */
    public LayeredLayout setReferenceComponents(Component cmp, String refs) {
        getOrCreateConstraint(cmp).setReferenceComponentIndices(cmp.getParent(), refs);
        return this;
    }
    
    /**
     * Sets the reference component for the top inset of the given component.
     * @param cmp The component whose insets we are manipulating.
     * @param referenceComponent The component to anchor the inset to.
     * @return Self for chaining.
     */
    public LayeredLayout setReferenceComponentTop(Component cmp, Component referenceComponent) {
        getOrCreateConstraint(cmp).top().referenceComponent(referenceComponent);
        return this;
    }
    
    /**
     * Sets the reference component for the bottom inset of the given component.
     * @param cmp The component whose insets we are manipulating.
     * @param referenceComponent The component to anchor the inset to.
     * @return Self for chaining.
     */
    public LayeredLayout setReferenceComponentBottom(Component cmp, Component referenceComponent) {
        getOrCreateConstraint(cmp).bottom().referenceComponent(referenceComponent);
        return this;
    }
    
    /**
     * Sets the reference component for the left inset of the given component.
     * @param cmp The component whose insets we are manipulating.
     * @param referenceComponent The component to anchor the inset to.
     * @return Self for chaining.
     */
    public LayeredLayout setReferenceComponentLeft(Component cmp, Component referenceComponent) {
        getOrCreateConstraint(cmp).left().referenceComponent(referenceComponent);
        return this;
    }
    
    /**
     * Sets the reference component for the right inset of the given component.
     * @param cmp The component whose insets we are manipulating.
     * @param referenceComponent The component to anchor the inset to.
     * @return Self for chaining.
     */
    public LayeredLayout setReferenceComponentRight(Component cmp, Component referenceComponent) {
        getOrCreateConstraint(cmp).top().referenceComponent(referenceComponent);
        return this;
    }
    
    /**
     * Sets the reference positions for reference components.  See {@link LayeredLayoutConstraint#setReferencePositions(float...) }
     * for a description of the parameters.
     * @param cmp The component whose insets we are manipulating.
     * @param referencePositions The reference positions for the reference components. See {@link LayeredLayoutConstraint#setReferencePositions(float...) }
     * for a full description of this parameter.
     * @return Self for chaining.
     */
    public LayeredLayout setReferencePositions(Component cmp, float... referencePositions) {
        getOrCreateConstraint(cmp).setReferencePositions(referencePositions);
        return this;
    }
    
    /**
     * Sets the reference positions for reference components.  See {@link LayeredLayoutConstraint#setReferencePositions(float...) }
     * for a description of the parameters.
     * @param cmp The component whose insets we are manipulating.
     * @param positions The reference positions for the reference components. See {@link LayeredLayoutConstraint#setReferencePositions(float...) }
     * for a full description of this parameter.
     * @return Self for chaining.
     */
    public LayeredLayout setReferencePositions(Component cmp, String positions) {
        getOrCreateConstraint(cmp).setReferencePositions(positions);
        return this;
    }
    
    /**
     * Sets the top inset reference position.  Only applicable if the top inset has a reference
     * component specified. 
     * @param cmp The component whose insets were are manipulating.
     * @param position The position.  See {@link LayeredLayoutConstraint#setReferencePositions(float...) } for a full
     * description of the possible values here.
     * @return 
     */
    public LayeredLayout setReferencePositionTop(Component cmp, float position) {
        getOrCreateConstraint(cmp).top().referencePosition(position);
        return this;
    }
    
    /**
     * Sets the reference component for the top inset of the given component.
     * @param cmp The component whose insets we are manipulating.
     * @param referenceComponent The component to which the inset should be anchored.
     * @param position The position of the reference anchor.  See {@link LayeredLayoutConstraint#setReferencePositions(float...) }
     * for a full description of reference positions.
     * @return 
     */
    public LayeredLayout setReferenceComponentTop(Component cmp, Component referenceComponent, float position) {
        getOrCreateConstraint(cmp).top().referenceComponent(referenceComponent).referencePosition(position);
        return this;
    }
    
    /**
     * Sets the bottom inset reference position.  Only applicable if the top inset has a reference
     * component specified. 
     * @param cmp The component whose insets were are manipulating.
     * @param position The position.  See {@link LayeredLayoutConstraint#setReferencePositions(float...) } for a full
     * description of the possible values here.
     * @return 
     */
    public LayeredLayout setReferencePositionBottom(Component cmp, float position) {
        getOrCreateConstraint(cmp).bottom().referencePosition(position);
        return this;
    }
    
    /**
     * Sets the reference component for the bottom inset of the given component.
     * @param cmp The component whose insets we are manipulating.
     * @param referenceComponent The component to which the inset should be anchored.
     * @param position The position of the reference anchor.  See {@link LayeredLayoutConstraint#setReferencePositions(float...) }
     * for a full description of reference positions.
     * @return 
     */
    public LayeredLayout setReferenceComponentBottom(Component cmp, Component referenceComponent, float position) {
        getOrCreateConstraint(cmp).bottom().referenceComponent(referenceComponent).referencePosition(position);
        return this;
    }
    
    /**
     * Sets the left inset reference position.  Only applicable if the top inset has a reference
     * component specified. 
     * @param cmp The component whose insets were are manipulating.
     * @param position The position.  See {@link LayeredLayoutConstraint#setReferencePositions(float...) } for a full
     * description of the possible values here.
     * @return 
     */
    public LayeredLayout setReferencePositionLeft(Component cmp, float position) {
        getOrCreateConstraint(cmp).left().referencePosition(position);
        return this;
    }
    
    /**
     * Sets the reference component for the left inset of the given component.
     * @param cmp The component whose insets we are manipulating.
     * @param referenceComponent The component to which the inset should be anchored.
     * @param position The position of the reference anchor.  See {@link LayeredLayoutConstraint#setReferencePositions(float...) }
     * for a full description of reference positions.
     * @return 
     */
    public LayeredLayout setReferenceComponentLeft(Component cmp, Component referenceComponent, float position) {
        getOrCreateConstraint(cmp).left().referenceComponent(referenceComponent).referencePosition(position);
        return this;
    }
    
    /**
     * Sets the right inset reference position.  Only applicable if the top inset has a reference
     * component specified. 
     * @param cmp The component whose insets were are manipulating.
     * @param position The position.  See {@link LayeredLayoutConstraint#setReferencePositions(float...) } for a full
     * description of the possible values here.
     * @return 
     */
    public LayeredLayout setReferencePositionRight(Component cmp, float position) {
        getOrCreateConstraint(cmp).right().referencePosition(position);
        return this;
    }
    
    /**
     * Sets the reference component for the right inset of the given component.
     * @param cmp The component whose insets we are manipulating.
     * @param referenceComponent The component to which the inset should be anchored.
     * @param position The position of the reference anchor.  See {@link LayeredLayoutConstraint#setReferencePositions(float...) }
     * for a full description of reference positions.
     * @return 
     */
    public LayeredLayout setReferenceComponentRight(Component cmp, Component referenceComponent, float position) {
        getOrCreateConstraint(cmp).right().referenceComponent(referenceComponent).referencePosition(position);
        return this;
    }
    /**
     * {@inheritDoc}
     */
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

    /**
     * Lays out the specific component within the container.  This will first lay out any components that it depends on.
     * @param parent The parent container being laid out.
     * @param cmp The component being laid out.
     * @param top 
     * @param left
     * @param bottom
     * @param right 
     */
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
            int innerTop = top;
            int innerBottom = bottom;
            //left = 0;
            //right = parent.getLayoutWidth();
            int leftInset = constraint.insets[Component.LEFT].calculate(cmp, innerTop, left, innerBottom, right);
            int rightInset = constraint.insets[Component.RIGHT].calculate(cmp, innerTop, left, innerBottom, right);
            int topInset = constraint.insets[Component.TOP].calculate(cmp, innerTop, left, innerBottom, right);
            int bottomInset = constraint.insets[Component.BOTTOM].calculate(cmp, innerTop, left, innerBottom, right);
            cmp.setX(leftInset + s.getMarginLeft(parent.isRTL()));
            cmp.setY(topInset + s.getMarginTop());
            cmp.setWidth(Math.max(0, right - left - s.getHorizontalMargins() - rightInset - leftInset));
            cmp.setHeight(Math.max(0, bottom - top - s.getVerticalMargins() - bottomInset - topInset));

        } else {

            int x = left + s.getMarginLeft(parent.isRTL());
            int y = top + s.getMarginTop();
            int w = right - left - s.getHorizontalMargins();
            int h = bottom - top - s.getVerticalMargins();

            cmp.setX(x);
            cmp.setY(y);
            cmp.setWidth(Math.max(0,w));
            cmp.setHeight(Math.max(0,h));
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
    
    /**
     * {@inheritDoc}
     */
    public Dimension getPreferredSize(Container parent) {
        int maxWidth = 0;
        int maxHeight = 0;
        int numOfcomponents = parent.getComponentCount();
        tmpLaidOut.clear();
        for (int i = 0; i < numOfcomponents; i++) {
            Component cmp = parent.getComponentAt(i);
            calcPreferredValues(cmp);
            LayeredLayoutConstraint constraint = (LayeredLayoutConstraint) getComponentConstraint(cmp);
            int vInsets = 0;
            int hInsets = 0;
            if (constraint != null) {
                vInsets += constraint.insets[Component.TOP].preferredValue
                        + constraint.insets[Component.BOTTOM].preferredValue;
                hInsets += constraint.insets[Component.LEFT].preferredValue
                        + constraint.insets[Component.RIGHT].preferredValue;
            }
            maxHeight = Math.max(maxHeight, cmp.getPreferredH() + cmp.getStyle().getMarginTop() + cmp.getStyle().getMarginBottom() + vInsets);
            maxWidth = Math.max(maxWidth, cmp.getPreferredW() + cmp.getStyle().getMarginLeftNoRTL() + cmp.getStyle().getMarginRightNoRTL() + hInsets);

        }
        Style s = parent.getStyle();
        Dimension d = new Dimension(maxWidth + s.getPaddingLeftNoRTL() + s.getPaddingRightNoRTL(),
                maxHeight + s.getPaddingTop() + s.getPaddingBottom());
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
        return d;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "LayeredLayout";
    }

    /**
     * {@inheritDoc}
     */
    public boolean isOverlapSupported() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public boolean obscuresPotential(Container parent) {
        return true;
    }

    /**
     * Shorthand for Container.encloseIn(new LayeredLayout(), cmps);
     *
     * @param cmps the components to add to a new layered layout container
     * @return a newly created layered layout
     */
    public static Container encloseIn(Component... cmps) {
        return Container.encloseIn(new LayeredLayout(), cmps);
    }
    
    /**
     * Creates a new {@link LayeredLayoutConstraint}
     * @return 
     */
    public LayeredLayoutConstraint createConstraint() {
        return new LayeredLayoutConstraint();
    }

    private static int getOuterHeight(Component cmp) {
        Style s = cmp.getStyle();
        return cmp.getHeight() + s.getVerticalMargins();
    }
    
    private static int getOuterPreferredH(Component cmp) {
        Style s = cmp.getStyle();
        return cmp.getPreferredH() + s.getVerticalMargins();
    }
    
    private static int getInnerHeight(Component cmp) {
        Style s = cmp.getStyle();
        return cmp.getHeight() - s.getPaddingTop() - s.getPaddingBottom();
    }
    
    private static int getInnerPreferredH(Component cmp) {
        Style s = cmp.getStyle();
        return cmp.getPreferredH() - s.getPaddingTop() - s.getPaddingBottom();
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
    
    /**
     * A class that encapsulates the insets for a component in layered layout.
     */
    public class LayeredLayoutConstraint {
        
        /**
         * The component that this constraint belongs to.  If you try to add
         * this constraint to a different component, then it will cause a copy
         * to be made rather than using the same component so that constraints
         * to bleed into other components.
         */
        private Component cmp;
        
        /**
         * Gets the insets as a string.
         * @return 
         */
        public String toString() {
            return getInsetsAsString(true);
        }
        
        
        private LayeredLayout outer() {
            return LayeredLayout.this;
        }
        
        /**
         * Recursively fixes all dependencies so that they are contained inside
         * the provided parent.  A dependency is a "referenceComponent".
         * @param parent The parent container within which all dependencies should reside.
         * @return Self for chaining.
         * @see #setReferenceComponents(com.codename1.ui.Component...) 
         */
        public LayeredLayoutConstraint fixDependencies(Container parent) {
            for (Inset inset : insets) {
                inset.fixDependencies(parent);
            }
            return this;
        }
        
        /**
         * Checks to see if this constraint has any circular dependencies.  E.g.
         * Component A has an inset that has Component B as a reference, which has
         * an inset that depends on Component A. 
         * 
         * @param start The start component to check.
         * @return True this forms a circular dependency.
         */
        public boolean hasCircularDependency(Component start) {
            return dependsOn(start);
        }
        
        /**
         * Gets the inset for a particular side.  
         * @param inset One of {@link Component#TOP}, {@link Component#BOTTOM }, {@link Component#LEFT} or
         * {@link Component#RIGHT}.
         * @return The inset.
         */
        public Inset getInset(int inset) {
            return insets[inset];
        }
        
        /**
         * Makes a full copy of this inset. 
         * @return 
         */
        public LayeredLayoutConstraint copy() {
            return copyTo(new LayeredLayoutConstraint());
        }
        
        /**
         * Copies the settings of this constraint into another constraint.
         * @param dest The inset to copy to.
         * @return Self for chaining.
         */
        public LayeredLayoutConstraint copyTo(LayeredLayoutConstraint dest) {
            for (int i=0; i<4; i++) {
                //Inset inset = new Inset(i);
                dest.insets[i] = insets[i].copyTo(dest.insets[i]);
            }
            return dest;
        }
        
        /**
         * Returns a reference box within which insets of the given component are calculated.  If 
         * {@link cmp} has no reference components in any of its insets, then the resulting box will
         * just bee the inner box of the parent (e.g. the parent's inner bounds.
         * @param parent The parent container.
         * @param parent
         * @param box An out parameter.  This will store the bounds of the box.
         * @return The reference box.  (This will be the same object that is passed in the {@literal box} parameter.
         */
        public Rectangle getReferenceBox(Container parent, Rectangle box) {
            return getReferenceBox(parent, null, box);
            
        }
        /**
         * Returns a reference box within which insets of the given component are calculated.  If 
         * {@link cmp} has no reference components in any of its insets, then the resulting box will
         * just bee the inner box of the parent (e.g. the parent's inner bounds.
         * @param parent The parent container.
         * @param cmp The component whose reference box we are obtaining.  Not used.  May be null.
         * @param box An out parameter.  This will store the bounds of the box.
         * @return The reference box.  (This will be the same object that is passed in the {@literal box} parameter.
         * @deprecated Use {@link #getReferenceBox(com.codename1.ui.Container, com.codename1.ui.geom.Rectangle) } instead.
         */
        public Rectangle getReferenceBox(Container parent, Component cmp2, Rectangle box) {
            Style parentStyle = parent.getStyle();
            //Style cmpStyle = cmp.getStyle();
            
            if (top().getReferenceComponent() == null) {
                box.setY(parentStyle.getPaddingTop());
            } else {
                Component ref = top().getReferenceComponent();
                box.setY((int)(getOuterY(ref) + (top().getReferencePosition() * getOuterHeight(ref))));
            }
            
            if (left().getReferenceComponent() == null) {
                box.setX(parentStyle.getPaddingLeftNoRTL());
            } else {
                Component ref = left().getReferenceComponent();
                box.setX((int)(getOuterX(ref) + (left().getReferencePosition() * getOuterWidth(ref))));
            }
            
            if (right().getReferenceComponent() == null) {
                box.setWidth(parent.getWidth() - box.getX() - parentStyle.getPaddingRightNoRTL() - parent.getSideGap());
            } else {
                Component ref = right().getReferenceComponent();
                int refX = (int)(getOuterX(ref) + getOuterWidth(ref) - (right().getReferencePosition() * getOuterWidth(ref)));
                box.setWidth(refX - box.getX());
            }
            
            if (bottom().getReferenceComponent() == null) {
                box.setHeight(parent.getHeight() - box.getY() - parentStyle.getPaddingBottom() - parent.getBottomGap());
            } else {
                Component ref = bottom().getReferenceComponent();
                int refY = (int)(getOuterY(ref) + getOuterHeight(ref) - (bottom().getReferencePosition() * getOuterHeight(ref)));
                box.setHeight(refY - box.getY());
            }
            return box;
        }
        
        /**
         * Returns a reference box within which insets of the given component are calculated.  If 
         * {@link cmp} has no reference components in any of its insets, then the resulting box will
         * just bee the inner box of the parent (e.g. the parent's inner bounds.
         * @param parent The parent container.
         * @param cmp The component whose reference box we are obtaining.
         * @return The reference box. 
         * @deprecated Use {@link #getReferenceBox(com.codename1.ui.Container) 
         */
        public Rectangle getReferenceBox(Container parent, Component cmp) {
            return getReferenceBox(parent, cmp, new Rectangle());
        }
        
        /**
         * Returns a reference box within which insets of the given component are calculated.  If 
         * {@link cmp} has no reference components in any of its insets, then the resulting box will
         * just bee the inner box of the parent (e.g. the parent's inner bounds.
         * @param parent The parent container.
         * @return The reference box.  
         */
        public Rectangle getReferenceBox(Container parent) {
            return getReferenceBox(parent, (Component)null);
        }
        
        /**
         * Shifts the constraint by the specified number of pixels while maintaining the same units.  This is
         * used mainly in the GUI builder to facilitate dragging and resizing of the component.
         * @param x The number of pixels that the insets should be shifted on the x axis.
         * @param y The number of pixels that the insets should be shifted on the y axis.
         * @param preferMM If an inset needs to be switched from flexible to fixed, then this indicates where it will
         * be changed to millimetres or pixels.  {@literal true} for millimetres.
         * @param parent The parent container in which calculations should be performed. 
         * @return Self for chaining.
         * @see #translateMM(float, float, boolean, com.codename1.ui.Container) 
         */
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
        
        /**
         * Shifts the constraint by the specified number of millimetres while maintaining the same units.  This is
         * used mainly in the GUI builder to facilitate dragging and resizing of the component.
         * @param x The number of pixels that the insets should be shifted on the x axis.
         * @param y The number of pixels that the insets should be shifted on the y axis.
         * @param preferMM If an inset needs to be switched from flexible to fixed, then this indicates where it will
         * be changed to millimetres or pixels.  {@literal true} for millimetres.
         * @param parent The parent container in which calculations should be performed. 
         * @return Self for chaining.
         * @see #translatePixels(int, int, boolean, com.codename1.ui.Container) 
         */
        public LayeredLayoutConstraint translateMM(float x, float y, boolean preferMM, Container parent) {
            return translatePixels(Display.getInstance().convertToPixels(x), Display.getInstance().convertToPixels(y), preferMM, parent);
            
        }
        
        /**
         * Gets the set of insets on this constraint that are fixed.  An inset is 
         * considered fixed if it's unit is NOT {@link #UNIT_AUTO}.
         * @return 
         */
        public Collection<Inset> getFixedInsets() {
            ArrayList<Inset> out = new ArrayList<Inset>();
            for (Inset i : insets) {
                if (i.unit != UNIT_AUTO) {
                    out.add(i);
                }
            }
            return out;
        }
        
        /**
         * Gets the set of insets in this constraint that are flexible.  An inset is 
         * considered flexible if it's unit is {@link #UNIT_AUTO}.
         * @return 
         */
        public Collection<Inset> getFlexibleInsets() {
            ArrayList<Inset> out = new ArrayList<Inset>();
            for (Inset i : insets) {
                if (i.unit == UNIT_AUTO) {
                    out.add(i);
                }
            }
            return out;
        }
        
        /**
         * Gets the reference positions of this constraint as a string.
         * @param withLabels True to return the string in CSS format:  e.g. {@code "top:1.0; right:0; bottom:1.0; left:1.0"}  {@literal false}
         * to return as a space-delimited string of inset reference positions in the order "top right bottom left".  E.g. {@literal "1.0 0 1.0 1.0"}
         * @return The reference positions as a string.
         */
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
        
        /**
         * Sets the reference component positions for this constraint from a string.  The string format 
         * may be either using labels following the same output format of {@literal getReferencePositionsAsString(true)}
         * or as a space-delimited string (e.g. {@literal getReferencePositionsAsString(false)}.  When using the label
         * format, you may provide one or more inset values in the string.  E.g. the following are all acceptable:
         * <p>
         * <ul><li>{@literal top:1.0; left:0; right:0; bottom:1.0}</li>
         * <li>{@literal left:0.5}</li>
         * <li>{@literal left:1.0; right:0.5}</li>
         * </ul>
         * </p>
         * <p>
         * If you provide the positions as a space-delimited string, then they are expected to follow the same format
         * as is used in CSS for providing <a href="https://developer.mozilla.org/en/docs/Web/CSS/margin">margin</a>. To summarize:
         * </p>
         * <p>
         * {@code 
//Apply to all four sides 
1.0

//vertical | horizontal
1.0 0

// top | horizontal | bottom 
1.0 0.0 0.5

// top | right | bottom | left 
1.0 1.0 1.0 1.0
}
         * </p>
         * 
         * <p><strong>Interpretation of Reference Positions:</strong></p>
         * <p>When an inset includes a reference component, that means that the inset is "anchored" to that
         * reference component.  I.e. An inset of {@literal 1mm} is measured 1mm from the outer edge of the
         * reference component.  By default it chooses the edge of on the <em>same side</em> as the inset.  So
         * if this is a "left" inset, then it will measure against the "left" outer edge of the reference component.
         * This is the meaning of a {@literal 0} value for the associated reference positions.</p>
         * 
         * <p>A reference position of {@literal 1.0} will start measuring from the opposite edge.  So for a "left" inset,
         * it will measure from the "right" outer edge of the reference component.  You can choose any real value for the
         * reference position, and it will cause the measurement to be scaled accordingly.  E.g. {@literal 0.5.} would measure
         * from the center point of the reference component.</p>
         * @param positionsStr The reference positions.
         * @return Self for chaining.
         */
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
        
        /**
         * Gets the reference component indexes within the provided {@literal parent} container as a string.
         * If an inset doesn't have a reference component, then the corresponding index will be {@literal -1}.
         * <p>
         * Use the {@literal withLabels} parameter to choose whether to include labels with the indices or not.  E.g:
         * 
         * {@code
         * String indices = getReferenceComponentIndicesAsString(parent, true);
         * // Would return something like
         * // "top:-1; right:2; bottom:-1; left: 0"
         * 
         * indices = getReferenceComponentIndicesAsString(parent, false);
         * // Would return something like:
         * // "-1 2 -1 0"  (i.e. Top Right Bottom Left)
         * 
         * // Interpretation: 
         * //   Top inset has no reference component
         * //   Right inset has component with index 2 (i.e. parent.getComponentIndex(rightReferenceComponent) == 2)
         * //   Bottom inset has no reference component
         * //   Left inset has component with index 0 as a reference component.
         * }
         * </p>
         * @param parent
         * @param withLabels
         * @return 
         */
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
        
        /**
         * Sets the reference components of the insets of this constraint as indices of the provided parent
         * container.
         * @param parent The parent container whose children are to be used as reference components.
         * @param indices The indices to set as the reference components.
         * <p>The string format 
         * may be either using labels following the same output format of {@literal cnst.getReferenceComponentIndicesAsString(true)}
         * or as a space-delimited string (e.g. {@literal cnst.getReferenceComponentIndicesAsString(false)}.  When using the label
         * format, you may provide one or more inset values in the string.  E.g. the following are all acceptable:</p>
         * <p>
         * <ul><li>{@literal top:-1; left:0; right:0; bottom:1}</li>
         * <li>{@literal left:1}</li>
         * <li>{@literal left:10; right:-1}</li>
         * </ul>
         * </p>
         * <p>
         * If you provide the positions as a space-delimited string, then they are expected to follow the same format
         * as is used in CSS for providing <a href="https://developer.mozilla.org/en/docs/Web/CSS/margin">margin</a>. To summarize:
         * </p>
         * <p>
         * {@code 
//Set component at index 0 as reference for all 4 insets.
0

//vertical insets use component index 2 | horizontal insets use component index 1
2 1

// top | horizontal | bottom 
-1 3 10

// top | right | bottom | left 
-1 -1 -1 -1
}
*           
         * </p>
         * <p><strong>Note: An index of {@literal -1} means that the corresponding inset has no reference component.</strong></p>
         * @return 
         */
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
        
        /**
         * Gets the insets of this constraint as a string. If {@literal withLabels} is {@literal true}, then it
         * will return a string of the format:
         * <p>{@literal top:2mm; right:0; bottom:10%; left:auto}</p>
         * <p>If {@literal withLabels} is {@literal false} then it will return a space-delimited string with 
         * the inset values ordered "top right bottom left" (the same as for CSS margins) order.</p>
         * 
         * @param withLabels
         * @return 
         */
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
        
        /**
         * Sets the reference components for the constraint.
         * @param refs May contain 1, 2, 3, or 4 values.  If only 1 value is passed, then it is
         * set on all 4 insets.  If two values are passed, then the first is set on the top and bottom
         * insets, and the 2nd is set on the left and right insets (i.e. vertical | horizontal).
         * If 3 values are passed, then, they are used for top, horizontal, and bottom.
         * If 4 values are passed, then they are used for top, right, bottom, left (in that order).
         * @return Self for chaining.
         */
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
        
        
        /**
         * Sets the reference positions for the constraint.
         * <p><strong>Interpretation of Reference Positions:</strong></p>
         * <p>When an inset includes a reference component, that means that the inset is "anchored" to that
         * reference component.  I.e. An inset of {@literal 1mm} is measured 1mm from the outer edge of the
         * reference component.  By default it chooses the edge of on the <em>same side</em> as the inset.  So
         * if this is a "left" inset, then it will measure against the "left" outer edge of the reference component.
         * This is the meaning of a {@literal 0} value for the associated reference positions.</p>
         * 
         * <p>A reference position of {@literal 1.0} will start measuring from the opposite edge.  So for a "left" inset,
         * it will measure from the "right" outer edge of the reference component.  You can choose any real value for the
         * reference position, and it will cause the measurement to be scaled accordingly.  E.g. {@literal 0.5.} would measure
         * from the center point of the reference component.</p>
         * 
         * @param p May contain 1, 2, 3, or 4 values.  If only 1 value is passed, then it is
         * set on all 4 insets.  If two values are passed, then the first is set on the top and bottom
         * insets, and the 2nd is set on the left and right insets (i.e. vertical | horizontal).
         * If 3 values are passed, then, they are used for top, horizontal, and bottom.
         * If 4 values are passed, then they are used for top, right, bottom, left (in that order).
         * @return Self for chaining.
         */
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
        
        /**
         * Sets the insets for this constraint as a string.  The string may include labels
         * or it may be a space delimited string of values with "top right bottom left" order.
         * 
         * <p>
         * If providing as a space-delimited string of inset values, then you can provide 1, 2, 3, or 4
         * values.  If only 1 value is passed, then it is
         * set on all 4 insets.  If two values are passed, then the first is set on the top and bottom
         * insets, and the 2nd is set on the left and right insets (i.e. vertical | horizontal).
         * If 3 values are passed, then, they are used for top, horizontal, and bottom.
         * If 4 values are passed, then they are used for top, right, bottom, left (in that order).
         * </p>
         * <p>
         * <strong>Example Inputs</strong>
         * </p>
         * <p>
         * <ul>
         *   <li>{@literal "0 0 0 0"} = all 4 insets are zero pixels</li>
         *   <li>{@literal "0 1mm"} = Vertical insets are zero.  Horizontal insets are 1mm</li>
         *   <li>{@literal "10% auto 20%"} = Top inset is 10%.  Horizontal insets are flexible.  Bottom is 20%</li>
         *   <li>{@literal "1mm 2mm 3mm 4mm"} = Top=1mm, Right=2mm, Bottom=3mm, Left=4mm</li>
         * </ul>
         * </p>
         * @param insetStr
         * @return Self for chaining.
         */
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
        
        /**
         * Gets the left inset.
         * @return The left inset
         */
        public Inset left() {
            return insets[Component.LEFT];
        }
        
        /**
         * Gets the right inset.
         * @return The right inset.
         */
        public Inset right() {
            return insets[Component.RIGHT];
        }
        
        /**
         * Gets the top inset
         * @return The top inset
         */
        public Inset top() {
            return insets[Component.TOP];
        }
        
        /**
         * Gets the bottom inset.
         * @return The bottom inset
         */
        public Inset bottom() {
            return insets[Component.BOTTOM];
        }
        
        /**
         * Gets the constraint itself.
         * @return 
         */
        public LayeredLayoutConstraint constraint() {
            return this;
        }
        
        /**
         * The insets for this constraint.
         */
        private final Inset[] insets = new Inset[]{
            new Inset(Component.TOP),
            new Inset(Component.LEFT),
            new Inset(Component.BOTTOM),
            new Inset(Component.RIGHT)
        };

        //private Rectangle preferredBounds;

        /**
         * Gets the dependencies (i.e. recursively gets  all reference components).
         * @param deps A set to add the dependencies to. (An "out" parameter).
         * @return The set of dependencies.  Same as {@literal dep} parameter.
         */
        public Set<Component> getDependencies(Set<Component> deps) {
            for (Inset inset : insets) {
                inset.getDependencies(deps);
            }
            return deps;
        }
        
        /**
         * Gets the dependencies (i.e. recursively gets  all reference components).
         * @return The set of dependencies. 
         */
        public Set<Component> getDependencies() {
            return getDependencies(new HashSet<Component>());
        }
        
        /**
         * Checks to see if this constraint has the given component in its set of dependencies.
         * @param cmp The component to check.
         * @return True if {@literal cmp} is a reference component of some inset in this
         * constraint (recursively).
         */
        public boolean dependsOn(Component cmp) {
            return getDependencies().contains(cmp);
        }

        /**
         * Encapsulates an inset.
         */
        public class Inset {
            int delta;
            
            /**
             * Creates a new inset for the given side.
             * @param side One of {@link Component#TOP}, {@link Component#BOTTOM}, {@link Component#LEFT}, or {@link Component#RIGHT}.
             */
            public Inset(int side) {
                this.side = side;
            }

            /**
             * Prints this inset as a string.
             * @return 
             */
            public String toString() {
                switch (side) {
                    case Component.TOP : return "top="+getValueAsString();
                    case Component.BOTTOM: return "bottom="+getValueAsString();
                    case Component.LEFT: return "left="+getValueAsString();
                    default: return "right="+getValueAsString();
                }
            }
            
           
            
            /**
             * Gets the value of this inset as a string. Values will be in the format {@literal <value><unit>}, e.g.
             * {@literal 2mm}, {@literal 15%}, {@literal 5px}, {@literal auto} (meaning it is flexible.
             * @return The value of this inset as a string.
             */
            public String getValueAsString() {
                switch (unit) {
                    case UNIT_DIPS: return value +"mm";
                    case UNIT_PIXELS: return ((int)value)+"px";
                    case UNIT_PERCENT: return value + "%";
                    case UNIT_AUTO: return "auto";
                    case UNIT_BASELINE: return "baseline";
                }
                return null;
            }
            
            /**
             * Gets the value of this inset as a string rounding to the specified number of decimal places. 
             * Values will be in the format {@literal <value><unit>}, e.g.
             * {@literal 2mm}, {@literal 15%}, {@literal 5px}, {@literal auto} (meaning it is flexible.
             * @return The value of this inset as a string.
             * @see #getValueAsString() 
             */
            public String getValueAsString(int decimalPlaces) {
                L10NManager l10n = L10NManager.getInstance();
                
                switch (unit) {
                    case UNIT_DIPS: return l10n.format(value, decimalPlaces) +"mm";
                    case UNIT_PIXELS: return ((int)value)+"px";
                    case UNIT_PERCENT: return l10n.format(value, decimalPlaces) + "%";
                    case UNIT_AUTO: return "auto";
                    case UNIT_BASELINE: return "baseline";
                }
                return null;
            }
            
            /**
             * Fixes dependencies in this inset recursively so that all reference
             * components are children of the given parent container.  If a reference
             * component is not in the parent, then it will first check to find a
             * child of {@literal parent} with the same name as the reference component. 
             * Failing that, it will try to find a child of {@literal parent} with the 
             * same index. 
             * 
             * If an appropriate match is found, it will replace the referenceComponent
             * with the match.
             * 
             * 
             * @param parent The container in which all reference components should reside.
             * @return Self for chaining.
             */
            private Inset fixDependencies(Container parent) {
                Container refParent;
                if (referenceComponent != null && (refParent = referenceComponent.getParent()) != parent) {
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
            
            /**
             * Sets the value of this inset as a string.  E.g. "2mm", or "2px", or "3%", "auto", or "baseline".
             * @param value The value of this inset.
             * @return Self for chaining.
             */
            public Inset setValueAsString(String value) {
                setValue(value);
                return this;
            }
            
            /**
             * Gets the left inset in this constraint.
             * @return The left inset of the constraint.
             */
            public Inset left() {
                return constraint().left();
            }
            
            /**
             * Gets the right inset in the constraint.
             * @return The right inset of the constraint.
             */
            public Inset right() {
                return constraint().right();
            }
            
            /**
             * Gets the top inset in this constraint.
             * @return The top inset in this constraint.
             */
            public Inset top() {
                return constraint().top();
            }
            
            /**
             * Gets the bottom inset in this constraint.
             * @return The bottom inset.
             */
            public Inset bottom() {
                return constraint().bottom();
            }
            
            /**
             * Gets the constraint that contains this inset.
             * @return The parent constraint of this inset.
             */
            public LayeredLayoutConstraint constraint() {
                return LayeredLayoutConstraint.this;
            }
            
            /**
             * Sets the unit for this constraint.  This doesn't perform any recalculation
             * on the value.  Just sets the unit.
             * @param unit The unit.  One of {@link #UNIT_AUTO}, {@link #UNIT_DIPS}, {@link #UNIT_PIXELS}, or {@link #UNIT_PERCENT}.
             * @return Self for chaining.
             * @see #setAuto() 
             * @see #setDips() 
             * @see #setPixels() 
             * @see #setPercent() 
             * @see #changeUnits(byte) To change units while recalculating the value to be effectively equivalent.
             */
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
            
            /**
             * Sets the units to "auto" (i.e. makes the inset flexible).  Doesn't perform any calculations
             * on the value.
             * @return Self for chaining.
             * @see #unit(byte) 
             * 
             */
            public Inset setAuto() {
                return unit(UNIT_AUTO);
            }
            
            /**
             * Sets the units to "dips" (millimetres).  Doesn't perform any calculations on the value.
             * @return Self for chaining.
             * @see #unit(byte) 
             */
            public Inset setDips() {
                return unit(UNIT_DIPS);
            }
            
            /**
             * Sets the units to percent.  Doesn't perform any calculations on the value.
             * @return Self for chaining.
             * @see #unit(byte) 
             */
            public Inset setPercent() {
                return unit(UNIT_PERCENT);
            }
            
            /**
             * Sets the units to pixels.  Doesn't perform any calculations on the value.
             * @return Self for chaining.
             */
            public Inset setPixels() {
                return unit(UNIT_PIXELS);
            }
            
            /**
             * Sets the inset value to the provided number of pixels.  This will chnage the unit
             * to pixels.
             * @param px The pixel value of the inset.
             * @return Self for chaining.
             */
            public Inset setPixels(int px) {
                this.value = px;
                return unit(UNIT_PIXELS);
            }
            
            /**
             * Sets the inset value to the provided dips/millimetre value. This will change 
             * the unit to millimetres.
             * @param dips The inset value in millimetres.
             * @return Self for chaining.
             */
            public Inset setDips(float dips) {
                this.value = dips;
                return unit(UNIT_DIPS);
            }
            
            /**
             * Sets the inset value in percentage.  This will change the unit to percentage.
             * @param percent The inset value as a percentage.
             * @return Self for chaining.
             */
            public Inset setPercent(float percent) {
                if (percent == Float.POSITIVE_INFINITY || percent == Float.NEGATIVE_INFINITY) {
                    throw new IllegalArgumentException("Attempt to set illegal percent value");
                }
                this.value = percent;
                return unit(UNIT_PERCENT);
            }
            
            /**
             * Sets the reference component for this inset.
             * @param cmp The reference component. (I.e. the component that the inset is "anchored" to).
             * @return Self for chaining.
             * @see #referencePosition(float) 
             * @see LayeredLayoutConstraint#setReferenceComponents(com.codename1.ui.Component...) 
             */
            public Inset referenceComponent(Component cmp) {
                referenceComponent = cmp;
                return this;
            }
            
            /**
             * Sets the reference position for this inset.  A value of {@literal 0} indicates that the inset
             * is anchored to the same side of the reference component (e.g. right inset anchored to right edge of reference component, 
             * left inset anchored to left edge of reference component).  A value of {@literal 1} indicates that the 
             * inset is anchored to the opposite side of the reference component. E.g. right inset to left edge.
             * @param position The reference position.
             * @return Self for chaining.
             * @see #setReferencePositions(java.lang.String) 
             * @see #setReferencePositions(com.codename1.ui.Component, java.lang.String) 
             * @see #setReferencePositions(com.codename1.ui.Component, float...) 
             */
            public Inset referencePosition(float position) {
                this.referencePosition = position;
                return this;
            }
            
            /**
             * Sets the value of this inset.  The interpretation of the value will depend on the {@link #unit}.
             * If the unit is {@link #UNIT_DIPS}, then this value is interpreted in millimetres, etc..
             * @param value The value to set this inset to.
             * @return Self for chaining.
             */
            public Inset value(float value) {
                this.value = value;
                return this;
            }
            
            /**
             * Gets the side of this inset. One of {@link Component#TOP}, {@link Component#Bottom}, {@link Component#LEFT}, {@link Component#RIGHT}
             * @return The side of this inset.  One of {@link Component#TOP}, {@link Component#Bottom}, {@link Component#LEFT}, {@link Component#RIGHT}
             */
            public int getSide() {
                return side;
            }
            
            /**
             * Gets the reference component for this inset.
             * @return The reference component for this inset.
             * @see #referenceComponent(com.codename1.ui.Component) 
             */
            public Component getReferenceComponent() {
                return referenceComponent;
            }
            
            /**
             * Gets the reference position for this inset.
             * @return The reference position for this inset.
             */
            public float getReferencePosition() {
                return referencePosition;
            }

            /**
             * One of
             * {@link Component#TOP}, {@link Component#Bottom}, {@link Component#LEFT}, {@link Component#RIGHT}
             */
            private int side;

            /**
             * The component that is used a reference for this inset.
             * {@literal null} for the parent component.
             */
            private Component referenceComponent;

            /**
             * {@code 0.0 } = left/top of {@link #referenceComponent}.  {@code 1.0 } for bottom/right or {@link #referenceComponent}.
             */
            private float referencePosition;

            /**
             * The value of this inset. Interpreted in {@link #unit} units.
             */
            private float value;

            /**
             * The unit of this inset.
             */
            private byte unit = UNIT_PIXELS;

            /**
             * Caches the preferred value of this inset last time it was calculated.
             */
            private int preferredValue;
            
            /**
             * The calculated value of this inset in pixels.  This is calculated in the {@link #calculate(com.codename1.ui.Component, int, int, int, int) }
             * method which is only called during layout.  It will be the absolute size of the inset in pixels 
             * including all reference components.
             */
            private int calculatedValue;
            
            /**
             * The calculated base value of this inset in pixels.  This is calculated during the layout step, so
             * this will always be the pixel "base" value the last time layout was performed.  The base value
             * is the absolute value of the reference box inset.  E.g. if this inset has no reference component,
             * then this will always be zero.  If there is a reference componnet, then this will be the value
             * of the "zero" point for measuing the inset.  {@link #calculatedValue} - {@link #calculatedBaseValue} should
             * be equal to {@link #value} (if value is in pixels).
             */
            private int calculatedBaseValue;
            
            /**
             * Tracks whether the size of the component was clipped during the last layout.  This will occur
             * when the preferred size of the component would have it overflowing the reference box.  In such cases
             * the component is "clipped" to not obtain its full preferred value.
             */
            private boolean autoIsClipped;

            /**
             * Calculate the preferred value of this inset.
             * @param parent The parent container.
             * @param cmp The component
             * @return The preferred value of this inset in pixels.
             */
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
                            preferredValue = 0;
                            break;
                        case UNIT_AUTO:
                            preferredValue = 0;
                            break;
                        case UNIT_BASELINE:
                            preferredValue = 0;
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
                                baseValue += ((float) refPreferredH ) * referencePosition;
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
                            preferredValue = baseValue;
                            break;
                        case UNIT_AUTO:
                            preferredValue = baseValue;
                            break;
                        case UNIT_BASELINE: {
                            Style rs = referenceComponent.getStyle();
                            Style s = cmp.getStyle();
                            preferredValue = baseValue + (referenceComponent.getPreferredH() - cmp.getPreferredH())/2
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

            /**
             * Calculates the "base" value off of which the inset's value should be calculated.
             * 
             * @param top The top "y" coordinate within the parent container from which insets are measured.
             * @param left The left "x" coordinate within the parent container from which insets are measured.
             * @param bottom The bottom "y" coordinate within the parent container from which insets are measured.
             * @param right The right "x" coordinate within the parent container from which insets are measured.
             * @return 
             */
            private int calcBaseValue(int top, int left, int bottom, int right) {//, int paddingTop, int paddingLeft, int paddingBottom, int paddingRight) {
                int h = bottom - top;
                int w = right - left;
                int baseValue = 0;
                if (referenceComponent != null) {
                        switch (side) {
                            case Component.TOP:
                                baseValue = getOuterY(referenceComponent) + (int)(getOuterHeight(referenceComponent) * referencePosition) - top;
                                break;
                            case Component.BOTTOM:
                                baseValue = (bottom - getOuterHeight(referenceComponent) - getOuterY(referenceComponent)) + (int)(getOuterHeight(referenceComponent) * referencePosition);
                                break;
                            case Component.LEFT:
                                baseValue = getOuterX(referenceComponent) + (int)(getOuterWidth(referenceComponent) * referencePosition) - left;
                                break;
                            default:
                                baseValue = (right - getOuterWidth(referenceComponent) - getOuterX(referenceComponent)) + (int)(getOuterWidth(referenceComponent)* referencePosition);
                                break;
                        }
                    calculatedBaseValue = baseValue;
                    return baseValue;
                }
                        
                if (referencePosition != 0) {
                    switch (side) {
                        case Component.TOP:
                            baseValue = (int) ((float) h * referencePosition);
                            break;
                        case Component.BOTTOM:
                            baseValue = (int) ((float) h * referencePosition);
                            break;
                        case Component.LEFT:
                            baseValue = (int) ((float) w * referencePosition);
                            break;
                        case Component.RIGHT:
                            baseValue = (int) ((float) w * referencePosition);
                            break;
                        default:
                            throw new RuntimeException("Illegal side for inset: " + side);
                    }
                }
                calculatedBaseValue = baseValue;
                return baseValue;
            }
            
            /**
             * True if this is top or bottom.
             * @return 
             */
            private boolean isVerticalInset() {
                return side == Component.TOP || side == Component.BOTTOM;
            }
            
            /**
             * True if this is left or right.
             * @return 
             */
            private boolean isHorizontalInset() {
                return side == Component.LEFT || side == Component.RIGHT;
            }
            
            
            
            /**
             * Calculates the actual value of this inset.  This is used inside {@link #layoutComponent(com.codename1.ui.Container, com.codename1.ui.Component, int, int, int, int) }.
             * 
             * @param cmp The component.
             * @param top
             * @param left
             * @param bottom
             * @param right
             * @return The actual value of this inset.
             */
            private int calculate(Component cmp, int top, int left, int bottom, int right) {
                if (side == Component.BOTTOM && getOppositeInset().unit == UNIT_BASELINE) {
                    unit = UNIT_AUTO;
                }
                int w = right - left;
                int h = bottom - top;
                int baseValue = calcBaseValue(top, left, bottom ,right);
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
                            calculatedValue = (int)(baseValue + (h - oppositeBaseValue - baseValue) * value / 100f);
                        } else {
                            calculatedValue = (int)(baseValue + (w - oppositeBaseValue - baseValue) * value / 100f);
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
                            calculatedValue = baseValue + (ref.getHeight() - cmp.getPreferredH())/2
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
                                    calculatedValue = baseValue + (h - oppositeBaseValue - baseValue - getOuterPreferredH(cmp))/2;
                                }
                            } else {
                                if (cmp.getPreferredW() <= 0) {
                                    calculatedValue = baseValue;
                                    autoIsClipped = true;
                                } else {
                                    calculatedValue = baseValue + (w - oppositeBaseValue - baseValue - getOuterPreferredW(cmp))/2;
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

            /**
             * Recursively gets all of the reference components of this inset.
             * @param deps An "out" parameter.  The set that will hold the dependencies.
             * @return The set of all reference components (crawled recursively of this inset.
             */
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
            
            /**
             * Recursively gets all of the reference components of this inset.
             * @return The set of all reference components (crawled recursively of this inset.
             */
            public Set<Component> getDependencies() {
                return getDependencies(new HashSet<Component>());
            }

            /**
             * Gets the opposite inset of this inset within its parent constraint.  E.g. if this is the
             * left inset, it will get the associated right inset.
             * @return The opposite inset.
             */
            private Inset getOppositeInset() {
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

                    }
                    return cnst.insets[oppSide];
                }
                return null;
            }
            
            /**
             * Sets the value of this inset.  E.g. "2mm", "1px", "25%", or "auto".
             * @param val 
             */
            private void setValue(String val) {
                int pos;
                if ((pos=val.indexOf("mm")) != -1) {
                    this.setDips(Float.parseFloat(val.substring(0, pos)));
                } else if ((pos=val.indexOf("px")) != -1) {
                    this.setPixels(Integer.parseInt(val.substring(0, pos)));
                } else if ((pos=val.indexOf("%")) != -1) {
                    this.setPercent(Float.parseFloat(val.substring(0, pos)));
                } else if ("auto".equals(val)) {
                    this.setAuto();
                } else if ("baseline".equals(val)) {
                    this.unit(UNIT_BASELINE);
                } else {
                    this.setPixels(Integer.parseInt(val));
                    
                }
            }
            
            /**
             * Copies this inset into another inset.
             * @param dest The inset to copy to.
             * @return The copied inset.
             */
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
            
            /**
             * Copies this inset to the corresponding inset of the provided constraint.
             * @param dest The constraint to copy the inset into.
             * @return The corresponding inset in {@literal dest} that we copied the inset into.
             */
            public Inset copyTo(LayeredLayoutConstraint dest) {
                copyTo(dest.insets[side]);
                return dest.insets[side];
            }
            
            /**
             * Copies this inset into the corresponding inset of the provided component.
             * @param cmp The component that we are copying the inset into.
             * @return The copied inset.
             */
            public Inset copyTo(Component cmp) {
                copyTo(getOrCreateConstraint(cmp));
                return this;
            }
            
            /**
             * Creates a copy of this inset.
             * @return 
             */
            public Inset copy() {
                return copyTo(new Inset(side));
            }
            
            /**
             * Gets the unit of this inset. 
             * @return One of {@link #UNIT_AUTO}, {@link #UNIT_DIPS}, {@link #UNIT_PIXELS}, or {@link #UNIT_PERCENT}.
             */
            public byte getUnit() {
                return unit;
            }
            
            /**
             * Checks if this is a fixed inset. An inset is considered "fixed" if its unit is not {@link #UNIT_AUTO}
             * @return True if the inset is fixed.
             */
            public boolean isFixed() {
                return unit != UNIT_AUTO;
            }
            
            /**
             * Gets the current value of this inset in millimetres.  If the inset uses a different unit, then 
             * this will calculate the corresponding value.
             * @return 
             */
            public float getCurrentValueMM() {
                if (unit == UNIT_DIPS) {
                    return value;
                } else if (unit == UNIT_PIXELS) {
                    float pixelsPerDip = Display.getInstance().convertToPixels(1000)/1000f;
                    return value / pixelsPerDip;
                } else {
                    // In both auto and percent cases, we'll use the existing calculated value as our base
                    float pixelsPerDip = Display.getInstance().convertToPixels(1000)/1000f;
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
            
            /**
             * Gets the current value of this inset in pixels.  If the inset uses a different unit
             * then this will calculate the corresponding value.
             * @return The value of this inset in pixels.
             */
            public int getCurrentValuePx() {
                if (unit == UNIT_DIPS) {
                    return Display.getInstance().convertToPixels(value);
                } else if (unit == UNIT_PIXELS) {
                    return (int)value;
                } else {
                    // In both auto and percent cases, we'll use the existing calculated value as our source.
                    int calc = calculatedValue;
                    if (referenceComponent != null) {
                        
                        calc -= calculatedBaseValue;
                    }
                    return calc + delta;
                }
            }
            
            /**
             * True if this is a vertical inset (top or bottom).
             * @return 
             */
            public boolean isVertical() {
                return side == Component.TOP || side == Component.BOTTOM;
            }
            
            /**
             * True if this is a horizontal inset (left or right).
             * @return 
             */
            public boolean isHorizontal() {
                return side == Component.LEFT || side == Component.RIGHT;
            }
            
            /**
             * Changes the units of this inset, and updates the value to remain 
             * the same as the current value. 
             * @param unit The unit.  One of {@link #UNIT_AUTO}, {@link #UNIT_DIPS}, {@link #UNIT_PIXELS}, or {@link #UNIT_PERCENT}.
             * @return Self for chaining.
             * @deprecated Use {@link #changeUnitsTo(byte, com.codename1.ui.Container) }
             */
            public Inset changeUnits(byte unit) {
                return changeUnits(unit, cmp);
                
            }
            
            /**
             * Changes the units of this inset, and updates the value to remain 
             * the same as the current value. 
             * @param unit The unit.  One of {@link #UNIT_AUTO}, {@link #UNIT_DIPS}, {@link #UNIT_PIXELS}, or {@link #UNIT_PERCENT}.
             * @param cmp The component for which the inset is applying.
             * @return Self for chaining.
             * @deprecated Use {@link #changeUnitsTo(byte, com.codename1.ui.Container) }
             */
            public Inset changeUnits(byte unit, Component cmp) {
                return changeUnitsTo(unit, cmp == null ? null : cmp.getParent());
            }
            
            /**
             * Changes the units of this inset, and updates the value to remain 
             * the same as the current value. 
             * @param unit The unit.  One of {@link #UNIT_AUTO}, {@link #UNIT_DIPS}, {@link #UNIT_PIXELS}, or {@link #UNIT_PERCENT}.
             * @param parent The container in which the layout applies.
             * @return Self for chaining.
             */
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

                                setPercent(getCurrentValuePx() * 100f / (isVertical()?refBox.getHeight() : refBox.getWidth()));
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
                            setPercent(getCurrentValuePx() * 100f / (isVertical()?refBox.getHeight() : refBox.getWidth()));
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
            
            /**
             * Changes the reference component, while updating the value to remain in the same
             * absolute position.
             * @param parent The parent container.
             * @param newRef The new reference component.
             * @param pos The reference position.
             * @return Self for chaining.
             */
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
                    if (newRef != referenceComponent || pos != referencePosition) {
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
                        if (restoreUnit >=0) {
                            changeUnitsTo(restoreUnit, parent);
                        }
                        if (oppRestoreUnit >= 0) {
                            getOppositeInset().changeUnitsTo(oppRestoreUnit, parent);
                        }
                    }
                //}
                
                return this;
                
                
            }
            
            /**
             * Checks if this is a flexible inset.  An inset is considered flexible if its unit is {@link #UNIT_AUTO}.
             * @return True if this is a flexible inset.
             * @see #isFixed() 
             */
            public boolean isFlexible() {
                return unit == UNIT_AUTO;
            }
            
            /**
             * Returns the total inset of this inset when applied to the given component.
             * This will calculate and sum all of the insets of reference components to 
             * get the total inset in pixels from the parent component.
             * @param cmp The component context.
             * @return The total inset in pixels from the parent.
             */
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
                    case UNIT_PIXELS :
                        return baseValue + (int)value;
                    case UNIT_DIPS :
                        return baseValue + Display.getInstance().convertToPixels(value);
                    case UNIT_PERCENT : {
                        
                        Rectangle baseRect = getReferenceBox(parent, cmp);
                        //System.out.println("Baserect is "+baseRect+" baseValue="+baseValue+" for percent "+value);
                        int out = (int)(baseValue + (isHorizontalInset() ? baseRect.getWidth() : baseRect.getHeight()) * value / 100f);
                        //System.out.println("Result is "+out);
                        return out;
                    }
                    case UNIT_BASELINE : {
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
                            return baseValue + (ref.getHeight()-cmp.getPreferredH())/2
                                    + (rs.getPaddingTop() - cs.getPaddingTop())
                                    + (rs.getMarginTop() - cs.getMarginTop())
                                    + (ra - ca);
                        }
                    }
                    case UNIT_AUTO : {
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
                                int out =  right - left - oppositeInset.getAbsolutePixels(cmp) - baseValue - getOuterPreferredW(cmp);
                                //System.out.println("Auto value is "+out);
                                return out;
                            }
                        }
                    }
                    default :
                        throw new RuntimeException("Illegal state in inset.  Unknown unit "+unit);
                        
                }
                
            }
            
            /**
             * Translates the inset by {@literal delta} pixels.
             * @param delta Pixels to translate this inset by.
             * @param preferMM If this is a flexible inset, then translating it will require changing it to fixed.  {@literal true} to use millimetres.  {@literal false} to use pixels.
             * @param parent The parent container used for calculating equivalent percent if this is a percent inset.
             * @return Self for chaining.
             * @see #translateMM(float, boolean, com.codename1.ui.Container) 
             */
            public Inset translatePixels(int delta, boolean preferMM, Container parent) {
                
                switch (unit) {
                    case UNIT_PIXELS :
                        value += delta;
                        break;
                    case UNIT_DIPS : {
                        float pixelsPerDip = Display.getInstance().convertToPixels(1000)/1000f;
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
                            float percentDelta = delta / (float)relH * 100f;
                            if (percentDelta == Float.NEGATIVE_INFINITY || percentDelta == Float.POSITIVE_INFINITY) {
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
                            if (percentDelta == Float.NEGATIVE_INFINITY || percentDelta == Float.POSITIVE_INFINITY) {
                                percentDelta = 0f;
                            }
                            value += percentDelta;
                            //System.out.println("Value="+value);
                        }
                        break;
                        
                    }
                    case UNIT_BASELINE : {
                        changeUnitsTo(UNIT_DIPS, parent);
                        return translatePixels(delta, preferMM, parent);
                    }
                    case UNIT_AUTO : {
                        // If this is auto then we'll need to make it fixed... but we'll start
                        // by making it fixed
                        unit = preferMM ? UNIT_DIPS : UNIT_PIXELS;
                        if (unit == UNIT_PIXELS) {
                            value = calculatedValue + delta - calculatedBaseValue;
                        } else {
                            float pixelsPerDip = Display.getInstance().convertToPixels(1000)/1000f;
                            value = (calculatedValue + delta - calculatedBaseValue) / pixelsPerDip;
                        }
                        break;
                    }
                        
                }
                //calculatedValue += delta;
                
                this.delta += delta;
                if (getOppositeInset().isFlexible()) {
                    getOppositeInset().delta -= delta;
                }
                return this;
            }
            
            /**
             * Translates the inset by {@literal delta} millimetres.
             * @param delta Pixels to translate this inset by.
             * @param preferMM If this is a flexible inset, then translating it will require changing it to fixed.  {@literal true} to use millimetres.  {@literal false} to use pixels.
             * @param parent The parent container used for calculating equivalent percent if this is a percent inset.
             * @return Self for chaining.
             */
            public Inset translateMM(float delta, boolean preferMM, Container parent) {
                return translatePixels(Display.getInstance().convertToPixels(delta), preferMM, parent);
               
            }

        }

    }

}