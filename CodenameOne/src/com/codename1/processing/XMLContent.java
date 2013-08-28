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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import com.codename1.xml.Element;
import com.codename1.xml.XMLParser;
import java.io.Reader;

/**
 * A DOM accessor implementation for working with XML content.
 * 
 * @author Eric Coolman
 *
 */
class XMLContent implements StructuredContent {
	private Element root;

	/**
	 * Construct from a parsed XML dom
	 * 
	 * @param content a parsed XML dom
	 */
	public XMLContent(Element content) {
		this.root = content;
	}

	/**
	 * Construct from an XML string
	 * 
	 * @param content an XML string
	 */
	public XMLContent(String content) {
		this(new ByteArrayInputStream(content.getBytes()));
	}

	/**
	 * Construct from an XML inputstream
	 * 
	 * @param content XML input stream
	 */
	public XMLContent(InputStream content) {
		this(new XMLParser().parse(new InputStreamReader(content)));
	}
	
	/**
	 * Construct from an XML inputstream
	 * 
	 * @param content XML reader
	 */
	public XMLContent(Reader content) {
		this(new XMLParser().parse(content));
	}
	
	/**
	 * Convert the object back to an xml string.
	 * 
	 * @return the object as a string
	 */
	public String toString() {
		if ("ROOT".equals(root.getTagName())) {
			return root.getChildAt(0).toString();
		}
		return getText();
	}

	/**
	 * Convert from an array of Element objects to an array of StructuredContent objects.
	 * 
	 * @param array
	 * @return
	 */
	private Vector _asStructuredContentArray(Vector array) {
		Vector children = new Vector();
		for (Enumeration elements = array.elements(); elements.hasMoreElements(); ) {
			children.addElement(new XMLContent((Element)elements.nextElement()));
		}
		return children;
	}

	/* (non-Javadoc)
	 * @see com.codename1.processing.StructuredContent#getChildren(java.lang.String)
	 */
	public Vector getChildren(String name) {
		return _asStructuredContentArray(root.getChildrenByTagName(name));
	}
	
	/* (non-Javadoc)
	 * @see com.codename1.processing.StructuredContent#getChild(int)
	 */
	public StructuredContent getChild(int index) {
		return new XMLContent(root.getChildAt(index));
	}
	

	/* (non-Javadoc)
	 * @see com.codename1.processing.StructuredContent#getDescendants(java.lang.String)
	 */
	public Vector getDescendants(String name) {
		return _asStructuredContentArray(root.getDescendantsByTagName(name));
	}

	/* (non-Javadoc)
	 * @see com.codename1.processing.StructuredContent#getAttribute(java.lang.String)
	 */
	public String getAttribute(String name) {
		return root.getAttribute(name);
	}

        /* (non-Javadoc)
         * @see com.codename1.processing.StructuredContent#getAttributes()
         */
        public Hashtable getAttributes() {
            return root.getAttributes();
        }        
        
	/* (non-Javadoc)
	 * @see com.codename1.processing.StructuredContent#getParent()
	 */
	public StructuredContent getParent() {
		Element parent = root.getParent();
		if (parent == null) {
			return null;
		}
		return new XMLContent(parent);
	}
	
	/* (non-Javadoc)
	 * @see com.codename1.processing.StructuredContent#getText()
	 */
	public String getText() {
		if (root.isTextElement()) {
			return root.getText();
                } else if (root.getNumChildren() == 0) {
                        return "";
		} else if (root.getChildAt(0).isTextElement()){
			return root.getChildAt(0).getText();
		} else {
			return root.toString();
		}
	}

	public Object getNativeRoot() {
		return root;
	}

}
