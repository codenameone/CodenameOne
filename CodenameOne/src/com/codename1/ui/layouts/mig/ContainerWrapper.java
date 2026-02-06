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

/// A class that wraps a container that contains components.
public interface ContainerWrapper extends ComponentWrapper {
    /// Returns the components of the container that wrapper is wrapping.
    ///
    /// #### Returns
    ///
    /// The components of the container that wrapper is wrapping. Never `null`.
    ComponentWrapper[] getComponents();

    /// Returns the number of components that this parent has.
    ///
    /// #### Returns
    ///
    /// The number of components that this parent has.
    int getComponentCount();

    /// Returns the `LayoutHandler` (in Swing terms) that is handling the layout of this container.
    /// If there exist no such class the method should return the same as `#getComponent()`, which is the
    /// container itself.
    ///
    /// #### Returns
    ///
    /// The layout handler instance. Never `null`.
    Object getLayout();

    /// Returns if this container is using left-to-right component ordering.
    ///
    /// #### Returns
    ///
    /// If this container is using left-to-right component ordering.
    boolean isLeftToRight();

    /// Paints a cell to indicate where it is.
    ///
    /// #### Parameters
    ///
    /// - `x`: The x coordinate to start the drwaing.
    ///
    /// - `y`: The x coordinate to start the drwaing.
    ///
    /// - `width`: The width to draw/fill
    ///
    /// - `height`: The height to draw/fill
    void paintDebugCell(int x, int y, int width, int height);
}
