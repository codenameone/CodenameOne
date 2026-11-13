package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.media.audio.AnalyserNode;
import com.codename1.media.audio.AudioContext;
import com.codename1.media.audio.AudioData;
import com.codename1.media.audio.AudioProcessor;
import com.codename1.media.audio.AudioProcessorNode;
import com.codename1.media.audio.BiquadFilterNode;
import com.codename1.media.audio.BiquadFilterType;
import com.codename1.media.audio.ConstantSourceNode;
import com.codename1.media.audio.ConvolverNode;
import com.codename1.media.audio.GainNode;
import com.codename1.media.audio.OfflineAudioContext;
import com.codename1.media.audio.OscillatorNode;
import com.codename1.media.audio.PannerNode;
import com.codename1.media.audio.PanningModel;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.util.UITimer;

/**
 * On-device coverage for the audio graph. The offline renders are the same
 * portable code core-unittests runs on JavaSE, so running them here checks
 * the iOS, Android, native desktop and JavaScript translations produce the
 * same numbers. Where the platform has a real-time output the test also plays
 * a quiet tone through an analyser drawn as a spectrum, and fails if the
 * context's clock never moves, which is what a dead output sink looks like.
 */
public class AudioGraphApiTest extends BaseTest {
    private static final float SR = 48000;

