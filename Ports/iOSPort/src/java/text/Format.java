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

/**
 * An abstract class for parsing and formatting localisation sensitive information, compatible with JDK 6.
 * 
 * @see http://docs.oracle.com/javase/6/docs/api/java/text/Format.html
 * @author Eric Coolman
 */
public abstract class Format implements Cloneable {
	/**
	 * Format an object.
	 * 
	 * @param source object to be formatted to text.
	 * @return formatted text.
	 * @throws IllegalArgumentException if the source can not be formatted. 
	 */
	public abstract String format(Object source) throws IllegalArgumentException;

	/**
	 * Parse an string to an object.
	 * 
	 * @param source document to be parsed.
	 * @return parsed object.
	 * @throws ParseException if the source could not be parsed.
	 */
	public abstract Object parseObject(String source) throws ParseException;
}