package com.codename1.ui.layouts.mig;

/*
 * License (BSD):
 * ==============
 *
 * Copyright (c) 2004, Mikael Grev, MiG InfoCom AB. (miglayout (at) miginfocom (dot) com)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this list
 * of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, this
 * list of conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 * Neither the name of the MiG InfoCom AB nor the names of its contributors may be
 * used to endorse or promote products derived from this software without specific
 * prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGE.
 *
 * @version 1.0
 * @author Mikael Grev, MiG InfoCom AB
 *         Date: 2006-sep-08
 */

/** A class to extend if you want to provide more control over where a component is placed or the size of it.
 * <p>
 * Note! Returned arrays from this class will never be altered. This means that caching of arrays in these methods
 * is OK.
 */
public abstract class LayoutCallback
{
	/** Returns a position similar to the "pos" the component constraint.
	 * @param comp The component wrapper that holds the actual component (JComponent is Swing and Control in SWT).
	 * <b>Should not be altered.</b>
	 * @return The [x, y, x2, y2] as explained in the documentation for "pos". If <code>null</code>
	 * is returned nothing is done and this is the default.
	 * @see UnitValue
	 * @see net.miginfocom.layout.ConstraintParser#parseUnitValue(String, boolean)
	 */
	public UnitValue[] getPosition(ComponentWrapper comp)
	{
		return null;
	}

	/** Returns a size similar to the "width" and "height" in the component constraint.
	 * @param comp The component wrapper that holds the actual component (JComponent is Swing and Control in SWT).
	 * <b>Should not be altered.</b>
	 * @return The [width, height] as explained in the documentation for "width" and "height". If <code>null</code>
	 * is returned nothing is done and this is the default.
	 * @see net.miginfocom.layout.BoundSize
	 * @see net.miginfocom.layout.ConstraintParser#parseBoundSize(String, boolean, boolean)
	 */
	public BoundSize[] getSize(ComponentWrapper comp)
	{
		return null;
	}

	/** A last minute change of the bounds. The bound for the layout cycle has been set and you can correct there
	 * after any set of rules you like.
	 * @param comp The component wrapper that holds the actual component (JComponent is Swing and Control in SWT).
	 */
	public void correctBounds(ComponentWrapper comp)
	{
	}
}
