/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.spreadsheet;

import java.util.HashMap;

/// What the formula editor shows about a function: its arguments, as Excel names them, and
/// a sentence on what it does. A line per function, `NAME|arguments|description`, kept as
/// code so that an application that never edits a formula strips it with the editor.
final class FormulaHelp {

    private FormulaHelp() {
    }

    // an array, not two constants, so the text is not copied into every class that reads it
    private static final String[] PARTS = { ""
            + "ABS|number|Returns the absolute value of a number.\n"
            + "ACOS|number|Returns the arccosine of a number, in radians.\n"
            + "ACOSH|number|Returns the inverse hyperbolic cosine of a number.\n"
            + "ACOT|number|Returns the arccotangent of a number, in radians.\n"
            + "ACOTH|number|Returns the inverse hyperbolic cotangent of a number.\n"
            + "ADDRESS|row_num, column_num, [abs_num], [a1], [sheet_text]|Creates a cell reference as text from row and column numbers.\n"
            + "AGGREGATE|function_num, options, ref1, [ref2], ...|Returns an aggregate such as SUM or LARGE over a list, optionally ignoring errors and hidden rows.\n"
            + "AND|logical1, [logical2], ...|Returns TRUE if all of its arguments are TRUE.\n"
            + "ARABIC|text|Converts a Roman numeral to an Arabic number.\n"
            + "AREAS|reference|Returns the number of areas in a reference.\n"
            + "ASIN|number|Returns the arcsine of a number, in radians.\n"
            + "ASINH|number|Returns the inverse hyperbolic sine of a number.\n"
            + "ATAN|number|Returns the arctangent of a number, in radians.\n"
            + "ATAN2|x_num, y_num|Returns the arctangent from x and y coordinates, in radians.\n"
            + "ATANH|number|Returns the inverse hyperbolic tangent of a number.\n"
            + "AVEDEV|number1, [number2], ...|Returns the average of the absolute deviations of data points from their mean.\n"
            + "AVERAGE|number1, [number2], ...|Returns the average of its arguments.\n"
            + "AVERAGEA|value1, [value2], ...|Returns the average of its arguments, counting text as 0 and TRUE as 1.\n"
            + "AVERAGEIF|range, criteria, [average_range]|Returns the average of the cells that meet a condition.\n"
            + "AVERAGEIFS|average_range, criteria_range1, criteria1, [criteria_range2, criteria2], ...|Returns the average of the cells that meet all of several conditions.\n"
            + "BASE|number, radix, [min_length]|Converts a number into text in the given base.\n"
            + "BETA.DIST|x, alpha, beta, cumulative, [A], [B]|Returns the beta distribution.\n"
            + "BETA.INV|probability, alpha, beta, [A], [B]|Returns the inverse of the cumulative beta distribution.\n"
            + "BETADIST|x, alpha, beta, [A], [B]|Returns the cumulative beta distribution; the same as BETA.DIST with cumulative TRUE.\n"
            + "BETAINV|probability, alpha, beta, [A], [B]|Returns the inverse of the cumulative beta distribution; the same as BETA.INV.\n"
            + "BIN2DEC|number|Converts a binary number to decimal.\n"
            + "BIN2HEX|number, [places]|Converts a binary number to hexadecimal.\n"
            + "BIN2OCT|number, [places]|Converts a binary number to octal.\n"
            + "BINOM.DIST|number_s, trials, probability_s, cumulative|Returns the binomial distribution probability.\n"
            + "BINOM.DIST.RANGE|trials, probability_s, number_s, [number_s2]|Returns the probability of a trial result using a binomial distribution.\n"
            + "BINOM.INV|trials, probability_s, alpha|Returns the smallest value for which the cumulative binomial distribution reaches a criterion.\n"
            + "BINOMDIST|number_s, trials, probability_s, cumulative|Returns the binomial distribution probability; the same as BINOM.DIST.\n"
            + "BITAND|number1, number2|Returns a bitwise AND of two numbers.\n"
            + "BITLSHIFT|number, shift_amount|Returns a number shifted left by the given number of bits.\n"
            + "BITOR|number1, number2|Returns a bitwise OR of two numbers.\n"
            + "BITRSHIFT|number, shift_amount|Returns a number shifted right by the given number of bits.\n"
            + "BITXOR|number1, number2|Returns a bitwise exclusive OR of two numbers.\n"
            + "CEILING|number, significance|Rounds a number up to the nearest multiple of significance.\n"
            + "CEILING.MATH|number, [significance], [mode]|Rounds a number up to the nearest integer or multiple of significance.\n"
            + "CEILING.PRECISE|number, [significance]|Rounds a number up to the nearest integer or multiple of significance, regardless of sign.\n"
            + "CHAR|number|Returns the character for a code number.\n"
            + "CHIDIST|x, deg_freedom|Returns the right-tailed chi-squared distribution; the same as CHISQ.DIST.RT.\n"
            + "CHIINV|probability, deg_freedom|Returns the inverse of the right-tailed chi-squared distribution; the same as CHISQ.INV.RT.\n"
            + "CHISQ.DIST|x, deg_freedom, cumulative|Returns the left-tailed chi-squared distribution.\n"
            + "CHISQ.DIST.RT|x, deg_freedom|Returns the right-tailed chi-squared distribution.\n"
            + "CHISQ.INV|probability, deg_freedom|Returns the inverse of the left-tailed chi-squared distribution.\n"
            + "CHISQ.INV.RT|probability, deg_freedom|Returns the inverse of the right-tailed chi-squared distribution.\n"
            + "CHISQ.TEST|actual_range, expected_range|Returns the chi-squared test for independence.\n"
            + "CHITEST|actual_range, expected_range|Returns the chi-squared test for independence; the same as CHISQ.TEST.\n"
            + "CHOOSE|index_num, value1, [value2], ...|Chooses a value from a list of values by index number.\n"
            + "CHOOSECOLS|array, col_num1, [col_num2], ...|Returns the specified columns from an array.\n"
            + "CHOOSEROWS|array, row_num1, [row_num2], ...|Returns the specified rows from an array.\n"
            + "CLEAN|text|Removes all nonprintable characters from text.\n"
            + "CODE|text|Returns the numeric code of the first character in text.\n"
            + "COLUMN|[reference]|Returns the column number of a reference.\n"
            + "COLUMNS|array|Returns the number of columns in a reference or array.\n"
            + "COMBIN|number, number_chosen|Returns the number of combinations for a given number of items.\n"
            + "COMBINA|number, number_chosen|Returns the number of combinations with repetitions for a given number of items.\n"
            + "CONCAT|text1, [text2], ...|Joins text from several ranges or strings.\n"
            + "CONCATENATE|text1, [text2], ...|Joins several text strings into one string.\n"
            + "CONFIDENCE|alpha, standard_dev, size|Returns the confidence interval for a population mean; the same as CONFIDENCE.NORM.\n"
            + "CONFIDENCE.NORM|alpha, standard_dev, size|Returns the confidence interval for a population mean using a normal distribution.\n"
            + "CONFIDENCE.T|alpha, standard_dev, size|Returns the confidence interval for a population mean using a Student's t distribution.\n"
            + "CONVERT|number, from_unit, to_unit|Converts a number from one measurement system to another.\n"
            + "CORREL|array1, array2|Returns the correlation coefficient between two data sets.\n"
            + "COS|number|Returns the cosine of an angle given in radians.\n"
            + "COSH|number|Returns the hyperbolic cosine of a number.\n"
            + "COT|number|Returns the cotangent of an angle given in radians.\n"
            + "COTH|number|Returns the hyperbolic cotangent of a number.\n"
            + "COUNT|value1, [value2], ...|Counts the cells that contain numbers.\n"
            + "COUNTA|value1, [value2], ...|Counts the cells that are not empty.\n"
            + "COUNTBLANK|range|Counts the empty cells in a range.\n"
            + "COUNTIF|range, criteria|Counts the cells that meet a condition.\n"
            + "COUNTIFS|criteria_range1, criteria1, [criteria_range2, criteria2], ...|Counts the cells that meet all of several conditions.\n"
            + "COVAR|array1, array2|Returns the population covariance; the same as COVARIANCE.P.\n"
            + "COVARIANCE.P|array1, array2|Returns the population covariance, the average of the products of paired deviations.\n"
            + "COVARIANCE.S|array1, array2|Returns the sample covariance, the average of the products of paired deviations.\n"
            + "CRITBINOM|trials, probability_s, alpha|Returns the smallest value whose cumulative binomial distribution reaches a criterion; same as BINOM.INV.\n"
            + "CSC|number|Returns the cosecant of an angle given in radians.\n"
            + "CSCH|number|Returns the hyperbolic cosecant of an angle given in radians.\n"
            + "CUMIPMT|rate, nper, pv, start_period, end_period, type|Returns the cumulative interest paid between two periods.\n"
            + "CUMPRINC|rate, nper, pv, start_period, end_period, type|Returns the cumulative principal paid on a loan between two periods.\n"
            + "DATE|year, month, day|Returns the serial number of a date from year, month and day.\n"
            + "DATEDIF|start_date, end_date, unit|Returns the difference between two dates in days, months or years.\n"
            + "DATEVALUE|date_text|Converts a date in text form to a serial number.\n"
            + "DAVERAGE|database, field, criteria|Returns the average of the database entries that match the criteria.\n"
            + "DAY|serial_number|Returns the day of the month of a date.\n"
            + "DAYS|end_date, start_date|Returns the number of days between two dates.\n"
            + "DAYS360|start_date, end_date, [method]|Returns the number of days between two dates based on a 360-day year.\n"
            + "DB|cost, salvage, life, period, [month]|Returns the depreciation of an asset using the fixed-declining balance method.\n"
            + "DCOUNT|database, field, criteria|Counts the cells with numbers in the database entries that match the criteria.\n"
            + "DCOUNTA|database, field, criteria|Counts the nonblank cells in the database entries that match the criteria.\n"
            + "DDB|cost, salvage, life, period, [factor]|Returns the depreciation of an asset using the double-declining balance method.\n"
            + "DEC2BIN|number, [places]|Converts a decimal number to binary.\n"
            + "DEC2HEX|number, [places]|Converts a decimal number to hexadecimal.\n"
            + "DEC2OCT|number, [places]|Converts a decimal number to octal.\n"
            + "DECIMAL|text, radix|Converts a text representation of a number in a given base into a decimal number.\n"
            + "DEGREES|angle|Converts radians to degrees.\n"
            + "DELTA|number1, [number2]|Tests whether two numbers are equal.\n"
            + "DEVSQ|number1, [number2], ...|Returns the sum of squares of deviations from the sample mean.\n"
            + "DGET|database, field, criteria|Extracts the single database entry that matches the criteria.\n"
            + "DMAX|database, field, criteria|Returns the maximum value among the database entries that match the criteria.\n"
            + "DMIN|database, field, criteria|Returns the minimum value among the database entries that match the criteria.\n"
            + "DOLLAR|number, [decimals]|Converts a number to text in currency format.\n"
            + "DOLLARDE|fractional_dollar, fraction|Converts a dollar price expressed as a fraction into a decimal number.\n"
            + "DOLLARFR|decimal_dollar, fraction|Converts a dollar price expressed as a decimal number into a fraction.\n"
            + "DPRODUCT|database, field, criteria|Multiplies the values in the database entries that match the criteria.\n"
            + "DROP|array, rows, [columns]|Excludes a number of rows or columns from the start or end of an array.\n"
            + "DSTDEV|database, field, criteria|Estimates the standard deviation of a sample from the matching database entries.\n"
            + "DSTDEVP|database, field, criteria|Returns the population standard deviation of the matching database entries.\n"
            + "DSUM|database, field, criteria|Adds the numbers in the database entries that match the criteria.\n"
            + "DVAR|database, field, criteria|Estimates the variance of a sample from the matching database entries.\n"
            + "DVARP|database, field, criteria|Returns the population variance of the matching database entries.\n"
            + "EDATE|start_date, months|Returns the date a given number of months before or after a date.\n"
            + "EFFECT|nominal_rate, npery|Returns the effective annual interest rate.\n"
            + "EOMONTH|start_date, months|Returns the last day of the month a given number of months away.\n"
            + "ERF|lower_limit, [upper_limit]|Returns the error function.\n"
            + "ERF.PRECISE|x|Returns the error function integrated from 0 to x.\n"
            + "ERFC|x|Returns the complementary error function.\n"
            + "ERFC.PRECISE|x|Returns the complementary error function integrated from x to infinity.\n"
            + "ERROR.TYPE|error_val|Returns a number matching an error type.\n"
            + "EVEN|number|Rounds a number up to the nearest even integer.\n"
            + "EXACT|text1, text2|Checks whether two text strings are exactly the same.\n"
            + "EXP|number|Returns e raised to the power of a number.\n"
            + "EXPAND|array, rows, [columns], [pad_with]|Expands an array to the given dimensions, padding with a value.\n"
            + "EXPON.DIST|x, lambda, cumulative|Returns the exponential distribution.\n"
            + "EXPONDIST|x, lambda, cumulative|Returns the exponential distribution; the same as EXPON.DIST.\n"
            + "F.DIST|x, deg_freedom1, deg_freedom2, cumulative|Returns the left-tailed F probability distribution.\n"
            + "F.DIST.RT|x, deg_freedom1, deg_freedom2|Returns the right-tailed F probability distribution.\n"
            + "F.INV|probability, deg_freedom1, deg_freedom2|Returns the inverse of the left-tailed F probability distribution.\n"
            + "F.INV.RT|probability, deg_freedom1, deg_freedom2|Returns the inverse of the right-tailed F probability distribution.\n"
            + "F.TEST|array1, array2|Returns the result of an F-test, the two-tailed probability that the variances differ.\n"
            + "FACT|number|Returns the factorial of a number.\n"
            + "FACTDOUBLE|number|Returns the double factorial of a number.\n"
            + "FALSE||Returns the logical value FALSE.\n"
            + "FDIST|x, deg_freedom1, deg_freedom2|Returns the right-tailed F probability distribution; the same as F.DIST.RT.\n"
            + "FILTER|array, include, [if_empty]|Filters a range or array based on a condition.\n"
            + "FIND|find_text, within_text, [start_num]|Finds one text string inside another, case-sensitive.\n"
            + "FINV|probability, deg_freedom1, deg_freedom2|Returns the inverse of the right-tailed F distribution; the same as F.INV.RT.\n"
            + "FISHER|x|Returns the Fisher transformation.\n"
            + "FISHERINV|y|Returns the inverse of the Fisher transformation.\n"
            + "FIXED|number, [decimals], [no_commas]|Formats a number as text with a fixed number of decimals.\n"
            + "FLOOR|number, significance|Rounds a number down to the nearest multiple of significance.\n"
            + "FLOOR.MATH|number, [significance], [mode]|Rounds a number down to the nearest integer or multiple of significance.\n"
            + "FLOOR.PRECISE|number, [significance]|Rounds a number down to the nearest integer or multiple of significance, regardless of sign.\n"
            + "FORECAST|x, known_y's, known_x's|Predicts a value along a linear trend; the same as FORECAST.LINEAR.\n"
            + "FORECAST.LINEAR|x, known_y's, known_x's|Predicts a future value along a linear trend from existing values.\n"
            + "FORMULATEXT|reference|Returns the formula in a cell as text.\n"
            + "FREQUENCY|data_array, bins_array|Returns a frequency distribution as a vertical array.\n"
            + "FTEST|array1, array2|Returns the result of an F-test; the same as F.TEST.\n"
            + "FV|rate, nper, pmt, [pv], [type]|Returns the future value of an investment.\n"
            + "FVSCHEDULE|principal, schedule|Returns the future value of a principal after a series of compound interest rates.\n"
            + "GAMMA|x|Returns the gamma function value.\n"
            + "GAMMA.DIST|x, alpha, beta, cumulative|Returns the gamma distribution.\n"
            + "GAMMA.INV|probability, alpha, beta|Returns the inverse of the gamma cumulative distribution.\n"
            + "GAMMADIST|x, alpha, beta, cumulative|Returns the gamma distribution; the same as GAMMA.DIST.\n"
            + "GAMMAINV|probability, alpha, beta|Returns the inverse of the gamma cumulative distribution; the same as GAMMA.INV.\n"
            + "GAMMALN|x|Returns the natural logarithm of the gamma function.\n"
            + "GAMMALN.PRECISE|x|Returns the natural logarithm of the gamma function.\n"
            + "GAUSS|z|Returns 0.5 less than the standard normal cumulative distribution.\n"
            + "GCD|number1, [number2], ...|Returns the greatest common divisor.\n"
            + "GEOMEAN|number1, [number2], ...|Returns the geometric mean.\n"
            + "GESTEP|number, [step]|Tests whether a number is greater than a threshold value.\n"
            + "HARMEAN|number1, [number2], ...|Returns the harmonic mean.\n"
            + "HEX2BIN|number, [places]|Converts a hexadecimal number to binary.\n"
            + "HEX2DEC|number|Converts a hexadecimal number to decimal.\n"
            + "HEX2OCT|number, [places]|Converts a hexadecimal number to octal.\n"
            + "HLOOKUP|lookup_value, table_array, row_index_num, [range_lookup]|Looks for a value in the top row of a table and returns a value in the same column from another row.\n"
            + "HOUR|serial_number|Returns the hour of a time value.\n"
            + "HSTACK|array1, [array2], ...|Appends arrays horizontally and in sequence into one array.\n"
            + "HYPGEOM.DIST|sample_s, number_sample, population_s, number_pop, cumulative|Returns the hypergeometric distribution.\n"
            + "HYPGEOMDIST|sample_s, number_sample, population_s, number_pop|Returns the hypergeometric distribution; like HYPGEOM.DIST with cumulative FALSE.\n"
            + "IF|logical_test, [value_if_true], [value_if_false]|Returns one value if a condition is TRUE and another if it is FALSE.\n"
            + "IFERROR|value, value_if_error|Returns a given value if a formula results in an error, otherwise the formula result.\n"
            + "IFNA|value, value_if_na|Returns a given value if a formula results in #N/A, otherwise the formula result.\n"
            + "IFS|logical_test1, value_if_true1, [logical_test2, value_if_true2], ...|Returns the value for the first condition that is TRUE.\n"
            + "INDEX|array, row_num, [column_num], [area_num]|Returns the value at a given position in a range or array.\n"
            + "INDIRECT|ref_text, [a1]|Returns the reference specified by a text string.\n"
            + "INT|number|Rounds a number down to the nearest integer.\n"
            + "INTERCEPT|known_y's, known_x's|Returns the intercept of the linear regression line.\n"
            + "IPMT|rate, per, nper, pv, [fv], [type]|Returns the interest payment for a given period of a loan.\n"
            + "IRR|values, [guess]|Returns the internal rate of return for a series of cash flows.\n"
            + "ISBLANK|value|Returns TRUE if the value is empty.\n"
            + "ISERR|value|Returns TRUE if the value is any error except #N/A.\n"
            + "ISERROR|value|Returns TRUE if the value is any error.\n"
            + "ISEVEN|number|Returns TRUE if the number is even.\n"
            + "ISFORMULA|reference|Returns TRUE if the reference is to a cell containing a formula.\n"
            + "ISLOGICAL|value|Returns TRUE if the value is a logical value.\n"
            + "ISNA|value|Returns TRUE if the value is the #N/A error.\n"
            + "ISNONTEXT|value|Returns TRUE if the value is not text.\n"
            + "ISNUMBER|value|Returns TRUE if the value is a number.\n"
            + "ISO.CEILING|number, [significance]|Rounds a number up to the nearest integer or multiple of significance, regardless of sign.\n"
            + "ISODD|number|Returns TRUE if the number is odd.\n"
            + "ISOWEEKNUM|date|Returns the ISO week number of the year for a date.\n"
            + "ISPMT|rate, per, nper, pv|Returns the interest paid during a specific period of an investment.\n"
            + "ISREF|value|Returns TRUE if the value is a reference.\n"
            + "ISTEXT|value|Returns TRUE if the value is text.\n"
            + "KURT|number1, [number2], ...|Returns the kurtosis of a data set.\n"
            + "LARGE|array, k|Returns the k-th largest value in a data set.\n"
            + "LCM|number1, [number2], ...|Returns the least common multiple.\n"
            + "LEFT|text, [num_chars]|Returns the leftmost characters of a text string.\n"
            + "",
            ""
            + "LEN|text|Returns the number of characters in a text string.\n"
            + "LET|name1, name_value1, calculation_or_name2, ...|Assigns names to calculation results and returns the final calculation.\n"
            + "LN|number|Returns the natural logarithm of a number.\n"
            + "LOG|number, [base]|Returns the logarithm of a number to a specified base.\n"
            + "LOG10|number|Returns the base-10 logarithm of a number.\n"
            + "LOGINV|probability, mean, standard_dev|Returns the inverse of the lognormal distribution; the same as LOGNORM.INV.\n"
            + "LOGNORM.DIST|x, mean, standard_dev, cumulative|Returns the lognormal distribution.\n"
            + "LOGNORM.INV|probability, mean, standard_dev|Returns the inverse of the lognormal cumulative distribution.\n"
            + "LOGNORMDIST|x, mean, standard_dev|Returns the cumulative lognormal distribution; like LOGNORM.DIST with cumulative TRUE.\n"
            + "LOOKUP|lookup_value, lookup_vector, [result_vector]|Looks up a value in a one-row or one-column range and returns the matching value.\n"
            + "LOWER|text|Converts text to lowercase.\n"
            + "MATCH|lookup_value, lookup_array, [match_type]|Returns the relative position of an item in a range.\n"
            + "MAX|number1, [number2], ...|Returns the largest value in a set of values.\n"
            + "MAXA|value1, [value2], ...|Returns the largest value, counting text and logical values.\n"
            + "MAXIFS|max_range, criteria_range1, criteria1, [criteria_range2, criteria2], ...|Returns the largest value among the cells that meet all conditions.\n"
            + "MDETERM|array|Returns the matrix determinant of an array.\n"
            + "MEDIAN|number1, [number2], ...|Returns the median of the given numbers.\n"
            + "MID|text, start_num, num_chars|Returns characters from the middle of a text string.\n"
            + "MIN|number1, [number2], ...|Returns the smallest value in a set of values.\n"
            + "MINA|value1, [value2], ...|Returns the smallest value, counting text and logical values.\n"
            + "MINIFS|min_range, criteria_range1, criteria1, [criteria_range2, criteria2], ...|Returns the smallest value among the cells that meet all conditions.\n"
            + "MINUTE|serial_number|Returns the minute of a time value.\n"
            + "MINVERSE|array|Returns the matrix inverse of an array.\n"
            + "MIRR|values, finance_rate, reinvest_rate|Returns the modified internal rate of return for a series of cash flows.\n"
            + "MMULT|array1, array2|Returns the matrix product of two arrays.\n"
            + "MOD|number, divisor|Returns the remainder after division.\n"
            + "MODE|number1, [number2], ...|Returns the most common value in a data set; the same as MODE.SNGL.\n"
            + "MODE.MULT|number1, [number2], ...|Returns a vertical array of the most frequently occurring values.\n"
            + "MODE.SNGL|number1, [number2], ...|Returns the most common value in a data set.\n"
            + "MONTH|serial_number|Returns the month of a date.\n"
            + "MROUND|number, multiple|Returns a number rounded to the desired multiple.\n"
            + "MULTINOMIAL|number1, [number2], ...|Returns the multinomial of a set of numbers.\n"
            + "MUNIT|dimension|Returns the unit matrix for the specified dimension.\n"
            + "N|value|Converts a value to a number.\n"
            + "NA||Returns the error value #N/A.\n"
            + "NEGBINOM.DIST|number_f, number_s, probability_s, cumulative|Returns the negative binomial distribution.\n"
            + "NEGBINOMDIST|number_f, number_s, probability_s|Returns the negative binomial distribution; like NEGBINOM.DIST with cumulative FALSE.\n"
            + "NETWORKDAYS|start_date, end_date, [holidays]|Returns the number of whole working days between two dates.\n"
            + "NETWORKDAYS.INTL|start_date, end_date, [weekend], [holidays]|Returns the number of whole working days between two dates with custom weekends.\n"
            + "NOMINAL|effect_rate, npery|Returns the annual nominal interest rate.\n"
            + "NORM.DIST|x, mean, standard_dev, cumulative|Returns the normal distribution.\n"
            + "NORM.INV|probability, mean, standard_dev|Returns the inverse of the normal cumulative distribution.\n"
            + "NORM.S.DIST|z, cumulative|Returns the standard normal distribution.\n"
            + "NORM.S.INV|probability|Returns the inverse of the standard normal cumulative distribution.\n"
            + "NORMDIST|x, mean, standard_dev, cumulative|Returns the normal distribution; the same as NORM.DIST.\n"
            + "NORMINV|probability, mean, standard_dev|Returns the inverse of the normal cumulative distribution; the same as NORM.INV.\n"
            + "NORMSDIST|z|Returns the standard normal cumulative distribution; like NORM.S.DIST with cumulative TRUE.\n"
            + "NORMSINV|probability|Returns the inverse of the standard normal cumulative distribution; the same as NORM.S.INV.\n"
            + "NOT|logical|Reverses the logic of its argument.\n"
            + "NOW||Returns the current date and time.\n"
            + "NPER|rate, pmt, pv, [fv], [type]|Returns the number of periods for an investment.\n"
            + "NPV|rate, value1, [value2], ...|Returns the net present value of an investment from a discount rate and future cash flows.\n"
            + "NUMBERVALUE|text, [decimal_separator], [group_separator]|Converts text to a number independently of locale.\n"
            + "OCT2BIN|number, [places]|Converts an octal number to binary.\n"
            + "OCT2DEC|number|Converts an octal number to decimal.\n"
            + "OCT2HEX|number, [places]|Converts an octal number to hexadecimal.\n"
            + "ODD|number|Rounds a number up to the nearest odd integer.\n"
            + "OFFSET|reference, rows, cols, [height], [width]|Returns a reference offset from a given reference.\n"
            + "OR|logical1, [logical2], ...|Returns TRUE if any argument is TRUE.\n"
            + "PDURATION|rate, pv, fv|Returns the number of periods an investment needs to reach a value.\n"
            + "PEARSON|array1, array2|Returns the Pearson product-moment correlation coefficient.\n"
            + "PERCENTILE|array, k|Returns the k-th percentile of values in a range; the same as PERCENTILE.INC.\n"
            + "PERCENTILE.EXC|array, k|Returns the k-th percentile of values in a range, exclusive of 0 and 1.\n"
            + "PERCENTILE.INC|array, k|Returns the k-th percentile of values in a range, inclusive of 0 and 1.\n"
            + "PERCENTRANK|array, x, [significance]|Returns the percentage rank of a value in a data set; the same as PERCENTRANK.INC.\n"
            + "PERCENTRANK.EXC|array, x, [significance]|Returns the rank of a value in a data set as a percentage, exclusive of 0 and 1.\n"
            + "PERCENTRANK.INC|array, x, [significance]|Returns the rank of a value in a data set as a percentage, inclusive of 0 and 1.\n"
            + "PERMUT|number, number_chosen|Returns the number of permutations for a given number of objects.\n"
            + "PERMUTATIONA|number, number_chosen|Returns the number of permutations with repetitions for a given number of objects.\n"
            + "PHI|x|Returns the density function for the standard normal distribution.\n"
            + "PI||Returns the value of pi.\n"
            + "PMT|rate, nper, pv, [fv], [type]|Returns the periodic payment for a loan.\n"
            + "POISSON|x, mean, cumulative|Returns the Poisson distribution; the same as POISSON.DIST.\n"
            + "POISSON.DIST|x, mean, cumulative|Returns the Poisson distribution.\n"
            + "POWER|number, power|Returns the result of a number raised to a power.\n"
            + "PPMT|rate, per, nper, pv, [fv], [type]|Returns the principal payment for a given period of a loan.\n"
            + "PRODUCT|number1, [number2], ...|Multiplies its arguments.\n"
            + "PROPER|text|Capitalizes the first letter of each word in a text string.\n"
            + "PV|rate, nper, pmt, [fv], [type]|Returns the present value of an investment.\n"
            + "QUARTILE|array, quart|Returns the quartile of a data set; the same as QUARTILE.INC.\n"
            + "QUARTILE.EXC|array, quart|Returns the quartile of a data set based on percentiles from 0 to 1, exclusive.\n"
            + "QUARTILE.INC|array, quart|Returns the quartile of a data set based on percentiles from 0 to 1, inclusive.\n"
            + "QUOTIENT|numerator, denominator|Returns the integer portion of a division.\n"
            + "RADIANS|angle|Converts degrees to radians.\n"
            + "RAND||Returns a random number greater than or equal to 0 and less than 1.\n"
            + "RANDARRAY|[rows], [columns], [min], [max], [whole_number]|Returns an array of random numbers.\n"
            + "RANDBETWEEN|bottom, top|Returns a random integer between the numbers you specify.\n"
            + "RANK|number, ref, [order]|Returns the rank of a number in a list; the same as RANK.EQ.\n"
            + "RANK.AVG|number, ref, [order]|Returns the rank of a number in a list, averaging the rank of ties.\n"
            + "RANK.EQ|number, ref, [order]|Returns the rank of a number in a list, giving ties the top rank.\n"
            + "RATE|nper, pmt, pv, [fv], [type], [guess]|Returns the interest rate per period of an annuity.\n"
            + "REPLACE|old_text, start_num, num_chars, new_text|Replaces characters within text.\n"
            + "REPT|text, number_times|Repeats text a given number of times.\n"
            + "RIGHT|text, [num_chars]|Returns the rightmost characters of a text string.\n"
            + "ROMAN|number, [form]|Converts an Arabic number to a Roman numeral as text.\n"
            + "ROUND|number, num_digits|Rounds a number to a specified number of digits.\n"
            + "ROUNDDOWN|number, num_digits|Rounds a number down, toward zero.\n"
            + "ROUNDUP|number, num_digits|Rounds a number up, away from zero.\n"
            + "ROW|[reference]|Returns the row number of a reference.\n"
            + "ROWS|array|Returns the number of rows in a reference or array.\n"
            + "RRI|nper, pv, fv|Returns the equivalent interest rate for the growth of an investment.\n"
            + "RSQ|known_y's, known_x's|Returns the square of the Pearson correlation coefficient.\n"
            + "SEARCH|find_text, within_text, [start_num]|Finds one text string inside another, case-insensitive.\n"
            + "SEC|number|Returns the secant of an angle given in radians.\n"
            + "SECH|number|Returns the hyperbolic secant of an angle given in radians.\n"
            + "SECOND|serial_number|Returns the second of a time value.\n"
            + "SEQUENCE|rows, [columns], [start], [step]|Returns an array of sequential numbers.\n"
            + "SERIESSUM|x, n, m, coefficients|Returns the sum of a power series based on a formula.\n"
            + "SHEET|[value]|Returns the sheet number of a referenced sheet.\n"
            + "SHEETS|[reference]|Returns the number of sheets in a reference.\n"
            + "SIGN|number|Returns the sign of a number.\n"
            + "SIN|number|Returns the sine of an angle given in radians.\n"
            + "SINH|number|Returns the hyperbolic sine of a number.\n"
            + "SKEW|number1, [number2], ...|Returns the skewness of a distribution.\n"
            + "SKEW.P|number1, [number2], ...|Returns the skewness of a distribution based on a population.\n"
            + "SLN|cost, salvage, life|Returns the straight-line depreciation of an asset for one period.\n"
            + "SLOPE|known_y's, known_x's|Returns the slope of the linear regression line.\n"
            + "SMALL|array, k|Returns the k-th smallest value in a data set.\n"
            + "SORT|array, [sort_index], [sort_order], [by_col]|Sorts the contents of a range or array.\n"
            + "SORTBY|array, by_array1, [sort_order1], [by_array2, sort_order2], ...|Sorts a range or array based on the values in another range or array.\n"
            + "SQRT|number|Returns the positive square root of a number.\n"
            + "SQRTPI|number|Returns the square root of a number multiplied by pi.\n"
            + "STANDARDIZE|x, mean, standard_dev|Returns a normalized value.\n"
            + "STDEV|number1, [number2], ...|Estimates standard deviation from a sample; the same as STDEV.S.\n"
            + "STDEV.P|number1, [number2], ...|Calculates standard deviation from an entire population.\n"
            + "STDEV.S|number1, [number2], ...|Estimates standard deviation from a sample.\n"
            + "STDEVA|value1, [value2], ...|Estimates standard deviation from a sample, counting text and logical values.\n"
            + "STDEVP|number1, [number2], ...|Calculates standard deviation from an entire population; the same as STDEV.P.\n"
            + "STDEVPA|value1, [value2], ...|Calculates standard deviation from a population, counting text and logical values.\n"
            + "STEYX|known_y's, known_x's|Returns the standard error of the predicted y-value for each x in a regression.\n"
            + "SUBSTITUTE|text, old_text, new_text, [instance_num]|Substitutes new text for old text in a string.\n"
            + "SUBTOTAL|function_num, ref1, [ref2], ...|Returns a subtotal in a list or database.\n"
            + "SUM|number1, [number2], ...|Adds its arguments.\n"
            + "SUMIF|range, criteria, [sum_range]|Adds the cells that meet a condition.\n"
            + "SUMIFS|sum_range, criteria_range1, criteria1, [criteria_range2, criteria2], ...|Adds the cells that meet all of several conditions.\n"
            + "SUMPRODUCT|array1, [array2], ...|Returns the sum of the products of corresponding array components.\n"
            + "SUMSQ|number1, [number2], ...|Returns the sum of the squares of the arguments.\n"
            + "SUMX2MY2|array_x, array_y|Returns the sum of the difference of squares of corresponding values in two arrays.\n"
            + "SUMX2PY2|array_x, array_y|Returns the sum of the sum of squares of corresponding values in two arrays.\n"
            + "SUMXMY2|array_x, array_y|Returns the sum of squares of differences of corresponding values in two arrays.\n"
            + "SWITCH|expression, value1, result1, [default_or_value2, result2], ...|Compares an expression to a list of values and returns the first matching result.\n"
            + "SYD|cost, salvage, life, per|Returns the sum-of-years' digits depreciation of an asset for a period.\n"
            + "T|value|Returns the text if the value is text, otherwise empty text.\n"
            + "T.DIST|x, deg_freedom, cumulative|Returns the left-tailed Student's t-distribution.\n"
            + "T.DIST.2T|x, deg_freedom|Returns the two-tailed Student's t-distribution.\n"
            + "T.DIST.RT|x, deg_freedom|Returns the right-tailed Student's t-distribution.\n"
            + "T.INV|probability, deg_freedom|Returns the left-tailed inverse of the Student's t-distribution.\n"
            + "T.INV.2T|probability, deg_freedom|Returns the two-tailed inverse of the Student's t-distribution.\n"
            + "T.TEST|array1, array2, tails, type|Returns the probability associated with a Student's t-test.\n"
            + "TAKE|array, rows, [columns]|Returns a number of rows or columns from the start or end of an array.\n"
            + "TAN|number|Returns the tangent of an angle given in radians.\n"
            + "TANH|number|Returns the hyperbolic tangent of a number.\n"
            + "TDIST|x, deg_freedom, tails|Returns the Student's t-distribution; like T.DIST.RT or T.DIST.2T by tails.\n"
            + "TEXT|value, format_text|Formats a number and converts it to text.\n"
            + "TEXTAFTER|text, delimiter, [instance_num], [match_mode], [match_end], [if_not_found]|Returns the text that occurs after a given delimiter.\n"
            + "TEXTBEFORE|text, delimiter, [instance_num], [match_mode], [match_end], [if_not_found]|Returns the text that occurs before a given delimiter.\n"
            + "TEXTJOIN|delimiter, ignore_empty, text1, [text2], ...|Joins text from several ranges or strings with a delimiter.\n"
            + "TIME|hour, minute, second|Returns the serial number of a particular time.\n"
            + "TIMEVALUE|time_text|Converts a time in text form to a serial number.\n"
            + "TINV|probability, deg_freedom|Returns the two-tailed inverse of the Student's t-distribution; the same as T.INV.2T.\n"
            + "TOCOL|array, [ignore], [scan_by_column]|Returns an array as a single column.\n"
            + "TODAY||Returns the serial number of the current date.\n"
            + "TOROW|array, [ignore], [scan_by_column]|Returns an array as a single row.\n"
            + "TRANSPOSE|array|Returns the transpose of an array.\n"
            + "TRIM|text|Removes extra spaces from text.\n"
            + "TRIMMEAN|array, percent|Returns the mean of the interior of a data set.\n"
            + "TRUE||Returns the logical value TRUE.\n"
            + "TRUNC|number, [num_digits]|Truncates a number to an integer or to a given precision.\n"
            + "TTEST|array1, array2, tails, type|Returns the probability associated with a Student's t-test; the same as T.TEST.\n"
            + "TYPE|value|Returns a number indicating the data type of a value.\n"
            + "UNICHAR|number|Returns the Unicode character for a given number.\n"
            + "UNICODE|text|Returns the number for the first character of the text.\n"
            + "UNIQUE|array, [by_col], [exactly_once]|Returns the unique values from a range or array.\n"
            + "UPPER|text|Converts text to uppercase.\n"
            + "VALUE|text|Converts a text argument to a number.\n"
            + "VAR|number1, [number2], ...|Estimates variance based on a sample; the same as VAR.S.\n"
            + "VAR.P|number1, [number2], ...|Calculates variance based on an entire population.\n"
            + "VAR.S|number1, [number2], ...|Estimates variance based on a sample.\n"
            + "VARA|value1, [value2], ...|Estimates variance based on a sample, counting text and logical values.\n"
            + "VARP|number1, [number2], ...|Calculates variance based on an entire population; the same as VAR.P.\n"
            + "VARPA|value1, [value2], ...|Calculates variance based on a population, counting text and logical values.\n"
            + "VDB|cost, salvage, life, start_period, end_period, [factor], [no_switch]|Returns the depreciation over a period using the variable declining balance method.\n"
            + "VLOOKUP|lookup_value, table_array, col_index_num, [range_lookup]|Looks for a value in the first column of a table and returns a value in the same row from another column.\n"
            + "VSTACK|array1, [array2], ...|Appends arrays vertically and in sequence into one array.\n"
            + "WEEKDAY|serial_number, [return_type]|Returns the day of the week of a date.\n"
            + "WEEKNUM|serial_number, [return_type]|Returns the week number of the year for a date.\n"
            + "WEIBULL|x, alpha, beta, cumulative|Returns the Weibull distribution; the same as WEIBULL.DIST.\n"
            + "WEIBULL.DIST|x, alpha, beta, cumulative|Returns the Weibull distribution.\n"
            + "WORKDAY|start_date, days, [holidays]|Returns the date a number of working days before or after a date.\n"
            + "WORKDAY.INTL|start_date, days, [weekend], [holidays]|Returns the date a number of working days away, with custom weekends.\n"
            + "WRAPCOLS|vector, wrap_count, [pad_with]|Wraps a row or column of values into columns after a given count.\n"
            + "WRAPROWS|vector, wrap_count, [pad_with]|Wraps a row or column of values into rows after a given count.\n"
            + "XIRR|values, dates, [guess]|Returns the internal rate of return for cash flows that are not periodic.\n"
            + "XLOOKUP|lookup_value, lookup_array, return_array, [if_not_found], [match_mode], [search_mode]|Searches a range or array and returns the item for the first match.\n"
            + "XMATCH|lookup_value, lookup_array, [match_mode], [search_mode]|Returns the relative position of an item in an array or range.\n"
            + "XNPV|rate, values, dates|Returns the net present value for cash flows that are not periodic.\n"
            + "XOR|logical1, [logical2], ...|Returns a logical exclusive OR of all arguments.\n"
            + "YEAR|serial_number|Returns the year of a date.\n"
            + "YEARFRAC|start_date, end_date, [basis]|Returns the year fraction between two dates.\n"
            + "Z.TEST|array, x, [sigma]|Returns the one-tailed probability value of a z-test.\n"
            + "ZTEST|array, x, [sigma]|Returns the one-tailed probability value of a z-test; the same as Z.TEST.\n"
            + ""};

