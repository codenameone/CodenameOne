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

import com.codename1.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

/// Private class, do not use.
///
/// An abstract predicate evaluator handles common tasks of breaking the
/// expression down to lvalue, rvalue, and comparator, then calling the
/// appropriate abstracted method with the result.
///
/// Example predicates:
///
/// `Test price attribute:
///
///  [@price > 45]
///
///  Test Child node:
///
///  [town='Exeter']
///
///  Test attribute exists
///
///  [@price]
///
///  Test attribute doesn't exist:
///
///  [@price = null]
///
///  Select by index:
///
///  [3]
///
///  Select by position:
///
///  [position() < 5]
///
///  Select by position:
///
///  [last() - 5]`
///
/// @author Eric Coolman
abstract class AbstractEvaluator implements Evaluator {

    private final String expr;

    /// Construct with the full predicate expression.
    ///
    /// #### Parameters
    ///
    /// - `expr`: The full predicate expression
    protected AbstractEvaluator(String expr) {
        this.expr = expr;
    }

    /// Evaluate the predicate expression against a given element.
    ///
    /// #### Parameters
    ///
    /// - `element`: the element to apply predicate against.
    @Override
    public Object evaluate(StructuredContent element) {
        return evaluateInternal(element);
    }

    /// Evaluate the predicate expression against an array of elements.
    ///
    /// #### Parameters
    ///
    /// - `elements`: an array of elements to apply predicate against.
    @Override
    public Object evaluate(List elements) {
        return evaluateInternal(elements);
    }

    /// This internal method takes care of determining the style of expression
    /// (less-than, greater-than, equals, etc), and passing on the to next
    /// internal processor.
    ///
    /// #### Parameters
    ///
    /// - `element`: source element, either a List or StructuredContent
    ///
    /// #### Returns
    ///
    /// either a List or StructuredContent
    ///
    /// #### Throws
    ///
    /// - `ResultException`
    ///
    /// #### See also
    ///
    /// - `int)`
    ///
    /// - #evaluateSingleInternal(Object)
    private Object evaluateInternal(Object element) {
        if (element == null) {
            return null;
        }
        int index = expr.indexOf("=");
        if (index != -1) {
            return evaluateInternal(element, index);
        }
        index = expr.indexOf(">");
        if (index != -1) {
            return evaluateInternal(element, index);
        }
        index = expr.indexOf("<");
        if (index != -1) {
            return evaluateInternal(element, index);
        }
        index = expr.indexOf("%");
        if (index != -1) {
            return evaluateInternal(element, index);
        }
        return evaluateSingleInternal(element);

    }

    /// This internal method handles breaking down an expression into it's
    /// components (lvalue, rvalue, comparand), and then passing along to the
    /// next internal processor.
    ///
    /// #### Parameters
    ///
    /// - `element`: either a List or a StructuredContent object
    ///
    /// - `index`: pointer to the comparator within the predicate expression
    ///
    /// #### Returns
    ///
    /// either a List or a StructuredContent object
    ///
    /// #### See also
    ///
    /// - #evaluateLeftEqualsRightInternal(Object, String, String)
    ///
    /// - #evaluateLeftGreaterRightInternal(Object, String, String)
    ///
    /// - #evaluateLeftLessRightInternal(Object, String, String)
    private Object evaluateInternal(Object element, int index) {
        String lvalue = expr.substring(0, index).trim();
        String rvalue = expr.substring(index + 1).trim();
        char comparator = expr.charAt(index);
        switch (comparator) {
            case '=':
                return evaluateLeftEqualsRightInternal(element, lvalue, rvalue);
            case '>':
                return evaluateLeftGreaterRightInternal(element, lvalue, rvalue);
            case '<':
                return evaluateLeftLessRightInternal(element, lvalue, rvalue);
            case '%':
                return evaluateLeftContainsRightInternal(element, lvalue, rvalue);
            default:
                return null;
        }
    }

