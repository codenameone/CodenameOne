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
 * Predicate Evaluator Factory, attempts to create a predicate evaluator given the expression.
 * 
 * ie:
 * 
 * <code>
 * [0] = IndexEvaluator
 * [last()] = IndexEvaluator
 * [@ponum=3] = AttributeEvaluator
 * [lastname='Coolman'] = TextEvaluator
 * </code>
 * 
 * @author Eric Coolman
 *
 */
class EvaluatorFactory {
	/**
	 * Construct an evaluator for a given predicate expression.
	 * 
	 * @param text a full predicate expression.
	 * @return an Evaluator instance that can process the expression.
	 */
	public static Evaluator createEvaluator(String text) {
		if (isNumeric(text)) {
			return new IndexEvaluator(text);
		} else if (text.startsWith("@")) {
			return new AttributeEvaluator(text);
		} else if (text.indexOf(IndexEvaluator.FUNC_LAST) != -1) {
			return new IndexEvaluator(text);
		} else if (text.indexOf(TextEvaluator.FUNC_TEXT) != -1) { 
			return new TextEvaluator(text);
		} else if (text.indexOf(IndexEvaluator.FUNC_POSITION) != -1) {
			return new IndexEvaluator(text);
		} else if (text.startsWith("@")) {
			return new AttributeEvaluator(text);
		} else if (text.indexOf('=') != -1) {
			return new TextEvaluator(text);
		}
		throw new IllegalStateException("Could not create a comparator for value: " + text);
	}
	
	/**
	 * Test if string contains only digits.
	 * 
	 * @param text
	 * @return
	 */
	private static boolean isNumeric(String text) {
		text = text.trim();
		for (int i = 0; i < text.length(); i++) {
			if (Character.isDigit(text.charAt(i)) == false) {
				return false;
			}
		}
		return true;
	}
	
}
