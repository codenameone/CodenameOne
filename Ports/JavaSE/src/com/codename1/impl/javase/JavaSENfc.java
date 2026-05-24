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
package com.codename1.impl.javase;

import com.codename1.nfc.ApduResponse;
import com.codename1.nfc.HostCardEmulationService;
import com.codename1.nfc.NdefMessage;
import com.codename1.nfc.NdefRecord;
import com.codename1.nfc.Nfc;
import com.codename1.nfc.NfcError;
import com.codename1.nfc.NfcException;
import com.codename1.nfc.NfcListener;
import com.codename1.nfc.NfcReadOptions;
import com.codename1.nfc.TagType;
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * JavaSE-simulator backing for {@link Nfc}. Exposes a virtual tag the
 * developer can edit from the Simulate -> NFC menu and (when armed by
 * readTag()) tap to fire the discovery callback. The HCE side delivers a
 * synthetic APDU to any registered {@link HostCardEmulationService}.
 */
public class JavaSENfc extends Nfc {

    /** Toggle from the Simulate -> NFC menu -- hides the API entirely if false. */
    public static boolean simSupported = true;
    /** Toggle: when false, isSupported() still returns true but canRead() is false. */
    public static boolean simEnabled = true;
    /** Toggle: the device claims to support host card emulation. */
    public static boolean simHceSupported = true;

    /** Virtual tag contents -- edited via the simulator dialog. */
    public static volatile NdefMessage simNdef =
            new NdefMessage(NdefRecord.createUri("https://codenameone.com"));

    /** Tag technology list reported on the virtual tag. */
    public static volatile Set<TagType> simTagTechs;

    static {
        Set<TagType> defaultTechs = new HashSet<TagType>();
        defaultTechs.add(TagType.NDEF);
        defaultTechs.add(TagType.NFC_A);
        simTagTechs = defaultTechs;
    }

    /** UID of the virtual tag (defensively copied on access). */
    public static volatile byte[] simTagUid = new byte[] {
            (byte) 0x04, (byte) 0x12, (byte) 0x34,
            (byte) 0x56, (byte) 0x78, (byte) 0x9A, (byte) 0xBC };

    /** Configured outcome of the next readTag() call. */
    public enum SimReadOutcome {
        DISCOVER_TAG, USER_CANCELED, TAG_LOST, TIMEOUT, READ_ONLY
    }

    public static volatile SimReadOutcome nextReadOutcome = SimReadOutcome.DISCOVER_TAG;
    public static volatile boolean tagWritable = true;

    /** Last APDU exchanged with the registered HCE service, for menu display. */
    public static volatile byte[] lastHceCommand;
    public static volatile byte[] lastHceResponse;

    private AsyncResource<com.codename1.nfc.Tag> pendingRead;
    private final Set<NfcListener> listeners = new HashSet<NfcListener>();
    private HostCardEmulationService hceService;

    @Override
    public boolean isSupported() {
        return simSupported;
    }

    @Override
    public boolean canRead() {
        return simSupported && simEnabled;
    }

    @Override
    public boolean canWrite() {
        return canRead();
    }

    @Override
    public boolean canHostEmulate() {
        return simHceSupported && simSupported;
    }

    @Override
    public synchronized AsyncResource<com.codename1.nfc.Tag> readTag(NfcReadOptions options) {
        AsyncResource<com.codename1.nfc.Tag> r = new AsyncResource<com.codename1.nfc.Tag>();
        if (!simSupported) {
            r.error(new NfcException(NfcError.NOT_AVAILABLE,
                    "Simulator reports NFC unsupported"));
            return r;
        }
        if (!simEnabled) {
            r.error(new NfcException(NfcError.DISABLED,
                    "Simulator reports NFC disabled"));
            return r;
        }
        pendingRead = r;
        return r;
    }

    @Override
    public synchronized boolean stopRead() {
        if (pendingRead == null) {
            return false;
        }
        AsyncResource<com.codename1.nfc.Tag> r = pendingRead;
        pendingRead = null;
        r.error(new NfcException(NfcError.USER_CANCELED, "cancelled in simulator"));
        return true;
    }

