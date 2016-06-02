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

import com.codename1.ui.Display;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Class implementing the socket API
 *
 * @author Shai Almog
 */
public class Socket {
    private Socket() {}
        
    /**
     * Returns true if sockets are supported in this port, false otherwise
     * @return true if sockets are supported in this port, false otherwise
     */
    public static boolean isSupported() {
        return Util.getImplementation().isSocketAvailable();
    }
    
    /**
     * Returns true if server sockets are supported in this port, if this method returns
     * false invocations of listen will always fail
     * @return true if server sockets are supported in this port, false otherwise
     */
    public static boolean isServerSocketSupported() {
        return Util.getImplementation().isServerSocketAvailable();
    }

    /**
     * Connect to a remote host
     * @param host the host
     * @param port the connection port
     * @param sc callback for when the connection is established or fails
     */
    public static void connect(final String host, final int port, final SocketConnection sc) {
        if(host.indexOf('.') > -1 && host.indexOf(':') > -1) {
            throw new IllegalArgumentException("Port should be provided separately");
        }
        Display.getInstance().startThread(new Runnable() {
            public void run() {
                Object connection = Util.getImplementation().connectSocket(host, port);
                if(connection != null) {
                    sc.setConnected(true);
                    sc.connectionEstablished(new SocketInputStream(connection, sc), new SocketOutputStream(connection, sc));
                } else {
                    sc.setConnected(false);
                    if(connection == null) {
                        sc.connectionError(-1, "Failed to connect");
                    } else {
                        sc.connectionError(Util.getImplementation().getSocketErrorCode(connection), Util.getImplementation().getSocketErrorMessage(connection));
                    }
                }
            }
        }, "Connection to " + host).start();
    }

    /**
     * Listen to incoming connections on port
     * @param port the device port
     * @param scClass class of callback for when the connection is established or fails, this class
     * will be instantiated for every incoming connection and must have a public no argument constructor.
     * @return StopListening instance that allows the the caller to stop listening on a server socket
     */
    public static StopListening listen(final int port, final Class scClass) {
        class Listener implements StopListening, Runnable {
            private boolean stopped;
            public void run() {
                try {
                    while(!stopped) {
                        final Object connection = Util.getImplementation().listenSocket(port);
                        final SocketConnection sc = (SocketConnection)scClass.newInstance();
                        if(connection != null) {
                            sc.setConnected(true);
                            Display.getInstance().startThread(new Runnable() {
                                public void run() {
                                    sc.connectionEstablished(new SocketInputStream(connection, sc), new SocketOutputStream(connection, sc));
                                    sc.setConnected(false);
                                }
                            }, "Connection " + port).start();
                        } else {
                            sc.connectionError(Util.getImplementation().getSocketErrorCode(connection), Util.getImplementation().getSocketErrorMessage(connection));
                        }
                    }
                } catch(Exception err) {
                    // instansiating the class has caused a problem
                    err.printStackTrace();
                }
            }

            public void stop() {
                stopped = true;
            }
            
        }
        Listener l = new Listener();
        Display.getInstance().startThread(l, "Listening on " + port).start();
        return l;
    }
    
    /**
     * Returns the hostname or ip address of the device if available/applicable
     * @return the hostname or ip address of the device if available/applicable
     */
    public static String getHostOrIP() {
        return Util.getImplementation().getHostOrIP();
    }
    
    static class SocketInputStream extends InputStream {
        private Object impl;
        private byte[] buffer;
        private int bufferOffset;
        private SocketConnection con;
        private boolean closed;
        SocketInputStream(Object impl, SocketConnection con) {
            this.impl = impl;
            this.con = con;
        }

        @Override
        public boolean markSupported() {
            return false;
        }

        @Override
        public synchronized void reset() throws IOException {
        }

        @Override
        public void close() throws IOException {
            closed = true;
            if(Util.getImplementation().isSocketConnected(impl)) {
                Util.getImplementation().disconnectSocket(impl);
                con.setConnected(false);
            }
        }

        @Override
        public int available() throws IOException {
            return Util.getImplementation().getSocketAvailableInput(impl);
        }

        private void throwEOF() throws IOException {
            if(closed) {
                throw new EOFException();
            }
        }
        
