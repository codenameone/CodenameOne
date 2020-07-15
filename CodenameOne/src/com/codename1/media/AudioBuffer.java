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
package com.codename1.media;

import java.util.ArrayList;

/**
 * This class can be used to capture raw PCM data from the device's microphone.
 * AudioBuffers should be obtained via the {@link MediaManager#getAudioBuffer(java.lang.String, boolean, int) }
 * method.  
 * 
 * @author shannah
 */
public class AudioBuffer {
    private Object refLock = new Object();
    private int refCounter;
    
    int retain() {
        synchronized(refLock) {
            refCounter++;
            return refCounter;
        }
    }
    int release() {
        synchronized(refLock) {
            refCounter--;
            return refCounter;
        }
    }
    
    /**
     * A callback that can be registered to receive notifications when the contents of the 
     * AudioBuffer is changed.
     * <p><strong>IMPORTANT:</strong> There are no guarantees what thread this callback will be 
     * run on, and it will almost never occur on the EDT.</p>
     */
    public static interface AudioBufferCallback {
        /**
         * Method called when the contents of the AudioBuffer are changed.
         * <p><strong>IMPORTANT:</strong> There are no guarantees what thread this callback will be 
     * run on, and it will almost never occur on the EDT.</p>
         * @param buffer 
         */
        public void frameReceived(AudioBuffer buffer);
    }
    
    /**
     * Registered callbacks to be notified when the contents of this buffer changes. 
     */
    private ArrayList<AudioBufferCallback> callbacks = new ArrayList<AudioBufferCallback>();
    
    /**
     * The buffer contents.
     */
    private float[] buffer;
    private float[] tmpDownSampleBuffer;
    
    /**
     * Internal flag used to indicate that we are currently firing callbacks.  This is used 
     * internally to prevent modification of the callbacks array while we are firing callbacks.
     * A call is made to addCallback or removeCallback while this flag is set, then,
     * the add/remove will be delated until after the fireCallback sequence is complete
     * so we don't get a concurrentModificationException on the callbacks list.
     */
    private boolean inFireFrame = false;
    
    /**
     * Used to store pending add/remove calls while inFireFrame is true.  These are all
     * executed when the callbacks have all finished firing.
     */
    private ArrayList<Runnable> pendingOps = new ArrayList<Runnable>();
    
    /**
     * The current size of the buffer.  Every time the buffer contents are changed, this value
     * is set.  This is not to be confused with the maximum buffer size.
     */
    private int size;
    
    private int sampleRate;
    private int numChannels;
    
    /**
     * Creates a new AudioBuffer with the given maximum size.
     * @param maxSize The maximum size of the buffer.
     */
    public AudioBuffer(int maxSize) {
        buffer = new float[maxSize];
        tmpDownSampleBuffer = new float[maxSize];
    }

    /**
     * Copies data into the buffer from the given source buffer.  This will trigger the callbacks' {@link AudioBufferCallback#frameReceived(com.codename1.media.AudioBuffer) }
     * method.
     * @param source The source buffer to copy from.
     */
    public void copyFrom(AudioBuffer source) {
        copyFrom(source.getSampleRate(), source.getNumChannels(), source.buffer, 0, source.size);
    }
    
    /**
     * Copies data from the source array into this buffer. This will trigger the callbacks' {@link AudioBufferCallback#frameReceived(com.codename1.media.AudioBuffer) }
     * method.
     * @param source 
     */
    public void copyFrom(int sampleRate, int numChannels, float[] source) {
        copyFrom(sampleRate, numChannels, source, 0, source.length);
    }
    
    /**
     * Copies data from the source array (in the given range) into the buffer. This will trigger the callbacks' {@link AudioBufferCallback#frameReceived(com.codename1.media.AudioBuffer) }
     * method.
     * @param source The source array to copy data from.
     * @param offset The offset in the source array to begin copying from.
     * @param len The length of the range to copy.
     */
    public void copyFrom(int sampleRate, int numChannels, float[] source, int offset, int len) {
        if (len > buffer.length) {
            throw new IllegalArgumentException("Buffer size is "+buffer.length+" but attempt to copy "+len+" samples into it");
        }
        this.sampleRate = sampleRate;
        this.numChannels = numChannels;
        System.arraycopy(source, offset, buffer, 0, len);
        size = len;
        fireFrameReceived();
    }
    
    /**
     * Copies data to another audio buffer. This will trigger callbacks in the destination.
     * @param dest The destination audio buffer.
     */
    public void copyTo(AudioBuffer dest) {
        dest.copyFrom(this);
    }
    
