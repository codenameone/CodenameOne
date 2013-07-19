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

import java.util.Calendar;
import java.util.Date;
import net.sourceforge.retroweaver.harmony.runtime.java.util.List;
import java.util.TimeZone;
import net.sourceforge.retroweaver.harmony.runtime.java.util.Vector;

/**
 * A class for parsing and formatting dates with a given pattern, compatible
 * with the Java 6 API.
 * 
 * @see http://docs.oracle.com/javase/6/docs/api/java/text/DateFormat.html
 * @author Eric Coolman
 */
public class SimpleDateFormat extends DateFormat {
	/**
	 * Pattern character for ERA (ie. BC, AD).
	 */
	private static final char ERA_LETTER = 'G';
	/**
	 * Pattern character for year.
	 */
	private static final char YEAR_LETTER = 'y';
	/**
	 * Pattern character for month.
	 */
	private static final char MONTH_LETTER = 'M';
	/**
	 * Pattern character for week in year.
	 */
	private static final char WEEK_IN_YEAR_LETTER = 'w';
	/**
	 * Pattern character for week in month.
	 */
	private static final char WEEK_IN_MONTH_LETTER = 'W';
	/**
	 * Pattern character for day in year.
	 */
	private static final char DAY_IN_YEAR_LETTER = 'D';
	/**
	 * Pattern character for day.
	 */
	private static final char DAY_LETTER = 'd';
	/**
	 * Pattern character for day-of-week in month.
	 */
	private static final char DOW_IN_MONTH_LETTER = 'F';
	/**
	 * Pattern character for day of week.
	 */
	private static final char DAY_OF_WEEK_LETTER = 'E';
	/**
	 * Pattern character for am/pm.
	 */
	private static final char AMPM_LETTER = 'a';
	/**
	 * Pattern character for hour (0-23).
	 */
	private static final char HOUR_LETTER = 'H';
	/**
	 * Pattern character for 1-based hour (1-24).
	 */
	private static final char HOUR_1_LETTER = 'k';
	/**
	 * Pattern character for 12-hour (0-11).
	 */
	private static final char HOUR12_LETTER = 'K';
	/**
	 * Pattern character for 1-based 12-hour (1-12).
	 */
	private static final char HOUR12_1_LETTER = 'h';
	/**
	 * Pattern character for minute.
	 */
	private static final char MINUTE_LETTER = 'm';
	/**
	 * Pattern character for second.
	 */
	private static final char SECOND_LETTER = 's';
	/**
	 * Pattern character for millisecond.
	 */
	private static final char MILLISECOND_LETTER = 'S';
	/**
	 * Pattern character for general timezone.
	 */
	private static final char TIMEZONE_LETTER = 'z';
	/**
	 * Pattern character for RFC 822-style timezone.
	 */
	private static final char TIMEZONE822_LETTER = 'Z';
	/**
	 * Internally used character for literal text.
	 */
	private static final char LITERAL_LETTER = '*';
	/**
	 * Pattern character for starting/ending literal text explicitly in pattern.
	 */
	private static final char EXPLICIT_LITERAL = '\'';
	/**
	 * positive sign
	 */
	private static final char SIGN_POSITIVE = '+';
	/**
	 * negative sign
	 */
	private static final char SIGN_NEGATIVE = '-';
	/**
	 * The number of milliseconds in a minute.
	 */
	private static final int MILLIS_TO_MINUTES = 60000;
	/**
	 * Pattern characters recognised by this implementation (same as JDK 1.6).
	 */
	private static final String PATTERN_LETTERS = "adDEFGHhKkMmsSwWyzZ";
	/**
	 * TimeZone ID for Greenwich Mean Time
	 */
	private static final String GMT = "GMT";
	/**
	 * This is missing from the Codename One Calendar object, but required by
	 * TimeZone.getOffset()
	 */
	private static final int ERA = 0;
	/**
	 * More missing from the calendar object - being lower field values, it is
	 * likely they do  exist on the devices Calendar object, if they don't, certain
	 * lesser-used letters will not work in a pattern.
	 */
	private static final int WEEK_OF_MONTH = 4;
	private static final int WEEK_OF_YEAR = 3;
	private static final int DAY_OF_WEEK_IN_MONTH = 8;
	private static final int DAY_OF_YEAR = 6;
	/**
	 * Localisation sensitive symbols used for handling text components. 
	 */
	private DateFormatSymbols dateFormatSymbols;

