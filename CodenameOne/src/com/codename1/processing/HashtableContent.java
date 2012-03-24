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
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import com.codename1.io.JSONParser;

/**
 * Internal class, do not use.
 * 
 * A DOM accessor implementation for working with hashtable data.
 * 
 * @author Eric Coolman
 *
 */
class HashtableContent implements StructuredContent {
	private Object root;
	private StructuredContent parent;
	
	/**
	 * Construct from parsed hashtable content.
	 * 
	 * @param content parsed hashtable content
	 */
	public HashtableContent(Hashtable content) {
		//Hashtable h = new Hashtable();
		//h.put("ROOT", content);
		this.root = content;
	}

	/**
	 * Construct from a JSON string.
	 * 
	 * @param content a JSON string.
	 * @throws IOException on error reading/parsing the string
	 */
	public HashtableContent(String content) throws IOException {
		this(new ByteArrayInputStream(content.getBytes()));
	}

	/**
	 * Construct from a JSON input stream.
	 * 
	 * @param content a JSON input stream.
	 * @throws IOException on error reading/parsing the stream
	 */
	public HashtableContent(InputStream content) throws IOException {
		this(new JSONParser().parse(new InputStreamReader(content)));
	}
	
	/**
	 * INTERNAL - link a node to it's parent so we can traverse backwards when required.
	 * 
	 * @param content a Hashtable, Vector, or String node.
	 * @param parent the parent element of content.
	 */
	private HashtableContent(Object content, StructuredContent parent) {
		this.root = content;
		this.parent = parent;
	}

	/**
	 * Convert the object back to an JSON string.
	 * 
	 * @return the object as a string
	 */
	public String toString() {
		if (root instanceof Hashtable) {
			if (((Hashtable)root).containsKey("ROOT")) {
				return PrettyPrinter.print((Hashtable)((Hashtable)root).get("ROOT"));
			} else {
				return PrettyPrinter.print((Hashtable)root);
			}
		} else if (root instanceof Vector) {
			return PrettyPrinter.print((Vector)root);
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
	 * Copy an array of hashtable elements to an array of StructuredContent nodes, also linking the parent.
	 * 
	 * @param array
	 * @return
	 */
	private Vector _asStructuredContentArray(Vector array) {
		if (array == null) {
			return null;
		}
		Vector children = new Vector();
		for (Enumeration elements = array.elements(); elements.hasMoreElements(); ) {
			Object o = elements.nextElement();
			// There is a bug that needs to be addressed, should always have hashtables.
			// for now prevent the critical cast exception.
			if (o instanceof Hashtable) {
				children.addElement(new HashtableContent((Hashtable)o, this));
			}
		}
		return children;
	}

	/* (non-Javadoc)
	 * @see com.codename1.processing.StructuredContent#getChildren(java.lang.String)
	 */
	public Vector getChildren(String name) {
		if (root instanceof String) {
			return new Vector();
		}
		// on arrays, auto select first element
		Object node = root;
		if (node instanceof Vector) {
			node = ((Vector) node).firstElement();
			if (node == null) {
				return new Vector();
			}
			if ((node instanceof Hashtable) == false) {
				System.err.println("Unhandled nested child: " + node.getClass().getName());
				return new Vector();
			}
		}
		node = ((Hashtable)node).get(name);
		if (node == null) {
			return new Vector();
		} else if (node instanceof Vector) {
			return _asStructuredContentArray((Vector)node);
		} else if (node instanceof Hashtable) {
			Vector array = new Vector();
			array.addElement(new HashtableContent((Hashtable)node, this));
			return array;
		} else {
			if ((node instanceof String) == false) {
				System.err.println("Warning - handled child type as string: " + node.getClass().getName());
			}
			Vector array = new Vector();
			array.addElement(node.toString());
			return array;
		}
	}

	/* (non-Javadoc)
	 * @see com.codename1.processing.StructuredContent#getChild(int)
	 */
	public StructuredContent getChild(int index) {
		if (root instanceof Vector) {
			return new HashtableContent(((Vector)root).elementAt(index), this);
		}
		Hashtable h = (Hashtable)root;
		if (index < 0 || index >= h.size()) {
			return null;
		}
		Enumeration elements = h.elements();
		for (int i = 0; i < index; i++) {
			elements.nextElement();
		}
		Object node = elements.nextElement();
		return new HashtableContent(node, this);
	}


	/* (non-Javadoc)
	 * @see com.codename1.processing.StructuredContent#getDescendants(java.lang.String)
	 */
	public Vector getDescendants(String name) {
		return getChildren(name);
	}

	/* (non-Javadoc)
	 * @see com.codename1.processing.StructuredContent#getAttribute(java.lang.String)
	 */
	public String getAttribute(String name) {
		if (root instanceof Hashtable) { 
			((Hashtable)root).get(name);
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see com.codename1.processing.StructuredContent#getParent()
	 */
	public StructuredContent getParent() {
		if (parent == null) {
			return null;
		}
		return parent;
	}

	/* (non-Javadoc)
	 * @see com.codename1.processing.StructuredContent#getText()
	 */
	public String getText() {
		Object obj = getChild(0);
		if (obj == null) {
			return null;
		}
		if (obj instanceof String) {
			return (String)obj;
		}
		return obj.toString();
	}

}
