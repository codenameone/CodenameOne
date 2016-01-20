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
package com.codename1.ui.html;

import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.geom.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * ResourceThreadQueue is a thread queue used to create and manage threads that download images and CSS files that were referred from HTML pages
 * Was called ImageThreadQueue but name was changed since it now handles CSS as well
 *
 * @author Ofir Leitner
 */
class ResourceThreadQueue {

    /**
     * The default number of maximum threads used for image download
     */
    private static int DEFAULT_MAX_THREADS = 2;

    HTMLComponent htmlC;
    Vector queue = new Vector();
    Vector running = new Vector();
    Vector bgImageCompsUnselected = new Vector();
    Vector bgImageCompsSelected = new Vector();
    Vector bgImageCompsPressed = new Vector();

    Hashtable images = new Hashtable();
    static int maxThreads = DEFAULT_MAX_THREADS;
    int threadCount;
    private int cssCount=-1; // As long as there are no CSS files this stays -1 and thus CSS loading is ignored
    boolean started;

    /**
     * Constructs the queue
     * 
     * @param htmlC The HTMLComponent this queue belongs to
     */
    ResourceThreadQueue(HTMLComponent htmlC) {
        this.htmlC=htmlC;
    }

    /**
     * Sets the maximum number of threads to use for image download
     * If startRunning was already called, this will takes effect in the next page loaded.
     *
     * @param threadsNum the maximum number of threads to use for image download
     */
    static void setMaxThreads(int threadsNum) {
        maxThreads=threadsNum;
    }

    /**
     * Adds the image to the queue
     *
     * @param imgLabel The label in which the image should be contained after loaded
     * @param imageUrl The URL this image should be fetched from
     */
    synchronized void add(Component imgLabel,String imageUrl) {
        /*if ((HTMLComponent.TABLES_LOCK_SIZE) && (htmlC.curTable!=null)) {
            downloadImageImmediately(imgLabel, imageUrl, 0);
        } else {*/
        if (started) {
            throw new IllegalStateException("ResourceThreadQueue already started! stop/cancel first");
        }
        images.put(imgLabel, imageUrl); // Using a hashtable to collect all requests first enables overriding urls for labels (For example in CSS use cases)
        //}
    }

    /**
     * Adds the image to the queue, to be used as a background image for a component
     *
     * @param imgComp The component for which the image should be used after loaded
     * @param imageUrl The URL this image should be fetched from
     * @param styles A mask of CSSEngine.STYLE_* values indicating in which styles this background image should be displayed
     */
    synchronized void addBgImage(Component imgComp,String imageUrl,int styles) {
        if (HTMLComponent.SUPPORT_CSS) {
            /*if ((HTMLComponent.TABLES_LOCK_SIZE) && (htmlC.curTable!=null)) {
                downloadImageImmediately(imgComp,imageUrl,styles);
            } else {*/
                add(imgComp,imageUrl);
                if ((styles & CSSEngine.STYLE_SELECTED)!=0) {
                    bgImageCompsSelected.addElement(imgComp);
                }
                if ((styles & CSSEngine.STYLE_UNSELECTED)!=0) {
                    bgImageCompsUnselected.addElement(imgComp);
                }
                if ((styles & CSSEngine.STYLE_PRESSED)!=0) {
                    bgImageCompsPressed.addElement(imgComp);
                }
            //}
        }
    }

    /**
     * Downloads the image immediately // It seems that the issue that required this was already solved in the table package
     * The image is not added to the queue, and not loaded on another thread but rather downloaded on the same thread that builds up the document
     * This is useful for HTMLCoponent.TABLES_PATCH
     *
     * @param imgComp The component for which the image should be used after loaded
     * @param imageUrl The URL this image should be fetched from
     * @param styles A mask of CSSEngine.STYLE_* values indicating in which styles this background image should be displayed
     *
    synchronized void downloadImageImmediately(Component imgComp,String imageUrl,int styles) {
        try {
            InputStream is = htmlC.getRequestHandler().resourceRequested(new DocumentInfo(imageUrl,DocumentInfo.TYPE_IMAGE));
            Image img = Image.createImage(is);
            ResourceThread t = new ResourceThread(imageUrl, imgComp, htmlC, null);
            t.handleImage(img, imgComp,((styles & CSSEngine.STYLE_UNSELECTED)!=0),((styles & CSSEngine.STYLE_SELECTED)!=0),((styles & CSSEngine.STYLE_PRESSED)!=0));
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }*/

