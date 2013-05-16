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

import com.codename1.ui.Button;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.io.ConnectionRequest;
import com.codename1.io.NetworkEvent;
import com.codename1.io.NetworkManager;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.plaf.UIManager;

/**
 * Displays a progress dialog with the ability to cancel an ongoing operation
 *
 * @author Shai Almog
 */
public class Progress extends Dialog implements ActionListener {
    private ConnectionRequest request;
    private boolean disposeOnCompletion;
    private boolean autoShow;

    /**
     * Binds the progress UI to the completion of this request
     *
     * @param title the title of the progress dialog
     * @param request the network request pending
     */
    public Progress(String title, ConnectionRequest request) {
        this(title, request, false);
    }

    /**
     * Binds the progress UI to the completion of this request
     *
     * @param title the title of the progress dialog
     * @param request the network request pending
     * @param showPercentage shows percentage on the progress bar
     */
    public Progress(String title, ConnectionRequest request, boolean showPercentage) {
        super(title);
        this.request = request;
        SliderBridge b = new SliderBridge(request);
        b.setRenderPercentageOnTop(showPercentage);
        b.setRenderValueOnTop(true);
        setLayout(new BoxLayout(BoxLayout.Y_AXIS));
        addComponent(b);
        Command cancel = new Command(UIManager.getInstance().localize("cancel", "Cancel"));
        if(Display.getInstance().isTouchScreenDevice() || getSoftButtonCount() < 2) {
            // if this is a touch screen device or a blackberry use a centered button
            Button btn = new Button(cancel);
            Container cnt = new Container(new FlowLayout(CENTER));
            cnt.addComponent(btn);
            addComponent(cnt);
        } else {
            // otherwise use a command
            addCommand(cancel);
        }
        setDisposeWhenPointerOutOfBounds(false);
        setAutoDispose(false);
        NetworkManager.getInstance().addProgressListener(this);
    }

    /**
     * @inheritDoc
     */
    protected void actionCommand(Command cmd) {
        if(Display.getInstance().isTouchScreenDevice() || getSoftButtonCount() < 2) {
            for(int iter = 0 ; iter < getComponentCount() ; iter++) {
                Component c = getComponentAt(iter);
                if(c instanceof Button) {
                    c.setEnabled(false);
                }
            }
        } else {
            removeAllCommands();
        }
        // cancel was pressed
        request.kill();
        dispose();
    }

    /**
     * @inheritDoc
     */
    public void dispose() {
        NetworkManager.getInstance().removeProgressListener(this);
        super.dispose();
        showing = false;
        autoShow = false;
    }
    
    /**
     * @return the disposeOnCompletion
     */
    public boolean isDisposeOnCompletion() {
        return disposeOnCompletion;
    }

    /**
     * @param disposeOnCompletion the disposeOnCompletion to set
     */
    public void setDisposeOnCompletion(boolean disposeOnCompletion) {
        this.disposeOnCompletion = disposeOnCompletion;
    }

    private boolean showing;
    
    /**
     * @inheritDoc
     */
    public void actionPerformed(ActionEvent evt) {
         NetworkEvent ev = (NetworkEvent)evt;
         if(ev.getConnectionRequest() == request) {
             if(disposeOnCompletion && ev.getProgressType() == NetworkEvent.PROGRESS_TYPE_COMPLETED) {
                 dispose();
                 return;
             }
             if(autoShow && !showing) {
                 showing = true;
                 showModeless();
             }
         }
    }

    /**
     * Shows the progress automatically when the request processing is started
     *
     * @return the autoShow
     */
    public boolean isAutoShow() {
        return autoShow;
    }

    /**
     * Shows the progress automatically when the request processing is started
     * @param autoShow the autoShow to set
     */
    public void setAutoShow(boolean autoShow) {
        this.autoShow = autoShow;
    }
}