    /// This internal method simply makes a type safe call the the proper
    /// abstract method based on the type of element passed.
    ///
    /// #### Parameters
    ///
    /// - `element`: either a StructuredContent or List object.
    ///
    /// - `lvalue`: lvalue of predicate expression
    ///
    /// - `rvalue`: rvalue of predicate expression
    ///
    /// #### Returns
    ///
    /// either a StructuredContent or List object.
    ///
    /// #### See also
    ///
    /// - #evaluateLeftLessRight(StructuredContent, String, String)
    ///
    /// - #evaluateLeftLessRight(List, String, String)
    private Object evaluateLeftLessRightInternal(Object element, String lvalue,
                                                 String rvalue) {
        if (element instanceof List) {
            return evaluateLeftLessRight((List) element, lvalue, rvalue);
        } else {
            return evaluateLeftLessRight((StructuredContent) element, lvalue,
                    rvalue);
        }
    }

    /// This internal method simply makes a type safe call the the proper
    /// abstract method based on the type of element passed.
    ///
    /// #### Parameters
    ///
    /// - `element`: either a StructuredContent or List object.
    ///
    /// - `lvalue`: lvalue of predicate expression
    ///
    /// - `rvalue`: rvalue of predicate expression
    ///
    /// #### Returns
    ///
    /// either a StructuredContent or List object.
    ///
    /// #### See also
    ///
    /// - #evaluateLeftGreaterRight(StructuredContent, String, String)
    ///
    /// - #evaluateLeftGreaterRight(List, String, String)
    private Object evaluateLeftGreaterRightInternal(Object element, String lvalue,
                                                    String rvalue) {
        if (element instanceof List) {
            return evaluateLeftGreaterRight((List) element, lvalue, rvalue);
        } else {
            return evaluateLeftGreaterRight((StructuredContent) element,
                    lvalue, rvalue);
        }
    }

    /// This internal method simply makes a type safe call the the proper
    /// abstract method based on the type of element passed.
    ///
    /// #### Parameters
    ///
    /// - `element`: either a StructuredContent or List object.
    ///
    /// - `lvalue`: lvalue of predicate expression
    ///
    /// - `rvalue`: rvalue of predicate expression
    ///
    /// #### Returns
    ///
    /// either a StructuredContent or List object.
    ///
    /// #### See also
    ///
    /// - #evaluateLeftEqualsRight(List, String, String)
    ///
    /// - #evaluateLeftEqualsRight(StructuredContent, String, String)
    private Object evaluateLeftEqualsRightInternal(Object element, String lvalue,
                                                   String rvalue) {
        if (element instanceof List) {
            return evaluateLeftEqualsRight((List) element, lvalue, rvalue);
        } else {
            return evaluateLeftEqualsRight((StructuredContent) element, lvalue,
                    rvalue);
        }
    }

    /// This internal method simply makes a type safe call the the proper
    /// abstract method based on the type of element passed.
    ///
    /// #### Parameters
    ///
    /// - `element`: either a StructuredContent or List object.
    ///
    /// - `lvalue`: lvalue of predicate expression
    ///
    /// - `rvalue`: rvalue of predicate expression
    ///
    /// #### Returns
    ///
    /// either a StructuredContent or List object.
    ///
    /// #### See also
    ///
    /// - #evaluateLeftContainsRight(List, String, String)
    ///
    /// - #evaluateLeftContainsRight(StructuredContent, String, String)
    private Object evaluateLeftContainsRightInternal(Object element, String lvalue,
                                                     String rvalue) {
        if (element instanceof List) {
            return evaluateLeftContainsRight((List) element, lvalue, rvalue);
        } else {
            return evaluateLeftContainsRight((StructuredContent) element, lvalue,
                    rvalue);
        }
    }

    /// This internal method simply makes a type safe call the the proper
    /// abstract method based on the type of element passed.
    ///
    /// #### Parameters
    ///
    /// - `element`: either a StructuredContent or List object.
    ///
    /// - `lvalue`: lvalue of predicate expression
    ///
    /// - `rvalue`: rvalue of predicate expression
    ///
    /// #### Returns
    ///
    /// either a StructuredContent or List object.
    ///
    /// #### See also
    ///
    /// - #evaluateSingle(StructuredContent, String)
    ///
    /// - #evaluateSingle(List, String)
    private Object evaluateSingleInternal(Object element) {
        if (element instanceof List) {
            return evaluateSingle((List) element, expr);
        } else {
            return evaluateSingle((StructuredContent) element, expr);
        }
    }

