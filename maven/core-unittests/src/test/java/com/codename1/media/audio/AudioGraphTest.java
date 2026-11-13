package com.codename1.media.audio;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Assertions;

class AudioGraphTest extends UITestBase {
    private static final float SR = 48000f;
    private static final float EPS = 1e-5f;

    private static OfflineAudioContext ctx(int channels, int frames) {
        return new OfflineAudioContext(channels, frames, SR);
    }

    @FormTest
    void constantSourceReachesDestinationAndUpmixesMonoToStereo() {
        OfflineAudioContext c = ctx(2, 300);
        ConstantSourceNode s = c.createConstantSource();
        s.getOffset().setValue(0.25f);
        s.connect(c.getDestination());
        s.start();
        AudioData out = c.startRendering();
        for (int i = 0; i < 300; i++) {
            Assertions.assertEquals(0.25f, out.getChannelData(0)[i], EPS);
            Assertions.assertEquals(0.25f, out.getChannelData(1)[i], EPS);
        }
    }

    @FormTest
    void startAndStopAreSampleAccurateAndFireEnded() {
        OfflineAudioContext c = ctx(1, 1024);
        ConstantSourceNode s = c.createConstantSource();
        s.connect(c.getDestination());
        final int[] ended = new int[1];
        s.setOnEnded(new Runnable() {
            public void run() {
                ended[0]++;
            }
        });
        s.start(200 / SR);
        s.stop(500 / SR);
        float[] d = c.startRendering().getChannelData(0);
        Assertions.assertEquals(0f, d[199], 0f);
        Assertions.assertEquals(1f, d[200], 0f);
        Assertions.assertEquals(1f, d[499], 0f);
        Assertions.assertEquals(0f, d[500], 0f);
        Assertions.assertEquals(1, ended[0]);
    }

    @FormTest
    void linearAndExponentialRampsFollowTheSpecFormulas() {
        OfflineAudioContext c = ctx(1, 2048);
        ConstantSourceNode s = c.createConstantSource();
        s.getOffset().setValueAtTime(0, 0).linearRampToValueAtTime(1, 1000 / SR)
                .exponentialRampToValueAtTime(4, 2000 / SR);
        s.connect(c.getDestination());
        s.start();
        float[] d = c.startRendering().getChannelData(0);
        Assertions.assertEquals(0.5f, d[500], 1e-4f);
        Assertions.assertEquals(1f, d[1000], 1e-4f);
        Assertions.assertEquals((float) Math.pow(4, 0.5), d[1500], 1e-3f);
        Assertions.assertEquals(4f, d[2040], 1e-4f);
    }

    @FormTest
    void setTargetApproachesExponentiallyAndCurveInterpolates() {
        OfflineAudioContext c = ctx(1, 4096);
        ConstantSourceNode s = c.createConstantSource();
        s.getOffset().setValueAtTime(1, 0).setTargetAtTime(0, 0, 1000 / SR)
                .setValueCurveAtTime(new float[] {2, 4, 0}, 2048 / SR, 1000 / SR);
        s.connect(c.getDestination());
        s.start();
        float[] d = c.startRendering().getChannelData(0);
        Assertions.assertEquals((float) Math.exp(-1), d[1000], 1e-4f);
        Assertions.assertEquals(3f, d[2048 + 250], 1e-3f);
        Assertions.assertEquals(2f, d[2048 + 750], 1e-3f);
        Assertions.assertEquals(0f, d[4000], 1e-6f);
    }

    @FormTest
    void cancelAndHoldFreezesARampMidway() {
        OfflineAudioContext c = ctx(1, 2048);
        ConstantSourceNode s = c.createConstantSource();
        AudioParam p = s.getOffset();
        p.setValueAtTime(0, 0).linearRampToValueAtTime(1, 2000 / SR);
        p.cancelAndHoldAtTime(1000 / SR);
        s.connect(c.getDestination());
        s.start();
        float[] d = c.startRendering().getChannelData(0);
        Assertions.assertEquals(0.25f, d[500], 1e-4f);
        Assertions.assertEquals(0.5f, d[1500], 1e-4f);
    }

