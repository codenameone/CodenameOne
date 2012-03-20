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

import java.util.Vector;

/**
 * Private interface, do not use.
 * 
 * Predicate Evaluator interface, for handling predicate expressions (those within square backets 
 * of the path).  Example:
 * 
 * Example predicates:
 * 
 * <code>
 * Test price attribute:
 * 
 * [@price > 45]
 * 
 * Test Child node:
 * 
 * [town='Exeter']
 * 
 * Test attribute exists
 * 
 * [@price]
 * 
 * Test attribute doesn't exist:
 * 
 * [@price = null]
 * 
 * Select by index:
 * 
 * [3]
 * 
 * Select by position:
 * 
 * [position() < 5]
 * 
 * Select by position:
 * 
 * [last() - 5]
 * </code>
 * 
 * 
 * @author Eric Coolman
 *
 */
interface Evaluator {

	/**
	 * Evaluate a predicate expression against an array of StructuredContent elements.  This method
	 * should return an array of elements that matched the given predicate expression.  If only a
	 * single match is found, a single StructuredContent element may be returned (not required).
	 * 
	 * @param elements an array of StructuredContent elements.
	 * @return Either a single StructuredContent element, or an array of StructuredContent elements.
	 * @throws IllegalArgumentException thrown if there is an error processing the predicate expression.
	 */
	public Object evaluate(Vector elements)
			throws IllegalArgumentException;

	/**
	 * Evaluate a predicate expression against a single StructuredContent elements.  This method
	 * should return an array of elements that matched the given predicate expression.  If only a
	 * single match is found, a single StructuredContent element may be returned (not required).
	 * NOTE: Normally this method would return a single element, but an array could be produced
	 * if the predicate uses globbing.
	 * 
	 * @param elements a single StructuredContent element.
	 * @return Either a single StructuredContent element, or an array of StructuredContent elements.
	 * @throws IllegalArgumentException thrown if there is an error processing the predicate expression.
	 */
	public Object evaluate(StructuredContent element)
			throws IllegalArgumentException;
}