    /// Utility method for subclasses to determine if a string is a number
    ///
    /// Digits, and also a leading sign, one decimal point and an exponent --
    /// the grammar `Double.parseDouble` reads, because that is what the
    /// callers hand it. Digits alone was too narrow once an attribute
    /// expression could read a JSON field: the parser returns numbers as
    /// doubles, so a rank of 1 arrives as "1.0" and a rank of ten million as
    /// "1.0E7". A comparison that cannot see either as a number falls through
    /// to comparing them as text, where "5.0" is less than "3" and "1.0E7"
    /// comes before it.
    ///
    /// #### Parameters
    ///
    /// - `text`: value to test
    ///
    /// #### Returns
    ///
    /// true when the value is a number
    protected boolean isNumeric(String text) {
        String value = text.trim();
        int length = value.length();
        int at = 0;
        if (at < length && (value.charAt(at) == '-' || value.charAt(at) == '+')) {
            at++;
        }
        int digits = 0;
        while (at < length && Character.isDigit(value.charAt(at))) {
            at++;
            digits++;
        }
        if (at < length && value.charAt(at) == '.') {
            at++;
            while (at < length && Character.isDigit(value.charAt(at))) {
                at++;
                digits++;
            }
        }
        if (digits == 0) {
            return false;
        }
        if (at < length && (value.charAt(at) == 'e' || value.charAt(at) == 'E')) {
            at++;
            if (at < length
                    && (value.charAt(at) == '-' || value.charAt(at) == '+')) {
                at++;
            }
            int exponent = 0;
            while (at < length && Character.isDigit(value.charAt(at))) {
                at++;
                exponent++;
            }
            if (exponent == 0) {
                return false;
            }
        }
        return at == length;
    }

    /// Compares two numeric strings, exactly where the values allow it.
    ///
    /// Integers go through `long`. Coercing them to `double` loses precision
    /// above 2^53, which is inside the range of an ordinary 64-bit id: the
    /// literals 9007199254740992 and 9007199254740993 round to the same
    /// double, so a predicate written for one of them selected both. Anything
    /// with a decimal point or an exponent is a `double` and has no exact
    /// alternative.
    ///
    /// - `left`: the value read from the document
    ///
    /// - `right`: the value written in the predicate
    ///
    /// #### Returns
    ///
    /// negative, zero or positive, as `compareTo` does
    protected int compareNumbers(String left, String right) {
        if (isInteger(left) && isInteger(right)) {
            try {
                long l = Long.parseLong(left.trim());
                long r = Long.parseLong(right.trim());
                return l < r ? -1 : (l > r ? 1 : 0);
            } catch (NumberFormatException tooBigForALong) {
                // Falls through to the double comparison below, which is the
                // best available answer for a value no integer type holds.
            }
        }
        double l = Double.parseDouble(left.trim());
        double r = Double.parseDouble(right.trim());
        return l < r ? -1 : (l > r ? 1 : 0);
    }

