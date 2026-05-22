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
package com.codename1.impl.android;

import android.app.Activity;
import android.content.Context;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.nfc.tech.NfcF;
import android.nfc.tech.NfcV;
import android.os.Bundle;

import com.codename1.nfc.HostCardEmulationService;
import com.codename1.nfc.NdefMessage;
import com.codename1.nfc.NfcError;
import com.codename1.nfc.NfcException;
import com.codename1.nfc.NfcListener;
import com.codename1.nfc.NfcReadOptions;
import com.codename1.nfc.TagType;
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Android implementation of {@link com.codename1.nfc.Nfc} that bridges to
 * the platform NfcAdapter via reader-mode (API 19+).
 *
 * The host card emulation side is wired through CodenameOneHostApduService
 * which the Codename One Maven plugin / build daemon register in the
 * AndroidManifest when an app references com.codename1.nfc.* .
 */
class AndroidNfc extends com.codename1.nfc.Nfc {

    private final AndroidImplementation impl;
    private AsyncResource<com.codename1.nfc.Tag> pendingRead;
    private NfcReadOptions pendingOptions;
    private boolean readerArmed;
    private final Set<NfcListener> listeners = new HashSet<NfcListener>();
    private HostCardEmulationService hceService;

    AndroidNfc(AndroidImplementation impl) {
        this.impl = impl;
    }

    private NfcAdapter adapter() {
        Activity a = AndroidImplementation.getActivity();
        if (a == null) {
            return null;
        }
        return NfcAdapter.getDefaultAdapter(a);
    }

    @Override
    public boolean isSupported() {
        return adapter() != null;
    }

    @Override
    public boolean canRead() {
        NfcAdapter a = adapter();
        return a != null && a.isEnabled();
    }

    @Override
    public boolean canWrite() {
        return canRead();
    }

    @Override
    public boolean canHostEmulate() {
        Context ctx = AndroidImplementation.getActivity();
        if (ctx == null) {
            return false;
        }
        return ctx.getPackageManager()
                .hasSystemFeature("android.hardware.nfc.hce");
    }

    @Override
    public synchronized AsyncResource<com.codename1.nfc.Tag> readTag(NfcReadOptions options) {
        AsyncResource<com.codename1.nfc.Tag> r = new AsyncResource<com.codename1.nfc.Tag>();
        NfcAdapter a = adapter();
        if (a == null) {
            r.error(new NfcException(NfcError.NOT_AVAILABLE,
                    "NfcAdapter is unavailable on this device"));
            return r;
        }
        if (!a.isEnabled()) {
            r.error(new NfcException(NfcError.DISABLED,
                    "NFC is disabled in system settings"));
            return r;
        }
        if (pendingRead != null) {
            r.error(new NfcException(NfcError.SYSTEM_CANCELED,
                    "another NFC read is already in progress"));
            return r;
        }
        pendingRead = r;
        pendingOptions = options != null ? options : new NfcReadOptions();
        armReader();
        long timeout = pendingOptions.getTimeoutMs();
        if (timeout > 0) {
            scheduleTimeout(timeout);
        }
        return r;
    }

    @Override
    public synchronized boolean stopRead() {
        boolean had = pendingRead != null;
        disarmReader();
        if (had) {
            AsyncResource<com.codename1.nfc.Tag> r = pendingRead;
            pendingRead = null;
            pendingOptions = null;
            r.error(new NfcException(NfcError.USER_CANCELED,
                    "NFC read cancelled"));
        }
        return had;
    }

    @Override
    public synchronized void addTagListener(NfcListener listener) {
        if (listener == null) {
            return;
        }
        listeners.add(listener);
        if (!readerArmed) {
            pendingOptions = new NfcReadOptions();
            armReader();
        }
    }

    @Override
    public synchronized void removeTagListener(NfcListener listener) {
        listeners.remove(listener);
        if (listeners.isEmpty() && pendingRead == null) {
            disarmReader();
        }
    }

    @Override
    public synchronized void registerHostCardEmulationService(
            HostCardEmulationService service) {
        this.hceService = service;
        CodenameOneHostApduService.bind(service);
    }

    @Override
    public synchronized void unregisterHostCardEmulationService() {
        this.hceService = null;
        CodenameOneHostApduService.bind(null);
    }

