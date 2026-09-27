/*
 * Copyright (c) 2018, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.demos.signin;

import com.codename1.ui.Command;
import com.codename1.ui.Display;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.URLImage;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.layouts.BorderLayout;

public class UserForm extends Form{

    private String name;
    private String imageURL;
    
    public UserForm(String name, EncodedImage placeHolder, String imageURL) {
        this.name = name;
        this.imageURL = imageURL;
        setTitle("Welcome!");
        setLayout(new BorderLayout());
        
        Label icon = new Label(placeHolder);
        icon.setUIID("Picture");
        if(imageURL != null){
            icon.setIcon(URLImage.createToStorage(placeHolder, name, imageURL, null));
        }
        addComponent(BorderLayout.CENTER, icon);
        Label nameLbl = new Label(name);
        nameLbl.setUIID("Name");
        addComponent(BorderLayout.SOUTH, nameLbl);
        final Form current = Display.getInstance().getCurrent();
        Command back = new Command("Back"){

            @Override
            public void actionPerformed(ActionEvent evt) {
                current.showBack();;
            }

        };
        addCommand(back);
        setBackCommand(back);
        
    }
    
}
