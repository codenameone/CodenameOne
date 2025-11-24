package com.codename1.media;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.Slider;
import com.codename1.ui.SpanLabel;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.MessageEvent;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.util.SuccessCallback;

import static org.junit.jupiter.api.Assertions.*;

class AsyncMediaSampleTest extends UITestBase {

    @FormTest
    void promptMessagesCompletePromptPromiseAndAreConsumed() {
        AsyncMediaSampleHarness harness = new AsyncMediaSampleHarness(true);
        Form form = new Form();
        harness.initUI(form);

        final boolean[] resolved = new boolean[1];
        MessageEvent.PromptPromise promptPromise = new MessageEvent.PromptPromise();
        promptPromise.ready(new SuccessCallback<Boolean>() {
            public void onSucess(Boolean arg) {
                resolved[0] = arg != null && arg.booleanValue();
            }
        });

        MessageEvent evt = implementation.fireMessageEvent(promptPromise, "", 426);
        flushSerialCalls();

        assertTrue(evt.isConsumed());
        assertTrue(resolved[0]);
    }

    @FormTest
    void playMessageStartsRecordingAndDisablesPlayButton() {
        AsyncMediaSampleHarness harness = new AsyncMediaSampleHarness(true);
        Form form = new Form();
        harness.initUI(form);

        implementation.fireMessageEvent(this, "play", 0);

        assertEquals(1, harness.getRecordRequests());
        assertTrue(harness.getPauseButton().isEnabled());
        assertTrue(harness.getPauseButton().isVisible());
        assertFalse(harness.getPlayButton().isEnabled());
        assertFalse(harness.getPlayButton().isVisible());
    }

    @FormTest
    void pauseMessageStopsRecordingAndRestoresPlayButton() {
        AsyncMediaSampleHarness harness = new AsyncMediaSampleHarness(true);
        Form form = new Form();
        harness.initUI(form);

        implementation.fireMessageEvent(this, "play", 0);
        implementation.fireMessageEvent(this, "pause", 0);

        assertEquals(1, harness.getPauseRequests());
        assertEquals("Paused", harness.getStatus().getText());
        assertEquals("Press play to begin", harness.getInstructions().getText());
        assertFalse(harness.getPauseButton().isVisible());
        assertFalse(harness.getPauseButton().isEnabled());
        assertTrue(harness.getPlayButton().isVisible());
        assertTrue(harness.getPlayButton().isEnabled());
    }

    private static class AsyncMediaSampleHarness {
        private final Slider slider = new Slider();
        private final Label status = new Label();
        private final SpanLabel instructions = new SpanLabel();
        private final Button pause = new Button("Pause");
        private final Button play = new Button("Play");
        private final boolean promptResponse;
        private int recordRequests;
        private int pauseRequests;

        AsyncMediaSampleHarness(boolean promptResponse) {
            this.promptResponse = promptResponse;
        }

        void initUI(Form form) {
            pause.addActionListener(evt -> {
                pause();
                status.setText("Paused");
                pause.setEnabled(false);
                play.setEnabled(true);
                play.setVisible(true);
                pause.setVisible(false);
                instructions.setText("Press play to begin");

            });

            play.addActionListener(evt -> {
                pause.setEnabled(true);
                play.setEnabled(false);
                play.setVisible(false);
                pause.setVisible(true);
                record();

            });
            CN.addMessageListener((ActionListener<MessageEvent>) evt -> {
                if (evt.isPromptForAudioPlayer()) {
                    evt.consume();
                    CN.callSerially(new Runnable() {
                        public void run() {
                            MessageEvent.PromptPromise res = evt.getPromptPromise();
                            if (res != null) {
                                res.complete(Boolean.valueOf(promptResponse));
                            }
                        }
                    });
                    return;

                }

                if (evt.isPromptForAudioRecorder()) {
                    evt.consume();
                    CN.callSerially(new Runnable() {
                        public void run() {
                            MessageEvent.PromptPromise res = evt.getPromptPromise();
                            if (res != null) {
                                res.complete(Boolean.valueOf(promptResponse));
                            }
                        }
                    });
                    return;

                }

                String message = evt.getMessage();
                if ("play".equals(message)) {
                    pause.setEnabled(true);
                    play.setEnabled(false);
                    play.setVisible(false);
                    pause.setVisible(true);
                    record();
                    return;
                }
                if ("pause".equals(message)) {
                    pause();
                    status.setText("Paused");
                    pause.setEnabled(false);
                    play.setEnabled(true);
                    play.setVisible(true);
                    pause.setVisible(false);
                    instructions.setText("Press play to begin");
                    return;
                }
            });
            form.setLayout(new BorderLayout());
            form.add(BorderLayout.CENTER, LayeredLayout.encloseIn(pause, play));
            form.add(BorderLayout.SOUTH, BorderLayout.south(slider).add(BorderLayout.CENTER, status));
            form.add(BorderLayout.NORTH, instructions);
            instructions.setText("Press play to begin");
            pause.setVisible(false);
            pause.setEnabled(false);
            slider.setEditable(false);
        }

        int getRecordRequests() {
            return recordRequests;
        }

        int getPauseRequests() {
            return pauseRequests;
        }

        Button getPauseButton() {
            return pause;
        }

        Button getPlayButton() {
            return play;
        }

        Label getStatus() {
            return status;
        }

        SpanLabel getInstructions() {
            return instructions;
        }

        private void pause() {
            pauseRequests++;
        }

        private void record() {
            recordRequests++;
        }
    }
}