    @FormTest
    void gainIsModulatedByAConnectedNode() {
        OfflineAudioContext c = ctx(1, 256);
        ConstantSourceNode one = c.createConstantSource();
        ConstantSourceNode mod = c.createConstantSource();
        mod.getOffset().setValue(0.5f);
        GainNode g = c.createGain();
        g.getGain().setValue(0.25f);
        one.connect(g).connect(c.getDestination());
        mod.connect(g.getGain());
        one.start();
        mod.start();
        float[] d = c.startRendering().getChannelData(0);
        Assertions.assertEquals(0.75f, d[100], EPS);
    }

    @FormTest
    void bufferSourcePlaysOffsetsLoopsAndDoubleRate() {
        OfflineAudioContext c = ctx(1, 64);
        AudioData data = c.createBuffer(1, 8, SR);
        for (int i = 0; i < 8; i++) {
            data.getChannelData(0)[i] = i;
        }
        AudioBufferSourceNode a = c.createBufferSource();
        a.setBuffer(data);
        a.setLoop(true);
        a.connect(c.getDestination());
        a.start(0, 2 / SR);
        float[] d = c.startRendering().getChannelData(0);
        Assertions.assertEquals(2f, d[0], 0f);
        Assertions.assertEquals(7f, d[5], 0f);
        Assertions.assertEquals(0f, d[6], 0f);
        Assertions.assertEquals(1f, d[7], 0f);

        OfflineAudioContext c2 = ctx(1, 16);
        AudioBufferSourceNode b = c2.createBufferSource();
        b.setBuffer(data);
        b.getPlaybackRate().setValue(2);
        b.connect(c2.getDestination());
        b.start();
        float[] e = c2.startRendering().getChannelData(0);
        Assertions.assertEquals(0f, e[0], 0f);
        Assertions.assertEquals(4f, e[2], 0f);
        Assertions.assertEquals(6f, e[3], 0f);
        Assertions.assertEquals(0f, e[4], 0f);
    }

    @FormTest
    void sineOscillatorMatchesMathSin() {
        OfflineAudioContext c = ctx(1, 1000);
        OscillatorNode o = c.createOscillator();
        o.getFrequency().setValue(1000);
        o.connect(c.getDestination());
        o.start();
        float[] d = c.startRendering().getChannelData(0);
        for (int i = 0; i < 1000; i++) {
            Assertions.assertEquals(Math.sin(2 * Math.PI * 1000 * i / SR), d[i], 1e-4);
        }
    }

    @FormTest
    void squareWaveIsNormalizedAndBandLimited() {
        OfflineAudioContext c = ctx(1, 4800);
        OscillatorNode o = c.createOscillator();
        o.setType(OscillatorType.SQUARE);
        o.getFrequency().setValue(100);
        o.connect(c.getDestination());
        o.start();
        float[] d = c.startRendering().getChannelData(0);
        float max = 0;
        for (int i = 0; i < d.length; i++) {
            max = Math.max(max, Math.abs(d[i]));
        }
        Assertions.assertEquals(1f, max, 0.02f);
        Assertions.assertTrue(d[120] > 0.8f, "first half of the period is high");
        Assertions.assertTrue(d[360] < -0.8f, "second half is low");
    }

    @FormTest
    void delayShiftsByWholeFramesAndFeedbackCycleDecays() {
        OfflineAudioContext c = ctx(1, 1024);
        AudioData imp = c.createBuffer(1, 1, SR);
        imp.getChannelData(0)[0] = 1;
        AudioBufferSourceNode s = c.createBufferSource();
        s.setBuffer(imp);
        DelayNode delay = c.createDelay(0.1);
        delay.getDelayTime().setValue(300 / SR);
        GainNode fb = c.createGain();
        fb.getGain().setValue(0.5f);
        s.connect(delay).connect(fb).connect(delay);
        delay.connect(c.getDestination());
        s.start();
        float[] d = c.startRendering().getChannelData(0);
        Assertions.assertEquals(1f, d[300], EPS);
        Assertions.assertEquals(0.5f, d[600], EPS);
        Assertions.assertEquals(0.25f, d[900], EPS);
        Assertions.assertEquals(0f, d[301], EPS);
    }

    @FormTest
    void cycleWithoutDelayIsSilentNotAStackOverflow() {
        OfflineAudioContext c = ctx(1, 256);
        ConstantSourceNode s = c.createConstantSource();
        GainNode a = c.createGain();
        GainNode b = c.createGain();
        s.connect(a).connect(b).connect(a);
        b.connect(c.getDestination());
        s.start();
        c.startRendering();
    }

