/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */

package com.codename1.teavm.io;



import com.codename1.impl.html5.HTML5Implementation;
import com.codename1.impl.html5.JSOImplementations;
import com.codename1.io.FileSystemStorage;
import com.codename1.io.Log;
import com.codename1.io.Util;
import com.codename1.teavm.jso.io.Blob;
import com.codename1.teavm.jso.io.FileReader;
import com.codename1.teavm.jso.util.JS;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import com.codename1.html5.js.JSBody;

import com.codename1.html5.js.JSFunctor;
import com.codename1.html5.js.JSObject;
import com.codename1.html5.js.core.JSArray;
import com.codename1.html5.js.core.JSString;
import com.codename1.html5.js.dom.Event;
import com.codename1.html5.js.dom.EventListener;
import com.codename1.html5.js.dom.HTMLCanvasElement;
import com.codename1.html5.js.typedarrays.ArrayBuffer;
import com.codename1.html5.js.typedarrays.Uint8Array;

/**
 *
 * @author shannah
 */
public class BlobUtil {

    public static Blob toType(Blob blob, String mimeType) {
        return _toType(blob, mimeType);
    }
    
    @JSBody(params={"blob", "type"}, script="return new Blob([blob], {type:type})")
    private native static Blob _toType(Blob blob, String type);
    
    
    @JSBody(params={"parts", "type"}, script="return new Blob(parts, {type:type})")
    private native static Blob createBlobNative(JSArray parts, String type);
    
    private static class FileType {
        private final String mimeType;
        private final short[] magic;
        
        
        
        FileType(String mimetype, short[] magic) {
            this.mimeType = mimetype;
            this.magic = magic;
        }

