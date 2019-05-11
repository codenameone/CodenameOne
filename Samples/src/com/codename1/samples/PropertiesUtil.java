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
import java.util.Properties;

/**
 *
 * @author shannah
 */
class PropertiesUtil {
    
    static Properties loadProperties(File f) throws IOException {
        
        Properties props = new Properties();
        if (!f.exists()) {
            return props;
        }
        try (FileInputStream fis = new FileInputStream(f)) {
            props.load(fis);
        }
        return props;
    }
    
    
    
    static void saveProperties(Properties p, File f) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(f)) {
            p.store(fos, "Updating properties");
        }
    }
}
