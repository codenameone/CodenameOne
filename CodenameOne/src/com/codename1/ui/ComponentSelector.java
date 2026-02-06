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
package com.codename1.ui;


import com.codename1.components.SpanButton;
import com.codename1.components.SpanLabel;
import com.codename1.io.Util;
import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.animations.ComponentAnimation;
import com.codename1.ui.animations.Transition;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.ActionSource;
import com.codename1.ui.events.DataChangedListener;
import com.codename1.ui.events.FocusListener;
import com.codename1.ui.events.ScrollListener;
import com.codename1.ui.events.StyleListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.plaf.Border;
import com.codename1.ui.plaf.Style;
import com.codename1.util.SuccessCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/// A tool to facilitate selection and manipulation of UI components as sets.  This uses fluent API style, similar
/// to jQuery to make it easy to find UI components and modify them as groups.
///
/// Set Selection
///
/// Sets of components can either be created by explicitly adding components to the set, or by
/// providing a "selector" string that specifies how the set should be formed.  Some examples:
///
///
/// - `$("Label")`  - The set of all components on the current form with UIID="Label"
///
/// - `$("#AddressField")` - The set of components with name="AddressField"
///
/// - `$("TextField#AddressField")` - The set of components with UIID=TextField and Name=AddressField
///
/// - `$("Label, Button")` - Set of components in current form with UIID="Label" or UIID="Button"
///
/// - `$("Label", myContainer)` - Set of labels within the container `myContainer`
///
/// - `$("MyContainer *")` - All descendants of the container with UIID="MyContainer".  Will not include "MyContainer".
///
/// - `$("MyContainer > *")` - All children of of the container with UIID="MyContainer".
///
/// - `$("MyContainer > Label)` - All children with UIID=Label of container with UIID=MyContainer
///
/// - `$("Label").getParent()` - All parent components of labels in the current form.
///
/// Tags
///
/// To make selection more flexible, you can "tag" components so that they can be easily targeted by a selector.
/// You can add tags to components using `#addTags(java.lang.String...)`, and remove them using `#removeTags(java.lang.String...)`.
/// Once you have tagged a component, it can be targeted quite easily using a selector.  Tags are specified in a selector with a .
/// prefix.  E.g.:
///
///
/// - `$(".my-tag")` - The set of all components with tag "my-tag".
///
/// - `$("Label.my-tag")` - The set of all components with tag "my-tag" and UIID="Label"
///
/// - `$("Label.my-tag.myother-tag")` - The set of all components with tags "my-tag" and "myother-tag" and UIID="Label".  Matches only components that include all of those tags.
///
/// Modifying Components in Set
///
/// While component selection alone in the ComponentSelector is quite powerful, the true power comes when
/// you start to operate on the entire set of components using the Fluent API of ComponentSelector.  ComponentSelector
/// includes wrapper methods for most of the mutator methods of `Component`, `Container`, and a few other common
/// component types.
///
/// For example, the following two snippets are equivalent:
///
/// ```java
/// `for (Component c : $("Label")) {
///     c.getStyle().setFgColor(0xff0000);`
/// }
/// ```
///
/// and
///
/// ```java
/// `$("Label").setFgColor(0xff0000);`
/// ```
///
/// The second snippet is clearly easier to type and more compact.  But we can take it further.  The Fluent API
/// style allows you to chain together multiple method calls.  This even makes it desirable to operate on
/// single-element sets.  E.g.:
///
/// ```java
/// `Button myButton = $(new Button("Some text"))
/// .setUIID("Label")
/// .addTags("cell", "row-"+rowNum, "col-"+colNum, rowNum%2==0 ? "even":"odd")
/// .putClientProperty("row", rowNum)
/// .putClientProperty("col", colNum)
/// .asComponent(Button.class);`
/// ```
///
/// The above snippet wraps a new Button in a ComponentSelector, then uses the fluent API to apply several properties
/// to the button, before using `#asComponent()` to return the Button itself.
///
/// API Overview
///
/// ComponentSelector includes a few different types of methods:
///
///
/// - Wrapper methods for `Component`, `Container`, etc... to operate on all applicable components in the set.
///
/// - Component Tree Traversal Methods to return other sets of components based on the current set.  E.g. `#find(java.lang.String)`, `#getParent()`, `#getComponentAt(int)`,
/// `#closest(java.lang.String)`, `#nextSibling()`, `#prevSibling()`, `#parents(java.lang.String)`,
/// `#getComponentForm()`, and many more.
///
/// - Effects. E.g. `#fadeIn()`, `#fadeOut()`, `#slideDown()`, `#slideUp()`, `#animateLayout(int)`,
/// `#animateHierarchy(int)`, etc..
///
/// - Convenience methods to help with common tasks.  E.g. `#$(java.lang.Runnable)` as a short-hand for `Display#callSerially(java.lang.Runnable)`
///
/// - Methods to implement `java.util.Set` because ComponentSelector is a set.
///
/// Effects
///
/// The following is an example form that demonstrates the use of ComponentSelector to
/// easily create effects on components in a form.
///
/// ```java
///     private void showEffectsForm() {
///         Form f = new Form("Effects", new BorderLayout());
///         applyToolbar(f);
///         Button fadeInFadeOut = $(new Button("Fade"))
///                 .setIcon(FontImage.MATERIAL_BLUR_ON, 4)
///                 .addActionListener(e->{
///                     $(e).getParent().find(">*").fadeOutAndWait(1000).fadeInAndWait(1000);
///                 })
///                 .asComponent(Button.class);
///
///
///         Button slideUp = $(new Button("Slide Up"))
///                 .setIcon(FontImage.MATERIAL_EXPAND_LESS)
///                 .addActionListener(e->{
///                     $(e).getParent().find(">*").slideUpAndWait(1000).slideDownAndWait(1000);
///                 })
///                 .asComponent(Button.class);
///
///
///         Button replace = $(new Button("Replace Fade/Slide"))
///                 .setIcon(FontImage.MATERIAL_REDEEM)
///                 .addActionListener(e->{
///                     $(e).getParent()
///                             .find(">*")
///                             .replaceAndWait(c->{
///                                 return $(new Label("Replacement"))
///                                         .putClientProperty("origComponent", c)
///                                         .asComponent();
///                             }, CommonTransitions.createFade(1000))
///                             .replaceAndWait(c->{
///                                 Component orig = (Component)c.getClientProperty("origComponent");
///                                 if (orig != null) {
///                                     c.putClientProperty("origComponent", null);
///                                     return orig;
///                                 }
///                                 return c;
///
///                             }, CommonTransitions.createCover(CommonTransitions.SLIDE_HORIZONTAL, false, 1000));
///
///
///                 })
///                 .asComponent(Button.class);
///
///
///         Button replaceFlip = $(new Button("Replace Flip"))
///                 .setIcon(FontImage.MATERIAL_REDEEM)
///                 .addActionListener(e->{
///                     $(e).getParent()
///                             .find(">*")
///                             .replaceAndWait(c->{
///                                 return $(new Label("Replacement"))
///                                         .putClientProperty("origComponent", c)
///                                         .asComponent();
///                             },new FlipTransition(0xffffff, 1000))
///                             .replaceAndWait(c->{
///                                 Component orig = (Component)c.getClientProperty("origComponent");
///                                 if (orig != null) {
///                                     c.putClientProperty("origComponent", null);
///                                     return orig;
///                                 }
///                                 return c;
///
///                             },new FlipTransition(0xffffff, 1000));
///
///
///                 })
///                 .asComponent(Button.class);
///
///         Container root = GridLayout.encloseIn(3, fadeInFadeOut, slideUp, replace, replaceFlip);
///         f.addComponent(BorderLayout.CENTER, root);
///         f.show();
///     }
/// ```
///
/// Advanced Use of Tags
///
/// The following shows the use of tags to help with striping a table, and selecting rows
/// when clicked on.
///
/// ```java
///     private void showTableDemo() {
///         Form f = new Form("Table Demo", new BorderLayout());
///         applyToolbar(f);
///         CSVParser parser = new CSVParser();
///         String[][] data = null;
///         try {
///             data =   parser.parse(Display.getInstance().getResourceAsStream(null, "/sample-data.csv"));
///         } catch (Exception ex) {
///             ex.printStackTrace();
///         }
///         if (data== null) {
///             ToastBar.showMessage("Failed to parse sample data", FontImage.MATERIAL_INFO);
///             return;
///         }
///         int numRows = data.length;
///         int numCols = data[0].length;
///         TableLayout tl = new TableLayout(numRows, numCols);
///         Container table = new Container(tl);
///         int rowNum = 0;
///         int colNum = 0;
///         for (String[] row : data) {
///             colNum = 0;
///             for (String cell : row) {
///                 table.add(
///                         tl.createConstraint(rowNum, colNum),
///                         $(new Button(cell))
///                                 .setUIID("Label")
///                                 .addTags("cell", "row-"+rowNum, "col-"+colNum, rowNum%2==0 ? "even":"odd")
///                                 .putClientProperty("row", rowNum)
///                                 .putClientProperty("col", colNum)
///                                 .asComponent()
///
///                 );
///                 colNum++;
///             }
///             rowNum++;
///         }
///
///
///         $(".cell", table).setMargin(0).setPadding(0)
///                 .addActionListener(e->{
///                     // Action listener in each cell so that we can highlight the
///                     // selected row
///                     Component cell = (Component)e.getSource();
///                     int row = (int)cell.getClientProperty("row");
///                     int col = (int)cell.getClientProperty("col");
///
///                     // Restore the style of the previously selected row
///                     // and remove the selected-row tag
///                     $(e).getParent().find(">.selected-row")
///                             .each(c->{
///                                 // Restore the old style that we stored when we made
///                                 // the row selected originally (see below)
///                                 c.setUnselectedStyle((Style)c.getClientProperty("default-style"));
///                             })
///                             .removeTags("selected-row")
///                             .getParent()
///                             .repaint();
///
///                     // Now add the "selected-row" tag, and modify the styles
///                     $(e).getParent().find(">.row-"+row).addTags("selected-row")
///                             .each(c->{
///                                 // Store the existing style so that we can
///                                 // reapply it when the row becomes unselected
///                                 Style oldStyle = new Style(c.getStyle());
///                                 c.putClientProperty("default-style", oldStyle);
///                             })
///                             .setBgColor(0x89cff0)
///                             .setFgColor(0xffffff)
///                             .setBgTransparency(255)
///                             .getParent()
///                             .repaint();
///                 });
///
///         // Add striping to the table  (make the even rows gray)
///         $(".even", table)
///             .setBgColor(0xcccccc)
///             .setBgTransparency(255);
///
///
///
///
///         f.addComponent(BorderLayout.CENTER, $(BoxLayout.encloseY(table)).setScrollableY(true).asComponent());
///
///         f.show();
///     }
/// ```
///
/// See full Demo App in this  [Github Repo](https://github.com/shannah/cn1-component-selector-demo)
///
/// Modifying Style Properties
///
/// Modifying styles deserves special mention because components have multiple `Style` objects associated with them.
/// `Component#getStyle()` returns the style of the component in its current state.  `Component#getPressedStyle()`
/// gets the pressed style of the component, `Component#getSelectedStyle()` get its selected style, etc..
///
/// ComponentSelector wraps each of the getXXXStyle() methods of `Component` with corresponding methods
/// that return proxy styles for all components in the set.  `#getStyle()` returns a proxy `Style` that proxies
/// all of the styles returned from each of the `Component#getStyle()` methods in the set.  `#getPressedStyle()` returns
/// a proxy for all of the pressed styles, etc..
///
/// Example Modifying Text Color of All Buttons in a container when they are pressed only
///
/// ```java
/// `Style pressed = $("Button", myContainer).getPressedStyle();
/// pressed.setFgColor(0xff0000);`
/// ```
///
/// A slightly more elegant pattern would be to use the `#selectPressedStyle()` method to set the default
/// style for mutations to "pressed".  Then we could use the fluent API of ComponentSelector to chain multiple style
/// mutations.  E.g.:
///
/// ```java
/// `$("Button", myContainer)
///     .selectPressedStyle()
///     .setFgColor(0xffffff)
///     .setBgColor(0x0)
///     .setBgTransparency(255);`
/// ```
///
/// A short-hand for this would be to add the :pressed pseudo-class to the selector.  E.g.
///
/// ```java
/// `$("Button:pressed", myContainer)
///     .setFgColor(0xffffff)
///     .setBgColor(0x0)
///     .setBgTransparency(255);`
/// ```
///
/// The following style pseudo-classes are supported:
///
///
/// - :pressed - Same as calling `#selectPressedStyle()`
///
/// - :selected - Same as calling `#selectSelectedStyle()`
///
/// - :unselected - Same as calling `#selectUnselectedStyle()`
///
/// - :all - Same as calling `#selectAllStyles()`
///
/// - :* - Alias for :all
///
/// - :disabled - Same as calling `#selectDisabledStyle()`
///
/// You can chain calls to selectedXXXStyle(), enabling to chain together mutations of multiple different
/// style properties.  E.g  To change the pressed foreground color, and then change the selected foreground color, you could do:
///
/// ```java
/// `$("Button", myContainer)
///    .selectPressedStyle()
///    .setFgColor(0x0000ff)
///    .selectSelectedStyle()
///    .setFgColor(0x00ff00);`
/// ```
///
/// Filtering Sets
///
/// There are many ways to remove components from a set.  Obviously you can use the standard `java.util.Set` methods
/// to explicitly remove components from your set:
///
/// ```java
/// `ComponentSelector sel = $("Button").remove(myButton, true);
///     // The set of all buttons on the current form, except myButton`
/// ```
///
/// or
///
/// ```java
/// `ComponentSelector sel = $("Button").removeAll($(".some-tag"), true);
///    // The set of all buttons that do NOT contain the tag ".some-tag"`
/// ```
///
/// You could also use the `#filter(com.codename1.ui.ComponentSelector.Filter)` to explicitly
/// declare which elements should be kept, and which should be discarded:
///
/// ```java
/// `ComponentSelector sel = $("Button").filter(c->{
///     return c.isVisible();`);
///     // The set of all buttons that are currently visible.
/// }
/// ```
///
/// Tree Navigation
///
/// One powerful aspect of working with sets of components is that you can generate very specific
/// sets of components using very simple queries.  Consider the following queries:
///
///
/// - `$(myButton1, myButton2).getParent()` - The set of parents of myButton1 and myButton2.  If they have the
/// same parent, then this set will only contain a single element: the common parent container.   If they have different parents, then this
/// set will include both parent containers.
///
/// - `$(myButton).getParent().find(">TextField")` - The set of siblings of myButton that have UIID=TextField
///
/// - `$(myButton).closest(".some-tag")` - The set containing the "nearest" parent container of myButton that has
/// the tag ".some-tag".  If there are no matching components, then this will be an empty set.  This is formed by crawling up the tree
/// until it finds a matching component.  Works the same as jQuery's closest() method.
///
/// - `$(".my-tag").getComponentAt(4)` - The set of 5th child components of containers with tag ".my-tag".
///
/// @author shannah
///
/// #### See also
///
/// - [jQuery/CSS Style Selectors for Codename One](https://www.codenameone.com/blog/jquery-css-style-selectors-for-cn1.html)
public class ComponentSelector implements Iterable<Component>, Set<Component> {
    private static final String PROPERTY_TAG = "com.codename1.ui.ComponentSelector#tags";
    private static final int ALL_STYLES = 1;
    private static final int PRESSED_STYLE = 2;
    private static final int SELECTED_STYLE = 3;
    private static final int UNSELECTED_STYLE = 4;
    private static final int DISABLED_STYLE = 5;
    private final Set<Component> roots;
    private String name;
    private String uiid;
    private String[] tags;
    private String[] tagsNeedles;
    private String state;
    private ComponentSelector parent;
    private Set<ComponentSelector> aggregateSelectors;
    private Set<Component> results;
    private boolean childrenOnly = false;
    private Style currentStyle = null;
    private int currentStyleType = 0;


    /// Creates a component selector that wraps the provided components.  The provided
    /// components are treated as the "results" of this selector.  Not the roots.  However
    /// you can use `#find(java.lang.String)` to perform a query using this selector
    /// as the roots.
    ///
    /// #### Parameters
    ///
    /// - `cmps`: Components to add to this selector results.
    public ComponentSelector(Component... cmps) {
        this.roots = new LinkedHashSet<Component>();
        this.results = new LinkedHashSet<Component>();
        Collections.addAll(this.results, cmps);
    }

    /// Creates a component selector that wraps the provided components.  The provided
    /// components are treated as the "results" of this selector.  Not the roots.  However
    /// you can use `#find(java.lang.String)` to perform a query using this selector
    /// as the roots.
    ///
    /// #### Parameters
    ///
    /// - `cmps`: Components to add to this selector results.
    public ComponentSelector(Set<Component> cmps) {
        this.roots = new LinkedHashSet<Component>();
        this.results = new LinkedHashSet<Component>();
        this.results.addAll(cmps);

    }

    /// Creates a selector that will query the current form.  If there is no
    /// current form, then this selector will have no roots.
    ///
    /// Generally it is better to provide a root explicitly using {@link ComponentSelector#ComponentSelector(java.lang.String, com.codename1.ui.Component...)
    /// to ensure that the selector has a tree to walk down.
    ///
    /// #### Parameters
    ///
    /// - `selector`: The selector string.
    public ComponentSelector(String selector) {
        this.roots = new LinkedHashSet<Component>();
        Form f = Display.getInstance().getCurrent();
        if (f != null) {
            this.roots.add(f);
        } else {
            throw new RuntimeException("Attempt to create selector on current form, but there is no current form.  Best practice is to explicitly provide a root for your ComponentSelector.");
        }
        parse(selector);
    }

    /// Creates a selector with the provided roots.  This will only search through the subtrees
    /// of the provided roots to find results that match the provided selector string.
    ///
    /// #### Parameters
    ///
    /// - `selector`: The selector string
    ///
    /// - `roots`: The roots for this selector.
    public ComponentSelector(String selector, Component... roots) {
        this.roots = new LinkedHashSet<Component>();
        Collections.addAll(this.roots, roots);
        parse(selector);
    }

    /// Creates a selector with the provided roots.  This will only search through the subtrees
    /// of the provided roots to find results that match the provided selector string.
    ///
    /// #### Parameters
    ///
    /// - `selector`: The selector string
    ///
    /// - `roots`: The roots for this selector.
    public ComponentSelector(String selector, Collection<Component> roots) {
        this.roots = new LinkedHashSet<Component>();
        this.roots.addAll(roots);
        parse(selector);
    }

    /// Wraps provided components in a ComponentSelector set.
    ///
    /// #### Parameters
    ///
    /// - `cmps`: Components to be includd in the set.
    ///
    /// #### Returns
    ///
    /// ComponentSelector with specified components.
    ///
    /// #### Deprecated
    ///
    /// Use `#select(Component...)`.
    @Deprecated
    @SuppressWarnings("PMD.MethodNamingConventions")
    public static ComponentSelector $(Component... cmps) {
        return select(cmps);
    }

