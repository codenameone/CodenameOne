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

import java.util.Enumeration;
import java.util.Vector;

/**
 * Private class, do not use.
 * 
 * An abstract predicate evaluator handles common tasks of breaking the expression 
 * down to lvalue, rvalue, and comparator, then calling the appropriate abstracted 
 * method with the result.
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
 * @author Eric Coolman
 * 
 */
abstract class AbstractEvaluator implements Evaluator {
	private String expr;

	/**
	 * Construct with the full predicate expression.
	 * 
	 * @param expr The full predicate expression
	 */
	protected AbstractEvaluator(String expr) {
		this.expr = expr;
	}

	/**
	 * Evaluate the predicate expression against a given element.
	 * 
	 * @param element the element to apply predicate against.
	 */
	public Object evaluate(StructuredContent element) {
		return _evaluate(element);
	}

	/**
	 * Evaluate the predicate expression against an array of elements.
	 * 
	 * @param elements an array of elements to apply predicate against.
	 */
	public Object evaluate(Vector elements) {
		return _evaluate(elements);
	}

	/**
	 * This internal method takes care of determining the style of expression
	 * (less-than, greater-than, equals, etc), and passing on the to next
	 * internal processor.
	 * 
	 * @param element
	 *            source element, either a Vector or StructuredContent
	 * @return either a Vector or StructuredContent
	 * @throws ResultException
	 * @see {@link #_evaluate(Object, int)}
	 * @see #_evaluateSingle(Object)
	 */
	private Object _evaluate(Object element) {
		if (element == null) {
			return null;
		}
		int index = expr.indexOf("=");
		if (index != -1) {
			return _evaluate(element, index);
		}
		index = expr.indexOf(">");
		if (index != -1) {
			return _evaluate(element, index);
		}
		index = expr.indexOf("<");
		if (index != -1) {
			return _evaluate(element, index);
		}
		return _evaluateSingle(element);

	}

	/**
	 * This internal method handles breaking down an expression into it's
	 * components (lvalue, rvalue, comparand), and then passing along to the
	 * next internal processor.
	 * 
	 * @param element
	 *            either a Vector or a StructuredContent object
	 * @param index
	 *            pointer to the comparator within the predicate expression
	 * @return either a Vector or a StructuredContent object
	 * @see #_evaluateLeftEqualsRight(Object, String, String)
	 * @see #_evaluateLeftGreaterRight(Object, String, String)
	 * @see #_evaluateLeftLessRight(Object, String, String)
	 */
	private Object _evaluate(Object element, int index) {
		String lvalue = expr.substring(0, index).trim();
		String rvalue = expr.substring(index + 1).trim();
		char comparator = expr.charAt(index);
		switch (comparator) {
		case '=':
			return _evaluateLeftEqualsRight(element, lvalue, rvalue);
		case '>':
			return _evaluateLeftGreaterRight(element, lvalue, rvalue);
		case '<':
			return _evaluateLeftLessRight(element, lvalue, rvalue);
		}
		return null;
	}

	/**
	 * This internal method simply makes a type safe call the the proper
	 * abstract method based on the type of element passed.
	 * 
	 * @param element
	 *            either a StructuredContent or Vector object.
	 * @param lvalue
	 *            lvalue of predicate expression
	 * @param rvalue
	 *            rvalue of predicate expression
	 * @return either a StructuredContent or Vector object.
	 * @see #evaluateLeftLessRight(StructuredContent, String, String)
	 * @see #evaluateLeftLessRight(Vector, String, String)
	 */
	private Object _evaluateLeftLessRight(Object element, String lvalue,
			String rvalue) {
		if (element instanceof Vector) {
			return evaluateLeftLessRight((Vector) element, lvalue, rvalue);
		} else {
			return evaluateLeftLessRight((StructuredContent) element, lvalue,
					rvalue);
		}
	}

