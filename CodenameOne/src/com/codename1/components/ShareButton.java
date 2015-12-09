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
import com.codename1.ui.Image;
import com.codename1.ui.List;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.util.Resources;
import java.util.Vector;

/**
 * This is a share button.
 * The share button is responsible to share the required info with the available 
 * sharing services
 * 
 * @author Chen
 */
public class ShareButton extends Button implements ActionListener{
    
    private String textToShare;
    
    private String imageToShare;
    
    private String imageMimeType;
    
    private Vector shareServices = new Vector();
    
    /**
     * Default constructor
     */
    public ShareButton() {
        setUIID("ShareButton");
        //Image shareIcon =  Resources.getSystemResource().getImage("share.png");
        //setIcon(shareIcon);
        FontImage.setMaterialIcon(this, FontImage.MATERIAL_SHARE);
        addActionListener(this);
        shareServices.addElement(new SMSShare());
        shareServices.addElement(new EmailShare());
        shareServices.addElement(new FacebookShare());
    }
    
    /**
     * Sets the information to share
     * @param textToShare 
     */
    public void setTextToShare(String textToShare){
        this.textToShare = textToShare;
    }

    /**
     * Gets the text to share
     * @return 
     */
    public String getTextToShare() {
        return textToShare;
    }

    /**
     * Sets the image to share.
     * Notice some sharing services cannot share image and a text, therefore if
     * setTextToShare(...) is also used, the sharing service gives image sharing 
     * higher priority.
     * 
     * @param imagePath the full file path
     * @param imageMimeType the image mime type e.g. image/png, image/jpeg
     */
    public void setImageToShare(String imagePath, String imageMimeType) {
        this.imageToShare = imagePath;
        this.imageMimeType = imageMimeType;
    }

    /**
     * Gets the image path to share
     * @return 
     */
    public String getImagePathToShare() {
        return imageToShare;
    }
    

    /**
     * Adds a sharing service.
     * @param share ShareService
     */
    public void addShareService(ShareService share){
        shareServices.addElement(share);
    }
    
    /**
     * invoked when the button is pressed
     * @param evt 
     */
    public void actionPerformed(ActionEvent evt) {
        if(Display.getInstance().isNativeShareSupported()){
            Display.getInstance().share(textToShare, imageToShare, imageMimeType, new Rectangle(
                    ShareButton.this.getAbsoluteX(),
                    ShareButton.this.getAbsoluteY(),
                    ShareButton.this.getWidth(),
                    ShareButton.this.getHeight()
            ));                
            return;
        }
        Vector sharing;
        if(imageToShare != null){
            sharing = new Vector();
            for (int i = 0; i < shareServices.size(); i++) {
                ShareService share = (ShareService) shareServices.elementAt(i);
                if(share.canShareImage()){
                    sharing.add(share);
                }
            }
        }else{
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

            public void actionPerformed(ActionEvent evt) {
                dialog.dispose();
            }
        });
        dialog.show();
    }

    /**
     * @inheritDoc
     */
    public String[] getPropertyNames() {
        return new String[]{"textToShare"};
    }

    /**
     * @inheritDoc
     */
    public Class[] getPropertyTypes() {
        return new Class[]{String.class};
    }

    /**
     * @inheritDoc
     */
    public Object getPropertyValue(String name) {
        if (name.equals("textToShare")) {
            return getTextToShare();
        }
        return null;
    }

    /**
     * @inheritDoc
     */
    public String setPropertyValue(String name, Object value) {
        if (name.equals("textToShare")) {
            setTextToShare((String) value);
            return null;
        }
        return super.setPropertyValue(name, value);
    }
    
}