	/**
	 * The user-supplied pattern
	 */
	private String pattern;

	/**
	 * The parsed pattern
	 */
	private List<String> patternTokens;

	/**
	 * Construct a SimpleDateFormat with no pattern.
	 */
	public SimpleDateFormat() {
		super();
	}

	/**
	 * Construct a SimpleDateFormat with a given pattern.
	 * 
	 * @param pattern
	 */
	public SimpleDateFormat(String pattern) {
		super();
		this.pattern = pattern;
	}

	/**
	 * @return the pattern
	 */
	public String toPattern() {
		return pattern;
	}

	/**
	 * Get the date format symbols for parsing/formatting textual components of
	 * dates in a localization sensitive way.
	 * 
	 * @return current symbols.
	 */
	public DateFormatSymbols getDateFormatSymbols() {
		if (dateFormatSymbols == null) {
			dateFormatSymbols = new DateFormatSymbols();
		}
		return dateFormatSymbols;
	}

	/**
	 * Apply new date format symbols for parsing/formatting textual components
	 * of dates in a localisation sensitive way.
	 * 
	 * @param newSymbols new format symbols.
	 */
	public void setDateFormatSymbols(DateFormatSymbols newSymbols) {
		dateFormatSymbols = newSymbols;
	}

	/**
	 * Apply a new pattern.
	 * 
	 * @param pattern the pattern to set
	 */
	public void applyPattern(String pattern) {
		this.pattern = pattern;
		if (patternTokens != null) {
			patternTokens.clear();
			patternTokens = null;
		}
	}