    /// Alias of `#$(com.codename1.ui.Component...)`
    ///
    /// #### Parameters
    ///
    /// - `cmps`
    public static ComponentSelector select(Component... cmps) {
        return new ComponentSelector(cmps);
    }

    /// Creates a ComponentInspector with the source component of the provided event.
    ///
    /// #### Parameters
    ///
    /// - `e`: The event whose source component is added to the set.
    ///
    /// #### Returns
    ///
    /// A ComponentSelector with a single component - the source of the event.
    ///
    /// #### Deprecated
    ///
    /// Use `#select(ActionEvent)`.
    @Deprecated
    @SuppressWarnings("PMD.MethodNamingConventions")
    public static ComponentSelector $(ActionEvent e) {
        return select(e);
    }

    /// Alias of `#$(com.codename1.ui.events.ActionEvent)`
    ///
    /// #### Parameters
    ///
    /// - `e`
    public static ComponentSelector select(ActionEvent e) {
        Object src = e.getSource();
        if (src == null) {
            return new ComponentSelector();
        } else if (src instanceof Component) {
            return new ComponentSelector((Component) src);
        }
        return new ComponentSelector();
    }

    /// Wraps `Display#callSerially(java.lang.Runnable)`
    ///
    /// #### Parameters
    ///
    /// - `r`
    ///
    /// #### Returns
    ///
    /// Empty ComponentSelector.
    ///
    /// #### Deprecated
    ///
    /// Use `#select(Runnable)`.
    @Deprecated
    @SuppressWarnings("PMD.MethodNamingConventions")
    public static ComponentSelector $(Runnable r) {
        return select(r);
    }

    /// Alias of `#$(java.lang.Runnable)`
    ///
    /// #### Parameters
    ///
    /// - `r`
    public static ComponentSelector select(Runnable r) {
        Display.getInstance().callSerially(r);
        return new ComponentSelector();
    }

    /// Creates a new ComponentSelector with the provided set of components.
    ///
    /// #### Parameters
    ///
    /// - `cmps`: The components to include in the set.
    ///
    /// #### Returns
    ///
    /// ComponentSelector with provided components.
    ///
    /// #### Deprecated
    ///
    /// Use `#select(Set)`.
    @Deprecated
    @SuppressWarnings("PMD.MethodNamingConventions")
    public static ComponentSelector $(Set<Component> cmps) {
        return select(cmps);
    }

    /// Alias of `#$(java.util.Set)`
    ///
    /// #### Parameters
    ///
    /// - `cmps`
    public static ComponentSelector select(Set<Component> cmps) {
        return new ComponentSelector(cmps);
    }

    /// Creates a new ComponentSelector with the components matched by the provided selector.  The current form
    /// is used as the root for searches.  Will throw a runtime exception if there is no current form.
    ///
    /// #### Parameters
    ///
    /// - `selector`: @param selector A selector string that defines which components to include in the
    /// set.
    ///
    /// #### Returns
    ///
    /// ComponentSelector with matching components.
    ///
    /// #### Deprecated
    ///
    /// Use `#select(String)`.
    @Deprecated
    @SuppressWarnings("PMD.MethodNamingConventions")
    public static ComponentSelector $(String selector) {
        return select(selector);
    }

    /// Alias of `#$(java.lang.String)`
    ///
    /// #### Parameters
    ///
    /// - `selector`
    public static ComponentSelector select(String selector) {
        return new ComponentSelector(selector);
    }

    /// Creates a ComponentSelector with the components matched by the provided selector in the provided
    /// roots' subtrees.
    ///
    /// #### Parameters
    ///
    /// - `selector`: Selector string to define which components will be included in the set.
    ///
    /// - `roots`: Roots for the selector to search.  Only components within the roots' subtrees will be included in the set.
    ///
    /// #### Returns
    ///
    /// ComponentSelector with matching components.
    ///
    /// #### Deprecated
    ///
    /// Use `Component...)`.
    @Deprecated
    @SuppressWarnings("PMD.MethodNamingConventions")
    public static ComponentSelector $(String selector, Component... roots) {
        return select(selector, roots);
    }

    /// Alias of `com.codename1.ui.Component...)`
    ///
    /// #### Parameters
    ///
    /// - `selector`
    ///
    /// - `roots`
    public static ComponentSelector select(String selector, Component... roots) {
        return new ComponentSelector(selector, roots);
    }

    /// Creates a ComponentSelector with the components matched by the provided selector in the provided
    /// roots' subtrees.
    ///
    /// #### Parameters
    ///
    /// - `selector`: Selector string to define which components will be included in the set.
    ///
    /// - `roots`: Roots for the selector to search.  Only components within the roots' subtrees will be included in the set.
    ///
    /// #### Returns
    ///
    /// ComponentSelector with matching components.
    ///
    /// #### Deprecated
    ///
    /// Use `Collection)`.
    @Deprecated
    @SuppressWarnings("PMD.MethodNamingConventions")
    public static ComponentSelector $(String selector, Collection<Component> roots) {
        return select(selector, roots);
    }

    /// Alias of `java.util.Collection)`
    ///
    /// #### Parameters
    ///
    /// - `selector`
    ///
    /// - `roots`
    public static ComponentSelector select(String selector, Collection<Component> roots) {
        return new ComponentSelector(selector, roots);
    }

    private static int dip2px(float dips) {
        return Display.getInstance().convertToPixels(dips);
    }

    private Style currentStyle() {
        if (currentStyle == null) {
            switch (currentStyleType) {
                case ALL_STYLES:
                    currentStyle = getAllStyles();
                    break;
                case PRESSED_STYLE:
                    currentStyle = getPressedStyle();
                    break;
                case SELECTED_STYLE:
                    currentStyle = getSelectedStyle();
                    break;
                case UNSELECTED_STYLE:
                    currentStyle = getUnselectedStyle();
                    break;
                case DISABLED_STYLE:
                    currentStyle = getDisabledStyle();
                    break;
                default:
                    currentStyle = getStyle();
                    break;


            }
        }
        return currentStyle;
    }

    private void setDirty() {
        currentStyle = null;
    }

    /// Applies the given callback to each component in the set.
    ///
    /// #### Parameters
    ///
    /// - `closure`: Callback which will be called once for each component in the set.
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public ComponentSelector each(ComponentClosure closure) {
        for (Component c : this) {
            closure.call(c);
        }
        return this;
    }

    /// Creates a new set based on the elements of the current set and a mapping function
    /// which defines the elements that should be in the new set.
    ///
    /// #### Parameters
    ///
    /// - `mapper`: @param mapper The mapper which will be called once for each element in the set.  The return value
    /// of the mapper function dictates which component should be included in the resulting set.
    ///
    /// #### Returns
    ///
    /// A new set of components.
    public ComponentSelector map(ComponentMapper mapper) {
        LinkedHashSet<Component> out = new LinkedHashSet<Component>();
        for (Component c : this) {
            Component res = mapper.map(c);
            if (res != null) {
                out.add(res);
            }
        }
        return new ComponentSelector(out);
    }

    /// Creates a new set of components formed by filtering the current set using a filter function.
    ///
    /// #### Parameters
    ///
    /// - `filter`: @param filter The filter function called for each element in the set.  If it returns true,
    /// then the element is included in the resulting set.  If false, it will not be included.
    ///
    /// #### Returns
    ///
    /// A new set with the results of the filter.
    public ComponentSelector filter(Filter filter) {
        LinkedHashSet<Component> out = new LinkedHashSet<Component>();
        for (Component c : this) {
            boolean res = filter.filter(c);
            if (res) {
                out.add(c);
            }
        }
        return new ComponentSelector(out);
    }

    /// Filters the current found set against the given selector.
    ///
    /// #### Parameters
    ///
    /// - `selector`: The selector to filter the found set on.
    ///
    /// #### Returns
    ///
    /// A new set of elements matching the selector.
    public ComponentSelector filter(String selector) {
        ComponentSelector matcher = new ComponentSelector(selector, new Label());
        LinkedHashSet<Component> matches = new LinkedHashSet<Component>();
        for (Component c : this) {
            if (matcher.match(c)) {
                matches.add(c);
            }
        }
        return matcher.addAll(matches, true);
    }

    /// Creates a new set of components consisting of all of the parents of components in this set.
    /// Only parent components matching the provided selector will be included in the set.
    ///
    /// #### Parameters
    ///
    /// - `selector`: Selector to filter the parent components.
    ///
    /// #### Returns
    ///
    /// New set with parents of elements in current set.
    public ComponentSelector parent(String selector) {
        ComponentSelector matcher = new ComponentSelector(selector, new Label());
        LinkedHashSet<Component> matches = new LinkedHashSet<Component>();
        for (Component c : this) {
            Component parent = c.getParent();
            if (parent != null && matcher.match(parent)) {
                matches.add(parent);
            }
        }
        return matcher.addAll(matches, true);
    }

    /// Creates new set of components consisting of all of the ancestors of components in this set which
    /// match the provided selector.
    ///
    /// #### Parameters
    ///
    /// - `selector`: The selector to filter the ancestors.
    ///
    /// #### Returns
    ///
    /// New set with ancestors of elements in current set.
    public ComponentSelector parents(String selector) {
        ComponentSelector matcher = new ComponentSelector(selector, new Label());
        LinkedHashSet<Component> matches = new LinkedHashSet<Component>();
        for (Component c : this) {
            Component parent = c.getParent();
            while (parent != null) {
                if (matcher.match(parent)) {
                    matches.add(parent);
                }
                parent = parent.getParent();
            }

        }
        return matcher.addAll(matches, true);
    }

    /// Creates a new set of components consistng of all "closest" ancestors of components
    /// in this set which match the provided selector.
    ///
    /// #### Parameters
    ///
    /// - `selector`: The selector to use to match the nearest ancestor.
    ///
    /// #### Returns
    ///
    /// New set with ancestors of components in current set.
    public ComponentSelector closest(String selector) {
        ComponentSelector matcher = new ComponentSelector(selector, new Label());
        LinkedHashSet<Component> matches = new LinkedHashSet<Component>();
        for (Component c : this) {
            Component parent = c.getParent();
            while (parent != null) {
                if (matcher.match(parent)) {
                    matches.add(parent);
                    break;
                }
                parent = parent.getParent();
            }

        }
        return matcher.addAll(matches, true);
    }

    /// Creates new set consisting of the first child of each component in the current set.
    ///
    /// #### Returns
    ///
    /// New set with first child of each component in current set.
    public ComponentSelector firstChild() {
        LinkedHashSet<Component> out = new LinkedHashSet<Component>();
        for (Component c : this) {
            if (c instanceof Container) {
                Container cnt = (Container) c;
                if (cnt.getComponentCount() > 0) {
                    out.add(cnt.getComponentAt(0));
                }
            }
        }
        return new ComponentSelector(out);
    }

    /// Creates new set consisting of the last child of each component in the current set.
    ///
    /// #### Returns
    ///
    /// New set with last child of each component in current set.
    public ComponentSelector lastChild() {
        LinkedHashSet<Component> out = new LinkedHashSet<Component>();
        for (Component c : this) {
            if (c instanceof Container) {
                Container cnt = (Container) c;
                if (cnt.getComponentCount() > 0) {
                    out.add(cnt.getComponentAt(cnt.getComponentCount() - 1));
                }
            }
        }
        return new ComponentSelector(out);
    }

    /// Creates set of "next" siblings of components in this set.
    ///
    /// #### Returns
    ///
    /// New ComponentSelector with next siblings of this set.
    public ComponentSelector nextSibling() {
        LinkedHashSet<Component> out = new LinkedHashSet<Component>();
        for (Component c : this) {
            Container parent = c.getParent();
            if (parent != null) {
                int index = parent.getComponentIndex(c);
                if (index < parent.getComponentCount() - 1) {
                    out.add(parent.getComponentAt(index + 1));
                }
            }
        }
        return new ComponentSelector(out);
    }

    /// Creates set of "previous" siblings of components in this set.
    ///
    /// #### Returns
    ///
    /// New ComponentSelector with previous siblings of this set.
    public ComponentSelector prevSibling() {
        LinkedHashSet<Component> out = new LinkedHashSet<Component>();
        for (Component c : this) {
            Container parent = c.getParent();
            if (parent != null) {
                int index = parent.getComponentIndex(c);
                if (index > 0) {
                    out.add(parent.getComponentAt(index - 1));
                }
            }
        }
        return new ComponentSelector(out);
    }

    /// Animates this set of components, replacing any modified style properties of the
    /// destination style to the components.
    ///
    /// #### Parameters
    ///
    /// - `destStyle`: The style to apply to the components via animation.
    ///
    /// - `duration`: The duration of the animation (ms)
    ///
    /// - `callback`: Callback to call after animation is complete.
    ///
    /// #### Returns
    ///
    /// Self for chaining
    ///
    /// #### See also
    ///
    /// - Component#createStyleAnimation(java.lang.String, int)
    public ComponentSelector animateStyle(Style destStyle, int duration, final SuccessCallback<ComponentSelector> callback) {
        ArrayList<ComponentAnimation> animations = new ArrayList<ComponentAnimation>();
        AnimationManager mgr = null;
        for (Component c : this) {
            AnimationManager cmgr = c.getAnimationManager();
            if (cmgr != null) {
                mgr = cmgr;
                Style sourceStyle = c.getUnselectedStyle();
                destStyle.merge(sourceStyle);
                animations.add(c.createStyleAnimation(sourceStyle, destStyle, duration, null));
            }
        }

        if (mgr != null) {
            ComponentAnimation anim = ComponentAnimation.compoundAnimation(animations.toArray(new ComponentAnimation[animations.size()]));

            mgr.addAnimation(anim, new Runnable() {
                @Override
                public void run() {
                    if (callback != null) {
                        callback.onSucess(ComponentSelector.this);
                    }
                }
            });

        }
        return this;
    }

    /// Fade in this set of components.  Prior to calling this, the component visibility
    /// should be set to "false".    This uses the default duration of 500ms.
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public ComponentSelector fadeIn() {
        return fadeIn(500);
    }

    /// Fade in this set of components.  Prior to calling this, the component visibility
    /// should be set to "false".
    ///
    /// #### Parameters
    ///
    /// - `duration`: The duration of the fade in.
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public ComponentSelector fadeIn(int duration) {
        return fadeIn(duration, null);
    }

    /// Fade in this set of components.  Prior to calling this, the component visibility should
    /// be set to "false".
    ///
    /// #### Parameters
    ///
    /// - `duration`: The duration of the fade in.
    ///
    /// - `callback`: Callback to run when animation completes.
    public ComponentSelector fadeIn(final int duration, final SuccessCallback<ComponentSelector> callback) {
        final String placeholderProperty = "com.codename1.ui.ComponentSelector#fadeOutPlaceholder";

        AnimationManager mgr = null;
        ArrayList<ComponentAnimation> animations1 = new ArrayList<ComponentAnimation>();
        final ArrayList<ComponentAnimation> animations2 = new ArrayList<ComponentAnimation>();
        final ArrayList<Component> animatingComponents = new ArrayList<Component>();
        for (Component c : this) {
            Container parent = c.getParent();
            if (parent != null) {

                AnimationManager cmgr = c.getAnimationManager();
                if (cmgr != null) {
                    mgr = cmgr;
                    Container placeholder = new Container();
                    //placeholder.getStyle().setBgColor(0xff0000);
                    //placeholder.getStyle().setBgTransparency(255);
                    //placeholder.setShowEvenIfBlank(true);
                    c.putClientProperty(placeholderProperty, placeholder);
                    Component.setSameHeight(placeholder, c);
                    Component.setSameWidth(placeholder, c);
                    $(placeholder)
                            .setMargin(c.getStyle().getMarginTop(), c.getStyle().getMarginRight(false), c.getStyle().getMarginBottom(), c.getStyle().getMarginLeft(false))
                            .setPadding(c.getStyle().getPaddingTop(), c.getStyle().getPaddingRight(false), c.getStyle().getPaddingBottom(), c.getStyle().getPaddingLeft(false));

                    //System.out.println("Placeholder height "+c.getHeight());
                    //parent.replace(c, placeholder, false);
                    //c.setHidden(false);
                    ComponentAnimation a = parent.createReplaceTransition(c, placeholder, CommonTransitions.createEmpty());
                    animations1.add(a);
                    animatingComponents.add(c);
                }
                //centerBackground.add(BorderLayout.CENTER, boxy);
            }
        }
        if (mgr != null) {
            mgr.addAnimation(ComponentAnimation.compoundAnimation(animations1.toArray(new ComponentAnimation[animations1.size()])), new Runnable() {

                @Override
                public void run() {
                    AnimationManager mgr = null;
                    for (final Component c : animatingComponents) {

                        Container placeholder = (Container) c.getClientProperty(placeholderProperty);
                        if (placeholder != null) {
                            //System.out.println("Placeholder height after replace "+(c.getHeight() + c.getStyle().getMarginBottom() + c.getStyle().getMarginTop()));
                            //System.out.println("Placeholder not null");
                            c.putClientProperty(placeholderProperty, null);
                            AnimationManager cmgr = placeholder.getAnimationManager();

                            if (cmgr != null) {
                                //System.out.println("Animation manager not null");
                                mgr = cmgr;
                                c.setVisible(true);
                                Container parent = placeholder.getParent();
                                if (parent != null) {
                                    //System.out.println("Parent not null");

                                    ComponentAnimation a = parent.createReplaceTransition(placeholder, c, CommonTransitions.createFade(duration));
                                    animations2.add(a);

                                }
                            }
                        }


                    }
                    final AnimationManager fmgr = mgr;
                    $(new Runnable() {
                        @Override
                        public void run() {
                            fmgr.addAnimation(ComponentAnimation.compoundAnimation(animations2.toArray(new ComponentAnimation[animations2.size()])), new Runnable() {
                                @Override
                                public void run() {
                                    if (callback != null) {
                                        callback.onSucess(ComponentSelector.this);
                                    }
                                }

                            });
                        }
                    });


                }

            });
        }

        return this;
    }

    /// Fades in this component and blocks until animation is complete.
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public ComponentSelector fadeInAndWait() {
        return fadeInAndWait(500);
    }

