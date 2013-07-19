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

/**
 * Common patterns for dates, times, and timestamps.
 * 
 * @author Eric Coolman
 *
 */
public class DateFormatPatterns {
	/**
	 * Pattern for parsing/formatting RFC-2822 timestamp.
	 */
	public static final String RFC2822 = "EEE, dd MMM yyyy HH:mm:ss Z";
	/**
	 * Pattern for parsing/formatting RFC-822 timestamp.
	 */
	public static final String RFC822 = RFC2822;
	/**
	 * Pattern for parsing/formatting ISO8601 timestamp.
	 */
	public static final String ISO8601 = "yyyy-MM-dd'T'HH:mm:ssZ";
	/**
	 * Pattern for parsing/formatting Twitter timeline timestamp.
	 */
	public static final String TWITTER_TIMELINE = "EEE MMM dd HH:mm:ss ZZZ yyyy";
	/**
	 * Pattern for parsing/formatting Twitter search timestamp.
	 */
	public static final String TWITTER_SEARCH = "EEE, dd MMM yyyy HH:mm:ss Z";
	/**
	 * Pattern for a verbose date
	 */
	public static final String VERBOSE_DATE = "EEEE, MMM dd, yyyy";
	/**
	 * Pattern for a verbose time
	 */
	public static final String VERBOSE_TIME = "HH:mm:ssaa z";
	/**
	 * Pattern for a verbose timestamp
	 */
	public static final String VERBOSE_TIMESTAMP = VERBOSE_DATE + ", " + VERBOSE_TIME;
}