    /**
     * Adds a stylesheet to the queue
     *
     * @param cssUrl The URL this style sheet should be fetched from
     * @param dom the dom object on which the CSS should be applied
     */
    synchronized void addCSS(String cssUrl,String encoding) {
        if (started) {
            throw new IllegalStateException("ResourceThreadQueue alreadey started! stop/cancel first");
        }
        DocumentInfo cssDocInfo=new DocumentInfo(cssUrl, DocumentInfo.TYPE_CSS);
        if (encoding!=null) {
            cssDocInfo.setEncoding(encoding);
        }
        ResourceThread t =  new ResourceThread(cssDocInfo, htmlC, this);
        queue.addElement(t);
        incCSSCount();
    }

    /**
     * Incereases the internal count of the number of pending CSS documents
     */
    private void incCSSCount() {
        if (cssCount==-1) { // first CSS make sure we bump it from -1 to 1.
            cssCount++;
        }
        cssCount++;
    }

    /**
     * Returns the number of pending CSS documents
     * 
     * @return the number of pending CSS documents
     */
    synchronized int getCSSCount() {
        return cssCount;
    }

    /**
     * Returns the queue size, this is relevant only when the queue hasn't started yet
     *
     * @return the queue size
     */
    int getQueueSize() {
        return (images.size()+queue.size()); // CSS files are added directly to queue while images to the images vector
        //return queue.size();
    }

    /**
     * Notifies the queue that all images and CSS have been queues and it can start dequeuing and download the images.
     * The queue isn't started before that to prevent multiple downloads of the same image
     */
    synchronized void startRunning() {
        if (!startDequeue()) {
            startRunningImages();
        }
    }

    /**
     * Starts downloading the images (This is called only after all CSS files have been downloaded, since they may contain image references)
     */
    synchronized void startRunningImages() {
        queue.removeAllElements();
        Vector urls=new Vector();
        for(Enumeration e=images.keys();e.hasMoreElements();) {
            Component imgComp = (Component)e.nextElement();
            String imageUrl = (String)images.get(imgComp);
            int urlIndex=urls.indexOf(imageUrl);

            if (urlIndex!=-1) {
                ResourceThread t=(ResourceThread)queue.elementAt(urlIndex);
                t.addLabel(imgComp);
            } else {
                ResourceThread t =  new ResourceThread(imageUrl, imgComp, htmlC, this);
                queue.addElement(t);
                urls.addElement(imageUrl);
            }
        }
        urls=null;
        
        images=new Hashtable();

        if (!startDequeue()) {
            htmlC.setPageStatus(HTMLCallback.STATUS_COMPLETED);
        }

    }

    /**
     *  Starts dequeuing the queue into the running pool and launch them
     * 
     * @return true if there are at least one active thread, false otherwise
     */
    private synchronized boolean startDequeue() {
        int threads=Math.min(queue.size(), maxThreads);

        for(int i=0;i<threads;i++) {
            ResourceThread t=(ResourceThread)queue.firstElement();
            queue.removeElementAt(0);
            running.addElement(t);
            threadCount++;
            //t.go();          //new Thread(t).start();
        }
        for(Enumeration e=running.elements();e.hasMoreElements();) {
            ResourceThread t=(ResourceThread)e.nextElement();
            t.go();
        }
        
        
        
        return (threads>0);
    }

