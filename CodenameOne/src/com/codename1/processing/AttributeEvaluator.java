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
 * Private class, do not use
 * 
 * This evaluator handles expressions that involve an attribute. Examples:
 * 
 * <code>
 *  Get all players names that are from Canada
 *  
 *  /tournament/player[@country='Canada']/name
 *  
 *  Get all players names that have a country specified
 *  
 *  /tournament/player[@country]/name
 *  
 *  Get all players names that don't have a country specified
 *  
 *  //player[@country=null]/name
 *  
 *  Get the tax charged on all items over $5
 *  
 *  //lineitem[@total > 5]/tax
 *  
 *  Get the tax charged on all items under $5
 * 
 *  //lineitem[@total < 5]/tax
 * </code>
 * 
 * @author Eric Coolman
 * 
 */
class AttributeEvaluator extends AbstractEvaluator {

	/**
	 * Construct with the full predicate expression.
	 * 
	 * @param expr
	 */
	protected AttributeEvaluator(String expr) {
		super(expr);
	}

	/* (non-Javadoc)
	 * @see com.codename1.path.impl.AbstractEvaluator#evaluateSingle(com.codename1.path.impl.StructuredContent, java.lang.String)
	 */
	protected Object evaluateSingle(StructuredContent element, String expr) {
		if (element.getAttribute(expr) != null) {
			return element;
		}
		return super.evaluateSingle(element, expr);
	}


	/* (non-Javadoc)
	 * @see com.codename1.path.impl.AbstractEvaluator#evaluateLeftLessRight(com.codename1.path.impl.StructuredContent, java.lang.String, java.lang.String)
	 */
	protected Object evaluateLeftLessRight(StructuredContent element,
			String lvalue, String rvalue) {
		lvalue = lvalue.substring(1);
		String attr = element.getAttribute(lvalue);
		if (attr == null) {
			return null;
		}
		if (isNumeric(rvalue) && isNumeric(attr)) {
			int l = Integer.parseInt(attr);
			int r = Integer.parseInt(rvalue);
			if (l < r) {
				return element;
			} 
			return null;
		}
		rvalue = stripQuotes(rvalue);
		if (attr.compareTo(rvalue) > 0) {
			return element;
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see com.codename1.path.impl.AbstractEvaluator#evaluateLeftGreaterRight(com.codename1.path.impl.StructuredContent, java.lang.String, java.lang.String)
	 */
	protected Object evaluateLeftGreaterRight(StructuredContent element,
			String lvalue, String rvalue) {
		lvalue = lvalue.substring(1);
		String attr = element.getAttribute(lvalue);
		if (attr == null) {
			return null;
		}
		if (isNumeric(rvalue) && isNumeric(attr)) {
			int l = Integer.parseInt(attr);
			int r = Integer.parseInt(rvalue);
			if (l > r) {
				return element;
			} 
			return null;
		}
		rvalue = stripQuotes(rvalue);
		if (attr.compareTo(rvalue) < 0) {
			return element;
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see com.codename1.path.impl.AbstractEvaluator#evaluateLeftEqualsRight(com.codename1.path.impl.StructuredContent, java.lang.String, java.lang.String)
	 */
	protected Object evaluateLeftEqualsRight(StructuredContent element,
			String lvalue, String rvalue) {
		lvalue = lvalue.substring(1);
		String attr = element.getAttribute(lvalue);
		if (attr == null) {
			return null;
		}
		if (isNumeric(rvalue) && isNumeric(attr)) {
			int l = Integer.parseInt(attr);
			int r = Integer.parseInt(rvalue);
			if (l == r) {
				return element;
			} 
			return null;
		}
		rvalue = stripQuotes(rvalue);
		if (attr.compareTo(rvalue) == 0) {
			return element;
		}
		return null;
	}

}
