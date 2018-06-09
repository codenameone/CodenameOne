/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.ui;

import com.codename1.io.FileSystemStorage;
import com.codename1.io.Log;
import com.codename1.io.Util;
import com.codename1.testing.AbstractTest;
import com.codename1.testing.TestUtils;

import com.codename1.ui.layouts.BorderLayout;
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

    @Override
    public boolean runTest() throws Exception {
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
