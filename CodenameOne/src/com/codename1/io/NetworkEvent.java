/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */

package com.codename1.io;

import com.codename1.ui.events.ActionEvent;

/**
 * Event containing more meta data for network events which may be error events or
 * an update for progress indication code.
 *
 * @author Shai Almog
 */
public class NetworkEvent extends ActionEvent {
    /**
     * Indicates that a new request is initializing its connection
     */
    public static final int PROGRESS_TYPE_INITIALIZING = 1;

    /**
     * Indicates the value for the current output stream writing
     */
    public static final int PROGRESS_TYPE_OUTPUT = 2;

    /**
     * Indicates the value for the current input stream reading
     */
    public static final int PROGRESS_TYPE_INPUT = 3;

    /**
     * Indicates that the connection request has completed
     */
    public static final int PROGRESS_TYPE_COMPLETED = 4;

    private Exception error;
    private int progressType;
    private int length = -1;
    private int received;
    private Object metaData;
    private String message;

    /**
     * Constructs an event for an error message
     *
     * @param source source of the error message
     * @param error the exception
     */
    public NetworkEvent(ConnectionRequest source, Exception error) {
        super(source);
        this.error = error;
    }

    /**
     * Constructs a response code even
     *
     * @param request the connection request indicating progress
     * @param responseCode the code
     * @param message the error message
     */
    public NetworkEvent(ConnectionRequest request, int responseCode, String message) {
        super(request);
        this.progressType = responseCode;
        this.message = message;
    }

    /**
     * Constructs a network status update event used to update progress indicators
     * and application logic of the current state in the NetworkManager
     *
     * @param request the connection request indicating progress
     * @param progressType
     */
    public NetworkEvent(ConnectionRequest request, int progressType) {
        super(request);
        this.progressType = progressType;
    }

    /**
     * Constructs a callback event from a connection request with some arbitrary associated meta data
     *
     * @param request the connection request 
     * @param metaData the data associated with the event
     */
    public NetworkEvent(ConnectionRequest request, Object metaData) {
        super(request);
        if(request != null){
            this.progressType = request.getResponseCode();
        }
        this.metaData = metaData;
    }

    /**
     * Equivalent to getSource() casted to connection request
     * 
     * @return the source connection request
     */
    public ConnectionRequest getConnectionRequest() {
        return (ConnectionRequest)getSource();
    }

    /**
     * @return the error
     */
    public Exception getError() {
        return error;
    }

    /**
     * @param error the error to set
     */
    public void setError(Exception error) {
        this.error = error;
    }

    /**
     * Indicates the type of progres indication for this event
     *
     * @return One of PROGRESS_TYPE_COMPLETED, PROGRESS_TYPE_INITIALIZING, PROGRESS_TYPE_INPUT,
     * PROGRESS_TYPE_OUTPUT
     */
    public int getProgressType() {
        return progressType;
    }

    /**
     * Indicates the response code sent by the response code listener
     *
     * @return HTTP error code
     */
    public int getResponseCode() {
        return progressType;
    }

    /**
     * Returns the length of the download event or the upload event in case it is known.
     * -1 is returned when the value is unknown.
     *
     * @return the length length in bytes or -1
     */
    public int getLength() {
        return length;
    }

    /**
     * Returns the amount of data received up to this event or sent up to this event (depending on the
     * event type).
     *
     * @return the receieved or sent value
     */
    public int getSentReceived() {
        return received;
    }

    /**
     * Returns the percentage of progress for a download operation assuming length is known
     * 
     * @return the percentage of the download or -1
     */
    public int getProgressPercentage() {
        if(length > 0) {
            return (int)(((float)received) / ((float)length) * 100.0f);
        }
        return -1;
    }

    /**
     * @param length the length to set
     */
    void setLength(int length) {
        this.length = length;
    }

    /**
     * @param received the received to set
     */
    void setSentReceived(int received) {
        this.received = received;
    }

    /**
     * @return the metaData
     */
    public Object getMetaData() {
        return metaData;
    }

    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }
    
    
}