    /// Fades in this component and blocks until animation is complete.
    ///
    /// #### Parameters
    ///
    /// - `duration`: The duration of the animation.
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public ComponentSelector fadeInAndWait(final int duration) {
        final String placeholderProperty = "com.codename1.ui.ComponentSelector#fadeOutPlaceholder";

        AnimationManager mgr = null;
        ArrayList<ComponentAnimation> animations1 = new ArrayList<ComponentAnimation>();
        final ArrayList<ComponentAnimation> animations2 = new ArrayList<ComponentAnimation>();
        final ArrayList<Component> animatingComponents = new ArrayList<Component>();
        for (Component c : this) {
            Container parent = c.getParent();
            if (parent != null) {

                AnimationManager cmgr = c.getAnimationManager();
                if (cmgr != null) {
                    mgr = cmgr;
                    Container placeholder = new Container();
                    //placeholder.getStyle().setBgColor(0xff0000);
                    //placeholder.getStyle().setBgTransparency(255);
                    //placeholder.setShowEvenIfBlank(true);
                    c.putClientProperty(placeholderProperty, placeholder);
                    Component.setSameHeight(placeholder, c);
                    Component.setSameWidth(placeholder, c);
                    $(placeholder)
                            .setMargin(c.getStyle().getMarginTop(), c.getStyle().getMarginRight(false), c.getStyle().getMarginBottom(), c.getStyle().getMarginLeft(false))
                            .setPadding(c.getStyle().getPaddingTop(), c.getStyle().getPaddingRight(false), c.getStyle().getPaddingBottom(), c.getStyle().getPaddingLeft(false));

                    //System.out.println("Placeholder height "+c.getHeight());
                    //parent.replace(c, placeholder, false);
                    //c.setHidden(false);
                    ComponentAnimation a = parent.createReplaceTransition(c, placeholder, CommonTransitions.createEmpty());
                    animations1.add(a);
                    animatingComponents.add(c);
                }
                //centerBackground.add(BorderLayout.CENTER, boxy);
            }
        }
        if (mgr != null) {
            mgr.addAnimationAndBlock(ComponentAnimation.compoundAnimation(animations1.toArray(new ComponentAnimation[animations1.size()])));
            for (final Component c : animatingComponents) {

                Container placeholder = (Container) c.getClientProperty(placeholderProperty);
                if (placeholder != null) {
                    //System.out.println("Placeholder height after replace "+(c.getHeight() + c.getStyle().getMarginBottom() + c.getStyle().getMarginTop()));
                    //System.out.println("Placeholder not null");
                    c.putClientProperty(placeholderProperty, null);
                    AnimationManager cmgr = placeholder.getAnimationManager();

                    if (cmgr != null) {
                        //System.out.println("Animation manager not null");
                        mgr = cmgr;
                        c.setVisible(true);
                        Container parent = placeholder.getParent();
                        if (parent != null) {
                            //System.out.println("Parent not null");

                            ComponentAnimation a = parent.createReplaceTransition(placeholder, c, CommonTransitions.createFade(duration));
                            animations2.add(a);

                        }
                    }
                }


            }
            mgr.addAnimationAndBlock(
                    ComponentAnimation.compoundAnimation(animations2.toArray(new ComponentAnimation[animations2.size()]))
            );
        }

        return this;
    }

    /// Returns true if the first component in this set is visible.
    ///
    /// #### Returns
    ///
    /// True if first component in this set is visible.
    ///
    /// #### See also
    ///
    /// - Component#isVisible()
    public boolean isVisible() {
        Iterator<Component> iterator = iterator(); // PMD Fix: AvoidBranchingStatementAsLastInLoop
        if (iterator.hasNext()) {
            return iterator.next().isVisible();
        }
        return false;
    }

    /// Wrapper for `Component#setVisible(boolean)`
    ///
    /// #### Parameters
    ///
    /// - `visible`: True to make all components in result set visible.  False for hidden.
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public ComponentSelector setVisible(boolean visible) {
        for (Component c : this) {
            c.setVisible(visible);
        }
        return this;
    }

    /// Returns true if first component in this set is hidden.
    ///
    /// #### Returns
    ///
    /// True if first component in set is hidden.
    ///
    /// #### See also
    ///
    /// - Component#isHidden()
    public boolean isHidden() {
        Iterator<Component> iterator = iterator(); // PMD Fix: AvoidBranchingStatementAsLastInLoop
        if (iterator.hasNext()) {
            return iterator.next().isHidden();
        }
        return false;
    }

    /// Wraps `Component#setHidden(boolean)`
    ///
    /// #### Parameters
    ///
    /// - `b`
    public ComponentSelector setHidden(boolean b) {
        for (Component c : this) {
            c.setHidden(b);
        }
        return this;
    }

    /// Fades out components in this set.  Uses default duration of 500ms.
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public ComponentSelector fadeOut() {
        return fadeOut(500);
    }

    /// Fades out components in this set.
    ///
    /// #### Parameters
    ///
    /// - `duration`: Duration of animation.
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public ComponentSelector fadeOut(int duration) {
        return fadeOut(duration, null);
    }

    /// Fades out components in this set.
    ///
    /// #### Parameters
    ///
    /// - `duration`: Duration of animation.
    ///
    /// - `callback`: Callback to run when animation completes.
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public ComponentSelector fadeOut(int duration, final SuccessCallback<ComponentSelector> callback) {
        final String placeholderProperty = "com.codename1.ui.ComponentSelector#fadeOutPlaceholder";

        AnimationManager mgr = null;
        ArrayList<ComponentAnimation> animations = new ArrayList<ComponentAnimation>();
        final ArrayList<Component> animatingComponents = new ArrayList<Component>();
        for (Component c : this) {
            Container parent = c.getParent();
            if (parent != null) {

                AnimationManager cmgr = c.getAnimationManager();
                if (cmgr != null) {
                    mgr = cmgr;
                    Container placeholder = new Container();
                    //placeholder.setShowEvenIfBlank(true);
                    c.putClientProperty(placeholderProperty, placeholder);
                    Component.setSameHeight(placeholder, c);
                    Component.setSameWidth(placeholder, c);
                    $(placeholder)
                            .setMargin(c.getStyle().getMarginTop(), c.getStyle().getMarginRight(false), c.getStyle().getMarginBottom(), c.getStyle().getMarginLeft(false))
                            .setPadding(c.getStyle().getPaddingTop(), c.getStyle().getPaddingRight(false), c.getStyle().getPaddingBottom(), c.getStyle().getPaddingLeft(false));

                    ComponentAnimation a = parent.createReplaceTransition(c, placeholder, CommonTransitions.createFade(duration));
                    animations.add(a);
                    animatingComponents.add(c);
                }
                //centerBackground.add(BorderLayout.CENTER, boxy);
            }
        }
        if (mgr != null) {
            mgr.addAnimation(ComponentAnimation.compoundAnimation(animations.toArray(new ComponentAnimation[animations.size()])), new Runnable() {

                @Override
                public void run() {
                    for (final Component c : animatingComponents) {
                        //c.setHidden(true);
                        c.setVisible(false);
                        final Container placeholder = (Container) c.getClientProperty(placeholderProperty);
                        c.putClientProperty(placeholderProperty, null);
                        if (placeholder != null) {
                            Container parent = placeholder.getParent();
                            /*
                            if (parent == null) {
                                System.out.println("Deferring replace back");
                                $(new Runnable() {
                                    public void run() {
                                        Container parent = placeholder.getParent();
                                        if (parent != null) {
                                            System.out.println("Found parent after deferral");
                                            parent.replace(placeholder, c, CommonTransitions.createEmpty());
                                        }
                                    }
                                });
                            } else {
                            */
                            if (parent != null) {

                                parent.replace(placeholder, c, CommonTransitions.createEmpty());
                            }
                            //}
                        }
                    }
                    if (callback != null) {
                        callback.onSucess(ComponentSelector.this);
                    }

                }

            });
        }

        return this;
    }

    /// Hide the matched components with a sliding motion.
    ///
    /// #### Parameters
    ///
    /// - `duration`: Duration of animation
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public ComponentSelector slideUp(int duration) {
        return slideUp(duration, null);
    }

    /// Hide the matched elements with a sliding motion.
    ///
    /// #### Parameters
    ///
    /// - `duration`: Duration of animation.
    ///
    /// - `callback`: Callback to run when animation completes
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public ComponentSelector slideUp(int duration, final SuccessCallback<ComponentSelector> callback) {
        final ArrayList<Component> animatedComponents = new ArrayList<Component>();
        for (Component c : this) {
            c.setHeight(0);
            animatedComponents.add(c);
        }
        getParent().animateUnlayout(duration, 255, new SuccessCallback<ComponentSelector>() {

            @Override
            public void onSucess(ComponentSelector value) {
                for (Component c : animatedComponents) {
                    c.setVisible(false);
                }
                getParent().revalidate();
                if (callback != null) {
                    callback.onSucess(ComponentSelector.this);
                }
            }

        });
        return this;
    }

    /// Hide the matched elements with a sliding motion.  Blocks until animation is complete.
    ///
    /// #### Parameters
    ///
    /// - `duration`: Duration of animation.
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public ComponentSelector slideUpAndWait(int duration) {
        for (Component c : this) {
            c.setHeight(0);
        }
        getParent().animateUnlayoutAndWait(duration, 255);
        return this;
    }

    /// Display the matched elements with a sliding motion.  Uses default duration of 500ms
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public ComponentSelector slideDown() {
        return slideDown(500);
    }

    /// Hide the matched elements with a sliding motion.  Uses default duration of 500ms
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public ComponentSelector slideUp() {
        return slideUp(500);
    }

    /// Display the matched elements with a sliding motion.
    ///
    /// #### Parameters
    ///
    /// - `duration`: Duration of animation.
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public ComponentSelector slideDown(int duration) {
        return slideDown(duration, null);
    }

    /// Display the matched elements with a sliding motion.
    ///
    /// #### Parameters
    ///
    /// - `duration`: Duration of animation.
    ///
    /// - `callback`: Callback to run when animation completes.
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public ComponentSelector slideDown(final int duration, final SuccessCallback<ComponentSelector> callback) {
        for (Component c : this) {
            c.setHeight(0);
            c.setVisible(true);
        }
        getParent().animateLayout(duration, callback);

        return this;
    }

    /// Display the matched elements with a sliding motion. Blocks until animation is complete.
    ///
    /// #### Parameters
    ///
    /// - `duration`: Duration of animation.
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public ComponentSelector slideDownAndWait(final int duration) {
        for (Component c : this) {
            c.setHeight(0);
            c.setVisible(true);
        }
        getParent().animateLayoutAndWait(duration);

        return this;
    }

    /// Hide the matched elements by fading them to transparent. Blocks thread until animation is complete.
    ///
    /// #### Parameters
    ///
    /// - `duration`: Duration of animation.
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public ComponentSelector fadeOutAndWait(int duration) {
        final String placeholderProperty = "com.codename1.ui.ComponentSelector#fadeOutPlaceholder";

        AnimationManager mgr = null;
        ArrayList<ComponentAnimation> animations = new ArrayList<ComponentAnimation>();
        final ArrayList<Component> animatingComponents = new ArrayList<Component>();
        for (Component c : this) {
            Container parent = c.getParent();
            if (parent != null) {

                AnimationManager cmgr = c.getAnimationManager();
                if (cmgr != null) {
                    mgr = cmgr;
                    Container placeholder = new Container();
                    //placeholder.setShowEvenIfBlank(true);
                    c.putClientProperty(placeholderProperty, placeholder);
                    Component.setSameHeight(placeholder, c);
                    Component.setSameWidth(placeholder, c);
                    $(placeholder)
                            .setMargin(c.getStyle().getMarginTop(), c.getStyle().getMarginRight(false), c.getStyle().getMarginBottom(), c.getStyle().getMarginLeft(false))
                            .setPadding(c.getStyle().getPaddingTop(), c.getStyle().getPaddingRight(false), c.getStyle().getPaddingBottom(), c.getStyle().getPaddingLeft(false));

                    ComponentAnimation a = parent.createReplaceTransition(c, placeholder, CommonTransitions.createFade(duration));
                    animations.add(a);
                    animatingComponents.add(c);
                }
                //centerBackground.add(BorderLayout.CENTER, boxy);
            }
        }
        if (mgr != null) {
            mgr.addAnimationAndBlock(ComponentAnimation.compoundAnimation(animations.toArray(new ComponentAnimation[animations.size()])));

            for (final Component c : animatingComponents) {

                c.setVisible(false);
                final Container placeholder = (Container) c.getClientProperty(placeholderProperty);
                c.putClientProperty(placeholderProperty, null);
                if (placeholder != null) {
                    Container parent = placeholder.getParent();

                    if (parent != null) {

                        parent.replace(placeholder, c, CommonTransitions.createEmpty());
                    }

                }
            }


        }

        return this;
    }

    /// Replaces the matched components within respective parents with replacements defined by the provided mapper.  Replacements
    /// are replaced in the UI itself  (i.e. `c.getParent().replace(c, replacement)`) with an empty
    /// transition.
    ///
    /// #### Parameters
    ///
    /// - `mapper`: @param mapper Mapper that defines the replacements for each component in the set.  If the mapper returns
    /// the input component, then no change is made for that component.  A null return value cause the component
    /// to be removed from its parent.  Returning a Component results in that component replacing the original component
    /// within its parent.
    ///
    /// #### Returns
    ///
    /// A new ComponentSelector with the replacement components.
    public ComponentSelector replace(ComponentMapper mapper) {
        return replace(mapper, CommonTransitions.createEmpty());
    }

    /// Replaces the matched components within respective parents with replacements defined by the provided mapper.  Replacements
    /// are replaced in the UI itself  (i.e. `c.getParent().replace(c, replacement)`) with the provided transition.
    ///
    /// #### Parameters
    ///
    /// - `mapper`: @param mapper Mapper that defines the replacements for each component in the set.  If the mapper returns
    /// the input component, then no change is made for that component.  A null return value cause the component
    /// to be removed from its parent.  Returning a Component results in that component replacing the original component
    /// within its parent.
    ///
    /// - `t`: Transition to use for replacements.
    ///
    /// #### Returns
    ///
    /// A new ComponentSelector with the replacement components.
    public ComponentSelector replace(ComponentMapper mapper, Transition t) {
        ArrayList<ComponentAnimation> animations = new ArrayList<ComponentAnimation>();
        LinkedHashSet<Component> replacements = new LinkedHashSet<Component>();
        for (Component c : this) {
            Container parent = c.getParent();
            Component replacement = mapper.map(c);
            if (parent != null) {
                if (replacement != c) { //NOPMD CompareObjectsWithEquals
                    if (replacement != null) {
                        animations.add(parent.createReplaceTransition(c, replacement, t.copy(false)));
                    } else {
                        c.remove();
                    }
                }
            }
            if (replacement != null) {
                replacements.add(replacement);
            }
        }
        AnimationManager mgr = getAnimationManager();
        if (mgr != null && !animations.isEmpty()) {
            mgr.addAnimation(ComponentAnimation.compoundAnimation(animations.toArray(new ComponentAnimation[animations.size()])));
        }
        return new ComponentSelector(replacements);

    }

    /// Replaces the matched components within respective parents with replacements defined by the provided mapper.  Replacements
    /// are replaced in the UI itself  (i.e. `c.getParent().replace(c, replacement)`) with the provided transition.
    /// Blocks the thread until the transition animation is complete.
    ///
    /// #### Parameters
    ///
    /// - `mapper`: @param mapper Mapper that defines the replacements for each component in the set.  If the mapper returns
    /// the input component, then no change is made for that component.  A null return value cause the component
    /// to be removed from its parent.  Returning a Component results in that component replacing the original component
    /// within its parent.
    ///
    /// - `t`
    ///
    /// #### Returns
    ///
    /// A new ComponentSelector with the replacement components.
    public ComponentSelector replaceAndWait(ComponentMapper mapper, Transition t) {
        ArrayList<ComponentAnimation> animations = new ArrayList<ComponentAnimation>();
        LinkedHashSet<Component> replacements = new LinkedHashSet<Component>();
        for (Component c : this) {
            Container parent = c.getParent();
            Component replacement = mapper.map(c);
            if (parent != null) {
                if (replacement != c) { //NOPMD CompareObjectsWithEquals
                    if (replacement != null) {
                        animations.add(parent.createReplaceTransition(c, replacement, t.copy(false)));
                    } else {
                        c.remove();
                    }
                }
            }
            if (replacement != null) {
                replacements.add(replacement);
            }
        }
        AnimationManager mgr = getAnimationManager();
        if (mgr != null && !animations.isEmpty()) {
            mgr.addAnimationAndBlock(ComponentAnimation.compoundAnimation(animations.toArray(new ComponentAnimation[animations.size()])));
        }

        return new ComponentSelector(replacements);
    }

    /// Uses the results of this selector as the roots to create a new selector with
    /// the provided selector string.
    ///
    /// #### Parameters
    ///
    /// - `selector`: The selector string.
    ///
    /// #### Returns
    ///
    /// A new ComponentSelector with the results of the query.
    public ComponentSelector find(String selector) {
        return new ComponentSelector(selector, resultsImpl());
    }

    private void parse(String selector) {
        selector = selector.trim();

        if (selector.indexOf(",") != -1) {
            // this is an aggregate selector
            String[] parts = Util.split(selector, ",");

            aggregateSelectors = new LinkedHashSet<ComponentSelector>();
            for (String part : parts) {
                part = part.trim();
                if (part.length() == 0) {
                    continue;
                }
                aggregateSelectors.add(new ComponentSelector(part, roots));
            }
            return;

        }

        String[] parts = Util.split(selector, " ");
        int len = parts.length;
        if (len > 1) {
            StringBuilder parentSelector = new StringBuilder();
            for (int i = 0; i < len; i++) {
                if (">".equals(parts[i])) {
                    if (i < len - 1) {
                        parts[i] = ">" + parts[i + 1].trim();
                        for (int j = i + 1; j < len - 1; j++) {
                            parts[j] = parts[j + 1];
                        }
                        len--;
                        parts[len] = null;
                    } else {
                        throw new IllegalArgumentException("Failed to parse selector.  Selector cannot end with '>'");
                    }
                }
                if (i > 0 && i < len - 1) {
                    parentSelector.append(" ");
                }
                if (i < len - 1) {
                    parentSelector.append(parts[i]);
                }
                if (i == len - 1) {
                    selector = parts[i];
                }
            }
            if (parentSelector.length() > 0) {
                parent = new ComponentSelector(parentSelector.toString(), roots);
                roots.clear();
            }
        }


        if (selector.indexOf(">") == 0) {
            childrenOnly = true;
            selector = selector.substring(1).trim();
        }

        if (selector.indexOf(",") != -1) {
            throw new IllegalArgumentException("Invalid character in selector " + selector);
        } else {
            ComponentSelector out = this;
            selector = selector.trim();


            int pos = selector.indexOf(":");
            if (pos != -1) {
                out.state = selector.substring(pos + 1);
                selector = selector.substring(0, pos);
            }
            pos = selector.indexOf(".");
            if (pos != -1) {
                out.tags = Util.split(selector.substring(pos + 1), ".");
                len = out.tags.length;
                out.tagsNeedles = new String[len];
                String[] needles = out.tagsNeedles;
                String[] tags = out.tags;
                for (int i = 0; i < len; i++) {
                    // Will make it easier to match against components' tags.
                    needles[i] = " " + tags[i] + " ";
                }
                selector = selector.substring(0, pos);
            }
            pos = selector.indexOf("#");
            if (pos >= 0) {
                out.name = selector.substring(pos + 1);
                selector = selector.substring(0, pos);
            }
            if (selector.length() > 0 && !"*".equals(selector)) {
                out.uiid = selector;
            }
            if (state != null) {
                if ("pressed".equals(state)) {
                    currentStyleType = PRESSED_STYLE;
                } else if ("selected".equals(state)) {
                    currentStyleType = SELECTED_STYLE;
                } else if ("unselected".equals(state)) {
                    currentStyleType = UNSELECTED_STYLE;
                } else if ("disabled".equals(state)) {
                    currentStyleType = DISABLED_STYLE;
                } else if ("all".equals(state) || "*".equals(state)) {
                    currentStyleType = ALL_STYLES;
                }
            }
            //return out;

        }

    }

