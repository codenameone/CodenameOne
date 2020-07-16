package com.codename1.impl.javase.cef;

import com.codename1.io.Log;
import java.io.IOException;

import org.cef.callback.CefCallback;
import org.cef.handler.CefResourceHandlerAdapter;
import org.cef.misc.IntRef;
import org.cef.misc.StringRef;
import org.cef.network.CefRequest;
import org.cef.network.CefResponse;

public class ResourceHandler extends CefResourceHandlerAdapter {
    private StreamWrapper stream;

    
    
    @Override
    public boolean processRequest(CefRequest request, CefCallback callback) {
        String url = request.getURL();
        String streamId = null;
        if (url.indexOf("/") != -1) {
            streamId = url.substring(url.lastIndexOf("/")+1);
        }
        if (streamId != null) {
            stream = BrowserPanel.getStreamRegistry().getStream(streamId);
            
            
        }

        callback.Continue();
        return true;
    }

    @Override
    public void getResponseHeaders(
            CefResponse response, IntRef response_length, StringRef redirectUrl) {
        
        response_length.set((int)stream.getLength());
        response.setMimeType(stream.getMimeType());
        response.setStatus(200);
    }
    private boolean closed;
    private int written;
    @Override
    public boolean readResponse(
            byte[] data_out, int bytes_to_read, IntRef bytes_read, CefCallback callback) {


        boolean has_data = true;
        if (closed || stream == null) {
            System.out.println("Stream was closed");
            bytes_read.set(0);
            return false;
        }


        try {

            if (stream.getStream().available() > 0) {
                int read = stream.getStream().read(data_out, 0, bytes_to_read > 0 ? Math.min(bytes_to_read, data_out.length) : data_out.length);
                if (read == -1) {
                    has_data = false;
                    bytes_read.set(0);
                    stream.getStream().close();
                    closed = true;
                    BrowserPanel.getStreamRegistry().removeStream(stream);
                    stream = null;
                    return false;
                } else {
                    written += read;
                    //System.out.println("Zero bytes were available");
                    long oldOffset = stream.getOffset();
                    oldOffset += read;
                    stream.setOffset(oldOffset);
                    bytes_read.set(read);
                    
                    if (written == stream.getLength()) {
                        stream.getStream().close();
                        closed = true;
                        BrowserPanel.getStreamRegistry().removeStream(stream);
                        stream = null;
                        //return false;
                    }
                    
                    return true;
                }

            } else {
                bytes_read.set(0);


            }
        } catch (IOException ex) {
            Log.e(ex);
        }


        return has_data;

    }

    @Override
    public void cancel() {
        
        try {
            stream.getStream().close();
            closed = true;
            BrowserPanel.getStreamRegistry().removeStream(stream);
            stream = null;
        } catch (Exception ex){}
    }
}
