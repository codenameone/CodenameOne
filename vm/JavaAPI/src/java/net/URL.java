/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package java.net;

import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author shannah
 */
public class URL {
    
    public URL(String spec) {
        throw new UnsupportedOperationException("URL not implemented on this platform");
    }
    
    public URL(String protocol, String host, int port, String file) {
        throw new UnsupportedOperationException("URL not implemented on this platform");
    }
    
    public URL(String protocl, String host, String file) {
        throw new UnsupportedOperationException("URL not implemented on this platform");
    }
    
    public URL(URL context, String spec) {
        throw new UnsupportedOperationException("URL not implemented on this platform");
    }
    
    public URL(URL context, String spec, URLStreamHandler handler) {
        throw new UnsupportedOperationException("URL not implemented on this platform");
    }
    
    public final InputStream openStream() throws IOException {
        return null;
    }
}