    private boolean match(Component c) {


        if (name != null && !name.equals(c.getName())) {
            return false;
        }
        if (uiid != null && !uiid.equals(c.getUIID())) {
            return false;
        }
        if (tags != null) {
            String ctags = (String) c.getClientProperty(PROPERTY_TAG);

            if (ctags != null) {
                for (String ctag : tagsNeedles) {
                    if (ctags.indexOf(ctag) == -1) {
                        return false;
                    }
                }
            } else {
                return false;
            }
        }

        return true;


    }

    /// Returns the results of this selector.
    @Override
    public Iterator<Component> iterator() {
        return resultsImpl().iterator();
    }

    private Set<Component> resultsImpl() {
        if (results == null) {
            results = new LinkedHashSet<Component>();

            if (aggregateSelectors != null) {
                for (ComponentSelector sel : aggregateSelectors) {
                    results.addAll(sel.resultsImpl());
                }
                return results;
            }

            if (parent != null) {
                roots.clear();
                roots.addAll(parent.resultsImpl());
            }

            for (Component root : roots) {
                if (childrenOnly) {
                    if (root instanceof Container) {
                        Container cnt = (Container) root;
                        for (Component child : cnt) {
                            if (match(child)) {
                                results.add(child);
                            }
                        }
                    }
                } else {
                    if (root instanceof Container) {
                        Container cnt = (Container) root;
                        for (int iter = 0; iter < cnt.getComponentCount(); iter++) {
                            Component child = cnt.getComponentAt(iter);
                            resultsImpl(results, child);
                        }
                    }
                }
            }
        }
        return results;
    }


    private Set<Component> resultsImpl(Set<Component> out, Component root) {
        if (match(root)) {
            out.add(root);
        }
        if (root instanceof Container) {
            Container cnt = (Container) root;
            for (int iter = 0; iter < cnt.getComponentCount(); iter++) {
                Component child = cnt.getComponentAt(iter);
                resultsImpl(out, child);
            }
        }
        return out;
    }

    /// Gets a proxy style that wraps the result of `Component#getStyle()` of each component in set.
    public Style getStyle() {
        HashSet<Style> out = new HashSet<Style>();
        for (Component c : this) {
            out.add(c.getStyle());
        }
        return Style.createProxyStyle(out.toArray(new Style[out.size()]));
    }

    /// Gets a style object for the given component that can be used to modify the
    /// component's styles.  This takes into account any state-pseudo classes that
    /// were used to create this selector so that the style returned will be appropriate.
    ///
    /// E.g.
    /// ```java
    /// `ComponentSelector sel = new ComponentSelector("Button:pressed");
    /// Style style = sel.getStyle(sel.get(0));
    ///     // This should be equivalent to sel.get(0).getPressedStyle()
    ///
    /// sel = new ComponentSelector("Button");
    /// style = sel.getStyle(sel.get(0));
    ///     // This should be equivalent to sel.get(0).getAllStyles()
    ///
    /// sel = new ComponentSelector("Button:pressed, Button:selected");
    /// style = sel.getStyle(sel.get(0));
    ///     // This should be same as
    ///     // Style.createProxyStyle(sel.get(0).getPressedStyle(), sel.get(0).getSelectedStyle())`
    /// ```
    ///
    /// #### Parameters
    ///
    /// - `component`: The component whose style object we wish to obtain.
    ///
    /// #### Returns
    ///
    /// A style object that will allow us to modify the style of the given component.
    public Style getStyle(Component component) {
        switch (currentStyleType) {
            case SELECTED_STYLE:
                return component.getSelectedStyle();
            case UNSELECTED_STYLE:
                return component.getUnselectedStyle();
            case DISABLED_STYLE:
                return component.getDisabledStyle();
            case PRESSED_STYLE:
                return component.getPressedStyle();
            case ALL_STYLES:
                return component.getAllStyles();
            default:
                return component.getStyle();
        }

    }

    /// Returns a proxy style for all of the selected styles of the components in this set.
    ///
    /// #### Returns
    ///
    /// @return Proxy style to easily change properties of the selected styles of all
    /// components in this set.
    public Style getSelectedStyle() {
        ArrayList<Style> styles = new ArrayList<Style>();
        for (Component c : this) {
            styles.add(c.getSelectedStyle());
        }
        return Style.createProxyStyle(styles.toArray(new Style[styles.size()]));
    }

    /// Sets selected style of all components in found set.
    /// Wraps `Component#setSelectedStyle(com.codename1.ui.plaf.Style)`
    ///
    /// #### Parameters
    ///
    /// - `style`
    public ComponentSelector setSelectedStyle(Style style) {
        for (Component c : this) {
            c.setSelectedStyle(style);
        }
        return this;
    }

    /// Sets the current style to the selected style.  Style mutation methods called after
    /// calling this method will modify the components' "selected style".
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    ///
    /// #### See also
    ///
    /// - Component#getSelectedStyle()
    public ComponentSelector selectSelectedStyle() {
        currentStyle = getSelectedStyle();
        currentStyleType = SELECTED_STYLE;
        return this;
    }

    /// Sets the current style to the unselected style.  Style mutation methods called after
    /// calling this method will modify the components' "unselected style".
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    ///
    /// #### See also
    ///
    /// - Component#getUnselectedStyle()
    public ComponentSelector selectUnselectedStyle() {
        currentStyle = getUnselectedStyle();
        currentStyleType = UNSELECTED_STYLE;
        return this;
    }

    /// Sets the current style to the pressed style.  Style mutation methods called after
    /// calling this method will modify the components' "pressed style".
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    ///
    /// #### See also
    ///
    /// - Component#getPressedStyle()
    public ComponentSelector selectPressedStyle() {
        currentStyle = getPressedStyle();
        currentStyleType = PRESSED_STYLE;
        return this;
    }

    /// Sets the current style to the disabled style.  Style mutation methods called after
    /// calling this method will modify the components' "disabled style".
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    ///
    /// #### See also
    ///
    /// - Component#getDisabledStyle()
    public ComponentSelector selectDisabledStyle() {
        currentStyle = getDisabledStyle();
        currentStyleType = DISABLED_STYLE;
        return this;
    }

    /// Sets the current style to each component's ALL STYLES proxy style.  Style mutation methods called after
    /// calling this method will modify the components' "all styles" proxy style.
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    ///
    /// #### See also
    ///
    /// - Component#getAllStyles()
    public ComponentSelector selectAllStyles() {
        currentStyle = getAllStyles();
        currentStyleType = ALL_STYLES;
        return this;
    }

    /// Returns a proxy style for all of the unselected styles of the components in this set.
    ///
    /// #### Returns
    ///
    /// @return Proxy style to easily change properties of the unselected styles of all
    /// components in this set.
    public Style getUnselectedStyle() {
        ArrayList<Style> styles = new ArrayList<Style>();
        for (Component c : this) {
            styles.add(c.getUnselectedStyle());
        }
        return Style.createProxyStyle(styles.toArray(new Style[styles.size()]));
    }

    /// Sets unselected style of all components in found set.
    /// Wraps `Component#setUnselectedStyle(com.codename1.ui.plaf.Style)`
    ///
    /// #### Parameters
    ///
    /// - `style`
    public ComponentSelector setUnselectedStyle(Style style) {
        for (Component c : this) {
            c.setUnselectedStyle(style);
        }
        return this;
    }

    /// Returns a proxy style for all of the pressed styles of the components in this set.
    ///
    /// #### Returns
    ///
    /// @return Proxy style to easily change properties of the pressed styles of all
    /// components in this set.
    public Style getPressedStyle() {
        ArrayList<Style> styles = new ArrayList<Style>();
        for (Component c : this) {
            styles.add(c.getPressedStyle());
        }
        return Style.createProxyStyle(styles.toArray(new Style[styles.size()]));
    }

    /// Sets pressed style of all components in found set.
    /// Wraps `Component#setPressedStyle(com.codename1.ui.plaf.Style)`
    ///
    /// #### Parameters
    ///
    /// - `style`
    public ComponentSelector setPressedStyle(Style style) {
        for (Component c : this) {
            c.setPressedStyle(style);
        }
        return this;
    }

    /// Returns a proxy style for all of the disabled styles of the components in this set.
    ///
    /// #### Returns
    ///
    /// @return Proxy style to easily change properties of the disabled styles of all
    /// components in this set.
    public Style getDisabledStyle() {
        ArrayList<Style> styles = new ArrayList<Style>();
        for (Component c : this) {
            styles.add(c.getDisabledStyle());
        }
        return Style.createProxyStyle(styles.toArray(new Style[styles.size()]));
    }

    /// Sets disabled style of all components in found set.
    /// Wraps `Component#setDisabledStyle(com.codename1.ui.plaf.Style)`
    ///
    /// #### Parameters
    ///
    /// - `style`
    public ComponentSelector setDisabledStyle(Style style) {
        for (Component c : this) {
            c.setDisabledStyle(style);
        }
        return this;
    }

    /// Returns a proxy style for all of the "all" styles of the components in this set.
    ///
    /// #### Returns
    ///
    /// @return Proxy style to easily change properties of the "all" styles of all
    /// components in this set.
    public Style getAllStyles() {
        ArrayList<Style> styles = new ArrayList<Style>();
        for (Component c : this) {
            styles.add(c.getAllStyles());
        }
        return Style.createProxyStyle(styles.toArray(new Style[styles.size()]));
    }

    /// Returns number of results found.
    @Override
    public int size() {
        return resultsImpl().size();
    }

    /// #### Returns
    ///
    /// True if there were no results.
    @Override
    public boolean isEmpty() {
        return resultsImpl().isEmpty();
    }

    /// Checks if an object is contained in result set.
    ///
    /// #### Parameters
    ///
    /// - `o`
    @Override
    public boolean contains(Object o) {
        return resultsImpl().contains(o);
    }

    /// Returns results as an array.
    @Override
    public Object[] toArray() {
        return resultsImpl().toArray();
    }

    /// Returns results as an array.
    ///
    /// @param
    ///
    /// #### Parameters
    ///
    /// - `a`
    @Override
    public <T> T[] toArray(T[] a) {
        return resultsImpl().toArray(a);
    }

    /// Explicitly adds a component to the result set.
    ///
    /// #### Parameters
    ///
    /// - `e`
    ///
    /// #### Returns
    ///
    /// True on success
    @Override
    public boolean add(Component e) {
        setDirty();
        return resultsImpl().add(e);
    }

    /// Appends a child component to the first container in this set.  Same as calling
    /// `Container#add(com.codename1.ui.Component)` padding child on first container
    /// in this set.
    ///
    /// #### Parameters
    ///
    /// - `child`: Component to add to container.
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public ComponentSelector append(Component child) {
        for (Component c : this) {
            if (c instanceof Container) {
                Container cnt = (Container) c;
                cnt.add(child);
                return this;
            }
        }
        return this;
    }

    /// Appends a child component to the first container in this set.  Same as calling
    /// `com.codename1.ui.Component)` padding child on first container
    /// in this set.
    ///
    /// #### Parameters
    ///
    /// - `constraint`
    ///
    /// - `child`
    public ComponentSelector append(Object constraint, Component child) {
        for (Component c : this) {
            if (c instanceof Container) {
                Container cnt = (Container) c;
                cnt.add(constraint, child);
                return this;
            }
        }
        return this;
    }

    /// Append a child element to each container in this set.  The mapper callback
    /// will receive a Container as input (that is the parent to be added to), and should return
    /// a Component that is to be added to it.  If the mapper returns null, then nothing is
    /// added to that container.
    ///
    /// #### Parameters
    ///
    /// - `mapper`
    public ComponentSelector append(ComponentMapper mapper) {
        for (Component c : this) {
            if (c instanceof Container) {
                Component child = mapper.map(c);
                if (child != null) {
                    ((Container) c).add(child);
                }
            }
        }
        return this;
    }

    /// Append a child element to each container in this set.  The mapper callback
    /// will receive a Container as input (that is the parent to be added to), and should return
    /// a Component that is to be added to it.  If the mapper returns null, then nothing is
    /// added to that container.
    ///
    /// #### Parameters
    ///
    /// - `constraint`
    ///
    /// - `mapper`
    public ComponentSelector append(Object constraint, ComponentMapper mapper) {
        for (Component c : this) {
            if (c instanceof Container) {
                Component child = mapper.map(c);
                ((Container) c).add(constraint, child);
            }
        }
        return this;
    }

    /// Fluent API wrapper for `#add(com.codename1.ui.Component)`
    ///
    /// #### Parameters
    ///
    /// - `e`: Component to add to set.
    ///
    /// - `chain`: Dummy argument so that this version would have a different signature than `Set#add(java.lang.Object)`
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public ComponentSelector add(Component e, boolean chain) {
        add(e);
        return this;
    }

    /// Explicitly removes a component from the result set.
    ///
    /// #### Parameters
    ///
    /// - `o`
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    @Override
    public boolean remove(Object o) {
        setDirty();
        return resultsImpl().remove(o);
    }

    /// Fluent API wrapper for `#remove(java.lang.Object)`.
    ///
    /// #### Parameters
    ///
    /// - `o`: The component to remove from set.
    ///
    /// - `chain`: Dummy argument so that this version would have a different signature than `Set#remove(java.lang.Object)`
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public ComponentSelector remove(Object o, boolean chain) {
        remove(o);
        return this;
    }

    /// Checks if the result set contains all of the components found in the provided
    /// collection.
    ///
    /// #### Parameters
    ///
    /// - `c`
    @Override
    public boolean containsAll(Collection<?> c) {
        return resultsImpl().containsAll(c);
    }

    /// Adds all components in the given collection to the result set.
    ///
    /// #### Parameters
    ///
    /// - `c`
    @Override
    public boolean addAll(Collection<? extends Component> c) {
        setDirty();
        return resultsImpl().addAll(c);
    }

    /// Fluent API wrapper for `#addAll(java.util.Collection)`.
    ///
    /// #### Parameters
    ///
    /// - `c`: The set of components to add to this set.
    ///
    /// - `chain`: Dummy argument so that this version would have a different signature than `#addAll(java.util.Collection)`
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public ComponentSelector addAll(Collection<? extends Component> c, boolean chain) {
        addAll(c);
        return this;
    }

    /// Returns the first component in this set.  This is useful for single component sets (e.g. `$(new Label()).setFgColor(0xff0000).asComponent()`).
    ///
    /// #### Returns
    ///
    /// The first component in this set.
    public Component asComponent() {
        Iterator<Component> iterator = iterator(); // PMD Fix: AvoidBranchingStatementAsLastInLoop
        if (iterator.hasNext()) {
            return iterator.next();
        }
        return null;
    }

    /// Returns the first component in this set.  This is useful for single component sets (e.g. `$(new Label()).setFgColor(0xff0000).asComponent(Label.class)`).
    ///
    /// #### Parameters
    ///
    /// - `The`: type of component to return
    ///
    /// - `type`: The type of component that is expected to be returned.
    ///
    /// #### Returns
    ///
    /// The first component in this set.
    public <T extends Component> T asComponent(Class<T> type) {
        return (T) asComponent();
    }

    /// Returns the components of this set as a List.  Note that order of elements is not maintained since
    /// ComponentSelector is a set (i.e. has no notion of element order).
    ///
    /// #### Returns
    ///
    /// The components in this set as a list.
    public java.util.List<Component> asList() {
        ArrayList<Component> out = new ArrayList<Component>();
        out.addAll(this);
        return out;
    }

    /// Retains only elements of the result set that are contained in the provided collection.
    ///
    /// #### Parameters
    ///
    /// - `c`
    @Override
    public boolean retainAll(Collection<?> c) {
        setDirty();
        return resultsImpl().retainAll(c);
    }

    /// Fluent API wrapper for `#retainAll(java.util.Collection)`
    ///
    /// #### Parameters
    ///
    /// - `c`: The collection to retain.
    ///
    /// - `chain`: Dummy arg.
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public ComponentSelector retainAll(Collection<?> c, boolean chain) {
        retainAll(c);
        return this;
    }

    /// Removes all of the components in the provided collection from the result set.
    ///
    /// #### Parameters
    ///
    /// - `c`
    @Override
    public boolean removeAll(Collection<?> c) {
        setDirty();
        return resultsImpl().removeAll(c);
    }

    /// Fluent API wrapper for `#removeAll(java.util.Collection)`
    ///
    /// #### Parameters
    ///
    /// - `c`: Collection with components to remove,
    ///
    /// - `chain`: Dummy arg.
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public ComponentSelector removeAll(Collection<?> c, boolean chain) {
        removeAll(c);
        return this;
    }

    /// Clears the result set.
    @Override
    public void clear() {
        setDirty();
        resultsImpl().clear();
    }

    /// Fluent API wrapper for (@link #clear()}
    ///
    /// #### Parameters
    ///
    /// - `chain`: Dummy arg
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public ComponentSelector clear(boolean chain) {
        clear();
        return this;
    }

    @Override
    public String toString() {
        return "ComponentSelector{ name=" + name + ", uiid=" + uiid + ", tags=" + Arrays.toString(tags) + ", roots=" + roots + ", parent=" + parent + ", results = " + results + "}";
    }

    /// Adds the given tags to all elements in the result set.
    ///
    /// #### Parameters
    ///
    /// - `tags`: Tags to add.
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public ComponentSelector addTags(String... tags) {
        for (Component c : this) {

            String existing = (String) c.getClientProperty(PROPERTY_TAG);
            if (existing == null) {
                existing = "";
            }

            StringBuilder existingBuilder = new StringBuilder(existing);
            for (String tag : tags) {
                if (existingBuilder.toString().indexOf(" " + tag + " ") == -1) {
                    existingBuilder.append(" ").append(tag).append(" ");
                }
            }
            c.putClientProperty(PROPERTY_TAG, existingBuilder.toString());

        }
        return this;
    }

    /// Removes the given tags from all elements in the result set.
    ///
    /// #### Parameters
    ///
    /// - `tags`
    public ComponentSelector removeTags(String... tags) {
        for (Component c : this) {
            String existing = (String) c.getClientProperty(PROPERTY_TAG);
            if (existing == null) {
                continue;
            }
            Set<String> existingSet = new HashSet<String>();
            String[] existingStrs = Util.split(" ", existing);
            for (String existingStr : existingStrs) {
                existingStr = existingStr.trim();
                if (existingStr.length() > 0) {
                    existingSet.add(existingStr);
                }
            }
            Collections.addAll(existingSet, tags);
            existing = "";
            if (existingSet.isEmpty()) {
                c.putClientProperty(PROPERTY_TAG, null);
                continue;
            }
            StringBuilder existingBuilder = new StringBuilder(existing);
            for (String tag : existingSet) {
                existingBuilder.append(" ").append(tag).append(" ");
            }
            c.putClientProperty(PROPERTY_TAG, existingBuilder.toString());
        }
        return this;
    }

