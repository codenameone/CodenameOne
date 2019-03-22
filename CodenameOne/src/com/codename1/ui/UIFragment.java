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

import com.codename1.io.CharArrayReader;
import com.codename1.io.JSONParser;
import com.codename1.io.Log;

import com.codename1.io.Util;
import static com.codename1.ui.ComponentSelector.$;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.table.TableLayout;
import com.codename1.xml.Element;
import com.codename1.xml.XMLParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Vector;

/**
 * Encapsulates an XML or JSON template for a UI component hierarchy.  The UI can be defined
 * using XML.  The XML is compiled into a view at runtime.  Custom components may be
 * injected into the template using special placeholder tags (i.e. tags where the tag
 * name begins with '$'.
 * 
 * <h3>Supported Tags</h3>
 * 
 * <ul>
 * <li><strong>border</strong> - A container with layout=BorderLayout</li>
 * <li><strong>y</strong> - A container with layout=BoxLayout Y</li>
 * <li><strong>x</strong> - A container with layout=BoxLayout X</li>
 * <li><strong>flow</strong> - A container with layout=FlowLayout</li>
 * <li><strong>layered</strong> - A container with layout=LayeredLayout</li>
 * <li><strong>grid</strong> - A container with layout=GridLayout.  Accepted attributes {@literal rows} and {@literal cols}</li>
 * <li><strong>table</strong> - A container with layout=TableLayout.  Accepted attributes {@literal rows} and {@literal cols}.  May have zero or more nested {@literal <tr>} tags.
 * <li><strong>label</strong> - A Label</li>
 * <li><strong>button</strong> - A button</li>
 * </ul>
 * 
 * <h3>Layout Variant Tags</h3>
 * 
 * <p>BorderLayout and BoxLayout include some variant tags to customize their behaviour also:</p>
 * 
 * <ul>
 *  <li><strong>borderAbs, borderAbsolute</strong> - BorderLayout with center absolute behaviour.  This is the same as <code>&lt;border behavior='absolute'/&gt;</code></li>
 *  <li><strong>borderTotalBelow</strong> - BorderLayout with Total Below center behaviour. This is the same as <code>&lt;border behavior='totalBelow'/&gt;</code></li>
 *  <li><strong>yBottomLast, ybl</strong> - BoxLayout with Y_BOTTOM_LAST setting.</li>
 *  <li><strong>xNoGrow, xng</strong> - BoxLayout X with No Grow option.  This is the same as <code>&lt;x noGrow='true'/&gt;</code></li>
 * </ul>
 * 
 * <h3>Supported Attributes</h3>
 * 
 * <ul>
 * <li><strong>uiid</strong> - The UIID of the component.  I.e. {@link Component#getUIID() }</li>
 * <li><strong>id</strong> - The ID of the component so that it can be retrieved using {@link #findById(java.lang.String) }</li>
 * <li><strong>name</strong> - The name of the component (i.e. {@link Component#getName() }</li>
 * <li><strong>constraint</strong> - The layout constraint used for adding to
 * the parent. Supports north, south, east, west, center, when parent is
 * border</li>
 * <li><strong>rows</strong> - Used by grid only. Represents number of rows in
 * grid or table.</li>
 * <li><strong>cols</strong> - Used by grid only. Represents number of columns
 * in grid or table.</li>
 * <li><strong>behavior, behaviour</strong> - Used by Border Layout only.  Specifies the center behaviour.  Accepts values "scale", "absolute", and "totalBelow".</li>
 * <li><strong>noGrow</strong> - Used by <code>&lt;x&gt;</code> only.  Specifies that BoxLayout should be X_AXIS_NO_GROW.  Accepts values "true" and "false".</li>
 * <li><strong>bottomLast</strong> - Used by <code>&lt;y&gt;</code> only.  Specifies that BoxLayout should use Y_AXIS_BOTTOM_LAST option.  Accepts values "true" and "false"</li>
 * </ul>
 *
 * <h3>Example XML Notation</h3>
 * <pre>{@code
 * Form f = new Form("Test", new BorderLayout());
 * String tpl = "<border>"
 *     + "<border constraint='center'><border constraint='south'><$button constraint='east'/></border></border>"
 *     + "<$search constraint='south'/>"
 *     + "</border>";
 *
 * f.setFormBottomPaddingEditingMode(true);
 * TextField searchField = new TextField();
 * searchField.addActionListener(e->{
 *    Log.p("Search field action performed");
 * });
 * Button submit = new Button("Submit");
 * submit.addActionListener(e->{
 *     Log.p("Button action performed");
 * });
 *
 * UIFragment template = UIFragment.parseXML(tpl)
 *     .set("button", submit)
 *     .set("search", searchField);
 * f.add(BorderLayout.CENTER, template.getView());
 * f.show();
 * }</pre>
 *
 * <h3>JSON Notation</h3>
 * 
 * <p>When trying to express a UI structure inside a Java String, the XML notation may be 
 * a little bit verbose and cumbersome.  For this reason, there is an alternative JSON-based
 * notation that will allow you to express a UI structure more succinctly.</p>
 * 
 * <p>A JSON object (i.e. curly braces) denotes a Container.  If this object includes properties
 * corresponding to the constraints of {@link BorderLayout} (e.g. center, east, west, north, south, or overlay), then
 * the container will use a {@link BorderLayout}, and the associated properties will represent its children
 * with the appropriate constraint.</p>
 * 
 * <p>E.g.:</p>
 * <pre>{@code {center:'Center Label', south:'South Content'}}</pre>
 * <p>Will create a Container with labels in its {@link BorderLayout#CENTER} and {@link BorderLayout#SOUTH} positions.</p>
 * <p>To make things even more succinct, it supports single-character property keys for the BorderLayout constraint values.  E.g. The following 
 * is equivalent to the previous example:</p>
 * <pre>{@code {c:'Center Label', s:'South Content'}}
 * 
 * <p><strong>Other Layouts</strong>:</p>
 * <ul>
 *    <li><strong>Flow Layout</strong> - {@code {flow:[...]}}</li>
 *    <li><strong>Grid Layout</strong> - {@code {grid:[...], cols:3, rows:2}}</li>
 *    <li><strong>Box Layout X</strong> - {@code {x:[...]}}</li>
 *    <li><strong>Box Layout Y</strong> - {@code {y:[...]}}</li>
 *    <li><strong>Layered Layout</strong> - {@code {layered:[...]}}</li>
 *    <li><strong>Table Layout</strong> - {@code {table:[['A1', 'B1', 'C1'], ['A2', 'B2', 'C2'], ...]}}</li>
 * </ul>
 * 
 * <p><strong>Layout Variants</strong></p>
 * 
 * <p>BoxLayout and BorderLayout include variant shorthands to customize their behaviour.</p>
 * 
 * <ul>
 *  <li><strong>xNoGrow, xng</strong> - Same as {@code {x:[...], noGrow:true}}</li>
 *  <li><strong>yBottomLast, ybl</strong> - Same as {@code {y:[...], bottomLast:true}}</li>
 *  <li><strong>centerAbsolute, centerAbs, ca</strong> - Same as {@code {center:[...], behavior:absolute}}</li>
 *  <li><strong>centerTotalBelow, ctb</strong> - Same as {@code {center:[...], behavior:totalBelow}}</li>
 * </ul>
 * 
 * <p><strong>Embedding Placeholders/Parameters</strong></p>
 * <p>The notation for embedding placeholder components (which must be injected via {@link UIFragment#set(java.lang.String, com.codename1.ui.Component) }), 
 * is similar to the XML equivalent.  Just place the parmeter name, prefixed with '$'.  E.g. </p>
 * <pre>{@code $submitButton}</pre>
 * 
 * 
 * <h3>Example JSON Notation</h3>
 * 
 * 
 * <pre>{@code
 * Component view = UIFragment.parseJSON("{n:['Hello', 'World', $checkbox], c:[y, {class:'MyTable', table:[['Username', $username], ['Password', $password]]}, {flow:['Some text'], align:center}], s:$submit}")
 *     .set("username", new TextField())
 *     .set("password", new TextField())
 *     .set("submit", new Button("Submit"))
 *     .set("checkbox", new CheckBox("Check Me"))
 *     .getView();
 * }</pre>
 *
 * @author shannah
 * @since 7.0
 */
