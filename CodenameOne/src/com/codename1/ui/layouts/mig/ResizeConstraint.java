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

/** A parsed constraint that specifies how an entity (normally column/row or component) can shrink or
 * grow compared to other entities.
 */
final class ResizeConstraint 
{
	static final Float WEIGHT_100 = new Float(100);

	/** How flexilble the entity should be, relative to other entities, when it comes to growing. <code>null</code> or
	 * zero mean it will never grow. An entity that has twise the growWeight compared to another entity will get twice
	 * as much of available space.
	 * <p>
	 * "grow" are only compared within the same "growPrio".
	 */
	Float grow = null;

	/** The relative priority used for determining which entities gets the extra space first.
	 */
	int growPrio = 100;

	Float shrink = WEIGHT_100;

	int shrinkPrio = 100;

	public ResizeConstraint()   // For Externalizable
	{
	}

	ResizeConstraint(int shrinkPrio, Float shrinkWeight, int growPrio, Float growWeight)
	{
		this.shrinkPrio = shrinkPrio;
		this.shrink = shrinkWeight;
		this.growPrio = growPrio;
		this.grow = growWeight;
	}
}