    private void armReader() {
        final Activity activity = AndroidImplementation.getActivity();
        final NfcAdapter a = adapter();
        if (activity == null || a == null) {
            return;
        }
        final int flags = computeReaderFlags(pendingOptions);
        final Bundle extras = new Bundle();
        // 250 ms presence-check delay is the platform default.
        extras.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250);
        // Ports/Android compiles with -source 1.6, so locals captured by
        // anonymous inner classes have to be explicitly declared final
        // (Java 8's effectively-final semantics don't apply here).
        activity.runOnUiThread(new Runnable() {
            public void run() {
                a.enableReaderMode(activity, new NfcAdapter.ReaderCallback() {
                    public void onTagDiscovered(Tag tag) {
                        deliverTag(tag);
                    }
                }, flags, extras);
                readerArmed = true;
            }
        });
    }

    private void disarmReader() {
        final Activity activity = AndroidImplementation.getActivity();
        final NfcAdapter a = adapter();
        if (activity == null || a == null || !readerArmed) {
            readerArmed = false;
            return;
        }
        activity.runOnUiThread(new Runnable() {
            public void run() {
                try {
                    a.disableReaderMode(activity);
                } catch (Throwable ignore) {
                }
                readerArmed = false;
            }
        });
    }

    private void scheduleTimeout(final long ms) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(ms);
                } catch (InterruptedException ignore) {
                }
                AsyncResource<com.codename1.nfc.Tag> r;
                synchronized (AndroidNfc.this) {
                    if (pendingRead == null) {
                        return;
                    }
                    r = pendingRead;
                    pendingRead = null;
                    pendingOptions = null;
                    disarmReader();
                }
                r.error(new NfcException(NfcError.SYSTEM_CANCELED,
                        "NFC read timed out"));
            }
        }, "AndroidNfc-timeout").start();
    }

    private int computeReaderFlags(NfcReadOptions opts) {
        if (opts == null) {
            return NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_NFC_B
                    | NfcAdapter.FLAG_READER_NFC_F | NfcAdapter.FLAG_READER_NFC_V;
        }
        List<TagType> filter = opts.getTechFilter();
        if (filter.isEmpty()) {
            return NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_NFC_B
                    | NfcAdapter.FLAG_READER_NFC_F | NfcAdapter.FLAG_READER_NFC_V;
        }
        int flags = 0;
        for (TagType t : filter) {
            switch (t) {
                case NFC_A:
                case ISO_DEP:
                case MIFARE_CLASSIC:
                case MIFARE_ULTRALIGHT:
                    flags |= NfcAdapter.FLAG_READER_NFC_A;
                    break;
                case NFC_B:
                    flags |= NfcAdapter.FLAG_READER_NFC_B;
                    break;
                case NFC_F:
                    flags |= NfcAdapter.FLAG_READER_NFC_F;
                    break;
                case NFC_V:
                    flags |= NfcAdapter.FLAG_READER_NFC_V;
                    break;
                case NDEF:
                    flags |= NfcAdapter.FLAG_READER_NFC_A
                            | NfcAdapter.FLAG_READER_NFC_B
                            | NfcAdapter.FLAG_READER_NFC_F
                            | NfcAdapter.FLAG_READER_NFC_V;
                    break;
                default:
                    break;
            }
        }
        return flags;
    }

    private void deliverTag(Tag rawTag) {
        Set<TagType> types = new HashSet<TagType>();
        for (String t : rawTag.getTechList()) {
            TagType tt = mapTech(t);
            if (tt != null) {
                types.add(tt);
            }
        }
        AndroidTag wrapped = new AndroidTag(rawTag, types);
        AsyncResource<com.codename1.nfc.Tag> r;
        Set<NfcListener> listenerSnapshot;
        synchronized (this) {
            r = pendingRead;
            pendingRead = null;
            pendingOptions = null;
            listenerSnapshot = new HashSet<NfcListener>(listeners);
            if (listeners.isEmpty()) {
                disarmReader();
            }
        }
        scheduleTagDelivery(r, wrapped, listenerSnapshot);
    }

    // Static so the Runnable does not capture a synthetic outer-AndroidNfc
    // reference (SpotBugs SIC_INNER_SHOULD_BE_STATIC_ANON).
    private static void scheduleTagDelivery(
            final AsyncResource<com.codename1.nfc.Tag> r,
            final AndroidTag wrapped,
            final Set<NfcListener> listenerSnapshot) {
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                if (r != null) {
                    r.complete(wrapped);
                }
                for (NfcListener l : listenerSnapshot) {
                    try {
                        l.tagDiscovered(wrapped);
                    } catch (Throwable ignore) {
                    }
                }
            }
        });
    }

    static TagType mapTech(String s) {
        if (s == null) {
            return null;
        }
        if (s.endsWith(".Ndef") || s.endsWith(".NdefFormatable")) {
            return TagType.NDEF;
        }
        if (s.endsWith(".IsoDep")) {
            return TagType.ISO_DEP;
        }
        if (s.endsWith(".MifareClassic")) {
            return TagType.MIFARE_CLASSIC;
        }
        if (s.endsWith(".MifareUltralight")) {
            return TagType.MIFARE_ULTRALIGHT;
        }
        if (s.endsWith(".NfcA")) {
            return TagType.NFC_A;
        }
        if (s.endsWith(".NfcB")) {
            return TagType.NFC_B;
        }
        if (s.endsWith(".NfcF")) {
            return TagType.NFC_F;
        }
        if (s.endsWith(".NfcV")) {
            return TagType.NFC_V;
        }
        return null;
    }

    /**
     * Tag implementation that wraps the native android.nfc.Tag and exposes
     * the Codename One tag-technology APIs.
     */
    static final class AndroidTag extends com.codename1.nfc.Tag {
        private final Tag native_;

        AndroidTag(Tag t, Set<TagType> types) {
            super(types, t.getId());
            this.native_ = t;
        }

        @Override
        public boolean isWritable() {
            Ndef n = Ndef.get(native_);
            return n != null && n.isWritable();
        }

        @Override
        public int getMaxNdefSize() {
            Ndef n = Ndef.get(native_);
            return n != null ? n.getMaxSize() : -1;
        }

        @Override
        public AsyncResource<NdefMessage> readNdef() {
            final AsyncResource<NdefMessage> r = new AsyncResource<NdefMessage>();
            new Thread(new Runnable() {
                public void run() {
                    Ndef n = Ndef.get(native_);
                    if (n == null) {
                        r.error(new NfcException(NfcError.UNSUPPORTED_TAG,
                                "tag has no NDEF technology"));
                        return;
                    }
                    try {
                        n.connect();
                        android.nfc.NdefMessage msg = n.getNdefMessage();
                        if (msg == null) {
                            r.error(new NfcException(NfcError.INVALID_NDEF,
                                    "tag has no NDEF message"));
                            return;
                        }
                        r.complete(NdefMessage.parse(msg.toByteArray()));
                    } catch (IOException ioe) {
                        r.error(new NfcException(NfcError.IO_ERROR,
                                ioe.getMessage(), ioe));
                    } catch (Throwable t) {
                        r.error(new NfcException(NfcError.UNKNOWN,
                                t.getMessage(), t));
                    } finally {
                        try {
                            n.close();
                        } catch (Throwable ignore) {
                        }
                    }
                }
            }, "AndroidNfc-readNdef").start();
            return r;
        }

        @Override
        public AsyncResource<Boolean> writeNdef(final NdefMessage message) {
            final AsyncResource<Boolean> r = new AsyncResource<Boolean>();
            new Thread(new Runnable() {
                public void run() {
                    if (message == null) {
                        r.error(new NfcException(NfcError.INVALID_NDEF,
                                "null message"));
                        return;
                    }
                    Ndef n = Ndef.get(native_);
                    if (n != null) {
                        try {
                            n.connect();
                            if (!n.isWritable()) {
                                r.error(new NfcException(NfcError.READ_ONLY,
                                        "tag is read-only"));
                                return;
                            }
                            byte[] raw = message.toByteArray();
                            if (n.getMaxSize() > 0 && raw.length > n.getMaxSize()) {
                                r.error(new NfcException(
                                        NfcError.CAPACITY_EXCEEDED,
                                        "message exceeds tag capacity"));
                                return;
                            }
                            n.writeNdefMessage(new android.nfc.NdefMessage(raw));
                            r.complete(Boolean.TRUE);
                        } catch (IOException ioe) {
                            r.error(new NfcException(NfcError.IO_ERROR,
                                    ioe.getMessage(), ioe));
                        } catch (Throwable t) {
                            r.error(new NfcException(NfcError.UNKNOWN,
                                    t.getMessage(), t));
                        } finally {
                            try {
                                n.close();
                            } catch (Throwable ignore) {
                            }
                        }
                        return;
                    }
                    NdefFormatable nf = NdefFormatable.get(native_);
                    if (nf != null) {
                        try {
                            nf.connect();
                            nf.format(new android.nfc.NdefMessage(message.toByteArray()));
                            r.complete(Boolean.TRUE);
                        } catch (IOException ioe) {
                            r.error(new NfcException(NfcError.IO_ERROR,
                                    ioe.getMessage(), ioe));
                        } catch (Throwable t) {
                            r.error(new NfcException(NfcError.UNKNOWN,
                                    t.getMessage(), t));
                        } finally {
                            try {
                                nf.close();
                            } catch (Throwable ignore) {
                            }
                        }
                        return;
                    }
                    r.error(new NfcException(NfcError.UNSUPPORTED_TAG,
                            "tag does not support NDEF writing"));
                }
            }, "AndroidNfc-writeNdef").start();
            return r;
        }

        @Override
        public AsyncResource<Boolean> makeReadOnly() {
            final AsyncResource<Boolean> r = new AsyncResource<Boolean>();
            new Thread(new Runnable() {
                public void run() {
                    Ndef n = Ndef.get(native_);
                    if (n == null) {
                        r.error(new NfcException(NfcError.UNSUPPORTED_TAG,
                                "tag has no NDEF technology"));
                        return;
                    }
                    try {
                        n.connect();
                        boolean ok = n.makeReadOnly();
                        r.complete(ok ? Boolean.TRUE : Boolean.FALSE);
                    } catch (IOException ioe) {
                        r.error(new NfcException(NfcError.IO_ERROR,
                                ioe.getMessage(), ioe));
                    } finally {
                        try {
                            n.close();
                        } catch (Throwable ignore) {
                        }
                    }
                }
            }, "AndroidNfc-lock").start();
            return r;
        }

        @Override
        public com.codename1.nfc.IsoDep getIsoDep() {
            IsoDep d = IsoDep.get(native_);
            return d != null ? new AndroidIsoDep(d) : null;
        }

        @Override
        public com.codename1.nfc.MifareClassic getMifareClassic() {
            MifareClassic m = MifareClassic.get(native_);
            return m != null ? new AndroidMifareClassic(m) : null;
        }

        @Override
        public com.codename1.nfc.MifareUltralight getMifareUltralight() {
            MifareUltralight m = MifareUltralight.get(native_);
            return m != null ? new AndroidMifareUltralight(m) : null;
        }

        @Override
        public com.codename1.nfc.NfcA getNfcA() {
            NfcA t = NfcA.get(native_);
            return t != null ? new AndroidNfcA(t) : null;
        }

        @Override
        public com.codename1.nfc.NfcB getNfcB() {
            NfcB t = NfcB.get(native_);
            return t != null ? new AndroidNfcB(t) : null;
        }

        @Override
        public com.codename1.nfc.NfcF getNfcF() {
            NfcF t = NfcF.get(native_);
            return t != null ? new AndroidNfcF(t) : null;
        }

        @Override
        public com.codename1.nfc.NfcV getNfcV() {
            NfcV t = NfcV.get(native_);
            return t != null ? new AndroidNfcV(t) : null;
        }
    }

    /** Helper that runs the given Callable on a worker thread and resolves
     * the AsyncResource with its outcome. */
    static <T> AsyncResource<T> asyncIo(final java.util.concurrent.Callable<T> c) {
        final AsyncResource<T> r = new AsyncResource<T>();
        new Thread(new Runnable() {
            public void run() {
                try {
                    r.complete(c.call());
                } catch (NfcException ne) {
                    r.error(ne);
                } catch (IOException ioe) {
                    r.error(new NfcException(NfcError.IO_ERROR,
                            ioe.getMessage(), ioe));
                } catch (Throwable t) {
                    r.error(new NfcException(NfcError.UNKNOWN,
                            t.getMessage(), t));
                }
            }
        }, "AndroidNfc-io").start();
        return r;
    }
}
