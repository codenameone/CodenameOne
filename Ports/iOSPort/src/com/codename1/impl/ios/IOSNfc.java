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
package com.codename1.impl.ios;

import com.codename1.nfc.HostCardEmulationService;
import com.codename1.nfc.NdefMessage;
import com.codename1.nfc.Nfc;
import com.codename1.nfc.NfcError;
import com.codename1.nfc.NfcException;
import com.codename1.nfc.NfcListener;
import com.codename1.nfc.NfcReadOptions;
import com.codename1.nfc.TagType;
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * iOS implementation of {@link Nfc} backed by Core NFC. NDEF read/write is
 * available on iOS 13+; ISO 7816 / MIFARE / FeliCa transceive via
 * NFCTagReaderSession on iOS 13+; host card emulation via CardSession on
 * iOS 17.4+ EU-only.
 *
 * <p>The native side dispatches results back via the static
 * {@link #nativeNdefResult(int, byte[])} /
 * {@link #nativeTagDiscovered(int, long, int, byte[])} /
 * {@link #nativeTransceiveResult(int, byte[])} /
 * {@link #nativeNfcError(int, int, String)} methods. The static initializer
 * touches each to stop the ParparVM dead-code eliminator from stripping
 * them.</p>
 */
public final class IOSNfc extends Nfc {

    static {
        nativeNdefResult(-1, null);
        nativeTagDiscovered(-1, 0L, 0, null);
        nativeTransceiveResult(-1, null);
        nativeWriteResult(-1, false);
        nativeNfcError(-1, 0, null);
        nativeHceApdu(-1, null);
        nativeHceDeactivated(0);
    }

    private static final Map<Integer, Object> REQUESTS =
            new HashMap<Integer, Object>();
    private static final Map<Long, IOSTag> TAGS = new HashMap<Long, IOSTag>();
    private static int nextRequestId = 1;
    private static volatile HostCardEmulationService hceService;

    private final IOSNative nativeInstance;
    private final Set<NfcListener> listeners = new HashSet<NfcListener>();
    private int activeReadRequestId;

    IOSNfc(IOSNative nativeInstance) {
        this.nativeInstance = nativeInstance;
    }

    @Override
    public boolean isSupported() {
        return nativeInstance.isNfcSupported();
    }

    @Override
    public boolean canRead() {
        return nativeInstance.canReadNfc();
    }

    @Override
    public boolean canWrite() {
        return nativeInstance.canReadNfcTags();
    }

    @Override
    public boolean canHostEmulate() {
        return nativeInstance.canHostEmulateNfc();
    }

    @Override
    public synchronized AsyncResource<com.codename1.nfc.Tag> readTag(NfcReadOptions options) {
        AsyncResource<com.codename1.nfc.Tag> r =
                new AsyncResource<com.codename1.nfc.Tag>();
        if (!nativeInstance.isNfcSupported()) {
            r.error(new NfcException(NfcError.NOT_AVAILABLE,
                    "Core NFC is not available on this device"));
            return r;
        }
        NfcReadOptions opts = options != null ? options : new NfcReadOptions();
        int rid = takeId(r);
        activeReadRequestId = rid;
        String alert = opts.getAlertMessage();
        if (opts.isNdefOnly() || opts.getTechFilter().isEmpty()
                || opts.getTechFilter().contains(TagType.NDEF)
                && opts.getTechFilter().size() == 1) {
            nativeInstance.startNdefRead(rid, alert, opts.getTimeoutMs());
        } else {
            int polling = pollingMask(opts);
            String[] systemCodes = opts.getFelicaSystemCodes().toArray(new String[0]);
            byte[][] aids = opts.getIsoSelectAids().toArray(new byte[0][]);
            nativeInstance.startTagRead(rid, alert, polling, systemCodes,
                    aids, opts.getTimeoutMs());
        }
        return r;
    }

    @Override
    public synchronized boolean stopRead() {
        if (activeReadRequestId == 0) {
            return false;
        }
        nativeInstance.stopNfcRead(activeReadRequestId);
        Object o = REQUESTS.remove(Integer.valueOf(activeReadRequestId));
        activeReadRequestId = 0;
        if (o instanceof AsyncResource) {
            AsyncResource<?> ar = (AsyncResource<?>) o;
            if (!ar.isDone()) {
                ar.error(new NfcException(NfcError.USER_CANCELED,
                        "NFC read cancelled"));
            }
        }
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
    public synchronized void registerHostCardEmulationService(
            HostCardEmulationService service) {
        hceService = service;
        if (service == null) {
            nativeInstance.registerHceAids(new String[0]);
        } else {
            String[] aids = service.getAids();
            nativeInstance.registerHceAids(aids != null ? aids : new String[0]);
        }
    }

    @Override
    public synchronized void unregisterHostCardEmulationService() {
        hceService = null;
        nativeInstance.registerHceAids(new String[0]);
    }

    private static int pollingMask(NfcReadOptions opts) {
        int mask = 0;
        List<TagType> filter = opts.getTechFilter();
        if (filter.isEmpty()) {
            return 1 | 4; // A + F by default
        }
        for (TagType t : filter) {
            switch (t) {
                case NFC_A:
                case ISO_DEP:
                case MIFARE_CLASSIC:
                case MIFARE_ULTRALIGHT:
                    mask |= 1;
                    break;
                case NFC_B:
                    mask |= 2;
                    break;
                case NFC_F:
                    mask |= 4;
                    break;
                case NFC_V:
                    mask |= 8;
                    break;
                default:
                    break;
            }
        }
        if (mask == 0) {
            mask = 1;
        }
        return mask;
    }

    @SuppressWarnings("unchecked")
    private static <T> AsyncResource<T> takeAsync(int requestId) {
        synchronized (REQUESTS) {
            Object o = REQUESTS.remove(Integer.valueOf(requestId));
            return o instanceof AsyncResource ? (AsyncResource<T>) o : null;
        }
    }

    private static int takeId(Object holder) {
        synchronized (REQUESTS) {
            int id = nextRequestId++;
            REQUESTS.put(Integer.valueOf(id), holder);
            return id;
        }
    }

    static NfcError mapNativeError(int code) {
        // NFCReaderError values from Core NFC.
        switch (code) {
            case 200: // NFCReaderSessionInvalidationErrorUserCanceled
                return NfcError.USER_CANCELED;
            case 201: // ...SessionTimeout
                return NfcError.SYSTEM_CANCELED;
            case 202: // ...SystemIsBusy
                return NfcError.SYSTEM_CANCELED;
            case 203: // ...FirstNDEFTagRead
                return NfcError.SYSTEM_CANCELED;
            case 204: // ...InvalidParameter
                return NfcError.UNKNOWN;
            case 100: // NFCReaderTransceiveErrorTagConnectionLost
                return NfcError.TAG_LOST;
            case 102: // ...RetryExceeded
                return NfcError.IO_ERROR;
            case 105: // ...PacketTooLong
                return NfcError.IO_ERROR;
            case 1: // NFCNdefReaderSessionErrorTagNotWritable
                return NfcError.READ_ONLY;
            case 2: // ...TagSizeTooSmall
                return NfcError.CAPACITY_EXCEEDED;
            case 3: // ...TagUpdateFailure
                return NfcError.IO_ERROR;
            case 4: // ...ZeroLengthMessage
                return NfcError.INVALID_NDEF;
            case 1001: // NFCFeatureUnavailableError
                return NfcError.NOT_AVAILABLE;
            default:
                return NfcError.UNKNOWN;
        }
    }

    // ---- Callbacks invoked from native code (do not rename) ----------------

    /** Single-shot NDEF read result -- raw bytes of the NDEF message. */
    public static void nativeNdefResult(final int requestId, final byte[] raw) {
        final AsyncResource<com.codename1.nfc.Tag> r = takeAsync(requestId);
        if (r == null) {
            return;
        }
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                if (r.isDone()) {
                    return;
                }
                try {
                    NdefMessage msg = NdefMessage.parse(raw);
                    Set<TagType> types = new HashSet<TagType>();
                    types.add(TagType.NDEF);
                    r.complete(new IOSTag(0L, types, new byte[0],
                            msg, true, -1));
                } catch (NfcException e) {
                    r.error(e);
                }
            }
        });
    }

    /** A tag was discovered during a tag-reader session (ISO / FeliCa / MIFARE). */
    public static void nativeTagDiscovered(final int requestId,
            final long handle, final int techMask, final byte[] uid) {
        final AsyncResource<com.codename1.nfc.Tag> r = takeAsync(requestId);
        if (r == null) {
            return;
        }
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                Set<TagType> types = decodeTechMask(techMask);
                IOSTag t = new IOSTag(handle, types,
                        uid != null ? uid : new byte[0], null, false, -1);
                synchronized (TAGS) {
                    TAGS.put(Long.valueOf(handle), t);
                }
                if (!r.isDone()) {
                    r.complete(t);
                }
            }
        });
    }

    /** Result from {@code nfcTransceive}. */
    public static void nativeTransceiveResult(int requestId, final byte[] data) {
        final AsyncResource<byte[]> r = takeAsync(requestId);
        if (r == null) {
            return;
        }
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                if (!r.isDone()) {
                    r.complete(data != null ? data : new byte[0]);
                }
            }
        });
    }

    /** Result from {@code nfcWriteNdefToTag} / {@code nfcLockTag}. */
    public static void nativeWriteResult(int requestId, final boolean ok) {
        final AsyncResource<Boolean> r = takeAsync(requestId);
        if (r == null) {
            return;
        }
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                if (!r.isDone()) {
                    r.complete(Boolean.valueOf(ok));
                }
            }
        });
    }

    /** Error path for any of the async NFC calls. */
    public static void nativeNfcError(final int requestId, final int code,
            final String msg) {
        final AsyncResource<?> r = takeAsync(requestId);
        if (r == null) {
            return;
        }
        final NfcError mapped = mapNativeError(code);
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                if (!r.isDone()) {
                    r.error(new NfcException(mapped,
                            msg == null ? mapped.name() : msg));
                }
            }
        });
    }

    /** APDU received from a terminal while CardSession is active. The
     * application supplied service produces a response and we hand it back
     * to the OS via IOSNative.hceSendResponse. */
    public static void nativeHceApdu(final int sessionId, final byte[] apdu) {
        final HostCardEmulationService svc = hceService;
        if (svc == null) {
            return;
        }
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                byte[] resp;
                try {
                    resp = svc.processCommand(apdu);
                } catch (Throwable t) {
                    resp = com.codename1.nfc.ApduResponse.SW_UNKNOWN_ERROR;
                }
                IOSNfc nfc = (IOSNfc) Display.getInstance().getNfc();
                if (nfc != null) {
                    nfc.nativeInstance.hceSendResponse(resp != null
                            ? resp
                            : com.codename1.nfc.ApduResponse.SW_UNKNOWN_ERROR);
                }
            }
        });
    }

    /** CardSession deactivation callback. */
    public static void nativeHceDeactivated(final int reason) {
        final HostCardEmulationService svc = hceService;
        if (svc == null) {
            return;
        }
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                try {
                    svc.onDeactivated(reason);
                } catch (Throwable ignore) {
                }
            }
        });
    }

    private static Set<TagType> decodeTechMask(int mask) {
        Set<TagType> types = new HashSet<TagType>();
        if ((mask & 1) != 0) {
            types.add(TagType.NFC_A);
        }
        if ((mask & 2) != 0) {
            types.add(TagType.NFC_F);
        }
        if ((mask & 4) != 0) {
            types.add(TagType.ISO_DEP);
        }
        if ((mask & 8) != 0) {
            types.add(TagType.MIFARE_ULTRALIGHT);
        }
        if ((mask & 16) != 0) {
            types.add(TagType.NDEF);
        }
        return types;
    }

    /** iOS-specific Tag subclass that holds the native session-side
     * handle plus optional eager-read NDEF message. */
    static final class IOSTag extends com.codename1.nfc.Tag {
        final long handle;
        final NdefMessage eagerNdef;
        final boolean writable;
        final int maxNdefSize;
        final IOSIsoDep iso;

        IOSTag(long handle, Set<TagType> types, byte[] id,
                NdefMessage eagerNdef, boolean writable, int maxNdefSize) {
            super(types, id);
            this.handle = handle;
            this.eagerNdef = eagerNdef;
            this.writable = writable;
            this.maxNdefSize = maxNdefSize;
            this.iso = supports(TagType.ISO_DEP) ? new IOSIsoDep(handle) : null;
        }

        @Override
        public boolean isWritable() {
            return writable;
        }

        @Override
        public int getMaxNdefSize() {
            return maxNdefSize;
        }

        @Override
        public AsyncResource<NdefMessage> readNdef() {
            if (eagerNdef != null) {
                AsyncResource<NdefMessage> r = new AsyncResource<NdefMessage>();
                r.complete(eagerNdef);
                return r;
            }
            return tagOp(new TagOpInvoker<NdefMessage>() {
                public void run(int rid, long h, IOSNative ni) {
                    ni.nfcReadNdefFromTag(rid, h);
                }
            });
        }

        @Override
        public AsyncResource<Boolean> writeNdef(final NdefMessage message) {
            if (message == null) {
                AsyncResource<Boolean> r = new AsyncResource<Boolean>();
                r.error(new NfcException(NfcError.INVALID_NDEF,
                        "null message"));
                return r;
            }
            return tagOp(new TagOpInvoker<Boolean>() {
                public void run(int rid, long h, IOSNative ni) {
                    ni.nfcWriteNdefToTag(rid, h, message.toByteArray());
                }
            });
        }

        @Override
        public AsyncResource<Boolean> makeReadOnly() {
            return tagOp(new TagOpInvoker<Boolean>() {
                public void run(int rid, long h, IOSNative ni) {
                    ni.nfcLockTag(rid, h);
                }
            });
        }

        @Override
        public com.codename1.nfc.IsoDep getIsoDep() {
            return iso;
        }

        private <T> AsyncResource<T> tagOp(TagOpInvoker<T> inv) {
            AsyncResource<T> r = new AsyncResource<T>();
            if (handle == 0L) {
                r.error(new NfcException(NfcError.TAG_LOST,
                        "tag handle no longer valid"));
                return r;
            }
            IOSNfc nfc = (IOSNfc) Display.getInstance().getNfc();
            if (nfc == null) {
                r.error(new NfcException(NfcError.NOT_AVAILABLE,
                        "NFC not available"));
                return r;
            }
            int rid = takeId(r);
            inv.run(rid, handle, nfc.nativeInstance);
            return r;
        }
    }

    interface TagOpInvoker<T> {
        void run(int rid, long handle, IOSNative ni);
    }

    /** iOS IsoDep view that posts transceive requests to Core NFC's
     * NFCISO7816Tag through {@link IOSNative#nfcTransceive(int, long, byte[])}. */
    static final class IOSIsoDep extends com.codename1.nfc.IsoDep {
        private final long handle;

        IOSIsoDep(long handle) {
            this.handle = handle;
        }

        @Override
        public AsyncResource<byte[]> transceive(byte[] apdu) {
            AsyncResource<byte[]> r = new AsyncResource<byte[]>();
            if (handle == 0L) {
                r.error(new NfcException(NfcError.TAG_LOST,
                        "tag handle no longer valid"));
                return r;
            }
            IOSNfc nfc = (IOSNfc) Display.getInstance().getNfc();
            if (nfc == null) {
                r.error(new NfcException(NfcError.NOT_AVAILABLE,
                        "NFC not available"));
                return r;
            }
            int rid;
            synchronized (REQUESTS) {
                rid = nextRequestId++;
                REQUESTS.put(Integer.valueOf(rid), r);
            }
            nfc.nativeInstance.nfcTransceive(rid, handle, apdu);
            return r;
        }
    }
}
