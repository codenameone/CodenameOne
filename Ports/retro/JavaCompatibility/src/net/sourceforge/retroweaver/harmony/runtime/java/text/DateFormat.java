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
package net.sourceforge.retroweaver.harmony.runtime.java.text;

import java.util.Date;

/**
 * A class for parsing and formatting localisation sensitive dates, compatible
 * with Jave 6 SDK. This implementation uses the <a href=
 * "https://codenameone.googlecode.com/svn/trunk/CodenameOne/javadoc/index.html"
 * >Codename One localization manager</a> for handling formatting dates. Parsing
 * dates is not implemented in this class since the localization pattern is not
 * exposed.
 * 
 * @author Eric Coolman
 * @see <a href="http://docs.oracle.com/javase/6/docs/api/java/text/DateFormat.html">http://docs.oracle.com/javase/6/docs/api/java/text/DateFormat.html</a>
 */
public class DateFormat extends Format {
	/**
	 * Constant for full style parsing/formatting pattern.
	 */
	public static final int FULL = 0;
	/**
	 * Constant for long style parsing/formatting pattern.
	 */
	public static final int LONG = 1;
	/**
	 * Constant for medium style parsing/formatting pattern.
	 */
	public static final int MEDIUM = 2;
	/**
	 * Constant for short style parsing/formatting pattern.
	 */
	public static final int SHORT = 3;
	/**
	 * Constant for default style (MEDIUM) parsing/formatting pattern.
	 */
	public static final int DEFAULT = MEDIUM;

	private int dateStyle;
	private int timeStyle;

	/**
	 * Construct a date formatter using default patterns for date and time (SHORT/SHORT).
	 */
	DateFormat() {
		this(SHORT, SHORT);
	}

	/**
	 * 
	 */
	DateFormat(int dateStyle, int timeStyle) {
		this.dateStyle = dateStyle;
		this.timeStyle = timeStyle;
	}


	/**
	 * Format a given object.
	 * 
	 * @obj object to be formatted.
	 * @return formatted object.
	 * @throws IllegalArgumentException of the source can not be formatted.
	 */
	@Override
	public String format(Object obj) throws IllegalArgumentException {
		return format(obj, new StringBuffer());
	}

	/**
	 * Format a given object.
	 * 
	 * @param source object to be formatted.
	 * @param toAppendTo buffer to which to append output.
	 * @return  formatted date.
	 * @throws IllegalArgumentException of the source can not be formatted.
	 */
	String format(Object obj, StringBuffer toAppendTo) throws IllegalArgumentException {
		Date source = null;
		if (obj instanceof Date) {
			source = (Date) obj;
		} else if (obj instanceof String) {
			try {
				source = parse((String) obj);
			} catch (ParseException pe) {
				throw new RuntimeException(pe.toString());
			}
		}
		if (source == null) {
			throw new IllegalArgumentException((obj == null) ? "null" : obj.toString());
		}
		return format(source, toAppendTo);
	}

	/**
	 * Format a given date.
	 * 
	 * @param source date to be formatted.
	 * @return  formatted date.
	 */
	public String format(Date source) {
		return format(source, new StringBuffer());
	}

	/**
	 * Format a given date.
	 * 
	 * @param source date to be formatted.
	 * @param toAppendTo buffer to which to append output.
	 * @return  formatted date.
	 */
	String format(Date source, StringBuffer toAppendTo) {
		/*if (source == null) {
			throw new IllegalArgumentException();
		}
		switch (dateStyle) {
			case MEDIUM :
				String medium = getL10NManager().formatDateTimeMedium(source);
				int i = medium.indexOf(' ');
				if (i != -1) {
					toAppendTo.append(medium.substring(0, i));
				}
				// default to long style
			case LONG :
			case FULL :
				toAppendTo.append(getL10NManager().formatDateLongStyle(source));
				break;
			case SHORT :
			default :
				toAppendTo.append(getL10NManager().formatDateShortStyle(source));
				break;
		}
		if (timeStyle != DEFAULT) {
			String fulldate = null;
			switch (timeStyle) {
				case MEDIUM :
					fulldate = getL10NManager().formatDateTimeMedium(source);
					break;
				case LONG :
				case FULL :
					// there's no 'long' format method for datetime.
					fulldate = getL10NManager().formatDateTime(source);
					break;
				case SHORT :
				default :
					fulldate = getL10NManager().formatDateTimeShort(source);
					break;
			}
			if (fulldate != null) {
				int i = fulldate.indexOf(' ');
				if (i != -1) {
					if (dateStyle != DEFAULT) {
						toAppendTo.append(' ');
					}
					toAppendTo.append(fulldate.substring(i + 1));
				}
			}
		}
		return toAppendTo.toString();*/
            return source.toString();
	}

	/**
	 * NOT IMPLEMENTED - use SimpleDateFormat for parsing instead.
	 */
	@Override
	public Object parseObject(String source) throws ParseException {
		// can't parse because we don't know the L10NManagers templates
		throw new ParseException("Not implemented", 0);
	}

	/**
	 * NOT IMPLEMENTED - use SimpleDateFormat for parsing instead.
	 */
	public Date parse(String source) throws ParseException {
		return (Date) parseObject(source);
	}

	/**
	 * Get a DateFormat instance with default style for date/time (SHORT/SHORT).
	 * @return a DateFormat instance.
	 */
	public static final DateFormat getInstance() {
		return getDateTimeInstance(SHORT, SHORT);
	}

	/**
	 * Get a DateFormat instance with default style for date (SHORT). 
	 * @return a DateFormat instance.
	 */
	public static final DateFormat getDateInstance() {
		return getDateInstance(SHORT);
	}

	/**
	 * Get a DateFormat instance with default style for time (SHORT).
	 * @return a DateFormat instance.
	 */
	public static final DateFormat getTimeInstance() {
		return getTimeInstance(SHORT);
	}

	/**
	 * Get a DateFormat instance that uses a given style for dates. 
	 * 
	 * @param style style to use for parsing and formatting (SHORT, MEDIUM, LONG, FULL, DEFAULT);
	 * @return a DateFormat instance.
	 * @see #SHORT
	 * @see #MEDIUM
	 * @see #LONG
	 * @see #FULL
	 * @see #DEFAULT
	 */
	public static final DateFormat getDateInstance(int style) {
		return getDateTimeInstance(style, DEFAULT);
	}

	/**
	 * Get a DateFormat instance that uses a given style for times. 
	 * 
	 * @param style style to use for parsing and formatting (SHORT, MEDIUM, LONG, FULL, DEFAULT);
	 * @return a DateFormat instance.
	 * @see #SHORT
	 * @see #MEDIUM
	 * @see #LONG
	 * @see #FULL
	 * @see #DEFAULT
	 */
	public static final DateFormat getTimeInstance(int style) {
		return getDateTimeInstance(DEFAULT, style);
	}

	/**
	 * Get a DateFormat instance that uses a given style for dates and times. 
	 * 
	 * @param style style to use for parsing and formatting (SHORT, MEDIUM, LONG, FULL, DEFAULT);
	 * @return a DateFormat instance.
	 * @see #SHORT
	 * @see #MEDIUM
	 * @see #LONG
	 * @see #FULL
	 * @see #DEFAULT
	 */
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