	/**
	 * This internal method simply makes a type safe call the the proper
	 * abstract method based on the type of element passed.
	 * 
	 * @param element
	 *            either a StructuredContent or Vector object.
	 * @param lvalue
	 *            lvalue of predicate expression
	 * @param rvalue
	 *            rvalue of predicate expression
	 * @return either a StructuredContent or Vector object.
	 * @see #evaluateLeftGreaterRight(StructuredContent, String, String)
	 * @see #evaluateLeftGreaterRight(Vector, String, String)
	 */
	private Object _evaluateLeftGreaterRight(Object element, String lvalue,
			String rvalue) {
		if (element instanceof Vector) {
			return evaluateLeftGreaterRight((Vector) element, lvalue, rvalue);
		} else {
			return evaluateLeftGreaterRight((StructuredContent) element,
					lvalue, rvalue);
		}
	}

	/**
	 * This internal method simply makes a type safe call the the proper
	 * abstract method based on the type of element passed.
	 * 
	 * @param element
	 *            either a StructuredContent or Vector object.
	 * @param lvalue
	 *            lvalue of predicate expression
	 * @param rvalue
	 *            rvalue of predicate expression
	 * @return either a StructuredContent or Vector object.
	 * @see #evaluateLeftEqualsRight(Vector, String, String)
	 * @see #evaluateLeftEqualsRight(StructuredContent, String, String)
	 */
	private Object _evaluateLeftEqualsRight(Object element, String lvalue,
			String rvalue) {
		if (element instanceof Vector) {
			return evaluateLeftEqualsRight((Vector) element, lvalue, rvalue);
		} else {
			return evaluateLeftEqualsRight((StructuredContent) element, lvalue,
					rvalue);
		}
	}

	/**
	 * This internal method simply makes a type safe call the the proper
	 * abstract method based on the type of element passed.
	 * 
	 * @param element
	 *            either a StructuredContent or Vector object.
	 * @param lvalue
	 *            lvalue of predicate expression
	 * @param rvalue
	 *            rvalue of predicate expression
	 * @return either a StructuredContent or Vector object.
	 * @see #evaluateSingle(StructuredContent, String)
	 * @see #evaluateSingle(Vector, String)
	 */
	private Object _evaluateSingle(Object element) {
		if (element instanceof Vector) {
			return evaluateSingle((Vector) element, expr);
		} else {
			return evaluateSingle((StructuredContent) element, expr);
		}
	}

	/**
	 * Utility method for subclasses to determine if an entire string is digits
	 * 
	 * @param text value to test
	 * @return true of the value contains only digits.
	 */
	protected boolean isNumeric(String text) {
		text = text.trim();
		for (int i = 0; i < text.length(); i++) {
			if (Character.isDigit(text.charAt(i)) == false) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Utility method for subclasses to determine strip single/double quotes from a string
	 * 
	 * @param text value to transform
	 * @return the value without quotes.
	 */
	protected String stripQuotes(String rvalue) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < rvalue.length(); i++) {
			char ch = rvalue.charAt(i);
			if (ch != '\'' && ch != '\"') {
				buf.append(ch);
			}
		}
		return buf.toString();
	}
	
	/**
	 * Override this element to handle testing a predicate expression with no
	 * comparator.
	 * 
	 * @param element
	 *            a single StructuredContent element
	 * @param expr
	 *            the full predicate expression
	 * @return either a single StructuredContent or an array (vector) of
	 *         StructuredContent object.
	 */
	protected Object evaluateSingle(StructuredContent element, String expr) {
		return null;
	}

	/**
	 * Override this element to handle testing a predicate expression with no
	 * comparator.  By default, this implementation will call evaluateSingle()
	 * against each element of the array, and return an array of all elements
	 * that didn't return null.
	 * 
	 * @param element
	 *            an array of StructuredContent elements
	 * @param expr
	 *            the full predicate expression
	 * @return either a single StructuredContent or an array (vector) of
	 *         StructuredContent object.
	 */
	protected Object evaluateSingle(Vector elements, String expr) {
		Vector array = new Vector();
		for (Enumeration e = elements.elements(); e.hasMoreElements();) {
			Object o = e.nextElement();
			if (o instanceof StructuredContent) {
				if ((o = evaluateSingle((StructuredContent) o, expr)) != null) {
					array.addElement(o);
				}
			}
		}
		if (array.size() == 1) {
			return (StructuredContent) array.elementAt(0);
		}
		return array;
	}

