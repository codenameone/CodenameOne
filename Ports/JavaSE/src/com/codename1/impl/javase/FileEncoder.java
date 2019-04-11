/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.impl.javase;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author shannah
 */
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