    @FormTest
    void biquadMatchesSpecCoefficientsAndResponse() {
        OfflineAudioContext c = ctx(1, 256);
        AudioData imp = c.createBuffer(1, 1, SR);
        imp.getChannelData(0)[0] = 1;
        AudioBufferSourceNode s = c.createBufferSource();
        s.setBuffer(imp);
        BiquadFilterNode f = c.createBiquadFilter();
        f.getFrequency().setValue(1000);
        f.getQ().setValue(3);
        s.connect(f).connect(c.getDestination());
        s.start();
        float[] h = c.startRendering().getChannelData(0);

        double w0 = 2 * Math.PI * 1000 / SR;
        double alpha = Math.sin(w0) / (2 * Math.pow(10, 3 / 20.0));
        double cos = Math.cos(w0);
        double a0 = 1 + alpha;
        double b0 = (1 - cos) / 2 / a0;
        double b1 = (1 - cos) / a0;
        double a1 = -2 * cos / a0;
        Assertions.assertEquals(b0, h[0], 1e-6);
        Assertions.assertEquals(b1 - a1 * b0, h[1], 1e-6);

        float[] mag = new float[2];
        float[] ph = new float[2];
        f.getFrequencyResponse(new float[] {0, 1000}, mag, ph);
        Assertions.assertEquals(1f, mag[0], 1e-4f);
        Assertions.assertEquals((float) Math.pow(10, 3 / 20.0), mag[1], 2e-3f);
    }

    @FormTest
    void iirFilterWithBiquadCoefficientsMatchesTheBiquad() {
        double[] c5 = new double[5];
        BiquadFilterNode.coefficients(BiquadFilterType.PEAKING, 2000, 0, 2, 6, SR, c5);
        OfflineAudioContext a = ctx(1, 512);
        OfflineAudioContext b = ctx(1, 512);
        AudioNode fa = a.createIIRFilter(new double[] {c5[0], c5[1], c5[2]}, new double[] {1, c5[3], c5[4]});
        BiquadFilterNode fb = b.createBiquadFilter();
        fb.setType(BiquadFilterType.PEAKING);
        fb.getFrequency().setValue(2000);
        fb.getQ().setValue(2);
        fb.getGain().setValue(6);
        float[] ra = renderNoiseThrough(a, fa);
        float[] rb = renderNoiseThrough(b, fb);
        for (int i = 0; i < ra.length; i++) {
            Assertions.assertEquals(rb[i], ra[i], 1e-5f);
        }
    }

    private static float[] renderNoiseThrough(OfflineAudioContext c, AudioNode filter) {
        AudioData noise = c.createBuffer(1, 512, SR);
        long seed = 1;
        for (int i = 0; i < 512; i++) {
            seed = seed * 6364136223846793005L + 1442695040888963407L;
            noise.getChannelData(0)[i] = ((seed >>> 40) / (float) (1L << 24)) * 2 - 1;
        }
        AudioBufferSourceNode s = c.createBufferSource();
        s.setBuffer(noise);
        s.connect(filter).connect(c.getDestination());
        s.start();
        return c.startRendering().getChannelData(0);
    }

    @FormTest
    void stereoPannerUsesEqualPowerGains() {
        OfflineAudioContext c = ctx(2, 128);
        ConstantSourceNode s = c.createConstantSource();
        StereoPannerNode p = c.createStereoPanner();
        p.getPan().setValue(0.5f);
        s.connect(p).connect(c.getDestination());
        s.start();
        AudioData out = c.startRendering();
        double x = (0.5 + 1) / 2;
        Assertions.assertEquals(Math.cos(x * Math.PI / 2), out.getChannelData(0)[10], 1e-5);
        Assertions.assertEquals(Math.sin(x * Math.PI / 2), out.getChannelData(1)[10], 1e-5);
    }

