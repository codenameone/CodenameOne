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

///
public abstract class UnitConverter {
    /// Value to return if this converter can not handle the `unit` sent in as an argument
    /// to the convert method.
    public static final int UNABLE = -87654312;

    /// Converts `value` to pixels.
    ///
    /// #### Parameters
    ///
    /// - `value`: The value to be converted.
    ///
    /// - `unit`: The unit of `value`. Never `null` and at least one character.
    ///
    /// - `refValue`: @param refValue Some reference value that may of may not be used. If the unit is percent for instance this value
    ///                 is the value to take the percent from. Usually the size of the parent component in the appropriate dimension.
    ///
    /// - `isHor`: If the value is horizontal (`true`) or vertical (`false`).
    ///
    /// - `parent`: @param parent   The parent of the target component that `value` is to be applied to.
    ///                 Might for instance be needed to get the screen that the component is on in a multi screen environment.
    ///
    ///
    ///                 May be `null` in which case a "best guess" value should be returned.
    ///
    /// - `comp`: The component, if applicable, or `null` if none.
    ///
    /// #### Returns
    ///
    /// The number of pixels if `unit` is handled by this converter, `UnitConverter.UNABLE` if not.
    public abstract int convertToPixels(float value, String unit, boolean isHor, float refValue, ContainerWrapper parent, ComponentWrapper comp);
}
