/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *  
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 * 
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * Please contact Codename One through http://www.codenameone.com/ if you 
 * need additional information or have any questions.
 */

package com.codename1.io;

import com.codename1.util.Callback;
import com.codename1.util.FailureCallback;
import com.codename1.util.SuccessCallback;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Utility class used by the webservice proxy code to invoke server code
 *
 * @author Shai Almog
 */
public class WebServiceProxyCall {
    /**
     * Web protocol argument/return type
     */
    public static final int TYPE_VOID = 0;

    /**
     * Web protocol argument/return type
     */
    public static final int TYPE_BYTE = 1;

    /**
     * Web protocol argument/return type
     */
    public static final int TYPE_CHAR = 2;

    /**
     * Web protocol argument/return type
     */
    public static final int TYPE_SHORT = 3;

    /**
     * Web protocol argument/return type
     */
    public static final int TYPE_INT = 4;

    /**
     * Web protocol argument/return type
     */
    public static final int TYPE_LONG = 5;

    /**
     * Web protocol argument/return type
     */
    public static final int TYPE_DOUBLE = 6;

    /**
     * Web protocol argument/return type
     */
    public static final int TYPE_FLOAT = 7;

    /**
     * Web protocol argument/return type
     */
    public static final int TYPE_BOOLEAN = 8;

    /**
     * Web protocol argument/return type
     */
    public static final int TYPE_BYTE_OBJECT = 9; 

    /**
     * Web protocol argument/return type
     */
    public static final int TYPE_CHARACTER_OBJECT = 10;

    /**
     * Web protocol argument/return type
     */
    public static final int TYPE_SHORT_OBJECT = 11;

    /**
     * Web protocol argument/return type
     */
    public static final int TYPE_INTEGER_OBJECT = 12;

    /**
     * Web protocol argument/return type
     */
    public static final int TYPE_LONG_OBJECT = 13;

    /**
     * Web protocol argument/return type
     */
    public static final int TYPE_DOUBLE_OBJECT = 14;

    /**
     * Web protocol argument/return type
     */
    public static final int TYPE_FLOAT_OBJECT = 15;

    /**
     * Web protocol argument/return type
     */
    public static final int TYPE_BOOLEAN_OBJECT = 16;

    /**
     * Web protocol argument/return type
     */
    public static final int TYPE_STRING = 17;

    /**
     * Web protocol argument/return type
     */
    public static final int TYPE_BYTE_ARRAY = 18;

    /**
     * Web protocol argument/return type
     */
    public static final int TYPE_CHAR_ARRAY = 19;

    /**
     * Web protocol argument/return type
     */
    public static final int TYPE_SHORT_ARRAY = 20;

    /**
     * Web protocol argument/return type
     */
    public static final int TYPE_INT_ARRAY = 21;

    /**
     * Web protocol argument/return type
     */
    public static final int TYPE_LONG_ARRAY = 22;

    /**
     * Web protocol argument/return type
     */
    public static final int TYPE_DOUBLE_ARRAY = 23;

    /**
     * Web protocol argument/return type
     */
    public static final int TYPE_FLOAT_ARRAY = 24;

    /**
     * Web protocol argument/return type
     */
    public static final int TYPE_BOOLEAN_ARRAY = 25;

    /**
     * Web protocol argument/return type
     */
    public static final int TYPE_STRING_ARRAY = 26;

    /**
     * Web protocol argument/return type
     */
    public static final int TYPE_EXTERNALIABLE = 1000;

    
    /**
     * Invokes a webservice synchronously and returns result
     * 
     * @param def definition of the webservice request
     * @param arguments the arguments to the service
     * @return the value of the sync call
     * @throws IOException an exception in case of a webservice fail
     */
    public static Object invokeWebserviceSync(WSDefinition def, Object... arguments) throws IOException {
        WSConnection cr = new WSConnection(def, null, arguments);
        NetworkManager.getInstance().addToQueueAndWait(cr);
        int rc = cr.getResponseCode();
        if(rc != 200 && rc != 201) {
            throw new IOException("Server error: " + cr.getResponseCode());
        }
        return cr.returnValue;
    }
    
