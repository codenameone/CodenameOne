package com.codename1.samples;

import com.codename1.components.AudioRecorderComponent;
import com.codename1.components.ToastBar;
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
        final boolean[] completed = new boolean[1];
        String mime = MediaManager.getAvailableRecordingMimeTypes()[0];
        String ext = mime.indexOf("mp3") != -1 ? "mp3" : mime.indexOf("wav") != -1 ? "wav" : mime.indexOf("aiff") != -1 ? "aiff" : "aac";
        String appHome = FileSystemStorage.getInstance().getAppHomePath();
        if (appHome == null) {
            appHome = "file://app/";
        }
        MediaRecorderBuilder builder = new MediaRecorderBuilder()
                .path(appHome + "myaudio." + ext)
                .mimeType(mime);
        lastRecordingPath = builder.getPath();

        Form host = CN.getCurrentForm();
        if (host == null) {
            host = new Form("Audio Recorder Host", BoxLayout.y());
            host.show();
        }
        final AudioRecorderComponent cmp = new AudioRecorderComponent(builder);
        final Sheet sheet = new Sheet(host, "Record Audio");
        sheet.getContentPane().setLayout(new com.codename1.ui.layouts.BorderLayout());
        sheet.getContentPane().add(com.codename1.ui.layouts.BorderLayout.CENTER, cmp);
        final com.codename1.ui.events.ActionListener stateHandler = new com.codename1.ui.events.ActionListener() {
            @Override
            public void actionPerformed(com.codename1.ui.events.ActionEvent e) {
                switch (cmp.getState()) {
                    case Accepted:
                        if (!completed[0]) {
                            completed[0] = true;
                            out.complete(builder.getPath());
                        }
                        CN.getCurrentForm().getAnimationManager().flushAnimation(new Runnable() {
                            public void run() {
                                sheet.back();
                            }
                        });
                        break;
                    case Canceled:
                        FileSystemStorage fs = FileSystemStorage.getInstance();
                        if (fs.exists(builder.getPath())) {
                            FileSystemStorage.getInstance().delete(builder.getPath());
                        }
                        if (!completed[0]) {
                            completed[0] = true;
                            out.complete(null);
                        }
                        CN.getCurrentForm().getAnimationManager().flushAnimation(new Runnable() {
                            public void run() {
                                sheet.back();
                            }
                        });
                        break;
                    default:
                        break;
                }
            }

        };
        cmp.addActionListener(stateHandler);
        com.codename1.ui.events.ActionListener closingHandler = new com.codename1.ui.events.ActionListener() {
            public void actionPerformed(com.codename1.ui.events.ActionEvent e) {
                if (completed[0]) {
                    return;
                }
                AudioRecorderComponent.RecorderState state = cmp.getState();
                if (state == AudioRecorderComponent.RecorderState.Accepted || state == AudioRecorderComponent.RecorderState.Pending) {
                    completed[0] = true;
                    out.complete(builder.getPath());
                    return;
                }
                FileSystemStorage fs = FileSystemStorage.getInstance();
                if (fs.exists(builder.getPath())) {
                    FileSystemStorage.getInstance().delete(builder.getPath());
                }
                completed[0] = true;
                out.complete(null);
            }
        };
        sheet.addCloseListener(closingHandler);
        sheet.addBackListener(closingHandler);
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