    @FormTest
    void splitterAndMergerSwapChannelsAndStereoDownmixesToMono() {
        OfflineAudioContext c = ctx(2, 128);
        ConstantSourceNode l = c.createConstantSource();
        ConstantSourceNode r = c.createConstantSource();
        l.getOffset().setValue(0.2f);
        r.getOffset().setValue(0.6f);
        ChannelMergerNode m = c.createChannelMerger(2);
        l.connect(m, 0, 0);
        r.connect(m, 0, 1);
        ChannelSplitterNode sp = c.createChannelSplitter(2);
        ChannelMergerNode swap = c.createChannelMerger(2);
        m.connect(sp);
        sp.connect(swap, 0, 1);
        sp.connect(swap, 1, 0);
        swap.connect(c.getDestination());
        l.start();
        r.start();
        AudioData out = c.startRendering();
        Assertions.assertEquals(0.6f, out.getChannelData(0)[5], EPS);
        Assertions.assertEquals(0.2f, out.getChannelData(1)[5], EPS);

        OfflineAudioContext c2 = ctx(1, 128);
        ConstantSourceNode l2 = c2.createConstantSource();
        ConstantSourceNode r2 = c2.createConstantSource();
        l2.getOffset().setValue(0.2f);
        r2.getOffset().setValue(0.6f);
        ChannelMergerNode m2 = c2.createChannelMerger(2);
        l2.connect(m2, 0, 0);
        r2.connect(m2, 0, 1);
        m2.connect(c2.getDestination());
        l2.start();
        r2.start();
        Assertions.assertEquals(0.4f, c2.startRendering().getChannelData(0)[5], EPS);
    }

    @FormTest
    void unconnectedSourceStillAdvancesInTime() {
        OfflineAudioContext c = ctx(1, 1024);
        AudioData ramp = c.createBuffer(1, 1024, SR);
        for (int i = 0; i < 1024; i++) {
            ramp.getChannelData(0)[i] = i;
        }
        final AudioBufferSourceNode s = c.createBufferSource();
        s.setBuffer(ramp);
        s.start();
        final ConstantSourceNode trigger = c.createConstantSource();
        final OfflineAudioContext cc = c;
        trigger.setOnEnded(new Runnable() {
            public void run() {
                s.connect(cc.getDestination());
            }
        });
        trigger.start();
        trigger.stop(256 / SR);
        float[] d = c.startRendering().getChannelData(0);
        Assertions.assertEquals(0f, d[100], 0f);
        Assertions.assertEquals(400f, d[400], 0f);
    }

