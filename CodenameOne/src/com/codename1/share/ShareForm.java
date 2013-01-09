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

import com.codename1.io.FileSystemStorage;
import com.codename1.io.Util;
import com.codename1.ui.*;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.util.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 * @author Chen
 */
class ShareForm extends Form{

    private TextField to = new TextField();
    private TextArea message = new TextArea(5, 20);
    private Button post = new Button("Post");
    
    ShareForm(final Form contacts, String title, String toShare, String txt, ActionListener share) {
        this(contacts, title, toShare, txt, null, share);
    }
    
    ShareForm(final Form contacts, String title, String toShare, String txt, String image, 
            ActionListener share) {
        setTitle(title);
        setLayout(new BorderLayout());
        this.message.setText(txt);
        post.addActionListener(share);
        if(toShare != null){
            this.to.setText(toShare);
            addComponent(BorderLayout.NORTH, to);
        }
        if(image == null){
            addComponent(BorderLayout.CENTER, message);
        }else{
            Container body = new Container(new BorderLayout());
            if(txt != null && txt.length() > 0){
                body.addComponent(BorderLayout.SOUTH, message);
            }
            Label im = new Label();
            ImageIO scale = ImageIO.getImageIO();
            if(scale != null){
                InputStream is = null;
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                try {
                    is = FileSystemStorage.getInstance().openInputStream(image);
                    scale.save(is, os, ImageIO.FORMAT_JPEG, 200, 200, 1);
                    Image i = Image.createImage(os.toByteArray(), 0, os.toByteArray().length);
                    im.setIcon(i);
                } catch (IOException ex) {
                    //if failed to open the image file simply put the path as text
                    im.setText(image);                   
                }finally{
                    Util.cleanup(os);
                    Util.cleanup(is);                                    
                }
            }else{
                im.setText(image);
            }
            body.addComponent(BorderLayout.CENTER, im);            
            addComponent(BorderLayout.CENTER, body);        
        }
        addComponent(BorderLayout.SOUTH, post);
        Command back = new Command("Back") {

            public void actionPerformed(ActionEvent evt) {
                
                contacts.showBack();
            }
        };
        addCommand(back);
        setBackCommand(back);
    }
    
    String getTo(){
        return to.getText();
    }
    
    String getMessage(){
        return message.getText();
    }
    
}
