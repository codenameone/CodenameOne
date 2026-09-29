/*
 * Copyright (c) 2018, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.ui;

import com.codename1.io.File;
import com.codename1.io.FileSystemStorage;
import com.codename1.io.Log;
import com.codename1.io.URL;
import com.codename1.io.Util;
import com.codename1.testing.AbstractTest;
import com.codename1.testing.TestUtils;

import com.codename1.ui.layouts.BorderLayout;
import com.codename1.util.StringUtil;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

public class URITests extends AbstractTest {

    private static class Res {

        boolean complete;
        Throwable error;
    }

    
    
    
    @Override
    public boolean runTest() throws Exception {
        FileSystemStorage fs = FileSystemStorage.getInstance();
        String home = fs.getAppHomePath();
        if (!home.endsWith(fs.getFileSystemSeparator()+"")) {
            home += fs.getFileSystemSeparator();
        }
        String homePath = home.substring(6);
        homePath = StringUtil.replaceAll(homePath, "\\", "/");
        while(homePath.startsWith("/")) {
            homePath = homePath.substring(1);
        }
        homePath = "/" + homePath;
        com.codename1.io.URL url = new com.codename1.io.File("hello world.txt").toURL();
        
        //https://en.wikipedia.org/wiki/File_URI_scheme
        assertTrue(url.toString().startsWith("file:/"), "URL should start with file:///");
        assertEqual("file:"+homePath+"hello%20world.txt", url.toString(), "URL failed to encode space in file name");
        
        
        URI uri = new com.codename1.io.File("hello world.txt").toURI();
        assertEqual("file:"+homePath+"hello%20world.txt", uri.toString(), "URI failed to encode space in file name");
        assertEqual(homePath+"hello world.txt", uri.getPath(), "Incorrect URI path");
        return true;

    }

}
