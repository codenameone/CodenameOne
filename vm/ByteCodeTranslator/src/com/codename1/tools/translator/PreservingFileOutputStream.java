package com.codename1.tools.translator;

import java.io.*;
import java.util.Arrays;
import java.util.HashSet;

/**
 * Created by admin on 8/17/15.
 */
public class PreservingFileOutputStream extends OutputStream {

    final public static String NEW_SUFFIX = ".new_by_translator";

    static int total;
    static int preserved;
    boolean equal;

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    File file;


    public PreservingFileOutputStream(File file) throws FileNotFoundException {
        this.file = file;
        total++;
    }

    @Override
    public void write(int b) throws IOException {
        baos.write(b);
    }

    @Override
    public void close() throws IOException {
        equal = false;
        if (file.exists() && file.length() == baos.size()) {
            byte[] oldCopy = new byte[baos.size()];
            FileInputStream fis = new FileInputStream(file);
            fis.read(oldCopy);
            fis.close();
            final byte[] thisCopy = baos.toByteArray();
            equal = true;
            for (int i = 0; i < thisCopy.length; i++) {
                if (thisCopy[i] != oldCopy[i]) {
                    equal = false;
                    break;
                }
            }
            if (equal) {
                preserved++;
                return;
            }
        }
        System.out.println("Producing(stream): " + file.getName());
        FileOutputStream x = new FileOutputStream(file);
        baos.writeTo(x);
        x.close();
    }


    /**
     * move temporary created file to its final destination. If final destination doesn't differ from
     * new file, do nothing (remove new file), thus keeping timestamps.
     */
    public static boolean preservingMove(File from, File to) throws IOException {
        if (from.exists() && to.exists() && to.length() == from.length()) {
            byte[] thisCopy = new byte[(int)from.length()];
            FileInputStream fis = new FileInputStream(from);
            fis.read(thisCopy);
            fis.close();

            byte[] oldCopy = new byte[(int)to.length()];
            fis = new FileInputStream(to);
            fis.read(oldCopy);
            fis.close();

            boolean equal = true;
            for (int i = 0; i < thisCopy.length; i++) {
                if (thisCopy[i] != thisCopy[i]) {
                    equal = false;
                    break;
                }
            }

            if (equal) {
                // preserve
                return from.delete();   // keep old
            }
        }
        if(ByteCodeTranslator.verbose) {
            System.out.println("Producing(move): " + to.getName());
        }
        to.delete();
        return from.renameTo(to);
    }

    public static boolean finishWithNewFile(File newFile) throws IOException {
        String path = newFile.getPath();
        if (path.endsWith(NEW_SUFFIX)) {
            String finalDestination = path.substring(0, path.length() - NEW_SUFFIX.length());
            return preservingMove(newFile, new File(finalDestination));
        }
        return true;    // not a new file
    }
}
