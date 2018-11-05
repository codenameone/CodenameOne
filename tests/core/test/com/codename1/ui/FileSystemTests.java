/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.ui;

import com.codename1.io.File;
import com.codename1.io.FileSystemStorage;
import com.codename1.io.Log;
import com.codename1.io.Util;
import com.codename1.javascript.JavascriptContext;
import com.codename1.testing.AbstractTest;
import com.codename1.testing.TestUtils;

import com.codename1.ui.layouts.BorderLayout;
import com.codename1.util.StringUtil;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 * @author shannah
 */
public class FileSystemTests extends AbstractTest {

    private static class Res {

        boolean complete;
        Throwable error;
    }

    
    
    
    private void testFileClass() throws Exception {
        FileSystemStorage fs = FileSystemStorage.getInstance();
        String appRoot = fs.getAppHomePath();
        if (!appRoot.endsWith(""+fs.getFileSystemSeparator())) {
            appRoot += fs.getFileSystemSeparator();
        }
        
        String appRootPath = appRoot.substring(6);
        appRootPath = StringUtil.replaceAll(appRootPath, "\\", "/");
        while (appRootPath.startsWith("/")) {
            appRootPath = appRootPath.substring(1);
        }
        appRootPath = "/"+appRootPath;
        String appRootUrl = StringUtil.replaceAll("file:"+appRootPath, " ", "%20");
       
        assertTrue(appRoot.startsWith("file:/"), "App root doesn't start with file:/");
        File hello = new File("hello world.txt");
        assertEqual(appRoot+"hello world.txt", hello.getAbsolutePath(), "Incorrect absolute path for file");
        assertEqual(appRootUrl + "hello%20world.txt", hello.toURL().toString(), "Incorrect URL.");
        
        String contents = "hello world "+System.currentTimeMillis();
        //Let's try writing to the file
        try (OutputStream os = fs.openOutputStream(hello.getAbsolutePath())) {
            os.write(contents.getBytes("UTF-8"));
        }
        
        // Read in the contents via the URL's open stream.
        try (InputStream is = hello.toURL().openStream()) {
            String readContent = Util.readToString(is);
            assertEqual(contents, readContent, "Contents file differed after reading using URL.openStream()");
        } catch (IOException fex) {
            throw new RuntimeException("Failed to open stream for url "+hello, fex);
        }
        
        contents = "<!doctype html><html><head><title>Hello World</title></head><body><div id='hello'>Hello World</div></body></html>";
        hello = new File("hello world.html");
        try (OutputStream os = hello.toURL().openConnection().getOutputStream()) {
            os.write(contents.getBytes("UTF-8"));
        }
        
        // Read in the contents via the URL's open stream.
        try (InputStream is = hello.toURL().openStream()) {
            String readContent = Util.readToString(is);
            assertEqual(contents, readContent, "Contents file differed after reading using URL.openStream()");
        } catch (IOException fex) {
            throw new RuntimeException("Failed to open stream for url "+hello, fex);
        }
        
        BrowserComponent bc = new BrowserComponent();
        
        Res res = new Res();
        
        bc.addWebEventListener("onLoad", e->{
            Log.p("URL: "+bc.getURL());
            if (!"Hello World".equals(bc.getTitle())) {
                res.error = new RuntimeException("Incorrect page title.  Should be Hello World but was "+bc.getTitle());
            }
            JavascriptContext ctx = new JavascriptContext(bc);
            Log.p("Inner html: "+ctx.get("document.getElementById('hello').innerHTML"));
            synchronized(res) {
                res.complete = true;
                res.notifyAll();
            }
        });
        bc.setURL(hello.toURL().toString());
        
        while (!res.complete) {
            Display.getInstance().invokeAndBlock(()->{
                Util.sleep(100);
            });
        }
        if (res.error != null) {
            assertNull(res.error, res.error.getMessage());
        }
    }
    
    @Override
    public boolean runTest() throws Exception {
        testFileClass();
        FileSystemStorage fs = FileSystemStorage.getInstance();
        String homePath = fs.getAppHomePath();
        this.assertTrue(homePath.startsWith("file://"), "App Home Path should start with file:// but was " + homePath);

        String testFileContents = "Hello World";
        String testFilePath = homePath + fs.getFileSystemSeparator() + "testfile.txt";
        try (OutputStream os = fs.openOutputStream(testFilePath)) {
            os.write(testFileContents.getBytes("UTF-8"));
        }

        this.assertTrue(fs.exists(testFilePath), "Created file " + testFilePath + " but fs says it doesn't exist");
        String readContents = null;
        try (InputStream is = fs.openInputStream(testFilePath)) {
            byte[] buf = testFileContents.getBytes("UTF-8");
            for (int i = 0; i < buf.length; i++) {
                buf[i] = (byte) 0;
            }
            int len = is.read(buf);
            this.assertEqual(buf.length, len, "Bytes read didn't match expected");
            readContents = new String(buf, "UTF-8");
        }
        this.assertEqual(testFileContents, readContents, "Contents of file doesn't match expected contents after write and read");

        return true;

    }

}
