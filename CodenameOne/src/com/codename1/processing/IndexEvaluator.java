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
 * Internal class, do not use.
 * 
 * This evaluator handles expressions that involve the index. Example:
 * 
 * <code>
 * 
 * Get the name of the second player
 * 
 *  /tournament/player[2]/name
 *  
 *  Get the name of the last player
 * 
 *  /tournament/player[last()]/name
 *  
 *  Get the name of the second last player
 *  
 *  //player[last()-1]/name
 *  
 *  Get players 4 and greater
 *  
 *  //player[position() > 3]/name
 *  
 *  Get players 0 to 4
 *  
 *  //player[position() < 5]/name
 * </code>
 * 
 * @author Eric Coolman
 * 
 */
class IndexEvaluator extends AbstractEvaluator {
	static String FUNC_LAST = "last()";
	static String FUNC_POSITION = "position()";

	/**
	 * Construct with a full predicate expression.
	 * 
	 * @param expr a full predicate expression
	 */
	public IndexEvaluator(String expr) {
		super(expr);
	}

	/**
	 * Select all elements from the array with an index less than the given value.
	 * 
	 * Example:
	 * 
	 * <code>
	 * [position() < 5]
	 * </code>
	 * 
	 * @param elements array of StructuredContent elements
	 * @param rvalue index value
	 * @return an array of matching elements.
	 */
	private Vector _getByPositionLess(Vector elements, int rvalue) {
		if (rvalue > elements.size()) {
			return elements;
		}
		if (rvalue < 0) {
			return null;
		}
		Vector array = new Vector();
		for (int i = 0; i < rvalue; i++) {
			array.addElement(elements.elementAt(i));
		}
		return array;
	}

	/**
	 * Select all elements from the array with an index greater than the given value.
	 * 
	 * Example:
	 * 
	 * <code>
	 * [position() > 5]
	 * </code>
	 * 
	 * @param elements array of StructuredContent elements
	 * @param rvalue index value
	 * @return an array of matching elements.
	 */
	private Vector _getByPositionGreater(Vector elements, int rvalue) {
		if (rvalue >= elements.size()) {
			return null;
		}
		if (rvalue <= 0) {
			return elements;
		}
		Vector array = new Vector();
		for (int i = rvalue; i < elements.size(); i++) {
			array.addElement(elements.elementAt(i));
		}
		return array;
	}

	/**
	 * Select a single element from an array, relative to the last element.  Ie.
	 * 
	 * <code>
	 * Fifth last element:
	 * 
	 * [last() - 5]
	 * 
	 * Last element:
	 * 
	 * [last()]
	 * 
	 * </code>
	 * 
	 * @param elements array of StructuredContent elements
	 * @param rvalue index valu
	 * @return an array of matching elements.
	 */
	private StructuredContent _getByLast(Vector elements, String expr) throws IllegalArgumentException {
		int index = expr.indexOf("-");
		if (index == -1) {
			throw new IllegalArgumentException("Could not handle expression: " + expr);
		}
		String rvalue = expr.substring(index + 1).trim();
		int dim = (elements.size() - 1) - Integer.parseInt(rvalue);
		if (dim < 0) {
			return null;
		}
		return (StructuredContent) elements.elementAt(dim);
	}

	/* (non-Javadoc)
	 * @see com.codename1.path.impl.AbstractEvaluator#evaluateSingle(java.util.Vector, java.lang.String)
	 */
	protected Object evaluateSingle(Vector elements, String expr) {
		if (isNumeric(expr)) {
			int dim = Integer.parseInt(expr);
			if ((dim < 0) || (dim >= elements.size())) {
				return null;
			}
			return (StructuredContent) elements.elementAt(dim);
		} else if (expr.equals(FUNC_LAST)) {
			return (StructuredContent) elements.lastElement();
		} else if (expr.indexOf(FUNC_LAST) != -1) {
			return _getByLast(elements, expr);
		} else if (expr.equals(FUNC_POSITION)) {
			return elements;
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see com.codename1.path.impl.AbstractEvaluator#evaluateLeftLessRight(java.util.Vector, java.lang.String, java.lang.String)
	 */
	protected Object evaluateLeftLessRight(Vector elements, String lvalue,
			String rvalue) {
		if (FUNC_POSITION.equals(lvalue)) {
			return _getByPositionLess(elements, Integer.parseInt(rvalue));
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see com.codename1.path.impl.AbstractEvaluator#evaluateLeftGreaterRight(java.util.Vector, java.lang.String, java.lang.String)
	 */
	protected Object evaluateLeftGreaterRight(Vector elements, String lvalue,
			String rvalue) {
		if (FUNC_POSITION.equals(lvalue)) {
			return _getByPositionGreater(elements, Integer.parseInt(rvalue));
		}
		return null;
	}

}