        private boolean matches(Uint8Array buf) {
            int len = buf.getLength();
            if (len >= magic.length) {
                int magicLen = magic.length;
                for (int i=0; i<magicLen; i++) {
                    if (buf.get(i) != magic[i]) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }
    }
    
    // Not used yet.
    private static final FileType[] fileTypes = new FileType[] {
        new FileType("audio/mp3", new short[]{(byte)0xff, (byte)0xfb}),
        new FileType("audio/mp3", new short[]{(byte)0x49, (byte)0x44, (byte)0x33}),
        new FileType("audio/wav", new short[]{(byte)0x52, (byte)0x49, (byte)0x46}),
        new FileType("image/gif", new short[]{0x47, 0x49, 0x46, 0x38, 0x37, 0x61}),
        new FileType("image/gif", new short[]{0x47, 0x49, 0x46, 0x38, 0x39, 0x61}),
        new FileType("image/tif", new short[]{0x49, 0x49, 0x2A, 0x00}),
        new FileType("image/tif", new short[]{0x4D, 0x4D, 0x00, 0x2A}),
        new FileType("image/jpeg", new short[]{0xFF, 0xD8, 0xFF, 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01}),
        new FileType("image/jpeg", new short[]{0xFF, 0xD8, 0xFF, 0xDB}),
        new FileType("image/jpeg", new short[]{0xFF, 0xD8, 0xFF, 0xEE}),
        new FileType("image/png", new short[]{0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}),
        new FileType("application/pdf", new short[]{0x25, 0x50, 0x44, 0x46, 0x2d}),
        new FileType("audio/ogg", new short[]{0x4F, 0x67, 0x67, 0x53}),
        new FileType("image/bmp", new short[]{0x42, 0x4D}),
        new FileType("audio/flac", new short[]{0x66, 0x4C, 0x61, 0x43}),
        

        
        
    };
    
   
    
    public static Blob createBlob(Uint8Array buf, String type) {
        
        JSArray arr = JSArray.create(0);
        arr.push(buf);
        return createBlobNative(arr, type);
    }
    
    public static Blob createBlob(byte[] bytes, String type) {
        int len = bytes.length;
        Uint8Array arr = Uint8Array.create(len);
        for (int i=0; i<len; i++){
            arr.set(i, bytes[i]);
        }
        
        return BlobUtil.createBlob(arr, type);
    }
    
    public static String createObjectURL(Blob blob){
        return createObjectURLNative(blob);
    }
    
    @JSBody(
            params={"blob"},
            script="var url = (typeof URL !== 'undefined' && URL) ? URL : ((typeof window !== 'undefined' && window.webkitURL) ? window.webkitURL : null);"
                    + "return url ? url.createObjectURL(blob) : null;")
    private native static String createObjectURLNative(Blob blob);
    
    public static interface BlobToFileCallback extends JSObject {
        public void complete(String fileName);
        public void error(String error);
    }
    
    
    @JSFunctor
    public static interface BlobToFileFunc extends JSObject {
        public void convertToFile(Blob blob, String fileName, BlobToFileCallback callback);
    }
    
    
    @JSBody(params={"func"}, script="window.saveBlobToFile = func")
    private native static void installNativeBlobToFileConverter(BlobToFileFunc func);
    
    public static void registerNativeBlobToFileConverter() {
        installNativeBlobToFileConverter(new BlobToFileFunc() {
            @Override
            public void convertToFile(final Blob blob, final String fileName, final BlobToFileCallback callback) {
                new Thread() {
                    public void run() {
                        try {
                            String path = fileName;
                            if (path != null && !path.isEmpty()) {
                                InputStream input = openInputStream(blob);
                                OutputStream output = FileSystemStorage.getInstance().openOutputStream(path);
                                Util.copy(input, output);
                                if (input != null) {
                                    try {
                                        input.close();
                                    } catch (IOException ex){}
                                }
                                if (output != null) {
                                    try {
                                        output.close();
                                    } catch (IOException ex){}
                                }
                            } else {
                                path = HTML5Implementation.createTempFile(blob);
                            }
                            callback.complete(path);
                        } catch (Exception ex) {
                            Log.e(ex);
                            callback.error(ex.getMessage());
                        }
                    }
                    
                }.start();
            }
            
        });
    }
    
    public static Blob canvasToBlob(HTMLCanvasElement canvas, String format, double quality) throws IOException {
        
        CanvasExt canvasExt = (CanvasExt)canvas;
        final Object lock = new Object();
        final Blob[] result = new Blob[1];
        final boolean[] complete = new boolean[1];

        canvasExt.toBlob(
                new BlobCallback(){
                    public void onBlob(final Blob blob){
                        new Thread(){
                            public void run(){
                                result[0]=blob;
                                complete[0]=true;
                                synchronized(lock){
                                    lock.notifyAll();
                                }
                            }
                        }.start();

                    }
                }       
                , format, 
                quality
        );

        while (!complete[0]){
            synchronized(lock){
                try {
                    lock.wait(200);
                } catch (InterruptedException ex) {
                    Log.e(ex);
                }
            }
        }

        if (result[0]==null){
            throw new IOException("Failed to generate blob for image");
        }

        return result[0];
    
                
    }    
    
    public static Uint8Array toUint8Array(Blob blob) throws IOException {
        final Object lock = new Object();
        final Uint8Array[] bufs = new Uint8Array[1];
        final boolean[] complete = new boolean[1];
        final String[] errors = new String[1];
        final FileReader reader = createFileReader();
        reader.setOnloadend(new EventListener(){

            @Override
            public void handleEvent(Event evt) {
                new Thread(){
                    public void run(){
                        bufs[0]= Uint8Array.create((ArrayBuffer)reader.getResult());
                        complete[0] = true;
                        synchronized(lock){
                            lock.notifyAll();
                        }
                    }
                }.start();
            }

        });
        reader.setOnerror(new EventListener(){

            @Override
            public void handleEvent(Event evt) {
                errors[0]="Failed to read blob";
                new Thread(){
                    public void run(){
                        synchronized(lock){
                            lock.notifyAll();
                        }
                    }
                }.start();
            }

        });
        reader.readAsArrayBuffer(blob);
            

        while (!complete[0] && errors[0]==null){
            synchronized(lock){
                try {
                    lock.wait();
                } catch (InterruptedException ex) {
                    Log.e(ex);
                }
            }
        }

        if (errors[0] != null){
            throw new IOException(errors[0]);
        }
        return bufs[0];
    }
    
    public static InputStream openInputStream(Blob blob) throws IOException {
        return new ArrayBufferInputStream(toUint8Array(blob), blob.getType());
    }
    
    public static InputStream openInputStream(Uint8Array arr, String type) throws IOException {
        return new ArrayBufferInputStream(arr, type);
    }
    
    private static interface CanvasExt extends HTMLCanvasElement {
        public void toBlob(BlobCallback callback);
        public void toBlob(BlobCallback callback, String type);
        public void toBlob(BlobCallback callback, String type, double quality);
    }
    
    @JSFunctor
    private static interface BlobCallback extends JSObject {
        public void onBlob(Blob blob);
    }
    
    @JSBody(params={"data"}, script="return window.Base64ToBlob(data)")
    private native static Blob base64ToBlob_(String data);
    
    
    public static Blob base64ToBlob(String data) {
        return base64ToBlob_(data);
    }
    
    public static String blobToBase64(Blob blob){
        final String[] out = new String[1];
        final boolean[] complete = new boolean[1];
        final Object lock = new Object();
        blobToBase64Native(blob, new JSOImplementations.DataURLCallback(){

            @Override
            public void callback(final JSString str) {
                new Thread(){
                    public void run(){
                        
                        out[0] = (str != null && !JS.isUndefined(str)) ? str.stringValue() : null;
                        complete[0] = true;
                        synchronized(lock){
                            lock.notifyAll();
                        }
                    }
                }.start();
                
            }
            
        });
        
        while (!complete[0]) {
            synchronized(lock) {
                try {
                    lock.wait();
                } catch (InterruptedException ex) {
                    //Log.e(ex);
                    
                }
            }
        }
        return out[0];
    }
    
    @JSBody(params={}, script="return new FileReader()")
    private native static FileReader createFileReader();
    
    @JSBody(params={"blob", "callback"}, script="window.BlobToBase64(blob, callback)")
    private native static void blobToBase64Native(Blob blob, JSOImplementations.DataURLCallback callback);
}