    /// Gets the set of all "parent" components of components in the result set.
    ///
    /// #### Returns
    ///
    /// @return New Component selector with respective parents of the components in the
    /// current result set.
    public ComponentSelector getParent() {

        HashSet<Component> parents = new HashSet<Component>();
        for (Component c : this) {
            Component parent = c.getParent();
            //System.out.println("Parent is "+parent);
            if (parent != null) {
                parents.add(parent);
            }
        }
        //System.out.println(resultsImpl() + "Creating parent component selector with "+parents);
        return new ComponentSelector(parents);

    }

    /// Wrapper for `Component#setSameWidth(com.codename1.ui.Component...)`.  Passes all
    /// components in the result set as parameters of this method, effectively making them all the
    /// same width.
    public ComponentSelector setSameWidth() {
        Component.setSameWidth(toArray(new Component[size()]));
        return this;
    }

    /// Wrapper for `Component#setSameHeight(com.codename1.ui.Component...)`.  Passes all
    /// components in the result set as parameters of this method, effectively making them all the
    /// same height.
    public ComponentSelector setSameHeight() {
        Component.setSameHeight(toArray(new Component[size()]));
        return this;
    }

    /// Wrapper for `Component#clearClientProperties()`.
    ///
    /// #### Returns
    ///
    /// Self for Chaining.
    public ComponentSelector clearClientProperties() {
        for (Component c : this) {
            c.clearClientProperties();
        }
        return this;
    }

    /// Wrapper for `java.lang.Object)`
    ///
    /// #### Parameters
    ///
    /// - `key`: Property key
    ///
    /// - `value`: Property value
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public ComponentSelector putClientProperty(String key, Object value) {
        for (Component c : this) {
            c.putClientProperty(key, value);

        }
        return this;
    }

    /// Gets a client property from the first component in the set.  Wraps `Component#getClientProperty(java.lang.String)`
    ///
    /// #### Parameters
    ///
    /// - `key`: The key of the client property to retrieve.
    ///
    /// #### Returns
    ///
    /// The value of the client property.
    public Object getClientProperty(String key) {
        Iterator<Component> iterator = iterator(); // PMD Fix: AvoidBranchingStatementAsLastInLoop
        if (iterator.hasNext()) {
            return iterator.next().getClientProperty(key);
        }
        return null;
    }

    /// Wrapper for `Component#setDirtyRegion(com.codename1.ui.geom.Rectangle)`
    ///
    /// #### Parameters
    ///
    /// - `rect`: Dirty region
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public ComponentSelector setDirtyRegion(Rectangle rect) {
        for (Component c : this) {
            c.setDirtyRegion(rect);
        }
        return this;
    }

    /// Wrapper for `Component#setX(int)`
    ///
    /// #### Parameters
    ///
    /// - `x`
    public ComponentSelector setX(int x) {
        for (Component c : this) {
            c.setX(x);
        }
        return this;
    }

    /// Wrapper for `Component#setY(int)`
    ///
    /// #### Parameters
    ///
    /// - `y`
    public ComponentSelector setY(int y) {
        for (Component c : this) {
            c.setY(y);
        }
        return this;
    }

    /// Wrapper for `Component#setWidth(int)`
    ///
    /// #### Parameters
    ///
    /// - `width`
    public ComponentSelector setWidth(int width) {
        for (Component c : this) {
            c.setWidth(width);
        }
        return this;
    }

    /// Wrapper for `Component#setHeight(int)`
    ///
    /// #### Parameters
    ///
    /// - `height`
    public ComponentSelector setHeight(int height) {
        for (Component c : this) {
            c.setHeight(height);
        }
        return this;
    }

    /// Wrapper for `Component#setPreferredSize(com.codename1.ui.geom.Dimension)`
    ///
    /// #### Parameters
    ///
    /// - `dim`
    public ComponentSelector setPreferredSize(Dimension dim) {
        for (Component c : this) {
            c.setPreferredSize(dim);
        }
        return this;
    }

    /// Wrapper for `Component#setPreferredH(int)`
    ///
    /// #### Parameters
    ///
    /// - `h`
    public ComponentSelector setPreferredH(int h) {
        for (Component c : this) {
            c.setPreferredH(h);
        }
        return this;
    }

    /// Wrapper for `Component#setPreferredW(int)`
    ///
    /// #### Parameters
    ///
    /// - `w`
    public ComponentSelector setPreferredW(int w) {
        for (Component c : this) {
            c.setPreferredW(w);
        }
        return this;
    }

    /// Wrapper for `Component#setScrollSize`
    ///
    /// #### Parameters
    ///
    /// - `size`
    public ComponentSelector setScrollSize(Dimension size) {
        for (Component c : this) {
            c.setScrollSize(size);
        }
        return this;
    }

    /// Wrapper for `Component#setSize(com.codename1.ui.geom.Dimension)`
    ///
    /// #### Parameters
    ///
    /// - `size`
    public ComponentSelector setSize(Dimension size) {
        for (Component c : this) {
            c.setSize(size);
        }
        return this;
    }

    /// Wrapper for `Component#setUIID(java.lang.String)`
    ///
    /// #### Parameters
    ///
    /// - `uiid`
    public ComponentSelector setUIID(String uiid) {
        for (Component c : this) {
            c.setUIID(uiid);
        }
        return this;
    }

    /// Wrapper for `Component#remove()`.  This will remove all of the components
    /// in the current found set from their respective parents.
    public ComponentSelector remove() {
        for (Component c : this) {
            c.remove();
        }
        return this;
    }

    /// Adds a focus listener to all components in found set.  Wraps `Component#addFocusListener(com.codename1.ui.events.FocusListener)`
    ///
    /// #### Parameters
    ///
    /// - `l`
    public ComponentSelector addFocusListener(FocusListener l) {
        for (Component c : this) {
            c.addFocusListener(l);
        }
        return this;
    }

    /// Removes focus listener from all components in found set.  Wraps `Component#removeFocusListener(com.codename1.ui.events.FocusListener)`
    ///
    /// #### Parameters
    ///
    /// - `l`
    public ComponentSelector removeFocusListener(FocusListener l) {
        for (Component c : this) {
            c.removeFocusListener(l);
        }
        return this;
    }

    /// Adds scroll listener to all components in found set.  Wraps `Component#addScrollListener(com.codename1.ui.events.ScrollListener)`
    ///
    /// #### Parameters
    ///
    /// - `l`
    public ComponentSelector addScrollListener(ScrollListener l) {
        for (Component c : this) {
            c.addScrollListener(l);
        }
        return this;
    }

    /// Removes scroll listener from all components in found set. Wraps `Component#removeScrollListener(com.codename1.ui.events.ScrollListener)`
    ///
    /// #### Parameters
    ///
    /// - `l`
    public ComponentSelector removeScrollListener(ScrollListener l) {
        for (Component c : this) {
            c.removeScrollListener(l);
        }
        return this;
    }

    /// Sets select command text on all components in found set.  Wraps `Component#setSelectCommandText(java.lang.String)`
    ///
    /// #### Parameters
    ///
    /// - `text`
    public ComponentSelector setSelectCommandText(String text) {
        for (Component c : this) {
            c.setSelectCommandText(text);
        }
        return this;
    }

    /// Wraps `Component#setLabelForComponent(com.codename1.ui.Label)`
    ///
    /// #### Parameters
    ///
    /// - `l`
    public ComponentSelector setLabelForComponent(Label l) {
        for (Component c : this) {
            c.setLabelForComponent(l);
        }
        return this;
    }

    /// Wraps `Component#paintBackgrounds(com.codename1.ui.Graphics)`
    ///
    /// #### Parameters
    ///
    /// - `g`
    public ComponentSelector paintBackgrounds(Graphics g) {
        for (Component c : this) {
            c.paintBackgrounds(g);
        }
        return this;
    }

    /// Wraps `Component#paintComponent(com.codename1.ui.Graphics)`
    ///
    /// #### Parameters
    ///
    /// - `g`
    public ComponentSelector paintComponent(Graphics g) {
        for (Component c : this) {
            c.paintComponent(g);
        }
        return this;
    }

    /// Wraps `Component#paint(com.codename1.ui.Graphics)`
    ///
    /// #### Parameters
    ///
    /// - `g`
    public ComponentSelector paint(Graphics g) {
        for (Component c : this) {
            c.paint(g);
        }
        return this;
    }

    /// Returns true if any of the components in the found set contains the provided coordinate.
    /// Wraps `int)`
    ///
    /// #### Parameters
    ///
    /// - `x`
    ///
    /// - `y`
    public boolean contains(int x, int y) {
        for (Component c : this) {
            if (c.contains(x, y)) {
                return true;
            }
        }
        return false;
    }

    /// Sets all components in the found set focusability.
    /// Wraps `Component#setFocusable(boolean)`
    ///
    /// #### Parameters
    ///
    /// - `focus`
    public ComponentSelector setFocusable(boolean focus) {
        for (Component c : this) {
            c.setFocusable(focus);
        }
        return this;
    }

    /// Wraps `Component#repaint()`
    public ComponentSelector repaint() {
        for (Component c : this) {
            c.repaint();
        }
        return this;
    }

    /// Wraps `int, int, int)`
    ///
    /// #### Parameters
    ///
    /// - `x`
    ///
    /// - `y`
    ///
    /// - `w`
    ///
    /// - `h`
    public ComponentSelector repaint(int x, int y, int w, int h) {
        for (Component c : this) {
            c.repaint(x, y, w, h);
        }
        return this;
    }

    /// Wraps `Component#setScrollAnimationSpeed(int)`
    ///
    /// #### Parameters
    ///
    /// - `speed`
    public ComponentSelector setScrollAnimationSpeed(int speed) {
        for (Component c : this) {
            c.setScrollAnimationSpeed(speed);
        }
        return this;
    }

    /// Wraps `Component#setSmoothScrolling(boolean)`
    ///
    /// #### Parameters
    ///
    /// - `smooth`
    public ComponentSelector setSmoothScrolling(boolean smooth) {
        for (Component c : this) {
            c.setSmoothScrolling(smooth);
        }
        return this;
    }

    /// Adds a drop listener to all components in found set.  Wraps `Component#addDropListener(com.codename1.ui.events.ActionListener)`
    ///
    /// #### Parameters
    ///
    /// - `l`
    public ComponentSelector addDropListener(ActionListener l) {
        for (Component c : this) {
            c.addDropListener(l);
        }
        return this;
    }

    /// Removes a drop listener from all components in found set.
    /// Wraps `Component#removeDropListener(com.codename1.ui.events.ActionListener)`
    ///
    /// #### Parameters
    ///
    /// - `l`
    public ComponentSelector removeDropListener(ActionListener l) {
        for (Component c : this) {
            c.removeDropListener(l);
        }
        return this;
    }

    /// Adds drag over listener to all components in found set.
    /// Wraps `Component#addDragOverListener(com.codename1.ui.events.ActionListener)`
    ///
    /// #### Parameters
    ///
    /// - `l`
    public ComponentSelector addDragOverListener(ActionListener l) {
        for (Component c : this) {
            c.addDragOverListener(l);
        }
        return this;
    }

    /// Removes drag over listener from all components in found set.
    /// Wraps `Component#removeDragOverListener(com.codename1.ui.events.ActionListener)`
    ///
    /// #### Parameters
    ///
    /// - `l`
    public ComponentSelector removeDragOverListener(ActionListener l) {
        for (Component c : this) {
            c.removeDragOverListener(l);
        }
        return this;
    }

    /// Adds pointer pressed listener to all components in found set.
    /// Wraps `Component#addPointerPressedListener(com.codename1.ui.events.ActionListener)`
    ///
    /// #### Parameters
    ///
    /// - `l`
    public ComponentSelector addPointerPressedListener(ActionListener l) {
        for (Component c : this) {
            c.addPointerPressedListener(l);
        }
        return this;
    }

    /// Adds long pointer pressed listener to all components in found set.
    /// Wraps `Component#addLongPressListener(com.codename1.ui.events.ActionListener)`
    ///
    /// #### Parameters
    ///
    /// - `l`
    public ComponentSelector addLongPressListener(ActionListener l) {
        for (Component c : this) {
            c.addLongPressListener(l);
        }
        return this;
    }

    /// Removes pointer pressed listener from all components in found set.
    /// Wraps `Component#removePointerPressedListener(com.codename1.ui.events.ActionListener)`
    ///
    /// #### Parameters
    ///
    /// - `l`
    public ComponentSelector removePointerPressedListener(ActionListener l) {
        for (Component c : this) {
            c.removePointerPressedListener(l);
        }
        return this;
    }

    /// Removes long pointer pressed listener from all components in found set.
    /// Wraps `Component#removeLongPressListener(com.codename1.ui.events.ActionListener)`
    ///
    /// #### Parameters
    ///
    /// - `l`
    public ComponentSelector removeLongPressListener(ActionListener l) {
        for (Component c : this) {
            c.removeLongPressListener(l);
        }
        return this;
    }

    /// Adds pointer released listener to all components in found set.
    /// Wraps `Component#addPointerReleasedListener(com.codename1.ui.events.ActionListener)`
    ///
    /// #### Parameters
    ///
    /// - `l`
    public ComponentSelector addPointerReleasedListener(ActionListener l) {
        for (Component c : this) {
            c.addPointerReleasedListener(l);
        }
        return this;
    }

    /// Removes pointer released listener from all components in found set.
    /// Wraps `Component#removePointerReleasedListener(com.codename1.ui.events.ActionListener)`
    ///
    /// #### Parameters
    ///
    /// - `l`
    public ComponentSelector removePointerReleasedListener(ActionListener l) {
        for (Component c : this) {
            c.removePointerReleasedListener(l);
        }
        return this;
    }

    /// Adds pointer dragged listener to all components in found set.
    /// Wraps `Component#addPointerDraggedListener(com.codename1.ui.events.ActionListener)`
    ///
    /// #### Parameters
    ///
    /// - `l`
    public ComponentSelector addPointerDraggedListener(ActionListener l) {
        for (Component c : this) {
            c.addPointerDraggedListener(l);
        }
        return this;
    }

    /// REmoves pointer dragged listener from all components in found set.
    /// Wraps `Component#removePointerDraggedListener(com.codename1.ui.events.ActionListener)`
    ///
    /// #### Parameters
    ///
    /// - `l`
    public ComponentSelector removePointerDraggedListener(ActionListener l) {
        for (Component c : this) {
            c.removePointerDraggedListener(l);
        }
        return this;
    }

    /// Wraps `Component#requestFocus()`
    public ComponentSelector requestFocus() {
        Iterator<Component> iterator = iterator(); // PMD Fix: AvoidBranchingStatementAsLastInLoop
        if (iterator.hasNext()) {
            iterator.next().requestFocus();
        }
        return this;
    }

    /// Wraps `Component#refreshTheme()`
    public ComponentSelector refreshTheme() {
        for (Component c : this) {
            c.refreshTheme();
        }
        return this;
    }

    /// Wraps `Component#refreshTheme(boolean)`
    ///
    /// #### Parameters
    ///
    /// - `merge`
    public ComponentSelector refreshTheme(boolean merge) {
        for (Component c : this) {
            c.refreshTheme(merge);
        }
        return this;
    }

    /// Wraps `Component#setCellRenderer(boolean)`
    ///
    /// #### Parameters
    ///
    /// - `cell`
    public ComponentSelector setCellRenderer(boolean cell) {
        for (Component c : this) {
            c.setCellRenderer(cell);
        }
        return this;
    }

    /// Wraps `Component#setScrollVisible(boolean)`
    ///
    /// #### Parameters
    ///
    /// - `vis`
    public ComponentSelector setScrollVisible(boolean vis) {
        for (Component c : this) {
            c.setScrollVisible(vis);
        }
        return this;
    }

    /// Wraps `Component#setEnabled(boolean)`
    ///
    /// #### Parameters
    ///
    /// - `enabled`
    public ComponentSelector setEnabled(boolean enabled) {
        for (Component c : this) {
            c.setEnabled(enabled);
        }
        return this;
    }

    /// Wraps `Component#setName(java.lang.String)`
    ///
    /// #### Parameters
    ///
    /// - `name`
    public ComponentSelector setName(String name) {
        for (Component c : this) {
            c.setName(name);
        }
        return this;
    }

    /// Wraps `Component#setRTL(boolean)`
    ///
    /// #### Parameters
    ///
    /// - `rtl`
    public ComponentSelector setRTL(boolean rtl) {
        for (Component c : this) {
            c.setRTL(rtl);
        }
        return this;
    }

    /// Wraps `Component#setTactileTouch(boolean)`
    ///
    /// #### Parameters
    ///
    /// - `t`
    public ComponentSelector setTactileTouch(boolean t) {
        for (Component c : this) {
            c.setTactileTouch(t);
        }
        return this;
    }

    /// Wraps `java.lang.Object)`
    ///
    /// #### Parameters
    ///
    /// - `key`
    ///
    /// - `value`
    public ComponentSelector setPropertyValue(String key, Object value) {
        for (Component c : this) {
            c.setPropertyValue(key, value);
        }
        return this;
    }

    /// Wraps `Component#paintLockRelease()`
    public ComponentSelector paintLockRelease() {
        for (Component c : this) {
            c.paintLockRelease();
        }
        return this;
    }

    /// Wraps `Component#setSnapToGrid(boolean)`
    ///
    /// #### Parameters
    ///
    /// - `s`
    public ComponentSelector setSnapToGrid(boolean s) {
        for (Component c : this) {
            c.setSnapToGrid(s);
        }
        return this;
    }

    public boolean isIgnorePointerEvents() {
        Iterator<Component> iterator = iterator(); // PMD Fix: AvoidBranchingStatementAsLastInLoop
        if (iterator.hasNext()) {
            return iterator.next().isIgnorePointerEvents();
        }
        return false;
    }

    public ComponentSelector setIgnorePointerEvents(boolean ignore) {
        for (Component c : this) {
            c.setIgnorePointerEvents(ignore);
        }
        return this;
    }

    /// Wraps `Component#setFlatten(boolean)`
    ///
    /// #### Parameters
    ///
    /// - `f`
    public ComponentSelector setFlatten(boolean f) {
        for (Component c : this) {
            c.setFlatten(f);
        }
        return this;
    }

    /// Wraps `Component#setTensileLength(int)`
    ///
    /// #### Parameters
    ///
    /// - `len`
    public ComponentSelector setTensileLength(int len) {
        for (Component c : this) {
            c.setTensileLength(len);
        }
        return this;
    }

