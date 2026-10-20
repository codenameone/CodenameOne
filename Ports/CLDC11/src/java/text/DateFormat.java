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
package java.text;

import java.util.Date;


/// A class for parsing and formatting localisation sensitive dates, compatible
/// with Jave 6 SDK. This implementation uses the Codename One localization manager for handling formatting dates. Parsing
/// dates is not implemented in this class since the localization pattern is not
/// exposed.
///
/// #### Deprecated
///
/// this class has many issues in iOS and other platforms, please use the L10NManager
///
/// #### See also
///
/// - [http://docs.oracle.com/javase/6/docs/api/java/text/DateFormat.html](http://docs.oracle.com/javase/6/docs/api/java/text/DateFormat.html)
public class DateFormat extends Format {
	/// Constant for full style parsing/formatting pattern.
	public static final int FULL = 0;
	/// Constant for long style parsing/formatting pattern.
	public static final int LONG = 1;
	/// Constant for medium style parsing/formatting pattern.
	public static final int MEDIUM = 2;
	/// Constant for short style parsing/formatting pattern.
	public static final int SHORT = 3;
	/// Constant for default style (MEDIUM) parsing/formatting pattern.
	public static final int DEFAULT = MEDIUM;

	private int dateStyle;
	private int timeStyle;

	/// Construct a date formatter using default patterns for date and time (SHORT/SHORT).
	DateFormat() {
		this(SHORT, SHORT);
	}

	///
	DateFormat(int dateStyle, int timeStyle) {
		this.dateStyle = dateStyle;
		this.timeStyle = timeStyle;
	}