public class UIFragment {

    
    // The root element of this template
    private Element root;
    
    // The root view of the template
    private Container view;
    
    // The factory that creates components from XML elements
    private ComponentFactory factory = new DefaultComponentFactory();
    
    // Index that stores any component which included an "id" attribute
    private Map<String,Component> index = new HashMap<String,Component>();
    
    // Parameters for the template.  Any tag with tag name startign with $ will
    // be replaced by a corresponding parameter
    private Map<String,Component> parameters = new HashMap<String,Component>();
    
    /**
     * A factory for converting XML elements to Components.
     * @see #setFactory(com.codename1.ui.UIFragment.ComponentFactory) 
     * @see #getFactory() 
     */
    public static interface ComponentFactory  {
        /**
         * Creates a new component given its XML description.
         * @param el The XML element
         * @return A Component.
         */
        public Component newComponent(Element el);
        
        /**
         * Creates a layout constraint for adding a child component to a parent component.
         * @param parent The parent component.
         * @param parentEl The XML element for the parent component.
         * @param child The child component.
         * @param childEl The XML element for the child component.
         * @return Layout constraint for adding to the parent component.
         */
        public Object newConstraint(Container parent, Element parentEl, Component child, Element childEl);
    }
    
    /**
     * Default component factory that is used in templates.  Supports the following tags:
     * <ul>
     * <li>y</li>
     * <li>x</li>
     * <li>flow</li>
     * <li>layered</li>
     * <li>border</li>
     * <li>table</li>
     * <li>label</li>
     * <li>button</li>
     * </ul>
     */
    public static class DefaultComponentFactory implements ComponentFactory {

