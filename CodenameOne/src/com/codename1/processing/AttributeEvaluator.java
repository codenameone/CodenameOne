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
        if (element.getAttribute(name) != null || hasJsonField(element, name)) {
            return element;
        }
        return super.evaluateSingle(element, expr);
    }


    /// Whether this JSON element has a usable value under this name.
    ///
    /// The JSON half of "does this element have it?". A document parsed from
    /// JSON has no attributes at all -- MapContent.getAttribute() answers null
    /// for every name -- and the field an attribute expression names is a
    /// child there.
    ///
    /// A field explicitly set to null counts as NOT having one, which makes
    /// `[@rank]` and `[@rank=null]` exact complements over three cases --
    /// no key, a null value, a real value -- and matches what the path
    /// answers on its own: reading that field gives null either way. It is
    /// also the only reading XML can share, since an attribute there cannot
    /// be present and null at once, and these two predicates have to mean the
    /// same thing in both formats. Key presence would be a different question
    /// and the expression language has no way to ask it.
    ///
    /// Restricted to JSON on purpose. XML draws the distinction the expression
    /// language does: `[@rank]` asks about an attribute and `[rank]` asks
    /// about a child element, and reading a child here would make the first
    /// match an element that only has the second.
    ///
    /// - `element`: the element to ask
    ///
    /// - `name`: the attribute name, already stripped of its '@'
    ///
    /// #### Returns
    ///
    /// true when this is a JSON element carrying a field of that name
    private static boolean hasJsonField(StructuredContent element, String name) {
        if (!(element instanceof MapContent)) {
            return false;
        }
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
        // A JSON document has no attributes: MapContent.getAttribute() answers
        // null for every name, and the field an attribute expression names is
        // a child there. Absence therefore has to mean no attribute AND no
        // such field, or this predicate matched every object in a JSON
        // document, including the ones that carry the field.
        if ("null".equals(rvalue)) {
            return attr == null && !hasJsonField(element, lvalue) ? element : null;
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
