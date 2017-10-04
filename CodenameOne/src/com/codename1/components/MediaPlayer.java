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

import com.codename1.io.Log;
import com.codename1.media.Media;
import com.codename1.media.MediaManager;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.Image;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.UITimer;
import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;

/**
 * <p>Video playback component with control buttons for back, play/pause and 
 * forward buttons. In the simulator those controls are implemented locally but on the
 * device the native playback controls are used.
 * </p>
 * 
 * <script src="https://gist.github.com/codenameone/fb73f5d47443052f8956.js"></script>
 * <img src="https://www.codenameone.com/img/developer-guide/components-mediaplayer.png" alt="Media player sample" />
 * 
 */
public class MediaPlayer extends Container {
    private Image playIcon;
    private Image pauseIcon;
    private Image backIcon;
    private Image fwdIcon;
    
    private boolean userSetIcons = false;
    private Media video;
    private String dataSource;
    
    private String pendingDataURI;
    private boolean autoplay;
    private boolean loop;
    private Runnable onCompletion;
    
    /**
     * Empty constructor
     */
    public MediaPlayer() {
        playIcon = FontImage.createMaterial(FontImage.MATERIAL_PLAY_ARROW, "Button", 3);
        pauseIcon = FontImage.createMaterial(FontImage.MATERIAL_PAUSE, "Button", 3);
        fwdIcon = FontImage.createMaterial(FontImage.MATERIAL_FAST_FORWARD, "Button", 3);
        backIcon = FontImage.createMaterial(FontImage.MATERIAL_FAST_REWIND, "Button", 3);
    }
    
    /**
     * Empty constructor
     */
    public MediaPlayer(Media video) {
        this();
        this.video = video;
        initUI();
    }
    
    /**
     * Returns the Media Object of this MediaPlayer
     * @return 
     */
    public Media getMedia(){
        return video;
    }
    