        private static int centerBehaviour(String behaviour) {
            if ("scale".equalsIgnoreCase(behaviour)) {
                return BorderLayout.CENTER_BEHAVIOR_SCALE;
            }
            if ("absolute".equalsIgnoreCase(behaviour)) {
                return BorderLayout.CENTER_BEHAVIOR_CENTER_ABSOLUTE;
            }
            if ("center".equalsIgnoreCase(behaviour)) {
                return BorderLayout.CENTER_BEHAVIOR_CENTER;
            }
            if ("totalBelow".equalsIgnoreCase(behaviour)) {
                return BorderLayout.CENTER_BEHAVIOR_TOTAL_BELOW;
            }
            return BorderLayout.CENTER_BEHAVIOR_SCALE;
        }
        
        private static boolean grow(String grow) {
            if ("true".equalsIgnoreCase(grow)) {
                return true;
            } else {
                return false;
            }
        }
        
        private static int align(String align) {
            if ("left".equals(align)) {
                return Component.LEFT;
            }
            if ("right".equals(align)){
                return Component.RIGHT;
            }
            if ("center".equals(align)) {
                return Component.CENTER;
            }
            return 0;
        }
        
        private static int valign(String valign) {
            if ("top".equals(valign)) {
                return Component.TOP;
            }
            if ("center".equals(valign)) {
                return Component.CENTER;
            }
            if ("bottom".equals(valign)) {
                return Component.BOTTOM;
            }
            return 0;
        }
        
        private static boolean empty(Element el, String attName) {
            String val = el.getAttribute(attName);
            return val == null || val.length() == 0;
        }
        