    @FormTest
    void validationAndCapabilityAnswersNeverThrowOnGaps() {
        OfflineAudioContext c = ctx(1, 128);
        Assertions.assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                new OfflineAudioContext(1, 128, 100);
            }
        });
        final AudioParam p = c.createGain().getGain();
        Assertions.assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                p.exponentialRampToValueAtTime(0, 1);
            }
        });
        final ConstantSourceNode s = c.createConstantSource();
        s.start();
        Assertions.assertThrows(IllegalStateException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                s.start();
            }
        });
        if (!AudioContext.isSupported()) {
            Assertions.assertNull(AudioContext.create());
        }
    }

    /// The render loop must not allocate: a per-quantum allocation on a real-time
    /// thread is a GC pause waiting to become a glitch. The same busy graph is
    /// rendered for 1 s and for 10 s; beyond the bigger result buffer, the longer
    /// render may not allocate more than a few kilobytes.
    @FormTest
    void renderLoopDoesNotAllocatePerQuantum() {
        java.lang.management.ThreadMXBean mx = java.lang.management.ManagementFactory.getThreadMXBean();
        if (!(mx instanceof com.sun.management.ThreadMXBean)) {
            return;
        }
        com.sun.management.ThreadMXBean bean = (com.sun.management.ThreadMXBean) mx;
        if (!bean.isThreadAllocatedMemorySupported()) {
            return;
        }
        busyGraph(48000).startRendering();
        long tid = Thread.currentThread().getId();
        OfflineAudioContext shortCtx = busyGraph(48000);
        long a0 = bean.getThreadAllocatedBytes(tid);
        shortCtx.startRendering();
        long shortBytes = bean.getThreadAllocatedBytes(tid) - a0;
        OfflineAudioContext longCtx = busyGraph(480000);
        long b0 = bean.getThreadAllocatedBytes(tid);
        longCtx.startRendering();
        long longBytes = bean.getThreadAllocatedBytes(tid) - b0;
        long resultGrowth = (480000L - 48000L) * 2 * 4;
        long extra = longBytes - shortBytes - resultGrowth;
        Assertions.assertTrue(extra < 32 * 1024,
                "render loop allocated " + extra + " bytes over 3375 extra quanta");
    }

    private static OfflineAudioContext busyGraph(int frames) {
        OfflineAudioContext c = ctx(2, frames);
        OscillatorNode osc = c.createOscillator();
        osc.setType(OscillatorType.SAWTOOTH);
        osc.getFrequency().setValueAtTime(110, 0).exponentialRampToValueAtTime(880, frames / SR);
        BiquadFilterNode f = c.createBiquadFilter();
        f.getFrequency().setValueAtTime(200, 0).linearRampToValueAtTime(4000, frames / SR);
        GainNode g = c.createGain();
        g.getGain().setTargetAtTime(0.3f, 0, 0.5);
        DelayNode d = c.createDelay(0.5);
        d.getDelayTime().setValue(0.25f);
        GainNode fb = c.createGain();
        fb.getGain().setValue(0.4f);
        StereoPannerNode p = c.createStereoPanner();
        OscillatorNode lfo = c.createOscillator();
        lfo.getFrequency().setValue(0.5f);
        lfo.connect(p.getPan());
        AudioData loop = c.createBuffer(1, 1000, 44100);
        AudioBufferSourceNode src = c.createBufferSource();
        src.setBuffer(loop);
        src.setLoop(true);
        src.getPlaybackRate().setValue(1.3f);
        osc.connect(f).connect(g).connect(d).connect(fb).connect(d);
        d.connect(p).connect(c.getDestination());
        g.connect(c.getDestination());
        src.connect(c.getDestination());
        osc.start();
        lfo.start();
        src.start();
        return c;
    }

    private static AudioData pan(float x, float y, float z, PannerNode[] hold) {
        OfflineAudioContext c = ctx(2, 256);
        ConstantSourceNode s = c.createConstantSource();
        PannerNode p = c.createPanner();
        p.setPosition(x, y, z);
        if (hold != null) {
            hold[0] = p;
        }
        s.connect(p).connect(c.getDestination());
        s.start();
        return c.startRendering();
    }

    @FormTest
    void pannerPlacesSourcesByAzimuthAndInverseDistance() {
        AudioData right = pan(1, 0, 0, null);
        Assertions.assertEquals(0, right.getChannelData(0)[50], 1e-5);
        Assertions.assertEquals(1, right.getChannelData(1)[50], 1e-5);
        AudioData left = pan(-1, 0, 0, null);
        Assertions.assertEquals(1, left.getChannelData(0)[50], 1e-5);
        Assertions.assertEquals(0, left.getChannelData(1)[50], 1e-5);
        // straight ahead at distance 2: equal power and 1 / (1 + (2 - 1)) = 0.5
        AudioData front = pan(0, 0, -2, null);
        Assertions.assertEquals(Math.sqrt(0.5) * 0.5, front.getChannelData(0)[50], 1e-5);
        Assertions.assertEquals(Math.sqrt(0.5) * 0.5, front.getChannelData(1)[50], 1e-5);
        // behind mirrors to the front
        AudioData back = pan(0, 0, 2, null);
        Assertions.assertEquals(front.getChannelData(0)[50], back.getChannelData(0)[50], 1e-5);
    }

    @FormTest
    void distanceModelsAndConeFollowTheSpecFormulas() {
        Assertions.assertEquals(1 - (5.0 - 1) / (10 - 1),
                PannerNode.distanceGain(DistanceModelType.LINEAR, 5, 1, 10, 1), 1e-9);
        Assertions.assertEquals(0, PannerNode.distanceGain(DistanceModelType.LINEAR, 50, 1, 10, 1), 1e-9);
        Assertions.assertEquals(Math.pow(4, -2),
                PannerNode.distanceGain(DistanceModelType.EXPONENTIAL, 4, 1, 10, 2), 1e-9);
        Assertions.assertEquals(1, PannerNode.distanceGain(DistanceModelType.INVERSE, 0.5, 1, 10, 1), 1e-9);
        // facing +x, listener behind (at -x): outside a 90 degree outer cone
        Assertions.assertEquals(0.25, PannerNode.coneGain(-1, 0, 0, 1, 0, 0, 60, 90, 0.25), 1e-9);
        Assertions.assertEquals(1, PannerNode.coneGain(1, 0, 0, 1, 0, 0, 60, 90, 0.25), 1e-9);
        // 37.5 degrees off axis, halfway between 30 and 45
        double a = Math.toRadians(37.5);
        Assertions.assertEquals(0.625, PannerNode.coneGain(Math.cos(a), Math.sin(a), 0, 1, 0, 0, 60, 90, 0.25), 1e-9);

        OfflineAudioContext c = ctx(2, 128);
        ConstantSourceNode s = c.createConstantSource();
        PannerNode p = c.createPanner();
        p.setPosition(0, 0, -1);
        p.setOrientation(0, 0, -1);
        p.setConeInnerAngle(10);
        p.setConeOuterAngle(20);
        p.setConeOuterGain(0.5);
        s.connect(p).connect(c.getDestination());
        s.start();
        Assertions.assertEquals(Math.sqrt(0.5) * 0.5, c.startRendering().getChannelData(0)[20], 1e-5);
    }

    @FormTest
    void rotatedListenerHearsAheadAsRight() {
        OfflineAudioContext c = ctx(2, 128);
        // facing -x, a source at -z is on the listener's right
        c.getListener().setOrientation(-1, 0, 0, 0, 1, 0);
        ConstantSourceNode s = c.createConstantSource();
        PannerNode p = c.createPanner();
        p.setPosition(0, 0, -1);
        s.connect(p).connect(c.getDestination());
        s.start();
        AudioData out = c.startRendering();
        Assertions.assertEquals(0, out.getChannelData(0)[10], 1e-5);
        Assertions.assertEquals(1, out.getChannelData(1)[10], 1e-5);
    }

    @FormTest
    void hrtfDelaysAndShadowsTheFarEar() {
        OfflineAudioContext c = ctx(2, 1024);
        AudioData imp = c.createBuffer(1, 1, SR);
        imp.getChannelData(0)[0] = 1;
        AudioBufferSourceNode s = c.createBufferSource();
        s.setBuffer(imp);
        PannerNode p = c.createPanner();
        p.setPanningModel(PanningModel.HRTF);
        p.setPosition(1, 0, 0);
        s.connect(p).connect(c.getDestination());
        s.start();
        AudioData out = c.startRendering();
        float[] l = out.getChannelData(0);
        float[] r = out.getChannelData(1);
        int pl = 0;
        int pr = 0;
        double el = 0;
        double er = 0;
        for (int i = 0; i < l.length; i++) {
            if (Math.abs(l[i]) > Math.abs(l[pl])) {
                pl = i;
            }
            if (Math.abs(r[i]) > Math.abs(r[pr])) {
                pr = i;
            }
            el += l[i] * l[i];
            er += r[i] * r[i];
        }
        Assertions.assertTrue(er > 2 * el, "near ear louder: " + er + " vs " + el);
        // Woodworth: a / c * (theta + sin theta) at 90 degrees = 0.656 ms
        double itd = 0.0875 / 343 * (Math.PI / 2 + 1) * SR;
        Assertions.assertEquals(itd, pl - pr, 3, "left " + pl + " right " + pr);
    }

    @FormTest
    void pannerAndHrtfRenderWithoutAllocating() {
        bytesFor(1);
        // the context allocates its result up front, 2 channels of floats per frame
        long extra = bytesFor(10) - bytesFor(1) - 2L * 4 * (long) SR * 9;
        Assertions.assertTrue(extra < 4096, "render loop allocated " + extra + " bytes");
    }

    private static long bytesFor(int seconds) {
        OfflineAudioContext c = new OfflineAudioContext(2, (int) SR * seconds, SR);
        OscillatorNode o = c.createOscillator();
        PannerNode p = c.createPanner();
        p.setPanningModel(PanningModel.HRTF);
        p.getPositionX().linearRampToValueAtTime(-5, seconds);
        PannerNode q = c.createPanner();
        q.getPositionZ().linearRampToValueAtTime(5, seconds);
        o.connect(p).connect(c.getDestination());
        o.connect(q).connect(c.getDestination());
        o.start();
        com.sun.management.ThreadMXBean b =
                (com.sun.management.ThreadMXBean) java.lang.management.ManagementFactory.getThreadMXBean();
        long t = Thread.currentThread().getId();
        long before = b.getThreadAllocatedBytes(t);
        c.startRendering();
        return b.getThreadAllocatedBytes(t) - before;
    }
}
