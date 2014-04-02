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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.List;

import com.codename1.io.JSONParser;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

/**
 * Internal class, do not use.
 *
 * A DOM accessor implementation for working with Map data.
 *
 * @author Eric Coolman
 *
 */
class MapContent implements StructuredContent {

    private Object root;
    private StructuredContent parent;

    /**
     * Construct from parsed Map content.
     *
     * @param content parsed Map content
     */
    public MapContent(Map content) {
        this.root = content;
    }

    /**
     * Construct from a JSON string.
     *
     * @param content a JSON string.
     * @throws IOException on error reading/parsing the string
     */
    public MapContent(String content) throws IOException {
        this(new InputStreamReader(new ByteArrayInputStream(content.getBytes("UTF-8")), "UTF-8"));
    }

    /**
     * Construct from a JSON input stream.
     *
     * @param content a JSON input stream.
     * @throws IOException on error reading/parsing the stream
     */
    public MapContent(InputStream content) throws IOException {
        this(new JSONParser().parse(new InputStreamReader(content)));
    }

    /**
     * Construct from a JSON input stream.
     *
     * @param content a JSON reader.
     * @throws IOException on error reading/parsing the stream
     */
    public MapContent(Reader content) throws IOException {
        this(new JSONParser().parse(content));
    }

    /**
     * INTERNAL - link a node to it's parent so we can traverse backwards when
     * required.
     *
     * @param content a Map, List, or String node.
     * @param parent the parent element of content.
     */
    MapContent(Object content, StructuredContent parent) {
        this.root = content;
        this.parent = parent;
    }

    /**
     * Convert the object back to an JSON string.
     *
     * @return the object as a string
     */
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

    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return root.hashCode();
    }

    /**
     * @see java.lang.Object#equals(Object)
     */
    public boolean equals(Object o) {
        return root.hashCode() == o.hashCode();
    }

    /**
     * Copy an array of Map elements to an array of StructuredContent nodes,
     * also linking the parent.
     *
     * @param array
     * @return
     */
    private List _asStructuredContentArray(List array) {
        if (array == null) {
            return null;
        }
        List children;
        if (array instanceof java.util.Vector) {
            children = new java.util.Vector();
        } else {
            children = new ArrayList();
        }

        for (Iterator elements = array.iterator(); elements.hasNext();) {
            Object o = elements.next();
			// There is a bug that needs to be addressed, should always have
            // Maps.
            // for now prevent the critical cast exception.
            if (o instanceof Map) {
                children.add(new MapContent((Map) o, this));
            } else if (o instanceof String) {
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
    public List getChildren(String name) {
        if (root instanceof String) {
            return new Vector();
        }
        // on arrays, auto select first element that contains 'name'.
        Object node = root;
        boolean oldList = node == null || (node instanceof Vector);
        if (node instanceof List) {
            Object tmp = null;
            for (Iterator e = ((List) node).iterator(); e.hasNext();) {
                tmp = e.next();
                if ((tmp instanceof Map)) {
                    if (((Map) tmp).containsKey(name)) {
                        break;
                    }
                }
                tmp = null;
            }
            if (tmp == null) {
                if (oldList) {
                    return new java.util.Vector();
                } else {
                    return new ArrayList();
                }
            }
            node = tmp;
        }
        node = ((Map) node).get(name);
        if (node == null) {
            if(oldList) {
                return new Vector();
            }
            return new ArrayList();
        } else if (node instanceof List) {
            return _asStructuredContentArray((List) node);
        } else if (node instanceof Map) {
            List array;
            if(node instanceof Hashtable) {
                array = new Vector();
            } else {
                array = new ArrayList();
            }
            array.add(new MapContent((Map) node, this));
            return array;
        } else {
            List array;
            if(oldList) {
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
    public StructuredContent getChild(int index) {
        if (root instanceof List) {
            return new MapContent(((List) root).get(index), this);
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
    public List getDescendants(String name) {
        List decendants;
        if(root instanceof Vector || root instanceof Hashtable) {
            decendants = new Vector();
        } else {
            decendants = new ArrayList();
        }
        if (root instanceof List) {
            _findByName(decendants, (List) root, name);
        } else if (root instanceof Map) {
            _findByName(decendants, (Map) root, name);
        }
        return decendants;
    }

    /**
     * Internal method for finding decendant nodes
     *
     * @param target List for collecting results
     * @param source source array to search
     * @param name node name we are searching for
     */
    private void _findByName(List target, List source, String name) {
        for (int i = 0; i < source.size(); i++) {
            Object o = source.get(i);
            if (o instanceof Map) {
                _findByName(target, (Map) o, name);
            } else if (o instanceof List) {
                _findByName(target, (List) o, name);
            }
        }
    }

    /**
     * Internal method for finding decendant nodes
     *
     * @param target List for collecting results
     * @param source source element to search
     * @param name node name we are searching for
     */
    private void _findByName(List target, Map source, String name) {
        if (source.containsKey(name)) {
            Object o = source.get(name);
            if (o instanceof StructuredContent) {
                target.add((StructuredContent) o);
            } else {
                // TODO: there will be a bug here with parent node, won't be able to walk up more than one node
                target.add(new MapContent(o, new MapContent(source)));
            }
        }
        for (Iterator e = source.values().iterator(); e.hasNext();) {
            Object o = e.next();
            if (o instanceof List) {
                _findByName(target, (List) o, name);
            } else if (o instanceof Map) {
                _findByName(target, (Map) o, name);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.codename1.processing.StructuredContent#getAttribute(java.lang.String)
     */
    public String getAttribute(String name) {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.codename1.processing.StructuredContent#getAttributes()
     */
    public Map getAttributes() {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.codename1.processing.StructuredContent#getParent()
     */
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
    public String getText() {
        if (root instanceof String) {
            return (String) root;
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

    public Object getNativeRoot() {
        return root;
    }

}
