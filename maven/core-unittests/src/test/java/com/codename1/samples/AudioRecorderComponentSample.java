package com.codename1.samples;

import com.codename1.components.AudioRecorderComponent;
import com.codename1.components.ToastBar;
import com.codename1.io.File;
import com.codename1.io.FileSystemStorage;
import com.codename1.io.Log;
import com.codename1.media.MediaManager;
import com.codename1.media.MediaRecorderBuilder;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Dialog;
import com.codename1.ui.Form;
import com.codename1.ui.Sheet;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.util.AsyncResource;
import java.io.IOException;

/**
 * Test-local copy of the AudioRecorderComponent sample used by AudioRecorderComponentSampleTest.
 */
public class AudioRecorderComponentSample {

    private Form current;
    private String lastRecordingPath;

    public void start() {
        if (current != null) {
            current.show();
            return;
        }
        Form hi = new Form("Audio Recorder Sample", BoxLayout.y());
        Button record = new Button("Record Audio");
        record.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                recordAudio().onResult(new com.codename1.util.AsyncResult<String>() {
                    @Override
                    public void onReady(String res, Throwable err) {
                        if (err != null) {
                            Log.e(err);
                            ToastBar.showErrorMessage(err.getMessage());
                            return;
                        }
                        if (res == null) {
                            return;
                        }
                        try {
                            MediaManager.createMedia(res, false).play();
                        } catch (IOException ex) {
                            Log.e(ex);
                            ToastBar.showErrorMessage(ex.getMessage());
                        }
                    }
                });
            }
        });
        hi.add(record);
        hi.show();
    }

    AsyncResource<String> recordAudio() {
        AsyncResource<String> out = new AsyncResource<String>();
        String mime = MediaManager.getAvailableRecordingMimeTypes()[0];
        String ext = mime.indexOf("mp3") != -1 ? "mp3" : mime.indexOf("wav") != -1 ? "wav" : mime.indexOf("aiff") != -1 ? "aiff" : "aac";
        MediaRecorderBuilder builder = new MediaRecorderBuilder()
                .path(new File("myaudio." + ext).getAbsolutePath())
                .mimeType(mime);
        lastRecordingPath = builder.getPath();

        final AudioRecorderComponent cmp = new AudioRecorderComponent(builder);
        final Sheet sheet = new Sheet(null, "Record Audio");
        sheet.getContentPane().setLayout(new com.codename1.ui.layouts.BorderLayout());
        sheet.getContentPane().add(com.codename1.ui.layouts.BorderLayout.CENTER, cmp);
        cmp.addActionListener(new com.codename1.ui.events.ActionListener() {
            @Override
            public void actionPerformed(com.codename1.ui.events.ActionEvent e) {
                switch (cmp.getState()) {
                    case Accepted:
                        CN.getCurrentForm().getAnimationManager().flushAnimation(new Runnable() {
                            public void run() {
                                sheet.back();
                                sheet.addCloseListener(new ActionListener() {
                                    @Override
                                    public void actionPerformed(ActionEvent evt) {
                                        sheet.removeCloseListener(this);
                                        out.complete(builder.getPath());
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
                            public void run() {
                                sheet.back();
                                sheet.addCloseListener(new ActionListener() {
                                    @Override
                                    public void actionPerformed(ActionEvent evt) {
                                        sheet.removeCloseListener(this);
                                        out.complete(null);
                                    }

                                });
                            }
                        });


                        break;
                }
            }

        });
        sheet.addCloseListener(new com.codename1.ui.events.ActionListener() {
            @Override
            public void actionPerformed(com.codename1.ui.events.ActionEvent e) {
                if (cmp.getState() != AudioRecorderComponent.RecorderState.Accepted && cmp.getState() != AudioRecorderComponent.RecorderState.Canceled) {
                    FileSystemStorage fs = FileSystemStorage.getInstance();
                    if (fs.exists(builder.getPath())) {
                        FileSystemStorage.getInstance().delete(builder.getPath());
                    }
                    CN.getCurrentForm().getAnimationManager().flushAnimation(new Runnable() {
                        public void run() {
                            out.complete(null);
                        }
                    });
                }
            }

        });
        sheet.show();
        return out;
    }

    public String getLastRecordingPath() {
        return lastRecordingPath;
    }

    public void stop() {
        current = com.codename1.ui.CN.getCurrentForm();
        if (current instanceof Dialog) {
            ((Dialog) current).dispose();
            current = com.codename1.ui.CN.getCurrentForm();
        }
    }

    public void destroy() {
    }
}
