package com.codename1.tools.translator;

import java.io.*;

/**
 * Created by san on 2/13/16.
 */
public class ConcatenatingFileOutputSteeam extends java.io.OutputStream {

    public static final int MODULO = 32;

    ByteArrayOutputStream []dest = new ByteArrayOutputStream[MODULO];
    ByteArrayOutputStream current;
    private File outputDirectory;

    public ConcatenatingFileOutputSteeam(File outputDirectory) {

        this.outputDirectory = outputDirectory;
    }

    public void beginNextFile(String fileid) {
        int destIndex = Math.abs(fileid.hashCode() % MODULO);
        current = dest[destIndex];
        if (current == null) {
            current = dest[destIndex] = new ByteArrayOutputStream();
        }
    }

    @Override
    public void write(int b) throws IOException {
        if (current == null) {
            throw new RuntimeException("beginNextFile() not called.");
        }
        current.write(b);
    }

    @Override
    public void close() {
        current.write('\n');
    }

    public void realClose() throws IOException {
        for (int i = 0; i < dest.length; i++) {
            ByteArrayOutputStream byteArrayOutputStream = dest[i];
            File destFile = new File(outputDirectory, "concatenated_" + i+"."+ByteCodeTranslator.output.extension());
            if (dest == null || dest[i].size() ==0) {
                destFile.delete();
            } else {
                FileOutputStream pfos = new FileOutputStream(destFile);
                pfos.write(byteArrayOutputStream.toByteArray());
                pfos.close();
            }
        }
    }
}
