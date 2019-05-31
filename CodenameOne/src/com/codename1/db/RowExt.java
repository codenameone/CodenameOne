/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.db;

import java.io.IOException;

/**
 * An extension of the {@link Row} interface to support {@link #wasNull() }.  Not all
 * ports currently implement this interface.  Currently this is supported in iOS, Simulator, 
 * UWP, and Android ports.  Use {@link Database#supportsWasNull(com.codename1.db.Row) } to check
 * whether a row supports wasNull(), and use {@link Database#wasNull(com.codename1.db.Row) } as
 * an abstraction to avoid needing to cast a Row to RowExt.
 * 
 * @author shannah
 * @since 7.0
 */
public interface RowExt extends Row {
    public boolean wasNull() throws IOException;
    
}
