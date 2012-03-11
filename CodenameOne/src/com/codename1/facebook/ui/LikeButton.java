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
package com.codename1.facebook.ui;

import com.codename1.facebook.FaceBookAccess;
import com.codename1.ui.Button;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import java.io.IOException;

/**
 * Generic "Like" button that enables us to submit a like to facebook
 *
 * @author Chen Fishbein
 */
public class LikeButton extends Button implements ActionListener {

    private String postId;
    
    /**
     * Constructor accepting the post id
     * 
     * @param postId 
     */
    public LikeButton(String postId) {
        this.postId = postId;
    }

    /**
     * Default constructor
     */
    public LikeButton() {
        setUIID("LikeButton");
        setText("Like");
        addActionListener(this);
    }

    /**
     * @inheritDoc
     */
    public void actionPerformed(ActionEvent evt) {
        try {
            FaceBookAccess.getInstance().postLike(getPostId());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * @return the postId
     */
    public String getPostId() {
        return postId;
    }

    /**
     * @param postId the postId to set
     */
    public void setPostId(String postId) {
        this.postId = postId;
    }
    
}