    /**
     * Called by the ResourceThread when it finishes downloading and setting the image.
     * This in turns starts another thread if the queue is not empty
     * 
     * @param finishedThread The calling thread
     * @param success true if the image download was successful, false otherwise
     */
    synchronized void threadFinished(ResourceThread finishedThread,boolean success) {
        
        if(finishedThread.cssDocInfo!=null) {
            cssCount--; // Reduce the number of waiting CSS, even if reading failed
        }

        if ((HTMLComponent.SUPPORT_CSS) && (cssCount==0)) {
            cssCount=-1; // So it won't get applied again
            htmlC.applyAllCSS();
            htmlC.cssCompleted();
        }
        running.removeElement(finishedThread);


        if (queue.size()>0) {
            ResourceThread t=(ResourceThread)queue.firstElement();
            queue.removeElementAt(0);
            running.addElement(t);
            t.go();          //new Thread(t).start();
        } else {
            threadCount--;
        }

        if (threadCount==0) {
            if (images.size()==0) {
                htmlC.setPageStatus(HTMLCallback.STATUS_COMPLETED);
            } else {
                startRunningImages();
            }
        }
    }

    /**
     * Discards the entire queue and signals the running threads to cancel.
     * THis will be triggered if the user cancelled the page or moved to another page.
     */
    synchronized void discardQueue() {
        queue.removeAllElements();

        for(Enumeration e=running.elements();e.hasMoreElements();) {
            ResourceThread t = (ResourceThread)e.nextElement();
            t.cancel();
        }
        running.removeAllElements();
        bgImageCompsSelected.removeAllElements();
        bgImageCompsUnselected.removeAllElements();
        bgImageCompsPressed.removeAllElements();

        threadCount=0;
        cssCount=-1;
        started=false;

    }

    /**
     * Returns a printout of the threads queue, can be used for debugging
     *
     * @return a printout of the threads queue
     */
    public String toString() {
        String str=("---- Running ----\n");
        int i=1;
        for(Enumeration e=running.elements();e.hasMoreElements();) {
            ResourceThread t = (ResourceThread)e.nextElement();
            if (t.imageUrl!=null) {
                str+="#"+i+": "+t.imageUrl+"\n";
            } else {
                str+="#"+i+": CSS - "+t.cssDocInfo.getUrl()+"\n";
            }
            i++;
        }
        i=1;
        str+="Queue:\n";
        for(Enumeration e=queue.elements();e.hasMoreElements();) {
            ResourceThread t = (ResourceThread)e.nextElement();
            if (t.imageUrl!=null) {
                str+="#"+i+": "+t.imageUrl+"\n";
            } else {
                str+="#"+i+": CSS - "+t.cssDocInfo.getUrl()+"\n";
            }
            i++;
        }
        str+="---- count:"+threadCount+" ----\n";
        return str;
    }

    // Inner classes:

    /**
     * An ResourceThread downloads an Image as requested
     *
     * @author Ofir Leitner
     */
    class ResourceThread implements Runnable, IOCallback {

        Component imgLabel;
        Vector labels;
        String imageUrl;
        DocumentRequestHandler handler;
        ResourceThreadQueue threadQueue;
        boolean cancelled;
        HTMLComponent htmlC;
        Image img;
        DocumentInfo cssDocInfo;

        /**
         * Constructs the ResourceThread for an image file
         *
         * @param imgLabel The label in which the image should be contained after loaded
         * @param imageUrl The URL this image should be fetched from
         * @param handler The RequestHandler through which to retrieve the image
         * @param threadQueue The main queue, for callback purposes
         */
        ResourceThread(String imageUrl, Component imgLabel,HTMLComponent htmlC,ResourceThreadQueue threadQueue) {
            this.imageUrl=imageUrl;
            this.imgLabel=imgLabel;
            this.handler=htmlC.getRequestHandler();
            this.threadQueue=threadQueue;
            this.htmlC=htmlC;
        }

