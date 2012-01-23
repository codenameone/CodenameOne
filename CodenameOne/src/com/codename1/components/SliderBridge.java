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
package com.codename1.components;

import com.codename1.ui.Slider;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.io.ConnectionRequest;
import com.codename1.io.NetworkEvent;
import com.codename1.io.NetworkManager;
import java.util.Vector;

/**
 * Binds a slider to network progress events so it shows the pro
 *
 * @author Shai Almog
 */
public class SliderBridge extends Slider {
    private ConnectionRequest[] sources;

    /**
     * Default constuctor
     */
    public SliderBridge() {
        bindProgress((ConnectionRequest[])null, this);
    }

    /**
     * Allows binding progress to an arbitrary slider
     *
     * @param source the source connection request 
     * @param s the slider
     */
    public static void bindProgress(final ConnectionRequest source, final Slider s) {
        if(source == null) {
            bindProgress((ConnectionRequest[]) null, s);
        } else {
            bindProgress(new ConnectionRequest[] {source}, s);
        }
    }
    
    /**
     * Allows binding progress to an arbitrary slider
     * 
     * @param sources the source connection request (null for all network activity)
     * @param s the slider
     */
    public static void bindProgress(final ConnectionRequest[] sources, final Slider s) {
        Vector v = null;
        int portions = 1000;
        if(sources != null) {
            v = new Vector();
            for(int iter = 0 ; iter < sources.length ; iter++) {
                v.addElement(sources[iter]);
            }
            portions = portions / sources.length;
        }
        final Vector sourceVec = v;
        final int portionPerSource = portions;
        NetworkManager.getInstance().addProgressListener(new ActionListener() {
            private float currentLength;
            private int soFar;

            /**
             * @inheritDoc
             */
            public void actionPerformed(ActionEvent evt) {
                if(sources != null) {
                    if(!sourceVec.contains(evt.getSource())) {
                        return;
                    }
                }
                NetworkEvent e = (NetworkEvent)evt;
                switch(e.getProgressType()) {
                    case NetworkEvent.PROGRESS_TYPE_COMPLETED:
                        s.setInfinite(false);
                        //s.setProgress(s.getMaxValue());
                        soFar += portionPerSource;
                        s.setProgress(soFar);
                        if(sources != null) {
                            NetworkManager.getInstance().removeProgressListener(this);
                        }
                        break;
                    case NetworkEvent.PROGRESS_TYPE_INITIALIZING:
                        s.setInfinite(true);
                        break;
                    case NetworkEvent.PROGRESS_TYPE_INPUT:
                    case NetworkEvent.PROGRESS_TYPE_OUTPUT:
                        if(e.getLength() > 0) {
                            currentLength = e.getLength();
                            s.setMaxValue(1000);
                            s.setInfinite(false);
                            float sentReceived = e.getSentReceived();
                            sentReceived = sentReceived / currentLength * portionPerSource;
                            s.setProgress((int)sentReceived + soFar);
                            //s.setProgress(e.getSentReceived());
                            //s.setMaxValue(e.getLength());
                        } else {
                            s.setInfinite(true);
                        }
                        break;
                }
            }
        });
    }

    /**
     * Displays progress only for the source object, every other object in the queue
     * before completion will produce an infinite progress. After 100% the progress will
     * no longer move.
     *
     * @param source the request whose progress should be followed
     */
    public SliderBridge(ConnectionRequest source) {
        if(source != null) {
            sources = new ConnectionRequest[] {source};
        }
        bindProgress(sources, this);
    }

    /**
     * Allows displaying progress of multiple requests being sent
     *
     * @param sources the requests whose progress should be followed
     */
    public SliderBridge(ConnectionRequest[] sources) {
        this.sources = sources;
        bindProgress(sources, this);
    }
}
