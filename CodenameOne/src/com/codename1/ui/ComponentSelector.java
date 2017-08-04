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


import com.codename1.components.OnOffSwitch;
import com.codename1.components.SpanButton;
import com.codename1.components.SpanLabel;
import com.codename1.io.Util;
import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.animations.ComponentAnimation;
import com.codename1.ui.animations.Transition;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * A tool to facilitate selection and manipulation of UI components as sets.  This uses fluent API style, similar
 * to jQuery to make it easy to find UI components and modify them as groups. 
 * 
 * <h3>Set Selection</h3>
 * 
 * <p>Sets of components can either be created by explicitly adding components to the set, or by
 * providing a "selector" string that specifies how the set should be formed.  Some examples:</p>
 * 
 * <ul>
 *   <li>{@code $("Label") }  - The set of all components on the current form with UIID="Label"</li>
 *   <li>{@code $("#AddressField") } - The set of components with name="AddressField"</li>
 *   <li>{@code $("TextField#AddressField") } - The set of components with UIID=TextField and Name=AddressField</li>
 *   <li>{@code $("Label, Button")} - Set of components in current form with UIID="Label" or UIID="Button"</li>
 *   <li>{@code $("Label", myContainer)} - Set of labels within the container {@code myContainer}</li>
 *   <li>{@code $("MyContainer *") } - All descendants of the container with UIID="MyContainer".  Will not include "MyContainer".</li>
 *   <li>{@code $("MyContainer > *") } - All children of of the container with UIID="MyContainer".</li>
 *   <li>{@code $("MyContainer > Label) } - All children with UIID=Label of container with UIID=MyContainer</li>
 *   <li>{@code $("Label").getParent() } - All parent components of labels in the current form.</li>
 * </ul>
 * 
 * <h4>Tags</h4>
 * 
 * <p>To make selection more flexible, you can "tag" components so that they can be easily targeted by a selector.  
 * You can add tags to components using {@link #addTags(java.lang.String...) }, and remove them using {@link #removeTags(java.lang.String...) }.
 * Once you have tagged a component, it can be targeted quite easily using a selector.  Tags are specified in a selector with a {@literal .} 
 * prefix.  E.g.:</p>
 * 
 * <ul>
 *  <li>{@code $(".my-tag") } - The set of all components with tag "my-tag".</li>
 *  <li>{@code $("Label.my-tag") } - The set of all components with tag "my-tag" and UIID="Label"</li>
 *  <li>{@code $("Label.my-tag.myother-tag")} - The set of all components with tags "my-tag" and "myother-tag" and UIID="Label".  Matches only components that include all of those tags.</li>
 * </ul>
 * 
 * <h3>Modifying Components in Set</h3>
 * 
 * <p>While component selection alone in the ComponentSelector is quite powerful, the true power comes when
 * you start to operate on the entire set of components using the Fluent API of ComponentSelector.  ComponentSelector
 * includes wrapper methods for most of the mutator methods of {@link Component}, {@link Container}, and a few other common
 * component types.</p>
 * 
 * <p>For example, the following two snippets are equivalent:</p>
 * 
 * <pre>
 * {@code
 * for (Component c : $("Label")) {
 *     c.getStyle().setFgColor(0xff0000);
 * }
 * }
 * </pre>
 * 
 * and
 * 
 * <pre>
 * {@code 
 * $("Label").setFgColor(0xff0000);
 * }
 * </pre>
 * 
 * <p>The second snippet is clearly easier to type and more compact.  But we can take it further.  The Fluent API
 * style allows you to chain together multiple method calls.  This even makes it desirable to operate on
 * single-element sets.  E.g.:</p>
 * 
 * <pre>
 * {@code
 * Button myButton = $(new Button("Some text"))
        .setUIID("Label")
        .addTags("cell", "row-"+rowNum, "col-"+colNum, rowNum%2==0 ? "even":"odd")
        .putClientProperty("row", rowNum)
        .putClientProperty("col", colNum)
        .asComponent(Button.class);
 * }
 * </pre>
 * 
 * <p>
 * The above snippet wraps a new Button in a ComponentSelector, then uses the fluent API to apply several properties
 * to the button, before using {@link #asComponent()} to return the Button itself.
 * </p>
 * 
 * <h3>API Overview</h3>
 * 
 * <p>ComponentSelector includes a few different types of methods:</p>
 * 
 * <ol>
 *   <li>Wrapper methods for {@link Component}, {@link Container}, etc... to operate on all applicable components in the set.</li>
 *   <li>Component Tree Traversal Methods to return other sets of components based on the current set.  E.g. {@link #find(java.lang.String) }, {@link #getParent()}, {@link #getComponentAt(int) },
 * {@link #closest(java.lang.String) }, {@link #nextSibling() }, {@link #prevSibling() }, {@link #parents(java.lang.String) }, 
 * {@link #getComponentForm() }, and many more.</li>
 *   <li>Effects. E.g. {@link #fadeIn() }, {@link #fadeOut() }, {@link #slideDown() }, {@link #slideUp() }, {@link #animateLayout(int) },
 * {@link #animateHierarchy(int) }, etc..</li>
 *   <li>Convenience methods to help with common tasks.  E.g. {@link #$(java.lang.Runnable) } as a short-hand for {@link Display#callSerially(java.lang.Runnable) }
 *   <li>Methods to implement {@link java.util.Set} because ComponentSelector is a set.</li>
 * </ol>
 * 
 * 
 * <h3>Effects</h3>
 * 
 * <p>The following is an example form that demonstrates the use of ComponentSelector to
 * easily create effects on components in a form.</p>
 * 
 * <p><script src="https://gist.github.com/shannah/536f852c3b7242a4d3106fc9e5b5d147.js"></script>
 * </p>
 * 
 * <h3>Advanced Use of Tags</h3>
 * 
 * <p>The following shows the use of tags to help with striping a table, and selecting rows
 * when clicked on.</p>
 * 
 * <p><script src="https://gist.github.com/shannah/6da5728888e01abb54486a02b8c1a7c9.js"></script>
 * </p>
 * 
 * <p>See full Demo App in this  <a href="https://github.com/shannah/cn1-component-selector-demo">Github Repo</a></p>
 * 
 * <h3>Modifying Style Properties</h3>
 * 
 * <p>Modifying styles deserves special mention because components have multiple {@link Style} objects associated with them.  
 * {@link Component#getStyle() } returns the style of the component in its current state.  {@link Component#getPressedStyle() }
 * gets the pressed style of the component, {@link Component#getSelectedStyle() } get its selected style, etc..</p>
 * 
 * <p>ComponentSelector wraps each of the {@literal getXXXStyle() } methods of {@link Component} with corresponding methods
 * that return proxy styles for all components in the set.  {@link #getStyle() } returns a proxy {@link Style} that proxies
 * all of the styles returned from each of the {@link Component#getStyle()} methods in the set.  {@link #getPressedStyle() } returns
 * a proxy for all of the pressed styles, etc..</p>
 * 
 * <p>Example Modifying Text Color of All Buttons in a container when they are pressed only</p>
 * 
 * <pre>
 * {@code
 * Style pressed = $("Button", myContainer).getPressedStyle();
 * pressed.setFgColor(0xff0000);
 * }
 * </pre>
 * 
 * <p>A slightly more elegant pattern would be to use the {@link #selectPressedStyle() } method to set the default
 * style for mutations to "pressed".  Then we could use the fluent API of ComponentSelector to chain multiple style
 * mutations.  E.g.:
 * </p>
 * 
 * <pre>
 * {@code 
 * $("Button", myContainer)
 *     .selectPressedStyle()
 *     .setFgColor(0xffffff)
 *     .setBgColor(0x0)
 *     .setBgTransparency(255);
 * }
 * </pre>
 * 
 * <p>A short-hand for this would be to add the {@literal :pressed} pseudo-class to the selector.  E.g.</p>
 * 
 * <pre>
 * {@code 
 * $("Button:pressed", myContainer)
 *     .setFgColor(0xffffff)
 *     .setBgColor(0x0)
 *     .setBgTransparency(255);
 * }
 * </pre>
 * 
 * <p>The following style pseudo-classes are supported:</p>
 * 
 * <ul>
 *    <li>{@literal :pressed} - Same as calling {@link #selectPressedStyle() }</li>
 *    <li>{@literal :selected} - Same as calling {@link #selectSelectedStyle() }</li>
 *    <li>{@literal :unselected} - Same as calling {@link #selectUnselectedStyle() }</li>
 *    <li>{@literal :all} - Same as calling {@link #selectAllStyles() }</li>
 *    <li>{@literal :*} - Alias for {@literal :all}</li>
 *    <li>{@literal :disabled} - Same as calling {@link #selectDisabledStyle() }</li>
 * </ul>
 * 
 * <p>You can chain calls to {@literal selectedXXXStyle()}, enabling to chain together mutations of multiple different
 * style properties.  E.g  To change the pressed foreground color, and then change the selected foreground color, you could do:</p>
 * 
 * <pre>
 * {@code
 * $("Button", myContainer)
 *    .selectPressedStyle()
 *    .setFgColor(0x0000ff)
 *    .selectSelectedStyle()
 *    .setFgColor(0x00ff00);
 * }
 * </pre>
 * 
 * <h3>Filtering Sets</h3>
 * 
 * <p>There are many ways to remove components from a set.  Obviously you can use the standard {@link java.util.Set } methods
 * to explicitly remove components from your set:</p>
 * 
 * <pre>
 * {@code
 * ComponentSelector sel = $("Button").remove(myButton, true);
 *     // The set of all buttons on the current form, except myButton
 * }
 * </pre>
 * 
 * or
 * 
 * <pre>
 * {@code
 * ComponentSelector sel = $("Button").removeAll($(".some-tag"), true);
 *    // The set of all buttons that do NOT contain the tag ".some-tag"
 * }
 * </pre>
 * 
 * <p>You could also use the {@link #filter(com.codename1.ui.ComponentSelector.Filter) } to explicitly 
 * declare which elements should be kept, and which should be discarded:</p>
 * 
 * <pre>
 * {@code 
 * ComponentSelector sel = $("Button").filter(c->{
 *     return c.isVisible();
 * });
 *     // The set of all buttons that are currently visible.
 * }
 * </pre>
 * 
 * <h3>Tree Navigation</h3>
 * 
 * <p>
 * One powerful aspect of working with sets of components is that you can generate very specific
 * sets of components using very simple queries.  Consider the following queries:</p>
 * 
 * <ul>
 *     <li>{@code $(myButton1, myButton2).getParent()} - The set of parents of {@literal myButton1} and {@literal myButton2}.  If they have the
 * same parent, then this set will only contain a single element: the common parent container.   If they have different parents, then this
 * set will include both parent containers.</li>
 *     <li>{@code $(myButton).getParent().find(">TextField")} - The set of siblings of {@literal myButton} that have UIID=TextField</li>
 *     <li>{@code $(myButton).closest(".some-tag")} - The set containing the "nearest" parent container of {@literal myButton} that has 
 * the tag ".some-tag".  If there are no matching components, then this will be an empty set.  This is formed by crawling up the tree 
 * until it finds a matching component.  Works the same as jQuery's {@literal closest() } method.</li>
 *     <li>{@code $(".my-tag").getComponentAt(4)} - The set of 5th child components of containers with tag ".my-tag".</li>
 * </ul>
 * 
 * @author shannah
 */
public class ComponentSelector implements Iterable<Component>, Set<Component> {
    private static final String PROPERTY_TAG = "com.codename1.ui.ComponentSelector#tags";
    private String name;
    private String uiid;
    private String[] tags;
    private String[] tagsNeedles;
    private String state;
    private ComponentSelector parent;
    private Set<ComponentSelector> aggregateSelectors;
    
    private final Set<Component> roots;
    private Set<Component> results;
    private boolean childrenOnly = false;
    private Style currentStyle = null;
    private int currentStyleType = 0;
    
    private static final int ALL_STYLES=1;
    private static final int PRESSED_STYLE=2;
    private static final int SELECTED_STYLE=3;
    private static final int UNSELECTED_STYLE=4;
    private static final int DISABLED_STYLE=5;
    
    
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
    
    /**
     * Interface used for providing callbacks that receive a Component as input.
     */
    public static interface ComponentClosure {

        /**
         * Callback to apply.
         * @param c Component that is passed to the closure.
         */
        public void call(Component c);
    }
    
    /**
     * Interface used by {@link #map(com.codename1.ui.ComponentSelector.ComponentMapper) } to form a new set of 
     * components based on the components in one set.
     */
    public static interface ComponentMapper {

        /**
         * Maps component {@literal c } to a replacement component.
         * @param c The source component.
         * @return The component that should replace {@literal c} in the new set.
         */
        public Component map(Component c);
    }
    
    /**
     * Interface used by {@link #filter(com.codename1.ui.ComponentSelector.Filter) } to form a new set of 
     * components based on the components in one set.
     */
    public static interface Filter {

        /**
         * Determines whether component {@literal c} should be included in new set.  
         * @param c The component to test for inclusion in new set.
         * @return True if {@literal c } should be included in new set.  False otherwise
         */
        public boolean filter(Component c);
    }
    
    /**
     * Wraps provided components in a ComponentSelector set.
     * @param cmps Components to be includd in the set.
     * @return ComponentSelector with specified components.
     */
    public static ComponentSelector $(Component... cmps) {
        return new ComponentSelector(cmps);
    }
    
    /**
     * Alias of {@link #$(com.codename1.ui.Component...) }
     * @param cmps
     * @return 
     */
    public static ComponentSelector select(Component... cmps) {
        return $(cmps);
    }
    
    
    private void setDirty() {
        currentStyle = null;
    }
    
    /**
     * Creates a ComponentInspector with the source component of the provided event.
     * @param e The event whose source component is added to the set.
     * @return A ComponentSelector with a single component - the source of the event.
     */
    public static ComponentSelector $(ActionEvent e) {
        Object src = e.getSource();
        if (src == null) {
            return new ComponentSelector();
        } else if (src instanceof Component) {
            return new ComponentSelector((Component)src);
        }
        return new ComponentSelector();
    }
    
    /**
     * Alias of {@link #$(com.codename1.ui.events.ActionEvent) }
     * @param e
     * @return 
     */
    public static ComponentSelector select(ActionEvent e) {
        return $(e);
    }
    
    /**
     * Wraps {@link Display#callSerially(java.lang.Runnable) }
     * @param r
     * @return Empty ComponentSelector.
     */
    public static ComponentSelector $(Runnable r) {
        Display.getInstance().callSerially(r);
        return new ComponentSelector();
    }
    
    /**
     * Alias of {@link #$(java.lang.Runnable) }
     * @param r
     * @return 
     */
    public static ComponentSelector select(Runnable r) {
        return $(r);
    }

    
    /**
     * Applies the given callback to each component in the set.
     * @param closure Callback which will be called once for each component in the set.
     * @return Self for chaining.
     */
    public ComponentSelector each(ComponentClosure closure) {
        for (Component c : this) {
            closure.call(c);
        }
        return this;
    }
    
    /**
     * Creates a new set based on the elements of the current set and a mapping function
     * which defines the elements that should be in the new set. 
     * @param mapper The mapper which will be called once for each element in the set.  The return value
     * of the mapper function dictates which component should be included in the resulting set. 
     * @return A new set of components.
     */
    public ComponentSelector map(ComponentMapper mapper) {
        HashSet<Component> out = new HashSet<Component>();
        for (Component c : this) {
            Component res = mapper.map(c);
            if (res != null) {
                out.add(res);
            }
        }
        return new ComponentSelector(out);
    }
    
    /**
     * Creates a new set of components formed by filtering the current set using a filter function.
     * @param filter The filter function called for each element in the set.  If it returns true,
     * then the element is included in the resulting set.  If false, it will not be included.
     * @return A new set with the results of the filter.
     */
    public ComponentSelector filter(Filter filter) {
        HashSet<Component> out = new HashSet<Component>();
        for (Component c : this) {
            boolean res = filter.filter(c);
            if (res) {
                out.add(c);
            }
        }
        return new ComponentSelector(out);
    }
    
    /**
     * Creates a new set of components consisting of all of the parents of components in this set.
     * Only parent components matching the provided selector will be included in the set.
     * @param selector Selector to filter the parent components.
     * @return New set with parents of elements in current set.
     */
    public ComponentSelector parent(String selector) {
        ComponentSelector matcher = new ComponentSelector(selector, new Label());
        HashSet<Component> matches = new HashSet<Component>();
        for (Component c : this) {
            Component parent = c.getParent();
            if (parent != null && matcher.match(parent)) {
                matches.add(parent);
            }
        }
        return matcher.addAll(matches, true);
    }
    
    /**
     * Creates new set of components consisting of all of the ancestors of components in this set which
     * match the provided selector.
     * @param selector The selector to filter the ancestors.
     * @return New set with ancestors of elements in current set.
     */
    public ComponentSelector parents(String selector) {
        ComponentSelector matcher = new ComponentSelector(selector, new Label());
        HashSet<Component> matches = new HashSet<Component>();
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
    
    /**
     * Creates a new set of components consistng of all "closest" ancestors of components
     * in this set which match the provided selector. 
     * @param selector The selector to use to match the nearest ancestor.
     * @return New set with ancestors of components in current set.
     */
    public ComponentSelector closest(String selector) {
        ComponentSelector matcher = new ComponentSelector(selector, new Label());
        HashSet<Component> matches = new HashSet<Component>();
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
    
    /**
     * Creates new set consisting of the first child of each component in the current set.
     * @return New set with first child of each component in current set.
     */
    public ComponentSelector firstChild() {
        HashSet<Component> out = new HashSet<Component>();
        for (Component c : this) {
            if (c instanceof Container) {
                Container cnt = (Container)c;
                if (cnt.getComponentCount() > 0) {
                    out.add(cnt.getComponentAt(0));
                }
            }
        }
        return new ComponentSelector(out);
    }
    
    /**
     * Creates new set consisting of the last child of each component in the current set.
     * @return New set with last child of each component in current set.
     */
    public ComponentSelector lastChild() {
        HashSet<Component> out = new HashSet<Component>();
        for (Component c : this) {
            if (c instanceof Container) {
                Container cnt = (Container)c;
                if (cnt.getComponentCount() > 0) {
                    out.add(cnt.getComponentAt(cnt.getComponentCount()-1));
                }
            }
        }
        return new ComponentSelector(out);
    }
    
    /**
     * Creates set of "next" siblings of components in this set.
     * @return New ComponentSelector with next siblings of this set.
     */
    public ComponentSelector nextSibling() {
        HashSet<Component> out = new HashSet<Component>();
        for (Component c : this) {
            Container parent = c.getParent();
            if (parent != null) {
                int index = parent.getComponentIndex(c);
                if (index < parent.getComponentCount()-1) {
                    out.add(parent.getComponentAt(index+1));
                }
            }
        }
        return new ComponentSelector(out);
    }
    
    /**
     * Creates set of "previous" siblings of components in this set.
     * @return New ComponentSelector with previous siblings of this set.
     */
    public ComponentSelector prevSibling() {
        HashSet<Component> out = new HashSet<Component>();
        for (Component c : this) {
            Container parent = c.getParent();
            if (parent != null) {
                int index = parent.getComponentIndex(c);
                if (index > 0) {
                    out.add(parent.getComponentAt(index-1));
                }
            }
        }
        return new ComponentSelector(out);
    }
    
    
    
    /**
     * Animates this set of components, replacing any modified style properties of the
     * destination style to the components.
     * @param destStyle The style to apply to the components via animation.
     * @param duration The duration of the animation (ms)
     * @param callback Callback to call after animation is complete.
     * @return Self for chaining
     * @see Component#createStyleAnimation(java.lang.String, int) 
     */
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
                public void run() {
                    if (callback != null) {
                        callback.onSucess(ComponentSelector.this);
                    }
                }
            });
            
        }
        return this;
    }
    
    /**
     * Fade in this set of components.  Prior to calling this, the component visibility
     * should be set to "false".    This uses the default duration of 500ms.
     * @return Self for chaining.
     */
    public ComponentSelector fadeIn() {
        return fadeIn(500);
    }
    
    /**
     * Fade in this set of components.  Prior to calling this, the component visibility
     * should be set to "false". 
     * @param duration The duration of the fade in.
     * @return Self for chaining.
     */
    public ComponentSelector fadeIn(int duration) {
        return fadeIn(duration, null);
    }
    
    /**
     * Fade in this set of components.  Prior to calling this, the component visibility should
     * be set to "false". 
     * @param duration The duration of the fade in.
     * @param callback Callback to run when animation completes.
     * @return 
     */
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

                public void run() {
                    AnimationManager mgr = null;
                    for (final Component c : animatingComponents) {
                        
                        Container placeholder = (Container)c.getClientProperty(placeholderProperty);
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
                    if (mgr != null) {
                        final AnimationManager fmgr = mgr;
                        $(new Runnable() {
                            public void run() {
                                fmgr.addAnimation(ComponentAnimation.compoundAnimation(animations2.toArray(new ComponentAnimation[animations2.size()])), new Runnable() {
                                    public void run() {
                                        if (callback != null) {
                                            callback.onSucess(ComponentSelector.this);
                                        }
                                    }

                                });
                            }
                        });
                        
                    }
                    
                    
                }
                
            });
        }
        
        return this;
    }
    
    /**
     * Fades in this component and blocks until animation is complete.
     * @return Self for chaining.
     */
    public ComponentSelector fadeInAndWait() {
        return fadeInAndWait(500);
    }
    
    /**
     * Fades in this component and blocks until animation is complete.
     * @param duration The duration of the animation.
     * @return Self for chaining.
     */
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

                Container placeholder = (Container)c.getClientProperty(placeholderProperty);
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
            if (mgr != null) {
                mgr.addAnimationAndBlock(
                        ComponentAnimation.compoundAnimation(animations2.toArray(new ComponentAnimation[animations2.size()]))
                );

            }
        }
        
        return this;
    }
    
    /**
     * Returns true if the first component in this set is visible.
     * @return True if first component in this set is visible.
     * @see Component#isVisible() 
     */
    public boolean isVisible() {
        for (Component c : this) {
            return c.isVisible();
        }
        return false;
    }
    
    /**
     * Returns true if first component in this set is hidden.
     * @return True if first component in set is hidden.
     * @see Component#isHidden()
     */
    public boolean isHidden() {
        for (Component c : this) {
            return c.isHidden();
        }
        return false;
    }
    
    /**
     * Fades out components in this set.  Uses default duration of 500ms.
     * @return Self for chaining.
     */
    public ComponentSelector fadeOut() {
        return fadeOut(500);
    }
    
    /**
     * Fades out components in this set.
     * @param duration Duration of animation.
     * @return Self for chaining.
     */
    public ComponentSelector fadeOut(int duration) {
        return fadeOut(duration, null);
    }
    
    /**
     * Fades out components in this set.
     * @param duration Duration of animation.
     * @param callback Callback to run when animation completes.
     * @return Self for chaining.
     */
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

                public void run() {
                    for (final Component c : animatingComponents) {
                        //c.setHidden(true);
                        c.setVisible(false);
                        final Container placeholder = (Container)c.getClientProperty(placeholderProperty);
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
    
    /**
     * Hide the matched components with a sliding motion.
     * @param duration Duration of animation
     * @return Self for chaining.
     */
    public ComponentSelector slideUp(int duration) {
        return slideUp(duration, null);
    }
    
    /**
     * Hide the matched elements with a sliding motion.
     * @param duration Duration of animation.
     * @param callback Callback to run when animation completes
     * @return Self for chaining.
     */
    public ComponentSelector slideUp(int duration, final SuccessCallback<ComponentSelector> callback) {
        final ArrayList<Component> animatedComponents = new ArrayList<Component>();
        for (Component c : this) {
            c.setHeight(0);
            animatedComponents.add(c);
        }
        getParent().animateUnlayout(duration, 255, new SuccessCallback<ComponentSelector>() {

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
    
    /**
     * Hide the matched elements with a sliding motion.  Blocks until animation is complete.
     * @param duration Duration of animation.
     * @return Self for chaining.
     */
    public ComponentSelector slideUpAndWait(int duration) {
        for (Component c : this) {
            c.setHeight(0);
        }
        getParent().animateUnlayoutAndWait(duration, 255);
        return this;
    }
    
    /**
     * Display the matched elements with a sliding motion.  Uses default duration of 500ms
     * @return Self for chaining.
     */
    public ComponentSelector slideDown() {
        return slideDown(500);
    }
    
    /**
     * Hide the matched elements with a sliding motion.  Uses default duration of 500ms
     * @return Self for chaining.
     */
    public ComponentSelector slideUp() {
        return slideUp(500);
    }
    /**
     * Display the matched elements with a sliding motion.
     * @param duration Duration of animation.
     * @return Self for chaining.
     */
    public ComponentSelector slideDown(int duration) {
        return slideDown(duration, null);
    }
    
    /**
     * Display the matched elements with a sliding motion.
     * @param duration Duration of animation.
     * @param callback Callback to run when animation completes.
     * @return Self for chaining.
     */
    public ComponentSelector slideDown(final int duration, final SuccessCallback<ComponentSelector> callback) {
        for (Component c : this) {
            c.setHeight(0);
            c.setVisible(true);
        }
        getParent().animateLayout(duration, callback);
        
        return this;
    }
    
    /**
     * Display the matched elements with a sliding motion. Blocks until animation is complete.
     * @param duration Duration of animation.
     * @return Self for chaining.
     */
    public ComponentSelector slideDownAndWait(final int duration) {
        final ArrayList<Component> animatedComponents = new ArrayList<Component>();
        for (Component c : this) {
            c.setHeight(0);
            c.setVisible(true);
            animatedComponents.add(c);
        }
        getParent().animateLayoutAndWait(duration);
        
        return this;
    }
    
    /**
     * Hide the matched elements by fading them to transparent. Blocks thread until animation is complete.
     * @param duration Duration of animation.
     * @return Self for chaining.
     */
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
                final Container placeholder = (Container)c.getClientProperty(placeholderProperty);
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
    
    /**
     * Replaces the matched components within respective parents with replacements defined by the provided mapper.  Replacements
     * are replaced in the UI itself  (i.e. {@code c.getParent().replace(c, replacement)}) with an empty
     * transition.
     * @param mapper Mapper that defines the replacements for each component in the set.  If the mapper returns
     * the input component, then no change is made for that component.  A null return value cause the component
     * to be removed from its parent.  Returning a Component results in that component replacing the original component
     * within its parent.
     * @return A new ComponentSelector with the replacement components.
     */
    public ComponentSelector replace(ComponentMapper mapper) {
        return replace(mapper, CommonTransitions.createEmpty());
    }
    
    /**
     * Replaces the matched components within respective parents with replacements defined by the provided mapper.  Replacements
     * are replaced in the UI itself  (i.e. {@code c.getParent().replace(c, replacement)}) with the provided transition.
     * @param mapper Mapper that defines the replacements for each component in the set.  If the mapper returns
     * the input component, then no change is made for that component.  A null return value cause the component
     * to be removed from its parent.  Returning a Component results in that component replacing the original component
     * within its parent.
     * @param t Transition to use for replacements.
     * @return A new ComponentSelector with the replacement components.
     */
    public ComponentSelector replace(ComponentMapper mapper, Transition t) {
        ArrayList<ComponentAnimation> animations = new ArrayList<ComponentAnimation>();
        HashSet<Component> replacements = new HashSet<Component>();
        for (Component c : this) {
            Container parent = c.getParent();
            Component replacement = mapper.map(c);
            if (parent != null) {
                if (replacement != c) {
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
        if (mgr != null && animations.size() > 0) {
            mgr.addAnimation(ComponentAnimation.compoundAnimation(animations.toArray(new ComponentAnimation[animations.size()])));
        }
        return new ComponentSelector(replacements);
        
    }
    
    
    /**
     * Replaces the matched components within respective parents with replacements defined by the provided mapper.  Replacements
     * are replaced in the UI itself  (i.e. {@code c.getParent().replace(c, replacement)}) with the provided transition.
     * Blocks the thread until the transition animation is complete.
     * @param mapper Mapper that defines the replacements for each component in the set.  If the mapper returns
     * the input component, then no change is made for that component.  A null return value cause the component
     * to be removed from its parent.  Returning a Component results in that component replacing the original component
     * within its parent.
     * @param t
     * @return A new ComponentSelector with the replacement components.
     */
    public ComponentSelector replaceAndWait(ComponentMapper mapper, Transition t) {
        ArrayList<ComponentAnimation> animations = new ArrayList<ComponentAnimation>();
        HashSet<Component> replacements = new HashSet<Component>();
        for (Component c : this) {
            Container parent = c.getParent();
            Component replacement = mapper.map(c);
            if (parent != null) {
                if (replacement != c) {
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
        if (mgr != null && animations.size() > 0) {
            mgr.addAnimationAndBlock(ComponentAnimation.compoundAnimation(animations.toArray(new ComponentAnimation[animations.size()])));
        }
        
        return new ComponentSelector(replacements);
    }
    
    
    
    /**
     * Creates a component selector that wraps the provided components.  The provided 
     * components are treated as the "results" of this selector.  Not the roots.  However
     * you can use {@link #find(java.lang.String) } to perform a query using this selector
     * as the roots.
     * @param cmps Components to add to this selector results.
     */
    public ComponentSelector(Component... cmps) {
        this.roots = new HashSet<Component>();
        this.results = new HashSet<Component>();
        for (Component cmp : cmps) {
            this.results.add(cmp);
        }
    }
    
    /**
     * Creates a new ComponentSelector with the provided set of components.
     * @param cmps The components to include in the set.
     * @return ComponentSelector with provided components.
     */
    public static ComponentSelector $(Set<Component> cmps) {
        return new ComponentSelector(cmps);
    }
    
    /**
     * Alias of {@link #$(java.util.Set) }
     * @param cmps
     * @return 
     */
    public static ComponentSelector select(Set<Component> cmps) {
        return $(cmps);
    }
    
    /**
     * Creates a component selector that wraps the provided components.  The provided 
     * components are treated as the "results" of this selector.  Not the roots.  However
     * you can use {@link #find(java.lang.String) } to perform a query using this selector
     * as the roots.
     * @param cmps Components to add to this selector results.
     */
    public ComponentSelector(Set<Component> cmps) {
        this.roots = new HashSet<Component>();
        this.results = new HashSet<Component>();
        this.results.addAll(cmps);

    }
    
    /**
     * Creates a new ComponentSelector with the components matched by the provided selector.  The current form
     * is used as the root for searches.  Will throw a runtime exception if there is no current form.
     * @param selector A selector string that defines which components to include in the 
     * set.
     * @return ComponentSelector with matching components.
     */
    public static ComponentSelector $(String selector) {
        return new ComponentSelector(selector);
    }
    
    /**
     * Alias of {@link #$(java.lang.String) }
     * @param selector
     * @return 
     */
    public static ComponentSelector select(String selector) {
        return $(selector);
    }
    
    /**
     * Creates a selector that will query the current form.  If there is no 
     * current form, then this selector will have no roots.
     * <p>Generally it is better to provide a root explicitly using {@link ComponentSelector#ComponentSelector(java.lang.String, com.codename1.ui.Component...) 
     * to ensure that the selector has a tree to walk down.</p>
     * @param selector The selector string.
     */
    public ComponentSelector(String selector) {
        this.roots = new HashSet<Component>();
        Form f = Display.getInstance().getCurrent();
        if (f != null) {
            this.roots.add(f);
        } else {
            throw new RuntimeException("Attempt to create selector on current form, but there is no current form.  Best practice is to explicitly provide a root for your ComponentSelector.");
        }
        parse(selector);
    }
    
    /**
     * Creates a ComponentSelector with the components matched by the provided selector in the provided
     * roots' subtrees.
     * @param selector Selector string to define which components will be included in the set.
     * @param roots Roots for the selector to search.  Only components within the roots' subtrees will be included in the set.
     * @return ComponentSelector with matching components.
     */
    public static ComponentSelector $(String selector, Component... roots) {
        return new ComponentSelector(selector, roots);
    }
    
    /**
     * Alias of {@link #$(java.lang.String, com.codename1.ui.Component...) }
     * @param selector
     * @param roots
     * @return 
     */
    public static ComponentSelector select(String selector, Component... roots) {
        return $(selector, roots);
    }
    
    /**
     * Creates a selector with the provided roots.  This will only search through the subtrees
     * of the provided roots to find results that match the provided selector string.
     * @param selector The selector string
     * @param roots The roots for this selector.
     */
    public ComponentSelector(String selector, Component... roots) {
        this.roots = new HashSet<Component>();
        for (Component root : roots) {
            this.roots.add(root);
        }
        parse(selector);
    }
    
    /**
     * Creates a ComponentSelector with the components matched by the provided selector in the provided
     * roots' subtrees.
     * @param selector Selector string to define which components will be included in the set.
     * @param roots Roots for the selector to search.  Only components within the roots' subtrees will be included in the set.
     * @return ComponentSelector with matching components.
     */
    public static ComponentSelector $(String selector, Collection<Component> roots) {
        return new ComponentSelector(selector, roots);
    }
    
    /**
     * Alias of {@link #$(java.lang.String, java.util.Collection) }
     * @param selector
     * @param roots
     * @return 
     */
    public static ComponentSelector select(String selector, Collection<Component> roots) {
        return $(selector, roots);
    }
    
    /**
     * Creates a selector with the provided roots.  This will only search through the subtrees
     * of the provided roots to find results that match the provided selector string.
     * @param selector The selector string
     * @param roots The roots for this selector.
     */
    public ComponentSelector(String selector, Collection<Component> roots) {
        this.roots = new HashSet<Component>();
        this.roots.addAll(roots);
        parse(selector);
    }
    
    /**
     * Uses the results of this selector as the roots to create a new selector with 
     * the provided selector string.
     * @param selector The selector string.
     * @return A new ComponentSelector with the results of the query.
     */
    public ComponentSelector find(String selector) {
        return new ComponentSelector(selector, resultsImpl());
    }
    
    private void parse(String selector) {
        selector = selector.trim();
        
        if (selector.indexOf(",") != -1) {
            // this is an aggregate selector
            String[] parts = Util.split(selector, ",");
            
            aggregateSelectors = new HashSet<ComponentSelector>();
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
            for (int i=0; i<len; i++) {
                if (">".equals(parts[i])) {
                    if (i < len-1) {
                        parts[i] = ">" + parts[i+1].trim();
                        for (int j=i+1; j<len-1; j++) {
                            parts[j] = parts[j+1];
                        }
                        len--;
                        parts[len] = null;
                    } else {
                        throw new IllegalArgumentException("Failed to parse selector.  Selector cannot end with '>'");
                    }
                }
                if (i>0 && i < len-1) {
                    parentSelector.append(" ");
                }
                if (i < len-1) {
                    parentSelector.append(parts[i]);
                }
                if (i == len-1) {
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
            throw new IllegalArgumentException("Invalid character in selector "+selector);
        } else {
            ComponentSelector out = this;
            selector = selector.trim();


            int pos;

            if ((pos = selector.indexOf(":")) != -1) {
                out.state = selector.substring(pos+1);
                selector = selector.substring(0, pos);
            }
            if ((pos = selector.indexOf(".")) != -1) {
                out.tags = Util.split(selector.substring(pos+1), ".");
                len = out.tags.length;
                out.tagsNeedles = new String[len];
                String[] needles = out.tagsNeedles;
                String[] tags = out.tags;
                for (int i=0; i<len; i++) {
                    needles[i] = " " + tags[i] + " "; // Will make it easier to match against components' tags.
                }
                selector = selector.substring(0, pos);
            }
            if ((pos = selector.indexOf("#")) >= 0) {
                out.name = selector.substring(pos+1);
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
            String ctags = (String)c.getClientProperty(PROPERTY_TAG);

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

    /**
     * Returns the results of this selector.
     * @return 
     */
    public Iterator<Component> iterator() {
        return resultsImpl().iterator();
    }
    
    private Set<Component> resultsImpl() {
        if (results == null) {
            results = new HashSet<Component>();
            
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
                        Container cnt = (Container)root;
                        for (Component child : cnt) {
                            if (match(child)) {
                                results.add(child);
                            }
                        }
                    }
                } else {
                    if (root instanceof Container) {
                        Container cnt = (Container)root;
                        for (Component child : cnt) {
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
            Container cnt = (Container)root;
            for (Component child : cnt) {
                resultsImpl(out, child);
            }
        }
        return out;
    }
    
    /**
     * Gets a proxy style that wraps the result of {@link Component#getStyle() } of each component in set.
     * @return
     */
    public Style getStyle() {
        HashSet<Style> out = new HashSet<Style>();
        for (Component c : this) {
            out.add(c.getStyle());
        }
        return Style.createProxyStyle(out.toArray(new Style[out.size()]));
    }
    
    /**
     * Gets a style object for the given component that can be used to modify the
     * component's styles.  This takes into account any state-pseudo classes that
     * were used to create this selector so that the style returned will be appropriate.
     * 
     * <p>E.g. <pre>
     * {@code 
     * 
     * ComponentSelector sel = new ComponentSelector("Button:pressed");
     * Style style = sel.getStyle(sel.get(0));
     *     // This should be equivalent to sel.get(0).getPressedStyle()
     * 
     * sel = new ComponentSelector("Button");
     * style = sel.getStyle(sel.get(0));
     *     // This should be equivalent to sel.get(0).getAllStyles()
     * 
     * sel = new ComponentSelector("Button:pressed, Button:selected");
     * style = sel.getStyle(sel.get(0));
     *     // This should be same as 
     *     // Style.createProxyStyle(sel.get(0).getPressedStyle(), sel.get(0).getSelectedStyle())
     * }
     * </pre>
     * @param component The component whose style object we wish to obtain.
     * @return A style object that will allow us to modify the style of the given component.
     */
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
    
    /**
     * Returns a proxy style for all of the selected styles of the components in this set.
     * @return Proxy style to easily change properties of the selected styles of all
     * components in this set.
     */
    public Style getSelectedStyle() {
        ArrayList<Style> styles = new ArrayList<Style>();
        for (Component c : this) {
            styles.add(c.getSelectedStyle());
        }
        return Style.createProxyStyle(styles.toArray(new Style[styles.size()]));
    }
    
    /**
     * Sets the current style to the selected style.  Style mutation methods called after 
     * calling this method will modify the components' "selected style".
     * @return Self for chaining.
     * @see Component#getSelectedStyle() 
     */
    public ComponentSelector selectSelectedStyle() {
        currentStyle = getSelectedStyle();
        currentStyleType = SELECTED_STYLE;
        return this;
    }
    
    /**
     * Sets the current style to the unselected style.  Style mutation methods called after 
     * calling this method will modify the components' "unselected style".
     * @return Self for chaining.
     * @see Component#getUnselectedStyle() 
     */
    public ComponentSelector selectUnselectedStyle() {
        currentStyle = getUnselectedStyle();
        currentStyleType = UNSELECTED_STYLE;
        return this;
    }
    
    /**
     * Sets the current style to the pressed style.  Style mutation methods called after 
     * calling this method will modify the components' "pressed style".
     * @return Self for chaining.
     * @see Component#getPressedStyle() 
     */
    public ComponentSelector selectPressedStyle() {
        currentStyle = getPressedStyle();
        currentStyleType = PRESSED_STYLE;
        return this;
    }
    
  
    /**
     * Sets the current style to the disabled style.  Style mutation methods called after 
     * calling this method will modify the components' "disabled style".
     * @return Self for chaining.
     * @see Component#getDisabledStyle() 
     */
    public ComponentSelector selectDisabledStyle() {
        currentStyle = getDisabledStyle();
        currentStyleType = DISABLED_STYLE;
        return this;
    }
    
    /**
     * Sets the current style to each component's ALL STYLES proxy style.  Style mutation methods called after 
     * calling this method will modify the components' "all styles" proxy style.
     * @return Self for chaining.
     * @see Component#getAllStyles() 
     */
    public ComponentSelector selectAllStyles() {
        currentStyle = getAllStyles();
        currentStyleType = ALL_STYLES;
        return this;
    }
    
    /**
     * Returns a proxy style for all of the unselected styles of the components in this set.
     * @return Proxy style to easily change properties of the unselected styles of all
     * components in this set.
     */
    public Style getUnselectedStyle() {
        ArrayList<Style> styles = new ArrayList<Style>();
        for (Component c : this) {
            styles.add(c.getUnselectedStyle());
        }
        return Style.createProxyStyle(styles.toArray(new Style[styles.size()]));
    }
    
    /**
     * Returns a proxy style for all of the pressed styles of the components in this set.
     * @return Proxy style to easily change properties of the pressed styles of all
     * components in this set.
     */
    public Style getPressedStyle() {
        ArrayList<Style> styles = new ArrayList<Style>();
        for (Component c : this) {
            styles.add(c.getPressedStyle());
        }
        return Style.createProxyStyle(styles.toArray(new Style[styles.size()]));
    }
    
    /**
     * Returns a proxy style for all of the disabled styles of the components in this set.
     * @return Proxy style to easily change properties of the disabled styles of all
     * components in this set.
     */
    public Style getDisabledStyle() {
        ArrayList<Style> styles = new ArrayList<Style>();
        for (Component c : this) {
            styles.add(c.getDisabledStyle());
        }
        return Style.createProxyStyle(styles.toArray(new Style[styles.size()]));
    }
    
    /**
     * Returns a proxy style for all of the "all" styles of the components in this set.
     * @return Proxy style to easily change properties of the "all" styles of all
     * components in this set.
     */
    public Style getAllStyles() {
        ArrayList<Style> styles = new ArrayList<Style>();
        for (Component c : this) {
            styles.add(c.getAllStyles());
        }
        return Style.createProxyStyle(styles.toArray(new Style[styles.size()]));
    }
    
    /**
     * Returns number of results found.
     * @return 
     */
    public int size() {
        return resultsImpl().size();
    }

    /**
     * 
     * @return True if there were no results.
     */
    public boolean isEmpty() {
        return resultsImpl().isEmpty();
    }

    /**
     * Checks if an object is contained in result set.
     * @param o
     * @return 
     */
    public boolean contains(Object o) {
        return resultsImpl().contains(o);
    }

    /**
     * Returns results as an array.
     * @return 
     */
    public Object[] toArray() {
        return resultsImpl().toArray();
    }

    /**
     * Returns results as an array.
     * @param <T>
     * @param a
     * @return 
     */
    public <T> T[] toArray(T[] a) {
        return resultsImpl().toArray(a);
    }

    /**
     * Explicitly adds a component to the result set.
     * @param e
     * @return True on success
     */
    public boolean add(Component e) {
        setDirty();
        return resultsImpl().add(e);
    }

    /**
     * Appends a child component to the first container in this set.  Same as calling
     * {@link Container#add(com.codename1.ui.Component) } padding {@literal child} on first container
     * in this set.
     * @param child Component to add to container.
     * @return Self for chaining.
     */
    public ComponentSelector append(Component child) {
        for (Component c : this) {
            if (c instanceof Container) {
                Container cnt = (Container)c;
                cnt.add(child);
                return this;
            }
        }
        return this;
    }
    
    /**
     * Appends a child component to the first container in this set.  Same as calling
     * {@link Container#add(java.lang.Object, com.codename1.ui.Component) } padding {@literal child} on first container
     * in this set.
     * @param constraint
     * @param child
     * @return 
     */
    public ComponentSelector append(Object constraint, Component child) {
        for (Component c : this) {
            if (c instanceof Container) {
                Container cnt = (Container)c;
                cnt.add(constraint, child);
                return this;
            }
        }
        return this;
    }
    
    /**
     * Append a child element to each container in this set.  The {@literal mapper} callback
     * will receive a Container as input (that is the parent to be added to), and should return 
     * a Component that is to be added to it.  If the mapper returns null, then nothing is 
     * added to that container.
     * @param mapper
     * @return 
     */
    public ComponentSelector append(ComponentMapper mapper) {
        for (Component c : this) {
            if (c instanceof Container) {
                Component child = mapper.map(c);
                if (child != null) {
                    ((Container)c).add(child);
                }
            }
        }
        return this;
    }
    
    /**
     * Append a child element to each container in this set.  The {@literal mapper} callback
     * will receive a Container as input (that is the parent to be added to), and should return 
     * a Component that is to be added to it.  If the mapper returns null, then nothing is 
     * added to that container.
     * @param constraint
     * @param mapper
     * @return 
     */
    public ComponentSelector append(Object constraint, ComponentMapper mapper) {
        for (Component c : this) {
            if (c instanceof Container) {
                Component child = mapper.map(c);
                ((Container)c).add(constraint, child);
            }
        }
        return this;
    }
    
    /**
     * Fluent API wrapper for {@link #add(com.codename1.ui.Component) }
     * @param e Component to add to set.
     * @param chain Dummy argument so that this version would have a different signature than {@link Set#add(java.lang.Object) }
     * @return Self for chaining.
     */
    public ComponentSelector add(Component e, boolean chain) {
        add(e);
        return this;
    }
    
    /**
     * Explicitly removes a component from the result set.
     * @param o
     * @return Self for chaining.
     */
    public boolean remove(Object o) {
        setDirty();
        return resultsImpl().remove(o);
    }

    /**
     * Fluent API wrapper for {@link #remove(java.lang.Object) }.
     * @param o The component to remove from set.
     * @param chain Dummy argument so that this version would have a different signature than {@link Set#remove(java.lang.Object) }
     * @return Self for chaining.
     */
    public ComponentSelector remove(Object o, boolean chain) {
        remove(o);
        return this;
    }
    
    
    /**
     * Checks if the result set contains all of the components found in the provided
     * collection.
     * @param c
     * @return 
     */
    public boolean containsAll(Collection<?> c) {
        return resultsImpl().containsAll(c);
    }

    /**
     * Adds all components in the given collection to the result set.
     * @param c
     * @return 
     */
    public boolean addAll(Collection<? extends Component> c) {
        setDirty();
        return resultsImpl().addAll(c);
    }
    
    /**
     * Fluent API wrapper for {@link #addAll(java.util.Collection) }.
     * @param c The set of components to add to this set.
     * @param chain Dummy argument so that this version would have a different signature than {@link #addAll(java.util.Collection) }
     * @return Self for chaining.
     */
    public ComponentSelector addAll(Collection<? extends Component> c, boolean chain) {
        addAll(c);
        return this;
    }
    
    /**
     * Returns the first component in this set.  This is useful for single component sets (e.g. {@code $(new Label()).setFgColor(0xff0000).asComponent() }).
     * 
     * @return The first component in this set.
     */
    public Component asComponent() {
        for (Component c : this) {
            return c;
        }
        return null;
    }
    
    /**
     * Returns the first component in this set.  This is useful for single component sets (e.g. {@code $(new Label()).setFgColor(0xff0000).asComponent(Label.class) }).
     * 
     * @param <T> The type of component to return
     * @param type The type of component that is expected to be returned.
     * @return The first component in this set.
     */
    public <T extends Component> T asComponent(Class<T> type) {
        return (T)asComponent();
    }
    
    /**
     * Returns the components of this set as a List.  Note that order of elements is not maintained since
     * ComponentSelector is a set (i.e. has no notion of element order).
     * @return The components in this set as a list.
     */
    public java.util.List<Component> asList() {
        ArrayList<Component> out = new ArrayList<Component>();
        out.addAll(this);
        return out;
    }

    /**
     * Retains only elements of the result set that are contained in the provided collection.
     * @param c
     * @return 
     */
    public boolean retainAll(Collection<?> c) {
        setDirty();
        return resultsImpl().retainAll(c);
    }
    
    /**
     * Fluent API wrapper for {@link #retainAll(java.util.Collection) }
     * @param c The collection to retain.
     * @param chain Dummy arg.
     * @return Self for chaining.
     */
    public ComponentSelector retainAll(Collection<?> c, boolean chain) {
        retainAll(c);
        return this;
    }

    /**
     * Removes all of the components in the provided collection from the result set.
     * @param c
     * @return 
     */
    public boolean removeAll(Collection<?> c) {
        setDirty();
        return resultsImpl().removeAll(c);
    }
    
    /**
     * Fluent API wrapper for {@link #removeAll(java.util.Collection) }
     * @param c Collection with components to remove,
     * @param chain Dummy arg.
     * @return Self for chaining.
     */
    public ComponentSelector removeAll(Collection<?> c, boolean chain) {
         removeAll(c);
         return this;
    }

    /**
     * Clears the result set.
     */
    public void clear() {
        setDirty();
        resultsImpl().clear();
    }
    
    /**
     * Fluent API wrapper for (@link #clear()}
     * @param chain Dummy arg
     * @return Self for chaining.
     */
    public ComponentSelector clear(boolean chain) {
        clear();
        return this;
    }
    
    @Override
    public String toString() {
        return "ComponentSelector{ name="+name+", uiid="+uiid+", tags="+Arrays.toString(tags)+", roots="+roots+", parent="+parent+", results = "+results+"}";
    }
    
    /**
     * Adds the given tags to all elements in the result set.
     * @param tags Tags to add.
     * @return Self for chaining.
     */
    public ComponentSelector addTags(String... tags) {
        for (Component c : this) {
            
            String existing = (String)c.getClientProperty(PROPERTY_TAG);
            if (existing == null) {
                existing = "";
            }
            
            for (String tag : tags) {
                if (existing.indexOf(" "+tag+" ") == -1) {
                    existing += " "+tag+" ";
                }
            }
            c.putClientProperty(PROPERTY_TAG, existing);
           
        }
        return this;
    }
    
    /**
     * Removes the given tags from all elements in the result set.
     * @param tags
     * @return 
     */
    public ComponentSelector removeTags(String... tags) {
        for (Component c : this) {
            String existing = (String)c.getClientProperty(PROPERTY_TAG);
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
            for (String tag : tags) {
                existingSet.add(tag);
            }
            existing = "";
            if (existingSet.isEmpty()) {
                c.putClientProperty(PROPERTY_TAG, null);
                continue;
            }
            for (String tag : existingSet) {
                existing += " " + tag + " ";
            }
            c.putClientProperty(PROPERTY_TAG, existing);
        }
        return this;
    }
    
    
    /**
     * Gets the set of all "parent" components of components in the result set.
     * @return New Component selector with respective parents of the components in the
     * current result set.
     */
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
    
    /**
     * Wrapper for {@link Component#setSameWidth(com.codename1.ui.Component...) }.  Passes all 
     * components in the result set as parameters of this method, effectively making them all the
     * same width.
     * @return 
     */
    public ComponentSelector setSameWidth() {
        Component.setSameWidth(toArray(new Component[size()]));
        return this;
    }
    
    /**
     * Wrapper for {@link Component#setSameHeight(com.codename1.ui.Component...) }.  Passes all 
     * components in the result set as parameters of this method, effectively making them all the
     * same height.
     * @return 
     */
    public ComponentSelector setSameHeight() {
        Component.setSameHeight(toArray(new Component[size()]));
        return this;
    }
    
    /**
     * Wrapper for {@link Component#clearClientProperties() }.
     * @return Self for Chaining.
     */
    public ComponentSelector clearClientProperties() {
        for (Component c : this) {
            c.clearClientProperties();
        }
        return this;
    }
    
    /**
     * Wrapper for {@link Component#putClientProperty(java.lang.String, java.lang.Object) }
     * @param key Property key
     * @param value Property value
     * @return Self for chaining.
     */
    public ComponentSelector putClientProperty(String key, Object value) {
        for (Component c : this) {
            c.putClientProperty(key, value);
            
        }
        return this;
    }
    
    /**
     * Gets a client property from the first component in the set.  Wraps {@link Component#getClientProperty(java.lang.String) }
     * @param key The key of the client property to retrieve.
     * @return The value of the client property.
     */
    public Object getClientProperty(String key) {
        for (Component c : this) {
            return c.getClientProperty(key);
        }
        return null;
    }
    
    /**
     * Wrapper for {@link Component#setDirtyRegion(com.codename1.ui.geom.Rectangle) }
     * @param rect Dirty region
     * @return Self for chaining.
     */
    public ComponentSelector setDirtyRegion(Rectangle rect) {
        for (Component c : this) {
            c.setDirtyRegion(rect);
        }
        return this;
    }
    
    /**
     * Wrapper for {@link Component#setVisible(boolean) }
     * @param visible True to make all components in result set visible.  False for hidden.
     * @return Self for chaining.
     */
    public ComponentSelector setVisible(boolean visible) {
        for (Component c : this) {
            c.setVisible(visible);
        }
        return this;
    }
    
    /**
     * Wrapper for {@link Component#setX(int) }
     * @param x
     * @return 
     */
    public ComponentSelector setX(int x) {
        for (Component c : this) {
            c.setX(x);
        }
        return this;
    }
    
    /**
     * Wrapper for {@link Component#setY(int) }
     * @param y
     * @return 
     */
    public ComponentSelector setY(int y) {
        for (Component c : this) {
            c.setY(y);
        }
        return this;
    }
    
    /**
     * Wrapper for {@link Component#setWidth(int) }
     * @param width
     * @return 
     */
    public ComponentSelector setWidth(int width) {
        for (Component c : this) {
            c.setWidth(width);
        }
        return this;
    }
    
    /**
     * Wrapper for {@link Component#setHeight(int) }
     * @param height
     * @return 
     */
    public ComponentSelector setHeight(int height) {
        for (Component c : this) {
            c.setHeight(height);
        }
        return this;
    }
    
    /**
     * Wrapper for {@link Component#setPreferredSize(com.codename1.ui.geom.Dimension) }
     * @param dim
     * @return 
     */
    public ComponentSelector setPreferredSize(Dimension dim) {
        for (Component c : this) {
            c.setPreferredSize(dim);
        }
        return this;
    }
    
    /**
     * Wrapper for {@link Component#setPreferredH(int) }
     * @param h
     * @return 
     */
    public ComponentSelector setPreferredH(int h) {
        for (Component c : this) {
            c.setPreferredH(h);
        }
        return this;
    }
    
    /**
     * Wrapper for {@link Component#setPreferredW(int) }
     * @param w
     * @return 
     */
    public ComponentSelector setPreferredW(int w) {
        for (Component c : this) {
            c.setPreferredW(w);
        }
        return this;
    }
    
    
    /**
     * Wrapper for {@link Component#setScrollSize}
     * @param size
     * @return 
     */
    public ComponentSelector setScrollSize(Dimension size) {
        for (Component c : this) {
            c.setScrollSize(size);
        }
        return this;
    }
    
    /**
     * Wrapper for {@link Component#setSize(com.codename1.ui.geom.Dimension) }
     * @param size
     * @return 
     */
    public ComponentSelector setSize(Dimension size) {
        for (Component c : this) {
            c.setSize(size);
        }
        return this;
    }
    
    /**
     * Wrapper for {@link Component#setUIID(java.lang.String) }
     * @param uiid
     * @return 
     */
    public ComponentSelector setUIID(String uiid) {
        for (Component c : this) {
            c.setUIID(uiid);
        }
        return this;
    }
    
    /**
     * Wrapper for {@link Component#remove() }.  This will remove all of the components
     * in the current found set from their respective parents.
     * @return 
     */
    public ComponentSelector remove() {
        for (Component c : this) {
            c.remove();
        }
        return this;
    }
    
    /**
     * Adds a focus listener to all components in found set.  Wraps {@link Component#addFocusListener(com.codename1.ui.events.FocusListener) }
     * @param l
     * @return 
     */
    public ComponentSelector addFocusListener(FocusListener l) {
        for (Component c : this) {
            c.addFocusListener(l);
        }
        return this;
    }
    
    /**
     * Removes focus listener from all components in found set.  Wraps {@link Component#removeFocusListener(com.codename1.ui.events.FocusListener) }
     * @param l
     * @return 
     */
    public ComponentSelector removeFocusListener(FocusListener l) {
        for (Component c : this) {
            c.removeFocusListener(l);
        }
        return this;
    }
    
    /**
     * Adds scroll listener to all components in found set.  Wraps {@link Component#addScrollListener(com.codename1.ui.events.ScrollListener) }
     * @param l
     * @return 
     */
    public ComponentSelector addScrollListener(ScrollListener l) {
        for (Component c : this) {
            c.addScrollListener(l);
        }
        return this;
    }
    
    /**
     * Removes scroll listener from all components in found set. Wraps {@link Component#removeScrollListener(com.codename1.ui.events.ScrollListener) }
     * @param l
     * @return 
     */
    public ComponentSelector removeScrollListener(ScrollListener l) {
        for (Component c : this) {
            c.removeScrollListener(l);
        }
        return this;
    }
    
    /**
     * Sets select command text on all components in found set.  Wraps {@link Component#setSelectCommandText(java.lang.String) }
     * @param text
     * @return 
     */
    public ComponentSelector setSelectCommandText(String text) {
        for (Component c : this) {
            c.setSelectCommandText(text);
        }
        return this;
    }
    
    /**
     * Wraps {@link Component#setLabelForComponent(com.codename1.ui.Label) }
     * @param l
     * @return 
     */
    public ComponentSelector setLabelForComponent(Label l) {
        for (Component c : this) {
            c.setLabelForComponent(l);
        }
        return this;
    }
    
    /**
     * Wraps {@link Component#paintBackgrounds(com.codename1.ui.Graphics) }
     * @param g
     * @return 
     */
    public ComponentSelector paintBackgrounds(Graphics g) {
        for (Component c : this) {
            c.paintBackgrounds(g);
        }
        return this;
    }
    
    /**
     * Wraps {@link Component#paintComponent(com.codename1.ui.Graphics) }
     * @param g
     * @return 
     */
    public ComponentSelector paintComponent(Graphics g) {
        for (Component c : this) {
            c.paintComponent(g);
        }
        return this;
    }
    
    /**
     * Wraps {@link Component#paint(com.codename1.ui.Graphics) }
     * @param g
     * @return 
     */
    public ComponentSelector paint(Graphics g) {
        for (Component c : this) {
            c.paint(g);
        }
        return this;
    }
    
    /**
     * Returns true if any of the components in the found set contains the provided coordinate.
     * Wraps {@link Component#contains(int, int) }
     * @param x
     * @param y
     * @return 
     */
    public boolean contains(int x, int y) {
        for (Component c : this) {
            if (c.contains(x, y)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Sets all components in the found set focusability.
     * Wraps {@link Component#setFocusable(boolean) }
     * @param focus
     * @return 
     */
    public ComponentSelector setFocusable(boolean focus) {
        for (Component c : this) {
            c.setFocusable(focus);
        }
        return this;
    }
    
    /**
     * Wraps {@link Component#repaint() }
     * @return 
     */
    public ComponentSelector repaint() {
        for (Component c : this) {
            c.repaint();
        }
        return this;
    }
    
    /**
     * Wraps {@link Component#repaint(int, int, int, int) }
     * @param x
     * @param y
     * @param w
     * @param h
     * @return 
     */
    public ComponentSelector repaint(int x, int y, int w, int h) {
        for (Component c : this) {
            c.repaint(x, y, w, h);
        }
        return this;
    }
    
    /**
     * Wraps {@link Component#setScrollAnimationSpeed(int) }
     * @param speed
     * @return 
     */
    public ComponentSelector setScrollAnimationSpeed(int speed) {
        for (Component c : this) {
            c.setScrollAnimationSpeed(speed);
        }
        return this;
    }
    
    /**
     * Wraps {@link Component#setSmoothScrolling(boolean) }
     * @param smooth
     * @return 
     */
    public ComponentSelector setSmoothScrolling(boolean smooth) {
        for (Component c : this) {
            c.setSmoothScrolling(smooth);
        }
        return this;
    }
    
    /**
     * Adds a drop listener to all components in found set.  Wraps {@link Component#addDropListener(com.codename1.ui.events.ActionListener) } 
     * @param l
     * @return 
     */
    public ComponentSelector addDropListener(ActionListener l) {
        for (Component c : this) {
            c.addDropListener(l);
        }
        return this;
    }
    
    /**
     * Removes a drop listener from all components in found set.
     * Wraps {@link Component#removeDropListener(com.codename1.ui.events.ActionListener) }
     * @param l
     * @return 
     */
    public ComponentSelector removeDropListener(ActionListener l) {
        for (Component c : this) {
            c.removeDropListener(l);
        }
        return this;
    }
    
    /**
     * Adds drag over listener to all components in found set.
     * Wraps {@link Component#addDragOverListener(com.codename1.ui.events.ActionListener) }
     * @param l
     * @return 
     */
    public ComponentSelector addDragOverListener(ActionListener l) {
        for (Component c : this) {
            c.addDragOverListener(l);
        }
        return this;
    }
    
    /**
     * Removes drag over listener from all components in found set.
     * Wraps {@link Component#removeDragOverListener(com.codename1.ui.events.ActionListener) }
     * @param l
     * @return 
     */
    public ComponentSelector removeDragOverListener(ActionListener l) {
        for (Component c : this) {
            c.removeDragOverListener(l);
        }
        return this;
    }
    
    /**
     * Adds pointer pressed listener to all components in found set.
     * Wraps {@link Component#addPointerPressedListener(com.codename1.ui.events.ActionListener) }
     * @param l
     * @return 
     */
    public ComponentSelector addPointerPressedListener(ActionListener l) {
        for (Component c : this) {
            c.addPointerPressedListener(l);
        }
        return this;
    }
    
    /**
     * Removes pointer pressed listener from all components in found set.
     * Wraps {@link Component#removePointerPressedListener(com.codename1.ui.events.ActionListener) }
     * @param l
     * @return 
     */
    public ComponentSelector removePointerPressedListener(ActionListener l) {
        for (Component c : this) {
            c.removePointerPressedListener(l);
        }
        return this;
    }
    
    /**
     * Adds pointer released listener to all components in found set. 
     * Wraps {@link Component#addPointerReleasedListener(com.codename1.ui.events.ActionListener) }
     * @param l
     * @return 
     */
    public ComponentSelector addPointerReleasedListener(ActionListener l) {
        for (Component c : this) {
            c.addPointerReleasedListener(l);
        }
        return this;
    }
    
    /**
     * Removes pointer released listener from all components in found set.
     * Wraps {@link Component#removePointerReleasedListener(com.codename1.ui.events.ActionListener) }
     * @param l
     * @return 
     */
    public ComponentSelector removePointerReleasedListener(ActionListener l) {
        for (Component c : this) {
            c.removePointerReleasedListener(l);
        }
        return this;
    }
    
    /**
     * Adds pointer dragged listener to all components in found set.
     * Wraps {@link Component#addPointerDraggedListener(com.codename1.ui.events.ActionListener) }
     * @param l
     * @return 
     */
    public ComponentSelector addPointerDraggedListener(ActionListener l) {
        for (Component c : this) {
            c.addPointerDraggedListener(l);
        }
        return this;
    }
    
    /**
     * REmoves pointer dragged listener from all components in found set.
     * Wraps {@link Component#removePointerDraggedListener(com.codename1.ui.events.ActionListener) }
     * @param l
     * @return 
     */
    public ComponentSelector removePointerDraggedListener(ActionListener l) {
        for (Component c : this) {
            c.removePointerDraggedListener(l);
        }
        return this;
    }
    
    /**
     * Sets pressed style of all components in found set.
     * Wraps {@link Component#setPressedStyle(com.codename1.ui.plaf.Style) }
     * @param style
     * @return 
     */
    public ComponentSelector setPressedStyle(Style style) {
        for (Component c : this) {
            c.setPressedStyle(style);
        }
        return this;
    }
    
    /**
     * Sets selected style of all components in found set.
     * Wraps {@link Component#setSelectedStyle(com.codename1.ui.plaf.Style) }
     * @param style
     * @return 
     */
    public ComponentSelector setSelectedStyle(Style style) {
        for (Component c : this) {
            c.setSelectedStyle(style);
        }
        return this;
    }
    
    /**
     * Sets disabled style of all components in found set.
     * Wraps {@link Component#setDisabledStyle(com.codename1.ui.plaf.Style) }
     * @param style
     * @return 
     */
    public ComponentSelector setDisabledStyle(Style style) {
        for (Component c : this) {
            c.setDisabledStyle(style);
        }
        return this;
    }
    
    /**
     * Sets unselected style of all components in found set.
     * Wraps {@link Component#setUnselectedStyle(com.codename1.ui.plaf.Style) }
     * @param style
     * @return 
     */
    public ComponentSelector setUnselectedStyle(Style style) {
        for (Component c : this) {
            c.setUnselectedStyle(style);
        }
        return this;
    }
    
    /**
     * Wraps {@link Component#requestFocus()}
     * @return 
     */
    public ComponentSelector requestFocus() {
        for (Component c : this) {
            c.requestFocus();
            return this;
        }
        return this;
    }
    
    /**
     * Wraps {@link Component#refreshTheme()}
     * @return 
     */
    public ComponentSelector refreshTheme() {
        for (Component c : this) {
            c.refreshTheme();
        }
        return this;
    }
    
    /**
     * Wraps {@link Component#refreshTheme(boolean) }
     * @param merge 
     * @return 
     */
    public ComponentSelector refreshTheme(boolean merge) {
        for (Component c : this) {
            c.refreshTheme(merge);
        }
        return this;
    }
    
    /**
     * Wraps {@link Component#setCellRenderer(boolean) }
     * @param cell
     * @return 
     */
    public ComponentSelector setCellRenderer(boolean cell) {
        for (Component c : this) {
            c.setCellRenderer(cell);
        }
        return this;
    }
    
    /**
     * Wraps {@link Component#setScrollVisible(boolean) }
     * @param vis
     * @return 
     */
    public ComponentSelector setScrollVisible(boolean vis) {
        for (Component c : this) {
            c.setScrollVisible(vis);
        }
        return this;
    }
    
    /**
     * Wraps {@link Component#setEnabled(boolean) }
     * @param enabled
     * @return 
     */
    public ComponentSelector setEnabled(boolean enabled) {
        for (Component c : this) {
            c.setEnabled(enabled);
        }
        return this;
    }
    
    /**
     * Wraps {@link Component#setName(java.lang.String) }
     * @param name
     * @return 
     */
    public ComponentSelector setName(String name) {
        for (Component c : this) {
            c.setName(name);
        }
        return this;
    }
    
    /**
     * Wraps {@link Component#setRTL(boolean) }
     * @param rtl
     * @return 
     */
    public ComponentSelector setRTL(boolean rtl) {
        for (Component c : this) {
            c.setRTL(rtl);
        }
        return this;
    }
    
    /**
     * Wraps {@link Component#setTactileTouch(boolean) }
     * @param t
     * @return 
     */
    public ComponentSelector setTactileTouch(boolean t) {
        for (Component c : this) {
            c.setTactileTouch(t);
        }
        return this;
    }
    
    /**
     * Wraps {@link Component#setPropertyValue(java.lang.String, java.lang.Object) }
     * @param key
     * @param value
     * @return 
     */
    public ComponentSelector setPropertyValue(String key, Object value) {
        for (Component c : this) {
            c.setPropertyValue(key, value);
        }
        return this;
    }
    
    /**
     * Wraps {@link Component#paintLockRelease() }
     * @return 
     */
    public ComponentSelector paintLockRelease() {
        for (Component c : this) {
            c.paintLockRelease();
        }
        return this;
    }
    
    /**
     * Wraps {@link Component#setSnapToGrid(boolean) }
     * @param s
     * @return 
     */
    public ComponentSelector setSnapToGrid(boolean s) {
        for (Component c : this) {
            c.setSnapToGrid(s);
        }
        return this;
    }
    
    
    public ComponentSelector setIgnorePointerEvents(boolean ignore) {
        for (Component c : this) {
            c.setIgnorePointerEvents(ignore);
        }
        return this;
    }
    
    public boolean isIgnorePointerEvents() {
        for (Component c : this) {
            return c.isIgnorePointerEvents();
        }
        return false;
    }
    
    /**
     * Wraps {@link Component#setFlatten(boolean)}
     * @param f
     * @return 
     */
    public ComponentSelector setFlatten(boolean f) {
        for (Component c : this) {
            c.setFlatten(f);
        }
        return this;
    }
    
    /**
     * Wraps {@link Component#setTensileLength(int)}
     * @param len
     * @return 
     */
    public ComponentSelector setTensileLength(int len) {
        for (Component c : this) {
            c.setTensileLength(len);
        }
        return this;
    }
    
    /**
     * Wraps {@link Component#setGrabsPointerEvents(boolean) }
     * @param g
     * @return 
     */
    public ComponentSelector setGrabsPointerEvents(boolean g) {
        for (Component c : this) {
            c.setGrabsPointerEvents(g);
        }
        return this;
    }
    
    /**
     * Wraps {@link Component#setScrollOpacityChangeSpeed(int) }
     * @param scrollOpacityChangeSpeed
     * @return 
     */
    public ComponentSelector setScrollOpacityChangeSpeed(int scrollOpacityChangeSpeed) {
        for (Component c : this) {
            c.setScrollOpacityChangeSpeed(scrollOpacityChangeSpeed);
        }
        return this;
    }
    
    /**
     * Wraps {@link Component#growShrink(int) }
     * @param duration
     * @return 
     */
    public ComponentSelector growShrink(int duration) {
        for (Component c : this) {
            c.growShrink(duration);
        }
        return this;
    }
    
    /**
     * Wraps {@link Component#setDraggable(boolean) }
     * @param draggable
     * @return 
     */
    public ComponentSelector setDraggable(boolean draggable) {
        for (Component c : this) {
            c.setDraggable(draggable);
        }
        return this;
    }
    
    /**
     * Wraps {@link Component#setDropTarget(boolean) }
     * @param target
     * @return 
     */
    public ComponentSelector setDropTarget(boolean target) {
        for (Component c : this) {
            c.setDropTarget(target);
        }
        return this;
    }
    
    /**
     * Wraps {@link Component#setHideInPortrait(boolean) }
     * @param hide
     * @return 
     */
    public ComponentSelector setHideInPortait(boolean hide) {
        for (Component c : this) {
            c.setHideInPortrait(hide);
        }
        return this;
    }
    
    /**
     * Wraps {@link Component#setHidden(boolean,boolean) }
     * @param b
     * @param changeMargin
     * @return 
     */
    public ComponentSelector setHidden(boolean b, boolean changeMargin) {
        for (Component c : this) {
            c.setHidden(b, changeMargin);
        }
        return this;
    }
    
    /**
     * Wraps {@link Component#setHidden(boolean) }
     * @param b
     * @return 
     */
    public ComponentSelector setHidden(boolean b) {
        for (Component c : this) {
            c.setHidden(b);
        }
        return this;
    }
    
    /**
     * Wraps {@link Component#setComponentState(java.lang.Object) }
     * @param state
     * @return 
     */
    public ComponentSelector setComponentState(Object state) {
        for (Component c : this) {
            c.setComponentState(state);
        }
        return this;
    }
    
    /**
     * Wraps {@link Container#setLeadComponent(com.codename1.ui.Component) }
     * @param lead
     * @return 
     */
    public ComponentSelector setLeadComponent(Component lead) {
        for (Component c : this) {
            if (c instanceof Container) {
                Container cnt = (Container)c;
                cnt.setLeadComponent(lead);
            }
        }
        return this;
    }
    
    /**
     * Wraps {@link Container#setLayout(com.codename1.ui.layouts.Layout)  }
     * @param layout
     * @return 
     */
    public ComponentSelector setLayout(Layout layout) {
        for (Component c : this) {
            if (c instanceof Container) {
                Container cnt = (Container)c;
                cnt.setLayout(layout);
            }
        }
        return this;
    }
    
    /**
     * Wraps {@link Container#invalidate() }
     * @return 
     */
    public ComponentSelector invalidate() {
        for (Component c : this) {
            if (c instanceof Container) {
                Container cnt = (Container)c;
                cnt.invalidate();
            }
            
        }
        return this;
    }
    
    /**
     * Wraps {@link Container#setShouldCalcPreferredSize(boolean) }
     * @param shouldCalcPreferredSize
     * @return 
     */
    public ComponentSelector setShouldCalcPreferredSize(boolean shouldCalcPreferredSize) {
        for (Component c : this) {
            if (c instanceof Container) {
                Container cnt = (Container)c;
                cnt.setShouldCalcPreferredSize(shouldCalcPreferredSize);
            }
            
        }
        return this;
    }
    
    /**
     * Wraps {@link Container#applyRTL(boolean)  }
     * @param rtl
     * @return 
     */
    public ComponentSelector applyRTL(boolean rtl) {
        for (Component c : this) {
            if (c instanceof Container) {
                Container cnt = (Container)c;
                cnt.applyRTL(rtl);
            }
        }
        return this;
    }
    
    /**
     * This removes all children from all containers in found set.  Not to be confused with {@link #clear()}, which
     * removes components from the found set, but not from their respective parents.
     * Wraps {@link Container#removeAll() }
     * @return 
     */
    public ComponentSelector removeAll() {
        for (Component c : this) {
            if (c instanceof Container) {
                ((Container)c).removeAll();
            }
        }
        return this;
    }
    
    /**
     * Wraps {@link Container#revalidate()  }
     * @return 
     */
    public ComponentSelector revalidate() {
        for (Component c : this) {
            if (c instanceof Container) {
                ((Container)c).revalidate();
            }
        }
        return this;
    }
    
    /**
     * Wraps {@link Container#forceRevalidate()  }
     * @return 
     */
    public ComponentSelector forceRevalidate() {
       
        for (Component c : this) {
            if (c instanceof Container) {
                ((Container)c).forceRevalidate();
            }
        }
        return this;
    }
    
    /**
     * Wraps {@link Container#layoutContainer()  }
     * @return 
     */
    public ComponentSelector layoutContainer() {
        
        for (Component c : this) {
            if (c instanceof Container) {
                ((Container)c).layoutContainer();
            }
        }
        return this;
    }
    
    /**
     * This returns a new ComponentSelector which includes a set of all results
     * of calling {@link Container#getComponentAt(int) } on containers in this
     * found set.  This effectively allows us to get all of the ith elements of all 
     * matched components.
     * @param index
     * @return New ComponentSelector with {@code index}th child of each container in the current found set.
     */
    public ComponentSelector getComponentAt(int index) {
        HashSet<Component> out = new HashSet<Component>();
        for (Component c : this) {
            if (c instanceof Container) {
                Container cnt = (Container)c;
                if (cnt.getComponentCount() > index) {
                    out.add(cnt.getComponentAt(index));
                }
            }
        }
        return new ComponentSelector(out);
    }
    
    /**
     * Returns true if any of the containers in the current found set contains
     * the provided component in its subtree.
     * Wraps {@link Container#contains(com.codename1.ui.Component) }
     * @param cmp
     * @return 
     */
    public boolean containsInSubtree(Component cmp) {
        for (Component c : this) {
            if (c instanceof Container) {
                if (((Container)c).contains(cmp)) {
                    return true;
                }
            }
            
        }
        return false;
    }
    
    /**
     * Wraps {@link Container#scrollComponentToVisible(com.codename1.ui.Component) }
     * @param cmp
     * @return 
     */
    public ComponentSelector scrollComponentToVisible(Component cmp) {
        for (Component c : this) {
            if (c instanceof Container) {
                ((Container)c).scrollComponentToVisible(cmp);   
            }
        }
        return this;
    }
    
    /**
     * Returns new ComponentSelector with the set of all components returned from calling
     * {@link Container#getComponentAt(int, int) } in the current found set.
     * @param x
     * @param y
     * @return New ComponentSelector with components at the given coordinates.
     */
    public ComponentSelector getComponentAt(int x, int y) {
        HashSet<Component> out = new HashSet<Component>();
        for (Component c : this) {
            if (c instanceof Container) {
                Component match = ((Container)c).getComponentAt(x, y);
                if (match != null) {
                    out.add(match);
                }
            }
        }
        return new ComponentSelector(out);
    }
    
    /**
     * Wraps {@link Container#setScrollableX(boolean)  }
     * @param b
     * @return 
     */
    public ComponentSelector setScrollableX(boolean b) {
         for (Component c : this) {
            if (c instanceof Container) {
                ((Container)c).setScrollableX(b);
            }
        }
        return this;
    }
    
    /**
     * Wraps {@link Container#setScrollableY(boolean)  }
     * @param b
     * @return 
     */
    public ComponentSelector setScrollableY(boolean b) {
         for (Component c : this) {
            if (c instanceof Container) {
                ((Container)c).setScrollableY(b);
            }
        }
        return this;
    }
    
    /**
     * Wraps {@link Container#setScrollIncrement(int)  }
     * @param b
     * @return 
     */
    public ComponentSelector setScrollIncrement(int b) {
         for (Component c : this) {
            if (c instanceof Container) {
                ((Container)c).setScrollIncrement(b);
            }
        }
        return this;
    }
    
    /**
     * Creates new ComponentSelector with the set of first focusable elements of
     * each of the containers in the current result set.
     * @return New component selector with first focusable element of each container in 
     * current found set.
     * @see Container#findFirstFocusable() 
     */
    public ComponentSelector findFirstFocusable() {
        HashSet<Component> out = new HashSet<Component>();
        for (Component c : this) {
            if (c instanceof Container) {
                Component match = ((Container)c).findFirstFocusable();
                if (match != null) {
                    out.add(match);
                }
            }
        }
        return new ComponentSelector(out);
    } 
    
    /**
     * Wraps {@link Container#animateHierarchyAndWait(int) }.  
     * @param duration
     * @return 
     */
    public ComponentSelector animateHierarchyAndWait(int duration) {
        ArrayList<ComponentAnimation> animations = new ArrayList<ComponentAnimation>();
        for (Component c : this) {
            if (c instanceof Container) {
                animations.add(((Container)c).createAnimateHierarchy(duration));
            }
        }
        AnimationManager mgr = getAnimationManager();
        if (animations.size() > 0 && mgr != null) {
            mgr.addAnimationAndBlock(ComponentAnimation.compoundAnimation(animations.toArray(new ComponentAnimation[animations.size()])));
        }
        return this;
    }
    
    /**
     * Animates the hierarchy of containers in this set.  Wraps {@link Container#animateHierarchy(int) }
     * @param duration
     * @return 
     */
    public ComponentSelector animateHierarchy(int duration) {
        return animateHierarchy(duration, null);
    }
    /**
     * Wraps {@link Container#animateHierarchy(int) }.  
     * @param duration
     * @param callback
     * @return 
     */
    public ComponentSelector animateHierarchy(int duration, final SuccessCallback<ComponentSelector> callback) {
        ArrayList<ComponentAnimation> animations = new ArrayList<ComponentAnimation>();
        for (Component c : this) {
            if (c instanceof Container) {
                animations.add(((Container)c).createAnimateHierarchy(duration));
            }
        }
        AnimationManager mgr = getAnimationManager();
        if (animations.size() > 0 && mgr != null) {
            mgr.addAnimation(ComponentAnimation.compoundAnimation(animations.toArray(new ComponentAnimation[animations.size()])), new Runnable() {
                public void run() {
                    if (callback != null) {
                        callback.onSucess(ComponentSelector.this);
                    }
                }
            });
        }
        return this;
    }
    
    /**
     * Wraps {@link Container#animateHierarchyFadeAndWait(int,int) }.  
     * @param duration
     * @param startingOpacity
     * @return 
     */
    public ComponentSelector animateHierarchyFadeAndWait(int duration, int startingOpacity) {
         ArrayList<ComponentAnimation> animations = new ArrayList<ComponentAnimation>();
        for (Component c : this) {
            if (c instanceof Container) {
                animations.add(((Container)c).createAnimateHierarchyFade(duration, startingOpacity));
            }
        }
        AnimationManager mgr = getAnimationManager();
        if (animations.size() > 0 && mgr != null) {
            mgr.addAnimationAndBlock(ComponentAnimation.compoundAnimation(animations.toArray(new ComponentAnimation[animations.size()])));
        }
        return this;
    }
    
    /**
     * Wraps {@link Container#animateHierarchyAndWait(int) }
     * @param duration The duration of the animation.
     * @param startingOpacity The starting opacity.
     * @return Self for chaining.
     */
    public ComponentSelector animateHierarchyFade(int duration, int startingOpacity) {
        return animateHierarchyFade(duration, startingOpacity, null);
    }
    
    /**
     * Wraps {@link Container#animateHierarchyFade(int,int) }.  
     * @param duration
     * @param startingOpacity
     * @param callback
     * @return 
     */
    public ComponentSelector animateHierarchyFade(int duration, int startingOpacity, final SuccessCallback<ComponentSelector> callback) {
        ArrayList<ComponentAnimation> animations = new ArrayList<ComponentAnimation>();
        for (Component c : this) {
            if (c instanceof Container) {
                animations.add(((Container)c).createAnimateHierarchyFade(duration, startingOpacity));
            }
        }
        AnimationManager mgr = getAnimationManager();
        if (animations.size() > 0 && mgr != null) {
            mgr.addAnimation(ComponentAnimation.compoundAnimation(animations.toArray(new ComponentAnimation[animations.size()])), new Runnable() {
                public void run() {
                    if (callback != null) {
                        callback.onSucess(ComponentSelector.this);
                    }
                }
            });
        }
        return this;
    }
    
    /**
     * Wraps {@link Container#animateLayoutFadeAndWait(int,int) }.  
     * @param duration
     * @param startingOpacity
     * @return 
     */
    public ComponentSelector animateLayoutFadeAndWait(int duration, int startingOpacity) {
        ArrayList<ComponentAnimation> animations = new ArrayList<ComponentAnimation>();
        for (Component c : this) {
            if (c instanceof Container) {
                animations.add(((Container)c).createAnimateLayoutFade(duration, startingOpacity));
            }
        }
        AnimationManager mgr = getAnimationManager();
        if (animations.size() > 0 && mgr != null) {
            mgr.addAnimationAndBlock(ComponentAnimation.compoundAnimation(animations.toArray(new ComponentAnimation[animations.size()])));
        }
        return this;
    }
    
    /**
     * Animates layout with fade on all containers in this set.  Wraps {@link Container#animateLayoutFade(int, int) }.
     * @param duration
     * @param startingOpacity
     * @return Self for chaining.
     */
    public ComponentSelector animateLayoutFade(int duration, int startingOpacity) {
        return animateLayoutFade(duration, startingOpacity, null);
    }
    
    /**
     * Wraps {@link Container#animateLayoutFade(int,int) }. 
     * @param duration
     * @param startingOpacity
     * @param callback
     * @return 
     */
    public ComponentSelector animateLayoutFade(int duration, int startingOpacity, final SuccessCallback<ComponentSelector> callback) {
        ArrayList<ComponentAnimation> animations = new ArrayList<ComponentAnimation>();
        for (Component c : this) {
            if (c instanceof Container) {
                animations.add(((Container)c).createAnimateLayoutFade(duration, startingOpacity));
            }
        }
        AnimationManager mgr = getAnimationManager();
        if (animations.size() > 0 && mgr != null) {
            mgr.addAnimation(ComponentAnimation.compoundAnimation(animations.toArray(new ComponentAnimation[animations.size()])), new Runnable() {

                public void run() {
                    if (callback != null) {
                        callback.onSucess(ComponentSelector.this);
                    }
                }
                
            });
        }
        return this;
    }
    
    /**
     * Wraps {@link Container#animateLayout(int) }
     * @param duration
     * @return 
     */
    public ComponentSelector animateLayout(int duration) {
        return animateLayout(duration, null);
    }
    
    /**
     * Wraps {@link Container#animateLayout(int) }. 
     * @param duration
     * @param callback
     * @return 
     */
    public ComponentSelector animateLayout(int duration, final SuccessCallback<ComponentSelector> callback) {
        ArrayList<ComponentAnimation> animations = new ArrayList<ComponentAnimation>();
        for (Component c : this) {
            if (c instanceof Container) {
                animations.add(((Container)c).createAnimateLayout(duration));
            }
        }
        AnimationManager mgr = getAnimationManager();
        if (animations.size() > 0 && mgr != null) {
            mgr.addAnimation(ComponentAnimation.compoundAnimation(animations.toArray(new ComponentAnimation[animations.size()])), new Runnable() {

                public void run() {
                    if (callback != null) {
                        callback.onSucess(ComponentSelector.this);
                    }
                }
                
            });
        }
        return this;
    }
    
    /**
     * Wraps {@link Container#animateLayoutAndWait(int) }
     * @param duration
     * @return 
     */
    public ComponentSelector animateLayoutAndWait(int duration) {
        ArrayList<ComponentAnimation> animations = new ArrayList<ComponentAnimation>();
        for (Component c : this) {
            if (c instanceof Container) {
                animations.add(((Container)c).createAnimateLayout(duration));
            }
        }
        AnimationManager mgr = getAnimationManager();
        if (animations.size() > 0 && mgr != null) {
            mgr.addAnimationAndBlock(ComponentAnimation.compoundAnimation(animations.toArray(new ComponentAnimation[animations.size()])));
        }
        return this;
    }
    
    /**
     * Wraps {@link Container#animateUnlayout(int, int, java.lang.Runnable) }
     * @param duration
     * @param opacity
     * @return 
     */
    public ComponentSelector animateUnlayout(int duration, int opacity) {
        return animateUnlayout(duration, opacity, null);
    }
    
    /**
     * Wraps {@link Container#animateUnlayout(int, int, java.lang.Runnable) }
     * @param duration
     * @param opacity
     * @param callback Callback to run when animation has completed.
     * @return 
     */
    public ComponentSelector animateUnlayout(int duration, int opacity, final SuccessCallback<ComponentSelector> callback) {
        ArrayList<ComponentAnimation> animations = new ArrayList<ComponentAnimation>();
        for (Component c : this) {
            if (c instanceof Container) {
                
                animations.add(((Container)c).createAnimateUnlayout(duration, opacity, null));
            }
        }
        AnimationManager mgr = getAnimationManager();
        if (animations.size() > 0 && mgr != null) {
            mgr.addAnimation(ComponentAnimation.compoundAnimation(animations.toArray(new ComponentAnimation[animations.size()])), new Runnable() {
                public void run() {
                    if (callback != null) {
                        callback.onSucess(ComponentSelector.this);
                    }
                }
            });
        }
        return this;
    }
    
    /**
     * Wraps {@link Container#animateUnlayoutAndWait(int, int) }
     * @param duration
     * @param opacity
     * @return 
     */
    public ComponentSelector animateUnlayoutAndWait(int duration, int opacity) {
        ArrayList<ComponentAnimation> animations = new ArrayList<ComponentAnimation>();
        for (Component c : this) {
            if (c instanceof Container) {
                
                animations.add(((Container)c).createAnimateUnlayout(duration, opacity, null));
            }
        }
        AnimationManager mgr = getAnimationManager();
        if (animations.size() > 0 && mgr != null) {
            mgr.addAnimationAndBlock(ComponentAnimation.compoundAnimation(animations.toArray(new ComponentAnimation[animations.size()])));
        }
        return this;
    }
    
    /**
     * Gets the AnimationManager for the components in this set.
     * 
     * @return The AnimationManager for the components in this set.
     * @see Component#getAnimationManager() 
     */
    public AnimationManager getAnimationManager() {
        for (Component c : this) {
            AnimationManager mgr = c.getAnimationManager();
            if (mgr != null) {
                return mgr;
            }
        }
        return null;
    }
    
    /**
     * Sets the text on all components in found set that support this.  Currently this works with
     * Label, TextArea, SpanLabel, and SpanButtons, and subclasses thereof.
     * @param text The text to set in the componnet.
     * @return 
     */
    public ComponentSelector setText(String text) {
        for (Component c : this) {
            if (c instanceof Label) {
                ((Label)c).setText(text);
            } else if (c instanceof TextArea) {
                ((TextArea)c).setText(text);
            } else if (c instanceof SpanLabel) {
                ((SpanLabel)c).setText(text);
            } else if (c instanceof SpanButton) {
                ((SpanButton)c).setText(text);
            }
        }
        return this;
    }
    
    /**
     * Gets the text on the first component in this set that supports this property.  Currently this works with
     * Label, TextArea, SpanLabel, and SpanButtons, and subclasses thereof. 
     * @return 
     */
    public String getText() {
        for (Component c : this) {
            if (c instanceof Label) {
                return ((Label)c).getText();
            } else if (c instanceof TextArea) {
                return ((TextArea)c).getText();
            } else if (c instanceof SpanLabel) {
                return ((SpanLabel)c).getText();
            } else if (c instanceof SpanButton) {
                return ((SpanButton)c).getText();
            }
        }
        return null;
    }
    
    /**
     * Sets the icon for components in found set.  Only relevant to Labels, SpanLabels, and SpanButtons, and subclasses thereof.
     * @param icon
     * @return 
     */
    public ComponentSelector setIcon(Image icon) {
        for (Component c : this) {
            if (c instanceof Label) {
                ((Label)c).setIcon(icon);
            } else if (c instanceof SpanLabel) {
                ((SpanLabel)c).setIcon(icon);
            } else if (c instanceof SpanButton) {
                ((SpanButton)c).setIcon(icon);
            }
        }
        return this;
    }
    
    /**
     * Sets the icons of all elements in this set to a material icon.
     * @param materialIcon Material icon charcode.
     * @param style The style for the icon.
     * @param size The size for the icon. (in mm)
     * @return Self for chaining.
     * @see FontImage#createMaterial(char, java.lang.String, float) 
     */
    public ComponentSelector setIcon(char materialIcon, Style style, float size) {
        FontImage img = FontImage.createMaterial(materialIcon, style, size);
        return setIcon(img);
    }
    
    
    /**
     * Sets the icon of all elements in this set to a material icon.  This will use
     * the foreground color of each label as the icon's foreground color.
     * @param materialIcon The icon charcode.
     * @param size The size of the icon (in mm)
     * @return Self for chaining
     * @see FontImage#createMaterial(char, com.codename1.ui.plaf.Style, float) 
     */
    public ComponentSelector setIcon(char materialIcon, float size) {
        for (Component c : this) {
            if (c instanceof Label) {
                Label l = (Label)c;
                Style style = new Style();
                Style cStyle = c.getUnselectedStyle();
                style.setBgTransparency(0);
                style.setFgColor(cStyle.getFgColor());
                l.setIcon(FontImage.createMaterial(materialIcon, style, size));
                
                if (c instanceof Button) {
                    Button b = (Button)c;
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
    
    /**
     * Sets the icon of all elements in this set to a material icon.  This will use
     * the foreground color of the label.
     * @param materialIcon The material icon charcode.
     * @return Self for chaining.
     */
    public ComponentSelector setIcon(char materialIcon) {
        for (Component c : this) {
            if (c instanceof Label) {
                Label l = (Label)c;
                Style style = new Style();
                Style cStyle = c.getUnselectedStyle();
                style.setBgTransparency(0);
                style.setFgColor(cStyle.getFgColor());
                
                l.setIcon(FontImage.createMaterial(materialIcon, style, 3));
                
                if (c instanceof Button) {
                    Button b = (Button)c;
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
    
    /**
     * Gets the set of all component forms from components in this set.
     * @return New ComponentSelector with forms all components in this set.
     */
    public ComponentSelector getComponentForm() {
        HashSet<Component> out = new HashSet<Component>();
        for (Component c : this) {
            out.add(c.getComponentForm());
        }
        return new ComponentSelector(out);
    }
    
    /**
     * Sets vertical alignment of text.  
     * @param valign
     * @return 
     * @see Label#setVerticalAlignment(int) 
     */
    public ComponentSelector setVerticalAlignment(int valign) {
        for (Component c : this) {
            if (c instanceof Label) {
                ((Label)c).setVerticalAlignment(valign);
            }
        }
        return this;
    }
    
    /**
     * Sets text position of text.  Only relevant to labels.
     * @param pos
     * @return 
     * @see Label#setTextPosition(int) 
     */
    public ComponentSelector setTextPosition(int pos) {
        for (Component c : this) {
            if (c instanceof Label) {
                ((Label)c).setTextPosition(pos);
            }
        }
        return this;
    }
    
    /**
     * Sets gap.  Only relevant to labels.
     * @param gap
     * @return 
     * @see Label#setGap(int) 
     * 
     */
    public ComponentSelector setGap(int gap) {
        for (Component c : this) {
            if (c instanceof Label) {
                ((Label)c).setGap(gap);
            }
        }
        return this;
    }
    
    /**
     * Sets shift text. Only relevant to labels.
     * @param shift
     * @return 
     * @see Label#setShiftText(int) 
     */
    public ComponentSelector setShiftText(int shift) {
        for (Component c : this) {
            if (c instanceof Label) {
                ((Label)c).setShiftText(shift);
            }
        }
        return this;
    }
    
    /**
     * Wraps {@link Label#startTicker(long, boolean) }
     * @param delay
     * @param rightToLeft
     * @return 
     */
    public ComponentSelector startTicker(long delay, boolean rightToLeft) {
        for (Component c : this) {
            if (c instanceof Label) {
                ((Label)c).startTicker(delay, rightToLeft);
            }
        }
        return this;
    }
    
    /**
     * Wraps {@link Label#stopTicker() }
     * @return 
     */
    public ComponentSelector stopTicker() {
        for (Component c : this) {
            if (c instanceof Label) {
                ((Label)c).stopTicker();
            }
        }
        return this;
    }
    
    /**
     * Wraps {@link Label#setTickerEnabled(boolean) }
     * @param b
     * @return 
     */
    public ComponentSelector setTickerEnabled(boolean b) {
        for (Component c : this) {
            if (c instanceof Label) {
                ((Label)c).setTickerEnabled(b);
            }
        }
        return this;
    }
    
    
    /**
     * Wraps {@link Label#setEndsWith3Points(boolean) }
     * @param b
     * @return 
     */
    public ComponentSelector setEndsWith3Points(boolean b) {
        for (Component c : this) {
            if (c instanceof Label) {
                ((Label)c).setEndsWith3Points(b);
            }
        }
        return this;
    }
    
    /**
     * Wraps {@link Label#setMask(java.lang.Object) }
     * @param mask
     * @return 
     */
    public ComponentSelector setMask(Object mask) {
        for (Component c : this) {
            if (c instanceof Label) {
                ((Label)c).setMask(mask);
            }
        }
        return this;
    }
    
    /**
     * Wraps {@link Label#setMaskName(java.lang.String) }
     * @param name
     * @return 
     */
    public ComponentSelector setMaskName(String name) {
        for (Component c : this) {
            if (c instanceof Label) {
                ((Label)c).setMaskName(name);
            }
        }
        return this;
    }
    
    /**
     * Wraps {@link Label#setShouldLocalize(boolean) }
     * @param b
     * @return 
     */
    public ComponentSelector setShouldLocalize(boolean b) {
        for (Component c : this) {
            if (c instanceof Label) {
                ((Label)c).setShouldLocalize(b);
            }
        }
        return this;
    }
    
    /**
     * Wraps {@link Label#setShiftMillimeters(int) }
     * @param b
     * @return 
     */
    public ComponentSelector setShiftMillimeters(int b) {
        for (Component c : this) {
            if (c instanceof Label) {
                ((Label)c).setShiftMillimeters(b);
            }
        }
        return this;
    }
    
    /**
     * Wraps {@link Label#setShowEvenIfBlank(boolean) }
     * @param b
     * @return 
     */
    public ComponentSelector setShowEvenIfBlank(boolean b) {
        for (Component c : this) {
            if (c instanceof Label) {
                ((Label)c).setShowEvenIfBlank(b);
            }
        }
        return this;
    }
    
    /**
     * Wraps {@link Label#setLegacyRenderer(boolean) }
     * @param b
     * @return 
     */
    public ComponentSelector setLegacyRenderer(boolean b) {
        for (Component c : this) {
            if (c instanceof Label) {
                ((Label)c).setLegacyRenderer(b);
            }
        }
        return this;
    }
    
    /**
     * Wraps {@link Label#setAutoSizeMode(boolean) }
     * @param b
     * @return 
     */
    public ComponentSelector setAutoSizeMode(boolean b) {
        for (Component c : this) {
            if (c instanceof Label) {
                ((Label)c).setAutoSizeMode(b);
            }
        }
        return this;
    }
    
    /**
     * Wraps {@link Button#setCommand(com.codename1.ui.Command) }
     * @param cmd
     * @return 
     */
    public ComponentSelector setCommand(Command cmd) {
        for (Component c : this) {
            if (c instanceof Button) {
                ((Button)c).setCommand(cmd);
            }
        }
        return this;
    }
    
    /**
     * Wraps {@link Button#setRolloverPressedIcon(com.codename1.ui.Image) }
     * @param icon
     * @return 
     */
    public ComponentSelector setRolloverPressedIcon(Image icon) {
        for (Component c : this) {
            if (c instanceof Button) {
                ((Button)c).setRolloverPressedIcon(icon);
            }
        }
        return this;
    }
    
    /**
     * Wraps {@link Button#setRolloverIcon(com.codename1.ui.Image) }
     * @param icon
     * @return 
     */
    public ComponentSelector setRolloverIcon(Image icon) {
        for (Component c : this) {
            if (c instanceof Button) {
                ((Button)c).setRolloverIcon(icon);
            }
        }
        return this;
    }
    
    /**
     * Wraps {@link Button#setPressedIcon(com.codename1.ui.Image) }
     * @param icon
     * @return 
     */
    public ComponentSelector setPressedIcon(Image icon) {
        for (Component c : this) {
            if (c instanceof Button) {
                ((Button)c).setPressedIcon(icon);
            }
        }
        return this;
    }
    
    /**
     * Wraps {@link Button#setDisabledIcon(com.codename1.ui.Image) }
     * @param icon
     * @return 
     */
    public ComponentSelector setDisabledIcon(Image icon) {
        for (Component c : this) {
            if (c instanceof Button) {
                ((Button)c).setDisabledIcon(icon);
            }
        }
        return this;
    }
    
    /**
     * Adds action listener to applicable components in found set.  Currently this will apply to 
     * Buttons, Lists, Sliders, TextAreas, OnOffSwitches, SpanButtons, and subclasses thereof.
     * @param l
     * @return 
     */
    public ComponentSelector addActionListener(ActionListener l) {
        for (Component c : this) {
            if (c instanceof Button) {
                ((Button)c).addActionListener(l);
            } else if (c instanceof com.codename1.ui.List) {
                ((com.codename1.ui.List)c).addActionListener(l);
            } else if (c instanceof Slider) {
                ((Slider)c).addActionListener(l);
            } else if (c instanceof TextArea) {
                ((TextArea)c).addActionListener(l);
            } else if (c instanceof OnOffSwitch) {
                ((OnOffSwitch)c).addActionListener(l);
            } else if (c instanceof SpanButton) {
                ((SpanButton)c).addActionListener(l);
            }
        }
        return this;
    }
    
    /**
     * Wraps {@link TextArea#setEditable(boolean) }
     * @param b
     * @return 
     */
    public ComponentSelector setEditable(boolean b) {
        for (Component c : this) {
            if (c instanceof TextArea) {
                ((TextArea)c).setEditable(b);
            }
        }
        return this;
    }
    
    /**
     * Wraps {@link TextField#addDataChangedListener(com.codename1.ui.events.DataChangedListener) }
     * @param l
     * @return 
     */
    public ComponentSelector addDataChangedListener(DataChangedListener l) {
        for (Component c : this) {
            if (c instanceof TextField) {
                ((TextField)c).addDataChangedListener(l);
            }
        }
        return this;
    }
    
    /**
     * Wraps {@link TextField#removeDataChangedListener(com.codename1.ui.events.DataChangedListener) }
     * @param l
     * @return 
     */
    public ComponentSelector removeDataChangedListener(DataChangedListener l) {
        for (Component c : this) {
            if (c instanceof TextField) {
                ((TextField)c).removeDataChangedListener(l);
            }
        }
        return this;
    }
    
    /**
     * Wraps {@link TextField#setDoneListener(com.codename1.ui.events.ActionListener) }
     * @param l
     * @return 
     */
    public ComponentSelector setDoneListener(ActionListener l) {
        for (Component c : this) {
            if (c instanceof TextField) {
                ((TextField)c).setDoneListener(l);
            }
        }
        return this;
    }
    
    // Now for the style stuff
    
    private static int dip2px(float dips) {
        return Display.getInstance().convertToPixels(dips);
    }
    
    /**
     * Sets padding to all sides of found set components in pixels.
     * @param padding Padding in pixels
     * @return 
     * @see #getStyle(com.codename1.ui.Component) 
     */
    public ComponentSelector setPadding(int padding) {
        return setPadding(padding, padding, padding, padding);
    }
    
    
    /**
     * Sets padding to all components in found set.
     * @param top Top padding in pixels.
     * @param right Right padding in pixels
     * @param bottom Bottom padding in pixels.
     * @param left Left padding in pixels.
     * @return 
     * @see #getStyle(com.codename1.ui.Component) 
     */
    public ComponentSelector setPadding(int top, int right, int bottom, int left) {
        Style s = currentStyle();
        s.setPaddingUnit(Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS);
        s.setPadding(top, bottom, left, right);
        return this;
    }
    
    /**
     * Sets padding to all components in found set in millimeters.
     * @param top Top padding in mm.
     * @param right Right padding in mm
     * @param bottom Bottom padding in mm.
     * @param left Left padding in mm.
     * @return 
     * @see #getStyle(com.codename1.ui.Component) 
     */
    public ComponentSelector setPaddingMillimeters(float top, float right, float bottom, float left) {
        return setPadding(dip2px(top), dip2px(right), dip2px(bottom), dip2px(left));
    }
    
    /**
     * Sets padding in millimeters to all components in found set.
     * @param topBottom Top and bottom padding in mm.
     * @param leftRight Left and right padding in mm.
     * @return 
     * @see #getStyle(com.codename1.ui.Component) 
     */
    public ComponentSelector setPaddingMillimeters(float topBottom, float leftRight) {
        return setPaddingMillimeters(topBottom, leftRight, topBottom, leftRight);
    };
    
    /**
     * Sets padding to all components in found set.
     * @param padding Padding applied to all sides in mm.
     * @return 
     */
    public ComponentSelector setPaddingMillimeters(float padding) {
        return setPaddingMillimeters(padding, padding);
    }
    
    /**
     * Sets padding on all components in found set.
     * @param topBottom Top and bottom padding in pixels.
     * @param leftRight Left and right padding in pixels.
     * @return 
     */
    public ComponentSelector setPadding(int topBottom, int leftRight) {
        return setPadding(topBottom, leftRight, topBottom, leftRight);
    }
    

    private int percentHeight(double percentage, Component parent) {
        return (int)Math.round((parent.getHeight() * percentage / 100));
    }
    
    private int percentWidth(double percentage, Component parent) {
        return (int)Math.round((parent.getWidth() * percentage / 100));
    }
    
    /**
     * Sets the padding on all components in found set as a percentage of their respective
     * parents' dimensions.  Horizontal padding is set as a percentage of parent width.  Vertical padding
     * is set as a percentage of parent height.
     * @param padding The padding expressed as a percent.
     * @return 
     */
    public ComponentSelector setPaddingPercent(double padding) {
        return setPaddingPercent(padding, padding, padding, padding);
        
    }
    
    /**
     * Sets padding on all components in found set as a percentage of their respective parents' dimensions.
     * @param topBottom Top and bottom padding as percentage of parent heights.
     * @param leftRight Left and right padding as percentage of parent widths.
     * @return 
     */
    public ComponentSelector setPaddingPercent(double topBottom, double leftRight) {
        return setPaddingPercent(topBottom, leftRight, topBottom, leftRight);
    }
    
    /**
     * Sets padding on all components in found set as a percentage of their respective parents' dimensions.
     * @param top Top padding as percentage of parent height.
     * @param right Right padding as percentage of parent width.
     * @param bottom Bottom padding as percentage of parent height.
     * @param left Left padding as percentage of parent width.
     * @return 
     */
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
    
    /**
     * Sets margin to all sides of found set components in pixels.
     * @param margin Margin in pixels
     * @return 
     * @see #getStyle(com.codename1.ui.Component) 
     */
    public ComponentSelector setMargin(int margin) {
        return setMargin(margin, margin, margin, margin);
    }
    
    
    /**
     * Sets margin to all components in found set.
     * @param top Top margin in pixels.
     * @param right Right margin in pixels
     * @param bottom Bottom margin in pixels.
     * @param left Left margin in pixels.
     * @return 
     * @see #getStyle(com.codename1.ui.Component) 
     */
    public ComponentSelector setMargin(int top, int right, int bottom, int left) {
        Style s = currentStyle();
        s.setMarginUnit(Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS);
        s.setMargin(top, bottom, left, right);
        return this;
    }
    
    /**
     * Sets margin on all components in found set.
     * @param topBottom Top and bottom margin in pixels.
     * @param leftRight Left and right margin in pixels.
     * @return 
     */
    public ComponentSelector setMargin(int topBottom, int leftRight) {
        return setMargin(topBottom, leftRight, topBottom, leftRight);
    }
    
    /**
     * Sets the margin on all components in found set as a percentage of their respective
     * parents' dimensions.  Horizontal margin is set as a percentage of parent width.  Vertical margin
     * is set as a percentage of parent height.
     * @param margin The margin expressed as a percent.
     * @return 
     */
    public ComponentSelector setMarginPercent(double margin) {
        return setMarginPercent(margin, margin, margin, margin);
        
    }
    
    /**
     * Sets margin on all components in found set as a percentage of their respective parents' dimensions.
     * @param topBottom Top and bottom margin as percentage of parent heights.
     * @param leftRight Left and right margin as percentage of parent widths.
     * @return 
     */
    public ComponentSelector setMarginPercent(double topBottom, double leftRight) {
        return setMarginPercent(topBottom, leftRight, topBottom, leftRight);
    }
    
    /**
     * Sets margin on all components in found set as a percentage of their respective parents' dimensions.
     * @param top Top margin as percentage of parent height.
     * @param right Right margin as percentage of parent width.
     * @param bottom Bottom margin as percentage of parent height.
     * @param left Left margin as percentage of parent width.
     * @return 
     */
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
    
    /**
     * Sets margin to all components in found set in millimeters.
     * @param top Top margin in mm.
     * @param right Right margin in mm
     * @param bottom Bottom margin in mm.
     * @param left Left margin in mm.
     * @return 
     * @see #getStyle(com.codename1.ui.Component) 
     */
    public ComponentSelector setMarginMillimeters(float top, float right, float bottom, float left) {
        return setMargin(dip2px(top), dip2px(right), dip2px(bottom), dip2px(left));
    }
    
    /**
     * Sets margin in millimeters to all components in found set.
     * @param topBottom Top and bottom margin in mm.
     * @param leftRight Left and right margin in mm.
     * @return 
     * @see #getStyle(com.codename1.ui.Component) 
     */
    public ComponentSelector setMarginMillimeters(float topBottom, float leftRight) {
        return setMarginMillimeters(topBottom, leftRight, topBottom, leftRight);
    };
    
    /**
     * Sets margin to all components in found set.
     * @param margin Margin applied to all sides in mm.
     * @return 
     */
    public ComponentSelector setMarginMillimeters(float margin) {
        return setMarginMillimeters(margin, margin);
    }
    
    /**
     * Creates a proxy style to mutate the styles of all component styles in found set.
     * @return 
     * @see #getStyle(com.codename1.ui.Component) 
     * @see Style#createProxyStyle(com.codename1.ui.plaf.Style...) 
     */
    public Style createProxyStyle() {
        HashSet<Style> styles = new HashSet<Style>();
        for (Component c : this) {
            styles.add(getStyle(c));
        }
        return Style.createProxyStyle(styles.toArray(new Style[styles.size()]));
    }
    
    /**
     * Merges style with all styles of components in current found set.
     * @param style
     * @return 
     * @see Style#merge(com.codename1.ui.plaf.Style) 
     * @see #getStyle(com.codename1.ui.Component) 
     */
    public ComponentSelector merge(Style style) {
        for (Component c : this) {
            getStyle(c).merge(style);
        }
        return this;
    }
    
    /**
     * Wraps {@link Style#setBgColor(int) }
     * @param bgColor
     * @return 
     * @see #getStyle(com.codename1.ui.Component) 
     */
    public ComponentSelector setBgColor(int bgColor) {
        currentStyle().setBgColor(bgColor);
        return this;
    }
    
    /**
     * Wraps {@link Style#setAlignment(int) }
     * @param alignment
     * @return 
     * @see #getStyle(com.codename1.ui.Component) 
     */
    public ComponentSelector setAlignment(int alignment) {
        currentStyle().setAlignment(alignment);
        return this;
    }
    
    /**
     * Wraps {@link Style#setBgImage(com.codename1.ui.Image)  }
     * @param bgImage
     * @return 
     * @see #getStyle(com.codename1.ui.Component) 
     */
    public ComponentSelector setBgImage(Image bgImage) {
        currentStyle().setBgImage(bgImage);
        return this;
    }
    
    /**
     * Wraps {@link Style#setBackgroundType(byte)  }
     * @param backgroundType
     * @return 
     * @see #getStyle(com.codename1.ui.Component) 
     */
    public ComponentSelector setBackgroundType(byte backgroundType) {
        currentStyle().setBackgroundType(backgroundType);
        return this;
    }
    
    /**
     * Wraps {@link Style#setBackgroundGradientStartColor(int) }
     * @param startColor
     * @return 
     * @see #getStyle(com.codename1.ui.Component) 
     */
    public ComponentSelector setBackgroundGradientStartColor(int startColor) {
        currentStyle().setBackgroundGradientStartColor(startColor);
        return this;
    }
    
    /**
     * Wraps {@link Style#setBackgroundGradientEndColor(int) (int) }
     * @param endColor
     * @return 
     * @see #getStyle(com.codename1.ui.Component) 
     */
    public ComponentSelector setBackgroundGradientEndColor(int endColor) {
        currentStyle().setBackgroundGradientEndColor(endColor);
        return this;
    }
    
    /**
     * Wraps {@link Style#setBackgroundGradientRelativeX(float) }
     * @param x
     * @return 
     * @see #getStyle(com.codename1.ui.Component) 
     */
    public ComponentSelector setBackgroundGradientRelativeX(float x) {
        currentStyle().setBackgroundGradientRelativeX(x);
        return this;
    }
    
    /**
     * Wraps {@link Style#setBackgroundGradientRelativeY(float)  }
     * @param y
     * @return 
     * @see #getStyle(com.codename1.ui.Component) 
     */
    public ComponentSelector setBackgroundGradientRelativeY(float y) {
        currentStyle().setBackgroundGradientRelativeY(y);
        return this;
    }
    
    /**
     * Wraps {@link Style#setBackgroundGradientRelativeSize(float)  }
     * @param size
     * @return 
     * @see #getStyle(com.codename1.ui.Component) 
     */
    public ComponentSelector setBackgroundGradientRelativeSize(float size) {
        currentStyle().setBackgroundGradientRelativeSize(size);
        return this;
    }
    
    /**
     * Wraps {@link Style#setFgColor(int)  }
     * @param color
     * @return 
     * @see #getStyle(com.codename1.ui.Component) 
     */
    public ComponentSelector setFgColor(int color) {
        currentStyle().setFgColor(color);
        return this;
    }
    
    /**
     * Wraps {@link Style#setFont(com.codename1.ui.Font)  }
     * @param f
     * @return 
     * @see #getStyle(com.codename1.ui.Component) 
     */
    public ComponentSelector setFont(Font f) {
        currentStyle().setFont(f);
        return this;
    }
    
    /**
     * Wraps {@link Style#setUnderline(boolean) }
     * @param b
     * @return 
     * @see #getStyle(com.codename1.ui.Component) 
     */
    public ComponentSelector setUnderline(boolean b) {
        currentStyle().setUnderline(b);
        return this;
    }
    
    /**
     * Wraps {@link Style#set3DText(boolean, boolean) }
     * @param t
     * @param raised
     * @return 
     * @see #getStyle(com.codename1.ui.Component) 
     */
    public ComponentSelector set3DText(boolean t, boolean raised) {
        currentStyle().set3DText(t, raised);
        return this;
    }
    
    /**
     * Wraps {@link Style#set3DTextNorth(boolean) }
     * @param north
     * @return 
     * @see #getStyle(com.codename1.ui.Component) 
     */
    public ComponentSelector set3DTextNorth(boolean north) {
        currentStyle().set3DTextNorth(north);
        return this;
    }
    
    /**
     * Wraps {@link Style#setOverline(boolean) }
     * @param b
     * @return 
     * @see #getStyle(com.codename1.ui.Component) 
     */
    public ComponentSelector setOverline(boolean b) {
        currentStyle().setOverline(b);
        return this;
    }
    
    /**
     * Wraps {@link Style#setStrikeThru(boolean)  }
     * @param b
     * @return 
     * @see #getStyle(com.codename1.ui.Component) 
     */
    public ComponentSelector setStrikeThru(boolean b) {
        currentStyle().setStrikeThru(b);
        return this;
    }
    
    /**
     * Wraps {@link Style#setTextDecoration(int)  }
     * @param textDecoration
     * @return 
     * @see #getStyle(com.codename1.ui.Component) 
     */
    public ComponentSelector setTextDecoration(int textDecoration) {
        currentStyle().setTextDecoration(textDecoration);
        return this;
    }
    
    /**
     * Wraps {@link Style#setBgTransparency(byte) }
     * @param bgTransparency
     * @return 
     * @see #getStyle(com.codename1.ui.Component) 
     */
    public ComponentSelector setBgTransparency(int bgTransparency) {
        currentStyle().setBgTransparency(bgTransparency);
        return this;
    }
    
    /**
     * Wraps {@link Style#setOpacity(int)  }
     * @param opacity
     * @return 
     * @see #getStyle(com.codename1.ui.Component) 
     */
    public ComponentSelector setOpacity(int opacity) {
        currentStyle().setOpacity(opacity);
        
        return this;
    }
    
    /**
     * Wraps {@link Style#addStyleListener(com.codename1.ui.events.StyleListener) }
     * @param l
     * @return 
     * @see #getStyle(com.codename1.ui.Component) 
     */
    public ComponentSelector addStyleListener(StyleListener l) {
        currentStyle().addStyleListener(l);
        return this;
    }
    
    /**
     * Wraps {@link Style#removeStyleListener(com.codename1.ui.events.StyleListener)  }
     * @param l
     * @return 
     * @see #getStyle(com.codename1.ui.Component) 
     */
    public ComponentSelector removeStyleListener(StyleListener l) {
        currentStyle().removeStyleListener(l);
        return this;
    }
    
    /**
     * Wraps {@link Style#removeListeners() }
     * @return 
     * @see #getStyle(com.codename1.ui.Component) 
     */
    public ComponentSelector removeStyleListeners() {
        currentStyle().removeListeners();
        return this;
    }
    
    /**
     * Wraps {@link Style#setBorder(com.codename1.ui.plaf.Border)  }
     * @param b
     * @return 
     * @see #getStyle(com.codename1.ui.Component) 
     */
    public ComponentSelector setBorder(Border b) {
        currentStyle().setBorder(b);
        return this;
    }
    
    /**
     * Wraps {@link Style#setBgPainter(com.codename1.ui.Painter)  }
     * @param bgPainter
     * @return 
     * @see #getStyle(com.codename1.ui.Component) 
     */
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
    
    /**
     * Sets the font size of all components in found set.  In pixels.
     * @param size Font size in pixels.
     * @return 
     * @see #getStyle(com.codename1.ui.Component) 
     */
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
    
    public ComponentSelector setCursor(int cursor) {
        for (Component c : this) {
            c.setCursor(cursor);
        }
        return this;
    }
    
    /**
     * Sets the font size of all components in found set.  In millimeters.
     * @param sizeMM Font size in mm.
     * @return 
     */
    public ComponentSelector setFontSizeMillimeters(float sizeMM) {
        return setFontSize(dip2px(sizeMM));
    }
    
    /**
     * Sets the fonts size of all components in the found set as a percentage of the font
     * size of the components' respective parents.
     * @param sizePercentage Font size as a percentage of parent font size.
     * @return 
     */
    public ComponentSelector setFontSizePercent(double sizePercentage) {
        for (Component c : this) {
            Component parent = c.getParent();
                if (parent != null) {
                    float size = (float)(getEffectiveFontSize(parent) * sizePercentage / 100.0);
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
    
    
    
    
    
}