        @Override
        public Component newComponent(Element el) {
            String name = el.getTagName().toLowerCase();
            if (name.startsWith("border")) {
                BorderLayout l = new BorderLayout();
                if (name.startsWith("borderabs")) {
                    l.setCenterBehavior(BorderLayout.CENTER_BEHAVIOR_CENTER_ABSOLUTE);
                } else if (name.startsWith("borderscale")) {
                    l.setCenterBehavior(BorderLayout.CENTER_BEHAVIOR_SCALE);
                } else if (name.startsWith("bordertotalbelow")) {
                    l.setCenterBehavior(BorderLayout.CENTER_BEHAVIOR_TOTAL_BELOW);
                } else {
                    if (!empty(el, "behaviour")) {
                        l.setCenterBehavior(centerBehaviour(el.getAttribute("behaviour")));
                    } else if (!empty(el, "behavior")) {
                        l.setCenterBehavior(centerBehaviour(el.getAttribute("behavior")));
                    }
                }
                System.out.println("Border behaviour is "+l.getCenterBehavior());
                return new Container(l);
            }
            if (name.startsWith("y")) {
                BoxLayout l;
                if (name.startsWith("ybottom") || name.equals("ybl")) {
                    l = new BoxLayout(BoxLayout.Y_AXIS_BOTTOM_LAST);
                } else {
                    if ("true".equalsIgnoreCase(el.getAttribute("bottomLast"))) {
                        l = new BoxLayout(BoxLayout.Y_AXIS_BOTTOM_LAST);
                    } else {
                        l = new BoxLayout(BoxLayout.Y_AXIS);
                    }
                }
                return new Container(l);
            }
            if (name.startsWith("x")) {
                BoxLayout l;
                if (name.startsWith("xnogrow") || name.equals("xng")) {
                    l = new BoxLayout(BoxLayout.X_AXIS_NO_GROW);
                } else {
                    if ("true".equalsIgnoreCase(el.getAttribute("noGrow")) || "false".equalsIgnoreCase(el.getAttribute("grow"))) {
                        l = new BoxLayout(BoxLayout.X_AXIS_NO_GROW);
                    } else {
                        l = new BoxLayout(BoxLayout.X_AXIS);
                    }
                }
                return new Container(l);
            }
            if (name.equals("flow")) {
                FlowLayout fl = new FlowLayout();
                String align = el.getAttribute("align");
                String valign = el.getAttribute("valign");
                if (align != null && align.length() > 0) {
                    fl.setAlign(align(align));
                }
                if (valign != null && valign.length()> 0) {
                    fl.setValign(valign(valign));
                }
                return new Container(fl);
            }
            if (name.equals("grid")) {
                String colsStr = el.getAttribute("cols");
                int cols;
                try {
                    cols = Integer.parseInt(colsStr);
                } catch (Throwable t) {

                    throw new RuntimeException("grid requires cols attribute.");
                }
                String rowsStr = el.getAttribute("rows");
                int rows=-1;
                try {
                    rows = Integer.parseInt(rowsStr);
                } catch (Throwable t){
                    rows = (int)Math.ceil(el.getNumChildren()/(double)cols);
                    if (rows == 0) {
                        rows = 1;
                    }
                }
                return new Container(new GridLayout(rows, cols));
            }
            if (name.equals("table")) {
                String colsStr = el.getAttribute("cols");
                String rowsStr = el.getAttribute("rows");
                int rows = -1;
                int cols = -1;
                if (colsStr != null && colsStr.length() > 0) {
                    cols = Integer.parseInt(colsStr);
                }
                if (rowsStr != null && rowsStr.length() > 0) {
                    rows = Integer.parseInt(rowsStr);
                }
                Vector children = el.getChildrenByTagName("tr");
                if (rows < 0) {
                    rows = children.size();
                }
                if (cols < 0) {
                    if (children.size() > 0) {
                        Element firstRow = (Element)children.get(0);
                        Vector firstRowCols = firstRow.getChildrenByTagName("td");
                        cols = firstRowCols.size();
                    }
                }
                
                rows = Math.max(1, rows);
                cols = Math.max(1, cols);
                TableLayout tl = new TableLayout(rows, cols);
                return new Container(tl);
                
                
            }
            if (name.equals("layered")) {
                return new Container(new LayeredLayout());
            }
            if (name.equals("label")) {
                Element textEl = el.getNumChildren() > 0 ? el.getChildAt(0) : null;
                String text = textEl != null ? textEl.getText() : "";
                return new Label(text);
            }
            if (name.equals("button")) {
                Element textEl = el.getNumChildren() > 0 ? el.getChildAt(0) : null;
                String text = textEl != null ? textEl.getText() : "";
                return new Button(text);
            }
            throw new IllegalArgumentException("Unsupported element "+name);
        }

