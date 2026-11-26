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

import com.codename1.io.Socket;
import com.codename1.io.SocketConnection;
import com.codename1.io.Util;
import com.codename1.ui.CN;
import java.io.InputStream;
import java.io.OutputStream;

/// Sends a job to a network receipt printer's raw port over a Codename One
/// socket: connect, write every byte, close. The printer prints what it is
/// sent; there is nothing to read back.
public final class SocketPrinter implements PrinterSink {
    private final PrinterAddress address;

    public SocketPrinter(PrinterAddress address) {
        this.address = address;
    }

    @Override
    public void send(final byte[] job, final Done done) {
        if (!Socket.isSupported()) {
            done.done("Sockets are not available on this device");
            return;
        }
        Socket.connect(address.host, address.port, new SocketConnection(5000) {
            @Override
            public void connectionError(int errorCode, final String message) {
                CN.callSerially(() -> done.done(message == null || message.length() == 0
                        ? "The printer did not answer" : message));
            }

            @Override
            public void connectionEstablished(InputStream is, OutputStream os) {
                String error = null;
                try {
                    os.write(job);
                    os.flush();
                } catch (java.io.IOException failed) {
                    error = failed.getMessage() == null ? "The printer closed the connection"
                            : failed.getMessage();
                } finally {
                    Util.cleanup(os);
                    Util.cleanup(is);
                }
                final String result = error;
                CN.callSerially(() -> done.done(result));
            }
        });
    }
}
