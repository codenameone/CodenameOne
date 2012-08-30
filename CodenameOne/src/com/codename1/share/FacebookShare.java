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
package com.codename1.share;

import com.codename1.components.InfiniteProgress;
import com.codename1.facebook.FaceBookAccess;
import com.codename1.io.NetworkEvent;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.util.Resources;
import java.io.IOException;

/**
 * Facebook sharing service
 *
 * @author Chen
 */
public class FacebookShare extends ShareService {

    /**
     * Default Constructor
     */
    public FacebookShare() {
        super("Facebook", Resources.getSystemResource().getImage("facebook.png"));
    }

    /**
     * @inheritDoc
     */
    public void actionPerformed(ActionEvent evt) {
        if (!FaceBookAccess.getInstance().isAuthenticated()) {
            FaceBookAccess.getInstance().showAuthentication(this);
            return;
        }
        if (evt.getSource() instanceof Exception) {
            return;
        }

        super.actionPerformed(evt);
    }

    /**
     * @inheritDoc
     */
    public void share(String toShare) {
        final ShareForm[] f = new ShareForm[1];
        f[0] = new ShareForm(getOriginal(), "Post on My Wall", null, toShare,
                new ActionListener() {
                    public void actionPerformed(ActionEvent evt) {
                        try {
                            InfiniteProgress inf = new InfiniteProgress();
                            final Dialog progress = inf.showInifiniteBlocking();
                            FaceBookAccess.getInstance().addResponseCodeListener(new ActionListener() {
                                public void actionPerformed(ActionEvent evt) {
                                    NetworkEvent ne = (NetworkEvent) evt;
                                    int code = ne.getResponseCode();
                                    FaceBookAccess.getInstance().removeResponseCodeListener(this);
                                    progress.dispose();
                                    Dialog.show("Failed to Share", "for some reason sharing has failed, try again later.", "Ok", null);
                                    finish();
                                }
                            });
                            FaceBookAccess.getInstance().postOnWall("me", f[0].getMessage(), new ActionListener() {
                                public void actionPerformed(ActionEvent evt) {
                                    progress.dispose();
                                    finish();
                                }
                            });
                            
                        } catch (IOException ex) {
                            ex.printStackTrace();
                            System.out.println("failed to share " + ex.getMessage());
                        }
                    }
                });
        f[0].show();

    }
}