        @Override
        public Object newConstraint(Container parent, Element parentEl, Component child, Element childEl) {
            Layout l = parent.getLayout();
            if (l instanceof BorderLayout) {
                String cnst = childEl.getAttribute("constraint");
                if (cnst == null) {
                    return BorderLayout.CENTER;
                }
                cnst = cnst.toLowerCase();
                if ("north".equals(cnst)) {
                    return BorderLayout.NORTH;
                }
                if ("south".equals(cnst)) {
                    return BorderLayout.SOUTH;
                }
                if ("east".equals(cnst)) {
                    return BorderLayout.EAST;
                }
                if ("west".equals(cnst)) {
                    return BorderLayout.WEST;
                }
                if ("center".equals(cnst)) {
                    return BorderLayout.CENTER;
                }
                if ("overlay".equals(cnst)) {
                    return BorderLayout.OVERLAY;
                }
                throw new IllegalArgumentException("Unsupported constraint "+cnst);
                
            }
            return null;
        }
        
    }

    /**
     * Parses input stream of XML into a Template
     * @param input InputStream with XML content to parse
     * @return The corresponding template, or a RuntimeException if parsing failed.
     */
    public static UIFragment parseXML(InputStream input) {
        try {
            XMLParser p = new XMLParser();
            Element el = p.parse(new InputStreamReader(input));
            return new UIFragment(el);
        } catch (Exception ex) {
            Log.e(ex);
            throw new RuntimeException(ex.getMessage());
        }
    }
    
    /**
     * Parses XML string into a Template
     * @param xml XML string describing a UI.
     * @return The corresponding template, or a RuntimeException if parsing failed.
     */
    public static UIFragment parseXML(String xml) {
        try {
            XMLParser p = new XMLParser();
            Element el = p.parse(new CharArrayReader(xml.toCharArray()));
            return new UIFragment(el);
        } catch (Exception ex) {
            Log.e(ex);
            throw new RuntimeException(ex.getMessage());
        }
    }
    
    /**
     * Parses a JSON string into a template.
     * @param json A JSON string representing a UI hierarchy.
     * @return
     * @throws IOException 
     */
    public static UIFragment parseJSON(String json) {
        try {
            Element el = UINotationParser.parseJSONNotation(json);
            return new UIFragment(el);
        } catch (Exception ex) {
            Log.e(ex);
            throw new RuntimeException(ex.getMessage());
        }
    }
    
    private UIFragment(Element el) {
        this.root = el;
    }
    
    /**
     * Gets the view that is generated by this template.
     * @return 
     */
    public Container getView() {
        if (view == null) {
            view = (Container)getFactory().newComponent(root);
            decorate(root, view);
            build(root, view);
        }
        return view;
    }
    
    private void decorate(Element el, Component cmp) {
        String classAttr = el.getAttribute("class");
        if (classAttr != null && classAttr.length() > 0) {
            String[] tags = Util.split(classAttr, " ");
            $(cmp).addTags(tags);
        }
        String uiid = el.getAttribute("uiid");
        if (uiid != null && uiid.length() > 0){
            cmp.setUIID(uiid);
        }
        String id = el.getAttribute("id");
        if (id != null && id.length() > 0) {
            index.put(id, cmp);
        }
        
        String name = el.getAttribute("name");
        if (name != null && name.length() > 0) {
            cmp.setName(name);
        }
    }
    
