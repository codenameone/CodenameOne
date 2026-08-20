/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.html5.js.dom;

/**
 * Interface for the JavaScript PopStateEvent object.
 * https://developer.mozilla.org/en-US/docs/Web/API/PopStateEvent
 *
 * <p>The state is read because popstate fires for forward traversal as well as backward, and
 * the two are only distinguishable by comparing the state the browser restored against the one
 * currently displayed. It is typed as Object rather than String: history state is any
 * structured-cloneable value, and a page that embeds this canvas may keep its router's own
 * object there. Binding it as a String would put that entry through a conversion it cannot
 * satisfy, when all the port needs is to see that the value is not one of its own.</p>
 */
public interface PopStateEvent extends Event {
    Object getState();
}