    /**
     * Invokes a web asynchronously and calls the callback on completion
     * 
     * @param def definition of the webservice request
     * @param scall the return value callback 
     * @param fcall the error callback 
     * @param arguments the arguments to the webservice
     */
    public static void invokeWebserviceASync(WSDefinition def, SuccessCallback scall, 
            FailureCallback fcall, Object... arguments) {
        WSConnection cr = new WSConnection(def, scall, fcall, arguments);
        NetworkManager.getInstance().addToQueue(cr);
    }

    /**
     * Invokes a web asynchronously and calls the callback on completion
     * 
     * @param def definition of the webservice request
     * @param call the return value containing an error callback or value 
     * @param arguments the arguments to the webservice
     */
    public static void invokeWebserviceASync(WSDefinition def, final Callback call, Object... arguments) {
        WSConnection cr = new WSConnection(def, call, arguments);
        NetworkManager.getInstance().addToQueue(cr);
    }
        
    /**
     * Creates a webservice definition object which can be used to invoke the webservice.
     * 
     * @param url the url of the webservice
     * @param serviceName the name of the service method
     * @param returnType the return type for the webservice one of the TYPE_* constants
     * @param argumentTypes the arguments for the webservice using the TYPE_* constants
     * @return a WSDefinition object
     */
    public static WSDefinition defineWebService(String url, String serviceName, int returnType, int... argumentTypes) {
        WSDefinition def = new WSDefinition();
        def.url = url;
        def.name = serviceName;
        def.returnType = returnType;
        def.arguments = argumentTypes;
        return def;
    }
    
    /**
     * Webservice definition type, allows defining the argument values for a specific WS call
     */
    public static class WSDefinition {
        String url;
        String name;
        int returnType;
        int[] arguments;
    }
    
    static class WSConnection extends ConnectionRequest {
        private WSDefinition def;
        private Object[] arguments;
        Object returnValue;
        private SuccessCallback scall;
        private FailureCallback fcall;
        
        public WSConnection(WSDefinition def, Callback call, Object... arguments) {
            this(def, call, call, arguments);
        }
        
        public WSConnection(WSDefinition def, SuccessCallback scall, FailureCallback fcall, Object... arguments) {
            this.def = def;
            setUrl(def.url);
            this.arguments = arguments;
            this.scall = scall;
            this.fcall = fcall;
            setPost(true);
        }
        
        @Override
        protected void postResponse() {
            if(scall != null) {
                scall.onSucess(returnValue);
            }
        }
        
        @Override
        protected void handleErrorResponseCode(int code, String message) {
            if(fcall != null) {
                fcall.onError(this, null, code, message);
            }
        }