    /// Wraps `Component#setGrabsPointerEvents(boolean)`
    ///
    /// #### Parameters
    ///
    /// - `g`
    public ComponentSelector setGrabsPointerEvents(boolean g) {
        for (Component c : this) {
            c.setGrabsPointerEvents(g);
        }
        return this;
    }

    /// Wraps `Component#setScrollOpacityChangeSpeed(int)`
    ///
    /// #### Parameters
    ///
    /// - `scrollOpacityChangeSpeed`
    public ComponentSelector setScrollOpacityChangeSpeed(int scrollOpacityChangeSpeed) {
        for (Component c : this) {
            c.setScrollOpacityChangeSpeed(scrollOpacityChangeSpeed);
        }
        return this;
    }

    /// Wraps `Component#growShrink(int)`
    ///
    /// #### Parameters
    ///
    /// - `duration`
    public ComponentSelector growShrink(int duration) {
        for (Component c : this) {
            c.growShrink(duration);
        }
        return this;
    }

    /// Wraps `Component#setDraggable(boolean)`
    ///
    /// #### Parameters
    ///
    /// - `draggable`
    public ComponentSelector setDraggable(boolean draggable) {
        for (Component c : this) {
            c.setDraggable(draggable);
        }
        return this;
    }

    /// Wraps `Component#setDropTarget(boolean)`
    ///
    /// #### Parameters
    ///
    /// - `target`
    public ComponentSelector setDropTarget(boolean target) {
        for (Component c : this) {
            c.setDropTarget(target);
        }
        return this;
    }

    /// Wraps `Component#setHideInPortrait(boolean)`
    ///
    /// #### Parameters
    ///
    /// - `hide`
    public ComponentSelector setHideInPortait(boolean hide) {
        for (Component c : this) {
            c.setHideInPortrait(hide);
        }
        return this;
    }

    /// Wraps `boolean)`
    ///
    /// #### Parameters
    ///
    /// - `b`
    ///
    /// - `changeMargin`
    public ComponentSelector setHidden(boolean b, boolean changeMargin) {
        for (Component c : this) {
            c.setHidden(b, changeMargin);
        }
        return this;
    }

    /// Wraps `Component#setComponentState(java.lang.Object)`
    ///
    /// #### Parameters
    ///
    /// - `state`
    public ComponentSelector setComponentState(Object state) {
        for (Component c : this) {
            c.setComponentState(state);
        }
        return this;
    }

    /// Wraps `Container#setLeadComponent(com.codename1.ui.Component)`
    ///
    /// #### Parameters
    ///
    /// - `lead`
    public ComponentSelector setLeadComponent(Component lead) {
        for (Component c : this) {
            if (c instanceof Container) {
                Container cnt = (Container) c;
                cnt.setLeadComponent(lead);
            }
        }
        return this;
    }

    /// Wraps `Container#setLayout(com.codename1.ui.layouts.Layout)`
    ///
    /// #### Parameters
    ///
    /// - `layout`
    public ComponentSelector setLayout(Layout layout) {
        for (Component c : this) {
            if (c instanceof Container) {
                Container cnt = (Container) c;
                cnt.setLayout(layout);
            }
        }
        return this;
    }

    /// Wraps `Container#invalidate()`
    public ComponentSelector invalidate() {
        for (Component c : this) {
            if (c instanceof Container) {
                Container cnt = (Container) c;
                cnt.invalidate();
            }

        }
        return this;
    }

    /// Wraps `Container#setShouldCalcPreferredSize(boolean)`
    ///
    /// #### Parameters
    ///
    /// - `shouldCalcPreferredSize`
    public ComponentSelector setShouldCalcPreferredSize(boolean shouldCalcPreferredSize) {
        for (Component c : this) {
            if (c instanceof Container) {
                Container cnt = (Container) c;
                cnt.setShouldCalcPreferredSize(shouldCalcPreferredSize);
            }

        }
        return this;
    }

    /// Wraps `Container#applyRTL(boolean)`
    ///
    /// #### Parameters
    ///
    /// - `rtl`
    public ComponentSelector applyRTL(boolean rtl) {
        for (Component c : this) {
            if (c instanceof Container) {
                Container cnt = (Container) c;
                cnt.applyRTL(rtl);
            }
        }
        return this;
    }

    /// This removes all children from all containers in found set.  Not to be confused with `#clear()`, which
    /// removes components from the found set, but not from their respective parents.
    /// Wraps `Container#removeAll()`
    public ComponentSelector removeAll() {
        for (Component c : this) {
            if (c instanceof Container) {
                ((Container) c).removeAll();
            }
        }
        return this;
    }

    /// Wraps `Container#revalidate()`
    public ComponentSelector revalidate() {
        for (Component c : this) {
            if (c instanceof Container) {
                ((Container) c).revalidate();
            }
        }
        return this;
    }

    /// Wraps `Container#forceRevalidate()`
    public ComponentSelector forceRevalidate() {

        for (Component c : this) {
            if (c instanceof Container) {
                ((Container) c).forceRevalidate();
            }
        }
        return this;
    }

    /// Wraps `Container#layoutContainer()`
    public ComponentSelector layoutContainer() {

        for (Component c : this) {
            if (c instanceof Container) {
                ((Container) c).layoutContainer();
            }
        }
        return this;
    }

    /// This returns a new ComponentSelector which includes a set of all results
    /// of calling `Container#getComponentAt(int)` on containers in this
    /// found set.  This effectively allows us to get all of the ith elements of all
    /// matched components.
    ///
    /// #### Parameters
    ///
    /// - `index`
    ///
    /// #### Returns
    ///
    /// New ComponentSelector with `index`th child of each container in the current found set.
    public ComponentSelector getComponentAt(int index) {
        HashSet<Component> out = new HashSet<Component>();
        for (Component c : this) {
            if (c instanceof Container) {
                Container cnt = (Container) c;
                if (cnt.getComponentCount() > index) {
                    out.add(cnt.getComponentAt(index));
                }
            }
        }
        return new ComponentSelector(out);
    }

    /// Returns true if any of the containers in the current found set contains
    /// the provided component in its subtree.
    /// Wraps `Container#contains(com.codename1.ui.Component)`
    ///
    /// #### Parameters
    ///
    /// - `cmp`
    public boolean containsInSubtree(Component cmp) {
        for (Component c : this) {
            if (c instanceof Container) {
                if (((Container) c).contains(cmp)) {
                    return true;
                }
            }

        }
        return false;
    }

    /// Wraps `Container#scrollComponentToVisible(com.codename1.ui.Component)`
    ///
    /// #### Parameters
    ///
    /// - `cmp`
    public ComponentSelector scrollComponentToVisible(Component cmp) {
        for (Component c : this) {
            if (c instanceof Container) {
                ((Container) c).scrollComponentToVisible(cmp);
            }
        }
        return this;
    }

    /// Returns new ComponentSelector with the set of all components returned from calling
    /// `int)` in the current found set.
    ///
    /// #### Parameters
    ///
    /// - `x`
    ///
    /// - `y`
    ///
    /// #### Returns
    ///
    /// New ComponentSelector with components at the given coordinates.
    public ComponentSelector getComponentAt(int x, int y) {
        HashSet<Component> out = new HashSet<Component>();
        for (Component c : this) {
            if (c instanceof Container) {
                Component match = ((Container) c).getComponentAt(x, y);
                if (match != null) {
                    out.add(match);
                }
            }
        }
        return new ComponentSelector(out);
    }

    /// Wraps `Container#setScrollableX(boolean)`
    ///
    /// #### Parameters
    ///
    /// - `b`
    public ComponentSelector setScrollableX(boolean b) {
        for (Component c : this) {
            if (c instanceof Container) {
                ((Container) c).setScrollableX(b);
            }
        }
        return this;
    }

    /// Wraps `Container#setScrollableY(boolean)`
    ///
    /// #### Parameters
    ///
    /// - `b`
    public ComponentSelector setScrollableY(boolean b) {
        for (Component c : this) {
            if (c instanceof Container) {
                ((Container) c).setScrollableY(b);
            }
        }
        return this;
    }

    /// Wraps `Container#setScrollIncrement(int)`
    ///
    /// #### Parameters
    ///
    /// - `b`
    public ComponentSelector setScrollIncrement(int b) {
        for (Component c : this) {
            if (c instanceof Container) {
                ((Container) c).setScrollIncrement(b);
            }
        }
        return this;
    }

    /// Creates new ComponentSelector with the set of first focusable elements of
    /// each of the containers in the current result set.
    ///
    /// #### Returns
    ///
    /// @return New component selector with first focusable element of each container in
    /// current found set.
    ///
    /// #### See also
    ///
    /// - Container#findFirstFocusable()
    public ComponentSelector findFirstFocusable() {
        HashSet<Component> out = new HashSet<Component>();
        for (Component c : this) {
            if (c instanceof Container) {
                Component match = ((Container) c).findFirstFocusable();
                if (match != null) {
                    out.add(match);
                }
            }
        }
        return new ComponentSelector(out);
    }

    /// Wraps `Container#animateHierarchyAndWait(int)`.
    ///
    /// #### Parameters
    ///
    /// - `duration`
    public ComponentSelector animateHierarchyAndWait(int duration) {
        ArrayList<ComponentAnimation> animations = new ArrayList<ComponentAnimation>();
        for (Component c : this) {
            if (c instanceof Container) {
                animations.add(((Container) c).createAnimateHierarchy(duration));
            }
        }
        AnimationManager mgr = getAnimationManager();
        if (!animations.isEmpty() && mgr != null) {
            mgr.addAnimationAndBlock(ComponentAnimation.compoundAnimation(animations.toArray(new ComponentAnimation[animations.size()])));
        }
        return this;
    }

    /// Animates the hierarchy of containers in this set.  Wraps `Container#animateHierarchy(int)`
    ///
    /// #### Parameters
    ///
    /// - `duration`
    public ComponentSelector animateHierarchy(int duration) {
        return animateHierarchy(duration, null);
    }

    /// Wraps `Container#animateHierarchy(int)`.
    ///
    /// #### Parameters
    ///
    /// - `duration`
    ///
    /// - `callback`
    public ComponentSelector animateHierarchy(int duration, final SuccessCallback<ComponentSelector> callback) {
        ArrayList<ComponentAnimation> animations = new ArrayList<ComponentAnimation>();
        for (Component c : this) {
            if (c instanceof Container) {
                animations.add(((Container) c).createAnimateHierarchy(duration));
            }
        }
        AnimationManager mgr = getAnimationManager();
        if (!animations.isEmpty() && mgr != null) {
            mgr.addAnimation(ComponentAnimation.compoundAnimation(animations.toArray(new ComponentAnimation[animations.size()])), new Runnable() {
                @Override
                public void run() {
                    if (callback != null) {
                        callback.onSucess(ComponentSelector.this);
                    }
                }
            });
        }
        return this;
    }

    /// Wraps `int)`.
    ///
    /// #### Parameters
    ///
    /// - `duration`
    ///
    /// - `startingOpacity`
    public ComponentSelector animateHierarchyFadeAndWait(int duration, int startingOpacity) {
        ArrayList<ComponentAnimation> animations = new ArrayList<ComponentAnimation>();
        for (Component c : this) {
            if (c instanceof Container) {
                animations.add(((Container) c).createAnimateHierarchyFade(duration, startingOpacity));
            }
        }
        AnimationManager mgr = getAnimationManager();
        if (!animations.isEmpty() && mgr != null) {
            mgr.addAnimationAndBlock(ComponentAnimation.compoundAnimation(animations.toArray(new ComponentAnimation[animations.size()])));
        }
        return this;
    }

    /// Wraps `Container#animateHierarchyAndWait(int)`
    ///
    /// #### Parameters
    ///
    /// - `duration`: The duration of the animation.
    ///
    /// - `startingOpacity`: The starting opacity.
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public ComponentSelector animateHierarchyFade(int duration, int startingOpacity) {
        return animateHierarchyFade(duration, startingOpacity, null);
    }

    /// Wraps `int)`.
    ///
    /// #### Parameters
    ///
    /// - `duration`
    ///
    /// - `startingOpacity`
    ///
    /// - `callback`
    public ComponentSelector animateHierarchyFade(int duration, int startingOpacity, final SuccessCallback<ComponentSelector> callback) {
        ArrayList<ComponentAnimation> animations = new ArrayList<ComponentAnimation>();
        for (Component c : this) {
            if (c instanceof Container) {
                animations.add(((Container) c).createAnimateHierarchyFade(duration, startingOpacity));
            }
        }
        AnimationManager mgr = getAnimationManager();
        if (!animations.isEmpty() && mgr != null) {
            mgr.addAnimation(ComponentAnimation.compoundAnimation(animations.toArray(new ComponentAnimation[animations.size()])), new Runnable() {
                @Override
                public void run() {
                    if (callback != null) {
                        callback.onSucess(ComponentSelector.this);
                    }
                }
            });
        }
        return this;
    }

    /// Wraps `int)`.
    ///
    /// #### Parameters
    ///
    /// - `duration`
    ///
    /// - `startingOpacity`
    public ComponentSelector animateLayoutFadeAndWait(int duration, int startingOpacity) {
        ArrayList<ComponentAnimation> animations = new ArrayList<ComponentAnimation>();
        for (Component c : this) {
            if (c instanceof Container) {
                animations.add(((Container) c).createAnimateLayoutFade(duration, startingOpacity));
            }
        }
        AnimationManager mgr = getAnimationManager();
        if (!animations.isEmpty() && mgr != null) {
            mgr.addAnimationAndBlock(ComponentAnimation.compoundAnimation(animations.toArray(new ComponentAnimation[animations.size()])));
        }
        return this;
    }

    /// Animates layout with fade on all containers in this set.  Wraps `int)`.
    ///
    /// #### Parameters
    ///
    /// - `duration`
    ///
    /// - `startingOpacity`
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public ComponentSelector animateLayoutFade(int duration, int startingOpacity) {
        return animateLayoutFade(duration, startingOpacity, null);
    }

    /// Wraps `int)`.
    ///
    /// #### Parameters
    ///
    /// - `duration`
    ///
    /// - `startingOpacity`
    ///
    /// - `callback`
    public ComponentSelector animateLayoutFade(int duration, int startingOpacity, final SuccessCallback<ComponentSelector> callback) {
        ArrayList<ComponentAnimation> animations = new ArrayList<ComponentAnimation>();
        for (Component c : this) {
            if (c instanceof Container) {
                animations.add(((Container) c).createAnimateLayoutFade(duration, startingOpacity));
            }
        }
        AnimationManager mgr = getAnimationManager();
        if (!animations.isEmpty() && mgr != null) {
            mgr.addAnimation(ComponentAnimation.compoundAnimation(animations.toArray(new ComponentAnimation[animations.size()])), new Runnable() {

                @Override
                public void run() {
                    if (callback != null) {
                        callback.onSucess(ComponentSelector.this);
                    }
                }

            });
        }
        return this;
    }

    /// Wraps `Container#animateLayout(int)`
    ///
    /// #### Parameters
    ///
    /// - `duration`
    public ComponentSelector animateLayout(int duration) {
        return animateLayout(duration, null);
    }

    /// Wraps `Container#animateLayout(int)`.
    ///
    /// #### Parameters
    ///
    /// - `duration`
    ///
    /// - `callback`
    public ComponentSelector animateLayout(int duration, final SuccessCallback<ComponentSelector> callback) {
        ArrayList<ComponentAnimation> animations = new ArrayList<ComponentAnimation>();
        for (Component c : this) {
            if (c instanceof Container) {
                animations.add(((Container) c).createAnimateLayout(duration));
            }
        }
        AnimationManager mgr = getAnimationManager();
        if (!animations.isEmpty() && mgr != null) {
            mgr.addAnimation(ComponentAnimation.compoundAnimation(animations.toArray(new ComponentAnimation[animations.size()])), new Runnable() {

                @Override
                public void run() {
                    if (callback != null) {
                        callback.onSucess(ComponentSelector.this);
                    }
                }

            });
        }
        return this;
    }

    /// Wraps `Container#animateLayoutAndWait(int)`
    ///
    /// #### Parameters
    ///
    /// - `duration`
    public ComponentSelector animateLayoutAndWait(int duration) {
        ArrayList<ComponentAnimation> animations = new ArrayList<ComponentAnimation>();
        for (Component c : this) {
            if (c instanceof Container) {
                animations.add(((Container) c).createAnimateLayout(duration));
            }
        }
        AnimationManager mgr = getAnimationManager();
        if (!animations.isEmpty() && mgr != null) {
            mgr.addAnimationAndBlock(ComponentAnimation.compoundAnimation(animations.toArray(new ComponentAnimation[animations.size()])));
        }
        return this;
    }

    /// Wraps `int, java.lang.Runnable)`
    ///
    /// #### Parameters
    ///
    /// - `duration`
    ///
    /// - `opacity`
    public ComponentSelector animateUnlayout(int duration, int opacity) {
        return animateUnlayout(duration, opacity, null);
    }

    /// Wraps `int, java.lang.Runnable)`
    ///
    /// #### Parameters
    ///
    /// - `duration`
    ///
    /// - `opacity`
    ///
    /// - `callback`: Callback to run when animation has completed.
    public ComponentSelector animateUnlayout(int duration, int opacity, final SuccessCallback<ComponentSelector> callback) {
        ArrayList<ComponentAnimation> animations = new ArrayList<ComponentAnimation>();
        for (Component c : this) {
            if (c instanceof Container) {

                animations.add(((Container) c).createAnimateUnlayout(duration, opacity, null));
            }
        }
        AnimationManager mgr = getAnimationManager();
        if (!animations.isEmpty() && mgr != null) {
            mgr.addAnimation(ComponentAnimation.compoundAnimation(animations.toArray(new ComponentAnimation[animations.size()])), new Runnable() {
                @Override
                public void run() {
                    if (callback != null) {
                        callback.onSucess(ComponentSelector.this);
                    }
                }
            });
        }
        return this;
    }

    /// Wraps `int)`
    ///
    /// #### Parameters
    ///
    /// - `duration`
    ///
    /// - `opacity`
    public ComponentSelector animateUnlayoutAndWait(int duration, int opacity) {
        ArrayList<ComponentAnimation> animations = new ArrayList<ComponentAnimation>();
        for (Component c : this) {
            if (c instanceof Container) {

                animations.add(((Container) c).createAnimateUnlayout(duration, opacity, null));
            }
        }
        AnimationManager mgr = getAnimationManager();
        if (!animations.isEmpty() && mgr != null) {
            mgr.addAnimationAndBlock(ComponentAnimation.compoundAnimation(animations.toArray(new ComponentAnimation[animations.size()])));
        }
        return this;
    }

    /// Gets the AnimationManager for the components in this set.
    ///
    /// #### Returns
    ///
    /// The AnimationManager for the components in this set.
    ///
    /// #### See also
    ///
    /// - Component#getAnimationManager()
    public AnimationManager getAnimationManager() {
        for (Component c : this) {
            AnimationManager mgr = c.getAnimationManager();
            if (mgr != null) {
                return mgr;
            }
        }
        return null;
    }

