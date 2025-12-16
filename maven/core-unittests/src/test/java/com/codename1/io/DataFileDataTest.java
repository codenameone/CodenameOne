package com.codename1.io;

import com.codename1.junit.UITestBase;
import com.codename1.io.Data.FileData;
import org.junit.jupiter.api.Assertions;
import com.codename1.junit.FormTest;
import java.io.IOException;

public class DataFileDataTest extends UITestBase {

    @FormTest
    public void testFileData() throws IOException {
        // Data.FileData wraps a File.
        // It has constructor FileData(File file).

        File file = new File("testfile");
        // Mock the file existence/content if needed, but FileData just holds the reference usually.
        // However, FileData.getSize() calls file.length(), which might depend on FS.
        // FileData.appendTo() uses FileSystemStorage.

        FileData fileData = new FileData(file);

        // No getters for file, mimeType in this version of class.
        // It implements Data interface: appendTo, getSize.

        // To test getSize(), we need the file to exist in FileSystemStorage.
        // File wrapper wraps a java.io.File on desktop, or virtual file in CN1.
        // Wait, com.codename1.io.File is a CN1 class.

        // Let's create a file in FileSystemStorage and point to it.
        String filePath = FileSystemStorage.getInstance().getAppHomePath() + "testfile";
        FileSystemStorage.getInstance().openOutputStream(filePath).close(); // Create empty file

        // Or write something
        byte[] content = "Hello".getBytes();
        java.io.OutputStream os = FileSystemStorage.getInstance().openOutputStream(filePath);
        os.write(content);
        os.close();

        File f = new File(filePath);
        FileData fd = new FileData(f);

        Assertions.assertEquals(content.length, fd.getSize());

        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        fd.appendTo(baos);
        Assertions.assertArrayEquals(content, baos.toByteArray());
    }
}
