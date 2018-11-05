/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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

/**
 *
 * @author shannah
 */
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