    /// Whether this number is written as a whole number, with no point or
    /// exponent, so it can be compared exactly.
    ///
    /// - `text`: a value [#isNumeric] already accepted
    ///
    /// #### Returns
    ///
    /// true when the value is an integer literal
    private boolean isInteger(String text) {
        String value = text.trim();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '.' || c == 'e' || c == 'E') {
                return false;
            }
        }
        return value.length() > 0;
    }

    /// Utility method for subclasses to determine strip single/double quotes
    /// from a string
    ///
    /// #### Parameters
    ///
    /// - `text`: value to transform
    ///
    /// #### Returns
    ///
    /// the value without quotes.
    protected String stripQuotes(String rvalue) {
        StringBuffer buf = new StringBuffer();
        int rvlen = rvalue.length();
        for (int i = 0; i < rvlen; i++) {
            char ch = rvalue.charAt(i);
            if (ch != '\'' && ch != '\"') {
                buf.append(ch);
            }
        }
        return buf.toString();
    }

    /// Utility method for subclasses to convert a string to an array, delimited
    /// by comma, optionally enclosed in brackets, and elements optionally
    /// enclosed in quotes.
    ///
    /// #### Parameters
    ///
    /// - `text`: value to transform
    ///
    /// #### Returns
    ///
    /// the value as an array.
    protected String[] explode(String arrayAsString) {
        arrayAsString = arrayAsString.trim();
        if (arrayAsString.startsWith("(") && arrayAsString.endsWith(")")) {
            arrayAsString = arrayAsString.substring(1, arrayAsString.length() - 1);
        }
        List v = StringUtil.tokenizeString(arrayAsString, ',');
        String[] a = new String[v.size()];
        int index = 0;
        for (Object item : v) {
            a[index++] = stripQuotes(item.toString().trim());
        }
        return a;
    }

    /// Override this element to handle testing a predicate expression with no
    /// comparator.
    ///
    /// #### Parameters
    ///
    /// - `element`: a single StructuredContent element
    ///
    /// - `expr`: the full predicate expression
    ///
    /// #### Returns
    ///
    /// @return either a single StructuredContent or an array (List) of
    /// StructuredContent object.
    protected Object evaluateSingle(StructuredContent element, String expr) {
        return null;
    }

    /// Override this element to handle testing a predicate expression with no
    /// comparator. By default, this implementation will call evaluateSingle()
    /// against each element of the array, and return an array of all elements
    /// that didn't return null.
    ///
    /// #### Parameters
    ///
    /// - `element`: an array of StructuredContent elements
    ///
    /// - `expr`: the full predicate expression
    ///
    /// #### Returns
    ///
    /// @return either a single StructuredContent or an array (List) of
    /// StructuredContent object.
    protected Object evaluateSingle(List elements, String expr) {
        List array;
        if (elements instanceof java.util.Vector) {
            array = new java.util.Vector();
        } else {
            array = new ArrayList();
        }
        for (Object o : elements) {
            if (o instanceof StructuredContent) {
                o = evaluateSingle((StructuredContent) o, expr);
                if (o != null) {
                    array.add(o);
                }
            }
        }
        if (array.size() == 1) {
            return array.get(0);
        }
        return array;
    }

    /// Override this element to handle testing a predicate expression where
    /// lvalue < rvalue. By default, this implementation will call
    /// evaluateLeftLessRight() against each element of the array, and return an
    /// array of all elements that didn't return null.
    ///
    /// #### Parameters
    ///
    /// - `element`: an array of StructuredContent elements
    ///
    /// - `expr`: the full predicate expression
    ///
    /// #### Returns
    ///
    /// @return either a single StructuredContent or an array (List) of
    /// StructuredContent object.
    protected Object evaluateLeftLessRight(List elements, String lvalue,
                                           String rvalue) {
        List array;
        if (elements instanceof java.util.Vector) {
            array = new java.util.Vector();
        } else {
            array = new ArrayList();
        }

        for (Object o : elements) {
            if (o instanceof StructuredContent) {
                o = evaluateLeftLessRight((StructuredContent) o, lvalue, rvalue);
                if (o != null) {
                    array.add(o);
                }
            }
        }
        if (array.size() == 1) {
            return array.get(0);
        }
        return array;
    }

    /// Override this element to handle testing a predicate expression where
    /// lvalue < rvalue.
    ///
    /// #### Parameters
    ///
    /// - `element`: a single StructuredContent element
    ///
    /// - `expr`: the full predicate expression
    ///
    /// #### Returns
    ///
    /// @return either a single StructuredContent or an array (List) of
    /// StructuredContent object.
    protected Object evaluateLeftLessRight(StructuredContent element,
                                           String lvalue, String rvalue) {
        return null;
    }

    /// Override this element to handle testing a predicate expression where
    /// lvalue > rvalue. By default, this implementation will call
    /// evaluateLeftGreaterRight() against each element of the array, and return
    /// an array of all elements that didn't return null.
    ///
    /// #### Parameters
    ///
    /// - `element`: an array of StructuredContent elements
    ///
    /// - `expr`: the full predicate expression
    ///
    /// #### Returns
    ///
    /// @return either a single StructuredContent or an array (List) of
    /// StructuredContent object.
    protected Object evaluateLeftGreaterRight(List elements, String lvalue,
                                              String rvalue) {
        List array;
        if (elements instanceof java.util.Vector) {
            array = new java.util.Vector();
        } else {
            array = new ArrayList();
        }

        for (Object o : elements) {
            if (o instanceof StructuredContent) {
                o = evaluateLeftGreaterRight((StructuredContent) o, lvalue, rvalue);
                if (o != null) {
                    array.add(o);
                }
            }
        }
        if (array.size() == 1) {
            return array.get(0);
        }
        return array;
    }

    /// Override this element to handle testing a predicate expression where
    /// lvalue > rvalue.
    ///
    /// #### Parameters
    ///
    /// - `element`: a single StructuredContent element
    ///
    /// - `expr`: the full predicate expression
    ///
    /// #### Returns
    ///
    /// @return either a single StructuredContent or an array (List) of
    /// StructuredContent object.
    protected Object evaluateLeftGreaterRight(StructuredContent element,
                                              String lvalue, String rvalue) {
        return null;
    }

    /// Override this element to handle testing a predicate expression where
    /// lvalue = rvalue. By default, this implementation will call
    /// evaluateLeftEqualsRight() against each element of the array, and return
    /// an array of all elements that didn't return null.
    ///
    /// #### Parameters
    ///
    /// - `element`: an array of StructuredContent elements
    ///
    /// - `expr`: the full predicate expression
    ///
    /// #### Returns
    ///
    /// @return either a single StructuredContent or an array (List) of
    /// StructuredContent object.
    protected Object evaluateLeftEqualsRight(List elements, String lvalue,
                                             String rvalue) {
        List array;
        if (elements instanceof java.util.Vector) {
            array = new java.util.Vector();
        } else {
            array = new ArrayList();
        }

        for (Object o : elements) {
            if (o instanceof StructuredContent) {
                o = evaluateLeftEqualsRight((StructuredContent) o, lvalue, rvalue);
                if (o != null) {
                    array.add(o);
                }
            }
        }
        if (array.size() == 1) {
            return array.get(0);
        }
        return array;
    }

    /// Override this element to handle testing a predicate expression where
    /// lvalue = rvalue.
    ///
    /// #### Parameters
    ///
    /// - `element`: a single StructuredContent element
    ///
    /// - `expr`: the full predicate expression
    ///
    /// #### Returns
    ///
    /// @return either a single StructuredContent or an array (List) of
    /// StructuredContent object.
    protected Object evaluateLeftEqualsRight(StructuredContent element,
                                             String lvalue, String rvalue) {
        return null;
    }

    /// Override this element to handle testing a predicate expression where
    /// lvalue % rvalue. By default, this implementation will call
    /// evaluateLeftContainsRight() against each element of the array, and return
    /// an array of all elements that didn't return null.
    ///
    /// #### Parameters
    ///
    /// - `element`: an array of StructuredContent elements
    ///
    /// - `expr`: the full predicate expression
    ///
    /// #### Returns
    ///
    /// @return either a single StructuredContent or an array (List) of
    /// StructuredContent object.
    protected Object evaluateLeftContainsRight(List elements, String lvalue,
                                               String rvalue) {
        List array;
        if (elements instanceof java.util.Vector) {
            array = new java.util.Vector();
        } else {
            array = new ArrayList();
        }

        for (Object o : elements) {
            if (o instanceof StructuredContent) {
                o = evaluateLeftContainsRight((StructuredContent) o, lvalue, rvalue);
                if (o != null) {
                    array.add(o);
                }
            }
        }
        if (array.size() == 1) {
            return array.get(0);
        }
        return array;
    }

    /// Override this element to handle testing a predicate expression where
    /// lvalue % rvalue.
    ///
    /// #### Parameters
    ///
    /// - `element`: a single StructuredContent element
    ///
    /// - `expr`: the full predicate expression
    ///
    /// #### Returns
    ///
    /// @return either a single StructuredContent or an array (List) of
    /// StructuredContent object.
    protected Object evaluateLeftContainsRight(StructuredContent element,
                                               String lvalue, String rvalue) {
        return null;
    }
}
