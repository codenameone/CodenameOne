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
package com.codename1.ui.html;

import java.io.InputStream;

/**
 * Provides an interface for Asynchronous request handling.
 * The resourceRequestedAsync will be called without creating a new thread, but it has to return immediately.
 * This can be useful when an external threading mechanism is to be utilized instead of the internal threading which is done by HTMLComponent.
 *
 * Note that the resourceRequested method should be implemented as well, since HTMLComponent has situations in which a resource
 * needs to be fetched immediately (And block all the rest).
 *
 * @author Ofir Leitner
 */
public interface AsyncDocumentRequestHandler extends DocumentRequestHandler {

    /**
     * This method is called by HTMLComponent when a resource is requested asynchronously.
     * This method should return immediately and start a process of fetching the requested resource on another thread.
     * When the resource was fetched, the HTMLComponent.streamReady should be called.
     * 
     * @param docInfo A DocumentInfo object representing the requested URL and its attributes
     * @param callback The HTMLComponent that should be called back when the stream was fetched.
     */
    public void resourceRequestedAsync(DocumentInfo docInfo, IOCallback callback);
}
