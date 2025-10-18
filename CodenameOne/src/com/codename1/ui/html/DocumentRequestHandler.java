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
 * The DocumentRequestHandler interface should be implemented so it returns documents in requested URLs.<br>
 * Concrete classes should handle in its single method all necessary networking and IO issues.<br>
 * Implementations of this interface are used by HTMLComponent to obtain links and form results<br>
 *
 * @author Ofir Leitner
 */
public interface DocumentRequestHandler {

    /**
     * Implementations should return the document in the requested url as an InputStream
     * This is triggered only for the main document requested and not for its resources.
     *
     * @param docInfo A DocumentInfo object representing the requested URL and its attributes
     * @return the document at the URL as an InputStream
     */
    InputStream resourceRequested(DocumentInfo docInfo);

}
