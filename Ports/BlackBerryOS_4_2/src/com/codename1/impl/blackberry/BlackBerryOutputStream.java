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
package com.codename1.impl.blackberry;

import java.io.IOException;
import java.io.OutputStream;

/**
 * A facade to ensure only a single flush() operation on HTTP OutputStream to
 * prevent "IllegalStateException: Stream Closed" bug. Affects BlackBerry
 * platform 6.x and earlier. For more details, see here:
 * 
 * http://supportforums.blackberry.com/t5/Java-Development/HTTP-POST-Problem/m-p
 * /59716/highlight/true#M6647
 * 
 * @author Eric Coolman
 * 
 */
public class BlackBerryOutputStream extends OutputStream {
	private OutputStream os;

	/**
	 * Construct with a given HTTP output stream (ie. from
	 * javax.microedition.io.HttpConnection.openOutputStream())
	 * 
	 * @param os an HTTP output stream
	 */
	public BlackBerryOutputStream(OutputStream os) {
		this.os = os;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.OutputStream#write(int)
	 */
	public void write(int b) throws IOException {
		os.write(b);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.OutputStream#write(byte[])
	 */
	public void write(byte[] b) throws IOException {
		os.write(b);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.OutputStream#write(byte[], int, int)
	 */
	public void write(byte[] b, int off, int len) throws IOException {
		os.write(b, off, len);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.OutputStream#flush()
	 */
	public void flush() throws IOException {
		// Ignoring explicit call to flush, for details, see
		// http://supportforums.blackberry.com/t5/Java-Development/HTTP-POST-Problem/m-p/59716/highlight/true#M6647
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.OutputStream#close()
	 */
	public void close() throws IOException {
		os.flush();
		os.close();
	}
}