    @Override
    public boolean runTest() {
        try {
            runOffline();
        } catch (Throwable t) {
            fail("Audio graph offline test failed: " + t);
            return false;
        }
        // A browser starts audio only after a user gesture, which an
        // automated run never makes; its clock would stand still.
        AudioContext ctx = "HTML5".equals(Display.getInstance().getPlatformName()) ? null : AudioContext.create();
        if (ctx == null) {
            done();
            return true;
        }
        runRealTime(ctx);
        return true;
    }

    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }

    private void runOffline() {
        // gain and constant source
        OfflineAudioContext c = new OfflineAudioContext(1, 256, SR);
        ConstantSourceNode k = c.createConstantSource();
        GainNode g = c.createGain();
        g.getGain().setValue(0.5f);
        k.connect(g).connect(c.getDestination());
        k.start();
        near(0.5, c.startRendering().getChannelData(0)[200], 1e-6, "gain");

        // a 500 Hz low-pass takes most of a 10 kHz sine away
        c = new OfflineAudioContext(1, 4800, SR);
        OscillatorNode osc = c.createOscillator();
        osc.getFrequency().setValue(10000);
        BiquadFilterNode lp = c.createBiquadFilter();
        lp.setType(BiquadFilterType.LOWPASS);
        lp.getFrequency().setValue(500);
        osc.connect(lp).connect(c.getDestination());
        osc.start();
        checkTrue(rms(c.startRendering().getChannelData(0), 2400) < 0.01, "low-pass attenuation");

        // HRTF: a source to the right is louder in the right ear. At 4 kHz,
        // where the head shadows the far ear; a low tone bends round it.
        c = new OfflineAudioContext(2, 4800, SR);
        osc = c.createOscillator();
        osc.getFrequency().setValue(4000);
        PannerNode p = c.createPanner();
        p.setPanningModel(PanningModel.HRTF);
        p.getPositionX().setValue(2);
        osc.connect(p).connect(c.getDestination());
        osc.start();
        AudioData stereo = c.startRendering();
        checkTrue(rms(stereo.getChannelData(1), 2400) > 2 * rms(stereo.getChannelData(0), 2400), "HRTF right");

        // a unit impulse response leaves the signal as it is
        c = new OfflineAudioContext(1, 1024, SR);
        osc = c.createOscillator();
        ConvolverNode conv = c.createConvolver();
        conv.setNormalize(false);
        AudioData ir = c.createBuffer(1, 64, SR);
        ir.getChannelData(0)[0] = 1;
        conv.setBuffer(ir);
        osc.connect(conv).connect(c.getDestination());
        osc.start();
        float[] wet = c.startRendering().getChannelData(0);
        near(Math.sin(2 * Math.PI * 440 * 700 / SR), wet[700], 1e-3, "convolver identity");

        // custom processor
        c = new OfflineAudioContext(1, 256, SR);
        AudioProcessorNode neg = c.createAudioProcessor(new AudioProcessor() {
            @Override
            public boolean process(float[][][] in, float[][][] out, float[][] params) {
                for (int i = 0; i < 128; i++) {
                    out[0][0][i] = -in[0][0][i];
                }
                return true;
            }
        }, 1);
        k = c.createConstantSource();
        k.connect(neg).connect(c.getDestination());
        k.start();
        near(-1, c.startRendering().getChannelData(0)[100], 1e-6, "processor");

        // WAV decoding: one 16-bit mono sample of 0.5
        byte[] wav = new byte[46];
        put(wav, 0, "RIFF");
        le(wav, 4, 38, 4);
        put(wav, 8, "WAVE");
        put(wav, 12, "fmt ");
        le(wav, 16, 16, 4);
        le(wav, 20, 1, 2);
        le(wav, 22, 1, 2);
        le(wav, 24, 8000, 4);
        le(wav, 28, 16000, 4);
        le(wav, 32, 2, 2);
        le(wav, 34, 16, 2);
        put(wav, 36, "data");
        le(wav, 40, 2, 4);
        le(wav, 44, 0x4000, 2);
        AudioData decoded = c.decodeAudioData(wav);
        checkTrue(decoded != null, "WAV decoded");
        near(0.5, decoded.getChannelData(0)[0], 1e-6, "WAV sample");
    }

    private void runRealTime(final AudioContext ctx) {
        final OscillatorNode osc = ctx.createOscillator();
        osc.getFrequency().setValue(880);
        GainNode quiet = ctx.createGain();
        quiet.getGain().setValue(0.02f);
        final AnalyserNode analyser = ctx.createAnalyser();
        analyser.setFftSize(256);
        osc.connect(quiet).connect(analyser).connect(ctx.getDestination());
        osc.start();
        Form f = new Form("Audio graph", new BorderLayout());
        f.add(BorderLayout.CENTER, new Spectrum(analyser));
        f.show();
        final long started = System.currentTimeMillis();
        UITimer.timer(100, true, f, new Runnable() {
            private boolean finished;

            @Override
            public void run() {
                if (finished) {
                    return;
                }
                boolean advanced = ctx.getCurrentTime() >= 0.3;
                if (!advanced && System.currentTimeMillis() - started < 5000) {
                    return;
                }
                finished = true;
                osc.stop();
                ctx.close();
                if (!advanced) {
                    fail("real-time audio context never advanced past " + ctx.getCurrentTime() + "s");
                }
                done();
            }
        });
    }

    /// The analyser's spectrum as bars, repainted every frame.
    static final class Spectrum extends Component {
        private final AnalyserNode analyser;
        private final byte[] bins;

        Spectrum(AnalyserNode analyser) {
            this.analyser = analyser;
            bins = new byte[analyser.getFrequencyBinCount()];
        }

        @Override
        protected void initComponent() {
            super.initComponent();
            getComponentForm().registerAnimated(this);
        }

        @Override
        protected void deinitialize() {
            getComponentForm().deregisterAnimated(this);
            super.deinitialize();
        }

        @Override
        public boolean animate() {
            return true;
        }

        @Override
        public void paint(Graphics g) {
            analyser.getByteFrequencyData(bins);
            int w = Math.max(1, getWidth() / bins.length);
            g.setColor(0x2a7ae2);
            for (int i = 0; i < bins.length; i++) {
                int h = (bins[i] & 0xff) * getHeight() / 255;
                g.fillRect(getX() + i * w, getY() + getHeight() - h, Math.max(1, w - 1), h);
            }
        }

        @Override
        protected Dimension calcPreferredSize() {
            return new Dimension(Display.getInstance().getDisplayWidth(), Display.getInstance().getDisplayHeight() / 3);
        }
    }

    private static void put(byte[] b, int at, String tag) {
        for (int i = 0; i < 4; i++) {
            b[at + i] = (byte) tag.charAt(i);
        }
    }

    private static void le(byte[] b, int at, int v, int n) {
        for (int i = 0; i < n; i++) {
            b[at + i] = (byte) (v >> (8 * i));
        }
    }

    private static double rms(float[] d, int from) {
        double sum = 0;
        for (int i = from; i < d.length; i++) {
            sum += d[i] * d[i];
        }
        return Math.sqrt(sum / (d.length - from));
    }

    private void near(double expected, double actual, double tolerance, String label) {
        if (!(Math.abs(expected - actual) <= tolerance)) {
            failNow(label + ": expected " + expected + " got " + actual);
        }
    }

    private void checkTrue(boolean value, String label) {
        if (!value) {
            failNow(label);
        }
    }

    private void failNow(String message) {
        fail(message);
        throw new IllegalStateException(message);
    }
}
