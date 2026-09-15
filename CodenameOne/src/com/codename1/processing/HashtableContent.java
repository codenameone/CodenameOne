/*
 * Copyright (c) 2012, Eric Coolman, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.processing;

import com.codename1.io.JSONParser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/// Internal class do not use.
///
/// A DOM accessor implementation for working with Map data.
///
/// @author Eric Coolman
class MapContent implements StructuredContent {

    private final Object root;
    private StructuredContent parent;

    /// Construct from parsed Map content.
    ///
    /// #### Parameters
    ///
    /// - `content`: parsed Map content
    public MapContent(Map<?, ?> content) {
        this.root = content;
    }

    /// Construct from a JSON string.
    ///
    /// #### Parameters
    ///
    /// - `content`: a JSON string.
    ///
    /// #### Throws
    ///
    /// - `IOException`: on error reading/parsing the string
    public MapContent(String content) throws IOException {
        this(com.codename1.io.Util.getReader(new ByteArrayInputStream(com.codename1.util.StringUtil.getBytes(content))));
    }

    /// Construct from a JSON input stream.
    ///
    /// #### Parameters
    ///
    /// - `content`: a JSON input stream.
    ///
    /// #### Throws
    ///
    /// - `IOException`: on error reading/parsing the stream
    public MapContent(InputStream content) throws IOException {
        this(new JSONParser().parse(com.codename1.io.Util.getReader(content)));
    }

    /// Construct from a JSON input stream.
    ///
    /// #### Parameters
    ///
    /// - `content`: a JSON reader.
    ///
    /// #### Throws
    ///
    /// - `IOException`: on error reading/parsing the stream
    public MapContent(Reader content) throws IOException {
        this(new JSONParser().parse(content));
    }

    /// INTERNAL - link a node to it's parent so we can traverse backwards when
    /// required.
    ///
    /// #### Parameters
    ///
    /// - `content`: a Map, List, or String node.
    ///
    /// - `parent`: the parent element of content.
    MapContent(Object content, StructuredContent parent) {
        this.root = content;
        this.parent = parent;
    }

    /// Convert the object back to a JSON string.
    ///
    /// #### Returns
    ///
    /// the object as a string
    @Override
    public String toString() {
        if (root instanceof Map) {
            if (((Map) root).containsKey("ROOT")) {
                return PrettyPrinter.print((Map) ((Map) root).get("ROOT"));
            } else {
                return PrettyPrinter.print((Map) root);
            }
        } else if (root instanceof List) {
            return PrettyPrinter.print((List) root);
        } else {
            return root.toString();
        }
    }

    /// #### See also
    ///
    /// - java.lang.Object#hashCode()
    @Override
    public int hashCode() {
        return root.hashCode();
    }

    /// #### See also
    ///
    /// - java.lang.Object#equals(Object)
    @Override
    public boolean equals(Object o) {
        return o instanceof MapContent &&
                (root == ((MapContent) o).root || //NOPMD CompareObjectsWithEquals
                        (root != null && root.equals(((MapContent) o).root)));
    }

    /// Copy an array of Map elements to an array of StructuredContent nodes,
    /// also linking the parent.
    ///
    /// #### Parameters
    ///
    /// - `array`
    private List asStructuredContentArray(List array) {
        if (array == null) {
            return null;
        }
        List children;
        if (array instanceof Vector) {
            children = new Vector();
        } else {
            children = new ArrayList();
        }

        for (Object o : array) {
            // Maps and strings were the only shapes kept, so an array of
            // numbers or booleans -- "scores":[1,2] -- came back EMPTY, from
            // the child form of a path as well as the attribute form. The
            // JSON parser hands those back as Double, Long and Boolean, and a
            // value is a value whatever its type; MapContent reads it through
            // getText() either way.
            //
            // null is still dropped: there is no value to read from it, and
            // the absence tests are what ask about that.
            if (o != null) {
                children.add(new MapContent(o, this));
            }
        }
        return children;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.codename1.processing.StructuredContent#getChildren(java.lang.String)
     */
    @Override
    public List getChildren(String name) {
        if (root instanceof String) {
            return new Vector();
        }
        // Nor has any other scalar. An array of numbers now keeps its values,
        // so a path can walk into one -- "/items/scores/value" -- and the cast
        // to Map further down threw. ParparVM does not throw for a failed
        // cast, so on a device it would have read the Double as a Map.
        if (!(root instanceof Map) && !(root instanceof List)) {
            return new Vector();
        }
        // on arrays, auto select first element that contains 'name'.
        //
        // Past the guard above, root is a Map or a List and cannot be null, so
        // the two null checks this method used to make on it are gone: one
        // decided oldList and the other guarded the cast below. The cast is
        // safe for the same reason -- a List only gets here through an entry
        // the loop kept, and it keeps one only when it is a Map.
        Object node = root;
        boolean oldList = node instanceof Vector;
        if (node instanceof List) {
            Object tmp = null;
            for (Object entry : (List) node) {
                tmp = entry;
                if ((tmp instanceof Map)) {
                    if (((Map) tmp).containsKey(name)) {
                        break;
                    }
                }
                tmp = null;
            }
            if (tmp == null) {
                if (oldList) {
                    return new Vector();
                } else {
                    return new ArrayList();
                }
            }
            node = tmp;
        }
        node = ((Map) node).get(name);
        if (node == null) {
            if (oldList) {
                return new Vector();
            }
            return new ArrayList();
        } else if (node instanceof List) {
            return asStructuredContentArray((List) node);
        } else if (node instanceof Map) {
            List array;
            if (node instanceof Hashtable) {
                array = new Vector();
            } else {
                array = new ArrayList();
            }
            array.add(new MapContent(node, this));
            return array;
        } else {
            List array;
            if (oldList) {
                array = new Vector();
            } else {
                array = new ArrayList();
            }
            array.add(new MapContent(node.toString(), this));
            return array;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.codename1.processing.StructuredContent#getChild(int)
     */
    @Override
    public StructuredContent getChild(int index) {
        if (root instanceof List) {
            return new MapContent(((List) root).get(index), this);
        }
        if (!(root instanceof Map)) {
            // A scalar has no children. The cast below used to be reached for
            // one and throw -- and ParparVM does not throw for a failed cast,
            // so on a device it would have read a Double as a Map instead.
            return null;
        }
        Map h = (Map) root;
        if (index < 0 || index >= h.size()) {
            return null;
        }
        Iterator elements = h.keySet().iterator();
        for (int i = 0; i < index; i++) {
            elements.next();
        }
        Object node = elements.next();
        return new MapContent(node, this);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.codename1.processing.StructuredContent#getDescendants(java.lang.String )
     */
    @Override
    public List getDescendants(String name) {
        List decendants;
        if (root instanceof Vector || root instanceof Hashtable) {
            decendants = new Vector();
        } else {
            decendants = new ArrayList();
        }
        if (root instanceof List) {
            findByName(decendants, (List) root, name);
        } else if (root instanceof Map) {
            findByName(decendants, (Map) root, name);
        }
        return decendants;
    }

    /// Internal method for finding decendant nodes
    ///
    /// #### Parameters
    ///
    /// - `target`: List for collecting results
    ///
    /// - `source`: source array to search
    ///
    /// - `name`: node name we are searching for
    private void findByName(List target, List source, String name) {
        for (Object o : source) {
            if (o instanceof Map) {
                findByName(target, (Map) o, name);
            } else if (o instanceof List) {
                findByName(target, (List) o, name);
            }
        }
    }

    /// Internal method for finding decendant nodes
    ///
    /// #### Parameters
    ///
    /// - `target`: List for collecting results
    ///
    /// - `source`: source element to search
    ///
    /// - `name`: node name we are searching for
    private void findByName(List target, Map source, String name) {
        if (source.containsKey(name)) {
            Object o = source.get(name);
            if (o instanceof StructuredContent) {
                target.add(o);
            } else {
                // TODO: there will be a bug here with parent node, won't be able to walk up more than one node
                target.add(new MapContent(o, new MapContent(source)));
            }
        }
        for (Object o : source.values()) {
            if (o instanceof List) {
                findByName(target, (List) o, name);
            } else if (o instanceof Map) {
                findByName(target, (Map) o, name);
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.codename1.processing.StructuredContent#getAttribute(java.lang.String)
     */
    @Override
    public String getAttribute(String name) {
        return null;
    }

    /// The values an `@name` expression asks for, whatever the format holds.
    ///
    /// XML keeps one in an attribute. JSON has no attributes at all -- the
    /// method above answers null for every name -- and the field the
    /// expression names is a child there, which is what the developer guide
    /// has always said an attribute expression does on a JSON document.
    ///
    /// A list rather than one value, because a JSON field can be an array:
    /// `{"tags":["first","target"]}` has two, and answering with the first
    /// hid the second from a predicate and dropped it from a path that read
    /// the field. Child evaluation has always walked all of them.
    ///
    /// One place, because everything that reads an attribute has to agree:
    /// the predicates that compare one, the predicate that tests for one, and
    /// the step that reads one at the end of a path. They did not -- only the
    /// existence test looked at the child -- so `/players[@rank]/name` found
    /// both players while `/players[@rank='1']/name` and `/players/@id` found
    /// none.
    ///
    /// - `element`: the element to read
    ///
    /// - `name`: the name, already stripped of its '@'
    ///
    /// Nodes rather than strings, because a JSON field can be an object and
    /// the step that reads one at the end of a path has to hand back the
    /// object. Answering with its text gave `getAsArray("/items/@profile")`
    /// the first key of the map instead of the map, while the child form of
    /// the same path returned the map. A predicate reads the text off each.
    ///
    /// #### Returns
    ///
    /// the value nodes, empty when this element has none
    static List attributeOrFields(StructuredContent element, String name) {
        List values = new ArrayList();
        String attribute = element.getAttribute(name);
        if (attribute != null) {
            values.add(new MapContent(attribute, element));
            return values;
        }
        // XML draws the distinction the expression language draws: "[@rank]"
        // asks about an attribute and "[rank]" about a child element, so the
        // child is only the answer where there are no attributes to be had.
        if (!(element instanceof MapContent)) {
            return values;
        }
        List children = element.getChildren(name);
        if (children == null) {
            return values;
        }
        for (Object child : children) {
            if (child instanceof StructuredContent) {
                values.add(child);
            }
        }
        return values;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.codename1.processing.StructuredContent#getAttributes()
     */
    @Override
    public Map getAttributes() {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.codename1.processing.StructuredContent#getParent()
     */
    @Override
    public StructuredContent getParent() {
        if (parent == null) {
            return null;
        }
        return parent;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.codename1.processing.StructuredContent#getText()
     */
    @Override
    public String getText() {
        if (root instanceof String) {
            return (String) root;
        }
        // A scalar is its own text. The parser hands numbers back as Double
        // or Long and booleans as Boolean, and every one of those is a value
        // a path can read or a predicate can compare.
        if (root != null && !(root instanceof Map) && !(root instanceof List)) {
            return root.toString();
        }
        StructuredContent sc = getChild(0);
        if (sc == null) {
            return null;
        }
        if (sc.getNativeRoot() instanceof String) {
            return (String) sc.getNativeRoot();
        }
        return sc.toString();
    }

    @Override
    public Object getNativeRoot() {
        return root;
    }

}