    /// Gets the text on the first component in this set that supports this property.  Currently this works with
    /// Label, TextArea, SpanLabel, and SpanButtons, and subclasses thereof.
    public String getText() {
        for (Component c : this) {
            if (c instanceof Label) {
                return ((Label) c).getText();
            } else if (c instanceof TextArea) {
                return ((TextArea) c).getText();
            } else if (c instanceof SpanLabel) {
                return ((SpanLabel) c).getText();
            } else if (c instanceof SpanButton) {
                return ((SpanButton) c).getText();
            }
        }
        return null;
    }

    /// Sets the text on all components in found set that support this.  Currently this works with
    /// Label, TextArea, SpanLabel, and SpanButtons, and subclasses thereof.
    ///
    /// #### Parameters
    ///
    /// - `text`: The text to set in the componnet.
    public ComponentSelector setText(String text) {
        for (Component c : this) {
            if (c instanceof Label) {
                ((Label) c).setText(text);
            } else if (c instanceof TextArea) {
                ((TextArea) c).setText(text);
            } else if (c instanceof SpanLabel) {
                ((SpanLabel) c).setText(text);
            } else if (c instanceof SpanButton) {
                ((SpanButton) c).setText(text);
            }
        }
        return this;
    }

    /// Sets the icon for components in found set.  Only relevant to Labels, SpanLabels, and SpanButtons, and subclasses thereof.
    ///
    /// #### Parameters
    ///
    /// - `icon`
    public ComponentSelector setIcon(Image icon) {
        for (Component c : this) {
            if (c instanceof IconHolder) {
                ((IconHolder) c).setIcon(icon);
            }
        }
        return this;
    }

    /// Sets the icons of all elements in this set to a material icon.
    ///
    /// #### Parameters
    ///
    /// - `materialIcon`: Material icon charcode.
    ///
    /// - `style`: The style for the icon.
    ///
    /// - `size`: The size for the icon. (in mm)
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    ///
    /// #### See also
    ///
    /// - FontImage#createMaterial(char, java.lang.String, float)
    public ComponentSelector setIcon(char materialIcon, Style style, float size) {
        FontImage img = FontImage.createMaterial(materialIcon, style, size);
        return setIcon(img);
    }

    /// Sets the icon of all elements in this set to a material icon.  This will use
    /// the foreground color of each label as the icon's foreground color.
    ///
    /// #### Parameters
    ///
    /// - `materialIcon`: The icon charcode.
    ///
    /// - `size`: The size of the icon (in mm)
    ///
    /// #### Returns
    ///
    /// Self for chaining
    ///
    /// #### See also
    ///
    /// - FontImage#createMaterial(char, com.codename1.ui.plaf.Style, float)
    public ComponentSelector setIcon(char materialIcon, float size) {
        for (Component c : this) {
            if (c instanceof Label) {
                Label l = (Label) c;
                Style style = new Style();
                Style cStyle = c.getUnselectedStyle();
                style.setBgTransparency(0);
                style.setFgColor(cStyle.getFgColor());
                l.setIcon(FontImage.createMaterial(materialIcon, style, size));

                if (c instanceof Button) {
                    Button b = (Button) c;
                    style = new Style();
                    cStyle = c.getPressedStyle();
                    style.setBgTransparency(0);
                    style.setFgColor(cStyle.getFgColor());
                    b.setPressedIcon(FontImage.createMaterial(materialIcon, style, size));
                }

            }

        }
        return this;

    }

    /// Sets the icon of all elements in this set to a material icon.  This will use
    /// the foreground color of the label.
    ///
    /// #### Parameters
    ///
    /// - `materialIcon`: The material icon charcode.
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public ComponentSelector setIcon(char materialIcon) {
        for (Component c : this) {
            if (c instanceof Label) {
                Label l = (Label) c;
                Style style = new Style();
                Style cStyle = c.getUnselectedStyle();
                style.setBgTransparency(0);
                style.setFgColor(cStyle.getFgColor());

                l.setIcon(FontImage.createMaterial(materialIcon, style, 3));

                if (c instanceof Button) {
                    Button b = (Button) c;
                    style = new Style();
                    cStyle = c.getPressedStyle();
                    style.setBgTransparency(0);
                    style.setFgColor(cStyle.getFgColor());

                    b.setPressedIcon(FontImage.createMaterial(materialIcon, style, 3));
                }

            }

        }
        return this;

    }

    /// Gets the set of all component forms from components in this set.
    ///
    /// #### Returns
    ///
    /// New ComponentSelector with forms all components in this set.
    public ComponentSelector getComponentForm() {
        HashSet<Component> out = new HashSet<Component>();
        for (Component c : this) {
            out.add(c.getComponentForm());
        }
        return new ComponentSelector(out);
    }

    /// Sets vertical alignment of text.
    ///
    /// #### Parameters
    ///
    /// - `valign`
    ///
    /// #### See also
    ///
    /// - Label#setVerticalAlignment(int)
    public ComponentSelector setVerticalAlignment(int valign) {
        for (Component c : this) {
            if (c instanceof Label) {
                ((Label) c).setVerticalAlignment(valign);
            }
        }
        return this;
    }

    /// Sets text position of text.  Only relevant to labels.
    ///
    /// #### Parameters
    ///
    /// - `pos`
    ///
    /// #### See also
    ///
    /// - Label#setTextPosition(int)
    public ComponentSelector setTextPosition(int pos) {
        for (Component c : this) {
            if (c instanceof IconHolder) {
                ((IconHolder) c).setTextPosition(pos);
            }
        }
        return this;
    }

    /// Sets the Icon UIID of elements in found set.
    ///
    /// #### Parameters
    ///
    /// - `uiid`: The UIID for icons.
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    ///
    /// #### Since
    ///
    /// 7.0
    ///
    /// #### See also
    ///
    /// - IconHolder#setIconUIID(java.lang.String)
    public ComponentSelector setIconUIID(String uiid) {
        for (Component c : this) {
            if (c instanceof IconHolder) {
                ((IconHolder) c).setIconUIID(uiid);
            }
        }
        return this;
    }

    /// Sets gap.  Only relevant to labels.
    ///
    /// #### Parameters
    ///
    /// - `gap`
    ///
    /// #### See also
    ///
    /// - Label#setGap(int)
    public ComponentSelector setGap(int gap) {
        for (Component c : this) {
            if (c instanceof IconHolder) {
                ((IconHolder) c).setGap(gap);
            }
        }
        return this;
    }

    /// Sets shift text. Only relevant to labels.
    ///
    /// #### Parameters
    ///
    /// - `shift`
    ///
    /// #### See also
    ///
    /// - Label#setShiftText(int)
    public ComponentSelector setShiftText(int shift) {
        for (Component c : this) {
            if (c instanceof Label) {
                ((Label) c).setShiftText(shift);
            }
        }
        return this;
    }

    /// Wraps `boolean)`
    ///
    /// #### Parameters
    ///
    /// - `delay`
    ///
    /// - `rightToLeft`
    public ComponentSelector startTicker(long delay, boolean rightToLeft) {
        for (Component c : this) {
            if (c instanceof Label) {
                ((Label) c).startTicker(delay, rightToLeft);
            }
        }
        return this;
    }

    /// Wraps `Label#stopTicker()`
    public ComponentSelector stopTicker() {
        for (Component c : this) {
            if (c instanceof Label) {
                ((Label) c).stopTicker();
            }
        }
        return this;
    }

    /// Wraps `Label#setTickerEnabled(boolean)`
    ///
    /// #### Parameters
    ///
    /// - `b`
    public ComponentSelector setTickerEnabled(boolean b) {
        for (Component c : this) {
            if (c instanceof Label) {
                ((Label) c).setTickerEnabled(b);
            }
        }
        return this;
    }

    /// Wraps `Label#setEndsWith3Points(boolean)`
    ///
    /// #### Parameters
    ///
    /// - `b`
    public ComponentSelector setEndsWith3Points(boolean b) {
        for (Component c : this) {
            if (c instanceof Label) {
                ((Label) c).setEndsWith3Points(b);
            }
        }
        return this;
    }

    /// Wraps `Label#setMask(java.lang.Object)`
    ///
    /// #### Parameters
    ///
    /// - `mask`
    public ComponentSelector setMask(Object mask) {
        for (Component c : this) {
            if (c instanceof Label) {
                ((Label) c).setMask(mask);
            }
        }
        return this;
    }

    /// Wraps `Label#setMaskName(java.lang.String)`
    ///
    /// #### Parameters
    ///
    /// - `name`
    public ComponentSelector setMaskName(String name) {
        for (Component c : this) {
            if (c instanceof Label) {
                ((Label) c).setMaskName(name);
            }
        }
        return this;
    }

    /// Wraps `Label#setShouldLocalize(boolean)`
    ///
    /// #### Parameters
    ///
    /// - `b`
    public ComponentSelector setShouldLocalize(boolean b) {
        for (Component c : this) {
            if (c instanceof Label) {
                ((Label) c).setShouldLocalize(b);
            }
        }
        return this;
    }

    /// Wraps `Label#setShiftMillimeters(int)`
    ///
    /// #### Parameters
    ///
    /// - `b`
    public ComponentSelector setShiftMillimeters(int b) {
        for (Component c : this) {
            if (c instanceof Label) {
                ((Label) c).setShiftMillimeters(b);
            }
        }
        return this;
    }

    /// Wraps `Label#setShowEvenIfBlank(boolean)`
    ///
    /// #### Parameters
    ///
    /// - `b`
    public ComponentSelector setShowEvenIfBlank(boolean b) {
        for (Component c : this) {
            if (c instanceof Label) {
                ((Label) c).setShowEvenIfBlank(b);
            }
        }
        return this;
    }

    /// Wraps `Label#setLegacyRenderer(boolean)`
    ///
    /// #### Parameters
    ///
    /// - `b`
    public ComponentSelector setLegacyRenderer(boolean b) {
        for (Component c : this) {
            if (c instanceof Label) {
                ((Label) c).setLegacyRenderer(b);
            }
        }
        return this;
    }

    /// Wraps `Label#setAutoSizeMode(boolean)`
    ///
    /// #### Parameters
    ///
    /// - `b`
    public ComponentSelector setAutoSizeMode(boolean b) {
        for (Component c : this) {
            if (c instanceof Label) {
                ((Label) c).setAutoSizeMode(b);
            }
        }
        return this;
    }

    /// Wraps `Button#setCommand(com.codename1.ui.Command)`
    ///
    /// #### Parameters
    ///
    /// - `cmd`
    public ComponentSelector setCommand(Command cmd) {
        for (Component c : this) {
            if (c instanceof Button) {
                ((Button) c).setCommand(cmd);
            }
        }
        return this;
    }

    /// Wraps `Button#setRolloverPressedIcon(com.codename1.ui.Image)`
    ///
    /// #### Parameters
    ///
    /// - `icon`
    public ComponentSelector setRolloverPressedIcon(Image icon) {
        for (Component c : this) {
            if (c instanceof Button) {
                ((Button) c).setRolloverPressedIcon(icon);
            }
        }
        return this;
    }

    /// Wraps `Button#setRolloverIcon(com.codename1.ui.Image)`
    ///
    /// #### Parameters
    ///
    /// - `icon`
    public ComponentSelector setRolloverIcon(Image icon) {
        for (Component c : this) {
            if (c instanceof Button) {
                ((Button) c).setRolloverIcon(icon);
            }
        }
        return this;
    }

    /// Wraps `Button#setPressedIcon(com.codename1.ui.Image)`
    ///
    /// #### Parameters
    ///
    /// - `icon`
    public ComponentSelector setPressedIcon(Image icon) {
        for (Component c : this) {
            if (c instanceof Button) {
                ((Button) c).setPressedIcon(icon);
            }
        }
        return this;
    }

    /// Wraps `Button#setDisabledIcon(com.codename1.ui.Image)`
    ///
    /// #### Parameters
    ///
    /// - `icon`
    public ComponentSelector setDisabledIcon(Image icon) {
        for (Component c : this) {
            if (c instanceof Button) {
                ((Button) c).setDisabledIcon(icon);
            }
        }
        return this;
    }

    /// Adds action listener to applicable components in found set.  Currently this will apply to
    /// Buttons, Lists, Sliders, TextAreas, OnOffSwitches, SpanButtons, and subclasses thereof.
    ///
    /// #### Parameters
    ///
    /// - `l`
    public ComponentSelector addActionListener(ActionListener l) {
        for (Component c : this) {
            if (c instanceof ActionSource) {
                ((ActionSource) c).addActionListener(l);
            }
        }
        return this;
    }

    /// Removes action listeners from components in set.
    ///
    /// #### Parameters
    ///
    /// - `l`: The listener to remove
    ///
    /// #### Since
    ///
    /// 7.0
    public ComponentSelector removeActionListener(ActionListener l) {
        for (Component c : this) {
            if (c instanceof ActionSource) {
                ((ActionSource) c).removeActionListener(l);
            }
        }
        return this;
    }

    /// Wraps `TextArea#setEditable(boolean)`
    ///
    /// #### Parameters
    ///
    /// - `b`
    public ComponentSelector setEditable(boolean b) {
        for (Component c : this) {
            if (c instanceof TextArea) {
                ((TextArea) c).setEditable(b);
            }
        }
        return this;
    }

    /// Wraps `TextField#addDataChangedListener(com.codename1.ui.events.DataChangedListener)`
    ///
    /// #### Parameters
    ///
    /// - `l`
    public ComponentSelector addDataChangedListener(DataChangedListener l) {
        for (Component c : this) {
            if (c instanceof TextField) {
                ((TextField) c).addDataChangedListener(l);
            }
        }
        return this;
    }

    /// Wraps `TextField#removeDataChangedListener(com.codename1.ui.events.DataChangedListener)`
    ///
    /// #### Parameters
    ///
    /// - `l`
    public ComponentSelector removeDataChangedListener(DataChangedListener l) {
        for (Component c : this) {
            if (c instanceof TextField) {
                ((TextField) c).removeDataChangedListener(l);
            }
        }
        return this;
    }

    /// Wraps `TextField#setDoneListener(com.codename1.ui.events.ActionListener)`
    ///
    /// #### Parameters
    ///
    /// - `l`
    public ComponentSelector setDoneListener(ActionListener l) {
        for (Component c : this) {
            if (c instanceof TextField) {
                ((TextField) c).setDoneListener(l);
            }
        }
        return this;
    }

    /// Sets padding to all sides of found set components in pixels.
    ///
    /// #### Parameters
    ///
    /// - `padding`: Padding in pixels
    ///
    /// #### See also
    ///
    /// - #getStyle(com.codename1.ui.Component)
    public ComponentSelector setPadding(int padding) {
        return setPadding(padding, padding, padding, padding);
    }

    /// Strips margin and padding from components in found set.
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    ///
    /// #### Since
    ///
    /// 7.0
    ///
    /// #### See also
    ///
    /// - Style#stripMarginAndPadding()
    public ComponentSelector stripMarginAndPadding() {
        Style s = currentStyle();
        s.stripMarginAndPadding();
        return this;
    }

    // Now for the style stuff

    /// Sets padding to all components in found set.
    ///
    /// #### Parameters
    ///
    /// - `top`: Top padding in pixels.
    ///
    /// - `right`: Right padding in pixels
    ///
    /// - `bottom`: Bottom padding in pixels.
    ///
    /// - `left`: Left padding in pixels.
    ///
    /// #### See also
    ///
    /// - #getStyle(com.codename1.ui.Component)
    public ComponentSelector setPadding(int top, int right, int bottom, int left) {
        Style s = currentStyle();
        s.setPaddingUnit(Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS);
        s.setPadding(top, bottom, left, right);
        return this;
    }

    /// Sets padding to all components in found set in millimeters.
    ///
    /// #### Parameters
    ///
    /// - `top`: Top padding in mm.
    ///
    /// - `right`: Right padding in mm
    ///
    /// - `bottom`: Bottom padding in mm.
    ///
    /// - `left`: Left padding in mm.
    ///
    /// #### See also
    ///
    /// - #getStyle(com.codename1.ui.Component)
    public ComponentSelector setPaddingMillimeters(float top, float right, float bottom, float left) {
        return setPadding(dip2px(top), dip2px(right), dip2px(bottom), dip2px(left));
    }

    /// Sets padding in millimeters to all components in found set.
    ///
    /// #### Parameters
    ///
    /// - `topBottom`: Top and bottom padding in mm.
    ///
    /// - `leftRight`: Left and right padding in mm.
    ///
    /// #### See also
    ///
    /// - #getStyle(com.codename1.ui.Component)
    public ComponentSelector setPaddingMillimeters(float topBottom, float leftRight) {
        return setPaddingMillimeters(topBottom, leftRight, topBottom, leftRight);
    }

    /// Sets padding to all components in found set.
    ///
    /// #### Parameters
    ///
    /// - `padding`: Padding applied to all sides in mm.
    public ComponentSelector setPaddingMillimeters(float padding) {
        return setPaddingMillimeters(padding, padding);
    }

    /// Sets padding on all components in found set.
    ///
    /// #### Parameters
    ///
    /// - `topBottom`: Top and bottom padding in pixels.
    ///
    /// - `leftRight`: Left and right padding in pixels.
    public ComponentSelector setPadding(int topBottom, int leftRight) {
        return setPadding(topBottom, leftRight, topBottom, leftRight);
    }

    private int percentHeight(double percentage, Component parent) {
        return (int) Math.round((parent.getHeight() * percentage / 100));
    }

    private int percentWidth(double percentage, Component parent) {
        return (int) Math.round((parent.getWidth() * percentage / 100));
    }

    /// Sets the padding on all components in found set as a percentage of their respective
    /// parents' dimensions.  Horizontal padding is set as a percentage of parent width.  Vertical padding
    /// is set as a percentage of parent height.
    ///
    /// #### Parameters
    ///
    /// - `padding`: The padding expressed as a percent.
    public ComponentSelector setPaddingPercent(double padding) {
        return setPaddingPercent(padding, padding, padding, padding);

    }

    /// Sets padding on all components in found set as a percentage of their respective parents' dimensions.
    ///
    /// #### Parameters
    ///
    /// - `topBottom`: Top and bottom padding as percentage of parent heights.
    ///
    /// - `leftRight`: Left and right padding as percentage of parent widths.
    public ComponentSelector setPaddingPercent(double topBottom, double leftRight) {
        return setPaddingPercent(topBottom, leftRight, topBottom, leftRight);
    }

    /// Sets padding on all components in found set as a percentage of their respective parents' dimensions.
    ///
    /// #### Parameters
    ///
    /// - `top`: Top padding as percentage of parent height.
    ///
    /// - `right`: Right padding as percentage of parent width.
    ///
    /// - `bottom`: Bottom padding as percentage of parent height.
    ///
    /// - `left`: Left padding as percentage of parent width.
    public ComponentSelector setPaddingPercent(double top, double right, double bottom, double left) {
        for (Component c : this) {

            Component parent = c.getParent();
            if (parent != null) {
                // TODO : Change to currentStyle api... more complex with percents.
                Style s = getStyle(c);
                s.setPaddingUnit(Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS);
                s.setPadding(
                        percentHeight(top, parent),
                        percentHeight(bottom, parent),
                        percentWidth(left, parent),
                        percentWidth(right, parent)
                );
            }
        }
        return this;
    }

