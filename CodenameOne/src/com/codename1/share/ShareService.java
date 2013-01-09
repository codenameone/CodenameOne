/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.share;

import com.codename1.ui.Command;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.events.ActionEvent;

/**
 * This is an abstract sharing service.
 * 
 * @author Chen
 */
public abstract class ShareService extends Command{

    private String message;
    private String image;
    private String mimeType;
    private Form original;
    
    /**
     * Constructor with the service name and icon
     * @param name the service name
     * @param icon the service icon
     */
    public ShareService(String name, Image icon) {
        super(name, icon);
    }

    /**
     * @inheritDoc
     */
    public void actionPerformed(ActionEvent evt) {
        if(image != null){
            share(message, image, mimeType);
        }else{
            share(message);
        }
    }
    
    /**
     * This is the sharing method which should be implemented by the service.
     * 
     * @param text text to share 
     */
    public abstract void share(String text);
    
    /**
     * This is the image sharing method which should be implemented by the 
     * service, if the service returned true on the canShareImage() method
     * Notice not all services are able to share text and image together, in this
     * case the image sharing will be preferred by these services
     * 
     * @param text text to share
     * @param image image to share
     */
    public void share(String text, String image, String imageMimeType){
    }
    
    /**
     * The implementing service needs to declare if it is capable to share an 
     * image
     * @return true if the service can share images
     */
    public abstract boolean canShareImage();
    
    /**
     * Sets the message to share, this is done by the ShareButton and shouldn't
     * be used by the developers
     * @param message 
     */
    public void setMessage(String message){
        this.message = message;
    }
    
    /**
     * Sets the image to share, this is done by the ShareButton and shouldn't
     * be used by the developers
     * @param image the file path to the image 
     * @param mime the image mime type e.g. image/png, image/jpeg
     */
    public void setImage(String image, String mime){
        this.image = image;
        this.mimeType = mime;
    }
    
    /**
     * Sets the Original Form (this is the Form of the share button)
     */
    public void setOriginalForm(Form original){
        this.original = original;
    }
    
    /**
     * Gets the original Form
     */
    public Form getOriginal() {
        return original;
    }
    
    /**
     * Once the share service has finished sharing it should call the finish 
     * method
     */ 
    public void finish(){
        original.showBack();
    }

}
