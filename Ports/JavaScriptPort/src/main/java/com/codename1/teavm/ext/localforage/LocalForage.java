/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */

package com.codename1.teavm.ext.localforage;

import com.codename1.impl.html5.HTML5Implementation;
import com.codename1.teavm.io.BlobUtil;
import com.codename1.teavm.jso.io.Blob;
import com.codename1.teavm.jso.util.JS;
import com.codename1.teavm.jso.util.JSType;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import com.codename1.html5.interop.AsyncCallback;
import com.codename1.html5.js.browser.Window;
import com.codename1.html5.js.typedarrays.Uint8Array;
import com.codename1.html5.js.JSBody;
import com.codename1.html5.js.JSFunctor;
import com.codename1.html5.js.JSObject;
import com.codename1.html5.js.JSProperty;
import com.codename1.html5.js.core.JSArray;
import com.codename1.html5.js.core.JSString;

/**
 *
 * @author shannah
 */
public class LocalForage {

    /**
     * @return the driver
     */
    public static Driver getDriver() {
        return driver;
    }

    /**
     * @param aDriver the driver to set
     */
    public static void setDriver(Driver aDriver) {
        if (instance != null ){
            throw new RuntimeException("LocalForage already initialized.");
        }
        driver = aDriver;
    }

    /**
     * @return the name
     */
    public static String getName() {
        return name;
    }

    /**
     * @param aName the name to set
     */
    public static void setName(String aName) {
        if (instance != null ){
            throw new RuntimeException("LocalForage already initialized.");
        }
        name = aName;
    }

    /**
     * @return the size
     */
    public static int getSize() {
        return size;
    }

    /**
     * @param aSize the size to set
     */
    public static void setSize(int aSize) {
        if (instance != null ){
            throw new RuntimeException("LocalForage already initialized.");
        }
        size = aSize;
    }

    /**
     * @return the storeName
     */
    public static String getStoreName() {
        return storeName;
    }

    /**
     * @param aStoreName the storeName to set
     */
    public static void setStoreName(String aStoreName) {
        if (instance != null ){
            throw new RuntimeException("LocalForage already initialized.");
        }
        storeName = aStoreName;
    }

    /**
     * @return the version
     */
    public static String getVersion() {
        return version;
    }

    /**
     * @param aVersion the version to set
     */
    public static void setVersion(String aVersion) {
        if (instance != null ){
            throw new RuntimeException("LocalForage already initialized.");
        }
        version = aVersion;
    }

    /**
     * @return the description
     */
    public static String getDescription() {
        return description;
    }

    /**
     * @param aDescription the description to set
     */
    public static void setDescription(String aDescription) {
        if (instance != null ){
            throw new RuntimeException("LocalForage already initialized.");
        }
        description = aDescription;
    }
    
    private LocalForageImpl impl;
    
    public static enum Driver {
        INDEXEDDB,
        WEBSQL,
        LOCALSTORAGE
    }
    
    private static Driver driver;
    private static String name;
    private static int size;
    private static String storeName;
    private static String version;
    private static String description;
    
    
    
    private static LocalForage instance;
    
    private LocalForage() {
        
        impl = ((LocalForageFactory)Window.current()).getLocalforage();
        ConfigOptions opts = newConfigOptions();
        if (driver != null) {
            switch (driver){
                case INDEXEDDB:
                    opts.setDriver(impl.getINDEXEDDB());
                    break;
                case WEBSQL:
                    opts.setDriver(impl.getWEBSQL());
                    break;
                case LOCALSTORAGE:
                    opts.setDriver(impl.getLOCALSTORAGE());
                    break;
            }
        }
        
        if (name != null) {
            opts.setName(name);
        }
        
        if (storeName != null) {
            opts.setStoreName(storeName);
        }
        
        if (version != null) {
            opts.setVersion(version);
        }
        
        if (size != 0) {
            opts.setSize(size);
        }
        
        if (description != null) {
            opts.setDescription(description);
        }
        impl.config(opts);
    }
    
    public static LocalForage getInstance() {
        if (instance==null) {
            instance = new LocalForage();
        }
        return instance;
    }
    
    static interface LocalForageFactory extends JSObject {
        @JSProperty
        LocalForageImpl getLocalforage();
    }
    