    /// Sets margin to all sides of found set components in pixels.
    ///
    /// #### Parameters
    ///
    /// - `margin`: Margin in pixels
    ///
    /// #### See also
    ///
    /// - #getStyle(com.codename1.ui.Component)
    public ComponentSelector setMargin(int margin) {
        return setMargin(margin, margin, margin, margin);
    }

    /// Sets margin to all components in found set.
    ///
    /// #### Parameters
    ///
    /// - `top`: Top margin in pixels.
    ///
    /// - `right`: Right margin in pixels
    ///
    /// - `bottom`: Bottom margin in pixels.
    ///
    /// - `left`: Left margin in pixels.
    ///
    /// #### See also
    ///
    /// - #getStyle(com.codename1.ui.Component)
    public ComponentSelector setMargin(int top, int right, int bottom, int left) {
        Style s = currentStyle();
        s.setMarginUnit(Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS);
        s.setMargin(top, bottom, left, right);
        return this;
    }

    /// Sets margin on all components in found set.
    ///
    /// #### Parameters
    ///
    /// - `topBottom`: Top and bottom margin in pixels.
    ///
    /// - `leftRight`: Left and right margin in pixels.
    public ComponentSelector setMargin(int topBottom, int leftRight) {
        return setMargin(topBottom, leftRight, topBottom, leftRight);
    }

    /// Sets the margin on all components in found set as a percentage of their respective
    /// parents' dimensions.  Horizontal margin is set as a percentage of parent width.  Vertical margin
    /// is set as a percentage of parent height.
    ///
    /// #### Parameters
    ///
    /// - `margin`: The margin expressed as a percent.
    public ComponentSelector setMarginPercent(double margin) {
        return setMarginPercent(margin, margin, margin, margin);

    }

    /// Sets margin on all components in found set as a percentage of their respective parents' dimensions.
    ///
    /// #### Parameters
    ///
    /// - `topBottom`: Top and bottom margin as percentage of parent heights.
    ///
    /// - `leftRight`: Left and right margin as percentage of parent widths.
    public ComponentSelector setMarginPercent(double topBottom, double leftRight) {
        return setMarginPercent(topBottom, leftRight, topBottom, leftRight);
    }

    /// Sets margin on all components in found set as a percentage of their respective parents' dimensions.
    ///
    /// #### Parameters
    ///
    /// - `top`: Top margin as percentage of parent height.
    ///
    /// - `right`: Right margin as percentage of parent width.
    ///
    /// - `bottom`: Bottom margin as percentage of parent height.
    ///
    /// - `left`: Left margin as percentage of parent width.
    public ComponentSelector setMarginPercent(double top, double right, double bottom, double left) {
        for (Component c : this) {

            Component parent = c.getParent();
            if (parent != null) {
                Style s = getStyle(c);
                s.setMarginUnit(Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS);
                s.setMargin(
                        percentHeight(top, parent),
                        percentHeight(bottom, parent),
                        percentWidth(left, parent),
                        percentWidth(right, parent)
                );
            }
        }
        return this;
    }

    /// Sets margin to all components in found set in millimeters.
    ///
    /// #### Parameters
    ///
    /// - `top`: Top margin in mm.
    ///
    /// - `right`: Right margin in mm
    ///
    /// - `bottom`: Bottom margin in mm.
    ///
    /// - `left`: Left margin in mm.
    ///
    /// #### See also
    ///
    /// - #getStyle(com.codename1.ui.Component)
    public ComponentSelector setMarginMillimeters(float top, float right, float bottom, float left) {
        return setMargin(dip2px(top), dip2px(right), dip2px(bottom), dip2px(left));
    }

    /// Sets margin in millimeters to all components in found set.
    ///
    /// #### Parameters
    ///
    /// - `topBottom`: Top and bottom margin in mm.
    ///
    /// - `leftRight`: Left and right margin in mm.
    ///
    /// #### See also
    ///
    /// - #getStyle(com.codename1.ui.Component)
    public ComponentSelector setMarginMillimeters(float topBottom, float leftRight) {
        return setMarginMillimeters(topBottom, leftRight, topBottom, leftRight);
    }

    /// Sets margin to all components in found set.
    ///
    /// #### Parameters
    ///
    /// - `margin`: Margin applied to all sides in mm.
    public ComponentSelector setMarginMillimeters(float margin) {
        return setMarginMillimeters(margin, margin);
    }

    /// Creates a proxy style to mutate the styles of all component styles in found set.
    ///
    /// #### See also
    ///
    /// - #getStyle(com.codename1.ui.Component)
    ///
    /// - Style#createProxyStyle(com.codename1.ui.plaf.Style...)
    public Style createProxyStyle() {
        HashSet<Style> styles = new HashSet<Style>();
        for (Component c : this) {
            styles.add(this.getStyle(c));
        }
        return Style.createProxyStyle(styles.toArray(new Style[styles.size()]));
    }

    /// Merges style with all styles of components in current found set.
    ///
    /// #### Parameters
    ///
    /// - `style`
    ///
    /// #### See also
    ///
    /// - Style#merge(com.codename1.ui.plaf.Style)
    ///
    /// - #getStyle(com.codename1.ui.Component)
    public ComponentSelector merge(Style style) {
        for (Component c : this) {
            getStyle(c).merge(style);
        }
        return this;
    }

    /// Wraps `Style#setBgColor(int)`
    ///
    /// #### Parameters
    ///
    /// - `bgColor`
    ///
    /// #### See also
    ///
    /// - #getStyle(com.codename1.ui.Component)
    public ComponentSelector setBgColor(int bgColor) {
        currentStyle().setBgColor(bgColor);
        return this;
    }

    /// Wraps `Style#setAlignment(int)`
    ///
    /// #### Parameters
    ///
    /// - `alignment`
    ///
    /// #### See also
    ///
    /// - #getStyle(com.codename1.ui.Component)
    public ComponentSelector setAlignment(int alignment) {
        currentStyle().setAlignment(alignment);
        return this;
    }

    /// Wraps `Style#setBgImage(com.codename1.ui.Image)`
    ///
    /// #### Parameters
    ///
    /// - `bgImage`
    ///
    /// #### See also
    ///
    /// - #getStyle(com.codename1.ui.Component)
    public ComponentSelector setBgImage(Image bgImage) {
        currentStyle().setBgImage(bgImage);
        return this;
    }

    /// Wraps `Style#setBackgroundType(byte)`
    ///
    /// #### Parameters
    ///
    /// - `backgroundType`
    ///
    /// #### See also
    ///
    /// - #getStyle(com.codename1.ui.Component)
    public ComponentSelector setBackgroundType(byte backgroundType) {
        currentStyle().setBackgroundType(backgroundType);
        return this;
    }

    /// Wraps `Style#setBackgroundGradientStartColor(int)`
    ///
    /// #### Parameters
    ///
    /// - `startColor`
    ///
    /// #### See also
    ///
    /// - #getStyle(com.codename1.ui.Component)
    public ComponentSelector setBackgroundGradientStartColor(int startColor) {
        currentStyle().setBackgroundGradientStartColor(startColor);
        return this;
    }

    /// Wraps `(int)`
    ///
    /// #### Parameters
    ///
    /// - `endColor`
    ///
    /// #### See also
    ///
    /// - #getStyle(com.codename1.ui.Component)
    public ComponentSelector setBackgroundGradientEndColor(int endColor) {
        currentStyle().setBackgroundGradientEndColor(endColor);
        return this;
    }

    /// Wraps `Style#setBackgroundGradientRelativeX(float)`
    ///
    /// #### Parameters
    ///
    /// - `x`
    ///
    /// #### See also
    ///
    /// - #getStyle(com.codename1.ui.Component)
    public ComponentSelector setBackgroundGradientRelativeX(float x) {
        currentStyle().setBackgroundGradientRelativeX(x);
        return this;
    }

    /// Wraps `Style#setBackgroundGradientRelativeY(float)`
    ///
    /// #### Parameters
    ///
    /// - `y`
    ///
    /// #### See also
    ///
    /// - #getStyle(com.codename1.ui.Component)
    public ComponentSelector setBackgroundGradientRelativeY(float y) {
        currentStyle().setBackgroundGradientRelativeY(y);
        return this;
    }

    /// Wraps `Style#setBackgroundGradientRelativeSize(float)`
    ///
    /// #### Parameters
    ///
    /// - `size`
    ///
    /// #### See also
    ///
    /// - #getStyle(com.codename1.ui.Component)
    public ComponentSelector setBackgroundGradientRelativeSize(float size) {
        currentStyle().setBackgroundGradientRelativeSize(size);
        return this;
    }

    /// Wraps `Style#setFgColor(int)`
    ///
    /// #### Parameters
    ///
    /// - `color`
    ///
    /// #### See also
    ///
    /// - #getStyle(com.codename1.ui.Component)
    public ComponentSelector setFgColor(int color) {
        currentStyle().setFgColor(color);
        return this;
    }

    /// Wraps `Style#setFont(com.codename1.ui.Font)`
    ///
    /// #### Parameters
    ///
    /// - `f`
    ///
    /// #### See also
    ///
    /// - #getStyle(com.codename1.ui.Component)
    public ComponentSelector setFont(Font f) {
        currentStyle().setFont(f);
        return this;
    }

    /// Wraps `Style#setUnderline(boolean)`
    ///
    /// #### Parameters
    ///
    /// - `b`
    ///
    /// #### See also
    ///
    /// - #getStyle(com.codename1.ui.Component)
    public ComponentSelector setUnderline(boolean b) {
        currentStyle().setUnderline(b);
        return this;
    }

    /// Wraps `boolean)`
    ///
    /// #### Parameters
    ///
    /// - `t`
    ///
    /// - `raised`
    ///
    /// #### See also
    ///
    /// - #getStyle(com.codename1.ui.Component)
    public ComponentSelector set3DText(boolean t, boolean raised) {
        currentStyle().set3DText(t, raised);
        return this;
    }

    /// Wraps `Style#set3DTextNorth(boolean)`
    ///
    /// #### Parameters
    ///
    /// - `north`
    ///
    /// #### See also
    ///
    /// - #getStyle(com.codename1.ui.Component)
    public ComponentSelector set3DTextNorth(boolean north) {
        currentStyle().set3DTextNorth(north);
        return this;
    }

    /// Wraps `Style#setOverline(boolean)`
    ///
    /// #### Parameters
    ///
    /// - `b`
    ///
    /// #### See also
    ///
    /// - #getStyle(com.codename1.ui.Component)
    public ComponentSelector setOverline(boolean b) {
        currentStyle().setOverline(b);
        return this;
    }

    /// Wraps `Style#setStrikeThru(boolean)`
    ///
    /// #### Parameters
    ///
    /// - `b`
    ///
    /// #### See also
    ///
    /// - #getStyle(com.codename1.ui.Component)
    public ComponentSelector setStrikeThru(boolean b) {
        currentStyle().setStrikeThru(b);
        return this;
    }

    /// Wraps `Style#setTextDecoration(int)`
    ///
    /// #### Parameters
    ///
    /// - `textDecoration`
    ///
    /// #### See also
    ///
    /// - #getStyle(com.codename1.ui.Component)
    public ComponentSelector setTextDecoration(int textDecoration) {
        currentStyle().setTextDecoration(textDecoration);
        return this;
    }

    /// Wraps `Style#setBgTransparency(byte)`
    ///
    /// #### Parameters
    ///
    /// - `bgTransparency`
    ///
    /// #### See also
    ///
    /// - #getStyle(com.codename1.ui.Component)
    public ComponentSelector setBgTransparency(int bgTransparency) {
        currentStyle().setBgTransparency(bgTransparency);
        return this;
    }

    /// Wraps `Style#setOpacity(int)`
    ///
    /// #### Parameters
    ///
    /// - `opacity`
    ///
    /// #### See also
    ///
    /// - #getStyle(com.codename1.ui.Component)
    public ComponentSelector setOpacity(int opacity) {
        currentStyle().setOpacity(opacity);

        return this;
    }

    /// Wraps `Style#addStyleListener(com.codename1.ui.events.StyleListener)`
    ///
    /// #### Parameters
    ///
    /// - `l`
    ///
    /// #### See also
    ///
    /// - #getStyle(com.codename1.ui.Component)
    public ComponentSelector addStyleListener(StyleListener l) {
        currentStyle().addStyleListener(l);
        return this;
    }

    /// Wraps `Style#removeStyleListener(com.codename1.ui.events.StyleListener)`
    ///
    /// #### Parameters
    ///
    /// - `l`
    ///
    /// #### See also
    ///
    /// - #getStyle(com.codename1.ui.Component)
    public ComponentSelector removeStyleListener(StyleListener l) {
        currentStyle().removeStyleListener(l);
        return this;
    }

    /// Wraps `Style#removeListeners()`
    ///
    /// #### See also
    ///
    /// - #getStyle(com.codename1.ui.Component)
    public ComponentSelector removeStyleListeners() {
        currentStyle().removeListeners();
        return this;
    }

    /// Wraps `Style#setBorder(com.codename1.ui.plaf.Border)`
    ///
    /// #### Parameters
    ///
    /// - `b`
    ///
    /// #### See also
    ///
    /// - #getStyle(com.codename1.ui.Component)
    public ComponentSelector setBorder(Border b) {
        currentStyle().setBorder(b);
        return this;
    }

    /// Wraps `Style#setBgPainter(com.codename1.ui.Painter)`
    ///
    /// #### Parameters
    ///
    /// - `bgPainter`
    ///
    /// #### See also
    ///
    /// - #getStyle(com.codename1.ui.Component)
    public ComponentSelector setBgPainter(Painter bgPainter) {
        currentStyle().setBgPainter(bgPainter);
        return this;
    }

    private float getEffectiveFontSize(Component c) {
        Font f = c.getStyle().getFont();

        while (f == null && c != null) {
            c = c.getParent();
            if (c != null) {
                f = c.getStyle().getFont();
            }
        }
        if (f != null) {
            return f.getPixelSize();
        }
        return Font.getDefaultFont().getHeight() * 1.6f;
    }

    /// Sets the font size of all components in found set.  In pixels.
    ///
    /// #### Parameters
    ///
    /// - `size`: Font size in pixels.
    ///
    /// #### See also
    ///
    /// - #getStyle(com.codename1.ui.Component)
    public ComponentSelector setFontSize(float size) {
        for (Component c : this) {
            Style style = getStyle(c);
            Font curr = style.getFont();
            if (curr == null || !curr.isTTFNativeFont()) {
                curr = c.getStyle().getFont();
            }

            if (curr == null || !curr.isTTFNativeFont()) {
                Component parent = c.getParent();
                while (parent != null && (curr == null || !curr.isTTFNativeFont())) {
                    curr = parent.getStyle().getFont();
                    parent = parent.getParent();
                }
            }
            if (curr == null || !curr.isTTFNativeFont()) {
                curr = Font.create("native:MainRegular");
            }

            if (curr != null && curr.isTTFNativeFont()) {
                curr = curr.derive(size, 0);
                style.setFont(curr);
            }

        }
        return this;
    }

    /// Sets the material icon of all labels in the set.
    ///
    /// #### Parameters
    ///
    /// - `icon`: Material icon to set.  See `char)`
    ///
    /// - `size`: The icon size in millimeters.
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    ///
    /// #### Since
    ///
    /// 7.0
    public ComponentSelector setMaterialIcon(char icon, float size) {
        for (Component c : this) {
            if (c instanceof Label) {
                FontImage.setMaterialIcon((Label) c, icon, size);
            }
        }
        return this;
    }

    public ComponentSelector setCursor(int cursor) {
        for (Component c : this) {
            c.setCursor(cursor);
        }
        return this;
    }

    /// Sets the font size of all components in found set.  In millimeters.
    ///
    /// #### Parameters
    ///
    /// - `sizeMM`: Font size in mm.
    public ComponentSelector setFontSizeMillimeters(float sizeMM) {
        return setFontSize(dip2px(sizeMM));
    }

    /// Sets the fonts size of all components in the found set as a percentage of the font
    /// size of the components' respective parents.
    ///
    /// #### Parameters
    ///
    /// - `sizePercentage`: Font size as a percentage of parent font size.
    public ComponentSelector setFontSizePercent(double sizePercentage) {
        for (Component c : this) {
            Component parent = c.getParent();
            if (parent != null) {
                float size = (float) (getEffectiveFontSize(parent) * sizePercentage / 100.0);
                Style style = getStyle(c);

                Font curr = style.getFont();
                if (curr == null || !curr.isTTFNativeFont()) {
                    curr = c.getStyle().getFont();
                }

                if (curr == null || !curr.isTTFNativeFont()) {
                    parent = c.getParent();
                    while (parent != null && (curr == null || !curr.isTTFNativeFont())) {
                        curr = parent.getStyle().getFont();
                        parent = parent.getParent();
                    }
                }
                if (curr == null || !curr.isTTFNativeFont()) {
                    curr = Font.create("native:MainRegular");
                }

                if (curr != null && curr.isTTFNativeFont()) {
                    curr = curr.derive(size, 0);
                    style.setFont(curr);
                }
            }
        }
        return this;
    }

    /// Returns a set with the first element of the current set, or an empty set if the
    /// current set is empty.
    ///
    /// #### Returns
    ///
    /// A ComponentSelector with 0 or 1 element.
    ///
    /// #### Since
    ///
    /// 7.0
    public ComponentSelector first() {
        if (!isEmpty()) {
            return new ComponentSelector(results.iterator().next());
        } else {
            return new ComponentSelector();
        }
    }

    /// Interface used for providing callbacks that receive a Component as input.
    public interface ComponentClosure {

        /// Callback to apply.
        ///
        /// #### Parameters
        ///
        /// - `c`: Component that is passed to the closure.
        void call(Component c);
    }

    /// Interface used by `#map(com.codename1.ui.ComponentSelector.ComponentMapper)` to form a new set of
    /// components based on the components in one set.
    public interface ComponentMapper {

        /// Maps component c to a replacement component.
        ///
        /// #### Parameters
        ///
        /// - `c`: The source component.
        ///
        /// #### Returns
        ///
        /// The component that should replace c in the new set.
        Component map(Component c);
    }

    /// Interface used by `#filter(com.codename1.ui.ComponentSelector.Filter)` to form a new set of
    /// components based on the components in one set.
    public interface Filter {

        /// Determines whether component c should be included in new set.
        ///
        /// #### Parameters
        ///
        /// - `c`: The component to test for inclusion in new set.
        ///
        /// #### Returns
        ///
        /// True if c should be included in new set.  False otherwise
        boolean filter(Component c);
    }


}
