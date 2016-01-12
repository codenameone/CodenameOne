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

/**
 * Internal class, do not use.
 * 
 * This evaluator handles expressions that involve child text. Examples:
 * 
 * <code>
 *  Get last names of all players named 'Andre'
 *  
 *  /tournament/player[name='Andre']/lastname
 * 
 *  Get all lineitem numbers with a price over 35
 *  
 *  //order/lineitem[price>35]/@linenum
 * 
 *  Get all PO numbers of orders that contain a lineitem with a price over 35
 *  
 *  //order/lineitem[price>35]/../order/@ponum
 *  
 * </code>
 * 
 * @author Eric Coolman
 * 
 */
class TextEvaluator extends AbstractEvaluator {
	static final String FUNC_TEXT = "text()";

	/**
	 * Construct with a full predicate expression.
	 * 
	 * @param expr a full predicate expression.
	 */
	public TextEvaluator(String expr) {
		super(expr);
	}

	private String[] _getLeftValue(StructuredContent element, String lvalue) {
		String v[];
		if (FUNC_TEXT.equals(lvalue)) {
			v = new String[]{element.getText()};
		} else {
			// getChild() is a bit of a hack here because the content object
			// calls getParent()
			StructuredContent child = element.getChild(0);
			Result result = Result.fromContent(child);
			v = result.getAsStringArray(lvalue);
		}
		return v;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.codename1.path.impl.AbstractEvaluator#evaluateLeftLessRight(com.codename1
	 * .path.impl.StructuredContent, java.lang.String, java.lang.String)
	 */
	protected Object evaluateLeftLessRight(StructuredContent element,
			String lvalue, String rvalue) {
		String v[] = _getLeftValue(element, lvalue);
		if (v == null) {
			return null;
		}
                int vlen = v.length;
		for (int i = 0; i < vlen; i++) {
			if (isNumeric(rvalue) && isNumeric(v[i])) {
				int l = Integer.parseInt(v[i]);
				int r = Integer.parseInt(rvalue);
				if (l < r) {
					return element;
				}
				return null;
			}
			rvalue = stripQuotes(rvalue);
			if (v[i].compareTo(rvalue) > 0) {
				return element;
			}
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.codename1.path.impl.AbstractEvaluator#evaluateLeftGreaterRight(com
	 * .codename1.path.impl.StructuredContent, java.lang.String,
	 * java.lang.String)
	 */
	protected Object evaluateLeftGreaterRight(StructuredContent element,
			String lvalue, String rvalue) {
		String v[] = _getLeftValue(element, lvalue);
		if (v == null) {
			return null;
		}
                int vlen = v.length;
		for (int i = 0; i < vlen; i++) {
			if (isNumeric(rvalue) && isNumeric(v[i])) {
				int l = Integer.parseInt(v[i]);
				int r = Integer.parseInt(rvalue);
				if (l > r) {
					return element;
				}
				return null;
			}
			rvalue = stripQuotes(rvalue);
			if (v[i].compareTo(rvalue) < 0) {
				return element;
			}
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.codename1.path.impl.AbstractEvaluator#evaluateLeftEqualsRight(com
	 * .codename1.path.impl.StructuredContent, java.lang.String,
	 * java.lang.String)
	 */
	protected Object evaluateLeftEqualsRight(StructuredContent element,
			String lvalue, String rvalue) {
		String v[] = _getLeftValue(element, lvalue);
		if (v == null) {
			return null;
		}
                int vlen = v.length;
		for (int i = 0; i < vlen; i++) {
			if (isNumeric(rvalue) && isNumeric(v[i])) {
				int l = Integer.parseInt(v[i]);
				int r = Integer.parseInt(rvalue);
				if (l == r) {
					return element;
				}
				return null;
			}
			rvalue = stripQuotes(rvalue);
			if (v[i].compareTo(rvalue) == 0) {
				return element;
			}
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.codename1.processing.AbstractEvaluator#evaluateSingle(java.util.List
	 * , java.lang.String)
	 */
	protected Object evaluateSingle(StructuredContent element, String expr) {
		Result result = Result.fromContent(element.getChild(0));
		String v = result.getAsString(expr);
		if (v == null) {
			return null;
		}
		return element;
	}

}
