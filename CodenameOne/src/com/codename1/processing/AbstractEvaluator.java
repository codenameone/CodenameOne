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
    /// Whole numbers are compared digit by digit, at any size. Coercing them
    /// to `double` loses precision above 2^53, which is inside the range of an
    /// ordinary 64-bit id -- 9007199254740992 and 9007199254740993 are the
    /// same double, so a predicate written for one selected both -- and a
    /// `long` only moves the edge to 2^63 rather than removing it. Digits have
    /// no edge. Anything with a decimal point or an exponent is a `double`,
    /// which is what it was parsed as and what it has to be compared as.
    ///
    /// - `left`: the value read from the document
    ///
    /// - `right`: the value written in the predicate
    ///
    /// #### Returns
    ///
    /// negative, zero or positive, as `compareTo` does
    protected int compareNumbers(String left, String right) {
        // Exponents are written out rather than parsed. A document can carry
        // one -- an XML attribute holds whatever text it likes -- and through
        // a double, 9007199254740992e0 and 9007199254740993e0 are one value
        // while 1e309 and 2e309 are both infinity.
        String l = left.trim();
        String r = right.trim();
        if (isBeyondExpansion(l) || isBeyondExpansion(r)) {
            return compareBeyondExpansion(l, r);
        }
        return compareFixedPoint(withoutExponent(l), withoutExponent(r));
    }

    /// Whether writing this value out would take more digits than it is worth.
    private boolean isBeyondExpansion(String text) {
        int marker = exponentMarker(text);
        if (marker < 0) {
            return false;
        }
        String exponent = text.substring(marker + 1).trim();
        int at = exponent.length() > 0
                && (exponent.charAt(0) == '-' || exponent.charAt(0) == '+')
                ? 1 : 0;
        while (at < exponent.length()
                && Character.digit(exponent.charAt(at), 10) == 0) {
            at++;
        }
        // By length rather than by value, so no ceiling is involved: more
        // digits than the limit has cannot be inside it.
        int digits = exponent.length() - at;
        return digits > EXPONENT_LIMIT_DIGITS
                || (digits == EXPONENT_LIMIT_DIGITS
                    && exponent.substring(at).compareTo(EXPONENT_LIMIT_TEXT) > 0);
    }

    /// Orders two values at least one of which is too large to write out.
    ///
    /// By sign, then by the power of ten the first significant digit sits at,
    /// then by the digits. No expansion, and exact for any exponent a long
    /// holds -- which is every exponent anything can produce.
    private int compareBeyondExpansion(String left, String right) {
        boolean leftNegative = left.charAt(0) == '-';
        boolean rightNegative = right.charAt(0) == '-';
        String leftDigits = significantOf(left);
        String rightDigits = significantOf(right);
        if (leftDigits.length() == 0 || rightDigits.length() == 0) {
            // One of them is zero, and zero is below every positive value and
            // above every negative one.
            if (leftDigits.length() == rightDigits.length()) {
                return 0;
            }
            if (leftDigits.length() == 0) {
                return rightNegative ? 1 : -1;
            }
            return leftNegative ? -1 : 1;
        }
        if (leftNegative != rightNegative) {
            return leftNegative ? -1 : 1;
        }
        int magnitude = compareIntegerText(orderOf(left), orderOf(right));
        if (magnitude == 0) {
            magnitude = compareFraction(leftDigits, rightDigits);
        }
        return leftNegative ? -magnitude : magnitude;
    }

    /// The power of ten the first significant digit of this value sits at.
    ///
    /// As a signed digit string rather than a number, because the exponent it
    /// is built from has no bound: clamping one to any ceiling makes every
    /// value past that ceiling equal, which is the defect this replaced.
    private String orderOf(String text) {
        int marker = exponentMarker(text);
        String exponent = marker < 0 ? "0" : text.substring(marker + 1);
        String mantissa = marker < 0 ? text : text.substring(0, marker);
        int at = 0;
        if (at < mantissa.length()
                && (mantissa.charAt(at) == '-' || mantissa.charAt(at) == '+')) {
            at++;
        }
        int point = mantissa.indexOf('.');
        int whole = (point < 0 ? mantissa.length() : point) - at;
        int leading = 0;
        for (int i = at; i < mantissa.length(); i++) {
            char c = mantissa.charAt(i);
            if (c == '.') {
                continue;
            }
            if (Character.digit(c, 10) != 0) {
                break;
            }
            leading++;
        }
        return addSmall(exponent, whole - leading - 1);
    }

    /// Adds a small whole number to an integer written as text.
    ///
    /// Digit by digit, so an exponent of any length keeps every digit it was
    /// written with. `delta` is the handful of places the mantissa shifts the
    /// point by, never more than the mantissa is long.
    private String addSmall(String text, int delta) {
        String value = text.trim();
        boolean negative = value.length() > 0 && value.charAt(0) == '-';
        if (value.length() > 0
                && (value.charAt(0) == '-' || value.charAt(0) == '+')) {
            value = value.substring(1);
        }
        int at = 0;
        while (at < value.length() - 1 && Character.digit(value.charAt(at), 10) == 0) {
            at++;
        }
        value = value.substring(at);
        // Signed arithmetic, one step at a time: the magnitudes are what the
        // digits hold, and delta is small enough that stepping is exact and
        // bounded by the mantissa's length.
        int steps = delta < 0 ? -delta : delta;
        boolean up = (delta > 0) != negative;
        for (int i = 0; i < steps; i++) {
            if (up) {
                value = incrementDigits(value);
            } else if (isZeroDigits(value)) {
                negative = !negative;
                value = "1";
            } else {
                value = decrementDigits(value);
            }
        }
        if (isZeroDigits(value)) {
            negative = false;
        }
        return negative ? "-" + value : value;
    }

    /// Adds one to a string of digits.
    private String incrementDigits(String digits) {
        StringBuilder out = new StringBuilder(digits);
        int at = out.length() - 1;
        while (at >= 0) {
            int digit = Character.digit(out.charAt(at), 10) + 1;
            if (digit < 10) {
                out.setCharAt(at, (char) ('0' + digit));
                return out.toString();
            }
            out.setCharAt(at, '0');
            at--;
        }
        return "1" + out.toString();
    }

    /// Subtracts one from a string of digits that is not zero.
    private String decrementDigits(String digits) {
        StringBuilder out = new StringBuilder(digits);
        int at = out.length() - 1;
        while (at >= 0) {
            int digit = Character.digit(out.charAt(at), 10);
            if (digit > 0) {
                out.setCharAt(at, (char) ('0' + (digit - 1)));
                break;
            }
            out.setCharAt(at, '9');
            at--;
        }
        int lead = 0;
        while (lead < out.length() - 1 && out.charAt(lead) == '0') {
            lead++;
        }
        // Through the String: StringBuilder.substring is not in the class
        // library Codename One compiles against -- CLDC11 has no such method
        // and the VM's copy returns a StringBuilder -- so this is a compile
        // error on the framework build even though it is fine on the JDK.
        return out.toString().substring(lead);
    }

    /// Whether every character is a zero digit.
    private boolean isZeroDigits(String digits) {
        for (int i = 0; i < digits.length(); i++) {
            if (Character.digit(digits.charAt(i), 10) != 0) {
                return false;
            }
        }
        return true;
    }

    /// Compares two whole numbers written as signed text, at any length.
    private int compareIntegerText(String left, String right) {
        boolean leftNegative = left.length() > 0 && left.charAt(0) == '-';
        boolean rightNegative = right.length() > 0 && right.charAt(0) == '-';
        String leftDigits = leftNegative ? left.substring(1) : left;
        String rightDigits = rightNegative ? right.substring(1) : right;
        if (isZeroDigits(leftDigits) && isZeroDigits(rightDigits)) {
            return 0;
        }
        if (leftNegative != rightNegative) {
            return leftNegative ? -1 : 1;
        }
        int magnitude = compareDigits(leftDigits, rightDigits);
        return leftNegative ? -magnitude : magnitude;
    }

    /// The significant digits of a value, ignoring sign, point and exponent.
    private String significantOf(String text) {
        int marker = exponentMarker(text);
        String mantissa = marker < 0 ? text : text.substring(0, marker);
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < mantissa.length(); i++) {
            char c = mantissa.charAt(i);
            if (Character.digit(c, 10) >= 0) {
                if (digits.length() > 0 || Character.digit(c, 10) != 0) {
                    digits.append(c);
                }
            }
        }
        int end = digits.length();
        while (end > 0 && Character.digit(digits.charAt(end - 1), 10) == 0) {
            end--;
        }
        return digits.toString().substring(0, end);
    }

    /// Where the exponent begins, or -1.
    private int exponentMarker(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == 'e' || c == 'E') {
                return i;
            }
        }
        return -1;
    }

    /// Rewrites a number in exponent form as plain digits.
    ///
    /// - `text`: a value [#isNumeric] already accepted
    ///
    /// #### Returns
    ///
    /// the same value with the point moved and the exponent gone
    private String withoutExponent(String text) {
        int marker = exponentMarker(text);
        if (marker < 0) {
            return text;
        }
        int exponent = parseExponent(text.substring(marker + 1));
        String mantissa = text.substring(0, marker);
        String sign = "";
        if (mantissa.length() > 0
                && (mantissa.charAt(0) == '-' || mantissa.charAt(0) == '+')) {
            sign = mantissa.charAt(0) == '-' ? "-" : "";
            mantissa = mantissa.substring(1);
        }
        int point = mantissa.indexOf('.');
        String digits = point < 0 ? mantissa
                : mantissa.substring(0, point) + mantissa.substring(point + 1);
        int pointAt = (point < 0 ? mantissa.length() : point) + exponent;
        StringBuilder out = new StringBuilder();
        if (pointAt <= 0) {
            out.append("0.");
            for (int i = 0; i < -pointAt; i++) {
                out.append('0');
            }
            out.append(digits);
        } else if (pointAt >= digits.length()) {
            out.append(digits);
            for (int i = digits.length(); i < pointAt; i++) {
                out.append('0');
            }
        } else {
            out.append(digits.substring(0, pointAt));
            out.append('.');
            out.append(digits.substring(pointAt));
        }
        return sign + out.toString();
    }

    /// Reads a small exponent's digits, which [#isNumeric] has already checked.
    ///
    /// Only reached for an exponent inside the expansion limit, so it fits an
    /// int by construction. Every larger one is compared as text instead --
    /// reading it into a number at all is what made 1e1000020 and 1e1000029
    /// the same value, whether by stopping partway through the digits or by
    /// clamping them to a shared ceiling.
    private int parseExponent(String text) {
        boolean negative = text.length() > 0 && text.charAt(0) == '-';
        int at = negative || (text.length() > 0 && text.charAt(0) == '+')
                ? 1 : 0;
        int value = 0;
        for (int i = at; i < text.length(); i++) {
            value = value * 10 + Character.digit(text.charAt(i), 10);
        }
        return negative ? -value : value;
    }

    /// The largest exponent worth writing out as digits.
    ///
    /// Ten thousand places is already far past anything a document carries,
    /// and past it the digits are compared instead of expanded.
    private static final String EXPONENT_LIMIT_TEXT = "10000";

    /// How long that limit is, so the test can be made on length first.
    private static final int EXPONENT_LIMIT_DIGITS = 5;

    /// Compares two numbers written without an exponent, digit by digit.
    ///
    /// - `left`: a number with no exponent
    ///
    /// - `right`: a number with no exponent
    ///
    /// #### Returns
    ///
    /// negative, zero or positive
    private int compareFixedPoint(String left, String right) {
        String leftWhole = wholePart(left);
        String rightWhole = wholePart(right);
        String leftFraction = fractionPart(left);
        String rightFraction = fractionPart(right);
        boolean leftNegative = left.charAt(0) == '-'
                && !(leftWhole.length() == 0 && leftFraction.length() == 0);
        boolean rightNegative = right.charAt(0) == '-'
                && !(rightWhole.length() == 0 && rightFraction.length() == 0);
        if (leftNegative != rightNegative) {
            return leftNegative ? -1 : 1;
        }
        int magnitude = compareDigits(leftWhole, rightWhole);
        if (magnitude == 0) {
            magnitude = compareFraction(leftFraction, rightFraction);
        }
        return leftNegative ? -magnitude : magnitude;
    }

    /// Compares two whole-number digit strings by value.
    ///
    /// Digit by digit rather than by comparing the strings: isNumeric accepts
    /// whatever Character.isDigit does, so a value can be written in
    /// Arabic-Indic or any other decimal script, and those code points do not
    /// sort in numeric order against ASCII. Integer.parseInt read them as
    /// numbers, and so does this.
    private int compareDigits(String left, String right) {
        if (left.length() != right.length()) {
            return left.length() < right.length() ? -1 : 1;
        }
        for (int i = 0; i < left.length(); i++) {
            int l = Character.digit(left.charAt(i), 10);
            int r = Character.digit(right.charAt(i), 10);
            if (l != r) {
                return l < r ? -1 : 1;
            }
        }
        return 0;
    }

    /// Compares two fractional digit strings, padding the shorter with zeros.
    private int compareFraction(String left, String right) {
        int length = left.length() > right.length()
                ? left.length() : right.length();
        for (int i = 0; i < length; i++) {
            int l = i < left.length() ? Character.digit(left.charAt(i), 10) : 0;
            int r = i < right.length() ? Character.digit(right.charAt(i), 10) : 0;
            if (l != r) {
                return l < r ? -1 : 1;
            }
        }
        return 0;
    }

    /// The digits before the point, without sign or leading zeros.
    private String wholePart(String text) {
        int at = 0;
        if (at < text.length()
                && (text.charAt(at) == '-' || text.charAt(at) == '+')) {
            at++;
        }
        int point = text.indexOf('.');
        int end = point < 0 ? text.length() : point;
        while (at < end && Character.digit(text.charAt(at), 10) == 0) {
            at++;
        }
        return text.substring(at, end);
    }

    /// The digits after the point, without trailing zeros.
    private String fractionPart(String text) {
        int point = text.indexOf('.');
        if (point < 0) {
            return "";
        }
        int end = text.length();
        while (end > point + 1 && Character.digit(text.charAt(end - 1), 10) == 0) {
            end--;
        }
        return text.substring(point + 1, end);
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