    private List<Element> getChildren(Element el) {
        String tagName = el.getTagName();
        if ("table".equals(tagName)) {
            List<Element> out = new ArrayList<Element>();
            for (Object row : el.getChildrenByTagName("tr")) {
                Element erow = (Element)row;
                for (Object cell : erow.getChildrenByTagName("td")) {
                    Element ecell = (Element)cell;
                    if (ecell.getNumChildren() > 0) {
                        out.add(ecell.getChildAt(0));
                    }
                }
                
            }
            return out;
        } else {
            List<Element> out = new ArrayList<Element>();
            int len = el.getNumChildren();
            for (int i=0; i<len; i++) {
                out.add(el.getChildAt(i));
            }
            return out;
        }
    }
    
    private void build(Element model, Container view) {
        List<Element> children = getChildren(model);
        int len = children.size();
        for (int i=0; i<len; i++) {
            Element child = children.get(i);
            if (child.isTextElement()) {
                continue;
            }
            String tagName = child.getTagName();
            
            Component cmp;
            if (tagName.startsWith("$")) {
                String pname = tagName.substring(1);
                if (!parameters.containsKey(pname)) {
                    throw new IllegalArgumentException("Missing parameter "+pname);
                }
                cmp = parameters.get(pname);
            } else {
                cmp = getFactory().newComponent(child);
                decorate(child, cmp);
            }
            
            Object constraint = getFactory().newConstraint(view, model, cmp, child);
            view.add(constraint, cmp);
            if (cmp instanceof Container) {
                build(child, (Container)cmp);
            }
            
        }
    }
    
    private void reset() {
        view = null;
        index.clear();
        
    }
    
    /**
     * Sets a parameter component in this template.  Templates that include "parameter"
     * tags will inject these parameters into the resulting view.
     * @param paramName The name of the parameter.
     * @param param The component to inject into the template.
     * @return Self for chaining.
     */
    public UIFragment set(String paramName, Component param) {
        if (view != null) {
            reset();
        }
        parameters.put(paramName, param);
        if (param != null && param.getName() == null) {
            param.setName(paramName);
        }
        return this;
    }
    
    /**
     * Gets a component in the template by its ID.
     * @param id The ID of the component.
     * @return The component with matching ID.
     */
    public Component findById(String id) {
        getView();
        return index.get(id);
    }
    
    private static class UINotationParser { 

        private static Map parseJSON(String json) throws IOException {
            JSONParser parser = new JSONParser();
            parser.setStrict(false);
            return parser.parseJSON(new CharArrayReader(json.toCharArray()));

        }

        private static Element parseJSONNotation(String json) throws IOException {
            Map m = parseJSON(json);
            return buildXMLFromJSONNotation(m);

        }

        private static boolean isBorderLayout(Map m) {
            for (String key : BORDER_LAYOUT_KEYS) {
                if (m.containsKey(key)) {
                    return true;
                }
            }
            return false;
        }
        private static final String[] ATTRIBUTES = new String[]{"uiid", "id", "class", "cols", "rows", "align", "valign", "name", "grow", "noGrow", "behaviour", "behavior", "bottomLast"};
        private static final String[] BORDER_LAYOUT_CONSTRAINTS = new String[]{"north", "south", "east", "west", "overlay", "center"};
        private static final String[] BORDER_LAYOUT_SINGLE_CHAR_ALIASES = new String[]{"n", "s", "e", "w", "c", "o"};
        private static final String[] BORDER_LAYOUT_KEYS = new String[]{"north", "south", "east", "west", "overlay", "center", "n", "s", "e", "w", "c", "o", "centerAbsolute", "centerScale", "centerAbs", "centerTotalBelow", "ca", "cs", "ctb"};
        private static final String[] BORDER_LAYOUT_CENTER_ALIASES = new String[]{"c", "centerAbsolute", "centerScale", "centerAbs", "centerTotalBelow", "ca", "cs", "ctb"};
        private static void setBorderLayoutBehaviour(Map m) {
            if (m.containsKey("behavior")) {
                return;
            }
            if (m.containsKey("centerAbsolute") || m.containsKey("ca") || m.containsKey("centerAbs")) {
                m.put("behavior", "absolute");
                return;
            }
            if (m.containsKey("centerScale") || m.containsKey("cs")) {
                m.put("behavior", "scale");
                return;
            }
            if (m.containsKey("centerTotalBelow") || m.containsKey("ctb")) {
                m.put("behavior", "totalBelow");
                return;
            }
            
            
        }

