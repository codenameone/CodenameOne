/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.facebook.ui;

import com.codename1.facebook.FaceBookAccess;
import com.codename1.ui.Button;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import java.io.IOException;

/**
 *
 * @author Chen Fishbein
 */
public class LikeButton extends Button implements ActionListener{

    private String postId;
    
    public LikeButton(String postId) {
        this.postId = postId;
        setUIID("LikeButton");
        setText("Like");
        addActionListener(this);
    }

    public void actionPerformed(ActionEvent evt) {
        try {
            FaceBookAccess.getInstance().postLike(postId);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
}