	/// Format a given object, a date or a number of milliseconds since the epoch.
	///
	/// #### Parameters
	///
	/// - `obj`: object to be formatted.
	///
	/// - `toAppendTo`: buffer to which to append output.
	///
	/// - `fieldPosition`: not updated: no field is reported.
	///
	/// #### Returns
	///
	/// `toAppendTo`.
	///
	/// #### Throws
	///
	/// - `IllegalArgumentException`: if the source is neither a date nor a number.
	@Override
	public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition fieldPosition) {
		if (obj instanceof Date) {
			return format((Date) obj, toAppendTo, fieldPosition);
		}
		if (obj instanceof Number) {
			return format(new Date(((Number) obj).longValue()), toAppendTo, fieldPosition);
		}
		throw new IllegalArgumentException("Cannot format given Object as a Date");
	}

	/// Format a given date.
	///
	/// #### Parameters
	///
	/// - `date`: date to be formatted.
	///
	/// - `toAppendTo`: buffer to which to append output.
	///
	/// - `fieldPosition`: not updated: no field is reported.
	///
	/// #### Returns
	///
	/// `toAppendTo`.
	public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition fieldPosition) {
		return toAppendTo.append(format(date));
	}

	/// Format a given object.
	///
	/// #### Parameters
	///
	/// - `source`: object to be formatted.
	///
	/// - `toAppendTo`: buffer to which to append output.
	///
	/// #### Returns
	///
	/// formatted date.
	///
	/// #### Throws
	///
	/// - `IllegalArgumentException`: of the source can not be formatted.
	String format(Object obj, StringBuffer toAppendTo) throws IllegalArgumentException {
		return null;
	}

	/// Format a given date.
	///
	/// #### Parameters
	///
	/// - `source`: date to be formatted.
	///
	/// #### Returns
	///
	/// formatted date.
	public String format(Date source) {
		return format(source, new StringBuffer());
	}

	/// Format a given date.
	///
	/// #### Parameters
	///
	/// - `source`: date to be formatted.
	///
	/// - `toAppendTo`: buffer to which to append output.
	///
	/// #### Returns
	///
	/// formatted date.
	String format(Date source, StringBuffer toAppendTo) {
		return null;
	}

	/// Parse a date from the beginning of the source. NOT IMPLEMENTED for the
	/// style based formats - use SimpleDateFormat for parsing instead.
	@Override
	public Object parseObject(String source) throws ParseException {
		return parse(source);
	}

	/// Parse a date from the beginning of the source. NOT IMPLEMENTED for the
	/// style based formats - use SimpleDateFormat for parsing instead.
	public Date parse(String source) throws ParseException {
		return parseAt(source, 0, null);
	}

	/// Parse a date starting at a position. On success the index of the
	/// position is moved past the date; on failure it is left alone, its
	/// error index is set and null is returned.
	public Date parse(String source, ParsePosition pos) {
		// A start past the end is a failed parse at that index, as in the
		// JDK, not a StringIndexOutOfBoundsException from the field readers.
		if (pos.getIndex() > source.length()) {
			pos.setErrorIndex(pos.getIndex());
			return null;
		}
		int[] end = new int[1];
		try {
			Date result = parseAt(source, pos.getIndex(), end);
			pos.setIndex(end[0]);
			return result;
		} catch (ParseException e) {
			pos.setErrorIndex(e.getErrorOffset());
			return null;
		}
	}

	@Override
	public Object parseObject(String source, ParsePosition pos) {
		return parse(source, pos);
	}

	/// Parse a date at index start of source and, when end is not null, store
	/// in end[0] the index just past it.
	Date parseAt(String source, int start, int[] end) throws ParseException {
		// can't parse because we don't know the L10NManagers templates
		throw new ParseException("Not implemented", start);
	}

	/// Get a DateFormat instance with default style for date/time (SHORT/SHORT).
	///
	/// #### Returns
	///
	/// a DateFormat instance.
	public static final DateFormat getInstance() {
		return getDateTimeInstance(SHORT, SHORT);
	}

	/// Get a DateFormat instance with default style for date (SHORT).
	///
	/// #### Returns
	///
	/// a DateFormat instance.
	public static final DateFormat getDateInstance() {
		return getDateInstance(SHORT);
	}

	/// Get a DateFormat instance with default style for time (SHORT).
	///
	/// #### Returns
	///
	/// a DateFormat instance.
	public static final DateFormat getTimeInstance() {
		return getTimeInstance(SHORT);
	}

	/// Get a DateFormat instance that uses a given style for dates.
	///
	/// #### Parameters
	///
	/// - `style`: style to use for parsing and formatting (SHORT, MEDIUM, LONG, FULL, DEFAULT);
	///
	/// #### Returns
	///
	/// a DateFormat instance.
	///
	/// #### See also
	///
	/// - #SHORT
	///
	/// - #MEDIUM
	///
	/// - #LONG
	///
	/// - #FULL
	///
	/// - #DEFAULT
	public static final DateFormat getDateInstance(int style) {
		return getDateTimeInstance(style, DEFAULT);
	}

	/// Get a DateFormat instance that uses a given style for times.
	///
	/// #### Parameters
	///
	/// - `style`: style to use for parsing and formatting (SHORT, MEDIUM, LONG, FULL, DEFAULT);
	///
	/// #### Returns
	///
	/// a DateFormat instance.
	///
	/// #### See also
	///
	/// - #SHORT
	///
	/// - #MEDIUM
	///
	/// - #LONG
	///
	/// - #FULL
	///
	/// - #DEFAULT
	public static final DateFormat getTimeInstance(int style) {
		return getDateTimeInstance(DEFAULT, style);
	}

	/// Get a DateFormat instance that uses a given style for dates and times.
	///
	/// #### Parameters
	///
	/// - `style`: style to use for parsing and formatting (SHORT, MEDIUM, LONG, FULL, DEFAULT);
	///
	/// #### Returns
	///
	/// a DateFormat instance.
	///
	/// #### See also
	///
	/// - #SHORT
	///
	/// - #MEDIUM
	///
	/// - #LONG
	///
	/// - #FULL
	///
	/// - #DEFAULT
	public static final DateFormat getDateTimeInstance(int dateStyle, int timeStyle) {
		return new DateFormat(dateStyle, timeStyle);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + dateStyle;
		result = prime * result + timeStyle;
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DateFormat other = (DateFormat) obj;
		if (dateStyle != other.dateStyle)
			return false;
		if (timeStyle != other.timeStyle)
			return false;
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#clone()
	 */
	public Object clone() {
		return new DateFormat(dateStyle, timeStyle);
	}
}