        private static boolean isBoxLayoutX(Map m) {
            return m.containsKey("x") || m.containsKey("xng") || m.containsKey("xNoGrow");
        }
        
        private static void setBoxLayoutXNoGrow(Map m) {
            if (m.containsKey("xNoGrow")) {
                m.put("x", m.get("xNoGrow"));
                m.put("noGrow", "true");
                return;
            }
            if (m.containsKey("xng")) {
                m.put("x", m.get("xng"));
                m.put("noGrow", "true");
                return;
            } 
        }
        
        private static void setBoxLayoutYBottomLast(Map m) {
            if (m.containsKey("yBottom")) {
                m.put("y", m.get("yBottom"));
                m.put("bottomLast", "true");
                return;
            }
            if (m.containsKey("yBottomLast")) {
                m.put("y", m.get("yBottomLast"));
                m.put("bottomLast", "true");
                return;
            }
            if (m.containsKey("ybl")) {
                m.put("y", m.get("ybl"));
                m.put("bottomLast", "true");
                return;
            }
        }

        private static boolean isBoxLayoutY(Map m) {
            return m.containsKey("y") || m.containsKey("yBottom") || m.containsKey("yBottomLast") || m.containsKey("ybl");
        }

        private static boolean isGridLayout(Map m) {
            return m.containsKey("grid");
        }

        private static boolean isFlowLayout(Map m) {
            return m.containsKey("flow");
        }

        private static boolean isLayered(Map m) {
            return m.containsKey("layered");
        }
        
        private static boolean isTableLayout(Map m) {
            return m.containsKey("table");
        }


