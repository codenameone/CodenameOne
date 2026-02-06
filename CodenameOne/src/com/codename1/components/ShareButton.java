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
package com.codename1.components;

import com.codename1.share.EmailShare;
import com.codename1.share.FacebookShare;
import com.codename1.share.SMSShare;
import com.codename1.share.ShareService;
import com.codename1.ui.Button;
import com.codename1.ui.Command;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.List;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.layouts.BorderLayout;

import java.util.Vector;

/// The share button allows sharing a String or an image either thru the defined
/// sharing services or thru the native OS sharing support. On Android & iOS the native
/// sharing API is invoked for this class.
///
/// The code below demonstrates image sharing, notice that an image must be stored using
/// the `com.codename1.io.FileSystemStorage` API and shouldn't use a different API
/// like `com.codename1.io.Storage`!
///
/// ```java
/// Form hi = new Form("ShareButton");
/// ShareButton sb = new ShareButton();
/// sb.setText("Share Screenshot");
/// hi.add(sb);
///
/// Image screenshot = Image.createImage(hi.getWidth(), hi.getHeight());
/// hi.revalidate();
/// hi.setVisible(true);
/// hi.paintComponent(screenshot.getGraphics(), true);
///
/// String imageFile = FileSystemStorage.getInstance().getAppHomePath() + "screenshot.png";
/// try(OutputStream os = FileSystemStorage.getInstance().openOutputStream(imageFile)) {
///     ImageIO.getImageIO().save(screenshot, os, ImageIO.FORMAT_PNG, 1);
/// } catch(IOException err) {
///     Log.e(err);
/// }
/// sb.setImageToShare(imageFile, "image/png");
/// ```
///
/// Notice that share looks different on a device
///
/// @author Chen Fishbein
public class ShareButton extends Button implements ActionListener {

    private final Vector shareServices = new Vector();
    private String textToShare;
    private String imageToShare;
    private String imageMimeType;

    /// Default constructor
    public ShareButton() {
        setUIIDFinal("ShareButton");
        //Image shareIcon =  Resources.getSystemResource().getImage("share.png");
        //setIcon(shareIcon);
        FontImage.setMaterialIcon(this, FontImage.MATERIAL_SHARE);
        super.addActionListener(this);
        shareServices.addElement(new SMSShare());
        shareServices.addElement(new EmailShare());
        shareServices.addElement(new FacebookShare());
    }

    /// Gets the text to share
    public String getTextToShare() {
        return textToShare;
    }

    /// Sets the information to share
    ///
    /// #### Parameters
    ///
    /// - `textToShare`
    public void setTextToShare(String textToShare) {
        this.textToShare = textToShare;
    }

    /// Sets the image to share.
    /// Notice some sharing services cannot share image and a text, therefore if
    /// setTextToShare(...) is also used, the sharing service gives image sharing
    /// higher priority.
    ///
    /// #### Parameters
    ///
    /// - `imagePath`: the full file path
    ///
    /// - `imageMimeType`: the image mime type e.g. image/png, image/jpeg
    public void setImageToShare(String imagePath, String imageMimeType) {
        this.imageToShare = imagePath;
        this.imageMimeType = imageMimeType;
    }

    /// Gets the image path to share
    public String getImagePathToShare() {
        return imageToShare;
    }


    /// Adds a sharing service.
    ///
    /// #### Parameters
    ///
    /// - `share`: ShareService
    public void addShareService(ShareService share) {
        shareServices.addElement(share);
    }

    /// invoked when the button is pressed
    ///
    /// #### Parameters
    ///
    /// - `evt`
    @Override
    public void actionPerformed(ActionEvent evt) {
        // postpone the share button action to the next EDT cycle to allow action listeners on the button to 
        // process first
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                if (Display.getInstance().isNativeShareSupported()) {
                    Display.getInstance().share(textToShare, imageToShare, imageMimeType, new Rectangle(
                            ShareButton.this.getAbsoluteX(),
                            ShareButton.this.getAbsoluteY(),
                            ShareButton.this.getWidth(),
                            ShareButton.this.getHeight()
                    ));
                    return;
                }
                Vector sharing;
                if (imageToShare != null) {
                    sharing = new Vector();
                    for (int i = 0; i < shareServices.size(); i++) {
                        ShareService share = (ShareService) shareServices.elementAt(i);
                        if (share.canShareImage()) {
                            sharing.add(share);
                        }
                    }
                } else {
                    sharing = shareServices;
                }
                for (int i = 0; i < sharing.size(); i++) {
                    ShareService share = (ShareService) sharing.elementAt(i);
                    share.setMessage(textToShare);
                    share.setImage(imageToShare, imageMimeType);
                    share.setOriginalForm(getComponentForm());
                }
                List l = new List(sharing);
                l.setCommandList(true);
                final Dialog dialog = new Dialog("Share");
                dialog.setLayout(new BorderLayout());
                dialog.addComponent(BorderLayout.CENTER, l);
                dialog.placeButtonCommands(new Command[]{new Command("Cancel")});
                l.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent evt) {
                        dialog.dispose();
                    }
                });
                dialog.show();
            }
        });
    }

    /// {@inheritDoc}
    @Override
    public String[] getPropertyNames() {
        return new String[]{"textToShare"};
    }

    /// {@inheritDoc}
    @Override
    public Class[] getPropertyTypes() {
        return new Class[]{String.class};
    }

    /// {@inheritDoc}
    @Override
    public Object getPropertyValue(String name) {
        if ("textToShare".equals(name)) {
            return getTextToShare();
        }
        return null;
    }

    /// {@inheritDoc}
    @Override
    public String setPropertyValue(String name, Object value) {
        if ("textToShare".equals(name)) {
            setTextToShare((String) value);
            return null;
        }
        return super.setPropertyValue(name, value);
    }

}