        /**
         * Constructs the ResourceThread for a CSS file
         *
         * @param cssDocInfo A DocumentInfo object with the URL this CSS file should be fetched from
         * @param handler The RequestHandler through which to retrieve the image
         * @param threadQueue The main queue, for callback purposes
         */
        ResourceThread(DocumentInfo cssDocInfo,HTMLComponent htmlC,ResourceThreadQueue threadQueue) {
            this.cssDocInfo=cssDocInfo;
            this.handler=htmlC.getRequestHandler();
            this.threadQueue=threadQueue;
            this.htmlC=htmlC;
        }

        /**
         * Cancels this thread
         */
        void cancel() {
            cancelled=true;
        }

        /**
         * Adds a label which has the same URL, useful for duplicate images in the same page
         *
         * @param label A label which has the same image URL
         */
        void addLabel(Component label) {
            if (labels==null) {
                labels=new Vector();
            }
            labels.addElement(label);
        }

        /**
         * This is the main entry point to this runnable, it checks whether the callback is synchronous or async.
         * According to that it either runs this as a thread (sync) or simply calls the async method (async implements threading itself)
         */
        void go() {
                if (handler instanceof AsyncDocumentRequestHandler) {
                DocumentInfo docInfo=cssDocInfo!=null?cssDocInfo:new DocumentInfo(imageUrl,DocumentInfo.TYPE_IMAGE);
                    ((AsyncDocumentRequestHandler)handler).resourceRequestedAsync(docInfo, this);
                } else {
                    Display.getInstance().startThread(this, "HTML Resources").start();
                }
        }

        /**
         * {{@inheritDoc}}
         */
        public void run() {
                DocumentInfo docInfo=cssDocInfo!=null?cssDocInfo:new DocumentInfo(imageUrl,DocumentInfo.TYPE_IMAGE);
                InputStream is = handler.resourceRequested(docInfo);
                streamReady(is, docInfo);
        }

        /**
         * {{@inheritDoc}}
         */
        public void streamReady(InputStream is,DocumentInfo docInfo) {
            try {
                if (is==null) {
                    if (htmlC.getHTMLCallback()!=null) {
                        htmlC.getHTMLCallback().parsingError(cssDocInfo!=null?HTMLCallback.ERROR_CSS_NOT_FOUND:HTMLCallback.ERROR_IMAGE_NOT_FOUND, null, null, null, (cssDocInfo!=null?"CSS":"Image")+" not found at "+(cssDocInfo!=null?cssDocInfo.getUrl():imageUrl));
                    }
                } else {
                    if(cssDocInfo!=null) { // CSS
                        if (HTMLComponent.SUPPORT_CSS) { // no need to also check if loadCSS is true, since if we got so far - it is...
                            CSSElement result = CSSParser.getInstance().parseCSSSegment(new InputStreamReader(is),is,htmlC,cssDocInfo.getUrl());
                            result.setAttribute(result.getAttributeName(new Integer(CSSElement.CSS_PAGEURL)), cssDocInfo.getUrl());
                            htmlC.addToExternalCSS(result);
                        }
                        threadQueue.threadFinished(this,true);
                        return;
                    } else {
                        img=Image.createImage(is);
                        if (img==null) {
                            if (htmlC.getHTMLCallback()!=null) {
                                htmlC.getHTMLCallback().parsingError(HTMLCallback.ERROR_IMAGE_BAD_FORMAT, null, null, null, "Image could not be created from "+imageUrl);
                            }
                        }
                    }
                }

                if (img==null) {
                    threadQueue.threadFinished(this,false);
                    return;
                }
                if (!cancelled) {
                    Display.getInstance().callSerially(new Runnable() {
                        public void run() {
                            // prevent a redirect or another thread from breaking the UI
                            handleImage(img,imgLabel);
                            if (labels!=null) {
                                for(Enumeration e=labels.elements();e.hasMoreElements();) {
                                    Component cmp=(Component)e.nextElement();
                                    handleImage(img,cmp);
                                }
                            }
                        }
                    });
                    threadQueue.threadFinished(this,true);
                }
            } catch (IOException ioe) {
                if (htmlC.getHTMLCallback()!=null) {
                    htmlC.getHTMLCallback().parsingError(HTMLCallback.ERROR_IMAGE_BAD_FORMAT, null, null, null, "Image could not be created from "+imageUrl+": "+ioe.getMessage());
                }
                if(!cancelled) {
                    threadQueue.threadFinished(this,false);
                }
            }

        }

