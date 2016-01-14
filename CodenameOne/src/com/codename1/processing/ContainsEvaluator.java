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
 * This evaluator handles expressions that test if rvalue is contained in lvalue. Examples:
 * 
 * <code>
 *  Get all long name of the address component where the types array includes both values "neighborhood" and "political"
 *  
 *  /results/address_components[types % (neighborhood, political)]/long_name
 * 
 *  Get all long name of the address component where the types array includes a value of "political"
 *  
 *  /results/address_components[types % (neighborhood, political)]/long_name
 * 
 *  Get all long name of the address component where the short name contains "ny"
 *  
 *  /results/address_components[short_name % ny]/long_name
 * </code>
 * 
 * @author Eric Coolman
 * 
 */
class ContainsEvaluator extends AbstractEvaluator {
	/**
	 * Construct with a full predicate expression.
	 * 
	 * @param expr a full predicate expression.
	 */
	public ContainsEvaluator(String expr) {
		super(expr);
	}

	private String[] _getLeftValue(StructuredContent element, String lvalue) {
		String v[];
		// getChild() is a bit of a hack here because the content object
		// calls getParent()
		StructuredContent child = element.getChild(0);
		Result result = Result.fromContent(child);
		v = result.getAsStringArray(lvalue);
		return v;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.codename1.path.impl.AbstractEvaluator#evaluateLeftEqualsRight(com
	 * .codename1.path.impl.StructuredContent, java.lang.String,
	 * java.lang.String)
	 */
	protected Object evaluateLeftContainsRight(StructuredContent element, String lvalue, String rvalue) {
		String lvalues[] = _getLeftValue(element, lvalue);
		if (lvalues == null) {
			return null;
		}
		// if the rvalue is wrapped with "()", the caller explicitly expects the lvalue to be an array of values, 
		// otherwise try to do a "string contains" match first if there's only one lvalue
		if (rvalue.indexOf("(") == -1 && lvalues.length == 1) {
			if (lvalues[0].toLowerCase().indexOf(rvalue.toLowerCase()) != -1) {
				return element;
			}
		}
		String rvalues[] = explode(rvalue);
		int i;
                int lvlen = lvalues.length;
		for (String r : rvalues) { 
			for (i = 0; i < lvlen; i++) {
				String l = lvalues[i];
				if (l.equalsIgnoreCase(r)) {
					break;
				}
			}
			if (i == lvlen) {
				return null;
			}
		}
		return element;
	}
}
