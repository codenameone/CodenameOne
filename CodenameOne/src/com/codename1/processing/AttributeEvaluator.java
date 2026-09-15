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
        if (!MapContent.attributeOrFields(element, name).isEmpty()) {
            return element;
        }
        return super.evaluateSingle(element, expr);
    }


    /// The text of a value that can be compared, or null when there is none.
    ///
    /// A JSON field can hold an object, and getText() answers a map with its
    /// first KEY -- so "[@profile='name']" matched {"profile":{"name":"A"}},
    /// and which key it matched depended on the map's iteration order. An
    /// object is not a scalar and has nothing to compare against; the step
    /// that READS a field still hands back the object itself.
    ///
    /// An array is not the same case: its values arrive here one at a time,
    /// each in its own node.
    ///
    /// - `value`: one value of the named field
    ///
    /// #### Returns
    ///
    /// the text, or null when the value is structured or has none
    private static String scalarText(StructuredContent value) {
        Object root = value.getNativeRoot();
        if (root instanceof java.util.Map || root instanceof java.util.List) {
            return null;
        }
        return value.getText();
    }

    /* (non-Javadoc)
     * @see com.codename1.path.impl.AbstractEvaluator#evaluateLeftLessRight(com.codename1.path.impl.StructuredContent, java.lang.String, java.lang.String)
     */
    @Override
    protected Object evaluateLeftLessRight(StructuredContent element,
                                           String lvalue, String rvalue) {
        lvalue = lvalue.substring(1);
        java.util.List values = MapContent.attributeOrFields(element, lvalue);
        rvalue = stripQuotes(rvalue);
        // Every value, because a JSON field can be an array and a match on any
        // of them is a match -- the same rule child evaluation has always used.
        for (Object value : values) {
            String attr = scalarText((StructuredContent) value);
            if (attr == null) {
                continue;
            }
            if (isNumeric(rvalue) && isNumeric(attr)) {
                if (compareNumbers(attr, rvalue) < 0) {
                    return element;
                }
                continue;
            }
            if (attr.compareTo(rvalue) < 0) {
                return element;
            }
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
        java.util.List values = MapContent.attributeOrFields(element, lvalue);
        rvalue = stripQuotes(rvalue);
        // Every value, because a JSON field can be an array and a match on any
        // of them is a match -- the same rule child evaluation has always used.
        for (Object value : values) {
            String attr = scalarText((StructuredContent) value);
            if (attr == null) {
                continue;
            }
            if (isNumeric(rvalue) && isNumeric(attr)) {
                if (compareNumbers(attr, rvalue) > 0) {
                    return element;
                }
                continue;
            }
            if (attr.compareTo(rvalue) > 0) {
                return element;
            }
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
        java.util.List values = MapContent.attributeOrFields(element, lvalue);
        // "[@attr=null]" is the documented way to ask for the elements that do
        // NOT carry an attribute, and it is the one predicate whose answer is
        // yes precisely when there is no value. The null check used to return
        // before the rvalue was read, so it could never match one.
        //
        // Unquoted, because 'null' in quotes is a string the value might
        // really be.
        if ("null".equals(rvalue)) {
            return values.isEmpty() ? element : null;
        }
        rvalue = stripQuotes(rvalue);
        for (Object value : values) {
            String attr = scalarText((StructuredContent) value);
            if (attr == null) {
                continue;
            }
            if (isNumeric(rvalue) && isNumeric(attr)) {
                if (compareNumbers(attr, rvalue) == 0) {
                    return element;
                }
                continue;
            }
            if (attr.compareTo(rvalue) == 0) {
                return element;
            }
        }
        return null;
    }

}
