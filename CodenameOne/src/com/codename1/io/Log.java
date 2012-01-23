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

import com.codename1.ui.Command;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.TextArea;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.io.FileSystemStorage;
import com.codename1.ui.layouts.BorderLayout;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;


/**
 * Pluggable logging framework that allows a developer to log into storage
 * using the file connector API. It is highly recommended to use this 
 * class coupled with Netbeans preprocessing tags to reduce its overhead
 * completely in runtime.
 *
 * @author Shai Almog
 */
public class Log {
    /**
     * Constant indicating the logging level Debug is the default and the lowest level
     * followed by info, warning and error
     */
    public static final int DEBUG = 1;

    /**
     * Constant indicating the logging level Debug is the default and the lowest level
     * followed by info, warning and error
     */
    public static final int INFO = 2;

    /**
     * Constant indicating the logging level Debug is the default and the lowest level
     * followed by info, warning and error
     */
    public static final int WARNING = 3;

    /**
     * Constant indicating the logging level Debug is the default and the lowest level
     * followed by info, warning and error
     */
    public static final int ERROR = 4;
    
    
    private int level = DEBUG;
    private static Log instance = new Log();
    private long zeroTime = System.currentTimeMillis();
    private Writer output;
    private boolean fileWriteEnabled = false;//System.getProperty("microedition.io.file.FileConnection.version") != null;
    private String fileURL = null;
    
    /**
     * Installs a log subclass that can replace the logging destination/behavior
     * 
     * @param newInstance the new instance for the Log object
     */
    public static void install(Log newInstance) {
        instance = newInstance;
    }
    
    /**
     * Default println method invokes the print instance method, uses DEBUG level
     * 
     * @param text the text to print
     */
    public static void p(String text) {
        p(text, DEBUG);
    }
    
    /**
     * Default println method invokes the print instance method, uses given level
     * 
     * @param text the text to print
     * @param level one of DEBUG, INFO, WARNING, ERROR
     */
    public static void p(String text, int level) {
        instance.print(text, level);
    }

    /**
     * This method is a shorthand form for logThrowable
     *
     * @param t the exception
     */
    public static void e(Throwable t) {
        instance.logThrowable(t);
    }
    
    /**
     * Logs an exception to the log, by default print is called with the exception 
     * details, on supported devices the stack trace is also physically written to 
     * the log
     * @param t
     */
    protected void logThrowable(Throwable t) {
        print("Exception: " + t.getClass().getName() + " - " + t.getMessage(), ERROR);
        t.printStackTrace();
        try {
            synchronized(this) {
                Writer w = getWriter();
                Util.getImplementation().printStackTraceToStream(t, w);
                w.flush();
            }
        } catch(IOException err) {
            err.printStackTrace();
        }
    }

    /**
     * Default log implementation prints to the console and the file connector
     * if applicable. Also prepends the thread information and time before 
     * 
     * @param text the text to print
     * @param level one of DEBUG, INFO, WARNING, ERROR
     */
    protected void print(String text, int level) {
        if(this.level > level) {
            return;
        }
        text = getThreadAndTimeStamp() + " - " + text;
        System.out.println(text);
        try {
            synchronized(this) {
                Writer w = getWriter();
                w.write(text + "\n");
                w.flush();
            }
        } catch(Throwable err) {
            err.printStackTrace();
        }
    }
    
    /**
     * Default method for creating the output writer into which we write, this method
     * creates a simple log file using the file connector
     * 
     * @return writer object
     * @throws IOException when thrown by the connector
     */
    protected Writer createWriter() throws IOException {
        try {
            if(getFileURL() == null) {
                setFileURL("file:///" + FileSystemStorage.getInstance().getRoots()[0] + "/codenameOne.log");
            }
            if(FileSystemStorage.getInstance().exists(getFileURL())) {
                return new OutputStreamWriter(FileSystemStorage.getInstance().openOutputStream(getFileURL(),
                        (int)FileSystemStorage.getInstance().getLength(getFileURL())));
            } else {
                return new OutputStreamWriter(FileSystemStorage.getInstance().openOutputStream(getFileURL()));
            }
        } catch(Exception err) {
            setFileWriteEnabled(false);
            // currently return a "dummy" writer so we won't fail on device
            return new OutputStreamWriter(new ByteArrayOutputStream());
        }
    }
    