    /// The arguments and the description of a function, or null for a name it does not know.
    ///
    /// #### Parameters
    ///
    /// - `name`: the function name, in capitals
    ///
    /// #### Returns
    ///
    /// the arguments, as `number1, [number2], ...`, then the description
    static String[] of(String name) {
        return Table.TABLE.get(name);
    }

    /// Every function name, in alphabetical order.
    static String[] names() {
        return Table.NAMES;
    }

    /// The table, read the first time it is asked for: a class is initialized on first use.
    private static final class Table {
        static final HashMap<String, String[]> TABLE = new HashMap<String, String[]>();
        static final String[] NAMES;

        static {
            java.util.ArrayList<String> order = new java.util.ArrayList<String>();
            for (String part : PARTS) {
                read(TABLE, order, part);
            }
            NAMES = order.toArray(new String[order.size()]);
        }
    }

    private static void read(HashMap<String, String[]> t, java.util.List<String> order, String text) {
        int at = 0;
        while (at < text.length()) {
            int end = text.indexOf('\n', at);
            if (end < 0) {
                end = text.length();
            }
            int a = text.indexOf('|', at);
            int b = a < 0 ? -1 : text.indexOf('|', a + 1);
            if (b > 0 && b < end) {
                order.add(text.substring(at, a));
                t.put(text.substring(at, a), new String[] {text.substring(a + 1, b), text.substring(b + 1, end)});
            }
            at = end + 1;
        }
    }

    /// The arguments of a function split one by one: `[number2]` and `...` are entries too.
    static String[] arguments(String args) {
        if (args == null || args.length() == 0) {
            return new String[0];
        }
        java.util.ArrayList<String> out = new java.util.ArrayList<String>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < args.length(); i++) {
            char c = args.charAt(i);
            if (c == '[') {
                depth++;
            } else if (c == ']') {
                depth--;
            } else if (c == ',' && depth == 0) {
                out.add(args.substring(start, i).trim());
                start = i + 1;
            }
        }
        out.add(args.substring(start).trim());
        return out.toArray(new String[out.size()]);
    }
}