    static interface LocalForageImpl extends JSObject {
        public void setItem(String key, JSObject value, SetItemCallback callback);
        public void getItem(String key, GetItemCallback callback );
        public void removeItem(String key, SuccessCallback callback);
        public void clear(SuccessCallback callback);
        public void length(LengthCallback callback);
        public void keys(KeysCallback callback);
        public void iterate(IterateCallback callback, SuccessCallback success);

        // Synchronous, value-returning variants (see localforage-shim.js). These
        // are BLOCKING JSO host calls: the worker parks on HOST_CALL and resumes
        // on HOST_CALLBACK with the return value -- no async callback, so no
        // ``while(!done){Thread.sleep(20);}`` poll that would starve the worker
        // message pump and freeze the EDT (the input-freeze bug). localStorage
        // is synchronous on the host, so there is nothing async to await.
        public JSObject getItemSync(String key);
        public JSObject setItemSync(String key, JSObject value);
        public JSObject removeItemSync(String key);
        public JSObject clearSync();
        public JSObject lengthSync();
        public JSArray<JSString> keysSync();

        public void config(ConfigOptions opts);
        
        @JSProperty
        public String getWEBSQL();
        
        @JSProperty
        public String getINDEXEDDB();
        
        @JSProperty
        public String getLOCALSTORAGE();
        
        
    }
    
    public static class QuotaExceededException extends IOException {
        public QuotaExceededException(String msg){
            super(msg);
        }
    }
    
    @JSFunctor
    public static interface SetItemCallback extends JSObject {
        public void callback(JSObject error, JSObject value);
    }
    
    @JSFunctor
    public static interface SuccessCallback extends JSObject {
        public void callback(JSObject error);
    }
    
    @JSFunctor
    public static interface GetItemCallback extends JSObject {
        public void callback(JSObject error, JSObject value);
    }
    
    public String setItem(String key, String value) throws IOException {
        return ((JSString)setItem(key, JSString.valueOf(value))).stringValue();
    }
    
    public JSObject setItem(String key, JSObject value) throws IOException {
        return setValue(impl, key, value);
    }
    
    public JSObject getItem(String key) throws IOException {
        return getValue(impl, key);
    }
    
    public String getString(String key) throws IOException {
        JSObject o = getItem(key);
        if (o==null) {
            return null;
        }
        return ((JSString)o).stringValue();
    }
    
    public <T extends JSObject> T getItem(String key, Class<T> type) throws IOException {
        return (T)getItem(key);
    }
    
    public int getSize(String key) throws IOException {
        JSObject o = getItem(key);
        if (JS.isUndefined(o)) {
            return -1;
        } else if (JS.getType(o) == JSType.STRING){
            throw new RuntimeException("String sizes not implemented yet");
        } else if (JS.getType(o) == JSType.BOOLEAN) {
            throw new RuntimeException("Boolean sizes not implemented yet");
            
        } else if (JS.getType(o) == JSType.NUMBER) {
            throw new RuntimeException("Number sizes not implemented yet");
        } else if (JS.getType(o) == JSType.OBJECT) {
            if ( instanceOf(o, "Blob")) {
                return ((Blob)o).getSize();
            } else if (instanceOf(o, "Uint8Array")) {
                return ((Uint8Array)o).getByteLength();
            }
            byte[] bytes = objectBytes(o);
            if (bytes != null) {
                return bytes.length;
            }
        }
        throw new IOException("Failed to open stream.  Unknown object type.");
    }

    public InputStream openInputStream(String key) throws IOException {
        JSObject o = getItem(key);
        if (JS.isUndefined(o)) {
            return null;
        } else if (JS.getType(o) == JSType.STRING){
            String str = JS.unwrapString(o);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(baos);
            output.writeUTF(str);
            output.close();
            
            return new ByteArrayInputStream(baos.toByteArray());
        } else if (JS.getType(o) == JSType.BOOLEAN) {
            boolean val = JS.unwrapBoolean(o);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(baos);
            output.writeBoolean(val);
            output.close();
            
            return new ByteArrayInputStream(baos.toByteArray());
            
        } else if (JS.getType(o) == JSType.NUMBER) {
            double d = JS.unwrapDouble(o);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(baos);
            output.writeDouble(d);
            output.close();
            return new ByteArrayInputStream(baos.toByteArray());
        } else if (JS.getType(o) == JSType.OBJECT) {
            if ( instanceOf(o, "Blob")) {
                return BlobUtil.openInputStream((Blob)o);
            } else if (instanceOf(o, "Uint8Array")) {
                return BlobUtil.openInputStream((Uint8Array)o, "application/octet-stream");
            }
            byte[] bytes = objectBytes(o);
            if (bytes != null) {
                return new ByteArrayInputStream(bytes);
            }
        }
        throw new IOException("Failed to open stream.  Unknown object type.");
    }
    