    /**
     * {@inheritDoc}
     */
    protected void initComponent() {
        if(userSetIcons){
            Image play = UIManager.getInstance().getThemeImageConstant("mediaPlayImage");
            if(play != null){
                playIcon = play;
            }
            Image pause = UIManager.getInstance().getThemeImageConstant("mediaPauseImage");
            if(pause != null){
                pauseIcon = pause;
            }            
            Image back = UIManager.getInstance().getThemeImageConstant("mediaBackImage");
            if(back != null){
                backIcon = back;
            }
            Image fwd = UIManager.getInstance().getThemeImageConstant("mediaFwdImage");
            if(fwd != null){
                fwdIcon = fwd;
            }
            
        }
        if(pendingDataURI != null) {
            setDataSource(pendingDataURI);
            pendingDataURI = null;
        }
        initUI();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void deinitialize() {
        super.deinitialize();
        if(autoplay) {
            if(video != null && video.isPlaying()){
                video.pause();
            }
        }
    }
    
    /**
     * {@inheritDoc}
     */
    protected Dimension calcPreferredSize() {
        if(video == null && dataSource == null) {
            return new Dimension(240, 320);
        }
        return super.calcPreferredSize();
    }

    /**
     * Sets the back Button Icon
     * @param backIcon 
     */
    public void setBackIcon(Image backIcon) {
        this.backIcon = backIcon;
        userSetIcons = true;
    }

    /**
     * Sets the forward Button Icon
     * @param fwdIcon 
     */
    public void setFwdIcon(Image fwdIcon) {
        this.fwdIcon = fwdIcon;
        userSetIcons = true;
    }

    /**
     * Sets the pause Button Icon
     * @param pauseIcon 
     */
    public void setPauseIcon(Image pauseIcon) {
        this.pauseIcon = pauseIcon;
        userSetIcons = true;
    }

    /**
     * Sets the play Button Icon
     * @param playIcon 
     */
    public void setPlayIcon(Image playIcon) {
        this.playIcon = playIcon;
        userSetIcons = true;
    }
    
    /**
     * Sets the data source of this video player
     * @param uri the uri of the media can start with file://, http:// (can also
     * use rtsp:// although may not be supported on all target platforms)
     * @throws IOException if creation of media from the given URI has failed
     */
    public void setDataSource(String uri, Runnable onCompletion) throws IOException{
        dataSource = uri;
        if(onCompletion instanceof CompletionWrapper) {
            video = MediaManager.createMedia(uri, true, onCompletion);
        } else {
            this.onCompletion = onCompletion;
            video = MediaManager.createMedia(uri, true, new CompletionWrapper());
        }
        initUI();
    }
    
    /**
     * Convenience JavaBean method, see other version of this method
     * @param uri the URL for the media
     */
    public void setDataSource(final String uri) {
        if(!isInitialized()) {
            pendingDataURI = uri;
            return;
        }
        if(dataSource == null || !dataSource.equals(uri)) {
            Display.getInstance().startThread(new Runnable() {
                public void run() {
                    try {
                        setDataSource(uri, null);
                    } catch(Throwable t) {
                        Log.e(t);
                    }
                }
            }, "Media Thread").start();
        }
    }
    
    /**
     * Convenience JavaBean method, see other version of this method
     * 
     * @return the data source uri
     */
    public String getDataSource() {
        if(!isInitialized() && dataSource == null) {
            return pendingDataURI;
        }        
        return dataSource;
    }
    
    /**
     * Sets the data source of this video player
     * @param is the stream containing the media data
     * @param mimeType the type of the data in the stream
     * @throws java.io.IOException if the creation of the Media has failed
     */
    public void setDataSource(InputStream is, String mimeType, Runnable onCompletion) throws IOException{
        if(onCompletion instanceof CompletionWrapper) {
            video = MediaManager.createMedia(is, mimeType, onCompletion);
        } else {
            this.onCompletion = onCompletion;
            video = MediaManager.createMedia(is, mimeType, new CompletionWrapper());
        }
        initUI();
    }

    private void initUI() {
        removeAll();
        setLayout(new BorderLayout());        
        
        if(video != null){
            addComponent(BorderLayout.CENTER, video.getVideoComponent());        
        }
        
        Container buttonsBar = new Container(new FlowLayout(Container.CENTER));
        if(!Display.getInstance().isNativeVideoPlayerControlsIncluded()) {
            addComponent(BorderLayout.SOUTH, buttonsBar);
        }
        
        if(video == null || !video.isNativePlayerMode()){
            Button back = new Button();
            back.setUIID("MediaPlayerBack");
            if(backIcon != null){
                back.setIcon(backIcon);
            }else{
                back.setText("Back");
            }
            buttonsBar.addComponent(back);
            back.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent evt) {
                    if(video == null){
                        return;
                    }
                    int t = video.getTime();
                    video.setTime(t - 2);
                }
            });        
        }
        
        final Button play = new Button();
        play.setUIID("MediaPlayerPlay");
        if(playIcon != null){
            play.setIcon(playIcon);
        }else{
            play.setText("play");
        }
        if(autoplay) {
            if(video != null && !video.isPlaying()){
                if (getPauseIcon() != null) {
                    play.setIcon(getPauseIcon());
                } else {
                    play.setText("pause");
                }
                Timer t = new Timer();
                t.schedule(new TimerTask() {
                    public void run() {
                        if (isInitialized()) {
                            Display.getInstance().callSerially(new Runnable() {
                                public void run() {
                                    if (video != null && !video.isPlaying() && isInitialized()) {
                                        video.play();
                                    }
                                }
                            });
                        }
                    }
                        
                }, 300l);
                
                //video.play();
            }
        }
        play.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                if(video == null){
                    return;
                }
                
                if(!video.isPlaying()){
                    video.play();
                    play.setUIID("MediaPlayerPause");
                    if (getPauseIcon() != null) {
                        play.setIcon(getPauseIcon());
                    } else {
                        play.setText("pause");
                    }
                    play.repaint();
                }else{
                    video.pause();
                    play.setUIID("MediaPlayerPlay");
                    if (getPlayIcon() != null) {
                        play.setIcon(getPlayIcon());
                    } else {
                        play.setText("play");
                    }
                    play.repaint();
                }
            }
        });
        buttonsBar.addComponent(play);

        if(video == null || !video.isNativePlayerMode()){        
            Button fwd = new Button();
            fwd.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent evt) {
                    if(video == null){
                        return;
                    }
                    int t = video.getTime();
                    video.setTime(t + 1);
                }
            });
            fwd.setUIID("MediaPlayerFwd");
            if(fwdIcon != null){
                fwd.setIcon(fwdIcon);
            }else{
                fwd.setText("fwd");
            }
            buttonsBar.addComponent(fwd);           
        }
        if(isInitialized()) {
            revalidate();
        }
    }

    
    public void run() {
    }

    

    /**
     * {@inheritDoc}
     */
    public String[] getPropertyNames() {
        return new String[] {"backIcon", "forwardIcon", "pauseIcon", "playIcon", "dataSource"};
    }

    /**
     * {@inheritDoc}
     */
    public Class[] getPropertyTypes() {
       return new Class[] {Image.class, Image.class, Image.class, Image.class, String.class};
    }

    /**
     * {@inheritDoc}
     */
    public Object getPropertyValue(String name) {
        if(name.equals("backIcon")) {
            return getBackIcon();
        }
        if(name.equals("forwardIcon")) {
            return getFwdIcon();
        }
        if(name.equals("playIcon")) {
            return getPlayIcon();
        }
        if(name.equals("pauseIcon")) {
            return getPauseIcon();
        }
        if(name.equals("dataSource")) {
            return getDataSource();
        }
        return super.getPropertyValue(name);
    }

    /**
     * {@inheritDoc}
     */
    public String setPropertyValue(String name, Object value) {
        if(name.equals("backIcon")) {
            this.backIcon = (Image)value;
            return null;
        }
        if(name.equals("forwardIcon")) {
            this.fwdIcon = (Image)value;
            return null;
        }
        if(name.equals("playIcon")) {
            this.playIcon = (Image)value;
            return null;
        }
        if(name.equals("pauseIcon")) {
            this.pauseIcon = (Image)value;
            return null;
        }
        if(name.equals("dataSource")) {
            setDataSource((String)value);
            return null;
        }
        return super.setPropertyValue(name, value);
    }

    /**
     * @return the playIcon
     */
    public Image getPlayIcon() {
        return playIcon;
    }

    /**
     * @return the pauseIcon
     */
    public Image getPauseIcon() {
        return pauseIcon;
    }

    /**
     * @return the backIcon
     */
    public Image getBackIcon() {
        return backIcon;
    }

    /**
     * @return the fwdIcon
     */
    public Image getFwdIcon() {
        return fwdIcon;
    }

    /**
     * Sets playback to start automatically
     * @return the autoplay
     */
    public boolean isAutoplay() {
        return autoplay;
    }

    /**
     * Sets playback to start automatically
     * @param autoplay the autoplay to set
     */
    public void setAutoplay(boolean autoplay) {
        this.autoplay = autoplay;
    }

    /**
     * Sets playback to loop
     * @return the loop
     */
    public boolean isLoop() {
        return loop;
    }

    /**
     * Sets playback to loop
     * @param loop the loop to set
     */
    public void setLoop(boolean loop) {
        this.loop = loop;
    }

    class CompletionWrapper implements Runnable {
        public void run() {
            if(onCompletion != null) {
                onCompletion.run();
            }
            if(isLoop()) {
                try {
                    setDataSource(dataSource, this);
                } catch(IOException err) {
                    Log.e(err);
                }
            }
        }
    }
}