    @Override
    public synchronized void addTagListener(NfcListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    @Override
    public synchronized void removeTagListener(NfcListener listener) {
        listeners.remove(listener);
    }

    @Override
    public synchronized void registerHostCardEmulationService(HostCardEmulationService service) {
        this.hceService = service;
    }

    @Override
    public synchronized void unregisterHostCardEmulationService() {
        this.hceService = null;
    }

    /** Returns the HCE service currently registered with this Nfc instance,
     * or null. Used by the simulator menu to fire a manual APDU. */
    public synchronized HostCardEmulationService getHceService() {
        return hceService;
    }

    /** Fires the configured outcome for any pending readTag() call + every
     * registered listener. Called from the Simulate -> NFC menu's "Tap virtual
     * tag" item. */
    public void simulateTap() {
        final AsyncResource<com.codename1.nfc.Tag> r;
        final Set<NfcListener> snapshot;
        final SimReadOutcome outcome = nextReadOutcome;
        synchronized (this) {
            r = pendingRead;
            pendingRead = null;
            snapshot = new HashSet<NfcListener>(listeners);
        }
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                if (outcome != SimReadOutcome.DISCOVER_TAG) {
                    NfcError code;
                    switch (outcome) {
                        case USER_CANCELED:
                            code = NfcError.USER_CANCELED;
                            break;
                        case TAG_LOST:
                            code = NfcError.TAG_LOST;
                            break;
                        case TIMEOUT:
                            code = NfcError.SYSTEM_CANCELED;
                            break;
                        case READ_ONLY:
                            code = NfcError.READ_ONLY;
                            break;
                        default:
                            code = NfcError.UNKNOWN;
                            break;
                    }
                    NfcException ex = new NfcException(code,
                            "simulator outcome " + outcome.name());
                    if (r != null) {
                        r.error(ex);
                    }
                    for (NfcListener l : snapshot) {
                        try {
                            l.sessionFailed(ex);
                        } catch (Throwable ignore) {
                        }
                    }
                    return;
                }
                VirtualTag t = new VirtualTag(
                        new HashSet<TagType>(simTagTechs),
                        copyOf(simTagUid));
                if (r != null) {
                    r.complete(t);
                }
                for (NfcListener l : snapshot) {
                    try {
                        l.tagDiscovered(t);
                    } catch (Throwable ignore) {
                    }
                }
            }
        });
    }

    /** Sends the given command APDU to the registered HCE service and stores
     * the response for the simulator UI to display. */
    public byte[] simulateApdu(byte[] command) {
        HostCardEmulationService svc;
        synchronized (this) {
            svc = hceService;
        }
        lastHceCommand = copyOf(command);
        if (svc == null) {
            byte[] resp = ApduResponse.swFileNotFound();
            lastHceResponse = copyOf(resp);
            return resp;
        }
        byte[] resp;
        try {
            resp = svc.processCommand(command);
            if (resp == null) {
                resp = ApduResponse.swUnknownError();
            }
        } catch (Throwable t) {
            resp = ApduResponse.swUnknownError();
        }
        lastHceResponse = copyOf(resp);
        return resp;
    }

    /** Triggers the registered HCE service's onDeactivated callback. */
    public void simulateDeactivate(int reason) {
        HostCardEmulationService svc;
        synchronized (this) {
            svc = hceService;
        }
        if (svc != null) {
            try {
                svc.onDeactivated(reason);
            } catch (Throwable ignore) {
            }
        }
    }

    private static byte[] copyOf(byte[] in) {
        if (in == null) {
            return new byte[0];
        }
        byte[] out = new byte[in.length];
        System.arraycopy(in, 0, out, 0, in.length);
        return out;
    }

    /** Virtual tag implementation that hands back simNdef. */
    static final class VirtualTag extends com.codename1.nfc.Tag {

        VirtualTag(Set<TagType> types, byte[] id) {
            super(types, id);
        }

        @Override
        public boolean isWritable() {
            return tagWritable;
        }

        @Override
        public int getMaxNdefSize() {
            return 1024;
        }

        @Override
        public AsyncResource<NdefMessage> readNdef() {
            AsyncResource<NdefMessage> r = new AsyncResource<NdefMessage>();
            NdefMessage msg = simNdef;
            if (msg == null) {
                r.error(new NfcException(NfcError.INVALID_NDEF, "tag is empty"));
            } else {
                r.complete(msg);
            }
            return r;
        }

        @Override
        public AsyncResource<Boolean> writeNdef(NdefMessage message) {
            AsyncResource<Boolean> r = new AsyncResource<Boolean>();
            if (!tagWritable) {
                r.error(new NfcException(NfcError.READ_ONLY,
                        "virtual tag is locked"));
                return r;
            }
            if (message == null) {
                r.error(new NfcException(NfcError.INVALID_NDEF, "null message"));
                return r;
            }
            byte[] raw = message.toByteArray();
            if (raw.length > getMaxNdefSize()) {
                r.error(new NfcException(NfcError.CAPACITY_EXCEEDED,
                        "exceeds 1024 bytes"));
                return r;
            }
            simNdef = message;
            r.complete(Boolean.TRUE);
            return r;
        }

        @Override
        public AsyncResource<Boolean> makeReadOnly() {
            AsyncResource<Boolean> r = new AsyncResource<Boolean>();
            tagWritable = false;
            r.complete(Boolean.TRUE);
            return r;
        }

        @Override
        public com.codename1.nfc.IsoDep getIsoDep() {
            if (!supports(TagType.ISO_DEP)) {
                return null;
            }
            return new SimIsoDep();
        }
    }

    /** Loop-back IsoDep that echoes the command with a success status word. */
    static final class SimIsoDep extends com.codename1.nfc.IsoDep {
        @Override
        public AsyncResource<byte[]> transceive(byte[] apdu) {
            AsyncResource<byte[]> r = new AsyncResource<byte[]>();
            byte[] body = apdu != null ? apdu : new byte[0];
            r.complete(ApduResponse.withStatus(body, ApduResponse.swSuccess()));
            return r;
        }
    }
}