        // [ddyer 12/2015] 
        // try to read some data into the buffer if we think there is some
        // available, but don't wait if there is not.  This is used to get
        // additional data for a read that has more room in it's buffer.
        //
        private boolean getDataIfAvailable() 
        {	try {
        	if(available()>0)
        		{
        		buffer = Util.getImplementation().readFromSocketStream(impl);
          		bufferOffset = 0;
          		return((buffer!=null) && (buffer.length>0));
        		}
        	}
        	catch (IOException e) 
        		{
        		// we don't really expect an IOException here, but if one
        		// does occur, leave peacefully so the caller can return the
        		// data he has.
        		}
        	// we got nothing.
        	return(false);
        }
        // get some data in the input buffer.  Return true if we did
        // and false if we can't and never can.  This does not return
        // until either data is available or it never will be.
        //
        // ddyer 12/2015.
        // 
        private boolean getSomeData() 
        {  	// upon entry, there may be data leftover from the previous call to read
        	while((buffer==null) 
        			|| (bufferOffset>=buffer.length))
        	{	// we want new data, but if the socket is closed we won't get it.
          		if(!Util.getImplementation().isSocketConnected(impl))
          			{ return(false);	// we'll never get data
          			}
          		buffer = Util.getImplementation().readFromSocketStream(impl);
          		bufferOffset = 0;
          		
        		if((buffer==null) || (buffer.length==0))
         		{	// wait a while
        			if(!Util.getImplementation().isSocketConnected(impl))
        				{ return(false);	// we'll never get data, return without waiting
        				}
        			try {
                        Thread.sleep(10);
                    } catch(InterruptedException err) {}	
        		}
        	}
        	return(true);
        }
         
        // [ddyer 12/2015] 
        // rewritten to fix a bug that caused data loss if the the output 
        // and input buffer both ran out at the same time, then rewritten
        // again for clarity and to avoid waiting forever when the data stream
        // is closed while waiting for the first batch of data.  The old version
        // used a recursive call to read which might have gone an indefinite 
        // number of levels deep, but this version does not.
        @Override
        public int read(byte[] b, int off, int len) throws IOException 
        {
            throwEOF();
            // eventually a read should timeout and return what it has
            if(!getSomeData()) 
            	{ return(-1);	// nothing available ever
            	}
            int bytesRead = 0;
            
           // copy the available data, limited by whichever buffer is smaller
           do {
           while((bytesRead<len) && (bufferOffset<buffer.length))
            	{
                b[off + bytesRead++] = buffer[bufferOffset++];
            	}
           } 
           		// if there's more room and data is already available, go for it.
           while ((bytesRead<len) && getDataIfAvailable());

            // otherwise return with what we have
            return(bytesRead);
        }

        @Override
        public int read(byte[] b) throws IOException {
            throwEOF();
            return read(b, 0, b.length); 
        }

        @Override
        public int read() throws IOException {
            throwEOF();
            byte[] b = new byte[1];
            int v = read(b);
            if(v == -1) {
                return -1;
            }
            return b[0] & 0xff;
        }
        
    }
    
    static class SocketOutputStream extends OutputStream {
        private Object impl;
        private SocketConnection con;
        SocketOutputStream(Object impl, SocketConnection con) {
            this.impl = impl;
            this.con = con;
        }
        
        @Override
        public void close() throws IOException {
            if(Util.getImplementation().isSocketConnected(impl)) {
                Util.getImplementation().disconnectSocket(impl);
                con.setConnected(false);
            }
        }

        @Override
        public void flush() throws IOException {
        }

        private void handleSocketError() {
            int code = Util.getImplementation().getSocketErrorCode(impl);
            String msg = Util.getImplementation().getSocketErrorMessage(impl);
            if(code > 0 || msg != null) {
                con.connectionError(code, msg);
            }
        }
        
        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if(off == 0 && len == b.length) {
                Util.getImplementation().writeToSocketStream(impl, b);
                handleSocketError();
                return;
            }
            byte[] arr = new byte[len];
            System.arraycopy(b, off, arr, 0, len);
            Util.getImplementation().writeToSocketStream(impl, arr);
            handleSocketError();
        }

        @Override
        public void write(byte[] b) throws IOException {
            Util.getImplementation().writeToSocketStream(impl, b);
            handleSocketError();
        }

        @Override
        public void write(int b) throws IOException {
            Util.getImplementation().writeToSocketStream(impl, new byte[] {(byte)b});
            handleSocketError();
        }
        
    }
    
    /**
     * This interface can be invoked to stop listening on a server socket
     */
    public static interface StopListening {
        /**
         * Stop listening
         */
        public void stop();
    }
}
