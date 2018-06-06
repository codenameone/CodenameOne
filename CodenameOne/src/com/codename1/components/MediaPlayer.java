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
import com.codename1.ui.Slider;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.LayeredLayout;
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
    private Image maxIcon;
    private Container buttonsBar;
    private boolean hideNativeVideoControls;
    private boolean showControls=true;
    private Runnable loopOnCompletion;
    private Slider progress;
    private UITimer progressUpdater;
    
    /**
     * Shows the buttons on top of the video
     */
    private boolean onTopMode = true;
    
    /**
     * Shows video position bar as a slider
     */
    private boolean seekBar = true;
    
    /**
     * UIID for the seekBar slider
     */
    private String seekBarUIID = null;

    /**
     * Includes a maximize icon in the bar to show the native player
     */
    private boolean maximize = true;
    
    private boolean userSetIcons = false;
    private Media video;
    private String dataSource;
    
    private String pendingDataURI;
    private boolean autoplay;
    private boolean loop;
    //private Runnable onCompletion;
    
    /**
     * Empty constructor
     */
    public MediaPlayer() {
        playIcon = FontImage.createMaterial(FontImage.MATERIAL_PLAY_ARROW, "Button", 3);
        pauseIcon = FontImage.createMaterial(FontImage.MATERIAL_PAUSE, "Button", 3);
        fwdIcon = FontImage.createMaterial(FontImage.MATERIAL_FAST_FORWARD, "Button", 3);
        backIcon = FontImage.createMaterial(FontImage.MATERIAL_FAST_REWIND, "Button", 3);
        maxIcon = FontImage.createMaterial(FontImage.MATERIAL_FULLSCREEN, "Button", 3);
    }
    
    /**
     * On platforms that include native video player controls (Android and iOS), this allows you
     * to hide those controls.
     * @param hideNativeControls Set {@literal true} to hide the native video controls for this player.
     * @see Display#isNativeVideoPlayerControlsIncluded() 
     * @see #setHideNativeVideoControls(boolean) 
     * @see #usesNativeVideoControls() 
     */
    public void setHideNativeVideoControls(boolean hideNativeControls) {
        this.hideNativeVideoControls = hideNativeControls;
        if (video != null) {
            video.setVariable(Media.VARIABLE_NATIVE_CONTRLOLS_EMBEDDED, !hideNativeControls && showControls);
        }
    }
    
    /**
     * On platforms that include native video player controls (Android and iOS), this indicates whether
     * these controls should be hidden for this media player.
     * @return {@literal true} if native video player controls should be hidden.
     * @see Display#isNativeVideoPlayerControlsIncluded() 
     * @see #setHideNativeVideoControls(boolean) 
     * @see #usesNativeVideoControls() 
     */
    public boolean isHideNativeVideoControls() {
        return hideNativeVideoControls;
    }
    
    /**
     * Checks to see if this player uses native video controls.  For this to be {@literal true},
     * the platform must support native video controls (iOS and Android) (See {@link Display#isNativeVideoPlayerControlsIncluded() }
     * to find out if current platform supports this; <strong>AND</strong> {@link #isHideNativeVideoControls() }
     * must be false.
     * @return True if this player uses native video controls.
     * @see #isHideNativeVideoControls() 
     * @see #setHideNativeVideoControls(boolean) 
     * @see Display#isNativeVideoPlayerControlsIncluded() 
     */
    public boolean usesNativeVideoControls() {
        return Display.getInstance().isNativeVideoPlayerControlsIncluded() && !hideNativeVideoControls;
    }
    
    /**
     * Shows the controls for this media player.  If the player is set to use 
     * native controls, then this will show the native controls.  Otherwise it
     * shows the lightweight controls.
     */
    public void showControls() {
        if (!showControls) {
            showControls = true;
            if (isInitialized()) {
                buttonsBar.setVisible(true);
                buttonsBar.setHidden(false);
                animateLayoutFade(300, 0);
            }
        }
        if (video != null && usesNativeVideoControls()) {
            video.setVariable(Media.VARIABLE_NATIVE_CONTRLOLS_EMBEDDED, true);
        }
    }
    
    /**
     * Hides the controls for this media player.  If the player is set to use native 
     * controls, then this will hide the native controls.  Otherwise it hides the 
     * lightweight controls.
     */
    public void hideControls() {
        if (showControls) {
            showControls = false;
            if (isInitialized()) {
                buttonsBar.setVisible(false);
                buttonsBar.setHidden(true);
                animateLayoutFade(300, 0);
            }
        }
        if (video != null && usesNativeVideoControls()) {
            video.setVariable(Media.VARIABLE_NATIVE_CONTRLOLS_EMBEDDED, false);
        }
    }
    
    /**
     * Empty constructor
     */
    public MediaPlayer(Media video) {
        this();
        this.video = video;
        updateLoopOnCompletionHandler();
        //initUI();
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
            Image max = UIManager.getInstance().getThemeImageConstant("mediaMaxImage");
            if(max != null){
                maxIcon = max;
            }
            
        }
        if(pendingDataURI != null) {
            setDataSource(pendingDataURI);
            pendingDataURI = null;
        }
        initUI();
    }

    private void checkProgressSlider() {
        if(progressUpdater == null) {
            progressUpdater = UITimer.timer(1000, true, getComponentForm(),
                new Runnable() {
                public void run() {
                    float dur = video.getDuration();
                    if(dur > 0) {
                        float pos = video.getTime();
                        int offset = (int)(pos / dur * 100.0f);
                        if(offset > -1 && offset < 101) {
                            progress.setProgress(offset);
                        }
                    }
                }
            });
        }
    }
    
    private void stopProgressSlider() {
        if(progressUpdater != null) {
            progressUpdater.cancel();
            progressUpdater = null;
        }
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
                stopProgressSlider();
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
     * Sets the maximize Button Icon
     * @param maxIcon 
     */
    public void setMaxIcon(Image maxIcon ) {
        this.maxIcon = maxIcon ;
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
        video = MediaManager.createMedia(uri, true, onCompletion);
        updateLoopOnCompletionHandler();
        if (isInitialized()) {
            initUI();
        }
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
        
        video = MediaManager.createMedia(is, mimeType, onCompletion);
        
        updateLoopOnCompletionHandler();
        if (isInitialized()) {
            initUI();
        }
    }

    private void initUI() {
        removeAll();
        if(onTopMode) {
            setLayout(new LayeredLayout());        
        } else {
            setLayout(new BorderLayout());        
        }
        
        if(video != null && video.getVideoComponent() != null){
            Component videoComponent = video.getVideoComponent();
            if (videoComponent != null) {
                if(onTopMode) {
                    addComponent(videoComponent);        
                } else {
                    addComponent(BorderLayout.CENTER, videoComponent);        
                }
            }
        }
        
        
        if(seekBar) {
            buttonsBar = new Container(new BorderLayout());
            progress = new Slider();
            progress.setEditable(true);
            buttonsBar.addComponent(BorderLayout.CENTER, 
                FlowLayout.encloseCenterMiddle(progress));
            progress.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    float dur = video.getDuration();
                    if(dur > 0) {
                        float pos = progress.getProgress();
                        int t = (int)(pos / 100.0f * dur);
                        video.setTime(t);
                    }
                }
            });
        } else {
            buttonsBar = new Container(new FlowLayout(Container.CENTER));
        }
        if(onTopMode) {
            addComponent(BorderLayout.south(buttonsBar));
        } else {
            addComponent(BorderLayout.SOUTH, buttonsBar);
        }
        if (usesNativeVideoControls() || !showControls) {
            buttonsBar.setVisible(false);
            buttonsBar.setHidden(true);
        }
        
        if(!seekBar) {
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
                                        checkProgressSlider();
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
                    checkProgressSlider();
                    play.setUIID("MediaPlayerPause");
                    if (getPauseIcon() != null) {
                        play.setIcon(getPauseIcon());
                    } else {
                        play.setText("pause");
                    }
                    play.repaint();
                }else{
                    video.pause();
                    stopProgressSlider();
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
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                if (video != null && video.isPlaying()) {
                    play.setUIID("MediaPlayerPause");
                    if (getPauseIcon() != null) {
                        play.setIcon(getPauseIcon());
                    } else {
                        play.setText("pause");
                    }
                } else if (video != null && !video.isPlaying()) {
                    play.setUIID("MediaPlayerPlay");
                    if (getPlayIcon() != null) {
                        play.setIcon(getPlayIcon());
                    } else {
                        play.setText("play");
                    }
                }
            }
        });
        if(seekBar) {
            buttonsBar.addComponent(BorderLayout.WEST, play);
        } else {
            buttonsBar.addComponent(play);
        }

        //if(video == null || !video.isNativePlayerMode()){        
        if(!seekBar) {
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
        
        if(maximize) {
            Button max = new Button();
            max.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    if(video == null){
                        return;
                    }
                    video.setNativePlayerMode(true);
                }
            });
            max.setUIID("MediaPlayerMax");
            if(maxIcon != null){
                max.setIcon(maxIcon);
            }else{
                max.setText("max");
            }
            if(seekBar) {
                buttonsBar.addComponent(BorderLayout.EAST, max);
            } else {
                buttonsBar.addComponent(max);
            }
        }
        //}
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

    private void updateLoopOnCompletionHandler() {
        if (isLoop() && loopOnCompletion == null) {
            loopOnCompletion = new Runnable() {

                @Override
                public void run() {
                    if (video != null) {
                        video.setTime(0);
                        video.play();
                        checkProgressSlider();
                    }
                }

            };
        }
        if (isLoop()) {
            Display.getInstance().addCompletionHandler(video, loopOnCompletion);
        } else {
            if (loopOnCompletion != null) {
                Display.getInstance().removeCompletionHandler(video, loopOnCompletion);
            }
        }
    }
    
    /**
     * Sets playback to loop
     * @param loop the loop to set
     */
    public void setLoop(boolean loop) {
        if (loop != this.loop) {
            this.loop = loop;
            updateLoopOnCompletionHandler();
        }
    }

    /*
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
    */

    /**
     * Shows the buttons on top of the video
     * @return the onTopMode
     */
    public boolean isOnTopMode() {
        return onTopMode;
    }

    /**
     * Shows the buttons on top of the video
     * @param onTopMode the onTopMode to set
     */
    public void setOnTopMode(boolean onTopMode) {
        this.onTopMode = onTopMode;
    }

    /**
     * Shows video position bar as a slider
     * @return the seekBar
     */
    public boolean isSeekBar() {
        return seekBar;
    }

    /**
     * Shows video position bar as a slider
     * @param seekBar the seekBar to set
     */
    public void setSeekBar(boolean seekBar) {
        this.seekBar = seekBar;
    }

    /**
     * UIID for the seekBar slider
     * @return the seekBarUIID
     */
    public String getSeekBarUIID() {
        return seekBarUIID;
    }

    /**
     * UIID for the seekBar slider
     * @param seekBarUIID the seekBarUIID to set
     */
    public void setSeekBarUIID(String seekBarUIID) {
        this.seekBarUIID = seekBarUIID;
    }

    /**
     * Includes a maximize icon in the bar to show the native player
     * @return the maximize
     */
    public boolean isMaximize() {
        return maximize;
    }

    /**
     * Includes a maximize icon in the bar to show the native player
     * @param maximize the maximize to set
     */
    public void setMaximize(boolean maximize) {
        this.maximize = maximize;
    }
}
