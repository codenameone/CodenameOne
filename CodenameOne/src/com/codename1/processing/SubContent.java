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

import java.util.Hashtable;
import java.util.Vector;

/**
 * Internal class, do not use.
 * 
 * An accessor implementation for working with subset of data.
 * 
 * @author Eric Coolman
 * 
 */
class SubContent implements StructuredContent {
	private Vector<StructuredContent> root;
	private StructuredContent parent;

	/**
	 * Construct from subset of content.
	 * 
	 * @param content subset content
	 */
	public SubContent(Vector<StructuredContent> content) {
		this.root = content;
	}

	/**
	 * INTERNAL - link a node to it's parent so we can traverse backwards when
	 * required.
	 * 
	 * @param content a subset of data.
	 * @param parent the parent element of content.
	 */
	SubContent(Vector<StructuredContent> content, StructuredContent parent) {
		this.root = content;
		this.parent = parent;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.codename1.processing.StructuredContent#getChildren(java.lang.String)
	 */
	public Vector getChildren(String name) {
		Vector v = new Vector();
		for (StructuredContent sc : root) {
			v.addAll(sc.getChildren(name));
		}
		return v;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.codename1.processing.StructuredContent#getChild(int)
	 */
	public StructuredContent getChild(int index) {
		if (root != null && root.size() > 0) {
			return root.elementAt(0).getChild(0);
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.codename1.processing.StructuredContent#getDescendants(java.lang.String
	 * )
	 */
	public Vector getDescendants(String name) {
		Vector v = new Vector();
		for (StructuredContent sc : root) {
			v.addAll(sc.getDescendants(name));
		}
		return v;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.codename1.processing.StructuredContent#getAttribute(java.lang.String)
	 */
	public String getAttribute(String name) {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.codename1.processing.StructuredContent#getAttributes()
	 */
	public Hashtable getAttributes() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.codename1.processing.StructuredContent#getParent()
	 */
	public StructuredContent getParent() {
		return parent;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.codename1.processing.StructuredContent#getText()
	 */
	public String getText() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.codename1.processing.StructuredContent#getNativeRoot()
	 */
	public Object getNativeRoot() {
		if (parent != null) {
			return parent.getNativeRoot();
		}
		return null;
	}
}