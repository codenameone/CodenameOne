/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.samples;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author shannah
 */
public class FileUtil {
    public static File createTempDirectory(String prefix, String suffix, File parentDirectory) throws IOException {
        final File tmp = File.createTempFile(prefix, suffix, parentDirectory);
        tmp.delete();
        tmp.mkdir();
        Runtime.getRuntime().addShutdownHook(new Thread(()->{
            if (tmp.exists()) {
                delTree(tmp);
            }
        }));
        return tmp;
        
    }
    
    public static boolean delTree(File directory) {
        if (directory.isDirectory()) {
            for (File child : directory.listFiles()) {
                delTree(child);
            }
        }
        return directory.delete();
    }
    
    public static void writeStringToFile(String string, File file) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileOutputStream(file))) {
            writer.append(string);
        }
    }
    
    public static String readFileToString(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            return IOUtil.readToString(fis);
        }
    }
    
    public static long readFileToLong(File file) throws IOException {
        return Long.parseLong(readFileToString(file).trim());
    }
    
    public static List<File> find(File root, String prefix, String suffix) {
        return find(new ArrayList<File>(), root, prefix, suffix);
    }
    
    
    private static List<File> find(List<File> out, File root, String prefix, String suffix) {
        String name = root.getName();
        boolean match = true;
        if (prefix != null && !name.startsWith(prefix)) match = false;
        if (suffix != null && !name.endsWith(suffix)) match = false;
        if (match) out.add(root);
        if (root.isDirectory()) {
            for (File child : root.listFiles()) {
                find(out, child, prefix, suffix);
            }
        }
        return out;
    }
    
    public static void copy(File src, File dest) throws IOException {
        try (FileInputStream fis = new FileInputStream(src)) {
            try (FileOutputStream fos = new FileOutputStream(dest)) {
                IOUtil.copy(fis, fos);
            }
        }
    }
    
}