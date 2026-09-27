/*
 * Copyright (c) 2019, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.impl.javase;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class FileEncoder {
    private static List<FileEncoder> registeredEncoders = new ArrayList<FileEncoder>();
    
    public synchronized static void register(FileEncoder encoder) {
        registeredEncoders.add(encoder);
    }
    
    public synchronized static FileEncoder getEncoder(String sourceMimetype, String destMimetype) {
        for (FileEncoder enc : registeredEncoders) {
            if (sourceMimetype.equals(enc.getSourceMimetype()) && destMimetype.equals(enc.getTargetMimetype())) {
                return enc;
            }
        }
        return null;
    }
    
    public abstract String getSourceMimetype();
    public abstract String getTargetMimetype();
    public abstract void encode(File sourceFile, File destFile, Object arg) throws IOException;
    
}
