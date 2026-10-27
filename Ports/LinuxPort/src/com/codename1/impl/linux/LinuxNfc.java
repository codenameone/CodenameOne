/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.impl.linux;

import com.codename1.nfc.IsoDep;
import com.codename1.nfc.Nfc;
import com.codename1.nfc.NfcError;
import com.codename1.nfc.NfcException;
import com.codename1.nfc.NfcListener;
import com.codename1.nfc.NfcReadOptions;
import com.codename1.nfc.Tag;
import com.codename1.nfc.TagType;
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/// NFC tag reading on Linux through a PC/SC contactless reader (pcscd and
/// libpcsclite, loaded on first use). The card is reported as an ISO-DEP tag
/// whose id is the UID the reader returns for `FF CA 00 00 00`; APDUs are
/// exchanged with [IsoDep#transceive(byte[])]. NDEF, writing and host card
/// emulation are not available.
class LinuxNfc extends Nfc {
    private static final byte[] GET_UID = new byte[] {(byte) 0xFF, (byte) 0xCA, 0, 0, 0};

    /// 0 unknown, 1 available, -1 unavailable.
    private final AtomicInteger available = new AtomicInteger();
    private final AtomicReference<AsyncResource<Tag>> pending = new AtomicReference<AsyncResource<Tag>>();
    /// Handle of the card from the last successful read, 0 when none.
    private final AtomicLong card = new AtomicLong();
    private final List<NfcListener> listeners = new ArrayList<NfcListener>();

    @Override
    public boolean isSupported() {
        int a = available.get();
        if (a == 0) {
            a = LinuxNative.nfcAvailable() ? 1 : -1;
            available.set(a);
        }
        return a == 1;
    }

    @Override
    public boolean canRead() {
        return isSupported();
    }

    @Override
    public AsyncResource<Tag> readTag(NfcReadOptions options) {
        final AsyncResource<Tag> r = new AsyncResource<Tag>();
        if (!isSupported()) {
            r.error(new NfcException(NfcError.NOT_AVAILABLE, "No PC/SC service (pcscd) available"));
            return r;
        }
        if (!pending.compareAndSet(null, r)) {
            r.error(new NfcException(NfcError.IO_ERROR, "A read is already in progress"));
            return r;
        }
        releaseCard();
        long t = options == null ? 0 : options.getTimeoutMs();
        final int timeout = t <= 0 ? 0 : (int) Math.min(t, Integer.MAX_VALUE);
        Thread worker = new Thread(new Runnable() {
            public void run() {
                waitForCard(r, timeout);
            }
        }, "CN1 NFC reader");
        worker.setDaemon(true);
        worker.start();
        return r;
    }

    private void waitForCard(AsyncResource<Tag> r, int timeout) {
        long handle = LinuxNative.nfcWaitCard(timeout);
        if (handle <= 0) {
            NfcError e = handle == 0 ? NfcError.SYSTEM_CANCELED
                    : handle == -2 ? NfcError.USER_CANCELED : NfcError.IO_ERROR;
            finish(r, null, new NfcException(e, handle == 0 ? "Timed out waiting for a tag" : "Tag read failed"));
            return;
        }
        byte[] resp = LinuxNative.nfcTransmit(handle, GET_UID);
        byte[] uid;
        if (IsoDep.isSuccess(resp)) {
            uid = new byte[resp.length - 2];
            System.arraycopy(resp, 0, uid, 0, uid.length);
        } else {
            uid = new byte[0];
        }
        Set<TagType> types = new HashSet<TagType>();
        types.add(TagType.ISO_DEP);
        if (pending.get() != r) {
            LinuxNative.nfcDisconnect(handle);
            return;
        }
        card.set(handle);
        finish(r, new LinuxTag(types, uid, handle), null);
    }

    private void finish(final AsyncResource<Tag> r, final Tag tag, final NfcException err) {
        if (!pending.compareAndSet(r, null)) {
            return;
        }
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                NfcListener[] ls = listeners.toArray(new NfcListener[listeners.size()]);
                if (tag != null) {
                    r.complete(tag);
                    for (NfcListener l : ls) {
                        l.tagDiscovered(tag);
                    }
                } else {
                    r.error(err);
                    for (NfcListener l : ls) {
                        l.sessionFailed(err);
                    }
                }
            }
        });
    }

    private void releaseCard() {
        long old = card.getAndSet(0);
        if (old > 0) {
            LinuxNative.nfcDisconnect(old);
        }
    }

    @Override
    public boolean stopRead() {
        if (pending.get() == null) {
            return false;
        }
        LinuxNative.nfcCancel();
        return true;
    }

    @Override
    public void addTagListener(NfcListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeTagListener(NfcListener listener) {
        listeners.remove(listener);
    }

    /// A card on a PC/SC reader. The connection stays open until the next
    /// [Nfc#readTag(NfcReadOptions)].
    static final class LinuxTag extends Tag {
        private final IsoDep isoDep;

        LinuxTag(Set<TagType> types, byte[] id, final long handle) {
            super(types, id);
            isoDep = new IsoDep() {
                @Override
                public AsyncResource<byte[]> transceive(final byte[] apdu) {
                    final AsyncResource<byte[]> r = new AsyncResource<byte[]>();
                    if (apdu == null) {
                        r.error(new NfcException(NfcError.IO_ERROR, "null APDU"));
                        return r;
                    }
                    final byte[] copy = new byte[apdu.length];
                    System.arraycopy(apdu, 0, copy, 0, apdu.length);
                    Thread t = new Thread(new Runnable() {
                        public void run() {
                            final byte[] resp = LinuxNative.nfcTransmit(handle, copy);
                            Display.getInstance().callSerially(new Runnable() {
                                public void run() {
                                    if (resp == null) {
                                        r.error(new NfcException(NfcError.TAG_LOST, "Card removed or transmit failed"));
                                    } else {
                                        r.complete(resp);
                                    }
                                }
                            });
                        }
                    }, "CN1 NFC transceive");
                    t.setDaemon(true);
                    t.start();
                    return r;
                }

                @Override
                public int getMaxTransceiveLength() {
                    return 65535;
                }

                @Override
                public boolean isExtendedLengthSupported() {
                    return true;
                }
            };
        }

        @Override
        public IsoDep getIsoDep() {
            return isoDep;
        }
    }
}
