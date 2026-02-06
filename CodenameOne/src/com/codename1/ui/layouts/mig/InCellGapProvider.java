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

/// An interface to implement if you want to decide the gaps between two types of components within the same cell.
///
/// E.g.:
///
/// ```java
/// `if (adjacentComp == null || adjacentSide == SwingConstants.LEFT || adjacentSide == SwingConstants.TOP)
///       return null;
///
/// boolean isHor = (adjacentSide == SwingConstants.LEFT || adjacentSide == SwingConstants.RIGHT);
///
/// if (adjacentComp.getComponentType(false) == ComponentWrapper.TYPE_LABEL && comp.getComponentType(false) == ComponentWrapper.TYPE_TEXT_FIELD)
///    return isHor ? UNRELATED_Y : UNRELATED_Y;
///
/// return (adjacentSide == SwingConstants.LEFT || adjacentSide == SwingConstants.RIGHT) ? RELATED_X : RELATED_Y;`
/// ```
public interface InCellGapProvider {
    /// Returns the default gap between two components that **are in the same cell**.
    ///
    /// #### Parameters
    ///
    /// - `comp`: The component that the gap is for. Never `null`.
    ///
    /// - `adjacentComp`: The adjacent component if any. May be `null`.
    ///
    /// - `adjacentSide`: @param adjacentSide What side the `adjacentComp` is on. `javax.swing.SwingUtilities#TOP` or
    /// `javax.swing.SwingUtilities#LEFT` or `javax.swing.SwingUtilities#BOTTOM` or `javax.swing.SwingUtilities#RIGHT`.
    ///
    /// - `tag`: The tag string that the component might be tagged with in the component constraints. May be `null`.
    ///
    /// - `isLTR`: If it is left-to-right.
    ///
    /// #### Returns
    ///
    /// The default gap between two components or `null` if there should be no gap.
    BoundSize getDefaultGap(ComponentWrapper comp, ComponentWrapper adjacentComp, int adjacentSide, String tag, boolean isLTR);
}