	/**
	 * G
	 * 
	 * @return
	 */
	List<String> getPatternTokens() {
		if (this.patternTokens == null) {
			patternTokens = parseDatePattern(pattern);
		}
		return patternTokens;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#clone()
	 */
	@Override
	public Object clone() {
		SimpleDateFormat sdf = new SimpleDateFormat(pattern);
		sdf.setDateFormatSymbols(dateFormatSymbols);
		return sdf;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.text.DateFormat#format(java.util.Date)
	 */
	@Override
	public String format(Date source) {
		return format(source, new StringBuffer());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.text.DateFormat#format(java.util.Date, java.lang.StringBuffer)
	 */
	@Override
	String format(Date source, StringBuffer toAppendTo) {
		if (pattern == null) {
			return super.format(source, toAppendTo);
		}
		// format based on local timezone
		Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
		calendar.setTime(source);
		List<String> pattern = getPatternTokens();
		for (int i = 0; i < pattern.size(); i++) {
			String token = (String) pattern.get(i);
			char patternChar = token.charAt(0);
			token = token.substring(1);
			int len = token.length();
			int v = -1;
			switch (patternChar) {
				case LITERAL_LETTER :
					toAppendTo.append(token);
					break;
				case AMPM_LETTER :
					boolean am = calendar.get(Calendar.AM_PM) == Calendar.AM;
					String ampm[] = getDateFormatSymbols().getAmPmStrings();
					if (len == 1) {
						// JDK6 doesn't handle this, but likely useful
						// somewhere, and is parsable.
						toAppendTo.append(am ? ampm[0].charAt(0) : ampm[1].charAt(0));
					} else {
						toAppendTo.append(am ? ampm[0] : ampm[1]);
					}
					break;
				case ERA_LETTER :
					toAppendTo.append(getDateFormatSymbols().getEras()[calendar.get(ERA)]);
					break;
				case DAY_OF_WEEK_LETTER :
					v = calendar.get(Calendar.DAY_OF_WEEK) - 1;
					if (len > 3) {
						toAppendTo.append(getDateFormatSymbols().getWeekdays()[v]);
					} else {
						toAppendTo.append(getDateFormatSymbols().getShortWeekdays()[v]);
					}
					break;
				case TIMEZONE_LETTER :
					String names[] = getTimeZoneDisplayNames(calendar.getTimeZone().getID());
					if (names == null) {
						toAppendTo.append(calendar.getTimeZone().getID());
					} else {
						toAppendTo.append(names[DateFormatSymbols.ZONE_SHORTNAME]);
					}
					break;
				case TIMEZONE822_LETTER :
					v = getOffsetInMinutes(calendar, calendar.getTimeZone());
					if (v < 0) {
						toAppendTo.append(SIGN_NEGATIVE);
						v = -v;
					} else {
						toAppendTo.append(SIGN_POSITIVE);
					}
					toAppendTo.append(leftPad(v / 60, 2));
					toAppendTo.append(leftPad(v % 60, 2));
					break;
				case YEAR_LETTER :
					v = calendar.get(Calendar.YEAR);
					if (len == 2) {
						v %= 100;
					}
					toAppendTo.append(v);
					break;
				case MONTH_LETTER :
					v = calendar.get(Calendar.MONTH) - Calendar.JANUARY;
					if (len > 3) {
						toAppendTo.append(getDateFormatSymbols().getMonths()[v]);
					} else if (len == 3) {
						toAppendTo.append(getDateFormatSymbols().getShortMonths()[v]);
					} else {
						toAppendTo.append(leftPad(v + 1, len));
					}
					break;
				case DAY_LETTER :
					v = calendar.get(Calendar.DAY_OF_MONTH);
					toAppendTo.append(leftPad(v, len));
					break;
				case HOUR_LETTER :
				case HOUR_1_LETTER :
				case HOUR12_LETTER :
				case HOUR12_1_LETTER :
					v = calendar.get(Calendar.HOUR_OF_DAY);
					if (patternChar == HOUR_1_LETTER || patternChar == HOUR12_1_LETTER) {
						v += 1;
					}
					if (patternChar == HOUR12_LETTER || patternChar == HOUR12_1_LETTER) {
						v %= 12;
					}
					toAppendTo.append(leftPad(v, len));
					break;
				case MINUTE_LETTER :
					v = calendar.get(Calendar.MINUTE);
					toAppendTo.append(leftPad(v, len));
					break;
				case SECOND_LETTER :
					v = calendar.get(Calendar.SECOND);
					toAppendTo.append(leftPad(v, len));
					break;
				case MILLISECOND_LETTER :
					v = calendar.get(Calendar.MILLISECOND);
					toAppendTo.append(leftPad(v, len));
					break;
				case WEEK_IN_YEAR_LETTER :
					v = calendar.get(WEEK_OF_YEAR);
					toAppendTo.append(leftPad(v, len));
					break;
				case WEEK_IN_MONTH_LETTER :
					v = calendar.get(WEEK_OF_MONTH);
					toAppendTo.append(leftPad(v, len));
					break;
				case DAY_IN_YEAR_LETTER :
					v = calendar.get(DAY_OF_YEAR);
					toAppendTo.append(leftPad(v, len));
					break;
				case DOW_IN_MONTH_LETTER :
					v = calendar.get(DAY_OF_WEEK_IN_MONTH);
					toAppendTo.append(leftPad(v, len));
					break;
			}
		}
		return toAppendTo.toString();
	}

	private String[] getTimeZoneDisplayNames(String id) {
		for (String zoneStrings[] : getDateFormatSymbols().getZoneStrings()) {
			if (zoneStrings[DateFormatSymbols.ZONE_ID].equalsIgnoreCase(id)) {
				return zoneStrings;
			}
		}
		return null;
	}

	String leftPad(int v, int size) {
		String s = String.valueOf(v);
		for (int i = s.length(); i < size; i++) {
			s = '0' + s;
		}
		return s;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.text.DateFormat#parse(java.lang.String)
	 */
	@Override
	public Date parse(String source) throws ParseException {
		if (pattern == null) {
			return super.parse(source);
		}
		int startIndex = 0;
		// parse based on GMT timezone for handling offsets
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(GMT));
		int tzMinutes = -1;
		List<String> pattern = getPatternTokens();
		for (int i = 0; i < pattern.size(); i++) {
			String token = (String) pattern.get(i);
			boolean adjacent = false;
			if (i < (pattern.size() - 1)) {
				adjacent = ((String) pattern.get(i + 1)).charAt(0) != LITERAL_LETTER;
			}
			String s = null;
			int v = -1;
			char patternChar = token.charAt(0);
			token = token.substring(1);
			switch (patternChar) {
				case LITERAL_LETTER :
					s = readLiteral(source, startIndex, token);
					break;
				case AMPM_LETTER :
					s = readAmPmMarker(source, startIndex);
					if (s == null || ((v = parseAmPmMarker(source, startIndex)) == -1)) {
						throwInvalid("am/pm marker", startIndex);
					}
					if (v == Calendar.PM) {
						tzMinutes = ((tzMinutes == -1) ? 0 : tzMinutes) + 12 * 60;
					}
					break;
				case DAY_OF_WEEK_LETTER :
					s = readDayOfWeek(source, startIndex);
					if (s == null) {
						throwInvalid("weekday", startIndex);
					}
					break;
				case TIMEZONE_LETTER :
				case TIMEZONE822_LETTER :
					s = readTimeZone(source, startIndex);
					if (s == null || (v = parseTimeZone(s, startIndex)) == -1) {
						throwInvalid("timezone", startIndex);
					}
					tzMinutes = ((tzMinutes == -1) ? 0 : tzMinutes) + v;
					break;
				case YEAR_LETTER :
					s = readNumber(source, startIndex, token, adjacent);
					calendar.set(Calendar.YEAR, parseYear(s, token, startIndex));
					break;
				case MONTH_LETTER :
					s = readMonth(source, startIndex, token, adjacent);
					calendar.set(Calendar.MONTH, parseMonth(s, startIndex));
					break;
				case DAY_LETTER :
					s = readNumber(source, startIndex, token, adjacent);
					calendar.set(Calendar.DAY_OF_MONTH, parseNumber(s, startIndex, "day of month", 1, 31));
					break;
				case HOUR_LETTER :
				case HOUR_1_LETTER :
				case HOUR12_LETTER :
				case HOUR12_1_LETTER :
					s = readNumber(source, startIndex, token, adjacent);
					calendar.set(Calendar.HOUR_OF_DAY, parseHour(s, patternChar, startIndex));
					break;
				case MINUTE_LETTER :
					s = readNumber(source, startIndex, token, adjacent);
					calendar.set(Calendar.MINUTE, parseNumber(s, startIndex, "minute", 0, 59));
					break;
				case SECOND_LETTER :
					s = readNumber(source, startIndex, token, adjacent);
					calendar.set(Calendar.SECOND, parseNumber(s, startIndex, "second", 0, 59));
					break;
				case MILLISECOND_LETTER :
					s = readNumber(source, startIndex, token, adjacent);
					calendar.set(Calendar.MILLISECOND, parseNumber(s, startIndex, "millisecond", 0, 999));
					break;
				case WEEK_IN_YEAR_LETTER :
					s = readNumber(source, startIndex, token, adjacent);
					calendar.set(WEEK_OF_YEAR, parseNumber(s, startIndex, "week of year", 1, 52));
					break;
				case WEEK_IN_MONTH_LETTER :
					s = readNumber(source, startIndex, token, adjacent);
					calendar.set(WEEK_OF_MONTH, parseNumber(s, startIndex, "week of month", 0, 5));
					break;
				case DAY_IN_YEAR_LETTER :
					s = readNumber(source, startIndex, token, adjacent);
					calendar.set(DAY_OF_YEAR, parseNumber(s, startIndex, "day of year", 1, 365));
					break;
				case DOW_IN_MONTH_LETTER :
					s = readNumber(source, startIndex, token, adjacent);
					calendar.set(DAY_OF_WEEK_IN_MONTH,
							parseNumber(s, startIndex, "day of week in month", -5, 5));
					break;
			}
			if (s != null) {
				startIndex += s.length();
			}
		}
		TimeZone localTimezone = Calendar.getInstance().getTimeZone();
		calendar.setTimeZone(localTimezone);
		// If timezone offset not part of date, the date passed will be treated
		// as if it's local timezone.
		if (tzMinutes != -1) {
			// Adjusting the time to be GMT time, accounting for DST.
			// Doing this here allows tzoffset to be specified before or after
			// actual time in the pattern.
			tzMinutes += getDSTOffset(calendar);
			calendar.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE) + tzMinutes);
			// Now adjust the time again to local time.
			calendar.set(Calendar.MILLISECOND, localTimezone.getRawOffset());
		}
		return calendar.getTime();
	}

	/**
	 * Parse a hour value. Depending on patternChar parameter, the hour can be
	 * 0-23, 1-24, 0-11, or 1-12. The returned value will always be 0 based.
	 * 
	 * @param year as a string.
	 * @param offset the offset of original timestamp where marker started, for
	 *            error reporting.
	 * @return full year.
	 * @throws ParseException if the source could not be parsed.
	 * @see http 
	 *      ://docs.oracle.com/javase/6/docs/api/java/text/SimpleDateFormat.html
	 */
	int parseHour(String source, char patternChar, int offset) throws ParseException {
		int min = (patternChar == HOUR_1_LETTER || patternChar == HOUR12_1_LETTER) ? 1 : 0;
		int max = ((patternChar == HOUR_LETTER || patternChar == HOUR_1_LETTER) ? 23 : 11) + min;
		return parseNumber(source, offset, "hour", min, max) - min;
	}

	/**
	 * Utility method to validate a number is within given range.
	 */
	void validateNumber(int i, int ofs, String name, int min, int max) throws ParseException {
		if (i < min || i > max) {
			throwInvalid(name, ofs);
		}
	}

	/**
	 * Utility method to keep parsing errors consistent.
	 * 
	 * @param name name of the element being parsed when error occurred.
	 * @param offset offset within the original timestamp where named element
	 *            beings.
	 */
	int throwInvalid(String name, int offset) throws ParseException {
		throw new ParseException("Invalid " + name + " value", offset);
	}

	/**
	 * Parse a numeric value, validating against given min/max constraints.
	 * 
	 * @param number as a string.
	 * @param offset the offset of original timestamp where number starts, for
	 *            error reporting.
	 * @return numeric value as an int
	 * @throws ParseException if the source could not be parsed.
	 */
	int parseNumber(String source, int ofs, String name, int min, int max) throws ParseException {
		if (source == null) {
			throwInvalid(name, ofs);
		}
		int v = -1;
		try {
			v = Integer.parseInt(source);
		} catch (NumberFormatException nfe) {
			throwInvalid(name, ofs);
		}
		if (min != max) {
			validateNumber(v, ofs, name, min, max);
		}
		return v;
	}

	/**
	 * Determine the number of minutes to adjust the date for local DST. This
	 * should provide a historically correct value, also accounting for changes
	 * in GMT offset. See TimeZone javadoc for more details.
	 * 
	 * @param calendar
	 * @return
	 */
	int getDSTOffset(Calendar source) {
		TimeZone localTimezone = Calendar.getInstance().getTimeZone();
		int rawOffset = localTimezone.getRawOffset() / MILLIS_TO_MINUTES;
		return getOffsetInMinutes(source, localTimezone) - rawOffset;
	}

	/**
	 * Get the offset from GMT for a given timezone.
	 * 
	 * @param source
	 * @param timezone
	 * @return
	 */
	int getOffsetInMinutes(Calendar source, TimeZone timezone) {
		return timezone.getOffset(source.get(ERA), source.get(Calendar.YEAR), source.get(Calendar.MONTH),
				source.get(Calendar.DAY_OF_MONTH), source.get(Calendar.DAY_OF_WEEK), source.get(Calendar.MILLISECOND))
				/ MILLIS_TO_MINUTES;
	}

	/**
	 * Read an unparsable text string.
	 * 
	 * @param source full timestamp
	 * @param ofs offset within timestamp where text starts
	 * @return the text
	 */
	String readLiteral(String source, int ofs, String token) {
		return source.substring(ofs, ofs + token.length());
	}

	/**
	 * Read the number. Does not attempt to parse.
	 * 
	 * @param source full timestamp
	 * @param ofs offset within timestamp where number starts
	 * @param token the token currently being parsed
	 * @param adjacent true if the number is adjacent to next field with no
	 *            literal separator.
	 * @return the number as a string, or null if could not read.
	 * @see #parseNumber(String, int, String, int, int)
	 */
	String readNumber(String source, int ofs, String token, boolean adjacent) {
		if (adjacent) {
			return source.substring(ofs, ofs + token.length());
		}
		int len = source.length();
		for (int i = ofs; i < len; i++) {
			char ch = source.charAt(i);
			if (isNumeric(ch) == false) {
				// empty string would be invalid number
				if (i == 0) {
					return null;
				}
				return source.substring(ofs, i);
			}
		}
		return source.substring(ofs);
	}

	/**
	 * Parse a year value. If the year is a two digit value, if the value is
	 * within 20 years ahead of the current year, current century will be used
	 * (ie. if current year is 2013, a value of "33" will return 2033),
	 * otherwise previous century is used (ie. with current year of 2012, a
	 * value of 97 will return "1997"). See Java 6 documentation for more
	 * details of this algorithm.
	 * 
	 * @param year as a string.
	 * @param offset the offset of original timestamp where marker started, for
	 *            error reporting.
	 * @return full year.
	 * @throws ParseException if the source could not be parsed.
	 * @see http 
	 *      ://docs.oracle.com/javase/6/docs/api/java/text/SimpleDateFormat.html
	 */
	int parseYear(String source, String token, int ofs) throws ParseException {
		int year = parseNumber(source, ofs, "year", -1, -1);
		int len = source.length();
		int tokenLen = token.length();
		int thisYear = Calendar.getInstance().get(Calendar.YEAR);
		if ((len == 2) && (tokenLen < 3)) {
			int c = (thisYear / 100) * 100;
			year += c;
			if (year > (thisYear + 20)) {
				year -= 100;
			}
		}
		validateNumber(year, ofs, "year", 1000, thisYear + 1000);
		return year;
	}

	/**
	 * Read the day of week string. Does not attempt to parse.
	 * 
	 * @param source full timestamp
	 * @param ofs offset within timestamp where day of week starts
	 * @return the day of week as a string, or null if could not read.
	 */
	String readDayOfWeek(String source, int ofs) {
		int i = findEndText(source, ofs);
		if (i == -1) {
			i = source.length();
		}
		String fragment = source.substring(ofs, i);
		for (String weekday : getDateFormatSymbols().getWeekdays()) {
			if (fragment.equalsIgnoreCase(weekday)) {
				return source.substring(ofs, ofs + weekday.length());
			}
		}
		for (String weekday : getDateFormatSymbols().getShortWeekdays()) {
			if (fragment.equalsIgnoreCase(weekday)) {
				return source.substring(ofs, ofs + weekday.length());
			}
		}
		return null;
	}

	/**
	 * Read the am/pm marker string. Does not attempt to parse.
	 * 
	 * @param source full timestamp
	 * @param ofs offset within timestamp where marker starts
	 * @return the marker as a string, or null if could not read.
	 * @see #parseAmPmMarker(String, int)
	 */
	String readAmPmMarker(String source, int ofs) {
		int i = findEndText(source, ofs);
		if (i == -1) {
			i = source.length();
		}
		String fragment = source.substring(ofs, i).toLowerCase();
		String markers[] = getDateFormatSymbols().getAmPmStrings();
		for (String marker : markers) {
			if (fragment.startsWith(marker)) {
				return source.substring(ofs, ofs + marker.length());
			}
		}
		for (String marker : markers) {
			if (fragment.charAt(0) == marker.charAt(0)) {
				return source.substring(ofs, ofs + 1);
			}
		}
		return null;
	}

	/**
	 * Parse an AM/PM marker. The source marker can be the marker name as
	 * defined in DateFormatSymbols, or the first character of the marker name.
	 * 
	 * @param month as a string.
	 * @param offset the offset of original timestamp where marker started, for
	 *            error reporting.
	 * @return Calendar.AM or Calendar.PM
	 * @see DateFormatSymbols
	 * @throws ParseException if the source could not be parsed.
	 */
	int parseAmPmMarker(String source, int ofs) throws ParseException {
		String markers[] = getDateFormatSymbols().getAmPmStrings();
		for (int i = 0; i < markers.length; i++) {
			if (markers[i].equalsIgnoreCase(source)) {
				return i;
			}
		}
		char ch = source.charAt(0);
		if (ch == markers[0].charAt(0)) {
			return Calendar.AM;
		}
		if (ch == markers[1].charAt(0)) {
			return Calendar.PM;
		}
		return throwInvalid("am/pm marker", ofs);
	}

	/**
	 * Read the month string. Does not attempt to parse.
	 * 
	 * @param source full timestamp
	 * @param ofs offset within timestamp where month starts
	 * @return the month as a string, or null if could not read.
	 * @see #parseMonth(String, int)
	 */
	String readMonth(String source, int ofs, String token, boolean adjacent) {
		if (token.length() < 3) {
			if (adjacent) {
				return source.substring(ofs, ofs + token.length());
			}
			if (isNumeric(source.charAt(0))) {
				return readNumber(source, ofs, token, adjacent);
			}
		}
		int i = findEndText(source, ofs);
		if (i == -1) {
			i = source.length();
		}
		String fragment = source.substring(ofs, i);
		for (String month : getDateFormatSymbols().getMonths()) {
			if (fragment.equalsIgnoreCase(month)) {
				return source.substring(ofs, ofs + month.length());
			}
		}
		for (String month : getDateFormatSymbols().getShortMonths()) {
			if (fragment.equalsIgnoreCase(month)) {
				return source.substring(ofs, ofs + month.length());
			}
		}
		return null;
	}

	/**
	 * Parse a month value to an offset from Calendar.JANUARY. The source month
	 * value can be numeric (1-12), a shortform or longform month name as
	 * defined in DateFormatSymbols.
	 * 
	 * @param month as a string.
	 * @param offset the offset of original timestamp where month started, for
	 *            error reporting.
	 * @return month as an offset from Calendar.JANUARY.
	 * @see DateFormatSymbols
	 * @throws ParseException if the source could not be parsed.
	 */
	int parseMonth(String month, int offset) throws ParseException {
		if (month.length() < 3) {
			return (parseNumber(month, offset, "month", 1, 12) - 1) + Calendar.JANUARY;
		}
		String months[] = getDateFormatSymbols().getMonths();
		for (int i = 0; i < months.length; i++) {
			if (month.equalsIgnoreCase(months[i])) {
				return i + Calendar.JANUARY;
			}
		}
		months = getDateFormatSymbols().getShortMonths();
		for (int i = 0; i < months.length; i++) {
			if (month.equalsIgnoreCase(months[i])) {
				return i + Calendar.JANUARY;
			}
		}
		return throwInvalid("month", offset);
	}

	/**
	 * Read the timezone string. Does not attempt to parse.
	 * 
	 * @param source full timestamp
	 * @param ofs offset within timestamp where timezone starts
	 * @return the timezone as a string or null if error reading.
	 * @see #parseTimeZone(String)
	 */
	String readTimeZone(String source, int ofs) {
		int sp = source.indexOf(' ', ofs);
		String fragment;
		if (sp != -1) {
			fragment = source.substring(ofs, sp);
		} else {
			fragment = source.substring(ofs);
		}
		int len = fragment.length();
		// handle zulu
		if (len == 1) {
			return fragment.equals("z") ? source.substring(ofs, 1) : null;
		}
		// 8 is length of "GMT-H:MM"
		if (len >= 8 && fragment.startsWith(GMT)) {
			return source.substring(ofs);
		}
		int ch = fragment.charAt(0);
		if (len >= 5 && (ch == SIGN_NEGATIVE || ch == SIGN_POSITIVE)) {
			return source.substring(ofs, ofs + 5);
		}
		for (String timezone[] : getDateFormatSymbols().getZoneStrings()) {
			for (String z : timezone) {
				if (z.equalsIgnoreCase(fragment)) {
					return source.substring(ofs, ofs + z.length());
				}
			}
		}
		return null;
	}

	/**
	 * Parse the timezone to an offset from GMT in minutes. The source can be
	 * RFC-822 (ie. -0400), ISO8601 (ie. GMT+08:50), or TimeZone ID (ie. PDT,
	 * America/New_York, etc). This method does not adjust for DST.
	 * 
	 * @param source source timezone.
	 * @param offset the offset of original timestamp where month started, for
	 *            error reporting.
	 * @return offset from GMT in minutes.
	 * @throws ParseException if the source could not be parsed.
	 */
	int parseTimeZone(String source, int ofs) throws ParseException {
		char tzSign = source.charAt(0);
		// handle RFC822 style GMT offset (-0500)
		if (tzSign == SIGN_NEGATIVE || tzSign == SIGN_POSITIVE) {
			source = source.substring(1);
			// set the index to point to divider between hours
			// and minutes. Hour can be one or two digits, minutes
			// is always 2 digits.
			int index = 2;
			if (source.length() == 3) {
				index--;
			}
			int tzHours = parseNumber(source.substring(0, index), ofs, "timezone", 0, 23);
			int tzMinutes = parseNumber(source.substring(index), ofs, "timezone", 0, 59);
			tzMinutes += tzHours * 60;
			if (tzSign != SIGN_NEGATIVE) {
				tzMinutes = -tzMinutes;
			}
			return tzMinutes;
		}
		// handle explicit GMT offset (GMT+H:MM)
		if (source.startsWith(GMT)) {
			int index = source.indexOf(':');
			if (index != -1) {
				source = source.substring(3, index) + source.substring(index + 1);
			} else {
				source = source.substring(3);
			}
			return parseTimeZone(source, ofs);
		}
		// Handle timezone based on ID or full name
		for (String timezone[] : getDateFormatSymbols().getZoneStrings()) {
			for (String z : timezone) {
				if (z.equalsIgnoreCase(source)) {
					TimeZone tz = TimeZone.getTimeZone(timezone[DateFormatSymbols.ZONE_ID]);
					return -(tz.getRawOffset() / MILLIS_TO_MINUTES);
				}
			}
		}
		return throwInvalid("timezone", ofs);
	}

	/**
	 * Attempt to find the end of a field if the length is not known.
	 * 
	 * @param source the full source timestamp
	 * @param ofs index of where current field starts.
	 * @return the index of the end of field, or -1 if couldn't determine.
	 */
	int findEndText(String source, int ofs) {
		for (int i = ofs; i < source.length(); i++) {
			if (isAlpha(source.charAt(i)) == false && isNumeric(source.charAt(i)) == false) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Test if a character is alpha (A-Z,a-z).
	 */
	boolean isAlpha(char ch) {
		return ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z'));
	}

	/**
	 * Test if a character is number (0-9).
	 */
	boolean isNumeric(char ch) {
		return (ch >= '0' && ch <= '9');
	}

	/**
	 * Parse the date pattern.
	 * 
	 * The list will contain each token of the pattern. The first character of
	 * the token contains the pattern component type, or wildcard (*) for
	 * literal patterns.
	 * 
	 * @param pattern
	 * @return parsed pattern.
	 */
	List<String> parseDatePattern(String pattern) {
		List<String> tokens = new Vector<String>();
		String tmp = null;
		for (int i = 0; i < pattern.length(); i++) {
			char ch = pattern.charAt(i);
			// Handle literal text enclosed in quotes
			if (ch == EXPLICIT_LITERAL) {
				int n = pattern.indexOf(EXPLICIT_LITERAL, i + 1);
				if (n != -1) {
					if (tmp != null) {
						tokens.add(tmp.charAt(0) + tmp);
						tmp = null;
					}
					tokens.add(LITERAL_LETTER + pattern.substring(i + 1, n));
				}
				i = n;
				continue;
			}
			// Any invalid non-alpha characters are treated as literal text.
			// invalid alpha characters are illegal.
			boolean isValid = PATTERN_LETTERS.indexOf(ch) != -1;
			if (isValid == false) {
				if (tmp != null) {
					tokens.add(tmp.charAt(0) + tmp);
					tmp = null;
				}
				int n;
				for (n = i; n < pattern.length(); n++) {
					ch = pattern.charAt(n);
					if (PATTERN_LETTERS.indexOf(ch) != -1) {
						break;
					}
					if (isAlpha(ch)) {
						throw new IllegalArgumentException("Illegal pattern character: " + ch);
					}
				}
				tokens.add(LITERAL_LETTER + pattern.substring(i, n));
				i = n - 1;
				continue;
			}
			if (tmp == null) {
				tmp = String.valueOf(ch);
				continue;
			} else if (ch == tmp.charAt(0)) {
				tmp += ch;
			} else {
				tokens.add(tmp.charAt(0) + tmp);
				tmp = String.valueOf(ch);
			}
		}
		if (tmp != null) {
			tokens.add(tmp.charAt(0) + tmp);
		}
		return tokens;
	}
}