        private static Element buildXMLFromJSONNotation(Object o) throws IOException {
            Element el = null;
            //System.out.println("Building "+o);
            if (o instanceof Map) {
                Map m = (Map)o;
                if (m.get("root") != null) {
                    // The root is an array.
                    return buildXMLFromJSONNotation(m.get("root"));
                } else {
                    Object children = null;
                    String key = null;
                    if (isBorderLayout(m)) {
                        setBorderLayoutBehaviour(m);
                        el = new Element("border");
                    } else if (isBoxLayoutX(m)) {
                        setBoxLayoutXNoGrow(m);
                        el = new Element("x");
                        children = m.get("x");
                    } else if (isBoxLayoutY(m)) {
                        setBoxLayoutYBottomLast(m);
                        el = new Element("y");
                        children = m.get("y");
                    } else if (isGridLayout(m)) {
                        el = new Element("grid");
                        children = m.get("grid");
                    } else if (isLayered(m)) {
                        el = new Element("layered");
                        children = m.get("layered");
                    } else if (isFlowLayout(m)) {
                        el = new Element("flow");
                        children = m.get("flow");
                    } else if (isTableLayout(m)) {
                        el = new Element("table");
                        children = m.get("table");
                    }
                    if (children instanceof List) {
                        if (isTableLayout(m)) {
                            for (Object child : (List)children) {
                                if (child instanceof List) {
                                    Element row = new Element("tr");
                                    for (Object cell : (List)child) {
                                        Element td = new Element("td");
                                        td.addChild(buildXMLFromJSONNotation(cell));
                                        row.addChild(td);
                                    }
                                    el.addChild(row);
                                } else {
                                    throw new RuntimeException("Tables require 2D array representing rows and columns");
                                }
                            }
                        } else {
                            for (Object child : (List)children) {
                                el.addChild(buildXMLFromJSONNotation(child));
                            }
                        }
                    } else if (isBorderLayout(m)){
                        for (String constraint : BORDER_LAYOUT_SINGLE_CHAR_ALIASES) {
                            if (m.containsKey(constraint)) {
                                char c = constraint.charAt(0);
                                switch (c) {
                                    case 'n':
                                        m.put("north", m.get("n"));
                                        break;
                                    case 's':
                                        m.put("south", m.get("s"));
                                        break;
                                    case 'e':
                                        m.put("east", m.get("e"));
                                        break;
                                    case 'w':
                                        m.put("west", m.get("w"));
                                        break;
                                    case 'c':
                                        m.put("center", m.get("c"));
                                        break;
                                    case 'o':
                                        m.put("overlay", m.get("o"));
                                        

                                }
                            }
                        }
                        if (!m.containsKey("center")) {
                            for (String constraint : BORDER_LAYOUT_CENTER_ALIASES) {
                                if (m.containsKey(constraint)) {
                                    m.put("center", m.get(constraint));
                                    break;
                                }
                            }
                        }
                        for (String constraint : BORDER_LAYOUT_CONSTRAINTS) {
                            if (m.containsKey(constraint)) {
                                Element north = buildXMLFromJSONNotation(m.get(constraint));
                                north.setAttribute("constraint", constraint);
                                el.addChild(north);
                            }
                        }

                    } else {
                        el.addChild(buildXMLFromJSONNotation(children));
                    }
                    for (String k : ATTRIBUTES) {
                        if (m.containsKey(k)) {
                            Object val = m.get(k);
                            if (val instanceof Double && ("cols".equals(k) || "rows".equals(k))) {
                                val = ((Double)val).intValue();
                            }
                            el.setAttribute(k, String.valueOf(val));
                        }
                    }
                    
                    return el;
                }
            } else if (o instanceof List) {
                List l = (List)o;
                int len = l.size();
                if (len > 0) {
                    Object first = l.get(0);
                    if (first instanceof String) {
                        String s = (String)first;
                        if ("x".equals(first)) {
                            el = new Element("x");
                        } else if ("y".equals(first)) {
                            el = new Element("y");
                        } else if ("grid".equals(first)) {
                            el = new Element("grid");
                        } else if ("layered".equals(first)) {
                            el = new Element("layered");
                        } else {
                            
                            el = new Element("flow");
                            
                        }
                        if (!inArray(s, new String[]{"x", "y", "grid", "flow", "layered"})) {
                            el.addChild(buildXMLFromJSONNotation(first));
                        }
                    } else {
                        el.addChild(buildXMLFromJSONNotation(first));
                    }
                    for (int i=1; i<len; i++) {
                        Element child = buildXMLFromJSONNotation(l.get(i));
                        el.addChild(child);
                    }
                } else {
                    el = new Element("flow");
                }
                return el;
            } else if (o instanceof String) {
                String str = (String)o;
                if (str.startsWith("$")) {
                    el = new Element(str);
                    return el;
                } else {
                    el = new Element("label");
                    Element textEl = new Element(null, true);
                    textEl.setText(str);
                    el.addChild(textEl);
                    return el;
                }
            } else {
                throw new IOException("Unexpected token type in UINotation: "+o);
            }
        }
    }
    private static boolean inArray(Object needle, Object[] haystack) {
        int len = haystack.length;
        for (int i=0; i<len; i++) {
            if (needle.equals(haystack[i])) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Gets the component factory that is currently set for this fragment.
     * @return the factory
     */
    public ComponentFactory getFactory() {
        return factory;
    }

    /**
     * Sets the component factory to be used.
     * @param factory the factory to set
     * @return Self for chaining
     */
    public UIFragment setFactory(ComponentFactory factory) {
        this.factory = factory;
        return this;
    }
}