    public OutputStream openOutputStream(String key, ItemSavedListener onComplete) throws IOException {
        ItemOutputStream out = new ItemOutputStream(this, key);
        out.setOnComplete(onComplete);
        return out;
    }
    
    public OutputStream openOutputStream(String key) throws IOException {
        return openOutputStream(key, null);
    }
    
    
    public void removeItem(String key) throws IOException {
        removeItem(impl, key);
    }
    
    public void clear() throws IOException {
        clear(impl);
    }
    
    public String[] keys() throws IOException {
        return keys(impl);
    }
    
    public int length() throws IOException {
        return length(impl);
    }
    
    

    private static JSObject setValue(LocalForageImpl impl, String key, JSObject value) throws IOException {
        // Blocking host call that returns synchronously (see LocalForageImpl /
        // localforage-shim.js). No async callback + Thread.sleep poll, so the
        // worker message pump is never starved and the EDT never freezes.
        try {
            return impl.setItemSync(key, value);
        } catch (Throwable t) {
            throw new QuotaExceededException("Failed to set value: " + t);
        }
    }

    private static JSObject getValue(LocalForageImpl impl, String key) throws IOException {
        final Object[] result = new Object[1];
        final IOException[] error = new IOException[1];
        try {
            result[0] = impl.getItemSync(key);
        } catch (Throwable t) {
            error[0] = new IOException("Failed to get value: " + t);
        }

        if (error[0] != null) {
            throw error[0];
        }
        return (JSObject) result[0];
    }

    private static void removeItem(LocalForageImpl impl, String key) throws IOException {
        try {
            impl.removeItemSync(key);
        } catch (Throwable t) {
            throw new IOException("Failed to delete value: " + t);
        }
    }

    private static void clear(LocalForageImpl impl) throws IOException {
        try {
            impl.clearSync();
        } catch (Throwable t) {
            throw new IOException("Failed to clear forage: " + t);
        }
    }
    
    private static String[] keys(LocalForageImpl impl) throws IOException {
        try {
            return JS.unwrapStringArray(impl.keysSync());
        } catch (Throwable t) {
            // Cleanup callers treat empty as "no files to delete"; better than a
            // hard failure.
            return new String[0];
        }
    }

    private static int length(LocalForageImpl impl) throws IOException {
        try {
            return JS.unwrapInt(impl.lengthSync());
        } catch (Throwable t) {
            throw new IOException("Failed to get length: " + t);
        }
    }
    
    
    
    @JSBody(params="type", script="return (typeof globalThis!=='undefined'?globalThis:self)[type];")
    private static native JSObject getJSClassForType(String type);

    // A byte array round-tripped through localforage (which runs on the main
    // thread -- workers have no localStorage) loses its Uint8Array type by the
    // time it returns to the worker: `instanceOf(o,"Uint8Array")` is false and
    // the value is an opaque array-like object. Rather than depend on the type
    // surviving the host<->worker bridge, extract the bytes in JS as a base64
    // string (strings cross the bridge intact) and decode them Java-side.
    // Returns null when the value is not a byte source.
    @JSBody(params="o", script=
        "if (o == null || typeof o !== 'object') { return null; }"
        + "var u8 = null;"
        + "if (o instanceof Uint8Array) { u8 = o; }"
        + "else if (typeof ArrayBuffer !== 'undefined' && ArrayBuffer.isView && ArrayBuffer.isView(o) && !(o instanceof DataView)) { u8 = new Uint8Array(o.buffer, o.byteOffset, o.byteLength); }"
        + "else if (typeof ArrayBuffer !== 'undefined' && o instanceof ArrayBuffer) { u8 = new Uint8Array(o); }"
        + "else { var n = (typeof o.length === 'number') ? o.length : Object.keys(o).length; var a = new Uint8Array(n); for (var i=0;i<n;i++){ var b=o[i]; if (typeof b !== 'number') { return null; } a[i]=b & 255; } u8 = a; }"
        + "var s=''; for (var i=0;i<u8.length;i+=0x8000){ s += String.fromCharCode.apply(null, u8.subarray(i, i+0x8000)); } return btoa(s);")
    private static native String bytesAsBase64(JSObject o);

