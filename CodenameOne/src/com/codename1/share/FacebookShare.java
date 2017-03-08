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
import com.codename1.io.FileSystemStorage;
import com.codename1.io.MultipartRequest;
import com.codename1.io.NetworkEvent;
import com.codename1.io.NetworkManager;
import com.codename1.io.Util;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.util.Resources;
import java.io.IOException;
import java.io.InputStream;

/**
 * Facebook sharing service
 *
 * @author Chen
 */
public class FacebookShare extends ShareService {

    private String token;

    /**
     * Default Constructor
     */
    public FacebookShare() {
        super("Facebook", null);
    }

    @Override
    public Image getIcon() {
        Image i = super.getIcon();
        if(i == null) {
            i = Resources.getSystemResource().getImage("facebook.png");
            setIcon(i);
        }
        return i;
    }

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent evt) {
        if (!FaceBookAccess.getInstance().isAuthenticated()) {
            FaceBookAccess.getInstance().showAuthentication(this);
            return;
        }
        if (evt.getSource() instanceof Exception) {
            return;
        }
        if (evt.getSource() instanceof String) {
            token = (String) evt.getSource();
        }

        super.actionPerformed(evt);
    }

    /**
     * {@inheritDoc}
     */
    public void share(String text, final String image, final String mime) {
        final ShareForm[] f = new ShareForm[1];
        if (image == null) {
            f[0] = new ShareForm(getOriginal(), "Post on My Wall", null, text,
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
        } else {
            f[0] = new ShareForm(getOriginal(), "Post on My Wall", null, text, image,
                    new ActionListener() {

                        public void actionPerformed(ActionEvent evt) {

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

                            MultipartRequest req = new MultipartRequest();
                            req.addResponseListener(new ActionListener() {                                
                                public void actionPerformed(ActionEvent evt) {
                                    progress.dispose();
                                    finish();
                                }
                            });
                            final String endpoint = "https://graph.facebook.com/me/photos?access_token=" + token;
                            req.setUrl(endpoint);
                            req.addArgumentNoEncoding("message", f[0].getMessage());
                            InputStream is = null;
                            try {
                                is = FileSystemStorage.getInstance().openInputStream(image);
                                req.addData("source", is, FileSystemStorage.getInstance().getLength(image), mime);
                                NetworkManager.getInstance().addToQueue(req);
                            } catch (IOException ioe) {
                                ioe.printStackTrace();
                            }
                        }
                    });
            f[0].show();

        }

    }

    /**
     * {@inheritDoc}
     */
    public void share(String toShare) {
        share(toShare, null, null);
    }

    /**
     * {@inheritDoc}
     */
    public boolean canShareImage() {
        return true;
    }
}
