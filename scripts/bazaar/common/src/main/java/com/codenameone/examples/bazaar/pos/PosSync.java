/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bazaar.pos;

import com.codename1.appkit.Net;
import com.codename1.io.NetworkManager;
import com.codename1.io.NetworkTypeListener;
import com.codename1.ui.CN;
import com.codenameone.examples.bazaar.api.PosSaleDto;
import com.codenameone.examples.bazaar.api.PosSyncDto;
import com.codenameone.examples.bazaar.net.Api;
import java.util.ArrayList;
import java.util.List;

/// Uploads the outbox in the background.
///
/// Every few seconds, and at once after a sale or when the network comes
/// back, the sales whose time has come go up in one request. The server
/// answers each by its client id -- OK, DUPLICATE or ERROR -- so a request
/// that went through but whose answer was lost is simply sent again. A
/// request with no answer at all marks the register offline and puts its
/// sales back with a longer wait; see [Outbox].
public final class PosSync {
    /// Online, offline, or a request in flight: what the status pill shows.
    public static final String ONLINE = "ONLINE";
    public static final String OFFLINE = "OFFLINE";
    public static final String SYNCING = "SYNCING";

    private static final int TICK_MS = 5000;
    private static boolean started;
    private static boolean busy;
    private static long busySince;
    private static List<Outbox.Entry> inFlight;
    private static boolean online = true;

    private PosSync() {
    }

    /// Starts the background loop; safe to call again.
    public static void start() {
        if (started) {
            return;
        }
        started = true;
        CN.setInterval(TICK_MS, PosSync::tick);
        NetworkManager.getInstance().addNetworkTypeListener(new NetworkTypeListener() {
            @Override
            public void onNetworkTypeChanged(int oldType, int newType, boolean vpnActive) {
                CN.callSerially(() -> {
                    if (newType != NetworkManager.NETWORK_TYPE_NONE) {
                        PosState.outbox().wake();
                        tick();
                        PosState.refreshCatalog(null);
                    } else {
                        reachable(false);
                    }
                });
            }
        });
    }

    /// Tries now: after a sale, or from the outbox screen.
    public static void kick() {
        CN.callSerially(PosSync::tick);
    }

    public static String status() {
        return busy ? SYNCING : online ? ONLINE : OFFLINE;
    }

    public static boolean online() {
        return online;
    }

    /// What the last request said about the network.
    static void reachable(boolean yes) {
        if (online != yes) {
            online = yes;
            PosState.fireStatus();
        }
    }

    private static void tick() {
        if (busy && System.currentTimeMillis() - busySince > 60000L && inFlight != null) {
            // An answer that never came back (the session moved on and the
            // callback was dropped): its sales wait and go again.
            PosState.outbox().unanswered(inFlight, System.currentTimeMillis());
            busy = false;
        }
        if (busy || !PosState.hasRegister() || Api.pos() == null) {
            return;
        }
        ParkedPane.sync();
        final Outbox box = PosState.outbox();
        final List<Outbox.Entry> batch = box.due(System.currentTimeMillis());
        if (batch.isEmpty()) {
            return;
        }
        List<PosSaleDto> sales = new ArrayList<PosSaleDto>();
        for (int iter = 0; iter < batch.size(); iter++) {
            sales.add(batch.get(iter).sale);
        }
        busy = true;
        busySince = System.currentTimeMillis();
        inFlight = batch;
        box.sending(batch);
        PosState.fireStatus();
        Api.pos().sync(PosState.storeId(), PosState.registerId(), new PosSyncDto(sales),
                Net.to(result -> {
                    busy = false;
                    online = true;
                    box.answered(batch, result == null ? null : result.results(),
                            System.currentTimeMillis());
                    PosState.fireStatus();
                    if (!box.due(System.currentTimeMillis()).isEmpty()) {
                        kick();
                    }
                }, (status, message) -> {
                    busy = false;
                    // No answer, or one for the whole request (signed out,
                    // a server error): every sale waits and goes again.
                    if (status <= 0 || status >= 500) {
                        online = false;
                    }
                    box.unanswered(batch, System.currentTimeMillis());
                    PosState.fireStatus();
                }));
    }
}
