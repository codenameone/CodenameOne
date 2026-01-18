package com.codename1.impl;

import com.codename1.components.AudioRecorderComponent;
import com.codename1.io.FileSystemStorage;
import com.codename1.media.MediaRecorderBuilder;
import com.codename1.ui.CN;
import com.codename1.ui.Sheet;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;

class CaptureAudioActionListener implements ActionListener<ActionEvent> {
    private final AudioRecorderComponent cmp;
    private final Sheet sheet;
    private final ActionListener<ActionEvent> response;
    private final MediaRecorderBuilder builder;

    public CaptureAudioActionListener(AudioRecorderComponent cmp, Sheet sheet, ActionListener<ActionEvent> response, MediaRecorderBuilder builder) {
        this.cmp = cmp;
        this.sheet = sheet;
        this.response = response;
        this.builder = builder;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        switch (cmp.getState()) {
            case Accepted:
                CN.getCurrentForm().getAnimationManager().flushAnimation(new Runnable() {
                    @Override
                    public void run() {
                        sheet.back();
                        sheet.addCloseListener(new ActionListener<ActionEvent>() {
                            @Override
                            public void actionPerformed(ActionEvent evt) {
                                sheet.removeCloseListener(this);
                                response.actionPerformed(new ActionEvent(builder.getPath()));
                            }

                        });
                    }
                });


                break;
            case Canceled:
                FileSystemStorage fs = FileSystemStorage.getInstance();
                if (fs.exists(builder.getPath())) {
                    FileSystemStorage.getInstance().delete(builder.getPath());
                }
                CN.getCurrentForm().getAnimationManager().flushAnimation(new Runnable() {
                    @Override
                    public void run() {
                        sheet.back();
                        sheet.addCloseListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent evt) {
                                sheet.removeCloseListener(this);
                                response.actionPerformed(new ActionEvent(null));
                            }

                        });
                    }
                });


                break;
            default:
                break;
        }
    }

}
