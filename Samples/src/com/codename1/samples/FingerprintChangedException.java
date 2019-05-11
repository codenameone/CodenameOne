/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.samples;

import java.io.File;
import java.net.URL;

/**
 *
 * @author shannah
 */
class FingerprintChangedException extends Exception {

    /**
     * @return the url
     */
    public URL getUrl() {
        return url;
    }

    /**
     * @return the destFile
     */
    public File getDestFile() {
        return destFile;
    }
    private URL url;
    private File destFile;
    
    public FingerprintChangedException(URL url, File destFile) {
        super("Attempt to download file from url "+url+" but https is required.");
        this.url = url;
        this.destFile = destFile;
    }
}