    /**
     * Copies data from this buffer to the given float array.
     * @param dest The destination float array to copy to.
     */
    public void copyTo(float[] dest) {
        copyTo(dest, 0);
    }
    
    /**
     * Copies data from this buffer to the given float array.
     * @param dest The destination float array.
     * @param offset The offset in the destination array to start copying to.
     */
    public void copyTo(float[] dest, int offset) {
        int len = size;
        if (dest.length < offset + len) {
            throw new IllegalArgumentException("Destination is not big enough to store len "+len+" at offset "+offset+".  Length only "+dest.length);
        }

        System.arraycopy(buffer, 0, dest, offset, len);
    }
    
    /**
     * The current size of the buffer.  This value will be changed each time data is copied into the buffer to reflect the current size of the data.
     * @return 
     */
    public int getSize() {
        return size;
    }
    
    /**
     * Gets the maximum size of the buffer.  Trying to copy more than this amount of data into the buffer will result in an IndexOutOfBoundsException.
     * @return 
     */
    public int getMaxSize() {
        return buffer.length;
    }
    
    /**
     * Called when a frame is received.  This will call the {@link AudioBufferCallback#frameReceived(com.codename1.media.AudioBuffer) } method in all
     * registered callbacks.
     */
    private void fireFrameReceived() {
        inFireFrame = true;
        
        try {
            for (AudioBufferCallback l : callbacks) {
                l.frameReceived(this);
            }
        } finally {
            inFireFrame = false;
            while (!pendingOps.isEmpty()) {
                Runnable r = pendingOps.remove(0);
                r.run();
            }
        }
    }
    
    /**
     * Adds a callback to be notified when the contents of this buffer are changed.
     * @param l The AudioBufferCallback
     */
    public void addCallback(final AudioBufferCallback l) {
        if (inFireFrame) {
            pendingOps.add(new Runnable() {
                public void run() {
                    callbacks.add(l);
                }
            });
        } else {
            callbacks.add(l);
        }
    }
    
    /**
     * Removes a callback from the audio buffer.
     * @param l The callback to remove.
     */
    public void removeCallback(final AudioBufferCallback l) {
        if (inFireFrame) {
            pendingOps.add(new Runnable() {
                public void run() {
                    callbacks.remove(l);
                }
            });
        } else {
            callbacks.remove(l);
        }
    }

    /**
     * @return the sampleRate
     */
    public int getSampleRate() {
        return sampleRate;
    }

    /**
     * @return the numChannels
     */
    public int getNumChannels() {
        return numChannels;
    }

    
    
    /**
     * Downsamples the buffer to the given rate.  This will change the result of 
     * {@link #getSize() } and {@link #getSampleRate() }.
     * 
     * Note:  This should only be called inside the AudioBuffer callback since it is 
     * modifying the contents of the buffer.
     * @param targetSampleRate The new target rate.
     */
    public void downSample(int targetSampleRate) {
        
        if (targetSampleRate == sampleRate) {
            return;
        }
        if (targetSampleRate > sampleRate) {
            throw new IllegalArgumentException("downsample() expects a target rate that it same or lower than current sample rate.  Current rate="+sampleRate+" target rate requested="+targetSampleRate);
        }
        float ratio = targetSampleRate / (float)sampleRate;
        
        float stepSize = 1/ratio;
        int len = size;
        int j = 0;
        for (int channel=0; channel < numChannels; channel++) {
            if (numChannels == 1) {
                System.arraycopy(buffer, 0, tmpDownSampleBuffer, 0, len);
            } else {
                int k=0;
                for (int i=0; i<size; i+=numChannels) {
                    tmpDownSampleBuffer[k++] = buffer[i + channel];
                }
            }
            len = size / numChannels;
            for (float i=0; i<len; i+=stepSize ) {
            
                int i0 = (int)Math.floor(i);
                int i1 = (int)Math.ceil(i);
                if (i1 == i0 || i1 > len-1) {
                    if (i0 > len-1) {
                        break;
                    }
                    buffer[j + channel] = tmpDownSampleBuffer[i0];
                    j += numChannels;
                } else {
                    float v0 = tmpDownSampleBuffer[i0];
                    float v1 = tmpDownSampleBuffer[i1];
                    float t = Math.round(i) - i0;
                    float v = v0 + t * (v1-v0);
                    buffer[j + channel] = v;
                    j += numChannels;
                    
                }
                
            }
        }
        
        size = j;
        sampleRate = targetSampleRate;
        
        
    }
    
}