    private static byte[] objectBytes(JSObject o) {
        try {
            String b64 = bytesAsBase64(o);
            if (b64 != null && b64.length() > 0) {
                return com.codename1.util.Base64.decode(b64.getBytes("UTF-8"));
            }
            if (b64 != null) {
                return new byte[0];
            }
        } catch (Throwable t) {
            // fall through to "unsupported"
        }
        return null;
    }

    // The translated app runs in a Web Worker, where `window` is only a partial
    // shim and does NOT carry built-in constructors like Blob/Uint8Array, so
    // `window[type]` was undefined and `o instanceof undefined` threw
    // ("Right-hand side of 'instanceof' is not an object" / "invalid 'instanceof'
    // operand"). Resolve the constructor off the real worker global (globalThis,
    // falling back to self) and guard that it is callable before the instanceof.
    @JSBody(params={"o", "type"}, script="var t=(typeof globalThis!=='undefined'?globalThis:self)[type]; return (typeof t==='function')?(o instanceof t):false;")
    private static native boolean instanceOf(JSObject o, String type);
    
    
    public static interface ItemSavedListener {
        public void onSave(ItemSavedEvent evt);
    }
    
    public static class ItemSavedEvent {
        private String key;
        private long size;

        /**
         * @return the key
         */
        public String getKey() {
            return key;
        }

        /**
         * @param key the key to set
         */
        public void setKey(String key) {
            this.key = key;
        }

        /**
         * @return the size
         */
        public long getSize() {
            return size;
        }

        /**
         * @param size the size to set
         */
        public void setSize(long size) {
            this.size = size;
        }
    }
    
    private static class ItemOutputStream extends ByteArrayOutputStream {

        private String key;
        private LocalForage inst;
        private ItemSavedListener onComplete;

        ItemOutputStream(LocalForage inst, String key) {
            this.inst = inst;
            this.key = key;
        }
        
        public void setOnComplete(ItemSavedListener r){
            this.onComplete = r;
        }

        @Override
        public void flush() throws IOException {
            super.flush();
            save();
        }

        private void save() throws IOException {
            byte[] bytes = this.toByteArray();
            int len = bytes.length;
            // Bulk-copy the Java byte[] into a Uint8Array in one JS
            // call instead of paying a JSO virtual dispatch per byte.
            Uint8Array arr = BlobUtil.byteArrayToUint8Array(bytes);
            inst.setItem(key, arr);
            if (onComplete != null) {
                ItemSavedEvent evt = new ItemSavedEvent();
                evt.setKey(key);
                evt.setSize(len);
                onComplete.onSave(evt);
            }
        }

        @Override
        public void close() throws IOException {
            save();
            super.close();

        }

    }

    
    private static ConfigOptions newConfigOptions(){
        return ((ConfigOptionsFactory)JS.getGlobal()).createConfigOptions();
    }
    
    private static interface ConfigOptions extends JSObject {
        @JSProperty
        String getDriver();
        
        @JSProperty
        void setDriver(String str);
        
        @JSProperty
        String getDescription();
        
        @JSProperty
        void setDescription(String desc);
        
        @JSProperty
        String getVersion();
        
        @JSProperty
        void setVersion(String ver);
        
        @JSProperty
        String getName();
        
        @JSProperty
        void setName(String name);
        
        @JSProperty
        String getStoreName();
        
        @JSProperty
        void setStoreName(String name);
        
        @JSProperty
        int getSize();
        
        @JSProperty
        void setSize(int size);
    }
    
    private static interface ConfigOptionsFactory extends JSObject {
        @JSBody(params={}, script="return new Object()")
        ConfigOptions createConfigOptions();
    }
    @JSFunctor
    private static interface LengthCallback extends JSObject {
        public void callback(JSObject error, JSObject length);
    }
    
    @JSFunctor
    private static interface KeysCallback extends JSObject {
        public void callback(JSObject error, JSArray<JSString> keys);
    }
    
    @JSFunctor
    private static interface IterateCallback extends JSObject {
        /**
         * Returning a non-undefined value will cause the iteration to end,
         * and the returned value will be passed to success callback
         * @param value
         * @param key
         * @param iterationNumber
         * @return Undefined unless you want iteration to stop.
         */
        public JSObject callback(JSObject value, JSObject key, JSObject iterationNumber);
    }
    
}
