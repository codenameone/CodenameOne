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
import com.codename1.xml.XMLParser;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Utility class used to simplify the invocation of webservices
 *
 * @author Shai Almog
 */
class WebServiceCall {
    /**
     * Returns type for the webservice
     */
    public static final int RETURN_TYPE_NONE = 0;

    /**
     * Returns type for the webservice
     */
    public static final int RETURN_TYPE_STRING = 1;

    /**
     * Returns type for the webservice
     */
    public static final int RETURN_TYPE_BYTE_ARRAY = 2;

    /**
     * Returns type for the webservice
     */
    public static final int RETURN_TYPE_XML = 3;

    /**
     * Returns type for the webservice
     */
    public static final int RETURN_TYPE_JSON = 4;

    /**
     * Returns type for the webservice
     */
    public static final int RETURN_TYPE_EXTERNALIZED_JSON = 5;

    /**
     * Returns type for the webservice
     */
    public static final int RETURN_TYPE_EXTERNALIZED_XML = 6;


    /**
     * Invokes a webservice synchronously and returns parsed data
     * 
     * @param url the URL of the webservice
     * @param returnValueType the type of the return value RETURN_TYPE_* value
     * @param arguments the arguments to the service
     * @return the value of the sync call
     * @throws IOException an exception in case of a webservice fail
     */
    public static Object invokeWebserviceSync(String url, int returnValueType, Object... arguments) throws IOException {
        ConnectionRequest cr = new ConnectionRequest();
        createWebserviceImpl(cr, url, arguments);
        NetworkManager.getInstance().addToQueueAndWait(cr);
        if(cr.getResponseCode() != 200) {
            throw new IOException("Server error: " + cr.getResponseCode());
        }
        switch(returnValueType) {
            case RETURN_TYPE_STRING:
                return new String(cr.getResponseData(), "UTF-8");
                
            case RETURN_TYPE_BYTE_ARRAY:
                return cr.getResponseData();
                
            case RETURN_TYPE_XML:
                XMLParser xp = new XMLParser();
                return xp.parse(new InputStreamReader(new ByteArrayInputStream(cr.getResponseData()), "UTF-8"));

            case RETURN_TYPE_JSON:
                JSONParser jp = new JSONParser();
                return jp.parse(new InputStreamReader(new ByteArrayInputStream(cr.getResponseData()), "UTF-8"));

            case RETURN_TYPE_EXTERNALIZED_JSON:
                return JSONXMLObjectMap.parseJSON(new InputStreamReader(new ByteArrayInputStream(cr.getResponseData()), "UTF-8"), "codenameOneID");

            case RETURN_TYPE_EXTERNALIZED_XML:
                
        }
        return null;
    }
    
    /**
     * Invokes a web asynchronously and calls the callback on completion
     * 
     * @param url the URL of the webservice
     * @param returnValueType the type of return value expected
     * @param call the return value containing an error callback or value 
     * @param arguments the arguments to the webservice
     */
    public static void invokeWebserviceASync(String url, final int returnValueType, final Callback call, Object... arguments) {
        ConnectionRequest cr = new ConnectionRequest() {
            private Object returnVal; 
            
            @Override
            protected void postResponse() {
                call.onSucess(returnVal);
            }

            @Override
            protected void readResponse(InputStream input) throws IOException {
                switch(returnValueType) {
                    case RETURN_TYPE_STRING:
                        returnVal = Util.readToString(input);
                        break;

                    case RETURN_TYPE_BYTE_ARRAY:
                        returnVal = Util.readInputStream(input);
                        break;

                    case RETURN_TYPE_XML:
                        XMLParser xp = new XMLParser();
                        returnVal = xp.parse(new InputStreamReader(input, "UTF-8"));
                        break;

                    case RETURN_TYPE_JSON:
                        JSONParser jp = new JSONParser();
                        returnVal = jp.parse(new InputStreamReader(input, "UTF-8"));
                        break;

                    case RETURN_TYPE_EXTERNALIZED_JSON:
                        returnVal = JSONXMLObjectMap.parseJSON(new InputStreamReader(input, "UTF-8"), "codenameOneID");
                        break;

                    case RETURN_TYPE_EXTERNALIZED_XML:
                }
            }

            @Override
            protected void handleErrorResponseCode(int code, String message) {
                call.onError(this, null, code, message);
            }

            @Override
            protected void handleException(Exception err) {
                call.onError(this, err, -1, null);
            }
        };
        createWebserviceImpl(cr, url, arguments);
        NetworkManager.getInstance().addToQueue(cr);
    }
    
    private static void createWebserviceImpl(ConnectionRequest cr, String url, Object... arguments) {
        cr.setUrl(url);
        cr.setFailSilently(true);
        for(int iter = 0 ; iter < arguments.length ; iter += 2) {
            if(arguments[iter + 1] != null) {
                continue;
            }
            if(Util.instanceofByteArray(arguments[iter + 1])) {
                byte[] b = (byte[])arguments[iter + 1];
                String[] s = new String[b.length];
                for(int i = 0 ; i < b.length ; i++) {
                    s[i] = "" + b[i];
                }
                cr.addArgument((String)arguments[iter], s);
                continue;
            }
            if(Util.instanceofDoubleArray(arguments[iter + 1])) {
                double[] b = (double[])arguments[iter + 1];
                String[] s = new String[b.length];
                for(int i = 0 ; i < b.length ; i++) {
                    s[i] = "" + b[i];
                }
                cr.addArgument((String)arguments[iter], s);
                continue;
            }
            if(Util.instanceofFloatArray(arguments[iter + 1])) {
                float[] b = (float[])arguments[iter + 1];
                String[] s = new String[b.length];
                for(int i = 0 ; i < b.length ; i++) {
                    s[i] = "" + b[i];
                }
                cr.addArgument((String)arguments[iter], s);
                continue;
            }
            if(Util.instanceofIntArray(arguments[iter + 1])) {
                int[] b = (int[])arguments[iter + 1];
                String[] s = new String[b.length];
                for(int i = 0 ; i < b.length ; i++) {
                    s[i] = "" + b[i];
                }
                cr.addArgument((String)arguments[iter], s);
                continue;
            }
            if(Util.instanceofLongArray(arguments[iter + 1])) {
                long[] b = (long[])arguments[iter + 1];
                String[] s = new String[b.length];
                for(int i = 0 ; i < b.length ; i++) {
                    s[i] = "" + b[i];
                }
                cr.addArgument((String)arguments[iter], s);
                continue;
            }
            if(Util.instanceofShortArray(arguments[iter + 1])) {
                short[] b = (short[])arguments[iter + 1];
                String[] s = new String[b.length];
                for(int i = 0 ; i < b.length ; i++) {
                    s[i] = "" + b[i];
                }
                cr.addArgument((String)arguments[iter], s);
                continue;
            }
            if(arguments[iter + 1] instanceof Mappable || Util.instanceofObjArray(arguments[iter + 1])) {
                cr.addArgument((String)arguments[iter], JSONXMLObjectMap.generateJSON(arguments[iter + 1], "codenameOneID"));
                continue;
            }
            cr.addArgument((String)arguments[iter], arguments[iter + 1].toString());
        }
    }
}