        /**
         * After a successful download, this handles placing the image on the label and resizing if necessary
         *
         * @param img The image
         * @param label The label to apply the image on
         */
        private void handleImage(Image img,Component cmp) {
            boolean bgUnselected=(threadQueue.bgImageCompsUnselected.contains(cmp));;
            boolean bgSelected=(threadQueue.bgImageCompsSelected.contains(cmp));
            boolean bgPressed=(threadQueue.bgImageCompsPressed.contains(cmp));
            handleImage(img, cmp,bgUnselected,bgSelected,bgPressed);
        }

        /**
         * After a successful download, this handles placing the image on the label and resizing if necessary
         *
         * @param img The image
         * @param cmp The component to apply the image on
         * @param bgUnselected true if the image should be used as a background for the component when it is unselected, false otherwise
         * @param bgSelected true if the image should be used as a background for the component when it is selected, false otherwise
         * @param bgPressed true if the image should be used as a background for the component when it is pressed, false otherwise
         */
        void handleImage(Image img,Component cmp,boolean bgUnselected,boolean bgSelected,boolean bgPressed) {
            boolean bg=false;
            if (bgUnselected) {
                cmp.getUnselectedStyle().setBgImage(img);
                bg=true;
            }
            if (bgSelected) {
                cmp.getSelectedStyle().setBgImage(img);
                bg=true;
            }
            if (bgPressed) {
                if (cmp instanceof HTMLLink) {
                    ((HTMLLink)cmp).getPressedStyle().setBgImage(img);
                }
                bg=true;
            }
            if (bg) {
                cmp.repaint();
                return;
            }

            Label label = (Label)cmp;
            label.setText(""); // remove the alternate text (important to do here before checking the width/height)

            // Was set in HTMLComponent.handleImage if the width/height attributes were in the tag
            int width=label.getPreferredW()-label.getStyle().getPadding(Component.LEFT)-label.getStyle().getPadding(Component.RIGHT);
            int height=label.getPreferredH()-label.getStyle().getPadding(Component.TOP)-label.getStyle().getPadding(Component.BOTTOM);

            if (width!=0) {
                if (height==0) { // If only width was specified, height should be calculated so the image keeps its aspect ratio
                    height=img.getHeight()*width/img.getWidth();
                }
            } else if (height!=0) {
                if (width==0) { // If only height was specified, width should be calculated so the image keeps its aspect ratio
                    width=img.getWidth()*height/img.getHeight();
                }
            }

            if (width!=0) { // if any of width or height were not 0, the other one was set to non-zero above, so this check suffices
                img=img.scaled(width, height);
                width+=label.getStyle().getPadding(Component.LEFT)+label.getStyle().getPadding(Component.RIGHT);
                height+=label.getStyle().getPadding(Component.TOP)+label.getStyle().getPadding(Component.BOTTOM);
                label.setPreferredSize(new Dimension(width,height));
            }

            label.setIcon(img);

            htmlC.revalidate();
            if (label.getClientProperty(HTMLComponent.CLIENT_PROPERTY_IMG_BORDER)==null) { // if this property is defined, it means the image had a border already
                label.getUnselectedStyle().setBorder(null); //remove the border which is a sign the image is loading
            } else {
                int borderSize=((Integer)label.getClientProperty(HTMLComponent.CLIENT_PROPERTY_IMG_BORDER)).intValue();
                // Note that padding is set here and not in handleImage since we rely on the image size (which includes padding) to know if a width/height was specified
                label.getUnselectedStyle().setPadding(borderSize,borderSize,borderSize,borderSize);
                label.getSelectedStyle().setPadding(borderSize,borderSize,borderSize,borderSize);
            }

        }

    }
    
}