    private Writer getWriter() throws IOException {
        if(output == null) {
            output = createWriter();
        }
        return output;
    }

    /**
     * Returns a simple string containing a timestamp and thread name.
     * 
     * @return timestamp string for use in the log
     */
    protected String getThreadAndTimeStamp() {
        long time = System.currentTimeMillis() - zeroTime;
        long milli = time % 1000;
        time /= 1000;
        long sec = time % 60;
        time /= 60;
        long min = time % 60; 
        time /= 60;
        long hour = time % 60; 
        
        return "[" + Thread.currentThread().getName() + "] " + hour  + ":" + min + ":" + sec + "," + milli;
    }
    
    /**
     * Sets the logging level for printing log details, the lower the value 
     * the more verbose would the printouts be
     * 
     * @param level one of DEBUG, INFO, WARNING, ERROR
     */
    public static void setLevel(int level) {
        instance.level = level;
    }

    /**
     * Returns the logging level for printing log details, the lower the value 
     * the more verbose would the printouts be
     * 
     * @return one of DEBUG, INFO, WARNING, ERROR
     */
    public static int getLevel() {
        return instance.level;
    }
    
    /**
     * Returns the contents of the log as a single long string to be displayed by
     * the application any way it sees fit
     * 
     * @return string containing the whole log
     */
    public static String getLogContent() {
        try {
            String text = "";
            if(instance.isFileWriteEnabled()) {
                if(instance.getFileURL() == null) {
                    instance.setFileURL("file:///" + FileSystemStorage.getInstance().getRoots()[0] + "/codenameOne.log");
                }
                Reader r = new InputStreamReader(FileSystemStorage.getInstance().openInputStream(instance.getFileURL()));
                char[] buffer = new char[1024];
                int size = r.read(buffer);
                while(size > -1) {
                    text += new String(buffer, 0, size);
                    size = r.read(buffer);
                }
                r.close();
            } 
            return text;
        } catch (Exception ex) {
            ex.printStackTrace();
            return "";
        }
    }
    
    /**
     * Places a form with the log as a TextArea on the screen, this method can
     * be attached to appear at a given time or using a fixed global key. Using
     * this method might cause a problem with further log output
     */
    public static void showLog() {
        try {
            String text = getLogContent();
            TextArea area = new TextArea(text, 5, 20);
            Form f = new Form("Log");
            f.setScrollable(false);
            final Form current = Display.getInstance().getCurrent();
            Command back = new Command("Back") {
                public void actionPerformed(ActionEvent ev) {
                    current.show();
                }
            };
            f.addCommand(back);
            f.setBackCommand(back);
            f.setLayout(new BorderLayout());
            f.addComponent(BorderLayout.CENTER, area);
            f.show();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Returns the singleton instance of the log
     * 
     * @return the singleton instance of the log
     */
    public static Log getInstance() {
        return instance;
    }

    /**
     * Indicates whether GCF's file writing should be used to generate the log file
     *
     * @return the fileWriteEnabled
     */
    public boolean isFileWriteEnabled() {
        return fileWriteEnabled;
    }

    /**
     * Indicates whether GCF's file writing should be used to generate the log file
     * 
     * @param fileWriteEnabled the fileWriteEnabled to set
     */
    public void setFileWriteEnabled(boolean fileWriteEnabled) {
        this.fileWriteEnabled = fileWriteEnabled;
    }

    /**
     * Indicates the URL where the log file is saved
     *
     * @return the fileURL
     */
    public String getFileURL() {
        return fileURL;
    }

    /**
     * Indicates the URL where the log file is saved
     *
     * @param fileURL the fileURL to set
     */
    public void setFileURL(String fileURL) {
        this.fileURL = fileURL;
    }

    /**
     * Activates the filesystem tracking of file open/close operations
     */
    public void trackFileSystem() {
        Util.getImplementation().setLogListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                String s = (String)evt.getSource();
                // don't log the creation of the log itself
                if(output != null) {
                    p(s);
                }
            }
        });
    }
}
