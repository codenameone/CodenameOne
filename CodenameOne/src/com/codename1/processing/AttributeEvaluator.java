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


/// Private class, do not use
///
/// This evaluator handles expressions that involve an attribute. Examples:
///
/// `Get all players names that are from Canada
///
///  /tournament/player[@country='Canada']/name
///
///  Get all players names that have a country specified
///
///  /tournament/player[@country]/name
///
///  Get all players names that don't have a country specified
///
///  //player[@country=null]/name
///
///  Get the tax charged on all items over $5
///
///  //lineitem[@total > 5]/tax
///
///  Get the tax charged on all items under $5
///
///  //lineitem[@total < 5]/tax`
///
/// @author Eric Coolman
class AttributeEvaluator extends AbstractEvaluator {

    /// Construct with the full predicate expression.
    ///
    /// #### Parameters
    ///
    /// - `expr`
    protected AttributeEvaluator(String expr) {
        super(expr);
    }

    /* (non-Javadoc)
     * @see com.codename1.path.impl.AbstractEvaluator#evaluateSingle(com.codename1.path.impl.StructuredContent, java.lang.String)
     */
    @Override
    protected Object evaluateSingle(StructuredContent element, String expr) {
        // The '@' has to come off first. Every other method here strips it
        // before the lookup and this one did not, so it asked the element for
        // an attribute literally named "@rank" -- which no document has, so
        // "[@rank]", the documented way to select the elements that carry an
        // attribute, matched nothing at all and said nothing about it.
        String name = expr.startsWith("@") ? expr.substring(1) : expr;
        // The child counts as well, for the reason the absence test looks at
        // one: a JSON document has no attributes, and the field an attribute
        // expression names is a child there. Without this, "[@rank]" and
        // "[@rank=null]" -- which are complements -- disagreed on JSON, one
        // matching nothing and the other matching correctly.
        if (element.getAttribute(name) != null || hasChild(element, name)) {
            return element;
        }
        return super.evaluateSingle(element, expr);
    }


    /// Whether `element` carries a child of this name.
    ///
    /// The JSON half of "does this element have it?": a document parsed from
    /// JSON has no attributes at all, and the field an attribute expression
    /// names is a child there.
    ///
    /// - `element`: the element to ask
    ///
    /// - `name`: the attribute name, already stripped of its '@'
    ///
    /// #### Returns
    ///
    /// true when a child of that name exists
    private static boolean hasChild(StructuredContent element, String name) {
        java.util.List children = element.getChildren(name);
        return children != null && !children.isEmpty();
    }

    /* (non-Javadoc)
     * @see com.codename1.path.impl.AbstractEvaluator#evaluateLeftLessRight(com.codename1.path.impl.StructuredContent, java.lang.String, java.lang.String)
     */
    @Override
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
    @Override
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
    @Override
    protected Object evaluateLeftEqualsRight(StructuredContent element,
                                             String lvalue, String rvalue) {
        lvalue = lvalue.substring(1);
        String attr = element.getAttribute(lvalue);
        // "[@attr=null]" is the documented way to ask for the elements that do
        // NOT carry an attribute, and it is the one predicate whose answer is
        // yes precisely when the attribute is absent. The check below returned
        // before the rvalue was ever looked at, so it could never match one.
        //
        // Unquoted, because 'null' in quotes is a string the attribute might
        // really hold.
        //
        // A JSON document has no attributes: HashtableContent.getAttribute()
        // answers null for every name, and the guide says so -- an attribute
        // selects the child under that name there. Absence therefore has to
        // mean no attribute AND no such child, or this predicate matched every
        // object in a JSON document, including the ones that carry the field.
        if ("null".equals(rvalue)) {
            return attr == null && !hasChild(element, lvalue) ? element : null;
        }
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