        @Override
        protected void handleException(Exception err) {
            if(fcall != null) {
                fcall.onError(this, err, -1, null);
            }
        }
        
        
        @Override
        protected void readResponse(InputStream input) throws IOException {
            DataInputStream dis = new DataInputStream(input);
            
            switch(def.returnType) {
                case TYPE_VOID:
                    return;
                    
                case TYPE_BYTE:
                    returnValue = new Byte(dis.readByte());
                    break;

                case TYPE_CHAR:
                    returnValue = new Character(dis.readChar());
                    break;

                case TYPE_SHORT:
                    returnValue = new Short(dis.readShort());
                    break;

                case TYPE_INT:
                    returnValue = new Integer(dis.readInt());
                    break;

                case TYPE_LONG:
                    returnValue = new Long(dis.readLong());
                    break;

                case TYPE_DOUBLE:
                    returnValue = new Double(dis.readDouble());
                    break;

                case TYPE_FLOAT:
                    returnValue = new Float(dis.readFloat());
                    break;

                case TYPE_BOOLEAN:
                    returnValue = new Boolean(dis.readBoolean());
                    break;

                case TYPE_BYTE_OBJECT:
                    if(dis.readBoolean()) {
                        returnValue = new Byte(dis.readByte());
                    }
                    break;

                case TYPE_CHARACTER_OBJECT:
                    if(dis.readBoolean()) {
                        returnValue = new Character(dis.readChar());
                    }
                    break;

                case TYPE_SHORT_OBJECT:
                    if(dis.readBoolean()) {
                        returnValue = new Short(dis.readShort());
                    }
                    break;

                case TYPE_INTEGER_OBJECT:
                    if(dis.readBoolean()) {
                        returnValue = new Integer(dis.readInt());
                    }
                    break;

                case TYPE_LONG_OBJECT:
                    if(dis.readBoolean()) {
                        returnValue = new Long(dis.readLong());
                    }
                    break;

                case TYPE_DOUBLE_OBJECT:
                    if(dis.readBoolean()) {
                        returnValue = new Double(dis.readDouble());
                    }
                    break;

                case TYPE_FLOAT_OBJECT:
                    if(dis.readBoolean()) {
                        returnValue = new Float(dis.readFloat());
                    }
                    break;

                case TYPE_BOOLEAN_OBJECT:
                    if(dis.readBoolean()) {
                        returnValue = new Boolean(dis.readBoolean());
                    }
                    break;

                case TYPE_STRING:
                    if(dis.readBoolean()) {
                        returnValue = dis.readUTF();
                    }
                    break;

                case TYPE_BYTE_ARRAY: {
                    int size = dis.readInt();
                    if(size > -1) {
                        byte[] b = new byte[size];
                        for(int iter = 0 ; iter < size ; iter++) {
                            b[iter] = dis.readByte();
                        }
                        returnValue = b;
                    }
                    break;
                }

                case TYPE_CHAR_ARRAY: {
                    int size = dis.readInt();
                    if(size > -1) {
                        char[] b = new char[size];
                        for(int iter = 0 ; iter < size ; iter++) {
                            b[iter] = dis.readChar();
                        }
                        returnValue = b;
                    }
                    break;
                }

                case TYPE_SHORT_ARRAY: {
                    int size = dis.readInt();
                    if(size > -1) {
                        short[] b = new short[size];
                        for(int iter = 0 ; iter < size ; iter++) {
                            b[iter] = dis.readShort();
                        }
                        returnValue = b;
                    }
                    break;
                }

                case TYPE_INT_ARRAY: {
                    int size = dis.readInt();
                    if(size > -1) {
                        int[] b = new int[size];
                        for(int iter = 0 ; iter < size ; iter++) {
                            b[iter] = dis.readInt();
                        }
                        returnValue = b;
                    }
                    break;
                }

                case TYPE_LONG_ARRAY: {
                    int size = dis.readInt();
                    if(size > -1) {
                        long[] b = new long[size];
                        for(int iter = 0 ; iter < size ; iter++) {
                            b[iter] = dis.readLong();
                        }
                        returnValue = b;
                    }
                    break;
                }

                case TYPE_DOUBLE_ARRAY: {
                    int size = dis.readInt();
                    if(size > -1) {
                        double[] b = new double[size];
                        for(int iter = 0 ; iter < size ; iter++) {
                            b[iter] = dis.readDouble();
                        }
                        returnValue = b;
                    }
                    break;
                }

                case TYPE_FLOAT_ARRAY: {
                    int size = dis.readInt();
                    if(size > -1) {
                        float[] b = new float[size];
                        for(int iter = 0 ; iter < size ; iter++) {
                            b[iter] = dis.readFloat();
                        }
                        returnValue = b;
                    }
                    break;
                }

                case TYPE_BOOLEAN_ARRAY: {
                    int size = dis.readInt();
                    if(size > -1) {
                        boolean[] b = new boolean[size];
                        for(int iter = 0 ; iter < size ; iter++) {
                            b[iter] = dis.readBoolean();
                        }
                        returnValue = b;
                    }
                    break;
                }

                case TYPE_STRING_ARRAY: {
                    int size = dis.readInt();
                    if(size > -1) {
                        String[] b = new String[size];
                        for(int iter = 0 ; iter < size ; iter++) {
                            b[iter] = dis.readUTF();
                        }
                        returnValue = b;
                    }
                    break;
                }

                case TYPE_EXTERNALIABLE:
                    returnValue = Util.readObject(dis);
                    break;

                default:
                    throw new RuntimeException("Unrecognized type: " + returnValue);
            }
        }
        
        
        @Override
        protected void buildRequestBody(OutputStream os) throws IOException {
            DataOutputStream dos = new DataOutputStream(os);
            dos.writeUTF(def.name);
            int alen = arguments.length;
            for(int iter = 0 ; iter < alen ; iter++) {
                switch(def.arguments[iter]) {
                    case TYPE_BYTE:
                        dos.writeByte(((Byte)arguments[iter]).byteValue());
                        break;
                        
                    case TYPE_CHAR:
                        dos.writeChar(((Character)arguments[iter]).charValue());
                        break;
                        
                    case TYPE_SHORT:
                        dos.writeShort(((Short)arguments[iter]).shortValue());
                        break;
                        
                    case TYPE_INT:
                        dos.writeInt(((Integer)arguments[iter]).intValue());
                        break;
                        
                    case TYPE_LONG:
                        dos.writeLong(((Long)arguments[iter]).longValue());
                        break;
                        
                    case TYPE_DOUBLE:
                        dos.writeDouble(((Double)arguments[iter]).doubleValue());
                        break;
                        
                    case TYPE_FLOAT:
                        dos.writeFloat(((Float)arguments[iter]).floatValue());
                        break;
                        
                    case TYPE_BOOLEAN:
                        dos.writeBoolean(((Boolean)arguments[iter]).booleanValue());
                        break;
                        
                    case TYPE_BYTE_OBJECT:
                        if(arguments[iter] != null) {
                            dos.writeBoolean(true);
                            dos.writeByte(((Byte)arguments[iter]).byteValue());
                        } else {
                            dos.writeBoolean(false);
                        }
                        break;
                        
                    case TYPE_CHARACTER_OBJECT:
                        if(arguments[iter] != null) {
                            dos.writeBoolean(true);
                            dos.writeChar(((Character)arguments[iter]).charValue());
                        } else {
                            dos.writeBoolean(false);
                        }
                        break;
                        
                    case TYPE_SHORT_OBJECT:
                        if(arguments[iter] != null) {
                            dos.writeBoolean(true);
                            dos.writeShort(((Short)arguments[iter]).shortValue());
                        } else {
                            dos.writeBoolean(false);
                        }
                        break;
                        
                    case TYPE_INTEGER_OBJECT:
                        if(arguments[iter] != null) {
                            dos.writeBoolean(true);
                            dos.writeInt(((Integer)arguments[iter]).intValue());
                        } else {
                            dos.writeBoolean(false);
                        }
                        break;
                        
                    case TYPE_LONG_OBJECT:
                        if(arguments[iter] != null) {
                            dos.writeBoolean(true);
                            dos.writeLong(((Long)arguments[iter]).longValue());
                        } else {
                            dos.writeBoolean(false);
                        }
                        break;
                        
                    case TYPE_DOUBLE_OBJECT:
                        if(arguments[iter] != null) {
                            dos.writeBoolean(true);
                            dos.writeDouble(((Double)arguments[iter]).doubleValue());
                        } else {
                            dos.writeBoolean(false);
                        }
                        break;
                        
                    case TYPE_FLOAT_OBJECT:
                        if(arguments[iter] != null) {
                            dos.writeBoolean(true);
                            dos.writeFloat(((Float)arguments[iter]).floatValue());
                        } else {
                            dos.writeBoolean(false);
                        }
                        break;
                        
                    case TYPE_BOOLEAN_OBJECT:
                        if(arguments[iter] != null) {
                            dos.writeBoolean(true);
                            dos.writeBoolean(((Boolean)arguments[iter]).booleanValue());
                        } else {
                            dos.writeBoolean(false);
                        }
                        break;
                        
                    case TYPE_STRING:
                        if(arguments[iter] != null) {
                            dos.writeBoolean(true);
                            dos.writeUTF((String)arguments[iter]);
                        } else {
                            dos.writeBoolean(false);
                        }
                        break;
                        
                    case TYPE_BYTE_ARRAY:
                        if(arguments[iter] != null) {
                            byte[] b = (byte[])arguments[iter];
                            dos.writeInt(b.length);
                            for(byte bb : b) {
                                dos.writeByte(bb);
                            }
                        } else {
                            dos.writeInt(-1);
                        }
                        break;
                        
                    case TYPE_CHAR_ARRAY:
                        if(arguments[iter] != null) {
                            char[] b = (char[])arguments[iter];
                            dos.writeInt(b.length);
                            for(char bb : b) {
                                dos.writeChar(bb);
                            }
                        } else {
                            dos.writeInt(-1);
                        }
                        break;
                        
                    case TYPE_SHORT_ARRAY:
                        if(arguments[iter] != null) {
                            short[] b = (short[])arguments[iter];
                            dos.writeInt(b.length);
                            for(short bb : b) {
                                dos.writeShort(bb);
                            }
                        } else {
                            dos.writeInt(-1);
                        }
                        break;
                        
                    case TYPE_INT_ARRAY:
                        if(arguments[iter] != null) {
                            int[] b = (int[])arguments[iter];
                            dos.writeInt(b.length);
                            for(int bb : b) {
                                dos.writeInt(bb);
                            }
                        } else {
                            dos.writeInt(-1);
                        }
                        break;
                        
                    case TYPE_LONG_ARRAY:
                        if(arguments[iter] != null) {
                            long[] b = (long[])arguments[iter];
                            dos.writeInt(b.length);
                            for(long bb : b) {
                                dos.writeLong(bb);
                            }
                        } else {
                            dos.writeInt(-1);
                        }
                        break;
                        
                    case TYPE_DOUBLE_ARRAY:
                        if(arguments[iter] != null) {
                            double[] b = (double[])arguments[iter];
                            dos.writeInt(b.length);
                            for(double bb : b) {
                                dos.writeDouble(bb);
                            }
                        } else {
                            dos.writeInt(-1);
                        }
                        break;
                        
                    case TYPE_FLOAT_ARRAY:
                        if(arguments[iter] != null) {
                            float[] b = (float[])arguments[iter];
                            dos.writeInt(b.length);
                            for(float bb : b) {
                                dos.writeFloat(bb);
                            }
                        } else {
                            dos.writeInt(-1);
                        }
                        break;
                        
                    case TYPE_BOOLEAN_ARRAY:
                        if(arguments[iter] != null) {
                            boolean[] b = (boolean[])arguments[iter];
                            dos.writeInt(b.length);
                            for(boolean bb : b) {
                                dos.writeBoolean(bb);
                            }
                        } else {
                            dos.writeInt(-1);
                        }
                        break;
                        
                    case TYPE_STRING_ARRAY:
                        if(arguments[iter] != null) {
                            String[] b = (String[])arguments[iter];
                            dos.writeInt(b.length);
                            for(String bb : b) {
                                dos.writeUTF(bb);
                            }
                        } else {
                            dos.writeInt(-1);
                        }
                        break;
                        
                    case TYPE_EXTERNALIABLE:
                        Util.writeObject(arguments[iter], dos);
                        break;

                    default:
                        throw new RuntimeException("Unrecognized type: " + def.arguments[iter]);
                }
            }
        }
        
    }
}