	/**
	 * Override this element to handle testing a predicate expression where
	 * lvalue < rvalue. By default, this implementation will call evaluateLeftLessRight()
	 * against each element of the array, and return an array of all elements
	 * that didn't return null.
	 * 
	 * @param element
	 *            an array of StructuredContent elements
	 * @param expr
	 *            the full predicate expression
	 * @return either a single StructuredContent or an array (vector) of
	 *         StructuredContent object.
	 */
	protected Object evaluateLeftLessRight(Vector elements, String lvalue,
			String rvalue) {
		Vector array = new Vector();
		for (Enumeration e = elements.elements(); e.hasMoreElements();) {
			Object o = e.nextElement();
			if (o instanceof StructuredContent) {
				if ((o = evaluateLeftLessRight((StructuredContent) o, lvalue, rvalue)) != null) {
					array.addElement(o);
				}
			}
		}
		if (array.size() == 1) {
			return (StructuredContent) array.elementAt(0);
		}
		return array;
	}

	/**
	 * Override this element to handle testing a predicate expression where
	 * lvalue < rvalue.
	 * 
	 * @param element
	 *            a single StructuredContent element
	 * @param expr
	 *            the full predicate expression
	 * @return either a single StructuredContent or an array (vector) of
	 *         StructuredContent object.
	 */
	protected Object evaluateLeftLessRight(StructuredContent element,
			String lvalue, String rvalue) {
		return null;
	}

	/**
	 * Override this element to handle testing a predicate expression where
	 * lvalue > rvalue.    By default, this implementation will call evaluateLeftGreaterRight()
	 * against each element of the array, and return an array of all elements
	 * that didn't return null.
	 * 
	 * @param element
	 *            an array of StructuredContent elements
	 * @param expr
	 *            the full predicate expression
	 * @return either a single StructuredContent or an array (vector) of
	 *         StructuredContent object.
	 */
	protected Object evaluateLeftGreaterRight(Vector elements, String lvalue,
			String rvalue) {
		Vector array = new Vector();
		for (Enumeration e = elements.elements(); e.hasMoreElements();) {
			Object o = e.nextElement();
			if (o instanceof StructuredContent) {
				if ((o = evaluateLeftGreaterRight((StructuredContent) o, lvalue, rvalue)) != null) {
					array.addElement(o);
				}
			}
		}
		if (array.size() == 1) {
			return (StructuredContent) array.elementAt(0);
		}
		return array;
	}

	/**
	 * Override this element to handle testing a predicate expression where
	 * lvalue > rvalue.
	 * 
	 * @param element
	 *            a single StructuredContent element
	 * @param expr
	 *            the full predicate expression
	 * @return either a single StructuredContent or an array (vector) of
	 *         StructuredContent object.
	 */
	protected Object evaluateLeftGreaterRight(StructuredContent element,
			String lvalue, String rvalue) {
		return null;
	}

	/**
	 * Override this element to handle testing a predicate expression where
	 * lvalue = rvalue.    By default, this implementation will call evaluateLeftEqualsRight()
	 * against each element of the array, and return an array of all elements
	 * that didn't return null.
	 * 
	 * @param element
	 *            an array of StructuredContent elements
	 * @param expr
	 *            the full predicate expression
	 * @return either a single StructuredContent or an array (vector) of
	 *         StructuredContent object.
	 */
	protected Object evaluateLeftEqualsRight(Vector elements, String lvalue,
			String rvalue) {
		Vector array = new Vector();
		for (Enumeration e = elements.elements(); e.hasMoreElements();) {
			Object o = e.nextElement();
			if (o instanceof StructuredContent) {
				if ((o = evaluateLeftEqualsRight((StructuredContent) o, lvalue, rvalue)) != null) {
					array.addElement(o);
				}
			}
		}
		if (array.size() == 1) {
			return (StructuredContent) array.elementAt(0);
		}
		return array;
	}

	/**
	 * Override this element to handle testing a predicate expression where
	 * lvalue = rvalue.
	 * 
	 * @param element
	 *            a single StructuredContent element
	 * @param expr
	 *            the full predicate expression
	 * @return either a single StructuredContent or an array (vector) of
	 *         StructuredContent object.
	 */
	protected Object evaluateLeftEqualsRight(StructuredContent element,
			String lvalue, String rvalue) {
		return null;
	}
}
