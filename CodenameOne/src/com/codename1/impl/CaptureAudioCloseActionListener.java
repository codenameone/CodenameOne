package com.codename1.impl;

import com.codename1.components.AudioRecorderComponent;
import com.codename1.io.FileSystemStorage;
import com.codename1.media.MediaRecorderBuilder;
import com.codename1.ui.CN;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;

class CaptureAudioCloseActionListener implements ActionListener<ActionEvent> {
    private final AudioRecorderComponent cmp;
    private final MediaRecorderBuilder builder;
    private final ActionListener<ActionEvent> response;

    public CaptureAudioCloseActionListener(AudioRecorderComponent cmp, MediaRecorderBuilder builder, ActionListener<ActionEvent> response) {
        this.cmp = cmp;
        this.builder = builder;
        this.response = response;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (cmp.getState() != AudioRecorderComponent.RecorderState.Accepted && cmp.getState() != AudioRecorderComponent.RecorderState.Canceled) {
            FileSystemStorage fs = FileSystemStorage.getInstance();
            if (fs.exists(builder.getPath())) {
                FileSystemStorage.getInstance().delete(builder.getPath());
            }
            CN.getCurrentForm().getAnimationManager().flushAnimation(new Runnable() {
                public void run() {
                    response.actionPerformed(new ActionEvent(null));
                }
            });
        }
    }
}
