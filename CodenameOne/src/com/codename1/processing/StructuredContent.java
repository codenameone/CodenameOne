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
 * Internal interface, do not use.
 * 
 * An interface to abstract access to structured content DOMs. Implementations
 * of this interface work similar to a Node/Element object of a typical DOM
 * parser where it, where it represents a position within a structured document.
 * 
 * @author Eric Coolman
 * 
 */
interface StructuredContent {
	/**
	 * Select all children directly below the current position with a given tag
	 * name. An empty vector is returned if no matches found.
	 * 
	 * @param name tag name to select.
	 * @return an array of elements that match the tag name.
	 */
	public Vector getChildren(String name);

	/**
	 * Select a single direct child node from the current position.
	 * 
	 * @param index the index of the node to select
	 * @return a single element, or null if the index is out of range.
	 */
	public StructuredContent getChild(int index);

	/**
	 * Globally select all children from the current position with a given tag
	 * name. An empty vector is returned if no matches found.
	 * 
	 * @param name tag name to select.
	 * @return an array of elements that match the tag name.
	 */
	public Vector getDescendants(String name);

	/**
	 * Select an attribute from the current node.
	 * 
	 * @param name the name of the attribute to select.
	 * @return the value of the attribute, or null if the attribute is not
	 *         present.
	 */
	public String getAttribute(String name);

        /**
         * Select all attributes from the current node.
         *
         * @return all attributes, or null if no attributes are
         * present.
         */
        public Hashtable getAttributes();
        
	/**
	 * Select the parent of the current node.
	 * 
	 * @param name the name of the attribute to select.
	 * @return the value of the attribute, or null if the attribute is not
	 *         present.
	 */
	public StructuredContent getParent();

	/**
	 * Select the text at the current node. If the current node is not a text
	 * node, the a text representation of the current node is returned, for
	 * example, a JSON fragment could be returned from a JSON document.
	 * 
	 * @return
	 */
	public String getText();

	/**
	 * Get the native structured document object. For example, an XML document
	 * would return an Element object, and a JSON document would return a
	 * Hashtable.
	 * 
	 * @return native structured document object.
	 */
	public Object getNativeRoot();
}
