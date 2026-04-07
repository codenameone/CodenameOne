/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package java.net;

/// @author Eric Coolman
public class URISyntaxException extends Exception {
	private int index;
	private String input;
	private String reason;
	
	/// #### Parameters
	///
	/// - `input`
	///
	/// - `reason`
	public URISyntaxException(String input, String reason) {
		this(input, reason, -1);
	}

	/// #### Parameters
	///
	/// - `input`
	///
	/// - `reason`
	///
	/// - `index`
	public URISyntaxException(String input, String reason, int index) {
		super(reason);
		this.reason = reason;
		this.index = index;
		this.input = input;
	}

	/// #### Returns
	///
	/// the index
	public int getIndex() {
		return index;
	}

	/// #### Parameters
	///
	/// - `index`: the index to set
	void setIndex(int index) {
		this.index = index;
	}

	/// #### Returns
	///
	/// the input
	public String getInput() {
		return input;
	}

	/// #### Parameters
	///
	/// - `input`: the input to set
	void setInput(String input) {
		this.input = input;
	}

	/// #### Returns
	///
	/// the reason
	public String getReason() {
		return reason;
	}

	/// #### Parameters
	///
	/// - `reason`: the reason to set
	void setReason(String reason) {
		this.reason = reason;
	}
}
